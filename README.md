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

