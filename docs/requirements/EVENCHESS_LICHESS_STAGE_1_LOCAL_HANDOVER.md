# EVENCHESS_LICHESS_STAGE_1_LOCAL_HANDOVER.md

## 1. Purpose

This document records the **Stage 1 local development and testing environment** for the `foughtapple/EvenChess-Lichess` repository.

It is an operational handover for humans and Codex. It is **not** the product requirements document. The product requirements source of truth remains the EvenChess-Lichess Version 1 requirements suite under:

```text
docs/requirements
```

Stage 1 exists to prove that:

- the Lichess fork runs locally;
- accounts and ordinary Lichess flows work;
- the editable source repo is connected to the correct GitHub fork;
- Codex can safely inspect, test, and later modify the fork;
- EvenChess-specific changes can be implemented as a separated assisted-mode layer without rebuilding the chess platform.

## 2. Core Architecture Decision

The active implementation model is:

```text
Lichess provides the base chess platform.
EvenChess adds only the assisted-chess mode layer.
```

Do not rebuild Lichess-provided chess basics unless a specific, approved requirement proves a gap exists.

### Lichess-provided platform capabilities

Lichess/Lila already provides:

- account and login foundations;
- standard chess game lifecycle;
- legal move generation through existing chess libraries;
- board UI and live game UI;
- clocks and live round state;
- challenge, lobby, seek, and pairing foundations;
- WebSocket infrastructure through `lila-ws`;
- PGN/history/replay foundations;
- analysis/review foundations;
- MongoDB/Redis-backed local development stack;
- seeded development data through `lila-db-seed`.

### EvenChess-specific additions

EvenChess will add:

- EvenChess mode flag / assisted-mode metadata;
- Set Level;
- Used Level;
- Assistance Load;
- Used Offset;
- ECR / EvenChessRating;
- EvenChess-specific matchmaking / queue rules;
- server-authorised coaching overlays;
- Offset Count / Exchange Resolver;
- legal platform Stockfish assistance;
- distinction between legal platform help and banned outside help;
- assistance audit ledger;
- post-game AI match summaries;
- performance summaries;
- onboarding tokens;
- rewarded-ad game tokens;
- Standard/Premium subscription gates;
- blue EvenChess branding/theme;
- marketing/funnel controls where required;
- admin/operations dashboards for EvenChess-specific systems.

## 3. Non-Negotiable Local Development Invariants

These apply to all Stage 1 work and later Codex implementation:

- Lichess provides the base chess platform.
- EvenChess adds only the assisted-chess mode layer.
- Do not rebuild Lichess-provided chess basics.
- Do not replace normal Lichess chess.
- Do not scatter EvenChess logic across unrelated Lichess files.
- Prefer `evenchess`, `EvenChess`, `ECR`, `SetLevel`, `UsedLevel`, `AssistanceLoad`, `UsedOffset`, `TargetLevel`, `OffsetCount`, `CoachingOverlay`, and `AssistanceLedger` naming.
- Any required edit to upstream/core Lichess files must be recorded in the patch map.
- Client-side code must never decide coaching permission.
- Every coaching render must be server-authorised and audited.
- Stockfish for EvenChess live assistance must remain server-side and policy-gated.
- Premium/subscription must never make live rated help stronger.
- Target Level mode must not update normal ECR.
- Offset Count is the existing Exchange Resolver / take-take-take feature.

## 4. Active Local Environment

### Host

```text
Windows 11
Docker Desktop
WSL Ubuntu
```

### lila-docker wrapper path

Run Docker/Lichess lifecycle commands from:

```text
~/dev/lila-docker
```

Windows Explorer path:

```text
\\wsl$\Ubuntu\home\jayde\dev\lila-docker
```

### Active editable Lichess source repo

The actual editable EvenChess-Lichess source repository is:

```text
~/dev/lila-docker/repos/lila
```

Windows Explorer path:

```text
\\wsl$\Ubuntu\home\jayde\dev\lila-docker\repos\lila
```

This is the folder humans and Codex should treat as the active repo.

