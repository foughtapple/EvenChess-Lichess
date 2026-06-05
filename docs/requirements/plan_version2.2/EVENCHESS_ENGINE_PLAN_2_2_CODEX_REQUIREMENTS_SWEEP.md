# EvenChessEngine Plan 2.2 — Codex Requirements Sweep and Hardening Plan

**Plan name:** EvenChessEngine Plan 2.2
**Purpose:** Have Codex sweep, verify, test, and harden the ECE implementation that was created by AI Buddy against the revised ECE requirements.
**Target code repo:** `foughtapple/EvenChessEngine`
**Local ECE repo path:** `C:\Users\jayde\Documents\Chess apps\EvenChessEngine`
**Plan storage path in EvenChess-Lichess repo:** `docs/requirements/plan_version2.2/EVENCHESS_ENGINE_PLAN_2_2_CODEX_REQUIREMENTS_SWEEP.md`
**Primary ECE requirements source:** `docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md`
**Previous implementation plan:** `docs/requirements/plan_version2.1/EVENCHESS_ENGINE_CHANGED_REQUIREMENTS_IMPLEMENTATION_PLAN.md`

---

## 1. Why this plan exists

AI Buddy implemented a broad set of ECE changes using the revised requirements. AI Buddy used GPT-5.5 but may have made mistakes, skipped edge cases, misunderstood requirements, or introduced code drift.

Codex must now perform a **phase-by-phase verification and remediation sweep**.

This is not a new feature roadmap. This is a **requirements conformance audit and correction plan**.

For each phase, Codex must:

1. read the relevant ECE requirements;
2. inspect the current code;
3. compare code behavior against requirements;
4. classify each area as compliant, partially compliant, missing, risky, or contradictory;
5. fix only the current phase scope;
6. add or update tests;
7. run relevant tests;
8. produce a completion report;
9. stop for user review before the next phase.

---

## 2. Repos and source-of-truth

There are two related repos:

### 2.1 EvenChessEngine repo

This is the code repo to inspect and fix.

```text
C:\Users\jayde\Documents\Chess apps\EvenChessEngine
```

ECE is a separate private backend service. It must remain separate from the open-source EvenChess-Lichess fork.

### 2.2 EvenChess-Lichess repo

This stores the planning and integration requirements.

```text
\\wsl$\Ubuntu\home\jayde\dev\lila-docker\repos\lila
```

Plan 2.2 should be placed here:

```text
\\wsl$\Ubuntu\home\jayde\dev\lila-docker\repos\lila\docs\requirements\plan_version2.2\EVENCHESS_ENGINE_PLAN_2_2_CODEX_REQUIREMENTS_SWEEP.md
```

### 2.3 Primary requirements

Codex must use this file as the ECE requirements authority:

```text
docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md
```

If this combined requirements file is not present in the EvenChessEngine repo, Codex must stop and ask the user to copy it there before deep implementation sweeps.

Do not use older one-off ECE notes as authority unless they are explicitly incorporated into the combined requirements file.

---

## 3. Non-negotiable ECE rules

Codex must preserve these rules throughout the sweep:

- ECE is a separate private backend service.
- Browser/client code must not call ECE directly.
- ECE must not expose API keys, provider secrets, filesystem paths, raw prompts, or raw unrestricted engine output.
- ECE v1 board-state mode is timeless.
- FEN determines side to move.
- Explicit level `0` is real L0 / no coaching.
- Missing, null, or omitted levels default to `10`.
- Ratings are not part of the ECE v1 public request contract.
- Public board-state output must not return public `position`.
- Public board-state output must not return public `shared_calculations`.
- Public board-state output must be side-specific under `side_outputs.white` and `side_outputs.black`.
- Each side output must be independently level-gated.
- `custom.instructions` is a controlled numeric composer profile, not free-form live prompt text.
- AI never creates chess truth.
- Deterministic logic, rules provider, tablebase, Stockfish, opening book, eval cache, and other providers create/normalise chess facts.
- AI only compresses/explains approved facts where enabled.
- Persistent cache must not leak high-level facts into lower-level outputs.
- Do not commit `.env`, API keys, provider binaries, model weights, tablebases, raw downloads, generated cache databases, or local-only secrets.
- Do not use `git add .`.

---

