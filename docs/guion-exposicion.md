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

## 8. Lo más difícil (elige 2 o 3)

1. **El presupuesto de la IA.** Solo había 5 dólares para tres apps. El modelo gastaba los tokens «pensando» y el dato salía vacío; se resolvió desactivando el razonamiento, recortando el contexto, poniendo un tope por intensidad y, sobre todo, con una caché: un dato ya generado no se vuelve a pedir.
2. **Distinguir un lugar emblemático de una calle.** OpenStreetMap devuelve direcciones de todo; se clasifica por etiquetas turísticas o históricas y por la existencia de un artículo en Wikipedia.
3. **Que la IA no invente.** Solo recibe el texto real del lugar y se le prohíbe agregar nombres, fechas o cifras.
4. **Diseñar para una pantalla redonda.** El texto tocaba el borde curvo; se centró, se dejaron márgenes proporcionales y se probó en reloj pequeño y con letra grande.
5. **La demo sin internet.** El Modo demo deja precargada la ruta de Pasto.

## 9. Cierre (15 s)

«Una app de reloj que convierte una caminata en un recorrido guiado, en cualquier lugar del mundo, con IA que se basa en hechos reales y una arquitectura limpia con patrones de diseño. El código está en GitHub. ¿Preguntas?»

## Preguntas probables

| Pregunta | Respuesta corta |
|---|---|
| ¿Por qué el emulador y no un reloj? | No hay reloj físico; el emulador es el oficial de Google y simula GPS, así que la app es la misma. |
| ¿Qué pasa sin internet? | El Modo demo y la caché siguen funcionando; sin caché se muestra un estado de error diseñado, no se cae. |
| ¿Cómo evitan que la IA invente? | Solo recibe el contexto real de Wikipedia/OSM y la instrucción lo prohíbe. |
| ¿Dónde está la API key? | En `local.properties` (no se sube a Git) y se inyecta por `BuildConfig`. |
| ¿Por qué Clean Architecture? | Para separar responsabilidades: se cambiaron los datos simulados por los reales sin tocar la lógica, y el dominio se prueba sin Android. |
| ¿Por qué Kotlin y Compose? | Son el lenguaje y el toolkit oficiales recomendados por Google para Android y Wear OS. |
| ¿Cuánto cuesta un dato? | Una fracción de centavo (20–90 tokens de salida) y una sola vez por lugar, tipo e intensidad. |
| ¿Los datos del Modo demo los genera la IA? | No: son textos fijos para funcionar sin internet; en otras ciudades se generan en vivo. |
