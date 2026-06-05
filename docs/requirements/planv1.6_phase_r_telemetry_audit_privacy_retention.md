# EvenChess Plan v1.6 Phase R - Telemetry, Audit, Privacy, and Retention

## Phase Goal

Make EvenChess production behavior observable, auditable, privacy-safe, and supportable after deployment.

Phase R must let operators answer operational and fairness questions without exposing private ECE internals, raw provider output, secrets, or unnecessary personal data.

## Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- Appendix R: Telemetry, Audit, and Data Retention
- Appendix S: Abuse, Fairness, and Trust Controls
- Appendix T: Operations, Deployment, and Incident Response
- `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`
- `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`
- `docs/requirements/planv1.6_phase_o_admin_operator_console.md`
- `docs/requirements/planv1.6_phase_q_security_abuse_fairness_controls.md`

## Telemetry Authority Boundary

Server telemetry is authoritative for fairness, ratings, tokens, coaching permission, and audit reconstruction.

Client analytics may supplement product analytics only. Client analytics must never authorize:

- Set Level;
- Used Level;
- coaching render permission;
- proposed/potential consumable use;
- token grant or consumption;
- ECR settlement;
- no-rate/annul/review decisions.

## Required Metrics

Production telemetry must emit metrics for:

- ECE health and latency;
- ECE request failures;
- Stockfish/provider latency where surfaced through ECE status;
- AI latency, fallback, and cost;
- stale payload rejection;
- overlay render failures;
- display suppress/clear rates;
- queue times;
- match contract creation;
- bot fallback pairing rate;
- simulation bot population and pairing rate;
- token grants, consumption, refunds, and failures;
- custom/full-game analysis usage;
- proposed-move usage and failures;
- potential-move reveal usage and failures;
- rating settlement success/failure;
- ECOR calibration sample volume;
- admin setting mutations;
- incident actions.

Metrics must be grouped by safe dimensions such as mode, rated/casual, time control, Used Level, Set Level, pool, feature, policy version, and coarse ECR band. Metrics must not include raw provider payloads, secrets, filesystem paths, raw prompts, or raw AI responses.

## Required Audit Events

The audit ledger must record:

- match contract creation;
- Set Level at game start;
- every Used Level increase;
- every live ECE payload generated for live play;
- coaching render, hide, expand, suppress, and clear actions;
- proposed-move checks, including arrow move, legality, level, and whether result was shown;
- potential-move reveal consumption and display;
- final Used Level;
- Assistance Load;
- Used Offset;
- rating settlement;
- bot fallback match source where relevant;
- token grant, consumption, refund, and admin adjustment;
- full-game/custom analysis requests;
- admin fairness-affecting setting changes;
- incident actions such as pause, no-rate, annul, review, rollback, refund, and disable assistance.

Audit events must be append-only, schema-versioned, server-authored, and able to reconstruct why a game got its Set Levels and final rating adjustment.

## Retention Policy

Default retention requirements:

- each user keeps rolling recent live ECE history for the last 10 games;
- each user keeps requested full-game/custom analysis memory for the last 100 requested analyses;
- paid saved games may persist beyond normal rolling retention according to subscription policy;
- unavailable retained history means replayed games have no existing payload-backed overlays until the user requests analysis.

Retention must store enough to support Live White, Live Black, and Live Both review modes:

- FEN list or position hashes as required for replay;
- moves;
- side-to-move;
- requested and delivered levels per side;
- summary/plan text when retained;
- overlay essentials or output references;
- audit ids;
- ECE version/policy versions;
- final Used Level and settlement references.

Retention must avoid unnecessary raw AI/provider data. Full raw ECE payload retention is not the default and requires explicit retention policy approval.

## Privacy and Redaction Rules

Telemetry, audit, admin dashboards, and debug exports must redact or avoid:

- API keys;
- provider secrets;
- raw prompts;
- raw provider output;
- raw unrestricted Stockfish output;
- ECE private internals;
- provider filesystem paths;
- local engine paths;
- tablebase/model/database paths;
- anti-cheat internals;
- unnecessary personal data;
- sensitive ad or payment data beyond required provider-safe references.

Use pseudonymous user identifiers in analytics where possible. Keep direct user ids only where required for game audit, moderation, billing, or account-support operations.

## ECE Debug IO Policy

ECE debug IO is an operator diagnostic tool, not production telemetry.

Production policy:

- debug IO is off by default;
- debug IO may be temporarily enabled only for a specific incident or staging diagnostic;
- debug IO must use redaction;
- debug IO files must not be public;
- debug IO data must not be copied into EvenChess-Lichess source control;
- operators must tolerate missing/unreadable debug files because ECE writes atomically.

## Dashboard Requirements

Operator dashboards must answer:

- Is ECE healthy?
- Are ECE calls failing or slow?
- Are searches pairing?
- Are bots active?
- Are bot fallback games increasing?
- Are simulation bots running?
- Are overlays stale or failing to render?
- Are tokens flowing correctly?
- Are proposed/potential consumables failing or being abused?
- Are ratings settling correctly?
- Is ECOR calibration sample volume sufficient?
- Are admin changes and incident actions audited?
- Are any privacy/redaction checks failing?

Dashboards must expose alert thresholds for:

- ECE outage or latency spike;
- high stale payload rate;
- overlay render failure spike;
- queue time spike;
- bot fallback overuse;
- token grant/consumption anomaly;
- rating settlement failures;
- calibration sample ingestion failure;
- unexpected public debug/admin access;
- admin mutation without audit metadata.

## Current Implementation State

### Existing Foundations

The current codebase has strong Phase R modelling foundations:

- `modules/evenchess/src/main/TelemetryAnalytics.scala` defines telemetry requirements, event families, event names, server/client authority, append-only telemetry ledger behavior, audit envelopes, audit kinds, display actions, retention tiers, privacy scans, calibration dashboard dimensions, conversion events, and ECE history retention plans.
- `modules/evenchess/src/main/LevelBasedMatchmaking.scala` emits search/game/rating telemetry models and match-contract audit records.
- `modules/evenchess/src/main/PlaySearchIntegration.scala` carries search telemetry, match-contract audit records, and game-start persistence telemetry.
- `modules/evenchess/src/main/LiveCoaching.scala` models live ECE history, audit events, output references, retention limits, hidden prefetch, stale/suppressed decisions, and review-mode support.
- `modules/evenchess/src/main/CoachingPolicy.scala`, `CoachingOverlays.scala`, `LiveBoardIntegration.scala`, and `LiveOverlayUi.scala` require audit identity for coaching/display surfaces.
- `modules/evenchess/src/main/OpenAiCoaching.scala`, `OpenAiResponsesApi.scala`, and `AiCoachPolicy.scala` model AI request audit, source facts, audit tags, fallback telemetry, token/cost metadata, and raw debug exclusion.
- `modules/evenchess/src/main/AdminBackendSettings.scala` includes audit retention settings and redaction helpers for secret-like admin values.
- `modules/evenchess/src/main/AdminOpsDashboard.scala` models health dashboards and safe audit search rows.
- Existing tests cover server-authoritative telemetry, append-only ledgers, rating replay completeness, Appendix R audit envelopes, proposed-move audit, audit/calibration snapshot completeness, client analytics limits, calibration dashboard dimensions, stale/degraded/fallback signals, funnel conversion shape, privacy scan, retention tiers, and ECE history retention safety.

### Not Yet Release-Proven

Phase R is not release-complete until these are proven:

- telemetry events are emitted from actual production routes/services, not only represented as models;
- audit envelopes are persisted durably in a queryable store;
- admin audit search reads real durable audit data;
- dashboards ingest real metrics, not only sample/static rows;
- retention pruning runs automatically for rolling recent games and requested analyses;
- paid saved-game retention policy is enforced;
- redaction is validated against real logs, dashboard data, and debug exports;
- ECE debug IO is disabled by production config and cannot be reached publicly;
- dashboard alert thresholds are configured and tested;
- privacy/export/delete policy is documented and mapped to retained EvenChess data.

## Required Tests

Phase R implementation must have tests for:

- route/service emission for ECE latency and failure metrics;
- route/service emission for search, match contract, bot pairing, token flow, proposed/potential usage, rating settlement, and calibration sample metrics;
- audit persistence for match contract, Set Level, Used Level increase, ECE payload generation, display action, proposed/potential use, final settlement, token mutation, admin mutation, and incident action;
- audit query returns safe redacted rows;
- raw provider output, prompts, secrets, and filesystem paths do not appear in public/admin telemetry views;
- rolling recent-game retention prunes beyond 10 games per user;
- requested-analysis memory prunes beyond 100 requested analyses per user;
- paid saved-game retention bypasses normal rolling deletion only when policy allows;
- stale missing retained history causes replay with no existing payload-backed overlays until analysis is requested;
- dashboards show real health/queue/token/rating/bot/overlay/calibration values;
- alert thresholds fire in staging drills.

## Data Export and Deletion Policy

Public deployment needs an explicit EvenChess data handling policy covering:

- what live ECE history is retained;
- what requested analysis history is retained;
- what audit data is retained for fairness/rating/moderation obligations;
- what can be exported to a user;
- what can be deleted or anonymized;
- what must be retained for fraud, billing, moderation, or legal reasons;
- how pseudonymous analytics ids are separated from account identity.

This policy must align with base Lichess account/privacy behavior where applicable.

## Patch Map Impact

Future Phase R implementation may touch upstream Lichess analytics, logging, admin, moderation, game history, database, route, or privacy/export seams. Those changes must be patch-mapped and entered into the integration log.

This Phase R documentation pass does not itself change runtime code.

## Phase R Acceptance Status

Phase R is conducted as a readiness and requirements pass.

Status:

- Telemetry, audit, retention, privacy, dashboard, and calibration models exist.
- Unit tests cover many model-level invariants.
- Release readiness remains blocked until durable persistence, real route/service event emission, real dashboard ingestion, retention pruning, redaction validation, and privacy/export/delete policy are implemented and tested in staging.

## Phase S Entry Criteria

Before CI/CD and release automation can be considered complete, Phase R must provide:

- stable event schemas;
- migration/storage requirements for audit and retained history;
- test commands for telemetry/audit/retention suites;
- staging dashboard/alert verification steps;
- redaction and privacy checks that CI or release scripts can run.
