# Plan v1.6 Phase X - Legal, Policy, and Public Copy

## 1. Phase Goal

Phase X defines the public-facing rules, disclosures, terms/privacy scope, support FAQ requirements, and copy safety checklist needed before EvenChess can be launched to public users.

This phase is product/legal readiness documentation only. It is not legal advice and does not replace external legal review before paid launch, public marketing launch, or production payment processing.

## 2. Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/README_V2_REQUIREMENTS_SUITE.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `docs/requirements/APPENDIX_Z_V2_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md`
- `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`
- `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`
- `docs/requirements/planv1.6_phase_n_tokens_subscriptions_ads_entitlements.md`
- `docs/requirements/planv1.6_phase_p_bot_matchmaking_simulation_operations.md`
- `docs/requirements/planv1.6_phase_q_security_abuse_fairness_controls.md`
- `docs/requirements/planv1.6_phase_r_telemetry_audit_privacy_retention.md`

Key source requirements:

- `REQ-D-V2-040`: detailed landing page explains assisted variant, fairness model, Set Level, ECR, tokens, subscriptions, and prohibitions.
- `REQ-D-V2-041`: landing copy must not imply cheating, hidden engine use, or stronger paid live help.
- `REQ-E-V2-014`: account/token/plan pages must use deployment-ready labels and hide internal/debug keys.
- `REQ-G-V2-015`: setup/search discloses platform coaching is disclosed, capped, logged, and rated into ECR.
- `REQ-G-V2-032`: public search/status JSON exposes only deployment-safe information.
- `REQ-R-V2-021`: retention defaults are last 10 games with live ECE history and last 100 requested analyses per user.
- `REQ-R-V2-022`: privacy/storage controls avoid unnecessary raw AI/provider data.
- `REQ-S-V2-001`: rated EvenChess prohibits outside engines, humans, bots, extensions, notes, and stream chat.
- `REQ-S-V2-002`: public rules explain that only platform-delivered coaching is legal.
- `OVR-V2-014` and `OVR-V2-022`: public search hides debug/internal diagnostics by default.

## 3. Public Legal and Policy Boundary

EvenChess must be explained as a disclosed assisted chess variant, not normal chess with hidden help.

Public copy must state:

- platform coaching is part of EvenChess rules;
- platform coaching is disclosed to both sides;
- coaching is capped by each player's Set Level;
- actual use is tracked through Used Level and Used Offset;
- rated outcomes use EvenChess ECR, not normal Lichess rating;
- outside assistance remains prohibited in rated EvenChess;
- paid tiers, tokens, and ads do not create stronger live rated help than the server-authorized level allows.

Public copy must not present EvenChess as:

- legal cheating;
- a hidden engine;
- ordinary rated chess with help;
- pay-to-win live assistance;
- unrestricted Stockfish access during live rated games;
- a way to hide unaudited outside coaching.

## 4. Required Public Pages

Before public deployment, EvenChess needs the following user-facing pages or page sections.

| Page / Section | Required Content | Release Status |
| --- | --- | --- |
| EvenChess Rules | assisted-mode rules, allowed platform coaching, outside-help ban, Set/Used Level, rating effect | Required before public launch |
| Assistance and Fairness Explainer | why assistance is legal in EvenChess, how caps and audit work, what users can expect in-game | Required before public launch |
| ECR/MMR Explainer | ECR separation from normal rating, Set Level assignment, Used Offset settlement, uneven-match disclosure | Required before public launch |
| Bot Fallback Disclosure | low-pool bot fallback status, human-safe wording, no internal bot diagnostics | Required before public launch if bot fallback can run |
| Tokens and Subscriptions | what tokens affect, refund/consume rules, paid tier limits, no stronger live rated help | Required before monetisation |
| Privacy and Data Retention | live ECE history, requested analysis memory, audit logs, calibration samples, deletion/retention principles | Required before public launch |
| Support FAQ | common questions about help, cheating, levels, bots, rating, tokens, privacy, account issues | Required before public launch |
| Terms / Privacy Draft | payment terms, account terms, moderation, retained analysis/audit data, bot disclosure, jurisdictional review | Required before paid/public launch |

## 5. User-Facing Definitions

