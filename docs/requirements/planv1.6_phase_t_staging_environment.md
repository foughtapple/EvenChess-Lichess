# EvenChess Plan v1.6 Phase T - Staging Environment

## Phase Goal

Run a production-like staging deployment before any public EvenChess traffic.

Staging must prove that EvenChess-Lichess, ECE, databases, workers, reverse proxy, TLS, monitoring, admin controls, bots, tokens, overlays, ratings, and reset/rollback flows work together outside local Test Ground.

## Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- Appendix T: Operations, Deployment, and Incident Response
- Appendix V: Testing and QA Acceptance
- `docs/requirements/planv1.6_phase_c_architecture.md`
- `docs/requirements/planv1.6_phase_d_config_secrets.md`
- `docs/requirements/planv1.6_phase_e_persistence_migrations.md`
- `docs/requirements/planv1.6_phase_f_ece_deployment.md`
- `docs/requirements/planv1.6_phase_o_admin_operator_console.md`
- `docs/requirements/planv1.6_phase_q_security_abuse_fairness_controls.md`
- `docs/requirements/planv1.6_phase_r_telemetry_audit_privacy_retention.md`
- `docs/requirements/planv1.6_phase_s_ci_cd_build_release_automation.md`

## Staging Topology

Staging must mirror the planned production topology closely enough to catch deployment defects.

Required services:

- ECL web app;
- lila-ws or selected websocket runtime;
- background workers if separated by deployment;
- MongoDB or chosen durable database setup;
- Redis or selected queue/cache runtime;
- Elasticsearch/search if enabled by selected Lichess deployment;
- private ECE service;
- reverse proxy;
- TLS termination;
- monitoring/metrics/logging;
- admin/operator access;
- payment/token sandbox if monetisation is in scope.

Staging must not depend on:

- Windows PowerShell launch scripts;
- WSL Docker Desktop integration;
- local Test Ground panel;
- browser-direct ECE analysis calls;
- unauthenticated local admin/bot controls.

## Networking and TLS

Required staging network behavior:

- public users reach ECL only through the staging domain and reverse proxy;
- TLS is active for the staging domain;
- websocket routes work through the reverse proxy;
- ECE is private and reachable only from trusted backend callers;
- browser access to ECE board/proposed/game-review endpoints is blocked;
- ECE settings/diagnostic pages are private/operator-only where enabled;
- CLM/Test Ground/local diagnostic routes are not exposed publicly;
- public search/game JSON exposes only deployment-safe labels.

## Secrets and Environment

Staging must use staging-only secrets and provider limits:

- ECL private config;
- ECE private config;
- ECE base URL configured as backend-only private URL;
- AI/provider keys with staging quota limits where used;
- payment sandbox keys if monetisation is tested;
- token/ad sandbox settings if monetisation is tested;
- database credentials;
- admin bootstrap credentials;
- monitoring credentials;
- no production API keys unless explicitly approved for provider validation.

No secret, provider path, raw prompt, engine path, tablebase path, model weight path, or ECE private implementation detail may be committed to EvenChess-Lichess.

## Data and Migration Requirements

Staging must run migrations/storage setup from:

1. an empty database;
2. a prior staging snapshot.

Staging storage must prove durable repositories for:

- EvenChess search tickets;
- match contracts;
- game policy records;
- assistance state and Used Level;
- proposed/potential consumed counts and caches;
- ECOR active tables and rollback snapshots;
- ECOR calibration samples;
- bot runtime/config where needed;
- token ledger;
- live ECE history;
- requested analysis memory;
- audit ledger;
- admin settings and mutation audit.

If any repository is still in-memory/controller-local, staging must mark that subsystem as not deployable.

## ECE Validation

ECE staging validation must run:

- `/health`;
- `/ready`;
- provider validation;
- quick board-state smoke;
- deep/advanced board-state smoke;
- proposed-move smoke;
- game-review smoke if enabled;
- browser-origin/public-route rejection smoke;
- ECE restart survival test;
- ECL-to-ECE recovery test after ECE restart.

ECE must run as a Linux private service under a service manager or container. The staging test must not rely on WSL session lifetime.

## Staging Smoke Matrix

Minimum staging smoke tests:

- public home/search loads over TLS;
- admin account can access admin/operator pages;
- non-admin cannot access admin/operator pages;
- computer game starts and pieces move;
- online EvenChess search creates a stable search ticket;
- human-vs-human casual EvenChess game starts;
- rated EvenChess game starts only if Phase H/I gates are enabled;
- match contract creates Set Levels;
- ECE quick payload updates overlays without browser refresh;
- ECE deep payload updates eval bar only when deep/advanced data arrives;
- coach text updates on the student's turn and holds during opponent turn;
- toggles persist during the game;
- Used Level increases monotonically;
- proposed move sends ECE proposed-move call, caches, toggles, and clears after move;
- potential move reveal consumes server-side counter and caches/clears correctly;
- game end writes final Used Level, Used Offset, and rating/audit records;
- replay/review loads retained live ECE history when present;
- stale missing history behaves as no payload-backed overlays until analysis is requested;
- token grant/consume/refund flow works if enabled;
- bot fallback can match a human after low timeout when enabled;
- simulation bots can match humans and each other when enabled;
- bot/simulation stop controls stop cleanly;
- admin kill switches disable live help/proposed/potential/review/bots as expected;
- normal Lichess non-EvenChess game smoke still passes.

