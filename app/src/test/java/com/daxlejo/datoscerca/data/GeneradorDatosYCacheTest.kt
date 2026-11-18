package com.daxlejo.datoscerca.data

import com.daxlejo.datoscerca.data.demo.RutaDemoPasto
import com.daxlejo.datoscerca.data.ia.CacheDatos
import com.daxlejo.datoscerca.data.ia.GeneradorDatosConCache
import com.daxlejo.datoscerca.data.ia.GeneradorDatosOpenAi
import com.daxlejo.datoscerca.data.ia.GuardiaSalidaIa
import com.daxlejo.datoscerca.data.red.ClienteHttp
import com.daxlejo.datoscerca.data.red.RespuestaHttp
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.motor.FormatoDato
import com.daxlejo.datoscerca.domain.motor.PromptBuilder
import com.daxlejo.datoscerca.domain.puerto.SolicitudGeneracion
import com.daxlejo.datoscerca.soporte.GeneradorFalso
import com.daxlejo.datoscerca.soporte.lugar
import com.daxlejo.datoscerca.soporte.valorExitoso
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneradorDatosYCacheTest {

    private fun solicitud(
        idLugar: String = "wikipedia-es:9855193",
        tipo: TipoDato = TipoDato.HISTORICO,
        intensidad: Intensidad = Intensidad.NORMAL,
        excluidos: List<String> = emptyList(),
    ): SolicitudGeneracion {
        val sitio = lugar(id = idLugar, nombre = "Plaza de Nariño", contexto = "Plaza central de Pasto.")
        val formato = FormatoDato("exactamente dos frases", maxTokensSalida = 110)
        val prompt = PromptBuilder().lugar(sitio).tipo(tipo).formato(formato).evitar(excluidos).build()
        return SolicitudGeneracion(sitio, tipo, intensidad, prompt, textosExcluidos = excluidos)
    }

    @Test
    fun `la cache evita llamar a la IA cuando el mismo lugar tipo e intensidad ya existe`() = runTest {
        val delegado = GeneradorFalso(Resultado.Exito("Dato desde la IA."))
        val cache = CacheDatos()
        val generador = GeneradorDatosConCache(delegado, cache)
        val peticion = solicitud()

        val primero = generador.generar(peticion).valorExitoso()
        val segundo = generador.generar(peticion).valorExitoso()

        assertEquals("Dato desde la IA.", primero)
        assertEquals("Dato desde la IA.", segundo)
        assertEquals(1, delegado.solicitudes.size)
    }

    @Test
    fun `cambiar el tipo o la intensidad genera una clave distinta y consulta al delegado`() = runTest {
        val delegado = GeneradorFalso(Resultado.Exito("Dato nuevo."))
        val generador = GeneradorDatosConCache(delegado, CacheDatos())

        generador.generar(solicitud(tipo = TipoDato.HISTORICO, intensidad = Intensidad.SUAVE))
        generador.generar(solicitud(tipo = TipoDato.COMICO, intensidad = Intensidad.SUAVE))
        generador.generar(solicitud(tipo = TipoDato.HISTORICO, intensidad = Intensidad.INTENSO))

        assertEquals(3, delegado.solicitudes.size)
    }

    @Test
    fun `un fallo de la IA no se guarda en cache y permite reintentar`() = runTest {
        val delegado = GeneradorFalso(Resultado.Fallo(ErrorDato.ErrorIa("timeout")))
        val generador = GeneradorDatosConCache(delegado, CacheDatos())
        val peticion = solicitud()

        val fallo = generador.generar(peticion)
        delegado.respuesta = Resultado.Exito("Recuperado.")
        val exito = generador.generar(peticion)

        assertTrue(fallo is Resultado.Fallo)
        assertEquals("Recuperado.", exito.valorExitoso())
        assertEquals(2, delegado.solicitudes.size)
    }

    @Test
    fun `la cache persiste en archivo y sobrevive a una nueva instancia`() = runTest {
        val archivo = Files.createTempFile("cache-datos-test", ".json").toFile().apply { deleteOnExit() }
        val primera = CacheDatos(archivo)
        primera.guardar("lugar|HISTORICO|NORMAL", "Texto persistido.")

        val segunda = CacheDatos(archivo)

        assertEquals("Texto persistido.", segunda.obtener("lugar|HISTORICO|NORMAL"))
    }

    @Test
    fun `precargar el modo demo llena todas las paradas para funcionar sin internet`() = runTest {
        val delegado = GeneradorFalso(Resultado.Fallo(ErrorDato.SinInternet))
        val cache = CacheDatos()
        RutaDemoPasto.precargarEn(cache)
        val generador = GeneradorDatosConCache(delegado, cache)

        val resultado = generador.generar(
            solicitud(
                idLugar = "wikipedia-es:9855193",
                tipo = TipoDato.GASTRONOMICO,
                intensidad = Intensidad.INTENSO,
            ),
        )

        assertTrue(resultado is Resultado.Exito)
        assertTrue(resultado.valorExitoso().contains("hervidos"))
        assertTrue(delegado.solicitudes.isEmpty())
    }

    @Test
    fun `generador openai envia prompt max_output_tokens y reasoning none en modelos gpt-6`() = runTest {
        var cuerpoRecibido = ""
        val cliente = object : ClienteHttp {
            override suspend fun get(url: String, cabeceras: Map<String, String>) = RespuestaHttp.SinConexion
            override suspend fun postJson(
                url: String,
                cuerpoJson: String,
                cabeceras: Map<String, String>,
            ): RespuestaHttp {
                cuerpoRecibido = cuerpoJson
                return RespuestaHttp.Exito(
                    """
                    {
                      "status": "completed",
                      "output": [
                        {
                          "type": "message",
                          "content": [
                            {
                              "type": "output_text",
                              "text": "La Plaza de Nariño es el corazón histórico de Pasto."
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                )
            }
        }
        val generador = GeneradorDatosOpenAi(cliente, apiKey = "clave-test", modelo = "gpt-6-luna")

        val texto = generador.generar(solicitud()).valorExitoso()

        assertEquals("La Plaza de Nariño es el corazón histórico de Pasto.", texto)
        assertTrue(cuerpoRecibido.contains("\"model\":\"gpt-6-luna\""))
        assertTrue(cuerpoRecibido.contains("\"max_output_tokens\":110"))
        assertTrue(cuerpoRecibido.contains("\"effort\":\"none\""))
    }

    @Test
    fun `generador openai omite reasoning en modelos gpt-4o y mapea errores correctamente`() = runTest {
        var cuerpoGpt4 = ""
        val clienteGpt4 = object : ClienteHttp {
            override suspend fun get(url: String, cabeceras: Map<String, String>) = RespuestaHttp.SinConexion
            override suspend fun postJson(url: String, cuerpoJson: String, cabeceras: Map<String, String>): RespuestaHttp {
                cuerpoGpt4 = cuerpoJson
                return RespuestaHttp.Exito("""{"output_text":"Dato breve."}""")
            }
        }
        val genGpt4 = GeneradorDatosOpenAi(clienteGpt4, apiKey = "clave", modelo = "gpt-4o-mini")
        assertEquals("Dato breve.", genGpt4.generar(solicitud()).valorExitoso())
        assertFalse(cuerpoGpt4.contains("reasoning"))

        val sinRed = GeneradorDatosOpenAi(
            clienteHttp = object : ClienteHttp {
                override suspend fun get(url: String, cabeceras: Map<String, String>) = RespuestaHttp.SinConexion
                override suspend fun postJson(url: String, cuerpoJson: String, cabeceras: Map<String, String>) =
                    RespuestaHttp.SinConexion
            },
            apiKey = "clave",
            modelo = "gpt-6-luna",
        )
        val errorHttp = GeneradorDatosOpenAi(
            clienteHttp = object : ClienteHttp {
                override suspend fun get(url: String, cabeceras: Map<String, String>) = RespuestaHttp.SinConexion
                override suspend fun postJson(url: String, cuerpoJson: String, cabeceras: Map<String, String>) =
                    RespuestaHttp.ErrorServidor(429, "Quota exceeded")
            },
            apiKey = "clave",
            modelo = "gpt-6-luna",
        )
        val sinClave = GeneradorDatosOpenAi(clienteGpt4, apiKey = "", modelo = "gpt-6-luna")

        assertEquals(Resultado.Fallo(ErrorDato.SinInternet), sinRed.generar(solicitud()))
        assertTrue((errorHttp.generar(solicitud()) as Resultado.Fallo).error is ErrorDato.ErrorIa)
        assertTrue((sinClave.generar(solicitud()) as Resultado.Fallo).error is ErrorDato.ErrorIa)
    }

    @Test
    fun `guardia de salida rechaza metacomentarios reintenta una vez como interesante y nunca guarda el texto rechazado`() = runTest {
        val cuerposEnviados = mutableListOf<String>()
        val respuestas = ArrayDeque(
            listOf(
                """{"output_text":"El contexto no menciona un hecho histórico vinculado a Ipiales."}""",
                """{"output_text":"Ipiales es un municipio fronterizo famoso por el Santuario de Las Lajas."}""",
            ),
        )
        val cliente = object : ClienteHttp {
            override suspend fun get(url: String, cabeceras: Map<String, String>) = RespuestaHttp.SinConexion
            override suspend fun postJson(url: String, cuerpoJson: String, cabeceras: Map<String, String>): RespuestaHttp {
                cuerposEnviados += cuerpoJson
                return RespuestaHttp.Exito(respuestas.removeFirst())
            }
        }
        val cache = CacheDatos()
        val generador = GeneradorDatosConCache(
            delegado = GeneradorDatosOpenAi(cliente, apiKey = "clave", modelo = "gpt-6-luna"),
            cache = cache,
        )
        val peticion = solicitud(tipo = TipoDato.HISTORICO)

        val resultado = generador.generar(peticion).valorExitoso()

        assertEquals("Ipiales es un municipio fronterizo famoso por el Santuario de Las Lajas.", resultado)
        assertEquals(2, cuerposEnviados.size)
        assertTrue(cuerposEnviados[0].contains("hecho histórico"))
        assertTrue(cuerposEnviados[1].contains("lo más interesante y relevante del lugar"))
        assertEquals(resultado, cache.obtener(peticion.claveCache))
    }

    @Test
    fun `si el reintento tambien contiene frases prohibidas devuelve ErrorIa y la cache queda vacia`() = runTest {
        var llamadas = 0
        val cliente = object : ClienteHttp {
            override suspend fun get(url: String, cabeceras: Map<String, String>) = RespuestaHttp.SinConexion
            override suspend fun postJson(url: String, cuerpoJson: String, cabeceras: Map<String, String>): RespuestaHttp {
                llamadas++
                val texto = if (llamadas == 1) {
                    "No se menciona ninguna fecha histórica en el texto."
                } else {
                    "No hay información ni aparece detalle alguno."
                }
                return RespuestaHttp.Exito("""{"output_text":"$texto"}""")
            }
        }
        val cache = CacheDatos()
        val generador = GeneradorDatosConCache(
            delegado = GeneradorDatosOpenAi(cliente, apiKey = "clave", modelo = "gpt-6-luna"),
            cache = cache,
        )
        val peticion = solicitud(tipo = TipoDato.HISTORICO)

        val resultado = generador.generar(peticion)

        assertTrue(resultado is Resultado.Fallo)
        assertTrue((resultado as Resultado.Fallo).error is ErrorDato.ErrorIa)
        assertEquals(2, llamadas)
        assertEquals(0, cache.tamano())
        assertTrue(GuardiaSalidaIa.contieneMetacomentarios("El contexto no indica fechas."))
        assertTrue(GuardiaSalidaIa.contieneMetacomentarios("No aparece registrado."))
        assertFalse(GuardiaSalidaIa.contieneMetacomentarios("Fue fundada en 1537 por Sebastián de Belalcázar."))
    }

    @Test
    fun `permite hasta 3 variantes por clave sirve las no mostradas y no supera 3 llamadas a la IA`() = runTest {
        var contador = 0
        val delegado = object : com.daxlejo.datoscerca.domain.puerto.GeneradorDatos {
            var llamadas = 0
            override suspend fun generar(solicitud: SolicitudGeneracion): Resultado<String> {
                llamadas++
                contador++
                return Resultado.Exito("Variante histórica número $contador.")
            }
        }
        val cache = CacheDatos()
        val generador = GeneradorDatosConCache(delegado, cache)

        val v1 = generador.generar(solicitud(excluidos = emptyList())).valorExitoso()
        val v2 = generador.generar(solicitud(excluidos = listOf(v1))).valorExitoso()
        val v3 = generador.generar(solicitud(excluidos = listOf(v2, v1))).valorExitoso()
        // Al tener ya 3 variantes en caché, una cuarta petición no llama a OpenAI y rota a la menos reciente (v1).
        val v4 = generador.generar(solicitud(excluidos = listOf(v3, v2, v1))).valorExitoso()
        // Si solo se excluye v1, sirve v2 desde caché sin llamar a la IA.
        val desdeCache = generador.generar(solicitud(excluidos = listOf(v1))).valorExitoso()

        assertEquals("Variante histórica número 1.", v1)
        assertEquals("Variante histórica número 2.", v2)
        assertEquals("Variante histórica número 3.", v3)
        assertEquals(v1, v4)
        assertEquals(v2, desdeCache)
        assertEquals(3, delegado.llamadas)
        assertEquals(3, cache.obtenerVariantes(solicitud().claveCache).size)
    }

    @Test
    fun `cache datos lee el formato antiguo de un solo string por clave de forma retrocompatible`() = runTest {
        val archivo = Files.createTempFile("cache-formato-antiguo", ".json").toFile().apply { deleteOnExit() }
        archivo.writeText(
            """
            {
              "wikipedia-es:9855193|HISTORICO|NORMAL": "La Plaza de Nariño fue la Plaza Mayor colonial."
            }
            """.trimIndent(),
        )

        val cache = CacheDatos(archivo)

        assertEquals(
            "La Plaza de Nariño fue la Plaza Mayor colonial.",
            cache.obtener("wikipedia-es:9855193|HISTORICO|NORMAL"),
        )
        assertEquals(
            listOf("La Plaza de Nariño fue la Plaza Mayor colonial."),
            cache.obtenerVariantes("wikipedia-es:9855193|HISTORICO|NORMAL"),
        )
    }

    @Test
    fun `al cargar desde disco descarta entradas con metacomentarios de Ipiales y Popayan sin borrar las buenas`() = runTest {
        val archivo = Files.createTempFile("cache-limpieza", ".json").toFile().apply { deleteOnExit() }
        archivo.writeText(
            """
            {
              "municipio:ipiales|HISTORICO|NORMAL": "El contexto no menciona un hecho histórico ni fechas o personajes vinculados a Ipiales. Sí destaca el Santuario de Las Lajas.",
              "municipio:popayan|HISTORICO|NORMAL": [
                "Popayán es la capital y la ciudad más poblada del departamento del Cauca. El contexto no menciona hechos históricos, fechas ni personajes vinculados con la ciudad."
              ],
              "wikipedia-es:9855193|HISTORICO|NORMAL": "Durante la Colonia fue la Plaza Mayor de la ciudad y escenario de mercados y cabildos.",
              "municipio:pasto|HISTORICO|NORMAL": [
                "No se menciona la fecha exacta en el texto.",
                "San Juan de Pasto fue fundada en 1537 en el valle de Atriz."
              ]
            }
            """.trimIndent(),
        )

        val cache = CacheDatos(archivo)

        assertEquals(null, cache.obtener("municipio:ipiales|HISTORICO|NORMAL"))
        assertEquals(null, cache.obtener("municipio:popayan|HISTORICO|NORMAL"))
        assertEquals(
            "Durante la Colonia fue la Plaza Mayor de la ciudad y escenario de mercados y cabildos.",
            cache.obtener("wikipedia-es:9855193|HISTORICO|NORMAL"),
        )
        assertEquals(
            listOf("San Juan de Pasto fue fundada en 1537 en el valle de Atriz."),
            cache.obtenerVariantes("municipio:pasto|HISTORICO|NORMAL"),
        )
        assertFalse(archivo.readText().contains("El contexto no menciona"))
    }
}
