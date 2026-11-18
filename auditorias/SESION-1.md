# Auditoría — Sesión 1: Arquitectura, diseño completo y lógica local

**Fecha:** 2026-11-16 (correcciones del orquestador: 2026-11-17)
**Estado propuesto:** Listo para nueva revisión. Se aplicaron las correcciones pedidas por el orquestador (sección 10). Las capturas de Small Round pasan a la Sesión 2; lo demás pendiente está en la sección 7.

## 1. Resumen

Se creó la app Wear OS standalone "Datos Cerca" (`com.daxlejo.datoscerca`) con arquitectura por capas (`domain` puro, `data`, `ui`, `di`), el design system, las cuatro pantallas con todos sus estados, configuración real en DataStore, historial real en Room y el motor de datos del dominio con tests. La ubicación, los lugares y la IA son fakes con datos de muestra del centro de Pasto. La app se instaló y se recorrió en los emuladores Large Round y Small Round (API 37).

La sesión se cortó dos veces por límite de uso y al final el equipo se quedó sin memoria para mantener los dos emuladores abiertos; por eso quedan los pendientes de la sección 7.

## 2. Entregables

| Entregable | Estado | Ruta |
|---|---|---|
| Proyecto Gradle (Kotlin DSL, version catalog, minSdk 30, BuildConfig desde `local.properties`) | Hecho | `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml` |
| `local.properties.example` | Hecho | `local.properties.example` |
| Dominio: modelos, errores, puertos | Hecho | `app/src/main/java/com/daxlejo/datoscerca/domain/{modelo,error,puerto}` |
| Motor de datos (política por hora, filtro de relevancia, generación única, selector, anti-repetición, PromptBuilder, motor automático) | Hecho | `domain/motor` |
| Casos de uso | Hecho | `domain/casouso` |
| Room (entity, DAO, mapper, repositorio) | Hecho | `data/historial` |
| DataStore | Hecho | `data/configuracion` |
| Fakes de Pasto | Hecho | `data/fake` |
| Vibración propia y TTS en español | Hecho (voz no verificada de oído) | `data/plataforma` |
| DI manual | Hecho | `di/AppContainer.kt`, `di/FabricaViewModels.kt` |
| Design system y componentes | Hecho | `ui/theme`, `ui/componentes` |
| 4 pantallas con ViewModel y UiState | Hecho | `ui/{inicio,dato,configuracion,historial}` |
| `@Preview` grande, pequeña y fuente grande | Hecho (no revisadas en Android Studio) | `ui/*/…Previews.kt`, `ui/util/PreviewsWear.kt` |
| Ícono adaptive (pin + bombillo) | Hecho | `res/mipmap/ic_launcher.xml`, `res/drawable/ic_launcher_foreground.xml` |
| Tests unitarios | Hecho: 101 | `app/src/test` |
| Documentación | Hecho | `docs/arquitectura.md`, `README.md` |
| Capturas Large Round | Hecho: 25, sobre el código final | `auditorias/capturas/s1-grande-*` |
| Capturas Small Round | Parcial: 17, casi todas anteriores a los últimos ajustes. Pasan a la Sesión 2 por indicación del orquestador | `auditorias/capturas/s1-pequena-*` |
| Commit y push | Hecho | rama `master` en `https://github.com/Daxlejo/SmartWatch.git` |

## 3. Decisiones de arquitectura y diseño

**Patrones** (detalle en `docs/arquitectura.md`, sección 6):
- Repository: `RepositorioHistorial`/`RepositorioConfiguracion` → `RepositorioHistorialRoom`, `RepositorioConfiguracionDataStore`.
- Strategy: `PoliticaIntensidad` (Suave/Normal/Intenso) y `SelectorTipoDato` (EnOrden/Aleatorio), con `FabricaPoliticas` y `FabricaSelectores`.
- Builder: `PromptBuilder`. Observer: Flows. State: `EstadoMotor` y los `UiState` sellados.
- Adapter: `RelojSistema`, `VibradorAndroid`, `NarradorTts`, `EntradaMapper`.
- Chain of Responsibility: solo el contrato (`FuenteLugares`); la cadena llega en la Sesión 2.

