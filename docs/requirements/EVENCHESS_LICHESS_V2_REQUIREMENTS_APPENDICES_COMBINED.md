# EvenChess-Lichess Version 2 Requirements — Combined Main + Appendices
**Combined file name:** `EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
**Purpose:** Single Markdown read file for Codex/reference. The individual files under `docs/requirements` remain useful for targeted editing unless project instructions designate this combined file as the source of truth.
**Generated from uploaded Markdown files in this chat.**

## Combined file contents

- README: `README_V2_REQUIREMENTS_SUITE.md`
- MAIN: `EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- APPENDIX A: `APPENDIX_A_V2_PRODUCT_INVARIANTS_AND_SCOPE.md`
- APPENDIX B: `APPENDIX_B_V2_SEWN_IN_LICHESS_ARCHITECTURE.md`
- APPENDIX C: `APPENDIX_C_V2_LICHESS_PROVIDED_CAPABILITIES_AND_BOUNDARIES.md`
- APPENDIX D: `APPENDIX_D_V2_BRANDING_THEME_HOME_LANDING_AND_NAVIGATION.md`
- APPENDIX E: `APPENDIX_E_V2_ACCOUNTS_TOKENS_TOP_BAR_AND_MONETISATION.md`
- APPENDIX F: `APPENDIX_F_V2_SETTINGS_AND_ADMIN_CONTROLS.md`
- APPENDIX G: `APPENDIX_G_V2_PLAY_SETUP_QUICK_SEARCH_AND_MATCHMAKING_UI.md`
- APPENDIX H: `APPENDIX_H_V2_MMR_ENGINE_AND_MATCH_CONTRACTS.md`
- APPENDIX I: `APPENDIX_I_V2_ECR_RATING_SETTLEMENT_AND_CALIBRATION.md`
- APPENDIX J: `APPENDIX_J_V2_ECE_ENGINE_REQUIREMENTS.md`
- APPENDIX K: **MISSING FROM UPLOAD SET**
- APPENDIX L: `APPENDIX_L_V2_LIVE_GAME_ASSISTANCE_AND_ECE_HISTORY.md`
- APPENDIX M: `APPENDIX_M_V2_REVIEW_ANALYSIS_CUSTOM_LEVELS_AND_FULL_GAME_ECE.md`
- APPENDIX N: `APPENDIX_N_V2_COACHING_LADDER_OVERLAY_AND_TEXT_LEVEL_GATES.md`
- APPENDIX O: `APPENDIX_O_V2_OFFSET_COUNT_HANGING_THREATS_AND_PINS.md`
- APPENDIX P: `APPENDIX_P_V2_STOCKFISH_AI_AND_TTS_POLICY.md`
- APPENDIX Q: `APPENDIX_Q_V2_PUZZLES_STUDY_OPENINGS_ANALYSIS_AND_COMPUTER_PLAY.md`
- APPENDIX R: `APPENDIX_R_V2_TELEMETRY_AUDIT_AND_DATA_RETENTION.md`
- APPENDIX S: `APPENDIX_S_V2_ABUSE_FAIRNESS_AND_TRUST_CONTROLS.md`
- APPENDIX T: `APPENDIX_T_V2_OPERATIONS_DEPLOYMENT_AND_INCIDENT_RESPONSE.md`
- APPENDIX U: `APPENDIX_U_V2_DATA_MODELS_AND_LICHESS_SEAMS.md`
- APPENDIX V: `APPENDIX_V_V2_TESTING_QA_AND_ACCEPTANCE_CRITERIA.md`
- APPENDIX W: `APPENDIX_W_V2_CODEX_PHASE_PLAN_AND_TASK_PACKETS.md`
- APPENDIX X: `APPENDIX_X_V2_UPSTREAM_SYNC_AND_PATCH_MAP_REQUIREMENTS.md`
- APPENDIX Y: `APPENDIX_Y_V2_RESERVED_FUTURE_REQUIREMENTS.md`
- APPENDIX Z: `APPENDIX_Z_V2_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md`

---

<!-- BEGIN FILE: README_V2_REQUIREMENTS_SUITE.md -->

# EvenChess-Lichess Version 2 Requirements Suite

This folder contains the Markdown-only Version 2 requirements suite.

Version 2 direction:

- EvenChess is the public product.
- Lichess/lila is the underlying platform.
- EvenChess is sewn into the Lichess experience rather than implemented as a detached add-on.
- Public play/search/rating/matchmaking use EvenChess ECR/MMR, Set Level, Used Level, ECE, Display Engine, tokens, and coaching audit rules.
- Lichess-provided chess basics are reused, not rebuilt.

Start with:

