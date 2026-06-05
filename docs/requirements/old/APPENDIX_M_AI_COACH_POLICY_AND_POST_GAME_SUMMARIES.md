# Appendix M - AI Coach Policy and Post-Game Summaries

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Defines AI usage and summary quotas/quality.


AI-L1-001: AI explains and compresses authorized truth packets; it does not choose moves or invent facts.
AI-L1-002: Live AI outputs schema-constrained JSON with policy version, exactness class, message, visual cues, source facts, audit tags.
AI-L1-003: Output is scanned for illegal notation, over-exact coordinates, direct commands, best-move labels, stale position, visual overreach.
AI-L1-004: Invalid output regenerates once then falls back/suppresses.
AI-L1-005: Every AI request logs model, prompt/schema version, token counts, cost, validation, fallback, delivered exactness.
AI-L1-006: Paid plans cannot receive stronger live help or deeper live engine truth through AI.

Provider access is server-side via runtime settings/secrets. The client never exposes or decides credentials. Cheap/default models are allowed if configurable and validation passes. Prompts state AI explains supplied packets only. Live rated prompts avoid labelled best-move wording unless later approved.

## M.5 Post-game summaries

SUMMARY-L1-001: Post-game summaries are learning/review surfaces and must not mutate live rated fairness state.
SUMMARY-L1-002: New eligible accounts receive 3 full-quality match-summary tokens.
SUMMARY-L1-003: New eligible accounts receive 1 full-quality performance-summary token unlocked after 10 completed games.
SUMMARY-L1-004: Free and paid summaries use the same product-quality review pipeline.
SUMMARY-L1-005: Failed generation does not consume token/quota; cached views do not consume new quota.
SUMMARY-L1-006: Product promises review quality, not a named frontier model.

| Summary type | Free/onboarding | Premium |
| --- | --- | --- |
| Match summary | 3 full-quality tokens | Up to 10 full-quality match summaries/day. |
| Performance summary | 1 full-quality token unlocked after 10 completed games | Up to 1 full-quality performance summary/day. |

Performance-summary AI analysis uses recent completed online games only; bot/computer/study/non-online games are excluded. Launch default is up to 50 completed online games unless changed. Tests: grounding, injection, schema, prohibited wording, stale scanner, cost/audit, fallback, same quality, no fairness mutation, online-only window.
