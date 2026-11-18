package com.daxlejo.datoscerca.domain.puerto

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Lugar

/**
 * Devuelve los lugares dentro de [radioMetros], del más cercano al más lejano, cada uno con su
 * [com.daxlejo.datoscerca.domain.modelo.Relevancia]. Qué lugar se usa lo decide el dominio.
 * Una lista vacía significa "aquí no hay nada": en una cadena de fuentes, pasa a la siguiente.
 * Fallo es un error de la fuente (red, servicio caído).
 */
interface FuenteLugares {
    suspend fun buscarCercanos(coordenada: Coordenada, radioMetros: Int): Resultado<List<Lugar>>
}
