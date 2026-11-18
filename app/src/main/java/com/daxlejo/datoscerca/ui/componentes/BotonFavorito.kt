package com.daxlejo.datoscerca.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.ui.theme.Animaciones
import com.daxlejo.datoscerca.ui.theme.Duraciones
import com.daxlejo.datoscerca.ui.theme.Tamanos

/** Estrella de favorito: al marcarla rebota y da un toque háptico. */
@Composable
fun BotonFavorito(favorito: Boolean, alAlternar: () -> Unit, modifier: Modifier = Modifier) {
    val haptica = LocalHapticFeedback.current
    val escala = remember { Animatable(1f) }
    var anterior by remember { mutableStateOf(favorito) }

    LaunchedEffect(favorito) {
        if (favorito && !anterior) {
            escala.animateTo(Animaciones.ESCALA_REBOTE_FAVORITO, tween(Duraciones.REBOTE_FAVORITO_MS / 2))
            escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        anterior = favorito
    }

    IconButton(
        onClick = {
            haptica.performHapticFeedback(HapticFeedbackType.LongPress)
            alAlternar()
        },
        modifier = modifier.size(Tamanos.toqueMinimo),
    ) {
        Icon(
            painter = painterResource(if (favorito) R.drawable.ic_estrella else R.drawable.ic_estrella_borde),
            contentDescription = stringResource(
                if (favorito) R.string.accion_quitar_favorito else R.string.accion_marcar_favorito,
            ),
            modifier = Modifier.graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            },
            tint = if (favorito) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
