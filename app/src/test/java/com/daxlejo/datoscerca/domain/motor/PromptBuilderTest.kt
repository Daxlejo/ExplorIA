package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.soporte.lugar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {
    private val formato = FormatoDato("exactamente dos frases", maxTokensSalida = 110)
    private val catedral = lugar(nombre = "Catedral de Pasto", contexto = "Se construyó entre 1899 y 1920.")

    private fun construir(tipo: TipoDato = TipoDato.HISTORICO) =
        PromptBuilder().lugar(catedral).tipo(tipo).formato(formato).build()

    @Test
    fun `incluye lugar, contexto y largo pedido`() {
        val prompt = construir()
        assertTrue(prompt.usuario.contains("Lugar: Catedral de Pasto"))
        assertTrue(prompt.usuario.contains("Se construyó entre 1899 y 1920."))
        assertTrue(prompt.usuario.contains("Largo: exactamente dos frases"))
        assertEquals(110, prompt.maxTokensSalida)
    }

    @Test
    fun `ordena usar solo el contexto y responder en español`() {
        val sistema = construir().sistema
        assertTrue(sistema.contains("únicamente los hechos del CONTEXTO"))
        assertTrue(sistema.contains("no inventes"))
        assertTrue(sistema.contains("español"))
    }

    @Test
    fun `cada tipo de dato cambia el enfoque`() {
        val enfoques = TipoDato.entries.map { tipo ->
            construir(tipo).usuario.lineSequence().first { it.startsWith("Enfoque:") }
        }
        assertEquals(TipoDato.entries.size, enfoques.distinct().size)
        assertTrue(construir(TipoDato.COMICO).usuario.contains("humor"))
        assertTrue(construir(TipoDato.GASTRONOMICO).usuario.contains("comida"))
    }

    @Test
    fun `recorta el contexto largo sin partir palabras`() {
        val largo = lugar(contexto = "palabra ".repeat(400))
        val prompt = PromptBuilder().lugar(largo).tipo(TipoDato.CURIOSO).formato(formato).build()
        val contexto = prompt.usuario.substringAfter("CONTEXTO:\n")
        assertTrue(contexto.length <= PromptBuilder.MAX_CONTEXTO_CHARS)
        assertTrue(contexto.endsWith("palabra"))
    }

    @Test
    fun `no recorta un contexto corto`() {
        assertTrue(construir().usuario.endsWith("Se construyó entre 1899 y 1920."))
    }

    @Test
    fun `el prompt es corto para ahorrar tokens`() {
        val prompt = construir()
        assertTrue(prompt.sistema.length < 400)
        assertFalse(prompt.usuario.contains("\n\n"))
    }

    @Test
    fun `falla si falta alguna parte`() {
        assertThrows(IllegalStateException::class.java) { PromptBuilder().tipo(TipoDato.CURIOSO).formato(formato).build() }
        assertThrows(IllegalStateException::class.java) { PromptBuilder().lugar(catedral).formato(formato).build() }
        assertThrows(IllegalStateException::class.java) { PromptBuilder().lugar(catedral).tipo(TipoDato.CURIOSO).build() }
    }
}
