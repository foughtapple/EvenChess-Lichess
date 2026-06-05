import type RoundController from './ctrl';
import type {
  EvenChessLiveOverlay,
  EvenChessPotentialMoveKind,
  EvenChessPotentialMoveReveal,
  EvenChessProposedMoveCard,
  EvenChessTestGroundState,
  RoundData,
} from './interfaces';

export interface TestGroundLocation {
  origin: string;
  protocol: string;
  hostname: string;
}

interface OverlayRequestState {
  key?: string;
  requestedAt?: number;
  inFlight?: boolean;
  queued?: boolean;
  queuedContext?: OverlayRequestContext;
  queuedForce?: boolean;
  missingVisualRetries?: number;
}

const endpointPath = '/evenchess/testground/ece/board-overlay';
const gameHistoryEndpointPath = '/evenchess/testground/ece/game-history';
const fullGameReviewEndpointPath = '/evenchess/testground/ece/full-game-review';
const proposedMoveEndpointPath = '/evenchess/testground/ece/proposed-move';
const potentialMoveEndpointPath = '/evenchess/testground/ece/potential-move';
const dockerHostEceBaseUrl = 'http://host.docker.internal:8787';
const defaultLevel = 10;
const defaultTtlMillis = 60_000;
const samePositionRetryMillis = 5_000;
const queuedPositionRetryMillis = 100;
const missingVisualRetryMillis = 1_500;
const requestTimeoutMillis = 15_000;
const stateByController = new WeakMap<RoundController, OverlayRequestState>();
interface OverlayRequestContext {
  gameId?: string;
  ply?: number;
  fen?: string;
  level?: number;
  historyOnly?: boolean;
}

export interface EvenChessProposedMoveFetchResult {
  card?: EvenChessProposedMoveCard;
  error?: string;
  consumed?: number;
  quota?: number;
}

export interface EvenChessPotentialMoveFetchResult {
  reveal?: EvenChessPotentialMoveReveal;
  error?: string;
}

export interface EvenChessFullGameReviewFetchResult {
  ok?: boolean;
  framesStored?: number;
  error?: string;
}

export function shouldUseEvenChessTestGround(_location: TestGroundLocation, data: RoundData): boolean {
  return !data.player.spectator;
}

export function evenChessTestGroundFullFen(_data: RoundData, ply: number, fen: string): string {
  const trimmed = fen.trim();
  if (isFullFen(trimmed) || !isBoardPlacementFen(trimmed)) return trimmed;

  const activeColor = ply % 2 === 0 ? 'w' : 'b';
  const castlingRights = inferInitialCastlingRights(trimmed);
  const fullMoveNumber = Math.max(1, Math.floor(ply / 2) + 1);
  return `${trimmed} ${activeColor} ${castlingRights || '-'} - 0 ${fullMoveNumber}`;
}

export function testGroundOverlayUrl(
  origin: string,
  data: RoundData,
  ply: number,
  fen: string,
  level: number = defaultLevel,
  ttlMillis: number = defaultTtlMillis,
  historyOnly: boolean = false,
): string {
  const url = new URL(endpointPath, origin);
  const fullFen = evenChessTestGroundFullFen(data, ply, fen);
  url.searchParams.set('gameId', data.game.id);
  url.searchParams.set('ply', String(ply));
  url.searchParams.set('fen', fullFen);
  url.searchParams.set('side', data.player.color);
  url.searchParams.set('level', String(level));
  url.searchParams.set('whiteLevel', String(level));
  url.searchParams.set('blackLevel', String(level));
  url.searchParams.set('ttlMillis', String(ttlMillis));
  url.searchParams.set('playerId', data.player.user?.id ?? 'local-test-player');
  url.searchParams.set('eceBaseUrl', dockerHostEceBaseUrl);
  if (historyOnly) url.searchParams.set('historyOnly', '1');
  return `${url.pathname}${url.search}`;
}

export function testGroundProposedMoveUrl(
  origin: string,
  data: RoundData,
  ply: number,
  fen: string,
  moveUci: string,
  level: number = defaultLevel,
  proposalIndex: number = 1,
): string {
  const url = new URL(proposedMoveEndpointPath, origin);
  const fullFen = evenChessTestGroundFullFen(data, ply, fen);
  url.searchParams.set('gameId', data.game.id);
  url.searchParams.set('ply', String(ply));
  url.searchParams.set('fen', fullFen);
  url.searchParams.set('side', data.player.color);
  url.searchParams.set('level', String(level));
  url.searchParams.set('whiteLevel', String(level));
  url.searchParams.set('blackLevel', String(level));
  url.searchParams.set('moveUci', moveUci);
  url.searchParams.set('proposalIndex', String(proposalIndex));
  url.searchParams.set('playerId', data.player.user?.id ?? 'local-test-player');
  url.searchParams.set('eceBaseUrl', dockerHostEceBaseUrl);
  return `${url.pathname}${url.search}`;
}

