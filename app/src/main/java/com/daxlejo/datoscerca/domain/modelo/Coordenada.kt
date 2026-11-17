package com.daxlejo.datoscerca.domain.modelo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Coordenada(val latitud: Double, val longitud: Double) {

    init {
        require(latitud in -90.0..90.0) { "Latitud fuera de rango: $latitud" }
        require(longitud in -180.0..180.0) { "Longitud fuera de rango: $longitud" }
    }

    // Distancia en línea recta sobre la esfera (fórmula del semiverseno).
    fun distanciaMetrosA(otra: Coordenada): Double {
        val deltaLatitud = Math.toRadians(otra.latitud - latitud)
        val deltaLongitud = Math.toRadians(otra.longitud - longitud)
        val a = sin(deltaLatitud / 2).pow(2) +
            cos(Math.toRadians(latitud)) * cos(Math.toRadians(otra.latitud)) * sin(deltaLongitud / 2).pow(2)
        return 2 * RADIO_TIERRA_METROS * asin(sqrt(a))
    }

    private companion object {
        const val RADIO_TIERRA_METROS = 6_371_000.0
    }
}
