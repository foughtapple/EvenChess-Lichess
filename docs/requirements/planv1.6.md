# EvenChess-Lichess Public Deployment Readiness Plan v1.6

**Date:** 2026-06-03
**Repo:** EvenChess-Lichess / Lila fork
**Local path:** `/home/jayde/dev/lila-docker/repos/lila`
**Purpose:** Define the full deployment-readiness plan for taking EvenChess from local Test Ground development to a public, server-deployable product.

This document is a deployment-readiness plan. It is not a statement that the current codebase is ready to deploy. It records the work needed to make EvenChess usable by the public while preserving the EvenChess-Lichess/ECE boundary, Lichess compatibility, public fairness rules, auditability, and operational safety.

---

## 1. Source Requirements Used

Primary EvenChess-Lichess requirements used for this plan:

- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `docs/requirements/plan_version_1.1/PLAN.md`
- `docs/requirements/plan_version_1.1/PHASE_J_BOT_OPERATIONS_ADMIN.md`
- `docs/requirements/plan_analysis_memory/PLAN.md`
- `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`
- `docs/integration/EVENCHESS_INTEGRATION_LOG.md`

ECE boundary requirements used for this plan:

- `/home/jayde/dev/lila-docker/repos/ece/docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md`

Notes:

- The ECE requirements remain the single source of truth for ECE internals.
- ECL must call ECE server-to-server only.
- Browser/client code must not call ECE analysis endpoints.
- ECE provider paths, secrets, prompts, private engine implementation, raw provider outputs, tablebases, weights, generated caches, and private databases must not be copied into ECL.

---

## 2. Deployment Readiness Definition

EvenChess is deployable when all of the following are true:

1. Public EvenChess games can be created, played, completed, reviewed, and settled without corrupting normal Lichess behavior.
2. EvenChess matchmaking, Set Level assignment, Used Level tracking, Used Offset, ECR settlement, and bot fallback are server-authoritative.
3. ECE is deployed as a private backend service and is reachable only by trusted backend callers.
4. ECE quick/deep payloads, proposed-move payloads, potential-move reveal payloads, and retained analysis history are handled without stale rendering, browser-side permission decisions, or refresh exploits.
5. The public UI is polished, non-debug, responsive, accessible, and consistent with the existing Lichess experience.
6. Admin-only operations are actually admin-only and cannot be reached through unauthenticated Test Ground routes.
7. Tokens, subscriptions, ads, and premium benefits cannot alter rated fairness or leak stronger live assistance.
8. Audit, telemetry, abuse monitoring, privacy retention, backups, rollback, and incident procedures exist before public traffic.
9. All intended EvenChess implementation files are tracked selectively, patch-mapped where upstream seams are touched, and covered by focused tests.
10. A staging environment has passed release-candidate smoke, load, security, browser, and ECE-failure testing.

---

## 3. Current Readiness Review

The project has substantial local functionality and phase work, but it should be treated as pre-deployment until the items below are closed.

### 3.1 Critical Blockers Before Public Deployment

- **Git/release hygiene:** The working tree contains many EvenChess implementation files and generated/dirty changes. A release branch must include only intended files. Do not use `git add .`.
- **Production persistence:** Any current in-memory or local-only state for match tickets, bot operation state, game assistance state, proposed/potential move usage, ECE history, ECOR calibration samples, tokens, or debug logs must be moved to production-safe storage.
- **ECE private deployment:** ECE must run as a Linux private service reachable from ECL backend only, not as a Windows/WSL/Test Ground dependency.
- **Local assumptions:** WSL paths, Windows PowerShell scripts, `localhost` assumptions, Test Ground ports, and developer debug endpoints must not be part of public runtime paths.
- **Server authority:** Browser state must not control Set Level, Used Level, move-consumable counts, token use, rating settlement, bot operations, or ECE permission.
- **Matchmaking integration:** EvenChess matching must be fully integrated with the intended Lichess search lifecycle and ECR model, including bot fallback and simulation constraints.
- **Rating safety:** Normal Lichess ratings must remain isolated from EvenChess ECR unless a future approved migration explicitly changes that.
- **Admin controls:** Bot operations, ECOR calibration, token adjustments, ECE toggles, and operational switches must require admin permissions and CSRF protections.
- **Public disclosure:** Search and game surfaces must disclose EvenChess assistance and bot fallback clearly without showing internal debug details.
- **Observability:** Operators must see queue health, ECE health, ECE latency, stale payloads, token flow, rating settlement anomalies, bot operation status, and error budgets.
- **Rollback:** Operators must be able to disable ECE live help, AI summaries, proposed moves, potential moves, bot fallback, simulation, monetisation, and rated ECR settlement independently.

### 3.2 Recommended First Public Launch Scope

Use a staged public launch rather than enabling every feature on day one:

1. **Private staging:** Human-vs-human EvenChess, computer training, ECE quick/deep calls, overlays, coach card, admin settings, audit logs.
2. **Closed alpha:** Casual EvenChess plus replay/history, no public monetisation, bot simulation limited to staging/admin testing.
3. **Public beta:** Rated EvenChess ECR enabled, bot fallback optionally enabled with disclosure, tokens/subscriptions disabled or soft-launched behind admin flag.
4. **Monetised beta:** Tokens/subscriptions/ads enabled only after ledger, refund, quota, and abuse tests pass.
5. **General availability:** Full monitoring, support, moderation, backup, rollback, and ECOR calibration workflows in place.

---

## 4. Global Architecture Target

### 4.1 Services

Production should separate these responsibilities:

- **EvenChess-Lichess web app:** Public website, login, lobby, games, UI, game authority, ECR, tokens, admin.
- **Lichess runtime services:** Existing Lila dependencies such as MongoDB, Redis, Elasticsearch/search, lila-ws, workers, mail, asset pipeline, and reverse proxy.
- **ECE private service:** Private HTTP service for board-state quick/deep, proposed move, game review, provider orchestration, deterministic composer, and private caches.
- **Background workers:** Jobs for ECE history retention, full-game analysis, token grants, subscription sync, ECOR calibration, bot simulation, and cleanup.
- **Monitoring stack:** Logs, metrics, traces, alerts, dashboards, backup verification, and incident events.

### 4.2 Network Rules

- Public browser traffic reaches ECL only.
- ECL backend reaches ECE over a private network address.
- ECE analysis endpoints are never browser-callable.
- ECE operator/settings pages are local/private diagnostics only and must not be public internet surfaces.
- CORS should not allow arbitrary browser origins to ECE.
- ECL should use explicit service URLs from environment/config, not hardcoded WSL/Windows/local addresses.

### 4.3 Data Ownership

ECL owns:

- user accounts and sessions;
- matchmaking tickets and match contracts;
- Set Level, Used Level, Used Offset, assistance load;
- ECR and rating settlement;
- token/subscription/ads state;
- audit logs and coaching render logs;
- game history retention and requested analysis metadata;
- bot operation settings and public disclosure state.

ECE owns:

