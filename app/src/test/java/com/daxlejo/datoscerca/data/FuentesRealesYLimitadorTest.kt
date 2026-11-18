package com.daxlejo.datoscerca.data

import com.daxlejo.datoscerca.data.lugares.FuenteBusquedaWebOpenAi
import com.daxlejo.datoscerca.data.lugares.FuenteMunicipio
import com.daxlejo.datoscerca.data.lugares.FuenteNominatim
import com.daxlejo.datoscerca.data.lugares.FuenteWikipedia
import com.daxlejo.datoscerca.data.lugares.LimitadorFrecuencia
import com.daxlejo.datoscerca.data.red.ClienteHttp
import com.daxlejo.datoscerca.data.red.RespuestaHttp
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Relevancia
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.motor.PromptBuilder
import com.daxlejo.datoscerca.soporte.PLAZA
import com.daxlejo.datoscerca.soporte.RelojFalso
import com.daxlejo.datoscerca.soporte.valorExitoso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class ClienteHttpFalso(
    private val respuestasGet: (String) -> RespuestaHttp = { RespuestaHttp.SinConexion },
    private val respuestasPost: (String, String) -> RespuestaHttp = { _, _ -> RespuestaHttp.SinConexion },
) : ClienteHttp {
    val urlsGet = mutableListOf<String>()
    val llamadasPost = mutableListOf<Pair<String, String>>()

    override suspend fun get(url: String, cabeceras: Map<String, String>): RespuestaHttp {
        urlsGet += url
        return respuestasGet(url)
    }

    override suspend fun postJson(
        url: String,
        cuerpoJson: String,
        cabeceras: Map<String, String>,
    ): RespuestaHttp {
        llamadasPost += url to cuerpoJson
        return respuestasPost(url, cuerpoJson)
    }
}

class FuentesRealesYLimitadorTest {

    @Test
    fun `wikipedia geosearch obtiene lugares cercanos con su resumen y limpia el titulo`() = runTest {
        val cliente = ClienteHttpFalso(
            respuestasGet = { url ->
                when {
                    url.contains("list=geosearch") -> RespuestaHttp.Exito(
                        """
                        {
                          "query": {
                            "geosearch": [
                              {
                                "pageid": 9855193,
                                "title": "Plaza de Nariño (Pasto)",
                                "lat": 1.214583,
                                "lon": -77.278305,
                                "dist": 12.0
                              },
                              {
                                "pageid": 111,
                                "title": "Anexo:Monumentos de Nariño",
                                "lat": 1.214600,
                                "lon": -77.278200,
                                "dist": 5.0
                              }
                            ]
                          }
                        }
                        """.trimIndent(),
                    )
                    url.contains("prop=extracts") -> RespuestaHttp.Exito(
                        """
                        {
                          "query": {
                            "pages": {
                              "9855193": {
                                "pageid": 9855193,
                                "title": "Plaza de Nariño (Pasto)",
                                "extract": "La Plaza de Nariño es la plaza principal de Pasto."
                              }
                            }
                          }
                        }
                        """.trimIndent(),
                    )
                    else -> RespuestaHttp.SinConexion
                }
            },
        )
        val fuente = FuenteWikipedia(idioma = "es", clienteHttp = cliente)

        val lugares = fuente.buscarCercanos(PLAZA, 200).valorExitoso()

        assertEquals(1, lugares.size)
        val unico = lugares.single()
        assertEquals("wikipedia-es:9855193", unico.id)
        assertEquals("Plaza de Nariño", unico.nombre)
        assertEquals("La Plaza de Nariño es la plaza principal de Pasto.", unico.contexto)
        assertEquals(Relevancia.ARTICULO_WIKIPEDIA, unico.relevancia)
    }

    @Test
    fun `wikipedia mapea errores de red y servidor a errores del dominio`() = runTest {
        val sinRed = FuenteWikipedia("es", ClienteHttpFalso(respuestasGet = { RespuestaHttp.SinConexion }))
        val caido = FuenteWikipedia("es", ClienteHttpFalso(respuestasGet = { RespuestaHttp.ErrorServidor(503, "Unavailable") }))
        val corrupto = FuenteWikipedia("es", ClienteHttpFalso(respuestasGet = { RespuestaHttp.Exito("{malformado") }))

        assertEquals(Resultado.Fallo(ErrorDato.SinInternet), sinRed.buscarCercanos(PLAZA, 200))
        assertTrue((caido.buscarCercanos(PLAZA, 200) as Resultado.Fallo).error is ErrorDato.FuenteNoDisponible)
        assertTrue((corrupto.buscarCercanos(PLAZA, 200) as Resultado.Fallo).error is ErrorDato.FuenteNoDisponible)
    }

