import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import { type EvenChessTtsConfig, ttsSafetyReason } from 'lib/evenchessTts';

import type {
  EvenChessLevelFeatureKey,
  EvenChessLiveOverlay,
  EvenChessPotentialMoveKind,
  EvenChessPotentialMoveReveal,
  RoundData,
} from '../src/interfaces';
import {
  applyEvenChessLevelPreset,
  applyEvenChessLiveOverlay,
  clearEvenChessLiveOverlay,
  evenChessBoardShapes,
  evenChessOpeningWikiPathFromSteps,
  evenChessTtsAutoDelayMillis,
  evenChessTtsConfigForData,
  initializeEvenChessDisplayForGame,
  liveCardTtsItem,
  overlayStaleReason,
  payloadHasUnsafeDisplayData,
  potentialMoveQuotaForUsedLevel,
  proposedMoveQuotaForUsedLevel,
  readEvenChessProposedMoveSelection,
  requestEvenChessPotentialMoves,
  requestEvenChessProposedMovePreview,
  renderEvenChessBoardOverlay,
  renderEvenChessOverlay,
  renderableEvenChessBoardOverlayItems,
  renderableEvenChessBoardShapes,
  renderableEvenChessCards,
  renderableEvenChessVisuals,
  selectedEvenChessDisplayLevel,
  setEvenChessLevelFeature,
  shouldRenderEvenChessOverlay,
  syncEvenChessCoachTextSnapshot,
  syncEvenChessProposedMovePreview,
} from '../src/view/evenchessOverlay';

const board = {
  gameId: 'live-game',
  ply: 12,
  boardStateKey: 'fen-key-12',
  now: 1000,
};

const allFeatureKeys: EvenChessLevelFeatureKey[] = [
  'rules',
  'loosePieces',
  'hangingPieces',
  'offsetCount',
  'studentThreats',
  'opponentThreats',
  'pins',
  'coachText',
  'candidate1',
  'candidate2',
  'openingWiki',
  'candidate3',
  'evalBar',
  'evalNumbers',
  'humanRisk',
  'expertLines',
  'fullSpecificity',
];

const overlay = (): EvenChessLiveOverlay => ({
  enabled: true,
  gameId: board.gameId,
  ply: board.ply,
  boardStateKey: board.boardStateKey,
  perspective: 'white',
  auditId: 'audit-live-12',
  serverAuthorized: true,
  ttlMillis: 5000,
  expiresAt: Number.MAX_SAFE_INTEGER,
  cards: [
    {
      id: 'card-a',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'offset_count',
      title: 'Offset Count',
      body: 'Equal trade',
      level: 3,
      auditId: 'audit-live-12',
      defaultActive: true,
      serverAuthorized: true,
      approvedDisplayPayload: true,
      ttlMillis: 5000,
    },
    {
      id: 'card-b',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'candidate',
      title: 'Candidate',
      body: 'Consider the forcing move.',
      level: 5,
      auditId: 'audit-live-12',
      defaultActive: false,
      serverAuthorized: true,
      approvedDisplayPayload: true,
      ttlMillis: 5000,
    },
  ],
  visuals: [
    {
      id: 'visual-a',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'ece.marker.hanging_attackable.student',
      label: 'f6: Hanging and attackable',
      auditId: 'audit-live-12',
      primary: true,
      serverAuthorized: true,
      approvedDisplayPayload: true,
    },
  ],
});

const roundData = (live?: EvenChessLiveOverlay): RoundData =>
  ({
    game: { id: board.gameId },
    pref: { evenchess: { preferredUsedLevel: 10 } },
    evenchess: live ? { live } : undefined,
  }) as RoundData;

const ttsConfig: EvenChessTtsConfig = {
  enabled: true,
  provider: 'browser-speech',
  serverAuthorized: true,
  policyVersion: 'tts-v1',
  muteDuringOpponentTurn: true,
};

const proposedMoveCtrl = (shapes: Array<{ orig: Key; dest?: Key; brush: string }>) => ({
  data: {
    game: { id: board.gameId },
    player: { color: 'white', spectator: false },
    opponent: { color: 'black' },
    pref: { evenchess: { preferredUsedLevel: 10 } },
    evenchess: { display: { setLevel: 10, preferredUsedLevel: 10, usedLevel: 10 } },
  } as RoundData,
  ply: board.ply,
  stepAt: () => ({ fen: board.boardStateKey }),
  canMove: () => true,
  flip: false,
  redraw: () => undefined,
  chessground: {
    state: {
      drawable: { shapes },
      movable: {
        dests: new Map<Key, Key[]>([
          ['g1', ['f3']],
          ['b1', ['c3']],
        ]),
      },
      pieces: new Map<Key, Piece>([['g1', { role: 'knight', color: 'white' }]]),
    },
  },
});

const proposedMoveCard = (key: string) => ({
  key,
  gameId: board.gameId,
  ply: board.ply,
  boardStateKey: board.boardStateKey,
  perspective: 'white' as Color,
  moveUci: 'g1f3',
  san: 'Nf3',
  legal: true,
  level: 5,
  title: 'Proposed Move g1f3',
  body: 'Develop the knight.',
  auditId: 'audit-proposed',
  serverAuthorized: true,
  approvedDisplayPayload: true,
});

const potentialMoveRevealKey = (kind: EvenChessPotentialMoveKind, usedLevel: number): string =>
  `${board.gameId}:white:potential:${kind}:L${usedLevel}:${board.ply}:${board.boardStateKey}`;

const potentialMoveVisual = (
  live: EvenChessLiveOverlay,
  index: number,
  label: string,
  auditId = live.auditId,
): EvenChessPotentialMoveReveal['visuals'][number] => ({
  id: `potential-visual-${index}`,
  gameId: board.gameId,
  ply: board.ply,
  boardStateKey: board.boardStateKey,
  featureKey: `ece.candidate.${index}`,
  label,
  auditId,
  primary: false,
  serverAuthorized: true,
  approvedDisplayPayload: true,
});

const potentialMoveReveal = (
  live: EvenChessLiveOverlay,
  kind: EvenChessPotentialMoveKind,
  usedLevel: number,
  visuals: EvenChessPotentialMoveReveal['visuals'],
  cards: EvenChessPotentialMoveReveal['cards'] = [],
  auditId = live.auditId,
  perspective: 'white' | 'black' = 'white',
): EvenChessPotentialMoveReveal => {
  const quota = potentialMoveQuotaForUsedLevel(usedLevel, kind);
  return {
    key: potentialMoveRevealKey(kind, usedLevel),
    gameId: board.gameId,
    playerId: 'student1',
    ply: board.ply,
    boardStateKey: board.boardStateKey,
    perspective,
    kind,
    level: usedLevel,
    quota,
    consumed: 1,
    cards,
    visuals,
    auditId,
    serverAuthorized: true,
    approvedDisplayPayload: true,
  };
};

const coachTextCtrl = (data: RoundData, canMove: () => boolean, ply = board.ply) => ({
  data,
  ply,
  stepAt: () => ({ fen: data.evenchess?.live?.boardStateKey ?? board.boardStateKey }),
  canMove,
  replaying: () => false,
  flip: false,
  redraw: () => undefined,
  chessground: {
    state: {
      drawable: { shapes: [] },
      movable: { dests: new Map<Key, Key[]>() },
      pieces: new Map<Key, Piece>(),
    },
  },
});

