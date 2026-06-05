# EvenChess Plan v1.6 Phase G - ECL-to-ECE Gateway Hardening

## 1. Phase Goal

Phase G defines the production hardening work required for the EvenChess-Lichess to EvenChessEngine gateway.

The public deployment goal is:

- ECL calls ECE server-to-server only;
- every ECE call is authorized by ECL game state, Set Level, Used Level, side, ply, and policy;
- accepted ECE payloads update live overlays without browser refresh;
- stale, slow, malformed, or unavailable ECE responses never break legal chess play;
- quick and deep payloads are merged safely;
- proposed-move and potential-move views remain server-authorized, quota-bound, and non-exploitable;
- ECE private URLs, provider paths, secrets, raw prompts, and raw provider output never reach browser JSON.

This phase does not move ECE logic into ECL. ECE remains the private engine/composer service.

## 2. Requirements Used

Authoritative inputs:

- `AGENTS.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/planv1.6_phase_a_scope_freeze.md`
- `docs/requirements/planv1.6_phase_c_architecture.md`
- `docs/requirements/planv1.6_phase_d_config_secrets.md`
- `docs/requirements/planv1.6_phase_e_persistence_migrations.md`
- `docs/requirements/planv1.6_phase_f_ece_deployment.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `/home/jayde/dev/lila-docker/repos/ece/docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md`

Implementation files inspected:

- `modules/evenchess/src/main/EngineGateway.scala`
- `modules/evenchess/src/main/EceLiveBridge.scala`
- `modules/evenchess/src/main/LiveBoardIntegration.scala`
- `app/controllers/EvenChess.scala`
- `modules/evenchess/src/test/EngineGatewayTest.scala`
- `modules/evenchess/src/test/EceLiveBridgeTest.scala`

## 3. Non-Negotiable Boundary

ECE is private.

Allowed:

```text
Browser -> ECL same-origin endpoints
ECL backend -> ECE private service
ECE -> internal providers
ECL backend -> browser-safe approved display payload
```

Forbidden:

```text
Browser -> ECE board/proposed/review endpoints
Browser -> ECE provider settings or provider internals in production
ECL repo -> ECE provider code, model weights, tablebases, API keys, hidden prompts, raw engine output
ECE payload -> public position, shared_calculations, raw_provider_output, filesystem paths, API keys
```

Local Test Ground may open local operator diagnostics, but production ECL must not expose ECE analysis endpoints.

## 4. Current Gateway State

### 4.1 Existing positive coverage

Current code already has important foundations:

| Area | Current evidence | Status |
| --- | --- | --- |
| Central ECE contract models | `EngineGateway.EceBoardStateRequest`, `EceBoardDeepRequest`, `EceProposedMoveRequest`, `EceGameReviewRequest` | Partial |
| Split quick/deep endpoints | `EceServiceConfig.boardQuickPath`, `boardDeepPath`; controller calls quick then optional deep | Partial |
| Server-side endpoint policy | `EngineGateway.EceEndpointPolicy.browserMayCallEceDirectly = false` | Partial |
| Request IDs | deterministic IDs include game id, ply, phase | Partial |
| Deep stale validation | deep response checks request id, quick request id, FEN, levels, diagnostics, forbidden fields | Partial |
| Board stale validation | `EceLiveBridge.compileBoardOverlay` clears stale/mismatched output | Partial |
| Proposed nested output | controller reads `proposed_move_evaluation.after_move_side_output` when legal | Partial |
| Non-fatal failures | controller returns error JSON with `nonFatalToGameLifecycle = true` | Partial |
| Tests | `EngineGatewayTest`, `EceLiveBridgeTest` cover contract invariants and some stale behavior | Partial |

### 4.2 Current release blockers

| Blocker | Current behavior | Required production behavior |
| --- | --- | --- |
| Production ECE URL | `EceServiceConfig.validLocalBaseUrl` allows local hosts only | Backend-only production config must allow private service names such as `http://ece:8787` while still blocking browser exposure |
| ECE URL leakage in JSON | error/success JSON includes `boardQuickUrl`, `boardDeepUrl`, `proposedMoveUrl` | Browser JSON must never include private ECE base URLs in production |
| HTTP call model | controller performs synchronous `java.net.http.HttpClient.send` | Gateway should use bounded async execution or isolated blocking pool with backpressure |
| Circuit breaker | no durable circuit breaker state found | ECE outage must trip degraded mode and avoid request storms |
| Retry policy | no explicit retry policy found | Carefully retry transient connect/read failures only where safe; never duplicate token-consuming operations |
| Backpressure | no queue/concurrency limit at ECL gateway found | Cap quick/deep/proposed concurrency per node, per game, and globally |
| Payload persistence | Phase E found controller mutable maps and payload state are not durable | Accepted payload refs, stale rejections, proposed/potential caches, and quota state must be durable |
| Browser refresh state | some assistance state remains controller/browser-memory dependent | Refresh must not restore spent consumables or lose accepted live payload history |
| Move-trigger verification | visual evidence has previously failed after moves until refresh | Browser/integration tests must verify move -> ECE -> render without refresh |
| Deep eval source | eval must update only from accepted deep payload | Quick placeholder eval must never replace last accepted deep eval |

