# EvenChess-Lichess Integration Log

**Suite:** EvenChess-Lichess Version 1
**Status:** Live integration ledger
**Created:** 2026-05-29

## Purpose

This document records EvenChess-to-Lichess integration seams required to preserve updateability on future Lichess upstream syncs.

For this phase, it records documentation-only direction alignment for the native Lichess setup-flow integration model.

## Govenance

- Upstream/core-file edits remain bound to the patch map.
- This log records seam intent, rollback notes, and reapplication guidance for future upstream syncs.
- If the native-flow interpretation changes, supersede this entry and update Appendix Z first.

## Entry Registry

| Entry ID | Phase | Date | Seam / area | Lichess files touched | EvenChess files touched | Patch map | Tests / checks | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| INT-2026-026 | Native-Lichess-Integration-B/D | 2026-05-29 | Public shell narrowed back toward Lichess-native homepage and EvenChess route pages | `app/http/KeyPages.scala`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`; `app/views/evenchess/play.scala`; `app/views/evenchess/account.scala` | `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`; `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_3_COMPLETION_REPORT.md` | `PM-2026-013` | `PublicShellTest`; `evenchess/test`; root compile; route smoke | Complete |
| INT-2026-027 | Native-Lichess-Integration-B | 2026-05-29 | Shared shell wordmark says EvenChess while preserving Lichess layout | `modules/web/src/main/ui/layout.scala` | None | `PM-2026-014` | root compile; homepage smoke | Complete |
| INT-2026-028 | Native-Lichess-Integration-B/C | 2026-05-29 | Primary Play navigation starts EvenChess while preserving Lichess-style nav | `modules/web/src/main/ui/TopNav.scala` | `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala` | `PM-2026-015` | `PublicShellTest`; root compile; homepage/nav smoke | Complete |
| INT-2026-029 | Current-Version-1-Native-Setup | 2026-05-29 | Native lobby setup modal carries EvenChess settings and submits to EvenChess search contract | `app/controllers/EvenChess.scala`; `conf/routes`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`; `ui/lobby/src/view/table.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/interfaces.ts`; `ui/lobby/css/_setup.scss`; `ui/lobby/css/_table.scss` | `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PublicShellTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala` | `PM-2026-016` | `PublicShellTest`; `PlaySearchIntegrationTest`; lobby TypeScript/build/style checks; root compile; route smoke | Complete |
| INT-2026-030 | Native-Lichess-Integration-A | 2026-05-29 | Lichess-first documentation/override alignment | None | `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/requirements/APPENDIX_T_CODEX_PHASE_PLAN_AND_TASK_PACKETS.md`; `docs/requirements/APPENDIX_Z_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md`; `docs/requirements/EVENCHESS_LICHESS_REQUIREMENTS_MAIN.md`; `docs/requirements/EVENCHESS_LICHESS_STAGE_1_LOCAL_HANDOVER.md`; `docs/requirements/EVENCHESS_UPSTREAM_SYNC_PROCESS.md`; `docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/requirements/EVENCHESS_LICHESS_REQUIREMENTS_DIFF.md` | None | `git diff --check` | Complete |
| INT-2026-032 | Native-Lichess-Integration-C | 2026-05-29 | Lichess navigation restoration and EvenChess play entrypoint to native setup flow | `modules/web/src/main/ui/TopNav.scala` | None | `PM-2026-015`, `PM-2026-016` | `./bin/sbt testOnly lila.evenchess.PublicShellTest` (if environment available); compile smoke | Complete |
| INT-2026-033 | Native-Lichess-Integration-C | 2026-05-29 | Keep `/evenchess/*` routes namespaced and explicit | `conf/routes`; `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala` | None | `PM-2026-010`, `PM-2026-016` | `./bin/sbt testOnly lila.evenchess.PublicShellTest` | Complete |
| INT-2026-034 | Native-Lichess-Integration-F | 2026-05-29 | EvenChess admin backend setting hardening and secret-safety enforcement | `app/controllers/Dev.scala` | `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala` | `PM-2026-017` | `./bin/sbt testOnly lila.evenchess.AdminBackendSettingsTest` | Complete |
| INT-2026-035 | Native-Lichess-Integration-G | 2026-05-29 | Live-board overlay payload hardening | `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts` | `docs/requirements/APPENDIX_T_CODEX_PHASE_PLAN_AND_TASK_PACKETS.md` | `PM-2026-018` | `./ui/test round/tests/evenchessOverlay.test.ts` | Complete |
| INT-2026-036 | Native-Lichess-Integration-H | 2026-05-29 | Opening overlay stale-clear behavior and payload gating | `ui/opening/src/evenchessOpeningAi.ts` | `ui/opening/tests/evenchessOpeningAi.test.ts` | `PM-2026-036` | `./ui/test opening/tests/evenchessOpeningAi.test.ts` | Complete |
| INT-2026-037 | Native-Lichess-Integration-I | 2026-05-29 | TTS overlay-only audit gate for live and learning surfaces | `ui/lib/src/evenchessTts.ts`; `ui/lib/tests/evenchessTts.test.ts` | `modules/evenchess/src/main/TtsCoach.scala`; `modules/evenchess/src/test/TtsCoachTest.scala` | `PM-2026-037` | `./ui/test lib/tests/evenchessTts.test.ts`; `./ui/test round/tests/evenchessOverlay.test.ts`; `./ui/test analyse/tests/evenchessLearning.test.ts`; `./ui/test opening/tests/evenchessOpeningAi.test.ts`; `evenchess/testOnly lila.evenchess.TtsCoachTest` | Complete |
| INT-2026-038 | Native-Lichess-Integration-J | 2026-05-29 | Patch map and integration ledger reconciliation for narrowed upstream seams | None | `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md` | None | scoped `git diff --check`; ledger/patch-map ID audit | Complete |
| INT-2026-039 | V2-Phase-C-Branding-theme | 2026-05-31 | Homepage branding and lobby theme summary | `app/views/lobby/home.scala`; `ui/lobby/css/_lobby.scss` | `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala` | `PM-2026-039` | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-040 | V2-Phase-D-Top-bar-tokens-settings-shell | 2026-05-31 | Top-bar game-token chip and account/settings shell links | `modules/web/src/main/ui/layout.scala`; `ui/lib/css/header/_buttons.scss`; `modules/pref/src/main/ui/AccountUi.scala`; `modules/pref/src/main/ui/AccountPref.scala` | `modules/evenchess/src/main/AccountMonetisationUi.scala`; `modules/evenchess/src/test/AccountMonetisationUiTest.scala` | `PM-2026-040` | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-041 | V2-Phase-E-Setup-search-UI | 2026-05-31 | Native setup modal target controls, Apply preferences gate, token gate copy, and search-status echo | `app/views/lobby/home.scala`; `app/controllers/EvenChess.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/view/table.ts`; `ui/lobby/css/_setup.scss` | `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts` | `PM-2026-041` | `node ui/test lobby/tests/evenchessSetup.test.ts`; `evenchess/testOnly LevelBasedMatchmakingTest PlaySearchIntegrationTest` via Docker fallback | Complete |
| INT-2026-042 | V2-Phase-F-MMR-engine-framework | 2026-05-31 | EvenChess-owned MMR contract, target preference, assigned-level search, audit, and simulation framework | None | `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | `evenchess/testOnly LevelBasedMatchmakingTest PlaySearchIntegrationTest` via Docker fallback | Complete |
| INT-2026-043 | V2-Phase-G-Matchmaking-integration | 2026-05-31 | Search JSON handoff to the EvenChess MMR Engine integration seam with rated/casual preference preservation | `app/controllers/EvenChess.scala` | `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md` | `PM-2026-043` | `evenchess/testOnly LevelBasedMatchmakingTest PlaySearchIntegrationTest` via Docker fallback | Complete |
| INT-2026-044 | V2-Phase-H-ECE-framework-integration | 2026-05-31 | Server-only ECE health/board contract and side-gated response validation framework | None | `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-045 | V2-Phase-I-Display-engine-framework | 2026-05-31 | Mock side-output display compiler for level-gated cards, markers, arrows, and clear decisions | None | `modules/evenchess/src/main/CoachingOverlays.scala`; `modules/evenchess/src/test/CoachingOverlaysTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-046 | V2-Phase-J-Live-ECE-history | 2026-05-31 | Live ECE per-ply history model, scheduled request metadata, and canonical used-level history | None | `modules/evenchess/src/main/LiveCoaching.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-047 | V2-Phase-K-Proposed-move | 2026-05-31 | Proposed-move ECE request/response contract and preview-only live scheduler framework | None | `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/main/LiveCoaching.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-048 | V2-Phase-L-Review-modes | 2026-05-31 | Live White/Black/Both saved-history review modes and custom review cache/token-intent framework | None | `modules/evenchess/src/main/LiveCoaching.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-049 | V2-Phase-M-Full-game-ECE | 2026-05-31 | Full-game ECE request/response contract, saved snapshot handoff, and token/quota-gated post-game review plan | None | `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/main/LiveCoaching.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-050 | V2-Phase-N-Stockfish-AI | 2026-05-31 | Lichess-side Stockfish/AI provider boundary, level caps, AI call budget, and sanitized provider diagnostics framework | None | `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/main/StockfishAnalysisGateway.scala`; `modules/evenchess/src/main/AiCoachPolicy.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/StockfishAnalysisGatewayTest.scala`; `modules/evenchess/src/test/AiCoachPolicyTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-051 | V2-Phase-O-Feature-surfaces | 2026-05-31 | Feature-surface policy framework for puzzles, study, openings, analysis/replay, and computer play | None | `modules/evenchess/src/main/FeatureSurfacePolicy.scala`; `modules/evenchess/src/test/FeatureSurfacePolicyTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-052 | V2-Phase-P-Monetisation | 2026-05-31 | Monetisation policy framework for review/custom-analysis tokens, subscriptions, rewarded ads, game-token settlement, and saved games | None | `modules/evenchess/src/main/SubscriptionTokensAds.scala`; `modules/evenchess/src/test/SubscriptionTokensAdsTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-053 | V2-Phase-Q-Telemetry-audit | 2026-05-31 | Telemetry and audit framework for match contracts, Set Level, Used Level, live ECE payloads, display actions, proposed moves, settlement, and retention | None | `modules/evenchess/src/main/TelemetryAnalytics.scala`; `modules/evenchess/src/test/TelemetryAnalyticsTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-054 | V2-Phase-R-Abuse-ops | 2026-05-31 | Abuse and operations framework for trust controls, feature flags, health checks, incident pause/no-rate plans, and campaign copy kill switches | None | `modules/evenchess/src/main/AbuseTrustControls.scala`; `modules/evenchess/src/test/AbuseTrustControlsTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-055 | V2-Phase-S-Regression-hardening | 2026-05-31 | Regression-hardening framework for Lichess baseline surfaces, EvenChess core acceptance surfaces, and Phase S release gates | None | `modules/evenchess/src/main/TestingQaAcceptance.scala`; `modules/evenchess/src/test/TestingQaAcceptanceTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-056 | V2-Phase-T-Release-candidate | 2026-05-31 | Release-candidate evidence wrapper for Phase S hardening, release documents, upstream-sync safeguards, high-risk approvals, metadata, and approval-aware go/no-go | None | `modules/evenchess/src/main/ReleaseHardeningGoNoGo.scala`; `modules/evenchess/src/test/ReleaseHardeningGoNoGoTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | scoped `git diff --check`; Java unavailable for Scala test | Complete |
| INT-2026-057 | V2-Test-ground-ECE-fallback | 2026-05-31 | Local Test Ground can start/stop real ECE separately, start/stop a deterministic test ECE payload server, and fall back to test ECE when real ECE is unavailable | None | `scripts/evenchess-testground.ps1`; `scripts/evenchess-test-ece-server.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parse; Node syntax check; test ECE start/health/sample-board/stop smoke; scoped `git diff --check` | Complete |
| INT-2026-058 | V2-Test-ground-browser-panel | 2026-05-31 | Local Test Ground browser panel for WSL/Docker controls, ECE selection, EvenChess launch, ECE debug IO monitoring, health checks, sample board calls, and debugging output | None | `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `scripts/evenchess-test-ece-server.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parse; Node syntax check; panel start/status/stop smoke; scoped `git diff --check` | Complete |
| INT-2026-059 | V2-Test-ground-Docker-clean-shutdown | 2026-05-31 | Local Test Ground full cleanup now shuts down Docker Desktop before shutting down WSL and keeps cleanup available when Docker is unhealthy | None | `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parse; Node syntax check; scoped `git diff --check` | Complete |
| INT-2026-060 | V2-Test-ground-shortcut-exit | 2026-05-31 | Local Test Ground shortcut no longer keeps its PowerShell launcher window open after the browser panel starts | None | `scripts/install-evenchess-testground-shortcut.ps1`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parse; shortcut refresh smoke; scoped `git diff --check` | Complete |
| INT-2026-061 | V2-Test-ground-explicit-ECE-controls | 2026-05-31 | Local Test Ground panel exposes explicit Real ECE and Test ECE controls only, blocks starting one while the other is active, and makes panel shutdown wording distinct from stopping ECE | None | `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parse; Node syntax check; scoped `git diff --check` | Complete |
| INT-2026-062 | V2-Test-ground-ECL-ECE-bridge-smoke | 2026-05-31 | Dev-only EvenChess backend smoke endpoint calls ECE server-to-server, validates side-gated board output, compiles the Display Engine overlay payload, and exposes a Test Ground panel button for ECL overlay smoke testing | `app/controllers/EvenChess.scala`; `conf/routes` | `modules/evenchess/src/main/EceLiveBridge.scala`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`; `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md` | `PM-2026-062` | `node --check`; PowerShell parse; panel smoke on throwaway port; scoped `git diff --check`; Scala test attempted but WSL lacks `sbt` | Complete |
| INT-2026-063 | V2-Test-ground-live-round-overlay-adapter | 2026-05-31 | Local round page always shows EvenChess level/status cards, display toggles, and coach-card shell, then requests the same-origin ECL-to-ECE bridge for L10 test payloads on localhost | `ui/round/src/ctrl.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss` | `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-063` | `./ui/test round`; oxlint; TypeScript; stylelint; full UI build | Complete |
| INT-2026-064 | V2-Test-ground-asset-lifecycle-and-explicit-ECE | 2026-05-31 | Local Test Ground launch now rebuilds the full UI manifest before starting EvenChess, reports Round UI asset freshness, keeps Real/Test ECE selection explicit, and can stop discovered Test ECE processes when PID state is stale | None | `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parse; Node syntax check; full UI build; scoped `git diff --check` | Complete |
| INT-2026-065 | V2-Test-ground-Docker-host-ECE-bridge | 2026-05-31 | Dev-only ECE bridge targets the Docker-host alias so dockerized Lila can reach the developer-hosted ECE/test ECE while browser code still calls only same-origin Lichess | `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EngineGateway.scala`; `ui/round/src/evenchessTestGround.ts` | `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-064` | focused ECE endpoint-policy and round URL-construction assertions; scoped checks recorded in completion report | Complete |
| INT-2026-066 | V2-Test-ground-board-layer-visual-rendering | 2026-05-31 | Local round board layer renders approved ECE/test ECE square markers and arrows through Chessground auto-shapes, then clears/reapplies them on payload or move changes | `ui/round/src/ctrl.ts`; `ui/round/src/view/evenchessOverlay.ts` | `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-066` | focused round overlay marker/arrow assertions; scoped checks recorded in completion report | Complete |
| INT-2026-067 | V2-Test-ground-ECE-status-classification | 2026-05-31 | Local Test Ground panel distinguishes Real ECE from Test ECE independently from ECE `mode`, reports lifecycle PID state, and polls action status until start/stop button state settles | None | `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parse; Node syntax check; scoped `git diff --check` | Complete |
| INT-2026-068 | V2-Test-ground-level-ladder-fixed-coach-card | 2026-05-31 | Local round overlay renders the Set-Level-capped level ladder, per-feature display toggles, monotonic Used-Level display, and a fixed-size coach card while continuing to gate only authorized same-origin overlay payloads | `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss` | `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-068` | `./ui/test round`; oxlint; TypeScript; stylelint; scoped `git diff --check` | Complete |
| INT-2026-069 | V2-Test-ground-real-ECE-contract-alignment | 2026-05-31 | Real ECE and Test ECE public board-state payloads now normalize through the same server-side adapter into approved round overlay cards and visuals, including Docker-reachable real ECE launch and real Offset Count keys | `app/controllers/EvenChess.scala` | `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-testground-panel.js`; `scripts/evenchess-testground.ps1`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-069` | Real ECE probe; WSL reachability; ECL-to-real/test bridge smokes; L0/L10 side checks; non-fatal unavailable check; root compile/dev recompile; EvenChess backend tests; Node syntax checks | Complete |
| INT-2026-070 | V2-Test-ground-spec-style-board-overlay | 2026-05-31 | Local round board visuals now render through a pointer-transparent board-attached SVG/badge layer matching the supplied overlay recreation spec, and local feature toggles immediately suppress their matching available overlay families | `app/controllers/EvenChess.scala`; `ui/round/src/view/main.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss` | `modules/evenchess/src/main/EceLiveBridge.scala`; `scripts/evenchess-testground-panel.js`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-070` | round overlay test; real-ECE-style feature-gating regression; round TypeScript; oxfmt; stylelint; root compile; EvenChess backend tests; Node syntax checks | Complete |
| INT-2026-071 | V2-Test-ground-Linux-ECE-lifecycle | 2026-06-01 | Local Test Ground real ECE launch/stop/monitoring now uses the Linux/WSL ECE repo bash lifecycle scripts and reads the Linux debug IO log/PID through WSL paths | None | `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md` | None | PowerShell parse; Node syntax check; Linux ECE script presence; mock ECE debug cycle; real ECE start attempt exposed ECE-side service exit | Complete |
| INT-2026-072 | V2-Test-ground-round-card-layout-eval-controls | 2026-06-01 | Native round layout places the granular level control card left of the eval bar and board, keeps the coach card right of the board, and gates each available ECE payload family by its own local display toggle | `ui/round/src/interfaces.ts`; `ui/round/src/view/main.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_app-layout.scss`; `ui/round/css/_evenchess-live.scss` | `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-072` | `node ui/test evenchessOverlay.test.ts`; round TypeScript/style checks; browser layout smoke | Complete |
| INT-2026-074 | V2-lobby-computer-native-flow-correction | 2026-06-01 | Lobby start cards keep native labels; computer games keep native Lichess AI setup while EvenChess Set Level is forced to L10 internally | `ui/lobby/src/view/table.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/setupCtrl.ts` | `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-041` | `node ui/test lobby/tests/evenchessSetup.test.ts`; lobby TypeScript; oxfmt; oxlint; full UI build; browser lobby/modal smoke | Complete |
| INT-2026-075 | V2-Test-ground-real-ECE-bind-host | 2026-06-01 | Test Ground passes `ECE_HOST=0.0.0.0` when launching Linux real ECE so Windows and Docker can reach the service through `127.0.0.1` / `host.docker.internal` | None | `scripts/evenchess-testground.ps1`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parser check; Test Ground health/sample-board against manually detached real ECE | Complete |
| INT-2026-077 | V1.1-Phase-C-Bot-matchmaking-fill | 2026-06-01 | Timed bot fallback seeding activates after the configured threshold when queue scope matches and matchmaking settings allow it; public search JSON is later narrowed to deployment-safe status labels | `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `ui/lobby/src/interfaces.ts` | `modules/evenchess/src/test/PlaySearchIntegrationTest.scala` | `PM-2026-077` | `PlaySearchIntegrationTest`; `node --check ui/lobby/src/interfaces.ts`; and a focused local search-json poll smoke (local test environment currently cannot run `sbt` in this session). | Complete |
| INT-2026-079 | V1.1-Phase-G-Contract-disclosure-and-status | 2026-06-01 | Search status carries deployment-safe bot disclosure and wait/status labels while detailed bot/source diagnostics stay server/admin-side | `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/table.ts` | `modules/evenchess/src/test/PlaySearchIntegrationTest.scala` | `PM-2026-078` | `PlaySearchIntegrationTest`; focused status-payload schema/field verification pending local `sbt` availability | Complete |
| INT-2026-080 | V1.1-Phase-I-ECE-bridge-verification | 2026-06-01 | Human, matchmaking-bot, and simulation-bot game contexts now prepare the same server-only ECE quick/deep board-state request path, with stale/leak rejection preserved | None | `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `docs/requirements/plan_version_1.1/PHASE_I_ECE_BRIDGE_VERIFICATION.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | `EngineGatewayTest`; Test ECE quick/deep fixture smoke via `scripts/evenchess-test-ece-server.test.mjs`; focused local checks recorded in completion report | Complete |
| INT-2026-081 | Analysis-memory-retention-and-overlay-policy | 2026-06-01 | Analysis memory framework retains last 10 live-history games and last 100 requested full-game/custom analyses per user while reusing one mode-neutral overlay shell policy | None | `modules/evenchess/src/main/AnalysisMemory.scala`; `modules/evenchess/src/test/AnalysisMemoryTest.scala`; `docs/requirements/plan_analysis_memory/PLAN.md`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | `AnalysisMemoryTest` added; local execution blocked by missing `sbt`; scoped static checks recorded in completion report | Complete |
| INT-2026-084 | V2-Round-overlay-safety-marker-correction | 2026-06-01 | Round overlay requests now queue a fresh ECE request after move-time in-flight requests, retry accepted visual-empty payloads, controls use round-safe `bind` hooks, and real/test ECE safety markers normalize into separate loose orange and hanging purple board families with tolerant offset/pin parsing | `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/css/_evenchess-live.scss` | `scripts/evenchess-test-ece-server.js`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-084` | `node ui/test round/tests/evenchessOverlay.test.ts round/tests/evenchessTestGround.test.ts`; `CI=true ./ui/build -n -k`; browser toggle smoke | Complete |
| INT-2026-093 | V2-Test-ground-ECE-CLM-launch | 2026-06-02 | Local Test Ground can launch/open/stop the private ECE Composer Learning Model while keeping CLM separate from live ECE and avoiding the former panel/CLM port collision | None | `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parser check; Node syntax check; panel ping/status smoke | Complete |
| INT-2026-107 | V2-server-side-proposed-potential-consumables | 2026-06-02 | Proposed and potential move consumables are enforced by same-origin ECL server endpoints so browser refresh cannot reset usage, and potential move data is returned only after an authorized reveal | `app/controllers/EvenChess.scala`; `conf/routes`; `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss` | `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-103` | focused round/Test Ground tests; route generation; full UI build; root Scala compile via Docker sbt fallback | Complete |
| INT-2026-115 | V2-ECE-mirrored-offset-count | 2026-06-03 | Offset Count side-output mirrored board facts and signed value priority | `app/controllers/EvenChess.scala` | `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-111` | Test ECE fixture test; round overlay test; scoped diff check | Complete |
| INT-2026-116 | V2-preferred-level-submit-normalization | 2026-06-03 | Lobby search submit omits Preferred Set Level for Any/empty stale values and only sends concrete L0-L10 | `ui/lobby/src/setupCtrl.ts` | `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-112` | focused lobby setup helper test; scoped diff check | Complete |
| INT-2026-120 | V2-deep-only-stockfish-eval-display | 2026-06-03 | Eval bar and coach eval strip ignore quick placeholder eval and update only from accepted deep/advanced ECE eval | `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts` | `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-116` | focused round overlay test; compile check | Complete |
| INT-2026-121 | V2-proposed-move-post-move-side-output-preview | 2026-06-03 | Proposed-move preview reads legal nested `proposed_move_evaluation.after_move_side_output` and toggles cached post-move cards/visuals against the normal board-state payload | `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `ui/round/src/view/evenchessOverlay.ts` | `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-117` | focused round overlay test; compile check | Complete |
| INT-2026-122 | V2-Test-ground-ECE-settings-page-open | 2026-06-03 | Local Test Ground can open the private read-only ECE operator settings page at `{ECE_BASE_URL}/ece/settings` without adding browser analysis calls | None | `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | None | PowerShell parser check; Node syntax check | Complete |
| INT-2026-123 | V2-stable-live-overlay-refresh-during-ECE-loads | 2026-06-03 | Move-triggered ECE refresh keeps stable live overlay DOM and last safe coach text while stale board visuals stay hidden until the next current-position payload | `ui/round/src/view/evenchessOverlay.ts` | `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-118` | focused round overlay test; UI build | Complete |
| INT-2026-124 | V2-potential-proposed-move-side-state-correction | 2026-06-03 | Opponent potential reveals use opponent-side perspective and invalid proposed-move clicks preserve the active overlay state | `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts` | `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-119` | focused round overlay test; compile check; UI build | Complete |
| INT-2026-130 | V2-ECL-multi-user-load-harness | 2026-06-04 | ECL-side load harness exercises current same-origin Test Ground ECL-to-ECE bridge, replay cache lookups, proposed/potential bridge calls, and captures ECE metrics for launch hardening | `package.json` | `scripts/evenchess-ecl-load-harness.js`; `scripts/evenchess-ecl-load-harness.test.mjs`; `docs/evenchess/EVENCHESS_ECL_MULTI_USER_LOAD_HARNESS.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-125` | Node harness self-test; syntax check | Complete |
| INT-2026-142 | V2-live-L6-WikiBook-fieldset | 2026-06-04 | Live games expose the existing Lichess analysis-board WikiBook fieldset as a Level 6 feature above the level selector, derived from round SAN history | `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/build/round.scss`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss` | `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-137` | focused round overlay test; UI build | Complete |
| INT-2026-143 | V2-fixed-perspective-stockfish-eval-stabilization | 2026-06-05 | Eval bar/coach strip prefer structured ECE fixed White-positive eval values, use nonlinear winning-chances-style bar mapping, and retain the last accepted eval until live or proposed payloads supply a replacement | `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `modules/evenchess/src/main/LiveBoardIntegration.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss` | `modules/evenchess/src/test/EceLiveBridgeTest.scala`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-138` | focused bridge/round overlay tests; compile/UI checks | Complete |
| INT-2026-144 | V2-roster-backed-bot-account-provisioning-and-runner | 2026-06-05 | Bot operations can create/check the shared roster as normal local/staging accounts; matched roster-backed bots are registered with an ECL-managed legal-move runner; simulation seeds center-out same-pool cohorts, prioritizes human searches, and can pump throttled sim-vs-sim contracts into normal roster-account games | `app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/web/src/main/ui/DevUi.scala` | `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/requirements/plan_version_1.1/PHASE_A_REQUIREMENTS_LOCK.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` | `PM-2026-139` | bot/search tests; compile; browser matchmaking and simulation search verification | Complete |

## Detailed Entries

### INT-2026-026 - Native Lichess integration plan public shell narrowing

- Phase: Native-Lichess-Integration-B/D.
- Lichess seam: Homepage render path, top navigation, and native-looking EvenChess route pages.
- Lichess files touched: `app/http/KeyPages.scala`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`; `app/views/evenchess/play.scala`; `app/views/evenchess/account.scala`.
- EvenChess files touched: `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`; `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_3_COMPLETION_REPORT.md`.
- Why this seam exists: Version 1.3 narrowed the broader Version 1.1/1.2 public-shell takeover. The homepage and public shell should stay structurally Lichess while preserving explicit EvenChess identity and route entrypoints.
- Public UX effect: Root/homepage behavior returns to the Lichess lobby shape; EvenChess play/account route pages stay available and visually restrained.
- Preserved Lichess capability: Normal lobby structure, route discoverability, study/opening/analysis routes, account foundations, and existing game internals remain available.
- Patch map entry: `PM-2026-013`.
- Tests / checks: `PublicShellTest`; `evenchess/test`; root compile; route smoke for `/`, `/study`, `/analysis`, `/opening`, `/evenchess/play`, and account/admin redirects.
- Upstream update notes: Reapply this before later visible-shell changes. Prefer upstream `KeyPages`, `home.scala`, and `TopNav.scala` first, then reapply only the small EvenChess route/callout/wording changes recorded here.
- Rollback notes: Revert the shell/page edits from `PM-2026-013`; keep module contracts and settings/admin/overlay logic unless product ownership explicitly removes EvenChess mode.

### INT-2026-027 - Native Lichess integration plan wordmark

- Phase: Native-Lichess-Integration-B.
- Lichess seam: Shared page layout/header wordmark.
- Lichess files touched: `modules/web/src/main/ui/layout.scala`.
- EvenChess files touched: None.
- Why this seam exists: The visible product name is EvenChess while the shell remains Lichess-native.
- Public UX effect: The global header identifies the fork as EvenChess without changing chess behavior.
- Preserved Lichess capability: Shared layout structure, navigation behavior, route behavior, games, ratings, study/opening/analysis, and account flows remain unchanged.
- Patch map entry: `PM-2026-014`.
- Tests / checks: root compile; homepage smoke.
- Upstream update notes: If upstream changes `layout.scala`, keep the upstream header structure and reapply only the wordmark text or move it to a config seam if one exists.
- Rollback notes: Restore the upstream wordmark only if product ownership changes the visible brand.

### INT-2026-028 - Native Lichess integration plan primary Play navigation

- Phase: Native-Lichess-Integration-B/C.
- Lichess seam: Shared top navigation primary Play target.
- Lichess files touched: `modules/web/src/main/ui/TopNav.scala`.
- EvenChess files touched: `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`.
- Why this seam exists: Public Play must start EvenChess search/setup rather than ordinary Lichess rated pools, while retaining the native Lichess navigation structure.
- Public UX effect: Users see Lichess-style navigation, but the primary Play path is EvenChess-owned.
- Preserved Lichess capability: Non-play navigation sections remain available; this does not alter game rules, clocks, study/opening/analysis, or rating internals.
- Patch map entry: `PM-2026-015`.
- Tests / checks: `PublicShellTest`; root compile; homepage/nav smoke.
- Upstream update notes: When upstream changes `TopNav.scala`, keep upstream menu grouping and reapply only the primary Play target/EvenChess label behavior.
- Rollback notes: Revert only the Play-target change if product ownership later exposes normal public Lichess play.

### INT-2026-029 - Current Version 1 native setup modal integration

- Phase: Current-Version-1-Native-Setup.
- Lichess seam: Lobby table/start controls, setup modal, setup controller, route/controller adapter, and lobby status styling.
- Lichess files touched: `app/controllers/EvenChess.scala`; `conf/routes`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`; `ui/lobby/src/view/table.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/interfaces.ts`; `ui/lobby/css/_setup.scss`; `ui/lobby/css/_table.scss`.
- EvenChess files touched: `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PublicShellTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`.
- Why this seam exists: Current Version 1 direction says EvenChess should feel like Lichess through public play. The native Lichess setup modal carries EvenChess Set Level/Target/disclosure controls and submits to the EvenChess search contract.
- Public UX effect: Users click Play, configure a game in the native modal shape, and start an EvenChess search intent instead of going to a separate search page.
- Preserved Lichess capability: Lichess still owns the lobby UI frame, legal move generation, board UI, clocks, game lifecycle, study/opening/analysis, and normal internals.
- Patch map entry: `PM-2026-016`.
- Tests / checks: `PublicShellTest`; `PlaySearchIntegrationTest`; lobby TypeScript/build/style checks; root compile; route smoke.
- Upstream update notes: Reapply by restoring upstream lobby/setup modal code first, then reinsert only the EvenChess fields, submit adapter, and status rendering. Keep policy/search meaning in `modules/evenchess`.
- Rollback notes: Remove setup modal EvenChess fields and submit adapter if product ownership returns to namespaced `/evenchess/play/search`; keep route/controller contracts until the replacement is ready.

### INT-2026-030 - Native Lichess integration plan phase A direction reset

- Phase: Native-Lichess-Integration-A.
- Lichess seam: Requirements govenance only.
- Lichess files touched: None.
- EvenChess files touched: Requirements and integration docs listed above.
- Why this seam exists: Codex must lock and reuse the native Lichess public-flow strategy before proceeding with deeper technical phases.
- Public UX effect: No product behavior is changed by this phase; it records that play/search should stay on Lichess-native surfaces (lobby/setup modal + setup flow), with EvenChess as an explicit assisted-mode layer.
- Preserved Lichess capability: Legal move generation, board UI, lobby/game lifecycle, studies/openings/analysis surfaces, accounts, and admin foundations remain under Lichess control unless a later phase adds explicit seams.
- Patch map entry: None. No upstream/core source edits in this phase.
- Tests / checks: `git diff --check` (docs-only sanity).
- Upstream update notes: Keep this entry and Appendix Z override records as the first reapplication point for this direction before touching any upstream `Home`, `TopNav`, lobby, or setup files in future phases.
- Rollback notes: If strategy changes, supersede this entry in a new phase log and update Appendix Z with a follow-on override.

### INT-2026-032 - Native Lichess integration plan phase C navigation restoration

- Phase: Native-Lichess-Integration-C.
- Lichess seam: Top navigation and public root shell behavior.
- Lichess files touched: `modules/web/src/main/ui/TopNav.scala`.
- EvenChess files touched: None.
- Why this seam exists: Play must remain inside the Lichess shell while routing the Play entrypoint to the native EvenChess flow (`/?any#hook`) and replacing legacy root label text.
- Public UX effect: Primary nav keeps Lichess structure, restores standard feature sections, and makes the Play surface point at EvenChess-mapped setup flow instead of a plain Lichess homepage.
- Preserved Lichess capability: Puzzle, watch, lean, community, and tools sections are still exposed for immediate reuse.
- Patch map entry: Referenced by `PM-2026-015` and `PM-2026-016`.
- Tests / checks: `./bin/sbt testOnly lila.evenchess.PublicShellTest` (environment not available in this shell today) and route/UI smoke around `/`, `/account`, `/lean`, `/analysis`.
- Upstream update notes: Keep this entry with `PM-2026-015` and `PM-2026-016` so future reapplication preserves nav-level compatibility instead of full-shell rewrites.
- Rollback notes: Revert `TopNav.scala` edit and reapply under dedicated extension seam if available.

### INT-2026-033 - Native Lichess integration plan phase C namespaced routes

- Phase: Native-Lichess-Integration-C (namespaced entrypoints).
- Lichess seam: Route surface and discovery intent.
- Lichess files touched: `conf/routes`.
- EvenChess files touched: `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`.
- Why this seam exists: EvenChess gameplay/search must stay intentionally explicit and namespaced (`/evenchess/*`) so it is clear this is an assisted-mode layer, not a replacement of normal Lichess route behavior.
- Public UX effect: `/evenchess/play`, `/evenchess/play/search`, `/evenchess/play/search.json`, and `/evenchess/account` remain the EvenChess entrypoints while core Lichess routes stay unchanged.
- Preserved Lichess capability: Top nav and normal route table paths remain Lichess-led; legal move generation, board UI, clocks, lifecycle, studies/openings/analysis, and account surfaces continue to be Lichess-owned.
- Patch map entry: Referenced by `PM-2026-010` and `PM-2026-016`.
- Tests / checks: `./bin/sbt testOnly lila.evenchess.PublicShellTest` and route smoke for `/evenchess/play`, `/evenchess/play/search`, `/evenchess/account`.
- Upstream update notes: Reapply phase C by checking `PlaySearchIntegration.Routes` and `PublicShell.PublicRoutes` first, then keep only the tiny top-nav/endpoint seam in `TopNav`.
- Rollback notes: Revert routes/controllers tests if product direction is changed to a different entrypoint model.

### INT-2026-034 - Native Lichess integration plan phase F backend safety hardening

- Phase: Native-Lichess-Integration-F.
- Lichess seam: Admin settings save endpoint and EvenChess backend snapshot safety.
- Lichess files touched: `app/controllers/Dev.scala`.
- EvenChess files touched: `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`.
- Why this seam exists: EvenChess backend configuration must remain secret-safe while still being admin-editable. This phase blocks raw-seeming secret strings from being persisted through `/dev/settings` and keeps audit logs redacted.
- Public UX effect: Backend settings still editable in `/dev/settings`, but secret-shaped inputs on EvenChess namespace settings are rejected with a failure flash.
- Preserved Lichess capability: Lichess settings framework, `Settings` admin permission flow, and setting persistence path remain unchanged.
- Patch map entry: `PM-2026-017`.
- Tests / checks: `./bin/sbt testOnly lila.evenchess.AdminBackendSettingsTest`; route smoke for `/dev/settings` in existing PublicShell/Dev checks.
- Upstream update notes: Reapply by keeping the same save-guard at the `Dev.settingsPost` seam and keeping `AdminBackendSettings` secret markers + safe-log helpers aligned.
- Rollback notes: If admin tooling is migrated to a dedicated EvenChess config surface, remove this guard from `Dev` and add equivalent validation inside that surface.

### INT-2026-035 - Native Lichess integration plan phase G live overlay hardening

- Phase: Native-Lichess-Integration-G.
- Lichess seam: Round live-overlay receive and render safety.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: None.
- Why this seam exists: The project requirement is to keep round overlays inert for normal games, and this phase narrows acceptance at payload ingress by immediately stale/clearing non-matching-game overlays.
- Public UX effect: There is no visual or gameplay change for normal games; EvenChess round overlays only render when the incoming payload is authorized and game-matched.
- Preserved Lichess capability: Round lifecycle, move validation, clocks, clocks, premove handling, and board UI are unchanged.
- Patch map entry: `PM-2026-018`.
- Tests / checks: `./ui/test round/tests/evenchessOverlay.test.ts`.
- Upstream update notes: This hardening should be reapplied whenever Lichess changes the round socket event adapter path. Keep one socket event hook and one render hook in `ui/round/src/view/evenchessOverlay.ts` and `ui/round/src/socket.ts`.
- Rollback notes: Revert the mismatch clear branch in `applyEvenChessLiveOverlay` only; keep other V1.3 guardrails unless explicitly removed for transport compatibility work.

