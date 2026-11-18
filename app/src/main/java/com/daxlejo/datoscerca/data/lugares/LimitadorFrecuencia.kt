package com.daxlejo.datoscerca.data.lugares

import com.daxlejo.datoscerca.domain.puerto.Reloj
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Garantiza un intervalo mínimo entre peticiones consecutivas (por ejemplo, máximo 1 petición
 * por segundo según la política de uso de OpenStreetMap Nominatim).
 */
class LimitadorFrecuencia(
    private val intervaloMinimoMs: Long = UN_SEGUNDO_MS,
    private val reloj: Reloj,
    private val pausar: suspend (Long) -> Unit = ::delay,
) {
    private val mutex = Mutex()
    private var ultimaEjecucionMs: Long? = null

    suspend fun <T> ejecutar(accion: suspend () -> T): T = mutex.withLock {
        val anterior = ultimaEjecucionMs
        if (anterior != null) {
            val transcurrido = reloj.ahoraMs() - anterior
            val espera = intervaloMinimoMs - transcurrido
            if (espera > 0) pausar(espera)
        }
        ultimaEjecucionMs = reloj.ahoraMs()
        accion()
    }

    companion object {
        const val UN_SEGUNDO_MS = 1_000L
    }
}
