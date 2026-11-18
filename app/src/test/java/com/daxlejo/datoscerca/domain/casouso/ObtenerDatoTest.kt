package com.daxlejo.datoscerca.domain.casouso

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.OrigenDato
import com.daxlejo.datoscerca.domain.modelo.Relevancia
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.motor.MotivoOmision
import com.daxlejo.datoscerca.domain.motor.RadioBusqueda
import com.daxlejo.datoscerca.soporte.Banco
import com.daxlejo.datoscerca.soporte.PLAZA
import com.daxlejo.datoscerca.soporte.lugar
import com.daxlejo.datoscerca.soporte.valorExitoso
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val IGLESIA = Coordenada(1.21512, -77.27788) // a unos 65 m de la plaza
private val CATEDRAL = Coordenada(1.21363, -77.28106) // a unos 330 m de la plaza
private val TIENDA = Coordenada(1.21470, -77.27820) // a unos 9 m de la plaza

private val plaza = lugar("prueba:plaza", "Plaza de Nariño", PLAZA)
private val iglesia = lugar("prueba:iglesia", "San Juan Bautista", IGLESIA)
private val catedral = lugar("prueba:catedral", "Catedral de Pasto", CATEDRAL)
private val tienda = lugar("prueba:tienda", "Tienda La Esquina", TIENDA, relevancia = Relevancia.NINGUNA)

// Varios lugares relevantes en el mismo punto: cada dato automático encuentra uno nuevo.
private fun muchosEnLaPlaza(cantidad: Int) = (1..cantidad).map { lugar("prueba:sitio-$it", "Sitio $it", PLAZA) }

private fun con(intensidad: Intensidad, modoDemo: Boolean = false) =
    Configuracion.PorDefecto.copy(intensidad = intensidad, modoDemo = modoDemo)

class ObtenerDatoParaUbicacionTest {

    @Test
    fun `al pasar por un lugar relevante genera, guarda como automatico y avisa`() = runTest {
        val banco = Banco(listOf(plaza))

        val resultado = banco.obtenerDatoParaUbicacion(PLAZA)

        val entrada = (resultado as Exploracion.NuevoDato).entrada
        assertEquals("Dato generado.", entrada.dato.texto)
        assertEquals(plaza.id, entrada.dato.lugar.id)
        assertEquals(OrigenDato.AUTOMATICO, entrada.dato.origen)
        assertEquals(banco.reloj.ahora, entrada.dato.generadoEnMs)
        assertEquals(listOf(entrada), banco.historial.actuales)
        assertEquals(1, banco.vibrador.vibraciones)
        assertEquals(200, banco.fuente.busquedas.single())
    }

    @Test
    fun `en pausa no busca lugares ni llama a la IA`() = runTest {
        val banco = Banco(listOf(plaza), Configuracion.PorDefecto.copy(pausa = true))

        val resultado = banco.obtenerDatoParaUbicacion(PLAZA)

        assertEquals(Exploracion.Fallo(ErrorDato.EnPausa), resultado)
        assertTrue(banco.fuente.busquedas.isEmpty())
        assertTrue(banco.generador.solicitudes.isEmpty())
    }

    @Test
    fun `un lugar relevante fuera del radio de llegada no da dato`() = runTest {
        val banco = Banco(listOf(catedral))

        val resultado = banco.obtenerDatoParaUbicacion(PLAZA)

        assertEquals(Exploracion.Omitido(MotivoOmision.NADA_RELEVANTE_CERCA, lugarCercano = null), resultado)
        assertTrue(banco.generador.solicitudes.isEmpty())
    }

    @Test
    fun `un lugar no relevante no da dato aunque este encima`() = runTest {
        val banco = Banco(listOf(tienda))

        val resultado = banco.obtenerDatoParaUbicacion(TIENDA)

        assertEquals(Exploracion.Omitido(MotivoOmision.NADA_RELEVANTE_CERCA, lugarCercano = null), resultado)
        assertTrue(banco.generador.solicitudes.isEmpty())
    }