describe('EvenChess live round overlay adapter', () => {
  test('normal games without EvenChess payloads do not render overlays', () => {
    assert.equal(overlayStaleReason(undefined, board), 'not-enabled');
    assert.equal(shouldRenderEvenChessOverlay(undefined, board), false);
  });

  test('round UI still renders the coach shell before a payload exists', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => false,
    };

    const rendered = renderEvenChessOverlay(ctrl as any);
    const serialized = JSON.stringify(rendered);

    assert.equal((rendered as any).sel, 'aside.evenchess-live');
    assert.match(serialized, /EvenChess Coach/);
    assert.match(serialized, /Set Level: 10/);
    assert.match(serialized, /Used Level: 0/);
    assert.match(serialized, /Apply up to/);
    assert.match(serialized, /Full Co-pilot/);
    assert.match(serialized, /Coach text/);
    assert.match(serialized, /Hanging pieces/);
    assert.match(serialized, /Awaiting payload/);
    assert.doesNotMatch(serialized, /Payload ready/);
  });

  test('live opening WikiBook path is derived from board move history', () => {
    const steps = [
      { ply: 0, fen: 'start', san: '', uci: '' },
      { ply: 1, fen: 'fen-1', san: 'e4', uci: 'e2e4' },
      { ply: 2, fen: 'fen-2', san: 'c5', uci: 'c7c5' },
      { ply: 3, fen: 'fen-3', san: 'Nf3+', uci: 'g1f3' },
      { ply: 4, fen: 'fen-4', san: 'd6', uci: 'd7d6' },
    ] as RoundData['steps'];

    assert.equal(
      evenChessOpeningWikiPathFromSteps(steps, 4),
      '1._e4/1...c5/2._Nf3/2...d6',
    );
  });

  test('level 6 WikiBook toggle renders below coach controls and above levels', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      steps: [
        { ply: 0, fen: 'start', san: '', uci: '' },
        { ply: 1, fen: 'fen-1', san: 'e4', uci: 'e2e4' },
        { ply: 2, fen: 'fen-2', san: 'c5', uci: 'c7c5' },
      ],
      evenchess: { display: { setLevel: 10 } },
    } as RoundData;
    const ctrl = {
      data,
      ply: 2,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => false,
    };

    applyEvenChessLevelPreset(data, 6);
    const serialized = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.match(serialized, /evenchess-live__level-column/);
    assert.match(serialized, /wikibook-field/);
    assert.match(serialized, /analyse__wiki/);
    assert.match(serialized, /toggle-box/);
    assert.match(serialized, /toggle-box--ready/);
    assert.match(serialized, /WikiBook/);
    assert.match(serialized, /No WikiBook entry for this line yet/);
    assert.match(serialized, /data-opening-path/);
    assert.ok(serialized.includes('1._e4/1...c5'));
    assert.match(serialized, /My potentials/);

    const coachColumnIndex = serialized.indexOf('evenchess-live__coach-column');
    const levelColumnIndex = serialized.indexOf('evenchess-live__level-column');
    const wikiIndex = serialized.indexOf('WikiBook');
    const levelsIndex = serialized.indexOf('EvenChess Levels');
    assert.ok(coachColumnIndex >= 0);
    assert.ok(levelColumnIndex > coachColumnIndex);
    assert.ok(wikiIndex >= 0);
    assert.ok(levelsIndex > wikiIndex);
  });

  test('opening WikiBook fieldset follows the L6 feature toggle', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      steps: [
        { ply: 0, fen: 'start', san: '', uci: '' },
        { ply: 1, fen: 'fen-1', san: 'e4', uci: 'e2e4' },
      ],
      evenchess: { display: { setLevel: 10 } },
    } as RoundData;
    const ctrl = {
      data,
      ply: 1,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => false,
    };

    applyEvenChessLevelPreset(data, 6);
    setEvenChessLevelFeature(data, 'openingWiki', false);

    assert.doesNotMatch(
      JSON.stringify(renderEvenChessOverlay(ctrl as any)),
      /wikibook-field/,
    );

    setEvenChessLevelFeature(data, 'openingWiki', true);

    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /wikibook-field/);
    assert.ok((data.evenchess?.display?.usedLevel ?? 0) >= 6);
  });

  test('level feature toggles do not remount the live panel and reset list scroll', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      evenchess: { live: overlay() },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => false,
    };
    applyEvenChessLevelPreset(data, 10);
    const before = renderEvenChessOverlay(ctrl as any) as any;
    const beforeKey = before?.data?.key;

    setEvenChessLevelFeature(data, 'candidate3', false);

    const after = renderEvenChessOverlay(ctrl as any) as any;
    assert.equal(after?.data?.key, beforeKey);
    assert.notEqual(after?.data?.attrs?.['data-feature-selection'], before?.data?.attrs?.['data-feature-selection']);
  });

  test('move updates keep stable overlay keys while previous safe visuals stay visible during refresh', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      evenchess: { live: overlay() },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: (ply: number) => ({ fen: ply === 13 ? 'fen-key-13' : board.boardStateKey }),
      canMove: () => true,
    };
    applyEvenChessLevelPreset(data, 10);

    const liveBefore = renderEvenChessOverlay(ctrl as any) as any;
    const boardBefore = renderEvenChessBoardOverlay(ctrl as any) as any;

    clearEvenChessLiveOverlay(data, 'move-played', 13, 'fen-key-13');

    const boardDuringIntermediateRedraw = renderEvenChessBoardOverlay(ctrl as any) as any;
    assert.equal(boardDuringIntermediateRedraw?.data?.key, boardBefore?.data?.key);
    assert.equal(boardDuringIntermediateRedraw?.data?.attrs?.['data-transition'], 'move-refresh');

    ctrl.ply = 13;

    const liveDuringLoad = renderEvenChessOverlay(ctrl as any) as any;
    const boardDuringLoad = renderEvenChessBoardOverlay(ctrl as any) as any;

    assert.equal(liveDuringLoad?.data?.key, liveBefore?.data?.key);
    assert.equal(boardDuringLoad?.data?.key, boardBefore?.data?.key);
    assert.equal(boardDuringLoad?.data?.attrs?.['data-transition'], 'move-refresh');
    assert.match(JSON.stringify(boardDuringLoad), /evenchess-board-overlay__highlight/);
    assert.match(JSON.stringify(liveDuringLoad), /Equal trade/);

    const next = overlay();
    next.ply = 13;
    next.boardStateKey = 'fen-key-13';
    next.auditId = 'audit-live-13';
    next.cards = next.cards?.map(card => ({
      ...card,
      ply: 13,
      boardStateKey: 'fen-key-13',
      auditId: 'audit-live-13',
      body: card.id === 'card-a' ? 'Updated coach text.' : card.body,
    }));
    next.visuals = next.visuals?.map(visual => ({
      ...visual,
      ply: 13,
      boardStateKey: 'fen-key-13',
      auditId: 'audit-live-13',
    }));
    applyEvenChessLiveOverlay(data, next);

    const liveAfter = renderEvenChessOverlay(ctrl as any) as any;
    const boardAfter = renderEvenChessBoardOverlay(ctrl as any) as any;

    assert.equal(liveAfter?.data?.key, liveBefore?.data?.key);
    assert.equal(boardAfter?.data?.key, boardBefore?.data?.key);
    assert.match(JSON.stringify(liveAfter), /Updated coach text/);
  });

  test('local rounds render the coach shell before a payload exists', () => {
    const data = {
      game: { id: 'synthetic' },
      local: true,
      player: { color: 'white', spectator: true },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => false,
    };

    const rendered = renderEvenChessOverlay(ctrl as any);
    const serialized = JSON.stringify(rendered);

    assert.equal((rendered as any).sel, 'aside.evenchess-live');
    assert.match(serialized, /EvenChess Coach/);
    assert.match(serialized, /Set Level: 10/);
    assert.match(serialized, /Used Level: 0/);
    assert.match(serialized, /Apply up to/);
    assert.match(serialized, /Awaiting payload/);
  });

  test('local Test Ground state renders a visible coach and level shell before payload arrival', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white' },
      evenchess: {
        testGround: {
          enabled: true,
          level: 10,
          status: 'loading',
          message: 'Checking ECE',
        },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => false,
    };

    const rendered = renderEvenChessOverlay(ctrl as any);
    const serialized = JSON.stringify(rendered);

    assert.equal((rendered as any).sel, 'aside.evenchess-live');
    assert.match(serialized, /EvenChess Coach/);
    assert.match(serialized, /Set Level: 10/);
    assert.match(serialized, /Used Level: 0/);
    assert.match(serialized, /Apply up to/);
    assert.match(serialized, /Full Co-pilot/);
    assert.match(serialized, /Coach text/);
    assert.match(serialized, /Checking ECE/);
  });

  test('full-game review request button shows a spinner while processing', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      evenchess: {
        testGround: {
          enabled: true,
          level: 10,
          status: 'loading',
          message: 'Running L10 review',
        },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => false,
      replaying: () => true,
    };

    const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.match(rendered, /evenchess-live__spinner/);
    assert.match(rendered, /Running EvenChess full-game level 10 review/);
    assert.match(rendered, /Processing full game/);
  });

  test('coach column does not duplicate board visuals as text chips', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live: overlay(),
        display: { setLevel: 10 },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => false,
      flip: false,
    };

    const rendered = renderEvenChessOverlay(ctrl as any);
    const serialized = JSON.stringify(rendered);

    assert.match(serialized, /data-display-visual-count/);
    assert.doesNotMatch(serialized, /evenchess-live__visuals/);
    assert.doesNotMatch(serialized, /f6: Hanging and attackable/);
  });

  test('level preset toggles all features up to the capped selected level', () => {
    const data = {
      game: { id: board.gameId },
      evenchess: { display: { setLevel: 6 } },
    } as RoundData;

    applyEvenChessLevelPreset(data, 9);

    assert.equal(selectedEvenChessDisplayLevel(data), 6);
    assert.equal(data.evenchess?.display?.usedLevel, 6);
    assert.equal(data.evenchess?.display?.toggles?.appliedLevel, 6);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.candidate2, true);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.candidate3, false);

    applyEvenChessLevelPreset(data, 2);

    assert.equal(selectedEvenChessDisplayLevel(data), 2);
    assert.equal(data.evenchess?.display?.usedLevel, 6);
    assert.equal(data.evenchess?.display?.toggles?.appliedLevel, 2);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.hangingPieces, true);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.offsetCount, false);
  });

  test('level preset applies saved default feature toggles for the selected level', () => {
    const data = {
      game: { id: board.gameId },
      pref: {
        evenchess: {
          preferredUsedLevel: 0,
          defaultFeatureToggles: {
            offsetCount: false,
            candidate2: false,
            evalNumbers: false,
          },
        },
      },
      evenchess: { display: { setLevel: 10 } },
    } as RoundData;

    applyEvenChessLevelPreset(data, 8);

    assert.equal(data.evenchess?.display?.usedLevel, 8);
    assert.equal(data.evenchess?.display?.toggles?.appliedLevel, 8);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.hangingPieces, true);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.offsetCount, false);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.candidate2, false);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.candidate3, true);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.evalBar, true);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.evalNumbers, false);
  });

  test('new games initialize from preferred Used Level capped by Set Level', () => {
    const data = {
      game: { id: board.gameId },
      pref: {
        evenchess: {
          preferredUsedLevel: 8,
          defaultFeatureToggles: {
            candidate1: false,
          },
        },
      },
      evenchess: { display: { setLevel: 5 } },
    } as RoundData;

    initializeEvenChessDisplayForGame(data);

    assert.equal(data.evenchess?.display?.initializedForGameId, board.gameId);
    assert.equal(data.evenchess?.display?.preferredUsedLevel, 5);
    assert.equal(data.evenchess?.display?.usedLevel, 5);
    assert.equal(data.evenchess?.display?.toggles?.appliedLevel, 5);
    assert.equal(selectedEvenChessDisplayLevel(data), 4);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.candidate1, false);
    assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.candidate2, false);

    applyEvenChessLevelPreset(data, 2);
    initializeEvenChessDisplayForGame(data);

    assert.equal(selectedEvenChessDisplayLevel(data), 2);
    assert.equal(data.evenchess?.display?.usedLevel, 5);
  });

  test('new games without preferred Used Level start at L0 even when Set Level is higher', () => {
    const data = {
      game: { id: board.gameId },
      pref: {
        evenchess: {
          defaultSetLevel: 10,
        },
      },
      evenchess: { display: { setLevel: 6 } },
    } as RoundData;

    initializeEvenChessDisplayForGame(data);

    assert.equal(data.evenchess?.display?.initializedForGameId, board.gameId);
    assert.equal(data.evenchess?.display?.preferredUsedLevel, 0);
    assert.equal(data.evenchess?.display?.usedLevel, 0);
    assert.equal(data.evenchess?.display?.toggles?.appliedLevel, 0);
    assert.equal(selectedEvenChessDisplayLevel(data), 0);
  });

  test('manual feature selection raises but never lowers the retained used level', () => {
    const data = {
      game: { id: board.gameId },
      evenchess: {
        display: {
          setLevel: 10,
          preferredUsedLevel: 3,
          usedLevel: 3,
          toggles: { coachCards: true, boardVisuals: true, appliedLevel: 3 },
        },
      },
    } as RoundData;

    setEvenChessLevelFeature(data, 'evalBar', true);

    assert.equal(selectedEvenChessDisplayLevel(data), 8);
    assert.equal(data.evenchess?.display?.usedLevel, 8);

    setEvenChessLevelFeature(data, 'evalBar', false);

    assert.equal(selectedEvenChessDisplayLevel(data), 3);
    assert.equal(data.evenchess?.display?.usedLevel, 8);
  });

  test('eval bar toggle hides the eval bar without lowering retained used level', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live: overlay(),
        display: { setLevel: 10 },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => false,
      flip: false,
    };

    applyEvenChessLevelPreset(data, 10);
    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /evenchess-live__eval/);

    setEvenChessLevelFeature(data, 'evalBar', false);

    assert.doesNotMatch(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /evenchess-live__eval/);
    assert.equal(data.evenchess?.display?.usedLevel, 10);
  });

  test('eval text renders as a color-coded coach-card strip', () => {
    const live = overlay();
    live.visuals.push({
      id: 'visual-eval',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'ece.eval.deep',
      label: 'Eval -235 cp',
      auditId: live.auditId,
      primary: false,
      serverAuthorized: true,
      approvedDisplayPayload: true,
    });
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: { setLevel: 10 },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
    };

    applyEvenChessLevelPreset(data, 10);
    const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));
    assert.match(rendered, /evenchess-live__coach-eval/);
    assert.match(rendered, /Worse/);
    assert.match(rendered, /-2\.35/);
    assert.match(rendered, /"background":"#f97316"/);

    setEvenChessLevelFeature(data, 'evalNumbers', false);

    assert.doesNotMatch(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /evenchess-live__coach-eval/);
    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /evenchess-live__eval/);
  });

  test('eval display prefers the latest advanced eval visual over an earlier quick zero', () => {
    const live = overlay();
    live.visuals.push(
      {
        id: 'visual-eval-quick',
        gameId: board.gameId,
        ply: board.ply,
        boardStateKey: board.boardStateKey,
        featureKey: 'ece.eval',
        label: 'Eval +0 cp',
        auditId: live.auditId,
        primary: false,
        serverAuthorized: true,
        approvedDisplayPayload: true,
      },
      {
        id: 'visual-eval-deep',
        gameId: board.gameId,
        ply: board.ply,
        boardStateKey: board.boardStateKey,
        featureKey: 'ece.eval',
        label: 'Stockfish eval +184 cp',
        auditId: live.auditId,
        primary: false,
        serverAuthorized: true,
        approvedDisplayPayload: true,
      },
    );
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: { setLevel: 10 },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
    };

    applyEvenChessLevelPreset(data, 10);
    const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.match(rendered, /evenchess-live__coach-eval/);
    assert.match(rendered, /Better/);
    assert.match(rendered, /\+1\.84/);
    assert.doesNotMatch(rendered, /evenchess-live__coach-eval-state","data":\{\},"text":"Equal/);
  });

  test('eval display uses structured White-positive cp over side-relative label text', () => {
    const live = overlay();
    live.visuals.push({
      id: 'visual-eval-structured',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'ece.eval.deep',
      label: 'black eval +70 cp',
      evalCpWhite: -70,
      evalSource: 'stockfish',
      auditId: live.auditId,
      primary: false,
      serverAuthorized: true,
      approvedDisplayPayload: true,
    });
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: { setLevel: 10 },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
    };

    applyEvenChessLevelPreset(data, 10);
    const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.match(rendered, /Slightly worse/);
    assert.match(rendered, /-0\.70/);
    assert.doesNotMatch(rendered, /\+0\.70/);
  });

  test('eval display renders structured White-positive mate scores', () => {
    const live = overlay();
    live.visuals.push({
      id: 'visual-eval-mate',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'ece.eval.deep',
      label: 'stockfish eval #-3',
      evalMateWhite: -3,
      evalSource: 'stockfish',
      auditId: live.auditId,
      primary: false,
      serverAuthorized: true,
      approvedDisplayPayload: true,
    });
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: { setLevel: 10 },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
    };

    applyEvenChessLevelPreset(data, 10);
    const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.match(rendered, /Mated/);
    assert.match(rendered, /#3/);
  });

  test('eval display ignores quick placeholder zero when no advanced eval is present', () => {
    const live = overlay();
    live.visuals.push({
      id: 'visual-eval-quick',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'ece.eval',
      label: 'Eval +0 cp',
      auditId: live.auditId,
      primary: false,
      serverAuthorized: true,
      approvedDisplayPayload: true,
    });
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: { setLevel: 10 },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
    };

    applyEvenChessLevelPreset(data, 10);
    const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.doesNotMatch(rendered, /evenchess-live__coach-eval/);
    assert.doesNotMatch(rendered, /evenchess-live__coach-eval-state/);
  });

  test('eval display ignores stockfish-labelled quick zero placeholders', () => {
    const live = overlay();
    live.visuals.push({
      id: 'visual-eval-quick-stockfish',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'ece.eval',
      label: 'Stockfish eval +0 cp',
      auditId: live.auditId,
      primary: false,
      serverAuthorized: true,
      approvedDisplayPayload: true,
    });
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: { setLevel: 10 },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
    };

    applyEvenChessLevelPreset(data, 10);
    const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.doesNotMatch(rendered, /evenchess-live__coach-eval/);
  });

  test('eval display retains the last accepted eval until a new eval payload arrives', () => {
    const live = overlay();
    live.visuals.push({
      id: 'visual-eval-deep',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'ece.eval.deep',
      label: 'Stockfish eval +184 cp',
      auditId: live.auditId,
      primary: false,
      serverAuthorized: true,
      approvedDisplayPayload: true,
    });
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: { setLevel: 10 },
      },
    } as RoundData;
    let currentPly = board.ply;
    let currentFen = board.boardStateKey;
    const ctrl = {
      data,
      get ply() {
        return currentPly;
      },
      stepAt: () => ({ fen: currentFen }),
      canMove: () => true,
      flip: false,
    };

    applyEvenChessLevelPreset(data, 10);
    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /\+1\.84/);

    const later = overlay();
    later.ply = 13;
    later.boardStateKey = 'fen-key-13';
    later.auditId = 'audit-live-13';
    later.visuals = (later.visuals ?? []).map(visual => ({
      ...visual,
      ply: later.ply,
      boardStateKey: later.boardStateKey,
      auditId: later.auditId,
    }));
    later.cards = (later.cards ?? []).map(card => ({
      ...card,
      ply: later.ply,
      boardStateKey: later.boardStateKey,
      auditId: later.auditId,
    }));

    currentPly = later.ply;
    currentFen = later.boardStateKey;
    applyEvenChessLiveOverlay(data, later);
    const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.match(rendered, /evenchess-live__coach-eval/);
    assert.match(rendered, /\+1\.84/);
    assert.doesNotMatch(rendered, /evenchess-live__coach-eval-state","data":\{\},"text":"Equal/);
  });

  test('manual feature toggles persist when a later ECE payload is applied', () => {
    const data = {
      game: { id: board.gameId },
      evenchess: { display: { setLevel: 10 } },
    } as RoundData;

    applyEvenChessLevelPreset(data, 10);
    for (const key of allFeatureKeys) setEvenChessLevelFeature(data, key, false);

    assert.equal(selectedEvenChessDisplayLevel(data), 0);
    assert.equal(data.evenchess?.display?.usedLevel, 10);

    applyEvenChessLiveOverlay(data, overlay());

    assert.equal(selectedEvenChessDisplayLevel(data), 0);
    assert.equal(data.evenchess?.display?.usedLevel, 10);
    for (const key of allFeatureKeys) {
      assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.[key], false);
    }

    const later = overlay();
    later.ply = 13;
    later.boardStateKey = 'fen-key-13';
    later.auditId = 'audit-live-13';
    later.cards = later.cards?.map(card => ({
      ...card,
      id: `${card.id}-13`,
      ply: later.ply,
      boardStateKey: later.boardStateKey,
      auditId: later.auditId,
    }));
    later.visuals = later.visuals?.map(visual => ({
      ...visual,
      id: `${visual.id}-13`,
      ply: later.ply,
      boardStateKey: later.boardStateKey,
      auditId: later.auditId,
    }));

    applyEvenChessLiveOverlay(data, later);

    assert.equal(selectedEvenChessDisplayLevel(data), 0);
    assert.equal(data.evenchess?.display?.usedLevel, 10);
    for (const key of allFeatureKeys) {
      assert.equal(data.evenchess?.display?.toggles?.levelFeatures?.[key], false);
    }
  });

  test('per-level board toggles gate board-attached visuals without changing payload purity helpers', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live: overlay(),
        display: {
          setLevel: 10,
          toggles: {
            coachCards: true,
            boardVisuals: true,
            appliedLevel: 1,
            levelFeatures: { hangingPieces: false },
          },
        },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      flip: false,
      redraw: () => undefined,
    };

    assert.equal(
      renderableEvenChessBoardOverlayItems(data, data.evenchess?.live, board).indicators.length,
      0,
    );
    assert.equal(evenChessBoardShapes(ctrl as any).length, 0);
    assert.equal(renderableEvenChessBoardShapes(data.evenchess?.live, board).length, 1);
    assert.equal(renderEvenChessBoardOverlay(ctrl as any), undefined);

    setEvenChessLevelFeature(data, 'hangingPieces', true);

    assert.equal(evenChessBoardShapes(ctrl as any).length, 0);
    assert.equal(
      renderableEvenChessBoardOverlayItems(data, data.evenchess?.live, board).indicators.length,
      1,
    );
    assert.equal((renderEvenChessBoardOverlay(ctrl as any) as any).sel, 'div.evenchess-board-overlay');
  });

  test('Offset Count renders every signed side-output board fact including own and opponent targets', () => {
    const live = overlay();
    live.visuals = [
      {
        ...live.visuals![0]!,
        id: 'offset-own-piece-unfavourable',
        featureKey: 'ece.marker.offset_count.opponent_win',
        label: 'd3: Offset Count -1',
      },
      {
        ...live.visuals![0]!,
        id: 'offset-opponent-piece-favourable',
        featureKey: 'ece.marker.offset_count.student_win',
        label: 'e6: Offset Count 1',
      },
      {
        ...live.visuals![0]!,
        id: 'offset-equal',
        featureKey: 'ece.marker.offset_count.equal',
        label: 'f4: Offset Count 0',
      },
    ];
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: {
          setLevel: 10,
          toggles: { coachCards: true, boardVisuals: true, appliedLevel: 10 },
        },
      },
    } as RoundData;

    let items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.deepEqual(
      items.indicators.map(indicator => [indicator.square, indicator.text, indicator.colour, indicator.icon]),
      [
        ['d3', '1', '#dc2626', undefined],
        ['e6', '1', '#16a34a', undefined],
        ['f4', '0', '#2563eb', 'shield'],
      ],
    );

    setEvenChessLevelFeature(data, 'offsetCount', false);
    items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(items.indicators.length, 0);
  });

  test('real ECE-style feature toggles gate matching board overlay families', () => {
    const live = overlay();
    live.visuals = [
      {
        id: 'real-loose',
        gameId: board.gameId,
        ply: board.ply,
        boardStateKey: board.boardStateKey,
        featureKey: 'ece.marker.hanging_not_attackable',
        label: 'c4: Hanging',
        auditId: 'audit-live-12',
        serverAuthorized: true,
        approvedDisplayPayload: true,
      },
      {
        id: 'real-hanging',
        gameId: board.gameId,
        ply: board.ply,
        boardStateKey: board.boardStateKey,
        featureKey: 'ece.marker.hanging_attackable.student',
        label: 'h5: Student hanging and attackable',
        auditId: 'audit-live-12',
        serverAuthorized: true,
        approvedDisplayPayload: true,
      },
      {
        id: 'real-offset',
        gameId: board.gameId,
        ply: board.ply,
        boardStateKey: board.boardStateKey,
        featureKey: 'ece.marker.offset_count.student_win',
        label: 'e5: Offset Count 1',
        auditId: 'audit-live-12',
        serverAuthorized: true,
        approvedDisplayPayload: true,
      },
      {
        id: 'real-threat',
        gameId: board.gameId,
        ply: board.ply,
        boardStateKey: board.boardStateKey,
        featureKey: 'ece.arrow.student_threat',
        label: 'c4-f7: Student threat',
        auditId: 'audit-live-12',
        serverAuthorized: true,
        approvedDisplayPayload: true,
      },
      {
        id: 'real-opponent-threat',
        gameId: board.gameId,
        ply: board.ply,
        boardStateKey: board.boardStateKey,
        featureKey: 'ece.arrow.opponent_threat',
        label: 'd8-h4: Opponent threat',
        auditId: 'audit-live-12',
        serverAuthorized: true,
        approvedDisplayPayload: true,
      },
    ];
    const reveal = potentialMoveReveal(live, 'player', 10, [
      potentialMoveVisual(live, 1, 'f3-e5: Candidate A engine_candidate'),
      potentialMoveVisual(live, 2, 'c4-f7: Candidate B engine_candidate'),
      potentialMoveVisual(live, 3, 'f3-d4: Candidate C engine_candidate'),
    ]);

    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: {
          setLevel: 10,
          toggles: { coachCards: true, boardVisuals: true, appliedLevel: 10 },
        },
      },
    } as RoundData;

    const allItems = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(allItems.indicators.length, 3);
    assert.equal(allItems.highlights.length, 1);
    assert.equal(allItems.arrows.length, 2);
    assert.equal(live.visuals.length, 5);

    setEvenChessLevelFeature(data, 'hangingPieces', false);
    let items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(
      items.indicators.some(indicator => indicator.text === '!' && indicator.position === 'bottom_left'),
      true,
    );
    assert.equal(
      items.indicators.some(indicator => indicator.text === '!' && indicator.colour === '#dc2626'),
      false,
    );
    assert.equal(
      items.indicators.some(indicator => indicator.text === '!' && indicator.colour === '#f97316'),
      true,
    );
    assert.equal(items.highlights.length, 0);
    assert.equal(
      items.indicators.some(indicator => indicator.text === '1'),
      true,
    );

    setEvenChessLevelFeature(data, 'loosePieces', false);
    items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(
      items.indicators.some(indicator => indicator.text === '!'),
      false,
    );
    assert.equal(items.highlights.length, 0);
    assert.equal(
      items.indicators.some(indicator => indicator.text === '1'),
      true,
    );

    setEvenChessLevelFeature(data, 'offsetCount', false);
    items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(items.indicators.length, 0);

    setEvenChessLevelFeature(data, 'studentThreats', false);
    items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(
      items.arrows.some(arrow => arrow.label === '' && arrow.colour === '#22c55e'),
      false,
    );
    assert.equal(
      items.arrows.some(arrow => arrow.label === '' && arrow.colour === '#ef4444'),
      true,
    );

    setEvenChessLevelFeature(data, 'opponentThreats', false);
    items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(
      items.arrows.some(arrow => arrow.label === ''),
      false,
    );

    data.evenchess = {
      ...data.evenchess,
      potentialMoves: {
        status: 'ready',
        activeKey: reveal.key,
        activeKind: 'player',
        active: reveal,
        cache: { [reveal.key]: reveal },
        consumedByKind: { player: 1 },
      },
    };
    items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(
      items.arrows
        .map(arrow => arrow.label)
        .sort()
        .join(','),
      'A,B,C',
    );

    setEvenChessLevelFeature(data, 'candidate1', false);
    setEvenChessLevelFeature(data, 'candidate2', false);
    setEvenChessLevelFeature(data, 'candidate3', false);
    items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(items.arrows.length, 0);
    assert.equal(live.visuals.length, 5);
  });

  test('server-authorized overlays expose one active card and one primary visual', () => {
    const live = overlay();

    assert.equal(overlayStaleReason(live, board), undefined);
    assert.equal(shouldRenderEvenChessOverlay(live, board), true);
    assert.deepEqual(
      renderableEvenChessCards(live, board).map(card => card.id),
      ['card-a'],
    );
    assert.deepEqual(
      renderableEvenChessVisuals(live, board).map(visual => visual.id),
      ['visual-a'],
    );
  });

  test('approved square and arrow visuals become Chessground board shapes', () => {
    const live = overlay();
    live.visuals = [
      live.visuals![0]!,
      {
        ...live.visuals![0]!,
        id: 'visual-b',
        featureKey: 'threat',
        label: 'd1-h5: Student threat',
        primary: false,
      },
      {
        ...live.visuals![0]!,
        id: 'visual-c',
        featureKey: 'ece.eval',
        label: 'Approximate eval +42 cp',
        primary: false,
      },
    ];

    const shapes = renderableEvenChessBoardShapes(live, board);

    assert.equal(shapes.length, 2);
    assert.equal(shapes[0]!.orig, 'f6');
    assert.equal(shapes[0]!.brush, 'red');
    assert.equal(shapes[0]!.label?.text, 'Hanging and attackable');
    assert.equal(shapes[1]!.orig, 'd1');
    assert.equal(shapes[1]!.dest, 'h5');
    assert.equal(shapes[1]!.brush, 'green');
    assert.equal(shapes[1]!.label?.text, 'Student threat');
  });

  test('board-attached overlay renders spec-style arrows, candidate labels, and badges', () => {
    const live = overlay();
    live.visuals = [
      {
        ...live.visuals![0]!,
        id: 'visual-hanging',
        featureKey: 'ece.marker.hanging_attackable.student',
        label: 'f6: Student hanging and attackable',
      },
      {
        ...live.visuals![0]!,
        id: 'visual-opponent-hanging',
        featureKey: 'ece.marker.hanging_attackable.opponent',
        label: 'h5: Opponent hanging and attackable',
      },
      {
        ...live.visuals![0]!,
        id: 'visual-loose',
        featureKey: 'ece.marker.hanging_not_attackable',
        label: 'c4: Hanging',
      },
      {
        ...live.visuals![0]!,
        id: 'visual-offset',
        featureKey: 'ece.marker.offset_count.equal',
        label: 'e4: Offset Count 0',
      },
      {
        ...live.visuals![0]!,
        id: 'visual-pin',
        featureKey: 'ece.marker.pin',
        label: 'f6: Pinned piece',
      },
      {
        ...live.visuals![0]!,
        id: 'visual-threat',
        featureKey: 'ece.arrow.student_threat',
        label: 'd1-h5: Student threat',
      },
      {
        ...live.visuals![0]!,
        id: 'visual-opponent-threat',
        featureKey: 'ece.arrow.opponent_threat',
        label: 'd8-h4: Opponent threat',
      },
    ];
    const reveal = potentialMoveReveal(live, 'player', 10, [
      potentialMoveVisual(live, 1, 'e2-e4: Candidate A'),
    ]);
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: { live },
    } as RoundData;
    applyEvenChessLevelPreset(data, 10);
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
      redraw: () => undefined,
    };

    let items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(items.indicators.length, 5);
    assert.equal(items.highlights.length, 2);
    assert.equal(items.arrows.length, 2);
    assert.equal(items.indicators.find(indicator => indicator.icon === 'shield')?.colour, '#2563eb');
    assert.equal(items.indicators.find(indicator => indicator.icon === 'shield')?.position, 'top_right');
    assert.equal(items.indicators.find(indicator => indicator.icon === 'pin')?.position, 'top_left');
    assert.equal(
      items.indicators.find(indicator => indicator.text === '!' && indicator.colour === '#dc2626')?.position,
      'bottom_left',
    );
    assert.equal(
      items.indicators.find(indicator => indicator.text === '!' && indicator.colour === '#8b5cf6')?.position,
      'bottom_left',
    );
    assert.equal(
      items.indicators.find(indicator => indicator.text === '!' && indicator.colour === '#f97316')?.position,
      'bottom_left',
    );
    assert.equal(items.arrows.find(arrow => arrow.label === 'A'), undefined);
    assert.equal(items.arrows.find(arrow => arrow.lineStyle === 'dotted')?.colour, '#22c55e');
    assert.equal(items.arrows.find(arrow => arrow.colour === '#ef4444')?.lineStyle, 'dotted');

    data.evenchess = {
      ...data.evenchess,
      potentialMoves: {
        status: 'ready',
        activeKey: reveal.key,
        activeKind: 'player',
        active: reveal,
        cache: { [reveal.key]: reveal },
        consumedByKind: { player: 1 },
      },
    };
    items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.equal(items.arrows.find(arrow => arrow.label === 'A')?.lineStyle, 'solid');

    const rendered = renderEvenChessBoardOverlay(ctrl as any);
    const serialized = JSON.stringify(rendered);

    assert.equal((rendered as any).sel, 'div.evenchess-board-overlay');
    assert.match(serialized, /evenchess-board-overlay__highlight/);
    assert.match(serialized, /svg\.evenchess-board-overlay__arrows/);
    assert.match(serialized, /1\.1 1\.6/);
    assert.match(serialized, /evenchess-board-overlay__indicator-icon/);
    assert.match(serialized, /background-color: #dc2626/);
    assert.match(serialized, /background-color: #8b5cf6/);
    assert.match(serialized, /box-shadow: inset 0 0 0 2px #dc2626/);
    assert.match(serialized, /A/);
  });

  test('move clearing marks the overlay as transitioning and retains safe payload until replacement arrives', () => {
    const live = overlay();
    const data = {
      ...roundData(live),
      player: { color: 'white', spectator: false },
    } as RoundData;

    clearEvenChessLiveOverlay(data, 'move-played', 13, 'fen-key-13');

    assert.equal(data.evenchess?.live?.stale, true);
    assert.equal(data.evenchess?.live?.cards?.length, 2);
    assert.equal(data.evenchess?.live?.visuals?.length, live.visuals?.length);
    assert.equal(data.evenchess?.live?.clear?.[0].reason, 'move-played');
    assert.equal(data.evenchess?.coachText?.card.id, 'card-a');
    assert.equal(data.evenchess?.coachText?.card.body, 'Equal trade');
    assert.equal(
      shouldRenderEvenChessOverlay(data.evenchess?.live, { ...board, ply: 13, boardStateKey: 'fen-key-13' }),
      true,
    );

    const ctrl = {
      data,
      ply: 13,
      stepAt: () => ({ fen: 'fen-key-13' }),
      canMove: () => false,
    };
    const transitionBoard = renderEvenChessBoardOverlay(ctrl as any) as any;
    assert.equal(transitionBoard?.data?.attrs?.['data-transition'], 'move-refresh');
    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /Equal trade/);
  });

  test('player-turn payloads refresh the visible coach text snapshot', () => {
    const first = overlay();
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: { live: first },
    } as RoundData;
    applyEvenChessLevelPreset(data, 10);
    const ctrl = coachTextCtrl(data, () => true);

    assert.equal(syncEvenChessCoachTextSnapshot(ctrl as any), true);
    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /Equal trade/);

    const second = overlay();
    second.ply = 14;
    second.boardStateKey = 'fen-key-14';
    second.auditId = 'audit-live-14';
    second.cards = second.cards?.map(card => ({
      ...card,
      id: `${card.id}-14`,
      ply: 14,
      boardStateKey: 'fen-key-14',
      auditId: 'audit-live-14',
      body: card.id === 'card-a' ? 'Fresh player-turn coach text.' : card.body,
    }));
    second.visuals = second.visuals?.map(visual => ({
      ...visual,
      id: `${visual.id}-14`,
      ply: 14,
      boardStateKey: 'fen-key-14',
      auditId: 'audit-live-14',
    }));
    data.evenchess = { ...data.evenchess, live: second };
    ctrl.ply = 14;

    assert.equal(syncEvenChessCoachTextSnapshot(ctrl as any), true);
    const serialized = JSON.stringify(renderEvenChessOverlay(ctrl as any));
    assert.match(serialized, /Fresh player-turn coach text/);
    assert.doesNotMatch(serialized, /Equal trade/);
  });

  test('player-turn payloads use round active color when chessground turn state lags', () => {
    const live = overlay();
    const data = {
      game: { id: board.gameId, player: 'white' },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: { live },
    } as RoundData;
    applyEvenChessLevelPreset(data, 10);
    const ctrl = coachTextCtrl(data, () => false);

    assert.equal(syncEvenChessCoachTextSnapshot(ctrl as any), true);

    const serialized = JSON.stringify(renderEvenChessOverlay(ctrl as any));
    assert.match(serialized, /Equal trade/);
    assert.match(serialized, /audit-live-12/);
  });

  test('opponent-turn payloads update board visuals without replacing visible coach text', () => {
    const first = overlay();
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: { live: first },
    } as RoundData;
    applyEvenChessLevelPreset(data, 10);
    let playerCanMove = true;
    const ctrl = coachTextCtrl(data, () => playerCanMove);

    assert.equal(syncEvenChessCoachTextSnapshot(ctrl as any), true);

    const opponentTurn = overlay();
    opponentTurn.ply = 13;
    opponentTurn.boardStateKey = 'fen-key-13';
    opponentTurn.auditId = 'audit-live-13';
    opponentTurn.cards = opponentTurn.cards?.map(card => ({
      ...card,
      id: `${card.id}-13`,
      ply: 13,
      boardStateKey: 'fen-key-13',
      auditId: 'audit-live-13',
      body: card.id === 'card-a' ? 'Opponent-turn payload text must not replace the snapshot.' : card.body,
    }));
    opponentTurn.visuals = [
      {
        id: 'visual-current-opponent-turn',
        gameId: board.gameId,
        ply: 13,
        boardStateKey: 'fen-key-13',
        featureKey: 'ece.marker.offset_count.equal',
        label: 'e4: Offset Count 0',
        auditId: 'audit-live-13',
        primary: true,
        serverAuthorized: true,
        approvedDisplayPayload: true,
      },
    ];

    data.evenchess = { ...data.evenchess, live: opponentTurn };
    ctrl.ply = 13;
    playerCanMove = false;

    assert.equal(syncEvenChessCoachTextSnapshot(ctrl as any), false);
    const serialized = JSON.stringify(renderEvenChessOverlay(ctrl as any));
    assert.match(serialized, /Equal trade/);
    assert.doesNotMatch(serialized, /Opponent-turn payload text/);

    const currentBoard = { ...board, ply: 13, boardStateKey: 'fen-key-13' };
    const items = renderableEvenChessBoardOverlayItems(data, opponentTurn, currentBoard);
    assert.equal(
      items.indicators.some(indicator => indicator.square === 'e4' && indicator.icon === 'shield'),
      true,
    );
  });

  test('proposed move quota increases by used level', () => {
    assert.equal(proposedMoveQuotaForUsedLevel(4), 0);
    assert.equal(proposedMoveQuotaForUsedLevel(5), 1);
    assert.equal(proposedMoveQuotaForUsedLevel(6), 2);
    assert.equal(proposedMoveQuotaForUsedLevel(7), 2);
    assert.equal(proposedMoveQuotaForUsedLevel(8), 3);
    assert.equal(proposedMoveQuotaForUsedLevel(10), 3);
  });

  test('potential move quotas separate opponent and player reveals', () => {
    assert.equal(potentialMoveQuotaForUsedLevel(4, 'opponent'), 0);
    assert.equal(potentialMoveQuotaForUsedLevel(5, 'opponent'), 1);
    assert.equal(potentialMoveQuotaForUsedLevel(7, 'opponent'), 2);
    assert.equal(potentialMoveQuotaForUsedLevel(8, 'opponent'), 3);
    assert.equal(potentialMoveQuotaForUsedLevel(5, 'player'), 0);
    assert.equal(potentialMoveQuotaForUsedLevel(6, 'player'), 1);
    assert.equal(potentialMoveQuotaForUsedLevel(7, 'player'), 2);
    assert.equal(potentialMoveQuotaForUsedLevel(8, 'player'), 3);
  });

  test('potential move reveal displays the authorized option set instead of gating by A/B/C index', () => {
    const live = overlay();
    const revealAuditId = 'audit-opponent-potential-reveal-12';
    const reveal = potentialMoveReveal(
      live,
      'opponent',
      5,
      [
        potentialMoveVisual(live, 1, 'g8-f6: Candidate A', revealAuditId),
        potentialMoveVisual(live, 2, 'd7-d5: Candidate B', revealAuditId),
        potentialMoveVisual(live, 3, 'c7-c5: Candidate C', revealAuditId),
      ],
      [],
      revealAuditId,
    );
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: { live, display: { setLevel: 10, preferredUsedLevel: 0, usedLevel: 0 } },
    } as RoundData;
    applyEvenChessLevelPreset(data, 5);
    data.evenchess = {
      ...data.evenchess,
      potentialMoves: {
        status: 'ready',
        activeKey: reveal.key,
        activeKind: 'opponent',
        active: reveal,
        cache: { [reveal.key]: reveal },
        consumedByKind: { opponent: 1 },
      },
    };
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
      redraw: () => undefined,
    };

    const items = renderableEvenChessBoardOverlayItems(data, live, board);
    assert.deepEqual(
      items.arrows.filter(arrow => ['A', 'B', 'C'].includes(arrow.label)).map(arrow => arrow.label),
      ['A', 'B', 'C'],
    );
    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /Opponent Potential Moves/);
  });

  test('potential move reveal uses server-authorized cached per-position quota state', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    const live = overlay();
    const revealAuditId = 'audit-potential-reveal-12';
    const reveal = potentialMoveReveal(
      live,
      'player',
      6,
      [
        {
          ...potentialMoveVisual(live, 1, 'g1-f3: Candidate A', revealAuditId),
          id: 'potential-a',
        },
      ],
      [
        {
          id: 'potential-card-a',
          gameId: board.gameId,
          ply: board.ply,
          boardStateKey: board.boardStateKey,
          featureKey: 'ece.candidate.1',
          title: 'Potential A',
          body: 'Develop the knight.',
          level: 6,
          auditId: revealAuditId,
          defaultActive: true,
          serverAuthorized: true,
          approvedDisplayPayload: true,
        },
      ],
      revealAuditId,
    );
    const calls: string[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ potential: reveal }),
      });
    };
    live.visuals = [
      {
        ...live.visuals![0]!,
        id: 'non-potential-live-visual',
      },
    ];
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: { setLevel: 10, usedLevel: 6, toggles: { coachCards: true, boardVisuals: true, appliedLevel: 6 } },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
      redraw: () => undefined,
    };

    try {
      assert.equal(renderableEvenChessBoardOverlayItems(data, live, board).arrows.length, 0);

      requestEvenChessPotentialMoves(ctrl as any, 'player');
      assert.equal(data.evenchess?.potentialMoves?.status, 'loading');
      await new Promise(resolve => setTimeout(resolve, 20));

      assert.equal(calls.length, 1);
      const parsed = new URL(calls[0]!, 'http://localhost:8080');
      assert.equal(parsed.pathname, '/evenchess/testground/ece/potential-move');
      assert.equal(parsed.searchParams.get('kind'), 'player');
      assert.equal(data.evenchess?.potentialMoves?.status, 'ready');
      assert.equal(data.evenchess?.potentialMoves?.consumedByKind?.player, 1);
      assert.equal(renderableEvenChessBoardOverlayItems(data, live, board).arrows[0]?.label, 'A');
      const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));
      assert.match(rendered, /My Potential Moves/);
      assert.match(rendered, /Potential A: Develop the knight/);

      requestEvenChessPotentialMoves(ctrl as any, 'player');
      assert.equal(data.evenchess?.potentialMoves?.status, 'idle');
      assert.equal(data.evenchess?.potentialMoves?.consumedByKind?.player, 1);
      assert.equal(calls.length, 1);
      assert.equal(renderableEvenChessBoardOverlayItems(data, live, board).arrows.length, 0);

      requestEvenChessPotentialMoves(ctrl as any, 'player');
      assert.equal(data.evenchess?.potentialMoves?.status, 'ready');
      assert.equal(data.evenchess?.potentialMoves?.active?.cached, true);
      assert.equal(data.evenchess?.potentialMoves?.consumedByKind?.player, 1);
      assert.equal(calls.length, 1);
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('opponent potential move reveal accepts opponent-side perspective and request kind', async () => {
    const originalLocation = (globalThis as any).location;
    const originalFetch = (globalThis as any).fetch;
    Object.defineProperty(globalThis, 'location', {
      configurable: true,
      value: { origin: 'http://localhost:8080', protocol: 'http:', hostname: 'localhost' },
    });

    const live = overlay();
    const revealAuditId = 'audit-opponent-potential-12';
    const reveal = potentialMoveReveal(
      live,
      'opponent',
      5,
      [{ ...potentialMoveVisual(live, 1, 'e7-e5: Candidate A', revealAuditId), id: 'opponent-potential-a' }],
      [],
      revealAuditId,
      'black',
    );
    const calls: string[] = [];
    (globalThis as any).fetch = (url: string) => {
      calls.push(url);
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ potential: reveal }),
      });
    };

    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      evenchess: {
        live,
        display: { setLevel: 10, usedLevel: 5, toggles: { coachCards: true, boardVisuals: true, appliedLevel: 5 } },
      },
    } as RoundData;
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
      redraw: () => undefined,
    };

    try {
      requestEvenChessPotentialMoves(ctrl as any, 'opponent');
      await new Promise(resolve => setTimeout(resolve, 20));

      assert.equal(calls.length, 1);
      const parsed = new URL(calls[0]!, 'http://localhost:8080');
      assert.equal(parsed.pathname, '/evenchess/testground/ece/potential-move');
      assert.equal(parsed.searchParams.get('kind'), 'opponent');
      assert.equal(data.evenchess?.potentialMoves?.status, 'ready');
      assert.equal(data.evenchess?.potentialMoves?.active?.kind, 'opponent');
      assert.equal(data.evenchess?.potentialMoves?.active?.perspective, 'black');
      assert.equal(renderableEvenChessBoardOverlayItems(data, live, board).arrows[0]?.from, 'e7');
      assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /Opponent Potential Moves/);
    } finally {
      (globalThis as any).fetch = originalFetch;
      if (originalLocation === undefined) delete (globalThis as any).location;
      else Object.defineProperty(globalThis, 'location', { configurable: true, value: originalLocation });
    }
  });

  test('proposed move selection requires exactly one legal green arrow', () => {
    const ctrl = proposedMoveCtrl([{ orig: 'g1', dest: 'f3', brush: 'green' }]);

    const selection = readEvenChessProposedMoveSelection(ctrl as any);
    assert.equal(selection.kind, 'move');
    if (selection.kind === 'move') {
      assert.equal(selection.moveUci, 'g1f3');
      assert.match(selection.key, /g1f3/);
    }

    assert.equal(readEvenChessProposedMoveSelection(proposedMoveCtrl([]) as any).kind, 'error');
    assert.equal(
      readEvenChessProposedMoveSelection(
        proposedMoveCtrl([
          { orig: 'g1', dest: 'f3', brush: 'green' },
          { orig: 'b1', dest: 'c3', brush: 'green' },
        ]) as any,
      ).kind,
      'error',
    );
    assert.equal(
      (
        readEvenChessProposedMoveSelection(
          proposedMoveCtrl([{ orig: 'g1', dest: 'h3', brush: 'green' }]) as any,
        ) as any
      ).code,
      'illegal',
    );
  });

  test('proposed move preview is visible only while the matching arrow remains', () => {
    const ctrl = proposedMoveCtrl([{ orig: 'g1', dest: 'f3', brush: 'green' }]);
    const selection = readEvenChessProposedMoveSelection(ctrl as any);
    assert.equal(selection.kind, 'move');
    if (selection.kind !== 'move') return;

    ctrl.data.evenchess = {
      ...ctrl.data.evenchess,
      proposedMove: {
        status: 'ready',
        activeKey: selection.key,
        active: proposedMoveCard(selection.key),
        cache: { [selection.key]: proposedMoveCard(selection.key) },
        consumedByTurn: { [selection.turnKey]: selection.key },
      },
    };

    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /Proposed Move g1f3/);

    ctrl.chessground.state.drawable.shapes = [];
    assert.equal(syncEvenChessProposedMovePreview(ctrl as any), true);
    assert.equal(ctrl.data.evenchess.proposedMove?.status, 'idle');
    assert.doesNotMatch(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /Proposed Move g1f3/);
  });

  test('proposed move button toggles an active cached preview back to board-state display', () => {
    const ctrl = proposedMoveCtrl([{ orig: 'g1', dest: 'f3', brush: 'green' }]);
    const selection = readEvenChessProposedMoveSelection(ctrl as any);
    assert.equal(selection.kind, 'move');
    if (selection.kind !== 'move') return;
    const live = overlay();
    const card = {
      ...proposedMoveCard(selection.key),
      postMoveBoardStateKey: 'fen-after-g1f3',
      cards: [
        {
          id: 'proposed-after-summary',
          gameId: board.gameId,
          ply: board.ply,
          boardStateKey: 'fen-after-g1f3',
          featureKey: 'ece.card.summarycard',
          title: 'After Move Summary',
          body: 'This is the proposed preview payload.',
          level: 5,
          auditId: 'audit-proposed-after',
          defaultActive: true,
          serverAuthorized: true,
          approvedDisplayPayload: true,
        },
      ],
      visuals: [],
    };

    ctrl.data.evenchess = {
      ...ctrl.data.evenchess,
      proposedMove: {
        status: 'ready',
        activeKey: selection.key,
        active: card,
        baseOverlay: live,
        cache: { [selection.key]: card },
        consumedByTurn: { [selection.turnKey]: selection.key },
      },
    };
    applyEvenChessLevelPreset(ctrl.data, 10);

    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /This is the proposed preview payload/);

    requestEvenChessProposedMovePreview(ctrl as any);
    assert.equal(ctrl.data.evenchess.proposedMove?.status, 'idle');
    assert.equal(ctrl.data.evenchess.proposedMove?.active, undefined);
    let rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));
    assert.doesNotMatch(rendered, /This is the proposed preview payload/);
    assert.match(rendered, /Offset Count/);
    assert.match(rendered, /Equal trade/);

    requestEvenChessProposedMovePreview(ctrl as any);
    assert.equal(ctrl.data.evenchess.proposedMove?.status, 'ready');
    assert.equal(ctrl.data.evenchess.proposedMove?.active?.cached, true);
    rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));
    assert.match(rendered, /This is the proposed preview payload/);
  });

  test('illegal proposed move click preserves the current proposed overlay state', () => {
    const ctrl = proposedMoveCtrl([{ orig: 'g1', dest: 'f3', brush: 'green' }]);
    const selection = readEvenChessProposedMoveSelection(ctrl as any);
    assert.equal(selection.kind, 'move');
    if (selection.kind !== 'move') return;

    const card = {
      ...proposedMoveCard(selection.key),
      postMoveBoardStateKey: 'fen-after-g1f3',
      cards: [
        {
          id: 'proposed-after-summary',
          gameId: board.gameId,
          ply: board.ply,
          boardStateKey: 'fen-after-g1f3',
          featureKey: 'ece.card.summarycard',
          title: 'After Move Summary',
          body: 'This is the active legal preview.',
          level: 5,
          auditId: 'audit-proposed',
          defaultActive: true,
          serverAuthorized: true,
          approvedDisplayPayload: true,
        },
      ],
      visuals: [],
    };
    ctrl.data.evenchess = {
      ...ctrl.data.evenchess,
      proposedMove: {
        status: 'ready',
        activeKey: selection.key,
        active: card,
        cache: { [selection.key]: card },
        consumedByTurn: { [selection.turnKey]: selection.key },
      },
    };
    applyEvenChessLevelPreset(ctrl.data, 10);

    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /This is the active legal preview/);

    ctrl.chessground.state.drawable.shapes = [{ orig: 'g1', dest: 'h3', brush: 'green' }];
    assert.equal(syncEvenChessProposedMovePreview(ctrl as any), false);

    requestEvenChessProposedMovePreview(ctrl as any);

    assert.equal(ctrl.data.evenchess.proposedMove?.status, 'error');
    assert.equal(ctrl.data.evenchess.proposedMove?.activeKey, selection.key);
    assert.equal(ctrl.data.evenchess.proposedMove?.active?.key, selection.key);
    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /This is the active legal preview/);
  });

  test('proposed move preview displays cached post-move ECE cards and visuals while active', () => {
    const ctrl = proposedMoveCtrl([{ orig: 'g1', dest: 'f3', brush: 'green' }]);
    const selection = readEvenChessProposedMoveSelection(ctrl as any);
    assert.equal(selection.kind, 'move');
    if (selection.kind !== 'move') return;

    const card = {
      ...proposedMoveCard(selection.key),
      postMoveBoardStateKey: 'fen-after-g1f3',
      cards: [
        {
          id: 'proposed-after-summary',
          gameId: board.gameId,
          ply: board.ply,
          boardStateKey: 'fen-after-g1f3',
          featureKey: 'ece.card.summarycard',
          title: 'After Move Summary',
          body: 'Your knight would increase pressure after this move.',
          level: 5,
          auditId: 'audit-proposed-after',
          defaultActive: true,
          serverAuthorized: true,
          approvedDisplayPayload: true,
        },
      ],
      visuals: [
        {
          id: 'proposed-after-offset',
          gameId: board.gameId,
          ply: board.ply,
          boardStateKey: 'fen-after-g1f3',
          featureKey: 'ece.marker.offset_count.equal',
          label: 'e4: Offset Count 0',
          auditId: 'audit-proposed-after',
          primary: true,
          serverAuthorized: true,
          approvedDisplayPayload: true,
        },
      ],
    };
    ctrl.data.evenchess = {
      ...ctrl.data.evenchess,
      proposedMove: {
        status: 'ready',
        activeKey: selection.key,
        active: card,
        cache: { [selection.key]: card },
        consumedByTurn: { [selection.turnKey]: selection.key },
      },
    };

    applyEvenChessLevelPreset(ctrl.data, 10);

    const renderedCoach = JSON.stringify(renderEvenChessOverlay(ctrl as any));
    assert.match(renderedCoach, /After Move Summary/);
    assert.match(renderedCoach, /Your knight would increase pressure/);
    assert.match(renderedCoach, /Proposed Move g1f3/);

    const renderedBoard = JSON.stringify(renderEvenChessBoardOverlay(ctrl as any));
    assert.match(renderedBoard, /evenchess-board-overlay__indicator-icon/);
    assert.match(renderedBoard, /Even trade/);

    requestEvenChessProposedMovePreview(ctrl as any);

    assert.equal(ctrl.data.evenchess.proposedMove?.status, 'idle');
    assert.doesNotMatch(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /After Move Summary/);
  });

  test('proposed move preview switches to cached post-move eval and toggles back to live eval', () => {
    const ctrl = proposedMoveCtrl([{ orig: 'g1', dest: 'f3', brush: 'green' }]);
    const selection = readEvenChessProposedMoveSelection(ctrl as any);
    assert.equal(selection.kind, 'move');
    if (selection.kind !== 'move') return;

    const live = overlay();
    live.visuals.push({
      id: 'visual-live-eval',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'ece.eval.deep',
      label: 'Stockfish eval +184 cp',
      auditId: live.auditId,
      primary: false,
      serverAuthorized: true,
      approvedDisplayPayload: true,
    });
    const card = {
      ...proposedMoveCard(selection.key),
      postMoveBoardStateKey: 'fen-after-g1f3',
      cards: [],
      visuals: [
        {
          id: 'proposed-after-eval',
          gameId: board.gameId,
          ply: board.ply,
          boardStateKey: 'fen-after-g1f3',
          featureKey: 'ece.eval.deep',
          label: 'Stockfish eval -72 cp',
          auditId: 'audit-proposed-after',
          primary: false,
          serverAuthorized: true,
          approvedDisplayPayload: true,
        },
      ],
    };
    ctrl.data.evenchess = {
      ...ctrl.data.evenchess,
      live,
      proposedMove: {
        status: 'ready',
        activeKey: selection.key,
        active: card,
        cache: { [selection.key]: card },
        consumedByTurn: { [selection.turnKey]: selection.key },
      },
    };

    applyEvenChessLevelPreset(ctrl.data, 10);
    let rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));
    assert.match(rendered, /-0\.72/);
    assert.match(rendered, /Slightly worse/);

    requestEvenChessProposedMovePreview(ctrl as any);
    rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.match(rendered, /\+1\.84/);
    assert.match(rendered, /Better/);
    assert.doesNotMatch(rendered, /-0\.72/);
  });

  test('proposed move preview without eval retains the live eval display', () => {
    const ctrl = proposedMoveCtrl([{ orig: 'g1', dest: 'f3', brush: 'green' }]);
    const selection = readEvenChessProposedMoveSelection(ctrl as any);
    assert.equal(selection.kind, 'move');
    if (selection.kind !== 'move') return;

    const live = overlay();
    live.visuals.push({
      id: 'visual-live-eval',
      gameId: board.gameId,
      ply: board.ply,
      boardStateKey: board.boardStateKey,
      featureKey: 'ece.eval.deep',
      label: 'Stockfish eval +184 cp',
      auditId: live.auditId,
      primary: false,
      serverAuthorized: true,
      approvedDisplayPayload: true,
    });
    const card = {
      ...proposedMoveCard(selection.key),
      postMoveBoardStateKey: 'fen-after-g1f3',
      cards: [],
      visuals: [],
    };
    ctrl.data.evenchess = {
      ...ctrl.data.evenchess,
      live,
      proposedMove: {
        status: 'ready',
        activeKey: selection.key,
        active: card,
        cache: { [selection.key]: card },
        consumedByTurn: { [selection.turnKey]: selection.key },
      },
    };

    applyEvenChessLevelPreset(ctrl.data, 10);
    assert.match(JSON.stringify(renderEvenChessOverlay(ctrl as any)), /\+1\.84/);
  });

  test('proposed move button does not duplicate an in-flight same-arrow request', () => {
    const ctrl = proposedMoveCtrl([{ orig: 'g1', dest: 'f3', brush: 'green' }]);
    const selection = readEvenChessProposedMoveSelection(ctrl as any);
    assert.equal(selection.kind, 'move');
    if (selection.kind !== 'move') return;

    ctrl.data.evenchess = {
      ...ctrl.data.evenchess,
      proposedMove: {
        status: 'loading',
        message: 'Checking',
        activeKey: selection.key,
        updatedAt: 100,
      },
    };

    requestEvenChessProposedMovePreview(ctrl as any);

    assert.equal(ctrl.data.evenchess.proposedMove?.status, 'loading');
    assert.equal(ctrl.data.evenchess.proposedMove?.activeKey, selection.key);
    assert.equal(ctrl.data.evenchess.proposedMove?.updatedAt, 100);
  });

  test('used level is retained after a rendered payload raises it', () => {
    const data = roundData();
    data.evenchess = { display: { setLevel: 10, preferredUsedLevel: 0, usedLevel: 0 } };
    const live = overlay();
    live.cards![0].level = 6;

    applyEvenChessLiveOverlay(data, live);
    clearEvenChessLiveOverlay(data, 'move-played', 13, 'fen-key-13');

    assert.equal(data.evenchess?.display?.usedLevel, 6);
    assert.equal(data.evenchess?.live?.stale, true);
  });

  test('live overlay assistance usage hydrates server-side consumable counts after refresh', () => {
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
    } as RoundData;
    const live = {
      ...overlay(),
      assistance: {
        proposedMove: { consumed: 2, quota: 3 },
        potentialMoves: {
          consumedByKind: { player: 1, opponent: 2 },
          quotaByKind: { player: 3, opponent: 3 },
        },
      },
    };
    const ctrl = {
      data,
      ply: board.ply,
      stepAt: () => ({ fen: board.boardStateKey }),
      canMove: () => true,
      flip: false,
      redraw: () => undefined,
      chessground: {
        state: {
          drawable: { shapes: [] },
          movable: { dests: new Map<Key, Key[]>() },
          pieces: new Map<Key, Piece>(),
        },
      },
    };

    applyEvenChessLiveOverlay(data, live);
    applyEvenChessLevelPreset(data, 10);

    assert.equal(data.evenchess?.proposedMove?.status, 'idle');
    assert.equal(data.evenchess?.proposedMove?.consumed, 2);
    assert.equal(data.evenchess?.proposedMove?.quota, 3);
    assert.equal(data.evenchess?.potentialMoves?.status, 'idle');
    assert.equal(data.evenchess?.potentialMoves?.consumedByKind?.player, 1);
    assert.equal(data.evenchess?.potentialMoves?.consumedByKind?.opponent, 2);
    const rendered = JSON.stringify(renderEvenChessOverlay(ctrl as any));
    assert.match(rendered, /Used 2\/3/);
    assert.match(rendered, /Used 1\/3/);
  });

  test('raw engine and hidden debug payloads are suppressed client-side', () => {
    const live = overlay();
    live.cards![0].rawStockfishLine = 'pv e2e4 e7e5';
    live.visuals![0].hiddenDebugData = 'multipv';
    const data = roundData();

    applyEvenChessLiveOverlay(data, live);

    assert.equal(payloadHasUnsafeDisplayData(live), true);
    assert.equal(data.evenchess?.live?.stale, true);
    assert.equal(data.evenchess?.live?.clear?.[0].reason, 'unsafe-payload');
    assert.equal(shouldRenderEvenChessOverlay(data.evenchess?.live, board), false);
  });

  test('live card TTS can read only the authorized shown card text', () => {
    const live = overlay();
    const card = renderableEvenChessCards(live, board)[0]!;
    const item = liveCardTtsItem(card, live, true);

    assert.equal(ttsSafetyReason(ttsConfig, item), undefined);
    assert.equal(item.displayedText, 'Offset Count Equal trade');
    assert.equal(item.auditId, live.auditId);

    card.ttsText = 'Play the hidden engine line instead.';
    assert.equal(ttsSafetyReason(ttsConfig, liveCardTtsItem(card, live, true)), 'text-mismatch');
  });

  test('live card TTS respects opponent-turn mute preference', () => {
    const live = overlay();
    const card = renderableEvenChessCards(live, board)[0]!;

    assert.equal(ttsSafetyReason(ttsConfig, liveCardTtsItem(card, live, false)), 'muted-opponent-turn');
  });

  test('round TTS config falls back to EvenChess account preferences', () => {
    const data = roundData();
    data.pref = {
      evenchess: {
        ttsEnabled: true,
        ttsAutoSpeak: true,
        ttsAutoDelaySeconds: 5,
        ttsVoice: 'warm',
        ttsRatePercent: 120,
        ttsVolumePercent: 50,
        ttsQueueBehavior: 'queue',
        ttsMuteDuringOpponentTurn: false,
      },
    } as any;

    const config = evenChessTtsConfigForData(data)!;

    assert.equal(config.enabled, true);
    assert.equal(config.autoSpeak, true);
    assert.equal(config.autoDelaySeconds, 5);
    assert.equal(config.voice, 'warm');
    assert.equal(config.ratePercent, 120);
    assert.equal(config.volumePercent, 50);
    assert.equal(config.queueBehavior, 'queue');
    assert.equal(config.muteDuringOpponentTurn, false);
    assert.equal(config.serverAuthorized, true);
    assert.equal(evenChessTtsAutoDelayMillis(config), 5000);
    assert.equal(evenChessTtsAutoDelayMillis({ enabled: true, autoDelaySeconds: 50 }), 30000);
    assert.equal(evenChessTtsAutoDelayMillis({ enabled: true, autoDelaySeconds: -5 }), 0);
  });

  test('coach card renders a discoverable Speak button even when TTS is disabled', () => {
    const live = overlay();
    const data = {
      game: { id: board.gameId },
      player: { color: 'white', spectator: false },
      opponent: { color: 'black' },
      pref: {
        evenchess: {
          ttsEnabled: false,
          ttsAutoSpeak: false,
          ttsAutoDelaySeconds: 1,
          ttsVoice: 'system-default',
          ttsRatePercent: 100,
          ttsVolumePercent: 80,
          ttsQueueBehavior: 'replace-current',
          ttsMuteDuringOpponentTurn: true,
        },
      },
      evenchess: { live },
    } as RoundData;
    applyEvenChessLevelPreset(data, 10);
    const ctrl = coachTextCtrl(data, () => true);

    const serialized = JSON.stringify(renderEvenChessOverlay(ctrl as any));

    assert.match(serialized, /Speak/);
    assert.match(serialized, /Enable TTS Coach in EvenChess settings/);
  });

  test('board, ply, expiry, and authorization mismatches do not render', () => {
    assert.equal(overlayStaleReason({ ...overlay(), serverAuthorized: false }, board), 'unauthorized');
    assert.equal(overlayStaleReason({ ...overlay(), ply: 11 }, board), 'ply-mismatch');
    assert.equal(overlayStaleReason({ ...overlay(), boardStateKey: 'old-key' }, board), 'board-mismatch');
    assert.equal(overlayStaleReason({ ...overlay(), expiresAt: 999 }, board), 'expired');
  });

  test('overlay for a different game is marked stale and cleared on receipt', () => {
    const live = { ...overlay(), gameId: 'other-game' };
    const data = roundData(overlay());

    applyEvenChessLiveOverlay(data, live);

    assert.equal(data.evenchess?.live?.stale, true);
    assert.equal(data.evenchess?.live?.clear?.[0].reason, 'game-mismatch');
    assert.equal(data.evenchess?.live?.cards?.length, 0);
    assert.equal(data.evenchess?.live?.visuals?.length, 0);
    assert.equal(shouldRenderEvenChessOverlay(data.evenchess?.live, board), false);
  });
});