## 4. Standard phase workflow

For every Plan 2.2 phase, Codex must do this:

1. Confirm current branch and dirty state.
2. Read the combined ECE requirements file.
3. Read this Plan 2.2 file.
4. Read the relevant source files.
5. Inspect tests before editing.
6. Write a short implementation/audit plan.
7. Make only scoped changes for the current phase.
8. Add or update tests.
9. Run tests.
10. Report exact files changed.
11. Stop.

### Required baseline commands

Run from:

```text
C:\Users\jayde\Documents\Chess apps\EvenChessEngine
```

Commands:

```powershell
git status --short
git branch --show-current
git log --oneline -5
node test\self-test.js
```

If `npm test` exists and is meaningful:

```powershell
npm test
```

ECE service smoke:

```powershell
node src\server.js
```

In another PowerShell window:

```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:8787/health
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8787/v1/ece/board -ContentType "application/json" -Body (Get-Content fixtures/ece-v1-sample-input.json -Raw)
```

---

## 5. Status classification for each phase

Codex must classify each requirement inspected as one of:

| Status | Meaning |
|---|---|
| Compliant | Code and tests satisfy the requirement. |
| Partially compliant | Some behavior exists but gaps remain. |
| Missing | Requirement not implemented. |
| Risky | Implementation may work but is fragile, leaky, or under-tested. |
| Contradictory | Code conflicts with the requirement. |
| Deferred | Requirement is explicitly later-phase and should not be implemented now. |

---

# Phase A — Baseline inventory and drift report

## Goal

Establish the current state after AI Buddy’s implementation.

## Requirements

Read:

- ECE combined requirements: main sections and Appendix Z.
- Plan 2.1.
- Current source tree.

## Inspect

- `README.md`
- `package.json`
- `.gitignore`
- `.env.example`
- `src/`
- `test/`
- `fixtures/`
- `docs/requirements/` if present
- `scripts/`
- `config/`
- `data/`
- `external_engines/`

## Actions

- Do not make code changes unless a trivial broken test path must be fixed.
- Produce a tree summary.
- Identify files AI Buddy added or changed.
- Identify obvious violations of the revised requirements.
- Identify missing expected folders.
- Confirm whether the combined ECE requirements file exists in the ECE repo.
- Confirm whether tests run.

## Tests

Run:

```powershell
git status --short
node test\self-test.js
npm test
```

If service starts:

```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:8787/health
```

## Acceptance

Codex produces a clear baseline drift report before any deeper fixes.

---

# Phase B — Public API contract compliance sweep

## Goal

Verify and fix the public `/v1/ece/board` contract.

## Requirements

Use combined requirements:

- Public Output Rule
- Appendix C — Public API Contracts
- Appendix D — Board-State Mode
- Appendix Z decisions

## Required checks

- Board-state input must not require ratings.
- Public response must not include `position`.
- Public response must not include `shared_calculations`.
- Public response must not include public composer packets.
- Public response must include:
  - `schema`
  - `request_echo`
  - `side_outputs.white`
  - `side_outputs.black`
  - `diagnostics`
  - `unavailable`
- Request echo must include enough stale-check data.
- No secrets/provider paths in response.
- Invalid FEN returns structured diagnostics.

## Likely files

- `src/server.js`
- `src/ece/ece-v1-framework.js`
- `src/ece/modes/board-state*`
- `src/ece/payload/*`
- `fixtures/board-state/*`
- `test/contracts/*`
- `test/self-test.js`

## Tests

Add/update contract tests:

- no `position`;
- no `shared_calculations`;
- no ratings in public echo;
- valid side outputs;
- invalid FEN handling;
- schema fields exist.

## Acceptance

Sample board call matches revised public contract exactly.

---

# Phase C — Request normalisation and level-gating sweep

## Goal

Verify request normalisation, L0 handling, default levels, and side-specific gating.

## Requirements

Use:

- Appendix G — Level Gating
- Appendix D — Board-State Mode
- Appendix A invariants

## Required checks

- `white_level` and `black_level` allow `0` through `10`.
- Missing/null/omitted levels default to `10`.
- Invalid levels default to `10` with diagnostics.
- Explicit `0` remains L0.
- L0 side output has no coaching.
- White and Black side outputs are gated independently.
- Non-moving side does not receive live summary/plan text.
- Lower-level side does not receive higher-level facts.