**Decisiones propias:**
- Omitir un dato automático no es un error: `ObtenerDatoParaUbicacion` devuelve `Exploracion` (`NuevoDato`, `Omitido`, `Fallo`).
- Dos radios de búsqueda: el de llegada de `FiltroRelevancia` (200 m, configurable entre 150 y 250) para el dato automático y 500 m para "Dato ahora". (Antes eran 80 m; ver sección 10.)
- La configuración se guarda con una transformación atómica (`actualizar { }`), para que dos toques seguidos no se pisen.
- Con varios tipos marcados y Aleatorio apagado, la "selección fija" recorre los tipos en orden.
- Un dato automático solo se abre solo si la persona está en Inicio; en otra pantalla vibra y queda en el historial.

**Diseño visual:**
- Inicio: el pin al centro con ondas de exploración, flanqueado por historial y configuración; así "Dato ahora" cabe sin scroll.
- Dato: un único momento animado (el bombillo ámbar se enciende y el contenido entra en cascada). Nombre del lugar en serif.
- Ámbar reservado a bombillo y favoritos; un color por tipo de dato.
- Márgenes proporcionales a la pantalla redonda y un velo superior para que la hora no choque con el texto al hacer scroll.

## 4. Evidencias

- **Versiones verificadas** en documentación oficial: AGP 9.5.1, Gradle 9.9.1, Kotlin 2.4.21, KSP 2.3.14, Compose BOM 2026.09.00, Wear Compose 1.7.1, Room 2.8.5, DataStore 1.3.0, Lifecycle 2.11.0.
- **`./gradlew assembleDebug testDebugUnitTest lint`** sobre el código final (después de las correcciones de la sección 10): `BUILD SUCCESSFUL`, 15 clases de test, **101 tests, 0 fallidos**, **lint 0 hallazgos**.
- **Emulador:** flujo completo recorrido en Large Round y Small Round: Inicio → Dato ahora → Dato → favorito → diálogo de borrado → Otro dato → Historial → filtro → Configuración → pausa → reanudar, más los estados de error y el dato automático al llegar a un lugar.
- **Secretos:** `grep "sk-"` solo encuentra el ejemplo `sk-...` de `PLAN_FASES.md`. `local.properties` está ignorado por git.
- **Reglas:** sin `!!`, sin `import android` en `domain`, `ui` no importa `data`, ningún `catch` vacío.

**Hallazgos de pr-review-toolkit y qué se hizo:**

| Hallazgo | Resolución |
|---|---|
| Inicio se quedaba mostrando un error o "En pausa" viejo | Corregido: el fallo manual se limpia cuando el motor cambia de estado |
| La pantalla de problema en Dato no tenía vuelta atrás | Corregido: acción "Volver al dato" |
| El motor no reintentaba un lugar omitido por intervalo o fallo | Corregido: reintento a los 30 s, con dos tests nuevos |
| Estados imposibles en `DatoUiState` (dos booleanos) | Corregido: `AccionDato` |
| `SinPermiso`/`SinGps` duplicados y un `else` que tragaba casos | Corregido: `ErrorUbicacion` y `EstadoUbicacion.NoDisponible` |
| Faltaba un error para fuentes caídas y una clave de caché | Añadidos `FuenteNoDisponible` y `SolicitudGeneracion.claveCache` |
| El primer dato no se leía en voz alta (TTS aún arrancando) | Corregido: el texto queda pendiente y se lee al iniciar |
| Tamaños sueltos en componentes | Movidos a `Tamanos` |
| Valores desconocidos reemplazados sin aviso | Ahora se registran en el log |
| DataStore corrupto fallaba para siempre | Añadido `ReplaceFileCorruptionHandler` |

No atendidos: ver sección 7.

## 5. Checklist de criterios

- [x] Capas separadas; `domain` sin Android
- [x] Sin lógica en Composables
- [x] Sin secretos en el código
- [x] Design system usado (los tamaños sueltos se movieron a tokens)
- [x] Build, tests y lint en verde sobre el código final
- [x] Configuración persiste (DataStore) e historial persiste (Room)
- [x] Todos los estados diseñados
- [x] Motor de datos con tests
- [x] Capturas finales de Large Round
- [ ] Capturas finales de Small Round y con fuente grande
- [x] Commit y push

## 6. Cómo probarlo