- deterministic chess calculations;
- provider orchestration;
- Stockfish, Syzygy, Maia, eval-cache, AI provider wrappers;
- deterministic composer;
- private FEN/side/level/profile cache;
- provider paths, secrets, prompts, raw provider output, and private diagnostics.

---

## 5. Required Production Data Models

The exact collection/table names may follow existing Lichess patterns, but these data domains must be durable and versioned.

### 5.1 Matchmaking and Game Authority

- `EvenChessSearchTicket`
- `EvenChessMatchContract`
- `EvenChessGamePolicy`
- `EvenChessFriendChallengePolicy`
- `EvenChessBotTicket`
- `EvenChessSimulationRun`

Required fields include policy version, queue mode, time control, rated/casual, preferred set level, assigned Set Levels, source of pairing, bot profile when applicable, uneven-match flag, expiry, and audit IDs.

### 5.2 Live Assistance State

- `EvenChessGameAssistanceState`
- `EvenChessMovePayloadPointer`
- `EvenChessConsumableState`

Required fields include game ID, ply/FEN key, side, Set Level, highest Used Level, quick payload status, deep payload status, latest accepted payload version, proposed move use count, potential move reveal counts, cached reveal state, and stale rejection reasons.

### 5.3 Analysis Memory

- rolling last 10 user games with attached live ECE history pointers;
- rolling last 100 requested full-game/custom analyses;
- paid saved-game exemptions where applicable;
- requested analysis key: game, perspective, White level, Black level, ECE version, policy version, AI flag.

### 5.4 Rating and Calibration

- `EvenChessEcrRating`
- `EvenChessRatingEvent`
- `EvenChessEcorTableSnapshot`
- `EvenChessBaseLevelTableSnapshot`
- `EvenChessCalibrationSample`
- `EvenChessCalibrationRun`

Calibration samples must retain the five required variables: White ECR, Black ECR, White Used Level, Black Used Level, and result. Store up to the latest 1,000,000 qualifying games by active policy.

### 5.5 Monetisation and Entitlements

- token ledger;
- subscription entitlement snapshot;
- ad reward grants;
- token spend/refund events;
- analysis quota events;
- paid save events;
- operator adjustment events.

Ledger-style accounting is required. Do not make token balances a mutable counter without an audit trail.

### 5.6 Audit and Operations

- match contract audit;
- Set Level and Used Level audit;
- ECE payload request/accept/reject audit;
- coaching render/hide/suppress audit;
- proposed/potential move audit;
- rating settlement audit;
- bot operation audit;
- admin config change audit;
- abuse/moderation action audit.

---

## 6. Phase Plan A-Z

Each phase must produce:

- implementation or documentation changes scoped to that phase;
- requirements updates if behavior changes;
- patch-map/integration-log updates for upstream/core Lichess seams;
- focused tests or a documented reason tests cannot run;
- rollback notes.

### Phase A - Requirements Freeze and Launch Scope

**Phase A output:** `docs/requirements/planv1.6_phase_a_scope_freeze.md`

**Phase A status:** Conducted as the v1.6 working scope freeze. Product-owner/operator sign-off remains required before production launch.

**Goal:** Freeze the public-deployment scope for v1.6 so implementation does not keep moving underneath release hardening.

Required work:

- Confirm the active requirements source list.
- Mark old/superseded requirements in Appendix Z before changing behavior.
- Decide first public launch scope: casual only, rated beta, bot fallback on/off, tokens on/off, AI on/off, proposed/potential moves on/off.
- Define feature flags for every major deployable subsystem.
- Define launch-stage gates: local, staging, closed alpha, public beta, GA.
- Record non-goals for v1.6, including ECE internals and upstream Lichess sync unless explicitly approved.

Acceptance gates:

- A signed-off v1.6 scope table exists.
- Every enabled launch feature has an owner, data model, tests, and rollback switch.
- Every deferred feature has a disabled-by-default flag or is removed from public navigation.

Risks/blockers:

- Scope creep can prevent deployment.
- Old local/Test Ground assumptions can accidentally become public behavior.

Evidence to update:

- requirements combined file;
- Appendix Z override register;
- integration log if policy changes affect seams.

### Phase B - Repository, Branch, and Patch Hygiene

**Phase B output:** `docs/requirements/planv1.6_phase_b_repo_hygiene.md`

**Phase B status:** Conducted on branch `codex/evenchess-v1.6-readiness`. Release hygiene remains blocked until stale staged files, untracked release candidates, old requirements archival, and patch-map coverage are selectively reconciled.

**Goal:** Make the codebase shippable from source control.

Required work:

- Create a release branch for v1.6.
- Audit `git status --short`.
- Classify dirty files as intended implementation, requirements/docs, generated assets, local config, or accidental edits.
- Selectively add intended files only. Do not use `git add .`.
- Ensure major EvenChess implementation files are tracked.
- Ensure generated build outputs, logs, `.env`, caches, provider data, tablebases, weights, and local databases are ignored.
- Review patch map for every upstream/core Lichess seam.
- Review integration log entries for current seams and rollback notes.

Acceptance gates:

- Release branch contains only intended source/docs/tests.
- No implementation file needed for EvenChess is untracked.
- No private ECE internals or generated provider data are tracked.
- Patch map and integration log are current.

Risks/blockers:

- Current local success can fail to ship if files remain untracked.
- Accidental staging of private or generated files is a deployment risk.

Evidence to update:

- `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`;
- `docs/integration/EVENCHESS_INTEGRATION_LOG.md`;
- `.gitignore` if needed.

### Phase C - Production Architecture and Infrastructure Design

**Phase C output:** `docs/requirements/planv1.6_phase_c_architecture.md`

**Phase C status:** Conducted as the v1.6 architecture baseline. The chosen beta topology is a single Linux host/private service deployment with ECL public behind a reverse proxy and ECE private on the internal network.

**Goal:** Define how ECL, ECE, databases, workers, and monitoring run on a server.

Required work:

- Choose hosting topology: single server, multi-service VM, container host, or managed orchestration.
- Define service map for ECL web, lila-ws, workers, MongoDB, Redis, Elasticsearch/search, ECE, reverse proxy, TLS, and monitoring.
- Define internal DNS/service names.
- Define resource requirements for ECE providers, especially Stockfish/Syzygy/AI.
- Define scaling assumptions and initial limits.
- Define blue/green or rolling deployment approach.
- Define staging and production environment parity.

Acceptance gates:

- Architecture diagram exists.
- Every service has CPU, memory, disk, port, health check, restart policy, and logs defined.
- Public and private network boundaries are explicit.
- Deployment does not depend on WSL, Windows paths, Docker Desktop, or local Test Ground.

Risks/blockers:

- ECE provider cost and latency can dominate infrastructure needs.
- A single host can be acceptable for early beta but must still have backups and observability.

Evidence to update:

- deployment runbook;
- environment variable inventory;
- service inventory.

### Phase D - Secrets, Config, and Environment Management

**Phase D output:** `docs/requirements/planv1.6_phase_d_config_secrets.md`

**Phase D status:** Conducted as the v1.6 config/secrets baseline. Production deployment remains blocked until ECL supports a backend-only production ECE base URL such as `http://ece:8787` without exposing it to browser bundles or public JSON.

