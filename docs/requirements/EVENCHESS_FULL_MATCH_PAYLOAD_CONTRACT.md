# EvenChess Full-Match ECE Payload Contract

Status: ECL-side contract aligned to the ECE v1.2 caller contract.

Purpose: allow EvenChess-Lichess to request, store, replay, and summarize complete match review payloads using the same canonical full-match format across the site.

## Ownership

ECE owns chess truth and full-match analysis generation.

ECL owns game history authority, display authorization, saved-game retention, tokens, review UI, and storage of sanitized display payloads.

The browser must not call ECE directly. Browser review controls call ECL. ECL calls ECE server-to-server.

## Full-Match Request

Endpoint:

```text
POST /v1/ece/full-match
```

`POST /v1/ece/game-review` is a legacy-compatible alias during migration. ECL should prefer `/v1/ece/full-match`.

Request body:

```json
{
  "request": {
    "mode": "full_match",
    "request_id": "game123-review-1",
    "game": {
      "game_id": "game123",
      "initial_fen": "start-or-first-fen",
      "pgn": "optional pgn",
      "moves": ["e2e4", "e7e5"],
      "fen_history": ["fen-after-ply-0", "fen-after-ply-1"],
      "result": "1-0",
      "termination": "checkmate"
    },
    "rating_type": "ecr",
    "white_rating_input": 1500,
    "black_rating_input": 1500,
    "review_level": 10,
    "use_ai": 0,
    "custom": {
      "opening": 0,
      "instructions": 0
    },
    "live_ece_snapshots": [
      {
        "ply": 12,
        "fen": "fen-at-ply-12",
        "side_to_move": "white",
        "white_output_ref": "optional-saved-white-ref",
        "black_output_ref": "optional-saved-black-ref"
      }
    ]
  }
}
```

## Full-Match Response

ECE returns one canonical `evenchess_full_game` object. Each `turns[]` item represents one board state, ordered from the initial position at ply 0 through each post-move position. Each turn embeds the normal public board-state payload at `turns[].ece_payload`.

```json
{
  "schema": {
    "name": "evenchess_full_game",
    "version": "1.0"
  },
  "request_echo": {
    "request_id": "game123-review-1",
    "game_id": "game123",
    "review_level": 10
  },
  "evenchess_full_game": {
    "format_name": "evenchess_full_game",
    "format_version": "1.0",
    "game_id": "game123",
    "turn_payload_format": {
      "schema": "ece_full_match_turn_payload_v1",
      "sequence": "turns is ordered from initial position at ply 0 through each post-move board state.",
      "fen_field": "turns[].fen",
      "payload_field": "turns[].ece_payload"
    },
    "turns": [
      {
        "ply": 12,
        "move_number": 6,
        "side_to_move": "white",
        "fen": "fen-at-ply-12",
        "fen_before_move": "previous-fen",
        "fen_after_move": "fen-at-ply-12",
        "fen_role": "after_move",
        "move_played": "e2e4",
        "ece_payload": {
          "schema": {
            "name": "evenchess_engine_output"
          },
          "request_echo": {
            "input_fen": "fen-at-ply-12"
          },
          "side_outputs": {
            "white": {},
            "black": {}
          },
          "diagnostics": {},
          "unavailable": {}
        },
        "side_outputs": {
          "white": {},
          "black": {}
        }
      }
    ],
    "key_moments": [],
    "phase_review": {}
  },
  "diagnostics": {
    "status": "ok",
    "engine_version": "ece-1.2.0"
  },
  "unavailable": {}
}
```

`frames` and `move_outputs` may be accepted by ECL only as temporary legacy aliases. New ECE output and ECL storage/interchange should use `evenchess_full_game.turns[]`.

## Full-Match Summary Request

Endpoint:

```text
POST /v1/ece/full-match-summary
```

Input:

```json
{
  "request": {
    "mode": "full_match_summary",
    "request_id": "summary_game123",
    "user_id": "user123",
    "user_side": "white",
    "use_ai": 1,
    "full_match": {
      "format_name": "evenchess_full_game",
      "format_version": "1.0",
      "game_id": "game123",
      "turns": [],
      "key_moments": [],
      "phase_review": {}
    }
  }
}
```

`full_match` must be the canonical `evenchess_full_game` object returned by `/v1/ece/full-match` or assembled by ECL in the same format. Instruction text and AI provider settings stay inside ECE and are not supplied by ECL.

Output:

```json
{
  "schema": {
    "name": "ece_full_match_summary",
    "version": "1.0"
  },
  "request_echo": {
    "request_id": "summary_game123",
    "mode": "full_match_summary",
    "user_id": "user123",
    "user_side": "white",
    "ai_used": true,
    "ai_calls_made": 1
  },
  "full_match_summary": {
    "source": "ai_text_summary",
    "summary_type": "full_match_summary",
    "ai_used": true,
    "validated": true,
    "summary_text": "Overall game summary text.",
    "what_went_well": [],
    "mistakes": [],
    "training_focus": [],
    "next_steps": []
  },
  "diagnostics": {
    "status": "ok"
  },
  "unavailable": {}
}
```

When AI is unavailable or not requested, `full_match_summary` may be `0` and diagnostics must explain the display-safe status.

## ECL Storage Rule

ECL accepts canonical `evenchess_full_game.turns[]` and converts each `turns[].ece_payload.side_outputs.<side>` object into the same approved `EvenChessLiveOverlay` display payload used during live play.

For each saved game, side, FEN, and ply:

- store only sanitized approved display payloads;
- store one canonical payload per side/FEN/ply;
- the highest delivered level wins;
- proposed-move previews are not stored in match history;
- potential-move display data is suppressed until a separate server-authorized reveal is used;
- level-10 full-match review frames may upgrade lower live frames for the same FEN;
- replay/history reads the stored payload by move/ply and lets the user adjust local display toggles without recalculation.

## Forbidden Response Fields

ECE must not return these in public full-match payloads:

- raw provider output;
- raw Stockfish lines;
- raw prompts;
- raw AI provider responses;
- provider paths;
- local filesystem paths;
- API keys or secrets;
- public `position`;
- public `shared_calculations`;
- anything that attempts to mutate game result, live Used Level, Assistance Load, Used Offset, ECR, or matchmaking state.

## Missing History Behavior

If ECL has no stored frame for a move, the review shell remains available but shows no payload. The user may request full-match level-10 review, which calls `/v1/ece/full-match` and stores returned turns.
