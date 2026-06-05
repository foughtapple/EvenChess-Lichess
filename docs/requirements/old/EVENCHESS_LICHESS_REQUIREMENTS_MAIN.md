# EvenChess-Lichess Requirements Main Index

**Suite:** EvenChess-Lichess Version 1
**Status:** Live requirements suite - Lichess fork migration from EvenChess Version 1.4
**Repository target:** `foughtapple/EvenChess-Lichess`
**Live requirements path:** `docs/requirements`
**Supporting implementation path:** `docs/evenchess`
**Generated:** 2026-05-28

## 1. Purpose and authority

This document is the navigation and authority layer for the new EvenChess-Lichess Version 1 requirements suite.

The product has migrated from a custom-built chess-platform plan to a Lichess/lila fork plan. The decisive architectural rule is:

> Lichess provides the chess platform. EvenChess adds the assisted-chess mode layer.

The old EvenChess Version 1.4 requirements remain source material, but they are not direct implementation instructions. Any requirement that asks Codex to rebuild basic chess-platform infrastructure must be reclassified before implementation.

## 2. Version 1 goal

EvenChess-Lichess Version 1 must produce a local and then deployable fork of Lichess/lila that supports a separate disclosed assisted chess mode.

The core deliverable is a server-authoritative assisted mode with disclosed Set Level, audited L0-L10 coaching, Used Level, Assistance Load, Used Offset, ECR calibration, server-side Stockfish, grounded AI explanation boundaries, Offset Count, Target Level isolation, token/subscription/advertising fairness, and telemetry/operations sufficient to calibrate and pause unsafe rollout.

## 2.1 Version 1.1 production direction

Version 1.1 production work makes the public product EvenChess-only.

Lichess/lila remains the underlying chess platform for accounts, game lifecycle, legal move handling, clocks, board surfaces, analysis infrastructure, and local/production operations. Normal Lichess chess must remain preserved in the codebase for reuse, diagnostics, regression testing, and upstream update compatibility, but public start, search, matchmaking, and play flows should route users into EvenChess.

Version 1.1 public matchmaking/search must use EvenChess concepts such as Set Level, ECR, pool key, expected offset, time control, token/subscription gates where applicable, and server-owned assisted-mode policy. Normal Lichess ratings and pools must not be treated as EvenChess ECR.

Each Version 1.1 integration phase must update `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` with the seams used, upstream files touched, tests, rollback notes, and reimplementation notes for future Lichess updates.

## 2.2 Version 1.2 product direction

Version 1.2 refines the Version 1.1 public-product direction.

EvenChess should present as a polished, modern, deep-blue themed Lichess-powered chess platform. It should preserve access to Lichess-provided feature areas where they can be safely adapted, including study, opening explorer, analysis, puzzles, account/settings, and community/profile surfaces.

EvenChess-specific behavior remains the assisted-chess layer: Set Level, Used Level, Assistance Load, Used Offset, ECR, level-based matchmaking, server-authorized overlays, AI explanations/summaries, tokens/subscriptions/ads, and EvenChess operations. Rated public play must still use EvenChess mode metadata, EvenChess ECR, disclosed assistance, audit logging, and server-owned coaching permission. Normal Lichess ratings and pools must not be treated as ECR.

Version 1.2 adds explicit planning for:

- a polished deep-blue EvenChess theme that stays compatible with Lichess layout patterns;
- restoring/exposing Lichess feature navigation through EvenChess branding instead of hiding useful platform areas;
- a user-facing EvenChess settings section;
- admin/backend EvenChess settings for provider status, feature flags, safety toggles, rate limits, and secret-safe configuration;
- EvenChess AI overlays in live play, study, opening explorer, and analysis surfaces;
- optional Text-to-Speech Coach that reads only server-authorized coaching/explanation text.

Each Version 1.2 integration phase must also update `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`. The detailed Version 1.2 phase plan lives in `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_2_PLAN.md`.

## 2.3 Version 1.3 minimal Lichess integration direction

Version 1.3 supersedes the Version 1.1/1.2 public-shell takeover approach.

The production product should be Lichess-first with explicit EvenChess assisted-mode entrypoints. Lichess remains structurally Lichess for the homepage, top navigation, ordinary play, study, openings, analysis, puzzles, accounts, settings, community, and admin foundations, while the visible product wordmark is EvenChess. EvenChess should integrate as a clearly disclosed assisted mode/layer with namespaced routes, settings, overlays, AI/TTS services, ECR/matchmaking contracts, token/account systems, and admin controls.

