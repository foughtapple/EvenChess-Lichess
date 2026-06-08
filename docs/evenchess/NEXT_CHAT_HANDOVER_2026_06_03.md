# EvenChess-Lichess Next Chat Handover - 2026-06-03

Use this file as the startup brief for a new Codex chat in the EvenChess-Lichess project.

## Repo

Active repo:

```text
/home/jayde/dev/lila-docker/repos/lila
```

Windows UNC path:

```text
\\wsl$\Ubuntu\home\jayde\dev\lila-docker\repos\lila
```

This is the public Lichess fork / EvenChess-Lichess repo. It must not contain private ECE internals, provider secrets, model weights, tablebases, raw prompts, private engine logic, or external engine binaries.

## Required Reading Before Work

Read these first:

1. `AGENTS.md`
2. `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
3. `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md`
4. Relevant local files for the task.

If the task touches ECE calls, ECE payloads, ECE adapters, ECE cache, proposed move mode, full-game mode, or ECE test harness, also read the ECE combined requirements authority:

```text
/home/jayde/dev/lila-docker/repos/ece/docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md
```

Important ECE rule: browser/client code must never call ECE directly. ECL calls ECE server-to-server or through same-origin local Test Ground bridge endpoints only.

## Current Project Shape

EvenChess-Lichess owns:

- Public site, lobby/search UI, round UI, overlays, coach cards, account/tokens/settings surfaces.
- EvenChess MMR/ECR search and match-contract logic.
- Token/subscription/account gates.
- Game policy and live coaching permission on the site side.
- Test Ground launcher/control surfaces.

ECE owns:

- Chess calculations.
- Provider orchestration.
- Stockfish/Maia/Syzygy/opening book/eval-cache.
- Deterministic composer.
- Proposed-move, board-state, and full-game analysis payloads.
- Private prompts, provider paths, secrets, engine internals.

## Most Recent Work Completed

The most recent task was fixing why searching with matchmaking bots and simulation bots enabled still did not produce a game.

Two server-side issues were found and fixed:

1. `app/controllers/EvenChess.scala` only handed off platform-bot matches to Lichess game creation. Human MMR contracts could be produced internally but were not converted into playable games.
2. `modules/evenchess/src/main/PlaySearchIntegration.scala` seeded simulation bot tickets into the shared repository, but the evaluator filtered all bot tickets out of the candidate list before matching. Simulation bots existed but were invisible to human search.

Files changed in the last completed pass:

```text
app/controllers/EvenChess.scala
modules/evenchess/src/main/PlaySearchIntegration.scala
modules/evenchess/src/test/PlaySearchIntegrationTest.scala
docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md
docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md
```

Git status at handover showed these as modified/untracked:

```text
 M docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md
 M docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md
