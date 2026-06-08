# EvenChess Plan v1.6 Phase P - Bot Matchmaking and Simulation Operations

## Phase Goal

Make EvenChess platform bots usable for low-population matchmaking and controlled simulation while keeping bot behavior admin-controlled, disclosed where required, and routed through native Lichess-safe game mechanics.

Phase P is not a requirement to hide bots as humans. The deployable requirement is that bots can fill or simulate the player pool in a controlled way without weakening trust, clocks, rating fairness, or operational safety.

## Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_PATCH_MAP.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_INTEGRATION_LOG.md`
- `docs/requirements/planv1.6_phase_h_matchmaking_mmr_completion.md`
- `docs/requirements/planv1.6_phase_i_ecr_settlement_ecor_calibration.md`
- `docs/requirements/planv1.6_phase_o_admin_operator_console.md`

## Bot Authority Boundary

Bots are an EvenChess-Lichess operations feature. They must not require ECE internals and must not cause browser/client code to call ECE directly.

Server-side authority must own:

- whether fallback bots are enabled;
- whether simulation bots are enabled;
- bot scope: rated, casual, or both;
- bot population counts;
- bot rating and Stockfish strength tables;
- pairing contract creation;
- Set Level assignment;
- ECR settlement;
- simulation start, stop, resize, and monitoring;
- public disclosure state.

Browser UI may request admin actions only through protected admin routes. Browser UI must not decide whether a bot is eligible to pair.

## Supported Modes

### Matchmaking Fallback Mode

Matchmaking fallback mode is for low-population public operation. If enabled by an admin, a human search that waits longer than the configured timeout may be offered a bot candidate in the same EvenChess search pool.

Fallback mode requirements:

- admin-controlled enable/disable;
- admin-controlled rated/casual/both scope;
- admin-controlled timeout;
- public search surface discloses fallback status when enabled;
- bot candidate uses the user's requested time control;
- bot candidate is generated through the EvenChess search ticket and match contract path;
- bot profile rating is selected from rating/equivalent-strength policy, not directly from EvenChess level;
- no fallback bot is seeded before the configured timeout;
- disabling fallback prevents new fallback seeding.

### Simulation Mode

Simulation mode is for staging, stress testing, and controlled pool-density testing. It may be used for production fill only after explicit operational approval and public disclosure decisions.

Simulation mode requirements:

- admin/staging controlled by default;
- configurable bot count with hard upper bound;
- configurable rated/casual/both scope;
- configurable rating range;
- configurable level range;
- configurable persona/timing profile;
- active population resizes to the admin target count;
- bots enter the same EvenChess ticket and contract path as humans;
- simulation bot tickets can pair with humans and with other simulation bot tickets;
- after a simulation bot game completes, the simulation system should eventually requeue replacement activity according to the active population behavior;
- stop action clears active simulation tickets cleanly.

## Bot Strength and Rating Policy

Bot strength must be derived from rating-equivalent tables. EvenChess level is not the bot's playing strength.

Required policy tables:

- ECOR table: EvenChess level assistance offsets.
- Base Set Level by rating table: recommended lower-player Set Level.
- Stockfish/Lichess AI equivalent-rating table: maps bot target rating to native AI/Stockfish strength.

For no-preference human search:

- choose a bot target rating near the user's ECR/effective target in the relevant pool;
- assign Set Levels through the same EvenChess level-assignment policy used for human matches;
- choose Stockfish/native AI strength from the bot equivalent-rating table.

For preferred-level human search:

- keep the user's preferred Set Level as their own Set Level target;
- account for ECOR offset when selecting a bot profile that produces a fair effective-rating target;
- choose Stockfish/native AI strength from the bot equivalent-rating table.

Admin settings must display and allow controlled editing of the bot equivalent-rating table. Changes must be auditable and revertible through the calibration/admin history model defined in Phase I and Phase O.

## Time Control and Clock Requirements

Bot games must preserve the requested time control. Both the user and bot must have normal clocks wherever native Lichess timed games would have clocks.

Deployable bot play should use native Lichess AI/bot game mechanics where feasible. EvenChess should not recreate chess move legality, clocks, adjudication, or game result authority in a parallel bot runner.

Human-like timing requirements:

- search delay may be randomized within admin-configured bounds;
- move delay may be randomized within fair and operationally transparent bounds;
- timing profiles must not break clocks or create illegal hidden advantages;
- timing behavior should be implemented through native game/bot paths where possible.

## Disclosure and Trust Requirements

When fallback bots are enabled for public matchmaking, the search surface must disclose:

> Bots may be implemented after long wait times while EvenChess's player pool is low. This will be removed as we grow. Bots are currently On.

When fallback bots are disabled, the same area may show the status as Off or omit the disclosure according to product policy, but it must not falsely imply bots are active.

Simulation bots are an operator/staging tool by default. If simulation bots are approved for production fill, the disclosure policy must be revisited before launch.

## Current Implementation State

### Existing Foundations

