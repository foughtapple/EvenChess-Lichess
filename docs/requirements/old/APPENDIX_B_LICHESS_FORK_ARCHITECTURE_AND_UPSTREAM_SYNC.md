# Appendix B - Lichess Fork Architecture and Upstream Sync

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Defines the technical posture of EvenChess-Lichess as a fork of Lichess/lila, including boundaries, upstream sync, and patch discipline.


## B.2 Lichess foundation assumptions to verify in Stage 1

ARCH-L1-001: The active upstream foundation is `lichess-org/lila`.
ARCH-L1-002: Local development should start with `lichess-org/lila-docker` unless Stage 1 proves a better path.
ARCH-L1-003: Treat lila as the main backend/frontend application; associated public projects include scalachess, chessground, lila-ws, search, tablebase, and related services.
ARCH-L1-004: Codex must inspect checked-out source before selecting integration points.
ARCH-L1-005: The fork must remain updateable; any core edit must be justified, recorded, tested, and isolated when possible.

## B.3 Fork strategy

| Layer | Desired approach | Patch-map requirement |
| --- | --- | --- |
| Requirements/docs | Own files under docs/requirements and docs/evenchess. | No patch risk. |
| Backend policy | Prefer new EvenChess package/module/service. | Record lila hooks. |
| Game metadata | Add minimal EvenChess mode/policy metadata. | Record model/serialization edits. |
| UI | Prefer EvenChess-specific components, classes, data payloads. | Record board/chessground/lila UI edits. |
| Engine/AI | Internal service boundary; server-side only. | Record analysis/fishnet/tablebase hooks. |
| Rating/matchmaking | Separate ECR pool and matchmaking policy. | Record pairing/rating edits. |
| Marketing/monetisation | Config-driven fork-specific surfaces. | Record account/billing UI edits. |

## B.4 Namespace and file-boundary rule

ARCH-L1-010: EvenChess code must be namespaced using `evenchess` or equivalent.
ARCH-L1-011: New backend services, models, routes, forms, event types, and UI components must use EvenChess naming.
ARCH-L1-012: Core lila edits must be small hooks/delegators/feature flags, not broad mixed logic.
ARCH-L1-013: Touched upstream files must be recorded in `EVENCHESS_LICHESS_PATCH_MAP.md`.
ARCH-L1-014: Normal chess behavior must remain unchanged unless explicitly approved.

## B.5 Upstream sync

SYNC-L1-001: Keep an `upstream` remote pointing to official Lichess/lila.
SYNC-L1-002: Use feature branches or a controlled integration branch.
SYNC-L1-003: Before upstream sync, review patch map and conflict zones.
SYNC-L1-004: After upstream sync, run baseline lila checks and EvenChess regression checks.
SYNC-L1-005: Do not silently delete EvenChess hooks during conflict resolution.
SYNC-L1-006: Patch map entries must include upstream merge risk: Low, Medium, High, or Unknown.

## B.6 Licensing and branding

ARCH-L1-020: Retain required open-source notices and comply with upstream lila license obligations.
ARCH-L1-021: Do not imply official Lichess affiliation without permission.
ARCH-L1-022: Public naming must distinguish EvenChess-Lichess as a fork-based product.
ARCH-L1-023: Distribution/hosting/source-publication obligations require review before launch.

## B.7 Integration decision tree

Before editing code, Codex must answer: Is it Lichess-provided? Is it EvenChess-specific? Does it need a lila lifecycle hook? Does it alter normal chess? Does it expose engine/AI truth? Does it affect rating, tokens, billing, or matchmaking?

## B.8 Stage 1 architecture output

Produce `docs/evenchess/stage1_go_no_go.md` with local boot status, inspected commit/version, account/game status, proposed backend/UI seams, game metadata seam, overlay delivery seam, rating/matchmaking seam candidates, patch map entries, and go/no-go decision.