### INT-2026-036 - Native Lichess integration plan phase H opening stale clear

- Phase: Native-Lichess-Integration-H.
- Lichess seam: Opening explorer AI adaptor lifecycle.
- Lichess files touched: `ui/opening/src/evenchessOpeningAi.ts`.
- EvenChess files touched: `ui/opening/tests/evenchessOpeningAi.test.ts`.
- Why this seam exists: Phase H requires learning overlays to be optional and server-authorized. This phase removes a stale overlay retention path where old opening cards could remain if a prior valid payload is replaced by an invalid/missing payload.
- Public UX effect: Normal opening pages do not show EvenChess AI cards unless the current payload is valid and authorized.
- Preserved Lichess capability: Opening explorer structure and study/analysis integrations remain unchanged.
- Patch map entry: `PM-2026-036`.
- Tests / checks: `./ui/test opening/tests/evenchessOpeningAi.test.ts`.
- Upstream update notes: Reapply stale-clearing before early return after any future opening-adapter refactor.
- Rollback notes: If the opening feature gets a stable extension hook, remove this direct DOM clear/render path and move both checks into the extension seam.

### INT-2026-037 - Native Lichess integration plan phase I TTS overlay-only audit gate

- Phase: Native-Lichess-Integration-I.
- Lichess seam: Shared browser TTS adapter used by EvenChess overlay buttons.
- Lichess files touched: `ui/lib/src/evenchessTts.ts`; `ui/lib/tests/evenchessTts.test.ts`.
- EvenChess files touched: `modules/evenchess/src/main/TtsCoach.scala`; `modules/evenchess/src/test/TtsCoachTest.scala`.
- Why this seam exists: Phase I requires TTS to read only visible, authorized overlay text and not become a separate unaudited advice channel. The prior live/rated-only audit check is tightened so every surface, including analysis, study, and opening overlays, must have the originating overlay audit id.
- Public UX effect: Read-aloud buttons still appear only on valid EvenChess overlay cards. Cards without audit identity do not offer or allow TTS playback.
- Preserved Lichess capability: Browser speech is only invoked by EvenChess overlay buttons. Lichess study, analysis, opening, live game, board, clock, and game lifecycle behavior are unchanged without EvenChess payloads.
- Patch map entry: `PM-2026-037`.
- Tests / checks: `./ui/test lib/tests/evenchessTts.test.ts`; `./ui/test round/tests/evenchessOverlay.test.ts`; `./ui/test analyse/tests/evenchessLearning.test.ts`; `./ui/test opening/tests/evenchessOpeningAi.test.ts`; container `evenchess/testOnly lila.evenchess.TtsCoachTest`.
- Upstream update notes: Reapply the all-surfaces audit requirement if `ui/lib/src/evenchessTts.ts` or the EvenChess server TTS policy is refactored. Keep the client and server policies aligned.
- Rollback notes: Revert only the all-surfaces audit requirement if product ownership explicitly approves unaudited learning-surface TTS; live/rated TTS audit identity must remain required.

### INT-2026-038 - Native Lichess integration plan phase J ledger reconciliation

- Phase: Native-Lichess-Integration-J.
- Lichess seam: Documentation records only.
- Lichess files touched: None.
- EvenChess files touched: `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md`.
- Why this seam exists: Phase J requires every narrowed upstream seam to be patch-mapped and ledgered so future upstream updates can reapply EvenChess without rediscovering intent.
- Public UX effect: None.
- Preserved Lichess capability: No runtime behavior changed.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; manual ID audit for `PM-2026-013` through `PM-2026-018`, `PM-2026-036`, `PM-2026-037`, and their integration-log links.
- Upstream update notes: Start future sync work with the reconciliation table below, then open each linked patch-map entry before editing upstream files.
- Rollback notes: If the records become stale, supersede this entry with a new reconciliation entry rather than deleting historical seam records.

### INT-2026-039 - V2 Phase C homepage branding and lobby theme summary

- Phase: V2-Phase-C-Branding-theme.
- Lichess seam: Native homepage metadata, anonymous about-side copy, compact homepage summary, and lobby stylesheet.
- Lichess files touched: `app/views/lobby/home.scala`; `ui/lobby/css/_lobby.scss`.
- EvenChess files touched: `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`.
- Why this seam exists: Version 2 Phase C requires the public shell to say EvenChess, keep Lichess-style navigation/layout, use a polished deep-blue theme treatment, and explain that platform coaching is disclosed, capped by Set Level, logged, and reflected in ECR.
- Public UX effect: The root page uses EvenChess-branded title/OpenGraph copy, replaces anonymous Lichess marketing text with EvenChess/source attribution copy, and adds a compact "What is EvenChess?" summary above the lobby table.
- Preserved Lichess capability: Board, clocks, setup modal, lobby table, homepage layout, legal move generation, game lifecycle, and compatible feature areas remain Lichess-owned.
- Patch map entry: `PM-2026-039`.
- Tests / checks: scoped `git diff --check`; manual homepage/CSS seam review; attempted `./lila.sh "evenchess/testOnly lila.evenchess.PublicShellTest"` but local WSL environment has no Java.
- Upstream update notes: Reapply after accepting upstream changes to `app/views/lobby/home.scala` or `ui/lobby/css/_lobby.scss`; keep the reusable copy in `PublicShell.PublicCopy`.
- Rollback notes: Revert `PM-2026-039` and remove this entry if Phase C branding moves to a dedicated extension hook.

### INT-2026-040 - V2 Phase D top bar token and account/settings shell

- Phase: V2-Phase-D-Top-bar-tokens-settings-shell.
- Lichess seam: Shared header buttons, account menu navigation, and EvenChess preference shell copy.
- Lichess files touched: `modules/web/src/main/ui/layout.scala`; `ui/lib/css/header/_buttons.scss`; `modules/pref/src/main/ui/AccountUi.scala`; `modules/pref/src/main/ui/AccountPref.scala`.
- EvenChess files touched: `modules/evenchess/src/main/AccountMonetisationUi.scala`; `modules/evenchess/src/test/AccountMonetisationUiTest.scala`.
- Why this seam exists: Version 2 Phase D requires game-token balances to integrate into a top-bar/account-native surface, clicking the game-token balance to route to the token/ad screen, and EvenChess settings to remain inside the native Lichess preferences shell.
- Public UX effect: Authenticated users see a compact EvenChess game-token chip in the top bar that links to `/evenchess/account#rewarded-ads`; account navigation exposes "Tokens & plans"; EvenChess preferences link back to the account token/plan shell.
- Preserved Lichess capability: Header notifications, dasher, account pages, preferences autosave, board, clocks, game lifecycle, and normal account security flows remain Lichess-owned.
- Patch map entry: `PM-2026-040`.
- Tests / checks: scoped `git diff --check`; manual header/account/settings seam review; attempted `./lila.sh "evenchess/testOnly lila.evenchess.AccountMonetisationUiTest"` but local WSL environment has no Java.
- Upstream update notes: Reapply after accepting upstream changes to shared header buttons or account preference menus. Keep token-display meaning in `AccountMonetisationUi.TopBarGameTokenBalance`.
- Rollback notes: Revert `PM-2026-040` if token display moves to a dedicated extension hook; keep the namespaced account route until a replacement token/ad screen exists.

### INT-2026-041 - V2 Phase E native setup/search UI controls

- Phase: V2-Phase-E-Setup-search-UI.
- Lichess seam: Native lobby setup modal, setup controller/store, search submit params, token-gate display, and lobby search-status echo.
- Lichess files touched: `app/views/lobby/home.scala`; `app/controllers/EvenChess.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/view/table.ts`; `ui/lobby/css/_setup.scss`.
- EvenChess files touched: `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`.
- Why this seam exists: Version 2 Phase E requires setup/search to stay in native Lichess-style modal/cards while collecting Apply preferences, optional player/opponent target levels, strict-search preference, disclosure, and token/subscription gate state for EvenChess search intent.
- Public UX effect: The setup modal now keeps normal rated/casual search preference-free by default, reveals Any/L0-L10 Player Target Level and Opponent Target Level controls only when Apply preferences is checked, offers strict preference search only for those preferences, shows token-gate state, and echoes target/search details in the EvenChess search-status card.
- Preserved Lichess capability: Native time controls, variant controls, rating controls, modal shell, lobby table, and no-game-created search-start status remain Lichess-owned.
- Patch map entry: `PM-2026-041`.
- Tests / checks: `node ui/test lobby/tests/evenchessSetup.test.ts`; `evenchess/testOnly lila.evenchess.LevelBasedMatchmakingTest lila.evenchess.PlaySearchIntegrationTest` via Docker fallback.
- Upstream update notes: Reapply after upstream changes to lobby setup modal/controller/table files. Keep scenario/level/token-gate wording in `ui/lobby/src/evenchessSetup.ts` and EvenChess search parsing in `PlaySearchIntegration`.
- Rollback notes: Revert `PM-2026-041` if setup/search moves to a dedicated extension hook; leave backend matchmaking untouched unless a later phase explicitly implements it.

### INT-2026-042 - V2 Phase F MMR engine framework

- Phase: V2-Phase-F-MMR-engine-framework.
- Lichess seam: Documentation record only; no live lila pairing seam is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase F requires an EvenChess-owned matchmaking engine framework that can produce auditable match contracts while keeping normal Lichess pools and rating internals separate. Rated/casual search now evaluates assigned Set Level combinations from L0-L10 according to active target preferences rather than treating the UI default Set Level as fixed.
- Public UX effect: None. This phase adds framework contracts and simulation behavior only; it does not start live pairing or game creation.
- Preserved Lichess capability: Native Lichess lobby, pool pairing, board, clocks, lifecycle, and normal ratings remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: `evenchess/testOnly lila.evenchess.LevelBasedMatchmakingTest lila.evenchess.PlaySearchIntegrationTest` via Docker fallback.
- Upstream update notes: Future live integration should call `MmrEngine` from a narrow adapter and add a patch-map entry for that seam when it touches lila pairing/search code.
- Rollback notes: Remove the Phase F additions in `LevelBasedMatchmaking` and its tests; no upstream files need rollback for this phase.

### INT-2026-043 - V2 Phase G matchmaking integration

- Phase: V2-Phase-G-Matchmaking-integration.
- Lichess seam: Authenticated EvenChess search JSON endpoint; no live lila pairing/game-creation seam is wired in this phase.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`.
- Why this seam exists: Version 2 Phase G requires prepared EvenChess search records to flow into the MMR Engine rather than ordinary Lichess public pools, producing a contract that a later thin lila adapter can consume. Applying preferences no longer reroutes rated/casual hook searches to the separate Target Level queue.
- Public UX effect: Search JSON now includes MMR matchmaking status and a contract when a queued candidate is found; applied rated/casual target preferences stay in the selected queue and constrain only assigned Set Level combinations. It still does not create a game.
- Preserved Lichess capability: Native Lichess lobby, public pools, board, clocks, lifecycle, and normal ratings remain unchanged.
- Patch map entry: `PM-2026-043`.
- Tests / checks: `evenchess/testOnly lila.evenchess.LevelBasedMatchmakingTest lila.evenchess.PlaySearchIntegrationTest` via Docker fallback; added PlaySearchIntegration tests for Apply preferences parsing, strict waiting, widened non-strict contracts, rated/casual queue preservation, and no coaching render before game policy.
- Upstream update notes: When a later phase wires live game creation, call `MatchmakingIntegrationService` from a single patch-mapped lila adapter and hand off only the finalized `MatchContract`.
- Rollback notes: Remove the Phase G additions in `PlaySearchIntegration` and its tests; no upstream files need rollback for this phase.

### INT-2026-044 - V2 Phase H ECE framework integration

- Phase: V2-Phase-H-ECE-framework-integration.
- Lichess seam: Documentation record only; no browser, live round, or game-lifecycle ECE call is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase H requires EvenChess-Lichess to treat ECE as a separate private server-side service with health and board-state contracts, side-gated outputs, stale rejection, and graceful unavailable behavior.
- Public UX effect: None. This phase adds framework validation only; it does not call ECE from a live game or expose ECE details to the browser.
- Preserved Lichess capability: Native Lichess page loading, board, legal moves, clocks, lifecycle, and normal ratings remain unchanged if ECE is unavailable.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added EngineGateway tests for server-only ECE policy, local endpoints, board-state request shape, side-gated response validation, stale rejection, and non-fatal degraded state.
- Upstream update notes: Future live wiring should call the ECE framework from a narrow server-side adapter and patch-map any touched round/game lifecycle seam.
- Rollback notes: Remove the Phase H additions in `EngineGateway` and its tests; no upstream files need rollback for this phase.

### INT-2026-045 - V2 Phase I display engine framework

- Phase: V2-Phase-I-Display-engine-framework.
- Lichess seam: Documentation record only; no browser overlay renderer, round socket, or live ECE transport seam is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/CoachingOverlays.scala`; `modules/evenchess/src/test/CoachingOverlaysTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase I requires a Display Engine framework that can compile approved side-specific/mock ECE output into level-gated cards, square markers, arrows, and clear-only decisions before any live browser renderer is wired.
- Public UX effect: None. This phase adds server-side display compilation rules only; existing browser overlays are not changed.
- Preserved Lichess capability: Native Lichess board, move input, clocks, legal move handling, and round lifecycle remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added CoachingOverlays tests for display requirement classification, side-only rendering, stale/expired/mismatch clearing, level-gated cards/markers/arrows, text budgets, and actual-vs-proposed labels.
- Upstream update notes: Future browser rendering should consume this compiled display result from a narrow patch-mapped round/overlay adapter and avoid client-side chess interpretation.
- Rollback notes: Remove the Phase I additions in `CoachingOverlays` and its tests; no upstream files need rollback for this phase.

### INT-2026-046 - V2 Phase J live ECE history

- Phase: V2-Phase-J-Live-ECE-history.
- Lichess seam: Documentation record only; no database, round socket, live ECE call, or review UI seam is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/LiveCoaching.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase J requires live ECE history to retain per-ply FEN/output metadata, requested and delivered levels, policy/ECE versions, output references, and audit atoms while making the highest delivered level canonical per side.
- Public UX effect: None. This phase adds server-side history data structures only; it does not display review history or call ECE.
- Preserved Lichess capability: Native Lichess board, legal move handling, clocks, game lifecycle, and normal ratings remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added LiveCoaching tests for ECE scheduling metadata, per-ply history validity, higher-level canonical selection, monotonic used levels, limited review essentials, and raw-retention rejection.
- Upstream update notes: Future persistence/review phases should store and read this history from a narrow repository or adapter and patch-map any touched lila game/review seam.
- Rollback notes: Remove the Phase J additions in `LiveCoaching` and its tests; no upstream files need rollback for this phase.

### INT-2026-047 - V2 Phase K proposed move

- Phase: V2-Phase-K-Proposed-move.
- Lichess seam: Documentation record only; no browser proposed-move UI, move validation hook, live round socket, or real ECE HTTP call is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/main/LiveCoaching.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase K requires proposed-move mode to send exactly one proposed UCI move with current FEN and server-authorized levels, accept output only for the side to move, and keep the result preview-only rather than actual-position history.
- Public UX effect: None. This phase adds server-side proposed-move contracts and scheduling rules only.
- Preserved Lichess capability: Native legal move validation, move input, board, clocks, lifecycle, and normal ratings remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added EngineGateway and LiveCoaching tests for single-move UCI validation, current-FEN echo checks, requester side-to-move gating, policy permission, side-gated output, and preview-only decisions.
- Upstream update notes: Future UI/live phases should call this through a narrow server-side adapter and patch-map any touched move-input or round transport seam.
- Rollback notes: Remove the Phase K additions in `EngineGateway`, `LiveCoaching`, and their tests; no upstream files need rollback for this phase.

### INT-2026-048 - V2 Phase L review modes

- Phase: V2-Phase-L-Review-modes.
- Lichess seam: Documentation record only; no analysis UI, review route, database, token ledger, or ECE HTTP call is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/LiveCoaching.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase L requires review modes to read saved live ECE history for Live White, Live Black, and Live Both, and to define a custom review mode with selectable side levels, perspective, cache identity, and token requirement intent.
- Public UX effect: None. This phase adds server-side review-mode selection and planning contracts only.
- Preserved Lichess capability: Native Lichess analysis, replay, study, board interaction, normal ratings, and completed-game views remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added LiveCoaching tests for Phase L requirement classification, saved-history live review side selection, no custom-token spend for live modes, custom cache keys, token intent, and no live fairness mutation.
- Upstream update notes: Future analysis/review UI phases should consume these contracts through a narrow patch-mapped analysis/replay adapter and keep token spending in the later token/full-game phases.
- Rollback notes: Remove the Phase L additions in `LiveCoaching` and its tests; no upstream files need rollback for this phase.

### INT-2026-049 - V2 Phase M full-game ECE

- Phase: V2-Phase-M-Full-game-ECE.
- Lichess seam: Documentation record only; no analysis UI, review route, database repository, token ledger mutation, or real ECE HTTP call is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/main/LiveCoaching.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase M requires a full-game ECE request shape for post-game Match History review, including whole-game input, review level, optional one-call AI narrative allowance, live ECE snapshot references, token/quota gate status, and output validation that cannot alter the rated game.
- Public UX effect: None. This phase adds server-side full-game review contracts and planning rules only.
- Preserved Lichess capability: Native Lichess analysis, replay, study, board interaction, game result authority, normal ratings, and completed-game views remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added EngineGateway and LiveCoaching tests for game-review endpoint/request shape, token-gated preparation, saved snapshot handoff, safe response acceptance, raw-data rejection, and no mutation of result, live Used Level, Assistance Load, Used Offset, ECR, or matchmaking state.
- Upstream update notes: Future review UI/token phases should consume this through a narrow patch-mapped analysis/replay adapter and perform actual ledger mutation in the monetisation/token layer, not in the ECE payload adapter.
- Rollback notes: Remove the Phase M additions in `EngineGateway`, `LiveCoaching`, and their tests; no upstream files need rollback for this phase.

### INT-2026-050 - V2 Phase N Stockfish/AI

- Phase: V2-Phase-N-Stockfish-AI.
- Lichess seam: Documentation record only; no direct Stockfish, Syzygy, Maia, opening-book, rules, eval-cache, OpenAI, browser, or real ECE provider call is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/main/StockfishAnalysisGateway.scala`; `modules/evenchess/src/main/AiCoachPolicy.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/StockfishAnalysisGatewayTest.scala`; `modules/evenchess/src/test/AiCoachPolicyTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase N requires Stockfish and AI to remain bounded, server-side, level-gated, and secret-safe while AGENTS/ECE rules keep provider execution and private chess logic inside the separate ECE service.
- Public UX effect: None. This phase adds provider-boundary and validation contracts only.
- Preserved Lichess capability: Native Lichess analysis, eval-cache/fishnet foundations, board interaction, game result authority, ratings, and completed-game views remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added tests for ECE-only provider calls, sanitized provider diagnostics, no raw provider output, Stockfish candidate/eval caps, no direct Lichess Stockfish execution, one-call AI budgets, server-side credentials, and deterministic fallback after invalid AI output.
- Upstream update notes: Future provider/runtime work should remain in EvenChessEngine/ECE unless explicitly scoped to a patch-mapped Lichess adapter; Lichess-side adapters must call ECE and consume normalized side outputs only.
- Rollback notes: Remove the Phase N additions in `EngineGateway`, `StockfishAnalysisGateway`, `AiCoachPolicy`, and their tests; no upstream files need rollback for this phase.

### INT-2026-051 - V2 Phase O feature surfaces

- Phase: V2-Phase-O-Feature-surfaces.
- Lichess seam: Documentation record only; no puzzle, study, opening, analysis, replay, computer-play UI adapter, browser payload, or real ECE HTTP call is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/FeatureSurfacePolicy.scala`; `modules/evenchess/src/test/FeatureSurfacePolicyTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase O requires a feature-surface policy that reuses Lichess foundations while defining when EvenChess overlays, opening guidance, saved-history review, custom review, and computer-play training output may appear without mutating live ECR or rated settlement.
- Public UX effect: None. This phase adds server-side feature-surface policy contracts only; existing puzzle, study, opening, analysis, replay, and computer-play UI behavior is unchanged.
- Preserved Lichess capability: Native Lichess puzzles, study boards, opening explorer/book, analysis/replay, computer play, board interaction, ratings, and completed-game views remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added FeatureSurfacePolicy tests for Lichess-foundation reuse, puzzle rating/ECR separation, study/review neutrality, opening FEN/opening/level gates, analysis saved/custom review layering, computer training defaults, settings/product suppression, and future adapter guards.
- Upstream update notes: Future surface adapters should consume `FeatureSurfacePolicy` through narrow patch-mapped puzzle/study/opening/analysis/computer seams and keep browser clients display-only with no direct ECE calls.
- Rollback notes: Remove the Phase O additions in `FeatureSurfacePolicy` and its tests; no upstream files need rollback for this phase.

### INT-2026-052 - V2 Phase P monetisation

- Phase: V2-Phase-P-Monetisation.
- Lichess seam: Documentation record only; no checkout provider, rewarded-ad provider, account UI adapter, database migration, or payment callback is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/SubscriptionTokensAds.scala`; `modules/evenchess/src/test/SubscriptionTokensAdsTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase P requires monetisation state to cover tokens, ads, subscriptions, review/custom-analysis tokens, and saved games while preserving the fairness boundary that paid state never changes live rated help strength or ECR settlement.
- Public UX effect: None. This phase adds server-side monetisation policy contracts only; visible account/top-bar/payment/ad surfaces are unchanged.
- Preserved Lichess capability: Native account identity, sessions, game lifecycle, result authority, board, clocks, normal ratings, and existing account pages remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added SubscriptionTokensAds tests for Phase P classification, custom-analysis tokens, match-review/full-analysis token consumption, saved-game retention and paid persistence, and monetisation fairness snapshots.
- Upstream update notes: Future account, checkout, ad, token-ledger, saved-game, or review adapters should consume `SubscriptionTokensAds` through narrow patch-mapped seams and keep provider callbacks server-verified.
- Rollback notes: Remove the Phase P additions in `SubscriptionTokensAds` and its tests; no upstream files need rollback for this phase.

### INT-2026-053 - V2 Phase Q telemetry and audit

- Phase: V2-Phase-Q-Telemetry-audit.
- Lichess seam: Documentation record only; no telemetry sink, database migration, dashboard UI, browser analytics adapter, or live event transport is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/TelemetryAnalytics.scala`; `modules/evenchess/src/test/TelemetryAnalyticsTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase Q requires server-authored audit records for match contracts, game-start Set Level, Used Level increases, live ECE payload generation, display actions, proposed-move checks, final settlement, and retention/privacy policy.
- Public UX effect: None. This phase adds telemetry/audit contracts only; existing UI, gameplay, review, rating, and account behavior is unchanged.
- Preserved Lichess capability: Native Lichess game lifecycle, board, clocks, account identity, normal ratings, analysis, and database behavior remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added TelemetryAnalytics tests for Phase Q requirement classification, audit envelope validity, append-only audit ledger behavior, proposed-move audit fields, calibration completeness, and ECE retention privacy.
- Upstream update notes: Future telemetry sinks, dashboards, review views, browser analytics, or database adapters should consume `TelemetryAnalytics` through narrow patch-mapped seams and keep fairness/rating/token authority server-side.
- Rollback notes: Remove the Phase Q additions in `TelemetryAnalytics` and its tests; no upstream files need rollback for this phase.

### INT-2026-054 - V2 Phase R abuse and operations

- Phase: V2-Phase-R-Abuse-ops.
- Lichess seam: Documentation record only; no moderation UI, admin dashboard, live enforcement adapter, operations transport, or incident persistence is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/AbuseTrustControls.scala`; `modules/evenchess/src/test/AbuseTrustControlsTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase R requires abuse and operational controls for non-platform guidance, matchmaking abuse, token/review abuse, ECE/AI output abuse, feature flags, health monitoring, incident pause/no-rate/annul/review flows, campaign copy safety, and local dev rollback readiness.
- Public UX effect: None. This phase adds server-side abuse/ops policy contracts only; existing moderation, admin, gameplay, rating, account, and campaign behavior is unchanged.
- Preserved Lichess capability: Native Lichess moderation patterns, game lifecycle, board, clocks, account identity, normal ratings, admin shell, and local lila-docker flow remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added AbuseTrustControls tests for Phase R classifications, repeat-opponent/strict-preference abuse controls, token/review limits, ECE/AI abuse guards, feature-flag audit rules, health snapshots, incident plans, and campaign copy kill switches.
- Upstream update notes: Future moderation/admin/ops adapters should consume `AbuseTrustControls` through narrow patch-mapped seams and keep fairness-affecting changes audited with policy versions and rollback notes.
- Rollback notes: Remove the Phase R additions in `AbuseTrustControls` and its tests; no upstream files need rollback for this phase.

### INT-2026-055 - V2 Phase S regression hardening

- Phase: V2-Phase-S-Regression-hardening.
- Lichess seam: Documentation record only; no setup UI, search controller, game lifecycle, rating settlement, browser overlay, ECE adapter, or release-candidate automation is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/TestingQaAcceptance.scala`; `modules/evenchess/src/test/TestingQaAcceptanceTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase S requires regression hardening across the native setup/search entry, EvenChess match contracts, normal Lichess mechanics, ECR/MMR separation, level gates, stale payload rejection, overlay/card stability, proposed-move caching, review modes, custom token logic, full-game ECE neutrality, and appendix acceptance gates.
- Public UX effect: None. This phase adds QA acceptance contracts only; existing UI, gameplay, rating, review, overlay, and account behavior is unchanged.
- Preserved Lichess capability: Native Lichess board, legal moves, clocks, PGN/replay, analysis, result flow, accounts, normal ratings, and regression-test ownership remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added TestingQaAcceptance tests for Phase S classifications, every required V2 core regression surface, command/reason evidence, rollback notes, integration-log/patch-map currency, normal-rated-pool protection, server-side coaching authority, secret safety, and desktop/mobile board-layout checks.
- Upstream update notes: Future release-candidate automation should consume `TestingQaAcceptance` evidence and add patch-map entries only when upstream/core Lichess seams are touched.
- Rollback notes: Remove the Phase S additions in `TestingQaAcceptance` and its tests; no upstream files need rollback for this phase.

### INT-2026-056 - V2 Phase T release candidate

- Phase: V2-Phase-T-Release-candidate.
- Lichess seam: Documentation record only; no production deploy automation, release tagging command, upstream sync, route/controller/UI adapter, database migration, ECE service change, or public launch switch is wired in this phase.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/ReleaseHardeningGoNoGo.scala`; `modules/evenchess/src/test/ReleaseHardeningGoNoGoTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 2 Phase T requires final release-candidate integration and go/no-go evidence that composes Phase S regression hardening, release documents, patch-map/integration-log currency, upstream-sync safeguards, high-risk area approvals, candidate metadata, rollback notes, and approval-aware release decisions.
- Public UX effect: None. This phase adds release-candidate policy contracts only; existing UI, gameplay, rating, review, overlay, account, ops, and monetisation behavior is unchanged.
- Preserved Lichess capability: Native Lichess game mechanics, accounts, ratings, admin foundations, upstream-sync process, patch-map discipline, and local development flow remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this phase.
- Tests / checks: scoped `git diff --check`; added ReleaseHardeningGoNoGo tests for Phase T classifications, upstream-sync safeguards, high-risk area approval, release-candidate metadata, approval-aware Go/AwaitingApproval decisions, and blocker reporting for failed Phase S, document, sync, future-scope, and base evidence gates.
- Upstream update notes: Future production launch or tagging automation should consume `ReleaseCandidateEvidence`; any upstream/core file changes must create patch-map entries before release-candidate approval.
- Rollback notes: Remove the Phase T additions in `ReleaseHardeningGoNoGo` and its tests; no upstream files need rollback for this phase.

### INT-2026-057 - V2 Test Ground ECE fallback

- Phase: V2-Test-ground-ECE-fallback.
- Lichess seam: Local launcher/test support only; no route/controller/UI adapter, gameplay path, browser direct ECE call, ECE service internals, or production deployment behavior is changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-test-ece-server.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Real ECE remains under active development, and local EvenChess-Lichess testing needs a reachable ECE-compatible endpoint with a broad deterministic payload for UI and integration smoke testing.
- Public UX effect: None. This affects the local Windows Test Ground launcher only.
- Preserved Lichess capability: Native Lichess local stack start/stop, Docker flow, WSL flow, and site URLs remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this launcher-support update.
- Tests / checks: PowerShell parse, Windows Node syntax check, scoped `git diff --check`, and noninteractive Test Ground actions for `start-test-ece`, `health`, `sample-board`, and `stop-test-ece`.
- Upstream update notes: Keep this launcher-only fallback separate from production ECE wiring; browser code must still never call ECE directly.
- Rollback notes: Remove `scripts/evenchess-test-ece-server.js` and the ECE fallback/action additions in `scripts/evenchess-testground.ps1`; no upstream files need rollback.

### INT-2026-058 - V2 Test Ground browser panel

- Phase: V2-Test-ground-browser-panel.
- Lichess seam: Local launcher/test support only; no route/controller/UI adapter, gameplay path, browser direct ECE call, ECE service internals, or production deployment behavior is changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `scripts/evenchess-test-ece-server.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Local EvenChess-Lichess testing needs a browser control surface with buttons for WSL/Docker startup and shutdown, ECE selection, EvenChess launch, Docker/WSL status, site opening, health checks, sample board payloads, ECE debug IO monitoring, and shutdown/debug output while ECE is still being developed. Running `scripts/evenchess-testground.ps1` now opens the browser panel by default; the legacy text menu remains available with `-Menu`.
- Public UX effect: None. This affects the local Windows Test Ground launcher only.
- Preserved Lichess capability: Native Lichess local stack start/stop, Docker flow, WSL flow, and site URLs remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this launcher-support update.
- Tests / checks: PowerShell parse, Windows Node syntax checks, scoped `git diff --check`, localhost panel smoke checks for `/api/status` and `/api/ece-debug`, and test ECE debug IO sample-board smoke.
- Upstream update notes: Keep this panel bound to localhost and backed by whitelisted launcher actions; browser code in the production app must still never call ECE directly.
- Rollback notes: Remove `scripts/evenchess-testground-panel.js` and the panel/action additions in `scripts/evenchess-testground.ps1`; no upstream files need rollback.

### INT-2026-059 - V2 Test Ground Docker clean shutdown

- Phase: V2-Test-ground-Docker-clean-shutdown.
- Lichess seam: Local launcher/test support only; no route/controller/UI adapter, gameplay path, browser direct ECE call, ECE service internals, or production deployment behavior is changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The local full-cleanup path must release WSL memory without leaving Docker Desktop active against a stopped or terminating internal WSL engine.
- Public UX effect: None. This affects the local Windows Test Ground launcher only.
- Preserved Lichess capability: Native Lichess local stack start/stop, Docker flow, WSL flow, and site URLs remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this launcher-support update.
- Tests / checks: PowerShell parse, Windows Node syntax check, and scoped `git diff --check`.
- Upstream update notes: Keep cleanup behavior local to the Test Ground launcher; production app code must not manage Docker Desktop or WSL.
- Rollback notes: Remove the Docker Desktop stop call from the full-cleanup action and reapply the previous panel blocking rule; no upstream files need rollback.

### INT-2026-060 - V2 Test Ground shortcut exit

- Phase: V2-Test-ground-shortcut-exit.
- Lichess seam: Local launcher/test support only; no route/controller/UI adapter, gameplay path, browser direct ECE call, ECE service internals, or production deployment behavior is changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/install-evenchess-testground-shortcut.ps1`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The Windows shortcut should open the browser panel without leaving a persistent PowerShell window behind after the panel process is detached.
- Public UX effect: None. This affects the local Windows Test Ground shortcut only.
- Preserved Lichess capability: Native Lichess local stack start/stop, Docker flow, WSL flow, and site URLs remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this launcher-support update.
- Tests / checks: PowerShell parse, shortcut refresh smoke, and scoped `git diff --check`.
- Upstream update notes: Keep shortcut behavior local to the Test Ground launcher; production app code must not depend on the shortcut.
- Rollback notes: Restore `-NoExit` in the shortcut installer if persistent launcher output is needed for debugging; no upstream files need rollback.

### INT-2026-061 - V2 Test Ground explicit ECE controls

- Phase: V2-Test-ground-explicit-ECE-controls.
- Lichess seam: Local launcher/test support only; no route/controller/UI adapter, gameplay path, browser direct ECE call, ECE service internals, or production deployment behavior is changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Local ECE testing should be explicit: either start Real ECE or start Test ECE. The panel blocks starting one while the other owns or occupies the ECE endpoint, and the control-panel shutdown action must not be confused with stopping ECE.
- Public UX effect: None. This affects the local Windows Test Ground panel only.
- Preserved Lichess capability: Native Lichess local stack start/stop, Docker flow, WSL flow, and site URLs remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this launcher-support update.
- Tests / checks: PowerShell parse, Windows Node syntax check, and scoped `git diff --check`.
- Upstream update notes: Keep ECE lifecycle choices explicit in local tooling; production app code must not directly start or stop ECE.
- Rollback notes: Restore the Auto ECE button/action if a single fallback choice is needed later; no upstream files need rollback.

### INT-2026-063 - V2 Test Ground live round overlay adapter

- Phase: V2-Test-ground-live-round-overlay-adapter.
- Lichess seam: Native round controller hook only; local browser calls the same-origin Lichess dev endpoint, and the Lichess backend remains the only code that posts to ECE/test ECE.
- Lichess files touched: `ui/round/src/ctrl.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Manual local AI-game testing needs the current round page to show the EvenChess level/status cards, display toggles, and coach-card space at all times, then request the existing ECL-to-ECE smoke bridge for the active game id, ply, FEN, player side, and L10 test payload. Without this hook the renderer has no live payload to draw and previously showed no EvenChess surface at all.
- Public UX effect: Localhost development only. Production/non-local hosts and spectators do not run the adapter, and ECE remains unreachable from browser code.
- Preserved Lichess capability: Native Lichess board, legal moves, clocks, AI-game lifecycle, socket move handling, replay, and ordinary games continue without blocking on ECE.
- Patch map entry: `PM-2026-063`.
- Tests / checks: Added focused UI tests for localhost gating, URL construction, always-visible coach/level shell rendering, display toggles, coach-card placeholder, and monotonic Used Level behavior before and after payload arrival; scoped checks recorded in the completion report.
- Upstream update notes: Remove or feature-flag this temporary adapter when production live ECE scheduling and socket emission are available.
- Rollback notes: Remove `ui/round/src/evenchessTestGround.ts`, the `requestEvenChessTestGroundOverlay` import/calls in `ui/round/src/ctrl.ts`, the Test Ground state/interface/render/CSS additions, and the focused test additions.

### INT-2026-064 - V2 Test Ground asset lifecycle and explicit ECE hardening

- Phase: V2-Test-ground-asset-lifecycle-and-explicit-ECE.
- Lichess seam: Local launcher/test support only; no route/controller/UI adapter, gameplay path, browser direct ECE call, ECE service internals, or production deployment behavior is changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The round package build can emit current bundle/CSS files without rewriting `public/compiled/manifest.json`; the running site then serves stale round assets and no EvenChess shell appears. Launch EvenChess now repairs generated asset ownership and runs the full UI build so the manifest points at the current round bundle before the local stack starts. The panel also reports whether the manifest-selected round JS/CSS contain the current combined coach-level controls and toggle styles.
- Public UX effect: None. This affects only the local Windows Test Ground launcher/panel.
- Preserved Lichess capability: Native Lichess local stack start/stop, Docker flow, WSL flow, site URLs, and explicit Real/Test ECE lifecycle controls remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this launcher-support update.
- Tests / checks: PowerShell parse, Windows Node syntax check, full UI build, and scoped `git diff --check`.
- Upstream update notes: Keep asset-manifest repair/build behavior in local tooling only; production deployments should use the normal asset build pipeline.
- Rollback notes: Remove the `build-ui` launcher action, automatic UI build before local EvenChess launch, Round UI status tile, and stale-PID Test ECE process discovery fallback.

### INT-2026-065 - V2 Test Ground Docker-host ECE bridge target

