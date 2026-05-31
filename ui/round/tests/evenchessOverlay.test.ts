import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import type { EvenChessTtsConfig } from 'lib/evenchessTts';
import { ttsSafetyReason } from 'lib/evenchessTts';

import type { EvenChessLiveOverlay, RoundData } from '../src/interfaces';
import {
  applyEvenChessLiveOverlay,
  clearEvenChessLiveOverlay,
  liveCardTtsItem,
  overlayStaleReason,
  payloadHasUnsafeDisplayData,
  renderableEvenChessCards,
  renderableEvenChessVisuals,
  shouldRenderEvenChessOverlay,
} from '../src/view/evenchessOverlay';

const board = {
  gameId: 'live-game',
  ply: 12,
  boardStateKey: 'fen-key-12',
  now: 1000,
};

const overlay = (): EvenChessLiveOverlay => ({
  enabled: true,
  gameId: board.gameId,
  ply: board.ply,
  boardStateKey: board.boardStateKey,
  perspective: 'white',
  auditId: 'audit-live-12',
  serverAuthorized: true,
  ttlMillis: 5000,
  expiresAt: 10000,
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
      featureKey: 'offset_count',
      label: 'Exchange marker',
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
    evenchess: live ? { live } : undefined,
  }) as RoundData;

const ttsConfig: EvenChessTtsConfig = {
  enabled: true,
  provider: 'browser-speech',
  serverAuthorized: true,
  policyVersion: 'tts-v1',
  muteDuringOpponentTurn: true,
};

describe('EvenChess live round overlay adapter', () => {
  test('normal games without EvenChess payloads do not render overlays', () => {
    assert.equal(overlayStaleReason(undefined, board), 'not-enabled');
    assert.equal(shouldRenderEvenChessOverlay(undefined, board), false);
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

  test('move clearing marks the overlay stale and removes cards and visuals', () => {
    const data = roundData(overlay());

    clearEvenChessLiveOverlay(data, 'move-played', 13, 'fen-key-13');

    assert.equal(data.evenchess?.live?.stale, true);
    assert.deepEqual(data.evenchess?.live?.cards, []);
    assert.deepEqual(data.evenchess?.live?.visuals, []);
    assert.equal(data.evenchess?.live?.clear?.[0].reason, 'move-played');
    assert.equal(
      shouldRenderEvenChessOverlay(data.evenchess?.live, { ...board, ply: 13, boardStateKey: 'fen-key-13' }),
      false,
    );
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
