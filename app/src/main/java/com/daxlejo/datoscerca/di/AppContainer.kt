package com.daxlejo.datoscerca.di

import android.content.Context
import com.daxlejo.datoscerca.BuildConfig
import com.daxlejo.datoscerca.data.configuracion.RepositorioConfiguracionDataStore
import com.daxlejo.datoscerca.data.configuracion.almacenConfiguracion
import com.daxlejo.datoscerca.data.demo.FuenteLugaresDemo
import com.daxlejo.datoscerca.data.demo.RutaDemoPasto
import com.daxlejo.datoscerca.data.historial.BaseDatosApp
import com.daxlejo.datoscerca.data.historial.RepositorioHistorialRoom
import com.daxlejo.datoscerca.data.ia.CacheDatos
import com.daxlejo.datoscerca.data.ia.GeneradorDatosConCache
import com.daxlejo.datoscerca.data.ia.GeneradorDatosOpenAi
import com.daxlejo.datoscerca.data.lugares.CadenaFuentesLugares
import com.daxlejo.datoscerca.data.lugares.FuenteBusquedaWebOpenAi
import com.daxlejo.datoscerca.data.lugares.FuenteMunicipio
import com.daxlejo.datoscerca.data.lugares.FuenteNominatim
import com.daxlejo.datoscerca.data.lugares.FuenteWikipedia
import com.daxlejo.datoscerca.data.lugares.LimitadorFrecuencia
import com.daxlejo.datoscerca.data.plataforma.NarradorTts
import com.daxlejo.datoscerca.data.plataforma.RelojSistema
import com.daxlejo.datoscerca.data.plataforma.VibradorAndroid
import com.daxlejo.datoscerca.data.red.ClienteHttp
import com.daxlejo.datoscerca.data.red.ClienteHttpOkHttp
import com.daxlejo.datoscerca.data.ubicacion.ProveedorUbicacionFused
import com.daxlejo.datoscerca.domain.casouso.AlternarFavorito
import com.daxlejo.datoscerca.domain.casouso.BorrarEntradaHistorial
import com.daxlejo.datoscerca.domain.casouso.GuardarConfiguracion
import com.daxlejo.datoscerca.domain.casouso.ObservarConfiguracion
import com.daxlejo.datoscerca.domain.casouso.ObservarEntrada
import com.daxlejo.datoscerca.domain.casouso.ObservarHistorial
import com.daxlejo.datoscerca.domain.casouso.ObtenerDatoAhora
import com.daxlejo.datoscerca.domain.casouso.ObtenerDatoParaUbicacion
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.motor.AvisadorDato
import com.daxlejo.datoscerca.domain.motor.FabricaPoliticas
import com.daxlejo.datoscerca.domain.motor.FabricaSelectores
import com.daxlejo.datoscerca.domain.motor.FiltroRelevancia
import com.daxlejo.datoscerca.domain.motor.FiltroRepeticion
import com.daxlejo.datoscerca.domain.motor.GeneracionUnica
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
import java.io.File
import kotlinx.coroutines.flow.first

/**
 * Inyección de dependencias manual: único lugar que conoce las implementaciones concretas.
 * Conecta ubicación real (Fused Location), la cadena de fuentes de lugares y OpenAI con caché.
 */
class AppContainer(context: Context) {
    private val contextoApp = context.applicationContext

    // Puertos de plataforma
    val reloj: Reloj = RelojSistema()
    val narrador: Narrador by lazy { NarradorTts(contextoApp) }
    private val vibrador: Vibrador = VibradorAndroid(contextoApp)

    // Persistencia local
    private val repositorioHistorial: RepositorioHistorial by lazy {
        RepositorioHistorialRoom(BaseDatosApp.crear(contextoApp).entradaDao())
    }
    private val repositorioConfiguracion: RepositorioConfiguracion =
        RepositorioConfiguracionDataStore(contextoApp.almacenConfiguracion)

    // Motor de datos
    private val fabricaPoliticas = FabricaPoliticas()
    private val filtroRepeticion = FiltroRepeticion()
    private val filtroRelevancia =
        FiltroRelevancia(radioLlegadaMetros = FiltroRelevancia.RADIO_LLEGADA_POR_DEFECTO)

    // Red, ubicación, lugares e IA reales
    private val clienteHttp: ClienteHttp by lazy { ClienteHttpOkHttp() }
    val proveedorUbicacionFused: ProveedorUbicacionFused by lazy {
        ProveedorUbicacionFused(contextoApp)
    }
    private val proveedorUbicacion: ProveedorUbicacion get() = proveedorUbicacionFused

