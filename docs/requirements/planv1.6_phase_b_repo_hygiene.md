# EvenChess-Lichess Plan v1.6 Phase B - Repository, Branch, and Patch Hygiene

**Date:** 2026-06-03
**Phase:** B
**Status:** Conducted; final release hygiene remains blocked pending selective staging/review
**Repo:** EvenChess-Lichess
**Path:** `/home/jayde/dev/lila-docker/repos/lila`
**Branch created:** `codex/evenchess-v1.6-readiness`
**Base observed before branch:** `master` at `da62f80dd1`

This document conducts Plan v1.6 Phase B by creating the release-hygiene branch, auditing the current working tree, classifying the dirty state, adding safe ignore rules for obvious local scratch artifacts, and recording the exact blockers that must be resolved before a release candidate can be cut.

No broad staging was performed. Do not use `git add .`.

---

## 1. Phase B Scope

Phase B goal from `planv1.6.md`:

- Make the codebase shippable from source control.

Phase B required work:

- Create a release branch for v1.6.
- Audit `git status --short`.
- Classify dirty files as intended implementation, requirements/docs, generated assets, local config, or accidental edits.
- Selectively add intended files only.
- Ensure major EvenChess implementation files are tracked.
- Ensure generated build outputs, logs, `.env`, caches, provider data, tablebases, weights, and local databases are ignored.
- Review patch map and integration log state.

---

## 2. Branch Status

Current release-hygiene branch:

```text
codex/evenchess-v1.6-readiness
```

Branch creation completed successfully.

Important constraint:

- The branch was created with the existing dirty working tree intact.
- This preserves local work, but it means the branch is not yet release-clean.
- Phase B must not be marked release-complete until the staged/unstaged/untracked state is reconciled.

---

## 3. Working Tree Audit Summary

Observed after creating the branch and adding safe ignore rules:

```text
tracked/staged-or-modified status lines: 230
cached/staged changed file count:       116
unstaged tracked changed file count:    99
untracked status entries:               35
tracked requirement deletions:          33
staged-and-unstaged critical files:      20
```

Interpretation:

- A large EvenChess implementation slice is already staged.
- Several staged files also have unstaged edits (`AM`/`MM`), so the staged snapshot is stale for those files.
- Many related docs/scripts/UI assets remain untracked.
- Old individual requirements files are deleted at the root and appear to have been moved/retained under `docs/requirements/old/` and `docs/requirements/old2/`.
- The release branch is not yet shippable because required release files may still be untracked and some staged files do not include their latest edits.

---

## 4. Safe Ignore Rules Added

`.gitignore` was updated to ignore only obvious local scratch artifacts:

```text
/.codex/
/NUL/
/=*
/tmp_test_file.txt
/modules/web/src/main/ui/tmp_phaseB_test.txt
```

Reason:

- `.codex/` contains local Codex environment state.
- `NUL/` contains Windows shell artifact files.
- `=1`, `=320`, `=360`, `=620` are zero-byte shell scratch files.
- `tmp_test_file.txt` and `tmp_phaseB_test.txt` are temporary test files.

This deliberately does not ignore:

- EvenChess source files;
- Test Ground scripts;
- requirement plan directories;
- release evidence docs;
- UI tests;
- patch map or integration log files.

Those may be intended release inputs and must be reviewed explicitly.

---

## 5. Dirty File Classification

### 5.1 Intended EvenChess implementation candidates

These are likely intended release files because they are EvenChess-owned modules, tests, views, controllers, or UI surfaces:

- `app/controllers/EvenChess.scala`
- `app/views/evenchess/account.scala`
- `app/views/evenchess/play.scala`
- `modules/evenchess/src/main/*.scala`
- `modules/evenchess/src/test/*.scala`
- `ui/lobby/src/evenchessSetup.ts`
- `ui/lobby/tests/evenchessSetup.test.ts`
- `ui/round/src/view/evenchessOverlay.ts`
- `ui/round/tests/evenchessOverlay.test.ts`
- `ui/round/src/evenchessTestGround.ts`
- `ui/round/src/evenchessTestGroundMoveBridge.ts`
- `ui/round/tests/evenchessTestGround*.test.ts`
- `ui/analyse/src/view/evenchessLearning.ts`
- `ui/analyse/tests/evenchessLearning.test.ts`
- `ui/opening/src/evenchessOpeningAi.ts`
- `ui/opening/tests/evenchessOpeningAi.test.ts`
- `ui/lib/src/evenchessTts.ts`
- `ui/lib/tests/evenchessTts.test.ts`
- EvenChess CSS files under `ui/*/css/`

Required action:

- Review each file against the active requirements and current test status.
- Stage only reviewed files.
- For `AM` files, restage after final review because the staged snapshot is stale.

