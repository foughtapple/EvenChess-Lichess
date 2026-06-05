# EvenChess Plan v1.6 Phase Q - Security, Abuse, and Fairness Controls

## Phase Goal

Protect public EvenChess from common abuse, cheating, fairness drift, and operational misuse before deployment.

EvenChess is a disclosed assisted-chess variant. The security model must make that distinction enforceable:

- platform coaching is allowed only inside EvenChess;
- platform coaching must be disclosed, capped, server-authorized, audited, and priced into ECR;
- non-platform help remains prohibited in rated EvenChess;
- paid status must never make live rated help stronger.

## Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- Appendix S: Abuse, Fairness, and Trust Controls
- Appendix T: Operations, Deployment, and Incident Response
- `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`
- `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`
- `docs/requirements/planv1.6_phase_n_tokens_subscriptions_ads_entitlements.md`
- `docs/requirements/planv1.6_phase_o_admin_operator_console.md`
- `docs/requirements/planv1.6_phase_p_bot_matchmaking_simulation_operations.md`

## Security Boundary

EvenChess-Lichess must not expose private ECE internals.

Required boundary rules:

- browser/client code must not call ECE board, proposed-move, game-review, or analysis endpoints directly;
- ECE calls must remain server-to-server;
- public payloads must not include provider paths, raw prompts, raw provider output, API keys, tablebase paths, private engine data, or unrestricted Stockfish output;
- ECE debug IO must remain local/operator-only and disabled in production unless temporarily enabled for a specific incident;
- Test Ground and operator-only routes must not be exposed as public production features.

## Public Fair-Play Rules

Public rules must clearly state:

- EvenChess platform coaching is legal only in EvenChess games;
- non-platform engines, humans, bots, browser extensions, unaudited notes, and stream chat are prohibited in rated EvenChess;
- coaching is capped by Set Level;
- Used Level may increase and never decreases within a rated game;
- ECR settlement prices in actual Used Level/Used Offset;
- premium/subscription/token status does not grant stronger live rated help.

The site must avoid wording that implies hidden cheating, undisclosed engine use, normal chess with secret help, or pay-to-win live assistance.

## Rate Limits and Abuse Budgets

Public deployment requires server-side rate limits for:

- EvenChess search creation and resume;
- ECE board calls;
- ECE proposed-move calls;
- potential-move reveals;
- full-game analysis requests;
- custom analysis requests;
- token grants and ad/reward flows;
- subscription/payment webhook mutations;
- admin setting mutations;
- bot operations start/stop/resize/seed actions.

Existing admin settings include AI/TTS/Stockfish cost and rate-limit values, but release readiness requires proving these values are enforced in live routes rather than only represented in settings/models.

## Matchmaking and Rating Abuse Controls

Required monitoring and controls:

- repeat pairings;
- queue sniping;
- preferred-level manipulation;
- uneven-match manipulation;
- collusion patterns;
- rating transfer;
- abort abuse;
- bot fallback farming;
- simulation leakage into unintended production pools.

Rated EvenChess pairings must produce auditable match contracts. ECR settlement must use actual Used Level/Used Offset and must remain isolated from normal Lichess ratings.

Bot controls must include:

- fallback kill switch;
- simulation kill switch;
- bot-farming detection;
- repeat bot pairing detection;
- bot game no-rate/review path for incidents;
- public fallback disclosure when enabled.

## Token and Review Abuse Controls

Token and review features must enforce:

- no token farming through ads/rewards;
- daily or policy-defined caps for high-cost custom L10/full-game analysis;
- refund or no-charge behavior for failed eligible calls;
- abuse-state gating where appropriate;
- audit records for grants, consumption, refunds, and operator adjustments.

Paid status may affect access, volume, convenience, or non-live surfaces, but it must not increase live rated coaching strength.

## AI and Coaching Output Safety

AI and composer output must be bounded by deterministic ECE facts and EvenChess display policy.

Required controls:

- reject or ignore hidden higher-level requests;
- reject or ignore custom instructions requesting forbidden information;
- validate AI output for forbidden best-move wording where policy prohibits exact answers;
- never let AI invent chess truth;
- never expose raw provider output;
- record audit tags/source facts for delivered AI text;
- suppress stale, desynced, or unauthorized payloads with audit.

Proposed-move and potential-move features must consume server-authorized counters and must not leak higher-level data before server authorization.

## Admin and Web Security Controls

Required deployment controls:

- admin routes protected by existing Lichess admin permissions;
- CSRF protection on admin mutations;
- same-origin enforcement for browser-facing routes;
- secure cookie configuration appropriate for production;
- CSP reviewed for EvenChess-added scripts/styles;
- no public route useful to non-admin users for operator settings, Test Ground, ECE diagnostics, CLM diagnostics, bot controls, or ECOR calibration mutation;
- admin pages must not expose anti-cheat internals, provider secrets, raw prompts, raw ECE output, or private engine paths.

## Incident Response Controls

Operators must be able to:

- pause ECE live help;
- pause AI summaries;
- pause proposed-move analysis;
- pause potential-move reveals;
- pause custom/full-game review;
- pause ads/reward grants;
- pause token consumption if billing/token integrity is affected;
- pause bot fallback;
- pause simulation bots;
- apply no-rate/review/annul workflows for asymmetric assistance incidents;
- roll back fairness-affecting settings;
- publish safe public notices where needed.

