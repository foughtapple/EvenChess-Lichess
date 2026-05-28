# Appendix U - Data Models and Integration Seams

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Defines logical data models and lila integration seams.


| Model | Required fields / purpose |
| --- | --- |
| EvenChessGamePolicy | game_id, mode, rated flag, time control, players, Set Levels, policy versions, feature flags. |
| EvenChessPlayerRating | player_id, pool_key, raw_ecr, RD, volatility, game count, provisional, timestamps, model version. |
| EvenChessGameAssistanceSummary | game_id, player_id, Used Level, Assistance Load, Used Offset, feature mix, model version. |
| CoachingFeature | registry metadata and gating from Appendix E. |
| CoachingRenderEvent | server audit of rendered/suppressed/expanded/blocked coaching artifact. |
| OffsetCountPayload | exchange resolver result payload. |
| EngineAnalysisJob | Stockfish job/profile/cache/fallback audit. |
| AIWordingRequest | AI prompt/schema/validation/cost audit. |
| TokenLedgerEntry | onboarding, ad-earned, consumed, refunded, reason, game link. |
| SummaryQuotaLedgerEntry | summary generation and token/quota consumption. |
| MarketingAttribution | UTM, click ID, variant, first/latest touch, account/subscription links. |
| PatchMapEntry | upstream file touched, requirement, risk, tests, isolation plan. |

| Seam | Requirement |
| --- | --- |
| Game creation | Attach server-owned EvenChess mode/policy metadata. |
| Seek/challenge/matchmaking | Include ECR, expected offset, pool, level compatibility. |
| Move commit | Clear stale overlays, record position, evaluate pending coaching. |
| Clock update | Feed stale-help and clock-pressure accounting. |
| Board/WebSocket payload | Deliver only server-authorized overlays. |
| Engine service | Return bounded truth packets. |
| AI service | Schema-constrained wording over truth packets. |
| Rating update | Apply ECR using Used Offset; normal ratings separate. |
| Review surface | Read completed game plus audit/analysis; no live mutation. |
| Account/subscription | Store tokens, quotas, plan state under account extension. |
| Marketing/funnel | Serve config and events without affecting fairness. |
| Admin/ops | Query health, ledgers, feature flags. |

DATA-L1-001: Fairness-affecting ledger/event/config models include schema/model version.  
DATA-L1-002: Rating/offset calculations record policy/model versions.  
DATA-L1-003: Marketing events record config version/variant.  
DATA-L1-004: AI/engine records record prompt/profile/model versions.  
DATA-L1-005: Migrations preserve replayability.

Codex must inspect lila storage conventions before choosing database implementation. Prefer dedicated EvenChess stores over broad core fields. Avoid raw AI prompts, raw engine lines, raw emails, or sensitive attribution unless required and privacy-reviewed. Stage 1 requires only mode stub, dummy overlay event, patch-map record, and mock AI request/response if implemented.