## 5. Required Gateway Architecture

Production ECL must isolate all ECE traffic behind a backend gateway service layer.

Required service shape:

```text
EvenChess round/search/controller code
  -> EvenChess ECE Gateway
    -> authorization policy
    -> request builder
    -> dedupe/cache lookup
    -> bounded queue/backpressure
    -> HTTP client with timeout/retry/circuit breaker
    -> response parser
    -> stale response validator
    -> safe display payload compiler
    -> accepted payload/audit persistence
  -> browser-safe ECL response
```

The gateway must be the only production code path that can call:

- `POST /v1/ece/board/quick`
- `POST /v1/ece/board/deep`
- `POST /v1/ece/proposed-move`
- `POST /v1/ece/game-review`
- `GET /health`
- `GET /ready`

## 6. Request Authority

Every ECE request must be created by server-side ECL state.

Required fields:

| Field | Source of truth |
| --- | --- |
| `request_id` | ECL gateway |
| `game_id` | ECL game/round state |
| `ply` | ECL game/round state |
| `input_fen` | ECL legal game state |
| `side` / perspective | authenticated player and game color |
| `white_level` / `black_level` | authoritative EvenChess match/game policy |
| Set Level | authoritative EvenChess match/game policy |
| Used Level | authoritative assistance accounting |
| ECR/rating context | ECL EvenChess rating state |
| `use_ai` | ECL feature flag, entitlement, phase gate, and level policy |
| `custom.instructions` | server-approved numeric composer profile only |
| requested deep modules | ECL gateway policy, not browser |
| policy version | ECL deployment policy |

Browser requests may ask ECL for an approved view, but browser requests must not decide:

- levels;
- request IDs;
- ECE endpoint;
- ECE base URL;
- deep modules;
- AI provider use;
- token/quota consumption;
- accepted payload identity.

## 7. Quick and Deep Payload Handling

### 7.1 Quick request

Quick payload rules:

- sent after every accepted move where EvenChess display is enabled;
- must be non-blocking to legal move play;
- may update deterministic overlays when accepted;
- may update coach text only according to turn-gating rules;
- must not update eval bar/status from placeholder eval values;
- must include enough context to request deep when ECE reports deep is ready.

### 7.2 Deep request

Deep payload rules:

- requested only when level/policy requires it;
- must use quick context id and quick request id;
- must validate request id, quick request id, FEN, levels, diagnostics, and forbidden public fields;
- may merge into accepted quick payload;
- is the only source allowed to update Stockfish eval display;
- failure must leave the last accepted quick payload and last accepted deep eval intact.

### 7.3 Merge policy

Accepted deep data may add/replace:

- eval bar/status;
- provider-backed potential moves;
- provider-backed deterministic overlays;
- AI text if enabled and side-to-move policy allows.

Deep data must not overwrite a newer quick payload for a later ply.

## 8. Stale Response Rejection

Any ECE response is stale if one or more of these fail:

- response request id matches outstanding request;
- response FEN matches current board FEN for board-state output;
- deep response quick request id matches the accepted quick request;
- levels match server-authorized levels;
- side output matches requested perspective;
- diagnostics status is displayable;
- response is for the current game and ply;
- response is not older than the currently accepted payload for that game/side/ply;
- proposed-move response still matches the current drawn move and current FEN.

