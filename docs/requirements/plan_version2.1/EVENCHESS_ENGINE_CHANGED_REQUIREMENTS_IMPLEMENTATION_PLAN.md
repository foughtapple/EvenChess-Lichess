# EvenChessEngine — Delta Implementation Plan for Updated ECE Requirements

**Purpose:** Implement the changes introduced by the revised ECE requirements after the initial scaffold was created.
**Repo:** `foughtapple/EvenChessEngine`
**Local path:** `C:\Users\jayde\Documents\Chess apps\EvenChessEngine`
**Current state:** A draft Node.js ECE service scaffold exists and responds at `http://127.0.0.1:8787`, but it was built from earlier requirements.
**Target:** Bring the scaffold into alignment with `docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md`.

This is not a full product roadmap. This is a **delta implementation plan** for changing the existing scaffold to match the revised ECE contract.

---

## Current scaffold assumptions that must change

The existing scaffold was useful for proving the service can run, but several contract details are now outdated.

| Current scaffold behavior | Required new behavior |
|---|---|
| Board-state request includes `rating_type`, `white_rating_input`, and `black_rating_input`. | Remove ratings from the ECE v1 public board-state request contract. Ratings are not ECE v1 input. |
| Public response includes `position`. | Public board-state response must not return `position`. Use request echo and caller-side FEN tracking for stale checks. |
| Public response includes `shared_calculations`. | Public board-state response must not return `shared_calculations`; shared calculations are internal only. |
| Overlay output includes `hanging_attackable`. | Remove `hanging_attackable`; attackable/contested pieces are represented through `offset_count`. |
| `custom.instructions` may be treated as generic instructions. | `custom.instructions` must be a controlled numeric composer profile. |
| Mock summary/plan is written directly. | Add deterministic composer pipeline: facts → ranking → intents → templates → validated text. |
| No persistent cache. | Add persistent FEN/side/level/profile cache with completeness/version checks. |
| Provider placeholders are very broad. | Add explicit provider registry structure, source labels, timeouts, and authority order. |
| One file contains most ECE logic. | Move toward modular structure while keeping compatibility entrypoint. |

---

## Implementation principle

Do not try to implement every provider immediately.

The correct order is:

1. Make the **public contract correct**.
2. Make the **payload no-leak and level-gated**.
3. Make the **repo/module structure match the new architecture**.
4. Add the **deterministic composer**.
5. Add the **persistent cache**.
6. Add **providers one at a time** behind mocks.
7. Add proposed-move and full-game modes after board-state is stable.

---

# Phase 0 — Baseline and safety check

## Goal

Confirm the current scaffold state before changing it.

## Actions

Run:

```powershell
cd "C:\Users\jayde\Documents\Chess apps\EvenChessEngine"

git status --short
node test\self-test.js
node src\server.js
```

In another terminal:

```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:8787/health
```

## Output

Record:

- current branch;
- current commit;
- current passing tests;
- current endpoint behavior;
- known differences from the revised requirements.

## Acceptance

Current scaffold is working before contract changes begin.

---

# Phase 1 — Update public API contract to revised ECE v1

## Goal

Make `/v1/ece/board` match the revised public API contract.

## Required changes

### Input contract

Remove public rating fields from the accepted/echoed v1 board-state contract:

- remove `rating_type`;
- remove `white_rating_input`;
- remove `black_rating_input`.

Keep:

- `request.mode`;
- `request.request_id`;
- `request.input_fen`;
- `request.white_level`;
- `request.black_level`;
- `request.use_ai`;
- `request.custom.opening`;
- `request.custom.instructions`.

### Output contract

Remove public:

- `position`;
- `shared_calculations`.

Return only:

```json
{
  "schema": {},
  "request_echo": {},
  "side_outputs": {
    "white": {},
    "black": {}
  },
  "diagnostics": {},
  "unavailable": {}
}
```

### Request echo

Keep `request_echo.input_fen`, levels, mode, AI flag, and composer profile fields so the caller can reject stale responses.

## Files likely touched

