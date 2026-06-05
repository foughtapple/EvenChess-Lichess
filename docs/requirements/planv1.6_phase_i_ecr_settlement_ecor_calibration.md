# EvenChess Plan v1.6 Phase I - ECR Settlement and ECOR Calibration

## 1. Phase Goal

Phase I makes EvenChess Rating, ECR, fair, auditable, isolated from normal Lichess rating, and calibratable through the EvenChess Offset Ratings table, ECOR.

The deployment goal is:

- normal Lichess ratings are never changed by EvenChess games;
- only rated EvenChess games update normal EvenChess ECR;
- settlement expected-score inputs use each player's actual final Used Level and Used Offset;
- ECOR table and rating-to-level table are versioned snapshots;
- every rated settlement records policy/table versions and uneven-match flags;
- calibration collects the latest 1,000,000 qualifying game samples;
- admin can run calibration, inspect recommendations, compare current and calculated values, edit tables, and revert snapshots;
- calibration recommendations never auto-apply to production tables without admin action.

## 2. Requirements Used

Authoritative inputs:

- `AGENTS.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/planv1.6_phase_e_persistence_migrations.md`
- `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`

Implementation files inspected:

- `modules/evenchess/src/main/EcrRating.scala`
- `modules/evenchess/src/main/EvenChessRatingCalibration.scala`
- `modules/evenchess/src/main/AssistanceAccounting.scala`
- `modules/evenchess/src/main/LevelBasedMatchmaking.scala`
- `modules/evenchess/src/main/AdminBackendSettings.scala`
- `modules/evenchess/src/main/AdminOpsDashboard.scala`
- `modules/evenchess/src/test/EcrRatingTest.scala`
- `modules/evenchess/src/test/EvenChessRatingCalibrationTest.scala`
- `modules/evenchess/src/test/LevelBasedMatchmakingTest.scala`

## 3. Rating Isolation Rules

EvenChess rating state is separate from normal Lichess rating state.

Allowed:

```text
Rated EvenChess game -> ECR update
Casual EvenChess game -> no normal ECR update
Target/training game -> no normal ECR update
Computer practice -> no normal ECR update
Full-game review -> no normal ECR update
Simulation-only/admin test game -> no public rated ECR update unless deliberately marked as rated production traffic
Normal Lichess game -> normal Lichess rating path only
```

Forbidden:

```text
EvenChess game -> normal Lichess rating update
Review/analysis output -> settlement mutation
Browser/client -> rating settlement authority
Subscription/token/ads -> rating advantage or reduced offset
ECE -> game result or rating mutation
```

## 4. Settlement Formula

Rated ECR settlement uses effective rating inputs.

```text
whiteEffectiveRating = whiteEcr + whiteFinalUsedOffset
blackEffectiveRating = blackEcr + blackFinalUsedOffset
expectedScore = expectedScore(whiteEffectiveRating, blackEffectiveRating)
ratingDelta = ratingModel(score - expectedScore)
```

Interpretation:

- higher Used Level normally means higher Used Offset;
- higher Used Offset increases the player's effective rating input;
- a player using stronger assistance is expected to score better;
- therefore that player gains less for winning and loses more for losing than the same result with lower assistance.

Settlement must use final actual Used Offset, not starting Set Level and not preferred level.

## 5. Required Settlement Inputs

Every rated ECR settlement requires:

- game id;
- pool;
- rated mode;
- result;
- termination;
- white user id;
- black user id;
- white starting ECR;
- black starting ECR;
- white Set Level;
- black Set Level;
- white final Used Level;
- black final Used Level;
- white final Used Offset;
- black final Used Offset;
- white assistance audit refs;
- black assistance audit refs;
- match contract id;
- uneven-match flag and reason;
- ECOR table version;
- assistance-offset model version;
- rating model version;
- matchmaking policy version;
- game policy version;
- settlement timestamp.

Settlement is invalid if any required server-authoritative record is missing.

## 6. Current Implementation State

### 6.1 Completed foundations