    @Test
    fun `prefiere el lugar relevante aunque uno comun quede mas cerca`() = runTest {
        val banco = Banco(listOf(tienda, iglesia))

        val resultado = banco.obtenerDatoParaUbicacion(TIENDA) as Exploracion.NuevoDato

        assertEquals(iglesia.id, resultado.entrada.dato.lugar.id)
    }

    @Test
    fun `suave da como maximo un dato automatico por hora`() = runTest {
        assertLimitePorHora(Intensidad.SUAVE, maximo = 1)
    }

    @Test
    fun `normal da como maximo dos datos automaticos por hora`() = runTest {
        assertLimitePorHora(Intensidad.NORMAL, maximo = 2)
    }

    @Test
    fun `intenso da como maximo cuatro datos automaticos por hora`() = runTest {
        assertLimitePorHora(Intensidad.INTENSO, maximo = 4)
    }

    // Llena el cupo con un dato por minuto, comprueba el rechazo y que se libera al salir el primero de la ventana.
    private suspend fun assertLimitePorHora(intensidad: Intensidad, maximo: Int) {
        val banco = Banco(muchosEnLaPlaza(maximo + 2), con(intensidad))
        repeat(maximo) {
            assertTrue(banco.obtenerDatoParaUbicacion(PLAZA) is Exploracion.NuevoDato)
            banco.reloj.avanzarMinutos(1)
        }

        val enElLimite = banco.obtenerDatoParaUbicacion(PLAZA)
        banco.reloj.avanzarMinutos(60 - maximo + 1)
        val alLiberarse = banco.obtenerDatoParaUbicacion(PLAZA)

        assertEquals(Exploracion.Omitido(MotivoOmision.LIMITE_POR_HORA, lugarCercano = null), enElLimite)
        assertTrue(alLiberarse is Exploracion.NuevoDato)
        assertEquals(maximo + 1, banco.generador.solicitudes.size)
    }

    @Test
    fun `la ventana es movil y no un intervalo fijo entre datos`() = runTest {
        val banco = Banco(muchosEnLaPlaza(4), con(Intensidad.NORMAL))
        banco.obtenerDatoParaUbicacion(PLAZA) // minuto 0
        banco.reloj.avanzarMinutos(40)
        banco.obtenerDatoParaUbicacion(PLAZA) // minuto 40

        banco.reloj.avanzarMinutos(21)
        val minuto61 = banco.obtenerDatoParaUbicacion(PLAZA) // el del minuto 0 ya salió de la ventana
        banco.reloj.avanzarMinutos(1)
        val minuto62 = banco.obtenerDatoParaUbicacion(PLAZA) // siguen dentro los de los minutos 40 y 61

        assertTrue(minuto61 is Exploracion.NuevoDato)
        assertEquals(MotivoOmision.LIMITE_POR_HORA, (minuto62 as Exploracion.Omitido).motivo)
    }

    @Test
    fun `al llegar al limite no consulta lugares`() = runTest {
        val banco = Banco(muchosEnLaPlaza(2), con(Intensidad.SUAVE))
        banco.obtenerDatoParaUbicacion(PLAZA)
        val busquedas = banco.fuente.busquedas.size

        banco.obtenerDatoParaUbicacion(PLAZA)

        assertEquals(busquedas, banco.fuente.busquedas.size)
    }

    @Test
    fun `el modo demo ignora el limite por hora`() = runTest {
        val banco = Banco(muchosEnLaPlaza(3), con(Intensidad.SUAVE, modoDemo = true))

        val resultados = (1..3).map { banco.obtenerDatoParaUbicacion(PLAZA) }

        assertTrue(resultados.all { it is Exploracion.NuevoDato })
        assertTrue(banco.generador.solicitudes.all { it.prompt.usuario.contains("una sola frase") })
    }