**Goal:** Make production configuration safe, reproducible, and reviewable.

Required work:

- Inventory every environment variable used by ECL and ECE.
- Separate public config, private config, secrets, local dev defaults, staging, and production.
- Store secrets in a server secret manager or locked-down environment mechanism.
- Remove hardcoded local URLs from production paths.
- Define ECE base URL for backend only.
- Define feature flags and defaults.
- Define safe ECE debug settings: debug IO off by default in production.
- Ensure public JSON cannot expose secrets, provider paths, prompts, internal IDs, or raw diagnostics.

Acceptance gates:

- Production config can be generated without local machine paths.
- Secret values are not committed or printed to logs.
- Backend has a single authoritative ECE service URL.
- Browser bundles contain no private ECE endpoints.

Risks/blockers:

- Local debug variables can leak private paths or raw payloads.
- Misconfigured ECE host can expose analysis endpoints publicly.

Evidence to update:

- deployment env example with fake values;
- secret inventory;
- admin operations guide.

### Phase E - Database Persistence and Migration Strategy

**Phase E output:** `docs/requirements/planv1.6_phase_e_persistence_migrations.md`

**Phase E status:** Conducted as the v1.6 persistence and migration baseline. Production deployment remains blocked until in-memory search, game policy, assistance state, proposed/potential caches, ECOR samples, bot runtime, token ledger, and ECE history are replaced with durable repositories.

**Goal:** Persist all server-authoritative EvenChess state in production-safe storage.

Required work:

- Design schemas for match contracts, game policy, assistance state, consumables, ECE history, analysis memory, token ledger, ECR, ECOR snapshots, calibration samples, bot runs, and audit events.
- Add schema version fields and policy version fields.
- Write migrations or migration-safe write paths.
- Define indexes for game lookup, user history, queue matching, calibration scans, and admin dashboards.
- Define retention jobs and cleanup jobs.
- Confirm data does not corrupt normal Lichess game assumptions.

Acceptance gates:

- Refreshing the browser cannot reset Used Level, proposed move count, potential reveal count, token use, or match policy.
- Completed games retain the required payload history.
- Old records can be read safely after policy/table changes.
- Retention limits are enforced.

Risks/blockers:

- Editing core game BSON/schema internals is high risk and requires explicit approval and patch mapping.
- In-memory state is acceptable for local tests but not for production authority.

Evidence to update:

- migration notes;
- data model docs;
- tests for persistence and restart recovery.

### Phase F - ECE Production Service Deployment

**Phase F output:** `docs/requirements/planv1.6_phase_f_ece_deployment.md`

**Phase F status:** Conducted as the v1.6 ECE production service deployment baseline. Production deployment remains blocked until ECE runs under a service manager or container in staging, provider validation and smoke tests pass, ECL has production-safe backend-only ECE URL support, and browser access to ECE is blocked.

**Goal:** Deploy ECE as a private, stable, Linux service that ECL can call.

Required work:

- Deploy ECE from `/home/jayde/dev/lila-docker/repos/ece` or production equivalent.
- Run ECE under a service manager or container with restart policy.
- Configure ECE host/port for private backend access.
- Configure provider paths, cache paths, API keys, timeouts, and mode via secrets/config.
- Validate `/health` and `/ready`.
- Validate quick/deep board endpoints.
- Validate proposed move and game review endpoints.
- Validate providers using ECE-owned scripts.
- Disable public exposure of ECE analysis endpoints.
- Confirm ECE logs redact secrets and private paths.

Acceptance gates:

- ECE survives process restart and deploy restart.
- ECL backend can call ECE; browser cannot call ECE analysis endpoints.
- ECE readiness reports provider status.
- ECE debug IO is disabled by default and can be enabled only by operator action.

Risks/blockers:

- Provider binaries, tablebases, weights, and DBs are large and private.
- AI provider latency/cost can affect game responsiveness.

Evidence to update:

- ECE deployment runbook;
- provider validation report;
- ECL/ECE integration smoke logs.

### Phase G - ECL-to-ECE Gateway Hardening

**Phase G output:** `docs/requirements/planv1.6_phase_g_ecl_ece_gateway_hardening.md`

**Phase G status:** Conducted as the v1.6 ECL-to-ECE gateway hardening baseline. The current code has useful quick/deep, stale-validation, proposed-move, and server-side contract foundations, but production remains blocked until gateway HTTP handling is extracted from controller helpers, production private ECE service URLs are supported without browser exposure, ECE URLs are removed from browser JSON, circuit breaker/backpressure/durable payload state are implemented, and browser tests prove move-triggered overlay updates without refresh.

**Goal:** Make ECL's server-side ECE integration reliable under public traffic.

Required work:

- Centralize ECE calls in a backend adapter/gateway.
- Enforce server-side authorization before each ECE request.
- Implement request IDs, game IDs, ply/FEN keys, side, Set Level, Used Level, and policy versions.
- Implement stale-response rejection.
- Implement quick/deep request handling.
- Ignore quick placeholder eval values for eval bar/status; update eval only from accepted deep payload.
- Implement timeouts, retries, circuit breakers, and backpressure.
- Implement graceful degraded states when ECE is down.
- Ensure potential/candidate data is suppressed until a server-authorized reveal.
- Ensure proposed-move result uses `proposed_move_evaluation.after_move_side_output` only for legal proposed moves.

Acceptance gates:

- A move triggers ECE update and accepted payloads render without page refresh.
- ECE failures do not break legal move play.
- Opponent-turn text remains stable until the player's turn, while overlays update from board facts.
- Proposed illegal move does not clear the current overlay.
- Proposed legal move caches/toggles between current payload and proposed post-move payload until a move is made.

Risks/blockers:

- Race conditions between quick and deep responses can create stale overlays.
- Browser retries must not become direct ECE calls.

Evidence to update:

- gateway tests;
- stale-payload tests;
- overlay visual/browser tests;
- integration log.

### Phase H - Matchmaking and MMR Completion

**Phase H output:** `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`

**Phase H status:** Conducted as the v1.6 matchmaking/MMR completion baseline. The current code has the simplified Preferred Level model, ECOR/base-level assignment foundations, friend-level contract logic, bot fallback scaffolding, simulation bot controls, and unit-test coverage. Public deployment remains blocked until the native Lichess game-creation/search seam is proven end-to-end, search/contracts are durable, friend challenges use an explicit friend contract path, bot fallback and simulation are browser-tested, public search JSON is redacted, and all lobby/search/friend/game seams are patch-mapped.

**Goal:** Make EvenChess search and matching production-authoritative.

Required work:

- Finalize the two-state public search model: Preferred Level dropdown with `Any` and `0` to `10`.
- `Any` means no preference; ECL computes rating windows and assigned Set Levels.
- `0` to `10` fixes the requester's preferred Set Level; opponent level is assigned through ECOR.
- Remove opponent target, both-target, strict wait, and manual rating-window controls from public search.
- Compute search windows from the ECOR adjacent-gap table and base-level-by-rating table.
- Create authoritative match contracts before native game creation.
- Integrate friend challenge level policy separately from generic pool search.
- Ensure bot fallback and simulation tickets use the same contract path.
- Ensure public search keeps native Lichess search UX, with debug card hidden behind a local/debug flag.

