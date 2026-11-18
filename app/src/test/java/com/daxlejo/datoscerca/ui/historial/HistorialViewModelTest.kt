package com.daxlejo.datoscerca.ui.historial

import com.daxlejo.datoscerca.domain.casouso.ObservarHistorial
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.soporte.RelojFalso
import com.daxlejo.datoscerca.soporte.RepositorioHistorialEnMemoria
import com.daxlejo.datoscerca.soporte.dato
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelTest {
    private val repositorio = RepositorioHistorialEnMemoria()

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun viewModel() =
        HistorialViewModel(ObservarHistorial(repositorio), AgrupadorHistorial(ZoneId.of("America/Bogota")), RelojFalso())

    @Test
    fun `un error al leer la base se muestra como estado de error`() = runTest {
        repositorio.falloLectura = ErrorDato.Almacenamiento("base dañada")
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(HistorialUiState.Error(ErrorDato.Almacenamiento("base dañada")), viewModel.uiState.value)
    }

    @Test
    fun `sin errores muestra la lista`() = runTest {
        repositorio.guardar(dato())
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertTrue(viewModel.uiState.value is HistorialUiState.Lista)
    }
}
