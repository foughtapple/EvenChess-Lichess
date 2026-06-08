# EvenChess Plan v1.6 Phase S - CI/CD, Build, and Release Automation

## Phase Goal

Make EvenChess-Lichess builds, tests, packaging, and deployment repeatable without relying on local manual fixes.

Phase S must separate development launch from build work. Launching EvenChess in local Test Ground or production must not unexpectedly compile the whole project.

## Requirements Used

- `docs/requirements/planv1.6.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_MAIN.md`
- `docs/requirements/EVENCHESS_LICHESS_V2_REQUIREMENTS_APPENDICES_COMBINED.md`
- Appendix T: Operations, Deployment, and Incident Response
- Appendix V: Testing and QA Acceptance
- `docs/requirements/planv1.6_phase_o_admin_operator_console.md`
- `docs/requirements/planv1.6_phase_q_security_abuse_fairness_controls.md`
- `docs/requirements/planv1.6_phase_r_telemetry_audit_privacy_retention.md`

## Required Toolchain

The release toolchain must install and verify:

- Java 21 JDK;
- sbt 1.12.11 or the repo wrapper/runtime equivalent;
- Node.js matching `.node-version`;
- pnpm matching `package.json`;
- Docker/Buildx for packaging images where used;
- zstd/tar for release artifacts;
- browser automation dependencies for UI/browser tests where required.

CI must print tool versions at the start of jobs and fail if the wrong Java, Node, pnpm, or sbt version is used.

## Build Commands

Required backend commands:

```bash
./lila.sh "compile"
./lila.sh "evenchess/compile"
./lila.sh "evenchess/test"
./lila.sh "test;stage"
./lila.sh "scalafmtCheckAll"
```

Required frontend commands:

```bash
pnpm install
pnpm lint
pnpm check-format
pnpm test
./ui/build --no-install -p
node ui/test
pnpm test:ui-overlay
pnpm test:ui-tsx -- ui/round/tests/evenchessOverlay.test.ts
```

