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

## T.5 Completion report template

```text
Phase:
Scope completed:
Files changed:
Upstream Lichess files touched:
Patch map entries added/updated:
Tests added/updated:
Tests run:
Invariant checks:
Normal chess regression status:
EvenChess mode regression status:
Unresolved items:
Risks:
Ready for next phase: yes/no
```