Every fairness-affecting incident action must carry an audit id, visible reason, operator identity, timestamp, old value, new value, and policy/config version.

## Current Implementation State

### Existing Foundations

The current codebase has meaningful Phase Q scaffolding:

- `modules/evenchess/src/main/TrustOpsIncidentControls.scala` models trust signals, fair-play escalation, incident mapping, audited fairness remedies, probe suppression, runtime remedies, and acceptance evidence.
- `modules/evenchess/src/main/AbuseTrustControls.scala` models legal platform guidance versus prohibited outside help, exploit registers, matchmaking abuse signals, token/review abuse limits, engine/AI abuse guards, and operational feature flags.
- `modules/evenchess/src/main/AdminOpsDashboard.scala` models operator dashboards, incident actions, audit search, feature flags, and no-rate/pause/rollback paths.
- `modules/evenchess/src/main/AdminBackendSettings.scala` includes safety limit settings, audit retention, feature flags, provider settings, and kill-switch-like controls.
- `modules/evenchess/src/main/CoachingPolicy.scala`, `CoachingOverlays.scala`, `LiveBoardIntegration.scala`, and related overlay modules require audit identity and server authorization for live coaching render surfaces.
- `modules/evenchess/src/main/AiCoachPolicy.scala`, `OpenAiCoaching.scala`, and `OpenAiResponsesApi.scala` include source-fact/audit-tag constraints and AI output validation concepts.
- `modules/evenchess/src/main/PlaySearchIntegration.scala` includes abuse-clear admission gating and match-contract audit signal keys.
- Existing tests cover abuse classifications, outside-help distinction, exploit register coverage, audited fairness changes, high-volume probing, runtime remedies, matchmaking abuse policy, token/review abuse limits, engine/AI guardrails, feature flag audit requirements, incident records, and ops readiness evidence.

### Not Yet Release-Proven

Phase Q is not release-complete until these are proven in live routes and staging:

- no public/browser route can call ECE analysis endpoints directly;
- all EvenChess admin mutation routes have CSRF and admin-permission protection;
- rate limits are enforced at the actual route/service level for search, ECE calls, proposed/potential calls, review, token grants, and admin mutations;
- public fair-play rules and search/game disclosures are published and reviewed;
- outside-help reports can reach existing Lichess moderation with EvenChess ledger context;
- repeat-pairing, queue-sniping, collusion, rating-transfer, abort-abuse, and bot-farming signals are emitted from real runtime events;
- AI forbidden-best-move validation is wired into delivered text, not only modeled;
- feature kill switches actually suppress the relevant runtime features during an incident;
- no-rate/annul/review flows are connected to actual game/rating records;
- bot fallback and simulation cannot be farmed for ECR/token gains;
- admin audit records are durable, queryable, and not only in-memory/model-level.

## Required Tests

Phase Q implementation must have tests for:

- public routes cannot call ECE analysis directly;
- admin routes reject non-admin users;
- admin mutations reject missing/invalid CSRF;
- rate limits reject excessive search/ECE/proposed/potential/review/token/admin calls;
- outside-help reports include game id, player id, move range, ledger summary, and visible assistance state;
- repeat-opponent/collusion/queue-sniping signals are produced from match history;
- bot fallback farming is detected or blocked;
- AI exact-answer text is suppressed where policy forbids it;
- stale/desynced payloads clear or suppress overlays with audit;
- feature kill switches disable live help/proposed/potential/review/bots;
- no-rate/annul/review incident actions are audited;
- token abuse limits and refunds are enforced server-side;
- public copy scans reject cheating/pay-to-win/hidden-engine wording.

## Deployment Gate Checklist

Phase Q must block public deployment unless all of the following are true:

- security review finds no public ECE analysis path;
- browser code does not call ECE directly;
- production config does not expose ECE diagnostics/Test Ground/CLM/admin tools publicly;
- rated EvenChess rules are visible and unambiguous;
- rate limits are enforced for high-cost and abuse-prone flows;
- abuse events are logged and actionable;
- admin routes are protected and audited;
- kill switches work during an incident drill;
- no-rate/annul/review path is operational for asymmetric assistance incidents;
- bot fallback disclosure is correct when enabled;
- normal Lichess chess and normal Lichess rating remain isolated from EvenChess ECR changes.

## Patch Map Impact

Future Phase Q implementation may touch upstream Lichess auth, controller, moderation, report, game result, rating, lobby, route, or admin seams. Those changes must be patch-mapped and entered into the integration log.

This Phase Q documentation pass does not itself change runtime code.

## Phase Q Acceptance Status

Phase Q is conducted as a readiness and requirements pass.

Status:

- Security, abuse, trust, incident, and admin-control models exist.
- Unit tests cover several policy-level controls.
- Release readiness remains blocked until controls are wired into production routes, runtime telemetry, Lichess moderation/rating seams, durable admin audit, and incident drills.

## Phase R Entry Criteria

Phase R telemetry/audit/privacy work should use this Phase Q document as its abuse-control baseline. Before Phase R is considered complete, it must provide durable evidence for:

- security-relevant event capture;
- abuse signal capture;
- operator audit search;
- privacy-safe retention;
- redaction of secrets and private ECE internals;
- dashboards that show whether Phase Q controls are working.