Required local Test Ground commands:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/evenchess-testground.ps1 -Action build-ui
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/evenchess-testground.ps1 -Action launch-evenchess
```

`launch-evenchess` must check that UI assets are built. It must not build them implicitly.

## Test Commands

Phase S CI must include test jobs for:

- Scala backend tests;
- EvenChess module tests;
- frontend TypeScript/UI tests;
- overlay/render tests;
- Test ECE fixture contract tests;
- route/controller tests where available;
- admin permission/CSRF tests;
- normal Lichess regression tests for touched seams.

Known local command coverage:

```bash
./lila.sh "evenchess/test"
node scripts/evenchess-test-ece-server.test.mjs
pnpm test:ui-overlay
pnpm test:ui-tsx -- ui/round/tests/evenchessOverlay.test.ts
```

CI must include the module-alias resolver used by local UI tests so `@/index` and other source aliases resolve consistently.

## Build Version Metadata

Every build should expose:

- commit hash;
- dirty/clean status where local;
- build timestamp;
- asset manifest timestamp;
- round JS hash;
- round CSS hash;
- backend `conf/version.conf` values where packaged;
- artifact/image tag.

Current local Test Ground writes UI build metadata after the explicit build action:

- built at;
- git version;
- manifest update time;
- round JS hash;
- round CSS hash;
- manifest path.

Production/admin visibility must show equivalent version information for server and assets, not only the local Test Ground panel.

## Artifact and Deployment Requirements

Release automation must produce:

- server artifact;
- asset artifact;
- optional server container image;
- optional asset container image;
- version/commit metadata in artifacts;
- deployment notes for ECL and ECE compatibility;
- rollback target information.

Server and asset artifacts must be compatible with the deployment target. If GitHub Actions publishes images, repository names, registry permissions, and deploy webhooks must be updated for the EvenChess fork rather than upstream-only `lichess-org/lila` conditions.

## Local Test Ground Requirements

The local Test Ground must remain a development tool only.

Required local behavior:

- Start WSL/Docker separately;
- Build UI assets separately;
- Launch EvenChess only after Docker and UI readiness checks pass;
- display build timestamp/version/hash;
- launch real ECE or test ECE separately;
- stop stack cleanly;
- keep unauthenticated bot/admin controls out of Test Ground, except links to authenticated admin pages;
- avoid hiding production build problems behind local repair commands.

The current Test Ground has a separate UI build action and readiness checks for `public/compiled/manifest.json`, round JS/CSS hashes, level shell, overlay renderer, feature-toggle CSS, and board overlay CSS.

## Current Implementation State

### Existing Foundations

The current repo has useful Phase S foundations:

- `.github/workflows/server.yml` builds/tests/stages the server on Java 21, writes `conf/version.conf`, creates `lila-3.0.tar.zst`, and builds a server Docker image on default-branch push/workflow dispatch.
- `.github/workflows/assets.yml` installs pnpm/Node, builds production UI assets, runs UI tests, creates `assets.tar.zst`, and builds an assets Docker image on default-branch push/workflow dispatch.
- `.github/workflows/lint.yml` runs frontend lint and format checks plus CodeQL JavaScript analysis.
- `package.json` defines pnpm scripts for lint, format, UI tests, overlay tests, and TSX source-alias test execution.
- `build.sbt` defines the EvenChess Scala module and root build structure.
- `project/build.properties` pins sbt version.
- `scripts/evenchess-testground.ps1` separates `build-ui` from `launch-evenchess`, checks asset readiness before launch, and writes local UI build metadata.
- `scripts/evenchess-testground-panel.js` displays UI asset status, build time, git version, and asset hashes.

### Not Yet Release-Proven

Phase S is not release-complete until:

- a fresh CI environment builds the current EvenChess fork without WSL-specific manual fixes;
- CI runs EvenChess-specific backend tests, UI tests, Test ECE fixture tests, admin tests, and browser/regression tests;
- CI catches the UI module-alias test-runner issue that previously affected `ui/round/tests/evenchessOverlay.test.ts`;
- packaging/deploy workflows target the EvenChess repository and deployment environment, not only upstream `lichess-org/lila`;
- build artifact/image tags are visible to operators;
- server and asset versions are shown in admin/operator pages;
- ECE deployment compatibility is checked without bundling private ECE internals into ECL;
- release scripts include rollback steps;
- local Test Ground build/launch docs are current;
- known failing tests are fixed or explicitly quarantined with issue references.

## CI Job Matrix

Minimum CI matrix:

| Job | Required checks |
| --- | --- |
| `server-build` | Java 21, sbt setup, `./lila.sh "compile"`, `./lila.sh "test;stage"` |
| `evenchess-backend` | `./lila.sh "evenchess/compile"`, `./lila.sh "evenchess/test"` |
| `frontend-assets` | Node from `.node-version`, pnpm from `package.json`, `pnpm install`, `./ui/build --no-install -p`, `node ui/test` |
| `frontend-quality` | `pnpm lint`, `pnpm check-format`, overlay/source-alias tests |
| `test-ece-fixture` | `node scripts/evenchess-test-ece-server.test.mjs` |
| `admin-security` | admin permission/CSRF/rate-limit tests when implemented |
| `browser-smoke` | public search, computer game, online game, overlay-after-move, proposed/potential controls where environment supports it |
| `package` | server artifact, asset artifact, image metadata |
| `release-checklist` | patch map current, integration log current, requirements outputs linked, no forbidden ECE internals |

## Release Checklist Automation

The release checklist should fail if:

- patch map is stale for touched upstream files;
- integration log is stale for touched Lichess seams;
- requirements phase docs are missing;
- ECE internals/secrets/provider paths are found in ECL public code or artifacts;
- Test Ground-only APIs are exposed in production routes;
- public search JSON exposes internal token/bot/match-contract diagnostics;
- build metadata is missing;
- migrations/storage requirements are missing for durable audit/history/calibration data;
- normal Lichess regression tests are skipped without explicit release signoff.

## Rollback Requirements

Each deployable release must record:

- previous server artifact/image tag;
- previous asset artifact/image tag;
- previous active ECL config version;
- compatible ECE version;
- database/audit retention migration rollback constraints;
- feature flags that can disable live ECE calls, overlays, proposed/potential moves, custom analysis, tokens, ads, bot fallback, and simulation bots.

Rollback must not silently alter active rated game fairness. If rollback changes live assistance behavior, it must be audited and may require no-rate/review handling.

## Patch Map Impact

Future Phase S implementation may touch GitHub workflows, Dockerfiles, deployment scripts, Test Ground scripts, admin version display, or build metadata routes. If any upstream Lichess runtime seam changes, update the patch map and integration log.

This Phase S documentation pass does not itself change runtime code.

## Phase S Acceptance Status

Phase S is conducted as a readiness and requirements pass.

Status:

- Upstream-style CI workflows and local Test Ground build separation exist.
- Local UI build metadata exists.
- Release readiness remains blocked until EvenChess-specific CI coverage, fresh-environment proof, repository/deployment-target updates, operator build-version visibility, release checklist automation, and rollback documentation are complete.

## Phase T Entry Criteria

Before staging deployment starts, Phase S must provide:

- passing server and asset builds in CI;
- passing EvenChess backend/UI/Test ECE fixture tests in CI;
- artifact/image outputs with version metadata;
- deployment scripts or documented deploy commands;
- rollback commands;
- clear separation between local Test Ground workflows and production deployment workflows.
