import { type VNode, hl } from 'lib/view';

import type AnalyseCtrl from '../ctrl';

type ReviewSide = 'white' | 'black';

interface ReviewQuota {
  available?: number;
  dailyRemaining?: number;
  adRemaining?: number;
  adminUnlimited?: boolean;
}

interface ReviewCardPayload {
  title?: string;
  body?: string;
  featureKey?: string;
}

interface ReviewLivePayload {
  cards?: ReviewCardPayload[];
}

interface ReviewAskAiPayload {
  position?: {
    title?: string;
    body?: string;
  };
}

interface ReviewStatePayload {
  ok?: boolean;
  gameId?: string;
  ply?: number;
  viewerSide?: ReviewSide;
  displaySide?: ReviewSide;
  label?: string;
  ecemfStatus?: string;
  frameStatus?: string;
  live?: ReviewLivePayload;
  askAi?: ReviewAskAiPayload;
  matchSummary?: any;
  quota?: {
    nonLiveAskAi?: ReviewQuota;
  };
  review?: {
    framesStored?: number;
    hasFullMatch?: boolean;
  };
}

interface ReviewUiState {
  key: string;
  loading: boolean;
  error?: string;
  status?: string;
  payload?: ReviewStatePayload;
  generating: boolean;
  summarizing: boolean;
  askingAi: boolean;
}

const reviewStates = new WeakMap<AnalyseCtrl, ReviewUiState>();

export function renderEvenChessReview(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.synthetic || ctrl.study) return undefined;
  const state = ensureReviewState(ctrl);
  const payload = state.payload;
  const coach = coachDisplay(payload);
  const quota = payload?.quota?.nonLiveAskAi;
  const hasEcemf = payload?.ecemfStatus === 'ready';

  return hl(
    'section.evenchess-review',
    {
      attrs: {
        role: 'region',
        'aria-label': 'EvenChess post-game coach review',
        'data-ply': String(ctrl.node.ply),
      },
    },
    [
      hl('div.evenchess-review__head', [
        hl('div', [
          hl('strong.evenchess-review__brand', 'EvenChess Coach Review'),
          hl('span.evenchess-review__subhead', payload?.label ?? 'Post-game review'),
        ]),
        hl('span.evenchess-review__meta', statusLabel(state, payload)),
      ]),
      hl('div.evenchess-review__actions', [
        actionButton(hasEcemf ? 'Regenerate ECEMF' : 'Generate ECEMF', state.generating, () => generateEcemf(ctrl), 'evenchess-review__button--secondary'),
        actionButton('Match Summary', state.summarizing || !hasEcemf, () => matchSummary(ctrl), ''),
        actionButton('Ask AI', state.askingAi || !hasEcemf || !quotaAvailable(quota), () => askAi(ctrl), ''),
      ]),
      hl('div.evenchess-review__quota', [
        hl('span', `Ask AI: ${quotaText(quota)}`),
        payload?.review?.framesStored ? hl('span', `${payload.review.framesStored} ECEMF frames`) : undefined,
      ]),
      state.status ? hl('p.evenchess-review__status', state.status) : undefined,
      state.error ? hl('p.evenchess-review__error', state.error) : undefined,
      hl('div.evenchess-review__body', [
        state.loading && !payload ? hl('p', 'Loading review payload...') : undefined,
        !hasEcemf ? hl('p', 'Generate ECEMF to replay EvenChess coach text on this game.') : undefined,
        hasEcemf && !coach.body ? hl('p', 'No coach frame is stored for this ply yet.') : undefined,
        coach.title ? hl('h3', coach.title) : undefined,
        coach.body ? hl('p', coach.body) : undefined,
        summaryText(payload?.matchSummary) ? hl('article.evenchess-review__summary', [hl('strong', 'Match Summary'), hl('p', summaryText(payload?.matchSummary))]) : undefined,
      ]),
    ],
  );
}

function ensureReviewState(ctrl: AnalyseCtrl): ReviewUiState {
  const key = reviewKey(ctrl);
  let state = reviewStates.get(ctrl);
  if (!state || state.key !== key) {
    state = { key, loading: true, generating: false, summarizing: false, askingAi: false };
    reviewStates.set(ctrl, state);
    void fetchReviewState(ctrl, state);
  }
  return state;
}

function reviewKey(ctrl: AnalyseCtrl): string {
  return `${ctrl.data.game.id}:${ctrl.node.ply}:${ctrl.node.fen}:${ctrl.bottomColor()}`;
}

function reviewUrl(ctrl: AnalyseCtrl, action: string): string {
  return `/evenchess/review/${encodeURIComponent(ctrl.data.game.id)}/${action}`;
}