- Phase: V2-Test-ground-Docker-host-ECE-bridge.
- Lichess seam: Dev-only EvenChess controller bridge and native round Test Ground adapter; browser code still calls only the same-origin Lichess endpoint, and Lichess remains responsible for posting to ECE server-to-server.
- Lichess files touched: `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EngineGateway.scala`; `ui/round/src/evenchessTestGround.ts`.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Dockerized Lila cannot treat its own `127.0.0.1:8787` as the developer-hosted ECE process. The Test Ground bridge now allowlists local ECE aliases including `host.docker.internal`, the round adapter and panel smoke send that backend target to the dev-only Lichess endpoint, and the mock ECE binds for Docker-host access.
- Public UX effect: Localhost development only. Production/non-local hosts do not run this Test Ground adapter, and no browser code receives permission to call ECE directly.
- Preserved Lichess capability: Native Lichess board, move legality, clocks, AI games, local stack startup, and non-EvenChess flows remain unchanged.
- Patch map entry: `PM-2026-064`.
- Tests / checks: Added focused ECE endpoint-policy and round URL-construction assertions; scoped checks recorded in the completion report.
- Upstream update notes: Replace this dev-only query target with production server-side ECE configuration when live ECE scheduling and socket emission are available.
- Rollback notes: Remove the `eceBaseUrl` Test Ground query plumbing, restore the ECE config validator to strict `127.0.0.1`, and revert the Test ECE bind-host launcher default.

### INT-2026-066 - V2 Test Ground board-layer visual rendering

- Phase: V2-Test-ground-board-layer-visual-rendering.
- Lichess seam: Native round controller Chessground auto-shape update path; the browser still consumes only same-origin Lichess overlay JSON and does not call ECE directly.
- Lichess files touched: `ui/round/src/ctrl.ts`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Manual Test Ground play now receives approved visual labels from ECE/test ECE, but the previous renderer only showed those labels as side-panel chips. The board layer now converts current, authorized coordinate labels into Chessground square markers and arrows, and clears/reapplies them when moves, payloads, or the board-label toggle change.
- Public UX effect: Localhost Test Ground and round overlay testing now show board-level markers/arrows from the same approved overlay payload. Non-coordinate diagnostics stay out of the board layer.
- Preserved Lichess capability: Native board movement, legal moves, premoves, clocks, AI-game flow, and existing Chessground overlays remain owned by Lichess; EvenChess shapes are added through the auto-shape API and cleared on stale moves.
- Patch map entry: `PM-2026-066`.
- Tests / checks: Added focused round overlay assertions for square-marker and arrow conversion; scoped checks recorded in the completion report.
- Upstream update notes: Replace the temporary label parser with structured marker/arrow fields when production ECE socket payloads are available.
- Rollback notes: Remove `evenChessBoardShapes`/`renderableEvenChessBoardShapes`, the controller `updateEvenChessAutoShapes` hook, and the board-shape test assertions.

### INT-2026-067 - V2 Test Ground ECE status classification

- Phase: V2-Test-ground-ECE-status-classification.
- Lichess seam: Local launcher/test support only; no route/controller/UI adapter, gameplay path, browser direct ECE call, ECE service internals, or production deployment behavior is changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Real ECE can legitimately report a development/mock mode while still being the real ECE process. The Test Ground panel now classifies the active endpoint by explicit Test ECE signals (`test_payload`, `test-ground-mock`, or Test Ground PID) and labels real ECE separately from Test ECE. The panel reads the ECE repo `.ece-local.pid` written by the real lifecycle script and polls status after Real/Test ECE start/stop actions until the matching green/blocked state settles, so users do not need to refresh the browser manually.
- Public UX effect: None. This affects only the local Windows Test Ground launcher/panel.
- Preserved Lichess capability: Native Lichess local stack start/stop, Docker flow, WSL flow, site URLs, and explicit Real/Test ECE lifecycle controls remain unchanged.
- Patch map entry: None; no upstream/core source file changed in this launcher-support update.
- Tests / checks: PowerShell parse, Windows Node syntax check, and scoped `git diff --check`.
- Upstream update notes: Keep Real/Test ECE classification based on endpoint ownership/test markers, not raw ECE provider mode, because real ECE may expose development/mock mode safely in health.
- Rollback notes: Restore the raw health `mode` label and previous fire-and-forget refresh behavior if the panel is replaced by a richer process manager.

### INT-2026-068 - V2 Test Ground level ladder and fixed coach card

- Phase: V2-Test-ground-level-ladder-fixed-coach-card.
- Lichess seam: Native round overlay display shell and Chessground auto-shape filtering; the browser still consumes only same-origin Lichess overlay JSON and does not call ECE directly.
- Lichess files touched: `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Manual ECL/ECE testing needs the production-shaped EvenChess display controls before ECE is complete. The round overlay now keeps the level controls visible, lets testers apply all features up to a Set-Level-capped level or toggle individual level features, raises the local Used-Level display without lowering it later, and reserves stable coach-card space so incoming payload text does not move the surrounding layout.
- Public UX effect: Localhost Test Ground and round overlay testing now show the expected left level ladder and right coach card. The feature toggles only hide or show already-authorized payload surfaces; they do not grant stronger coaching than the Set Level permits.
- Preserved Lichess capability: Native board movement, legal moves, clocks, AI games, side panels, and non-EvenChess rounds remain owned by Lichess. EvenChess display state stays namespaced under `data.evenchess`.
- Patch map entry: `PM-2026-068`.
- Tests / checks: Added focused round overlay assertions for the level shell, capped presets, monotonic Used-Level display behavior, and per-feature board-shape gating; scoped checks recorded in the completion report.
- Upstream update notes: Reapply the level display shell after accepting upstream round layout changes. Replace the temporary feature-key classifier when production ECE emits structured feature atoms.
- Rollback notes: Revert the per-level display toggle additions, restore the compact status card/global toggles, and remove the `PM-2026-068` test assertions.

### INT-2026-069 - V2 real ECE public payload adapter alignment

- Phase: V2-Test-ground-real-ECE-contract-alignment.
- Lichess seam: Dev-only EvenChess backend bridge parses real ECE public board-state output into server-approved round overlay payloads; browser code still consumes same-origin Lichess JSON only.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-testground-panel.js`; `scripts/evenchess-testground.ps1`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Real ECE returns structured `summary`, `immediate_waning`, and `plan` objects; `candidate_moves`; top-level `evaluation`, `opening`, and `human_risk`; and nested `overlays.trade_status`, `overlays.threats`, and `overlays.pinned_pieces`. Real Offset Count entries use public keys such as `target_square` and `piece_count_delta`, and Dockerized Lila needs a Docker-host-reachable local ECE target so `host.docker.internal:8787` can reach the developer-hosted service. The Test Ground bridge must normalize those public fields so the round Display Engine receives the same approved cards, visual labels, and board-shape atoms from either real ECE or the test fixture.
- Public UX effect: Localhost Test Ground can now show real ECE output where available and the test ECE fixture exercises the same public response shape, including candidate cards, eval visual, hanging/offset/threat/pin overlays, and human-risk/opening cards. Starting real ECE from Test Ground binds it for Docker reachability, while unreachable ECE returns a clean non-fatal JSON error instead of a controller exception.
- Preserved Lichess capability: Native board input, legal moves, clocks, AI games, and normal game lifecycle remain unchanged. ECE remains a server-to-server dependency and output is still side-gated before it reaches the round UI.
- Patch map entry: `PM-2026-069`.
- Tests / checks: Direct real ECE health/ready/board probe; WSL `host.docker.internal:8787` reachability; direct test ECE fixture smoke; ECL-to-real-ECE bridge smoke for L0/L10 and white/black sides; ECL-to-test-ECE bridge smoke; clean non-fatal ECE-unavailable JSON; root Scala compile/dev stack recompile; EvenChess backend tests; Node syntax checks; scoped whitespace checks.
- Upstream update notes: Reapply the parser normalization if the Test Ground controller seam moves. Replace string-label board visual conversion when production ECE emits structured round socket visual atoms.
- Rollback notes: Revert the parser compatibility helpers and restore the previous flat test ECE fixture only if real ECE changes back to the older flat payload shape.

### INT-2026-070 - V2 spec-style board-attached overlay renderer

- Phase: V2-Test-ground-spec-style-board-overlay.
- Lichess seam: Native round board render tree receives a pointer-transparent EvenChess board layer inside `.main-board`; backend bridge emits structured-enough visual labels for that layer.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/view/main.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `modules/evenchess/src/main/EceLiveBridge.scala`; `scripts/evenchess-testground-panel.js`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The supplied overlay recreation spec requires details the previous Chessground auto-shape approach could not match cleanly: exact 100x100 board-coordinate math, shortened straight arrows, dotted threat lines, candidate A/B/C source labels, coner badge math, square-outline highlights, and Offset Count shield/number colors. The layer is board-attached and pointer-transparent, so Lichess still owns movement, legal targets, premoves, promotion, clocks, and lifecycle.
- Public UX effect: Test ECE and real ECE board visuals appear closer to the reference format: green/red dotted threat arrows, amber pin lines, green candidate arrows with labels, purple/orange loose-piece small badges, square outlines for pinned/loose squares, top-left pin badges, and bottom-right blue/green/red Offset Count markers. Feature toggles remain local display controls over the already-authorized ECE payload, so unchecking Safety, Offset Count, Pattern, or candidate controls immediately hides those overlay families without changing the retained Used Level or mutating the returned payload.
- Preserved Lichess capability: Browser code still receives only same-origin Lichess payloads. ECE stays server-to-server. Normal non-EvenChess rounds do not render the layer because they lack an authorized EvenChess live payload.
- Patch map entry: `PM-2026-070`.
- Tests / checks: `node ui/test evenchessOverlay.test.ts`; real-ECE-style feature-gating regression for Safety, Offset Count, Pattern, and candidate overlay families; `pnpm exec tsc -p ui/round/tsconfig.json --noEmit`; `pnpm exec oxfmt --check ...`; `pnpm exec stylelint ui/round/css/_evenchess-live.scss`; `node --check` for Test Ground scripts; root Scala compile; EvenChess backend tests.
- Upstream update notes: Reapply the `renderEvenChessBoardOverlay(ctrl)` child inside the native board container after upstream round view changes. If production socket payloads become structured, replace the label parser while keeping the 100x100 renderer.
- Rollback notes: Remove the board-attached layer from `main.ts` and restore Chessground auto-shape rendering for EvenChess visuals if this renderer causes board-layout conflicts.

### INT-2026-071 - V2 Test Ground Linux ECE lifecycle

- Phase: V2-Test-ground-Linux-ECE-lifecycle.
- Lichess seam: Local launcher/test support only; no route/controller/UI adapter, gameplay path, browser direct ECE call, ECE service internals, or production deployment behavior is changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`.
- Why this seam exists: The active ECE repo moved to `/home/jayde/dev/lila-docker/repos/ece`. Test Ground real ECE lifecycle now calls `bash scripts/start-ece-linux.sh` and `bash scripts/stop-ece-linux.sh` through WSL, passes debug IO logging into the Linux process, reads `/home/jayde/dev/lila-docker/repos/ece/.ece-local.pid`, and monitors `/home/jayde/dev/lila-docker/repos/ece/logs/ece-debug-io.json` through the corresponding `\\wsl$` host path.
- Public UX effect: None. This affects only local Test Ground startup/shutdown/status/debug tooling.
- Preserved Lichess capability: Browser code still calls only same-origin Lichess endpoints, and Lichess continues to call ECE server-to-server. No ECE internals, provider paths, secrets, prompts, engines, tablebases, model weights, or generated DBs are copied into ECL.
- Patch map entry: None; no upstream/core source file changed in this lifecycle update.
- Tests / checks: PowerShell parse, Node syntax check, Linux ECE lifecycle script presence checks, Test ECE start/sample-board/debug-log/stop cycle, and scoped `git diff --check`. Real ECE start was attempted through `scripts/start-ece-linux.sh`; the ECE script briefly reported health, but the service then exited and the launcher correctly reported ECE unavailable after its health wait.
- Upstream update notes: Keep Test Ground lifecycle paths configurable by environment, but default them to the Linux ECE repo while ECE runs as a sibling service.
- Rollback notes: Restore the previous launcher paths only if the active ECE development repo moves again; do not reintroduce Windows `.ps1` ECE lifecycle calls for the current setup.

### INT-2026-072 - V2 Test Ground round card layout, eval bar, and granular controls

- Phase: V2-Test-ground-round-card-layout-eval-controls.
- Lichess seam: Native round grid and EvenChess live display shell; browser code still receives only same-origin Lichess overlay JSON and never calls ECE directly.
- Lichess files touched: `ui/round/src/interfaces.ts`; `ui/round/src/view/main.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_app-layout.scss`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: EvenChess needs a stable three-surface live round layout: the level-selection card sits left of the board, the eval bar sits immediately between the level card and the board, and the coach card sits right of the board. The level card now exposes granular display toggles for the available ECE payload families instead of one broad toggle per level, while shared text cards use one shared coach-text toggle.
- Public UX effect: Test ECE and real ECE overlay testing now keeps controls and coaching in the intended board-adjacent positions. Selecting "Apply up to" enables all features through the capped level, and unchecking a feature hides only its matching already-authorized payload family. Used Level still never decreases from local display selections. Follow-up level-card polish keeps the scroll position stable while ticking individual toggles and shows only the header `Set Level: X` / `Used Level: Y` labels, without the duplicate Set/Used/Payload summary block.
- Preserved Lichess capability: Lichess still owns legal moves, clocks, table/move list, board geometry, and game lifecycle. ECE remains a private server-to-server service, and the browser does not receive provider paths, secrets, raw engine output, or ECE internals.
- Patch map entry: `PM-2026-072`.
- Tests / checks: `node ui/test evenchessOverlay.test.ts`; focused round TypeScript/style/build/browser checks recorded in the completion report. Follow-up scroll-stability regression verifies individual feature toggles do not remount the live panel.
- Upstream update notes: Reapply the `evenchess-live-layout` grid override after upstream round layout changes. Replace the temporary string classifier when production ECE emits structured display atoms.
- Rollback notes: Remove the `evenchess-live-layout` round class/grid override, restore the prior right-side compact shell, and collapse granular feature keys back only if the production design explicitly changes away from left controls/eval/right coach.

### INT-2026-073 - V2 ECE quick/deep board-state bridge

- Phase: V2-ECE-quick-deep-contract-split.
- Lichess seam: Dev-only EvenChess backend bridge and EvenChess gateway contract models; browser code still calls only same-origin Lichess endpoints and never calls ECE directly.
- Lichess files touched: `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EngineGateway.scala`.
- EvenChess files touched: `modules/evenchess/src/test/EngineGatewayTest.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `scripts/evenchess-testground.ps1`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE now separates deterministic quick board-state output from slower provider-backed deep addenda. The Lichess bridge must call `/v1/ece/board/quick`, render quick side output when valid, call `/v1/ece/board/deep` only when a side's authorised level/profile needs deep modules, and merge deep addenda only after request echo, quick context, FEN, level, diagnostics, and public-shape checks pass.
- Public UX effect: Local Test Ground overlays can appear from quick output without waiting for deep provider work; candidate/eval/human-risk addenda arrive through the same approved overlay pipeline when deep succeeds. The Test ECE now uses real-shaped health, ready, quick, deep, proposed-move, and game-review envelopes so ECL testing exercises the same public structure expected from real ECE. If deep is unavailable or stale, quick output remains non-fatal.
- Preserved Lichess capability: Native move legality, clocks, board rendering, and game lifecycle remain unchanged. ECE remains a private server-to-server service, and ECL still does not copy provider internals, paths, secrets, prompts, engine binaries, tablebases, model weights, or generated ECE databases.
- Patch map entry: `PM-2026-073`.
- Tests / checks: `node --test scripts/evenchess-test-ece-server.test.mjs`; Test ECE quick/deep/proposed smoke on port `8799`; Node syntax check; PowerShell parser check; scoped `git diff --check`; attempted focused `evenchess/testOnly lila.evenchess.EngineGatewayTest` but local WSL lacks `sbt`.
- Upstream update notes: Keep the quick/deep caller logic in the server-side bridge. When production live ECE scheduling replaces the Test Ground endpoint, preserve the same stale-check and addenda-merge rules.
- Rollback notes: Restore legacy `/v1/ece/board` calls only while ECE legacy compatibility is explicitly required; otherwise prefer the split quick/deep contract.

### INT-2026-074 - V2 lobby computer native-flow correction

- Phase: V2-lobby-computer-native-flow-correction.
- Lichess seam: Native lobby start-card labels, setup modal button copy, setup controller submit path, and hidden EvenChess Set Level defaulting for computer games.
- Lichess files touched: `ui/lobby/src/view/table.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/setupCtrl.ts`.
- EvenChess files touched: `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The lobby start cards should preserve Lichess-native affordances and wording. Computer games are not an EvenChess matchmaking/search flow; they use native `/setup/ai` while EvenChess policy treats them as Set Level L10 for local overlay/testing and live coaching caps.
- Public UX effect: The homepage cards show `Create lobby game`, `Challenge a friend`, and `Play against computer`; the extra `/bots` card is not exposed. Opening `Play against computer` shows the normal AI setup modal without the EvenChess search settings block, and submitting it uses the native computer-game setup route.
- Preserved Lichess capability: Native AI game creation, AI strength selection, side selection, time controls, variant/FEN controls, and modal shell remain Lichess-owned. Hook/friend setup still carries EvenChess target/search controls and status echo.
- Patch map entry: `PM-2026-041`.
- Tests / checks: `node ui/test lobby/tests/evenchessSetup.test.ts`; `pnpm exec oxfmt --check` on touched lobby files; `pnpm exec oxlint` on touched lobby files; `pnpm exec tsc -p ui/lobby/tsconfig.json --noEmit`; full `ui/build -n`; browser smoke verified the three visible lobby cards and computer modal.
- Upstream update notes: Reapply only the hook/friend EvenChess setup fields and status echo after upstream lobby changes; keep AI/computer setup on the native submit path unless product requirements explicitly change.
- Rollback notes: If computer games later become an EvenChess-owned matchmaking mode, add a new requirement and patch-map entry before routing AI setup through `/evenchess/play/search.json`.

### INT-2026-075 - V2 Test Ground real ECE bind host

- Phase: V2-Test-ground-real-ECE-bind-host.
- Lichess seam: Local Test Ground real-ECE lifecycle invocation only; no production route/controller/gameplay behavior is changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The Linux ECE repo defaults `ECE_HOST` to `127.0.0.1`, which is appropriate inside WSL but not sufficient for the local ECL Test Ground. ECL needs Windows to reach `http://127.0.0.1:8787` and dockerized Lila to reach `http://host.docker.internal:8787`, so the launcher passes `ECE_HOST=0.0.0.0` while preserving the public local URL.
- Public UX effect: None outside local testing. When the ECE lifecycle script keeps the process alive, the Test Ground health/sample-board buttons can reach real ECE from Windows and Docker-host contexts.
- Preserved Lichess capability: ECE remains a separate private backend service; browser code still calls only same-origin Lichess endpoints.
- Patch map entry: None; no upstream/core Lichess file changed.
- Tests / checks: PowerShell parser check, Test Ground health action, and Test Ground sample-board action against a manually detached real ECE process.
- Upstream update notes: Keep this bind-host override in the local launcher unless ECE changes its Linux start script to expose a separate documented external bind option.
- Rollback notes: Remove the override only if real ECE is intentionally restricted to WSL-only access and ECL no longer needs Docker-host or Windows-local reachability.

### INT-2026-077 - V1.1-Phase-C-Bot-matchmaking-fill

- Phase: ECE-1.1-Phase-C-Bot-matchmaking-fill.
- Lichess seam: Search endpoint payload contract (`search.json`) and `PlaySearchIntegration` fallback-seeding service seam.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`.
- Why this seam exists: Version 1.1 platform-bot matchmaking requires a timed fallback when an active human search waits without a human match. A bot ticket should only be injected after timeout and only when scope/rate settings permit.
- Public UX effect: Human-only matches remain unchanged until timeout. Public search JSON/UI is deployment-safe after the later polish pass; seed/ticket diagnostics are kept out of public browser-facing status.
- Preserved Lichess capability: Normal human matchmaking pools, clock/rule enforcement, and game lifecycle remain Lichess-owned. ECE remains a separate private backend and browser code still calls Lichess endpoints only.
- Patch map entry: `PM-2026-077`.
- Tests / checks: `PlaySearchIntegrationTest` scenarios for config parsing, pre-timeout no seed, timeout seed, scoped seed blocking, and disabled bot mode; focused search JSON poll smoke not executed due environment limitation (`./lila.sh` works but `sbt` is unavailable locally).
- Upstream update notes: Preserve this logic as an internal seam in the MMR integration service; if upstream changes polling cadence/routes, reapply the controller status payload contract after route/controller merge.
- Rollback notes: Revert bot seeding and payload bot-mode output if policy decides to disable simulated matchmaking; keep `PlaySearchIntegration` seeding logic isolated for staged reintroduction.

### INT-2026-079 - V1.1-Phase-G-Contract-disclosure-and-status

- Phase: ECE-1.1-Phase-G-Disclosure-status-gameplay impact surfaces.
- Lichess seam: Lobby search status render seam (`ui/lobby/src/view/table.ts`) and search contract payload seam (`app/controllers/EvenChess.scala`).
- EvenChess files touched: `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`.
- Why this seam exists: Version 1.1 must surface bot introduction state clearly enough for users while keeping detailed contract provenance available to operations/admin code rather than the public browser UI.
- Public UX effect: Search status now displays deployment-safe bot enabled/disabled disclosure, wait/status labels, access labels, and assigned levels without exposing internal seed/ticket diagnostics or raw contract-source fields.
- Preserved Lichess capability: Native lobby navigation, route flow, and game lifecycle remain unchanged; only status display and contract telemetry are expanded.
- Patch map entry: `PM-2026-078`.
- Tests / checks: `PlaySearchIntegrationTest` updated for `contractSource` values on direct human matches and timed bot matches; manual status schema smoke requested for `search.json` in this environment was not run due local `sbt` limitations.
- Upstream update notes: Reapply the contract source and bot-mode disclosure rendering when the setup/search UI is reworked; keep bot-mode computation and contract classification inside EvenChess-owned integration service.
- Rollback notes: Remove the public disclosure/status rendering only if product policy changes; keep detailed contract-source state out of public UI unless a later requirement explicitly reverses `OVR-V2-014`.

### INT-2026-080 - V1.1-Phase-I-ECE-bridge-verification

- Phase: ECE-1.1-Phase-I-ECE-contract-and-live-bridge-verification.
- Lichess seam: Server-side EvenChess ECE gateway model. No browser ECE endpoint, round UI path, ECE provider integration, or Lichess game lifecycle path changed.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `docs/requirements/plan_version_1.1/PHASE_I_ECE_BRIDGE_VERIFICATION.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Version 1.1 bot fallback and simulation games must not introduce a separate ECE contract branch. Human, matchmaking-bot, and simulation-bot game contexts now enter the same `EceBoardStateRequest.boardState` construction path and continue through `/v1/ece/board/quick` plus conditional `/v1/ece/board/deep`.
- Public UX effect: None directly. This preserves the existing overlay bridge behavior while proving bot-backed games use the same validation, stale-FEN checks, level echo checks, and public-shape leak rejection as human games.
- Preserved Lichess capability: Normal game lifecycle, legal moves, clocks, and browser overlay rendering remain Lichess-owned. ECE remains a separate private backend, and ECL does not copy ECE provider paths, secrets, prompts, engine binaries, tablebases, model weights, or generated databases.
- Patch map entry: None; no upstream/core Lichess file changed.
- Tests / checks: `EngineGatewayTest` now covers human, matchmaking-bot, and simulation-bot contexts sharing the same quick/deep bridge and rejecting stale/leaky payloads. Test Ground's Test ECE fixture still covers real-shaped quick/deep/proposed/review envelopes through `scripts/evenchess-test-ece-server.test.mjs`.
- Upstream update notes: Keep bot source classification outside the ECE public request shape unless the ECE caller contract explicitly adds such a field. Reapply the typed context around the server gateway if ECE live scheduling moves out of the current module.
- Rollback notes: Remove `EceBridgeGameContext` and the Phase I test only if bot/simulation matchmaking is removed; do not replace it with browser-side ECE calls or a bot-specific ECE endpoint.

### INT-2026-081 - Analysis memory retention and mode-neutral overlay policy

- Phase: Analysis-memory-retention-and-overlay-policy.
- Lichess seam: None in this implementation step. This adds EvenChess-owned memory/policy framework only; later DB/controller/UI wiring to native game history or analysis pages must be patch-mapped when it touches upstream/core files.
- Lichess files touched: None.
- EvenChess files touched: `modules/evenchess/src/main/AnalysisMemory.scala`; `modules/evenchess/src/test/AnalysisMemoryTest.scala`; `docs/requirements/plan_analysis_memory/PLAN.md`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Replay and analysis should reuse attached ECE payload history instead of recalculating when the payload is still retained. The default memory limits are now concrete: each user keeps the last 10 games with live ECE history and the last 100 requested full-game/custom analyses.
- Public UX effect: Future analysis/replay integration can show the same EvenChess overlay shell across live play, computer play, analysis/replay, and review surfaces. If retained history is missing, the shell can remain visible but payload-backed overlays are unavailable until analysis is requested.
- Preserved Lichess capability: Native game history, PGN/replay, analysis tree, legal moves, clocks, and fishnet analysis remain Lichess-owned. This framework does not call ECE from the browser and does not store raw provider payloads by default.
- Patch map entry: None; no upstream/core Lichess file changed.
- Tests / checks: `AnalysisMemoryTest` covers 10-game recent retention, 100 requested-analysis retention, different White/Black analysis-level keys, missing-history lookup, requested-analysis lookup, and mode-neutral overlay Set Level / Used Level behavior. Local execution was blocked because `./lila.sh` could not find `sbt`; scoped static checks passed.
- Upstream update notes: Wire this through existing Lichess game-history/analysis foundations when implementing persistence. Keep storage of raw ECE/provider payloads disabled unless a later retention requirement explicitly permits it.
- Rollback notes: Remove `AnalysisMemory` and its requirements/log entry if the product decides not to persist ECE history; keep existing `LiveCoaching.LiveEceHistoryRecord` unless live ECE history itself is removed.

### INT-2026-082 - Test Ground explicit UI build before fast EvenChess launch

- Phase: Test-ground-launcher-ergonomics.
- Lichess seam: Local development launcher only. No browser ECE call path, production route, live game lifecycle, or ECE adapter contract changed.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Launching EvenChess from the Test Ground should start the local stack quickly once assets are already built. UI asset compilation is now an explicit Build UI Assets action, while Launch EvenChess only verifies the existing manifest/round assets before starting containers and opening the site.
- Public UX effect: None in production. The local Test Ground control panel now places Build UI Assets beside Open Site in the EvenChess section and shows the last explicit build time plus built git/hash version in the status row.
- Preserved Lichess capability: Native `./ui/build` remains the only asset compiler; Docker/WSL startup and Lila container launch still use existing local scripts.
- Patch map entry: None; no upstream/core Lichess file changed.
- Tests / checks: PowerShell parser check for `scripts/evenchess-testground.ps1`; `node --check scripts/evenchess-testground-panel.js`; launcher load smoke with an intentionally unknown action.
- Upstream update notes: Keep build metadata local to the Test Ground state directory. Do not make production startup depend on this metadata.
- Rollback notes: Revert the Test Ground launch check to call `Build-EvenChessUiAssets` if the development workflow later prefers automatic rebuilds over fast launch.

### INT-2026-083 - Local startup compile hardening for bot matchmaking changes

- Phase: V1.1-bot-matchmaking-startup-hardening.
- Lichess seam: EvenChess controller search-status JSON and EvenChess-owned matchmaking modules. No ECE route, browser-to-ECE behavior, or core game lifecycle path changed.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/AnalysisMemoryTest.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`.
- Why this seam exists: Local launch was waiting on `localhost:8080` while the Lila container repeatedly failed Scala compilation in recent bot-mode and analysis-memory code. The startup path now compiles through the full app controller and focused EvenChess test classes.
- Public UX effect: None directly, except the local EvenChess site can start again after bot-mode changes.
- Preserved Lichess capability: Docker/Lila startup remains owned by lila-docker; bot-mode remains server-side and disabled by default unless configured.
- Patch map entry: `PM-2026-078`.
- Tests / checks: Docker `evenchess/testOnly` for `PlaySearchIntegrationTest`, `LevelBasedMatchmakingTest`, and `AnalysisMemoryTest`; Docker full `Compile / compile`; local `http://localhost:8080/` returned `200` after Lila restart.
- Upstream update notes: Keep `MatchContractSource` referenced from `PlaySearchIntegration`, not nested under `MatchmakingIntegrationResult`, if the controller search-status JSON is reapplied after upstream changes.
- Rollback notes: Revert only the compile-hardening fixes if the bot-mode implementation is removed; do not restore invalid test defaults or browser-side bot control.

### INT-2026-078 - V1.1-Phase-F-Rating-time-control-and-humanization

- Phase: ECE-1.1-Phase-F-Rating-time-control-humanization.
- Lichess seam: Search queue integration for bot fallback candidate profile seeding and existing matchmaking seam (`search.json` polling contract).
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`.
- Why this seam exists: Bot candidates inserted after timeout must remain within the same pool/time-control contract as the request they are standing in for and still participate in normal widening + level-contract enforcement. Phase F adds randomized human-like/fast timing profiles and deterministic bot-seed derivation by bot id while capping matchmaking latency to prevent stale matching and deterministic profile fingerprints.
- Public UX effect: No direct user-visible UI change. This phase improves match quality and fairness by keeping bot timing profiles realistic while preserving existing bot-mode wait behavior and time/control alignment.
- Preserved Lichess capability: Human matchmaking windows, game lifecycle, clock model, and round engine behavior remain unchanged.
- Patch map entry: None; no upstream/core Lichess files changed in this phase.
- Tests / checks: `PlaySearchIntegrationTest` and `LevelBasedMatchmakingTest` cases for bot profile generation, persona split, pool/time-control alignment, and latency cap behavior; focused test-ground smoke not executed in this environment due absent local Scala toolchain (`sbt` unavailable via `./lila.sh`).
- Upstream update notes: Reapply the `search.json` status payload and bot seeding controls where routes/services are re-merged. Keep bot profile and timing split in `LevelBasedMatchmaking` to avoid deterministic move signatures.
- Rollback notes: Remove bot match profile timing spread controls only if policy requires fully deterministic replacement opponents; keep seeding timeout and pool/time-control matching safeguards.

### INT-2026-076 - V2 persistent round display feature toggles

- Phase: V2-round-display-control-hardening.
- Lichess seam: EvenChess live round display shell and board-attached overlay filtering; browser code still consumes only same-origin Lichess overlay JSON.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The level-selection card needs local display controls over already-authorized ECE payload families. Feature toggles must persist across later payloads/moves, while the "Apply up to" selector remains only a quick bulk setter. Used Level must stay monotonic when visible features are lowered or hidden.
- Public UX effect: Unchecking every EvenChess feature hides matching coach and board-overlay families and stays unchecked after a move/payload refresh. Selecting a lower preset can reduce visible features without lowering the retained Used Level.
- Preserved Lichess capability: Lichess still owns legal moves, clocks, board geometry, and game lifecycle. The client still cannot grant itself stronger coaching than the server-authorized payload permits.
- Patch map entry: `PM-2026-072`.
- Tests / checks: `node ui/test evenchessOverlay.test.ts`; `pnpm exec oxfmt --check ui/round/src/view/evenchessOverlay.ts ui/round/tests/evenchessOverlay.test.ts`; `pnpm exec tsc -p ui/round/tsconfig.json --noEmit`; `ui/build -n`; browser smoke on a local computer game verified all feature toggles stayed off after a move while Used Level remained 10.
- Upstream update notes: Preserve complete toggle-map materialization and the feature-selection keyed redraw if the round overlay shell is reapplied after upstream round UI changes.
- Rollback notes: Revert only this display-state persistence patch if the product later replaces granular feature toggles with server-persisted display preferences.

### INT-2026-084 - V2 round overlay safety marker correction

- Phase: V2-round-overlay-correction.
- Lichess seam: Dev-only ECE bridge plus native round board-attached overlay renderer.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `scripts/evenchess-test-ece-server.js`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: After a move, the overlay fetch could time out or be skipped while an older ECE bridge request was still in flight, leaving the board cleared until manual refresh. Accepted quick/deep bridge responses could also arrive visual-empty before a later request returned the board atoms, so the Test Ground client now performs a bounded retry for accepted visual-empty payloads. The client also treated generic `attackable` text too broadly, which caused non-attackable hanging/loose pieces to be gated and colored as hanging pieces. The round overlay controls were using normal snabbdom `on` handlers, but the round renderer only installs class and attribute modules, so level and feature toggles needed the repo's `bind` hook path. The controller parser needed to accept real ECE safety/pin variations without copying ECE internals into ECL.
- Public UX effect: The board overlay can repopulate after moves, local toggles redraw the right overlay families immediately and persist through later payload refreshes, non-attackable loose/hanging pieces render orange at the top-right of the square, attackable hanging pieces render purple at the bottom-left, Offset Count remains bottom-right, and pin badges remain top-left.
- Preserved Lichess capability: Lichess still owns moves, clocks, board input, and game lifecycle. Browser code still calls only same-origin ECL endpoints and never calls ECE directly.
- Patch map entry: `PM-2026-084`.
- Tests / checks: `node ui/test round/tests/evenchessOverlay.test.ts round/tests/evenchessTestGround.test.ts`; `CI=true ./ui/build -n -k`; browser toggle smoke on a local computer game.
- Upstream update notes: Keep the safety classifier order: explicit non-attackable cues must win over the substring `attackable`. Preserve the ply/FEN/feature-selection keyed redraw so feature toggles and post-move payloads refresh the board layer. Preserve `bind` hooks for these round controls unless the round renderer adds the snabbdom event-listener module.
- Rollback notes: Revert `PM-2026-084` if ECE changes to emit only strongly typed structured marker fields and the label classifier is replaced by a typed adapter.

### INT-2026-085 - V2 MMR preference and contract handoff sweep

- Phase: V2-MMR-full-sweep.
- Lichess seam: EvenChess search-status JSON/UI and the pre-coaching game-policy persistence handoff.
- Lichess files touched: `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/table.ts`; `ui/lobby/src/evenchessSetup.ts`.
- EvenChess files touched: `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The MMR Engine assigns Set Levels in the match contract, so the game-start policy and visible status must use those assigned levels rather than queued ticket defaults or UI hint values.
- Public UX effect: Normal search can remain preference-free, Apply preferences still gates target controls, Any remains no constraint, and matched search status shows assigned White/Black contract levels without exposing internal contract stage/source diagnostics.
- Preserved Lichess capability: Native lobby/search UI remains the host shell. Server-side EvenChess code still owns matchmaking decisions and coaching permission; the browser only displays same-origin contract summaries.
- Patch map entry: `PM-2026-085`.
- Tests / checks: Focused Scala and lobby UI tests verify contract-assigned level persistence and Any target handling.
- Upstream update notes: Preserve the contract-aware `GameStartService.persistBeforeCoaching` argument when wiring future production game creation; otherwise games can start with stale queued L5 hints instead of MMR-assigned levels.
- Rollback notes: If contract-level assignment is removed, revert this entry with the MMR assignment requirements and return policy persistence to ticket-level-only behavior intentionally.

### INT-2026-086 - V2 exact overlay marker semantics and Used-Level refresh

