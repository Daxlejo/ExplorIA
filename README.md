# Datos Cerca — app para Wear OS

App de reloj que, según dónde estés, te cuenta un dato sobre lo que tienes cerca. Tú eliges el tipo de dato (interesante, histórico, cómico, curioso, gastronómico) y la intensidad (cuántos llegan por hora y qué tan largos son). Solo habla de lugares emblemáticos: plazas, iglesias, museos, monumentos.

Proyecto de la materia **Diseño de Software**: la arquitectura y el diseño visual pesan tanto como la funcionalidad.

> **Estado: Sesión 1 de 2.** Todo funciona sin red. La ubicación, los lugares y la IA están simulados con datos de muestra del centro de Pasto (Nariño, Colombia). La Sesión 2 conecta GPS real, Wikipedia/OpenStreetMap y OpenAI.

## Qué hace hoy

- **Inicio:** muestra que la app está explorando y cerca de qué lugar estás. Botón "Dato ahora".
- **Dato:** tipo, lugar, distancia y texto, con entrada animada. Se puede escuchar, marcar como favorito, borrar o pedir otro.
- **Configuración:** tipos (selección múltiple, mínimo uno), aleatorio, intensidad en tres niveles, voz, vibración y pausa. Se guarda con DataStore.
- **Historial:** agrupado por día, con filtro de favoritos. Se guarda con Room.
- **Dato automático:** una caminata simulada recorre el centro de Pasto. Al pasar a menos de 200 m de un lugar emblemático, el motor da un dato si cabe en el límite por hora: Suave 1, Normal 2, Intenso 4 (ventana móvil de una hora). El modo demo quita ese límite. La ruta pasa también por una tienda de barrio, que no es relevante y nunca da datos.
- **"Dato ahora":** ignora el límite por hora y no lo gasta. Si ya viene un dato automático en camino, no se pide otro: se espera ese.

## Requisitos

- JDK 17 o superior (probado con JDK 21).
- Android SDK con la plataforma API 37 y Build Tools 36.
- Un emulador Wear OS (Large Round y, para revisar el diseño, Small Round).

## Configuración

Copia `local.properties.example` como `local.properties` y completa los valores. Git ignora ese archivo.

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
OPENAI_API_KEY=
OPENAI_MODEL=
OPENAI_WEB_SEARCH=false
```

En Windows la ruta del SDK lleva las barras escapadas (`C\:\\Users\\...`). En la Sesión 1 la clave de OpenAI no se usa.

## Cómo ejecutar

```bash
./gradlew assembleDebug testDebugUnitTest lint
```

Con el emulador Wear OS abierto:

```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.daxlejo.datoscerca/.ui.MainActivity
```

Si hay un teléfono conectado por ADB, usa siempre `-s emulator-XXXX` para apuntar al reloj.

### Ver los estados de error (solo en debug)

Los fakes pueden simular cada problema. Se elige al abrir la app:

```bash
adb -s emulator-5554 shell am start -S -n com.daxlejo.datoscerca/.ui.MainActivity --es escenario SIN_INTERNET
```

Valores: `NORMAL`, `SIN_PERMISO`, `SIN_GPS`, `SIN_INTERNET`, `SIN_LUGARES`, `ERROR_IA`.

## Arquitectura

Clean Architecture ligera + MVVM + flujo de datos unidireccional, con inyección de dependencias manual.

```
app/src/main/java/com/daxlejo/datoscerca/
├── domain/   Kotlin puro: modelos, errores, puertos, motor de datos y casos de uso
├── data/     Room, DataStore, adaptadores de plataforma y fakes
├── ui/       theme (design system), componentes, pantallas y ViewModels
└── di/       AppContainer y FabricaViewModels
```

Patrones aplicados: Repository, Strategy (intensidad y selección de tipo), Decorator (modo demo), Factory, Builder (prompt), Observer (Flows), State, Adapter y el contrato listo para Chain of Responsibility. El detalle, con diagramas, está en [docs/arquitectura.md](docs/arquitectura.md).

## Design system

Vive en `ui/theme`:

- **Color:** fondo negro, teal `#1D9E75` / `#5DCAA5` como primario y ámbar `#FAC775` reservado al bombillo y a los favoritos. Cada tipo de dato tiene su color.
- **Tipografía:** el nombre del lugar en serif, como la placa de un sitio; el dato en sans.
- **Tokens:** espaciados, tamaños (toque mínimo de 48 dp), formas y duraciones de animación.

Las pantallas usan `AppScaffold`, `ScreenScaffold`, `TransformingLazyColumn`, `EdgeButton`, `TimeText` e indicador de scroll, con márgenes proporcionales al tamaño de la pantalla redonda.

## Tests

```bash
./gradlew testDebugUnitTest
```

Cubren el límite por hora de cada intensidad (ventana móvil), el modo demo, el dato manual, el filtro de relevancia, la generación única, el selector de tipo, la anti-repetición, el `PromptBuilder`, los casos de uso, el motor automático, el historial (incluido un error de lectura) y su agrupado, todo con dobles en memoria.

## Capturas

En `auditorias/capturas/` (`s1-grande-*` y `s1-pequena-*`).
