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

## Z.5 Unresolved product-owner decisions

| Decision ID | Decision needed | Default until decided |
| --- | --- | --- |
| DEC-L1-001 | Variant vs mode flag vs metadata layer. | Stage 1 mode flag only. |
| DEC-L1-002 | Which ECR pools launch first. | Rapid first unless changed. |
| DEC-L1-003 | Should assisted PGNs be public/exported. | Do not publish as normal chess; require metadata decision. |
| DEC-L1-004 | Payment provider. | Interface only. |
| DEC-L1-005 | Rewarded-ad provider. | Interface/config only. |
| DEC-L1-006 | AI provider/model. | Mock first; configurable server-side provider. |
| DEC-L1-007 | Retain normal Lichess ratings in fork UI. | Keep separate; do not label as ECR. |
| DEC-L1-008 | Upstream sync cadence. | Use sync process until decided. |
| DEC-L1-009 | Branding/legal wording around fork. | Do not imply official affiliation. |

## Z.6 Override template

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
