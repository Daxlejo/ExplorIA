package com.daxlejo.datoscerca.ui.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.TipoDato

/** Cómo se cuenta un problema en pantalla: qué pasó, cómo arreglarlo y con qué acción. */
data class PresentacionEstado(
    @DrawableRes val icono: Int,
    @StringRes val titulo: Int,
    @StringRes val mensaje: Int,
    @StringRes val accion: Int = R.string.accion_reintentar,
)

fun ErrorDato.presentacion(): PresentacionEstado = when (this) {
    ErrorDato.SinPermiso -> PresentacionEstado(
        R.drawable.ic_candado,
        R.string.error_permiso_titulo,
        R.string.error_permiso_mensaje,
        R.string.accion_permitir,
    )
    ErrorDato.SinGps ->
        PresentacionEstado(R.drawable.ic_gps, R.string.error_gps_titulo, R.string.error_gps_mensaje)
    ErrorDato.SinInternet ->
        PresentacionEstado(R.drawable.ic_sin_red, R.string.error_internet_titulo, R.string.error_internet_mensaje)
    ErrorDato.SinLugares ->
        PresentacionEstado(R.drawable.ic_brujula, R.string.error_lugares_titulo, R.string.error_lugares_mensaje)
    ErrorDato.EnPausa -> PresentacionEstado(
        R.drawable.ic_pausa,
        R.string.pausa_titulo,
        R.string.pausa_mensaje,
        R.string.accion_reanudar,
    )
    ErrorDato.GeneracionEnCurso ->
        PresentacionEstado(R.drawable.ic_bombillo, R.string.en_curso_titulo, R.string.en_curso_mensaje)
    ErrorDato.VozNoDisponible ->
        PresentacionEstado(R.drawable.ic_volumen, R.string.error_voz_titulo, R.string.error_voz_mensaje)
    is ErrorDato.FuenteNoDisponible ->
        PresentacionEstado(R.drawable.ic_sin_red, R.string.error_fuente_titulo, R.string.error_fuente_mensaje)
    is ErrorDato.ErrorIa ->
        PresentacionEstado(R.drawable.ic_alerta, R.string.error_ia_titulo, R.string.error_ia_mensaje)
    is ErrorDato.Almacenamiento ->
        PresentacionEstado(R.drawable.ic_alerta, R.string.error_guardado_titulo, R.string.error_guardado_mensaje)
}

@StringRes
fun TipoDato.etiqueta(): Int = when (this) {
    TipoDato.INTERESANTE -> R.string.tipo_interesante
    TipoDato.HISTORICO -> R.string.tipo_historico
    TipoDato.COMICO -> R.string.tipo_comico
    TipoDato.CURIOSO -> R.string.tipo_curioso
    TipoDato.GASTRONOMICO -> R.string.tipo_gastronomico
}

@StringRes
fun Intensidad.etiqueta(): Int = when (this) {
    Intensidad.SUAVE -> R.string.intensidad_suave
    Intensidad.NORMAL -> R.string.intensidad_normal
    Intensidad.INTENSO -> R.string.intensidad_intenso
}

@StringRes
fun Intensidad.descripcion(): Int = when (this) {
    Intensidad.SUAVE -> R.string.intensidad_suave_descripcion
    Intensidad.NORMAL -> R.string.intensidad_normal_descripcion
    Intensidad.INTENSO -> R.string.intensidad_intenso_descripcion
}
