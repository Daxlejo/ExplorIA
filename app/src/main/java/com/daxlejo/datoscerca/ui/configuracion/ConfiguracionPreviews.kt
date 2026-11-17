package com.daxlejo.datoscerca.ui.configuracion

import androidx.compose.runtime.Composable
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.ui.util.MarcoPreview
import com.daxlejo.datoscerca.ui.util.PreviewsWear

@Composable
private fun Muestra(estado: ConfiguracionUiState) {
    MarcoPreview { ConfiguracionPantalla(estado, AccionesConfiguracion()) }
}

@PreviewsWear
@Composable
private fun ConfiguracionPorDefectoPreview() = Muestra(ConfiguracionUiState.Lista(Configuracion.PorDefecto))

@PreviewsWear
@Composable
private fun ConfiguracionUltimoTipoPreview() = Muestra(
    ConfiguracionUiState.Lista(
        configuracion = Configuracion.PorDefecto.copy(
            tipos = setOf(TipoDato.COMICO),
            aleatorio = true,
            intensidad = Intensidad.INTENSO,
        ),
        aviso = AvisoConfiguracion.MINIMO_UN_TIPO,
    ),
)

@PreviewsWear
@Composable
private fun ConfiguracionFalloAlGuardarPreview() =
    Muestra(ConfiguracionUiState.Lista(Configuracion.PorDefecto, AvisoConfiguracion.FALLO_AL_GUARDAR))

@PreviewsWear
@Composable
private fun ConfiguracionCargandoPreview() = Muestra(ConfiguracionUiState.Cargando)
