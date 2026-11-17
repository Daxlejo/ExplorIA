package com.daxlejo.datoscerca.domain.puerto

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Lugar

/**
 * Busca el lugar más cercano dentro de [radioMetros], saltando los ids de [excluir].
 * Fallo(SinLugares) significa "aquí no hay nada": en una cadena de fuentes, pasa a la siguiente.
 */
interface FuenteLugares {
    suspend fun buscarCercano(
        coordenada: Coordenada,
        radioMetros: Int,
        excluir: Set<String> = emptySet(),
    ): Resultado<Lugar>
}
