# Appendix Z - Superseded and Overridden Requirements Register

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Records contradictions, supersessions, overrides, unresolved decisions, and migration decisions.


## Z.2 Instructions

OVR-L1-001: Every contradiction, amendment, replacement, or product-owner override must be recorded here.
OVR-L1-002: Codex updates this appendix when requirements change.
OVR-L1-003: Codex prints replacement summary before implementation.
OVR-L1-004: Old source documents remain archived records, not implementation authority.

## Z.3 Initial Lichess-fork migration overrides

| Override ID | Old requirement/assumption | New Lichess-fork requirement | Reason |
| --- | --- | --- | --- |
| OVR-LICHESS-001 | EvenChess rebuilds chess platform. | Lichess/lila provides chess platform; EvenChess adds mode layer. | Architecture decision. |
| OVR-LICHESS-002 | Build legal move generation and board rules. | Use lila/scalachess/chessops rules. | Mature platform foundation. |
| OVR-LICHESS-003 | Build rooms, clocks, history, PGN, board UI. | Use Lichess baseline; add metadata/overlays. | Avoid duplicate work. |
| OVR-LICHESS-004 | Old UI may replace whole live surface. | Integrate as overlays/cards on Lichess surface. | Preserve baseline/updateability. |
| OVR-LICHESS-005 | Old phase plan starts product implementation. | Stage 1 local boot/boundary comes first. | Prevent building before baseline. |
| OVR-LICHESS-006 | Duplicate old Appendix Q/R mappings. | New Q=abuse, N=monetisation, M=summaries, R=ops. | Clean structure. |
| OVR-LICHESS-007 | Appendix AA outside A-Z. | Advertising/funnel moves into Appendix O. | Required A-Z set. |
| OVR-LICHESS-008 | Account system custom. | Lichess account basics provided unless gap; EvenChess token rules remain. | Fork adaptation. |
| OVR-LICHESS-009 | Stockfish fully custom. | Inspect Lichess analysis/fishnet/tablebase; add gateway wrapper/seam. | Avoid duplicate engine infrastructure. |
| OVR-LICHESS-010 | Matchmaking custom from scratch. | Adapt lila pairing/search or add thin queue only if needed. | Use baseline. |
| OVR-V11-001 | Normal Lichess chess may remain a public peer mode unless later decided. | Version 1.1 public production is EvenChess-only; normal Lichess chess remains preserved internally for platform reuse, diagnostics, regression tests, and upstream compatibility. | Product-owner direction for production launch. |
| OVR-V11-002 | Patch map alone records future reimplementation context. | Version 1.1 requires `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md` for every integration phase, alongside patch-map entries for upstream/core file edits. | Future Lichess updates need seam-level reimplementation notes, not only file-level patch records. |
| OVR-V12-001 | Version 1.1 public shell may hide most ordinary Lichess feature navigation while keeping internals preserved. | Version 1.2 should present EvenChess as a polished deep-blue Lichess-powered platform, preserving and exposing compatible Lichess feature areas such as study, openings, analysis, puzzles, account/settings, and community/profile surfaces while keeping rated play, overlays, AI, TTS, ECR, tokens, and admin controls EvenChess-owned and server-authoritative. | Product-owner direction after Version 1.1: EvenChess is basically Lichess with the level overlay system and AI integrated, not a stripped-down replacement platform. |
| OVR-V13-001 | Version 1.1/1.2 public shell may make the root homepage and shared navigation feel like an EvenChess replacement for Lichess. | Version 1.3 restores a Lichess-first public shell. The homepage and top navigation should remain normal Lichess with minimal explicit EvenChess entrypoints; EvenChess-specific pages, overlays, settings, admin controls, AI/TTS, ECR/search, tokens, and audits remain namespaced. | Product-owner clarification: EvenChess should use Lichess as the chess platform and change only what is unique to EvenChess. |
| OVR-V1-ACTIVE-001 | Version 1.3 may be read as leaving ordinary Lichess play/search visible as the main public flow with EvenChess only as a small side entrypoint or separate search page. | Current Version 1 direction: keep the Lichess style, shell, lobby shape, native setup modal, and internals, but public `PLAY` opens the native setup modal, lobby start controls open that same modal, and setup modal submission starts an EvenChess-owned search contract using Set Level, ECR, tokens, disclosure, overlays, AI/TTS, and audits. | Product-owner clarification: users should open EvenChess, click Play, use the normal Lichess setup modal with extra EvenChess settings, and reach an EvenChess game with overlays/coach rather than visiting a separate play/search page. |
| OVR-NAT-001 | Public play may be treated as a separate EvenChess page while keeping the Lichess shell intact. | Public `PLAY` and lobby controls must continue to use the native Lichess setup modal and flow, with EvenChess metadata (Set Level, ECR, Target Level, tokens, disclosure, AI/TTS, audit controls) attached only through server-owned seams and optional authorized payloads. | Native integration should be a thin layer on top of Lichess, not a public-shell replacement.

## Z.4 Requirements still active

- disclosed assisted variant and outside-help prohibition;
- L0-L10 public ladder and high-level rated legality;
- server-authoritative coaching and audited renders;
- Used Level monotonicity, Assistance Load, Used Offset;
- ECR and Effective Rating;
- Offset Count / Exchange Resolver;
- Target Level isolation;
- server-side Stockfish and AI explainer/compressor boundary;
- token/subscription/ad fairness boundary;
- marketing/funnel config cannot affect fairness;
- same-quality free/Premium summaries;
- telemetry/calibration, exploit controls, operations.

