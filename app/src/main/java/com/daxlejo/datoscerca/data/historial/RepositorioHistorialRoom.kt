package com.daxlejo.datoscerca.data.historial

import android.database.sqlite.SQLiteException
import android.util.Log
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Dato
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.puerto.RepositorioHistorial
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RepositorioHistorialRoom(private val dao: EntradaDao) : RepositorioHistorial {

    override fun observar(): Flow<List<EntradaHistorial>> =
        dao.observar().map { entradas -> entradas.map(EntradaEntity::aDominio) }

    override fun observarEntrada(id: Long): Flow<EntradaHistorial?> =
        dao.observarPorId(id).map { it?.aDominio() }

    override suspend fun recientes(limite: Int): Resultado<List<EntradaHistorial>> =
        intentar("leer recientes") { dao.recientes(limite).map(EntradaEntity::aDominio) }

    override suspend fun guardar(dato: Dato): Resultado<EntradaHistorial> =
        intentar("guardar") {
            val entity = dato.aEntity()
            entity.copy(id = dao.insertar(entity)).aDominio()
        }

    override suspend fun alternarFavorito(id: Long): Resultado<Unit> =
        intentar("alternar favorito") { dao.alternarFavorito(id) }

    override suspend fun borrar(id: Long): Resultado<Unit> =
        intentar("borrar") { dao.borrar(id) }

    private inline fun <T> intentar(operacion: String, accion: () -> T): Resultado<T> =
        try {
            Resultado.Exito(accion())
        } catch (e: SQLiteException) {
            Log.e(ETIQUETA, "Fallo al $operacion en el historial", e)
            Resultado.Fallo(ErrorDato.Almacenamiento(e.message))
        }

    private companion object {
        const val ETIQUETA = "RepositorioHistorial"
    }
}
