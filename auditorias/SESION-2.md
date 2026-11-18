# Auditoría — Sesión 2: Ubicación, lugares e IA reales, pulido final y demo

**Fecha:** 2026-11-17
**Estado propuesto:** Lista para revisión

## 1. Resumen

Se reemplazaron todos los *fakes* de la Sesión 1 por implementaciones reales conectadas en `AppContainer` sin tocar el dominio (`domain` sigue siendo Kotlin puro sin ningún `import android.*`). Se implementaron la ubicación en tiempo real con `FusedLocationProviderClient` y permisos en tiempo de ejecución, la cadena de responsabilidad de fuentes de lugares (Wikipedia `es` → Wikipedia `en` → OpenStreetMap Nominatim a máx. 1 req/s → OpenAI Responses API con búsqueda web si está activa → respaldo de municipio/región exclusivo de "Dato ahora"), el generador real contra OpenAI Responses API (`gpt-6-luna`) decorado con caché en memoria y disco (`CacheDatos`), el interruptor de **Modo demo** que precarga la ruta de Pasto para funcionar sin internet, los activos de demostración (`demo/ruta-pasto.gpx`, `demo/teletransporte.bat` y `demo/teletransporte.sh`), el pulido visual sobre pantallas redondas grandes, pequeñas y con fuente grande, la documentación final y 21 tests unitarios nuevos (122 en total).

## 2. Entregables

| Entregable | Estado | Ruta |
|---|---|---|
| Ubicación real con `FusedLocationProviderClient` + flujo de permisos en UI | Hecho | `app/src/main/java/com/daxlejo/datoscerca/data/ubicacion/ProveedorUbicacionFused.kt`, `ui/MainActivity.kt`, `ui/inicio/InicioPantalla.kt` |
| Cliente HTTP con `OkHttp` y `User-Agent` descriptivo | Hecho | `app/src/main/java/com/daxlejo/datoscerca/data/red/ClienteHttp.kt` |
| Cadena de fuentes (`CadenaFuentesLugares`: Wikipedia `es` → `en` → Nominatim → OpenAI Web Search → Municipio) | Hecho | `app/src/main/java/com/daxlejo/datoscerca/data/lugares/{CadenaFuentesLugares,FuenteWikipedia,FuenteNominatim,FuenteBusquedaWebOpenAi,FuenteMunicipio,LimitadorFrecuencia}.kt` |
| Cliente OpenAI real (`Responses API`) + `CacheDatos` + Decorator `GeneradorDatosConCache` | Hecho | `app/src/main/java/com/daxlejo/datoscerca/data/ia/{GeneradorDatosOpenAi,CacheDatos}.kt` |
| Modo demo sin internet + interruptor en Configuración + precarga de paradas de Pasto | Hecho | `app/src/main/java/com/daxlejo/datoscerca/data/demo/RutaDemoPasto.kt`, `ui/configuracion/Configuracion{Pantalla,ViewModel}.kt` |
| Eliminación de los fakes de producción de la Sesión 1 | Hecho | Eliminado `app/src/main/java/com/daxlejo/datoscerca/data/fake/*` |
| Ruta GPX verificada contra OpenStreetMap (8 hitos del centro de Pasto) | Hecho | `demo/ruta-pasto.gpx` |
| Scripts de teletransporte (`adb emu geo fix` con longitud primero) | Hecho | `demo/teletransporte.bat`, `demo/teletransporte.sh` |
| Tests unitarios de cadena de fuentes, parseo, limitador 1 req/s, caché y mapeo de errores OpenAI | Hecho (122 tests en total, 0 fallidos) | `app/src/test/java/com/daxlejo/datoscerca/data/{CadenaFuentesLugaresTest,FuentesRealesYLimitadorTest,GeneradorDatosYCacheTest}.kt` |
| Pulido visual + capturas antes/después, Large Round, Small Round y fuente grande (`1.24x`) | Hecho (41 capturas de la Sesión 2) | `auditorias/capturas/s2-*.png` |
| Documentación final y guion de demo de 5 minutos | Hecho | `README.md`, `docs/arquitectura.md`, `local.properties.example` |

## 3. Decisiones de arquitectura y diseño

