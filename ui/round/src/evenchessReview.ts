import { type VNode, bind, hl } from 'lib/view/snabbdom';

import type RoundController from './ctrl';
import { evenChessTestGroundFullFen } from './evenchessTestGround';
import type { EvenChessLiveOverlay } from './interfaces';

type ReviewSide = 'white' | 'black';

interface ReviewQuota {
  available?: number;
  adminUnlimited?: boolean;
  adRemaining?: number;
  dailyRemaining?: number;
}

interface ReviewCardPayload {
  body?: string;
  featureKey?: string;
  title?: string;
}

interface ReviewLivePayload {
  cards?: ReviewCardPayload[];
}

interface ReviewAskAiPayload {
  position?: {
    body?: string;
    title?: string;
  };
}

interface ReviewStatePayload {
  askAi?: ReviewAskAiPayload;
  displaySide?: ReviewSide;
  ecemfStatus?: string;
  frameStatus?: string;
  gameId?: string;
  label?: string;
  live?: ReviewLivePayload;
  matchSummary?: unknown;
  ok?: boolean;
  ply?: number;
  quota?: {
    nonLiveAskAi?: ReviewQuota;
  };
  review?: {
    framesStored?: number;
    hasFullMatch?: boolean;
  };
  viewerSide?: ReviewSide;
}

interface ReviewUiState {
  askingAi: boolean;
  error?: string;
  generating: boolean;
  key: string;
  loading: boolean;
  payload?: ReviewStatePayload;
  status?: string;
  summarizing: boolean;
}

const reviewStates = new WeakMap<RoundController, ReviewUiState>();

export function renderEvenChessPostGameReviewPanel(ctrl: RoundController): VNode | undefined {
  if (ctrl.isPlaying?.()) return undefined;
  const current = reviewCurrentPosition(ctrl);
  if (!current) return undefined;

  const state = ensureReviewState(ctrl, current);
  const payload = state.payload;
  const coach = coachDisplay(payload);
  const quota = payload?.quota?.nonLiveAskAi;
  const hasEcemf = payload?.ecemfStatus === 'ready';
  const framesStored = payload?.review?.framesStored ?? 0;

  return hl(
    'section.evenchess-live__card.evenchess-live__card--post-review',
    {
      attrs: {
        role: 'region',
        'aria-label': 'EvenChess post-game coach review',
        'data-evenchess-post-review': 'true',
        'data-ply': String(current.ply),
      },
    },
    [
      hl('div.evenchess-live__review-head', [
        hl('strong.evenchess-live__label', 'EvenChess Coach Review'),
        hl('span.evenchess-live__review-meta', statusLabel(state, payload)),
      ]),
      hl('div.evenchess-live__review-actions', [
        reviewButton(hasEcemf ? 'Regenerate ECEMF' : 'Generate ECEMF', state.generating, () =>
          generateEcemf(ctrl, current),
        ),
        reviewButton('Match Summary', state.summarizing || !hasEcemf, () => matchSummary(ctrl)),
        reviewButton('Ask AI', state.askingAi || !hasEcemf || !quotaAvailable(quota), () => askAi(ctrl, current)),
      ]),
      hl('div.evenchess-live__review-amounts', [
        hl('span', `Ask AI: ${quotaText(quota)}`),
        framesStored ? hl('span', `${framesStored} ECEMF frames`) : undefined,
      ]),
      state.status ? hl('p.evenchess-live__review-status', state.status) : undefined,
      state.error ? hl('p.evenchess-live__review-error', state.error) : undefined,
      hl('div.evenchess-live__review-body', [
        state.loading && !payload ? hl('p', 'Loading review payload...') : undefined,
        !hasEcemf ? hl('p', 'Generate ECEMF to replay EvenChess coach text on this game.') : undefined,
        hasEcemf && !coach.body ? hl('p', 'No coach frame is stored for this ply yet.') : undefined,
        payload?.label ? hl('p.evenchess-live__review-label', payload.label) : undefined,
        coach.title ? hl('h3', coach.title) : undefined,
        coach.body ? hl('p', coach.body) : undefined,
        summaryText(payload?.matchSummary)
          ? hl('article.evenchess-live__review-summary', [
              hl('strong', 'Match Summary'),
              hl('p', summaryText(payload?.matchSummary)),
            ])
          : undefined,
      ]),
    ],
  );
}

