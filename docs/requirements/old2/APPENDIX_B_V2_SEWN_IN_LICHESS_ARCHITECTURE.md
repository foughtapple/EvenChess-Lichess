# Appendix B — Sewn-In Lichess Architecture

## B.1 Purpose

This appendix defines how EvenChess is integrated into Lichess/lila in Version 2.

The rule is not "build another thing on top". The rule is:

> Keep the Lichess platform and user experience shape, but make the public product EvenChess.

## B.2 Architecture model

REQ-B-V2-001: Lichess/lila remains the core application foundation.

REQ-B-V2-002: EvenChess replaces public play/search/rating behavior through integrated seams, not by creating a disconnected duplicate chess platform.

REQ-B-V2-003: EvenChess-specific engines and modules should remain logically separated where feasible: MMR Engine, ECE, Display Engine, token economy, settings, admin controls, audit, and review systems.

REQ-B-V2-004: The UI should reuse Lichess layout patterns. EvenChess controls should look native to the page they are placed in.

## B.3 Likely integration seams

Likely seams include:

- homepage/product identity templates;
- top navigation and wordmark;
- lobby quick-pairing cards;
- native play/setup modal;
- seek/search submission;
- lobby/pairing service path;
- challenge creation and acceptance;
- round page payloads;
- board overlay rendering layer;
- analysis/replay pages;
- study and opening explorer board surfaces;
- user settings/preferences;
- account/top-bar surfaces;
- plan/subscription pages;
- admin/ops pages.

REQ-B-V2-010: Any upstream/core Lichess file touched for these seams must be recorded in the patch map.

REQ-B-V2-011: The integration should prefer thin adapters that call EvenChess-owned modules.

REQ-B-V2-012: Broad rewrites of Lichess pages are prohibited unless the phase explicitly approves the seam and rollback path.

## B.4 Public product behavior

REQ-B-V2-020: Public `PLAY`, quick pairing, and setup should initiate EvenChess search/match contracts.

REQ-B-V2-021: Public rated games must use EvenChess ECR/MMR and not ordinary Lichess rating pools.

REQ-B-V2-022: Existing Lichess feature areas may remain available under EvenChess branding when safe.

REQ-B-V2-023: Ordinary Lichess mechanics may remain internally available for regression and development, but must not confuse public rated EvenChess behavior.

## B.5 Patch discipline

REQ-B-V2-030: Every upstream seam must include: file, reason, requirement link, risk, tests, rollback note, and whether the change can later be isolated.

REQ-B-V2-031: Codex must not use a separate route/page where the requirement says native Lichess flow must be adapted.

REQ-B-V2-032: Any previous addon-style implementation must be identified and either removed, adapted, or documented as temporary.