### Patrones aplicados y dónde viven
- **Repository:** `RepositorioHistorial` / `RepositorioConfiguracion` → `RepositorioHistorialRoom` (`data/historial/RepositorioHistorialRoom.kt`), `RepositorioConfiguracionDataStore` (`data/configuracion/RepositorioConfiguracionDataStore.kt`).
- **Chain of Responsibility:** `FuenteLugares` → `CadenaFuentesLugares` (`data/lugares/CadenaFuentesLugares.kt`), que encadena en orden:
  1. `FuenteWikipedia(idioma = "es")` (`data/lugares/FuenteWikipedia.kt`)
  2. `FuenteWikipedia(idioma = "en")` (`data/lugares/FuenteWikipedia.kt`)
  3. `FuenteNominatim` (`data/lugares/FuenteNominatim.kt`, con `LimitadorFrecuencia` a máx. 1 petición/segundo)
  4. `FuenteBusquedaWebOpenAi` (`data/lugares/FuenteBusquedaWebOpenAi.kt`, activa únicamente si `BuildConfig.OPENAI_WEB_SEARCH == true`)
  5. `FuenteMunicipio` (`data/lugares/FuenteMunicipio.kt`, inyectada como `respaldoMunicipio` final y activa solo cuando `radioMetros > 250`, es decir, solo con el botón manual "Dato ahora").
- **Decorator:**
  - `PoliticaDemo` (`domain/motor/PoliticaIntensidad.kt`): envuelve la política de intensidad activa para ignorar el tope por hora en modo demo conservando el largo del nivel.
  - `GeneradorDatosConCache` (`data/ia/CacheDatos.kt`): envuelve a `GeneradorDatosOpenAi` para devolver de inmediato los datos ya presentes en `CacheDatos` (`lugar.id|tipo|intensidad`) sin llamar a OpenAI.
- **Strategy:** `PoliticaIntensidad` (`PoliticaSuave`, `PoliticaNormal`, `PoliticaIntenso`) y `SelectorTipoDato` (`SelectorEnOrden`, `SelectorAleatorio`).
- **Builder:** `PromptBuilder` (`domain/motor/PromptBuilder.kt`), con recorte de contexto en frontera de palabra hasta `1200` caracteres.
- **Observer:** `Flow` / `StateFlow` / `SharedFlow` en `ProveedorUbicacionFused`, `MotorAutomatico`, repositorios y ViewModels.
- **State:** `EstadoMotor`, `EstadoUbicacion` y los `UiState` sellados de las 4 pantallas.
- **Adapter:** `ProveedorUbicacionFused`, `ClienteHttpOkHttp`, `RelojSistema`, `VibradorAndroid`, `NarradorTts` y `EntradaMapper`.

### Decisiones técnicas clave
- **Ahorro de presupuesto en modelos `gpt-5` / `gpt-6` (`gpt-6-luna`):** al verificar contra el endpoint real `POST https://api.openai.com/v1/responses`, se comprobó que `gpt-6-luna` usa por defecto `reasoning.effort = "medium"` y consume los `60` tokens del nivel Suave en tokens internos de razonamiento. En `GeneradorDatosOpenAi` se envía `"reasoning": {"effort": "none"}` y `"store": false` para modelos `gpt-5*`/`gpt-6*` (y se omite `reasoning` en modelos `gpt-4*`), logrando `0` tokens de razonamiento y respuestas en menos de 2 segundos con ~20–90 tokens de salida.
- **Modo demo 100% sin conexión:** cuando el usuario activa **Modo demo** en Configuración, `RutaDemoPasto.precargarEn(cacheDatos)` llena `CacheDatos` con los 8 hitos del recorrido de Pasto en los 5 tipos y 3 intensidades (registrando tanto el ID de Wikipedia como el de Nominatim), y `CadenaFuentesLugares` consulta primero `FuenteLugaresDemo` para las coordenadas de Pasto. Si el usuario se teletransporta a otra ciudad (Bogotá, Cartagena, París) con internet activo, `FuenteLugaresDemo` devuelve lista vacía y la cadena real toma el relevo.
- **Pulido visual en pantallas redondas:**
  - En `TarjetaDato.kt`, `TextoDato` se centró (`TextAlign.Center`) con relleno horizontal adicional (`Espaciado.m`) sumado al margen lateral proporcional (`MARGEN_LATERAL = 0.08f`), evitando que las líneas inferiores toquen el bisel curvo del reloj (`s2-antes-dato-borde.png` → `s2-grande-03-dato-pasto-plaza-narino.png`).
  - En `Indicadores.kt`, las barras inactivas de `IndicadorIntensidad` pasaron de `outlineVariant` a `outline` (`#7C928A`) para superar el contraste mínimo WCAG AA sobre el contenedor del `RadioButton`.
  - En `strings.xml`, la descripción del nivel Suave pasó a `"Hasta 1 por hora, una frase"` para eliminar el truncamiento con puntos suspensivos en `RadioButton` (`s2-antes-configuracion-intensidad.png` → `s2-grande-08-configuracion-intensidad.png`).
  - Todas las constantes de animación y opacidad se movieron al objeto `Animaciones` en `ui/theme/Tokens.kt`, y `GlifoDeEstado` en `InicioPantalla.kt` incorpora transición `Crossfade`.

