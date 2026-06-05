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

## 10. Immediate open decisions

| Decision | Default for Version 2 |
|---|---|
| Whether normal Lichess public rated play remains exposed | No, public play routes to EvenChess; normal internals remain for regression/reuse. |
| Whether Lichess wordmark appears anywhere user-facing | Replace public product identity with EvenChess except where legal attribution/source context requires Lichess mention. |
| ECE history retention limit | Define a rolling default plus paid saved-game slots; exact numbers TBD. |
| Custom analysis token quantities | Define interface now; product numbers TBD. |
| Which Lichess analysis pin style is reused | Match existing visual style where feasible; exact implementation after UI inspection. |
| Whether full-game ECE accepts PGN, FEN list, or both | Prefer both: PGN plus normalized move/FEN list if available. |
