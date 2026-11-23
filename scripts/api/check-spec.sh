#!/usr/bin/env bash
# Fails unless api/openapi.json is byte for byte the spec of the HolyArrs
# release named in api/VERSION (upstream HolyArrs/HolyArrs, file
# app/backend/cmd/holyarrs/openapi.json at that tag).
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
VERSION="$(tr -d '[:space:]' < api/VERSION)"
URL="https://raw.githubusercontent.com/HolyArrs/HolyArrs/$VERSION/app/backend/cmd/holyarrs/openapi.json"
UPSTREAM="$(mktemp)"
trap 'rm -f "$UPSTREAM"' EXIT

if ! curl -fsSL --retry 3 -o "$UPSTREAM" "$URL"; then
	echo "::error::Could not download the spec of HolyArrs $VERSION from $URL. Is api/VERSION a release tag?"
	exit 1
fi

if ! cmp -s "$UPSTREAM" api/openapi.json; then
	echo "::error::api/openapi.json is not the spec of HolyArrs $VERSION. Run scripts/api/update-spec.sh $VERSION instead of editing it."
	diff <(python3 -m json.tool --sort-keys api/openapi.json) <(python3 -m json.tool --sort-keys "$UPSTREAM") | head -60 || true
	exit 1
fi
echo "api/openapi.json is the spec of HolyArrs $VERSION."