async function fetchReviewState(ctrl: AnalyseCtrl, state = ensureReviewState(ctrl)): Promise<void> {
  const url = new URL(reviewUrl(ctrl, 'state'), location.origin);
  url.searchParams.set('ply', String(ctrl.node.ply));
  url.searchParams.set('fen', ctrl.node.fen);
  url.searchParams.set('side', ctrl.bottomColor());
  try {
    const response = await fetch(url.pathname + url.search, {
      cache: 'no-cache',
      credentials: 'same-origin',
      headers: { accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
    });
    state.payload = response.ok ? await response.json() : undefined;
    state.error = response.ok ? undefined : 'Review state is unavailable.';
  } catch {
    state.error = 'Review state is unavailable.';
  } finally {
    state.loading = false;
    ctrl.redraw();
  }
}

async function generateEcemf(ctrl: AnalyseCtrl): Promise<void> {
  const state = ensureReviewState(ctrl);
  if (state.generating) return;
  state.generating = true;
  state.status = 'Generating ECEMF...';
  state.error = undefined;
  ctrl.redraw();
  try {
    const response = await postJson(reviewUrl(ctrl, 'ecemf'), {
      level: 10,
      result: ctrl.data.game.winner ? `${ctrl.data.game.winner}_wins` : 'unknown',
      termination: 'completed',
      frames: ctrl.mainline
        .filter(node => Boolean(node.fen))
        .map(node => ({
          ply: node.ply,
          fen: node.fen,
          moveUci: node.uci,
        })),
    });
    const data = await response.json().catch(() => undefined);
    if (!response.ok || !data?.ok) throw new Error(data?.error || 'ECEMF generation failed.');
    state.status = `ECEMF ready: ${data.framesStored ?? 0} frames stored.`;
    state.key = reviewKey(ctrl);
    await fetchReviewState(ctrl, state);
  } catch (error) {
    state.error = error instanceof Error ? error.message : 'ECEMF generation failed.';
  } finally {
    state.generating = false;
    ctrl.evenChessUniversalOverlay?.refresh();
    ctrl.redraw();
  }
}

async function matchSummary(ctrl: AnalyseCtrl): Promise<void> {
  const state = ensureReviewState(ctrl);
  if (state.summarizing) return;
  state.summarizing = true;
  state.status = 'Creating match summary...';
  state.error = undefined;
  ctrl.redraw();
  try {
    const response = await postJson(reviewUrl(ctrl, 'match-summary'), {
      side: state.payload?.viewerSide ?? ctrl.bottomColor(),
    });
    const data = await response.json().catch(() => undefined);
    if (!response.ok || !data?.ok) throw new Error(data?.error || 'Match summary failed.');
    state.status = 'Match summary ready.';
    state.key = reviewKey(ctrl);
    await fetchReviewState(ctrl, state);
  } catch (error) {
    state.error = error instanceof Error ? error.message : 'Match summary failed.';
  } finally {
    state.summarizing = false;
    ctrl.redraw();
  }
}

async function askAi(ctrl: AnalyseCtrl): Promise<void> {
  const state = ensureReviewState(ctrl);
  if (state.askingAi) return;
  state.askingAi = true;
  state.status = 'Asking AI...';
  state.error = undefined;
  ctrl.redraw();
  try {
    const response = await postJson(reviewUrl(ctrl, 'non-live-ask-ai'), {
      ply: ctrl.node.ply,
      fen: ctrl.node.fen,
      viewerSide: state.payload?.viewerSide ?? ctrl.bottomColor(),
      displaySide: state.payload?.displaySide,
    });
    const data = await response.json().catch(() => undefined);
    if (!response.ok || !data?.ok) throw new Error(data?.error || 'Ask AI failed.');
    state.status = 'Ask AI ready.';
    state.key = reviewKey(ctrl);
    await fetchReviewState(ctrl, state);
  } catch (error) {
    state.error = error instanceof Error ? error.message : 'Ask AI failed.';
  } finally {
    state.askingAi = false;
    ctrl.redraw();
  }
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

function actionButton(label: string, disabled: boolean, action: () => void, extraClass: string): VNode {
  return hl(
    `button.evenchess-review__button${extraClass ? `.${extraClass}` : ''}`,
    {
      attrs: { type: 'button', disabled },
      on: {
        click: (event: Event) => {
          event.preventDefault();
          if (!disabled) action();
        },
      },
    },
    label,
  );
}

export function coachDisplay(payload?: ReviewStatePayload): { title?: string; body?: string } {
  const askAi = payload?.askAi?.position;
  if (askAi?.body) return { title: askAi.title || 'Ask AI', body: askAi.body };
  const cards = payload?.live?.cards ?? [];
  const summary = cards.find(card => card.featureKey === 'coachText' || card.title?.toLowerCase().includes('summary')) ?? cards[0];
  return { title: summary?.title, body: summary?.body };
}

export function summaryText(summary: any): string | undefined {
  if (!summary) return undefined;
  return (
    summary?.summary_text ||
    summary?.summaryText ||
    summary?.match_summary?.summary_text ||
    summary?.summary?.summary_text ||
    summary?.body
  );
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
