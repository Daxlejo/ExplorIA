# ExplorIA — Datos con IA sobre los lugares que tienes cerca (Wear OS)

**ExplorIA** es una aplicación *standalone* para relojes inteligentes **Wear OS** que acompaña a la persona mientras camina: detecta cuándo pasa cerca de un lugar emblemático (una plaza, una iglesia, un museo, un teatro, un monumento) y le cuenta un dato breve sobre él, generado con inteligencia artificial a partir de información real.

Funciona **en cualquier lugar del mundo**, no solo en Pasto, y permite elegir el tipo de dato (interesante, histórico, cómico, curioso o gastronómico) y la intensidad con la que aparece.

> **Proyecto de la materia Diseño de Software.** Los dos ejes del trabajo son la arquitectura (Clean Architecture + MVVM + patrones de diseño explícitos, sin código espagueti) y el diseño visual (Compose for Wear OS Material 3 con un design system propio, pensado para pantallas redondas).

| | |
|---|---|
| **Plataforma** | Wear OS (Android 11+, `minSdk 30`) |
| **Lenguaje** | Kotlin 2.4 |
| **Interfaz** | Jetpack Compose for Wear OS, Material 3 |
| **IA** | OpenAI Responses API |
| **Pruebas** | 122 tests unitarios, Android Lint sin errores |

