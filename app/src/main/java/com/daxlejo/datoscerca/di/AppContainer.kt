package com.daxlejo.datoscerca.di

import android.content.Context
import android.util.Log
import com.daxlejo.datoscerca.data.configuracion.RepositorioConfiguracionDataStore
import com.daxlejo.datoscerca.data.configuracion.almacenConfiguracion
import com.daxlejo.datoscerca.data.fake.EscenarioFake
import com.daxlejo.datoscerca.data.fake.FakeFuenteLugares
import com.daxlejo.datoscerca.data.fake.FakeGeneradorDatos
import com.daxlejo.datoscerca.data.fake.FakeProveedorUbicacion
import com.daxlejo.datoscerca.data.historial.BaseDatosApp
import com.daxlejo.datoscerca.data.historial.RepositorioHistorialRoom
import com.daxlejo.datoscerca.data.plataforma.NarradorTts
import com.daxlejo.datoscerca.data.plataforma.RelojSistema
import com.daxlejo.datoscerca.data.plataforma.VibradorAndroid
import com.daxlejo.datoscerca.domain.casouso.AlternarFavorito
import com.daxlejo.datoscerca.domain.casouso.BorrarEntradaHistorial
import com.daxlejo.datoscerca.domain.casouso.GuardarConfiguracion
import com.daxlejo.datoscerca.domain.casouso.ObservarConfiguracion
import com.daxlejo.datoscerca.domain.casouso.ObservarEntrada
import com.daxlejo.datoscerca.domain.casouso.ObservarHistorial
import com.daxlejo.datoscerca.domain.casouso.ObtenerDatoAhora
import com.daxlejo.datoscerca.domain.casouso.ObtenerDatoParaUbicacion
import com.daxlejo.datoscerca.domain.motor.AvisadorDato
import com.daxlejo.datoscerca.domain.motor.FabricaPoliticas
import com.daxlejo.datoscerca.domain.motor.FabricaSelectores
import com.daxlejo.datoscerca.domain.motor.FiltroRepeticion
import com.daxlejo.datoscerca.domain.motor.MotorAutomatico
import com.daxlejo.datoscerca.domain.motor.ProducirDato
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import com.daxlejo.datoscerca.domain.puerto.GeneradorDatos
import com.daxlejo.datoscerca.domain.puerto.Narrador
import com.daxlejo.datoscerca.domain.puerto.ProveedorUbicacion
import com.daxlejo.datoscerca.domain.puerto.Reloj
import com.daxlejo.datoscerca.domain.puerto.RepositorioConfiguracion
import com.daxlejo.datoscerca.domain.puerto.RepositorioHistorial
import com.daxlejo.datoscerca.domain.puerto.Vibrador
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Inyección de dependencias manual: único lugar que conoce las implementaciones concretas.
 * En la Sesión 2 solo cambian las tres líneas marcadas como fake.
 */
class AppContainer(context: Context) {
    private val contextoApp = context.applicationContext
    private val escenarioFake = MutableStateFlow(EscenarioFake.NORMAL)

    // Puertos de plataforma
    val reloj: Reloj = RelojSistema()
    val narrador: Narrador by lazy { NarradorTts(contextoApp) }
    private val vibrador: Vibrador = VibradorAndroid(contextoApp)

    // Datos reales
    private val repositorioHistorial: RepositorioHistorial by lazy {
        RepositorioHistorialRoom(BaseDatosApp.crear(contextoApp).entradaDao())
    }
    private val repositorioConfiguracion: RepositorioConfiguracion =
        RepositorioConfiguracionDataStore(contextoApp.almacenConfiguracion)

    // Datos fake (la Sesión 2 los reemplaza por ubicación, lugares e IA reales)
    private val proveedorUbicacion: ProveedorUbicacion = FakeProveedorUbicacion(escenarioFake)
    private val fuenteLugares: FuenteLugares = FakeFuenteLugares(escenarioFake)
    private val generadorDatos: GeneradorDatos = FakeGeneradorDatos(escenarioFake)

    // Motor de datos
    private val fabricaPoliticas = FabricaPoliticas(reloj)
    private val filtroRepeticion = FiltroRepeticion()
    private val producirDato by lazy {
        ProducirDato(
            generador = generadorDatos,
            historial = repositorioHistorial,
            fabricaSelectores = FabricaSelectores(),
            fabricaPoliticas = fabricaPoliticas,
            avisador = AvisadorDato(vibrador, narrador),
            reloj = reloj,
        )
    }

    // Casos de uso
    val observarConfiguracion = ObservarConfiguracion(repositorioConfiguracion)
    val guardarConfiguracion = GuardarConfiguracion(repositorioConfiguracion)
    val observarHistorial by lazy { ObservarHistorial(repositorioHistorial) }
    val observarEntrada by lazy { ObservarEntrada(repositorioHistorial) }
    val alternarFavorito by lazy { AlternarFavorito(repositorioHistorial) }
    val borrarEntradaHistorial by lazy { BorrarEntradaHistorial(repositorioHistorial) }
    val obtenerDatoAhora by lazy {
        ObtenerDatoAhora(
            ubicacion = proveedorUbicacion,
            fuente = fuenteLugares,
            historial = repositorioHistorial,
            configuracion = repositorioConfiguracion,
            filtro = filtroRepeticion,
            producir = producirDato,
            reloj = reloj,
        )
    }
    private val obtenerDatoParaUbicacion by lazy {
        ObtenerDatoParaUbicacion(
            fuente = fuenteLugares,
            historial = repositorioHistorial,
            configuracion = repositorioConfiguracion,
            fabricaPoliticas = fabricaPoliticas,
            filtro = filtroRepeticion,
            producir = producirDato,
            reloj = reloj,
        )
    }

    val motorAutomatico by lazy {
        MotorAutomatico(proveedorUbicacion, repositorioConfiguracion, obtenerDatoParaUbicacion, narrador, reloj)
    }

    /** Solo para pruebas manuales de la Sesión 1: fuerza un estado de error en los fakes. */
    fun aplicarEscenarioDePrueba(nombre: String?) {
        val pedido = EscenarioFake.entries.firstOrNull { it.name.equals(nombre, ignoreCase = true) }
        if (nombre != null && pedido == null) Log.w(ETIQUETA, "Escenario desconocido: $nombre. Se usa NORMAL.")
        escenarioFake.value = pedido ?: EscenarioFake.NORMAL
    }

    private companion object {
        const val ETIQUETA = "AppContainer"
    }
}
