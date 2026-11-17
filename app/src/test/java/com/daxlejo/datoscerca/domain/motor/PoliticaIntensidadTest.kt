package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.soporte.MINUTO_MS
import com.daxlejo.datoscerca.soporte.RelojFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaIntensidadTest {
    private val reloj = RelojFalso(ahora = 60 * MINUTO_MS)
    private fun hace(minutos: Int) = reloj.ahora - minutos * MINUTO_MS

    private val rechazoPorIntervalo = Decision.Rechazar(MotivoOmision.INTERVALO_NO_CUMPLIDO)
    private val rechazoPorRepetido = Decision.Rechazar(MotivoOmision.LUGAR_YA_MOSTRADO)

    @Test
    fun `suave permite el primer dato`() {
        assertEquals(Decision.Permitir, PoliticaSuave(reloj).evaluar(ultimoDatoMs = null, lugarEsNuevo = true))
    }

    @Test
    fun `suave rechaza antes de diez minutos`() {
        assertEquals(rechazoPorIntervalo, PoliticaSuave(reloj).evaluar(hace(9), lugarEsNuevo = true))
    }

    @Test
    fun `suave permite justo a los diez minutos`() {
        assertEquals(Decision.Permitir, PoliticaSuave(reloj).evaluar(hace(10), lugarEsNuevo = true))
    }

    @Test
    fun `suave no exige lugar nuevo porque busca otro`() {
        assertEquals(Decision.Permitir, PoliticaSuave(reloj).evaluar(hace(15), lugarEsNuevo = false))
    }

    @Test
    fun `normal rechaza un lugar ya mostrado aunque haya pasado el tiempo`() {
        assertEquals(rechazoPorRepetido, PoliticaNormal(reloj).evaluar(hace(30), lugarEsNuevo = false))
    }

    @Test
    fun `normal rechaza un lugar nuevo antes de tres minutos`() {
        assertEquals(rechazoPorIntervalo, PoliticaNormal(reloj).evaluar(hace(2), lugarEsNuevo = true))
    }

    @Test
    fun `normal permite un lugar nuevo a los tres minutos`() {
        assertEquals(Decision.Permitir, PoliticaNormal(reloj).evaluar(hace(3), lugarEsNuevo = true))
    }

    @Test
    fun `intenso permite cada lugar nuevo sin esperar`() {
        assertEquals(Decision.Permitir, PoliticaIntenso().evaluar(hace(0), lugarEsNuevo = true))
    }

    @Test
    fun `intenso rechaza un lugar repetido`() {
        assertEquals(rechazoPorRepetido, PoliticaIntenso().evaluar(hace(60), lugarEsNuevo = false))
    }

    @Test
    fun `el largo pedido crece con la intensidad`() {
        val fabrica = FabricaPoliticas(reloj)
        val tokens = Intensidad.entries.map { fabrica.para(it).formato.maxTokensSalida }
        assertEquals(tokens.sorted(), tokens)
        assertEquals(tokens.distinct(), tokens)
        assertTrue(fabrica.para(Intensidad.SUAVE).formato.instruccionLargo.contains("una sola frase"))
        assertTrue(fabrica.para(Intensidad.NORMAL).formato.instruccionLargo.contains("dos frases"))
        assertTrue(fabrica.para(Intensidad.INTENSO).formato.instruccionLargo.contains("párrafo corto"))
    }

    @Test
    fun `la fabrica entrega la politica de cada nivel`() {
        val fabrica = FabricaPoliticas(reloj)
        Intensidad.entries.forEach { assertEquals(it, fabrica.para(it).intensidad) }
    }
}