- Phase: V2-round-overlay-marker-correction.
- Lichess seam: Native round board-attached overlay renderer, round display controls, local Test Ground ECE request adapter, and dev-only ECE bridge parser.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `modules/evenchess/src/main/CoachingOverlays.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `modules/evenchess/src/test/CoachingOverlaysTest.scala`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The board renderer must display ECE overlay atoms exactly as authorized: student threats green dotted, opponent threats red dotted, pins top-left, non-attackable hanging/loose bottom-left orange, attackable student hanging bottom-left red with red rim, attackable opponent hanging bottom-left purple with purple rim, and Offset Count top-right green/red/blue. Local display toggles must hide available payload families without mutating payloads, while raising Used Level must trigger a new same-position ECE request. Board visual atoms are not duplicated as coach-side text chips because the board overlay already communicates them and the extra list destabilizes the right column.
- Public UX effect: The level selector now has separate Student threat arrows and Opponent threat arrows toggles. Toggling on a higher-level feature raises Used Level and the Test Ground requests that higher ECE level; toggling off hides the feature without lowering Used Level, including the eval bar. The coach side no longer repeats board overlay visuals as text chips. The Test ECE fixture now exposes both attackable-hanging ownership colors and all three Offset Count badge outcomes so the browser panel can verify the full overlay set while real ECE is in development.
- Preserved Lichess capability: Lichess still owns board input, legal moves, clocks, and orientation. Browser code calls only same-origin ECL endpoints and never calls ECE directly.
- Patch map entry: `PM-2026-086`.
- Tests / checks: `node ui/test round/tests/evenchessOverlay.test.ts round/tests/evenchessTestGround.test.ts`; `node scripts/evenchess-test-ece-server.test.mjs`; Docker-backed `evenchess/testOnly` compiled the ECE bridge, overlay, and live board tests as part of the EvenChess suite; browser smoke with Test ECE verified live payload rendering, preset behavior, per-family toggle gating, and coach-side visual chip removal.
- Upstream update notes: Preserve ownership-aware hanging marker parsing and split threat feature keys when rebasing round UI. Do not restore the generic `threats` toggle or bottom-right Offset Count placement.
- Rollback notes: Revert together with `OVR-V2-012` and the ownership-aware marker requirements if the visual spec changes again.

### INT-2026-087 - V2 proposed-move preview bridge and round UI control

- Phase: V2-proposed-move-preview.
- Lichess seam: Same-origin ECL proposed-move bridge, native round UI coach column, and chessground drawable-change hook.
- Lichess files touched: `conf/routes`; `app/controllers/EvenChess.scala`; `ui/round/src/ctrl.ts`; `ui/round/src/ground.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `scripts/evenchess-test-ece-server.test.mjs`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Proposed-move mode depends on the current user-drawn board arrow and the current legal-destination map. The client validates exactly one green arrow and a legal move, then calls only the same-origin ECL bridge. ECL calls ECE server-to-server and accepts only matching FEN/move/request output. The preview is separate from the saved board-state coach payload and is hidden when the selected arrow is removed or changed.
- Public UX effect: A Proposed Move button appears under the coach. L0-L4 cannot use it; L5-L10 get one proposed-move call per turn. Re-pressing the same arrow on the same turn returns the cached preview without consuming another call. A different arrow after consumption is rejected until the board changes.
- Preserved Lichess capability: Lichess still owns board input, move legality, clocks, and game lifecycle. Browser code never calls ECE directly and proposed-move previews do not replace actual-position board-state history.
- Patch map entry: `PM-2026-087`.
- Tests / checks: `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `scripts/evenchess-test-ece-server.test.mjs`.
- Upstream update notes: Preserve `drawable.onChange`, one-green-arrow validation, legal-destination gating, Used-Level quota gating, and same-origin bridge behavior when rebasing round UI or route/controller changes.
- Rollback notes: Remove the proposed-move route/helper/panel together if proposed-move mode is paused; do not leave browser-direct ECE fallback code.

### INT-2026-088 - V2 player-turn coach text cadence

- Phase: V2-live-coach-text-cadence.
- Lichess seam: Native round payload application and coach-card rendering.
- Lichess files touched: `ui/round/src/ctrl.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE board-state payloads are expected after every move, but coach text should update for the player only when it becomes their turn. The round UI now keeps a separate player-turn coach text snapshot. Current payloads can still update board overlays and history while opponent-turn payload cards do not replace visible player text.
- Public UX effect: Coach text no longer changes immediately after the player moves just because a new opponent-turn payload arrived. It refreshes from the next safe payload when the player is on move again. Board overlays continue to update every move.
- Preserved Lichess capability: Lichess still owns move legality, turn state, board orientation, clocks, and round lifecycle. The client does not grant itself additional coaching authority; it only chooses when to refresh already-authorized text.
- Patch map entry: `PM-2026-088`.
- Tests / checks: `ui/round/tests/evenchessOverlay.test.ts`.
- Upstream update notes: Preserve the separate `evenchess.coachText` snapshot and the `ctrl.canMove()` gate around snapshot refreshes. Do not use current live payload cards directly for visible coach text on opponent turns.
- Rollback notes: Revert only if product decides coach text should follow every payload regardless of whose turn it is.

### INT-2026-089 - V2 preferred starting Used Level and persistent level selections

- Phase: V2-level-selection-persistence.
- Lichess seam: Native account preferences, pref JSON, native round level selector, and local Test Ground ECE request adapter.
- Lichess files touched: `modules/pref/src/main/FormCompatLayer.scala`; `modules/pref/src/main/JsonView.scala`; `modules/pref/src/main/PrefForm.scala`; `modules/pref/src/main/ui/AccountPref.scala`; `ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `modules/evenchess/src/main/UserSettings.scala`; `modules/evenchess/src/test/UserSettingsTest.scala`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Preferred starting Used Level is stored in the normal Lichess account preference system and serialized into round data. The round UI must initialize from that preference once per game, cap it by the game Set Level, and then preserve manual dropdown/toggle selections across later ECE payloads.
- Public UX effect: A new settings field controls the starting Used Level for new games. If the preference exceeds the game Set Level, the game starts at the Set-Level cap. During a game, lowering the dropdown or toggles hides features without lowering retained Used Level, and new payloads do not re-enable toggled-off features.
- Preserved Lichess capability: Lichess still owns account preference submission, board input, move legality, clocks, and lifecycle. The client preference cannot raise Set Level or grant unauthorized coaching.
- Patch map entry: `PM-2026-089`.
- Tests / checks: `UserSettingsTest`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`.
- Upstream update notes: Preserve `preferredUsedLevel` in preference defaults, pref JSON, and round initialization. Do not let Test Ground's last request level become the Set-Level cap.
- Rollback notes: Remove `preferredUsedLevel` settings and initialization together if product reverts to fixed L0 game starts; keep monotonic Used Level behavior.

### INT-2026-090 - V2 deployment polish for token and search/game status surfaces

- Phase: V2-deployment-polish-token-search-surfaces.
- Lichess seam: EvenChess account/play pages, native lobby search-status JSON/UI, and account monetisation presenter copy.
- Lichess files touched: `app/controllers/EvenChess.scala`; `app/views/evenchess/account.scala`; `app/views/evenchess/play.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/table.ts`.
- EvenChess files touched: `modules/evenchess/src/main/AccountMonetisationUi.scala`; `modules/evenchess/src/test/AccountMonetisationUiTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Token/account pages and the lobby search status are deployment-facing surfaces. They should present normal account access, level, preference, bot disclosure, and wait-time information without exposing internal provider callback names, analytics event names, raw token gate keys, ticket ids, expected-offset model fields, bot seed diagnostics, match-contract stage/source diagnostics, or dead checkout links.
- Public UX effect: Account pages now use polished labels for balances, earned tokens, summaries, token settlement, unavailable rewarded-token earning, and unavailable plan upgrades. The lobby search status shows access labels and assigned levels while keeping implementation details out of the browser-facing status contract.
- Preserved Lichess capability: Lichess remains the native lobby and account shell. EvenChess server-side code still owns token gates, MMR matching, bot fallback settings, and game-start policy; the browser receives only the display-safe summary.
- Patch map entry: `PM-2026-090`.
- Tests / checks: `AccountMonetisationUiTest` updated for the polished presenter behavior; focused Scala/UI checks should be run after the patch.
- Upstream update notes: Preserve the narrowed `EvenChessSearchStatus` interface and the controller's display-label JSON when rebasing lobby or controller changes.
- Rollback notes: Revert only the presenter/status polish if a separate internal admin view is needed; do not re-expose internal status fields on public lobby/account pages.

### INT-2026-095 - V2 production availability for Test Ground bridge

- Phase: V2-overlay-live-deployment-readiness.
- Lichess seam: Live round overlay polling adapter and EvenChess Test Ground bridge controller.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`.
- Why this seam exists: The Test Ground bridge had been limited to localhost/dev-only checks, which blocked round overlays on production-hosted online games. The bridge endpoint is now available outside `env.mode.isDev`, and the client-side gate initially suppressed spectator and synthetic rounds.
- Public UX effect: EvenChess overlays can now be requested on online round pages from the same-origin path in production flow, while the browser still never calls ECE directly.
- Preserved Lichess capability: Lichess still owns move legality, round lifecycle, clocks, and player-state; only the existing EvenChess-specific same-origin adapter path was widened on host/protocol handling.
- Patch map entry: `PM-2026-063`.
- Tests / checks: `node ui/test evenchessTestGround.test.ts`; smoke verification of `/evenchess/testground/ece/board-overlay` and `/evenchess/testground/ece/proposed-move` against a running online round.
- Upstream update notes: Keep `/evenchess/testground/*` as same-origin adapter endpoints and reapply this gating behavior if round UI host/protocol assumptions change.
- Rollback notes: Revert the route/controller edits and restore a dedicated environment/host gate if a production live transport replaces this adapter.

### INT-2026-096 - V2 computer-round Test Ground overlay availability

- Phase: V2-computer-round-overlay-availability.
- Lichess seam: Local Test Ground round overlay polling adapter.
- Lichess files touched: `ui/round/src/evenchessTestGround.ts`.
- EvenChess files touched: `ui/round/tests/evenchessTestGround.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Lichess computer games use the synthetic game id, but EvenChess computer games are valid player rounds with Set Level 10. The local ECE bridge must request overlays for those rounds instead of suppressing them as non-live synthetic boards.
- Public UX effect: Computer games can show the EvenChess level shell, coach card, and board overlay path through the same Test Ground ECE bridge used by online games.
- Preserved Lichess capability: Lichess still owns move legality, clocks, Stockfish opponent flow, and game lifecycle. Spectators remain blocked from local Test Ground ECE requests.
- Patch map entry: `PM-2026-092`.
- Tests / checks: `node ui/test evenchessTestGround.test.ts evenchessOverlay.test.ts`; Test Ground `build-ui`; Test Ground `launch-evenchess`.
- Upstream update notes: Keep spectator suppression, but do not suppress player computer rounds solely because their id is `synthetic`.
- Rollback notes: Revert only the synthetic-id allowance if production metadata later distinguishes computer rounds from analysis-board synthetic positions before the local adapter runs.

### INT-2026-097 - V2 computer-round boot hardening for native controls and EvenChess shell

- Phase: V2-round-boot-hardening.
- Lichess seam: Native round module boot sequence and round layout render seam.
- Lichess files touched: `ui/round/src/round.ts`; `ui/round/src/view/main.ts`.
- EvenChess files touched: `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Computer-game rounds were able to remain in the server-rendered static board state when the round boot waited indefinitely on piece preload or when optional EvenChess rendering failed before native controls mounted. The round boot now continues after a bounded preload wait, and EvenChess side/board overlay rendering fails open so clocks, turn/status cards, move controls, and chessground still mount.
- Public UX effect: Opening a Play Against Computer round as the player now shows the native turn/status area, clocks or correspondence turn card, and EvenChess level/coach shell after the rebuilt round asset loads. The EvenChess board overlay remains `pointer-events: none`, and legal move destinations continue to come from the normal Lichess player payload.
- Preserved Lichess capability: Lichess still owns legal moves, chessground input, turn state, clocks, Stockfish opponent flow, and game lifecycle. EvenChess rendering cannot block those native systems from mounting.
- Patch map entry: `PM-2026-093`.
- Tests / checks: Test Ground `build-ui` produced `round.4PUTXK5X.js`; browser verification on authenticated computer game `iyMtQ4UErW82` confirmed the rebuilt bundle, mounted native status/controls, mounted EvenChess live cards/board overlay, and non-blocking board overlay. Raw player-page payload contained legal moves including `e2e3e4`.
- Upstream update notes: Preserve `waitForPiecePreload` bounded wait in `round.ts` and the `safeRenderEvenChessOverlay`/`safeRenderEvenChessBoardOverlay` guards in `view/main.ts` when rebasing.
- Rollback notes: Revert only this boot-hardening patch if the optional overlay shell is removed or if upstream provides an equivalent non-blocking extension point. Do not remove the computer-round Test Ground allowance from `INT-2026-096` unless that separate behavior is intentionally reverted.

### INT-2026-098 - V2 desktop levels/eval/board alignment

- Phase: V2-round-layout-polish.
- Lichess seam: Native desktop round grid and EvenChess live card/eval styling.
- Lichess files touched: `ui/round/css/_layout.scss`.
- EvenChess files touched: `ui/round/css/_evenchess-live.scss`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The live board surface needs the native game metadata card, EvenChess levels card, eval bar, board, coach, and normal round table controls to align as one desktop layout. The levels card now occupies the side column below the game metadata card, matching its width and stretching to the board bottom. The eval bar sits between that side column and the board and spans the same vertical grid rows as the board.
- Public UX effect: On wide desktop rounds, the eval bar top and bottom are flush with the chess board, and the levels card sits directly under the game metadata card rather than as a separate board-height column.
- Preserved Lichess capability: Lichess still owns board sizing, legal moves, clocks, side metadata, and table controls. The change is layout-only.
- Patch map entry: `PM-2026-094`.
- Tests / checks: Test Ground `build-ui` produced round CSS `1b206b7c`; `git diff --check` passed for the touched CSS files. The narrow in-app browser still falls back to the squeezed/mobile layout, where the desktop parent-grid rule does not apply.
- Upstream update notes: Preserve the wide-desktop parent-grid mapping gated by `.round__app.evenchess-live-layout`. Do not move levels back into a board-height column inside `.round__app` unless the native side-column extension point changes.
- Rollback notes: Revert this layout polish only if the product chooses the earlier board-adjacent levels column. Keep the round boot hardening from `INT-2026-097`.

### INT-2026-099 - V2 live ECE move refresh and proposed-move cached toggle

- Phase: V2-live-ECE-refresh-and-proposed-move.
- Lichess seam: Native round post-move plugin update path, EvenChess Test Ground ECE bridge polling adapter, and EvenChess proposed-move card state.
- Lichess files touched: `ui/round/src/ctrl.ts`.
- EvenChess files touched: `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`.
- Why this seam exists: After a committed move, EvenChess must request and apply the current board-state payload immediately through the same-origin ECL bridge. If another ECE request is already in flight, a forced refresh must stay queued so the current position is fetched instead of waiting for a manual page refresh. If the in-flight response returns for an older ply/FEN, the adapter must immediately retry the current board instead of silently discarding it and waiting for refresh. Local/computer rounds can receive the engine reply shortly after the player's move, so non-duplicate delayed checks catch the final current FEN.
- Public UX effect: Board overlays and audit/cache payloads update after moves. Visible coach text still refreshes only on the player's turn, using the authoritative round active color instead of transient chessground turn state, and opponent-turn payloads do not replace the saved coach text snapshot. The Proposed Move button now toggles the active cached same-turn preview back to the normal board-state display and can re-show it without another ECE call.
- Preserved Lichess capability: Lichess still owns move legality, clocks, round lifecycle, and chessground input. The browser continues to call only ECL same-origin endpoints; ECL remains responsible for server-to-server ECE calls.
- Patch map entry: `PM-2026-095`.
- Tests / checks: `node ui/test round/tests/evenchessTestGround.test.ts round/tests/evenchessOverlay.test.ts`; `pnpm exec tsc -p ui/round/tsconfig.json --noEmit`; Test Ground `build-ui` produced `round.Q53Z25I3.js`.
- Upstream update notes: Preserve the post-position-change ECE refresh helper, stale-response current-board retry, delayed non-duplicate checks, authoritative round-active-color coach gate, and queued forced refresh state in the Test Ground adapter.
- Rollback notes: Revert the forced refresh and queued-force behavior only if a server-pushed live ECE transport replaces the polling adapter. Keep proposed-move quota/cache enforcement unless the ECE contract changes.

### INT-2026-100 - V2 board-measured levels/eval sizing

- Phase: V2-round-layout-hardening.
- Lichess seam: Native round view root and desktop round grid.
- Lichess files touched: `ui/round/src/view/main.ts`; `ui/round/css/_layout.scss`.
- EvenChess files touched: `ui/round/css/_evenchess-live.scss`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The eval bar and level selector must follow the rendered chess board, not the viewport height or the native move table rows. The EvenChess layout now measures the board and native game metadata card and writes CSS variables that keep the eval rail board-height and the levels card bottom-aligned with the board.
- Public UX effect: Zooming or changing viewport height no longer makes the eval rail or level card grow beyond the board. The level list scrolls inside the fixed card.
- Preserved Lichess capability: Lichess still owns board rendering, move table, clocks, controls, and responsive breakpoints. The measurement hook only writes EvenChess CSS variables when the EvenChess live shell is present.
- Patch map entry: `PM-2026-096`.
- Tests / checks: `pnpm exec tsc -p ui/round/tsconfig.json --noEmit`; Test Ground `build-ui` produced `round.O6NV6DSZ.js` and CSS `c918b571`; browser measurement confirmed a 389px board, 389px eval rail, and 389px levels card in the active round viewport.
- Upstream update notes: Preserve `evenChessLayoutHook` if the round app root changes, and keep `.evenchess-live__eval` / `.evenchess-live__card--levels` sized from `--evenchess-board-height` / `--evenchess-levels-height`.
- Rollback notes: Revert this hook and CSS variable sizing only if the round layout moves to a native board-adjacent extension point that can provide board-height tracks directly.

### INT-2026-101 - V2 local Test Ground browser move bridge

- Phase: V2-Test-Ground-round-automation.
- Lichess seam: Native round controller playable-move path.
- Lichess files touched: `ui/round/src/ctrl.ts`.
- EvenChess files touched: `ui/round/src/evenchessTestGroundMoveBridge.ts`; `ui/round/tests/evenchessTestGroundMoveBridge.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Codex in-app browser automation could hit the Chessground board element but could not reliably trigger Chessground's native click/drag move handling, and its read-only page context cannot dispatch DOM events. Test Ground needs a reliable way to make one legal move so post-move ECE payload refresh, overlay redraw, and coach-text cadence can be visually verified. The bridge supports both a normal DOM event and a one-shot localhost URL parameter such as `?evenchessTestMove=e2e4`.
- Public UX effect: None. The bridge is localhost-only and event-driven; normal player clicks, drags, legal move generation, clocks, and game lifecycle remain unchanged.
- Preserved Lichess capability: Lichess still owns move legality and socket submission. The bridge validates against the current `possibleMoves` payload and delegates to the existing `pluginMove` path instead of creating a separate move API.
- Patch map entry: `PM-2026-097`.
- Tests / checks: `pnpm exec tsx --test ui/round/tests/evenchessTestGroundMoveBridge.test.ts ui/round/tests/evenchessTestGround.test.ts`; `./ui/build -n`; browser URL-parameter smoke on an authenticated computer game.
- Upstream update notes: Preserve the localhost guard, active-player/current-turn checks, legal destination validation, and `pluginMove` delegation. Do not turn this into a production browser move API.
- Rollback notes: Remove `evenchessTestGroundMoveBridge.ts`, the controller listener, and the focused test if the browser automation layer can directly drive Chessground again.

### INT-2026-102 - V2 full-FEN ECE round adapter keys

- Phase: V2-live-ECE-post-move-rendering.
- Lichess seam: Native round post-move controller path and EvenChess round overlay renderer.
- Lichess files touched: `ui/round/src/ctrl.ts`.
- EvenChess files touched: `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The live round `step.fen` value can be board-placement only, but ECE requires full FEN. The Test Ground ECE request, overlay stale comparison, proposed-move cache key, and move-played clear instruction must all use the same full-FEN board-state key or the UI will treat valid returned payloads as missing/stale until a manual refresh path supplies a full FEN.
- Public UX effect: After a player move, the same-origin ECE bridge can return a payload that applies to the current board immediately. Coach text cadence remains unchanged: text updates on the player's turn, while board overlays remain available from the current payload.
- Preserved Lichess capability: Lichess still owns move legality, clocks, turn state, and game lifecycle. The browser still calls only same-origin ECL endpoints; ECL remains the server-to-server ECE caller.
- Patch map entry: `PM-2026-098`.
- Tests / checks: `pnpm exec tsx --test ui/round/tests/evenchessTestGround.test.ts ui/round/tests/evenchessTestGroundMoveBridge.test.ts`; Test Ground browser move smoke after rebuild.
- Upstream update notes: Preserve the normalized full-FEN board-state key if upstream changes round step data or FEN storage. Do not compare ECE payloads against raw board-placement FENs.
- Rollback notes: Revert this adapter key normalization only if ECE changes its board-state contract to accept and return the same board-placement-only key, or if the round server supplies an authoritative full-FEN key directly.

### INT-2026-103 - V2 proposed/potential move split and quotas

- Phase: V2-ECE-contract-alignment.
- Lichess seam: Same-origin EvenChess Test Ground ECE bridge and native round overlay renderer.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE v1.2 separates board-state potential moves from explicit proposed-move what-if calls. ECL owns the browser buttons, reveal limits, and per-game quota state, while ECE remains a server-side service. The bridge now accepts proposed-move coaching text without requiring a public `legal` display field, and the round overlay no longer auto-renders potential/candidate arrows merely because the payload contains them.
- Public UX effect: The coach column exposes Potential Moves buttons and a Proposed Move button with visible used/quota counts. Potential arrows appear only after the matching reveal action for the current FEN/Used Level. Proposed Move previews remain separate cards and cache/re-toggle the same arrow without spending another call.
- Preserved Lichess capability: Lichess still owns move legality, clocks, current turn, board input, and game lifecycle. Browser code still calls only same-origin ECL endpoints; ECL remains the server-to-server ECE caller.
- Patch map entry: `PM-2026-099`.
- Tests / checks: `pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts`; `pnpm exec tsx --test ui/round/tests/evenchessTestGround.test.ts`; `./lila.sh "evenchess/compile"` through Docker sbt fallback.
- Upstream update notes: Preserve optional proposed `legal` handling, public "Potential" terminology, hidden-by-default potential arrows, user-turn gating for player potential reveals, and per-game quotas of Proposed L5=1/L6-L7=2/L8-L10=3.
- Rollback notes: Revert this entry only if ECE collapses candidate and proposed assistance back into one feature. Otherwise keep candidate payload fields mapped to Potential Moves in public ECL UI.

### INT-2026-104 - V2 coach-card board-top alignment and eval strip

- Phase: V2-live-overlay-polish.
- Lichess seam: Native round desktop grid and EvenChess round overlay renderer.
- Lichess files touched: `ui/round/css/_layout.scss`; `ui/round/css/_app-layout.scss`.
- EvenChess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The native desktop round grid placed the top-right row in the `voice` area, which pushed the EvenChess coach card below the board top. The EvenChess live layout now maps the coach column into the top board row while leaving the already board-height eval rail and bottom-aligned level card intact.
- Public UX effect: The coach card starts flush with the chess board top. When L8 eval text is enabled and the payload contains eval/WDL/precision data, a compact traffic-light eval strip renders at the top of the coach card.
- Preserved Lichess capability: Lichess still owns board sizing, voice controls, move table, legal moves, clocks, and game lifecycle. ECE remains same-origin/server-to-server only.
- Patch map entry: `PM-2026-100`.
- Tests / checks: `ui/round/tests/evenchessOverlay.test.ts` covers the coach-card eval strip and separate Eval Text toggle behavior. Browser verification measures board and coach card top alignment after rebuild.
- Upstream update notes: Preserve the EvenChess-live coach area starting in the top board row and keep eval text tied to `evalNumbers`, not the vertical `evalBar` toggle.
- Rollback notes: Revert the coach grid-row mapping and eval-strip renderer if the round layout gains a native top-aligned coach extension point or if eval text moves to another approved surface.

### INT-2026-105 - V2 numeric ECE eval normalization for coach strip

- Phase: V2-live-overlay-polish.
- Lichess seam: Same-origin EvenChess Test Ground ECE bridge.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE quick board-state payloads can return `evaluation` as a number or numeric string, while deep payloads can return an object with `cp`, `centipawns`, `score`, or related fields. The controller now normalizes all supported public eval shapes into an approved `ece.eval` visual before the browser receives the round overlay payload.
- Public UX effect: The L8 Eval Text coach-card strip has a numeric source from quick and deep ECE payloads instead of only appearing for object-shaped deep evals.
- Preserved Lichess capability: Browser code still calls only the same-origin ECL bridge. ECL continues to call ECE server-to-server and does not expose private provider paths, raw prompts, or unrestricted engine output.
- Patch map entry: `PM-2026-101`.
- Tests / checks: `./lila.sh "compile"` confirms the controller patch; `pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts` confirms the renderer consumes normalized `ece.eval` visuals.
- Upstream update notes: Preserve numeric and object-shaped eval normalization if the Test Ground bridge moves to a module or production live scheduler.
- Rollback notes: Revert only if ECE guarantees a single object-shaped eval payload forever and the bridge parser is updated accordingly.

### INT-2026-106 - V2 attackable hanging-piece ownership normalization

- Phase: V2-live-overlay-polish.
- Lichess seam: Same-origin EvenChess Test Ground ECE bridge.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE may return attackable hanging pieces as object payloads or simple square strings. The bridge now extracts square strings such as `"g4"`, accepts explicit `student_hanging_attackable` and `opponent_hanging_attackable` arrays from either `overlays` or `overlays.trade_status`, and recognizes additional owner keys such as `piece_owner`, `target_owner`, `owner_color`, and colour spellings.
- Public UX effect: Student-owned attackable hanging pieces render with the red exclamation badge and red inner rim. Opponent-owned attackable hanging pieces render with the purple exclamation badge and purple inner rim when ECE provides ownership or an opponent-specific array.
- Preserved Lichess capability: Browser code still receives only approved round overlay atoms from ECL. ECE remains server-to-server and private provider details remain hidden.
- Patch map entry: `PM-2026-102`.
- Tests / checks: `./lila.sh "compile"` confirms the controller patch; `pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts` confirms board renderer behavior for student/opponent attackable hanging markers.
- Upstream update notes: Preserve string-square extraction and owner-specific hanging-piece mapping if the bridge parser moves out of the controller.
- Rollback notes: Revert only if ECE guarantees a single object-shaped schema with stable `owner_side` and `square` fields.

### INT-2026-107 - V2 server-side proposed/potential move consumables

- Phase: V2-live-assistance-consumables.
- Lichess seam: Same-origin EvenChess Test Ground ECE bridge, route table, and native round overlay controls.
- Lichess files touched: `app/controllers/EvenChess.scala`; `conf/routes`; `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Proposed and potential move uses are consumable assistance. They must be counted server-side by ECL, not by volatile browser state, and potential move data must not be readable from ordinary board-state payloads before the server authorizes a reveal.
- Public UX effect: Refreshing the browser no longer restores proposed/potential move uses in the local Test Ground bridge. Potential move arrows/text appear only after the Potential Moves button receives an authorized reveal; clicking the active reveal hides it, and clicking again re-shows the already-authorized cached reveal without another use. Active potential moves are also listed at the bottom of the coach card.
- Preserved Lichess capability: Lichess still owns legal moves, clocks, board rendering, and game lifecycle. Browser code still calls only same-origin ECL endpoints; ECL remains the server-to-server ECE caller.
- Patch map entry: `PM-2026-103`.
- Tests / checks: `pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts ui/round/tests/evenchessTestGround.test.ts`; `./lila.sh playRoutes`; `./ui/build -n`; `./lila.sh "compile"` via Docker sbt fallback.
- Upstream update notes: Preserve server-side proposed/potential usage counters, same-FEN cached reveal semantics, and potential-data suppression if the Test Ground bridge is replaced by production live scheduling.
- Rollback notes: Stop relying on controller process memory only after a production game-assistance persistence layer is available; do not return to browser-only usage counters.

### INT-2026-112 - V2 two-state EvenChess matchmaking and MMR preference rehaul

- Phase: V2-matchmaking-MMR-rehaul.
- Lichess seam: Native lobby setup modal/status card, setup controller submission/polling, and same-origin EvenChess search JSON controller.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/view/table.ts`.
- EvenChess files touched: `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/evenchess/src/test/TelemetryAnalyticsTest.scala`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Public EvenChess search must be embedded in the native Lichess setup flow while EvenChess MMR owns all level-offset windows, Set Level assignment, bot/simulation ticket matching, and rated/casual match contracts. The browser now sends one optional Preferred Set Level value only; Any means normal search and L0-L10 fixes/prefers the requester's own Set Level.
- Public UX effect: The search card no longer exposes Apply Preferences, Opponent Target Level, Search until preferences are met, or manual rating-window controls. Status JSON exposes only deployment-safe labels, assigned levels, preference flags, and uneven-match notices.
- Preserved Lichess capability: Lichess still owns accounts, legal moves, clocks, game shell, and game lifecycle. Normal Lichess ratings remain separate; EvenChess ECR settlement uses the EvenChess path and actual Used Offset.
- Patch map entry: `PM-2026-108`.
- Tests / checks: Focused EvenChess Scala tests verify two-state preferences, dynamic level-offset assignment, simulation-bot matching, compatibility parsing, and rating/telemetry flag construction. Focused lobby unit tests verify the simplified scenario helper.
- Upstream update notes: Preserve the single Preferred Set Level dropdown, table-derived rating windows, server-side Set Level assignment, and deployment-safe public status if upstream lobby setup or controller code changes.
- Rollback notes: Revert this entry only if product policy restores opponent-target/strict/manual-window controls. Otherwise keep old query fields as server-side compatibility aliases only and do not re-expose them in public UI.

### INT-2026-113 - ECOR table calibration and admin controls

- Phase: V2-ECOR-rating-calibration.
- Lichess seam: Existing dev/settings admin shell, SettingStore-backed backend settings, secure dev routes, and EvenChess search-controller activation of runtime calibration tables.
- Lichess files touched: `app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `conf/routes`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`.
- EvenChess files touched: `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/AdminOpsDashboard.scala`; `modules/evenchess/src/main/EvenChessRatingCalibration.scala`; `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/AdminOpsDashboardTest.scala`; `modules/evenchess/src/test/EvenChessRatingCalibrationTest.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECOR is an EvenChess fairness policy table, but operators need to adjust it and review calibration statistics through the existing Lichess admin/settings shell. Matchmaking and expected-offset calculations must use the active admin table while public search surfaces continue to hide raw offset internals.
- Public UX effect: None. ECOR controls are admin-only. Public matchmaking behavior changes only through server-authoritative policy after an admin applies a table.
- Preserved Lichess capability: Lichess still owns accounts, legal moves, clocks, challenge/game creation, and admin auth/CSRF. Normal Lichess ratings remain separate from ECR/ECOR.
- Patch map entry: `PM-2026-109`.
- Tests / checks: Focused Scala tests cover ECOR parsing, default gap/rating tables, calibration statistics, snapshot history, admin settings/dashboard exposure, and rated-game sample collection.
- Upstream update notes: Preserve `/dev/evenchess/ops/ecor`, the ECOR SettingStore entries, and runtime activation before search ticket creation if upstream dev settings or route code changes.
- Rollback notes: Restore a prior ECOR snapshot through the admin panel before reverting code. If reverting code, disable table changes by restoring the compiled default gap table and keep any collected calibration data out of public JSON.

### INT-2026-114 - EvenChess friend challenge level contracts

- Phase: V2-friend-challenge-contracts.
- Lichess seam: Native friend setup submit path, challenge model/JSON, and challenge request card rendering.
- Lichess files touched: `app/controllers/Challenge.scala`; `app/controllers/EvenChess.scala`; `app/controllers/Setup.scala`; `modules/challenge/src/main/Challenge.scala`; `modules/challenge/src/main/JsonView.scala`; `modules/challenge/src/main/ui/ChallengeUi.scala`; `ui/challenge/src/interfaces.ts`; `ui/challenge/src/view.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/view/setup/modal.ts`.
- EvenChess files touched: `modules/evenchess/src/main/GamePolicy.scala`; `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Friend challenges have a known recipient and must not be converted into generic public EvenChess pool searches. The setup modal now submits friend challenges through native `/setup/friend`, carries the selected friend-level mode, resolves the levels server-side from the active ECOR/base-level tables, stores/renders a deployment-safe summary on the challenge for the recipient card, and persists the resolved Set Levels when the friend accepts.
- Public UX effect: Challenge-a-friend setup shows Auto level, Set my level, Set opponent level, and Set both levels. The recipient sees the resolved EvenChess level settings before accepting.
- Preserved Lichess capability: Lichess still owns challenge creation, challenge lifecycle, legal moves, clocks, and game shell. Browser code does not decide final ECOR fairness internals and does not receive raw MMR diagnostics.
- Patch map entry: `PM-2026-110`.
- Tests / checks: Focused EvenChess MMR tests cover all friend-level modes. Full Scala compile verifies challenge/setup serialization and controller integration. Focused lobby test and oxlint verify changed setup UI files.
- Upstream update notes: Preserve native friend routing and the public challenge summary if upstream changes setup modal submit logic or challenge card rendering.
- Rollback notes: Remove the friend-specific modal fields and challenge metadata only if a replacement explicit EvenChess friend-contract route carries the recipient and displays the resolved settings before acceptance.

### INT-2026-115 - Mirrored side-output Offset Count contract

- Phase: V2-ECE-offset-contract-alignment.
- Lichess seam: Same-origin ECL-to-ECE bridge and round board overlay renderer tests.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE now sends Offset Count as mirrored deterministic board facts to both side outputs. `piece_count_delta` is already signed from the receiving side output's perspective, and target ownership is no longer restricted to opponent pieces.
- Public UX effect: When the Offset Count toggle is on, the user sees every delivered offset marker for their selected side output: positive as green, equal as blue shield, and negative as red, regardless of whether the target is their own piece or the opponent's piece.
- Preserved Lichess capability: Browser code still calls only same-origin ECL routes. ECE remains a private server-to-server service and ECL continues to sanitize public display atoms before the round client receives them.
- Patch map entry: `PM-2026-111`.
- Tests / checks: Test ECE fixture test covers mirrored `target_square` entries and flipped signed deltas for white/black. Round overlay test covers rendering all signed offset visuals and hiding all of them through the Offset Count toggle.
- Upstream update notes: Preserve `target_square` priority and `piece_count_delta` signed-value priority in the ECE payload adapter when controller or live scheduler seams are refactored.
- Rollback notes: Revert only if ECE reverts the public contract to attacker-side-only Offset Count. Otherwise, filtering to opponent targets or trusting legacy `student_first_move` will hide valid defensive markers.

### INT-2026-116 - Preferred set level submit normalization

- Phase: V2-matchmaking-preference-contract-cleanup.
- Lichess seam: Native lobby setup submit path for EvenChess hook searches.
- Lichess files touched: `ui/lobby/src/setupCtrl.ts`.
- EvenChess files touched: `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The simplified public search contract treats Any/no preference as absence of `preferredSetLevel`. Stale stored `"any"` must not be serialized as a query param.
- Public UX effect: None visually. Search requests now carry a cleaner contract: no preference omits `preferredSetLevel`; concrete preferred levels submit `0` through `10`.
- Preserved Lichess capability: Native lobby setup remains responsible for modal state and search submission. The backend still tolerates old values as a compatibility fallback.
- Patch map entry: `PM-2026-112`.
- Tests / checks: Focused lobby setup helper test verifies empty/Any omission and L0-L10 retention.
- Upstream update notes: Preserve `evenChessPreferredSetLevelParam` or equivalent normalization if upstream changes setup submission code.
- Rollback notes: Reverting this only restores redundant `preferredSetLevel=any` submissions; backend compatibility means no data migration is needed.

### INT-2026-117 - Quick-pairing cards route through EvenChess search contracts

- Phase: V2-matchmaking-bot-fallback-integration.
- Lichess seam: Native lobby quick-pairing card click handler, pool active/spinner rendering, and EvenChess setup/search polling branch.
- Lichess files touched: `ui/lobby/src/ctrl.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/view/pools.ts`; `ui/lobby/src/view/tabs.ts`; `ui/lobby/src/view/table.ts`.
- EvenChess files touched: `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Public EvenChess quick search must use the clicked native clock card and remembered EvenChess search settings, but it must create an EvenChess match contract so MMR, Set Level assignment, bot fallback, and game policy persistence can run before game start.
- Public UX effect: The verbose EvenChess search-status card is hidden by default. Quick-pairing cards show the native active spinner while the hidden EvenChess search polls. With matchmaking bots enabled, a low-pool search redirects into a bot-created game after the configured fallback timeout.
- Preserved Lichess capability: Anonymous quick-pairing still uses the existing anonymous path. Authenticated EvenChess quick search does not send native `poolIn` for public EvenChess play, avoiding duplicate matchmaking tickets.
- Patch map entry: `PM-2026-113`.
- Tests / checks: Focused lobby helper tests, EvenChess search integration test, UI build/type check, and browser verification for debug and non-debug quick-pairing flows.
- Upstream update notes: Preserve `evenChessPoolMember` or equivalent separate active marker when updating lobby quick-pairing code. Do not replace it with `poolMember`, because `poolMember` triggers native pool socket joins.
- Rollback notes: Disable matchmaking bots before rollback if testing live. Reverting this returns quick cards to native pool tickets and will bypass EvenChess bot fallback/MMR contracts.

## Phase J Reconciliation Summary

### INT-2026-091 - V1.1 bot simulation admin operations and Test Ground embedding

