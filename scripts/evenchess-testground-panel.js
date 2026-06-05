"use strict";

const childProcess = require("node:child_process");
const fs = require("node:fs");
const http = require("node:http");
const os = require("node:os");
const path = require("node:path");

const args = process.argv.slice(2);

function argValue(name, fallback) {
  const index = args.indexOf(name);
  if (index >= 0 && args[index + 1]) return args[index + 1];
  return fallback;
}

const host = argValue("--host", "127.0.0.1");
const port = Number.parseInt(argValue("--port", "8791"), 10);
const panelVersion = "2026-06-03.1";
const scriptDir = __dirname;
const launcherPath = path.join(scriptDir, "evenchess-testground.ps1");
const repoRoot = path.resolve(scriptDir, "..");
const mainUrl = process.env.EVENCHESS_TESTGROUND_SITE || "http://localhost:8080/";
const botAdminUrl = new URL("/dev/evenchess/ops/bots", mainUrl).toString();
const wslDistro = process.env.EVENCHESS_WSL_DISTRO || "Ubuntu";
const eceRoot = process.env.EVENCHESS_ENGINE_ROOT || "/home/jayde/dev/lila-docker/repos/ece";
const eceRootHostPath = process.env.EVENCHESS_ENGINE_ROOT_HOST_PATH || linuxPathToWindowsUnc(eceRoot);
const eceUrl = (process.env.EVENCHESS_ENGINE_URL || "http://127.0.0.1:8787").replace(/\/$/, "");
const eceSettingsUrl = `${eceUrl}/ece/settings`;
const lilaEceUrl = (process.env.EVENCHESS_LILA_ECE_URL || "http://host.docker.internal:8787").replace(/\/$/, "");
const clmUrl = (process.env.ECE_CLM_URL || "http://127.0.0.1:8790").replace(/\/$/, "");
const clmAppUrl = `${clmUrl}/clm`;
const clmStatusUrl = `${clmUrl}/api/clm/status`;
const panelClmPath = "/clm";
const eceDebugLogPath = process.env.ECE_DEBUG_IO_LOG_PATH || path.posix.join(eceRoot, "logs", "ece-debug-io.json");
const eceDebugLogReadPath = process.env.ECE_DEBUG_IO_LOG_HOST_PATH || linuxPathToWindowsUnc(eceDebugLogPath);
const ecePidPath = process.env.ECE_PID_HOST_PATH || path.join(eceRootHostPath, ".ece-local.pid");
const clmPidPath = process.env.ECE_CLM_PID_HOST_PATH || path.join(eceRootHostPath, "ECE_CLM", ".ece-clm-local.pid");
const eclSmokeFen = "rnbqkbnr/pppp1ppp/5n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4";
const stateDir = path.join(process.env.LOCALAPPDATA || path.join(os.homedir(), "AppData", "Local"), "EvenChess", "TestGround");
const panelPidPath = path.join(stateDir, "panel.pid");
const uiBuildMetadataPath = path.join(stateDir, "ui-build.json");

const actions = new Map([
  ["start-docker", "Start WSL/Docker"],
  ["stop-docker", "Stop WSL/Docker"],
  ["launch-evenchess", "Launch EvenChess"],
  ["build-ui", "Build UI assets"],
  ["start-stack", "Start WSL/Docker + local stack"],
  ["status", "Status check"],
  ["open-site", "Open site"],
  ["stop-containers", "Stop containers"],
  ["shutdown", "Stop containers + shut down WSL"],
  ["start-real-ece", "Start real ECE"],
  ["start-test-ece", "Start test ECE"],
  ["health", "ECE health"],
  ["sample-board", "Sample ECE board"],
  ["stop-real-ece", "Stop real ECE"],
  ["stop-test-ece", "Stop test ECE"],
  ["launch-clm", "Launch ECE CLM"],
  ["stop-clm", "Stop ECE CLM"],
  ["clm-status", "ECE CLM status"],
  ["grant-admin-access", "Grant Admin bot/settings access"],
]);

function powershellExe() {
  const systemRoot = process.env.SystemRoot || "C:\\Windows";
  return path.join(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
}

function linuxPathToWindowsUnc(value) {
  if (!value || !value.startsWith("/")) return value;
  return "\\\\wsl$\\" + wslDistro + value.replace(/\//g, "\\");
}

function runCommand(command, commandArgs, timeoutMs = 120000) {
  return new Promise(resolve => {
    const startedAt = Date.now();
    childProcess.execFile(command, commandArgs, { windowsHide: true, timeout: timeoutMs }, (error, stdout, stderr) => {
      const timedOut = Boolean(error && error.killed);
      resolve({
        code: error ? (typeof error.code === "number" ? error.code : 1) : 0,
        timedOut,
        durationMs: Date.now() - startedAt,
        stdout: stdout || "",
        stderr: stderr || "",
        error: error && !timedOut ? error.message : "",
      });
    });
  });
}

function runCommandWithInput(command, commandArgs, input = "", timeoutMs = 120000) {
  return new Promise(resolve => {
    const startedAt = Date.now();
    const child = childProcess.spawn(command, commandArgs, {
      windowsHide: true,
      stdio: ["pipe", "pipe", "pipe"],
    });
    const stdout = [];
    const stderr = [];
    let settled = false;
    const timer = setTimeout(() => {
      if (settled) return;
      settled = true;
      child.kill();
      resolve({
        code: 1,
        timedOut: true,
        durationMs: Date.now() - startedAt,
        stdout: Buffer.concat(stdout).toString("utf8"),
        stderr: Buffer.concat(stderr).toString("utf8"),
        error: "Command timed out.",
      });
    }, timeoutMs);

    child.stdout.on("data", chunk => stdout.push(Buffer.from(chunk)));
    child.stderr.on("data", chunk => stderr.push(Buffer.from(chunk)));
    child.on("error", error => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      resolve({
        code: 1,
        timedOut: false,
        durationMs: Date.now() - startedAt,
        stdout: Buffer.concat(stdout).toString("utf8"),
        stderr: Buffer.concat(stderr).toString("utf8"),
        error: error.message,
      });
    });
    child.on("close", code => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      resolve({
        code: typeof code === "number" ? code : 1,
        timedOut: false,
        durationMs: Date.now() - startedAt,
        stdout: Buffer.concat(stdout).toString("utf8"),
        stderr: Buffer.concat(stderr).toString("utf8"),
        error: "",
      });
    });

    if (input) child.stdin.end(input);
    else child.stdin.end();
  });
}

