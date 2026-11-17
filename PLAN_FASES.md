# Plan de sesiones — App para SmartWatch (Wear OS)

App de Wear OS que, según la ubicación de la persona (en cualquier lugar del mundo), le da datos con IA sobre lo que tiene cerca. El tipo de dato y la intensidad son configurables. Se presenta en el emulador de Wear OS de Android Studio.

> **Materia: Diseño de Software.** La arquitectura y el diseño visual se evalúan tanto como la funcionalidad. Nada de código espagueti.

**Modelo:** Claude Fable en Claude Code (selecciónalo con `/model` al iniciar cada sesión).

---

## 1. Cómo funciona este plan

Son **2 sesiones**. Cada una se ejecuta en una sesión nueva de Claude Code.

| Rol | Quién | Qué hace |
|---|---|---|
| Orquestador | Claude (chat) | Da o niega el visto bueno de cada sesión leyendo la auditoría |
| Agente | Claude Code + Fable (terminal) | Ejecuta **una** sesión, deja la auditoría con la guía de pruebas y se detiene |
| Puente | David | Pega el prompt, prueba la app con la guía, lleva la auditoría al orquestador y registra la aprobación |

**Flujo por sesión**

1. David abre `claude` en la carpeta del proyecto, elige Fable con `/model` y pega el prompt de la sesión (en inglés).
2. El agente verifica la puerta de aprobación, ejecuta la sesión y escribe `auditorias/SESION-N.md` **en español**. Esa auditoría incluye una **guía de pruebas** paso a paso.
3. El agente **se detiene**.
4. David sigue la guía de pruebas en su PC y anota lo que vio, sobre todo si algo falló.
5. David le pasa al orquestador la auditoría, sus notas de prueba y las capturas.
6. El orquestador responde **VISTO BUENO** o **SIN VISTO BUENO + lista de correcciones**.
7. Si hay visto bueno, David agrega en `auditorias/APROBACIONES.md` la línea `SESION 1: APROBADA — <fecha>`. Si no lo hay, pega las correcciones en la misma sesión del agente, que corrige y actualiza la sección "Correcciones" de la auditoría. Se repite el paso 4.

**Puerta de aprobación:** cada prompt obliga al agente a revisar `auditorias/APROBACIONES.md` antes de hacer cualquier cosa. Si la sesión anterior no aparece como `APROBADA`, el agente no hace nada.

---

## 2. Preparación (la hace David antes de la Sesión 1, ~10 min)

1. **Android Studio** actualizado, con un emulador **Wear OS Large Round** (API 33 o superior, imagen con Google Play) y otro **Small Round** para revisar el diseño.
2. **Repositorio:** `git init` en esta carpeta y crear el repo en GitHub.
3. **API key:** crear `local.properties` (git lo ignora) con:
   ```
   sdk.dir=C\:\\Users\\CABEZAS\\AppData\\Local\\Android\\Sdk
   OPENAI_API_KEY=sk-...
   OPENAI_MODEL=gpt-6-luna
   OPENAI_WEB_SEARCH=false
   ```
   Presupuesto: 5 USD compartidos entre 3 apps. Pon un límite de gasto en el panel de OpenAI. La búsqueda web queda apagada por defecto porque se cobra aparte por cada búsqueda.
4. **Plugins de Claude Code** (dentro de una sesión de `claude`):
   ```
   /plugin install feature-dev@claude-plugins-official
   /plugin install frontend-design@claude-plugins-official
   /plugin install pr-review-toolkit@claude-plugins-official
   /plugin install security-guidance@claude-plugins-official
   /plugin install commit-commands@claude-plugins-official
   ```
   Si no encuentra el marketplace: `/plugin marketplace add anthropics/claude-plugins-official` y repetir.

   | Plugin | Para qué se usa |
   |---|---|
   | `feature-dev` | Agente `code-architect` para validar la arquitectura antes de programar |
   | `frontend-design` | Criterio de diseño visual |
   | `pr-review-toolkit` | Revisión de calidad, simplificación, errores silenciosos y diseño de tipos |
   | `security-guidance` | Avisa si la API key u otro secreto termina en el código |
   | `commit-commands` | Commits limpios (`/commit`) |

5. Crear `auditorias/APROBACIONES.md` con la línea `PREPARACION: APROBADA`.

---

## 3. Requisitos globales (aplican a las dos sesiones)

