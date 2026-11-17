package com.daxlejo.datoscerca.ui.historial

import androidx.compose.runtime.Composable
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.ui.util.MarcoPreview
import com.daxlejo.datoscerca.ui.util.PreviewsWear

private val gruposDeMuestra = listOf(
    GrupoDia(
        dia = DiaHistorial.Hoy,
        filas = listOf(
            FilaHistorialUi(
                id = 3,
                lugar = "Museo del Oro Nariño",
                texto = "Entre las piezas hay discos de oro hechos para girar y jugar con la luz.",
                tipo = TipoDato.CURIOSO,
                hora = "3:45 p. m.",
                favorito = true,
            ),
            FilaHistorialUi(
                id = 2,
                lugar = "Plaza de Nariño",
                texto = "Cerca de la plaza se toman hervidos: una bebida caliente de fruta con aguardiente.",
                tipo = TipoDato.GASTRONOMICO,
                hora = "3:20 p. m.",
                favorito = false,
            ),
        ),
    ),
    GrupoDia(
        dia = DiaHistorial.Ayer,
        filas = listOf(
            FilaHistorialUi(
                id = 1,
                lugar = "Catedral de Pasto",
                texto = "La construcción de la catedral empezó en 1899 y terminó en 1920.",
                tipo = TipoDato.HISTORICO,
                hora = "10:05 a. m.",
                favorito = false,
            ),
        ),
    ),
)

@Composable
private fun Muestra(estado: HistorialUiState) {
    MarcoPreview { HistorialPantalla(estado, {}, {}) }
}

@PreviewsWear
@Composable
private fun HistorialConDatosPreview() = Muestra(HistorialUiState.Lista(soloFavoritos = false, gruposDeMuestra))

@PreviewsWear
@Composable
private fun HistorialSinFavoritosPreview() = Muestra(HistorialUiState.Lista(soloFavoritos = true, emptyList()))

@PreviewsWear
@Composable
private fun HistorialVacioPreview() = Muestra(HistorialUiState.Vacio)

@PreviewsWear
@Composable
private fun HistorialCargandoPreview() = Muestra(HistorialUiState.Cargando)
