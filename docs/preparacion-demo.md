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

## 4. Precargar la caché

Con el **Modo demo apagado**, para que Pasto también se genere con IA y quede guardado:

1. En Configuración deja los tipos que vas a mostrar y la intensidad **Normal**.
2. Para cada destino ejecuta el script, espera 3 segundos, vuelve a Inicio (desliza a la derecha) y toca **Dato ahora**:

```powershell
.\demo\teletransporte.bat pasto
.\demo\teletransporte.bat bogota
.\demo\teletransporte.bat cartagena
.\demo\teletransporte.bat paris
.\demo\teletransporte.bat calle
```

3. Cambia a **Cómico + Intenso** y repite `pasto` y `paris`. La caché guarda cada combinación *lugar + tipo + intensidad* por separado.

La primera consulta de cada destino tarda de 4 a 7 segundos; después responde al instante.

