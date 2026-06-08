# EvenChess New Chat Handover

Last updated: 2026-06-05

This handover is for a new Codex chat taking over the EvenChess-Lichess project. Read this file first, then read the source-of-truth requirements listed below before editing code.

## Immediate Instruction For New Chat

You are working in the EvenChess-Lichess repo:

```text
/home/jayde/dev/lila-docker/repos/lila
```

The related private ECE repo is separate:

```text
/home/jayde/dev/lila-docker/repos/ece
```

Do not copy ECE internals, engine paths, prompts, secrets, provider data, model weights, tablebases, or generated databases into ECL.

Browser/client code must never call ECE directly. ECL calls ECE server-to-server only.

The worktree is dirty and contains many intentional EvenChess changes. Do not revert files you did not touch. Do not use `git add .`.

## Source Of Truth

Read these before changing anything:

```text
AGENTS.md
docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md
docs/requirements/EVENCHESS_FULL_MATCH_PAYLOAD_CONTRACT.md
docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md
docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md
```

For ECE contract/API changes, read the ECE repo combined requirements:

```text
/home/jayde/dev/lila-docker/repos/ece/docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md
```

The older separate requirement appendix files were moved under `docs/requirements/old` and `docs/requirements/old2`. Do not treat those as current unless the user explicitly asks.

## Completion Report Requirement

Every response after code or requirements changes must include:

```text
# Completion Report

## Summary
## Repo Worked In
## Requirements Used
## Requirements Updated
## Superseded Requirements Recorded
## Files Changed
## Patch Map / Integration Log Updates
## Tests Added or Updated
## Tests Run
## Results
## Incomplete Items
## Risks / Follow-Ups
## Ready for Next Step?
```

Any edit to upstream/core Lichess files must be recorded in:

```text
docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md
docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md
```

## Current Repo State Summary

This repo has broad local changes across:

- `app/controllers/EvenChess.scala`
- `modules/evenchess/src/main/*`
- `modules/evenchess/src/test/*`
- lobby UI, challenge UI, round UI, analyse UI, puzzle UI, opening UI
- Test Ground scripts
- requirements and patch-map docs

Many files are untracked or staged as added in `git status --short`. This is expected from the long EvenChess implementation sequence. Do not clean it up unless the user explicitly asks.

Run this at the start of a new coding turn:

```bash
git status --short
```

Use targeted staging only if asked to stage:

```bash
git add path/to/file1 path/to/file2
```

Never use:

```bash
git add .
git reset --hard
git checkout -- .
```

## Local Runtime

The user normally works through WSL/Docker/Test Ground.

ECL repo:

```text
/home/jayde/dev/lila-docker/repos/lila
```

ECE repo:

```text
/home/jayde/dev/lila-docker/repos/ece
```

ECE local URL:

```text
http://127.0.0.1:8787
```

ECE endpoints:

```text
GET  /health
GET  /ready
POST /v1/ece/board
POST /v1/ece/proposed-move
POST /v1/ece/full-match
```

ECE Linux lifecycle:

```bash
cd /home/jayde/dev/lila-docker/repos/ece
bash scripts/start-ece-linux.sh
bash scripts/stop-ece-linux.sh
bash scripts/smoke-ece-linux.sh
```

When launched from ECL/Test Ground, ECE should be started with debug IO logging and reachable host settings, typically:

```bash
ECE_HOST=0.0.0.0 ECE_DEBUG_IO_LOG=1 ECE_DEBUG_IO_LOG_PATH=/home/jayde/dev/lila-docker/repos/ece/logs/ece-debug-io.json ECE_DEBUG_IO_LOG_MAX_ENTRIES=100 bash scripts/start-ece-linux.sh
```

Test Ground files:

```text
scripts/evenchess-testground.ps1
scripts/evenchess-testground-panel.js
scripts/evenchess-testground-launcher.vbs
scripts/evenchess-test-ece-server.js
```

The Test Ground panel is meant to provide buttons for Docker/WSL, real ECE, test ECE, ECE settings, ECE CLM, build/launch ECL, bot/admin actions, status/debug logs, and sample ECE calls.

## Build And Test Commands

Use WSL:

```powershell
wsl.exe -d Ubuntu --cd /home/jayde/dev/lila-docker/repos/lila -- bash -lc "<command>"
```

