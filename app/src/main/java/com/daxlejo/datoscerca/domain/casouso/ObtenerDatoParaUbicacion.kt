package com.daxlejo.datoscerca.domain.casouso

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.OrigenDato
import com.daxlejo.datoscerca.domain.motor.Decision
import com.daxlejo.datoscerca.domain.motor.FabricaPoliticas
import com.daxlejo.datoscerca.domain.motor.FiltroRelevancia
import com.daxlejo.datoscerca.domain.motor.FiltroRepeticion
import com.daxlejo.datoscerca.domain.motor.GeneracionUnica
import com.daxlejo.datoscerca.domain.motor.MotivoOmision
import com.daxlejo.datoscerca.domain.motor.PoliticaIntensidad
import com.daxlejo.datoscerca.domain.motor.ProducirDato
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
 * Dato automático al pasar cerca de un lugar relevante. Antes de gastar una llamada a la IA
 * aplica, en este orden: pausa, generación única, límite por hora, relevancia y anti-repetición.
 */
class ObtenerDatoParaUbicacion(
    private val fuente: FuenteLugares,
    private val historial: RepositorioHistorial,
    private val configuracion: RepositorioConfiguracion,
    private val fabricaPoliticas: FabricaPoliticas,
    private val filtroRepeticion: FiltroRepeticion,
    private val filtroRelevancia: FiltroRelevancia,
    private val producir: ProducirDato,
    private val generacion: GeneracionUnica,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(coordenada: Coordenada): Exploracion {
        val actual = configuracion.observar().first()
        if (actual.pausa) return Exploracion.Fallo(ErrorDato.EnPausa)
        return generacion.siEstaLibre { explorar(coordenada, actual) }
            ?: Exploracion.Omitido(MotivoOmision.GENERACION_EN_CURSO, lugarCercano = null)
    }

    private suspend fun explorar(coordenada: Coordenada, actual: Configuracion): Exploracion =
        revisarLimitePorHora(actual) ?: when (val recientes = historial.recientes(filtroRepeticion.maxRecientes)) {
            is Resultado.Fallo -> Exploracion.Fallo(recientes.error)
            is Resultado.Exito -> buscarLugar(coordenada, actual, recientes.valor)
        }

    // Devuelve null si todavía cabe un dato automático en la última hora (o si es modo demo).
    private suspend fun revisarLimitePorHora(actual: Configuracion): Exploracion? {
        val politica = fabricaPoliticas.para(actual.intensidad, actual.modoDemo)
        val desdeMs = reloj.ahoraMs() - PoliticaIntensidad.VENTANA_MS
        return when (val cuenta = historial.contarAutomaticosDesde(desdeMs)) {
            is Resultado.Fallo -> Exploracion.Fallo(cuenta.error)
            is Resultado.Exito -> (politica.evaluar(cuenta.valor) as? Decision.Rechazar)
                ?.let { Exploracion.Omitido(it.motivo, lugarCercano = null) }
        }
    }

    private suspend fun buscarLugar(
        coordenada: Coordenada,
        actual: Configuracion,
        recientes: List<EntradaHistorial>,
    ): Exploracion = when (val cercanos = fuente.buscarCercanos(coordenada, filtroRelevancia.radioLlegadaMetros)) {
        is Resultado.Fallo -> Exploracion.Fallo(cercanos.error)
        is Resultado.Exito -> elegir(filtroRelevancia.alLlegar(cercanos.valor), actual, recientes)
    }

    private suspend fun elegir(
        candidatos: List<Lugar>,
        actual: Configuracion,
        recientes: List<EntradaHistorial>,
    ): Exploracion {
        val masCercano = candidatos.firstOrNull()
            ?: return Exploracion.Omitido(MotivoOmision.NADA_RELEVANTE_CERCA, lugarCercano = null)
        val excluir = filtroRepeticion.idsAExcluir(recientes, reloj.ahoraMs())
        val nuevo = candidatos.firstOrNull { it.id !in excluir }
            ?: return Exploracion.Omitido(MotivoOmision.LUGAR_YA_MOSTRADO, masCercano)
        return when (val resultado = producir(nuevo, actual, recientes, OrigenDato.AUTOMATICO)) {
            is Resultado.Exito -> Exploracion.NuevoDato(resultado.valor)
            is Resultado.Fallo -> Exploracion.Fallo(resultado.error)
        }
    }
}
