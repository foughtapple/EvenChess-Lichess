# Appendix A - Product Invariants and Vocabulary

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Defines invariant product rules, vocabulary, public positioning boundaries, and player-mode definitions.


## Non-negotiable EvenChess invariants

- EvenChess is a disclosed assisted chess variant, not ordinary chess with hidden help.
- Lichess/lila provides the mature chess platform foundation; EvenChess adds the assisted-chess mode layer.
- Platform coaching is legal only because it is disclosed, capped by Set Level, logged by the server, and priced into ECR.
- Non-platform guidance remains prohibited in rated EvenChess, including external engines, humans, bots, browser extensions, notes, stream chat, and unaudited analysis.
- All public coaching levels L0-L10 may be used in rated EvenChess when assigned or allowed by the platform; high levels are priced, audited, and calibrated, not hard-banned.
- Client-side code must never decide coaching permission. The server decides.
- Stockfish remains server-side only for live EvenChess assistance. The client never receives unrestricted raw engine access.
- AI explains and compresses server-authorized truth packets. Hard-coded logic, legal move validation, stored state, and Stockfish provide chess truth.
- Higher levels improve specificity, timing, and candidate precision, not live text volume.
- Used Level may increase during a game but must never decrease.
- Every coaching render, suppression, expansion, and block must be audited.
- Assistance Load, Used Level, Used Offset, and ECR calibration are core fairness requirements.
- Target Level mode must not update or corrupt normal ECR.
- Live rated play should avoid labelled "best move" wording unless explicitly approved later.
- Subscription, ads, tokens, marketing copy, landing variants, campaign configuration, and launch windows must not bypass rated fairness.
- Premium must never be described or implemented as stronger live help.
- Marketing configuration must not alter Set Level, Used Level, Assistance Load, Used Offset, ECR, matchmaking fairness, coaching permission, Stockfish profile, or live coaching strength.
- Offset Count is the existing Exchange Resolver / take-take-take feature, not a missing feature.
- Offset Count display rule: shield/blue/0 = equal trade, green = student wins material, red = opponent wins material.

## A.2 Canonical public claim

PUB-L1-001: EvenChess must be publicly described as a separate rated assisted chess variant where platform-delivered guidance is allowed only because it is disclosed, capped by Set Level, logged by the server, and accounted for in ECR.  
PUB-L1-002: Public copy must not describe the product as "cheating allowed", "secret engine use", "normal chess with help", or "pay to win".  
PUB-L1-003: Public rules must plainly prohibit external engines, coaches, friends, stream chat, notes, browser extensions, bots, unaudited analysis, and other non-platform guidance during rated EvenChess.  
PUB-L1-004: Required disclosure surfaces include lobby, queue, game-start confirmation, persistent game header, result screen, review/history, fair-play report flow, onboarding, FAQ, pricing, and landing pages.

## A.3 Vocabulary

| Term | Definition | Authority |
| --- | --- | --- |
| EvenChess | The disclosed assisted chess mode added by this fork. | Product invariant. |
| Normal chess | Ordinary chess without EvenChess platform coaching. | Provided by Lichess and must remain separate. |
| ECR | Stored estimate of underlying human skill in an EvenChess rated pool after assistance correction. | Appendix J. |
| Rating Level | Platform-assigned default or recommended coaching level derived from ECR/policy. | Appendix J. |
| Set Level | Server-authorized maximum assistance level for a game. Permission, not actual use. | Appendix G. |
| Used Level | Highest/strongest public coaching level actually delivered or consumed; never decreases. | Appendix I. |
| Assistance Load | Continuous dose of assistance from feature, exactness, surface, timing, latency, criticality, visibility, follow-rate, and quality. | Appendix I. |
| Used Offset | Rating-point correction assigned to actual assistance consumed. | Appendix I/J. |
| Effective Rating | Game-specific playing strength: ECR + Used Offset. | Appendix J. |
| Target Level | Player-selected assistance target for Target Level mode; not Rating Level and not normal ECR input. | Appendix K. |
| Exactness class | Internal axis for concept, zone, piece, candidate, line, eval, proof, tablebase precision. | Appendix E/G. |
| Candidate follow-rate | Whether surfaced candidate/idea was followed, avoided, or ignored when validly visible. | Appendix P. |
| Offset Count | Existing Exchange Resolver / take-take-take local exchange feature. | Appendix H. |
| Coaching render | Any visible, hidden, suppressed, expanded, blocked, or stale coaching artifact decision. | Appendix G. |
| Truth packet | Server-generated chess facts supplied to AI for compression/explanation. | Appendix M. |

## A.4 Relationship rules

VOC-L1-001: Set Level is permission; Used Level is actual use.  
VOC-L1-002: Assistance Load is measured dose; Used Offset is rating correction.  
VOC-L1-003: Effective Rating is transient and game-specific; ECR is persistent per rated pool.  
VOC-L1-004: Target Level is not Rating Level.  
VOC-L1-005: AI text is not chess authority; it is a presentation layer over server-authorized truth packets.  
VOC-L1-006: Lichess-provided account/game/board/rules capabilities are not EvenChess-specific unless explicitly stated.

## A.5 Player modes

| Mode | Purpose | Rating effect | Implementation direction |
| --- | --- | --- | --- |
| Normal EvenChess | Main rated assisted mode. | Updates ECR after Used Offset. | Separate mode/layer and pool; normal chess unaffected. |
| Casual EvenChess | Flexible assisted play. | No public ECR update. | Can reuse Lichess casual patterns with EvenChess metadata. |
| Target Level Mode | Player-selected practice level. | No normal ECR update in MVP. | Separate queue and metadata. |
| AI/Bot practice | Training/drill play. | Rating neutral. | May reuse Lichess AI/bot paths if safe. |
| Post-game review | Learn after completed game. | Rating neutral. | Integrate with analysis/review; no live mutation. |
| Normal Lichess chess | Ordinary chess in the fork. | Normal fork rating only if retained. | No EvenChess assistance. |
| Future classroom/coach | Coach-led learning. | No normal ECR by default. | Reserved. |

## A.6 Marketing and account invariants

INV-L1-020: New eligible accounts receive 10 game tokens, 3 full-quality match-summary tokens, and 1 full-quality performance-summary token unlocked after 10 completed games.  
INV-L1-021: Free users may earn game tokens by rewarded ads; free accounts may hold at most 3 earned ad game tokens.  
INV-L1-022: A game token is consumed only when a valid game starts and reaches meaningful play; opponent abort before meaningful play does not consume the token.  
INV-L1-023: Standard and Premium must not change rated live strength or rating fairness.  
INV-L1-024: Premium adds review/summary quotas, not stronger live coaching.

## A.7 Product-owner decision guardrail

When a new idea changes an invariant, Codex must identify the invariant, state old and new requirement, ask for approval, update Appendix Z if approved, and then implement only the approved scope.