Acceptance gates:

- Human-vs-human search can pair and redirect into an EvenChess game.
- Search settings persist as normal user preference, not a debug-only field.
- Backend ignores stale/legacy `any`, opponent target, and strict search fields.
- Friend challenge carries intended level settings to the recipient.
- Search JSON does not expose internal ticket IDs, policy internals, or raw diagnostics.

Risks/blockers:

- Native Lichess matching is complex. Upstream seams must be minimal and patch-mapped.
- Bot fallback can hide human matching bugs if enabled too early.

Evidence to update:

- matchmaking unit tests;
- integration search tests;
- UI serialization tests;
- patch map for lobby/search seams.

### Phase I - ECR Settlement and ECOR Calibration

**Phase I output:** `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`

**Phase I status:** Conducted as the v1.6 ECR settlement and ECOR calibration baseline. The current code has ECR isolation foundations, effective-rating settlement math using Used Offset, ECOR/base-level table parsing, calibration sample/recommendation logic, admin-state scaffolding, and unit tests. Public deployment remains blocked until real completed-game settlement is hooked into the Lichess result lifecycle, ECR/ECOR/calibration samples are durable, admin activate/revert workflows are proven, settlement is idempotent, and integration tests prove normal Lichess ratings remain untouched.

**Goal:** Make EvenChess rating fair, auditable, and calibratable.

Required work:

- Keep EvenChess ECR separate from normal Lichess ratings.
- Adjust expected-score inputs using each player's actual Used Level and Used Offset.
- Use active ECOR table snapshot for settlement.
- Store settlement policy version and ECOR version on each rated EvenChess game.
- Record uneven-match flags.
- Build ECOR calibration sample collection from the latest 1,000,000 qualifying games.
- Build admin calibration controls to run history analysis, display recommended adjacent-gap values, standard deviation, confidence/fit metrics, and comparison against current table.
- Store ECOR snapshots with revert history.
- Store base-level-by-rating table snapshots with revert history.

Acceptance gates:

- Higher Used Level increases effective rating input and changes rating gain/loss accordingly.
- Casual, computer, target/training, review, and simulation-only games do not incorrectly update rated ECR.
- Admin can view and edit ECOR/base-level tables.
- Admin can revert to a prior table snapshot.
- Calibration does not automatically change production tables without admin action.

Risks/blockers:

- Early data will be noisy. Do not overfit the ECOR table from small samples.
- Changing ECOR affects fairness and must be policy-versioned.

Evidence to update:

- rating tests;
- calibration tests;
- admin tests;
- audit log entries.

### Phase J - Game Policy and Live Assistance Authority

**Phase J output:** `docs/requirements/planv1.6_phase_j_game_policy_live_assistance_authority.md`

**Phase J status:** Conducted as the v1.6 game policy and live assistance authority baseline. The current code has game policy, server-owned coaching decisions, Set Level caps, monotonic Used Level accounting, Used Offset derivation, audit event models, live ECE history models, preferred starting Used Level settings, and unit coverage. Public deployment remains blocked until game policy and assistance ledgers are durable, every real game creation path writes policy before render, live Used Level state is atomic across refresh/multi-tab, higher-level toggle authorization triggers ECE refresh, coach text turn-gating is persisted, and browser/integration tests prove the full live path.

**Goal:** Ensure every live game has a server-authoritative EvenChess policy.

Required work:

- Attach EvenChess policy to game creation.
- Store Set Level for each side at game start.
- Store Used Level monotonic increases.
- Store Used Offset from actual Used Level.
- Enforce Set Level caps server-side.
- Make level toggles display controls only; toggling on a higher feature raises Used Level and triggers an ECE refresh if needed.
- Ensure lowering/toggling off display items does not lower Used Level.
- Persist preferred starting level from user settings, clamped to Set Level.
- Ensure live coaching text updates only when it becomes the student's turn.

Acceptance gates:

- Browser refresh preserves Used Level and assistance state.
- Toggle changes affect display immediately without overriding server authorization.
- Used Level cannot decrease in a live game.
- Set Level cannot be exceeded by client input.
- Every coaching render/hide/suppress event is auditable.

Risks/blockers:

- Mixing display toggles with authority state can create exploits.
- Multiple tabs can race level increases and consumables.

Evidence to update:

- assistance-state tests;
- multi-tab/reload tests;
- audit tests.

### Phase K - Board Overlay, Coach Card, and UI Polish

**Goal:** Make the public board experience stable, clear, and responsive.

Required work:

- Keep eval bar height flush with board top/bottom.
- Keep level card positioned under the left-side card and flush with board bottom.
- Keep coach card top aligned with board top.
- Ensure layout ratios hold under browser zoom and common screen sizes.
- Render Offset Count from every delivered side-output item using target square and signed delta.
- Render equal offsets as blue shield.
- Render student-favorable offsets as green numbered circles.
- Render opponent-favorable offsets as red numbered circles.
- Render student threats and opponent threats with separate toggles.
- Render pins in top-left.
- Render non-attackable loose/hanging pieces as orange bottom-left exclamation.
- Render student-owned attackable hanging as red bottom-left exclamation plus red inner rim.
- Render opponent-owned attackable hanging as purple bottom-left exclamation plus purple inner rim.
- Remove duplicated overlay-text chips from the coach side.
- Keep cards fixed-size enough to avoid jumping.
- Update changed overlay atoms without visible full clear/repaint flicker where practical.

Acceptance gates:

- Overlays appear after moves without page refresh.
- Toggling each overlay family on/off works without scroll jumping.
- Computer games, live games, replay, and analysis use the same shell where allowed.
- Board interaction is not blocked by overlay layers.
- Mobile/tablet/desktop screenshots meet layout requirements.

Risks/blockers:

- Chessground and Lichess round templates are sensitive upstream seams.
- Overlay hit testing can accidentally block moves.

Evidence to update:

- browser screenshot tests;
- overlay unit tests;
- manual visual checklist;
- patch map for round/chessground seams.

### Phase L - Proposed and Potential Move Consumables

**Phase L output:** `docs/requirements/planv1.6_phase_l_proposed_potential_consumables.md`

**Phase L status:** Conducted. Current proposed/potential UI and test-ground scaffolding exists, including nested proposed `after_move_side_output` handling and server-side potential filtering in the normal board payload path. Release completion remains blocked on durable atomic consumable storage, production live-game endpoints that derive state from server authority, server-side legal move proof, move-lifecycle invalidation, persisted audit events, and refresh/multi-node tests.

**Goal:** Make proposed and potential move help useful, level-gated, cached, and non-exploitable.

Required work:

- Store proposed-move use counts server-side.
- Store potential-move reveal counts server-side.
- Enforce per-game quota by Used Level.
- Allow proposed-move call only for exactly one green arrow that maps to one legal move.
- Illegal/invalid proposed move keeps current overlay unchanged.
- Legal proposed move calls ECE `/v1/ece/proposed-move`.
- Display proposed coaching text in its own card under/near coach content.
- Use nested `proposed_move_evaluation.after_move_side_output` for post-move deterministic overlay state.
- Cache the proposed result until a real move is made.
- Proposed button toggles between cached current board payload and cached proposed post-move payload without spending another use.
- Potential move buttons distinguish student-turn and opponent-side intent correctly.
- Potential move reveals show text at the bottom of the coach card and arrows/overlays only after server reveal.
- Potential reveal data clears after a move.

Acceptance gates:

- Browser refresh cannot restore spent uses.
- Re-clicking a cached reveal does not spend another use.
- Opponent potential moves are for the opponent side, not the current user side.
- Unauthorized potential data is not present in normal board payloads returned to browser.

Risks/blockers:

- Potential moves are ECE-provided board facts but ECL owns reveal permission.
- Proposed post-move overlays must not be treated as a new board-state response.

Evidence to update:

- server consumable tests;
- proposed-move tests;
- potential-reveal tests;
- browser interaction tests.

### Phase M - Analysis Memory and Review Modes

**Phase M output:** `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`

**Phase M status:** Conducted. Current domain/test foundations exist for rolling last-10 recent game memory, last-100 requested analysis memory, level-keyed full-game analysis, and mode-neutral overlay shells. Release completion remains blocked on durable repositories/migrations, live ECE history capture, native replay/analysis UI integration, requested full-game analysis jobs, token/quota idempotency, retention cleanup, paid saved-game exceptions, and browser replay tests.

**Goal:** Make replay and analysis reuse existing ECE payloads without unnecessary recalculation.

Required work:

- Store the last 10 completed games per user with live ECE history.
- Store the last 100 requested full-game/custom analyses per user.
- Key analyses by game, perspective, White/Black levels, ECE version, policy version, and AI flag.
- Use the same overlay shell in live, computer, analysis, study/review, and replay contexts.
- If retained payload exists, display it while allowing local display toggles.
- If retained payload does not exist, show shell but require analysis request.
- Ensure review-mode Used Level/session selections do not mutate original live game state.
- Implement retention cleanup and paid saved-game exceptions.

Acceptance gates:

- Opening a retained game can step through moves and show attached payloads.
- Missing retained history does not fabricate payloads.
- Full-game analysis consumes correct tokens/quota where enabled.
- Full-game analysis does not alter live ECR settlement.

Risks/blockers:

- Analysis storage can grow quickly.
- Stored ECE payloads must avoid raw provider/private data.

Evidence to update:

- retention tests;
- replay lookup tests;
- review UI tests.

### Phase N - Tokens, Subscriptions, Ads, and Entitlements

**Phase N output:** `docs/requirements/planv1.6_phase_n_tokens_subscriptions_ads_entitlements.md`

**Phase N status:** Conducted. Current policy/domain/UI foundations exist for plans, token grants, rewarded-ad grants, token settlement/refunds, review tokens, saved-game retention, fairness boundaries, account dashboard copy, and admin feature switches. Release completion remains blocked on durable entitlement and ledger storage, real persisted account lookup, provider webhook verification/idempotency, game lifecycle token reservation/settlement, rewarded-ad callback integration, support/admin correction tooling, and end-to-end payment/token tests.

**Goal:** Make monetisation deployable without changing rated fairness.

Required work:

- Define free, paid, token, ad, and subscription entitlements.
- Use ledger-based token accounting.
- Define token grants, spends, refunds, expiry, and operator adjustments.
- Gate high-cost ECE actions such as custom/full-game L10 analysis.
- Ensure premium never grants stronger live rated assistance.
- Integrate payment provider only after secrets/config and webhook verification are ready.
- Add entitlement snapshots to relevant audit events.
- Build user-facing billing/account pages with production copy.
- Build support/admin tools for token corrections and subscription troubleshooting.

Acceptance gates:

- Token balance cannot be forged by browser.
- Failed ECE full-game analysis can refund or not spend according to policy.
- Subscription webhook replay is idempotent.
- Paid status does not affect live rated Set Level or ECR fairness except approved quotas/storage.

Risks/blockers:

- Launching paid features without ledger and refunds creates support and trust risk.
- Ads/rewards can be abused for token farming.

Evidence to update:

- payment webhook tests;
- ledger tests;
- entitlement tests;
- support runbook.

### Phase O - Admin and Operator Console

**Phase O output:** `docs/requirements/planv1.6_phase_o_admin_operator_console.md`

**Phase O status:** Conducted. Current admin foundations exist for `/dev/settings`, `/dev/evenchess/ops`, bot operations, ECOR controls, conservative settings, secret redaction, dashboard scaffolding, and unit-tested admin models. Release completion remains blocked on durable config-change audit, live telemetry-backed dashboards, backend-safe production ECE monitoring, token adjustment tooling, admin auth/CSRF integration tests, real audit search, and proof that Test Ground cannot bypass production admin.

**Goal:** Give operators safe controls for deployment without exposing debug panels to normal users.

Required work:

- Ensure `/dev/settings` or equivalent admin pages require correct admin permission.
- Add admin controls for feature flags, ECE status, bot settings, ECOR tables, calibration runs, token adjustments, and release kill switches.
- Display ECE health/readiness through backend-safe calls.
- Link local-only ECE settings from Test Ground only, not public navigation.
- Remove Test Ground-only controls from production pages.
- Add CSRF protection to mutating admin operations.
- Add admin audit events for every config change.
- Add read-only monitoring panels for queue health, bot health, ECE latency, token flow, and rating settlement.

Acceptance gates:

- Non-admin users cannot access bot/admin settings.
- Test Ground cannot bypass production admin auth.
- Admin changes are versioned and auditable.
- Operators can disable risky systems without a deploy.

Risks/blockers:

- Bot controls affect public matchmaking and must not be local unauthenticated actions.
- Admin pages can leak internal IDs or paths if not scrubbed.

Evidence to update:

- admin authorization tests;
- CSRF tests;
- admin audit tests.

### Phase P - Bot Matchmaking and Simulation Operations

**Phase P output:** `docs/requirements/planv1.6_phase_p_bot_matchmaking_simulation_operations.md`

**Phase P status:** Conducted. Current bot fallback and simulation scaffolding exists across bot operations, matchmaking tickets, admin controls, and unit tests. Release readiness remains blocked until native Lichess game creation, clocks, move execution, simulation game completion/requeue, admin audit, and public disclosure are proven end-to-end in staging.

**Goal:** Make platform bots usable for low-population matchmaking and simulation without pretending implementation details are human accounts.

Required work:

- Keep bot fallback admin-controlled.
- Keep simulation mode admin/staging controlled unless explicitly approved for production fill.
- Match bot strength from bot equivalent rating tables, not directly from EvenChess level.
- Use admin-editable Stockfish/Lichess AI equivalent-rating table.
- For no-preference human search, choose bot rating near user's effective target.
- For preferred-level search, account for preferred level and ECOR offset when choosing bot profile.
- Preserve requested time controls and clocks.
- Use native Lichess AI/bot game mechanics where feasible rather than recreating chess play.
- Add randomized human-like search timing and move timing only within fair/deployable constraints.
- Ensure simulation bots queue, get paired, play, finish, and requeue according to configured population behavior.
- Resize simulation population when admin changes target count.
- Clearly disclose bot fallback status on public search surfaces when enabled.