Deep-blue EvenChess styling remains appropriate for EvenChess-specific pages and overlay components, but it must not replace the normal Lichess public shell. The primary `PLAY` navigation should start EvenChess search. Normal Lichess internals remain available for platform reuse and regression, but EvenChess public play must remain separate from normal Lichess ratings and pools.

REQ-MAIN-015: Version 1.3 public shell work must preserve the normal Lichess homepage/top navigation structure and add only minimal explicit EvenChess entrypoints.
REQ-MAIN-016: Version 1.3 must keep existing EvenChess module contracts, settings, admin controls, overlays, AI/TTS seams, and namespaced routes while narrowing broad upstream Lichess page edits wherever feasible.

## 2.4 Current Version 1 public integration direction

The product-owner clarification after Version 1.3 refines the public-shell rule again:

EvenChess should look and behave like a polished, deep-blue Lichess-powered chess site, but the public product is EvenChess. The Lichess shell, lobby shape, native setup modal, board, clocks, accounts, study, openings, analysis, puzzles, community, admin foundations, and game lifecycle remain the platform. Public `PLAY` should open the native Lichess setup modal, that modal should carry EvenChess Set Level/search controls, and submission must start an EvenChess-owned search contract instead of navigating to a separate search page or ordinary Lichess rated pools.

This means Version 1.3's "minimal entrypoint only" interpretation is superseded where it conflicts with public play/search. The durable rule is: keep the Lichess style and internals, but integrate EvenChess as the public play mode with Set Level, ECR, token/account gates, disclosed outside-help rules, server-authorized overlays, AI/TTS, and audit requirements.

REQ-MAIN-017: The visible product name is EvenChess and public play/search starts must route to EvenChess-owned search/matchmaking contracts.
REQ-MAIN-018: The Lichess lobby/setup modal may be adapted with EvenChess Set Level, mode, time-control, target-level, token/account, and disclosure controls, provided normal Lichess chess internals remain preserved and the patch map records every upstream seam.
REQ-MAIN-019: Compatible Lichess feature areas remain available under EvenChess branding, but rated public games must not use normal Lichess ratings or normal rated pools as ECR.

## 3. Explicit non-goals

Do not rebuild Lichess-provided chess basics:

- legal move generation;
- base game rooms;
- base clocks;
- normal board UI;
- ordinary game history or PGN;
- ordinary account basics unless a gap is found;
- base game lifecycle and result flow;
- normal challenge/seek/play surfaces except for narrow EvenChess integration seams;
- normal Lichess chess.

EvenChess must be added as a separate assisted mode/layer. Normal chess must remain normal chess.

## 4. Lichess source assumptions

This suite assumes the fork starts from official `lichess-org/lila` and uses `lichess-org/lila-docker` for local development unless Stage 1 proves otherwise. Source URLs to verify during Stage 1:

- `https://github.com/lichess-org/lila`
- `https://github.com/lichess-org/lila-docker`
- `https://lichess.org/source`
- `https://github.com/lichess-org/scalachess`
- `https://github.com/lichess-org/chessground`

Any conflict between these assumptions and the checked-out codebase must be reported before implementation.

## Non-negotiable EvenChess invariants

- EvenChess is a disclosed assisted chess variant, not ordinary chess with hidden help.
- Lichess/lila provides the mature chess platform foundation; EvenChess adds the assisted-chess mode layer.
- Platform coaching is legal only because it is disclosed, capped by Set Level, logged by the server, and priced into ECR.
- Non-platform guidance remains prohibited in rated EvenChess, including external engines, humans, bots, browser extensions, notes, stream chat, and unaudited analysis.
- All public coaching levels L0-L10 may be used in rated EvenChess when assigned or allowed by the platform; high levels are priced, audited, and calibrated, not hard-banned.
- Client-side code must never decide coaching permission. The server decides.
- Stockfish remains server-side only for live EvenChess assistance. The client never receives unrestricted raw engine access.
- AI explains and compresses server-authorized truth packets. Hard-coded logic, legal move validation, stored state, and Stockfish provide chess truth.
- Higher levels improve specificity, timing, and candidate precision, not live text volume.
- Used Level may increase during a game but must never decrease.
- Every coaching render, suppression, expansion, and block must be audited.
- Assistance Load, Used Level, Used Offset, and ECR calibration are core fairness requirements.
- Target Level mode must not update or corrupt normal ECR.
- Live rated play should avoid labelled "best move" wording unless explicitly approved later.
- Subscription, ads, tokens, marketing copy, landing variants, campaign configuration, and launch windows must not bypass rated fairness.
- Premium must never be described or implemented as stronger live help.
- Marketing configuration must not alter Set Level, Used Level, Assistance Load, Used Offset, ECR, matchmaking fairness, coaching permission, Stockfish profile, or live coaching strength.
- Offset Count is the existing Exchange Resolver / take-take-take feature, not a missing feature.
- Offset Count display rule: shield/blue/0 = equal trade, green = student wins material, red = opponent wins material.


