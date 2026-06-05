import type { EvenChessTtsConfig, EvenChessTtsItem } from 'lib/evenchessTts';
import { shouldOfferEvenChessTts, shownTtsText, speakEvenChessTts } from 'lib/evenchessTts';
import * as licon from 'lib/licon';

import type { EvenChessOpeningAiCard, EvenChessOpeningAiPayload, OpeningPage } from './interfaces';

const maxCards = 3;
const maxBullets = 5;

export function openingPayloadHasUnsafeAiData(payload?: EvenChessOpeningAiPayload): boolean {
  if (!payload) return false;
  return (payload.cards ?? []).some(card =>
    Boolean(card.rawEnginePayload || card.hiddenDebugData || card.providerSecret || card.rawPrompt),
  );
}

export function openingPayloadHasInventedFacts(payload?: EvenChessOpeningAiPayload): boolean {
  if (!payload || !(payload.sourceFacts ?? []).length) return false;
  const factIds = new Set((payload.sourceFacts ?? []).map(fact => fact.factId));
  return (payload.cards ?? []).some(
    card => !card.sourceFactIds.length || !card.sourceFactIds.every(factId => factIds.has(factId)),
  );
}

export function openingAiStaleReason(
  payload: EvenChessOpeningAiPayload | undefined,
  now: number = Date.now(),
): string | undefined {
  if (!payload || !payload.enabled) return 'not-enabled';
  if (payload.surface !== 'opening') return 'surface-mismatch';
  if (!payload.serverAuthorized) return 'unauthorized';
  if (payload.ratedLive) return 'live-rated';
  if (openingPayloadHasUnsafeAiData(payload)) return 'unsafe-payload';
  if (!payload.contextId || !payload.boardStateKey || payload.ply < 0) return 'missing-context';
  if (payload.expiresAt && now >= payload.expiresAt) return 'expired';
  if (!(payload.sourceFacts ?? []).length) return 'no-source-facts';
  if (openingPayloadHasInventedFacts(payload)) return 'invented-source-fact';
  if (!(payload.cards ?? []).length) return 'no-cards';
  return undefined;
}

export function renderableOpeningAiCards(
  payload: EvenChessOpeningAiPayload | undefined,
  now: number = Date.now(),
): EvenChessOpeningAiCard[] {
  if (openingAiStaleReason(payload, now) || !payload) return [];
  const factIds = new Set((payload.sourceFacts ?? []).map(fact => fact.factId));
  return (payload.cards ?? []).filter(card => cardRenderable(card, payload, factIds)).slice(0, maxCards);
}

export function shouldRenderOpeningAi(
  payload: EvenChessOpeningAiPayload | undefined,
  now: number = Date.now(),
): boolean {
  return renderableOpeningAiCards(payload, now).length > 0;
}

export function openingCardTtsItem(
  card: EvenChessOpeningAiCard,
  payload: EvenChessOpeningAiPayload,
): EvenChessTtsItem {
  return {
    id: card.id,
    surface: 'opening',
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

export function renderEvenChessOpeningAi(data: OpeningPage, root: ParentNode = document): boolean {
  const payload = data.evenchess?.openingAi;
  const ttsConfig = data.evenchess?.tts;
  const cards = renderableOpeningAiCards(payload);
  const target = root.querySelector<HTMLElement>('.opening__intro__content');

  root.querySelector('.opening__evenchess-ai')?.remove();
  if (!payload || !cards.length || !target) return false;

  target.appendChild(section(payload, cards, ttsConfig));
  return true;
}

function cardRenderable(
  card: EvenChessOpeningAiCard,
  payload: EvenChessOpeningAiPayload,
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

function section(
  payload: EvenChessOpeningAiPayload,
  cards: EvenChessOpeningAiCard[],
  ttsConfig: EvenChessTtsConfig | undefined,
): HTMLElement {
  const node = document.createElement('section');
  node.className = 'opening__evenchess-ai';
  node.dataset['evenchessOverlay'] = 'opening';
  node.dataset['auditId'] = payload.auditId;
  node.setAttribute('role', 'region');
  node.setAttribute('aria-live', 'polite');
  node.setAttribute('aria-label', 'EvenChess opening AI coach');

  const header = document.createElement('div');
  header.className = 'opening__evenchess-ai__head';

  const brand = document.createElement('strong');
  brand.className = 'opening__evenchess-ai__brand';
  brand.textContent = 'EvenChess AI Coach';

  const label = document.createElement('span');
  label.className = 'opening__evenchess-ai__surface';
  label.textContent = 'Opening explorer';

  header.append(brand, label);
  node.appendChild(header);

  cards.forEach(card => node.appendChild(cardNode(card, payload, ttsConfig)));
  return node;
}

function cardNode(
  card: EvenChessOpeningAiCard,
  payload: EvenChessOpeningAiPayload,
  ttsConfig: EvenChessTtsConfig | undefined,
): HTMLElement {
  const article = document.createElement('article');
  article.className = 'opening__evenchess-ai__card';
  article.dataset['cardKind'] = card.kind;

  const header = document.createElement('div');
  header.className = 'opening__evenchess-ai__card-head';

  const title = document.createElement('h2');
  title.className = 'opening__evenchess-ai__title';
  title.textContent = card.title;

  header.appendChild(title);
  appendTtsButton(header, ttsConfig, openingCardTtsItem(card, payload));

  const body = document.createElement('p');
  body.className = 'opening__evenchess-ai__body';
  body.textContent = card.body;

  article.append(header, body);

  const bullets = (card.bullets ?? []).filter(Boolean).slice(0, maxBullets);
  if (bullets.length) {
    const list = document.createElement('ul');
    list.className = 'opening__evenchess-ai__bullets';
    bullets.forEach(bullet => {
      const item = document.createElement('li');
      item.textContent = bullet;
      list.appendChild(item);
    });
    article.appendChild(list);
  }

  const source = document.createElement('p');
  source.className = 'opening__evenchess-ai__source';
  source.textContent = `${card.sourceFactIds.length} server fact${card.sourceFactIds.length === 1 ? '' : 's'}`;
  article.appendChild(source);

  return article;
}

function appendTtsButton(
  parent: HTMLElement,
  config: EvenChessTtsConfig | undefined,
  item: EvenChessTtsItem,
): void {
  if (!shouldOfferEvenChessTts(config, item)) return;

  const button = document.createElement('button');
  button.className = 'opening__evenchess-ai__tts';
  button.type = 'button';
  button.title = 'Read aloud';
  button.setAttribute('aria-label', 'Read EvenChess opening coach card aloud');
  button.setAttribute('data-icon', licon.Voice);
  button.addEventListener('click', event => {
    event.preventDefault();
    event.stopPropagation();
    speakEvenChessTts(config, item);
  });
  parent.appendChild(button);
}
