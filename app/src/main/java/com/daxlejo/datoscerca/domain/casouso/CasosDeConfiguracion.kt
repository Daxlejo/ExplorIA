package com.daxlejo.datoscerca.domain.casouso

import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Configuracion
import com.daxlejo.datoscerca.domain.puerto.RepositorioConfiguracion
import kotlinx.coroutines.flow.Flow

class ObservarConfiguracion(private val repositorio: RepositorioConfiguracion) {
    operator fun invoke(): Flow<Configuracion> = repositorio.observar()
}

/** Guarda un cambio sobre la configuración vigente, sin pisar cambios hechos en paralelo. */
class GuardarConfiguracion(private val repositorio: RepositorioConfiguracion) {
    suspend operator fun invoke(cambio: (Configuracion) -> Configuracion): Resultado<Unit> =
        repositorio.actualizar(cambio)
}