## Likely files

- `src/ece/core/level*`
- `src/ece/payload/side-output*`
- `src/ece/modes/board-state*`
- `src/ece/composer/*`
- `test/level*`
- `test/payload*`

## Tests

- white L0 / black L10.
- white L4 / black L2.
- null levels.
- missing levels.
- invalid levels.
- non-moving side text remains `0`.

## Acceptance

No high-level fact can appear in a lower-level side output.

---

# Phase D — Composer profile and deterministic text sweep

## Goal

Verify `custom.instructions` is a numeric composer profile and deterministic text works safely.

## Requirements

Use:

- Appendix D composer profile rules
- Appendix E deterministic composer facts
- Appendix G composer gating
- Appendix O AI text provider boundaries where relevant

## Required checks

- `custom.instructions` is numeric.
- Missing/null/invalid profile defaults to `0`.
- Profiles 0–5 are recognised.
- Profile affects wording only.
- Composer does not change chess truth.
- Composer does not change candidate count, eval visibility, level gates, or schema.
- Composer runs only for side to move in board-state mode.
- Composer receives only side-specific, level-gated facts.
- Composer output writes only:
  - `summary`
  - `immediate_warning`
  - `plan`
- No public composer packet leaks.

## Likely files

- `src/ece/composer/*`
- `src/ece/modes/board-state*`
- `src/ece/payload/*`
- `test/composer/*`

## Tests

- all profile IDs.
- invalid profile fallback.
- L4 summary does not mention candidate moves.
- L4 summary does not mention L8 eval.
- non-moving side no summary/plan.
- validation rejects unsupported claims.

## Acceptance

Deterministic composer is safe, bounded, and level-gated.

---

# Phase E — Payload overlay contract sweep

## Goal

Verify overlay field names and level gates align with the changed requirements.

## Requirements

Use:

- Appendix F — Overlays and Side Payloads
- Appendix G — Level Gating
- Appendix Z decisions

## Required checks

- There is no `hanging_attackable` field.
- `hanging_not_attackable` exists and is L2+.
- Attackable/contested pieces go through `offset_count`.
- `offset_count` exists and is L3+.
- `advantage_offset_value` and `disadvantage_offset_value` exist and are L3+.
- Threats are L4+.
- Pinned pieces are L4+.
- Side mapping is correct:
  - White payload: White threats = student, Black threats = opponent.
  - Black payload: Black threats = student, White threats = opponent.

## Likely files

- `src/ece/payload/side-output*`
- `src/ece/analysis/*`
- `fixtures/*`
- `test/payload/*`
- `test/analysis/*`

## Tests

- field absence/presence tests.
- side-mapping tests.
- level-gate tests.
- no cross-side leakage tests.

## Acceptance

Payload overlays match the revised side-output contract.

---

# Phase F — Deterministic chess core sweep

## Goal

Verify the internal board model and deterministic calculations are correct enough to support overlays/composer.

## Requirements

Use:

- Appendix E — Deterministic Chess Core
- Appendix M — Rules / Legal Move Provider
- Appendix F overlays

## Required checks

- FEN parser is robust.
- Internal board model exists.
- No public board model leaks.
- Legal/rules source exists or is properly abstracted.
- Attack/defence maps exist or are planned with tests.
- Pins, hanging, threats, offset, value trade modules are deterministic and source-labelled.
- Pseudo-legal vs legal distinctions are understood.
- King-safety legality is not ignored in final public claims.

## Likely files

- `src/ece/core/*`
- `src/ece/analysis/*`
- `src/ece/providers/rules-legal/*`
- `test/core/*`
- `test/analysis/*`

## Tests

Use tactical fixtures:

- pinned piece.
- loose piece.
- contested square.
- equal exchange.
- favourable exchange.
- unfavourable exchange.
- simple legal/illegal move examples.

## Acceptance

Core deterministic facts can be trusted by composer and payload builder.

---

# Phase G — Provider architecture and mocks sweep

## Goal

Verify provider registry, provider authority order, timeouts, source labels, and fallback behavior.

## Requirements

Use:

- Appendix H — Provider Architecture
- Appendix I — Stockfish Provider
- Appendix J — Syzygy
- Appendix K — Opening Book
- Appendix L — Lichess Eval Cache
- Appendix M — Rules Provider
- Appendix N — Maia
- Appendix O — AI Text

## Required checks

- Provider interfaces exist.
- Provider calls are optional and level/mode-gated.
- Provider authority order is encoded or documented in code.
- Provider timeouts exist.
- Provider results are normalised.
- Provider output is source-labelled.
- Missing providers degrade safely.
- No raw provider output leaks publicly.

## Likely files

- `src/ece/providers/*`
- `src/ece/uci/*`
- `src/ece/modes/*`
- `test/providers/*`

## Tests

For each provider mock:

- configured.
- unavailable.
- timeout.
- malformed output.
- successful normalised output.

## Acceptance

Provider layer can be tested without real binaries and degrades safely.

---

# Phase H — Lichess eval-cache before Stockfish sweep

## Goal

Verify eval-cache routing is correct and does not leak high-level data.

## Requirements

Use:

- Appendix L — Lichess Eval Cache Provider
- Appendix T — Performance, Caching, and Timeouts
- Appendix G — Level Gating

## Required checks

- Eval-cache provider exists or is explicitly deferred.
- Eval-cache is checked before live Stockfish where suitable.
- Cached eval is source-labelled `cached_lichess_eval`.
- Depth/quality/staleness checks exist or are stubbed.
- PV/eval data does not appear below allowed levels.
- Missing eval-cache falls back safely.

## Tests

- cache hit before Stockfish.
- cache miss then Stockfish.
- insufficient depth then Stockfish.
- L4 cannot see eval/PV from cache.
- L8 can see allowed eval.

## Acceptance

Eval-cache is a performance optimisation, not a leak path.

---

# Phase I — Stockfish provider sweep

## Goal

Verify real or scaffolded Stockfish integration is bounded, source-labelled, and level-gated.

## Requirements

Use:

- Appendix I — Stockfish Provider
- Appendix G — Level Gating
- Appendix H Provider Architecture

## Required checks

- Stockfish path is environment/config only.
- UCI command construction is internal, not caller-controlled.
- Candidate counts obey levels:
  - L5: 1
  - L6: 2
  - L7+: up to 3
- Eval appears only L8+.
- Time/depth/node/movetime bounds exist.
- Raw UCI logs are not public.
- Timeout degrades safely.

## Tests

- Stockfish unavailable.
- mock Stockfish candidates.
- candidate count limits.
- eval gating.
- no raw UCI output public.
- timeout fallback.

## Acceptance

Stockfish cannot bypass level gates or expose raw engine access.

---

# Phase J — Opening book provider sweep

## Goal

Verify opening guidance is deterministic, requested, and level-gated.

## Requirements

Use:

- Appendix K — Opening Book Provider
- Appendix D — Board-State Mode
- Appendix G — Level Gating

## Required checks

- `custom.opening = 0` returns `opening = 0`.
- Non-zero opening request can be handled.
- Opening output is L4+.
- AI does not invent opening facts.
- Opening provider missing degrades safely.

## Tests

- no opening request.
- requested opening at L4.
- requested opening at L2 returns 0.
- provider unavailable.

## Acceptance

Opening guidance does not produce ungrounded text.

---

# Phase K — Persistent ECE cache sweep

## Goal

Verify persistent FEN/side/level/profile cache implementation.

## Requirements

Use:

- Appendix T — Performance, Caching, and Timeouts
- Appendix D cache behavior
- Appendix A cache invariant
- Appendix G cache gating

## Required checks

- Cache is optional and safe to disable.
- Cache key includes required version/profile/provider fields.
- Side/level entries have completeness status.
- Partial side hits are supported.
- Stale entries invalidate.
- Write-through happens only after valid successful output.
- No invalid FEN write-through.
- No high-level cache leakage into lower-level output.
- Cache data files are ignored if generated.

## Tests

- miss.
- full hit.
- white-only hit.
- black-only hit.
- stale version.
- template version.
- provider config hash.
- cache unavailable fallback.
- L0 cache marker/derived output.

## Acceptance

Cache improves performance without changing correctness or level safety.

---

# Phase L — Proposed-move mode sweep

## Goal

