# Solución de problemas

## El emulador o adb

| Síntoma | Solución |
|---|---|
| `adb devices` no muestra `emulator-5554` | Espera a que el reloj termine de encender; si persiste, `adb kill-server` y `adb start-server` |
| Hay un teléfono conectado y los comandos fallan | Pasa siempre `-s emulator-5554` en los comandos manuales de `adb` |
| El teletransporte no mueve el reloj | `adb emu geo fix` recibe **primero la longitud**; usa los scripts de `demo/` que ya lo hacen bien |
| La ruta GPX no avanza | Revisa que el emulador tenga ubicación habilitada y pulsa *Play route* en *Extended Controls → Location → Routes* |

## La app

| Síntoma | Solución |
|---|---|
| Se queda en *Sin permiso de ubicación* | Toca **Permitir** y elige *While using app*; la pantalla cambia sola a *Explorando* |
| No sale ningún dato automático | Es normal si no hay un lugar emblemático a menos de 150–250 m, si la app está en pausa o si ya se alcanzó el tope por hora (el Modo demo lo quita). Con **Dato ahora** siempre responde |
| Error de IA o dato vacío | Revisa `OPENAI_API_KEY` y `OPENAI_MODEL` en `local.properties` y el saldo de la cuenta |
| Un dato tarda varios segundos | La primera consulta en frío (lugares + IA) tarda de 4 a 7 s; después queda en caché |
| Se perdieron los datos precargados | `adb shell pm clear` y reinstalar con datos limpios borran la caché; actualiza solo con `install -r` |
| Falla la compilación por `sdk.dir` | Revisa `local.properties` (copia de `local.properties.example`) |

## Seguridad

`local.properties` está en `.gitignore`. Si una clave llegara a subirse a Git, **revócala** en el panel de OpenAI y crea una nueva.
