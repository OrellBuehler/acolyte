#!/usr/bin/env bash
# Captures the App Store screenshots from a booted simulator that has the Debug app
# installed. The app is launched once per screen with launch arguments that sign it in
# to the demo server and open the screen (ScreenshotLaunch.swift), so nothing has to be
# tapped. The demo server runs on the host, which the simulator shares.
#
# Usage: ios.sh <simulator udid> <output dir>
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
source "$HERE/screens.sh"

DEVICE_ID="$1"
OUT="$2"
BUNDLE_ID="ch.orellbuehler.acolyte"
SERVER="http://127.0.0.1:$DEMO_PORT"
SETTLE_SECONDS="${SETTLE_SECONDS:-10}"
WIDTH=1320
HEIGHT=2868

xcrun simctl status_bar "$DEVICE_ID" override \
  --time "9:41" --dataNetwork wifi --wifiMode active --wifiBars 3 \
  --cellularMode active --cellularBars 4 --batteryState charged --batteryLevel 100
xcrun simctl ui "$DEVICE_ID" appearance dark

# capture <language> <apple locale> <store locale> <file name> <what to open>
capture() {
  local language="$1" apple="$2" store="$3" name="$4" open="$5"
  xcrun simctl terminate "$DEVICE_ID" "$BUNDLE_ID" 2>/dev/null || true
  xcrun simctl launch "$DEVICE_ID" "$BUNDLE_ID" \
    -AppleLanguages "($language)" -AppleLocale "$apple" \
    -acolyte_screenshot_server "$SERVER" -acolyte_screenshot_user "$DEMO_USER" \
    -acolyte_screenshot_password "$DEMO_PASSWORD" -acolyte_screenshot_open "$open" >/dev/null
  sleep "$SETTLE_SECONDS"
  mkdir -p "$OUT/$store/iphone-6.9"
  local file="$OUT/$store/iphone-6.9/$name.png"
  xcrun simctl io "$DEVICE_ID" screenshot "$file"
  if [ "$(stat -f%z "$file")" -lt 150000 ]; then
    echo "$store/$name looks blank, waiting and capturing again" >&2
    sleep "$SETTLE_SECONDS"
    xcrun simctl io "$DEVICE_ID" screenshot "$file"
  fi
  local w h
  w="$(sips -g pixelWidth "$file" | awk '/pixelWidth/ {print $2}')"
  h="$(sips -g pixelHeight "$file" | awk '/pixelHeight/ {print $2}')"
  echo "$store/$name: ${w}x${h}"
  if [ "$w" != "$WIDTH" ] || [ "$h" != "$HEIGHT" ]; then
    echo "::error::$store/$name is ${w}x${h}, the 6.9 inch slot wants ${WIDTH}x${HEIGHT}" >&2
    exit 1
  fi
}

# The first launch after an install is slow enough to be captured blank, so start the
# app once and throw that launch away.
xcrun simctl launch "$DEVICE_ID" "$BUNDLE_ID" >/dev/null
sleep 20

for entry in "en|en_US|en-US" "de|de_CH|de-DE"; do
  IFS='|' read -r language apple store <<< "$entry"
  for screen in "${SCREENS[@]}"; do
    capture "$language" "$apple" "$store" "${screen%%|*}" "${screen##*|}"
  done
done

xcrun simctl ui "$DEVICE_ID" appearance light
xcrun simctl status_bar "$DEVICE_ID" clear
