package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.Lugar

/**
 * Solo los lugares emblemáticos dan datos. Para el dato automático, además, la persona debe
 * estar dentro de [radioLlegadaMetros] (configurable entre 150 y 250 m).
 */
class FiltroRelevancia(val radioLlegadaMetros: Int = RADIO_LLEGADA_POR_DEFECTO) {
    init {
        require(radioLlegadaMetros in RANGO_LLEGADA) { "El radio de llegada debe estar entre 150 y 250 m" }
    }

    fun esEmblematico(lugar: Lugar): Boolean = lugar.relevancia.emblematico

    fun calificaParaLlegada(lugar: Lugar): Boolean =
        esEmblematico(lugar) && lugar.distanciaMetros <= radioLlegadaMetros

    /** Lugares que pueden dar un dato automático, del más cercano al más lejano. */
    fun alLlegar(cercanos: List<Lugar>): List<Lugar> =
        cercanos.filter(::calificaParaLlegada).sortedBy(Lugar::distanciaMetros)

    /** Lugares que pueden dar un dato manual: emblemáticos, sin importar el radio de llegada. */
    fun emblematicos(cercanos: List<Lugar>): List<Lugar> =
        cercanos.filter(::esEmblematico).sortedBy(Lugar::distanciaMetros)

    companion object {
        const val RADIO_LLEGADA_POR_DEFECTO = 200
        val RANGO_LLEGADA = 150..250
    }
}
