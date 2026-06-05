# EvenChess-Lichess Plan v1.6 Phase A - Requirements Freeze and Launch Scope

**Date:** 2026-06-03
**Phase:** A
**Status:** Conducted as the v1.6 working scope freeze
**Repo:** EvenChess-Lichess
**Path:** `/home/jayde/dev/lila-docker/repos/lila`

This document completes Plan v1.6 Phase A by freezing the deployment-readiness scope, launch-stage gates, feature-flag baseline, non-goals, and release acceptance criteria for v1.6.

The scope below is the working release baseline. Product-owner/operator sign-off is still required before production launch because deployment choices, server secrets, payment settings, domain/TLS setup, and final bot policy cannot be completed from the local repository alone.

---

## 1. Active Requirements Confirmed

The v1.6 scope freeze uses these active EvenChess-Lichess requirements:

- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/plan_version_1.1/PLAN.md`
- `docs/requirements/plan_version_1.1/PHASE_J_BOT_OPERATIONS_ADMIN.md`
- `docs/requirements/plan_analysis_memory/PLAN.md`
- `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`
- `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`

ECE boundary authority:

- `/home/jayde/dev/lila-docker/repos/ece/docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md`

Confirmed invariants:

- ECE is a separate private backend service.
- ECL calls ECE server-to-server only.
- Browser/client code must not call ECE analysis endpoints.
- ECL owns matchmaking, ECR, tokens, admin, game policy, audit, and display permission.
- ECE owns chess calculations, provider orchestration, deterministic composer, private cache, provider secrets, private paths, and raw provider output.
- Normal Lichess ratings remain isolated from EvenChess ECR.
- Server authority is required for Set Level, Used Level, Used Offset, ECE permission, token spending, bot operations, and rating settlement.

No new requirement supersession is introduced by Phase A. Existing Appendix Z overrides remain active.

---

## 2. v1.6 Launch Principle

v1.6 is a deployment-readiness release, not a feature-expansion release.

The release target is:

1. Make core EvenChess playable on a server.
2. Keep the product integrated into native Lichess-style flows.
3. Keep ECE private and reliable.
4. Keep rated fairness server-authoritative and auditable.
5. Keep debug/Test Ground behavior out of public surfaces.
6. Enable risky or cost-bearing systems only after their gates pass.

If a feature is incomplete, expensive, abuse-sensitive, or not backed by durable server state, it must be disabled by default in public environments.

---

## 3. Launch Stage Gates

| Stage | Purpose | Public traffic | Default scope | Exit gate |
|---|---|---:|---|---|
| Local Test Ground | Developer validation | No | All local tools may exist, including Test ECE and debug panels | Local smoke confirms build, ECE launch, game start, move, overlay update, and admin pages |
| Private Staging | Production-like server test | No | Core play, real ECE, admin, bots, telemetry, migrations, feature flags | Staging smoke, restart, ECE failure, bot, and rollback tests pass |
| Closed Alpha | Invite-only real-user validation | Limited | Casual EvenChess, computer training, ECE overlays/coach, retained history, admin monitoring | No critical fairness, move, overlay, ECE, or queue defects across alpha games |
| Public Beta | Public assisted-chess launch | Yes | Casual EvenChess, rated ECR if gates pass, bot fallback optional, monetisation disabled by default | Stable queue, game completion, ECE latency, ECR settlement, and support metrics |
| Monetised Beta | Paid/token launch | Yes | Token/subscription/ads enabled only after ledger/payment gates pass | Payment, refund, entitlement, quota, and abuse tests pass |
| GA | Normal public operation | Yes | Full approved scope | Monitoring, support, moderation, backups, rollback, and ECOR calibration are routine |

---

## 4. First Public Beta Scope

The first public beta should launch with a conservative scope.

### 4.1 Enabled for Public Beta When Gates Pass

| Feature | Public beta default | Owner | Data model required | Test gate | Rollback switch |
|---|---:|---|---|---|---|
| Public EvenChess entry points | On | ECL | User/session plus EvenChess route config | Public navigation smoke | `evenchess.public.enabled` |
| Native-style EvenChess setup/search | On | ECL | `EvenChessSearchTicket` | Lobby/search UI and backend search tests | `evenchess.search.enabled` |
| Preferred Level dropdown: Any, L0-L10 | On | ECL/MMR | Search ticket preference field | UI serialization and backend normalization tests | `evenchess.search.preferredLevel.enabled` |
| Casual EvenChess games | On | ECL | Match contract and game policy | Human-vs-human casual game smoke | `evenchess.casual.enabled` |
| Rated EvenChess ECR games | Conditional on Phase H/I gates | ECL/MMR/ECR | ECR rating, rating event, match contract | ECR settlement tests and staging rated smoke | `evenchess.rated.enabled` |
| Computer training with EvenChess shell | On | ECL | Game policy or training policy | Computer game move and overlay smoke | `evenchess.computer.enabled` |
| ECE quick board payloads | On | ECL/ECE gateway | Payload request/accept audit | ECE quick adapter and stale rejection tests | `evenchess.ece.quick.enabled` |
| ECE deep board payloads | On if ECE stable | ECL/ECE gateway | Payload request/accept audit | ECE deep adapter, timeout, and eval tests | `evenchess.ece.deep.enabled` |
| Display Engine overlays | On | ECL UI | Assistance state and payload pointer | Overlay unit/browser tests | `evenchess.overlay.enabled` |
| Coach card text | On | ECL UI/ECE | Coaching render audit | Turn-gated text and stale tests | `evenchess.coach.enabled` |
| Eval bar and coach eval strip | On at eligible levels | ECL UI/ECE | Deep payload accepted eval state | Deep-only eval tests | `evenchess.eval.enabled` |
| Live assistance audit | On | ECL | Audit events | Audit event tests | `evenchess.audit.enabled` |
| Admin feature flags and kill switches | On | ECL admin | Admin config snapshot and audit | Admin auth/CSRF tests | Admin-controlled |
| ECE health/readiness monitoring | On | ECL ops/ECE | Metrics/logs | Monitoring smoke | `evenchess.ece.liveHelp.enabled` |

### 4.2 Disabled by Default for First Public Beta

These features may be tested locally/staging but must stay off in public beta unless their later phase gates pass.

| Feature | Public beta default | Reason | Required gate before enabling | Rollback switch |
|---|---:|---|---|---|
| Bot fallback matchmaking | Off initially, admin-enable after staging proof | Affects public pairing trust and fairness disclosure | Phase P bot fallback tests, public disclosure, admin controls | `evenchess.botFallback.enabled` |
| Bot simulation | Off | Stress-test/admin tool, not normal public user feature | Phase P simulation tests and explicit operator decision | `evenchess.botSimulation.enabled` |
| Proposed move help | Off until server consumables prove stable | Consumes assistance and changes overlay state | Phase L server-side quota/cache tests | `evenchess.proposedMove.enabled` |
| Potential move reveals | Off until server reveal path proves stable | Must not leak unrevealed potential data | Phase L reveal suppression/cache tests | `evenchess.potentialMoves.enabled` |
| Full-game custom analysis | Off | Cost/token-heavy and storage-heavy | Phase M/N token and retention gates | `evenchess.fullGameAnalysis.enabled` |
| Custom analysis tokens | Off | Requires token ledger and refunds | Phase N ledger/payment tests | `evenchess.tokens.customAnalysis.enabled` |
| Subscriptions | Off | Requires payment provider and support flows | Phase N payment/subscription gates | `evenchess.subscriptions.enabled` |
| Ads/reward tokens | Off | Abuse-sensitive | Phase N/Q abuse and reward tests | `evenchess.ads.enabled` |
| AI summaries beyond deterministic/ECE-safe output | Off unless ECE validates provider | Cost and hallucination risk | ECE provider readiness and validation tests | `evenchess.aiSummaries.enabled` |
| Public debug search card | Off | Product owner rejected debug card in public search | Local/debug-only flag and no public route exposure | `evenchess.debug.searchCard.enabled` |
| Test ECE payload server | Off | Local-only test harness | Never public | Local script only |
| ECE CLM/settings browser links | Off public | Local/private diagnostics only | Never public unless separately approved | Local Test Ground only |

---

## 5. Required Feature Flag Baseline

The production config must support these flags before public launch:

| Flag | Default public beta | Fairness-affecting | Audit/version required |
|---|---:|---:|---:|
| `evenchess.public.enabled` | On | No | No |
| `evenchess.search.enabled` | On | Yes | Yes |
| `evenchess.search.preferredLevel.enabled` | On | Yes | Yes |
| `evenchess.casual.enabled` | On | Yes | Yes |
| `evenchess.rated.enabled` | Conditional | Yes | Yes |
| `evenchess.computer.enabled` | On | No | No |
| `evenchess.ece.quick.enabled` | On | Yes | Yes |
| `evenchess.ece.deep.enabled` | On if stable | Yes | Yes |
| `evenchess.ece.liveHelp.enabled` | On | Yes | Yes |
| `evenchess.overlay.enabled` | On | Yes | Yes |
| `evenchess.coach.enabled` | On | Yes | Yes |
| `evenchess.eval.enabled` | On at eligible levels | Yes | Yes |
| `evenchess.proposedMove.enabled` | Off | Yes | Yes |
| `evenchess.potentialMoves.enabled` | Off | Yes | Yes |
| `evenchess.analysisMemory.enabled` | Conditional | Yes for replay surfaces | Yes |
| `evenchess.fullGameAnalysis.enabled` | Off | No live fairness, but token/cost-sensitive | Yes |
| `evenchess.tokens.enabled` | Off | No stronger live help allowed | Yes |
| `evenchess.subscriptions.enabled` | Off | No stronger live help allowed | Yes |
| `evenchess.ads.enabled` | Off | No stronger live help allowed | Yes |
| `evenchess.botFallback.enabled` | Off initially | Yes | Yes |
| `evenchess.botSimulation.enabled` | Off | Yes/ops-sensitive | Yes |
| `evenchess.ecorCalibration.enabled` | Admin-only | Yes if table changes | Yes |
| `evenchess.debug.searchCard.enabled` | Off | No, but public-surface sensitive | No |

Fairness-affecting flag changes must not silently alter active rated games. They must be logged with operator, timestamp, old value, new value, reason, and policy version where applicable.

---

## 6. Non-Goals for v1.6

The following are out of scope for v1.6 unless explicitly approved later:

- Editing ECE internals from the ECL repo.
- Copying ECE provider code, secrets, prompts, Stockfish paths, model weights, tablebases, generated DBs, or private caches into ECL.
- Exposing ECE analysis endpoints to browser clients.
- Upstream Lichess sync or broad Lichess refactor.
- Replacing core chess rules, move legality, clocks, PGN, or game-result authority.
- Merging EvenChess ECR into normal Lichess ratings.
- Public monetisation launch before ledger/payment/refund gates pass.
- Public bot simulation as a visible product feature.
- Public Test Ground/debug panels.
- Automatic ECOR table changes from calibration without admin review.
- Launching paid benefits that provide stronger live rated assistance.

---

## 7. Public-Beta User Experience Scope

Public beta should look like a deployed site, not a test harness.

Included:

- EvenChess-branded public shell.
- Native-style Play/search flow.
- One Preferred Level dropdown with `Any` and `L0` to `L10`.
- Native Lichess searching indicator.
- Clear fairness disclosure.
- In-game board, clocks, legal moves, game end, and replay.
- EvenChess eval bar, level card, overlay layer, and coach card where allowed.
- Admin-only operational settings.

Excluded from public surfaces:

- raw ECE diagnostics;
- match ticket internals;
- queue debug stage text;
- Test ECE controls;
- local WSL/Docker controls;
- ECE CLM/settings links;
- unauthenticated bot start/stop controls;
- provider paths;
- raw prompts or raw provider output;
- dead/scaffold monetisation copy.

---

## 8. Data Model Commitments for Enabled Features

Any feature enabled in public beta must have durable server-side state.

| Enabled feature | Minimum durable state |
|---|---|
| Search/matching | Search ticket, match contract, assigned levels, policy version, expiry |
| Rated ECR | ECR rating record, rating event, ECOR snapshot, final Used Levels |
| ECE live help | Payload request audit, accepted payload pointer, stale rejection record |
| Used Level | Game assistance state with monotonic highest Used Level |
| Display toggles | User/session display preference only; not permission authority |
| Eval bar | Accepted deep payload eval state |
| Coach text | Last player-turn accepted text payload and render audit |
| Computer training | Training/game policy with Set Level 10 default |
| Admin flags | Config snapshot and admin audit |

Features without durable state must remain disabled in public beta.

---

## 9. Test Commitments for Enabled Features

The public-beta enabled scope requires these test categories before release:

- Public navigation and setup/search UI tests.
- Search serialization tests for Preferred Level `Any` and `L0` to `L10`.
- Backend search/match-contract tests.
- Human-vs-human casual game smoke.
- Rated ECR settlement tests if rated is enabled.
- Computer game move/playability smoke.
- ECE quick/deep adapter tests.
- Stale payload rejection tests.
- Deep-only eval tests.
- Overlay visual/toggle tests.
- Coach text turn-gating tests.
- Admin authorization and CSRF tests.
- Feature flag rollback tests.
- Normal Lichess regression smoke.

Tests that fail or cannot run must be recorded before release-candidate approval.

---

## 10. Rollback Commitments

Every enabled public-beta feature must have an operator rollback path.

Minimum rollback behavior:

- Disable new EvenChess searches while allowing active games to finish.
- Disable rated EvenChess while preserving casual/training if needed.
- Disable ECE live help without breaking legal move play.
- Disable deep payloads while retaining quick payloads if needed.
- Disable overlays while retaining normal chess board play.
- Disable coach text while retaining board play.
- Disable bot fallback/simulation independently.
- Disable proposed/potential move help independently.
- Disable token/subscription/ads independently.
- Freeze ECOR table edits while keeping current settlement policy stable.

Rollback must be admin/operator controlled and audited.

---

## 11. Phase A Acceptance Status

| Acceptance gate | Status | Notes |
|---|---|---|
| Active requirements source list confirmed | Complete | Listed in Section 1 |
| Old/superseded requirements handled | Complete for Phase A | No new supersession introduced |
| First public launch scope decided | Complete as working baseline | Public beta defaults defined in Sections 3 and 4 |
| Feature flags defined | Complete as baseline | Section 5 |
| Launch-stage gates defined | Complete | Section 3 |
| Non-goals recorded | Complete | Section 6 |
| Every enabled launch feature has owner/data model/tests/rollback | Complete as planning baseline | Sections 4, 8, 9, and 10 |
| Deferred features disabled or removed from public navigation | Complete as planning baseline | Section 4.2 and Section 7 |
| Product-owner/operator sign-off | Pending external sign-off | Required before production deployment |

Phase A is complete for repository planning purposes. Production launch remains blocked until later phases satisfy implementation, staging, secrets, server, and sign-off requirements.

---

## 12. Phase B Entry Criteria

Phase B may start after this document is accepted as the working scope baseline.

Phase B must focus on:

1. Release branch and dirty-worktree classification.
2. Selective tracking of intended EvenChess files.
3. Removal or ignoring of generated/local/private files.
4. Patch-map and integration-log verification.
5. No broad implementation rewrites.
