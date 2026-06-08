# EvenChess-Lichess Plan v1.6 Phase F - ECE Production Service Deployment

**Date:** 2026-06-03
**Phase:** F
**Status:** Conducted as the v1.6 ECE production service deployment baseline
**Repo:** EvenChess-Lichess
**Path:** `/home/jayde/dev/lila-docker/repos/lila`
**ECE repo referenced:** `/home/jayde/dev/lila-docker/repos/ece`
**Branch:** `codex/evenchess-v1.6-readiness`

This document completes Plan v1.6 Phase F by defining how the separate EvenChessEngine service should be deployed, started, stopped, validated, monitored, and connected to ECL in staging/production.

Phase F is deployment planning/documentation only. It does not modify the ECE repo, ECL gateway, service files, firewall rules, or provider configuration.

---

## 1. Phase F Decision Summary

Production ECE deployment decision:

```text
Run ECE as a separate private Linux service beside ECL.
ECL calls ECE server-to-server over a private network address.
Browsers never call ECE analysis endpoints.
```

Recommended v1.6 beta runtime:

```text
systemd service or container service on the same Linux host/private network as ECL.
```

Required production ECE base URL from ECL:

```text
ECE_BASE_URL=http://ece:8787
```

or equivalent private host/service name.

Local development may continue to use:

```text
http://127.0.0.1:8787
```

Production must not depend on WSL, Windows PowerShell scripts, Docker Desktop, Test Ground, or a browser-accessible ECE endpoint.

---

## 2. ECE Boundary Rules

ECE owns:

- deterministic chess calculations;
- legal/rules provider coordination;
- Stockfish provider orchestration;
- Syzygy provider orchestration;
- opening-book provider orchestration;
- Lichess eval-cache provider orchestration;
- Maia/human-risk provider orchestration when enabled;
- AI text provider orchestration when enabled;
- provider timeouts and normalization;
- deterministic composer;
- private cache and debug logs;
- provider paths, API keys, raw prompts, raw provider output, model weights, tablebases, and generated provider DBs.

ECL owns:

- public site and browser UI;
- authentication and authorization;
- match contracts and game policy;
- Set Level and Used Level authority;
- ECR settlement;
- tokens/subscriptions/admin;
- display permission and audit;
- server-to-server ECE adapter.

Forbidden:

- ECL browser code calling ECE analysis endpoints.
- ECL repo containing ECE provider secrets, provider paths, engine binaries, model weights, tablebases, generated provider DBs, raw prompts, or private ECE implementation.
- Public reverse proxy routing to ECE board/proposed/review endpoints.

---

## 3. ECE Runtime Commands

The ECE repo currently exposes these relevant commands in `package.json`:

```bash
npm test
npm run release:gate
npm run linux:start
npm run linux:stop
npm run linux:smoke
npm run linux:validate-providers
npm run linux:build-lichess-eval-cache
```

Underlying Linux scripts:

```bash
scripts/start-ece-linux.sh
scripts/stop-ece-linux.sh
scripts/smoke-ece-linux.sh
scripts/validate-external-providers-linux.sh
scripts/ece-release-gate.sh
scripts/ece-linux-env.sh
```

Current local lifecycle behavior:

- `start-ece-linux.sh` loads `scripts/ece-linux-env.sh`.
- It starts `node src/server.js` detached with `setsid` or `nohup`.
- It writes PID to `.ece-local.pid`.
- It writes stdout/stderr logs under `logs/`.
- It waits for `/health`.
- `stop-ece-linux.sh` stops the PID and removes the PID file.

Production note:

- The local scripts are useful for staging smoke and manual operation.
- Production should run ECE under systemd or a container supervisor, not rely only on `.ece-local.pid`.

---

## 4. Production Environment Variables

ECE environment variables belong to the ECE service configuration, not ECL.

Minimum production/staging ECE variables:

| Variable | Class | Production default / rule |
|---|---|---|
| `ECE_HOST` | private non-secret | bind private interface only; local `0.0.0.0` is test-only unless firewalled inside container network |
| `ECE_PORT` | private non-secret | `8787` |
| `ECE_MODE` | private non-secret | `production` or `staging` |
| `ECE_INTERNAL_API_KEY` | secret | required if ECE service auth is enabled |
| `OPENAI_API_KEY` | secret | ECE-only; may be empty if AI disabled |
| `ECE_STOCKFISH_PATH` | private path | ECE-only |
| `ECE_SYZYGY_PATH` | private path | ECE-only |
| `ECE_OPENING_BOOK_DB` | private path | ECE-only |
| `OPENING_BOOK_RAW_PATH` | private path | ECE-only |
| `ECE_LICHESS_EVAL_DB` | private path | ECE-only |
| `LICHESS_EVAL_RAW_PATH` | private path | ECE-only |
| `ECE_LC0_PATH` | private path | ECE-only |
| `ECE_MAIA_WEIGHTS_DIR` | private path | ECE-only |
| `ECE_CACHE_ENABLED` | private non-secret | `1` if cache is production-ready |
| `ECE_CACHE_PATH` | private path | ECE-only durable/private path |
| `ECE_DEBUG_IO_LOG` | private debug flag | `0` in production by default |
| `ECE_DEBUG_IO_LOG_PATH` | private path | private path only; no public access |
| `ECE_DEBUG_IO_LOG_MAX_ENTRIES` | private non-secret | only set when debug IO enabled |

Rules:

- `.env.wsl` and other local env files are local defaults only.
- Explicit service environment variables must override local env files.
- Production env files must not be committed.
- ECL must not send ECE provider paths or API keys in request bodies.

---

## 5. Network and Firewall Requirements

Production allowed paths:

```text
ECL backend -> ECE /health
ECL backend -> ECE /ready
ECL backend -> ECE /v1/ece/board
ECL backend -> ECE /v1/ece/board/quick
ECL backend -> ECE /v1/ece/board/deep
ECL backend -> ECE /v1/ece/proposed-move
ECL backend -> ECE /v1/ece/game-review
Operator private access -> ECE diagnostics only when approved
```

Production forbidden paths:

```text
Browser -> ECE
Public internet -> ECE :8787
Reverse proxy public route -> ECE analysis endpoints
Public route -> /ece/settings
Public route -> /api/ece/settings
Public route -> ECE CLM
```

If ECE and ECL run on the same host:

- bind ECE to loopback or private bridge network;
- allow only local/private ECL process access;
- do not expose port `8787` publicly.

If ECE and ECL run in containers:

- place both on an internal container network;
- do not publish ECE port to host/public interfaces;
- use service name `ece:8787` from ECL.

If ECE and ECL run on separate hosts:

- use private VPC/subnet;
- firewall ECE to ECL host(s) only;
- use service auth;
- avoid public DNS for ECE.

---

## 6. Service Manager Baseline

Production can use systemd or containers. The exact deployment is Phase S/T work, but Phase F defines the minimum behavior.

### 6.1 systemd shape

Illustrative only:

```ini
[Unit]
Description=EvenChess Engine
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/evenchess/ece
EnvironmentFile=/etc/evenchess/ece.env
ExecStart=/usr/bin/node src/server.js
Restart=on-failure
RestartSec=3
User=evenchess
Group=evenchess
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true
ReadWritePaths=/opt/evenchess/ece/logs /opt/evenchess/ece/data /opt/evenchess/ece/external_engines

[Install]
WantedBy=multi-user.target
```

Production service requirements:

- start on boot;
- restart on failure;
- run as non-root;
- log stdout/stderr to private logs or journald;
- expose health/readiness internally;
- have access only to required provider/cache directories;
- use environment/secret files with strict permissions.

### 6.2 container shape

Container requirements:

- `node >= 20`;
- read-only application layer where practical;
- mounted private provider/cache/log volumes;
- internal network only;
- health check on `/health`;
- readiness check on `/ready` for deployment gate;
- no public port publish;
- secrets mounted as environment or secret files.

---

## 7. Health, Readiness, and Smoke Gates

Minimum ECE gates before ECL enables live help:

```bash
cd /home/jayde/dev/lila-docker/repos/ece
npm test
npm run release:gate
npm run linux:validate-providers
npm run linux:start
npm run linux:smoke
npm run linux:stop
```

Required endpoint checks:

```text
GET  /health
GET  /ready
POST /v1/ece/board
POST /v1/ece/board/quick
POST /v1/ece/board/deep
POST /v1/ece/proposed-move
POST /v1/ece/game-review
```

Smoke must confirm:

- `/health` succeeds;
- `/ready` succeeds and returns safe provider/readiness status;
- quick board-state response is public-safe;
- deep board-state response is public-safe;
- proposed-move response is public-safe;
- game-review response is public-safe;
- invalid JSON is rejected;
- invalid envelopes are rejected;
- browser-origin calls are rejected;
- not-found routes return safe JSON;
- no public `position`;
- no public `shared_calculations`;
- no raw provider data;
- no secrets;
- no private provider paths.

---

## 8. Provider Validation

ECE provider validation command:

```bash
npm run linux:validate-providers
```

Provider checks must cover:

- Stockfish executable exists and is executable;
- lc0/Maia runtime exists and is executable when enabled;
- Maia weights directory exists when Maia enabled;
- Syzygy tablebases exist when enabled;
- opening book data exists when enabled;
- Lichess eval raw/cache DB exists when enabled;
- provider registry/readiness is safe and does not expose paths/secrets publicly.

Production policy:

- Missing optional providers degrade ECE output.
- Missing required providers block the ECE readiness gate if the launch scope depends on them.
- Provider failures must not crash ECE for normal request failures.
- Provider timeouts must return degraded diagnostics.

---

## 9. Logging and Debug Policy

Production defaults:

```text
ECE_DEBUG_IO_LOG=0
```

ECE logs may include:

- request IDs;
- endpoint;
- status;
- timings;
- provider status keys;
- cache hit/miss;
- safe diagnostics;
- unavailable provider identifiers.

ECE logs must not include:

- API keys;
- provider secrets;
- private deployment tokens;
- raw prompts;
- raw AI output where unsafe;
- raw provider output;
- raw UCI session logs;
- provider filesystem paths unless explicitly local/debug and not public;
- tablebase/model/cache internals that are not safe to expose.

Temporary debug IO:

- may be enabled only by operator action;
- must write to private local path;
- must be time-limited;
- must redact sensitive fields;
- must not be exposed through ECL public pages;
- must be disabled after incident/debug window.

---

## 10. ECL/ECE Integration Gate

Before ECL enables public live ECE help:

1. ECE is started by service manager/container.
2. ECE `/health` passes internally.
3. ECE `/ready` passes internally.
4. ECE smoke passes from the ECE host/network.
5. ECL backend can call ECE using production `ECE_BASE_URL`.
6. Browser cannot call ECE directly.
7. ECL rejects stale ECE payloads.
8. ECL can degrade gracefully when ECE is stopped.
9. ECL does not expose ECE base URL, provider status internals, or raw diagnostics in browser JSON.
10. ECL audit records ECE request/accept/reject state.

Phase D dependency:

- ECL still needs production-safe backend-only `ECE_BASE_URL` config support for private service names.

Phase E dependency:

- ECL still needs durable persistence for ECE payload history, stale rejection records, assistance state, and consumables.

---

## 11. Deployment Runbook Baseline

### 11.1 Initial staging deploy