| Area | Current evidence | Status |
| --- | --- | --- |
| ECR separated from normal rating | `EcrPool`, `PoolPolicy`, `EcrRecord.isNormalChessElo = false` | Partial/strong |
| Eligibility model | `EcrUpdateEligibility.updatesNormalEcr` only true for `NormalRatedEvenChess` | Partial/strong |
| Effective rating formula | `EcrGameResult.expectedScore` uses `EffectiveRating(player.rating, playerUsedOffset)` | Partial/strong |
| Simple rating replay | `RatingReplay.simpleDelta` and `applySimpleUpdate` | Partial |
| ECOR adjacent-gap table | `EcorLevelGap`, `EcorTableConfig`, default gaps | Partial/strong |
| Rating-to-level table | `RatingLevelBand`, default bands | Partial/strong |
| Runtime active ECOR | `EcorRuntime.active` / `activate` | Partial |
| Snapshot parsing/history | `EcorSnapshot`, `EcorHistory` | Partial |
| Calibration samples | `GameCalibrationSample`, `GameHistory` | Partial |
| Calibration recommendations | `CalibrationEngine.run`, residual stats, gap estimates | Partial/strong |
| Admin state | `AdminBackendSettings.EcorControls`, `EcorAdminState`, dashboard rows | Partial |
| Tests | `EcrRatingTest`, `EvenChessRatingCalibrationTest` | Partial |

### 6.2 Release blockers

| Blocker | Current risk | Required outcome |
| --- | --- | --- |
| Real game-result hook | Settlement model exists, but production hook from completed Lichess game to ECR update must be proven | Rated EvenChess completion updates ECR once and only once |
| Durable ECR records | Current model/tests do not prove DB-backed ECR persistence | ECR records persist across restart/multi-node |
| Durable ECOR table | `EcorRuntime` is volatile in-memory | Active ECOR table must persist and load on startup |
| Durable calibration history | `GameHistory` is in-memory | Latest 1,000,000 qualifying samples must be durable |
| Snapshot apply/revert | Snapshot parser exists, but production admin apply/revert path must be proven | Admin can save, activate, and revert snapshots |
| Settlement idempotency | No completed-game idempotency proof found | Duplicate result events cannot double-update ECR |
| Uneven-match settlement | Match contract can flag uneven, but settlement path must record/use it | Uneven flag stored with settlement/audit |
| Simulation exclusion | Simulation bots can create tickets; settlement exclusion policy must be enforced | Simulation-only games cannot pollute public rated ECR |
| Calibration governance | Recommendations exist, but no production safety workflow proven | Recommendations remain advisory until admin applies |

## 7. ECOR Table Requirements

ECOR table is the deployed assistance offset policy.

Required adjacent-gap representation:

```text
L0-L1=5
L1-L2=5
L2-L3=8
L3-L4=10
L4-L5=17
L5-L6=20
L6-L7=25
L7-L8=30
L8-L9=35
L9-L10=35
```

This produces cumulative offsets:

```text
L0=0, L1=5, L2=10, L3=18, L4=28, L5=45,
L6=65, L7=90, L8=120, L9=155, L10=190
```

Rules:

- exactly ten adjacent gaps;
- gaps must be non-negative;
- cumulative offset for L0 is zero;
- values must be versioned;
- changes require admin action and snapshot creation;
- old games must remain explainable under their original ECOR version.

## 8. Base-Level-by-Rating Table Requirements

The base-level-by-rating table assigns the lower-rated player's Set Level during no-preference matchmaking.

Initial table:

```text
<=674=L0
675-799=L1
800-924=L2
925-1049=L3
1050-1179=L4
1180-1324=L5
1325-1499=L6
1500-1699=L7
1700-1899=L8
1900-2099=L9
>=2100=L10
```

Rules:

- exactly one band per L0-L10;
- bands must be non-overlapping;
- bands must be admin-visible;
- changes require snapshot history;
- matchmaking and settlement logs must record the table version used.

## 9. Calibration Sample Collection

ECL must collect the latest 1,000,000 qualifying samples.

Each sample records the five core variables:

- side A rating;
- side B rating;
- side A level;
- side B level;
- result.

Production sample should also record:

- game id;
- played timestamp;
- time control/pool;
- ECOR version used;
- settlement version used;
- uneven-match flag;
- bot/simulation/human source;
- provisional status;
- abuse/disqualification exclusion flags.

Qualifying samples:

- completed rated EvenChess games;
- valid result;
- both players have valid ECR records;
- both final Used Levels/Offsets known;
- no disqualifying abuse/admin void flag;
- not simulation-only unless admin explicitly includes test data in a separate calibration run.

Non-qualifying samples:

- casual games;
- computer games;
- target/training games;
- full-game review-only events;
- aborted/no-result games;
- games missing final Used Level/Offset;
- games with unresolved cheating/abuse flags;
- local Test Ground samples unless imported into a test-only calibration dataset.

## 10. Calibration Engine Requirements

The calibration engine must:

- aggregate sample results;
- estimate adjacent ECOR gap values;
- report current value vs calculated value for each gap;
- report support sample count for each gap;
- report residual mean;
- report residual standard deviation;
- report mean absolute residual;
- report model version;
- report generated timestamp;
- leave production table unchanged until admin applies a snapshot.

Current algorithm foundations:

- bucket by rating difference and level pair;
- estimate target offset from observed score via Elo transform;
- solve weighted least squares with ridge toward current table;
- clamp gap recommendations to safe bounds;
- compute residual stats.

Production requirement:

- calibration output is advisory;
- admin must review and apply;
- every apply creates snapshot history;
- revert restores a prior full ECOR/base-level table snapshot.

## 11. Admin Controls

Admin area must expose:

- active ECOR policy version;
- active adjacent ECOR gaps;
- active cumulative level offsets;
- active base-level-by-rating table;
- active Stockfish-equivalent rating table if bot settings share the calibration page;
- snapshot history;
- latest calibration run summary;
- sample count;
- informative sample count;
- residual metrics;
- calculated recommended gap table;
- delta between current and calculated gaps;
- support sample count per gap;
- actions: save draft, validate, activate, run calibration, revert snapshot.

Admin controls must not expose:

- raw user identity lists;
- private abuse diagnostics;
- private ECE payloads;
- provider internals;
- raw prompts or engine output.

## 12. Settlement Audit Requirements

Every ECR update must write an audit row with:

- settlement id;
- game id;
- player id;
- opponent id;
- pool;
- score;
- old rating;
- new rating;
- delta;
- expected score;
- player Used Level;
- opponent Used Level;
- player Used Offset;
- opponent Used Offset;
- ECOR version;
- rating model version;
- assistance model version;
- match contract id;
- uneven-match flag;
- audit event ids;
- created timestamp.

Audit rows must be append-only or otherwise tamper-evident.

## 13. Idempotency and Safety

Settlement must be idempotent.

Required rules:

- one settlement per rated game/player;
- duplicate result event returns existing settlement;
- voided game creates a correction/reversal record, not silent mutation;
- ECOR table changes never retroactively mutate old settlements;
- recalibration can replay history for analysis but cannot automatically rewrite ratings;
- simulation/test data cannot enter production public ECR without explicit admin classification.

## 14. Testing Requirements

### 14.1 Rating tests

Required:

- higher Used Offset increases expected score;
- higher-assistance winner gains less than low-assistance winner;
- higher-assistance loser loses more than low-assistance loser;
- equal offsets behave like normal Elo expected score;
- only `NormalRatedEvenChess` updates ECR;
- casual, target, computer, review, and simulation-only games do not update public ECR;
- settlement records ECOR version and policy version;
- duplicate settlement is idempotent.

### 14.2 Calibration tests

Required:

- ECOR parser accepts valid ten-gap table;
- ECOR parser rejects missing/negative/out-of-order gaps;
- rating band parser accepts one L0-L10 band per level;
- GameHistory caps at 1,000,000 samples;
- sample deduplication by game id works;
- calibration run reports sample count, informative count, residual mean, residual SD, MAE, support samples, and calculated gaps;
- calibration recommendations do not activate runtime table automatically.

### 14.3 Admin tests

Required:

