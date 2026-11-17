package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.EstadoUbicacion
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.soporte.Banco
import com.daxlejo.datoscerca.soporte.PLAZA
import com.daxlejo.datoscerca.soporte.lugar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MotorAutomaticoTest {
    private val catedralCoordenada = Coordenada(1.21363, -77.28106)
    private val lejos = Coordenada(1.2300, -77.2900)
    private val plaza = lugar("prueba:plaza", "Plaza de Nariño", PLAZA)
    private val catedral = lugar("prueba:catedral", "Catedral de Pasto", catedralCoordenada)

    private fun banco(configuracion: Configuracion = Configuracion.PorDefecto.copy(intensidad = Intensidad.INTENSO)) =
        Banco(listOf(plaza, catedral), configuracion)

    private fun motor(banco: Banco) =
        MotorAutomatico(banco.ubicacion, banco.configuracion, banco.obtenerDatoParaUbicacion, banco.narrador, banco.reloj)

    // Arranca el motor y recoge los datos nuevos mientras dura la prueba.
    private fun TestScope.arrancar(motor: MotorAutomatico): List<EntradaHistorial> {
        val nuevos = mutableListOf<EntradaHistorial>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { motor.nuevosDatos.collect { nuevos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { motor.ejecutar() }
        return nuevos
    }

    @Test
    fun `antes de ejecutar esta iniciando`() {
        assertEquals(EstadoMotor.Iniciando, motor(banco()).estado.value)
    }

    @Test
    fun `al llegar a un lugar publica el dato y queda explorando cerca de el`() = runTest {
        val banco = banco()
        val motor = motor(banco)

        val nuevos = arrancar(motor)

        assertEquals(listOf(plaza.id), nuevos.map { it.dato.lugar.id })
        assertEquals(EstadoMotor.Explorando(PLAZA, cercaDe = "Plaza de Nariño"), motor.estado.value)
    }

    @Test
    fun `cada lugar nuevo del recorrido produce un dato`() = runTest {
        val banco = banco()
        val motor = motor(banco)
        val nuevos = arrancar(motor)

        banco.ubicacion.flujo.value = EstadoUbicacion.Disponible(catedralCoordenada)

        assertEquals(listOf(plaza.id, catedral.id), nuevos.map { it.dato.lugar.id })
    }

    @Test
    fun `un movimiento pequeño no vuelve a evaluar`() = runTest {
        val banco = banco()
        val motor = motor(banco)
        arrancar(motor)
        val busquedasAntes = banco.fuente.busquedas.size

        banco.ubicacion.flujo.value = EstadoUbicacion.Disponible(Coordenada(PLAZA.latitud + 0.00005, PLAZA.longitud))

        assertEquals(busquedasAntes, banco.fuente.busquedas.size)
    }

    @Test
    fun `lejos de todo sigue explorando sin dato ni aviso`() = runTest {
        val banco = banco()
        banco.ubicacion.flujo.value = EstadoUbicacion.Disponible(lejos)
        val motor = motor(banco)

        val nuevos = arrancar(motor)

        assertTrue(nuevos.isEmpty())
        assertEquals(EstadoMotor.Explorando(lejos), motor.estado.value)
    }

    @Test
    fun `sin permiso o sin gps queda bloqueado`() = runTest {
        val banco = banco()
        banco.ubicacion.flujo.value = EstadoUbicacion.NoDisponible(ErrorDato.SinPermiso)
        val motor = motor(banco)
        arrancar(motor)
        assertEquals(EstadoMotor.Bloqueado(ErrorDato.SinPermiso), motor.estado.value)

        banco.ubicacion.flujo.value = EstadoUbicacion.NoDisponible(ErrorDato.SinGps)
        assertEquals(EstadoMotor.Bloqueado(ErrorDato.SinGps), motor.estado.value)
    }

    @Test
    fun `al recuperar la ubicacion vuelve a explorar y da el dato`() = runTest {
        val banco = banco()
        banco.ubicacion.flujo.value = EstadoUbicacion.NoDisponible(ErrorDato.SinGps)
        val motor = motor(banco)
        val nuevos = arrancar(motor)

        banco.ubicacion.flujo.value = EstadoUbicacion.Disponible(PLAZA)

        assertEquals(1, nuevos.size)
    }

    @Test
    fun `la pausa detiene los datos y calla la voz`() = runTest {
        val banco = banco(Configuracion.PorDefecto.copy(pausa = true))
        val motor = motor(banco)

        val nuevos = arrancar(motor)

        assertEquals(EstadoMotor.EnPausa, motor.estado.value)
        assertTrue(nuevos.isEmpty())
        assertTrue(banco.generador.solicitudes.isEmpty())
        assertTrue(banco.narrador.detenciones > 0)
    }

    @Test
    fun `al reanudar evalua la ubicacion actual`() = runTest {
        val banco = banco(Configuracion.PorDefecto.copy(pausa = true, intensidad = Intensidad.INTENSO))
        val motor = motor(banco)
        val nuevos = arrancar(motor)

        banco.configuracion.actualizar { it.copy(pausa = false) }

        assertEquals(listOf(plaza.id), nuevos.map { it.dato.lugar.id })
    }

    @Test
    fun `tras un fallo reintenta en el mismo sitio cuando pasa la espera`() = runTest {
        val banco = banco()
        banco.fuente.fallo = ErrorDato.SinInternet
        val motor = motor(banco)
        val nuevos = arrancar(motor)
        banco.fuente.fallo = null
        val casiIgual = Coordenada(PLAZA.latitud + 0.00001, PLAZA.longitud)

        banco.ubicacion.flujo.value = EstadoUbicacion.Disponible(casiIgual)
        val antesDeLaEspera = nuevos.size
        banco.reloj.ahora += MotorAutomatico.ESPERA_REINTENTO_MS
        banco.ubicacion.flujo.value = EstadoUbicacion.Disponible(PLAZA)

        assertEquals(0, antesDeLaEspera)
        assertEquals(listOf(plaza.id), nuevos.map { it.dato.lugar.id })
        assertEquals(EstadoMotor.Explorando(PLAZA, cercaDe = "Plaza de Nariño"), motor.estado.value)
    }

    @Test
    fun `quieto en un lugar recibe el dato cuando se cumple el intervalo`() = runTest {
        val banco = banco(Configuracion.PorDefecto.copy(intensidad = Intensidad.NORMAL))
        val motor = motor(banco)
        val nuevos = arrancar(motor)
        banco.ubicacion.flujo.value = EstadoUbicacion.Disponible(catedralCoordenada)
        val duranteElIntervalo = nuevos.size
        val casiIgual = Coordenada(catedralCoordenada.latitud + 0.00001, catedralCoordenada.longitud)

        banco.reloj.avanzarMinutos(3)
        banco.ubicacion.flujo.value = EstadoUbicacion.Disponible(casiIgual)

        assertEquals(1, duranteElIntervalo)
        assertEquals(listOf(plaza.id, catedral.id), nuevos.map { it.dato.lugar.id })
    }

    @Test
    fun `un fallo automatico queda como aviso sin dejar de explorar`() = runTest {
        val banco = banco()
        banco.fuente.fallo = ErrorDato.SinInternet
        val motor = motor(banco)

        arrancar(motor)

        assertEquals(EstadoMotor.Explorando(PLAZA, cercaDe = null, aviso = ErrorDato.SinInternet), motor.estado.value)
    }
}
