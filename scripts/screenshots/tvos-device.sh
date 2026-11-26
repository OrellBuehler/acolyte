#!/usr/bin/env bash
# Creates and boots an Apple TV 4K simulator at 3840 x 2160 and prints its UDID. The runner's
# tvOS 27.0 runtime is listed but does not always boot, so every tvOS runtime is tried in turn.
set -uo pipefail

TYPE=$(xcrun simctl list devicetypes --json | jq -r '
  [.devicetypes[] | select(.name | test("^Apple TV 4K")) | select(.name | test("1080p") | not) | .identifier] | last')
if [ -z "$TYPE" ] || [ "$TYPE" = "null" ]; then
  echo "No Apple TV 4K device type available" >&2
  xcrun simctl list devicetypes >&2
  exit 1
fi

for runtime in $(xcrun simctl list runtimes --json | jq -r '.runtimes[] | select(.platform == "tvOS") | .identifier' | sort); do
  echo "Trying $TYPE on $runtime" >&2
  id=$(xcrun simctl create "Acolyte Apple TV" "$TYPE" "$runtime") || continue
  if xcrun simctl boot "$id" >&2 && xcrun simctl bootstatus "$id" -b >&2; then
    echo "$id"
    exit 0
  fi
  xcrun simctl shutdown "$id" >&2 || true
  xcrun simctl delete "$id" >&2 || true
done
echo "No tvOS runtime booted" >&2
xcrun simctl list runtimes >&2
exit 1
