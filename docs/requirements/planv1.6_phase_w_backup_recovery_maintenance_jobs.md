# EvenChess Plan v1.6 Phase W - Data Backup, Recovery, and Maintenance Jobs

## Phase Goal

Avoid losing EvenChess ratings, game policy, live ECE history, paid entitlements, token ledgers, audit records, or calibration data.

Phase W defines the backup, restore, retention, cleanup, archive, and maintenance-job requirements that must exist before public traffic.

## Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- Appendix E: Accounts, Tokens, Top Bar, and Monetisation
- Appendix I: ECR Rating Settlement and Calibration
- Appendix L: Live Game Assistance and ECE History
- Appendix R: Telemetry, Audit, and Data Retention
- `docs/requirements/planv1.6_phase_c_architecture.md`
- `docs/requirements/planv1.6_phase_e_persistence_migrations.md`
- `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`
- `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`
- `docs/requirements/planv1.6_phase_n_tokens_subscriptions_ads_entitlements.md`
- `docs/requirements/planv1.6_phase_r_telemetry_audit_privacy_retention.md`
- `docs/requirements/planv1.6_phase_t_staging_environment.md`
- `docs/requirements/planv1.6_phase_v_browser_device_performance_qa.md`

## Critical State Inventory

Backups and recovery plans must cover:

- normal Lichess durable state required by the selected deployment;
- EvenChess search tickets and public search keys;
- match contracts;
- game policy records;
- assistance state and monotonic Used Level;
- proposed-move consumed counts and cached legal previews;
- potential-move reveal counts and cached authorized reveal payloads;
- live ECE history and output references;
- requested full-game/custom analysis memory;
- ECR rating records;
- rating settlement snapshots and audit refs;
- ECOR table snapshots;
- rating-to-base-level table snapshots;
- Stockfish/Lichess AI equivalent-rating table snapshots;
- ECOR calibration samples, capped at latest 1,000,000 by game id/time;
- calibration run results;
- token ledger;
- entitlement/subscription snapshots;
- saved-game retention flags;
- bot fallback/simulation runtime state where it must survive restart;
- audit ledgers;
- admin setting mutation audit;
- telemetry required for dispute and incident reconstruction.

## Backup Schedule

Recommended public-beta schedule:

| Store | Backup frequency | Retention target | Notes |
| --- | --- | --- | --- |
| Primary database | daily full plus frequent incremental/oplog where supported | 30 days hot, 90 days warm | Must include EvenChess collections |
| Audit/rating/token ledgers | daily plus incremental if separated | policy/legal/accounting retention | Do not destructively prune without policy |
| ECE history/analysis memory | daily | according to retention policy | Avoid raw provider payloads |
| ECOR snapshots/calibration runs | daily and before every ECOR activation | retain rollback history | Required for admin restore |
| Config/settings snapshots | before every deploy and setting activation | 90 days minimum | Include feature flags, not secrets in plaintext |
| Logs/metrics | according to privacy/retention policy | short hot logs, longer aggregate metrics | Secrets/path redaction required |

Backups must be encrypted at rest and protected from public access.

## Restore Procedure

The restore runbook must document:

1. stop or isolate affected services;
2. identify target restore point;
3. restore database/state store;
4. verify schema/migration compatibility;
5. restore or reapply safe config snapshots;
6. verify ECR/rating snapshots;
7. verify token ledger balances and idempotency keys;
8. verify ECOR table active snapshot and rollback history;
9. verify live ECE history and requested analysis counts;
10. verify audit ledger integrity;
11. start services;
12. run smoke tests;
13. record restore audit and incident notes.

Restore must be tested in staging before public launch.

## Disaster Recovery Targets

Initial public-beta targets:

- RPO: 24 hours for non-ledger gameplay history; lower target required for token/rating ledgers if feasible.
- RTO: 4 hours for single-host beta recovery target.
- Token ledger loss tolerance: none without manual reconciliation.
- ECR/rating settlement loss tolerance: none without manual replay/reconciliation.
- ECOR snapshot loss tolerance: none for active/latest rollback snapshot.
- Audit ledger loss tolerance: no silent loss for rated fairness decisions.

If these targets cannot be met, public beta must document the risk and limit monetisation/rated scope accordingly.

## Retention Jobs

Required retention/cleanup jobs:

| Job | Scope | Frequency | Rule |
| --- | --- | --- | --- |
| Expire abandoned search tickets | search tickets | every 1-5 minutes | expire by `expiresAt` |
| Retire completed tickets | search/contracts | every 5-15 minutes | retire after game handoff |
| Clear stale simulation tickets | bot tickets | every 1-5 minutes | remove simulation tickets when stopped/expired |
| Recent live history retention | live ECE history | on game completion plus daily repair | keep last 10 completed games per user unless saved |
| Requested analysis retention | analysis memory | on request plus daily repair | keep last 100 requested analyses per user unless saved |
| Paid saved-game protection | saved game flags/history | daily repair | never delete eligible saved games during rolling cleanup |
| Calibration sample cap | calibration samples | on settlement plus daily repair | keep latest 1,000,000 by playedAt/game id |
| Audit retention | assistance/admin audit | daily | obey admin/privacy policy while preserving rating/payment/legal records |
| Token ledger archive | token ledger | policy-based | archive only; never destructive by default |
| Raw payload privacy cleanup | ECE/debug payload history | daily | remove accidental raw payloads or expired debug samples |
| Expired proposed/potential caches | consumable caches | daily or by game lifecycle | retain audit/counts; remove stale display cache |
| Expired bot runtime state | bot operation records | daily | clear stopped stale runtime records after audit retention |