1. `EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
2. relevant appendix files
3. `APPENDIX_Z_V2_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md` for supersessions

<!-- END FILE: README_V2_REQUIREMENTS_SUITE.md -->

---

<!-- BEGIN FILE: EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md -->

# EvenChess-Lichess Version 2 Requirements Main Index

**Suite:** EvenChess-Lichess Version 2
**Status:** Live Markdown requirements draft for sewn-in Lichess integration
**Repository target:** `foughtapple/EvenChess-Lichess`
**Live requirements path:** `docs/requirements`
**Supporting implementation path:** `docs/evenchess`
**Generated:** 2026-05-30

## 1. Purpose and authority

This document is the navigation and authority layer for the EvenChess-Lichess Version 2 requirements suite.

Version 2 supersedes the earlier interpretation where EvenChess was treated as a separate thing sitting beside or on top of Lichess. The Version 2 product direction is:

> EvenChess is the public product. Lichess/lila is the underlying chess platform. EvenChess must be sewn into the Lichess experience, not bolted on as a detached feature.

The site should still look and feel like Lichess: native lobby/search flow, native board feel, native clocks, native game lifecycle, native analysis and study surfaces, native account/settings concepts, and compatible Lichess feature areas. The visible product identity, public play/search, rating system, matchmaking, coaching surfaces, token economy, and assisted-mode behavior are EvenChess-owned.

## 2. Version 2 product goal

EvenChess-Lichess Version 2 must transform the fork into a polished, deep-blue, Lichess-powered EvenChess platform.

The player should not feel that EvenChess is an extra plugin page. A normal user should experience:

- EvenChess branding instead of public Lichess branding where product identity appears;
- a Lichess-shaped homepage, top navigation, play modal, quick pairing cards, board, analysis, study, puzzles, openings, settings, and account flow;
- EvenChess MMR/ECR and matchmaking replacing ordinary public Lichess rating/search behavior for public play;
- EvenChess Set Level / Used Level / Target Level controls integrated into the native Lichess search/setup UI;
- EvenChess coaching overlays and cards integrated into every board surface where allowed;
- EvenChess token/subscription/ad balances integrated into the native top bar and account surfaces;
- game review and analysis integrated into the native analysis/replay experience, but powered by ECE history and review-token rules.

## 3. Version 2 supersession statement

Version 2 replaces these earlier assumptions where they conflict:

| Earlier assumption | Version 2 replacement |
|---|---|
| EvenChess as a separate mode page or addon beside Lichess. | EvenChess is sewn into public Lichess-style play/search/board/review surfaces. |
| Preserve normal Lichess public shell with minimal EvenChess entrypoints. | Preserve the Lichess feel and internals, but the public product identity and public play are EvenChess. |
| Feed an equivalent rating into Lichess matchmaking. | Use an EvenChess MMR Engine / match-contract layer that owns pairing logic, then hands a finalized game contract into the Lichess game lifecycle. |
| Show many separate coaching text sections. | Consolidate deterministic facts and AI wording into compact Board Summary and Plan cards. |
| Store only normal game history. | Store ECE-per-turn history with FEN and highest-level-per-turn output for replay/review. |

## 4. Non-negotiable invariants

- EvenChess is a disclosed assisted chess variant, not ordinary chess with hidden help.
- Lichess/lila provides the mature platform foundation; EvenChess changes public product identity, rating/search/matchmaking, and assisted-mode behavior.
- Do not rebuild Lichess-provided chess basics: legal moves, base board, clocks, base game rooms, accounts, PGN/replay, analysis infrastructure, puzzles/study/openings foundations, WebSockets, or game lifecycle.
- Public play/search must use EvenChess-owned matchmaking, ECR/MMR, Set Level, token/account gates, and assisted-mode disclosure.
- Normal Lichess ratings and rated pools must not be treated as EvenChess ECR/MMR.
- Client-side code must never decide coaching permission. Server-authorized policy controls what the UI can show.
- ECE produces coaching payloads; the EvenChess Display Engine renders them; the server remains authority over live coaching permission and audit.
- Used Level may increase during a game but must never decrease for rating/accounting.
- Every coaching render, suppression, expansion, proposed-move check, and level increase must be auditable.
- Stockfish remains server-side/ECE-side only. The client never receives unrestricted raw engine access.
- AI explains and compresses deterministic/ECE truth packets. It does not invent chess truth, select moves independently, or bypass level gates.
- Higher levels improve specificity and candidate precision; they must not flood the player with long text.
- Premium, tokens, ads, and marketing configuration must not create stronger live rated help than the server-authorized level permits.
- Offset Count is the existing Exchange Resolver / take-take-take feature, not a missing feature.
- Offset Count visual rule: equal = blue shield; student-positive = green number; student-negative = red number; unknown = not equal.

## 5. Requirement classification key

| Class | Meaning | Implementation direction |
|---|---|---|
| Lichess-provided | Capability already exists in lila/Lichess. | Reuse, adapt, theme, or hook; do not rebuild. |
| EvenChess-owned replacement | Public Lichess behavior is replaced by EvenChess logic. | Implement through thin seams and match contracts; record core-file edits. |
| EvenChess-specific addition | Capability unique to assisted chess. | Implement under EvenChess namespace where possible. |
| Adapted Lichess surface | Existing Lichess UI/flow is extended with EvenChess controls. | Preserve native Lichess feel; add integrated controls. |
| Superseded | Earlier requirement no longer fits Version 2. | Do not implement; record in Appendix Z. |
| Unresolved | Product or technical decision needed. | Stop and ask; do not guess. |

## 6. Required folder structure

| Path | Role |
|---|---|
| `docs/requirements` | Live Markdown requirements source of truth. |
| `docs/evenchess` | Operational notes, implementation reports, handovers, and design notes. |
| `docs/integration` | Lichess seam/integration ledger and patch-map operational reports. |
| `AGENTS.md` | Always-on Codex operating instructions. |

Markdown is authoritative. Do not maintain duplicate `.docx` requirement copies unless explicitly requested as export-only documents.

## 7. Appendix navigation map

| Appendix | File | Primary authority |
|---|---|---|
| A | `APPENDIX_A_V2_PRODUCT_INVARIANTS_AND_SCOPE.md` | Product invariants and Version 2 sewn-in scope. |
| B | `APPENDIX_B_V2_SEWN_IN_LICHESS_ARCHITECTURE.md` | Architecture, Lichess seams, and integration posture. |
| C | `APPENDIX_C_V2_LICHESS_PROVIDED_CAPABILITIES_AND_BOUNDARIES.md` | What Lichess provides and where EvenChess may adapt. |
| D | `APPENDIX_D_V2_BRANDING_THEME_HOME_LANDING_AND_NAVIGATION.md` | Deep-blue theme, wordmark, homepage, landing page, top nav. |
| E | `APPENDIX_E_V2_ACCOUNTS_TOKENS_TOP_BAR_AND_MONETISATION.md` | Tokens, subscriptions, ads, top-bar balances, account gates. |
| F | `APPENDIX_F_V2_SETTINGS_AND_ADMIN_CONTROLS.md` | User EvenChess settings and admin/backend controls. |
| G | `APPENDIX_G_V2_PLAY_SETUP_QUICK_SEARCH_AND_MATCHMAKING_UI.md` | Native setup modal, quick search cards, target-level controls. |
| H | `APPENDIX_H_V2_MMR_ENGINE_AND_MATCH_CONTRACTS.md` | EvenChess MMR Engine, scenarios, match contracts, widening. |
| I | `APPENDIX_I_V2_ECR_RATING_SETTLEMENT_AND_CALIBRATION.md` | ECR, Used Offset, settlement, calibration. |
| J | `APPENDIX_J_V2_ECE_ENGINE_REQUIREMENTS.md` | ECE board/proposed/full-game modes and payload contracts. |
| K | `APPENDIX_K_V2_DISPLAY_ENGINE_OVERLAYS_AND_CARDS.md` | Display Engine, overlays, cards, cache, proposed moves. |
| L | `APPENDIX_L_V2_LIVE_GAME_ASSISTANCE_AND_ECE_HISTORY.md` | Live ECE calls, Used Level, per-turn history. |
| M | `APPENDIX_M_V2_REVIEW_ANALYSIS_CUSTOM_LEVELS_AND_FULL_GAME_ECE.md` | Live review modes, custom analysis, L10 review tokens. |
| N | `APPENDIX_N_V2_COACHING_LADDER_OVERLAY_AND_TEXT_LEVEL_GATES.md` | Level-by-level coaching gates. |
| O | `APPENDIX_O_V2_OFFSET_COUNT_HANGING_THREATS_AND_PINS.md` | Overlay-specific requirements for Offset Count, hanging pieces, threats, pins. |
| P | `APPENDIX_P_V2_STOCKFISH_AI_AND_TTS_POLICY.md` | Stockfish, AI, TTS, provider policy. |
| Q | `APPENDIX_Q_V2_PUZZLES_STUDY_OPENINGS_ANALYSIS_AND_COMPUTER_PLAY.md` | Adapting Lichess feature areas under EvenChess. |
| R | `APPENDIX_R_V2_TELEMETRY_AUDIT_AND_DATA_RETENTION.md` | Telemetry, audit, ECE history retention. |
| S | `APPENDIX_S_V2_ABUSE_FAIRNESS_AND_TRUST_CONTROLS.md` | Abuse, non-platform help, token/review abuse, collusion. |
| T | `APPENDIX_T_V2_OPERATIONS_DEPLOYMENT_AND_INCIDENT_RESPONSE.md` | Operations, rollout, kill switches, incidents. |
| U | `APPENDIX_U_V2_DATA_MODELS_AND_LICHESS_SEAMS.md` | Data models and lila integration seams. |
| V | `APPENDIX_V_V2_TESTING_QA_AND_ACCEPTANCE_CRITERIA.md` | Tests, QA, acceptance gates. |
| W | `APPENDIX_W_V2_CODEX_PHASE_PLAN_AND_TASK_PACKETS.md` | Phases and task packets. |
| X | `APPENDIX_X_V2_UPSTREAM_SYNC_AND_PATCH_MAP_REQUIREMENTS.md` | Patch map and upstream sync. |
| Y | `APPENDIX_Y_V2_RESERVED_FUTURE_REQUIREMENTS.md` | Reserved. |
| Z | `APPENDIX_Z_V2_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md` | Overrides and supersessions. |

## 8. Source-of-truth workflow

REQ-MAIN-V2-001: Codex must read this main document first.
REQ-MAIN-V2-002: Codex must read relevant appendices before implementation.
REQ-MAIN-V2-003: Codex must treat Version 2 as superseding earlier public-shell/entrypoint interpretations where conflicts exist.
REQ-MAIN-V2-004: Codex must not implement a separate EvenChess app/page if the requirement says to sew into native Lichess flow.
REQ-MAIN-V2-005: Every upstream/core Lichess file edit must be recorded in the patch map or integration log.
REQ-MAIN-V2-006: Code changes must be small, inspectable, and phase-scoped.
REQ-MAIN-V2-007: New contradictions must be recorded in Appendix Z before implementation proceeds.
REQ-MAIN-V2-008: Implementation reports must state files changed, tests run, seams touched, rollback notes, and remaining risks.

## 9. Implementation priority

Version 2 implementation should proceed in controlled phases:

1. Rewrite requirements and patch-map rules.
2. Confirm current code state and identify wrong earlier addon-style work.
3. Repair public product identity and deep-blue theme without broad rewrites.
4. Integrate EvenChess controls into native Lichess play/setup/search UI.
5. Introduce EvenChess MMR Engine and match contracts.
6. Integrate live ECE call/response storage and Display Engine rendering.
7. Add review/custom analysis/full-game ECE token flow.
8. Expand feature surfaces: puzzles, study, openings, analysis, computer play.
9. Add monetisation, ads, account gates, and top-bar token balances.
10. Harden telemetry, audit, abuse controls, and deployment gates.

## 10. Current implementation decisions

| Decision | Default for Version 2 |
|---|---|
| Whether normal Lichess public rated play remains exposed | No, public play routes to EvenChess; normal internals remain for regression/reuse. |
| Whether Lichess wordmark appears anywhere user-facing | Replace public product identity with EvenChess except where legal attribution/source context requires Lichess mention. |
| ECE history retention limit | Rolling default is the user's last 10 games with attached live ECE history; requested full-game/custom analyses retain the user's last 100 requested analyses. |
| Custom analysis token quantities | Launch defaults are product-configured; the interface must support separate game-start, proposed-move, custom-analysis, and full-game-analysis quota paths without granting stronger live help. |
| Which Lichess analysis pin style is reused | Pinned pieces display a top-left pin/lock marker on the pinned square, using Lichess-compatible board styling where feasible. |
| Whether full-game ECE accepts PGN, FEN list, or both | Prefer both: PGN plus normalized move/FEN list if available. |

<!-- END FILE: EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md -->

---

<!-- BEGIN FILE: APPENDIX_A_V2_PRODUCT_INVARIANTS_AND_SCOPE.md -->

# Appendix A — Version 2 Product Invariants and Scope

## A.1 Purpose

This appendix defines the non-negotiable product rules for EvenChess-Lichess Version 2.

Version 2 changes the integration posture: EvenChess is no longer treated as a detached add-on. EvenChess is the public product sewn into the Lichess-powered experience.

## A.2 Product identity

REQ-A-V2-001: The visible public product name is EvenChess.

REQ-A-V2-002: Public users should experience a polished deep-blue Lichess-powered chess site, not an obviously separate plugin or addon.

REQ-A-V2-003: Lichess/lila remains the underlying platform foundation for accounts, board UI, legal moves, game lifecycle, clocks, PGN/replay, analysis infrastructure, puzzles, study, opening explorer, and operational scaffolding.

REQ-A-V2-004: Public play, public matchmaking, public rating language, coaching overlays, tokens, settings, and monetisation are EvenChess-owned.

## A.3 Assisted chess invariants

REQ-A-V2-010: EvenChess is a disclosed assisted chess variant.

REQ-A-V2-011: Non-platform guidance remains prohibited in rated EvenChess, including external engines, humans, bots, browser extensions, unaudited notes, and stream chat.

REQ-A-V2-012: Platform coaching is legal only because it is disclosed, capped by Set Level, logged by the server, measured as actual use, and priced into ECR/EvenChessRating.

REQ-A-V2-013: All public L0-L10 levels may be used in rated play when assigned or allowed. High levels are calibrated and priced, not hard-banned.

REQ-A-V2-014: Premium must never provide stronger live rated help.

## A.4 Authority invariants

REQ-A-V2-020: Client code must never decide live coaching permission.

REQ-A-V2-021: Server-side policy owns Set Level, allowed live level, Used Level, Assistance Load, Used Offset, ECR, token gates, and whether coaching can be rendered.

REQ-A-V2-022: Used Level never decreases during a rated game.

REQ-A-V2-023: Every displayed or suppressed coaching object must be auditable.

REQ-A-V2-024: ECE and the Display Engine may generate and render payloads, but only within server-authorized limits.

## A.5 Scope boundaries

REQ-A-V2-030: Do not rebuild Lichess-provided platform basics.

REQ-A-V2-031: Do adapt Lichess public flows where necessary so EvenChess feels native.

REQ-A-V2-032: Do replace public Lichess rating/matchmaking behavior with EvenChess ECR/MMR behavior for public EvenChess play.

REQ-A-V2-033: Do preserve normal Lichess internals for reuse, diagnostics, upstream sync, and regression tests.

REQ-A-V2-034: Do not scatter EvenChess logic through unrelated files without patch-map entries and a seam justification.

<!-- END FILE: APPENDIX_A_V2_PRODUCT_INVARIANTS_AND_SCOPE.md -->

---

<!-- BEGIN FILE: APPENDIX_B_V2_SEWN_IN_LICHESS_ARCHITECTURE.md -->

# Appendix B — Sewn-In Lichess Architecture

## B.1 Purpose

This appendix defines how EvenChess is integrated into Lichess/lila in Version 2.

The rule is not "build another thing on top". The rule is:

> Keep the Lichess platform and user experience shape, but make the public product EvenChess.

## B.2 Architecture model

REQ-B-V2-001: Lichess/lila remains the core application foundation.

REQ-B-V2-002: EvenChess replaces public play/search/rating behavior through integrated seams, not by creating a disconnected duplicate chess platform.

REQ-B-V2-003: EvenChess-specific engines and modules should remain logically separated where feasible: MMR Engine, ECE, Display Engine, token economy, settings, admin controls, audit, and review systems.

REQ-B-V2-004: The UI should reuse Lichess layout patterns. EvenChess controls should look native to the page they are placed in.

## B.3 Likely integration seams

Likely seams include:

- homepage/product identity templates;
- top navigation and wordmark;
- lobby quick-pairing cards;
- native play/setup modal;
- seek/search submission;
- lobby/pairing service path;
- challenge creation and acceptance;
- round page payloads;
- board overlay rendering layer;
- analysis/replay pages;
- study and opening explorer board surfaces;
- user settings/preferences;
- account/top-bar surfaces;
- plan/subscription pages;
- admin/ops pages.

REQ-B-V2-010: Any upstream/core Lichess file touched for these seams must be recorded in the patch map.

REQ-B-V2-011: The integration should prefer thin adapters that call EvenChess-owned modules.

REQ-B-V2-012: Broad rewrites of Lichess pages are prohibited unless the phase explicitly approves the seam and rollback path.

## B.4 Public product behavior

REQ-B-V2-020: Public `PLAY`, quick pairing, and setup should initiate EvenChess search/match contracts.

REQ-B-V2-021: Public rated games must use EvenChess ECR/MMR and not ordinary Lichess rating pools.

REQ-B-V2-022: Existing Lichess feature areas may remain available under EvenChess branding when safe.

REQ-B-V2-023: Ordinary Lichess mechanics may remain internally available for regression and development, but must not confuse public rated EvenChess behavior.

## B.5 Patch discipline

REQ-B-V2-030: Every upstream seam must include: file, reason, requirement link, risk, tests, rollback note, and whether the change can later be isolated.

REQ-B-V2-031: Codex must not use a separate route/page where the requirement says native Lichess flow must be adapted.

REQ-B-V2-032: Any previous addon-style implementation must be identified and either removed, adapted, or documented as temporary.

<!-- END FILE: APPENDIX_B_V2_SEWN_IN_LICHESS_ARCHITECTURE.md -->

---

<!-- BEGIN FILE: APPENDIX_C_V2_LICHESS_PROVIDED_CAPABILITIES_AND_BOUNDARIES.md -->

# Appendix C — Lichess-Provided Capabilities and Boundaries

## C.1 Purpose

This appendix prevents rebuilding mature Lichess capabilities while still allowing EvenChess to replace public rating/search/coaching behavior.

## C.2 Lichess-provided, do not rebuild

REQ-C-V2-001: Do not rebuild legal move generation.

REQ-C-V2-002: Do not rebuild the base board UI.

REQ-C-V2-003: Do not rebuild clocks or live round state.

REQ-C-V2-004: Do not rebuild base accounts, login, logout, session, or profile foundations.

REQ-C-V2-005: Do not rebuild normal game lifecycle, move persistence, result handling, PGN, replay, or analysis foundations.

REQ-C-V2-006: Do not rebuild base puzzle, study, opening explorer, or analysis infrastructure.

REQ-C-V2-007: Do not rebuild WebSocket plumbing where lila/lila-ws already handles live updates.

## C.3 EvenChess-owned replacements/adaptations

REQ-C-V2-010: Replace public rated matchmaking/rating behavior with EvenChess MMR/ECR behavior.

REQ-C-V2-011: Adapt the Lichess setup modal and quick-search cards to collect EvenChess-specific controls.

REQ-C-V2-012: Adapt round/board UI to render EvenChess Display Engine overlays and fixed cards.

REQ-C-V2-013: Adapt analysis/review/study/opening surfaces to support EvenChess levels, review modes, and ECE history where allowed.

REQ-C-V2-014: Adapt settings/account/top-bar areas to expose EvenChess settings and token balances.

REQ-C-V2-015: On desktop round surfaces, the EvenChess eval bar height must track the rendered chess board height exactly. The level-selection card must sit under the native game metadata card when that side card is present, share its side-column width, bottom-align with the board, and scroll internally instead of stretching from native move-table rows or viewport height.

## C.4 Boundary tests

REQ-C-V2-020: Tests must prove ordinary Lichess board/game mechanics still function after EvenChess integration.

REQ-C-V2-021: Tests must prove public EvenChess play does not accidentally use normal Lichess rating pools as ECR.

REQ-C-V2-022: Tests must prove EvenChess controls appear native to Lichess surfaces rather than detached pages.

<!-- END FILE: APPENDIX_C_V2_LICHESS_PROVIDED_CAPABILITIES_AND_BOUNDARIES.md -->

---

<!-- BEGIN FILE: APPENDIX_D_V2_BRANDING_THEME_HOME_LANDING_AND_NAVIGATION.md -->

# Appendix D — Branding, Theme, Home, Landing, and Navigation

## D.1 Purpose

This appendix defines the public presentation of EvenChess as a deep-blue Lichess-powered product.

## D.2 Product identity

REQ-D-V2-001: Public wordmark and visible site identity should say EvenChess.

REQ-D-V2-002: Replace public-facing "Lichess" identity with "EvenChess" where appropriate for product branding.

REQ-D-V2-003: Keep legally required source, attribution, or internal references where needed; do not falsify licensing/source context.

REQ-D-V2-004: The product should retain the familiar Lichess layout and interaction style.

## D.3 Deep-blue theme

REQ-D-V2-010: EvenChess theme should be polished, modern, and deep blue.

REQ-D-V2-011: Theme changes must not break board readability, contrast, accessibility, or native Lichess layout behavior.

REQ-D-V2-012: Coaching overlays must remain visually distinct from board/piece colors.

REQ-D-V2-013: Color cannot be the only signal; icons/text/shape must supplement color-coded states.

## D.4 Homepage

REQ-D-V2-020: The homepage should briefly explain EvenChess without becoming a full landing page.

REQ-D-V2-021: Homepage summary should state that EvenChess is chess where platform coaching is disclosed, capped, logged, and reflected in rating.

REQ-D-V2-022: Homepage should include a short "what is EvenChess" section in the same theme as the site.

REQ-D-V2-023: Detailed marketing explanation belongs on a landing/detail page, not the compact homepage summary.

REQ-D-V2-024: On the main lobby homepage, the compact EvenChess summary card should sit adjacent to the primary quick-pairing/lobby tabs on wide screens, while launch-page Donate and Swag Store support cards should not be rendered in the main content grid.

## D.5 Navigation

REQ-D-V2-030: Top navigation should preserve Lichess-style navigation and expose compatible feature areas: play, puzzles, study, analysis, openings, account/settings, and community/profile where retained.

REQ-D-V2-031: Public `PLAY` entry should start EvenChess play/setup/search, not ordinary Lichess rated search.

REQ-D-V2-032: Token balances must integrate into the top bar or a native account-area surface.

REQ-D-V2-033: Clicking game-token balance must navigate to the screen for earning/watching ads for game tokens.

## D.6 Landing page

REQ-D-V2-040: A more detailed landing page should explain the assisted variant, fairness model, Set Level, ECR, tokens, subscriptions, and what is prohibited.

REQ-D-V2-041: Landing copy must not imply cheating, hidden engine use, or stronger paid live help.

<!-- END FILE: APPENDIX_D_V2_BRANDING_THEME_HOME_LANDING_AND_NAVIGATION.md -->

---

<!-- BEGIN FILE: APPENDIX_E_V2_ACCOUNTS_TOKENS_TOP_BAR_AND_MONETISATION.md -->

# Appendix E — Accounts, Tokens, Top Bar, and Monetisation

## E.1 Purpose

This appendix defines account/token/subscription requirements integrated into Lichess-style account and top-bar surfaces.

## E.2 Account model

REQ-E-V2-001: Use Lichess account foundations where possible.

REQ-E-V2-002: EvenChess account state must include ECR/MMR records, token balances, subscriptions, review rights, saved-game slots, custom analysis tokens, and settings.

REQ-E-V2-003: Same email/account anti-duplication and onboarding-token eligibility rules from prior amendments remain active unless superseded.

## E.3 Token balances

REQ-E-V2-010: Game-token balance must be visible in a top-bar/account-native location.

REQ-E-V2-011: Clicking game-token balance must take the player to the token/ad screen.

REQ-E-V2-012: Summary/review/custom-analysis tokens should be visible in account/subscription/review surfaces, not necessarily crowded into the top bar.

REQ-E-V2-013: Token displays must not imply tokens buy stronger live rated help.

REQ-E-V2-014: Account, token, rewarded-token, plan, and summary pages must use deployment-ready user-facing labels. They must not expose provider callback names, analytics event identifiers, internal entitlement source keys, raw token reason keys, dead checkout links, or debug/scaffold copy.

## E.4 Game tokens

REQ-E-V2-020: A game token is consumed only when a valid game starts and passes the meaningful-play threshold.

REQ-E-V2-021: Failed queue/search must not consume a token.

REQ-E-V2-022: Opponent abort before meaningful play must not consume or must immediately refund the token.

REQ-E-V2-023: Standard/Premium access may bypass ad-supported game-token limits according to subscription rules, but must not affect rated fairness.

REQ-E-V2-024: Admins may configure a bounded launch free-match-token window. While active, rated/casual EvenChess match starts must not consume startup, onboarding, or rewarded-ad game tokens, and must not require ad watching for access; failed searches still consume nothing. This waiver must not affect Set Level, Used Level, ECR settlement, or rated fairness.

## E.5 Review and custom analysis tokens

REQ-E-V2-030: Custom ECE analysis at high levels, especially L10 for both sides, may require custom analysis tokens.

REQ-E-V2-031: Full-game ECE review may require a match-review or full-analysis token.

REQ-E-V2-032: Live ECE outputs produced during a game are recorded as part of the game history and do not require separate custom analysis tokens.

REQ-E-V2-033: Reanalysis at custom levels may consume tokens according to the Review appendix.

## E.6 Saved games and paid tier storage

REQ-E-V2-040: The system retains a rolling default of each user's last 10 completed games with attached ECE history.

REQ-E-V2-041: Paid tiers may allow users to mark games as saved so they persist beyond normal retention.

REQ-E-V2-042: A game saved while the account is paid should remain saved if the user later downgrades, but new saves may require an active eligible tier.

REQ-E-V2-043: Exact free/paid saved-game counts are a product decision and must be configurable.

<!-- END FILE: APPENDIX_E_V2_ACCOUNTS_TOKENS_TOP_BAR_AND_MONETISATION.md -->

---

<!-- BEGIN FILE: APPENDIX_F_V2_SETTINGS_AND_ADMIN_CONTROLS.md -->

# Appendix F — EvenChess Settings and Admin Controls

## F.1 Purpose

This appendix defines user-facing and admin/backend settings sewn into Lichess settings/admin surfaces.

## F.2 User settings

REQ-F-V2-001: Add an EvenChess section to the normal settings/preferences flow.

REQ-F-V2-002: Settings should preserve the Lichess-style layout and not become a detached standalone settings site.

REQ-F-V2-003: User settings should include default live coaching display preferences, overlay visibility, card behavior, review defaults, proposed-move button behavior, TTS options if enabled, and accessibility preferences.

REQ-F-V2-004: Live rated permissions are server-owned. User settings may request display preferences but cannot grant stronger assistance than policy allows.

REQ-F-V2-005: User settings must include a Preferred starting Used Level. Each new game initializes the player's level selector, feature toggles, and starting Used Level from this preference, capped by that game's Set Level.

REQ-F-V2-005A: User settings must include preferred starting feature toggles for every EvenChess live display feature available in the level selector. These saved toggle defaults are applied when a new game initializes from Preferred starting Used Level and whenever the in-game "Apply up to" dropdown is used. Features above the selected/applied level remain off, and missing stored toggle values default to on for backward compatibility.

REQ-F-V2-006: Preferred starting Used Level is a starting display/request preference only. It cannot raise the game's Set Level, and once a game has started, lowering visible selections or toggles must not reduce the retained Used Level.

## F.3 Admin/backend settings

REQ-F-V2-010: Admin controls should include ECE provider status, AI provider status, Stockfish status, feature flags, token/ad settings, subscription toggles, review-token settings, retention settings, and safety kill switches.

REQ-F-V2-011: Secrets must be configured server-side or environment-side and must not be exposed to the client.

REQ-F-V2-012: Admin toggles must not silently alter rated fairness; any fairness-affecting toggle requires audit and versioning.

REQ-F-V2-013: Bot matchmaking fallback controls must be admin-only and include enabled state, rated/casual/both scope, and fallback timeout. Public search surfaces may show only deployment-safe ON/OFF disclosure and wait-time labels.

REQ-F-V2-014: Bot simulation controls must be admin-only and include enabled/running state, rated/casual/both scope, bot count, target rating range, Set Level range, persona/timing mix, active ticket count, and a stop path that clears local simulation tickets.

REQ-F-V2-014A: Bot simulation admin controls must use operator-readable wording such as simulated players, queue refill, game types, bot style, rating range, and Set Level range. The UI must not expose internal-only terms such as seed or duplicate the same settings form. Each control must provide hover/help text explaining what it changes.

REQ-F-V2-014B: Matchmaking fallback bots and simulation bots must use one shared bulk bot-account roster controlled from the admin bot-operations page. The roster UI must support large lists, including the generated 1000-name local roster, and must not require entry through a tiny single-line text field.

REQ-F-V2-015: The local EvenChess Test Ground may embed or link the admin bot-operations panel for development control, but must not expose unauthenticated bot start/stop/settings APIs.

REQ-F-V2-016: Admin controls must include an ECE rating calibration / ECOR panel behind the existing Lichess Settings permission. The panel must display and allow admin adjustment of the active ECOR gap table, the equivalent rating-to-base-level table, calibration recommendations, stored sample counts, and timestamped snapshots for rollback.

REQ-F-V2-017: Admin controls must display and allow adjustment of the Stockfish AI equivalent rating table used for platform-bot handoff. This table maps Lichess AI/Stockfish levels SF1-SF8 to approximate rating windows and must be validated before activation.

REQ-F-V2-018: Admin controls must include enabled, starts-at, and ends-at settings for the launch free-match-token window. The window must validate that an enabled range has parseable dates and an end after the start, expose no secret values, and provide only deployment-safe public copy.

## F.4 Setting persistence

REQ-F-V2-020: User EvenChess settings must persist per account.

REQ-F-V2-021: Live game state must include a snapshot of relevant display/coaching settings where needed for audit/replay.

REQ-F-V2-022: Review mode may allow temporary overrides without changing the user's default settings.

<!-- END FILE: APPENDIX_F_V2_SETTINGS_AND_ADMIN_CONTROLS.md -->

---

<!-- BEGIN FILE: APPENDIX_G_V2_PLAY_SETUP_QUICK_SEARCH_AND_MATCHMAKING_UI.md -->

# Appendix G — Play Setup, Quick Search, and Matchmaking UI

## G.1 Purpose

This appendix defines how EvenChess search controls are sewn into the native Lichess play/setup experience.

## G.2 Native flow

REQ-G-V2-001: Public play/search must use the native Lichess-style setup modal and quick-search cards where feasible.

REQ-G-V2-002: Do not route users to a separate EvenChess search page if the native Lichess modal/card can be adapted.

REQ-G-V2-003: The adapted UI must look like Lichess, with deep-blue EvenChess styling and native spacing.

## G.3 Required setup controls

REQ-G-V2-010: Setup/search must include time control selection using native Lichess-style controls.

REQ-G-V2-011: Setup/search must allow normal rated/casual search with no fixed Set Level preference; in that state the MMR Engine assigns both players' Set Levels in the match contract.

REQ-G-V2-012: Setup/search must include exactly one Preferred Set Level dropdown for public rated/casual EvenChess search. The dropdown supports Any plus L0-L10.

REQ-G-V2-013: Preferred Set Level = Any means normal search with no level preference. Preferred Set Level = L0-L10 means the searching player's own Set Level is fixed/preferred at that value for the MMR Engine.

REQ-G-V2-014: Setup/search must not expose Opponent Target Level, Both Target Level, manual rating-window controls, or Search until preferences are met controls for public EvenChess search.

REQ-G-V2-015: Setup/search must disclose that platform coaching is allowed only because it is disclosed, capped, logged, and rated into ECR.

REQ-G-V2-016: Setup/search must show token/subscription gate state where a game token or subscription is required.

REQ-G-V2-017: When the launch free-match-token window is active for rated/casual EvenChess search, the setup/search card must display the deployment-safe message "Tokens are temporarily free" near the token/access copy.

## G.4 Search modes

REQ-G-V2-020: Normal search means the player chooses time control and Preferred Set Level = Any, then EvenChess finds a fair match using the level-offset table to assign both players' Set Levels.

REQ-G-V2-021: Preferred Set Level search fixes or strongly prefers the searching player's own Set Level and lets the MMR Engine assign the opponent's Set Level from the rating difference and level-offset table.

REQ-G-V2-022: If two players independently enter the pool with their own Preferred Set Level values and those fixed levels cannot satisfy the effective-rating target, the game may still start but the contract must mark an uneven match.

REQ-G-V2-023: The public UI must disclose only deployment-safe search status, assigned levels, and any uneven-match notice needed for fairness. It must not expose internal offset tables, raw ticket ids, or private MMR diagnostics.

REQ-G-V2-024: Search widening is controlled by server-side MMR windows and level-offset tables, not by user-visible strict-preference controls.

REQ-G-V2-025: Applying rated/casual level preferences must not route the search into the separate Target Level queue; rated preferences stay rated and casual preferences stay casual.

REQ-G-V2-026: Manual rating-window controls must be removed from the EvenChess public search card because allowable windows are derived from the active level-offset table.

REQ-G-V2-027: Challenge-a-friend setup must not route through the generic public EvenChess search queue. It must submit through the native friend-challenge path or an explicit friend-contract path that carries the recipient.

REQ-G-V2-028: Challenge-a-friend setup must expose one Level Setup dropdown with: Auto level, Set my level, Set opponent level, and Set both levels. Set my level reveals a My level dropdown L0-L10; Set opponent level reveals an Opponent level dropdown L0-L10; Set both levels reveals both.

REQ-G-V2-029: A friend challenge request card must show the resulting EvenChess level settings in deployment-safe terms before the recipient accepts.

REQ-G-V2-029A: Starting a public rated/casual EvenChess search must not show a separate EvenChess debug/status card by default. The search surface should continue to look like native Lichess searching, using the remembered setup-card settings and native searching indicator. Verbose EvenChess search diagnostics may be available only through an explicit local/debug flag.

REQ-G-V2-029B: Native quick-pairing cards must start an EvenChess search contract using the clicked clock bucket plus the user's remembered public EvenChess search settings. They must not submit an ordinary native Lichess pool ticket for authenticated public EvenChess play.

REQ-G-V2-029C: Native Create lobby game submits that map to an existing quick-pairing card must show the same native pending/searching quick-pairing animation as direct quick-card searches. The modal must not simply disappear back to an idle lobby while an EvenChess search ticket is active.

REQ-G-V2-029D: Public EvenChess search tickets must retain the exact selected real-time clock limit and increment for native Lichess game creation while still using the broader time-control bucket for EvenChess MMR pooling. A 3+2, 5+0, or 10+5 search must create the same clocked game when matched with a human, matchmaking fallback bot, or simulation bot.

## G.5 Confirmation

REQ-G-V2-030: Before game start or at game start, both players must know the match contract: time control, each side's Set Level, rated/casual state, and relevant fairness disclosure.

REQ-G-V2-031: The game-start display should use native Lichess confirmation/status patterns.

REQ-G-V2-032: Public search status and browser-facing search JSON must show only deployment-safe user-facing information such as queue/status text, assigned levels, preference labels, access labels, bot disclosure, and wait time. It must not expose ticket ids, internal pool keys, expected-offset model values, player ids, request ids, policy versions, match-contract source/stage diagnostics, bot seed diagnostics, raw token gate keys, or internal MMR/ECR calculation fields.

<!-- END FILE: APPENDIX_G_V2_PLAY_SETUP_QUICK_SEARCH_AND_MATCHMAKING_UI.md -->

---

<!-- BEGIN FILE: APPENDIX_H_V2_MMR_ENGINE_AND_MATCH_CONTRACTS.md -->

# Appendix H — EvenChess MMR Engine and Match Contracts

## H.1 Purpose

This appendix defines the EvenChess MMR Engine and how it replaces ordinary public Lichess matchmaking behavior while using Lichess for the game shell.

## H.2 Architecture

REQ-H-V2-001: EvenChess public matchmaking must be owned by the EvenChess MMR Engine, not ordinary Lichess rating pools.

REQ-H-V2-002: The MMR Engine should output a match contract, not merely an equivalent Lichess rating.

REQ-H-V2-003: Lichess should receive the finalized pairing/game contract and provide board, clocks, game lifecycle, live state, and result mechanics.

REQ-H-V2-004: Any use of existing Lichess pairing infrastructure must be adapted through EvenChess search contracts and must not mix normal ratings with ECR.

## H.3 Match contract fields

A match contract should include:

- game id / request id;
- time control;
- rated/casual flag;
- white player id;
- black player id;
- white ECR/MMR;
- black ECR/MMR;
- white Set Level;
- black Set Level;
- white expected offset;
- black expected offset;
- white effective rating;
- black effective rating;
- match-quality score;
- preference-match flags;
- widened search flag;
- uneven-match flag/reason where applicable;
- token/subscription gate result;
- policy version.

REQ-H-V2-010: Effective rating is `ECR + expected offset` for matchmaking.

REQ-H-V2-011: Expected offset is based on assigned/planned Set Level and calibration model, not actual Used Level after the game.

REQ-H-V2-012: Used Offset after the game may differ from expected offset and is used for rating settlement/calibration.

## H.4 Two search scenarios

REQ-H-V2-020: Scenario 1 - Normal search: Preferred Set Level = Any. The engine searches normally while deriving allowable rating windows from the active L0-L10 level-offset table and assigning both players' Set Levels in the match contract.

REQ-H-V2-021: Scenario 2 - Preferred own Set Level: Preferred Set Level = L0-L10. The searching player's own Set Level is fixed/preferred at that value, and the engine searches opponent candidates plus opponent Set Level assignments that create a fair effective-rating match.

REQ-H-V2-022: Opponent Target Level, Both Target Level, Search until preferences are met, and manual rating-window preference controls are superseded and must not be exposed in public rated/casual EvenChess search.

REQ-H-V2-023: If two players independently enter the pool with fixed/preferred own Set Levels and those levels cannot satisfy the effective-rating target, the game may still start, but the match contract must mark `unevenMatch = true` with a deployment-safe reason.

REQ-H-V2-024: For rated/casual search, MMR simulation must evaluate candidate Set Level assignments from L0-L10 using the active two-state preference model rather than treating a UI default Set Level as a fixed constraint.

REQ-H-V2-025: Game-start policy persistence must use the Set Levels assigned by the MMR match contract, not the queued ticket defaults or UI hint values, before any live coaching can render.

REQ-H-V2-025A: Round bootstrap JSON must expose the server-persisted per-player EvenChess game policy to the player display. The browser must initialize Set Level from that persisted game policy, not from account defaults or fallback L10 values. Policy lookup must tolerate username/UserId case normalization so a stored contract for `Admin` still resolves for round user id `admin`. Used Level starts from the account Preferred starting Used Level capped by Set Level; when the account has no explicit preferred starting Used Level, the game starts at Used Level L0.

## H.5 Search widening

REQ-H-V2-030: Public EvenChess search widening is server-side and table-driven. The browser must not send strict-search, opponent-target, both-target, or manual rating-window controls.

REQ-H-V2-031: Widening may expand effective-rating tolerance over time, but it must not mutate a concrete Preferred Set Level chosen by the searching player.

REQ-H-V2-032: Raw rating windows and effective-rating compatibility are derived from the active level-offset table. No-preference search may consider the requester and opponent across L0-L10; preferred search fixes the requester's own preferred Set Level and ranges the opponent across L0-L10.

REQ-H-V2-033: Widened or uneven search outcomes must be visible only through deployment-safe status text and match-contract flags, without exposing internal offset tables, ticket ids, or MMR diagnostics.

REQ-H-V2-034: The MMR Engine must use centralized, versioned level-offset and lower-player rating-to-base-level tables for search windows, Set Level assignment, match-contract policy labels, and recalibration.

REQ-H-V2-035: The active level-offset table is the EvenChess Offset Ratings Table (ECOR Table). It stores one rating-point value per adjacent level gap from L0-L1 through L9-L10. Any level-vs-level effective offset is computed as the sum of the relevant adjacent gaps.

REQ-H-V2-036: The active equivalent rating-to-base-level table maps ECR bands to a recommended base Set Level for the lower-rated player. The initial best-guess table is: <675=L0, 675-799=L1, 800-924=L2, 925-1049=L3, 1050-1179=L4, 1180-1324=L5, 1325-1499=L6, 1500-1699=L7, 1700-1899=L8, 1900-2099=L9, >=2100=L10.

## H.6 Anti-abuse

REQ-H-V2-040: Repeated pairings, collusion patterns, abort abuse, and level-target manipulation must be monitored.

REQ-H-V2-041: Match contracts must be logged for audit and calibration.

REQ-H-V2-042: The MMR Engine must support simulation tests before production rollout.

REQ-H-V2-043: Simulation mode must seed synthetic bot tickets through the same server-side search repository and MMR contract pipeline as human and fallback-bot tickets. It must not create a separate client-side matchmaking path.

REQ-H-V2-044: Simulation-mode bots must be removable as a distinct ticket family without deleting human search records. Monitoring should expose active simulation tickets and potential bot-vs-bot contracts.

REQ-H-V2-044A: Simulation-mode bot tickets must use the admin-configured rating spread as their active ECR values, not default provisional ratings. Their Stockfish/Lichess AI level must be derived from that target rating through the active Stockfish equivalent rating table.

REQ-H-V2-044B: When simulation mode is running, repeated seeding must maintain the requested active bot count. If a simulation ticket is consumed by a match or the admin changes the count and starts/runs simulation again, ECL must remove stale simulation tickets and recreate missing desired tickets without duplicating active bot population.

REQ-H-V2-044C: Simulation-mode bot tickets must be distributed across supported playable time-control pools, weighted toward common quick-search pools such as Blitz and Rapid. When the simulation scope is rated and casual, each mode must receive playable time-control coverage instead of accidentally assigning a time-control only to one mode. Simulation mode must not seed all synthetic players into one default pool, because a visible global bot count is not enough for a human quick search to find a same-pool candidate.

REQ-H-V2-044D: Simulation-mode admin controls must expose explicit time-control family checkboxes. Simulated players may only enter selected time-control pools, and human searches must only match same-pool simulated players through the normal server-side search/MMR repository.

REQ-H-V2-044E: Simulation and fallback bot matches must require roster-backed real local bot accounts. When a matched bot ticket is backed by a configured account, ECL must hand the match to the normal human challenge/game creation path so the round shell, usernames, clocks, and game behaviour match ordinary human-vs-human EvenChess games. ECL must not create generated non-roster matchmaking or simulation bot games that render as native Stockfish/computer games.

REQ-H-V2-044F: Matchmaking fallback mode and simulation mode are mutually exclusive operator modes. Starting matchmaking fallback must stop simulation mode and clear simulation tickets; starting or refilling simulation mode must turn matchmaking fallback off. Both modes must read the same shared bot-account roster.

REQ-H-V2-044G: Bot operations must provide an admin-only provisioning/check action for the shared bot-account roster. The action must create missing roster usernames as normal local/staging user accounts with private generated passwords, leave existing accounts unchanged, and report disabled or BOT-titled accounts that would prevent human-style rendering. Matchmaking fallback and simulation must not depend on manual database edits before roster-backed bot games can be tested.

REQ-H-V2-044H: Roster-backed matchmaking and simulation bots must actively play through the normal Lichess round move path after a match is created. ECL may use a server-side managed bot runner for local/staging bot accounts, but it must submit only legal moves through existing round actors, preserve both clocks, use the bot profile's timing policy for human-like delay, and avoid native AI/computer-game presentation.

REQ-H-V2-044I: Simulation mode must not only seed synthetic search tickets. While simulation mode is running, ECL must be able to convert valid sim-vs-sim MMR contracts into normal roster-account games through the same human-style challenge/game path used for human-vs-bot fallback. Created sim-vs-sim games must retire their matched simulation tickets, register both roster-backed sides with the managed legal-move runner, preserve normal clocks, and avoid native Stockfish/computer-game presentation.

REQ-H-V2-044I.1: When ECL attaches a managed runner to a roster-backed matchmaking or simulation bot, it must also mark that bot side as connected through Lichess's native round bot-presence mechanism. Bot-backed games must not show "opponent left" or offline/gone prompts while the managed runner is active, and sim-vs-sim games must keep both roster-backed sides registered independently.

REQ-H-V2-044J: Simulation seeding must distribute synthetic users across selected rated/casual and time-control pools while still creating same-pool cohorts close enough in effective rating to match. Small simulation batches must seed each selected pool center-out across the configured rating range so a typical mid-rating human search can find a simulated player instead of only seeing extreme-rating candidates. A simulation run that fills the queue but produces no possible sim-vs-sim contracts is not acceptable for launch-fill or stress-test purposes.

REQ-H-V2-044K: Human searches have priority over the simulation bot-vs-bot pump. When a human search is polling, ECL must evaluate and create the human match redirect before pumping sim-vs-sim games from the remaining queue. The sim-vs-sim pump must be throttled and must not repeatedly recreate the same ticket pair while a simulation revision is active.

REQ-H-V2-044L: Roster-backed matchmaking and simulation accounts must have established, non-provisional, non-round normal Lichess display ratings before a game is created. The visible game/user rating must align with the bot profile target rating closely enough for testing and must not show default new-account values such as `1500?`.

REQ-H-V2-045: Public EvenChess search must keep a stable server-side search ticket across browser polling through an opaque public search key. Polling must not create a new MMR ticket, reset the waiting timer, expose the raw internal ticket id, or allow the browser to decide matchmaking.

REQ-H-V2-046: When a waiting human search receives a platform-bot match contract, ECL must only create a game if the matched bot ticket is backed by a configured real bot account. The game handoff must use the normal human challenge/game creation path and return only a same-origin game redirect to the browser. The MMR ticket and matched bot ticket must be retired after the game handoff so repeated polling cannot create duplicate games. If no roster-backed bot account is available, bot fallback must keep the user in search instead of creating a native Stockfish/computer game.

REQ-H-V2-046A: The admin-editable Stockfish equivalent rating table may still inform the bot runner's target playing strength, but ECL matchmaking fallback must not hand public searches to native Lichess AI/computer setup. Bot strength and timing are properties of the configured bot account runner, while ECL owns the search ticket, match contract, Set Level assignment, and game policy persistence.

REQ-H-V2-046B: After a roster-backed bot or sim-bot match is accepted through the native challenge/game path, ECL must attach the managed bot runner from the accepted game object before returning the human redirect, publish native bot presence for the bot side, and return native full player URLs for matched human players rather than watcher URLs. Bot-backed games must therefore render with normal human-vs-human round data, both clocks, exact selected real-time clock settings, and no native Stockfish/computer label.

REQ-H-V2-047: When two human EvenChess search tickets receive a valid MMR match contract, ECL must hand that finalized contract into native Lichess game creation, preserve the contract-assigned colors and rated/casual mode, persist the EvenChess game policy before coaching may render, cache same-origin redirects for both tickets, and retire both tickets so repeated polling cannot create duplicate games.

REQ-H-V2-048: Friend-level contracts are not public queue searches. Auto level assigns the lower-rated player from the active rating-to-base-level table and assigns the other player through the active ECOR table. Set my level fixes the challenger level and assigns the recipient through ECOR. Set opponent level fixes the recipient level and assigns the challenger through ECOR. Set both levels fixes both selected levels and may mark the contract as uneven when the effective-rating gap remains outside the fairness target.

<!-- END FILE: APPENDIX_H_V2_MMR_ENGINE_AND_MATCH_CONTRACTS.md -->

---

<!-- BEGIN FILE: APPENDIX_I_V2_ECR_RATING_SETTLEMENT_AND_CALIBRATION.md -->

# Appendix I — ECR Rating Settlement and Calibration

## I.1 Purpose

This appendix defines EvenChess ECR/MMR and rating settlement.

## I.2 ECR definition

REQ-I-V2-001: ECR estimates underlying human skill after accounting for platform assistance.

REQ-I-V2-002: ECR is distinct from normal Lichess rating and must not be mixed with normal Lichess public rated pools.

REQ-I-V2-003: Separate pools may exist by time-control category if launched.

## I.3 Matchmaking vs settlement

REQ-I-V2-010: Matchmaking uses expected offset based on Set Level and calibration assumptions.

REQ-I-V2-011: Rating settlement uses actual game outcome and actual Used Level / Assistance Load / Used Offset.

REQ-I-V2-012: Used Level is the highest level actually delivered/consumed and never decreases during the game.

REQ-I-V2-013: If the player raises level mid-game and ECE returns higher-level output, the game must record the higher Used Level.

REQ-I-V2-014: Rated EvenChess settlement must calculate expected score from each player's ECR plus their actual Used Offset for that game. Higher Used Level / Used Offset increases that player's effective rating input, so normal ECR gain/loss pressure accounts for assistance used without touching normal Lichess ratings.

## I.4 Calibration

REQ-I-V2-020: Calibration must track residuals by ECR band, Set Level, Used Level, Assistance Load, Used Offset, time control, feature mix, and candidate follow-rate where available.

REQ-I-V2-021: Initial offset values may be heuristic but must be versioned and recalibrated.

REQ-I-V2-022: Changes to offset tables/models must be policy-versioned.

REQ-I-V2-023: Rating settlement must record pre/post rating snapshots and model version.

REQ-I-V2-024: ECL must collect rating-calibration samples from rated EvenChess game settlement. Each sample must retain the five calibration variables required for ECOR review: both player ratings, both player levels, and the result. The runtime collector must retain the latest 1,000,000 samples by game id for calibration analysis.

REQ-I-V2-025: The admin ECOR calibration run must calculate recommended adjacent level-gap values from retained samples and report sample counts, informative sample counts, residual mean, residual standard deviation, mean absolute residual, support per gap, and deltas versus the active ECOR table.

REQ-I-V2-026: Applying ECOR recommendations is an explicit admin decision. Calibration runs must not silently change live matchmaking tables. Any applied or manually edited ECOR table must be policy-versioned and snapshotted so admins can restore a previous table date.

<!-- END FILE: APPENDIX_I_V2_ECR_RATING_SETTLEMENT_AND_CALIBRATION.md -->

---

<!-- BEGIN FILE: APPENDIX_J_V2_ECE_ENGINE_REQUIREMENTS.md -->

# Appendix J — EvenChess-Lichess to EvenChessEngine Integration Contract

**Document status:** Version 2 replacement Appendix J for the EvenChess-Lichess requirements suite
**Scope:** How EvenChess-Lichess talks to the separate EvenChessEngine service
**Does not define:** Internal EvenChessEngine implementation requirements
**Primary consumer:** Codex working inside `foughtapple/EvenChess-Lichess`

---

## J.1 Purpose

EvenChess-Lichess must treat EvenChessEngine, shortened to **ECE**, as a separate private server-side engine service.

EvenChess-Lichess must not reimplement ECE chess logic in the Lichess fork.

EvenChess-Lichess is responsible for:

- knowing the game;
- knowing the players;
- knowing the current FEN;
- knowing each side's authorised EvenChess level;
- knowing match/rating context;
- deciding when ECE should be called;
- auditing coaching render/use;
- displaying only server-authorised payloads.

ECE is responsible for:

- receiving a known input from EvenChess-Lichess;
- running deterministic chess/provider calculations;
- applying level-gating to White and Black side payloads;
- returning side-specific payloads for EvenChess-Lichess to display, cache, audit, or ignore.

---

## J.2 Local ECE Address on This PC

For current local development, EvenChessEngine runs as a local HTTP service.

Default local base URL:

```text
http://127.0.0.1:8787
```

Current health check:

```http
GET http://127.0.0.1:8787/health
```

Current board-state endpoints:

```http
POST http://127.0.0.1:8787/v1/ece/board/quick
POST http://127.0.0.1:8787/v1/ece/board/deep
POST http://127.0.0.1:8787/v1/ece/board   (legacy compatibility only)
Content-Type: application/json
```

The ECE service may allow the host and port to be changed by environment variables later, but EvenChess-Lichess local development should default to:

```text
ECE_BASE_URL=http://127.0.0.1:8787
```

EvenChess-Lichess must never call ECE from browser/client code. ECE calls must be server-to-server only.

---

## J.3 Integration Boundary

EvenChess-Lichess may call ECE from backend/server code only.

Browser clients must not receive:

- ECE provider URLs;
- ECE internal provider configuration;
- API keys;
- Stockfish provider paths;
- Syzygy tablebase paths;
- Maia model paths;
- Lichess eval-cache paths;
- raw ECE provider output;
- unrestricted engine output.

EvenChess-Lichess should receive ECE output through its own backend, then decide what to expose to the browser based on:

- match contract;
- Set Level;
- Used Level;
- game mode;
- audit state;
- player side;
- payload validity.

---

## J.4 Required ECE Health Check

EvenChess-Lichess local startup/status tooling should verify ECE is reachable before live ECE overlays are expected to work.

Request:

```http
GET http://127.0.0.1:8787/health
```

Expected successful response shape:

```json
{
  "status": "ok",
  "service": "EvenChessEngine",
  "mode": "mock",
  "openai_configured": false,
  "stockfish_configured": false
}
```

EvenChess-Lichess must treat missing/unreachable ECE as non-fatal for ordinary Lichess foundation behavior.

If ECE is unavailable:

- ordinary page/game loading must not break;
- EvenChess live coaching payloads should be unavailable;
- UI may show coaching unavailable;
- diagnostics/logs should identify ECE unreachable.

---

## J.5 Board-State Call

EvenChess-Lichess calls board-state mode when it needs a coaching/overlay payload for a current board state.

Current endpoints:

```http
POST http://127.0.0.1:8787/v1/ece/board/quick
POST http://127.0.0.1:8787/v1/ece/board/deep
Content-Type: application/json
```

EvenChess-Lichess must call the quick endpoint first and may render the accepted quick side payload immediately.
When either side's authorised level requires deep/provider-backed output, EvenChess-Lichess sets `deep_requested = true`, records the returned quick context, and calls the deep endpoint only after the quick response says deep is ready to request.

Request body:

```json
{
  "request": {
    "mode": "board_state",
    "request_id": "ec_game_123_ply_18_quick",
    "input_fen": "nbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
    "rating_type": "ecr",
    "white_rating_input": 1000,
    "black_rating_input": 1200,
    "white_level": 4,
    "black_level": 8,
    "use_ai": 0,
    "deep_requested": true,
    "requested_deep_modules": ["stockfish", "lichess_eval_cache"],
    "custom": {
      "opening": 0,
      "instructions": 0
    }
  }
}
```

---

## J.6 Board-State Request Field Rules

| Field | EvenChess-Lichess responsibility |
|---|---|
| `request.mode` | Send `board_state`. May be omitted only if ECE default is stable, but explicit is preferred. |
| `request.request_id` | Generate a unique ID tied to game, ply/FEN, mode, and render request. |
| `request.input_fen` | Send the current authoritative FEN known to the EvenChess-Lichess server. |
| `request.rating_type` | Send `ecr` for normal EvenChess rated games, or `unknown` if not available. |
| `request.white_rating_input` | Send White's ECR/rating context, or `0` if unknown. |
| `request.black_rating_input` | Send Black's ECR/rating context, or `0` if unknown. |
| `request.white_level` | Send White's authorised Set Level/current allowed level for this position. |
| `request.black_level` | Send Black's authorised Set Level/current allowed level for this position. |
| `request.use_ai` | Send `1` only if EvenChess policy allows AI text for this mode and ECE is configured. Otherwise send `0`. |
| `request.deep_requested` | Send `true` on quick requests when either side's authorised level/profile requires deep/provider-backed addenda. |
| `request.requested_deep_modules` | Optional server-side allow-list for deep modules such as `stockfish`, `syzygy`, `lichess_eval_cache`, `maia`, `ai_text`, and `opening_book`. |
| `request.custom.opening` | Send `0` unless the user/session has requested a specific opening. |
| `request.custom.instructions` | Send `0` unless server-approved custom wording instructions exist. |

EvenChess-Lichess must not send API keys to ECE in the request body.

Deep request body:

```json
{
  "request": {
    "mode": "board_deep",
    "request_id": "ec_game_123_ply_18_deep",
    "quick_request_id": "ec_game_123_ply_18_quick",
    "quick_context_id": "ece_ctx_abc123",
    "input_fen": "nbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
    "white_level": 4,
    "black_level": 8,
    "use_ai": 0,
    "requested_deep_modules": ["stockfish", "lichess_eval_cache"]
  }
}
```

---

## J.7 Board-State Response Contract Expected by EvenChess-Lichess

ECE board-state response must be treated as side-gated output.

Expected public response shape:

```json
{
  "schema": {},
  "request_echo": {},
  "quick_context": {},
  "side_outputs": {
    "white": {},
    "black": {}
  },
  "diagnostics": {},
  "unavailable": {}
}
```

ECE board-state response must not include:

```json
{
  "position": {},
  "shared_calculations": {}
}
```

Quick response reason:

- EvenChess-Lichess already has the FEN;
- side to move is derived from the FEN;
- ECE should return only what each side is allowed to receive;
- shared chess facts must not bypass level gating;
- all displayable facts must be mapped into `side_outputs.white` and `side_outputs.black`.

Deep response shape:

```json
{
  "schema": {
    "phase": "deep"
  },
  "request_echo": {},
  "quick_context": {},
  "side_output_addenda": {
    "white": {},
    "black": {}
  },
  "diagnostics": {},
  "unavailable": {}
}
```

EvenChess-Lichess may merge `side_output_addenda.<side>` into the already accepted quick side payload only after the deep request echo, quick context, FEN, levels, current game state, and diagnostics checks all pass.

---

## J.8 Side Output Use by EvenChess-Lichess

Each side payload is already level-gated by ECE.

EvenChess-Lichess must still enforce server-side display authority.

Expected side shape:

```json
{
  "side": "white",
  "student_side": "white",
  "opponent_side": "black",
  "level": {
    "requested_level": 4,
    "delivered_level": 4,
    "defaulted": false
  },
  "is_side_to_move": true,
  "summary": 0,
  "immediate_waning": 0,
  "plan": 0,
  "candidate_moves": 0,
  "evaluation": 0,
  "opening": 0,
  "overlays": {
    "trade_status": {
      "hanging_not_attackable": [],
      "offset_count": [],
      "advantage_offset_value": 0,
      "disadvantage_offset_value": 0
    },
    "threats": {
      "student_threats": [],
      "opponent_threats": []
    },
    "pinned_pieces": {
      "student_pinned": [],
      "opponent_pinned": []
    }
  },
  "raw_deterministic": {
    "summary_inputs": [],
    "modules_used": []
  }
}
```

EvenChess-Lichess must display a player's payload from that player's side output only.

Examples:

- White player display uses `side_outputs.white`.
- Black player display uses `side_outputs.black`.
- Review surfaces may show one or both side outputs depending on review mode and entitlement.
- Live opponent payload must not be shown to a player unless the game/review mode explicitly allows it.

---

## J.9 Stale Payload Rejection

ECE no longer needs to return a public `position` object.

EvenChess-Lichess must reject stale ECE payloads using its own request tracking.

Minimum checks before using an ECE response:

1. `request_echo.request_id` matches the outstanding ECE request.
2. `request_echo.input_fen` equals the FEN used for that request.
3. `request_echo.white_level` and `request_echo.black_level` match the authorised levels used for that request.
4. The current game state still matches the FEN/request context.
5. `diagnostics.status` is acceptable for display.

Recommended cache key:

```text
mode + input_fen + white_level + black_level + use_ai + custom.opening + custom.instructions + deep_requested + requested_deep_modules + engine_version
```

EvenChess-Lichess may hash this cache key internally.

---

## J.10 Diagnostics Handling

ECE may return these statuses:

| Status | EvenChess-Lichess action |
|---|---|
| `ok` | Payload may be used if request validation checks pass. |
| `partial` | Payload may be used, but UI/logs may show degraded provider status. |
| `invalid_request` | Do not display coaching. Log request-shape issue. |
| `invalid_fen` | Do not display coaching. Log FEN issue. |
| `invalid_game` | Applies to full-game mode. Do not display full-game review. |
| `stockfish_unavailable` | Use lower-level deterministic payload if present; otherwise show unavailable. |
| `ai_unavailable` | Use deterministic fallback text if present; otherwise show no AI text. |
| `internal_error` | Do not display coaching. Log ECE failure. |

EvenChess-Lichess must not treat ECE diagnostics as browser-safe by default. Sanitize before showing to users.

---

## J.11 Live Game Board-State Timing

EvenChess-Lichess may call ECE:

- after a move is committed;
- when the board FEN changes;
- when a player opens/requests coaching;
- when review mode needs a stored/saved payload;
- when cache is missing or stale.

EvenChess-Lichess should avoid unnecessary repeated calls for identical inputs.

ECE calls must not block the legal move lifecycle, clocks, or core game state.

If ECE is slow or unavailable:

- game play continues;
- coaching payload can arrive later or be skipped;
- stale checks must prevent old payloads from rendering on a new position.

---

## J.12 Proposed-Move Integration

Proposed-move mode is for a player asking about one proposed move from the current FEN.

Target future endpoint:

```http
POST http://127.0.0.1:8787/v1/ece/proposed-move
Content-Type: application/json
```

If ECE instead supports proposed-move mode through a unified endpoint, EvenChess-Lichess must follow the ECE API version contract.

Request shape:

```json
{
  "request": {
    "mode": "proposed_move",
    "request_id": "ec_game_123_ply_18_pm_1",
    "input_fen": "string",
    "proposed_move_uci": "g1f3",
    "rating_type": "ecr",
    "white_rating_input": 1000,
    "black_rating_input": 1200,
    "white_level": 10,
    "black_level": 10,
    "use_ai": 0,
    "custom": {
      "opening": 0,
      "instructions": 0
    }
  }
}
```

EvenChess-Lichess must call proposed-move mode only when:

- there is exactly one proposed move;
- that move comes from one user-drawn green board arrow with an origin and destination;
- the ECL client has checked the current legal-destination map and the arrow is a legal move;
- the move belongs to the side to move;
- the requester's live/review context allows proposed-move help;
- the current FEN has not changed.

ECE derives the moving side from FEN.

Proposed-move usage is platform-owned, not ECE-owned:

- Levels whose ECE profile exposes no potential/proposed help must not call proposed-move mode.
- Under the current ECE contract recommendations, proposed-move calls are per-game: L0-L4 have zero calls, L5 has one call, L6-L7 have two calls, and L8-L10 have three calls.
- A consumed proposed-move call is keyed by game, ply, current FEN, player side, Used Level, and UCI move, but the total limit is counted across the game.
- Re-requesting the same proposed move for the same turn uses the cached preview and must not consume another call.
- Pressing Proposed Move again while that same cached preview is active hides the preview and restores the normal saved board-state display; pressing it again re-shows the cached preview without consuming another call.
- Pressing Proposed Move when the drawn input is not exactly one legal green move must not replace, clear, or otherwise change the currently displayed board-state/proposed preview. The UI may show a status message, but only a legal single green arrow may switch the overlay into a proposed-move preview.
- After one proposed-move call is consumed for a turn, a different arrow on that same turn is rejected until the board changes.
- Raising Used Level to a proposed-move-eligible level enables the proposed-move button without lowering any retained Used Level state.
- The public returned text must be coaching advice about what the proposed move changes or what to inspect; it must not be reduced to a bare legal/illegal status message.

Proposed-move display requirements:

- Proposed-move output is preview output and must be visually separate from actual-position board-state output.
- The preview text appears under the EvenChess Coach in its own card.
- For legal ECE proposed-move responses, ECL must read deterministic post-move display data from `proposed_move_evaluation.after_move_side_output`.
- `after_move_side_output` is nested under `proposed_move_evaluation`; it is not a top-level `side_outputs` object and must not be treated as a second board-state response or shared/internal calculation.
- `after_move_side_output` is populated only for legal proposed moves. If it is `0`, missing, illegal, or invalid, ECL must not render post-move overlay cards or visuals from it.
- The nested side output is for the original proposing/moving side and is built from `proposed_move_evaluation.new_fen` after the proposed move has already been applied; ECL must not require `after_move_side_output.is_side_to_move` to be true.
- ECL should use `proposed_move_evaluation.sentence` or `proposed_move_evaluation.coaching.text` for proposed-move advice and use `proposed_move_evaluation.new_fen` as the post-move display board-state key when rendering the nested deterministic payload.
- If a legal proposed-move response includes valid `after_move_side_output` but omits `new_fen`, ECL must still expose a stable post-move preview key to the browser so normalized cards/visuals are not dropped. This fallback key is preview-only identity metadata and must not be treated as a canonical ECE board-state FEN.
- Proposed-move text-card audit identity may differ from the normalized post-move cards/visuals audit identity. When rendering the post-move overlay branch, ECL must validate child post-move cards/visuals against the nested post-move display payload audit identity instead of suppressing them because the outer proposed-call audit id differs.
- When a legal proposed-move response includes post-move deterministic data, ECL caches both the normal current-position board-state payload and the proposed post-move cards/visuals for that turn.
- While the proposed preview is active, the board and coach display the cached post-move cards/visuals. Pressing Proposed Move again restores the cached normal board-state payload; pressing it again re-shows the cached proposed preview without consuming another call.
- Removing or changing the green arrow clears the preview card and restores the normal saved board-state payload display.
- Playing a move or changing FEN clears the active proposed-move preview.
- Browser/client code must call only the ECL same-origin proposed-move bridge; ECL calls ECE server-to-server.

Potential-move display requirements:

- Potential moves are separate from proposed moves. Potential moves are board-state payload artifacts; proposed moves are explicit caller-supplied what-if requests.
- ECE may send up to three potential moves for each side when either side's delivered level is high enough; ECL owns reveal buttons, per-game reveal limits, and Assistance Load accounting.
- Opponent Potential Moves must reveal the opponent side's delivered side-output/perspective, not the requester/player side-output. For example, when White requests opponent potentials, the reveal is built from Black's potential moves while remaining authorized and counted against White's opponent-potential reveal allowance.
- ECL must not automatically display potential-move arrows just because they are present in the payload. A matching potential-move reveal button must be active for the current game, FEN, side, and Used Level.
- Potential-move display data must not be exposed to the browser by the normal board-state bridge before a server-authorized reveal. The board-state bridge must strip or suppress potential/candidate display cards and visuals, and the browser may receive them only from a same-origin ECL reveal endpoint after ECL authorizes the reveal or returns an already-authorized cached reveal.
- L5+ allows a button to reveal opponent potential moves.
- L6+ allows a button on the user's turn to reveal the user's own potential moves.
- User-turn potential reveals are consumable by game: L6 one reveal, L7 two reveals, and L8-L10 three reveals. Opponent-potential reveals are also per-game and level-gated by ECL policy. Raising Used Level increases the remaining maximum but never lowers the retained Used Level.
- Potential-move and proposed-move consumed counts must be enforced server-side by ECL, keyed at least by game, player side, Used Level, ply/FEN, and reveal/proposed-move identity as appropriate. Browser refresh, tab reopen, or local client-state reset must not restore spent uses.
- Revealing the same current-position potential set again may use already-authorized browser cache and must not consume a second reveal; pressing the active reveal button again hides the arrows, and pressing it again re-shows the cached reveal without a new use.
- Playing a move or changing FEN clears the active/cached browser potential reveal for that position, while the server-side consumed counts remain retained for the game.
- Active potential moves should be listed as concise text at the bottom of the EvenChess Coach card in addition to any board arrows, so the player can read the current authorized potential set without relying only on board graphics.
- Candidate terminology in older payload fields maps to Potential Moves in ECL UI and requirements.

---

## J.13 Full-Game Review Integration

Full-game mode is for post-game EvenChess Match History generation.

Target endpoint:

```http
POST http://127.0.0.1:8787/v1/ece/full-match
Content-Type: application/json
```

`POST /v1/ece/game-review` is a legacy-compatible alias during migration.

Input should provide enough data for ECE to process the game move by move.

Preferred request shape:

```json
{
  "request": {
    "mode": "full_match",
    "request_id": "ec_game_123_review_1",
    "game": {
      "game_id": "ec_game_123",
      "initial_fen": "startpos",
      "pgn": "optional PGN",
      "moves": [],
      "fen_history": [],
      "result": "1-0",
      "termination": "checkmate/resignation/timeout/draw/unknown"
    },
    "rating_type": "ecr",
    "white_rating_input": 1000,
    "black_rating_input": 1200,
    "review_level": 10,
    "use_ai": 0,
    "custom": {
      "opening": 0,
      "instructions": 0
    },
    "live_ece_snapshots": []
  }
}
```

EvenChess-Lichess must treat full-game output as post-game review only.

Full-match AI summary endpoint:

```http
POST http://127.0.0.1:8787/v1/ece/full-match-summary
Content-Type: application/json
```

ECL must send `mode=full_match_summary` and a canonical `full_match` object in `evenchess_full_game` format. ECL must not send AI instructions, raw prompts, provider settings, or browser-generated entitlement truth.

Full-game ECE output must not alter:

- game result;
- live Used Level;
- live Assistance Load;
- live Used Offset;
- ECR;
- matchmaking state.

---

## J.14 Provider Awareness

EvenChess-Lichess does not call Stockfish, Syzygy, Maia, opening-book, rules, or Lichess eval-cache providers directly for ECE coaching.

EvenChess-Lichess calls ECE.

ECE may then call its local providers:

- Stockfish provider;
- Syzygy tablebase provider;
- opening-book provider;
- Lichess eval-cache provider;
- rules/legal-move provider;
- Maia human-risk provider;
- AI text provider.

EvenChess-Lichess may display provider-derived output only after ECE normalizes and level-gates it into side outputs.

---

## J.15 Security Requirements

EvenChess-Lichess must not:

- expose ECE directly to browser clients;
- put ECE provider paths in frontend code;
- put API keys in ECE request payloads;
- trust client-supplied level values;
- trust client-supplied ECE payloads;
- let browser code decide coaching permission;
- let ECE output override match contract or fairness state.

EvenChess-Lichess must:

- derive levels server-side from the EvenChess match contract/settings;
- audit coaching render/use;
- check stale payloads;
- sanitize diagnostics before user display;
- keep ECE calls server-side.

---

## J.16 Local Developer Commands

Start ECE from the sibling Linux EvenChessEngine repo:

```bash
cd /home/jayde/dev/lila-docker/repos/ece
bash scripts/start-ece-linux.sh
```

Health check:

```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:8787/health
```

Sample quick board-state call:

```bash
curl -X POST http://127.0.0.1:8787/v1/ece/board/quick \
  -H "Content-Type: application/json" \
  --data @fixtures/ece-v1-sample-input.json
