package com.daxlejo.datoscerca.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.tooling.preview.devices.WearDevices
import com.daxlejo.datoscerca.ui.theme.DatosCercaTheme

/** Las tres condiciones en que se revisa cada pantalla: redonda grande, redonda pequeña y fuente grande. */
@Preview(name = "Redonda grande", device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(name = "Redonda pequeña", device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Preview(name = "Fuente grande", device = WearDevices.SMALL_ROUND, showSystemUi = true, fontScale = 1.24f)
annotation class PreviewsWear

@Composable
fun MarcoPreview(contenido: @Composable () -> Unit) {
    DatosCercaTheme { AppScaffold { contenido() } }
}
