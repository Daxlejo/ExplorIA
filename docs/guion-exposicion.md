# Guion de la exposición de ExplorIA

Cada bloque indica **qué haces** y **qué dices**. No hace falta memorizarlo palabra por palabra. Duración total: 5 a 6 minutos.

## 0. Presentación (30 s)

**Dices:** «Esta es *ExplorIA*, una app para smartwatch con Wear OS. Mientras caminas, detecta cuándo pasas cerca de un lugar emblemático y te cuenta un dato sobre él, generado con inteligencia artificial: interesante, histórico, curioso, gastronómico o cómico, con la intensidad que elijas. Como no tenemos un reloj físico, la corremos en el emulador oficial de Wear OS y le simulamos el GPS.»

## 1. Ruta por el centro de Pasto (1 min 30 s)

**Haces:** en *Extended Controls → Routes*, **Play** a `ruta-pasto.gpx`.

**Dices:** «Cargamos una caminata por el centro de Pasto con 8 hitos verificados en OpenStreetMap. Al inicio pasamos por una tienda de barrio y no sale nada: la app solo habla de lugares emblemáticos, a menos de 150–250 metros. Una calle, una casa o una tienda nunca disparan un dato.»

Cuando salte un dato: «Vibra y muestra el tipo de dato, el lugar, la distancia y el texto. Puedo escucharlo, marcarlo como favorito o pedir otro.»

## 2. Cualquier lugar del mundo (1 min)

**Haces:** `.\demo\teletransporte.bat bogota` → **Dato ahora**; luego `cartagena` y `paris`.

**Dices:** «La app no está atada a Pasto. En Bogotá encuentra la Catedral Primada; en Cartagena, la Torre del Reloj; en París, la Torre Eiffel, con el dato en español. Funciona en cualquier punto del planeta porque los lugares se buscan en vivo en Wikipedia y OpenStreetMap.»

