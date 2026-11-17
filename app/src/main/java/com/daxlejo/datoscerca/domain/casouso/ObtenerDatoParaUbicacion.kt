package com.daxlejo.datoscerca.domain.casouso

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.motor.Decision
import com.daxlejo.datoscerca.domain.motor.FabricaPoliticas
import com.daxlejo.datoscerca.domain.motor.FiltroRepeticion
import com.daxlejo.datoscerca.domain.motor.MotivoOmision
import com.daxlejo.datoscerca.domain.motor.ProducirDato
import com.daxlejo.datoscerca.domain.motor.RadioBusqueda
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import com.daxlejo.datoscerca.domain.puerto.Reloj
import com.daxlejo.datoscerca.domain.puerto.RepositorioConfiguracion
import com.daxlejo.datoscerca.domain.puerto.RepositorioHistorial
import kotlinx.coroutines.flow.first

/** Resultado de evaluar una ubicación: omitir un dato es lo normal, no un error. */
sealed interface Exploracion {
    data class NuevoDato(val entrada: EntradaHistorial) : Exploracion
    data class Omitido(val motivo: MotivoOmision, val lugarCercano: Lugar?) : Exploracion
    data class Fallo(val error: ErrorDato) : Exploracion
}

/**
 * Dato automático al llegar a una ubicación. Aplica pausa, anti-repetición y política de
 * intensidad antes de gastar una llamada a la IA.
 */
class ObtenerDatoParaUbicacion(
    private val fuente: FuenteLugares,
    private val historial: RepositorioHistorial,
    private val configuracion: RepositorioConfiguracion,
    private val fabricaPoliticas: FabricaPoliticas,
    private val filtro: FiltroRepeticion,
    private val producir: ProducirDato,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(coordenada: Coordenada): Exploracion {
        val actual = configuracion.observar().first()
        if (actual.pausa) return Exploracion.Fallo(ErrorDato.EnPausa)
        return when (val recientes = historial.recientes(filtro.maxRecientes)) {
            is Resultado.Fallo -> Exploracion.Fallo(recientes.error)
            is Resultado.Exito -> explorar(coordenada, actual, recientes.valor)
        }
    }

    private suspend fun explorar(
        coordenada: Coordenada,
        actual: Configuracion,
        recientes: List<EntradaHistorial>,
    ): Exploracion = when (val cercano = fuente.buscarCercano(coordenada, RadioBusqueda.LLEGADA_METROS)) {
        is Resultado.Fallo -> alNoEncontrar(cercano.error, MotivoOmision.NADA_CERCA, lugarCercano = null)
        is Resultado.Exito -> evaluar(coordenada, cercano.valor, actual, recientes)
    }

    private suspend fun evaluar(
        coordenada: Coordenada,
        cercano: Lugar,
        actual: Configuracion,
        recientes: List<EntradaHistorial>,
    ): Exploracion {
        val excluir = filtro.idsAExcluir(recientes, reloj.ahoraMs())
        val esNuevo = cercano.id !in excluir
        val ultimoDatoMs = recientes.firstOrNull()?.dato?.generadoEnMs
        val decision = fabricaPoliticas.para(actual.intensidad).evaluar(ultimoDatoMs, esNuevo)
        if (decision is Decision.Rechazar) return Exploracion.Omitido(decision.motivo, cercano)
        if (esNuevo) return producirCon(cercano, actual, recientes)
        return conOtroLugar(coordenada, cercano, excluir, actual, recientes)
    }

    // La política permitió el dato pero el lugar más cercano ya se mostró: se prueba con el siguiente.
    private suspend fun conOtroLugar(
        coordenada: Coordenada,
        cercano: Lugar,
        excluir: Set<String>,
        actual: Configuracion,
        recientes: List<EntradaHistorial>,
    ): Exploracion = when (val otro = fuente.buscarCercano(coordenada, RadioBusqueda.LLEGADA_METROS, excluir)) {
        is Resultado.Fallo -> alNoEncontrar(otro.error, MotivoOmision.LUGAR_YA_MOSTRADO, cercano)
        is Resultado.Exito -> producirCon(otro.valor, actual, recientes)
    }

    private fun alNoEncontrar(error: ErrorDato, motivo: MotivoOmision, lugarCercano: Lugar?): Exploracion =
        if (error == ErrorDato.SinLugares) Exploracion.Omitido(motivo, lugarCercano) else Exploracion.Fallo(error)

    private suspend fun producirCon(
        lugar: Lugar,
        actual: Configuracion,
        recientes: List<EntradaHistorial>,
    ): Exploracion = when (val resultado = producir(lugar, actual, recientes)) {
        is Resultado.Exito -> Exploracion.NuevoDato(resultado.valor)
        is Resultado.Fallo -> Exploracion.Fallo(resultado.error)
    }
}
