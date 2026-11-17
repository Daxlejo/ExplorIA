package com.daxlejo.datoscerca.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import com.daxlejo.datoscerca.ui.theme.Espaciado
import com.daxlejo.datoscerca.ui.theme.Tamanos
import com.daxlejo.datoscerca.ui.util.PresentacionEstado

/** Ícono (o progreso), título y mensaje centrados: el bloque con el que se cuenta cualquier estado. */
@Composable
fun BloqueEstado(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    tinte: Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
    ) {
        if (icono == null) {
            CircularProgressIndicator(Modifier.size(Tamanos.glifoEstado))
        } else {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(Tamanos.glifoEstado), tint = tinte)
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Pantalla completa para estados vacíos, de carga o de error, con una acción opcional en el borde. */
@Composable
fun PantallaEstado(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    tinte: Color = MaterialTheme.colorScheme.primary,
    etiquetaAccion: String? = null,
    alAccionar: () -> Unit = {},
    etiquetaSecundaria: String? = null,
    alAccionSecundaria: () -> Unit = {},
) {
    ColumnaPantalla(
        modifier = modifier,
        botonBorde = etiquetaAccion?.let { etiqueta -> { BotonBorde(etiqueta, alAccionar) } },
    ) { transformacion ->
        item { BloqueEstado(titulo, mensaje, icono = icono, tinte = tinte) }
        if (etiquetaSecundaria != null) {
            item {
                FilledTonalButton(
                    onClick = alAccionSecundaria,
                    modifier = Modifier.padding(top = Espaciado.s).transformedHeight(this, transformacion),
                    transformation = SurfaceTransformation(transformacion),
                ) { Text(etiquetaSecundaria) }
            }
        }
    }
}

/** Variante para errores del dominio: usa su ícono, textos y acción, en color de error. */
@Composable
fun PantallaProblema(
    presentacion: PresentacionEstado,
    alAccionar: () -> Unit,
    modifier: Modifier = Modifier,
    etiquetaSecundaria: String? = null,
    alAccionSecundaria: () -> Unit = {},
) {
    PantallaEstado(
        titulo = stringResource(presentacion.titulo),
        mensaje = stringResource(presentacion.mensaje),
        modifier = modifier,
        icono = presentacion.icono,
        tinte = MaterialTheme.colorScheme.error,
        etiquetaAccion = stringResource(presentacion.accion),
        alAccionar = alAccionar,
        etiquetaSecundaria = etiquetaSecundaria,
        alAccionSecundaria = alAccionSecundaria,
    )
}
