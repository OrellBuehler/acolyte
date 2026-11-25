#!/usr/bin/env bash
# Captures the Google Play phone screenshots from a running emulator. The debug app is
# launched once per screen with extras that sign it in to the demo server and open the
# screen (ScreenshotLaunch.kt), so nothing has to be tapped.
#
# Usage: android.sh <apk> <output dir> [log dir]   (demo server on the host, see demo-server.py)
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
source "$HERE/screens.sh"

APK="$1"
OUT="$2"
LOGS="${3:-$OUT/../android-logs}"
mkdir -p "$LOGS"
adb logcat -c
trap 'adb logcat -d > "$LOGS/logcat.txt" 2>&1 || true' EXIT
PKG="ch.orellbuehler.acolyte"
SERVER="http://10.0.2.2:$DEMO_PORT"
SETTLE_SECONDS="${SETTLE_SECONDS:-12}"

adb install -r "$APK" >/dev/null
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural >/dev/null 2>&1 || true
adb shell cmd uimode night yes

adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo "$@" >/dev/null; }
demo -e command enter
demo -e command clock -e hhmm 0941
demo -e command battery -e level 100 -e plugged false
demo -e command network -e wifi show -e level 4 -e fully true
demo -e command network -e mobile show -e datatype none -e level 4 -e fully true
demo -e command notifications -e visible false

# capture <locale tag> <store locale> <file name> <what to open>
capture() {
  local tag="$1" store="$2" name="$3" open="$4"
  adb shell cmd locale set-app-locales "$PKG" --locales "$tag" >/dev/null
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/.MainActivity" \
    --es screenshot_server "$SERVER" --es screenshot_user "$DEMO_USER" \
    --es screenshot_password "$DEMO_PASSWORD" --es screenshot_open "$open" | tail -3 >&2
  sleep "$SETTLE_SECONDS"
  mkdir -p "$OUT/$store/phone"
  local file="$OUT/$store/phone/$name.png"
  adb exec-out screencap -p > "$file"
  if [ "$(stat -c%s "$file")" -lt 60000 ]; then
    echo "$store/$name looks blank, waiting and capturing again" >&2
    sleep "$SETTLE_SECONDS"
    adb exec-out screencap -p > "$file"
  fi
  echo "$store/$name: $(file -b "$file" | cut -c1-60)"
}

# The first launch after an install is slow, start the app once and throw that launch away.
adb shell am start -W -n "$PKG/.MainActivity" >/dev/null
sleep 15
if adb logcat -d -b crash | grep -q "FATAL EXCEPTION"; then
  echo "::error::the app crashed on launch" >&2
  adb logcat -d -b crash | head -60 >&2
fi

for entry in "en-US|en-US" "de-CH|de-DE"; do
  tag="${entry%%|*}"
  store="${entry##*|}"
  for screen in "${SCREENS[@]}"; do
    capture "$tag" "$store" "${screen%%|*}" "${screen##*|}"
  done
done

demo -e command exit
adb shell cmd uimode night no
