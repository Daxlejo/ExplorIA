package com.daxlejo.datoscerca.data.ubicacion

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.EstadoUbicacion
import com.daxlejo.datoscerca.domain.puerto.ProveedorUbicacion
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ubicación real en primer plano con [FusedLocationProviderClient] y respaldo directo del
 * [LocationManager] para que tanto el GPS real como `adb emu geo fix` y las rutas GPX del emulador
 * se reflejen al instante.
 */
class ProveedorUbicacionFused(
    private val contexto: Context,
    private val clienteFused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(contexto),
) : ProveedorUbicacion {

    private val gestorUbicacion =
        contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val senalRefresco = MutableStateFlow(0L)

    @Volatile
    private var ultimaCoordenada: Coordenada? = null

    /** Fuerza a reevaluar permiso y estado del GPS (por ejemplo al volver del diálogo de permisos). */
    fun refrescarEstado() {
        senalRefresco.value++
    }

    fun tienePermiso(): Boolean = tienePermisoUbicacion(contexto)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun ubicaciones(): Flow<EstadoUbicacion> =
        senalRefresco
            .flatMapLatest { flujoUbicacion() }
            .distinctUntilChanged()

    override suspend fun ubicacionActual(): Resultado<Coordenada> {
        if (!tienePermiso()) return Resultado.Fallo(ErrorDato.SinPermiso)
        if (!gpsActivo()) return Resultado.Fallo(ErrorDato.SinGps)
        val coordenada = obtenerMejorCoordenadaActual()
        return coordenada?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ErrorDato.SinGps)
    }

    @SuppressLint("MissingPermission")
    private fun flujoUbicacion(): Flow<EstadoUbicacion> = callbackFlow {
        if (!tienePermiso()) {
            trySend(EstadoUbicacion.NoDisponible(ErrorDato.SinPermiso))
            while (!tienePermiso()) {
                delay(SONDEO_PERMISO_MS)
            }
        }
        val receptorGps = registrarReceptorGps()
        if (!gpsActivo()) {
            trySend(EstadoUbicacion.NoDisponible(ErrorDato.SinGps))
        } else {
            emitirInicial()
        }
        val callbackFused = crearCallbackFused()
        val listenerGps = crearListenerGps()
        iniciarSuscripciones(callbackFused, listenerGps)
        awaitClose { detenerSuscripciones(callbackFused, listenerGps, receptorGps) }
    }

    @SuppressLint("MissingPermission")
    private fun ProducerScope<EstadoUbicacion>.emitirInicial() {
        ultimaConocidaGps()?.aCoordenada()?.let { publicarDisponible(it) }
        clienteFused.lastLocation.addOnSuccessListener { ubicacion ->
            ubicacion?.aCoordenada()?.let { publicarDisponible(it) }
        }
    }

    private fun ProducerScope<EstadoUbicacion>.crearCallbackFused(): LocationCallback =
        object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val ubicacion = resultado.lastLocation ?: return
                ubicacion.aCoordenada()?.let { publicarDisponible(it) }
            }

            override fun onLocationAvailability(disponibilidad: LocationAvailability) {
                if (!gpsActivo()) {
                    trySend(EstadoUbicacion.NoDisponible(ErrorDato.SinGps))
                }
            }
        }

    private fun ProducerScope<EstadoUbicacion>.crearListenerGps(): LocationListener =
        LocationListener { ubicacion ->
            ubicacion.aCoordenada()?.let { publicarDisponible(it) }
        }

    private fun ProducerScope<EstadoUbicacion>.publicarDisponible(coordenada: Coordenada) {
        if (!tienePermiso()) {
            trySend(EstadoUbicacion.NoDisponible(ErrorDato.SinPermiso))
            return
        }
        if (!gpsActivo()) {
            trySend(EstadoUbicacion.NoDisponible(ErrorDato.SinGps))
            return
        }
        ultimaCoordenada = coordenada
        trySend(EstadoUbicacion.Disponible(coordenada))
    }

    @SuppressLint("MissingPermission")
    private fun iniciarSuscripciones(callbackFused: LocationCallback, listenerGps: LocationListener) {
        val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_MS)
            .setMinUpdateIntervalMillis(INTERVALO_MINIMO_MS)
            .setWaitForAccurateLocation(false)
            .build()
        try {
            clienteFused.requestLocationUpdates(solicitud, callbackFused, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.w(ETIQUETA, "Sin permiso al suscribir FusedLocation", e)
        }
        try {
            if (gestorUbicacion.allProviders.contains(LocationManager.GPS_PROVIDER)) {
                gestorUbicacion.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    INTERVALO_MINIMO_MS,
                    0f,
                    listenerGps,
                    Looper.getMainLooper(),
                )
            }
        } catch (e: SecurityException) {
            Log.w(ETIQUETA, "Sin permiso al suscribir GPS_PROVIDER", e)
        } catch (e: IllegalArgumentException) {
            Log.w(ETIQUETA, "GPS_PROVIDER no disponible", e)
        }
    }

    private fun detenerSuscripciones(
        callbackFused: LocationCallback,
        listenerGps: LocationListener,
        receptorGps: BroadcastReceiver,
    ) {
        clienteFused.removeLocationUpdates(callbackFused)
        try {
            gestorUbicacion.removeUpdates(listenerGps)
        } catch (e: IllegalArgumentException) {
            Log.w(ETIQUETA, "Listener GPS ya estaba desregistrado", e)
        }
        try {
            contexto.unregisterReceiver(receptorGps)
        } catch (e: IllegalArgumentException) {
            Log.w(ETIQUETA, "Receptor GPS ya estaba desregistrado", e)
        }
    }

    private fun ProducerScope<EstadoUbicacion>.registrarReceptorGps(): BroadcastReceiver {
        val receptor = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!gpsActivo()) {
                    trySend(EstadoUbicacion.NoDisponible(ErrorDato.SinGps))
                } else {
                    ultimaCoordenada?.let { trySend(EstadoUbicacion.Disponible(it)) } ?: emitirInicial()
                }
            }
        }
        val filtro = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(contexto, receptor, filtro, ContextCompat.RECEIVER_NOT_EXPORTED)
        return receptor
    }

    @SuppressLint("MissingPermission")
    private suspend fun obtenerMejorCoordenadaActual(): Coordenada? {
        ultimaConocidaGps()?.aCoordenada()?.let {
            ultimaCoordenada = it
        }
        ultimaCoordenada?.let { return it }
        val desdeLast = clienteFused.lastLocation.esperar()
        if (desdeLast != null) {
            return desdeLast.aCoordenada().also { ultimaCoordenada = it }
        }
        val token = CancellationTokenSource()
        val actual = withTimeoutOrNull(TIMEOUT_UBICACION_ACTUAL_MS) {
            clienteFused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).esperar()
        }
        if (actual == null) token.cancel()
        return actual?.aCoordenada()?.also { ultimaCoordenada = it }
    }

    @SuppressLint("MissingPermission")
    private fun ultimaConocidaGps(): Location? = try {
        val gps = gestorUbicacion.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val red = gestorUbicacion.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        listOfNotNull(gps, red).maxByOrNull(Location::getTime)
    } catch (e: SecurityException) {
        Log.w(ETIQUETA, "Sin permiso al consultar última ubicación", e)
        null
    }

    private fun gpsActivo(): Boolean = LocationManagerCompat.isLocationEnabled(gestorUbicacion)

    private fun Location.aCoordenada(): Coordenada? =
        if (latitude in -90.0..90.0 && longitude in -180.0..180.0) {
            Coordenada(latitude, longitude)
        } else {
            null
        }

    private suspend fun <T> Task<T>.esperar(): T? = suspendCancellableCoroutine { continuacion ->
        addOnSuccessListener { resultado ->
            if (continuacion.isActive) continuacion.resume(resultado)
        }
        addOnFailureListener { error ->
            Log.w(ETIQUETA, "Tarea de ubicación de Play Services falló", error)
            if (continuacion.isActive) continuacion.resume(null)
        }
        addOnCanceledListener {
            if (continuacion.isActive) continuacion.resume(null)
        }
    }

    companion object {
        private const val ETIQUETA = "ProveedorUbicacion"
        private const val INTERVALO_MS = 3_000L
        private const val INTERVALO_MINIMO_MS = 1_500L
        private const val TIMEOUT_UBICACION_ACTUAL_MS = 4_000L
        private const val SONDEO_PERMISO_MS = 1_000L

        val PERMISOS_UBICACION = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        fun tienePermisoUbicacion(contexto: Context): Boolean =
            PERMISOS_UBICACION.any { permiso ->
                ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED
            }
    }
}
