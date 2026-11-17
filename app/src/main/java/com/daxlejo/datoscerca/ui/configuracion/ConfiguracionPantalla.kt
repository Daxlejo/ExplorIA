package com.daxlejo.datoscerca.ui.configuracion

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.ui.componentes.ColumnaPantalla
import com.daxlejo.datoscerca.ui.componentes.IndicadorIntensidad
import com.daxlejo.datoscerca.ui.componentes.PantallaEstado
import com.daxlejo.datoscerca.ui.componentes.PuntoTipo
import com.daxlejo.datoscerca.ui.util.descripcion
import com.daxlejo.datoscerca.ui.util.etiqueta

/** Eventos que la pantalla Configuración envía al ViewModel. */
data class AccionesConfiguracion(
    val alAlternarTipo: (TipoDato) -> Unit = {},
    val alCambiarAleatorio: (Boolean) -> Unit = {},
    val alElegirIntensidad: (Intensidad) -> Unit = {},
    val alCambiarVoz: (Boolean) -> Unit = {},
    val alCambiarVibracion: (Boolean) -> Unit = {},
    val alCambiarPausa: (Boolean) -> Unit = {},
)

@Composable
fun ConfiguracionRuta(viewModel: ConfiguracionViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val acciones = remember(viewModel) {
        AccionesConfiguracion(
            alAlternarTipo = viewModel::alAlternarTipo,
            alCambiarAleatorio = viewModel::alCambiarAleatorio,
            alElegirIntensidad = viewModel::alElegirIntensidad,
            alCambiarVoz = viewModel::alCambiarVoz,
            alCambiarVibracion = viewModel::alCambiarVibracion,
            alCambiarPausa = viewModel::alCambiarPausa,
        )
    }
    ConfiguracionPantalla(estado, acciones)
}

@Composable
fun ConfiguracionPantalla(estado: ConfiguracionUiState, acciones: AccionesConfiguracion) {
    when (estado) {
        ConfiguracionUiState.Cargando -> PantallaEstado(
            titulo = stringResource(R.string.configuracion_titulo),
            mensaje = stringResource(R.string.configuracion_cargando),
        )
        is ConfiguracionUiState.Lista -> ListaConfiguracion(estado, acciones)
    }
}

@Composable
private fun ListaConfiguracion(estado: ConfiguracionUiState.Lista, acciones: AccionesConfiguracion) {
    val configuracion = estado.configuracion
    ColumnaPantalla { transformacion ->
        item {
            ListHeader(Modifier.transformedHeight(this, transformacion), transformation = SurfaceTransformation(transformacion)) {
                Text(stringResource(R.string.configuracion_titulo))
            }
        }
        if (estado.aviso == AvisoConfiguracion.FALLO_AL_GUARDAR) aviso(R.string.configuracion_error_guardado)
        seccionTipos(configuracion, estado.aviso == AvisoConfiguracion.MINIMO_UN_TIPO, acciones, transformacion)
        seccionIntensidad(configuracion.intensidad, acciones.alElegirIntensidad, transformacion)
        seccionAvisos(configuracion, estado.aviso == AvisoConfiguracion.VOZ_NO_DISPONIBLE, acciones, transformacion)
    }
}

private fun TransformingLazyColumnScope.seccionTipos(
    configuracion: Configuracion,
    avisoMinimo: Boolean,
    acciones: AccionesConfiguracion,
    transformacion: TransformationSpec,
) {
    subtitulo(R.string.configuracion_tipos, transformacion)
    items(TipoDato.entries.size, key = { TipoDato.entries[it].name }) { indice ->
        val tipo = TipoDato.entries[indice]
        CheckboxButton(
            checked = tipo in configuracion.tipos,
            onCheckedChange = { acciones.alAlternarTipo(tipo) },
            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformacion),
            transformation = SurfaceTransformation(transformacion),
            icon = { PuntoTipo(tipo) },
            label = { Text(stringResource(tipo.etiqueta())) },
        )
    }
    if (avisoMinimo) aviso(R.string.configuracion_minimo_un_tipo)
    item {
        SwitchButton(
            checked = configuracion.aleatorio,
            onCheckedChange = acciones.alCambiarAleatorio,
            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformacion),
            transformation = SurfaceTransformation(transformacion),
            label = { Text(stringResource(R.string.configuracion_aleatorio)) },
            secondaryLabel = { Text(stringResource(R.string.configuracion_aleatorio_descripcion)) },
        )
    }
}

private fun TransformingLazyColumnScope.seccionIntensidad(
    elegida: Intensidad,
    alElegir: (Intensidad) -> Unit,
    transformacion: TransformationSpec,
) {
    subtitulo(R.string.configuracion_intensidad, transformacion)
    items(Intensidad.entries.size, key = { Intensidad.entries[it].name }) { indice ->
        val nivel = Intensidad.entries[indice]
        RadioButton(
            selected = nivel == elegida,
            onSelect = { alElegir(nivel) },
            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformacion),
            transformation = SurfaceTransformation(transformacion),
            icon = { IndicadorIntensidad(nivel) },
            label = { Text(stringResource(nivel.etiqueta())) },
            secondaryLabel = { Text(stringResource(nivel.descripcion())) },
        )
    }
}

private fun TransformingLazyColumnScope.seccionAvisos(
    configuracion: Configuracion,
    sinVoz: Boolean,
    acciones: AccionesConfiguracion,
    transformacion: TransformationSpec,
) {
    subtitulo(R.string.configuracion_avisos, transformacion)
    interruptor(R.string.configuracion_voz, configuracion.vozActiva, acciones.alCambiarVoz, transformacion)
    if (sinVoz) aviso(R.string.configuracion_voz_no_disponible)
    interruptor(R.string.configuracion_vibracion, configuracion.vibracionActiva, acciones.alCambiarVibracion, transformacion)
    item {
        SwitchButton(
            checked = configuracion.pausa,
            onCheckedChange = acciones.alCambiarPausa,
            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformacion),
            transformation = SurfaceTransformation(transformacion),
            label = { Text(stringResource(R.string.configuracion_pausa)) },
            secondaryLabel = { Text(stringResource(R.string.configuracion_pausa_descripcion)) },
        )
    }
}

private fun TransformingLazyColumnScope.subtitulo(@StringRes texto: Int, transformacion: TransformationSpec) {
    item {
        ListSubHeader(
            modifier = Modifier.transformedHeight(this, transformacion),
            transformation = SurfaceTransformation(transformacion),
        ) { Text(stringResource(texto)) }
    }
}

private fun TransformingLazyColumnScope.interruptor(
    @StringRes etiqueta: Int,
    activo: Boolean,
    alCambiar: (Boolean) -> Unit,
    transformacion: TransformationSpec,
) {
    item {
        SwitchButton(
            checked = activo,
            onCheckedChange = alCambiar,
            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformacion),
            transformation = SurfaceTransformation(transformacion),
            label = { Text(stringResource(etiqueta)) },
        )
    }
}

private fun TransformingLazyColumnScope.aviso(@StringRes texto: Int) {
    item {
        Text(
            text = stringResource(texto),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}
