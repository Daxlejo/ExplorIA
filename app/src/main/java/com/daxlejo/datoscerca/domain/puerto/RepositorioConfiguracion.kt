package com.daxlejo.datoscerca.domain.puerto

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import kotlinx.coroutines.flow.Flow

interface RepositorioConfiguracion {
    fun observar(): Flow<Configuracion>

    /** Aplica [cambio] sobre el valor guardado de forma atómica. */
    suspend fun actualizar(cambio: (Configuracion) -> Configuracion): Resultado<Unit>
}