## Restart and Failure Drills

Staging must prove:

- ECL restart does not lose durable search/game/assistance/token/audit state;
- ECE restart does not break active games permanently;
- database restart recovers according to expected service behavior;
- Redis/cache restart behavior is understood and documented;
- websocket restart behavior is understood and documented;
- reverse proxy reload preserves active sessions where practical;
- ECE outage suppresses or degrades live help safely;
- asymmetric assistance outage can be no-rated/reviewed;
- stale payloads are rejected and audited;
- bot fallback/simulation kill switches work during incident mode;
- rollback to previous ECL artifact is possible;
- rollback to previous ECE artifact is possible;
- rollback does not silently change active rated fairness.

## Monitoring and Dashboards

Staging must show live values for:

- ECE health/latency/failures;
- Stockfish/provider readiness as reported by ECE;
- AI latency/cost/fallback where enabled;
- queue times;
- bot fallback state and pairing count;
- simulation bot state and active count;
- token flow;
- proposed/potential usage;
- overlay stale-payload errors;
- display render failures;
- rating settlement success/failure;
- ECOR calibration sample volume;
- admin setting changes;
- incident actions.

Dashboards may be rough in staging, but values must come from real staging events rather than static sample rows before public beta.

## Payment and Token Sandbox

If monetisation is in scope for staging:

- use sandbox payment provider keys;
- test checkout success;
- test checkout failure;
- test webhook verification;
- test subscription activation/deactivation;
- test token grant/consume/refund;
- test ad/reward token cap if enabled;
- verify paid status does not alter live rated coaching strength.

If monetisation is not in scope, token/payment public surfaces must be hidden or clearly disabled with deployment-safe copy.

## Reset and Cleanup Requirements

Staging must be resettable without ambiguity:

- stop services;
- clear or snapshot databases;
- clear search tickets;
- clear bot simulation tickets;
- clear token sandbox data where appropriate;
- clear ECE debug IO and local logs according to policy;
- reseed admin/test users;
- rerun migrations;
- rerun smoke.

Reset scripts must not be usable against production by accident. Environment names and destructive confirmations must be explicit.

## Known Environment Differences Register

Staging must maintain a register of differences from production:

- host size/resources;
- provider quotas;
- payment sandbox mode;
- bot policy;
- debug flags;
- retention durations;
- monitoring/alert recipients;
- domain/TLS certificate type;
- database replication/backup level;
- traffic volume.

Any difference that can hide a production bug must be marked as a release risk.

## Current Implementation State

### Existing Foundations

The current repo has staging-relevant foundations:

- Phase C defines a single Linux host/private-service beta topology, reverse proxy/TLS boundary, service map, and staging parity requirements.
- Phase D defines config/secrets separation and backend-only ECE URL requirements.
- Phase F defines ECE private deployment baseline, health/readiness/provider validation, smoke commands, and private networking requirements.
- Phase S documents build/test/package requirements and existing upstream-style CI workflows.
- Test Ground can validate local flows, but it is explicitly not a staging substitute.
- Admin/operator models exist for settings, bot controls, ECOR tables, audit/search, feature flags, and kill switches.

### Not Yet Release-Proven

Phase T is not release-complete until:

- an actual staging host/environment exists;
- ECL and ECE are deployed there as independent services;
- staging domain and TLS are configured;
- private ECE networking is validated;
- durable databases/stores are configured;
- migrations/storage setup runs from empty and snapshot states;
- ECE provider validation passes on staging resources;
- staging smoke matrix passes;
- restart/failure drills pass;
- dashboards read live staging events;
- bot fallback and simulation are tested in staging;
- payment/token sandbox is tested or explicitly out of scope;
- reset/cleanup procedure is tested;
- environment differences register is complete.

## Patch Map Impact

Future Phase T implementation may touch deployment scripts, service definitions, environment templates, reverse proxy config, admin bootstrap scripts, staging smoke scripts, or monitoring config. Runtime Lichess seam changes must be patch-mapped and entered into the integration log.

This Phase T documentation pass does not itself change runtime code.

## Phase T Acceptance Status

Phase T is conducted as a readiness and requirements pass.

Status:

- Staging environment requirements, smoke matrix, restart drills, ECE validation, monitoring, sandbox, and reset requirements are defined.
- Release readiness remains blocked until staging is actually provisioned, deployed, tested, monitored, and reset successfully.

## Phase U Entry Criteria

Before the automated test matrix can be treated as deployment evidence, staging must provide:

- a reachable staging domain;
- deployed ECL and private ECE;
- durable staging data stores;
- admin and test users;
- real staging smoke results;
- known environment differences;
- logs/screenshots or dashboard evidence for queue, ECE, overlays, bots, tokens, ratings, and admin controls.