### Funcionales
- **Ubicación** con GPS en tiempo real (Fused Location). En la demo se simula con rutas del emulador.
- **Lugares cercanos en cualquier parte del mundo** mediante una cadena de fuentes:
  1. Wikipedia GeoSearch: primero `es`, luego `en`, más el resumen del artículo.
  2. OpenStreetMap Nominatim (geocodificación inversa).
  3. Respaldo: OpenAI Responses API con búsqueda web, **solo si `OPENAI_WEB_SEARCH=true`** (apagada por defecto para cuidar el presupuesto).
  4. Último recurso: dato sobre el municipio o la región.
- **Dato generado con OpenAI**, siempre basado en el contexto real obtenido. La IA cambia el tono, no inventa hechos.
- **Configuración:**
  - Tipos con selección múltiple: Interesante, Histórico, Cómico, Curioso, Gastronómico (mínimo uno).
  - Interruptor **Aleatorio**: en cada dato elige al azar entre los tipos marcados.
  - **Intensidad** en 3 niveles:

    | Nivel | Frecuencia | Largo |
    |---|---|---|
    | Suave | máx. 1 dato / ~10 min | 1 frase |
    | Normal | lugar nuevo, mín. ~3 min entre datos | 2 frases |
    | Intenso | cada lugar nuevo | párrafo corto |

  - Voz (TTS), vibración y pausa, cada una activable o desactivable.
- **Historial** (Room): lugar, texto, tipo, fecha y hora, coordenadas y favorito. Agrupado por día; tocar una entrada abre el dato; se puede marcar favorito y borrar. También evita repetir lugares recientes.
- **Caché** por lugar y **modo demo** sin internet.
- **Ahorro de tokens:** prompt corto, contexto de Wikipedia recortado (máx. ~1200 caracteres), `max_output_tokens` limitado según la intensidad, y ninguna llamada a OpenAI si el dato ya está en caché o el lugar se mostró hace poco.
- **Ícono A:** pin de ubicación + bombillo, adaptive icon, sin texto.

### Arquitectura y calidad
- **Clean Architecture ligera + MVVM + flujo de estado unidireccional** (`StateFlow` con `UiState` inmutable).
- Capas: `domain` (Kotlin puro, sin Android), `data`, `ui`, `di`. Las dependencias apuntan hacia `domain`.
- **Inyección de dependencias manual** con un `AppContainer`.
- **Patrones explícitos y documentados:**
  - Repository.
  - Chain of Responsibility (fuentes de lugares).
  - Strategy (selector de tipo; política de intensidad).
  - Builder (prompt).
  - Observer (Flows).
- **SOLID**, funciones cortas, sin lógica en los Composables, sin `!!`, errores modelados con `Result`/`sealed class`. Nada de `catch` vacíos.
- **Convenciones:**
  - El dominio se nombra en español (`Lugar`, `Dato`, `TipoDato`, `Intensidad`, `Configuracion`).
  - Comentarios mínimos y en español.
- La API key solo se lee de `BuildConfig`, que la toma de `local.properties`.
- Tests unitarios del dominio.
- `./gradlew assembleDebug testDebugUnitTest lint` sin errores al cerrar cada sesión.

### Diseño visual (prioridad máxima)
- **Compose for Wear OS Material 3** con componentes para pantallas redondas: `AppScaffold`/`ScreenScaffold`, `TransformingLazyColumn`, `EdgeButton`, `TimeText`, indicadores de scroll.
- **Design system propio** en `ui/theme`:
  - Fondo negro. Primario teal `#1D9E75` / `#5DCAA5`; acento ámbar `#FAC775` para bombillo y favoritos.
  - Un color por tipo de dato.
  - Tipografía, formas y espaciados como tokens.
- Nada pegado a los bordes redondos. Legible con fuente grande. Contraste AA. Áreas táctiles de al menos 48 dp.
- Todos los estados diseñados: cargando, sin permisos, sin GPS, sin internet, sin lugares, error de IA, historial vacío.
- **Microinteracciones:** entrada animada del dato, patrón de vibración propio, feedback al marcar favorito.
- `@Preview` de cada pantalla en Large Round, Small Round y fuente grande.
- Capturas reales del emulador en cada auditoría (`adb exec-out screencap -p > auditorias/capturas/...png`).

---

## 4. Sesiones

### Sesión 1 — Arquitectura, diseño completo y lógica local

Todo lo que funciona **sin red**: la app queda navegable, con el diseño final, la configuración y el historial reales, y el motor de datos probado con tests. La IA y la ubicación se simulan con fakes detrás de sus interfaces.