All jobs must log:

- job id;
- start/end time;
- count scanned;
- count changed;
- count skipped because saved/active/protected;
- duration;
- errors;
- policy/config version.

## Maintenance Job Safety

Maintenance jobs must be:

- idempotent;
- retry-safe;
- concurrency-safe;
- environment-scoped;
- auditable;
- safe around active games;
- blocked from running destructive production paths from staging/local scripts.

Jobs must never delete:

- active games;
- active searches;
- active analysis jobs;
- paid saved games protected by policy;
- required token ledger records;
- required rating settlement records;
- required audit records for fairness decisions.

## Token Ledger Recovery

The token ledger must support:

- balance reconstruction from ledger entries;
- idempotent grant/spend/refund/admin adjustment events;
- manual support correction with audit id;
- reconciliation after payment webhook replay;
- archive without losing accounting/audit trail.

A restore test must prove a user's token balance can be recovered from ledger data and that replaying a known webhook or game-start idempotency key does not double-grant or double-spend.

## Rating and ECOR Recovery

ECR recovery must support:

- replay or verification of rating settlement from stored game result, final Used Levels, Used Offsets, pre/post rating snapshots, model version, and audit event ids;
- idempotent settlement by game id;
- no mutation of normal Lichess ratings;
- restoration of the active ECOR table snapshot;
- restoration of prior ECOR/base-level/Stockfish AI equivalent table snapshots;
- recalculation reports without auto-applying recommendations.

Calibration data recovery must preserve the latest 1,000,000 samples needed for ECOR review or document any gap.

## Analysis Memory Recovery

Recovery must prove:

- last 10 recent completed games with retained live ECE history are available per user where not pruned;
- last 100 requested full-game/custom analyses are available per user where not pruned;
- paid saved games persist beyond rolling cleanup;
- raw ECE/provider payloads are not required for replay;
- missing retained history behaves correctly: no payload-backed overlays until analysis is requested.

## Backup Verification

Backups must be verified, not just created.

Required checks:

- latest backup exists;
- backup is encrypted;
- backup size is plausible;
- restore into staging succeeds;
- schema/version compatibility passes;
- sample ECR record restores;
- sample token ledger restores;
- sample ECOR snapshot restores;
- sample live ECE history restores;
- sample requested analysis restores;
- audit search can query restored records.

## Current Implementation State

### Existing Foundations

The current repo has model-level foundations:

- Phase E defines production-safe persistence targets, indexes, restart recovery, and cleanup job requirements.
- Phase R defines retention and privacy requirements for live ECE history, requested analysis, audit, and telemetry.
- `modules/evenchess/src/main/AnalysisMemory.scala` models rolling last-10 recent game memory and last-100 requested analysis memory without retaining raw ECE payloads.
- `modules/evenchess/src/main/EvenChessRatingCalibration.scala` models ECOR snapshots and in-memory calibration sample capture/capping behavior.
- `modules/evenchess/src/main/SubscriptionTokensAds.scala` models token ledger entries and saved-game retention policy.
- Existing tests cover analysis memory limits, ECOR snapshot/sample behavior, token ledger events, and saved-game retention policy at model level.

### Not Yet Release-Proven

Phase W is not release-complete until:

- durable stores exist for all critical EvenChess state;
- backup jobs are implemented for the selected production/staging stores;
- restore has been tested in staging;
- retention jobs run automatically and safely;
- paid saved-game protection is enforced in real storage;
- calibration cap is enforced in real storage;
- token ledger balance reconstruction is tested from restored data;
- rating settlement replay/reconciliation is tested from restored data;
- maintenance jobs emit live telemetry and audit logs;
- destructive reset/cleanup scripts are environment-safe.

## Required Tests

Phase W implementation must have tests for:

- staging restore from backup;
- recent live history cleanup keeps 10 and preserves saved games;
- requested analysis cleanup keeps 100 and preserves saved records;
- cleanup skips active analysis jobs;
- calibration sample cap keeps latest 1,000,000 and deduplicates by game id;
- token ledger reconstruction;
- token idempotency after restore;
- ECR settlement idempotency after restore;
- ECOR snapshot restore;
- admin settings snapshot restore;
- raw payload privacy cleanup;
- maintenance job idempotency;
- maintenance job interruption/retry;
- backup verification failure alert.

## Patch Map Impact

Future Phase W implementation may touch database repositories, background workers, scheduler jobs, admin maintenance pages, deployment scripts, backup scripts, or monitoring config. Runtime Lichess seam changes must be patch-mapped and entered into the integration log.

This Phase W documentation pass does not itself change runtime code.

## Phase W Acceptance Status

Phase W is conducted as a readiness and maintenance-plan pass.

Status:

- Backup, restore, retention, cleanup, archive, and recovery requirements are defined.
- Release readiness remains blocked until durable stores, backup jobs, restore tests, retention jobs, protected saved-game behavior, token/rating recovery, and maintenance telemetry are implemented and proven in staging.

## Phase X Entry Criteria

Before legal/policy/public-copy work can be finalized, Phase W must provide clear user-facing facts for:

- retained live ECE history;
- requested analysis retention;
- paid saved-game persistence;
- audit retention boundaries;
- token ledger/accounting retention;
- data export/delete limits;
- calibration sample use and retention.