- `src/ece/ece-v1-framework.js`
- `fixtures/ece-v1-sample-input.json`
- `test/self-test.js`
- `docs/API.md`
- `README.md`

## Tests to add/update

- ratings are ignored/rejected/absent from echo;
- public response has no `position`;
- public response has no `shared_calculations`;
- invalid FEN still returns structured diagnostics;
- request echo includes enough stale-check data.

## Acceptance

`node test\self-test.js` passes and sample output matches the revised public shape.

---

# Phase 2 — Add explicit contract/schema tests

## Goal

Prevent future drift from the revised public contract.

## Actions

Add a small contract test suite.

Suggested files:

```text
test/contracts/board-state-contract.test.js
test/contracts/no-leak-contract.test.js
```

If no test runner exists yet, keep plain Node `assert` tests and call them from `test/self-test.js`.

## Tests

Must verify:

- no public `position`;
- no public `shared_calculations`;
- no public `composer_packet`;
- no secrets/API key/provider paths;
- no rating fields in public v1 board request echo;
- side outputs exist;
- L0 output has no coaching;
- non-side-to-move has no summary/plan;
- public shape remains stable.

## Acceptance

Contract tests fail if old scaffold fields are reintroduced.

---

# Phase 3 — Normalize `custom.instructions` as composer profile

## Goal

Replace free-form instruction handling with controlled numeric composer profiles.

## Required behavior

`custom.instructions` is a numeric profile ID:

| ID | Profile |
|---:|---|
| 0 | normal |
| 1 | brief |
| 2 | detailed |
| 3 | beginner |
| 4 | direct |
| 5 | coach |

Missing/null/invalid defaults to `0`.

## Files likely touched

- `src/ece/ece-v1-framework.js`
- `src/config.js`
- `fixtures/ece-v1-sample-input.json`
- `test/self-test.js`

## New files recommended

```text
src/ece/composer/composer-profile.js
src/ece/composer/data/composer-profiles.json
```

## Tests

- missing `custom.instructions` -> profile 0;
- invalid value -> profile 0 with warning;
- valid values 0–5 accepted;
- profile affects wording style only;
- profile does not affect level gates or provider calls.

## Acceptance

Request echo shows safe profile metadata, not free-form live instructions.

---

# Phase 4 — Reorganize source structure without changing behavior

## Goal

Move from one large scaffold file toward the required module layout while preserving the existing entrypoint.

## Required structure

Create:

```text
src/ece/contracts/
src/ece/core/
src/ece/analysis/
src/ece/composer/
src/ece/modes/
src/ece/payload/
src/ece/providers/
src/ece/uci/
src/ece/full-game/
src/ece/cache/
src/ece/utils/
```

Keep:

```text
src/ece/ece-v1-framework.js
```

as a compatibility entrypoint that imports from the new modules.

## Suggested first extraction

Move functions into:

```text
src/ece/core/fen.js
src/ece/core/level.js
src/ece/modes/board-state-mode.js
src/ece/payload/side-output-builder.js
src/ece/payload/response-builder.js
src/ece/utils/hash.js
```

## Tests

Existing tests must still pass.

## Acceptance

Behavior remains unchanged after extraction.

---

# Phase 5 — Fix overlay payload semantics

## Goal

Align overlays with revised Appendix F.

## Required changes

Remove:

```text
trade_status.hanging_attackable
```

Keep:

```text
trade_status.hanging_not_attackable
trade_status.offset_count
trade_status.advantage_offset_value
trade_status.disadvantage_offset_value
threats.student_threats
threats.opponent_threats
pinned_pieces.student_pinned
pinned_pieces.opponent_pinned
```

Attackable/contested pieces must be represented through `offset_count`.

## Files likely touched

- `src/ece/payload/side-output-builder.js`
- `src/ece/analysis/*`
- `fixtures/*`
- `test/contracts/*`

## Tests

- `hanging_attackable` does not exist in output;
- `hanging_not_attackable` allowed at L2+;
- `offset_count` allowed at L3+;
- threats/pins allowed at L4+;
- side mapping works for White/Black.

## Acceptance

