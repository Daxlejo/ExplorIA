package com.daxlejo.datoscerca.ui.dato

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxlejo.datoscerca.domain.casouso.AlternarFavorito
import com.daxlejo.datoscerca.domain.casouso.BorrarEntradaHistorial
import com.daxlejo.datoscerca.domain.casouso.ObservarEntrada
import com.daxlejo.datoscerca.domain.casouso.ObtenerDatoAhora
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.puerto.Narrador
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que la pantalla está haciendo con el dato; solo puede pasar una cosa a la vez. */
enum class AccionDato { NINGUNA, BUSCANDO_OTRO, CONFIRMANDO_BORRADO, BORRANDO }

sealed interface DatoUiState {
    data object Cargando : DatoUiState
    data object NoDisponible : DatoUiState

    /** No se pudo traer otro dato; el anterior sigue guardado y se puede volver a él. */
    data class Problema(val error: ErrorDato) : DatoUiState

    data class Visible(
        val id: Long,
        val lugar: String,
        val texto: String,
        val tipo: TipoDato,
        val distanciaMetros: Int,
        val favorito: Boolean,
        val accion: AccionDato,
        val aviso: ErrorDato?,
    ) : DatoUiState
}

sealed interface EfectoDato {
    data object Cerrar : EfectoDato
}

class DatoViewModel(
    idInicial: Long,
    observarEntrada: ObservarEntrada,
    private val alternarFavorito: AlternarFavorito,
    private val borrarEntrada: BorrarEntradaHistorial,
    private val obtenerDatoAhora: ObtenerDatoAhora,
    private val narrador: Narrador,
) : ViewModel() {

    // Estado que vive solo en esta pantalla (no en el historial).
    private data class Interaccion(
        val accion: AccionDato = AccionDato.NINGUNA,
        val problema: ErrorDato? = null,
        val aviso: ErrorDato? = null,
    )

    private val idActual = MutableStateFlow(idInicial)
    private val interaccion = MutableStateFlow(Interaccion())
    private val canalEfectos = Channel<EfectoDato>(Channel.BUFFERED)
    val efectos: Flow<EfectoDato> = canalEfectos.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DatoUiState> =
        combine(idActual.flatMapLatest { observarEntrada(it) }, interaccion, ::reducir)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DatoUiState.Cargando)

    fun alEscuchar() {
        val visible = uiState.value as? DatoUiState.Visible ?: return
        val resultado = narrador.hablar(visible.texto)
        interaccion.update { it.copy(aviso = (resultado as? Resultado.Fallo)?.error) }
    }

    fun alAlternarFavorito() {
        viewModelScope.launch {
            val resultado = alternarFavorito(idActual.value)
            interaccion.update { it.copy(aviso = (resultado as? Resultado.Fallo)?.error) }
        }
    }

    fun alPedirOtro() {
        if (!empezar(AccionDato.BUSCANDO_OTRO)) return
        narrador.detener()
        viewModelScope.launch {
            when (val resultado = obtenerDatoAhora()) {
                is Resultado.Exito -> {
                    idActual.value = resultado.valor.id
                    interaccion.update { it.copy(accion = AccionDato.NINGUNA) }
                }
                is Resultado.Fallo -> alFallarOtro(resultado.error)
            }
        }
    }

    // La pausa no es un error de pantalla completa: basta con avisar bajo el dato actual.
    private fun alFallarOtro(error: ErrorDato) = interaccion.update {
        if (error == ErrorDato.EnPausa) {
            it.copy(accion = AccionDato.NINGUNA, aviso = error)
        } else {
            it.copy(accion = AccionDato.NINGUNA, problema = error)
        }
    }

    fun alDescartarProblema() = interaccion.update { it.copy(problema = null) }

    fun alPedirBorrado() {
        empezar(AccionDato.CONFIRMANDO_BORRADO)
    }

    fun alCancelarBorrado() = interaccion.update {
        if (it.accion == AccionDato.CONFIRMANDO_BORRADO) it.copy(accion = AccionDato.NINGUNA) else it
    }

    fun alConfirmarBorrado() {
        if (interaccion.value.accion != AccionDato.CONFIRMANDO_BORRADO) return
        narrador.detener()
        interaccion.update { it.copy(accion = AccionDato.BORRANDO) }
        viewModelScope.launch {
            when (val resultado = borrarEntrada(idActual.value)) {
                is Resultado.Exito -> canalEfectos.send(EfectoDato.Cerrar)
                is Resultado.Fallo ->
                    interaccion.update { it.copy(accion = AccionDato.NINGUNA, aviso = resultado.error) }
            }
        }
    }

    override fun onCleared() {
        narrador.detener()
    }

    // Una acción solo empieza si no hay otra en curso; así no se borra un dato distinto del que se ve.
    private fun empezar(accion: AccionDato): Boolean {
        val libre = interaccion.value.accion == AccionDato.NINGUNA
        if (libre) interaccion.update { it.copy(accion = accion, problema = null, aviso = null) }
        return libre
    }

    private fun reducir(entrada: EntradaHistorial?, actual: Interaccion): DatoUiState = when {
        actual.accion == AccionDato.BORRANDO -> DatoUiState.Cargando
        actual.problema != null -> DatoUiState.Problema(actual.problema)
        entrada == null -> DatoUiState.NoDisponible
        else -> DatoUiState.Visible(
            id = entrada.id,
            lugar = entrada.dato.lugar.nombre,
            texto = entrada.dato.texto,
            tipo = entrada.dato.tipo,
            distanciaMetros = entrada.dato.lugar.distanciaMetros,
            favorito = entrada.favorito,
            accion = actual.accion,
            aviso = actual.aviso,
        )
    }
}
