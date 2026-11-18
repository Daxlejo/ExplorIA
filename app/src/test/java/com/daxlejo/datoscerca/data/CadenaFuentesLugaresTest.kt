package com.daxlejo.datoscerca.data

import com.daxlejo.datoscerca.data.demo.FuenteLugaresDemo
import com.daxlejo.datoscerca.data.lugares.CadenaFuentesLugares
import com.daxlejo.datoscerca.data.lugares.FuenteMunicipio
import com.daxlejo.datoscerca.data.lugares.FuenteNominatim
import com.daxlejo.datoscerca.data.lugares.LimitadorFrecuencia
import com.daxlejo.datoscerca.data.red.ClienteHttp
import com.daxlejo.datoscerca.data.red.RespuestaHttp
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.Relevancia
import com.daxlejo.datoscerca.domain.motor.FiltroRelevancia
import com.daxlejo.datoscerca.domain.motor.RadioBusqueda
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import com.daxlejo.datoscerca.soporte.PLAZA
import com.daxlejo.datoscerca.soporte.RelojFalso
import com.daxlejo.datoscerca.soporte.lugar
import com.daxlejo.datoscerca.soporte.valorExitoso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FuenteEslabonFalsa(
    val nombre: String,
    var resultado: Resultado<List<Lugar>> = Resultado.Exito(emptyList()),
    private val registro: MutableList<String>,
) : FuenteLugares {
    override suspend fun buscarCercanos(coordenada: Coordenada, radioMetros: Int): Resultado<List<Lugar>> {
        registro += nombre
        return resultado
    }
}

class CadenaFuentesLugaresTest {

    private val lugarWikiEs = lugar(id = "wikipedia-es:1", nombre = "Plaza de Nariño", relevancia = Relevancia.ARTICULO_WIKIPEDIA)
    private val lugarWikiEn = lugar(id = "wikipedia-en:2", nombre = "St. Francis Xavier", relevancia = Relevancia.ARTICULO_WIKIPEDIA)
    private val lugarOsm = lugar(id = "nominatim:way:3", nombre = "Museo Taminango", relevancia = Relevancia.ETIQUETA_OSM_PATRIMONIAL)
    private val calleComun = lugar(id = "nominatim:way:99", nombre = "Calle 18", relevancia = Relevancia.NINGUNA)
    private val lugarWeb = lugar(id = "openai-web:4", nombre = "Monumento Local", relevancia = Relevancia.ARTICULO_WIKIPEDIA)

    @Test
    fun `cuando wikipedia es encuentra un lugar emblematico se detiene sin consultar las demas`() = runTest {
        val llamadas = mutableListOf<String>()
        val wikiEs = FuenteEslabonFalsa("wiki-es", Resultado.Exito(listOf(lugarWikiEs)), llamadas)
        val wikiEn = FuenteEslabonFalsa("wiki-en", Resultado.Exito(listOf(lugarWikiEn)), llamadas)
        val nominatim = FuenteEslabonFalsa("nominatim", Resultado.Exito(listOf(lugarOsm)), llamadas)
        val web = FuenteEslabonFalsa("openai-web", Resultado.Exito(listOf(lugarWeb)), llamadas)
        val cadena = CadenaFuentesLugares(listOf(wikiEs, wikiEn, nominatim, web))

        val resultado = cadena.buscarCercanos(PLAZA, 200).valorExitoso()

        assertEquals(listOf("wiki-es"), llamadas)
        assertEquals(listOf(lugarWikiEs), resultado)
    }

    @Test
    fun `recorre la cadena en orden hasta encontrar el primer lugar emblematico`() = runTest {
        val llamadas = mutableListOf<String>()
        val wikiEs = FuenteEslabonFalsa("wiki-es", Resultado.Exito(emptyList()), llamadas)
        val wikiEn = FuenteEslabonFalsa("wiki-en", Resultado.Exito(emptyList()), llamadas)
        val nominatim = FuenteEslabonFalsa("nominatim", Resultado.Exito(listOf(lugarOsm)), llamadas)
        val web = FuenteEslabonFalsa("openai-web", Resultado.Exito(listOf(lugarWeb)), llamadas)
        val cadena = CadenaFuentesLugares(listOf(wikiEs, wikiEn, nominatim, web))

        val resultado = cadena.buscarCercanos(PLAZA, 200).valorExitoso()

        assertEquals(listOf("wiki-es", "wiki-en", "nominatim"), llamadas)
        assertEquals(listOf(lugarOsm), resultado)
    }

    @Test
    fun `una calle comun sin relevancia no detiene la cadena y pasa al siguiente eslabon`() = runTest {
        val llamadas = mutableListOf<String>()
        val wikiEs = FuenteEslabonFalsa("wiki-es", Resultado.Exito(emptyList()), llamadas)
        val wikiEn = FuenteEslabonFalsa("wiki-en", Resultado.Exito(emptyList()), llamadas)
        val nominatim = FuenteEslabonFalsa("nominatim", Resultado.Exito(listOf(calleComun)), llamadas)
        val web = FuenteEslabonFalsa("openai-web", Resultado.Exito(listOf(lugarWeb)), llamadas)
        val cadena = CadenaFuentesLugares(listOf(wikiEs, wikiEn, nominatim, web))

        val resultado = cadena.buscarCercanos(PLAZA, 500).valorExitoso()

        assertEquals(listOf("wiki-es", "wiki-en", "nominatim", "openai-web"), llamadas)
        assertTrue(resultado.any { it.id == lugarWeb.id })
    }

