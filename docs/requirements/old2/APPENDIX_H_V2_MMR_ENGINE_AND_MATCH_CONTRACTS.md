# Appendix H — EvenChess MMR Engine and Match Contracts

## H.1 Purpose

This appendix defines the EvenChess MMR Engine and how it replaces ordinary public Lichess matchmaking behavior while using Lichess for the game shell.

## H.2 Architecture

REQ-H-V2-001: EvenChess public matchmaking must be owned by the EvenChess MMR Engine, not ordinary Lichess rating pools.

REQ-H-V2-002: The MMR Engine should output a match contract, not merely an equivalent Lichess rating.

REQ-H-V2-003: Lichess should receive the finalized pairing/game contract and provide board, clocks, game lifecycle, live state, and result mechanics.

REQ-H-V2-004: Any use of existing Lichess pairing infrastructure must be adapted through EvenChess search contracts and must not mix normal ratings with ECR.

## H.3 Match contract fields

A match contract should include:

- game id / request id;
- time control;
- rated/casual flag;
- white player id;
- black player id;
- white ECR/MMR;
- black ECR/MMR;
- white Set Level;
- black Set Level;
- white expected offset;
- black expected offset;
- white effective rating;
- black effective rating;
- match-quality score;
- preference-match flags;
- strict/widened search flag;
- token/subscription gate result;
- policy version.

REQ-H-V2-010: Effective rating is `ECR + expected offset` for matchmaking.

REQ-H-V2-011: Expected offset is based on assigned/planned Set Level and calibration model, not actual Used Level after the game.

REQ-H-V2-012: Used Offset after the game may differ from expected offset and is used for rating settlement/calibration.

## H.4 Four search scenarios

REQ-H-V2-020: Scenario 1 — Normal search: no fixed level preferences. The engine chooses level pairings that produce fair effective-rating matches.

REQ-H-V2-021: Scenario 2 — Player Target Level only: player's level is fixed/preferred; engine searches opponent candidates and opponent-level combinations that create a fair effective-rating match.

REQ-H-V2-022: Scenario 3 — Opponent Target Level only: opponent level is fixed/preferred; engine searches candidates where that opponent level is allowed, then chooses the player's level if needed to balance.

REQ-H-V2-023: Scenario 4 — Both target levels: both levels are fixed/preferred; engine matches on effective rating using those fixed levels.

## H.5 Search widening

REQ-H-V2-030: Strict preference enabled means do not relax target-level criteria; continue until match or cancellation.

REQ-H-V2-031: Strict preference disabled means widen gradually.

REQ-H-V2-032: Widening order should be: ECR window, effective-rating window, then level-preference tolerance.

REQ-H-V2-033: Widening must be visible enough that the player understands if preferences were relaxed.

## H.6 Anti-abuse

REQ-H-V2-040: Repeated pairings, collusion patterns, abort abuse, and level-target manipulation must be monitored.

REQ-H-V2-041: Match contracts must be logged for audit and calibration.

REQ-H-V2-042: The MMR Engine must support simulation tests before production rollout.