    private val fuenteLugares: FuenteLugares by lazy {
        val wikipediaEs = FuenteWikipedia(
            idioma = "es",
            clienteHttp = clienteHttp,
            enfoquePreferido = ::obtenerEnfoquePreferido,
        )
        val wikipediaEn = FuenteWikipedia(
            idioma = "en",
            clienteHttp = clienteHttp,
            enfoquePreferido = ::obtenerEnfoquePreferido,
        )
        val limitadorNominatim = LimitadorFrecuencia(reloj = reloj)
        val nominatim = FuenteNominatim(
            clienteHttp = clienteHttp,
            limitador = limitadorNominatim,
            fuenteWikipediaEs = wikipediaEs,
        )
        val busquedaWebOpenAi = FuenteBusquedaWebOpenAi(
            clienteHttp = clienteHttp,
            apiKey = BuildConfig.OPENAI_API_KEY,
            modelo = BuildConfig.OPENAI_MODEL,
            habilitada = BuildConfig.OPENAI_WEB_SEARCH,
        )
        val municipio = FuenteMunicipio(
            fuenteNominatim = nominatim,
            fuenteWikipediaEs = wikipediaEs,
        )
        CadenaFuentesLugares(
            fuentes = listOf(wikipediaEs, wikipediaEn, nominatim, busquedaWebOpenAi),
            respaldoMunicipio = municipio,
            idsRecientes = ::obtenerIdsRecientes,
            fuenteDemo = FuenteLugaresDemo(),
            modoDemoActivo = ::esModoDemoActivo,
        )
    }

    private val cacheDatos: CacheDatos by lazy {
        CacheDatos(archivoPersistencia = File(contextoApp.filesDir, ARCHIVO_CACHE_DATOS))
    }

    private val generadorDatos: GeneradorDatos by lazy {
        val openAi = GeneradorDatosOpenAi(
            clienteHttp = clienteHttp,
            apiKey = BuildConfig.OPENAI_API_KEY,
            modelo = BuildConfig.OPENAI_MODEL,
        )
        GeneradorDatosConCache(delegado = openAi, cache = cacheDatos)
    }

    // Compartida por el dato manual y el automático: nunca se preparan dos a la vez.
    private val generacionUnica = GeneracionUnica()
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
            filtroRepeticion = filtroRepeticion,
            filtroRelevancia = filtroRelevancia,
            producir = producirDato,
            generacion = generacionUnica,
            reloj = reloj,
        )
    }
    private val obtenerDatoParaUbicacion by lazy {
        ObtenerDatoParaUbicacion(
            fuente = fuenteLugares,
            historial = repositorioHistorial,
            configuracion = repositorioConfiguracion,
            fabricaPoliticas = fabricaPoliticas,
            filtroRepeticion = filtroRepeticion,
            filtroRelevancia = filtroRelevancia,
            producir = producirDato,
            generacion = generacionUnica,
            reloj = reloj,
        )
    }

    val motorAutomatico by lazy {
        MotorAutomatico(
            ubicacion = proveedorUbicacion,
            configuracion = repositorioConfiguracion,
            obtenerDato = obtenerDatoParaUbicacion,
            narrador = narrador,
            reloj = reloj,
        )
    }

    /** Precarga en caché todos los datos de la ruta demo de Pasto para funcionar sin internet. */
    fun precargarModoDemo() {
        RutaDemoPasto.precargarEn(cacheDatos)
    }

    private suspend fun obtenerIdsRecientes(): Set<String> =
        when (val recientes = repositorioHistorial.recientes(filtroRepeticion.maxRecientes)) {
            is Resultado.Exito -> filtroRepeticion.idsAExcluir(recientes.valor, reloj.ahoraMs())
            is Resultado.Fallo -> emptySet()
        }

    private suspend fun obtenerEnfoquePreferido(): TipoDato? {
        val config = repositorioConfiguracion.observar().first()
        val ultimoTipo = when (val recientes = repositorioHistorial.recientes(1)) {
            is Resultado.Exito -> recientes.valor.firstOrNull()?.dato?.tipo
            is Resultado.Fallo -> null
        }
        return FabricaSelectores().para(config).seleccionar(config.tipos, ultimoTipo)
    }

    private suspend fun esModoDemoActivo(): Boolean {
        val activa = repositorioConfiguracion.observar().first().modoDemo
        if (activa) RutaDemoPasto.precargarEn(cacheDatos)
        return activa
    }

    private companion object {
        const val ARCHIVO_CACHE_DATOS = "cache_datos.json"
    }
}
