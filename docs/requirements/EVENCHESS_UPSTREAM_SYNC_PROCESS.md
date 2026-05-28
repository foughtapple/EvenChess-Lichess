# EvenChess Upstream Sync Process

**Suite:** EvenChess-Lichess Version 1

## 1. Remote setup

```bash
git remote -v
git remote add upstream https://github.com/lichess-org/lila.git
```

Use actual repo URLs selected by maintainer.

## 2. Branch policy

Keep main controlled. Implement features in branches. Do upstream sync in a dedicated branch. Do not mix sync with unrelated features.

## 3. Sync workflow

```bash
git status
git fetch upstream
git switch -c sync/upstream-YYYY-MM-DD
git merge upstream/master
```

Then resolve conflicts, consult patch map, preserve invariants, run baseline lila checks, run EvenChess regressions, update patch map, and write sync report.

## 4. Conflict handling

Do not delete EvenChess behavior silently. Identify linked requirements, whether conflict is upstream core or EvenChess-owned, update patch map risk, run normal chess and EvenChess regressions, and report unresolved conflicts.

## 5. Patch isolation strategy

Prefer new EvenChess modules/services, small hooks, server-side flags, stable contracts, UI components consuming server payloads, and registered routes. Avoid broad game lifecycle rewrites, replacing chessground, duplicating legality, mixing ECR into normal ratings, and frontend-only fairness.

## 6. Minimum checks

Local site boots, normal game plays, EvenChess mode flag works, dummy overlay works if implemented, ledger writes if implemented, touched seam tests pass, patch map current.

## 7. Sync report template

```markdown
# Upstream Sync Report - YYYY-MM-DD
- Upstream remote/branch:
- Upstream commit merged:
- EvenChess branch:
- Conflicts:
- Patch map entries affected:
- Normal chess regression:
- EvenChess regression:
- Tests run:
- Remaining risks:
- Decision:
```
