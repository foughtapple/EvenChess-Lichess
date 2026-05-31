# EvenChess-Lichess Integration Log

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live integration ledger  
**Created:** 2026-05-29

## Purpose

This document records EvenChess-to-Lichess integration seams required to preserve updateability on future Lichess upstream syncs.

For this phase, it records documentation-only direction alignment for the native Lichess setup-flow integration model.

## Governance

- Upstream/core-file edits remain bound to the patch map.
- This log records seam intent, rollback notes, and reapplication guidance for future upstream syncs.
- If the native-flow interpretation changes, supersede this entry and update Appendix Z first.

## Entry Registry

| Entry ID | Phase | Date | Seam / area | Lichess files touched | EvenChess files touched | Patch map | Tests / checks | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| INT-2026-030 | Native-Lichess-Integration-A | 2026-05-29 | Lichess-first documentation/override alignment | None | `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`; `docs/requirements/APPENDIX_T_CODEX_PHASE_PLAN_AND_TASK_PACKETS.md`; `docs/requirements/APPENDIX_Z_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md`; `docs/requirements/EVENCHESS_LICHESS_REQUIREMENTS_MAIN.md`; `docs/requirements/EVENCHESS_LICHESS_STAGE_1_LOCAL_HANDOVER.md`; `docs/requirements/EVENCHESS_UPSTREAM_SYNC_PROCESS.md`; `docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md`; `docs/requirements/EVENCHESS_LICHESS_REQUIREMENTS_DIFF.md` | None | `git diff --check` | Complete |
| INT-2026-032 | Native-Lichess-Integration-C | 2026-05-29 | Lichess navigation restoration and EvenChess play entrypoint to native setup flow | `modules/web/src/main/ui/TopNav.scala` | None | `PM-2026-029`, `PM-2026-015` | `./bin/sbt testOnly lila.evenchess.PublicShellTest` (if environment available); compile smoke | Complete |
| INT-2026-033 | Native-Lichess-Integration-C | 2026-05-29 | Keep `/evenchess/*` routes namespaced and explicit | `conf/routes`; `app/controllers/EvenChess.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala` | None | `PM-2026-010`, `PM-2026-016` | `./bin/sbt testOnly lila.evenchess.PublicShellTest` | Complete |
| INT-2026-034 | Native-Lichess-Integration-F | 2026-05-29 | EvenChess admin backend setting hardening and secret-safety enforcement | `app/controllers/Dev.scala` | `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala` | `PM-2026-017` | `./bin/sbt testOnly lila.evenchess.AdminBackendSettingsTest` | Complete |
| INT-2026-035 | Native-Lichess-Integration-G | 2026-05-29 | Live-board overlay payload hardening | `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts` | `docs/requirements/APPENDIX_T_CODEX_PHASE_PLAN_AND_TASK_PACKETS.md` | `PM-2026-018` | `./ui/test round/tests/evenchessOverlay.test.ts` | Complete |

## Detailed Entries

### INT-2026-030 - Native Lichess integration plan phase A direction reset

- Phase: Native-Lichess-Integration-A.
- Lichess seam: Requirements governance only.
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
- Preserved Lichess capability: Puzzle, watch, learn, community, and tools sections are still exposed for immediate reuse.
- Patch map entry: Referenced by `PM-2026-029` and `PM-2026-015`.
- Tests / checks: `./bin/sbt testOnly lila.evenchess.PublicShellTest` (environment not available in this shell today) and route/UI smoke around `/`, `/account`, `/learn`, `/analysis`.
- Upstream update notes: Keep this entry with `PM-2026-015/029` so future reapplication preserves nav-level compatibility instead of full-shell rewrites.
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
