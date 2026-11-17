# Arquitectura — Datos Cerca (Wear OS)

Estado: **Sesión 1**. Todo funciona sin red; la ubicación, los lugares y la IA son *fakes* detrás de las interfaces del dominio. La Sesión 2 conecta las implementaciones reales sin tocar el dominio.

## 1. Idea general

Clean Architecture ligera + MVVM + flujo de datos unidireccional, en un solo módulo Gradle (`app`) con cuatro paquetes:

| Paquete | Qué contiene | Depende de |
|---|---|---|
| `domain` | Modelos, errores, puertos (interfaces), motor de datos y casos de uso. Kotlin puro: ningún `import android.*` | nada |
| `data` | Room, DataStore, adaptadores de plataforma (reloj, vibración, voz) y los fakes | `domain` |
| `ui` | Design system, componentes, pantallas y ViewModels | `domain` |
| `di` | `AppContainer` y `FabricaViewModels`: el único lugar que conoce las clases concretas | todos |

Las dependencias apuntan siempre hacia `domain`. `ui` nunca importa `data`.

## 2. Capas y componentes

```mermaid
flowchart TB
    subgraph UI["ui — Compose for Wear OS Material 3"]
        direction TB
        Pantallas["Pantallas<br/>Inicio · Dato · Configuración · Historial"]
        VM["ViewModels<br/>StateFlow de UiState inmutable"]
        Tema["theme + componentes<br/>design system"]
        Pantallas -- eventos --> VM
        VM -- estado --> Pantallas
        Pantallas --> Tema
    end

    subgraph DI["di — inyección manual"]
        Contenedor["AppContainer"]
        Fabrica["FabricaViewModels"]
    end

    subgraph DOMINIO["domain — Kotlin puro"]
        direction TB
        Casos["Casos de uso<br/>ObtenerDatoAhora · ObtenerDatoParaUbicacion<br/>Guardar/ObservarConfiguracion<br/>ObservarHistorial · ObservarEntrada<br/>AlternarFavorito · BorrarEntradaHistorial"]
        Motor["Motor de datos<br/>MotorAutomatico · ProducirDato<br/>PoliticaIntensidad · SelectorTipoDato<br/>FiltroRepeticion · PromptBuilder"]
        Puertos["Puertos<br/>ProveedorUbicacion · FuenteLugares · GeneradorDatos<br/>RepositorioHistorial · RepositorioConfiguracion<br/>Reloj · Vibrador · Narrador"]
        Modelos["Modelos y errores<br/>Lugar · Dato · Configuracion · ErrorDato · Resultado"]
        Casos --> Motor
        Casos --> Puertos
        Motor --> Puertos
        Motor --> Modelos
    end

    subgraph DATA["data"]
        direction TB
        Room["historial<br/>Room: Entity · DAO · Mapper"]
        Store["configuracion<br/>DataStore Preferences"]
        Plataforma["plataforma<br/>RelojSistema · VibradorAndroid · NarradorTts"]
        Fakes["fake (Sesión 1)<br/>FakeProveedorUbicacion · FakeFuenteLugares<br/>FakeGeneradorDatos · DatosPasto"]
    end

    VM --> Casos
    VM --> Motor
    Room -. implementa .-> Puertos
    Store -. implementa .-> Puertos
    Plataforma -. implementa .-> Puertos
    Fakes -. implementa .-> Puertos
    Contenedor --> DATA
    Contenedor --> DOMINIO
    Fabrica --> VM
```

## 3. Modelo del dominio

