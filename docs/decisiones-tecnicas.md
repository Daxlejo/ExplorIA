# Decisiones técnicas de ExplorIA

Registro breve de las decisiones de diseño más importantes: contexto, decisión y consecuencias.

## ADR-001 — Clean Architecture ligera + MVVM + flujo unidireccional

- **Contexto:** la materia exige responsabilidades bien separadas y código sin espagueti; además, la ubicación, los lugares y la IA debían poder cambiarse (simulados primero, reales después).
- **Decisión:** cuatro capas (`domain`, `data`, `ui`, `di`). `domain` es Kotlin puro y define *puertos* (interfaces); `data` los implementa; cada pantalla tiene un ViewModel que expone un `StateFlow<UiState>` inmutable.
- **Consecuencias:** el dominio se prueba sin Android ni red; se reemplazaron los datos simulados por los reales sin modificar el dominio. A cambio, hay más archivos e interfaces que en un enfoque directo.

## ADR-002 — Wear OS con Jetpack Compose y Material 3

- **Contexto:** la presentación original era en un smartwatch y no hay reloj físico disponible.
- **Decisión:** app *standalone* para Wear OS (`minSdk 30`) con Compose for Wear OS Material 3, ejecutada en el emulador oficial de Google.
- **Consecuencias:** se usan componentes diseñados para pantallas redondas (`ScreenScaffold`, `TransformingLazyColumn`, `EdgeButton`, `TimeText`). La demo depende del emulador y de simular el GPS.

