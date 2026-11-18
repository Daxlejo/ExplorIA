package com.daxlejo.datoscerca.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxlejo.datoscerca.domain.casouso.GuardarConfiguracion
import com.daxlejo.datoscerca.domain.casouso.ObtenerDatoAhora
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.motor.EstadoMotor
import com.daxlejo.datoscerca.domain.motor.MotorAutomatico
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface InicioUiState {
    data object Iniciando : InicioUiState
    data object BuscandoDato : InicioUiState
    data object EnPausa : InicioUiState
    data class Problema(val error: ErrorDato) : InicioUiState

    /** [aviso] es el último fallo de un dato automático; se muestra sin interrumpir. */
    data class Explorando(
        val coordenada: Coordenada,
        val cercaDe: String?,
        val aviso: ErrorDato?,
    ) : InicioUiState
}

sealed interface EfectoInicio {
    data class AbrirDato(val id: Long) : EfectoInicio
}

class InicioViewModel(
    motor: MotorAutomatico,
    private val obtenerDatoAhora: ObtenerDatoAhora,
    private val guardarConfiguracion: GuardarConfiguracion,
) : ViewModel() {

    private sealed interface PeticionManual {
        data object Reposo : PeticionManual
        data object Buscando : PeticionManual
        data class Fallida(val error: ErrorDato) : PeticionManual
    }

    private val peticion = MutableStateFlow<PeticionManual>(PeticionManual.Reposo)
    private val efectosManuales = Channel<EfectoInicio>(Channel.BUFFERED)

    val uiState: StateFlow<InicioUiState> =
        combine(motor.estado, peticion, ::reducir)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState.Iniciando)

    init {
        // Un fallo manual deja de mostrarse en cuanto el motor cambia de estado (volvió el GPS, se reanudó...).
        viewModelScope.launch {
            motor.estado.drop(1).collect {
                peticion.update { actual -> if (actual is PeticionManual.Fallida) PeticionManual.Reposo else actual }
            }
        }
    }

    /** Datos listos para abrir: los pedidos a mano y los que llegan solos al caminar. */
    val efectos: Flow<EfectoInicio> = merge(
        efectosManuales.receiveAsFlow(),
        motor.nuevosDatos.map { EfectoInicio.AbrirDato(it.id) },
    )

    fun alPedirDato() {
        if (peticion.value == PeticionManual.Buscando) return
        peticion.value = PeticionManual.Buscando
        viewModelScope.launch {
            when (val resultado = obtenerDatoAhora()) {
                is Resultado.Exito -> {
                    peticion.value = PeticionManual.Reposo
                    efectosManuales.send(EfectoInicio.AbrirDato(resultado.valor.id))
                }
                is Resultado.Fallo -> peticion.value = alFallar(resultado.error)
            }
        }
    }

    fun alReanudar() {
        viewModelScope.launch {
            val resultado = guardarConfiguracion { it.copy(pausa = false) }
            peticion.value = when (resultado) {
                is Resultado.Exito -> PeticionManual.Reposo
                is Resultado.Fallo -> PeticionManual.Fallida(resultado.error)
            }
        }
    }

    private fun reducir(motor: EstadoMotor, manual: PeticionManual): InicioUiState = when (manual) {
        PeticionManual.Buscando -> InicioUiState.BuscandoDato
        is PeticionManual.Fallida -> InicioUiState.Problema(manual.error)
        PeticionManual.Reposo -> desdeMotor(motor)
    }

    // La pausa ya la refleja el motor; guardarla aquí la dejaría pegada tras reanudar desde Configuración.
    // Si ya venía un dato automático en camino, ese dato se abrirá solo: no es un error.
    private fun alFallar(error: ErrorDato): PeticionManual = when (error) {
        ErrorDato.EnPausa, ErrorDato.GeneracionEnCurso -> PeticionManual.Reposo
        else -> PeticionManual.Fallida(error)
    }

    private fun desdeMotor(motor: EstadoMotor): InicioUiState = when (motor) {
        EstadoMotor.Iniciando -> InicioUiState.Iniciando
        EstadoMotor.EnPausa -> InicioUiState.EnPausa
        is EstadoMotor.Bloqueado -> InicioUiState.Problema(motor.error)
        is EstadoMotor.Explorando -> InicioUiState.Explorando(motor.coordenada, motor.cercaDe, motor.aviso)
    }
}
