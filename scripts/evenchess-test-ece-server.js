'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);

function argValue(name, fallback) {
  const index = args.indexOf(name);
  if (index >= 0 && args[index + 1]) return args[index + 1];
  return fallback;
}

const HOST = argValue('--host', process.env.EVENCHESS_TEST_ECE_HOST || '127.0.0.1');
const PORT = Number(argValue('--port', process.env.EVENCHESS_TEST_ECE_PORT || '8787'));
const ENGINE_VERSION = 'test-ece-v2-ground-1';
const DEBUG_IO_ENABLED = process.env.ECE_DEBUG_IO_LOG === '1';
const DEBUG_IO_PATH = process.env.ECE_DEBUG_IO_LOG_PATH || path.join(process.cwd(), 'logs', 'ece-debug-io.json');
const DEBUG_IO_MAX_ENTRIES = Math.min(Math.max(Number(process.env.ECE_DEBUG_IO_LOG_MAX_ENTRIES || '100') || 100, 1), 1000);
const DEFAULT_FEN = 'rnbqkbnr/pppp1ppp/5n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4';
const ALLOWED_DEEP_MODULES = new Set(['stockfish', 'syzygy', 'opening_book', 'lichess_eval_cache', 'maia', 'ai_text']);

function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = '';
    req.setEncoding('utf8');
    req.on('data', chunk => {
      data += chunk;
      if (data.length > 1024 * 1024) {
        reject(new Error('Request body too large'));
        req.destroy();
      }
    });
    req.on('end', () => resolve(data));
    req.on('error', reject);
  });
}

function writeJson(res, status, payload) {
  const body = JSON.stringify(payload, null, 2);
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store',
    'access-control-allow-origin': 'http://localhost:8080',
    'access-control-allow-methods': 'GET,POST,OPTIONS',
    'access-control-allow-headers': 'content-type',
  });
  res.end(body);
}

function parseJson(raw) {
  if (!raw || !raw.trim()) return {};
  return JSON.parse(raw);
}

function appendDebugIo(entry) {
  if (!DEBUG_IO_ENABLED) return;

  try {
    fs.mkdirSync(path.dirname(DEBUG_IO_PATH), { recursive: true });
    let records = [];
    try {
      const raw = fs.readFileSync(DEBUG_IO_PATH, 'utf8');
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) records = parsed;
    } catch {
      records = [];
    }

    records.push(entry);
    records = records.slice(-DEBUG_IO_MAX_ENTRIES);
    const tempPath = `${DEBUG_IO_PATH}.tmp`;
    fs.writeFileSync(tempPath, `${JSON.stringify(records, null, 2)}\n`, 'utf8');
    fs.renameSync(tempPath, DEBUG_IO_PATH);
  } catch (err) {
    console.error(`Failed to write ECE debug IO log: ${err && err.message ? err.message : err}`);
  }
}

function requestRoot(input) {
  if (input && typeof input === 'object' && input.request && typeof input.request === 'object') return input.request;
  if (input && typeof input === 'object') return input;
  return {};
}

function level(value, fallback) {
  return normalizeLevel(value, fallback).value;
}

function normalizeLevel(value, fallback) {
  if (value === undefined || value === null || value === '') return { value: fallback, defaulted: true, warning: '' };
  const n = Number(value);
  if (Number.isInteger(n) && n >= 0 && n <= 10) return { value: n, defaulted: false, warning: '' };
  return { value: fallback, defaulted: true, warning: 'invalid_level_defaulted' };
}

function normalizeUseAi(value) {
  return value === 1 || value === '1' || value === true || String(value).toLowerCase() === 'true' ? 1 : 0;
}

function normalizeRating(value) {
  const n = Number(value || 0);
  return Number.isFinite(n) && n >= 0 ? n : 0;
}

function normalizeDeepModuleName(value) {
  const name = typeof value === 'string' ? value.trim().toLowerCase() : '';
  if (!name) return '';
  if (name === 'ai') return 'ai_text';
  if (name === 'eval_cache') return 'lichess_eval_cache';
  if (name === 'tablebase') return 'syzygy';
  return name;
}

