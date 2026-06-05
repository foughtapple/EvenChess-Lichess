import { render as renderKeyboardMove } from 'keyboard-move';
import { renderVoiceBar } from 'voice';

import { displayColumns, isTouchDevice } from 'lib/device';
import { playable } from 'lib/game';
import { renderMaterialDiffs } from 'lib/game/view/material';
import { storage } from 'lib/storage';
import { type VNode, hl, bind } from 'lib/view';
import { renderBlindfoldToggle } from 'lib/view/blindfold';
import stepwiseScroll from 'lib/view/stepwiseScroll';

import crazyView from '../crazy/crazyView';
import type RoundController from '../ctrl';
import { render as renderGround } from '../ground';
import { next, prev, view } from '../keyboard';
import { renderEvenChessBoardOverlay, renderEvenChessOverlay } from './evenchessOverlay';
import { renderTable } from './table';

interface EvenChessLayoutBinding {
  cleanup: () => void;
  observed: Set<Element>;
  observer?: ResizeObserver;
  schedule: () => void;
}

const evenChessLayoutBindings = new WeakMap<HTMLElement, EvenChessLayoutBinding>();

export function main(ctrl: RoundController): VNode {
  const d = ctrl.data,
    topColor = d[ctrl.flip ? 'player' : 'opponent'].color,
    bottomColor = d[ctrl.flip ? 'opponent' : 'player'].color,
    materialDiffs = renderMaterialDiffs(
      ctrl.data.pref.showCaptured,
      ctrl.flip ? ctrl.data.opponent.color : ctrl.data.player.color,
      ctrl.stepAt(ctrl.ply).fen,
      !!(ctrl.data.player.checks || ctrl.data.opponent.checks), // showChecks
      ctrl.data.steps,
      ctrl.ply,
    );
  const hideBoard = ctrl.data.player.blindfold && playable(ctrl.data);
  const evenChessLive = safeRenderEvenChessOverlay(ctrl);
  return ctrl.nvui
    ? ctrl.nvui.render()
    : hl(
        'div.round__app.variant-' + d.game.variant.key,
        {
          class: {
            'swap-clock': isTouchDevice() && displayColumns() === 1 && storage.boolean('swapClock').get(),
            'evenchess-live-layout': !!evenChessLive,
          },
          hook: evenChessLive ? evenChessLayoutHook : undefined,
        },
        [
          renderBlindfoldToggle(ctrl.blindfold),
          hl(
            'div.round__app__board.main-board' + (hideBoard ? '.blindfold' : ''),
            {
              hook:
                'ontouchstart' in window || !storage.boolean('scrollMoves').getOrDefault(true)
                  ? undefined
                  : bind(
                      'wheel',
                      stepwiseScroll(
                        e => {
                          if (e.deltaY > 0) next(ctrl);
                          else if (e.deltaY < 0) prev(ctrl);
                          ctrl.redraw();
                        },
                        () => ctrl.isPlaying(),
                      ),
                      undefined,
                      false,
                    ),
            },
            [
              renderGround(ctrl),
              ctrl.promotion.view(ctrl.data.game.variant.key === 'antichess'),
              safeRenderEvenChessBoardOverlay(ctrl),
            ],
          ),
          ctrl.voiceMove && renderVoiceBar(ctrl.voiceMove.ctrl, ctrl.redraw),
          evenChessLive,
          ctrl.keyboardHelp && view(ctrl),
          crazyView(ctrl, topColor, 'top') || materialDiffs[0],
          renderTable(ctrl),
          crazyView(ctrl, bottomColor, 'bottom') || materialDiffs[1],
          ctrl.keyboardMove && renderKeyboardMove(ctrl.keyboardMove),
        ],
      );
}

const evenChessLayoutHook = {
  insert: (vnode: VNode) => installEvenChessLayoutMetrics(vnode.elm as HTMLElement),
  postpatch: (_old: VNode, vnode: VNode) => installEvenChessLayoutMetrics(vnode.elm as HTMLElement),
  destroy: (vnode: VNode) => {
    const app = vnode.elm as HTMLElement;
    const binding = evenChessLayoutBindings.get(app);
    if (binding) {
      binding.cleanup();
      evenChessLayoutBindings.delete(app);
    }
  },
};