1. **Compilar y probar.** En la carpeta del proyecto: `.\gradlew.bat assembleDebug testDebugUnitTest lint`. Esperado: `BUILD SUCCESSFUL`, 101 tests sin fallos.
2. **Abrir el emulador** Wear OS Large Round y comprobar su nombre con `adb devices` (normalmente `emulator-5554`). Si tienes el teléfono conectado por ADB, usa siempre `-s emulator-5554`; `gradlew installDebug` falla porque intenta instalar también en el teléfono.
3. **Instalar y abrir:**
   `adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk`
   `adb -s emulator-5554 shell am start -n com.daxlejo.datoscerca/.ui.MainActivity`
   Esperado: aparece el ícono (pin teal con bombillo ámbar) y luego Inicio con "Explorando".
4. **Dato ahora.** Toca el botón inferior nada más abrir. Esperado: "Buscando un dato" un segundo y luego la pantalla Dato (bombillo que se enciende, tipo, "Iglesia de San Juan Bautista", unos 380 m y texto). Al lado del punto de partida hay una "Tienda La Esquina" a unos 50 m, pero no es un lugar relevante y el dato nunca habla de ella.
5. **Acciones del dato.** Desliza hasta abajo. Toca la estrella: se pone ámbar y rebota. Toca "Otro dato": cambia a otro lugar y vuelve arriba.
6. **Historial.** Vuelve a Inicio (desliza a la derecha) y toca el ícono del reloj. Esperado: los datos bajo "Hoy", con estrella en el favorito. Activa "Solo favoritos": queda solo ese.
7. **Borrar.** Abre un dato del historial, baja, toca la papelera y confirma. Borra todos. Esperado: "Aún no hay datos".
8. **Configuración.** Desde Inicio toca el engranaje. Cambia tipos, Aleatorio e intensidad. Intenta desmarcar el último tipo: no se desmarca y aparece "Deja al menos un tipo marcado.". Cierra la app (`adb -s emulator-5554 shell am force-stop com.daxlejo.datoscerca`), ábrela de nuevo: la configuración se mantiene.
9. **Pausa.** Activa "Pausar datos" y vuelve a Inicio. Esperado: "En pausa" y botón "Reanudar". Tócalo: vuelve a "Explorando".
10. **Dato automático.** Borra los datos de la app (`adb -s emulator-5554 shell pm clear com.daxlejo.datoscerca`), ábrela y déjala en Inicio alrededor de un minuto con la intensidad por defecto (Normal). Esperado: al empezar, junto a la tienda, no pasa nada; al quedar a menos de 200 m de San Juan Bautista o de la Plaza de Nariño vibra y se abre solo un dato. En Normal salen como mucho dos datos automáticos por hora; después el reloj sigue caminando sin dar más.
11. **Estados de error.** Para cada valor (`SIN_PERMISO`, `SIN_GPS`, `SIN_INTERNET`, `SIN_LUGARES`, `ERROR_IA`):
    `adb -s emulator-5554 shell am start -S -n com.daxlejo.datoscerca/.ui.MainActivity --es escenario SIN_INTERNET`
    Con los dos primeros el estado aparece solo; con los otros toca "Dato ahora". Esperado: ícono, título, mensaje y "Reintentar".
11b. **Fallo al pedir otro dato.** En el emulador no hay una forma fiable de provocarlo (ver sección 7), así que se revisa en las previews: abre `ui/dato/DatoPreviews.kt` en Android Studio y mira `DatoErrorIaPreview`, `DatoSinInternetPreview` y `DatoSinLugaresPreview`. Esperado: ícono de error, título, mensaje, botón "Volver al dato" y "Reintentar" en el borde.
12. **Small Round y fuente grande.** Repite los pasos 3 a 6 en el emulador pequeño. Para fuente grande: `adb -s emulator-XXXX shell settings put system font_scale 1.24` (vuelve a `1.0` al terminar). Esperado: nada cortado; en pantalla pequeña el botón dice "Dato ahora" en dos líneas.
13. **Previews.** En Android Studio abre `ui/inicio/InicioPreviews.kt` y los otros tres `…Previews.kt`.

## 7. Desviaciones y pendientes

