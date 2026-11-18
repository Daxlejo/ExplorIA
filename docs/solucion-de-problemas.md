# Solución de problemas

## El emulador o adb

| Síntoma | Solución |
|---|---|
| `adb devices` no muestra `emulator-5554` | Espera a que el reloj termine de encender; si persiste, `adb kill-server` y `adb start-server` |
| Hay un teléfono conectado y los comandos fallan | Pasa siempre `-s emulator-5554` en los comandos manuales de `adb` |
| El teletransporte no mueve el reloj | `adb emu geo fix` recibe **primero la longitud**; usa los scripts de `demo/` que ya lo hacen bien |
| La ruta GPX no avanza | Revisa que el emulador tenga ubicación habilitada y pulsa *Play route* en *Extended Controls → Location → Routes* |

