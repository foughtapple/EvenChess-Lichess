# Appendix R - Admin Operations, Dashboards, and Incident Response

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Defines ops controls, dashboards, incidents, launch health.


OPS-L1-001: Monitor Stockfish queue, engine latency, AI latency, fallback, stale events, cost.
OPS-L1-002: Support pause/downgrade/no-rate/annul when outages create asymmetric assistance.
OPS-L1-003: Risky levels/features must be feature-flagged and rollbackable.
OPS-L1-004: Operators identify active policy/model/config versions for a game.
OPS-L1-005: Admin controls must not alter hidden rated fairness outside versioned policy path.

Launch controls must pause/suppress rewarded ads, Standard/Premium promotion, campaign variants, broken tracking destinations, play windows, and queue-facing campaigns when health degrades. Pause notices are public and do not manipulate hidden queue/fairness. Monitoring exposes signups, first games, 3/10-game progression, ads, token grants/consumption, summaries, purchases, cancellations, queue time, failed match rate, campaign source/variant.

| Incident | Examples | Required response |
| --- | --- | --- |
| Engine outage | High latency, missing candidates, asymmetric help | Suppress/downgrade/no-rate/annul if fairness affected. |
| AI outage | Timeout, invalid output, cost spike | Fallback/suppress; preserve fairness. |
| Stale coaching | Advice for old state | Clear, audit, investigate TTL/hash. |
| Rating corruption | Wrong offset/model | Stop rating flow, replay, correct by audit. |
| Token/billing issue | Wrong consumption/sub state | Refund/restore, audit, disable bad path. |
| Marketing copy issue | Cheating/pay-to-win implication | Kill variant, correct config. |
| Queue health issue | Paid traffic causes waits | Pause campaigns, show window/waitlist. |
| Data/privacy issue | Excessive capture/export | Stop capture/export, review retention. |

Minimum dashboards: engine/AI health, coaching delivery/stale/suppressed events, Assistance Load/Used Offset, ECR residuals, queue time/failures, tokens, summaries, funnel, purchases, abuse cases, feature flags, active policy versions.

No serious paid launch proceeds unless signup, first game, ten-game milestone, ad token flow, Standard/Premium purchase, cancellation, GA4, Google Ads, Meta Pixel/CAPI, dedupe are verified or explicitly unavailable with launch decision. Tests: rollback, outage simulation, fallback, no-rate/annul, stale cleanup, pause notice, dashboard source, conversion tracking.