function runLauncherAction(action) {
  return runCommand(
    powershellExe(),
    ["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", launcherPath, "-Action", action],
    action === "start-docker" || action === "start-stack" || action === "launch-evenchess" || action === "build-ui" || action === "shutdown" ? 360000 : 120000
  );
}

function json(res, statusCode, body) {
  const payload = JSON.stringify(body, null, 2);
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "Content-Length": Buffer.byteLength(payload),
  });
  res.end(payload);
}

function html(res, body) {
  res.writeHead(200, {
    "Content-Type": "text/html; charset=utf-8",
    "Cache-Control": "no-store",
  });
  res.end(body);
}

async function readRequestBody(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  return Buffer.concat(chunks).toString("utf8");
}

function safeReadPid(filePath) {
  try {
    return fs.readFileSync(filePath, "utf8").trim();
  } catch {
    return "";
  }
}

function pidRunning(pidText) {
  const pid = Number.parseInt(pidText, 10);
  if (!Number.isInteger(pid) || pid <= 0) return false;
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

async function wslPidRunning(pidText) {
  const pid = Number.parseInt(pidText, 10);
  if (!Number.isInteger(pid) || pid <= 0 || String(pid) !== String(pidText).trim()) return false;
  const result = await runCommand("wsl.exe", ["-d", wslDistro, "--", "bash", "-lc", `kill -0 ${pid}`], 5000);
  return result.code === 0;
}

function diagnosticsStatus(record) {
  return (
    record &&
    record.output &&
    record.output.diagnostics &&
    record.output.diagnostics.status
  ) || "";
}

function debugSummary(record) {
  if (!record || typeof record !== "object") return null;
  return {
    timestamp: record.timestamp || "",
    method: record.method || "",
    endpoint: record.endpoint || "",
    status_code: record.status_code || "",
    request_id: record.request_id || "",
    mode: record.mode || "",
    diagnostics_status: diagnosticsStatus(record),
  };
}

function readDebugLog() {
  try {
    const raw = fs.readFileSync(eceDebugLogReadPath, "utf8");
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return {
        path: eceDebugLogPath,
        readPath: eceDebugLogReadPath,
        exists: true,
        count: 0,
        latest: null,
        latestEntries: [],
        parseError: "Debug log JSON root is not an array.",
      };
    }

    const latestRecords = parsed.slice(-10).reverse();
    return {
      path: eceDebugLogPath,
      readPath: eceDebugLogReadPath,
      exists: true,
      count: parsed.length,
      latest: debugSummary(parsed[parsed.length - 1]),
      latestEntries: latestRecords.map(record => ({ summary: debugSummary(record), record })),
      parseError: "",
    };
  } catch (error) {
    return {
      path: eceDebugLogPath,
      readPath: eceDebugLogReadPath,
      exists: fs.existsSync(eceDebugLogReadPath),
      count: 0,
      latest: null,
      latestEntries: [],
      parseError: error.code === "ENOENT" ? "" : error.message,
    };
  }
}

