# Auditoría — Sesión 3: Corrección de calidad (sin metacomentarios, contexto por sección y variedad al repetir)

**Fecha:** 2026-11-18
**Estado propuesto:** Lista para revisión

## 1. Resumen

Se corrigió el defecto de calidad detectado en el emulador al consultar municipios o lugares con introducciones cortas (como Ipiales y Popayán con enfoque `HISTORICO`) y al pulsar repetidamente «Dato ahora» / «Otro dato» en el mismo punto:

1. **Prompt sin metacomentarios (`PromptBuilder`):** se reescribió `PromptBuilder.SISTEMA` para prohibir explícitamente que el modelo mencione «el contexto», «la información», «no se menciona» o «no aparece», ordenándole contar el hecho real más interesante disponible como una afirmación natural a quien camina cuando falte material del enfoque pedido, e incluyendo la directiva opcional `"No repitas estos datos (cuenta otro hecho distinto): ..."` al pedir una variante nueva.
2. **Contexto enriquecido por secciones (`ExtractorContextoWikipedia`, `FuenteWikipedia`, `FuenteMunicipio`):** cuando el extracto inicial (`exintro=1`) tiene menos de `400` caracteres o corresponde a un municipio, la capa de datos solicita el artículo completo en texto plano (`prop=extracts&explaintext=1` sin `exintro`) y selecciona las secciones que coinciden con el enfoque (`Historia`/`History`, `Gastronomía`/`Cocina`/`Cuisine`, `Cultura`/`Turismo`/`Patrimonio`), cayendo al resumen inicial si no existen secciones y recortando siempre en límite de palabra a `MAX_CONTEXTO_CHARS` (`1200`).
3. **Guardia de salida (`GuardiaSalidaIa`, `GeneradorDatosOpenAi`):** tras cada respuesta de OpenAI se rechazan textos con frases meta («el contexto», «no menciona», «no se menciona», «no aparece», «no hay información», «no se indica»); ante un rechazo se reintenta una única vez con `TipoDato.INTERESANTE` para el mismo lugar y, si vuelve a fallar, se devuelve `Resultado.Fallo(ErrorDato.ErrorIa(...))` sin guardar nunca el texto rechazado en caché.
4. **Variedad al repetir (`CacheDatos`, `GeneradorDatosConCache`, `ProducirDato`):** `CacheDatos` admite hasta `3` variantes por clave (`lugar.id|tipo|intensidad`), sirve primero una variante no mostrada en el historial reciente sin llamar a OpenAI, solicita una nueva variante pasando los textos previos en el prompt solo cuando todas las cacheadas ya se mostraron y hay menos de `3`, y rota a otro `TipoDato` marcado cuando las `3` variantes ya fueron mostradas. Mantiene lectura retrocompatible del formato antiguo de un solo `String`.
5. **Limpieza automática de caché (`CacheDatos`):** al cargar `files/cache_datos.json` desde disco se descartan las entradas o variantes que coinciden con `GuardiaSalidaIa` y se persiste el archivo limpio, eliminando los textos defectuosos previos de Ipiales y Popayán sin borrar la precarga válida del modo demo.

El dominio (`com.daxlejo.datoscerca.domain`) permanece 100 % en Kotlin puro (`0` `import android.*`), sin `!!` y sin bloques `catch` vacíos.

## 2. Entregables