Stale response behavior:

- reject display;
- preserve existing overlay if it is still valid for current board state;
- preserve player-turn coach text when it is still the player's waiting/opponent-turn text state;
- record stale rejection metric/audit;
- do not consume additional quota for stale duplicate responses.

## 9. Coach Text Turn-Gating

Text behavior must be stable:

- overlays update from accepted board facts after each move;
- coach text updates only when it becomes that player's turn and the payload contains authorized text;
- while it is the opponent's turn, keep the player's previous text instead of clearing or replacing it with empty text;
- ECE text must be side-specific and level-gated;
- browser refresh must reconstruct the same visible text state from server-side accepted payload/history.

## 10. Overlay Update Behavior

The browser should not clear the overlay while waiting for ECE.

Required behavior:

1. Keep current accepted overlay on screen.
2. Send ECL same-origin request or receive ECL round event for new ply.
3. ECL calls ECE server-to-server.
4. If accepted, send an overlay delta or full replacement with same board key/ply identity.
5. Browser updates only changed visuals where feasible.
6. If ECE fails or times out, keep legal chess play working and display degraded state only in coach/status areas.

The UI must never need a browser refresh to show the latest accepted payload.

## 11. Proposed Move Handling

Proposed move is a server-authorized preview, not a board-state replacement.

Required state machine:

```text
current board payload
  -> user draws exactly one green legal candidate arrow
  -> user presses Proposed Move
  -> ECL validates turn, arrow count, legal move shape, quota, and cache
  -> ECL calls ECE /v1/ece/proposed-move if not cached
  -> if legal:
       cache current payload and proposed preview
       render proposed_move_evaluation.sentence/coaching
       render proposed_move_evaluation.after_move_side_output as post-move preview
     if illegal:
       preserve current board payload and overlay
  -> user presses Proposed Move again:
       toggle between current payload and cached proposed preview without consuming another quota
  -> user makes a move:
       clear proposed preview/cache for that ply
```

Acceptance requirements:

- illegal proposed move must not clear current overlay;
- legal proposed move must use `proposed_move_evaluation.after_move_side_output`;
- `after_move_side_output = 0` must not render;
- proposed preview must be marked preview/non-settlement;
- proposed preview must not be saved as the actual board-state payload;
- quota must be server-side and survive refresh.

## 12. Potential Move Handling

Potential move display is server-authorized.

Required behavior:

- ECE may send potential moves for both sides where levels allow;
- ECL must suppress potential moves until the user spends or toggles an authorized reveal;
- potential move reveal quota must be server-side and durable;
- clicking the same potential button again toggles visibility without consuming another quota while still on the same turn;
- making a move clears temporary potential display state;
- opponent potential moves must use the opponent side output, not the student's side output;
- both current-player and opponent potential move text should appear in the coach card's lower area when revealed.

## 13. Timeout, Retry, Circuit Breaker, and Backpressure

Production gateway must define these operational controls:

| Control | Required behavior |
| --- | --- |
| Connect timeout | short timeout for ECE service connection |
| Request timeout | quick shorter than deep; proposed bounded separately |
| Retry | retry only safe transient failures; do not retry token-consuming calls after ECE may have processed them unless idempotency is guaranteed |
| Circuit breaker | open after repeated failures; half-open probe via health/ready; close only after success |
| Backpressure | global, per-node, per-game, and per-user concurrency limits |
| Queue depth | bounded queue for deep/provider-heavy work |
| Degraded mode | legal game continues; overlays/text show last accepted data or clear only when stale for board |
| Metrics | latency, timeout, circuit state, queue depth, accepted/rejected payloads |

Recommended initial policy:

| Endpoint | Timeout | Retry | Notes |
| --- | ---: | ---: | --- |
| `/health` | 2s | 1 | startup/monitor only |
| `/ready` | 3s | 1 | startup/monitor only |
| `/v1/ece/board/quick` | 2-4s | 0-1 | must not block moves |
| `/v1/ece/board/deep` | 8-12s | 0 | provider-heavy; stale likely after move |
| `/v1/ece/proposed-move` | 6-10s | 0 | quota/cache sensitive |
| `/v1/ece/game-review` | async job | job retry | post-game only |