function requestedDeepModules(root) {
  const raw = Array.isArray(root.requested_deep_modules)
    ? root.requested_deep_modules
    : typeof root.requested_deep_modules === 'string'
      ? root.requested_deep_modules.split(',')
      : [];

  return [...new Set(
    raw
      .map(normalizeDeepModuleName)
      .filter(name => ALLOWED_DEEP_MODULES.has(name))
  )];
}

function boolish(value) {
  return value === true || value === 1 || value === '1' || String(value).toLowerCase() === 'true';
}

function normalizeEceRequest(input) {
  const root = requestRoot(input);
  const whiteLevel = normalizeLevel(root.white_level, 10);
  const blackLevel = normalizeLevel(root.black_level, 10);
  const modules = requestedDeepModules(root);

  return {
    mode: root.mode || 'board_state',
    request_id: root.request_id || `test-ece-${Date.now()}`,
    input_fen: root.input_fen || root.fen || DEFAULT_FEN,
    proposed_move_uci: typeof root.proposed_move_uci === 'string' ? root.proposed_move_uci.trim() : '',
    quick_request_id: root.quick_request_id || 0,
    quick_context_id: root.quick_context_id || root.context_id || 0,
    rating_type: root.rating_type || 'unknown',
    white_rating_input: normalizeRating(root.white_rating_input),
    black_rating_input: normalizeRating(root.black_rating_input),
    white_level: whiteLevel.value,
    black_level: blackLevel.value,
    white_level_defaulted: whiteLevel.defaulted,
    black_level_defaulted: blackLevel.defaulted,
    use_ai: normalizeUseAi(root.use_ai),
    custom: {
      opening: root.custom && root.custom.opening !== undefined ? root.custom.opening : 0,
      instructions: root.custom && root.custom.instructions !== undefined ? root.custom.instructions : 0,
    },
    deep_requested: boolish(root.deep_requested) || modules.length > 0,
    requested_deep_modules: modules,
    warnings: [whiteLevel.warning, blackLevel.warning].filter(Boolean),
  };
}

function requestEcho(request) {
  return {
    request_id: request.request_id,
    mode: request.mode,
    input_fen: request.input_fen,
    proposed_move_uci: request.proposed_move_uci || 0,
    rating_type: request.rating_type,
    white_rating_input: request.white_rating_input,
    black_rating_input: request.black_rating_input,
    white_level: request.white_level,
    black_level: request.black_level,
    white_level_defaulted: request.white_level_defaulted,
    black_level_defaulted: request.black_level_defaulted,
    use_ai_requested: request.use_ai,
    ai_used: false,
    custom_opening_used: request.custom.opening !== 0,
    custom_instructions_used: request.custom.instructions !== 0,
    deep_requested: Boolean(request.deep_requested),
    requested_deep_modules: request.requested_deep_modules,
    quick_request_id: request.quick_request_id || 0,
    quick_context_id: request.quick_context_id || 0,
  };
}

function diagnostics(status, started, modulesUsed, warnings = [], errors = []) {
  return {
    status,
    elapsed_ms: Date.now() - started,
    engine_version: ENGINE_VERSION,
    mode: 'test-ground-mock',
    modules_used: modulesUsed,
    warnings: [...new Set(['fixture_payload_not_for_chess_truth', ...warnings])],
    errors,
  };
}

function unavailable(request) {
  return {
    stockfish: request.requested_deep_modules.includes('stockfish') ? 0 : 'deep_module_not_requested',
    ai: request.use_ai === 1 ? 'fixture_only' : 'use_ai_0',
    opening: request.custom.opening === 0 ? 'custom_opening_0' : 'fixture_only',
    tablebase: request.requested_deep_modules.includes('syzygy') ? 0 : 'deep_module_not_requested',
    syzygy: request.requested_deep_modules.includes('syzygy') ? 0 : 'deep_module_not_requested',
    lichess_eval_cache: request.requested_deep_modules.includes('lichess_eval_cache') ? 0 : 'deep_module_not_requested',
    maia: request.requested_deep_modules.includes('maia') ? 0 : 'deep_module_not_requested',
  };
}

