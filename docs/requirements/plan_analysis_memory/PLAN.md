# EvenChess Analysis Memory Plan

**Date:** 2026-06-01
**Repo:** EvenChess-Lichess
**Scope:** Persist and reuse ECE payload history for replay/analysis without recalculating when retained payloads are already available.

## Goal

EvenChess should reuse stored ECE payloads during replay and analysis whenever possible.

Users should be able to open a game they played, step through moves, and see the same EvenChess overlay shell used in live games, computer games, and analysis/replay. Local display toggles remain adjustable, but they only hide or show already-authorized payload families up to the current Set Level.

## Retention Rules

- Keep each user's last 10 completed games with attached per-ply live ECE history.
- When the 11th game is retained, the oldest unsaved recent game history is replaced.
- If a game is outside retained memory, replay has no attached ECE payload and a new analysis must be requested before ECE overlays are available.
- Keep each user's last 100 requested full-game/custom analyses.
- Requested analyses are keyed by game, White level, Black level, perspective, ECE version, policy version, and AI flag.

## Payload Sources

1. **Live retained history:** created during live play and attached to the completed game.
2. **Requested full-game/custom analysis:** created when the user asks for a full-game analysis, including cases where White and Black use different levels.
3. **Missing history:** no payload exists; the UI can still show the overlay shell, but analysis must be requested before payload-backed overlays/cards appear.

## Overlay Rules

- The same EvenChess overlay shell is used across live play, computer play, analysis/replay, study/review, and other board surfaces.
- Live games use the live game's server-authorized Set Level.
- Retained history uses the Set Level represented by the retained payload.
- Requested analyses use the levels in the requested-analysis key.
- Non-live missing-history replay defaults to L10 as the analysis-request target, but shows no payload until analysis exists.
- Display toggles can hide authorized features but cannot create stronger help.
- Used Level is monotonic within the current viewing/session context.
- Review/analysis display selections must not mutate live Used Level, Assistance Load, Used Offset, ECR, result, or matchmaking state.

## Implementation Shape

- `AnalysisMemory.RetentionPolicy` owns the default `10` and `100` limits.
- `UserAnalysisMemory` stores recent-game history and requested-analysis history.
- `FullGameAnalysisKey` allows different White/Black analysis levels for the same game without overwriting other requested analyses.
- `AnalysisFrameLookup` reports whether a payload exists or an analysis request is required.
- `ModeNeutralOverlayPolicy` defines shared overlay shell behavior and Set Level / Used Level handling across surfaces.

## Deferred Integration

This plan implements the EvenChess-owned memory and display-policy framework. A later database/UI integration should wire it into the native Lichess game-history and analysis controllers with patch-map entries for any upstream/core files touched.
