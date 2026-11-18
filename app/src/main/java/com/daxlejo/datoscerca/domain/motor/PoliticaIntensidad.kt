package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.Intensidad

enum class MotivoOmision {
    /** Ya salieron los datos automáticos que permite la intensidad en la última hora. */
    LIMITE_POR_HORA,

    /** Los lugares relevantes del radio ya se mostraron hace poco. */
    LUGAR_YA_MOSTRADO,

    /** No hay ningún lugar relevante dentro del radio de llegada. */
    NADA_RELEVANTE_CERCA,

    /** Ya se está generando otro dato; no se encadenan dos seguidos. */
    GENERACION_EN_CURSO,
}

sealed interface Decision {
    data object Permitir : Decision
    data class Rechazar(val motivo: MotivoOmision) : Decision
}

/** Largo del texto que se le pide a la IA y su tope de tokens de salida. */
data class FormatoDato(val instruccionLargo: String, val maxTokensSalida: Int)

/**
 * Strategy: cada nivel de intensidad decide cuántos datos automáticos caben en una hora y qué tan largos son.
 * La ventana es móvil: cuenta los datos automáticos del historial de los últimos [VENTANA_MS].
 */
interface PoliticaIntensidad {
    val intensidad: Intensidad
    val formato: FormatoDato
    fun evaluar(automaticosUltimaHora: Int): Decision

    companion object {
        const val VENTANA_MS = 60 * 60_000L
    }
}

/** Base común: permite mientras no se llegue a [maxPorHora]. */
abstract class PoliticaConLimitePorHora(private val maxPorHora: Int) : PoliticaIntensidad {
    init {
        require(maxPorHora > 0) { "El límite por hora debe ser positivo" }
    }

    override fun evaluar(automaticosUltimaHora: Int): Decision =
        if (automaticosUltimaHora < maxPorHora) Decision.Permitir else Decision.Rechazar(MotivoOmision.LIMITE_POR_HORA)
}

/** Suave: como mucho un dato por hora, de una frase. */
class PoliticaSuave : PoliticaConLimitePorHora(MAX_POR_HORA) {
    override val intensidad = Intensidad.SUAVE
    override val formato = FormatoDato("una sola frase corta", maxTokensSalida = 60)

    companion object {
        const val MAX_POR_HORA = 1
    }
}

/** Normal: como mucho dos datos por hora, de dos frases. */
class PoliticaNormal : PoliticaConLimitePorHora(MAX_POR_HORA) {
    override val intensidad = Intensidad.NORMAL
    override val formato = FormatoDato("exactamente dos frases", maxTokensSalida = 110)

    companion object {
        const val MAX_POR_HORA = 2
    }
}

/** Intenso: como mucho cuatro datos por hora, de un párrafo corto. */
class PoliticaIntenso : PoliticaConLimitePorHora(MAX_POR_HORA) {
    override val intensidad = Intensidad.INTENSO
    override val formato = FormatoDato("un párrafo corto de tres o cuatro frases", maxTokensSalida = 220)

    companion object {
        const val MAX_POR_HORA = 4
    }
}

/** Decorator del modo demo: mantiene el largo del nivel, pero sin límite por hora. */
class PoliticaDemo(private val base: PoliticaIntensidad) : PoliticaIntensidad by base {
    override fun evaluar(automaticosUltimaHora: Int): Decision = Decision.Permitir
}

class FabricaPoliticas {
    fun para(intensidad: Intensidad, modoDemo: Boolean = false): PoliticaIntensidad {
        val base = when (intensidad) {
            Intensidad.SUAVE -> PoliticaSuave()
            Intensidad.NORMAL -> PoliticaNormal()
            Intensidad.INTENSO -> PoliticaIntenso()
        }
        return if (modoDemo) PoliticaDemo(base) else base
    }
}
