# Plan v1.6 Phase Y - Release Candidate, Freeze, and Rollback

## 1. Phase Goal

Phase Y defines the release-candidate freeze, final verification checklist, known-issue acceptance process, rollback runbook requirements, and explicit go/no-go decision gates for EvenChess public deployment.

This phase is a readiness-control phase. It does not declare the current repository release-ready. The current repository remains blocked from RC until the gates below are satisfied.

## 2. Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/README_V2_REQUIREMENTS_SUITE.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `docs/requirements/APPENDIX_Z_V2_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md`
- `docs/requirements/planv1.6_phase_a_scope_freeze.md`
- `docs/requirements/planv1.6_phase_b_repo_hygiene.md`
- `docs/requirements/planv1.6_phase_s_ci_cd_build_release_automation.md`
- `docs/requirements/planv1.6_phase_t_staging_environment.md`
- `docs/requirements/planv1.6_phase_u_automated_test_matrix.md`
- `docs/requirements/planv1.6_phase_v_browser_device_performance_qa.md`
- `docs/requirements/planv1.6_phase_w_backup_recovery_maintenance_jobs.md`
- `docs/requirements/planv1.6_phase_x_legal_policy_public_copy.md`

Key source requirements:

- `REQ-MAIN-V2-005`: upstream/core Lichess edits must be recorded in patch map or integration log.
- `REQ-MAIN-V2-008`: implementation reports must state changed files, tests, seams, rollback notes, and risks.
- `REQ-B-V2-030`: every upstream seam must include file, reason, requirement link, risk, tests, rollback note, and isolation status.
- `REQ-T-V2-031`: each phase must include rollback notes and tests.
- `REQ-U-V2-003`: every seam must be listed in integration log with rollback notes.
- Cross-phase release gates in `planv1.6.md` Section 7.

## 3. Current RC Status

Phase Y is conducted as an RC readiness definition.

The current repo is not ready to freeze or cut as a release candidate.

Observed local blocker snapshot:

- `git status --short` currently reports 252 changed/untracked/staged status entries.
- Patch map and integration log are modified and need final reconciliation.
- Many implementation files are staged/added/modified, and some remain untracked or partially staged.
- Earlier phases B, D, E, F, G, H, I, J, L, M, N, O, P, Q, R, S, T, U, V, W, and X still record release blockers.

RC approval must therefore be explicit and must not be inferred from local tests or from this document existing.

## 4. Freeze Rules

Once RC freeze begins:

1. No broad rewrites.
2. No new product scope.
3. No new public feature without owner approval and a rollback switch.
4. Only release-blocking fixes, test fixes, docs/evidence updates, and rollback/runbook corrections may land.
5. Every code change during freeze must state:
   - blocker fixed;
   - files changed;
   - requirement link;
   - tests run;
   - rollback note;
   - risk of not taking the fix.
6. Every upstream/core Lichess seam changed during freeze must update patch map and integration log in the same change.
7. Feature flags must default conservatively.
8. Rated fairness behavior must not change silently for active games.

Non-blocking polish must be deferred to the next version unless explicitly approved as a launch blocker.

## 5. RC Branch and Source Checklist

Before RC can be cut:

- release branch exists and is named intentionally;
- `git status --short` is clean except explicitly approved release artifacts;
- all intended implementation files are tracked;
- no required implementation/test/doc file is left untracked;
- no stale staged state exists;
- old deleted/superseded requirements are either intentionally removed, archived, or restored;
- active requirements are linked from the main requirements suite;
- Appendix Z records all superseded behavior;
- patch map covers every upstream/core Lichess file touched;
- integration log covers every Lichess seam and includes rollback notes;
- no ECE private implementation, provider paths, raw prompts, raw provider output, model weights, tablebases, generated caches, `.env`, API keys, or secrets are tracked in ECL;
- generated build artifacts are excluded.

Required command evidence:

```bash
git status --short
git diff --check
git ls-files --others --exclude-standard
git diff --name-only --cached
git diff --name-only
```

Any non-empty result must be classified before RC approval.

## 6. Build and CI Checklist

RC evidence must include:

- clean fresh-environment dependency install;
- ECL backend compile;
- ECL backend EvenChess tests;
- TypeScript/UI tests;
- round overlay/display tests;
- lobby/search serialization tests;
- admin/settings tests;
- bot operation tests;
- ECR/ECOR settlement tests;
- ECE adapter contract tests;
- normal Lichess regression smoke;
- build metadata/version visible to operators;
- release artifact creation;
- artifact checksum or immutable image tag.

Minimum command evidence should include the current accepted equivalents of:

```bash
./lila.sh evenchess/compile
./lila.sh evenchess/test
pnpm test
pnpm exec tsx --test ui/lobby/tests/evenchessSetup.test.ts
pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts
```

If commands evolve, the RC report must record the replacement command and why.

## 7. Staging Smoke Checklist

RC cannot proceed without staging smoke against production-like topology:

- ECL starts from release artifact/image;
- ECE starts as separate private service;
- browser cannot call ECE analysis endpoints directly;
- ECL server can call ECE quick/deep/proposed endpoints;
- Redis/Mongo/search/mail/payment/ad dependencies use staging-safe config;
- admin can view build version and enabled flags;
- human search creates game;
- bot fallback can create game when enabled;
- simulation bots can queue/play/requeue when enabled;
- computer training game remains playable;
- clocks work for human-human, human-bot, and bot-bot games;
- move triggers ECE payload update without browser refresh;
- quick/deep stale payloads are handled correctly;
- coach text updates only on the user's turn where applicable;
- overlays, eval bar, level card, and coach card stay stable;
- proposed move and potential move consumables survive refresh and clear correctly after move;
- game finish triggers ECR settlement for rated EvenChess;
- normal Lichess ratings remain untouched;
- token reserve/consume/refund flow works;
- admin kill switches disable live help, proposed/potential, bots, token gates, and rated/casual entrypoints as expected;
- logs and public JSON contain no private ECE internals.

## 8. Load and Performance Checklist

RC evidence must include load testing with:

- concurrent searches;
- human-bot fallback after configured timeout;
- simulation bots at configured population;
- quick ECE board requests;
- deep ECE board requests;
- proposed move requests;
- potential move reveal requests;
- ECR settlement throughput;
- audit/telemetry writes;
- token ledger updates;
- retention/cleanup jobs not running during latency-sensitive windows.

Load report must include:

- scenario parameters;
- peak concurrent users/bots;
- ECE latency percentiles;
- ECL request latency percentiles;
- queue time distribution;
- bot fallback rate;
- error rates;
- stale payload rejection rate;
- resource use for ECL, ECE, database, Redis, and proxy;
- pass/fail decision.

## 9. Security and Privacy Checklist

RC security checklist must prove:

- admin routes require admin permission;
- admin mutations require CSRF or equivalent scoped protection;
- browser bundles contain no private ECE service URLs, provider paths, secrets, prompts, raw outputs, or internal diagnostics;
- ECE service is private and not internet-routable;
- public search/status JSON is deployment-safe;
- token/bot/rating/admin actions are server-authoritative;
- outside-help report and moderation paths exist;
- rate limits exist for search, ECE-backed actions, proposed/potential moves, bot controls, token endpoints, and admin mutation paths;
- audit logs redact secrets and private ECE internals;
- retention jobs do not keep unnecessary raw AI/provider data;
- privacy/export/delete policy is documented.

Required scans:

```bash
grep -RInE 'api[_-]?key|secret|password|provider_path|raw_prompt|raw_provider_output|external_engines|tablebases|\\.env' .
grep -RInE '127\\.0\\.0\\.1:8787|localhost:8787|/v1/ece/board|/v1/ece/proposed-move' ui app public
```

Findings must be triaged as safe local docs/tests, blocked leak, or false positive.

## 10. Backup and Restore Checklist

Before RC approval:

- Mongo backups exist and restore has been tested;
- Redis or queue state recovery policy is defined;
- ECE persistent cache/history backup policy is defined;
- ECE generated/provider data regeneration policy is defined;
- object/file storage backups exist if paid saved games or analysis exports are stored there;
- token ledger restore is proven;
- ECR/ECOR restore is proven;
- ECOR table rollback snapshot is proven;
- retention cleanup jobs are tested;
- maintenance windows are documented.

Restore evidence must include a dry-run or actual staging restore, not just backup creation.

## 11. Feature Flag and Kill Switch Checklist

Every deployable subsystem must have a conservative flag or operator control:

- EvenChess public entrypoints;
- rated EvenChess;
- casual EvenChess;
- computer training;
- ECE quick;
- ECE deep;
- ECE AI/coaching text;
- overlays;
- eval bar;
- proposed move;
- potential move;
- analysis/review;
- token gates;
- subscriptions;
- ad rewards;
- bot fallback;
- simulation bots;
- ECOR calibration writes;
- admin ECOR editing;
- public debug search card.

Kill-switch drill must prove:

- switch takes effect without redeploy where required;
- active rated games are handled according to fairness policy;
- operator action is audited;
- public copy/status remains safe;
- rollback value is recorded.

## 12. Rollback Runbook

The release rollback runbook must include steps for each layer.

### 12.1 ECL App Rollback

1. Disable new public entrypoints if needed.
2. Disable rated/casual EvenChess creation if game policy is compromised.
3. Deploy previous known-good ECL image/artifact.
4. Verify health, login, lobby, active games, and admin.
5. Confirm no new schema incompatibility blocks old app.
6. Record incident/audit entry.

### 12.2 ECE Service Rollback

1. Disable ECE deep/AI/proposed/potential if ECE is faulty but games can continue.
2. Keep games playable with stale/empty coaching according to game policy.
3. Roll ECE service to previous known-good image/version.
4. Verify `/health` and `/ready`.
5. Run board/proposed smoke from ECL server-side path.
6. Confirm ECE is still private.

### 12.3 Database Migration Rollback

