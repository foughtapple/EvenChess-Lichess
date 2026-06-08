# EvenChess-Lichess Plan v1.6 Phase D - Secrets, Config, and Environment Management

**Date:** 2026-06-03
**Phase:** D
**Status:** Conducted as the v1.6 configuration and secrets baseline
**Repo:** EvenChess-Lichess
**Path:** `/home/jayde/dev/lila-docker/repos/lila`
**Branch:** `codex/evenchess-v1.6-readiness`

This document completes Plan v1.6 Phase D by defining the production configuration model, environment variable inventory, public/private/secret separation, feature flag defaults, ECE backend-only URL rules, safe debug policy, and blockers that must be fixed before server deployment.

Phase D is documentation and release planning only. It does not implement config loaders, service files, or secret storage.

---

## 1. Phase D Decision Summary

Production config model:

```text
Lichess/ECL runtime config comes from production config files plus environment overrides.
EvenChess feature flags and operational settings use admin/audited backend settings where appropriate.
Secrets are environment/secret-store only.
ECE private provider configuration stays in the ECE repo/service only.
```

Required production change before deployment:

```text
ECL must support a backend-only production ECE base URL such as http://ece:8787.
```

Current code concern:

- `EngineGateway.EceServiceConfig.validLocalBaseUrl` currently allows only local hosts such as `127.0.0.1`, `localhost`, `host.docker.internal`, and loopback.
- This is correct for local Test Ground safety but is not production-ready for a private service name like `http://ece:8787`.
- Later implementation must add a production-safe backend-only config path that allows private service names without exposing them to browser clients.

---

## 2. Config Classes

| Class | Description | Examples | Public exposure |
|---|---|---|---|
| Public non-secret config | Safe site/runtime labels and public routes | public domain, asset URL, enabled public entry points | May appear in pages/bundles when intended |
| Private non-secret config | Backend-only service URLs and operational limits | `ECE_BASE_URL`, Mongo URI host, Redis URI host, feature flag defaults | Backend/admin only |
| Secrets | Credentials, keys, tokens, signing material | Play secret, DB passwords, API tokens, ECE internal key, OpenAI key | Never public |
| Local dev defaults | Values used only for local development | `localhost:9663`, `127.0.0.1:8787`, Test ECE | Never production authority |
| Admin runtime settings | Audited operator-controlled settings | bot fallback, ECOR table, feature flags, incident pauses | Admin only; public sees safe derived labels |

Rules:

- Public JSON must never expose private URLs, secret values, raw diagnostics, provider paths, ticket IDs, request IDs, policy internals, raw prompts, or raw provider output.
- Admin pages may show configured/not-configured status for secrets, but not the secret value.
- ECE provider paths and API keys belong to ECE configuration, not ECL.

---

## 3. ECL Environment Inventory

The exact config mechanism can be Play/HOCON, environment variables, service-manager environment files, or a secret manager. The production inventory must include at least the following.

### 3.1 Core Lichess/ECL runtime

| Key / config | Class | Local default observed | Production requirement |
|---|---|---|---|
| `http.port` | Private non-secret | `9663` | Internal app port behind reverse proxy |
| `net.domain` | Public non-secret | `localhost:9663` | Public EvenChess domain |
| `net.base_url` | Public non-secret | derived from `net.domain` | HTTPS public base URL |
| `net.asset.domain` | Public non-secret | same as `net.domain` | asset/CDN domain or public domain |
| `net.asset.base_url` | Public non-secret | HTTP local | HTTPS asset URL |
| `net.socket.domains` | Public/private routing config | `localhost:9664` | production websocket domain(s) |
| `mongodb.uri` | Secret/private | local no-password URI | secret/private Mongo URI |
| `redis.uri` | Secret/private | `redis://127.0.0.1` | secret/private Redis URI |
| Play HTTP secret/session config | Secret | dev defaults in `base.conf` | production secret value |
| mail/SMTP config | Secret/private | dev/blank | production SMTP or disabled policy |
| OAuth/API tokens | Secret | dev/blank | production secret values |
| monitoring endpoints/tokens | Secret/private | dev endpoints/blanks | production metrics/log config |

Notes:

- `conf/base.conf` explicitly states its secret keys are development-only.
- Production must not use `conf/base.conf` dev secrets as production secrets.

### 3.2 EvenChess-Lichess backend integration