| Entregable | Estado | Ruta |
|---|---|---|
| 1. `PromptBuilder.SISTEMA` sin metacomentarios + soporte de `.evitar(previos)` | Hecho | `app/src/main/java/com/daxlejo/datoscerca/domain/motor/PromptBuilder.kt`, `app/src/test/java/com/daxlejo/datoscerca/domain/motor/PromptBuilderTest.kt` |
| 2. Contexto enriquecido por sección (`Historia`, `Gastronomía`, `Cultura`/`Turismo`) en `FuenteWikipedia` y `FuenteMunicipio` | Hecho | `app/src/main/java/com/daxlejo/datoscerca/data/lugares/{ExtractorContextoWikipedia,FuenteWikipedia,FuenteMunicipio}.kt`, `di/AppContainer.kt` |
| 3. Guardia de salida `GuardiaSalidaIa` + reintento único con `TipoDato.INTERESANTE` en `GeneradorDatosOpenAi` | Hecho | `app/src/main/java/com/daxlejo/datoscerca/data/ia/{GuardiaSalidaIa,GeneradorDatosOpenAi}.kt` |
| 4. Hasta 3 variantes por clave en `CacheDatos`, retrocompatibilidad JSON y rotación de `TipoDato` en `ProducirDato` | Hecho | `app/src/main/java/com/daxlejo/datoscerca/data/ia/CacheDatos.kt`, `domain/motor/ProducirDato.kt`, `domain/puerto/GeneradorDatos.kt` |
| 5. Limpieza de entradas con metacomentarios al cargar `CacheDatos` desde disco | Hecho | `app/src/main/java/com/daxlejo/datoscerca/data/ia/CacheDatos.kt` |
| 6. Compilación, 130 tests unitarios en verde, 0 errores de Lint y verificación en emulador (Popayán e Ipiales x3 con `HISTORICO`) | Hecho | `auditorias/capturas/s3-01-config-historico.png` a `s3-08-historial.png` |
| 7. Documentación (`ADR-008` en `docs/decisiones-tecnicas.md`, `README.md`) y esta auditoría | Hecho | `docs/decisiones-tecnicas.md`, `README.md`, `auditorias/SESION-3.md` |

## 3. Decisiones de arquitectura y diseño

- **Dominio intacto y puro (`ADR-008`):** la extracción de secciones de Wikipedia (`ExtractorContextoWikipedia`) y la guardia de salida (`GuardiaSalidaIa`) viven íntegramente en la capa `data` (`data/lugares` y `data/ia`). El dominio continúa recibiendo únicamente `Lugar.contexto` y `Resultado<String>`.
- **Reconocimiento jerárquico de secciones MediaWiki y encabezados planos:** cuando MediaWiki devuelve `prop=extracts&explaintext=1` sin `exintro=1`, las secciones como `== Historia ==` suelen agrupar subsecciones nivel 3 (`=== Época precolombina ===`, `=== Fundación ===`). `ExtractorContextoWikipedia` conserva la categoría de la sección padre de nivel 2 para que todas sus subsecciones históricas, gastronómicas o culturales se incluyan, y soporta también encabezados planos (`exsectionformat=plain`).
- **Presupuesto estricto de tokens:** se limita a un máximo de `3` variantes por clave (`lugar.id|tipo|intensidad`), nunca más de `1` llamada a OpenAI por toque del usuario (más como máximo `1` reintento si se dispara `GuardiaSalidaIa`), contexto recortado en frontera de palabra a `1200` caracteres e instrucción de no repetición acotada a las variantes previas del lugar.

## 4. Evidencias

### Compilación, tests unitarios y lint
- Comando ejecutado: `./gradlew assembleDebug testDebugUnitTest lint`
- Resultado: **`BUILD SUCCESSFUL`**
- **Tests unitarios:** 18 clases de prueba, **130 tests ejecutados, 130 pasados, 0 fallidos** (8 tests nuevos para prohibición de metacomentarios, textos previos en prompt, extracción de secciones con JSON simulado en `FuenteWikipedia` y `FuenteMunicipio`, guardia de salida con reintento a `INTERESANTE`, hasta 3 variantes en caché, rotación de `TipoDato`, retrocompatibilidad de formato JSON antiguo y limpieza de caché al iniciar).
- **Android Lint:** **0 errores, 0 advertencias**.

### Prompt crudo antes y después (`PromptBuilder`)

#### Antes (Sesión 2)
**`instructions` (`Prompt.sistema`):**
```text
Eres un guía local que cuenta datos breves sobre lugares. Usa únicamente los hechos del CONTEXTO: no inventes nombres, fechas ni cifras. Si el contexto no alcanza para el enfoque pedido, cuenta lo que sí aparece. Responde siempre en español, en texto plano, sin listas, títulos ni emojis.
```

