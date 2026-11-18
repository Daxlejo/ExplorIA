package com.daxlejo.datoscerca.ui.inicio

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.ui.componentes.BotonBorde
import com.daxlejo.datoscerca.ui.componentes.ColumnaPantalla
import com.daxlejo.datoscerca.ui.componentes.PulsoExplorador
import com.daxlejo.datoscerca.ui.theme.Duraciones
import com.daxlejo.datoscerca.ui.theme.Espaciado
import com.daxlejo.datoscerca.ui.theme.Tamanos
import com.daxlejo.datoscerca.ui.util.presentacion

@Composable
fun InicioRuta(
    viewModel: InicioViewModel,
    alAbrirDato: (Long) -> Unit,
    alAbrirHistorial: () -> Unit,
    alAbrirConfiguracion: () -> Unit,
    alSolicitarPermiso: () -> Unit = {},
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.efectos.collect { efecto ->
            when (efecto) {
                is EfectoInicio.AbrirDato -> alAbrirDato(efecto.id)
            }
        }
    }
    InicioPantalla(
        estado = estado,
        alPedirDato = viewModel::alPedirDato,
        alReanudar = viewModel::alReanudar,
        alAbrirHistorial = alAbrirHistorial,
        alAbrirConfiguracion = alAbrirConfiguracion,
        alSolicitarPermiso = alSolicitarPermiso,
    )
}

@Composable
fun InicioPantalla(
    estado: InicioUiState,
    alPedirDato: () -> Unit,
    alReanudar: () -> Unit,
    alAbrirHistorial: () -> Unit,
    alAbrirConfiguracion: () -> Unit,
    alSolicitarPermiso: () -> Unit = {},
) {
    ColumnaPantalla(
        botonBorde = { AccionPrincipal(estado, alPedirDato, alReanudar, alSolicitarPermiso) },
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FilaCentral(estado, alAbrirHistorial, alAbrirConfiguracion)
                Mensajes(estado)
            }
        }
    }
}

@Composable
private fun AccionPrincipal(
    estado: InicioUiState,
    alPedirDato: () -> Unit,
    alReanudar: () -> Unit,
    alSolicitarPermiso: () -> Unit,
) {
    when (estado) {
        InicioUiState.EnPausa ->
            BotonBorde(stringResource(R.string.accion_reanudar), alReanudar)
        is InicioUiState.Problema -> {
            val accion = if (estado.error == ErrorDato.SinPermiso) alSolicitarPermiso else alPedirDato
            BotonBorde(stringResource(estado.error.presentacion().accion), accion)
        }
        else -> BotonBorde(
            etiqueta = stringResource(R.string.accion_dato_ahora),
            alPulsar = alPedirDato,
            habilitado = estado is InicioUiState.Explorando,
        )
    }
}

/** El estado al centro, con el historial y la configuración a cada lado. */
@Composable
private fun FilaCentral(estado: InicioUiState, alAbrirHistorial: () -> Unit, alAbrirConfiguracion: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espaciado.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Acceso(R.drawable.ic_historial, stringResource(R.string.accion_abrir_historial), alAbrirHistorial)
        GlifoDeEstado(estado)
        Acceso(R.drawable.ic_ajustes, stringResource(R.string.accion_abrir_configuracion), alAbrirConfiguracion)
    }
}

@Composable
private fun GlifoDeEstado(estado: InicioUiState) {
    Crossfade(
        targetState = estado,
        animationSpec = tween(Duraciones.TRANSICION_ESTADO_MS),
        label = "glifo_estado",
    ) { actual ->
        Box(Modifier.size(Tamanos.glifoInicio), contentAlignment = Alignment.Center) {
            when (actual) {
                InicioUiState.Iniciando, InicioUiState.BuscandoDato ->
                    CircularProgressIndicator(Modifier.size(Tamanos.glifoEstado))
                InicioUiState.EnPausa -> PulsoExplorador(activo = false, icono = R.drawable.ic_pausa)
                is InicioUiState.Problema -> Icon(
                    painter = painterResource(actual.error.presentacion().icono),
                    contentDescription = null,
                    modifier = Modifier.size(Tamanos.glifoEstado),
                    tint = MaterialTheme.colorScheme.error,
                )
                is InicioUiState.Explorando -> PulsoExplorador(activo = true)
            }
        }
    }
}

@Composable
private fun Mensajes(estado: InicioUiState) {
    when (estado) {
        InicioUiState.Iniciando ->
            TituloYMensaje(stringResource(R.string.inicio_iniciando_titulo), stringResource(R.string.inicio_iniciando_mensaje))
        InicioUiState.BuscandoDato ->
            TituloYMensaje(stringResource(R.string.inicio_buscando_titulo), stringResource(R.string.inicio_buscando_mensaje))
        InicioUiState.EnPausa ->
            TituloYMensaje(stringResource(R.string.pausa_titulo), stringResource(R.string.pausa_mensaje))
        is InicioUiState.Problema -> estado.error.presentacion().let {
            TituloYMensaje(stringResource(it.titulo), stringResource(it.mensaje))
        }
        is InicioUiState.Explorando -> MensajesExplorando(estado)
    }
}

@Composable
private fun MensajesExplorando(estado: InicioUiState.Explorando) {
    TituloYMensaje(
        titulo = stringResource(R.string.inicio_explorando_titulo),
        mensaje = estado.cercaDe?.let { stringResource(R.string.inicio_cerca_de, it) }
            ?: stringResource(R.string.inicio_sin_lugar_cerca),
    )
    estado.aviso?.let { aviso ->
        Text(
            text = stringResource(R.string.inicio_aviso_automatico, stringResource(aviso.presentacion().titulo)),
            modifier = Modifier.padding(top = Espaciado.xs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TituloYMensaje(titulo: String, mensaje: String) {
    Text(
        text = titulo,
        modifier = Modifier.padding(top = Espaciado.xs),
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

@Composable
private fun Acceso(@DrawableRes icono: Int, descripcion: String, alPulsar: () -> Unit) {
    FilledTonalIconButton(onClick = alPulsar, modifier = Modifier.size(Tamanos.toqueMinimo)) {
        Icon(painterResource(icono), contentDescription = descripcion)
    }
}
