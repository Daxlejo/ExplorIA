package com.daxlejo.datoscerca.data.ia

import android.util.Log
import com.daxlejo.datoscerca.data.lugares.FuenteWikipedia
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.puerto.GeneradorDatos
import com.daxlejo.datoscerca.domain.puerto.SolicitudGeneracion
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Caché de datos generados por clave (`lugar.id|tipo|intensidad`), con hasta [MAX_VARIANTES]
 * variantes distintas por clave.
 *
 * Mantiene las entradas en memoria y, si se le entrega un [archivoPersistencia], las sincroniza
 * en disco. Es retrocompatible al leer el formato antiguo de un solo string por clave (`Map<String, String>`)
 * para que la precarga del modo demo y los archivos previos sigan funcionando.
 */
class CacheDatos(
    private val archivoPersistencia: File? = null,
    private val json: Json = FuenteWikipedia.jsonPorDefecto,
) {
    private val entradas = ConcurrentHashMap<String, List<String>>()

    init {
        cargarDesdeArchivo()
    }

    /** Devuelve la primera variante disponible para la clave (retrocompatible). */
    fun obtener(clave: String): String? = entradas[clave]?.firstOrNull()

    /** Devuelve todas las variantes almacenadas para la clave (máximo [MAX_VARIANTES]). */
    fun obtenerVariantes(clave: String): List<String> = entradas[clave].orEmpty()

    /** Devuelve la primera variante en caché que no esté en [excluidos]. */
    fun obtenerVarianteNoMostrada(clave: String, excluidos: Collection<String>): String? {
        val variantes = entradas[clave] ?: return null
        if (excluidos.isEmpty()) return variantes.firstOrNull()
        val conjuntoExcluidos = excluidos.map(String::trim).toSet()
        return variantes.firstOrNull { it !in conjuntoExcluidos }
    }

    fun guardar(clave: String, texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || GuardiaSalidaIa.contieneMetacomentarios(limpio)) return
        entradas.compute(clave) { _, actuales ->
            val base = actuales.orEmpty()
            if (limpio in base || base.size >= MAX_VARIANTES) {
                base
            } else {
                base + limpio
            }
        }
        persistirEnArchivo()
    }

    fun guardarLote(lote: Map<String, String>) {
        lote.forEach { (clave, texto) ->
            val limpio = texto.trim()
            if (limpio.isNotEmpty() && !GuardiaSalidaIa.contieneMetacomentarios(limpio)) {
                val previas = entradas[clave].orEmpty()
                if (limpio !in previas && previas.size < MAX_VARIANTES) {
                    entradas[clave] = previas + limpio
                }
            }
        }
        persistirEnArchivo()
    }

    fun tamano(): Int = entradas.size

    private fun cargarDesdeArchivo() {
        val archivo = archivoPersistencia?.takeIf(File::exists) ?: return
        try {
            val texto = archivo.readText()
            if (texto.isBlank()) return
            val raiz = json.parseToJsonElement(texto) as? JsonObject ?: return
            var huboDescartadas = false
            for ((clave, elemento) in raiz) {
                val brutas = extraerTextosBrutos(elemento)
                val limpias = brutas
                    .map(String::trim)
                    .filter { it.isNotEmpty() && !GuardiaSalidaIa.contieneMetacomentarios(it) }
                    .distinct()
                    .take(MAX_VARIANTES)
                if (limpias.size < brutas.count { it.isNotBlank() }) {
                    huboDescartadas = true
                }
                if (limpias.isNotEmpty()) {
                    entradas[clave] = limpias
                }
            }
            if (huboDescartadas) {
                persistirEnArchivo()
            }
        } catch (e: IOException) {
            Log.w(ETIQUETA, "No se pudo leer el archivo de caché", e)
        } catch (e: SerializationException) {
            Log.w(ETIQUETA, "Caché en disco corrupta; se ignora", e)
        } catch (e: IllegalArgumentException) {
            Log.w(ETIQUETA, "Formato JSON inesperado en caché; se ignora", e)
        }
    }

    private fun extraerTextosBrutos(elemento: JsonElement): List<String> = when (elemento) {
        is JsonPrimitive -> if (elemento.isString) listOf(elemento.content) else emptyList()
        is JsonArray -> elemento.mapNotNull { item ->
            (item as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content
        }
        else -> emptyList()
    }

    private fun persistirEnArchivo() {
        val archivo = archivoPersistencia ?: return
        try {
            archivo.parentFile?.mkdirs()
            archivo.writeText(json.encodeToString(entradas.toMap()))
        } catch (e: IOException) {
            Log.w(ETIQUETA, "No se pudo persistir la caché en disco", e)
        }
    }

    companion object {
        private const val ETIQUETA = "CacheDatos"
        const val MAX_VARIANTES = SolicitudGeneracion.MAX_VARIANTES_POR_CLAVE
    }
}

/**
 * Decorator sobre [GeneradorDatos]: sirve una variante desde [CacheDatos] que no se haya mostrado
 * recientemente. Solo invoca a OpenAI cuando todas las variantes en caché ya se mostraron y aún
 * hay cupo (< [CacheDatos.MAX_VARIANTES] variantes por clave).
 */
class GeneradorDatosConCache(
    private val delegado: GeneradorDatos,
    private val cache: CacheDatos,
) : GeneradorDatos {

    override suspend fun generar(solicitud: SolicitudGeneracion): Resultado<String> {
        val clave = solicitud.claveCache
        val excluidos = solicitud.textosExcluidos.map(String::trim).filter(String::isNotEmpty)

        cache.obtenerVarianteNoMostrada(clave, excluidos)?.let { noMostrada ->
            return Resultado.Exito(noMostrada)
        }

        val variantesActuales = cache.obtenerVariantes(clave)
        if (variantesActuales.size >= CacheDatos.MAX_VARIANTES) {
            val menosReciente = elegirMenosReciente(variantesActuales, excluidos)
            return Resultado.Exito(menosReciente)
        }

        return when (val resultado = delegado.generar(solicitud)) {
            is Resultado.Exito -> {
                val limpio = resultado.valor.trim()
                if (GuardiaSalidaIa.contieneMetacomentarios(limpio)) {
                    return Resultado.Fallo(ErrorDato.ErrorIa("Respuesta con metacomentarios rechazada"))
                }
                if (limpio.isNotEmpty()) cache.guardar(clave, limpio)
                Resultado.Exito(limpio)
            }
            is Resultado.Fallo -> resultado
        }
    }

    // `excluidos` viene ordenado del más reciente (índice 0) al más antiguo: se elige el de mayor índice.
    private fun elegirMenosReciente(variantes: List<String>, excluidos: List<String>): String =
        variantes.maxByOrNull { excluidos.indexOf(it) } ?: variantes.first()
}
