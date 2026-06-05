# Appendix I — ECR Rating Settlement and Calibration

## I.1 Purpose

This appendix defines EvenChess ECR/MMR and rating settlement.

## I.2 ECR definition

REQ-I-V2-001: ECR estimates underlying human skill after accounting for platform assistance.

REQ-I-V2-002: ECR is distinct from normal Lichess rating and must not be mixed with normal Lichess public rated pools.

REQ-I-V2-003: Separate pools may exist by time-control category if launched.

## I.3 Matchmaking vs settlement

REQ-I-V2-010: Matchmaking uses expected offset based on Set Level and calibration assumptions.

REQ-I-V2-011: Rating settlement uses actual game outcome and actual Used Level / Assistance Load / Used Offset.

REQ-I-V2-012: Used Level is the highest level actually delivered/consumed and never decreases during the game.

REQ-I-V2-013: If the player raises level mid-game and ECE returns higher-level output, the game must record the higher Used Level.

## I.4 Calibration

REQ-I-V2-020: Calibration must track residuals by ECR band, Set Level, Used Level, Assistance Load, Used Offset, time control, feature mix, and candidate follow-rate where available.

REQ-I-V2-021: Initial offset values may be heuristic but must be versioned and recalibrated.

REQ-I-V2-022: Changes to offset tables/models must be policy-versioned.

REQ-I-V2-023: Rating settlement must record pre/post rating snapshots and model version.
