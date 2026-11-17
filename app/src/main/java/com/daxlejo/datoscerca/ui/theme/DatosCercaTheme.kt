package com.daxlejo.datoscerca.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun DatosCercaTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaDatosCerca,
        shapes = FormasDatosCerca,
        content = contenido,
    )
}
