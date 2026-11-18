package com.daxlejo.datoscerca.data.lugares

import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.motor.PromptBuilder
import java.text.Normalizer
import java.util.Locale

/**
 * Extrae y prioriza las secciones de un artículo de Wikipedia en texto plano según el enfoque
 * (`Historia`, `Gastronomía`, `Cultura`/`Turismo`/`Patrimonio`), recortando siempre en límite de
 * palabra hasta [PromptBuilder.MAX_CONTEXTO_CHARS].
 */
object ExtractorContextoWikipedia {

    const val UMBRAL_EXTRACTO_CORTO = 400
    private const val MAX_INTRO_RESUMIDA_CHARS = 240
    private const val CUOTA_POR_CATEGORIA_SIN_ENFOQUE = 340

    private val ENCABEZADO_WIKI_REGEX = Regex("""^(=+)\s*(.+?)\s*\1\s*$""")
    private val ESPACIOS_MULTIPLES_REGEX = Regex("""\s+""")

    private val INDICADORES_MUNICIPIO = listOf(
        "es un municipio",
        "es una ciudad",
        "es la capital",
        "capital del departamento",
        "capital de la provincia",
        "municipio colombiano",
        "ciudad colombiana",
        "es una comuna",
        "es un canton",
        "es un distrito",
        "is a municipality",
        "is a city",
        "is a town",
        "is the capital",
        "is a commune",
    )

    private val CLAVES_HISTORIA = listOf(
        "historia",
        "history",
        "origen",
        "origenes",
        "fundacion",
        "epoca colonial",
        "epoca precolombina",
        "prehispanic",
        "independencia",
        "siglo",
        "antecedentes",
        "cronologia",
        " conquista",
        "republica",
    )

    private val CLAVES_GASTRONOMIA = listOf(
        "gastronomia",
        "cocina",
        "cuisine",
        "comida",
        "platos",
        "bebidas",
        "culinaria",
        "alimentacion",
    )

    private val CLAVES_CULTURA_Y_CURIOSIDADES = listOf(
        "cultura",
        "culture",
        "turismo",
        "tourism",
        "patrimonio",
        "heritage",
        "curiosidades",
        "lugares de interes",
        "sitios de interes",
        "monumentos",
        "arquitectura",
        "festividades",
        "fiestas",
        "tradiciones",
        "carnaval",
        "semana santa",
        "atractivos",
        "descripcion",
    )

    private val SECCIONES_IGNORADAS = listOf(
        "referencias",
        "references",
        "vease tambien",
        "see also",
        "enlaces externos",
        "external links",
        "bibliografia",
        "notas",
    )

    private val OTRAS_SECCIONES_CONOCIDAS = listOf(
        "geografia",
        "geography",
        "demografia",
        "clima",
        "economia",
        "educacion",
        "transporte",
        "deporte",
        "politica",
        "gobierno",
        "toponimia",
        "simbolos",
        "administracion",
        "hidrografia",
        "limites",
    )

    fun requiereTextoCompleto(extractoIntro: String, esMunicipio: Boolean = false): Boolean {
        if (esMunicipio) return true
        val limpio = extractoIntro.trim()
        return limpio.length < UMBRAL_EXTRACTO_CORTO || esExtractoDeMunicipio(limpio)
    }

    fun esExtractoDeMunicipio(extracto: String): Boolean {
        val inicio = normalizar(extracto.take(UMBRAL_EXTRACTO_CORTO))
        return INDICADORES_MUNICIPIO.any { inicio.contains(it) }
    }

