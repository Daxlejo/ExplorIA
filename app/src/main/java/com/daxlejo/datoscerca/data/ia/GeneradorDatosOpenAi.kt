package com.daxlejo.datoscerca.data.ia

import android.util.Log
import com.daxlejo.datoscerca.data.lugares.FuenteWikipedia
import com.daxlejo.datoscerca.data.red.ClienteHttp
import com.daxlejo.datoscerca.data.red.RespuestaHttp
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.puerto.GeneradorDatos
import com.daxlejo.datoscerca.domain.puerto.SolicitudGeneracion
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementación real de [GeneradorDatos] contra la API Responses de OpenAI (`/v1/responses`).
 * Usa el modelo configurado en `BuildConfig.OPENAI_MODEL` y el prompt armado por `PromptBuilder`,
 * respetando `max_output_tokens` según la intensidad y desactivando tokens de razonamiento en
 * modelos `gpt-5`/`gpt-6` para cuidar el presupuesto.
 */
class GeneradorDatosOpenAi(
    private val clienteHttp: ClienteHttp,
    private val apiKey: String,
    private val modelo: String,
    private val json: Json = FuenteWikipedia.jsonPorDefecto,
) : GeneradorDatos {

    override suspend fun generar(solicitud: SolicitudGeneracion): Resultado<String> {
        if (apiKey.isBlank()) {
            Log.e(ETIQUETA, "OPENAI_API_KEY vacía en BuildConfig")
            return Resultado.Fallo(ErrorDato.ErrorIa("Falta configurar OPENAI_API_KEY"))
        }
        if (modelo.isBlank()) {
            Log.e(ETIQUETA, "OPENAI_MODEL vacío en BuildConfig")
            return Resultado.Fallo(ErrorDato.ErrorIa("Falta configurar OPENAI_MODEL"))
        }
        val prompt = solicitud.prompt
        val peticion = PeticionResponses(
            model = modelo,
            instructions = prompt.sistema,
            input = prompt.usuario,
            maxOutputTokens = prompt.maxTokensSalida,
            reasoning = ConfigRazonamiento(effort = "none").takeIf { admiteSinRazonamiento(modelo) },
            store = false,
        )
        val cuerpoJson = json.encodeToString(peticion)
        val cabeceras = mapOf("Authorization" to "Bearer $apiKey")

        return when (val respuesta = clienteHttp.postJson(URL_RESPONSES, cuerpoJson, cabeceras)) {
            RespuestaHttp.SinConexion -> Resultado.Fallo(ErrorDato.SinInternet)
            is RespuestaHttp.ErrorServidor -> {
                Log.w(ETIQUETA, "Error HTTP ${respuesta.codigo} de OpenAI: ${respuesta.detalle}")
                Resultado.Fallo(ErrorDato.ErrorIa("OpenAI HTTP ${respuesta.codigo}"))
            }
            is RespuestaHttp.FalloTransporte -> {
                Log.w(ETIQUETA, "Fallo de transporte con OpenAI: ${respuesta.detalle}")
                Resultado.Fallo(ErrorDato.ErrorIa(respuesta.detalle ?: "Error de red con OpenAI"))
            }
            is RespuestaHttp.Exito -> extraerTexto(respuesta.cuerpo)
        }
    }

    private fun extraerTexto(cuerpoJson: String): Resultado<String> {
        val respuesta = try {
            json.decodeFromString<RespuestaResponses>(cuerpoJson)
        } catch (e: SerializationException) {
            Log.w(ETIQUETA, "JSON inválido de OpenAI", e)
            return Resultado.Fallo(ErrorDato.ErrorIa("Respuesta inválida de OpenAI"))
        }
        if (respuesta.error != null) {
            val mensaje = respuesta.error.message ?: "Error de OpenAI"
            Log.w(ETIQUETA, "OpenAI devolvió error: $mensaje")
            return Resultado.Fallo(ErrorDato.ErrorIa(mensaje))
        }
        val directo = respuesta.outputText?.trim()?.takeIf { it.isNotEmpty() }
        val desdeItems = respuesta.output
            .flatMap { it.content.orEmpty() }
            .mapNotNull { it.text?.trim() }
            .firstOrNull { it.isNotEmpty() }
        val texto = directo ?: desdeItems
        return if (!texto.isNullOrBlank()) {
            Resultado.Exito(texto)
        } else {
            Log.w(ETIQUETA, "OpenAI respondió sin texto útil (status=${respuesta.status})")
            Resultado.Fallo(ErrorDato.ErrorIa("La IA respondió vacío"))
        }
    }

    private fun admiteSinRazonamiento(nombreModelo: String): Boolean {
        val limpio = nombreModelo.lowercase(Locale.ROOT).trim()
        return limpio.startsWith("gpt-5") || limpio.startsWith("gpt-6")
    }

    @Serializable
    private data class PeticionResponses(
        val model: String,
        val instructions: String,
        val input: String,
        @SerialName("max_output_tokens") val maxOutputTokens: Int,
        val reasoning: ConfigRazonamiento? = null,
        val store: Boolean = false,
    )

    @Serializable
    private data class ConfigRazonamiento(val effort: String)

    @Serializable
    private data class RespuestaResponses(
        val status: String? = null,
        @SerialName("output_text") val outputText: String? = null,
        val output: List<ItemSalida> = emptyList(),
        val error: ErrorApiOpenAi? = null,
    )

    @Serializable
    private data class ItemSalida(
        val type: String? = null,
        val content: List<ContenidoSalida>? = null,
    )

    @Serializable
    private data class ContenidoSalida(
        val type: String? = null,
        val text: String? = null,
    )

    @Serializable
    private data class ErrorApiOpenAi(
        val message: String? = null,
        val code: String? = null,
    )

    companion object {
        private const val ETIQUETA = "GeneradorOpenAi"
        const val URL_RESPONSES = "https://api.openai.com/v1/responses"
    }
}
