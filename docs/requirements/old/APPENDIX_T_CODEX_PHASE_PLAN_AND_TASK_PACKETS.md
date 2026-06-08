# Appendix T - Codex Phase Plan and Task Packets

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Replaces old A-Z plan with fork-specific Stage 1 and later phase families.


CODEX-L1-001: Codex implements only the requested phase.
CODEX-L1-002: Each phase reads main, relevant appendices, AGENTS.md, patch map.
CODEX-L1-003: Each phase adds/updates tests unless documentation-only.
CODEX-L1-004: Each phase prints completion report and invariant checks.
CODEX-L1-005: Each phase updates patch map if upstream files are touched.
CODEX-L1-006: Each Version 1.1 production integration phase updates `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
CODEX-L1-007: Each Version 1.2 production integration phase reads `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_2_PLAN.md` and updates `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
CODEX-L1-008: Version 1.3 cleanup work preserves useful EvenChess module contracts while narrowing broad Lichess public-shell edits and updating the patch map/integration log.
CODEX-L1-009: Current Version 1 public integration keeps the Lichess style and shell but routes public Play/lobby/setup-modal controls to EvenChess-owned Set Level, ECR, token, disclosure, overlay, AI/TTS, and audit contracts.

## T.3 Stage 1 task packets

| Packet | Prompt starter |
| --- | --- |
| S1.1 Architecture inspection | Implement Stage 1.1 only. Read main, B, C, S, T, AGENTS, patch map. Inspect lila/lila-docker. No product features. Produce architecture inspection. |
| S1.2 Local boot | Boot local Lichess via lila-docker or verified path. Confirm accounts and games. Record commands/errors/fixes. |
| S1.3 Module boundary | Create minimal EvenChess namespace/module boundary. No game behavior changes. Patch-map upstream touches. |
| S1.4 Mode flag | Add harmless server-owned EvenChess mode flag/metadata only. No coaching/rating/matchmaking/tokens. |
| S1.5 Blue shell | Add simple blue EvenChess badge/theme for flagged surfaces only. |
| S1.6 Dummy overlay | Add dummy non-advisory server-authorized overlay. No engine/AI. |
| S1.7 Ledger foundation | Add append-only audit event foundation for dummy overlay. |
| S1.8 AI mock/provider | Define server-side AI provider interface and mock; optional provider only server-side/configurable. |
| S1.9 Go/no-go | Produce stage1_go_no_go.md with status, tests, risks, next phase. |

## T.4 Post-Stage 1 phase families

| Phase | Purpose | Appendices |
| --- | --- | --- |
| P2 | Source-of-truth hardening | Main, B, Z |
| P3 | Feature registry and policy | E, G |
| P4 | Offset Count | H, I, V |
| P5 | Overlay primitives | F, G, E |
| P6 | Assistance accounting | I, P, U |
| P7 | Engine gateway | L, G, V |
| P8 | AI wording | M, G, V |
| P9 | Post-game summaries | M, N, P |
| P10 | ECR and matchmaking | J, K, P |
| P11 | Monetisation | N, Q, O |
| P12 | Marketing/funnel | O, P, R |
| P13 | Abuse/ops | Q, R |
| P14 | Release hardening | V, R, Z |

## T.5 Version 1.1 production integration phases

| Phase | Purpose | Primary requirements |
| --- | --- | --- |
| V1.1-A | Product override and integration ledger. | Main, T, Z, patch map, upstream sync |
| V1.1-B | EvenChess-only public shell, routing, and navigation. | A, B, C, D, O, Z |
| V1.1-C | Game policy, persistence, and mode metadata. | D, G, U, V |
| V1.1-D | Level-based search, matchmaking, and ECR. | J, K, P, U |
| V1.1-E | Server coaching policy, audit ledger, and live transport. | E, F, G, I, U |
| V1.1-F | Overlay UI, coaching cards, and Offset Count. | E, F, G, H, V |
| V1.1-G | Stockfish and analysis gateway. | L, G, M, V |
| V1.1-H | Live OpenAI coaching and summaries. | M, G, N, P, V |
| V1.1-I | Subscriptions, tokens, and rewarded ads. | N, O, Q, U |
| V1.1-J | Marketing, attribution, and funnel control. | O, P, R |
| V1.1-K | Abuse, trust, admin, and incident controls. | Q, R, V |
| V1.1-L | Production QA, release hardening, and go/no-go. | V, R, Z |

## T.6 Version 1.2 production polish and integration phases

Version 1.2 phase details live in `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_2_PLAN.md`.