Frontend focused tests:

```bash
pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts
pnpm exec tsx --test ui/round/tests/evenchessTestGround.test.ts
pnpm exec tsx --test ui/lobby/tests/evenchessSetup.test.ts
pnpm exec tsx --test ui/lib/tests/evenchessTts.test.ts
pnpm exec tsx --test ui/lib/tests/evenchessUniversalOverlay.test.ts
```

Backend focused tests:

```bash
./lila.sh "evenchess/test"
./lila.sh "evenchess/testOnly lila.evenchess.UserSettingsTest"
./lila.sh "evenchess/testOnly lila.evenchess.PlaySearchIntegrationTest"
./lila.sh "evenchess/testOnly lila.evenchess.LevelBasedMatchmakingTest"
./lila.sh "evenchess/testOnly lila.evenchess.BotOperationsTest"
```

Compile checks commonly used:

```bash
./lila.sh "evenchess/compile"
./lila.sh "round/compile"
./lila.sh "pref/compile"
```

If `pnpm exec tsx --test ...` fails resolving `@/index` from `ui/lib/dist/view/boardMenu.js`, that was previously a test-runner/module-alias issue. Check current aliases before assuming the test target is broken.

## Main Architecture

### ECL Owns

- public Lichess fork/site UI
- matchmaking and EvenChess MMR/ECR
- token/subscription/ad entitlements
- display controls and local rendering
- game clocks and game result authority
- server-side ECE gateway calls
- bot fallback/simulation control and admin settings
- audit/telemetry/history/cache surfaces

### ECE Owns

- deterministic chess calculations
- provider orchestration
- Stockfish/lc0/Maia/Syzygy/opening-book access
- AI/composer text compression
- proposed-move mode
- full-match mode
- ECE-side persistent FEN/side/level/profile cache

### Key ECL Files

Round/overlay:

```text
ui/round/src/view/evenchessOverlay.ts
ui/round/src/interfaces.ts
ui/round/src/ctrl.ts
ui/round/src/socket.ts
ui/round/src/evenchessTestGround.ts
ui/round/css/_evenchess-live.scss
ui/round/tests/evenchessOverlay.test.ts
ui/round/tests/evenchessTestGround.test.ts
```

Lobby/search:

```text
ui/lobby/src/evenchessSetup.ts
ui/lobby/src/setupCtrl.ts
ui/lobby/src/ctrl.ts
ui/lobby/src/view/pools.ts
ui/lobby/src/view/setup/modal.ts
ui/lobby/tests/evenchessSetup.test.ts
modules/evenchess/src/main/PlaySearchIntegration.scala
modules/evenchess/src/main/LevelBasedMatchmaking.scala
modules/evenchess/src/test/PlaySearchIntegrationTest.scala
modules/evenchess/src/test/LevelBasedMatchmakingTest.scala
```

ECE gateway and payload parsing:

```text
app/controllers/EvenChess.scala
modules/evenchess/src/main/EceLiveBridge.scala
modules/evenchess/src/main/EngineGateway.scala
modules/evenchess/src/test/EceLiveBridgeTest.scala
modules/evenchess/src/test/EngineGatewayTest.scala
```

Admin/settings:

```text
app/controllers/Dev.scala
modules/web/src/main/ui/DevUi.scala
modules/web/src/main/Env.scala
modules/evenchess/src/main/AdminBackendSettings.scala
modules/evenchess/src/main/AdminOpsDashboard.scala
modules/evenchess/src/main/UserSettings.scala
modules/pref/src/main/PrefForm.scala
modules/pref/src/main/JsonView.scala
modules/pref/src/main/ui/AccountPref.scala
modules/round/src/main/JsonView.scala
```

Bots:

```text
modules/evenchess/src/main/BotOperations.scala
modules/evenchess/src/test/BotOperationsTest.scala
app/controllers/EvenChess.scala
```

Analysis/history/full-match:

```text
modules/evenchess/src/main/AnalysisMemory.scala
modules/evenchess/src/test/AnalysisMemoryTest.scala
docs/requirements/EVENCHESS_FULL_MATCH_PAYLOAD_CONTRACT.md
```

Universal overlays for puzzle/analysis/opening/study:

```text
ui/lib/src/evenchessUniversalOverlay.ts
ui/lib/tests/evenchessUniversalOverlay.test.ts
ui/puzzle/src/ctrl.ts
ui/puzzle/src/view/main.ts
ui/analyse/src/ctrl.ts
ui/analyse/src/view/main.ts
ui/opening/src/opening.ts
```

## Recent Fixes And Patch Map Tail

Recent patch-map entries include:

- `PM-2026-143`: roster bot established ratings and exact clocks.
- `PM-2026-144`: live WikiBook analysis-style collapse and scroll.
- `PM-2026-145`: managed bot round presence and multi-bot tracking.
- `PM-2026-146`: account default feature toggles.
- `PM-2026-147`: coach-card TTS controls and auto-read preferences.
- `PM-2026-148`: proposed-move toggle restores normal payload cache.

Latest completed change:

The Proposed Move toggle-back bug was fixed in:

```text
ui/round/src/interfaces.ts
ui/round/src/view/evenchessOverlay.ts
ui/round/tests/evenchessOverlay.test.ts
docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md
```

Behavior now:

- A legal proposed move caches a post-move preview payload.
- Proposed preview and normal current-FEN payload are stored separately.
- Clicking Proposed Move while preview is active clears `active` but keeps `baseOverlay`.
- Rendering falls back to `baseOverlay` when `data.evenchess.live` is missing/stale.
- Clicking Proposed Move again reuses the cached proposed preview without consuming another call.

Test run after that fix:

```bash
pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts
```

Result:

```text
56/56 passed
```

Browser screenshot verification was not performed for that latest fix because the current in-app browser tab was on `/account/preferences/evenchess`, not a live round, and no rebuild/reload was forced.

## Overlay Requirements To Preserve

EvenChess overlay should appear wherever a board is available. Non-live surfaces use Set Level 10 unless a stricter mode says otherwise.

Round layout intent:

- Eval bar left of board, full board height, top/bottom flush with board.
- Level selector left of eval bar, under native game card/WikiBook area, flush with board bottom as much as layout allows.
- Coach card right of board, top aligned with board top.
- WikiBook appears above level selector at L6+ and should behave like the existing Lichess analysis WikiBook, not a custom blue card.

Visual overlay rules:

- Student threats: green dotted arrows from student pieces to pieces they can take.
- Opponent threats: red dotted arrows from opponent pieces to pieces they can take.
- Pinned pieces: pin symbol top-left of pinned square.
- Loose/hanging not attackable: orange exclamation circle bottom-left.
- Student-owned attackable hanging: red exclamation circle bottom-left plus red inner rim.
- Opponent-owned attackable hanging: purple exclamation circle bottom-left plus purple inner rim.
- Offset Count: top-right badge on every piece square in payload.
- Positive from side-output perspective: green circle with number.
- Negative from side-output perspective: red circle with number.
- Zero/equal: blue circle with white shield.
- Do not assume offset targets are only opponent pieces. ECE now mirrors board facts into both side outputs.

The user is very sensitive to overlays blanking or jumping. Preserve stable DOM keys and transition behavior.

## Level/Toggles Model

Definitions:

- Set Level: server-authoritative maximum level allowed in the game.
- Used Level: highest level used in that game. It can rise but never fall.
- Preferred starting Used Level: account setting, capped by Set Level at game start.
- Feature toggles: display controls only. They can hide/show authorized payload features but do not lower Used Level.

Expected behavior:

- The level dropdown applies all default toggles up to selected level.
- Per-feature toggles can be manually changed and persist during the game.
- Account settings include preferred default toggles.
- If the user increases the selected level above previous Used Level, ECL must request a fresh ECE payload at the new level.
- New ECE payloads must not reset manual toggle choices.

## Proposed And Potential Moves

Proposed Move:

- User draws exactly one legal green arrow.
- User clicks Proposed Move.
- ECL calls ECE `/v1/ece/proposed-move` server-to-server through the same-origin ECL bridge.
- Legal response contains `proposed_move_evaluation.after_move_side_output`.
- ECL renders the post-move deterministic cards/visuals as a what-if preview.
- Proposed preview can be toggled off to restore normal current-FEN payload.
- Toggling on again uses cache and does not consume another call.
- Illegal/no-arrow/multiple-arrow clicks must not clear the current normal or proposed overlay state.
- Proposed preview clears when a move is made or arrow/FEN changes.
- Proposed preview payloads are not stored in match-history ECE payload memory.

