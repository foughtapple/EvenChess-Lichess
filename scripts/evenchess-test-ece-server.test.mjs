import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import net from 'node:net';
import { test } from 'node:test';

function freePort() {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      const port = address && typeof address === 'object' ? address.port : 0;
      server.close(() => resolve(port));
    });
  });
}

async function waitForHealth(baseUrl) {
  const deadline = Date.now() + 5000;
  let lastError;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(`${baseUrl}/health`);
      if (response.ok) return response.json();
    } catch (error) {
      lastError = error;
    }
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  throw lastError || new Error('Test ECE health did not become ready.');
}

async function postJson(url, body) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
  assert.equal(response.ok, true);
  return response.json();
}

test('Test ECE fixture mirrors real quick/deep/proposed/review envelopes', async () => {
  const port = await freePort();
  const baseUrl = `http://127.0.0.1:${port}`;
  const child = spawn(process.execPath, ['scripts/evenchess-test-ece-server.js', '--host', '127.0.0.1', '--port', String(port)], {
    stdio: 'ignore',
    windowsHide: true,
  });

  try {
    const health = await waitForHealth(baseUrl);
    assert.equal(health.service, 'EvenChessEngine');
    assert.equal(health.test_payload, true);

    const ready = await fetch(`${baseUrl}/ready`).then(response => response.json());
    assert.equal(ready.status, 'ok');
    assert.equal(ready.checks.required_providers.rules_legal, 'ok');

    const quickRequest = {
      request: {
        mode: 'board_state',
        request_id: 'fixture_quick',
        input_fen: 'rnbqkbnr/pppp1ppp/5n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4',
        rating_type: 'ecr',
        white_rating_input: 1200,
        black_rating_input: 1180,
        white_level: 4,
        black_level: 8,
        use_ai: 0,
        deep_requested: true,
        requested_deep_modules: ['stockfish', 'lichess_eval_cache', 'maia'],
        custom: { opening: 1, instructions: 0 },
      },
    };
    const quick = await postJson(`${baseUrl}/v1/ece/board/quick`, quickRequest);
    assert.equal(quick.schema.phase, 'quick');
    assert.equal(quick.quick_context.deep_status, 'ready_to_request');
    assert.equal(quick.request_echo.rating_type, 'ecr');
    assert.equal(quick.side_outputs.white.candidate_moves, 0);
    assert.equal(quick.side_outputs.black.evaluation, 0);
    assert.equal(Array.isArray(quick.side_outputs.white.overlays.threats.student_threats), true);
    assert.deepEqual(
      quick.side_outputs.white.overlays.trade_status.hanging_attackable.map(item => item.piece),
      ['white_knight', 'black_pawn'],
    );
    assert.deepEqual(
      quick.side_outputs.white.overlays.trade_status.offset_count.map(item => [
        item.target_square,
        item.piece_count_delta,
        item.result,
      ]),
      [
        ['d3', -1, 'opponent_gain'],
        ['e6', 1, 'student_gain'],
        ['f4', 0, 'equal'],
      ],
    );
    assert.deepEqual(
      quick.side_outputs.black.overlays.trade_status.offset_count.map(item => [
        item.target_square,
        item.piece_count_delta,
        item.result,
      ]),
      [
        ['d3', 1, 'student_gain'],
        ['e6', -1, 'opponent_gain'],
        ['f4', 0, 'equal'],
      ],
    );

    const deep = await postJson(`${baseUrl}/v1/ece/board/deep`, {
      request: {
        mode: 'board_deep',
        request_id: 'fixture_deep',
        quick_request_id: 'fixture_quick',
        quick_context_id: quick.quick_context.context_id,
        input_fen: quickRequest.request.input_fen,
        white_level: 4,
        black_level: 8,
        use_ai: 0,
        requested_deep_modules: ['stockfish', 'lichess_eval_cache', 'maia'],
      },
    });
    assert.equal(deep.schema.phase, 'deep');
    assert.equal(deep.quick_context.context_status, 'matched');
    assert.equal(deep.side_output_addenda.white.candidate_moves, 0);
    assert.equal(deep.side_output_addenda.black.evaluation.source, 'stockfish');
    assert.equal(Array.isArray(deep.side_output_addenda.black.provider_notes), true);

    const proposed = await postJson(`${baseUrl}/v1/ece/proposed-move`, {
      request: {
        mode: 'proposed_move',
        request_id: 'fixture_pm',
        input_fen: quickRequest.request.input_fen,
        proposed_move_uci: 'g1f3',
        white_level: 10,
        black_level: 10,
      },
    });
    assert.equal(proposed.schema.mode, 'proposed_move');
    assert.equal(proposed.request_echo.proposed_move_uci, 'g1f3');
    assert.equal(proposed.proposed_move_evaluation.legal, true);
    assert.equal(proposed.proposed_move_evaluation.move, 'g1f3');
    assert.equal(typeof proposed.proposed_move_evaluation.sentence, 'string');

    const review = await postJson(`${baseUrl}/v1/ece/game-review`, {
      request: {
        mode: 'game_review',
        request_id: 'fixture_review',
        review_level: 10,
        game: { game_id: 'fixture_game', initial_fen: quickRequest.request.input_fen, moves: ['g1f3'] },
      },
    });
    assert.equal(review.schema.name, 'evenchess_full_game');
    assert.equal(review.evenchess_full_game.format_name, 'evenchess_full_game');
  } finally {
    child.kill();
  }
});