Overlay payload matches revised requirements and no old field remains.

---

# Phase 6 — Internal board model and deterministic core foundation

## Goal

Add internal board/position facts without exposing them publicly.

## Modules

```text
src/ece/core/board-model.js
src/ece/core/square.js
src/ece/core/piece.js
src/ece/core/side.js
src/ece/core/move.js
```

## Required internal capabilities

- FEN to board model;
- piece lists by side;
- king squares;
- square conversion helpers;
- simple internal position hash/FEN key;
- no public `position`.

## Tests

- FEN parsed into internal board model;
- side to move is derived;
- board model is not returned publicly.

## Acceptance

Board model exists internally and supports later deterministic analysis.

---

# Phase 7 — Rules/legal move provider foundation

## Goal

Create the highest-authority provider interface for legality.

## Start with

A simple internal or placeholder provider that can later be replaced/enhanced.

## Files

```text
src/ece/providers/rules-legal/rules-legal-provider.js
src/ece/providers/provider-contract.js
src/ece/providers/provider-registry.js
```

## Required capabilities

- validate FEN;
- generate legal moves later;
- validate proposed moves later;
- produce new FEN later;
- detect check/mate/stalemate later.

## Tests

- provider configured/unavailable paths;
- provider output labelled `exact_rules`;
- illegal provider state does not crash board endpoint.

## Acceptance

Legal provider boundary exists before serious overlay and proposed-move work.

---

# Phase 8 — Attack/defence, hanging-not-attackable, pins, threats, Offset Count

## Goal

Replace mock overlay generation with deterministic module interfaces, then fill real logic incrementally.

## Modules

```text
src/ece/analysis/attack-defense-map.js
src/ece/analysis/hanging-pieces.js
src/ece/analysis/pins.js
src/ece/analysis/threats.js
src/ece/analysis/offset-count.js
src/ece/analysis/value-based-trade.js
src/ece/analysis/motifs.js
```

## Implementation order

1. Attack/defence map.
2. Pin detector.
3. Hanging-not-attackable detector.
4. Trade candidates.
5. Offset Count piece-count resolver.
6. Value-based trade resolver.
7. Threat arrows.
8. Motifs/king-safety facts for composer.

## Tests

Use fixtures for:

- loose but not attackable;
- contested/attackable piece through `offset_count`;
- equal offset;
- positive student offset;
- negative student offset;
- advantage/disadvantage value offset;
- absolute pin;
- relative pin;
- student/opponent threat mapping.

## Acceptance

Core deterministic overlay facts are produced without AI.

---

# Phase 9 — Deterministic composer foundation

## Goal

Build deterministic text without AI.

## Required modules

```text
src/ece/composer/composer-orchestrator.js
src/ece/composer/composer-packet-builder.js
src/ece/composer/fact-coordinate-builder.js
src/ece/composer/fact-ranker.js
src/ece/composer/fact-grouper.js
src/ece/composer/intent-selector.js
src/ece/composer/template-registry.js
src/ece/composer/template-renderer.js
src/ece/composer/text-assembler.js
src/ece/composer/text-validator.js
```

## Required behavior

Deterministic pathway:

```text
side-specific allowed facts
→ composer packet
→ ranked facts
→ grouped facts
→ selected intents
→ templates
→ validated summary / immediate_warning / plan
```

## Text outputs

Only write into:

- `summary`;
- `immediate_warning`;
- `plan`.

Do not expose composer packets publicly.

## Tests

- composer only runs for side to move;
- composer receives only side-specific level-gated facts;
- L4 summary does not mention candidate moves;
- L4 summary does not mention eval;
- profile changes wording but not chess truth;
- unsupported fact claims are rejected;
- deterministic output is stable for the same facts/profile/template version.

## Acceptance

Live summary/plan works without AI.

---

# Phase 10 — Persistent ECE FEN/side/level/profile cache

## Goal

Add cache support after payload and composer shape are stable.

## Modules

```text
src/ece/cache/cache-key.js
src/ece/cache/ece-fen-cache.js
src/ece/cache/cache-entry.js
src/ece/cache/cache-completeness.js
src/ece/cache/cache-versioning.js
```

