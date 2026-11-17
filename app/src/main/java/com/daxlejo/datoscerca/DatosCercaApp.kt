package com.daxlejo.datoscerca

import android.app.Application
import com.daxlejo.datoscerca.di.AppContainer

class DatosCercaApp : Application() {
    val contenedor: AppContainer by lazy { AppContainer(this) }
}
