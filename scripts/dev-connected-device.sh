#!/usr/bin/env bash

set -euo pipefail

SERIAL="${1:-}"
APP_ID="com.example.spotifish"
ACTIVITY="com.example.spotifish/com.example.musicapp.MainActivity"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb is not installed or not on PATH."
  exit 1
fi

if [[ -z "${SERIAL}" ]]; then
  mapfile -t DEVICES < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
  if [[ "${#DEVICES[@]}" -eq 0 ]]; then
    echo "No connected ADB device found."
    exit 1
  fi
  SERIAL="${DEVICES[0]}"
fi

echo "Using device: ${SERIAL}"
rtk ./gradlew installDebug
adb -s "${SERIAL}" shell am force-stop "${APP_ID}" >/dev/null 2>&1 || true
adb -s "${SERIAL}" shell am start -n "${ACTIVITY}"

echo
echo "App installed and launched on ${SERIAL}."
echo "For Compose hot iteration, run the app from Android Studio in Debug mode and enable Live Edit."