## Requirement classification key

| Class | Meaning | Implementation direction |
|---|---|---|
| Lichess-provided | Capability exists in lila/Lichess and is not an EvenChess build target. | Use/configure/hook; do not rebuild. |
| EvenChess-specific | Unique to the assisted-chess model. | Implement under an EvenChess namespace/module boundary. |
| Adapted-to-fork | Old requirement remains valid but must run through lila architecture. | Add thin integration seams; record upstream edits in patch map. |
| Superseded | Replaced by the fork approach. | Do not implement unless later revived; record in Appendix Z. |
| Uncertain | Needs product-owner or technical decision. | Stop, report the decision, do not guess. |


## 5. Required folder structure

| Path | Role | Notes |
| --- | --- | --- |
| AGENTS.md | Always-on Codex operating instructions. | Must tell Codex to read this main document first and classify old requirements before coding. |
| docs/requirements | Live requirements source of truth. | Contains this main document, appendices A-Z, diff, patch map, and upstream sync process. |
| docs/evenchess | Implementation notes, go/no-go reports, architecture notes, handovers. | Not a replacement for requirements. |
| docs/integration | Version 1.1+ integration ledger. | Records each EvenChess-to-Lichess seam, upstream touch, rollback note, and reimplementation note for future upstream updates. |
| docs/requirements/source | Optional future source archive. | Old prompts, reports, and amendments can be kept here after incorporation. |
| docs/requirements/version1.4 | Optional legacy archive. | Old custom-platform requirements are records only. |

## 6. Appendix navigation map

| Appendix | File | Primary authority |
| --- | --- | --- |
| A | APPENDIX_A_*.md | Product invariants, vocabulary, modes, public positioning boundaries. |
| B | APPENDIX_B_*.md | Fork architecture, upstream sync, namespace strategy, source assumptions. |
| C | APPENDIX_C_*.md | What Lichess provides and must not be rebuilt. |
| D | APPENDIX_D_*.md | EvenChess mode identity, routing, player modes. |
| E | APPENDIX_E_*.md | Coaching ladder, registry, level gates. |
| F | APPENDIX_F_*.md | Board-first overlays, cards, accessibility. |
| G | APPENDIX_G_*.md | Server policy, coaching permission, ledger. |
| H | APPENDIX_H_*.md | Offset Count / Exchange Resolver. |
| I | APPENDIX_I_*.md | Assistance accounting and offset. |
| J | APPENDIX_J_*.md | ECR, calibration, matchmaking. |
| K | APPENDIX_K_*.md | Target mode isolation. |
| L | APPENDIX_L_*.md | Server-side Stockfish and engine help. |
| M | APPENDIX_M_*.md | AI policy and summaries. |
| N | APPENDIX_N_*.md | Tokens, subscriptions, ads, monetisation. |
| O | APPENDIX_O_*.md | Marketing config, landing variants, attribution. |
| P | APPENDIX_P_*.md | Telemetry, analytics, calibration. |
| Q | APPENDIX_Q_*.md | Abuse controls and trust. |
| R | APPENDIX_R_*.md | Admin, ops, dashboards, incidents. |
| S | APPENDIX_S_*.md | Stage 1 local boot and go/no-go. |
| T | APPENDIX_T_*.md | Codex phases and task packets. |
| U | APPENDIX_U_*.md | Logical data models and seams. |
| V | APPENDIX_V_*.md | Testing and acceptance. |
| W | APPENDIX_W_*.md | Reserved. |
| X | APPENDIX_X_*.md | Reserved. |
| Y | APPENDIX_Y_*.md | Reserved. |
| Z | APPENDIX_Z_*.md | Override/supersession register. |

