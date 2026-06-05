# EvenChess Plan v1.6 Phase J - Game Policy and Live Assistance Authority

## 1. Phase Goal

Phase J makes live EvenChess assistance authority server-owned and replayable.

The deployment goal is:

- every live EvenChess game has a persisted server-authoritative game policy before coaching can render;
- each side's Set Level is fixed at game start from the match/friend/computer policy;
- Used Level is monotonic and cannot decrease during a live game;
- Used Offset is derived from actual Used Level and assistance ledger, not from browser toggles;
- client overlay toggles are display controls only;
- toggling on a higher-level feature can raise Used Level and trigger a new ECE request, but cannot exceed Set Level;
- toggling off or lowering visible display does not lower Used Level;
- preferred starting Used Level comes from user settings but is clamped to game Set Level;
- coach text updates only when it becomes the student's turn;
- every render, hide, suppression, stale response, fallback, and level increase is auditable.

## 2. Requirements Used

Authoritative inputs:

- `AGENTS.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/planv1.6_phase_e_persistence_migrations.md`
- `docs/requirements/planv1.6_phase_g_ecl_ece_gateway_hardening.md`
- `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`
- `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

Implementation files inspected:

- `modules/evenchess/src/main/GamePolicy.scala`
- `modules/evenchess/src/main/CoachingPolicy.scala`
- `modules/evenchess/src/main/AssistanceAccounting.scala`
- `modules/evenchess/src/main/LiveCoaching.scala`
- `modules/evenchess/src/main/UserSettings.scala`
- `modules/evenchess/src/main/PlaySearchIntegration.scala`
- `app/controllers/Challenge.scala`
- `app/controllers/EvenChess.scala`
- `ui/round/src/evenchessTestGround.ts`
- `ui/round/src/interfaces.ts`
- `ui/round/src/view/evenchessOverlay.ts`
- `modules/evenchess/src/test/GamePolicyTest.scala`
- `modules/evenchess/src/test/CoachingPolicyTest.scala`
- `modules/evenchess/src/test/AssistanceAccountingTest.scala`
- `modules/evenchess/src/test/LiveCoachingTest.scala`
- `modules/evenchess/src/test/UserSettingsTest.scala`
- `ui/round/tests/evenchessOverlay.test.ts`

## 3. Server Authority Model

Server authority owns:

- whether the game is EvenChess;
- game mode;
- rated/casual/target/computer status;
- time control;
- player ids;
- each side's Set Level;
- current Used Level;
- Used Offset;
- assistance ledger;
- coaching render permission;
- ECE request level;
- ECE payload acceptance;
- proposed/potential quota and cache;
- ECR settlement input;
- audit records.

Browser/client may own only:

- local display toggles;
- collapsed/expanded UI state;
- overlay density/preferences;
- selected requested level as a request to the server;
- drawn proposed-move arrow before server validation.

Browser/client must not own:

- Set Level;
- Used Level;
- Used Offset;
- assistance accounting;
- coaching permission;
- ECE payload truth;
- rating settlement;
- token consumption;
- audit records.

## 4. Game Policy Requirements

Every EvenChess game must have a policy record before any live coaching can render.

Required game policy fields:

- game id;
- mode;
- rated flag;
- time control bucket;
- white player id;
- black player id;
- white Set Level;
- black Set Level;
- white pool key;
- black pool key;
- schema version;
- Set Level policy version;
- assistance policy version;
- ECR policy version;
- audit ledger version;
- match contract id where applicable;
- source path: matchmaking, friend challenge, computer practice, target/training;
- created timestamp;
- updated timestamp.

Invalid policy:

- missing game id;
- duplicate player ids;
- invalid Set Level;
- missing versions;
- missing feature flag values;
- policy created after coaching render.

## 5. Set Level Rules

Set Level is the maximum live assistance level permitted for a side in that game.

Rules:

- Set Level is decided before game start;
- Set Level is persisted server-side;
- Set Level is not changed by browser toggles;
- Set Level is not changed by ECE payloads;
- Set Level is not changed by subscriptions, ads, tokens, or marketing;
- client requests above Set Level are suppressed and audited;
- computer games use Set Level 10 unless a later explicit training policy overrides it;
- target/training/review modes must not mutate normal rated ECR.

## 6. Used Level Rules

Used Level records the highest live assistance level actually delivered, consumed, or revealed during the game.

Rules:

- starts from user preferred starting Used Level clamped to Set Level, or L0 if no preference applies;
- increases when a visible/rendered authorized feature at a higher level is delivered;
- increases when a higher-level proposed/potential consumable is used where policy counts it;
- may trigger an ECE request at the higher level;
- never decreases during a live game;
- lowering visible display or unticking toggles does not lower Used Level;
- hidden prefetch does not raise Used Level;
- stale/non-decision help does not raise Used Level;
- post-game review does not mutate live Used Level.

## 7. Used Offset Rules

Used Offset is derived server-side from final Used Level plus assistance load.

Rules:

- derived from assistance summary and calibration parameters;
- versioned by assistance-offset model;
- non-negative;
- capped by configured model maximum;
- used for ECR expected-score inputs;
- cannot be reduced by premium/subscription/ad/token/marketing state;
- cannot be supplied by browser;
- cannot be supplied by ECE.

## 8. Display Toggle Rules

Display toggles are local UI preferences over already-authorized data.

Rules:

- toggles may hide/show a feature family if the server has authorized and delivered that feature;
- toggles must not request/display content above Set Level without server approval;
- turning a higher-level toggle on may request authorization and, if granted, raise Used Level;
- turning a toggle off hides display but does not lower Used Level;
- level dropdown is a convenience control that sets visible toggles up to the selected level;
- feature-specific toggles remain individually controllable;
- shared features such as summary/coach text use shared toggles across levels;
- toggles must persist for the current game/display session;
- toggles must not scroll the level card to the top;
- multi-tab changes must converge on server Used Level, not local stale state.

## 9. Coach Text Turn-Gating

Coach text must be stable and turn-gated.

Rules:

- ECE payloads are requested after moves as required;
- overlays update from accepted board facts after each move;
- coach text updates only when it becomes the student's turn and authorized text exists;
- while it is the opponent's turn, retain the student's previous visible text;
- empty opponent-turn text must not clear the last relevant student-turn coach text;
- browser refresh must reconstruct the same coach text state from server-side accepted payload/history;
- proposed-move text is separate preview text and must revert to the current-board text when preview is toggled off or cleared.

## 10. Assistance Ledger Requirements

Every live assistance decision writes a schema-versioned audit event.

Audit event required fields:

- event id;
- game id;
- player id;
- ply;
- board-state key;
- feature key;
- requested level;
- Set Level;
- delivered level;
- Used Level after event;
- assistance weight delta;
- exactness class;
- UI surface;
- visibility;
- source type;
- engine job id if relevant;
- AI request id if relevant;
- policy version;
- schema version;
- created timestamp;
- outcome;
- delivered-kind summary;
- rated flag.

Events must be append-only. Replacing rated history is forbidden.

## 11. Live ECE History Requirements

Live ECE history is required for replay, review, refresh recovery, and settlement explainability.

Each per-ply entry stores:

- game id;
- ply;
- FEN;
- move UCI where known;
- position hash;
- side to move;
- white requested level;
- black requested level;
- policy version;
- ECE version;
- white output ref;
- black output ref;
- output audit ids;
- delivered levels;
- safe summary/plan refs;
- overlay atom refs;
- created timestamp.

Raw ECE output is not retained unless a retention policy explicitly allows it.

When multiple outputs exist for a ply/side, the highest delivered level is canonical for Used Level and later review.

## 12. Current Implementation State

### 12.1 Completed foundations

| Area | Current evidence | Status |
| --- | --- | --- |
| Game policy model | `GamePolicyRecord`, `GamePolicyCreateRequest`, `PlayerPolicy`, `PolicyVersions` | Partial/strong |
| Server-owned mode metadata | `GamePolicyRecord.serverMetadata`, `authorityDecision` | Partial/strong |
| Coaching render gate | `CoachingRenderGate.mayRender` requires policy repo entry | Partial |
| Set Level lookup | `PolicyService.setLevelFor` | Partial/strong |
| Client claim ignored | `ModeAuthority` and `LiveCoachingService` tests | Partial/strong |
| Set Level cap | `PolicyEngine.decide` suppresses requested level above Set Level | Partial/strong |
| Used Level monotonicity | `UsedLevelState.afterEvent` and tests | Partial/strong |
| Hidden/stale/post-game exclusions | `AssistanceLoadFormula`, `UsedLevelState`, tests | Partial/strong |
| Used Offset derivation | `UsedOffset.fromSummary` | Partial/strong |
| Audit event model | `CoachingPolicy.AuditEvent` | Partial/strong |
| Live service model | `LiveCoachingService.process` | Partial |
| ECE history model | `LiveEceHistoryEntry`, `LiveEceHistoryRecord` | Partial |
| Preferred starting level | `UserSettings.startingUsedLevelFor` tests | Partial |
| Round UI display state | `EvenChessDisplayState.setLevel/usedLevel`, overlay tests | Partial |

### 12.2 Release blockers

| Blocker | Current risk | Required outcome |
| --- | --- | --- |
| Durable policy store | `GamePolicy.Runtime` uses in-memory repository | DB-backed game policy required |
| Durable assistance ledger | Audit events are modeled but not proven durable in live game path | Append-only ledger survives refresh/restart/multi-node |
| Live round hook | Model exists, but every real game creation path must persist policy before render | Matchmaking, friend, computer, and target paths all create policy |
| Used Level live persistence | Controller/UI state can still be local/session dependent | Refresh and multi-tab preserve server Used Level |
| Toggle authority wiring | UI toggles may not be fully mediated by server authorization | Higher-level toggle must call server, cap to Set Level, audit result |
| ECE refresh on Used Level raise | Model requires refresh but production event path must be proven | Raising Used Level triggers new accepted ECE request |
| Coach text turn-gate persistence | Behavior must survive move events and refresh | Text state reconstructed from accepted payload/history |
| Multi-tab race | Multiple tabs can race Used Level/quota increases | Atomic compare-and-raise or idempotent ledger semantics |
| Completion settlement bridge | Phase I settlement needs final Used Level/Offset | Completion must read assistance summaries from durable ledger |

## 13. Persistence Requirements

Production must persist:

- game policy;
- assistance ledger;
- per-player live assistance state;
- final Used Level;
- final Used Offset;
- accepted ECE payload refs;
- live ECE history;
- coach text state;
- display toggle state where intended to survive refresh;
- proposed/potential quota/caches;
- completion rating state;
- audit indexes.

Required indexes:

- `gameId`;
- `gameId + playerId`;
- `gameId + playerId + ply`;
- `eventId unique`;
- `gameId + playerId + featureKey + ply`;
- `schemaVersion + policyVersion`;
- `createdAt`.

## 14. Concurrency and Multi-Tab Rules

Server operations must be atomic or idempotent.

Required:

- Used Level raise uses max(current, requested/delivered level);
- duplicate same event id does not double-count assistance;
- proposed/potential quotas are consumed once per authorized use;
- simultaneous tabs cannot lower Used Level;
- stale lower-level payload cannot replace newer higher-level canonical state;
- display toggles can diverge per tab, but authority state cannot.

## 15. Computer, Live, Replay, and Analysis Shell Consistency

The EvenChess shell should look consistent across:

- online live game;
- friend game;
- computer practice;
- replay/history;
- analysis/review.

Authority differs by mode:

| Mode | Set Level | Used Level authority | ECR settlement |
| --- | --- | --- | --- |
| Rated live EvenChess | match contract | live assistance ledger | yes |
| Casual live EvenChess | match contract | live assistance ledger | no |
| Friend EvenChess | friend contract | live assistance ledger | depends on rated flag |
| Computer practice | fixed training policy, normally L10 | training display state | no normal ECR |
| Retained history | saved payload/history | viewing-session only | no mutation |
| Full-game analysis | requested analysis policy | review-only | no mutation |

## 16. Testing Requirements

### 16.1 Backend tests

Required:

- game policy exists before coaching render;
- every game creation path persists policy;
- Set Level cannot be exceeded;
- Used Level starts from preference clamped to Set Level;
- Used Level increases monotonically;
- toggling off/lowering visible level does not lower Used Level;
- stale/hidden/suppressed/post-game events do not increase Used Level/load;
- Used Offset derives from final summary;
- duplicate audit event is idempotent;
- multi-tab concurrent Used Level raise resolves to max level;
- completion cannot rate without assistance summary for rated games.

### 16.2 Frontend/browser tests

Required:

- browser refresh preserves Set Level and Used Level;
- toggles hide/show authorized overlays immediately;
- higher-level toggle requests server authorization and refreshes ECE payload;
- lower-level toggle does not reduce Used Level display after server state returns;
- level card does not jump to top on toggle click;
- coach text updates only on player's turn;
- opponent-turn payload does not clear previous coach text;
- computer game shows same shell and fixed L10 policy;
- board moves remain playable with overlay shell active.

### 16.3 Integration tests

Required:

- matchmaking game creates policy before redirect/render;
- friend game creates friend policy before redirect/render;
- computer game creates training policy before render;
- completed rated game reads final Used Level/Offset from ledger;
- multi-tab test cannot lower Used Level or double-consume quota.

## 17. Observability

Metrics required:

- game policy creation count;
- missing game policy render block count;
- assistance audit event count;
- Used Level raise count;
- attempted above-Set-Level suppression count;
- stale/suppressed/hidden audit count;
- current Used Level distribution;
- Used Offset distribution;
- duplicate/idempotent event count;
- multi-tab conflict count;
- render allowed/hidden/suppressed/degraded counts;
- coach text update count on player turn;
- coach text retention count on opponent turn.

Logs must include sanitized:

- game id;
- player id or safe hash;
- feature key;
- requested level;
- Set Level;
- Used Level before/after;
- outcome;
- policy version;
- audit id.

Logs must not include raw ECE payload, provider internals, raw prompts, or private analysis data.

## 18. Patch Map and Integration Log Impact

Patch-map updates are required for implementation touching:

- native game creation;
- challenge acceptance;
- round socket/message transport;
- move lifecycle hooks;
- controller routes for assistance/toggles;
- round UI payload transport;
- game completion/rating lifecycle.

This Phase J document itself is planning/requirements only and does not require patch-map updates.

## 19. Implementation Work Items

### J1 - Durable game policy store

Persist game policy records in a production store.

Acceptance:

- policy survives restart;
- all game start paths write policy;
- missing policy blocks coaching render.

### J2 - Durable assistance ledger

Persist append-only assistance audit events.

Acceptance:

- render/hide/suppress/stale/fallback events are recorded;
- event ids are unique;
- ledger can recompute Used Level/load/offset.

### J3 - Live assistance state service

Centralize live Set Level, Used Level, and toggle authorization.

Acceptance:

- server enforces Set Level;
- Used Level raises atomically and never lowers;
- client toggles are display-only unless server grants a higher-level request.

### J4 - ECE refresh on Used Level raise

Wire higher-level authorization to ECE gateway refresh.

Acceptance:

- raising Used Level sends a new ECE board request at the updated level;
- accepted payload replaces/augments current payload without refresh.

### J5 - Coach text state

Persist and reconstruct turn-gated coach text.

Acceptance:

- text updates on student's turn;
- opponent-turn empty text does not clear previous text;
- refresh reconstructs same text state.

### J6 - Multi-tab/idempotency guard

Add atomic concurrency rules for assistance state.

Acceptance:

- simultaneous higher-level toggles produce one monotonic state;
- duplicate events do not double-charge;
- stale lower state cannot overwrite newer state.

### J7 - Integration and browser proof

Add end-to-end tests.

Acceptance:

- online, friend, computer, and refresh paths prove policy and assistance state;
- board remains playable;
- Used Level cannot be lowered by UI.

## 20. Phase J Acceptance Status

| Gate | Status | Notes |
| --- | --- | --- |
| Game policy authority documented | Complete | Sections 3-5 |
| Used Level/Offset rules documented | Complete | Sections 6-7 |
| Toggle/display boundary documented | Complete | Section 8 |
| Coach text turn-gating documented | Complete | Section 9 |
| Current implementation audited | Complete | Section 12 |
| Durable game policy | Blocked pending implementation | J1 |
| Durable assistance ledger | Blocked pending implementation | J2 |
| Live assistance state service | Blocked pending implementation | J3 |
| ECE refresh on Used Level raise | Blocked pending implementation/test | J4 |
| Multi-tab/idempotency proof | Blocked pending implementation/test | J6 |
| Browser refresh proof | Blocked pending browser/integration tests | J7 |

## 21. Phase K Entry Criteria

Before Phase K board overlay and UI polish can be deployable:

1. Every EvenChess game creation path persists game policy.
2. Coaching render is impossible without server policy.
3. Used Level is durable and monotonic.
4. Assistance ledger is durable and append-only.
5. Display toggles cannot change authority state except through server-authorized raises.
6. Higher-level toggle raises trigger ECE refresh.
7. Coach text turn-gating is durable across refresh.
8. Browser tests prove refresh and multi-tab behavior.
