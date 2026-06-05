# EvenChess-Lichess Codex Instructions

This repo is an EvenChess fork of Lichess/Lila.

Lichess provides the base chess platform. EvenChess adds only the assisted-chess mode layer and related business/operations systems.

## Primary rule

Keep EvenChess-specific code separated, namespaced, documented, and easy to review against future upstream Lichess updates.

## Before making code changes

1. Read docs/evenchess/EVENCHESS_LICHESS_VERSION_1_2_PLAN.md if present and the task mentions Version 1.2.
2. Read docs/evenchess/EVENCHESS_LICHESS_VERSION_1_PLAN.md if present.
3. Read docs/evenchess/EVENCHESS_LICHESS_REQUIREMENTS_DIFF.md if present.
4. Read docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md if present.
5. Read docs/evenchess/EVENCHESS_UPSTREAM_SYNC_PROCESS.md if present.
6. Read relevant files in docs/requirements only as source/reference requirements.
7. Do not blindly implement old custom-platform requirements. First decide whether Lichess already provides that platform feature.

## EvenChess invariants

- EvenChess is a disclosed assisted chess variant.
- Platform coaching is legal only because it is disclosed, capped by Set Level, logged by the server, and priced into ECR.
- Non-platform guidance remains prohibited in rated EvenChess.
- Client-side code must never decide coaching permission.
- Server authority is required for Set Level, Used Level, Assistance Load, Used Offset, ECR, and coaching renders.
- Used Level never decreases.
- Every coaching render must be audited.
- Marketing, subscriptions, ads, tokens, and landing variants must not alter rated fairness.
- Premium must never be stronger live help.
- Target Level mode must not update normal ECR.
- Offset Count is the existing Exchange Resolver / take-take-take feature.

## Lichess fork rules

- Preserve normal Lichess chess behaviour unless explicitly changed.
- Prefer adding EvenChess as a separate mode/layer rather than modifying core chess rules.
- Do not scatter EvenChess logic across unrelated Lichess core files.
- Any required edit to upstream Lichess core files must be recorded in docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md.
- Keep upstream update compatibility in mind.
- Do not implement future phases unless explicitly asked.
- Add tests for every implementation change.

## Completion reports must include

- Summary
- Requirements Used
- Requirements Updated
- Files Changed
- Patch Map Updates
- Tests Added or Updated
- Tests Run
- Results
- Incomplete Items
- Risks / Follow-Ups
- Ready for Next Step?
