import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import {
  evenChessTestGroundFullFen,
  requestEvenChessTestGroundOverlay,
  requestEvenChessTestGroundFullGameReview,
  shouldUseEvenChessTestGround,
  testGroundFullGameReviewUrl,
  testGroundOverlayUrl,
  testGroundPotentialMoveUrl,
  testGroundProposedMoveUrl,
} from '../src/evenchessTestGround';
import type { RoundData } from '../src/interfaces';

const roundData = (overrides: Partial<RoundData> = {}): RoundData =>
  ({
    game: { id: 'abc12345' },
    player: {
      color: 'white',
      spectator: false,
      user: { id: 'student1' },
    },
    ...overrides,
  }) as RoundData;

const visualFixture = [{ id: 'visual-fixture', label: 'a8: Hanging' }];

describe('EvenChess Test Ground round adapter', () => {
  test('enables overlay fetch for non-spectator live and computer rounds', () => {
    assert.equal(
      shouldUseEvenChessTestGround(
        { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
        roundData(),
      ),
      true,
    );
    assert.equal(
      shouldUseEvenChessTestGround(
        { origin: 'https://example.test', protocol: 'https:', hostname: 'example.test' },
        roundData(),
      ),
      true,
    );
    assert.equal(
      shouldUseEvenChessTestGround(
        { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
        roundData({ game: { id: 'synthetic' } } as Partial<RoundData>),
      ),
      true,
    );
    assert.equal(
      shouldUseEvenChessTestGround(
        { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
        roundData({ player: { color: 'white', spectator: true } } as Partial<RoundData>),
      ),
      false,
    );
  });

  test('builds same-origin ECL backend URLs with level 10 test payload inputs', () => {
    const url = testGroundOverlayUrl(
      'http://localhost:8080',
      roundData(),
      6,
      'rnbqkbnr/pppp1ppp/5n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4',
    );
    const parsed = new URL(url, 'http://localhost:8080');

    assert.equal(parsed.pathname, '/evenchess/testground/ece/board-overlay');
    assert.equal(parsed.searchParams.get('gameId'), 'abc12345');
    assert.equal(parsed.searchParams.get('ply'), '6');
    assert.equal(parsed.searchParams.get('side'), 'white');
    assert.equal(parsed.searchParams.get('level'), '10');
    assert.equal(parsed.searchParams.get('whiteLevel'), '10');
    assert.equal(parsed.searchParams.get('blackLevel'), '10');
    assert.equal(parsed.searchParams.get('playerId'), 'student1');
    assert.equal(parsed.searchParams.get('ttlMillis'), '60000');
    assert.equal(parsed.searchParams.get('eceBaseUrl'), 'http://host.docker.internal:8787');
  });

  test('expands board-only Lichess step FEN before calling ECE', () => {
    const boardOnlyFen = 'r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R';

    assert.equal(
      evenChessTestGroundFullFen(roundData(), 6, boardOnlyFen),
      'r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R w KQkq - 0 4',
    );
    assert.equal(evenChessTestGroundFullFen(roundData(), 1, 'fen-1'), 'fen-1');
    assert.equal(
      evenChessTestGroundFullFen(
        roundData(),
        6,
        'r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R w KQkq - 4 4',
      ),
      'r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R w KQkq - 4 4',
    );
  });

  test('overlay URL expands board-only FEN into the ECE board-state contract shape', () => {
    const url = testGroundOverlayUrl(
      'http://localhost:8080',
      roundData(),
      6,
      'r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R',
    );
    const parsed = new URL(url, 'http://localhost:8080');

    assert.equal(
      parsed.searchParams.get('fen'),
      'r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R w KQkq - 0 4',
    );
  });

  test('builds same-origin ECL backend URLs with retained Used Level when present', () => {
    const url = testGroundOverlayUrl(
      'http://localhost:8080',
      roundData({ evenchess: { display: { usedLevel: 4 } } } as Partial<RoundData>),
      6,
      'fen-6',
      4,
    );
    const parsed = new URL(url, 'http://localhost:8080');

    assert.equal(parsed.searchParams.get('level'), '4');
    assert.equal(parsed.searchParams.get('whiteLevel'), '4');
    assert.equal(parsed.searchParams.get('blackLevel'), '4');
  });

  test('builds same-origin ECL backend URLs for proposed-move requests', () => {
    const url = testGroundProposedMoveUrl(
      'http://localhost:8080',
      roundData(),
      9,
      'rnbqkbnr/pppp1ppp/5n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4',
      'g1f3',
      5,
    );
    const parsed = new URL(url, 'http://localhost:8080');

    assert.equal(parsed.pathname, '/evenchess/testground/ece/proposed-move');
    assert.equal(parsed.searchParams.get('gameId'), 'abc12345');
    assert.equal(parsed.searchParams.get('ply'), '9');
    assert.equal(parsed.searchParams.get('side'), 'white');
    assert.equal(parsed.searchParams.get('level'), '5');
    assert.equal(parsed.searchParams.get('moveUci'), 'g1f3');
    assert.equal(parsed.searchParams.get('proposalIndex'), '1');
    assert.equal(parsed.searchParams.get('eceBaseUrl'), 'http://host.docker.internal:8787');
  });

  test('builds same-origin ECL backend URLs for server-authorized potential move reveals', () => {
    const url = testGroundPotentialMoveUrl(
      'http://localhost:8080',
      roundData(),
      9,
      'rnbqkbnr/pppp1ppp/5n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4',
      'player',
      6,
    );
    const parsed = new URL(url, 'http://localhost:8080');

    assert.equal(parsed.pathname, '/evenchess/testground/ece/potential-move');
    assert.equal(parsed.searchParams.get('gameId'), 'abc12345');
    assert.equal(parsed.searchParams.get('ply'), '9');
    assert.equal(parsed.searchParams.get('side'), 'white');
    assert.equal(parsed.searchParams.get('level'), '6');
    assert.equal(parsed.searchParams.get('kind'), 'player');
    assert.equal(parsed.searchParams.get('playerId'), 'student1');
    assert.equal(parsed.searchParams.get('eceBaseUrl'), 'http://host.docker.internal:8787');
  });

  test('builds same-origin ECL backend URL for full-game L10 review backfill', () => {
    const url = testGroundFullGameReviewUrl('http://localhost:8080');
    const parsed = new URL(url, 'http://localhost:8080');

    assert.equal(parsed.pathname, '/evenchess/testground/ece/full-game-review');
  });

  test('queues a fresh overlay request when the board changes during an in-flight ECE call', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    let resolveFirst: (value: unknown) => void = () => undefined;
    const calls: string[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      if (calls.length === 1) {
        return new Promise(resolve => {
          resolveFirst = resolve;
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 2, 'fen-2', visualFixture)),
      });
    };

    const data = roundData();
    const ctrl = {
      data,
      ply: 1,
      stepAt: (ply: number) => ({ fen: `fen-${ply}` }),
      replaying: () => false,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: unknown) => {
        data.evenchess = { ...data.evenchess, live: overlay as any };
      },
    };

    try {
      requestEvenChessTestGroundOverlay(ctrl as any);
      ctrl.ply = 2;
      requestEvenChessTestGroundOverlay(ctrl as any);
      assert.equal(calls.length, 1);

      resolveFirst({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 1, 'fen-1', visualFixture)),
      });

      await new Promise(resolve => setTimeout(resolve, 180));

      assert.equal(calls.length, 2);
      const retryUrl = new URL(calls[1]!, 'http://localhost:8080');
      assert.equal(retryUrl.searchParams.get('ply'), '2');
      assert.equal(retryUrl.searchParams.get('fen'), 'fen-2');
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('immediately retries the current board when an ECE response arrives stale', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    let resolveFirst: (value: unknown) => void = () => undefined;
    const calls: string[] = [];
    const applied: unknown[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      if (calls.length === 1) {
        return new Promise(resolve => {
          resolveFirst = resolve;
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 2, 'fen-2', visualFixture)),
      });
    };

    const data = roundData();
    const ctrl = {
      data,
      ply: 1,
      stepAt: (ply: number) => ({ fen: `fen-${ply}` }),
      replaying: () => false,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: unknown) => {
        applied.push(overlay);
        data.evenchess = { ...data.evenchess, live: overlay as any };
      },
    };

    try {
      requestEvenChessTestGroundOverlay(ctrl as any);
      ctrl.ply = 2;

      resolveFirst({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 1, 'fen-1', visualFixture)),
      });

      await new Promise(resolve => setTimeout(resolve, 180));

      assert.equal(calls.length, 2);
      assert.equal(applied.length, 1);
      const retryUrl = new URL(calls[1]!, 'http://localhost:8080');
      assert.equal(retryUrl.searchParams.get('ply'), '2');
      assert.equal(retryUrl.searchParams.get('fen'), 'fen-2');
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('preserves forced refreshes queued behind an in-flight ECE call', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    let resolveFirst: (value: unknown) => void = () => undefined;
    const calls: string[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      if (calls.length === 1) {
        return new Promise(resolve => {
          resolveFirst = resolve;
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 1, 'fen-1', visualFixture)),
      });
    };

    const data = roundData();
    const ctrl = {
      data,
      ply: 1,
      stepAt: (ply: number) => ({ fen: `fen-${ply}` }),
      replaying: () => false,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: unknown) => {
        data.evenchess = { ...data.evenchess, live: overlay as any };
      },
    };

    try {
      requestEvenChessTestGroundOverlay(ctrl as any);
      requestEvenChessTestGroundOverlay(ctrl as any, true);
      assert.equal(calls.length, 1);

      resolveFirst({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 1, 'fen-1', visualFixture)),
      });

      await new Promise(resolve => setTimeout(resolve, 180));

      assert.equal(calls.length, 2);
      const retryUrl = new URL(calls[1]!, 'http://localhost:8080');
      assert.equal(retryUrl.searchParams.get('ply'), '1');
      assert.equal(retryUrl.searchParams.get('fen'), 'fen-1');
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('does not queue duplicate same-position requests while one ECE call is in flight', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    let resolveFirst: (value: unknown) => void = () => undefined;
    const calls: string[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      return new Promise(resolve => {
        resolveFirst = resolve;
      });
    };

    const data = roundData();
    const ctrl = {
      data,
      ply: 1,
      stepAt: (ply: number) => ({ fen: `fen-${ply}` }),
      replaying: () => false,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: unknown) => {
        data.evenchess = { ...data.evenchess, live: overlay as any };
      },
    };

    try {
      requestEvenChessTestGroundOverlay(ctrl as any);
      requestEvenChessTestGroundOverlay(ctrl as any);
      assert.equal(calls.length, 1);

      resolveFirst({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 1, 'fen-1', visualFixture)),
      });

      await new Promise(resolve => setTimeout(resolve, 180));

      assert.equal(calls.length, 1);
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('overlay requests use retained Used Level so higher-level toggles trigger higher ECE payloads', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    const calls: string[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 1, 'fen-1', visualFixture)),
      });
    };

    const data = roundData({ evenchess: { display: { usedLevel: 8 } } } as Partial<RoundData>);
    const ctrl = {
      data,
      ply: 1,
      stepAt: (ply: number) => ({ fen: `fen-${ply}` }),
      replaying: () => false,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: unknown) => {
        data.evenchess = { ...data.evenchess, live: overlay as any };
      },
    };

    try {
      requestEvenChessTestGroundOverlay(ctrl as any, true);
      await new Promise(resolve => setTimeout(resolve, 20));

      const parsed = new URL(calls[0]!, 'http://localhost:8080');
      assert.equal(parsed.searchParams.get('level'), '8');
      assert.equal(parsed.searchParams.get('whiteLevel'), '8');
      assert.equal(parsed.searchParams.get('blackLevel'), '8');
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('overlay requests fall back to preferred Used Level capped by Set Level', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    const calls: string[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 1, 'fen-1', visualFixture)),
      });
    };

    const data = roundData({
      pref: { evenchess: { preferredUsedLevel: 8 } },
      evenchess: { display: { setLevel: 4 } },
    } as Partial<RoundData>);
    const ctrl = {
      data,
      ply: 1,
      stepAt: (ply: number) => ({ fen: `fen-${ply}` }),
      replaying: () => false,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: unknown) => {
        data.evenchess = { ...data.evenchess, live: overlay as any };
      },
    };

    try {
      requestEvenChessTestGroundOverlay(ctrl as any, true);
      await new Promise(resolve => setTimeout(resolve, 20));

      const parsed = new URL(calls[0]!, 'http://localhost:8080');
      assert.equal(parsed.searchParams.get('level'), '4');
      assert.equal(parsed.searchParams.get('whiteLevel'), '4');
      assert.equal(parsed.searchParams.get('blackLevel'), '4');
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('requests cached payloads when stepping through move history replay', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    const calls: string[] = [];
    const applied: unknown[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(liveOverlay('abc12345', 3, 'fen-3', visualFixture)),
      });
    };

    const data = roundData();
    const ctrl = {
      data,
      ply: 3,
      stepAt: (ply: number) => ({ fen: `fen-${ply}` }),
      replaying: () => true,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: unknown) => {
        applied.push(overlay);
        data.evenchess = { ...data.evenchess, live: overlay as any };
      },
    };

    try {
      requestEvenChessTestGroundOverlay(ctrl as any, true);
      await new Promise(resolve => setTimeout(resolve, 20));

      assert.equal(calls.length, 1);
      assert.equal(applied.length, 1);
      const parsed = new URL(calls[0]!, 'http://localhost:8080');
      assert.equal(parsed.searchParams.get('ply'), '3');
      assert.equal(parsed.searchParams.get('fen'), 'fen-3');
      assert.equal(parsed.searchParams.get('historyOnly'), '1');
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('full-game review request posts all round FEN frames to the ECL bridge', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    let postedBody: any;
    const calls: string[] = [];
    (globalThis as any).fetch = (url: string, init?: RequestInit) => {
      calls.push(url);
      if (init?.body) postedBody = JSON.parse(String(init.body));
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve(
            init?.body ? { ok: true, framesStored: 3 } : liveOverlay('abc12345', 2, 'fen-2', visualFixture),
          ),
      });
    };

    const data = roundData();
    const ctrl = {
      data,
      ply: 2,
      lastPly: () => 2,
      stepAt: (ply: number) => ({ fen: `fen-${ply}`, uci: ply > 0 ? `a${ply}a${ply + 1}` : undefined }),
      replaying: () => true,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: unknown) => {
        data.evenchess = { ...data.evenchess, live: overlay as any };
      },
    };

    try {
      const result = await requestEvenChessTestGroundFullGameReview(ctrl as any, 10);

      assert.equal(result.ok, true);
      assert.equal(result.framesStored, 3);
      assert.equal(calls[0], '/evenchess/testground/ece/full-game-review');
      assert.equal(postedBody.gameId, 'abc12345');
      assert.equal(postedBody.side, 'white');
      assert.equal(postedBody.level, 10);
      assert.deepEqual(
        postedBody.frames.map((frame: any) => frame.ply),
        [0, 1, 2],
      );
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('retries the same position without applying an accepted empty L10 payload', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    const calls: string[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve(
            calls.length === 1
              ? liveOverlay('abc12345', 1, 'fen-1')
              : liveOverlay('abc12345', 1, 'fen-1', [{ id: 'visual-1', label: 'a8: Hanging' }]),
          ),
      });
    };

    const data = roundData();
    const appliedVisualCounts: number[] = [];
    const ctrl = {
      data,
      ply: 1,
      stepAt: (ply: number) => ({ fen: `fen-${ply}` }),
      replaying: () => false,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: any) => {
        appliedVisualCounts.push((overlay.visuals ?? []).length);
        data.evenchess = { ...data.evenchess, live: overlay };
      },
    };

    try {
      requestEvenChessTestGroundOverlay(ctrl as any);
      await new Promise(resolve => setTimeout(resolve, 1_700));

      assert.equal(calls.length, 2);
      assert.deepEqual(appliedVisualCounts, [1]);
      const retryUrl = new URL(calls[1]!, 'http://localhost:8080');
      assert.equal(retryUrl.searchParams.get('ply'), '1');
      assert.equal(retryUrl.searchParams.get('fen'), 'fen-1');
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('applies a card-only accepted payload without waiting for board visuals', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    const calls: string[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            ...liveOverlay('abc12345', 1, 'fen-1'),
            live: {
              ...liveOverlay('abc12345', 1, 'fen-1').live,
              cards: [
                {
                  id: 'card-1',
                  gameId: 'abc12345',
                  ply: 1,
                  boardStateKey: 'fen-1',
                  featureKey: 'ece.card.summarycard',
                  title: 'Summary',
                  body: 'Develop calmly.',
                  level: 10,
                  auditId: 'audit-1',
                  serverAuthorized: true,
                  approvedDisplayPayload: true,
                },
              ],
            },
          }),
      });
    };

    const data = roundData();
    const appliedCardCounts: number[] = [];
    const ctrl = {
      data,
      ply: 1,
      stepAt: (ply: number) => ({ fen: `fen-${ply}` }),
      replaying: () => false,
      redraw: () => undefined,
      applyEvenChessLiveOverlay: (overlay: any) => {
        appliedCardCounts.push((overlay.cards ?? []).length);
        data.evenchess = { ...data.evenchess, live: overlay };
      },
    };

    try {
      requestEvenChessTestGroundOverlay(ctrl as any);
      await new Promise(resolve => setTimeout(resolve, 20));

      assert.equal(calls.length, 1);
      assert.deepEqual(appliedCardCounts, [1]);
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });
});

function liveOverlay(
  gameId: string,
  ply: number,
  boardStateKey: string,
  visuals: { id: string; label: string }[] = [],
) {
  return {
    live: {
      enabled: true,
      gameId,
      ply,
      boardStateKey,
      perspective: 'white',
      auditId: `audit-${ply}`,
      serverAuthorized: true,
      ttlMillis: 60000,
      cards: [],
      visuals: visuals.map(visual => ({
        ...visual,
        gameId,
        ply,
        boardStateKey,
        featureKey: 'ece.marker.hanging_not_attackable',
        auditId: `audit-${ply}`,
        serverAuthorized: true,
        approvedDisplayPayload: true,
        stale: false,
      })),
    },
  };
}
