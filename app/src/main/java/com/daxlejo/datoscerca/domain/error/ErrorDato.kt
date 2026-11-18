package com.daxlejo.datoscerca.domain.error

/** Todo lo que puede impedir mostrar un dato. La UI diseña un estado para cada caso. */
sealed interface ErrorDato {
    /** Motivos por los que no hay ubicación; son los únicos que bloquean la exploración. */
    sealed interface ErrorUbicacion : ErrorDato

    data object SinPermiso : ErrorUbicacion
    data object SinGps : ErrorUbicacion
    data object SinInternet : ErrorDato
    data object SinLugares : ErrorDato
    data object EnPausa : ErrorDato
    data object VozNoDisponible : ErrorDato

    /** Ya se está preparando otro dato (manual o automático); no se generan dos seguidos. */
    data object GeneracionEnCurso : ErrorDato

    /** La fuente de lugares respondió con un error (no es lo mismo que "no hay nada cerca"). */
    data class FuenteNoDisponible(val causa: String?) : ErrorDato
    data class ErrorIa(val causa: String?) : ErrorDato
    data class Almacenamiento(val causa: String?) : ErrorDato
}