    @Test
    fun `nominatim clasifica museos e iglesias como relevantes y calles o tiendas como ninguna`() = runTest {
        var jsonActual = ""
        val cliente = ClienteHttpFalso(respuestasGet = { RespuestaHttp.Exito(jsonActual) })
        val nominatim = FuenteNominatim(
            clienteHttp = cliente,
            limitador = LimitadorFrecuencia(intervaloMinimoMs = 0, reloj = RelojFalso()),
        )

        jsonActual = """
            {"osm_type":"way","osm_id":115238095,"lat":"1.21464","lon":"-77.27825","category":"tourism","type":"museum","name":"Museo Taminango"}
        """.trimIndent()
        val museo = nominatim.buscarCercanos(PLAZA, 200).valorExitoso().single()

        jsonActual = """
            {"osm_type":"way","osm_id":999,"lat":"1.21464","lon":"-77.27825","category":"shop","type":"convenience","name":"Tienda La Esquina"}
        """.trimIndent()
        val tienda = nominatim.buscarCercanos(PLAZA, 200).valorExitoso().single()

        assertEquals(Relevancia.ETIQUETA_OSM_PATRIMONIAL, museo.relevancia)
        assertEquals(Relevancia.NINGUNA, tienda.relevancia)
    }

    @Test
    fun `el limitador de frecuencia espera hasta cumplir un segundo entre peticiones`() = runTest {
        val reloj = RelojFalso(ahora = 5_000L)
        val pausas = mutableListOf<Long>()
        val limitador = LimitadorFrecuencia(
            intervaloMinimoMs = 1_000L,
            reloj = reloj,
            pausar = { espera ->
                pausas += espera
                reloj.ahora += espera
            },
        )

        limitador.ejecutar { }
        reloj.ahora += 350L
        limitador.ejecutar { }
        reloj.ahora += 1_200L
        limitador.ejecutar { }

        assertEquals(listOf(650L), pausas)
    }

    @Test
    fun `busqueda web de openai no hace llamadas cuando esta desactivada`() = runTest {
        val cliente = ClienteHttpFalso()
        val fuente = FuenteBusquedaWebOpenAi(
            clienteHttp = cliente,
            apiKey = "clave-prueba",
            modelo = "gpt-6-luna",
            habilitada = false,
        )

        val resultado = fuente.buscarCercanos(PLAZA, 500).valorExitoso()

        assertTrue(resultado.isEmpty())
        assertTrue(cliente.llamadasPost.isEmpty())
    }