function sideToMoveFromFen(fen) {
  const parts = String(fen || '').trim().split(/\s+/);
  return parts[1] === 'b' ? 'black' : 'white';
}

function sideOutput(side, requestedLevel, deliveredLevel, isSideToMove, options = {}) {
  const opponent = side === 'white' ? 'black' : 'white';
  const textSide = side === 'white' ? 'White' : 'Black';
  const delivered = Math.min(requestedLevel, deliveredLevel);
  const summaryText = `${textSide} has a test payload with material, king safety, hanging-piece, threat, pin, opening, candidate, eval, and human-risk examples.`;
  const warningText = side === 'white'
    ? 'loose-piece safety deserves an immediate safety check.'
    : 'pinned-piece safety deserves an immediate safety check.';
  const includeDeep = options.includeDeep !== false;
  const openingRequested = Boolean(options.openingRequested);
  const output = {
    side,
    student_side: side,
    opponent_side: opponent,
    level: {
      requested_level: requestedLevel,
      delivered_level: delivered,
      defaulted: false,
    },
    is_side_to_move: isSideToMove,
    summary: 0,
    immediate_warning: 0,
    plan: 0,
    candidate_moves: 0,
    evaluation: 0,
    human_risk: 0,
    opening: 0,
    overlays: {
      trade_status: {
        hanging_attackable: [],
        hanging_not_attackable: [],
        offset_count: [],
        advantage_offset_value: 0,
        disadvantage_offset_value: 0,
      },
      threats: {
        student_threats: [],
        opponent_threats: [],
      },
      pinned_pieces: {
        student_pinned: [],
        opponent_pinned: [],
      },
    },
    raw_deterministic: {
      summary_inputs: [],
      modules_used: [],
    },
    audit: {
      audit_id: `test-ece-${side}-${Date.now()}`,
      render_allowed: true,
      policy_version: 'test-ground-policy-v1',
      output_ref: `test-output-${side}-${Date.now()}`,
    },
  };

  if (delivered === 0) return output;

  if (delivered >= 2) {
    output.overlays.trade_status.hanging_attackable = [
      {
        piece_square: side === 'white' ? 'h5' : 'f1',
        piece: side === 'white' ? 'white_knight' : 'black_knight',
        reason: 'undefended_currently_attackable',
        source: 'deterministic_ece',
      },
      {
        piece_square: side === 'white' ? 'g7' : 'b2',
        piece: side === 'white' ? 'black_pawn' : 'white_pawn',
        reason: 'undefended_currently_attackable',
        source: 'deterministic_ece',
      },
    ];
    output.overlays.trade_status.hanging_not_attackable = [
      {
        piece_square: side === 'white' ? 'c4' : 'a8',
        piece: side === 'white' ? 'white_bishop' : 'black_rook',
        reason: 'undefended_not_currently_attackable',
        source: 'deterministic_ece',
      },
      {
        piece_square: side === 'white' ? 'e4' : 'h8',
        piece: side === 'white' ? 'white_pawn' : 'black_rook',
        reason: 'undefended_not_currently_attackable',
        source: 'deterministic_ece',
      },
    ];
  }

  if (delivered >= 3) {
    output.overlays.trade_status.offset_count = [
      {
        feature_id: 'offset_count',
        target_square: 'd3',
        target_piece: 'white_pawn',
        owner_side: 'white',
        attacking_side: 'black',
        perspective_side: side,
        first_capture_move: 'e4d3',
        piece_count_delta: side === 'black' ? 1 : -1,
        result: side === 'black' ? 'student_gain' : 'opponent_gain',
        source: 'deterministic_ece',
      },
      {
        feature_id: 'offset_count',
        target_square: 'e6',
        target_piece: 'black_knight',
        owner_side: 'black',
        attacking_side: 'white',
        perspective_side: side,
        first_capture_move: 'd5e6',
        piece_count_delta: side === 'white' ? 1 : -1,
        result: side === 'white' ? 'student_gain' : 'opponent_gain',
        source: 'deterministic_ece',
      },
      {
        feature_id: 'offset_count',
        target_square: 'f4',
        target_piece: side === 'white' ? 'black_bishop' : 'white_bishop',
        owner_side: side === 'white' ? 'black' : 'white',
        attacking_side: side,
        perspective_side: side,
        first_capture_move: side === 'white' ? 'e3f4' : 'e5f4',
        piece_count_delta: 0,
        result: 'equal',
        source: 'deterministic_ece',
      },
    ];
    output.overlays.trade_status.advantage_offset_value = 1;
    output.overlays.trade_status.disadvantage_offset_value = -1;
  }

  if (delivered >= 4) {
    if (isSideToMove) {
      output.summary = {
      source: 'deterministic_ece',
      profile: 0,
      profile_name: 'normal',
      text: summaryText,
      };
      output.immediate_warning = {
        exists: true,
        source: 'deterministic_ece',
        profile: 0,
        profile_name: 'normal',
        severity: 'medium',
        category: side === 'white' ? 'loose_piece_safety' : 'pinned_piece_safety',
        text: warningText,
      };
      output.plan = {
        source: 'deterministic_ece',
        profile: 0,
        profile_name: 'normal',
        horizon: '5_to_10_moves',
        text: side === 'white'
          ? 'Improve safety first, then build pressure with coordinated pieces.'
          : 'Resolve the pin first, then build pressure with coordinated pieces.',
      };
      if (openingRequested) {
        output.opening = {
          source: 'deterministic_opening_book',
          requested_opening: 1,
          matched: true,
          detected_opening: 'Open Game test line',
          book_moves: [],
          plan_tags: ['Open Game test line'],
          deviation: 0,
        };
      }
    }

    output.overlays.threats.student_threats = [
      { from: side === 'white' ? 'd1' : 'd8', to: side === 'white' ? 'h5' : 'h4', source: 'deterministic_ece' },
    ];
    output.overlays.threats.opponent_threats = [
      { from: side === 'white' ? 'd8' : 'd1', to: side === 'white' ? 'h4' : 'h5', source: 'deterministic_ece' },
    ];
    output.overlays.pinned_pieces.student_pinned = [
      {
        pinned_square: side === 'white' ? 'f3' : 'f6',
        pinning_piece_square: side === 'white' ? 'g4' : 'g5',
        target_square: side === 'white' ? 'e2' : 'e7',
        pin_type: 'relative',
        source: 'deterministic_ece',
      },
    ];
    output.overlays.pinned_pieces.opponent_pinned = [
      {
        pinned_square: side === 'white' ? 'f6' : 'f3',
        pinning_piece_square: side === 'white' ? 'g5' : 'g4',
        target_square: side === 'white' ? 'e7' : 'e2',
        pin_type: 'relative',
        source: 'deterministic_ece',
      },
    ];
  }

  if (includeDeep && delivered >= 5) {
    output.candidate_moves = [
      {
        rank: 1,
        uci: side === 'white' ? 'g1f3' : 'e7e5',
        san: side === 'white' ? 'Nf3' : '',
        category: 'engine_candidate',
        score: { cp: side === 'white' ? 42 : -31, mate: 0, wdl: 0, label: 'approximate' },
        pv: side === 'white' ? ['g1f3', 'b8c6', 'd2d4'] : ['e7e5', 'g1f3', 'b8c6'],
        source: 'stockfish',
      },
      {
        rank: 2,
        uci: side === 'white' ? 'd2d4' : 'e7e6',
        san: side === 'white' ? 'd4' : '',
        category: 'engine_candidate',
        score: { cp: side === 'white' ? 30 : -49, mate: 0, wdl: 0, label: 'approximate' },
        pv: side === 'white' ? ['d2d4', 'd7d5', 'g1f3'] : ['e7e6', 'd2d4', 'd7d5'],
        source: 'stockfish',
      },
      {
        rank: 3,
        uci: side === 'white' ? 'c2c3' : 'c7c5',
        san: side === 'white' ? 'c3' : '',
        category: 'engine_candidate',
        score: { cp: side === 'white' ? 18 : -51, mate: 0, wdl: 0, label: 'approximate' },
        pv: side === 'white' ? ['c2c3', 'g8f6', 'd2d4'] : ['c7c5', 'g1f3', 'b8c6'],
        source: 'stockfish',
      },
    ];
  }

  if (includeDeep && delivered >= 8) {
    output.evaluation = {
      source: 'stockfish',
      label: 'approximate',
      cp: side === 'white' ? 42 : -46,
      mate: 0,
      wdl: 0,
      pv: side === 'white' ? ['g1f3', 'b8c6', 'd2d4'] : ['e7e5', 'g1f3', 'b8c6'],
    };
    if (isSideToMove) {
      output.human_risk = {
        source: 'maia_human_risk',
        model_bucket: 'maia-1500',
        bucket_selection: 'configured_default',
        likely_human_moves: [
          {
            uci: side === 'white' ? 'g1f3' : 'e7e6',
            probability: 0.1461,
            policy_rank: 1,
            risk_label: 'likely_human_move_not_top_candidate',
            source: 'maia_human_risk',
            candidate_alignment: 'differs_from_top_candidate',
          },
          {
            uci: side === 'white' ? 'd2d4' : 'e7e5',
            probability: 0.1414,
            policy_rank: 2,
            risk_label: 'common_human_move',
            source: 'maia_human_risk',
            candidate_alignment: 'matches_top_candidate',
          },
        ],
        risk_summary_input: 'maia-1500 marks a likely human move. This is human-likelihood only, not chess-truth or candidate ranking.',
      };
    }
  }

  output.raw_deterministic.modules_used = ['deterministic_core_foundation'];
  return output;
}