## 4. Evidencias

### Compilación, tests unitarios y lint
- Comando ejecutado: `./gradlew assembleDebug testDebugUnitTest lint`
- Resultado: **`BUILD SUCCESSFUL`**
- **Tests unitarios:** 18 clases de prueba, **122 tests ejecutados, 122 pasados, 0 fallidos**.
  - 101 tests de dominio e interfaz de la Sesión 1 en verde.
  - 21 tests nuevos de la Sesión 2 en `CadenaFuentesLugaresTest` (8), `FuentesRealesYLimitadorTest` (6) y `GeneradorDatosYCacheTest` (7).
- **Android Lint:** **0 errores, 0 advertencias**.
- **Verificación de secretos:** `git grep "sk-"` solo coincide con el texto explicativo de `PLAN_FASES.md` y `auditorias/SESION-1.md`. `local.properties` está ignorado por Git.

### Prompt crudo enviado a OpenAI (`PromptBuilder`)

**`instructions` (`Prompt.sistema`):**
```text
Eres un guía local que cuenta datos breves sobre lugares. Usa únicamente los hechos del CONTEXTO: no inventes nombres, fechas ni cifras. Si el contexto no alcanza para el enfoque pedido, cuenta lo que sí aparece. Responde siempre en español, en texto plano, sin listas, títulos ni emojis.
```

**`input` (`Prompt.usuario`, ejemplo real enviado para Plaza de Nariño en Pasto, tipo `HISTORICO`, intensidad `NORMAL`, `max_output_tokens = 110`):**
```text
Lugar: Plaza de Nariño
Enfoque: un hecho histórico, con las fechas o personajes que aparezcan en el contexto
Largo: Responde en exactamente dos frases.
CONTEXTO:
La Plaza de Nariño es el punto de referencia central en la ciudad de Pasto, ahí se encontraba el reconocido monumento a Antonio Nariño, razón por la cual lleva este nombre. El 1 de mayo de 2021, en el marco de las protestas en Colombia de 2021 contra la reforma tributaria propuesta por el gobierno de Iván Duque, el monumento de Antonio Nariño fue derribado por manifestantes. Es considerada una zona histórica y turística de la ciudad, ya que a partir de su punto céntrico se despliega las principales calles de la ciudad, además de poder apreciar las diferentes obras que están en el corazón de la ciudad.
```

### Datos reales generados por OpenAI en cada ciudad probada

1. **Pasto — Plaza de Nariño** (`wikipedia-es:9855193`, tipo `INTERESANTE`, intensidad `NORMAL`):
   > «La Plaza de Nariño es el punto de referencia central de Pasto, desde donde se despliegan las principales calles y se aprecian obras destacadas del corazón de la ciudad. Debe su nombre al monumento dedicado a Antonio Nariño, derribado el 1 de mayo de 2021 durante protestas contra la reforma tributaria propuesta por Iván Duque.»
2. **Pasto — Templo de San Juan Bautista** (`wikipedia-es:9856229`, tipo `HISTORICO`, intensidad `NORMAL`):
   > «El templo de San Juan Bautista fue la primera catedral de Pasto y conserva la imagen de San Juan Bautista utilizada por Sebastián de Belalcázar para fundar la ciudad en 1537. También guarda los restos del caudillo pastuso de la Independencia Agustín Agualongo.»
3. **Pasto — St. Francis Xavier College / Templo de Cristo Rey** (eslabón `Wikipedia EN`, `wikipedia-en:51548567`, tipo `CURIOSO`, intensidad `NORMAL`):
   > «Un detalle curioso es que, aunque el colegio fue fundado por la Compañía de Jesús en 1925, su sección desde jardín de infancia hasta séptimo grado abrió en 1956 bajo el cuidado de las Hermanas de San José. Hoy ofrece educación preescolar, primaria y secundaria en San Juan de Pasto.»
4. **Pasto — Calle residencial sin hitos ("Dato ahora" → eslabón final `FuenteMunicipio`)** (`municipio:pasto`, tipo `HISTORICO`, intensidad `INTENSO`):
   > «Durante la época virreinal, Pasto fue el centro administrativo, militar, cultural y religioso de la región. En el siglo XIX, sus habitantes defendieron la ciudad con firmeza durante la Independencia de Colombia. Por esa resistencia, Pasto llegó a ser conocida como «La Leona de los Andes».»
