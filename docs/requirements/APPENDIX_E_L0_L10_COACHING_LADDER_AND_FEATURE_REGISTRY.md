# Appendix E - L0-L10 Coaching Ladder and Feature Registry

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Preserves and adapts the public L0-L10 ladder and feature registry.


## E.2 Ladder requirements

LVL-L1-001: L0 must not surface advisory coaching.  
LVL-L1-002: L3 is Offset Count / Exchange Resolver, not a missing feature.  
LVL-L1-003: L5 is the first live engine-backed candidate level.  
LVL-L1-004: Centipawns/WDL appear first at L8 and must be labelled approximate.  
LVL-L1-005: L10 may unlock maximum specificity but must remain compact by default.  
LVL-L1-006: All L0-L10 may be used in rated EvenChess when assigned or allowed.  
LVL-L1-007: Higher levels improve specificity and timing, not text volume.

## E.3 Final ladder

| Level | Name | Purpose | Allowed surfaces | Source/exactness | Base weight |
| --- | --- | --- | --- | --- | --- |
| L0 | Standard Board | No coaching baseline | Board, clocks, move list, result state | None | 0 |
| L1 | Legal Moves | Rules clarity | Legal destinations and exact rule states | Hard-coded exact rules | 2 |
| L2 | Safety Scanner | Local attack/defence awareness | Attacked/defended squares, hanging-piece badges, safety rings | Local deterministic | 5 |
| L3 | Offset Count | Exchange calculation | Existing Exchange Resolver / take-take-take card | Local exchange estimate | 9 |
| L4 | Pattern Coach | Motif awareness without engine advice | Pins/forks/back-rank/critical square concepts | Heuristic/local motif; AI may compress facts | 14 |
| L5 | Single Hint | One on-demand engine-backed candidate | One candidate cue with short reason | Bounded Stockfish MultiPV=1 | 20 |
| L6 | Choice Coach | Two-candidate comparison | Two candidate cues and hidden short line | Bounded Stockfish MultiPV=2 | 28 |
| L7 | Guided Engine | Three candidates and compact plan cue | Up to three cues and plan card | Bounded Stockfish MultiPV=3 | 37 |
| L8 | Precision Engine | Numeric eval and exact proof badges | Eval/proof markers | Approx eval; exact tablebase where configured | 47 |
| L9 | Expert Sparring | Deeper contrast and why-not analysis | Branch compare and taboo-move marker | Bounded MultiPV=4/searchmoves | 58 |
| L10 | Full Co-pilot | Maximum disclosed assistance in stable UI | Counterplay and collapsible tabs | Dynamic bounded profiles; richest specificity | 70 |

## E.4 Feature registry schema

Every feature row must define `feature_key`, `display_name`, `unlock_level`, `category`, `source_type`, `exactness_class`, `ui_slot`, `assistance_weight`, `audit_required`, `telemetry_required`, `rated_allowed`, `subscription_visibility`, `implementation_notes`, and `tests_required`.

FEAT-L1-001: Features without audit metadata must not be enabled in rated EvenChess.  
FEAT-L1-002: Subscription visibility may affect marketing/quota only, never live rated strength.  
FEAT-L1-003: Rated_allowed means server policy may allow it; the client cannot self-enable it.

## E.5 Base registry

| Feature key | Unlock | Source | UI slot | Notes |
| --- | --- | --- | --- | --- |
| move_history | L0 | Hard-coded exact | Board/move list | Standard move list and PGN basis. |
| legal_targets | L1 | Hard-coded exact | Board layer | Legal move dots/highlights. |
| material_panel | L1-L2 | Hard-coded exact/local | Summary | Displays material state; not engine advice. |
| loose_pieces | L2 | Hard-coded local | Board/warning | Undefended/tactically loose cue. |
| king_safety | L2-L4 | Hard-coded/hybrid | Warning | Simple king danger. |
| offset_count | L3 | Hard-coded SEE/local | Offset card | Existing Exchange Resolver; shield/blue/green/red semantics. |
| pins | L4 | Hard-coded/hybrid | Board/warning | Absolute pins affect legality; relative pins are warnings. |
| x_rays | L4-L5 | Hard-coded/hybrid | Board/Offset | Relevant to Offset Count and motifs. |
| student_threats | L4+ | Hybrid heuristic | Plan/warning | Threats without exact move below allowed level. |
| opponent_threats | L4+ | Hybrid/Stockfish | Warning | Higher impact under clock pressure. |
| pressure_markers | L4+ | Hard-coded/hybrid | Board/plan | File/diagonal/square pressure. |
| summary | L1+ | Hybrid mixed | Summary card | Compact coach headline and game state. |
| plan | L4+ | AI over truth packets/hybrid | Plan card | No AI invention. |
| opening_endgame | L4+ | Hybrid | Opening/endgame card | Tablebase exactness only where source exists. |
| coarse_eval_band | L6 | Stockfish approximate | Candidate/summary | No raw CP until L8. |
| move_advice | L5+ | Stockfish | Candidate area | First exact candidate at L5; avoid best-move label in rated live. |
| candidate_cards | L5+ | Stockfish + AI wording | Candidate area | Candidate count level-gated. |
| move_pool | L6+ | Stockfish | Candidate area | No raw unrestricted engine access. |
| eval_difference | L8+ | Stockfish approximate | Candidate/AI explain | Label approximate. |
| themes | L4+ | Hard-coded/AI wording | Coach/review | Used in review/drills. |
| warnings | L2+ | Hybrid local/engine | Warning card | Priority over summary/plan. |
| ai_explain | L5+/Review | AI over truth packets | AI explain | Must validate and cannot invent. |
| post_game_review | Post-game | Mixed | Review | Rating-neutral after game. |

## E.6 Fork adaptation

FEAT-L1-010: Features must be server-authorized payloads consumed by UI overlays.  
FEAT-L1-011: Chessground overlays must be fed by server payloads with board hash/FEN/ply validation.  
FEAT-L1-012: Registry should initially live in a simple EvenChess config/service.  
FEAT-L1-013: Registry must be testable offline with policy inputs.
