# Appendix P — Stockfish, AI, and TTS Policy

## P.1 Purpose

This appendix defines policy for engine help, AI text, and optional Text-to-Speech.

## P.2 Stockfish

REQ-P-V2-001: Stockfish remains server-side/ECE-side only.

REQ-P-V2-002: Clients must never receive unrestricted raw engine access.

REQ-P-V2-003: Candidate counts are level-gated: L5 one, L6 two, L7+ up to three unless configured otherwise.

REQ-P-V2-004: Numeric eval begins at L8 unless explicitly changed later.

REQ-P-V2-005: All Stockfish profiles must be bounded by depth/nodes/movetime/MultiPV/time-control-independent config where applicable.

## P.3 AI

REQ-P-V2-010: AI is an explainer/compressor of deterministic/ECE/Stockfish truth packets.

REQ-P-V2-011: AI must not invent chess facts or bypass level gates.

REQ-P-V2-012: Each board-state ECE call may use at most one AI call where enabled.

REQ-P-V2-013: Full-game ECE mode may use one larger AI call for narrative compression where feasible.

REQ-P-V2-014: AI credentials must be server-side only.

REQ-P-V2-015: AI output must be validated and fall back deterministically on failure.

## P.4 TTS

REQ-P-V2-020: TTS Coach, if enabled, may read only server-authorized Summary/Plan or review text.

REQ-P-V2-021: TTS must not generate new coaching content separate from authorized text.

REQ-P-V2-022: TTS settings must live in EvenChess settings and be off/configurable according to product decision.
