# EvenChess Plan v1.6 Phase M - Analysis Memory and Review Modes

## 1. Phase Goal

Phase M makes EvenChess replay, review, and analysis reuse retained ECE history where possible and request new analysis only when necessary.

The deployment goal is:

- every completed EvenChess game can retain sanitized per-ply live ECE history for each participating user;
- each user keeps a rolling default of the last 10 completed games with attached live ECE history;
- each user keeps a rolling default of the last 100 requested full-game/custom analyses;
- paid saved-game exceptions can extend retention without changing live rated fairness;
- replay, analysis, study, computer, and live boards use the same EvenChess overlay shell and local display toggles;
- retained payloads are displayed by ply without recalculating when valid history exists;
- missing retained payloads are not fabricated and require an explicit analysis request;
- review-mode level selections never mutate original live Used Level, Assistance Load, Used Offset, ECR, matchmaking, result, or audit settlement;
- full-game analysis calls ECE server-to-server only through `/v1/ece/game-review`;
- stored payloads never retain raw provider output, raw prompts, local provider paths, secrets, or private ECE internals.

## 2. Requirements Used

Authoritative inputs:

- `AGENTS.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/planv1.6_phase_e_persistence_migrations.md`
- `docs/requirements/planv1.6_phase_g_ecl_ece_gateway_hardening.md`
- `docs/requirements/planv1.6_phase_j_game_policy_live_assistance_authority.md`
- `docs/requirements/planv1.6_phase_l_proposed_potential_consumables.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `/home/jayde/dev/lila-docker/repos/ece/docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md`

Implementation files inspected:

- `modules/evenchess/src/main/AnalysisMemory.scala`
- `modules/evenchess/src/main/LiveCoaching.scala`
- `modules/evenchess/src/main/FeatureSurfacePolicy.scala`
- `modules/evenchess/src/main/LiveBoardIntegration.scala`
- `modules/evenchess/src/main/TelemetryAnalytics.scala`
- `modules/evenchess/src/main/AiCoachPolicy.scala`
- `modules/evenchess/src/main/OpenAiCoaching.scala`
- `modules/evenchess/src/main/AccountMonetisationUi.scala`
- `app/controllers/EvenChess.scala`
- `ui/round/src/view/evenchessOverlay.ts`
- `modules/evenchess/src/test/AnalysisMemoryTest.scala`
- `modules/evenchess/src/test/LiveCoachingTest.scala`
- `modules/evenchess/src/test/SubscriptionTokensAdsTest.scala`

## 3. Source of Truth Boundaries

ECL owns:

- game ownership and completed-game visibility;
- retention policy;
- paid saved-game exceptions;
- user-level memory indexes;
- token/quota checks for requested full-game analysis;
- review surface routing;
- overlay shell reuse;
- display toggle state;
- whether retained payload exists for a ply;
- audit records for retention, lookup, request, display, and deletion;
- ECR and live fairness immutability.

ECE owns:

- deterministic board-state generation;
- proposed-move generation;
- full-game `/v1/ece/game-review` review payload generation;
- provider orchestration;
- Stockfish, Syzygy, Maia, eval-cache, AI text compression, and private caches;
- public payload construction that excludes private internals.

ECL must not:

- copy ECE internals into the Lichess fork;
- store raw provider payloads by default;
- expose ECE review endpoints to browser code;
- treat review analysis as live assistance;
- let review displays mutate live game state or settlement.

## 4. Retained Live Game Memory

Default rule:

- keep the last 10 completed EvenChess games per user;
- retention is per user, not globally visible;
- each retained game stores sanitized live ECE history needed to replay overlays by ply;
- each retained frame is keyed by owner, game, side, ply, and FEN, like an EvenChess-specific PGN payload track;
- only actual board-state payloads are retained in match history; proposed-move preview payloads are excluded;
- if multiple payloads exist for the same game/side/ply/FEN, the highest delivered level is canonical;
- live board-state changes check the current game's sanitized FEN payload cache before calling ECE again;
- a cached same-game/same-side/same-FEN payload can be reused when its stored level is equal to or above the current Used Level;
- raising Used Level past the cached payload level must trigger a new ECE request and upgrade that FEN's cached payload;
- when an eleventh game is retained, the oldest non-exempt record is removed;
- paid saved-game exceptions may protect selected records according to entitlement policy;
- deletion/privacy requests must remove or anonymize the user's retained history according to platform policy.

Minimum retained record fields:

- owner user id;
- game id;
- opponent id where safe;
- game completed timestamp;
- mode: rated, casual, computer, friend, target/training, review;
- result and termination reason;
- White/Black Set Levels;
- final White/Black Used Levels;
- final White/Black Used Offsets;
- policy versions: game policy, ECE, ECOR, ECR, assistance ledger, overlay schema;
- move list or reference to Lichess move history;
- per-ply FEN or validated position reference;
- side to move per ply;
- sanitized side-output references or sanitized display payloads;
- summary/plan/warning text actually displayed where retained;
- overlay atom references actually displayed where retained;
- audit ids;
- raw ECE retained flag, default `false`.

Retained live history must not include:

- raw provider logs;
- raw Stockfish lines;
- raw AI prompts;
- raw AI outputs beyond approved public coaching text;
- ECE filesystem paths;
- engine binary paths;
- secrets;
- public `position` or `shared_calculations` from ECE.

## 5. Requested Full-Game Analysis Memory

Default rule:

- keep the last 100 requested full-game/custom analyses per user;
- full-game/custom analyses are keyed by game, perspective, White level, Black level, ECE version, policy version, and AI flag;
- identical valid keys should be cache hits rather than duplicate token spends;
- new requested analyses do not overwrite retained live history;
- requested analysis records may contain generated review-level payloads but cannot mutate original live-state records.

Required cache key fields:

- game id;
- White review level;
- Black review level;
- perspective: White, Black, side-to-move/Both, or custom mode;
- ECE version;
- ECE review schema version;
- ECL policy version;
- AI narrative allowed flag;
- analysis mode;
- sanitized game fingerprint or PGN/move-history hash.

Required record fields:

- owner user id;
- analysis id;
- cache key;
- requested timestamp;
- token/quota decision id;
- token/quota checked flag;
- ECE request id;
- ECE response id or output reference;
- sanitized per-ply review frames;
- review-level Set Levels;
- AI narrative call count;
- post-game-only flag;
- no-live-mutation flags.

## 6. Review Lookup Rules

When opening a completed game:

1. Determine whether the viewer owns retained live history for that game.
2. Determine whether the viewer selected an existing requested analysis.
3. If a selected requested analysis exists and matches the requested key, use it.
4. Otherwise, if retained live history exists, use retained live history.
5. Otherwise, show the EvenChess shell with no payload and require an explicit analysis request.

When stepping through moves:

- look up the frame by game id, ply, side/perspective, policy version, and retained source;
- only display frames whose game id, ply, FEN/position hash, side, delivered level, and audit id match;
- round replay/navigation must still request retained/cached EvenChess payloads instead of skipping overlay lookup only because the controller is replaying;
- keep local display toggles available;
- never fabricate missing cards or overlays;
- if the retained frame is missing, stale, or mismatched, show the shell and an analysis-required state.

## 7. Review Modes

Supported retained-history modes:

- Live White: display White's saved live ECE perception at the canonical level used for that ply;
- Live Black: display Black's saved live ECE perception at the canonical level used for that ply;
- Live Both: display the saved side-to-move perception for each reviewed position;
- Custom Review: use selected White/Black review levels and perspective, backed by requested full-game/custom analysis.

Rules:

- retained live modes consume retained history only and do not spend custom analysis tokens;
- custom review can require tokens/quota when it exceeds saved levels, uses AI, or requests expensive L10/full-game analysis;
- review Used Level/session selection is display-session state only;
- review level changes do not lower or raise the original live Used Level;
- review displays do not affect ECR settlement or ECOR calibration inputs except as separately tagged review telemetry.

## 8. Full-Game ECE Review Contract

ECL may call ECE `/v1/ece/game-review` only server-to-server.

Request conditions:

- game is completed;
- requester has permission to review the game;
- token/quota/entitlement check has passed where required;
- game input is reconstructed from authoritative Lichess game history;
- FEN/move history is bounded by input size limits;
- review level and perspective are server-authorized;
- ECE host is private backend configuration;
- browser receives only sanitized ECL review output or references.

Response handling:

- full-game ECE output is post-game review output, not live coaching transport;
- full-game ECE output must follow `docs/requirements/EVENCHESS_FULL_MATCH_PAYLOAD_CONTRACT.md` and return per-frame side outputs that ECL can convert into the same approved live-overlay payload format;
- a level-10 full-game review can upgrade saved lower-level live frames for the same FEN/ply/side;
- at most one AI narrative compression call is allowed by ECE for full-game review;
- ECL stores sanitized review frames or references;
- ECL records token/quota spend/refund state according to monetisation policy;
- failed ECE review does not mutate live game state and follows token refund/no-spend policy;
- ECE `invalid_game` or malformed response is surfaced as an analysis failure, not as fabricated advice.

## 9. Shared Overlay Shell

The same EvenChess overlay shell must be used across:

- live games;
- computer games;
- analysis replay;
- study board review;
- opening explorer/training where allowed;
- requested full-game/custom review.

Shared shell behavior:

- board overlays render in the same layer and visual grammar;
- level card uses the same toggle model;
- coach card uses the same text layout;
- eval strip uses the same display component;
- toggles hide/show authorized features locally;
- missing payload state keeps the shell stable without fake content;
- review mode labels must be clear enough to distinguish retained live history from requested review analysis.

Live-vs-review difference:

- live shell mutates live Used Level and assistance ledger only through server-authorized live delivery;
- review shell never mutates live fairness state;
- review shell may track a viewing-session selected level for display only.

## 10. Current Implementation State

Foundations already present:

- `AnalysisMemory.scala` defines retention policy defaults of last 10 recent games and last 100 requested analyses;
- `AnalysisMemory.scala` models `StoredRecentGame`, `FullGameAnalysisKey`, `StoredFullGameAnalysis`, `UserAnalysisMemory`, lookup results, and mode-neutral overlay shell policy;
- `AnalysisMemory.scala` rejects raw ECE retention by default through `retainRawEcePayloads=false` and `rawEceRetained=false` validity checks;
- `LiveCoaching.scala` contains retained-history and review planning types, including `ReviewModeEngine.liveReviewFrame`, `planCustomReview`, and `planFullGameReview`;
- `LiveCoaching.scala` states that full-game review is post-game-only and does not mutate live Used Level, Assistance Load, Used Offset, ECR, or matchmaking;
- `AnalysisMemoryTest.scala` covers last-10 recent game retention, last-100 requested analysis retention, level-keyed analysis keys, best available frame lookup, and mode-neutral overlay shell separation from live fairness;
- `LiveCoachingTest.scala` has review-mode and full-game review planner coverage;
- ECE combined requirements confirm `/v1/ece/game-review` is a post-game review route and must not alter live fairness.

2026-06-08 implementation update:

- production ECL routes now exist for post-game review state lookup, ECEMF generation, match summary, non-live Ask AI, and ad-earned non-live Ask AI credit grants under `/evenchess/review/...`;
- ECL now has a server-side JSON review store, configurable with `EVENCHESS_REVIEW_STORE_DIR`, for sanitized canonical ECEMF objects, per-ply per-side display frames, match summaries, non-live Ask AI payloads, and quota ledger files;
- the native analysis/replay side panel now renders an EvenChess Coach Review card driven by `ctrl.node.ply`; the card is mounted in an `analyse__side-stack` so real-game replay pages keep the card when Lichess replaces `aside.analyse__side` with server-side replay HTML;
- the round replay overlay also has a production post-game review panel for completed-game contexts that still use the round bundle;
- the existing universal board overlay endpoint can render stored ECEMF frames during replay/analysis;
- ECEMF generation is L10-only, post-game-only, validates diagnostics and forbidden fields before storing, and stores both white and black side display frames.
- browser verification on `http://localhost:8080/5H4cNrww/black` after restarting `lila` generated 6 ECEMF frames, returned a Match Summary, returned non-live Ask AI text, and preserved the saved Ask AI payload after page reload.

