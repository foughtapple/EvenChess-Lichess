# Appendix N — Coaching Ladder, Overlay, and Text Level Gates

## N.1 Purpose

This appendix consolidates level-gating rules for display and ECE output.

## N.2 Level gates

| Level | Name | Live display behavior |
|---|---|---|
| L0 | Standard Board | No coaching; normal board only. |
| L1 | Rules | Rule/legal state only where surfaced. |
| L2 | Safety | Hanging/loose pieces, basic danger. |
| L3 | Offset Count | Exchange Resolver / take-take-take overlays. |
| L4 | Pattern Coach | Threats, pins, motifs, short Summary/Plan. |
| L5 | Single Hint | First candidate move. |
| L6 | Choice Coach | Two candidate moves/comparison. |
| L7 | Guided Engine | Up to three candidates and compact plan cue. |
| L8 | Precision | Numeric eval/WDL/proof where available. |
| L9 | Expert Sparring | Why-not/branch comparison/proposed-move depth. |
| L10 | Full Co-pilot | Maximum compact specificity; no text flood. |

## N.3 Board Summary

REQ-N-V2-001: Board Summary must be a short paragraph tuned to level.

REQ-N-V2-002: Board Summary may include board situation, hanging pieces, threats, pins, potential forks/combinations, exchange warnings, and other deterministic facts allowed by level.

REQ-N-V2-003: Board Summary must name pieces and squares clearly when useful, for example "the knight on e4 is hanging".

REQ-N-V2-004: Board Summary must not become a long engine report.

## N.4 Plan

REQ-N-V2-010: Plan should describe the medium-term aim over several moves.

REQ-N-V2-011: Plan must be level-gated and compact.

REQ-N-V2-012: Plan should differ from Summary: Summary is current/near-term state; Plan is what to aim for.

## N.5 Overlay tuning

REQ-N-V2-020: Each overlay must have clear level gating.

REQ-N-V2-021: The same raw ECE fact may appear differently by level.

REQ-N-V2-022: Higher levels may increase specificity but must not overload the board.