Do **not** use `C:\dev\lila-docker` as the product repo. That is a wrapper/helper location and can contain non-source setup files.

### GitHub fork and remotes

Expected branch:

```text
master
```

Expected remotes:

```text
origin   https://github.com/foughtapple/EvenChess-Lichess.git
upstream https://github.com/lichess-org/lila.git
```

Useful verification commands:

```bash
cd ~/dev/lila-docker/repos/lila

git status
git remote -v
git log --oneline -5
```

Known good state at the time of this handover:

- working tree clean;
- `origin/master` points to `foughtapple/EvenChess-Lichess`;
- `upstream/master` points to `lichess-org/lila`;
- EvenChess-Lichess requirements suite committed;
- root `AGENTS.md` committed;
- Lichess source tree preserved with top-level folders such as `app`, `conf`, `modules`, `public`, and `ui`.

## 5. Local URLs

### Required

```text
Main Lichess site: http://localhost:8080/
```

### Enabled useful local services

```text
Mongo admin:       http://localhost:8081/
Mailpit inbox:     http://localhost:8025/
Chessground demo:  http://localhost:8090/demo.html
PGN Viewer:        http://localhost:8091/
Search admin:      http://localhost:8092/
Elasticsearch:     http://localhost:9200/
Search docs:       http://localhost:9673/docs/
```

### Disabled / deferred

```text
API docs:          http://localhost:8089/
lila_fishnet:      internal service, currently under repair/deferred
```

The API docs container was disabled/deferred because it restarted with:

```text
npm Missing script: "serve"
```

The `lila_fishnet` bridge was under active repair due SBT/native-client, Java path, and Docker Compose variable interpolation issues. It is not required for basic human-vs-human local Stage 1 testing.

## 6. Active Services

Current useful service set includes:

- `lila`
- `lila_ws`
- `mongodb`
- `mongodb_secondary`
- `redis`
- `caddy`
- `mongo_express_primary`
- `mailpit`
- `chessground`
- `pgn_viewer`
- `elasticsearch`
- `elasticvue`
- `lila_search_app`
- `lila_search_ingestor`
- `fishnet_play`
- `fishnet_analysis`

Optional repos/services that may show as not active or not a git repo can be ignored unless later requirements need them:

- `bbpPairings`
- `berserk`
- `lila-engine`
- `lila-gif`
- `lila-push`
- `monitoring`
- `swiss pairings`

## 7. Running, Stopping, Restarting, and Checking the Stack

Run these commands from WSL Ubuntu.

### Start

```bash
cd ~/dev/lila-docker

./lila-docker start
```

Lila may be the last service to become ready. Cold starts can take several minutes.

### Stop safely

```bash
cd ~/dev/lila-docker

./lila-docker stop
```

Do **not** use `down` as the normal stop command. `down` is more destructive and may remove containers/volumes depending on configuration.

### Status

```bash
cd ~/dev/lila-docker

./lila-docker status

docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Logs

```bash
cd ~/dev/lila-docker

./lila-docker logs
```

Targeted logs:

```bash
docker logs --tail 150 lila-docker-lila-1
docker logs --tail 150 lila-docker-lila_ws-1
docker logs --tail 150 lila-docker-caddy-1
```

### Restart only Lila

Use after backend/Scala changes:

```bash
cd ~/dev/lila-docker

./lila-docker lila restart
```

### Build frontend assets

Use after TypeScript/SCSS/UI changes:

```bash
cd ~/dev/lila-docker

./lila-docker ui
```

Watch mode:

```bash
cd ~/dev/lila-docker

./lila-docker ui --watch
```

### Route refresh

Use after editing `conf/routes`:

```bash
cd ~/dev/lila-docker

docker compose exec lila ./lila.sh playRoutes
```

### Translation key refresh

Use after editing translation XML files:

```bash
cd ~/dev/lila-docker

docker compose run --rm -w /lila ui pnpm run i18n-file-gen
```

### Database reseed

Use deliberately only. It resets local data.

```bash
cd ~/dev/lila-docker