    fun seleccionarContexto(
        extractoCompleto: String,
        extractoIntroRespaldo: String = "",
        enfoque: TipoDato? = null,
        maxChars: Int = PromptBuilder.MAX_CONTEXTO_CHARS,
    ): String {
        val (intro, bloques) = parsearArticulo(extractoCompleto)
        val baseIntro = intro.ifBlank { extractoIntroRespaldo }.trim()
        val porCategoria = agruparPorCategoria(bloques)
        if (porCategoria.isEmpty()) {
            val respaldo = baseIntro.ifBlank { limpiarEspacios(extractoCompleto) }
            return recortarEnPalabra(respaldo, maxChars)
        }

        val introBreve = resumirIntro(baseIntro)
        val cuerpoSecciones = if (enfoque != null) {
            ordenCategorias(enfoque)
                .mapNotNull { porCategoria[it] }
                .joinToString(" ")
        } else {
            combinarCategoriasEquilibradas(porCategoria)
        }
        val combinado = if (introBreve.isNotEmpty()) {
            "$introBreve $cuerpoSecciones"
        } else {
            cuerpoSecciones
        }
        return recortarEnPalabra(combinado, maxChars)
    }

    fun recortarEnPalabra(texto: String, maxChars: Int = PromptBuilder.MAX_CONTEXTO_CHARS): String {
        val limpio = limpiarEspacios(texto)
        if (limpio.length <= maxChars) return limpio
        val corte = limpio.take(maxChars)
        val ultimoEspacio = corte.lastIndexOf(' ')
        return if (ultimoEspacio > 0) corte.take(ultimoEspacio).trimEnd() else corte
    }

    private fun agruparPorCategoria(bloques: List<BloqueSeccion>): Map<CategoriaSeccion, String> {
        val resultado = mutableMapOf<CategoriaSeccion, MutableList<String>>()
        for (bloque in bloques) {
            if (bloque.contenido.isBlank() || esSeccionIgnorada(bloque.titulo)) continue
            val categoria = clasificarTitulo(bloque.titulo)
                ?: bloque.tituloPadre?.let(::clasificarTitulo)
                ?: continue
            resultado.getOrPut(categoria) { mutableListOf() }.add(bloque.contenido)
        }
        return resultado.mapValues { (_, textos) -> textos.joinToString(" ") }
    }

    private fun combinarCategoriasEquilibradas(porCategoria: Map<CategoriaSeccion, String>): String {
        val orden = listOf(
            CategoriaSeccion.HISTORIA,
            CategoriaSeccion.GASTRONOMIA,
            CategoriaSeccion.CULTURA_Y_CURIOSIDADES,
        )
        val primerosFragmentos = orden.mapNotNull { cat ->
            porCategoria[cat]?.let { recortarEnPalabra(it, CUOTA_POR_CATEGORIA_SIN_ENFOQUE) }
        }
        val restos = orden.mapNotNull { cat ->
            val completo = porCategoria[cat] ?: return@mapNotNull null
            val recorte = recortarEnPalabra(completo, CUOTA_POR_CATEGORIA_SIN_ENFOQUE)
            completo.removePrefix(recorte).trim().takeIf { it.isNotEmpty() }
        }
        return (primerosFragmentos + restos).joinToString(" ")
    }

    private fun ordenCategorias(enfoque: TipoDato): List<CategoriaSeccion> = when (enfoque) {
        TipoDato.HISTORICO -> listOf(
            CategoriaSeccion.HISTORIA,
            CategoriaSeccion.CULTURA_Y_CURIOSIDADES,
            CategoriaSeccion.GASTRONOMIA,
        )
        TipoDato.GASTRONOMICO -> listOf(
            CategoriaSeccion.GASTRONOMIA,
            CategoriaSeccion.CULTURA_Y_CURIOSIDADES,
            CategoriaSeccion.HISTORIA,
        )
        TipoDato.CURIOSO, TipoDato.INTERESANTE, TipoDato.COMICO -> listOf(
            CategoriaSeccion.CULTURA_Y_CURIOSIDADES,
            CategoriaSeccion.HISTORIA,
            CategoriaSeccion.GASTRONOMIA,
        )
    }

