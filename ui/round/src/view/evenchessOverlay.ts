import type { EvenChessTtsConfig, EvenChessTtsItem } from 'lib/evenchessTts';
import { shouldOfferEvenChessTts, shownTtsText, speakEvenChessTts } from 'lib/evenchessTts';
import * as licon from 'lib/licon';
import { type VNode, dataIcon, hl } from 'lib/view';

import type RoundController from '../ctrl';
import type {
  EvenChessBoardVisual,
  EvenChessCoachCard,
  EvenChessClearInstruction,
  EvenChessLiveOverlay,
  RoundData,
} from '../interfaces';

export interface EvenChessBoardSnapshot {
  gameId: string;
  ply: number;
  boardStateKey: string;
  now?: number;
}

const maxCards = 1;
const maxVisuals = 1;

export function currentEvenChessBoardSnapshot(ctrl: RoundController): EvenChessBoardSnapshot {
  return {
    gameId: ctrl.data.game.id,
    ply: ctrl.ply,
    boardStateKey: ctrl.stepAt(ctrl.ply).fen,
  };
}

export function payloadHasUnsafeDisplayData(overlay?: EvenChessLiveOverlay): boolean {
  if (!overlay) return false;
  return [...(overlay.cards ?? []), ...(overlay.visuals ?? [])].some(item =>
    Boolean(item.rawStockfishLine || item.hiddenDebugData),
  );
}

export function overlayStaleReason(
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): string | undefined {
  if (!overlay || !overlay.enabled) return 'not-enabled';
  if (!overlay.serverAuthorized) return 'unauthorized';
  if (overlay.stale) return 'stale';
  if (payloadHasUnsafeDisplayData(overlay)) return 'unsafe-payload';
  if (overlay.gameId !== current.gameId) return 'game-mismatch';
  if (overlay.ply !== current.ply) return 'ply-mismatch';
  if (overlay.boardStateKey !== current.boardStateKey) return 'board-mismatch';
  if (overlay.expiresAt && (current.now ?? Date.now()) >= overlay.expiresAt) return 'expired';
  if (overlay.ttlMillis <= 0) return 'expired';
  return undefined;
}

export function renderableEvenChessCards(
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): EvenChessCoachCard[] {
  if (overlayStaleReason(overlay, current) || !overlay) return [];
  return (overlay.cards ?? [])
    .filter(card => cardRenderable(card, overlay))
    .sort((a, b) => Number(Boolean(b.defaultActive)) - Number(Boolean(a.defaultActive)))
    .slice(0, maxCards);
}

export function renderableEvenChessVisuals(
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): EvenChessBoardVisual[] {
  if (overlayStaleReason(overlay, current) || !overlay) return [];
  return (overlay.visuals ?? [])
    .filter(visual => visualRenderable(visual, overlay))
    .sort((a, b) => Number(Boolean(b.primary)) - Number(Boolean(a.primary)))
    .slice(0, maxVisuals);
}

export function clearEvenChessLiveOverlay(
  data: RoundData,
  reason: string,
  ply: number,
  boardStateKey: string,
): void {
  const live = data.evenchess?.live;
  if (!live) return;
  const clear: EvenChessClearInstruction = {
    gameId: live.gameId || data.game.id,
    ply,
    boardStateKey,
    reason,
    auditId: live.auditId,
  };
  data.evenchess = {
    ...data.evenchess,
    live: {
      ...live,
      stale: true,
      cards: [],
      visuals: [],
      clear: [clear],
    },
  };
}

export function applyEvenChessLiveOverlay(data: RoundData, overlay: EvenChessLiveOverlay): void {
  const clearOnMismatch = overlay.gameId !== data.game.id;
  const clearInstruction: EvenChessClearInstruction = {
    gameId: overlay.gameId,
    ply: overlay.ply,
    boardStateKey: overlay.boardStateKey,
    reason: clearOnMismatch ? 'game-mismatch' : 'unsafe-payload',
    auditId: overlay.auditId,
  };
  const sanitized = payloadHasUnsafeDisplayData(overlay)
    ? {
        ...overlay,
        stale: true,
        cards: [],
        visuals: [],
        clear: [clearInstruction],
      }
    : clearOnMismatch
      ? {
          ...overlay,
          stale: true,
          cards: [],
          visuals: [],
          clear: [clearInstruction],
        }
      : overlay;

  data.evenchess = {
    ...data.evenchess,
    live: sanitized,
  };
}