function roundUiStatus() {
  const manifestPath = path.join(repoRoot, "public", "compiled", "manifest.json");
  let buildMetadata = {};
  try {
    buildMetadata = JSON.parse(fs.readFileSync(uiBuildMetadataPath, "utf8"));
  } catch {
    buildMetadata = {};
  }

  try {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
    const round = manifest && manifest.js && manifest.js.round;
    const jsHash = typeof round === "string" ? round : round && round.hash;
    const cssHash = manifest && manifest.css && manifest.css.round;
    const jsPath = jsHash ? path.join(repoRoot, "public", "compiled", `round.${jsHash}.js`) : "";
    const cssPath = cssHash ? path.join(repoRoot, "public", "css", `round.${cssHash}.css`) : "";
    const jsText = jsPath && fs.existsSync(jsPath) ? fs.readFileSync(jsPath, "utf8") : "";
    const cssText = cssPath && fs.existsSync(cssPath) ? fs.readFileSync(cssPath, "utf8") : "";
    const hasLevelShell = jsText.includes("EvenChess Levels");
    const hasBoardOverlayRenderer = jsText.includes("evenchess-board-overlay");
    const hasFeatureToggleCss = cssText.includes("evenchess-live__feature-toggle");
    const hasBoardOverlayCss = cssText.includes("evenchess-board-overlay");
    return {
      ok: Boolean(jsHash && cssHash && hasLevelShell && hasBoardOverlayRenderer && hasFeatureToggleCss && hasBoardOverlayCss),
      jsHash: jsHash || "",
      cssHash: cssHash || "",
      hasLevelShell,
      hasBoardOverlayRenderer,
      hasFeatureToggleCss,
      hasBoardOverlayCss,
      manifestPath,
      updatedAt: fs.statSync(manifestPath).mtime.toISOString(),
      buildMetadataPath: uiBuildMetadataPath,
      builtAt: buildMetadata.builtAt || "",
      gitVersion: buildMetadata.gitVersion || "",
      buildRoundJsHash: buildMetadata.roundJsHash || "",
      buildRoundCssHash: buildMetadata.roundCssHash || "",
    };
  } catch (error) {
    return {
      ok: false,
      jsHash: "",
      cssHash: "",
      hasLevelShell: false,
      hasBoardOverlayRenderer: false,
      hasFeatureToggleCss: false,
      hasBoardOverlayCss: false,
      manifestPath,
      updatedAt: "",
      buildMetadataPath: uiBuildMetadataPath,
      builtAt: buildMetadata.builtAt || "",
      gitVersion: buildMetadata.gitVersion || "",
      buildRoundJsHash: buildMetadata.roundJsHash || "",
      buildRoundCssHash: buildMetadata.roundCssHash || "",
      error: error.message,
    };
  }
}

async function fetchEclBoardOverlay() {
  const target = new URL("/evenchess/testground/ece/board-overlay", mainUrl);
  target.searchParams.set("gameId", "test-ground-game");
  target.searchParams.set("playerId", "test-ground-student");
  target.searchParams.set("ply", "10");
  target.searchParams.set("side", "white");
  target.searchParams.set("level", "10");
  target.searchParams.set("fen", eclSmokeFen);
  target.searchParams.set("eceBaseUrl", lilaEceUrl);
  const result = await fetchJson(target.toString(), 15000);
  return {
    ok: result.ok,
    status: result.status,
    target: target.toString(),
    body: result.body,
    error: result.error || "",
  };
}

function clearDebugLog() {
  fs.mkdirSync(path.dirname(eceDebugLogReadPath), { recursive: true });
  fs.writeFileSync(eceDebugLogReadPath, "[]\n", "utf8");
}

function normalizeText(text) {
  return String(text || "").replace(/\u0000/g, "");
}

async function fetchJson(url, timeoutMs = 5000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { signal: controller.signal });
    const text = await response.text();
    let body = null;
    try {
      body = text ? JSON.parse(text) : null;
    } catch {
      body = text;
    }
    return { ok: response.ok, status: response.status, body };
  } catch (error) {
    return { ok: false, status: 0, error: error.message };
  } finally {
    clearTimeout(timer);
  }
}

async function fetchClmThroughWsl(pathnameAndSearch, options = {}) {
  const method = String(options.method || "GET").toUpperCase();
  const body = options.body || "";
  const timeoutMs = options.timeoutMs || 120000;
  const target = new URL(pathnameAndSearch, clmUrl);
  const markerStatus = "__ECE_CLM_HTTP_STATUS__";
  const markerContentType = "__ECE_CLM_CONTENT_TYPE__";
  const args = [
    "-d",
    wslDistro,
    "--",
    "curl",
    "-sS",
    "-L",
    "-X",
    method,
    "-w",
    `${markerStatus}%{http_code}${markerContentType}%{content_type}`,
  ];

  if (body) {
    args.push("-H", "Content-Type: application/json", "--data-binary", "@-");
  }

  args.push(target.toString());
  const result = await runCommandWithInput("wsl.exe", args, body, timeoutMs);
  if (result.code !== 0) {
    return {
      ok: false,
      status: 0,
      contentType: "application/json; charset=utf-8",
      bodyText: "",
      error: result.stderr || result.error || "CLM WSL proxy command failed.",
    };
  }

  let statusMarkerIndex = result.stdout.lastIndexOf(`\n${markerStatus}`);
  let statusMarkerOffset = 1;
  if (statusMarkerIndex < 0) {
    statusMarkerIndex = result.stdout.lastIndexOf(markerStatus);
    statusMarkerOffset = 0;
  }
  let contentTypeMarkerIndex = result.stdout.lastIndexOf(`\n${markerContentType}`);
  let contentTypeMarkerOffset = 1;
  if (contentTypeMarkerIndex < 0) {
    contentTypeMarkerIndex = result.stdout.lastIndexOf(markerContentType);
    contentTypeMarkerOffset = 0;
  }
  if (statusMarkerIndex < 0 || contentTypeMarkerIndex < 0 || contentTypeMarkerIndex < statusMarkerIndex) {
    return {
      ok: false,
      status: 502,
      contentType: "application/json; charset=utf-8",
      bodyText: "",
      error: "CLM proxy response did not include curl status markers.",
    };
  }

  const bodyText = result.stdout.slice(0, statusMarkerIndex);
  const statusText = result.stdout.slice(statusMarkerIndex + markerStatus.length + statusMarkerOffset, contentTypeMarkerIndex).trim();
  const contentType = result.stdout.slice(contentTypeMarkerIndex + markerContentType.length + contentTypeMarkerOffset).trim();
  const status = Number.parseInt(statusText, 10) || 502;
  return {
    ok: status >= 200 && status < 300,
    status,
    contentType: contentType || "application/octet-stream",
    bodyText,
    error: "",
  };
}