export function testGroundPotentialMoveUrl(
  origin: string,
  data: RoundData,
  ply: number,
  fen: string,
  kind: EvenChessPotentialMoveKind,
  level: number = defaultLevel,
): string {
  const url = new URL(potentialMoveEndpointPath, origin);
  const fullFen = evenChessTestGroundFullFen(data, ply, fen);
  url.searchParams.set('gameId', data.game.id);
  url.searchParams.set('ply', String(ply));
  url.searchParams.set('fen', fullFen);
  url.searchParams.set('side', data.player.color);
  url.searchParams.set('level', String(level));
  url.searchParams.set('whiteLevel', String(level));
  url.searchParams.set('blackLevel', String(level));
  url.searchParams.set('kind', kind);
  url.searchParams.set('playerId', data.player.user?.id ?? 'local-test-player');
  url.searchParams.set('eceBaseUrl', dockerHostEceBaseUrl);
  return `${url.pathname}${url.search}`;
}

export function testGroundGameHistoryUrl(origin: string, data: RoundData): string {
  const url = new URL(gameHistoryEndpointPath, origin);
  url.searchParams.set('gameId', data.game.id);
  url.searchParams.set('side', data.player.color);
  url.searchParams.set('playerId', data.player.user?.id ?? 'local-test-player');
  return `${url.pathname}${url.search}`;
}

export function testGroundFullGameReviewUrl(origin: string): string {
  const url = new URL(fullGameReviewEndpointPath, origin);
  return `${url.pathname}${url.search}`;
}

export function requestEvenChessTestGroundOverlay(ctrl: RoundController, force: boolean = false): void {
  return requestEvenChessTestGroundOverlayForPosition(ctrl, force, {});
}

export function requestEvenChessTestGroundOverlayForPosition(
  ctrl: RoundController,
  force: boolean = false,
  context: OverlayRequestContext = {},
): void {
  if (!shouldUseEvenChessTestGround(location, ctrl.data)) return;

  const ply = context.ply ?? ctrl.ply;
  const fen = evenChessTestGroundFullFen(ctrl.data, ply, context.fen ?? ctrl.stepAt(ply).fen);
  const level = context.level ?? requestedEvenChessLevel(ctrl.data);
  if (!fen) return;
  const requestContext: OverlayRequestContext = {
    gameId: context.gameId ?? ctrl.data.game.id,
    ply,
    fen,
    level,
    historyOnly: context.historyOnly ?? ctrl.replaying(),
  };
  const key = `${requestContext.gameId}:${ply}:${ctrl.data.player.color}:L${level}:${requestContext.historyOnly ? 'history' : 'live'}:${fen}`;
  const state = stateByController.get(ctrl) ?? {};
  const now = Date.now();
  const samePosition = state.key === key;
  if (state.inFlight) {
    if (!force && samePosition) return;
    state.queued = true;
    state.queuedContext = requestContext;
    state.queuedForce = Boolean(state.queuedForce || force);
    stateByController.set(ctrl, state);
    return;
  }
  if (!force && samePosition && state.requestedAt && now - state.requestedAt < samePositionRetryMillis)
    return;

  if (!samePosition) state.missingVisualRetries = 0;
  state.key = key;
  state.requestedAt = now;
  state.inFlight = true;
  state.queued = false;
  state.queuedForce = false;
  stateByController.set(ctrl, state);
  setTestGroundState(ctrl, testGroundState('loading', 'Checking ECE', now, level));

  let success = false;
  let retryMissingVisuals = false;
  void fetchOverlay(testGroundOverlayUrl(location.origin, ctrl.data, ply, fen, level, defaultTtlMillis, requestContext.historyOnly))
    .then(overlay => {
      if (!overlay) {
        setTestGroundState(ctrl, testGroundState('unavailable', 'ECE unavailable', now, level));
        return;
      }
      const currentPly = ctrl.ply;
      const currentFen = evenChessTestGroundFullFen(ctrl.data, currentPly, ctrl.stepAt(currentPly).fen);
      if (
        requestContext.gameId !== overlay.gameId ||
        requestContext.ply !== overlay.ply ||
        requestContext.fen !== overlay.boardStateKey ||
        currentPly !== requestContext.ply ||
        currentFen !== requestContext.fen
      ) {
        state.queued = true;
        state.queuedForce = true;
        return;
      }
      const hasDisplayPayload = Boolean((overlay.cards ?? []).length || (overlay.visuals ?? []).length);
      if (hasDisplayPayload || level === 0) state.missingVisualRetries = 0;
      else {
        state.missingVisualRetries = (state.missingVisualRetries ?? 0) + 1;
        retryMissingVisuals = true;
        setTestGroundState(ctrl, testGroundState('loading', 'Waiting for ECE payload', now, level));
        return;
      }
      success = true;
      setTestGroundState(ctrl, testGroundState('ready', `Level ${level} test payload`, now, level));
      ctrl.applyEvenChessLiveOverlay(overlay);
    })
    .catch(() => setTestGroundState(ctrl, testGroundState('unavailable', 'ECE unavailable', now, level)))
    .finally(() => {
      state.inFlight = false;
      const shouldRetrySoon = state.queued;
      const queuedForce = state.queuedForce;
      const queuedContext = state.queuedContext;
      state.queued = false;
      state.queuedContext = undefined;
      state.queuedForce = false;
      if (shouldRetrySoon)
        setTimeout(() => {
          if (queuedContext) requestEvenChessTestGroundOverlayForPosition(ctrl, queuedForce, queuedContext);
          else requestEvenChessTestGroundOverlay(ctrl, queuedForce);
        }, queuedPositionRetryMillis);
      else if (retryMissingVisuals)
        setTimeout(() => requestEvenChessTestGroundOverlay(ctrl, true), missingVisualRetryMillis);
      else if (!success) setTimeout(() => requestEvenChessTestGroundOverlay(ctrl), samePositionRetryMillis);
    });
}

