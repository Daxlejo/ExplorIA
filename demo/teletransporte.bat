@echo off
setlocal

REM Script de teletransporte para el emulador de Wear OS.
REM Importante: `adb emu geo fix` recibe primero LONGITUD y luego LATITUD.
REM Uso:
REM   demo\teletransporte.bat [pasto|bogota|cartagena|paris|calle] [serial-emulador]

set DESTINO=%~1
set SERIAL=%~2

if "%DESTINO%"=="" (
    echo Uso: demo\teletransporte.bat [pasto^|bogota^|cartagena^|paris^|calle] [serial-emulador]
    echo.
    echo Destinos disponibles ^(longitud, latitud^):
    echo   pasto      -^> Plaza de Narino, Pasto            ^(-77.278288,  1.214611^)
    echo   bogota     -^> Catedral Primada / Plaza Bolivar  ^(-74.075238,  4.597955^)
    echo   cartagena  -^> Torre del Reloj, Cartagena        ^(-75.549258, 10.423097^)
    echo   paris      -^> Torre Eiffel, Paris               ^(  2.294479, 48.858296^)
    echo   calle      -^> Calle residencial sin hitos       ^(-77.289500,  1.224500^)
    exit /b 1
)

set ADB_BIN=adb
where adb >nul 2>nul
if errorlevel 1 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "ADB_BIN=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    )
)

if "%SERIAL%"=="" (
    for /f "tokens=1" %%D in ('"%ADB_BIN%" devices ^| findstr /R "^emulator-"') do (
        if not defined SERIAL set SERIAL=%%D
    )
)

if "%SERIAL%"=="" (
    set ADB_ARGS=
) else (
    set ADB_ARGS=-s %SERIAL%
)

if /I "%DESTINO%"=="pasto" (
    echo Teletransportando a Pasto ^(Plaza de Narino: lon=-77.278288, lat=1.214611^)...
    "%ADB_BIN%" %ADB_ARGS% emu geo fix -77.278288 1.214611
    goto :eof
)

if /I "%DESTINO%"=="bogota" (
    echo Teletransportando a Bogota ^(Plaza de Bolivar / Catedral Primada: lon=-74.075238, lat=4.597955^)...
    "%ADB_BIN%" %ADB_ARGS% emu geo fix -74.075238 4.597955
    goto :eof
)

if /I "%DESTINO%"=="cartagena" (
    echo Teletransportando a Cartagena ^(Torre del Reloj: lon=-75.549258, lat=10.423097^)...
    "%ADB_BIN%" %ADB_ARGS% emu geo fix -75.549258 10.423097
    goto :eof
)

if /I "%DESTINO%"=="paris" (
    echo Teletransportando a Paris ^(Torre Eiffel: lon=2.294479, lat=48.858296^)...
    "%ADB_BIN%" %ADB_ARGS% emu geo fix 2.294479 48.858296
    goto :eof
)

if /I "%DESTINO%"=="calle" (
    echo Teletransportando a calle residencial sin hitos en Pasto ^(lon=-77.289500, lat=1.224500^)...
    "%ADB_BIN%" %ADB_ARGS% emu geo fix -77.289500 1.224500
    goto :eof
)

echo Destino desconocido: %DESTINO%
echo Usa: pasto, bogota, cartagena, paris o calle.
exit /b 1
