package com.daxlejo.datoscerca.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.ui.theme.Espaciado
import com.daxlejo.datoscerca.ui.theme.Tamanos
import com.daxlejo.datoscerca.ui.theme.TiposDatosCerca
import com.daxlejo.datoscerca.ui.theme.colores
import com.daxlejo.datoscerca.ui.util.etiqueta

/** Entrada del historial: lugar, comienzo del dato, tipo y hora. La estrella marca los favoritos. */
@Composable
fun FilaHistorial(
    lugar: String,
    texto: String,
    tipo: TipoDato,
    hora: String,
    favorito: Boolean,
    alAbrir: () -> Unit,
    modifier: Modifier = Modifier,
    transformacion: SurfaceTransformation? = null,
) {
    Card(onClick = alAbrir, modifier = modifier.fillMaxWidth(), transformation = transformacion) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            Text(
                text = lugar,
                modifier = Modifier.weight(1f),
                style = TiposDatosCerca.lugarCompacto,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (favorito) EstrellaFavorito()
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        PieFila(tipo, hora)
    }
}

@Composable
private fun EstrellaFavorito() {
    Icon(
        painter = painterResource(R.drawable.ic_estrella),
        contentDescription = stringResource(R.string.historial_favorito_descripcion),
        modifier = Modifier.size(Tamanos.iconoPequeno),
        tint = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun PieFila(tipo: TipoDato, hora: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espaciado.xs),
    ) {
        PuntoTipo(tipo)
        Text(
            text = stringResource(tipo.etiqueta()),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = tipo.colores().acento,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = hora,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
