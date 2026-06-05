import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import type { EvenChessTtsConfig } from 'lib/evenchessTts';
import { ttsSafetyReason } from 'lib/evenchessTts';

import type { EvenChessLearningPayload } from '../src/interfaces';
import {
  learningCardTtsItem,
  learningStaleReason,
  payloadHasInventedLearningFacts,
  payloadHasUnsafeLearningData,
  renderableLearningCards,
  shouldRenderLearningOverlay,
} from '../src/view/evenchessLearning';

const snapshot = {
  surface: 'analysis' as const,
  contextId: 'analysis-game',
  boardStateKey: 'fen-key-18',
  ply: 18,
  now: 1000,
};

const payload = (): EvenChessLearningPayload => ({
  enabled: true,
  surface: snapshot.surface,
  contextId: snapshot.contextId,
  boardStateKey: snapshot.boardStateKey,
  ply: snapshot.ply,
  serverAuthorized: true,
  auditId: 'audit-learning-18',
  expiresAt: 10000,
  sourceFacts: [
    {
      factId: 'fact-1',
      boardStateKey: snapshot.boardStateKey,
      auditTag: 'audit-1',
    },
  ],
  cards: [
    {
      id: 'card-1',
      kind: 'positionExplanation',
      title: 'Position explanation',
      body: 'The loose piece matters in this position.',
      bullets: ['Improve coordination first.'],
      sourceFactIds: ['fact-1'],
      auditId: 'audit-learning-18',
      serverAuthorized: true,
      approvedDisplayPayload: true,
    },
    {
      id: 'card-2',
      kind: 'explainMove',
      title: 'Explain this move',
      body: 'The move reduced pressure on the back rank.',
      sourceFactIds: ['fact-1'],
      auditId: 'audit-learning-18',
      serverAuthorized: true,
      approvedDisplayPayload: true,
    },
  ],
});

const ttsConfig: EvenChessTtsConfig = {
  enabled: true,
  provider: 'browser-speech',
  serverAuthorized: true,
  policyVersion: 'tts-v1',
  muteDuringOpponentTurn: true,
};

describe('EvenChess analysis and study learning overlay adapter', () => {
  test('normal analysis data without EvenChess learning payloads does not render', () => {
    assert.equal(learningStaleReason(undefined, snapshot), 'not-enabled');
    assert.equal(shouldRenderLearningOverlay(undefined, snapshot), false);
  });

  test('server-authorized analysis payload renders approved cards only', () => {
    const learning = payload();

    assert.equal(learningStaleReason(learning, snapshot), undefined);
    assert.equal(shouldRenderLearningOverlay(learning, snapshot), true);
    assert.deepEqual(
      renderableLearningCards(learning, snapshot).map(card => card.id),
      ['card-1', 'card-2'],
    );
  });

  test('study payloads render only on the study surface', () => {
    const learning = { ...payload(), surface: 'study' as const, contextId: 'study-1' };
    const studySnapshot = { ...snapshot, surface: 'study' as const, contextId: 'study-1' };

    assert.equal(learningStaleReason(learning, snapshot), 'surface-mismatch');
    assert.equal(shouldRenderLearningOverlay(learning, studySnapshot), true);
  });

  test('raw engine debug secret prompt payloads are suppressed client-side', () => {
    const learning = payload();
    learning.cards![0].rawEnginePayload = 'pv e2e4 e7e5';
    learning.cards![1].providerSecret = 'sk-secret';

    assert.equal(payloadHasUnsafeLearningData(learning), true);
    assert.equal(learningStaleReason(learning, snapshot), 'unsafe-payload');
    assert.equal(shouldRenderLearningOverlay(learning, snapshot), false);
  });

  test('live rated unauthorized board and ply mismatches do not render', () => {
    assert.equal(learningStaleReason({ ...payload(), ratedLive: true }, snapshot), 'live-rated');
    assert.equal(learningStaleReason({ ...payload(), serverAuthorized: false }, snapshot), 'unauthorized');
    assert.equal(learningStaleReason({ ...payload(), boardStateKey: 'old-fen' }, snapshot), 'board-mismatch');
    assert.equal(learningStaleReason({ ...payload(), ply: 17 }, snapshot), 'ply-mismatch');
  });

  test('invented source facts suppress the whole learning payload', () => {
    const learning = payload();
    learning.cards![0].sourceFactIds = ['invented'];

    assert.equal(payloadHasInventedLearningFacts(learning), true);
    assert.equal(learningStaleReason(learning, snapshot), 'invented-source-fact');
    assert.equal(shouldRenderLearningOverlay(learning, snapshot), false);
    assert.deepEqual(renderableLearningCards(learning, snapshot), []);
  });

  test('learning card TTS can read only the approved visible explanation text', () => {
    const learning = payload();
    const card = renderableLearningCards(learning, snapshot)[0]!;
    const item = learningCardTtsItem(card, learning);

    assert.equal(ttsSafetyReason(ttsConfig, item), undefined);
    assert.equal(
      item.displayedText,
      'Position explanation The loose piece matters in this position. Improve coordination first.',
    );

    card.ttsText = 'A hidden stronger line that was not displayed.';
    assert.equal(ttsSafetyReason(ttsConfig, learningCardTtsItem(card, learning)), 'text-mismatch');
  });
});