    @Test
    fun `los datos manuales no gastan el cupo automatico`() = runTest {
        val banco = Banco(muchosEnLaPlaza(3), con(Intensidad.SUAVE))
        banco.obtenerDatoAhora()
        banco.obtenerDatoAhora()

        assertTrue(banco.obtenerDatoParaUbicacion(PLAZA) is Exploracion.NuevoDato)
    }

    @Test
    fun `no repite el lugar donde ya se mostro un dato`() = runTest {
        val banco = Banco(listOf(plaza), con(Intensidad.INTENSO))
        banco.obtenerDatoParaUbicacion(PLAZA)
        banco.reloj.avanzarMinutos(10)

        val resultado = banco.obtenerDatoParaUbicacion(PLAZA) as Exploracion.Omitido

        assertEquals(MotivoOmision.LUGAR_YA_MOSTRADO, resultado.motivo)
        assertEquals(plaza.id, resultado.lugarCercano?.id)
        assertEquals(1, banco.generador.solicitudes.size)
    }

    @Test
    fun `si el mas cercano ya se mostro pasa al siguiente relevante`() = runTest {
        val banco = Banco(listOf(plaza, iglesia), con(Intensidad.INTENSO))

        banco.obtenerDatoParaUbicacion(PLAZA)
        val segundo = banco.obtenerDatoParaUbicacion(PLAZA) as Exploracion.NuevoDato

        assertEquals(iglesia.id, segundo.entrada.dato.lugar.id)
    }

    @Test
    fun `un lugar vuelve a contar como nuevo despues de la ventana de repeticion`() = runTest {
        val banco = Banco(listOf(plaza), con(Intensidad.INTENSO))
        banco.obtenerDatoParaUbicacion(PLAZA)
        banco.reloj.avanzarMinutos(31)

        assertTrue(banco.obtenerDatoParaUbicacion(PLAZA) is Exploracion.NuevoDato)
    }

    @Test
    fun `los fallos de lugares, IA e historial se propagan sin guardar`() = runTest {
        val sinRed = Banco(listOf(plaza)).apply { fuente.fallo = ErrorDato.SinInternet }
        val iaCaida = Banco(listOf(plaza)).apply { generador.respuesta = Resultado.Fallo(ErrorDato.ErrorIa("timeout")) }
        val discoLleno = Banco(listOf(plaza)).apply { historial.fallo = ErrorDato.Almacenamiento("lleno") }

        assertEquals(Exploracion.Fallo(ErrorDato.SinInternet), sinRed.obtenerDatoParaUbicacion(PLAZA))
        assertEquals(Exploracion.Fallo(ErrorDato.ErrorIa("timeout")), iaCaida.obtenerDatoParaUbicacion(PLAZA))
        assertEquals(Exploracion.Fallo(ErrorDato.Almacenamiento("lleno")), discoLleno.obtenerDatoParaUbicacion(PLAZA))
        assertTrue(iaCaida.historial.actuales.isEmpty())
        assertEquals(0, iaCaida.vibrador.vibraciones)
    }

    @Test
    fun `una respuesta vacia de la IA es un error y no se guarda`() = runTest {
        val banco = Banco(listOf(plaza)).apply { generador.respuesta = Resultado.Exito("   ") }

        val resultado = banco.obtenerDatoParaUbicacion(PLAZA) as Exploracion.Fallo

        assertTrue(resultado.error is ErrorDato.ErrorIa)
        assertTrue(banco.historial.actuales.isEmpty())
    }

    @Test
    fun `el prompt lleva el tipo elegido y el largo de la intensidad`() = runTest {
        val configuracion = con(Intensidad.INTENSO).copy(tipos = setOf(TipoDato.GASTRONOMICO))
        val banco = Banco(listOf(plaza), configuracion)

        banco.obtenerDatoParaUbicacion(PLAZA)

        val solicitud = banco.generador.solicitudes.single()
        assertEquals(TipoDato.GASTRONOMICO, solicitud.tipo)
        assertEquals(Intensidad.INTENSO, solicitud.intensidad)
        assertTrue(solicitud.prompt.usuario.contains("párrafo corto"))
        assertTrue(solicitud.prompt.usuario.contains(plaza.contexto))
    }

