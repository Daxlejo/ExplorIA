package com.daxlejo.datoscerca.domain.puerto

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Dato
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import kotlinx.coroutines.flow.Flow

interface RepositorioHistorial {
    /** Entradas de la más reciente a la más antigua. */
    fun observar(): Flow<List<EntradaHistorial>>

    /** Emite null si la entrada no existe o fue borrada. */
    fun observarEntrada(id: Long): Flow<EntradaHistorial?>

    suspend fun recientes(limite: Int): Resultado<List<EntradaHistorial>>
    suspend fun guardar(dato: Dato): Resultado<EntradaHistorial>
    suspend fun alternarFavorito(id: Long): Resultado<Unit>
    suspend fun borrar(id: Long): Resultado<Unit>
}
