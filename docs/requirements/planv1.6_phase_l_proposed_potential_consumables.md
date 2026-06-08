# EvenChess Plan v1.6 Phase L - Proposed and Potential Move Consumables

## 1. Phase Goal

Phase L makes proposed-move and potential-move help server-authoritative, level-gated, cached, and non-exploitable.

The deployment goal is:

- proposed-move calls are consumed and cached server-side per game, side, turn, level, FEN, and proposed move;
- potential-move reveals are consumed and cached server-side per game, side, kind, level, FEN, and ply;
- browser refresh, tab duplication, or client-state mutation cannot restore spent uses;
- re-clicking a cached valid reveal toggles display without spending another use;
- illegal or invalid proposed-move attempts do not change the current board-state overlay;
- legal proposed-move attempts call ECE `/v1/ece/proposed-move`;
- legal proposed-move responses display ECE coaching text and use the nested post-move deterministic side output for temporary overlays;
- potential-move data is hidden from normal board payloads and revealed only by an authorized server endpoint;
- potential-move buttons distinguish the student's move intent from the opponent-side reveal intent;
- all consumable activity is auditable and tied to the live game policy and Used Level.

## 2. Requirements Used

Authoritative inputs:

- `AGENTS.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/planv1.6_phase_e_persistence_migrations.md`
- `docs/requirements/planv1.6_phase_g_ecl_ece_gateway_hardening.md`
- `docs/requirements/planv1.6_phase_j_game_policy_live_assistance_authority.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `/home/jayde/dev/lila-docker/repos/ece/docs/requirements/EVENCHESS_ENGINE_CONTRACT.md`

Implementation files inspected:

- `app/controllers/EvenChess.scala`
- `modules/evenchess/src/main/EceLiveBridge.scala`
- `modules/evenchess/src/main/EngineGateway.scala`
- `modules/evenchess/src/main/GamePolicy.scala`
- `modules/evenchess/src/main/LiveBoardIntegration.scala`
- `ui/round/src/evenchessTestGround.ts`
- `ui/round/src/interfaces.ts`
- `ui/round/src/view/evenchessOverlay.ts`
- `ui/round/tests/evenchessOverlay.test.ts`

## 3. Server Authority Boundary

Server authority owns:

- whether proposed/potential help is available for the game mode;
- each side's Set Level;
- each side's current Used Level;
- current per-side quota by Used Level;
- spent proposed-move calls;
- spent potential-move reveals;
- cached proposed-move results;
- cached potential-move reveal results;
- whether cached data is still valid for the current game, side, ply, FEN, and level;
- whether potential data may be returned to the browser;
- audit events for requests, cache hits, denials, stale data, ECE failures, and quota exhaustion.

Browser/client may own only:

- drawing the tentative green proposed-move arrow;
- local display toggle state;
- active/inactive display of a server-returned cached proposed preview;
- active/inactive display of a server-returned cached potential reveal;
- transient loading and error messages.

Browser/client must not own:

- consumable counters;
- quota decisions;
- Used Level increases;
- Set Level authority;
- post-refresh restoration of spent uses;
- unauthorized potential-move data;
- ECE request construction beyond sending a permitted request intent to ECL.

## 4. Quota by Used Level

Initial proposed-move quota policy:

| Used Level | Proposed Move Calls Per Game |
| --- | ---: |
| L0-L4 | 0 |
| L5 | 1 |
| L6-L7 | 2 |
| L8-L10 | 3 |

Initial opponent-potential reveal quota policy:

| Used Level | Opponent Potential Reveals Per Game |
| --- | ---: |
| L0-L4 | 0 |
| L5-L6 | 1 |
| L7 | 2 |
| L8-L10 | 3 |

Initial player-potential reveal quota policy:

| Used Level | Player Potential Reveals Per Game |
| --- | ---: |
| L0-L5 | 0 |
| L6 | 1 |
| L7 | 2 |
| L8-L10 | 3 |

Rules:

- quotas are derived from the server-authoritative Used Level;
- quota increases are allowed when Used Level rises during the game;
- quota decreases are not allowed because Used Level is monotonic;
- a cached valid reveal does not consume another use;
- a denied, illegal, invalid, stale, or failed request does not consume a use;
- if the same turn has already consumed a proposed move for a different arrow, the server must reject the second arrow for that turn;
- usage is per game and per side, not global account balance.

## 5. Proposed Move Consumable Contract

Proposed Move is a request to evaluate one legal move from the current position.

Required request conditions:

- game is an EvenChess game or authorized local test-ground game;
- requester is the side to move;
- requester Used Level permits at least one proposed-move call;
- requester has remaining quota;
- the request contains exactly one proposed move;
- the move is legal in the current game position;
- request FEN, ply, game id, requester side, and Used Level match the server's current game state;
- ECL calls ECE server-to-server through `/v1/ece/proposed-move`;
- browser never calls ECE directly.

Required legal-response handling:

- use `proposed_move_evaluation.sentence` or `proposed_move_evaluation.coaching.text` for proposed coaching;
- use `proposed_move_evaluation.new_fen` as the temporary post-move board-state key where supplied;
- use `proposed_move_evaluation.after_move_side_output` as deterministic post-move overlay data;
- do not treat `after_move_side_output` as a normal board-state response;
- preserve the current live board-state payload as the base state;
- cache the proposed result until a real move is made;
- pressing the Proposed Move button again toggles between the current board payload and cached proposed preview;
- toggling the cached preview does not spend another use.

Required illegal/invalid-response handling:

- do not replace the current board-state overlay;
- do not clear an existing legal proposed preview unless the current arrow is removed or a real move is made;
- do not consume quota;
- show a small local error/status only.

## 6. Proposed Move State Machine

States:

- `idle`: no active proposed preview;
- `loading`: one authorized proposed request is in flight for the selected arrow;
- `ready`: a server-authorized proposed preview is cached and may be active or hidden;
- `error`: latest request was denied or failed, without replacing valid current overlay state.

Transitions:

- `idle -> loading`: valid one-arrow request starts;
- `loading -> ready`: ECE returns a legal displayable response that matches game, ply, FEN, side, move, and request id;
- `loading -> error`: ECE fails, denies display, or returns stale/mismatched data;
- `ready -> idle`: user presses the button again for the same selected cached arrow;
- `idle -> ready`: user presses the button again for a cached valid arrow;
- `ready -> error`: invalid click is reported while preserving the existing active preview where the existing preview still matches the turn;
- any state -> `idle`: a real move changes ply/FEN or the selected arrow is removed.

The server cache key must include:

- game id;
- requester side;
- ply;
- FEN;
- Used Level;
- proposed move UCI;
- ECE engine/policy version where available.

The per-turn consumption key must include:

- game id;
- requester side;
- ply;
- FEN;
- Used Level.

## 7. Potential Move Reveal Contract

Potential moves are ECE-provided candidate/potential board facts, but ECL owns reveal permission.

Required request conditions:

- game is an EvenChess game or authorized local test-ground game;
- requester Used Level permits the requested reveal kind;
- requester has remaining quota for that reveal kind;
- request FEN, ply, game id, requester side, and Used Level match the server's current game state;
- `kind=player` is available only on the requester's turn;
- `kind=opponent` requests the opponent side's potential moves, not the requester's side;
- potential data is not present in normal board payloads returned to the browser;
- ECL returns potential cards/visuals only from the reveal endpoint after server authorization.

Required response handling:

- reveal payload includes `serverAuthorized=true` and `approvedDisplayPayload=true`;
- reveal payload identifies `kind`, requester side, reveal perspective, ply, board-state key, level, consumed count, and quota;
- potential arrows/overlays render only while an active reveal matches current game, ply, FEN, level, audit id, and kind;
- potential text renders at the bottom of the coach card;
- clicking the same active reveal hides it without spending another use;
- clicking a cached valid reveal shows it again without spending another use;
- reveal data clears after a real move.

## 8. Potential Move State Machine

States:

- `idle`: no active potential reveal;
- `loading`: one authorized reveal request is in flight for a kind and position;
- `ready`: a server-authorized reveal is cached and may be active or hidden;
- `error`: latest request was denied or failed without exposing unauthorized data.

Transitions:

- `idle -> loading`: valid reveal request starts;
- `loading -> ready`: server returns an authorized reveal matching game, ply, FEN, level, and kind;
- `loading -> error`: server denies, ECE has no candidate data, ECE fails, or response is stale;
- `ready -> idle`: user clicks the same active reveal again;
- `idle -> ready`: user clicks a cached valid reveal;
- any state -> `idle`: real move changes ply/FEN.

The reveal cache key must include:

- game id;
- requester side;
- reveal kind;
- ply;
- FEN;
- Used Level;
- ECE engine/policy version where available.

## 9. Current Implementation State

Foundations already present:

- `app/controllers/EvenChess.scala` exposes local test-ground board, proposed-move, and potential-move endpoints;
- ECL calls ECE server-to-server for proposed move and board/potential reveal requests;
- proposed-move parsing reads nested `proposed_move_evaluation.after_move_side_output`;
- illegal `after_move_side_output: 0` is not rendered as a post-move overlay;
- normal board payloads pass through `hidePotentialMovePayload`;
- potential reveal endpoint chooses opponent perspective for `kind=opponent`;
- proposed and potential quota helpers exist on both Scala and TypeScript sides;
- UI helpers require one green arrow and check Chessground legal destinations before proposing;
- UI helpers cache/toggle proposed and potential results;
- UI tests cover proposed quotas, potential quotas, potential reveal display, server-authorized cached potential state, opponent potential perspective, one-green-arrow proposed selection, cached proposed toggling, illegal proposed click preservation, post-move proposed overlays, and duplicate in-flight proposed request prevention.

Deployment blockers:

- proposed and potential counters/caches are currently controller-local mutable maps in `app/controllers/EvenChess.scala`;
- controller-local maps are lost on process restart, deploy, or multi-node routing and are not safe for production quota authority;
- current test-ground endpoints are open/local scaffolds and must not be treated as the production live-game API shape;
- production live-game endpoints must derive FEN, ply, requester side, Used Level, and Set Level from server game state, not from browser query parameters;
- server-side legal move validation must be proven from authoritative game state, not only from UI Chessground destinations or UCI shape checks;
- reveal cache clear must be tied to authoritative move lifecycle;
- audit logs must persist all consume/cache/deny/fail events;
- server tests must prove browser refresh cannot restore spent uses.

## 10. Persistence Requirements

Add a durable consumable store with records for:

- game id;
- side;
- consumable type: `proposed_move`, `potential_player`, `potential_opponent`;
- Used Level at consumption;
- Set Level at consumption;
- ply;
- FEN hash or board-state key;
- proposed move UCI where applicable;
- ECE request id;
- ECE engine/policy version;
- cache key;
- consumed sequence number;
- cached display payload pointer or sanitized payload;
- audit id;
- created timestamp;
- expires/retention timestamp.

Store requirements:

- atomic consume-or-cache-hit operation;
- unique constraint preventing two different proposed moves for the same side/turn/FEN/level;
- unique constraint preventing duplicate consumption for the same cached reveal;
- idempotent cache hit response;
- query by game and side for current consumed counts;
- cleanup after game retention window;
- compatible with the Phase E persistence plan and Phase J assistance ledger.

## 11. Browser and UI Requirements

Browser behavior:

- Proposed Move button is visible only when the UI shell is active, but disabled or labelled as unavailable below L5;
- potential move buttons are visible in the coach control area and show quota state;
- `Opponent Potentials` requests opponent-side candidate data;
- `My Potentials` requests requester-side candidate data and is available only on requester turn;
- proposed coaching appears in a distinct card under/near coach content;
- potential reveal text appears at the bottom of the coach card;
- proposed post-move overlay preview uses cached cards/visuals from `after_move_side_output`;
- current board overlay is restored instantly when proposed preview is toggled off;
- invalid proposed clicks do not blank overlays;
- real moves clear active proposed and potential reveal display state.

UI must not:

- expose ECE URLs;
- call ECE directly;
- calculate consumable quota authority;
- persist consumed counts as authority;
- show potential data from normal board payloads before server reveal.

## 12. Audit and Observability Requirements

Audit each:

- proposed request started;
- proposed cache hit;
- proposed quota denial;
- proposed not-turn denial;
- proposed illegal/invalid denial;
- proposed ECE failure;
- proposed stale/mismatched response;
- proposed legal response rendered;
- proposed preview toggled on/off;
- potential reveal request started;
- potential cache hit;
- potential quota denial;
- potential not-turn denial;
- potential missing-data denial;
- potential stale/mismatched response;
- potential reveal rendered;
- potential reveal toggled on/off.

Metrics:

- proposed calls attempted/accepted/denied/cache-hit by level;
- potential reveals attempted/accepted/denied/cache-hit by kind and level;
- ECE proposed latency;
- ECE board/potential reveal latency;
- stale response rate;
- quota exhaustion rate;
- illegal proposed attempt rate;
- cache hit rate.

## 13. Required Tests

Server tests:

- L4 cannot use proposed move;
- L5/L6/L8 proposed quotas are 1/2/3;
- second proposed click for same cached arrow is a cache hit and does not consume;
- second different proposed arrow in same turn is denied;
- illegal proposed move does not consume and does not replace overlay;
- browser refresh/session reset cannot reset consumed count;
- L4 cannot reveal potentials;
- L5 can reveal opponent potentials once;
- L5 cannot reveal player potentials;
- L6/L7/L8 player-potential quotas are 1/2/3;
- opponent potential reveal uses opponent perspective;
- normal board payload omits potential cards/visuals;
- reveal endpoint returns potential cards/visuals only after authorization;
- move advancement clears active reveal eligibility;
- stale FEN/ply/requester-side data is denied.

UI tests:

- one green legal arrow required;
- multiple green arrows denied without clearing valid overlay;
- illegal arrow denied without clearing valid overlay;
- proposed response with nested `after_move_side_output` renders post-move preview;
- proposed toggle restores current board overlay without another fetch;
- potential reveal text renders at bottom of coach card;
- potential reveal arrows render only after server reveal;
- opponent potential button requests opponent perspective;
- active reveals clear after move;
- displayed quota counts come from server assistance usage when supplied.

Browser/manual tests:

- start a computer game and use proposed move at L10;
- confirm legal proposed response calls ECE and shows post-move overlay;
- press Proposed Move again and confirm it toggles back without ECE request;
- draw an illegal green arrow and confirm overlay remains unchanged;
- reveal opponent potentials and confirm they are opponent-side;
- reveal player potentials on own turn and confirm they are player-side;
- refresh and confirm spent counts remain spent;
- make a real move and confirm proposed/potential active reveals clear.

## 14. Patch Map and Integration Impact

Patch-map entries are required for any implementation edits to:

- `app/controllers/EvenChess.scala`;
- round controller and round UI integration files;
- live-game route definitions;
- game move lifecycle seams;
- persisted game policy/assistance ledger seams.

No patch-map update is required for this Phase L requirements-only output.

## 15. Implementation Work Items

L1. Add durable consumable repository and schema.

L2. Replace controller-local proposed/potential maps with repository-backed atomic operations.

L3. Add production live-game endpoints that derive state from server game authority.

L4. Keep test-ground endpoints as local diagnostics only and mark them as non-production scaffolding.

L5. Add server-side legal move validation against authoritative game state.

L6. Tie proposed/potential cache validity and clear behavior to the authoritative move lifecycle.

L7. Persist audit events and expose deployment-safe admin diagnostics.

L8. Add server, UI, and browser tests listed in this phase.

## 16. Phase L Acceptance Status

Status: conducted, not deployment-complete.

Completed foundations:

- ECE proposed-move call shape is understood;
- nested `after_move_side_output` usage is represented in ECL code;
- current UI has proposed/potential cache and toggle scaffolding;
- current UI tests cover several intended interactions;
- normal board payload filtering for potential items exists in ECL controller code.

Remaining release blockers:

- consumable authority must be durable and atomic;
- production endpoints must stop trusting browser-supplied FEN/ply/level as authority;
- server-side legal move validation must be proven;
- cache invalidation must be bound to real move lifecycle;
- refresh/multi-tab/multi-node tests must pass;
- audit persistence must be added.

## 17. Phase M Entry Criteria

Before Phase M analysis-memory/review-mode work depends on proposed/potential state, Phase L needs:

- durable per-game side consumable records;
- authoritative current-board lookup from game state;
- idempotent cached reveal reads;
- reliable move-lifecycle invalidation;
- test evidence that refresh cannot restore spent uses.