export async function requestEvenChessTestGroundProposedMove(
  data: RoundData,
  ply: number,
  fen: string,
  moveUci: string,
  level: number,
  proposalIndex: number = 1,
): Promise<EvenChessProposedMoveFetchResult> {
  if (!shouldUseEvenChessTestGround(location, data)) return { error: 'Proposed Move unavailable here' };

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), requestTimeoutMillis);
  try {
    const response = await fetch(
      testGroundProposedMoveUrl(location.origin, data, ply, fen, moveUci, level, proposalIndex),
      {
        method: 'POST',
        credentials: 'same-origin',
        headers: { accept: 'application/json' },
        signal: controller.signal,
      },
    );
    const payload: unknown = await response.json().catch(() => undefined);
    if (!response.ok) return { error: proposedMoveErrorMessage(payload) ?? 'Proposed Move unavailable' };
    const card = proposedMoveFromPayload(payload);
    return card
      ? { card, consumed: numberPayloadField(payload, 'consumed'), quota: numberPayloadField(payload, 'quota') }
      : { error: 'ECE returned no proposed-move preview' };
  } catch {
    return { error: 'ECE proposed move unavailable' };
  } finally {
    clearTimeout(timeout);
  }
}

export async function requestEvenChessTestGroundPotentialMoves(
  data: RoundData,
  ply: number,
  fen: string,
  kind: EvenChessPotentialMoveKind,
  level: number,
): Promise<EvenChessPotentialMoveFetchResult> {
  if (!shouldUseEvenChessTestGround(location, data)) return { error: 'Potential Moves unavailable here' };

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), requestTimeoutMillis);
  try {
    const response = await fetch(testGroundPotentialMoveUrl(location.origin, data, ply, fen, kind, level), {
      method: 'POST',
      credentials: 'same-origin',
      headers: { accept: 'application/json' },
      signal: controller.signal,
    });
    const payload: unknown = await response.json().catch(() => undefined);
    if (!response.ok) return { error: proposedMoveErrorMessage(payload) ?? 'Potential Moves unavailable' };
    const reveal = potentialMoveRevealFromPayload(payload);
    return reveal ? { reveal } : { error: 'ECE returned no potential moves' };
  } catch {
    return { error: 'ECE potential moves unavailable' };
  } finally {
    clearTimeout(timeout);
  }
}