**Pendientes de esta sesión (no hechos):**
- **Capturas de Small Round:** solo `s1-pequena-01-inicio-explorando.png` es del código final. Las otras 16 son anteriores a los últimos ajustes de pantalla pequeña (márgenes laterales, textos más cortos, etiqueta del botón en dos líneas, etiqueta del tipo en el historial), así que el aspecto real es algo distinto. Faltan además las de estados de error, pausa y "buscando" en Small Round: las que había salieron mal por un toque fallido del guion y se borraron. No se repitieron porque el sistema cerró el emulador pequeño por falta de memoria.
- **Pantalla de problema dentro de Dato** ("Otro dato" falla → ícono, mensaje, "Reintentar" y "Volver al dato"): está implementada y compila, pero **no se vio en el emulador**. El guion no logró forzar el fallo con la app ya abierta (cambiar de escenario con `am start --es escenario …` sobre la app en marcha no surtió efecto; no se investigó por qué). Para verla hay que seguir el paso 11b de "Cómo probarlo".
- **Aviso de voz no disponible en Configuración:** implementado, no visto en el emulador.
- **Fuente grande:** no se capturó ni se revisó en el emulador.
- **Voz:** no se comprobó de oído que el emulador lea en español.

**Hallazgos de revisión no atendidos:**
- ~~Los Flows de Room no tienen manejo de errores~~: corregido en la sección 10. Sigue sin exportarse el esquema (`exportSchema = false`); la migración 1→2 está escrita a mano.
- Si DataStore falla al leer (no por corrupción), se usan los valores por defecto, lo que quita la pausa sin avisar.
- El aviso "No se pudo guardar el cambio" de Configuración aparece arriba de la lista, lejos del control tocado.
- Un fallo de voz en un dato automático solo queda en el log.
- Las causas de `ErrorIa` y `Almacenamiento` no se registran ni se muestran.
- ~~"Dato ahora" y el dato automático pueden coincidir y producir dos datos seguidos~~: corregido en la sección 10.
- `Lugar` del historial llega con `contexto` vacío; el revisor sugiere un tipo aparte (`LugarVisitado`).

**Desviaciones respecto al plan:**
- Borrar se hace desde la pantalla Dato (abierta desde el Historial), no deslizando la fila.
- `ProveedorUbicacion` emite `Flow<EstadoUbicacion>` en lugar de `Flow<Coordenada>`, para modelar sin permiso y sin GPS.
- Se añadieron `ObservarEntrada`, `Configuracion.modoDemo` (sin interfaz todavía) y el mecanismo `--es escenario` para ver estados de error (solo en debug).
- No se declaran permisos de ubicación todavía; van en la Sesión 2.
- El `EdgeButton` no lleva ícono: no cabía junto al texto.
- Se corrigió la línea `sdk.dir` de `local.properties` (barras sin escapar); la clave no se tocó.
- El ícono adaptive está en `res/mipmap/` y no en `mipmap-anydpi`, como sugería lint: con esa carpeta AAPT no encontraba el recurso.

## 8. Riesgos para la demo

- Las coordenadas de los lugares de muestra son aproximadas; la Sesión 2 debe verificarlas contra OpenStreetMap.
- Con un teléfono conectado por ADB, los comandos sin `-s` fallan o apuntan al dispositivo equivocado.
- La voz depende de que el emulador tenga una voz en español instalada.
- La base ya está en la versión 2 (`MIGRACION_1_2`). La migración se generó y validó contra el esquema de Room al compilar, pero no se ejecutó sobre una base v1 real en el emulador. Si falla, `pm clear` la resuelve (se pierde el historial de prueba).

## 9. Archivos creados / modificados

- Raíz: `.gitignore`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `local.properties.example`, `README.md`, `docs/arquitectura.md`.
- `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/**` (strings, ícono adaptive, 14 íconos vectoriales, reglas de copia).
- `app/src/main/java/com/daxlejo/datoscerca/**`: `DatosCercaApp`, `domain` (modelo, error, puerto, motor, casouso), `data` (historial, configuracion, plataforma, fake), `di`, `ui` (theme, componentes, navegacion, util, inicio, dato, configuracion, historial).
- `app/src/test/java/com/daxlejo/datoscerca/**`: soporte (dobles y banco de pruebas) y 15 clases de test.
- `auditorias/SESION-1.md`, `auditorias/capturas/s1-*.png`.
- Modificado: `local.properties` (solo la línea `sdk.dir`).