Acceptance gates:

- With matchmaking bots enabled and timeout set low in staging, a human search gets a bot game.
- With matchmaking bots disabled and simulation bots enabled, human searches can pair with active simulation tickets.
- Simulation bots can pair with each other and complete games in staging.
- Bot games preserve clocks and time controls.
- Bot operations can be stopped cleanly.

Risks/blockers:

- Bot-vs-human behavior can undermine trust if disclosure is unclear.
- Autonomous bot game creation and move execution must use native Lichess-safe paths.

Evidence to update:

- bot fallback tests;
- simulation tests;
- admin operation tests;
- public disclosure tests.

### Phase Q - Security, Abuse, and Fairness Controls

**Phase Q output:** `docs/requirements/planv1.6_phase_q_security_abuse_fairness_controls.md`

**Phase Q status:** Conducted. Security, abuse, trust, incident, and admin-control models/tests exist, but release readiness remains blocked until controls are wired into production routes, runtime telemetry, Lichess moderation/rating seams, durable admin audit, rate limits, and incident drills.

**Goal:** Protect public EvenChess from common abuse, cheating, and operational misuse.

Required work:

- Publish clear rules: EvenChess platform coaching is allowed only inside EvenChess because it is disclosed, capped, logged, and priced into ECR.
- Prohibit outside engines, humans, browser extensions, unaudited notes, and stream chat in rated EvenChess.
- Rate-limit search, ECE calls, proposed move calls, potential reveals, custom analysis, token grants, and admin mutations.
- Monitor repeat pairings, queue sniping, target-level manipulation, collusion, abort abuse, and rating transfer.
- Prevent bot fallback from being farmed.
- Ensure AI cannot expose forbidden best moves where policy prohibits exact-answer coaching.
- Ensure ECE/provider raw outputs are never public.
- Enforce secure cookies, CSRF, CSP, same-origin rules, and admin authorization.
- Add abuse response flows: warn, shadow limit, block rated, annul/no-rate, refund tokens, disable assistance.

Acceptance gates:

- Security review has no known public ECE analysis path.
- Rated EvenChess abuse events are logged and actionable.
- Admin routes are not discoverable/useful to non-admin users.
- Feature kill switches work during an incident.

Risks/blockers:

- Assisted chess requires clearer disclosure and abuse policy than ordinary chess.
- External engine abuse is harder to distinguish because platform assistance exists.

Evidence to update:

- security checklist;
- abuse runbook;
- moderation docs;
- rate-limit tests.

### Phase R - Telemetry, Audit, Privacy, and Retention

**Phase R output:** `docs/requirements/planv1.6_phase_r_telemetry_audit_privacy_retention.md`

**Phase R status:** Conducted. Telemetry, audit, retention, privacy, dashboard, and calibration models/tests exist, but release readiness remains blocked until durable persistence, real route/service event emission, dashboard ingestion, retention pruning, redaction validation, and privacy/export/delete policy are implemented and tested in staging.

**Goal:** Make production behavior observable while respecting privacy and data minimisation.

Required work:

- Emit metrics for ECE latency, Stockfish latency, AI latency/cost, ECE failures, stale payload rejections, overlay render failures, queue times, bot pairing rates, token flow, custom analysis use, rating settlement health, and calibration sample volume.
- Emit audit logs for match contracts, Set Level, Used Level increases, ECE payloads, coaching render/hide/suppress, proposed/potential calls, final Used Level, Used Offset, and settlement.
- Define log retention.
- Define PII handling and export/delete policy where required.
- Redact secrets, provider paths, raw prompts, raw provider outputs, and private ECE internals.
- Keep ECE debug IO off in production unless temporarily enabled by operator for a specific incident.
- Add dashboard panels and alert thresholds.

Acceptance gates:

- Operators can answer: is ECE healthy, are searches pairing, are bots active, are overlays stale, are tokens flowing, and are ratings settling correctly.
- Audit records can reconstruct why a game got its Set Levels and final rating adjustment.
- Private provider data is not in public logs.

Risks/blockers:

- Excessive payload logging can create privacy and storage risk.
- Insufficient logging makes disputes and rating bugs impossible to diagnose.

Evidence to update:

- telemetry event catalog;
- dashboard screenshots/links;
- retention policy.

### Phase S - CI/CD, Build, and Release Automation

**Phase S output:** `docs/requirements/planv1.6_phase_s_ci_cd_build_release_automation.md`

**Phase S status:** Conducted. Upstream-style CI workflows and local Test Ground build separation exist, but release readiness remains blocked until EvenChess-specific CI coverage, fresh-environment proof, repository/deployment-target updates, operator build-version visibility, release checklist automation, and rollback documentation are complete.

**Goal:** Make builds and deployments repeatable without manual local steps.

Required work:

- Define build commands for ECL frontend/backend assets.
- Define test commands for Scala, TypeScript, UI, and integration tests.
- Ensure sbt, Java, Node, pnpm, and other dependencies are installed in CI.
- Separate build from launch in local Test Ground and production deploy scripts.
- Produce build version metadata and build timestamp.
- Fail CI on lint/type/test failures.
- Add artifact packaging or deployment script.
- Add release checklist automation where practical.

Acceptance gates:

- A fresh environment can build without WSL-specific manual fixes.
- Launch does not compile unexpectedly in production.
- Build timestamp/version is visible to admin/operators.
- CI catches module alias/test-runner issues.

Risks/blockers:

- Lichess build is large. CI must be realistic about runtime and caching.
- Local fixes can hide missing CI dependencies.

Evidence to update:

- CI config;
- build runbook;
- Test Ground build/launch docs.

### Phase T - Staging Environment

**Phase T output:** `docs/requirements/planv1.6_phase_t_staging_environment.md`

**Phase T status:** Conducted. Staging environment requirements, smoke matrix, restart drills, ECE validation, monitoring, sandbox, and reset requirements are defined. Release readiness remains blocked until staging is actually provisioned, deployed, tested, monitored, and reset successfully.

**Goal:** Run a production-like staging deployment before public traffic.

Required work:

- Deploy ECL, ECE, databases, workers, reverse proxy, and monitoring to staging.
- Use staging domain and TLS.
- Use staging secrets and provider limits.
- Seed admin account and test users.
- Enable debug-only tools only behind admin/local flags.
- Run staging migrations from empty database and from prior snapshot.
- Run ECE provider validation.
- Run bot fallback and simulation in staging.
- Run payment/token sandbox if monetisation is in scope.

Acceptance gates:

- Staging survives service restarts.
- ECL can play games after ECE restart.
- Queue, bot, token, rating, overlay, and admin dashboards work.
- Staging can be reset safely.

Risks/blockers:

- Staging that differs too much from production will miss deployment bugs.
- Provider availability can differ between local and server.

Evidence to update:

- staging deploy notes;
- smoke test report;
- known environment differences.

### Phase U - Automated Test Matrix

**Phase U output:** `docs/requirements/planv1.6_phase_u_automated_test_matrix.md`

**Phase U status:** Conducted. Current test inventory and required release matrix are defined. Release readiness remains blocked until the matrix runs in CI and browser/staging tests prove integrated game flow, ECL/ECE interaction, bots, clocks, ratings, tokens, audit, admin controls, and normal Lichess regression behavior.

**Goal:** Lock down the highest-risk EvenChess behavior with tests.

Required work:

- Backend tests for matchmaking, Set Level assignment, ECOR offsets, ECR settlement, bot tickets, simulation tickets, friend challenge policies, assistance state, consumables, and audit logs.
- ECE adapter tests for quick/deep, stale rejection, deep eval handling, proposed nested output, potential suppression/reveal, timeout and retry behavior.
- UI tests for search card, preferred level dropdown, level toggles, coach card, eval strip, board overlays, proposed/potential buttons, and no debug card in public search.
- Browser tests for move flow, overlay updates after move without refresh, legal/illegal proposed arrows, potential reveal cache, layout at common viewports, and board clickability.
- Admin tests for permissions, CSRF, ECOR tables, bot controls, token controls, and feature flags.
- Regression tests for normal Lichess play.

Acceptance gates:

- Test suite runs in CI.
- Known failing tests are either fixed or explicitly quarantined with issue links and no release-blocking ambiguity.
- Normal Lichess behavior still passes core regression tests.

Risks/blockers:

- Browser automation must interact correctly with Chessground.
- Test ECE fixtures must mirror real ECE contract changes.

Evidence to update:

- test matrix doc;
- CI results;
- browser screenshots.

### Phase V - Browser, Device, and Performance QA

**Phase V output:** `docs/requirements/planv1.6_phase_v_browser_device_performance_qa.md`

**Phase V status:** Conducted. Browser/device/performance QA matrix and evidence requirements are defined. Release readiness remains blocked until the QA matrix is executed and evidence confirms board playability, stable overlays, responsive layout, failure handling, accessibility, performance, and normal Lichess regression behavior.

**Goal:** Verify public usability across realistic devices and network conditions.

Required work:

- Test desktop Chrome, Edge, Firefox, and Safari where feasible.
- Test mobile/tablet layout for play, search, coach, level card, and overlays.
- Test browser zoom and responsive ratios.
- Test slow ECE, failed ECE, slow deep payload, and AI timeout.
- Test memory usage in long sessions.
- Test asset bundle size and load time.
- Test WebSocket/round update behavior.
- Test screen-reader labels and keyboard navigation for level controls and buttons.

Acceptance gates:

- Board remains playable under overlays.
- UI does not jump when toggles change or payloads arrive.
- Moves, clocks, and game end flow work under realistic latency.
- Performance budgets are documented and met for public beta.

Risks/blockers:

- Heavy overlay rendering can degrade weaker devices.
- Layout that works locally can break on mobile and zoomed desktop.

Evidence to update:

- QA checklist;
- screenshot set;
- performance report.

### Phase W - Data Backup, Recovery, and Maintenance Jobs

**Phase W output:** `docs/requirements/planv1.6_phase_w_backup_recovery_maintenance_jobs.md`

**Phase W status:** Conducted. Backup, restore, retention, cleanup, archive, and recovery requirements are defined. Release readiness remains blocked until durable stores, backup jobs, restore tests, retention jobs, protected saved-game behavior, token/rating recovery, and maintenance telemetry are implemented and proven in staging.

**Goal:** Avoid losing ratings, game history, paid entitlements, or calibration data.

Required work:

- Define backup schedule for MongoDB and other stateful stores.
- Define restore procedure and test it.
- Define retention jobs for recent-game payload history, requested analyses, logs, calibration samples, bot stale tickets, expired searches, and token ledger archives.
- Define disaster recovery target: RPO/RTO.
- Define maintenance windows and zero-downtime expectations.
- Define data export needed for support and calibration review.

Acceptance gates:

- A staging restore test succeeds.
- Retention jobs do not delete paid saved games or active analyses.
- Calibration data cap is enforced.
- Token ledger is recoverable and auditable.

Risks/blockers:

- Analysis payloads and calibration samples can grow quickly.
- Rating/token data loss is a trust-critical incident.

Evidence to update:

- backup runbook;
- restore test report;
- retention job tests.

### Phase X - Legal, Policy, and Public Copy

**Phase X output:** `docs/requirements/planv1.6_phase_x_legal_policy_public_copy.md`

**Phase X status:** Conducted. Public rules, disclosure, terms/privacy, support FAQ, forbidden-copy, and release-evidence requirements are defined. Release readiness remains blocked until actual public pages, rendered copy review, terms/privacy drafts, bot disclosure proof, support FAQ, payment-copy review, and legal/product sign-off are completed.

**Goal:** Make public-facing rules, disclosures, and site copy match the assisted-chess model.

Required work:

- Write EvenChess rules page.
- Explain Set Level, Used Level, Used Offset, ECR, bot fallback, and platform assistance at a user-friendly level.
- Explain that normal outside assistance is prohibited in rated EvenChess.
- Explain bot fallback disclosure when low player pool mode is enabled.
- Explain token/subscription terms if monetisation is enabled.
- Explain privacy and data retention for ECE history, audits, calibration, and analysis.
- Update landing/search copy to remove debug wording.
- Confirm no page implies hidden cheating, pay-to-win live help, or unrestricted engine access.

Acceptance gates:

- Public pages make assistance and rating model clear.
- Search cards disclose bot fallback status without debug internals.
- Terms/privacy cover retained analysis and audit data.
- Support FAQ exists for common questions.

Risks/blockers:

- Ambiguous disclosure can damage user trust.
- Payment terms must be correct before paid launch.

Evidence to update:

- public copy review;
- rules page;
- terms/privacy draft.

### Phase Y - Release Candidate, Freeze, and Rollback

**Phase Y output:** `docs/requirements/planv1.6_phase_y_release_candidate_freeze_rollback.md`

**Phase Y status:** Conducted. RC freeze rules, source/build/staging/security/backup checklists, rollback runbook, known-issue acceptance process, release report template, and go/no-go gate are defined. The current repository is not RC-ready because the working tree is dirty/untracked/staged and earlier phase release blockers remain unresolved.

**Goal:** Produce a release candidate that can be deployed or rolled back with confidence.

Required work:

- Freeze implementation except release-blocking fixes.
- Run full CI.
- Run full staging smoke.
- Run load test with ECE quick/deep and bot simulation.
- Run security checklist.
- Run backup/restore check.
- Verify feature flags and kill switches.
- Verify patch map and integration log.
- Verify no untracked required implementation files.
- Verify no private ECE internals/secrets in ECL.
- Create rollback plan for app deploy, ECE deploy, DB migration, feature flags, payment/tokens, bots, and rating settlement.

