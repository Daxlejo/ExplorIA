# Decisiones técnicas de ExplorIA

Registro breve de las decisiones de diseño más importantes: contexto, decisión y consecuencias.

## ADR-001 — Clean Architecture ligera + MVVM + flujo unidireccional

- **Contexto:** la materia exige responsabilidades bien separadas y código sin espagueti; además, la ubicación, los lugares y la IA debían poder cambiarse (simulados primero, reales después).
- **Decisión:** cuatro capas (`domain`, `data`, `ui`, `di`). `domain` es Kotlin puro y define *puertos* (interfaces); `data` los implementa; cada pantalla tiene un ViewModel que expone un `StateFlow<UiState>` inmutable.
- **Consecuencias:** el dominio se prueba sin Android ni red; se reemplazaron los datos simulados por los reales sin modificar el dominio. A cambio, hay más archivos e interfaces que en un enfoque directo.

