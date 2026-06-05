# EvenChess Plan v1.6 Phase U - Automated Test Matrix

## Phase Goal

Lock down the highest-risk EvenChess behavior with a CI-runnable automated test matrix and staging/browser evidence.

Phase U must prove that EvenChess works as an integrated Lichess fork, not only as isolated policy/model tests.

## Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- Appendix V: Testing and QA Acceptance
- Appendix X: Upstream Sync and Patch Map Requirements
- `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`
- `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`
- `docs/requirements/planv1.6_phase_j_game_policy_live_assistance_authority.md`
- `docs/requirements/planv1.6_phase_l_proposed_potential_consumables.md`
- `docs/requirements/planv1.6_phase_p_bot_matchmaking_simulation_operations.md`
- `docs/requirements/planv1.6_phase_q_security_abuse_fairness_controls.md`
- `docs/requirements/planv1.6_phase_s_ci_cd_build_release_automation.md`
- `docs/requirements/planv1.6_phase_t_staging_environment.md`

## Test Authority Rule

Automated tests must prove server authority for all fairness-sensitive behavior.

Client/UI tests may prove rendering and interaction. They must not be treated as proof that the browser owns:

- Set Level;
- Used Level;
- Used Offset;
- ECR settlement;
- token grant/consume/refund;
- proposed/potential consumed counts;
- coaching permission;
- matchmaking windows;
- bot eligibility;
- admin authorization.

## Minimum Test Matrix

| Area | Required coverage | Evidence |
| --- | --- | --- |
| Matchmaking/MMR | normal search, preferred level, ECOR assignment, uneven match, friend contracts, no normal Lichess rated pool use | Scala unit tests plus browser/staging match smoke |
| ECR settlement | actual Used Offset, idempotency, no normal rating mutation, ECOR sample capture | Scala tests plus completed-game staging proof |
| Game policy/live authority | Set Level cap, monotonic Used Level, preferred starting Used Level, payload refresh on Used Level increase | Scala tests plus browser game proof |
| ECE adapter | quick/deep split, stale rejection, deep eval only, timeout/retry, proposed nested output, potential reveal suppression | Scala/integration tests plus Test ECE/real ECE smoke |
| Display/overlay UI | level card, toggles, coach card, eval strip, offset/shield markers, hanging/threat/pin overlays, no layout jump | UI tests plus screenshot/browser smoke |
| Proposed moves | exactly one legal green arrow, ECE call, cache/toggle, illegal no-change, quota server-side | UI/backend/browser tests |
| Potential moves | own/opponent side correct, server reveal, cache/toggle, clears after move, quota server-side | UI/backend/browser tests |
| Tokens/monetisation | grants, consume, refund, caps, abuse state, subscription non-strength | Scala/controller tests plus sandbox if enabled |
| Bots | fallback timeout, scope gating, stockfish-equivalent rating, simulation seeding, cleanup, bot-vs-human, bot-vs-bot | Scala tests plus staging/browser proof |
| Admin/security | permissions, CSRF, ECOR tables, bot controls, token controls, feature flags, audit mutation | Scala/controller/browser tests |
| Telemetry/audit | audit envelopes, durable persistence, route emission, redaction, retention pruning | Scala tests plus staging storage/dashboard proof |
| Normal Lichess regression | normal board, moves, clocks, PGN/replay, analysis, non-EvenChess games | core regression and browser smoke |

## Backend Scala Tests

Required backend test command:

```bash
./lila.sh "evenchess/test"
```

Important existing EvenChess test files include:

- `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`
- `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`
- `modules/evenchess/src/test/EcrRatingTest.scala`
- `modules/evenchess/src/test/EvenChessRatingCalibrationTest.scala`
- `modules/evenchess/src/test/GamePolicyTest.scala`
- `modules/evenchess/src/test/AssistanceAccountingTest.scala`
- `modules/evenchess/src/test/LiveCoachingTest.scala`
- `modules/evenchess/src/test/EceLiveBridgeTest.scala`
- `modules/evenchess/src/test/CoachingPolicyTest.scala`
- `modules/evenchess/src/test/CoachingOverlaysTest.scala`
- `modules/evenchess/src/test/LiveBoardIntegrationTest.scala`
- `modules/evenchess/src/test/BotOperationsTest.scala`
- `modules/evenchess/src/test/AdminBackendSettingsTest.scala`
- `modules/evenchess/src/test/AdminOperationsTest.scala`
- `modules/evenchess/src/test/AdminOpsDashboardTest.scala`
- `modules/evenchess/src/test/TelemetryAnalyticsTest.scala`
- `modules/evenchess/src/test/AbuseTrustControlsTest.scala`
- `modules/evenchess/src/test/TrustOpsIncidentControlsTest.scala`
- `modules/evenchess/src/test/TestingQaAcceptanceTest.scala`

Backend release gaps:

- controller/route-level tests are still needed for live endpoints and admin mutations;
- durable persistence tests are needed for repositories currently represented as in-memory/controller-local;
- completed-game result lifecycle tests are needed for real ECR settlement;
- staging integration tests are needed for native game creation and clocks.

## ECE Adapter and Fixture Tests

Required fixture test command:

```bash
node scripts/evenchess-test-ece-server.test.mjs
```

Required ECE adapter coverage:

- Test ECE fixture mirrors real quick/deep/proposed/review envelopes;
- quick payload applies overlays but does not update eval from known quick-zero data;
- deep payload updates eval/eval strip;
- stale payloads are rejected;
- ECE timeout/retry produces bounded fallback;
- proposed-move output uses `proposed_move_evaluation.after_move_side_output`;
- illegal proposed move does not clear existing overlays;
- potential-move data is not sent to browser until ECL reveal authorizes it;
- browser never calls ECE board/proposed/review endpoints directly.

Release gap:

- Test ECE and real ECE contract changes must be compared in CI/staging whenever ECE contract changes.

## Frontend/UI Tests

Required frontend commands:

```bash
pnpm test
pnpm test:ui-overlay
pnpm test:ui-tsx -- ui/round/tests/evenchessOverlay.test.ts
```

Important existing UI tests:

- `ui/lobby/tests/evenchessSetup.test.ts`
- `ui/round/tests/evenchessOverlay.test.ts`
- `ui/round/tests/evenchessTestGround.test.ts`
- `ui/round/tests/evenchessTestGroundMoveBridge.test.ts`
- `ui/round/tests/premove.test.ts`

Required UI coverage:

- public search card only shows Preferred Set Level dropdown with Any/L0-L10;
- no debug search status card unless local/debug flag is active;
- old `any` value is normalized/omitted in submit payload;
- level toggles do not scroll/jump card to top;
- toggles persist across payload refresh;
- set/used level labels are not duplicated;
- coach card aligns with board top;
- eval strip is visible and uses deep payload eval;
- board overlays match offset/hanging/threat/pin requirements;
- proposed/potential buttons reflect availability and server counts;
- illegal proposed move does not clear overlays.

Release gap:

- UI tests must run in CI with source/module aliases configured consistently. The previous `@/index` resolver issue must be fixed or explicitly quarantined with issue link.

## Browser and Staging Tests

Browser tests must prove behavior that unit tests cannot:

- board is clickable and pieces move under overlays;
- overlay updates after a move without browser refresh;
- coach text updates only when it becomes the student's turn;
- board overlays persist while waiting for new payload;
- layout stays stable at common desktop/mobile widths and browser zoom levels;
- proposed legal arrow calls ECE and toggles cached result;
- proposed illegal/no-single-arrow state leaves overlay unchanged;
- potential own/opponent buttons show the correct side's moves;
- search with matchmaking bots enabled finds a bot game after low timeout;
- search with fallback disabled and simulation enabled can match simulation bots;
- normal Lichess non-EvenChess game still plays.

Required evidence:

- screenshots or videos for high-risk UI paths;
- browser test logs;
- staging smoke report;
- failures linked to issues or marked release-blocking.

## Admin and Security Tests

Admin/security tests must prove:

- non-admin users cannot access bot, ECOR, token, feature-flag, ECE diagnostics, or operator controls;
- admin mutations require valid CSRF;
- ECOR changes require reason/audit;
- bot operations are admin-only and stop cleanly;
- token adjustments are audited;
- feature flags and kill switches work;
- public JSON does not expose internal ticket ids, raw token keys, policy versions, bot seed ids, or MMR/ECR diagnostics;
- Test Ground-only routes are not production-exposed.

## Quarantine Policy

Known failing tests may be quarantined only when all are true:

- issue link exists;
- owner exists;
- release impact is stated;
- expected fix phase is stated;
- CI still runs the rest of the suite;
- release blockers are not hidden behind quarantine.

No test that proves fairness, rating isolation, token accounting, ECE privacy, or admin authorization may be silently quarantined for public release.

## Current Implementation State

### Existing Foundations

The current repo has substantial model/unit coverage:

- many EvenChess Scala test files exist under `modules/evenchess/src/test`;
- UI tests exist for lobby setup and round overlay/Test Ground behavior;
- Test ECE fixture test exists;
- `package.json` has scripts for UI tests, overlay tests, and TSX source-alias test execution;
- Phase S documents CI build/test job needs;
- Phase T defines staging smoke requirements.

### Not Yet Release-Proven

Phase U is not release-complete until:

- the full EvenChess test matrix runs in CI;
- known resolver/module-alias UI test issues are fixed or properly quarantined;
- route/controller tests cover live endpoints and admin mutations;
- durable repository tests replace in-memory-only confidence;
- browser automation proves move/overlay/proposed/potential behavior without refresh;
- staging proves real ECL/ECE interaction, bots, clocks, ratings, tokens, audit, and admin controls;
- normal Lichess regression smoke passes after EvenChess seams are active;
- every release-blocking failure has an explicit issue and go/no-go status.

## Patch Map Impact

Future Phase U implementation may add tests around upstream Lichess seams and may reveal required runtime changes. Any runtime seam changes must be patch-mapped and entered into the integration log.

This Phase U documentation pass does not itself change runtime code.

## Phase U Acceptance Status

Phase U is conducted as a readiness and requirements pass.

Status:

- Current test inventory and required release matrix are defined.
- Release readiness remains blocked until the matrix runs in CI and browser/staging tests prove the integrated game flow.

## Phase V Entry Criteria

Before browser/device/performance QA can be treated as final release evidence, Phase U must provide:

- passing CI test matrix;
- browser automation baseline for game move and overlay update;
- screenshot/video evidence for overlay/layout behavior;
- known failing tests list with issue links;
- staging smoke report from Phase T;
- proof normal Lichess core behavior still works.
