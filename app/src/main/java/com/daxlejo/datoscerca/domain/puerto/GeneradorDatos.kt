package com.daxlejo.datoscerca.domain.puerto

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.motor.Prompt

/** Lugar, tipo e intensidad identifican la petición; [prompt] es lo que se envía. */
data class SolicitudGeneracion(
    val lugar: Lugar,
    val tipo: TipoDato,
    val intensidad: Intensidad,
    val prompt: Prompt,
) {
    /** Clave estable para cachear: no depende de la distancia ni del contexto del momento. */
    val claveCache: String get() = "${lugar.id}|${tipo.name}|${intensidad.name}"
}

interface GeneradorDatos {
    suspend fun generar(solicitud: SolicitudGeneracion): Resultado<String>
}
