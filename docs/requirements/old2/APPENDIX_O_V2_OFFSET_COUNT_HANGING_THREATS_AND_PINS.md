# Appendix O — Offset Count, Hanging Pieces, Threats, and Pins

## O.1 Purpose

This appendix defines the core deterministic overlays that make the EvenChess board useful.

## O.2 Offset Count

REQ-O-V2-001: Offset Count is the Exchange Resolver / take-take-take feature.

REQ-O-V2-002: Offset Count overlay is based on piece-count exchange units: each captured piece counts as 1.

REQ-O-V2-003: Equal result displays as blue shield/circle.

REQ-O-V2-004: Student-favorable result displays as green circle with white number.

REQ-O-V2-005: Student-unfavorable result displays as red circle with white number.

REQ-O-V2-006: Unknown/unavailable must not display as equal.

REQ-O-V2-007: ECE must also calculate value-based offset for summary/warning text.

REQ-O-V2-008: Significant positive value offset creates `advantage_offset_value`.

REQ-O-V2-009: Significant negative value offset creates `disadvantage_offset_value`.

## O.3 Hanging pieces

REQ-O-V2-010: Hanging piece that cannot currently be taken uses orange exclamation and thin orange border.

REQ-O-V2-011: Hanging piece that can be taken uses red exclamation and thin red border.

REQ-O-V2-012: ECE should separate `hanging_attackable` and `hanging_not_attackable`.

REQ-O-V2-013: Display Engine must map those categories into square markers without layout instability.

## O.4 Threat arrows

REQ-O-V2-020: Student/player threats are green dotted arrows from center square to center square.

REQ-O-V2-021: Opponent threats are red dotted arrows from center square to center square.

REQ-O-V2-022: Red arrows render above green arrows if overlapping.

REQ-O-V2-023: If red and green arrows overlap along the same path, both must remain visible through offset, dash pattern, opacity, or another accessible rendering technique.

REQ-O-V2-024: Threats are side-perspective outputs: student threats and opponent threats.

## O.5 Pins

REQ-O-V2-030: Pinned pieces use the same or closest feasible Lichess analysis pin style.

REQ-O-V2-031: Pin payload should include pinned square, pinning piece square, target behind the pinned piece, pin line, and pin type.

REQ-O-V2-032: Pins feed Offset Count legality and Summary/Plan where allowed.
