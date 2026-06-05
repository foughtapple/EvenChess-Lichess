import { timePickerAndSliders } from 'lib/setup/view/timeControl';
import { hl, type VNode, type LooseVNodes, snabDialog, spinnerVdom } from 'lib/view';

import type LobbyController from '@/ctrl';
import {
  evenChessScenarioLabel,
  evenChessSubmitMode,
  evenChessTemporaryFreeTokenMessage,
  evenChessTokenGateText,
} from '@/evenchessSetup';

import { colorButtons } from './components/colorButtons';
import { fenInput } from './components/fenInput';
import { gameModeButtons } from './components/gameModeButtons';
import { levelButtons } from './components/levelButtons';
import { variantPicker } from './components/variantPicker';

export default function setupModal(ctrl: LobbyController): VNode[] | null {
  const { setupCtrl } = ctrl;
  if (!setupCtrl.gameType) return null;
  const buttonText = {
    hook: i18n.site.createLobbyGame,
    friend: setupCtrl.friendUser ? i18n.site.challengeX(setupCtrl.friendUser) : i18n.site.challengeAFriend,
    ai: i18n.site.playAgainstComputer,
  }[setupCtrl.gameType];
  const disabled = !setupCtrl.valid() || setupCtrl.loading;
  return [
    snabDialog({
      attrs: { dialog: { 'aria-labelledBy': 'lobby-setup-modal-title', 'aria-modal': 'true' } },
      class: 'game-setup',
      css: [{ hashed: 'lobby.setup' }],
      onClose: () => {
        setupCtrl.closeModal = undefined;
        setupCtrl.gameType = null;
        setupCtrl.root.redraw();
      },
      modal: true,
      vnodes: [
        hl('h2#lobby-setup-modal-title', i18n.site.gameSetup),
        hl('div.setup-content', views[setupCtrl.gameType](ctrl)),
        hl('div.footer', [
          hl(
            `button.button.button-metal.lobby__start__button.lobby__start__button--${setupCtrl.friendUser ? 'friend-user' : setupCtrl.gameType}`,
            {
              attrs: { disabled },
              class: { disabled },
              on: { click: setupCtrl.submit },
            },
            buttonText,
          ),
          setupCtrl.loading && spinnerVdom(),
        ]),
      ],
      onInsert: dlg => {
        setupCtrl.closeModal = dlg.close;
        dlg.show();
      },
    }),
  ].filter(v => v !== null);
}

const views = {
  hook: (ctrl: LobbyController): LooseVNodes => [
    variantPicker(ctrl.setupCtrl),
    timePickerAndSliders(ctrl.setupCtrl.timeControl, 0),
    gameModeButtons(ctrl),
    evenChessSettings(ctrl),
    colorButtons(ctrl.setupCtrl),
  ],
  friend: (ctrl: LobbyController): LooseVNodes => [
    variantPicker(ctrl.setupCtrl),
    fenInput(ctrl.setupCtrl),
    timePickerAndSliders(ctrl.setupCtrl.timeControl, 0),
    gameModeButtons(ctrl),
    evenChessSettings(ctrl),
    colorButtons(ctrl.setupCtrl),
  ],
  ai: (ctrl: LobbyController): LooseVNodes => [
    variantPicker(ctrl.setupCtrl),
    fenInput(ctrl.setupCtrl),
    timePickerAndSliders(ctrl.setupCtrl.timeControl, ctrl.setupCtrl.minimumTimeIfReal()),
    levelButtons(ctrl.setupCtrl),
    colorButtons(ctrl.setupCtrl),
  ],
};

