# EvenChess-Lichess Patch Map

This file records every intentional edit to upstream Lichess/Lila source files.

## Patch entry template

File touched:
EvenChess requirement:
Why core Lichess file had to be touched:
Could this be isolated later:
Upstream merge risk:
Tests added or updated:
Notes:

## Current patches

### PM-2026-039 - V2 Phase C homepage branding and lobby theme summary

File touched:
`app/views/lobby/home.scala`; `ui/lobby/css/_lobby.scss`

EvenChess requirement:
REQ-D-V2-001, REQ-D-V2-002, REQ-D-V2-003, REQ-D-V2-010, REQ-D-V2-011, REQ-D-V2-020, REQ-D-V2-021, REQ-D-V2-022, REQ-D-V2-030, REQ-D-V2-031.

Why core Lichess file had to be touched:
The public root page and native lobby stylesheet are upstream Lichess surfaces. Version 2 Phase C requires the public shell to identify as EvenChess, preserve the familiar Lichess layout, and explain disclosed/capped/logged coaching directly on the homepage without replacing the lobby with a detached landing page.

Could this be isolated later:
Partially. The copy source is isolated in `modules/evenchess/src/main/PublicShell.scala`, but the root-page render and lobby stylesheet still need a small native seam unless Lichess exposes a homepage extension point.

Upstream merge risk:
Medium. Future upstream homepage or lobby stylesheet changes may conflict around homepage metadata, anonymous about-side copy, the inserted summary block, or lobby CSS ordering.

Tests added or updated:
`modules/evenchess/src/test/PublicShellTest.scala` verifies the homepage copy remains disclosure-safe and includes the required Set Level, audit/logging, and ECR concepts.

Notes:
Integration log entry: `INT-2026-039`.

### PM-2026-040 - V2 Phase D top bar token and account/settings shell

File touched:
`modules/web/src/main/ui/layout.scala`; `ui/lib/css/header/_buttons.scss`; `modules/pref/src/main/ui/AccountUi.scala`; `modules/pref/src/main/ui/AccountPref.scala`

EvenChess requirement:
REQ-D-V2-032, REQ-D-V2-033, REQ-E-V2-010, REQ-E-V2-011, REQ-E-V2-012, REQ-E-V2-013, REQ-F-V2-001, REQ-F-V2-002, REQ-F-V2-003, REQ-F-V2-004.

Why core Lichess file had to be touched:
The visible header buttons and account/preferences navigation are native Lichess shell surfaces. Version 2 Phase D requires game-token balance access and EvenChess settings to appear in top-bar/account-native locations without replacing Lichess account or preference flows.

Could this be isolated later:
Partially. The token-balance display contract is isolated in `modules/evenchess/src/main/AccountMonetisationUi.scala`, but header rendering and account menu placement still need small native seams unless Lichess exposes account-header extension points.

Upstream merge risk:
Medium. Future upstream header button or account menu changes may conflict with the token chip placement, header CSS, or the account menu link.

Tests added or updated:
`modules/evenchess/src/test/AccountMonetisationUiTest.scala` verifies the top-bar token display links to rewarded ads, remains fairness-neutral, and avoids stronger-help claims.

Notes:
Integration log entry: `INT-2026-040`.

### PM-2026-041 - V2 Phase E native setup/search UI controls

File touched:
`app/views/lobby/home.scala`; `app/controllers/EvenChess.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/view/table.ts`; `ui/lobby/css/_setup.scss`

EvenChess requirement:
REQ-G-V2-001, REQ-G-V2-002, REQ-G-V2-003, REQ-G-V2-010, REQ-G-V2-011, REQ-G-V2-012, REQ-G-V2-013, REQ-G-V2-014, REQ-G-V2-015, REQ-G-V2-016, REQ-G-V2-020, REQ-G-V2-021, REQ-G-V2-022, REQ-G-V2-023, REQ-G-V2-024, REQ-G-V2-025, REQ-G-V2-026, REQ-G-V2-030, REQ-G-V2-031.

Why core Lichess file had to be touched:
Version 2 Phase E requires EvenChess setup/search controls to live inside the native Lichess lobby setup modal and search-status surface. The native modal/controller/table files are the seam where time controls, level preferences, disclosure, token gate state, and search status are displayed.

Could this be isolated later:
Partially. UI helper logic is isolated in `ui/lobby/src/evenchessSetup.ts`, and search contract parsing remains in `modules/evenchess/src/main/PlaySearchIntegration.scala`, but native modal rendering and lobby status still require small Lichess UI seams unless a setup extension point is introduced.

Upstream merge risk:
Medium. Future upstream lobby/setup modal refactors may conflict with EvenChess fields, stored setup keys, submit params, or status rendering.

Tests added or updated:
`ui/lobby/tests/evenchessSetup.test.ts` covers level validation, Apply preferences scenario labels, rated/casual mode preservation when preferences are set, computer-game Set Level L10 defaulting, and token-gate copy. `modules/evenchess/src/test/PlaySearchIntegrationTest.scala` covers Apply preferences parsing, ignored Any/no-preference targets, and adapter flags.

Notes:
Integration log entry: `INT-2026-041`.
2026-06-01 correction: native lobby start labels remain `Create lobby game`, `Challenge a friend`, and `Play against computer`; the optional `/bots` start card is not exposed on the EvenChess lobby. Computer games keep the native `/setup/ai` submit path and force EvenChess Set Level L10 internally for overlay/coaching policy, while hook/friend games retain the EvenChess search setup controls and `/evenchess/play/search.json` handoff.
2026-06-01 correction: target-level preferences are gated behind `Apply preferences`, use Any/L0-L10 dropdowns, and stay in the selected rated/casual queue instead of switching hook searches into the separate Target Level queue.

### PM-2026-043 - V2 Phase G search JSON matchmaking handoff

File touched:
`app/controllers/EvenChess.scala`

EvenChess requirement:
REQ-G-V2-024, REQ-G-V2-025, REQ-G-V2-026, REQ-G-V2-030, REQ-H-V2-001, REQ-H-V2-002, REQ-H-V2-003, REQ-H-V2-004, REQ-H-V2-010, REQ-H-V2-011, REQ-H-V2-020, REQ-H-V2-021, REQ-H-V2-022, REQ-H-V2-023, REQ-H-V2-024, REQ-H-V2-030, REQ-H-V2-031, REQ-H-V2-032, REQ-H-V2-033, REQ-H-V2-034, REQ-H-V2-041.

Why core Lichess file had to be touched:
The authenticated EvenChess search endpoint is the native controller seam that starts search from the Lichess-style setup modal. Phase G requires that prepared search records hand off to the EvenChess MMR Engine and expose a match-contract status without using normal Lichess public pools.

Could this be isolated later:
Yes. The actual matchmaking handoff is isolated in `modules/evenchess/src/main/PlaySearchIntegration.scala`; this controller should remain a thin JSON adapter until a dedicated lila matchmaking extension point exists.

Upstream merge risk:
Low to medium. Future route/controller refactors may change the search JSON model or authenticated controller helper shape.

Tests added or updated:
`modules/evenchess/src/test/PlaySearchIntegrationTest.scala` covers the MMR handoff, strict waiting, widened non-strict contracts, rated/casual queue preservation for preferences, and no coaching render before game policy.

Notes:
Integration log entry: `INT-2026-043`.

### PM-2026-062 - V2 Test Ground ECL-to-ECE bridge smoke

File touched:
`app/controllers/EvenChess.scala`; `conf/routes`

EvenChess requirement:
REQ-J-V2 ECE server-to-server boundary, local ECE base URL, side-gated response validation, stale-payload rejection, and non-fatal ECE outage handling; REQ-L-V2-001, REQ-L-V2-002, REQ-L-V2-023; REQ-T-V2-001.

Why core Lichess file had to be touched:
The dev Test Ground needs a backend EvenChess endpoint that exercises the real ECL-to-ECE path without letting browser code call ECE directly. `conf/routes` exposes a namespaced dev-only smoke endpoint, and `app/controllers/EvenChess.scala` remains the thin Lichess controller seam that posts to ECE server-to-server, validates the payload, and returns only approved round overlay JSON.

Could this be isolated later:
Mostly. The bridge mapping is isolated in `modules/evenchess/src/main/EceLiveBridge.scala`; the remaining route/controller seam can move behind a dedicated test/dev controller once the live round socket integration exists.

Upstream merge risk:
Low. The route is namespaced under `/evenchess/testground/*`, and the controller change is limited to EvenChess-owned actions. Future controller route generation changes may require reapplying the route line.

Tests added or updated:
`modules/evenchess/src/test/EceLiveBridgeTest.scala` covers accepted ECE output compilation and stale response clearing.

Notes:
Integration log entry: `INT-2026-062`.

### PM-2026-063 - V2 Test Ground live round overlay adapter

File touched:
`ui/round/src/ctrl.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`

EvenChess requirement:
REQ-A-V2-020, REQ-A-V2-024, REQ-B-V2-010, REQ-J-V2 server-to-server ECE boundary and non-fatal ECE outage handling, REQ-L-V2-001, REQ-L-V2-002, REQ-L-V2-020, REQ-L-V2-023, REQ-N-V2-020, REQ-V-V2-006.

Why core Lichess file had to be touched:
The live game screenshot exposed that the round renderer was ready for `evenchessLive` payloads, but local AI games never requested one and the coach surface disappeared entirely without payload content. The round controller is the narrow native seam that knows the current game id, player side, ply, and FEN needed by the dev Test Ground backend bridge. The round overlay view now keeps persistent level/status cards, display toggles, and coach-card space visible before payload arrival; the new call remains same-origin and works from production round pages while the browser still requests only Lichess backend JSON. ECE is still called only by the backend endpoint.

Could this be isolated later:
Yes. This is a temporary Test Ground adapter until production live ECE scheduling and socket emission are wired server-side. It should be removed or feature-flagged behind the production live assistance transport once round sockets carry audited ECE history.

Upstream merge risk:
Medium. `ui/round/src/ctrl.ts` is an active upstream controller file. Future upstream changes around `pluginUpdate` or constructor initialization may need a small reapplication of the local-only adapter hook.

Tests added or updated:
`ui/round/tests/evenchessTestGround.test.ts` verifies non-spectator live/computer round gating and same-origin overlay URL construction with level 10 test inputs. `ui/round/tests/evenchessOverlay.test.ts` verifies the coach/level shell, display toggles, coach-card placeholder, and monotonic Used Level behavior before and after payload arrival.

Notes:
Integration log entry: `INT-2026-063`.

