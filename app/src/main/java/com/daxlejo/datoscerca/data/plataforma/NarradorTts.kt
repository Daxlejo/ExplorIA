package com.daxlejo.datoscerca.data.plataforma

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.daxlejo.datoscerca.domain.error.ErrorDato
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.puerto.Narrador
import java.util.Locale

/** Lee los datos en voz alta en español. Si el motor de voz no está listo, lo informa como fallo. */
class NarradorTts(context: Context) : Narrador {

    private enum class Estado { INICIANDO, LISTO, NO_DISPONIBLE }

    @Volatile
    private var estado = Estado.INICIANDO

    // Texto pedido mientras el motor de voz arrancaba.
    @Volatile
    private var pendiente: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { codigo -> alIniciar(codigo) }

    private fun alIniciar(codigo: Int) {
        estado = if (codigo == TextToSpeech.SUCCESS && configurarEspanol()) Estado.LISTO else Estado.NO_DISPONIBLE
        if (estado == Estado.NO_DISPONIBLE) Log.w(ETIQUETA, "Voz en español no disponible (código $codigo)")
        pendiente?.let { texto -> if (estado == Estado.LISTO) hablar(texto) }
        pendiente = null
    }

    private fun configurarEspanol(): Boolean =
        IDIOMAS.any { idioma -> tts.setLanguage(idioma) >= TextToSpeech.LANG_AVAILABLE }

    override fun hablar(texto: String): Resultado<Unit> {
        if (estado == Estado.INICIANDO) {
            pendiente = texto
            return Resultado.Exito(Unit)
        }
        if (estado != Estado.LISTO) {
            Log.w(ETIQUETA, "No se pudo leer el dato: voz en estado $estado")
            return Resultado.Fallo(ErrorDato.VozNoDisponible)
        }
        val codigo = tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, ID_LECTURA)
        return if (codigo == TextToSpeech.SUCCESS) {
            Resultado.Exito(Unit)
        } else {
            Log.w(ETIQUETA, "El motor de voz rechazó la lectura")
            Resultado.Fallo(ErrorDato.VozNoDisponible)
        }
    }

    override fun detener() {
        pendiente = null
        if (estado == Estado.LISTO) tts.stop()
    }

    private companion object {
        const val ETIQUETA = "NarradorTts"
        const val ID_LECTURA = "dato"
        val IDIOMAS = listOf(Locale.forLanguageTag("es-CO"), Locale.forLanguageTag("es-US"), Locale.forLanguageTag("es"))
    }
}
