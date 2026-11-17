package com.daxlejo.datoscerca.domain.modelo

/**
 * Sitio cercano sobre el que se puede contar un dato.
 * [id] es estable e incluye el prefijo de la fuente (por ejemplo "wikipedia-es:123").
 * [contexto] es el texto real del que la IA saca los hechos; llega vacío al leerlo del historial.
 */
data class Lugar(
    val id: String,
    val nombre: String,
    val coordenada: Coordenada,
    val distanciaMetros: Int,
    val contexto: String,
    val fuente: String,
) {
    init {
        require(id.isNotBlank()) { "El lugar necesita un id" }
        require(nombre.isNotBlank()) { "El lugar necesita un nombre" }
        require(distanciaMetros >= 0) { "La distancia no puede ser negativa" }
    }
}
