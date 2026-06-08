import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import {
  type EvenChessUniversalDisplayState,
  evenChessUniversalPanelCards,
  evenChessUniversalOverlayItems,
  evenChessUniversalOverlayUrl,
} from '../src/evenchessUniversalOverlay';

const visual = (featureKey: string, label: string, id = featureKey) => ({
  id,
  gameId: 'analysis-1',
  ply: 4,
  boardStateKey: '8/8/8/8/8/8/8/8 w - - 0 1',
  featureKey,
  label,
  auditId: 'audit-1',
  serverAuthorized: true,
  approvedDisplayPayload: true,
});

const live = (visuals = [visual('ece.marker.offset_count.equal', 'd4: Offset Count 0')]) => ({
  enabled: true,
  gameId: 'analysis-1',
  ply: 4,
  boardStateKey: '8/8/8/8/8/8/8/8 w - - 0 1',
  perspective: 'white' as Color,
  auditId: 'audit-1',
  serverAuthorized: true,
  ttlMillis: 60_000,
  visuals,
});

const card = (featureKey: string, title: string, body: string, id = featureKey) => ({
  id,
  gameId: 'analysis-1',
  ply: 4,
  boardStateKey: '8/8/8/8/8/8/8/8 w - - 0 1',
  featureKey,
  title,
  body,
  level: 10,
  auditId: 'audit-1',
  serverAuthorized: true,
  approvedDisplayPayload: true,
});

const displayState = (
  enabled: Partial<EvenChessUniversalDisplayState['levelFeatures']>,
): EvenChessUniversalDisplayState => ({
  setLevel: 10,
  usedLevel: 10,
  appliedLevel: 10,
  levelFeatures: {
    rules: true,
    loosePieces: true,
    hangingPieces: true,
    offsetCount: true,
    studentThreats: true,
    opponentThreats: true,
    pins: true,
    coachText: true,
    candidate1: true,
    candidate2: true,
    openingWiki: true,
    candidate3: true,
    evalBar: true,
    evalNumbers: true,
    humanRisk: true,
    expertLines: true,
    fullSpecificity: true,
    ...enabled,
  },
});

describe('EvenChess universal board overlay', () => {
  test('builds a same-origin overlay request without client-supplied level', () => {
    const url = evenChessUniversalOverlayUrl({
      surface: 'analysis',
      gameId: 'analysis-1',
      fen: '8/8/8/8/8/8/8/8 w - - 0 1',
      ply: 4,
      side: 'white',
    });

    assert.equal(url.startsWith('/evenchess/ece/board-overlay?'), true);
    assert.equal(url.includes('level='), false);
    assert.equal(url.includes('fen=8%2F8%2F8%2F8%2F8%2F8%2F8%2F8+w+-+-+0+1'), true);
  });

  test('renders threat arrows, pin markers, hanging markers, and offset shields from approved payloads', () => {
    const items = evenChessUniversalOverlayItems(
      live([
        visual('ece.arrow.student_threat', 'g1-f3: Student threat'),
        visual('ece.arrow.opponent_threat', 'c8-h3: Opponent threat'),
        visual('ece.marker.pin', 'd3: Pinned piece'),
        visual('ece.marker.hanging_attackable.student', 'a1: Student hanging attackable'),
        visual('ece.marker.hanging_attackable.opponent', 'h1: Opponent hanging attackable'),
        visual('ece.marker.hanging_not_attackable', 'b2: Loose unprotected piece'),
        visual('ece.marker.offset_count.equal', 'd4: Offset Count 0'),
        visual('ece.marker.offset_count.student_win', 'e5: Offset Count +2'),
        visual('ece.marker.offset_count.opponent_win', 'f6: Offset Count -1'),
      ]),
      '8/8/8/8/8/8/8/8 w - - 0 1',
      4,
    );

    assert.equal(items.arrows.length, 2);
    assert.equal(items.indicators.length, 7);
    assert.equal(items.highlights.length, 2);
    assert.equal(items.indicators.some(item => item.square === 'd4' && item.icon === 'shield'), true);
    assert.equal(items.indicators.some(item => item.square === 'd3' && item.icon === 'pin'), true);
    assert.equal(items.indicators.some(item => item.square === 'a1' && item.text === '!'), true);
    assert.equal(items.indicators.some(item => item.square === 'h1' && item.text === '!'), true);
  });

  test('rejects stale or mismatched board-state payloads', () => {
    assert.deepEqual(
      evenChessUniversalOverlayItems(live(), '8/8/8/8/8/8/8/8 b - - 0 1', 4),
      { arrows: [], highlights: [], indicators: [] },
    );
    assert.deepEqual(evenChessUniversalOverlayItems({ ...live(), stale: true }, live().boardStateKey, 4), {
      arrows: [],
      highlights: [],
      indicators: [],
    });
  });

  test('selects safe coach cards for non-live puzzle panels', () => {
    const payload = {
      ...live(),
      cards: [
        card('ece.card.plan', 'Plan', 'Improve the weakest piece.'),
        card('ece.card.summary', 'Summary', 'The position is stable.'),
        { ...card('ece.card.warning', 'Warning', 'Check the back rank.'), stale: true },
        { ...card('ece.card.raw', 'Raw', 'Hidden.'), rawStockfishLine: 'pv e2e4' },
      ],
    };

    const cards = evenChessUniversalPanelCards(payload, payload.boardStateKey, 4);

    assert.equal(cards.length, 2);
    assert.equal(cards[0].title, 'Summary');
    assert.equal(cards[1].title, 'Plan');
  });

  test('filters puzzle board overlays with level feature toggles', () => {
    const payload = live([
      visual('ece.arrow.student_threat', 'g1-f3: Student threat'),
      visual('ece.marker.offset_count.equal', 'd4: Offset Count 0'),
      visual('ece.marker.hanging_attackable.student', 'a1: Student hanging attackable'),
    ]);

    const items = evenChessUniversalOverlayItems(
      payload,
      payload.boardStateKey,
      4,
      displayState({ studentThreats: false, offsetCount: false }),
    );

    assert.equal(items.arrows.length, 0);
    assert.equal(items.indicators.some(item => item.square === 'd4'), false);
    assert.equal(items.indicators.some(item => item.square === 'a1'), true);
  });

  test('filters puzzle coach cards with level feature toggles', () => {
    const payload = {
      ...live(),
      cards: [
        card('ece.card.summary', 'Summary', 'The position is stable.'),
        card('ece.card.eval', 'Eval', '+0.8'),
      ],
    };

    const cards = evenChessUniversalPanelCards(
      payload,
      payload.boardStateKey,
      4,
      3,
      displayState({ coachText: false }),
    );

    assert.equal(cards.length, 1);
    assert.equal(cards[0].title, 'Eval');
  });
});
