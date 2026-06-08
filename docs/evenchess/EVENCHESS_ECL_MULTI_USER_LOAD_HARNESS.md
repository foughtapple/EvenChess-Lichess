# EvenChess ECL Multi-User Load Harness

This harness is the Lichess-side companion to ECE production load testing.

It is intentionally ECL-owned because only EvenChess-Lichess can exercise matchmaking/search, game creation, move flow, server-to-server ECE calls, replay caching, and audit behaviour together.

## Run

From the EvenChess-Lichess repo:

```bash
cd /home/jayde/dev/lila-docker/repos/lila
ECL_LOAD_BASE_URL=http://localhost:8080 \
ECE_LOAD_TEST_TARGET=http://127.0.0.1:8787 \
ECE_INTERNAL_API_KEY=<staging-or-production-like-key> \
ECL_LOAD_REQUESTS=100 \
ECL_LOAD_CONCURRENCY=10 \
pnpm evenchess:load:ecl
```

Self-test:

```bash
pnpm evenchess:load:ecl:test
```

## Current Harness Phases

- `ecl_home`: verifies the site is reachable.
- `ecl_evenchess_play`: verifies the EvenChess play surface responds.
- `ecl_search_json_auth_surface`: verifies the search JSON auth surface responds.
- `ecl_bot_ops_panel`: verifies bot-ops admin surface when `ECL_LOAD_AUTH_COOKIE` is supplied; otherwise records a skipped phase.
- `ecl_ece_board_overlay`: calls the same-origin Test Ground ECL-to-ECE board bridge.
- `ecl_replay_payload_cache`: calls the history/cache path after board overlay generation.
- `ecl_proposed_move_bridge`: samples proposed-move server bridge calls.
- `ecl_potential_move_bridge`: samples potential-move server bridge calls.
- `ece_health`: captures ECE health from the same run window.
- `ece_metrics`: captures protected ECE metrics from the same run window.

## Required Staging Extension Before Public Launch

The current script exercises the available local/Test Ground seams. Before public launch, extend the same harness to use staging test users and real backend actions for:

- authenticated search creation;
- sim-bot seeding and bot-vs-user matching;
- game creation;
- legal move submission;
- round socket or replay payload propagation;
- audited ECE render events;
- stale payload rejection after moves;
- replay payload cache hits after game completion.

The launch gate is not satisfied until this integrated staging path passes together with ECE `npm run load:ece:clean`.

## Output

The harness prints JSON with per-phase call counts, status codes, failures, skipped phases, and p50/p95/p99 latency.

Any HTTP `5xx`, connection failure, or timeout is counted as a failed phase. Auth-required phases may be skipped only in local unauthenticated runs; staging launch runs must provide `ECL_LOAD_AUTH_COOKIE` or a staging test-user auth mechanism.
