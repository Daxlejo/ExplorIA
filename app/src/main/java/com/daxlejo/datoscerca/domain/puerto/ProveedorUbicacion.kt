package com.daxlejo.datoscerca.domain.puerto

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.EstadoUbicacion
import kotlinx.coroutines.flow.Flow

interface ProveedorUbicacion {
    fun ubicaciones(): Flow<EstadoUbicacion>
    suspend fun ubicacionActual(): Resultado<Coordenada>
}
