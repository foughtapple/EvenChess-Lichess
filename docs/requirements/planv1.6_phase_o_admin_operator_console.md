# EvenChess Plan v1.6 Phase O - Admin and Operator Console

## 1. Phase Goal

Phase O makes EvenChess operator controls safe enough for public deployment.

The deployment goal is:

- all EvenChess admin/operator pages are accessible only to authorized admin accounts;
- all mutating operator actions use CSRF-protected POST routes;
- every config change, bot operation, ECOR update, calibration action, token adjustment, incident control, and release switch change is versioned and audited;
- operators can inspect backend-safe ECE health/readiness without exposing ECE analysis endpoints or private internals;
- operators can monitor queue, bots, ECE latency, overlay delivery, token flow, ECR settlement, ECOR calibration, abuse, funnel, payments, and active versions;
- risky systems can be paused or rolled back without a deploy;
- local Test Ground diagnostics remain local-only and cannot bypass production admin auth;
- public pages do not expose Test Ground-only controls, internal ids, provider paths, secrets, raw prompts, raw provider outputs, anti-cheat internals, or ECE private implementation details.

## 2. Requirements Used

Authoritative inputs:

- `AGENTS.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/planv1.6_phase_d_config_secrets.md`
- `docs/requirements/planv1.6_phase_g_ecl_ece_gateway_hardening.md`
- `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`
- `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`
- `docs/requirements/planv1.6_phase_n_tokens_subscriptions_ads_entitlements.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

Implementation files inspected:

- `app/controllers/Dev.scala`
- `conf/routes`
- `modules/web/src/main/ui/DevUi.scala`
- `modules/web/src/main/Env.scala`
- `modules/evenchess/src/main/AdminBackendSettings.scala`
- `modules/evenchess/src/main/AdminOpsDashboard.scala`
- `modules/evenchess/src/main/AdminOperations.scala`
- `modules/evenchess/src/main/BotOperations.scala`
- `modules/evenchess/src/main/EvenChessRatingCalibration.scala`
- `modules/evenchess/src/main/TrustOpsIncidentControls.scala`
- `scripts/evenchess-testground.ps1`
- `scripts/evenchess-testground-panel.js`
- `modules/evenchess/src/test/AdminBackendSettingsTest.scala`
- `modules/evenchess/src/test/AdminOpsDashboardTest.scala`
- `modules/evenchess/src/test/AdminOperationsTest.scala`
- `modules/evenchess/src/test/BotOperationsTest.scala`
- `modules/evenchess/src/test/EvenChessRatingCalibrationTest.scala`

## 3. Admin Authority Boundary

Admin/operator authority belongs to ECL server-side code only.

Admin pages may:

- view sanitized settings and health;
- update EvenChess backend settings through authorized SettingStore routes;
- start/stop bot matchmaking;
- start/stop/seed bot simulation;
- update ECOR tables;
- update Stockfish AI equivalent-rating tables;
- run ECOR calibration;
- apply or restore ECOR snapshots;
- pause risky systems;
- inspect sanitized audit and incident summaries.

Admin pages must not:

- expose ECE raw analysis endpoints to browser clients;
- expose ECE provider paths, secrets, model weights, tablebases, raw prompts, raw provider outputs, or private cache paths;
- expose anti-cheat internals;
- change rated fairness without a versioned policy/config path and audit id;
- let Test Ground local actions mutate production settings;
- make unauthenticated bot or calibration changes;
- rely on browser-provided authority for bot/simulation/token/ECOR decisions.

## 4. Required Admin Surfaces

Minimum production admin surfaces:

- backend settings page;
- EvenChess operations dashboard;
- bot matchmaking controls;
- bot simulation controls;
- ECE status/readiness monitor;
- ECE latency/degradation monitor;
- ECOR table editor;
- rating-to-level table editor;
- Stockfish AI equivalent-rating table editor;
- ECOR calibration runner and comparison view;
- ECOR snapshot history and restore controls;
- token/entitlement ledger lookup;
- token adjustment/refund/restore tool;
- incident controls and public notice editor;
- release kill switches;
- feature flag/rollback view;
- audit search;
- queue/bot health panel;
- ECR settlement/rating residual panel;
- payment/rewarded-ad/funnel readiness panel.

## 5. Authorization and CSRF Requirements

Authorization rules:

- `/dev/settings` and `/dev/evenchess/*` routes require Lichess `Settings` admin permission or stricter;
- non-admin users receive normal access denial;
- admin access is based on authenticated account permission, not local Test Ground state;
- Test Ground may grant local admin access only in local/dev if explicitly implemented and must not affect production.

CSRF rules:

- all mutating routes use POST;
- all mutating routes use Lichess CSRF-protected form helpers or equivalent `SecureBody` handling;
- no mutating operation is reachable by GET;
- API-style admin mutations, if later added, require CSRF or scoped token protection plus admin permission.

Current routes inspected:

- `GET /dev/settings`;
- `POST /dev/settings/:id`;
- `GET /dev/evenchess/ops`;
- `GET /dev/evenchess/ops/bots`;
- `POST /dev/evenchess/ops/bots/:action`;
- `GET /dev/evenchess/ops/ecor`;
- `POST /dev/evenchess/ops/ecor/:action`.

## 6. Audit Requirements

Every mutating admin operation must persist a durable audit event.

Required audit fields:

- audit id;
- admin account id;
- route/action;
- setting id or target object;
- before value, redacted where needed;
- after value, redacted where needed;
- reason;
- fairness-affecting flag;
- rollback value or rollback version;
- policy/config version;
- incident id where applicable;
- timestamp;
- request id;
- source IP/session metadata according to existing Lichess admin policy.

Audit rules:

- raw-looking secrets are rejected before persistence;
- logged values are redacted;
- fairness-affecting changes require a non-empty reason;
- ECOR updates append snapshot history;
- bot start/stop/update actions record admin id and runtime revision;
- token adjustments require ledger entries, not direct balance edits;
- no-rate/pause/downgrade actions require versioned policy context.

## 7. ECE Operator Visibility

Production admin should show ECE status through ECL backend-safe calls only.

Required ECE status fields:

- configured base URL, redacted to service label where needed;
- health status;
- ready status;
- mode label;
- engine/provider readiness summary without filesystem paths;
- quick endpoint latency;
- deep endpoint latency;
- proposed-move endpoint latency;
- game-review endpoint latency;
- recent error rate;
- circuit-breaker/backpressure state;
- debug IO logging enabled/disabled status without raw payload display;
- ECE version/schema versions.

Rules:

- browser/admin UI never calls ECE analysis endpoints directly;
- local ECE settings page may be opened only from Test Ground/local diagnostics;
- public navigation must not link to local ECE settings;
- production admin must not show ECE provider paths, API keys, prompts, local logs, cache paths, model weights, or tablebases.

## 8. Monitoring Requirements

Required monitoring panels:

- engine/AI/TTS health;
- ECE gateway health and latency;
- overlay delivery and stale/mismatched payloads;
- assistance accounting;
- Used Level and Used Offset;
- ECR settlement and residuals;
- ECOR calibration sample health;
- queue time and failed match rate;
- matchmaking bot fallback health;
- simulation bot runtime and active tickets;
- token grants/consumption/refunds;
- rewarded-ad events;
- payment/subscription events;
- funnel/purchase/cancellation readiness;
- abuse/fair-play incident load;
- feature flag and rollback status;
- active policy/model/config/engine versions.

Monitoring data must come from real server telemetry before production launch. Sample/default rows are acceptable only for local scaffolding.

## 9. Current Implementation State

Foundations already present:

- `/dev/settings` uses `Secure(_.Settings)`;
- `/dev/settings/:id` uses `SecureBody(_.Settings)`;
- `/dev/evenchess/ops` uses `Secure(_.Settings)`;
- bot and ECOR mutating routes use `SecureBody(parse.tolerantFormUrlEncoded)(_.Settings)`;
- `DevUi` uses `postForm` helpers for admin mutations;
- EvenChess backend setting ids are namespaced under `evenchess.backend.*`;
- raw-looking EvenChess backend secret values are rejected before SettingStore persistence;
- admin log values are redacted when they look like secrets;
- default backend settings are conservative: payments, tokens, rewarded ads, bot simulation, AI surfaces, and risky campaign paths default off/paused;
- admin settings include AI/TTS/Stockfish controls, feature flags, monetisation switches, bot controls, ECOR tables, cost/rate limits, audit retention, and incident pause/no-rate controls;
- `/dev/evenchess/ops` exposes panels for active versions, runtime dashboards, operator actions, audit search, incidents, paid launch readiness, feature flags, bot operations, and ECOR;
- bot operations panel exposes matchmaking and simulation controls;
- ECOR panel exposes active ECOR table, equivalent rating-to-level table, Stockfish AI rating bands, calibration run/apply, and snapshot restore controls;
- tests cover admin settings namespacing, conservative defaults, raw secret rejection/redaction, fairness-affecting control classification, config-change audit model validity, dashboard panels, audit search safety, paid launch fail-closed rows, incidents, feature flags, and ops requirements.

Deployment blockers:

- current dashboard health values are generated from default/synthetic snapshots, not real telemetry;
- audit search uses sample rows, not durable audit ledger queries;
- settings/bot/ECOR mutations log to `lila.log` and SettingStore but do not yet persist full durable config-change audit events with audit ids and reasons for every action;
- token adjustment/refund/restore tooling is not implemented end to end;
- ECE health/readiness is available through Test Ground scripts/panel, but a production admin backend-safe ECE monitor is not proven;
- admin authorization/CSRF behavior has not been proven by integration tests;
- bot and ECOR mutating routes need explicit audit records and rollback metadata before production;
- Test Ground is local tooling and must not be considered production admin;
- code-health note: `app/controllers/Dev.scala` currently shows a duplicate local `webSettings` declaration inside `evenChessEcorOps`; build verification should catch or remove it before release.

## 10. Test Ground Boundary

Test Ground may:

- start/stop local Docker/WSL stack;
- start/stop real/test ECE locally;
- open local ECE settings page;
- open local ECE CLM page through a local proxy;
- run local smoke checks;
- assist local admin bootstrap where explicitly local.

Test Ground must not:

- be deployed as public admin;
- bypass production admin permission;
- expose ECE internals publicly;
- control production bots without admin auth;
- control production token/payment state;
- be linked from public navigation.

## 11. Required Tests

Authorization tests:

- non-admin cannot access `/dev/settings`;
- non-admin cannot access `/dev/evenchess/ops`;
- non-admin cannot access bot or ECOR panels;
- non-admin POST to bot/ECOR mutations is denied;
- admin with `Settings` permission can access read-only admin pages;
- admin with `Settings` permission can submit allowed mutations.

CSRF tests:

- mutating admin POST without CSRF is rejected where Lichess CSRF is required;
- mutating admin GET is unavailable;
- bot/ECOR forms include CSRF token through `postForm`.

Audit tests:

- settings change writes durable config-change audit;
- bot start/stop writes durable audit;
- simulation seed writes durable audit;
- ECOR update writes durable audit and snapshot;
- calibration run/apply/restore writes durable audit;
- fairness-affecting incident change requires reason and versioned policy path;
- secret-looking value is rejected and not logged raw;
- token adjustment requires ledger event and admin reason.

Monitoring tests:

- dashboard reads real telemetry source;
- dashboard degrades when ECE health fails;
- dashboard degrades when queue time/fail rate exceeds thresholds;
- dashboard shows bot runtime and simulation active tickets;
- dashboard shows ECOR policy version and latest calibration;
- dashboard shows token/payment paths unavailable when disabled.

Browser/manual tests:

- admin can open operations dashboard;
- normal user receives access denied;
- bot controls change only admin settings;
- ECOR save/run/apply/restore works and records snapshot/audit;
- local ECE settings link exists in Test Ground only;
- public pages do not show admin/Test Ground controls.

## 12. Patch Map and Integration Impact

Patch-map entries are required when implementation edits touch:

- `app/controllers/Dev.scala`;
- `conf/routes`;
- `modules/web/src/main/ui/DevUi.scala`;
- admin permission seams;
- setting persistence;
- telemetry/audit repositories;
- token adjustment routes;
- bot operation routes;
- ECE gateway monitoring routes.

No patch-map update is required for this Phase O requirements-only output.

## 13. Implementation Work Items

O1. Add durable admin config-change audit repository and write events for all EvenChess admin mutations.

O2. Add explicit reason/audit-id fields to fairness-affecting admin forms.

O3. Replace sample audit search rows with durable audit queries.

O4. Replace synthetic dashboard health with real telemetry sources.

O5. Add backend-safe ECE health/readiness/latency monitor for production admin.

O6. Add token adjustment/refund/restore admin tooling backed by the token ledger.

O7. Add integration tests for admin authorization and CSRF.

O8. Add dashboard tests for real telemetry degradation states.

O9. Ensure Test Ground links and local ECE settings are excluded from production navigation.

O10. Build-verification cleanup for `Dev.scala` and any admin route compile warnings.

## 14. Phase O Acceptance Status

Status: conducted, not deployment-complete.

Completed foundations:

- admin pages and EvenChess ops routes exist;
- routes use existing Lichess `Settings` permission;
- mutating routes are POST/SecureBody-based;
- settings are namespaced and conservative by default;
- bot and ECOR panels exist;
- secret-looking setting values are rejected/redacted;
- dashboard model and tests cover required admin concepts.

Remaining release blockers:

- durable config-change audit;
- live telemetry-backed dashboard;
- production ECE health monitor;
- token adjustment tooling;
- admin auth/CSRF integration tests;
- real audit search;
- production proof that Test Ground cannot bypass admin.

## 15. Phase P Entry Criteria

Before Phase P bot operations can be considered production-ready:

- bot controls must be admin-only with durable audit;
- public bot disclosure must read from server settings;
- simulation controls must not be reachable from Test Ground in production;
- dashboard must show real bot runtime, active tickets, failures, and stop status.
