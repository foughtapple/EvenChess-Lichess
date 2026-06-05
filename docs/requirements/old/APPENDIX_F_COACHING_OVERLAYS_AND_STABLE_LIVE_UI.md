# Appendix F - Coaching Overlays and Stable Live UI

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Adapts board-first live UI and accessibility requirements to the Lichess fork.


## F.2 UI principles

UI-L1-001: Board-first; the board remains the primary play surface.
UI-L1-002: One active card and one primary visual idea by default.
UI-L1-003: Text budgets enforced by UI/schema, not AI discretion.
UI-L1-004: Mobile keeps board primary and uses bottom/adjacent card/sheet.
UI-L1-005: Color must never be the only signal. Offset Count needs icon/text redundancy.
UI-L1-006: Accessibility features are baseline and not charged as coaching.
UI-L1-007: Client navigation is display-only; server decides state, tokens, coaching, result, audits.

## F.3 Stable UI surfaces

| Surface | Purpose | Default | Higher-level expansion |
| --- | --- | --- | --- |
| Board layer | Primary play surface | Board, clocks, move list | Legal dots, rings, arrows, eval chip by level. |
| Summary card | Game/coach headline | Exact state | Safety summary and source badges. |
| Warning card | Critical warning | Collapsed headline | Reason/line if allowed. |
| Plan card | Strategic idea | Hidden/minimal | Motif, plan, counterplay tabs. |
| Opening/Endgame card | Phase guidance | Compact | Tablebase/proof when available. |
| Candidate area | Candidate moves | Hidden before L5 | 1/2/3 candidates and comparisons. |
| AI Explain | Grounded explanation | Hidden/compact | Validated concise explanation. |
| Post-game review | Rating-neutral review | After game | Deep review/drills. |

## F.4 Live board layout

UI-L1-010: Desktop live play should form a three-column rectangle: level controls left, board/eval/actions centre, Moves/Coach/Chat/Search right.
UI-L1-011: Side cards should align vertically with board/header/action area.
UI-L1-012: Board header combines live/casual/rated/time-control/player status compactly.
UI-L1-013: Eval bar, if shown, should align with board top/bottom and have accessible alternative.
UI-L1-014: Draw/takeback/add-time/report/resign controls should sit directly under board where available.
UI-L1-015: Keyboard move entry defaults off for new accounts.
UI-L1-016: Mobile makes board full phone width where possible with Moves/Coach/Chat/Search/Level tabs.

## F.5 Overlay rules

UI-L1-020: Overlays must not interfere with legal move input, premoves, drag/drop, keyboard entry, or accessibility.
UI-L1-021: Candidate arrows, rings, Offset markers, and warning badges clear on move, board mismatch, stale payload, or suppression.
UI-L1-022: Overlay payloads include game ID, ply, board state key, perspective, feature key, level, visibility, TTL/staleness, audit ID.
UI-L1-023: Client must not construct stronger help by combining hidden/debug data.
UI-L1-024: Raw Stockfish lines may not be shown unless server sends approved display payload.

## F.6 Landing and review surfaces

UI-L1-030: Landing supports Hero, Trust strip, difference section, How it works, Product proof, Pricing, FAQ, Final CTA.
UI-L1-031: Landing copy and offer/pricing/FAQ/variant/ad/summary/play-window wording comes from marketing config, not frontend-only hardcoding.
UI-L1-032: Public pricing shows four-week billed amount first and weekly equivalent second, with fairness footnote.
UI-L1-033: Top-of-page Create account and Log in controls are required.
UI-L1-040: Review displays result/termination at top and behaves like a post-game board experience.
UI-L1-041: Review-legal coaching options live in Coach/Level interactions and must not mutate live fairness state.
