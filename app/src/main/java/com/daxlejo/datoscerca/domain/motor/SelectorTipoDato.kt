package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import kotlin.random.Random

/** Strategy: elige el tipo del próximo dato entre los marcados. [tipos] nunca llega vacío. */
fun interface SelectorTipoDato {
    fun seleccionar(tipos: Set<TipoDato>, anterior: TipoDato?): TipoDato
}

/** Selección fija: recorre los tipos marcados siempre en el mismo orden. Con un solo tipo, siempre ese. */
class SelectorEnOrden : SelectorTipoDato {
    override fun seleccionar(tipos: Set<TipoDato>, anterior: TipoDato?): TipoDato {
        val ordenados = tipos.sorted()
        val siguiente = anterior?.let { previo -> ordenados.firstOrNull { it > previo } }
        return siguiente ?: ordenados.first()
    }
}

class SelectorAleatorio(private val random: Random) : SelectorTipoDato {
    override fun seleccionar(tipos: Set<TipoDato>, anterior: TipoDato?): TipoDato =
        tipos.sorted().random(random)
}

class FabricaSelectores(private val random: Random = Random.Default) {
    fun para(configuracion: Configuracion): SelectorTipoDato =
        if (configuracion.aleatorio) SelectorAleatorio(random) else SelectorEnOrden()
}
