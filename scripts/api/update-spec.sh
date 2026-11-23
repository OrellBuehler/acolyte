#!/usr/bin/env bash
# Replaces api/openapi.json with the spec of a HolyArrs release and records
# the tag in api/VERSION, prints what changed for the endpoints the app uses,
# then runs the contract test.
#
# Usage: scripts/api/update-spec.sh [tag]   (default: the latest release)
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
TAG="${1:-}"
if [ -z "$TAG" ]; then
	TAG="$(curl -fsSL https://api.github.com/repos/HolyArrs/HolyArrs/releases/latest | python3 -c 'import json, sys; print(json.load(sys.stdin)["tag_name"])')"
fi
OLD="$(tr -d '[:space:]' < api/VERSION)"
NEW="$(mktemp)"
trap 'rm -f "$NEW"' EXIT

curl -fsSL --retry 3 -o "$NEW" "https://raw.githubusercontent.com/HolyArrs/HolyArrs/$TAG/app/backend/cmd/holyarrs/openapi.json"
python3 -c 'import json, sys; json.load(open(sys.argv[1]))' "$NEW"

USED=shared/build/reports/api/client-endpoints.json
if [ -f "$USED" ]; then
	python3 scripts/api/drift.py --pinned api/openapi.json --pinned-label "$OLD" --compare "$NEW" --compare-label "$TAG" --used "$USED"
fi

cp "$NEW" api/openapi.json
echo "$TAG" > api/VERSION
echo "api/openapi.json is now HolyArrs $TAG (was $OLD)."

if [ "${SKIP_TESTS:-}" != "1" ]; then
	./gradlew shared:testDebugUnitTest --tests 'ch.orellbuehler.acolyte.contract.ApiContractTest'
	echo "Contract test passed. For the live test: eval \"\$(scripts/api/live-server.sh)\" && ./gradlew shared:testDebugUnitTest --tests '*LiveContractTest*'; scripts/api/live-server.sh stop"
fi
