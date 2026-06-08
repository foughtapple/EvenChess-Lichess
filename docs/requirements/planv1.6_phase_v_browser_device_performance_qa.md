# EvenChess Plan v1.6 Phase V - Browser, Device, and Performance QA

## Phase Goal

Verify public EvenChess usability across realistic browsers, devices, viewport sizes, zoom levels, and network conditions.

Phase V must prove that EvenChess feels like a polished Lichess-integrated product, not a local test harness. The key user-facing risks are board playability, overlay stability, coach-card layout stability, WebSocket/update behavior, and latency tolerance.

## Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- Appendix K: Display Engine, Overlays, and Cards
- Appendix O: Offset Count, Hanging, Threats, and Pins
- Appendix V: Testing and QA Acceptance
- `docs/requirements/planv1.6_phase_j_game_policy_live_assistance_authority.md`
- `docs/requirements/planv1.6_phase_l_proposed_potential_consumables.md`
- `docs/requirements/planv1.6_phase_m_analysis_memory_review_modes.md`
- `docs/requirements/planv1.6_phase_t_staging_environment.md`
- `docs/requirements/planv1.6_phase_u_automated_test_matrix.md`

## Browser Coverage

Minimum browser coverage:

- Chrome desktop;
- Edge desktop;
- Firefox desktop;
- Safari desktop where feasible;
- Chrome mobile emulation;
- Safari/iOS mobile or closest feasible emulator/manual test;
- tablet viewport.

Each browser run must include:

- public home/search;
- online EvenChess search;
- computer game;
- round board move input;
- overlays after ECE payload;
- move-triggered overlay update without refresh;
- proposed move legal/illegal states;
- potential move own/opponent reveals;
- level selector scrolling/toggles;
- coach card/eval strip;
- game end/replay where available.

## Device and Viewport Matrix

Required viewport checks:

| Target | Width/height examples | Required proof |
| --- | --- | --- |
| Small mobile | 360x740, 390x844 | board playable, controls reachable, no overlap |
| Large mobile | 430x932 | level/coach UI usable, no clipped text |
| Tablet | 768x1024, 1024x768 | board/card layout stable |
| Desktop small | 1280x720 | board, eval bar, level card, coach card visible and aligned |
| Desktop standard | 1440x900, 1920x1080 | requested board ratios hold |
| Zoomed desktop | 90%, 100%, 110%, 125% | eval bar/card ratios stay tied to board as far as practical |

The eval bar must track the board height. The level card must sit under the native side card where present, share its side-column width, bottom-align with the board, and scroll internally. The coach card top must align with the board top on desktop layouts where the right card column is present.

## Board Playability Requirements

The board must remain playable under all EvenChess overlays.

QA must prove:

- pieces can be clicked/dragged/moved with overlays visible;
- overlay layers do not intercept normal Chessground input;
- legal move highlights/premove behavior are not broken;
- clocks remain visible and functional;
- native turn/move cards remain visible;
- game end flow works;
- normal non-EvenChess games remain unaffected.

Board playability is a release blocker. If users cannot move pieces after overlays render, public release must stop.

## Overlay and Layout Stability Requirements

QA must prove:

- overlays update after moves without requiring browser refresh;
- during ECE refresh, previous stale square visuals do not remain on the wrong FEN;
- stable shell/card DOM prevents visible flicker where feasible;
- coach text holds during opponent turn and updates on the student's turn;
- toggles remain persistent during the game;
- toggling controls does not scroll the level card to the top;
- board overlay atoms are not duplicated as coach-side text chips;
- Summary/Plan/coach cards reserve enough space and do not jump;
- eval strip remains visible at top of coach card;
- offset count markers, shields, hanging markers, threat arrows, and pin markers display at the required square locations;
- student/opponent threat toggles affect the correct arrows;
- loose/hanging toggles affect the correct markers;
- proposed/ potential preview states clear only on the correct game/FEN/move transitions.

## Network and Failure Conditions

Required network/failure QA:

- normal ECE latency;
- slow quick payload;
- slow deep payload;
- failed quick payload;
- failed deep payload;
- ECE restart during game;
- AI timeout when AI text is enabled;
- WebSocket reconnect during game;
- browser refresh during game;
- duplicate tab during game;
- stale payload arrival after a newer board state;
- proposed move timeout;
- potential reveal timeout.

Expected behavior:

- board remains playable;
- no higher-level or unauthorized data appears;
- stale overlays are suppressed;
- old coach text may be held only when policy allows;
- error states are user-safe and not debug-heavy;
- audit/telemetry records failure/suppression where required.

## Performance Budgets

Initial public-beta budgets must be measured in staging and may be adjusted after real data.

Recommended starting budgets:

- initial public page load: under 3 seconds on normal broadband;
- round page interactive: under 3 seconds after navigation on standard desktop;
- overlay render/update after payload arrival: under 150 ms target, under 300 ms warning;
- quick ECE response visible update: under 1 second target on healthy staging;
- deep ECE response: async/deferred and must not block board input;
- proposed move response: under 2 seconds target;
- potential reveal response: under 2 seconds target;
- no sustained memory growth across a 30-minute game/replay session;
- overlay update should not cause repeated layout shifts visible to the user.

Performance evidence must include:

- asset bundle size;
- round page load timing;
- overlay render timing;
- ECE quick/deep latency;
- proposed/potential latency;
- memory observations in long session;
- CPU observations during overlay-heavy test payload.

## Accessibility and Keyboard Requirements

QA must verify:

- level dropdown is keyboard accessible;
- level toggles are keyboard accessible;
- proposed/potential buttons are keyboard accessible;
- buttons have useful accessible labels;
- icon-only controls have labels/tooltips where needed;
- focus order is logical;
- text contrast is sufficient;
- screen-reader labels do not expose debug/internal payload information;
- TTS/read-aloud, if enabled, reads only the visible authorized coach text and is tied to audit identity.

## WebSocket and Round Update Requirements

QA must prove:

- Lichess-provided WebSocket/round update plumbing remains intact;
- EvenChess does not rebuild or bypass WebSocket ownership;
- move updates trigger ECE refresh path correctly;
- browser refresh resumes the current game state without losing server-owned Used Level or consumed counts;
- opponent move updates do not overwrite local coach text incorrectly;
- stale/late payloads are ignored or suppressed;
- normal Lichess socket behavior remains unchanged for non-EvenChess games.

## Screenshot and Evidence Set

Required evidence before public beta:

- desktop board with eval bar, level card, coach card aligned;
- mobile board with usable controls;
- overlay-rich Test ECE payload;
- real ECE payload after a move without refresh;
- proposed legal preview;
- proposed illegal/no-change state;
- potential own reveal;
- potential opponent reveal;
- toggles off/on without card jump;
- bot fallback game if enabled;
- normal Lichess game without EvenChess UI regression;
- browser console with no release-blocking errors;
- network/failure state screenshot or log.

## Current Implementation State

### Existing Foundations

The current repo has some Phase V foundations:

- UI tests exist for round overlays and Test Ground move bridge behavior.
- Phase U defines the automated test matrix and browser/staging evidence requirements.
- Phase T defines staging smoke, restart, and failure drills.
- V2 requirements define exact board layout rules for eval bar, level card, coach card, offset markers, hanging markers, threats, pins, proposed moves, and layout stability.
- Test ECE fixture includes overlay-rich payloads for visual regression testing while real ECE is in development.

### Not Yet Release-Proven

Phase V is not release-complete until:

- browser automation reliably plays moves through Chessground in staging/local;
- screenshots/videos prove overlay update after move without refresh;
- desktop/mobile/zoom layout screenshots are captured;
- slow/failed ECE behavior is tested;
- WebSocket reconnect/refresh/duplicate-tab behavior is tested;
- accessibility checks are run;
- performance budgets are measured and met or explicitly accepted;
- normal Lichess regression smoke is captured;
- release-blocking visual defects have tracked issues.

## QA Checklist

Before public beta, QA must mark each item:

- `pass`;
- `fail-release-blocking`;
- `fail-non-blocking-with-issue`;
- `not-applicable-with-reason`.

Checklist:

1. Chrome desktop complete.
2. Edge desktop complete.
3. Firefox desktop complete.
4. Safari desktop or documented substitute complete.
5. Mobile viewport complete.
6. Tablet viewport complete.
7. Zoom matrix complete.
8. Board playability under overlays complete.
9. Overlay-after-move without refresh complete.
10. Layout stability complete.
11. Slow/failed ECE complete.
12. WebSocket reconnect complete.
13. Proposed/potential controls complete.
14. Accessibility keyboard/labels complete.
15. Performance measurements complete.
16. Normal Lichess regression complete.

## Patch Map Impact

Future Phase V fixes may touch round layout, round UI, overlay renderer, socket handling, CSS, or accessibility labels. Runtime Lichess seam changes must be patch-mapped and entered into the integration log.

This Phase V documentation pass does not itself change runtime code.

## Phase V Acceptance Status

Phase V is conducted as a readiness and QA-plan pass.

Status:

- Browser/device/performance QA matrix and evidence requirements are defined.
- Release readiness remains blocked until the QA matrix is executed and evidence confirms board playability, stable overlays, responsive layout, failure handling, accessibility, performance, and normal Lichess regression behavior.

## Phase W Entry Criteria

Before backup/recovery/maintenance can be treated as final release support work, Phase V should provide:

- evidence of actual staging/browser behavior;
- list of data-producing flows exercised in QA;
- confirmed retention-sensitive flows: live history, requested analyses, tokens, audit, calibration;
- performance observations that inform backup/retention sizing.