async function fetchClmJson(pathnameAndSearch, timeoutMs = 8000) {
  const result = await fetchClmThroughWsl(pathnameAndSearch, { method: "GET", timeoutMs });
  if (!result.ok) return { ok: false, status: result.status, error: result.error || result.bodyText };

  try {
    return { ok: true, status: result.status, body: JSON.parse(result.bodyText || "null") };
  } catch (error) {
    return { ok: false, status: result.status, error: error.message, body: result.bodyText };
  }
}

async function proxyClmRequest(req, res, url) {
  const body = req.method === "GET" || req.method === "HEAD" ? "" : await readRequestBody(req);
  const timeoutMs = url.pathname.startsWith("/api/clm/jobs/") ? 30 * 60 * 1000 : 120000;
  const result = await fetchClmThroughWsl(`${url.pathname}${url.search}`, {
    method: req.method,
    body,
    timeoutMs,
  });

  if (result.status === 0 || result.error) {
    json(res, 502, {
      status: "error",
      error: "clm_proxy_failed",
      detail: result.error || "ECE CLM is not reachable inside WSL.",
      clm: clmUrl,
    });
    return;
  }

  res.writeHead(result.status, {
    "Content-Type": result.contentType || "application/octet-stream",
    "Cache-Control": "no-store",
    "Content-Length": Buffer.byteLength(result.bodyText),
  });
  res.end(result.bodyText);
}

async function statusPayload() {
  const [eceHealth, clmStatus, siteHealth, wslList, dockerWindows, dockerWsl] = await Promise.all([
    fetchJson(`${eceUrl}/health`, 4000),
    fetchClmJson("/api/clm/status", 8000),
    fetchJson(mainUrl, 4000),
    runCommand("wsl.exe", ["-l", "-v"], 5000),
    runCommand("docker", ["version", "--format", "{{json .}}"], 8000),
    runCommand("wsl.exe", ["-d", wslDistro, "--", "bash", "-lc", "command -v docker >/dev/null 2>&1 && docker version >/dev/null 2>&1"], 10000),
  ]);

  const testEcePid = safeReadPid(path.join(stateDir, "test-ece.pid"));
  const realEcePid = safeReadPid(ecePidPath);
  const clmPid = safeReadPid(clmPidPath);
  const realEceRunning = await wslPidRunning(realEcePid);
  const clmRunning = await wslPidRunning(clmPid);
  const debugLog = readDebugLog();
  const uiAssets = roundUiStatus();
  const wslOutput = normalizeText(wslList.stdout || wslList.stderr || wslList.error);
  const wslLine = wslOutput
    .split(/\r?\n/)
    .map(line => line.replace(/^\*\s*/, "").trim())
    .find(line => line.toLowerCase().startsWith(wslDistro.toLowerCase()));

  return {
    site: mainUrl,
    botAdmin: botAdminUrl,
    ece: eceUrl,
    lilaEce: lilaEceUrl,
    eceRoot,
    eceRootHostPath,
    eceDebugLogPath,
    eceDebugLogReadPath,
    panel: `http://${host}:${port}/`,
    panelVersion,
    clm: clmUrl,
    clmApp: clmAppUrl,
    clmStatus,
    wsl: {
      distro: wslDistro,
      running: Boolean(wslLine && /\bRunning\b/i.test(wslLine)),
      output: wslOutput,
    },
    eceHealth,
    evenchess: {
      reachable: siteHealth.ok,
      status: siteHealth.status,
      error: siteHealth.error || "",
    },
    docker: {
      windowsReady: dockerWindows.code === 0,
      windowsOutput: dockerWindows.stdout || dockerWindows.stderr || dockerWindows.error,
      wslReady: dockerWsl.code === 0,
      wslOutput: dockerWsl.stdout || dockerWsl.stderr || dockerWsl.error,
    },
    processes: {
      realEcePid,
      realEceRunning,
      testEcePid,
      testEceRunning: pidRunning(testEcePid),
      clmPid,
      clmRunning,
    },
    debugLog: {
      path: debugLog.path,
      exists: debugLog.exists,
      count: debugLog.count,
      latest: debugLog.latest,
      parseError: debugLog.parseError,
    },
    uiAssets,
  };
}

