# EvenChess-Lichess Version 1.2 Release Evidence

**Version:** 1.2 Phase L
**Date:** 2026-05-29
**Status:** Phase L engineering evidence recorded; production launch is not yet approved.

## Summary

Version 1.2 Phase L adds the production-readiness evidence model, release evidence record, and final mobile polish checks for the deep-blue EvenChess shell.

Engineering readiness for this phase is complete when the recorded tests and route checks pass. Production launch remains blocked until product-owner/provider decisions are resolved, authenticated/admin visual QA is repeated with production-like accounts, and live provider telemetry is captured.

## Requirements Used

- `docs/evenchess/EVENCHESS_LICHESS_VERSION_1_2_PLAN.md` Phase L.
- `docs/requirements/APPENDIX_V_TESTING_QA_AND_ACCEPTANCE_CRITERIA.md`.
- `docs/requirements/APPENDIX_R_ADMIN_OPERATIONS_DASHBOARDS_AND_INCIDENT_RESPONSE.md`.
- `docs/requirements/APPENDIX_Q_EXPLOIT_ABUSE_AND_TRUST_CONTROLS.md`.
- `docs/requirements/APPENDIX_P_TELEMETRY_ANALYTICS_AND_CALIBRATION.md`.
- `docs/requirements/APPENDIX_U_DATA_MODELS_AND_INTEGRATION_SEAMS.md`.
- `docs/requirements/APPENDIX_Z_SUPERSEDED_AND_OVERRIDDEN_REQUIREMENTS_REGISTER.md`.

## Evidence Model

Phase L added `Version12ProductionReadiness`, which records:

- visual QA surfaces for homepage, play/search, study, opening explorer, analysis, settings, admin, live overlay, and mobile views;
- browser/route smoke coverage for public routes, protected gates, and live overlay path;
- accessibility and TTS checks, including same-visible-text TTS behavior;
- performance budget evidence for overlays, TTS start, AI health, and board interaction;
- patch-map, integration-log, upstream-sync, and unresolved-decision coverage;
- regression evidence for `evenchess/test`, root compile, route smoke, and `git diff --check`.

The model distinguishes `phaseEvidenceRecorded` from `productionLaunchAllowed`. Phase evidence can be complete while production launch remains blocked.

## Commands Run

```bash
cd ~/dev/lila-docker
docker compose exec -T lila sbt "evenchess/testOnly lila.evenchess.EvenChessThemeTest lila.evenchess.Version12ProductionReadinessTest"
docker compose exec -T lila sbt compile
docker compose exec -T lila sbt evenchess/test
```

Results:

- EvenChess test command passed the full local EvenChess suite: 374 passed, 0 failed.
- Root compile passed.
- `lila` was restarted before final visual checks.
- `git diff --check` passed.

## Route Smoke

Route smoke was run against `http://localhost:8080/`.

| Route | Expected | Result |
| --- | ---: | ---: |
| `/` | 200 | 200 |
| `/evenchess/play` | 200 | 200 |
| `/study` | 200 | 200 |
| `/analysis` | 200 | 200 |
| `/opening` | 200 | 200 |
| `/account/preferences/evenchess` | 303 | 303 |
| `/evenchess/account` | 303 | 303 |
| `/dev/evenchess/ops` | 303 | 303 |

## Visual QA

Browser plugin QA was attempted but the in-app browser was unavailable in this session with `Browser is not available: iab`.

Chrome headless screenshots were captured to the local temp folder:

- `C:\Users\jayde\AppData\Local\Temp\evenchess-phase-l-qa\home-mobile-cdp-final.png`
- `C:\Users\jayde\AppData\Local\Temp\evenchess-phase-l-qa\play-mobile-cdp-final.png`

CDP mobile emulation verified:

- homepage `innerWidth=390`, document scroll width `390`, EvenChess main width `390`;
- play page `innerWidth=390`, document scroll width `390`, EvenChess main width `390`.

During Phase L, the first mobile screenshot pass exposed horizontal clipping in the EvenChess hero. The fix stayed in `EvenChessTheme` tokens by adding viewport-safe box sizing and overflow guards to the page, hero, panel, and full-width bands.

## Accessibility And TTS

The Phase L evidence model requires:

- persistent overlay disclosure;
- no color-only signals;
- keyboard/screen-reader labels;
- TTS off by default;
- TTS reading the same visible authorized text;
- live TTS tied to auditable coaching identity.

These are contract-tested in `Version12ProductionReadinessTest`, `TtsCoachTest`, `LiveOverlayUiTest`, and the frontend TTS/overlay tests from earlier Version 1.2 phases. Authenticated screen-reader QA still needs to be repeated before public launch.

## Performance Evidence

The Phase L evidence model records budget checks for overlay render, TTS start, AI health, and board interaction. Contract-level budgets pass. Production launch still needs live provider telemetry for OpenAI/TTS latency, payment callbacks, ad callbacks, and queue/overlay behavior under realistic traffic.

## Records

- Patch map status: current for upstream/core Lichess edits recorded through Version 1.2 Phase K.
- Phase L patch map: no new entry required because Phase L did not edit upstream/core Lichess files.
- Integration log status: updated with `INT-2026-024`.
- Upstream sync process: reviewed and still required for future Lichess updates.

## Unresolved Decisions Before Launch

The release model tracks these unresolved or provider-dependent decisions:

- `DEC-L1-002`: exact ECR calibration and rollout thresholds.
- `DEC-L1-003`: final monetisation/provider policy and pricing.
- `DEC-L1-008`: production AI/provider model and budget approval.
- `DEC-L1-009`: final paid-launch go/no-go approval.
- `DEC-V12-001`: exact normal-chess visibility policy in production.
- `DEC-V12-002`: AI overlay scope across study/openings/analysis and launch defaults.
- `DEC-V12-003`: TTS provider/browser fallback policy.
- `DEC-V12-004`: admin backend provider secret storage and rotation policy.
- `DEC-V12-005`: public launch QA scope for authenticated/admin/mobile browser matrix.

## Launch Decision

Engineering Phase L status: complete after final test pass.

Production launch status: no-go for now.

Reasons:

- product-owner launch approval is not captured;
- provider/API/payment/ad decisions are not finalized;
- authenticated/admin visual QA still needs production-like accounts;
- live OpenAI/TTS/payment/ad telemetry is not yet proven under production conditions.
