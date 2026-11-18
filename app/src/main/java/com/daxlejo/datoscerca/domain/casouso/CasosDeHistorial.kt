package com.daxlejo.datoscerca.domain.casouso

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.puerto.RepositorioHistorial
import kotlinx.coroutines.flow.Flow

class ObservarHistorial(private val repositorio: RepositorioHistorial) {
    operator fun invoke(): Flow<Resultado<List<EntradaHistorial>>> = repositorio.observar()
}

class ObservarEntrada(private val repositorio: RepositorioHistorial) {
    operator fun invoke(id: Long): Flow<Resultado<EntradaHistorial?>> = repositorio.observarEntrada(id)
}

class AlternarFavorito(private val repositorio: RepositorioHistorial) {
    suspend operator fun invoke(id: Long): Resultado<Unit> = repositorio.alternarFavorito(id)
}

class BorrarEntradaHistorial(private val repositorio: RepositorioHistorial) {
    suspend operator fun invoke(id: Long): Resultado<Unit> = repositorio.borrar(id)
}