- Phase: ECE-1.1-Phase-J-bot-operations-admin.
- Lichess seam: Dev/admin routes and UI (`/dev/evenchess/ops/bots`), EvenChess search repository/runtime seam, and local Test Ground panel embedding.
- Lichess files touched: `conf/routes`; `app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/web/src/main/ui/DevUi.scala`.
- EvenChess files touched: `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/AdminOpsDashboard.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/AdminOpsDashboardTest.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `scripts/evenchess-testground-panel.js`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/requirements/plan_version_1.1/PHASE_J_BOT_OPERATIONS_ADMIN.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Bot matchmaking fallback and synthetic simulation affect public search behavior and must be controlled by admin accounts. The admin panel now exposes matchmaking fallback settings, simulation settings, start/stop/seed actions, and monitoring. Simulation tickets are written into the same local search repository and MMR pipeline used by normal EvenChess search.
- Public UX effect: Public search status remains deployment-safe. The new controls are behind the existing Settings permission. Test Ground embeds the authenticated admin panel for local workflow rather than adding unauthenticated bot-control endpoints.
- Preserved Lichess capability: Lichess still owns accounts, game lifecycle, legal moves, clocks, and native admin auth/CSRF. EvenChess simulation bots are tagged by ticket prefix and can be cleared without deleting human search records.
- Patch map entry: `PM-2026-091`.
- Tests / checks: `BotOperationsTest` covers simulation config, seeding, simulation-ticket-only clearing, and admin-state monitoring. `AdminBackendSettingsTest` and `AdminOpsDashboardTest` cover settings/model inclusion. Node syntax check covers Test Ground panel changes.
- Upstream update notes: Preserve `/dev/evenchess/ops/bots` as admin-only. Do not move bot start/stop controls into unauthenticated local launcher endpoints.
- Rollback notes: Stop simulation, clear `ec-sim-` tickets, disable `evenchess.backend.simulation.enabled`, and disable `evenchess.backend.matchmaking.botModeEnabled` before reverting the admin route/view and runtime.

### INT-2026-092 - V1.1 requirements deployment-readiness sweep

- Phase: ECE-1.1-requirements-deployment-readiness.
- Lichess seam: Requirements, patch map, and integration-log documentation for the bot operations surface.
- Lichess files touched: none.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/requirements/plan_version_1.1/PLAN.md`; `docs/requirements/plan_version_1.1/PHASE_A_REQUIREMENTS_LOCK.md`; `docs/requirements/plan_version_1.1/PHASE_I_ECE_BRIDGE_VERIFICATION.md`; `docs/requirements/plan_version_1.1/PHASE_J_BOT_OPERATIONS_ADMIN.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The active v1.1 requirements needed to match the implemented admin bot operations surface and distinguish deployment-ready controls from the remaining production game-execution and dedicated telemetry boundaries.
- Public UX effect: No public UI change. Requirements now state that public search disclosure remains deployment-safe and that Test Ground uses the authenticated admin panel rather than unauthenticated bot-control APIs.
- Preserved Lichess capability: No Lichess runtime code was changed in this sweep.
- Patch map entry: `PM-2026-091`.
- Tests / checks: Documentation sweep plus the Phase J focused compile/test set.
- Upstream update notes: Preserve the platform-bot terminology and admin-only control boundary when updating v1.1 bot requirements.
- Rollback notes: Revert only these requirement/log clarifications if product policy changes terminology or scope; do not remove the Phase J admin controls without also following `INT-2026-091` rollback.

### INT-2026-093 - V2 Test Ground ECE CLM launch

- Phase: V2-Test-ground-ECE-CLM-launch.
- Lichess seam: Local Windows Test Ground launcher and browser panel only.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE CLM is a private offline composer-learning tool in the sibling ECE repo. Local Test Ground should be able to launch and open it for development without treating it as the live ECE endpoint. The Test Ground panel now uses `127.0.0.1:8791` so CLM can keep its documented `127.0.0.1:8790/clm` URL.
- Public UX effect: None. This affects only local Test Ground workflow.
- Preserved Lichess capability: No production Lichess route, gameplay, account, or browser-to-ECE boundary changes. CLM remains local-only and separate from live ECE.
- Patch map entry: None.
- Tests / checks: PowerShell parser check, Node syntax check, panel ping/status smoke, `/clm` proxy smoke, `/api/clm/status` proxy smoke, and full CLM stop/start lifecycle through `stop-ece-clm-linux.sh` and `start-ece-clm-linux.sh`.
- Upstream update notes: Keep CLM launch controls in local Test Ground tooling. Do not expose CLM in production Lichess UI or route browser clients to ECE internals. The launcher must replace stale panel processes and route browser CLM traffic through `127.0.0.1:8791`, not directly to the WSL-only CLM port.
- Rollback notes: Revert the CLM controls and restore the prior panel port only if CLM moves away from `8790`; otherwise the port collision will return.

### INT-2026-122 - V2 Test Ground ECE settings page open

- Phase: V2-Test-ground-ECE-settings-page-open.
- Lichess seam: Local Windows Test Ground launcher and browser panel only.
- Lichess files touched: none.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE now exposes a local/private read-only operator settings page at `{ECE_BASE_URL}/ece/settings`. Test Ground should open that diagnostic page from the ECE section without creating browser-callable board/proposed/game-review analysis paths.
- Public UX effect: None. This affects only local Test Ground workflow.
- Preserved Lichess capability: No production Lichess route, gameplay, account, or browser-to-ECE analysis boundary changes. Browser analysis calls to ECE remain forbidden.
- Patch map entry: None.
- Tests / checks: PowerShell parser check and Node syntax check.
- Upstream update notes: Keep ECE settings as a local diagnostics link only. Do not proxy or expose ECE analysis endpoints through Test Ground browser controls.
- Rollback notes: Remove the settings link/action if the ECE operator page path changes or is disabled.

### INT-2026-118 - V2 admin-editable Stockfish equivalent rating table

- Phase: V2-bot-matchmaking-strength-calibration.
- Lichess seam: Dev/admin settings registration, Dev controller admin action, Dev UI ECOR/calibration panel, and public EvenChess search runtime-table activation.
- Lichess files touched: `app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`.
- EvenChess files touched: `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/AdminOpsDashboard.scala`; `modules/evenchess/src/main/EvenChessRatingCalibration.scala`; `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/AdminOpsDashboardTest.scala`; `modules/evenchess/src/test/EvenChessRatingCalibrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Platform-bot handoff to native Lichess AI must use approximate rating-equivalent Stockfish levels, and admins need to inspect/adjust that mapping as real data improves.
- Public UX effect: No new public setting. Search and bot fallback remain deployment-safe; only the backend-selected native AI strength changes according to the active admin table.
- Preserved Lichess capability: Lichess still owns native AI/computer game creation and clocks. EvenChess only chooses the bounded native AI level from a validated admin table before handoff.
- Patch map entry: `PM-2026-114`.
- Tests / checks: `EvenChessRatingCalibrationTest` covers table parsing and validation; `AdminBackendSettingsTest` and `AdminOpsDashboardTest` cover settings/model exposure; full EvenChess test run should remain green.
- Upstream update notes: Preserve `evenchess.backend.stockfish.equivalentRatingBands`, the admin-only update action, and search-time runtime activation when admin/settings code changes.
- Rollback notes: Revert the setting/action/runtime activation and return `LichessEquivalentStockfishLevel` to static defaults only if bot fallback is disabled first.

### INT-2026-119 - V1.1 simulation bot configured ratings and playable pools

- Phase: ECE-1.1-simulation-bot-lifecycle.
- Lichess seam: None. This updates EvenChess-owned search/MMR preparation and bot-operations tests.
- Lichess files touched: none.
- EvenChess files touched: `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Simulation mode should behave like a configured pool of human-like queue participants. Bot-profile search tickets now persist the profile target ECR as the active ticket rating, reseeding recreates consumed simulation tickets while preserving the requested active count, and seeding distributes bots across playable time-control pools in both rated and casual scopes so a human quick search can find same-pool candidates.
- Public UX effect: Human searches can match simulation bots even when timed fallback matchmaking bots are off. The public search flow still uses normal native Lichess searching indicators and same-origin game redirects.
- Preserved Lichess capability: Lichess still owns native AI/computer game creation, legal moves, and clock handling. Simulation bot handoff uses native AI config rather than custom move/timer code.
- Patch map entry: `PM-2026-115`.
- Tests / checks: `BotOperationsTest` covers configured rating spread, playable time-control distribution, and replenishment after a consumed ticket. `PlaySearchIntegrationTest` covers bot-profile target ECR persistence.
- Upstream update notes: Preserve bot-profile target ECR when replacing the local in-memory repository with production persistence.
- Rollback notes: Disable simulation mode and clear `ec-sim-` tickets before reverting; otherwise stale simulation records may remain in the local repository.

### INT-2026-121 - V2 proposed-move post-move side-output preview

- Phase: V2-proposed-move-post-move-preview.
- Lichess seam: Same-origin ECE bridge and native round overlay rendering.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `modules/evenchess/src/main/EceLiveBridge.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE proposed-move responses now return deterministic post-move display data as `proposed_move_evaluation.after_move_side_output`, nested under the proposed-move evaluation. ECL must parse that legal-only nested side output server-side, normalize it through the existing Display Engine path, and let the round UI toggle the cached post-move preview against the cached normal board-state payload. If legal post-move cards/visuals are present but `new_fen` is absent, ECL still sends the normalized payload identity as the preview board-state key so the browser applies the approved display payload instead of suppressing it. The browser overlay branch uses the nested post-move audit id when validating child cards/visuals, because the outer proposed-call text card can legitimately use a different audit id.
- Public UX effect: A legal proposed-move request can show the board/coach as if the arrow move had happened, while pressing Proposed Move again restores the actual-position display without consuming another token. Illegal/invalid proposed moves still show only advice/error text and do not render post-move overlays.
- Preserved Lichess capability: Browser code still calls only same-origin ECL endpoints. ECL remains the server-to-server caller to ECE and does not expose ECE internals, shared calculations, provider outputs, or paths.
- Patch map entry: `PM-2026-117`.
- Tests / checks: `ui/round/tests/evenchessOverlay.test.ts` covers cached post-move preview rendering and toggle-back behavior.
- Upstream update notes: Preserve the distinction between normal board-state payloads and proposed-move preview payloads when changing round rendering. Do not treat `after_move_side_output` as top-level `side_outputs`.
- Rollback notes: Remove the nested-side-output parse and UI preview substitution; keep the basic proposed-move text card path if ECE still returns `sentence`/`coaching.text`.

### INT-2026-123 - V2 stable live overlay refresh during ECE loads

- Phase: V2-live-overlay-refresh-polish.
- Lichess seam: Native round overlay rendering.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE board-state refreshes can have a short loading gap after each move. The round UI should not remount the whole EvenChess live shell or blank safe coach text during that gap, but it also must not show stale board-square visuals from the previous FEN.
- Public UX effect: The EvenChess sidebar stays visually stable between move and ECE response. Board-attached arrows, highlights, and markers still disappear while stale and reappear only from a current-position payload.
- Preserved Lichess capability: Normal round rendering, legal moves, clocks, and stale-payload safety remain unchanged.
- Patch map entry: `PM-2026-118`.
- Tests / checks: `ui/round/tests/evenchessOverlay.test.ts` verifies stable keys, retained safe coach text, and hidden stale board visuals across a move-triggered refresh.
- Upstream update notes: Preserve stable keys on the EvenChess live shell and board overlay root if upstream changes round view patching.
- Rollback notes: Reverting restores the visible remount/loading blink but does not affect ECE payload validity or game authority.

### INT-2026-124 - V2 potential/proposed move side-state correction

- Phase: V2-potential-proposed-move-state-correction.
- Lichess seam: Same-origin ECE bridge and native round overlay rendering.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Opponent Potential Moves must reveal the opponent side-output/perspective while counting against the requester's allowance. Proposed Move is a state toggle; invalid input must not clear or replace an active legal preview.
- Public UX effect: A white player pressing Opponent Potentials receives black-side potential moves. Pressing Proposed Move with an illegal/multiple/non-actionable green arrow leaves the current overlay unchanged except for an optional status message.
- Preserved Lichess capability: Browser calls remain same-origin ECL calls. ECL still performs the ECE server-to-server call and stale/game/FEN validation.
- Patch map entry: `PM-2026-119`.
- Tests / checks: `ui/round/tests/evenchessOverlay.test.ts` verifies opponent-side perspective and invalid proposed-click preservation.
- Upstream update notes: Preserve the distinction between requester side for authorization/accounting and reveal side for opponent potential payloads.
- Rollback notes: Reverting may make opponent potential reveals appear as requester-side moves again and may restore the invalid-click overlay blanking behavior.

### INT-2026-127 - V2 saved match ECE payload history and L10 full-game review backfill

- Phase: V2-analysis-memory-history-review.
- Lichess seam: Same-origin ECL/Test Ground ECE bridge, route table, analysis-memory model, and native round replay overlay rendering.
- Lichess files touched: `app/controllers/EvenChess.scala`; `conf/routes`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `modules/evenchess/src/main/AnalysisMemory.scala`; `modules/evenchess/src/main/EceLiveBridge.scala`; `modules/evenchess/src/test/AnalysisMemoryTest.scala`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`; `ui/round/tests/evenchessTestGround.test.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_FULL_MATCH_PAYLOAD_CONTRACT.md`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Match history needs an EvenChess PGN-like record: one approved, display-safe ECE overlay payload per FEN/ply/side, keeping the highest delivered level and excluding proposed-move previews. Replay should read saved frames first, and a level-10 review action should call ECE server-to-server for full-game frame data before storing it in the same renderer-compatible format.
- Public UX effect: Replay/history can show saved EvenChess coach/overlay payloads without recalculating when a frame already exists. During replay, the user gets a `Run L10 Review` control to request a level-10 full-game refresh for that game's frames.
- Preserved Lichess capability: Browser code still calls only same-origin ECL endpoints. ECE remains private and owns `/v1/ece/game-review`; ECL only stores sanitized approved display payloads and does not store proposed-move previews.
- Patch map entry: `PM-2026-122`.
- Tests / checks: `AnalysisMemoryTest`, `EceLiveBridgeTest`, `evenchessTestGround.test.ts`, `evenchessOverlay.test.ts`, `evenchess/compile`, and `evenchess/test`.
- Upstream update notes: Preserve history-only cache lookup on replay, full-game review as a server-side ECE call, and the same live-overlay payload shape for saved review frames.
- Rollback notes: Remove the full-game review route/UI first, then disable history-only replay lookup. Keep live board-state ECE calls intact so live games continue to render current payloads.

### INT-2026-128 - V2 launch free-match-token window

- Phase: V2-monetisation-launch-token-waiver.
- Lichess seam: Dev/admin settings registration, Dev controller backend settings snapshot, lobby JSON token-balance payload, native setup modal token-gate copy, same-origin EvenChess search admission, and EvenChess play-page access label.
- Lichess files touched: `app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `app/views/lobby/home.scala`; `app/views/evenchess/play.scala`; `modules/web/src/main/Env.scala`; `ui/lobby/src/interfaces.ts`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/css/_setup.scss`.
- EvenChess files touched: `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/main/SubscriptionTokensAds.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/evenchess/src/test/SubscriptionTokensAdsTest.scala`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Launch-period free matching must be operator-controlled, date-bounded, and server-authoritative. The browser may display only the public message, while search admission uses the effective token snapshot produced by the ECL controller and meaningful-play settlement preserves existing token balances.
- Public UX effect: When the configured window is currently active, rated/casual setup cards display `Tokens are temporarily free`; search starts can proceed without consuming startup/earned game tokens or requiring ad access. Outside the window, existing token/subscription gates apply.
- Preserved Lichess capability: Normal native setup modal/search presentation remains. The waiver does not alter clocks, game creation, Set Level, Used Level, ECR settlement, or ECE behavior.
- Patch map entry: `PM-2026-123`.
- Tests / checks: `AdminBackendSettingsTest`, `PlaySearchIntegrationTest`, `SubscriptionTokensAdsTest`, and `ui/lobby/tests/evenchessSetup.test.ts` cover settings, admission, settlement, and setup-card copy.
- Upstream update notes: Preserve the three `evenchess.backend.monetisation.freeMatchTokens.*` settings and the server-side `TokenSnapshot.withFreeMatchTokenWindow` path when admin/settings or lobby setup code changes.
- Rollback notes: Disable `evenchess.backend.monetisation.freeMatchTokens.enabled` first, then remove the UI copy and token snapshot override.

### INT-2026-129 - V2 ECE full-match v1.2 canonical payload and summary bridge

- Phase: V2-analysis-memory-full-match-contract-alignment.
- Lichess seam: Same-origin ECL/Test Ground ECE full-match bridge, route table, and EngineGateway ECE endpoint config.
- Lichess files touched: `app/controllers/EvenChess.scala`; `conf/routes`.
- EvenChess files touched: `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `docs/requirements/EVENCHESS_FULL_MATCH_PAYLOAD_CONTRACT.md`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE v1.2 makes `/v1/ece/full-match` the explicit full-match review endpoint and `/v1/ece/full-match-summary` the AI summary endpoint. ECL must call ECE server-to-server, parse canonical `evenchess_full_game.turns[]`, and convert public `turns[].ece_payload.side_outputs.<side>` into the same approved overlay-frame storage used by live/replay.
- Public UX effect: Existing L10 review flow can accept ECE's canonical full-match payload. A same-origin Test Ground bridge can call full-match summaries without browser code calling ECE directly.
- Preserved Lichess capability: Normal replay, move history, and board rendering remain Lichess-owned. ECL does not import ECE internals or expose provider paths, prompts, raw engine output, or secrets.
- Patch map entry: `PM-2026-124`.
- Tests / checks: `EngineGatewayTest`, `LiveCoachingTest`, `AnalysisMemoryTest`, `ui/round/tests/evenchessTestGround.test.ts`, and root `compile`.
- Upstream update notes: Preserve `/v1/ece/full-match` as the primary route and keep `/v1/ece/game-review` as a legacy alias only. Preserve the parser fallback for old `frames`/`move_outputs` until ECE migration is complete.
- Rollback notes: Revert to `config.gameReviewUrl` and old top-level frame parsing only if ECE full-match v1.2 is not deployed; keep browser calls same-origin.

### INT-2026-130 - V2 ECL multi-user load harness

- Phase: V2 launch hardening.
- Lichess seam: Repo command registry for EvenChess load-harness execution.
- Lichess files touched: `package.json`.
- EvenChess files touched: `scripts/evenchess-ecl-load-harness.js`; `scripts/evenchess-ecl-load-harness.test.mjs`; `docs/evenchess/EVENCHESS_ECL_MULTI_USER_LOAD_HARNESS.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE load tests only verify the private engine service. Public-launch readiness also needs an ECL-owned harness that exercises same-origin ECL endpoints, replay caching, proposed/potential bridge calls, and ECE metrics capture.
- Public UX effect: None. This is operator/test tooling only.
- Preserved Lichess capability: Normal Lichess runtime, routes, game flow, and browser behaviour are unchanged.
- Patch map entry: `PM-2026-125`.
- Tests / checks: `node scripts/evenchess-ecl-load-harness.test.mjs`; `node --check scripts/evenchess-ecl-load-harness.js`.
- Upstream update notes: Reapply only the package script entries if upstream rewrites `package.json`; the harness itself is EvenChess-owned under `scripts/`.
- Rollback notes: Remove the two package scripts and the EvenChess harness files if a different staging load-test runner replaces this one.
- Staging extension note: Current local harness covers available Test Ground bridge surfaces. Before public launch it must be extended with staging test-user authentication, real search creation, sim-bot matching, game creation, move flow, audited render events, and replay-cache verification.

### INT-2026-131 - V2 review request processing indicator

- Phase: V2 review/summary UX hardening.
- Lichess seam: Native round/replay EvenChess overlay controls and styling.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Full-game review and later AI summary requests can take long enough that users need a clear processing state and duplicate submission prevention.
- Public UX effect: During replay, the `Run L10 Review` action now displays an inline spinner plus `Processing full game` while the server-side request is in flight.
- Preserved Lichess capability: Normal replay and board interaction remain unchanged. The spinner does not change ECE request payloads, stored history, quotas, or review results.
- Patch map entry: `PM-2026-126`.
- Tests / checks: `ui/round/tests/evenchessOverlay.test.ts` covers the in-flight spinner state.
- Upstream update notes: Preserve the loading-state branch on the EvenChess review control if upstream rewrites round-side panels.
- Rollback notes: Remove the spinner vnode and SCSS animation; the existing disabled/loading logic will still prevent duplicate review requests.

### INT-2026-132 - V2 bot simulation admin wording and help text

- Phase: V2 bot operations admin UX hardening.
- Lichess seam: Protected Dev/admin bot-operations page and bot-operation controller flash messages.
- Lichess files touched: `app/controllers/Dev.scala`; `modules/web/src/main/ui/DevUi.scala`.
- EvenChess files touched: `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Simulation bots are an operator tool, and the admin page must explain them in production-readable terms without duplicate forms or internal seed/ticket wording.
- Public UX effect: None for normal users. Admins see one simulation settings form with clear labels, hover/help text, `Start simulation` / `Update running simulation`, `Refill queue now`, and `Stop simulation`.
- Preserved Lichess capability: Admin auth/CSRF and existing route/action names remain unchanged. Bot simulation still uses the same server-side search repository and MMR pipeline.
- Patch map entry: `PM-2026-127`.
- Tests / checks: `BotOperationsTest` covers the new summary wording.
- Upstream update notes: Preserve the single-form simulation controls and help text if the Dev UI section is refactored.
- Rollback notes: Reverting restores the old duplicate simulation forms and seed/ticket wording but does not change bot runtime behavior.

### INT-2026-133 - V2 roster-backed bot simulation and fallback handoff

- Phase: V2 matchmaking/bot operations hardening.
- Lichess seam: Protected Dev/admin bot-operations settings, EvenChess search controller game handoff, and web SettingStore-backed admin configuration.
- Lichess files touched: `app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`.
- EvenChess files touched: `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Simulation/fallback bots that are meant to look like humans cannot be generated from fake string ids or native Stockfish AI setup. The platform needs configurable real local bot account rosters, selected time-control pools, and a handoff that uses the normal human challenge path when a bot record is roster-backed.
- Public UX effect: Roster-backed bot matches render through the ordinary human-vs-human round shell with normal usernames and clocks. Admins can choose simulation time-control families and provide account rosters.
- Preserved Lichess capability: Normal human-vs-human challenge creation is reused rather than reimplemented. Native AI/computer play remains available only through explicit computer-game flows, not public matchmaking bot fallback.
- Patch map entry: `PM-2026-128`.
- Tests / checks: `./lila.sh "evenchess/testOnly lila.evenchess.BotOperationsTest lila.evenchess.PlaySearchIntegrationTest"`; `./lila.sh "web/compile"`.
- Upstream update notes: Preserve the roster-backed bot branch and the no-handoff behavior for non-roster bot tickets if `EvenChess.scala` search handoff is refactored.
- Rollback notes: Remove roster/time-control settings only if human-looking bot simulation is deferred; do not restore native AI fallback for public matchmaking without an explicit product override.

### INT-2026-134 - V2 matchmaking bots fail closed without roster accounts

- Phase: V2 matchmaking/bot operations hardening.
- Lichess seam: EvenChess search controller game handoff and EvenChess MMR fallback seeding.
- Lichess files touched: `app/controllers/EvenChess.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`.
- EvenChess files touched: `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Public matchmaking bots must look like ordinary players with both clocks and normal usernames. Generated non-roster fallback tickets were being converted to native Stockfish/computer games, which exposed the wrong UX.
- Public UX effect: Matchmaking fallback no longer creates Stockfish-labeled one-clock games. If no roster-backed bot account is configured, the search remains waiting instead of creating the wrong game type.
- Preserved Lichess capability: Explicit computer games still use native Lichess AI/computer setup. Public EvenChess bot fallback uses the normal human challenge/game path only when a real bot account is available.
- Patch map entry: `PM-2026-129`.
- Tests / checks: `./lila.sh "evenchess/testOnly lila.evenchess.BotOperationsTest lila.evenchess.PlaySearchIntegrationTest"`; `./lila.sh "compile"`.
- Upstream update notes: Keep non-roster bot tickets out of `env.setup.processor.ai` and other native computer-game creation paths.
- Rollback notes: Reverting this restores the observed Stockfish label/one-sided-clock bug.

### INT-2026-135 - V2 bot operations deployment wording cleanup

- Phase: V2 production-readiness cleanup.
- Lichess seam: Protected Dev/admin bot-operations page, bot-operation controller flash messages, and web SettingStore descriptions.
- Lichess files touched: `app/controllers/Dev.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`.
- EvenChess files touched: `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/DataModelsAndSeams.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Operator-facing bot controls must use deployment-safe wording that matches the roster-backed simulated-player model and avoid fake-player/synthetic-ticket internals.
- Public UX effect: None for normal users. Admin/operator text is clearer and consistent with fail-closed bot fallback.
- Preserved Lichess capability: Admin auth, CSRF, route names, setting ids, and bot runtime behavior are unchanged.
- Patch map entry: `PM-2026-130`.
- Tests / checks: `./lila.sh "compile"`; `./lila.sh "evenchess/test"`; `pnpm test:ui-tsx ui/lobby/tests/evenchessSetup.test.ts ui/round/tests/evenchessOverlay.test.ts ui/round/tests/evenchessTestGround.test.ts ui/round/tests/evenchessTestGroundMoveBridge.test.ts ui/analyse/tests/evenchessLearning.test.ts ui/lib/tests/evenchessTts.test.ts ui/opening/tests/evenchessOpeningAi.test.ts`; `git diff --check`.
- Upstream update notes: Preserve roster-backed simulated-player wording if the Dev/admin UI is refactored.
- Rollback notes: Reverting only restores older operator wording; it should not change bot matching behavior.

### INT-2026-136 - V2 non-live board overlay availability

- Phase: V2 feature-surface overlay expansion.
- Lichess seam: Analysis/study board controller, puzzle board controller, opening PGN viewer initialization, and same-origin EvenChess ECE overlay route.
- Lichess files touched: `app/controllers/EvenChess.scala`; `conf/routes`; `ui/analyse/src/ctrl.ts`; `ui/puzzle/src/ctrl.ts`; `ui/opening/src/opening.ts`.
- EvenChess files touched: `ui/lib/src/evenchessUniversalOverlay.ts`; `ui/lib/tests/evenchessUniversalOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Non-live boards should have EvenChess overlays available without changing the native page flow. The page controllers provide current FEN/ply/orientation, while ECL owns the server-side L10 authorization and ECE call.
- Public UX effect: Logged-in users can receive L10 EvenChess board overlays on analysis, study, puzzle, and opening PGN boards when ECE is reachable. Live-game round behavior remains unchanged.
- Preserved Lichess capability: Native legal moves, puzzle solving, analysis navigation, study chapter navigation, PGN viewer controls, and board rendering remain owned by Lichess/chessground.
- Patch map entry: `PM-2026-131`.
- Tests / checks: `ui/lib/tests/evenchessUniversalOverlay.test.ts` covers URL/level safety, visual atom conversion, and stale payload rejection.
- Upstream update notes: Reapply the small install hooks after upstream changes to analysis/puzzle/opening board initialization. Preserve the same-origin route and server-forced L10 rule.
- Rollback notes: Remove the installer calls and route if non-live overlays need to be temporarily disabled; do not add browser-direct ECE calls.

### INT-2026-137 - V2 lobby start actions remain clickable

- Phase: V2 setup/search UX correction.
- Lichess seam: Native lobby start-card button gating.
- Lichess files touched: `ui/lobby/src/view/table.ts`.
- EvenChess files touched: `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/tests/evenchessSetup.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The Lichess lobby disabled all start actions when a real-time game was waiting for the user. EvenChess needs those homepage actions available for testing and launch navigation; server-side search/setup policy remains authoritative for actual game admission.
- Public UX effect: `Create lobby game`, `Challenge a friend`, and `Play against computer` stay pressable even if the account has another active real-time game waiting.
- Preserved Lichess capability: Playban, unread-message, and bot-account gates for open lobby game creation remain intact. Friend and computer setup keep their native modal paths.
- Patch map entry: `PM-2026-132`.
- Tests / checks: `ui/lobby/tests/evenchessSetup.test.ts`.
- Upstream update notes: Preserve the EvenChess helper-based gating if upstream rewrites `ui/lobby/src/view/table.ts`.
- Rollback notes: Reverting restores the previous ongoing-game disable behavior on the lobby start card.

### INT-2026-138 - V2 shared bot roster and exclusive bot modes

- Phase: V2 bot operations launch hardening.
- Lichess seam: Protected Dev/admin bot-operations controller, settings registration, and admin UI.
- Lichess files touched: `app/controllers/Dev.scala`; `modules/web/src/main/Env.scala`; `modules/web/src/main/ui/DevUi.scala`.
- EvenChess files touched: `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Matchmaking fallback and simulation both need the same roster-backed bot accounts. Operators also need large-roster input and one-mode-at-a-time controls so simulation and fallback do not run concurrently.
- Public UX effect: None directly. Bot matches continue to require human-style account-backed game creation; admin configuration now defaults to `ecbot0001` through `ecbot1000` and uses a shared bulk roster textarea.
- Preserved Lichess capability: Native human challenge/game creation remains the only public bot-game handoff path. The controller does not create native Stockfish/computer fallback games.
- Patch map entry: `PM-2026-133`.
- Tests / checks: `AdminBackendSettingsTest`, `BotOperationsTest`, and `PlaySearchIntegrationTest` cover default roster generation, shared admin-state roster selection, and generated-roster fallback matching.
- Upstream update notes: Preserve the shared roster setting behavior and mutual-exclusion start/seed logic after upstream Dev/admin refactors.
- Rollback notes: Reverting restores separate roster inputs and allows both bot modes to be enabled concurrently; do not roll back without also revisiting public bot-game UX.
- Deployment note: The generated roster is a username/name roster. Corresponding local user records still need to be created before staging/production bot games can render as real human-style rounds.

### INT-2026-139 - V2 move-triggered overlay transition layer

- Phase: V2 live ECE display stability.
- Lichess seam: Native round board overlay renderer and move-triggered EvenChess clear handling.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The native round controller changes ply/FEN immediately after a move, while ECE payloads arrive asynchronously. The overlay renderer must keep the last accepted safe board layer mounted during the in-flight refresh so the user does not see the whole overlay disappear between turns.
- Public UX effect: Board arrows, highlights, markers, and eval display no longer blank during ordinary move-triggered ECE refresh. The next accepted current-position payload replaces only the changed overlay elements.
- Preserved Lichess capability: Legal move lifecycle, clocks, board movement, and stale-response rejection remain unchanged. Browser code still does not call ECE directly.
- Patch map entry: `PM-2026-134`.
- Tests / checks: `ui/round/tests/evenchessOverlay.test.ts` covers retaining the previous safe payload and rendering the `move-refresh` transition layer.
- Upstream update notes: Preserve the transition check if upstream rewrites round redraw or board overlay mounting. Do not remove stale-response rejection for incoming payloads.
- Rollback notes: Reverting restores the old blanking behavior during ECE refresh and should only be done if transition overlays prove visually misleading.

### INT-2026-140 - V2 puzzle coach panel overlay integration

- Phase: V2 feature-surface overlay expansion.
- Lichess seam: Native puzzle controller/view layout and universal same-origin overlay renderer.
- Lichess files touched: `ui/puzzle/src/ctrl.ts`; `ui/puzzle/src/view/main.ts`; `ui/puzzle/css/_layout.scss`.
- EvenChess files touched: `ui/lib/src/evenchessUniversalOverlay.ts`; `ui/lib/tests/evenchessUniversalOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Puzzle pages need EvenChess board markers, level-selection/toggle controls, and coach text without rebuilding the puzzle page or floating controls over the native puzzle workflow.
- Public UX effect: Logged-in users can see integrated EvenChess Levels and Coach panels in the puzzle tools column while board markers mount on the puzzle board when the same-origin ECL overlay payload includes approved visuals. Per-feature toggles hide/show matching board and coach payload features.
- Preserved Lichess capability: Puzzle solving, native feedback, move list, keyboard/voice move support, chessground input, and puzzle ratings remain owned by Lichess.
- Patch map entry: `PM-2026-135`.
- Tests / checks: `ui/lib/tests/evenchessUniversalOverlay.test.ts` covers safe non-live coach-card selection, rejects stale/raw card payloads, and verifies level-feature toggle filtering for puzzle board visuals and coach cards.
- Upstream update notes: Preserve the puzzle left-column levels host/grid area, right-column coach host, universal level-control render, feature-filtering behavior, and controller panel hooks after upstream puzzle view/controller refactors.
- Rollback notes: Remove the puzzle panel host and `getPanelElement` hook to keep board-only non-live overlays; do not replace this with browser-direct ECE calls.

### INT-2026-141 - V2 homepage EvenChess summary placement

- Phase: V2 homepage polish.
- Lichess seam: Native lobby homepage view and responsive lobby grid layout.
- Lichess files touched: `app/views/lobby/home.scala`; `ui/lobby/css/_layout.scss`; `ui/lobby/css/_lobby.scss`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The server-rendered homepage owns the main-page card list, while the lobby stylesheet owns the responsive position of the tabbed quick-pairing app. EvenChess needs its compact summary beside that app on wide screens and should not render the old Donate/Swag Store support cards in the main homepage grid.
- Public UX effect: The compact EvenChess summary appears in the left rail beside the quick-pairing/lobby tabs on desktop layouts. The homepage Donate and Swag Store cards are removed from the main content grid.
- Preserved Lichess capability: The top navigation Donate link and native lobby quick-pairing, lobby, correspondence, game-count, puzzle, feed, and setup actions remain intact.
- Patch map entry: `PM-2026-136`.
- Tests / checks: CSS build and browser DOM/layout verification.
- Upstream update notes: Preserve the `evenchess` grid area and absent `lobby__support` homepage block after upstream homepage/layout changes.
- Rollback notes: Revert the layout grid and restore the support-card block if the launch homepage needs the upstream support cards again.

### INT-2026-142 - V2 live L6 WikiBook fieldset

- Phase: V2 live feature-surface reuse.
- Lichess seam: Native round side shell, round move-history data, and responsive round layout.
- Lichess files touched: `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/build/round.scss`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Live games need the analysis-board WikiBooks opening explanation as an L6 display feature, controlled from the EvenChess level list and positioned above the level selector. The round renderer is the place that has current ply, SAN history, and the existing EvenChess side-column layout, while the visible UI should reuse the Lichess `wikibook-field` / `analyse__wiki` fieldset rather than an EvenChess-specific card.
- Public UX effect: At Set Level 6 or higher, the L6 section includes a `WikiBook` toggle. When enabled, the existing Lichess-style collapsible WikiBook fieldset appears above the level selector and loads the current line from public WikiBooks data.
- Preserved Lichess capability: Native round board, clocks, legal moves, move table, and ECE server-to-server boundary remain unchanged. This uses public WikiBooks data and does not call ECE from the browser.
- Patch map entry: `PM-2026-137`.
- Tests / checks: `ui/round/tests/evenchessOverlay.test.ts` covers path building, fieldset rendering, and toggle gating.
- Upstream update notes: Preserve the `openingWiki` feature key, shared `toggle-box` CSS import, `wikibook-field`/`analyse__wiki` fieldset structure, and the `evenchess-live__level-column` layout wrapper after upstream round view/layout changes.
- Rollback notes: Remove the L6 feature key/fieldset and return the level card to the direct `levels` grid area if live opening theory needs to be disabled.

### INT-2026-143 - V2 fixed-perspective Stockfish eval stabilization

- Phase: V2 ECE eval contract alignment.
- Lichess seam: Same-origin ECE bridge parser and native round eval rendering.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `modules/evenchess/src/main/EceLiveBridge.scala`; `modules/evenchess/src/main/LiveBoardIntegration.scala`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`; `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE v1.2 normalized Stockfish eval now exposes fixed White-positive fields (`score.cp_white`, `score.mate_white`) and side-output-relative fields (`score.cp`). ECL's eval bar is fixed White-up/Black-down, so it must carry the fixed values as structured visual metadata, ignore quick/no-eval placeholders, map centipawns nonlinearly, and inject legal proposed-move `eval_after` into the cached post-move preview when the nested side output lacks its own evaluation.
- Public UX effect: The eval bar and coach eval strip no longer snap to neutral or jump during quick/deep loading gaps. Proposed-move preview toggles smoothly between current-FEN eval and post-move eval without consuming another call.
- Preserved Lichess capability: Native round rendering, legal moves, clocks, and ECE server-to-server boundaries remain unchanged.
- Patch map entry: `PM-2026-138`.
- Tests / checks: `modules/evenchess/src/test/EceLiveBridgeTest.scala` covers bridge metadata preservation/gating; `ui/round/tests/evenchessOverlay.test.ts` covers live eval retention, structured White-positive eval preference, mate rendering, quick zero rejection, and proposed eval toggling.
- Upstream update notes: Preserve the live/proposed eval cache separation, structured eval visual fields, fixed-perspective eval preference, mate saturation, and nonlinear bar mapping if upstream round rendering or ECE bridge parsing changes.
- Rollback notes: Reverting restores older quick/deep eval behavior and may show neutral flicker or side-relative values; do not revert unless ECE removes the fixed-perspective fields.

### INT-2026-144 - V2 roster-backed bot account provisioning and runner

- Phase: V2 bot matchmaking/simulation operations.
- Lichess seam: Native dev/admin controller, user repository/authenticator, EvenChess game-creation controller, round bus, and web admin UI.
- Lichess files touched: `app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/web/src/main/ui/DevUi.scala`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Matchmaking fallback and simulation require roster-backed real local accounts to create human-style rounds through the normal challenge/game path. Saved roster names alone are not enough; if the users do not exist, match contracts can form but game creation cannot resolve the opponent account. Normal local accounts also do not move like native AI players, so ECL registers the matched roster-backed side with a managed legal-move runner after game creation. Simulation mode also needs an active sim-vs-sim pump so seeded synthetic users can pair and play without waiting for a human search to poll their pool, while human searches must claim matchable simulation tickets before the pump consumes them. Seeding must create same-pool center-out close-rating cohorts instead of isolated non-matching extreme-rating tickets.
- Public UX effect: Operators can provision/check the shared roster before starting either bot mode. Matched bot rounds use normal usernames and clocks instead of native Stockfish/computer-game presentation, the roster-backed side makes delayed legal moves, and simulation can match both humans and simulated users because the queue contains matchable same-pool mid-rating cohorts.
- Preserved Lichess capability: Native account creation, challenge acceptance, clocks, user display, normal game creation, legal move generation, and round move application stay owned by Lichess. The provisioned accounts are ordinary local/staging accounts with private generated passwords and are not assigned the Lichess BOT title.
- Patch map entry: `PM-2026-139`.
- Tests / checks: Existing bot/search tests, Scala compile, browser/runtime verification that matchmaking fallback creates `Admin` vs `ecbot0052`, and browser/runtime verification that simulation creates `Admin` vs `ecbot0003` from a 24-player 5+0 Blitz search.
- Upstream update notes: Preserve the `provision-bot-accounts` admin action, shared-roster flow, 1-second local test timeout support, managed-bot registration after matched game creation, human-priority simulation matching, center-out simulation seeding, and throttled duplicate-guarded simulation pump if upstream changes `Dev.scala`, `DevUi.scala`, or the challenge handoff in `EvenChess.scala`.
- Rollback notes: Reverting this requires manually creating every roster account before bot matchmaking/simulation can work; do not rollback unless a dedicated operator/user-provisioning service replaces it.

### INT-2026-145 - V2 create-lobby native pending search indicator

- Phase: V2 lobby/search UX hardening.
- Lichess seam: Native lobby setup controller, quick-pairing pool state, and homepage carousel helper.
- Lichess files touched: `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/src/view/carousel.ts`; `ui/lobby/tests/evenchessSetup.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Create lobby game uses the native setup modal, but EvenChess replaces the native pool ticket with an EvenChess search ticket. When the chosen setup maps to a quick-pairing card, the lobby must still show the native active/spinner card instead of closing the modal back to an idle lobby.
- Public UX effect: Submitting the default Create lobby game setup now closes the modal into the same visible quick-pairing pending animation used by direct quick-card searches. The separate EvenChess debug/status card remains hidden unless an explicit local/debug flag is enabled.
- Preserved Lichess capability: Native quick-pairing visual state, setup modal, pool cards, and debug-hidden search status behavior remain intact.
- Patch map entry: `PM-2026-140`.
- Tests / checks: Focused lobby helper test covers pending-card eligibility for matching and non-matching setup states; browser verification covers the create-lobby submit path.
- Upstream update notes: Preserve `showEvenChessPendingPool`, `evenChessPendingPoolId`, and the `.lobby__support` null guard after upstream lobby/setup or homepage carousel changes.
- Rollback notes: Reverting restores the confusing idle-lobby state after create-lobby submit and may reintroduce a homepage script error when support cards are absent.

