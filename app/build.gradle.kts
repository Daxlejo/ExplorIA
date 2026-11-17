import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Los secretos viven solo en local.properties (ignorado por git).
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use(::load)
}

fun propiedadLocal(nombre: String, porDefecto: String = ""): String =
    propiedadesLocales.getProperty(nombre, porDefecto).trim()

fun comoLiteral(valor: String): String =
    "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.daxlejo.datoscerca"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.daxlejo.datoscerca"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "OPENAI_API_KEY", comoLiteral(propiedadLocal("OPENAI_API_KEY")))
        buildConfigField("String", "OPENAI_MODEL", comoLiteral(propiedadLocal("OPENAI_MODEL")))
        buildConfigField(
            "boolean",
            "OPENAI_WEB_SEARCH",
            propiedadLocal("OPENAI_WEB_SEARCH", "false").toBoolean().toString(),
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.compose.ui.tooling)
    implementation(libs.wear.tooling.preview)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
