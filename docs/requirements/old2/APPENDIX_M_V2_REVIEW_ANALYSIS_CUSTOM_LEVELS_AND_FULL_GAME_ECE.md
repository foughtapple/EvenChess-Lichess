# Appendix M — Review, Analysis, Custom Levels, and Full-Game ECE

## M.1 Purpose

This appendix defines replay/review analysis modes using stored live ECE history and optional custom/full-game ECE analysis.

## M.2 Live review modes

REQ-M-V2-001: Review must support Live White: show White's saved live perception at the level actually used on each turn.

REQ-M-V2-002: Review must support Live Black: show Black's saved live perception at the level actually used on each turn.

REQ-M-V2-003: Review must support Live Both: switch perception based on whose turn it was.

REQ-M-V2-004: Live review modes use saved ECE history where available and should not consume custom analysis tokens.

## M.3 Custom review modes

REQ-M-V2-010: Review must support Custom mode with selectable White level and Black level.

REQ-M-V2-011: Custom mode must support viewing from White, Black, or Both/side-to-move perspective.

REQ-M-V2-012: Custom mode may require custom ECE analysis tokens, especially if levels exceed live stored outputs or use L10/AI.

REQ-M-V2-013: Custom mode must cache generated custom analysis by game, levels, perspective mode, ECE version, and policy version.

## M.4 Full-game ECE mode

REQ-M-V2-020: Full-game ECE review consumes whole-game input and produces a level-10-capable review for both sides.

REQ-M-V2-021: Full-game input should support PGN plus FEN/move history plus any saved live ECE outputs.

REQ-M-V2-022: Full-game output may include game summary, player performance summary, turning points, recurring motifs, missed threats, and level-10 overlay/review data by move.

REQ-M-V2-023: Full-game review should use deterministic ECE facts and at most one large AI call for narrative compression where feasible.

REQ-M-V2-024: Full-game review requires token/quota checks and must not retroactively alter the rated game's live Used Level or ECR settlement.

## M.5 Saved games

REQ-M-V2-030: Users may save review games according to tier/storage rules.

REQ-M-V2-031: Paid saved games remain saved after downgrade, but new saves may require an active eligible tier.

REQ-M-V2-032: Retention limits must be visible and configurable.
