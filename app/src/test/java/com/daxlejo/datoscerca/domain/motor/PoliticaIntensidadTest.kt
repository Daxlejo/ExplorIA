package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.Intensidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaIntensidadTest {
    private val fabrica = FabricaPoliticas()
    private val limite = Decision.Rechazar(MotivoOmision.LIMITE_POR_HORA)

    @Test
    fun `suave permite un dato por hora`() {
        val politica = fabrica.para(Intensidad.SUAVE)
        assertEquals(Decision.Permitir, politica.evaluar(automaticosUltimaHora = 0))
        assertEquals(limite, politica.evaluar(automaticosUltimaHora = 1))
    }

    @Test
    fun `normal permite dos datos por hora`() {
        val politica = fabrica.para(Intensidad.NORMAL)
        assertEquals(Decision.Permitir, politica.evaluar(automaticosUltimaHora = 1))
        assertEquals(limite, politica.evaluar(automaticosUltimaHora = 2))
    }

    @Test
    fun `intenso permite cuatro datos por hora`() {
        val politica = fabrica.para(Intensidad.INTENSO)
        assertEquals(Decision.Permitir, politica.evaluar(automaticosUltimaHora = 3))
        assertEquals(limite, politica.evaluar(automaticosUltimaHora = 4))
    }

    @Test
    fun `el modo demo ignora el limite y conserva el largo del nivel`() {
        Intensidad.entries.forEach { intensidad ->
            val demo = fabrica.para(intensidad, modoDemo = true)
            assertEquals(Decision.Permitir, demo.evaluar(automaticosUltimaHora = 50))
            assertEquals(fabrica.para(intensidad).formato, demo.formato)
            assertEquals(intensidad, demo.intensidad)
        }
    }

    @Test
    fun `la ventana es de una hora`() {
        assertEquals(60 * 60_000L, PoliticaIntensidad.VENTANA_MS)
    }

    @Test
    fun `el largo pedido crece con la intensidad`() {
        val tokens = Intensidad.entries.map { fabrica.para(it).formato.maxTokensSalida }
        assertEquals(tokens.sorted(), tokens)
        assertEquals(tokens.distinct(), tokens)
        assertTrue(fabrica.para(Intensidad.SUAVE).formato.instruccionLargo.contains("una sola frase"))
        assertTrue(fabrica.para(Intensidad.NORMAL).formato.instruccionLargo.contains("dos frases"))
        assertTrue(fabrica.para(Intensidad.INTENSO).formato.instruccionLargo.contains("párrafo corto"))
    }

    @Test
    fun `la fabrica entrega la politica de cada nivel`() {
        Intensidad.entries.forEach { assertEquals(it, fabrica.para(it).intensidad) }
    }
}
