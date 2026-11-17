package com.daxlejo.datoscerca.domain.puerto

import com.daxlejo.datoscerca.domain.error.Resultado

fun interface Reloj {
    fun ahoraMs(): Long
}

fun interface Vibrador {
    fun vibrarNuevoDato()
}

interface Narrador {
    fun hablar(texto: String): Resultado<Unit>
    fun detener()
}