Remaining deployment blockers:

- the review store is durable and server-deployable when its directory is persisted, but it is not yet a Mongo/schema-migrated repository;
- no production game-completion hook writes every live ECE output into retained recent-game history automatically;
- no production cleanup job enforces last-10/last-100 retention or paid saved-game exceptions yet;
- non-live Ask AI has a dedicated server-side daily/ad-credit ledger, but final subscription-provider wiring for Standard/Premium tier source is still pending;
- full restart/reopen coverage across multiple old games and step-every-ply overlay verification remains required before public launch.

## 11. Persistence Requirements

Add durable storage for:

- retained recent game memory;
- requested full-game/custom analyses;
- per-ply sanitized ECE history frame references;
- paid saved-game exception records;
- retention cleanup audit events;
- analysis request audit events;
- full-game ECE request/response references;
- token/quota spend/refund references.

Repository operations:

- remember completed game for a user;
- list retained recent games;
- look up retained frame by game, ply, and mode;
- remember requested analysis by key;
- fetch requested analysis by key;
- enforce last-10 and last-100 retention;
- preserve paid saved-game exceptions;
- delete/anonymize retained history for privacy requests;
- audit all retention mutations.

Storage rules:

- raw ECE retention defaults to disallowed;
- sanitized payload references must be versioned;
- old schema versions must fail closed or migrate explicitly;
- retention cleanup must be idempotent;
- multi-node writes must be atomic enough to avoid duplicate requested-analysis spends.