The current codebase has important Phase P scaffolding:

- `modules/evenchess/src/main/BotOperations.scala` defines bot simulation config, runtime state, admin-state projection, seeding, stop/clear behavior, active ticket counting, population bounds, rating/level ranges, and persona modes.
- `modules/evenchess/src/main/PlaySearchIntegration.scala` defines bot mode config, bot fallback seeding after timeout, bot scope checks, simulation candidate visibility, match-contract evaluation, and `readyForLilaGameCreationAdapter`.
- `modules/evenchess/src/main/LevelBasedMatchmaking.scala` defines `BotMatchProfile`, bot target ECR, preferred Set Level, rating-derived Stockfish level, persona timing profiles, and Stockfish/Lichess equivalent-rating lookup.
- `app/controllers/Dev.scala` exposes admin-controlled bot operations through Settings-gated dev/admin routes.
- `modules/web/src/main/ui/DevUi.scala` renders the bot operations admin panel.
- Existing tests cover bot mode config, timeout behavior, scope gating, fallback seeding, simulation candidate visibility, simulation seeding, simulation stop clearing, bot profile validity, rating-derived Stockfish level selection, and persona timing profiles.

### Not Yet Release-Proven

Phase P is not release-complete until the following are proven:

- native Lichess game creation is invoked after a valid EvenChess bot match contract;
- fallback bot-vs-human games actually start in staging with the requested time control and clocks;
- simulation bot-vs-human games start in staging with the requested time control and clocks;
- simulation bot-vs-bot games can start, play, finish, and generate expected post-game records in staging;
- bot move execution uses native Lichess AI/bot-safe mechanics rather than an unreviewed custom game runner;
- simulation bots requeue or are replaced after game completion according to configured population behavior;
- admin changes to bot config are durably audited;
- public disclosure is verified on the actual search surfaces when fallback is enabled;
- all bot-created games remain isolated from normal Lichess rating settlement and use EvenChess ECR rules where rated.

## Admin Operations Requirements

The admin/operator console must expose:

- fallback enabled/disabled;
- fallback scope;
- fallback timeout;
- current public disclosure status;
- simulation enabled/disabled;
- simulation scope;
- simulation target population;
- simulation active ticket count;
- simulation rating range;
- simulation level range;
- simulation persona/timing mode;
- potential bot-vs-bot contract count;
- last start/stop/seed action;
- last admin actor;
- last action timestamp;
- seed/reseed result summary;
- clean stop action.

Admin routes must be protected. Non-admin accounts must not access bot controls.

## Testing Requirements

Phase P implementation must have tests for:

- fallback disabled means no fallback bot is seeded;
- fallback enabled but before timeout means no fallback bot is seeded;
- fallback enabled after timeout seeds a bot candidate;
- fallback scope blocks incompatible rated/casual queues;
- fallback bot target rating uses the equivalent-rating table;
- preferred-level search influences bot target selection through ECOR;
- simulation seeding creates same-pool bot tickets;
- simulation resizing increases and decreases active tickets to target count;
- simulation stop clears simulation tickets but does not clear human tickets;
- simulation candidates can match humans without fallback mode;
- simulation candidates can match each other;
- generated bot games preserve time controls;
- bot games route through native game creation;
- public disclosure appears when fallback is enabled;
- admin bot config changes are audited.

## Patch Map Impact

Any future Phase P implementation that changes upstream Lichess controller, lobby, challenge, game creation, AI game, clock, rating, or round seams must update the patch map and integration log.

The Phase P documentation pass does not itself modify upstream runtime code.

## Implementation Work Items

1. Confirm the bot equivalent-rating table is the only path used to select native bot strength.
2. Confirm no direct EvenChess-level-to-Stockfish-strength mapping remains in fallback or simulation matching.
3. Wire valid bot match contracts into native Lichess game creation.
4. Verify bot games preserve requested time control and both clocks.
5. Verify fallback bot games start after the configured timeout in staging.
6. Verify simulation bots can match humans with fallback disabled.
7. Verify simulation bots can match each other and complete games.
8. Add simulation requeue/replacement after game completion.
9. Add durable audit records for bot admin config mutations and runtime actions.
10. Verify disclosure on public search surfaces.

## Phase P Acceptance Status

Phase P is conducted as a readiness and requirements pass.

Status:

- Bot domain, ticket, admin, and unit-test scaffolding exists.
- Bot equivalent-rating policy exists and is represented in code.
- Release readiness is blocked until native game creation, clocks, move execution, simulation completion/requeue, admin audit, and disclosure are proven end-to-end in staging.

## Phase Q Entry Criteria

Before Phase Q security/abuse work can be considered complete, Phase P must provide:

- a known kill switch for fallback bots;
- a known kill switch for simulation bots;
- bot pairing telemetry;
- bot game creation telemetry;
- clear public disclosure behavior;
- abuse controls preventing bot farming, rating transfer, repeated pairing abuse, and simulation leakage into production unintentionally.
