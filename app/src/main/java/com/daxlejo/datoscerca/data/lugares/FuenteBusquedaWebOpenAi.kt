package com.daxlejo.datoscerca.data.lugares

import android.util.Log
import com.daxlejo.datoscerca.data.red.ClienteHttp
import com.daxlejo.datoscerca.data.red.RespuestaHttp
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.Relevancia
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tercer eslabón de la cadena: consulta la API de Responses de OpenAI con la herramienta de
 * búsqueda web únicamente si [habilitada] (`BuildConfig.OPENAI_WEB_SEARCH`) es `true`.
 * Por defecto permanece desactivada para cuidar el presupuesto.
 */
class FuenteBusquedaWebOpenAi(
    private val clienteHttp: ClienteHttp,
    private val apiKey: String,
    private val modelo: String,
    private val habilitada: Boolean,
    private val json: Json = FuenteWikipedia.jsonPorDefecto,
) : FuenteLugares {

    override suspend fun buscarCercanos(coordenada: Coordenada, radioMetros: Int): Resultado<List<Lugar>> {
        if (!habilitada || apiKey.isBlank() || modelo.isBlank()) {
            return Resultado.Exito(emptyList())
        }
        val cuerpo = json.encodeToString(
            PeticionBusquedaWeb(
                model = modelo,
                input = mensajeBusqueda(coordenada, radioMetros),
                maxOutputTokens = MAX_TOKENS_BUSQUEDA,
                tools = listOf(HerramientaWeb(type = "web_search_preview")),
                store = false,
            ),
        )
        val cabeceras = mapOf("Authorization" to "Bearer $apiKey")
        return when (val respuesta = clienteHttp.postJson(URL_RESPONSES, cuerpo, cabeceras)) {
            RespuestaHttp.SinConexion -> Resultado.Fallo(ErrorDato.SinInternet)
            is RespuestaHttp.ErrorServidor ->
                Resultado.Fallo(ErrorDato.FuenteNoDisponible("OpenAI web search HTTP ${respuesta.codigo}"))
            is RespuestaHttp.FalloTransporte ->
                Resultado.Fallo(ErrorDato.FuenteNoDisponible(respuesta.detalle))
            is RespuestaHttp.Exito -> interpretarRespuesta(coordenada, respuesta.cuerpo)
        }
    }

    private fun interpretarRespuesta(origen: Coordenada, cuerpoJson: String): Resultado<List<Lugar>> {
        val texto = try {
            val respuesta = json.decodeFromString<RespuestaOpenAiWeb>(cuerpoJson)
            respuesta.output
                .flatMap { it.content.orEmpty() }
                .firstOrNull { !it.text.isNullOrBlank() }
                ?.text?.trim().orEmpty()
        } catch (e: SerializationException) {
            Log.w(ETIQUETA, "JSON inválido en búsqueda web OpenAI", e)
            return Resultado.Fallo(ErrorDato.FuenteNoDisponible("Respuesta inválida de búsqueda web"))
        }
        if (texto.isBlank() || texto.startsWith(SIN_SITIO, ignoreCase = true)) {
            return Resultado.Exito(emptyList())
        }
        val lineas = texto.lines().map(String::trim).filter(String::isNotEmpty)
        val nombre = lineas.firstOrNull()?.removePrefix("LUGAR:")?.trim()?.takeIf { it.isNotEmpty() }
            ?: return Resultado.Exito(emptyList())
        val contexto = lineas.drop(1).joinToString(" ").ifBlank { nombre }
        val id = "openai-web:${nombre.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-")}"
        val lugar = Lugar(
            id = id,
            nombre = nombre,
            coordenada = origen,
            distanciaMetros = 0,
            contexto = contexto,
            fuente = "openai-web",
            relevancia = Relevancia.ARTICULO_WIKIPEDIA,
        )
        return Resultado.Exito(listOf(lugar))
    }

    private fun mensajeBusqueda(coordenada: Coordenada, radioMetros: Int): String {
        val lat = String.format(Locale.US, "%.5f", coordenada.latitud)
        val lon = String.format(Locale.US, "%.5f", coordenada.longitud)
        return "Busca si existe un monumento, iglesia, museo, plaza o sitio histórico reconocido " +
            "a menos de $radioMetros metros de ($lat, $lon). " +
            "Si no hay ninguno, responde solo $SIN_SITIO. " +
            "Si lo hay, escribe en la primera línea 'LUGAR: <nombre>' y en las siguientes un resumen factual breve en español."
    }

    @Serializable
    private data class PeticionBusquedaWeb(
        val model: String,
        val input: String,
        @SerialName("max_output_tokens") val maxOutputTokens: Int,
        val tools: List<HerramientaWeb>,
        val store: Boolean,
    )

    @Serializable
    private data class HerramientaWeb(val type: String)

    @Serializable
    private data class RespuestaOpenAiWeb(val output: List<ItemSalidaWeb> = emptyList())

    @Serializable
    private data class ItemSalidaWeb(val content: List<ContenidoTextoWeb>? = null)

    @Serializable
    private data class ContenidoTextoWeb(val text: String? = null)

    companion object {
        private const val ETIQUETA = "FuenteBusquedaWeb"
        private const val URL_RESPONSES = "https://api.openai.com/v1/responses"
        private const val MAX_TOKENS_BUSQUEDA = 180
        private const val SIN_SITIO = "NINGUNO"
    }
}
