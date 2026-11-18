package com.daxlejo.datoscerca.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Shapes

object Espaciado {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
}

object Tamanos {
    /** Área táctil mínima de cualquier control. */
    val toqueMinimo = 48.dp
    val iconoPequeno = 16.dp
    val iconoBoton = 20.dp
    val icono = 24.dp
    val glifoEstado = 40.dp
    val glifoInicio = 48.dp
    val puntoTipo = 8.dp
    val barraIntensidad = 4.dp
    val barraIntensidadBaja = 8.dp
    val barraIntensidadMedia = 13.dp
    val barraIntensidadAlta = 18.dp
    val grosorOnda = 1.5.dp
    val desplazamientoEntrada = 14.dp

    /** Fracción de la pantalla que se deja libre contra el borde redondo. */
    const val MARGEN_LATERAL = 0.08f
    const val MARGEN_VERTICAL = 0.17f

    /** Franja superior donde el contenido se desvanece bajo la hora. */
    const val VELO_SUPERIOR = 0.18f
    const val VELO_CORTE_SOLIDO = 0.45f
}

object Animaciones {
    const val ONDAS_EXPLORADOR = 2
    const val RADIO_MINIMO_ONDA = 0.22f
    const val RADIO_MAXIMO_ONDA = 0.5f
    const val ALFA_MAXIMA_ONDA = 0.7f

    const val ALFA_RESPLANDOR_BOMBILLO = 0.45f
    const val ESCALA_BASE_BOMBILLO = 0.6f
    const val ESCALA_RANGO_BOMBILLO = 0.4f
    const val ALFA_BASE_BOMBILLO = 0.3f
    const val ALFA_RANGO_BOMBILLO = 0.7f

    const val ESCALA_REBOTE_FAVORITO = 1.4f

    const val BOMBILLO_HASTA = 0.45f
    const val CHIP_DESDE = 0.15f
    const val CHIP_HASTA = 0.55f
    const val LUGAR_DESDE = 0.3f
    const val LUGAR_HASTA = 0.7f
    const val DISTANCIA_DESDE = 0.4f
    const val DISTANCIA_HASTA = 0.8f
    const val TEXTO_DESDE = 0.55f
    const val TEXTO_HASTA = 1f
    const val ACCIONES_DESDE = 0.65f
    const val ACCIONES_HASTA = 1f
}

internal val FormasDatosCerca = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Estilos propios que complementan la tipografía de Material.
 * El nombre del lugar va en serif, como la placa de un sitio; el dato, en sans para leer de un vistazo.
 */
object TiposDatosCerca {
    val lugar = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    )
    val lugarCompacto = lugar.copy(fontSize = 16.sp, lineHeight = 20.sp)
    val dato = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
}

object Duraciones {
    const val ENTRADA_DATO_MS = 900
    const val PULSO_EXPLORADOR_MS = 2600
    const val REBOTE_FAVORITO_MS = 320
    const val TRANSICION_ESTADO_MS = 240
}
