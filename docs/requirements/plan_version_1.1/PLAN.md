# EvenChess-Lichess Version 1.1 Platform-Bot Matchmaking and ECE v1.1 Alignment Plan

**Folder:** `docs/requirements/plan_version_1.1`
**Scope date:** 2026-06-01
**Primary sources:**
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `docs/requirements/APPENDIX_H_V2_MMR_ENGINE_AND_MATCH_CONTRACTS.md`
- `docs/requirements/APPENDIX_G_V2_PLAY_SETUP_QUICK_SEARCH_AND_MATCHMAKING_UI.md`
- `docs/requirements/old2/APPENDIX_J_V2_ECE_ENGINE_REQUIREMENTS.md`
- `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`
- `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`

**Execution rule:** one phase at a time, no broad rewrites, and no browser-side calls to ECE.

## Current status as of 2026-06-02

- Phases A-C, F-G, I, and J have implementation evidence in code, tests, patch map, and integration log.
- Matchmaking fallback is admin-controlled and can seed a same-pool bot ticket only after the configured timeout.
- Simulation mode is admin-controlled and can seed/remove simulation bot search tickets through the existing server-side search repository and MMR contract pipeline.
- Test Ground embeds the authenticated `/dev/evenchess/ops/bots` admin panel for local control and monitoring; it does not expose unauthenticated bot-control APIs.
- Public search status remains deployment-safe and shows only ON/OFF disclosure, wait/status labels, access labels, and assigned-level summaries.
- Deployment boundary: autonomous Lichess game creation and bot move execution from simulation/fallback contracts remain pending beyond this plan slice.
- Operations boundary: dedicated bot telemetry/audit events beyond generic search telemetry remain pending.

## A — Requirements lock + implementation map

**Goal:** Confirm exact acceptance criteria before touching code.

- Use `PHASE_A_REQUIREMENTS_LOCK.md` as the Phase A requirements lock artifact.
- Treat Phase A complete when the lock is approved and there are no unresolved contradictions.

- Finalise user story:
  - **Matchmaking mode:** after waiting `N` seconds with no human match, fill with bot only when the feature is enabled.
  - **Simulation mode:** create configurable bot users and allow stress testing by searching vs bot pool or bot-vs-bot games.
  - Rated/casual/both pool restrictions must be enforced.
  - Bots map to existing Lichess "AI/computer" game style and keep normal game lifecycle.
- Read and reconcile relevant requirements + existing `docs/requirements/APPENDIX_Z_*` and local invariants.
- Confirm whether to reuse:
  - `PlayMode.AiPractice`
  - `SearchQueue.AiPractice`
  - existing `LevelBasedMatchmaking` and `PlaySearchIntegration` contracts.
- Confirm telemetry and audit schema fields for bot actions are defined for later enforcement.

**Completion checklist:**
- Final requirements matrix captured in `PHASE_A_REQUIREMENTS_LOCK.md`.
- Test matrix written (unit/integration/e2e scenarios).
- Current lock has been updated after Phase J to separate implemented controls from remaining game-execution and telemetry gaps.


## B — Bot profile, rating, and Stockfish-level model

**Goal:** Build a server-side bot profile model that does not leak into Lichess internals.

- Add/review bot profile source-of-truth fields:
  - `botId`, optional `userRef` or stable internal id
  - `targetECR` (or effective rating bucket for matchmaking)
  - preferred `setLevel`
  - optional `timeControl`.
- Add deterministic/randomized profile generation for undeclared users:
  - rated pool: sample from platform-like ECR distribution
  - casual pool: separate distribution if needed.
- Define explicit **rate mapping contract**:
  - `setLevel -> stockfish depth/strength profile`
  - fallback mapping when no bot level provided.
- Anti-detect requirement:
  - no static engine-style timings; add randomized move latency profile.

**Files likely touched:** `modules/evenchess/src/main/*` (domain model/services), optional simulator config files.

**Acceptance:**
- A bot identity can be created from profile only.
- Bot profile can be persisted/replayed from config.
- `setLevel` and rating feed are available without invoking ECE.

## C — Matchmaking fill behavior ("bot matchmaking mode")

**Goal:** Enable low-latency fallback matches by standing up bot tickets.

- Add a `matchmakingBotModeEnabled` gate (environment + ops setting).
- Add user controls to enable:
  - `rated`
  - `casual`
  - `both`.
- Update search/waiting state machine:
  - maintain a timer for each waiting human ticket.
  - after threshold `X seconds`, seed bot ticket(s) using same mode constraints.
- Ensure existing human-vs-human remains unchanged when threshold not reached.
- Keep dependencies isolated:
  - bot seeding only through a thin service that writes a regular `SearchIntentRecord` into existing repository/seams.

**Acceptance:**
- If enabled and no match within threshold, bot candidate appears in the same contract pipeline as human candidates.
- Human search does not start against bots before timer elapses.

## D — Simulation mode ("load mode" / synthetic population)

**Goal:** Run controlled populations of bots to simulate concurrent user demand and match pressure.

- Add simulation runner that spawns bot tickets per configured counts and rating bins.
- Provide startup/teardown commands and profile presets (e.g. `xLow`, `xMid`, `xHigh`).
- Enable bot-vs-bot matchmaking in simulation without forcing human game-start prompts.
- Emit simulation-specific audit tags for separation:
  - `mode=simulation`
- Simulation should support a "human-like mix" of target ratings:
  - uniform or normal profiles across requested bins.

