# EvenChess Plan v1.6 Phase H - Matchmaking and MMR Completion

## 1. Phase Goal

Phase H makes EvenChess search, matching, friend challenges, bot fallback, and simulation production-authoritative.

The deployment goal is:

- public search uses one Preferred Level dropdown: `Any`, `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`, `10`;
- `Any` means no preference;
- `0` to `10` fixes the requester's own preferred Set Level;
- opponent-target, both-target, strict-wait, and manual rating-window controls are not part of public pool search;
- ECL computes search windows and assigned Set Levels from the active ECOR table and base-level-by-rating table;
- ECL creates an authoritative EvenChess match contract before native game creation;
- friend challenges use an explicit friend-level contract path, not generic pool search;
- matchmaking bots and simulation bots use the same contract path as humans;
- public search UX remains native Lichess-like, with debug surfaces hidden behind debug/admin gates.

## 2. Requirements Used

Authoritative inputs:

- `AGENTS.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/planv1.6_phase_a_scope_freeze.md`
- `docs/requirements/planv1.6_phase_b_repo_hygiene.md`
- `docs/requirements/planv1.6_phase_c_architecture.md`
- `docs/requirements/planv1.6_phase_e_persistence_migrations.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

Implementation files inspected:

- `modules/evenchess/src/main/LevelBasedMatchmaking.scala`
- `modules/evenchess/src/main/PlaySearchIntegration.scala`
- `modules/evenchess/src/main/BotOperations.scala`
- `modules/evenchess/src/main/EvenChessRatingCalibration.scala`
- `app/controllers/EvenChess.scala`
- `app/controllers/Dev.scala`
- `ui/lobby/src/evenchessSetup.ts`
- `ui/lobby/src/setupCtrl.ts`
- `ui/lobby/src/view/setup/modal.ts`
- `ui/lobby/src/view/table.ts`
- `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`
- `modules/evenchess/src/test/PlaySearchIntegrationTest.scala`
- `ui/lobby/tests/evenchessSetup.test.ts`

## 3. Public Search Contract

Public pool search has exactly one EvenChess level preference control:

```text
Preferred level: Any | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10
```

Interpretation:

| Dropdown value | Meaning | Backend representation |
| --- | --- | --- |
| `Any` | no preferred own Set Level | `preferredOwnSetLevel = None` |
| `0` to `10` | requester fixes own Set Level | `preferredOwnSetLevel = Some(Level(value))` |

Removed public controls:

- opponent preferred level;
- both-level preference;
- strict wait/search until preferences are met;
- manual rating-window selection;
- old search scenario debug card.

Backend compatibility:

- stale `preferredSetLevel=any` must be normalized to no preference;
- stale `playerTargetLevel` may be accepted as a compatibility alias only while migrating old local/browser state;
- stale opponent-target and strict-search fields must be ignored server-side;
- browser input is never authoritative for windows, assignment, rating settlement, or game policy.

## 4. Search Window and Level Assignment Model

### 4.1 ECOR table

The active EvenChess Offset Ratings table, ECOR, defines assistance value by level.

Current initial policy table:

```text
L0=0, L1=5, L2=10, L3=18, L4=28, L5=45,
L6=65, L7=90, L8=120, L9=155, L10=190
```

The active table must be versioned and stored/admin-visible through the ECOR calibration/admin path.

### 4.2 Base-level-by-rating table

The lower-rated player receives a base Set Level from the active rating-to-level table when neither player fixed their level.

Current initial table:

```text
<675=L0, 675-799=L1, 800-924=L2, 925-1049=L3,
1050-1179=L4, 1180-1324=L5, 1325-1499=L6,
1500-1699=L7, 1700-1899=L8, 1900-2099=L9, >=2100=L10
```

### 4.3 No-preference search

For `Any`:

1. ECL computes a reachable rating window using the maximum ECOR spread between L0 and L10.
2. ECL searches candidates using normal Lichess-like UX and EvenChess pool constraints.
3. After a candidate is chosen, ECL assigns levels:
   - lower-rated player gets the base level from the rating-to-level table;
   - other player receives the level that minimizes effective rating gap using ECOR;
   - contract records both Set Levels, expected offsets, effective ratings, ECOR version, and policy version.

### 4.4 Preferred-own-level search

For `0` to `10`:

1. Requester's Set Level is fixed to the selected level.
2. ECL computes the candidate window using requester fixed level and opponent L0-L10 possible offsets.
3. After a candidate is chosen, opponent level is assigned to minimize effective rating gap using ECOR.
4. If both players independently fixed own levels and the effective gap cannot fit the widest configured fairness window, ECL may still create the contract but must mark `unevenMatch = true`.

## 5. Current Implementation State

### 5.1 Completed foundations

| Area | Current evidence | Status |
| --- | --- | --- |
| Two-state preference model | `MatchPreferences(preferredOwnSetLevel)` and `SearchPreferenceScenario.NormalSearch/PreferredOwnSetLevel` | Partial/strong |
| `Any` normalization | `PlayForm.fromValues` filters empty/`any`; tests cover `any` | Partial/strong |
| ECOR use | `LevelOffsetTable` delegates to `EvenChessRatingCalibration.EcorRuntime.active` | Partial/strong |
| Base rating table use | `BaseSetLevelByRatingTable.levelForRating` delegates to ECOR runtime | Partial/strong |
| Contract before game policy | `MmrEngine.contractFromTickets`; `GameStartService.persistBeforeCoaching` accepts assigned contract | Partial |
| Friend contract model | `FriendLevelContract` supports auto, set-my, set-opponent, set-both | Partial/strong |
| Bot rating/Stockfish mapping | `BotMatchProfile`, `LichessEquivalentStockfishLevel`, `StockfishAiRatingRuntime` | Partial |
| Bot fallback timing | `BotModeConfig`, timeout gate in `MatchmakingIntegrationService.evaluate` | Partial |
| Simulation controls | `BotOperations`, admin settings, `DevUi` controls | Partial |
| Tests | Scala tests cover models and several contract paths; UI test exists for setup helpers | Partial |

### 5.2 Release blockers

| Blocker | Current risk | Required outcome |
| --- | --- | --- |
| Native game creation seam | Match contracts exist, but real Lichess game creation/redirect path must be proven end-to-end | Human search creates native game with persisted EvenChess policy |
| Durable search state | Phase E found in-memory repositories and controller maps | Search tickets/contracts must survive refresh/restart/multi-node deployment |
| Friend challenge routing | Friend challenge must not fall through generic pool search | Explicit friend path carries selected levels and shows recipient summary |
| Bot fallback proof | Local tests can pass while live bot fallback fails in real search loop | Browser/user test proves timeout -> bot contract -> game redirect |
| Simulation lifecycle | Current simulation seeds tickets, but production loop must keep bots cycling like users | Sim bots requeue after games and can match humans or each other |
| Public JSON safety | Search JSON must avoid internal ticket ids/raw diagnostics/policy internals | Browser sees safe status only |
| Patch-map completeness | Lobby/search/controller seams have been edited | Every upstream/core seam must be patch-mapped |
| Old public controls | Some legacy compatibility fields remain | Public UI must show only Preferred Level dropdown |

## 6. Match Contract Requirements

Every paired EvenChess game must have a match contract before native game creation.

Required contract fields:

- request id;
- time control;
- rated/casual flag;
- white player id;
- black player id;
- white ECR;
- black ECR;
- white Set Level;
- black Set Level;
- white expected offset;
- black expected offset;
- white effective rating;
- black effective rating;
- ECOR table version;
- base-level table version;
- MMR policy version;
- search stage;
- preference scenario;
- uneven-match flag and reason if applicable;
- token/admission result;
- bot/simulation source where applicable;
- audit record id.

Contract persistence must happen before:

- native Lichess game creation;
- coaching render permission;
- ECE live payload requests;
- ECR settlement scheduling.

## 7. Native Lichess Search UX

Public users should not see a custom debug status card by default.

Required UX:

- search starts from normal lobby/search flow;
- selected EvenChess settings are shown in the search card/setup area;
- after search starts, native Lichess spinner/waiting UX is used;
- debug card/status panel is hidden unless local/debug flag is enabled;
- quick search reuses the user's latest EvenChess search settings;
- search does not expose internal ticket ids or raw contract internals.

Admin/local debug may expose additional information only behind protected settings.

## 8. Friend Challenge Requirements

Friend challenges are not generic pool search.

The challenger chooses one of:

| Mode | UI fields | Assignment |
| --- | --- | --- |
| Auto level | none | same as regular no-preference assignment |
| Set my level | `My level` L0-L10 | challenger fixed; recipient assigned through ECOR |
| Set opponent level | `Opponent level` L0-L10 | recipient fixed; challenger assigned through ECOR |
| Set both levels | both dropdowns | both fixed; uneven flag if needed |

Recipient challenge card must show:

- challenger name;
- time control;
- rated/casual state;
- challenger Set Level;
- recipient Set Level;
- uneven-match notice if flagged;
- disclosed EvenChess assisted-game status.

Acceptance:

- friend form does not submit to generic `/evenchess/play/search.json` without recipient;
- recipient acceptance persists the contract before game creation;
- rejection/cancel cleans the pending friend contract.

## 9. Bot Matchmaking Fallback

Matchmaking fallback bots are for low-population queues.

Required behavior:

- admin enables/disables rated, casual, or both;
- admin sets fallback timeout;
- bots are hidden as ordinary low-population fallback opponents with site disclosure;
- bots use the same search ticket, match contract, game policy, and ECR/ECOR path as humans;
- bot Stockfish strength is chosen from rating-equivalent Stockfish windows, not from EvenChess Set Level directly;
- if user selected `Any`, bot target rating matches the user rating window;
- if user selected preferred own level, bot target rating accounts for that fixed level and ECOR offset;
- bot games use native clocks and legal move/game lifecycle;
- bot move timing is human-like and time-control aware;
- bot fallback must not mask human-human matching failures in tests.

Public disclosure text:

```text
Bots may be implemented after long wait times while EvenChess's player pool is low. This will be removed as we grow. Bots are currently On/Off.
```

## 10. Simulation Bot Requirements

Simulation bots are admin/testing actors that simulate a user population.

Required behavior:

- admin sets enabled state;
- admin sets count;
- admin sets rated/casual/both scope;
- admin sets rating range;
- admin sets level range;
- admin sets persona/timing mode;
- changing count while running reconciles desired active tickets;
- simulation bots stagger queue entry;
- simulation bots requeue after completing games;
- simulation bots can match humans;
- simulation bots can match other simulation bots;
- games use native clocks and normal legal lifecycle;
- Stockfish strength is rating-equivalent and time-control aware;
- bot accounts/games are not disclosed as obvious "version A" test accounts to ordinary users.

Production distinction:

- matchmaking fallback bots can be deployable for low population if disclosed;
- simulation bots are admin/stress-test tooling and must not run publicly unless deliberately enabled for a controlled test.

## 11. Search JSON Safety

Public search JSON may include:

- `ok`;
- safe status label;
- native redirect URL when game is created;
- safe assigned Set Levels once a match is finalized;
- safe bot disclosure On/Off label;
- uneven-match public notice if needed.

Public search JSON must not include:

- raw ticket ids;
- internal search keys;
- ECOR internals beyond public/admin-intended table views;
- raw MMR diagnostics;
- bot seed ids;
- simulation ids;
- policy internals;
- raw candidate lists;
- abuse signals;
- private calibration details.

## 12. Persistence Requirements

Phase H depends on Phase E durability.

Durable records required:

- search ticket;
- user latest search setting;
- match preference;
- match contract;
- contract audit record;
- bot fallback seed/candidate state;
- simulation runtime/config snapshot;
- friend pending contract;
- matched-game redirect pointer;
- game policy record created from contract;
- cleanup/expiry tombstones.

In-memory repositories are acceptable for Test Ground only.

## 13. Testing Requirements

### 13.1 Backend tests

Required:

- `Any` search computes levels from rating/base table and ECOR;
- preferred own level fixes requester and assigns opponent;
- stale `any` omits preference;
- stale opponent target and strict-search fields are ignored;
- rating windows are derived from ECOR, not manual UI windows;
- match contract exists before game policy creation;
- uneven fixed preferences are flagged, not silently hidden;
- friend auto, set-my, set-opponent, and set-both modes assign correct levels;
- bot fallback target rating uses rating-equivalent Stockfish table;
- simulation bots use same contract path;
- public JSON serializer redacts internal ids/diagnostics.

### 13.2 UI tests

Required:

- public modal shows only Preferred Level dropdown;
- dropdown options are `Any` and L0-L10;
- selecting `Any` omits `preferredSetLevel`;
- selecting L0-L10 sends only `preferredSetLevel`;
- old opponent/strict/manual rating controls are absent;
- debug search status card hidden by default;
- debug card shown only with debug flag;
- friend modal shows friend-specific level mode controls;
- recipient card shows resulting level settings.

### 13.3 Integration/browser tests

Required:

- human-vs-human search pairs and redirects to an EvenChess game;
- preferred-level search pairs and persists assigned levels;
- friend challenge creates a friend contract and recipient sees settings;
- matchmaking bot timeout of 1 second pairs a user with a bot in test mode;
- simulation bots enabled with small count produce findable games;
- bot-vs-bot simulation contracts are observed;
- resulting game has clocks for both sides;
- resulting game has EvenChess game policy before overlays render.

## 14. Observability

Metrics required:

- active search tickets by pool;
- average wait time by pool;
- match contract count;
- failed contract count;
- human-human match count;
- human-bot fallback match count;
- simulation-human match count;
- simulation-simulation match count;
- uneven match count;
- preferred-level search count;
- `Any` search count;
- friend challenge contract count;
- stale/legacy param ignored count;
- search timeout/no-match count;
- redirect failure count;
- game policy missing before render count.

Logs must include sanitized:

- request id or safe audit id;
- pool;
- time control;
- scenario;
- assigned levels;
- ECOR version;
- match source;
- game id after creation.

Logs must not include internal candidate lists or abuse diagnostics in public responses.

## 15. Patch Map and Integration Log Impact

Patch-map updates are required for implementation touching:

- `app/controllers/EvenChess.scala`;
- `app/controllers/Setup.scala`;
- `app/controllers/Search.scala`;
- routes;
- lobby setup controller;
- lobby modal/table UI;
- friend challenge setup;
- game creation adapter;
- bot/admin settings surfaces.

This Phase H document itself is planning/requirements only and does not require patch-map updates.

## 16. Implementation Work Items

### H1 - Native game creation adapter proof

Prove contract -> native game creation -> redirect -> game policy exists.

Acceptance:

- tested human-human game starts from public search;
- contract levels persist into `GamePolicy`;
- no coaching render before policy exists.

### H2 - Durable search/contract repository

Replace local in-memory search runtime for production.

Acceptance:

- search survives refresh/restart;
- duplicate search resumes instead of spawning inconsistent tickets;
- matched redirect survives across polling requests.

### H3 - Public search UI simplification

Enforce one public Preferred Level dropdown.

Acceptance:

- only `Any` and L0-L10 visible;
- no old opponent/strict/manual window controls;
- quick search reuses latest settings.

### H4 - Friend challenge contract path

Implement or complete friend-specific EvenChess challenge routing.

Acceptance:

- recipient is carried through request;
- recipient card displays resulting settings;
- acceptance creates game with friend contract.

### H5 - Bot fallback end-to-end

Verify and harden matchmaking fallback bots.

Acceptance:

- timeout can be set to 1 second in local/admin mode;
- user search pairs with bot;
- bot Stockfish strength uses rating-equivalent table;
- clocks and move timing work.

### H6 - Simulation bot lifecycle

Make simulation bots behave like a population loop.

Acceptance:

- start/update/stop count works;
- bots stagger queue entry;
- bots requeue after games;
- humans can find sim bots;
- sim bots can play each other.

### H7 - Public JSON redaction

Audit and harden search/status serializers.

Acceptance:

- no raw ticket ids/search keys in public JSON except opaque user-owned polling tokens if required;
- no raw diagnostics or candidate internals.

### H8 - Patch map and integration log

Update patch map for all touched upstream seams.

Acceptance:

- each Lichess core/lobby/search/friend/game seam is recorded;
- integration log explains why the seam exists and how to rebase it.

## 17. Phase H Acceptance Status

| Gate | Status | Notes |
| --- | --- | --- |
| Two-state search model documented | Complete | Section 3 |
| ECOR/base-level assignment documented | Complete | Section 4 |
| Current implementation audited | Complete | Section 5 |
| Friend challenge requirements documented | Complete | Section 8 |
| Bot fallback requirements documented | Complete | Section 9 |
| Simulation bot requirements documented | Complete | Section 10 |
| Native Lichess game creation proven | Blocked pending implementation/test | H1 |
| Durable search/contract state | Blocked pending implementation | H2 / Phase E |
| Public JSON fully redacted | Blocked pending audit | H7 |
| Bot fallback browser-tested | Blocked pending test | H5 |
| Simulation lifecycle browser-tested | Blocked pending test | H6 |
| Patch map complete | Blocked pending implementation seam review | H8 |

## 18. Phase I Entry Criteria

Before Phase I ECR settlement can be considered deployable:

1. Every rated EvenChess game must have a persisted match contract.
2. Every contract must include Set Levels, expected offsets, ECOR version, and policy version.
3. Game policy must exist before coaching render.
4. Search and friend paths must not bypass the contract path.
5. Bot and simulation games must use the same contract path.
6. Search JSON must be public-safe.
7. Matchmaking integration tests must prove human-human, human-bot, and simulation matching.
