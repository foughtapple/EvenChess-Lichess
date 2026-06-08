# Appendix X — Upstream Sync and Patch-Map Requirements

## X.1 Purpose

This appendix keeps the EvenChess fork updateable while allowing necessary sewn-in changes.

## X.2 Patch-map entries

Every core/upstream Lichess edit must record:

- date;
- phase/task;
- upstream base SHA;
- fork SHA;
- file touched;
- requirement reference;
- why the core file had to be touched;
- whether the change can be isolated later;
- merge risk;
- tests added/run;
- rollback note.

REQ-X-V2-001: Integration seams must be documented in `docs/integration` or the patch map.

REQ-X-V2-002: Requirements-only documentation changes may skip patch-map entries unless they change implementation policy.

## X.3 Upstream sync

REQ-X-V2-010: Do not sync upstream casually during feature work.

REQ-X-V2-011: Before upstream sync, ensure working tree is clean and patch map is current.

REQ-X-V2-012: After upstream sync, rerun Lichess regression and EvenChess integration tests.

REQ-X-V2-013: High-risk areas require explicit approval before modification: scalachess, chessground, pgn-viewer, lila-ws, lila-search, lila-fishnet, fishnet, global rating/perf internals, and core game BSON/schema internals.