function quickSideOutput(side, request, isSideToMove) {
  return sideOutput(side, side === 'white' ? request.white_level : request.black_level, side === 'white' ? request.white_level : request.black_level, isSideToMove, {
    includeDeep: false,
    openingRequested: request.custom.opening !== 0,
  });
}

function deepSideAddendum(side, request, isSideToMove) {
  const sideLevel = side === 'white' ? request.white_level : request.black_level;
  const output = sideOutput(side, sideLevel, sideLevel, isSideToMove, {
    includeDeep: true,
    openingRequested: request.custom.opening !== 0,
  });
  return {
    side: output.side,
    student_side: output.student_side,
    opponent_side: output.opponent_side,
    level: output.level,
    is_side_to_move: output.is_side_to_move,
    summary: 0,
    immediate_warning: 0,
    plan: 0,
    candidate_moves: output.candidate_moves,
    evaluation: output.evaluation,
    opening: output.opening,
    human_risk: output.human_risk,
    provider_notes: [
      {
        source: 'stockfish',
        status: sideLevel >= 5 ? 'used' : 'not_used',
        unavailable: sideLevel >= 5 ? 0 : 'level_not_allowed',
        profile: sideLevel >= 5 ? { multipv: Math.min(3, Math.max(1, sideLevel - 4)), fixture: true } : 0,
      },
      {
        source: 'cached_lichess_eval',
        status: sideLevel >= 8 ? 'used' : 'not_used',
        unavailable: sideLevel >= 8 ? 0 : 'level_not_allowed',
      },
      {
        source: 'maia_human_risk',
        status: sideLevel >= 8 ? 'used' : 'not_used',
        unavailable: sideLevel >= 8 ? 0 : 'level_not_allowed',
      },
    ],
    audit: output.audit,
  };
}

