package com.daxlejo.datoscerca.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.ui.theme.Espaciado
import com.daxlejo.datoscerca.ui.theme.Tamanos
import com.daxlejo.datoscerca.ui.theme.colores
import com.daxlejo.datoscerca.ui.util.etiqueta

/** Punto con el color del tipo de dato. */
@Composable
fun PuntoTipo(tipo: TipoDato, modifier: Modifier = Modifier) {
    Box(modifier.size(Tamanos.puntoTipo).background(tipo.colores().acento, CircleShape))
}

/** Etiqueta del tipo de dato: píldora con el color propio del tipo. */
@Composable
fun ChipTipoDato(tipo: TipoDato, modifier: Modifier = Modifier) {
    val colores = tipo.colores()
    Row(
        modifier = modifier
            .background(colores.contenedor, CircleShape)
            .padding(horizontal = Espaciado.m, vertical = Espaciado.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PuntoTipo(tipo)
        Spacer(Modifier.width(Espaciado.s))
        Text(
            text = stringResource(tipo.etiqueta()),
            style = MaterialTheme.typography.labelMedium,
            color = colores.acento,
        )
    }
}

/** Tres barras crecientes: se encienden tantas como el nivel de intensidad. */
@Composable
fun IndicadorIntensidad(
    intensidad: Intensidad,
    modifier: Modifier = Modifier,
    colorActivo: Color = MaterialTheme.colorScheme.primary,
    colorInactivo: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val descripcion = stringResource(
        R.string.intensidad_descripcion_accesible,
        stringResource(intensidad.etiqueta()),
    )
    Row(
        modifier = modifier.semantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(Espaciado.xxs),
        verticalAlignment = Alignment.Bottom,
    ) {
        Intensidad.entries.forEach { nivel ->
            Box(
                Modifier
                    .width(Tamanos.barraIntensidad)
                    .height(alturaBarra(nivel))
                    .background(if (nivel <= intensidad) colorActivo else colorInactivo, CircleShape),
            )
        }
    }
}

private fun alturaBarra(nivel: Intensidad): Dp = when (nivel) {
    Intensidad.SUAVE -> Tamanos.barraIntensidadBaja
    Intensidad.NORMAL -> Tamanos.barraIntensidadMedia
    Intensidad.INTENSO -> Tamanos.barraIntensidadAlta
}
