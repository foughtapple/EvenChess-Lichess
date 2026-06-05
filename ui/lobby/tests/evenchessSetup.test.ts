import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import {
  evenChessClockParamsForPoolId,
  evenChessComputerSetLevel,
  evenChessLobbyActionDisabled,
  evenChessLevelValid,
  evenChessPendingPoolId,
  evenChessPreferredSetLevelParam,
  evenChessScenarioLabel,
  evenChessSearchStatusDebugEnabled,
  evenChessSetLevelForGameType,
  evenChessSubmitMode,
  evenChessTemporaryFreeTokenMessage,
  evenChessTimeControlKeyForPoolId,
  evenChessTokenGateText,
} from '../src/evenchessSetup';
import type { EvenChessTokenBalance } from '../src/interfaces';

const tokenBalance: EvenChessTokenBalance = {
  visibleGameTokens: 10,
  displayCount: '10',
  displayLabel: 'tokens',
  href: '/evenchess/account#rewarded-ads',
  source: 'evenchess-account-entitlements-v1',
  subscriptionActive: false,
};

describe('EvenChess setup UI helpers', () => {
  test('validates L0-L10 setup levels only', () => {
    assert.equal(evenChessLevelValid(''), true);
    assert.equal(evenChessLevelValid('  '), true);
    assert.equal(evenChessLevelValid(0), true);
    assert.equal(evenChessLevelValid('10'), true);
    assert.equal(evenChessLevelValid('-1'), false);
    assert.equal(evenChessLevelValid('11'), false);
    assert.equal(evenChessLevelValid('4.5'), false);
  });

  test('labels normal and preferred set level scenarios', () => {
    assert.equal(evenChessScenarioLabel(''), 'Normal search');
    assert.equal(evenChessScenarioLabel(' '), 'Normal search');
    assert.equal(evenChessScenarioLabel('any'), 'Normal search');
    assert.equal(evenChessScenarioLabel('ANY'), 'Normal search');
    assert.equal(evenChessScenarioLabel('4'), 'Preferred set level search');
  });

  test('normalizes preferred set level submit param to Any omission or L0-L10 only', () => {
    assert.equal(evenChessPreferredSetLevelParam(''), undefined);
    assert.equal(evenChessPreferredSetLevelParam('  '), undefined);
    assert.equal(evenChessPreferredSetLevelParam('any'), undefined);
    assert.equal(evenChessPreferredSetLevelParam('ANY'), undefined);
    assert.equal(evenChessPreferredSetLevelParam('0'), '0');
    assert.equal(evenChessPreferredSetLevelParam('10'), '10');
    assert.equal(evenChessPreferredSetLevelParam('-1'), undefined);
    assert.equal(evenChessPreferredSetLevelParam('11'), undefined);
  });

  test('maps quick-pairing pool clocks to EvenChess search time-control buckets', () => {
    assert.equal(evenChessTimeControlKeyForPoolId('1+0'), 'bullet');
    assert.equal(evenChessTimeControlKeyForPoolId('3+0'), 'blitz');
    assert.equal(evenChessTimeControlKeyForPoolId('5+0'), 'blitz');
    assert.equal(evenChessTimeControlKeyForPoolId('10+0'), 'rapid');
    assert.equal(evenChessTimeControlKeyForPoolId('30+0'), 'classical');
    assert.equal(evenChessTimeControlKeyForPoolId('bad'), 'rapid');
  });

  test('extracts exact clock params from quick-pairing pool ids', () => {
    assert.deepEqual(evenChessClockParamsForPoolId('5+3'), {
      clockLimitSeconds: '300',
      clockIncrementSeconds: '3',
    });
    assert.deepEqual(evenChessClockParamsForPoolId('10+0'), {
      clockLimitSeconds: '600',
      clockIncrementSeconds: '0',
    });
    assert.equal(evenChessClockParamsForPoolId('bad'), undefined);
  });

  test('maps create-lobby submit to native pending quick-pairing card when settings match a pool', () => {
    assert.equal(
      evenChessPendingPoolId({
        gameType: 'hook',
        variant: 'standard',
        gameMode: 'rated',
        color: 'random',
        isRealTime: true,
        clock: '5+3',
        poolIds: ['3+0', '5+3', '10+0'],
      }),
      '5+3',
    );
    assert.equal(
      evenChessPendingPoolId({
        gameType: 'hook',
        variant: 'standard',
        gameMode: 'rated',
        color: 'white',
        isRealTime: true,
        clock: '5+3',
        poolIds: ['5+3'],
      }),
      undefined,
    );
    assert.equal(
      evenChessPendingPoolId({
        gameType: 'hook',
        variant: 'standard',
        gameMode: 'casual',
        color: 'random',
        isRealTime: true,
        clock: '5+3',
        poolIds: ['5+3'],
      }),
      undefined,
    );
  });

  test('preferred set level search keeps hook search in rated or casual mode', () => {
    assert.equal(evenChessSubmitMode('hook', 'rated'), 'rated');
    assert.equal(evenChessSubmitMode('hook', 'casual'), 'casual');
    assert.equal(evenChessSubmitMode('friend', 'rated'), 'casual');
    assert.equal(evenChessSubmitMode('ai', 'casual'), 'ai');
  });

  test('keeps verbose search status hidden unless explicitly debug-enabled', () => {
    assert.equal(evenChessSearchStatusDebugEnabled('', undefined), false);
    assert.equal(evenChessSearchStatusDebugEnabled('?preferredSetLevel=4', undefined), false);
    assert.equal(evenChessSearchStatusDebugEnabled('?evenchessSearchDebug=1', undefined), true);
    assert.equal(evenChessSearchStatusDebugEnabled('?evenChessSearchDebug=true', undefined), true);
    assert.equal(evenChessSearchStatusDebugEnabled('', 'on'), true);
    assert.equal(evenChessSearchStatusDebugEnabled('', '0'), false);
  });

  test('computer games force the EvenChess set level to L10 without changing other setup defaults', () => {
    assert.equal(evenChessSetLevelForGameType('ai', undefined), evenChessComputerSetLevel);
    assert.equal(evenChessSetLevelForGameType('ai', 4), evenChessComputerSetLevel);
    assert.equal(evenChessSetLevelForGameType('hook', undefined), 5);
    assert.equal(evenChessSetLevelForGameType('hook', 7), 7);
    assert.equal(evenChessSetLevelForGameType('friend', 3), 3);
  });

  test('keeps homepage play actions available while another real-time game is waiting', () => {
    const gate = {
      playban: false,
      hasUnreadLichessMessage: false,
      isBot: false,
      hasOngoingRealTimeGame: true,
    };

    assert.equal(evenChessLobbyActionDisabled('hook', gate), false);
    assert.equal(evenChessLobbyActionDisabled('friend', gate), false);
    assert.equal(evenChessLobbyActionDisabled('ai', gate), false);
    assert.equal(evenChessLobbyActionDisabled('hook', { ...gate, isBot: true }), true);
  });

  test('token-gate copy is visible and does not imply stronger help', () => {
    assert.equal(
      evenChessTokenGateText(tokenBalance, 'rated'),
      '10 game tokens available; failed search does not spend a token.',
    );
    assert.equal(
      evenChessTokenGateText(undefined, 'rated'),
      'Token or subscription access will be checked on submit.',
    );
    assert.equal(
      evenChessTokenGateText(tokenBalance, 'target'),
      'No game token required for this search mode.',
    );
    assert.equal(evenChessTokenGateText(tokenBalance, 'rated').includes('stronger'), false);
  });

  test('temporary free-token launch copy overrides rated and casual token gate copy only', () => {
    const freeTokenBalance = {
      ...tokenBalance,
      freeMatchTokensActive: true,
      freeMatchTokensMessage: 'Tokens are temporarily free',
    };

    assert.equal(
      evenChessTokenGateText(freeTokenBalance, 'rated'),
      'This search will not consume startup or earned game tokens.',
    );
    assert.equal(evenChessTemporaryFreeTokenMessage(freeTokenBalance, 'rated'), 'Tokens are temporarily free');
    assert.equal(evenChessTemporaryFreeTokenMessage(freeTokenBalance, 'casual'), 'Tokens are temporarily free');
    assert.equal(evenChessTemporaryFreeTokenMessage(freeTokenBalance, 'target'), undefined);
    assert.equal(evenChessTemporaryFreeTokenMessage(freeTokenBalance, 'ai'), undefined);
  });
});