function boardPayload(input) {
  const started = Date.now();
  const request = normalizeEceRequest(input);
  const sideToMove = sideToMoveFromFen(request.input_fen);

  return {
    schema: {
      name: 'evenchess_engine_output',
      version: '1.0',
      engine_version: ENGINE_VERSION,
    },
    request_echo: requestEcho(request),
    side_outputs: {
      white: sideOutput('white', request.white_level, request.white_level, sideToMove === 'white', {
        includeDeep: true,
        openingRequested: request.custom.opening !== 0,
      }),
      black: sideOutput('black', request.black_level, request.black_level, sideToMove === 'black', {
        includeDeep: true,
        openingRequested: request.custom.opening !== 0,
      }),
    },
    diagnostics: diagnostics(
      request.mode === 'board_state' ? 'ok' : 'invalid_request',
      started,
      [
        'material',
        'offset_count',
        'hanging_pieces',
        'threats',
        'pins',
        'opening',
        'stockfish_fixture',
        'ai_text_fixture',
        'proposed_move_fixture',
        'full_game_fixture',
      ],
      ['test_ece_contains_examples_for_ui_regression_only', ...request.warnings],
      request.mode === 'board_state' ? [] : [`Mode ${request.mode} is not implemented by legacy board-state mode.`]
    ),
    unavailable: unavailable(request),
  };
}