?? app/controllers/EvenChess.scala
?? modules/evenchess/src/main/PlaySearchIntegration.scala
?? modules/evenchess/src/test/PlaySearchIntegrationTest.scala
```

Do not assume untracked means unwanted. Treat these as active local EvenChess files unless the user explicitly asks otherwise.

## Matchmaking Fix Details

### Human MMR Contract Handoff

`app/controllers/EvenChess.scala` now generalizes the previous bot-only redirect path:

- Old path: `maybeCreateBotGameRedirect`
- New path: `maybeCreateMatchedGameRedirect`

Behavior:

- Existing platform-bot contracts still create native Lichess AI/computer games.
- Human-vs-human MMR contracts now create and accept a native Lichess challenge.
- Contract-assigned colors are preserved.
- Rated/casual mode is preserved.
- Redirects are cached for both matched tickets.
- Both tickets are retired after successful game handoff.
- `GameStartService.persistBeforeCoaching(...)` is called before coaching may render.

Current limitation:

- The controller uses `GamePolicy.InMemoryGamePolicyRepository`. This is acceptable for local/Test Ground work but is not deployment-grade persistence.

### Simulation Bot Visibility

`modules/evenchess/src/main/PlaySearchIntegration.scala` now treats simulation bot tickets as visible queue candidates:

```text
ec-sim-*
```

Fallback platform bots are still held behind the configured timeout. Simulation bots are available immediately when seeded because they are intended to simulate humans in the pool.

Regression test added:

```text
modules/evenchess/src/test/PlaySearchIntegrationTest.scala
```

Test name:

```text
simulation bot tickets are visible candidates without fallback timeout
```

## Requirements / Patch Map Updates

Added requirement:

```text
REQ-H-V2-047
```

Meaning:

When two human EvenChess search tickets receive a valid MMR match contract, ECL must hand that finalized contract into native Lichess game creation, preserve contract-assigned colors and rated/casual mode, persist EvenChess game policy before coaching may render, cache same-origin redirects for both tickets, and retire both tickets so repeated polling cannot create duplicate games.

Patch map entries added:

```text
PM-2026-105 / INT-2026-109 - Human MMR contract game handoff
PM-2026-106 / INT-2026-110 - Simulation bot tickets visible to MMR search
```

## Tests Already Run

Commands run from Windows/PowerShell via WSL:

```powershell
wsl.exe -d Ubuntu --cd /home/jayde/dev/lila-docker/repos/lila -- ./lila.sh "evenchess/test"
wsl.exe -d Ubuntu --cd /home/jayde/dev/lila-docker/repos/lila -- ./lila.sh "compile"
```

Results:

```text
evenchess/test: 488 passed, 0 failed
compile: passed
```

Notes:

- WSL did not have `sbt` on PATH, so `./lila.sh` used Docker Compose fallback.
- This is currently expected in this workspace unless sbt is installed/configured.

## What Is Still Not Complete

### 1. Browser End-to-End Search Verification

The server-side fixes compile and tests pass, but the actual browser flow still needs manual or automated verification:

1. Launch Test Ground.
2. Start WSL/Docker if needed.
3. Launch EvenChess.
4. Enable simulation bots or matchmaking bot fallback from the admin bot controls.
5. Start a normal EvenChess search with no preferences.
6. Confirm search returns a redirect into a playable game.
7. Confirm rated/casual scope behaves correctly.
8. Confirm no duplicate games are created by repeated polling.

### 2. Deployment-Grade Game Policy Persistence

Current game policy persistence in the controller path is process-local/in-memory. Production needs persistent server-owned EvenChess game-policy storage.

Do not let live coaching depend on browser state. Server policy must remain authoritative.

### 3. ECR Rating Settlement Is Not Fully Production-Integrated

The MMR/search contract path is present, but full deployment-grade ECR settlement after game completion should be audited separately. Do not assume normal Lichess rating settlement is enough for rated EvenChess.

### 4. Simulation Bot Execution Scope

Simulation bot tickets now match through the MMR pipeline. Confirm whether the user expects:

- human vs simulation bot games only;
- bot-vs-bot simulation games to autonomously create and play games;
- or both.

Autonomous bot-vs-bot execution may still need a production worker/control loop. Do not fake this in the browser.

### 5. Test Ground / Local State

Several Test Ground and local ECE bridge counters are still process-local. That is useful for local testing but not a deployment persistence model.

### 6. Overlay / ECE Live Update History

Prior user-reported issue: ECE payloads/overlays sometimes only appeared after browser refresh. There have been fixes for stale FEN, stale payload retries, overlay redraws, toggles, and current-position checks, but a new chat should not assume this is fully verified until it is tested in-browser after current matchmaking fixes.

## Next Practical Test Plan

Run this sequence:

1. Open EvenChess Test Ground.
2. Start Docker/WSL.
3. Launch EvenChess without rebuilding unless needed.
4. Open bot admin from Test Ground.
5. Enable simulation bots with a small batch:
   - count: 2 to 6;
   - scope matching the search mode being tested;
   - level range broad enough, for example 0-10;
   - rating range around default user rating, for example 900-2100.
6. Search from the public EvenChess search card with no preferences.
7. Watch `/evenchess/play/search.json` polling if it does not redirect.
8. If no redirect:
   - inspect search JSON for `matchmaking.matched`;
   - inspect active simulation tickets in admin bot panel;
   - inspect whether the requested queue is rated/casual and matches bot scope;
   - inspect controller logs for challenge/create/accept failures.
9. If redirect occurs:
   - verify game is playable;
   - verify EvenChess side UI appears;
   - verify coaching policy is present before ECE/overlay render.

## Useful Commands

Compile:

```powershell
wsl.exe -d Ubuntu --cd /home/jayde/dev/lila-docker/repos/lila -- ./lila.sh "compile"
```

EvenChess tests:

```powershell
wsl.exe -d Ubuntu --cd /home/jayde/dev/lila-docker/repos/lila -- ./lila.sh "evenchess/test"
```

Search code:

```powershell
rg -n "maybeCreateMatchedGameRedirect|MatchmakingIntegrationService|simulationTicketPrefix|GameStartService|BotSimulation" app modules/evenchess
```

Check ECE health if needed:

```powershell
wsl.exe -d Ubuntu -- curl -s http://127.0.0.1:8787/health
wsl.exe -d Ubuntu -- curl -s http://127.0.0.1:8787/ready
```

## Development Rules To Preserve

- Do not put ECE private internals in ECL.
- Do not make browser/client call ECE directly.
- Do not let browser decide matchmaking, coaching permission, Set Level, Used Level, tokens, or game policy.
- Do not use normal Lichess ratings as EvenChess ECR/MMR.
- Any upstream/core Lichess file edit must be patch-mapped.
- Do not use `git add .`.
- Do not revert user or previous-agent changes unless explicitly requested.

## Suggested First Message For New Chat

```text
Read docs/evenchess/NEXT_CHAT_HANDOVER_2026_06_03.md, AGENTS.md, docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md, and docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md. Continue from the current matchmaking/simulation-bot fix. First verify the browser end-to-end search flow with simulation bots enabled, then report any remaining issue before making broad changes.
```