5. **Bogotá — Catedral primada de Colombia** (`wikipedia-es:333627`, tipo `HISTORICO`, intensidad `INTENSO`):
   > «La Catedral Primada de Colombia recibió ese título honorífico por decreto de la Congregación Consistorial, emitido por el papa León XIII el 7 de noviembre de 1902. El templo actual fue diseñado por el arquitecto capuchino fray Domingo de Petrés entre 1807 y 1823. Se construyó en el mismo lugar donde antes hubo otros tres templos que sirvieron sucesivamente como catedrales de la ciudad.»
6. **Cartagena — Torre del Reloj** (`wikipedia-es:8424637`, tipo `CURIOSO`, intensidad `INTENSO`):
   > «Un detalle curioso es que la puerta no solo servía de entrada: el puente levadizo que había a sus pies podía alzarse para impedir el paso de piratas y bucaneros. De ahí viene el nombre Boca del Puente, pues conectaba la Ciudad Amurallada con Getsemaní sobre el antiguo Canal de San Anastasio. La torre, en cambio, recibió el nombre de Puerta del Reloj por el reloj que la coronó a principios del siglo XVIII.»
7. **París — Torre Eiffel** (`wikipedia-es:7635`, tipo `INTERESANTE`, intensidad `INTENSO`):
   > «La Torre Eiffel, construida en hierro pudelado para la Exposición Universal de 1889 en París, se convirtió en símbolo de la ciudad y de Francia. Mide 300 metros, ampliados a 324 con una antena, y fue la estructura más alta del mundo durante cuarenta y un años. Su construcción duró poco más de dos años y provocó controversia entre algunos artistas, que la consideraban un «monstruo» de hierro. Hoy es uno de los monumentos turísticos más visitados del mundo.»

### Lista final de capturas de la Sesión 2 (`auditorias/capturas/s2-*.png`)

