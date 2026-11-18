package com.daxlejo.datoscerca.data.historial

import android.database.sqlite.SQLiteException
import android.util.Log
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Dato
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.OrigenDato
import com.daxlejo.datoscerca.domain.puerto.RepositorioHistorial
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RepositorioHistorialRoom(private val dao: EntradaDao) : RepositorioHistorial {

    override fun observar(): Flow<Resultado<List<EntradaHistorial>>> =
        dao.observar()
            .map { entradas -> exito(entradas.map(EntradaEntity::aDominio)) }
            .capturarLectura("observar el historial")

    override fun observarEntrada(id: Long): Flow<Resultado<EntradaHistorial?>> =
        dao.observarPorId(id)
            .map { exito(it?.aDominio()) }
            .capturarLectura("observar la entrada $id")

    override suspend fun contarAutomaticosDesde(desdeMs: Long): Resultado<Int> =
        intentar("contar automáticos") { dao.contarDesde(OrigenDato.AUTOMATICO.name, desdeMs) }

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

    private fun <T> exito(valor: T): Resultado<T> = Resultado.Exito(valor)

    // Un error de lectura llega a la UI como Fallo(Almacenamiento) en lugar de cerrar la app.
    private fun <T> Flow<Resultado<T>>.capturarLectura(operacion: String): Flow<Resultado<T>> =
        catch { e ->
            if (e !is SQLiteException) throw e
            Log.e(ETIQUETA, "Fallo al $operacion", e)
            emit(Resultado.Fallo(ErrorDato.Almacenamiento(e.message)))
        }

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