**Acceptance:**
- Can launch simulation mode and observe stable queue churn across all requested bins.
- Can pause/stop simulation safely without impacting existing real searches.
- Current implementation seeds/removes simulation tickets and monitors possible bot-vs-bot contracts; full autonomous bot-vs-bot game execution remains a pending production game-execution boundary.

## E — Lichess game routing for bot games

**Goal:** Ensure bot matches use Lichess-provided game path rather than inventing new game infrastructure.

- Use existing Lichess AI/computer play route as the game execution path.
- Translate matched bot pairings to "AI/computer" compatible game creation inputs.
- Preserve normal legal chess, clocks, and clocks side effects.
- Ensure the opponent color assignment and level assignment are stable per game.
- Keep non-rated/casual updates consistent with existing queue semantics:
  - rated-bot pair in rated mode should behave as `AiBotPractice`/platform-authored route, not normal rated ECR pathways.

**Acceptance:**
- Bot games are indistinguishable from native AI/computer flow except for policy-specific disclosure and telemetry.
- No direct server-side move generation occurs on the EvenChess layer.
- Current status: human search tickets now poll through a stable opaque search key, fallback/simulation bot tickets can match through the MMR contract pipeline, and a human-vs-platform-bot match creates a one-shot native Lichess AI/computer game redirect. Full autonomous bot-vs-bot game execution remains a pending production game-execution boundary.

## F — Rating/time-control matching and humanization

**Goal:** Keep pairing quality and bot behavior credible for stress testing.

- Keep bot seeding inside existing queue/pool constraints:
  - `timeControl` + pool (`EcrPool`) alignment.
- Preserve existing target-level and widening logic while allowing bot tickets as candidates.
- Implement randomized move timing:
  - jitter distributions per bot persona
  - capped minimum think-time to prevent suspiciously deterministic play.
- Add hardening to block perfect/engine-level deterministic play signatures where feasible.

**Acceptance:**
- Match quality messages remain valid when bot tickets are candidates.
- Simulation queues can represent both "human-like" and "fast" bot populations.

## G — Disclosure, status, and gameplay impact surfaces

**Goal:** Make matchmaking behavior and bot presence transparent to users.

- Add search-card disclosure text:
  - `"Bots may be implemented after long wait times while EvenChess's player pool is low. This will be removed as we grow. Bots are currently ON/OFF."`
- Add status telemetry in search contract response and waiting UI:
 - waiting duration
 - whether contract came from bot or human opponent.
- Add operational kill switches:
 - disable bot introduction instantly for incident response.

**Acceptance:**
- Search UI shows ON/OFF state exactly.
- Users can verify state from setup UI and logs without ambiguity.

## H — Telemetry, audits, abuse/fairness controls

**Goal:** Add auditable, replayable records for matchmaking and simulation behavior.

- Extend telemetry events for:
  - bot enabled/disabled,
  - bot ticket create/start/cancel,
  - bot-vs-human and bot-vs-bot match decisions,
  - threshold-triggered activation.
- Add abuse flags for:
  - repeated bot re-matching,
  - pattern loops,
  - abort/resign abuse on bot games.
- Ensure all bot actions are in server-authoritative audit ledger.

**Acceptance:**
- Every bot contract can be reconstructed from logged events.
- Simulation mode artifacts are explicitly marked and excluded from production fairness metrics.
- Current status: admin actions and the simulation ticket family are observable; the dedicated bot audit event schema remains a follow-up.

## I — ECE v1.1 contract and live bridge verification

**Goal:** Keep bot-driven games compatible with quick/deep ECE contract behavior.

- Ensure bot gameplay contract and game-level metadata are sent to ECE through the same server-only gateway as human games.
- Keep existing quick-first flow:
1. `POST /v1/ece/board/quick`
2. conditional `POST /v1/ece/board/deep` using `quick_context_id`/`request_id`.
- Ensure deep requests for bot games include:
  - matching levels,
  - same diagnostics constraints,
  - same stale/position validation checks.
- Confirm all side outputs are server-side mapped to overlay model and never bypassed to client directly.
- Keep local/CI smoke script for:
  - real ECE unavailable fallback,
  - mock ECE payload pass-through,
  - quick/deep context handling.

**Acceptance:**
- Bot games have no separate ECE contract branch.
- ECE payload path and validation behavior remain identical to human games.

## J — E2E test package, rollout gate, and operations readiness

**Goal:** Ship behind controlled flags with evidence.

- Add end-to-end test scenarios:
  - matchmaking mode: rated-only, casual-only, both (timeouts + fallback),
  - simulation mode startup/shutdown,
  - bot-vs-human and bot-vs-bot pairing,
  - real ECE and mock ECE quick/deep bridge behavior.
- Add admin/runbook docs:
  - how to enable bot mode,
  - thresholds and kill-switches,
  - rollback path to disable seeding immediately.
- Update:
  - integration log entries for each seam touched,
  - patch map entries for upstream Lichess file edits,
  - testground controls if needed for launch/teardown.

**Acceptance:**
- No known blind spots in deployment/runbook coverage.
- Feature can be switched off quickly with no data-corruption side effects.
- Current status: admin operations and Test Ground embedding are implemented. Stop simulation clears only simulation tickets and leaves human search records intact.

## Suggested implementation order (hard dependency)

1. A, B, C, E
2. F, G
3. H, I
4. D, J

## Deliverables for each phase

- Design doc updates in this folder.
- Minimal scoped diffs in `modules/evenchess`, `app/controllers`, `conf`, and UI seams only when required.
- `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` entry per seam class.
- Test files under `modules/evenchess/src/test`, `ui/round/tests`, and any e2e harness used for Test Ground.