| Phase | Purpose | Primary requirements |
| --- | --- | --- |
| V1.2-A | Product direction and documentation reset for Lichess-powered EvenChess. | Main, T, Z, integration log, upstream sync |
| V1.2-B | Deep blue design system and polished public shell. | A, B, C, D, F, O, V |
| V1.2-C | Restore/expose compatible Lichess feature navigation under EvenChess branding. | B, C, D, O, Z |
| V1.2-D | User-facing EvenChess settings section. | D, E, F, G, H, I, M, N, P, U |
| V1.2-E | Admin/backend EvenChess settings for provider config, feature flags, secrets status, and safety toggles. | L, M, N, O, P, Q, R, U |
| V1.2-F | Real live-board overlay integration and UI polish. | E, F, G, H, I, V |
| V1.2-G | AI overlays for study, analysis, and opening explorer. | C, F, G, L, M, P, U, V |
| V1.2-H | Text-to-Speech Coach for authorized coaching and explanation text. | F, G, M, N, P, Q, R, U, V |
| V1.2-I | Production play/search/matchmaking route integration. | D, J, K, N, P, U, V |
| V1.2-J | Subscription, token, rewarded-ad, and account UX integration. | N, O, P, Q, U, V |
| V1.2-K | Admin operations dashboard integration for AI, TTS, engine, overlays, tokens, incidents, and calibration. | P, Q, R, U, V |
| V1.2-L | Production polish, visual QA, accessibility, performance, upstream-sync readiness, and go/no-go evidence. | V, R, Z, upstream sync |

## T.6.1 Version 1.3 minimal Lichess integration cleanup

Version 1.3 is a cleanup pass, not a new product-feature expansion.

| Phase | Purpose | Primary requirements |
| --- | --- | --- |
| V1.3-A | Record the Lichess-first integration direction and supersede broad public-shell takeover requirements. | Main, Z, integration log, upstream sync |
| V1.3-B | Restore standard Lichess homepage/top navigation with only small explicit EvenChess entrypoints. | B, C, D, O, Z |
| V1.3-C | Keep `/evenchess/*` routes namespaced and explicit. | D, U, V |
| V1.3-D | Make EvenChess play/account pages look native to Lichess with restrained blue accents. | F, N, O, V |
| V1.3-E | Preserve EvenChess user settings as display/default preferences only. | F, G, H, I, M, P |
| V1.3-F | Preserve admin/backend settings while keeping raw secrets in server environment/secret config. | L, M, N, O, P, Q, R |
| V1.3-G | Keep live board overlays optional, server-authorized, and inert for normal games. | F, G, I, U, V |
| V1.3-H | Keep study/analysis/opening overlays optional and grounded in server-authorized payloads. | C, F, G, L, M, U, V |
| V1.3-I | Keep TTS overlay-only and limited to visible authorized text. | F, G, M, P, Q, V |
| V1.3-J | Update patch map and integration ledger for every narrowed upstream seam. | Patch map, integration log |
| V1.3-K | Run EvenChess and root compile regressions plus route smoke where possible. | V |
| V1.3-L | Write the Version 1.3 completion report. | Main, T, Z |

## T.6.2 Current Version 1 active integration alignment

This alignment supersedes the "minimal entrypoint only" reading of Version 1.3 where it conflicts with public Play/Search.

| Area | Direction | Primary requirements |
| --- | --- | --- |
| Shell and brand | Keep Lichess page structure and navigation density; visible product name is EvenChess with restrained deep-blue accents. | A, B, C, F, Z |
| Public Play | Primary `PLAY`, lobby start buttons, and native setup-modal submission route to EvenChess search, not ordinary Lichess rated pools. | D, J, U, V |
| Lobby setup controls | Reuse the Lichess setup modal/time-control surface and add EvenChess mode, Set Level, Target Level, token/account, and disclosure controls. | D, F, J, N, U |
| Live game surface | Use Lichess board/clocks/game lifecycle; render EvenChess overlays only from server-authorized payloads tied to persisted policy metadata and audit IDs. | F, G, I, U |
| Learning surfaces | Keep study/openings/analysis native; EvenChess AI/TTS appears only through optional, grounded, server-authorized overlay payloads. | C, F, L, M, U |
| Integration records | Every upstream Lichess seam remains patch-mapped and recorded in `docs/integration`. | B, T, Z |

## T.7 Completion report template

```text
Phase:
Scope completed:
Files changed:
Upstream Lichess files touched:
Patch map entries added/updated:
Integration log entries added/updated:
Tests added/updated:
Tests run:
Invariant checks:
Normal chess regression status:
EvenChess mode regression status:
Unresolved items:
Risks:
Ready for next phase: yes/no
```
