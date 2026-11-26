#!/usr/bin/env bash
# Creates and boots an Apple TV 4K simulator at 3840 x 2160 and prints its UDID. The runner's
# tvOS 27.0 runtime is listed but does not always boot, so every tvOS runtime and device type is tried in turn, up to three rounds.
set -uo pipefail

TYPES=$(xcrun simctl list devicetypes --json | jq -r '
  [.devicetypes[] | select(.name | test("^Apple TV 4K")) | select(.name | test("1080p") | not) | .name + "|" + .identifier] | sort | reverse | .[]')
if [ -z "$TYPES" ]; then
  echo "No Apple TV 4K device type available" >&2
  xcrun simctl list devicetypes >&2
  exit 1
fi

for attempt in 1 2 3; do
  for runtime in $(xcrun simctl list runtimes --json | jq -r '.runtimes[] | select(.platform == "tvOS") | .identifier' | sort); do
    while IFS='|' read -r name type; do
      echo "Trying $name on $runtime" >&2
      id=$(xcrun simctl create "Acolyte Apple TV" "$type" "$runtime") || continue
      if xcrun simctl boot "$id" >&2 && xcrun simctl bootstatus "$id" -b >&2; then
        echo "$id"
        exit 0
      fi
      xcrun simctl shutdown "$id" >&2 || true
      xcrun simctl delete "$id" >&2 || true
    done <<< "$TYPES"
  done
  echo "Attempt $attempt failed, trying again" >&2
  sleep 20
done
echo "No tvOS runtime booted" >&2
xcrun simctl list runtimes >&2
exit 1
