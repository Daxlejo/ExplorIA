package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial

/** Anti-repetición: un lugar mostrado dentro de la ventana no se vuelve a ofrecer. */
class FiltroRepeticion(
    private val ventanaMs: Long = VENTANA_POR_DEFECTO_MS,
    val maxRecientes: Int = MAX_RECIENTES_POR_DEFECTO,
) {
    init {
        require(ventanaMs > 0) { "La ventana debe ser positiva" }
        require(maxRecientes > 0) { "Hay que revisar al menos una entrada" }
    }

    fun idsAExcluir(recientes: List<EntradaHistorial>, ahoraMs: Long): Set<String> =
        recientes
            .take(maxRecientes)
            .filter { ahoraMs - it.dato.generadoEnMs < ventanaMs }
            .map { it.dato.lugar.id }
            .toSet()

    companion object {
        const val VENTANA_POR_DEFECTO_MS = 30 * 60_000L
        const val MAX_RECIENTES_POR_DEFECTO = 20
    }
}