    @Test
    fun `voz y vibracion siguen la configuracion`() = runTest {
        val conVoz = Banco(listOf(plaza), Configuracion.PorDefecto.copy(vozActiva = true, vibracionActiva = false))
        val callado = Banco(listOf(plaza), Configuracion.PorDefecto.copy(vozActiva = false, vibracionActiva = false))

        conVoz.obtenerDatoParaUbicacion(PLAZA)
        callado.obtenerDatoParaUbicacion(PLAZA)

        assertEquals(listOf("Dato generado."), conVoz.narrador.leidos)
        assertEquals(0, conVoz.vibrador.vibraciones)
        assertTrue(callado.narrador.leidos.isEmpty())
    }
}

class ObtenerDatoAhoraTest {

    @Test
    fun `busca en el radio amplio y guarda el dato como manual`() = runTest {
        val banco = Banco(listOf(catedral))

        val resultado = banco.obtenerDatoAhora().valorExitoso()

        assertEquals(catedral.id, resultado.dato.lugar.id)
        assertEquals(OrigenDato.MANUAL, resultado.dato.origen)
        assertTrue(resultado.dato.lugar.distanciaMetros in 300..400)
        assertEquals(RadioBusqueda.CERCANIA_METROS, banco.fuente.busquedas.single())
        assertEquals(1, banco.historial.actuales.size)
    }

    @Test
    fun `ignora el limite por hora aunque el cupo automatico este lleno`() = runTest {
        val banco = Banco(listOf(plaza, iglesia, catedral), con(Intensidad.SUAVE))
        banco.obtenerDatoParaUbicacion(PLAZA)

        val manuales = (1..3).map { banco.obtenerDatoAhora() }

        assertTrue(manuales.all { it is Resultado.Exito })
    }

    @Test
    fun `solo habla de lugares relevantes`() = runTest {
        val soloComun = Banco(listOf(tienda))
        val conAmbos = Banco(listOf(tienda, catedral))
        soloComun.ubicacion.actual = Resultado.Exito(TIENDA)
        conAmbos.ubicacion.actual = Resultado.Exito(TIENDA)

        assertEquals(Resultado.Fallo(ErrorDato.SinLugares), soloComun.obtenerDatoAhora())
        assertEquals(catedral.id, conAmbos.obtenerDatoAhora().valorExitoso().dato.lugar.id)
        assertTrue(soloComun.generador.solicitudes.isEmpty())
    }

    @Test
    fun `prefiere un lugar que no se haya mostrado hace poco`() = runTest {
        val banco = Banco(listOf(plaza, iglesia, catedral))

        val lugares = (1..3).map { banco.obtenerDatoAhora().valorExitoso().dato.lugar.id }

        assertEquals(listOf(plaza.id, iglesia.id, catedral.id), lugares)
    }

    @Test
    fun `si todo se mostro repite el mas cercano antes que fallar`() = runTest {
        val banco = Banco(listOf(plaza))
        banco.obtenerDatoAhora()

        val repetido = banco.obtenerDatoAhora().valorExitoso()

        assertEquals(plaza.id, repetido.dato.lugar.id)
        assertEquals(2, banco.historial.actuales.size)
    }

    @Test
    fun `con varios tipos marcados los recorre en orden`() = runTest {
        val tipos = setOf(TipoDato.INTERESANTE, TipoDato.COMICO)
        val banco = Banco(listOf(plaza), Configuracion.PorDefecto.copy(tipos = tipos))

        val elegidos = (1..3).map { banco.obtenerDatoAhora().valorExitoso().dato.tipo }

        assertEquals(listOf(TipoDato.INTERESANTE, TipoDato.COMICO, TipoDato.INTERESANTE), elegidos)
    }