## 10. Correcciones

Revisión del orquestador: **no aprobada todavía**. Se aplicaron las seis correcciones pedidas. Las capturas de Small Round pasan a la Sesión 2, como se indicó.

**1. Política de intensidad según el plan actual (secciones 3 y 4 de `PLAN_FASES.md`).**
- `PoliticaIntensidad` ya no usa intervalos entre datos. Cada nivel tiene un máximo por hora: `PoliticaSuave` 1 (una frase), `PoliticaNormal` 2 (dos frases), `PoliticaIntenso` 4 (párrafo corto). La lógica común está en `PoliticaConLimitePorHora`.
- La ventana es móvil: `ObtenerDatoParaUbicacion` cuenta en el historial los datos automáticos de los últimos 60 minutos (`RepositorioHistorial.contarAutomaticosDesde`, consulta `COUNT` en Room) y se los pasa a `evaluar(automaticosUltimaHora)`.
- Para distinguir manuales de automáticos, `Dato` tiene ahora `origen: OrigenDato` (`AUTOMATICO` o `MANUAL`), guardado en Room.
- El modo demo envuelve la política en `PoliticaDemo` (Decorator): mantiene el largo y nunca rechaza. `FabricaPoliticas.para(intensidad, modoDemo)` decide.
- "Dato ahora" no consulta la política (solo toma de ella el largo) y no gasta el cupo.
- El límite se revisa antes de buscar lugares: con el cupo lleno no se consulta ninguna fuente (en la Sesión 2 eso ahorra llamadas a Wikipedia).
- El motor reintenta a los 30 s cuando el motivo es `LIMITE_POR_HORA`, para que una persona quieta junto a un lugar reciba el dato al liberarse el cupo.
- Textos de Configuración: "Hasta 1 dato por hora, una frase", "Hasta 2 por hora, dos frases", "Hasta 4 por hora, un párrafo".

**2. `FiltroRelevancia` en el dominio.**
- `Lugar` lleva `relevancia: Relevancia` (`ARTICULO_WIKIPEDIA`, `ETIQUETA_OSM_PATRIMONIAL`, `NINGUNA`), que llena la fuente. No tiene valor por defecto, para que ninguna fuente de la Sesión 2 la olvide.
- `domain/motor/FiltroRelevancia(radioLlegadaMetros = 200)` exige un radio entre 150 y 250 m (`require`). `alLlegar(cercanos)` deja solo los emblemáticos dentro del radio; `emblematicos(cercanos)` sirve para "Dato ahora", que también habla solo de lugares relevantes, pero en 500 m.
- `FuenteLugares` pasó de `buscarCercano(...): Lugar` a `buscarCercanos(coordenada, radio): List<Lugar>`. Así el dominio puede saltarse un lugar común más cercano y quedarse con el relevante. La lista vacía reemplaza a `Fallo(SinLugares)` como "aquí no hay nada" en la cadena.
- El radio de llegada se configura en `AppContainer` (no es un ajuste de la persona). `RadioBusqueda.LLEGADA_METROS = 80` se eliminó.
- Fakes: se añadió "Tienda La Esquina" (`Relevancia.NINGUNA`) a unos 50 m del punto de partida, que se movió para que ningún lugar relevante quede a menos de 200 m. La ruta simulada pasa primero por la tienda y no sale ningún dato. Museo Taminango quedó como `ETIQUETA_OSM_PATRIMONIAL` y el resto como `ARTICULO_WIKIPEDIA`.

**3. Una sola generación a la vez.**
- `domain/motor/GeneracionUnica` envuelve un `Mutex` con `tryLock`. Una sola instancia, creada en `AppContainer`, la comparten `ObtenerDatoAhora` y `ObtenerDatoParaUbicacion`, y cubre todo el caso de uso: el conteo, la elección del lugar, la IA y el guardado.
- Si hay un dato en curso, el otro **no espera turno**, porque saldrían dos datos seguidos. El automático devuelve `Omitido(GENERACION_EN_CURSO)` y no se reintenta hasta que la persona se mueva. El manual devuelve `Fallo(ErrorDato.GeneracionEnCurso)`.
- En Inicio ese fallo no se muestra como error: el dato automático que ya viene se abre solo. En Dato ("Otro dato") aparece como aviso bajo el dato: "Hay otro dato en preparación; aparece en un momento."

