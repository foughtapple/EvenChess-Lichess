# Appendix J — EvenChess-Lichess to EvenChessEngine Integration Contract

**Document status:** Version 2 replacement Appendix J for the EvenChess-Lichess requirements suite
**Scope:** How EvenChess-Lichess talks to the separate EvenChessEngine service
**Does not define:** Internal EvenChessEngine implementation requirements
**Primary consumer:** Codex working inside `foughtapple/EvenChess-Lichess`

---

## J.1 Purpose

EvenChess-Lichess must treat EvenChessEngine, shortened to **ECE**, as a separate private server-side engine service.

EvenChess-Lichess must not reimplement ECE chess logic in the Lichess fork.

EvenChess-Lichess is responsible for:

- knowing the game;
- knowing the players;
- knowing the current FEN;
- knowing each side's authorised EvenChess level;
- knowing match/rating context;
- deciding when ECE should be called;
- auditing coaching render/use;
- displaying only server-authorised payloads.

ECE is responsible for:

- receiving a known input from EvenChess-Lichess;
- running deterministic chess/provider calculations;
- applying level-gating to White and Black side payloads;
- returning side-specific payloads for EvenChess-Lichess to display, cache, audit, or ignore.

---

## J.2 Local ECE Address on This PC

For current local development, EvenChessEngine runs as a local HTTP service.

Default local base URL:

```text
http://127.0.0.1:8787
```

Current health check:

```http
GET http://127.0.0.1:8787/health
```

Current board-state endpoint:

```http
POST http://127.0.0.1:8787/v1/ece/board
Content-Type: application/json
```

The ECE service may allow the host and port to be changed by environment variables later, but EvenChess-Lichess local development should default to:

```text
ECE_BASE_URL=http://127.0.0.1:8787
```

EvenChess-Lichess must never call ECE from browser/client code. ECE calls must be server-to-server only.

---

## J.3 Integration Boundary

EvenChess-Lichess may call ECE from backend/server code only.

Browser clients must not receive:

- ECE provider URLs;
- ECE internal provider configuration;
- API keys;
- Stockfish provider paths;
- Syzygy tablebase paths;
- Maia model paths;
- Lichess eval-cache paths;
- raw ECE provider output;
- unrestricted engine output.

EvenChess-Lichess should receive ECE output through its own backend, then decide what to expose to the browser based on:

- match contract;
- Set Level;
- Used Level;
- game mode;
- audit state;
- player side;
- payload validity.

---

## J.4 Required ECE Health Check

EvenChess-Lichess local startup/status tooling should verify ECE is reachable before live ECE overlays are expected to work.

Request:

```http
GET http://127.0.0.1:8787/health
```

Expected successful response shape:

```json
{
  "status": "ok",
  "service": "EvenChessEngine",
  "mode": "mock",
  "openai_configured": false,
  "stockfish_configured": false
}
```

EvenChess-Lichess must treat missing/unreachable ECE as non-fatal for ordinary Lichess foundation behavior.

If ECE is unavailable:

- ordinary page/game loading must not break;
- EvenChess live coaching payloads should be unavailable;
- UI may show coaching unavailable;
- diagnostics/logs should identify ECE unreachable.

---

## J.5 Board-State Call

EvenChess-Lichess calls board-state mode when it needs a coaching/overlay payload for a current board state.

Current endpoint:

```http
POST http://127.0.0.1:8787/v1/ece/board
Content-Type: application/json
```

Request body:

```json
{
  "request": {
    "mode": "board_state",
    "request_id": "ec_game_123_ply_18",
    "input_fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
    "rating_type": "ecr",
    "white_rating_input": 1000,
    "black_rating_input": 1200,
    "white_level": 4,
    "black_level": 2,
    "use_ai": 0,
    "custom": {
      "opening": 0,
      "instructions": 0
    }
  }
}
```

---

## J.6 Board-State Request Field Rules

| Field | EvenChess-Lichess responsibility |
|---|---|
| `request.mode` | Send `board_state`. May be omitted only if ECE default is stable, but explicit is preferred. |
| `request.request_id` | Generate a unique ID tied to game, ply/FEN, mode, and render request. |
| `request.input_fen` | Send the current authoritative FEN known to the EvenChess-Lichess server. |
| `request.rating_type` | Send `ecr` for normal EvenChess rated games, or `unknown` if not available. |
| `request.white_rating_input` | Send White's ECR/rating context, or `0` if unknown. |
| `request.black_rating_input` | Send Black's ECR/rating context, or `0` if unknown. |
| `request.white_level` | Send White's authorised Set Level/current allowed level for this position. |
| `request.black_level` | Send Black's authorised Set Level/current allowed level for this position. |
| `request.use_ai` | Send `1` only if EvenChess policy allows AI text for this mode and ECE is configured. Otherwise send `0`. |
| `request.custom.opening` | Send `0` unless the user/session has requested a specific opening. |
| `request.custom.instructions` | Send `0` unless server-approved custom wording instructions exist. |

