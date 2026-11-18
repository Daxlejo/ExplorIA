package com.daxlejo.datoscerca.data.ia

import java.text.Normalizer
import java.util.Locale

/**
 * Guardia de salida para las respuestas del modelo de IA y las entradas de caché:
 * detecta y rechaza textos con metacomentarios sobre el contexto o sobre lo que no se menciona.
 */
object GuardiaSalidaIa {

    private val FRASES_PROHIBIDAS_NORMALIZADAS = listOf(
        "el contexto",
        "del contexto",
        "en el contexto",
        "no menciona",
        "no se menciona",
        "no aparece",
        "no hay informacion",
        "no se indica",
        "no se especifica",
        "la informacion proporcionada",
    )

    fun contieneMetacomentarios(texto: String): Boolean {
        if (texto.isBlank()) return false
        val normalizado = normalizar(texto)
        return FRASES_PROHIBIDAS_NORMALIZADAS.any { frase -> normalizado.contains(frase) }
    }

    private fun normalizar(texto: String): String {
        val sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return sinAcentos.lowercase(Locale.ROOT)
    }
}
