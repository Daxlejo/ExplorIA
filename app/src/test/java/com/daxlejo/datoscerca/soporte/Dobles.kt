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
import com.daxlejo.datoscerca.domain.modelo.OrigenDato
import com.daxlejo.datoscerca.domain.modelo.Relevancia
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
    relevancia: Relevancia = Relevancia.ARTICULO_WIKIPEDIA,
    distanciaMetros: Int = 0,
) = Lugar(id, nombre, coordenada, distanciaMetros, contexto, fuente = "prueba", relevancia = relevancia)

fun dato(
    lugar: Lugar = lugar(),
    generadoEnMs: Long = 0,
    tipo: TipoDato = TipoDato.INTERESANTE,
    origen: OrigenDato = OrigenDato.AUTOMATICO,
) = Dato("Un dato de prueba.", tipo, Intensidad.NORMAL, lugar, generadoEnMs, origen)

fun <T> Resultado<T>.valorExitoso(): T = (this as Resultado.Exito).valor

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

    /** Simula una lectura de la base que falla: los flujos emiten Fallo en lugar de datos. */
    var falloLectura: ErrorDato? = null

    val actuales: List<EntradaHistorial> get() = entradas.value

    override fun observar(): Flow<Resultado<List<EntradaHistorial>>> =
        falloLectura?.let { flowOf(Resultado.Fallo(it)) } ?: entradas.map { Resultado.Exito(it) }

    override fun observarEntrada(id: Long): Flow<Resultado<EntradaHistorial?>> =
        falloLectura?.let { flowOf(Resultado.Fallo(it)) }
            ?: entradas.map { lista -> Resultado.Exito(lista.firstOrNull { it.id == id }) }

    override suspend fun recientes(limite: Int): Resultado<List<EntradaHistorial>> =
        intentar { entradas.value.take(limite) }

    override suspend fun contarAutomaticosDesde(desdeMs: Long): Resultado<Int> = intentar {
        entradas.value.count { it.dato.origen == OrigenDato.AUTOMATICO && it.dato.generadoEnMs >= desdeMs }
    }

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

/** Devuelve los lugares dentro del radio, del más cercano al más lejano, y anota el radio de cada búsqueda. */
class FuenteLugaresFalsa(private val lugares: List<Lugar> = listOf(lugar())) : FuenteLugares {
    var fallo: ErrorDato? = null
    val busquedas = mutableListOf<Int>()

    override suspend fun buscarCercanos(coordenada: Coordenada, radioMetros: Int): Resultado<List<Lugar>> {
        busquedas += radioMetros
        fallo?.let { return Resultado.Fallo(it) }
        return Resultado.Exito(
            lugares
                .map { it.copy(distanciaMetros = coordenada.distanciaMetrosA(it.coordenada).roundToInt()) }
                .filter { it.distanciaMetros <= radioMetros }
                .sortedBy { it.distanciaMetros },
        )
    }
}

/** Si [retener] no es null, cada generación espera a que se complete: sirve para probar la generación única. */
class GeneradorFalso(var respuesta: Resultado<String> = Resultado.Exito("Dato generado.")) : GeneradorDatos {
    val solicitudes = mutableListOf<SolicitudGeneracion>()
    var retener: CompletableDeferred<Unit>? = null

    override suspend fun generar(solicitud: SolicitudGeneracion): Resultado<String> {
        solicitudes += solicitud
        retener?.await()
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