```

Sample deep board-state call:

```bash
curl -X POST http://127.0.0.1:8787/v1/ece/board/deep \
  -H "Content-Type: application/json" \
  --data '{"request":{"mode":"board_deep","request_id":"sample_deep","quick_request_id":"sample_quick","quick_context_id":"ece_ctx_from_quick","input_fen":"nbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1","white_level":4,"black_level":8,"use_ai":0,"requested_deep_modules":["stockfish","lichess_eval_cache"]}}'
```

---

## J.17 Acceptance Criteria

1. EvenChess-Lichess calls ECE server-to-server only.
2. Local default ECE base URL is `http://127.0.0.1:8787`.
3. EvenChess-Lichess health/status tooling can check `GET /health`.
4. EvenChess-Lichess can call `POST /v1/ece/board/quick` and conditionally call `POST /v1/ece/board/deep`.
5. EvenChess-Lichess sends authoritative FEN, ratings, levels, and AI flag.
6. EvenChess-Lichess does not send API keys to ECE.
7. EvenChess-Lichess expects side-gated quick output under `side_outputs.white` and `side_outputs.black`, and side-gated deep addenda under `side_output_addenda.white` and `side_output_addenda.black`.
8. EvenChess-Lichess does not expect public `position`.
9. EvenChess-Lichess does not expect public `shared_calculations`.
10. EvenChess-Lichess rejects stale quick/deep ECE payloads using request ID, quick context ID, FEN echo, level echo, diagnostics, and current game state.
11. EvenChess-Lichess does not call Stockfish/Syzygy/Maia/opening/eval-cache providers directly for coaching.
12. EvenChess-Lichess can degrade gracefully if ECE is unavailable.

