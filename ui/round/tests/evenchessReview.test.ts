import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import { coachDisplay, quotaAvailable, quotaText, summaryText } from '../src/evenchessReview';

describe('EvenChess round post-game review card adapter', () => {
  test('uses stored ECEMF coach text for the selected ply', () => {
    const display = coachDisplay({
      live: {
        cards: [
          {
            featureKey: 'coachText',
            title: 'Summary',
            body: 'Develop pieces before starting a flank attack.',
          },
        ],
      },
    } as any);

    assert.equal(display.title, 'Summary');
    assert.equal(display.body, 'Develop pieces before starting a flank attack.');
  });

  test('prefers saved non-live Ask AI text over the base ECEMF card', () => {
    const display = coachDisplay({
      askAi: {
        position: {
          title: 'Ask AI',
          body: 'The candidate works because the defender is pinned.',
        },
      },
      live: {
        cards: [
          {
            featureKey: 'coachText',
            title: 'Summary',
            body: 'Base ECEMF text.',
          },
        ],
      },
    } as any);

    assert.equal(display.title, 'Ask AI');
    assert.equal(display.body, 'The candidate works because the defender is pinned.');
  });

  test('extracts match summary text from supported ECE summary shapes', () => {
    assert.equal(summaryText({ summary_text: 'Direct summary.' }), 'Direct summary.');
    assert.equal(summaryText({ summary: { summary_text: 'Nested summary.' } }), 'Nested summary.');
  });

  test('shows non-live Ask AI quota availability', () => {
    assert.equal(quotaAvailable({ available: 1 }), true);
    assert.equal(quotaText({ available: 1 }), '1 available');
    assert.equal(quotaAvailable({ available: 0 }), false);
    assert.equal(quotaText({ adminUnlimited: true }), 'Unlimited');
  });
});
