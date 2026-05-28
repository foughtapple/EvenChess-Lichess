# Appendix C - Lichess-Provided Platform Capabilities

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Identifies custom-platform requirements now provided by Lichess/lila and therefore not to rebuild.


## C.2 Capabilities not to rebuild

| Capability | Classification | EvenChess action |
| --- | --- | --- |
| Legal chess rules and move legality | Lichess-provided | Use lila/scalachess/chessops; do not write a new legal move engine. |
| Standard board UI | Lichess-provided | Add overlays; do not replace chessground for normal play. |
| Base game lifecycle | Lichess-provided | Hook for metadata/audit only. |
| Clocks and time controls | Lichess-provided baseline | Use existing clocks; add assistance multipliers/stale-help logic. |
| Move list, PGN, replay/history | Lichess-provided baseline | Add assisted metadata/review; do not rebuild ordinary PGN. |
| Game rooms/play pages | Lichess-provided baseline | Add mode routing/disclosures. |
| Challenges/seeks/matchmaking base | Lichess-provided baseline | Adapt for ECR, Set Level, level compatibility. |
| Accounts/session basics | Lichess-provided baseline | Verify email/password/username gaps; add EvenChess token/subscription metadata. |
| Chat and standard controls | Lichess-provided baseline | Keep unless EvenChess-specific restriction is required. |
| Analysis/review foundation | Lichess-provided baseline | Adapt summaries and audit-aware review. |
| Normal ratings | Lichess-provided for ordinary chess | Do not use as ECR unless approved. |
| Mobile/responsive chess play | Lichess-provided baseline | Add mobile overlay/card behavior. |

## C.3 Gap verification areas

| Area | Likely Lichess baseline | EvenChess gap |
| --- | --- | --- |
| Account creation | Existing account system. | One active account/email, reused email token lockout, onboarding tokens. |
| Rating pools | Existing rating logic. | ECR, Effective Rating, Used Offset, assisted pool isolation. |
| Engine analysis | Existing analysis/fishnet/tablebase paths. | Live server-authorized assistance with gates/audit. |
| Studies/lessons | Existing study/analysis features. | Future classroom not active Version 1. |
| Moderation | Existing moderation primitives. | Non-platform guidance, assistance abuse, token/ad abuse. |
| Payment | No paid-product assumption for fork. | Standard/Premium/rewarded-ad economy. |
| Landing pages | Existing public pages. | Marketing config, variants, UTM, conversion events. |

## C.4 Superseded requirements

C-L1-001: Custom chess server from scratch is superseded.  
C-L1-002: Custom legal move generation is superseded.  
C-L1-003: Custom primary board renderer is superseded.  
C-L1-004: Custom base game rooms/clocks/history/PGN are superseded.  
C-L1-005: Replacing normal Lichess chess with EvenChess is superseded.  
C-L1-006: Old custom UI specs must become overlay/surface requirements, not full replacements.

## C.5 Adapted requirements

Time-control logic remains for assistance multipliers and stale-help. Matchmaking remains for ECR/offset/level compatibility. Account rules remain for tokens/subscriptions. Review and summaries remain but should integrate with existing review. Operations remain for engine/AI/fairness/marketing health.

## C.6 Implementation rule

Before writing code that looks like chess-platform infrastructure, Codex must state why Lichess does not already provide it. If no specific gap is identified, stop.
