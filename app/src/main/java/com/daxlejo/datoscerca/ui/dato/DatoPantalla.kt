package com.daxlejo.datoscerca.ui.dato

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.daxlejo.datoscerca.R
import com.daxlejo.datoscerca.ui.componentes.BotonBorde
import com.daxlejo.datoscerca.ui.componentes.BotonFavorito
import com.daxlejo.datoscerca.ui.componentes.ColumnaPantalla
import com.daxlejo.datoscerca.ui.componentes.EncabezadoDato
import com.daxlejo.datoscerca.ui.componentes.PantallaEstado
import com.daxlejo.datoscerca.ui.componentes.PantallaProblema
import com.daxlejo.datoscerca.ui.componentes.TextoDato
import com.daxlejo.datoscerca.ui.theme.Duraciones
import com.daxlejo.datoscerca.ui.theme.Espaciado
import com.daxlejo.datoscerca.ui.theme.Tamanos
import com.daxlejo.datoscerca.ui.util.presentacion

/** Acciones que la pantalla Dato envía al ViewModel. */
data class AccionesDato(
    val alEscuchar: () -> Unit = {},
    val alAlternarFavorito: () -> Unit = {},
    val alPedirOtro: () -> Unit = {},
    val alPedirBorrado: () -> Unit = {},
    val alCancelarBorrado: () -> Unit = {},
    val alConfirmarBorrado: () -> Unit = {},
    val alDescartarProblema: () -> Unit = {},
    val alCerrar: () -> Unit = {},
)

@Composable
fun DatoRuta(viewModel: DatoViewModel, alCerrar: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.efectos.collect { efecto ->
            when (efecto) {
                EfectoDato.Cerrar -> alCerrar()
            }
        }
    }
    val acciones = remember(viewModel, alCerrar) {
        AccionesDato(
            alEscuchar = viewModel::alEscuchar,
            alAlternarFavorito = viewModel::alAlternarFavorito,
            alPedirOtro = viewModel::alPedirOtro,
            alPedirBorrado = viewModel::alPedirBorrado,
            alCancelarBorrado = viewModel::alCancelarBorrado,
            alConfirmarBorrado = viewModel::alConfirmarBorrado,
            alDescartarProblema = viewModel::alDescartarProblema,
            alCerrar = alCerrar,
        )
    }
    DatoPantalla(estado, acciones)
}

@Composable
fun DatoPantalla(estado: DatoUiState, acciones: AccionesDato, animarEntrada: Boolean = true) {
    when (estado) {
        DatoUiState.Cargando -> PantallaEstado(
            titulo = stringResource(R.string.app_nombre),
            mensaje = stringResource(R.string.dato_cargando),
        )
        DatoUiState.NoDisponible -> PantallaEstado(
            titulo = stringResource(R.string.dato_no_disponible_titulo),
            mensaje = stringResource(R.string.dato_no_disponible_mensaje),
            icono = R.drawable.ic_historial,
            tinte = MaterialTheme.colorScheme.onSurfaceVariant,
            etiquetaAccion = stringResource(R.string.accion_volver),
            alAccionar = acciones.alCerrar,
        )
        is DatoUiState.Problema -> PantallaProblema(
            presentacion = estado.error.presentacion(),
            alAccionar = acciones.alPedirOtro,
            etiquetaSecundaria = stringResource(R.string.accion_volver_al_dato),
            alAccionSecundaria = acciones.alDescartarProblema,
        )
        // Cada dato estrena su propia lista: así "Otro dato" vuelve a empezar arriba.
        is DatoUiState.Visible -> key(estado.id) { DatoVisible(estado, acciones, animarEntrada) }
    }
}

@Composable
private fun DatoVisible(estado: DatoUiState.Visible, acciones: AccionesDato, animarEntrada: Boolean) {
    // Un único momento animado por dato: el bombillo se enciende y el contenido entra en cascada.
    val entrada = remember(estado.id) { Animatable(if (animarEntrada) 0f else 1f) }
    LaunchedEffect(estado.id) {
        entrada.animateTo(1f, tween(Duraciones.ENTRADA_DATO_MS, easing = FastOutSlowInEasing))
    }
    val progreso = { entrada.value }

    ColumnaPantalla(botonBorde = { BotonOtroDato(estado.accion == AccionDato.BUSCANDO_OTRO, acciones.alPedirOtro) }) {
        item { EncabezadoDato(estado.tipo, estado.lugar, estado.distanciaMetros, progreso) }
        item { TextoDato(estado.texto, progreso, Modifier.padding(top = Espaciado.s)) }
        item { AccionesSecundarias(estado.favorito, acciones) }
        estado.aviso?.let { aviso ->
            item {
                Text(
                    text = stringResource(aviso.presentacion().mensaje),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    DialogoBorrado(estado.accion == AccionDato.CONFIRMANDO_BORRADO, acciones)
}

@Composable
private fun BotonOtroDato(buscando: Boolean, alPedirOtro: () -> Unit) {
    BotonBorde(
        etiqueta = stringResource(if (buscando) R.string.dato_buscando_otro else R.string.accion_otro_dato),
        alPulsar = alPedirOtro,
        habilitado = !buscando,
    )
}

@Composable
private fun AccionesSecundarias(favorito: Boolean, acciones: AccionesDato, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = Espaciado.s),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = acciones.alEscuchar, modifier = Modifier.size(Tamanos.toqueMinimo)) {
            Icon(painterResource(R.drawable.ic_volumen), stringResource(R.string.accion_escuchar))
        }
        BotonFavorito(favorito, acciones.alAlternarFavorito)
        IconButton(onClick = acciones.alPedirBorrado, modifier = Modifier.size(Tamanos.toqueMinimo)) {
            Icon(
                painter = painterResource(R.drawable.ic_borrar),
                contentDescription = stringResource(R.string.accion_borrar),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DialogoBorrado(visible: Boolean, acciones: AccionesDato) {
    AlertDialog(
        visible = visible,
        onDismissRequest = acciones.alCancelarBorrado,
        confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = acciones.alConfirmarBorrado) },
        title = { Text(stringResource(R.string.dato_borrar_titulo), textAlign = TextAlign.Center) },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = acciones.alCancelarBorrado) },
        text = { Text(stringResource(R.string.dato_borrar_mensaje), textAlign = TextAlign.Center) },
    )
}