The current controller timeouts are useful local defaults but are not enough for public deployment because they are not paired with queue isolation, circuit breaker state, or backpressure.

## 14. Browser-Safe JSON Contract

Browser-safe ECL responses may include:

- `ok`;
- display cards;
- display visuals;
- public game id;
- ply;
- board-state key;
- perspective;
- level shown to user;
- sanitized diagnostics label;
- server-authorized flags;
- stale/degraded status;
- quota consumed/remaining;
- audit id or safe display payload id.

Browser-safe ECL responses must not include:

- ECE base URL;
- ECE endpoint URL;
- provider path;
- provider secrets;
- raw ECE diagnostics;
- raw provider output;
- prompt text;
- private offset tables unless intentionally public;
- internal ticket ids;
- cache keys that reveal private implementation;
- filesystem paths.

Current controller JSON includes ECE URLs in some `ece` diagnostic objects. Production implementation must remove or guard these behind local-only debug mode.

## 15. Persistence Requirements

Gateway hardening depends on Phase E persistence.

Durable records required:

- accepted quick payload pointer;
- accepted deep payload pointer;
- accepted eval source and timestamp;
- stale rejection record;
- proposed move quota use;
- proposed move cache for same turn;
- potential move quota use;
- potential move reveal cache for same turn;
- payload history for the user's last 10 games;
- full-game analysis records for the requested-analysis retention window;
- ECE outage/degraded events.

Without this persistence, browser refresh and server restart can cause exploitable quota resets or missing overlays.

## 16. Testing Requirements

### 16.1 Backend tests

Required:

- production private service URL accepted by backend config and never serialized to browser JSON;
- local debug URL still works in Test Ground;
- quick accepted payload updates overlay fields;
- deep accepted payload updates eval and deep-only fields;
- quick placeholder eval does not replace last accepted deep eval;
- deep stale FEN rejected;
- deep stale quick request id rejected;
- stale late quick payload rejected when a newer ply exists;
- ECE unavailable returns degraded/non-fatal response;
- circuit breaker opens after configured failure threshold;
- backpressure rejects or degrades excess deep calls;
- proposed illegal move preserves current overlay;
- proposed legal move uses nested `after_move_side_output`;
- proposed cache toggles without consuming quota;
- potential reveal quota survives refresh;
- opponent potential reveal uses opponent output.

### 16.2 Browser/integration tests

Required:

- start a computer game;
- make a legal move through Chessground;
- observe ECE quick/deep accepted payload render without page refresh;
- repeat with online search game;
- toggle overlay controls without scroll jump or level state reset;
- raise Used Level and verify a new ECE request at the new level;
- verify coach text updates only on player's turn;
- verify proposed legal preview toggles;
- verify proposed illegal preview leaves overlay intact;
- verify no browser network call targets ECE analysis endpoints.

### 16.3 Release scans

Required:

```text
grep -RIn "ECE_BASE_URL\|http://ece:8787\|127.0.0.1:8787\|host.docker.internal" public ui app/views
grep -RIn "provider_path\|raw_prompt\|raw_provider_output\|OPENAI_API_KEY\|STOCKFISH_PATH" public ui app/views app/controllers modules/evenchess/src/main
```

Findings in browser bundles or public JSON paths must be removed or justified as local-only Test Ground code.

## 17. Observability

Metrics required:

- ECE quick request count;
- ECE quick latency;
- ECE quick timeout count;
- ECE deep request count;
- ECE deep latency;
- ECE deep timeout count;
- ECE proposed request count;
- ECE proposed latency;
- ECE proposed timeout count;
- ECE circuit state;
- ECE queue depth;
- ECE rejected/backpressure count;
- accepted quick payload count;
- accepted deep payload count;
- stale rejected payload count;
- deep eval accepted count;
- quick eval ignored count;
- proposed legal/illegal count;
- proposed quota consumed count;
- potential quota consumed count;
- overlay render update failures after accepted payload.

Logs must include request id, game id, ply, side, phase, policy version, and sanitized result. Logs must not include provider secrets or raw provider output.

## 18. Implementation Work Items

### G1 - Gateway service extraction