### 5.2 Upstream/core Lichess seam candidates

These files modify native Lichess surfaces and require patch-map/integration-log coverage:

- `app/Lila.scala`
- `app/controllers/Challenge.scala`
- `app/controllers/Dev.scala`
- `app/controllers/Setup.scala`
- `app/views/lobby/home.scala`
- `conf/routes`
- `modules/challenge/src/main/Challenge.scala`
- `modules/challenge/src/main/JsonView.scala`
- `modules/challenge/src/main/ui/ChallengeUi.scala`
- `modules/mod/src/main/ui/ModUi.scala`
- `modules/pref/src/main/*`
- `modules/web/src/main/Env.scala`
- `modules/web/src/main/ui/*`
- `ui/challenge/src/*`
- `ui/lobby/src/*`
- `ui/round/src/*`
- `ui/analyse/src/*`
- `ui/opening/src/*`

Required action:

- Confirm each upstream seam is necessary for public v1.6.
- Confirm each seam has a patch-map entry with reason, isolation notes, upstream merge risk, tests, and rollback notes.
- Remove or defer seams that are not needed for first public beta.

### 5.3 Requirements and planning candidates

These are likely intended documentation inputs:

- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- `docs/requirements/planv1.6.md`
- `docs/requirements/planv1.6_phase_a_scope_freeze.md`
- `docs/requirements/planv1.6_phase_b_repo_hygiene.md`
- `docs/requirements/plan_analysis_memory/`
- `docs/requirements/plan_version_1.1/`
- `docs/requirements/plan_version2.1/`
- `docs/requirements/plan_version2.2/`
- `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_2_PLAN.md`
- `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_2_RELEASE_EVIDENCE.md`
- `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_3_COMPLETION_REPORT.md`
- `docs/evenchess/NEXT_CHAT_HANDOVER_2026_06_03.md`

Required action:

- Keep current active requirements and current phase plans.
- Decide whether historical plan folders are release evidence or should remain local-only.
- Do not lose old requirements unless their move to `old/` and `old2/` is intentional and reviewed.

### 5.4 Root old requirement deletions

There are 33 tracked deletions under `docs/requirements/`, including old `APPENDIX_*`, old main requirements, old patch map, and file manifest documents.

Likely interpretation:

- These appear superseded by the combined V2 requirements and archived under `docs/requirements/old/` / `old2/`.

Required action:

- Confirm this archival move is intentional.
- If intentional, stage the deletions and the archive directories together.
- If not intentional, restore the root files before release.

### 5.5 Test Ground and local launch scripts

Untracked candidates:

- `scripts/evenchess-local-start.sh`
- `scripts/evenchess-local-status.sh`
- `scripts/evenchess-local-stop.sh`
- `scripts/evenchess-test-ece-server.js`
- `scripts/evenchess-test-ece-server.test.mjs`
- `scripts/evenchess-testground-launcher.vbs`
- `scripts/evenchess-testground-panel.js`
- `scripts/evenchess-testground.ps1`
- `scripts/install-evenchess-testground-shortcut.ps1`

Required action:

- Keep local Test Ground scripts if they are part of local developer testing.
- Ensure none are exposed as public production runtime.
- Document them as local tooling.
- Verify they do not contain secrets or private provider paths.

### 5.6 Local/generated/private data screen

Path-level screen did not show pending external engine directories, model weights, tablebases, `.env`, or obvious secret files.

Expected matches:

- `modules/evenchess/src/main/StockfishAnalysisGateway.scala`
- `modules/evenchess/src/test/StockfishAnalysisGatewayTest.scala`
- old requirements docs mentioning Stockfish.

These are source/policy files and still need normal code review, but they are not provider binaries or private data by path.

Required action:

- Run a content-level secret scan before release.
- Confirm no ECE provider paths, API keys, raw prompts, or raw provider output are committed.

---

## 6. Staged-and-Unstaged Files Requiring Immediate Restage Review

The following files have both staged and unstaged changes (`AM` or `MM`). Their staged snapshots are stale and must be reviewed/restaged or intentionally split before release:

```text
app/controllers/EvenChess.scala
docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md
docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md
docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md
modules/evenchess/src/main/AdminBackendSettings.scala
modules/evenchess/src/main/AdminOpsDashboard.scala
modules/evenchess/src/main/BotOperations.scala
modules/evenchess/src/main/EceLiveBridge.scala
modules/evenchess/src/main/EvenChessRatingCalibration.scala
modules/evenchess/src/main/LevelBasedMatchmaking.scala
modules/evenchess/src/main/PlaySearchIntegration.scala
modules/evenchess/src/test/AdminBackendSettingsTest.scala
modules/evenchess/src/test/AdminOpsDashboardTest.scala
modules/evenchess/src/test/BotOperationsTest.scala
modules/evenchess/src/test/EvenChessRatingCalibrationTest.scala
modules/evenchess/src/test/LevelBasedMatchmakingTest.scala
modules/evenchess/src/test/PlaySearchIntegrationTest.scala
ui/lobby/src/evenchessSetup.ts
ui/lobby/src/setupCtrl.ts
ui/lobby/tests/evenchessSetup.test.ts
```