## Required cache key components

- mode;
- normalized FEN key;
- side;
- level;
- composer profile;
- custom opening;
- use_ai;
- engine version;
- analysis version;
- level rules version;
- provider config hash;
- template version;
- output schema version.

## Required behavior

- full two-side cache hit;
- White-only hit + Black recompute;
- Black-only hit + White recompute;
- stale version invalidation;
- no write-through after invalid FEN;
- no high-level data used for lower-level output unless fully re-gated and validated.

## Scripts

Add:

```text
scripts/seed-ece-fen-cache.ps1
scripts/inspect-ece-cache.ps1
scripts/clear-ece-cache.ps1
```

## Tests

Cache hit/miss/completeness/version/no-leak tests.

## Acceptance

Repeated FEN/side/level/profile calls can reuse cache safely.

---

# Phase 11 — Provider registry and provider mocks

## Goal

Create provider infrastructure before real providers.

## Modules

```text
src/ece/providers/provider-contract.js
src/ece/providers/provider-registry.js
src/ece/providers/provider-timeout.js
src/ece/providers/provider-result-normalizer.js
```

## Providers with mocks

- rules/legal;
- Stockfish;
- Syzygy;
- opening book;
- Lichess eval-cache;
- Maia;
- AI text.

## Required behavior

- configured/unavailable states;
- timeout handling;
- source labelling;
- normalization;
- safe diagnostics;
- no raw provider output public leak.

## Acceptance

All provider types can be mocked and routed without real external binaries.

---

# Phase 12 — Lichess eval-cache provider before Stockfish

## Goal

Implement the caching/eval source that can reduce fresh Stockfish calls.

## Modules

```text
src/ece/providers/lichess-eval-cache/lichess-eval-cache-provider.js
src/ece/providers/lichess-eval-cache/lichess-eval-normalizer.js
src/ece/providers/lichess-eval-cache/lichess-eval-importer.js
```

## Scripts

```text
scripts/build-lichess-eval-cache.ps1
```

## Required behavior

- optional local DB path;
- lookup by normalized FEN key;
- source label `cached_lichess_eval`;
- depth/quality checks;
- fallback to Stockfish if insufficient/missing;
- never leak PV/eval below level.

## Acceptance

Provider can be disabled, missing, hit, or miss safely.

---

# Phase 13 — Stockfish provider

## Goal

Add real bounded Stockfish support after cache/provider routing is stable.

## Modules

```text
src/ece/uci/uci-session.js
src/ece/uci/uci-parser.js
src/ece/providers/stockfish/stockfish-provider.js
src/ece/providers/stockfish/stockfish-uci-client.js
src/ece/providers/stockfish/stockfish-output-parser.js
```

## Requirements

- ECE-side only;
- bounded profiles;
- candidate counts by level;
- eval only L8+;
- no raw UCI public output;
- check Lichess eval-cache before live Stockfish where appropriate.

## Tests

- Stockfish unavailable;
- timeout;
- mock successful candidates;
- candidate count gating;
- eval gating;
- no raw UCI in payload.

## Acceptance

ECE can produce bounded candidates/eval through Stockfish where level allows.

---

# Phase 14 — Opening book provider

## Goal

Implement requested opening lookup.

## Modules

```text
src/ece/providers/opening-book/opening-book-provider.js
src/ece/providers/opening-book/opening-book-query.js
```

## Requirements

- only run when `custom.opening` is non-zero;
- level-gated, recommended L4+;
- deterministic source-labelled output;
- no AI invention.

## Acceptance

Opening output works or returns `0` cleanly.

---

# Phase 15 — Syzygy and Maia providers

## Goal

Add optional exact endgame and human-risk providers after core/Stockfish are stable.

## Syzygy

- exact tablebase source;
- L8+ proof/eval where eligible;
- outranks Stockfish eval.

## Maia

- human-risk only;
- no public rating dependency in v1;
- configured default bucket/profile;
- useful mostly in review/proposed-move/high-level warnings;
- never best-move truth.