| Proposed key / setting | Class | Default | Production requirement |
|---|---|---|---|
| `ECE_BASE_URL` | Private non-secret | local `http://127.0.0.1:8787` | backend-only private URL, e.g. `http://ece:8787` |
| `ECE_INTERNAL_API_KEY` or equivalent ECL-side client secret | Secret | none locally | required if ECE service auth is enabled |
| `EVENCHESS_ENV` | Private non-secret | local/dev | `staging` or `production` |
| `EVENCHESS_PUBLIC_ENABLED` | Private/admin setting | off/on by environment | controls public EvenChess entry |
| `EVENCHESS_DEBUG_SEARCH_CARD_ENABLED` | Private/admin setting | local-only | false in production |
| `EVENCHESS_ECE_DEBUG_PROXY_ENABLED` | Private/admin setting | local-only | false in production |
| `EVENCHESS_TEST_ECE_ENABLED` | Local-only | local optional | false/not installed in production |

Required implementation follow-up:

- Replace or extend local-only ECE URL validation so production can use private DNS/service names.
- Ensure `ECE_BASE_URL` is read only by backend code.
- Ensure frontend bundles cannot access `ECE_BASE_URL`.
- If `ECE_INTERNAL_API_KEY` is used, send it only as backend-to-ECE auth, never to browser or request payloads visible to users.

### 3.3 EvenChess admin SettingStore/runtime settings

Current EvenChess backend admin setting IDs include:

- OpenAI provider/model/key status flags;
- TTS provider/key status flags;
- Stockfish profile, max depth, max MultiPV, jobs/minute, equivalent rating bands;
- AI enablement by surface;
- overlay/coaching/Offset Count flags;
- token/rewarded-ad/payment switches;
- campaign variant and kill switches;
- bot fallback enabled/scope/timeout;
- bot simulation enabled/scope/count/rating range/level range/persona;
- ECOR policy version, ECOR gap table, rating-level bands, snapshot history;
- AI/TTS cost and rate limits;
- audit retention days;
- incident global pause, live coaching pause, AI pause, TTS pause, engine pause, token/ad pause, no-rate, public notice.

Production rule:

- These settings are operational config, not raw secret storage.
- Raw API keys must not be stored through SettingStore.
- Secret status flags may show configured/rotated state only.

---

## 4. ECE Environment Inventory

ECE configuration belongs to the ECE service/repo. ECL must know only the backend service URL and optional service-auth material.

ECE production/staging keys from the ECE requirements:

| ECE key | Class | Production notes |
|---|---|---|
| `ECE_HOST` | Private non-secret | Private interface only; local `0.0.0.0` is test-only |
| `ECE_PORT` | Private non-secret | Default `8787` |
| `ECE_MODE` | Private non-secret | `staging` or `production` |
| `ECE_INTERNAL_API_KEY` | Secret | production/staging service auth if enabled |
| `OPENAI_API_KEY` | Secret | internal ECE-only, never ECL/browser |
| provider paths | Private/secret-adjacent | ECE-only; never ECL/browser |
| cache paths | Private | ECE-only |
| debug IO controls | Private | off by default in production |

ECE local/test keys that must not become production public assumptions:

- `ECE_DEBUG_IO_LOG`
- `ECE_DEBUG_IO_LOG_PATH`
- `ECE_DEBUG_IO_LOG_MAX_ENTRIES`
- local provider paths under `external_engines/...`
- local Windows/WSL paths
- Test Ground ECE launch paths

Production rule:

- ECE `/health` and `/ready` must not include secrets or provider paths.
- ECE `/ece/settings` and `/api/ece/settings` are local/private operator diagnostics only and must not be publicly routed.

---

## 5. Feature Flag Defaults

These defaults align Phase A scope with Phase C architecture.