./lila-docker db
```

## 8. Seeded Accounts and Passwords

The local database was seeded through the lila-docker/lila-db-seed flow.

Local test password configured during setup:

```text
password
```

Use this for seeded users unless the database is reseeded with different values.

### Useful seeded account classes

Privileged / admin-style users:

```text
superadmin
admin
shusher
hunter
puzzler
api
```

Regular test users mentioned during setup include:

```text
ana
luis
yunel
veer
yarah
emmanuel
lola
yulia
angel
elena
```

Bot accounts include:

```text
bot0
bot1
bot2
...
bot9
```

Marked / edge-case accounts include examples such as:

```text
troll
kid
wwwwwwwwwwwwwwwwwwww
```

Do not treat the `lichess` system account as a normal login target.

### Secret handling rule

Do not commit real API keys, real OAuth secrets, real payment secrets, or real production passwords.

If local tokens or secrets are generated later, store them in an untracked local file such as:

```text
.env.local
```

or a local state folder outside the repo, not in tracked Markdown.

## 9. Local Testing Available Now

The current environment is good for:

- homepage and routing checks;
- seeded account login/logout checks;
- normal Lichess navigation;
- human-vs-human local game testing;
- board UI testing;
- WebSocket/live move propagation testing;
- PGN Viewer checks;
- Chessground demo checks;
- MongoDB inspection through Mongo admin;
- Mailpit email capture checks;
- Elasticsearch/search service inspection;
- Git/Codex source editing against `repos/lila`.

The current environment is **not yet fully verified** for:

- play-against-computer move generation;
- `lila_fishnet` bridge stability;
- API docs browser UI;
- production-like monitoring;
- production deployment;
- real billing/payment;
- real AI API integration;
- public hosting.

## 10. Known Current Caveats

### Computer opponent

“Play against computer” may not move until the `lila_fishnet` bridge is healthy.

Observed issues during local repair:

- initial SBT native-client `sbtn` archive problem;
- then Java path / shell interpolation issues in `compose.override.yml`;
- Docker Compose required `$$` escaping for container-side shell variables;
- fishnet play/analysis worker containers can run while the `lila_fishnet` bridge itself still fails.

Stage 1 can proceed with human-vs-human testing, but any work touching engine play or legal platform Stockfish help should treat `lila_fishnet` as a known local setup item to verify or repair.

### API docs

API docs were deferred because the container restarted with a missing `serve` script. This does not block core Stage 1.

### Upstream drift

`lila-docker status` may report that local `lila` is behind upstream. Do not sync upstream casually. Upstream sync should happen only through the documented process after the patch-map discipline is in place.

## 11. One-Click Helper Script Requirements

The repo should eventually include local helper scripts. They must be thin wrappers around official lila-docker commands, not a second orchestration system.

Recommended files:

```text
scripts/evenchess-local-start.sh
scripts/evenchess-local-status.sh
scripts/evenchess-local-stop.sh
```

Optional Windows wrappers may be added later:

```text
scripts/evenchess-local-start.ps1
scripts/evenchess-local-status.ps1
scripts/evenchess-local-stop.ps1
```

PowerShell wrappers should delegate into WSL and should not duplicate Bash logic.

### Start script requirements

`scripts/evenchess-local-start.sh` should:

1. resolve `LILA_DOCKER_ROOT`, defaulting to `~/dev/lila-docker`;
2. resolve `LILA_REPO`, defaulting to `~/dev/lila-docker/repos/lila`;
3. print timestamp;
4. print paths;
5. print Git branch/SHA/remotes;
6. run `./lila-docker start`;
7. print `docker ps`;
8. check `http://localhost:8080/`;
9. print useful local URLs;
10. exit non-zero if the main site is unreachable.

It must not call:

- `down`;
- `db`;
- `build`;
- `add-services`;
- destructive cleanup.

### Status script requirements

`scripts/evenchess-local-status.sh` should:

1. print timestamp;
2. print resolved paths;
3. print Git branch/SHA/remotes/status;
4. run `./lila-docker status`;
5. run `docker ps`;
6. curl-check local URLs;
7. report enabled, deferred, and failing services;
8. exit non-zero if the main site is unreachable or required core containers are down.