export async function requestEvenChessTestGroundFullGameReview(
  ctrl: RoundController,
  level: number = defaultLevel,
): Promise<EvenChessFullGameReviewFetchResult> {
  if (!shouldUseEvenChessTestGround(location, ctrl.data)) return { error: 'Full-game review unavailable here' };

  const now = Date.now();
  setTestGroundState(ctrl, testGroundState('loading', 'Running L10 review', now, level));
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), requestTimeoutMillis * 4);
  try {
    const frames = [];
    for (let ply = 0; ply <= ctrl.lastPly(); ply++) {
      const step = ctrl.stepAt(ply);
      if (!step?.fen) continue;
      frames.push({
        ply,
        fen: evenChessTestGroundFullFen(ctrl.data, ply, step.fen),
        moveUci: step.uci,
      });
    }
    const response = await fetch(testGroundFullGameReviewUrl(location.origin), {
      method: 'POST',
      credentials: 'same-origin',
      cache: 'no-store',
      headers: { accept: 'application/json', 'content-type': 'application/json' },
      signal: controller.signal,
      body: JSON.stringify({
        gameId: ctrl.data.game.id,
        playerId: ctrl.data.player.user?.id ?? 'local-test-player',
        side: ctrl.data.player.color,
        level,
        frames,
        eceBaseUrl: dockerHostEceBaseUrl,
      }),
    });
    const payload: unknown = await response.json().catch(() => undefined);
    if (!response.ok) {
      const error = proposedMoveErrorMessage(payload) ?? 'Full-game review unavailable';
      setTestGroundState(ctrl, testGroundState('unavailable', error, now, level));
      return { error };
    }
    const framesStored = numberPayloadField(payload, 'framesStored') ?? 0;
    setTestGroundState(ctrl, testGroundState('ready', `Stored ${framesStored} review frames`, now, level));
    requestEvenChessTestGroundOverlay(ctrl, true);
    return { ok: true, framesStored };
  } catch {
    setTestGroundState(ctrl, testGroundState('unavailable', 'Full-game review unavailable', now, level));
    return { error: 'Full-game review unavailable' };
  } finally {
    clearTimeout(timeout);
  }
}

function setTestGroundState(ctrl: RoundController, testGround: EvenChessTestGroundState): void {
  ctrl.data.evenchess = {
    ...ctrl.data.evenchess,
    testGround,
  };
  ctrl.redraw();
}

function testGroundState(
  status: EvenChessTestGroundState['status'],
  message: string,
  requestedAt: number,
  level: number = defaultLevel,
): EvenChessTestGroundState {
  return {
    enabled: true,
    level,
    status,
    message,
    requestedAt,
    updatedAt: Date.now(),
  };
}

function requestedEvenChessLevel(data: RoundData): number {
  const display = data.evenchess?.display;
  const rawLevel =
    display && display.usedLevel !== undefined
      ? display.usedLevel
      : (display?.preferredUsedLevel ??
        data.pref?.evenchess?.preferredUsedLevel ??
        data.pref?.evenchess?.defaultSetLevel ??
        data.evenchess?.testGround?.level ??
        defaultLevel);
  const setLevel = normalizeLevel(display?.setLevel ?? defaultLevel, defaultLevel);
  return Math.min(normalizeLevel(rawLevel, defaultLevel), setLevel);
}

function normalizeLevel(rawLevel: number | undefined, fallback: number): number {
  const level = typeof rawLevel === 'number' && Number.isFinite(rawLevel) ? rawLevel : fallback;
  return Math.max(0, Math.min(10, Math.trunc(level)));
}

function isFullFen(fen: string): boolean {
  return fen.split(/\s+/).length >= 4;
}

function isBoardPlacementFen(fen: string): boolean {
  const ranks = fen.split('/');
  return ranks.length === 8 && ranks.every(rank => rankWidth(rank) === 8);
}

function rankWidth(rank: string): number {
  let width = 0;
  for (const char of rank) {
    if (char >= '1' && char <= '8') width += Number(char);
    else if (/^[pnbrqkPNBRQK]$/.test(char)) width += 1;
    else return -1;
  }
  return width;
}

function inferInitialCastlingRights(boardPlacement: string): string {
  const ranks = boardPlacement.split('/');
  const blackBackRank = expandFenRank(ranks[0] ?? '');
  const whiteBackRank = expandFenRank(ranks[7] ?? '');
  let rights = '';

  if (whiteBackRank[4] === 'K') {
    if (whiteBackRank[7] === 'R') rights += 'K';
    if (whiteBackRank[0] === 'R') rights += 'Q';
  }
  if (blackBackRank[4] === 'k') {
    if (blackBackRank[7] === 'r') rights += 'k';
    if (blackBackRank[0] === 'r') rights += 'q';
  }
  return rights;
}

