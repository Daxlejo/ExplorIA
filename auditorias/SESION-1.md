# Auditoría — Sesión 1: Arquitectura, diseño completo y lógica local

**Fecha:** 2026-11-16
**Estado propuesto:** Con pendientes (ver sección 7). El código está completo, verificado y en un commit; faltan las capturas finales de Small Round y ver en el emulador dos cosas que no se llegaron a comprobar.

## 1. Resumen

Se creó la app Wear OS standalone "Datos Cerca" (`com.daxlejo.datoscerca`) con arquitectura por capas (`domain` puro, `data`, `ui`, `di`), el design system, las cuatro pantallas con todos sus estados, configuración real en DataStore, historial real en Room y el motor de datos del dominio con tests. La ubicación, los lugares y la IA son fakes con datos de muestra del centro de Pasto. La app se instaló y se recorrió en los emuladores Large Round y Small Round (API 37).

La sesión se cortó dos veces por límite de uso y al final el equipo se quedó sin memoria para mantener los dos emuladores abiertos; por eso quedan los pendientes de la sección 7.

## 2. Entregables

| Entregable | Estado | Ruta |
|---|---|---|
| Proyecto Gradle (Kotlin DSL, version catalog, minSdk 30, BuildConfig desde `local.properties`) | Hecho | `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml` |
| `local.properties.example` | Hecho | `local.properties.example` |
| Dominio: modelos, errores, puertos | Hecho | `app/src/main/java/com/daxlejo/datoscerca/domain/{modelo,error,puerto}` |
| Motor de datos (política, selector, anti-repetición, PromptBuilder, motor automático) | Hecho | `domain/motor` |
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
| Tests unitarios | Hecho: 82 | `app/src/test` |
| Documentación | Hecho | `docs/arquitectura.md`, `README.md` |
| Capturas Large Round | Hecho: 25, sobre el código final | `auditorias/capturas/s1-grande-*` |
| Capturas Small Round | Parcial: 17, casi todas anteriores a los últimos ajustes (ver sección 7) | `auditorias/capturas/s1-pequena-*` |
| Commit | Hecho, sin push | rama `master`; remoto `origin` = `https://github.com/Daxlejo/SmartWatch.git` |

## 3. Decisiones de arquitectura y diseño

**Patrones** (detalle en `docs/arquitectura.md`, sección 6):
- Repository: `RepositorioHistorial`/`RepositorioConfiguracion` → `RepositorioHistorialRoom`, `RepositorioConfiguracionDataStore`.
- Strategy: `PoliticaIntensidad` (Suave/Normal/Intenso) y `SelectorTipoDato` (EnOrden/Aleatorio), con `FabricaPoliticas` y `FabricaSelectores`.
- Builder: `PromptBuilder`. Observer: Flows. State: `EstadoMotor` y los `UiState` sellados.
- Adapter: `RelojSistema`, `VibradorAndroid`, `NarradorTts`, `EntradaMapper`.
- Chain of Responsibility: solo el contrato (`FuenteLugares`); la cadena llega en la Sesión 2.

**Decisiones propias:**
- Omitir un dato automático no es un error: `ObtenerDatoParaUbicacion` devuelve `Exploracion` (`NuevoDato`, `Omitido`, `Fallo`).
- Dos radios de búsqueda: 80 m para el dato automático y 500 m para "Dato ahora".
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
- **`./gradlew assembleDebug testDebugUnitTest lint`** sobre el código final: `BUILD SUCCESSFUL`, 11 clases de test, **82 tests, 0 fallidos**, **lint 0 hallazgos**.
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
- [x] Commit (sin push)

## 6. Cómo probarlo

1. **Compilar y probar.** En la carpeta del proyecto: `.\gradlew.bat assembleDebug testDebugUnitTest lint`. Esperado: `BUILD SUCCESSFUL`, 82 tests sin fallos.
2. **Abrir el emulador** Wear OS Large Round y comprobar su nombre con `adb devices` (normalmente `emulator-5554`). Si tienes el teléfono conectado por ADB, usa siempre `-s emulator-5554`; `gradlew installDebug` falla porque intenta instalar también en el teléfono.
3. **Instalar y abrir:**
   `adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk`
   `adb -s emulator-5554 shell am start -n com.daxlejo.datoscerca/.ui.MainActivity`
   Esperado: aparece el ícono (pin teal con bombillo ámbar) y luego Inicio con "Explorando".
4. **Dato ahora.** Toca el botón inferior. Esperado: "Buscando un dato" un segundo y luego la pantalla Dato (bombillo que se enciende, tipo, "Iglesia de San Juan Bautista", distancia y texto).
5. **Acciones del dato.** Desliza hasta abajo. Toca la estrella: se pone ámbar y rebota. Toca "Otro dato": cambia a otro lugar y vuelve arriba.
6. **Historial.** Vuelve a Inicio (desliza a la derecha) y toca el ícono del reloj. Esperado: los datos bajo "Hoy", con estrella en el favorito. Activa "Solo favoritos": queda solo ese.
7. **Borrar.** Abre un dato del historial, baja, toca la papelera y confirma. Borra todos. Esperado: "Aún no hay datos".
8. **Configuración.** Desde Inicio toca el engranaje. Cambia tipos, Aleatorio e intensidad. Intenta desmarcar el último tipo: no se desmarca y aparece "Deja al menos un tipo marcado.". Cierra la app (`adb -s emulator-5554 shell am force-stop com.daxlejo.datoscerca`), ábrela de nuevo: la configuración se mantiene.
9. **Pausa.** Activa "Pausar datos" y vuelve a Inicio. Esperado: "En pausa" y botón "Reanudar". Tócalo: vuelve a "Explorando".
10. **Dato automático.** Deja la app en Inicio unos 40 s con intensidad Normal o Intenso. Esperado: vibra y se abre sola un dato al "llegar" a la plaza.
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
- **Push:** el commit no se subió a GitHub.

**Hallazgos de revisión no atendidos:**
- Los Flows de Room no tienen manejo de errores: un fallo de lectura de la base cerraría la app. No hay política de migraciones (`exportSchema = false`).
- Si DataStore falla al leer (no por corrupción), se usan los valores por defecto, lo que quita la pausa sin avisar.
- El aviso "No se pudo guardar el cambio" de Configuración aparece arriba de la lista, lejos del control tocado.
- Un fallo de voz en un dato automático solo queda en el log.
- Las causas de `ErrorIa` y `Almacenamiento` no se registran ni se muestran.
- "Dato ahora" y el dato automático pueden coincidir y producir dos datos seguidos.
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
- Al subir la versión de la base de datos en la Sesión 2 hará falta una migración o borrar los datos de la app.

## 9. Archivos creados / modificados

- Raíz: `.gitignore`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `local.properties.example`, `README.md`, `docs/arquitectura.md`.
- `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/**` (strings, ícono adaptive, 14 íconos vectoriales, reglas de copia).
- `app/src/main/java/com/daxlejo/datoscerca/**`: `DatosCercaApp`, `domain` (modelo, error, puerto, motor, casouso), `data` (historial, configuracion, plataforma, fake), `di`, `ui` (theme, componentes, navegacion, util, inicio, dato, configuracion, historial).
- `app/src/test/java/com/daxlejo/datoscerca/**`: soporte (dobles y banco de pruebas) y 11 clases de test.
- `auditorias/SESION-1.md`, `auditorias/capturas/s1-*.png`.
- Modificado: `local.properties` (solo la línea `sdk.dir`).

## 10. Correcciones

(Sin correcciones todavía.)
