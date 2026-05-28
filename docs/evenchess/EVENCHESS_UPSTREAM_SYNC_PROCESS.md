# EvenChess-Lichess Upstream Sync Process

## Purpose

Keep the EvenChess fork updateable from upstream Lichess.

## Expected remotes

origin: https://github.com/foughtapple/EvenChess-Lichess.git
upstream: https://github.com/lichess-org/lila.git

## Sync process

1. Fetch upstream.
2. Create a sync branch.
3. Merge or rebase upstream into the sync branch.
4. Resolve conflicts.
5. Check the patch map.
6. Run tests.
7. Merge to the EvenChess working branch only after validation.

## Rule

Every EvenChess edit to an upstream Lichess file must be recorded in docs/evenchess/EVENCHESS_LICHESS_PATCH_MAP.md.
