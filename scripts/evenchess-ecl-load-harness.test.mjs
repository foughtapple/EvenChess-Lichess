import assert from "node:assert/strict";
import harness from "./evenchess-ecl-load-harness.js";

const parsed = harness.parseArgs([
  "--ecl-base-url",
  "http://localhost:8080/",
  "--requests",
  "25",
  "--concurrency",
  "5",
]);

assert.equal(harness.camelArg("ecl-base-url"), "eclBaseUrl");
assert.equal(parsed.eclBaseUrl, "http://localhost:8080");
assert.equal(parsed.requests, 25);
assert.equal(parsed.concurrency, 5);

const summary = harness.summarize([
  { phase: "ecl_ece_board_overlay", status: 200, ok: true, elapsed_ms: 10 },
  { phase: "ecl_ece_board_overlay", status: 200, ok: true, elapsed_ms: 20 },
  { phase: "ecl_ece_board_overlay", status: 503, ok: false, elapsed_ms: 50 },
  { phase: "ecl_bot_ops_panel", status: "skipped", ok: true, skipped: true, elapsed_ms: 0 },
]);

assert.equal(summary.total, 4);
assert.equal(summary.failed, 1);
assert.equal(summary.skipped, 1);
assert.equal(summary.phases.ecl_ece_board_overlay.latency_ms.p50, 20);
assert.equal(summary.phases.ecl_ece_board_overlay.latency_ms.p95, 50);

console.log("EvenChess ECL load harness tests passed.");
