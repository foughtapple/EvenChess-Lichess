# Appendix C — Lichess-Provided Capabilities and Boundaries

## C.1 Purpose

This appendix prevents rebuilding mature Lichess capabilities while still allowing EvenChess to replace public rating/search/coaching behavior.

## C.2 Lichess-provided, do not rebuild

REQ-C-V2-001: Do not rebuild legal move generation.

REQ-C-V2-002: Do not rebuild the base board UI.

REQ-C-V2-003: Do not rebuild clocks or live round state.

REQ-C-V2-004: Do not rebuild base accounts, login, logout, session, or profile foundations.

REQ-C-V2-005: Do not rebuild normal game lifecycle, move persistence, result handling, PGN, replay, or analysis foundations.

REQ-C-V2-006: Do not rebuild base puzzle, study, opening explorer, or analysis infrastructure.

REQ-C-V2-007: Do not rebuild WebSocket plumbing where lila/lila-ws already handles live updates.

## C.3 EvenChess-owned replacements/adaptations

REQ-C-V2-010: Replace public rated matchmaking/rating behavior with EvenChess MMR/ECR behavior.

REQ-C-V2-011: Adapt the Lichess setup modal and quick-search cards to collect EvenChess-specific controls.

REQ-C-V2-012: Adapt round/board UI to render EvenChess Display Engine overlays and fixed cards.

REQ-C-V2-013: Adapt analysis/review/study/opening surfaces to support EvenChess levels, review modes, and ECE history where allowed.

REQ-C-V2-014: Adapt settings/account/top-bar areas to expose EvenChess settings and token balances.

## C.4 Boundary tests

REQ-C-V2-020: Tests must prove ordinary Lichess board/game mechanics still function after EvenChess integration.

REQ-C-V2-021: Tests must prove public EvenChess play does not accidentally use normal Lichess rating pools as ECR.

REQ-C-V2-022: Tests must prove EvenChess controls appear native to Lichess surfaces rather than detached pages.
