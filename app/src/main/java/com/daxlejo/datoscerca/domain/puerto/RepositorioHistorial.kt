package com.daxlejo.datoscerca.domain.puerto

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Dato
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import kotlinx.coroutines.flow.Flow

/** Los flujos emiten Fallo(Almacenamiento) si la lectura falla, en lugar de lanzar la excepción. */
interface RepositorioHistorial {
    /** Entradas de la más reciente a la más antigua. */
    fun observar(): Flow<Resultado<List<EntradaHistorial>>>

    /** Emite Exito(null) si la entrada no existe o fue borrada. */
    fun observarEntrada(id: Long): Flow<Resultado<EntradaHistorial?>>

    suspend fun recientes(limite: Int): Resultado<List<EntradaHistorial>>

    /** Datos automáticos generados desde [desdeMs] (incluido); alimenta el límite por hora. */
    suspend fun contarAutomaticosDesde(desdeMs: Long): Resultado<Int>

    suspend fun guardar(dato: Dato): Resultado<EntradaHistorial>
    suspend fun alternarFavorito(id: Long): Resultado<Unit>
    suspend fun borrar(id: Long): Resultado<Unit>
}
