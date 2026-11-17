package com.daxlejo.datoscerca.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.ui.theme.Espaciado
import com.daxlejo.datoscerca.ui.theme.Tamanos
import com.daxlejo.datoscerca.ui.theme.TiposDatosCerca

/**
 * Aparición escalonada: el elemento entra entre [desde] y [hasta] del progreso total (0..1),
 * subiendo unos dp mientras gana opacidad.
 */
fun Modifier.entradaEscalonada(progreso: () -> Float, desde: Float, hasta: Float): Modifier = graphicsLayer {
    val local = ((progreso() - desde) / (hasta - desde)).coerceIn(0f, 1f)
    alpha = local
    translationY = (1f - local) * Tamanos.desplazamientoEntrada.toPx()
}

/** Cabecera del dato: bombillo que se enciende, tipo, lugar y distancia. */
@Composable
fun EncabezadoDato(
    tipo: TipoDato,
    lugar: String,
    distanciaMetros: Int,
    progreso: () -> Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
    ) {
        BombilloEncendido(encendido = { (progreso() / 0.45f).coerceIn(0f, 1f) })
        ChipTipoDato(tipo, Modifier.entradaEscalonada(progreso, desde = 0.15f, hasta = 0.55f))
        Text(
            text = lugar,
            modifier = Modifier.entradaEscalonada(progreso, desde = 0.3f, hasta = 0.7f),
            style = TiposDatosCerca.lugar,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.dato_distancia, distanciaMetros),
            modifier = Modifier.entradaEscalonada(progreso, desde = 0.4f, hasta = 0.8f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Cuerpo del dato: alineado a la izquierda para leerlo cómodo. */
@Composable
fun TextoDato(texto: String, progreso: () -> Float, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        modifier = modifier.fillMaxWidth().entradaEscalonada(progreso, desde = 0.55f, hasta = 1f),
        style = TiposDatosCerca.dato,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
