# Appendix J - ECR Rating, Calibration, and Matchmaking

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Defines ECR, Effective Rating, calibration, and matchmaking.


## J.2 ECR definition

ECR-L1-001: ECR represents underlying human skill after accounting for assistance used in prior rated EvenChess games.  
ECR-L1-002: ECR is not normal chess Elo and must be labelled as EvenChess Rating/ECR.  
ECR-L1-003: Expected score uses Effective Rating = ECR + Used Offset.  
ECR-L1-004: Store rating, RD, volatility, game count, provisional status, pool, timestamps, and policy versions per rated pool.  
ECR-L1-005: Maintain separate bullet/blitz/rapid/classical/correspondence pools if launched; casual and Target MVP do not update normal ECR.  
ECR-L1-006: ECR updates must be server-side and auditable.

## J.3 Pool isolation

| Pool | Version 1 status | Notes |
| --- | --- | --- |
| EvenChess rapid | Primary likely launch pool | Start here unless changed. |
| EvenChess blitz | Optional launch/beta | Needs compact UI calibration. |
| EvenChess bullet | Later unless approved | High assistance timing risk. |
| EvenChess classical | Optional | Lower time-saving but exact help strong. |
| EvenChess correspondence | Later/uncertain | Different model. |
| Target shadow rating | Hidden only if enabled | Must not update normal ECR. |
| Normal Lichess rating | Separate | Must not be polluted by EvenChess. |

## J.4 Matchmaking

MATCH-L1-001: Normal EvenChess matchmaking uses ECR, expected offset, time control, level compatibility, latency, abuse controls.  
MATCH-L1-002: Search widening gradually widens ECR and Effective Rating windows.  
MATCH-L1-003: Widening that changes material level contract requires confirmation.  
MATCH-L1-004: Repeat rematches/rating transfer patterns are monitored and capped/flagged.  
MATCH-L1-005: Game-start confirmation displays Set Level and time-control pool.  
MATCH-L1-006: Post-game reporting explains Used Level and Used Offset.

## J.5 Search stages

| Stage | Preferred rule |
| --- | --- |
| Initial | Same time control, tight ECR and expected Effective Rating. |
| Widening 1 | Expand ECR range first. |
| Widening 2 | Expand expected Effective Rating range. |
| Widening 3 | Expand level compatibility only with confirmation. |
| Confirmation | Display Set Level, pool, rated/casual, outside-help rule. |
| Post-game | Show Used Level, Used Offset, rating trace. |

## J.6 Calibration

CAL-L1-001: Dashboards show residuals by Used Level, Assistance Load, time control, ECR band, exactness, feature mix, follow-rate.  
CAL-L1-002: ECR model changes are versioned.  
CAL-L1-003: Old games remain explainable under original model version.  
CAL-L1-004: L8-L10 should not be public rated until calibration/safety metrics are acceptable, but are not hard-banned by policy.

## J.7 Integration and tests

Inspect lila rating/pairing before seams. If first version cannot safely integrate, implement EvenChess-only rating service separate from normal ratings. Patch-map core rating/pairing edits. Tests: Effective Rating, pool isolation, provisional/inactivity if used, Target isolation, subscription non-effect, search widening, repeat/collusion, rating replay.