    private fun parsearArticulo(texto: String): Pair<String, List<BloqueSeccion>> {
        val lineas = texto.lines()
        val usaMarcadoresWiki = lineas.any { ENCABEZADO_WIKI_REGEX.matches(it.trim()) }
        val introLineas = mutableListOf<String>()
        val bloques = mutableListOf<BloqueSeccion>()
        var tituloActual: String? = null
        var tituloPadreNivel2: String? = null
        val bufferSeccion = mutableListOf<String>()

        fun cerrarBloqueActual() {
            val titulo = tituloActual ?: return
            val contenido = limpiarEspacios(bufferSeccion.joinToString(" "))
            if (contenido.isNotEmpty()) {
                bloques += BloqueSeccion(titulo = titulo, tituloPadre = tituloPadreNivel2, contenido = contenido)
            }
            bufferSeccion.clear()
        }

        for (lineaRaw in lineas) {
            val linea = lineaRaw.trim()
            if (linea.isEmpty()) continue
            val matchWiki = ENCABEZADO_WIKI_REGEX.matchEntire(linea)
            if (matchWiki != null) {
                cerrarBloqueActual()
                val nivel = matchWiki.groupValues[1].length
                val titulo = matchWiki.groupValues[2].trim()
                if (nivel <= 2) tituloPadreNivel2 = titulo
                tituloActual = titulo
                continue
            }
            if (!usaMarcadoresWiki && esEncabezadoPlano(linea)) {
                cerrarBloqueActual()
                tituloPadreNivel2 = linea
                tituloActual = linea
                continue
            }
            if (tituloActual == null) {
                introLineas += linea
            } else {
                bufferSeccion += linea
            }
        }
        cerrarBloqueActual()
        return limpiarEspacios(introLineas.joinToString(" ")) to bloques
    }

    private fun esEncabezadoPlano(linea: String): Boolean {
        if (linea.length !in 3..55) return false
        val ultimo = linea.last()
        if (ultimo == '.' || ultimo == ':' || ultimo == ';' || ultimo == '!' || ultimo == '?') return false
        val norm = normalizar(linea)
        return clasificarTituloNormalizado(norm) != null ||
            SECCIONES_IGNORADAS.any { norm == it || norm.startsWith("$it ") } ||
            OTRAS_SECCIONES_CONOCIDAS.any { norm == it || norm.startsWith("$it ") }
    }

    private fun resumirIntro(intro: String): String {
        val limpio = limpiarEspacios(intro)
        if (limpio.length <= MAX_INTRO_RESUMIDA_CHARS) return limpio
        val primerPunto = limpio.indexOf(". ")
        if (primerPunto in 25 until MAX_INTRO_RESUMIDA_CHARS) {
            return limpio.substring(0, primerPunto + 1)
        }
        return recortarEnPalabra(limpio, MAX_INTRO_RESUMIDA_CHARS)
    }

    private fun clasificarTitulo(titulo: String): CategoriaSeccion? =
        clasificarTituloNormalizado(normalizar(titulo))

    private fun clasificarTituloNormalizado(norm: String): CategoriaSeccion? = when {
        CLAVES_HISTORIA.any { norm.contains(it.trim()) } -> CategoriaSeccion.HISTORIA
        CLAVES_GASTRONOMIA.any { norm.contains(it.trim()) } -> CategoriaSeccion.GASTRONOMIA
        CLAVES_CULTURA_Y_CURIOSIDADES.any { norm.contains(it.trim()) } -> CategoriaSeccion.CULTURA_Y_CURIOSIDADES
        else -> null
    }

    private fun esSeccionIgnorada(titulo: String): Boolean {
        val norm = normalizar(titulo)
        return SECCIONES_IGNORADAS.any { norm.contains(it) }
    }

    private fun normalizar(texto: String): String {
        val sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return sinAcentos.lowercase(Locale.ROOT).trim()
    }

    private fun limpiarEspacios(texto: String): String =
        texto.replace(ESPACIOS_MULTIPLES_REGEX, " ").trim()

    private data class BloqueSeccion(
        val titulo: String,
        val tituloPadre: String?,
        val contenido: String,
    )

    private enum class CategoriaSeccion {
        HISTORIA,
        GASTRONOMIA,
        CULTURA_Y_CURIOSIDADES,
    }
}