EvenChess-Lichess must not send API keys to ECE in the request body.

---

## J.7 Board-State Response Contract Expected by EvenChess-Lichess

ECE board-state response must be treated as side-gated output.

Expected public response shape:

```json
{
  "schema": {},
  "request_echo": {},
  "side_outputs": {
    "white": {},
    "black": {}
  },
  "diagnostics": {},
  "unavailable": {}
}
```

ECE board-state response must not include:

```json
{
  "position": {},
  "shared_calculations": {}
}
```

Reason:

- EvenChess-Lichess already has the FEN;
- side to move is derived from the FEN;
- ECE should return only what each side is allowed to receive;
- shared chess facts must not bypass level gating;
- all displayable facts must be mapped into `side_outputs.white` and `side_outputs.black`.

---

## J.8 Side Output Use by EvenChess-Lichess

Each side payload is already level-gated by ECE.

EvenChess-Lichess must still enforce server-side display authority.

Expected side shape:

```json
{
  "side": "white",
  "student_side": "white",
  "opponent_side": "black",
  "level": {
    "requested_level": 4,
    "delivered_level": 4,
    "defaulted": false
  },
  "is_side_to_move": true,
  "summary": 0,
  "immediate_warning": 0,
  "plan": 0,
  "candidate_moves": 0,
  "evaluation": 0,
  "opening": 0,
  "overlays": {
    "trade_status": {
      "hanging_not_attackable": [],
      "offset_count": [],
      "advantage_offset_value": 0,
      "disadvantage_offset_value": 0
    },
    "threats": {
      "student_threats": [],
      "opponent_threats": []
    },
    "pinned_pieces": {
      "student_pinned": [],
      "opponent_pinned": []
    }
  },
  "raw_deterministic": {
    "summary_inputs": [],
    "modules_used": []
  }
}
```

EvenChess-Lichess must display a player's payload from that player's side output only.

Examples:

- White player display uses `side_outputs.white`.
- Black player display uses `side_outputs.black`.
- Review surfaces may show one or both side outputs depending on review mode and entitlement.
- Live opponent payload must not be shown to a player unless the game/review mode explicitly allows it.

---

## J.9 Stale Payload Rejection

ECE no longer needs to return a public `position` object.

EvenChess-Lichess must reject stale ECE payloads using its own request tracking.

Minimum checks before using an ECE response:

1. `request_echo.request_id` matches the outstanding ECE request.
2. `request_echo.input_fen` equals the FEN used for that request.
3. `request_echo.white_level` and `request_echo.black_level` match the authorised levels used for that request.
4. The current game state still matches the FEN/request context.
5. `diagnostics.status` is acceptable for display.

Recommended cache key:

```text
mode + input_fen + white_level + black_level + use_ai + custom.opening + custom.instructions + engine_version
```

EvenChess-Lichess may hash this cache key internally.

---

## J.10 Diagnostics Handling

ECE may return these statuses:

| Status | EvenChess-Lichess action |
|---|---|
| `ok` | Payload may be used if request validation checks pass. |
| `partial` | Payload may be used, but UI/logs may show degraded provider status. |
| `invalid_request` | Do not display coaching. Log request-shape issue. |
| `invalid_fen` | Do not display coaching. Log FEN issue. |
| `invalid_game` | Applies to full-game mode. Do not display full-game review. |
| `stockfish_unavailable` | Use lower-level deterministic payload if present; otherwise show unavailable. |
| `ai_unavailable` | Use deterministic fallback text if present; otherwise show no AI text. |
| `internal_error` | Do not display coaching. Log ECE failure. |

EvenChess-Lichess must not treat ECE diagnostics as browser-safe by default. Sanitize before showing to users.

---

## J.11 Live Game Board-State Timing

EvenChess-Lichess may call ECE:

- after a move is committed;
- when the board FEN changes;
- when a player opens/requests coaching;
- when review mode needs a stored/saved payload;
- when cache is missing or stale.

EvenChess-Lichess should avoid unnecessary repeated calls for identical inputs.

ECE calls must not block the legal move lifecycle, clocks, or core game state.

If ECE is slow or unavailable:

- game play continues;
- coaching payload can arrive later or be skipped;
- stale checks must prevent old payloads from rendering on a new position.

---

## J.12 Proposed-Move Integration

Proposed-move mode is for a player asking about one proposed move from the current FEN.

Target future endpoint:

```http
POST http://127.0.0.1:8787/v1/ece/proposed-move
Content-Type: application/json
```

