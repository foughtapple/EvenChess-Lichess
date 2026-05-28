# Appendix P - Telemetry, Analytics, and Calibration

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Defines telemetry and analytics for calibration, funnel, and operations.


| Family | Examples |
| --- | --- |
| Match lifecycle | match.search_started, game.started, game.ended, rating.applied |
| Move | move.server_committed, clock.updated, position.state_recorded |
| Coaching | coaching.requested/generated/surfaced/viewed/expanded/suppressed |
| Feature | feature.offset_count_shown, plan_viewed, explanation_expanded |
| Rating | assistance.summary_computed, offset.computed, rating.applied |
| AI/Engine | ai.requested, engine.job.completed, fallback.used |
| Abuse | abuse.signal_recorded, review.case_opened |
| Funnel | landing_page_view, sign_up_complete, first_game_completed, purchases |

TEL-L1-001: Rated games require append-only server event ledger with schema versions.  
TEL-L1-002: Ledger completeness supports rating replay, assistance recomputation, fair-play review, incident handling.  
TEL-L1-003: Client-only analytics may supplement but never authorise fairness/rating/token decisions.  
TEL-L1-004: Events include policy/model/config versions where relevant.

CAL-L1-001: Dashboards show residuals by Used Level, Assistance Load, time control, ECR band, exactness, feature mix, follow-rate.  
CAL-L1-002: Separate normal EvenChess, Target, casual, bot/AI practice, and review.  
CAL-L1-003: Identify stale/degraded/engine-fallback games.  
CAL-L1-004: Show whether features/levels appear underpriced, overpriced, overused, or unsafe.

ATTR-L1-010: Funnel telemetry captures UTM, click IDs, variant, first/latest touch, signup, first game, subscription linkage.  
ATTR-L1-011: Conversion events include stable names, timestamps, dedupe IDs, pseudonymous user IDs, campaign fields, value/plan.  
ATTR-L1-012: Launch dashboards group by source, campaign, variant, account type, token source, summary source, queue health, plan without invasive MVP risk scoring.

Privacy: collect only needed gameplay/coaching/rating/policy events; use pseudonymous analytics IDs; separate hot raw logs, medium derived metrics, long-term aggregates; avoid unnecessary sensitive ad data. Tests: completeness, schema versions, recomputation, replay, dashboard validation, dedupe, privacy scan, retention, analytics failure non-effect.
