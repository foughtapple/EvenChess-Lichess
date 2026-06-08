# EvenChess-Lichess Version 1.2 Implementation Plan

**Status:** Planning source for prompt-by-prompt implementation
**Created:** 2026-05-29
**Repository:** `foughtapple/EvenChess-Lichess`

## 1. Version 1.2 Product Direction

Version 1.2 refines the Version 1.1 direction.

EvenChess should feel like a polished, modern Lichess-powered chess platform with a deep blue EvenChess visual identity, not like a separate custom chess app and not like a stripped-down lobby.

Lichess remains the base platform for accounts, board UI, game lifecycle, legal moves, clocks, study, opening explorer, analysis, puzzles, social/community features, moderation foundations, admin foundations, and operational infrastructure. EvenChess adds the level overlay system, server-authoritative coaching, ECR, AI explanation/summaries, monetisation, and EvenChess-specific settings/admin controls.

The public product may expose Lichess-provided feature areas through EvenChess-branded navigation and layout. Rated live EvenChess games still use EvenChess mode metadata, Set Level, Used Level, Assistance Load, Used Offset, ECR, audit logging, and server-owned coaching permission. Normal Lichess ratings and pools must not become ECR.

## 2. Requirement Classification

| Area | Classification | Direction |
| --- | --- | --- |
| Accounts, sessions, profiles, baseline settings shell | Lichess-provided | Reuse and extend with an EvenChess settings section. |
| Game board, clocks, legal moves, move history, PGN, study, opening explorer, analysis, puzzles | Lichess-provided | Keep Lichess-owned; add EvenChess overlays and AI panels only through thin adapters. |
| Deep blue EvenChess visual identity | EvenChess-specific | Add theme tokens/components without replacing Lichess layout patterns. |
| Level overlays, coaching cards, Set Level, Used Level, Assistance Load, Used Offset, ECR | EvenChess-specific | Server-owned EvenChess logic; client renders authorized payloads only. |
| AI coach in live games, study, openings, analysis, and summaries | Adapted to Lichess fork | Build on Lichess analysis/study/opening surfaces and Version 1.1 AI contracts. |
| Text-to-Speech Coach | EvenChess-specific | Optional user-controlled reading of already-authorized coach text; no extra advice or client-side permission decisions. |
| User EvenChess settings section | Adapted to Lichess fork | Extend Lichess settings/preferences with a namespaced EvenChess section. |
| Admin/backend EvenChess settings | Adapted to Lichess fork | Use Lichess admin/settings patterns with secret-safe server-only config handling. |
| Hiding most Lichess feature areas from the public product | Superseded by Version 1.2 | Preserve and expose Lichess-provided capabilities where compatible with EvenChess. |
| TTS provider choice, exact voices, paid/free TTS limits | Unresolved | Implement provider seams and safe defaults; product owner can choose provider/limits. |

## 3. Non-Negotiable Constraints

- Do not rebuild Lichess-provided chess basics.
- Do not delete Lichess study, opening explorer, analysis, puzzles, accounts, settings, admin, moderation, or game lifecycle features.
- EvenChess-rated live help remains disclosed, capped by Set Level, audited, and priced into ECR.
- Client-side code never decides coaching permission.
- Every live coaching render and every TTS reading of coaching content must be tied to an audited server-authorized coaching payload.
- Study/opening/analysis AI assistance may be richer than live rated coaching only when it is clearly outside rated live play and separated from ECR updates.
- API keys and model/provider secrets must never be sent to browser code.
- Premium, ads, tokens, campaign variants, and TTS must not provide stronger live rated help.
- Each Version 1.2 phase must update `docs/integration/EVENCHESS_LICHESS_INTEGRATION_LOG.md`.
- Any upstream/core Lichess file edit must be recorded in `docs/requirements/EVENCHESS_LICHESS_PATCH_MAP.md`.

## 4. Version 1.2 Phase Map

