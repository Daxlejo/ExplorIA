package com.daxlejo.datoscerca.ui.dato

import androidx.compose.runtime.Composable
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.ui.util.MarcoPreview
import com.daxlejo.datoscerca.ui.util.PreviewsWear

private val datoDeMuestra = DatoUiState.Visible(
    id = 1,
    lugar = "Catedral de Pasto",
    texto = "La construcción de la catedral empezó en 1899 y terminó en 1920. " +
        "Fueron más de veinte años de obra para levantar sus tres naves de ladrillo.",
    tipo = TipoDato.HISTORICO,
    distanciaMetros = 120,
    favorito = false,
    accion = AccionDato.NINGUNA,
    aviso = null,
)

@Composable
private fun Muestra(estado: DatoUiState) {
    MarcoPreview { DatoPantalla(estado, AccionesDato(), animarEntrada = false) }
}

@PreviewsWear
@Composable
private fun DatoVisiblePreview() = Muestra(datoDeMuestra)

@PreviewsWear
@Composable
private fun DatoFavoritoComicoPreview() = Muestra(
    datoDeMuestra.copy(
        tipo = TipoDato.COMICO,
        texto = "La catedral tardó veintiún años en construirse; tu remodelación ya no parece tan lenta.",
        favorito = true,
    ),
)

@PreviewsWear
@Composable
private fun DatoBuscandoOtroPreview() = Muestra(datoDeMuestra.copy(accion = AccionDato.BUSCANDO_OTRO))

@PreviewsWear
@Composable
private fun DatoVozNoDisponiblePreview() = Muestra(datoDeMuestra.copy(aviso = ErrorDato.VozNoDisponible))

@PreviewsWear
@Composable
private fun DatoConfirmandoBorradoPreview() = Muestra(datoDeMuestra.copy(accion = AccionDato.CONFIRMANDO_BORRADO))

@PreviewsWear
@Composable
private fun DatoCargandoPreview() = Muestra(DatoUiState.Cargando)

@PreviewsWear
@Composable
private fun DatoNoDisponiblePreview() = Muestra(DatoUiState.NoDisponible)

@PreviewsWear
@Composable
private fun DatoSinInternetPreview() = Muestra(DatoUiState.Problema(ErrorDato.SinInternet))

@PreviewsWear
@Composable
private fun DatoErrorIaPreview() = Muestra(DatoUiState.Problema(ErrorDato.ErrorIa(null)))

@PreviewsWear
@Composable
private fun DatoSinLugaresPreview() = Muestra(DatoUiState.Problema(ErrorDato.SinLugares))
