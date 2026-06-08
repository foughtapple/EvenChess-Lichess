# Phase A Requirements Lock - Platform-Bot Matchmaking (ECE v1.1)

**Plan:** `docs/requirements/plan_version_1.1/PLAN.md`
**Status:** Locked; updated after Phase J operations/admin implementation sweep
**Date:** 2026-06-02
**Scope:** EvenChess-Lichess matchmaking plus ECE compatibility for platform-owned bot match backfill and simulation modes.

## 1) Authoritative sources to honor

Phase A uses the following requirement surfaces as source of truth:

- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `docs/requirements/old2/APPENDIX_H_V2_MMR_ENGINE_AND_MATCH_CONTRACTS.md`
- `docs/requirements/old2/APPENDIX_G_V2_PLAY_SETUP_QUICK_SEARCH_AND_MATCHMAKING_UI.md`
- `docs/requirements/APPENDIX_J_V2_ECE_ENGINE_REQUIREMENTS.md` (legacy source; check against combined files first)
- `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`
- `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`

## 2) Requirement lock for Phase A

### Behavioral requirements

1. Matchmaking mode may auto-fill unmatched human waits with bot candidates after a configured delay.
2. Simulation mode can run configurable bot pools for load testing, including bot-vs-bot behavior.
3. Rated pool / casual pool / both constraints are explicitly controllable.
4. Bot opponents are only introduced through server-side EvenChess matchmaking seams and remain observable as controlled state.
5. Human-vs-human search is never disturbed before the threshold timeout.
6. Bot usage is immediately reversible by kill switch.
7. ECE integration path for bot matches must be identical to human matches (quick-first + optional deep flow).

### Policy interpretation for conflicts and existing terms

#### Existing invariants to keep

- Lichess/Chess game lifecycle remains untouched.
- Server is authoritative for permissions and policy flags.
- Matching still produces normal Lichess game records via existing game contracts.
- ECE remains server-to-server only.

#### Existing “no bots” language interpretation

Where requirements phrase “outside bots are disallowed” in rated play, Phase A interprets this as external/non-platform help by users.
For platform-bot matchmaking, bots are **platform-owned replacement opponents** implemented through the existing Lichess AI/computer route and therefore remain a platform integrity function, not user-driven outside assistance.

This interpretation is required to proceed; if your product policy says otherwise, Phase A must pause and be superseded before Phase B begins.

## 3) Current implementation status after Phase J

### Already present and reusable

- `modules/evenchess/src/main/PlaySearchIntegration.scala`
  - `PlayMode.AiPractice`
  - `SearchQueue.AiPractice` mapping in search integration
  - `PlayForm` and search intent repository patterns
  - `MatchmakingIntegrationService` contract evaluation flow
  - `GamePolicyMode.AiBotPractice` path in persistence layer
- `modules/evenchess/src/main/LevelBasedMatchmaking.scala`
  - queue/rating/window widening mechanics
  - contract + telemetry + pairing validation machinery
- `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`
- `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`

### Implemented for the Phase A outcome

- `LevelBasedMatchmaking.BotMatchProfile` defines server-side bot profile, target ECR, preferred Set Level, time-control, and timing/persona metadata.
- `PlaySearchIntegration.MatchmakingIntegrationService` can inject a fallback bot ticket after the configured timeout and only in the configured rated/casual/both scope.
- `BotOperations` owns admin-controlled simulation config, runtime status, simulation ticket seeding, and simulation-ticket-only cleanup.
- `/dev/evenchess/ops/bots` provides admin-only start/stop/settings/monitoring for matchmaking fallback and simulation mode.
- Test Ground embeds the authenticated admin bot operations panel instead of exposing unauthenticated bot-control APIs.
- Search-disclosure text is tied to live bot mode and remains deployment-safe in public search status.

### Deployment boundaries after Phase J

- Dedicated bot introduction telemetry/audit extensions are still pending beyond existing generic search telemetry.
- End-to-end platform-bot gameplay harness coverage remains pending.
- Simulation can seed bot tickets and monitor possible bot-vs-bot contracts, but autonomous Lichess game creation and bot move execution remain the production game-execution boundary.

## 4) Locked deliverables

Phase A is complete because:

- All bot-mode decisions are explicitly documented.
- Feature switches and defaults are defined.
- Matching pools and timeout behavior are specified.
- Telemetry/event names and fields are named for later enforcement.
- Replay/audit expectation is explicitly defined for bot-originated matches.
- Test matrix is written for unit + integration + local e2e; implementation currently has focused unit/integration coverage.

## 5) Decision checklist for Phase A lock

Recorded decisions:

- `matchmakingBotModeEnabled` scope: persisted backend/admin boolean.
- `matchmakingBotModeScope`: persisted backend/admin scope with `rated`, `casual`, or `both`.
- `botMatchTimeoutSeconds`: default 45 seconds; normalized to 1-3600 seconds so local operator testing can force near-immediate fallback.
- Pool selection model:
  - rated-only / casual-only / both
  - fallback and simulation tickets stay in rated/casual EvenChess search queues; `AiPractice` remains separate.
- Bot profile policy:
  - stable bot profile id from generated ticket id
  - target ECR from configured or deterministic profile generation
  - Set Level from configured range/profile
- Route mapping for created bot matches:
  - final production game creation remains a pending seam; current queue tickets flow through the normal MMR match-contract path.
  - Lichess AI/computer execution remains the intended game-play route when the game-creation seam is implemented.
- Kill-switches:
  - immediate disable from admin bot operations
  - simulation stop clears active simulation tickets without deleting human tickets
- UX disclosure:
  - ON/OFF state in setup/search card
- Replay audit:
  - event set still targeted for `bot_enabled`, `bot_match_selected`, `bot_vs_human`, `bot_vs_bot`, and reason fields

## 6) Acceptance gates for Phase A

### Mandatory (must pass)

- No upstream Lichess core file edits in Phase A.
- Bot mode behavior scoped and recorded in this doc.
- Clear policy map from requested user outcome to existing seams.
- Written evidence of dependencies and files for Phase B.
- No unresolved contradiction left unrecorded.

### Optional (must be captured if encountered)

- `AiPractice` remains separate from rated/casual fallback and simulation ticket queues.
- Bot-game policy persistence remains a later game-creation concern.
- Simulation currently supports configured rating/level ranges and persona mix rather than named presets.

## 7) Interface map

Implemented or active seams:

- `modules/evenchess/src/main/PlaySearchIntegration.scala`
- `modules/evenchess/src/main/LevelBasedMatchmaking.scala`
- `modules/evenchess/src/main/GamePolicy.scala` (policy mode consistency)
- `modules/evenchess/src/main/BotOperations.scala`
- `app/controllers/EvenChess.scala`
- `app/controllers/Dev.scala`
- `modules/web/src/main/ui/DevUi.scala`
- `modules/evenchess/src/test/*`

Telemetry event fields remain a follow-up seam.

## 8) Evidence to hand off

Implemented evidence:

- `PlaySearchIntegrationTest`: bot-mode gating, timeout injection, scope blocking, bot/human contract source.
- `BotOperationsTest`: simulation config, seeding, simulation-ticket-only clearing, admin-state monitoring.
- `AdminBackendSettingsTest` and `AdminOpsDashboardTest`: settings and admin dashboard wiring.
- `PHASE_J_BOT_OPERATIONS_ADMIN.md`: admin/runbook-style requirements and acceptance.
