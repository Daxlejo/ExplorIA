package com.daxlejo.datoscerca.domain.motor

import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.error.encadenar
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.modelo.Dato
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.OrigenDato
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.puerto.GeneradorDatos
import com.daxlejo.datoscerca.domain.puerto.Narrador
import com.daxlejo.datoscerca.domain.puerto.Reloj
import com.daxlejo.datoscerca.domain.puerto.RepositorioHistorial
import com.daxlejo.datoscerca.domain.puerto.SolicitudGeneracion
import com.daxlejo.datoscerca.domain.puerto.Vibrador

/** El radio del dato automático vive en [FiltroRelevancia.radioLlegadaMetros]. */
object RadioBusqueda {
    /** Dato pedido a mano: cualquier lugar emblemático que quede a pocos minutos caminando. */
    const val CERCANIA_METROS = 500
}

/** Avisa de un dato recién guardado según las preferencias: vibración y voz. */
class AvisadorDato(private val vibrador: Vibrador, private val narrador: Narrador) {
    fun avisar(entrada: EntradaHistorial, configuracion: Configuracion) {
        if (configuracion.vibracionActiva) vibrador.vibrarNuevoDato()
        // Si la voz falla, el dato igual se muestra en pantalla; el narrador registra el motivo.
        if (configuracion.vozActiva) narrador.hablar(entrada.dato.texto)
    }
}

/**
 * Tramo común de los datos manuales y automáticos, una vez elegido el lugar:
 * tipo -> prompt -> IA -> historial -> aviso.
 */
class ProducirDato(
    private val generador: GeneradorDatos,
    private val historial: RepositorioHistorial,
    private val fabricaSelectores: FabricaSelectores,
    private val fabricaPoliticas: FabricaPoliticas,
    private val avisador: AvisadorDato,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(
        lugar: Lugar,
        configuracion: Configuracion,
        recientes: List<EntradaHistorial>,
        origen: OrigenDato,
    ): Resultado<EntradaHistorial> {
        val tipo = elegirTipoConVariedad(lugar, configuracion, recientes)
        val previos = textosRecientesDeClave(lugar.id, tipo, configuracion, recientes)
        val solicitud = solicitud(lugar, tipo, configuracion, previos)
        return generador.generar(solicitud)
            .encadenar { texto -> guardar(texto, solicitud, origen) }
            .also { if (it is Resultado.Exito) avisador.avisar(it.valor, configuracion) }
    }

    private fun elegirTipoConVariedad(
        lugar: Lugar,
        configuracion: Configuracion,
        recientes: List<EntradaHistorial>,
    ): TipoDato {
        val preferido = fabricaSelectores.para(configuracion)
            .seleccionar(configuracion.tipos, recientes.firstOrNull()?.dato?.tipo)
        val vistosPreferido = textosRecientesDeClave(lugar.id, preferido, configuracion, recientes)
        if (vistosPreferido.size < SolicitudGeneracion.MAX_VARIANTES_POR_CLAVE || configuracion.tipos.size <= 1) {
            return preferido
        }
        val ordenados = configuracion.tipos.sorted()
        val indiceInicial = ordenados.indexOf(preferido).coerceAtLeast(0)
        val rotados = (1 until ordenados.size).map { desplazamiento ->
            ordenados[(indiceInicial + desplazamiento) % ordenados.size]
        }
        return rotados.firstOrNull { candidato ->
            textosRecientesDeClave(lugar.id, candidato, configuracion, recientes).size <
                SolicitudGeneracion.MAX_VARIANTES_POR_CLAVE
        } ?: rotados.first()
    }

    private fun textosRecientesDeClave(
        idLugar: String,
        tipo: TipoDato,
        configuracion: Configuracion,
        recientes: List<EntradaHistorial>,
    ): List<String> = recientes
        .asSequence()
        .filter {
            it.dato.lugar.id == idLugar &&
                it.dato.tipo == tipo &&
                it.dato.intensidad == configuracion.intensidad
        }
        .map { it.dato.texto.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()

    private fun solicitud(
        lugar: Lugar,
        tipo: TipoDato,
        configuracion: Configuracion,
        previos: List<String>,
    ): SolicitudGeneracion {
        val formato = fabricaPoliticas.para(configuracion.intensidad).formato
        val prompt = PromptBuilder()
            .lugar(lugar)
            .tipo(tipo)
            .formato(formato)
            .evitar(previos)
            .build()
        return SolicitudGeneracion(
            lugar = lugar,
            tipo = tipo,
            intensidad = configuracion.intensidad,
            prompt = prompt,
            textosExcluidos = previos,
        )
    }

    private suspend fun guardar(
        texto: String,
        solicitud: SolicitudGeneracion,
        origen: OrigenDato,
    ): Resultado<EntradaHistorial> {
        if (texto.isBlank()) return Resultado.Fallo(ErrorDato.ErrorIa("La IA respondió vacío"))
        val dato = Dato(texto.trim(), solicitud.tipo, solicitud.intensidad, solicitud.lugar, reloj.ahoraMs(), origen)
        return historial.guardar(dato)
    }
}
