import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import type { EvenChessSpeechDriver, EvenChessTtsConfig, EvenChessTtsItem } from '../src/evenchessTts';
import {
  normalizeEvenChessTtsText,
  shouldOfferEvenChessTts,
  shownTtsText,
  speakEvenChessTts,
  ttsAuditEvent,
  ttsSafetyReason,
} from '../src/evenchessTts';

const config = (overrides: Partial<EvenChessTtsConfig> = {}): EvenChessTtsConfig => ({
  enabled: true,
  provider: 'browser-speech',
  voice: 'system-default',
  ratePercent: 100,
  volumePercent: 80,
  queueBehavior: 'replace-current',
  muteDuringOpponentTurn: true,
  serverAuthorized: true,
  policyVersion: 'tts-v1',
  ...overrides,
});

const item = (overrides: Partial<EvenChessTtsItem> = {}): EvenChessTtsItem => ({
  id: 'card-1',
  surface: 'live',
  displayedText: shownTtsText('Coach card', 'Improve the loose piece first.'),
  auditId: 'audit-live-1',
  serverAuthorized: true,
  approvedDisplayPayload: true,
  ratedLive: true,
  isPlayerTurn: true,
  ...overrides,
});

const driver = () => {
  const calls: string[] = [];
  const fake: EvenChessSpeechDriver = {
    supported: true,
    cancel: () => calls.push('cancel'),
    speak: text => calls.push(`speak:${text}`),
  };
  return { fake, calls };
};

describe('EvenChess TTS browser policy', () => {
  test('normalizes card text for spoken output', () => {
    assert.equal(normalizeEvenChessTtsText('  A\n  safe   card  '), 'A safe card');
    assert.equal(shownTtsText('Title', 'Body', ['Bullet']), 'Title Body Bullet');
  });

  test('is off by default when config is missing or disabled', () => {
    assert.equal(ttsSafetyReason(undefined, item()), 'disabled');
    assert.equal(ttsSafetyReason(config({ enabled: false }), item()), 'disabled');
    assert.equal(shouldOfferEvenChessTts(config({ enabled: false }), item()), false);
  });

  test('only speaks the same text already shown in the UI', () => {
    const mismatch = item({ text: 'A stronger move order that was not displayed.' });

    assert.equal(ttsSafetyReason(config(), mismatch), 'text-mismatch');
    assert.equal(shouldOfferEvenChessTts(config(), mismatch), false);
  });

  test('blocks raw engine prompt provider secret and debug payloads', () => {
    assert.equal(ttsSafetyReason(config({ rawPrompt: 'hidden prompt' }), item()), 'unsafe-payload');
    assert.equal(ttsSafetyReason(config(), item({ rawStockfishLine: 'pv e2e4 e7e5' })), 'unsafe-payload');
    assert.equal(ttsSafetyReason(config(), item({ providerSecret: 'sk-secret' })), 'unsafe-payload');
    assert.equal(ttsSafetyReason(config(), item({ hiddenDebugData: 'trace' })), 'unsafe-payload');
  });

  test('requires authorization and live audit identity', () => {
    assert.equal(ttsSafetyReason(config({ serverAuthorized: false }), item()), 'unauthorized');
    assert.equal(ttsSafetyReason(config(), item({ approvedDisplayPayload: false })), 'unauthorized');
    assert.equal(ttsSafetyReason(config(), item({ auditId: '' })), 'missing-audit');
  });

  test('requires audit identity for learning-surface overlay reads too', () => {
    assert.equal(
      ttsSafetyReason(
        config(),
        item({
          surface: 'analysis',
          ratedLive: false,
          auditId: '',
        }),
      ),
      'missing-audit',
    );
  });

  test('mutes live TTS during opponent turn when configured', () => {
    assert.equal(ttsSafetyReason(config(), item({ isPlayerTurn: false })), 'muted-opponent-turn');
    assert.equal(
      ttsSafetyReason(config({ muteDuringOpponentTurn: false }), item({ isPlayerTurn: false })),
      undefined,
    );
  });

  test('uses browser speech with replace-current queue behavior', () => {
    const { fake, calls } = driver();
    const result = speakEvenChessTts(config(), item(), fake);

    assert.deepEqual(calls, ['cancel', `speak:${item().displayedText}`]);
    assert.equal(result.spoken, true);
    assert.equal(result.auditId, 'audit-live-1');
    assert.deepEqual(result.auditEvent, {
      sourceAuditId: 'audit-live-1',
      surface: 'live',
      itemId: 'card-1',
      policyVersion: 'tts-v1',
      charCount: item().displayedText.length,
      ratedLive: true,
    });
    assert.deepEqual(ttsAuditEvent(config(), item()), result.auditEvent);
  });

  test('server provider is a seam but unsupported by the browser path', () => {
    const result = speakEvenChessTts(config({ provider: 'server-provider' }), item(), driver().fake);

    assert.deepEqual(result, { spoken: false, reason: 'unsupported-provider' });
  });

  test('reports unsupported browser without leaking text through fallback code', () => {
    const result = speakEvenChessTts(config(), item(), {
      supported: false,
      cancel: () => {},
      speak: () => {},
    });

    assert.deepEqual(result, { spoken: false, reason: 'unsupported-browser' });
  });
});
