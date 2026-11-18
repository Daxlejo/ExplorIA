# ExplorIA — Datos con IA sobre los lugares que tienes cerca (Wear OS)

**ExplorIA** es una aplicación *standalone* para relojes inteligentes **Wear OS** que acompaña a la persona mientras camina: detecta cuándo pasa cerca de un lugar emblemático (una plaza, una iglesia, un museo, un teatro, un monumento) y le cuenta un dato breve sobre él, generado con inteligencia artificial a partir de información real.

Funciona **en cualquier lugar del mundo**, no solo en Pasto, y permite elegir el tipo de dato (interesante, histórico, cómico, curioso o gastronómico) y la intensidad con la que aparece.

> **Proyecto de la materia Diseño de Software.** Los dos ejes del trabajo son la arquitectura (Clean Architecture + MVVM + patrones de diseño explícitos, sin código espagueti) y el diseño visual (Compose for Wear OS Material 3 con un design system propio, pensado para pantallas redondas).

| | |
|---|---|
| **Plataforma** | Wear OS (Android 11+, `minSdk 30`) |
| **Lenguaje** | Kotlin 2.4 |
| **Interfaz** | Jetpack Compose for Wear OS, Material 3 |
| **IA** | OpenAI Responses API |
| **Pruebas** | 122 tests unitarios, Android Lint sin errores |

## Contenido

1. [Qué hace ExplorIA](#1-qué-hace-exploria)
2. [Capturas](#2-capturas)
3. [Tecnologías utilizadas](#3-tecnologías-utilizadas)
4. [Arquitectura](#4-arquitectura)
5. [Patrones de diseño](#5-patrones-de-diseño-y-dónde-viven)
6. [Cómo se genera un dato](#6-cómo-se-genera-un-dato)
7. [Design system](#7-design-system)
8. [Configuración e instalación](#8-configuración-e-instalación)
9. [Pruebas](#9-pruebas)
10. [Demo: ruta GPX y teletransporte](#10-demo-ruta-gpx-y-teletransporte)
11. [Guion de demostración (5 minutos)](#11-guion-de-demostración-de-5-minutos)
12. [Control de costos de la IA](#12-control-de-costos-de-la-ia)
13. [Estructura del repositorio](#13-estructura-del-repositorio)
14. [Dificultades y decisiones técnicas](#14-dificultades-y-decisiones-técnicas)
15. [Limitaciones conocidas](#15-limitaciones-conocidas)
16. [Fuentes de datos y créditos](#16-fuentes-de-datos-y-créditos)

---

## 1. Qué hace ExplorIA

- **Ubicación en tiempo real.** Usa `FusedLocationProviderClient` solo en primer plano, con el flujo de permisos de Android y estados diseñados para *sin permiso* y *sin GPS*.
- **Solo lugares emblemáticos.** Un lugar califica si tiene artículo en Wikipedia o etiquetas patrimoniales/turísticas en OpenStreetMap, y si está a menos de 150–250 m. Una calle, una casa o una tienda nunca disparan un dato automático.
- **Datos generados con IA, sin inventar.** El modelo recibe únicamente el texto real del lugar como contexto y responde en español, en texto plano.
- **Tipo de dato configurable** (selección múltiple, mínimo uno): *Interesante, Histórico, Cómico, Curioso, Gastronómico*, con un interruptor **Aleatorio**.
- **Tres intensidades** con ventana móvil de 60 minutos:

  | Nivel | Datos automáticos | Largo | Tope de tokens de salida |
  |---|---|---|---|
  | Suave | máx. 1 por hora | 1 frase | 60 |
  | Normal | máx. 2 por hora | 2 frases | 110 |
  | Intenso | máx. 4 por hora | párrafo corto | 220 |

- **Botón "Dato ahora".** Ignora los topes por hora y siempre responde: si no hay ningún hito cerca, cuenta algo del municipio.
- **Historial persistente (Room).** Agrupado por día (*Hoy, Ayer, fecha*), con favoritos, filtro *Solo favoritos* y borrado. También evita repetir lugares mostrados hace poco.
- **Voz y vibración.** Lectura en voz alta con TextToSpeech en español y vibración con patrón propio al llegar un dato.
- **Caché y Modo demo.** Un dato ya generado nunca se vuelve a pedir a OpenAI, y el Modo demo permite recorrer la ruta de Pasto sin internet y sin tope por hora.

---