    @Test
    fun `en aleatorio solo salen tipos marcados`() = runTest {
        val tipos = setOf(TipoDato.CURIOSO, TipoDato.GASTRONOMICO)
        val banco = Banco(listOf(plaza), Configuracion.PorDefecto.copy(tipos = tipos, aleatorio = true))

        val elegidos = (1..12).map { banco.obtenerDatoAhora().valorExitoso().dato.tipo }.toSet()

        assertEquals(tipos, elegidos)
    }

    @Test
    fun `la pausa tambien detiene el dato manual`() = runTest {
        val banco = Banco(listOf(plaza), Configuracion.PorDefecto.copy(pausa = true))

        assertEquals(Resultado.Fallo(ErrorDato.EnPausa), banco.obtenerDatoAhora())
        assertTrue(banco.generador.solicitudes.isEmpty())
    }

    @Test
    fun `propaga cada fallo del camino`() = runTest {
        val sinPermiso = Banco().apply { ubicacion.actual = Resultado.Fallo(ErrorDato.SinPermiso) }
        val sinGps = Banco().apply { ubicacion.actual = Resultado.Fallo(ErrorDato.SinGps) }
        val sinLugares = Banco(emptyList())
        val sinRed = Banco().apply { fuente.fallo = ErrorDato.SinInternet }
        val iaCaida = Banco().apply { generador.respuesta = Resultado.Fallo(ErrorDato.ErrorIa(null)) }

        assertEquals(Resultado.Fallo(ErrorDato.SinPermiso), sinPermiso.obtenerDatoAhora())
        assertEquals(Resultado.Fallo(ErrorDato.SinGps), sinGps.obtenerDatoAhora())
        assertEquals(Resultado.Fallo(ErrorDato.SinLugares), sinLugares.obtenerDatoAhora())
        assertEquals(Resultado.Fallo(ErrorDato.SinInternet), sinRed.obtenerDatoAhora())
        assertEquals(Resultado.Fallo(ErrorDato.ErrorIa(null)), iaCaida.obtenerDatoAhora())
        assertTrue(sinPermiso.fuente.busquedas.isEmpty())
    }
}

/** El dato manual y el automático comparten una sola generación en curso: nunca salen dos seguidos. */
@OptIn(ExperimentalCoroutinesApi::class)
class GeneracionEnCursoTest {

    @Test
    fun `con un dato manual en camino el automatico se omite sin llamar a la IA`() = runTest {
        val banco = Banco(listOf(plaza, iglesia), con(Intensidad.INTENSO))
        val soltar = CompletableDeferred<Unit>().also { banco.generador.retener = it }
        val manual = async { banco.obtenerDatoAhora() }
        runCurrent()

        val automatico = banco.obtenerDatoParaUbicacion(PLAZA)
        soltar.complete(Unit)

        assertEquals(Exploracion.Omitido(MotivoOmision.GENERACION_EN_CURSO, lugarCercano = null), automatico)
        assertTrue(manual.await() is Resultado.Exito)
        assertEquals(1, banco.generador.solicitudes.size)
        assertEquals(1, banco.historial.actuales.size)
    }

    @Test
    fun `con un dato automatico en camino el manual responde que ya viene uno`() = runTest {
        val banco = Banco(listOf(plaza, iglesia), con(Intensidad.INTENSO))
        val soltar = CompletableDeferred<Unit>().also { banco.generador.retener = it }
        val automatico = async { banco.obtenerDatoParaUbicacion(PLAZA) }
        runCurrent()

        val manual = banco.obtenerDatoAhora()
        soltar.complete(Unit)

        assertEquals(Resultado.Fallo(ErrorDato.GeneracionEnCurso), manual)
        assertTrue(automatico.await() is Exploracion.NuevoDato)
        assertEquals(1, banco.historial.actuales.size)
    }

    @Test
    fun `al terminar el primero el siguiente pedido se atiende`() = runTest {
        val banco = Banco(listOf(plaza, iglesia), con(Intensidad.INTENSO))

        banco.obtenerDatoParaUbicacion(PLAZA)

        assertTrue(banco.obtenerDatoAhora() is Resultado.Exito)
    }
}
