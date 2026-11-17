package com.daxlejo.datoscerca.domain.casouso

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.soporte.RepositorioConfiguracionEnMemoria
import com.daxlejo.datoscerca.soporte.RepositorioHistorialEnMemoria
import com.daxlejo.datoscerca.soporte.dato
import com.daxlejo.datoscerca.soporte.lugar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CasosDeConfiguracionTest {
    private val repositorio = RepositorioConfiguracionEnMemoria()
    private val guardar = GuardarConfiguracion(repositorio)
    private val observar = ObservarConfiguracion(repositorio)

    @Test
    fun `observar entrega la configuracion vigente`() = runTest {
        assertEquals(Configuracion.PorDefecto, observar().first())
    }

    @Test
    fun `guardar aplica el cambio y los observadores lo ven`() = runTest {
        val resultado = guardar { it.copy(intensidad = Intensidad.INTENSO, aleatorio = true) }

        assertEquals(Resultado.Exito(Unit), resultado)
        assertEquals(Intensidad.INTENSO, observar().first().intensidad)
        assertTrue(observar().first().aleatorio)
    }

    @Test
    fun `los cambios seguidos se acumulan sin pisarse`() = runTest {
        guardar { it.copy(vozActiva = true) }
        guardar { it.copy(pausa = true) }
        guardar { it.conTipoAlternado(TipoDato.COMICO) }

        val final = observar().first()
        assertTrue(final.vozActiva && final.pausa && TipoDato.COMICO in final.tipos)
    }

    @Test
    fun `intentar desmarcar el ultimo tipo deja la configuracion igual`() = runTest {
        guardar { it.copy(tipos = setOf(TipoDato.CURIOSO)) }

        guardar { it.conTipoAlternado(TipoDato.CURIOSO) }

        assertEquals(setOf(TipoDato.CURIOSO), observar().first().tipos)
    }

    @Test
    fun `un fallo de almacenamiento se devuelve y no cambia nada`() = runTest {
        repositorio.fallo = ErrorDato.Almacenamiento("disco")

        val resultado = guardar { it.copy(pausa = true) }

        assertEquals(Resultado.Fallo(ErrorDato.Almacenamiento("disco")), resultado)
        assertFalse(repositorio.actual.pausa)
    }
}

class CasosDeHistorialTest {
    private val repositorio = RepositorioHistorialEnMemoria()
    private val observarHistorial = ObservarHistorial(repositorio)
    private val observarEntrada = ObservarEntrada(repositorio)
    private val alternarFavorito = AlternarFavorito(repositorio)
    private val borrar = BorrarEntradaHistorial(repositorio)

    private suspend fun guardar(lugarId: String, ms: Long) =
        (repositorio.guardar(dato(lugar(id = lugarId), generadoEnMs = ms)) as Resultado.Exito).valor

    @Test
    fun `observar entrega primero lo mas reciente`() = runTest {
        guardar("plaza", ms = 1)
        guardar("catedral", ms = 2)

        assertEquals(listOf("catedral", "plaza"), observarHistorial().first().map { it.dato.lugar.id })
    }

    @Test
    fun `alternar favorito marca y desmarca`() = runTest {
        val entrada = guardar("plaza", ms = 1)

        alternarFavorito(entrada.id)
        val marcado = observarEntrada(entrada.id).first()?.favorito
        alternarFavorito(entrada.id)
        val desmarcado = observarEntrada(entrada.id).first()?.favorito

        assertEquals(true, marcado)
        assertEquals(false, desmarcado)
    }

    @Test
    fun `alternar favorito no toca las demas entradas`() = runTest {
        val plaza = guardar("plaza", ms = 1)
        val catedral = guardar("catedral", ms = 2)

        alternarFavorito(plaza.id)

        assertEquals(false, observarEntrada(catedral.id).first()?.favorito)
    }

    @Test
    fun `borrar quita la entrada y su observador emite null`() = runTest {
        val plaza = guardar("plaza", ms = 1)
        val catedral = guardar("catedral", ms = 2)

        val resultado = borrar(plaza.id)

        assertEquals(Resultado.Exito(Unit), resultado)
        assertNull(observarEntrada(plaza.id).first())
        assertEquals(listOf(catedral.id), observarHistorial().first().map { it.id })
    }

    @Test
    fun `borrar la ultima entrada deja el historial vacio`() = runTest {
        val unica = guardar("plaza", ms = 1)

        borrar(unica.id)

        assertTrue(observarHistorial().first().isEmpty())
    }

    @Test
    fun `los fallos de almacenamiento se devuelven`() = runTest {
        val entrada = guardar("plaza", ms = 1)
        repositorio.fallo = ErrorDato.Almacenamiento("disco")

        assertEquals(Resultado.Fallo(ErrorDato.Almacenamiento("disco")), alternarFavorito(entrada.id))
        assertEquals(Resultado.Fallo(ErrorDato.Almacenamiento("disco")), borrar(entrada.id))
    }
}
