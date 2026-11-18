package com.daxlejo.datoscerca.domain.modelo

/**
 * Señal de relevancia que pone la fuente al encontrar un lugar.
 * Solo los emblemáticos (artículo de Wikipedia o etiquetas OSM `tourism`/`historic`) dan datos.
 */
enum class Relevancia(val emblematico: Boolean) {
    ARTICULO_WIKIPEDIA(emblematico = true),
    ETIQUETA_OSM_PATRIMONIAL(emblematico = true),
    NINGUNA(emblematico = false),
}

/** Quién pidió el dato: los automáticos cuentan para el límite por hora; los manuales no. */
enum class OrigenDato { AUTOMATICO, MANUAL }
