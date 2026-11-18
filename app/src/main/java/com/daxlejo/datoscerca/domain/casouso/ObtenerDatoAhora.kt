package com.daxlejo.datoscerca.domain.casouso

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.error.encadenar
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.OrigenDato
import com.daxlejo.datoscerca.domain.motor.FiltroRelevancia
import com.daxlejo.datoscerca.domain.motor.FiltroRepeticion
import com.daxlejo.datoscerca.domain.motor.GeneracionUnica
import com.daxlejo.datoscerca.domain.motor.ProducirDato
import com.daxlejo.datoscerca.domain.motor.RadioBusqueda
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import com.daxlejo.datoscerca.domain.puerto.ProveedorUbicacion
import com.daxlejo.datoscerca.domain.puerto.Reloj
import com.daxlejo.datoscerca.domain.puerto.RepositorioConfiguracion
import com.daxlejo.datoscerca.domain.puerto.RepositorioHistorial
import kotlinx.coroutines.flow.first

/**
 * Dato pedido a mano ("Dato ahora"). Ignora el límite por hora (la persona lo pidió) y no cuenta
 * para él, pero respeta la pausa, la generación única y solo habla de lugares emblemáticos.
 */
class ObtenerDatoAhora(
    private val ubicacion: ProveedorUbicacion,
    private val fuente: FuenteLugares,
    private val historial: RepositorioHistorial,
    private val configuracion: RepositorioConfiguracion,
    private val filtroRepeticion: FiltroRepeticion,
    private val filtroRelevancia: FiltroRelevancia,
    private val producir: ProducirDato,
    private val generacion: GeneracionUnica,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(): Resultado<EntradaHistorial> {
        val actual = configuracion.observar().first()
        if (actual.pausa) return Resultado.Fallo(ErrorDato.EnPausa)
        return generacion.siEstaLibre { producirManual(actual) } ?: Resultado.Fallo(ErrorDato.GeneracionEnCurso)
    }

    private suspend fun producirManual(actual: Configuracion): Resultado<EntradaHistorial> =
        ubicacion.ubicacionActual().encadenar { coordenada ->
            historial.recientes(filtroRepeticion.maxRecientes).encadenar { recientes ->
                elegirLugar(coordenada, recientes)
                    .encadenar { lugar -> producir(lugar, actual, recientes, OrigenDato.MANUAL) }
            }
        }

    // Si todo lo cercano ya se mostró, se repite el más cercano antes que dejar a la persona sin dato.
    private suspend fun elegirLugar(coordenada: Coordenada, recientes: List<EntradaHistorial>): Resultado<Lugar> =
        fuente.buscarCercanos(coordenada, RadioBusqueda.CERCANIA_METROS).encadenar { cercanos ->
            val emblematicos = filtroRelevancia.emblematicos(cercanos)
            val excluir = filtroRepeticion.idsAExcluir(recientes, reloj.ahoraMs())
            val elegido = emblematicos.firstOrNull { it.id !in excluir } ?: emblematicos.firstOrNull()
            elegido?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ErrorDato.SinLugares)
        }
}