function installEvenChessLayoutMetrics(app: HTMLElement): void {
  const existing = evenChessLayoutBindings.get(app);
  if (existing) {
    observeEvenChessLayoutTargets(app, existing);
    existing.schedule();
    return;
  }

  let frame: number | undefined;
  const schedule = () => {
    if (frame !== undefined) return;
    frame = requestAnimationFrame(() => {
      frame = undefined;
      updateEvenChessLayoutMetrics(app);
    });
  };
  const binding: EvenChessLayoutBinding = {
    observed: new Set(),
    schedule,
    observer: typeof ResizeObserver === 'undefined' ? undefined : new ResizeObserver(schedule),
    cleanup: () => {
      if (frame !== undefined) cancelAnimationFrame(frame);
      binding.observer?.disconnect();
      window.removeEventListener('resize', schedule);
    },
  };

  evenChessLayoutBindings.set(app, binding);
  observeEvenChessLayoutTargets(app, binding);
  window.addEventListener('resize', schedule, { passive: true });
  schedule();
}

function observeEvenChessLayoutTargets(app: HTMLElement, binding: EvenChessLayoutBinding): void {
  if (!binding.observer) return;

  const round = app.closest<HTMLElement>('.round');
  const targets = [
    app,
    app.querySelector<HTMLElement>('.round__app__board'),
    app.querySelector<HTMLElement>('cg-board'),
    round?.querySelector<HTMLElement>(':scope > .round__side .game__meta'),
  ].filter((target): target is HTMLElement => Boolean(target));

  targets.forEach(target => {
    if (binding.observed.has(target)) return;
    binding.observed.add(target);
    binding.observer?.observe(target);
  });
}

function updateEvenChessLayoutMetrics(app: HTMLElement): void {
  const round = app.closest<HTMLElement>('.round');
  const board =
    app.querySelector<HTMLElement>('cg-board') ?? app.querySelector<HTMLElement>('.round__app__board');
  if (!round || !board) return;

  const boardRect = board.getBoundingClientRect();
  if (boardRect.height < 1) return;

  const sideMeta = round.querySelector<HTMLElement>(':scope > .round__side .game__meta');
  const sideRect = sideMeta?.getBoundingClientRect();
  const roundStyle = getComputedStyle(round);
  const gridGap = parseFloat(roundStyle.rowGap || roundStyle.gap || '0') || 0;
  const sideSitsAboveLevels =
    !!sideRect &&
    sideRect.height > 0 &&
    sideRect.top <= boardRect.top + 12 &&
    sideRect.left < boardRect.left - 4 &&
    sideRect.bottom < boardRect.bottom;
  const sideHeight = sideSitsAboveLevels ? sideRect.height : 0;
  const levelsHeight = Math.max(180, boardRect.height - sideHeight - (sideHeight ? gridGap : 0));

  app.style.setProperty('--evenchess-board-height', `${Math.round(boardRect.height)}px`);
  app.style.setProperty('--evenchess-levels-height', `${Math.round(levelsHeight)}px`);
  app.style.setProperty('--evenchess-side-card-height', `${Math.round(sideHeight)}px`);
}

function safeRenderEvenChessOverlay(ctrl: RoundController): VNode | undefined {
  try {
    return renderEvenChessOverlay(ctrl);
  } catch (error) {
    console.warn('EvenChess live overlay render failed; continuing with native round UI.', error);
    return undefined;
  }
}

function safeRenderEvenChessBoardOverlay(ctrl: RoundController): VNode | undefined {
  try {
    return renderEvenChessBoardOverlay(ctrl);
  } catch (error) {
    console.warn('EvenChess board overlay render failed; continuing with native board UI.', error);
    return undefined;
  }
}

export function endGameView(): void {
  const $body = $('body');
  if ($body.hasClass('zen-auto') && $body.hasClass('zen')) {
    $body.toggleClass('zen');
    window.dispatchEvent(new Event('resize'));
  }
}
