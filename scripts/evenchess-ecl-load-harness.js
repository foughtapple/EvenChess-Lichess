"use strict";

const http = require("node:http");
const https = require("node:https");

const START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

function parseArgs(argv = process.argv.slice(2), env = process.env) {
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) continue;
    const key = camelArg(token.slice(2));
    const next = argv[i + 1];
    if (!next || next.startsWith("--")) args[key] = true;
    else {
      args[key] = next;
      i += 1;
    }
  }

  return {
    eclBaseUrl: String(args.eclBaseUrl || env.ECL_LOAD_BASE_URL || "http://localhost:8080").replace(/\/+$/, ""),
    eceBaseUrl: String(args.eceBaseUrl || env.ECE_LOAD_TEST_TARGET || "http://127.0.0.1:8787").replace(/\/+$/, ""),
    requests: positiveInt(args.requests || env.ECL_LOAD_REQUESTS, 100),
    concurrency: positiveInt(args.concurrency || env.ECL_LOAD_CONCURRENCY, 10),
    authCookie: String(args.authCookie || env.ECL_LOAD_AUTH_COOKIE || ""),
    eceInternalKey: String(args.eceInternalKey || env.ECE_INTERNAL_API_KEY || ""),
    fen: String(args.fen || env.ECL_LOAD_FEN || START_FEN),
    timeoutMs: positiveInt(args.timeoutMs || env.ECL_LOAD_TIMEOUT_MS, 30000)
  };
}

async function runHarness(options = {}) {
  const config = { ...parseArgs([], {}), ...options };
  const results = [];

  await runStaticPhases(config, results);
  await runConcurrentBridgePhases(config, results);
  await captureEceMetrics(config, results);

  const summary = summarize(results);
  return {
    ok: summary.failed === 0,
    summary,
    results
  };
}

async function runStaticPhases(config, results) {
  await measure("ecl_home", () => request("GET", `${config.eclBaseUrl}/`, config), results);
  await measure("ecl_evenchess_play", () => request("GET", `${config.eclBaseUrl}/evenchess/play`, config), results);
  await measure("ecl_search_json_auth_surface", () => request("GET", `${config.eclBaseUrl}/evenchess/play/search.json`, config), results);

  if (config.authCookie) {
    await measure("ecl_bot_ops_panel", () => request("GET", `${config.eclBaseUrl}/dev/evenchess/ops/bots`, config), results);
  } else {
    results.push(skipped("ecl_bot_ops_panel", "ECL_LOAD_AUTH_COOKIE not configured"));
  }
}

async function runConcurrentBridgePhases(config, results) {
  let next = 0;
  const workers = Array.from({ length: config.concurrency }, async () => {
    for (;;) {
      const index = next;
      next += 1;
      if (index >= config.requests) return;

      const gameId = `ecl-load-${Date.now()}-${index}`;
      const boardPath = `/evenchess/testground/ece/board-overlay?${query({
        fen: config.fen,
        gameId,
        playerId: `student-${index}`,
        side: "white",
        level: "10",
        ply: String(index)
      })}`;
      await measure("ecl_ece_board_overlay", () => request("GET", `${config.eclBaseUrl}${boardPath}`, config), results);

      const cachePath = `${boardPath}&historyOnly=true`;
      await measure("ecl_replay_payload_cache", () => request("GET", `${config.eclBaseUrl}${cachePath}`, config), results);

      if (index % 5 === 0) {
        const proposedPath = `/evenchess/testground/ece/proposed-move?${query({
          fen: START_FEN,
          gameId: `${gameId}-proposal`,
          playerId: `student-${index}`,
          side: "white",
          level: "10",
          ply: String(index),
          moveUci: "e2e4"
        })}`;
        await measure("ecl_proposed_move_bridge", () => request("GET", `${config.eclBaseUrl}${proposedPath}`, config), results);
      }

      if (index % 5 === 1) {
        const potentialPath = `/evenchess/testground/ece/potential-move?${query({
          fen: START_FEN,
          gameId: `${gameId}-potential`,
          playerId: `student-${index}`,
          side: "white",
          level: "10",
          ply: String(index),
          kind: "player"
        })}`;
        await measure("ecl_potential_move_bridge", () => request("GET", `${config.eclBaseUrl}${potentialPath}`, config), results);
      }
    }
  });

  await Promise.all(workers);
}

