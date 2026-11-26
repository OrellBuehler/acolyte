#!/usr/bin/env bash
# Captures the Google Play Android TV screenshots (1920 x 1080) from a running Android TV
# emulator. The debug TV app is launched once per screen with extras that sign it in to the demo
# server and open the screen (TvScreenshotLaunch.kt). A screen counts as settled when two frames
# taken two seconds apart are identical (pictures loaded, nothing animating); one DPAD_DOWN then
# moves the focus from the tab bar onto the first card.
#
# Before capturing, the script walks the app once (every tab with the remote, then the pushed
# pages, a person and a genre) and fails at the end when the app process crashed or hung, so
# this run is also the smoke test of the TV app. The screenshots are taken first.
#
# Usage: androidtv.sh <apk> <output dir> [log dir]   (demo server on the host, see demo-server.py)
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
source "$HERE/screens.sh"

APK="$1"
OUT="$2"
LOGS="${3:-$OUT/../androidtv-logs}"
mkdir -p "$LOGS"
adb logcat -c
trap 'adb logcat -d > "$LOGS/logcat.txt" 2>&1 || true' EXIT
PKG="ch.orellbuehler.acolyte"
ACTIVITY="$PKG/ch.orellbuehler.acolyte.tv.TvActivity"
SERVER="http://10.0.2.2:$DEMO_PORT"
MIN_SECONDS="${MIN_SECONDS:-8}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-60}"
FAILED=0

# The tab screens are reached with the remote from Home (RIGHT, <n> times) like a person would; the
# others are opened by the launch intent. Then the keys that move the focus onto a card or a button.
declare -A TAB_STEPS=(
  ["02-explore"]=1
  ["05-calendar"]=4
  ["06-requests"]=3
)
declare -A KEYS=(
  ["01-home"]="DOWN"
  ["02-explore"]="DOWN DOWN"
  ["03-title"]=""
  ["04-new-and-popular"]="DOWN"
  ["05-calendar"]="DOWN DOWN DOWN DOWN"
  ["06-requests"]="DOWN DOWN DOWN"
)

adb install -r "$APK" >/dev/null
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell wm size
adb shell wm density

launch() {
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$ACTIVITY" \
    --es screenshot_server "$SERVER" --es screenshot_user "$DEMO_USER" \
    --es screenshot_password "$DEMO_PASSWORD" --es screenshot_open "$1" | tail -3 >&2
}

check_crash() {
  if adb logcat -d -b crash | grep -q "FATAL EXCEPTION"; then
    echo "::error::the app crashed ($1)" >&2
    adb logcat -d -b crash | head -80 >&2
    adb logcat -b crash -c
    FAILED=1
  fi
  if adb logcat -d | grep -q "ANR in $PKG"; then
    echo "::error::the app did not respond ($1)" >&2
    FAILED=1
  fi
}

shot() {
  adb exec-out screencap -p > "$1"
}

wait_settled() {
  local tmp="$LOGS/settle.png" waited=0
  sleep "$MIN_SECONDS"
  while [ "$waited" -lt "$MAX_WAIT_SECONDS" ]; do
    shot "$tmp"
    local before
    before="$(md5sum < "$tmp")"
    sleep 2
    shot "$tmp"
    if [ "$before" = "$(md5sum < "$tmp")" ]; then
      echo "settled after $((MIN_SECONDS + waited))s"
      return 0
    fi
    waited=$((waited + 2))
  done
  echo "::warning::$1 did not settle within ${MAX_WAIT_SECONDS}s, capturing anyway" >&2
}

press() {
  for key in $1; do
    adb shell input keyevent "KEYCODE_DPAD_$key"
    sleep 1
  done
}

# capture <locale tag> <store locale> <file name> <what to open>
capture() {
  local tag="$1" store="$2" name="$3" open="$4"
  adb shell cmd locale set-app-locales "$PKG" --locales "$tag" >/dev/null
  local steps="${TAB_STEPS[$name]-}"
  if [ -n "$steps" ]; then
    launch home
    wait_settled "$store/$name home"
    press "$(printf 'RIGHT %.0s' $(seq 1 "$steps"))"
  else
    launch "$open"
  fi
  wait_settled "$store/$name"
  press "${KEYS[$name]}"
  wait_settled "$store/$name"
  mkdir -p "$OUT/$store"
  local file="$OUT/$store/$name.png"
  shot "$file"
  if [ "$(stat -c%s "$file")" -lt 60000 ]; then
    echo "$store/$name looks blank, capturing again" >&2
    sleep "$MIN_SECONDS"
    shot "$file"
  fi
  echo "$store/$name: $(file -b "$file" | cut -c1-60)"
  check_crash "$store/$name"
}

# The first launch after an install is slow, start the app once and throw that launch away.
adb shell am start -W -n "$ACTIVITY" >/dev/null
sleep 20
check_crash "first launch"

for entry in "en-US|en-US" "de-CH|de-DE"; do
  tag="${entry%%|*}"
  store="${entry##*|}"
  for screen in "${TV_SCREENS[@]}"; do
    capture "$tag" "$store" "${screen%%|*}" "${screen##*|}"
  done
done

adb shell cmd locale set-app-locales "$PKG" --locales en-US >/dev/null
launch home
sleep 15
for tab in 1 2 3 4 5 6; do
  adb shell input keyevent KEYCODE_DPAD_RIGHT
  sleep 3
  check_crash "tab $tab"
done
for open in popular downloads settings person:5000 genre:movie:18 genre:show:18 title:show:920001; do
  launch "$open"
  sleep 10
  adb shell input keyevent KEYCODE_DPAD_DOWN
  sleep 2
  adb shell input keyevent KEYCODE_BACK
  sleep 2
  check_crash "$open"
done

if [ "$FAILED" -ne 0 ]; then
  exit 1
fi