| Archivo | Qué muestra |
|---|---|
| `s2-antes-dato-borde.png` | **Antes del pulido:** texto alineado a la izquierda rozando el bisel inferior |
| `s2-antes-configuracion-intensidad.png` | **Antes del pulido:** descripción de intensidad Suave truncada con `...` |
| `s2-grande-00-icono-launcher.png` | Ícono adaptive A (pin teal + bombillo ámbar) en el launcher de Wear OS |
| `s2-grande-00b-splash-icono.png` | Pantalla de arranque del sistema con el ícono adaptive |
| `s2-grande-01-dialogo-permiso.png` | Diálogo en tiempo de ejecución del permiso de ubicación en Wear OS |
| `s2-grande-02-sin-permiso.png` | Estado diseñado *Sin permiso de ubicación* con botón de borde *Permitir* |
| `s2-grande-02b-buscando-dato.png` | Estado *Buscando un dato — Mirando alrededor…* |
| `s2-grande-03-dato-pasto-plaza-narino.png` | **Después del pulido:** cabecera de Dato con bombillo completo y texto centrado |
| `s2-grande-03b-dato-pasto-cuerpo.png` | Cuerpo completo del dato real generado para la Plaza de Nariño |
| `s2-grande-04-dato-pasto-acciones.png` | Cierre del dato, botones de escuchar/favorito/borrar y botón *Otro dato* |
| `s2-grande-05-inicio-explorando.png` | Pantalla Inicio en estado *Explorando* |
| `s2-grande-06-dato-pasto-wikipedia-en.png` | Dato en Pasto obtenido por el segundo eslabón (`en.wikipedia.org`: *St. Francis Xavier College, Pasto*) |
| `s2-grande-07-configuracion-tipos.png` | Configuración: encabezado y selección múltiple de tipos de dato |
| `s2-grande-07b-configuracion-aleatorio.png` | Configuración: tipos *Curioso*, *Gastronómico* e interruptor *Aleatorio* |
| `s2-grande-08-configuracion-intensidad.png` | **Después del pulido:** niveles *Suave*, *Normal* e *Intenso* sin truncamiento |
| `s2-grande-08b-configuracion-intenso-activo.png` | Nivel *Intenso* seleccionado en Configuración |
| `s2-grande-09-configuracion-modo-demo.png` | Configuración: *Vibrar con cada dato*, *Pausar datos* y *Modo demo* |
| `s2-grande-10-dato-bogota.png` | Dato real generado al teletransportarse a **Bogotá** (*Catedral primada de Colombia*) |
| `s2-grande-10b-inicio-bogota.png` | Inicio en Bogotá: *Explorando — Cerca de Catedral primada de Colombia* |
| `s2-grande-11-dato-cartagena.png` | Dato real generado al teletransportarse a **Cartagena** (*Torre del Reloj*) |
| `s2-grande-12-dato-paris.png` | Dato real generado al teletransportarse a **París** (*Torre Eiffel*) |
| `s2-grande-12b-inicio-paris.png` | Inicio tras visitar París |
| `s2-grande-13-calle-sin-dato-automatico.png` | Calle residencial sin hitos: el modo automático no dispara ningún dato |
| `s2-grande-13b-dato-municipio-pasto.png` | Botón manual *Dato ahora* en calle común: responde con dato del municipio (*Pasto*) |
| `s2-grande-14-historial-pasto.png` | Historial con entradas reales de Pasto (*Templo de San Juan Bautista*, *Plaza de Nariño*) |
| `s2-grande-14-historial-ciudades.png` | Historial agrupado bajo *Hoy* mostrando *Pasto* y *Torre Eiffel* |
| `s2-grande-14b-historial-lista.png` | Historial con filtro *Solo favoritos* y tarjeta en pantalla redonda |
| `s2-grande-14c-historial-ciudades-lista.png` | Historial con *Pasto*, *Torre Eiffel* y *Torre del Reloj* |
| `s2-grande-15-modo-demo-activo.png` | Interruptor *Modo demo* activado en Configuración |
| `s2-grande-16-modo-demo-sin-internet-teatro.png` | Modo demo con Wi-Fi apagado: dato de *Teatro Imperial* en Pasto |
| `s2-grande-16b-modo-demo-sin-internet-carnaval.png` | Modo demo con Wi-Fi apagado: dato de *Plaza del Carnaval* en Pasto |
| `s2-fuente-grande-01-inicio.png` | Escala de fuente grande (`1.24x`): Inicio (*Cerca de Plaza del Carnaval*) |
| `s2-fuente-grande-02-dato.png` | Escala de fuente grande (`1.24x`): Dato (*Plaza del Carnaval*) |
| `s2-fuente-grande-03-configuracion.png` | Escala de fuente grande (`1.24x`): Configuración |
| `s2-fuente-grande-04-historial.png` | Escala de fuente grande (`1.24x`): Historial |
| `s2-pequena-01-inicio-explorando.png` | Pantalla pequeña (`Small Round`, `192 dp`): Inicio (*Cerca de Plaza del Carnaval*) |
| `s2-pequena-02-dato.png` | Pantalla pequeña (`Small Round`): Dato (*Museo del Oro Nariño*) |
| `s2-pequena-03-dato-acciones.png` | Pantalla pequeña (`Small Round`): cuerpo del dato centrado |
| `s2-pequena-04-configuracion-intensidad.png` | Pantalla pequeña (`Small Round`): selector de intensidad |
| `s2-pequena-05-configuracion-modo-demo.png` | Pantalla pequeña (`Small Round`): *Pausar datos* y *Modo demo* |
| `s2-pequena-06-historial.png` | Pantalla pequeña (`Small Round`): Historial |

### Hallazgos de revisión de calidad y cómo se resolvieron
| Hallazgo | Resolución |
|---|---|
| Operador `!!` en `FuenteWikipedia.obtenerResumenes` al asociar `pageid` y `extract` | Reemplazado por `mapNotNull` con comprobación explícita de nulabilidad; quedan `0` usos de `!!` en todo el proyecto |
| Comprobación de tipo concreto `is FuenteMunicipio` dentro de `CadenaFuentesLugares` | Extraído `respaldoMunicipio: FuenteLugares?` como parámetro polimórfico explícito del último eslabón |
| `runCatching` silenciosos al desregistrar listeners de GPS y abrir ajustes del sistema | Reemplazados por `try / catch` específicos (`IllegalArgumentException`, `ActivityNotFoundException`) con registro en `Log.w` |
| `InvalidFragmentVersionForActivityResult` reportado por Android Lint al usar `registerForActivityResult` con `play-services-location` | Reemplazado por `ActivityCompat.requestPermissions` + `onRequestPermissionsResult` + sondeo reactivo de permiso en `ProveedorUbicacionFused` |
| `android.util.Log` lanzando excepción en tests unitarios JVM de la capa `data` | Habilitado `testOptions.unitTests.isReturnDefaultValues = true` en `app/build.gradle.kts` y validación de archivos vacíos en `CacheDatos` |

## 5. Checklist de criterios y requisitos globales

