package com.daxlejo.datoscerca.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.daxlejo.datoscerca.ui.configuracion.ConfiguracionViewModel
import com.daxlejo.datoscerca.ui.dato.DatoViewModel
import com.daxlejo.datoscerca.ui.historial.AgrupadorHistorial
import com.daxlejo.datoscerca.ui.historial.HistorialViewModel
import com.daxlejo.datoscerca.ui.inicio.InicioViewModel

/** Factory: construye cada ViewModel con las dependencias del contenedor. */
class FabricaViewModels(private val contenedor: AppContainer) {

    fun inicio(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            InicioViewModel(contenedor.motorAutomatico, contenedor.obtenerDatoAhora, contenedor.guardarConfiguracion)
        }
    }

    fun dato(id: Long): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            DatoViewModel(
                idInicial = id,
                observarEntrada = contenedor.observarEntrada,
                alternarFavorito = contenedor.alternarFavorito,
                borrarEntrada = contenedor.borrarEntradaHistorial,
                obtenerDatoAhora = contenedor.obtenerDatoAhora,
                narrador = contenedor.narrador,
            )
        }
    }

    fun configuracion(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ConfiguracionViewModel(
                observarConfiguracion = contenedor.observarConfiguracion,
                guardarConfiguracion = contenedor.guardarConfiguracion,
                narrador = contenedor.narrador,
                alPrecargarDemo = contenedor::precargarModoDemo,
            )
        }
    }

    fun historial(): ViewModelProvider.Factory = viewModelFactory {
        initializer { HistorialViewModel(contenedor.observarHistorial, AgrupadorHistorial(), contenedor.reloj) }
    }
}
