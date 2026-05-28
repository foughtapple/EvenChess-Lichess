# EvenChess-Lichess Requirements Diff

**Suite:** EvenChess-Lichess Version 1

## 1. Executive diff

The old plan assumed EvenChess would build a chess platform. The new plan assumes Lichess/lila provides the chess platform, and EvenChess implements only the assisted-chess layer.

## 2. Old custom-platform requirements now provided by Lichess

| Old requirement area | New classification | Direction |
| --- | --- | --- |
| Legal move generation | Lichess-provided | Use lila/scalachess/chessops. Do not rebuild. |
| Base board UI | Lichess-provided | Use chessground/lila UI. Add overlays only. |
| Base clocks | Lichess-provided | Add assistance timing multipliers only. |
| Base game rooms | Lichess-provided | Hook/extend only for EvenChess mode. |
| Move history and PGN | Lichess-provided | Add assisted metadata/review; do not rebuild. |
| Ordinary account basics | Lichess-provided unless gap | Add token/subscription/account eligibility rules. |
| Challenge/search/play flows | Lichess-provided baseline | Adapt for ECR and Set Level. |
| Review/analysis foundation | Lichess-provided baseline | Add summaries and audit-aware review. |
| Mobile chess play surface | Lichess-provided baseline | Add mobile cards/overlays. |

## 3. Old requirements still required

Disclosed assisted identity; outside-help prohibition; server-authoritative coaching; Set Level, Used Level, Assistance Load, Used Offset; ECR; L0-L10 ladder; Offset Count; Target isolation; Stockfish server-side boundary; grounded AI; audited renders; monetisation fairness; onboarding tokens; Premium summary quotas; marketing config; telemetry/calibration; abuse/ops.

## 4. Old requirements adapted

| Old concept | New adaptation |
| --- | --- |
| Custom game mode model | Server-owned EvenChess metadata/flag/mode inside lila flow. |
| Custom matchmaking | Adapt lila pairing/search or add thin queue with ECR/Set Level. |
| Custom rating | Separate ECR; do not corrupt normal ratings. |
| Custom board overlays | Server-authorized payloads on existing board surface. |
| Custom engine service | Inspect lila/fishnet/analysis; add Engine Gateway wrapper/seam. |
| Custom AI summaries | Server-side provider boundary integrated with review surfaces. |
| Custom telemetry | EvenChess ledger plus lila-compatible logging/analytics. |
| Custom admin dashboard | Use existing admin/ops patterns where possible. |
| Custom marketing site | Fork-specific configurable landing/funnel. |

## 5. Superseded

Building a complete platform from scratch; rebuilding rules/rooms/clocks/board/PGN/history; implementing product features before local lila boot; replacing the whole UI; duplicate Q/R mappings; Appendix AA outside A-Z.

## 6. Uncertain decisions

Variant/mode representation, assisted PGN/export, first rated pools, payment provider, rewarded-ad provider, AI provider/model, branding/legal posture, upstream sync cadence.

## 7. Rule

Codex must classify every old requirement as Lichess-provided, EvenChess-specific, adapted-to-fork, superseded, or uncertain before implementation.
