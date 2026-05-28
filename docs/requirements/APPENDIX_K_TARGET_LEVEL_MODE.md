# Appendix K - Target Level Mode

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Defines Target Level practice mode and isolation.


TARGET-L1-001: Target Level is player-selected practice context, not Rating Level and not normal ECR input.  
TARGET-L1-002: MVP Target games set `normal_ecr_changed=false` and update only hidden Target shadow rating if enabled.  
TARGET-L1-003: Target Mode must not search Normal EvenChess queue.  
TARGET-L1-004: Adjacent Target Level widening requires explicit UI confirmation.  
TARGET-L1-005: Target Mode is labelled in lobby, header, result, and review.  
TARGET-L1-006: Target games do not contribute to normal ECR leaderboard eligibility.

| Area | Requirement |
| --- | --- |
| Queue | Separate from Normal EvenChess. |
| Rating | No normal ECR update in MVP. |
| Shadow rating | Optional hidden target shadow rating. |
| Coaching | Uses target Set Level policy; still server-authorized/audited. |
| Tokens | Product-owner decision; may consume resources but cannot affect fairness. |
| Summaries | Reviewable; performance online-game windows exclude non-online/target/bot unless approved. |
| Disclosure | Both players see Target Mode and level contract. |

Stage 1 should only preserve taxonomy/metadata space, not implement full Target Mode. Tests: no ECR mutation, queue separation, widening confirmation, UI visibility, summary non-mutation, token/subscription non-effect.
