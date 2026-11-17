package com.daxlejo.datoscerca.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxlejo.datoscerca.domain.casouso.ObservarHistorial
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.puerto.Reloj
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface HistorialUiState {
    data object Cargando : HistorialUiState

    /** No se ha guardado ningún dato todavía. */
    data object Vacio : HistorialUiState

    /** [grupos] puede quedar vacío si se filtran favoritos y no hay ninguno. */
    data class Lista(val soloFavoritos: Boolean, val grupos: List<GrupoDia>) : HistorialUiState
}

class HistorialViewModel(
    observarHistorial: ObservarHistorial,
    private val agrupador: AgrupadorHistorial,
    private val reloj: Reloj,
) : ViewModel() {

    private val soloFavoritos = MutableStateFlow(false)

    val uiState: StateFlow<HistorialUiState> =
        combine(observarHistorial(), soloFavoritos, ::reducir)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState.Cargando)

    fun alCambiarSoloFavoritos(activo: Boolean) = soloFavoritos.update { activo }

    private fun reducir(entradas: List<EntradaHistorial>, filtrar: Boolean): HistorialUiState {
        if (entradas.isEmpty()) return HistorialUiState.Vacio
        val visibles = if (filtrar) entradas.filter(EntradaHistorial::favorito) else entradas
        return HistorialUiState.Lista(filtrar, agrupador.agrupar(visibles, reloj.ahoraMs()))
    }
}