## 12. Native Lichess Integration Requirements

Reuse Lichess foundations:

- game history;
- PGN/move replay;
- analysis board;
- study board;
- permissions;
- account ownership;
- completed-game lookup.

Required integration seams:

- game-completion hook to register retained memory;
- live ECE payload capture hook after accepted live board-state response;
- replay/analysis data extender for namespaced `evenchess` review data;
- route/controller endpoint for requested analysis;
- token/quota check seam;
- background job for full-game ECE analysis;
- retention cleanup job;
- audit/admin inspection seam.

Do not rebuild:

- ordinary chess replay;
- PGN storage;
- move legality;
- normal Lichess analysis board;
- normal Lichess rating history.

## 13. Token and Entitlement Rules

No token required:

- viewing retained live history inside the last-10 memory window;
- switching between Live White, Live Black, and Live Both when retained history exists;
- local display toggles;
- replaying existing requested analysis cache hits where policy says no re-spend.

Token/quota may be required:

- full-game requested analysis;
- custom review levels above saved live levels;
- review with AI narrative enabled;
- L10 full-game analysis;
- saved-game retention exceptions beyond default policy.

Token policy must be ledger-backed and idempotent.

## 14. Audit and Telemetry Requirements

Audit each:

- retained game created;
- retained game replaced by rolling cleanup;
- paid saved-game exception applied or removed;
- retained frame displayed;
- retained frame missing;
- requested analysis started;
- requested analysis cache hit;
- token/quota check;
- token spend;
- refund/no-spend decision;
- ECE game-review request;
- ECE game-review success/failure;
- requested analysis stored;
- review display shown;
- privacy deletion/anonymization.