function quickBoardPayload(input) {
  const started = Date.now();
  const request = normalizeEceRequest(input);
  const sideToMove = sideToMoveFromFen(request.input_fen);
  const validMode = request.mode === 'board_state';
  const deepRequested = validMode && request.deep_requested;

  return {
    schema: {
      name: 'evenchess_engine_output',
      version: '1.0',
      engine_version: ENGINE_VERSION,
      phase: 'quick',
    },
    request_echo: requestEcho(request),
    quick_context: {
      context_id: deepRequested ? `test_ctx_${request.request_id}` : 0,
      quick_request_id: request.request_id,
      deep_requested: deepRequested,
      deep_status: validMode ? (deepRequested ? 'ready_to_request' : 'not_requested') : 'not_available',
      deep_endpoint: '/v1/ece/board/deep',
      expires_at: deepRequested ? new Date(Date.now() + 30000).toISOString() : 0,
    },
    side_outputs: {
      white: validMode ? quickSideOutput('white', request, sideToMove === 'white') : 0,
      black: validMode ? quickSideOutput('black', request, sideToMove === 'black') : 0,
    },
    diagnostics: diagnostics(
      validMode ? 'ok' : 'invalid_request',
      started,
      ['material', 'offset_count', 'hanging_pieces', 'threats', 'pins', 'quick_text_fixture'],
      ['quick_phase_fixture', ...request.warnings],
      validMode ? [] : [`Mode ${request.mode} is not implemented by board-state quick mode.`]
    ),
    unavailable: unavailable(request),
  };
}

function deepBoardPayload(input) {
  const started = Date.now();
  const request = normalizeEceRequest(input);
  const sideToMove = sideToMoveFromFen(request.input_fen);
  const validMode = request.mode === 'board_deep';
  const contextStatus = request.quick_context_id ? 'matched' : 'missing';

  return {
    schema: {
      name: 'evenchess_engine_output',
      version: '1.0',
      engine_version: ENGINE_VERSION,
      phase: 'deep',
    },
    request_echo: requestEcho(request),
    quick_context: {
      context_id: request.quick_context_id || 0,
      quick_request_id: request.quick_request_id || 0,
      context_status: validMode ? contextStatus : 'not_checked',
    },
    side_output_addenda: {
      white: validMode && request.quick_context_id ? deepSideAddendum('white', request, sideToMove === 'white') : 0,
      black: validMode && request.quick_context_id ? deepSideAddendum('black', request, sideToMove === 'black') : 0,
    },
    diagnostics: diagnostics(
      validMode && request.quick_context_id ? 'ok' : validMode ? 'stale_context' : 'invalid_request',
      started,
      ['board_deep_mode', 'stockfish_fixture', 'maia_fixture', 'eval_fixture', 'deep_addenda_fixture'],
      ['deep_phase_fixture', ...request.warnings],
      validMode
        ? request.quick_context_id ? [] : ['quick_context_id is required for board-state deep requests.']
        : [`Mode ${request.mode} is not implemented by board-state deep mode.`]
    ),
    unavailable: unavailable(request),
  };
}

