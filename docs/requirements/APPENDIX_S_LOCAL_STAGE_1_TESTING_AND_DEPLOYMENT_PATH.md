# Appendix S - Local Stage 1 Testing and Deployment Path

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Defines local boot and proof path before full implementation.


## S.1 Stage 1 goal

- run Lichess locally on the user's PC;
- confirm accounts work;
- confirm local games work;
- confirm the codebase can be safely modified;
- create EvenChess namespace/module boundary;
- do not implement full product before local baseline works.

## S.2 Phases

STAGE1-L1-001: Architecture inspection - inspect repo, upstream commit, lila/lila-docker docs, modules, routes, UI build path, tests, patch map. Deliver `docs/evenchess/stage1_architecture_inspection.md`.  
STAGE1-L1-010: Local Lichess/lila-docker boot - confirm site, accounts/dev accounts, local game creation, legal moves, clocks, result, review/history. Stop if local Lichess cannot run.  
STAGE1-L1-020: EvenChess module boundary - create docs/evenchess notes and minimal namespace/module stub without changing game behavior.  
STAGE1-L1-030: EvenChess mode flag only - harmless server-owned mode flag/metadata in a controlled dev path. It must not alter legal moves, clocks, rating, matchmaking, or normal chess.  
STAGE1-L1-040: Blue theme / brand shell - simple EvenChess badge/theme for flagged surfaces only; no official Lichess implication.  
STAGE1-L1-050: Dummy server-authorised overlay - non-advisory payload from server logic; client displays only server payload; no engine/AI.  
STAGE1-L1-060: Assistance ledger foundation - append-only event for dummy overlay render/suppression; include game/player/feature/visibility/ply/policy version if available.  
STAGE1-L1-070: AI summary interface mock/OpenAI provider - server-side interface, mock first, optional OpenAI provider only with server-side credentials and schema validation.  
STAGE1-L1-080: Go/no-go report - produce `docs/evenchess/stage1_go_no_go.md` with boot, account, game, boundary, mode, overlay, ledger, AI, patch map, tests, risks, next phase.

## S.3 Stop rules

Stop and report if local lila does not boot, normal chess breaks, EvenChess changes cannot be isolated, a broad core rewrite is needed before a narrow hook is tested, a requirement conflicts with architecture/invariants, or live engine/AI coaching is attempted before ledger foundation.
