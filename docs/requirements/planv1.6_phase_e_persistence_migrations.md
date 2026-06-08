# EvenChess-Lichess Plan v1.6 Phase E - Database Persistence and Migration Strategy

**Date:** 2026-06-03
**Phase:** E
**Status:** Conducted as the v1.6 persistence and migration baseline
**Repo:** EvenChess-Lichess
**Path:** `/home/jayde/dev/lila-docker/repos/lila`
**Branch:** `codex/evenchess-v1.6-readiness`

This document completes Plan v1.6 Phase E by defining production-safe persistence, migration strategy, indexes, retention jobs, and restart-recovery requirements for all server-authoritative EvenChess state.

Phase E is planning/documentation only. It does not implement Mongo repositories, migrations, or codecs.

---

## 1. Phase E Decision Summary

Production persistence decision:

```text
EvenChess production authority must use dedicated durable EvenChess stores.
Do not put broad EvenChess state into core Lichess game BSON unless explicitly approved and patch-mapped.
Use the core game id as a reference key, not as the storage location for every EvenChess field.
```

Preferred initial database:

```text
MongoDB, following existing Lila persistence conventions, with dedicated EvenChess collections.
```

Critical production blockers found in current code:

- `GamePolicy.Runtime.gamePolicyRepository` is currently in-memory.
- `PlaySearchIntegration.SearchRepositoryRuntime.local` is currently in-memory.
- `EvenChessRatingCalibration.GameHistory` stores calibration samples in memory.
- `BotOperations.BotSimulationRuntime` stores runtime state in memory.
- `app/controllers/EvenChess.scala` contains mutable maps for proposed-move cache, potential-move cache, public search keys, ticket lookup, and matched-game redirects.

These are acceptable for local/Test Ground work but not production authority. Browser refresh, service restart, or multi-process deployment must not reset these states.

---

## 2. Persistence Principles

1. Every fairness-affecting record has `schemaVersion`.
2. Every matchmaking/rating/assistance record has `policyVersion` or more specific version fields.
3. Every record that affects rating settlement can be replayed from stored inputs.
4. Browser display state may hide/show data, but cannot be the source of authority.
5. Use append-only ledgers for audit, token, rating, and coaching render events.
6. Use idempotency keys for writes triggered by polling, retries, or websocket reconnect.
7. Avoid storing raw ECE provider data, raw prompts, raw unrestricted engine output, private paths, or secrets.
8. Store enough ECE payload history to reconstruct approved overlays/review, not private internals.
9. Retention jobs must be explicit, observable, and safe to rerun.
10. Old records must remain readable after policy/table changes.

---

## 3. Collection Baseline

Collection names are proposed. Final names may be adjusted to match Lila conventions during implementation, but the data domains must remain separate and durable.

| Collection | Purpose | Primary key | Retention |
|---|---|---|---|
| `evenchess_search_ticket` | Active and recently completed search tickets | `ticketId` | expire completed/abandoned tickets |
| `evenchess_match_contract` | Finalized human/human, bot, and friend match contracts | `contractId` or `requestId` | retain with game/rating audit |
| `evenchess_game_policy` | Server-owned game policy and Set Levels | `gameId` | retain with game |
| `evenchess_game_assistance_state` | Current per-game/player assistance state | `gameId + playerId` | retain while active, summarize on completion |
| `evenchess_assistance_ledger` | Append-only coaching/display/Used Level audit events | `eventId` | policy-retained, default admin retention |
| `evenchess_ece_payload_history` | Per-ply accepted ECE payload metadata and safe display payload refs | `gameId + ply + side + phase + level` | rolling last 10 games per user unless saved |
| `evenchess_proposed_move_use` | Proposed move uses and cached legal previews | `gameId + playerId + ply + uci` plus game counters | game lifetime plus audit retention |
| `evenchess_potential_reveal_use` | Potential move reveal uses and cached authorized reveals | `gameId + playerId + revealSide + ply + fenKey` plus game counters | game lifetime plus audit retention |
| `evenchess_analysis_memory` | Requested full-game/custom analyses | `analysisId` | rolling last 100 per user unless saved |
| `evenchess_ecr_rating` | ECR rating by user/pool | `userId + poolKey` | durable |
| `evenchess_ecr_rating_event` | Append-only rating settlement events | `eventId` | durable/audit |
| `evenchess_ecor_table_snapshot` | ECOR/base-level/stockfish table snapshots | `snapshotId` | keep at least latest 30 or admin policy |
| `evenchess_calibration_sample` | Latest rating calibration samples | `gameId` | cap latest 1,000,000 |
| `evenchess_calibration_run` | Admin calibration run results | `runId` | admin policy |
| `evenchess_token_ledger` | Token grants/spends/refunds/adjustments | `ledgerEventId` | durable/accounting |
| `evenchess_entitlement_snapshot` | Subscription/ad/plan entitlement state snapshots | `userId + source + version` | durable while active plus audit |
| `evenchess_bot_operation` | Bot fallback/simulation config, run state, and events | `operationId` | admin policy |
| `evenchess_admin_audit` | Admin setting and incident control changes | `auditId` | admin policy |