Telemetry must distinguish:

- live coaching;
- retained live review;
- requested full-game analysis;
- custom analysis;
- computer review;
- study/training review.

Review telemetry must not feed live ECR settlement.

## 15. Required Tests

Server tests:

- last-10 recent game retention drops oldest non-exempt record;
- paid saved-game exception protects selected record;
- last-100 requested analysis retention drops oldest non-exempt requested analysis;
- full-game analysis key separates game, levels, perspective, ECE version, policy version, and AI flag;
- cache hit does not spend duplicate token;
- missing history returns analysis-required state;
- retained history frame returns correct ply/side/perspective;
- retained frame rejects mismatched FEN/position hash;
- raw ECE payload retention is rejected by default;
- review level/session selection does not mutate live Used Level or ECR;
- full-game review plan is post-game-only and no-live-mutation.

Controller/integration tests:

- completed game writes retained memory;
- replay page receives namespaced EvenChess review shell state;
- stepping through retained game loads matching frame;
- missing retained game shows shell with analysis request state;
- requested analysis endpoint validates ownership and token/quota;
- ECE game-review failures do not create fake payloads;
- retention cleanup is idempotent.

Browser/UI tests:

- retained game opens with overlay shell;
- stepping moves updates cards/overlays from retained frames;
- local toggles work in review mode;
- missing history keeps stable shell and shows analysis request action;
- custom review result uses same overlay components;
- review mode does not show live-game assistance state as mutable.

## 16. Patch Map and Integration Impact

Patch-map entries are required when implementation edits touch:

- native game completion lifecycle;
- replay/analysis controller data;
- analysis board UI;
- study board UI;
- routes;
- token/quota controller paths;
- background job scheduler;
- storage repository wiring.

No patch-map update is required for this Phase M requirements-only output.

## 17. Implementation Work Items

M1. Add durable schema/repositories for recent game memory, requested analyses, saved-game exceptions, and per-ply frame references.

M2. Add live ECE capture from accepted board-state responses into sanitized per-ply history.

M3. Add game-completion hook that finalizes each user's retained recent-game memory.

M4. Add retention cleanup and paid saved-game exception logic.

M5. Add replay/analysis controller integration with namespaced EvenChess review data.

M6. Add requested full-game/custom analysis controller and background job.

M7. Wire server-to-server `/v1/ece/game-review` through the hardened ECE gateway.

M8. Integrate token/quota ledger checks for requested analyses.

M9. Reuse the round overlay shell in replay/analysis/study surfaces without live-state mutation.

M10. Add server, integration, and browser tests listed in this phase.

## 18. Phase M Acceptance Status

Status: conducted, not deployment-complete.

Completed foundations:

- analysis-memory domain model exists;
- rolling last-10 and last-100 policy is represented and unit-tested;
- full-game analysis keys include levels, perspective, ECE version, policy version, and AI flag;
- mode-neutral overlay shell policy exists;
- review planning correctly marks full-game review as post-game-only and non-mutating.

Remaining release blockers:

- durable storage and migrations are not complete;
- live ECE history capture into storage is not proven;
- native Lichess replay/analysis UI integration is not proven;
- requested full-game analysis endpoint/job is not proven;
- token/quota ledger integration is not proven;
- retention cleanup and paid saved-game exceptions are not implemented end to end;
- browser tests for retained replay are still required.

## 19. Phase N Entry Criteria

Before Phase N monetisation can rely on analysis/review:

- requested analysis token/quota hooks must have durable idempotency;
- failed full-game ECE review must have a defined spend/refund/no-spend outcome;
- paid saved-game exceptions must be durable and reversible;
- retained-history lookup must be stable enough to avoid charging users for already-retained payloads.