## 7. Special documents

| File | Purpose |
| --- | --- |
| EVENCHESS_LICHESS_REQUIREMENTS_DIFF.md | Explains what changed from custom platform to Lichess fork. |
| EVENCHESS_LICHESS_PATCH_MAP.md | Registry for edits to upstream Lichess files. |
| EVENCHESS_UPSTREAM_SYNC_PROCESS.md | Process for keeping the fork updateable from upstream Lichess. |
| docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md | Operational record of EvenChess integration seams for future Lichess updates. |
| AGENTS.md | Codex operating instructions. |

## 8. Source-of-truth workflow

REQ-MAIN-001: Codex must read this main document before any EvenChess-Lichess work.
REQ-MAIN-002: Codex must read every relevant appendix before editing code or requirements.
REQ-MAIN-003: Codex must not implement old Version 1.4 custom-platform requirements until classified.
REQ-MAIN-004: New amendments require affected appendix updates and Appendix Z supersession records.
REQ-MAIN-005: If a user instruction contradicts a current requirement, Codex must stop and report the contradiction unless the product owner explicitly approves the override.
REQ-MAIN-006: Appendix Z is the override register. No silent replacement is allowed.
REQ-MAIN-007: Upstream Lichess file edits must be recorded in the patch map.
REQ-MAIN-008: Docs and code must preserve an EvenChess namespace/module boundary wherever feasible.
REQ-MAIN-009: Each implementation phase must include tests or a stated reason why it is documentation-only.
REQ-MAIN-010: Codex must implement one phase at a time.
REQ-MAIN-011: Version 1.1 integration phases must update `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
REQ-MAIN-012: Version 1.1 public production flows are EvenChess-only; underlying normal Lichess chess must be preserved for reuse, regression, diagnostics, and upstream sync.
REQ-MAIN-013: Version 1.2 phases refine the public product into a deep-blue EvenChess-branded Lichess platform that preserves and exposes compatible Lichess feature areas while keeping rated play, ECR, coaching permission, overlays, AI, TTS, tokens, subscriptions, and admin controls EvenChess-owned and server-authoritative.
REQ-MAIN-014: Version 1.2 integration phases must update `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.

## 9. Stage 1 priority

Stage 1 goal:

- run Lichess locally on the user's PC;
- confirm accounts work;
- confirm local games work;
- confirm the codebase can be safely modified;
- create an EvenChess namespace/module boundary;
- do not implement the full product before local baseline works.

Stage 1 phases:

- Local architecture inspection.
- Local Lichess/lila-docker boot.
- EvenChess module boundary.
- EvenChess mode flag only.
- Blue theme / brand shell.
- Dummy server-authorised overlay.
- Assistance ledger foundation.
- AI summary interface mock/OpenAI provider.
- Stage 1 go/no-go report.

## 10. Immediate unresolved decisions

| Decision | Why it matters | Default handling |
| --- | --- | --- |
| New variant vs mode flag vs metadata layer | Affects lila game model, routes, PGN, ratings, matchmaking, UI. | Stage 1 mode flag only. |
| Whether normal Lichess ratings remain visible | Affects user trust and UI separation. | Keep normal chess separate and do not mix with ECR. |
| Whether EvenChess PGNs should be public/exported | Assisted games could be misread as normal chess. | Require assisted-mode metadata decision. |
| Payment and rewarded-ad providers | Affects data and compliance. | Define interfaces only. |
| AI provider/model | Affects cost/reliability. | Mock first; server-side provider only. |
| Upstream sync cadence | Affects merge risk. | Use sync process and patch map until cadence decided. |

## 11. Codex completion report

At the end of every implementation phase Codex must report: phase, scope, files changed, upstream files touched, patch map entries, tests, invariant checks, normal chess regression, EvenChess regression, unresolved items, risks, and whether the next phase is safe.

## Markdown-only requirements policy

The live EvenChess-Lichess requirements source is Markdown.

Only `.md` files under `docs/requirements` are authoritative for Codex implementation work. `.docx` copies were removed to prevent duplicate maintenance and requirement drift.

If a future report or amendment arrives as `.docx`, it may be stored as a historical source document in a version/archive folder, but it must be incorporated into the live `.md` requirements before implementation.