**`input` (`Prompt.usuario`, con solo el párrafo inicial `exintro=1` y sin control de variantes):**
```text
Lugar: Ipiales
Enfoque: un hecho histórico, con las fechas o personajes que aparezcan en el contexto
Largo: Responde en exactamente dos frases.
CONTEXTO:
Ipiales es un municipio colombiano situado en el departamento de Nariño, sobre la frontera con Ecuador. Se destaca por el Santuario de Las Lajas, considerado una de las maravillas arquitectónicas de Colombia.
```

#### Después (Sesión 3)
**`instructions` (`Prompt.sistema`, 388 caracteres):**
```text
Eres un guía local para un caminante. Usa únicamente los hechos del CONTEXTO: no inventes nombres, fechas ni cifras. Si falta material para el enfoque pedido, cuenta el dato real más interesante disponible como afirmación natural. Prohibido decir qué no sabes o usar frases como "el contexto", "la información", "no se menciona" o "no aparece". Español, texto plano, sin listas ni emojis.
```

**`input` (`Prompt.usuario`, con contexto enriquecido por sección `Historia` y exclusión de variante previa al repetir en el mismo lugar):**
```text
Lugar: Ipiales
Enfoque: un hecho histórico, su origen, fechas o personajes vinculados al lugar
Largo: Responde en exactamente dos frases.
No repitas estos datos (cuenta otro hecho distinto): Según los cronistas españoles, el puente de Rumichaca fue el puesto más septentrional del Imperio inca, conquistado a los pastos a comienzos del siglo XVI. Cruza el río Carchi, en
CONTEXTO:
Ipiales es un municipio colombiano situado en el departamento de Nariño. Antes de la llegada de los europeos, la comarca de Ipiales estaba habitada por los pastos, quienes resistieron hacia 1480 la incursión de los incas comandados por Huayna Cápac. Su historia quedó registrada por el cronista Pedro Cieza de León en 1545. En 1585 fray Andrés Moreno de Zúñiga y Pedro de Henao fundaron la población sobre el asentamiento del cacique Ipial. Uno de los grandes acontecimientos festivos e históricos de Ipiales es el Carnaval Multicolor de la Frontera (Carnaval de Negros y Blancos), dedicado a celebrar la diversidad cultural y racial andina.
```

### Datos reales generados en el emulador (`HISTORICO` + «Dato ahora» / «Otro dato» x3)

#### Popayán (`adb -s emulator-5554 emu geo fix -76.6064 2.4419`)
1. **Intento 1 (`Parque Caldas`, `HISTORICO`, `NORMAL`):**
   > «El Parque Caldas es la plaza principal de Popayán y se encuentra en su Centro Histórico, donde convergen los edificios más representativos de la ciudad. En 2010 se entregaron las obras de ampliación, ornato y embellecimiento de su área peatonal, financiadas por FONADE y la Alcaldía de Popayán.»
2. **Intento 2 (`Catedral basílica de Nuestra Señora de la Asunción`, `HISTORICO`, `NORMAL`):**
   > «La Catedral Basílica Metropolitana de Nuestra Señora de la Asunción es el principal templo de la arquidiócesis de Popayán y sede del arzobispo. Los frecuentes movimientos telúricos de la región han obligado a modificar su construcción.»
3. **Intento 3 (`Palacio Arzobispal de Popayán`, `HISTORICO`, `NORMAL`):**
   > «La creación de la diócesis de Popayán, el 1 de septiembre de 1546, impulsó la construcción de una casa para los obispos en los solares al sur de la plaza. El terremoto de 1736 devastó el conjunto, incluida la humilde residencia episcopal.»

#### Ipiales (`adb -s emulator-5554 emu geo fix -77.6431 0.8303`)
1. **Intento 1 (`Ipiales`, `HISTORICO`, `NORMAL`):**
   > «Antes de la llegada de los europeos, la comarca de Ipiales estaba habitada por los pastos, quienes resistieron hacia 1480 la incursión de los incas, comandados por Huayna Cápac. Su historia quedó registrada también por el cronista Pedro Cieza de León, quien escribió sobre ellos en 1545.»
2. **Intento 2 (`Ipiales`, `HISTORICO`, `NORMAL` — variante 1 de `wikipedia-en`):**
   > «Según los cronistas españoles, el puente de Rumichaca fue el puesto más septentrional del Imperio inca, conquistado a los pastos a comienzos del siglo XVI. Cruza el río Carchi, en la frontera con Ecuador, a tres kilómetros de Ipiales.»
