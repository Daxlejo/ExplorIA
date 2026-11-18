package com.daxlejo.datoscerca.data.historial

import android.util.Log
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Dato
import com.daxlejo.datoscerca.domain.modelo.EntradaHistorial
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.OrigenDato
import com.daxlejo.datoscerca.domain.modelo.Relevancia
import com.daxlejo.datoscerca.domain.modelo.TipoDato

fun Dato.aEntity(): EntradaEntity = EntradaEntity(
    lugarId = lugar.id,
    lugarNombre = lugar.nombre,
    lugarFuente = lugar.fuente,
    lugarRelevancia = lugar.relevancia.name,
    latitud = lugar.coordenada.latitud,
    longitud = lugar.coordenada.longitud,
    distanciaMetros = lugar.distanciaMetros,
    texto = texto,
    tipo = tipo.name,
    intensidad = intensidad.name,
    generadoEnMs = generadoEnMs,
    favorito = false,
    origen = origen.name,
)

// El contexto del lugar no se guarda: solo sirve para generar el dato.
fun EntradaEntity.aDominio(): EntradaHistorial = EntradaHistorial(
    id = id,
    dato = Dato(
        texto = texto,
        tipo = tipoDesde(tipo),
        intensidad = intensidadDesde(intensidad),
        lugar = Lugar(
            id = lugarId,
            nombre = lugarNombre,
            coordenada = Coordenada(latitud, longitud),
            distanciaMetros = distanciaMetros,
            contexto = "",
            fuente = lugarFuente,
            relevancia = enumDesde(lugarRelevancia, Relevancia.NINGUNA),
        ),
        generadoEnMs = generadoEnMs,
        origen = enumDesde(origen, OrigenDato.MANUAL),
    ),
    favorito = favorito,
)

// Un valor desconocido (versión futura) cae en el tipo más neutro en lugar de romper el historial.
private fun tipoDesde(nombre: String): TipoDato = enumDesde(nombre, TipoDato.INTERESANTE)

private fun intensidadDesde(nombre: String): Intensidad = enumDesde(nombre, Intensidad.NORMAL)

private inline fun <reified E : Enum<E>> enumDesde(nombre: String, porDefecto: E): E =
    enumValues<E>().firstOrNull { it.name == nombre } ?: porDefecto.also { avisarDesconocido(nombre) }

private fun avisarDesconocido(valor: String) {
    Log.w("EntradaMapper", "Valor desconocido en el historial: $valor. Se usa el valor por defecto.")
}
