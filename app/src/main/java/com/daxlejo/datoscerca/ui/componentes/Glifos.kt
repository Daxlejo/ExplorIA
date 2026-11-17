package com.daxlejo.datoscerca.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.ui.theme.Duraciones
import com.daxlejo.datoscerca.ui.theme.Tamanos

private const val ONDAS = 2

/** Pin con ondas que se expanden mientras la app explora; quieto cuando no hay exploración. */
@Composable
fun PulsoExplorador(activo: Boolean, modifier: Modifier = Modifier, @DrawableRes icono: Int = R.drawable.ic_pin) {
    val color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Box(modifier.size(Tamanos.glifoInicio), contentAlignment = Alignment.Center) {
        if (activo) OndasExpansivas(color)
        Icon(
            painter = painterResource(icono),
            contentDescription = null,
            modifier = Modifier.size(Tamanos.icono),
            tint = color,
        )
    }
}

@Composable
private fun OndasExpansivas(color: Color) {
    val transicion = rememberInfiniteTransition(label = "pulso")
    val fase by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Duraciones.PULSO_EXPLORADOR_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "fase",
    )
    Canvas(Modifier.fillMaxSize()) {
        repeat(ONDAS) { indice -> onda(color, avance = (fase + indice.toFloat() / ONDAS) % 1f) }
    }
}

private fun DrawScope.onda(color: Color, avance: Float) {
    val radioMinimo = size.minDimension * 0.22f
    val radioMaximo = size.minDimension * 0.5f - Tamanos.grosorOnda.toPx()
    drawCircle(
        color = color.copy(alpha = (1f - avance) * 0.7f),
        radius = radioMinimo + (radioMaximo - radioMinimo) * avance,
        style = Stroke(Tamanos.grosorOnda.toPx()),
    )
}

/** Bombillo ámbar con resplandor: [encendido] va de 0 (apagado) a 1 (encendido del todo). */
@Composable
fun BombilloEncendido(encendido: () -> Float, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.tertiary
    Box(modifier.size(Tamanos.glifoEstado), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f * encendido()), Color.Transparent)))
        }
        Icon(
            painter = painterResource(R.drawable.ic_bombillo),
            contentDescription = null,
            modifier = Modifier.size(Tamanos.icono).graphicsLayer {
                val escala = 0.6f + 0.4f * encendido()
                scaleX = escala
                scaleY = escala
                alpha = 0.3f + 0.7f * encendido()
            },
            tint = color,
        )
    }
}