<!-- END FILE: APPENDIX_J_V2_ECE_ENGINE_REQUIREMENTS.md -->

---

# APPENDIX K — Missing from uploaded set

**Status:** `APPENDIX K` was not included in the uploaded Markdown files available for this combined document.

Add the relevant appendix file and regenerate this combined document if this appendix is required as part of the single source of truth.

---

<!-- BEGIN FILE: APPENDIX_L_V2_LIVE_GAME_ASSISTANCE_AND_ECE_HISTORY.md -->

# Appendix L — Live Game Assistance and ECE History

## L.1 Purpose

This appendix defines live ECE usage, Used Level handling, and per-turn ECE history.

## L.2 Live ECE calls

REQ-L-V2-001: After each live move, the server must request or schedule ECE board-state output for the new FEN according to each player's current authorized level.

REQ-L-V2-002: The ECE payload returned to UI must be split by side and must not expose the opponent's unauthorized private view.

REQ-L-V2-003: If a player raises their live level within allowed policy, ECE must be called again for that position at the higher level.

REQ-L-V2-004: If a lower-level result and higher-level result exist for the same turn/player, the higher-level result is canonical for Used Level and review history.

REQ-L-V2-005: Lowering visible level does not reduce Used Level for that game.

REQ-L-V2-006: Live display level selections and per-feature toggles persist for the duration of the game. New ECE payloads must not reset manual dropdown or toggle choices.

