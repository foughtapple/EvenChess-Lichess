# Plan v1.6 Phase Z - Production Launch, Monitoring, and Iteration

## 1. Phase Goal

Phase Z defines the production launch procedure, launch-window monitoring model, incident/rollback triggers, first-game review workflow, support triage loop, ECOR calibration policy, and post-launch iteration process.

This phase is a launch readiness and operating model. It does not authorize production launch by itself. Phase Z can begin only after Phase Y produces an approved release candidate with a written `GO` or approved private-beta `CONDITIONAL GO`.

## 2. Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/README_V2_REQUIREMENTS_SUITE.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `docs/requirements/APPENDIX_Z_V2_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md`
- `docs/requirements/planv1.6_phase_a_scope_freeze.md`
- `docs/requirements/planv1.6_phase_f_ece_deployment.md`
- `docs/requirements/planv1.6_phase_g_ecl_ece_gateway_hardening.md`
- `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`
- `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`
- `docs/requirements/planv1.6_phase_o_admin_operator_console.md`
- `docs/requirements/planv1.6_phase_p_bot_matchmaking_simulation_operations.md`
- `docs/requirements/planv1.6_phase_q_security_abuse_fairness_controls.md`
- `docs/requirements/planv1.6_phase_r_telemetry_audit_privacy_retention.md`
- `docs/requirements/planv1.6_phase_t_staging_environment.md`
- `docs/requirements/planv1.6_phase_w_backup_recovery_maintenance_jobs.md`
- `docs/requirements/planv1.6_phase_x_legal_policy_public_copy.md`
- `docs/requirements/planv1.6_phase_y_release_candidate_freeze_rollback.md`

## 3. Launch Preconditions

Production launch is blocked unless all of the following are true:

1. Phase Y release candidate report is complete.
2. Go/no-go decision is explicit.
3. Release branch is clean and all intended implementation files are tracked.
4. Patch map and integration log are current.
5. Full CI passed or each skipped/failed check has a signed launch decision.
6. Staging smoke passed against production-like ECL, ECE, database, proxy, and monitoring topology.
7. Browser/device/performance QA passed for game start, move, overlay update, proposed/potential moves, game finish, and refresh behavior.
8. Backup and restore were tested or dry-run.
9. Rollback runbook was tested or dry-run.
10. Feature flags and kill switches were verified.
11. Public rules, bot disclosure, terms/privacy, support FAQ, and payment/token copy are approved for the enabled launch scope.
12. On-call operator coverage is scheduled for the launch window.

If any item is false, Phase Z remains a plan only.

## 4. Recommended Launch Scope

The first public launch should be conservative.

Recommended first public beta defaults:

| Feature | Default | Reason |
| --- | --- | --- |
| EvenChess public entrypoints | On | required for beta |
| Casual EvenChess | On | lower rating-risk launch mode |
| Rated EvenChess ECR | Off until staging settlement proof is strong; then limited beta | rating integrity is trust-critical |
| Computer training | On if board and ECE flow are stable | useful low-risk test path |
| ECE quick/deep | On only after ECE health/stale-payload proof | core product dependency |
| AI summaries | Off or limited | cost/failure containment |
| Proposed move | On only if consumables are durable and tested | assistance/fairness sensitive |
| Potential moves | On only if consumables are durable and tested | assistance/fairness sensitive |
| Bot fallback | Off initially; enable after monitoring proves human matching and disclosure | trust/queue-risk feature |
| Bot simulation | Off in production public pool unless operator is running a controlled test | can distort production metrics |
| Tokens/subscriptions/ads | Off unless payment/token gates have passed | payment risk |
| ECOR calibration writes | On for sample collection if durable; recommendations only | data collection, no auto-apply |
| ECOR table auto-apply | Off | admin review required |
| Public debug search card | Off | debug-only |

Any departure from these defaults must be recorded in the launch decision with owner, reason, test evidence, and rollback switch.

## 5. Launch Sequence

Use this order for the first production deployment:

1. Confirm backup snapshot.
2. Confirm rollback artifact/image and previous DB restore point.
3. Deploy ECE privately.
4. Verify ECE `/health` and `/ready` from ECL server network only.
5. Deploy ECL with conservative flags.
6. Run post-deploy health checks.
7. Run server-side ECE quick/deep/proposed smoke from ECL.
8. Confirm browser cannot call ECE analysis endpoints directly.
9. Confirm admin dashboard sees build version, feature flags, ECE health, queue health, bot state, token state, and rating settlement state.
10. Enable closed/internal game creation.
11. Play manual internal smoke games.
12. Review first internal game contracts, Set Levels, Used Levels, ECE payloads, overlay updates, and settlement behavior.
13. Enable public beta entrypoint if smoke passes.
14. Monitor launch dashboards continuously.
15. Keep rollback path open for the entire launch window.

## 6. Launch Monitoring Dashboard

Operators must be able to monitor these signals in near real time.