Public copy should use these definitions or equivalent plain-language wording.

### Set Level

Set Level is the maximum level of platform coaching allowed for a player in a game. It is assigned by the EvenChess match contract or fixed by an allowed game setup choice.

### Used Level

Used Level is the highest coaching level the player actually uses during that game. It can increase during the game but never decreases for that game, even if the player later turns overlays off.

### Used Offset

Used Offset is the rating-equivalent assistance value tied to the player's actual Used Level. EvenChess uses it when settling ECR so stronger live help is priced into the result.

### ECR

ECR is the EvenChess rating. It is separate from normal Lichess rating and is designed for games where disclosed platform coaching is part of the rules.

### Platform Assistance

Platform assistance is the coaching EvenChess itself delivers through the server-authorized overlay, coach, proposed move, potential move, and review systems. It is allowed only because it is disclosed, capped, audited, and rated into ECR.

### Outside Assistance

Outside assistance means engines, humans, bots, browser extensions, notes, stream chat, private analysis, or any unaudited help that does not come from EvenChess's platform coaching. It remains prohibited in rated EvenChess.

### Bot Fallback

Bot fallback is an optional low-player-pool feature where EvenChess may pair a waiting player with a platform bot after a configured wait. Public copy should state only whether bot fallback is currently on or off and why it exists.

### Tokens and Subscriptions

Tokens and subscriptions may control access, volume, convenience, saved games, review, or analysis rights. They must not grant stronger live rated coaching beyond the server-authorized Set Level.

### Retained Game and Analysis Data

EvenChess may retain recent live ECE history for replay and requested analyses for review. Default rolling limits are the user's last 10 games with live ECE history and last 100 requested analyses unless a paid saved-game feature or legal/abuse retention rule applies.

## 6. Search and Game Copy Requirements

Search/setup copy must remain production-like and close to native Lichess behavior.

Required:

- the public search card shows the Preferred Set Level dropdown with `Any` plus `0` to `10`;
- `Any` means no preferred level and lets the MMR engine assign levels;
- `0` to `10` means the player is requesting that own Set Level;
- public search uses the normal/native searching indicator;
- no separate EvenChess debug/status card appears unless an explicit local/debug flag is active;
- bot fallback disclosure uses deployment-safe text such as `Bots may be implemented after long wait times while EvenChess's player pool is low. Bots are currently On/Off.`;
- before or at game start, users can see rated/casual state, time control, each player's Set Level, and the outside-help rule;
- friend challenge request cards show resulting EvenChess level settings before acceptance.

Forbidden on public search/game surfaces:

- ticket ids;
- internal pool keys;
- player ids not already public;
- request ids;
- policy versions;
- ECOR/raw offset model internals;
- match-contract source/stage diagnostics;
- bot seed diagnostics;
- token reason keys;
- provider callback names;
- Test Ground or mock ECE labels;
- raw ECE payloads.

## 7. Landing and Marketing Copy Requirements

Landing copy must state the product clearly without implying hidden or unfair help.

Required:

- EvenChess is a disclosed assisted chess mode;
- platform coaching is part of the rules;
- help is capped;
- help is logged/audited;
- ECR accounts for assistance;
- normal outside help is prohibited;
- paid tiers do not increase live rated help strength beyond the authorized level.

Forbidden positioning:

- `cheat legally`;
- `secret engine`;
- `normal chess with help`;
- `pay to win`;
- `premium gives stronger live help`;
- `best move shown live` where the live level contract does not permit that;
- `unrestricted engine access` for live rated play.

## 8. Terms, Privacy, and Retention Requirements

Terms/privacy drafts must cover:

- account data and EvenChess account state;
- ECR/MMR and rating settlement data;
- Set Level, Used Level, Used Offset, ECOR version, and policy version where retained;
- live ECE history retained for recent games;
- requested full-game/custom analyses retained under rolling limits;
- paid saved-game retention if enabled;
- token grants, consumption, refunds, subscriptions, ad reward records, and payment records;
- bot fallback disclosure and simulation/operator testing boundaries;
- audit logs for coaching renders, proposed/potential consumables, token changes, admin actions, and abuse investigation;
- ECOR calibration samples and aggregate reports;
- support/moderation data;
- deletion/export limitations where retention is needed for fraud, abuse, payment, rating integrity, or legal obligations;
- statement that ECE provider secrets, raw prompts, raw provider output, and private engine internals are not public user data surfaces.

