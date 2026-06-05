# Appendix L - Stockfish Infrastructure and Legal Engine Help

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Defines server-side Stockfish/engine help boundaries.


ENGINE-L1-001: Stockfish must run behind an internal Engine Gateway for live EvenChess assistance.
ENGINE-L1-002: Clients never receive unrestricted raw engine access.
ENGINE-L1-003: Every job uses approved depth/nodes/movetime/MultiPV/thread/hash limits by level/time control.
ENGINE-L1-004: Live/post-game queues, caching, cancellation, timeout, fallback, degraded states are required.
ENGINE-L1-005: Engine binary/source/version/hash must be recorded.
ENGINE-L1-006: GPL/license obligations must be preserved if distributed.

| Level | Engine access direction |
| --- | --- |
| L0-L4 | No live engine candidate; deterministic/local/hybrid only. |
| L5 | Bounded single candidate; MultiPV=1 or equivalent. |
| L6 | Bounded two-candidate comparison. |
| L7 | Bounded three-candidate guidance. |
| L8 | Numeric eval/WDL may appear; approximate label required. |
| L9 | Deeper bounded branch comparison. |
| L10 | Richest bounded profile, still controlled and compact. |

The Engine Gateway accepts game/player/board/ply/perspective/requested feature/Set Level/policy/time/clock/profile/cancellation token and returns bounded truth packets with candidate set, allowed eval/lines/proofs, stale/degraded/fallback status, job ID, and audit metadata.

Codex must inspect lila/fishnet/analysis/tablebase before building a new service. If reusable, wrap safely; if not, implement an EvenChess gateway and hook narrowly. Do not expose browser Stockfish as live rated help. Required tests: endpoint security, profile validation, no raw engine payloads, cancellation/stale, cache correctness, fallback, candidate counts, L8 labels, license inventory.
