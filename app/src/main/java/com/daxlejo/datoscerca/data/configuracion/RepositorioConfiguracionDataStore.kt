package com.daxlejo.datoscerca.data.configuracion

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.puerto.RepositorioConfiguracion
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Si el archivo se corrompe se empieza de cero (valores por defecto) en lugar de fallar en cada lectura.
val Context.almacenConfiguracion: DataStore<Preferences> by preferencesDataStore(
    name = "configuracion",
    corruptionHandler = ReplaceFileCorruptionHandler { error ->
        Log.e("RepositorioConfig", "Configuración corrupta; se restablece", error)
        emptyPreferences()
    },
)

class RepositorioConfiguracionDataStore(
    private val almacen: DataStore<Preferences>,
) : RepositorioConfiguracion {

    override fun observar(): Flow<Configuracion> =
        almacen.data
            .catch { error ->
                if (error !is IOException) throw error
                Log.e(ETIQUETA, "No se pudo leer la configuración; se usan los valores por defecto", error)
                emit(emptyPreferences())
            }
            .map(::leer)

    override suspend fun actualizar(cambio: (Configuracion) -> Configuracion): Resultado<Unit> =
        try {
            almacen.edit { preferencias -> escribir(preferencias, cambio(leer(preferencias))) }
            Resultado.Exito(Unit)
        } catch (e: IOException) {
            Log.e(ETIQUETA, "No se pudo guardar la configuración", e)
            Resultado.Fallo(ErrorDato.Almacenamiento(e.message))
        }

    // Un conjunto vacío o con nombres desconocidos se sanea para respetar el invariante del dominio.
    private fun leer(preferencias: Preferences): Configuracion {
        val porDefecto = Configuracion.PorDefecto
        val tipos = preferencias[Claves.TIPOS].orEmpty()
            .mapNotNull { nombre -> TipoDato.entries.firstOrNull { it.name == nombre } }
            .toSet()
        val intensidad = Intensidad.entries.firstOrNull { it.name == preferencias[Claves.INTENSIDAD] }
        return Configuracion(
            tipos = tipos.ifEmpty { porDefecto.tipos },
            aleatorio = preferencias[Claves.ALEATORIO] ?: porDefecto.aleatorio,
            intensidad = intensidad ?: porDefecto.intensidad,
            vozActiva = preferencias[Claves.VOZ] ?: porDefecto.vozActiva,
            vibracionActiva = preferencias[Claves.VIBRACION] ?: porDefecto.vibracionActiva,
            pausa = preferencias[Claves.PAUSA] ?: porDefecto.pausa,
            modoDemo = preferencias[Claves.MODO_DEMO] ?: porDefecto.modoDemo,
        )
    }

    private fun escribir(preferencias: MutablePreferences, configuracion: Configuracion) {
        preferencias[Claves.TIPOS] = configuracion.tipos.map(TipoDato::name).toSet()
        preferencias[Claves.ALEATORIO] = configuracion.aleatorio
        preferencias[Claves.INTENSIDAD] = configuracion.intensidad.name
        preferencias[Claves.VOZ] = configuracion.vozActiva
        preferencias[Claves.VIBRACION] = configuracion.vibracionActiva
        preferencias[Claves.PAUSA] = configuracion.pausa
        preferencias[Claves.MODO_DEMO] = configuracion.modoDemo
    }

    private object Claves {
        val TIPOS = stringSetPreferencesKey("tipos")
        val ALEATORIO = booleanPreferencesKey("aleatorio")
        val INTENSIDAD = stringPreferencesKey("intensidad")
        val VOZ = booleanPreferencesKey("voz")
        val VIBRACION = booleanPreferencesKey("vibracion")
        val PAUSA = booleanPreferencesKey("pausa")
        val MODO_DEMO = booleanPreferencesKey("modo_demo")
    }

    private companion object {
        const val ETIQUETA = "RepositorioConfig"
    }
}
