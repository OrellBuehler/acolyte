#!/usr/bin/env bash
# Captures the Apple TV App Store screenshots from a booted Apple TV 4K simulator that has
# the Debug AcolyteTV app installed. The app is launched once per screen with launch
# arguments that sign it in to the demo server and open the screen (TVScreenshotLaunch.swift),
# so nothing has to be pressed. A screen counts as settled when the demo server has stopped
# receiving requests (the pictures are all fetched), then the frame gets a moment to cross-fade.
#
# Usage: tvos.sh <simulator udid> <output dir> <demo server log>
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
source "$HERE/screens.sh"

DEVICE_ID="$1"
OUT="$2"
DEMO_LOG="$3"
BUNDLE_ID="ch.orellbuehler.acolyte"
SERVER="http://127.0.0.1:$DEMO_PORT"
MIN_REQUESTS="${MIN_REQUESTS:-8}"
QUIET_SECONDS="${QUIET_SECONDS:-4}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-90}"
RENDER_SECONDS="${RENDER_SECONDS:-4}"
WIDTH=3840
HEIGHT=2160

xcrun simctl ui "$DEVICE_ID" appearance dark

wait_settled() {
  local base="$1" last=-1 stable=0 waited=0 count
  while [ "$waited" -lt "$MAX_WAIT_SECONDS" ]; do
    sleep 1
    waited=$((waited + 1))
    count=$(wc -l < "$DEMO_LOG")
    if [ "$count" -eq "$last" ]; then
      stable=$((stable + 1))
    else
      stable=0
      last="$count"
    fi
    if [ $((count - base)) -ge "$MIN_REQUESTS" ] && [ "$stable" -ge "$QUIET_SECONDS" ]; then
      echo "settled after ${waited}s ($((count - base)) requests)"
      return 0
    fi
  done
  echo "::warning::$2 did not settle within ${MAX_WAIT_SECONDS}s, capturing anyway" >&2
}

# capture <language> <apple locale> <store locale> <file name> <what to open>
capture() {
  local language="$1" apple="$2" store="$3" name="$4" open="$5" base
  xcrun simctl terminate "$DEVICE_ID" "$BUNDLE_ID" 2>/dev/null || true
  base=$(wc -l < "$DEMO_LOG")
  xcrun simctl launch "$DEVICE_ID" "$BUNDLE_ID" \
    -AppleLanguages "($language)" -AppleLocale "$apple" \
    -acolyte_screenshot_server "$SERVER" -acolyte_screenshot_user "$DEMO_USER" \
    -acolyte_screenshot_password "$DEMO_PASSWORD" -acolyte_screenshot_open "$open" >/dev/null
  wait_settled "$base" "$store/$name"
  sleep "$RENDER_SECONDS"
  mkdir -p "$OUT/$store"
  local file="$OUT/$store/$name.png"
  xcrun simctl io "$DEVICE_ID" screenshot "$file"
  local w h
  w="$(sips -g pixelWidth "$file" | awk '/pixelWidth/ {print $2}')"
  h="$(sips -g pixelHeight "$file" | awk '/pixelHeight/ {print $2}')"
  if [ "$w" = "1920" ] && [ "$h" = "1080" ]; then
    echo "$store/$name: simulator captured ${w}x${h}, scaling to ${WIDTH}x${HEIGHT}"
    sips -z "$HEIGHT" "$WIDTH" "$file" > /dev/null
    w="$WIDTH"
    h="$HEIGHT"
  fi
  echo "$store/$name: ${w}x${h}"
  if [ "$w" != "$WIDTH" ] || [ "$h" != "$HEIGHT" ]; then
    echo "::error::$store/$name is ${w}x${h}, the Apple TV slot wants ${WIDTH}x${HEIGHT}" >&2
    exit 1
  fi
}

# The first launch after an install is slow enough to be captured half drawn, so start the
# app once and throw that launch away.
xcrun simctl launch "$DEVICE_ID" "$BUNDLE_ID" >/dev/null
sleep 20

for entry in "en|en_US|en-US" "de|de_CH|de-DE"; do
  IFS='|' read -r language apple store <<< "$entry"
  for screen in "${TV_SCREENS[@]}"; do
    capture "$language" "$apple" "$store" "${screen%%|*}" "${screen##*|}"
  done
done

xcrun simctl ui "$DEVICE_ID" appearance light
