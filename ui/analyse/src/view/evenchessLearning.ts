import type { EvenChessTtsConfig, EvenChessTtsItem } from 'lib/evenchessTts';
import { shouldOfferEvenChessTts, shownTtsText, speakEvenChessTts } from 'lib/evenchessTts';
import * as licon from 'lib/licon';
import { type VNode, dataIcon, hl } from 'lib/view';

import type AnalyseCtrl from '../ctrl';
import type {
  EvenChessLearningCard,
  EvenChessLearningPayload,
  EvenChessLearningSourceFact,
  EvenChessLearningSurface,
} from '../interfaces';

export interface EvenChessLearningSnapshot {
  surface: EvenChessLearningSurface;
  contextId: string;
  boardStateKey: string;
  ply: number;
  now?: number;
}

const maxCards = 3;
const maxBullets = 5;

export function currentEvenChessLearningSnapshot(
  ctrl: AnalyseCtrl,
  surface: EvenChessLearningSurface,
): EvenChessLearningSnapshot {
  return {
    surface,
    contextId: surface === 'study' ? (ctrl.study?.data.id ?? ctrl.data.game.id) : ctrl.data.game.id,
    boardStateKey: ctrl.node.fen,
    ply: ctrl.node.ply,
  };
}

export function payloadHasUnsafeLearningData(payload?: EvenChessLearningPayload): boolean {
  if (!payload) return false;
  return (payload.cards ?? []).some(card =>
    Boolean(card.rawEnginePayload || card.hiddenDebugData || card.providerSecret || card.rawPrompt),
  );
}

export function payloadHasInventedLearningFacts(payload?: EvenChessLearningPayload): boolean {
  if (!payload || !(payload.sourceFacts ?? []).length) return false;
  const factIds = new Set((payload.sourceFacts ?? []).map(fact => fact.factId));
  return (payload.cards ?? []).some(
    card => !card.sourceFactIds.length || !card.sourceFactIds.every(factId => factIds.has(factId)),
  );
}

export function learningStaleReason(
  payload: EvenChessLearningPayload | undefined,
  current: EvenChessLearningSnapshot,
): string | undefined {
  if (!payload || !payload.enabled) return 'not-enabled';
  if (payload.surface !== current.surface) return 'surface-mismatch';
  if (!payload.serverAuthorized) return 'unauthorized';
  if (payload.ratedLive) return 'live-rated';
  if (payloadHasUnsafeLearningData(payload)) return 'unsafe-payload';
  if (payload.contextId !== current.contextId) return 'context-mismatch';
  if (payload.boardStateKey !== current.boardStateKey) return 'board-mismatch';
  if (payload.ply !== current.ply) return 'ply-mismatch';
  if (payload.expiresAt && (current.now ?? Date.now()) >= payload.expiresAt) return 'expired';
  if (!(payload.sourceFacts ?? []).length) return 'no-source-facts';
  if (payloadHasInventedLearningFacts(payload)) return 'invented-source-fact';
  if (!(payload.cards ?? []).length) return 'no-cards';
  return undefined;
}

export function renderableLearningCards(
  payload: EvenChessLearningPayload | undefined,
  current: EvenChessLearningSnapshot,
): EvenChessLearningCard[] {
  if (learningStaleReason(payload, current) || !payload) return [];
  const factIds = new Set((payload.sourceFacts ?? []).map(fact => fact.factId));
  return (payload.cards ?? []).filter(card => cardRenderable(card, payload, factIds)).slice(0, maxCards);
}

export function shouldRenderLearningOverlay(
  payload: EvenChessLearningPayload | undefined,
  current: EvenChessLearningSnapshot,
): boolean {
  return renderableLearningCards(payload, current).length > 0;
}