- admin can view current ECOR table;
- admin can edit table text and validate;
- admin can activate new table;
- activation appends snapshot;
- admin can revert snapshot;
- invalid table cannot be activated;
- non-admin cannot access ECOR controls;
- public pages cannot see raw calibration internals.

## 15. Observability

Metrics required:

- rated EvenChess settlement count;
- settlement failure count;
- settlement idempotent replay count;
- ECR delta distribution;
- Used Level distribution;
- Used Offset distribution;
- uneven-match settlement count;
- calibration sample count;
- calibration run count;
- calibration residual mean;
- calibration residual standard deviation;
- calibration MAE;
- ECOR activation count;
- ECOR revert count;
- invalid ECOR activation attempt count.

Logs must include sanitized:

- game id;
- pool;
- settlement id;
- ECOR version;
- rating model version;
- old/new ratings;
- delta;
- uneven flag.

Logs must not include private ECE payloads, raw engine output, provider internals, or private abuse diagnostics.

## 16. Patch Map and Integration Log Impact

Patch-map updates are required for implementation touching:

- Lichess game result lifecycle;
- Lichess rating update paths;
- game finish/event bus hooks;
- admin settings UI;
- database migration/init files;
- any controller exposing calibration/admin routes.

This Phase I document itself is planning/requirements only and does not require patch-map updates.

## 17. Implementation Work Items

### I1 - ECR settlement service

Create a production ECR settlement service.

Acceptance:

- consumes completed EvenChess game result;
- validates game policy and match contract;
- reads final Used Level/Offset for both players;
- updates ECR once;
- writes audit rows.

### I2 - Durable ECR repository

Persist ECR records outside local tests.

Acceptance:

- records survive restart;
- pool-specific ECR is isolated;
- normal Lichess ratings remain untouched.

### I3 - Durable ECOR config

Persist active ECOR/base-level tables.

Acceptance:

- active table loads on startup;
- admin activation updates active version;
- old version remains in snapshot history.

### I4 - Calibration sample store

Persist latest 1,000,000 qualifying samples.

Acceptance:

- sample insertion dedupes by game id;
- retention caps at 1,000,000;
- excluded modes do not enter production calibration set.

### I5 - Admin calibration workflow

Complete admin controls for run/apply/revert.

Acceptance:

- admin can run calibration;
- admin sees calculated vs current values and residual metrics;
- admin can activate or revert snapshots;
- calibration never auto-applies.

### I6 - Settlement idempotency

Add settlement idempotency protection.

Acceptance:

- duplicate completion event cannot double-update rating;
- correction/reversal path is explicit.

### I7 - Tests and release evidence

Add tests and release evidence for settlement/calibration.

Acceptance:

- rating tests pass;
- calibration tests pass;
- admin tests pass;
- integration test proves completed rated game updates ECR and normal Lichess rating remains unchanged.

## 18. Phase I Acceptance Status

| Gate | Status | Notes |
| --- | --- | --- |
| ECR isolation documented | Complete | Sections 3-4 |
| Settlement input requirements documented | Complete | Section 5 |
| Current implementation audited | Complete | Section 6 |
| ECOR/base-level table requirements documented | Complete | Sections 7-8 |
| Calibration sample requirements documented | Complete | Section 9 |
| Admin calibration requirements documented | Complete | Section 11 |
| Real game-result settlement hook | Blocked pending implementation | I1 |
| Durable ECR records | Blocked pending implementation | I2 |
| Durable ECOR/history storage | Blocked pending implementation | I3-I4 |
| Admin apply/revert workflow proven | Blocked pending implementation/test | I5 |
| Settlement idempotency proven | Blocked pending implementation/test | I6 |
| Integration proof normal Lichess rating untouched | Blocked pending implementation/test | I7 |

## 19. Phase J Entry Criteria

Before Phase J game policy/live assistance authority can be deployable:

1. Every rated EvenChess game has a persisted match contract.
2. Settlement service can read final Used Level and Used Offset.
3. ECR records are durable and isolated.
4. Active ECOR table is durable and versioned.
5. Settlement writes audit records.
6. Calibration samples are collected durably.
7. Admin ECOR controls can view/edit/activate/revert.
8. Tests prove EvenChess does not update normal Lichess rating.
