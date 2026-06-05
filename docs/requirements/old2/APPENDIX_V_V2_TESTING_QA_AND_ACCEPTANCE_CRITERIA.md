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

REQ-V-V2-008: Summary and Plan cards remain fixed-size and do not cause layout jump.

REQ-V-V2-009: Proposed-move mode runs only for exactly one legal green arrow and is cached/cleared correctly.

REQ-V-V2-010: Live review modes show saved live ECE history: Live White, Live Black, Live Both.

REQ-V-V2-011: Custom review consumes/uses token logic where configured.

REQ-V-V2-012: Full-game ECE mode produces review output without altering live rating settlement.

## V.3 Acceptance gates

Version 2 phase acceptance requires:

- tests run or documented reason;
- patch-map/integration log updates;
- no unreported invariant conflicts;
- no accidental normal Lichess public rated pool use;
- no client-side coaching permission decision;
- no exposed API keys/secrets;
- no layout regression on desktop/mobile board surfaces.
