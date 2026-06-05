# EvenChess-Lichess Version 1.3 Completion Report

**Date:** 2026-05-29
**Status:** Implemented locally; superseded where noted by the current native-Lichess public-play alignment

## Summary

Version 1.3 narrowed the Version 1.1/1.2 public-shell takeover into a Lichess-first integration. The useful EvenChess module work was kept, while broad replacement-style homepage and navigation changes were reduced.

The current active Version 1 direction refines Version 1.3: EvenChess remains the visible product, but it should look and behave like Lichess. Public `PLAY` and lobby start controls use the native Lichess setup modal shape, add EvenChess settings, and submit to EvenChess-owned search contracts rather than a separate search page or ordinary Lichess rated pools.

The result is a native-Lichess integration strategy: Lichess owns the chess platform, board, clocks, game lifecycle, account foundations, study, analysis, openings, and shell structure; EvenChess owns assisted-mode policy, Set Level, ECR/search contracts, overlays, AI/TTS, tokens, admin controls, and audit requirements.

## Kept From Versions 1.1 And 1.2

- EvenChess module contracts and tests.
- `/evenchess/play`, `/evenchess/play/search`, and `/evenchess/account`.
- EvenChess user settings section.
- EvenChess admin/backend settings and read-only ops dashboard.
- Optional live-board, study/analysis, opening, and TTS overlay adapters.
- Server-side OpenAI Responses API provider wiring.
- Patch map and integration log governance.

## Narrowed Or Superseded

- `/` is no longer a standalone EvenChess marketing shell; it keeps the Lichess lobby structure.
- Top navigation keeps normal Lichess sections, but visible product branding says EvenChess and public `PLAY` opens the native EvenChess setup flow.
- The global wordmark now says EvenChess while preserving the Lichess shell structure.
- EvenChess play/account pages use simpler Lichess-native page structure with restrained blue accents instead of a standalone landing-site layout.
- A separate EvenChess search-page-first interpretation is superseded by the current native setup-modal flow.
- Version 1.1/1.2 public-shell takeover requirements are superseded/refined by Version 1.3 and current Version 1 alignment.

## Upstream Files Still Containing EvenChess Hooks

- `app/controllers/EvenChess.scala`, `conf/routes`, `app/views/lobby/home.scala`, `ui/lobby/src/view/table.ts`, `ui/lobby/src/view/setup/modal.ts`, and `ui/lobby/src/setupCtrl.ts`: public Play opens the native Lichess lobby/setup modal, the modal carries EvenChess controls, and submit starts the EvenChess JSON search contract with in-lobby status.
- `modules/web/src/main/ui/layout.scala`: shared shell wordmark says EvenChess.
- `modules/web/src/main/ui/TopNav.scala`: primary `PLAY` menu targets EvenChess search.
- `app/views/evenchess/play.scala` and `app/views/evenchess/account.scala`: namespaced EvenChess route pages.
- `modules/pref/src/main/*` and account preference UI files: add the EvenChess user settings category and safe preference JSON.
- `app/controllers/Dev.scala`, `modules/web/src/main/Env.scala`, `modules/web/src/main/ui/DevUi.scala`, and `modules/mod/src/main/ui/ModUi.scala`: expose EvenChess admin/backend settings and read-only operations surfaces.
- `ui/round/src/*`: accepts optional server-authorized EvenChess live overlay payloads and remains inert without them.
- `ui/analyse/src/*` and `ui/opening/src/*`: accepts optional server-authorized learning overlay payloads and clears stale payloads.
- `ui/lib/src/evenchessTts.ts`: browser TTS adapter for visible authorized overlay text only.

## Unavoidable Hooks

- `build.sbt`: required to register the isolated `modules/evenchess` module in the lila build.
- `conf/routes` and `app/controllers/EvenChess.scala`: required for namespaced EvenChess play/search/account/admin contracts.
- Lobby setup modal files under `ui/lobby`: required because public EvenChess search must happen inside the native Lichess setup flow.
- Round, analysis, and opening UI adapters: required because Lichess owns those surfaces and does not expose a formal plugin slot for EvenChess overlays.
- Preference/admin files: required because the approved UX puts EvenChess settings inside existing Lichess settings/admin surfaces.

These hooks should stay thin. Policy, validation, search semantics, AI/TTS safety, token rules, and fairness decisions should remain in `modules/evenchess` wherever feasible.

## Tests And Smoke Checks

- `docker compose exec -T lila sbt "evenchess/test"`: passed, 384 tests.
- `docker compose exec -T lila sbt compile`: passed.
- Focused UI tests in a Linux Node 24 Docker container passed:
  - `ui/lib/tests/evenchessTts.test.ts`.
  - `ui/round/tests/evenchessOverlay.test.ts`.
  - `ui/analyse/tests/evenchessLearning.test.ts`.
  - `ui/opening/tests/evenchessOpeningAi.test.ts`.
- HTTP route smoke:
  - `/`: 200.
  - `/evenchess/play`: 303 to `/#hook`.
  - `/evenchess/account`: 303 to `/signup` when unauthenticated.
  - `/account/preferences/evenchess`: 303 to `/login` when unauthenticated.
  - `/dev/evenchess/ops`: 303 to `/signup` when unauthenticated.
  - `/study`: 200.
  - `/analysis`: 200.
  - `/opening`: 200.
- Seeded normal game pages `/TaHSAsYD` and `/rVK1n3ZW`: 200.
- Core local services were up during Phase K: `lila`, `lila_ws`, `caddy`, `mongodb`, `redis`, `lila_fishnet`, and `fishnet_play`.

## Not Completed To Production Standard

- The EvenChess lobby search contract prepares and validates a search intent; it is not fully wired into production pairing/game creation.
- The native Lichess setup modal shows EvenChess settings and routes to the EvenChess contract, but real paired-game creation through lila pairing/game lifecycle is still incomplete.
- Server-side persistence of EvenChess game metadata and real overlay emission from live game state still need production integration.
- Token, subscription, rewarded-ad, checkout, and quota mutation remain provider-interface/UX work, not production billing.
- Overlay payload emission from real server game state is still incomplete; adapters are ready and inert without payloads.
- AI/TTS surfaces are wired through policy/provider seams, but real product workflows need final audit persistence, provider monitoring, and cost controls.
- Browser screenshot QA was not captured because the in-app Browser backend was unavailable in this session.

## Upstream Sync Notes

For future Lichess updates, use `docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md` and `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` as the reimplementation guide.

Recommended reapply order:

1. Accept upstream public shell/navigation changes first.
2. Reapply EvenChess wordmark and primary `PLAY` target.
3. Reapply native setup modal EvenChess fields and JSON submit adapter.
4. Reapply namespaced `/evenchess/*` routes and controller contracts.
5. Reapply settings/admin seams.
6. Reapply live-board, analysis/study, opening, and TTS overlay guards.
7. Run `evenchess/test`, root `compile`, focused UI overlay/TTS tests, and route smoke.

Do not reintroduce broad standalone marketing-shell behavior unless Appendix Z receives a new explicit override.

## Phase L Decision

Version 1.3 is complete as a cleanup and handover phase. It is not production-ready as a full public EvenChess launch because the real pairing/game creation path, persisted assisted-game metadata, production overlay emission, billing/provider fulfillment, and release-grade visual/browser QA remain unfinished.
