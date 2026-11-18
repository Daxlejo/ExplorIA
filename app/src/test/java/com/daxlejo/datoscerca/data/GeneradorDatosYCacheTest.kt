package com.daxlejo.datoscerca.data

import com.daxlejo.datoscerca.data.demo.RutaDemoPasto
import com.daxlejo.datoscerca.data.ia.CacheDatos
import com.daxlejo.datoscerca.data.ia.GeneradorDatosConCache
import com.daxlejo.datoscerca.data.ia.GeneradorDatosOpenAi
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
    ): SolicitudGeneracion {
        val sitio = lugar(id = idLugar, nombre = "Plaza de Nariño", contexto = "Plaza central de Pasto.")
        val formato = FormatoDato("exactamente dos frases", maxTokensSalida = 110)
        val prompt = PromptBuilder().lugar(sitio).tipo(tipo).formato(formato).build()
        return SolicitudGeneracion(sitio, tipo, intensidad, prompt)
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
}
