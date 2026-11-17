package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.casouso.Exploracion
import com.daxlejo.datoscerca.domain.casouso.ObtenerDatoParaUbicacion
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.EstadoUbicacion
import com.daxlejo.datoscerca.domain.puerto.Narrador
import com.daxlejo.datoscerca.domain.puerto.ProveedorUbicacion
import com.daxlejo.datoscerca.domain.puerto.Reloj
import com.daxlejo.datoscerca.domain.puerto.RepositorioConfiguracion
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

sealed interface EstadoMotor {
    data object Iniciando : EstadoMotor
    data object EnPausa : EstadoMotor
    data class Bloqueado(val error: ErrorDato.ErrorUbicacion) : EstadoMotor

    /** [aviso] es el último fallo de un intento automático; no interrumpe la exploración. */
    data class Explorando(
        val coordenada: Coordenada,
        val cercaDe: String? = null,
        val aviso: ErrorDato? = null,
    ) : EstadoMotor
}

/**
 * Máquina de estados del dato automático: observa la ubicación y la pausa,
 * y pide un dato cada vez que la persona se desplaza lo suficiente.
 *
 * [nuevosDatos] no guarda datos atrasados: si nadie lo escucha (la persona está en otra pantalla),
 * el dato queda en el historial y no interrumpe lo que esté haciendo.
 */
class MotorAutomatico(
    private val ubicacion: ProveedorUbicacion,
    private val configuracion: RepositorioConfiguracion,
    private val obtenerDato: ObtenerDatoParaUbicacion,
    private val narrador: Narrador,
    private val reloj: Reloj,
    private val minDesplazamientoMetros: Double = MIN_DESPLAZAMIENTO_METROS,
    private val esperaReintentoMs: Long = ESPERA_REINTENTO_MS,
) {
    private val _estado = MutableStateFlow<EstadoMotor>(EstadoMotor.Iniciando)
    val estado: StateFlow<EstadoMotor> = _estado.asStateFlow()

    private val _nuevosDatos = MutableSharedFlow<EntradaHistorial>(extraBufferCapacity = 1)
    val nuevosDatos: SharedFlow<EntradaHistorial> = _nuevosDatos.asSharedFlow()

    private var ultimaEvaluada: Coordenada? = null

    // Si el último intento falló o cayó dentro del intervalo, se repite sin exigir desplazamiento.
    private var reintentarDesdeMs: Long? = null

    /** Suspende mientras el motor trabaja; quien lo llama decide el ciclo de vida. */
    suspend fun ejecutar() {
        val pausa = configuracion.observar().map { it.pausa }.distinctUntilChanged()
        combine(ubicacion.ubicaciones(), pausa, ::Pair)
            .conflate()
            .collect { (estadoUbicacion, enPausa) -> procesar(estadoUbicacion, enPausa) }
    }

    private suspend fun procesar(estadoUbicacion: EstadoUbicacion, enPausa: Boolean) {
        when {
            enPausa -> pausar()
            estadoUbicacion is EstadoUbicacion.Disponible -> explorar(estadoUbicacion.coordenada)
            estadoUbicacion is EstadoUbicacion.NoDisponible -> detener(EstadoMotor.Bloqueado(estadoUbicacion.motivo))
        }
    }

    private fun pausar() {
        narrador.detener()
        detener(EstadoMotor.EnPausa)
    }

    private fun detener(nuevoEstado: EstadoMotor) {
        ultimaEvaluada = null
        reintentarDesdeMs = null
        _estado.value = nuevoEstado
    }

    private suspend fun explorar(coordenada: Coordenada) {
        val previo = _estado.value as? EstadoMotor.Explorando
        _estado.value = EstadoMotor.Explorando(coordenada, previo?.cercaDe, previo?.aviso)
        if (!tocaEvaluar(coordenada)) return
        val exploracion = obtenerDato(coordenada)
        recordar(coordenada, exploracion)
        aplicar(coordenada, exploracion, previo?.cercaDe)
    }

    private fun tocaEvaluar(coordenada: Coordenada): Boolean {
        val anterior = ultimaEvaluada ?: return true
        val seDesplazo = anterior.distanciaMetrosA(coordenada) >= minDesplazamientoMetros
        val tocaReintentar = reintentarDesdeMs?.let { reloj.ahoraMs() >= it } ?: false
        return seDesplazo || tocaReintentar
    }

    // Se anota después de evaluar: si la petición se cancela a medias, el lugar no se pierde.
    private fun recordar(coordenada: Coordenada, exploracion: Exploracion) {
        ultimaEvaluada = coordenada
        reintentarDesdeMs = if (quedaPendiente(exploracion)) reloj.ahoraMs() + esperaReintentoMs else null
    }

    private fun quedaPendiente(exploracion: Exploracion): Boolean = when (exploracion) {
        is Exploracion.NuevoDato -> false
        is Exploracion.Omitido -> exploracion.motivo == MotivoOmision.INTERVALO_NO_CUMPLIDO
        is Exploracion.Fallo -> true
    }

    private fun aplicar(coordenada: Coordenada, exploracion: Exploracion, cercaDePrevio: String?) {
        _estado.value = when (exploracion) {
            is Exploracion.NuevoDato ->
                EstadoMotor.Explorando(coordenada, exploracion.entrada.dato.lugar.nombre)
            is Exploracion.Omitido ->
                EstadoMotor.Explorando(coordenada, exploracion.lugarCercano?.nombre)
            is Exploracion.Fallo ->
                EstadoMotor.Explorando(coordenada, cercaDePrevio, exploracion.error)
        }
        if (exploracion is Exploracion.NuevoDato) _nuevosDatos.tryEmit(exploracion.entrada)
    }

    companion object {
        const val MIN_DESPLAZAMIENTO_METROS = 25.0
        const val ESPERA_REINTENTO_MS = 30_000L
    }
}
