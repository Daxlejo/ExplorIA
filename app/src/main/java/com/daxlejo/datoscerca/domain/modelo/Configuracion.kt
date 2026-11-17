package com.daxlejo.datoscerca.domain.modelo

/**
 * Preferencias del usuario. Invariante: siempre hay al menos un tipo marcado.
 * [modoDemo] se guarda desde ya para que la Sesión 2 lo active sin tocar el dominio.
 */
data class Configuracion(
    val tipos: Set<TipoDato>,
    val aleatorio: Boolean,
    val intensidad: Intensidad,
    val vozActiva: Boolean,
    val vibracionActiva: Boolean,
    val pausa: Boolean,
    val modoDemo: Boolean = false,
) {
    init {
        require(tipos.isNotEmpty()) { "Debe quedar al menos un tipo de dato" }
    }

    fun puedeDesmarcar(tipo: TipoDato): Boolean = tipo !in tipos || tipos.size > 1

    // Marca o desmarca un tipo; si es el último marcado, no cambia nada.
    fun conTipoAlternado(tipo: TipoDato): Configuracion = when {
        tipo !in tipos -> copy(tipos = tipos + tipo)
        tipos.size > 1 -> copy(tipos = tipos - tipo)
        else -> this
    }

    companion object {
        val PorDefecto = Configuracion(
            tipos = setOf(TipoDato.INTERESANTE, TipoDato.HISTORICO, TipoDato.CURIOSO),
            aleatorio = false,
            intensidad = Intensidad.NORMAL,
            vozActiva = false,
            vibracionActiva = true,
            pausa = false,
        )
    }
}
