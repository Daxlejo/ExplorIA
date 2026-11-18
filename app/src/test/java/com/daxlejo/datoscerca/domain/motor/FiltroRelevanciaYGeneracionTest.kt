package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.modelo.Relevancia
import com.daxlejo.datoscerca.soporte.lugar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltroRelevanciaTest {
    private val filtro = FiltroRelevancia()

    private fun emblema(distancia: Int, relevancia: Relevancia = Relevancia.ARTICULO_WIKIPEDIA) =
        lugar(id = "prueba:$relevancia-$distancia", relevancia = relevancia, distanciaMetros = distancia)

    private fun comun(distancia: Int) = emblema(distancia, Relevancia.NINGUNA)

    @Test
    fun `el radio de llegada por defecto es 200 m`() {
        assertEquals(200, filtro.radioLlegadaMetros)
    }

    @Test
    fun `un lugar relevante dentro del radio califica`() {
        assertTrue(filtro.calificaParaLlegada(emblema(150)))
        assertTrue(filtro.calificaParaLlegada(emblema(200)))
        assertTrue(filtro.calificaParaLlegada(emblema(120, Relevancia.ETIQUETA_OSM_PATRIMONIAL)))
    }

    @Test
    fun `un lugar relevante fuera del radio no califica`() {
        assertFalse(filtro.calificaParaLlegada(emblema(201)))
    }

    @Test
    fun `un lugar no relevante no califica ni estando encima`() {
        assertFalse(filtro.calificaParaLlegada(comun(0)))
        assertFalse(filtro.esEmblematico(comun(0)))
    }

    @Test
    fun `al llegar deja solo los relevantes del radio, del mas cercano al mas lejano`() {
        val lejano = emblema(180)
        val cercano = emblema(40)

        val resultado = filtro.alLlegar(listOf(comun(5), lejano, emblema(300), cercano))

        assertEquals(listOf(cercano, lejano), resultado)
    }

    @Test
    fun `para el dato manual cuenta la relevancia y no el radio de llegada`() {
        val lejano = emblema(450)

        assertEquals(listOf(lejano), filtro.emblematicos(listOf(comun(10), lejano)))
    }

    @Test
    fun `el radio de llegada se configura entre 150 y 250 m`() {
        assertEquals(150, FiltroRelevancia(150).radioLlegadaMetros)
        assertEquals(250, FiltroRelevancia(250).radioLlegadaMetros)
        assertTrue(FiltroRelevancia(250).calificaParaLlegada(emblema(240)))
        assertThrows(IllegalArgumentException::class.java) { FiltroRelevancia(149) }
        assertThrows(IllegalArgumentException::class.java) { FiltroRelevancia(251) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GeneracionUnicaTest {

    @Test
    fun `mientras una generacion esta en curso la segunda se descarta`() = runTest {
        val generacion = GeneracionUnica()
        val soltar = CompletableDeferred<Unit>()
        val primera = async { generacion.siEstaLibre { soltar.await(); "primera" } }
        runCurrent()

        val segunda = generacion.siEstaLibre { "segunda" }
        soltar.complete(Unit)

        assertNull(segunda)
        assertEquals("primera", primera.await())
    }

    @Test
    fun `al terminar queda libre otra vez`() = runTest {
        val generacion = GeneracionUnica()

        generacion.siEstaLibre { "primera" }

        assertEquals("segunda", generacion.siEstaLibre { "segunda" })
    }

    @Test
    fun `si la generacion falla tambien se libera`() = runTest {
        val generacion = GeneracionUnica()

        runCatching { generacion.siEstaLibre { error("falla") } }

        assertEquals("despues", generacion.siEstaLibre { "despues" })
    }
}