## Acceptance

Both providers degrade safely and source-label outputs.

---

# Phase 16 — AI text provider as optional enrichment

## Goal

Add AI after deterministic composer is already good.

## Modules

```text
src/ece/providers/ai-text/ai-text-provider.js
src/ece/providers/ai-text/ai-prompt-builder.js
src/ece/providers/ai-text/ai-output-validator.js
```

## Rules

- deterministic composer is default;
- AI optional;
- board-state max one AI call;
- AI gets level-gated facts/text only;
- AI output validated;
- fallback deterministic text on failure;
- no secrets in output/logs.

## Acceptance

AI can improve wording but cannot create chess truth or bypass gates.

---

# Phase 17 — Proposed-move mode

## Goal

Add `POST /v1/ece/proposed-move`.

## Required behavior

- validate FEN;
- validate proposed move by rules provider;
- return illegal result if illegal;
- generate new FEN if legal;
- evaluate warnings/advice by level;
- use Stockfish/eval/AI only where level allows;
- deterministic sentence by default.

## Tests

- legal move;
- illegal move;
- wrong side;
- castling;
- en passant;
- promotion;
- level gating;
- no rating fields;
- deterministic sentence.

## Acceptance

Proposed move mode can serve EDE single-arrow checks.

---

# Phase 18 — Full-game mode / compact ECM1

## Goal

Add deterministic-first full-game review output.

## Endpoint

```text
POST /v1/ece/game-review
```

## Required behavior

- reconstruct game from PGN/moves/FEN history;
- process move-by-move;
- reuse board-state cache;
- build key moments;
- support level-flexible review;
- not forced Level 10;
- AI optional;
- compact ECM1 output.

## Tests

- PGN reconstruction;
- move-list reconstruction;
- invalid game;
- level-flexible output;
- no AI required;
- key moments;
- cache reuse;
- compact format shape.

## Acceptance

Full-game review is useful without AI and does not alter live game state.

---

# Phase 19 — Local development and external provider setup

## Goal

Make the repo easy to run and inspect.

## Add/update

```text
config/providers.example.json
config/composer-profiles.example.json
config/cache.example.json
docs/setup/
docs/licenses/
external_engines/ README placeholders
data/ece_cache/ README placeholders
scripts/run-ece-local.ps1
scripts/smoke-health.ps1
scripts/smoke-board-state.ps1
scripts/validate-external-providers.ps1
```

## Acceptance

A developer can run ECE locally, see what providers are configured, and not commit secrets/binaries/cache DBs.

---

# Phase 20 — Final regression and cleanup

## Goal

Ensure scaffold-to-new-requirements migration is complete.

## Checklist

- public board output has no `position`;
- public board output has no `shared_calculations`;
- no public ratings in v1 request;
- no `hanging_attackable`;
- composer profile numeric;
- deterministic composer works;
- cache works;
- provider mocks work;
- Stockfish route works if configured;
- proposed-move mode works;
- full-game mode works or is clearly deferred;
- tests map to requirements;
- no secrets or external binaries committed.

## Acceptance

The ECE service implements the changed requirements in a stable, testable, modular way.

---

# Recommended Codex phase prompt template

```text
Implement EvenChessEngine changed-requirements Phase [NUMBER] only: [PHASE NAME].

Repo:
C:\Users\jayde\Documents\Chess apps\EvenChessEngine

Before editing, read:
1. docs/requirements/EVENCHESS_ENGINE_REQUIREMENTS_APPENDICES_COMBINED.md
2. README.md
3. package.json
4. existing src/ece/ece-v1-framework.js
5. relevant test files

Do not implement future phases.
Do not use git add .
Do not commit external engines, model files, tablebases, raw downloads, generated cache DBs, .env, API keys, or secrets.
Keep ECE as a private backend service.
Preserve current working /health and /v1/ece/board behavior unless this phase explicitly changes the contract.
Add or update tests.
Run node test/self-test.js and any added tests.
Report exact files changed, tests run, results, incomplete items, and next step.
```