async function captureEceMetrics(config, results) {
  await measure("ece_health", () => request("GET", `${config.eceBaseUrl}/health`, config, { ece: true }), results);
  await measure("ece_metrics", () => request("GET", `${config.eceBaseUrl}/ops/metrics`, config, { ece: true }), results);
}

async function measure(phase, fn, results) {
  const started = Date.now();
  try {
    const response = await fn();
    results.push({
      phase,
      status: response.status,
      ok: response.status >= 200 && response.status < 500,
      elapsed_ms: Date.now() - started,
      bytes: response.body.length
    });
  } catch (error) {
    results.push({
      phase,
      status: 0,
      ok: false,
      elapsed_ms: Date.now() - started,
      error: error && error.message ? error.message : "request_failed"
    });
  }
}

function request(method, urlText, config, options = {}) {
  const url = new URL(urlText);
  const transport = url.protocol === "https:" ? https : http;
  const headers = {};
  if (!options.ece && config.authCookie) headers.cookie = config.authCookie;
  if (options.ece && config.eceInternalKey) headers["x-ece-internal-key"] = config.eceInternalKey;

  return new Promise((resolve, reject) => {
    const req = transport.request({
      method,
      hostname: url.hostname,
      port: url.port || (url.protocol === "https:" ? 443 : 80),
      path: `${url.pathname}${url.search}`,
      headers,
      timeout: config.timeoutMs
    }, res => {
      const chunks = [];
      res.on("data", chunk => chunks.push(Buffer.from(chunk)));
      res.on("end", () => resolve({
        status: res.statusCode || 0,
        headers: res.headers,
        body: Buffer.concat(chunks).toString("utf8")
      }));
    });
    req.on("timeout", () => {
      req.destroy(new Error("request_timeout"));
    });
    req.on("error", reject);
    req.end();
  });
}

function summarize(results) {
  const phases = {};
  for (const result of results) {
    if (!phases[result.phase]) phases[result.phase] = [];
    phases[result.phase].push(result);
  }

  return {
    total: results.length,
    failed: results.filter(row => !row.ok).length,
    skipped: results.filter(row => row.skipped).length,
    phases: Object.fromEntries(Object.entries(phases).map(([phase, rows]) => [phase, summarizeRows(rows)]))
  };
}

function summarizeRows(rows) {
  const latencies = rows.map(row => row.elapsed_ms).filter(Number.isFinite).sort((a, b) => a - b);
  const status = {};
  for (const row of rows) status[String(row.status || "skipped")] = (status[String(row.status || "skipped")] || 0) + 1;
  return {
    calls: rows.length,
    ok: rows.filter(row => row.ok).length,
    failed: rows.filter(row => !row.ok).length,
    skipped: rows.filter(row => row.skipped).length,
    status,
    latency_ms: {
      p50: percentile(latencies, 50),
      p95: percentile(latencies, 95),
      p99: percentile(latencies, 99)
    }
  };
}

function skipped(phase, reason) {
  return { phase, skipped: true, ok: true, status: "skipped", elapsed_ms: 0, reason };
}

function query(params) {
  return new URLSearchParams(params).toString();
}

function percentile(sorted, p) {
  if (!sorted.length) return 0;
  const index = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.min(sorted.length - 1, Math.max(0, index))];
}

function positiveInt(value, fallback) {
  const n = Number(value || fallback);
  return Number.isInteger(n) && n > 0 ? n : fallback;
}

function camelArg(value) {
  return String(value || "")
    .split("-")
    .filter(Boolean)
    .map((part, index) => index === 0 ? part : `${part.slice(0, 1).toUpperCase()}${part.slice(1)}`)
    .join("");
}

async function main() {
  const result = await runHarness(parseArgs());
  console.log(JSON.stringify(result.summary, null, 2));
  if (!result.ok) process.exit(1);
}

if (require.main === module) {
  main().catch(error => {
    console.error(error && error.stack ? error.stack : error);
    process.exit(1);
  });
}

module.exports = {
  START_FEN,
  parseArgs,
  runHarness,
  summarize,
  summarizeRows,
  percentile,
  camelArg
};