```bash
# ECE repo
cd /opt/evenchess/ece
npm ci --omit=dev
npm run release:gate
npm run linux:validate-providers

# service-manager/container start
systemctl start evenchess-ece

# internal checks
curl -fsS http://ece:8787/health
curl -fsS http://ece:8787/ready

# full smoke from ECE host or internal network
BASE_URL=http://ece:8787 npm run linux:smoke
```

### 11.2 Stop/rollback

```bash
systemctl stop evenchess-ece
```

or container equivalent.

ECL rollback order:

1. Disable ECE live help in ECL feature flags.
2. Disable ECE deep calls if only provider path is failing.
3. Let active games continue without ECE if needed.
4. Stop or roll back ECE service.
5. Review ECE logs and ECL stale/error metrics.

### 11.3 Local script fallback

For local/manual validation only:

```bash
cd /home/jayde/dev/lila-docker/repos/ece
npm run linux:start
npm run linux:smoke
npm run linux:stop
```

Do not use `.ece-local.pid` as the only production supervisor.

---

## 12. Monitoring Requirements

ECE metrics/log signals required for production:

- process up/down;
- `/health` status;
- `/ready` status;
- quick board latency;
- deep board latency;
- proposed-move latency;
- game-review latency;
- provider availability;
- provider timeout count;
- ECE error rate;
- ECE partial/degraded rate;
- cache hit/miss;
- request count by endpoint;
- internal auth failure count if enabled;
- browser-origin rejection count;
- debug IO enabled/disabled status;
- memory and CPU;
- log volume.

ECL-side ECE metrics:

- ECE request count;
- ECE timeout count;
- stale ECE payload rejection count;
- accepted quick payload count;
- accepted deep payload count;
- ECE unavailable fallback count;
- overlay render failure count after accepted payload;
- coach text update count on player turn;
- proposed/potential authorization failure count.

---

## 13. Security Checklist

Before production:

- ECE port is not public.
- ECE analysis endpoints reject browser-origin requests.
- ECE uses service auth if enabled.
- ECE `/health` and `/ready` do not reveal secrets or provider paths.
- ECE settings/CLM pages are not public routes.
- Provider paths are not in ECL repo or browser bundles.
- Debug IO is off.
- Logs are private and redacted.
- Service runs as non-root.
- Provider directories have least-privilege permissions.
- ECL never sends API keys in ECE request body.

---

## 14. Phase F Acceptance Status

| Acceptance gate | Status | Notes |
|---|---|---|
| ECE deploy source identified | Complete | ECE repo `/home/jayde/dev/lila-docker/repos/ece` or production equivalent |
| Service manager/container baseline defined | Complete | Section 6 |
| Private host/port config defined | Complete | Sections 4 and 5 |
| Provider config rules defined | Complete | Sections 4 and 8 |
| `/health` and `/ready` validation defined | Complete | Section 7 |
| Quick/deep/proposed/game-review smoke defined | Complete | Section 7 |
| Provider validation command defined | Complete | Section 8 |
| Public ECE exposure disabled by design | Complete as architecture requirement | Needs server firewall/proxy implementation |
| ECE log redaction policy defined | Complete | Section 9 |
| ECE survives process/deploy restart | Blocked pending service implementation/test | systemd/container setup required |
| ECL backend can call production ECE URL | Blocked pending Phase D implementation | ECL currently needs production service-name URL support |
| Browser cannot call ECE analysis endpoints | Blocked pending server firewall/proxy validation | ECE has smoke coverage for browser-origin rejection |
| Debug IO off by default | Complete as policy | Must be verified in service env |

Phase F is complete as an ECE deployment plan. It is not production-deployed.

---

## 15. Phase G Entry Criteria

Phase G can begin after this deployment baseline, but public ECE live help remains blocked until:

1. Production-safe ECL `ECE_BASE_URL` config is implemented.
2. ECE service manager/container spec exists.
3. ECE provider validation passes in staging.
4. ECE smoke passes in staging.
5. ECL can call ECE internally.
6. Browser cannot call ECE directly.
7. ECL persists ECE payload history and stale rejection records.
