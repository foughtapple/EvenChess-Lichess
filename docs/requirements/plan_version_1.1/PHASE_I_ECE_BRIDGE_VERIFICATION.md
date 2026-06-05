# ECE Version 1.1 Plan - Phase I Bridge Verification

**Phase:** I - ECE v1.1 contract and live bridge verification
**Date:** 2026-06-01
**Repo:** EvenChess-Lichess
**Scope:** Verify platform-bot matchmaking and simulation games use the same server-only ECE quick/deep board-state bridge as human games.

## Requirements Used

- `docs/requirements/plan_version_1.1/PLAN.md`, Phase I.
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`.
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`.
- ECE source of truth: `/home/jayde/dev/lila-docker/repos/ece/docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md`.
- ECE caller contract: `/home/jayde/dev/lila-docker/repos/ece/docs/requirements/EVENCHESS_ENGINE_CONTRACT.md`.

## Contract Decision

Bot-driven games do not get a separate ECE endpoint, payload branch, or browser-side path.

EvenChess-Lichess now models the ECE live bridge with a typed game context:

- `EceBridgeGameSource.Human`
- `EceBridgeGameSource.MatchmakingBot`
- `EceBridgeGameSource.SimulationBot`

That context is used only by the EvenChess-Lichess server-side gateway to construct the normal board-state request:

1. `POST /v1/ece/board/quick`
2. conditional `POST /v1/ece/board/deep` with `quick_request_id` and `quick_context_id`

The ECE public request shape is unchanged. ECE remains separate, private, and server-to-server only. ECE is not given provider paths, secrets, raw prompts, or browser-generated entitlement truth. Bot source metadata does not create a separate chess-analysis branch in ECE.

## Verification Matrix

| Scenario | Expected result | Verified by |
|---|---|---|
| Human vs human live bridge | Normal `board_state` quick request | `EngineGatewayTest` |
| Human vs matchmaking bot | Same request shape as human vs human | `EngineGatewayTest` |
| Simulation bot vs simulation bot | Same request shape as human vs human | `EngineGatewayTest` |
| L5+ or higher mixed levels | `deep_requested=true` and allowed deep modules present | `EngineGatewayTest` |
| Deep request after quick context | `board_deep` request echoes quick request id, FEN, levels, and modules | `EngineGatewayTest` |
| Stale FEN or leaked public provider payload | rejected before display | `EngineGatewayTest` |
| Real ECE unavailable | non-fatal degraded path remains valid | existing `EngineGatewayTest` |
| Mock ECE quick/deep fixture | test payload mirrors quick/deep/proposed/review envelopes | `scripts/evenchess-test-ece-server.test.mjs` |

## Acceptance Status

- Bot games have no separate ECE contract branch.
- ECE payload path and validation behavior remain identical to human games.
- Quick/deep context handling is covered by focused gateway tests and the Test Ground fixture smoke.
- Browser/client code still never calls ECE directly.

## Remaining Work

Phase J has added the admin-only operations surface and Test Ground embedding for bot matchmaking fallback and simulation lifecycle controls.

Remaining work is limited to the production game-execution seam: turning finalized fallback/simulation contracts into autonomous Lichess games with bot move execution, plus dedicated bot telemetry/audit events beyond the current generic search/admin logs.
