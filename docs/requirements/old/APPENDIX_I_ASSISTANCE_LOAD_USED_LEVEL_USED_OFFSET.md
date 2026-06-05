# Appendix I - Assistance Load, Used Level, and Used Offset

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Defines assistance measurement and rating offset mechanics.


## I.2 Core requirements

ASSIST-L1-001: Used Level may increase but never decrease.
ASSIST-L1-002: Background prefetch or hidden help does not count as Used Level.
ASSIST-L1-003: Assistance Load sums feature weight, exactness, surface, time control, criticality, clock pressure, visibility, follow-rate, and quality multipliers.
ASSIST-L1-004: Used Offset is calculated from Used Level, Assistance Load, time control, exactness, feature mix, and model version.
ASSIST-L1-005: Used Offset must be recomputable from the audit ledger.
ASSIST-L1-006: Assistance accounting is server-authoritative.

## I.3 Conceptual formula

`Assistance Load = sum(feature_weight * exactness_multiplier * surface_multiplier * time_control_multiplier * criticality_multiplier * clock_pressure_multiplier * visibility_multiplier * follow_multiplier * quality_multiplier)`

Constants are calibration parameters, not permanent hard-coding.

## I.4 Multipliers

| Dimension | Examples | Notes |
| --- | --- | --- |
| Feature weight | legal target, safety ring, Offset Count, candidate card, eval proof | Base from registry. |
| Exactness | concept, zone, piece, candidate, line, eval, proof | More exact usually weighs more. |
| Surface | board arrow, warning card, plan card, review | Live board cues often stronger. |
| Time control | bullet, blitz, rapid, classical, correspondence | Reading burden differs. |
| Criticality | tactical swing, mate, tablebase, blunder point | High-leverage moments need calibration. |
| Clock pressure | low time/increment | Low-clock help can be stronger. |
| Visibility | shown, expanded, hidden, stale | Hidden does not count. |
| Follow-rate | followed, avoided, ignored | Calibration signal. |
| Quality | degraded, fallback, normal, exact proof | Engine/AI degradation affects load. |

## I.5 Used Level and Offset rules

USED-L1-001: Used Level starts at L0 per player per game.
USED-L1-002: It updates only when assistance is delivered/consumed.
USED-L1-003: If L5 is viewed then later only L2 is used, Used Level remains at least L5.
USED-L1-004: Stale non-decision-help must not raise Used Level as timely help.
USED-L1-005: Post-game review cannot change live Used Level.
OFFSET-L1-001: Expected score uses Effective Rating = ECR + Used Offset.
OFFSET-L1-002: Used Offset is a fairness correction, not punishment.
OFFSET-L1-003: Used Offset uses a versioned calibration model and cannot be lowered by subscription/ad/token/marketing state.

## I.6 Integration and tests

Assistance aggregates may be stored outside core game records if joinable to game/player/rating. Core rating flow edits must be patch-mapped. Required tests include monotonicity, hidden prefetch, stale payload, offline recomputation, expected-score impact, subscription/ad/token non-effect, per-player separation, and review non-mutation.
