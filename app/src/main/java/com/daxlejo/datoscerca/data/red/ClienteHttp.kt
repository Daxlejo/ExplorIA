package com.daxlejo.datoscerca.data.red

import android.util.Log
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Resultado de una llamada HTTP antes de traducirlo al error propio de cada puerto. */
sealed interface RespuestaHttp {
    data class Exito(val cuerpo: String) : RespuestaHttp
    data class ErrorServidor(val codigo: Int, val detalle: String?) : RespuestaHttp
    data object SinConexion : RespuestaHttp
    data class FalloTransporte(val detalle: String?) : RespuestaHttp
}

/** Puerto interno de red para desacoplar las fuentes y la IA de OkHttp en los tests. */
interface ClienteHttp {
    suspend fun get(url: String, cabeceras: Map<String, String> = emptyMap()): RespuestaHttp
    suspend fun postJson(
        url: String,
        cuerpoJson: String,
        cabeceras: Map<String, String> = emptyMap(),
    ): RespuestaHttp
}

/**
 * Cliente HTTP real con OkHttp. Envía siempre un User-Agent descriptivo que cumple la política
 * de uso de Wikimedia y de OpenStreetMap Nominatim.
 */
class ClienteHttpOkHttp(
    private val cliente: OkHttpClient = crearOkHttpPorDefecto(),
    private val despachador: CoroutineDispatcher = Dispatchers.IO,
) : ClienteHttp {

    override suspend fun get(url: String, cabeceras: Map<String, String>): RespuestaHttp =
        ejecutar(
            Request.Builder()
                .url(url)
                .header(CABECERA_USER_AGENT, USER_AGENT_DESCRIPTIVO)
                .header(CABECERA_ACCEPT, MIME_JSON)
                .aplicarCabeceras(cabeceras)
                .get()
                .build(),
        )

    override suspend fun postJson(
        url: String,
        cuerpoJson: String,
        cabeceras: Map<String, String>,
    ): RespuestaHttp = ejecutar(
        Request.Builder()
            .url(url)
            .header(CABECERA_USER_AGENT, USER_AGENT_DESCRIPTIVO)
            .header(CABECERA_ACCEPT, MIME_JSON)
            .aplicarCabeceras(cabeceras)
            .post(cuerpoJson.toRequestBody(MEDIA_JSON))
            .build(),
    )

    private suspend fun ejecutar(peticion: Request): RespuestaHttp = withContext(despachador) {
        try {
            cliente.newCall(peticion).execute().use { respuesta ->
                val cuerpo = respuesta.body.string()
                if (respuesta.isSuccessful) {
                    RespuestaHttp.Exito(cuerpo)
                } else {
                    Log.w(ETIQUETA, "HTTP ${respuesta.code} en ${peticion.url.host}")
                    RespuestaHttp.ErrorServidor(respuesta.code, cuerpo.take(MAX_DETALLE_ERROR))
                }
            }
        } catch (e: UnknownHostException) {
            Log.w(ETIQUETA, "Sin resolución DNS para ${peticion.url.host}", e)
            RespuestaHttp.SinConexion
        } catch (e: ConnectException) {
            Log.w(ETIQUETA, "Sin conexión hacia ${peticion.url.host}", e)
            RespuestaHttp.SinConexion
        } catch (e: NoRouteToHostException) {
            Log.w(ETIQUETA, "Sin ruta hacia ${peticion.url.host}", e)
            RespuestaHttp.SinConexion
        } catch (e: SocketTimeoutException) {
            Log.w(ETIQUETA, "Tiempo agotado en ${peticion.url.host}", e)
            RespuestaHttp.FalloTransporte("Tiempo de espera agotado")
        } catch (e: IOException) {
            Log.w(ETIQUETA, "Fallo de red en ${peticion.url.host}", e)
            RespuestaHttp.FalloTransporte(e.message)
        }
    }

    private fun Request.Builder.aplicarCabeceras(cabeceras: Map<String, String>): Request.Builder = apply {
        cabeceras.forEach { (nombre, valor) -> header(nombre, valor) }
    }

    companion object {
        private const val ETIQUETA = "ClienteHttp"
        private const val CABECERA_USER_AGENT = "User-Agent"
        private const val CABECERA_ACCEPT = "Accept"
        private const val MIME_JSON = "application/json"
        private const val MAX_DETALLE_ERROR = 240
        private val MEDIA_JSON = "application/json; charset=utf-8".toMediaType()

        const val USER_AGENT_DESCRIPTIVO =
            "DatosCercaWearOS/1.0 (https://github.com/Daxlejo/SmartWatch; contacto: daxlejo@udenar.edu.co)"

        fun crearOkHttpPorDefecto(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