REQ-L-V2-007: The "Apply up to" dropdown is only a bulk toggle tool: it applies the user's saved default toggle profile to eligible features at or below the selected level, disables features above it, and keeps retained Used Level monotonic. If no saved default exists for a feature, that feature is treated as on for backward compatibility.

REQ-L-V2-008: Live games at L6+ may show the existing Lichess WikiBook fieldset above the level-selection card. The fieldset must be controlled by a Level 6 feature toggle, derive its opening line from the current board/move history, reuse the analysis-board WikiBook markup/classes/behavior, support the same click/keyboard collapse behavior as analysis, provide a readable bounded open height with internal WikiBook-text scrolling, allow the same public WikiBooks API connection already used by analysis pages, remain visibly mounted with an empty-state message when no WikiBook extract exists for the current line, update as the live move history changes, and hide when that toggle is off without lowering retained Used Level.

## L.3 ECE history model

REQ-L-V2-010: Each game should store an EvenChess game history record containing FEN and ECE output metadata per ply/turn.

REQ-L-V2-011: The stored history should identify side-to-move, levels requested, levels delivered, position hash, policy version, ECE version, and output references.

REQ-L-V2-012: Full raw ECE history may be retained only according to retention/token/subscription policy.

REQ-L-V2-013: If storage must be limited, store enough to reconstruct: FEN list, moves, highest level used per side/per turn, summary/plan text, overlay essentials, and audit atoms.

