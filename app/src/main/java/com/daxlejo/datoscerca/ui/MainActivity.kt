package com.daxlejo.datoscerca.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.daxlejo.datoscerca.BuildConfig
import com.daxlejo.datoscerca.DatosCercaApp
import com.daxlejo.datoscerca.di.AppContainer
import com.daxlejo.datoscerca.di.FabricaViewModels
import com.daxlejo.datoscerca.ui.navegacion.AppDatosCerca
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contenedor = (application as DatosCercaApp).contenedor
        aplicarEscenarioDePrueba(contenedor, intent)
        explorarEnPrimerPlano(contenedor)
        val fabrica = FabricaViewModels(contenedor)
        setContent { AppDatosCerca(fabrica) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        aplicarEscenarioDePrueba((application as DatosCercaApp).contenedor, intent)
    }

    // El dato automático solo trabaja con la app visible (uso en primer plano).
    private fun explorarEnPrimerPlano(contenedor: AppContainer) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { contenedor.motorAutomatico.ejecutar() }
        }
    }

    // Solo en debug: `adb shell am start ... --es escenario SIN_INTERNET` fuerza un estado de los fakes.
    private fun aplicarEscenarioDePrueba(contenedor: AppContainer, intent: Intent?) {
        val escenario = intent?.getStringExtra(EXTRA_ESCENARIO) ?: return
        if (BuildConfig.DEBUG) contenedor.aplicarEscenarioDePrueba(escenario)
    }

    private companion object {
        const val EXTRA_ESCENARIO = "escenario"
    }
}