Verify proposed-move mode if AI Buddy implemented it; otherwise ensure it is correctly deferred.

## Requirements

Use:

- Appendix P — Proposed Move Mode
- Appendix M — Rules Provider
- Appendix G — Level Gating

## Required checks

- Endpoint exists or is marked deferred.
- FEN validated.
- Proposed move legality validated before eval.
- Illegal moves do not call normal eval path.
- New FEN generated for legal moves.
- Level gating based on moving side.
- No rating fields.
- Deterministic sentence by default.
- Provider use gated.

## Tests

- legal move.
- illegal move.
- wrong side.
- castling.
- en passant.
- promotion.
- level-gated output.

## Acceptance

Proposed-move mode is safe for EDE single-arrow workflow.

---

# Phase M — Full-game mode / ECM1 sweep

## Goal

Verify full-game mode if implemented; otherwise ensure it is correctly deferred and scaffolded.

## Requirements

Use:

- Appendix Q — Full Game Mode and Format
- Appendix T performance
- Appendix O AI rules
- Appendix D board-state pipeline

## Required checks

- Endpoint exists or is deferred.
- Input can reconstruct game from PGN/moves/FEN history.
- Output uses compact ECM1 or documented version.
- Review is deterministic-first.
- AI optional.
- Full-game is level-flexible, not forced L10.
- Full-game does not alter live state.
- Reuses board-state/cache where safe.
- Key moments and summaries are deterministic or validated.

## Tests

- PGN reconstruction.
- move-list reconstruction.
- invalid game.
- level-flexible output.
- no AI required.
- compact shape.

## Acceptance

Full-game mode is useful post-game and does not affect live fairness.

---

# Phase N — AI text provider sweep

## Goal

Verify optional AI text integration cannot invent truth or leak secrets.

## Requirements

Use:

- Appendix O — AI Text Provider
- Appendix S — Security
- Appendix G — Level Gating
- Appendix E Composer

## Required checks

- API key comes from env/config only.
- No API key in request.
- No API key in response/logs.
- AI runs after deterministic facts/text.
- Board-state max one AI call.
- AI only for side to move.
- AI output validator exists.
- Deterministic fallback exists.
- Custom instructions remain numeric composer profile.
- AI cannot mention hidden higher-level facts.

## Tests

- `use_ai=0`.
- `use_ai=1` missing key.
- mocked valid AI.
- mocked invalid AI.
- no-secret output.
- no higher-level leak in AI text.

## Acceptance

AI is polish/compression only, never chess truth.

---

# Phase O — Diagnostics, logging, and unavailable states sweep

## Goal

Verify errors are structured and safe.

## Requirements

Use:

- Appendix R — Diagnostics, Logging, and Errors
- Appendix C common API contracts

## Required checks

- Every response has diagnostics.
- Status codes are documented.
- Provider/cache unavailable states are populated.
- Invalid FEN and invalid request are distinct.
- Logs do not expose secrets.
- Diagnostics are safe or labelled backend-only.
- Composer validation failure falls back safely.

## Tests

- invalid FEN.
- invalid request.
- provider unavailable.
- cache unavailable.
- composer validation failed.
- internal error path where practical.

## Acceptance

Failures are debuggable without leaking secrets.

---

# Phase P — Security, secrets, and ignored local data sweep

## Goal

Verify repo cannot accidentally commit secrets or large provider data.

## Requirements

Use:

- Appendix S — Security, Secrets, and Licensing
- Appendix V local development
- Appendix B repo layout

## Required checks

- `.gitignore` covers:
  - `.env`
  - provider binaries
  - model weights
  - Syzygy tablebases
  - raw downloads
  - generated cache DBs
  - logs/tmp
- No secrets in files.
- No provider paths in public payload.
- No raw AI prompts/responses in fixtures.
- License docs exist or are marked TODO before provider distribution.

## Tests

- grep no obvious key names/secrets in tracked files.
- no generated DBs tracked.
- output no provider paths.

## Acceptance

Repo is safe to push publicly/private without secrets or huge local data.

---

# Phase Q — Local development scripts and service lifecycle sweep

## Goal

Verify ECE is easy to run locally and compatible with EvenChess-Lichess Test Ground.

## Requirements

Use:

- Appendix V — Local Development and Deployment
- Appendix W — EvenChess-Lichess Adapter

