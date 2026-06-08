# Appendix L — Live Game Assistance and ECE History

## L.1 Purpose

This appendix defines live ECE usage, Used Level handling, and per-turn ECE history.

## L.2 Live ECE calls

REQ-L-V2-001: After each live move, the server must request or schedule ECE board-state output for the new FEN according to each player's current authorized level.

REQ-L-V2-002: The ECE payload returned to UI must be split by side and must not expose the opponent's unauthorized private view.

REQ-L-V2-003: If a player raises their live level within allowed policy, ECE must be called again for that position at the higher level.

REQ-L-V2-004: If a lower-level result and higher-level result exist for the same turn/player, the higher-level result is canonical for Used Level and review history.

REQ-L-V2-005: Lowering visible level does not reduce Used Level for that game.

## L.3 ECE history model

REQ-L-V2-010: Each game should store an EvenChess game history record containing FEN and ECE output metadata per ply/turn.

REQ-L-V2-011: The stored history should identify side-to-move, levels requested, levels delivered, position hash, policy version, ECE version, and output references.

REQ-L-V2-012: Full raw ECE history may be retained only according to retention/token/subscription policy.

REQ-L-V2-013: If storage must be limited, store enough to reconstruct: FEN list, moves, highest level used per side/per turn, summary/plan text, overlay essentials, and audit atoms.

## L.4 Live display

REQ-L-V2-020: Live display uses the player's side and current authorized view.

REQ-L-V2-021: Live display must not allow side switching.

REQ-L-V2-022: Live display must clearly distinguish actual position output from proposed-move preview output.

REQ-L-V2-023: Payloads that arrive stale must be discarded or marked stale and must not be displayed as current advice.
