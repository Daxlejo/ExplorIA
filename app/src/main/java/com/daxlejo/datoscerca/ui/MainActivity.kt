package com.daxlejo.datoscerca.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.daxlejo.datoscerca.DatosCercaApp
import com.daxlejo.datoscerca.data.ubicacion.ProveedorUbicacionFused
import com.daxlejo.datoscerca.di.AppContainer
import com.daxlejo.datoscerca.di.FabricaViewModels
import com.daxlejo.datoscerca.ui.navegacion.AppDatosCerca
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var yaSePidioPermiso = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contenedor = contenedor()
        solicitarPermisoInicial(contenedor)
        explorarEnPrimerPlano(contenedor)
        val fabrica = FabricaViewModels(contenedor)
        setContent {
            AppDatosCerca(
                fabrica = fabrica,
                alSolicitarPermiso = ::solicitarPermisoDesdeBoton,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        contenedor().proveedorUbicacionFused.refrescarEstado()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        contenedor().proveedorUbicacionFused.refrescarEstado()
    }

    @Deprecated("Requerido por ComponentActivity para recibir el resultado del permiso del sistema")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODIGO_PERMISO_UBICACION) {
            yaSePidioPermiso = true
            contenedor().proveedorUbicacionFused.refrescarEstado()
        }
    }

    // El dato automático solo trabaja con la app visible (uso en primer plano).
    private fun explorarEnPrimerPlano(contenedor: AppContainer) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { contenedor.motorAutomatico.ejecutar() }
        }
    }

    private fun solicitarPermisoInicial(contenedor: AppContainer) {
        if (!contenedor.proveedorUbicacionFused.tienePermiso()) {
            ActivityCompat.requestPermissions(
                this,
                ProveedorUbicacionFused.PERMISOS_UBICACION,
                CODIGO_PERMISO_UBICACION,
            )
        }
    }

    private fun solicitarPermisoDesdeBoton() {
        val proveedor = contenedor().proveedorUbicacionFused
        if (proveedor.tienePermiso()) {
            proveedor.refrescarEstado()
            return
        }
        val mostrarRazon = ProveedorUbicacionFused.PERMISOS_UBICACION.any { permiso ->
            ActivityCompat.shouldShowRequestPermissionRationale(this, permiso)
        }
        if (!yaSePidioPermiso || mostrarRazon) {
            ActivityCompat.requestPermissions(
                this,
                ProveedorUbicacionFused.PERMISOS_UBICACION,
                CODIGO_PERMISO_UBICACION,
            )
        } else {
            abrirAjustesDePermisos()
        }
    }

    private fun abrirAjustesDePermisos() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(ETIQUETA, "No se pudo abrir la pantalla de permisos del sistema", e)
        }
    }

    private fun contenedor(): AppContainer = (application as DatosCercaApp).contenedor

    private companion object {
        const val ETIQUETA = "MainActivity"
        const val CODIGO_PERMISO_UBICACION = 1001
    }
}