Potential Moves:

- Separate from proposed moves.
- Payload may include potential data, but ECL must not auto-display it.
- User reveals through `Opponent Potentials` and `My Potentials` buttons.
- Quotas are Used Level based and server-authoritative.
- Browser can toggle an already revealed cached set off/on without consuming another token.
- Opponent potentials must use opponent-side perspective.

## Eval Bar And Eval Text

Stockfish eval surfaces should update only from accepted deep/advanced ECE payloads, not quick placeholder `0`.

ECE changed eval shape. ECL should prefer fixed White-positive values:

- `score.cp_white`
- `score.mate_white`

Fallbacks exist for legacy fields, but do not let quick `evaluation: 0` snap the UI to equal.

Behavior:

- Board eval bar uses White-positive values for fixed board orientation semantics.
- Coach eval strip displays from the viewer/player perspective.
- Proposed preview can switch to proposed post-move eval and back to live eval smoothly.
- If proposed preview lacks eval, keep showing last accepted live eval.

## Matchmaking/MMR Current Model

The user revised matchmaking to a simplified model:

- Search card has one dropdown: `Preferred level`.
- `Any` means no preference.
- `0` to `10` means own preferred Set Level.
- Opponent target level and strict/wait preference were removed.

Core policy:

- ECL computes rating windows from ECOR level-offset tables.
- No-preference search uses the full L0-L10 assistance window and assigns Set Levels after pairing.
- Preferred own Set Level fixes requester level and assigns opponent level using ECOR table.
- Rating settlement uses actual Used Level / Used Offset by adjusting effective rating inputs.
- Normal Lichess rating should remain isolated from EvenChess ECR unless explicitly intended.

Important modules:

```text
modules/evenchess/src/main/LevelBasedMatchmaking.scala
modules/evenchess/src/main/PlaySearchIntegration.scala
modules/evenchess/src/main/EcrRating.scala
modules/evenchess/src/main/EvenChessRatingCalibration.scala
```

Admin settings include ECOR/equivalent rating calibration and Stockfish equivalent rating bands.

## Bots

Two bot modes:

1. Matchmaking fallback bots.
   - If a human waits past configured timeout, ECL can match them with a roster-backed managed bot.
   - Should look like a normal user account, not Stockfish.
   - Should use the selected time control.
   - Should show both clocks like a human-vs-human game.
   - Should have established, non-round display ratings, not `1500?`.

2. Simulation bots.
   - Admin can run N simulated human accounts in rated/casual/both and selected time-control pools.
   - They seed/search like humans, get matched through the same EvenChess search contract path, and can play humans or each other.
   - Only one broad bot mode should run at a time if the UI/admin model requires that.

Recent bot fixes:

- Roster bot ratings and exact clock metadata were added.
- Managed bot presence publishes native bot-connected round bus events.
- Multi-bot tracking was fixed for sim-vs-sim games.

Still verify in browser:

- Bot games show both clocks.
- Bot opponent does not show as `Stockfish`.
- No "opponent left" prompt during managed bot thinking.
- Bot ratings are established and non-round.
- Matchmaking bots and sim bots both enter the correct time-control pools and can match a human search.

## TTS

Coach TTS is implemented through safe browser speech config:

```text
ui/lib/src/evenchessTts.ts
ui/round/src/view/evenchessOverlay.ts
modules/evenchess/src/main/UserSettings.scala
modules/pref/src/main/ui/AccountPref.scala
```

Expected behavior:

- Coach card has a visible manual Speak button when displayable coach text exists.
- Disabled Speak button explains why if TTS is disabled/unsafe.
- Account settings contain TTS enabled, voice/rate/volume/queue/mute, auto-read toggle, and auto-read delay.
- Auto-read only speaks authorized displayed coach text and should not repeat same audit/text payload on redraw.
- Auto-read respects mute-during-opponent-turn.

Recent tests:

```bash
pnpm exec tsx --test ui/lib/tests/evenchessTts.test.ts
pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts
./lila.sh "evenchess/testOnly lila.evenchess.UserSettingsTest"
./lila.sh "round/compile"
./lila.sh "pref/compile"
```

## Analysis Memory / History

Expected model:

