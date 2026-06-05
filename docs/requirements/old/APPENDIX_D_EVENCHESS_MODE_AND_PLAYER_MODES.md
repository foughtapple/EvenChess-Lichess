# Appendix D - EvenChess Mode and Player Modes

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Defines how EvenChess exists inside the fork as a separate assisted mode/layer.


## D.2 Mode identity

MODE-L1-001: EvenChess is a distinct game mode in the fork and must be visible before and during the game.
MODE-L1-002: EvenChess games must carry server-owned metadata for mode, rated/casual state, time-control pool, Set Level policy, player Set Levels, assistance policy version, ECR policy version, and audit ledger version.
MODE-L1-003: The server decides whether a game is EvenChess. Client flags are display-only.
MODE-L1-004: Normal chess must not receive EvenChess overlays, ECR updates, token consumption, or assistance logs unless server metadata makes it an EvenChess game.
MODE-L1-005: UI must never make a normal chess game appear assisted without server metadata.

## D.3 Primary modes

| Mode | Included? | Required behavior |
| --- | --- | --- |
| Normal rated EvenChess | Yes | Uses ECR, Set Level, Used Level, Assistance Load, Used Offset, audit ledger, rated result. |
| Casual EvenChess | Yes if low-risk | Same policy without public ECR update. |
| Target Level mode | Yes, isolated | Player-selected Target Level; no normal ECR update in MVP. |
| AI/Bot games | Stage 1/early testing if convenient | Rating-neutral and excluded from performance-summary online-game windows. |
| Post-game review | Yes | No live mutation; review-legal coaching may be deeper. |
| Normal Lichess chess | Yes, retained | No EvenChess assistance. |
| Future classroom/coach | No | Reserved. |

## D.4 Disclosure and routing

MODE-L1-010: Game creation must expose mode, time control, rated/casual state, Set Level or level-matching rules, and outside-help prohibition.
MODE-L1-011: Both players must see that the game is assisted EvenChess.
MODE-L1-012: Game-start confirmation displays Set Level and time-control pool.
MODE-L1-013: Post-game summary displays actual Used Level and Used Offset.
MODE-L1-014: Search widening that changes a material level contract requires confirmation.
MODE-L1-020: Online/search/challenge games belong in Live Games; computer games in AI Games; completed games in Review.
MODE-L1-021: Leaving an online game must not let the client decide the result.

## D.5 Time-control buckets

| Bucket | Definition | EvenChess meaning |
| --- | --- | --- |
| Bullet | Estimated duration <=179s | High live-assistance multiplier; visual over text. |
| Blitz | 180-479s | High multiplier; compact cards. |
| Rapid | 480-1499s | Neutral reference bucket. |
| Classical | >=1500s non-correspondence | Lower time-saving multiplier but exact help still strong. |
| Correspondence | Async/daily | Split model: low time-saving, high exact-candidate/tablebase value. |
| Casual | Non-rated | No public ECR effect. |

MODE-L1-030: Time controls may use different multipliers, UI rules, latency rules, Assistance Load rules, and calibration values.
MODE-L1-031: Help shown after premove commitment does not count as decision assistance for that committed move.
MODE-L1-032: Stale or late assistance must be marked and not charged as timely live decision help.

## D.6 Stage 1 mode flag only

Stage 1 adds a harmless server-owned EvenChess flag/metadata path only. No coaching/rating/token logic before local baseline and ledger foundation work.