### INT-2026-146 - V2 live WikiBook visible empty state

- Phase: V2 live feature-surface polish.
- Lichess seam: Native round overlay renderer and live EvenChess WikiBook styling.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The live L6 WikiBook fieldset reused the analysis-board empty class, which hides the whole fieldset when no extract exists. On live rounds that made the checked L6 WikiBook feature look missing. The live fieldset now remains visible and keeps the Lichess `analyse__wiki` / `toggle-box` structure while showing an empty-state message until a real extract loads.
- Public UX effect: At Set Level 6+, users can see the WikiBook fieldset above the level selector even on early plies or lines without WikiBook coverage.
- Preserved Lichess capability: Analysis-board WikiBook behavior remains unchanged; only the live EvenChess fieldset overrides empty visibility.
- Patch map entry: `PM-2026-141`.
- Tests / checks: Round overlay test covers the visible empty-state copy.
- Upstream update notes: Preserve the live-only `.evenchess-live__opening-wiki.empty` visibility override and placeholder body after upstream WikiBook or round layout changes.
- Rollback notes: Reverting restores the hidden empty-state behavior and may make the live L6 WikiBook feature appear missing.

### INT-2026-147 - V2 live WikiBook CSP allow-list

- Phase: V2 live feature-surface polish.
- Lichess seam: Native round page Content Security Policy.
- Lichess files touched: `modules/round/src/main/ui/RoundUi.scala`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The L6 live WikiBook fieldset reuses the same public WikiBooks API loader as analysis, but round pages did not include the analysis page's `withWikiBooks` CSP allowance. The fieldset could mount and derive a valid opening path while the browser still blocked the external WikiBooks request, leaving valid lines in the empty state.
- Public UX effect: L6+ live games can load WikiBooks opening text as moves change instead of showing the empty placeholder for lines that have public WikiBooks coverage.
- Preserved Lichess capability: Native round board, clocks, legal moves, peer connection, WebAssembly, and ECE server-to-server boundaries remain unchanged. This only permits `en.wikibooks.org`, the same public origin already allowed on analysis pages.
- Patch map entry: `PM-2026-142`.
- Tests / checks: Scoped UI build and browser/runtime CSP verification after local rebuild.
- Upstream update notes: Preserve `withWikiBooks` in `RoundUi.RoundPage` when upstream changes round CSP construction.
- Rollback notes: Reverting leaves the live fieldset visible but unable to fetch public WikiBooks entries under the round page CSP.

### INT-2026-148 - V2 roster bot established ratings and exact clocks

- Phase: V2 bot matchmaking/simulation polish.
- Lichess seam: Native dev/admin account provisioning, native lobby setup submit params, EvenChess search ticket metadata, and native challenge/game creation.
- Lichess files touched: `app/controllers/Dev.scala`; `app/controllers/EvenChess.scala`; `ui/lobby/src/evenchessSetup.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/tests/evenchessSetup.test.ts`.
- EvenChess files touched: `modules/evenchess/src/main/BotOperations.scala`; `modules/evenchess/src/main/LevelBasedMatchmaking.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/BotOperationsTest.scala`; `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Roster-backed bot games are normal Lichess challenge games, so the visible player rating/provisional marker is copied from normal Lichess perfs before the game starts. Local roster users created without perfs display as `1500?`. The search form also previously sent only a speed bucket, which meant a matched bot game could lose the selected clock's exact limit/increment.
- Public UX effect: Matched roster-backed fallback and simulation bots display as established, non-round local accounts aligned to their target rating instead of new `1500?` accounts. Real-time searches carry the selected clock into the created game, preserving human-style two-clock presentation for human-vs-bot and sim-vs-sim handoff paths.
- Preserved Lichess capability: Native accounts, challenge acceptance, game clocks, player display ratings, legal move lifecycle, and round rendering remain Lichess-owned. ECL only seeds/repairs staging bot account perfs and passes exact clock metadata into the normal challenge path.
- Patch map entry: `PM-2026-143`.
- Tests / checks: `BotOperationsTest`, `PlaySearchIntegrationTest`, `LevelBasedMatchmakingTest`, full Scala compile, lobby helper test, and UI build.
- Upstream update notes: Preserve roster perf repair before `enabledWithPerf`, exact `clockLimitSeconds`/`clockIncrementSeconds` ticket metadata, and fallback to bucket defaults when no exact clock exists.
- Rollback notes: Reverting can make roster-backed bot games look like new accounts again and can lose exact selected clock settings in bot-created games.

### INT-2026-149 - V2 live WikiBook analysis-style collapse and scroll

- Phase: V2 live feature-surface polish.
- Lichess seam: Native round overlay renderer and live EvenChess WikiBook styling.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The live L6 WikiBook fieldset is mounted inside the round side shell, not the native analysis side shell. It must therefore explicitly mark the shared `toggle-box` as initialized, opt back into pointer events inside the non-interactive overlay host, persist the open/closed state, and size the WikiBook text region so it scrolls internally while the legend remains clickable.
- Public UX effect: Live L6 WikiBook opens to a readable card, can be scrolled within the card, and collapses/reopens from the header like the analysis-board WikiBook.
- Preserved Lichess capability: Analysis-board WikiBook behavior, public WikiBooks fetch logic, round clocks, board interaction, and ECE server-to-server boundaries remain unchanged.
- Patch map entry: `PM-2026-144`.
- Tests / checks: Round overlay render test covers the initialized toggle-box marker; UI build verifies the updated SCSS/TypeScript compile.
- Upstream update notes: Preserve the live `toggle-box--ready` marker, `pointer-events: auto` on the live WikiBook fieldset, guarded stored open state, and `.analyse__wiki-text` internal scroll sizing after upstream round or analysis WikiBook layout changes.
- Rollback notes: Reverting can restore the duplicate-toggle behavior where the live WikiBook does not close and the cramped fieldset behavior where long entries are hard to read.

### INT-2026-150 - V2 managed bot round presence and multi-bot tracking

- Phase: V2 bot matchmaking/simulation polish.
- Lichess seam: EvenChess game-creation controller, native round bot-presence bus, native round bot-move bus, and roster-backed bot runtime registry.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Roster-backed bots are intentionally normal local/staging accounts for display, but normal accounts with no browser socket are offline in round state. ECL's managed runner must therefore use Lichess's existing `RoundBus.BotConnected` signal while it is driving a roster-backed bot, and it must track each bot player independently so sim-vs-sim games keep both runners attached.
- Public UX effect: Human-vs-bot fallback games and simulation-created games should no longer show offline/opponent-left prompts for active managed bots, and sim-vs-sim games can keep both sides driven rather than overwriting the first attached bot.
- Preserved Lichess capability: Game creation, clocks, player display, legal moves, round actors, and bot presence remain Lichess-owned. ECL only registers managed local bot sides and publishes the same native bot presence/move events Lichess already understands.
- Patch map entry: `PM-2026-145`.
- Tests / checks: Focused EvenChess bot/search tests and full Scala compile.
- Upstream update notes: Preserve per-player managed bot keys, `RoundBus.BotConnected(color, true)` on attach, disconnect on managed cleanup, and `RoundBus.BotPlay` for legal move submission if upstream changes round bus APIs or challenge handoff.
- Rollback notes: Reverting can make roster-backed bots play moves while appearing offline, restore "opponent left" prompts, and allow sim-vs-sim bot registration to overwrite one side.

### INT-2026-151 - V2 account default feature toggles

- Phase: V2 settings/display polish.
- Lichess seam: Native account preference form, preference JSON, and native round EvenChess overlay renderer.
- Lichess files touched: `modules/pref/src/main/PrefForm.scala`; `modules/pref/src/main/JsonView.scala`; `modules/pref/src/main/ui/AccountPref.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `modules/evenchess/src/main/UserSettings.scala`; `modules/evenchess/src/test/UserSettingsTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Preferred starting Used Level is account-scoped, but the prior in-game bulk selector still rebuilt feature toggles as "all on up to level." The native preference form now persists an EvenChess default feature-toggle profile, exposes it through safe preference JSON, and the round overlay applies it during new-game initialization and bulk level selection.
- Public UX effect: Users can set which EvenChess level-card toggles are on by default. Starting a game or using "Apply up to" follows that profile while still disabling features above the selected level and preserving monotonic Used Level.
- Preserved Lichess capability: Native account settings layout, autosave behavior, round board, and ECE server-to-server boundary remain unchanged. The added preference only controls display defaults and cannot grant coaching above Set Level.
- Patch map entry: `PM-2026-146`.
- Tests / checks: `UserSettingsTest` and `evenchessOverlay.test.ts` cover persistence and dropdown/default application.
- Upstream update notes: Preserve `evenchess.defaultFeatureToggles` form binding, JSON field, settings UI block, and round preset application after upstream preference or round UI changes.
- Rollback notes: Reverting restores old behavior where account settings cannot define per-feature defaults and "Apply up to" blindly enables every eligible feature.

| Patch map | Integration log | Current purpose | Reapply after upstream update |
| --- | --- | --- | --- |
| `PM-2026-013` | `INT-2026-026` | Narrow broad public-shell takeover back toward Lichess-native homepage and EvenChess route pages. | Restore upstream homepage/nav first, then reapply only EvenChess visible route/page adjustments. |
| `PM-2026-014` | `INT-2026-027` | Keep Lichess layout while visible wordmark says EvenChess. | Reapply wordmark/config only, not header structure rewrites. |
| `PM-2026-015` | `INT-2026-028` | Primary Play enters EvenChess flow while nav remains Lichess-style. | Reapply Play target only after accepting upstream `TopNav` changes. |
| `PM-2026-016` | `INT-2026-029` | Native lobby setup modal carries EvenChess settings and submits to EvenChess search contract. | Restore upstream lobby/setup modal first, then reinsert EvenChess fields, submit adapter, and status. |
| `PM-2026-017` | `INT-2026-034` | Admin/backend settings reject raw secret-like EvenChess values. | Reapply save-time guard at the admin settings endpoint or its replacement. |
| `PM-2026-018` | `INT-2026-035` | Live overlay rejects mismatched-game payloads and clears stale display. | Reapply payload/game-id gating in the round overlay adapter. |
| `PM-2026-036` | `INT-2026-036` | Opening overlay clears stale panels when payload becomes invalid. | Reapply stale DOM clearing before opening overlay early returns. |
| `PM-2026-037` | `INT-2026-037` | TTS requires overlay audit identity on all surfaces. | Keep browser and server TTS policies aligned on audit-id requirement. |
| `PM-2026-039` | `INT-2026-039` | Native homepage says EvenChess and carries the compact Phase C explanation. | Reapply homepage metadata, compact summary, and lobby summary CSS after upstream homepage/lobby changes. |
| `PM-2026-040` | `INT-2026-040` | Header/account shell exposes game-token balance, token/ad route, and settings/account links. | Reapply token chip and account menu link after upstream header/account-preference changes. |
| `PM-2026-041` | `INT-2026-041`, `INT-2026-074` | Native setup modal captures EvenChess target/search controls and token-gate/status display for hook/friend games while computer play keeps native AI setup with internal L10 defaulting. | Reapply hook/friend setup fields, store keys, submit params, and status echo after upstream lobby setup changes; keep AI/computer submit on the native `/setup/ai` path unless a later requirement changes it. |
| `PM-2026-043` | `INT-2026-043` | Search JSON hands prepared records to the EvenChess MMR Engine and returns match-contract status. | Reapply only the thin JSON handoff after upstream controller or route changes; keep MMR logic in `PlaySearchIntegration`. |
| `PM-2026-125` | `INT-2026-130` | ECL multi-user load-harness commands exercise current Test Ground bridge seams and capture ECE metrics. | Reapply package script commands; keep harness under EvenChess-owned `scripts/`. |
| `PM-2026-131` | `INT-2026-136` | Non-live analysis/study/puzzle/opening boards hydrate L10 EvenChess overlays through same-origin ECL endpoint. | Reapply page-controller installer hooks and server-forced L10 route after upstream board-surface changes. |
| `PM-2026-135` | `INT-2026-140` | Puzzle pages render EvenChess Levels, feature toggles, Coach cards, and board markers through the universal same-origin overlay renderer. | Reapply the puzzle levels grid area, tools host, controller panel hooks, universal level controls, and feature-filtering behavior after upstream puzzle layout/controller changes. |
| `PM-2026-136` | `INT-2026-141` | Homepage places the compact EvenChess summary beside the primary lobby tabs and removes the main-grid Donate/Swag Store cards. | Reapply the `evenchess` grid area and keep the `lobby__support` card block absent after upstream homepage/layout changes. |
| `PM-2026-137` | `INT-2026-142` | Live L6 games can show the existing Lichess WikiBook fieldset above the level selector, derived from round SAN history and gated by the L6 feature toggle. | Reapply the `openingWiki` feature key, shared `toggle-box` CSS import, fieldset renderer, and `evenchess-live__level-column` wrapper after upstream round UI/layout changes. |
| `PM-2026-138` | `INT-2026-143` | Eval bar/status use structured fixed White-positive ECE eval values, ignore quick/no-eval placeholders, use nonlinear winning-chances-style bar mapping, and retain live/proposed eval state until an accepted replacement arrives. | Reapply structured eval visual fields, `cp_white`/`mate_white` preference, proposed `eval_after` fallback, live/proposed eval cache separation, mate saturation, and nonlinear bar mapping after upstream round/ECE bridge changes. |
| `PM-2026-139` | `INT-2026-144` | Bot ops can provision/check the shared roster as real normal local/staging accounts, register matched roster-backed sides with an ECL-managed legal-move runner, and seed/pump simulation tickets into human-style games without starving human searches. | Reapply the `provision-bot-accounts` action, Create/check roster accounts admin control, 1-second test timeout support, post-game-creation managed-bot registration, center-out simulation cohort seeding, human-priority matching, and throttled duplicate-guarded simulation pump after upstream dev/admin/search/challenge changes. |
| `PM-2026-140` | `INT-2026-145` | Create lobby game submits that map to a quick-pairing card show the native pending/searching card animation while the EvenChess search ticket is active. | Reapply pending-pool handoff and `.lobby__support` null guard after upstream lobby setup or homepage carousel changes. |
| `PM-2026-141` | `INT-2026-146` | Live L6 WikiBook fieldset remains visibly mounted with an empty-state message until an actual WikiBook extract loads. | Reapply the live-only empty visibility override and placeholder body after upstream round/WikiBook changes. |
| `PM-2026-142` | `INT-2026-147` | Live L6 WikiBook fetches use the same public `en.wikibooks.org` CSP allowance as analysis pages. | Reapply `withWikiBooks` in the round page CSP after upstream `RoundUi` changes. |
| `PM-2026-143` | `INT-2026-148` | Roster-backed bot games seed/repair established non-round Lichess perfs and preserve exact selected real-time clocks through EvenChess search tickets into native challenge creation. | Reapply roster perf repair before `enabledWithPerf`, exact clock metadata on search tickets, and lobby `clockLimitSeconds`/`clockIncrementSeconds` submit params after upstream dev/admin, challenge, or lobby setup changes. |
| `PM-2026-144` | `INT-2026-149` | Live L6 WikiBook follows analysis-style collapse behavior and provides a readable internal scroll region above the EvenChess levels card. | Reapply the live `toggle-box--ready` marker, live fieldset pointer-events override, stored open state, and WikiBook text scroll sizing after upstream round/WikiBook layout changes. |
| `PM-2026-145` | `INT-2026-150` | Managed roster-backed bots publish native round bot presence and are tracked independently per player for human-vs-bot and sim-vs-sim games. | Reapply per-player managed bot keys and `RoundBus.BotConnected` attach/cleanup signals after upstream round bus or challenge handoff changes. |
| `PM-2026-146` | `INT-2026-151` | Account settings persist preferred feature-toggle defaults and round level presets apply them instead of blindly enabling all eligible features. | Reapply `evenchess.defaultFeatureToggles` form/JSON/settings UI and round preset application after upstream preference or round overlay changes. |
| `PM-2026-149` | `INT-2026-154` | Round bootstrap uses persisted MMR contract Set Level, server-persisted monotonic Used Level, and L0/default preferred starting Used Level, while roster-backed bot matches attach managed runners from the accepted game and redirect humans to full player URLs. | Reapply `evenchess.display` round JSON, case-tolerant policy player lookup, `/evenchess/live/used-level`, L0 used-level client fallback, accepted-game bot attachment, and full player-route redirects after upstream round JSON or challenge handoff changes. |
| `PM-2026-150` | `INT-2026-155` | Public search-key polling returns the cached game redirect even after matched tickets are retired from the active queue. | Reapply redirect-ledger lookup before active-ticket resume after upstream search-controller or lobby-polling changes. |
| `PM-2026-151` | `INT-2026-156` | Local Test Ground pages opened through `localhost` use a same-origin websocket while LAN/mobile pages keep the LAN websocket. | Reapply local/private same-origin socket preference and PC-vs-mobile Test Ground URL split after upstream socket or launcher changes. |
| `PM-2026-152` | `INT-2026-157` | Pinned pieces render with a top-left `P` marker and no lock/L terminology or padlock glyph. | Reapply pinned-piece `P` marker behavior after upstream round overlay renderer changes. |
| `PM-2026-153` | `INT-2026-158` | Live round ECE bridge accepts newer threat board-fact payload shapes and retries high-level card-only payloads before settling. | Reapply ECE threat normalization and L2+ missing-visual retry behavior after upstream controller or round adapter changes. |
| `PM-2026-154` | `INT-2026-159` | Per-game live display dropdown and feature toggles persist through browser refresh. | Reapply `/evenchess/live/display-state`, `StoredDisplayState`, round JSON toggle hydration, and round control persistence after upstream round/controller changes. |
| `PM-2026-155` | `INT-2026-160` | Potential/proposed move controls and their text results live inside the main coach card. | Reapply the single-card coach composition and inline result sections after upstream round overlay or right-column style changes. |
| `PM-2026-156` | `INT-2026-161` | Live coach cards expose an Auto Speak toggle beside the manual Speak button. | Reapply the adjacent Speak/Auto controls and live `autoSpeak` override after upstream round overlay or TTS control changes. |

### INT-2026-154 - V2 contract Set Level bootstrap and human-style bot redirect

- Phase: V2 matchmaking/MMR and bot game-presentation hardening.
- Lichess seam: Native round JSON bootstrap and native challenge-to-round redirect after EvenChess match-contract handoff.
- Lichess files touched: `app/controllers/EvenChess.scala`; `conf/routes`; `modules/round/src/main/JsonView.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `modules/evenchess/src/main/GamePolicy.scala`; `modules/evenchess/src/test/GamePolicyTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: EvenChess MMR assigns Set Levels server-side, but the live round client previously had no persisted per-player policy in its bootstrap and could fall back to L10. Username/UserId case normalization could also hide the stored policy for users such as `Admin`/`admin`. Browser refresh also needed a server-side per-game Used Level record so the monotonic Used Level bar cannot reset downward after a player raises it. Bot-backed game creation also returned watcher URLs and attached the managed runner after an async lookup, which could create one-clock or opponent-left presentation during the first render.
- Public UX effect: Preferred L6 searches should enter games showing the contract-assigned player Set Level rather than L10. Games with no explicit preferred starting Used Level initialize at L0, and raising Used Level during the game survives browser refresh while remaining capped by Set Level. Roster-backed matchmaking/simulation bot games should render as normal human-vs-human rounds with both clocks and immediate bot presence.
- Preserved Lichess capability: Native challenge acceptance, legal move processing, round clocks, player URLs, and round socket state remain Lichess-owned. ECL only supplies the match contract policy and managed bot presence/move runner.
- Patch map entry: `PM-2026-149`.
- Tests / checks: Added focused preferred-L6 bot fallback, case-tolerant policy player lookup, capped monotonic Used Level persistence, L0 used-level initialization, refresh-persistent Used Level, and Set-Level cap regression tests.
- Upstream update notes: Preserve top-level `data.evenchess.display` in round player/watcher JSON, keep GamePolicy player lookup tolerant of username/UserId case normalization, never use account default Set Level as live game Set Level, keep no-preference Used Level fallback at L0, keep `/evenchess/live/used-level` max-only and Set-Level-capped, attach managed bots from accepted game data, and redirect matched humans to `Round.player(fullId)`.
- Rollback notes: Reverting can restore L10/L10 display fallback for assigned lower-level games and can make bot-backed games look like watcher/computer/offline rounds.

### INT-2026-155 - V2 matched search redirect survives ticket retirement

- Phase: V2 matchmaking/search lifecycle hardening.
- Lichess seam: Native lobby search JSON polling endpoint and native challenge-to-round redirect after EvenChess match-contract handoff.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The EvenChess controller must retire matched tickets after native game creation to prevent duplicate games, but the browser that did not create the game keeps polling with its opaque public search key. The redirect ledger now resolves public search keys to cached redirects before active-ticket resume, so retired tickets do not produce `Search ticket not found`.
- Public UX effect: Two compatible users searching from different browsers/devices can both be redirected into the same created game after one side finalizes the native challenge handoff.
- Preserved Lichess capability: Native challenge creation, challenge acceptance, game IDs, round URLs, and lobby polling remain Lichess-owned. ECL only stores the opaque search-key-to-redirect association.
- Patch map entry: `PM-2026-150`.
- Tests / checks: `PlaySearchIntegrationTest` covers redirect lookup for both sides after matched ticket retirement.
- Upstream update notes: Preserve the search JSON action ordering: public search-key cached redirect lookup first, active ticket resume second, new ticket creation only when no public search key is supplied.
- Rollback notes: Reverting can make the second polling browser/device lose the match redirect after the first side creates the game.

### INT-2026-156 - Local LAN/localhost socket host preference

- Phase: Local Test Ground deployment and two-device usability hardening.
- Lichess seam: Native browser websocket URL selection and local Test Ground site launcher.
- Lichess files touched: `ui/lib/src/socket.ts`.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Local mobile testing needs the stack to advertise a LAN URL, but a PC browser often uses `localhost`. If the PC page connects its round socket to the LAN IP, host-scoped session cookies can be absent or inconsistent and live round pages can show `Reconnecting`, preventing computer/opponent events from arriving. The socket client now prefers the page's current host for loopback/private local hosts while preserving configured socket domains elsewhere.
- Public UX effect: A PC browser can keep using `http://localhost:8080/` while a phone uses `http://<LAN-IP>:8080/`; both should keep authenticated round sockets and receive computer/opponent moves.
- Preserved Lichess capability: Production socket-domain selection remains configured by the server. The same-origin override is limited to loopback/private local hosts.
- Patch map entry: `PM-2026-151`.
- Tests / checks: Local HTML inspection confirmed the page was advertising the LAN socket domain. TypeScript compile/build checks cover the socket code path.
- Upstream update notes: Preserve the local/private same-origin socket preference if upstream rewrites `WsSocket.nextBaseUrl`, and keep Test Ground's PC open action independent from the LAN/mobile URL.
- Rollback notes: Reverting can restore local PC `Reconnecting` when the stack is configured for mobile LAN access.

### INT-2026-157 - Pinned-piece marker terminology and visual glyph

- Phase: V2 overlay terminology and visual polish.
- Lichess seam: Native round EvenChess board overlay renderer.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The pinned-piece overlay was conceptually correct but rendered the `pin` indicator through the Lichess padlock icon. EvenChess requirements now explicitly require pinned pieces to be represented as pins or `P`, never as locked pieces or lock/`L` markers.
- Public UX effect: Pinned pieces display a top-left `P` badge on the pinned square instead of a padlock/locked-piece icon.
- Preserved Lichess capability: Native board input and all non-EvenChess board overlays remain unchanged.
- Patch map entry: `PM-2026-152`.
- Tests / checks: The round overlay test asserts the `P` marker and rejects `Padlock` serialization.
- Upstream update notes: Preserve the `P` marker in `boardOverlayIndicatorFromVisual` and do not route pinned-piece indicators through `licon.Padlock`.
- Rollback notes: Reverting can make pinned pieces look like locked pieces again.

### INT-2026-158 - Live ECE threat overlay adapter hardening

- Phase: V2 overlay display reliability and local mobile/computer-game hardening.
- Lichess seam: Native EvenChess controller ECE bridge and native round overlay request adapter.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessTestGround.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Live boards, including computer games on mobile, rely on the ECL controller to normalize ECE public side outputs into approved round overlay visuals. ECE can provide threat facts as nested student/opponent groups, direct overlay arrays, or flat board-fact entries using `attacking_side` plus source/target squares. The adapter now accepts those shapes and keeps them mapped to the existing student/opponent dotted-arrow features. The browser adapter also avoids accepting high-level card-only responses as final before retrying for board visuals.
- Public UX effect: Threat arrows are less likely to disappear in live computer games because valid ECE threat facts are normalized across payload shapes, and partial high-level ECE responses get a short chance to fill board visuals before the UI settles.
- Preserved Lichess capability: Native legal move handling, clocks, computer-game lifecycle, and round rendering remain Lichess-owned. Browser code still calls only ECL same-origin endpoints, never ECE directly.
- Patch map entry: `PM-2026-153`.
- Tests / checks: `ui/round/tests/evenchessTestGround.test.ts` covers low-level card-only acceptance and high-level card-only retry until board visuals arrive. A local board-overlay endpoint smoke check with a threat FEN returned both student and opponent threat visuals.
- Upstream update notes: Preserve `threatArrayField`, `threatSideField`, expanded `fromTo` parsing, and the L2+ missing-visual retry cap if upstream rewrites the controller or round adapter.
- Rollback notes: Reverting can make live computer/mobile boards miss threat arrows when ECE emits a newer board-fact threat shape or initially returns a partial card-only response.

### INT-2026-159 - Per-game live display state survives round refresh

- Phase: V2 live display state persistence hardening.
- Lichess seam: Native round JSON bootstrap, EvenChess controller route table, and round overlay level/toggle controls.
- Lichess files touched: `conf/routes`; `app/controllers/EvenChess.scala`; `modules/round/src/main/JsonView.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `modules/evenchess/src/main/GamePolicy.scala`; `modules/evenchess/src/test/GamePolicyTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The browser can change display-only controls during a game, but refresh reconstructs the round from server JSON. Persisting only Used Level meant the dropdown and feature toggles returned to the game-start defaults. The new display-state endpoint stores the player's per-game dropdown/toggle state alongside the existing game policy and serializes it back into `evenchess.display.toggles` on later bootstraps.
- Public UX effect: Refreshing an EvenChess live/computer game keeps the current "Apply up to" dropdown and individual feature toggles for that player and game. Used Level still cannot decrease and Set Level remains server-authorized.
- Preserved Lichess capability: Native round lifecycle, legal move handling, clocks, and normal game JSON remain Lichess-owned. ECL stores only EvenChess display state.
- Patch map entry: `PM-2026-154`.
- Tests / checks: `GamePolicyTest` covers display-state persistence without lowering Used Level. `evenchessOverlay.test.ts` covers round initialization from server-persisted toggles.
- Upstream update notes: Preserve the authenticated `/evenchess/live/display-state` route, `StoredDisplayState`, `GamePolicyRepository.recordDisplayState`, and `evenchess.display.toggles` bootstrap JSON if upstream changes controller or round JSON structure.
- Rollback notes: Reverting can make refresh reset the level dropdown and toggles to starting preferences even though Used Level remains persisted.

### INT-2026-160 - Live coach card embeds potential/proposed controls

- Phase: V2 coach surface polish.
- Lichess seam: Native round overlay renderer and round stylesheet.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Potential Moves and Proposed Move are coach actions, but they were rendered as separate cards below the coach. The right column now keeps one main coach card, puts potential/proposed buttons in the bottom action area, and renders authorized potential/proposed text sections inside the coach text area after the main coach text.
- Public UX effect: The coach surface reads as one integrated panel instead of three stacked add-on panels. Pressing potential/proposed controls keeps the resulting text in the coach content region while the controls remain anchored at the bottom.
- Preserved Lichess capability: Native round board, move list, clocks, chat, and move input remain untouched.
- Patch map entry: `PM-2026-155`.
- Tests / checks: `evenchessOverlay.test.ts` asserts the controls are inside the coach card and no standalone proposed-control card appears in normal live play.
- Upstream update notes: Preserve `evenchess-live__coach-text`, `evenchess-live__coach-actions`, and inline coach-result sections if upstream round side-panel styling changes.
- Rollback notes: Reverting can bring back separate Potential Moves and Proposed Move cards below the coach card.

### INT-2026-161 - Live coach card Auto Speak toggle

- Phase: V2 coach TTS usability polish.
- Lichess seam: Native round overlay renderer and round stylesheet.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The manual Speak button already lives in the native round coach-card header. Auto-read needed an adjacent in-game control that toggles the same `autoSpeak` flag consumed by the existing TTS scheduler when new coach text arrives.
- Public UX effect: Players can turn automatic coach-card reading on or off from the live coach card without leaving the game. The persistent default and delay remain managed by EvenChess settings.
- Preserved Lichess capability: Native round board, move input, clocks, and server-authorized TTS safety checks remain unchanged.
- Patch map entry: `PM-2026-156`.
- Tests / checks: `evenchessOverlay.test.ts` asserts the Auto control renders and that toggling updates the round TTS auto-read config.
- Upstream update notes: Preserve `renderTtsAutoToggle`, `setEvenChessTtsAutoSpeakForData`, and `.evenchess-live__tts-auto` styling if upstream round header or TTS UI changes.
- Rollback notes: Reverting removes the in-game Auto Speak control, leaving only the account settings toggle.

### INT-2026-162 - Canonical ids for two-device public search handoff

- Phase: V2 matchmaking/MMR public search reliability.
- Lichess seam: Authenticated EvenChess public search JSON controller and native challenge/game handoff.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Local PC/mobile testing can use accounts whose display usernames preserve capitalization, while Lichess challenge acceptance and user lookup operate on canonical user ids. Public search tickets and redirect-ledger ownership now use `me.userId.value` so the MMR contract, public search-key polling, and native challenge handoff all reference the same server identity.
- Public UX effect: Two authenticated users searching the same EvenChess format from separate devices should finalize into a native game instead of polling forever after the MMR layer finds a compatible contract.
- Preserved Lichess capability: Native challenge/game creation, colors, clocks, game policy persistence, and redirect URLs remain Lichess-owned after EvenChess produces the match contract.
- Patch map entry: `PM-2026-157`.
- Tests / checks: `PlaySearchIntegrationTest` covers the logged phone/PC no-preference casual rapid 10+5 case with different starting set-level defaults.
- Upstream update notes: Preserve canonical user-id ticket ownership in `search`, `searchJson`, public search-key lookup, and challenge handoff if upstream auth/controller code changes.
- Rollback notes: Reverting can reintroduce case-sensitive display-username mismatches where Admin/Superadmin-style accounts match at MMR but never receive a game redirect.

### INT-2026-163 - Potential-move reveal turn gating

- Phase: V2 potential-move consumable and coach-action correctness.
- Lichess seam: Native EvenChess controller ECE bridge and native round coach-card renderer.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Potential Moves are server-authorized coach actions, but the browser renders their buttons inside the native round coach card. Opponent potential moves must only be revealed on the opponent's turn, and student potential moves must only be revealed on the student's turn. The controller now rejects wrong-turn requests, and the round UI disables the wrong-turn button before a network call.
- Public UX effect: Players see the available potential-move action for the current side to move and cannot spend or request the wrong potential-move reveal kind for the current turn.
- Preserved Lichess capability: Native move input, clocks, round polling, and server-authorized ECE bridge behavior remain unchanged. Browser code still calls only ECL same-origin endpoints.
- Patch map entry: `PM-2026-158`.
- Tests / checks: `evenchessOverlay.test.ts` covers client-side wrong-turn blocking and opponent-turn request success.
- Upstream update notes: Preserve `potentialMoveTurnAllowed`, server-side `not_opponent_turn`, and existing `not_your_turn` checks if upstream controller or round coach controls change.
- Rollback notes: Reverting can allow wrong-turn potential requests, including opponent potentials on the player's turn.

### INT-2026-164 - Public search game-handoff diagnostics

- Phase: V2 matchmaking/MMR public search reliability.
- Lichess seam: Native EvenChess public search controller and native challenge/game creation handoff.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: A live PC/phone search showed `matchmaking.matched=true` and a valid MMR contract while `redirectUrl=null`. That means the MMR pairing layer had succeeded, but the native Lichess challenge/game handoff returned no game redirect. The controller previously swallowed `challenge.create=false`, `challenge.accept=None`, accept exceptions, and missing user lookup cases as plain `None`, leaving the UI polling forever without an actionable reason.
- Public UX effect: No visible UX change. Local/server logs now expose a throttled safe reason for matched searches that cannot create a native game, so the handoff can be fixed without guessing.
- Preserved Lichess capability: Native challenge creation, challenge acceptance, game policy persistence, and redirect ledger behavior remain unchanged.
- Patch map entry: `PM-2026-159`.
- Tests / checks: `./lila.sh 'compile'` passed. Live pre-restart evidence showed one Admin-owned search response with a valid contract and no redirect; after restart no fresh search requests arrived during the monitoring window.
- Upstream update notes: Preserve throttled diagnostics around challenge creation and acceptance when changing `maybeCreateMatchedGameRedirect` or `createHumanMatchedGameRedirect`.
- Rollback notes: Reverting removes the diagnostic and makes matched-but-not-redirected searches opaque again.

### INT-2026-165 - ECE 06 Jun 2026 call-contract alignment

- Phase: V2 ECL-to-ECE gateway contract maintenance.
- Lichess seam: Native EvenChess controller routes, server-to-server ECE HTTP envelopes, gateway config, and local Test ECE fixture.
- Lichess files touched: `conf/routes`; `app/controllers/EvenChess.scala`; `ui/round/src/evenchessTestGround.ts`.
- EvenChess files touched: `modules/evenchess/src/main/EngineGateway.scala`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE's 06 Jun 2026 contract keeps route URLs stable but changes call semantics. Proposed ECS now expects `input_fen` plus canonical `proposed_move_san`; Advanced ECS may be polled by `advanced_job_id`; Performance ECS is now `/v1/ece/performance-summary`. ECL owns the browser-safe bridge, so it must derive SAN from FEN+arrow server-side, pass async deep job IDs safely, and expose performance summary only through same-origin ECL routes.
- Public UX effect: Proposed Move requests are less likely to be rejected by new ECE because ECL sends canonical SAN derived from the current board. Deep/advanced addenda can degrade without clearing quick output when async work is pending. Performance-summary diagnostics can be tested through the ECL bridge.
- Preserved Lichess capability: Native round move input, board legality, clocks, and browser privacy boundaries remain unchanged. Browser code still calls only ECL same-origin endpoints and never ECE analysis endpoints directly.
- Patch map entry: `PM-2026-160`.
- Tests / checks: `EngineGatewayTest` covers endpoint config, async deep request handles, and SAN/UCI proposed-move validation. `scripts/evenchess-test-ece-server.test.mjs` covers Test ECE quick/deep/proposed/full-match/match-summary/performance-summary route shapes.
- Upstream update notes: Preserve `sanForProposedMove`, `advanced_job_id` support, `performanceSummaryUrl`, and the same-origin `/evenchess/testground/ece/performance-summary` route when moving the ECE bridge out of the controller.
- Rollback notes: Reverting can make real ECE proposed-move calls fail under the SAN-canonical contract, lose async Advanced ECS addenda, and leave performance-summary testing without an ECL bridge.