export function learningCardTtsItem(
  card: EvenChessLearningCard,
  payload: EvenChessLearningPayload,
): EvenChessTtsItem {
  return {
    id: card.id,
    surface: payload.surface,
    displayedText: shownTtsText(card.title, card.body, card.bullets ?? []),
    text: card.ttsText,
    auditId: card.auditId || payload.auditId,
    serverAuthorized: card.serverAuthorized && payload.serverAuthorized,
    approvedDisplayPayload: card.approvedDisplayPayload,
    ratedLive: Boolean(payload.ratedLive),
    rawEnginePayload: card.rawEnginePayload,
    hiddenDebugData: card.hiddenDebugData,
    providerSecret: card.providerSecret,
    rawPrompt: card.rawPrompt,
  };
}

export function renderEvenChessLearning(
  ctrl: AnalyseCtrl,
  surface: EvenChessLearningSurface,
): VNode | undefined {
  const payload = ctrl.data.evenchess?.learning;
  const ttsConfig = ctrl.data.evenchess?.tts;
  const current = currentEvenChessLearningSnapshot(ctrl, surface);
  const cards = renderableLearningCards(payload, current);

  if (!payload || !cards.length) return undefined;

  return hl(
    'section.evenchess-ai',
    {
      attrs: {
        'data-evenchess-overlay': surface,
        'data-audit-id': payload.auditId,
        'data-ply': String(payload.ply),
        role: 'region',
        'aria-live': 'polite',
        'aria-label': 'EvenChess AI coach',
      },
    },
    [
      hl('div.evenchess-ai__head', [
        hl('strong.evenchess-ai__brand', 'EvenChess AI Coach'),
        hl('span.evenchess-ai__surface', surfaceLabel(surface)),
      ]),
      ...cards.map(card =>
        hl('article.evenchess-ai__card', { attrs: { 'data-card-kind': card.kind } }, [
          hl('div.evenchess-ai__card-head', [
            hl('h2.evenchess-ai__title', card.title),
            renderTtsButton(ttsConfig, learningCardTtsItem(card, payload)),
          ]),
          hl('p.evenchess-ai__body', card.body),
          renderBullets(card.bullets ?? []),
          renderSourceFacts(payload.sourceFacts ?? [], card.sourceFactIds),
        ]),
      ),
    ],
  );
}

function renderTtsButton(config: EvenChessTtsConfig | undefined, item: EvenChessTtsItem): VNode | undefined {
  if (!shouldOfferEvenChessTts(config, item)) return undefined;
  return hl('button.evenchess-ai__tts', {
    attrs: {
      ...dataIcon(licon.Voice),
      type: 'button',
      title: 'Read aloud',
      'aria-label': 'Read EvenChess AI coach card aloud',
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

function cardRenderable(
  card: EvenChessLearningCard,
  payload: EvenChessLearningPayload,
  factIds: Set<string>,
): boolean {
  return (
    Boolean(card.id) &&
    Boolean(card.title) &&
    Boolean(card.body) &&
    card.auditId === payload.auditId &&
    card.serverAuthorized &&
    card.approvedDisplayPayload &&
    Boolean(card.sourceFactIds.length) &&
    card.sourceFactIds.every(factId => factIds.has(factId)) &&
    (card.bullets ?? []).length <= maxBullets &&
    (card.bullets ?? []).every(Boolean) &&
    !card.rawEnginePayload &&
    !card.hiddenDebugData &&
    !card.providerSecret &&
    !card.rawPrompt
  );
}

function renderBullets(bullets: string[]): VNode | undefined {
  const visibleBullets = bullets.filter(Boolean).slice(0, maxBullets);
  return visibleBullets.length
    ? hl(
        'ul.evenchess-ai__bullets',
        visibleBullets.map(bullet => hl('li', bullet)),
      )
    : undefined;
}

function renderSourceFacts(facts: EvenChessLearningSourceFact[], ids: string[]): VNode {
  const usedFacts = facts.filter(fact => ids.includes(fact.factId));
  return hl('p.evenchess-ai__source', `${usedFacts.length} server fact${usedFacts.length === 1 ? '' : 's'}`);
}

function surfaceLabel(surface: EvenChessLearningSurface): string {
  switch (surface) {
    case 'analysis':
      return 'Analysis';
    case 'study':
      return 'Study';
    case 'opening':
      return 'Opening';
  }
}