function healthPayload() {
  return {
    status: 'ok',
    service: 'EvenChessEngine',
    mode: 'test-ground-mock',
    engine_version: ENGINE_VERSION,
    production: false,
    providers: {
      rules_legal: 'test_fixture',
      stockfish: 'test_fixture',
      syzygy: 'test_fixture',
      opening_book: 'test_fixture',
      lichess_eval_cache: 'test_fixture',
      maia: 'test_fixture',
      ai: 'not_configured',
    },
    internal_auth_configured: false,
    openai_configured: false,
    stockfish_configured: true,
    test_payload: true,
  };
}

function readyPayload() {
  return {
    status: 'ok',
    service: 'EvenChessEngine',
    mode: 'test-ground-mock',
    production: false,
    checks: {
      config: {
        internal_auth: 'not_configured',
      },
      required_providers: {
        rules_legal: 'ok',
      },
      optional_providers: {
        stockfish: 'test_fixture',
        syzygy: 'test_fixture',
        opening_book: 'test_fixture',
        lichess_eval_cache: 'test_fixture',
        maia: 'test_fixture',
        ai: 'not_configured',
      },
      provider_manifest: [],
    },
    diagnostics: {
      warnings: ['test_ece_fixture_only'],
      errors: [],
    },
    test_payload: true,
  };
}

function proposedMovePayload(input) {
  const started = Date.now();
  const request = normalizeEceRequest(input);
  const movingSide = sideToMoveFromFen(request.input_fen);
  const legal = /^[a-h][1-8][a-h][1-8][qrbn]?$/.test(request.proposed_move_uci);
  const movingLevel = movingSide === 'white' ? request.white_level : request.black_level;

  return {
    schema: {
      name: 'evenchess_engine_output',
      version: '1.0',
      engine_version: ENGINE_VERSION,
      mode: 'proposed_move',
    },
    request_echo: requestEcho(request),
    proposed_move_evaluation: {
      moving_side: movingSide,
      move: request.proposed_move_uci || 0,
      legal,
      san: legal ? request.proposed_move_uci : 0,
      new_fen: 0,
      eval_before: 0,
      eval_after: 0,
      warnings: legal ? [] : ['Move could not be validated from this fixture FEN.'],
      sentence: legal && movingLevel > 0 ? `The proposed move ${request.proposed_move_uci} is legal in the Test ECE fixture.` : 0,
      source: 'test_rules_fixture',
      level: {
        requested_level: movingLevel,
        delivered_level: movingLevel,
        defaulted: movingSide === 'white' ? request.white_level_defaulted : request.black_level_defaulted,
      },
    },
    diagnostics: diagnostics(
      request.mode === 'proposed_move' ? 'ok' : 'invalid_request',
      started,
      ['proposed_move_mode', 'rules_legal_fixture'],
      request.warnings,
      request.mode === 'proposed_move' ? [] : [`Mode ${request.mode} is not implemented by proposed-move mode.`]
    ),
    unavailable: unavailable(request),
  };
}

