#!/usr/bin/env bash
# Boots the published HolyArrs image of the release in api/VERSION, finishes its
# first-run setup over the API and prints the ACOLYTE_LIVE_* variables that
# LiveContractTest reads (appended to $GITHUB_ENV in CI).
#
# Usage: scripts/api/live-server.sh [start|stop]
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
VERSION="$(tr -d '[:space:]' < api/VERSION)"
PORT="${ACOLYTE_LIVE_PORT:-6262}"
NAME="${ACOLYTE_LIVE_CONTAINER:-acolyte-live}"
IMAGE="${ACOLYTE_LIVE_IMAGE:-holyarrs/holyarrs:$VERSION}"
BASE="http://127.0.0.1:$PORT"
USER_NAME="acolyte"
PASSWORD="ci-$(openssl rand -hex 12)"

if [ "${1:-start}" = "stop" ]; then
	docker rm -f "$NAME" >/dev/null 2>&1 || true
	exit 0
fi

json_field() {
	python3 -c 'import json, sys; print(json.load(sys.stdin)[sys.argv[1]])' "$1"
}

docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" -p "127.0.0.1:$PORT:6262" "$IMAGE" >/dev/null

for _ in $(seq 1 120); do
	if curl -fsS "$BASE/readyz" >/dev/null 2>&1; then
		break
	fi
	sleep 1
done
if ! curl -fsS "$BASE/readyz" >/dev/null 2>&1; then
	docker logs "$NAME" | tail -50
	echo "::error::$IMAGE did not become ready on $BASE"
	exit 1
fi

JAR="$(mktemp)"
trap 'rm -f "$JAR"' EXIT

state="$(curl -fsS "$BASE/api/v1/auth/state")"
if [ "$(echo "$state" | json_field needs_setup)" != "True" ]; then
	echo "::error::the fresh server does not need setup: $state"
	exit 1
fi

csrf="$(curl -fsS -c "$JAR" -H 'Content-Type: application/json' \
	-d "{\"name\":\"$USER_NAME\",\"password\":\"$PASSWORD\",\"display_name\":\"Acolyte CI\"}" \
	"$BASE/api/v1/auth/setup" | json_field csrf)"
curl -fsS -b "$JAR" -c "$JAR" -X POST -H "X-CSRF-Token: $csrf" "$BASE/api/v1/auth/setup/done" >/dev/null

csrf="$(curl -fsS -b "$JAR" -c "$JAR" -H 'Content-Type: application/json' \
	-d "{\"name\":\"$USER_NAME\",\"password\":\"$PASSWORD\"}" \
	"$BASE/api/v1/auth/login" | json_field csrf)"
key="$(curl -fsS -b "$JAR" -H 'Content-Type: application/json' -H "X-CSRF-Token: $csrf" \
	-d '{"label":"acolyte-ci","scopes":["all"]}' \
	"$BASE/api/v1/auth/keys" | json_field key)"

vars="ACOLYTE_LIVE_URL=$BASE
ACOLYTE_LIVE_USER=$USER_NAME
ACOLYTE_LIVE_PASSWORD=$PASSWORD
ACOLYTE_LIVE_API_KEY=$key"

if [ -n "${GITHUB_ENV:-}" ]; then
	echo "::add-mask::$PASSWORD"
	echo "::add-mask::$key"
	echo "$vars" >> "$GITHUB_ENV"
	echo "HolyArrs $VERSION is set up on $BASE"
else
	echo "$vars" | sed 's/^/export /'
fi
