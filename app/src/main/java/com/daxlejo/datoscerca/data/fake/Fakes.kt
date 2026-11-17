package com.daxlejo.datoscerca.data.fake

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.EstadoUbicacion
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import com.daxlejo.datoscerca.domain.puerto.GeneradorDatos
import com.daxlejo.datoscerca.domain.puerto.ProveedorUbicacion
import com.daxlejo.datoscerca.domain.puerto.SolicitudGeneracion
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** Situación que simulan los fakes, para poder ver cada estado de la interfaz sin red ni GPS. */
enum class EscenarioFake { NORMAL, SIN_PERMISO, SIN_GPS, SIN_INTERNET, SIN_LUGARES, ERROR_IA }

/** Ubicación simulada: camina en bucle por el centro de Pasto. */
class FakeProveedorUbicacion(
    private val escenario: StateFlow<EscenarioFake>,
    private val intervaloMs: Long = 6_000,
) : ProveedorUbicacion {
    private val ruta = DatosPasto.ruta()

    @Volatile
    private var indice = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun ubicaciones(): Flow<EstadoUbicacion> = escenario.flatMapLatest { actual ->
        when (actual) {
            EscenarioFake.SIN_PERMISO -> flowOf(EstadoUbicacion.NoDisponible(ErrorDato.SinPermiso))
            EscenarioFake.SIN_GPS -> flowOf(EstadoUbicacion.NoDisponible(ErrorDato.SinGps))
            else -> caminata()
        }
    }

    private fun caminata(): Flow<EstadoUbicacion> = flow {
        while (true) {
            emit(EstadoUbicacion.Disponible(ruta[indice]))
            delay(intervaloMs)
            indice = (indice + 1) % ruta.size
        }
    }

    override suspend fun ubicacionActual(): Resultado<Coordenada> = when (escenario.value) {
        EscenarioFake.SIN_PERMISO -> Resultado.Fallo(ErrorDato.SinPermiso)
        EscenarioFake.SIN_GPS -> Resultado.Fallo(ErrorDato.SinGps)
        else -> Resultado.Exito(ruta[indice])
    }
}

/** Lugares simulados: el más cercano de [DatosPasto] dentro del radio pedido. */
class FakeFuenteLugares(
    private val escenario: StateFlow<EscenarioFake>,
    private val demoraMs: Long = 300,
) : FuenteLugares {

    override suspend fun buscarCercano(
        coordenada: Coordenada,
        radioMetros: Int,
        excluir: Set<String>,
    ): Resultado<Lugar> {
        delay(demoraMs)
        return when (escenario.value) {
            EscenarioFake.SIN_INTERNET -> Resultado.Fallo(ErrorDato.SinInternet)
            EscenarioFake.SIN_LUGARES -> Resultado.Fallo(ErrorDato.SinLugares)
            else -> masCercano(coordenada, radioMetros, excluir)
        }
    }

    private fun masCercano(coordenada: Coordenada, radioMetros: Int, excluir: Set<String>): Resultado<Lugar> {
        val elegido = DatosPasto.lugares
            .filter { it.id !in excluir }
            .map { it to coordenada.distanciaMetrosA(it.coordenada) }
            .filter { (_, distancia) -> distancia <= radioMetros }
            .minByOrNull { (_, distancia) -> distancia }
            ?: return Resultado.Fallo(ErrorDato.SinLugares)
        val (muestra, distancia) = elegido
        return Resultado.Exito(
            Lugar(muestra.id, muestra.nombre, muestra.coordenada, distancia.roundToInt(), muestra.contexto, DatosPasto.FUENTE),
        )
    }
}

/** IA simulada: arma el dato con frases de muestra; más frases cuanto mayor es la intensidad. */
class FakeGeneradorDatos(
    private val escenario: StateFlow<EscenarioFake>,
    private val demoraMs: Long = 1_200,
) : GeneradorDatos {

    override suspend fun generar(solicitud: SolicitudGeneracion): Resultado<String> {
        delay(demoraMs)
        return when (escenario.value) {
            EscenarioFake.SIN_INTERNET -> Resultado.Fallo(ErrorDato.SinInternet)
            EscenarioFake.ERROR_IA -> Resultado.Fallo(ErrorDato.ErrorIa("Fallo simulado de la IA"))
            else -> redactar(solicitud)
        }
    }

    private fun redactar(solicitud: SolicitudGeneracion): Resultado<String> {
        val frases = DatosPasto.lugares
            .firstOrNull { it.id == solicitud.lugar.id }
            ?.frases?.get(solicitud.tipo)
            ?: return Resultado.Fallo(ErrorDato.ErrorIa("Sin datos de muestra para ${solicitud.lugar.nombre}"))
        return Resultado.Exito(frases.take(cantidadFrases(solicitud.intensidad)).joinToString(" "))
    }

    private fun cantidadFrases(intensidad: Intensidad): Int = when (intensidad) {
        Intensidad.SUAVE -> 1
        Intensidad.NORMAL -> 2
        Intensidad.INTENSO -> 3
    }
}