### 6.1 ECE Health

- ECE service up/down;
- `/health` result;
- `/ready` result;
- quick board latency p50/p95/p99;
- deep board latency p50/p95/p99;
- proposed-move latency p50/p95/p99;
- ECE error rate;
- ECE timeout rate;
- stale payload rejection rate;
- ECE circuit-breaker/backpressure state;
- ECE debug IO enabled/disabled state.

### 6.2 Queue and Matchmaking

- active search tickets;
- median and p95 queue time;
- abandoned searches;
- human-human match rate;
- human-bot match rate;
- bot-bot match rate;
- bot fallback enabled/disabled;
- bot fallback timeout;
- simulation bot enabled/disabled;
- simulation bot population;
- failed contract creations;
- uneven-match rate.

### 6.3 Live Game Quality

- games started;
- games completed;
- abort rate;
- disconnect rate;
- illegal state/game-policy failures;
- overlay render failures;
- move-triggered payload update failures;
- browser refresh recovery failures;
- coach text update failures;
- proposed/potential quota failures;
- clock failures;
- computer-game playability failures.

### 6.4 Fairness and Rating

- Set Level assignment distribution;
- Used Level distribution;
- Used Offset distribution;
- ECR settlement queue depth;
- ECR settlement success/failure rate;
- settlement idempotency conflicts;
- normal Lichess rating mutation attempts;
- no-rate/pause status;
- ECOR calibration sample count;
- ECOR residual/error metrics when enough data exists.

### 6.5 Tokens, Payments, and Entitlements

Monitor only if monetisation is enabled:

- token reservations;
- token consumption;
- token refunds;
- failed token operations;
- negative-balance prevention events;
- subscription state changes;
- payment webhook success/failure;
- rewarded-ad callback success/failure;
- support adjustments;
- ledger reconciliation mismatches.

### 6.6 Security, Abuse, and Privacy

- admin login and mutation audit;
- rate-limit hits;
- outside-help reports;
- suspicious repeated search/proposed/potential behavior;
- bot farming indicators;
- ECE endpoint exposure alerts;
- public JSON redaction failures;
- privacy/export/delete request volume;
- retention job failures.

### 6.7 Infrastructure

- ECL CPU/memory/disk;
- ECE CPU/memory/disk;
- Mongo/Redis/search health;
- proxy/TLS health;
- background worker queue depth;
- backup job status;
- log ingestion health.

## 7. Launch Window Operator Coverage

Minimum coverage:

- one ECL operator with deploy/rollback access;
- one ECE operator with ECE service/provider knowledge;
- one product/support owner for public notices and known-issue decisions;
- one abuse/moderation owner if public traffic is enabled;
- payment/provider owner if monetisation is enabled.

Operator handoff must include:

- current release artifact/version;
- feature flag snapshot;
- dashboard links;
- rollback runbook link;
- incident channel;
- support escalation path;
- known issues;
- launch decision record.

## 8. First-Game Manual Review

Review the first production games manually.

Minimum review sample:

- first 10 computer games;
- first 10 casual human-human games;
- first 10 rated games if rated is enabled;
- first 10 human-bot fallback games if bot fallback is enabled;
- first 5 bot-bot simulation games if simulation is intentionally enabled;
- first 10 proposed-move calls;
- first 10 potential-move reveal calls;
- first 10 completed ECR settlements.

For each reviewed game, confirm:

- time control is correct;
- rated/casual state is correct;
- Set Level is assigned server-side;
- Used Level starts from allowed preference and never decreases;
- Used Offset is derived from actual Used Level;
- ECE payload is accepted for the correct game, ply, side, and level;
- overlays update after moves without refresh;
- coach text follows turn-gating rules;
- proposed/potential cache clears after move;
- game remains playable if ECE is slow/unavailable;
- ECR settlement does not mutate normal Lichess rating;
- audit events exist and are redacted.

## 9. Rollback and Pause Triggers

Immediate rollback or feature pause is required for:

- active games become unplayable;
- normal Lichess ratings are affected by EvenChess games;
- ECE private endpoints become browser/public reachable;
- secrets/provider paths/raw prompts/raw provider output appear in public logs, browser JSON, or UI;
- ECR settlement corrupts ratings or cannot be idempotently repaired;
- token/payment ledger corruption;
- admin route authorization or CSRF failure;
- widespread overlay updates fail and users cannot play cleanly;
- bot fallback creates undisclosed or incorrectly timed/rated games;
- database migration causes data loss;
- backup/restore path is not available during incident.

Feature-specific pause is preferred when it contains the problem:

- pause ECE deep/AI before disabling all games;
- pause proposed/potential before disabling overlays;
- pause bot fallback before disabling search;
- pause rated ECR before disabling casual play;
- pause payments/tokens before disabling free games.

## 10. Public Notices and Support

Before public launch:

- status page or public announcement path exists;
- support intake path exists;
- bot fallback public disclosure is correct for current flag;
- known issues are written in user-safe terms;
- outside-help reporting path is visible;
- token/payment support path is visible if monetisation is enabled.

During beta:

- review support reports daily;
- tag reports by feature area;
- escalate P0/P1 immediately;
- update public notice if a feature is paused;
- avoid exposing internal diagnostics, ECE internals, or abuse-detection details in user-facing replies.

## 11. ECOR Calibration After Launch

ECOR calibration must be data-informed and admin-reviewed.

Rules:

- do not run ECOR calibration on insufficient data;
- do not include local Test Ground data in production calibration;
- do not auto-apply calibration recommendations;
- do not rewrite historical ECR automatically from calibration output;
- store calibration run evidence, sample count, residuals, standard deviation, confidence/fit metrics, recommended adjacent gaps, current-table comparison, and operator decision;
- apply ECOR table changes only through audited admin action with rollback snapshot;
- after an ECOR change, monitor rating residuals and support reports.

Recommended first review:

- wait until there are enough real rated games across multiple level combinations;
- run report-only calibration;
- review outliers and bot-influenced samples separately;
- decide whether to keep, adjust, or defer the ECOR table.

## 12. Post-Launch Triage Loop

Create a daily beta triage loop:

1. Review dashboard health.
2. Review support reports.
3. Review incident log.
4. Review failed/stale ECE requests.
5. Review overlay/gameplay defects.
6. Review queue times and bot fallback usage.
7. Review ECR settlement anomalies.
8. Review token/payment issues if enabled.
9. Classify defects P0-P3.
10. Decide fix now, feature-pause, accept, or backlog.

Weekly beta review:

- evaluate launch scope;
- decide whether to enable or disable bot fallback;
- decide whether rated ECR can expand;
- decide whether monetisation remains disabled or can soft-launch;
- review ECOR calibration sample growth;
- update next-version backlog.

## 13. Launch Log Template

```text
# EvenChess v1.6 Launch Log

## Launch Decision
- date/time:
- decision: GO / CONDITIONAL GO
- approver:
- branch/commit:
- ECL build:
- ECE build/version:
- migration version:
- ECOR table version:
- feature flag snapshot:

## Pre-Launch Checks
- backup:
- rollback artifact:
- ECE health:
- ECL health:
- admin dashboard:
- public copy:
- support path:

## Launch Actions
- deploy start:
- deploy complete:
- smoke tests:
- public entrypoint enabled:
- flags changed:

## Monitoring Notes
- ECE:
- queue:
- games:
- overlays:
- ratings:
- tokens/payments:
- bots:
- support:

## Incidents
- id:
- time:
- impact:
- mitigation:
- rollback/pause:
- owner:

## Decision Updates
- continue / pause / rollback:
- reason:
```

## 14. Post-Launch Review Template

```text
# EvenChess v1.6 Post-Launch Review

## Period
- start:
- end:

## Usage
- users:
- games:
- searches:
- queue times:
- bot fallback:
- computer games:
- proposed/potential usage:

## Reliability
- ECL uptime:
- ECE uptime:
- ECE latency:
- stale payloads:
- overlay failures:
- game aborts:
- settlement failures:

## Fairness
- Set Level distribution:
- Used Level distribution:
- Used Offset distribution:
- ECR changes:
- no-rate events:
- outside-help reports:

## Support
- total reports:
- top issues:
- resolved:
- unresolved:

## Incidents
- P0:
- P1:
- P2:

## Decisions
- keep enabled:
- disable:
- expand:
- defer:

## Next-Version Backlog
- priority:
- owner:
- evidence:
```

## 15. Phase Z Acceptance Status

Phase Z is conducted as the production launch, monitoring, and iteration plan.

Current status:

- launch preconditions are defined;
- conservative launch scope is defined;
- launch sequence is defined;
- monitoring dashboard requirements are defined;
- first-game manual review is defined;
- rollback/pause triggers are defined;
- support/public notice workflow is defined;
- ECOR calibration after-launch policy is defined;
- launch log and post-launch review templates are defined;
- actual production launch remains blocked until Phase Y gates are satisfied and an explicit `GO` or private-beta `CONDITIONAL GO` exists.

## 16. Final v1.6 Plan Status

Plan v1.6 phases A through Z are now defined/conducted except any phase the product owner intentionally skipped or deferred. Completion of planning does not equal deployability.

The remaining deployment work is execution of the blockers recorded across phases, especially:

- repo hygiene and tracked-file reconciliation;
- durable persistence/migrations;
- production-safe ECL/ECE gateway;
- integrated matchmaking/game creation proof;
- ECR settlement lifecycle proof;
- live assistance refresh proof;
- admin auth/CSRF and durable audit;
- staging deployment and smoke;
- browser/device/performance QA;
- backups/restore;
- public copy/legal/support sign-off;
- RC approval and rollback drill.

Only after those blockers are resolved should EvenChess move from readiness planning to actual production launch.