If ECE instead supports proposed-move mode through a unified endpoint, EvenChess-Lichess must follow the ECE API version contract.

Request shape:

```json
{
  "request": {
    "mode": "proposed_move",
    "request_id": "ec_game_123_ply_18_pm_1",
    "input_fen": "string",
    "proposed_move_uci": "g1f3",
    "rating_type": "ecr",
    "white_rating_input": 1000,
    "black_rating_input": 1200,
    "white_level": 10,
    "black_level": 10,
    "use_ai": 0,
    "custom": {
      "opening": 0,
      "instructions": 0
    }
  }
}
```

EvenChess-Lichess must call proposed-move mode only when:

- there is exactly one proposed move;
- the move belongs to the side to move;
- the requester's live/review context allows proposed-move help;
- the current FEN has not changed.

ECE derives the moving side from FEN.

---

## J.13 Full-Game Review Integration

Full-game mode is for post-game EvenChess Match History generation.

Target future endpoint:

```http
POST http://127.0.0.1:8787/v1/ece/game-review
Content-Type: application/json
```

Input should provide enough data for ECE to process the game move by move.

Preferred request shape:

```json
{
  "request": {
    "mode": "game_review",
    "request_id": "ec_game_123_review_1",
    "game": {
      "game_id": "ec_game_123",
      "initial_fen": "startpos",
      "pgn": "optional PGN",
      "moves": [],
      "fen_history": [],
      "result": "1-0",
      "termination": "checkmate/resignation/timeout/draw/unknown"
    },
    "rating_type": "ecr",
    "white_rating_input": 1000,
    "black_rating_input": 1200,
    "review_level": 10,
    "use_ai": 0,
    "custom": {
      "opening": 0,
      "instructions": 0
    },
    "live_ece_snapshots": []
  }
}
```

EvenChess-Lichess must treat full-game output as post-game review only.

Full-game ECE output must not alter:

- game result;
- live Used Level;
- live Assistance Load;
- live Used Offset;
- ECR;
- matchmaking state.

---

## J.14 Provider Awareness

EvenChess-Lichess does not call Stockfish, Syzygy, Maia, opening-book, rules, or Lichess eval-cache providers directly for ECE coaching.

EvenChess-Lichess calls ECE.

ECE may then call its local providers:

- Stockfish provider;
- Syzygy tablebase provider;
- opening-book provider;
- Lichess eval-cache provider;
- rules/legal-move provider;
- Maia human-risk provider;
- AI text provider.

EvenChess-Lichess may display provider-derived output only after ECE normalizes and level-gates it into side outputs.

---

## J.15 Security Requirements

EvenChess-Lichess must not:

- expose ECE directly to browser clients;
- put ECE provider paths in frontend code;
- put API keys in ECE request payloads;
- trust client-supplied level values;
- trust client-supplied ECE payloads;
- let browser code decide coaching permission;
- let ECE output override match contract or fairness state.

EvenChess-Lichess must:

- derive levels server-side from the EvenChess match contract/settings;
- audit coaching render/use;
- check stale payloads;
- sanitize diagnostics before user display;
- keep ECE calls server-side.

---

## J.16 Local Developer Commands

Start ECE from the EvenChessEngine repo:

```powershell
cd "C:\Users\jayde\Documents\Chess apps\EvenChessEngine"
node src/server.js
```

Health check:

```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:8787/health
```

Sample board-state call:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://127.0.0.1:8787/v1/ece/board `
  -ContentType "application/json" `
  -Body (Get-Content fixtures/ece-v1-sample-input.json -Raw)
```

Equivalent curl:

```bash
curl http://127.0.0.1:8787/health

curl -X POST http://127.0.0.1:8787/v1/ece/board \
  -H "Content-Type: application/json" \
  --data @fixtures/ece-v1-sample-input.json
```

---

## J.17 Acceptance Criteria

1. EvenChess-Lichess calls ECE server-to-server only.
2. Local default ECE base URL is `http://127.0.0.1:8787`.
3. EvenChess-Lichess health/status tooling can check `GET /health`.
4. EvenChess-Lichess can call `POST /v1/ece/board`.
5. EvenChess-Lichess sends authoritative FEN, ratings, levels, and AI flag.
6. EvenChess-Lichess does not send API keys to ECE.
7. EvenChess-Lichess expects side-gated output under `side_outputs.white` and `side_outputs.black`.
8. EvenChess-Lichess does not expect public `position`.
9. EvenChess-Lichess does not expect public `shared_calculations`.
10. EvenChess-Lichess rejects stale ECE payloads using request ID, FEN echo, level echo, and current game state.
11. EvenChess-Lichess does not call Stockfish/Syzygy/Maia/opening/eval-cache providers directly for coaching.
12. EvenChess-Lichess can degrade gracefully if ECE is unavailable.
