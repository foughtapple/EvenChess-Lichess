# EvenChess-Lichess Patch Map

**Suite:** EvenChess-Lichess Version 1

## 1. Rule

Whenever Codex edits a file from upstream Lichess/lila, it must add/update a patch map entry before the phase is complete.

Patch-map entries are file-level records. Version 1.1 and later integration phases must also update `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`, which records seam-level intent, rollback notes, and reimplementation guidance for future Lichess updates.

## 2. Risk levels

| Risk | Meaning |
| --- | --- |
| Low | Small hook/delegator/config with low conflict risk. |
| Medium | Moderate integration with game/UI/rating flow. |
| High | Core game/rating/matchmaking/UI file likely to conflict. |
| Unknown | Not enough inspection; resolve before release. |

## 3. Registry template

| Entry ID | File touched | Why core file was touched | Linked requirement | Merge risk | Tests | Can later isolate? | Integration log | Notes/status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| PM-0001 | Example: app/... | Example hook for EvenChess mode flag. | MODE-L1-001 | Unknown | Example test. | TBD | INT-YYYY-### | Replace when real work starts. |
| PM-2026-001 | build.sbt | Register isolated `modules/evenchess` Appendix A foundation module in the lila build. | ARCH-L1-010, VOC-L1-006, PUB-L1-001..004, INV-L1-020..024 | Low | `evenchess/test` | Yes | INT-2026-000 | Build registration only; no game behavior changes. |
| PM-2026-002 | `app/http/KeyPages.scala`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala` | Adapt public homepage and top navigation to Version 1.1 EvenChess-only production shell. | REQ-MAIN-012, OVR-V11-001, PUB-L1-001..004, MODE-L1-001, MODE-L1-010, MKT-L1-001..005 | Medium | `evenchess/testOnly lila.evenchess.PublicShellTest`; compile check | Yes | INT-2026-002 | Public UX only; no chess rules, game lifecycle, ratings, matchmaking, or coaching authority changed. |
| PM-2026-003 | `app/views/lobby/home.scala` | Consume EvenChess deep-blue design tokens and polish homepage shell for Version 1.2. | REQ-MAIN-013, OVR-V12-001, UI-L1-001..006, UI-L1-030..033, MKT-L1-001..005 | Medium | `EvenChessThemeTest`; `PublicShellTest`; `evenchess/test`; compile check | Yes | INT-2026-014 | Public homepage styling/content only; no chess rules, game lifecycle, ratings, matchmaking, or coaching authority changed. |
| PM-2026-004 | `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala` | Restore safe Lichess feature and account navigation under EvenChess branding for Version 1.2. | REQ-MAIN-013, OVR-V12-001, DEC-V12-002, ARCH-L1-005, ARCH-L1-014, C-L1-005, MODE-L1-004, MKT-L1-001..005 | Medium | `PublicShellTest`; `evenchess/test`; compile check; route smoke | Yes | INT-2026-015 | Navigation only; rated public play entrypoints remain EvenChess/ECR-owned and do not route to normal Lichess rated pools. |
| PM-2026-005 | `build.sbt`; `modules/pref/src/main/PrefCateg.scala`; `modules/pref/src/main/PrefForm.scala`; `modules/pref/src/main/FormCompatLayer.scala`; `modules/pref/src/main/JsonView.scala`; `modules/pref/src/main/ui/AccountPref.scala`; `modules/pref/src/main/ui/AccountUi.scala`; `modules/web/src/main/ui/TopNav.scala` | Add a real EvenChess user preferences category using Lichess account settings, server-side `Pref.tags` storage, safe client JSON, and account-nav routing. | REQ-MAIN-013, OVR-V12-001, V1.2-D, APPENDIX_F, APPENDIX_H, APPENDIX_M, APPENDIX_P | Medium | `UserSettingsTest`; `PublicShellTest`; `pref/compile`; `web/compile`; root `compile`; route smoke | Yes | INT-2026-016 | Preference/display settings only; settings cannot authorize stronger live coaching or alter ECR, Used Level, Used Offset, Assistance Load, subscriptions, or audit authority. |
| PM-2026-006 | `build.sbt`; `app/controllers/Dev.scala`; `modules/web/src/main/Env.scala` | Add EvenChess backend/admin settings to the existing Lichess dev settings surface and redact EvenChess backend setting logs. | REQ-MAIN-013, OVR-V12-001, DEC-V12-004, V1.2-E, Appendix L, Appendix M, Appendix N, Appendix O, Appendix P, Appendix R | Medium | `AdminBackendSettingsTest`; `evenchess/test`; `web/compile`; root `compile` | Yes | INT-2026-017 | Admin/backend settings only; raw provider secrets are not stored as settings, and rated fairness can only be affected by explicit incident controls. |
| PM-2026-017 | `app/controllers/Dev.scala`; `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala` | Add EvenChess admin backend setting guardrails so raw secret-like values cannot be persisted through `/dev/settings`. | REQ-MAIN-013, OVR-V12-001, DEC-V12-004, V1.2-E, V1.3-F, APPENDIX_R, Appendix V | Medium | `AdminBackendSettingsTest`; `evenchess/test`; `web/compile`; root `compile` | Yes | INT-2026-034 | Adds save-time raw-secret detection and explicit admin feedback while leaving existing permissions/settings framework unchanged. |
| PM-2026-007 | `ui/round/src/interfaces.ts`; `ui/round/src/view/main.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/src/ctrl.ts`; `ui/round/src/socket.ts`; `ui/round/css/_app-layout.scss`; `ui/round/css/_round.scss`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts` | Attach EvenChess live overlay display to the existing Lichess round UI through an optional namespaced payload, display-only socket handler, stale-clear move hook, and non-overlapping coach grid area. | REQ-MAIN-013, OVR-V12-001, V1.2-F, UI-L1-001..007, UI-L1-020..024, Appendix G, Appendix H, Appendix I, Appendix U, Appendix V | High | `LiveBoardIntegrationTest`; `evenchessOverlay.test.ts`; `ui/round` TypeScript build; stylelint; formatter check | Yes | INT-2026-018 | Live-board UI adapter only; Lichess still owns legal move input, clocks, premoves, keyboard controls, and normal games omit EvenChess overlay data. |
| PM-2026-018 | `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts` | Harden live overlay application so even non-matching-game payloads are marked stale immediately and cleared, ensuring normal games remain unaffected without extra transport logic. | REQ-MAIN-013, OVR-V13-001, V1.3-G, Appendix G, Appendix I | High | `evenchessOverlay.test.ts`; `./ui/test round/tests/evenchessOverlay.test.ts` | Yes | INT-2026-035 | Tightens V1.2 live overlay seam with game-id validation at payload ingress; only namespaced round payload shape changed behavior. |
| PM-2026-036 | `ui/opening/src/evenchessOpeningAi.ts`; `ui/opening/tests/evenchessOpeningAi.test.ts` | Clear stale EvenChess opening overlays when payload is missing/invalid so the opening explorer remains inert without a valid server-authorized payload. | REQ-MAIN-013, V1.3-H, Appendix C, Appendix F, Appendix G, Appendix L, Appendix M, Appendix U, Appendix V | Medium | `./ui/test opening/tests/evenchessOpeningAi.test.ts` | Yes | INT-2026-036 | Opening panel is now removed before each render and only appended when `openingAiStaleReason` passes, preventing stale panel persistence across payload transitions. |
| PM-2026-037 | `ui/lib/src/evenchessTts.ts`; `ui/lib/tests/evenchessTts.test.ts`; `modules/evenchess/src/main/TtsCoach.scala`; `modules/evenchess/src/test/TtsCoachTest.scala` | Require an overlay audit identity for every EvenChess TTS read, including study, analysis, and opening cards, so TTS remains overlay-only and cannot become a separate unaudited advice channel. | REQ-MAIN-013, V1.3-I, Appendix F, Appendix G, Appendix M, Appendix P, Appendix Q, Appendix V | High | `./ui/test lib/tests/evenchessTts.test.ts`; `./ui/test round/tests/evenchessOverlay.test.ts`; `./ui/test analyse/tests/evenchessLearning.test.ts`; `./ui/test opening/tests/evenchessOpeningAi.test.ts`; `evenchess/testOnly lila.evenchess.TtsCoachTest` | Yes | INT-2026-037 | TTS remains browser-speech-only and reads the same visible text, but now all surfaces must be tied to the originating authorized overlay audit id before speech is offered or allowed. |
| PM-2026-008 | `ui/analyse/src/interfaces.ts`; `ui/analyse/src/view/main.ts`; `ui/analyse/src/study/studyView.ts`; `ui/analyse/src/view/evenchessLearning.ts`; `ui/analyse/css/_analyse.base.scss`; `ui/analyse/css/_evenchess-ai.scss`; `ui/analyse/tests/evenchessLearning.test.ts`; `ui/opening/src/interfaces.ts`; `ui/opening/src/opening.ts`; `ui/opening/src/evenchessOpeningAi.ts`; `ui/opening/css/_opening.scss`; `ui/opening/css/_evenchess-ai.scss`; `ui/opening/tests/evenchessOpeningAi.test.ts` | Add optional EvenChess AI learning overlay adapters to Lichess analysis, study, and opening explorer surfaces while leaving Lichess analysis/study/opening engines and UI ownership intact. | REQ-MAIN-013, OVR-V12-001, V1.2-G, Appendix C, Appendix F, Appendix G, Appendix L, Appendix M, Appendix P, Appendix U, Appendix V | Medium | `LearningAiOverlayTest`; `evenchessLearning.test.ts`; `evenchessOpeningAi.test.ts`; `ui/analyse` and `ui/opening` TypeScript build; stylelint; formatter check | Yes | INT-2026-019 | Learning-surface display adapter only; browser payloads are optional, server-authorized, and secret/raw-engine/debug safe. |
| PM-2026-009 | `modules/pref/src/main/PrefForm.scala`; `modules/pref/src/main/FormCompatLayer.scala`; `modules/pref/src/main/JsonView.scala`; `modules/pref/src/main/ui/AccountPref.scala`; `ui/lib/src/evenchessTts.ts`; `ui/lib/tests/evenchessTts.test.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/analyse/src/interfaces.ts`; `ui/analyse/src/view/evenchessLearning.ts`; `ui/analyse/css/_evenchess-ai.scss`; `ui/analyse/tests/evenchessLearning.test.ts`; `ui/opening/src/interfaces.ts`; `ui/opening/src/evenchessOpeningAi.ts`; `ui/opening/css/_evenchess-ai.scss`; `ui/opening/tests/evenchessOpeningAi.test.ts` | Add the optional EvenChess TTS Coach to existing Lichess settings and overlay surfaces through browser-speech-only client adapters that read only authorized visible card text. | REQ-MAIN-013, OVR-V12-001, V1.2-H, Appendix F, Appendix G, Appendix M, Appendix N, Appendix P, Appendix R, Appendix U, Appendix V | High | `TtsCoachTest`; `UserSettingsTest`; `evenchessTts.test.ts`; `evenchessOverlay.test.ts`; `evenchessLearning.test.ts`; `evenchessOpeningAi.test.ts`; pref/UI compile; stylelint; formatter check | Yes | INT-2026-020 | TTS is off by default, live reads require audit identity, and the browser path rejects raw engine/debug/provider/prompt data and separate stronger speech text. |
| PM-2026-010 | `app/Lila.scala`; `app/controllers/EvenChess.scala`; `app/views/evenchess/play.scala`; `conf/routes`; `modules/web/src/main/ui/TopNav.scala` | Add a real EvenChess play/search route and public navigation entrypoint that delegates level, ECR, token eligibility, queue, and coaching-readiness decisions to namespaced EvenChess code. | REQ-MAIN-013, OVR-V12-001, V1.2-I, MODE-L1-010, MODE-L1-012, MODE-L1-014, MODE-L1-020, Appendix J, Appendix K, Appendix N, Appendix P, Appendix U, Appendix V | Medium | `PlaySearchIntegrationTest`; `PublicShellTest`; root compile; route smoke | Yes | INT-2026-021 | Play/search adapter only; no legal move generation, board UI, clocks, normal ratings, or normal Lichess matchmaking changed. |
| PM-2026-011 | `app/controllers/EvenChess.scala`; `app/views/evenchess/play.scala`; `app/views/evenchess/account.scala`; `conf/routes`; `modules/web/src/main/ui/TopNav.scala` | Add an EvenChess account entitlement page and wire play starts to the account entitlement token snapshot instead of the Phase I placeholder. | REQ-MAIN-013, OVR-V12-001, V1.2-J, Appendix N, Appendix O, Appendix P, Appendix U, Appendix V | Medium | `AccountMonetisationUiTest`; `PublicShellTest`; `PlaySearchIntegrationTest`; root compile; route smoke | Yes | INT-2026-022 | Account/token UX adapter only; checkout and rewarded-ad provider callbacks remain server-verified future seams and cannot mutate live help strength or ECR. |
| PM-2026-012 | `app/controllers/Dev.scala`; `conf/routes`; `modules/web/src/main/ui/DevUi.scala`; `modules/mod/src/main/ui/ModUi.scala` | Add a read-only EvenChess operations dashboard under the existing Lichess admin settings permission. | REQ-MAIN-013, OVR-V12-001, V1.2-K, Appendix P, Appendix Q, Appendix R, Appendix U, Appendix V | Medium | `AdminOpsDashboardTest`; `AdminOperationsTest`; `TrustOpsIncidentControlsTest`; `AdminBackendSettingsTest`; root compile; route smoke | Yes | INT-2026-023 | Admin visibility adapter only; no raw provider secrets, no anti-cheat internals, no direct fairness mutations, and normal chess behavior unchanged. |
| PM-2026-013 | `app/http/KeyPages.scala`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`; `app/views/evenchess/play.scala`; `app/views/evenchess/account.scala` | Narrow Version 1.1/1.2 public-shell takeover into Version 1.3 Lichess-first public shell with minimal explicit EvenChess entrypoints and native-looking EvenChess route pages. | REQ-MAIN-015, REQ-MAIN-016, OVR-V13-001, DEC-V13-001, DEC-V13-002 | Medium | `PublicShellTest`; `evenchess/test`; root compile; route smoke | Yes | INT-2026-026 | Restores normal Lichess lobby preload/home/nav behavior while keeping namespaced `/evenchess/*` entrypoints and EvenChess module contracts. |
| PM-2026-014 | `modules/web/src/main/ui/layout.scala` | Keep the Lichess shell structure but change the global visible wordmark from the upstream Lichess/lila name to EvenChess. | REQ-MAIN-015, REQ-MAIN-016, OVR-V13-001, DEC-V13-001, DEC-V13-002 | Medium | root compile; homepage smoke | Yes | INT-2026-027 | Branding-only shell seam; no game, rating, overlay, AI, TTS, or matchmaking behavior changed. |
| PM-2026-015 | `modules/web/src/main/ui/TopNav.scala` | Route the primary `PLAY` navigation to EvenChess search while preserving the Lichess-style navigation shell. | REQ-MAIN-015, REQ-MAIN-016, OVR-V13-001, DEC-V13-003 | Medium | `PublicShellTest`; root compile; homepage/nav smoke | Yes | INT-2026-028 | Primary public play now starts EvenChess; deeper lobby search/game creation integration remains future work. |
| PM-2026-016 | `app/controllers/EvenChess.scala`; `conf/routes`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`; `ui/lobby/src/view/table.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/interfaces.ts`; `ui/lobby/css/_setup.scss`; `ui/lobby/css/_table.scss` | Reuse the native Lichess lobby/setup modal for public EvenChess play/search settings, open it from public Play, and submit to an EvenChess JSON search contract instead of a separate play/search page or normal Lichess game-start submit. | REQ-MAIN-017, REQ-MAIN-018, REQ-MAIN-019, OVR-V1-ACTIVE-001, DEC-V1-ACTIVE-001..003 | High | `PublicShellTest`; `PlaySearchIntegrationTest`; lobby TypeScript/build/style checks; root compile; route smoke | Yes | INT-2026-029 | Native Lichess modal now includes EvenChess Set Level/Target/disclosure controls and lobby search status; real paired-game creation and server overlay emission remain future seams. |

## 3.1 Native integration reapplication order

Phase J reconciles the narrowed/current native integration records. During an upstream Lichess update, reapply in this order:

| Order | Patch map | Integration log | Reapplication rule |
| --- | --- | --- | --- |
| 1 | `PM-2026-013` | `INT-2026-026` | Accept upstream public shell changes first; reapply only EvenChess route/page adjustments that preserve the Lichess shell. |
| 2 | `PM-2026-014` | `INT-2026-027` | Reapply the EvenChess wordmark without changing shared layout structure. |
| 3 | `PM-2026-015` | `INT-2026-028` | Reapply the primary Play target to EvenChess while keeping upstream navigation grouping. |
| 4 | `PM-2026-016` | `INT-2026-029` | Restore upstream lobby/setup modal first; reinsert EvenChess setup fields, submit adapter, and status rendering. |
| 5 | `PM-2026-017` | `INT-2026-034` | Reapply EvenChess admin secret-safety guard at the current settings-save endpoint. |
| 6 | `PM-2026-018` | `INT-2026-035` | Reapply live overlay game-id/payload stale gating in the round adapter. |
| 7 | `PM-2026-036` | `INT-2026-036` | Reapply opening overlay stale-clearing before early return. |
| 8 | `PM-2026-037` | `INT-2026-037` | Keep client/server TTS policy aligned so every read has an overlay audit identity. |

## 4. Detail template

```markdown
### PM-YYYY-### - <short title>
- File touched:
- Upstream source area:
- Why core Lichess file was touched:
- Linked EvenChess requirement(s):
- Upstream merge risk:
- Tests added/updated:
- Normal chess regression performed:
- EvenChess regression performed:
- Integration log entry:
- Can later be isolated:
- Isolation idea:
- Notes:
```

Before upstream sync, review all Medium/High/Unknown entries.

### PM-2026-001 - Register EvenChess Appendix A foundation module
- File touched: `build.sbt`
- Upstream source area: SBT module registry.
- Why core Lichess file was touched: New isolated `modules/evenchess` code must be registered to compile and run tests through the normal lila build.
- Linked EvenChess requirement(s): `ARCH-L1-010`, `VOC-L1-006`, `PUB-L1-001`, `PUB-L1-002`, `PUB-L1-003`, `PUB-L1-004`, `INV-L1-020`, `INV-L1-021`, `INV-L1-022`, `INV-L1-023`, `INV-L1-024`.
- Upstream merge risk: Low.
- Tests added/updated: `modules/evenchess/src/test/ProductInvariantsTest.scala`.
- Normal chess regression performed: Not required for constants-only module registration; no game code, routes, UI, ratings, or matchmaking touched.
- EvenChess regression performed: Appendix A invariant tests added.
- Integration log entry: `INT-2026-000`.
- Can later be isolated: Yes.
- Isolation idea: Keep EvenChess code in `modules/evenchess`; if needed, replace direct build registration with the fork's eventual EvenChess module aggregation pattern.
- Notes: This entry records build registration only. No upstream game behavior was modified.

### PM-2026-002 - Version 1.1 EvenChess-only public shell
- File touched: `app/http/KeyPages.scala`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`.
- Upstream source area: Lichess key-page homepage renderer, lobby homepage view, and shared top navigation.
- Why core Lichess file was touched: Public production entrypoints must present EvenChess-only start/navigation while preserving underlying Lichess internals for platform reuse and regression tests. The homepage no longer needs the normal Lichess lobby preloader because it renders the EvenChess shell.
- Linked EvenChess requirement(s): `REQ-MAIN-012`, `OVR-V11-001`, `PUB-L1-001`, `PUB-L1-002`, `PUB-L1-003`, `PUB-L1-004`, `MODE-L1-001`, `MODE-L1-010`, `MKT-L1-001`, `MKT-L1-002`, `MKT-L1-003`, `MKT-L1-004`, `MKT-L1-005`.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/test/PublicShellTest.scala`.
- Normal chess regression performed: Compile check only; no game rules, round lifecycle, ratings, matchmaking, or move handling touched.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.PublicShellTest`; compile check.
- Integration log entry: `INT-2026-002`.
- Can later be isolated: Yes.
- Isolation idea: Move rendered shell and navigation labels into an EvenChess UI module or config-backed renderer if a clean lila extension seam is added.
- Notes: Public navigation no longer offers ordinary lobby create/challenge/computer play CTAs. Future matchmaking phases should replace anchor targets with real EvenChess routes.

### PM-2026-003 - Version 1.2 deep-blue public shell polish
- File touched: `app/views/lobby/home.scala`.
- Upstream source area: Lichess lobby homepage view/public key page.
- Why core Lichess file was touched: The public homepage is the current Lichess seam for presenting EvenChess as a polished deep-blue Lichess-powered product while preserving compatible platform areas.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `UI-L1-001`, `UI-L1-002`, `UI-L1-003`, `UI-L1-004`, `UI-L1-005`, `UI-L1-006`, `UI-L1-030`, `UI-L1-031`, `UI-L1-032`, `UI-L1-033`, `MKT-L1-001`, `MKT-L1-002`, `MKT-L1-003`, `MKT-L1-004`, `MKT-L1-005`.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/test/EvenChessThemeTest.scala`; `modules/evenchess/src/test/PublicShellTest.scala`.
- Normal chess regression performed: Compile check only; no game rules, round lifecycle, ratings, matchmaking, move handling, study, opening, analysis, or puzzle internals touched.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.EvenChessThemeTest lila.evenchess.PublicShellTest`; `evenchess/test`; compile check.
- Integration log entry: `INT-2026-014`.
- Can later be isolated: Yes.
- Isolation idea: Move inline shell styles into an EvenChess CSS bundle/theme asset once a clean asset seam is chosen, leaving `app/views/lobby/home.scala` as a thin renderer.
- Notes: This entry extends `PM-2026-002` with visual polish and preserved-platform messaging only. It does not add live coaching, ECR, monetisation, AI, TTS, or admin behavior.

### PM-2026-004 - Version 1.2 Lichess feature navigation restoration
- File touched: `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`.
- Upstream source area: Lichess lobby homepage view/public key page and shared top navigation.
- Why core Lichess file was touched: Version 1.2 restores safe access to Lichess-provided learning, exploration, community, and account surfaces while keeping EvenChess as the public product identity and keeping rated play starts away from normal Lichess pools.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `DEC-V12-002`, `ARCH-L1-005`, `ARCH-L1-014`, `C-L1-005`, `MODE-L1-004`, `MKT-L1-001`, `MKT-L1-002`, `MKT-L1-003`, `MKT-L1-004`, `MKT-L1-005`.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/test/PublicShellTest.scala`.
- Normal chess regression performed: Compile check and route smoke for `/learn`, `/training`, `/study`, `/opening`, `/analysis`, `/player`, and account/profile paths. No game rules, ratings, matchmaking, move handling, study internals, opening internals, analysis internals, or account storage changed.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.PublicShellTest`; `evenchess/test`; compile check.
- Integration log entry: `INT-2026-015`.
- Can later be isolated: Yes.
- Isolation idea: Move public nav data into a small EvenChess-aware navigation adapter or config seam once `modules/web` can consume a stable EvenChess navigation contract without widening dependencies.
- Notes: This entry refines `PM-2026-002` and `PM-2026-003`. It restores compatible feature navigation only. It does not make normal rated Lichess play public, does not introduce ECR/matchmaking behavior, and does not attach AI overlays yet.

### PM-2026-005 - Version 1.2 EvenChess user preferences section
- File touched: `build.sbt`; `modules/pref/src/main/PrefCateg.scala`; `modules/pref/src/main/PrefForm.scala`; `modules/pref/src/main/FormCompatLayer.scala`; `modules/pref/src/main/JsonView.scala`; `modules/pref/src/main/ui/AccountPref.scala`; `modules/pref/src/main/ui/AccountUi.scala`; `modules/web/src/main/ui/TopNav.scala`.
- Upstream source area: SBT module dependencies, Lichess preference categories/forms/JSON/account menu, and shared top navigation.
- Why core Lichess file was touched: Version 1.2 Phase D requires an EvenChess section inside the existing Lichess user settings surface, with server-side per-user persistence and safe client config exposure. The smallest durable seam is the existing preference document `tags` map with namespaced `evenchess_` keys.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `V1.2-D`, Appendix F overlay display settings, Appendix H Offset Count display preference, Appendix M AI summary preference, Appendix P privacy/telemetry preference.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/main/UserSettings.scala`; `modules/evenchess/src/test/UserSettingsTest.scala`; `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`.
- Normal chess regression performed: `pref/compile`; `web/compile`; root `compile`; no game rules, round lifecycle, legal move generation, ratings, matchmaking, study internals, opening internals, or analysis internals changed.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.UserSettingsTest lila.evenchess.PublicShellTest` (runs the current EvenChess test suite in this local SBT setup).
- Integration log entry: `INT-2026-016`.
- Can later be isolated: Yes.
- Isolation idea: Keep the setting semantics in `modules/evenchess/src/main/UserSettings.scala`; if a dedicated EvenChess account/preferences service is introduced, replace the `Pref.tags` adapter while preserving the `/account/preferences/evenchess` route and safe client JSON contract.
- Notes: This entry stores user preferences only. These values are display/default requests and cannot grant coaching permission, raise live help above server policy, mutate ECR, change Used Level/Used Offset/Assistance Load, bypass audit logging, or expose provider secrets.

### PM-2026-006 - Version 1.2 EvenChess backend/admin settings
- File touched: `build.sbt`; `app/controllers/Dev.scala`; `modules/web/src/main/Env.scala`.
- Upstream source area: SBT module dependencies, Lichess dev/admin settings controller, and web SettingStore registry.
- Why core Lichess file was touched: Version 1.2 Phase E needs admin-only EvenChess backend settings while reusing the existing Lichess `Settings` permission, `/dev/settings` page, SettingStore persistence, and settings-change audit log path.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `DEC-V12-004`, `V1.2-E`, Appendix L Stockfish infrastructure, Appendix M AI coach policy, Appendix N subscriptions/tokens/ads, Appendix O campaign controls, Appendix P telemetry/audit retention, Appendix R admin operations.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`.
- Normal chess regression performed: `web/compile`; root `compile`; no game rules, board UI, legal move generation, clocks, normal ratings, matchmaking, study internals, opening internals, or analysis internals changed.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.AdminBackendSettingsTest`; `evenchess/test`.
- Integration log entry: `INT-2026-017`.
- Can later be isolated: Yes.
- Isolation idea: Keep backend-setting semantics in `modules/evenchess/src/main/AdminBackendSettings.scala`; if a dedicated EvenChess admin route/dashboard is introduced, keep `/dev/settings` as a thin fallback or migrate the SettingStore list behind an EvenChess admin adapter.
- Notes: The Lichess dev settings page stores provider/model/status/limit/flag values only. Raw OpenAI/TTS provider secrets remain environment/secret-store concerns; the controller redacts secret-looking EvenChess backend values before logging. Rated fairness remains unchanged except for explicit audited incident controls such as pause/no-rate.

### PM-2026-017 - Version 1.3 backend/admin setting guardrail
- File touched: `app/controllers/Dev.scala`; `modules/evenchess/src/main/AdminBackendSettings.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala`.
- Upstream source area: Lichess dev settings save path and EvenChess backend secret-safety helpers.
- Why core Lichess file was touched: Version 1.3-F requires secret-like values never be accepted by the shared `/dev/settings` save endpoint, even for EvenChess namespaced settings, to avoid accidental leakage and incorrect admin input.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `DEC-V12-004`, `V1.2-E`, `V1.3-F`, `Appendix R`, `Appendix V`.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/test/AdminBackendSettingsTest.scala`; `modules/evenchess/src/test/AdminBackendSettingsTest.scala` (targeted detection + redaction regression), `evenchess/test`.
- Normal chess regression performed: `web/compile`; root `compile`; no chess rules, board UI, ratings, matchmaking, study/opening/analysis engines, or account storage internals changed.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.AdminBackendSettingsTest`.
- Integration log entry: `INT-2026-034`.
- Can later be isolated: Yes.
- Isolation idea: Keep the guard with the EvenChess setting model and move to a dedicated EvenChess admin config route as soon as Lichess provides admin extension hooks.
- Notes: This is a defensive continuation of `PM-2026-006`, adding persistence-time safety and admin failure feedback. It does not alter setting keys, values, defaults, permissions, or fairness policy linkage.

### PM-2026-007 - Version 1.2 live round overlay adapte
- File touched: `ui/round/src/interfaces.ts`; `ui/round/src/view/main.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/src/ctrl.ts`; `ui/round/src/socket.ts`; `ui/round/css/_app-layout.scss`; `ui/round/css/_round.scss`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`.
- Upstream source area: Lichess round UI TypeScript, round socket handlers, controller move handling, round grid layout, and round UI tests.
- Why core Lichess file was touched: Version 1.2 Phase F requires the existing Lichess live-board surface to consume future server-authorized EvenChess overlay payloads without rebuilding chessground or moving coaching authority to the browser. The adapter adds an optional `evenchess.live` field, one display-only socket event, move-triggered stale clearing, and a grid area adjacent to the board.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `V1.2-F`, `UI-L1-001`, `UI-L1-002`, `UI-L1-004`, `UI-L1-005`, `UI-L1-007`, `UI-L1-020`, `UI-L1-021`, `UI-L1-022`, `UI-L1-023`, `UI-L1-024`, Appendix G server authority/audit rules, Appendix H Offset Count display boundary, Appendix I assistance accounting boundary, Appendix U board/WebSocket seam, Appendix V regression gates.
- Upstream merge risk: High.
- Tests added/updated: `modules/evenchess/src/main/LiveBoardIntegration.scala`; `modules/evenchess/src/test/LiveBoardIntegrationTest.scala`; `ui/round/tests/evenchessOverlay.test.ts`.
- Normal chess regression performed: `ui/round` TypeScript build, focused round UI test, stylelint, formatter check. Normal games without `evenchess.live` render no overlay and retain existing board/move UI ownership.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.LiveBoardIntegrationTest`; focused UI adapter test covering normal-game omission, one-card/one-visual cap, move stale clearing, unsafe raw-engine/debug suppression, board/ply/expiry/authorization mismatch suppression.
- Integration log entry: `INT-2026-018`.
- Can later be isolated: Yes.
- Isolation idea: Keep policy, payload semantics, and renderability checks in `modules/evenchess`; if lila gains a formal round-extension slot, move `ui/round/src/view/evenchessOverlay.ts` behind that slot and remove the direct view/layout hook.
- Notes: This phase does not emit real server WebSocket overlay payloads yet, does not add stronger client coaching, and does not add TTS playback. Study/opening/analysis overlay adapters are recorded separately in `PM-2026-008`.

### PM-2026-018 - Version 1.3 live round overlay ingress hardening
- File touched: `ui/round/src/view/evenchessOverlay.ts`; `ui/round/tests/evenchessOverlay.test.ts`.
- Upstream source area: Lichess live-board overlay receive/filter test path in `ui/round`.
- Why core Lichess file was touched: Version 1.3 cleanup requires that round overlay payloads for non-active games are inert. This change marks mismatched-game payloads stale at ingress and records a clear reason, rather than preserving potential stale content in state.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V13-001`, `V1.3-G`, Appendix G, Appendix I, Appendix U.
- Upstream merge risk: Medium.
- Tests added/updated: `ui/round/tests/evenchessOverlay.test.ts`.
- Normal chess regression performed: No game lifecycle/rule files changed; the live-board contract remains display-only and gated by game id plus existing server authorization/render checks.
- EvenChess regression performed: Added test for mismatched-game ingress staleness and clear reason enforcement.
- Integration log entry: `INT-2026-035`.
- Can later be isolated: Yes.
- Isolation idea: Keep this hardening inside `ui/round/src/view/evenchessOverlay.ts`; if Lichess adds round payload validation hooks, move this guard there and keep this file a thin adapter.
- Notes: This is a seam-tightening pass only; it does not change when overlays are rendered for authorized, matching-payload gameplay.

### PM-2026-036 - Version 1.3 learning-surface opening-overlay stale guard
- File touched: `ui/opening/src/evenchessOpeningAi.ts`; `ui/opening/tests/evenchessOpeningAi.test.ts`.
- Upstream source area: Lichess opening explorer TypeScript adapter and test.
- Why core Lichess file was touched: Phase H requires study/analysis/opening overlays to stay inert unless an explicit, authorized payload is present. The existing opening adapter could retain stale DOM when a valid payload is later replaced by an invalid/missing payload, so this change ensures stale overlays are removed whenever render runs.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `V1.3-H`, Appendix C Lichess learning-surface boundaries, Appendix F stable coaching overlays, Appendix G server-authoritative policy, Appendix L server-side truth, Appendix M AI content safety, Appendix U seam integrity, Appendix V acceptance gates.
- Upstream merge risk: Medium.
- Tests added/updated: `ui/opening/tests/evenchessOpeningAi.test.ts`.
- Normal chess regression performed: No game rules, legal move generation, board UI, or opening search internals changed. `ui/opening` now guarantees no residual EvenChess UI appears from stale payloads.
- EvenChess regression performed: Added invariant that stale opening panels are removed when payload becomes invalid after prior render.
- Integration log entry: `INT-2026-036`.
- Can later be isolated: Yes.
- Isolation idea: Keep opening overlay validation/render behavior in `ui/opening/src/evenchessOpeningAi.ts`; if Lichess exposes an opening extension hook, move this hook point behind that seam while preserving stale-clear semantics.
- Notes: This is a narrow hardening pass that only affects opening page rendering behavior under the existing `evenchess.openingAi` optional payload contract.

### PM-2026-037 - Version 1.3 TTS overlay-only audit gate
- File touched: `ui/lib/src/evenchessTts.ts`; `ui/lib/tests/evenchessTts.test.ts`; `modules/evenchess/src/main/TtsCoach.scala`; `modules/evenchess/src/test/TtsCoachTest.scala`.
- Upstream source area: Shared frontend library TTS adapter and EvenChess server-side TTS policy module.
- Why core Lichess file was touched: Phase I requires TTS to remain an overlay-only affordance. The existing policy required audit identity for live/rated reads, but learning-surface reads could be allowed without an audit id if a caller built an otherwise-valid item. This change requires every TTS item to carry the originating overlay audit id before the button is offered or playback is allowed.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `V1.3-I`, Appendix F stable overlay UI, Appendix G audit policy, Appendix M AI coach policy, Appendix P telemetry/audit, Appendix Q abuse controls, Appendix V acceptance gates.
- Upstream merge risk: High.
- Tests added/updated: `ui/lib/tests/evenchessTts.test.ts`; `modules/evenchess/src/test/TtsCoachTest.scala`.
- Normal chess regression performed: Focused UI overlay/TTS tests only; no legal move generation, board input, clocks, ratings, matchmaking, study/opening engines, or game lifecycle files changed.
- EvenChess regression performed: `./ui/test lib/tests/evenchessTts.test.ts`; `./ui/test round/tests/evenchessOverlay.test.ts`; `./ui/test analyse/tests/evenchessLearning.test.ts`; `./ui/test opening/tests/evenchessOpeningAi.test.ts`; container `evenchess/testOnly lila.evenchess.TtsCoachTest` passed with the current EvenChess suite.
- Integration log entry: `INT-2026-037`.
- Can later be isolated: Yes.
- Isolation idea: Keep the browser rule in `ui/lib/src/evenchessTts.ts` until Lichess has a formal plugin slot for speech actions; keep server policy in `modules/evenchess/src/main/TtsCoach.scala` as the durable EvenChess contract.
- Notes: Browser TTS still reads only the same visible text and still rejects raw engine, prompt, provider-secret, debug, unsupported-provider, unauthorized, and opponent-turn-muted cases.

### PM-2026-008 - Version 1.2 learning-surface AI overlay adapters
- File touched: `ui/analyse/src/interfaces.ts`; `ui/analyse/src/view/main.ts`; `ui/analyse/src/study/studyView.ts`; `ui/analyse/src/view/evenchessLearning.ts`; `ui/analyse/css/_analyse.base.scss`; `ui/analyse/css/_evenchess-ai.scss`; `ui/analyse/tests/evenchessLearning.test.ts`; `ui/opening/src/interfaces.ts`; `ui/opening/src/opening.ts`; `ui/opening/src/evenchessOpeningAi.ts`; `ui/opening/css/_opening.scss`; `ui/opening/css/_evenchess-ai.scss`; `ui/opening/tests/evenchessOpeningAi.test.ts`.
- Upstream source area: Lichess analysis/study TypeScript views, opening explorer TypeScript initializer, analysis/opening SCSS bundles, and UI tests.
- Why core Lichess file was touched: Version 1.2 Phase G requires optional EvenChess AI coach panels in Lichess-provided study, analysis, and opening explorer surfaces without rebuilding those surfaces or moving AI/provider authority to browser code.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `V1.2-G`, Appendix C Lichess-provided capability boundaries, Appendix F stable overlay UI, Appendix G server-authoritative coaching policy, Appendix L server-side Stockfish truth, Appendix M AI coach policy, Appendix P telemetry/audit, Appendix U integration seams, Appendix V acceptance gates.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/main/LearningAiOverlay.scala`; `modules/evenchess/src/test/LearningAiOverlayTest.scala`; `ui/analyse/tests/evenchessLearning.test.ts`; `ui/opening/tests/evenchessOpeningAi.test.ts`.
- Normal chess regression performed: `ui/analyse` and `ui/opening` TypeScript build, focused UI tests, stylelint, formatter check. Normal analysis/study/opening pages without `evenchess.learning` or `evenchess.openingAi` payloads render no EvenChess panel.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.LearningAiOverlayTest`; focused UI adapter tests covering optional omission, server-authorized rendering, live-rated suppression, raw engine/debug/provider/prompt suppression, context mismatch, and invented source fact suppression.
- Integration log entry: `INT-2026-019`.
- Can later be isolated: Yes.
- Isolation idea: Keep payload semantics and source-fact safety in `modules/evenchess`; if lila gains formal analysis/study/opening extension slots, move the direct `ui/analyse` and `ui/opening` hooks behind those slots while preserving the same optional payload keys.
- Notes: This phase does not create backend provider calls, persistence, audit storage, or real server payload emitters for study/opening/analysis yet. It only defines the safe contract and display adapters for future server-owned learning payloads.

### PM-2026-009 - Version 1.2 Text-to-Speech Coach adapters
- File touched: `modules/pref/src/main/PrefForm.scala`; `modules/pref/src/main/FormCompatLayer.scala`; `modules/pref/src/main/JsonView.scala`; `modules/pref/src/main/ui/AccountPref.scala`; `ui/lib/src/evenchessTts.ts`; `ui/lib/tests/evenchessTts.test.ts`; `ui/round/src/interfaces.ts`; `ui/round/src/view/evenchessOverlay.ts`; `ui/round/css/_evenchess-live.scss`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/analyse/src/interfaces.ts`; `ui/analyse/src/view/evenchessLearning.ts`; `ui/analyse/css/_evenchess-ai.scss`; `ui/analyse/tests/evenchessLearning.test.ts`; `ui/opening/src/interfaces.ts`; `ui/opening/src/evenchessOpeningAi.ts`; `ui/opening/css/_evenchess-ai.scss`; `ui/opening/tests/evenchessOpeningAi.test.ts`.
- Upstream source area: Lichess account preferences, shared frontend library, round overlay UI, analysis/study overlay UI, opening explorer overlay UI, and related tests/styles.
- Why core Lichess file was touched: Version 1.2 Phase H needs users to opt into TTS Coach and needs existing overlay surfaces to offer read-aloud controls without adding a separate advice channel or rebuilding Lichess speech/settings infrastructure.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `V1.2-H`, Appendix F stable overlay UI, Appendix G server-authoritative coaching/audit policy, Appendix M AI coach policy, Appendix N no paid stronger live help, Appendix P telemetry/audit, Appendix R admin incident controls, Appendix U integration seams, Appendix V acceptance gates.
- Upstream merge risk: High.
- Tests added/updated: `modules/evenchess/src/main/TtsCoach.scala`; `modules/evenchess/src/test/TtsCoachTest.scala`; `modules/evenchess/src/main/UserSettings.scala`; `modules/evenchess/src/test/UserSettingsTest.scala`; `ui/lib/tests/evenchessTts.test.ts`; `ui/round/tests/evenchessOverlay.test.ts`; `ui/analyse/tests/evenchessLearning.test.ts`; `ui/opening/tests/evenchessOpeningAi.test.ts`.
- Normal chess regression performed: Focused settings/UI compile checks and route/browser smoke; normal pages without EvenChess TTS payloads or enabled settings do not render read-aloud controls.
- EvenChess regression performed: TTS policy tests cover requirement classification, off-by-default behavior, same-visible-text enforcement, audit identity, unsafe payload suppression, opponent-turn muting, provider seam handling, and no paid stronger live TTS.
- Integration log entry: `INT-2026-020`.
- Can later be isolated: Yes.
- Isolation idea: Keep TTS policy in `modules/evenchess/src/main/TtsCoach.scala` and shared browser behavior in `ui/lib/src/evenchessTts.ts`; if Lichess adds formal extension slots for settings/overlays, move the direct round/analyse/opening button hooks behind those slots.
- Notes: Browser TTS is the only active playback path. Server-side/provider voices remain a seam controlled by admin settings and product-owner approval; no provider keys, raw prompts, raw engine lines, or hidden debug data are sent to clients.

### PM-2026-010 - Version 1.2 EvenChess play/search production entrypoint
- File touched: `app/Lila.scala`; `app/controllers/EvenChess.scala`; `app/views/evenchess/play.scala`; `conf/routes`; `modules/web/src/main/ui/TopNav.scala`.
- Upstream source area: Lichess application controller wiring, Play routes, public route table, and shared top navigation.
- Why core Lichess file was touched: Version 1.2 Phase I replaces public play anchors with a real EvenChess play/search route while keeping Lichess game lifecycle and normal chess internals separate. The new controller is intentionally thin and delegates mode, ECR, Set Level, token eligibility, search queue, and policy-readiness decisions to namespaced EvenChess code.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `V1.2-I`, `MODE-L1-010`, `MODE-L1-012`, `MODE-L1-014`, `MODE-L1-020`, Appendix J ECR/matchmaking isolation, Appendix K Target Level isolation, Appendix N token/access boundary, Appendix P server-authored telemetry, Appendix U play/search/game-policy seams, Appendix V acceptance gates.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`; `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`.
- Normal chess regression performed: Root `compile`; web compile; unauthenticated route smoke for `/evenchess/play` and `/evenchess/play/search`. No legal move generation, board UI, clocks, normal matchmaking pools, normal Lichess ratings, study internals, opening internals, or analysis internals changed.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.PlaySearchIntegrationTest lila.evenchess.PublicShellTest` inside the running `lila` container. The local command ran the current EvenChess suite and passed 351 tests.
- Integration log entry: `INT-2026-021`.
- Can later be isolated: Yes.
- Isolation idea: Keep `PlaySearchIntegration` as the stable EvenChess-owned contract. If Lichess gains formal extension points for public play/search/pairing, move `controllers.EvenChess` and the direct route/nav edits behind those extension points while preserving the same policy and telemetry gates.
- Notes: The route starts and persists an EvenChess search intent and blocks coaching until paired-game policy metadata is persisted. It does not yet attach the final lila hook/pairing/game-instantiation adapter or real account entitlement storage; Phase J should replace the Phase I onboarding token snapshot with the production token/subscription account source.

### PM-2026-011 - Version 1.2 EvenChess account entitlement UX
- File touched: `app/controllers/EvenChess.scala`; `app/views/evenchess/play.scala`; `app/views/evenchess/account.scala`; `conf/routes`; `modules/web/src/main/ui/TopNav.scala`.
- Upstream source area: Lichess route table, application controller/view layer, and shared top navigation.
- Why core Lichess file was touched: Version 1.2 Phase J needs a user-visible account surface for EvenChess tokens, plan descriptions, rewarded-ad state, summary quotas, and token settlement rules while reusing Lichess account/session routing. The play-search route also needs to read token eligibility from the account entitlement contract instead of a hard-coded Phase I token snapshot.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `V1.2-J`, Appendix N subscriptions/tokens/ads, Appendix O funnel/control copy, Appendix P stable funnel telemetry, Appendix U account/play seams, Appendix V acceptance gates.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/main/AccountMonetisationUi.scala`; `modules/evenchess/src/test/AccountMonetisationUiTest.scala`; `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`.
- Normal chess regression performed: Root `compile`; route smoke. No legal move generation, board UI, clocks, normal ratings, normal Lichess matchmaking, study, opening, analysis, or account persistence internals changed.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.AccountMonetisationUiTest lila.evenchess.PublicShellTest lila.evenchess.PlaySearchIntegrationTest` in the running `lila` container. The local SBT command ran the current EvenChess suite and passed 357 tests.
- Integration log entry: `INT-2026-022`.
- Can later be isolated: Yes.
- Isolation idea: Keep account economics and UX state in `modules/evenchess/src/main/AccountMonetisationUi.scala`; if Lichess gains a formal account-extension slot, move the direct `/evenchess/account` controller/view wiring behind that slot and preserve the same token snapshot, plan-copy, summary-quota, and settlement-rule contracts.
- Notes: This phase intentionally does not activate real checkout or rewarded-ad provider callbacks. Provider completion, purchase, refund, and quota mutation remain server-side seams requiring verified provider configuration. Payment, ad, campaign, and subscription state cannot change ECR, Set Level, Used Level, Used Offset, Stockfish profile, AI exactness, Target isolation, or live coaching strength.

### PM-2026-012 - Version 1.2 EvenChess operations dashboard
- File touched: `app/controllers/Dev.scala`; `conf/routes`; `modules/web/src/main/ui/DevUi.scala`; `modules/mod/src/main/ui/ModUi.scala`.
- Upstream source area: Lichess dev/admin controller, Play route table, dev/admin UI renderer, and moderation admin menu.
- Why core Lichess file was touched: Version 1.2 Phase K needs admins to see EvenChess runtime health, active policy/model/config/engine/TTS versions, audit search summaries, incident controls, rollbackable feature flags, and paid-launch readiness without rebuilding the Lichess admin platform.
- Linked EvenChess requirement(s): `REQ-MAIN-013`, `OVR-V12-001`, `V1.2-K`, Appendix P telemetry/analytics, Appendix Q trust controls, Appendix R admin operations, Appendix U admin/ops seam, Appendix V acceptance gates.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/main/AdminOpsDashboard.scala`; `modules/evenchess/src/test/AdminOpsDashboardTest.scala`.
- Normal chess regression performed: Root `compile`; unauthenticated route smoke for `/dev/evenchess/ops` returning `303 /signup`. No game rules, board UI, legal move generation, clocks, game lifecycle, ratings, matchmaking, study, opening, analysis, or account persistence internals changed.
- EvenChess regression performed: `evenchess/testOnly lila.evenchess.AdminOpsDashboardTest lila.evenchess.AdminOperationsTest lila.evenchess.TrustOpsIncidentControlsTest lila.evenchess.AdminBackendSettingsTest` in the running `lila` container. The local SBT command ran the current EvenChess suite and passed 366 tests.
- Integration log entry: `INT-2026-023`.
- Can later be isolated: Yes.
- Isolation idea: Keep dashboard semantics in `modules/evenchess/src/main/AdminOpsDashboard.scala`; if Lichess gains a formal admin-extension slot, move the direct `Dev` route/view/menu hooks behind that slot while preserving the same read-only, secret-safe dashboard model.
- Notes: The dashboard is read-only and reads current EvenChess admin setting values from the existing Lichess SettingStore-backed `/dev/settings` seam, while using sanitized Phase K/local metric and audit sample state until live metric, ledger, and provider adapters are wired. Fairness-affecting actions are displayed as audited/versioned-policy paths only; they are not executed directly from this page. Raw provider secrets and anti-cheat internals must not be rendered.

### PM-2026-013 - Version 1.3 minimal Lichess integration cleanup
- File touched: `app/http/KeyPages.scala`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`; `app/views/evenchess/play.scala`; `app/views/evenchess/account.scala`.
- Upstream source area: Lichess homepage preloader/render path, lobby homepage view, shared top navigation, and app view layer for namespaced EvenChess pages.
- Why core Lichess file was touched: Product direction changed from broad EvenChess public-shell takeover to Lichess-first integration. The homepage and top navigation were narrowed back toward upstream Lichess while retaining small explicit EvenChess entrypoints and namespaced route pages.
- Linked EvenChess requirement(s): `REQ-MAIN-015`, `REQ-MAIN-016`, `OVR-V13-001`, `DEC-V13-001`, `DEC-V13-002`.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`; `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_3_COMPLETION_REPORT.md`.
- Normal chess regression performed: Root `compile`; route smoke for `/`, `/study`, `/analysis`, `/opening`; homepage now uses normal Lichess preload/lobby app path and normal play navigation.
- EvenChess regression performed: `PublicShellTest`; `evenchess/test`; route smoke for `/evenchess/play`, `/evenchess/account`, `/account/preferences/evenchess`, and `/dev/evenchess/ops`.
- Integration log entry: `INT-2026-026`.
- Can later be isolated: Yes.
- Isolation idea: If Lichess adds public-shell extension slots, move the one homepage callout and one top-nav section behind those slots. Keep all policy, ECR, overlay, AI/TTS, settings, admin, and monetisation contracts in `modules/evenchess`.
- Notes: This entry supersedes the broad public homepage/nav portions of `PM-2026-002`, `PM-2026-003`, and `PM-2026-004`; it does not remove `/evenchess/*` routes, settings, admin, live-board overlay adapters, learning overlays, TTS, or OpenAI provider wiring.

### PM-2026-014 - Version 1.3 EvenChess shell wordmark
- File touched: `modules/web/src/main/ui/layout.scala`.
- Upstream source area: Shared Lichess page layout/header.
- Why core Lichess file was touched: Version 1.3 keeps the Lichess shell structure but the product owner clarified that the visible product name should be EvenChess, not `lila`/Lichess.
- Linked EvenChess requirement(s): `REQ-MAIN-015`, `REQ-MAIN-016`, `OVR-V13-001`, `DEC-V13-001`, `DEC-V13-002`.
- Upstream merge risk: Medium.
- Tests added/updated: None; branding-only template change.
- Normal chess regression performed: Root `compile`; homepage smoke.
- EvenChess regression performed: Homepage smoke verifies EvenChess wordmark remains visible.
- Integration log entry: `INT-2026-027`.
- Can later be isolated: Yes.
- Isolation idea: If a future branding config seam is added, move the wordmark string behind an EvenChess brand config instead of editing the shared layout directly.
- Notes: This is a visual branding seam only. It does not alter normal Lichess chess rules, game lifecycle, ratings, matchmaking, overlays, AI, TTS, or coaching authority.

### PM-2026-015 - Version 1.3 primary Play navigation routes to EvenChess
- File touched: `modules/web/src/main/ui/TopNav.scala`.
- Upstream source area: Shared Lichess top navigation.
- Why core Lichess file was touched: Product-owner clarification says the site should look like Lichess, but clicking `PLAY` should start EvenChess search using EvenChess matchmaking/settings rather than ordinary Lichess play.
- Linked EvenChess requirement(s): `REQ-MAIN-015`, `REQ-MAIN-016`, `OVR-V13-001`, `DEC-V13-003`.
- Upstream merge risk: Medium.
- Tests added/updated: `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/test/PublicShellTest.scala`.
- Normal chess regression performed: Root `compile`; navigation shell remains otherwise Lichess-style.
- EvenChess regression performed: `PublicShellTest`; homepage/nav smoke.
- Integration log entry: `INT-2026-028`.
- Can later be isolated: Yes.
- Isolation idea: If Lichess adds top-nav extension/config hooks, move the primary play target and menu entries behind an EvenChess navigation adapter.
- Notes: This is the first visible routing correction toward the desired flow. It does not yet replace the full Lichess lobby seek/search implementation, create real EvenChess paired games, or emit live coaching overlays.

### PM-2026-016 - Current V1 native Lichess setup modal with EvenChess settings
- File touched: `app/controllers/EvenChess.scala`; `conf/routes`; `app/views/lobby/home.scala`; `modules/web/src/main/ui/TopNav.scala`; `ui/lobby/src/view/table.ts`; `ui/lobby/src/view/setup/modal.ts`; `ui/lobby/src/setupCtrl.ts`; `ui/lobby/src/interfaces.ts`; `ui/lobby/css/_setup.scss`; `ui/lobby/css/_table.scss`.
- Upstream source area: EvenChess route/controller adapter, Lichess top navigation, lobby homepage SSR fallback, lobby table renderer, setup modal renderer, setup controller/storage, lobby search status styling, and setup modal styling.
- Why core Lichess file was touched: The product-owner clarified that EvenChess should look like Lichess all the way through public play. The existing Lichess Play navigation and setup modal/time-control flow should carry EvenChess settings rather than sending users to a separate search page or starting ordinary Lichess rated pools.
- Linked EvenChess requirement(s): `REQ-MAIN-017`, `REQ-MAIN-018`, `REQ-MAIN-019`, `OVR-V1-ACTIVE-001`, `DEC-V1-ACTIVE-001`, `DEC-V1-ACTIVE-002`, `DEC-V1-ACTIVE-003`, Appendix D, Appendix F, Appendix J, Appendix U.
- Upstream merge risk: High.
- Tests added/updated: `modules/evenchess/src/main/PublicShell.scala`; `modules/evenchess/src/main/PlaySearchIntegration.scala`; `modules/evenchess/src/test/PublicShellTest.scala`; `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`.
- Normal chess regression performed: Root compile and route smoke; Lichess board, clocks, legal moves, studies, openings, analysis, puzzles, accounts, and game lifecycle internals are not modified.
- EvenChess regression performed: Focused public shell and play/search integration tests; lobby TypeScript/build/style checks.
- Integration log entry: `INT-2026-029`.
- Can later be isolated: Yes.
- Isolation idea: Move the setup-modal fields, `/#hook` Play entrypoint, lobby search status, and submit adapter behind a formal Lichess lobby/setup extension slot if upstream provides one. Keep route/policy/search semantics in `modules/evenchess`.
- Notes: This changes the public lobby setup flow only. It still does not create real paired EvenChess games through lila pairing/game creation and does not emit live game overlay payloads; those remain the next high-risk production seams.
