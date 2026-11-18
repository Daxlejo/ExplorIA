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
import kotlin.math.roundToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Datos administrativos de un punto para el último recurso del municipio/región. */
data class DireccionAdministrativa(
    val municipio: String,
    val region: String?,
    val pais: String?,
    val barrio: String?,
)

/**
 * Eslabón de la cadena que consulta OpenStreetMap Nominatim (geocodificación inversa).
 * Respeta el límite de máximo 1 petición por segundo y envía el User-Agent descriptivo.
 * Clasifica la [Relevancia] según las etiquetas `tourism`/`historic` y patrimoniales de OSM:
 * calles, tiendas, bancos o casas ordinarias quedan como [Relevancia.NINGUNA].
 */
class FuenteNominatim(
    private val clienteHttp: ClienteHttp,
    private val limitador: LimitadorFrecuencia,
    private val fuenteWikipediaEs: FuenteWikipedia? = null,
    private val json: Json = FuenteWikipedia.jsonPorDefecto,
) : FuenteLugares {

    @Volatile
    private var ultimaConsulta: Pair<Coordenada, RespuestaReverse>? = null

    override suspend fun buscarCercanos(coordenada: Coordenada, radioMetros: Int): Resultado<List<Lugar>> =
        when (val resultado = consultarReverse(coordenada)) {
            is Resultado.Fallo -> resultado
            is Resultado.Exito -> construirLugares(coordenada, radioMetros, resultado.valor)
        }

    /** Devuelve el municipio/región del punto reutilizando la última respuesta si es cercana. */
    suspend fun obtenerDireccion(coordenada: Coordenada): Resultado<DireccionAdministrativa?> {
        val guardada = ultimaConsulta
            ?.takeIf { (coord, _) -> coord.distanciaMetrosA(coordenada) <= RADIO_CACHE_DIRECCION_METROS }
            ?.second
        val reverse = if (guardada != null) {
            guardada
        } else {
            when (val res = consultarReverse(coordenada)) {
                is Resultado.Fallo -> return Resultado.Fallo(res.error)
                is Resultado.Exito -> res.valor
            }
        }
        return Resultado.Exito(reverse.aDireccionAdministrativa())
    }

    private suspend fun consultarReverse(coordenada: Coordenada): Resultado<RespuestaReverse> {
        val lat = String.format(Locale.US, "%.6f", coordenada.latitud)
        val lon = String.format(Locale.US, "%.6f", coordenada.longitud)
        val url = "https://nominatim.openstreetmap.org/reverse" +
            "?lat=$lat&lon=$lon&format=jsonv2&addressdetails=1&extratags=1&namedetails=1&zoom=18&accept-language=es"
        return when (val respuesta = limitador.ejecutar { clienteHttp.get(url) }) {
            RespuestaHttp.SinConexion -> Resultado.Fallo(ErrorDato.SinInternet)
            is RespuestaHttp.ErrorServidor ->
                Resultado.Fallo(ErrorDato.FuenteNoDisponible("Nominatim HTTP ${respuesta.codigo}"))
            is RespuestaHttp.FalloTransporte ->
                Resultado.Fallo(ErrorDato.FuenteNoDisponible(respuesta.detalle))
            is RespuestaHttp.Exito -> parsearReverse(coordenada, respuesta.cuerpo)
        }
    }

    private fun parsearReverse(coordenada: Coordenada, cuerpoJson: String): Resultado<RespuestaReverse> = try {
        val reverse = json.decodeFromString<RespuestaReverse>(cuerpoJson)
        ultimaConsulta = coordenada to reverse
        Resultado.Exito(reverse)
    } catch (e: SerializationException) {
        Log.w(ETIQUETA, "JSON inválido de Nominatim", e)
        Resultado.Fallo(ErrorDato.FuenteNoDisponible("Respuesta inválida de Nominatim"))
    }

    private suspend fun construirLugares(
        origen: Coordenada,
        radioMetros: Int,
        reverse: RespuestaReverse,
    ): Resultado<List<Lugar>> {
        val directo = reverse.aLugar(origen, radioMetros)
        if (directo != null && directo.relevancia.emblematico) {
            return Resultado.Exito(listOf(enriquecerContexto(directo, reverse)))
        }
        if (esSenderoPeatonalOBanca(reverse)) {
            val plaza = buscarPlazaEnRecuadro(origen, radioMetros, reverse)
            if (plaza != null) return Resultado.Exito(listOf(enriquecerContexto(plaza, reverse)))
        }
        return Resultado.Exito(listOfNotNull(directo))
    }

    // Cuando el punto cae sobre un sendero o banca dentro de una plaza peatonal, busca el polígono place=square.
    private suspend fun buscarPlazaEnRecuadro(
        origen: Coordenada,
        radioMetros: Int,
        reverseBase: RespuestaReverse,
    ): Lugar? {
        val delta = (radioMetros.coerceAtMost(250) / METROS_POR_GRADO)
        val oeste = String.format(Locale.US, "%.6f", origen.longitud - delta)
        val norte = String.format(Locale.US, "%.6f", origen.latitud + delta)
        val este = String.format(Locale.US, "%.6f", origen.longitud + delta)
        val sur = String.format(Locale.US, "%.6f", origen.latitud - delta)
        val url = "https://nominatim.openstreetmap.org/search" +
            "?q=%5Bplace%3Dsquare%5D&viewbox=$oeste,$norte,$este,$sur&bounded=1" +
            "&format=jsonv2&addressdetails=1&extratags=1&limit=3&accept-language=es"
        val exito = limitador.ejecutar { clienteHttp.get(url) } as? RespuestaHttp.Exito ?: return null
        val encontrados = try {
            json.decodeFromString<List<RespuestaReverse>>(exito.cuerpo)
        } catch (e: SerializationException) {
            Log.w(ETIQUETA, "No se pudo leer búsqueda de plaza en Nominatim", e)
            return null
        }
        return encontrados
            .mapNotNull { item -> item.copy(address = item.address ?: reverseBase.address).aLugar(origen, radioMetros) }
            .filter { it.relevancia.emblematico }
            .minByOrNull(Lugar::distanciaMetros)
    }

    private suspend fun enriquecerContexto(lugar: Lugar, reverse: RespuestaReverse): Lugar {
        val wiki = fuenteWikipediaEs ?: return lugar
        val municipio = reverse.aDireccionAdministrativa()?.municipio.orEmpty()
        val consulta = "${lugar.nombre} $municipio".trim()
        val resumenWiki = wiki.buscarResumenPorTitulo(consulta) ?: return lugar
        return lugar.copy(contexto = "$resumenWiki ${lugar.contexto}".trim())
    }

    private fun RespuestaReverse.aLugar(origen: Coordenada, radioMetros: Int): Lugar? {
        val nombreLugar = nombreVisible() ?: return null
        val latitud = lat?.toDoubleOrNull() ?: origen.latitud
        val longitud = lon?.toDoubleOrNull() ?: origen.longitud
        if (latitud !in -90.0..90.0 || longitud !in -180.0..180.0) return null
        val coord = Coordenada(latitud, longitud)
        val distancia = origen.distanciaMetrosA(coord).roundToInt()
        if (distancia > radioMetros) return null
        val relevancia = clasificarRelevancia()
        val idOsm = if (!osmType.isNullOrBlank() && osmId != null) {
            "nominatim:$osmType:$osmId"
        } else {
            "nominatim:${nombreLugar.lowercase(Locale.ROOT).replace(' ', '-')}"
        }
        return Lugar(
            id = idOsm,
            nombre = nombreLugar,
            coordenada = coord,
            distanciaMetros = distancia,
            contexto = armarContexto(nombreLugar),
            fuente = "nominatim",
            relevancia = relevancia,
        )
    }

    private fun RespuestaReverse.nombreVisible(): String? {
        val directo = name?.trim()?.takeIf { it.isNotEmpty() }
        if (directo != null) return directo
        val addr = address ?: return null
        return listOfNotNull(
            addr.tourism,
            addr.historic,
            addr.square,
            addr.amenity,
            addr.building,
            addr.road,
        ).firstOrNull { it.isNotBlank() }?.trim()
    }

    private fun RespuestaReverse.clasificarRelevancia(): Relevancia {
        val tags = extratags.orEmpty()
        if (!tags["wikipedia"].isNullOrBlank()) return Relevancia.ARTICULO_WIKIPEDIA
        val cat = category?.lowercase(Locale.ROOT).orEmpty()
        val tip = type?.lowercase(Locale.ROOT).orEmpty()
        val tieneNombrePropio = !name.isNullOrBlank() || !address?.square.isNullOrBlank() ||
            !address?.tourism.isNullOrBlank() || !address?.historic.isNullOrBlank()
        if (!tieneNombrePropio) return Relevancia.NINGUNA

        val esHistorico = (cat == "historic" && tip != "no") ||
            (!tags["historic"].isNullOrBlank() && tags["historic"] != "no") ||
            !tags["heritage"].isNullOrBlank()
        val esTurismoEmblematico = (cat == "tourism" && tip in TIPOS_TURISMO_EMBLEMATICO) ||
            (tags["tourism"]?.lowercase(Locale.ROOT) in TIPOS_TURISMO_EMBLEMATICO)
        val esPlaza = (cat == "place" && tip == "square") || !address?.square.isNullOrBlank()
        val esArquitecturaEmblematica = cat in CATEGORIAS_ARQUITECTURA && tip in TIPOS_ARQUITECTURA_EMBLEMATICA

        return if (esHistorico || esTurismoEmblematico || esPlaza || esArquitecturaEmblematica) {
            Relevancia.ETIQUETA_OSM_PATRIMONIAL
        } else {
            Relevancia.NINGUNA
        }
    }

    private fun RespuestaReverse.armarContexto(nombreLugar: String): String = buildString {
        val descTipo = descripcionTipo(category, type)
        val dir = aDireccionAdministrativa()
        append(nombreLugar)
        if (descTipo != null) append(" es $descTipo")
        if (dir != null) {
            append(" ubicado en ")
            if (!dir.barrio.isNullOrBlank()) append("el sector de ${dir.barrio}, ")
            append(dir.municipio)
            if (!dir.region.isNullOrBlank() && !dir.region.equals(dir.municipio, ignoreCase = true)) {
                append(", ${dir.region}")
            }
            if (!dir.pais.isNullOrBlank()) append(", ${dir.pais}")
        }
        append(".")
        extratags?.get("description")?.takeIf { it.isNotBlank() }?.let { append(" $it.") }
        extratags?.get("inscription")?.takeIf { it.isNotBlank() }?.let { append(" Inscripción: $it.") }
        extratags?.get("operator")?.takeIf { it.isNotBlank() }?.let { append(" Operado por $it.") }
        extratags?.get("architect")?.takeIf { it.isNotBlank() }?.let { append(" Arquitecto: $it.") }
        extratags?.get("start_date")?.takeIf { it.isNotBlank() }?.let { append(" Fecha histórica: $it.") }
        displayName?.takeIf { it.isNotBlank() }?.let { append(" Referencia geográfica: $it.") }
    }

    private fun descripcionTipo(categoria: String?, tipo: String?): String? = when (tipo?.lowercase(Locale.ROOT)) {
        "museum" -> "un museo y espacio cultural"
        "church", "place_of_worship", "cathedral", "chapel" -> "un templo histórico y lugar de culto"
        "monastery" -> "un monasterio histórico"
        "theatre", "arts_centre" -> "un teatro y escenario cultural emblemático"
        "monument", "memorial" -> "un monumento conmemorativo"
        "square" -> "una plaza pública tradicional"
        "castle", "fort" -> "una fortificación histórica"
        "archaeological_site", "ruins" -> "un sitio arqueológico e histórico"
        "viewpoint" -> "un mirador reconocido"
        "artwork" -> "una obra de arte público"
        "townhall" -> "un edificio cívico y sede institucional"
        else -> if (categoria == "historic") "un sitio de valor histórico y patrimonial" else null
    }

    private fun esSenderoPeatonalOBanca(reverse: RespuestaReverse): Boolean {
        val tip = reverse.type?.lowercase(Locale.ROOT).orEmpty()
        return tip in TIPOS_SENDERO_O_MOBILIARIO
    }

    private fun RespuestaReverse.aDireccionAdministrativa(): DireccionAdministrativa? {
        val addr = address ?: return null
        val municipioRaw = listOfNotNull(
            addr.county,
            addr.city,
            addr.town,
            addr.municipality,
            addr.village,
        ).map(::limpiarNombreMunicipio).firstOrNull { it.isNotBlank() } ?: return null
        return DireccionAdministrativa(
            municipio = municipioRaw,
            region = addr.state?.trim()?.takeIf { it.isNotEmpty() },
            pais = addr.country?.trim()?.takeIf { it.isNotEmpty() },
            barrio = (addr.neighbourhood ?: addr.suburb)?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun limpiarNombreMunicipio(nombre: String): String =
        nombre.replace("Perímetro Urbano ", "", ignoreCase = true).trim()

    @Serializable
    private data class RespuestaReverse(
        @SerialName("osm_type") val osmType: String? = null,
        @SerialName("osm_id") val osmId: Long? = null,
        val lat: String? = null,
        val lon: String? = null,
        val category: String? = null,
        val type: String? = null,
        val name: String? = null,
        @SerialName("display_name") val displayName: String? = null,
        val address: DireccionOsm? = null,
        val extratags: Map<String, String>? = null,
    )

    @Serializable
    private data class DireccionOsm(
        val tourism: String? = null,
        val historic: String? = null,
        val square: String? = null,
        val amenity: String? = null,
        val building: String? = null,
        val road: String? = null,
        val neighbourhood: String? = null,
        val suburb: String? = null,
        val city: String? = null,
        val town: String? = null,
        val village: String? = null,
        val municipality: String? = null,
        val county: String? = null,
        val state: String? = null,
        val country: String? = null,
    )

    companion object {
        private const val ETIQUETA = "FuenteNominatim"
        private const val RADIO_CACHE_DIRECCION_METROS = 150.0
        private const val METROS_POR_GRADO = 111_320.0

        private val TIPOS_TURISMO_EMBLEMATICO = setOf(
            "museum",
            "gallery",
            "attraction",
            "artwork",
            "viewpoint",
            "monument",
            "theme_park",
            "zoo",
            "aquarium",
            "archaeological_site",
        )

        private val CATEGORIAS_ARQUITECTURA = setOf("amenity", "building", "leisure", "historic")

        private val TIPOS_ARQUITECTURA_EMBLEMATICA = setOf(
            "place_of_worship",
            "church",
            "cathedral",
            "chapel",
            "monastery",
            "shrine",
            "basilica",
            "theatre",
            "arts_centre",
            "townhall",
            "planetarium",
        )

        private val TIPOS_SENDERO_O_MOBILIARIO = setOf(
            "footway",
            "pedestrian",
            "bench",
            "path",
        )
    }
}