3. **Intento 3 (`Ipiales`, `HISTORICO`, `NORMAL` — variante 2 en el mismo lugar sin repetir la anterior):**
   > «Uno de los grandes acontecimientos festivos de Ipiales es el Carnaval de Negros y Blancos, dedicado a celebrar la diversidad racial. Durante la fiesta, la gente se pinta unos a otros con maquillaje y con careoca, un jabón blanco.»

### Lista de capturas de la Sesión 3 (`auditorias/capturas/s3-*.png`)

| Archivo | Qué muestra |
|---|---|
| `s3-01-config-historico.png` | Configuración con únicamente el tipo **Histórico** seleccionado |
| `s3-02-popayan-1.png` | Popayán — Intento 1 (`Parque Caldas`, dato histórico real sin metacomentarios) |
| `s3-03-popayan-2.png` | Popayán — Intento 2 (`Catedral basílica de Nuestra Señora de la Asunción`, dato histórico distinto) |
| `s3-04-popayan-3.png` | Popayán — Intento 3 (`Palacio Arzobispal de Popayán`, dato histórico con fechas `1546` y `1736`) |
| `s3-05-ipiales-1.png` | Ipiales — Intento 1 (`Ipiales`, resistencia de los pastos en `1480` ante Huayna Cápac y crónica de `1545`) |
| `s3-06-ipiales-2.png` | Ipiales — Intento 2 (`Ipiales`, puente de Rumichaca como frontera septentrional del Imperio inca en el siglo XVI) |
| `s3-07-ipiales-3.png` | Ipiales — Intento 3 (`Ipiales`, segunda variante en el mismo lugar sobre la tradición histórica del Carnaval) |
| `s3-08-historial.png` | Historial mostrando las entradas distintas generadas en Ipiales y Popayán |

## 5. Checklist de requisitos de la Sesión 3 y globales

| Requisito | Estado | Evidencia / Ubicación |
|---|---|---|
| 1. `PromptBuilder.SISTEMA` prohíbe mencionar «el contexto», «la información», «no se menciona», «no aparece» y pide afirmación natural | **Cumplido** | `PromptBuilder.kt`, `PromptBuilderTest.kt` |
| 2. Contexto enriquecido por sección (`Historia`, `Gastronomía`, `Cultura`/`Turismo`) cuando el extracto es `< 400` chars o es un municipio, recortado a `1200` chars | **Cumplido** | `ExtractorContextoWikipedia.kt`, `FuenteWikipedia.kt`, `FuenteMunicipio.kt`, `FuentesRealesYLimitadorTest.kt` |
| 3. Guardia de salida rechaza frases meta, reintenta 1 vez con `TipoDato.INTERESANTE` y nunca cachea textos rechazados | **Cumplido** | `GuardiaSalidaIa.kt`, `GeneradorDatosOpenAi.kt`, `CacheDatos.kt`, `GeneradorDatosYCacheTest.kt` |
| 4. Hasta 3 variantes en caché por clave, `"No repitas estos datos"` en prompt, rotación a otro `TipoDato` y retrocompatibilidad JSON | **Cumplido** | `CacheDatos.kt`, `ProducirDato.kt`, `PromptBuilder.kt`, `GeneradorDatosYCacheTest.kt`, `ObtenerDatoTest.kt` |
| 5. Limpieza automática de entradas con metacomentarios al cargar `CacheDatos` desde disco | **Cumplido** | `CacheDatos.cargarDesdeArchivo()`, `GeneradorDatosYCacheTest.kt` |
| 6. `./gradlew assembleDebug testDebugUnitTest lint` en verde + verificación con `install -r` (sin `pm clear`) en Popayán e Ipiales x3 | **Cumplido** | 130 tests pasados, 0 errores de Lint, capturas `s3-01` a `s3-08` |
| 7. `ADR-008` en `docs/decisiones-tecnicas.md`, `README.md` actualizado y `auditorias/SESION-3.md` | **Cumplido** | `docs/decisiones-tecnicas.md`, `README.md`, `auditorias/SESION-3.md` |
| Dominio en Kotlin puro (`0` `import android.*`), sin `!!`, sin `catch` vacíos, errores con `Resultado`/`ErrorDato` | **Cumplido** | Verificado en todo `app/src/main/java/com/daxlejo/datoscerca/domain` |

