package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.puerto.Reloj

enum class MotivoOmision { INTERVALO_NO_CUMPLIDO, LUGAR_YA_MOSTRADO, NADA_CERCA }

sealed interface Decision {
    data object Permitir : Decision
    data class Rechazar(val motivo: MotivoOmision) : Decision
}

/** Largo del texto que se le pide a la IA y su tope de tokens de salida. */
data class FormatoDato(val instruccionLargo: String, val maxTokensSalida: Int)

/**
 * Strategy: cada nivel de intensidad decide cuándo toca un dato automático y qué tan largo es.
 * [ultimoDatoMs] es el instante del último dato mostrado (null si no hay ninguno).
 */
interface PoliticaIntensidad {
    val intensidad: Intensidad
    val formato: FormatoDato
    fun evaluar(ultimoDatoMs: Long?, lugarEsNuevo: Boolean): Decision
}

/** Suave: como mucho un dato cada ~10 minutos, de una frase. Si el lugar se repite, se busca otro. */
class PoliticaSuave(private val reloj: Reloj) : PoliticaIntensidad {
    override val intensidad = Intensidad.SUAVE
    override val formato = FormatoDato("una sola frase corta", maxTokensSalida = 60)

    override fun evaluar(ultimoDatoMs: Long?, lugarEsNuevo: Boolean): Decision =
        reloj.decidirPorIntervalo(ultimoDatoMs, INTERVALO_MINIMO_MS)

    companion object {
        const val INTERVALO_MINIMO_MS = 10 * 60_000L
    }
}

/** Normal: solo al llegar a un lugar nuevo y con ~3 minutos entre datos; dos frases. */
class PoliticaNormal(private val reloj: Reloj) : PoliticaIntensidad {
    override val intensidad = Intensidad.NORMAL
    override val formato = FormatoDato("exactamente dos frases", maxTokensSalida = 110)

    override fun evaluar(ultimoDatoMs: Long?, lugarEsNuevo: Boolean): Decision =
        if (lugarEsNuevo) {
            reloj.decidirPorIntervalo(ultimoDatoMs, INTERVALO_MINIMO_MS)
        } else {
            Decision.Rechazar(MotivoOmision.LUGAR_YA_MOSTRADO)
        }

    companion object {
        const val INTERVALO_MINIMO_MS = 3 * 60_000L
    }
}

/** Intenso: cada lugar nuevo, sin espera; un párrafo corto. */
class PoliticaIntenso : PoliticaIntensidad {
    override val intensidad = Intensidad.INTENSO
    override val formato = FormatoDato("un párrafo corto de tres o cuatro frases", maxTokensSalida = 220)

    override fun evaluar(ultimoDatoMs: Long?, lugarEsNuevo: Boolean): Decision =
        if (lugarEsNuevo) Decision.Permitir else Decision.Rechazar(MotivoOmision.LUGAR_YA_MOSTRADO)
}

class FabricaPoliticas(private val reloj: Reloj) {
    fun para(intensidad: Intensidad): PoliticaIntensidad = when (intensidad) {
        Intensidad.SUAVE -> PoliticaSuave(reloj)
        Intensidad.NORMAL -> PoliticaNormal(reloj)
        Intensidad.INTENSO -> PoliticaIntenso()
    }
}

private fun Reloj.decidirPorIntervalo(ultimoDatoMs: Long?, intervaloMinimoMs: Long): Decision {
    val transcurrido = ultimoDatoMs?.let { ahoraMs() - it }
    return if (transcurrido == null || transcurrido >= intervaloMinimoMs) {
        Decision.Permitir
    } else {
        Decision.Rechazar(MotivoOmision.INTERVALO_NO_CUMPLIDO)
    }
}
