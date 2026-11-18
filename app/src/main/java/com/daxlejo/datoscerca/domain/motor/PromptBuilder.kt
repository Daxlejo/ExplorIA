package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.TipoDato

data class Prompt(val sistema: String, val usuario: String, val maxTokensSalida: Int)

/** Builder: arma un prompt corto con el contexto real del lugar, el tono y el largo pedidos. */
class PromptBuilder {
    private var lugar: Lugar? = null
    private var tipo: TipoDato? = null
    private var formato: FormatoDato? = null

    fun lugar(lugar: Lugar) = apply { this.lugar = lugar }
    fun tipo(tipo: TipoDato) = apply { this.tipo = tipo }
    fun formato(formato: FormatoDato) = apply { this.formato = formato }

    fun build(): Prompt {
        val lugar = checkNotNull(lugar) { "Falta el lugar" }
        val tipo = checkNotNull(tipo) { "Falta el tipo de dato" }
        val formato = checkNotNull(formato) { "Falta el formato" }
        return Prompt(SISTEMA, mensajeUsuario(lugar, tipo, formato), formato.maxTokensSalida)
    }

    private fun mensajeUsuario(lugar: Lugar, tipo: TipoDato, formato: FormatoDato): String = buildString {
        appendLine("Lugar: ${lugar.nombre}")
        appendLine("Enfoque: ${enfoque(tipo)}")
        appendLine("Largo: ${formato.instruccionLargo}")
        appendLine("CONTEXTO:")
        append(recortar(lugar.contexto))
    }

    private fun enfoque(tipo: TipoDato): String = when (tipo) {
        TipoDato.INTERESANTE -> "lo más interesante y relevante del lugar"
        TipoDato.HISTORICO -> "un hecho histórico, su origen, fechas o personajes vinculados al lugar"
        TipoDato.COMICO -> "humor ligero y amable, sin burlarse de nadie y sin cambiar los hechos"
        TipoDato.CURIOSO -> "un detalle curioso o poco conocido"
        TipoDato.GASTRONOMICO -> "comida, bebida o tradiciones de cocina relacionadas con el lugar"
    }

    // Recorta en el último espacio para no partir palabras y ahorrar tokens.
    private fun recortar(contexto: String): String {
        val limpio = contexto.trim()
        if (limpio.length <= MAX_CONTEXTO_CHARS) return limpio
        val corte = limpio.take(MAX_CONTEXTO_CHARS)
        val ultimoEspacio = corte.lastIndexOf(' ')
        return if (ultimoEspacio > 0) corte.take(ultimoEspacio) else corte
    }

    companion object {
        const val MAX_CONTEXTO_CHARS = 1200

        const val SISTEMA =
            "Eres un guía local para un caminante. " +
                "Usa únicamente los hechos del CONTEXTO: no inventes nombres, fechas ni cifras. " +
                "Si falta material para el enfoque pedido, cuenta el dato real más interesante disponible como afirmación natural. " +
                "Prohibido decir qué no sabes o usar frases como \"el contexto\", \"la información\", \"no se menciona\" o \"no aparece\". " +
                "Español, texto plano, sin listas ni emojis."
    }
}
