# Appendix W — Codex Phase Plan and Task Packets

## W.1 Purpose

This appendix defines implementation sequencing.

## W.2 Phase plan

| Phase | Name | Scope |
|---|---|---|
| A | Requirements V2 | Replace requirements with sewn-in V2 suite. |
| B | Current code audit | Identify addon-style work and current seams. |
| C | Branding/theme | Deep-blue EvenChess identity in native shell. |
| D | Top bar/tokens/settings shell | Token balances and EvenChess settings. |
| E | Setup/search UI | Add EvenChess controls to native play modal/cards. |
| F | MMR Engine framework | Search request -> match contract simulation. |
| G | Matchmaking integration | Route public search to EvenChess contracts. |
| H | ECE framework integration | Server gets ECE output for FEN/levels. |
| I | Display Engine framework | Render mock payload overlays/cards. |
| J | Live ECE history | Store per-turn ECE history and Used Level. |
| K | Proposed move | Single-arrow proposed move mode. |
| L | Review modes | Live White/Black/Both and custom mode framework. |
| M | Full-game ECE | Full-game review payload/token integration. |
| N | Stockfish/AI | Real bounded engine/AI providers. |
| O | Feature surfaces | Study, puzzles, openings, analysis, computer play. |
| P | Monetisation | Tokens, ads, subscriptions, saved games. |
| Q | Telemetry/audit | Calibration dashboards and event logging. |
| R | Abuse/ops | Trust controls, kill switches, incidents. |
| S | Regression hardening | Lichess regression + EvenChess acceptance. |
| T | Release candidate | Final integration and go/no-go. |

## W.3 Task packet rule

Each Codex task must state:

- phase;
- files to inspect first;
- exact allowed scope;
- likely seams;
- tests expected;
- patch-map requirement;
- rollback note requirement.

REQ-W-V2-001: Codex must not implement future phases during the current phase.

REQ-W-V2-002: Codex must not use `git add .` when unrelated changes may exist.
