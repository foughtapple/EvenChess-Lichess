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
