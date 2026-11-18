package com.daxlejo.datoscerca.ui.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxlejo.datoscerca.domain.casouso.GuardarConfiguracion
import com.daxlejo.datoscerca.domain.casouso.ObservarConfiguracion
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.puerto.Narrador
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Aviso que acompaña al último cambio; como mucho hay uno a la vez. */
enum class AvisoConfiguracion { MINIMO_UN_TIPO, VOZ_NO_DISPONIBLE, FALLO_AL_GUARDAR }

sealed interface ConfiguracionUiState {
    data object Cargando : ConfiguracionUiState
    data class Lista(val configuracion: Configuracion, val aviso: AvisoConfiguracion? = null) : ConfiguracionUiState
}

class ConfiguracionViewModel(
    observarConfiguracion: ObservarConfiguracion,
    private val guardarConfiguracion: GuardarConfiguracion,
    private val narrador: Narrador,
    private val alPrecargarDemo: () -> Unit = {},
) : ViewModel() {

    private val aviso = MutableStateFlow<AvisoConfiguracion?>(null)

    val uiState: StateFlow<ConfiguracionUiState> =
        combine(observarConfiguracion(), aviso) { configuracion, actual ->
            ConfiguracionUiState.Lista(configuracion, actual)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfiguracionUiState.Cargando)

    fun alAlternarTipo(tipo: TipoDato) {
        val actual = (uiState.value as? ConfiguracionUiState.Lista)?.configuracion ?: return
        if (actual.puedeDesmarcar(tipo)) {
            guardar { it.conTipoAlternado(tipo) }
        } else {
            aviso.value = AvisoConfiguracion.MINIMO_UN_TIPO
        }
    }

    fun alCambiarAleatorio(activo: Boolean) = guardar { it.copy(aleatorio = activo) }

    fun alElegirIntensidad(intensidad: Intensidad) = guardar { it.copy(intensidad = intensidad) }

    // Al activar la voz se prueba en el momento: si el reloj no tiene voz en español, se avisa aquí.
    fun alCambiarVoz(activa: Boolean) {
        val sinVoz = activa && narrador.hablar(FRASE_DE_PRUEBA) is Resultado.Fallo
        guardar(avisoSiSale = AvisoConfiguracion.VOZ_NO_DISPONIBLE.takeIf { sinVoz }) { it.copy(vozActiva = activa) }
    }

    fun alCambiarVibracion(activa: Boolean) = guardar { it.copy(vibracionActiva = activa) }

    fun alCambiarPausa(activa: Boolean) = guardar { it.copy(pausa = activa) }

    fun alCambiarModoDemo(activo: Boolean) {
        if (activo) alPrecargarDemo()
        guardar { it.copy(modoDemo = activo) }
    }

    private fun guardar(avisoSiSale: AvisoConfiguracion? = null, cambio: (Configuracion) -> Configuracion) {
        viewModelScope.launch {
            aviso.value = when (guardarConfiguracion(cambio)) {
                is Resultado.Exito -> avisoSiSale
                is Resultado.Fallo -> AvisoConfiguracion.FALLO_AL_GUARDAR
            }
        }
    }

    private companion object {
        const val FRASE_DE_PRUEBA = "Voz activada"
    }
}
