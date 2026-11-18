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

## ADR-003 — Cadena de responsabilidad para obtener lugares

- **Contexto:** la app debe funcionar en cualquier lugar del mundo y no existe una única fuente que cubra todo.
- **Decisión:** `CadenaFuentesLugares` prueba en orden Wikipedia (es), Wikipedia (en), Nominatim, la búsqueda web de OpenAI (solo si está activada) y, al final, el municipio. El respaldo del municipio solo se usa con el botón manual **Dato ahora**.
- **Consecuencias:** agregar o quitar una fuente no afecta a las demás. Nominatim se limita a 1 petición por segundo con `User-Agent` descriptivo, como exige su política de uso.

## ADR-004 — Hablar solo de lugares emblemáticos

- **Contexto:** OpenStreetMap devuelve direcciones, tiendas y casas; un dato sobre cada una sería ruido e interrumpiría a la persona.
- **Decisión:** `FiltroRelevancia` solo deja pasar lugares con artículo en Wikipedia o etiquetas turísticas/históricas/patrimoniales, a 150–250 m. Se esperan 1–2 datos por hora según los sitios por los que se pase.
- **Consecuencias:** una calle común nunca dispara un dato automático. Lugares sin artículo ni etiquetas no se detectan.

