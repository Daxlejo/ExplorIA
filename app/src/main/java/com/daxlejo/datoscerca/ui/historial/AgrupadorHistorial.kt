package com.daxlejo.datoscerca.ui.historial

import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface DiaHistorial {
    data object Hoy : DiaHistorial
    data object Ayer : DiaHistorial
    data class Fecha(val texto: String) : DiaHistorial
}

data class FilaHistorialUi(
    val id: Long,
    val lugar: String,
    val texto: String,
    val tipo: TipoDato,
    val hora: String,
    val favorito: Boolean,
)

data class GrupoDia(val dia: DiaHistorial, val filas: List<FilaHistorialUi>)

/** Convierte el historial del dominio en grupos por día listos para pintar. */
class AgrupadorHistorial(
    private val zona: ZoneId = ZoneId.systemDefault(),
    idioma: Locale = Locale.forLanguageTag("es-CO"),
) {
    private val formatoHora = DateTimeFormatter.ofPattern("h:mm a", idioma)
    private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM", idioma)

    /** [entradas] llega de la más reciente a la más antigua y ese orden se conserva. */
    fun agrupar(entradas: List<EntradaHistorial>, ahoraMs: Long): List<GrupoDia> {
        val hoy = fechaDe(ahoraMs)
        return entradas
            .groupBy { fechaDe(it.dato.generadoEnMs) }
            .map { (fecha, delDia) -> GrupoDia(etiquetar(fecha, hoy), delDia.map(::fila)) }
    }

    private fun fechaDe(instanteMs: Long): LocalDate = Instant.ofEpochMilli(instanteMs).atZone(zona).toLocalDate()

    private fun etiquetar(fecha: LocalDate, hoy: LocalDate): DiaHistorial = when (fecha) {
        hoy -> DiaHistorial.Hoy
        hoy.minusDays(1) -> DiaHistorial.Ayer
        else -> DiaHistorial.Fecha(formatoFecha.format(fecha))
    }

    private fun fila(entrada: EntradaHistorial) = FilaHistorialUi(
        id = entrada.id,
        lugar = entrada.dato.lugar.nombre,
        texto = entrada.dato.texto,
        tipo = entrada.dato.tipo,
        hora = formatoHora.format(Instant.ofEpochMilli(entrada.dato.generadoEnMs).atZone(zona)),
        favorito = entrada.favorito,
    )
}
