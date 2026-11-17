package com.daxlejo.datoscerca.domain.casouso

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.error.encadenar
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.motor.FiltroRepeticion
import com.daxlejo.datoscerca.domain.motor.ProducirDato
import com.daxlejo.datoscerca.domain.motor.RadioBusqueda
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import com.daxlejo.datoscerca.domain.puerto.ProveedorUbicacion
import com.daxlejo.datoscerca.domain.puerto.Reloj
import com.daxlejo.datoscerca.domain.puerto.RepositorioConfiguracion
import com.daxlejo.datoscerca.domain.puerto.RepositorioHistorial
import kotlinx.coroutines.flow.first

/**
 * Dato pedido a mano. No espera el intervalo de la intensidad (la persona lo pidió),
 * pero sí respeta la pausa y prefiere lugares que no se hayan mostrado hace poco.
 */
class ObtenerDatoAhora(
    private val ubicacion: ProveedorUbicacion,
    private val fuente: FuenteLugares,
    private val historial: RepositorioHistorial,
    private val configuracion: RepositorioConfiguracion,
    private val filtro: FiltroRepeticion,
    private val producir: ProducirDato,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(): Resultado<EntradaHistorial> {
        val actual = configuracion.observar().first()
        if (actual.pausa) return Resultado.Fallo(ErrorDato.EnPausa)
        return ubicacion.ubicacionActual().encadenar { coordenada ->
            historial.recientes(filtro.maxRecientes).encadenar { recientes ->
                val excluir = filtro.idsAExcluir(recientes, reloj.ahoraMs())
                buscarLugar(coordenada, excluir).encadenar { lugar -> producir(lugar, actual, recientes) }
            }
        }
    }

    // Si todo lo cercano ya se mostró, se repite el más cercano antes que dejar a la persona sin dato.
    private suspend fun buscarLugar(coordenada: Coordenada, excluir: Set<String>): Resultado<Lugar> {
        val novedad = fuente.buscarCercano(coordenada, RadioBusqueda.CERCANIA_METROS, excluir)
        val todoVisto = novedad is Resultado.Fallo && novedad.error == ErrorDato.SinLugares && excluir.isNotEmpty()
        return if (todoVisto) fuente.buscarCercano(coordenada, RadioBusqueda.CERCANIA_METROS) else novedad
    }
}