**Entregables**
- Proyecto Wear OS standalone, arquitectura por capas, `AppContainer`, modelo de dominio e interfaces.
- Design system, componentes, ícono A y las 4 pantallas (Inicio, Dato, Configuración, Historial) con diseño final y todos sus estados.
- Configuración real (DataStore) e historial real (Room).
- Motor de datos en el dominio: política de intensidad, selector de tipo/aleatorio, anti-repetición y `PromptBuilder`, con tests.
- Fakes de ubicación, lugares e IA para poder navegar y probar todo.
- `docs/arquitectura.md` con diagramas Mermaid y README inicial.

**Cómo probamos la Sesión 1**
- `./gradlew testDebugUnitTest` en verde.
- En el emulador:
  1. Abrir la app; el ícono A aparece en el launcher.
  2. Recorrer las 4 pantallas.
  3. Cambiar los tipos, el aleatorio y la intensidad, cerrar la app, volver a abrirla y comprobar que la configuración se mantiene.
  4. Pedir "Dato ahora" (con el fake) y comprobar que aparece en el historial.
  5. Marcarlo como favorito, borrarlo y ver el estado vacío.
- Revisar en Small Round y con fuente grande que nada se corte.

```text
You are the implementation agent for SESSION 1 of 2 of the "App para SmartWatch" project. Read PLAN_FASES.md fully before doing anything; its global requirements are mandatory.

APPROVAL GATE: Open auditorias/APROBACIONES.md. If it does not contain "PREPARACION: APROBADA", stop immediately and report that the session is blocked. Do nothing else.

GOAL: Deliver everything that works offline: architecture, complete final visual design, real settings and history, and the domain fact engine with tests. Location, places, and AI are fakes behind domain interfaces (the real implementations come in Session 2).

PLAN FIRST: Before writing code, use the feature-dev plugin's code-architect agent to produce the architecture (packages, interfaces, data flow). Use the frontend-design plugin to define the visual direction, adapted to Wear OS round screens. Then implement.

TASKS:
1. Project: standalone Wear OS app (Kotlin, Gradle Kotlin DSL, version catalog, minSdk 30, latest stable Compose for Wear OS Material 3, Room, DataStore). Verify current stable versions in official docs; do not guess. Read OPENAI_API_KEY, OPENAI_MODEL and OPENAI_WEB_SEARCH (boolean, default false) from local.properties into BuildConfig; add local.properties.example. Never hardcode secrets.
2. Architecture: packages domain (pure Kotlin), data, ui, di (manual AppContainer). Domain models in Spanish: Coordenada, Lugar, Dato, TipoDato, Intensidad, Configuracion, EntradaHistorial. Domain interfaces: ProveedorUbicacion, FuenteLugares, GeneradorDatos, RepositorioHistorial, RepositorioConfiguracion.
3. Domain fact engine (pure Kotlin, unit tested):
   - PoliticaIntensidad (Strategy per level): Suave = max 1 fact per ~10 min, 1 sentence; Normal = new place and min ~3 min between facts, 2 sentences; Intenso = every new place, short paragraph. Inject a clock abstraction so it is testable.
   - SelectorTipoDato (Strategy): fixed selection, or random among checked types when Aleatorio is on. At least one type must stay selected.
   - Anti-repetition based on recent history.
   - PromptBuilder (Builder): place context + TipoDato + Intensidad; instructs the model to use only the given context and answer in Spanish.
   - Use cases: ObtenerDatoParaUbicacion, ObtenerDatoAhora, GuardarConfiguracion, ObservarConfiguracion, ObservarHistorial, AlternarFavorito, BorrarEntradaHistorial.
4. Data (real): Room for history (entity, DAO, mappers) and DataStore Preferences for Configuracion. Data (fake, clearly named Fake*): location, places, and fact generator returning realistic sample data for central Pasto, Colombia.
5. Design system in ui/theme: black background, primary teal #1D9E75/#5DCAA5, amber accent #FAC775, one color per TipoDato, typography, shapes, spacing tokens. Reusable components (for example ChipTipoDato, IndicadorIntensidad, TarjetaDato, FilaHistorial, PantallaEstado for empty/error/loading states).
6. Screens, each with a ViewModel exposing an immutable UiState via StateFlow and no logic in composables:
   - Inicio: exploring state, current area, "Dato ahora" action, access to history and settings.
   - Dato: type chip, place name, distance, text, listen, favorite, another fact; animated entrance.
   - Configuracion: multi-select types, Aleatorio toggle, 3-level intensity, voice, vibration, pause.
   - Historial: grouped by day, favorites, delete, designed empty state.
   Use AppScaffold/ScreenScaffold, TimeText, TransformingLazyColumn, EdgeButton, scroll indicators, safe padding from round edges, min 48dp touch targets, large-font support. Design every state (loading, no permission, no GPS, no internet, no places, AI error, empty).
7. Haptics with a custom vibration pattern on a new fact; TextToSpeech (Spanish) when voice is on; pause stops new facts.
8. App icon: adaptive icon of a location pin with a small light bulb badge, no text, readable at 48dp.
9. @Preview for every screen on large round, small round, and large font scale.
10. Unit tests: PoliticaIntensidad, SelectorTipoDato, anti-repetition, PromptBuilder, and the use cases with fakes.
11. docs/arquitectura.md in Spanish with Mermaid diagrams: layers/components, domain class diagram, sequence "user arrives at a place", state diagram of the fact engine. Name every design pattern and where it lives. README.md in Spanish (initial).
12. Run ./gradlew assembleDebug testDebugUnitTest lint and fix every error.
13. Install on the Wear OS emulator and capture screenshots of every screen and key state into auditorias/capturas/s1-*.png (large round and small round).
14. Run pr-review-toolkit agents (code-reviewer, type-design-analyzer, silent-failure-hunter) and fix relevant findings. Grep the repo for "sk-" to confirm no secret is committed.
15. Commit with /commit.

CODE RULES: Clean Architecture + MVVM + unidirectional data flow, SOLID, small functions, no !!, no empty catch blocks, errors modeled with Result/sealed classes, minimal comments in Spanish, no spaghetti code.

AUDIT (mandatory, in SPANISH): write auditorias/SESION-1.md using the template in PLAN_FASES.md section 5, including section "Cómo probarlo" with exact step-by-step instructions for the user to verify everything on their own PC (commands, emulator steps, expected result for each step). Be honest: list deviations and anything unfinished.

STOP after writing the audit. Do not start Session 2. Tell the user the session is ready for the orchestrator's review.
```