function gameReviewPayload(input) {
  const started = Date.now();
  const request = normalizeEceRequest(input);
  const root = requestRoot(input);
  const game = root.game || {};
  const reviewLevel = level(root.review_level, 10);

  return {
    schema: {
      name: 'evenchess_full_game',
      version: '1.0',
      engine_version: ENGINE_VERSION,
    },
    request_echo: {
      request_id: request.request_id,
      mode: request.mode,
      game_id: game.game_id || 0,
      rating_type: request.rating_type,
      white_rating_input: request.white_rating_input,
      black_rating_input: request.black_rating_input,
      review_level: reviewLevel,
      use_ai_requested: request.use_ai,
      ai_used: false,
      live_ece_snapshots_count: Array.isArray(root.live_ece_snapshots) ? root.live_ece_snapshots.length : 0,
    },
    evenchess_full_game: {
      format_name: 'evenchess_full_game',
      format_version: '1.0',
      game_id: game.game_id || 'test-game-review',
      result: game.result || 'unknown',
      termination: game.termination || 'unknown',
      initial_fen: game.initial_fen || DEFAULT_FEN,
      move_count: Array.isArray(game.moves) ? Math.floor(game.moves.length / 2) : 0,
      ply_count: Array.isArray(game.moves) ? game.moves.length : 0,
      review_level: reviewLevel,
      white: {
        rating_input: request.white_rating_input,
        review_level: reviewLevel,
      },
      black: {
        rating_input: request.black_rating_input,
        review_level: reviewLevel,
      },
      turns: [
        {
          ply: 0,
          move_number: 1,
          side_to_move: sideToMoveFromFen(game.initial_fen || DEFAULT_FEN),
          fen: game.initial_fen || DEFAULT_FEN,
          move_played: 0,
          side_outputs: {
            white: sideOutput('white', reviewLevel, reviewLevel, true, { includeDeep: true, openingRequested: true }),
            black: sideOutput('black', reviewLevel, reviewLevel, false, { includeDeep: true, openingRequested: true }),
          },
          live_snapshot_ref: 0,
          review_tags: ['test_fixture'],
        },
      ],
      key_moments: [],
      phase_review: {
        opening: 0,
        middlegame: 0,
        endgame: 0,
      },
      live_vs_review: {
        live_snapshots_supplied: Array.isArray(root.live_ece_snapshots) ? root.live_ece_snapshots.length : 0,
        note: 'Review output is post-game and does not rewrite live assistance history.',
      },
      ai_game_summary: 0,
      reconstruction: {
        source: 'test_fixture',
        warnings: [],
      },
    },
    diagnostics: diagnostics(
      request.mode === 'game_review' ? 'ok' : 'invalid_request',
      started,
      ['game_review_mode', 'deterministic_board_state_fixture'],
      request.warnings,
      request.mode === 'game_review' ? [] : [`Mode ${request.mode} is not implemented by game-review mode.`]
    ),
    unavailable: unavailable(request),
  };
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === 'OPTIONS') {
      writeJson(res, 204, {});
      return;
    }
    if (req.method === 'GET' && req.url === '/health') {
      writeJson(res, 200, healthPayload());
      return;
    }
    if (req.method === 'GET' && req.url === '/ready') {
      writeJson(res, 200, readyPayload());
      return;
    }
    if (req.method === 'POST' && (req.url === '/v1/ece/board/quick' || req.url === '/v1/ece/board/deep' || req.url === '/v1/ece/board')) {
      const raw = await readBody(req);
      const input = parseJson(raw);
      const output = req.url === '/v1/ece/board/quick'
        ? quickBoardPayload(input)
        : req.url === '/v1/ece/board/deep'
          ? deepBoardPayload(input)
          : boardPayload(input);
      writeJson(res, 200, output);
      appendDebugIo({
        timestamp: new Date().toISOString(),
        method: req.method,
        endpoint: req.url,
        status_code: 200,
        request_id: output.request_echo && output.request_echo.request_id,
        mode: req.url === '/v1/ece/board/deep' ? 'board_deep' : 'board_state',
        input,
        output,
      });
      return;
    }
    if (req.method === 'POST' && (req.url === '/v1/ece/proposed-move' || req.url === '/v1/ece/game-review')) {
      const raw = await readBody(req);
      const input = parseJson(raw);
      const output = req.url === '/v1/ece/proposed-move' ? proposedMovePayload(input) : gameReviewPayload(input);
      writeJson(res, 200, output);
      appendDebugIo({
        timestamp: new Date().toISOString(),
        method: req.method,
        endpoint: req.url,
        status_code: 200,
        request_id: output.request_echo && output.request_echo.request_id,
        mode: output.request_echo && output.request_echo.mode,
        input,
        output,
      });
      return;
    }
    writeJson(res, 404, { status: 'not_found', service: 'EvenChessEngine', mode: 'test-ground-mock' });
  } catch (err) {
    writeJson(res, 400, {
      status: 'invalid_request',
      service: 'EvenChessEngine',
      mode: 'test-ground-mock',
      message: err && err.message ? err.message : 'Invalid request',
    });
  }
});

server.listen(PORT, HOST, () => {
  console.log(`EvenChess test ECE listening on http://${HOST}:${PORT}`);
});

process.on('SIGTERM', () => server.close(() => process.exit(0)));
process.on('SIGINT', () => server.close(() => process.exit(0)));