function evenChessSettings(ctrl: LobbyController): VNode {
  const { setupCtrl } = ctrl;
  if (setupCtrl.gameType === 'friend') return evenChessFriendSettings(ctrl);

  const preferredSetLevel = setupCtrl.evenChessPlayerTargetLevel().trim();
  const submitMode = evenChessSubmitMode(setupCtrl.gameType, setupCtrl.gameMode());
  const modeLabel = {
    hook: setupCtrl.gameMode() === 'rated' ? 'Rated EvenChess' : 'Casual EvenChess',
    friend: 'Casual EvenChess',
    ai: i18n.site.playAgainstComputer,
  }[setupCtrl.gameType || 'hook'];
  const targetOptions = (selected: string) => [
    hl('option', { attrs: { value: '', selected: selected === '' } }, 'Any'),
    ...(Array.from({ length: 11 }, (_, level) =>
      hl('option', { attrs: { value: String(level), selected: selected === String(level) } }, `L${level}`))),
  ];
  const freeTokenMessage = evenChessTemporaryFreeTokenMessage(
    setupCtrl.root.opts.evenChessTokenBalance,
    submitMode,
  );

  return hl('section.evenchess-setup.config-group', [
    hl('div.label', 'EvenChess'),
    hl('div.evenchess-setup__grid', [
      hl('label.evenchess-setup__field', [hl('span', 'Mode'), hl('strong', modeLabel)]),
      hl('label.evenchess-setup__field', [
        hl('span', 'Preferred set level'),
        hl(
          'select',
          {
            attrs: { value: preferredSetLevel },
            on: {
              change: (e: Event) =>
                setupCtrl.evenChessPlayerTargetLevel((e.target as HTMLSelectElement).value),
            },
          },
          targetOptions(preferredSetLevel),
        ),
      ]),
      hl('label.evenchess-setup__field.evenchess-setup__field--summary', [
        hl('span', 'Search scenario'),
        hl('strong', evenChessScenarioLabel(preferredSetLevel)),
      ]),
    ]),
    hl('p.evenchess-setup__gate', [
      hl('strong', 'Token gate: '),
      evenChessTokenGateText(setupCtrl.root.opts.evenChessTokenBalance, submitMode),
    ]),
    freeTokenMessage && hl('p.evenchess-setup__free-tokens', freeTokenMessage),
    hl('p.evenchess-setup__rule', [
      hl('strong', 'Disclosure: '),
      'Platform coaching is allowed only because it is disclosed, capped by Set Level, logged, and rated into ECR. External engines, people, notes, bots, chat, and unaudited analysis remain prohibited in rated games.',
    ]),
  ]);
}

function evenChessFriendSettings({ setupCtrl }: LobbyController): VNode {
  const submitMode = evenChessSubmitMode(setupCtrl.gameType, setupCtrl.gameMode());
  const mode = setupCtrl.evenChessFriendLevelMode();
  const myLevel = setupCtrl.evenChessFriendMyLevel();
  const opponentLevel = setupCtrl.evenChessFriendOpponentLevel();
  const freeTokenMessage = evenChessTemporaryFreeTokenMessage(
    setupCtrl.root.opts.evenChessTokenBalance,
    submitMode,
  );
  const levelOptions = (selected: string) =>
    Array.from({ length: 11 }, (_, level) =>
      hl('option', { attrs: { value: String(level), selected: selected === String(level) } }, `L${level}`));

  return hl('section.evenchess-setup.config-group', [
    hl('div.label', 'EvenChess'),
    hl('div.evenchess-setup__grid', [
      hl('label.evenchess-setup__field', [hl('span', 'Mode'), hl('strong', 'Friend EvenChess')]),
      hl('label.evenchess-setup__field', [
        hl('span', 'Level setup'),
        hl(
          'select',
          {
            attrs: { value: mode },
            on: {
              change: (e: Event) =>
                setupCtrl.evenChessFriendLevelMode((e.target as HTMLSelectElement).value),
            },
          },
          [
            hl('option', { attrs: { value: 'auto', selected: mode === 'auto' } }, 'Auto level'),
            hl('option', { attrs: { value: 'my', selected: mode === 'my' } }, 'Set my level'),
            hl('option', { attrs: { value: 'opponent', selected: mode === 'opponent' } }, 'Set opponent level'),
            hl('option', { attrs: { value: 'both', selected: mode === 'both' } }, 'Set both levels'),
          ],
        ),
      ]),
      (mode === 'my' || mode === 'both') &&
        hl('label.evenchess-setup__field', [
          hl('span', 'My level'),
          hl(
            'select',
            {
              attrs: { value: myLevel },
              on: {
                change: (e: Event) =>
                  setupCtrl.evenChessFriendMyLevel((e.target as HTMLSelectElement).value),
              },
            },
            levelOptions(myLevel),
          ),
        ]),
      (mode === 'opponent' || mode === 'both') &&
        hl('label.evenchess-setup__field', [
          hl('span', 'Opponent level'),
          hl(
            'select',
            {
              attrs: { value: opponentLevel },
              on: {
                change: (e: Event) =>
                  setupCtrl.evenChessFriendOpponentLevel((e.target as HTMLSelectElement).value),
              },
            },
            levelOptions(opponentLevel),
          ),
        ]),
    ]),
    hl('p.evenchess-setup__gate', [
      hl('strong', 'Token gate: '),
      evenChessTokenGateText(setupCtrl.root.opts.evenChessTokenBalance, submitMode),
    ]),
    freeTokenMessage && hl('p.evenchess-setup__free-tokens', freeTokenMessage),
    hl('p.evenchess-setup__rule', [
      hl('strong', 'Disclosure: '),
      'Platform coaching is allowed only because it is disclosed, capped by Set Level, logged, and rated into ECR. External engines, people, notes, bots, chat, and unaudited analysis remain prohibited in rated games.',
    ]),
  ]);
}
