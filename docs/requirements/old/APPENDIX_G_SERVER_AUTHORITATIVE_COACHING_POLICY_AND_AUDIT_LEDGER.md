# Appendix G - Server-Authoritative Coaching Policy and Audit Ledger

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Defines server-authoritative coaching policy and audit ledger.


## G.2 Server authority

POLICY-L1-001: Server owns coaching permission, board state, clock state, assistance accounting, token consumption, rating updates, and audit events.
POLICY-L1-002: Client may request/display/expand/collapse coaching but cannot decide permission.
POLICY-L1-003: Each decision uses game state, perspective, Set Level, mode, time control, registry, stale checks, abuse controls, engine/AI health, and policy version.
POLICY-L1-004: Client-side hiding is not security. Server suppression is required.
POLICY-L1-005: Debug endpoints must not expose unrestricted engine output or hidden policy data.

## G.3 Policy inputs

Required inputs include game ID, player ID, mode, rated state, time-control bucket, ply, board key, clock context, Set Level, current Used Level, requested feature, request type, registry row, exactness class, abuse state, engine/AI health, and policy version.

## G.4 Coaching render event

AUDIT-L1-001: Every render must be audited.
AUDIT-L1-002: Suppressed, blocked, stale, expired, and fallback decisions must also be audited.
AUDIT-L1-003: Events must reconstruct whether a visual idea, text, candidate, line, eval, proof, or warning was delivered.
AUDIT-L1-004: Rated-game audit events are append-only and schema-versioned.

| Field | Purpose |
| --- | --- |
| event_id | Unique ledger event ID. |
| game_id | Game reference. |
| player_id | Player receiving/requesting coaching. |
| ply | Position index. |
| board_state_key | FEN hash/equivalent. |
| feature_key | Registry key. |
| requested_level | Level implied by request. |
| set_level | Server-authorized maximum. |
| delivered_level | Actual delivered level. |
| used_level_after | Monotonic value after event. |
| assistance_weight_delta | Assistance contribution. |
| exactness_class | Concept/zone/piece/candidate/line/eval/proof. |
| surface | Board, card, warning, candidate area, review. |
| visibility | Shown, hidden, expanded, suppressed, blocked, stale, fallback. |
| source_type | Hard-coded, Stockfish, AI, hybrid. |
| engine_job_id | Engine link if applicable. |
| ai_request_id | AI link if applicable. |
| policy_version | Policy used. |
| created_at | Server timestamp. |

## G.5 Outcomes

| Outcome | Meaning | UI behavior |
| --- | --- | --- |
| allow_render | Feature allowed/current. | Show authorized payload. |
| allow_hidden | May compute but not show. | Do not count until rendered/consumed. |
| suppress_level | Above Set Level or disallowed. | Do not show; audit. |
| stale | Board/clock/premove makes advice untimely. | Mark/clear; do not charge as timely help. |
| degraded | Engine/AI unavailable but fallback safe. | Show bounded fallback. |
| block_abuse | Cooldown/cap triggered. | Suppress/throttle; audit. |

## G.6 Integration

Initial ledger may be a dedicated EvenChess store joinable to game/player. Core lila event stream edits must be small and patch-mapped. No rated EvenChess game may complete without a computable assistance summary unless marked no-rate/annul. Stage 1 uses a dummy overlay audit event before real coaching strength exists.
