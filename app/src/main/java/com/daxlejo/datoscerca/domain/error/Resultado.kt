package com.daxlejo.datoscerca.domain.error

sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Fallo(val error: ErrorDato) : Resultado<Nothing>
}

inline fun <T, R> Resultado<T>.encadenar(siguiente: (T) -> Resultado<R>): Resultado<R> = when (this) {
    is Resultado.Exito -> siguiente(valor)
    is Resultado.Fallo -> this
}

inline fun <T, R> Resultado<T>.mapear(transformar: (T) -> R): Resultado<R> =
    encadenar { Resultado.Exito(transformar(it)) }