| Flag | Local | Staging | First public beta | Production GA candidate |
|---|---:|---:|---:|---:|
| `evenchess.public.enabled` | on | on | on | on |
| `evenchess.search.enabled` | on | on | on | on |
| `evenchess.search.preferredLevel.enabled` | on | on | on | on |
| `evenchess.casual.enabled` | on | on | on | on |
| `evenchess.rated.enabled` | on for tests | on for smoke | conditional/off until ECR gates pass | on after gates |
| `evenchess.computer.enabled` | on | on | on | on |
| `evenchess.ece.quick.enabled` | on | on | on | on |
| `evenchess.ece.deep.enabled` | on | on | on if latency acceptable | on |
| `evenchess.ece.liveHelp.enabled` | on | on | on | on |
| `evenchess.overlay.enabled` | on | on | on | on |
| `evenchess.coach.enabled` | on | on | on | on |
| `evenchess.eval.enabled` | on | on | on | on |
| `evenchess.proposedMove.enabled` | on for tests | on for staging tests | off until Phase L gates pass | on after gates |
| `evenchess.potentialMoves.enabled` | on for tests | on for staging tests | off until Phase L gates pass | on after gates |
| `evenchess.analysisMemory.enabled` | on for tests | on | conditional | on after retention gates |
| `evenchess.fullGameAnalysis.enabled` | on for tests | conditional | off | conditional |
| `evenchess.tokens.enabled` | on for tests | conditional | off | on after Phase N |
| `evenchess.subscriptions.enabled` | off | sandbox only | off | on after Phase N |
| `evenchess.ads.enabled` | off | sandbox only | off | on after Phase N/Q |
| `evenchess.botFallback.enabled` | on for tests | on for staging tests | off initially | admin-controlled |
| `evenchess.botSimulation.enabled` | on for tests | on for staging tests | off | admin/staging-only unless approved |
| `evenchess.ecorCalibration.enabled` | on admin | on admin | admin-only | admin-only |
| `evenchess.debug.searchCard.enabled` | optional | restricted | off | off |
| `evenchess.testEce.enabled` | optional | off | off | off |

Fairness-affecting flag changes must be audited and versioned. They must not silently alter active rated games.

---

## 6. Public JSON and Browser Bundle Rules

Browser-safe:

- public domain;
- asset URL;
- public route names;
- deployment-safe search labels;
- assigned Set Levels where the match contract allows disclosure;
- bot fallback on/off disclosure;
- user-facing entitlement labels;
- sanitized ECE availability state where approved.

Browser-forbidden:

- `ECE_BASE_URL`;
- ECE internal API key;
- ECE provider paths;
- ECE raw diagnostics;
- raw prompts;
- raw provider output;
- ticket IDs;
- internal pool keys;
- request IDs;
- raw token gate keys;
- internal MMR/ECR calculation fields;
- policy internals not explicitly approved for disclosure;
- debug IO file paths.

Required release check:

```bash
grep -RIn "ECE_BASE_URL\\|http://ece:8787\\|127.0.0.1:8787\\|host.docker.internal" public ui app/views
```

Any browser bundle/page hit must be reviewed and removed unless it is a local-only Test Ground path not shipped publicly.

---

## 7. Debug Policy

### 7.1 Local development

Allowed:

- Test Ground panel;
- Test ECE payload server;
- ECE debug IO log;
- ECE settings/CLM links;
- verbose local search diagnostics;
- local WSL/Docker launch helpers.

### 7.2 Staging

Allowed only behind admin/private access:

- verbose search diagnostics;
- ECE readiness/provider status;
- feature-flag dashboards;
- bot operation dashboards;
- ECOR calibration controls;
- sampled safe logs.

### 7.3 Production

Default:

- ECE debug IO off;
- public debug search card off;
- Test ECE disabled/not deployed;
- Test Ground disabled/not public;
- ECE CLM/settings not public;
- raw diagnostics not sent to browser.

Temporary production debug:

- must be enabled by operator/admin only;
- must be time-limited;
- must be audited;
- must redact secrets/provider paths/raw prompts/raw outputs;
- must be disabled after incident.

---

## 8. Secret Storage Requirements

Acceptable for first beta:

- locked-down systemd environment files with strict file permissions;
- container runtime secrets;
- cloud/VPS secret manager;
- encrypted deployment variables.

Not acceptable:

- committed `.env`;
- committed `conf/application.conf` with production secrets;
- admin SettingStore raw API key entry;
- browser-local storage;
- Test Ground config as production source;
- logs containing raw secret values.

Minimum secret inventory:

- Play/app secret material;
- MongoDB credentials if remote/authenticated;
- Redis credentials if remote/authenticated;
- SMTP/mail credentials;
- OAuth/API tokens used by Lila;
- metrics/logging tokens;
- payment provider secrets when enabled;
- ad provider secrets when enabled;
- ECE internal API key if enabled;
- OpenAI/AI/TTS keys in ECE only;
- backup storage credentials.

---

## 9. Production Config Example With Fake Values

This is illustrative only. Do not copy it as a real secret file.