### Stop script requirements

`scripts/evenchess-local-stop.sh` should:

1. print timestamp;
2. print resolved paths;
3. call `./lila-docker stop`;
4. print post-stop `docker ps`;
5. warn that `down` is destructive and intentionally not invoked.

### Local state/logging

Helper scripts may write latest status output to:

```text
~/.local/state/evenchess-lichess/
```

Do not write noisy runtime logs into the Git working tree.

## 12. Stage 1 Smoke Tests

Before any EvenChess product code is implemented, this local baseline should be recorded.

### Required smoke test checklist

| Test | Required result |
|---|---|
| `./lila-docker status` runs | Pass |
| Docker containers running | Pass |
| Main site loads at `http://localhost:8080/` | Pass |
| Login works with a seeded user | Pass |
| Logout works | Pass |
| Login works with a second seeded user | Pass |
| Human-vs-human local game can be opened or started | Pass |
| Moves propagate between two sessions | Pass |
| Clocks/turn state behave normally | Pass |
| Game completion/replay/analysis page opens | Pass or known caveat |
| Mongo admin reachable | Pass if enabled |
| Mailpit reachable | Pass if enabled |
| Chessground demo reachable | Pass if enabled |
| PGN Viewer reachable | Pass if enabled |
| Search admin reachable | Pass if enabled |
| Git working tree clean before Codex edits | Pass |

### Recommended browser test flow

1. Open `http://localhost:8080/`.
2. Log in as `ana / password`.
3. Open an incognito/private browser.
4. Log in as `luis / password`.
5. Create or join a casual game.
6. Make moves from both sessions.
7. Finish or resign the game.
8. Open the game/replay/analysis page.

### Computer opponent caveat

If play-against-computer does not move, record:

```text
Computer opponent deferred due local lila_fishnet bridge issue.
Human-vs-human live play remains the Stage 1 baseline.
```

## 13. Codex Local Testing Workflow

Codex should not begin by coding features.

For read-only preparation, Codex should read:

```text
AGENTS.md
docs/requirements/EVENCHESS_LICHESS_REQUIREMENTS_MAIN.md
docs/requirements/EVENCHESS_LICHESS_STAGE_1_LOCAL_HANDOVER.md
docs/requirements/EVENCHESS_LICHESS_REQUIREMENTS_DIFF.md
docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md
docs/requirements/EVENCHESS_UPSTREAM_SYNC_PROCESS.md
```

Then inspect relevant appendices under:

```text
docs/requirements/APPENDIX_*.md
```

Before any implementation, Codex should:

1. identify the requirement;
2. identify whether Lichess already provides the platform feature;
3. classify the work as:
   - Lichess-provided;
   - EvenChess-specific;
   - adapted to the Lichess fork;
   - superseded by the fork architecture;
   - unresolved / needs product-owner decision;
4. inspect the current Git status;
5. avoid broad rewrites;
6. use the patch map for any core Lichess file edits.

## 14. Keeping EvenChess Update-Safe Against Upstream Lichess

### Default rule

Add new EvenChess code in separated, namespaced locations where possible.

Preferred future locations:

```text
modules/evenchess/
ui/evenchess/
app/views/evenchess/
public/evenchess/
docs/evenchess/
```

### Allowed integration seams

Core Lichess files should only be touched when necessary to expose EvenChess through existing platform flows.

Likely seams include:

```text
conf/routes
app/controllers/Setup.scala
app/controllers/Challenge.scala
app/controllers/Lobby.scala
app/controllers/Round.scala
app/controllers/PlayApi.scala
app/controllers/Analyse.scala
app/controllers/Plan.scala
app/controllers/Pref.scala
app/controllers/Dasher.scala
ui/lobby/
ui/challenge/
ui/round/
ui/analyse/
ui/user/
ui/site/
```

### Avoid unless explicitly approved

Do not modify these areas casually:

```text
scalachess
chessground
pgn-viewer
lila-ws
lila-search
lila-fishnet
fishnet
global rating/perf internals
core game BSON/schema internals
```

If one of these must be changed, create/update an explicit decision record and patch-map entry.

## 15. Patch-Map Process

Canonical patch map:

```text
docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md
```

Temporary / operational notes may also exist under:

```text
docs/evenchess
```

Every edit to upstream/core Lichess files must record:

- date;
- task or requirement reference;
- repo and branch;
- base upstream SHA;
- fork SHA;
- file touched;
- change category;
- reason for change;
- why a core file had to be touched;
- whether it can later be isolated;
- upstream merge risk: Low / Medium / High;
- tests added or updated;
- commands run;
- rollback note.

No patch-map entry is needed for pure documentation additions under `docs/requirements`, unless they change implementation policy.

## 16. Distinguishing Lichess-Provided Features From EvenChess-Specific Work

Codex and human implementers must classify each requirement before editing.

### Lichess-provided, do not rebuild

- normal chess movement rules;
- legal move generation;
- base board UI;
- base game rooms;
- clocks;
- base user accounts;
- login/logout;
- normal game lifecycle;
- ordinary PGN/history;
- ordinary analysis page;
- ordinary WebSocket plumbing;
- ordinary lobby/challenge/pairing primitives.

### EvenChess-specific, implement in the fork

- EvenChess mode metadata;
- disclosed assisted-game entry points;
- Set Level;
- Used Level;
- Assistance Load;
- Used Offset;
- ECR;
- assistance ledger;
- server-authorised coaching payloads;
- coaching overlays;
- legal platform Stockfish policy;
- Offset Count / Exchange Resolver presentation;
- Target Level mode;
- EvenChess-specific matchmaking rules;
- summary quotas and AI review outputs;
- tokens/subscription gates;
- marketing/launch controls;
- admin dashboards for EvenChess systems.

### Adapted to Lichess architecture

These areas use Lichess foundations but need EvenChess-specific extensions:

- lobby and challenge creation;
- round-page UI;
- WebSocket round payloads;
- post-game analysis/review surfaces;
- rating settlement;
- engine analysis path;
- preferences/settings;
- plan/subscription surfaces.

## 17. Stage 1 Acceptance Criteria

Stage 1 local baseline is accepted when:

- Docker Desktop works;
- WSL Ubuntu works;
- lila-docker advanced/base editable setup works;
- active source repo exists at `~/dev/lila-docker/repos/lila`;
- local source repo remotes are correct;
- Git working tree is clean;
- requirements suite exists under `docs/requirements`;
- `AGENTS.md` exists at repo root;
- main site loads at `http://localhost:8080/`;
- seeded user login works;
- human-vs-human local game path works or an explicit blocker is recorded;
- local smoke result is recorded;
- known deferred services are documented;
- Codex has a read-only preparation prompt;
- no EvenChess implementation begins before requirements are read.

## 18. Stage 1 Go / No-Go Criteria

### Go for first implementation phase

Proceed only if:

- repo is clean;
- local site loads;
- seeded users can log in;
- human-vs-human game flow works or the exact blocker is documented;
- Codex has read the requirements and Stage 1 handover;
- patch-map process is understood;
- first implementation phase is narrow and explicit.

### No-Go / pause

Pause if:

- active repo path is uncertain;
- Codex is pointed at the wrong folder/repo;
- Git remotes are wrong;
- working tree is dirty with unknown changes;
- main site does not load;
- login does not work;
- requirements suite is missing;
- Codex has not read the requirements;
- the requested implementation would modify high-risk core files without explicit approval;
- a requirement conflicts with a non-negotiable invariant.

## 19. Companion Notes for Codex

Codex should treat this document as the local operations and testing handover.

It should treat the Version 1 requirements suite as the product authority.

When in doubt:

1. read the main requirements document;
2. read the relevant appendix;
3. inspect the current local status;
4. classify the requirement;
5. ask if uncertain;
6. do not make broad changes;
7. update the patch map if core Lichess files are touched.
