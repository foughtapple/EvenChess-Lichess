import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import {
  isEvenChessTestGroundMoveBridgeAllowed,
  readEvenChessTestGroundMoveFromLocation,
  readEvenChessTestGroundMoveUci,
  validateEvenChessTestGroundMove,
} from '../src/evenchessTestGroundMoveBridge';

describe('EvenChess Test Ground local move bridge', () => {
  test('is local-only browser automation infrastructure', () => {
    assert.equal(isEvenChessTestGroundMoveBridgeAllowed({ protocol: 'http:', hostname: 'localhost' }), true);
    assert.equal(isEvenChessTestGroundMoveBridgeAllowed({ protocol: 'https:', hostname: '127.0.0.1' }), true);
    assert.equal(isEvenChessTestGroundMoveBridgeAllowed({ protocol: 'http:', hostname: 'example.test' }), false);
    assert.equal(isEvenChessTestGroundMoveBridgeAllowed({ protocol: 'file:', hostname: 'localhost' }), false);
  });

  test('reads UCI from string or simple event detail objects', () => {
    assert.equal(readEvenChessTestGroundMoveUci('e2e4'), 'e2e4');
    assert.equal(readEvenChessTestGroundMoveUci({ uci: 'g1f3' }), 'g1f3');
    assert.equal(readEvenChessTestGroundMoveUci({ moveUci: 'b1c3' }), 'b1c3');
    assert.equal(readEvenChessTestGroundMoveUci({ uci: 42 }), undefined);
  });

  test('reads UCI from the local browser automation URL parameter', () => {
    assert.equal(readEvenChessTestGroundMoveFromLocation({ search: '?evenchessTestMove=e2e4' }), 'e2e4');
    assert.equal(readEvenChessTestGroundMoveFromLocation({ search: '?other=e2e4' }), undefined);
  });

  test('accepts only well-formed legal moves from current possibleMoves', () => {
    const possibleMoves = 'e2e3e4 g1f3h3 e7e8';

    assert.deepEqual(validateEvenChessTestGroundMove('e2e4', possibleMoves), {
      ok: true,
      uci: 'e2e4',
      orig: 'e2',
      dest: 'e4',
    });
    assert.deepEqual(validateEvenChessTestGroundMove('E7E8Q', possibleMoves), {
      ok: true,
      uci: 'e7e8q',
      orig: 'e7',
      dest: 'e8',
      promotion: 'queen',
    });
    assert.deepEqual(validateEvenChessTestGroundMove('e2e5', possibleMoves), {
      ok: false,
      uci: 'e2e5',
      reason: 'illegal_move',
    });
    assert.deepEqual(validateEvenChessTestGroundMove('not-a-move', possibleMoves), {
      ok: false,
      reason: 'invalid_uci',
    });
  });
});