- Live games cache/store approved board-state ECE payloads by game, side, FEN/position key, ply, and level.
- Store canonical board-state payloads only. Do not store proposed-move previews.
- On board change/replay, first check cache/history for same game/FEN/side at equal or higher delivered level.
- If cached payload exists, apply it without recalculating.
- If user raises Used Level beyond cached delivered level, call ECE again and upgrade cache for that FEN.
- Last 10 live games per user retained.
- Full-match review requests retained separately, last 100 requested analyses.
- Full-match ECE format is documented in:

```text
docs/requirements/EVENCHESS_FULL_MATCH_PAYLOAD_CONTRACT.md
```

Review/history surfaces should use the same overlay shell, level controls, toggles, coach card, and eval behavior as live games.

## Public Launch / Plan v1.6

The repo contains the public-deployment plan:

```text
docs/requirements/planv1.6.md
docs/requirements/planv1.6_phase_*.md
```

Phases A-Z were requested and at least partially implemented/reported. Do not assume production readiness without re-running tests and browser flows. The user specifically wants local testing to converge toward server-deployable behavior.

High-priority production checks:

- ECL/ECE deployment topology.
- ECE gateway privacy and timeout behavior.
- Matchmaking/bot human-like behavior and clocks.
- ECOR/ECR correctness and admin calibration controls.
- Token-free launch date window behavior.
- Token consumption/audit after the free window.
- Overlay stability and payload cache correctness.
- Public copy/features retained from Lichess vs removed.
- Abuse/fairness controls.
- Telemetry/audit retention.
- Backups, migration, rollback.
- Load harness results.

## Known Open Or Recently Reported Items

Treat these as "verify/fix" unless a later test proves they are resolved:

1. Proposed Move toggle-back was just fixed in code and tests, but not browser-screenshot verified after rebuild.
2. Matchmaking/simulation bots must look and behave like human accounts, including both clocks and no Stockfish label.
3. User previously observed only the human timer in bot games and sometimes "opponent left" prompts.
4. WikiBook in live games should behave exactly like analysis WikiBook, including open/close and internal scroll.
5. Puzzle board should include EvenChess coach and level/toggle controls in a logical native layout.
6. Eval bar/status must use deep/advanced eval only and not snap to 0 from quick payloads.
7. Overlay should not disappear between moves; only changed items should update.
8. Home page create lobby game / challenge friend / play against computer buttons were reported greyed out at one point. Verify current state before assuming fixed.
9. Bot simulation admin UI text should be clear; avoid terms like "seed" without explanation/tooltips unless kept as operator jargon.
10. Stress/load testing has been requested; scripts exist but real capacity numbers still need a controlled run.

## Browser Verification Guidance

The user expects Codex to use the in-app browser when needed. If asked to visually verify:

1. Use the Browser skill.
2. Start/reload ECL only after ensuring the current code is built.
3. Open a computer game or EvenChess search result.
4. Make moves using the known Test Ground move bridge if normal Chessground automation is unreliable.
5. Screenshot before/after the move or button toggle.

Known browser note:

- In-app browser automation had trouble making native Chessground clicks directly.
- A Test Ground move bridge was added to advance moves for QA.

## Current Latest Test Evidence

Most recent verified command:

```bash
pnpm exec tsx --test ui/round/tests/evenchessOverlay.test.ts
```

Result:

```text
56 tests passed
```

This covered:

- stable overlay keys
- move-refresh retention
- level/toggle behavior
- eval handling
- offset/hanging/pins/threat visuals
- proposed move quotas and toggle behavior
- potential move reveal behavior
- TTS button/config behavior

## How To Continue Safely

For any new request:

1. Read this handover.
2. Read `AGENTS.md`.
3. Read the relevant current requirements sections.
4. Check `git status --short`.
5. Inspect existing code and tests before editing.
6. Make the smallest scoped change.
7. Add/update focused tests.
8. Run focused tests first.
9. Update requirements only if behavior changes.
10. Update patch map/integration log for upstream/core seams.
11. Report clearly what was verified and what was not.

## Suggested First Prompt To New Chat

Paste this to a new chat:

```text
Read /home/jayde/dev/lila-docker/repos/lila/docs/evenchess/EVENCHESS_NEW_CHAT_HANDOVER_CURRENT.md, then read AGENTS.md and the current EvenChess requirements it lists. Continue from that state. Do not revert existing worktree changes, do not use git add ., and report with the required Completion Report format after code or requirements changes.
```