```mermaid
classDiagram
    class Coordenada {
        +Double latitud
        +Double longitud
        +distanciaMetrosA(otra) Double
    }
    class Lugar {
        +String id
        +String nombre
        +int distanciaMetros
        +String contexto
        +String fuente
    }
    class Dato {
        +String texto
        +long generadoEnMs
    }
    class EntradaHistorial {
        +long id
        +boolean favorito
    }
    class Configuracion {
        +Set~TipoDato~ tipos
        +boolean aleatorio
        +boolean vozActiva
        +boolean vibracionActiva
        +boolean pausa
        +boolean modoDemo
        +puedeDesmarcar(tipo) boolean
        +conTipoAlternado(tipo) Configuracion
    }
    class TipoDato {
        <<enumeration>>
        INTERESANTE
        HISTORICO
        COMICO
        CURIOSO
        GASTRONOMICO
    }
    class Intensidad {
        <<enumeration>>
        SUAVE
        NORMAL
        INTENSO
    }
    class PoliticaIntensidad {
        <<interface>>
        +Intensidad intensidad
        +FormatoDato formato
        +evaluar(ultimoDatoMs, lugarEsNuevo) Decision
    }
    class SelectorTipoDato {
        <<interface>>
        +seleccionar(tipos, anterior) TipoDato
    }
    class FiltroRepeticion {
        +idsAExcluir(recientes, ahoraMs) Set~String~
    }
    class PromptBuilder {
        +lugar(l) PromptBuilder
        +tipo(t) PromptBuilder
        +formato(f) PromptBuilder
        +build() Prompt
    }
    class FuenteLugares {
        <<interface>>
        +buscarCercano(coordenada, radioMetros, excluir) Resultado~Lugar~
    }
    class GeneradorDatos {
        <<interface>>
        +generar(solicitud) Resultado~String~
    }
    class ProveedorUbicacion {
        <<interface>>
        +ubicaciones() Flow~EstadoUbicacion~
        +ubicacionActual() Resultado~Coordenada~
    }
    class RepositorioHistorial {
        <<interface>>
        +observar() Flow
        +guardar(dato) Resultado~EntradaHistorial~
        +alternarFavorito(id) Resultado
        +borrar(id) Resultado
    }
    class RepositorioConfiguracion {
        <<interface>>
        +observar() Flow~Configuracion~
        +actualizar(cambio) Resultado
    }

    Lugar --> Coordenada
    Dato --> Lugar
    Dato --> TipoDato
    Dato --> Intensidad
    EntradaHistorial --> Dato
    Configuracion --> TipoDato
    Configuracion --> Intensidad
    PoliticaIntensidad <|.. PoliticaSuave
    PoliticaIntensidad <|.. PoliticaNormal
    PoliticaIntensidad <|.. PoliticaIntenso
    SelectorTipoDato <|.. SelectorEnOrden
    SelectorTipoDato <|.. SelectorAleatorio
    ProducirDato --> GeneradorDatos
    ProducirDato --> RepositorioHistorial
    ProducirDato --> SelectorTipoDato
    ProducirDato --> PromptBuilder
    ObtenerDatoParaUbicacion --> FuenteLugares
    ObtenerDatoParaUbicacion --> PoliticaIntensidad
    ObtenerDatoParaUbicacion --> FiltroRepeticion
    ObtenerDatoParaUbicacion --> ProducirDato
    ObtenerDatoAhora --> ProveedorUbicacion
    ObtenerDatoAhora --> FuenteLugares
    ObtenerDatoAhora --> ProducirDato
    MotorAutomatico --> ProveedorUbicacion
    MotorAutomatico --> RepositorioConfiguracion
    MotorAutomatico --> ObtenerDatoParaUbicacion
```

### Errores

No se lanzan excepciones para fallos esperados. Todo puerto devuelve `Resultado<T>` (`Exito` o `Fallo(ErrorDato)`), y `ErrorDato` es una jerarquía sellada: `SinPermiso` y `SinGps` (que son `ErrorUbicacion`), `SinInternet`, `SinLugares`, `FuenteNoDisponible`, `ErrorIa`, `EnPausa`, `VozNoDisponible` y `Almacenamiento`. La interfaz tiene un estado diseñado para cada uno (`ui/util/Textos.kt`).

Omitir un dato automático no es un error: `ObtenerDatoParaUbicacion` devuelve `Exploracion`, que distingue `NuevoDato`, `Omitido(motivo)` y `Fallo(error)`.

## 4. Secuencia: la persona llega a un lugar