Move direct ECE HTTP handling out of controller-local helpers into a dedicated backend gateway service.

Acceptance:

- controllers call gateway methods;
- gateway owns config, HTTP, parsing, stale validation, timeout policy, and metrics;
- tests can stub gateway without network.

### G2 - Production-safe config

Add backend-only ECE config that supports private service names.

Acceptance:

- `http://ece:8787` or deployment equivalent is valid for backend production;
- browser JSON never includes that URL;
- local Test Ground can still use loopback/host.docker.internal;
- invalid public/userinfo/path/query URLs are rejected.

### G3 - Safe diagnostics

Replace browser-facing ECE URL diagnostics with sanitized labels.

Acceptance:

- user sees `ece_unavailable`, `deep_not_ready`, or sanitized status;
- admin/local debug can see more only through protected/local-only diagnostics;
- no production browser JSON contains ECE base URLs.

### G4 - Async/backpressure path

Add bounded async or isolated blocking execution for ECE calls.

Acceptance:

- deep calls cannot exhaust request threads;
- per-game duplicate deep calls are deduped or rejected;
- high ECE latency degrades gracefully.

### G5 - Circuit breaker

Add gateway circuit breaker.

Acceptance:

- repeated failures open circuit;
- open circuit skips non-essential ECE calls and returns degraded state;
- half-open probe uses health/ready;
- successful probe closes circuit.

### G6 - Accepted payload state machine

Make payload acceptance monotonic per game/side/ply/phase.

Acceptance:

- older responses cannot replace newer payloads;
- quick and deep merge only for same request context;
- current overlay remains visible while waiting;
- stale rejection is recorded.

### G7 - Deep eval authority

Enforce eval updates from deep only.

Acceptance:

- quick eval placeholder ignored;
- last accepted deep eval remains until newer accepted deep eval;
- eval bar and coach eval strip use the same accepted deep eval state.

### G8 - Proposed/potential server caches

Move remaining proposed/potential quota/cache state to durable server-side stores.

Acceptance:

- refresh does not reset quota;
- same-turn toggle does not consume extra quota;
- illegal proposed move does not alter overlay;
- opponent potential button uses opponent side output.

### G9 - Browser verification

Add repeatable browser test script for move-triggered ECE render.

Acceptance:

- test creates or opens a computer game;
- makes legal move;
- observes overlay update without refresh;
- captures failure screenshot and network log.

## 19. Patch Map and Integration Log Impact

Patch-map updates are required when implementation changes touch:

- `app/controllers/EvenChess.scala`;
- route definitions;
- round/socket controller seams;
- lobby/search/controller seams;
- frontend round payload rendering;
- core Lichess game move lifecycle.

This Phase G document itself does not require a patch-map update because it is requirements/planning only.

## 20. Phase G Acceptance Status

| Gate | Status | Notes |
| --- | --- | --- |
| Gateway boundary documented | Complete | This document |
| Existing gateway state audited | Complete | Section 4 |
| Server-to-server-only rule preserved | Complete as requirement | Needs production scans |
| Quick/deep handling requirements defined | Complete | Section 7 |
| Stale rejection policy defined | Complete | Section 8 |
| Proposed/potential state requirements defined | Complete | Sections 11-12 |
| Timeout/retry/circuit/backpressure policy defined | Complete | Section 13 |
| Browser-safe JSON rule defined | Complete | Section 14 |
| Production implementation release-ready | Blocked | Requires G1-G9 implementation |
| Move-triggered no-refresh render verified | Blocked | Requires browser/integration test |
| ECE URL private-service config ready | Blocked | Carries Phase D/F blocker |
| Durable gateway state ready | Blocked | Carries Phase E blocker |

## 21. Phase H Entry Criteria

Before Phase H can be considered deployable, Phase G implementation must provide:

1. Dedicated ECL ECE gateway service.
2. Production-safe private ECE base URL config.
3. Sanitized browser JSON with no ECE URLs.
4. Bounded ECE HTTP execution with timeout/backpressure.
5. Circuit breaker and degraded state.
6. Durable accepted payload state.
7. Durable proposed/potential quota and cache state.
8. Deep-only eval state.
9. Browser test proving move -> ECE -> overlay render without refresh.
10. Updated patch map/integration log for implementation seams.
