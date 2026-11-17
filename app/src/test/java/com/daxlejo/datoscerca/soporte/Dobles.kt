package com.daxlejo.datoscerca.soporte

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Dato
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.EstadoUbicacion
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import com.daxlejo.datoscerca.domain.puerto.GeneradorDatos
import com.daxlejo.datoscerca.domain.puerto.Narrador
import com.daxlejo.datoscerca.domain.puerto.ProveedorUbicacion
import com.daxlejo.datoscerca.domain.puerto.Reloj
import com.daxlejo.datoscerca.domain.puerto.RepositorioConfiguracion
import com.daxlejo.datoscerca.domain.puerto.RepositorioHistorial
import com.daxlejo.datoscerca.domain.puerto.SolicitudGeneracion
import com.daxlejo.datoscerca.domain.puerto.Vibrador
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

const val MINUTO_MS = 60_000L

val PLAZA = Coordenada(1.21464, -77.27825)

fun lugar(
    id: String = "prueba:plaza",
    nombre: String = "Plaza de Nariño",
    coordenada: Coordenada = PLAZA,
    contexto: String = "La plaza principal de Pasto.",
) = Lugar(id, nombre, coordenada, distanciaMetros = 0, contexto = contexto, fuente = "prueba")

fun dato(
    lugar: Lugar = lugar(),
    generadoEnMs: Long = 0,
    tipo: TipoDato = TipoDato.INTERESANTE,
) = Dato("Un dato de prueba.", tipo, Intensidad.NORMAL, lugar, generadoEnMs)

class RelojFalso(var ahora: Long = 0) : Reloj {
    override fun ahoraMs(): Long = ahora
    fun avanzarMinutos(minutos: Int) {
        ahora += minutos * MINUTO_MS
    }
}

class RepositorioHistorialEnMemoria : RepositorioHistorial {
    private val entradas = MutableStateFlow<List<EntradaHistorial>>(emptyList())
    private var siguienteId = 1L
    var fallo: ErrorDato? = null

    val actuales: List<EntradaHistorial> get() = entradas.value

    override fun observar(): Flow<List<EntradaHistorial>> = entradas

    override fun observarEntrada(id: Long): Flow<EntradaHistorial?> =
        entradas.map { lista -> lista.firstOrNull { it.id == id } }

    override suspend fun recientes(limite: Int): Resultado<List<EntradaHistorial>> =
        intentar { entradas.value.take(limite) }

    override suspend fun guardar(dato: Dato): Resultado<EntradaHistorial> = intentar {
        EntradaHistorial(siguienteId++, dato, favorito = false).also { nueva -> entradas.update { listOf(nueva) + it } }
    }

    override suspend fun alternarFavorito(id: Long): Resultado<Unit> = intentar {
        entradas.update { lista -> lista.map { if (it.id == id) it.copy(favorito = !it.favorito) else it } }
    }

    override suspend fun borrar(id: Long): Resultado<Unit> = intentar {
        entradas.update { lista -> lista.filterNot { it.id == id } }
    }

    private inline fun <T> intentar(accion: () -> T): Resultado<T> =
        fallo?.let { Resultado.Fallo(it) } ?: Resultado.Exito(accion())
}

class RepositorioConfiguracionEnMemoria(
    inicial: Configuracion = Configuracion.PorDefecto,
) : RepositorioConfiguracion {
    private val estado = MutableStateFlow(inicial)
    var fallo: ErrorDato? = null

    val actual: Configuracion get() = estado.value

    override fun observar(): Flow<Configuracion> = estado

    override suspend fun actualizar(cambio: (Configuracion) -> Configuracion): Resultado<Unit> {
        fallo?.let { return Resultado.Fallo(it) }
        estado.update(cambio)
        return Resultado.Exito(Unit)
    }
}

/** Devuelve el lugar más cercano dentro del radio, igual que una fuente real, y anota cada búsqueda. */
class FuenteLugaresFalsa(private val lugares: List<Lugar> = listOf(lugar())) : FuenteLugares {
    var fallo: ErrorDato? = null
    val busquedas = mutableListOf<Busqueda>()

    data class Busqueda(val radioMetros: Int, val excluir: Set<String>)

    override suspend fun buscarCercano(coordenada: Coordenada, radioMetros: Int, excluir: Set<String>): Resultado<Lugar> {
        busquedas += Busqueda(radioMetros, excluir)
        fallo?.let { return Resultado.Fallo(it) }
        val cercano = lugares
            .filter { it.id !in excluir }
            .map { it.copy(distanciaMetros = coordenada.distanciaMetrosA(it.coordenada).roundToInt()) }
            .filter { it.distanciaMetros <= radioMetros }
            .minByOrNull { it.distanciaMetros }
        return cercano?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ErrorDato.SinLugares)
    }
}

class GeneradorFalso(var respuesta: Resultado<String> = Resultado.Exito("Dato generado.")) : GeneradorDatos {
    val solicitudes = mutableListOf<SolicitudGeneracion>()

    override suspend fun generar(solicitud: SolicitudGeneracion): Resultado<String> {
        solicitudes += solicitud
        return respuesta
    }
}

class UbicacionFalsa(
    var actual: Resultado<Coordenada> = Resultado.Exito(PLAZA),
    val flujo: MutableStateFlow<EstadoUbicacion> = MutableStateFlow(EstadoUbicacion.Disponible(PLAZA)),
) : ProveedorUbicacion {
    override fun ubicaciones(): Flow<EstadoUbicacion> = flujo
    override suspend fun ubicacionActual(): Resultado<Coordenada> = actual
}

class VibradorEspia : Vibrador {
    var vibraciones = 0
    override fun vibrarNuevoDato() {
        vibraciones++
    }
}

class NarradorEspia : Narrador {
    val leidos = mutableListOf<String>()
    var detenciones = 0

    override fun hablar(texto: String): Resultado<Unit> {
        leidos += texto
        return Resultado.Exito(Unit)
    }

    override fun detener() {
        detenciones++
    }
}