| Requisito global (`PLAN_FASES.md` §3) | Estado | Evidencia / Ubicación |
|---|---|---|
| Ubicación con GPS en tiempo real (`FusedLocationProviderClient`), solo en primer plano | **Cumplido** | `ProveedorUbicacionFused.kt`, `MainActivity.kt` (`repeatOnLifecycle(STARTED)`) |
| Cadena de fuentes de lugares: 1) Wikipedia `es`, 2) Wikipedia `en`, 3) Nominatim (máx. 1 req/s), 4) OpenAI Web Search (solo si `OPENAI_WEB_SEARCH=true`), 5) Municipio/región | **Cumplido** | `CadenaFuentesLugares.kt`, `FuenteWikipedia.kt`, `FuenteNominatim.kt`, `FuenteBusquedaWebOpenAi.kt`, `FuenteMunicipio.kt` |
| `User-Agent` descriptivo en Wikipedia y Nominatim | **Cumplido** | `ClienteHttpOkHttp.USER_AGENT_DESCRIPTIVO` |
| Solo lugares relevantes (`FiltroRelevancia`: artículo de Wikipedia o etiquetas OSM `tourism`/`historic` a `150–250 m`) | **Cumplido** | `FiltroRelevancia.kt`, `FuenteNominatim.clasificarRelevancia()` |
| El respaldo de municipio/región solo se usa con "Dato ahora", nunca en automático | **Cumplido** | `FuenteMunicipio.kt` (`radioMetros <= FiltroRelevancia.RANGO_LLEGADA.last` → `emptyList()`), probado en `CadenaFuentesLugaresTest` y en el emulador |
| Botón manual "Dato ahora" ignora límites de frecuencia y responde siempre (lugar relevante o municipio) | **Cumplido** | `ObtenerDatoAhora.kt` + `FuenteMunicipio.kt` |
| Dato generado con OpenAI basado únicamente en el contexto real | **Cumplido** | `GeneradorDatosOpenAi.kt` + `PromptBuilder.kt` |
| Configuración: tipos con selección múltiple (mín. 1), Aleatorio, 3 niveles de intensidad (1/h, 2/h, 4/h), voz, vibración, pausa | **Cumplido** | `ConfiguracionPantalla.kt`, `RepositorioConfiguracionDataStore.kt`, `PoliticaIntensidad.kt` |
| Historial en Room agrupado por día, abrir entrada, marcar favorito, borrar y anti-repetición | **Cumplido** | `RepositorioHistorialRoom.kt`, `HistorialPantalla.kt`, `DatoPantalla.kt`, `FiltroRepeticion.kt` |
| Caché por `lugar + tipo + intensidad` y Modo demo sin internet | **Cumplido** | `CacheDatos.kt`, `GeneradorDatosConCache.kt`, `RutaDemoPasto.kt`, `FuenteLugaresDemo` |
| Ahorro de tokens: prompt corto, contexto `<= 1200` chars, `max_output_tokens` por intensidad, `reasoning = none`, sin llamar a OpenAI si está en caché o se mostró hace poco | **Cumplido** | `PromptBuilder.kt`, `GeneradorDatosOpenAi.kt`, `GeneradorDatosConCache.kt` |
| Ícono adaptive A (pin de ubicación + bombillo, sin texto) | **Cumplido** | `res/mipmap/ic_launcher.xml`, `res/drawable/ic_launcher_foreground.xml` |
| Clean Architecture + MVVM + UDF; `domain` en Kotlin puro sin Android; sin lógica en Composables | **Cumplido** | Paquetes `domain`, `data`, `ui`, `di` |
| Inyección manual con `AppContainer` y patrones explícitos documentados | **Cumplido** | `di/AppContainer.kt`, `docs/arquitectura.md` |
| SOLID, funciones cortas, sin `!!`, sin `catch` vacíos, errores con `Resultado` / `ErrorDato` | **Cumplido** | Verificado con búsqueda estática y Lint |
| API key leída únicamente de `BuildConfig` desde `local.properties` (sin secretos en el repositorio) | **Cumplido** | `app/build.gradle.kts`, `git grep "sk-"` |
| Design system en `ui/theme`, contraste WCAG AA, toque mín. 48 dp, Small Round y fuente grande | **Cumplido** | `ui/theme/{Color,Tokens,DatosCercaTheme}.kt`, capturas `s2-pequena-*` y `s2-fuente-grande-*` |
| `@Preview` en cada pantalla (Large Round, Small Round y fuente grande) | **Cumplido** | `ui/*/...Previews.kt`, `ui/util/PreviewsWear.kt` |
| `./gradlew assembleDebug testDebugUnitTest lint` sin errores | **Cumplido** | `BUILD SUCCESSFUL`, 122 tests en verde, 0 errores de Lint |

## 6. Cómo probarlo

Sigue estos pasos numerados en tu PC para verificar toda la Sesión 2 y ensayar la demostración completa:

1. **Compilar, pasar todos los tests y revisar lint:**
   ```powershell
   .\gradlew.bat assembleDebug testDebugUnitTest lint
   ```
   - **Resultado esperado:** `BUILD SUCCESSFUL`, 122 tests pasados (0 fallidos) y 0 errores de lint.

2. **Verificar el emulador e instalar la aplicación limpia:**
   - Abre el emulador `Wear_OS_Large_Round` en Android Studio y comprueba su identificador (`emulator-5554`):
     ```powershell
     & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
     ```
   - Instala el APK, limpia los datos previos para ver el flujo de permisos desde cero y abre la app:
     ```powershell
     $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
     & $adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
     & $adb -s emulator-5554 shell pm clear com.daxlejo.datoscerca
     & $adb -s emulator-5554 shell am start -n com.daxlejo.datoscerca/.ui.MainActivity
     ```
   - **Resultado esperado:** aparece el diálogo del sistema pidiendo permiso de ubicación (*While using app*). Si pulsas atrás sin concederlo, ves la pantalla diseñada **Sin permiso de ubicación** con el botón **Permitir**. Al tocar **Permitir** y elegir *While using app*, la pantalla pasa automáticamente a **Explorando**.

3. **Probar teletransporte y datos reales con IA en Pasto, Bogotá, Cartagena y París:**
   - En la terminal, ejecuta el script de teletransporte a **Pasto**:
     ```powershell
     .\demo\teletransporte.bat pasto
     ```
   - **Resultado esperado:** el reloj detecta la **Plaza de Nariño** en `es.wikipedia.org`, llama a OpenAI y abre la pantalla **Dato** con el bombillo encendiéndose (o toca **Dato ahora** si ya estabas en ese punto).
   - Vuelve a **Inicio** (desliza hacia la derecha) y prueba **Bogotá**, **Cartagena** y **París**:
     ```powershell
     .\demo\teletransporte.bat bogota
     ```
     Toca **Dato ahora** → sale un dato real de la **Catedral primada de Colombia / Plaza de Bolívar**.
     ```powershell
     .\demo\teletransporte.bat cartagena
     ```
     Toca **Dato ahora** → sale un dato real de la **Torre del Reloj**.
     ```powershell
     .\demo\teletransporte.bat paris
     ```
     Toca **Dato ahora** → sale un dato real en español de la **Torre Eiffel**.

4. **Probar cambio a tipo "Cómico" e intensidad "Intenso":**
   - Desde **Inicio**, toca el ícono de engranaje (**Configuración**).
   - En **Tipos de dato**, deja marcado **Cómico** (desmarca los demás).
   - Baja hasta **Intensidad** y selecciona **Intenso** (*Hasta 4 por hora, un párrafo*).
   - Vuelve a **Inicio** y toca **Dato ahora** (como la clave de caché incluye `tipo` e `intensidad`, se generará un párrafo con humor ligero basado en los hechos reales del lugar).

5. **Probar una calle común sin lugares emblemáticos vs. "Dato ahora" (respaldo del municipio):**
   - Ejecuta:
     ```powershell
     .\demo\teletransporte.bat calle
     ```
   - **Resultado esperado (automático):** el reloj permanece en **Explorando — Te aviso al llegar.** y no dispara ningún dato automático porque en esa calle residencial no hay ningún lugar emblemático a menos de 200 m.
   - Ahora toca el botón **Dato ahora**:
   - **Resultado esperado (manual):** tras consultar la cadena de fuentes y ver que no hay hitos en 500 m, entra el último eslabón (`FuenteMunicipio`) y se abre un dato sobre el municipio de **Pasto**.

6. **Probar la ruta GPX (`demo/ruta-pasto.gpx`) y el Modo demo sin internet:**
   - Abre **Configuración**, baja hasta el final y activa el interruptor **Modo demo** (*Ruta sin internet y sin límite*). Esto precarga en `CacheDatos` las 8 paradas de Pasto.
   - Apaga el Wi-Fi de tu PC (o desactiva la red del emulador con `& $adb -s emulator-5554 shell svc wifi disable` y `& $adb -s emulator-5554 shell svc data disable`).
   - En la barra lateral del emulador, abre **`...` (Extended Controls) → Location → Routes → Import GPX/KML**, selecciona `demo/ruta-pasto.gpx`, elige velocidad `2x` o `3x` y pulsa **Play route**.
   - **Resultado esperado:** en el punto inicial (junto a una tienda común) no sale nada; al pasar por **Museo Taminango**, **Teatro Imperial**, **Catedral de Pasto**, **Templo de San Juan Bautista**, **Plaza de Nariño**, **Templo de Cristo Rey**, **Museo del Oro Nariño** y **Plaza del Carnaval**, el reloj muestra el dato de cada parada sin conexión a internet y sin límite por hora.
   - Al terminar, vuelve a activar el Wi-Fi (`& $adb -s emulator-5554 shell svc wifi enable` y `& $adb -s emulator-5554 shell svc data enable`).