### INT-2026-166 - Threat-line adapter and overlay stacking hardening

- Phase: V2 display-engine reliability.
- Lichess seam: Native EvenChess controller ECE payload adapter and native round board-overlay stylesheet.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECE-compatible threat board facts may arrive under equivalent alias fields or nested move/attack/arrow objects, while the browser board layer must draw EvenChess dotted threat arrows above Chessground SVGs across desktop and mobile. The controller now normalizes more threat shapes into the same student/opponent arrow families, and the round stylesheet raises the custom overlay to Lichess's board-overlay z-index tier while preserving `pointer-events: none`.
- Public UX effect: Student and opponent threat lines should appear more consistently on mobile and desktop when the payload contains level-allowed threats and the corresponding toggles are on.
- Preserved Lichess capability: Native board input, piece dragging, Chessground shapes, and round clocks remain unchanged.
- Patch map entry: `PM-2026-161`.
- Tests / checks: `pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts` passed; `./lila.sh "compile"` passed through Docker fallback.
- Upstream update notes: Preserve `threatArrayField`, nested threat move extraction, and `.evenchess-board-overlay` z-index behavior if upstream board rendering changes.
- Rollback notes: Reverting can make valid ECE threat facts disappear when they use alternate shapes or can leave arrows hidden behind board SVG layers in some layouts.

### INT-2026-167 - Per-game level and toggle refresh persistence

- Phase: V2 live display-state persistence.
- Lichess seam: Native EvenChess live display-state controller endpoints and native round overlay renderer.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The user-facing level selector and feature toggles live in the round UI, but the per-game Used Level and display state must be authoritative and reloadable from the server. The save path now writes with canonical user ids, accepts the server's saved display-state echo, and keeps the "Apply up to" dropdown tied to the applied preset level instead of deriving it from whichever toggles are currently enabled.
- Public UX effect: Refreshing a live game should keep the current level selection, per-feature toggles, and monotonic Used Level until the player changes them in that game.
- Preserved Lichess capability: Native round board, move input, clocks, and server-authenticated round bootstrap remain unchanged.
- Patch map entry: `PM-2026-162`.
- Tests / checks: `evenchessOverlay.test.ts` covers refreshed Used Level and applied-level dropdown behavior; `GamePolicyTest` covers monotonic server Used Level/display state; `./lila.sh "compile"` passed.
- Upstream update notes: Preserve `recordDisplayState`, `recordUsedLevel`, canonical user-id usage, and `appliedEvenChessDisplayLevel` if upstream round/controller code changes.
- Rollback notes: Reverting can make browser refresh fall back to account defaults or infer the selector from toggles, causing the level/toggle UI to appear to reset mid-game.

### INT-2026-168 - Live opponent Set/Used Level display

- Phase: V2 live policy display.
- Lichess seam: Native round JSON bootstrap, native same-origin ECE bridge response, and native EvenChess round overlay renderer.
- Lichess files touched: `app/controllers/EvenChess.scala`; `modules/round/src/main/JsonView.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Live players need to see both sides' EvenChess policy state, but Set Level and Used Level are server-owned per-game values. The round bootstrap and live overlay response now serialize the opponent's Set Level and current Used Level from the stored EvenChess policy/display state, and the level card displays those values without client-side inference.
- Public UX effect: In a live EvenChess game, the level card shows the local player's Set/Used Levels and the opponent's Set/Used Levels when policy data exists.
- Preserved Lichess capability: Native round board, move input, clocks, and authenticated round bootstrap remain unchanged.
- Patch map entry: `PM-2026-163`.
- Tests / checks: `evenchessOverlay.test.ts` verifies opponent Set/Used Level rendering in the live level card and live overlay refresh updates.
- Upstream update notes: Preserve `evenchess.display.opponent` in round JSON, optional `live.display.opponent` in live overlay payloads, and the level-card opponent pills if upstream round JSON, ECE bridge, or overlay headers change.
- Rollback notes: Reverting hides opponent level state from the live card even though the local player's levels remain visible.

### INT-2026-169 - Disabled level dropdown options are visibly greyed

- Phase: V2 live level-control polish.
- Lichess seam: Native EvenChess round overlay renderer and round stylesheet.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The live "Apply up to" dropdown is capped by the server-authorized Set Level, but disabled options above that cap need to look unavailable in the dark dropdown. The renderer now adds explicit disabled metadata and classing, and the stylesheet greys out disabled options.
- Public UX effect: Players can see which higher levels are unavailable before trying to select them.
- Preserved Lichess capability: Native round board, clocks, move input, and server-owned Set Level remain unchanged.
- Patch map entry: `PM-2026-164`.
- Tests / checks: `evenchessOverlay.test.ts` verifies disabled/classed/titled/aria-disabled dropdown options above Set Level.
- Upstream update notes: Preserve disabled option styling and metadata if the level dropdown or round stylesheet is changed.
- Rollback notes: Reverting can make disabled levels appear selectable even though the browser still blocks selection.

### INT-2026-170 - Mobile-safe TTS and text-change-only auto-read

- Phase: V2 live coach TTS hardening.
- Lichess seam: Shared browser TTS helper and native EvenChess round coach-card renderer.
- Lichess files touched: `ui/lib/src/evenchessTts.ts`; `ui/lib/tests/evenchessTts.test.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Live TTS is controlled by EvenChess policy but executed through browser speech synthesis. Mobile browsers are stricter about `window` speech APIs and resume state, and the live coach card must keep Speak/Auto controls available during opponent turns when the text is safe. Auto-read also needs to follow displayed text changes, not payload/audit churn.
- Public UX effect: Mobile browsers have a more compatible speech driver path, manual Speak and Auto remain usable on opponent turns, and unchanged coach text is not spoken again just because the payload updated.
- Preserved Lichess capability: Native round board, move input, clocks, and server-authorized coach text remain unchanged.
- Patch map entry: `PM-2026-165`.
- Tests / checks: `evenchessTts.test.ts` covers opponent-turn availability and mobile-safe driver behavior; `evenchessOverlay.test.ts` covers unchanged-text auto-read suppression.
- Upstream update notes: Preserve `window.SpeechSynthesisUtterance`, `speechSynthesis.resume()`, opponent-turn availability, and normalized text auto-read keys if upstream TTS or round overlay code changes.
- Rollback notes: Reverting can make mobile TTS fail more often, disable controls on opponent turns, and repeat unchanged coach text after payload-only updates.

### INT-2026-171 - Executed premoves skip live ECE refresh

- Phase: V2 live ECE timing hardening.
- Lichess seam: Native round controller premove execution path and EvenChess same-origin ECE overlay request helper.
- Lichess files touched: `ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/tests/evenchessTestGround.test.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Chessground premoves are committed inside native round move handling immediately after the opponent's move, while EvenChess ECE refreshes are triggered from the same position-change flow. ECE should not be called for a transient post-opponent-move position that immediately auto-commits a premove, nor for the committed premove acknowledgement, but failed/cancelled premoves still need the normal current-position ECE refresh.
- Public UX effect: Executed premoves do not spend ECE work or produce stale coaching for a move the player already committed before seeing the opponent's reply. Normal move updates and failed premove positions continue to refresh.
- Preserved Lichess capability: Native premove setting, cancelling, execution, predrops, move submission, clocks, and notifications remain in the native round flow.
- Patch map entry: `PM-2026-166`.
- Tests / checks: `evenchessTestGround.test.ts` verifies skipped premove positions do not fetch and stale in-flight ECE responses do not requeue them.
- Upstream update notes: Preserve `evenChessCommittedPremoveUci`, delayed post-opponent ECE refresh, and `skipReason: 'executed-premove'` handling if upstream round premove or overlay-request code changes.
- Rollback notes: Reverting can cause ECE calls to run for positions created only by committed premove timing, including stale transient positions.

### INT-2026-172 - Position ECS Ask AI live coach action

- Phase: V2 live Position ECS integration.
- Lichess seam: Native EvenChess controller ECE bridge, native round coach-card renderer, ECE test harness, and EvenChess gateway config.
- Lichess files touched: `app/controllers/EvenChess.scala`; `conf/routes`; `modules/evenchess/src/main/EngineGateway.scala`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`.
- Why this seam exists: ECE Advanced ECS now stays deterministic/provider-backed, while optional live AI text is requested through Position ECS. The browser must not call ECE directly, so ECL owns a same-origin `/evenchess/testground/ece/position-ecs` bridge that enforces per-game own-move token accrual from the player's Used Level before posting to ECE `/v1/ece/position`. The round coach card exposes an Ask AI button, shows used/accrued counts, and toggles cached Position ECS text for the same FEN without spending another call.
- Public UX effect: Players at L4+ can ask for AI coaching only after enough of their own moves have accrued an allowance. L0-L3 cannot use Ask AI, L4 accrues one call every ten own moves, and L10 accrues one every four own moves. Advanced ECS board-state payloads remain deterministic and do not spend or invoke live AI.
- Preserved Lichess capability: Native round board, clocks, move input, deterministic coach payloads, potential moves, proposed moves, and server-authorized display gating remain unchanged.
- Patch map entry: `PM-2026-167`.
- Tests / checks: `EngineGatewayTest.scala`, `evenchessOverlay.test.ts`, `evenchessTestGround.test.ts`, and `evenchess-test-ece-server.test.mjs` were updated for Position ECS request/response shape, same-origin routing, and level-based accrual.
- Upstream update notes: Preserve `positionPath`/`positionUrl`, ECL server-side allowance enforcement, cached same-position toggle behavior, and the rule that `ai_text` is not an Advanced ECS deep board-state module.
- Rollback notes: Reverting removes live Ask AI support and can accidentally reintroduce Advanced ECS AI-text assumptions that conflict with the ECE 06 Jun 2026 contract.

### INT-2026-173 - Potential ECS replaces Advanced ECS candidate reveal

- Phase: V2 live Potential ECS integration.
- Lichess seam: Native EvenChess controller ECE bridge, EvenChess gateway config, and ECE test harness.
- Lichess files touched: `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EngineGateway.scala`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `modules/evenchess/src/test/EngineGatewayTest.scala`.
- Why this seam exists: ECE moved potential/candidate moves out of Advanced ECS and into `POST /v1/ece/potential`. The browser still needs a same-origin reveal button flow, so ECL now authorizes and accounts for the reveal before making the server-to-server Potential ECS call. Board-state/Advanced ECS payloads remain free of unrevealed potential-move data.
- Public UX effect: Opponent/My Potential Moves continue to work through the existing coach-card buttons, but the source data now comes from Potential ECS for the FEN side to move. Unavailable, low-level, or missing-Stockfish responses fail without changing the normal board overlay.
- Preserved Lichess capability: Native board, clocks, move input, proposed-move previews, Position ECS Ask AI, and normal board-state ECE refresh remain unchanged.
- Patch map entry: `PM-2026-168`.
- Tests / checks: `EngineGatewayTest.scala` covers Potential ECS request/config shape. `evenchess-test-ece-server.test.mjs` covers `/v1/ece/potential` and verifies deep board addenda no longer expose candidate moves.
- Upstream update notes: Preserve `potentialPath`/`potentialUrl`, the same-origin reveal gate, turn-side validation from current FEN, and the rule that browser code must not call ECE Potential ECS directly.
- Rollback notes: Reverting can make Potential Move buttons depend on stale Advanced ECS candidate fields that the current ECE contract no longer returns.

### INT-2026-174 - Standard ECS live bridge and Position ECS eval source

- Phase: V2 live ECE contract migration.
- Lichess seam: Native EvenChess controller ECE bridge, EvenChess gateway config, native round coach-card renderer, and local ECE test harness.
- Lichess files touched: `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/EngineGateway.scala`; `ui/round/src/interfaces.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `modules/evenchess/src/test/EngineGatewayTest.scala`; `modules/evenchess/src/test/LiveCoachingTest.scala`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`.
- Why this seam exists: ECE no longer exposes live board-state as Initial/Advanced quick/deep. ECL now calls Standard ECS every move for deterministic overlays/text, stores any returned `position_ecs_id`, and uses Position ECS only when Ask AI is authorized so the AI replacement text and provider eval can temporarily replace the Standard coach payload.
- Public UX effect: Normal live coaching remains fast and deterministic each move. Eval and AI coaching update only when the user spends or toggles an allowed Ask AI result, so the eval bar does not snap to placeholder Standard values.
- Preserved Lichess capability: Native board, clocks, move input, proposed-move preview, Potential ECS reveal buttons, full-game summaries, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-169`.
- Tests / checks: Gateway, live coaching, Test Ground, overlay, and local Test ECE tests cover Standard ECS, Position ECS context propagation, and Position ECS eval rendering.
- Upstream update notes: Preserve `standardPath`/`standardUrl`, `position_ecs_id` passthrough, Position ECS-only eval acceptance, and the absence of deep-module board-state merging.
- Rollback notes: Reverting can reintroduce stale quick/deep calls that current ECE no longer treats as the canonical live board-state contract.

### INT-2026-175 - Potential ECS current-position eval source

- Phase: V2 live Potential ECS eval integration.
- Lichess seam: Native EvenChess controller ECE bridge and native EvenChess round overlay renderer.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`; `ui/round/tests/evenchessOverlay.test.ts`.
- Why this seam exists: ECE Potential ECS now returns `potential_ecs.evaluation` as the current-position Stockfish eval for an authorized potential-move reveal. Browser code still must not call ECE directly, so the controller sanitizes that eval into the same approved display payload model used by Position ECS/proposed-move eval, while the round renderer treats active Potential ECS as a transient eval source.
- Public UX effect: When the player spends or toggles a Potential Moves reveal at an eval-enabled level, the eval bar and coach eval strip update from `potential_ecs.evaluation`. Candidate move scores remain per-move details and do not drive the overall eval bar. After the board changes, the eval bar clears until a fresh Potential ECS, Position ECS, or proposed-move eval result is active.
- Preserved Lichess capability: Native round board, clocks, move input, deterministic Standard ECS coaching, proposed-move preview, Position ECS Ask AI, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-170`.
- Tests / checks: `evenchess-test-ece-server.test.mjs` covers the Potential ECS current eval fixture. `evenchessOverlay.test.ts` covers active-reveal eval rendering, board-change clearing, and no Standard/live eval carryover.
- Upstream update notes: Preserve `potential_ecs.evaluation` as the only Potential ECS overall eval source and keep `potential_ecs.moves[*].score` out of eval-bar logic.
- Rollback notes: Reverting can make Potential Moves reveal candidate moves without updating eval, or can reintroduce stale Standard/live eval display between moves.

### INT-2026-176 - Top-bar EvenChess Help popup

- Phase: V2 public shell/top-bar polish.
- Lichess seam: Shared site header buttons/dropdowns and header CSS.
- Lichess files touched: `modules/web/src/main/ui/layout.scala`; `ui/lib/css/header/_buttons.scss`.
- EvenChess files touched: `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The player-facing help entry belongs in the native top bar beside the existing search/account/token controls, while the guide copy remains EvenChess-owned and testable. The dropdown uses the existing Lichess toggle/dropdown behavior and adds only EvenChess-specific copy and styling.
- Public UX effect: Users can open a brief scrollable "EvenChess Help" guide from the top bar explaining levels, matchmaking, ECR fairness, platform-only coaching, and token/plan limits.
- Preserved Lichess capability: Native top-bar search, notifications, token balance, account dasher, anonymous dasher, and existing dropdown behavior remain unchanged.
- Patch map entry: `PM-2026-171`.
- Tests / checks: `PublicShellTest.scala` covers guide copy safety and content scope.
- Upstream update notes: Preserve the `evenchess-help` top-bar item, dropdown scroll constraints, and `PublicShell.PublicCopy` ownership when merging future header changes.
- Rollback notes: Reverting removes the in-context public explanation surface and leaves players dependent on homepage/landing copy for level and matchmaking basics.

### INT-2026-177 - Live display toggle overlay stability

- Phase: V2 live display controls hardening.
- Lichess seam: Native EvenChess round overlay renderer and browser-side per-game display-state persistence.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Live toggles must filter the already approved ECE payload immediately without clearing unrelated overlays, while higher-level toggle use can still raise Used Level and request a richer Standard ECS payload in the background. Display-state persistence responses are acknowledgements and must not overwrite newer local toggle choices.
- Public UX effect: Turning individual level-card toggles on/off hides or shows only the mapped board/coach items. Existing safe board overlays stay visible while a higher-level Standard ECS refresh catches up.
- Preserved Lichess capability: Native board input, clocks, move flow, Standard ECS refresh, proposed/potential/Ask AI controls, and persisted per-game Used Level remain unchanged.
- Patch map entry: `PM-2026-172`.
- Tests / checks: `evenchessOverlay.test.ts` covers coach-only/higher-level toggle changes preserving current board visuals and stale/sparse persistence acknowledgements not erasing local feature toggles.
- Upstream update notes: Preserve conservative display-state acknowledgement merging and in-place board overlay filtering when future round-overlay changes touch level-card controls.
- Rollback notes: Reverting can make toggles or late persistence responses blank current overlays or roll the visible level/toggle state backward while a refresh is in flight.

### INT-2026-178 - Ask AI coach-card action visibility

- Phase: V2 Position ECS action discoverability.
- Lichess seam: Native EvenChess round overlay renderer and live coach-card action stack.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Position ECS is an explicit user action, but the live coach-card controls previously placed the `Ask AI` action after Potential Moves and Proposed Move. The action now appears first in the coach-card action area and stays visible with status text even before a use is earned.
- Public UX effect: Players can see where to request AI coaching/eval, and unavailable states explain availability as `Available in X moves` or `Available at Level 4+` instead of making the feature look absent.
- Preserved Lichess capability: Native board input, clocks, Standard ECS updates, Potential ECS reveals, Proposed Move previews, TTS, and server-side Position ECS authorization remain unchanged.
- Patch map entry: `PM-2026-173`.
- Tests / checks: `evenchessOverlay.test.ts` covers action ordering and the disabled-but-visible pre-accrual state.
- Upstream update notes: Preserve the `Ask AI` first action order and visible status text when future coach-card action layout changes.
- Rollback notes: Reverting can make Position ECS appear missing to users even though the backend action exists.

### INT-2026-179 - Proposed Move post-move Standard ECS preview payload

- Phase: V2 Proposed ECS contract alignment.
- Lichess seam: Native EvenChess controller ECE proposed-move bridge and local Test ECE fixture.
- Lichess files touched: `app/controllers/EvenChess.scala`; `scripts/evenchess-test-ece-server.js`; `scripts/evenchess-test-ece-server.test.mjs`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Proposed Move previews should show the same deterministic board/coach payload the player would see if the move happened. ECL now prefers `proposed_move_evaluation.after_move_standard_ecs` with normal Standard ECS `side_outputs`, while still accepting the legacy `after_move_side_output` migration shortcut.
- Public UX effect: Pressing Proposed Move displays the hypothetical after-move overlay/cards from the Standard ECS-style payload. Pressing it again toggles back to the cached current-FEN payload, and pressing again re-shows the cached proposed preview without spending another call.
- Preserved Lichess capability: Native board input, current-position Standard ECS history, proposed-move quota accounting, illegal-arrow preservation, Potential ECS, Position ECS, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-174`.
- Tests / checks: Existing `evenchessOverlay.test.ts` covers proposed preview toggle/cache behavior. `evenchess-test-ece-server.test.mjs` covers the Test ECE `after_move_standard_ecs` fixture. Full Scala `./lila.sh compile` passed.
- Upstream update notes: Preserve Standard ECS-first proposed preview parsing and the legacy fallback until ECE no longer emits `after_move_side_output`.
- Rollback notes: Reverting can make proposed previews lose the full hypothetical Standard payload and only display text or legacy partial side-output data.

### INT-2026-180 - Live coach TTS visible-text and auto-delta alignment

- Phase: V2 live coach TTS polish.
- Lichess seam: Native EvenChess round overlay renderer and live coach-card TTS controls.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The coach card now displays base Standard/Ask AI text plus inline Proposed Move and Potential Moves result text inside one coach text area. Manual and Auto TTS must use the allowed spoken subset while still passing the shared TTS safety check.
- Public UX effect: Manual Speak reads the base coach text plus active Ask AI and Proposed Move content. Potential Moves text remains visual-only. Auto Speak reads only newly appended Proposed Move text, reads replacement Ask AI text when the summary is replaced, and does not re-read the old normal summary just because an inline result is toggled off.
- Preserved Lichess capability: Native round board, move input, clocks, coach-card layout, server-authorized TTS safety policy, and user TTS settings remain unchanged.
- Patch map entry: `PM-2026-175`.
- Tests / checks: `evenchessOverlay.test.ts` covers full visible-text TTS composition and auto-delta text selection.
- Upstream update notes: Preserve `liveCardTtsItem`, `coachInlineResultTtsTexts`, and the auto-delta state fields if future round coach-card rendering or TTS controls change.
- Rollback notes: Reverting can make TTS speak text that differs from the intended spoken source, read Potential Moves option text, or auto-read the entire coach card instead of only the added/replaced text.

### INT-2026-181 - ECE-owned coach text length

- Phase: V2 ECE adapter/display contract hardening.
- Lichess seam: Native EvenChess controller ECE parser and EvenChess Display Engine renderability.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `modules/evenchess/src/main/CoachingOverlays.scala`; `modules/evenchess/src/main/LiveOverlayUi.scala`; `modules/evenchess/src/test/CoachingOverlaysTest.scala`; `modules/evenchess/src/test/LiveOverlayUiTest.scala`; `modules/evenchess/src/test/EceLiveBridgeTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECL must sanitize ECE public payloads before they become round payloads, but ECE now owns coach text length. The parser no longer truncates Summary/Plan/Warning/Ask AI/Proposed Move text, and Display Engine compact text budgets are advisory metadata rather than renderability gates.
- Public UX effect: Longer ECE-authored coach text can display and be read by TTS without being locally cut off or suppressed by ECL character budgets.
- Preserved Lichess capability: Native round board, clocks, move input, server authorization, forbidden-field redaction, bounded UI labels/ids/audit ids/error messages, and fixed-size potential-move option limits remain unchanged.
- Patch map entry: `PM-2026-176`.
- Tests / checks: `EceLiveBridgeTest.scala` covers long ECE summary rendering. `CoachingOverlaysTest.scala` and `LiveOverlayUiTest.scala` cover advisory budget behavior.
- Upstream update notes: Preserve ECE-owned coach text length in future controller parser and Display Engine changes; do not reintroduce local `.take(...)` caps for coach body fields.
- Rollback notes: Reverting can truncate ECE coach text or suppress otherwise safe long ECE cards before the player sees them.

### INT-2026-182 - Failed ECE action calls do not consume assistance uses

- Phase: V2 ECE action accounting hardening.
- Lichess seam: Native EvenChess controller same-origin action routes for Proposed Move, Potential ECS, and Position ECS / Ask AI.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: ECL owns browser-safe routing and per-game quota accounting for consumable ECE actions. A failed transport call, unavailable ECE result, parser rejection, forbidden-field rejection, `ok:false`, or otherwise non-displayable action payload must not decrement the player's Proposed Move, Potential Moves, or Ask AI allowance.
- Public UX effect: Players keep their consumable uses when ECE fails or returns no approved display payload. The browser receives the current server-side count instead of a consumed use.
- Preserved Lichess capability: Native round board, move input, clocks, legal-arrow validation, cached successful action replay, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-177`.
- Tests / checks: Focused Scala compile validates the changed controller route/helper shape. A future controller harness should assert rejected action responses return without changing consumed counts.
- Upstream update notes: Preserve `approvedDisplayPayload` / `serverAuthorized` gating before any action payload is cached or counted.
- Rollback notes: Reverting can consume player allowances when ECE is unavailable or returns a non-displayable action result.

### INT-2026-183 - Admin unlimited action-token debug setting

- Phase: V2 debug/admin assistance-token bypass.
- Lichess seam: Native `/dev/settings` setting store, native EvenChess controller action routes, and native round overlay action counters.
- Lichess files touched: `app/controllers/EvenChess.scala`; `app/controllers/Dev.scala`; `modules/web/src/main/Env.scala`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`.
- EvenChess files touched: `modules/evenchess/src/main/AdminBackendSettings.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Admins need a local/staged debug switch to test Ask AI, Potential ECS, and Proposed Move repeatedly without exhausting per-game action allowances or waiting for Ask AI move accrual. The bypass must be decided server-side from the authenticated admin/settings context, while the round UI needs the server quota marker to show `Unlimited`.
- Public UX effect: Non-admin users remain on normal quotas. Admin/settings users with the debug setting enabled can repeatedly use eligible action buttons and see `Unlimited` counters.
- Preserved Lichess capability: Native round board, clocks, move input, current-FEN checks, legal-arrow validation, side/turn checks, Set Level gates, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-178`.
- Tests / checks: Focused Scala compile and TypeScript overlay checks should validate the changed setting snapshot, controller helper signatures, and UI quota display.
- Upstream update notes: Preserve the `Admin unlimited tokens` setting as admin-only and server-authoritative if action route or setting-store seams change.
- Rollback notes: Reverting removes the admin debug bypass and returns admins to normal action quotas and Ask AI move-accrual waits.

### INT-2026-184 - Ask AI own-turn enforcement

- Phase: V2 Position ECS / Ask AI authorization hardening.
- Lichess seam: Native EvenChess controller same-origin Position ECS route and native round overlay Ask AI button.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Ask AI is a same-origin ECL action backed by Position ECS. It must be available only to the student/requester when that side is to move, and the backend must enforce that before cached result replay or ECE transport.
- Public UX effect: The Ask AI button remains visible off-turn but disabled with `Available on your turn`; direct off-turn calls return a safe `not_requester_turn` response without consuming allowance.
- Preserved Lichess capability: Native round board, clocks, move input, normal Standard ECS updates, TTS, Potential ECS turn gates, Proposed Move legal-arrow/turn gates, admin unlimited quotas, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-179`.
- Tests / checks: `evenchessOverlay.test.ts` covers off-turn status and no client fetch.
- Upstream update notes: Preserve Ask AI own-turn gating if the coach-card action stack or Position ECS route changes.
- Rollback notes: Reverting can allow cached or direct Ask AI calls during the opponent turn.

### INT-2026-185 - Coach action button copy and sizing

- Phase: V2 live coach-card UI polish.
- Lichess seam: Native EvenChess round overlay coach-card action stack.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The live coach card owns the Ask AI, Potential ECS, and Proposed Move controls. Their copy and button sizing need to be polished in the same render/CSS path that already enforces action availability and server-authorized requests.
- Public UX effect: The extra `Potential Moves` heading is removed. The action labels read `Ask AI`, `Show Opponents potential moves`, `Show my potential moves`, and `Assess my proposed move`, and the four buttons use a shared width.
- Preserved Lichess capability: Native board input, clocks, Standard ECS refresh, Potential ECS turn gates, Proposed Move preview, Ask AI / Position ECS gating, and TTS remain unchanged.
- Patch map entry: `PM-2026-180`.
- Tests / checks: `evenchessOverlay.test.ts` covers the labels, order, and removed heading.
- Upstream update notes: Preserve the simplified action stack if future coach-card rendering changes.
- Rollback notes: Reverting restores the shorter labels, uneven action button widths, and duplicate Potential Moves heading.

### INT-2026-186 - Coach Draw mode for proposed-move input

- Phase: V2 mobile/desktop proposed-move input.
- Lichess seam: Native round Chessground controller input routing and EvenChess coach-card header.
- Lichess files touched: `ui/round/src/ctrl.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Mobile browsers do not expose the native desktop right-click drawing gesture needed for proposed-move arrows. The coach-card Draw toggle must switch board pointer/touch input into green Chessground shape creation without weakening server-side proposed-move validation.
- Public UX effect: Players can press `Draw`, touch/click-drag the board to create a green arrow or tap/click one square for a green circle, then press `Assess my proposed move`. While Draw is active, board touch scrolling and piece movement are disabled; turning Draw off restores normal move input.
- Preserved Lichess capability: Native right-click drawing, normal piece movement, premoves, legal-arrow validation, proposed-move quotas, current-FEN checks, and server-to-server ECE privacy remain unchanged when Draw mode is off.
- Patch map entry: `PM-2026-181`.
- Tests / checks: `evenchessOverlay.test.ts` covers the Draw button and active render state. Controller gesture behavior is validated through focused TypeScript/style checks.
- Upstream update notes: Preserve `getKeyAtDomPos`, `setShapes`, movement-disable, and `blockTouchScroll` use if Chessground input plumbing changes.
- Rollback notes: Reverting leaves mobile users without a reliable way to draw the green proposed-move arrow.

### INT-2026-187 - Mobile-safe EvenChess browser TTS driver

- Phase: V2 live coach TTS mobile compatibility.
- Lichess seam: Shared browser UI TTS driver used by the native EvenChess round coach-card controls.
- Lichess files touched: `ui/lib/src/evenchessTts.ts`.
- EvenChess files touched: `ui/lib/tests/evenchessTts.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Mobile browsers can fail to play Web Speech output when a user tap is preceded by unnecessary cancellation, when synthesis is paused, or when the first utterance is dropped before starting. The shared EvenChess TTS driver must handle that without weakening the safety rule that speech text equals visible authorized coach text.
- Public UX effect: Manual Speak on mobile preserves the tap activation, resumes browser speech, uses the selected/default voice safely, and retries once if the browser drops the first utterance. Auto Speak continues to use the same driver after it has been enabled/unlocked by user interaction.
- Preserved Lichess capability: Native round controls, TTS authorization checks, unsafe-payload filtering, visible-text matching, and user TTS settings remain unchanged.
- Patch map entry: `PM-2026-182`.
- Tests / checks: `evenchessTts.test.ts` covers the mobile-safe driver behavior.
- Upstream update notes: Preserve the no-op cancel guard, resume-before-speak, language fallback, and one-shot retry behavior if shared browser speech code changes.
- Rollback notes: Reverting can make mobile Speak fail silently again even while desktop browser speech works.

### INT-2026-188 - EvenChess Help level ladder copy

- Phase: V2 public help polish.
- Lichess seam: Top-bar EvenChess Help popup copy model.
- Lichess files touched: `modules/evenchess/src/main/PublicShell.scala`.
- EvenChess files touched: `modules/evenchess/src/test/PublicShellTest.scala`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Players need a concise launch-safe explanation of what each assistance level gives them before or during play. Repeated allowance features, including Proposed Move, must be explained at first unlock and then referenced as extra uses at later levels.
- Public UX effect: The top-bar help popup now includes an L1-L10 level guide covering rules, safety, offset count, threats, pins, Ask AI accrual, opponent/my potentials, Proposed Move allowances, WikiBook, eval, expert notes, and full co-pilot coverage.
- Preserved Lichess capability: Native top-bar dropdown behavior, public navigation, account routes, and normal Lichess feature links remain unchanged.
- Patch map entry: `PM-2026-183`.
- Tests / checks: `PublicShellTest.scala` covers L1-L10 copy and the first-unlock Proposed Move explanation rule.
- Upstream update notes: Preserve the `PublicCopy.helpGuideSections` source if top-bar rendering changes.
- Rollback notes: Reverting removes the level-by-level explanation from the help popup.

### INT-2026-189 - Live level indicator row layout and coach Used Level badge

- Phase: V2 live round UI polish.
- Lichess seam: Native EvenChess round overlay levels card and coach-card header.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The live levels card must display the local player's Set/Used row above the opponent's Set/Used row with equal-width indicators on both desktop and mobile. The coach card must label the active badge as Used Level so players do not confuse payload/card level with the game's retained Used Level.
- Public UX effect: The levels card shows `Set Level` and `Used Level` in the first row, `Opponent Set` and `Opponent Used` in the second row when available, and all pills share equal columns. The coach badge now reads `Used Level: X`.
- Preserved Lichess capability: Native round layout, move input, coach actions, TTS controls, Draw mode, and level-toggle behavior remain unchanged.
- Patch map entry: `PM-2026-184`.
- Tests / checks: `evenchessOverlay.test.ts` covers the two-row level summary order and coach-card Used Level badge.
- Upstream update notes: Preserve `evenchess-live__level-summary` row/column behavior and the Used Level wording if the live overlay header changes.
- Rollback notes: Reverting can reintroduce mobile wrapping where opponent values sit beside or above player values, and can restore ambiguous `Level X` coach badge wording.

### INT-2026-190 - Coach TTS excludes Potential Moves text

- Phase: V2 live coach TTS polish.
- Lichess seam: Native EvenChess round overlay coach-card TTS source.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Potential Moves are displayed as visual/choice options in the coach card, but they should not be spoken by manual or automatic TTS. The round overlay must therefore build the speech source from base coach text plus Ask AI/Proposed Move text only.
- Public UX effect: Visible Potential Moves remain available in the coach card, while Speak and Auto Speak skip those option lines.
- Preserved Lichess capability: Native round layout, board input, Potential ECS reveal buttons, Proposed Move advice, Ask AI replacement text, and TTS authorization checks remain unchanged.
- Patch map entry: `PM-2026-185`.
- Tests / checks: `evenchessOverlay.test.ts` verifies visible potential text is excluded from the TTS source.
- Upstream update notes: Preserve the visual-only Potential Moves rule if coach-card result rendering or TTS source construction changes.
- Rollback notes: Reverting can make TTS read Potential Moves option lists again.

### INT-2026-190 - Test Ground shared ECE local AI model control

- Phase: Test Ground ECE deployment controls.
- Lichess seam: Local-only Test Ground launcher panel and PowerShell lifecycle script that start the private ECE provider stack.
- Lichess files touched: `scripts/evenchess-testground-panel.js`; `scripts/evenchess-testground.ps1`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Test Ground must not own a second local model setting. It now mirrors ECE's shared local Ollama model aliases, discovers installed Ollama models when available, and keeps OpenAI keys ECE-only/write-only.
- Public UX effect: No production/browser route calls ECE directly or receives secrets. Test Ground operators can change ECE launch sizing and local model values, while Match/Performance `Use Local` inherits the same Position/local model.
- Preserved Lichess capability: Local panel launch controls, ECE server-to-server boundary, debug status, and Start/Stop Real ECE workflow remain unchanged.
- Tests / checks: Syntax and focused ECE admin settings tests cover the private settings side; Test Ground panel/PowerShell parse checks cover the launcher side.
- Rollback notes: Reverting can reintroduce stale split-brain model settings between Test Ground launch JSON and ECE settings.

### INT-2026-191 - Live display-state hydration for threat toggles

- Phase: V2 live overlay display-state persistence.
- Lichess seam: Same-origin EvenChess live overlay refresh JSON and native round overlay display-state renderer.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Threat arrows are gated by the in-game feature toggles. Live refreshes and local computer/no-policy rounds must not silently reset the current display state to L0/all-off, because that makes one client hide threat arrows while another client with a different saved state can show them.
- Public UX effect: A browser reload or live ECE refresh keeps the player's selected Used Level and per-feature toggles, including separate student/opponent threat arrows, when those choices are allowed by the game's Set Level.
- Preserved Lichess capability: Normal round move input, ECE server-to-server privacy, Set Level caps, monotonic Used Level rules, and server policy authority remain unchanged. The local fallback is same-browser only and does not grant server assistance rights.
- Patch map entry: `PM-2026-186`.
- Tests / checks: `evenchessOverlay.test.ts` covers server display-state hydration and same-browser local display fallback.
- Upstream update notes: Preserve current-player display serialization in live overlay refreshes and the Set-Level-capped fallback if round JSON or display controls are refactored.
- Rollback notes: Reverting can make desktop/mobile clients disagree on threat-arrow visibility after refresh because one side may reinitialize toggles to defaults.

### INT-2026-192 - EvenChess live clock layout preservation

- Phase: V2 live round layout polish.
- Lichess seam: Native round grid layout for board, table, players, clocks, and EvenChess coach/level panels.
- Lichess files touched: `ui/round/css/_app-layout.scss`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: EvenChess adds coach and level panels into the native round grid. The grid must still reserve non-overlapping rows for Lichess clocks so the opponent clock text is not hidden behind the native table, and mobile player/timer rows must not be pushed below coaching.
- Public UX effect: Desktop timed games keep native opponent clock text visible instead of showing only the bar. Mobile games place the native timer/player stack before the EvenChess coach card.
- Preserved Lichess capability: Native clocks, clock bars, player rows, table/move controls, board, and EvenChess coach card remain in the standard round shell.
- Patch map entry: `PM-2026-187`.
- Tests / checks: Round SCSS was rebuilt with `ui/build --debug --no-install round`; browser geometry inspection confirmed the desktop table no longer starts on the same row as the top clock.
- Upstream update notes: Preserve the EvenChess-specific table row offset and mobile coach ordering if upstream round grid areas change.
- Rollback notes: Reverting can hide opponent clock digits behind the table on desktop and move the active mobile timer below the coaching card.

### INT-2026-193 - Draw-mode arrows feed Proposed Move validation

- Phase: V2 live proposed-move input polish.
- Lichess seam: Native round EvenChess coach-card Proposed Move action and Draw-mode board input.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Draw mode intentionally disables piece movement while touch/pointer input creates green board arrows. Proposed Move selection still needs to consume that same single green arrow, but legal-move validation cannot rely only on Chessground's cleared movable map.
- Public UX effect: A mobile or desktop user can enable Draw, draw one legal green arrow, and press `Assess my proposed move`; the same proposed-move bridge is used as before, with the arrow checked against server-provided current `possibleMoves`.
- Preserved Lichess capability: Native piece movement remains disabled only while Draw mode is active, normal right-click/drawable arrows still work, invalid/multiple arrows are still rejected, and ECE remains server-to-server through ECL.
- Patch map entry: `PM-2026-188`.
- Tests / checks: `evenchessOverlay.test.ts` covers Draw-mode arrows with empty Chessground movable destinations.
- Upstream update notes: Preserve the `possibleMoves` fallback if round movement/drawable state is refactored.
- Rollback notes: Reverting can make Draw-mode proposed-move arrows appear on the board but fail Proposed Move as illegal.

### INT-2026-194 - Proposed Move after-move ECS markers

- Phase: V2 live proposed-move display.
- Lichess seam: Same-origin EvenChess Proposed Move action controller and ECE response normalization.
- Lichess files touched: `app/controllers/EvenChess.scala`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Legal Proposed ECS responses now provide hypothetical after-move board data through `proposed_move_evaluation.after_move_initial_ecs`, with optional `after_move_advanced_ecs` addenda, not only the older `after_move_standard_ecs` alias. ECL must normalize those shapes before it caches the proposed preview for the round UI.
- Public UX effect: Pressing `Assess my proposed move` can display the post-move overlay markers and coach cards for the hypothetical position, then toggle back to the cached current-FEN payload without consuming another call.
- Preserved Lichess capability: ECE remains server-to-server, browser code still receives only approved display payloads, and invalid/illegal proposed moves do not replace the current overlay state.
- Patch map entry: `PM-2026-189`.
- Tests / checks: Existing UI coverage verifies that normalized proposed cards with post-move visuals render on the board; controller compile checks cover the expanded parser.
- Upstream update notes: Preserve `after_move_initial_ecs`/`after_move_advanced_ecs` parsing if Proposed ECS response aliases are refactored.
- Rollback notes: Reverting can make proposed-move text appear while board markers disappear because ECL falls back to incomplete legacy side-output parsing.

### INT-2026-195 - Combined coach card, level controls, and WikiBook placement

- Phase: V2 live round UI layout polish.
- Lichess seam: Native round overlay renderer plus round grid placement for coach, eval, board, clocks, controls, and WikiBook.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_app-layout.scss`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The separate levels card consumed a full side column and split related controls away from the coach text. The round layout must now present one combined coach card with Set/Used Level, quick level selection, and collapsible scrollable feature toggles, while keeping WikiBook visible as its own top-right panel.
- Public UX effect: Desktop players see the combined coach card left of the board and WikiBook at the top right. Mobile players see the board and native player/timer rows first, then the coach card, then WikiBook. The coach card exposes quick `Apply up to` level selection and a `Level toggles` disclosure for detailed feature toggles.
- Preserved Lichess capability: Native board input, clocks, player rows, move list/table controls, EvenChess eval bar, TTS controls, Draw mode, Ask AI, Potential Moves, Proposed Move, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-190`.
- Tests / checks: `evenchessOverlay.test.ts` verifies desktop/mobile grid area mapping, coach-card-contained level controls, no separate level card, and WikiBook render order. Full `ui/build --debug --no-install --no-color` regenerated `public/compiled/manifest.json`; direct HTTP checks confirmed both `http://127.0.0.1:8080/` and `http://192.168.5.3:8080/` now load `manifest.cf8931ef.js`, whose served contents point round to `round.WPJGB7CP.js` and `round.36f00acf.css`.
- Upstream update notes: Preserve `coach ece-eval board wiki` desktop mapping, the mobile `user-bot` before `coach` order, and the coach-card `details.evenchess-live__level-toggles` behavior if upstream round layout or overlay rendering changes.
- Rollback notes: Reverting restores the separate level card, moves the coach card back to the right side on desktop, and can put level controls away from the coach text again.

