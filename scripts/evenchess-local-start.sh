#!/usr/bin/env bash
set -u

LILA_DOCKER_ROOT="${LILA_DOCKER_ROOT:-$HOME/dev/lila-docker}"
LILA_REPO="${LILA_REPO:-$LILA_DOCKER_ROOT/repos/lila}"
START_WAIT_SECONDS="${EVENCHESS_START_WAIT_SECONDS:-300}"

read_lila_docker_env() {
  local key="$1"
  local env_file="$LILA_DOCKER_ROOT/.env"

  if [ -f "$env_file" ]; then
    sed -n "s/^${key}=//p" "$env_file" | tail -n 1
  fi
}

detect_windows_lan_ip() {
  if ! command -v powershell.exe >/dev/null 2>&1; then
    return 0
  fi

  powershell.exe -NoProfile -Command '
$addresses = Get-NetIPAddress -AddressFamily IPv4 | Where-Object {
  $_.IPAddress -notmatch "^(127|169\.254)\." -and
  $_.InterfaceAlias -notmatch "vEthernet|Loopback|Tailscale|Docker"
}
$selected = $addresses |
  Sort-Object @{ Expression = { if ($_.InterfaceAlias -match "Wi-Fi|Ethernet") { 0 } else { 1 } } }, InterfaceAlias |
  Select-Object -First 1
if ($selected) { $selected.IPAddress }
' 2>/dev/null | tr -d '\r' | sed -n '1p'
}

upsert_env_value() {
  local env_file="$1"
  local key="$2"
  local value="$3"
  local tmp

  tmp="$(mktemp)"
  if [ -f "$env_file" ]; then
    awk -v key="$key" -v value="$value" '
      BEGIN { found = 0 }
      $0 ~ "^" key "=" { print key "=" value; found = 1; next }
      { print }
      END { if (!found) print key "=" value }
    ' "$env_file" > "$tmp"
  else
    printf "%s=%s\n" "$key" "$value" > "$tmp"
  fi

  mv "$tmp" "$env_file"
}

configure_lan_access() {
  local detected_ip
  local configured_main_url

  if [ "${EVENCHESS_AUTO_LAN_URL:-1}" != "0" ]; then
    detected_ip="${EVENCHESS_LAN_IP:-$(detect_windows_lan_ip)}"
  else
    detected_ip=""
  fi

  if [ -n "$detected_ip" ]; then
    export LILA_DOMAIN="${EVENCHESS_LILA_DOMAIN:-${detected_ip}:8080}"
    export LILA_URL="${EVENCHESS_LILA_URL:-http://${LILA_DOMAIN}}"
    upsert_env_value "$LILA_DOCKER_ROOT/.env" LILA_DOMAIN "$LILA_DOMAIN"
    upsert_env_value "$LILA_DOCKER_ROOT/.env" LILA_URL "$LILA_URL"
    upsert_env_value "$LILA_DOCKER_ROOT/settings.env" LILA_DOMAIN "$LILA_DOMAIN"
    upsert_env_value "$LILA_DOCKER_ROOT/settings.env" LILA_URL "$LILA_URL"
  else
    export LILA_DOMAIN="${LILA_DOMAIN:-$(read_lila_docker_env LILA_DOMAIN)}"
    export LILA_URL="${LILA_URL:-$(read_lila_docker_env LILA_URL)}"
  fi

  configured_main_url="${EVENCHESS_MAIN_URL:-${LILA_URL:-http://localhost:8080/}}"
  MAIN_URL="${configured_main_url%/}/"
}

recreate_web_stack_if_needed() {
  local current_domain
  local current_url

  if [ -z "${LILA_DOMAIN:-}" ] || [ -z "${LILA_URL:-}" ]; then
    return 0
  fi

  current_domain="$(docker compose exec -T lila printenv LILA_DOMAIN 2>/dev/null | tr -d '\r' || true)"
  current_url="$(docker compose exec -T lila printenv LILA_URL 2>/dev/null | tr -d '\r' || true)"
  if [ "$current_domain" = "$LILA_DOMAIN" ] && [ "$current_url" = "$LILA_URL" ]; then
    return 0
  fi

  echo "Recreating web containers for LAN URL: $LILA_URL"
  LILA_DOMAIN="$LILA_DOMAIN" LILA_URL="$LILA_URL" docker compose up -d --no-deps --force-recreate lila lila_ws caddy
}

