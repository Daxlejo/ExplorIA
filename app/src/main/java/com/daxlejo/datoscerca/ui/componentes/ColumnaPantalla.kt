package com.daxlejo.datoscerca.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import com.daxlejo.datoscerca.ui.theme.Tamanos
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec

/**
 * Las superficies (botones, tarjetas, encabezados) reciben el [TransformationSpec] para deformarse
 * contra el borde; el texto suelto no lo usa, porque encogerlo lo montaría sobre el elemento vecino.
 *
 * Esqueleto común de pantalla: ScreenScaffold (hora, indicador de scroll, márgenes seguros para
 * pantalla redonda) con una columna que se adapta al borde y, si se pide, un botón de borde.
 */
@Composable
fun ColumnaPantalla(
    modifier: Modifier = Modifier,
    botonBorde: (@Composable BoxScope.() -> Unit)? = null,
    contenido: TransformingLazyColumnScope.(TransformationSpec) -> Unit,
) {
    val estado = rememberTransformingLazyColumnState()
    val transformacion = rememberTransformationSpec()
    val margenesSeguros = margenesPantallaRedonda()
    if (botonBorde == null) {
        ScreenScaffold(scrollState = estado, modifier = modifier, contentPadding = margenesSeguros) { margenes ->
            Columna(estado, margenes) { contenido(transformacion) }
            VeloSuperior()
        }
    } else {
        ScreenScaffold(
            scrollState = estado,
            edgeButton = botonBorde,
            modifier = modifier,
            contentPadding = margenesSeguros,
        ) { margenes ->
            Columna(estado, margenes) { contenido(transformacion) }
            VeloSuperior()
        }
    }
}

// En una pantalla redonda el ancho útil se pierde arriba y abajo: los márgenes van en proporción al tamaño.
@Composable
private fun margenesPantallaRedonda(): PaddingValues {
    val pantalla = LocalConfiguration.current
    return PaddingValues(
        horizontal = (pantalla.screenWidthDp * Tamanos.MARGEN_LATERAL).dp,
        vertical = (pantalla.screenHeightDp * Tamanos.MARGEN_VERTICAL).dp,
    )
}

// La hora vuelve a mostrarse cuando el scroll se detiene: el contenido se desvanece bajo ella para no chocar.
@Composable
private fun BoxScope.VeloSuperior() {
    val alto = (LocalConfiguration.current.screenHeightDp * Tamanos.VELO_SUPERIOR).dp
    val fondo = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(alto)
            .background(Brush.verticalGradient(0f to fondo, Tamanos.VELO_CORTE_SOLIDO to fondo, 1f to Color.Transparent)),
    )
}

@Composable
private fun Columna(
    estado: TransformingLazyColumnState,
    margenes: PaddingValues,
    contenido: TransformingLazyColumnScope.() -> Unit,
) {
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = estado,
        contentPadding = margenes,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = contenido,
    )
}

/** Acción principal de la pantalla, pegada al borde inferior curvo. Solo texto: el botón es estrecho y en pantallas pequeñas la etiqueta ocupa dos líneas. */
@Composable
fun BotonBorde(
    etiqueta: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    EdgeButton(onClick = alPulsar, modifier = modifier, enabled = habilitado) {
        Text(etiqueta, textAlign = TextAlign.Center)
    }
}
