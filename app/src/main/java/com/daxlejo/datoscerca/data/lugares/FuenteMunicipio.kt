package com.daxlejo.datoscerca.data.lugares

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.Relevancia
import com.daxlejo.datoscerca.domain.motor.FiltroRelevancia
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import java.text.Normalizer
import java.util.Locale

/**
 * Último eslabón de la cadena de lugares: construye un lugar representativo del municipio o la
 * región cuando no hay ningún hito emblemático cercano.
 *
 * Solo responde para búsquedas manuales ("Dato ahora", cuyo radio supera el rango de llegada
 * automática de [FiltroRelevancia.RANGO_LLEGADA]). En exploración automática devuelve siempre
 * lista vacía para que las calles comunes nunca disparen datos por sí solas.
 */
class FuenteMunicipio(
    private val fuenteNominatim: FuenteNominatim,
    private val fuenteWikipediaEs: FuenteWikipedia? = null,
) : FuenteLugares {

    override suspend fun buscarCercanos(coordenada: Coordenada, radioMetros: Int): Resultado<List<Lugar>> {
        if (radioMetros <= FiltroRelevancia.RANGO_LLEGADA.last) {
            return Resultado.Exito(emptyList())
        }
        return when (val direccionRes = fuenteNominatim.obtenerDireccion(coordenada)) {
            is Resultado.Fallo -> direccionRes
            is Resultado.Exito -> {
                val direccion = direccionRes.valor ?: return Resultado.Exito(emptyList())
                Resultado.Exito(listOf(crearLugarMunicipio(coordenada, direccion)))
            }
        }
    }

    private suspend fun crearLugarMunicipio(
        coordenada: Coordenada,
        direccion: DireccionAdministrativa,
    ): Lugar {
        val terminoWiki = listOfNotNull(direccion.municipio, direccion.region, direccion.pais)
            .joinToString(" ")
        val resumenWiki = fuenteWikipediaEs?.buscarResumenPorTitulo(terminoWiki)
            ?: fuenteWikipediaEs?.buscarResumenPorTitulo(direccion.municipio)
        val contextoBase = buildString {
            append("${direccion.municipio} es un municipio")
            if (!direccion.region.isNullOrBlank()) append(" de ${direccion.region}")
            if (!direccion.pais.isNullOrBlank()) append(", en ${direccion.pais}")
            append(".")
            if (!direccion.barrio.isNullOrBlank()) {
                append(" El punto consultado se encuentra en el sector de ${direccion.barrio}.")
            }
        }
        val contexto = if (!resumenWiki.isNullOrBlank()) {
            "$resumenWiki $contextoBase"
        } else {
            contextoBase
        }
        return Lugar(
            id = "municipio:${normalizarId(direccion.municipio)}",
            nombre = direccion.municipio,
            coordenada = coordenada,
            distanciaMetros = 0,
            contexto = contexto,
            fuente = "municipio",
            relevancia = Relevancia.ETIQUETA_OSM_PATRIMONIAL,
        )
    }

    private fun normalizarId(texto: String): String {
        val sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return sinAcentos.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "region" }
    }
}