export function shouldRenderEvenChessOverlay(
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): boolean {
  return (
    renderableEvenChessCards(overlay, current).length > 0 ||
    renderableEvenChessVisuals(overlay, current).length > 0
  );
}

export function liveCardTtsItem(
  card: EvenChessCoachCard,
  overlay: EvenChessLiveOverlay,
  isPlayerTurn: boolean,
): EvenChessTtsItem {
  return {
    id: card.id,
    surface: 'live',
    displayedText: shownTtsText(card.title, card.body),
    text: card.ttsText,
    auditId: card.auditId || overlay.auditId,
    serverAuthorized: card.serverAuthorized && overlay.serverAuthorized,
    approvedDisplayPayload: card.approvedDisplayPayload,
    ratedLive: true,
    isPlayerTurn,
    rawStockfishLine: card.rawStockfishLine,
    hiddenDebugData: card.hiddenDebugData,
  };
}

export function renderEvenChessOverlay(ctrl: RoundController): VNode | undefined {
  const overlay = ctrl.data.evenchess?.live;
  const ttsConfig = ctrl.data.evenchess?.tts;
  const current = currentEvenChessBoardSnapshot(ctrl);
  const cards = renderableEvenChessCards(overlay, current);
  const visuals = renderableEvenChessVisuals(overlay, current);

  if (!overlay || (!cards.length && !visuals.length)) return undefined;

  return hl(
    'aside.evenchess-live',
    {
      attrs: {
        'data-evenchess-overlay': 'live',
        'data-audit-id': overlay.auditId,
        'data-ply': String(overlay.ply),
        role: 'region',
        'aria-live': 'polite',
        'aria-label': 'EvenChess coaching',
      },
    },
    [
      ...cards.map(card =>
        hl('section.evenchess-live__card', [
          hl('div.evenchess-live__head', [
            hl('strong.evenchess-live__label', 'EvenChess Coach'),
            hl('span.evenchess-live__head-actions', [
              renderTtsButton(ttsConfig, liveCardTtsItem(card, overlay, ctrl.canMove())),
              hl('span.evenchess-live__audit', `Audit ${card.auditId}`),
            ]),
          ]),
          hl('h2.evenchess-live__title', card.title),
          hl('p.evenchess-live__body', card.body),
        ]),
      ),
      visuals.length
        ? hl(
            'div.evenchess-live__visuals',
            visuals.map(visual =>
              hl(
                'span.evenchess-live__visual',
                {
                  attrs: {
                    'data-feature': visual.featureKey,
                    'data-audit-id': visual.auditId,
                  },
                },
                visual.label,
              ),
            ),
          )
        : undefined,
    ],
  );
}

function renderTtsButton(config: EvenChessTtsConfig | undefined, item: EvenChessTtsItem): VNode | undefined {
  if (!shouldOfferEvenChessTts(config, item)) return undefined;
  return hl('button.evenchess-live__tts', {
    attrs: {
      ...dataIcon(licon.Voice),
      type: 'button',
      title: 'Read aloud',
      'aria-label': 'Read EvenChess coach card aloud',
    },
    on: {
      click: (event: Event) => {
        event.preventDefault();
        event.stopPropagation();
        speakEvenChessTts(config, item);
      },
    },
  });
}

function cardRenderable(card: EvenChessCoachCard, overlay: EvenChessLiveOverlay): boolean {
  return (
    Boolean(card.id) &&
    card.gameId === overlay.gameId &&
    card.ply === overlay.ply &&
    card.boardStateKey === overlay.boardStateKey &&
    card.auditId === overlay.auditId &&
    Boolean(card.featureKey) &&
    Boolean(card.title) &&
    Boolean(card.body) &&
    card.level >= 0 &&
    card.visibility !== 'suppressed' &&
    card.serverAuthorized &&
    card.approvedDisplayPayload &&
    !card.stale &&
    (card.ttlMillis ?? overlay.ttlMillis) > 0 &&
    !card.rawStockfishLine &&
    !card.hiddenDebugData
  );
}

function visualRenderable(visual: EvenChessBoardVisual, overlay: EvenChessLiveOverlay): boolean {
  return (
    Boolean(visual.id) &&
    visual.gameId === overlay.gameId &&
    visual.ply === overlay.ply &&
    visual.boardStateKey === overlay.boardStateKey &&
    visual.auditId === overlay.auditId &&
    Boolean(visual.featureKey) &&
    Boolean(visual.label) &&
    visual.serverAuthorized &&
    visual.approvedDisplayPayload &&
    !visual.stale &&
    !visual.rawStockfishLine &&
    !visual.hiddenDebugData
  );
}