Release rule:

- Do not commit these until the working-tree version is reviewed.
- If a partial commit is needed, use explicit path staging or interactive patch staging carefully.

---

## 7. Patch Map and Integration Log Review

Observed:

- `docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md` is modified in both staged and unstaged states.
- `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` is modified in both staged and unstaged states.
- Recent entries cover deep-only eval, proposed-move nested side output, stable overlay refresh, and potential/proposed side-state corrections.

Phase B status:

- Patch map and integration log exist and are active.
- They are not yet release-clean because the staged snapshots are stale.
- Before release, compare touched upstream seams against patch-map entries and add missing entries.

Required action:

- For every file listed in Section 5.2, confirm a corresponding patch map/integration log entry.
- Restage the final docs after any missing seam entries are added.

---

## 8. Selective Staging Plan

Do not run `git add .`.

Recommended order:

1. Stage ignore and v1.6 planning docs:

```bash
git add .gitignore \
  docs/requirements/planv1.6.md \
  docs/requirements/planv1.6_phase_a_scope_freeze.md \
  docs/requirements/planv1.6_phase_b_repo_hygiene.md
```

2. Review and stage active requirements docs:

```bash
git add docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md
```

3. Review old requirements archival decision:

```bash
git add docs/requirements/old docs/requirements/old2
git add -u docs/requirements
```

Only run this if the root-file deletions and archival folders are confirmed intentional.

4. Stage EvenChess-owned backend modules after review:

```bash
git add modules/evenchess/src/main modules/evenchess/src/test
```

5. Stage EvenChess public views/controllers after review:

```bash
git add app/controllers/EvenChess.scala app/views/evenchess
```

6. Stage upstream seam files only after patch-map coverage is verified:

```bash
git add app/Lila.scala app/controllers/Challenge.scala app/controllers/Dev.scala app/controllers/Setup.scala conf/routes
git add modules/challenge modules/pref modules/web modules/mod
git add ui/challenge ui/lobby ui/round ui/analyse ui/opening ui/lib
git add docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md
```

7. Stage local Test Ground scripts only if they are intended local tooling:

```bash
git add scripts/evenchess-local-start.sh scripts/evenchess-local-status.sh scripts/evenchess-local-stop.sh
git add scripts/evenchess-test-ece-server.js scripts/evenchess-test-ece-server.test.mjs
git add scripts/evenchess-testground-launcher.vbs scripts/evenchess-testground-panel.js scripts/evenchess-testground.ps1 scripts/install-evenchess-testground-shortcut.ps1
```

Each block should be followed by:

```bash
git status --short
git diff --cached --check
```

---

## 9. Phase B Acceptance Status

| Acceptance gate | Status | Notes |
|---|---|---|
| Release branch created | Complete | `codex/evenchess-v1.6-readiness` |
| `git status --short` audited | Complete | Dirty state classified in this document |
| Dirty files classified | Complete | Sections 5 and 6 |
| Generated/local artifacts ignored | Complete for obvious artifacts | `.gitignore` updated for `.codex`, `NUL`, `=*`, and temporary test files |
| Intended files selectively staged | Blocked / not performed | Requires review of stale staged files and untracked release candidates |
| Major EvenChess implementation files tracked | Blocked | Many are staged, but some related docs/scripts/UI assets remain untracked and `AM` files need restage |
| Private ECE/provider data absent from pending paths | Initial screen complete | No external engine dirs, weights, tablebases, `.env`, or obvious secret paths found |
| Patch map/integration log current | Blocked pending restage/review | Both docs are `MM`; final seam coverage must be checked before release |

Phase B is conducted but not release-complete. The branch and hygiene report are in place; final completion requires selective staging/review and patch-map reconciliation.

---

## 10. Phase C Entry Criteria

Phase C may start for architecture planning, but code release work should not proceed to RC until these Phase B blockers are resolved:

1. Decide whether old root requirements deletions and `old/` archives are intentional.
2. Review and restage all `AM`/`MM` files.
3. Review untracked docs/scripts/UI assets and stage only intended files.
4. Confirm every upstream seam is patch-mapped.
5. Run content-level secret/private-data checks.
6. Run `git diff --cached --check`.
7. Produce a clean release-candidate staging set without `git add .`.