1. Identify whether migration is reversible.
2. If reversible, run tested down migration in staging first unless production incident severity prevents it.
3. If irreversible, restore from backup or deploy compatibility shim.
4. Protect token ledger, ECR settlement, ECOR tables, audit logs, and game policy records from partial rollback corruption.
5. Record affected users/games and support plan.

### 12.4 Feature Flag Rollback

1. Disable affected feature through admin/operator settings.
2. Confirm flag state is persisted and audited.
3. Confirm public UI no longer exposes disabled feature.
4. Confirm active games use documented fallback behavior.

### 12.5 Payment/Token Rollback

1. Disable paid checkout/ad rewards if provider integration is faulty.
2. Preserve token ledger immutability.
3. Stop new charges/rewards before data repair.
4. Reconcile provider webhooks and local ledger.
5. Issue refunds/adjustments only through audited correction tooling.

### 12.6 Bot Rollback

1. Disable bot fallback and simulation flags.
2. Clear simulation tickets.
3. Let active games finish or abort according to game policy.
4. Confirm public bot fallback disclosure changes to Off.
5. Record bot incident metrics.

### 12.7 Rating Settlement Rollback

1. Pause ECR settlement if calculation is faulty.
2. Mark affected games pending/no-rate according to policy.
3. Do not mutate normal Lichess ratings.
4. Recompute ECR from durable game policy and Used Offset data after fix.
5. Audit all corrections.

## 13. Known-Issue Acceptance Process

Any known issue at RC must be classified:

| Class | Meaning | RC Decision |
| --- | --- | --- |
| P0 | data loss, security leak, active game breakage, payment corruption, rating corruption | Must fix before RC |
| P1 | public-game flow, ECE payload, matchmaking, token, bot, admin, or privacy blocker | Must fix or explicitly disable feature |
| P2 | non-critical UX, copy, local-only tooling, non-launch feature | May accept with owner/date |
| P3 | cosmetic/internal cleanup | May defer |

Known-issue acceptance must record:

- issue id/title;
- affected feature;
- severity;
- evidence;
- mitigation;
- owner;
- target version/date;
- launch decision.

## 14. RC Report Template

The RC report must include:

```text
# EvenChess v1.6 Release Candidate Report

## Candidate
- branch:
- commit:
- build id:
- ECL artifact:
- ECE artifact/version:
- database migration version:
- ECOR table version:
- feature flag snapshot:

## Requirements State
- active requirements:
- Appendix Z current:
- patch map current:
- integration log current:
- known superseded docs archived/removed:

## Test Evidence
- CI:
- backend:
- UI:
- browser:
- staging smoke:
- load:
- security:
- backup/restore:

## Public Readiness
- rules:
- terms/privacy:
- support FAQ:
- bot disclosure:
- payment/token copy:

## Known Issues
- P0:
- P1:
- accepted P2/P3:

## Rollback Evidence
- app rollback:
- ECE rollback:
- DB rollback/restore:
- feature flags:
- payments/tokens:
- bots:
- ratings:

## Decision
- Go / No-Go:
- approver:
- date/time:
- conditions:
```

## 15. Release Notes Requirements

Release notes must include:

- EvenChess assisted mode launch scope;
- known disabled features;
- ECR and level model summary;
- bot fallback policy if enabled;
- token/subscription status if enabled;
- admin/operator changes;
- migrations;
- rollback notes;
- known issues;
- support instructions.

Release notes must not include:

- private ECE paths;
- provider names/paths beyond public-safe status if approved;
- raw prompts;
- raw provider output;
- internal bot seed diagnostics;
- exploitable abuse or anti-cheat internals.

## 16. Explicit Go/No-Go Gate

RC approval requires a written decision:

- `GO`: all P0/P1 blockers fixed or feature-disabled, evidence complete, rollback ready.
- `NO-GO`: one or more blocking gates failed.
- `CONDITIONAL GO`: allowed only for private beta with named disabled features, owners, monitoring, and rollback.

Public paid launch requires separate explicit approval after payment terms, privacy, support, and provider/webhook evidence are complete.

## 17. Phase Y Acceptance Status

Phase Y is conducted as the RC/freeze/rollback readiness definition.

Current acceptance status:

- RC checklist is defined;
- freeze rules are defined;
- rollback runbook requirements are defined;
- known-issue acceptance process is defined;
- RC report template is defined;
- current repo is not RC-ready due to dirty/untracked/staged state and unresolved blockers from earlier phases.

## 18. Phase Z Entry Criteria

Do not enter Phase Z production launch until:

1. RC report is complete.
2. Release branch is clean.
3. Full CI/staging/browser/load/security/backup evidence is complete.
4. Patch map and integration log are current.
5. No required implementation files are untracked.
6. No private ECE internals/secrets are present in ECL.
7. Rollback has been tested or dry-run.
8. Public rules/copy/terms/privacy/support FAQ are ready.
9. Go/no-go decision is explicitly `GO` or approved `CONDITIONAL GO` for private beta only.