7. **Revisar Historial y Favoritos:**
   - Desde **Inicio**, toca el ícono de reloj (**Historial**).
   - **Resultado esperado:** aparecen todas las entradas generadas agrupadas bajo **Hoy**. Toca una entrada para abrirla, pulsa la estrella para marcarla como favorita (rebota en color ámbar), vuelve atrás y activa **Solo favoritos** para filtrar.

## 7. Desviaciones y pendientes

- Ningún pendiente: los pendientes de la Sesión 1 (capturas en `Small Round`, capturas con fuente grande `1.24x` y texto actualizado de intensidad en Configuración) quedaron completados y capturados en `auditorias/capturas/s2-*`.
- `ProveedorUbicacion` mantiene la firma aprobada en la Sesión 1 (`Flow<EstadoUbicacion>`, donde `EstadoUbicacion.Disponible` transporta la `Coordenada` y `EstadoUbicacion.NoDisponible` modela `SinPermiso` y `SinGps`), permitiendo conservar el dominio intacto sin cambios.

## 8. Riesgos para la demo

- **Conectividad y latencia en vivo:** una llamada en frío (Wikipedia GeoSearch + extracto + OpenAI `gpt-6-luna`) tarda entre 4 y 7 segundos según la red. Una vez consultado un lugar con un tipo e intensidad, queda guardado en `files/cache_datos.json` y responde de forma instantánea.
- **Recomendación para la exposición:** dejar activado **Modo demo** durante el recorrido de `demo/ruta-pasto.gpx` (responde en milisegundos y funciona aunque falle el Wi-Fi del salón) y haber visitado una vez antes de la exposición los destinos de teletransporte (`pasto`, `bogota`, `cartagena`, `paris`) para que también estén respaldados en `cache_datos.json`.
- **Múltiples dispositivos ADB:** si hay un teléfono conectado por USB o Wi-Fi ADB, los scripts `demo/teletransporte.bat` y `demo/teletransporte.sh` eligen automáticamente el primer `emulator-*`, pero para comandos manuales de `adb` conviene pasar `-s emulator-5554`.

## 9. Archivos creados / modificados

- **Creados:**
  - `app/src/main/java/com/daxlejo/datoscerca/data/ubicacion/ProveedorUbicacionFused.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/red/ClienteHttp.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/lugares/LimitadorFrecuencia.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/lugares/FuenteWikipedia.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/lugares/FuenteNominatim.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/lugares/FuenteBusquedaWebOpenAi.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/lugares/FuenteMunicipio.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/lugares/CadenaFuentesLugares.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/ia/GeneradorDatosOpenAi.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/ia/CacheDatos.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/demo/RutaDemoPasto.kt`
  - `app/src/test/java/com/daxlejo/datoscerca/data/CadenaFuentesLugaresTest.kt`
  - `app/src/test/java/com/daxlejo/datoscerca/data/FuentesRealesYLimitadorTest.kt`
  - `app/src/test/java/com/daxlejo/datoscerca/data/GeneradorDatosYCacheTest.kt`
  - `demo/ruta-pasto.gpx`, `demo/teletransporte.bat`, `demo/teletransporte.sh`
  - `auditorias/SESION-2.md`, `auditorias/capturas/s2-*.png` (41 capturas)
- **Eliminados:**
  - `app/src/main/java/com/daxlejo/datoscerca/data/fake/DatosPasto.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/fake/Fakes.kt`
- **Modificados:**
  - `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/daxlejo/datoscerca/di/AppContainer.kt`, `di/FabricaViewModels.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/ui/MainActivity.kt`, `ui/navegacion/NavegacionApp.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/ui/inicio/InicioPantalla.kt`, `ui/dato/DatoPantalla.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/ui/configuracion/Configuracion{Pantalla,ViewModel,Previews}.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/ui/componentes/{BotonFavorito,ColumnaPantalla,Glifos,Indicadores,TarjetaDato}.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/ui/theme/Tokens.kt`, `ui/util/Textos.kt`
  - `app/src/main/res/values/strings.xml`
  - `README.md`, `docs/arquitectura.md`, `local.properties.example`

## 10. Correcciones (solo si no hubo visto bueno)

*(Pendiente de revisión del orquestador).*
