package com.daxlejo.datoscerca.data.lugares

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares

/**
 * Patrón Chain of Responsibility para [FuenteLugares].
 *
 * Recorre los eslabones principales en orden (Wikipedia `es` -> Wikipedia `en` -> Nominatim ->
 * OpenAI Web Search) y se detiene en cuanto encuentra al menos un lugar emblemático que no haya
 * sido mostrado recientemente. Si ningún eslabón halló un hito emblemático, consulta el último
 * eslabón [respaldoMunicipio] (que solo responde para la acción manual "Dato ahora").
 */
class CadenaFuentesLugares(
    private val fuentes: List<FuenteLugares>,
    private val respaldoMunicipio: FuenteLugares? = null,
    private val idsRecientes: suspend () -> Set<String> = { emptySet() },
    private val fuenteDemo: FuenteLugares? = null,
    private val modoDemoActivo: suspend () -> Boolean = { false },
) : FuenteLugares {

    init {
        require(fuentes.isNotEmpty()) { "La cadena necesita al menos una fuente de lugares" }
    }

    override suspend fun buscarCercanos(coordenada: Coordenada, radioMetros: Int): Resultado<List<Lugar>> {
        if (modoDemoActivo() && fuenteDemo != null) {
            val locales = (fuenteDemo.buscarCercanos(coordenada, radioMetros) as? Resultado.Exito)?.valor.orEmpty()
            if (locales.any { it.relevancia.emblematico }) {
                return Resultado.Exito(locales.sortedBy(Lugar::distanciaMetros))
            }
        }
        val excluidos = idsRecientes()
        val acumulados = mutableListOf<Lugar>()
        var huboRespuestaExitosa = false
        var primerFallo: ErrorDato? = null

        for (fuente in fuentes) {
            when (val resultado = fuente.buscarCercanos(coordenada, radioMetros)) {
                is Resultado.Exito -> {
                    huboRespuestaExitosa = true
                    acumulados += resultado.valor
                    val tieneEmblematicoNuevo = resultado.valor.any {
                        it.relevancia.emblematico && it.id !in excluidos
                    }
                    if (tieneEmblematicoNuevo) {
                        return Resultado.Exito(ordenarSinDuplicados(acumulados))
                    }
                }
                is Resultado.Fallo -> {
                    if (resultado.error == ErrorDato.SinInternet && !huboRespuestaExitosa && acumulados.isEmpty()) {
                        return Resultado.Fallo(ErrorDato.SinInternet)
                    }
                    if (primerFallo == null) primerFallo = resultado.error
                }
            }
        }

        if (acumulados.none { it.relevancia.emblematico } && respaldoMunicipio != null) {
            when (val respaldo = respaldoMunicipio.buscarCercanos(coordenada, radioMetros)) {
                is Resultado.Exito -> {
                    huboRespuestaExitosa = true
                    acumulados += respaldo.valor
                }
                is Resultado.Fallo -> if (primerFallo == null) primerFallo = respaldo.error
            }
        }

        return if (huboRespuestaExitosa || acumulados.isNotEmpty()) {
            Resultado.Exito(ordenarSinDuplicados(acumulados))
        } else {
            Resultado.Fallo(primerFallo ?: ErrorDato.SinLugares)
        }
    }

    private fun ordenarSinDuplicados(lugares: List<Lugar>): List<Lugar> =
        lugares.distinctBy(Lugar::id).sortedBy(Lugar::distanciaMetros)
}
