package com.daxlejo.datoscerca.data.plataforma

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.daxlejo.datoscerca.domain.puerto.Reloj
import com.daxlejo.datoscerca.domain.puerto.Vibrador

class RelojSistema : Reloj {
    override fun ahoraMs(): Long = System.currentTimeMillis()
}

/** Patrón propio de "dato nuevo": dos toques cortos y uno largo que sube, como una idea que se enciende. */
class VibradorAndroid(context: Context) : Vibrador {
    private val vibrador: Vibrator? = context.getSystemService(Vibrator::class.java)

    override fun vibrarNuevoDato() {
        val disponible = vibrador?.takeIf { it.hasVibrator() }
        if (disponible == null) {
            Log.w(ETIQUETA, "El dispositivo no tiene vibrador")
            return
        }
        disponible.vibrate(VibrationEffect.createWaveform(TIEMPOS_MS, AMPLITUDES, SIN_REPETIR))
    }

    private companion object {
        const val ETIQUETA = "VibradorAndroid"
        const val SIN_REPETIR = -1
        val TIEMPOS_MS = longArrayOf(0, 40, 70, 40, 90, 180)
        val AMPLITUDES = intArrayOf(0, 120, 0, 160, 0, 255)
    }
}