    @Test
    fun `busqueda web de openai interpreta el lugar cuando esta activada`() = runTest {
        val cliente = ClienteHttpFalso(
            respuestasPost = { _, _ ->
                RespuestaHttp.Exito(
                    """
                    {
                      "output": [
                        {
                          "content": [
                            {
                              "text": "LUGAR: Santuario de Las Lajas\nTemplo neogótico construido sobre el cañón del río Guáitara."
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                )
            },
        )
        val fuente = FuenteBusquedaWebOpenAi(
            clienteHttp = cliente,
            apiKey = "clave-prueba",
            modelo = "gpt-6-luna",
            habilitada = true,
        )

        val lugares = fuente.buscarCercanos(PLAZA, 500).valorExitoso()

        assertEquals(1, lugares.size)
        assertEquals("Santuario de Las Lajas", lugares.single().nombre)
        assertTrue(lugares.single().contexto.contains("cañón del río Guáitara"))
        assertEquals(1, cliente.llamadasPost.size)
    }

    @Test
    fun `wikipedia pide extracto completo sin exintro cuando el resumen inicial es corto y selecciona la seccion del enfoque`() = runTest {
        var enfoqueActual: TipoDato? = TipoDato.GASTRONOMICO
        val cliente = ClienteHttpFalso(
            respuestasGet = { url ->
                when {
                    url.contains("list=geosearch") -> RespuestaHttp.Exito(
                        """
                        {
                          "query": {
                            "geosearch": [
                              {"pageid": 501, "title": "Ipiales", "lat": 1.21464, "lon": -77.27825, "dist": 10.0}
                            ]
                          }
                        }
                        """.trimIndent(),
                    )
                    url.contains("prop=extracts") && url.contains("exintro=1") -> RespuestaHttp.Exito(
                        """
                        {
                          "query": {
                            "pages": {
                              "501": {
                                "pageid": 501,
                                "title": "Ipiales",
                                "extract": "Ipiales es un municipio fronterizo del departamento de Nariño."
                              }
                            }
                          }
                        }
                        """.trimIndent(),
                    )
                    url.contains("prop=extracts") && !url.contains("exintro=1") -> RespuestaHttp.Exito(
                        """
                        {
                          "query": {
                            "pages": {
                              "501": {
                                "pageid": 501,
                                "title": "Ipiales",
                                "extract": "Ipiales es un municipio fronterizo del departamento de Nariño.\n== Geografía ==\nSe ubica a 2898 metros sobre el nivel del mar.\n== Historia ==\nFue fundado en 1585 por fray Andrés Moreno de Zúñiga en tierras del cacique Ipial.\n== Gastronomía ==\nDestacan el cuy asado a la brasa, los llapingachos de papa y el hervido de mora."
                              }
                            }
                          }
                        }
                        """.trimIndent(),
                    )
                    else -> RespuestaHttp.SinConexion
                }
            },
        )
        val fuente = FuenteWikipedia(
            idioma = "es",
            clienteHttp = cliente,
            enfoquePreferido = { enfoqueActual },
        )

        val gastronomico = fuente.buscarCercanos(PLAZA, 250).valorExitoso().single()
        enfoqueActual = TipoDato.HISTORICO
        val historico = fuente.buscarCercanos(PLAZA, 250).valorExitoso().single()

        assertTrue(cliente.urlsGet.any { it.contains("prop=extracts") && !it.contains("exintro=1") })
        assertTrue(gastronomico.contexto.contains("cuy asado a la brasa"))
        assertFalse(gastronomico.contexto.contains("2898 metros"))
        assertTrue(
            gastronomico.contexto.indexOf("cuy asado") <
                gastronomico.contexto.indexOf("1585"),
        )
        assertTrue(
            historico.contexto.indexOf("1585") <
                historico.contexto.indexOf("cuy asado"),
        )
    }

    @Test
    fun `fuente municipio obtiene secciones de historia o cultura recorta a 1200 chars sin partir palabras y cae al resumen inicial si no hay secciones`() = runTest {
        val historiaLarga = "Popayán fue fundada el 13 de enero de 1537 por Sebastián de Belalcázar. " +
            "palabraHistorica ".repeat(90)
        var extractoCompletoActual =
            "Popayán es un municipio colombiano, capital del departamento del Cauca.\n" +
                "== Demografía ==\nCuenta con más de trescientos mil habitantes.\n" +
                "== Historia ==\n=== Fundación ===\n$historiaLarga\n" +
                "== Cultura y patrimonio ==\nSus procesiones de Semana Santa son Patrimonio de la Humanidad."

        val cliente = ClienteHttpFalso(
            respuestasGet = { url ->
                when {
                    url.contains("nominatim.openstreetmap.org/reverse") -> RespuestaHttp.Exito(
                        """
                        {
                          "osm_type": "node",
                          "osm_id": 777,
                          "lat": "1.21464",
                          "lon": "-77.27825",
                          "address": {
                            "city": "Popayán",
                            "state": "Cauca",
                            "country": "Colombia"
                          }
                        }
                        """.trimIndent(),
                    )
                    url.contains("list=search") -> RespuestaHttp.Exito(
                        """
                        {
                          "query": {
                            "search": [
                              {"pageid": 888, "title": "Departamento del Cauca"},
                              {"pageid": 999, "title": "Popayán"}
                            ]
                          }
                        }
                        """.trimIndent(),
                    )
                    url.contains("prop=extracts") && !url.contains("exintro=1") -> RespuestaHttp.Exito(
                        """
                        {
                          "query": {
                            "pages": {
                              "999": {
                                "pageid": 999,
                                "title": "Popayán",
                                "extract": ${FuenteWikipedia.jsonPorDefecto.encodeToString(kotlinx.serialization.serializer(), extractoCompletoActual)}
                              }
                            }
                          }
                        }
                        """.trimIndent(),
                    )
                    else -> RespuestaHttp.SinConexion
                }
            },
        )
        val nominatim = FuenteNominatim(
            clienteHttp = cliente,
            limitador = LimitadorFrecuencia(intervaloMinimoMs = 0, reloj = RelojFalso()),
        )
        val wiki = FuenteWikipedia(
            idioma = "es",
            clienteHttp = cliente,
            enfoquePreferido = { TipoDato.HISTORICO },
        )
        val fuenteMunicipio = FuenteMunicipio(fuenteNominatim = nominatim, fuenteWikipediaEs = wiki)

        val conSeccion = fuenteMunicipio.buscarCercanos(PLAZA, 500).valorExitoso().single()

        assertEquals("municipio:popayan", conSeccion.id)
        assertTrue(conSeccion.contexto.contains("13 de enero de 1537"))
        assertFalse(conSeccion.contexto.contains("trescientos mil habitantes"))
        assertTrue(conSeccion.contexto.length <= PromptBuilder.MAX_CONTEXTO_CHARS)
        assertTrue(conSeccion.contexto.endsWith("palabraHistorica"))

        extractoCompletoActual = "Popayán es la capital histórica del departamento del Cauca en el suroccidente colombiano."
        val sinSecciones = fuenteMunicipio.buscarCercanos(PLAZA, 500).valorExitoso().single()
        assertTrue(sinSecciones.contexto.contains("capital histórica del departamento del Cauca"))
    }
}