REQ-L-V2-014: On each live board-state change, ECL must first check the current game's retained/cacheable ECE payload history by game, side, FEN/position key, and requested Used Level before calling ECE again.

REQ-L-V2-015: If a cached payload exists for the same game, side, and FEN at an equal or higher delivered/Used Level, ECL should apply that sanitized cached payload without recalculating. If the player has raised Used Level beyond the cached payload level, ECL must call ECE again at the higher level and replace/upgrade the cached payload for that FEN.

REQ-L-V2-016: Live match history stores only the canonical approved display payload for each game, side, FEN, and ply. Proposed-move preview payloads are excluded. If multiple board-state payloads exist for the same frame, the highest delivered level wins and is the frame used by replay/history.

## L.4 Live display

REQ-L-V2-020: Live display uses the player's side and current authorized view.

REQ-L-V2-021: Live display must not allow side switching.

REQ-L-V2-022: Live display must clearly distinguish actual position output from proposed-move preview output.

REQ-L-V2-023: Payloads that arrive stale must be discarded or marked stale and must not be displayed as current advice.

REQ-L-V2-024: ECE board-state payloads may be requested and applied after every move so board overlays/history stay current, but the player-facing live coach text snapshot must refresh only from a non-stale payload when it is that player's turn to move.

REQ-L-V2-025: Payloads received while it is the opponent's turn must not replace the local player's visible coach text. They may still update board overlays, audit/history state, and cached payload data.

REQ-L-V2-026: The live coach text turn check must use the authoritative round active-player state when available, not only chessground's transient turn state, so accepted current-position ECE payloads render after move/update timing settles. Local/computer rounds must schedule non-duplicate follow-up board-state checks after position changes so an engine reply that changes FEN shortly after the player's move cannot leave only the fallback shell visible.

REQ-L-V2-027: Live overlay refreshes should be visually stable. During move-triggered ECE refresh, the round UI must preserve stable shell/card/board-overlay DOM keys and may retain the last accepted safe coach text, eval state, and board visual layer as a clearly internal transition layer while the next payload is loading. Stale ECE responses must still be rejected as current advice, but a move-triggered clear must not blank the previously accepted visual layer before the replacement current-position payload arrives.

<!-- END FILE: APPENDIX_L_V2_LIVE_GAME_ASSISTANCE_AND_ECE_HISTORY.md -->

---

<!-- BEGIN FILE: APPENDIX_M_V2_REVIEW_ANALYSIS_CUSTOM_LEVELS_AND_FULL_GAME_ECE.md -->

# Appendix M — Review, Analysis, Custom Levels, and Full-Game ECE

## M.1 Purpose

This appendix defines replay/review analysis modes using stored live ECE history and optional custom/full-game ECE analysis.

## M.2 Live review modes

REQ-M-V2-001: Review must support Live White: show White's saved live perception at the level actually used on each turn.

REQ-M-V2-002: Review must support Live Black: show Black's saved live perception at the level actually used on each turn.

REQ-M-V2-003: Review must support Live Both: switch perception based on whose turn it was.

REQ-M-V2-004: Live review modes use saved ECE history where available and should not consume custom analysis tokens.

REQ-M-V2-005: When a player steps through move history for a game with retained live payloads, ECL should request/apply the cached payload for that FEN/ply/side even while the round controller is in replay mode. Cached replay payloads must still pass game, ply, FEN, side, level, audit, and safety checks before display.

## M.3 Custom review modes

REQ-M-V2-010: Review must support Custom mode with selectable White level and Black level.

REQ-M-V2-011: Custom mode must support viewing from White, Black, or Both/side-to-move perspective.

REQ-M-V2-012: Custom mode may require custom ECE analysis tokens, especially if levels exceed live stored outputs or use L10/AI.

REQ-M-V2-013: Custom mode must cache generated custom analysis by game, White level, Black level, perspective mode, ECE version, policy version, and AI flag.

## M.4 Full-game ECE mode

REQ-M-V2-020: Full-game ECE review consumes whole-game input and produces a level-10-capable review for both sides unless the user explicitly requests different White/Black analysis levels.

REQ-M-V2-021: Full-game input should support PGN plus FEN/move history plus any saved live ECE outputs.

REQ-M-V2-022: Full-game output may include game summary, player performance summary, tuning points, recurring motifs, missed threats, and level-10 overlay/review data by move.

REQ-M-V2-023: Full-game review should use deterministic ECE facts and at most one large AI call for narrative compression where feasible.

REQ-M-V2-024: Full-game review requires token/quota checks and must not retroactively alter the rated game's live Used Level or ECR settlement.

REQ-M-V2-025: Requested full-game/custom analyses are retained as a rolling default of the user's last 100 requested analyses.

REQ-M-V2-026: A user reviewing saved match history may request a level-10 full-match review. ECL must call ECE server-to-server through `/v1/ece/full-match`, accept canonical `evenchess_full_game.turns[]` output, convert `turns[].ece_payload.side_outputs.<side>` into the same approved live-overlay frame format, and attach those frames to the saved game history without mutating live Used Level, settlement, result, or matchmaking state. `/v1/ece/game-review` may be accepted only as a legacy-compatible ECE alias during migration.

REQ-M-V2-026A: Full-match AI summaries must use `/v1/ece/full-match-summary` with `mode=full_match_summary` and a canonical `full_match` object in the `evenchess_full_game` format. ECL must not send AI instructions, raw prompts, provider settings, or browser-generated entitlement truth in the summary request.

REQ-M-V2-026B: Review, match-summary, and performance-summary request controls must show a visible processing indicator while a long-running server-side summary/review request is in flight, and must prevent duplicate submissions until that request resolves or fails.

REQ-M-V2-027: The ECL/ECE full-match payload format is documented in `docs/requirements/EVENCHESS_FULL_MATCH_PAYLOAD_CONTRACT.md`. If ECE output differs, ECL must reject or ignore unsafe/mismatched frames rather than fabricating review payloads.

## M.5 Saved games

REQ-M-V2-030: Users may save review games according to tier/storage rules.

REQ-M-V2-031: Paid saved games remain saved after downgrade, but new saves may require an active eligible tier.

REQ-M-V2-032: Retention limits must be visible and configurable.

REQ-M-V2-033: Review and analysis surfaces use the same EvenChess overlay shell, level controls, display toggles, and coach card as live and computer-game board surfaces; only the payload source and Set Level authority differ.

<!-- END FILE: APPENDIX_M_V2_REVIEW_ANALYSIS_CUSTOM_LEVELS_AND_FULL_GAME_ECE.md -->

---

<!-- BEGIN FILE: APPENDIX_N_V2_COACHING_LADDER_OVERLAY_AND_TEXT_LEVEL_GATES.md -->

# Appendix N — Coaching Ladder, Overlay, and Text Level Gates

## N.1 Purpose

This appendix consolidates level-gating rules for display and ECE output.

## N.2 Level gates

| Level | Name | Live display behavior |
|---|---|---|
| L0 | Standard Board | No coaching; normal board only. |
| L1 | Rules | Rule/legal state only where surfaced. |
| L2 | Safety | Hanging/loose pieces, basic danger. |
| L3 | Offset Count | Exchange Resolver / take-take-take overlays. |
| L4 | Pattern Coach | Threats, pins, motifs, short Summary/Plan. |
| L5 | Potential Moves | Opponent potential-move reveal and one proposed-move call per game. |
| L6 | Choice Coach | User-turn potential-move reveal, two proposed-move calls per game, first user potential reveal, and optional live WikiBook fieldset reusing the existing Lichess analysis-board WikiBook feature. |
| L7 | Guided Engine | Additional potential reveal capacity and compact plan cue. |
| L8 | Precision | Numeric eval/WDL/proof where available. |
| L9 | Expert Sparring | Why-not/branch comparison/proposed-move depth. |
| L10 | Full Co-pilot | Maximum compact specificity; no text flood. |

## N.3 Board Summary

REQ-N-V2-001: Board Summary must be a short paragraph tuned to level.

REQ-N-V2-002: Board Summary may include board situation, hanging pieces, threats, pins, potential forks/combinations, exchange wanings, and other deterministic facts allowed by level.

REQ-N-V2-003: Board Summary must name pieces and squares clearly when useful, for example "the knight on e4 is hanging".

REQ-N-V2-004: Board Summary must not become a long engine report.

## N.4 Plan

REQ-N-V2-010: Plan should describe the medium-term aim over several moves.

REQ-N-V2-011: Plan must be level-gated and compact.

REQ-N-V2-012: Plan should differ from Summary: Summary is current/near-term state; Plan is what to aim for.

## N.5 Overlay tuning

REQ-N-V2-020: Each overlay must have clear level gating.

REQ-N-V2-021: The same raw ECE fact may appear differently by level.

REQ-N-V2-022: Higher levels may increase specificity but must not overload the board.

REQ-N-V2-023: Numeric eval text begins at L8 and must render as a compact horizontal strip at the top of the EvenChess Coach card when the Eval Text toggle is enabled and the current payload contains eval/WDL/precision data. The strip uses the player's perspective, blue for near-equal positions, and traffic-light colours for better/worse evaluations.

REQ-N-V2-023A: Stockfish numeric eval used by the board eval bar and coach eval status strip must update only from an accepted deep/advanced ECE payload. Quick board payload eval values are ignored for these UI surfaces because quick-phase eval may be a placeholder value such as zero. When ECE returns the normalized evaluation object, ECL must prefer `score.cp_white` / `score.mate_white` for the fixed White-up/Black-down eval bar; `score.cp` is side-output-relative and is only a fallback for legacy payloads. The eval bar must map centipawns with a nonlinear win-probability-style curve rather than a linear material scale, saturate mate scores toward the winning side, and retain the last accepted live-game eval until a new accepted eval payload arrives. The coach eval strip may invert the fixed White-positive value into the viewer/player perspective for better/worse status text. Proposed-move preview mode keeps its own cached post-move eval from `proposed_move_evaluation.eval_after` or `after_move_side_output.evaluation` so toggling the preview switches smoothly between current-FEN and proposed-FEN eval states; if a proposed preview has no accepted proposed eval yet, ECL keeps showing the live current-FEN eval instead of snapping to neutral.

<!-- END FILE: APPENDIX_N_V2_COACHING_LADDER_OVERLAY_AND_TEXT_LEVEL_GATES.md -->

---

<!-- BEGIN FILE: APPENDIX_O_V2_OFFSET_COUNT_HANGING_THREATS_AND_PINS.md -->

# Appendix O — Offset Count, Hanging Pieces, Threats, and Pins

## O.1 Purpose

This appendix defines the core deterministic overlays that make the EvenChess board useful.

## O.2 Offset Count

REQ-O-V2-001: Offset Count is the Exchange Resolver / take-take-take feature.

REQ-O-V2-002: Offset Count overlay is based on piece-count exchange units: each captured piece counts as 1.

REQ-O-V2-003: Equal result displays as a blue circle with a white shield in the top-right of the square.

REQ-O-V2-004: Student-favorable result displays as a green circle with a white number in the top-right of the square.

REQ-O-V2-005: Student-unfavorable result displays as a red circle with a white number in the top-right of the square.

REQ-O-V2-006: Unknown/unavailable must not display as equal.

REQ-O-V2-006A: ECL must interpret Offset Count as a side-output perspective board-fact dataset, not as opponent-target-only data. At L3+, every offset item delivered by the selected side output must be eligible for display when the Offset Count toggle is on, including own pieces and opponent pieces.

REQ-O-V2-006B: ECL must use `target_square` as the marker square when present and must prefer signed `piece_count_delta` over legacy `value`, `count`, `offset`, or label fields. Positive values are favourable for the receiving side output, zero is equal/blue shield, and negative values are unfavourable for the receiving side output.

REQ-O-V2-006C: ECL should preserve backward compatibility with `student_side`, `student_first_move`, and legacy offset labels during migration, but must prefer `perspective_side`, `owner_side`, `attacking_side`, `first_capture_move`, `target_square`, and `piece_count_delta` when present.

REQ-O-V2-007: ECE must also calculate value-based offset for summary/waning text.

REQ-O-V2-008: Significant positive value offset creates `advantage_offset_value`.

REQ-O-V2-009: Significant negative value offset creates `disadvantage_offset_value`.

## O.3 Hanging pieces

REQ-O-V2-010: Hanging or loose piece that cannot currently be taken uses an orange circle with a white exclamation mark in the bottom-left of the square and no attackable rim.

REQ-O-V2-011: Student hanging piece that can currently be taken uses a red circle with a white exclamation mark in the bottom-left of the square plus a red inside-rim border.

REQ-O-V2-011A: Opponent hanging piece that can currently be taken uses a purple circle with a white exclamation mark in the bottom-left of the square plus a purple inside-rim border.

REQ-O-V2-012: ECE should separate `hanging_attackable` and `hanging_not_attackable`.

REQ-O-V2-013: Display Engine must map those categories into square markers without layout instability.

REQ-O-V2-014: Display Engine must preserve ownership for attackable hanging markers so student-owned attackable pieces and opponent-owned attackable pieces can render with different colors.

## O.4 Threat arrows

REQ-O-V2-020: Student/player threats are green dotted arrows from center square to center square.

REQ-O-V2-021: Opponent threats are red dotted arrows from center square to center square.

REQ-O-V2-022: Red arrows render above green arrows if overlapping.

REQ-O-V2-023: If red and green arrows overlap along the same path, both must remain visible through offset, dash pattern, opacity, or another accessible rendering technique.

REQ-O-V2-024: Threats are side-perspective outputs: student threats and opponent threats.

REQ-O-V2-025: Student threat arrows and opponent threat arrows must have independent display toggles.

## O.5 Pins

REQ-O-V2-030: Pinned pieces display a pin/lock symbol in the top-left of the pinned square.

REQ-O-V2-031: Pin payload should include pinned square, pinning piece square, target behind the pinned piece, pin line, and pin type.

REQ-O-V2-032: Pins feed Offset Count legality and Summary/Plan where allowed.

## O.6 Coach-side visual duplication

REQ-O-V2-033: Board overlay visual atoms must not be duplicated as coach-side text chips or labels. The coach side displays coach-card text and explicit controls only; board visuals are represented on the board itself to avoid layout jumping and repeated information.

<!-- END FILE: APPENDIX_O_V2_OFFSET_COUNT_HANGING_THREATS_AND_PINS.md -->

---

<!-- BEGIN FILE: APPENDIX_P_V2_STOCKFISH_AI_AND_TTS_POLICY.md -->

# Appendix P — Stockfish, AI, and TTS Policy

## P.1 Purpose

This appendix defines policy for engine help, AI text, and optional Text-to-Speech.

## P.2 Stockfish

REQ-P-V2-001: Stockfish remains server-side/ECE-side only.

REQ-P-V2-002: Clients must never receive unrestricted raw engine access.

REQ-P-V2-003: Potential-move counts are level-gated and caller-revealed: older candidate fields map to Potential Moves; ECL controls reveal quotas and must not auto-display them solely because the payload contains them.

REQ-P-V2-003A: Potential-move and proposed-move usage counts are ECL server-side game assistance state. Browser/client state may only mirror the latest server count and must not be able to reset usage by refresh or tab reopen.

REQ-P-V2-003B: Potential-move display payloads are server-authorized reveal payloads, not ordinary always-visible board-state payloads. Normal ECL board-state responses to the browser must suppress potential/candidate display data until an authorized reveal endpoint returns it for the current game/FEN/side/Used Level.

