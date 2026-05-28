# Appendix V - Testing QA and Acceptance Criteria

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Defines test gates and release criteria.


## V.2 Baseline Lichess regression

TEST-L1-001: Local Lichess must boot before EvenChess features are safe.  
TEST-L1-002: Normal chess games must support legal moves, clocks, result flow, review/history.  
TEST-L1-003: EvenChess code must not break ordinary account/session behavior.  
TEST-L1-004: Upstream lila touches require normal chess regression checks.

## V.3 Cross-cutting gates

Required gates: invariant tests, level gates, server authority, Used Level monotonicity, every render audited, Offset Count fixtures, AI validator/prompt injection, Stockfish bounded profiles, ECR replay, Target isolation, mobile/accessibility QA, stale clearing, patch-map completeness.

## V.4 Stage 1 acceptance

Local lila boots; accounts/dev accounts work; local games work; normal chess baseline remains; EvenChess boundary exists; harmless mode flag displays; dummy server-authorized overlay works without advice; dummy audit event writes; AI mock exists if S1.8 completed; patch map updated; go/no-go report exists.

## V.5 Rated EvenChess acceptance

Requires server policy, L0-L10 gates, Assistance/Used Offset replay, ECR isolation, Target isolation, Offset Count tests, engine security, AI validators, client cannot bypass permission, subscription/ad/token non-effect, calibration dashboards, no hidden contradictions.

## V.6 Marketing and ops acceptance

Requires correct tokens, ad cap, consumption/refund, pricing display, Premium non-strength, backend landing config, UTM/events, kill switches, unsafe copy scan, event dedupe, engine/AI health, fallback/degraded states, stale clearing, no-rate/annul path, rollback, incident playbook, patch map current.

## V.8 Final acceptance

All appendices current; Appendix Z current; patch map current; upstream sync documented; full regression passes; no contradiction unreported; normal chess separate; EvenChess invariants pass; go/no-go approved.
