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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Eslabón de la cadena de lugares que consulta MediaWiki GeoSearch (`action=query&list=geosearch`)
 * en el idioma indicado (`es` o `en`) y obtiene el resumen en texto plano de los artículos cercanos.
 */
class FuenteWikipedia(
    private val idioma: String,
    private val clienteHttp: ClienteHttp,
    private val json: Json = jsonPorDefecto,
) : FuenteLugares {

    override suspend fun buscarCercanos(coordenada: Coordenada, radioMetros: Int): Resultado<List<Lugar>> {
        val radioConsulta = radioMetros.coerceIn(RADIO_MINIMO_METROS, RADIO_MAXIMO_METROS)
        val urlGeo = urlGeoSearch(coordenada, radioConsulta)
        return when (val respuesta = clienteHttp.get(urlGeo)) {
            RespuestaHttp.SinConexion -> Resultado.Fallo(ErrorDato.SinInternet)
            is RespuestaHttp.ErrorServidor ->
                Resultado.Fallo(ErrorDato.FuenteNoDisponible("Wikipedia $idioma HTTP ${respuesta.codigo}"))
            is RespuestaHttp.FalloTransporte ->
                Resultado.Fallo(ErrorDato.FuenteNoDisponible(respuesta.detalle))
            is RespuestaHttp.Exito -> procesarGeoSearch(coordenada, radioMetros, respuesta.cuerpo)
        }
    }

    /** Busca el resumen de un artículo por término (para enriquecer hitos OSM o el municipio). */
    suspend fun buscarResumenPorTitulo(termino: String): String? {
        if (termino.isBlank()) return null
        val codificado = URLEncoder.encode(termino, StandardCharsets.UTF_8.name())
        val urlBusqueda = "https://$idioma.wikipedia.org/w/api.php" +
            "?action=query&list=search&srsearch=$codificado&srlimit=1&format=json"
        val idPagina = when (val res = clienteHttp.get(urlBusqueda)) {
            is RespuestaHttp.Exito -> parsearPrimerIdBusqueda(res.cuerpo)
            else -> null
        } ?: return null
        return obtenerResumenes(listOf(idPagina))[idPagina]?.takeIf { it.isNotBlank() }
    }

    private suspend fun procesarGeoSearch(
        origen: Coordenada,
        radioMetros: Int,
        cuerpoJson: String,
    ): Resultado<List<Lugar>> {
        val items = try {
            json.decodeFromString<RespuestaMediaWikiGeo>(cuerpoJson).query?.geosearch.orEmpty()
        } catch (e: SerializationException) {
            Log.w(ETIQUETA, "JSON inválido en geosearch ($idioma)", e)
            return Resultado.Fallo(ErrorDato.FuenteNoDisponible("Respuesta inválida de Wikipedia ($idioma)"))
        }
        val dentroDelRadio = items
            .filter { esTituloDeLugar(it.title) }
            .mapNotNull { item -> item.aCandidato(origen, radioMetros) }
            .sortedBy(CandidatoGeo::distanciaMetros)
            .take(MAX_RESUMENES)
        if (dentroDelRadio.isEmpty()) return Resultado.Exito(emptyList())

        val resumenes = obtenerResumenes(dentroDelRadio.map(CandidatoGeo::pageId))
        val lugares = dentroDelRadio
            .filter { candidato -> esExtractoDeLugar(resumenes[candidato.pageId].orEmpty()) }
            .map { candidato ->
                val extracto = resumenes[candidato.pageId]?.trim().orEmpty()
                Lugar(
                    id = "wikipedia-$idioma:${candidato.pageId}",
                    nombre = limpiarTitulo(candidato.titulo),
                    coordenada = candidato.coordenada,
                    distanciaMetros = candidato.distanciaMetros,
                    contexto = extracto.ifBlank { "${candidato.titulo}." },
                    fuente = "wikipedia-$idioma",
                    relevancia = Relevancia.ARTICULO_WIKIPEDIA,
                )
            }
        return Resultado.Exito(lugares)
    }

    private suspend fun obtenerResumenes(ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val listaIds = ids.joinToString("|")
        val url = "https://$idioma.wikipedia.org/w/api.php" +
            "?action=query&prop=extracts&exintro=1&explaintext=1&redirects=1&pageids=$listaIds&format=json"
        val respuesta = clienteHttp.get(url) as? RespuestaHttp.Exito ?: return emptyMap()
        return try {
            val paginas = json.decodeFromString<RespuestaMediaWikiPaginas>(respuesta.cuerpo).query?.pages.orEmpty()
            paginas.values.mapNotNull { pagina ->
                val id = pagina.pageid
                val texto = pagina.extract?.trim()
                if (id != null && !texto.isNullOrEmpty()) id to texto else null
            }.toMap()
        } catch (e: SerializationException) {
            Log.w(ETIQUETA, "No se pudieron leer los extractos de Wikipedia ($idioma)", e)
            emptyMap()
        }
    }

    private fun parsearPrimerIdBusqueda(cuerpoJson: String): Long? = try {
        json.decodeFromString<RespuestaMediaWikiBusqueda>(cuerpoJson)
            .query?.search?.firstOrNull()?.pageid
    } catch (e: SerializationException) {
        Log.w(ETIQUETA, "No se pudo leer búsqueda de Wikipedia ($idioma)", e)
        null
    }

    private fun urlGeoSearch(coordenada: Coordenada, radioMetros: Int): String {
        val lat = String.format(Locale.US, "%.6f", coordenada.latitud)
        val lon = String.format(Locale.US, "%.6f", coordenada.longitud)
        return "https://$idioma.wikipedia.org/w/api.php" +
            "?action=query&list=geosearch&gscoord=$lat|$lon&gsradius=$radioMetros&gslimit=$LIMITE_GEOSEARCH&format=json"
    }

    private fun ItemGeoSearch.aCandidato(origen: Coordenada, radioMetros: Int): CandidatoGeo? {
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        val coord = Coordenada(lat, lon)
        val distancia = origen.distanciaMetrosA(coord).roundToInt()
        if (distancia > radioMetros) return null
        return CandidatoGeo(pageid, title, coord, distancia)
    }

    // Descarta anexos y artículos de eventos puntuales que comparten coordenada con plazas.
    private fun esTituloDeLugar(titulo: String): Boolean {
        val limpio = titulo.trim()
        return limpio.isNotEmpty() &&
            !limpio.startsWith("Anexo:", ignoreCase = true) &&
            !limpio.startsWith("Wikipedia:", ignoreCase = true) &&
            !limpio.startsWith("Investidura ", ignoreCase = true) &&
            !limpio.startsWith("Elecciones ", ignoreCase = true)
    }

    private fun esExtractoDeLugar(extracto: String): Boolean {
        if (extracto.isBlank()) return true
        val texto = extracto.lowercase(Locale.ROOT)
        return !texto.contains("es una emisora de radio") &&
            !texto.contains("es una cadena de radio") &&
            !texto.contains("radio station")
    }

    // Quita desambiguaciones finales entre paréntesis como "Plaza de Nariño (Pasto)" -> "Plaza de Nariño".
    private fun limpiarTitulo(titulo: String): String =
        titulo.replace( PARENTESIS_FINAL_REGEX, "").trim().ifEmpty { titulo.trim() }

    private data class CandidatoGeo(
        val pageId: Long,
        val titulo: String,
        val coordenada: Coordenada,
        val distanciaMetros: Int,
    )

    @Serializable
    private data class RespuestaMediaWikiGeo(val query: ConsultaGeo? = null)

    @Serializable
    private data class ConsultaGeo(val geosearch: List<ItemGeoSearch> = emptyList())

    @Serializable
    private data class ItemGeoSearch(
        val pageid: Long,
        val title: String,
        val lat: Double,
        val lon: Double,
        val dist: Double = 0.0,
    )

    @Serializable
    private data class RespuestaMediaWikiPaginas(val query: ConsultaPaginas? = null)

    @Serializable
    private data class ConsultaPaginas(val pages: Map<String, PaginaExtracto> = emptyMap())

    @Serializable
    private data class PaginaExtracto(
        val pageid: Long? = null,
        val title: String? = null,
        val extract: String? = null,
    )

    @Serializable
    private data class RespuestaMediaWikiBusqueda(val query: ConsultaBusqueda? = null)

    @Serializable
    private data class ConsultaBusqueda(val search: List<ItemBusqueda> = emptyList())

    @Serializable
    private data class ItemBusqueda(
        val pageid: Long,
        @SerialName("title") val titulo: String,
    )

    companion object {
        private const val ETIQUETA = "FuenteWikipedia"
        private const val RADIO_MINIMO_METROS = 10
        private const val RADIO_MAXIMO_METROS = 10_000
        private const val LIMITE_GEOSEARCH = 10
        private const val MAX_RESUMENES = 6
        private val PARENTESIS_FINAL_REGEX = Regex("""\s*\([^)]+\)\s*$""")

        val jsonPorDefecto = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}
