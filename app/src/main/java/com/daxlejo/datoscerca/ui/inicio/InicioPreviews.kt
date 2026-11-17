package com.daxlejo.datoscerca.ui.inicio

import androidx.compose.runtime.Composable
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.ui.util.MarcoPreview
import com.daxlejo.datoscerca.ui.util.PreviewsWear

private val centroDePasto = Coordenada(1.21464, -77.27825)

@Composable
private fun Muestra(estado: InicioUiState) {
    MarcoPreview { InicioPantalla(estado, {}, {}, {}, {}) }
}

@PreviewsWear
@Composable
private fun InicioExplorandoPreview() =
    Muestra(InicioUiState.Explorando(centroDePasto, cercaDe = "Plaza de Nariño", aviso = null))

@PreviewsWear
@Composable
private fun InicioSinLugarCercaPreview() =
    Muestra(InicioUiState.Explorando(centroDePasto, cercaDe = null, aviso = ErrorDato.SinInternet))

@PreviewsWear
@Composable
private fun InicioIniciandoPreview() = Muestra(InicioUiState.Iniciando)

@PreviewsWear
@Composable
private fun InicioBuscandoPreview() = Muestra(InicioUiState.BuscandoDato)

@PreviewsWear
@Composable
private fun InicioEnPausaPreview() = Muestra(InicioUiState.EnPausa)

@PreviewsWear
@Composable
private fun InicioSinPermisoPreview() = Muestra(InicioUiState.Problema(ErrorDato.SinPermiso))

@PreviewsWear
@Composable
private fun InicioSinGpsPreview() = Muestra(InicioUiState.Problema(ErrorDato.SinGps))

@PreviewsWear
@Composable
private fun InicioSinInternetPreview() = Muestra(InicioUiState.Problema(ErrorDato.SinInternet))

@PreviewsWear
@Composable
private fun InicioSinLugaresPreview() = Muestra(InicioUiState.Problema(ErrorDato.SinLugares))

@PreviewsWear
@Composable
private fun InicioErrorIaPreview() = Muestra(InicioUiState.Problema(ErrorDato.ErrorIa(null)))