## Required checks

- `node src/server.js` works.
- `/health` works.
- sample board call works.
- scripts exist or are deferred:
  - run local
  - smoke health
  - smoke board-state
  - validate providers
- ECE can run without providers in development/mock mode.
- EvenChess-Lichess launcher can start/stop/check ECE separately.

## Tests

- self-test.
- health smoke.
- board-state smoke.
- optional integration with Test Ground if in scope.

## Acceptance

The service is practical for the user to launch while testing the site.

---

# Phase R — Documentation drift and requirements traceability sweep

## Goal

Ensure docs match code after AI Buddy implementation.

## Requirements

Use:

- Appendix Y — Requirements Traceability
- Appendix Z — Decisions and Superseded Register
- README/API docs
- Plan 2.1 and Plan 2.2

## Required checks

- README describes current service accurately.
- API doc matches actual endpoints.
- Requirements combined file exists.
- Phase status is documented.
- Superseded old assumptions are not reintroduced.
- Tests map to requirements where practical.

## Acceptance

Docs, tests, and code tell the same story.

---

# Phase S — Performance and scalability sweep

## Goal

Verify the implementation will not do unnecessary work in live mode.

## Requirements

Use:

- Appendix T — Performance, Caching, and Timeouts
- Provider appendices
- Cache requirements

## Required checks

- Modules run only when level/mode requires them.
- Providers have timeouts.
- AI not required for live summary/plan.
- Stockfish calls bounded.
- Cache checked before heavy providers.
- Identical in-flight requests can be coalesced or are marked TODO.
- Full-game mode avoids unbounded provider calls per ply.

## Tests

- module execution planning.
- provider timeout.
- cache before provider.
- no AI for `use_ai=0`.
- no Stockfish for low levels where not required.

## Acceptance

ECE live path is efficient and bounded.

---

# Phase T — Final end-to-end regression and readiness report

## Goal

Perform a final verification of the AI Buddy implementation after all sweeps.

## Commands

Run:

```powershell
git status --short
node test\self-test.js
npm test
node src\server.js
```

In another terminal:

```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:8787/health
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8787/v1/ece/board -ContentType "application/json" -Body (Get-Content fixtures/ece-v1-sample-input.json -Raw)
```

## Final checks

- no public `position`;
- no public `shared_calculations`;
- no public ratings in v1 request/echo;
- no `hanging_attackable`;
- no secrets;
- no raw provider output;
- levels safe;
- composer safe;
- cache safe;
- provider failures safe;
- docs current.

## Output

Codex must produce:

- final compliance table;
- remaining gaps;
- deferred provider/mode list;
- exact tests run;
- readiness statement.

## Acceptance

ECE is ready for EvenChess-Lichess to call during integration testing.

---

# Standard Codex prompt for a Plan 2.2 phase

```text
Implement EvenChessEngine Plan 2.2 Phase [LETTER] only: [PHASE NAME].

Repo:
C:\Users\jayde\Documents\Chess apps\EvenChessEngine

Before editing, read:
1. docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md
2. docs/requirements/plan_version2.2/EVENCHESS_ENGINE_PLAN_2_2_CODEX_REQUIREMENTS_SWEEP.md if present in this repo, otherwise use the copy in EvenChess-Lichess
3. README.md
4. package.json
5. relevant source and test files for this phase

This is a requirements conformance sweep after AI Buddy implemented broad changes.
Do not implement future phases.
Do not make broad unrelated rewrites.
Do not use git add .
Do not commit external engines, model files, tablebases, raw downloads, generated cache DBs, .env, API keys, or secrets.
Keep ECE as a separate private backend service.
Preserve /health and /v1/ece/board unless this phase explicitly changes the contract.
Classify each inspected requirement as compliant, partially compliant, missing, risky, contradictory, or deferred.
Fix only issues within this phase scope.
Add or update tests.
Run node test/self-test.js and relevant added tests.
Stop after this phase and provide a completion report.

Completion report:
# Completion Report
## Phase
## Summary
## Requirements Used
## Compliance Findings
## Files Inspected
## Files Changed
## Tests Added or Updated
## Tests Run
## Results
## Contract Changes
## Incomplete Items
## Risks / Follow-Ups
## Ready for Review?
```
