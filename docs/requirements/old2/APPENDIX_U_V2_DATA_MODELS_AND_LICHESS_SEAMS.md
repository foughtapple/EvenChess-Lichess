# Appendix U — Data Models and Lichess Seams

## U.1 Purpose

This appendix defines logical data models and likely Lichess seams.

## U.2 Logical models

Required logical models include:

- EvenChessAccountState;
- EvenChessRating/ECR;
- EvenChessSearchRequest;
- EvenChessMatchContract;
- EvenChessGameMetadata;
- SetLevelPolicy;
- UsedLevelState;
- AssistanceLoadSummary;
- EceBoardPayload;
- EceGameHistory;
- EceFullGameReview;
- DisplayRenderEvent;
- ProposedMoveCheck;
- TokenLedger;
- ReviewTokenLedger;
- SavedGameRecord;
- EvenChessSettings;
- AdminFeatureFlag;
- PatchMapEntry.

## U.3 Lichess seams

Likely seams include:

- lobby setup modal and quick-pairing cards;
- seek/search service;
- challenge flow;
- round/game creation metadata;
- round WebSocket payloads;
- board overlay layer;
- analysis/replay page;
- study/opening explorer board surfaces;
- account/top-bar templates;
- settings/preferences;
- plans/subscription pages;
- admin/ops pages.

REQ-U-V2-001: Store EvenChess metadata in a way that is versioned and does not corrupt normal Lichess game assumptions.

REQ-U-V2-002: Avoid editing core game BSON/schema internals unless explicitly approved and patch-mapped.

REQ-U-V2-003: Every seam must be listed in integration log with rollback notes.
