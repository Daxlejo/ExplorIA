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

/**
 * Caché de datos generados por clave (`lugar.id|tipo|intensidad`).
 * Mantiene las entradas en memoria y, si se le entrega un [archivoPersistencia], las sincroniza
 * en disco para conservarlas al cerrar la app y ahorrar llamadas a OpenAI.
 */
class CacheDatos(
    private val archivoPersistencia: File? = null,
    private val json: Json = FuenteWikipedia.jsonPorDefecto,
) {
    private val entradas = ConcurrentHashMap<String, String>()

    init {
        cargarDesdeArchivo()
    }

    fun obtener(clave: String): String? = entradas[clave]

    fun guardar(clave: String, texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || GuardiaSalidaIa.contieneMetacomentarios(limpio)) return
        entradas[clave] = limpio
        persistirEnArchivo()
    }

    fun guardarLote(lote: Map<String, String>) {
        lote.forEach { (clave, texto) ->
            val limpio = texto.trim()
            if (limpio.isNotEmpty() && !GuardiaSalidaIa.contieneMetacomentarios(limpio)) {
                entradas[clave] = limpio
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
            val mapa = json.decodeFromString<Map<String, String>>(texto)
            entradas.putAll(mapa)
        } catch (e: IOException) {
            Log.w(ETIQUETA, "No se pudo leer el archivo de caché", e)
        } catch (e: SerializationException) {
            Log.w(ETIQUETA, "Caché en disco corrupta; se ignora", e)
        }
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

    private companion object {
        const val ETIQUETA = "CacheDatos"
    }
}

/**
 * Decorator sobre [GeneradorDatos]: devuelve el dato desde [CacheDatos] si ya existe para la
 * combinación `lugar + tipo + intensidad`, sin invocar a OpenAI. Si no existe, delega en el
 * generador real y almacena el resultado exitoso.
 */
class GeneradorDatosConCache(
    private val delegado: GeneradorDatos,
    private val cache: CacheDatos,
) : GeneradorDatos {

    override suspend fun generar(solicitud: SolicitudGeneracion): Resultado<String> {
        val clave = solicitud.claveCache
        cache.obtener(clave)?.let { enCache ->
            return Resultado.Exito(enCache)
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
}