REQ-P-V2-004: Numeric eval begins at L8 unless explicitly changed later.

REQ-P-V2-005: All Stockfish profiles must be bounded by depth/nodes/movetime/MultiPV/time-control-independent config where applicable.

## P.3 AI

REQ-P-V2-010: AI is an explainer/compressor of deterministic/ECE/Stockfish truth packets.

REQ-P-V2-011: AI must not invent chess facts or bypass level gates.

REQ-P-V2-012: Each board-state ECE call may use at most one AI call where enabled.

REQ-P-V2-013: Full-game ECE mode may use one larger AI call for narrative compression where feasible.

REQ-P-V2-014: AI credentials must be server-side only.

REQ-P-V2-015: AI output must be validated and fall back deterministically on failure.

## P.4 TTS

REQ-P-V2-020: TTS Coach, if enabled, may read only server-authorized Summary/Plan or review text.

REQ-P-V2-021: TTS must not generate new coaching content separate from authorized text.

REQ-P-V2-022: TTS settings must live in EvenChess settings and be off/configurable according to product decision.

REQ-P-V2-023: Live coach cards must expose a visible manual Speak control whenever a displayable coach text card exists. The control must read only the currently displayed, server-authorized, audit-linked text, must use the configured TTS voice/rate/volume/queue/mute settings, and must be disabled with an explanatory reason when TTS is off or the payload is unsafe/unauthorized.

REQ-P-V2-024: EvenChess settings must include an auto-read toggle and a configurable auto-read delay in seconds. When enabled, newly received coach text payloads must be spoken after the configured delay, respecting the same TTS safety checks and existing mute-during-opponent-turn behavior. Auto-read must not repeat the same audit/text payload on redraw.

<!-- END FILE: APPENDIX_P_V2_STOCKFISH_AI_AND_TTS_POLICY.md -->

---

<!-- BEGIN FILE: APPENDIX_Q_V2_PUZZLES_STUDY_OPENINGS_ANALYSIS_AND_COMPUTER_PLAY.md -->

# Appendix Q — Puzzles, Study, Openings, Analysis, and Computer Play

## Q.1 Purpose

This appendix defines how existing Lichess feature areas remain available under EvenChess branding and gain EvenChess coaching where safe.

## Q.2 Puzzles

REQ-Q-V2-001: Use Lichess puzzle foundations; do not rebuild puzzles.

REQ-Q-V2-002: EvenChess overlays may be available in puzzle/training contexts according to settings and product rules.

REQ-Q-V2-003: Puzzle rating, if shown, must not be confused with ECR unless explicitly integrated later.

REQ-Q-V2-004: Non-live board surfaces, including puzzles, analysis, study, opening PGN viewers, and comparable training/review boards, use Set Level 10 for EvenChess overlay availability by default. Browser code may request same-origin ECL overlay hydration for the current FEN/side, but ECL must force the non-live Set Level server-side and must continue to call ECE server-to-server only.

REQ-Q-V2-005: Puzzle/training pages must integrate EvenChess board markers on the puzzle board, EvenChess level-selection controls with per-feature toggles, and EvenChess coach cards in the native puzzle tools column. This integration must not float over the puzzle controls, must not block puzzle move input, must let toggles hide/show matching board and coach payload features, and must consume only same-origin ECL overlay payloads.

## Q.3 Study

REQ-Q-V2-010: Use Lichess study foundations.

REQ-Q-V2-011: EvenChess overlays/cards may be available on study boards according to selected review/training level.

REQ-Q-V2-012: Study usage must not affect live rated ECR.

## Q.4 Opening explorer

REQ-Q-V2-020: Use Lichess opening explorer/book foundations where available.

REQ-Q-V2-021: ECE opening option may use requested opening input and current FEN to provide allowed book guidance.

REQ-Q-V2-022: Opening guidance should be compact and level-gated.

REQ-Q-V2-023: When reused on live round surfaces, the Lichess/WikiBooks opening explanation must be treated as a Level 6 display feature, shown in the EvenChess side shell, driven by the current live board's SAN move history rather than by ECE internals or browser-direct ECE calls, and presented with analysis-style collapse and scroll behavior.

## Q.5 Analysis/replay

REQ-Q-V2-030: Use Lichess analysis/replay foundations.

REQ-Q-V2-031: EvenChess review modes must layer ECE history/custom ECE output onto the analysis surface.

## Q.6 Computer play

REQ-Q-V2-040: Use Lichess computer-play foundations where feasible.

REQ-Q-V2-041: Computer games are training/review by default unless a separate rated mode is explicitly defined.

REQ-Q-V2-042: Computer games may use EvenChess overlays but must not corrupt normal ECR.

<!-- END FILE: APPENDIX_Q_V2_PUZZLES_STUDY_OPENINGS_ANALYSIS_AND_COMPUTER_PLAY.md -->

---

<!-- BEGIN FILE: APPENDIX_R_V2_TELEMETRY_AUDIT_AND_DATA_RETENTION.md -->

# Appendix R — Telemetry, Audit, and Data Retention

## R.1 Purpose

This appendix defines events, audit records, and ECE history retention.

## R.2 Live audit events

REQ-R-V2-001: Audit match contracts.

REQ-R-V2-002: Audit Set Level at game start.

REQ-R-V2-003: Audit every Used Level increase.

REQ-R-V2-004: Audit each ECE payload generated for live play.

REQ-R-V2-005: Audit each coaching render, hide, expand, suppress, and proposed-move check.

REQ-R-V2-006: Audit final Used Level, Assistance Load, Used Offset, and rating settlement.

## R.3 Display events

REQ-R-V2-010: Display Engine should emit events for shown/hidden/expanded cards and overlays where feasible.

REQ-R-V2-011: Proposed move checks should record arrow move, legality, level, and whether result was shown.

## R.4 Retention

REQ-R-V2-020: Store enough game/ECE history to support Live White, Live Black, Live Both review modes.

REQ-R-V2-021: Retention is limited by rolling recent games, requested-analysis memory, and paid saved games; the default rolling limits are 10 recent games with live ECE history and 100 requested full-game/custom analyses per user.

REQ-R-V2-022: Privacy and storage controls must avoid keeping unnecessary raw AI/provider data.

REQ-R-V2-023: AI prompts/responses must be logged only according to safe diagnostic policy and must not contain secrets.

REQ-R-V2-024: If retained live ECE history is unavailable for a replayed game, the game has no existing payload-backed EvenChess overlays until the user requests analysis.

REQ-R-V2-025: Replay/analysis display toggles may change what is visible and may track a viewing-session Used Level, but must not mutate the original game's live Used Level, Assistance Load, Used Offset, ECR, result, or matchmaking state.

<!-- END FILE: APPENDIX_R_V2_TELEMETRY_AUDIT_AND_DATA_RETENTION.md -->

---

<!-- BEGIN FILE: APPENDIX_S_V2_ABUSE_FAIRNESS_AND_TRUST_CONTROLS.md -->

# Appendix S — Abuse, Fairness, and Trust Controls

## S.1 Purpose

This appendix defines abuse/fairness controls for a disclosed assisted chess platform.

## S.2 Non-platform help

REQ-S-V2-001: Rated EvenChess prohibits outside engines, humans, bots, browser extensions, unaudited notes, and stream chat.

REQ-S-V2-002: Public rules must explain that only platform-delivered coaching is legal.

## S.3 Matchmaking abuse

REQ-S-V2-010: Monitor repeat pairings, collusion, rating transfer, target-level manipulation, abort abuse, and queue sniping.

REQ-S-V2-011: MMR Engine must support repeat-opponent caps/flags.

REQ-S-V2-012: Preferred Set Level search and uneven-match handling must not become a collusion loophole.

## S.4 Token/review abuse

REQ-S-V2-020: Prevent token farming where practical without adding intrusive MVP controls unless approved.

REQ-S-V2-021: Custom analysis tokens should rate-limit high-cost L10/full-game analysis.

REQ-S-V2-022: Paid status must not change live rated fairness.

## S.5 Engine/AI abuse

REQ-S-V2-030: ECE and AI must reject/ignore custom instructions that request hidden, forbidden, or higher-level information.

REQ-S-V2-031: AI output must be validated for forbidden best-move wording where policy prohibits it.

REQ-S-V2-032: Stockfish raw output must not be exposed.

<!-- END FILE: APPENDIX_S_V2_ABUSE_FAIRNESS_AND_TRUST_CONTROLS.md -->

---

<!-- BEGIN FILE: APPENDIX_T_V2_OPERATIONS_DEPLOYMENT_AND_INCIDENT_RESPONSE.md -->

# Appendix T — Operations, Deployment, and Incident Response

## T.1 Purpose

This appendix defines operational controls for EvenChess V2.

## T.2 Feature flags

REQ-T-V2-001: Major EvenChess systems must be feature-flagged: MMR Engine, ECE live calls, Display Engine overlays, AI summaries, proposed-move mode, full-game mode, token gates, and ads.

REQ-T-V2-002: Feature flags must not silently change rated fairness without audit/versioning.

## T.3 Health checks

REQ-T-V2-010: Monitor ECE latency, Stockfish latency, AI latency/cost/fallback, queue times, token flow, custom analysis consumption, and rating settlement health.

REQ-T-V2-011: Monitor overlay stale-payload errors and Display Engine render failures.

## T.4 Incident handling

REQ-T-V2-020: Operators must be able to pause ECE live help, AI summaries, proposed-move analysis, custom review, ads, tokens, and paid promotions.

REQ-T-V2-021: Asymmetric assistance outages may require no-rate/annul/review flows.

REQ-T-V2-022: Campaigns must pause if copy implies cheating, hidden engine use, or pay-to-win help.

## T.5 Deployment

REQ-T-V2-030: Deployment must preserve local Lichess/lila-docker dev flow.

REQ-T-V2-031: Each phase must include rollback notes and tests.

<!-- END FILE: APPENDIX_T_V2_OPERATIONS_DEPLOYMENT_AND_INCIDENT_RESPONSE.md -->

---

<!-- BEGIN FILE: APPENDIX_U_V2_DATA_MODELS_AND_LICHESS_SEAMS.md -->

# Appendix U — Data Models and Lichess Seams

## U.1 Purpose

This appendix defines logical data models and likely Lichess seams.

## U.2 Logical models

Required logical models include:

- EvenChessAccountState;
- EvenChessRating/ECR;
- EvenChessSearchRequest;
- EvenChessMatchContract;
- EvenChessGameMetadata;
- SetLevelPolicy;
- UsedLevelState;
- AssistanceLoadSummary;
- EceBoardPayload;
- EceGameHistory;
- EceFullGameReview;
- DisplayRenderEvent;
- ProposedMoveCheck;
- TokenLedger;
- ReviewTokenLedger;
- SavedGameRecord;
- EvenChessSettings;
- AdminFeatureFlag;
- PatchMapEntry.

## U.3 Lichess seams

Likely seams include:

- lobby setup modal and quick-pairing cards;
- seek/search service;
- challenge flow;
- round/game creation metadata;
- round WebSocket payloads;
- board overlay layer;
- analysis/replay page;
- study/opening explorer board surfaces;
- account/top-bar templates;
- settings/preferences;
- plans/subscription pages;
- admin/ops pages.

REQ-U-V2-001: Store EvenChess metadata in a way that is versioned and does not corrupt normal Lichess game assumptions.

REQ-U-V2-002: Avoid editing core game BSON/schema internals unless explicitly approved and patch-mapped.

REQ-U-V2-003: Every seam must be listed in integration log with rollback notes.

<!-- END FILE: APPENDIX_U_V2_DATA_MODELS_AND_LICHESS_SEAMS.md -->

---

<!-- BEGIN FILE: APPENDIX_V_V2_TESTING_QA_AND_ACCEPTANCE_CRITERIA.md -->

# Appendix V — Testing, QA, and Acceptance Criteria

## V.1 Purpose

This appendix defines required tests and release gates.

## V.2 Core tests

REQ-V-V2-001: Public play opens native Lichess-style setup/search with EvenChess controls.

REQ-V-V2-002: Search submission creates EvenChess search/match contract, not ordinary Lichess rated search.

REQ-V-V2-003: Normal Lichess mechanics still work: board, moves, clocks, PGN/replay, analysis.

REQ-V-V2-004: ECR/MMR is separate from normal Lichess ratings.

REQ-V-V2-005: Level-gated payloads never expose higher-level data to lower-level side outputs.

REQ-V-V2-006: Display Engine rejects stale payloads.

REQ-V-V2-007: Overlay visuals match requirements for Offset Count, hanging pieces, threats, pins, opening, and eval.

REQ-V-V2-007A: Test Ground Test ECE L10 payload fixtures must include student-owned attackable hanging, opponent-owned attackable hanging, non-attackable hanging/loose, student-favorable Offset Count, opponent-favorable Offset Count, equal Offset Count shield, student threat arrows, opponent threat arrows, pinned pieces, candidate moves, eval, and coach text so ECL overlay/toggle behavior can be tested while real ECE is still in development.

REQ-V-V2-008: Summary and Plan cards remain fixed-size and do not cause layout jump.

REQ-V-V2-009: Proposed-move mode runs only for exactly one legal green arrow and is cached/cleared correctly.

REQ-V-V2-010: Live review modes show saved live ECE history: Live White, Live Black, Live Both.

REQ-V-V2-011: Custom review consumes/uses token logic where configured.

REQ-V-V2-012: Full-game ECE mode produces review output without altering live rating settlement.

REQ-V-V2-013: Bot matchmaking and simulation operations must have focused tests for admin settings, simulation seeding, simulation-ticket-only cleanup, dashboard monitoring, timeout-based fallback, scope gating, and deployment-safe public search disclosure.

REQ-V-V2-014: Before public launch, EvenChess-Lichess must provide and run a multi-user load harness that exercises search/matchmaking, sim bots or test users, game creation, move flow, server-to-server ECE calls, stale-response rejection, replay/payload caching, and assistance audit logging together.

## V.3 Acceptance gates

Version 2 phase acceptance requires:

- tests run or documented reason;
- patch-map/integration log updates;
- no unreported invariant conflicts;
- no accidental normal Lichess public rated pool use;
- no client-side coaching permission decision;
- no exposed API keys/secrets;
- no layout regression on desktop/mobile board surfaces.
- ECL multi-user load harness run or a documented blocker with public launch held.

<!-- END FILE: APPENDIX_V_V2_TESTING_QA_AND_ACCEPTANCE_CRITERIA.md -->

---

<!-- BEGIN FILE: APPENDIX_W_V2_CODEX_PHASE_PLAN_AND_TASK_PACKETS.md -->

# Appendix W — Codex Phase Plan and Task Packets

## W.1 Purpose

This appendix defines implementation sequencing.

## W.2 Phase plan

| Phase | Name | Scope |
|---|---|---|
| A | Requirements V2 | Replace requirements with sewn-in V2 suite. |
| B | Current code audit | Identify addon-style work and current seams. |
| C | Branding/theme | Deep-blue EvenChess identity in native shell. |
| D | Top bar/tokens/settings shell | Token balances and EvenChess settings. |
| E | Setup/search UI | Add EvenChess controls to native play modal/cards. |
| F | MMR Engine framework | Search request -> match contract simulation. |
| G | Matchmaking integration | Route public search to EvenChess contracts. |
| H | ECE framework integration | Server gets ECE output for FEN/levels. |
| I | Display Engine framework | Render mock payload overlays/cards. |
| J | Live ECE history | Store per-turn ECE history and Used Level. |
| K | Proposed move | Single-arrow proposed move mode. |
| L | Review modes | Live White/Black/Both and custom mode framework. |
| M | Full-game ECE | Full-game review payload/token integration. |
| N | Stockfish/AI | Real bounded engine/AI providers. |
| O | Feature surfaces | Study, puzzles, openings, analysis, computer play. |
| P | Monetisation | Tokens, ads, subscriptions, saved games. |
| Q | Telemetry/audit | Calibration dashboards and event logging. |
| R | Abuse/ops | Trust controls, kill switches, incidents. |
| S | Regression hardening | Lichess regression + EvenChess acceptance. |
| T | Release candidate | Final integration and go/no-go. |