| Phase | Name | Primary outcome | Likely Lichess seams | Tests/checks |
| --- | --- | --- | --- | --- |
| V1.2-A | Product direction and documentation reset | Record Version 1.2 as Lichess-powered EvenChess with preserved feature areas. | Requirements/docs only. | `git diff --check`. |
| V1.2-B | Deep blue design system and shell polish | Modern EvenChess theme tokens, layout rules, and polished public shell direction. | CSS/theme assets, homepage shell, top nav. | Theme/unit tests, compile, visual smoke. |
| V1.2-C | Lichess feature navigation restoration | Expose Lichess feature areas through EvenChess-branded navigation without routing rated play to normal chess pools. | Top nav, home, route labels/links. | Public shell tests, route smoke, normal chess regression where touched. |
| V1.2-D | User EvenChess settings section | Add a namespaced EvenChess section in user settings/preferences. | Preferences/settings controllers/views/storage. | Settings persistence tests, permission tests, compile. |
| V1.2-E | Admin/backend EvenChess settings | Add admin-only EvenChess config surfaces for provider keys, model flags, safety toggles, and ops controls. | Admin/dev settings, secure config, permission checks. | Admin permission tests, secret non-exposure tests, compile. |
| V1.2-F | Live game overlay integration polish | Attach Version 1.1 coaching card/view-model contracts to real Lichess board surfaces. | Round/play UI, WebSocket payload adapter, chessground-adjacent rendering. | Live overlay tests, mobile layout checks, normal move input regression. |
| V1.2-G | Study, analysis, and opening AI overlays | Add EvenChess AI coach panels/cards to study, analysis, and opening explorer surfaces. | Study views/socket, analysis view, opening controller/view. | Study/opening adapter tests, AI policy tests, browser smoke. |
| V1.2-H | Text-to-Speech Coach | Let users turn on coach reading for authorized coaching cards and eligible study/opening explanations. | User settings, live/study/opening overlay UI, optional TTS provider seam. | TTS policy tests, accessibility tests, no-extra-advice tests. |
| V1.2-I | Play/search/matchmaking production integration | Replace anchor/demo flows with real EvenChess play/search routes, level selectors, and ECR-safe pairing. | Lobby, challenge/pool/search/create-game seams. | Matchmaking tests, ECR isolation tests, normal rating regression. |
| V1.2-J | Subscription, token, ad, and account UX integration | Surface entitlements, tokens, rewarded ads, summary quotas, and payment entrypoints cleanly. | Account pages, billing/plan patterns, game start/token gates. | Entitlement tests, token settlement tests, fairness non-effect tests. |
| V1.2-K | Admin operations dashboard integration | Build practical admin dashboards for overlays, AI, engine, TTS, tokens, incidents, and calibration health. | Admin routes/views, ops dashboards, audit search. | Admin dashboard tests, audit visibility tests, incident control tests. |
| V1.2-L | Production polish, QA, and release evidence | End-to-end visual, mobile, accessibility, performance, settings, admin, and upstream-sync readiness pass. | All touched seams. | `evenchess/test`, root compile, browser smoke, visual QA, go/no-go evidence. |

## 5. Phase Details

### V1.2-A - Product Direction and Documentation Reset

Scope:

- Add Version 1.2 product direction to the live requirements.
- Mark the Version 1.1 "EvenChess-only" direction as refined, not deleted.
- Record that Lichess feature areas should remain available when they can be safely branded/adapted for EvenChess.
- Add Version 1.2 phase prompts to Appendix T.

Acceptance:

- Main requirements, Appendix T, Appendix Z, and integration log governance all agree.
- No code changes.

### V1.2-B - Deep Blue Design System and Shell Polish

Scope:

- Define a restrained deep blue EvenChess theme that still feels native to Lichess.
- Prefer Lichess layout density and controls over a marketing-card-heavy redesign.
- Create reusable theme tokens and component guidelines for buttons, cards, coaching surfaces, badges, settings panels, and admin status chips.
- Polish homepage/top navigation so the first viewport clearly says EvenChess while still exposing platform capability.

