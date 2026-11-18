package com.daxlejo.datoscerca.domain.motor

import kotlinx.coroutines.sync.Mutex

/**
 * Un solo dato en preparación a la vez. Lo comparten el dato manual y el automático:
 * si uno está en curso, el otro no espera turno (eso daría dos datos seguidos), se descarta.
 */
class GeneracionUnica {
    private val enCurso = Mutex()

    /** Ejecuta [accion] si no hay otra en curso; si la hay, devuelve null sin ejecutarla. */
    suspend fun <T> siEstaLibre(accion: suspend () -> T): T? {
        if (!enCurso.tryLock()) return null
        return try {
            accion()
        } finally {
            enCurso.unlock()
        }
    }
}
