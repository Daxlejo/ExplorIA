# Guion de la exposición de ExplorIA

Cada bloque indica **qué haces** y **qué dices**. No hace falta memorizarlo palabra por palabra. Duración total: 5 a 6 minutos.

## 0. Presentación (30 s)

**Dices:** «Esta es *ExplorIA*, una app para smartwatch con Wear OS. Mientras caminas, detecta cuándo pasas cerca de un lugar emblemático y te cuenta un dato sobre él, generado con inteligencia artificial: interesante, histórico, curioso, gastronómico o cómico, con la intensidad que elijas. Como no tenemos un reloj físico, la corremos en el emulador oficial de Wear OS y le simulamos el GPS.»

## 1. Ruta por el centro de Pasto (1 min 30 s)

**Haces:** en *Extended Controls → Routes*, **Play** a `ruta-pasto.gpx`.

**Dices:** «Cargamos una caminata por el centro de Pasto con 8 hitos verificados en OpenStreetMap. Al inicio pasamos por una tienda de barrio y no sale nada: la app solo habla de lugares emblemáticos, a menos de 150–250 metros. Una calle, una casa o una tienda nunca disparan un dato.»

Cuando salte un dato: «Vibra y muestra el tipo de dato, el lugar, la distancia y el texto. Puedo escucharlo, marcarlo como favorito o pedir otro.»

## 2. Cualquier lugar del mundo (1 min)

**Haces:** `.\demo\teletransporte.bat bogota` → **Dato ahora**; luego `cartagena` y `paris`.

**Dices:** «La app no está atada a Pasto. En Bogotá encuentra la Catedral Primada; en Cartagena, la Torre del Reloj; en París, la Torre Eiffel, con el dato en español. Funciona en cualquier punto del planeta porque los lugares se buscan en vivo en Wikipedia y OpenStreetMap.»

## 3. Tipo e intensidad (45 s)

**Haces:** Configuración → solo **Cómico**, intensidad **Intenso** → `paris` o `pasto` → **Dato ahora**.

**Dices:** «El usuario decide el tono. Suave da una frase y como máximo un dato por hora; Normal, dos frases y dos por hora; Intenso, un párrafo y hasta cuatro. En Cómico el texto cambia de tono, pero siempre se basa en hechos reales: a la IA le prohibimos inventar.»

## 4. Calle sin lugares y botón manual (30 s)

**Haces:** `.\demo\teletransporte.bat calle` → **Dato ahora**.

**Dices:** «En una calle residencial no sale nada automático, a propósito. Pero si pido un dato con el botón, la app nunca me deja sin respuesta: si no hay hitos cerca, me cuenta algo del municipio.»

## 5. Aquí, donde estamos (30 s)

**Haces:** pega el comando de la sede de la exposición → **Dato ahora**.

## 6. Historial y favoritos (20 s)

**Haces:** Inicio → ícono de reloj → abrir una entrada → estrella → **Solo favoritos**.

**Dices:** «Todo queda en un historial agrupado por día. También sirve para no repetir: un lugar mostrado hace poco no vuelve a salir.»

## 7. Cómo se construyó (1 min 30 s)

- **Lenguaje y plataforma:** Kotlin para Wear OS, con Jetpack Compose y Material 3 para pantallas redondas.
- **Arquitectura:** Clean Architecture + MVVM con flujo de datos unidireccional, en cuatro capas: `domain` (Kotlin puro, sin Android), `data` (GPS, Wikipedia, OpenStreetMap, OpenAI, Room, caché), `ui` (4 pantallas con ViewModel y estado inmutable) y `di` (contenedor manual). «Por eso en la primera fase usamos datos simulados y en la segunda los cambiamos por los reales sin tocar el dominio.»
- **Patrones de diseño:** Chain of Responsibility (fuentes de lugares), Strategy (intensidades y selección de tipo), Decorator (caché y modo demo), Builder (prompt), Repository, Observer (Flow), State y Adapter.
- **Tecnologías:** Fused Location, Wikipedia GeoSearch, OSM Nominatim (1 petición/segundo), OpenAI Responses API, OkHttp, kotlinx.serialization, Room, DataStore y TextToSpeech.
- **Calidad:** 122 tests unitarios en verde y Lint sin errores; unas 6.000 líneas en 76 archivos Kotlin.

