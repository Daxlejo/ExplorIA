package com.daxlejo.datoscerca.ui.historial

import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.soporte.dato
import com.daxlejo.datoscerca.soporte.lugar
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgrupadorHistorialTest {
    private val bogota = ZoneId.of("America/Bogota")
    private val agrupador = AgrupadorHistorial(bogota)

    private fun instante(dia: Int, hora: Int, minuto: Int = 0): Long =
        LocalDateTime.of(2026, 11, dia, hora, minuto).atZone(bogota).toInstant().toEpochMilli()

    private fun entrada(id: Long, dia: Int, hora: Int, favorito: Boolean = false) =
        EntradaHistorial(id, dato(lugar(id = "lugar-$id"), generadoEnMs = instante(dia, hora)), favorito)

    private val ahora = instante(dia = 16, hora = 18)

    @Test
    fun `sin entradas no hay grupos`() {
        assertTrue(agrupador.agrupar(emptyList(), ahora).isEmpty())
    }

    @Test
    fun `agrupa por dia con hoy, ayer y fecha`() {
        val entradas = listOf(entrada(4, 16, 15), entrada(3, 16, 9), entrada(2, 15, 20), entrada(1, 10, 12))

        val grupos = agrupador.agrupar(entradas, ahora)

        assertEquals(
            listOf(DiaHistorial.Hoy, DiaHistorial.Ayer, DiaHistorial.Fecha("10 de noviembre")),
            grupos.map(GrupoDia::dia),
        )
        assertEquals(listOf(listOf(4L, 3L), listOf(2L), listOf(1L)), grupos.map { grupo -> grupo.filas.map { it.id } })
    }

    @Test
    fun `el cambio de dia sigue la zona horaria del reloj`() {
        val casiMedianoche = EntradaHistorial(1, dato(generadoEnMs = instante(15, 23, 59)), favorito = false)

        assertEquals(DiaHistorial.Ayer, agrupador.agrupar(listOf(casiMedianoche), ahora).single().dia)
    }

    @Test
    fun `cada fila lleva lugar, hora y favorito`() {
        val fila = agrupador.agrupar(listOf(entrada(7, 16, 15, favorito = true)), ahora).single().filas.single()

        assertEquals(7L, fila.id)
        assertEquals("Plaza de Nariño", fila.lugar)
        assertTrue(fila.favorito)
        assertTrue(fila.hora.startsWith("3:00"))
    }
}
