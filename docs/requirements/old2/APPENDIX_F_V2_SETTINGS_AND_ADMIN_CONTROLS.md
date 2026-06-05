# Appendix F — EvenChess Settings and Admin Controls

## F.1 Purpose

This appendix defines user-facing and admin/backend settings sewn into Lichess settings/admin surfaces.

## F.2 User settings

REQ-F-V2-001: Add an EvenChess section to the normal settings/preferences flow.

REQ-F-V2-002: Settings should preserve the Lichess-style layout and not become a detached standalone settings site.

REQ-F-V2-003: User settings should include default live coaching display preferences, overlay visibility, card behavior, review defaults, proposed-move button behavior, TTS options if enabled, and accessibility preferences.

REQ-F-V2-004: Live rated permissions are server-owned. User settings may request display preferences but cannot grant stronger assistance than policy allows.

## F.3 Admin/backend settings

REQ-F-V2-010: Admin controls should include ECE provider status, AI provider status, Stockfish status, feature flags, token/ad settings, subscription toggles, review-token settings, retention settings, and safety kill switches.

REQ-F-V2-011: Secrets must be configured server-side or environment-side and must not be exposed to the client.

REQ-F-V2-012: Admin toggles must not silently alter rated fairness; any fairness-affecting toggle requires audit and versioning.

## F.4 Setting persistence

REQ-F-V2-020: User EvenChess settings must persist per account.

REQ-F-V2-021: Live game state must include a snapshot of relevant display/coaching settings where needed for audit/replay.

REQ-F-V2-022: Review mode may allow temporary overrides without changing the user's default settings.
