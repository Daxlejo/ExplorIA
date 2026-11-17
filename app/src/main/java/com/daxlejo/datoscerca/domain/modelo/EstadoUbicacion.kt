package com.daxlejo.datoscerca.domain.modelo

import com.daxlejo.datoscerca.domain.error.ErrorDato

sealed interface EstadoUbicacion {
    data class Disponible(val coordenada: Coordenada) : EstadoUbicacion
    data class NoDisponible(val motivo: ErrorDato.ErrorUbicacion) : EstadoUbicacion
}