---

## 4. Core Schemas

### 4.1 Search ticket

Required fields:

- `ticketId`
- `publicSearchKey`
- `playerId`
- `poolKey`
- `rated`
- `timeControl`
- `preferredOwnSetLevel`
- `ecrSnapshot`
- `expectedUsedOffset`
- `botProfile`
- `tokenSnapshotRef`
- `abuseClear`
- `createdAt`
- `expiresAt`
- `status`
- `schemaVersion`
- `matchmakingPolicyVersion`

Indexes:

- unique `ticketId`;
- unique `publicSearchKey`;
- `poolKey + status + createdAt`;
- `playerId + status + createdAt`;
- `expiresAt` TTL or cleanup index;
- `botProfile.kind + status` for simulation cleanup.

Migration rule:

- Existing in-memory ticket records must not be trusted after deployment restart. Production starts with an empty ticket collection unless a migration from a prior production store exists.

### 4.2 Match contract

Required fields:

- `contractId`
- `requestId`
- `gameId`
- `source`: human, bot fallback, simulation, friend
- `whitePlayerId`
- `blackPlayerId`
- `rated`
- `timeControl`
- `whiteSetLevel`
- `blackSetLevel`
- `whiteEcr`
- `blackEcr`
- `whiteExpectedOffset`
- `blackExpectedOffset`
- `whiteEffectiveRating`
- `blackEffectiveRating`
- `preferredLevelSummary`
- `unevenMatch`
- `unevenReason`
- `tokenGateResult`
- `createdAt`
- `acceptedAt`
- `retiredAt`
- `schemaVersion`
- `matchmakingPolicyVersion`
- `ecorPolicyVersion`
- `baseLevelTableVersion`

Indexes:

- unique `contractId`;
- unique sparse `gameId`;
- `requestId`;
- `whitePlayerId + createdAt`;
- `blackPlayerId + createdAt`;
- `source + createdAt`;
- `unevenMatch + createdAt`.

Migration rule:

- Contract creation must be idempotent. Repeated polling must return the same redirect/contract, not create duplicate games.

### 4.3 Game policy

Required fields:

- `gameId`
- `mode`
- `rated`
- `timeControlBucket`
- `white.playerId`
- `white.setLevel`
- `white.poolKey`
- `black.playerId`
- `black.setLevel`
- `black.poolKey`
- `featureFlags`
- `createdAt`
- `updatedAt`
- `completedAt`
- `versions.schemaVersion`
- `versions.setLevelPolicyVersion`
- `versions.assistancePolicyVersion`
- `versions.ecrPolicyVersion`
- `versions.auditLedgerVersion`
- `matchContractId`

Indexes:

- unique `gameId`;
- `white.playerId + createdAt`;
- `black.playerId + createdAt`;
- `mode + rated + timeControlBucket`;
- `versions.schemaVersion`;
- `matchContractId`.

Migration rule:

- Game policy must be persisted before any live coaching may render.
- Normal Lichess games must not get inferred EvenChess policy from client claims.

### 4.4 Game assistance state

Required fields:

- `gameId`
- `playerId`
- `side`
- `setLevel`
- `highestUsedLevel`
- `usedOffset`
- `assistanceLoad`
- `featureTogglesSnapshot`
- `lastAcceptedFenKey`
- `lastAcceptedPly`
- `lastPlayerTurnTextPayloadRef`
- `quickPayloadStatus`
- `deepPayloadStatus`
- `proposedMoveCount`
- `potentialOpponentRevealCount`
- `potentialOwnRevealCount`
- `updatedAt`
- `schemaVersion`
- `assistancePolicyVersion`
- `usedOffsetPolicyVersion`

Indexes:

- unique `gameId + playerId`;
- `gameId + side`;
- `playerId + updatedAt`;
- `highestUsedLevel + updatedAt`.

Migration rule:

- `highestUsedLevel` only increases in live games.
- Lowering toggles changes display state, not retained Used Level.

### 4.5 Assistance ledger

Required fields:

- `eventId`
- `gameId`
- `playerId`
- `side`
- `ply`
- `fenKey`
- `featureKey`
- `action`
- `requestedLevel`
- `setLevel`
- `deliveredLevel`
- `usedLevelAfter`
- `assistanceWeightDelta`
- `visibility`
- `sourceType`
- `engineJobId`
- `aiRequestId`
- `rated`
- `createdAt`
- `policyVersion`
- `schemaVersion`

Indexes:

- unique `eventId`;
- `gameId + playerId + ply + createdAt`;
- `gameId + featureKey`;
- `playerId + createdAt`;
- `schemaVersion + policyVersion`.

Migration rule:

- Ledger is append-only.
- Corrections require compensating events, not mutation, unless a migration explicitly marks old records as superseded.

### 4.6 ECE payload history

Required fields:

- `payloadId`
- `gameId`
- `ply`
- `fenKey`
- `sideToMove`
- `side`
- `requestedLevel`
- `deliveredLevel`
- `usedLevelAtAcceptance`
- `phase`: quick, deep, proposed-preview, full-game
- `eceRequestId`
- `eceVersion`
- `policyVersion`
- `acceptedAt`
- `staleRejected`
- `diagnosticsStatus`
- `safeDisplayPayload`
- `overlayAtoms`
- `coachText`
- `evalSource`
- `rawEceRetained`: false by default

Indexes:

- unique `payloadId`;
- `gameId + ply + side + phase + deliveredLevel`;
- `gameId + fenKey`;
- `eceRequestId`;
- `side + acceptedAt`.

Migration rule:

- Store safe display payloads or payload refs only.
- Do not store raw provider output, raw prompts, provider paths, or shared/internal calculations.

### 4.7 Proposed move use

Required fields:

- `useId`
- `gameId`
- `playerId`
- `side`
- `ply`
- `fenKey`
- `uci`
- `usedLevel`
- `legal`
- `consumed`
- `advicePayloadRef`
- `previewPayloadRef`
- `createdAt`
- `schemaVersion`
- `policyVersion`

Indexes:

- unique `useId`;
- unique `gameId + playerId + ply + uci`;
- `gameId + playerId + consumed`;
- `gameId + playerId + ply`.

Migration rule:

- Re-requesting the same legal proposed move for the same turn returns cached data without consuming another use.
- Illegal or invalid input must not consume and must not clear current state.

### 4.8 Potential reveal use

Required fields:

- `revealId`
- `gameId`
- `playerId`
- `requesterSide`
- `revealSide`
- `ply`
- `fenKey`
- `usedLevel`
- `revealType`: own, opponent
- `consumed`
- `authorizedPayloadRef`
- `createdAt`
- `schemaVersion`
- `policyVersion`

Indexes:

- unique `revealId`;
- unique `gameId + playerId + revealType + ply + fenKey`;
- `gameId + playerId + consumed`;
- `gameId + revealSide + ply`.

Migration rule:

- Browser refresh cannot restore reveal uses.
- Same-position cached reveal can be hidden/shown without consuming again.

### 4.9 Analysis memory

Required fields:

- `analysisId`
- `ownerUserId`
- `gameId`
- `source`: recent-live, requested-full-game, requested-custom
- `completedAt` or `requestedAt`
- `whiteLevel`
- `blackLevel`
- `perspective`
- `eceVersion`
- `policyVersion`
- `useAi`
- `payloadHistoryRefs`
- `tokenLedgerEventId`
- `saved`
- `createdAt`
- `expiresAt`
- `schemaVersion`

Indexes:

- unique `analysisId`;
- `ownerUserId + source + createdAt`;
- `ownerUserId + gameId`;
- `gameId + whiteLevel + blackLevel + perspective + eceVersion + policyVersion + useAi`;
- `expiresAt`.

Migration rule:

- Keep rolling last 10 completed games with live ECE history per user.
- Keep rolling last 100 requested full-game/custom analyses per user.
- Paid saved games bypass default deletion according to entitlement policy.

### 4.10 ECR rating and rating event

Rating record fields:

- `userId`
- `poolKey`
- `rating`
- `ratingDeviation`
- `volatility`
- `gameCount`
- `provisional`
- `createdAt`
- `updatedAt`
- `schemaVersion`
- `ratingPolicyVersion`
- `calibrationPolicyVersion`
- `matchmakingPolicyVersion`

Rating event fields:

- `eventId`
- `gameId`
- `whitePlayerId`
- `blackPlayerId`
- `whiteRatingBefore`
- `blackRatingBefore`
- `whiteRatingAfter`
- `blackRatingAfter`
- `whiteUsedLevel`
- `blackUsedLevel`
- `whiteUsedOffset`
- `blackUsedOffset`
- `result`
- `rated`
- `noRateReason`
- `ecorPolicyVersion`
- `schemaVersion`
- `createdAt`

