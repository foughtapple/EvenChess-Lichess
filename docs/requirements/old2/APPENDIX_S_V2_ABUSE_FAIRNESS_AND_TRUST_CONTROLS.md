# Appendix S — Abuse, Fairness, and Trust Controls

## S.1 Purpose

This appendix defines abuse/fairness controls for a disclosed assisted chess platform.

## S.2 Non-platform help

REQ-S-V2-001: Rated EvenChess prohibits outside engines, humans, bots, browser extensions, unaudited notes, and stream chat.

REQ-S-V2-002: Public rules must explain that only platform-delivered coaching is legal.

## S.3 Matchmaking abuse

REQ-S-V2-010: Monitor repeat pairings, collusion, rating transfer, target-level manipulation, abort abuse, and queue sniping.

REQ-S-V2-011: MMR Engine must support repeat-opponent caps/flags.

REQ-S-V2-012: Strict preference search must not become a collusion loophole.

## S.4 Token/review abuse

REQ-S-V2-020: Prevent token farming where practical without adding intrusive MVP controls unless approved.

REQ-S-V2-021: Custom analysis tokens should rate-limit high-cost L10/full-game analysis.

REQ-S-V2-022: Paid status must not change live rated fairness.

## S.5 Engine/AI abuse

REQ-S-V2-030: ECE and AI must reject/ignore custom instructions that request hidden, forbidden, or higher-level information.

REQ-S-V2-031: AI output must be validated for forbidden best-move wording where policy prohibits it.

REQ-S-V2-032: Stockfish raw output must not be exposed.