function page() {
  function button(id, label, kind = "") {
    return `<button data-action="${id}" class="${kind}">${label}</button>`;
  }

  function utilityButton(id, label, kind = "") {
    return `<button id="${id}" class="${kind}">${label}</button>`;
  }

  function section(title, rows) {
    return `<div class="control-section"><h2>${title}</h2><div class="control-rows">${rows.join("")}</div></div>`;
  }

  function row(left, right) {
    return `<div class="control-row">${left}${right}</div>`;
  }

  const controls = [
    section("1. WSL / Docker", [
      row(button("start-docker", "Start WSL / Docker", "start"), button("stop-docker", "Stop WSL / Docker", "stop")),
    ]),
    section("2. ECE", [
      row(button("start-real-ece", "Start Real ECE", "start"), button("stop-real-ece", "Stop Real ECE", "stop")),
      row(button("start-test-ece", "Start Test ECE", "start"), button("stop-test-ece", "Stop Test ECE", "stop")),
      row(`<a class="button" href="${eceSettingsUrl}" target="_blank" rel="noreferrer">Open ECE Settings</a>`, button("health", "ECE Health", "utility")),
    ]),
    section("3. ECE CLM", [
      row(button("launch-clm", "Launch ECE CLM", "start"), button("stop-clm", "Stop ECE CLM", "stop")),
      row(`<a class="button" href="${panelClmPath}" target="_blank" rel="noreferrer">Open CLM Page</a>`, button("clm-status", "CLM Status", "utility")),
    ]),
    section("4. EvenChess", [
      row(button("launch-evenchess", "Launch EvenChess", "start"), button("stop-containers", "Stop EvenChess", "stop")),
      row(`<a class="button" href="${mainUrl}" target="_blank" rel="noreferrer">Open Site</a>`, button("build-ui", "Build UI Assets", "utility")),
    ]),
    section("5. Bot Simulation Admin", [
      row(`<a class="button" href="${botAdminUrl}" target="_blank" rel="noreferrer">Open Bot Admin</a>`, button("grant-admin-access", "Grant Admin Access", "utility")),
    ]),
    section("Diagnostics", [
      row(button("status", "Stack Status", "utility"), button("sample-board", "Sample Board", "utility")),
      row(utilityButton("ecl-board-overlay", "ECL Overlay Smoke", "utility"), button("open-site", "Open Site Window", "utility")),
      row(button("shutdown", "Stop EvenChess + WSL", "stop"), utilityButton("refresh", "Refresh Status", "utility")),
      row(utilityButton("refresh-debug", "Refresh ECE Debug", "utility"), utilityButton("clear", "Clear Panel Log", "utility")),
      row(utilityButton("clear-debug", "Clear ECE Debug", "stop"), utilityButton("shutdown-panel", "Close Control Panel", "stop")),
    ]),
  ].join("");

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>EvenChess Test Ground</title>
  <style>
    :root {
      color-scheme: dark;
      --bg: #08111d;
      --panel: #101c2b;
      --panel-2: #142338;
      --line: #27405d;
      --text: #edf6ff;
      --muted: #9bb1c7;
      --ok: #38c172;
      --warn: #f0b849;
      --bad: #ef6262;
      --accent: #4aa3ff;
      --stop: #7f2d3a;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      min-height: 100vh;
      background: var(--bg);
      color: var(--text);
      font: 14px/1.45 system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }
    header {
      display: flex;
      justify-content: space-between;
      gap: 24px;
      padding: 22px 28px;
      border-bottom: 1px solid var(--line);
      background: #0b1726;
    }
    h1 { margin: 0; font-size: 22px; }
    .sub { color: var(--muted); margin-top: 4px; }
    main {
      display: grid;
      grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
      gap: 18px;
      padding: 18px;
    }
    section {
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 8px;
      overflow: hidden;
    }
    section h2 {
      margin: 0;
      padding: 12px 14px;
      font-size: 14px;
      border-bottom: 1px solid var(--line);
      background: var(--panel-2);
    }
    .control-section + .control-section {
      border-top: 1px solid var(--line);
    }
    .control-section h2 {
      margin: 0;
      padding: 12px 14px;
      font-size: 14px;
      border-bottom: 1px solid var(--line);
      background: var(--panel-2);
    }
    .control-rows {
      display: grid;
      gap: 8px;
      padding: 12px;
    }
    .control-row {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      gap: 8px;
    }
    button, a.button {
      border: 1px solid #396084;
      background: #16304c;
      color: var(--text);
      border-radius: 6px;
      min-height: 38px;
      padding: 8px 10px;
      font: inherit;
      cursor: pointer;
      text-align: left;
      text-decoration: none;
    }
    button:hover, a.button:hover { border-color: var(--accent); background: #1b3d61; }
    button.stop {
      border-color: #8b4250;
      background: #4a1d2a;
    }
    button.stop:hover { background: #632538; }
    button.is-ready, button.is-active {
      border-color: #42d487;
      background: #17603a;
      color: #f4fff8;
    }
    button.blocked {
      border-color: #514f5a;
      background: #222a35;
      color: #8f9aaa;
    }
    button:disabled { cursor: not-allowed; opacity: 0.62; }
    .status {
      display: grid;
      grid-template-columns: repeat(6, minmax(0, 1fr));
      gap: 10px;
      padding: 12px;
    }
    .tile {
      background: #0d1826;
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 10px;
      min-height: 78px;
    }
    .label { color: var(--muted); font-size: 12px; }
    .value { margin-top: 6px; font-weight: 650; overflow-wrap: anywhere; }
    .ok { color: var(--ok); }
    .warn { color: var(--warn); }
    .bad { color: var(--bad); }
    pre {
      margin: 0;
      padding: 12px;
      min-height: 260px;
      max-height: 58vh;
      overflow: auto;
      white-space: pre-wrap;
      background: #07101a;
      color: #dcecff;
      border-top: 1px solid var(--line);
      font: 12px/1.45 ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
    }
    .debug-list {
      display: grid;
      gap: 8px;
      padding: 12px;
      border-top: 1px solid var(--line);
    }
    .debug-entry {
      display: grid;
      grid-template-columns: 150px 150px minmax(0, 1fr) 90px;
      gap: 8px;
      align-items: start;
      padding: 8px;
      border: 1px solid var(--line);
      border-radius: 6px;
      background: #0d1826;
      font-size: 12px;
    }
    .debug-entry code {
      color: #dcecff;
      overflow-wrap: anywhere;
    }
    .debug-empty {
      padding: 12px;
      color: var(--muted);
      border-top: 1px solid var(--line);
    }
    @media (max-width: 920px) {
      main { grid-template-columns: 1fr; }
      .status { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .debug-entry { grid-template-columns: 1fr 1fr; }
    }
    @media (max-width: 520px) {
      header { display: block; }
      .status { grid-template-columns: 1fr; }
      .control-row { grid-template-columns: 1fr; }
      .debug-entry { grid-template-columns: 1fr; }
    }
  </style>
</head>
<body>
  <header>
    <div>
      <h1>EvenChess Test Ground</h1>
      <div class="sub">Local stack controls, ECE selector, and debugging output.</div>
    </div>
    <div class="sub">Site: <a href="${mainUrl}" target="_blank" rel="noreferrer">${mainUrl}</a><br>ECE: ${eceUrl}<br>CLM: <a href="${panelClmPath}" target="_blank" rel="noreferrer">${panelClmPath}</a> via ${clmAppUrl}<br>Lila -> ECE: ${lilaEceUrl}</div>
  </header>
  <main>
    <section>
      <h2>Controls</h2>
      ${controls}
    </section>
    <div>
      <section>
        <h2>Status</h2>
        <div class="status" id="status"></div>
      </section>
      <section style="margin-top:18px">
        <h2>ECE Debug IO</h2>
        <div class="debug-empty" id="debug-summary">No ECE debug records loaded.</div>
        <div class="debug-list" id="debug-list"></div>
      </section>
      <section style="margin-top:18px">
        <h2>Debug Output</h2>
        <pre id="log">Ready.</pre>
      </section>
    </div>
  </main>
  <script>
    const log = document.getElementById("log");
    const statusEl = document.getElementById("status");
    const debugSummaryEl = document.getElementById("debug-summary");
    const debugListEl = document.getElementById("debug-list");
    const buttons = [...document.querySelectorAll("button[data-action]")];

    function append(text) {
      const stamp = new Date().toLocaleTimeString();
      log.textContent += "\\n\\n[" + stamp + "] " + text;
      log.scrollTop = log.scrollHeight;
    }

    function cls(ok, warn) {
      if (ok) return "ok";
      if (warn) return "warn";
      return "bad";
    }

    function tile(label, value, klass) {
      return '<div class="tile"><div class="label">' + label + '</div><div class="value ' + (klass || "") + '">' + value + '</div></div>';
    }

    function escapeHtml(value) {
      return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
    }

    function actionButton(action) {
      return document.querySelector('button[data-action="' + action + '"]') || document.getElementById(action);
    }

    function setReady(action, ready) {
      const button = actionButton(action);
      if (button) button.classList.toggle("is-ready", Boolean(ready));
    }

    function setBlocked(action, blocked, reason) {
      const button = actionButton(action);
      if (!button) return;
      button.disabled = Boolean(blocked);
      button.classList.toggle("blocked", Boolean(blocked));
      button.title = blocked ? reason : "";
    }

    function healthBody(data) {
      const body = data.eceHealth && data.eceHealth.body;
      return body && typeof body === "object" ? body : {};
    }

    function healthValue(body, key) {
      if (body && Object.prototype.hasOwnProperty.call(body, key)) return body[key];
      if (body && body.data && typeof body.data === "object" && Object.prototype.hasOwnProperty.call(body.data, key)) return body.data[key];
      return "";
    }

    function boolish(value) {
      return value === true || String(value).toLowerCase() === "true";
    }

    function eceInfo(data) {
      const reachable = Boolean(data.eceHealth && data.eceHealth.ok);
      if (!reachable) {
        return { reachable: false, real: false, test: false, label: "not reachable" };
      }

      const body = healthBody(data);
      const mode = String(healthValue(body, "mode") || "");
      const service = String(healthValue(body, "service") || "");
      const processes = data.processes || {};
      const test = Boolean(
        processes.testEceRunning ||
        mode === "test-ground-mock" ||
        boolish(healthValue(body, "test_payload"))
      );
      const details = [];

      if (mode) details.push("mode " + mode);
      else if (service) details.push(service);

      if (test && processes.testEceRunning && processes.testEcePid) details.push("pid " + processes.testEcePid);
      if (!test && processes.realEceRunning && processes.realEcePid) details.push("pid " + processes.realEcePid);

      return {
        reachable,
        real: !test,
        test,
        label: (test ? "test ECE" : "real ECE") + (details.length ? " (" + details.join(", ") + ")" : ""),
      };
    }

    function clmInfo(data) {
      const reachable = Boolean(data.clmStatus && data.clmStatus.ok);
      const body = data.clmStatus && data.clmStatus.body && typeof data.clmStatus.body === "object" ? data.clmStatus.body : {};
      const processes = data.processes || {};
      if (!reachable) {
        return { reachable: false, label: "not reachable" };
      }

      const details = [];
      const counts = body.database && body.database.counts ? body.database.counts : {};
      if (counts.positions !== undefined) details.push(counts.positions + " FENs");
      if (counts.validated_labels !== undefined) details.push(counts.validated_labels + " labels");
      if (processes.clmRunning && processes.clmPid) details.push("pid " + processes.clmPid);
      return {
        reachable,
        label: "ready" + (details.length ? " (" + details.join(", ") + ")" : ""),
      };
    }

    function updateControls(data) {
      buttons.forEach(button => { button.disabled = false; });
      const dockerReady = Boolean(data.docker && data.docker.wslReady);
      const ece = eceInfo(data);
      const clm = clmInfo(data);
      const evenChessReady = Boolean(data.evenchess && data.evenchess.reachable);
      const uiAssetsReady = Boolean(data.uiAssets && data.uiAssets.ok);

      setReady("start-docker", dockerReady);
      setReady("start-real-ece", ece.real);
      setReady("start-test-ece", ece.test);
      setReady("launch-clm", clm.reachable);
      setReady("launch-evenchess", evenChessReady);

      setBlocked("start-real-ece", ece.test, "Stop Test ECE before starting Real ECE.");
      setBlocked("start-test-ece", ece.real, "Stop Real ECE before starting Test ECE.");
      setBlocked(
        "launch-evenchess",
        !dockerReady || !uiAssetsReady,
        !dockerReady
          ? "Docker must be ready in WSL before EvenChess can launch."
          : "Build UI Assets first; Launch EvenChess only starts an already-built stack."
      );
      setBlocked("build-ui", false, "");
      setBlocked("stop-containers", !dockerReady, "Docker must be ready in WSL before containers can be stopped.");
      setBlocked("shutdown", false, "");
      setBlocked("sample-board", !ece.reachable, "ECE must be reachable before calling the board endpoint.");
      setBlocked("clm-status", false, "");
      setBlocked("ecl-board-overlay", !ece.reachable || !evenChessReady, "ECE and EvenChess must both be reachable before running the ECL overlay smoke.");
    }

    async function refreshDebugLog() {
      const response = await fetch("/api/ece-debug", { cache: "no-store" });
      const data = await response.json();
      const latest = data.latest;
      const statusText = data.parseError
        ? "Read problem: " + data.parseError
        : data.exists
          ? data.count + " record(s) at " + data.path
          : "No ECE debug log yet at " + data.path;

      debugSummaryEl.textContent = latest
        ? statusText + " | latest " + latest.endpoint + " " + latest.request_id + " " + latest.diagnostics_status
        : statusText;

      if (!data.latestEntries || !data.latestEntries.length) {
        debugListEl.innerHTML = "";
        return data;
      }

      debugListEl.innerHTML = data.latestEntries.map(item => {
        const s = item.summary || {};
        return '<div class="debug-entry">' +
          '<code>' + escapeHtml(s.timestamp) + '</code>' +
          '<code>' + escapeHtml(s.endpoint) + '</code>' +
          '<code>' + escapeHtml(s.request_id || s.mode) + '</code>' +
          '<code>' + escapeHtml(s.diagnostics_status || s.status_code) + '</code>' +
        '</div>';
      }).join("");
      return data;
    }

    async function refreshStatus() {
      const response = await fetch("/api/status", { cache: "no-store" });
      const data = await response.json();
      const wslRunning = data.wsl && data.wsl.running;
      const dockerReady = data.docker && data.docker.wslReady;
      const ece = eceInfo(data);
      const clm = clmInfo(data);
      const evenChessReady = data.evenchess && data.evenchess.reachable;
      const debugCount = data.debugLog && data.debugLog.count;
      const uiAssets = data.uiAssets || {};
      const buildVersion = uiAssets.gitVersion
        ? uiAssets.gitVersion + " / round " + (uiAssets.buildRoundJsHash || uiAssets.jsHash || "unknown")
        : "manifest round " + (uiAssets.jsHash || "unknown");
      const buildTime = uiAssets.builtAt || (uiAssets.updatedAt ? "manifest " + uiAssets.updatedAt : "no build metadata");
      statusEl.innerHTML =
        tile("WSL / Docker", (wslRunning ? data.wsl.distro + " running" : data.wsl.distro + " stopped") + " / " + (dockerReady ? "Docker ready" : "Docker not ready"), cls(dockerReady, wslRunning)) +
        tile("ECE", ece.label, cls(ece.reachable)) +
        tile("ECE CLM", clm.label, cls(clm.reachable, true)) +
        tile("EvenChess", evenChessReady ? "site reachable" : "not running", cls(evenChessReady)) +
        tile("Bot Admin", evenChessReady ? "admin controls embedded" : "start EvenChess first", cls(evenChessReady, true)) +
        tile("UI Build", (uiAssets.ok ? buildTime : "stale/missing") + " | " + buildVersion, cls(uiAssets.ok, Boolean(uiAssets.jsHash))) +
        tile("ECE Debug IO", debugCount ? debugCount + " record(s)" : "empty", cls(Boolean(debugCount), true)) +
        tile("Latest ECE", data.debugLog && data.debugLog.latest ? data.debugLog.latest.endpoint + " " + data.debugLog.latest.diagnostics_status : "none", cls(Boolean(data.debugLog && data.debugLog.latest), true));
      updateControls(data);
      return data;
    }

    function delay(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    }

    function actionStatusSettled(action, data) {
      const ece = eceInfo(data);
      const clm = clmInfo(data);
      if (action === "start-real-ece") return ece.real;
      if (action === "start-test-ece") return ece.test;
      if (action === "stop-real-ece") return !ece.real;
      if (action === "stop-test-ece") return !ece.test;
      if (action === "launch-clm") return clm.reachable;
      if (action === "stop-clm") return !clm.reachable;
      return true;
    }

    async function refreshUntilActionSettled(action) {
      let latest = null;
      for (let attempt = 0; attempt < 12; attempt += 1) {
        latest = await refreshStatus();
        if (actionStatusSettled(action, latest)) return latest;
        await delay(750);
      }
      return latest || refreshStatus();
    }

    async function runAction(action) {
      if (action === "shutdown" && !confirm("Stop containers and shut down WSL now?")) return;
      buttons.forEach(button => button.disabled = true);
      append("Running " + action + "...");
      try {
        const response = await fetch("/api/action", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ action }),
        });
        const data = await response.json();
        append(action + " exit code: " + data.result.code + "\\n" + (data.result.stdout || "") + (data.result.stderr ? "\\nSTDERR:\\n" + data.result.stderr : "") + (data.result.error ? "\\nERROR:\\n" + data.result.error : ""));
      } catch (error) {
        append(action + " failed: " + error.message);
      } finally {
        try {
          await refreshUntilActionSettled(action);
          await refreshDebugLog();
        } catch (error) {
          buttons.forEach(button => { button.disabled = false; });
          append("status failed: " + error.message);
        }
      }
    }

    async function runEclOverlaySmoke() {
      append("Running ECL ECE bridge overlay smoke...");
      try {
        const response = await fetch("/api/ecl-board-overlay", { method: "POST" });
        const data = await response.json();
        append(JSON.stringify(data, null, 2));
        await refreshStatus();
        await refreshDebugLog();
      } catch (error) {
        append("ECL overlay smoke failed: " + error.message);
      }
    }

    buttons.forEach(button => button.addEventListener("click", () => runAction(button.dataset.action)));
    document.getElementById("ecl-board-overlay").addEventListener("click", runEclOverlaySmoke);
    document.getElementById("refresh").addEventListener("click", async () => {
      const status = await refreshStatus();
      await refreshDebugLog();
      append(JSON.stringify(status, null, 2));
    });
    document.getElementById("refresh-debug").addEventListener("click", async () => append(JSON.stringify(await refreshDebugLog(), null, 2)));
    document.getElementById("clear").addEventListener("click", () => { log.textContent = "Ready."; });
    document.getElementById("clear-debug").addEventListener("click", async () => {
      if (!confirm("Clear the local ECE debug IO log file?")) return;
      const response = await fetch("/api/ece-debug/clear", { method: "POST" });
      append(JSON.stringify(await response.json(), null, 2));
      await refreshStatus();
      await refreshDebugLog();
    });
    document.getElementById("shutdown-panel").addEventListener("click", async () => {
      if (!confirm("Close only this browser control panel server? ECE, Docker, and EvenChess will keep running.")) return;
      await fetch("/api/shutdown", { method: "POST" });
      append("Control panel close requested.");
    });
    refreshStatus().then(() => refreshDebugLog()).catch(error => append("status failed: " + error.message));
  </script>
</body>
</html>`;
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${host}:${port}`);
  if (req.method === "GET" && url.pathname === "/") {
    html(res, page());
    return;
  }

  if ((req.method === "GET" || req.method === "POST") && (url.pathname === "/clm" || url.pathname.startsWith("/clm/") || url.pathname.startsWith("/api/clm/"))) {
    await proxyClmRequest(req, res, url);
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/status") {
    json(res, 200, await statusPayload());
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/ping") {
    json(res, 200, { ok: true, panelVersion });
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/ece-debug") {
    json(res, 200, readDebugLog());
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/ecl-board-overlay") {
    const result = await fetchEclBoardOverlay();
    json(res, result.ok ? 200 : 502, result);
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/ece-debug/clear") {
    try {
      clearDebugLog();
      json(res, 200, { ok: true, path: eceDebugLogPath });
    } catch (error) {
      json(res, 500, { ok: false, path: eceDebugLogPath, error: error.message });
    }
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/action") {
    let body;
    try {
      body = JSON.parse(await readRequestBody(req));
    } catch {
      json(res, 400, { error: "Invalid JSON body." });
      return;
    }

    const action = String(body.action || "");
    if (!actions.has(action)) {
      json(res, 400, { error: `Unsupported action: ${action}` });
      return;
    }

    const result = await runLauncherAction(action);
    json(res, 200, { action, label: actions.get(action), result });
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/shutdown") {
    json(res, 200, { ok: true });
    setTimeout(() => process.exit(0), 100);
    return;
  }

  json(res, 404, { error: "Not found." });
});

fs.mkdirSync(stateDir, { recursive: true });
fs.writeFileSync(panelPidPath, String(process.pid), "ascii");

server.listen(port, host, () => {
  console.log(`EvenChess Test Ground panel listening at http://${host}:${port}/`);
});