Indexes:

- unique `userId + poolKey` on rating records;
- unique `eventId`;
- unique sparse `gameId` on rating events;
- `userId + updatedAt`;
- `ecorPolicyVersion + createdAt`.

Migration rule:

- Normal Lichess ratings are untouched.
- Settlement uses actual Used Level/Used Offset, not only starting Set Level.

### 4.11 ECOR snapshots and calibration samples

ECOR snapshot fields:

- `snapshotId`
- `createdAt`
- `createdByAdminId`
- `reason`
- `ecorPolicyVersion`
- `gapTable`
- `baseLevelTable`
- `stockfishEquivalentRatingTable`
- `active`
- `schemaVersion`

Calibration sample fields:

- `gameId`
- `playedAt`
- `whiteRating`
- `blackRating`
- `whiteUsedLevel`
- `blackUsedLevel`
- `whiteScore`
- `timeControl`
- `featureMix`
- `assistanceLoad`
- `usedOffsets`
- `schemaVersion`
- `ecorPolicyVersion`

Indexes:

- unique `snapshotId`;
- `active + createdAt`;
- unique `gameId` on calibration samples;
- `playedAt`;
- `whiteUsedLevel + blackUsedLevel`;
- `ecorPolicyVersion`.

Migration rule:

- Keep latest 1,000,000 calibration samples by game id.
- Calibration runs never auto-apply table changes.

### 4.12 Token ledger and entitlements

Token ledger fields:

- `ledgerEventId`
- `userId`
- `eventType`: grant, spend, refund, expire, admin-adjust
- `tokenType`
- `amount`
- `balanceAfter`
- `source`
- `relatedGameId`
- `relatedAnalysisId`
- `idempotencyKey`
- `createdAt`
- `schemaVersion`
- `policyVersion`

Indexes:

- unique `ledgerEventId`;
- unique sparse `idempotencyKey`;
- `userId + createdAt`;
- `relatedGameId`;
- `relatedAnalysisId`.

Migration rule:

- Ledger is append-only.
- Token balance is derived or snapshotted from ledger, not arbitrary client state.
- Premium/tokens never alter live rated assistance strength.

### 4.13 Bot operations

Required fields:

- `operationId`
- `mode`: fallback, simulation
- `enabled`
- `scope`
- `targetCount`
- `activeTicketCount`
- `ratingRange`
- `levelRange`
- `timeControlDistribution`
- `persona`
- `startedByAdminId`
- `stoppedByAdminId`
- `startedAt`
- `stoppedAt`
- `lastHeartbeatAt`
- `schemaVersion`
- `policyVersion`

Indexes:

- unique `operationId`;
- `mode + enabled + startedAt`;
- `lastHeartbeatAt`;
- `startedByAdminId + startedAt`.

Migration rule:

- Simulation tickets must be removable by family without deleting human tickets.
- Restart recovery must either rehydrate active simulation runs or stop them cleanly and retire their tickets.

---

## 5. Migration Strategy

### 5.1 Implementation order

1. Add read/write repository interfaces for each domain.
2. Keep in-memory repositories only under local/test configuration.
3. Add Mongo repository implementations.
4. Add schema validation codecs.
5. Add idempotent write operations.
6. Add background cleanup jobs.
7. Add restart-recovery tests.
8. Switch staging to durable stores.
9. Disable in-memory authority in production.

### 5.2 Migration style

Use migration-safe write paths:

- New records include `schemaVersion`.
- Readers accept current and previous compatible versions.
- One-way data corrections write `migrationVersion` and `migratedAt`.
- Append-only ledgers use compensating events.
- Backfills are idempotent and resumable.
- Destructive migrations require backup and explicit release approval.

### 5.3 No broad core game BSON changes

Phase E confirms:

- Avoid editing core Lichess game BSON/schema internals.
- Store EvenChess policy/history in dedicated collections keyed by `gameId`.
- Only add thin references to core game creation paths if explicitly required and patch-mapped.

---

## 6. Retention and Cleanup Jobs

