package com.daxlejo.datoscerca.domain.modelo

data class Dato(
    val texto: String,
    val tipo: TipoDato,
    val intensidad: Intensidad,
    val lugar: Lugar,
    val generadoEnMs: Long,
    val origen: OrigenDato,
) {
    init {
        require(texto.isNotBlank()) { "El dato no puede estar vacío" }
    }
}