---

### Sesión 2 — Ubicación, lugares e IA reales, pulido final y demo

Se reemplazan los fakes por las implementaciones reales, se pule el diseño y se prepara la entrega.

**Entregables**
- Ubicación real con permisos.
- Cadena de fuentes de lugares (Wikipedia `es` → `en` → Nominatim → búsqueda web de OpenAI → región).
- Cliente OpenAI real con caché.
- Modo demo sin internet.
- `demo/ruta-pasto.gpx` y scripts de teletransporte.
- Pulido visual final y README final con guion de demo.

**Cómo probamos la Sesión 2**
- Tests en verde.
- En el emulador:
  1. Conceder el permiso de ubicación.
  2. Cargar `ruta-pasto.gpx` en *Extended Controls → Location → Routes* y darle *Play*: deben salir datos reales en las paradas del centro de Pasto.
  3. Ejecutar el script de teletransporte a Bogotá y París: sale un dato de cada ciudad.
  4. Cambiar a tipo "Cómico" e intensidad "Intenso" y notar la diferencia.
  5. Apagar el wifi del PC con el modo demo activo: la ruta sigue funcionando.
- Leer el README y seguir el guion de demo de principio a fin.

```text
You are the implementation agent for SESSION 2 of 2 (final) of the "App para SmartWatch" project. Read PLAN_FASES.md and auditorias/SESION-1.md before doing anything.

APPROVAL GATE: Open auditorias/APROBACIONES.md. If it does not contain "SESION 1: APROBADA", stop immediately and report that the session is blocked. Do nothing else.

GOAL: Replace the fakes with real implementations (location, places anywhere in the world, OpenAI), polish the visual design to a flawless level, and leave a bulletproof demo and a delivery-ready repository. Keep the Session 1 architecture and design system intact; real implementations must plug in through the existing domain interfaces in AppContainer without changing the domain.

TASKS:
1. Location: FusedLocationProviderClient implementing ProveedorUbicacion as a Flow<Coordenada>. Runtime permission flow with the designed UI states. Foreground use only.
2. Places — Chain of Responsibility implementing FuenteLugares, in this order:
   a) Wikipedia GeoSearch (action=query&list=geosearch, gscoord, gsradius ~500 m) on es.wikipedia.org, then en.wikipedia.org, plus the article summary of the chosen place.
   b) Nominatim reverse geocoding (max 1 request/second, custom User-Agent per its usage policy).
   c) OpenAI Responses API with the web search tool as fallback, ONLY when BuildConfig.OPENAI_WEB_SEARCH is true (read from local.properties, default false). Budget is very tight.
   d) Final fallback: municipality/region fact.
   Descriptive User-Agent for Wikipedia and Nominatim. Choose the closest place not shown recently.
3. AI: real GeneradorDatos with OpenAI (Retrofit or OkHttp + kotlinx.serialization), model from BuildConfig.OPENAI_MODEL, using the existing PromptBuilder. Check the current OpenAI API docs for the request format; do not guess. Timeouts and errors mapped to domain errors. Token budget is very tight (shared 5 USD): keep the prompt short, trim the place context to ~1200 characters, cap max_output_tokens per intensity, never call OpenAI when the fact is cached or the place was shown recently, and during manual testing prefer demo mode/cache over repeated live calls.
4. Cache by place + type + intensity. Demo mode toggle (in settings) that preloads the cache for the demo route so it works without internet.
5. Unit tests: source chain fallback order (with fakes), cache behavior, error mapping. Keep all Session 1 tests green.
6. Demo assets:
   - demo/ruta-pasto.gpx: walking route through central Pasto, Colombia with 6–8 well-known places (for example Plaza de Nariño, Catedral, the Banco de la República cultural area, Plaza del Carnaval). Verify every coordinate against OpenStreetMap; do not invent coordinates.
   - demo/teletransporte.sh and demo/teletransporte.bat: adb emu geo fix jumps (longitude first) to Pasto, Bogotá, Cartagena, and Paris.
7. Visual polish with the frontend-design plugin: audit every screen and state against the design system, remove any hardcoded color/size/spacing, refine animations and transitions, verify WCAG AA contrast, 48dp touch targets, large font, and small round screens. Capture before/after screenshots into auditorias/capturas/s2-*.png.
8. Final README.md in Spanish: description, screenshots, architecture overview (link to docs/arquitectura.md), design patterns and where they live, design system summary, setup (local.properties.example), how to run on the emulator, how to load the GPX route (Extended Controls > Location > Routes), and a 5-minute demo script. Update docs/arquitectura.md diagrams to match the final code.
9. Final quality pass with pr-review-toolkit agents (code-reviewer, code-simplifier, comment-analyzer, silent-failure-hunter, type-design-analyzer) over the whole codebase; fix relevant findings. Remove dead code, TODOs, and debug logs. Delete or isolate the Session 1 fakes so they are only used in tests and previews. Grep for "sk-" to confirm no secret is committed.
10. Run ./gradlew assembleDebug testDebugUnitTest lint and fix every error.
11. Full run on the emulator following the demo script (Pasto route, teleport to Bogotá and Paris, demo mode offline). Then commit with /commit.

CODE RULES: same as PLAN_FASES.md section 3. Domain stays pure Kotlin. No spaghetti code.

AUDIT (mandatory, in SPANISH): write auditorias/SESION-2.md using the template in PLAN_FASES.md section 5. Include: one real generated fact per tested city, the raw prompt sent to OpenAI, the final screenshot list, a checklist of every global requirement (cumplido / parcial / no cumplido), and section "Cómo probarlo" with exact step-by-step instructions for the user to verify everything on their own PC, including the full demo rehearsal.

STOP after writing the audit.
```

