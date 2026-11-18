#!/usr/bin/env bash
set -euo pipefail

# Script de teletransporte para el emulador de Wear OS.
# Importante: `adb emu geo fix` recibe primero LONGITUD y luego LATITUD.
# Uso:
#   ./demo/teletransporte.sh <pasto|bogota|cartagena|paris|calle> [serial-emulador]

DESTINO="${1:-}"
SERIAL="${2:-}"

if [[ -z "$DESTINO" ]]; then
  echo "Uso: ./demo/teletransporte.sh <pasto|bogota|cartagena|paris|calle> [serial-emulador]"
  echo ""
  echo "Destinos disponibles (longitud, latitud):"
  echo "  pasto      -> Plaza de Nariño, Pasto            (-77.278288,  1.214611)"
  echo "  bogota     -> Catedral Primada / Plaza Bolívar  (-74.075238,  4.597955)"
  echo "  cartagena  -> Torre del Reloj, Cartagena        (-75.549258, 10.423097)"
  echo "  paris      -> Torre Eiffel, París               (  2.294479, 48.858296)"
  echo "  calle      -> Calle residencial sin hitos       (-77.289500,  1.224500)"
  exit 1
fi

ADB_BIN="adb"
if ! command -v adb >/dev/null 2>&1; then
  if [[ -n "${LOCALAPPDATA:-}" && -x "${LOCALAPPDATA}/Android/Sdk/platform-tools/adb.exe" ]]; then
    ADB_BIN="${LOCALAPPDATA}/Android/Sdk/platform-tools/adb.exe"
  elif [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/platform-tools/adb" ]]; then
    ADB_BIN="${ANDROID_HOME}/platform-tools/adb"
  fi
fi

if [[ -z "$SERIAL" ]]; then
  SERIAL="$("$ADB_BIN" devices | awk '/^emulator-/{print $1; exit}' | tr -d '\r')"
fi

ADB_ARGS=()
if [[ -n "$SERIAL" ]]; then
  ADB_ARGS=(-s "$SERIAL")
fi

case "${DESTINO,,}" in
  pasto)
    echo "Teletransportando a Pasto (Plaza de Nariño: lon=-77.278288, lat=1.214611)..."
    "$ADB_BIN" "${ADB_ARGS[@]}" emu geo fix -77.278288 1.214611
    ;;
  bogota|bogotá)
    echo "Teletransportando a Bogotá (Plaza de Bolívar / Catedral Primada: lon=-74.075238, lat=4.597955)..."
    "$ADB_BIN" "${ADB_ARGS[@]}" emu geo fix -74.075238 4.597955
    ;;
  cartagena)
    echo "Teletransportando a Cartagena (Torre del Reloj: lon=-75.549258, lat=10.423097)..."
    "$ADB_BIN" "${ADB_ARGS[@]}" emu geo fix -75.549258 10.423097
    ;;
  paris|parís)
    echo "Teletransportando a París (Torre Eiffel: lon=2.294479, lat=48.858296)..."
    "$ADB_BIN" "${ADB_ARGS[@]}" emu geo fix 2.294479 48.858296
    ;;
  calle)
    echo "Teletransportando a calle residencial sin hitos en Pasto (lon=-77.289500, lat=1.224500)..."
    "$ADB_BIN" "${ADB_ARGS[@]}" emu geo fix -77.289500 1.224500
    ;;
  *)
    echo "Destino desconocido: $DESTINO"
    echo "Usa: pasto, bogota, cartagena, paris o calle."
    exit 1
    ;;
esac
