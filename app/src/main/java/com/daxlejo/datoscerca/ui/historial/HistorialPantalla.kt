package com.daxlejo.datoscerca.ui.historial

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.ui.componentes.BloqueEstado
import com.daxlejo.datoscerca.ui.componentes.ColumnaPantalla
import com.daxlejo.datoscerca.ui.componentes.FilaHistorial
import com.daxlejo.datoscerca.ui.componentes.PantallaEstado

@Composable
fun HistorialRuta(viewModel: HistorialViewModel, alAbrirDato: (Long) -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    HistorialPantalla(estado, viewModel::alCambiarSoloFavoritos, alAbrirDato)
}

@Composable
fun HistorialPantalla(
    estado: HistorialUiState,
    alCambiarSoloFavoritos: (Boolean) -> Unit,
    alAbrirDato: (Long) -> Unit,
) {
    when (estado) {
        HistorialUiState.Cargando -> PantallaEstado(
            titulo = stringResource(R.string.historial_titulo),
            mensaje = stringResource(R.string.historial_cargando),
        )
        HistorialUiState.Vacio -> PantallaEstado(
            titulo = stringResource(R.string.historial_vacio_titulo),
            mensaje = stringResource(R.string.historial_vacio_mensaje),
            icono = R.drawable.ic_bombillo,
            tinte = MaterialTheme.colorScheme.tertiary,
        )
        is HistorialUiState.Lista -> ListaHistorial(estado, alCambiarSoloFavoritos, alAbrirDato)
    }
}

@Composable
private fun ListaHistorial(
    estado: HistorialUiState.Lista,
    alCambiarSoloFavoritos: (Boolean) -> Unit,
    alAbrirDato: (Long) -> Unit,
) {
    ColumnaPantalla { transformacion ->
        item {
            ListHeader(Modifier.transformedHeight(this, transformacion), transformation = SurfaceTransformation(transformacion)) {
                Text(stringResource(R.string.historial_titulo))
            }
        }
        item {
            SwitchButton(
                checked = estado.soloFavoritos,
                onCheckedChange = alCambiarSoloFavoritos,
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformacion),
                transformation = SurfaceTransformation(transformacion),
                label = { Text(stringResource(R.string.historial_solo_favoritos)) },
            )
        }
        if (estado.grupos.isEmpty()) sinFavoritos()
        estado.grupos.forEach { grupo -> grupoDia(grupo, alAbrirDato, transformacion) }
    }
}

private fun TransformingLazyColumnScope.sinFavoritos() {
    item {
        BloqueEstado(
            titulo = stringResource(R.string.historial_sin_favoritos_titulo),
            mensaje = stringResource(R.string.historial_sin_favoritos_mensaje),
            icono = R.drawable.ic_estrella_borde,
            tinte = MaterialTheme.colorScheme.tertiary,
        )
    }
}

private fun TransformingLazyColumnScope.grupoDia(
    grupo: GrupoDia,
    alAbrirDato: (Long) -> Unit,
    transformacion: TransformationSpec,
) {
    item {
        ListSubHeader(
            modifier = Modifier.transformedHeight(this, transformacion),
            transformation = SurfaceTransformation(transformacion),
        ) { Text(etiquetaDia(grupo.dia)) }
    }
    items(grupo.filas.size, key = { grupo.filas[it].id }) { indice ->
        val fila = grupo.filas[indice]
        FilaHistorial(
            lugar = fila.lugar,
            texto = fila.texto,
            tipo = fila.tipo,
            hora = fila.hora,
            favorito = fila.favorito,
            alAbrir = { alAbrirDato(fila.id) },
            modifier = Modifier.transformedHeight(this, transformacion),
            transformacion = SurfaceTransformation(transformacion),
        )
    }
}

@Composable
private fun etiquetaDia(dia: DiaHistorial): String = when (dia) {
    DiaHistorial.Hoy -> stringResource(R.string.historial_hoy)
    DiaHistorial.Ayer -> stringResource(R.string.historial_ayer)
    is DiaHistorial.Fecha -> dia.texto
}