Acceptance gates:

- Release candidate checklist is complete.
- Rollback has been tested or dry-run.
- Known issues are documented and accepted.
- Public launch decision is explicit.

Risks/blockers:

- Last-minute broad rewrites create untested release risk.
- DB migrations can make rollback harder unless planned.

Evidence to update:

- RC report;
- release notes;
- rollback runbook.

### Phase Z - Production Launch, Monitoring, and Iteration

**Phase Z output:** `docs/requirements/planv1.6_phase_z_production_launch_monitoring_iteration.md`

**Phase Z status:** Conducted. Launch preconditions, conservative launch scope, launch sequence, monitoring dashboards, operator coverage, first-game review, rollback/pause triggers, support workflow, ECOR calibration policy, launch log, and post-launch review process are defined. Actual production launch remains blocked until Phase Y gates are satisfied and an explicit `GO` or approved private-beta `CONDITIONAL GO` exists.

**Goal:** Launch safely, monitor closely, and iterate based on real usage.

Required work:

- Deploy with conservative feature flags.
- Watch ECE health, queue times, bot fallback usage, stale payloads, overlay errors, token events, rating settlements, payment events, and server resources.
- Keep operator on call during launch window.
- Review first games manually for Set Level, Used Level, overlays, ECE payload acceptance, and settlement correctness.
- Review bot fallback disclosure and usage.
- Review support reports daily during beta.
- Run ECOR calibration only after enough data exists; do not auto-apply recommendations.
- Keep a rapid rollback path open during first launch period.
- Create post-launch issue triage loop.

Acceptance gates:

- Public games complete successfully.
- No normal Lichess ratings are affected by EvenChess games.
- ECE failure modes degrade without breaking games.
- Operators can identify and respond to incidents.
- Post-launch data is sufficient to prioritize next fixes.

Risks/blockers:

- Early player pool may be sparse, requiring careful bot fallback handling.
- First real data can reveal ECOR table imbalance.

Evidence to update:

- launch log;
- post-launch review;
- incident reports if any;
- next-version backlog.

---

## 7. Cross-Phase Release Gates

These gates must be checked before any public release candidate.

### 7.1 Source and Requirements Gate

- Active requirements are updated.
- Appendix Z records superseded behavior.
- Patch map covers every upstream/core seam.
- Integration log includes rollback notes.
- All intended implementation files are tracked.
- No generated/private ECE files are tracked.

### 7.2 Security Gate

- ECE analysis endpoints are private.
- Browser bundles contain no ECE private URLs or secrets.
- Admin routes require admin auth and CSRF.
- Token/bot/rating/admin actions are server-authoritative.
- Logs redact secrets, provider paths, raw prompts, and raw provider output.

### 7.3 Fairness Gate

- Set Level is assigned server-side.
- Used Level is monotonic and persisted.
- Used Offset is stored and used for ECR settlement.
- Normal Lichess ratings remain isolated.
- Bot fallback is disclosed when enabled.
- Premium/tokens do not provide stronger live rated assistance.

### 7.4 Reliability Gate

- ECE downtime does not break game play.
- Stale payloads are rejected.
- Quick/deep race conditions are handled.
- Proposed/potential consumables survive refresh and multi-tab behavior.
- Feature kill switches work.

### 7.5 UX Gate

- Search looks like normal Lichess search plus the approved preferred-level control.
- No public debug cards appear by default.
- Board overlays update after moves without refresh.
- Board remains playable.
- Level card, eval bar, coach card, and overlays meet visual requirements.

### 7.6 Operations Gate

- Staging deployment passed.
- Backups and restore tested.
- Monitoring dashboards exist.
- Incident runbook exists.
- Rollback plan exists.
- Support/moderation plan exists.

---

## 8. Production Feature Flag Inventory

At minimum, production should support independent flags for:

- EvenChess public entry points.
- EvenChess rated games.
- EvenChess casual games.
- EvenChess computer training.
- ECE live quick calls.
- ECE live deep calls.
- ECE AI summaries.
- Board overlays.
- Coach text.
- Eval bar/eval strip.
- Proposed move.
- Potential move reveal.
- Full-game analysis.
- Analysis memory/replay overlays.
- Token gates.
- Subscriptions.
- Ad rewards.
- Bot fallback matchmaking.
- Bot simulation.
- ECOR calibration writes.
- Admin ECOR table editing.
- Public debug search card.

Flags that affect rated fairness must be versioned/audited and must not silently change active games.

---

## 9. Minimum Test Commands and Evidence

The exact commands may evolve with the repo, but release evidence should include:

- Scala/backend EvenChess tests.
- TypeScript UI unit tests.
- Lobby/search serialization tests.
- Round overlay/display tests.
- Admin settings tests.
- Bot operation tests.
- Rating/ECOR tests.
- ECE adapter contract tests.
- Browser end-to-end tests for game start, move, payload update, proposed/potential moves, and game end.
- Normal Lichess regression smoke.
- Staging smoke against real ECE.
- Load/stress test with ECE quick/deep and bot simulation.

Each failed or skipped test must have a release decision: fixed, quarantined with reason, or accepted as non-blocking.

---

## 10. Final Public Deployment Checklist

Before public launch:

- [ ] v1.6 scope frozen.
- [ ] Release branch clean.
- [ ] Requirements and Appendix Z current.
- [ ] Patch map and integration log current.
- [ ] Production architecture documented.
- [ ] Secrets/config inventory complete.
- [ ] Database persistence and migrations complete.
- [ ] ECE private deployment validated.
- [ ] ECL/ECE gateway hardened.
- [ ] Matchmaking and MMR complete.
- [ ] ECR settlement and ECOR calibration complete.
- [ ] Live assistance authority complete.
- [ ] Overlay/coach UI polished.
- [ ] Proposed/potential consumables complete.
- [ ] Analysis memory complete.
- [ ] Monetisation either complete or disabled.
- [ ] Admin console complete.
- [ ] Bot fallback/simulation complete or disabled.
- [ ] Security/abuse controls complete.
- [ ] Telemetry/audit/privacy complete.
- [ ] CI/CD complete.
- [ ] Staging passed.
- [ ] Automated test matrix passed.
- [ ] Browser/device/performance QA passed.
- [ ] Backup/restore tested.
- [ ] Public rules/copy complete.
- [ ] Release candidate approved.
- [ ] Rollback plan ready.
- [ ] Launch monitoring staffed.

---

## 11. Immediate Next Work Recommended

The fastest path toward deployability is:

1. Finish repo hygiene and ensure all intended EvenChess files are tracked selectively.
2. Decide the first public beta feature flags and explicitly disable everything outside scope.
3. Move any remaining in-memory authority state to durable storage.
4. Harden ECL-to-ECE gateway behavior and stale/retry handling.
5. Complete matchmaking/ECR/bot integration tests.
6. Create staging deployment and run real ECE smoke tests.
7. Polish public UI and remove debug-only surfaces from normal user flows.
8. Build the operator dashboards and kill switches needed for launch.

This sequence is intentionally conservative. It turns local functionality into a deployable system before adding more product surface area.