MAIN_URL="http://localhost:8080/"

print_header() {
  echo "EvenChess-Lichess local start"
  echo "Timestamp: $(date -Is)"
  echo "lila-docker: $LILA_DOCKER_ROOT"
  echo "source repo: $LILA_REPO"
  echo
}

print_git_info() {
  if git -C "$LILA_REPO" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Git:"
    echo "  branch: $(git -C "$LILA_REPO" branch --show-current 2>/dev/null || echo unknown)"
    echo "  sha:    $(git -C "$LILA_REPO" rev-parse --short HEAD 2>/dev/null || echo unknown)"
    echo "  remotes:"
    git -C "$LILA_REPO" remote -v 2>/dev/null | sed 's/^/    /'
    echo "  status:"
    if git -C "$LILA_REPO" diff --quiet && git -C "$LILA_REPO" diff --cached --quiet; then
      local untracked
      untracked="$(git -C "$LILA_REPO" ls-files --others --exclude-standard | head -n 1)"
      if [ -z "$untracked" ]; then
        echo "    clean"
      else
        git -C "$LILA_REPO" status --short | sed 's/^/    /'
      fi
    else
      git -C "$LILA_REPO" status --short | sed 's/^/    /'
    fi
  else
    echo "Git: source repo is not a Git work tree"
  fi
  echo
}

wait_for_main_site() {
  local waited=0
  local interval=5

  echo "Waiting for $MAIN_URL"
  while [ "$waited" -le "$START_WAIT_SECONDS" ]; do
    if curl -fsS --max-time 5 -o /dev/null "$MAIN_URL"; then
      echo "Main site reachable: $MAIN_URL"
      return 0
    fi

    printf "."
    sleep "$interval"
    waited=$((waited + interval))
  done

  echo
  echo "Main site was not reachable after ${START_WAIT_SECONDS}s: $MAIN_URL"
  return 1
}

stack_is_running() {
  if docker compose ps -q --services --filter "status=running" | grep -q .; then
    return 0
  fi
  return 1
}

print_urls() {
  cat <<URLS

Useful local URLs:
  Main site:        $MAIN_URL
  PC fallback:      http://localhost:8080/
  Mongo admin:      http://localhost:8081/
  Mailpit inbox:    http://localhost:8025/
  Chessground demo: http://localhost:8090/demo.html
  PGN Viewer:       http://localhost:8091/
  Search admin:     http://localhost:8092/

Known local caveats:
  API docs may be disabled or deferred.
  lila_fishnet may be disabled or under repair; use human-vs-human as the Stage 1 baseline.
URLS
}

if [ ! -d "$LILA_DOCKER_ROOT" ]; then
  echo "Missing lila-docker root: $LILA_DOCKER_ROOT" >&2
  exit 1
fi

configure_lan_access
print_header
print_git_info

cd "$LILA_DOCKER_ROOT" || exit 1

if [ ! -x ./lila-docker ]; then
  echo "Missing executable: $LILA_DOCKER_ROOT/lila-docker" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not available inside WSL." >&2
  echo "Start Docker Desktop and enable Settings > Resources > WSL integration > Ubuntu." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker Desktop is not ready inside WSL yet." >&2
  echo "Open Docker Desktop, wait until it finishes starting, then try again." >&2
  exit 1
fi

echo "Starting local stack with ./lila-docker start"
if ! ./lila-docker start; then
  if stack_is_running; then
    echo "lila-docker reported no stopped services, but the stack is already running. Continuing."
  else
    echo "lila-docker start failed." >&2
    exit 1
  fi
fi

recreate_web_stack_if_needed

echo
echo "Running containers:"
if ! docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"; then
  echo "Unable to list Docker containers." >&2
  exit 1
fi
echo

wait_for_main_site
result=$?

print_urls
exit "$result"