---

## 5. Plantilla de auditoría (el agente la escribe en español)

```markdown
# Auditoría — Sesión N: <nombre>

**Fecha:** AAAA-MM-DD
**Estado propuesto:** Lista para revisión / Con pendientes

## 1. Resumen
Qué se hizo en 3–5 líneas.

## 2. Entregables
| Entregable | Estado | Ruta |
|---|---|---|

## 3. Decisiones de arquitectura y diseño
- Patrones aplicados y dónde (archivo/clase).
- Decisiones de diseño visual y por qué.

## 4. Evidencias
- Salida resumida de `./gradlew assembleDebug testDebugUnitTest lint`.
- Tests: total / pasados / fallidos.
- Capturas en `auditorias/capturas/`.
- Hallazgos de pr-review-toolkit y cómo se resolvieron.

## 5. Checklist de criterios
- [ ] Capas separadas; `domain` sin Android
- [ ] Sin lógica en Composables
- [ ] Sin secretos en el código
- [ ] Design system usado (sin valores hardcodeados)
- [ ] Build, tests y lint en verde
- [ ] (criterios propios de la sesión)

## 6. Cómo probarlo
Pasos numerados para verificar todo en el PC: comandos, acciones en el emulador y **resultado esperado** de cada paso.

## 7. Desviaciones y pendientes

## 8. Riesgos para la demo

## 9. Archivos creados / modificados

## 10. Correcciones (solo si no hubo visto bueno)
```

---

## 6. Lo que David le envía al orquestador al final de cada sesión
1. El contenido de `auditorias/SESION-N.md`.
2. Sus notas siguiendo "Cómo probarlo": qué pasó en cada paso y qué falló.
3. Las capturas más importantes.
