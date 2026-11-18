# Documentación de ExplorIA

| Documento | Contenido |
|---|---|
| [arquitectura.md](arquitectura.md) | Capas, diagramas de clases, secuencia y máquina de estados, y patrones de diseño |
| [decisiones-tecnicas.md](decisiones-tecnicas.md) | Registro de decisiones (ADR) con contexto y consecuencias |
| [preparacion-demo.md](preparacion-demo.md) | Cómo dejar el emulador y la caché listos para presentar |
| [guion-exposicion.md](guion-exposicion.md) | Qué hacer y qué decir en cada momento de la demostración |
| [solucion-de-problemas.md](solucion-de-problemas.md) | Errores frecuentes de adb, del emulador y de la app |

Las auditorías de cada sesión de desarrollo y las capturas están en [`../auditorias/`](../auditorias/).

## Página web

La página de presentación del proyecto vive en [`../web/`](../web/): un solo `index.html` con `styles.css`, sin frameworks ni paso de compilación, y con el mismo design system de la app (fondo OLED, teal `#1D9E75` / `#5DCAA5`, ámbar `#FAC775`, títulos en serif). Para verla en local basta con `python -m http.server` dentro de `web/`. Para publicarla en Vercel se importa el repositorio y se elige `web` como *Root Directory*; `web/vercel.json` activa las URLs limpias y la caché de las imágenes.