## Z.5 Version 1.1 resolved product-owner decisions

| Decision ID | Version 1.1 decision | Notes |
| --- | --- | --- |
| DEC-L1-001 | Public product is EvenChess-only; implement through server-owned mode metadata and thin Lichess seams unless a later phase proves a variant is required. | Do not delete underlying normal Lichess internals. |
| DEC-L1-004 | Production payment providers are Stripe and PayPal where existing Lichess Plan patterns can be reused safely. | EvenChess fairness must not depend on payment tier. |
| DEC-L1-005 | Rewarded ads target a Google ad stack with server-side verification before token grants. | Ad configuration must not affect rated fairness. |
| DEC-L1-006 | Production AI uses live server-side OpenAI integration, with deterministic mocks for tests. | AI explains authorized truth packets only. |
| DEC-L1-007 | Public EvenChess flows use EvenChess identity/ECR; normal Lichess ratings remain separate and must not be labelled as ECR. | Normal rating data may remain internal where Lichess requires it. |

## Z.5.1 Version 1.2 resolved product-owner decisions

| Decision ID | Version 1.2 decision | Notes |
| --- | --- | --- |
| DEC-V12-010 | EvenChess should keep the breadth of Lichess feature areas where compatible, with EvenChess branding and assisted overlays rather than hiding those surfaces. | Rated public game starts remain EvenChess/ECR-owned. |
| DEC-V12-011 | EvenChess-specific player preferences belong in a dedicated EvenChess section inside user settings. | Settings cannot raise live coaching strength beyond server policy. |
| DEC-V12-012 | EvenChess backend/provider configuration belongs in admin/backend EvenChess settings. | API keys and secrets must remain server-side and should not be exposed to browsers. |
| DEC-V12-013 | Text-to-Speech Coach is an approved Version 1.2 feature to plan and integrate. | TTS may read only authorized/sanitized coaching or explanation text and must not become a stronger advice channel. |

## Z.5.2 Version 1.3 resolved product-owner decisions

| Decision ID | Version 1.3 decision | Notes |
| --- | --- | --- |
| DEC-V13-001 | Keep the normal Lichess public shell visible and integrate EvenChess through explicit assisted-mode entrypoints. | Supersedes the broad public-shell takeover parts of Version 1.1/1.2 without deleting EvenChess module contracts or namespaced routes. |
| DEC-V13-002 | Deep-blue styling remains for EvenChess-specific pages and overlays, not for replacing the whole Lichess homepage/top navigation. | Normal Lichess pages should stay native-looking and updateable. |
| DEC-V13-003 | The primary `PLAY` navigation routes to EvenChess search. | Users should feel like they are using Lichess, but public play/search uses EvenChess matchmaking, levels, ECR, and overlays. |

## Z.5.3 Current Version 1 resolved product-owner decisions

| Decision ID | Current Version 1 decision | Notes |
| --- | --- | --- |
| DEC-V1-ACTIVE-001 | EvenChess is the visible public product; the public Play and lobby search path starts EvenChess, not ordinary Lichess rated chess. | Lichess internals remain preserved for board, clocks, legal moves, lifecycle, studies/openings/analysis, accounts, diagnostics, and upstream sync. |
| DEC-V1-ACTIVE-002 | The existing Lichess lobby/setup modal should be reused and extended with EvenChess settings rather than replaced by a separate marketing or search page. | Set Level, mode, time control, Target Level, token/account eligibility, and outside-help disclosure are EvenChess-owned controls. |
| DEC-V1-ACTIVE-003 | Overlay/coach visibility in real games requires server-authorized EvenChess game payloads. | Client previews may explain the UX, but live advice must not render without persisted policy metadata and audit identity. |

## Z.6 Remaining unresolved product-owner decisions

| Decision ID | Decision needed | Default until decided |
| --- | --- | --- |
| DEC-L1-002 | Which ECR pools launch first. | Rapid first unless changed. |
| DEC-L1-003 | Should assisted PGNs be public/exported. | Do not publish as normal chess; require metadata decision. |
| DEC-L1-008 | Upstream sync cadence. | Use sync process until decided. |
| DEC-L1-009 | Branding/legal wording around fork. | Do not imply official affiliation. |
| DEC-V12-001 | Exact TTS provider strategy. | Browser speech first where feasible; server-side provider seam for later approval. |
| DEC-V12-002 | Which Lichess feature areas must be first-class in initial public navigation. | Include play, study, openings, analysis, puzzles, account/settings; inspect others during Version 1.2 Phase C. |
| DEC-V12-003 | Whether normal non-assisted game starts are publicly available or only internal/regression. | Rated public starts remain EvenChess; preserve internals and expose learning/exploration features. |
| DEC-V12-004 | Admin secret-entry mechanism. | Prefer environment/secret store status display; do not render raw secrets. |
| DEC-V12-005 | TTS availability by plan. | Same coaching strength for all; any paid difference may only be quota/voice/convenience if approved. |

## Z.7 Override template

| Field | Value |
| --- | --- |
| Override ID | OVR-YYYY-### |
| Date | YYYY-MM-DD |
| Source instruction | User/product-owner request or source document. |
| Affected appendix/files | List all live docs and code files. |
| Old requirement | Exact old requirement or assumption. |
| New requirement | Exact new requirement. |
| Reason | Why change was made. |
| Files/docs updated | List. |
| Codex printed to screen? | Yes/No. |