Retention copy must not overpromise immediate deletion of data needed for payment, abuse, security, or rating integrity.

## 9. Support FAQ Required Topics

Support FAQ must answer:

- What is EvenChess?
- Is this cheating?
- What help is allowed?
- What help is not allowed?
- What are Set Level and Used Level?
- Why did my Used Level stay high after I turned a toggle off?
- What is ECR and how is it different from normal rating?
- Why did my rating change this way?
- What is bot fallback?
- How do I know if I played a bot?
- Do paid plans make me stronger in rated games?
- When are tokens consumed or refunded?
- What game/analysis data is retained?
- How do I report outside assistance or abuse?
- What happens if ECE is unavailable during a game?

## 10. Existing Foundations Observed

Existing code/docs already provide partial foundations:

- `modules/evenchess/src/main/PublicShell.scala` models public copy constraints, outside-help copy, forbidden phrase checks, and safe EvenChess navigation targets.
- `modules/evenchess/src/test/PublicShellTest.scala` asserts assistance disclosure, outside-help prohibition, and forbidden phrase checks.
- V2 requirements already define public search copy safety, bot disclosure boundaries, token copy constraints, and privacy retention limits.
- Prior Plan v1.6 phases define matching, ECR settlement, tokens, bots, telemetry, security, and retention policies that public copy must reflect.

These foundations are not enough for public launch without actual public pages, screenshots, terms/privacy drafts, and a copy scan of rendered surfaces.

## 11. Release Blockers

Phase X blocks release until:

1. EvenChess rules page exists and is reachable from public/support/account surfaces.
2. Assistance/fairness explainer exists and matches the server-authoritative game policy.
3. ECR/MMR explainer exists and matches the ECOR settlement implementation.
4. Search/game surfaces are reviewed and screenshots prove no debug card or internal diagnostics appear by default.
5. Bot fallback disclosure appears when bot fallback can run.
6. Token/subscription/payment copy is reviewed before monetisation is enabled.
7. Terms/privacy drafts cover retained game history, requested analysis memory, audit, calibration, payments, abuse, and support data.
8. Support FAQ exists.
9. Copy scan finds no forbidden hidden-engine/pay-to-win/cheating language.
10. External legal/product sign-off is recorded for public launch and paid launch.

## 12. Required Evidence

Required release evidence:

- public copy review checklist;
- rendered screenshots of landing, search, game-start disclosure, rules, pricing/tokens, account/token, and support FAQ;
- copy scan output for forbidden/debug terms;
- links to rules, fairness explainer, ECR explainer, privacy, terms, and FAQ;
- bot fallback disclosure proof for On and Off states;
- payment/token copy sign-off if monetisation is enabled;
- privacy/retention sign-off;
- legal/product approval record.

## 13. Patch Map Impact

This Phase X document is requirements/readiness documentation only and does not edit upstream Lichess implementation files.

Future implementation of rules pages, public routes, rendered landing/search copy, pricing/account copy, and support FAQ must update the patch map or integration log if upstream Lichess seams are edited.

## 14. Phase X Acceptance Status

Phase X is conducted as a readiness definition.

Current status:

- public copy requirements are defined;
- required pages and disclosures are specified;
- forbidden copy/internal-debug exposure rules are specified;
- terms/privacy and support FAQ scope is specified;
- release remains blocked until the actual public pages, copy review, terms/privacy drafts, screenshots, and sign-offs exist.

## 15. Phase Y Entry Criteria

Before entering release-candidate freeze:

1. Phase X release blockers must be resolved or explicitly marked as accepted launch blockers.
2. Any public-copy implementation must pass copy scan and screenshot review.
3. Terms/privacy/payment copy must be signed off before monetisation is enabled.
4. Patch map/integration log must be updated for any upstream public-surface changes.
5. No public page may expose ECE internals, debug diagnostics, raw token keys, bot seed diagnostics, or hidden-engine/pay-to-win wording.