```mermaid
sequenceDiagram
    autonumber
    participant U as ProveedorUbicacion
    participant M as MotorAutomatico
    participant C as ObtenerDatoParaUbicacion
    participant F as FuenteLugares
    participant P as PoliticaIntensidad
    participant D as ProducirDato
    participant IA as GeneradorDatos
    participant H as RepositorioHistorial
    participant VM as InicioViewModel
    participant UI as Pantallas

    U-->>M: Disponible(coordenada)
    M->>M: ¿se movió 25 m o toca reintentar?
    M->>C: invoke(coordenada)
    C->>H: recientes()
    C->>F: buscarCercano(coordenada, 80 m)
    F-->>C: Lugar
    C->>C: FiltroRepeticion: ¿lugar nuevo?
    C->>P: evaluar(últimoDato, lugarEsNuevo)
    alt Rechazar
        P-->>C: Rechazar(motivo)
        C-->>M: Omitido(motivo)
        Note over M: sigue en Explorando, sin gastar IA
    else Permitir
        P-->>C: Permitir
        C->>D: invoke(lugar, configuración, recientes)
        D->>D: SelectorTipoDato + PromptBuilder
        D->>IA: generar(solicitud)
        IA-->>D: texto
        D->>H: guardar(dato)
        H-->>D: EntradaHistorial
        D->>D: AvisadorDato: vibrar y leer en voz alta
        D-->>C: Exito(entrada)
        C-->>M: NuevoDato(entrada)
        M-->>VM: nuevosDatos (SharedFlow)
        VM-->>UI: EfectoInicio.AbrirDato(id)
        UI->>UI: navegar a Dato con entrada animada
    end
```

El dato manual ("Dato ahora") sigue el mismo tramo final (`ProducirDato`), pero lo inicia `ObtenerDatoAhora`: no espera el intervalo de la intensidad, busca en un radio mayor (500 m) y, si todo lo cercano ya se mostró, repite el lugar más cercano.

## 5. Estados del motor de datos

```mermaid
stateDiagram-v2
    [*] --> Iniciando
    Iniciando --> Explorando: llega una ubicación
    Iniciando --> Bloqueado: sin permiso / sin GPS
    Iniciando --> EnPausa: pausa activada

    Explorando --> Evaluando: se movió ≥ 25 m o venció la espera de reintento
    Evaluando --> Explorando: Omitido (nada cerca, lugar repetido, intervalo)
    Evaluando --> Explorando: Fallo (queda como aviso y se reintenta en 30 s)
    Evaluando --> DatoNuevo: Permitir + IA + historial
    DatoNuevo --> Explorando: publica en nuevosDatos

    Explorando --> Bloqueado: se pierde el permiso o el GPS
    Bloqueado --> Explorando: vuelve la ubicación
    Explorando --> EnPausa: pausa activada
    Bloqueado --> EnPausa: pausa activada
    EnPausa --> Explorando: reanudar (reevalúa la ubicación actual)
```

`Evaluando` y `DatoNuevo` son pasos internos de `MotorAutomatico.explorar`; hacia afuera el motor publica `Iniciando`, `Explorando`, `EnPausa` y `Bloqueado` (`EstadoMotor`).

### Reglas de intensidad

| Nivel | Cuándo permite un dato automático | Largo | Tope de tokens |
|---|---|---|---|
| Suave | Han pasado ≥ 10 min desde el último dato. Si el lugar más cercano ya se mostró, usa el siguiente | 1 frase | 60 |
| Normal | El lugar es nuevo y han pasado ≥ 3 min | 2 frases | 110 |
| Intenso | El lugar es nuevo | Párrafo corto | 220 |

"Lugar nuevo" significa que no aparece en el historial de los últimos 30 minutos (`FiltroRepeticion`).

## 6. Patrones de diseño y dónde viven