**4. Errores de lectura de Room.**
- `RepositorioHistorial.observar()` y `observarEntrada()` emiten `Flow<Resultado<…>>`. `RepositorioHistorialRoom` captura la `SQLiteException` con `catch`, la registra en el log y emite `Fallo(Almacenamiento)`. Cualquier otra excepción se relanza.
- `HistorialUiState.Error` muestra "No se pudo leer" y "El historial guardado no se pudo abrir. Cierra y abre la app." `DatoUiState.ErrorLectura` muestra "No se pudo leer" y "Este dato no se pudo abrir. Vuelve e inténtalo de nuevo.", con el botón "Volver". Hay previews de los dos.
- Base de datos en la versión 2 con `MIGRACION_1_2` (columnas `lugar_relevancia` y `origen`). Lo guardado antes queda como `MANUAL`, así que no gasta cupo.

**5. Tests nuevos o reescritos** (de 82 a 101 tests, de 11 a 15 clases):
- `PoliticaIntensidadTest`: límite de cada nivel (1, 2 y 4), el modo demo permite siempre y conserva el largo, ventana de una hora.
- `ObtenerDatoParaUbicacionTest`: límite por hora de Suave, Normal e Intenso con ventana móvil (se llena el cupo, se rechaza y se libera cuando el primero sale de la hora). También: la ventana es móvil y no un intervalo fijo (datos en los minutos 0 y 40: el del 61 pasa y el del 62 no), modo demo sin límite, los manuales no gastan cupo, con el cupo lleno no se consultan lugares, un lugar no relevante no da dato aunque esté encima, uno relevante fuera del radio tampoco, y se prefiere el relevante aunque uno común quede más cerca.
- `ObtenerDatoAhoraTest`: ignora el límite aunque el cupo esté lleno, solo habla de lugares relevantes, guarda con origen `MANUAL`.
- `FiltroRelevanciaTest` (nuevo): relevante dentro o fuera del radio, no relevante, orden por distancia, criterio manual y validación del rango 150–250 m.
- `GeneracionUnicaTest` (nuevo): la segunda se descarta mientras la primera sigue, se libera al terminar y también si la acción falla.
- `GeneracionEnCursoTest` (nuevo): con un manual en curso el automático se omite sin llamar a la IA; con un automático en curso el manual responde `GeneracionEnCurso`; en ambos casos queda un solo dato en el historial.
- `CasosDeHistorialTest` y `HistorialViewModelTest` (nuevo): un fallo de lectura llega como `Fallo` en el flujo y como `HistorialUiState.Error` en pantalla.
- `MotorAutomaticoTest`: el caso "quieto en un lugar" ahora espera a que se libere el cupo de la hora.

**6. Verificación y entrega.**
- `./gradlew assembleDebug testDebugUnitTest lint`: `BUILD SUCCESSFUL`, 101 tests, 0 fallidos, lint 0 hallazgos.
- `docs/arquitectura.md` actualizado: módulos, diagrama de clases (`Relevancia`, `OrigenDato`, `PoliticaDemo`, `FiltroRelevancia`, `GeneracionUnica`), secuencia, estados del motor, tabla de intensidad y patrones (Decorator y exclusión mutua). El README también.
- Se incluye en el commit el cambio de `PLAN_FASES.md` que ya estaba en el árbol de trabajo: es la versión del plan que pide estas correcciones.

**Lo que no se hizo en esta corrección:**
- No se probó en el emulador. Los emuladores estaban cerrados por falta de memoria, y las correcciones son de dominio y están cubiertas por tests. Tampoco se ejecutó la migración 1→2 sobre una base real (ver sección 8).
- No se repitieron capturas. Las de Small Round pasan a la Sesión 2; las de Large Round siguen siendo válidas salvo la descripción de intensidad en Configuración (`s1-grande-13-configuracion-intensidad.png`), que todavía dice "Un dato cada 10 min".
- "Dato ahora" sin ningún lugar emblemático en 500 m responde `SinLugares`. El último recurso "dato del municipio" es de la Sesión 2 (cadena de fuentes).
- Borrar un dato automático libera su cupo, porque el límite se calcula sobre el historial guardado.
