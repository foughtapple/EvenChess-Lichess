# Appendix Z — Superseded and Overridden Requirements Register

## Z.1 Purpose

This appendix records supersessions created by Version 2.

## Z.2 Version 2 supersessions

| Override ID | Old requirement/assumption | New Version 2 requirement | Reason |
|---|---|---|---|
| OVR-V2-001 | EvenChess can be implemented as a separate add-on/page beside Lichess. | EvenChess must be sewn into native Lichess-style public flows. | Product owner clarified the desired product experience. |
| OVR-V2-002 | Public shell should preserve Lichess with minimal EvenChess entrypoints. | Public product is EvenChess, using Lichess internals and visual patterns. | Earlier V1.3 interpretation was too detached. |
| OVR-V2-003 | Matchmaking can be handled by feeding equivalent Lichess rating into Lichess search. | EvenChess MMR Engine owns search/match contracts; Lichess receives finalized game contract. | Level preferences and offsets require contract logic. |
| OVR-V2-004 | Coaching text can be many separate card sections. | Text is consolidated into fixed Summary and Plan cards. | Avoid live UI overload and layout jump. |
| OVR-V2-005 | Review can rely only on ordinary analysis/replay. | Review uses saved ECE history plus custom/full-game ECE modes. | Enables Live White/Black/Both and token-based L10 analysis. |
| OVR-V2-006 | Tokens only cover game starts and summaries. | Add custom ECE analysis/full-game analysis token paths. | L10 custom/full-game analysis can create AI/engine cost. |
| OVR-V2-007 | EvenChess overlays are generic. | Each overlay has explicit board position, visual behavior, priority, and level-gating. | Display tuning requires precision. |

## Z.3 Rules

REQ-Z-V2-001: Any future contradiction must be added here before implementation.

REQ-Z-V2-002: Codex must report supersession/override in its completion summary.

REQ-Z-V2-003: Appendix Z is not optional; silent requirement replacement is prohibited.