```bash
# Public ECL routing
ECL_ENV=production
ECL_PUBLIC_DOMAIN=evenchess.example.com
ECL_PUBLIC_BASE_URL=https://evenchess.example.com
ECL_ASSET_BASE_URL=https://evenchess.example.com

# Private service URLs
ECL_HTTP_PORT=9663
MONGODB_URI=mongodb://ecl_user:<secret>@mongo:27017/lila?appName=lila
REDIS_URI=redis://:<secret>@redis:6379
ELASTICSEARCH_URL=http://search:9200
ECE_BASE_URL=http://ece:8787

# Optional ECL->ECE service auth
ECE_INTERNAL_API_KEY=<secret>

# EvenChess public feature defaults
EVENCHESS_PUBLIC_ENABLED=1
EVENCHESS_SEARCH_ENABLED=1
EVENCHESS_CASUAL_ENABLED=1
EVENCHESS_RATED_ENABLED=0
EVENCHESS_ECE_QUICK_ENABLED=1
EVENCHESS_ECE_DEEP_ENABLED=1
EVENCHESS_OVERLAY_ENABLED=1
EVENCHESS_COACH_ENABLED=1
EVENCHESS_BOT_FALLBACK_ENABLED=0
EVENCHESS_BOT_SIMULATION_ENABLED=0
EVENCHESS_PROPOSED_MOVE_ENABLED=0
EVENCHESS_POTENTIAL_MOVES_ENABLED=0
EVENCHESS_TOKENS_ENABLED=0
EVENCHESS_SUBSCRIPTIONS_ENABLED=0
EVENCHESS_ADS_ENABLED=0
EVENCHESS_DEBUG_SEARCH_CARD_ENABLED=0
EVENCHESS_TEST_ECE_ENABLED=0
```

ECE service fake example:

```bash
ECE_HOST=127.0.0.1
ECE_PORT=8787
ECE_MODE=production
ECE_INTERNAL_API_KEY=<secret>
OPENAI_API_KEY=<secret-or-empty>
ECE_DEBUG_IO_LOG=0
```

On a container/private-network deployment, `ECE_HOST` may bind to the private service interface according to ECE deployment design. It must not expose analysis endpoints publicly.

---

## 10. Required Implementation Follow-Ups

Phase D found these implementation/config blockers:

1. Add production ECL config for `ECE_BASE_URL` that allows private service names such as `http://ece:8787`.
2. Keep the current local-only ECE URL validation for Test Ground paths, but separate it from production backend config validation.
3. Add optional ECL-to-ECE service auth header support if ECE enables `ECE_INTERNAL_API_KEY`.
4. Ensure `ECE_BASE_URL` cannot be serialized into browser bundles or public JSON.
5. Add release check/script for browser bundle private URL scans.
6. Replace hardcoded local ECE defaults in production paths with config-driven backend values.
7. Ensure debug search card/Test ECE/Test Ground flags are false in production config.
8. Ensure admin SettingStore never accepts raw API keys or provider paths as secret storage.
9. Add a deployment env example with fake values only.
10. Add production secret storage instructions in the deployment runbook.

---

## 11. Phase D Acceptance Status

| Acceptance gate | Status | Notes |
|---|---|---|
| Environment variables inventoried | Complete as baseline | Sections 3, 4, and 9 |
| Public/private/secret classes separated | Complete | Section 2 |
| Secret storage approach defined | Complete as accepted options | Section 8 |
| Hardcoded local URL risk identified | Complete | ECE config currently local-only |
| Backend-only ECE URL rule defined | Complete | Sections 3.2, 4, and 6 |
| Feature flags and defaults defined | Complete | Section 5 |
| ECE debug off by default in production | Complete as policy | Section 7 |
| Public JSON/browser no-secret rules defined | Complete | Section 6 |
| Production config can be generated without local paths | Blocked pending implementation | Requires production `ECE_BASE_URL` config path |
| Browser bundle contains no private ECE endpoints | Blocked pending release scan | Requires implementation and build scan |

Phase D is complete as a configuration/secrets plan. It is not production-ready until the implementation follow-ups are completed.

---

## 12. Phase E Entry Criteria

Phase E can start with this config baseline.

Phase E must define durable storage for:

1. match tickets and contracts;
2. game policy;
3. assistance state and consumables;
4. ECE payload/history pointers;
5. analysis memory;
6. ECR and ECOR;
7. calibration samples;
8. bot operations;
9. token ledger and entitlements;
10. audit events.

Phase E should also decide which config values are stored as admin settings versus durable data records versus deployment environment.