    @Test
    fun `si el lugar de wikipedia es ya se mostro sigue buscando uno nuevo en la cadena`() = runTest {
        val llamadas = mutableListOf<String>()
        val wikiEs = FuenteEslabonFalsa("wiki-es", Resultado.Exito(listOf(lugarWikiEs)), llamadas)
        val wikiEn = FuenteEslabonFalsa("wiki-en", Resultado.Exito(listOf(lugarWikiEn)), llamadas)
        val cadena = CadenaFuentesLugares(
            fuentes = listOf(wikiEs, wikiEn),
            idsRecientes = { setOf(lugarWikiEs.id) },
        )

        val resultado = cadena.buscarCercanos(PLAZA, 200).valorExitoso()

        assertEquals(listOf("wiki-es", "wiki-en"), llamadas)
        assertEquals(listOf(lugarWikiEs, lugarWikiEn), resultado)
    }

    @Test
    fun `el respaldo de municipio solo responde en busqueda manual y nunca en automatica`() = runTest {
        val clienteNominatimCalle = object : ClienteHttp {
            override suspend fun get(url: String, cabeceras: Map<String, String>): RespuestaHttp =
                RespuestaHttp.Exito(
                    """
                    {
                      "osm_type": "way",
                      "osm_id": 777,
                      "lat": "1.2180",
                      "lon": "-77.2760",
                      "category": "highway",
                      "type": "residential",
                      "name": "Calle 20",
                      "address": {
                        "road": "Calle 20",
                        "county": "Pasto",
                        "state": "Nariño",
                        "country": "Colombia"
                      }
                    }
                    """.trimIndent(),
                )

            override suspend fun postJson(url: String, cuerpoJson: String, cabeceras: Map<String, String>) =
                RespuestaHttp.SinConexion
        }
        val nominatim = FuenteNominatim(clienteNominatimCalle, LimitadorFrecuencia(reloj = RelojFalso()))
        val municipio = FuenteMunicipio(nominatim)
        val filtro = FiltroRelevancia()
        val cadena = CadenaFuentesLugares(fuentes = listOf(nominatim), respaldoMunicipio = municipio)

        val enAutomatico = cadena.buscarCercanos(PLAZA, filtro.radioLlegadaMetros).valorExitoso()
        val enManual = cadena.buscarCercanos(PLAZA, RadioBusqueda.CERCANIA_METROS).valorExitoso()

        assertTrue(filtro.alLlegar(enAutomatico).isEmpty())
        assertEquals(listOf("municipio:pasto"), filtro.emblematicos(enManual).map { it.id })
    }

    @Test
    fun `sin internet falla de inmediato sin recorrer el resto de la cadena`() = runTest {
        val llamadas = mutableListOf<String>()
        val wikiEs = FuenteEslabonFalsa("wiki-es", Resultado.Fallo(ErrorDato.SinInternet), llamadas)
        val wikiEn = FuenteEslabonFalsa("wiki-en", Resultado.Exito(listOf(lugarWikiEn)), llamadas)
        val cadena = CadenaFuentesLugares(listOf(wikiEs, wikiEn))

        val resultado = cadena.buscarCercanos(PLAZA, 200)

        assertEquals(Resultado.Fallo(ErrorDato.SinInternet), resultado)
        assertEquals(listOf("wiki-es"), llamadas)
    }

    @Test
    fun `si una fuente esta caida se recupera con la siguiente fuente disponible`() = runTest {
        val llamadas = mutableListOf<String>()
        val wikiEs = FuenteEslabonFalsa("wiki-es", Resultado.Fallo(ErrorDato.FuenteNoDisponible("503")), llamadas)
        val wikiEn = FuenteEslabonFalsa("wiki-en", Resultado.Exito(listOf(lugarWikiEn)), llamadas)
        val cadena = CadenaFuentesLugares(listOf(wikiEs, wikiEn))

        val resultado = cadena.buscarCercanos(PLAZA, 200).valorExitoso()

        assertEquals(listOf("wiki-es", "wiki-en"), llamadas)
        assertEquals(listOf(lugarWikiEn), resultado)
    }

    @Test
    fun `en modo demo resuelve la ruta de Pasto sin red y usa la cadena real en otra ciudad`() = runTest {
        val llamadasRed = mutableListOf<String>()
        val wikiParis = lugar(
            id = "wikipedia-es:7635",
            nombre = "Torre Eiffel",
            coordenada = Coordenada(48.85837, 2.29448),
        )
        val wikiEs = FuenteEslabonFalsa("wiki-es", Resultado.Exito(listOf(wikiParis)), llamadasRed)
        val cadena = CadenaFuentesLugares(
            fuentes = listOf(wikiEs),
            fuenteDemo = FuenteLugaresDemo(),
            modoDemoActivo = { true },
        )

        val enPasto = cadena.buscarCercanos(Coordenada(1.2146113, -77.2782881), 200).valorExitoso()
        val llamadasTrasPasto = llamadasRed.size
        val enParis = cadena.buscarCercanos(Coordenada(48.85837, 2.29448), 200).valorExitoso()

        assertEquals(0, llamadasTrasPasto)
        assertTrue(enPasto.any { it.nombre == "Plaza de Nariño" })
        assertEquals(1, llamadasRed.size)
        assertEquals(listOf(wikiParis), enParis)
    }
}