function expandFenRank(rank: string): string[] {
  const squares: string[] = [];
  for (const char of rank) {
    if (char >= '1' && char <= '8') {
      for (let i = 0; i < Number(char); i++) squares.push('');
    } else squares.push(char);
  }
  return squares;
}

async function fetchOverlay(url: string): Promise<EvenChessLiveOverlay | undefined> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), requestTimeoutMillis);
  try {
    const response = await fetch(url, {
      credentials: 'same-origin',
      cache: 'no-store',
      headers: { accept: 'application/json' },
      signal: controller.signal,
    });
    if (!response.ok) return undefined;
    const payload: unknown = await response.json();
    return liveOverlayFromPayload(payload);
  } finally {
    clearTimeout(timeout);
  }
}

function liveOverlayFromPayload(payload: unknown): EvenChessLiveOverlay | undefined {
  if (!isRecord(payload)) return undefined;
  const live = payload.live;
  return isLiveOverlay(live) ? live : undefined;
}

function proposedMoveFromPayload(payload: unknown): EvenChessProposedMoveCard | undefined {
  if (!isRecord(payload)) return undefined;
  const proposed = payload.proposed;
  return isProposedMoveCard(proposed) ? proposed : undefined;
}

function potentialMoveRevealFromPayload(payload: unknown): EvenChessPotentialMoveReveal | undefined {
  if (!isRecord(payload)) return undefined;
  const potential = payload.potential;
  return isPotentialMoveReveal(potential) ? potential : undefined;
}

function isLiveOverlay(value: unknown): value is EvenChessLiveOverlay {
  if (!isRecord(value)) return false;
  return (
    value.enabled === true &&
    typeof value.gameId === 'string' &&
    typeof value.ply === 'number' &&
    typeof value.boardStateKey === 'string' &&
    (value.perspective === 'white' || value.perspective === 'black') &&
    typeof value.auditId === 'string' &&
    value.serverAuthorized === true &&
    typeof value.ttlMillis === 'number' &&
    Array.isArray(value.cards) &&
    Array.isArray(value.visuals)
  );
}

function isProposedMoveCard(value: unknown): value is EvenChessProposedMoveCard {
  if (!isRecord(value)) return false;
  return (
    typeof value.key === 'string' &&
    typeof value.gameId === 'string' &&
    typeof value.ply === 'number' &&
    typeof value.boardStateKey === 'string' &&
    (value.perspective === 'white' || value.perspective === 'black') &&
    typeof value.moveUci === 'string' &&
    (value.legal === undefined || typeof value.legal === 'boolean') &&
    (value.postMoveBoardStateKey === undefined || typeof value.postMoveBoardStateKey === 'string') &&
    typeof value.level === 'number' &&
    typeof value.title === 'string' &&
    typeof value.body === 'string' &&
    (value.cards === undefined || Array.isArray(value.cards)) &&
    (value.visuals === undefined || Array.isArray(value.visuals)) &&
    typeof value.auditId === 'string' &&
    value.serverAuthorized === true &&
    value.approvedDisplayPayload === true
  );
}

function isPotentialMoveReveal(value: unknown): value is EvenChessPotentialMoveReveal {
  if (!isRecord(value)) return false;
  return (
    typeof value.key === 'string' &&
    typeof value.gameId === 'string' &&
    typeof value.ply === 'number' &&
    typeof value.boardStateKey === 'string' &&
    (value.perspective === 'white' || value.perspective === 'black') &&
    (value.kind === 'opponent' || value.kind === 'player') &&
    typeof value.level === 'number' &&
    typeof value.quota === 'number' &&
    typeof value.consumed === 'number' &&
    Array.isArray(value.cards) &&
    Array.isArray(value.visuals) &&
    typeof value.auditId === 'string' &&
    value.serverAuthorized === true &&
    value.approvedDisplayPayload === true
  );
}

function numberPayloadField(payload: unknown, key: string): number | undefined {
  if (!isRecord(payload)) return undefined;
  const value = payload[key];
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

function proposedMoveErrorMessage(payload: unknown): string | undefined {
  if (!isRecord(payload)) return undefined;
  const message = typeof payload.message === 'string' ? payload.message : undefined;
  const error = typeof payload.error === 'string' ? payload.error : undefined;
  return message || error;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
