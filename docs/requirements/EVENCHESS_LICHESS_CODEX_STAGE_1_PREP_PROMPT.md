# Codex Project Instruction Prompt — EvenChess-Lichess Stage 1

Paste this into Codex project chat or project instructions after the Stage 1 handover and requirements suite are committed.

```text
You are working in the `foughtapple/EvenChess-Lichess` repository.

This is a Lichess/Lila fork. The active implementation model is:

Lichess provides the base chess platform.
EvenChess adds only the assisted-chess mode layer.

Before making any code changes, always read:

1. `AGENTS.md`
2. `docs/requirements/EVENCHESS_LICHESS_REQUIREMENTS_MAIN.md`
3. `docs/requirements/EVENCHESS_LICHESS_STAGE_1_LOCAL_HANDOVER.md`
4. `docs/requirements/EVENCHESS_LICHESS_REQUIREMENTS_DIFF.md`
5. `docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md`
6. `docs/requirements/EVENCHESS_UPSTREAM_SYNC_PROCESS.md`
7. The relevant `docs/requirements/APPENDIX_*.md` files for the requested work.

If `.docx` and `.md` versions both exist, prefer `.md` for implementation reading unless the `.docx` is explicitly newer.

Core rules:

- Do not blindly implement old custom-platform requirements.
- First classify each requirement as:
  - Lichess-provided;
  - EvenChess-specific;
  - adapted to the Lichess fork;
  - superseded by the Lichess fork;
  - unresolved / needs product-owner decision.
- Do not rebuild Lichess-provided chess basics:
  - legal move generation;
  - base board UI;
  - base game rooms;
  - clocks;
  - PGN/history;
  - normal account basics;
  - normal game lifecycle.
- Preserve normal Lichess chess behaviour unless explicitly told otherwise.
- Add EvenChess as a separate assisted mode/layer.
- Keep EvenChess code clearly namespaced and separated.
- Prefer names such as `evenchess`, `EvenChess`, `ECR`, `SetLevel`, `UsedLevel`, `AssistanceLoad`, `UsedOffset`, `TargetLevel`, `OffsetCount`, `CoachingOverlay`, and `AssistanceLedger`.
- Any required edit to upstream/core Lichess files must be recorded in `docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md`.
- Every patch-map entry must include:
  - file touched;
  - reason;
  - linked EvenChess requirement;
  - upstream merge risk;
  - tests added/updated;
  - whether the change can later be isolated.
- Implement one phase at a time.
- Do not skip ahead to future phases.
- Do not make broad unrelated rewrites.
- If requirements conflict, stop and report the contradiction.
- If implementation would violate an invariant, stop and report.

EvenChess invariants:

- EvenChess is a disclosed assisted chess variant.
- Platform coaching is legal because it is disclosed, capped by Set Level, logged by the server, and priced into ECR.
- Non-platform guidance remains prohibited in rated EvenChess.
- Client-side code must never decide coaching permission.
- Server authority is required for Set Level, Used Level, Assistance Load, Used Offset, ECR, and coaching renders.
- Used Level never decreases.
- Every coaching render must be audited.
- Stockfish remains server-side only for legal EvenChess assistance.
- AI explains and compresses; hard-coded logic and Stockfish provide chess truth.
- Marketing, subscriptions, ads, tokens, and landing variants must not alter rated fairness.
- Premium must never be stronger live help.
- Target Level mode must not update normal ECR.
- Offset Count is the existing Exchange Resolver / take-take-take feature.

Local environment expectations:

- lila-docker path: `~/dev/lila-docker`
- editable source repo path: `~/dev/lila-docker/repos/lila`
- local site: `http://localhost:8080/`
- GitHub origin: `https://github.com/foughtapple/EvenChess-Lichess.git`
- upstream Lichess: `https://github.com/lichess-org/lila.git`

For local testing, use `docs/requirements/EVENCHESS_LICHESS_STAGE_1_LOCAL_HANDOVER.md`.

Do not implement anything until I give you an explicit phase prompt.

For this preparation task only:

1. Read the files listed above.
2. Inspect the repository structure.
3. Identify the appendices relevant to Stage 1 work.
4. Do not edit files.
5. Do not commit.

Respond with:

# EvenChess-Lichess Preparation Report

## Documents Read

## Documents Missing

## Requirements Structure Understood

## Appendix Map

## Stage 1 Local Environment Understood

## Lichess-Provided Capabilities Not To Rebuild

## EvenChess-Specific Systems To Implement Later

## Patch Map / Upstream Sync Rules Understood

## Local Test Commands To Use

## High-Risk Implementation Areas

## Questions / Ambiguities

## Ready for First Implementation Phase?
```
