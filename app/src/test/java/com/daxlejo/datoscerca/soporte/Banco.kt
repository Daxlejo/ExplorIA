package com.daxlejo.datoscerca.soporte

import com.daxlejo.datoscerca.domain.casouso.ObtenerDatoAhora
import com.daxlejo.datoscerca.domain.casouso.ObtenerDatoParaUbicacion
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.motor.AvisadorDato
import com.daxlejo.datoscerca.domain.motor.FabricaPoliticas
import com.daxlejo.datoscerca.domain.motor.FabricaSelectores
import com.daxlejo.datoscerca.domain.motor.FiltroRepeticion
import com.daxlejo.datoscerca.domain.motor.ProducirDato
import kotlin.random.Random

/** Banco de pruebas: arma el motor de datos completo con dobles en memoria. */
class Banco(
    lugares: List<Lugar> = listOf(lugar()),
    configuracionInicial: Configuracion = Configuracion.PorDefecto,
) {
    val reloj = RelojFalso(ahora = 100 * MINUTO_MS)
    val historial = RepositorioHistorialEnMemoria()
    val configuracion = RepositorioConfiguracionEnMemoria(configuracionInicial)
    val fuente = FuenteLugaresFalsa(lugares)
    val generador = GeneradorFalso()
    val ubicacion = UbicacionFalsa()
    val vibrador = VibradorEspia()
    val narrador = NarradorEspia()

    private val fabricaPoliticas = FabricaPoliticas(reloj)
    private val filtro = FiltroRepeticion()
    private val producir = ProducirDato(
        generador = generador,
        historial = historial,
        fabricaSelectores = FabricaSelectores(Random(7)),
        fabricaPoliticas = fabricaPoliticas,
        avisador = AvisadorDato(vibrador, narrador),
        reloj = reloj,
    )

    val obtenerDatoParaUbicacion =
        ObtenerDatoParaUbicacion(fuente, historial, configuracion, fabricaPoliticas, filtro, producir, reloj)

    val obtenerDatoAhora =
        ObtenerDatoAhora(ubicacion, fuente, historial, configuracion, filtro, producir, reloj)
}
