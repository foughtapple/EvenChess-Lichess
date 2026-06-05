# Appendix H - Offset Count Exchange Resolver

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Defines the existing Offset Count / Exchange Resolver feature.


## H.2 Identity

OC-L1-001: Offset Count is the existing Exchange Resolver / take-take-take feature and must not be described as missing.
OC-L1-002: Offset Count unlocks at L3.
OC-L1-003: Offset Count is a local exchange estimate, not a global best-move verdict.
OC-L1-004: Offset Count must be server-authorized in rated EvenChess.

## H.3 Display semantics

OC-L1-010: Shield/blue/0 means equal known trade.
OC-L1-011: Green means the student wins pieces/material in the local exchange.
OC-L1-012: Red means the opponent wins pieces/material.
OC-L1-013: Unknown is grey/disabled, never blue.
OC-L1-014: Player-facing number uses piece-count units: every captured piece counts as 1.
OC-L1-015: Equal outcomes show blue circle with shield and no positive/negative number.
OC-L1-016: Student wins display green number only, no plus sign.
OC-L1-017: Opponent wins display red number only, no plus sign.

## H.4 Calculation basis

OC-L1-020: Use deterministic static exchange logic.
OC-L1-021: Include legal capture, forced first capture, optional recaptures, least valuable legal attackers, occupancy updates, pins, x-rays, king legality.
OC-L1-022: Include discovered capture options and optimal legal capture/recapture choices for the local sequence.
OC-L1-023: Remain a local exchange estimate. Do not claim best-move or engine verdict.
OC-L1-024: Respect board state and perspective.

## H.5 Payload

| Field | Purpose |
| --- | --- |
| feature_key | Always offset_count. |
| game_id | Game reference. |
| ply | Position index. |
| board_state_key | Stale protection. |
| square | Target/capture square. |
| initial_move | Optional initiating move/probe. |
| result_state | equal/student_wins/opponent_wins/unknown. |
| display_color | blue/green/red/grey. |
| display_icon | shield, number, disabled. |
| display_count | Piece-count delta. |
| sequence_summary | Bounded local summary if allowed. |
| confidence | deterministic/local/stale/unknown. |
| audit_id | Render event reference. |

## H.6 Assistance and tests

Every visible reveal contributes to Assistance Load and is subject to per-position/probe abuse controls. Hidden availability does not count until visible. Required tests: golden SEE fixtures, pins/king legality, x-ray/discovered captures, display semantics, grey unknown, piece-count display, repeated reveal caps, stale clearing, L3 gating.