Acceptance:

- The theme does not break Lichess board sizing, move input, nav, or responsive layout.
- Text fits on mobile and desktop.
- Visual QA screenshots are captured for desktop and mobile.

### V1.2-C - Lichess Feature Navigation Restoration

Scope:

- Rework the Version 1.1 public shell/nav so Lichess feature areas are not accidentally hidden.
- Keep EvenChess as the product identity while exposing routes such as play, learn, puzzles, study, openings, analysis, community, and profile/account areas where safe.
- Ensure live rated game starts still route to EvenChess mode/search, not normal Lichess rated pools.

Acceptance:

- Users can reach Lichess-provided learning and exploration features.
- Normal Lichess capabilities remain available for reuse/regression.
- Rated public play entrypoints remain EvenChess/ECR owned.

### V1.2-D - User EvenChess Settings Section

Scope:

- Add an EvenChess section inside user settings/preferences.
- Include player-facing controls such as default Set Level, overlay density, coaching card verbosity, board highlight intensity, Offset Count display preference, AI summary preferences, TTS Coach enabled/disabled, TTS voice/rate/volume, study/opening AI overlay defaults, and privacy/telemetry preferences where allowed.
- Store settings server-side and expose only safe display/config values to the client.

Acceptance:

- Settings persist per user.
- Defaults are safe for new users.
- Settings cannot raise live coaching strength beyond server policy.

### V1.2-E - Admin/Backend EvenChess Settings

Scope:

- Add admin-only EvenChess backend settings.
- Include OpenAI provider/model/key status, TTS provider/key status, Stockfish profile controls, per-surface AI enablement, overlay feature flags, token/ad/payment provider switches, campaign kill switches, cost limits, rate limits, audit retention settings, and incident pause controls.
- Keep secrets server-side only. Admin pages may show configured/missing/rotated status, not raw secrets unless an existing safe Lichess pattern supports secret entry.

Acceptance:

- Only authorized admins can view/change backend EvenChess settings.
- API keys are never rendered into browser config or logs.
- Config changes are auditable and rollbackable.

### V1.2-F - Live Game Overlay Integration Polish

Scope:

- Connect the Version 1.1 live coaching and overlay contracts to real Lichess round/play surfaces.
- Render cards/highlights without interfering with legal move input, clocks, premoves, keyboard controls, or mobile board fit.
- Preserve server authority for every render, clear, suppression, expansion, and TTS-readable message.

Acceptance:

- Live board remains usable without layout shift or blocked input.
- Overlay stale-state clearing works.
- Normal non-EvenChess games do not receive EvenChess overlays.

### V1.2-G - Study, Analysis, and Opening AI Overlays

Scope:

- Add EvenChess AI coach affordances to Lichess study, analysis, and opening explorer surfaces.
- Build on existing Lichess analysis/study/opening data rather than duplicating those engines.
- Support chapter summaries, position explanations, opening plan explanations, mistake themes, and "explain this move" style cards where policy allows.
- Keep live rated restrictions separate from non-rated learning surfaces.

Acceptance:

- Study/opening/analysis overlays are clearly EvenChess-branded and optional.
- AI explanations are grounded in server-authorized truth/context.
- No provider secrets or unrestricted engine data are sent to the client.

### V1.2-H - Text-to-Speech Coach

Scope:

- Add optional Text-to-Speech Coach for live coaching cards and eligible study/opening/analysis explanations.
- The spoken text must be the same authorized/sanitized coach text shown in the UI, not a separate stronger advice channel.
- Add user controls for enablement, voice, speed, volume, queue behavior, and "mute during opponent turn" if useful.
- Add admin controls for provider selection, limits, kill switch, and cost monitoring.
- Support a browser speech-synthesis path where feasible and a server-side provider seam if higher-quality voices are later approved.

Acceptance:

- TTS is off by default unless product owner changes it.
- TTS events are auditable when tied to live rated coaching.
- TTS never leaks raw engine lines, hidden prompts, API keys, or stronger paid help.