| Patrón | Dónde | Para qué |
|---|---|---|
| **Repository** | `domain/puerto/RepositorioHistorial`, `RepositorioConfiguracion` → `data/historial/RepositorioHistorialRoom`, `data/configuracion/RepositorioConfiguracionDataStore` | El dominio no sabe si los datos están en Room o en DataStore |
| **Strategy** | `domain/motor/PoliticaIntensidad` (`PoliticaSuave`, `PoliticaNormal`, `PoliticaIntenso`) | Cada nivel decide cuándo toca un dato y qué tan largo es |
| **Strategy** | `domain/motor/SelectorTipoDato` (`SelectorEnOrden`, `SelectorAleatorio`) | Selección fija o al azar entre los tipos marcados |
| **Factory** | `FabricaPoliticas`, `FabricaSelectores`, `di/FabricaViewModels` | Elegir la estrategia según la configuración y construir ViewModels |
| **Builder** | `domain/motor/PromptBuilder` | Armar el prompt paso a paso: lugar, tipo y formato |
| **Observer** | `Flow`/`StateFlow`/`SharedFlow`: configuración, historial, ubicación, `MotorAutomatico.estado` y `nuevosDatos`, `uiState` de cada ViewModel | La interfaz reacciona a los cambios sin consultar |
| **State** | `EstadoMotor` + `MotorAutomatico`; `UiState` sellados de cada pantalla | Estados explícitos; los imposibles no se pueden representar |
| **Adapter** | `data/plataforma/RelojSistema`, `VibradorAndroid`, `NarradorTts`; `data/historial/EntradaMapper` | Traducir APIs de Android y filas de Room a los puertos y modelos del dominio |
| **Chain of Responsibility** | `domain/puerto/FuenteLugares` (contrato preparado) | En la Sesión 2 una `CadenaFuentesLugares` encadenará Wikipedia → Nominatim → búsqueda web → región. `Fallo(SinLugares)` significa "pasa a la siguiente" |
| **Inyección de dependencias manual** | `di/AppContainer` | Un solo punto de ensamblaje; cambiar fakes por reales son tres líneas |
| **MVVM + UDF** | `ui/*/…ViewModel` + `…Pantalla` | Eventos suben, estado inmutable baja; los composables no deciden nada |

## 7. Flujo de datos en la interfaz

- Cada pantalla tiene un par `XRuta` (conecta el ViewModel) y `XPantalla` (sin estado, solo pinta). Las previews usan `XPantalla` directamente.
- El `UiState` es una jerarquía sellada e inmutable expuesta como `StateFlow` y recogida con `collectAsStateWithLifecycle`.
- Los efectos de una sola vez (abrir un dato, cerrar la pantalla tras borrar) viajan por un `Channel`, no por el estado.
- Vibrar y leer en voz alta no son efectos de interfaz: son puertos del dominio (`Vibrador`, `Narrador`) que invoca `AvisadorDato`, igual para el dato manual y el automático.
- `MotorAutomatico` es un singleton del contenedor. Lo ejecuta `MainActivity` con `repeatOnLifecycle(STARTED)`: solo trabaja con la app en primer plano.

## 8. Qué cambia en la Sesión 2

Solo `AppContainer`:

```kotlin
private val proveedorUbicacion: ProveedorUbicacion = FakeProveedorUbicacion(escenarioFake)   // → Fused Location
private val fuenteLugares: FuenteLugares = FakeFuenteLugares(escenarioFake)                  // → CadenaFuentesLugares
private val generadorDatos: GeneradorDatos = FakeGeneradorDatos(escenarioFake)               // → OpenAI + caché
```

Lo que ya quedó preparado en el dominio:

- `FuenteLugares.buscarCercano(coordenada, radioMetros, excluir)` permite componer una cadena y elegir "el más cercano no mostrado".
- `SolicitudGeneracion.claveCache` (lugar + tipo + intensidad) para un decorador de caché.
- `Prompt.maxTokensSalida` y el recorte de contexto a 1200 caracteres en `PromptBuilder`.
- `ErrorDato.SinInternet`, `FuenteNoDisponible` y `ErrorIa` para mapear timeouts y respuestas 5xx.
- `EstadoUbicacion.NoDisponible(motivo)` para el flujo de permisos.
- `Configuracion.modoDemo`, ya persistido.

## 9. Decisiones que conviene conocer

- **El dato manual puede repetir un lugar.** Si todo lo cercano ya se mostró, `ObtenerDatoAhora` repite el más cercano en lugar de dejar a la persona sin respuesta.
- **Un dato automático no interrumpe otra pantalla.** Si la persona está en Historial o Configuración, el dato vibra, se guarda y queda en el historial; solo se abre solo cuando está en Inicio.
- **Dos radios de búsqueda** (`RadioBusqueda`): 80 m para "llegaste" (automático) y 500 m para "qué hay cerca" (manual).
- **`Lugar.contexto` no se guarda en el historial.** Solo sirve para generar; al leer una entrada llega vacío.