function ensureReviewState(ctrl: RoundController, current = reviewCurrentPosition(ctrl)): ReviewUiState {
  const key = current ? reviewKey(ctrl, current) : `${ctrl.data.game.id}:missing`;
  let state = reviewStates.get(ctrl);
  if (!state || state.key !== key) {
    state = { key, loading: true, generating: false, summarizing: false, askingAi: false };
    reviewStates.set(ctrl, state);
    if (current) void fetchReviewState(ctrl, current, state);
  }
  return state;
}

function reviewCurrentPosition(ctrl: RoundController): { fen: string; ply: number } | undefined {
  const ply = Math.max(0, Math.min(ctrl.ply, ctrl.lastPly()));
  const fen = ctrl.stepAt(ply)?.fen;
  if (!fen) return undefined;
  return { ply, fen: evenChessTestGroundFullFen(ctrl.data, ply, fen) };
}

function reviewKey(ctrl: RoundController, current: { fen: string; ply: number }): string {
  return `${ctrl.data.game.id}:${current.ply}:${current.fen}:${viewerSide(ctrl)}`;
}

function reviewUrl(ctrl: RoundController, action: string): string {
  return `/evenchess/review/${encodeURIComponent(ctrl.data.game.id)}/${action}`;
}

async function fetchReviewState(
  ctrl: RoundController,
  current: { fen: string; ply: number },
  state = ensureReviewState(ctrl, current),
): Promise<void> {
  const url = new URL(reviewUrl(ctrl, 'state'), location.origin);
  url.searchParams.set('ply', String(current.ply));
  url.searchParams.set('fen', current.fen);
  url.searchParams.set('side', viewerSide(ctrl));
  try {
    const response = await fetch(url.pathname + url.search, {
      cache: 'no-cache',
      credentials: 'same-origin',
      headers: { accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
    });
    state.payload = response.ok ? await response.json() : undefined;
    state.error = response.ok ? undefined : 'Review state is unavailable.';
    applyStoredReviewOverlay(ctrl, state.payload);
  } catch {
    state.error = 'Review state is unavailable.';
  } finally {
    state.loading = false;
    ctrl.redraw();
  }
}

async function generateEcemf(ctrl: RoundController, current = reviewCurrentPosition(ctrl)): Promise<void> {
  const state = ensureReviewState(ctrl, current);
  if (state.generating || !current) return;
  state.generating = true;
  state.status = 'Generating ECEMF...';
  state.error = undefined;
  ctrl.redraw();
  try {
    const response = await postJson(reviewUrl(ctrl, 'ecemf'), {
      level: 10,
      result: ctrl.data.game.winner ? `${ctrl.data.game.winner}_wins` : 'unknown',
      termination: ctrl.data.game.status?.name || 'completed',
      frames: reviewFrames(ctrl),
    });
    const data = await response.json().catch(() => undefined);
    if (!response.ok || !data?.ok) throw new Error(data?.error || 'ECEMF generation failed.');
    state.status = `ECEMF ready: ${data.framesStored ?? 0} frames stored.`;
    state.key = reviewKey(ctrl, current);
    await fetchReviewState(ctrl, current, state);
  } catch (error) {
    state.error = error instanceof Error ? error.message : 'ECEMF generation failed.';
  } finally {
    state.generating = false;
    ctrl.redraw();
  }
}

async function matchSummary(ctrl: RoundController): Promise<void> {
  const current = reviewCurrentPosition(ctrl);
  const state = ensureReviewState(ctrl, current);
  if (state.summarizing || !current) return;
  state.summarizing = true;
  state.status = 'Creating match summary...';
  state.error = undefined;
  ctrl.redraw();
  try {
    const response = await postJson(reviewUrl(ctrl, 'match-summary'), {
      side: state.payload?.viewerSide ?? viewerSide(ctrl),
    });
    const data = await response.json().catch(() => undefined);
    if (!response.ok || !data?.ok) throw new Error(data?.error || 'Match summary failed.');
    state.status = 'Match summary ready.';
    state.key = reviewKey(ctrl, current);
    await fetchReviewState(ctrl, current, state);
  } catch (error) {
    state.error = error instanceof Error ? error.message : 'Match summary failed.';
  } finally {
    state.summarizing = false;
    ctrl.redraw();
  }
}

async function askAi(ctrl: RoundController, current = reviewCurrentPosition(ctrl)): Promise<void> {
  const state = ensureReviewState(ctrl, current);
  if (state.askingAi || !current) return;
  state.askingAi = true;
  state.status = 'Asking AI...';
  state.error = undefined;
  ctrl.redraw();
  try {
    const response = await postJson(reviewUrl(ctrl, 'non-live-ask-ai'), {
      ply: current.ply,
      fen: current.fen,
      viewerSide: state.payload?.viewerSide ?? viewerSide(ctrl),
      displaySide: state.payload?.displaySide,
    });
    const data = await response.json().catch(() => undefined);
    if (!response.ok || !data?.ok) throw new Error(data?.error || 'Ask AI failed.');
    state.status = 'Ask AI ready.';
    state.key = reviewKey(ctrl, current);
    await fetchReviewState(ctrl, current, state);
  } catch (error) {
    state.error = error instanceof Error ? error.message : 'Ask AI failed.';
  } finally {
    state.askingAi = false;
    ctrl.redraw();
  }
}

function reviewFrames(ctrl: RoundController): Array<{ fen: string; moveUci?: string; ply: number }> {
  const frames: Array<{ fen: string; moveUci?: string; ply: number }> = [];
  for (let ply = 0; ply <= ctrl.lastPly(); ply++) {
    const step = ctrl.stepAt(ply);
    if (!step?.fen) continue;
    frames.push({
      ply,
      fen: evenChessTestGroundFullFen(ctrl.data, ply, step.fen),
      moveUci: step.uci,
    });
  }
  return frames;
}

function postJson(url: string, body: unknown): Promise<Response> {
  return fetch(url, {
    method: 'POST',
    cache: 'no-cache',
    credentials: 'same-origin',
    headers: {
      accept: 'application/json',
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
    },
    body: JSON.stringify(body),
  });
}

function reviewButton(label: string, disabled: boolean, action: () => void): VNode {
  return hl(
    'button.evenchess-live__review-button',
    {
      attrs: { type: 'button', disabled },
      hook: bind(
        'click',
        (event: Event) => {
          event.preventDefault();
          event.stopPropagation();
          if (!disabled) action();
        },
        undefined,
        false,
      ),
    },
    label,
  );
}

function applyStoredReviewOverlay(ctrl: RoundController, payload: ReviewStatePayload | undefined): void {
  const live = payload?.live;
  if (isLiveOverlay(live)) ctrl.applyEvenChessLiveOverlay(live);
}

function isLiveOverlay(value: unknown): value is EvenChessLiveOverlay {
  if (!value || typeof value !== 'object') return false;
  const overlay = value as EvenChessLiveOverlay;
  return (
    overlay.enabled === true &&
    typeof overlay.gameId === 'string' &&
    typeof overlay.ply === 'number' &&
    typeof overlay.boardStateKey === 'string' &&
    (overlay.perspective === 'white' || overlay.perspective === 'black') &&
    overlay.serverAuthorized === true &&
    Array.isArray(overlay.cards) &&
    Array.isArray(overlay.visuals)
  );
}

function viewerSide(ctrl: RoundController): ReviewSide {
  return ctrl.data.player.color === 'black' ? 'black' : 'white';
}

export function coachDisplay(payload?: ReviewStatePayload): { body?: string; title?: string } {
  const askAi = payload?.askAi?.position;
  if (askAi?.body) return { title: askAi.title || 'Ask AI', body: askAi.body };
  const cards = payload?.live?.cards ?? [];
  const summary =
    cards.find(card => card.featureKey === 'coachText' || card.title?.toLowerCase().includes('summary')) ??
    cards[0];
  return { title: summary?.title, body: summary?.body };
}

export function summaryText(summary: unknown): string | undefined {
  if (!summary || typeof summary !== 'object') return undefined;
  const record = summary as Record<string, unknown>;
  const direct = record.summary_text ?? record.summaryText ?? record.body;
  if (typeof direct === 'string') return direct;
  for (const key of ['match_summary', 'summary']) {
    const nested = record[key];
    if (nested && typeof nested === 'object') {
      const nestedText = (nested as Record<string, unknown>).summary_text;
      if (typeof nestedText === 'string') return nestedText;
    }
  }
  return undefined;
}

export function quotaAvailable(quota?: ReviewQuota): boolean {
  return Boolean(quota?.adminUnlimited || (quota?.available ?? 0) > 0);
}

export function quotaText(quota?: ReviewQuota): string {
  if (!quota) return 'loading';
  if (quota.adminUnlimited) return 'Unlimited';
  return `${quota.available ?? 0} available`;
}

function statusLabel(state: ReviewUiState, payload?: ReviewStatePayload): string {
  if (state.loading && !payload) return 'Loading';
  if (payload?.ecemfStatus === 'ready') return 'ECEMF ready';
  return 'ECEMF missing';
}