| Job | Scope | Frequency | Rule |
|---|---|---|---|
| Expire search tickets | `evenchess_search_ticket` | every 1-5 minutes | expire abandoned tickets by `expiresAt` |
| Retire completed tickets | search/contracts | every 5-15 minutes | mark retired after game handoff |
| Clear stale simulation tickets | bot tickets | every 1-5 minutes | remove `ec-sim` family when sim stopped/expired |
| Recent live history retention | analysis memory | on game completion plus daily repair | keep last 10 completed games per user unless saved |
| Requested analysis retention | analysis memory | on request plus daily repair | keep last 100 requested analyses per user unless saved |
| Calibration sample cap | calibration samples | on settlement plus daily repair | keep latest 1,000,000 by playedAt/game id |
| Audit retention | assistance/admin audit | daily | according to admin retention policy, preserving required rating/payment records |
| Token ledger audit | token ledger | never destructive by default | archive only with accounting policy |
| ECE payload privacy cleanup | payload history | daily | remove raw payloads if accidentally retained or after debug window |

All cleanup jobs must log count scanned, count changed, duration, and errors.

---

## 7. Restart and Multi-Process Recovery

Production must survive:

- browser refresh;
- websocket reconnect;
- ECL process restart;
- ECE process restart;
- multiple ECL app instances;
- background job retry;
- repeated search polling;
- duplicate proposed/potential requests;
- deployment restart during active games.

Required behavior:

- Search ticket resumes by `publicSearchKey`.
- Matched ticket returns same game redirect.
- Game policy remains available before coaching renders.
- Used Level remains monotonic.
- Proposed/potential counts remain consumed.
- Cached proposed/potential reveals remain available where policy allows.
- ECE payload history remains attached to game.
- Rating settlement is idempotent by `gameId`.
- Token spends/refunds are idempotent by `idempotencyKey`.

---

## 8. Release Tests Required

Backend persistence tests:

- search ticket survives repository restart;
- matched ticket cannot create duplicate game on repeated polling;
- game policy must exist before coaching render;
- Used Level cannot decrease after restart;
- proposed move count survives refresh/restart;
- potential reveal count survives refresh/restart;
- ECE payload accepted/stale records persist correctly;
- last 10 recent games retained per user;
- last 100 requested analyses retained per user;
- ECOR samples cap at 1,000,000 and dedupe by game id;
- token ledger idempotency;
- rating settlement idempotency;
- normal Lichess games do not gain EvenChess policy by client claim.

Migration tests:

- current schema round-trip;
- previous compatible schema read;
- missing optional fields default safely;
- incompatible schema rejected or migrated explicitly;
- cleanup jobs are idempotent.

Operational tests:

- restart ECL during search;
- restart ECL during game;
- restart ECE during game;
- restart background worker during retention job;
- verify no browser refresh restores spent consumables.

---

## 9. Current Code Gaps to Close

Phase E identified these current non-production persistence gaps:

1. Replace `InMemoryGamePolicyRepository` for production.
2. Replace `InMemoryPlaySearchRepository` for production.
3. Persist controller mutable maps for proposed/potential caches and public search keys.
4. Persist matched game redirects by ticket.
5. Persist bot simulation runtime state and simulation tickets.
6. Persist ECOR calibration samples instead of relying on `GameHistory` in-memory vector.
7. Persist ECE payload history and safe display payload refs.
8. Persist game assistance state, Used Level, Used Offset, and consumable counters.
9. Persist token ledger and entitlement snapshots before enabling monetisation.
10. Add migration/versioning tests.

---

## 10. Phase E Acceptance Status

| Acceptance gate | Status | Notes |
|---|---|---|
| Schemas designed for required domains | Complete as baseline | Sections 3 and 4 |
| Schema/policy version fields defined | Complete | Every authority/audit domain requires versions |
| Migration strategy defined | Complete | Section 5 |
| Indexes defined | Complete as baseline | Section 4 |
| Retention jobs defined | Complete | Section 6 |
| Cleanup jobs defined | Complete | Section 6 |
| Core game BSON corruption avoided | Complete as strategy | Dedicated collections keyed by game id |
| Browser refresh cannot reset authority state | Blocked pending implementation | Requires durable assistance/consumable stores |
| Completed games retain ECE history | Blocked pending implementation | Requires `evenchess_ece_payload_history` and analysis memory stores |
| Old records readable after policy changes | Blocked pending implementation | Requires codecs/migration tests |

Phase E is complete as a persistence and migration strategy. It is not production-ready until Mongo/durable repositories and restart-recovery tests are implemented.

---

## 11. Phase F Entry Criteria

Phase F can proceed with ECE deployment planning, but public deployment remains blocked until Phase E implementation follow-ups are completed.

Phase F should assume:

1. ECL persists game policy before coaching renders.
2. ECL persists ECE payload history and stale rejection records.
3. ECL can recover assistance state after restart.
4. ECE remains a private backend service.
5. ECE logs/debug output are not the source of ECL production authority.
