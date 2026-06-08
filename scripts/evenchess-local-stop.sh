#!/usr/bin/env bash
set -u

LILA_DOCKER_ROOT="${LILA_DOCKER_ROOT:-$HOME/dev/lila-docker}"

echo "EvenChess-Lichess local stop"
echo "Timestamp: $(date -Is)"
echo "lila-docker: $LILA_DOCKER_ROOT"
echo

if [ ! -d "$LILA_DOCKER_ROOT" ]; then
  echo "Missing lila-docker root: $LILA_DOCKER_ROOT" >&2
  exit 1
fi

cd "$LILA_DOCKER_ROOT" || exit 1

if [ ! -x ./lila-docker ]; then
  echo "Missing executable: $LILA_DOCKER_ROOT/lila-docker" >&2
  exit 1
fi

echo "Stopping local stack with ./lila-docker stop"
./lila-docker stop

echo
echo "Post-stop containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo
echo "Note: this script intentionally does not run 'down', database reset, or cleanup commands."
