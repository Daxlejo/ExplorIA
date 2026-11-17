package com.daxlejo.datoscerca.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import com.daxlejo.datoscerca.domain.modelo.TipoDato

/** Paleta base. Fondo negro puro (OLED), teal como primario y ámbar solo para bombillo y favoritos. */
object Paleta {
    val Negro = Color(0xFF000000)
    val Teal = Color(0xFF1D9E75)
    val TealClaro = Color(0xFF5DCAA5)
    val TealProfundo = Color(0xFF0E3B2D)
    val Ambar = Color(0xFFFAC775)
    val AmbarProfundo = Color(0xFF3F2E07)
    val Texto = Color(0xFFEAF4F0)
    val TextoSuave = Color(0xFFA3B8B0)
    val Superficie = Color(0xFF17211D)
}

internal val EsquemaDatosCerca = ColorScheme(
    primary = Paleta.TealClaro,
    primaryDim = Paleta.Teal,
    primaryContainer = Paleta.TealProfundo,
    onPrimary = Color(0xFF00210F),
    onPrimaryContainer = Color(0xFFB8F0DC),
    secondary = Color(0xFF9CCFBD),
    secondaryDim = Color(0xFF7FB3A1),
    secondaryContainer = Color(0xFF1B3A30),
    onSecondary = Color(0xFF06241A),
    onSecondaryContainer = Color(0xFFCFEDE1),
    tertiary = Paleta.Ambar,
    tertiaryDim = Color(0xFFD9A856),
    tertiaryContainer = Paleta.AmbarProfundo,
    onTertiary = Color(0xFF2A1C00),
    onTertiaryContainer = Color(0xFFFFE2A8),
    surfaceContainerLow = Color(0xFF101815),
    surfaceContainer = Paleta.Superficie,
    surfaceContainerHigh = Color(0xFF222E29),
    onSurface = Paleta.Texto,
    onSurfaceVariant = Paleta.TextoSuave,
    outline = Color(0xFF7C928A),
    outlineVariant = Color(0xFF3B4A44),
    background = Paleta.Negro,
    onBackground = Paleta.Texto,
    error = Color(0xFFFFB4AB),
    errorDim = Color(0xFFE08A80),
    errorContainer = Color(0xFF5C1A14),
    onError = Color(0xFF3A0907),
    onErrorContainer = Color(0xFFFFDAD5),
)

/** Un color por tipo de dato: [acento] para texto e íconos sobre negro, [contenedor] para fondos. */
data class ColoresTipo(val acento: Color, val contenedor: Color)

fun TipoDato.colores(): ColoresTipo = when (this) {
    TipoDato.INTERESANTE -> ColoresTipo(Paleta.TealClaro, Paleta.TealProfundo)
    TipoDato.HISTORICO -> ColoresTipo(Color(0xFF8AB4F8), Color(0xFF14294A))
    TipoDato.COMICO -> ColoresTipo(Color(0xFFFF8FB0), Color(0xFF4A1528))
    TipoDato.CURIOSO -> ColoresTipo(Color(0xFFC6A0F6), Color(0xFF2F1D4D))
    TipoDato.GASTRONOMICO -> ColoresTipo(Color(0xFFFF8A65), Color(0xFF4A1E10))
}