### V1.2-I - Play/Search/Matchmaking Production Integration

Scope:

- Turn Version 1.1 matchmaking/search contracts into real EvenChess play flows.
- Replace anchor/demo CTAs with actual routes for level selection, time control, rated/unrated, token eligibility, queue state, and pairing confirmation.
- Use ECR and Set Level for public EvenChess search while preserving normal Lichess rating data separately.

Acceptance:

- Users can start an EvenChess game from public navigation.
- Server-owned metadata is persisted before coaching can render.
- ECR updates remain isolated from normal Lichess ratings.

### V1.2-J - Subscription, Token, Ad, and Account UX Integration

Scope:

- Connect Version 1.1 subscription/token/ad contracts to user-visible account and game-start flows.
- Add clear token balance, game token consumption/refund states, rewarded ad grant states, summary quota states, and plan feature descriptions.
- Keep Premium as convenience/quantity/summary value, never stronger live help.

Acceptance:

- Token and subscription UI is understandable and fair.
- Aborts/refunds and failed summaries do not consume incorrectly.
- Payment/ad/campaign state cannot mutate live coaching strength or ECR.

### V1.2-K - Admin Operations Dashboard Integration

Scope:

- Build EvenChess admin dashboards for AI health, TTS health, Stockfish/engine health, queue health, ECR calibration, token/ad/payment health, overlay failures, audit-ledger search, incident controls, and active config versions.
- Add operator actions for pause/no-rate/rollback where already defined by Version 1.1 contracts.

Acceptance:

- Admins can see active policy/model/config/engine/TTS versions.
- Incidents and pause states are auditable.
- Admin tools do not expose anti-cheat internals or raw provider secrets.

### V1.2-L - Production Polish, QA, and Release Evidence

Scope:

- Run full visual QA on the deep blue theme across homepage, play, study, opening, analysis, settings, admin, and mobile.
- Run accessibility checks for overlays and TTS.
- Run performance checks for overlays, AI/TTS latency, and board interaction.
- Re-run release hardening/go-no-go evidence and update integration records.

Acceptance:

- `evenchess/test` passes.
- Root compile passes.
- Browser smoke checks cover main public routes and the live overlay path.
- Patch map and integration log are current.
- Release evidence lists unresolved provider/product decisions before launch.

## 6. Open Product Decisions Before Implementation

| Decision ID | Decision needed | Default for Version 1.2 planning |
| --- | --- | --- |
| DEC-V12-001 | Exact TTS provider strategy. | Browser speech first where feasible; server-side provider seam for later approval. |
| DEC-V12-002 | Which Lichess feature areas must be first-class in initial public nav. | Include play, study, openings, analysis, puzzles, account/settings; inspect others during Phase C. |
| DEC-V12-003 | Whether normal non-assisted game starts are publicly available or only internal/regression. | Rated public starts remain EvenChess; preserve internals and expose learning/exploration features. |
| DEC-V12-004 | Admin secret-entry mechanism. | Prefer environment/secret store status display; do not render raw secrets. |
| DEC-V12-005 | TTS availability by plan. | Same coaching strength for all; any paid difference may only be quota/voice/convenience if approved. |

## 7. Prompt Starters

Use these exact prompts to run phases one at a time:

```text
Implement Version 1.2 Phase A
Implement Version 1.2 Phase B
Implement Version 1.2 Phase C
Implement Version 1.2 Phase D
Implement Version 1.2 Phase E
Implement Version 1.2 Phase F
Implement Version 1.2 Phase G
Implement Version 1.2 Phase H
Implement Version 1.2 Phase I
Implement Version 1.2 Phase J
Implement Version 1.2 Phase K
Implement Version 1.2 Phase L
```

Each phase must read this plan, the main requirements, the Stage 1 handover, Appendix T, Appendix Z, the relevant appendices, the patch map, the upstream sync process, and the integration log before editing.
