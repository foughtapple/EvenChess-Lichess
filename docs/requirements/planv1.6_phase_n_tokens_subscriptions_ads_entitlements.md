# EvenChess Plan v1.6 Phase N - Tokens, Subscriptions, Ads, and Entitlements

## 1. Phase Goal

Phase N makes EvenChess monetisation deployable without changing rated fairness.

The deployment goal is:

- account entitlements are stored server-side in a durable EvenChess account extension;
- token balances are derived from an append-only ledger, not browser state;
- subscriptions, payments, rewarded ads, and operator adjustments are idempotent and auditable;
- game-token access is checked before search/game creation and settled from server-owned game outcomes;
- high-cost review actions such as custom/full-game L10 analysis are gated by review tokens or subscription quotas;
- failed review generation follows explicit spend/refund/no-spend policy;
- premium and paid plans never grant stronger live rated help, lower Used Offset, alter Set Level, alter ECR, or bypass fair-play controls;
- user-facing account/billing pages use production-safe copy and do not expose debug/provider internals;
- support/admin tooling can inspect and correct token/subscription state through ledger-backed operations.

## 2. Requirements Used

Authoritative inputs:

- `AGENTS.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/planv1.6_phase_e_persistence_migrations.md`
- `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`
- `docs/requirements/planv1.6_phase_j_game_policy_live_assistance_authority.md`
- `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

Implementation files inspected:

- `modules/evenchess/src/main/MonetisationPolicy.scala`
- `modules/evenchess/src/main/SubscriptionTokensAds.scala`
- `modules/evenchess/src/main/AccountMonetisationUi.scala`
- `modules/evenchess/src/main/PlaySearchIntegration.scala`
- `modules/evenchess/src/main/LiveCoaching.scala`
- `modules/evenchess/src/main/AdminBackendSettings.scala`
- `modules/evenchess/src/main/AdminOpsDashboard.scala`
- `modules/evenchess/src/main/MarketingFunnelPolicy.scala`
- `app/controllers/EvenChess.scala`
- `app/views/evenchess/account.scala`
- `app/controllers/Plan.scala`
- `conf/routes`
- `modules/web/src/main/Env.scala`
- `modules/evenchess/src/test/MonetisationPolicyTest.scala`
- `modules/evenchess/src/test/SubscriptionTokensAdsTest.scala`
- `modules/evenchess/src/test/AccountMonetisationUiTest.scala`

## 3. Fairness Boundary

Monetisation may affect:

- access to game starts;
- rewarded-ad token earning;
- review/custom-analysis quantity;
- saved-game retention slots;
- account page visibility;
- support/admin correction flows.

Monetisation must not affect:

- live rated Set Level;
- live Used Level;
- Assistance Load;
- Used Offset;
- ECR settlement;
- ECOR calibration inputs except as explicitly tagged monetisation metadata;
- Stockfish profile strength;
- AI exactness class;
- live coaching strength;
- matchmaking fairness windows;
- result authority;
- fair-play enforcement.

Every entitlement snapshot attached to a search, game, review, or audit event must state that it is fairness-neutral.

## 4. Entitlement Model

Required account entitlement fields:

- account id;
- ECR account key;
- plan tier;
- game token balance;
- rewarded-ad token balance;
- match summary token balance;
- performance summary token balance;
- premium daily match-summary remaining;
- premium daily performance-summary remaining;
- custom-analysis tokens;
- match-review tokens;
- full-analysis tokens;
- saved game ids or saved-game counter references;
- leaderboard eligibility flag;
- abuse-clear flag or reference;
- schema version;
- updated timestamp.

Plan tiers:

- New Account Onboarding: grants starter game and summary tokens only;
- Free Ad Supported: may earn capped rewarded-ad game tokens;
- Standard: paid access/convenience only, no stronger live help;
- Premium: Standard access plus review-summary quantity/convenience only, no stronger live help.

Initial policy values already represented in code:

- onboarding: 10 game tokens, 3 match summaries, 1 performance summary;
- Standard: `$10 AUD / 4 weeks`;
- Premium: `$16 AUD / 4 weeks`, 10 match summaries/day, 1 performance summary/day;
- rewarded ad cap: 3 earned game tokens;
- rewarded ad grant: 1 game token per verified completed reward.

## 5. Token Ledger Requirements

Token balance must be computed from ledger-backed state.

Required ledger event classes:

- onboarding grant;
- rewarded ad earned;
- game token reserved;
- game token consumed;
- game token refunded;
- game token expired;
- review token granted;
- review token consumed;
- review token refunded;
- subscription quota granted;
- subscription quota consumed;
- operator adjustment;
- abuse suppression;
- privacy deletion/anonymization marker.

Required ledger fields:

- ledger entry id;
- idempotency key;
- account id;
- token bucket;
- event type;
- amount;
- signed balance delta;
- reason;
- game id where applicable;
- analysis id where applicable;
- provider event id where applicable;
- admin actor id where applicable;
- audit id;
- schema version;
- created timestamp.

Ledger rules:

- append-only for financial/support reconstruction;
- idempotency key prevents duplicate grants/spends/refunds;
- balance cannot be forged by browser;
- negative balances are blocked unless an explicit operator debt policy is later approved;
- operator adjustments require reason, admin actor, and audit event;
- refunds reference the original reservation/spend;
- token expiry is explicit and auditable.

## 6. Game Token Flow

Search/game-start flow:

1. Server loads durable entitlement state.
2. Server creates a fairness-neutral token snapshot.
3. Search admission checks whether the mode needs token access.
4. If required, the server reserves or confirms eligible access before final game creation.
5. Native Lichess game creation proceeds only after token admission succeeds.

Settlement flow:

- failed queue: no token consumed;
- valid game reaches meaningful play: token consumed;
- opponent aborts before meaningful play: reserved token refunded;
- token holder aborts after accepting or meaningful play: token may be consumed and cooldown may apply;
- platform outage abort: reserved token refunded;
- subscription access: no game token ledger spend for normal game access.

Deployment requirement:

- meaningful play must be determined from server-owned Lichess game lifecycle, not from browser state;
- token reservation and final settlement must be idempotent;
- game creation retries must not double-spend;
- bot/simulation games must carry explicit token policy so operators can test without billing real users.

## 7. Review Token and Summary Quota Flow

No token required:

- replaying retained live ECE history;
- local display toggles;
- analysis cache hits where policy says no re-spend.

Token/quota may be required:

- custom ECE analysis above retained saved levels;
- L10 full-game analysis;
- full-game ECE review;
- AI narrative summaries;
- match summaries;
- performance summaries;
- saved-game retention exceptions.

Rules:

- token/quota check occurs before ECE full-game review request preparation;
- failed ECE full-game review follows explicit refund/no-spend policy;
- failed summary generation does not consume quota unless a future policy explicitly changes this and records it in requirements;
- premium daily quotas reset by server job and are ledger/audit visible;
- subscription quotas are convenience/review quantity only.

## 8. Subscription and Payment Provider Requirements

Payments may use existing Lichess payment foundations only through a narrow EvenChess adapter.

Provider activation requirements:

- provider credentials configured through secure server config only;
- webhook signature verification enabled;
- webhook idempotency by provider event id;
- checkout session id linked to account id and intended plan;
- plan activation is server-verified, not query-string/browser-authorized;
- cancellation/downgrade/refund/chargeback events are handled;
- subscription state changes write EvenChess subscription events and entitlement ledger entries;
- payment failures do not affect live rated games already started;
- checkout and billing pages do not expose secrets or provider internals.

Current production gate:

- `AccountMonetisationUi.EntitlementSource.checkoutProviderVerified` is `false`;
- EvenChess payment/rewarded-ad feature switches default to disabled;
- real checkout/rewarded-ad provider callbacks must remain unavailable until verification, idempotency, and support tooling are complete.

## 9. Rewarded Ad Requirements

Rewarded ads may grant capped game tokens to free accounts only.

Requirements:

- rewarded ad provider callback is verified server-side;
- callback idempotency prevents duplicate rewards;
- rewarded ad tokens are capped;
- subscribed accounts cannot farm rewarded ad tokens;
- campaign/health pause disables grants;
- abuse cooldown disables grants with an audit id;
- reward completion writes ledger and attribution events;
- ads never grant stronger live help or ECR advantage.

## 10. User-Facing Account and Billing Surface

Required surfaces:

- account entitlement dashboard;
- top-bar token/plan indicator;
- plan comparison;
- rewarded-ad status;
- summary quota status;
- game token settlement explanations;
- saved-game retention status;
- support/contact path for billing/token issues.

Copy requirements:

- production-safe wording only;
- no mock/test/provider labels on public pages;
- state clearly that plans/tokens/rewards never change live rated help strength;
- failed summary/review policy stated plainly;
- plan upgrades hidden or disabled while provider verification is incomplete.

Current state:

- account page and top-bar indicator exist;
- account page uses production-safe fairness copy;
- plan upgrade links are suppressed while checkout provider verification is false;
- displayed dashboard is currently generated from an onboarding-style snapshot rather than a durable entitlement repository.

## 11. Admin and Support Requirements

Admin/support tools must include:

- entitlement lookup by account;
- token ledger search;
- subscription event search;
- rewarded-ad event search;
- idempotency-key lookup;
- manual grant/refund/adjustment with reason;
- pause switches for token, ad, payment, and campaign paths;
- audit export for support incidents;
- chargeback/refund handling;
- privacy deletion/anonymization handling.

Admin tools must:

- require admin permission;
- use CSRF protection for mutations;
- write audit entries for every change;
- avoid exposing secrets;
- avoid changing live rated fairness fields.

## 12. Current Implementation State

Foundations already present:

- `MonetisationPolicy.scala` defines plan tiers, onboarding grants, rewarded-ad token cap, token consumption/refund policy, summary access, non-transferability, and fairness boundaries;
- `SubscriptionTokensAds.scala` defines entitlement state, token ledger entry construction, onboarding grants, plan activation, rewarded-ad grant logic, game-token settlement, summary quotas, review token consumption, saved-game retention, and account-subscription seam names;
- `AccountMonetisationUi.scala` defines account dashboard, top-bar token indicator, plan cards, rewarded-ad status, summary quota status, settlement copy, and fairness copy;
- `PlaySearchIntegration.scala` uses `TokenSnapshot` in search admission;
- admin settings include tokens, rewarded ads, payments, and token/ad incident pause switches;
- tests cover onboarding grants, duplicate/reused-account blocking, plan policy, rewarded-ad grants and gates, game-token settlement/refunds, summary quotas, review token gates, full-game review tokens, ad-abuse blocking, non-transferability, saved-game retention, and monetisation fairness boundaries.

Deployment blockers:

- no durable entitlement repository/migration was found;
- no durable token ledger repository/migration was found;
- no EvenChess payment-provider adapter was found;
- no EvenChess-specific verified webhook/idempotency implementation was found;
- no rewarded-ad provider callback verification was found;
- no production token reservation flow tied to native Lichess game creation was proven;
- no meaningful-play settlement hook tied to native game lifecycle was proven;
- account dashboard currently uses generated onboarding state, not persisted entitlement state;
- no admin/support ledger adjustment tool was proven;
- no end-to-end browser/payment/webhook tests were found.

## 13. Persistence Requirements

Add durable stores:

- `evenchess_entitlements`;
- `evenchess_token_ledger`;
- `evenchess_subscription_events`;
- `evenchess_rewarded_ad_events`;
- `evenchess_payment_idempotency`;
- `evenchess_operator_adjustments`;
- `evenchess_saved_game_entitlements`.

Required indexes:

- account id;
- ledger entry id;
- idempotency key;
- provider event id;
- game id;
- analysis id;
- token bucket;
- created timestamp;
- admin actor id.

## 14. Testing Requirements

Backend tests:

- ledger idempotency blocks duplicate onboarding grants;
- duplicate webhook does not duplicate subscription activation;
- failed checkout does not activate a plan;
- cancellation/downgrade updates entitlement state;
- chargeback/refund path removes access according to policy;
- rewarded-ad duplicate callback does not duplicate token;
- game creation retry does not double-reserve or double-spend;
- meaningful-play settlement consumes exactly once;
- platform outage refunds exactly once;
- failed ECE full-game review refunds or no-spends according to policy;
- premium does not alter live Set Level, Used Level, Used Offset, ECR, or matchmaking.

Controller/integration tests:

- play search loads entitlement state server-side;
- token-required mode blocks when no entitlement exists;
- subscribed access allows game start without token spend;
- account page reads persisted entitlement state;
- admin token adjustment requires auth, CSRF, reason, and audit;
- payment/rewarded-ad endpoints reject unsigned callbacks.

Browser/manual tests:

- account page shows real persisted balance;
- plan upgrade hidden/disabled when provider is off;
- rewarded-ad button unavailable when provider is off or cap reached;
- token settlement copy matches actual ledger events;
- user cannot refresh or edit browser state to increase balance;
- no public page claims stronger live help for paid plans.

## 15. Patch Map and Integration Impact

Patch-map entries are required when implementation edits touch:

- account/session extension seams;
- payment provider controller routes;
- native Lichess plan/payment integration;
- game creation/token reservation seams;
- game lifecycle/meaningful-play settlement seams;
- admin/support token adjustment pages;
- top-bar/layout token indicator wiring;
- account/billing page routing.

No patch-map update is required for this Phase N requirements-only output.

## 16. Implementation Work Items

N1. Add durable entitlement and token-ledger repositories with migrations.

N2. Replace generated onboarding dashboard state with persisted entitlement lookup.

N3. Add idempotent onboarding grant flow.

N4. Add token reservation and settlement integration with search/game lifecycle.

N5. Add meaningful-play settlement hook and refund/no-spend logic.

N6. Add review token/quota integration for custom/full-game analysis jobs.

N7. Add EvenChess payment provider adapter or narrow integration with existing Lichess payment foundations.

N8. Add verified subscription webhook handling with idempotency.

N9. Add verified rewarded-ad callback handling with idempotency and abuse gates.

N10. Add admin/support ledger adjustment and troubleshooting tools.

N11. Add end-to-end backend, controller, browser, and webhook tests.

## 17. Phase N Acceptance Status

Status: conducted, not deployment-complete.

Completed foundations:

- monetisation policy model exists;
- token/reward/plan/review/saved-game domain services exist;
- account dashboard and top-bar indicator scaffolding exists;
- admin feature switches exist;
- unit tests cover the core policy rules and fairness boundary.

Remaining release blockers:

- durable entitlement and ledger storage;
- provider webhook verification and idempotency;
- real account-page entitlement lookup;
- game lifecycle token reservation/settlement;
- rewarded-ad provider callback integration;
- admin/support correction tooling;
- end-to-end payment, token, and browser tests.

## 18. Phase O Entry Criteria

Before Phase O admin/operator console can safely expose monetisation controls:

- token ledger and entitlement repositories must exist;
- admin adjustments must be ledger-backed;
- pause switches must block real grants/spends;
- provider callbacks must have idempotency/audit data operators can inspect.
