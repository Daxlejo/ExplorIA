package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.soporte.MINUTO_MS
import com.daxlejo.datoscerca.soporte.dato
import com.daxlejo.datoscerca.soporte.lugar
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectorTipoDatoTest {
    private val marcados = setOf(TipoDato.HISTORICO, TipoDato.CURIOSO, TipoDato.GASTRONOMICO)

    @Test
    fun `en orden empieza por el primer tipo marcado`() {
        assertEquals(TipoDato.HISTORICO, SelectorEnOrden().seleccionar(marcados, anterior = null))
    }

    @Test
    fun `en orden avanza al siguiente tipo marcado`() {
        assertEquals(TipoDato.CURIOSO, SelectorEnOrden().seleccionar(marcados, anterior = TipoDato.HISTORICO))
    }

    @Test
    fun `en orden salta los tipos sin marcar y vuelve al principio`() {
        assertEquals(TipoDato.HISTORICO, SelectorEnOrden().seleccionar(marcados, anterior = TipoDato.GASTRONOMICO))
        assertEquals(TipoDato.CURIOSO, SelectorEnOrden().seleccionar(marcados, anterior = TipoDato.COMICO))
    }

    @Test
    fun `con un solo tipo marcado siempre sale ese`() {
        val unico = setOf(TipoDato.COMICO)
        assertEquals(TipoDato.COMICO, SelectorEnOrden().seleccionar(unico, anterior = TipoDato.COMICO))
        assertEquals(TipoDato.COMICO, SelectorAleatorio(Random(1)).seleccionar(unico, anterior = null))
    }

    @Test
    fun `aleatorio solo elige entre los marcados y los usa todos`() {
        val selector = SelectorAleatorio(Random(42))
        val elegidos = (1..200).map { selector.seleccionar(marcados, anterior = null) }.toSet()
        assertEquals(marcados, elegidos)
    }

    @Test
    fun `aleatorio es reproducible con la misma semilla`() {
        val primera = (1..20).map { SelectorAleatorio(Random(5)).seleccionar(marcados, null) }
        val segunda = (1..20).map { SelectorAleatorio(Random(5)).seleccionar(marcados, null) }
        assertEquals(primera, segunda)
    }

    @Test
    fun `la fabrica elige la estrategia segun el interruptor aleatorio`() {
        val fabrica = FabricaSelectores(Random(1))
        assertTrue(fabrica.para(Configuracion.PorDefecto.copy(aleatorio = true)) is SelectorAleatorio)
        assertTrue(fabrica.para(Configuracion.PorDefecto.copy(aleatorio = false)) is SelectorEnOrden)
    }
}

class ConfiguracionTest {
    private val base = Configuracion.PorDefecto.copy(tipos = setOf(TipoDato.HISTORICO, TipoDato.COMICO))

    @Test
    fun `no se puede crear sin tipos`() {
        assertThrows(IllegalArgumentException::class.java) { base.copy(tipos = emptySet()) }
    }

    @Test
    fun `alternar marca un tipo nuevo y desmarca uno existente`() {
        assertTrue(TipoDato.CURIOSO in base.conTipoAlternado(TipoDato.CURIOSO).tipos)
        assertFalse(TipoDato.COMICO in base.conTipoAlternado(TipoDato.COMICO).tipos)
    }

    @Test
    fun `el ultimo tipo marcado no se puede desmarcar`() {
        val unico = base.copy(tipos = setOf(TipoDato.COMICO))
        assertFalse(unico.puedeDesmarcar(TipoDato.COMICO))
        assertEquals(unico, unico.conTipoAlternado(TipoDato.COMICO))
        assertTrue(unico.puedeDesmarcar(TipoDato.CURIOSO))
    }
}

class FiltroRepeticionTest {
    private val ahora = 200 * MINUTO_MS
    private val filtro = FiltroRepeticion(ventanaMs = 30 * MINUTO_MS, maxRecientes = 3)

    private fun entrada(id: Long, lugarId: String, haceMinutos: Int) =
        EntradaHistorial(id, dato(lugar(id = lugarId), generadoEnMs = ahora - haceMinutos * MINUTO_MS), favorito = false)

    @Test
    fun `sin historial no excluye nada`() {
        assertTrue(filtro.idsAExcluir(emptyList(), ahora).isEmpty())
    }

    @Test
    fun `excluye los lugares mostrados dentro de la ventana`() {
        val recientes = listOf(entrada(2, "catedral", haceMinutos = 5), entrada(1, "plaza", haceMinutos = 29))
        assertEquals(setOf("catedral", "plaza"), filtro.idsAExcluir(recientes, ahora))
    }

    @Test
    fun `un lugar vuelve a estar disponible al salir de la ventana`() {
        val recientes = listOf(entrada(2, "catedral", haceMinutos = 5), entrada(1, "plaza", haceMinutos = 30))
        assertEquals(setOf("catedral"), filtro.idsAExcluir(recientes, ahora))
    }

    @Test
    fun `solo revisa las entradas mas recientes`() {
        val recientes = (1..5).map { entrada(it.toLong(), "lugar-$it", haceMinutos = it) }
        assertEquals(setOf("lugar-1", "lugar-2", "lugar-3"), filtro.idsAExcluir(recientes, ahora))
    }

    @Test
    fun `no duplica un lugar repetido`() {
        val recientes = listOf(entrada(2, "plaza", haceMinutos = 1), entrada(1, "plaza", haceMinutos = 2))
        assertEquals(setOf("plaza"), filtro.idsAExcluir(recientes, ahora))
    }

    @Test
    fun `rechaza parametros invalidos`() {
        assertThrows(IllegalArgumentException::class.java) { FiltroRepeticion(ventanaMs = 0) }
        assertThrows(IllegalArgumentException::class.java) { FiltroRepeticion(maxRecientes = 0) }
    }
}