### INT-2026-196 - Test Ground asset readiness follows combined coach controls

- Phase: V2 local Test Ground launch readiness.
- Lichess seam: Local-only Test Ground PowerShell launcher and browser control panel status API.
- Lichess files touched: None.
- EvenChess files touched: `scripts/evenchess-testground.ps1`; `scripts/evenchess-testground-panel.js`; `docs/requirements/planv1.6_phase_s_ci_cd_build_release_automation.md`; `docs/evenchess/EVENCHESS_NEW_CHAT_HANDOVER_CURRENT.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The live round UI intentionally removed the standalone `EvenChess Levels` card when level controls moved inside the coach card. The local Test Ground readiness check was still using that removed text as the required asset marker, so correctly rebuilt assets were reported as stale and `launch-evenchess` exited before starting ECL.
- Public UX effect: None in production. Local Test Ground launch and panel status now accept manifest-selected round assets that contain `evenchess-live__coach-levels` and `evenchess-live__level-toggles`, while still reporting whether a legacy level card marker is present for diagnostics.
- Preserved Lichess capability: Native local stack startup, explicit Build UI Assets flow, Docker/WSL controls, and ECE server-to-server boundaries remain unchanged.
- Patch map entry: None; this is local Test Ground tooling, not an upstream/core Lichess seam.
- Tests / checks: PowerShell parser check, Node syntax check, Test Ground `status` action, and scoped `git diff --check`.
- Upstream update notes: If round level controls move again, keep the readiness marker tied to stable current UI class names instead of removed display text.
- Rollback notes: Reverting can make Test Ground reject current combined coach-card assets and ask for a UI build even when the manifest-selected assets are current.

### INT-2026-197 - Right-side native table centers beside the board

- Phase: V2 live round desktop layout polish.
- Lichess seam: Native round grid placement for the right-side game table, board, and EvenChess WikiBook panel.
- Lichess files touched: `ui/round/css/_app-layout.scss`; `ui/round/css/_layout.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: EvenChess moved WikiBook into the top-right column but left the native table locked to lower grid rows. With the WikiBook collapsed or empty, the move/status table could appear far below the board rather than centered like standard Lichess. The table now spans the board-height row group and uses `align-self: center`; only a non-empty, open WikiBook starts the table below the opening panel so the two right-side panels do not overlap.
- Public UX effect: Desktop players see the native right-side move/status table vertically centered beside the board when WikiBook is collapsed or empty, matching the standard Lichess visual balance more closely. Opening a populated WikiBook can still push the table lower.
- Preserved Lichess capability: Native move list, player rows, controls, board, clocks, WikiBook toggle, and EvenChess coach/eval placement remain unchanged.
- Patch map entry: `PM-2026-191`.
- Tests / checks: `evenchessOverlay.test.ts` covers the centered table grid rule and non-empty open-WikiBook fallback. Full `ui/build --debug --no-install --no-color` regenerated `public/compiled/manifest.json`; direct HTTP checks confirmed both `http://127.0.0.1:8080/` and `http://192.168.5.3:8080/` now load `manifest.3bed384b.js`, whose served contents point round to `round.I7LQQFYQ.js` and `round.e31bb517.css`.
- Upstream update notes: Preserve the `grid-row: 1 / 15` centered table rule and the `:has(.evenchess-live__opening-wiki:not(.empty, .toggle-box--toggle-off))` fallback if the round grid is refactored.
- Rollback notes: Reverting can place the native game table too low whenever the EvenChess WikiBook panel is collapsed.

### INT-2026-198 - Compact coach action button labels

- Phase: V2 live coach-card copy and sizing polish.
- Lichess seam: Native round EvenChess coach-card renderer and coach-card action button styling.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The live coach card action buttons were too visually large and the potential-move labels repeated the command word. The shared action button width, height, padding, radius, and font size are now smaller and consistent, and the potential-move buttons read `Opponents potential moves` and `My potential moves`.
- Public UX effect: Coach-card action buttons are more compact, equal-sized, and easier to scan.
- Preserved Lichess capability: Ask AI, Potential Moves, Proposed Move, TTS, Draw mode, quotas, turn gating, and ECE server-to-server request paths remain unchanged.
- Patch map entry: `PM-2026-192`.
- Tests / checks: `evenchessOverlay.test.ts` covers the new labels and compact shared button sizing. Full `ui/build --debug --no-install --no-color` regenerated `public/compiled/manifest.json`; direct HTTP checks confirmed both `http://127.0.0.1:8080/` and `http://192.168.5.3:8080/` now load `manifest.3bed384b.js`, whose served contents point round to `round.I7LQQFYQ.js` and `round.e31bb517.css`.
- Upstream update notes: Keep all coach-card action buttons on the shared `evenchess-live__proposed-button` sizing token unless a future design system replaces it.
- Rollback notes: Reverting restores the longer `Show ...` labels and larger action buttons.

### INT-2026-199 - Compact homepage summary card sizing

- Phase: V2 lobby homepage polish.
- Lichess seam: Native lobby homepage stylesheet for the EvenChess summary callout.
- Lichess files touched: `ui/lobby/css/_lobby.scss`.
- EvenChess files touched: `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The lobby homepage owns the placement and styling of the `What is EvenChess?` card beside the active-games/lobby area. The card was too large for that supporting-summary role, so the EvenChess-specific summary and fact-card styling now uses a capped width, reduced padding, thinner top border, smaller inherited font size, tighter fact boxes, and shorter line spacing.
- Public UX effect: The `What is EvenChess?` card is now a compact left-side summary panel instead of a large landing-card-sized block.
- Preserved Lichess capability: Homepage copy, quick pairing/lobby tabs, active-games display, lobby start buttons, navigation, and public route behavior remain unchanged.
- Patch map entry: `PM-2026-193`.
- Tests / checks: Full `ui/build --debug --no-install --no-color` regenerated `public/compiled/manifest.json`; browser verification on `http://localhost:8080/` confirmed the live page serves `lobby.fcb064e9.css`, with the card about 231px wide in the checked viewport and about 10px body text.
- Upstream update notes: Preserve the compact EvenChess summary sizing if upstream lobby layout or homepage CSS is refactored.
- Rollback notes: Reverting restores the larger card sizing and can make the summary dominate the lobby homepage again.

### INT-2026-200 - Ask AI loaded-position hydration and Position ECS context retention

- Phase: V2 Position ECS / Ask AI reliability.
- Lichess seam: Native EvenChess controller same-origin ECE bridge and native round Ask AI action.
- Lichess files touched: `app/controllers/EvenChess.scala`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Ask AI must use ECL's same-origin Position ECS bridge and needs a current accepted Standard ECS live payload for the active game/FEN/ply. Loaded in-progress games could expose the Ask AI button before the initial current-position overlay was accepted, so the UI stopped at `Awaiting payload`. ECL now hydrates the current Standard ECS overlay and retries the Ask AI call once, preserves Position ECS context when assistance state is normalized, and accepts Position ECS ids from top-level Standard ECS, nested `standard.position_ecs`, nested `standard_ecs.position_ecs`, or matching `request_echo.position_ecs_id` fields.
- Public UX effect: Pressing `Ask AI` on a loaded in-progress game can fetch the current ECE payload first instead of requiring the user to make another move before Position ECS works.
- Preserved Lichess capability: Browser code still calls only ECL, ECE remains server-to-server, Ask AI remains own-turn gated, level gated, current-FEN gated, and failed/non-displayable Position ECS responses still do not consume an accepted-use token.
- Patch map entry: `PM-2026-194`.
- Tests / checks: `node ui/test round/tests/evenchessOverlay.test.ts`; `node ui/test round/tests/evenchessTestGround.test.ts`; `ui/build --debug --no-install --no-color`; Docker-backed `./lila.sh compile`; direct ECE `/health` and `/v1/ece/standard` shape check; same-origin `POST /evenchess/testground/ece/position-ecs` smoke returned `ok: true` with an approved `Ask AI` card.
- Upstream update notes: Preserve Standard ECS Position ECS context extraction and the loaded-position hydration retry if the round boot, socket reload, or ECE adapter path is refactored.
- Rollback notes: Reverting can make Ask AI show `Awaiting payload` on loaded in-progress games until the next move refreshes Standard ECS.

### INT-2026-201 - Mobile coach card non-overlap with native controls

- Phase: V2 live round mobile layout polish.
- Lichess seam: Native mobile round grid placement for player/timer rows, replay/action controls, horizontal move strip, and the EvenChess coach card.
- Lichess files touched: `ui/round/css/_app-layout.scss`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: EvenChess inserts a large combined coach card into the native mobile round surface. The prior mobile order let native action controls, horizontal moves, and game metadata visually cross the coach card on phones because the coach card began before those native strips had their own rows.
- Public UX effect: Mobile players now see board/player/timer rows, native controls, and the move strip before the EvenChess coach card starts. The coach header, level selector, action buttons, and status text wrap inside the card instead of crowding or overlaying each other.
- Preserved Lichess capability: Native board input, replay/actions, horizontal move list, player rows, clocks, game metadata, EvenChess coach controls, WikiBook, and ECE server-to-server privacy remain unchanged.
- Patch map entry: `PM-2026-195`.
- Tests / checks: `node ui/test round/tests/evenchessOverlay.test.ts`; `pnpm exec stylelint ui/round/css/_app-layout.scss ui/round/css/_evenchess-live.scss`; `git diff --check -- ui/round/css/_app-layout.scss ui/round/css/_evenchess-live.scss ui/round/tests/evenchessOverlay.test.ts`; full `ui/build --debug --no-install --no-color`, which regenerated `manifest.11a4c48e.js`, `round.2UJCI3BR.js`, and `round.3c3c2b22.css`.
- Upstream update notes: Keep `.round__app.evenchess-live-layout` mobile ordering as `user-bot -> pocket-bot -> controls -> moves -> coach`, and preserve the mobile wrapping rules for `.evenchess-live__head`, `.evenchess-live__apply`, and `.evenchess-live__proposed-action`.
- Rollback notes: Reverting can make mobile action controls, move text, and game metadata hover over the EvenChess coach card again.

### INT-2026-202 - Desktop native right rail stays aligned beside the board

- Phase: V2 live round desktop layout polish.
- Lichess seam: Native desktop round grid placement for WikiBook, captured-material strips, player rows, move table, controls, clocks, and EvenChess coach/eval side rails.
- Lichess files touched: `ui/round/css/_app-layout.scss`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: EvenChess moved WikiBook to the top-right desktop column while the native material/player/move table continued to use round-grid rows. The previous layout could leave captured pieces and the move table far below the board, especially when WikiBook occupied or reserved too much vertical space. The desktop EvenChess grid now caps WikiBook height, removes the old open-WikiBook push-down rule, zeroes empty right-side rows, and centers the native table across the board-height row group.
- Public UX effect: Desktop players see the captured-material strip and move/status table beside the board like standard Lichess. With WikiBook open, the top opening panel stays above the right rail without forcing the native table down the page.
- Preserved Lichess capability: Native move table, controls, player rows, clocks, captured-material display, board, WikiBook toggle, EvenChess coach card, eval rail, overlays, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-196`.
- Tests / checks: `node ui/test round/tests/evenchessOverlay.test.ts`; `pnpm exec stylelint ui/round/css/_app-layout.scss ui/round/css/_layout.scss ui/round/css/_evenchess-live.scss`; `git diff --check -- ui/round/css/_app-layout.scss ui/round/css/_layout.scss ui/round/css/_evenchess-live.scss ui/round/tests/evenchessOverlay.test.ts`; full `ui/build --debug --no-install --no-color`; browser verification at `2048x1065` on `http://localhost:8080/wBNOlfFn`, serving `manifest.6485012e.js` and `round.c6a2f3a0.css`. Final measured board height was 500px; WikiBook occupied the top 106.39px; material top began at board+106.39px; the user/move/control group center was board center+56.07px instead of being pushed down below the board.
- Upstream update notes: Preserve the EvenChess desktop right-column order `wiki -> mat-top -> clock-top -> user-top -> moves -> controls -> user-bot -> clock-bot -> mat-bot`, the zero-height spacer rows, and the centered `.round__app__table` span if upstream round grid or WikiBook layout changes.
- Rollback notes: Reverting can make the right-side captured-material strip and move table sit too low when WikiBook is open or reserving vertical space.

### INT-2026-203 - Compact coach-card controls and enlarged text pane

- Phase: V2 live round coach-card sizing polish.
- Lichess seam: Native round EvenChess coach-card styling inside the board-side layout.
- Lichess files touched: `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The coach card shares the native round layout with the board, WikiBook, move table, and player controls. The level/header/action controls were using too much vertical space, making the summary and AI text pane too short. The coach-card internals now use scoped compact typography, smaller pills/buttons/selects, tighter gaps, and a larger `.evenchess-live__coach-text` flex area.
- Public UX effect: Desktop coach-card content is about 25% smaller, and the text area is more than twice as tall in the verified viewport, making long summaries/potential-move text easier to read before scrolling.
- Preserved Lichess capability: Native board input, move table, player rows, clocks, WikiBook, EvenChess level selection, toggles, TTS, draw mode, Ask AI, potential moves, proposed move assessment, and ECE privacy boundaries remain unchanged.
- Patch map entry: `PM-2026-197`.
- Tests / checks: `pnpm exec stylelint ui/round/css/_evenchess-live.scss`; `git diff --check -- ui/round/css/_evenchess-live.scss ui/round/tests/evenchessOverlay.test.ts`; direct CSS smoke assertions for compact coach-card sizing; full `ui/build --debug --no-install --no-color`; browser verification at `2048x1065` on `http://localhost:8080/wBNOlfFn`, serving `manifest.8402ee45.js` and `round.7fac552b.css`. The focused `node ui/test round/tests/evenchessOverlay.test.ts` harness timed out before reporting results in this run, so the change-specific assertions were also executed directly.
- Upstream update notes: Preserve the compact scoped sizing under `.evenchess-live__card--coach` and the enlarged `.evenchess-live__coach-text` basis/min-height if coach-card layout is refactored.
- Rollback notes: Reverting restores the larger controls and reduces the visible coach text pane height.

### INT-2026-204 - Merged Potential Moves action with opponent-move refund guard

- Phase: V2 live round Potential ECS action polish.
- Lichess seam: Native round EvenChess coach-card renderer, round move lifecycle clear hook, and same-origin EvenChess Potential ECS controller bridge.
- Lichess files touched: `app/controllers/EvenChess.scala`; `conf/routes`; `ui/round/src/ctrl.ts`; `ui/round/src/evenchessTestGround.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `ui/round/tests/evenchessTestGround.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Potential ECS remains server-to-server through ECL, but the live coach UI needs one simpler player-facing action. The merged `Potential Moves` button chooses player-side reveal on the student's turn and opponent-side reveal on the opponent's turn, while a same-origin refund route lets ECL undo an opponent-potential cache/usage entry when the opponent moves during the configured grace window.
- Public UX effect: The coach card now shows one `Potential Moves` button with status like `White 2/3 - Black 1/1`, always listing the student's colour first. After the opponent moves, the button is briefly disabled for one second so a late tap/click cannot immediately become a student-turn request. If an opponent-potential reveal was requested just before the opponent moved, ECL attempts to restore that opponent reveal use within three seconds.
- Preserved Lichess capability: Native board input, move lifecycle, Ask AI, Proposed Move, TTS, Draw mode, level gates, turn checks, same-origin ECL action routes, Potential ECS server-to-server privacy, cached reveal replay, and admin-unlimited quota display remain unchanged.
- Patch map entry: `PM-2026-198`.
- Tests / checks: `node ui/test round/tests/evenchessTestGround.test.ts`; `ui/build --debug --no-install --no-color`; Docker-backed `./lila.sh compile`; `pnpm exec stylelint ui/round/css/_evenchess-live.scss`; scoped `git diff --check`; browser DOM verification on local ECL serving `manifest.0823620d.js` and `round.7fac552b.css`, showing one `Potential Moves` action with combined colour quota text. The full `node ui/test round/tests/evenchessOverlay.test.ts` harness timed out before reporting in this run, so the new assertions are present but not fully harness-verified yet.
- Upstream update notes: Preserve the merged action's turn-derived `kind`, student-colour-first quota text, post-opponent-move cooldown, and refund route if the round action area or Potential ECS bridge is refactored.
- Rollback notes: Reverting restores separate opponent/my potential buttons and removes the refund guard, allowing opponent-reveal uses to remain consumed when the opponent moves immediately after the request.

### INT-2026-205 - Coach card top action strip and shared action status

- Phase: V2 live round coach-card layout polish.
- Lichess seam: Native round EvenChess coach-card renderer and coach-card SCSS.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The coach card's primary actions should sit near the level and speech controls instead of occupying a separate bottom block. Action feedback such as `Awaiting payload` should not create multiple separate status rows.
- Public UX effect: The coach card top now reads as level badges, then `Speak`, `Auto`, `Draw`, `Ask AI`, then `Potential Moves` with colour quotas and `Proposed move check` with quota text. The visible `Audit ece-...` sentence is removed, while non-visible audit metadata remains available in attributes.
- Preserved Lichess capability: Native board input, TTS, Draw mode, Ask AI, Potential ECS, Proposed Move, same-origin ECL action routes, quota enforcement, action caching, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-199`.
- Tests / checks: `evenchessOverlay.test.ts` was updated for the new control order, proposed label, absence of a visible audit element, and compact top action sizing.
- Upstream update notes: Preserve the top action order, adjacent Potential/Proposed quota text, shared action status area, and absence of visible raw audit ids if the coach card renderer is refactored.
- Rollback notes: Reverting restores the bottom action block, visible raw audit-id text, old proposed action label, and separated per-button status placement.

### INT-2026-206 - Coach card bottom aligns with the board

- Phase: V2 live round coach-card layout polish.
- Lichess seam: Native round EvenChess coach-card SCSS, desktop grid layout, and board-height measurement variables.
- Lichess files touched: `ui/round/src/view/main.ts`; `ui/round/css/_app-layout.scss`; `ui/round/css/_layout.scss`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The coach card sits below the game-info card on the left, while the native move table can make the lower desktop grid rows continue past the board. The EvenChess desktop grid now stops the coach/board/eval areas before those move-table tail rows, the round view publishes `--evenchess-coach-height` from the coach row's actual top to the board bottom when it redraws, and the coach text pane scrolls internally for longer content.
- Public UX effect: On desktop, the coach card bottom lines up with the board bottom instead of dropping below it, while long Summary/Ask AI/Potential/Proposed text remains available through the card's scrollable text area.
- Preserved Lichess capability: Native board layout, game-info card, coach controls, level toggles, TTS, Draw mode, Ask AI, Potential Moves, Proposed move check, and mobile auto-height behavior remain unchanged.
- Patch map entry: `PM-2026-200`.
- Tests / checks: `evenchessOverlay.test.ts` was updated to assert the measured coach-height source, grid-height fallback, and shrinkable scroll text pane.
- Upstream update notes: Preserve the EvenChess desktop grid-area stop row, `--evenchess-coach-height` for the desktop coach-card height override, and `overflow-y: auto` on `.evenchess-live__coach-text` if round layout measurement is refactored.
- Rollback notes: Reverting can make the coach card extend below the board again when the game-info card sits above it.

### INT-2026-207 - Homepage summary column centering and mobile full width

- Phase: V2 lobby homepage polish.
- Lichess seam: Native lobby homepage stylesheet for the EvenChess summary callout.
- Lichess files touched: `ui/lobby/css/_lobby.scss`.
- EvenChess files touched: `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The compact `What is EvenChess?` card sits in the native lobby grid's supporting left column. It was the right size but left-aligned inside that column on wide screens, and its mobile/default width stayed capped instead of filling the portrait lobby row.
- Public UX effect: On wide screens, the summary card remains compact but is horizontally centered in the left lobby column and sits slightly below the top edge of the primary lobby row. On mobile portrait, it stays at the top and spans the full available content width.
- Preserved Lichess capability: Homepage copy, quick pairing/lobby tabs, active-games display, lobby start buttons, navigation, and public route behavior remain unchanged.
- Patch map entry: `PM-2026-201`.
- Tests / checks: Browser verification on `http://localhost:8080/` checked desktop column centering after the lobby CSS build. Generated lobby CSS inspection confirmed the mobile/default card uses full available width before the wider-screen compact centering rule applies.
- Upstream update notes: Preserve the mobile/default full-width summary card and desktop column-centered compact sizing if upstream lobby grid or homepage CSS is refactored.
- Rollback notes: Reverting can leave the summary card left-aligned in the wide lobby column and capped/narrow on mobile portrait.

### INT-2026-208 - Coach card dropdown/action row refinement

- Phase: V2 live round coach-card layout polish.
- Lichess seam: Native round EvenChess coach-card renderer and coach-card SCSS.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The coach card's controls are rendered inside the native round overlay. The requested layout keeps `Speak`, `Auto`, and `Draw` as the equal-sized tool row, moves `Ask AI` into the equal-width assistance row with `Potential Moves` and `Proposed move check`, places each action's allowance text under its button, and keeps one shared status line below the row.
- Public UX effect: Players see a cleaner coach card: Set/Used levels read as indicators, the three assistance actions share one row, the level selector and Level toggles disclosure share a row, and the open Level toggles panel covers the coach text pane instead of pushing Summary text out of the card.
- Preserved Lichess capability: Native board input, clocks, move table, WikiBook, TTS, Draw mode, Ask AI, Potential Moves, Proposed move check, level gates, same-origin ECL action routes, and server-to-server ECE privacy remain unchanged.
- Patch map entry: `PM-2026-202`.
- Tests / checks: `evenchessOverlay.test.ts` verifies row order, `Apply up to: ...` select labels, absence of the `Features` hint, three-column assistance sizing, absolute Level toggles overlay behavior, and compact button sizing.
- Upstream update notes: Preserve the tool/action row split, non-button Set/Used indicators, equal level-control row, overlay-style Level toggles disclosure, and scrollable coach text pane if the round overlay or coach-card SCSS is refactored.
- Rollback notes: Reverting can put `Ask AI` back in the tool row, restore stacked level controls, and make the open Level toggles panel push Summary/text below the card.

### INT-2026-209 - Merged Potential Moves current-turn click guard

- Phase: V2 live round Potential Moves merged-button polish.
- Lichess seam: Native round EvenChess coach-card action renderer and same-origin Potential ECS bridge trigger.
- Lichess files touched: `ui/round/src/view/evenchessOverlay.ts`.
- EvenChess files touched: `ui/round/tests/evenchessOverlay.test.ts`; `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: The merged `Potential Moves` button replaced separate player/opponent buttons, so the visible click handler must choose the reveal side from the current turn at click time. A stale rendered kind can otherwise generate old side-specific availability messages even though the merged UX should simply request the side to move.
- Public UX effect: Pressing `Potential Moves` on the student's turn requests student potentials; pressing it on the opponent's turn requests opponent potentials. Old shared-status messages like `Potential Moves: Available on opponent's turn` and `Potential Moves: Available on your turn` are no longer surfaced by the merged button path.
- Preserved Lichess capability: Explicit low-level side-mismatched requests remain rejected before ECE transport; server-to-server ECE privacy, level gates, quotas, caching, cooldown, and refund behavior remain unchanged.
- Patch map entry: `PM-2026-203`.
- Tests / checks: `evenchessOverlay.test.ts` adds a stale-render click regression for the merged button and keeps the explicit side-mismatch guard test.
- Upstream update notes: Preserve current-turn derivation in the visible merged-button click path if the coach-card action renderer is refactored.
- Rollback notes: Reverting can reintroduce old per-side availability messages when the turn changes between render and click.

### INT-2026-210 - Native chat ordered below the EvenChess coach card

- Phase: V2 live round coach-card layout polish.
- Lichess seam: Native round parent grid, native clocks/material rows, and EvenChess live-layout desktop override.
- Lichess files touched: `ui/round/css/_layout.scss`.
- EvenChess files touched: `ui/round/tests/evenchessLayout.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Online game chat renders as `.mchat` inside `round__side`, separate from the EvenChess coach card, while some empty/non-chat cases still expose `round__underchat`. The EvenChess desktop parent-grid override must split the side children, keep the game-info card in the single top side row, start coach below it with a small gap while preserving board-bottom alignment, explicitly place the real chat in the `uchat` area below the coach/moves row, and keep the native right-column clocks/material rows visually close to the move-card stack.
- Public UX effect: On wide desktop live rounds, the left column order is game info, EvenChess Coach, then chat when chat is available, with a gap above coach and before under-board content. The right column keeps compact clock text and content-sized material rows closer to the native lichess presentation.
- Preserved Lichess capability: Native chat, notes, game info, board, move table, WikiBook, coach card, board-bottom coach alignment, and mobile round order remain unchanged.
- Patch map entry: `PM-2026-204`.
- Tests / checks: `node ui/test round/tests/evenchessLayout.test.ts`; `pnpm exec stylelint ui/round/css/_layout.scss`; full `ui/build --debug --no-install --no-color` to refresh `public/compiled/manifest.json`; scoped `git diff --check`; browser DOM verification on `http://localhost:8080/5H4cNrwwCswo` after reload confirmed the page loaded `round.5df8aae0.css` through `manifest.069678d0.js`, with game info ending at `194px`, the coach card running from `284px` to the board bottom at `565px`, and the visible `.mchat` panel running below it from `580px` to `744px`.
- Upstream update notes: Preserve the `game__meta -> coach -> .mchat/uchat -> under` order, coach top gap with board-bottom alignment, compact clock override, and content-sized material rows in `.round:has(> .round__app.evenchess-live-layout)` if upstream round grid rows or chat placement are refactored.
- Rollback notes: Reverting can let the native chat appear above the EvenChess coach card again on wide desktop layouts.

### INT-2026-211 - EvenChess search games preserve native first-move no-start expiration

- Phase: V2 public search and native game lifecycle hardening.
- Lichess seam: Native challenge accept and challenge game-creation source assignment.
- Lichess files touched: `modules/challenge/src/main/ChallengeApi.scala`; `modules/challenge/src/main/ChallengeJoiner.scala`.
- EvenChess files touched: `app/controllers/EvenChess.scala`; `modules/challenge/src/test/JoinerTest.scala`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: EvenChess search-created human/human and roster-backed bot matches use the native challenge accept path, but that path normally creates `Source.Friend` games. Native Lichess first-move no-start expiration only applies to public matchmaking-style sources. EvenChess search now passes a `Source.Lobby` override at challenge acceptance, while direct friend challenges keep the default `Source.Friend` behavior.
- Public UX effect: Matched EvenChess search games again use Lichess's native first-move expiration path: before both sides have moved, the round JSON can expose the normal expiration countdown and the round scheduler can abort/no-start the game according to native Lichess rules.
- Preserved Lichess capability: Direct friend challenges remain non-expirable by first-move no-start rules, normal challenge acceptance behavior remains the default, clocks and game lifecycle are still Lichess-owned, and EvenChess does not add a custom timer.
- Patch map entry: `PM-2026-205`.
- Tests / checks: `./lila.sh 'challenge/testOnly lila.challenge.JoinerTest'`; `./lila.sh compile`.
- Upstream update notes: Preserve the default friend source and the EvenChess search-only `Source.Lobby` override if challenge joining or EvenChess game handoff is refactored.
- Rollback notes: Reverting can make EvenChess search-created games look like friend challenges to native Lichess, disabling first-move no-start expiration until the round clock starts after both sides move.

### INT-2026-212 - Native first-move countdown banner visible in EvenChess desktop layout

- Phase: V2 public search and native game lifecycle hardening.
- Lichess seam: Native round parent grid areas for first-move expiration.
- Lichess files touched: `ui/round/css/_layout.scss`.
- EvenChess files touched: `ui/round/tests/evenchessLayout.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: EvenChess search games now opt into native Lichess no-start expiration, but the wide desktop EvenChess grid had `0`-height tracks for `expi-top` and `expi-bot`. That hid the natural Lichess first-move countdown banner even though the server abort path worked.
- Public UX effect: On wide desktop live rounds, the native green first-move countdown can appear in its normal move-table stack location when a first move is pending. With no expiration banner, those rows collapse naturally.
- Preserved Lichess capability: Lichess still owns the first-move timer, countdown vnode, no-start abort, clocks, and game lifecycle. EvenChess only keeps the native expiration grid areas visible.
- Patch map entry: `PM-2026-206`.
- Tests / checks: `node ui/test round/tests/evenchessLayout.test.ts`; `pnpm exec stylelint ui/round/css/_layout.scss`; `ui/build --debug --no-install --no-color`; browser reload of `http://localhost:8080/5H4cNrwwCswo` confirmed served `round.3a9d90eb.css` and computed wide-grid rows with non-zero native top-expiration track.
- Upstream update notes: Preserve auto-sized `expi-top` and `expi-bot` tracks in the EvenChess wide desktop grid if coach/chat/move-table layout is refactored.
- Rollback notes: Reverting can hide the native first-move countdown banner on desktop while server-side no-start expiration still aborts the game.

### INT-2026-213 - EvenChess quick-pairing tile shows the native search spinner

- Phase: V2 public search UX parity.
- Lichess seam: Native lobby quick-pairing pool tile renderer.
- Lichess files touched: `ui/lobby/src/view/pools.ts`.
- EvenChess files touched: `ui/lobby/tests/evenchessPools.test.ts`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Native Lichess quick pairing shows a spinner in the active tile while searching. EvenChess search reuses the active pool tile state, but one path carried rating-range data and therefore rendered the native range sweep instead of the spinner.
- Public UX effect: When an EvenChess quick-pairing search is waiting, the selected quick-pairing tile shows the normal Lichess spinner animation. Ordinary native pool searches still keep their native range display when applicable.
- Preserved Lichess capability: Native quick-pairing tile layout, custom tile, native spinner vnode, pool range display for non-EvenChess searches, and EvenChess server-side search/polling are unchanged.
- Patch map entry: `PM-2026-207`.
- Tests / checks: `node ui/test lobby/tests/evenchessPools.test.ts`.
- Upstream update notes: Preserve the `evenChessPoolMember` distinction in the pool renderer if native lobby pooling is refactored.
- Rollback notes: Reverting can make EvenChess waiting searches show rating-range text instead of the normal Lichess spinner in the active quick-pairing tile.

### INT-2026-214 - Production post-game ECEMF Coach Review in native analysis

- Phase: V2 analysis memory and post-game review mode foundation.
- Lichess seam: Native route table, EvenChess controller, native analysis controller overlay options, native analysis side-panel renderer, replay side-stack layout, and round replay EvenChess overlay column.
- Lichess files touched: `conf/routes`; `app/controllers/EvenChess.scala`; `ui/analyse/src/ctrl.ts`; `ui/analyse/src/view/main.ts`; `ui/analyse/src/view/evenchessReview.ts`; `ui/analyse/css/_layout.scss`; `ui/analyse/css/_side.scss`; `ui/analyse/css/_evenchess-ai.scss`; `ui/round/src/evenchessReview.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`.
- EvenChess files touched: `ui/analyse/tests/evenchessReview.test.ts`; `ui/round/tests/evenchessReview.test.ts`; `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`; `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Why this seam exists: Users must open completed games in the normal Lichess analysis/replay surface, step through moves with the native move selector, and see the saved ECEMF frame for the selected ply and side-to-move. ECL therefore needs production review routes, durable sanitized storage, and a native analysis card while ECE stays server-to-server only.
- Public UX effect: Completed game analysis/replay pages can show an EvenChess Coach Review card with `Generate ECEMF`, `Match Summary`, and `Ask AI` actions. Stored ECEMF coach text is labeled as player/opponent text by side-to-move, non-live Ask AI is saved back into the review store, and board overlays can render stored ECEMF arrows/highlights during replay. The card remains visible on real game replay pages whose `aside.analyse__side` is replaced by server-side HTML.
- Preserved Lichess capability: Native analysis board, move selector, move table, study/analysis separation, ordinary completed games without ECEMF, live EvenChess coaching, ECR/result/clocks/matchmaking, and ECE server-to-server privacy remain unchanged.
- Patch map entry: `PM-2026-208`.
- Tests / checks: `./lila.sh playRoutes`; `./lila.sh compile`; `pnpm --filter analyse exec tsc --noEmit --pretty false`; `pnpm --filter round exec tsc --noEmit --pretty false`; `pnpm exec stylelint ui/analyse/css/_layout.scss ui/analyse/css/_side.scss ui/analyse/css/_evenchess-ai.scss ui/round/css/_evenchess-live.scss`; `pnpm test:ui-tsx ui/round/tests/evenchessReview.test.ts ui/analyse/tests/evenchessReview.test.ts`; `./ui/build -dn --no-color`; browser verified on `http://localhost:8080/5H4cNrww/black` after restarting `lila`: ECEMF generated 6 frames, Match Summary returned, Ask AI returned and persisted after reload.
- Upstream update notes: Preserve the review route names, the `EVENCHESS_REVIEW_STORE_DIR` persistence override, the analysis side-stack mount point, real-game-id/side-to-move overlay requests, and configured/default/docker-host ECE base URL fallback if analysis, route, or ECE gateway infrastructure is refactored.
- Rollback notes: Reverting removes the production post-game ECEMF review card and routes, leaving only the older test-ground full-match review scaffolding.
