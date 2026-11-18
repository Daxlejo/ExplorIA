# Preparación de la demo de ExplorIA

Guía para dejar el emulador listo antes de presentar. Todo se hace en PowerShell, dentro de la carpeta del proyecto, con el emulador *Wear OS Large Round* abierto desde Android Studio (Device Manager).

## 1. Preparar la terminal

```powershell
cd C:\Users\TU_USUARIO\ruta\al\proyecto
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
```

Debe aparecer `emulator-5554   device`. Si no, espera a que el reloj termine de encender.

## 2. Verificar la clave y el saldo

- `local.properties` debe tener `OPENAI_API_KEY` y `OPENAI_MODEL`.
- Revisa que quede saldo en la cuenta de OpenAI: precargar todos los destinos cuesta centavos.

## 3. Instalar desde cero (solo una vez)

```powershell
.\gradlew.bat assembleDebug
& $adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
& $adb -s emulator-5554 shell pm clear com.daxlejo.datoscerca
& $adb -s emulator-5554 shell am start -n com.daxlejo.datoscerca/.ui.MainActivity
```

Concede el permiso de ubicación (*While using app*). Para actualizar la app después **no uses `pm clear`**: borra la caché que se precarga en la sección 4.

