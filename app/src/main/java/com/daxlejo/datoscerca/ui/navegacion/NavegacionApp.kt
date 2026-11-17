package com.daxlejo.datoscerca.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.daxlejo.datoscerca.di.FabricaViewModels
import com.daxlejo.datoscerca.ui.configuracion.ConfiguracionRuta
import com.daxlejo.datoscerca.ui.dato.DatoRuta
import com.daxlejo.datoscerca.ui.historial.HistorialRuta
import com.daxlejo.datoscerca.ui.inicio.InicioRuta
import com.daxlejo.datoscerca.ui.theme.DatosCercaTheme

private object Rutas {
    const val INICIO = "inicio"
    const val CONFIGURACION = "configuracion"
    const val HISTORIAL = "historial"
    const val ARGUMENTO_ID = "id"
    const val DATO = "dato/{$ARGUMENTO_ID}"
    fun dato(id: Long) = "dato/$id"
}

/** Raíz de la interfaz: tema, AppScaffold (hora compartida) y navegación con deslizar para volver. */
@Composable
fun AppDatosCerca(fabrica: FabricaViewModels) {
    DatosCercaTheme {
        AppScaffold {
            val navegador = rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(navController = navegador, startDestination = Rutas.INICIO) {
                composable(Rutas.INICIO) {
                    InicioRuta(
                        viewModel = viewModel(factory = fabrica.inicio()),
                        alAbrirDato = navegador::abrirDato,
                        alAbrirHistorial = { navegador.navigate(Rutas.HISTORIAL) },
                        alAbrirConfiguracion = { navegador.navigate(Rutas.CONFIGURACION) },
                    )
                }
                composable(
                    route = Rutas.DATO,
                    arguments = listOf(navArgument(Rutas.ARGUMENTO_ID) { type = NavType.LongType }),
                ) { entrada ->
                    val id = entrada.arguments?.getLong(Rutas.ARGUMENTO_ID) ?: SIN_ID
                    DatoRuta(
                        viewModel = viewModel(factory = fabrica.dato(id)),
                        alCerrar = { navegador.popBackStack() },
                    )
                }
                composable(Rutas.CONFIGURACION) {
                    ConfiguracionRuta(viewModel(factory = fabrica.configuracion()))
                }
                composable(Rutas.HISTORIAL) {
                    HistorialRuta(viewModel(factory = fabrica.historial()), alAbrirDato = navegador::abrirDato)
                }
            }
        }
    }
}

// Un id inexistente lleva al estado "Este dato ya no está".
private const val SIN_ID = -1L

private fun NavHostController.abrirDato(id: Long) {
    navigate(Rutas.dato(id)) { launchSingleTop = true }
}
