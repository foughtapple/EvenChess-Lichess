import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import { coachDisplay, quotaAvailable, quotaText, summaryText } from '../src/view/evenchessReview';

describe('EvenChess post-game review card adapter', () => {
  test('uses stored ECEMF coach text for the selected ply', () => {
    const display = coachDisplay({
      live: {
        cards: [
          {
            featureKey: 'coachText',
            title: 'Summary',
            body: 'Use the opening to contest the centre.',
          },
        ],
      },
    } as any);

    assert.equal(display.title, 'Summary');
    assert.equal(display.body, 'Use the opening to contest the centre.');
  });

  test('prefers saved non-live Ask AI text over the base ECEMF card', () => {
    const display = coachDisplay({
      askAi: {
        position: {
          title: 'Ask AI',
          body: 'The tactic works because the defender is overloaded.',
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
    assert.equal(display.body, 'The tactic works because the defender is overloaded.');
  });

  test('extracts match summary text from supported ECE summary shapes', () => {
    assert.equal(summaryText({ summary_text: 'Direct summary.' }), 'Direct summary.');
    assert.equal(summaryText({ match_summary: { summary_text: 'Nested summary.' } }), 'Nested summary.');
  });

  test('shows non-live Ask AI quota availability', () => {
    assert.equal(quotaAvailable({ available: 2 }), true);
    assert.equal(quotaText({ available: 2 }), '2 available');
    assert.equal(quotaAvailable({ available: 0 }), false);
    assert.equal(quotaText({ adminUnlimited: true }), 'Unlimited');
  });
});
