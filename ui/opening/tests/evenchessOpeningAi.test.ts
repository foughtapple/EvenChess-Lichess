import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import type { EvenChessTtsConfig } from 'lib/evenchessTts';
import { ttsSafetyReason } from 'lib/evenchessTts';

import {
  openingAiStaleReason,
  openingCardTtsItem,
  openingPayloadHasInventedFacts,
  openingPayloadHasUnsafeAiData,
  renderEvenChessOpeningAi,
  renderableOpeningAiCards,
  shouldRenderOpeningAi,
} from '../src/evenchessOpeningAi';
import type { EvenChessOpeningAiPayload, OpeningPage } from '../src/interfaces';

const payload = (): EvenChessOpeningAiPayload => ({
  enabled: true,
  surface: 'opening',
  contextId: 'sicilian-defense',
  boardStateKey: 'opening-key-12',
  ply: 12,
  serverAuthorized: true,
  auditId: 'audit-opening-12',
  expiresAt: 10000,
  sourceFacts: [
    {
      factId: 'fact-opening',
      boardStateKey: 'opening-key-12',
      auditTag: 'audit-opening',
    },
  ],
  cards: [
    {
      id: 'opening-card',
      kind: 'openingPlan',
      title: 'Opening plan',
      body: 'The plan is based on the existing opening explorer context.',
      bullets: ['Fight for the center before expanding.'],
      sourceFactIds: ['fact-opening'],
      auditId: 'audit-opening-12',
      serverAuthorized: true,
      approvedDisplayPayload: true,
    },
  ],
});

const page = (openingAi?: EvenChessOpeningAiPayload): OpeningPage => ({
  history: [],
  sans: [],
  evenchess: openingAi ? { openingAi } : undefined,
});

const ttsConfig: EvenChessTtsConfig = {
  enabled: true,
  provider: 'browser-speech',
  serverAuthorized: true,
  policyVersion: 'tts-v1',
};

const pageWithTts = (openingAi?: EvenChessOpeningAiPayload): OpeningPage => ({
  history: [],
  sans: [],
  evenchess: openingAi ? { openingAi, tts: ttsConfig } : { tts: ttsConfig },
});

describe('EvenChess opening explorer AI adapter', () => {
  test('opening pages without EvenChess AI payloads do not render', () => {
    assert.equal(openingAiStaleReason(undefined, 1000), 'not-enabled');
    assert.equal(shouldRenderOpeningAi(undefined, 1000), false);
  });

  test('server-authorized opening payload renders approved cards', () => {
    const openingAi = payload();

    assert.equal(openingAiStaleReason(openingAi, 1000), undefined);
    assert.equal(shouldRenderOpeningAi(openingAi, 1000), true);
    assert.deepEqual(
      renderableOpeningAiCards(openingAi, 1000).map(card => card.id),
      ['opening-card'],
    );
  });

  test('raw provider and prompt data suppresses opening AI client rendering', () => {
    const openingAi = payload();
    openingAi.cards![0].providerSecret = 'secret';
    openingAi.cards![0].rawPrompt = 'raw prompt';

    assert.equal(openingPayloadHasUnsafeAiData(openingAi), true);
    assert.equal(openingAiStaleReason(openingAi, 1000), 'unsafe-payload');
    assert.equal(shouldRenderOpeningAi(openingAi, 1000), false);
  });

  test('live rated unauthorized and expired payloads do not render', () => {
    assert.equal(openingAiStaleReason({ ...payload(), ratedLive: true }, 1000), 'live-rated');
    assert.equal(openingAiStaleReason({ ...payload(), serverAuthorized: false }, 1000), 'unauthorized');
    assert.equal(openingAiStaleReason({ ...payload(), expiresAt: 999 }, 1000), 'expired');
  });

  test('safe opening payload appends a branded panel to the existing opening intro', () => {
    document.body.innerHTML = '<main><div class="opening__intro__content"></div></main>';

    assert.equal(renderEvenChessOpeningAi(page(payload()), document), true);
    assert.equal(document.querySelector('.opening__evenchess-ai__brand')?.textContent, 'EvenChess AI Coach');
    assert.equal(document.querySelector('.opening__evenchess-ai__title')?.textContent, 'Opening plan');
  });

  test('safe opening payload appends a TTS button when the visible text is eligible', () => {
    document.body.innerHTML = '<main><div class="opening__intro__content"></div></main>';

    assert.equal(renderEvenChessOpeningAi(pageWithTts(payload()), document), true);
    assert.equal(
      document.querySelector('.opening__evenchess-ai__tts')?.getAttribute('aria-label'),
      'Read EvenChess opening coach card aloud',
    );
  });

  test('opening card TTS rejects hidden stronger speech text', () => {
    const openingAi = payload();
    const card = renderableOpeningAiCards(openingAi, 1000)[0]!;

    assert.equal(ttsSafetyReason(ttsConfig, openingCardTtsItem(card, openingAi)), undefined);

    card.ttsText = 'A hidden engine plan that was not displayed.';
    assert.equal(ttsSafetyReason(ttsConfig, openingCardTtsItem(card, openingAi)), 'text-mismatch');
  });

  test('invented source facts do not append an empty opening panel', () => {
    const openingAi = payload();
    openingAi.cards![0].sourceFactIds = ['invented'];
    document.body.innerHTML = '<main><div class="opening__intro__content"></div></main>';

    assert.equal(openingPayloadHasInventedFacts(openingAi), true);
    assert.equal(openingAiStaleReason(openingAi, 1000), 'invented-source-fact');
    assert.equal(renderEvenChessOpeningAi(page(openingAi), document), false);
    assert.equal(document.querySelector('.opening__evenchess-ai'), null);
  });

  test('opening page clears existing panel when payload becomes invalid', () => {
    document.body.innerHTML = '<main><div class="opening__intro__content"></div></main>';

    assert.equal(renderEvenChessOpeningAi(page(payload()), document), true);
    assert.notEqual(document.querySelector('.opening__evenchess-ai'), null);

    assert.equal(renderEvenChessOpeningAi(page({ ...payload(), ratedLive: true }), document), false);
    assert.equal(document.querySelector('.opening__evenchess-ai'), null);
  });

  test('opening page clears stale panel even when target is not present', () => {
    document.body.innerHTML = '<main class="ghost-openings"><div class="opening__evenchess-ai"></div></main>';
    const stale = document.querySelector('.opening__evenchess-ai') as HTMLElement;
    assert.notEqual(stale, null);

    assert.equal(renderEvenChessOpeningAi(page({ ...payload(), ratedLive: true }), document), false);
    assert.equal(document.querySelector('.opening__evenchess-ai'), null);
  });
});
