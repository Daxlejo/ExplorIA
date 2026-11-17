package com.daxlejo.datoscerca.domain.casouso

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.motor.MotivoOmision
import com.daxlejo.datoscerca.domain.motor.RadioBusqueda
import com.daxlejo.datoscerca.soporte.Banco
import com.daxlejo.datoscerca.soporte.PLAZA
import com.daxlejo.datoscerca.soporte.lugar
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val IGLESIA = Coordenada(1.21512, -77.27788) // a unos 65 m de la plaza
private val CATEDRAL = Coordenada(1.21363, -77.28106) // a unos 330 m de la plaza

private val plaza = lugar("prueba:plaza", "Plaza de Nariño", PLAZA)
private val iglesia = lugar("prueba:iglesia", "San Juan Bautista", IGLESIA)
private val catedral = lugar("prueba:catedral", "Catedral de Pasto", CATEDRAL)

private fun con(intensidad: Intensidad) = Configuracion.PorDefecto.copy(intensidad = intensidad)

class ObtenerDatoParaUbicacionTest {

    @Test
    fun `al llegar a un lugar nuevo genera, guarda y avisa`() = runTest {
        val banco = Banco(listOf(plaza))

        val resultado = banco.obtenerDatoParaUbicacion(PLAZA)

        val entrada = (resultado as Exploracion.NuevoDato).entrada
        assertEquals("Dato generado.", entrada.dato.texto)
        assertEquals(plaza.id, entrada.dato.lugar.id)
        assertEquals(banco.reloj.ahora, entrada.dato.generadoEnMs)
        assertEquals(listOf(entrada), banco.historial.actuales)
        assertEquals(1, banco.vibrador.vibraciones)
        assertEquals(RadioBusqueda.LLEGADA_METROS, banco.fuente.busquedas.single().radioMetros)
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
    fun `lejos de todo se omite sin gastar IA`() = runTest {
        val banco = Banco(listOf(catedral))

        val resultado = banco.obtenerDatoParaUbicacion(PLAZA)

        assertEquals(Exploracion.Omitido(MotivoOmision.NADA_CERCA, lugarCercano = null), resultado)
        assertTrue(banco.generador.solicitudes.isEmpty())
    }

    @Test
    fun `normal respeta tres minutos entre datos`() = runTest {
        val banco = Banco(listOf(plaza, catedral), con(Intensidad.NORMAL))
        banco.obtenerDatoParaUbicacion(PLAZA)

        banco.reloj.avanzarMinutos(2)
        val pronto = banco.obtenerDatoParaUbicacion(CATEDRAL) as Exploracion.Omitido
        banco.reloj.avanzarMinutos(1)
        val aTiempo = banco.obtenerDatoParaUbicacion(CATEDRAL)

        assertEquals(MotivoOmision.INTERVALO_NO_CUMPLIDO, pronto.motivo)
        assertEquals(catedral.id, pronto.lugarCercano?.id)
        assertTrue(aTiempo is Exploracion.NuevoDato)
        assertEquals(2, banco.generador.solicitudes.size)
    }

    @Test
    fun `normal no repite el lugar donde ya se mostro un dato`() = runTest {
        val banco = Banco(listOf(plaza), con(Intensidad.NORMAL))
        banco.obtenerDatoParaUbicacion(PLAZA)
        banco.reloj.avanzarMinutos(10)

        val resultado = banco.obtenerDatoParaUbicacion(PLAZA) as Exploracion.Omitido

        assertEquals(MotivoOmision.LUGAR_YA_MOSTRADO, resultado.motivo)
        assertEquals(1, banco.generador.solicitudes.size)
    }

    @Test
    fun `intenso da un dato en cada lugar nuevo sin esperar`() = runTest {
        val banco = Banco(listOf(plaza, catedral), con(Intensidad.INTENSO))

        val primero = banco.obtenerDatoParaUbicacion(PLAZA)
        val segundo = banco.obtenerDatoParaUbicacion(CATEDRAL)
        val repetido = banco.obtenerDatoParaUbicacion(CATEDRAL) as Exploracion.Omitido

        assertTrue(primero is Exploracion.NuevoDato)
        assertTrue(segundo is Exploracion.NuevoDato)
        assertEquals(MotivoOmision.LUGAR_YA_MOSTRADO, repetido.motivo)
    }

    @Test
    fun `suave espera diez minutos y luego pasa al siguiente lugar cercano`() = runTest {
        val banco = Banco(listOf(plaza, iglesia), con(Intensidad.SUAVE))
        banco.obtenerDatoParaUbicacion(PLAZA)

        banco.reloj.avanzarMinutos(9)
        val pronto = banco.obtenerDatoParaUbicacion(PLAZA) as Exploracion.Omitido
        banco.reloj.avanzarMinutos(1)
        val despues = banco.obtenerDatoParaUbicacion(PLAZA) as Exploracion.NuevoDato

        assertEquals(MotivoOmision.INTERVALO_NO_CUMPLIDO, pronto.motivo)
        assertEquals(iglesia.id, despues.entrada.dato.lugar.id)
    }

    @Test
    fun `suave omite si todo lo cercano ya se mostro`() = runTest {
        val banco = Banco(listOf(plaza), con(Intensidad.SUAVE))
        banco.obtenerDatoParaUbicacion(PLAZA)
        banco.reloj.avanzarMinutos(12)

        val resultado = banco.obtenerDatoParaUbicacion(PLAZA) as Exploracion.Omitido

        assertEquals(MotivoOmision.LUGAR_YA_MOSTRADO, resultado.motivo)
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
    fun `busca en el radio amplio y guarda el dato`() = runTest {
        val banco = Banco(listOf(catedral))

        val resultado = banco.obtenerDatoAhora() as Resultado.Exito

        assertEquals(catedral.id, resultado.valor.dato.lugar.id)
        assertTrue(resultado.valor.dato.lugar.distanciaMetros in 300..400)
        assertEquals(RadioBusqueda.CERCANIA_METROS, banco.fuente.busquedas.single().radioMetros)
        assertEquals(1, banco.historial.actuales.size)
    }

    @Test
    fun `no espera el intervalo de la intensidad`() = runTest {
        val banco = Banco(listOf(plaza, iglesia), con(Intensidad.SUAVE))

        val primero = banco.obtenerDatoAhora()
        val segundo = banco.obtenerDatoAhora()

        assertTrue(primero is Resultado.Exito && segundo is Resultado.Exito)
    }

    @Test
    fun `prefiere un lugar que no se haya mostrado hace poco`() = runTest {
        val banco = Banco(listOf(plaza, iglesia, catedral))

        val lugares = (1..3).map { (banco.obtenerDatoAhora() as Resultado.Exito).valor.dato.lugar.id }

        assertEquals(listOf(plaza.id, iglesia.id, catedral.id), lugares)
    }

    @Test
    fun `si todo se mostro repite el mas cercano antes que fallar`() = runTest {
        val banco = Banco(listOf(plaza))
        banco.obtenerDatoAhora()

        val repetido = banco.obtenerDatoAhora() as Resultado.Exito

        assertEquals(plaza.id, repetido.valor.dato.lugar.id)
        assertEquals(2, banco.historial.actuales.size)
    }

    @Test
    fun `con varios tipos marcados los recorre en orden`() = runTest {
        val tipos = setOf(TipoDato.INTERESANTE, TipoDato.COMICO)
        val banco = Banco(listOf(plaza), Configuracion.PorDefecto.copy(tipos = tipos))

        val elegidos = (1..3).map { (banco.obtenerDatoAhora() as Resultado.Exito).valor.dato.tipo }

        assertEquals(listOf(TipoDato.INTERESANTE, TipoDato.COMICO, TipoDato.INTERESANTE), elegidos)
    }

    @Test
    fun `en aleatorio solo salen tipos marcados`() = runTest {
        val tipos = setOf(TipoDato.CURIOSO, TipoDato.GASTRONOMICO)
        val banco = Banco(listOf(plaza), Configuracion.PorDefecto.copy(tipos = tipos, aleatorio = true))

        val elegidos = (1..12).map { (banco.obtenerDatoAhora() as Resultado.Exito).valor.dato.tipo }.toSet()

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
