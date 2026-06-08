# ECE Version 1.1 Phase J - Bot Operations Admin and Test Ground Control

## Scope

This phase adds the deployable operations surface for the platform-bot matchmaking and simulation plan.

## Requirements

- Matchmaking fallback controls are admin-only and cover enabled state, rated/casual/both scope, and fallback timeout.
- Simulation controls are admin-only and cover enabled/running state, rated/casual/both scope, bot count, rating range, Set Level range, and persona/timing mix.
- Simulation start seeds synthetic bot search tickets into the existing EvenChess search repository and MMR contract pipeline.
- Simulation stop clears only simulation bot tickets and leaves human search records intact.
- Monitoring shows active simulation tickets, seed status, runtime revision, last action/admin, and possible bot-vs-bot contracts.
- Test Ground may embed or link the admin bot operations panel, but must not expose unauthenticated bot-control APIs.
- Browser/client code must not own matchmaking decisions or call ECE directly.

## Acceptance

- Admin accounts can start, stop, configure, and monitor bot matchmaking/simulation from `/dev/evenchess/ops/bots`.
- Test Ground exposes the same admin panel for local workflow.
- Synthetic tickets use the same server-side search repository and MMR Engine as normal search tickets.
- Stop removes simulation tickets without deleting human tickets.
- Public user search status remains deployment-safe and does not expose internal bot diagnostics.

## Deployment Runbook

1. Open `/dev/evenchess/ops/bots` as an account with the Settings permission.
2. Configure matchmaking fallback scope and timeout.
3. Use **Start matchmaking bots** only when the low-player-pool disclosure should show ON.
4. Configure simulation scope, count, rating range, level range, and persona mix.
5. Use **Start simulation** to seed simulation tickets into the local EvenChess search repository.
6. Use **Stop simulation** before rollout changes, incidents, or test cleanup. This clears simulation tickets only.
7. Use Test Ground's embedded admin panel for local workflow; do not add unauthenticated launcher endpoints for bot controls.

## Current Deployment Readiness

- Admin route and forms are behind the existing Lichess Settings permission and CSRF-backed `postForm` flow.
- Matchmaking fallback and simulation settings are persisted in EvenChess backend settings.
- Simulation runtime state is in-memory and local to the running app process; restart stops the runtime state and settings remain available.
- Public search status remains display-safe and does not expose internal ticket ids, seed diagnostics, or MMR internals.
- Public search polling uses an opaque search key so the same server-side ticket keeps its original wait time; polling does not start fresh searches.
- Human-vs-platform-bot matches now hand off once to the native Lichess AI/computer game path and return a same-origin game redirect.

## Remaining Product Boundaries

- Autonomous bot-vs-bot Lichess game creation and bot move execution remain pending.
- Dedicated bot telemetry/audit events beyond generic search/admin logs remain pending.
