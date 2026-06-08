import { numberFormat } from 'lib/i18n';
import { bind, onInsert, hl, thunk } from 'lib/view';

import type LobbyController from '@/ctrl';
import { evenChessLobbyActionDisabled, evenChessSearchStatusDebugEnabled } from '@/evenchessSetup';
import type { GameType } from '@/interfaces';

import renderSetupModal from './setup/modal';

type ButtonInfo = { gameType: GameType | 'dev'; label: string; disabled?: boolean; title?: string };

export default function table(ctrl: LobbyController) {
  const { data, opts } = ctrl;
  const gate = {
    playban: opts.playban,
    hasUnreadLichessMessage: opts.hasUnreadLichessMessage,
    isBot: ctrl.me?.isBot,
    hasOngoingRealTimeGame: ctrl.hasOngoingRealTimeGame(),
  };
  const { members, rounds } = data.counters;
  const lobbyButtons: ButtonInfo[] = [
    {
      gameType: 'hook',
      label: i18n.site.createLobbyGame,
      disabled: evenChessLobbyActionDisabled('hook', gate),
      title: 'Create a custom game that any online player can join.',
    },
    {
      gameType: 'friend',
      label: i18n.site.challengeAFriend,
      disabled: evenChessLobbyActionDisabled('friend', gate),
      title: $trim`
        Create a custom game and choose your opponent.

        You will receive a challenge link to share via email or text, as well as a QR code
        that someone nearby can scan.`,
    },
    {
      gameType: 'ai',
      label: i18n.site.playAgainstComputer,
      disabled: evenChessLobbyActionDisabled('ai', gate),
    },
  ];

  return hl('div.lobby__table', [
    hl('div.lobby__start', [site.blindMode && hl('h2', i18n.site.play), lobbyButtons.map(makeLobbyButton)]),
    renderEvenChessSearchStatus(ctrl),
    renderSetupModal(ctrl),
    site.blindMode
      ? undefined
      : // Use a thunk here so that snabbdom does not rerender; we will do so manually after insert
        thunk(
          'div.lobby__counters',
          () =>
            hl('div.lobby__counters', [
              hl(
                'a',
                { attrs: { href: '/player' } },
                i18n.site.nbPlayers.asArray(
                  members,
                  hl(
                    'strong',
                    {
                      attrs: { 'data-count': members },
                      hook: onInsert<HTMLAnchorElement>(elm => {
                        ctrl.spreadPlayersNumber = ctrl.initNumberSpreader(elm, 10, members);
                      }),
                    },
                    numberFormat(members),
                  ),
                ),
              ),
              hl(
                'a',
                { attrs: { href: '/games' } },
                i18n.site.nbGamesInPlay.asArray(
                  rounds,
                  hl(
                    'strong',
                    {
                      attrs: { 'data-count': rounds },
                      hook: onInsert<HTMLAnchorElement>(elm => {
                        ctrl.spreadGamesNumber = ctrl.initNumberSpreader(elm, 8, rounds);
                      }),
                    },
                    numberFormat(rounds),
                  ),
                ),
              ),
            ]),
          [],
        ),
  ]);

  function makeLobbyButton({ gameType, label, disabled, title }: ButtonInfo) {
    return hl(
      `button.button.button-metal.lobby__start__button.lobby__start__button--${gameType}`,
      {
        class: { active: ctrl.setupCtrl.gameType === gameType, disabled: !!disabled },
        attrs: {
          type: 'button',
          title: title ?? '',
          'aria-disabled': disabled ? 'true' : 'false',
        },
        hook: disabled
          ? {}
          : bind(
              'click',
              () => {
                if (gameType === 'dev') location.href = '/bots/dev';
                else ctrl.setupCtrl.openModal(gameType);
              },
              ctrl.redraw,
            ),
      },
      label,
    );
  }
}

function renderEvenChessSearchStatus(ctrl: LobbyController) {
  const status = ctrl.evenChessSearchStatus;
  if (!status || !evenChessSearchStatusDebugEnabled()) return undefined;
  const formatWaitMs = (elapsedMillis?: number) => {
    if (!Number.isFinite(elapsedMillis ?? NaN) || elapsedMillis! < 0) return '0s';
    const totalSeconds = Math.round(elapsedMillis! / 1000);
    return `${Math.max(0, totalSeconds)}s`;
  };
  const botMode = status.matchmaking?.botMode;
  const botModeStatus = botMode?.enabled ? 'On' : 'Off';
  const matchContract = status.matchmaking?.matchContract;
  const assignedLevels =
    typeof matchContract?.whiteSetLevel === 'number' && typeof matchContract?.blackSetLevel === 'number'
      ? `White L${matchContract.whiteSetLevel} / Black L${matchContract.blackSetLevel}`
      : undefined;

  return hl(
    'section.lobby__evenchess-status',
    {
      attrs: {
        role: 'status',
        'aria-live': 'polite',
      },
    },
    [
      hl('div.lobby__evenchess-status__head', [
        hl('strong', status.ok ? 'EvenChess search started' : 'EvenChess search blocked'),
        hl(
          'button.button-empty',
          {
            attrs: { type: 'button' },
            on: {
              click: () => {
                ctrl.setupCtrl.stopEvenChessSearchPolling();
                ctrl.evenChessSearchStatus = undefined;
                ctrl.redraw();
              },
            },
          },
          'Clear',
        ),
      ]),
      status.ok
        ? hl('div.lobby__evenchess-status__grid', [
            status.queueLabel ? statusItem('Queue', status.queueLabel) : undefined,
            statusItem('Search key', status.searchKey ? 'Active' : 'Missing'),
            statusItem('Redirect', status.redirectUrl ? 'Ready' : 'Pending'),
            assignedLevels
              ? statusItem('Assigned levels', assignedLevels)
              : typeof status.setLevel === 'number'
                ? statusItem('Search level hint', `L${status.setLevel}`)
                : undefined,
            typeof status.preferredSetLevel === 'number'
              ? statusItem('Preferred level', `L${status.preferredSetLevel}`)
              : undefined,
            status.searchScenario ? statusItem('Scenario', status.searchScenario) : undefined,
            status.accessLabel ? statusItem('Access', status.accessLabel) : undefined,
            botMode ? statusItem('Bots', botModeStatus) : undefined,
            botMode ? statusItem('Wait since enqueue', formatWaitMs(botMode.elapsedMillis)) : undefined,
            botMode?.disclosure ? statusItem('Bot mode note', botMode.disclosure) : undefined,
            status.matchmaking?.status ? statusItem('Match status', status.matchmaking.status) : undefined,
          ])
        : hl('p.lobby__evenchess-status__error', status.error || 'EvenChess search could not start.'),
    ],
  );
}

function statusItem(label: string, value: string) {
  return hl('span.lobby__evenchess-status__item', [hl('span', label), hl('strong', value)]);
}