## 6. Cómo probarlo

Sigue estos pasos en tu PC para verificar todas las correcciones de la Sesión 3:

1. **Ejecutar compilación, tests unitarios y lint:**
   ```powershell
   .\gradlew.bat assembleDebug testDebugUnitTest lint
   ```
   - **Resultado esperado:** `BUILD SUCCESSFUL`, 130 tests unitarios en verde y 0 errores de lint.

2. **Instalar conservando los datos y la caché (`install -r` sin `pm clear`):**
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
   & $adb -s emulator-5554 shell am start -n com.daxlejo.datoscerca/.ui.MainActivity
   ```
   - **Resultado esperado:** al iniciar, `CacheDatos` conserva todas las entradas válidas de la ruta demo de Pasto y descarta automáticamente de `files/cache_datos.json` cualquier entrada antigua con metacomentarios.

3. **Configurar el tipo Histórico:**
   - En la pantalla **Inicio**, toca el botón de **Configuración** (engranaje arriba a la derecha).
   - En **Tipos de dato**, deja marcado **Histórico** (y verifica al final de la lista que **Modo demo** esté desactivado para consultar las fuentes reales). Vuelve a **Inicio**.

4. **Verificar Popayán (`-76.6064, 2.4419`) pulsando 3 veces «Dato ahora» / «Otro dato»:**
   ```powershell
   & $adb -s emulator-5554 emu geo fix -76.6064 2.4419
   ```
   - Toca **Dato ahora** (intento 1), luego baja al final de la pantalla **Dato** y toca **Otro dato** dos veces más (intentos 2 y 3).
   - **Resultado esperado:** los 3 datos son hechos históricos reales de Popayán, ninguno menciona «el contexto» ni lo que falta, y los 3 textos son distintos entre sí.

5. **Verificar Ipiales (`-77.6431, 0.8303`) pulsando 3 veces «Dato ahora» / «Otro dato»:**
   ```powershell
   & $adb -s emulator-5554 emu geo fix -77.6431 0.8303
   ```
   - Toca **Dato ahora** (intento 1) y luego **Otro dato** dos veces más (intentos 2 y 3).
   - **Resultado esperado:** aunque Ipiales sea el único lugar en el radio, los 3 intentos devuelven 3 variantes históricas reales y distintas sobre Ipiales (época prehispánica de los pastos e incursión de Huayna Cápac en 1480, puente de Rumichaca en el siglo XVI, y tradición histórica de sus fiestas), sin repetir el mismo texto ni emitir metacomentarios.

## 7. Archivos creados / modificados en la Sesión 3

- **Creados:**
  - `app/src/main/java/com/daxlejo/datoscerca/data/lugares/ExtractorContextoWikipedia.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/ia/GuardiaSalidaIa.kt`
  - `auditorias/SESION-3.md`
  - `auditorias/capturas/s3-01-config-historico.png` a `s3-08-historial.png` (8 capturas)
- **Modificados:**
  - `app/src/main/java/com/daxlejo/datoscerca/domain/motor/PromptBuilder.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/domain/motor/ProducirDato.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/domain/puerto/GeneradorDatos.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/lugares/FuenteWikipedia.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/lugares/FuenteMunicipio.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/ia/GeneradorDatosOpenAi.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/data/ia/CacheDatos.kt`
  - `app/src/main/java/com/daxlejo/datoscerca/di/AppContainer.kt`
  - `app/src/test/java/com/daxlejo/datoscerca/domain/motor/PromptBuilderTest.kt`
  - `app/src/test/java/com/daxlejo/datoscerca/domain/casouso/ObtenerDatoTest.kt`
  - `app/src/test/java/com/daxlejo/datoscerca/data/FuentesRealesYLimitadorTest.kt`
  - `app/src/test/java/com/daxlejo/datoscerca/data/GeneradorDatosYCacheTest.kt`
  - `docs/decisiones-tecnicas.md`, `README.md`
