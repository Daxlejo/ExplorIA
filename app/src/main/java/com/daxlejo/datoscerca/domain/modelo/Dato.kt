package com.daxlejo.datoscerca.domain.modelo

data class Dato(
    val texto: String,
    val tipo: TipoDato,
    val intensidad: Intensidad,
    val lugar: Lugar,
    val generadoEnMs: Long,
) {
    init {
        require(texto.isNotBlank()) { "El dato no puede estar vacío" }
    }
}
