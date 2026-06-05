#!/usr/bin/env bash
set -u

LILA_DOCKER_ROOT="${LILA_DOCKER_ROOT:-$HOME/dev/lila-docker}"
LILA_REPO="${LILA_REPO:-$LILA_DOCKER_ROOT/repos/lila}"

read_lila_docker_env() {
  local key="$1"
  local env_file="$LILA_DOCKER_ROOT/.env"

  if [ -f "$env_file" ]; then
    sed -n "s/^${key}=//p" "$env_file" | tail -n 1
  fi
}

configured_main_url="${EVENCHESS_MAIN_URL:-${LILA_URL:-$(read_lila_docker_env LILA_URL)}}"
MAIN_URL="${configured_main_url:-http://localhost:8080/}"
MAIN_URL="${MAIN_URL%/}/"

required_containers=(
  "lila-docker-lila-1"
  "lila-docker-lila_ws-1"
  "lila-docker-caddy-1"
  "lila-docker-mongodb-1"
  "lila-docker-redis-1"
)

optional_urls=(
  "Mongo admin|http://localhost:8081/"
  "Mailpit inbox|http://localhost:8025/"
  "Chessground demo|http://localhost:8090/demo.html"
  "PGN Viewer|http://localhost:8091/"
  "Search admin|http://localhost:8092/"
)

overall=0

check_url() {
  local label="$1"
  local url="$2"
  local required="$3"

  if curl -fsS -L --max-time 5 -o /dev/null "$url"; then
    printf "  [ok]   %s: %s\n" "$label" "$url"
  else
    printf "  [miss] %s: %s\n" "$label" "$url"
    if [ "$required" = "required" ]; then
      overall=1
    fi
  fi
}

echo "EvenChess-Lichess local status"
echo "Timestamp: $(date -Is)"
echo "lila-docker: $LILA_DOCKER_ROOT"
echo "source repo: $LILA_REPO"
echo

if git -C "$LILA_REPO" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Git:"
  echo "  branch: $(git -C "$LILA_REPO" branch --show-current 2>/dev/null || echo unknown)"
  echo "  sha:    $(git -C "$LILA_REPO" rev-parse --short HEAD 2>/dev/null || echo unknown)"
  echo "  remotes:"
  git -C "$LILA_REPO" remote -v 2>/dev/null | sed 's/^/    /'
  echo "  status:"
  git -C "$LILA_REPO" status --short | sed 's/^/    /'
else
  echo "Git: source repo is not a Git work tree"
fi
echo

if [ ! -d "$LILA_DOCKER_ROOT" ]; then
  echo "Missing lila-docker root: $LILA_DOCKER_ROOT" >&2
  exit 1
fi

cd "$LILA_DOCKER_ROOT" || exit 1

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not available inside WSL."
  echo "Start Docker Desktop and enable Settings > Resources > WSL integration > Ubuntu."
  exit 1
fi

echo "lila-docker status:"
if [ -x ./lila-docker ]; then
  ./lila-docker status || overall=1
else
  echo "  Missing executable: $LILA_DOCKER_ROOT/lila-docker"
  overall=1
fi
echo

echo "Docker containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || overall=1
echo

running_names="$(docker ps --format '{{.Names}}' 2>/dev/null || true)"
echo "Required core containers:"
for name in "${required_containers[@]}"; do
  if printf "%s\n" "$running_names" | grep -qx "$name"; then
    echo "  [ok]   $name"
  else
    echo "  [miss] $name"
    overall=1
  fi
done
echo

echo "URL checks:"
check_url "Main site" "$MAIN_URL" required
for item in "${optional_urls[@]}"; do
  label="${item%%|*}"
  url="${item#*|}"
  check_url "$label" "$url" optional
done
echo

cat <<'CAVEATS'
Deferred or caveated:
  API docs may be disabled because the local container previously lacked a serve script.
  lila_fishnet may be disabled or under repair; computer-opponent failures do not invalidate the Stage 1 human-vs-human baseline.
CAVEATS

exit "$overall"