## W.3 Task packet rule

Each Codex task must state:

- phase;
- files to inspect first;
- exact allowed scope;
- likely seams;
- tests expected;
- patch-map requirement;
- rollback note requirement.

REQ-W-V2-001: Codex must not implement future phases during the current phase.

REQ-W-V2-002: Codex must not use `git add .` when unrelated changes may exist.

<!-- END FILE: APPENDIX_W_V2_CODEX_PHASE_PLAN_AND_TASK_PACKETS.md -->

---

<!-- BEGIN FILE: APPENDIX_X_V2_UPSTREAM_SYNC_AND_PATCH_MAP_REQUIREMENTS.md -->

# Appendix X — Upstream Sync and Patch-Map Requirements

## X.1 Purpose

This appendix keeps the EvenChess fork updateable while allowing necessary sewn-in changes.

## X.2 Patch-map entries

Every core/upstream Lichess edit must record:

- date;
- phase/task;
- upstream base SHA;
- fork SHA;
- file touched;
- requirement reference;
- why the core file had to be touched;
- whether the change can be isolated later;
- merge risk;
- tests added/run;
- rollback note.

REQ-X-V2-001: Integration seams must be documented in `docs/integration` or the patch map.

REQ-X-V2-002: Requirements-only documentation changes may skip patch-map entries unless they change implementation policy.

## X.3 Upstream sync

REQ-X-V2-010: Do not sync upstream casually during feature work.

REQ-X-V2-011: Before upstream sync, ensure working tree is clean and patch map is current.

REQ-X-V2-012: After upstream sync, rerun Lichess regression and EvenChess integration tests.

REQ-X-V2-013: High-risk areas require explicit approval before modification: scalachess, chessground, pgn-viewer, lila-ws, lila-search, lila-fishnet, fishnet, global rating/perf internals, and core game BSON/schema internals.

<!-- END FILE: APPENDIX_X_V2_UPSTREAM_SYNC_AND_PATCH_MAP_REQUIREMENTS.md -->

---

<!-- BEGIN FILE: APPENDIX_Y_V2_RESERVED_FUTURE_REQUIREMENTS.md -->

# Appendix Y — Reserved Future Requirements

This appendix is reserved for future approved requirements.

No implementation authority exists here until product-owner approved content is added.

<!-- END FILE: APPENDIX_Y_V2_RESERVED_FUTURE_REQUIREMENTS.md -->

---

<!-- BEGIN FILE: APPENDIX_Z_V2_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md -->

# Appendix Z — Superseded and Overridden Requirements Register

## Z.1 Purpose

This appendix records supersessions created by Version 2.

## Z.2 Version 2 supersessions

| Override ID | Old requirement/assumption | New Version 2 requirement | Reason |
|---|---|---|---|
| OVR-V2-001 | EvenChess can be implemented as a separate add-on/page beside Lichess. | EvenChess must be sewn into native Lichess-style public flows. | Product owner clarified the desired product experience. |
| OVR-V2-002 | Public shell should preserve Lichess with minimal EvenChess entrypoints. | Public product is EvenChess, using Lichess internals and visual patterns. | Earlier V1.3 interpretation was too detached. |
| OVR-V2-003 | Matchmaking can be handled by feeding equivalent Lichess rating into Lichess search. | EvenChess MMR Engine owns search/match contracts; Lichess receives finalized game contract. | Level preferences and offsets require contract logic. |
| OVR-V2-004 | Coaching text can be many separate card sections. | Text is consolidated into fixed Summary and Plan cards. | Avoid live UI overload and layout jump. |
| OVR-V2-005 | Review can rely only on ordinary analysis/replay. | Review uses saved ECE history plus custom/full-game ECE modes. | Enables Live White/Black/Both and token-based L10 analysis. |
| OVR-V2-006 | Tokens only cover game starts and summaries. | Add custom ECE analysis/full-game analysis token paths. | L10 custom/full-game analysis can create AI/engine cost. |
| OVR-V2-007 | EvenChess overlays are generic. | Each overlay has explicit board position, visual behavior, priority, and level-gating. | Display tuning requires precision. |
| OVR-V2-008 | Board-state ECE calls use a single synchronous `/v1/ece/board` endpoint. | Board-state ECE calls use `/v1/ece/board/quick` first and `/v1/ece/board/deep` only when deep/provider-backed addenda are needed. | ECE caller contract split fast deterministic UI output from slower provider work. |
| OVR-V2-009 | ECE history retention numbers were TBD. | Retain the user's last 10 games with live ECE history and last 100 requested full-game/custom analyses by default. | Product owner set concrete memory limits for replay and analysis reuse. |
| OVR-V2-010 | Attackable hanging pieces use one generic color and non-attackable loose/hanging pieces have a rim. | Student-owned attackable hanging pieces use red circle/rim, opponent-owned attackable hanging pieces use purple circle/rim, and non-attackable loose/hanging pieces use only an orange bottom-left exclamation marker. | Product owner clarified the active board-overlay styling target and ownership-specific marker colors. |
| OVR-V2-011 | Applying target-level preferences sends hook search to the separate Target Level queue. | Rated/casual target preferences stay in their selected rated/casual queue and constrain only the MMR Engine's assigned Set Level combinations. | Product owner clarified that target preferences are ordinary rated/casual search preferences, not a separate mode. |
| OVR-V2-012 | Threat arrows can be controlled by one generic threat toggle. | Student threat arrows and opponent threat arrows have separate display toggles while both remain level-gated. | Product owner clarified separate student/opponent threat control is required for testing and live play. |
| OVR-V2-013 | Live Used Level always starts at L0 regardless of account preference. | New games start from the account Preferred starting Used Level capped by the game Set Level, while Used Level remains monotonic for the game. | Product owner clarified level selections should persist and each game should start from the user's preferred level preference. |
| OVR-V2-013A | The in-game "Apply up to" dropdown always enables every feature at or below the selected level. | The dropdown applies the user's saved preferred starting feature-toggle profile for eligible features at or below the selected level, defaulting missing values to on. | Product owner clarified settings need per-feature defaults, and dropdown/preferred-start initialization should use those defaults rather than blindly enabling every feature. |
| OVR-V2-014 | Public lobby search status can show internal match-contract source/stage details and raw token/bot diagnostics. | Public search status and browser-facing search JSON must show only deployment-safe labels and hide internal matching, token, and bot diagnostics. | Deployment polish requires public game/search setup surfaces to look like a normal production site rather than a test/debug panel. |
| OVR-V2-015 | Local bot controls can be exposed as unauthenticated Test Ground actions. | Bot matchmaking and simulation start/stop/settings remain admin-only; Test Ground may embed/link the admin panel but must not bypass admin auth or CSRF. | Bot controls affect public matchmaking behavior and must remain operational/admin controls rather than local unauthenticated utilities. |
| OVR-V2-016 | Candidate moves auto-render when present and proposed-move help is one call per turn. | Candidate fields are treated as Potential Moves, revealed only by ECL buttons with per-game limits; proposed-move calls are per-game quotas of L5=1, L6-L7=2, L8-L10=3. | ECE v1.2 clarified the proposed/potential split and the caller-owned quota model. |
| OVR-V2-017 | Potential/proposed move uses can be tracked only in browser display state during local testing. | Potential/proposed move consumed counts are server-side ECL game assistance state; browser refresh cannot restore uses, and potential-move display data is returned to the browser only after a same-origin ECL reveal endpoint authorizes or returns a cached reveal. | Prevents exploiting consumable assistance by refreshing or reading unrevealed potential-move payload data. |
| OVR-V2-018 | Public search exposes Apply Preferences, Player Target Level, Opponent Target Level, Both Target Level, Search until preferences are met, or manual rating-window controls. | Public rated/casual EvenChess search exposes one Preferred Set Level dropdown with Any and L0-L10. Any is normal search; L0-L10 fixes/prefers the searching player's own Set Level. Rating windows and opponent assignment are computed server-side from versioned level-offset and base-level tables. | Product owner simplified the preference model and clarified that EvenChess MMR must be embedded into the existing Lichess search flow through server-side contract assignment. |
| OVR-V2-019 | Level offsets can be represented only as cumulative L0-L10 values with no admin-calibrated gap table. | The active ECOR Table stores adjacent level-gap rating values, derives cumulative offsets from those gaps, stores an admin-editable rating-to-base-level table, retains the latest 1,000,000 calibration samples, and exposes admin-only calibration recommendations plus rollback snapshots. | Product owner clarified that ECOR must be calibratable from game outcomes and adjustable by admin decision after reviewing statistics. |
| OVR-V2-020 | Challenge-a-friend setup can reuse the generic public EvenChess search endpoint. | Challenge-a-friend setup must use native friend challenge creation or an explicit friend-contract route carrying the recipient, with friend-specific level modes and a public request-card summary. | Friend challenges have a known recipient and must not become ordinary pool searches without `friendUser`. |
| OVR-V2-021 | Offset Count payloads can be treated as attacking-side-only or opponent-target-only lists. | Offset Count payloads are side-output perspective board-fact datasets. ECL displays every delivered offset item for the selected side output, using `target_square` and signed `piece_count_delta` when present. | ECE contract now mirrors deterministic overlay facts to both side outputs and flips polarity into each side's perspective. |
| OVR-V2-022 | A successful public EvenChess search can show a separate status/debug card with queue, scenario, access, bot, wait, and match diagnostics. | Public search keeps the native Lichess searching experience. Verbose EvenChess search diagnostics are hidden by default and require an explicit local/debug flag. | Product owner clarified the extra card is a debug artifact and normal quick/search flows should rely on remembered settings plus native searching indicators. |
| OVR-V2-023 | Platform-bot Stockfish/Lichess AI level can be a fixed hardcoded mapping or directly reuse the EvenChess Set Level. | Platform-bot AI handoff uses the matched bot profile's Stockfish equivalent level, derived from an admin-editable SF1-SF8 equivalent rating table. | Product owner clarified bot strength must match equivalent ratings and remain adjustable after calibration/review. |
| OVR-V2-024 | Simulation bots can be static placeholder queue tickets with default provisional ECR or a single default time-control pool. | Simulation bots are active queue participants with configured target ECR, rating-derived Stockfish profile, playable time-control distribution, and replenishment/resizing behavior while simulation mode is running. | Product owner clarified simulation mode must behave like a population of human-like accounts for low-population testing and launch fill. |
| OVR-V2-025 | Launch-period rated/casual searches always require normal game-start token/ad access. | Admins may enable a bounded launch free-match-token window that waives rated/casual game-start token consumption and ad access while displaying "Tokens are temporarily free" on setup/search surfaces. | Product owner clarified launch should allow temporary free matching while the player pool grows. |
| OVR-V2-026 | Platform-bot fallback may create a native Lichess AI/computer game from a generated non-roster bot ticket. | Matchmaking and simulation bots must be roster-backed real local bot accounts and must use the normal human challenge/game path; if no roster account is available, bot fallback keeps searching instead of creating a Stockfish/computer game. | Product owner observed Stockfish labels and one-sided clocks, clarifying that bot matches must look and behave like human-vs-human EvenChess games. |
| OVR-V2-027 | Matchmaking fallback and simulation can maintain separate small bot-account text fields and run at the same time. | Bot operations use one shared bulk roster, defaulting to a generated 1000-name local roster, and only one bot mode may run at a time. | Product owner asked to create/use a 1000-account roster for both modes and prevent simultaneous matchmaking/simulation bot runs. |
| OVR-V2-028 | Move-triggered ECE refresh must hide stale board-square visuals, arrows, highlights, and markers until a non-stale current-position payload arrives. | Move-triggered ECE refresh keeps the last accepted safe board visual layer mounted as an internal transition layer until the replacement current-position payload arrives, while stale/mismatched ECE responses are still rejected as current advice. | Product owner observed visible overlay blanking between moves and clarified the UI should update only changed overlay elements rather than disappearing during ECE refresh. |
| OVR-V2-029 | Eval bar/status may parse any numeric eval value, including quick placeholder zero or side-output-relative `score.cp`, and may return to neutral while waiting for deep eval. | Eval bar/status must ignore quick/no-eval placeholders, prefer fixed-perspective `score.cp_white` / `score.mate_white` when present, carry safe structured eval metadata through the ECL bridge, use a nonlinear win-probability-style centipawn mapping for the fixed White-up/Black-down bar, and retain the last accepted live/proposed eval until a new accepted eval payload replaces it. | ECE v1.2 clarified the normalized Stockfish eval payload and proposed-move `eval_after` usage; Stockfish/Lichess eval semantics require perspective-aware and nonlinear display handling. |
| OVR-V2-030 | Bot rosters can be considered valid once account names are saved, leaving local/staging user-account creation to manual database work. | Bot operations must provide admin-only roster provisioning/checking so missing usernames are created as normal local/staging accounts and disabled or BOT-titled accounts are reported before matchmaking or simulation is expected to work. | Runtime testing showed the bot queue logic could pass while game creation failed silently because saved roster usernames did not exist as real local users. |
| OVR-V2-031 | Roster-backed bot matching is complete once a normal user-vs-user game is created. | Roster-backed matchmaking and simulation bots must also be driven by an ECL-managed move runner or equivalent bot client that submits legal moves through Lichess's normal round move path with both clocks preserved. | Browser testing showed roster-backed games could start successfully but then stall because normal local accounts are not native Lichess AI players and do not move without a bot runner. |
| OVR-V2-032 | Simulation mode can be considered active when it has filled the synthetic queue and reports possible bot-vs-bot contracts. | Simulation mode must actively pump valid sim-vs-sim contracts into normal roster-account games, with ticket retirement and managed bot runners attached to both sides. | Runtime review showed seeded simulation tickets could remain waiting forever unless a human search polled the same pool; this did not satisfy the requirement to simulate humans playing each other. |
| OVR-V2-033 | Simulation queue pumping can consume matchable simulated-player tickets before a waiting human search, and small batches may spread selected pools only to rating extremes. | Human searches must be matched before sim-vs-sim pumping, pump attempts are throttled and duplicate-pair guarded, and small simulation batches seed selected pools center-out so mid-rating human searches have available simulated opponents. | Browser testing showed a 24-player simulation run could report active tickets but leave a 1500-rated 5+0 Blitz search spinning because the relevant pool had only extreme-rating simulated users. |
| OVR-V2-034 | Roster-backed bot accounts can rely on default newly-created Lichess perfs and EvenChess search can preserve only a broad time-control bucket. | Bot roster provisioning and game handoff must seed/repair established, non-round normal Lichess display ratings, and search tickets must carry the exact real-time clock limit/increment into the native challenge/game path. | Browser testing showed roster-backed bot games could still display `1500?` and fail to look like normal human clocked games unless Lichess perfs and exact clock metadata are fixed before challenge creation. |
| OVR-V2-035 | Live L6 WikiBook can be shown as a small static opening card with live-only collapse behavior. | Live L6 WikiBook must use the analysis-style `toggle-box` behavior, avoid duplicate toggle handlers, keep the header clickable/keyboard-toggleable, and provide a bounded readable open state with internal content scrolling. | Product owner clarified the live WikiBook should function like the analysis WikiBook and observed the live card was too small, hard to scroll, and did not close when clicked. |
| OVR-V2-036 | Matched EvenChess bot games can redirect the human to a watcher URL while bot presence catches up after game lookup. | Matched humans must receive full player URLs, and managed roster-backed bot runners must be attached from the accepted game before the redirect is returned so the first round render is human-vs-human shaped with both clocks. | Runtime testing showed preferred-level bot games could render with fallback levels, one clock, and opponent-left/computer-game presentation when round bootstrap and bot attachment happened through watcher/late paths. |

## Z.3 Rules

REQ-Z-V2-001: Any future contradiction must be added here before implementation.

REQ-Z-V2-002: Codex must report supersession/override in its completion summary.

REQ-Z-V2-003: Appendix Z is not optional; silent requirement replacement is prohibited.

<!-- END FILE: APPENDIX_Z_V2_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md -->

---
