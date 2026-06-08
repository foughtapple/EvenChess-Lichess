# Appendix G — Play Setup, Quick Search, and Matchmaking UI

## G.1 Purpose

This appendix defines how EvenChess search controls are sewn into the native Lichess play/setup experience.

## G.2 Native flow

REQ-G-V2-001: Public play/search must use the native Lichess-style setup modal and quick-search cards where feasible.

REQ-G-V2-002: Do not route users to a separate EvenChess search page if the native Lichess modal/card can be adapted.

REQ-G-V2-003: The adapted UI must look like Lichess, with deep-blue EvenChess styling and native spacing.

## G.3 Required setup controls

REQ-G-V2-010: Setup/search must include time control selection using native Lichess-style controls.

REQ-G-V2-011: Setup/search must include EvenChess Set Level or default-level control where applicable.

REQ-G-V2-012: Setup/search must support optional Player Target Level.

REQ-G-V2-013: Setup/search must support optional Opponent Target Level.

REQ-G-V2-014: Setup/search must include a checkbox or equivalent control: continue search until preferences are met.

REQ-G-V2-015: Setup/search must disclose that platform coaching is allowed only because it is disclosed, capped, logged, and rated into ECR.

REQ-G-V2-016: Setup/search must show token/subscription gate state where a game token or subscription is required.

## G.4 Search modes

REQ-G-V2-020: Normal search means the player chooses time control and lets EvenChess find a fair match with any allowed level pairing.

REQ-G-V2-021: Player Target Level search fixes or strongly prefers the player's own Set Level.

REQ-G-V2-022: Opponent Target Level search fixes or strongly prefers the opponent's Set Level.

REQ-G-V2-023: Both-target search fixes or strongly prefers both levels.

REQ-G-V2-024: If strict preference is enabled, search must continue until the requested criteria can be met or user cancels.

REQ-G-V2-025: If strict preference is disabled, search gradually widens according to the MMR Engine widening rules.

## G.5 Confirmation

REQ-G-V2-030: Before game start or at game start, both players must know the match contract: time control, each side's Set Level, rated/casual state, and relevant fairness disclosure.

REQ-G-V2-031: The game-start display should use native Lichess confirmation/status patterns.