### PM-2026-064 - V2 Test Ground Docker-host ECE bridge target

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EngineGateway.scala`; `ui/round/src/evenchessTestGround.ts`

EvenChess requirement:
REQ-J-V2 ECE server-to-server boundary and local ECE service URL; REQ-L-V2-001, REQ-L-V2-002, REQ-L-V2-023; REQ-T-V2-001.

Why core Lichess file had to be touched:
The local Test Ground browser calls only the same-origin Lichess dev endpoint, but Dockerized Lila cannot reach a developer-hosted ECE process through the container's own `127.0.0.1`. The controller seam now accepts only locally allowlisted ECE bridge base URLs, and the round Test Ground adapter points that backend bridge at Docker Desktop's `host.docker.internal` alias while keeping all browser-to-ECE direct calls blocked.

Could this be isolated later:
Yes. This remains a localhost development adapter. Production live assistance should use server configuration and socket-delivered audited ECE history instead of the Test Ground query parameter.

Upstream merge risk:
Medium. `ui/round/src/evenchessTestGround.ts` and `app/controllers/EvenChess.scala` are local EvenChess seams, but future route/controller or round adapter changes may need this dev-only bridge target reapplied.

Tests added or updated:
`modules/evenchess/src/test/EngineGatewayTest.scala` verifies Docker-host ECE aliases are local-only and external hosts are rejected. `ui/round/tests/evenchessTestGround.test.ts` verifies the Test Ground bridge URL sends the Docker-host ECE base URL only to the same-origin Lichess endpoint.

Notes:
Integration log entry: `INT-2026-065`.

### PM-2026-066 - V2 Test Ground board-layer visual rendering

File touched:
`ui/round/src/ctrl.ts`; `ui/round/src/view/evenchessOverlay.ts`

EvenChess requirement:
REQ-C-V2-012, REQ-D-V2-012, REQ-D-V2-013, REQ-L-V2-020, REQ-L-V2-023, REQ-N-V2-020, REQ-N-V2-022, REQ-O-V2-013, REQ-O-V2-020, REQ-O-V2-021, REQ-O-V2-030, REQ-V-V2-006, REQ-V-V2-007.

Why core Lichess file had to be touched:
The round page was receiving approved ECE/test-ECE visual payloads and rendering the side-panel visual chip, but the board layer never received corresponding Chessground auto-shapes. `ui/round/src/ctrl.ts` is the narrow native seam that owns the active Chessground API. `ui/round/src/view/evenchessOverlay.ts` now converts only current, server-authorized, approved, non-stale visual labels with board coordinates into Chessground markers/arrows.

Could this be isolated later:
Yes. When production live ECE scheduling emits structured board overlay atoms through the round socket, the label parser should be replaced by structured marker/arrow fields while keeping the Chessground auto-shape update seam.

Upstream merge risk:
Medium. `ui/round/src/ctrl.ts` is active upstream code and may need a small reapply if Chessground lifecycle or auto-shape handling changes.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies approved square and arrow visuals become Chessground board shapes and non-coordinate diagnostics remain out of the board layer.

Notes:
Integration log entry: `INT-2026-066`.

### PM-2026-068 - V2 Test Ground level ladder and fixed coach card

File touched:
`ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`

EvenChess requirement:
REQ-A-V2-021, REQ-A-V2-022, REQ-C-V2-012, REQ-D-V2-012, REQ-D-V2-013, REQ-I-V2-012, REQ-N-V2-020, REQ-N-V2-022, REQ-V-V2-008.

Why core Lichess file had to be touched:
The Test Ground round page needs the real EvenChess display shell while ECE is still under development: a Set-Level-capped feature ladder, per-level display toggles, Used-Level display that never decreases when local selections are raised, and a stable coach card for incoming audited text. These controls sit inside the native round page because they gate the already-authorized overlay payload and board auto-shapes at render time; browser code still consumes only same-origin Lichess data and never calls ECE directly.

Could this be isolated later:
Partly. The level definitions and feature-key classifier can move to an EvenChess UI module once production ECE emits structured feature atoms. The round seam should remain the final renderer because it owns the current board DOM/Chessground surface.

Upstream merge risk:
Medium. `ui/round/src/view/evenchessOverlay.ts` and `ui/round/css/_evenchess-live.scss` are round UI files that may need reapplication if upstream changes the round side-panel layout or Chessground auto-shape lifecycle.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies the visible level ladder, Set-Level cap, per-feature selection, Used-Level monotonic display behavior, and per-level board-shape gating.

Notes:
Integration log entry: `INT-2026-068`.

### PM-2026-069 - V2 real ECE public payload adapter alignment

File touched:
`app/controllers/EvenChess.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-testground-panel.js`; `scripts/evenchess-testground.ps1`

EvenChess requirement:
REQ-J-V2 ECE server-to-server boundary and side-gated public payload parsing; REQ-L-V2-001, REQ-L-V2-002, REQ-L-V2-023; REQ-N-V2-020; REQ-O-V2-010, REQ-O-V2-020, REQ-O-V2-030; REQ-V-V2-005, REQ-V-V2-006, REQ-V-V2-007; ECE combined requirements ZD-002, ZD-003, ZD-010, ZD-012.

Why core Lichess file had to be touched:
Real ECE board-state output now returns structured text objects, `candidate_moves`, top-level `evaluation`, `opening`, and `human_risk`, plus nested `overlays.trade_status`, `overlays.threats`, and `overlays.pinned_pieces`. Its Offset Count entries use real public keys such as `target_square` and `piece_count_delta`, so the dev bridge must normalize those fields into approved round overlay labels. `app/controllers/EvenChess.scala` is the existing dev-only server-to-server bridge seam that normalizes ECE public payloads into approved round overlay JSON and returns non-fatal JSON if ECE is unreachable. The browser still calls only the same-origin Lichess endpoint and never calls ECE directly.

Could this be isolated later:
Yes. Move the ECE public payload adapter into an EvenChess module once the production live ECE scheduler/socket path replaces the Test Ground controller endpoint.

Upstream merge risk:
Low to medium. The controller action is namespaced under `/evenchess/testground/*`, but the file is a Play controller and may need reapplication if upstream route/controller wiring changes.

Tests added or updated:
The Test Ground ECE fixture was updated to mirror the real ECE public board-state shape. Validation covered direct real ECE health/ready/board probes, WSL-to-`host.docker.internal` reachability, ECL-to-real-ECE bridge smoke for L0/L10 and both sides, clean non-fatal ECE-unavailable JSON, direct test ECE fixture smoke, ECL-to-test-ECE bridge smoke, root Scala compile, EvenChess backend tests, Node syntax checks, and scoped whitespace checks. Follow-up local verification on 2026-06-01 updated the local launcher/monitoring path to the Linux ECE sibling repo at `/home/jayde/dev/lila-docker/repos/ece` and its bash lifecycle scripts.

Notes:
Integration log entry: `INT-2026-069`.

### PM-2026-070 - V2 spec-style board-attached overlay renderer

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `scripts/evenchess-testground-panel.js`; `ui/round/src/view/main.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-C-V2-012, REQ-D-V2-012, REQ-D-V2-013, REQ-I-V2-012, REQ-L-V2-020, REQ-L-V2-023, REQ-N-V2-020, REQ-N-V2-022, REQ-O-V2-013, REQ-O-V2-020, REQ-O-V2-021, REQ-O-V2-030, REQ-V-V2-006, REQ-V-V2-007, REQ-V-V2-008.

Why core Lichess file had to be touched:
The board visuals need to match the supplied overlay recreation spec more closely than Chessground generic auto-shapes allow: exact 100x100 board-coordinate geometry, dotted threat lines, candidate labels, coner badges, square outlines, pin badges on the top-left coner, and Offset Count shield/number semantics. `ui/round/src/view/main.ts` is the native board render seam where a pointer-transparent, board-attached EvenChess layer can be inserted without owning move input or floating over the page. Server/controller changes keep the visual labels structured enough for this renderer while preserving same-origin Lichess transport and server-authorized payload gating.

Could this be isolated later:
Partly. The renderer can move to a dedicated EvenChess round UI module once the production live socket emits structured marker/arrow fields. The insertion point still needs to remain in the native round board render tree unless Lichess exposes a board-layer extension hook.

Upstream merge risk:
Medium. `ui/round/src/view/main.ts` is an upstream round UI file and may need a small reapply if upstream changes board child rendering. The overlay parser is isolated in `ui/round/src/view/evenchessOverlay.ts`.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies level-gated board-attached visuals, spec-style dotted arrows, candidate labels, square-outline highlights, pin top-left badges, Offset Count shield badges placed bottom-right per the latest visual direction, and real-ECE-style feature toggles suppressing Safety, Offset Count, Pattern, and candidate overlay families without mutating the returned ECE payload. Scala and TypeScript checks verify the bridge still compiles.

Notes:
Integration log entry: `INT-2026-070`. This supersedes the local rendering mechanism from `PM-2026-066`; the old Chessground-shape converter remains as a test/fallback helper, but live EvenChess board visuals now render through the board-attached layer. Follow-up visual correction on 2026-05-31 replaces large center-style square rings with square-outline highlights and uses the user-requested Offset Count bottom-right coner placement. Follow-up display-gating correction on 2026-06-01 keys the coach/board layer by the active feature selection so unchecking a locally available feature immediately redraws only the allowed overlay families.

### PM-2026-072 - V2 round card layout, eval bar, and granular level controls

File touched:
`ui/round/src/interfaces.ts`; `ui/round/src/view/main.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_app-layout.scss`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-C-V2-012, REQ-D-V2-012, REQ-D-V2-013, REQ-I-V2-012, REQ-L-V2-001, REQ-L-V2-002, REQ-L-V2-023, REQ-N-V2-020, REQ-N-V2-022, REQ-O-V2-013, REQ-O-V2-020, REQ-O-V2-021, REQ-O-V2-030, REQ-V-V2-006, REQ-V-V2-008, and the ECE combined requirements server-to-server/private-backend boundary.

Why core Lichess file had to be touched:
The native round page owns the board grid, side panels, and board-adjacent UI placement. EvenChess needs the level-selection card to sit left of the board, an eval bar between that card and the board, and the coach card on the right, while keeping browser display controls limited to already-authorized same-origin overlay payload data. The round renderer and layout stylesheet are the seam where those surfaces can be positioned without making client code call ECE directly or own chess truth.

Could this be isolated later:
Partly. The level definitions and feature classifiers can move to a dedicated EvenChess round UI module once production ECE emits structured display atoms. The native round grid and board insertion seam still need a small Lichess-side integration point unless upstream exposes a board/side-panel extension hook.

Upstream merge risk:
Medium. `ui/round/src/view/main.ts` and `ui/round/css/_app-layout.scss` are active upstream round layout files and may need reapplication if upstream changes board/table grid structure. The feature toggle logic remains isolated in `ui/round/src/view/evenchessOverlay.ts`.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies the visible eval bar shell, granular feature keys, Set-Level-capped apply-up-to behavior, monotonic Used-Level behavior, and local feature toggles suppressing matching board overlay families without mutating the ECE payload.

Notes:
Integration log entry: `INT-2026-072`. Follow-up control-state correction on 2026-06-01 materializes a complete feature-toggle map, keeps local per-feature display toggles persistent across later ECE payloads, treats the "Apply up to" dropdown as the only bulk setter, and preserves monotonic Used Level when visible features are lowered. Follow-up level-card polish on 2026-06-02 keeps the side panel vnode key stable across individual feature toggles so the scroll position is not reset, removes the duplicate Set/Used/Payload summary block, and shows the header labels as `Set Level: X` and `Used Level: Y`.

### PM-2026-073 - V2 ECE quick/deep board-state contract split

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `scripts/evenchess-testground.ps1`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-A-V2-021, REQ-A-V2-024, REQ-J-V2-001 through REQ-J-V2-017, REQ-L-V2 live ECE non-blocking/Used-Level rules, REQ-N-V2 level-gated Stockfish/ECE output, REQ-O-V2 side-gated display surfaces, REQ-V-V2 test acceptance, and the ECE caller contract `EVENCHESS_ENGINE_CONTRACT.md` quick/deep split.

Why core Lichess file had to be touched:
The dev-only EvenChess controller is the current server-to-server bridge from Lila to ECE. ECE split board-state work into fast deterministic `/v1/ece/board/quick` output and optional provider-backed `/v1/ece/board/deep` addenda, so the bridge now posts quick first, requests deep only when either side's level requires deep content, validates deep request echo/context/FEN/levels before merging addenda, and still exposes only same-origin approved round overlay JSON to the browser.

Could this be isolated later:
Yes. Move the quick/deep ECE adapter out of the controller into a dedicated EvenChess service module when production live scheduling/socket emission replaces the Test Ground controller endpoint.

Upstream merge risk:
Low to medium. The changed Play controller is namespaced under `/evenchess/testground/*`, and the gateway module is EvenChess-owned, but route/controller wiring can still need reapplication after upstream changes.

Tests added or updated:
`modules/evenchess/src/test/EngineGatewayTest.scala` verifies quick/deep endpoint defaults, deep-request decisioning for mixed side levels, and deep request envelope construction. `scripts/evenchess-test-ece-server.test.mjs` verifies the Test ECE fixture mirrors real-shaped health, ready, quick, deep, proposed-move, and game-review envelopes. The Test ECE fixture now level-gates quick output, returns deep side addenda, and keeps the legacy endpoint for migration smoke tests.

Notes:
Integration log entry: `INT-2026-073`. The Lichess requirements combined file records `OVR-V2-008`, superseding the previous single synchronous board endpoint assumption.

### PM-2026-078 - V1.1 Phase G matchmaking disclosure and contract source status

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/table.ts`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`

EvenChess requirement:
REQ-G-V2-030, REQ-H-V2-002, REQ-H-V2-005, REQ-J-V2-013, and plan requirement lock section for Phase G in `docs/requirements/plan_version_1.1/PLAN.md`.

Why core Lichess file had to be touched:
The search contract response is the user-visible seam for matchmaking transparency. This phase introduced bot-mode and contract-source state for matching decisions, then later deployment polish narrowed public search JSON/UI to display-safe labels: bot disclosure ON/OFF, wait labels, access labels, and assigned levels. Detailed seed/ticket diagnostics remain internal/admin-only.

Could this be isolated later:
Partly. Internal source and bot-mode telemetry can move to a dedicated EvenChess admin/ops model, but the public contract status seam remains in `EvenChess.scala` and `ui/lobby/src/view/table.ts` until `Play Search` gains a dedicated long-poll channel.

Upstream merge risk:
Low to medium. The status widget and JSON payload are on native lobby seams (`ui/lobby/src/view/table.ts`, `ui/lobby/src/interfaces.ts`) and a shared controller seam (`app/controllers/EvenChess.scala`) that is likely to be rebased onto upstream route/controller changes.

Tests added or updated:
`modules/evenchess/src/test/PlaySearchIntegrationTest.scala` verifies bot/human `contractSource` assignment on direct human matches and timed bot matches.

Notes:
Integration log entry: `INT-2026-078`. Follow-up compile hardening on 2026-06-01 corrected the controller-side `MatchContractSource` reference after the enum remained owned by `PlaySearchIntegration`, and verified local startup through the Docker full app compile plus `localhost:8080` smoke. Integration log entry: `INT-2026-083`.

### PM-2026-084 - V2 round overlay persistence and safety-marker correction

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `scripts/evenchess-test-ece-server.js`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-A-V2-020 through REQ-A-V2-024; REQ-C-V2-012; REQ-J/L/N/O/V2 server-authorized ECE display and live overlay rules; REQ-O-V2-010 through REQ-O-V2-013; REQ-O-V2-030 through REQ-O-V2-032; REQ-V-V2-006 through REQ-V-V2-008; OVR-V2-010.

Why core Lichess file had to be touched:
The dev-only EvenChess controller is the active same-origin server-to-server ECE bridge. Real ECE/test ECE safety, offset, and pin payloads must normalize into approved round visuals without exposing ECE internals or letting browser code call ECE directly. The round overlay view is the native board-adjacent render seam for local display toggles and board-attached marker rendering. Round-page controls use `bind` hooks because the round snabbdom setup does not install the normal event listener module.

Could this be isolated later:
Yes. Move the ECE public payload parser out of the controller into a dedicated EvenChess adapter module once production live ECE scheduling replaces the Test Ground endpoint. The round board layer still needs a native insertion point unless Lichess exposes a board overlay extension hook.

Upstream merge risk:
Medium. `ui/round/src/view/evenchessOverlay.ts` is an active round UI file, and `app/controllers/EvenChess.scala` is a Play controller seam. Reapply carefully if upstream changes round rendering or controller conventions.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` now verifies loose/non-attackable orange markers, attackable purple markers, offset badges, pin badges, and separate local feature toggles for loose versus hanging safety families. `ui/round/tests/evenchessTestGround.test.ts` verifies an in-flight ECE request queues a fresh request when the board changes during a move and retries a visual-empty accepted payload so quick/deep ECE timing can settle.

Notes:
Integration log entry: `INT-2026-084`. This updates `PM-2026-070`/`PM-2026-072` by making safety-marker classification exact, keeping overlay redraw keyed by ply/FEN/feature toggles, fixing round-control event wiring, queueing post-move overlay refreshes, retrying visual-empty accepted payloads, and extending the Test ECE fixture with both attackable and non-attackable safety markers.

### PM-2026-085 - V2 MMR contract-to-game-policy handoff sweep

File touched:
`modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/table.ts`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-G-V2-011 through REQ-G-V2-014; REQ-G-V2-026; REQ-G-V2-030; REQ-H-V2-002; REQ-H-V2-020 through REQ-H-V2-025; REQ-H-V2-030 through REQ-H-V2-034.

Why core Lichess file had to be touched:
The lobby search-status widget is the current native UI seam where players can inspect the matched EvenChess contract before handoff. The widget now renders the actual assigned White/Black Set Levels and contract source from the MMR contract instead of showing only the queued level hint.

Could this be isolated later:
Yes. Move the contract summary to a dedicated EvenChess search panel or pairing-confirmation modal once production game creation consumes the finalized contract directly.

Upstream merge risk:
Low to medium. The lobby table/status seam is active upstream UI code and should be reapplied carefully after upstream lobby changes. The game-policy handoff remains in EvenChess-owned Scala modules.

Tests added or updated:
`PlaySearchIntegrationTest` verifies game-start policy persistence writes the MMR-assigned contract levels, not queued default levels. `evenchessSetup.test.ts` verifies Any/ANY remains normal search in the scenario helper.

Notes:
Integration log entry: `INT-2026-085`. The MMR contract remains server-authoritative; browser status only displays same-origin contract summary data and does not decide coaching permission.

### PM-2026-086 - V2 exact overlay marker semantics and Used-Level refresh

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/CoachingOverlays.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `modules/evenchess/src/test/CoachingOverlaysTest.scala`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-A-V2-021 through REQ-A-V2-023; REQ-L-V2-001 through REQ-L-V2-005; REQ-O-V2-003 through REQ-O-V2-014; REQ-O-V2-020 through REQ-O-V2-025; REQ-O-V2-030 through REQ-O-V2-033; REQ-V-V2-007A; OVR-V2-010; OVR-V2-012.

Why core Lichess file had to be touched:
The round page owns the native board overlay insertion point and local display controls. EvenChess needs exact board-attached markers: non-attackable hanging/loose pieces as orange bottom-left exclamation circles; student-owned attackable hanging pieces as red bottom-left exclamation circles plus red inside rim; opponent-owned attackable hanging pieces as purple bottom-left exclamation circles plus purple inside rim; Offset Count as top-right green/red/blue badges; pins as top-left pin symbols; and separate student/opponent threat toggles. Board visual atoms must not also render as coach-side text chips, because the board already displays them and the extra list makes the coach side jump. The local Test Ground round adapter must also re-request ECE when Used Level rises so the payload matches the highest live level used.

Could this be isolated later:
Partly. The ECE ownership parser can move into a dedicated EvenChess payload adapter. The board-attached renderer and control hooks still need a round UI seam unless upstream exposes a plugin slot for board markers.

Upstream merge risk:
Medium. `ui/round/src/view/evenchessOverlay.ts`, `ui/round/src/ctrl.ts`, and `_evenchess-live.scss` are active round UI seams. Reapply marker positions, split threat toggles, and Used-Level refresh behavior carefully after upstream round UI changes.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies student/opponent threat splitting, top-right Offset Count badges, bottom-left loose/hanging markers, attackable ownership colors/rims, top-left pin icon, eval-bar hiding, no coach-side visual chip duplication, and feature toggles. `ui/round/tests/evenchessTestGround.test.ts` verifies retained Used Level is sent to ECE. `scripts/evenchess-test-ece-server.test.mjs` verifies the Test ECE fixture includes both attackable-hanging ownership cases and all Offset Count outcomes. Scala bridge tests compile the ownership-aware marker atoms through `EceLiveBridge`.

Notes:
Integration log entry: `INT-2026-086`. This supersedes the remaining generic threat-toggle and generic attackable-hanging marker assumptions from `PM-2026-084`.

### PM-2026-087 - V2 proposed-move preview bridge and round UI control

File touched:
`conf/routes`; `app/controllers/EvenChess.scala`; `ui/round/src/ctrl.ts`; `ui/round/src/ground.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `scripts/evenchess-test-ece-server.test.mjs`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
J.12 Proposed-Move Integration; REQ-L-V2-022; REQ-R-V2-005; ECE server-to-server and no direct browser ECE access rules.

Why core Lichess file had to be touched:
The round page owns user-drawn arrows and legal-destination state, so it must validate exactly one legal green arrow before offering a proposed-move preview. `conf/routes` and `app/controllers/EvenChess.scala` provide the same-origin ECL bridge so the browser never calls ECE directly. `ui/round/src/ground.ts` needs the chessground drawable-change hook to clear the preview when the selected arrow is removed or changed.

Could this be isolated later:
Partly. The dev-only proposed-move bridge should move into a production EvenChess service endpoint with auth/audit/quota persistence. The round UI still needs a native chessground drawable seam unless upstream exposes a plugin hook for proposed move annotations.

Upstream merge risk:
Medium. `conf/routes`, `app/controllers/EvenChess.scala`, `ui/round/src/ctrl.ts`, `ui/round/src/ground.ts`, and `ui/round/src/view/evenchessOverlay.ts` are active upstream seams. Preserve server-to-server ECE calls, one-arrow validation, Used-Level quota gating, and drawable-change clearing during rebase.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies proposed-move L5 quota, one-green-arrow validation, legal move gating, cached preview rendering, and clearing when the arrow is removed. `ui/round/tests/evenchessTestGround.test.ts` verifies same-origin proposed-move URL construction. `scripts/evenchess-test-ece-server.test.mjs` verifies the Test ECE proposed-move fixture echoes the move and supplies preview text.

Notes:
Integration log entry: `INT-2026-087`. Proposed-move previews are separate from saved board-state payloads and are cleared on arrow/FEN change.

### PM-2026-088 - V2 player-turn coach text cadence

File touched:
`ui/round/src/ctrl.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-L-V2-001; REQ-L-V2-020 through REQ-L-V2-025.

Why core Lichess file had to be touched:
The round page is where live ECE payloads are applied and rendered. EvenChess must still accept a payload after every move for board overlays/history, while keeping the visible coach text stable until a fresh non-stale payload arrives on the local player's turn. This requires a small round-side coach-text snapshot separate from the current live payload.

Could this be isolated later:
Partly. A production server-side history service can own longer-term snapshots, but the round UI still needs a local display snapshot to prevent opponent-turn payloads from replacing the player's visible coach text.

Upstream merge risk:
Medium. `ui/round/src/ctrl.ts` and `ui/round/src/view/evenchessOverlay.ts` are active round UI seams. Preserve the distinction between live payload/board overlay updates and player-turn-only coach text refreshes when rebasing.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies opponent-turn payloads do not replace the visible coach text snapshot while board visuals still use the current payload, and verifies player-turn payloads refresh the snapshot.

Notes:
Integration log entry: `INT-2026-088`.

### PM-2026-089 - V2 preferred starting Used Level and persistent level selections

File touched:
`modules/evenchess/src/main/UserSettings.scala`; `modules/pref/src/main/FormCompatLayer.scala`; `modules/pref/src/main/JsonView.scala`; `modules/pref/src/main/PrefForm.scala`; `modules/pref/src/main/ui/AccountPref.scala`; `ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `modules/evenchess/src/test/UserSettingsTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-F-V2-001 through REQ-F-V2-006; REQ-L-V2-001 through REQ-L-V2-007.

Why core Lichess file had to be touched:
The round page owns the live level selector and sends local Test Ground ECE requests. EvenChess needs each game to initialize from the account Preferred starting Used Level capped by the game Set Level, then keep manual dropdown/toggle selections stable across later payloads. The Test Ground request adapter must request the retained Used Level rather than re-defaulting after a move.

Could this be isolated later:
Partly. Production server-side game metadata should eventually provide the initial Set Level and preferred-used snapshot. The round UI still needs the native selector state so local display choices persist during the game.

Upstream merge risk:
Medium. `ui/round/src/ctrl.ts`, `ui/round/src/evenchessTestGround.ts`, and `ui/round/src/view/evenchessOverlay.ts` are active round seams. Preserve initialization-once-per-game, Set-Level capping, and monotonic Used Level behavior during rebase.

Tests added or updated:
`UserSettingsTest` verifies preferred-used storage, client config, tag round-trip, and Set-Level capping. `evenchessOverlay.test.ts` verifies round display initialization from preferred Used Level and persistence after lowering visible selections. `evenchessTestGround.test.ts` verifies Test Ground requests fall back to the preferred Used Level capped by Set Level.

Notes:
Integration log entry: `INT-2026-089`. This supersedes the old assumption that every live game must always start at L0; new games start from the account preferred level capped by Set Level, while Used Level remains monotonic.

### PM-2026-090 - V2 deployment polish for token and search/game status surfaces

File touched:
`app/controllers/EvenChess.scala`; `app/views/evenchess/account.scala`; `app/views/evenchess/play.scala`; `modules/evenchess/src/main/AccountMonetisationUi.scala`; `modules/evenchess/src/test/AccountMonetisationUiTest.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/table.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-E-V2-013; REQ-E-V2-014; REQ-G-V2-030 through REQ-G-V2-032; REQ-S-V2-022; REQ-T-V2-030.

Why core Lichess file had to be touched:
The native lobby status panel and EvenChess controller are the browser-facing seam for search/game-start feedback. Deployment-facing token and game setup pages must display normal product labels while hiding ticket ids, raw token gate keys, bot seed diagnostics, internal match-contract stage/source fields, expected-offset model values, and provider/scaffold wording.

Could this be isolated later:
Partly. The public status DTO can move into an EvenChess presenter module once the search API stabilizes, but the native lobby table still needs to render the deployment-safe summary in the Lichess setup flow.

Upstream merge risk:
Low to medium. `ui/lobby/src/view/table.ts` and `ui/lobby/src/interfaces.ts` are upstream lobby seams. Keep the display-only status contract when rebasing future lobby changes.

Tests added or updated:
`AccountMonetisationUiTest` verifies disabled checkout links and polished rewarded-token full-bank copy. Existing lobby UI build/type checks cover the narrowed browser-facing status interface.

Notes:
Integration log entry: `INT-2026-090`. This supersedes the remaining public-status assumption in `PM-2026-085` that contract stage/source details belong in the visible lobby status.

### PM-2026-091 - V1.1 bot simulation admin operations and Test Ground embedding

File touched:
`conf/routes`; `app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/web/src/main/ui/DevUi.scala`; `scripts/evenchess-testground-panel.js`; `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/AdminOpsDashboard.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/AdminOpsDashboardTest.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/requirements/plan_version_1.1/PLAN.md`; `docs/requirements/plan_version_1.1/PHASE_A_REQUIREMENTS_LOCK.md`; `docs/requirements/plan_version_1.1/PHASE_I_ECE_BRIDGE_VERIFICATION.md`; `docs/requirements/plan_version_1.1/PHASE_J_BOT_OPERATIONS_ADMIN.md`

EvenChess requirement:
REQ-F-V2-013 through REQ-F-V2-015; REQ-H-V2-042 through REQ-H-V2-044; REQ-V-V2-013; OVR-V2-015; Version 1.1 bot plan Phase D/J.

Why core Lichess file had to be touched:
The existing Lichess dev/admin controller and UI are the admin-authenticated shell for operational controls, and `conf/routes` is the route seam for the new bot operations panel/actions. The EvenChess search controller must use the same local search repository that admin simulation controls seed/clear, otherwise start/stop would not affect the local queue used by search.

Could this be isolated later:
Partly. `BotOperations.scala` owns the simulation runtime and ticket seeding. The admin route/view can later move behind a dedicated EvenChess admin module if Lichess exposes a plugin-style admin extension point.

Upstream merge risk:
Low to medium. The Dev controller/UI and routes are upstream seams. Reapply only the namespaced `/dev/evenchess/ops/bots` routes and the shared local search repository when rebasing.

Tests added or updated:
`BotOperationsTest` verifies simulation config, seeding, clearing only simulation tickets, and admin-state monitoring. `AdminBackendSettingsTest` and `AdminOpsDashboardTest` cover the new settings/model wiring.

Notes:
Integration log entries: `INT-2026-091`, `INT-2026-092`. This records `OVR-V2-015`: Test Ground embeds the authenticated admin panel and does not add unauthenticated bot-control APIs. Follow-up requirements polish on 2026-06-02 updated the v1.1 plan lock/runbook, replaced prototype terminology with platform-bot terminology in active deployment docs, and added `REQ-V-V2-013` for bot operations test coverage.

### PM-2026-092 - V2 computer-round Test Ground overlay availability

File touched:
`ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessTestGround.test.ts`

EvenChess requirement:
REQ-F-V2-001 through REQ-F-V2-006; REQ-M-V2-001; REQ-S-V2-022.

Why core Lichess file had to be touched:
The native round adapter owns local Test Ground ECE requests. Computer games use Lichess's synthetic round id, but EvenChess computer games are valid player surfaces with Set Level 10, so the adapter must not suppress them. Spectators remain suppressed.

Could this be isolated later:
Yes. Production live assistance should eventually be driven by server-side round metadata rather than the local Test Ground adapter. The round adapter still needs to accept computer rounds until that transport is in place.

Upstream merge risk:
Low to medium. `ui/round/src/evenchessTestGround.ts` is an EvenChess-namespaced round file, but it is mounted from the upstream round UI.

Tests added or updated:
`ui/round/tests/evenchessTestGround.test.ts` verifies that synthetic/computer player rounds are allowed while spectator rounds are still blocked.

Notes:
Integration log entry: `INT-2026-096`.

### PM-2026-093 - V2 round boot hardening for computer-game overlay shell

File touched:
`ui/round/src/round.ts`; `ui/round/src/view/main.ts`

EvenChess requirement:
REQ-C-V2-003; REQ-C-V2-012; REQ-M-V2-033; REQ-Q-V2-040 through REQ-Q-V2-042; REQ-T-V2-011.

Why core Lichess file had to be touched:
The native round module owns the boot sequence for chessground, turn/status cards, clocks, and the board input layer. A stalled piece preload or EvenChess overlay render exception could leave computer-game rounds in the server-rendered static state: visible board and game metadata, but missing live turn controls, move controls, and EvenChess cards. The round boot now waits only briefly for piece preload before continuing, and EvenChess overlay rendering is guarded so a local overlay failure cannot prevent the native Lichess round UI from mounting.

Could this be isolated later:
Partly. The EvenChess renderer can move behind a stricter plugin boundary if Lichess exposes a board/side-panel extension point, but the round module still needs a boot-time fail-open guard around optional EvenChess UI.

Upstream merge risk:
Medium. `ui/round/src/round.ts` is the core round boot path and `ui/round/src/view/main.ts` is the core round layout seam. Preserve the bounded preload wait and fail-open EvenChess render guards when rebasing round UI.

Tests added or updated:
No new automated test was added in this patch. Browser verification covered an authenticated computer game after rebuilding `round.4PUTXK5X.js`: native clocks/status controls mounted, the turn message displayed, legal moves were present in the raw player payload, the EvenChess level/coach shell rendered, and the board overlay remained non-interactive.

Notes:
Integration log entry: `INT-2026-097`. This does not change chess rules, legal move generation, clocks, or Stockfish game flow; it prevents optional EvenChess UI and asset preload from blocking the native playable round shell.

### PM-2026-094 - V2 desktop round layout alignment for levels and eval bar

File touched:
`ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`

EvenChess requirement:
REQ-C-V2-012; REQ-M-V2-033; REQ-N-V2-020; REQ-O-V2-030.

Why core Lichess file had to be touched:
The native round grid owns the side card, board, table, and responsive board sizing. EvenChess needs the desktop live layout to keep the eval bar exactly board-height with its top and bottom flush to the chess board, while placing the levels card under the native game metadata card at the same side-column width and with its bottom flush to the board. This requires the wide desktop `.round` grid to place side, levels, eval, board, coach, and native table controls in one parent grid instead of treating the levels card as a separate full-height column inside `.round__app`.

Could this be isolated later:
Partly. The EvenChess card styles stay namespaced, but the native round grid still needs a small parent-layout seam unless Lichess exposes a side-column/board-adjacent extension point.

Upstream merge risk:
Medium. `ui/round/css/_layout.scss` is an upstream round layout file. Preserve the desktop `:has(> .round__app.evenchess-live-layout)` parent-grid mapping and the board-height eval/levels alignment when rebasing.

Tests added or updated:
No automated layout test was added. Verification used the Test Ground UI build and CSS checks.

Notes:
Integration log entry: `INT-2026-098`. This is visual layout only; it does not alter ECE payload parsing, level gating, move legality, clocks, or game lifecycle.

### PM-2026-095 - V2 live ECE move refresh and proposed-move preview toggle

File touched:
`ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-J-V2-004 through REQ-J-V2-008; REQ-L-V2-022 through REQ-L-V2-025; Appendix J.12 proposed-move usage/display rules.

Why core Lichess file had to be touched:
The native round controller owns the post-move plugin update path. EvenChess must request a fresh same-origin ECE board-state payload after each committed move without waiting for a page refresh, while preserving Lichess's legal move lifecycle, clocks, and turn state.

Could this be isolated later:
Partly. The ECE polling adapter remains namespaced, but the move-complete notification still needs a round-controller seam until Lichess exposes a dedicated extension hook after accepted moves.

Upstream merge risk:
Medium. `ui/round/src/ctrl.ts` is a core round file. Preserve the forced EvenChess refresh on plugin update and the queued-force retention in the Test Ground adapter when rebasing round-controller changes.

Tests added or updated:
`evenchessTestGround.test.ts` verifies forced refreshes queued behind in-flight ECE calls are not lost and duplicate same-position requests are not queued while an ECE call is already in flight. `evenchessOverlay.test.ts` verifies the Proposed Move button hides and re-shows the same cached preview without consuming another call, and that player-turn coach text uses the authoritative round active color when chessground turn state lags.

Notes:
Integration log entry: `INT-2026-099`. This keeps browser calls same-origin through the ECL bridge; it does not add direct browser-to-ECE traffic or change legal move handling. Follow-up hardening on 2026-06-02 added delayed non-duplicate current-position checks after move updates and immediate current-board retries when an in-flight ECE response returns stale, so local/computer replies cannot leave a stale fallback shell until page refresh.

### PM-2026-096 - V2 board-measured levels and eval sizing

File touched:
`ui/round/src/view/main.ts`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-C-V2-012; REQ-C-V2-015; REQ-M-V2-033; REQ-N-V2-020.

Why core Lichess file had to be touched:
The native round view owns the rendered board size and native game metadata card. CSS viewport math could not reliably keep the EvenChess eval rail and level selector proportional to the actual board at different browser zoom/height settings, so the round view now measures the rendered board and side card and exposes those sizes as CSS variables for the EvenChess layout.

Could this be isolated later:
Partly. The measuring hook is namespaced to EvenChess live layout, but it still attaches to the native round app root because the board, side card, and table are upstream round elements.

Upstream merge risk:
Medium. `ui/round/src/view/main.ts` and `ui/round/css/_layout.scss` are upstream round seams. Preserve the measurement hook and board-height CSS variables if the upstream round DOM or grid changes.

Tests added or updated:
No automated test was added for measured layout. Verification used TypeScript checking, Test Ground UI build, `git diff --check`, and browser measurement of board/eval/levels dimensions after loading the rebuilt round bundle.

Notes:
Integration log entry: `INT-2026-100`. This is layout-only; it does not alter move legality, ECE payload parsing, level gating, clocks, or game lifecycle.

### PM-2026-097 - V2 local Test Ground browser move bridge

File touched:
`ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGroundMoveBridge.ts`; `ui/round/tests/evenchessTestGroundMoveBridge.test.ts`

EvenChess requirement:
REQ-V-V2-003; REQ-V-V2-006; REQ-V-V2-007A.

Why core Lichess file had to be touched:
The native round controller owns playable move submission, legal move destinations, and the existing `pluginMove` path. Browser automation could hit-test directly on `cg-board` but still fail to trigger Chessground click/drag moves in the Codex in-app browser. The local Test Ground now has localhost-only DOM event and URL-parameter bridges that validate active-player state, current turn, UCI format, and legal destinations before delegating to the existing `pluginMove` socket path. This gives QA a reliable way to advance a round and verify post-move ECE overlay refreshes without changing normal board input.

Could this be isolated later:
Yes. The validation helper is isolated in an EvenChess-namespaced round file, and the controller seam can be removed if browser automation can reliably drive Chessground directly or if Test Ground gains a server-side authenticated move runner.

Upstream merge risk:
Medium. `ui/round/src/ctrl.ts` is a core round file. Preserve the local-only guard and existing `pluginMove` delegation if rebasing round-controller input changes.

Tests added or updated:
`ui/round/tests/evenchessTestGroundMoveBridge.test.ts` verifies localhost-only gating, UCI detail parsing, URL-parameter parsing, legal move validation, illegal move rejection, and promotion role mapping.

Notes:
Integration log entry: `INT-2026-101`. This is local Test Ground automation infrastructure only; it does not expose ECE, change legal move generation, change player clicks/drags, or alter production move permissions.

### PM-2026-098 - V2 full-FEN ECE round adapter keys

File touched:
`ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessTestGround.test.ts`

EvenChess requirement:
REQ-J-V2-004 through REQ-J-V2-008; REQ-L-V2-022 through REQ-L-V2-025; REQ-V-V2-006; REQ-V-V2-007A.

Why core Lichess file had to be touched:
Lichess round steps expose board-placement FENs in the live round UI, while ECE board-state mode requires a full FEN with side to move. The native round controller and overlay renderer own the post-move FEN handoff, stale-payload comparison, and clear instruction key. EvenChess now normalizes the Test Ground board-state request, renderer snapshot, proposed-move request key, and move-played clear key to one full-FEN value so an ECE payload returned after a move is accepted and rendered without a browser refresh.

Could this be isolated later:
Partly. The normalization helper is EvenChess-namespaced, but the controller seam remains needed until the production ECE transport receives a server-authored full board-state key directly from round metadata.

Upstream merge risk:
Medium. `ui/round/src/ctrl.ts` is a core round file. Preserve the full-FEN adapter key if upstream changes how round step FENs are represented.

Tests added or updated:
`ui/round/tests/evenchessTestGround.test.ts` verifies board-placement FENs are expanded to full FENs before ECE bridge requests, and that already-full FENs plus non-FEN test keys are preserved.

Notes:
Integration log entry: `INT-2026-102`. This fixes the observed local/Test Ground failure where manual full-FEN backend smoke calls returned ECE payloads, but live post-move UI requests sent board-only FENs and then rejected or retried empty/stale payloads until a page refresh.

### PM-2026-099 - V2 proposed/potential move split and quotas

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
Appendix J.12 proposed-move and potential-move integration; Appendix N level gates; OVR-V2-016; ECE combined requirements candidate/proposed-move split and caller-owned quota model.

Why core Lichess file had to be touched:
The existing same-origin Test Ground bridge and native round overlay own the browser-facing proposed-move button, potential/candidate overlay display, and ECE payload normalization for local development. The bridge needed to accept ECE proposed-move coaching text without requiring a public `legal` display field, while the round overlay needed to hide potential/candidate arrows until ECL's own reveal buttons and per-game quotas allow them.

Could this be isolated later:
Partly. Potential/proposed display state is isolated in EvenChess round overlay code, but the Test Ground controller bridge remains the same-origin server-to-server ECE seam until production ECE transport is moved behind a dedicated module endpoint.

Upstream merge risk:
Medium. `app/controllers/EvenChess.scala` and round UI files are active integration seams. Preserve same-origin browser calls, server-to-server ECE calls, optional proposed `legal` handling, and local potential reveal gating when rebasing.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies proposed-move quota thresholds, potential-move quota thresholds, cached potential reveal toggling, hidden-by-default potential arrows, and reveal-gated board overlays. `ui/round/tests/evenchessTestGround.test.ts` verifies the ECE Test Ground bridge remains stable after the adapter change.

Notes:
Integration log entry: `INT-2026-103`. This supersedes the old assumption that candidate moves auto-render when present and that proposed-move help is limited per turn instead of per game.

### PM-2026-100 - V2 coach-card top alignment and eval strip

File touched:
`ui/round/css/_layout.scss`; `ui/round/css/_app-layout.scss`; `ui/round/css/_evenchess-live.scss`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-C-V2-015; REQ-N-V2-023; REQ-P-V2-004.

Why core Lichess file had to be touched:
The native round grid owns the board, voice row, coach column, and move-table placement. EvenChess needs the coach card's top edge to align with the rendered board top while preserving the already aligned level card and eval rail, so the EvenChess-specific grid area maps the coach column into the top board row on desktop layouts.

Could this be isolated later:
Partly. The eval strip and card styling are EvenChess-namespaced, but the desktop grid placement still needs the native round layout seam unless upstream exposes a board-adjacent coach extension point.

Upstream merge risk:
Medium. `ui/round/css/_layout.scss` and `ui/round/css/_app-layout.scss` are upstream layout files. Preserve the EvenChess-live `coach` area starting in the top board row when rebasing round layout changes.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies eval text renders as a coach-card strip and is controlled by the Eval Text toggle separately from the vertical eval bar.

Notes:
Integration log entry: `INT-2026-104`. This is display-only; it does not alter ECE payload calls, move legality, clocks, level authority, or game lifecycle.

### PM-2026-101 - V2 ECE numeric evaluation bridge normalization

File touched:
`app/controllers/EvenChess.scala`

EvenChess requirement:
REQ-J-V2 ECE server-to-server boundary; REQ-N-V2-023; REQ-P-V2-004.

Why core Lichess file had to be touched:
The local same-origin Test Ground bridge is the Lichess controller seam that converts ECE public board-state output into approved round overlay payloads. ECE quick payloads may return `evaluation` as a number or numeric string, while deep payloads may return an object. The bridge must normalize each supported public shape into an `ece.eval` visual so the round coach-card eval strip has a displayable eval source without exposing ECE internals.

Could this be isolated later:
Yes. Move the public ECE payload parser into an EvenChess module once the production live ECE scheduler/socket path replaces the Test Ground controller endpoint.

Upstream merge risk:
Low to medium. `app/controllers/EvenChess.scala` is a namespaced EvenChess controller, but future route/controller or ECE bridge refactors must preserve numeric and object-shaped eval normalization.

Tests added or updated:
No new Scala controller test was added because the parser is currently private to the Play controller seam. Existing UI eval-strip coverage verifies the renderer consumes normalized `ece.eval` visuals; broad Scala compile verifies the controller patch.

Notes:
Integration log entry: `INT-2026-105`. This keeps browser traffic same-origin and server-to-server; it does not add direct browser-to-ECE calls.

### PM-2026-102 - V2 ECE attackable hanging-piece ownership normalization

File touched:
`app/controllers/EvenChess.scala`

EvenChess requirement:
REQ-O-V2-010 through REQ-O-V2-012; REQ-O-V2-020 through REQ-O-V2-022; REQ-V-V2-007.

Why core Lichess file had to be touched:
The same-origin Test Ground bridge converts ECE public overlay payloads into approved round overlay atoms. ECE can describe attackable hanging pieces either as object entries with owner fields or as simple square strings. The bridge now accepts string square entries, explicit `student_hanging_attackable`/`opponent_hanging_attackable` arrays, and additional owner field names so the board renderer can apply the correct red student or purple opponent marker/rim instead of dropping the square or losing ownership.

Could this be isolated later:
Yes. Move this public ECE payload parser into an EvenChess module when the production live ECE transport replaces the Test Ground controller bridge.

Upstream merge risk:
Low to medium. `app/controllers/EvenChess.scala` is namespaced to EvenChess but remains a Play controller seam. Preserve string-square and owner-aware hanging-piece normalization when refactoring the bridge.

Tests added or updated:
Existing round overlay tests verify student attackable hanging pieces render red with an inner rim and opponent attackable hanging pieces render purple with an inner rim. Broad Scala compile verifies the controller parser patch.

Notes:
Integration log entry: `INT-2026-106`. If ECE sends opponent-owned attackable hanging pieces as anonymous generic strings with no owner or opponent-specific array, ECL cannot infer ownership; ECE should include an owner field or use the opponent-specific public array.

### PM-2026-103 - V2 server-side proposed/potential move consumables

File touched:
`app/controllers/EvenChess.scala`; `conf/routes`; `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
Appendix J.12 proposed-move and potential-move integration; REQ-P-V2-003, REQ-P-V2-003A, REQ-P-V2-003B; OVR-V2-017; ECE combined requirements section 3.1 caller-owned quota model.

Why core Lichess file had to be touched:
The same-origin Test Ground ECE bridge is the current Lichess controller seam for local live ECE calls, and the native round overlay owns the browser-facing buttons. Proposed and potential move consumables must be enforced server-side so refreshing the browser cannot restore uses, while potential move data must be withheld from ordinary board-state responses until the reveal endpoint authorizes it.

Could this be isolated later:
Yes. Move the process-local Test Ground counters into the production live assistance/game-history service once the server-side scheduler/socket path replaces local Test Ground polling. Keep the browser as a display/cache mirror only.

Upstream merge risk:
Medium. `conf/routes`, `app/controllers/EvenChess.scala`, and round UI files are active seams. Preserve the same-origin reveal endpoint, server-side counters, and potential-data suppression when rebasing ECE bridge or round renderer changes.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies server-authorized potential reveals, cached show/hide without extra fetches, retained server consumed count display, and coach-card potential text. `ui/round/tests/evenchessTestGround.test.ts` verifies the same-origin potential reveal URL contract.

Notes:
Integration log entry: `INT-2026-107`. This supersedes browser-only consumable tracking for proposed/potential move testing; production should persist the same concept in authoritative game assistance state rather than relying on controller process memory.

### PM-2026-104 - V1.1 stable search polling and platform-bot AI handoff

File touched:
`app/controllers/EvenChess.scala`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/table.ts`

EvenChess requirement:
REQ-H-V2-043, REQ-H-V2-044, REQ-H-V2-045, REQ-H-V2-046; ECE v1.1 Phase C, Phase D, Phase E, Phase J.

Why core Lichess file had to be touched:
The public lobby setup controller and status card are upstream Lichess lobby surfaces. The bot fallback timer could not advance because the UI issued one search request and stopped, while each new request created a fresh MMR ticket. The controller seam now resumes search through an opaque public search key, and platform-bot matches hand off once to Lichess's native AI/computer game path without browser-owned matchmaking decisions.

Could this be isolated later:
Partially. `PlaySearchIntegration.scala` owns the MMR ticket resume behavior, but the native lobby submit/polling and one-shot AI game handoff remain Lichess controller/UI seams until a production pairing adapter exists.

Upstream merge risk:
Medium. Future upstream lobby setup-controller changes may conflict with the EvenChess polling lifecycle, and future setup/AI creation refactors may require reapplying the bot game handoff.

Tests added or updated:
`modules/evenchess/src/test/PlaySearchIntegrationTest.scala` verifies an existing search ticket can be resumed without resetting its original wait time or creating duplicate active records.

Notes:
Integration log entry: `INT-2026-108`. This does not expose raw internal MMR ticket ids to the browser; it uses an opaque polling key and returns only public status plus an optional same-origin game redirect. Autonomous bot-vs-bot game execution remains a separate production boundary.

### PM-2026-105 - V2 human MMR contract game handoff

File touched:
`app/controllers/EvenChess.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-H-V2-025, REQ-H-V2-045, REQ-H-V2-047.

Why core Lichess file had to be touched:
The EvenChess play/search controller is the same-origin public search endpoint and the only current adapter between EvenChess MMR contracts and native Lichess game creation. It now handles both platform-bot contracts and human-vs-human contracts. Human contracts create and accept a native Lichess challenge using contract-assigned colors, rated/casual mode, and mapped time control, then cache redirects for both polling tickets and retire both tickets.

Could this be isolated later:
Yes. Move the process-local redirect cache and in-memory policy repository into a production pairing/game-start service when the final persistent EvenChess game policy store is introduced. Keep the browser-facing endpoint as status-only.

Upstream merge risk:
Medium. `app/controllers/EvenChess.scala` is namespaced, but the implementation depends on Lichess challenge creation and acceptance APIs. Future challenge or setup refactors must preserve the finalized-contract handoff and both-player redirect cache.

Tests added or updated:
No new controller haness was added in this patch. Broad Scala compile verifies the challenge handoff type integration; existing EvenChess search tests cover stable tickets, human contract production, and game-start policy persistence service behavior.

Notes:
Integration log entry: `INT-2026-109`. This completes the missing human contract handoff path for public EvenChess search. It does not expose raw MMR tickets, does not let the browser decide pairing, and does not touch ECE internals.

### PM-2026-106 - V1.1 simulation bot tickets visible to MMR search

File touched:
`modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`

EvenChess requirement:
REQ-H-V2-043, REQ-H-V2-044, REQ-V-V2-013.

Why core Lichess file had to be touched:
No upstream Lichess core file was touched. This is inside the EvenChess MMR integration module. Simulation bot tickets were seeded into the shared repository, but the evaluator filtered all bot tickets out of the normal candidate set, so a human search could not match a running simulation bot until a separate fallback bot path fired. The evaluator now treats `ec-sim-` bot tickets as visible queue candidates while keeping generated fallback bots behind the configured timeout.

Could this be isolated later:
Yes. The simulation-ticket prefix should move into a shared bot-operations constant or persistent ticket-type field when the production repository replaces the process-local search repository.

Upstream merge risk:
Low. The change is isolated to EvenChess-owned MMR candidate selection and tests.

Tests added or updated:
`modules/evenchess/src/test/PlaySearchIntegrationTest.scala` adds a regression proving a human search can match a simulation bot ticket immediately without seeding the fallback timeout bot.

Notes:
Integration log entry: `INT-2026-110`. This fixes the observed case where simulation bots were active but searches still did not pair because seeded simulation tickets were invisible to the evaluator.

### PM-2026-107 - V2 potential move reveal side and audit handling

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`

EvenChess requirement:
Appendix J.12 potential-move display requirements; REQ-P-V2-003, REQ-P-V2-003B; OVR-V2-016; OVR-V2-017; ECE combined requirements section 3.1 potential/proposed split and caller-owned reveal policy.

Why core Lichess file had to be touched:
The same-origin Test Ground ECE bridge and native round overlay own the local reveal path. The reveal endpoint now compiles potential moves from the intended side (`player` = requester side, `opponent` = opposite side), instead of trying to infer side from candidate card text. The round renderer accepts a server-authorized reveal payload with its own audit ID, because reveal calls may be separate from the normal board-state overlay call.

Could this be isolated later:
Yes. Move the controller-local potential reveal builder and consumable cache into the production EvenChess assistance service once the live scheduler/socket bridge replaces Test Ground polling.

Upstream merge risk:
Medium. `app/controllers/EvenChess.scala` and round UI files are active integration seams. Preserve same-origin ECL authorization, side-aware reveal selection, server-side consumable accounting, and suppression of normal board-state potential data during rebase.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` now verifies reveal visuals can render when their audit ID differs from the current board overlay audit ID. `modules/evenchess/src/test/EceLiveBridgeTest.scala` verifies all returned potential move visuals are allowed at L5+ rather than treating A/B/C as separate level gates.

Notes:
Integration log entry: `INT-2026-111`. This does not expose ECE directly to the browser and does not copy ECE internals into ECL.

### PM-2026-108 - V2 EvenChess two-state matchmaking and MMR preference rehaul

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/main/BotOperations.scala`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/view/table.ts`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/evenchess/src/test/TelemetryAnalyticsTest.scala`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-G-V2-011 through REQ-G-V2-026; REQ-H-V2-010 through REQ-H-V2-034; REQ-I-V2-010 through REQ-I-V2-014; OVR-V2-018.

Why core Lichess file had to be touched:
The public lobby setup modal, status card, and same-origin EvenChess search controller are the current upstream Lichess seams for public matchmaking. The search UI now exposes a single Preferred Set Level dropdown with Any/L0-L10, sends only the simplified preference field, and removes native manual rating-window controls from the EvenChess search flow. The controller serializes only deployment-safe public status while the EvenChess MMR module owns level assignment and match contracts.

Could this be isolated later:
Partially. The MMR policy and tables are isolated in EvenChess modules, but the lobby modal/status card and controller JSON adapter must remain Lichess seams until a dedicated production EvenChess search adapter replaces them.

Upstream merge risk:
Medium. Future upstream lobby setup, setup controller, or search-controller changes can conflict with the EvenChess modal fields, polling lifecycle, and public status JSON.

Tests added or updated:
`modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/evenchess/src/test/TelemetryAnalyticsTest.scala`; `ui/lobby/tests/evenchessSetup.test.ts`.

Notes:
Integration log entry: `INT-2026-112`. The initial deployed policy table is L0=0, L1=5, L2=10, L3=18, L4=28, L5=45, L6=65, L7=90, L8=120, L9=155, L10=190. Lower-player base Set Level uses the active rating ladder documented in Appendix H/Z.

### PM-2026-109 - ECOR table calibration and admin controls

File touched:
`app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `conf/routes`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`; `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/AdminOpsDashboard.scala`; `modules/evenchess/src/main/EvenChessRatingCalibration.scala`; `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/AdminOpsDashboardTest.scala`; `modules/evenchess/src/test/EvenChessRatingCalibrationTest.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-F-V2-016; REQ-H-V2-034 through REQ-H-V2-036; REQ-I-V2-020 through REQ-I-V2-026; OVR-V2-019.

Why core Lichess file had to be touched:
The existing Lichess dev/settings route and UI are the admin shell for EvenChess operational controls. The ECOR panel is added behind the existing `Secure(_.Settings)` permission, while public matchmaking activates the current ECOR SettingStore values before creating or polling search tickets. No browser-facing search JSON exposes raw ECOR internals.

Could this be isolated later:
Yes. Move the process-local calibration sample collector to the production EvenChess rating/audit store when the permanent game-settlement repository is introduced. Keep the admin ECOR table editor and calibration decision flow behind Lichess admin permissions.

Upstream merge risk:
Medium. `Dev.scala`, `DevUi.scala`, `Env.scala`, and `conf/routes` are upstream admin/settings seams. Preserve ECOR settings, snapshot history, and admin-only calibration actions when upstream dev settings or route code changes.

Tests added or updated:
`EvenChessRatingCalibrationTest` covers ECOR parsing, default cumulative offsets, rating-to-level mapping, snapshot history, calibration statistics, and sample dedupe. Existing admin/MMR tests now cover ECOR settings, dashboard exposure, and rated-game sample collection.

Notes:
Integration log entry: `INT-2026-113`. Active matchmaking now reads ECOR cumulative offsets from the admin-editable gap table. The initial adjacent gap values are L0-L1=5, L1-L2=5, L2-L3=8, L3-L4=10, L4-L5=17, L5-L6=20, L6-L7=25, L7-L8=30, L8-L9=35, L9-L10=35.

### PM-2026-110 - V2 EvenChess friend challenge level contracts

File touched:
`app/controllers/Challenge.scala`; `app/controllers/EvenChess.scala`; `app/controllers/Setup.scala`; `modules/challenge/src/main/Challenge.scala`; `modules/challenge/src/main/JsonView.scala`; `modules/challenge/src/main/ui/ChallengeUi.scala`; `modules/evenchess/src/main/GamePolicy.scala`; `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `ui/challenge/src/interfaces.ts`; `ui/challenge/src/view.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/setup/modal.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-G-V2-027 through REQ-G-V2-029; REQ-H-V2-048; OVR-V2-020.

Why core Lichess file had to be touched:
Native Lichess friend challenges are created through `Setup.friend`, accepted through `Challenge.accept`, and displayed through the core challenge model, JSON presenter, challenge page, and browser challenge dropdown UI. EvenChess friend challenges must keep that native recipient flow instead of entering the generic public search queue, while carrying a deployment-safe summary of the resolved Set Levels to the recipient before acceptance and persisting the resolved Set Levels before live coaching can render.

Could this be isolated later:
Partly. The friend-level assignment helper is EvenChess-owned, but the native friend challenge submit and display seams remain Lichess challenge/setup files unless a dedicated EvenChess friend-contract challenge extension point is introduced.

Upstream merge risk:
Medium. `Setup.scala`, `Challenge.scala`, `JsonView.scala`, and `ChallengeUi.scala` are upstream challenge/setup seams. Preserve native friend routing, recipient-aware level metadata, and deployment-safe request-card copy when rebasing.

Tests added or updated:
`modules/evenchess/src/test/LevelBasedMatchmakingTest.scala` covers Auto level, Set my level, Set opponent level, and Set both levels friend-contract assignment.

Notes:
Integration log entry: `INT-2026-114`. Friend setup now posts to native `/setup/friend` with friend-specific level fields. It does not expose raw ECOR internals, ticket ids, or private MMR diagnostics.

### PM-2026-111 - V2 mirrored side-output Offset Count contract

File touched:
`app/controllers/EvenChess.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-O-V2-006A through REQ-O-V2-006C; OVR-V2-021; ECE contract section 10.4.

Why core Lichess file had to be touched:
The same-origin ECL-to-ECE bridge normalizes ECE public side outputs into approved round overlay atoms. ECE Offset Count now returns mirrored board facts to each side output and signs `piece_count_delta` from that side output's perspective, so the bridge must prefer `target_square` and signed `piece_count_delta` and must not treat offset entries as opponent-target-only data.

Could this be isolated later:
Yes. Move the ECE public payload adapter out of the controller into a dedicated EvenChess adapter module once the production live ECE scheduler/socket path replaces the Test Ground bridge.

Upstream merge risk:
Low to medium. `app/controllers/EvenChess.scala` is a namespaced controller seam, but future ECE bridge refactors must preserve mirrored side-output offset semantics and signed value priority.

Tests added or updated:
`scripts/evenchess-test-ece-server.test.mjs` verifies the Test ECE fixture emits mirrored `target_square` entries with flipped `piece_count_delta` values for white and black. `ui/round/tests/evenchessOverlay.test.ts` verifies the board layer renders every signed offset visual it receives, including own and opponent targets, and the Offset Count toggle hides all of them.

Notes:
Integration log entry: `INT-2026-115`. This keeps browser code on the same-origin ECL path and does not expose ECE internals or raw diagnostics.

### PM-2026-112 - V2 preferred set level submit normalization

File touched:
`ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`

EvenChess requirement:
OVR-V2-018 simplified public search model: Preferred Set Level is either absent/Any or a concrete L0-L10 value.

Why core Lichess file had to be touched:
The native lobby setup controller owns the submit URL for EvenChess hook searches. It must not forward stale stored `"any"` values as `preferredSetLevel=any`; absence is the canonical no-preference state.

Could this be isolated later:
Yes. Keep preference-param normalization in the EvenChess lobby helper and call it from any future setup-submit adapter.

Upstream merge risk:
Low. This changes only the EvenChess-specific branch of lobby setup submission.

Tests added or updated:
`ui/lobby/tests/evenchessSetup.test.ts` verifies empty, whitespace, and `"any"` omit the submit param while L0-L10 are retained.

Notes:
Integration log entry: `INT-2026-116`. Backend ignore behavior remains as a compatibility fallback, but the browser now submits only the revised contract shape.

### PM-2026-113 - V2 quick-pairing cards use EvenChess search and bot fallback polling

File touched:
`ui/lobby/src/ctrl.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/src/view/pools.ts`; `ui/lobby/src/view/tabs.ts`; `ui/lobby/src/view/table.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-G-V2-029A; REQ-G-V2-029B; OVR-V2-022.

Why core Lichess file had to be touched:
Native quick-pairing cards are rendered and handled by the Lichess lobby controller/view. Public EvenChess quick play must keep the native active/spinner UX while creating an EvenChess search contract, not an ordinary native pool ticket, so the lobby click path now starts the EvenChess search JSON flow with the remembered search settings and clicked clock bucket. The polling loop keeps checking a valid search key until a game redirect is returned.

Could this be isolated later:
Partly. The time-bucket mapping and search parameter normalization are EvenChess helper code, but the quick-card click seam remains in the Lichess lobby controller unless upstream introduces a pluggable pool-search adapter.

Upstream merge risk:
Medium. `ctrl.ts`, `setupCtrl.ts`, and the pool/tab views are native lobby seams. Preserve the separate `evenChessPoolMember` marker so the UI can show the native spinner without sending a native `poolIn` socket message.

Tests added or updated:
`ui/lobby/tests/evenchessSetup.test.ts` covers quick-pairing clock to EvenChess time-control mapping. `PlaySearchIntegrationTest` continues to cover fallback bot seeding and matching after timeout.

Notes:
Integration log entry: `INT-2026-117`. Browser verification confirmed 5+0 and 1+0 quick searches used the hidden EvenChess search contract path, kept the verbose diagnostics hidden by default, and redirected into bot-created games after the configured fallback timeout once the live backend was restarted to load the current controller code.

### PM-2026-114 - V2 admin-editable Stockfish equivalent rating table

File touched:
`app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`; `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/AdminOpsDashboard.scala`; `modules/evenchess/src/main/EvenChessRatingCalibration.scala`; `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/AdminOpsDashboardTest.scala`; `modules/evenchess/src/test/EvenChessRatingCalibrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-F-V2-017; REQ-H-V2-046A; OVR-V2-023.

Why core Lichess file had to be touched:
The existing Lichess dev/settings route and UI are the admin shell for EvenChess backend controls. The Stockfish AI equivalent rating bands must be visible/editable there and activated before public EvenChess search creates or polls platform-bot contracts.

Could this be isolated later:
Partly. The parser/runtime table is EvenChess-owned, but the admin SettingStore registration, Dev controller action, and Dev UI form remain Lichess admin seams until a dedicated EvenChess admin module exists.

Upstream merge risk:
Medium. `Dev.scala`, `DevUi.scala`, and `Env.scala` are upstream admin/settings seams. Preserve the namespaced `evenchess.backend.stockfish.equivalentRatingBands` setting and validation/activation behavior when rebasing.

Tests added or updated:
`EvenChessRatingCalibrationTest` covers Stockfish equivalent table parsing and validation. `AdminBackendSettingsTest` and `AdminOpsDashboardTest` verify settings/model exposure.

Notes:
Integration log entry: `INT-2026-118`. Platform-bot native AI handoff now reads `botProfile.stockfishLevel`, which is derived from the active admin-editable SF1-SF8 rating table, instead of directly reusing EvenChess Set Level.

### PM-2026-115 - V1.1 simulation bots use configured ratings and playable pools

File touched:
`modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-H-V2-043; REQ-H-V2-044; REQ-H-V2-044A; REQ-H-V2-044B; REQ-H-V2-044C; OVR-V2-024.

Why core Lichess file had to be touched:
No upstream Lichess core file was touched. The change is inside the EvenChess search/MMR and bot-operations modules and tests. Simulation tickets still flow through the shared EvenChess repository and MMR contract path, but are now distributed across playable time-control pools instead of one default pool.

Could this be isolated later:
Yes. When the local process repository is replaced by a production persistent search repository, keep simulation bot ticket creation/replenishment in the EvenChess bot-operations module and preserve the same ticket metadata contract.

Upstream merge risk:
Low. The implementation is EvenChess-owned. The remaining production risk is autonomous bot-vs-bot game execution, which requires a real bot account/runner boundary rather than only queue tickets.

Tests added or updated:
`BotOperationsTest` verifies simulation tickets use configured target ECR values and reseeding restores a consumed ticket without duplicating the active population. `PlaySearchIntegrationTest` verifies bot-profile tickets persist target ECR as their active ticket rating.

Notes:
Integration log entry: `INT-2026-119`. Human searches can match simulation bot tickets with matchmaking fallback disabled. The matched simulation bot hands off to native Lichess AI with the rating-derived Stockfish level and native clock config.

### PM-2026-116 - V2 deep-only Stockfish eval display

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-N-V2-023; REQ-N-V2-023A.

Why core Lichess file had to be touched:
The same-origin ECL-to-ECE bridge and round overlay renderer own the payload fields that feed the EvenChess eval bar and coach eval strip. Quick ECE board payloads can carry placeholder zero eval values, so the bridge must only expose Stockfish eval visuals from accepted deep/advanced payloads, and the round UI must prefer the latest delivered eval visual.

Could this be isolated later:
Yes. Move ECE quick/deep payload normalization from the controller into a dedicated EvenChess adapter module when the live scheduler replaces the Test Ground bridge.

Upstream merge risk:
Low to medium. `app/controllers/EvenChess.scala` is namespaced, while `ui/round/src/view/evenchessOverlay.ts` is a native round-view seam with EvenChess-only UI branches.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies a later advanced eval visual overrides an earlier quick zero eval for the coach eval strip.

Notes:
Integration log entry: `INT-2026-120`. Quick-phase eval placeholders no longer update the eval bar or eval status strip.

### PM-2026-117 - V2 proposed-move post-move side-output preview

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-V-V2-009; Appendix J.12 Proposed-Move Integration.

Why core Lichess file had to be touched:
The same-origin ECL-to-ECE bridge receives ECE proposed-move responses and the native round overlay renderer owns how coach cards and board visuals are swapped while a proposed preview is active. ECE now returns post-move deterministic data under `proposed_move_evaluation.after_move_side_output`, so ECL must normalize that nested side output into the existing approved display payload shape before the browser can render it.

Could this be isolated later:
Yes. Move proposed-move response normalization from `app/controllers/EvenChess.scala` into a dedicated EvenChess ECE adapter module when the live scheduler/bridge is extracted from the controller.

Upstream merge risk:
Low to medium. The controller is EvenChess-owned, while `ui/round/src/view/evenchessOverlay.ts` remains a native round-view seam with EvenChess-only branches.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies an active proposed-move preview can render cached post-move ECE cards/visuals and toggle back to the normal board-state display without another request.

Notes:
Integration log entry: `INT-2026-121`. Legal proposed-move responses now use nested `proposed_move_evaluation.after_move_side_output`; illegal/invalid `0` values are not rendered. When legal nested post-move cards/visuals are normalized but ECE omits `new_fen`, the bridge now sends the normalized payload identity as `postMoveBoardStateKey` so the strict browser renderer can apply the preview instead of displaying only the text card. The round renderer also treats the nested post-move cards/visuals audit id as the active proposed overlay audit id, allowing the proposed text card and post-move payload to keep distinct audit identities.

### PM-2026-118 - V2 stable live overlay refresh during ECE loads

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-L-V2-023; REQ-L-V2-024; REQ-L-V2-027.

Why core Lichess file had to be touched:
The native round overlay renderer controls the EvenChess sidebar and board-attached overlay DOM. Move-triggered ECE refreshes must avoid remounting the whole live shell while still hiding stale board-square visuals from the previous FEN.

Could this be isolated later:
Partly. Keep the stale-payload checks in the ECE/live adapter, but the stable key/rendering behavior remains in the round UI seam unless EvenChess gets a fully separate round layout host.

Upstream merge risk:
Low to medium. `ui/round/src/view/evenchessOverlay.ts` is a native round-view seam with EvenChess-only branches.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies move clears retain a safe coach text snapshot, keep the live panel key stable across ply/FEN changes, and continue hiding stale board visuals until the next current-position payload arrives.

Notes:
Integration log entry: `INT-2026-123`. This improves visual continuity without showing stale board markers as current advice.

### PM-2026-119 - V2 potential/proposed move side-state correction

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
Appendix J.12 Proposed-Move Integration; Potential-move display requirements.

Why core Lichess file had to be touched:
The same-origin ECL bridge controls which ECE side-output is revealed for potential moves, and the native round overlay renderer owns proposed-move preview state. Opponent potential reveals must use the opponent side perspective, and invalid proposed-move button clicks must not clear an already-displayed legal preview.

Could this be isolated later:
Partly. Potential/proposed request normalization can move into an EvenChess adapter module, but the proposed preview state machine remains in the round UI seam.

Upstream merge risk:
Low to medium. `app/controllers/EvenChess.scala` is EvenChess-owned; `ui/round/src/view/evenchessOverlay.ts` is a native round-view seam with EvenChess-only branches.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies opponent potential reveal accepts black-side perspective for a white requester, and illegal proposed-move clicks preserve the current active proposed preview.

Notes:
Integration log entry: `INT-2026-124`. This fixes white requester opponent-potential reveals being stamped as white perspective and prevents illegal proposed-move clicks from blanking the preview.

### PM-2026-120 - V2 deep eval object parsing and quick-zero suppression

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-N-V2-023; REQ-N-V2-023A.

Why core Lichess file had to be touched:
The server-side ECE bridge normalizes quick/deep ECE payloads before approved display payloads reach the browser, and the native round overlay renderer owns the eval bar and coach-card eval strip. ECE deep addenda now return normalized evaluation objects such as `evaluation.score.cp`, while quick and unavailable provider payloads can still contain literal `evaluation: 0` placeholders. The bridge must parse only real provider-backed deep eval values, and the round UI must ignore quick placeholder eval text.

Could this be isolated later:
Yes. Move ECE quick/deep eval normalization from `app/controllers/EvenChess.scala` into a dedicated EvenChess ECE adapter module when the live scheduler replaces the Test Ground bridge.

Upstream merge risk:
Low to medium. `app/controllers/EvenChess.scala` is EvenChess-owned; `ui/round/src/view/evenchessOverlay.ts` is a native round-view seam with EvenChess-only branches.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies the eval strip accepts deep/provider-backed eval, prefers a later advanced eval over an earlier quick zero, and does not render an eval strip for a quick placeholder zero alone.

Notes:
Integration log entry: `INT-2026-125`. Quick-phase `0` eval placeholders no longer render as `Equal 0.00`, and normalized deep eval objects with `score.cp` now feed the eval bar/status strip.

### PM-2026-121 - V2 per-game FEN payload cache and replay lookup

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`

EvenChess requirement:
REQ-L-V2-010; REQ-L-V2-014; REQ-L-V2-015; REQ-M-V2-004; REQ-M-V2-005.

Why core Lichess file had to be touched:
The temporary same-origin ECL/Test Ground bridge is the current server-authoritative path that calls ECE and delivers approved live overlay payloads to the round UI. Board-history replay also runs through the native round controller/adapter, so it must not suppress EvenChess cache lookup solely because the user is replaying earlier plies.

Could this be isolated later:
Yes. Move the in-memory Test Ground cache into the production live ECE scheduler/history repository once persistent per-game ECE history replaces the local bridge.

Upstream merge risk:
Low to medium. `app/controllers/EvenChess.scala` is EvenChess-owned, while `ui/round/src/evenchessTestGround.ts` remains an EvenChess-only round adapter seam.

Tests added or updated:
`ui/round/tests/evenchessTestGround.test.ts` verifies replay/history navigation still requests and applies EvenChess payloads.

Notes:
Integration log entry: `INT-2026-126`. Normal board payloads are cached only after potential-move data is suppressed, so cached replay cannot expose unrevealed consumable potential moves. A higher requested Used Level bypasses the cache and refreshes ECE for that FEN.

### PM-2026-122 - V2 saved match ECE payload history and L10 full-game review backfill

File touched:
`app/controllers/EvenChess.scala`; `conf/routes`; `modules/evenchess/src/main/AnalysisMemory.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `modules/evenchess/src/test/AnalysisMemoryTest.scala`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_FULL_MATCH_PAYLOAD_CONTRACT.md`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`

EvenChess requirement:
REQ-L-V2-014; REQ-L-V2-015; REQ-L-V2-016; REQ-M-V2-005; REQ-M-V2-026; REQ-M-V2-027.

Why core Lichess file had to be touched:
The temporary same-origin ECL/Test Ground bridge is the current server-authoritative route that calls ECE, caches approved display payloads by game/FEN/ply/side/level, and serves history replay without letting browser code call ECE directly. Full-game review also needs a route seam so ECL can call ECE `/v1/ece/game-review`, validate display-safe per-frame side outputs, and convert them into the same round overlay payload shape used by live play.

Could this be isolated later:
Yes. Move the in-memory Test Ground cache and full-game backfill route into the production saved-game ECE history repository and live ECE scheduler once persistent storage replaces the local bridge.

Upstream merge risk:
Medium. `app/controllers/EvenChess.scala` and `conf/routes` define same-origin server seams; `ui/round/src/evenchessTestGround.ts` and `ui/round/src/view/evenchessOverlay.ts` are native round-view seams with EvenChess-only branches.

Tests added or updated:
`modules/evenchess/src/test/AnalysisMemoryTest.scala` verifies highest-level per-FEN storage and full-game frame attachment. `modules/evenchess/src/test/EceLiveBridgeTest.scala` verifies stored review frames compile into the live overlay shape. `ui/round/tests/evenchessTestGround.test.ts` verifies history-only replay lookup and L10 full-game review frame posting. `ui/round/tests/evenchessOverlay.test.ts` covers the replay review control while preserving current live overlay behavior.

Notes:
Integration log entry: `INT-2026-127`. Proposed-move previews are intentionally excluded from saved match history. Full-game ECE responses are accepted only when diagnostics are displayable and forbidden public ECE fields are absent; otherwise the local bridge falls back to sequential server-side board-state backfill for testing continuity.

### PM-2026-123 - V2 launch free-match-token window

File touched:
`app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `app/views/lobby/home.scala`; `app/views/evenchess/play.scala`; `modules/web/src/main/Env.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/css/_setup.scss`; `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/main/SubscriptionTokensAds.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/evenchess/src/test/SubscriptionTokensAdsTest.scala`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-E-V2-024; REQ-F-V2-018; REQ-G-V2-017; OVR-V2-025.

Why core Lichess file had to be touched:
The native lobby setup modal and same-origin EvenChess search controller own public rated/casual search access. The existing admin/backend settings surface owns operator configuration. A launch-period token waiver must be server-authoritative at search admission and visible in the native setup card without exposing internal token-gate reason keys.

Could this be isolated later:
Partly. The token-window model remains EvenChess-owned in `AdminBackendSettings` and `PlaySearchIntegration`; the lobby view/modal seams stay necessary while EvenChess is sewn into native Lichess setup.

Upstream merge risk:
Low to medium. Most changes are EvenChess-owned. The lobby setup modal, lobby JSON, dev settings registry, and setup CSS are upstream seams with EvenChess-only additions.

Tests added or updated:
`AdminBackendSettingsTest` covers setting IDs, defaults, active-window validation, and public copy. `PlaySearchIntegrationTest` covers rated admission when an empty free account is inside the launch free-token window and rejection outside it. `SubscriptionTokensAdsTest` covers meaningful-play settlement preserving existing game-token balances during the window. `ui/lobby/tests/evenchessSetup.test.ts` covers the setup-card free-token message and token-gate copy.

Notes:
Integration log entry: `INT-2026-128`. This does not mint tokens or change fairness; it only waives rated/casual game-start token/ad access during a bounded admin-configured date window and displays `Tokens are temporarily free`.

### PM-2026-124 - V2 ECE full-match v1.2 canonical payload and summary call

File touched:
`app/controllers/EvenChess.scala`; `conf/routes`; `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/requirements/EVENCHESS_FULL_MATCH_PAYLOAD_CONTRACT.md`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-M-V2-026; REQ-M-V2-026A; REQ-M-V2-027.

Why core Lichess file had to be touched:
The same-origin ECL/Test Ground bridge and route table own the current server-authoritative path from review UI to ECE. ECE v1.2 makes `/v1/ece/full-match` the explicit post-game route and returns canonical `evenchess_full_game.turns[]`; ECL must parse that shape, convert each turn's public side output into the same approved overlay frame used by live play, and expose a same-origin bridge for `/v1/ece/full-match-summary`.

Could this be isolated later:
Yes. Move the in-memory Test Ground bridge into the production ECE review scheduler/history repository once persistent saved-game analysis storage replaces the local bridge.

Upstream merge risk:
Medium. `app/controllers/EvenChess.scala` and `conf/routes` are route/controller seams. The EngineGateway changes are EvenChess-owned.

Tests added or updated:
`EngineGatewayTest` covers the full-match and full-match-summary URLs plus `mode=full_match`. `LiveCoachingTest` covers the post-game request mode. `evenchessTestGround.test.ts` still covers the same-origin full-game review bridge.

Notes:
Integration log entry: `INT-2026-129`. Legacy top-level `frames` and `move_outputs` remain parser fallbacks only. New full-match storage/interchange should use `evenchess_full_game.turns[].ece_payload`.

### PM-2026-125 - ECL multi-user load harness command

File touched:
`package.json`

EvenChess requirement:
Production launch hardening requires an ECL-owned multi-user load harness that can run beside ECE clean load tests and exercise the current same-origin ECL-to-ECE bridge seams.

Why core Lichess file had to be touched:
`package.json` is the repo command registry. Adding `evenchess:load:ecl` and `evenchess:load:ecl:test` makes the harness discoverable without changing runtime Lichess behaviour.

Could this be isolated later:
Yes. If the repo gains an EvenChess-specific task runner, the commands can move there while the script remains under `scripts/`.

Upstream merge risk:
Low. Future package script conflicts are easy to reapply.

Tests added or updated:
`scripts/evenchess-ecl-load-harness.test.mjs`

Notes:
Integration log entry: `INT-2026-130`. The current harness exercises reachable local/Test Ground surfaces and records auth-required bot/search phases. Staging must extend the same harness with real test-user auth, sim-bot matching, game creation, move flow, and replay-cache verification before public launch.

### PM-2026-126 - Review request processing indicator

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-M-V2-026B.

Why core Lichess file had to be touched:
The native round/replay overlay is the current analysis surface where a user can request the full-game level-10 EvenChess review. The request can take long enough that the user needs an explicit processing state, and the button must remain disabled while that request is in flight.

Could this be isolated later:
Partly. The spinner component can move into an EvenChess-specific shared UI helper once performance-summary and match-summary production routes are wired outside the Test Ground review bridge.

Upstream merge risk:
Low to medium. The touched TypeScript and SCSS files are native round-view seams already carrying EvenChess overlay UI.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies the replay review request button renders an inline spinner, accessible status label, and processing copy while in flight.

Notes:
Integration log entry: `INT-2026-131`. No profile performance-summary request route was present in this checkout, so this patch covers the existing full-game review request surface and records the broader summary-spinner requirement for the future production performance-summary route.

### PM-2026-127 - Bot simulation admin wording and help text

File touched:
`app/controllers/Dev.scala`; `modules/web/src/main/ui/DevUi.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-F-V2-014A.

Why core Lichess file had to be touched:
The Dev/admin bot-operations page is the protected operator surface for EvenChess matchmaking fallback and simulation bots. It previously rendered duplicate simulation settings and exposed internal queue language such as seed/tickets, which made the controls hard to understand for operations testing.

Could this be isolated later:
Partly. The wording and help model can move into an EvenChess admin presenter once the bot-operations page is split from the generic Dev UI.

Upstream merge risk:
Low to medium. The touched files are admin-only seams with EvenChess-specific sections.

Tests added or updated:
`BotOperationsTest` verifies the visible simulation summary avoids seed/ticket wording and uses queue/simulated-player language.

Notes:
Integration log entry: `INT-2026-132`. Internal route/action names are unchanged for compatibility, but visible UI and flash/status text now use operator-readable labels.

### PM-2026-128 - Roster-backed bot simulation controls

File touched:
`app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`; `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-H-V2-044D; REQ-H-V2-044E.

Why core Lichess file had to be touched:
The protected Dev/admin settings and EvenChess search controller are the existing seams that start simulation/fallback bots and perform game handoff. Roster-backed bot tickets must be routed through the normal human challenge path rather than native AI game setup so the round shell uses human-style usernames and clocks.

Could this be isolated later:
Partly. The roster/time-control settings can move to a dedicated EvenChess operator module after the admin surface is split from the generic Dev UI, but game handoff must remain close to the Lichess challenge/game creation adapter.

Upstream merge risk:
Medium. Controller handoff touches a Lichess setup/challenge seam already carrying EvenChess search integration.

Tests added or updated:
`BotOperationsTest`; `PlaySearchIntegrationTest`.

Notes:
Integration log entry: `INT-2026-133`. Roster-backed bot games require real local bot accounts to exist. Without a roster, generated bot tickets are not allowed to create Stockfish/computer games for matchmaking or simulation.

### PM-2026-129 - Remove non-roster bot AI handoff

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-H-V2-044E; REQ-H-V2-046; REQ-H-V2-046A; OVR-V2-026.

Why core Lichess file had to be touched:
The EvenChess search controller previously converted non-roster bot matches into native Stockfish/computer games. That produced the wrong public round shell: Stockfish label and one-sided clock. The controller now refuses non-roster bot handoff and only roster-backed bot tickets can enter the normal human challenge path.

Could this be isolated later:
Partly. A future dedicated bot-runner service can own move selection/timing, but ECL must still guard the public game-creation seam so generated non-roster tickets cannot produce computer-game UX.

Upstream merge risk:
Medium. This touches the search-to-game handoff seam.

Tests added or updated:
`BotOperationsTest`; `PlaySearchIntegrationTest`.

Notes:
Integration log entry: `INT-2026-134`. Matchmaking bots now fail closed without a real account roster. Configure bot accounts before enabling public matchmaking fallback.

### PM-2026-130 - Deployment wording cleanup for bot operations

File touched:
`app/controllers/Dev.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/DataModelsAndSeams.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`

EvenChess requirement:
REQ-H-V2-044E; OVR-V2-026.

Why core Lichess file had to be touched:
The protected Dev/admin bot-operations surface and SettingStore descriptions are where operators configure matchmaking fallback and simulation bots. Production-facing/admin-facing text must describe roster-backed simulated players instead of fake-player/synthetic-ticket internals.

Could this be isolated later:
Yes. These strings can move into a dedicated EvenChess admin presenter once the operator console is split away from generic Dev UI.

Upstream merge risk:
Low. This is wording/runtime-summary cleanup on existing EvenChess admin sections.

Tests added or updated:
`BotOperationsTest` title cleanup only; existing bot-operation assertions cover the same behavior.

Notes:
Integration log entry: `INT-2026-135`. No behavioral change. This cleanup keeps the fail-closed roster-backed bot model and removes sloppy internal wording from operator surfaces.

### PM-2026-131 - Non-live board overlay availability

File touched:
`app/controllers/EvenChess.scala`; `conf/routes`; `ui/analyse/src/ctrl.ts`; `ui/puzzle/src/ctrl.ts`; `ui/opening/src/opening.ts`; `ui/lib/src/evenchessUniversalOverlay.ts`; `ui/lib/tests/evenchessUniversalOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-C-V2-013; REQ-Q-V2-002; REQ-Q-V2-004; REQ-Q-V2-011; REQ-Q-V2-021; REQ-Q-V2-031.

Why core Lichess file had to be touched:
Analysis/study, puzzle, and opening pages are native Lichess board surfaces. EvenChess overlays need to hydrate on those boards through a same-origin ECL endpoint while preserving Lichess board ownership, move input, puzzle flow, study navigation, and PGN viewer behavior.

Could this be isolated later:
Partly. The board renderer is isolated in `ui/lib/src/evenchessUniversalOverlay.ts`, and the controller route is namespaced under `/evenchess/ece/board-overlay`. The page controllers still need small install hooks unless Lichess adds a generic chessground-extension registry.

Upstream merge risk:
Medium. Future upstream analysis, puzzle, opening, or route/controller refactors may move the board lifecycle hooks or PGN viewer initialization points.

Tests added or updated:
`ui/lib/tests/evenchessUniversalOverlay.test.ts` covers same-origin URL construction without client-supplied levels, approved visual atom conversion, and stale/mismatched payload suppression.

Notes:
Integration log entry: `INT-2026-136`. Non-live board overlays force Set Level 10 server-side; browser code supplies current FEN/side only and never calls ECE directly.

### PM-2026-132 - Lobby start actions remain available with an active game

File touched:
`ui/lobby/src/evenchessSetup.ts`; `ui/lobby/src/view/table.ts`; `ui/lobby/tests/evenchessSetup.test.ts`

EvenChess requirement:
REQ-G-V2 native setup/search controls remain available from the Lichess-style lobby; Preserve normal Lichess setup surfaces unless an EvenChess policy gate explicitly blocks them.

Why core Lichess file had to be touched:
The native lobby start card disabled `Create lobby game`, `Challenge a friend`, and `Play against computer` whenever the user had a real-time game waiting for their move. For EvenChess testing and launch UX, those start actions need to remain pressable; actual game creation/search admission remains server-authoritative and can still reject invalid states.

Could this be isolated later:
Partly. The button-gating rule now lives in the EvenChess lobby helper, but the native table renderer still needs to call it because that renderer owns the visible homepage actions.

Upstream merge risk:
Low to medium. Future upstream lobby table changes may reintroduce the ongoing-game disable rule or move the start buttons.

Tests added or updated:
`ui/lobby/tests/evenchessSetup.test.ts` verifies that an ongoing real-time game no longer disables hook/friend/AI start actions, while bot-account hook blocking remains.

Notes:
Integration log entry: `INT-2026-137`.

### PM-2026-133 - Shared 1000-name bot roster and exclusive bot modes

File touched:
`app/controllers/Dev.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`; `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-F-V2-014B; REQ-H-V2-044E; REQ-H-V2-044F; REQ-H-V2-046; OVR-V2-027.

Why core Lichess file had to be touched:
The protected Dev/admin settings page is the current operator surface for bot matchmaking and simulation controls. Both modes need a shared bulk account roster and mutually exclusive start behavior so fallback and simulation do not compete in the same local queue. The search integration also needs to resolve a blank saved roster to the generated 1000-name local roster.

Could this be isolated later:
Partly. The generated roster and parsing stay in EvenChess-owned code, but the admin form/controller remain Lichess Dev UI seams until a dedicated EvenChess operator console exists.

Upstream merge risk:
Medium. `Dev.scala`, `Env.scala`, and `DevUi.scala` are upstream admin/settings seams. Preserve the shared roster form and one-mode-at-a-time controller behavior after upstream admin UI changes.

Tests added or updated:
`AdminBackendSettingsTest` verifies default shared roster count and simulation count. `BotOperationsTest` verifies generated roster defaults and shared admin-state roster selection. `PlaySearchIntegrationTest` verifies fallback bot mode uses the generated roster when saved roster settings are blank.

Notes:
Integration log entry: `INT-2026-138`. The generated roster creates the account names `ecbot0001` through `ecbot1000`; those names still need corresponding local user records before production/staging bot games can render through the normal human challenge path.

### PM-2026-134 - Move-triggered overlay transition layer

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-L-V2-027; OVR-V2-028.

Why core Lichess file had to be touched:
The native round UI owns board move rendering and the EvenChess overlay mount point. Move-triggered ECE refresh previously cleared the board overlay layer while waiting for the next payload, causing visible blanking between turns.

Could this be isolated later:
Mostly. The transition rule is isolated in the EvenChess overlay renderer. It still needs to live at the round view seam because that is where current ply/FEN, board orientation, and rendered overlay DOM are available together.

Upstream merge risk:
Low to medium. Future upstream round view or chessground mount changes may move the board overlay insertion point or redraw timing.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` now verifies that move-triggered clears retain the previous safe payload as a `move-refresh` transition layer until replacement data arrives.

Notes:
Integration log entry: `INT-2026-139`. Stale or mismatched ECE responses are still rejected as current advice; only the last accepted safe visual layer is retained during the in-flight move refresh.

### PM-2026-135 - Puzzle coach panel and board overlay host

File touched:
`ui/puzzle/src/ctrl.ts`; `ui/puzzle/src/view/main.ts`; `ui/puzzle/css/_layout.scss`; `ui/lib/src/evenchessUniversalOverlay.ts`; `ui/lib/tests/evenchessUniversalOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-Q-V2-002; REQ-Q-V2-004; REQ-Q-V2-005.

Why core Lichess file had to be touched:
The puzzle page owns the native puzzle board, move controls, feedback panel, side grid, and right-side tools column. EvenChess puzzle coaching and the level-selection/toggle controls need to appear inside that existing layout while the board markers attach to chessground without blocking puzzle move input.

Could this be isolated later:
Mostly. The level controls, coach card, feature filtering, and board rendering are isolated in `ui/lib/src/evenchessUniversalOverlay.ts`. The puzzle view still needs a small native host element and the puzzle controller still needs to supply current FEN, ply, side, orientation, and the host element.

Upstream merge risk:
Medium. Future upstream puzzle layout or controller lifecycle changes may move `.puzzle__tools`, `.puzzle__board`, or the current-node/FEN accessors.

Tests added or updated:
`ui/lib/tests/evenchessUniversalOverlay.test.ts` now verifies safe coach-card selection for non-live puzzle panels, rejects stale/raw card payloads, and verifies level-feature toggles filter puzzle board visuals and coach cards.

Notes:
Integration log entry: `INT-2026-140`. Puzzle overlay hydration remains same-origin through ECL; browser code still does not call ECE directly. Puzzle level controls filter already-authorized display payloads only and do not decide server authorization.

### PM-2026-136 - V2 homepage EvenChess summary rail and support-card removal

File touched:
`app/views/lobby/home.scala`; `ui/lobby/css/_layout.scss`; `ui/lobby/css/_lobby.scss`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-D-V2-024.

Why core Lichess file had to be touched:
The Lichess homepage view and lobby grid own the main-page card placement. The EvenChess summary card must sit beside the primary lobby tabs on wide screens, and the launch homepage should not render the old Donate and Swag Store support-card block in the main grid.

Could this be isolated later:
Partly. The EvenChess summary copy remains namespaced through `PublicShell`, but the grid placement and removal of upstream support cards are native homepage seams.

Upstream merge risk:
Medium. Future upstream homepage or lobby grid changes may reintroduce support-card areas or move the tabbed lobby app. Preserve the `evenchess` grid area and absent `lobby__support` block when rebasing.

Tests added or updated:
No automated tests added. This is a layout-only homepage seam covered by CSS build and browser DOM/layout verification.

Notes:
Integration log entry: `INT-2026-141`. The top navigation Donate link is unchanged; only the homepage Donate/Swag content cards are removed.

### PM-2026-137 - V2 live L6 WikiBook fieldset

File touched:
`ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/build/round.scss`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-L-V2-008; REQ-Q-V2-023; L6 row in Appendix N.

Why core Lichess file had to be touched:
The native round UI owns the live side shell, board-derived move history, and responsive board/side layout. The analysis-board WikiBooks opening explainer can be reused in live play only if the round overlay renderer exposes a Level 6 feature toggle, derives the opening path from the current round SAN history, and renders the same Lichess WikiBook fieldset structure in the existing EvenChess left column above the level selector.

Could this be isolated later:
Mostly. The path builder and card renderer are EvenChess-owned inside the round overlay adapter. The layout seam remains native because the round grid controls the left side column height and board alignment.

Upstream merge risk:
Medium. Future upstream round layout changes may move `levels` grid placement or alter round step history shape. Preserve the `openingWiki` feature key, L6 toggle, shared `toggle-box` CSS import, `analyse__wiki`/`wikibook-field` fieldset structure, and board-history-derived Wikibooks path.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` covers live SAN-to-WikiBooks path construction, Lichess WikiBook fieldset rendering, and the feature toggle hiding/showing the fieldset without lowering Used Level.

Notes:
Integration log entry: `INT-2026-142`. This reuses the existing Lichess WikiBook fieldset/HTML behavior and public WikiBooks opening theory data; it does not add browser-direct ECE calls or expose ECE internals.

### PM-2026-138 - V2 fixed-perspective Stockfish eval stabilization

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `modules/evenchess/src/main/LiveBoardIntegration.scala`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-N-V2-023; REQ-N-V2-023A; OVR-V2-029.

Why core Lichess file had to be touched:
The same-origin ECL-to-ECE bridge parses the normalized ECE Stockfish eval payload and the native round overlay renderer owns the eval bar and coach eval strip. ECE now distinguishes fixed-perspective `score.cp_white` / `score.mate_white` from side-output-relative `score.cp`, and proposed-move eval may arrive as `proposed_move_evaluation.eval_after`, so the parser, bridge model, and round UI need a small seam update to keep the displayed eval stable and correct.

Could this be isolated later:
Partly. Move the eval payload normalization into a dedicated EvenChess ECE adapter module when the bridge leaves the controller; the visual state cache remains in the round UI seam unless EvenChess gets a separate round host.

Upstream merge risk:
Low to medium. Controller changes are namespaced under EvenChess, while `ui/round/src/view/evenchessOverlay.ts` and `_evenchess-live.scss` remain native round-view seams with EvenChess-only branches.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies live eval retention across no-eval payloads, structured White-positive eval preference over side-relative label text, mate-score rendering, quick placeholder rejection, and proposed-move preview switching between current-FEN and post-move eval values. `modules/evenchess/src/test/EceLiveBridgeTest.scala` verifies safe structured eval metadata survives the bridge and remains level-gated.

Notes:
Integration log entry: `INT-2026-143`. Quick/no-eval placeholders do not move the eval bar; accepted proposed-move eval toggles independently from the live current-position eval. The bar uses a nonlinear winning-chances-style centipawn mapping, saturates mate scores, and carries safe eval metadata as explicit visual fields instead of relying on label parsing.

### PM-2026-139 - V2 roster-backed bot account provisioning

File touched:
`app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/web/src/main/ui/DevUi.scala`

EvenChess requirement:
REQ-H-V2-044E, REQ-H-V2-044F, REQ-H-V2-044G, REQ-H-V2-044H, REQ-H-V2-044I, REQ-H-V2-044J, REQ-H-V2-044K, REQ-H-V2-046, OVR-V2-026, OVR-V2-027, OVR-V2-030, OVR-V2-031, OVR-V2-032, OVR-V2-033.

Why core Lichess file had to be touched:
The admin/dev controller and settings UI are native Lichess operational surfaces. Matchmaking fallback and simulation now require roster-backed real local accounts so bot games render through the normal human challenge/game path with usernames and both clocks; the settings page needs an admin-only action to create/check those local roster accounts before operators start either bot mode. The EvenChess search controller is also the seam that knows when a matched roster-backed bot game has been created, so it registers an ECL-managed bot runner that submits legal delayed moves through the existing round actor instead of creating native AI/computer games. Simulation mode also pumps valid sim-vs-sim contracts into normal roster-account games from the seeded queue so simulated users can play each other without waiting for a human poll, but human searches get priority before the pump consumes tickets. Simulation seeding now creates same-pool close-rating center-out cohorts so small local batches can match both simulated users and mid-rating human searches.

Could this be isolated later:
Mostly. The account roster policy remains in `modules/evenchess/src/main/BotOperations.scala`, but account creation must use the native user repository/authenticator and the admin UI route until a dedicated EvenChess operator module owns these controls.

Upstream merge risk:
Medium. Future upstream admin/dev settings changes may move `Dev.scala` or `DevUi.scala`; future search/challenge changes may move the EvenChess game-creation seam. Preserve the `provision-bot-accounts` operation, shared-roster operator flow, managed-bot registration after matched game creation, and simulation sim-vs-sim game pump when rebasing.

Tests added or updated:
No new automated test added for account creation because it depends on the local user repository and authenticator. `BotOperationsTest` covers same-pool close-rating simulation cohorts; `PlaySearchIntegrationTest` covers 1-second bot-mode test timeout and the 24-bot mid-rating rated Blitz simulation search regression. Browser/runtime verification covers matchmaking fallback and simulation search redirects into roster-backed human-style games.

Notes:
Integration log entry: `INT-2026-144`. Provisioned accounts are normal local/staging users with private generated passwords and are not assigned the Lichess BOT title, so matched rounds can render as human-style EvenChess games. The managed runner chooses only legal moves from the Lichess/scalachess position and sends them through `RoundBus.BotPlay`.

### PM-2026-140 - V2 create-lobby native pending search indicator

File touched:
`ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/src/view/carousel.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-G-V2-001; REQ-G-V2-002; REQ-G-V2-029A; REQ-G-V2-029B; REQ-G-V2-029C.

Why core Lichess file had to be touched:
The native lobby setup controller owns the Create lobby game submit path, while the quick-pairing grid owns the visible pending/searching animation. EvenChess public search tickets start through `/evenchess/play/search.json`, so create-lobby submits that match an existing quick-pairing card must explicitly mark the matching pool member active. The homepage carousel helper also needed a null guard because the EvenChess homepage intentionally removed the upstream `.lobby__support` cards.

Could this be isolated later:
Partly. The pending-pool eligibility helper is isolated in `ui/lobby/src/evenchessSetup.ts`, but the native setup controller and homepage carousel remain upstream lobby seams.

Upstream merge risk:
Low to medium. Future upstream lobby setup or homepage carousel refactors may alter the setup submit path, pool-member display state, or support-card carousel assumptions.

Tests added or updated:
`ui/lobby/tests/evenchessSetup.test.ts` verifies that a standard rated random real-time create-lobby setup maps to the matching quick-pairing pending card, while fixed-color and casual setups do not.

Notes:
Integration log entry: `INT-2026-145`. The verbose EvenChess search status card remains hidden unless the explicit debug flag is enabled.

### PM-2026-141 - V2 live WikiBook visible empty state

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-L-V2-008; REQ-Q-V2-023.

Why core Lichess file had to be touched:
The live round overlay renderer owns the L6 WikiBook fieldset and its level-card placement. The previous implementation mounted the Lichess-style fieldset, but reused the analysis empty-state visibility rule, so early or missing WikiBook lines made the live feature appear absent even while the L6 toggle was checked. Live games need the existing fieldset to remain visible with an empty-state message until a WikiBook extract loads.

Could this be isolated later:
Mostly. The renderer and CSS remain EvenChess-only branches in the round UI seam, but a future shared WikiBook component could centralize fieldset/empty-state behavior.

Upstream merge risk:
Low to medium. Future upstream round overlay or analysis WikiBook styling changes may require reapplying the live-only empty-state override while preserving the analysis-board behavior.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies the live L6 fieldset includes the visible empty-state copy.

Notes:
Integration log entry: `INT-2026-146`. Analysis-board WikiBook behavior is unchanged; only the live EvenChess fieldset overrides empty visibility.

### PM-2026-142 - V2 live WikiBook CSP allow-list

File touched:
`modules/round/src/main/ui/RoundUi.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-L-V2-008; REQ-Q-V2-023.

Why core Lichess file had to be touched:
The live round page owns its Content Security Policy. The L6 live WikiBook fieldset reuses the existing Lichess analysis-board WikiBooks API loader, but round pages previously allowed peer/WebAssembly connections only. Analysis pages already add `withWikiBooks`; live round pages need the same public `en.wikibooks.org` connection permission or valid opening lines remain stuck in the visible empty state.

Could this be isolated later:
Mostly. This remains a one-line round page CSP extension while the browser loader continues to reuse the shared WikiBooks helper.

Upstream merge risk:
Low. Future upstream round page CSP changes may require reapplying `withWikiBooks` alongside the existing peer and WebAssembly allowances.

Tests added or updated:
No new automated test was added for CSP header generation. Existing browser verification covers that the live fieldset can render; runtime verification checks the round CSP includes `en.wikibooks.org` after rebuild/reload.

Notes:
Integration log entry: `INT-2026-147`. This permits only the same public WikiBooks origin already used by analysis pages and does not add any browser-direct ECE access.

### PM-2026-143 - V2 roster bot established ratings and exact clocks

File touched:
`app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-G-V2-029D; REQ-H-V2-044E; REQ-H-V2-044H; REQ-H-V2-044L; REQ-H-V2-046.

Why core Lichess file had to be touched:
The native dev/admin controller creates local roster accounts, and the native challenge handoff copies normal Lichess perf ratings into the game players before a round exists. To prevent roster-backed bots from displaying as new provisional `1500?` accounts, ECL must seed/repair normal Lichess perfs before provisioning completes and before challenge creation calls `enabledWithPerf`. The lobby setup controller also owns the exact clock values; EvenChess search must carry those values server-side so bot-created games use the same clock the player selected, not only a broad speed bucket.

Could this be isolated later:
Partly. Rating repair could move into a dedicated EvenChess operator/account service, but challenge creation still needs a pre-handoff verification seam because Lichess freezes player display ratings into the created game. Exact clock metadata remains a narrow field on the EvenChess search ticket while MMR still pools by bucket.

Upstream merge risk:
Medium. Future upstream changes to `Dev.scala`, challenge/game creation, or lobby setup submit params may require reapplying the roster perf repair and exact-clock ticket metadata.

Tests added or updated:
`BotOperationsTest` verifies roster display ratings are bounded and non-round. `PlaySearchIntegrationTest` verifies exact clock metadata survives into search tickets and roster-backed bot fallback copies it. `LevelBasedMatchmakingTest` fixture was updated for the ticket field. `evenchessSetup.test.ts` verifies exact clock params can be extracted from quick-pairing pool ids.

Notes:
Integration log entry: `INT-2026-148`. Provisioning still creates ordinary local/staging accounts with private generated passwords and does not grant the Lichess BOT title. Matchmaking and simulation bot games continue to use the normal challenge/game path and managed legal-move runner.

### PM-2026-144 - V2 live WikiBook analysis-style collapse and scroll

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-L-V2-008; REQ-Q-V2-023; OVR-V2-035.

Why core Lichess file had to be touched:
The live round overlay renderer owns the L6 WikiBook fieldset placement above the EvenChess levels card. The previous live initializer used a custom toggle handler without marking the shared `toggle-box` as ready, so the global Lichess toggle-box initializer could also bind the same legend and cancel the close action. The live fieldset also inherited `pointer-events: none` from the overlay host because it is not an `evenchess-live__card`, and the live CSS capped the whole fieldset instead of giving the WikiBook text body a readable internal scroll area.

Could this be isolated later:
Mostly. The live renderer remains an EvenChess-owned branch in the round UI seam, but a future shared WikiBook component could centralize the analysis/live fieldset behavior.

Upstream merge risk:
Low to medium. Future upstream `toggle-box`, analysis WikiBook, or round side-shell layout changes may require reapplying the live ready marker, stored open state, and content-scroll sizing.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` verifies the live WikiBook fieldset is rendered with the initialized `toggle-box--ready` class.

Notes:
Integration log entry: `INT-2026-149`. Analysis-board WikiBook behavior remains unchanged; the live fieldset now follows its collapse semantics while keeping the L6 feature gate and visible empty state.

### PM-2026-145 - V2 managed bot round presence and multi-bot tracking

File touched:
`app/controllers/EvenChess.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-H-V2-044E; REQ-H-V2-044H; REQ-H-V2-044I; REQ-H-V2-044I.1; REQ-H-V2-046.

Why core Lichess file had to be touched:
The EvenChess game-creation controller attaches local/staging roster-backed bot accounts to normal Lichess challenge games. The previous managed runner submitted legal moves through `RoundBus.BotPlay`, but it did not publish Lichess's native `RoundBus.BotConnected` presence event. As a result, a matched bot could play moves while still rendering as an offline normal user, causing "opponent left" prompts in human-vs-bot fallback games. The runtime also keyed active bots by game id only, so sim-vs-sim games could overwrite the first bot entry when the second bot was attached.

Could this be isolated later:
Partly. The managed-runner registry could move into an EvenChess bot runtime module, but the controller currently owns the post-game-creation attach point and must use the native round bus to make bot presence visible to Lichess round state.

Upstream merge risk:
Medium. Future changes to the challenge handoff or round bot-presence bus must preserve `RoundBus.BotConnected(true)` while the managed runner is active and independent per-player tracking for sim-vs-sim games.

Tests added or updated:
Existing bot/search tests were rerun. No direct unit test was added because `EvenChessManagedBotRuntime` is a private controller-local runtime around Lichess round bus side effects.

Notes:
Integration log entry: `INT-2026-150`. This keeps roster-backed bots as normal local accounts for display/rating, while using only Lichess's existing bot-presence and legal-move round bus events for runtime behavior.

### PM-2026-146 - V2 account default feature toggles

File touched:
`modules/evenchess/src/main/UserSettings.scala`; `modules/evenchess/src/test/UserSettingsTest.scala`; `modules/pref/src/main/PrefForm.scala`; `modules/pref/src/main/JsonView.scala`; `modules/pref/src/main/ui/AccountPref.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-F-V2-005; REQ-F-V2-005A; REQ-L-V2-006; REQ-L-V2-007; OVR-V2-013A.

Why core Lichess file had to be touched:
EvenChess settings live inside the native Lichess account preference form and are serialized through the native preference JSON used by round pages. To make per-feature defaults account-scoped and server-persisted, the preference form, JSON view, and account settings UI must carry an EvenChess-owned default feature-toggle profile while keeping live permission and Used Level authority outside the browser.

Could this be isolated later:
Partly. The `UserSettings` schema remains EvenChess-owned, but the account preference form and JSON seam are native Lichess integration points unless EvenChess settings move to a detached page, which current requirements explicitly avoid.

Upstream merge risk:
Medium. Future upstream preference-form or account-settings changes may require reapplying the nested `evenchess.defaultFeatureToggles` mapping, JSON field, and settings UI block.

Tests added or updated:
`UserSettingsTest` covers default feature-toggle validity and tag round-tripping. `evenchessOverlay.test.ts` covers applying account default toggles during new-game initialization and the "Apply up to" dropdown.

Notes:
Integration log entry: `INT-2026-151`. Missing stored feature-toggle values default to on so existing users keep the previous "all eligible features on" behavior until they customize the profile.

### PM-2026-147 - V2 coach-card TTS controls and auto-read preferences

File touched:
`modules/evenchess/src/main/UserSettings.scala`; `modules/evenchess/src/test/UserSettingsTest.scala`; `modules/pref/src/main/PrefForm.scala`; `modules/pref/src/main/FormCompatLayer.scala`; `modules/pref/src/main/JsonView.scala`; `modules/pref/src/main/ui/AccountPref.scala`; `modules/round/src/main/JsonView.scala`; `ui/lib/src/evenchessTts.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-P-V2-020; REQ-P-V2-021; REQ-P-V2-022; REQ-P-V2-023; REQ-P-V2-024.

Why core Lichess file had to be touched:
EvenChess TTS settings live in the native Lichess account preference form and must be available to live round pages. The native preference form/JSON and round JSON seams carry only safe display settings, while the round coach-card renderer owns the visible Speak control and guarded auto-read scheduling for authorized coach text.

Could this be isolated later:
Partly. The TTS safety helper remains in the EvenChess client library and the preference schema remains EvenChess-owned, but the account preference form and round JSON are upstream Lichess integration points unless EvenChess settings and round rendering are moved to a detached surface, which current requirements avoid.

Upstream merge risk:
Medium. Future upstream preference-form, preference JSON, round JSON, or round side-panel changes may require reapplying the EvenChess TTS fields and coach-card button placement.

Tests added or updated:
`modules/evenchess/src/test/UserSettingsTest.scala` verifies auto-read defaults, normalization, client config, and tag round-trip. `ui/round/tests/evenchessOverlay.test.ts` verifies preference-derived TTS config, delay clamping, and visible disabled Speak control.

Notes:
Integration log entry: `INT-2026-152`. The browser still reads only server-authorized displayed coach text, and auto-read does not grant stronger coaching or speak unsafe/mismatched payloads.

### PM-2026-148 - V2 proposed-move toggle restores normal payload cache

File touched:
`ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-K-V2 proposed-move display behavior; REQ-L-V2-014; REQ-L-V2-015.

Why core Lichess file had to be touched:
The native round overlay renderer owns the Proposed Move button and switches between the current-position ECE display payload and the proposed post-move preview payload. The previous renderer assumed the normal payload would always still be present in `data.evenchess.live`; if that live slot was cleared or stale while a proposed preview was active, toggling the preview off could render an empty coach/overlay state instead of restoring the current-FEN payload. The proposed-move state now retains a validated normal board-state overlay snapshot and the renderer uses it when the active preview is hidden.

Could this be isolated later:
Partly. The proposed-move cache model is EvenChess-owned, but the live round renderer remains the native UI seam that swaps the board-adjacent overlay and coach card.

Upstream merge risk:
Low to medium. Future round-overlay changes must preserve the separation between current-FEN board-state payloads and proposed post-move preview payloads.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` now verifies the Proposed Move button can toggle an active cached preview off and restore the cached normal board-state payload text, then toggle the same proposed preview back on without another request.

Notes:
Integration log entry: `INT-2026-153`. Proposed-move preview payloads remain excluded from live match-history storage; this is a client display cache for the current turn only.

### PM-2026-149 - V2 match-contract level bootstrap and human-style bot round handoff

File touched:
`app/controllers/EvenChess.scala`; `conf/routes`; `modules/evenchess/src/main/GamePolicy.scala`; `modules/evenchess/src/test/GamePolicyTest.scala`; `modules/round/src/main/JsonView.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-H-V2-025; REQ-H-V2-025A; REQ-H-V2-046; REQ-H-V2-046A; REQ-H-V2-046B; OVR-V2-036.

Why core Lichess file had to be touched:
Native round JSON is the bootstrap source for the live round client, and native challenge/game creation is the handoff point after the EvenChess MMR contract is finalized. The round JSON seam now exposes the server-persisted per-player EvenChess Set Level and Used Level so the browser does not fall back to L10 or reset raised Used Level to L0 after refresh. Policy lookup now tolerates username/UserId case normalization so stored contracts created from display usernames still resolve during round bootstrap. The authenticated `/evenchess/live/used-level` seam records per-game Used Level as a capped monotonic max when the browser raises the selector or enables a higher-level feature. The game-creation seam now attaches roster-backed managed bots from the accepted game before redirect and returns full player URLs instead of watcher URLs so matched bot games render with normal human-vs-human round data and clocks.

Could this be isolated later:
Partly. The EvenChess policy repository and MMR contract remain EvenChess-owned, but the native round bootstrap and challenge/player redirect are upstream Lichess seams by design.

Upstream merge risk:
Medium. Future changes to round JSON, preference JSON, challenge acceptance, route generation, or live round POST endpoints must preserve persisted Set Level serialization, per-game monotonic Used Level recording, L0 default Used Level initialization, accepted-game bot attachment, and full player redirects for matched humans.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` now covers L0 initialization when no preferred Used Level is set, refresh bootstrap preserving server-persisted Used Level, and payload-derived Used Level capping by Set Level. `modules/evenchess/src/test/GamePolicyTest.scala` covers capped monotonic Used Level persistence. `modules/evenchess/src/test/PlaySearchIntegrationTest.scala` now covers preferred L6 bot fallback preserving the requester's assigned level and exact requested clock metadata.

Notes:
Integration log entry: `INT-2026-154`. This is not a native AI/computer-game path; roster-backed bots remain normal local accounts with a managed legal-move runner.

### PM-2026-150 - V2 matched search redirect survives ticket retirement

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-H-V2-045; REQ-H-V2-047; REQ-H-V2-047A.

Why core Lichess file had to be touched:
The native lobby polls the EvenChess search JSON endpoint through an opaque public search key. After a finalized MMR contract creates the native Lichess game, ECL retires both active tickets to prevent duplicate game creation. The controller must therefore check the cached redirect ledger before trying to resume a retired ticket; otherwise the browser that did not create the game can receive `Search ticket not found` instead of the game redirect.

Could this be isolated later:
Partly. The redirect ledger is EvenChess-owned, but the JSON polling endpoint remains the Lichess lobby/controller seam until EvenChess has a dedicated pairing socket.

Upstream merge risk:
Medium. Future search-controller or lobby-polling changes must preserve the order: public search key redirect lookup first, active ticket resume second, new ticket creation only when no public search key is supplied.

Tests added or updated:
`modules/evenchess/src/test/PlaySearchIntegrationTest.scala` covers public search redirects remaining available after matched tickets leave the active queue.

Notes:
Integration log entry: `INT-2026-155`. This does not change MMR table assignment; it fixes the post-contract delivery path for two-browser/two-device polling.

### PM-2026-151 - Local LAN/localhost round socket host preference

File touched:
`ui/lib/src/socket.ts`; `scripts/evenchess-testground.ps1`

EvenChess requirement:
Local Test Ground deployment usability: PC browser and mobile-on-Wi-Fi must be able to use the same local EvenChess stack without breaking authenticated round sockets.

Why core Lichess file had to be touched:
The native socket client owns round websocket URL selection. The local Test Ground can advertise a LAN IP so phones can connect, but a PC browser opened through `localhost` must use the same-origin `localhost` websocket or its session cookie is not sent to the LAN-IP socket. The socket client now prefers the current page host only for loopback/private local hosts when the configured socket list is also local/private, preserving production socket-domain behavior.

Could this be isolated later:
Partly. The Test Ground launcher is EvenChess-owned, but websocket URL selection is a native Lichess browser seam.

Upstream merge risk:
Low. The behavior is restricted to local/private hosts and should not affect production domains.

Tests added or updated:
No automated socket test was added; verified through the local page socket-domain inspection and TypeScript compile path.

Notes:
Integration log entry: `INT-2026-156`. The launcher now opens the PC URL from its `MainUrl` setting and prints the detected LAN URL separately for mobile testing.

### PM-2026-152 - Pinned-piece marker terminology and visual glyph

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-O-V2-030.

Why core Lichess file had to be touched:
The native round EvenChess overlay renderer owns board-attached pinned-piece indicators. Pinned pieces must be displayed as pins, not locked pieces, and must not use lock or `L` markers. The renderer now emits a top-left `P` marker for pinned pieces and no longer uses the Lichess padlock icon for that feature.

Could this be isolated later:
Partly. The feature is EvenChess-owned, but the board-attached renderer remains the native round UI seam.

Upstream merge risk:
Low. The change is limited to EvenChess overlay rendering and tests.

Tests added or updated:
`ui/round/tests/evenchessOverlay.test.ts` now asserts pinned indicators render as `P` and do not serialize the padlock icon.

Notes:
Integration log entry: `INT-2026-157`.

### PM-2026-153 - Live ECE threat overlay adapter hardening

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-O-V2-020 through REQ-O-V2-026.

Why core Lichess file had to be touched:
The native EvenChess controller is the same-origin server-to-server ECE bridge for live rounds, including computer games on mobile. ECE may provide threat facts as nested groups, direct overlay arrays, or flat board-fact entries with `attacking_side`, source, and target squares. The bridge now normalizes those shapes into the existing student/opponent dotted-arrow visuals. The round adapter also retries a high-level card-only response before accepting it, preventing L4+ live boards from getting stuck with coach text but no board overlay visuals while ECE is still returning a partial payload.

Could this be isolated later:
Partly. The ECE normalization remains EvenChess-owned, but the controller and round adapter are native Lichess seams because they own authenticated round payload transport and client refresh timing.

Upstream merge risk:
Low. The change is limited to EvenChess ECE payload parsing and EvenChess round overlay request retry behavior.

Tests added or updated:
`ui/round/tests/evenchessTestGround.test.ts` now covers low-level card-only acceptance and high-level card-only retry until board visuals arrive.

Notes:
Integration log entry: `INT-2026-158`.

### PM-2026-154 - Per-game live display state survives round refresh

File touched:
`conf/routes`; `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/GamePolicy.scala`; `modules/evenchess/src/test/GamePolicyTest.scala`; `modules/round/src/main/JsonView.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-F-V2-006; REQ-F-V2-006A; REQ-H-V2-025A; REQ-L-V2-006.

Why core Lichess file had to be touched:
The native round JSON bootstrap owns the data that reconstructs a game after browser refresh. Used Level was already persisted server-side, but the in-game display dropdown and individual feature toggles were only in the browser object, so refresh rebuilt them from starting preferences. ECL now persists per-game display state through an authenticated same-origin controller route and serializes it back into round JSON with the server-authorized Set Level and monotonic Used Level.

Could this be isolated later:
Partly. The display state is EvenChess-owned, but round JSON and the round overlay controls are native Lichess seams.

Upstream merge risk:
Medium. Future round JSON or controller route changes must preserve `/evenchess/live/display-state`, `StoredDisplayState`, and `evenchess.display.toggles` hydration.

Tests added or updated:
`GamePolicyTest` covers per-game display-state persistence without lowering Used Level. `evenchessOverlay.test.ts` covers refresh bootstrap preserving stored dropdown/toggle state.

Notes:
Integration log entry: `INT-2026-159`.

### PM-2026-155 - Live coach card embeds potential/proposed controls

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
Proposed-move display requirements; Potential-move display requirements.

Why core Lichess file had to be touched:
The native round side renderer owns the live EvenChess coach card, proposed-move button, potential-move buttons, and right-column card layout. Potential/proposed controls had drifted into separate side cards, which made the right side feel like add-on panels instead of one cohesive coach surface. The round overlay renderer now packages those controls into the bottom of the main coach card, and displays authorized potential/proposed text inside the coach text area after the main card text.

Could this be isolated later:
Mostly. The behavior remains in the EvenChess round overlay adapter and stylesheet, but the adapter is still mounted inside the native round layout seam.

Upstream merge risk:
Low to medium. Future round side-panel or stylesheet changes must preserve the single coach-card composition and avoid reintroducing standalone potential/proposed cards in normal live play.

Tests added or updated:
`evenchessOverlay.test.ts` asserts the potential/proposed controls are packaged inside the coach card and no standalone proposed-control card is rendered in normal live play.

Notes:
Integration log entry: `INT-2026-160`.

### PM-2026-156 - Live coach card Auto Speak toggle

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-P-V2-023; REQ-P-V2-024.

Why core Lichess file had to be touched:
The native round overlay renderer owns the live EvenChess coach card header and the manual Speak button. The Auto Speak control needs to sit beside that button and toggle the same round TTS auto-read state used by the scheduler when new coach text arrives.

Could this be isolated later:
Mostly. The control and scheduler are EvenChess-owned inside the round overlay adapter, but the card remains mounted in the native round side panel.

Upstream merge risk:
Low. Future round overlay or TTS control changes must preserve the adjacent Speak/Auto controls and the live `autoSpeak` override.

Tests added or updated:
`evenchessOverlay.test.ts` asserts the Auto control renders and that toggling it updates the existing round TTS auto-read config.

Notes:
Integration log entry: `INT-2026-161`.

### PM-2026-157 - Canonical user ids for public search ticket ownership

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-H-V2-045; REQ-H-V2-047; REQ-H-V2-047A; REQ-H-V2-047B.

Why core Lichess file had to be touched:
The native EvenChess search controller owns authenticated public search tickets and the handoff from an MMR contract into Lichess challenge/game creation. The controller previously used display usernames for search ticket ownership while the native challenge acceptance path uses canonical `UserId` values. Accounts whose display username casing differs from the canonical id can match at the MMR layer but fail native game handoff, leaving both browser sessions polling. Search ticket ownership and search-key redirect lookup now use canonical user ids.

Could this be isolated later:
Partly. The MMR records remain EvenChess-owned, but the authenticated controller and native challenge handoff are Lichess seams.

Upstream merge risk:
Medium. Future changes to authenticated search, public search-key polling, or challenge handoff must preserve canonical user-id ownership and avoid mixing display usernames into authoritative ticket ids.

Tests added or updated:
`PlaySearchIntegrationTest` now covers the logged phone/PC same-format casual rapid case with different starting set-level defaults, proving the service-level MMR contract is produced.

Notes:
Integration log entry: `INT-2026-162`.

### PM-2026-158 - Potential-move reveal turn gating

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
Potential-move display requirements.

Why core Lichess file had to be touched:
The native EvenChess controller owns server-authorized potential-move reveal requests, and the native round overlay renderer owns the live coach-card buttons. Opponent potential moves are only valid on the opponent's turn, and student potential moves are only valid on the student's turn. The controller now rejects wrong-turn reveal requests, while the round overlay disables the matching button and shows the reason before a request is sent.

Could this be isolated later:
Partly. The reveal policy is EvenChess-owned, but the controller bridge and round coach-card buttons remain native Lichess seams.

Upstream merge risk:
Low to medium. Future controller or round overlay rewrites must preserve the side-to-move gate for both potential-move reveal kinds.

Tests added or updated:
`evenchessOverlay.test.ts` now covers wrong-turn client blocking and preserves the opponent-side reveal request when it is the opponent's turn.

Notes:
Integration log entry: `INT-2026-163`.

### PM-2026-159 - Public search game-handoff diagnostics

File touched:
`app/controllers/EvenChess.scala`

EvenChess requirement:
V2 matchmaking/MMR public search reliability.

Why core Lichess file had to be touched:
The EvenChess public search controller owns the transition from a finalized EvenChess MMR contract into native Lichess challenge/game creation. A live phone/PC search produced a valid MMR contract but no round redirect, and the handoff path swallowed native challenge failures as `None`. The controller now logs a throttled, safe reason when challenge creation or immediate acceptance fails.

Could this be isolated later:
Partly. The diagnostic is EvenChess-owned, but the failure occurs at the native challenge/game creation seam.

Upstream merge risk:
Low. Future controller rewrites should preserve handoff diagnostics so matched-but-not-redirected searches are actionable.

Tests added or updated:
No automated test added; this is runtime diagnostic coverage for a native challenge handoff path that needs live lila services.

Notes:
Integration log entry: `INT-2026-164`.

### PM-2026-160 - ECE 06 Jun 2026 call-contract alignment

File touched:
`conf/routes`; `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `ui/round/src/evenchessTestGround.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
Appendix J ECE Engine Requirements; Appendix J.12 proposed-move display requirements; Appendix J.17 acceptance criteria; OVR-V2-037 through OVR-V2-039.

Why core Lichess file had to be touched:
The native EvenChess controller is the same-origin server-to-server ECE bridge. ECE's 06 Jun 2026 contract made proposed-move SAN canonical, added async Advanced ECS job IDs for deep addenda, and added `/v1/ece/performance-summary`. The controller now converts browser arrow UCI to SAN from the authoritative current FEN before calling ECE, sends SAN plus legacy UCI, accepts SAN or UCI echoes after FEN validation, handles `advanced_ecs.job_id` / `quick_context.advanced_job_id`, and exposes a same-origin performance-summary bridge without allowing browser-to-ECE direct calls.

Could this be isolated later:
Yes. Move the HTTP envelope construction, SAN conversion, deep job polling, and summary routes into a dedicated EvenChess ECE adapter service once the current controller-hosted Test Ground bridge is extracted.

Upstream merge risk:
Medium. Future controller/route/gateway changes must preserve server-side-only ECE calls, SAN derivation from FEN+arrow, async Advanced ECS non-fatal degradation, and the performance-summary route.

Tests added or updated:
`EngineGatewayTest` verifies the performance-summary endpoint, async deep request handle, and canonical SAN/legacy UCI proposed-move model. `scripts/evenchess-test-ece-server.test.mjs` verifies Test ECE quick/deep advanced job IDs, proposed SAN echo, `/full-match`, `/full-match-summary`, and `/performance-summary`.

Notes:
Integration log entry: `INT-2026-165`.

### PM-2026-161 - Threat-line adapter and overlay stacking hardening

File touched:
`app/controllers/EvenChess.scala`; `ui/round/css/_evenchess-live.scss`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
Appendix O threat overlays; Appendix Z override `OVR-V2-040`.

Why core Lichess file had to be touched:
The EvenChess controller is the same-origin ECE payload adapter, and the native round stylesheet owns the board-attached overlay layer. Threat board facts can arrive from ECE as simple arrays, alias arrays, flat side-tagged facts, or nested move/attack/arrow objects. The adapter now accepts those equivalent shapes, and the board overlay uses the existing Lichess board-overlay z-index tier so dotted threat arrows render above Chessground SVG layers on desktop and mobile while staying pointer-events disabled.

Could this be isolated later:
Partly. Payload normalization can move to a dedicated EvenChess ECE adapter module, but the native round board still needs a local overlay stacking seam unless Lichess exposes a board-overlay extension point.

Upstream merge risk:
Medium. Future controller adapter or round CSS changes must preserve threat alias/nested move extraction and keep EvenChess board visuals above Chessground custom SVGs without blocking move input.

Tests added or updated:
No new test file was added; existing `evenchessOverlay.test.ts` covers student/opponent dotted arrows and feature toggles. `./lila.sh "compile"` verifies the controller adapter change.

Notes:
Integration log entry: `INT-2026-166`.

### PM-2026-162 - Per-game level and toggle refresh persistence

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-F-V2-006A; REQ-L-V2-006 through REQ-L-V2-007.

Why core Lichess file had to be touched:
The native EvenChess controller owns same-origin writes for live Used Level and display state, while the native round overlay owns the in-game level selector and feature toggles. The save endpoints now use canonical Lichess user ids so writes line up with round bootstrap hydration, the client adopts the full server display-state response after saving, and the "Apply up to" dropdown renders from the persisted applied level instead of recalculating itself from whichever feature toggles remain enabled.

Could this be isolated later:
Partly. The display-state storage can move behind a dedicated EvenChess service, but the round overlay remains the local UI seam for level/toggle controls.

Upstream merge risk:
Medium. Future round overlay or controller changes must preserve per-game display state, canonical user-id writes, monotonic Used Level, and the distinction between applied preset level and highest currently enabled feature level.

Tests added or updated:
`evenchessOverlay.test.ts` now verifies a refreshed round keeps server-persisted Used Level and renders the level dropdown from persisted applied level even when a higher-level feature toggle is independently enabled.

Notes:
Integration log entry: `INT-2026-167`.

### PM-2026-163 - Live opponent Set/Used Level display

File touched:
`app/controllers/EvenChess.scala`; `modules/round/src/main/JsonView.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-F-V2-006B.

Why core Lichess file had to be touched:
The native round JSON bootstrap and same-origin ECE bridge are the sources of server-authorized live policy data, and the native round overlay owns the visible level controls. The round bootstrap and live overlay response now include the opponent's Set Level and current Used Level from the stored EvenChess game policy/display history, and the level card renders those values beside the local player values.

Could this be isolated later:
Partly. Opponent level lookup can move behind a dedicated EvenChess round-policy view service, but the native round JSON and level-control renderer still need to carry and display the server-owned values.

Upstream merge risk:
Low. Future round JSON or overlay header changes must preserve `evenchess.display.opponent` and avoid deriving opponent policy from browser state.

Tests added or updated:
`evenchessOverlay.test.ts` verifies that the round level card displays opponent Set and Used Levels when policy data is present and that live overlay refreshes update the opponent snapshot.

Notes:
Integration log entry: `INT-2026-168`.

### PM-2026-164 - Disabled level dropdown options are visibly greyed

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-F-V2-006C.

Why core Lichess file had to be touched:
The native round overlay renderer owns the live EvenChess "Apply up to" dropdown. Options above the server-authorized Set Level were disabled but did not carry explicit disabled styling or accessible metadata, making them look selectable in the dark menu. The renderer now marks unavailable options with a disabled class/title/ARIA state, and the round stylesheet greys those disabled options.

Could this be isolated later:
Mostly. This remains inside the EvenChess round overlay adapter and stylesheet, but it is mounted in the native round UI seam.

Upstream merge risk:
Low. Future round overlay dropdown changes must preserve disabled styling and metadata for levels above Set Level.

Tests added or updated:
`evenchessOverlay.test.ts` verifies that a dropdown option above Set Level is disabled, classed, titled, and aria-disabled while the highest allowed option remains enabled.

Notes:
Integration log entry: `INT-2026-169`.

### PM-2026-165 - Mobile-safe TTS and text-change-only auto-read

File touched:
`ui/lib/src/evenchessTts.ts`; `ui/lib/tests/evenchessTts.test.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-P-V2-023; REQ-P-V2-024; OVR-V2-041.

Why core Lichess file had to be touched:
The shared browser TTS helper owns speech synthesis policy, and the native round overlay owns the live Speak and Auto controls. The old TTS safety check treated opponent turns as a blocking reason, which disabled controls even when the coach text was safe and authorized. Auto-read also keyed repeats by audit/card identity, so payload churn with unchanged text could be spoken again. The browser speech driver now uses `window.SpeechSynthesisUtterance`, warms voices, and resumes speech synthesis before speaking for better mobile-browser compatibility; the round auto-read scheduler keys repeats by normalized spoken text.

Could this be isolated later:
Mostly. TTS remains EvenChess-owned, but the Speak/Auto controls live inside the native round overlay seam.

Upstream merge risk:
Low. Future TTS helper or round coach-card changes must preserve opponent-turn control availability, text-change-only auto-read, and the mobile-safe browser speech driver path.

Tests added or updated:
`evenchessTts.test.ts` verifies opponent-turn TTS is offered and the browser driver uses `window` speech synthesis with `resume()`. `evenchessOverlay.test.ts` verifies unchanged text is not auto-read twice.

Notes:
Integration log entry: `INT-2026-170`.

### PM-2026-166 - Executed premoves skip live ECE refresh

File touched:
`ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessTestGround.test.ts`

EvenChess requirement:
REQ-L-V2-028; OVR-V2-042.

Why core Lichess file had to be touched:
Native round move handling owns Chessground premove execution and the server move acknowledgement path. The EvenChess live ECE bridge is triggered from the same round controller after position changes, so the controller now defers the post-opponent-move ECE call through the premove window and marks executed premove positions as skipped. The ECE request helper records skipped keys so stale in-flight responses cannot requeue the skipped current position.

Could this be isolated later:
Partly. The skip bookkeeping remains EvenChess-owned, but detecting whether Chessground actually executed a stored premove must stay close to the native round controller unless upstream exposes a dedicated premove-commit event.

Upstream merge risk:
Medium. Future changes to `apiMove`, `actualSendMove`, Chessground premove handling, or the EvenChess overlay request helper must preserve the distinction between executed premoves and cancelled/failed premoves.

Tests added or updated:
`evenchessTestGround.test.ts` verifies executed-premove skip contexts do not fetch ECE and that stale in-flight responses do not requeue a skipped premove position.

Notes:
Integration log entry: `INT-2026-171`.

### PM-2026-167 - Position ECS Ask AI bridge and own-move token accrual

File touched:
`modules/evenchess/src/main/EngineGateway.scala`; `app/controllers/EvenChess.scala`; `conf/routes`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`

EvenChess requirement:
REQ-P-V2-012; REQ-P-V2-016; REQ-P-V2-017; REQ-P-V2-018; REQ-P-V2-019; OVR-V2-043.

Why core Lichess file had to be touched:
The native EvenChess round overlay owns the live coach-card actions, while ECE must remain server-to-server. The ECL controller now exposes a same-origin Position ECS bridge that enforces the Used-Level own-move allowance, caches same-position Ask AI results, and calls ECE `/v1/ece/position` without browser-direct ECE access. The round overlay renders the Ask AI button, displays consumed/accrued use counts, and toggles cached Position ECS text without spending another call. The gateway also stops treating `ai_text` as an Advanced ECS board-state module.

Could this be isolated later:
Partly. The Position ECS bridge remains EvenChess-owned, but the button and displayed Position ECS result must stay in the native round coach-card seam unless upstream exposes a separate extension slot for live board assistance actions.

Upstream merge risk:
Medium. Future ECE route/config changes, round overlay coach-card changes, or assistance-usage JSON changes must preserve server-side Position ECS gating and avoid browser-direct calls.

Tests added or updated:
`EngineGatewayTest.scala` covers the Position ECS request shape and verifies `ai_text` is no longer a board-state deep module. `evenchessOverlay.test.ts` covers Position ECS accrual and button/count rendering. `evenchessTestGround.test.ts` covers same-origin Position ECS URL construction. `evenchess-test-ece-server.test.mjs` covers the test ECE Position ECS response.

Notes:
Integration log entry: `INT-2026-172`.

### PM-2026-168 - Potential ECS dedicated reveal bridge

File touched:
`modules/evenchess/src/main/EngineGateway.scala`; `app/controllers/EvenChess.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `modules/evenchess/src/test/EngineGatewayTest.scala`

EvenChess requirement:
REQ-P-V2-003; REQ-P-V2-003A; REQ-P-V2-003B; REQ-P-V2-003C; OVR-V2-044.

Why core Lichess file had to be touched:
ECE Potential Moves moved out of Advanced ECS into a dedicated Potential ECS endpoint. The native EvenChess controller remains the same-origin browser bridge and now calls ECE `/v1/ece/potential` server-to-server only after ECL authorizes a reveal for the current game, FEN, player side, Used Level, and turn. The gateway now exposes the Potential ECS endpoint/request shape, and the local Test ECE fixture mirrors the new dedicated response.

Could this be isolated later:
Mostly. The Potential ECS request model and test fixture are EvenChess-owned, but the controller endpoint must stay in the native EvenChess HTTP seam so the browser never calls ECE directly.

Upstream merge risk:
Low to medium. Future ECE endpoint/config, assistance quota, or round coach-card changes must preserve the server-side reveal gate and must not reintroduce Potential Moves from board-state/Advanced ECS payloads.

Tests added or updated:
`EngineGatewayTest.scala` covers the Potential ECS endpoint/config and request shape. `evenchess-test-ece-server.test.mjs` covers the dedicated Test ECE Potential ECS route and verifies Advanced ECS no longer emits candidate moves.

Notes:
Integration log entry: `INT-2026-173`.

### PM-2026-169 - Standard ECS replaces live quick/deep board-state bridge

File touched:
`modules/evenchess/src/main/EngineGateway.scala`; `app/controllers/EvenChess.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`.

EvenChess requirement:
REQ-J-V2-005 through REQ-J-V2-010; REQ-N-V2-023A; REQ-P-V2-012; REQ-P-V2-012A; OVR-V2-045.

Why core Lichess file had to be touched:
The native EvenChess controller owns the server-to-server ECE bridge used by live rounds, review backfill, and local Test Ground. ECE merged the old Initial/Advanced quick/deep board-state contract into Standard ECS, so the controller now posts live board-state requests to `/v1/ece/standard`, preserves the returned `position_ecs_id`, and stops calling or merging deep board addenda. The round overlay treats Position ECS as the only live eval/AI replacement source and toggles that cached result over the Standard ECS coach payload.

Could this be isolated later:
Mostly. The request models, parser, and test ECE fixture are EvenChess-owned. The overlay integration must remain in the native round coach-card seam unless upstream exposes a formal extension slot for live board assistance.

Upstream merge risk:
Medium. Future ECE route/config changes, round coach-card changes, or eval parsing changes must preserve the Standard ECS per-move call, Position ECS Ask AI/eval separation, and the rule that Standard ECS does not provide potential moves or provider eval.

Tests added or updated:
Gateway, live coaching, Test Ground, overlay, and local Test ECE tests were updated to cover Standard ECS request/response shape, `position_ecs_id` propagation, Position ECS eval visuals, and removal of deep-module board-state assumptions.

Notes:
Integration log entry: `INT-2026-174`.

### PM-2026-170 - Potential ECS current-position eval display

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-N-V2-023; REQ-N-V2-023A; REQ-P-V2-003C; OVR-V2-046.

Why core Lichess file had to be touched:
The native EvenChess controller owns the same-origin server-to-server ECE bridge, while the native round overlay owns the eval bar and coach eval strip. ECE Potential ECS now returns `potential_ecs.evaluation` as the current-position Stockfish eval alongside candidate moves, so ECL must sanitize that field into an approved eval visual and the round renderer must treat it as an active reveal-scoped eval source. Standard ECS remains excluded from eval display.

Could this be isolated later:
Mostly. The Potential ECS parsing remains EvenChess-owned, but the eval bar integration must stay in the native round overlay seam unless upstream exposes a formal live-assistance sidebar extension point.

Upstream merge risk:
Low to medium. Future Potential ECS payload changes, round overlay changes, or eval rendering changes must preserve `potential_ecs.evaluation` as the overall eval source and must not use `potential_ecs.moves[*].score` for the eval bar.

Tests added or updated:
`evenchess-test-ece-server.test.mjs` verifies the Test ECE Potential ECS response includes current-position eval separate from candidate scores. `evenchessOverlay.test.ts` verifies Potential ECS eval renders only while the same-position reveal is active and clears after a move.

Notes:
Integration log entry: `INT-2026-175`.

### PM-2026-171 - Top-bar EvenChess Help guide

File touched:
`modules/web/src/main/ui/layout.scala`; `ui/lib/css/header/_buttons.scss`; `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`

EvenChess requirement:
REQ-D-V2-030; REQ-D-V2-034; REQ-E-V2-013.

Why core Lichess file had to be touched:
The shared Lichess site header owns the public top-bar buttons and dropdown shell. EvenChess needs a deployment-safe, always-available help guide in that native top-bar seam so players can quickly understand assisted play, levels, matchmaking, ECR fairness, and platform-only help without opening a detached page or debug panel.

Could this be isolated later:
Mostly. The copy remains EvenChess-owned in `PublicShell`, but the rendered button/dropdown must remain in the shared header unless upstream exposes a formal top-bar extension slot.

Upstream merge risk:
Low to medium. Future header/dropdown or top-bar CSS changes must preserve the `EvenChess Help` button, scrollable popup, and concise public safety copy.

Tests added or updated:
`PublicShellTest.scala` verifies the guide content covers levels, matchmaking, play rules, tokens/plans fairness, and avoids forbidden public-positioning phrases.

Notes:
Integration log entry: `INT-2026-176`.

### PM-2026-172 - Live display toggle overlay stability

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-L-V2-006; REQ-L-V2-006C.

Why core Lichess file had to be touched:
The native round overlay renderer owns the live level-card toggles and the approved board/coach payload display. Toggle changes must filter the current approved ECE payload in place while any higher-level Standard ECS refresh happens in the background, and display-state persistence acknowledgements must not replace newer local feature selections.

Could this be isolated later:
Mostly. The logic is EvenChess-owned, but it must remain in the round overlay seam unless upstream exposes a formal live-assistance control extension point.

Upstream merge risk:
Low to medium. Future round overlay or control persistence changes must preserve conservative display-state acknowledgement merging and avoid clearing safe overlays on feature-toggle changes.

Tests added or updated:
`evenchessOverlay.test.ts` covers coach-only/higher-level toggles preserving current board overlay items and stale/sparse persistence acknowledgements not erasing local feature toggles.

Notes:
Integration log entry: `INT-2026-177`.

### PM-2026-173 - Ask AI coach-card action visibility

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-P-V2-017; REQ-P-V2-017A.

Why core Lichess file had to be touched:
The native round overlay renderer owns the live EvenChess coach-card action stack. Position ECS is triggered by the player through the `Ask AI` button, so the action must be discoverable in that stack even before a use is earned. The renderer now places `Ask AI` before Potential Moves and Proposed Move, with disabled/status text for not-yet-earned uses.

Could this be isolated later:
Mostly. The behavior is EvenChess-owned, but it remains in the native round coach-card seam unless upstream exposes a formal live-assistance action extension point.

Upstream merge risk:
Low. Future coach-card layout changes must preserve `Ask AI` as the first action and keep unavailable status text visible.

Tests added or updated:
`evenchessOverlay.test.ts` covers `Ask AI` being packaged inside the coach card, ordered before Potential Moves/Proposed Move, and visible with `Available in 7 moves` before a Level 6 token is earned after one own move.

Notes:
Integration log entry: `INT-2026-178`.

### PM-2026-174 - Proposed Move post-move Standard ECS preview payload

File touched:
`app/controllers/EvenChess.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`

EvenChess requirement:
Proposed-move display requirements; OVR-V2-047.

Why core Lichess file had to be touched:
The native EvenChess controller owns the server-to-server ECE proposed-move bridge and must convert ECE output into the public round overlay model. Legal proposed moves now prefer `proposed_move_evaluation.after_move_standard_ecs`, a Standard ECS-style post-move payload, so the preview displays the same board/coach payload the player would receive if the move had occurred. The existing round renderer already toggles between cached current-FEN and proposed-FEN payloads; this patch keeps that behavior while changing the preferred payload source.

Could this be isolated later:
Partly. The ECE parser and Test ECE fixture are EvenChess-owned, but the authenticated controller and native round proposed-preview renderer remain Lichess seams.

Upstream merge risk:
Medium. Future ECE proposed-move contract changes must preserve Standard ECS-style `side_outputs` parsing, current-FEN validation, quota/caching, and the legacy `after_move_side_output` fallback until migration completes.

Tests added or updated:
`evenchess-test-ece-server.test.mjs` verifies Test ECE returns `after_move_standard_ecs` with both side outputs. Existing `evenchessOverlay.test.ts` verifies proposed preview toggles to the hypothetical payload and back to the cached current board-state payload.

Notes:
Integration log entry: `INT-2026-179`.

### PM-2026-174 - Test Ground deployment-shaped ECE launcher settings

Files:

`scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

Requirements:

REQ-J-V2 ECE separate private backend, server-to-server boundary, local ECE health/startup tooling, and Standard ECS local smoke; ECE combined requirements Phase J deployment topology and provider pool launch settings.

Reason:

The ECE service was restructured into deployment-shaped lanes with private provider pools for Redis/cache, Stockfish, CGC Ollama, and General Ollama. The local Test Ground launcher previously started only the legacy single Node ECE process, which could leave the real Test Ground pointed at an ECE without its expected provider topology. The launcher now defaults Real ECE to the ECE compose startup path and exposes local-only launch settings for ECE API replicas, Stockfish workers, CGC replicas, General Ollama replicas, GPU/model-pull flags, and model names.

Upstream/core impact:

No upstream Lichess core files are changed. This is local development tooling only. Browser code still never calls ECE directly; the Test Ground panel saves only launch topology settings and does not expose provider endpoints, API keys, prompts, or private ECE internals.

Migration note:

Legacy single-process ECE launch remains available as an explicit `process` mode for narrow debugging, but the default Test Ground Real ECE mode is `compose` so local testing matches the scalable ECE deployment architecture.

Tests:

No automated Test Ground launcher test exists for this PowerShell/browser-panel path. Manual validation should use the panel's Start Full Test Ground action, then check `/health`, `/ready`, `/ops/capacity-plan`, and the Standard ECS sample call.

### PM-2026-175 - Live coach TTS visible-text and auto-delta alignment

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-P-V2-023; REQ-P-V2-024; REQ-P-V2-024A; OVR-V2-041.

Why core Lichess file had to be touched:
The native round overlay renderer owns the live EvenChess coach card, including manual Speak and Auto Speak controls. Proposed Move, Potential Moves, and Ask AI text now all render inside the same coach text area, so the renderer must build TTS from the allowed spoken subset rather than only the base summary card. Auto Speak also needs round-local state to read only newly appended Proposed Move text or replacement Ask AI text.

Could this be isolated later:
Mostly. The behavior is EvenChess-owned, but it remains in the native round coach-card seam unless upstream exposes a formal assistant/TTS extension point for round side panels.

Upstream merge risk:
Low. Future round coach-card or TTS changes must preserve allowed coach/proposed/Ask AI speech, visual-only Potential Moves text, appended-result auto deltas, and replacement-summary auto speech.

Tests added or updated:
`evenchessOverlay.test.ts` covers manual TTS including inline proposed text and auto-delta selection for appended proposed text, Ask AI replacement text, and proposed-result toggle removal.

Notes:
Integration log entry: `INT-2026-180`.

### PM-2026-176 - ECE-owned coach text length

File touched:
`app/controllers/EvenChess.scala`; `modules/evenchess/src/main/CoachingOverlays.scala`; `modules/evenchess/src/main/LiveOverlayUi.scala`; `modules/evenchess/src/test/CoachingOverlaysTest.scala`; `modules/evenchess/src/test/LiveOverlayUiTest.scala`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`

EvenChess requirement:
REQ-L-V2-029; OVR-V2-048.

Why core Lichess file had to be touched:
The native EvenChess controller owns the server-to-server ECE parsing seam and previously truncated Summary/Plan/Warning/Ask AI/Proposed Move coach text before publishing round payloads. The EvenChess Display Engine also treated compact text budgets as renderability gates. ECE now owns coach text length, so ECL keeps sanitization and forbidden-field checks but no longer truncates or rejects otherwise safe coach text for local character/line budgets.

Could this be isolated later:
Partly. The controller parser remains the ECE-to-round bridge. The budget metadata can remain in EvenChess modules as advisory UI information, but renderability must not depend on it unless ECE delegates length ownership back to ECL.

Upstream merge risk:
Low to medium. Future ECE parser or Display Engine changes must preserve unbounded safe coach text while keeping bounded ids, labels, audit ids, error messages, and fixed-size option lists.

Tests added or updated:
`EceLiveBridgeTest.scala` verifies long ECE summary text remains renderable and unmodified. `CoachingOverlaysTest.scala` and `LiveOverlayUiTest.scala` verify budgets are advisory and no longer reject renderable coach cards.

Notes:
Integration log entry: `INT-2026-181`.

### PM-2026-177 - Failed ECE action calls do not consume assistance uses

File touched:
`app/controllers/EvenChess.scala`

EvenChess requirement:
J.12 proposed/potential usage rules; REQ-P-V2-019A; OVR-V2-049.

Why core Lichess file had to be touched:
The native EvenChess controller owns the same-origin bridge for Proposed Move, Potential ECS, and Position ECS / Ask AI. These routes also own server-side per-game quota accounting, so the no-charge-on-failure rule must be enforced before the controller caches an ECE action payload or increments consumed counts.

Could this be isolated later:
Partly. The pure accounting guard could move into an EvenChess service module, but the controller route still needs to map failed ECE responses to deployment-safe same-origin HTTP errors with current usage counts.

Upstream merge risk:
Low. Future ECE action route changes must preserve the rule that only server-authorized, approved display payloads are cached and counted.

Tests added or updated:
No new standalone test was added for this controller-private guard. Existing focused Scala compile validates the changed route/helper shape; follow-up controller-route tests should cover rejected `ok:false` Potential ECS and Position ECS payloads once a route harness exists.

Notes:
Integration log entry: `INT-2026-182`.

### PM-2026-178 - Admin unlimited action-token debug bypass

File touched:
`app/controllers/EvenChess.scala`; `app/controllers/Dev.scala`; `modules/web/src/main/Env.scala`; `modules/evenchess/src/main/AdminBackendSettings.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`

EvenChess requirement:
REQ-F-V2-019; REQ-P-V2-019B; OVR-V2-050.

Why core Lichess file had to be touched:
The native EvenChess controller owns same-origin action quota enforcement for Proposed Move, Potential ECS, and Position ECS / Ask AI, while the native `/dev/settings` store exposes admin/debug settings and the round overlay renders action counters. The admin debug bypass must be server-authoritative, but the browser also needs server quota markers so admin buttons show `Unlimited` instead of stale local quota calculations.

Could this be isolated later:
Partly. The pure quota policy can move into an EvenChess service module, but the controller routes still need to read the authenticated admin context and return browser-safe usage JSON.

Upstream merge risk:
Low. Future action-route or round-assistance UI changes must preserve the admin-only server setting and avoid granting unlimited action use to non-admin users.

Tests added or updated:
No new route harness was added. Focused Scala compile and TypeScript test/build checks should validate the settings/JSON/UI shape.

Notes:
Integration log entry: `INT-2026-183`.

### PM-2026-179 - Ask AI own-turn enforcement

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`

EvenChess requirement:
REQ-P-V2-019C; OVR-V2-051.

Why core Lichess file had to be touched:
The native EvenChess controller owns the same-origin Position ECS / Ask AI route authorization, while the native round overlay renders and dispatches the Ask AI action. Own-turn enforcement has to exist at both the user-facing control and backend route before cached replay or ECE transport.

Could this be isolated later:
Partly. The pure policy could move into an EvenChess assistance service, but the controller still needs to reject direct same-origin off-turn calls and the round action still needs to show disabled/off-turn status.

Upstream merge risk:
Low. Future Position ECS route or coach-card action changes must preserve own-turn gating and avoid admin-unlimited bypassing side-to-move checks.

Tests added or updated:
`evenchessOverlay.test.ts` covers off-turn Ask AI rendering and the client request guard.

Notes:
Integration log entry: `INT-2026-184`.

### PM-2026-180 - Coach action button copy and sizing

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`

EvenChess requirement:
REQ-P-V2-017B.

Why core Lichess file had to be touched:
The native round overlay owns the EvenChess coach-card action stack. The Potential ECS and Proposed Move action labels and shared action sizing are rendered in that seam, so the UI copy and layout rule have to be applied there.

Could this be isolated later:
Mostly. This is EvenChess-specific UI inside the round overlay, but it remains in the native round bundle unless the EvenChess coach card is moved into a standalone extension package.

Upstream merge risk:
Low. Future coach-card layout changes must preserve the deployment-facing action labels, remove the duplicate Potential Moves heading, and keep the four action buttons visually aligned.

Tests added or updated:
`evenchessOverlay.test.ts` covers the updated action labels and removed Potential Moves heading.

Notes:
Integration log entry: `INT-2026-185`.

### PM-2026-181 - Coach Draw mode for proposed-move input

File touched:
`ui/round/src/ctrl.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`

EvenChess requirement:
REQ-P-V2-017C; OVR-V2-052.

Why core Lichess file had to be touched:
The native round controller owns Chessground input routing and the native round overlay owns the EvenChess coach-card header. Mobile proposed-move input needs to convert touch/left-pointer board gestures into the same green Chessground shapes that desktop right-click drawing already produces, while temporarily disabling normal move/premove input and board touch scrolling.

Could this be isolated later:
Partly. The draw-mode gesture bridge is tied to Chessground controller state, but it remains EvenChess-specific and can move if the EvenChess coach layer becomes a standalone round plugin.

Upstream merge risk:
Low to medium. Future Chessground API or round input changes must preserve the Draw mode's `setShapes`, `getKeyAtDomPos`, movement-disable, and touch-scroll blocking behavior.

Tests added or updated:
`evenchessOverlay.test.ts` covers the Draw toggle render and active state.

Notes:
Integration log entry: `INT-2026-186`.

### PM-2026-182 - Mobile-safe EvenChess browser TTS driver

File touched:
`ui/lib/src/evenchessTts.ts`

EvenChess requirement:
REQ-P-V2-020 through REQ-P-V2-024B; OVR-V2-041.

Why core Lichess file had to be touched:
The shared UI library owns the EvenChess browser speech driver used by live round coach-card Speak and Auto Speak controls. Mobile browsers can drop speech when a user gesture is interrupted by an unnecessary `speechSynthesis.cancel()` or when speech starts before the mobile synthesis engine has resumed. The driver now preserves the user's tap activation, resumes synthesis, sets a language fallback, and retries once if the first utterance never starts.

Could this be isolated later:
Mostly. The TTS policy and driver are EvenChess-specific inside the shared UI library, but keeping them in `ui/lib` avoids duplicating TTS safety and speech behavior across live, review, study, and future analysis surfaces.

Upstream merge risk:
Low. Future shared UI or speech-synthesis changes must preserve the safety checks and mobile-safe browser speech behavior.

Tests added or updated:
`ui/lib/tests/evenchessTts.test.ts` covers the mobile-safe browser driver path.

Notes:
Integration log entry: `INT-2026-187`.

### PM-2026-183 - EvenChess Help level ladder copy

File touched:
`modules/evenchess/src/main/PublicShell.scala`

EvenChess requirement:
REQ-D-V2-034; REQ-D-V2-034A.

Why core Lichess file had to be touched:
The top-bar help popup renders deployment-safe public copy from the shared EvenChess public shell model. The level-by-level L1-L10 explanation belongs in that copy source so layout and navigation code can remain generic.

Could this be isolated later:
Yes. The copy is already EvenChess-namespaced in `modules/evenchess`; future work could move public help content into a dedicated CMS/config surface if launch copy needs frequent operator edits.

Upstream merge risk:
Low. Future top-bar help rendering must preserve the concise L1-L10 guide and explain repeated allowance features at their first unlock only.

Tests added or updated:
`PublicShellTest.scala` covers the L1-L10 help copy, first-unlock Proposed Move explanation, and repeated allowance wording.

Notes:
Integration log entry: `INT-2026-188`.

### PM-2026-184 - Live level indicator row layout and coach Used Level badge

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`

EvenChess requirement:
REQ-F-V2-006B; REQ-F-V2-006D.

Why core Lichess file had to be touched:
The native round EvenChess overlay owns the live levels card and coach-card header. The local and opponent Set/Used Level indicators need a stable two-row layout across desktop and mobile, and the coach-card badge must describe the displayed value as Used Level rather than a generic level.

Could this be isolated later:
Mostly. The behavior is EvenChess-specific, but the visible live round shell remains mounted in the native round UI bundle.

Upstream merge risk:
Low. Future round overlay or responsive stylesheet changes must preserve the two equal-column level rows and the `Used Level` coach badge wording.

Tests added or updated:
`evenchessOverlay.test.ts` covers the two-row player/opponent level summary order and coach-card Used Level badge text.

Notes:
Integration log entry: `INT-2026-189`.

### PM-2026-185 - Coach TTS excludes Potential Moves text

File touched:
`ui/round/src/view/evenchessOverlay.ts`

EvenChess requirement:
REQ-P-V2-024A; OVR-V2-041.

Why core Lichess file had to be touched:
The native round EvenChess overlay owns the live coach-card TTS source text. Potential Moves now remain visible in the coach text area as selectable/visual move options, but they are not spoken by manual Speak or Auto Speak. The speech source therefore includes the base coach card and Proposed Move/Ask AI text only.

Could this be isolated later:
Mostly. TTS behavior is EvenChess-specific, but it remains attached to the native round coach-card renderer until EvenChess has a detached round-side plugin surface.

Upstream merge risk:
Low. Future TTS or coach-card changes must preserve the visual-only rule for Potential Moves while keeping Ask AI and Proposed Move advice speakable.

Tests added or updated:
`evenchessOverlay.test.ts` verifies visible Potential Moves result text is excluded from the coach-card TTS source.

Notes:
Integration log entry: `INT-2026-190`.

### PM-2026-186 - Live display-state hydration for threat toggles

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`

EvenChess requirement:
REQ-F-V2-006A; REQ-F-V2-006E; REQ-L-V2-006; REQ-O-V2-025.

Why core Lichess file had to be touched:
The native round live overlay refresh endpoint owns the same-origin payload that rehydrates EvenChess display state during a game. It previously returned opponent level display only, so a browser reload or local no-policy computer round could fall back to default L0/all-off display state even when the player had enabled L4 threat arrows. The round overlay renderer now accepts current-player display state from live payloads and keeps a same-browser per-game fallback for local/no-policy sessions.

Could this be isolated later:
Mostly. The behavior is EvenChess-specific, but it remains attached to native round JSON/controller and round overlay rendering until EvenChess display policy is moved behind a dedicated production display-state service.

Upstream merge risk:
Low. Future round overlay refresh or controller changes must preserve current-player display-state hydration and the Set-Level-capped local fallback.

Tests added or updated:
`evenchessOverlay.test.ts` verifies live payload display-state hydration and same-browser refresh restoration of locally persisted threat-toggle state.

Notes:
Integration log entry: `INT-2026-191`.

### PM-2026-187 - EvenChess live clock layout preservation

File touched:
`ui/round/css/_app-layout.scss`

EvenChess requirement:
REQ-D-V2-011A; REQ-V-V2-003.

Why core Lichess file had to be touched:
The native round grid owns clock, player, table, board, and EvenChess coach placement. The EvenChess desktop grid placed the native table on the same row as `clock-top`, which could leave the opponent clock bar visible while covering the clock text. The mobile grid placed the coach card before the bottom player/timer rows, pushing the active timer below coaching. The round app layout is the correct seam for preserving native Lichess clocks while adding EvenChess side panels.

Could this be isolated later:
Mostly. The adjustment is EvenChess-specific inside the native round layout stylesheet. If EvenChess gets a fully separate round-side layout plugin, this can move with that plugin.

Upstream merge risk:
Low to medium. Future round grid changes must preserve non-overlap between native clocks/table and the EvenChess coach card, especially when `.round__app.evenchess-live-layout` uses `display: contents` on wide layouts.

Tests added or updated:
No unit test added; this is a SCSS grid placement correction. Verified through round UI build and browser geometry inspection.

Notes:
Integration log entry: `INT-2026-192`.

### PM-2026-188 - Draw-mode proposed move arrow validation

File touched:
`ui/round/src/view/evenchessOverlay.ts`

EvenChess requirement:
REQ-P-V2-017C; OVR-V2-052.

Why core Lichess file had to be touched:
The native round overlay owns the Proposed Move button and Draw-mode arrow selection. Draw mode disables native piece movement while touch/pointer input is being used for green arrows, which also clears Chessground's current movable-destination map. Proposed Move must still treat that one green Draw-mode arrow as the proposed move input, while validating it against the authoritative server-provided `possibleMoves` data before the same-origin ECE bridge is called.

Could this be isolated later:
Mostly. The behavior is EvenChess-specific, but it remains attached to the native round overlay until the live coach card has a dedicated plugin seam.

Upstream merge risk:
Low. Future Draw-mode or proposed-move selection changes must preserve server `possibleMoves` validation when Chessground movement is disabled.

Tests added or updated:
`evenchessOverlay.test.ts` covers a Draw-mode green arrow being accepted when Chessground movable destinations are empty but `data.possibleMoves` authorizes the move.

Notes:
Integration log entry: `INT-2026-193`.

### PM-2026-189 - Proposed Move after-move ECS payload parsing

File touched:
`app/controllers/EvenChess.scala`

EvenChess requirement:
REQ-K-V2 proposed-move display requirements; OVR-V2-047.

Why core Lichess file had to be touched:
The EvenChess controller owns the same-origin server-to-server bridge from the browser action to private ECE Proposed ECS. ECE now returns legal proposed-move post-move board data through `after_move_initial_ecs` plus optional `after_move_advanced_ecs` addenda, while older ECL parsing only recognized `after_move_standard_ecs` and the legacy `after_move_side_output` fallback. The controller must normalize these ECE shapes before the round overlay can render the hypothetical post-move cards and markers.

Could this be isolated later:
Yes. This can move behind a dedicated ECE action adapter once the EvenChess controller seams are split from local testing endpoints.

Upstream merge risk:
Low. The change is EvenChess-specific and remains inside the EvenChess controller. Future Proposed ECS contract changes must preserve post-move side-output normalization before browser delivery.

Tests added or updated:
No new test was added for the private controller helper; existing round overlay tests already verify that proposed post-move cards and visuals render when the normalized card contains them.

Notes:
Integration log entry: `INT-2026-194`.

### PM-2026-190 - Combined coach and level controls layout

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_app-layout.scss`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-MAIN-V2-005; REQ-X-V2-001; REQ-U-V2-003; REQ-V-V2-003.

Why core Lichess file had to be touched:
The native round grid owns board, clock, coach, WikiBook, and side-panel placement. The EvenChess levels card has been folded into the coach card so Set/Used Level, quick level selection, and scrollable feature toggles live in one player-facing coaching surface. Desktop layout now places the combined coach card left of the board and moves WikiBook to the top-right column; mobile layout keeps board/player/timer rows first, then coach, then WikiBook.

Could this be isolated later:
Mostly. The controls are EvenChess-specific, but placement remains in the native round grid until EvenChess has a dedicated round-side plugin/layout seam.

Upstream merge risk:
Medium. `ui/round/css/_app-layout.scss` and `ui/round/css/_layout.scss` are upstream round layout seams. Future round grid changes must preserve `coach ece-eval board wiki` desktop placement, the mobile player/timer-before-coach order, and the coach-card-contained level selector/toggles.

Tests added or updated:
`evenchessOverlay.test.ts` verifies the responsive layout map, absence of the separate level card, level controls inside the coach card, and WikiBook rendering after the coach card in mobile/render order.
The full UI build `ui/build --debug --no-install --no-color` was also run after the source change so `public/compiled/manifest.json` points local ECL at the regenerated round assets: `round.WPJGB7CP.js` and `round.36f00acf.css`.

Notes:
Integration log entry: `INT-2026-195`.

### PM-2026-191 - EvenChess right-side table vertical centering

File touched:
`ui/round/css/_app-layout.scss`; `ui/round/css/_layout.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-D-V2-011A; REQ-MAIN-V2-005.

Why core Lichess file had to be touched:
The native round grid owns the right-side game table placement. After moving WikiBook to the top-right column, the EvenChess layout still anchored `.round__app__table` to lower grid rows, so the native move/status card sat below the board instead of remaining vertically centered beside it. The table now spans the board-height row group and centers itself. Only a non-empty, open WikiBook can move it down into the space below the opening panel; a closed or empty WikiBook shell keeps the table centered.

Could this be isolated later:
Mostly. The behavior is EvenChess-specific round layout polish, but it remains in the native round grid until EvenChess has a dedicated round-side layout seam.

Upstream merge risk:
Medium. Future round grid changes must preserve centered native table placement on the right side of the board when the WikiBook panel is collapsed or empty, and the non-empty open-WikiBook fallback that starts the table below the opening panel.

Tests added or updated:
`evenchessOverlay.test.ts` verifies the EvenChess layout centers `.round__app__table` across board rows and switches it below only a non-empty open WikiBook panel. The full UI build `ui/build --debug --no-install --no-color` regenerated `public/compiled/manifest.json` so local ECL serves `round.I7LQQFYQ.js` and `round.e31bb517.css`.

Notes:
Integration log entry: `INT-2026-197`.

### PM-2026-192 - Compact coach action button labels

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-D-V2-010; REQ-D-V2-013; REQ-MAIN-V2-005.

Why core Lichess file had to be touched:
The native round overlay owns the EvenChess coach card action controls. The potential-move buttons needed shorter player-facing labels and the shared coach action buttons needed a smaller, consistent footprint so Ask AI, Opponent potential, My potential, and Proposed Move controls align cleanly inside the coach card.

Could this be isolated later:
Yes. This is EvenChess-specific UI copy and coach-card styling that can move with a dedicated EvenChess round-side component once available.

Upstream merge risk:
Low. Future coach-card action changes should keep the shared `evenchess-live__proposed-button` sizing consistent across all action buttons.

Tests added or updated:
`evenchessOverlay.test.ts` verifies the new potential-move button labels and compact shared button sizing. The full UI build `ui/build --debug --no-install --no-color` regenerated `public/compiled/manifest.json` so local ECL serves `round.I7LQQFYQ.js` and `round.e31bb517.css`.

Notes:
Integration log entry: `INT-2026-198`.

### PM-2026-193 - Compact homepage EvenChess summary card sizing

File touched:
`ui/lobby/css/_lobby.scss`

EvenChess requirement:
REQ-D-V2-020; REQ-D-V2-021; REQ-D-V2-023; REQ-D-V2-024.

Why core Lichess file had to be touched:
The native lobby homepage stylesheet owns the `What is EvenChess?` summary card and compact fact cards. The summary card was visually too large beside the active-games lobby, so its width, padding, border, text scale, fact-card grid, and line spacing were reduced while preserving the existing homepage copy and Lichess lobby layout.

Could this be isolated later:
Yes. This is EvenChess-specific public-copy styling that can move behind a dedicated homepage callout component or CSS entrypoint if the lobby gets an extension seam.

Upstream merge risk:
Low. The selector is EvenChess-specific, but the file is a native lobby stylesheet. Future lobby layout changes should keep the homepage callout compact and avoid turning it into a landing-page-sized panel.

Tests added or updated:
No automated test was added for the CSS-only size adjustment. The full UI build `ui/build --debug --no-install --no-color` regenerated `public/compiled/manifest.json` so local ECL serves `manifest.3da68719.js` with lobby CSS `lobby.fcb064e9.css`. Browser verification on `http://localhost:8080/` confirmed the live card uses `lobby.fcb064e9.css`, renders about 231px wide in the checked viewport, and uses about 10px body text with an 11.6px heading.

Notes:
Integration log entry: `INT-2026-199`.

### PM-2026-194 - Ask AI loaded-position hydration and Position ECS context retention

File touched:
`app/controllers/EvenChess.scala`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`

EvenChess requirement:
J.5 Board-State Call; REQ-L-V2-014; REQ-L-V2-026; REQ-P-V2-012; REQ-P-V2-012A; REQ-P-V2-016; REQ-P-V2-017; REQ-P-V2-019; REQ-P-V2-019A; REQ-P-V2-019C.

Why core Lichess file had to be touched:
The native EvenChess controller owns the same-origin Standard ECS and Position ECS bridge, and the native round overlay owns the Ask AI button. Loaded in-progress games could reach the Ask AI button before a current Standard ECS live payload was accepted, leaving the UI on `Awaiting payload` until a later move refreshed the overlay. ECL also only retained Position ECS context from a top-level `position_ecs` object and did not preserve the context id when syncing overlay assistance into round state.

Could this be isolated later:
Mostly. The ECE adapter parsing belongs in a dedicated EvenChess service adapter, and the round-side hydration/retry behavior belongs in the EvenChess coach action component once those seams exist.

Upstream merge risk:
Medium. The controller and round overlay are upstream/core seams. Future Standard ECS or Position ECS response-shape changes must preserve server-to-server ECE calls, current-FEN gating, retained `position_ecs_id` context, and the first-click hydration retry for loaded positions.

Tests added or updated:
`evenchessOverlay.test.ts` verifies Position ECS context id/status/expiry/endpoint are retained when a live overlay is applied. `evenchessTestGround.test.ts` verifies Position ECS request URLs forward a context id retained in normalized round state. `node ui/test round/tests/evenchessOverlay.test.ts`, `node ui/test round/tests/evenchessTestGround.test.ts`, full `ui/build --debug --no-install --no-color`, Docker-backed `./lila.sh compile`, and a same-origin `POST /evenchess/testground/ece/position-ecs` smoke call all passed. The UI build regenerated `public/compiled/manifest.json` so local ECL serves `manifest.34f24ab7.js` with `round.2UJCI3BR.js` and `round.e31bb517.css`.

Notes:
Integration log entry: `INT-2026-200`.

### PM-2026-195 - Mobile coach card non-overlap with native round strips

File touched:
`ui/round/css/_app-layout.scss`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-D-V2-010; REQ-D-V2-011; REQ-D-V2-011A; REQ-MAIN-V2-005.

Why core Lichess file had to be touched:
The native mobile round grid owns the player row, action strip, horizontal move strip, and the EvenChess coach-card insertion point. The previous EvenChess mobile grid put the combined coach card before native controls and moves, which allowed the replay/action controls, move strip, and game metadata to visually cross the coach content on phone layouts. The mobile EvenChess grid now reserves native rows first and starts the coach card below the move strip.

Could this be isolated later:
Mostly. The coach card is EvenChess-specific, but the non-overlap fix belongs in the native round grid until EvenChess has a dedicated mobile round-side layout seam.

Upstream merge risk:
Medium. Future mobile round grid changes must preserve the order `user-bot -> pocket-bot -> controls -> moves -> coach` for `.round__app.evenchess-live-layout` and keep coach-card controls wrapping instead of overflowing on narrow screens.

Tests added or updated:
`evenchessOverlay.test.ts` verifies the mobile grid order and mobile coach-card wrapping rules. `node ui/test round/tests/evenchessOverlay.test.ts`, `pnpm exec stylelint ui/round/css/_app-layout.scss ui/round/css/_evenchess-live.scss`, `git diff --check -- ui/round/css/_app-layout.scss ui/round/css/_evenchess-live.scss ui/round/tests/evenchessOverlay.test.ts`, and full `ui/build --debug --no-install --no-color` passed. The UI build regenerated local assets so ECL serves `manifest.11a4c48e.js`, `round.2UJCI3BR.js`, and `round.3c3c2b22.css`.

Notes:
Integration log entry: `INT-2026-201`.

### PM-2026-196 - Desktop native right rail stays beside board center with WikiBook

File touched:
`ui/round/css/_app-layout.scss`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-D-V2-010; REQ-D-V2-011A; REQ-MAIN-V2-005.

Why core Lichess file had to be touched:
EvenChess inserts WikiBook, the coach card, and the eval rail into the native round layout, but the native material/player/move table still belongs to the upstream round grid. The earlier desktop placement allowed WikiBook and empty right-side rows to push the captured-material strip and move table far below the board. The EvenChess desktop grid now keeps WikiBook capped in the top-right row, zeroes empty spacer rows, and centers `.round__app__table` across the board-height row group.

Could this be isolated later:
Partially. The WikiBook and coach placement are EvenChess-specific, but the material/player/move table remains a native round surface until an explicit EvenChess desktop side-rail seam exists.

Upstream merge risk:
Medium. Future upstream round-grid changes must preserve the EvenChess-specific `wiki -> mat-top -> clock/user/moves/controls` ordering, the centered table span, and the capped desktop WikiBook height so the native right rail does not drift below the board.

Tests added or updated:
`evenchessOverlay.test.ts` verifies the desktop grid order, the centered right-table span, removal of the old open-WikiBook table push-down rule, and the desktop WikiBook height cap. `node ui/test round/tests/evenchessOverlay.test.ts`, `pnpm exec stylelint ui/round/css/_app-layout.scss ui/round/css/_layout.scss ui/round/css/_evenchess-live.scss`, scoped `git diff --check`, and full `ui/build --debug --no-install --no-color` passed. Browser verification at a 2048x1065 viewport confirmed local ECL serves `manifest.6485012e.js` and `round.c6a2f3a0.css`, with the board at 500px high and the right-side material/user/move group beside the board rather than below it.

Notes:
Integration log entry: `INT-2026-202`.

### PM-2026-197 - Compact coach card controls and larger text pane

File touched:
`ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`

EvenChess requirement:
REQ-D-V2-010; REQ-D-V2-011A; REQ-MAIN-V2-005.

Why core Lichess file had to be touched:
The EvenChess coach card is inserted into the native round layout and its sizing must stay coordinated with the board-side rail. The coach-card controls were taking too much vertical room, leaving the summary/AI text pane cramped. The coach card now uses a compact scoped sizing pass for labels, level pills, TTS/draw controls, level controls, and action buttons, while the text pane gets a larger flex basis and minimum height.

Could this be isolated later:
Yes. This is EvenChess-specific coach-card styling that can move with a dedicated EvenChess round-side component if the current round CSS seam is split later.

Upstream merge risk:
Low to medium. The selectors are EvenChess-specific, but the file is a native round stylesheet. Future coach-card work should preserve the compact internal sizing and the larger `.evenchess-live__coach-text` area.

Tests added or updated:
`evenchessOverlay.test.ts` verifies compact coach-card padding/font sizing, the larger coach text pane, the reduced coach action width, and smaller action buttons. `pnpm exec stylelint ui/round/css/_evenchess-live.scss`, scoped `git diff --check`, direct CSS smoke assertions, and full `ui/build --debug --no-install --no-color` passed. Browser verification on local ECL serving `manifest.8402ee45.js` and `round.7fac552b.css` measured the coach text pane increasing from 121.15px to 266.33px while the card font changed from 14px to 10.5px.

Notes:
Integration log entry: `INT-2026-203`.

### PM-2026-198 - Merged Potential Moves action and race-safe opponent refund

File touched:
`app/controllers/EvenChess.scala`; `conf/routes`; `ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-P-V2-003B; REQ-P-V2-003C; REQ-P-V2-017B; REQ-P-V2-019B; REQ-MAIN-V2-005; OVR-V2-053.

Why core Lichess file had to be touched:
The native EvenChess round overlay owns the coach-card action buttons and local action state, while the EvenChess controller owns the same-origin server-to-server Potential ECS bridge and its local test-ground quota/cache maps. The two separate player/opponent potential buttons have been merged into one `Potential Moves` action that selects the correct reveal kind from the current turn and displays both colour quotas in one status line. The controller now exposes a same-origin refund route so an opponent-potential reveal that races with an immediate opponent move can be restored without browser-to-ECE access.

Could this be isolated later:
Mostly. The coach-card rendering and refund bookkeeping are EvenChess-specific and can move behind a dedicated EvenChess action component/repository once the local controller maps are replaced by production assistance-state storage.

Upstream merge risk:
Medium. The round controller move lifecycle now passes a redraw callback into the EvenChess clear path, and the live coach card relies on current-turn data for the merged action state. Future move lifecycle or action-button refactors must preserve the one-second post-opponent-move cooldown and the three-second opponent-reveal refund window.

Tests added or updated:
`evenchessOverlay.test.ts` verifies the merged button label, student-colour-first quota text, one-second cooldown after an opponent move, and opponent-potential refund request/state update during the grace window. `evenchessTestGround.test.ts` verifies the same-origin refund URL. `ui/build --debug --no-install --no-color` regenerated `public/compiled/manifest.json` so local ECL serves `manifest.0823620d.js` and round CSS `round.7fac552b.css`.

Notes:
Integration log entry: `INT-2026-204`.

### PM-2026-199 - Coach card top action strip and shared action status

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-P-V2-017B; REQ-D-V2-010; REQ-D-V2-011A; REQ-MAIN-V2-005; OVR-V2-054.

Why core Lichess file had to be touched:
The native round overlay owns the EvenChess coach card and its action control layout. The bottom Ask AI/Potential/Proposed action block has been moved into the top of the coach card beneath the level badges, with the tool row ordered `Speak`, `Auto`, `Draw`, `Ask AI`, and the assistance row showing `Potential Moves` plus quota text and `Proposed move check` plus quota text. The raw audit-id sentence is no longer rendered in the visible card.

Could this be isolated later:
Yes. This is EvenChess-specific coach-card layout and action rendering that can move with a dedicated EvenChess round-side component.

Upstream merge risk:
Low to medium. The selectors are EvenChess-specific, but the renderer and SCSS live in native round files. Future coach-card work must keep action feedback in one shared status area and preserve the top-row control order.

Tests added or updated:
`evenchessOverlay.test.ts` verifies the top action order, new `Proposed move check` label, absence of the visible audit element, adjacent Potential/Proposed quota text, and compact top action sizing.

Notes:
Integration log entry: `INT-2026-205`.

### PM-2026-200 - Coach card bottom aligns with board and scrolls text

File touched:
`ui/round/src/view/main.ts`; `ui/round/css/_app-layout.scss`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-P-V2-017B; REQ-D-V2-011A; REQ-MAIN-V2-005.

Why core Lichess file had to be touched:
The native round measurement hook, desktop grid template, and CSS control the EvenChess coach card's size relative to the board and the game-info card above it. The round layout now stops the coach/board/eval grid areas before the move-table tail rows, publishes a measured `--evenchess-coach-height` from the coach row's actual top to the board bottom when the round view redraws, and lets the coach card fill the corrected grid span on initial page load. The coach text pane shrinks and scrolls internally for longer summaries/results.

Could this be isolated later:
Yes. This is EvenChess-specific round-side layout and should move with a dedicated EvenChess coach card component if the round layout seam is split.

Upstream merge risk:
Low to medium. Preserve the EvenChess desktop grid-area stop row, `--evenchess-coach-height` as the desktop coach-card height override, and `.evenchess-live__coach-text` internal scrolling if upstream round layout or measurement changes.

Tests added or updated:
`evenchessOverlay.test.ts` verifies that the coach card uses `--evenchess-coach-height` with a grid-height fallback and that the text pane can shrink and scroll instead of forcing the card below the board.

Notes:
Integration log entry: `INT-2026-206`.

### PM-2026-201 - Homepage summary card column alignment and mobile width

File touched:
`ui/lobby/css/_lobby.scss`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-D-V2-020; REQ-D-V2-021; REQ-D-V2-023; REQ-D-V2-024.

Why core Lichess file had to be touched:
The native lobby stylesheet owns the `What is EvenChess?` summary card's grid item placement. The compact card is now full-width in the mobile/default lobby stack, while wider lobby layouts keep the existing compact size but center it horizontally within the left supporting column and move it slightly below the top edge of the quick-pairing/lobby row.

Could this be isolated later:
Yes. This is EvenChess-specific public-homepage styling that can move behind a dedicated homepage callout component or CSS entrypoint if the lobby gets an extension seam.

Upstream merge risk:
Low. The selector is EvenChess-specific, but the file is a native lobby stylesheet. Future lobby layout changes should preserve mobile full-width behavior and desktop column centering for the compact summary card.

Tests added or updated:
No automated test was added for the CSS-only placement adjustment. Browser verification on `http://localhost:8080/` checked the desktop card centered within the first lobby column. Generated lobby CSS inspection confirmed the mobile/default card uses full available width before the wider-screen compact centering rule applies.

Notes:
Integration log entry: `INT-2026-207`.

### PM-2026-202 - Coach card dropdown/action row refinement

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-P-V2-017B; REQ-P-V2-017A; REQ-D-V2-011A; REQ-MAIN-V2-005; OVR-V2-054; OVR-V2-055.

Why core Lichess file had to be touched:
The native round overlay owns the EvenChess coach-card vnode tree and SCSS. The coach-card controls now keep `Speak`, `Auto`, and `Draw` as the equal-sized tool row, move `Ask AI` into the equal-width assistance row with `Potential Moves` and `Proposed move check`, place per-action allowance text under each assistance button, and keep one shared status area for action feedback. The level selector and Level toggles disclosure now share an equal-width row, the selector options include `Apply up to: ...`, the `Features` hint is removed, and opening Level toggles overlays the text pane instead of reflowing Summary out of the card.

Could this be isolated later:
Yes. This remains EvenChess-specific coach-card presentation and can move into a dedicated EvenChess round-side component if the round overlay gains a stronger extension seam.

Upstream merge risk:
Low to medium. The touched selectors are EvenChess-specific, but they live in native round UI files. Future coach-card refactors must preserve the tool/action row split, non-button Set/Used indicators, equal level-control row, overlay-style Level toggles disclosure, and scrollable coach text pane.

Tests added or updated:
`evenchessOverlay.test.ts` verifies the player-turn coach-card row order, absence of the visible `Features` hint, `Apply up to: ...` select labels, equal three-column assistance layout, absolute Level toggles panel, and compact button sizing.

Notes:
Integration log entry: `INT-2026-208`.

### PM-2026-203 - Merged Potential Moves current-turn click guard

File touched:
`ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

EvenChess requirement:
REQ-P-V2-017B; REQ-P-V2-003; REQ-P-V2-003C; REQ-MAIN-V2-005; OVR-V2-056.

Why core Lichess file had to be touched:
The native round overlay owns the merged EvenChess `Potential Moves` button. The visible merged button now derives the request kind from the current turn at click time, so a turn change between render and click requests the current side's potential moves instead of using a stale `player`/`opponent` kind and surfacing old per-side availability text.

Could this be isolated later:
Yes. This is EvenChess-specific action-button behavior and can move with a dedicated EvenChess coach-card component.

Upstream merge risk:
Low. The change stays inside EvenChess overlay helpers. Future refactors must preserve current-turn derivation for the merged button while keeping explicit low-level side-mismatched requests blocked before ECE transport.

Tests added or updated:
`evenchessOverlay.test.ts` adds a stale-render click regression: render `Potential Moves` on the student's turn, switch to the opponent turn before invoking the click listener, then verify the request goes out with `kind=opponent` and no old "Available on..." message is set.

Notes:
Integration log entry: `INT-2026-209`.

### PM-2026-204 - Round chat stays below the EvenChess coach card

File touched:
`ui/round/css/_layout.scss`; `ui/round/tests/evenchessLayout.test.ts`

EvenChess requirement:
V2 live round coach-card layout polish; preserve the public round page order where the game-info card sits above the EvenChess coach card and the native chat, when present, sits below the coach card.

Why core Lichess file had to be touched:
The native round parent grid owns placement of `round__side`, `round__app`, `round__underchat`, clocks, material rows, and the EvenChess `evenchess-live-layout` desktop override. Online game chat renders as `.mchat` inside `round__side`, while some non-chat/empty cases still expose `round__underchat`. The EvenChess wide layout now lets the side children participate in the parent grid, keeps `game__meta` in the single top side slot, starts coach on the next row with a small internal top gap while preserving board-bottom alignment, and assigns both the real `.mchat` and `round__underchat` fallback to the `uchat` area immediately after the coach/moves row so chat cannot appear between the game-info card and the coach card or collapse to a tab-only strip. It also restores breathing room between chat and the under-board crosstable, keeps EvenChess-layout clock text compact, and keeps material rows content-sized near the move-card column.

Could this be isolated later:
Yes. If the round layout gains a dedicated EvenChess side-column extension seam, this ordering can move out of the upstream round grid override.

Upstream merge risk:
Low to medium. Future round grid changes must preserve the `side -> coach -> uchat -> under` order for `.round:has(> .round__app.evenchess-live-layout)` and keep the coach card aligned to the board bottom while chat starts below it with a visible gap before under-board content.

Tests added or updated:
`evenchessLayout.test.ts` verifies the desktop grid row order, the coach top gap, the chat-to-underboard spacer row, `round__underchat` fallback placement in the `uchat` area, the `.round__side > .mchat` placement used by online game chat, compact EvenChess clock text, and content-sized material rows.

Notes:
Integration log entry: `INT-2026-210`.

### PM-2026-205 - EvenChess search games use native lobby first-move expiration

File touched:
`modules/challenge/src/main/ChallengeApi.scala`; `modules/challenge/src/main/ChallengeJoiner.scala`; `app/controllers/EvenChess.scala`; `modules/challenge/src/test/JoinerTest.scala`

EvenChess requirement:
REQ-A-V2-003; REQ-C-V2-003; REQ-C-V2-005; REQ-H-V2-003; REQ-H-V2-044E.

Why core Lichess file had to be touched:
EvenChess public search hands finalized human/human and roster-backed bot contracts into the native challenge accept path so the game room, clocks, usernames, and round shell stay Lichess-shaped. Native challenge games are normally created as `Source.Friend`, but Lichess first-move no-start expiration is intentionally limited to public matchmaking-style sources such as `Source.Lobby`. The challenge accept seam now accepts an optional game-source override, and only the EvenChess search handoff passes `Source.Lobby`, so matched EvenChess search games opt back into native Lichess first-move expiration without changing ordinary friend challenges.

Could this be isolated later:
Yes. A dedicated EvenChess game-contract creation adapter could create the native game directly with the desired source instead of passing an override through the challenge joiner. Until then, the override keeps the existing normal challenge/game path and avoids duplicating Lichess clock or abort logic.

Upstream merge risk:
Medium. `ChallengeApi` and `ChallengeJoiner` are native challenge seams. Future upstream challenge refactors must preserve default friend-game behavior while allowing EvenChess search-created games to use a public matchmaking source so `GameExt.expirable`, round `ScheduleExpiration`, and native `NoStart` handling remain active.

Tests added or updated:
`JoinerTest` verifies that accepted challenges still default to `Source.Friend` and are not first-move expirable, while an accepted matchmaking challenge with the source override becomes `Source.Lobby` and is first-move expirable through the existing Lichess `GameExt.expirable` rule.

Notes:
Integration log entry: `INT-2026-211`.

### PM-2026-206 - Native first-move expiration banner remains visible in EvenChess desktop layout

File touched:
`ui/round/css/_layout.scss`; `ui/round/tests/evenchessLayout.test.ts`

EvenChess requirement:
REQ-A-V2-003; REQ-C-V2-003; REQ-C-V2-005; REQ-D-V2-011A; REQ-H-V2-003.

Why core Lichess file had to be touched:
The native round parent grid owns placement of the Lichess first-move expiration banner through the `expi-top` and `expi-bot` grid areas. The EvenChess wide desktop override had zero-height tracks for those rows while repositioning the coach, chat, and move table, so the server-side no-start timer could abort correctly while the player could not see the natural Lichess green countdown banner. The expiration rows are now auto-sized so they collapse when no banner exists and expand only when the native expiration vnode is present.

Could this be isolated later:
Yes. If EvenChess receives a dedicated round side-column extension point, the parent-grid override can move out of the native round layout file. The first-move expiration areas should still remain native Lichess-owned.

Upstream merge risk:
Low to medium. Future EvenChess desktop layout changes must not force `expi-top` or `expi-bot` to zero-height rows, because that hides the native Lichess first-move countdown while leaving server abort active.

Tests added or updated:
`evenchessLayout.test.ts` verifies the EvenChess wide desktop grid keeps auto-sized rows for the native expiration tracks and preserves `expi-top` between the top clock/user rows and `expi-bot` between bottom user/clock rows.

Notes:
Integration log entry: `INT-2026-212`.

### PM-2026-207 - EvenChess quick-pairing waiting tile uses native spinner

File touched:
`ui/lobby/src/view/pools.ts`; `ui/lobby/tests/evenchessPools.test.ts`

EvenChess requirement:
REQ-A-V2-003; REQ-D-V2-011; REQ-G-V2-029B; REQ-H-V2-003.

Why core Lichess file had to be touched:
The native lobby pool renderer owns the quick-pairing tiles and the normal Lichess waiting animation. EvenChess search can use the same active pool tile state, but the modal-to-pool path may carry rating-range data, which makes the native renderer show the range sweep instead of the spinner. The renderer now treats `evenChessPoolMember` as an EvenChess search state and always displays the native spinner in the active tile, while ordinary native pool searches still show the rating range when available.

Could this be isolated later:
Yes. If EvenChess gets a dedicated public search component, this can move out of the native pool renderer. Until then, the small branch keeps EvenChess search visually aligned with native quick pairing.

Upstream merge risk:
Low. The change preserves default native pool behavior and only changes the waiting child vnode when the active member is the EvenChess-specific `evenChessPoolMember`.

Tests added or updated:
`evenchessPools.test.ts` verifies native pool waiting still shows the range sweep with a range and EvenChess waiting shows the spinner even if range data exists.

Notes:
Integration log entry: `INT-2026-213`.

### PM-2026-208 - Native analysis ECEMF Coach Review surface

File touched:
`conf/routes`; `app/controllers/EvenChess.scala`; `ui/analyse/src/ctrl.ts`; `ui/analyse/src/view/main.ts`; `ui/analyse/src/view/evenchessReview.ts`; `ui/analyse/css/_layout.scss`; `ui/analyse/css/_side.scss`; `ui/analyse/css/_evenchess-ai.scss`; `ui/analyse/tests/evenchessReview.test.ts`; `ui/round/src/evenchessReview.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessReview.test.ts`; `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`

EvenChess requirement:
REQ-A-V2-003; REQ-D-V2-011A; REQ-H-V2-003; Phase M analysis memory/review mode requirements; ECE combined requirements for `/v1/ece/full-match`, `/v1/ece/full-match-summary`, and `/v1/ece/position`.

Why core Lichess file had to be touched:
The native Lichess analysis/replay page owns the move selector, selected ply, side panel, and board overlay lifecycle. EvenChess post-game ECEMF review must follow that native selected ply instead of building a separate replay board. The analysis controller now sends the real game id and side-to-move to the existing EvenChess universal overlay endpoint. The analysis side panel renders a namespaced EvenChess Coach Review card in an `analyse__side-stack` so Lichess's server-provided replay side panel can still replace its normal `aside.analyse__side` without deleting the review card. The round replay shell also mounts a production post-game review panel for completed-game contexts that still use the round bundle.

Could this be isolated later:
Yes. A future analysis-page extension registry could let EvenChess mount the review card and replay overlay without touching `ui/analyse/src/view/main.ts` or `ui/analyse/src/ctrl.ts`. Until then, the additions remain namespaced and small.

Upstream merge risk:
Medium. Future upstream analysis side-panel, server-side replay panel, round overlay, or controller refactors must preserve the EvenChess card mount point, the real game id passed to the overlay fetch, side-to-move replay overlay requests for stored ECEMF frames, and the production review routes' configured/default/docker-host ECE fallback behavior.

Tests added or updated:
`ui/analyse/tests/evenchessReview.test.ts` and `ui/round/tests/evenchessReview.test.ts` verify stored ECEMF text selection, non-live Ask AI text priority, match-summary text extraction, and quota labels.

Notes:
Integration log entry: `INT-2026-214`.
