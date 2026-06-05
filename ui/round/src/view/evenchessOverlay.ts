import type { DrawShape } from '@lichess-org/chessground/draw';
import type { Key } from '@lichess-org/chessground/types';

import {
  type EvenChessTtsConfig,
  type EvenChessTtsItem,
  shownTtsText,
  speakEvenChessTts,
  ttsSafetyReason,
} from 'lib/evenchessTts';
import * as licon from 'lib/licon';
import { type VNode, bind, dataIcon, hl } from 'lib/view/snabbdom';
import { apiArgs, transformWikiHtml, wikiBooksUrl } from 'lib/wikiBooks';

import type RoundController from '../ctrl';
import {
  evenChessTestGroundFullFen,
  requestEvenChessTestGroundFullGameReview,
  requestEvenChessTestGroundPotentialMoves,
  requestEvenChessTestGroundProposedMove,
} from '../evenchessTestGround';
import type {
  EvenChessBoardVisual,
  EvenChessCoachTextSnapshot,
  EvenChessCoachCard,
  EvenChessClearInstruction,
  EvenChessDisplayToggles,
  EvenChessLevelFeatureKey,
  EvenChessLevelFeatureToggles,
  EvenChessLiveOverlay,
  EvenChessPotentialMoveKind,
  EvenChessPotentialMoveReveal,
  EvenChessPotentialMoveState,
  EvenChessProposedMoveCard,
  EvenChessProposedMoveState,
  EvenChessTestGroundState,
  RoundData,
} from '../interfaces';

export interface EvenChessBoardSnapshot {
  gameId: string;
  ply: number;
  boardStateKey: string;
  now?: number;
}

const maxCards = 1;
const maxVisuals = 16;
const maxBoardShapes = 8;
const maxBoardOverlayVisuals = 64;
const maxTtsAutoDelaySeconds = 30;
const openingWikiCache = new Map<string, string>();
const openingWikiPending = new Map<string, Promise<string>>();
const evalInfoCache = new WeakMap<RoundData, Map<string, EvenChessEvalInfo>>();
const autoTtsState = new WeakMap<RoundData, EvenChessAutoTtsState>();
const defaultDisplayToggles: EvenChessDisplayToggles = {
  coachCards: true,
  boardVisuals: true,
};
const squareVisualPattern = /^([a-h][1-8]):\s*(.+)$/i;
const arrowVisualPattern = /^([a-h][1-8])-([a-h][1-8]):\s*(.+)$/i;
const files = 'abcdefgh';
const squareSize = 12.5;
const overlayColours = {
  studentThreat: '#22c55e',
  opponentThreat: '#ef4444',
  crossThreat: '#f97316',
  pin: '#f59e0b',
  loosePiece: '#f97316',
  studentHangingPiece: '#dc2626',
  opponentHangingPiece: '#8b5cf6',
  offsetOpponent: '#dc2626',
  offsetStudent: '#16a34a',
  offsetEqual: '#2563eb',
  offsetUnknown: '#64748b',
};

interface EvenChessLevelFeature {
  key: EvenChessLevelFeatureKey;
  level: number;
  label: string;
  surface: 'coach' | 'board' | 'both';
}

interface EvenChessLevelDefinition {
  level: number;
  name: string;
  features: EvenChessLevelFeature[];
}

interface EvenChessBoardOverlayPoint {
  x: number;
  y: number;
}

interface EvenChessBoardOverlayArrow {
  id: string;
  from: Key;
  to: Key;
  colour: string;
  width: number;
  label: string;
  lineStyle: 'solid' | 'dotted';
}

interface EvenChessBoardOverlayIndicator {
  id: string;
  square: Key;
  text: string;
  colour: string;
  tooltip: string;
  position: 'top_right' | 'bottom_right' | 'top_left' | 'bottom_left' | 'centre';
  icon?: 'shield' | 'pin';
}

interface EvenChessBoardOverlayHighlight {
  id: string;
  square: Key;
  colour: string;
  tooltip: string;
}

export interface EvenChessBoardOverlayItems {
  arrows: EvenChessBoardOverlayArrow[];
  highlights: EvenChessBoardOverlayHighlight[];
  indicators: EvenChessBoardOverlayIndicator[];
}

interface EvenChessEvalInfo {
  state: 'ready' | 'disabled' | 'unavailable';
  label: string;
  whitePercent: number;
  cp?: number;
  mate?: number;
  winWhite?: number;
  drawWhite?: number;
  lossWhite?: number;
}

interface EvenChessParsedEvalInfo {
  cp?: number;
  label: string;
  mate?: number;
  winWhite?: number;
  drawWhite?: number;
  lossWhite?: number;
}

type EvenChessEvalScope = 'live' | 'proposed';

interface EvenChessCoachDisplay {
  card: EvenChessCoachCard;
  overlay: EvenChessLiveOverlay;
}

interface EvenChessAutoTtsState {
  scheduledKey?: string;
  spokenKey?: string;
  timer?: ReturnType<typeof setTimeout>;
}

export type EvenChessProposedMoveSelection =
  | {
      kind: 'move';
      orig: Key;
      dest: Key;
      moveUci: string;
      key: string;
      turnKey: string;
      usedLevel: number;
      ply: number;
      fen: string;
    }
  | {
      kind: 'error';
      code: 'no-arrow' | 'multiple-arrows' | 'not-turn' | 'illegal' | 'promotion';
      message: string;
      turnKey: string;
      usedLevel: number;
      ply: number;
      fen: string;
    };

const evenChessLevels: EvenChessLevelDefinition[] = [
  { level: 0, name: 'Standard Board', features: [] },
  {
    level: 1,
    name: 'Rules',
    features: [{ key: 'rules', level: 1, label: 'Legal state', surface: 'coach' }],
  },
  {
    level: 2,
    name: 'Safety',
    features: [
      { key: 'loosePieces', level: 2, label: 'Loose pieces', surface: 'both' },
      { key: 'hangingPieces', level: 2, label: 'Hanging pieces', surface: 'both' },
    ],
  },
  {
    level: 3,
    name: 'Offset Count',
    features: [{ key: 'offsetCount', level: 3, label: 'Exchange count', surface: 'both' }],
  },
  {
    level: 4,
    name: 'Pattern Coach',
    features: [
      { key: 'studentThreats', level: 4, label: 'Student threat arrows', surface: 'board' },
      { key: 'opponentThreats', level: 4, label: 'Opponent threat arrows', surface: 'board' },
      { key: 'pins', level: 4, label: 'Pinned pieces', surface: 'both' },
      { key: 'coachText', level: 4, label: 'Coach text', surface: 'coach' },
    ],
  },
  {
    level: 5,
    name: 'Potential Moves',
    features: [{ key: 'candidate1', level: 5, label: 'Opponent potentials', surface: 'board' }],
  },
  {
    level: 6,
    name: 'Choice Coach',
    features: [
      { key: 'candidate2', level: 6, label: 'My potentials', surface: 'board' },
      { key: 'openingWiki', level: 6, label: 'WikiBook', surface: 'coach' },
    ],
  },
  {
    level: 7,
    name: 'Guided Engine',
    features: [{ key: 'candidate3', level: 7, label: 'Extra potentials', surface: 'board' }],
  },
  {
    level: 8,
    name: 'Precision',
    features: [
      { key: 'evalBar', level: 8, label: 'Eval bar', surface: 'board' },
      { key: 'evalNumbers', level: 8, label: 'Eval text', surface: 'coach' },
    ],
  },
  {
    level: 9,
    name: 'Expert Sparring',
    features: [
      { key: 'humanRisk', level: 9, label: 'Human-risk note', surface: 'coach' },
      { key: 'expertLines', level: 9, label: 'Why-not / branch', surface: 'coach' },
    ],
  },
  {
    level: 10,
    name: 'Full Co-pilot',
    features: [{ key: 'fullSpecificity', level: 10, label: 'Full specificity', surface: 'coach' }],
  },
];

const levelFeatures = evenChessLevels.reduce<EvenChessLevelFeature[]>(
  (features, level) => features.concat(level.features),
  [],
);

export function currentEvenChessBoardSnapshot(ctrl: RoundController): EvenChessBoardSnapshot {
  const fen = ctrl.stepAt(ctrl.ply).fen;
  return {
    gameId: ctrl.data.game.id,
    ply: ctrl.ply,
    boardStateKey: evenChessTestGroundFullFen(ctrl.data, ctrl.ply, fen),
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

function moveTransitionOverlayAllowed(
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): boolean {
  if (!overlay || !overlay.enabled || !overlay.serverAuthorized || !overlay.stale) return false;
  if (payloadHasUnsafeDisplayData(overlay)) return false;
  if (overlay.gameId !== current.gameId) return false;
  if (overlay.expiresAt && (current.now ?? Date.now()) >= overlay.expiresAt) return false;
  if (overlay.ttlMillis <= 0) return false;
  return (overlay.clear ?? []).some(
    clear => {
      if (clear.reason !== 'move-played' || clear.gameId !== current.gameId) return false;
      const previousSnapshot =
        overlay.ply === current.ply && overlay.boardStateKey === current.boardStateKey;
      const targetSnapshot = clear.ply === current.ply && clear.boardStateKey === current.boardStateKey;
      return previousSnapshot || targetSnapshot;
    },
  );
}

function overlayVisualStaleReason(
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): string | undefined {
  const reason = overlayStaleReason(overlay, current);
  if (!reason) return undefined;
  return moveTransitionOverlayAllowed(overlay, current) ? undefined : reason;
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
  if (overlayVisualStaleReason(overlay, current) || !overlay) return [];
  return (overlay.visuals ?? [])
    .filter(visual => visualRenderable(visual, overlay))
    .sort((a, b) => Number(Boolean(b.primary)) - Number(Boolean(a.primary)))
    .slice(0, maxVisuals);
}

export function renderableEvenChessBoardShapes(
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): DrawShape[] {
  if (overlayVisualStaleReason(overlay, current) || !overlay) return [];
  return (overlay.visuals ?? [])
    .filter(visual => visualRenderable(visual, overlay))
    .sort((a, b) => Number(Boolean(b.primary)) - Number(Boolean(a.primary)))
    .map(visualToBoardShape)
    .filter((shape): shape is DrawShape => Boolean(shape))
    .slice(0, maxBoardShapes);
}

export function evenChessBoardShapes(_ctrl: RoundController): DrawShape[] {
  // EvenChess board visuals use a board-attached renderer so dotted arrows,
  // badges, and Offset Count shields match the visual recreation spec.
  return [];
}

export function renderEvenChessBoardOverlay(ctrl: RoundController): VNode | undefined {
  const current = currentEvenChessBoardSnapshot(ctrl);
  const proposedOverlay = activeProposedMoveOverlay(ctrl, current);
  const overlay = proposedOverlay ?? normalEvenChessBoardStateOverlay(ctrl.data, current);
  const displayCurrent = proposedOverlay ? proposedMoveBoardSnapshot(current, proposedOverlay) : current;
  const items = renderableEvenChessBoardOverlayItems(ctrl.data, overlay, displayCurrent);
  if (!items.arrows.length && !items.highlights.length && !items.indicators.length) return undefined;

  const orientation = boardOrientationForCtrl(ctrl);
  const featureSelectionKey = displayFeatureSelectionKey(ctrl.data);
  const transition = moveTransitionOverlayAllowed(overlay, displayCurrent);
  return hl(
    'div.evenchess-board-overlay',
    {
      key: `evenchess-board-overlay-${current.gameId}-${orientation}`,
      attrs: {
        'data-evenchess-board-overlay': 'live',
        'data-orientation': orientation,
        'data-audit-id': overlay?.auditId ?? 'unknown',
        'data-feature-selection': featureSelectionKey,
        'data-transition': transition ? 'move-refresh' : '',
        'aria-hidden': 'true',
      },
    },
    [
      ...items.highlights.map(highlight => renderBoardOverlayHighlight(highlight, orientation)),
      hl(
        'svg.evenchess-board-overlay__arrows',
        {
          attrs: {
            viewBox: '0 0 100 100',
            preserveAspectRatio: 'none',
            focusable: 'false',
          },
        },
        items.arrows.map(arrow => renderBoardOverlayArrow(arrow, orientation)),
      ),
      ...items.indicators.map(indicator => renderBoardOverlayIndicator(indicator, orientation)),
    ],
  );
}

export function renderableEvenChessBoardOverlayItems(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): EvenChessBoardOverlayItems {
  const visuals = displayableEvenChessBoardLayerVisuals(data, overlay, current);
  const arrows = dedupeOverlayItems(
    visuals.flatMap(visual => {
      const arrow = boardOverlayArrowFromVisual(visual);
      return arrow ? [arrow] : [];
    }),
    arrow => `${arrow.from}-${arrow.to}-${arrow.lineStyle}-${arrow.label}`,
  );
  const indicators = dedupeOverlayItems(
    visuals.flatMap(visual => {
      const indicator = boardOverlayIndicatorFromVisual(visual);
      return indicator ? [indicator] : [];
    }),
    indicator => `${indicator.square}-${indicator.position}-${indicator.text}-${indicator.colour}`,
  );
  const highlights = dedupeOverlayItems(
    visuals.flatMap(visual => {
      const highlight = boardOverlayHighlightFromVisual(visual);
      return highlight ? [highlight] : [];
    }),
    highlight => `${highlight.square}-${highlight.colour}`,
  );
  return { arrows, highlights, indicators };
}

export function clearEvenChessLiveOverlay(
  data: RoundData,
  reason: string,
  ply: number,
  boardStateKey: string,
): void {
  const live = data.evenchess?.live;
  if (!live) return;
  const retainedCoachText = coachTextSnapshotFromOverlay(data, live) ?? data.evenchess?.coachText;
  const clear: EvenChessClearInstruction = {
    gameId: live.gameId || data.game.id,
    ply,
    boardStateKey,
    reason,
    auditId: live.auditId,
  };
  data.evenchess = {
    ...data.evenchess,
    coachText: retainedCoachText,
    live: {
      ...live,
      stale: true,
      clear: [clear],
    },
    potentialMoves: data.evenchess?.potentialMoves
      ? {
          status: 'idle',
          consumedByKind: data.evenchess.potentialMoves.consumedByKind,
          updatedAt: Date.now(),
        }
      : undefined,
    proposedMove: data.evenchess?.proposedMove
      ? {
          status: 'idle',
          consumed: data.evenchess.proposedMove.consumed,
          quota: data.evenchess.proposedMove.quota,
          updatedAt: Date.now(),
        }
      : undefined,
  };
}

function coachTextSnapshotFromOverlay(
  data: RoundData,
  overlay: EvenChessLiveOverlay,
): EvenChessCoachTextSnapshot | undefined {
  const current = {
    gameId: overlay.gameId,
    ply: overlay.ply,
    boardStateKey: overlay.boardStateKey,
  };
  if (overlayStaleReason(overlay, current) || payloadHasUnsafeDisplayData(overlay)) return undefined;
  const card = (overlay.cards ?? [])
    .filter(card => cardRenderable(card, overlay))
    .filter(card => cardFeatureEnabled(data, card))
    .sort((a, b) => Number(Boolean(b.defaultActive)) - Number(Boolean(a.defaultActive)))[0];
  return card ? coachTextSnapshotFromCard(card, overlay) : undefined;
}

export function applyEvenChessLiveOverlay(data: RoundData, overlay: EvenChessLiveOverlay): void {
  initializeEvenChessDisplayForGame(data);

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

  const usedLevel = Math.max(
    data.evenchess?.display?.usedLevel ?? 0,
    payloadUsedLevel(sanitized),
    selectedEvenChessDisplayLevel(data),
  );
  const proposedMove = assistanceSyncedProposedMoveState(data.evenchess?.proposedMove, sanitized);
  const potentialMoves = assistanceSyncedPotentialMoveState(data.evenchess?.potentialMoves, sanitized);

  data.evenchess = {
    ...data.evenchess,
    live: sanitized,
    proposedMove,
    potentialMoves,
    display: {
      ...data.evenchess?.display,
      usedLevel,
      toggles: displayToggles(data),
    },
  };
}

function assistanceSyncedProposedMoveState(
  state: EvenChessProposedMoveState | undefined,
  overlay: EvenChessLiveOverlay,
): EvenChessProposedMoveState | undefined {
  const usage = overlay.stale ? undefined : overlay.assistance?.proposedMove;
  if (!usage) return state;
  return {
    ...(state ?? { status: 'idle' as const }),
    consumed: usage.consumed,
    quota: usage.quota,
  };
}

function assistanceSyncedPotentialMoveState(
  state: EvenChessPotentialMoveState | undefined,
  overlay: EvenChessLiveOverlay,
): EvenChessPotentialMoveState | undefined {
  const usage = overlay.stale ? undefined : overlay.assistance?.potentialMoves;
  if (!usage?.consumedByKind) return state;
  return {
    ...(state ?? { status: 'idle' as const }),
    consumedByKind: {
      ...state?.consumedByKind,
      ...usage.consumedByKind,
    },
  };
}

export function initializeEvenChessDisplayForGame(data: RoundData): void {
  const gameId = data.game.id;
  const display = data.evenchess?.display;
  if (display?.initializedForGameId === gameId) return;

  const setLevel = setLevelForData(data);
  const preferredUsedLevel = preferredUsedLevelForData(data, setLevel);
  const defaultFeatureToggles = defaultFeatureTogglesForData(data);
  const stored = display?.toggles;
  const appliedLevel = clampLevel(stored?.appliedLevel ?? display?.usedLevel ?? preferredUsedLevel, setLevel);

  data.evenchess = {
    ...data.evenchess,
    display: {
      ...display,
      initializedForGameId: gameId,
      preferredUsedLevel,
      setLevel,
      usedLevel: Math.max(display?.usedLevel ?? 0, preferredUsedLevel, appliedLevel),
      toggles: {
        ...defaultDisplayToggles,
        ...stored,
        appliedLevel,
        levelFeatures: {
          ...levelFeatureTogglesForAppliedLevel(appliedLevel, setLevel, defaultFeatureToggles),
          ...stored?.levelFeatures,
        },
      },
    },
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
  const current = currentEvenChessBoardSnapshot(ctrl);
  const liveOverlay = normalEvenChessBoardStateOverlay(ctrl.data, current);
  const testGround = ctrl.data.evenchess?.testGround;
  const ttsConfig = evenChessTtsConfigForData(ctrl.data);
  const proposedOverlay = activeProposedMoveOverlay(ctrl, current);
  const overlay = proposedOverlay ?? liveOverlay;
  const displayCurrent = proposedOverlay ? proposedMoveBoardSnapshot(current, proposedOverlay) : current;
  const evalScope: EvenChessEvalScope = proposedOverlay ? 'proposed' : 'live';
  const coachDisplay = displayableEvenChessCoachDisplay(ctrl, overlay, displayCurrent);
  const visuals = displayableEvenChessVisuals(ctrl.data, overlay, displayCurrent);
  const featureSelectionKey = displayFeatureSelectionKey(ctrl.data);
  const staleReason = overlayStaleReason(overlay, displayCurrent);

  if (!coachDisplay && !visuals.length && !shouldShowEvenChessShell(ctrl)) return undefined;
  if (proposedOverlay && liveOverlay) {
    evenChessEvalInfo(ctrl.data, liveOverlay, current, 'evalBar', 'live');
    evenChessEvalInfo(ctrl.data, liveOverlay, current, 'evalNumbers', 'live');
  }

  return hl(
    'aside.evenchess-live',
    {
      key: `evenchess-live-${current.gameId}-${ctrl.data.player?.color ?? 'spectator'}`,
      attrs: {
        'data-evenchess-overlay': 'live',
        'data-audit-id': overlay?.auditId ?? 'test-ground',
        'data-ply': String(overlay?.ply ?? current.ply),
        'data-feature-selection': featureSelectionKey,
        'data-stale-reason': staleReason ?? '',
        'data-visual-count': String(overlay?.visuals?.length ?? 0),
        'data-display-visual-count': String(visuals.length),
        'data-display-card-count': String(coachDisplay ? 1 : 0),
        role: 'region',
        'aria-live': 'polite',
        'aria-label': 'EvenChess coaching',
      },
    },
    [
      renderCoachColumn(ctrl, testGround, ttsConfig, coachDisplay, overlay, displayCurrent, evalScope),
      renderEvenChessEvalBar(ctrl.data, overlay, displayCurrent, evalScope),
      renderLevelColumn(ctrl, liveOverlay),
    ],
  );
}

function renderLevelColumn(ctrl: RoundController, overlay: EvenChessLiveOverlay | undefined): VNode {
  return hl('div.evenchess-live__level-column', [
    renderOpeningWikiBookCard(ctrl),
    renderLevelControlCard(ctrl, overlay),
  ]);
}

function renderOpeningWikiBookCard(ctrl: RoundController): VNode | undefined {
  if (!featureEnabled(ctrl.data, 'openingWiki')) return undefined;

  const currentPath = evenChessOpeningWikiPathFromSteps(ctrl.data.steps, ctrl.ply);

  return hl(
    'fieldset.analyse__wiki.empty.toggle-box.toggle-box--toggle.toggle-box--ready.evenchess-live__opening-wiki',
    {
      attrs: {
        id: 'wikibook-field',
        'data-evenchess-opening-book': 'live',
        'data-opening-path': currentPath,
      },
      hook: {
        insert: (vnode: VNode) => hydrateEvenChessOpeningWikiCard(vnode.elm as HTMLElement, currentPath),
        postpatch: (_old: VNode, vnode: VNode) =>
          hydrateEvenChessOpeningWikiCard(vnode.elm as HTMLElement, currentPath),
      },
    },
    [
      hl(
        'legend',
        {
          attrs: { tabindex: 0 },
          hook: {
            insert: (vnode: VNode) => prepareEvenChessOpeningWikiLegend(vnode.elm as HTMLElement),
            postpatch: (_old: VNode, vnode: VNode) =>
              prepareEvenChessOpeningWikiLegend(vnode.elm as HTMLElement),
          },
        },
        'WikiBook',
      ),
      hl('div.analyse__wiki-text', [openingWikiEmptyNode()]),
    ],
  );
}

function renderLevelControlCard(
  ctrl: RoundController,
  overlay: EvenChessLiveOverlay | undefined,
): VNode {
  const setLevel = setLevelForData(ctrl.data);
  const selectedLevel = selectedEvenChessDisplayLevel(ctrl.data);
  const usedLevel = displayUsedLevel(ctrl.data, overlay);

  return hl('section.evenchess-live__card.evenchess-live__card--levels', [
    hl('div.evenchess-live__head', [
      hl('strong.evenchess-live__label', 'EvenChess Levels'),
      hl('span.evenchess-live__head-actions', [
        hl('span.evenchess-live__level', `Set Level: ${setLevel}`),
        hl('span.evenchess-live__used', `Used Level: ${usedLevel}`),
      ]),
    ]),
    hl('label.evenchess-live__apply', [
      hl('span', 'Apply up to'),
      hl(
        'select',
        {
          props: {
            value: String(selectedLevel),
          },
          attrs: {
            'aria-label': 'Apply EvenChess features up to level',
          },
          hook: bind('change', (event: Event) => {
            event.stopPropagation();
            applyLevelFromControl(
              ctrl,
              Number.parseInt((event.currentTarget as HTMLSelectElement).value, 10),
            );
          }),
        },
        evenChessLevels.map(level =>
          hl(
            'option',
            {
              attrs: {
                value: String(level.level),
                selected: level.level === selectedLevel,
                disabled: level.level > setLevel,
              },
            },
            `L${level.level} ${level.name}`,
          ),
        ),
      ),
    ]),
    hl(
      'div.evenchess-live__level-list',
      evenChessLevels.map(level => renderLevelRow(ctrl, level, setLevel)),
    ),
  ]);
}

function renderEvenChessEvalBar(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
  scope: EvenChessEvalScope = 'live',
): VNode | undefined {
  if (!featureEnabled(data, 'evalBar')) return undefined;

  const evalInfo = evenChessEvalInfo(data, overlay, current, 'evalBar', scope);
  const blackHeight = 100 - evalInfo.whitePercent;

  return hl(
    'div.evenchess-live__eval',
    {
      attrs: {
        'data-evenchess-eval': evalInfo.state,
        title: evalInfo.label,
        'aria-label': evalInfo.label || 'EvenChess eval bar',
      },
    },
    [
      hl('span.evenchess-live__eval-fill', {
        attrs: {
          style: styleAttr({
            height: `${fixed(blackHeight)}%`,
          }),
        },
      }),
      hl('span.evenchess-live__eval-midline'),
      evalInfo.label ? hl('span.evenchess-live__eval-label', evalInfo.label) : undefined,
    ],
  );
}

function renderLevelRow(ctrl: RoundController, level: EvenChessLevelDefinition, setLevel: number): VNode {
  const disabled = level.level > setLevel;
  return hl(
    `div.evenchess-live__level-row${disabled ? '.is-disabled' : ''}`,
    {
      attrs: {
        'data-evenchess-level': String(level.level),
      },
    },
    [
      hl('div.evenchess-live__level-row-head', [hl('strong', `L${level.level}`), hl('span', level.name)]),
      level.features.length
        ? hl(
            'div.evenchess-live__feature-list',
            level.features.map(feature =>
              renderFeatureToggle(ctrl, feature, featureEnabled(ctrl.data, feature.key), !disabled),
            ),
          )
        : hl('p.evenchess-live__level-empty', 'No coaching'),
    ],
  );
}

function renderFeatureToggle(
  ctrl: RoundController,
  feature: EvenChessLevelFeature,
  enabled: boolean,
  available: boolean,
): VNode {
  return hl('label.evenchess-live__feature-toggle', [
    hl('input', {
      attrs: {
        type: 'checkbox',
        checked: enabled,
        disabled: !available,
      },
      hook: bind('change', (event: Event) => {
        event.stopPropagation();
        if (!available) return;
        setFeatureFromControl(ctrl, feature.key, (event.currentTarget as HTMLInputElement).checked);
      }),
    }),
    hl('span.evenchess-live__feature-text', [
      hl('span.evenchess-live__feature-label', feature.label),
      hl('span.evenchess-live__surface', surfaceLabel(feature.surface)),
    ]),
  ]);
}

function renderCoachColumn(
  ctrl: RoundController,
  testGround: EvenChessTestGroundState | undefined,
  ttsConfig: EvenChessTtsConfig | undefined,
  coachDisplay: EvenChessCoachDisplay | undefined,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
  evalScope: EvenChessEvalScope,
): VNode {
  const potentialMoves = renderPotentialMovePanel(ctrl, overlay, current);
  const proposedMove = renderProposedMovePanel(ctrl);
  const evalStrip = renderCoachEvalStrip(ctrl.data, overlay, current, evalScope);
  const potentialFooter = renderPotentialMoveCoachFooter(ctrl.data, overlay, current);
  return hl('div.evenchess-live__coach-column', [
    coachDisplay
      ? renderCoachCard(ctrl, coachDisplay.overlay, ttsConfig, coachDisplay.card, evalStrip, potentialFooter)
      : renderCoachShell(testGround, evalStrip, potentialFooter),
    renderFullGameReviewPanel(ctrl),
    potentialMoves,
    proposedMove,
  ]);
}

function renderFullGameReviewPanel(ctrl: RoundController): VNode | undefined {
  const replaying = (ctrl as { replaying?: () => boolean }).replaying;
  if (typeof replaying !== 'function' || !replaying.call(ctrl)) return undefined;
  const state = ctrl.data.evenchess?.testGround;
  const loading = state?.status === 'loading' && state.message.toLowerCase().includes('review');
  return hl('section.evenchess-live__card.evenchess-live__card--proposed-control', [
    hl('div.evenchess-live__proposed-action', [
      hl(
        'button.evenchess-live__proposed-button',
        {
          attrs: {
            type: 'button',
            disabled: loading,
            'aria-label': 'Run EvenChess full-game level 10 review',
          },
          hook: bind(
            'click',
            (event: Event) => {
              event.preventDefault();
              event.stopPropagation();
              void requestEvenChessTestGroundFullGameReview(ctrl, 10);
            },
            undefined,
            false,
          ),
        },
        loading
          ? [
              renderEvenChessInlineSpinner('Running EvenChess full-game level 10 review'),
              hl('span', 'Running review'),
            ]
          : 'Run L10 Review',
      ),
      hl('span.evenchess-live__proposed-status', loading ? 'Processing full game' : 'Full game'),
    ]),
  ]);
}

function renderEvenChessInlineSpinner(label: string): VNode {
  return hl('span.evenchess-live__spinner', {
    attrs: {
      role: 'status',
      'aria-label': label,
    },
  });
}

function renderCoachCard(
  ctrl: RoundController,
  overlay: EvenChessLiveOverlay,
  ttsConfig: EvenChessTtsConfig | undefined,
  card: EvenChessCoachCard,
  evalStrip: VNode | undefined,
  potentialFooter: VNode | undefined,
): VNode {
  const ttsItem = liveCardTtsItem(card, overlay, evenChessPlayerTurn(ctrl));
  scheduleEvenChessAutoTts(ctrl.data, ttsConfig, ttsItem);

  return hl(
    'section.evenchess-live__card.evenchess-live__card--coach',
    {
      attrs: {
        'data-feature': card.featureKey,
        'data-audit-id': card.auditId,
      },
    },
    [
      evalStrip,
      hl('div.evenchess-live__head', [
        hl('strong.evenchess-live__label', 'EvenChess Coach'),
        hl('span.evenchess-live__head-actions', [
          renderLevelBadge(card.level),
          renderTtsButton(ttsConfig, ttsItem),
          hl('span.evenchess-live__audit', `Audit ${card.auditId}`),
        ]),
      ]),
      hl('h2.evenchess-live__title', card.title),
      hl('p.evenchess-live__body', card.body),
      potentialFooter,
    ],
  );
}

function renderProposedMovePanel(ctrl: RoundController): VNode {
  const selection = readEvenChessProposedMoveSelection(ctrl);
  const state = ctrl.data.evenchess?.proposedMove;
  const quota = proposedMoveQuotaForUsedLevel(selection.usedLevel);
  const consumed = proposedMoveConsumedCount(state);
  const active = visibleProposedMoveCard(ctrl, selection);
  const disabled = quota < 1 || state?.status === 'loading';
  const usedText = quota > 0 ? `Used ${Math.min(consumed, quota)}/${quota}` : 'Level 5+';
  const message =
    state?.status === 'loading'
      ? 'Checking'
      : state?.status === 'error'
        ? state.message
        : quota < 1
          ? 'Level 5+'
          : active?.cached
            ? `Cached ${usedText}`
            : usedText;

  return hl('div.evenchess-live__proposed-stack', [
    hl('section.evenchess-live__card.evenchess-live__card--proposed-control', [
      hl('div.evenchess-live__proposed-action', [
        hl(
          'button.evenchess-live__proposed-button',
          {
            attrs: {
              type: 'button',
              disabled,
              'aria-label': 'Request EvenChess proposed move preview',
            },
            hook: bind(
              'click',
              (event: Event) => {
                event.preventDefault();
                event.stopPropagation();
                requestEvenChessProposedMovePreview(ctrl);
              },
              undefined,
              false,
            ),
          },
          'Proposed Move',
        ),
        message ? hl('span.evenchess-live__proposed-status', message) : undefined,
      ]),
    ]),
    active ? renderProposedMoveCard(active) : undefined,
  ]);
}

function renderProposedMoveCard(card: EvenChessProposedMoveCard): VNode {
  return hl(
    'section.evenchess-live__card.evenchess-live__card--proposed-result',
    {
      attrs: {
        'data-evenchess-proposed-move': card.moveUci,
        'data-audit-id': card.auditId,
      },
    },
    [
      hl('div.evenchess-live__head', [
        hl('strong.evenchess-live__label', 'Proposed Move'),
        hl('span.evenchess-live__head-actions', [renderLevelBadge(card.level)]),
      ]),
      hl('h2.evenchess-live__title', card.title),
      hl('p.evenchess-live__body', card.body),
    ],
  );
}

function renderPotentialMovePanel(
  ctrl: RoundController,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): VNode {
  const opponent = potentialMoveButtonModel(ctrl.data, overlay, current, 'opponent');
  const player = potentialMoveButtonModel(ctrl.data, overlay, current, 'player');

  return hl('section.evenchess-live__card.evenchess-live__card--proposed-control', [
    hl('div.evenchess-live__head', [hl('strong.evenchess-live__label', 'Potential Moves')]),
    renderPotentialMoveAction(ctrl, opponent),
    renderPotentialMoveAction(ctrl, player),
  ]);
}

function renderPotentialMoveCoachFooter(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): VNode | undefined {
  const reveal = activePotentialMoveReveal(data, overlay, current);
  if (!reveal) return undefined;
  if (!potentialRevealFeatureEnabled(data, reveal)) return undefined;

  const items = potentialMoveRevealTexts(reveal);
  if (!items.length) return undefined;

  return hl('div.evenchess-live__coach-potentials', [
    hl('strong.evenchess-live__coach-potentials-title', [
      reveal.kind === 'player' ? 'My Potential Moves' : 'Opponent Potential Moves',
      hl('span', ` ${Math.min(reveal.consumed, reveal.quota)}/${reveal.quota}`),
    ]),
    hl(
      'ol.evenchess-live__coach-potentials-list',
      items.map(item => hl('li', item)),
    ),
  ]);
}

function potentialMoveRevealTexts(reveal: EvenChessPotentialMoveReveal): string[] {
  const cardTexts = reveal.cards
    .filter(card => card.serverAuthorized && card.approvedDisplayPayload)
    .map(card => `${card.title}: ${card.body}`.trim());
  const visualTexts = reveal.visuals
    .filter(visual => visual.serverAuthorized && visual.approvedDisplayPayload)
    .map(visual => boardLabelText(visual.label));
  return [...cardTexts, ...visualTexts].filter(Boolean).slice(0, 3);
}

interface EvenChessPotentialMoveButton {
  kind: EvenChessPotentialMoveKind;
  label: string;
  quota: number;
  consumed: number;
  active: boolean;
  disabled: boolean;
  message: string;
}

function renderPotentialMoveAction(ctrl: RoundController, model: EvenChessPotentialMoveButton): VNode {
  return hl('div.evenchess-live__proposed-action', [
    hl(
      `button.evenchess-live__proposed-button${model.active ? '.is-active' : ''}`,
      {
        attrs: {
          type: 'button',
          disabled: model.disabled,
          'aria-pressed': String(model.active),
          'aria-label': `Toggle EvenChess ${model.label}`,
        },
        hook: bind(
          'click',
          (event: Event) => {
            event.preventDefault();
            event.stopPropagation();
            requestEvenChessPotentialMoves(ctrl, model.kind);
          },
          undefined,
          false,
        ),
      },
      model.label,
    ),
    hl('span.evenchess-live__proposed-status', model.message),
  ]);
}

function evalInfoCacheKey(
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
  feature: EvenChessLevelFeatureKey,
  scope: EvenChessEvalScope,
): string {
  if (scope === 'proposed' && overlay) {
    return `${feature}:proposed:${overlay.gameId}:${overlay.ply}:${overlay.boardStateKey}`;
  }
  return `${feature}:live:${current.gameId}`;
}

function cachedEvalInfo(data: RoundData, key: string): EvenChessEvalInfo | undefined {
  return evalInfoCache.get(data)?.get(key);
}

function rememberEvalInfo(data: RoundData, key: string, info: EvenChessEvalInfo): void {
  if (info.state !== 'ready') return;
  const cache = evalInfoCache.get(data) ?? new Map<string, EvenChessEvalInfo>();
  cache.set(key, info);
  evalInfoCache.set(data, cache);
}

function evenChessEvalInfo(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
  feature: EvenChessLevelFeatureKey = 'evalBar',
  scope: EvenChessEvalScope = 'live',
): EvenChessEvalInfo {
  if (!featureEnabled(data, feature)) {
    return { state: 'disabled', label: '', whitePercent: 50 };
  }
  const cacheKey = evalInfoCacheKey(overlay, current, feature, scope);
  const cached = cachedEvalInfo(data, cacheKey);
  const liveCached =
    scope === 'proposed' ? cachedEvalInfo(data, evalInfoCacheKey(undefined, current, feature, 'live')) : undefined;
  if (overlayVisualStaleReason(overlay, current) || !overlay) {
    return cached ?? liveCached ?? { state: 'unavailable', label: '', whitePercent: 50 };
  }

  const evalVisual = (overlay.visuals ?? [])
    .filter(visual => visualRenderable(visual, overlay))
    .filter(isAcceptedEvalVisual)
    .pop();
  const structuredEval = evalVisual ? evalInfoFromVisual(evalVisual) : undefined;
  const evalCardText = (overlay.cards ?? [])
    .filter(card => cardRenderable(card, overlay))
    .filter(card => isDeepEvalText(`${card.featureKey} ${card.title} ${card.body}`))
    .map(card => `${card.title} ${card.body}`)
    .pop();
  const evalText = structuredEval ? undefined : evalVisual?.label ?? evalCardText;
  const parsed = structuredEval ?? (evalText ? parseEvalText(evalText) : undefined);
  if (!parsed) return cached ?? liveCached ?? { state: 'unavailable', label: '', whitePercent: 50 };

  const ready: EvenChessEvalInfo = {
    state: 'ready',
    label: parsed.label,
    whitePercent: evalWhitePercentFromInfo(parsed),
    cp: parsed.cp,
    mate: parsed.mate,
    winWhite: parsed.winWhite,
    drawWhite: parsed.drawWhite,
    lossWhite: parsed.lossWhite,
  };
  rememberEvalInfo(data, cacheKey, ready);
  return ready;
}

function renderCoachEvalStrip(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
  scope: EvenChessEvalScope = 'live',
): VNode | undefined {
  const evalInfo = evenChessEvalInfo(data, overlay, current, 'evalNumbers', scope);
  if (evalInfo.state !== 'ready' || (evalInfo.cp === undefined && evalInfo.mate === undefined)) return undefined;

  const playerColour = data.player?.color === 'black' ? 'black' : 'white';
  const relativeCp = evalInfo.cp === undefined ? undefined : playerColour === 'black' ? -evalInfo.cp : evalInfo.cp;
  const relativeMate = evalInfo.mate === undefined ? undefined : playerColour === 'black' ? -evalInfo.mate : evalInfo.mate;
  const state = coachEvalState(relativeCp, relativeMate);
  const value =
    relativeMate !== undefined ? `#${Math.abs(relativeMate)}` : formatCoachEvalValue(relativeCp ?? 0);

  return hl(
    'div.evenchess-live__coach-eval',
    {
      attrs: {
        'data-evenchess-coach-eval': state.kind,
      },
      style: {
        background: state.colour,
      },
    },
    [
      hl('span.evenchess-live__coach-eval-state', state.label),
      hl('span.evenchess-live__coach-eval-score', value),
    ],
  );
}

function coachEvalState(relativeCp: number | undefined, relativeMate?: number): { kind: string; label: string; colour: string } {
  if (relativeMate !== undefined) {
    return relativeMate > 0
      ? { kind: 'mate-for', label: 'Mate', colour: '#16a34a' }
      : { kind: 'mate-against', label: 'Mated', colour: '#dc2626' };
  }
  const cp = relativeCp ?? 0;
  const abs = Math.abs(cp);
  if (abs <= 25) return { kind: 'equal', label: 'Equal', colour: '#2563eb' };
  if (cp > 250) return { kind: 'better-strong', label: 'Better', colour: '#16a34a' };
  if (cp > 75) return { kind: 'better', label: 'Better', colour: '#65a30d' };
  if (cp > 0) return { kind: 'better-slight', label: 'Slightly better', colour: '#eab308' };
  if (cp < -250) return { kind: 'worse-strong', label: 'Worse', colour: '#dc2626' };
  if (cp < -75) return { kind: 'worse', label: 'Worse', colour: '#f97316' };
  return { kind: 'worse-slight', label: 'Slightly worse', colour: '#f59e0b' };
}

function formatCoachEvalValue(relativeCp: number): string {
  const pawns = relativeCp / 100;
  const prefix = pawns > 0 ? '+' : '';
  return `${prefix}${pawns.toFixed(2)}`;
}

function isEvalText(text: string): boolean {
  const normalized = text.toLowerCase();
  return (
    normalized.includes('eval') ||
    normalized.includes('wdl') ||
    normalized.includes('centipawn') ||
    normalized.includes('cp') ||
    normalized.includes('precision') ||
    normalized.includes('mate')
  );
}

function isDeepEvalText(text: string): boolean {
  const normalized = text.toLowerCase();
  if (!isEvalText(normalized)) return false;
  return (
    normalized.includes('ece.eval.deep') ||
    normalized.includes('deep') ||
    normalized.includes('advanced') ||
    normalized.includes('stockfish') ||
    normalized.includes('cached_lichess_eval') ||
    normalized.includes('lichess eval') ||
    normalized.includes('tablebase')
  );
}

function parseEvalText(text: string): EvenChessParsedEvalInfo | undefined {
  const mate = /(?:mate|#)\s*([+-]?\d+)/i.exec(text);
  if (mate) {
    const value = Number.parseInt(mate[1], 10);
    if (Number.isFinite(value)) return { label: `#${value}`, mate: value };
  }

  const centipawn = /([+-]?\d+(?:\.\d+)?)\s*(?:cp|centipawn)/i.exec(text);
  if (centipawn) {
    const cp = Number.parseFloat(centipawn[1]);
    if (Number.isFinite(cp)) return { cp, label: formatEvalLabel(cp) };
  }

  const pawn = /(?:eval|score)\D*([+-]?\d+(?:\.\d+)?)/i.exec(text);
  if (pawn) {
    const value = Number.parseFloat(pawn[1]);
    if (Number.isFinite(value)) {
      const cp = Math.abs(value) <= 20 ? value * 100 : value;
      return { cp, label: formatEvalLabel(cp) };
    }
  }

  return undefined;
}

function formatEvalLabel(cp: number): string {
  const pawns = cp / 100;
  const prefix = pawns > 0 ? '+' : '';
  return `${prefix}${pawns.toFixed(Math.abs(pawns) >= 10 ? 0 : 1)}`;
}

function evalInfoFromVisual(
  visual: EvenChessBoardVisual,
): EvenChessParsedEvalInfo | undefined {
  const cp = finiteNumber(visual.evalCpWhite);
  const mate = finiteNumber(visual.evalMateWhite);
  const winWhite = finiteNumber(visual.evalWinWhite);
  const drawWhite = finiteNumber(visual.evalDrawWhite);
  const lossWhite = finiteNumber(visual.evalLossWhite);
  if (cp === undefined && mate === undefined && winWhite === undefined) return undefined;
  return {
    cp,
    mate,
    winWhite,
    drawWhite,
    lossWhite,
    label:
      mate !== undefined && mate !== 0
        ? `#${mate}`
        : cp !== undefined
          ? formatEvalLabel(cp)
          : visual.label,
  };
}

function finiteNumber(value: unknown): number | undefined {
  const n = Number(value);
  return Number.isFinite(n) ? n : undefined;
}

function isAcceptedEvalVisual(visual: EvenChessBoardVisual): boolean {
  if (evalInfoFromVisual(visual)) return true;
  const text = `${visual.featureKey} ${visual.label}`;
  if (!isDeepEvalText(text)) return false;
  if (visual.featureKey === 'ece.eval') {
    const parsed = parseEvalText(visual.label);
    if (parsed?.cp === 0 && parsed.mate === undefined) return false;
  }
  return true;
}

function evalWhitePercentFromInfo(info: { cp?: number; mate?: number; winWhite?: number; drawWhite?: number }): number {
  if (info.mate !== undefined && info.mate !== 0) return info.mate > 0 ? 98 : 2;
  if (info.cp !== undefined) return evalWhitePercent(info.cp);
  if (info.winWhite !== undefined) {
    const draw = info.drawWhite ?? 0;
    return clampPercent(info.winWhite + draw / 2);
  }
  return 50;
}

function evalWhitePercent(cp: number): number {
  return clampPercent(100 / (1 + Math.exp(-0.00368208 * Math.max(-2000, Math.min(2000, cp)))));
}

function clampPercent(value: number): number {
  return Math.max(2, Math.min(98, value));
}

function applyLevelFromControl(ctrl: RoundController, level: number): void {
  const previousUsedLevel = displayUsedLevel(ctrl.data, ctrl.data.evenchess?.live);
  applyEvenChessLevelPreset(ctrl.data, level);
  requestOverlayRefreshIfUsedLevelRaised(ctrl, previousUsedLevel);
  ctrl.updateEvenChessAutoShapes();
  ctrl.redraw();
}

function setFeatureFromControl(ctrl: RoundController, key: EvenChessLevelFeatureKey, enabled: boolean): void {
  const previousUsedLevel = displayUsedLevel(ctrl.data, ctrl.data.evenchess?.live);
  setEvenChessLevelFeature(ctrl.data, key, enabled);
  requestOverlayRefreshIfUsedLevelRaised(ctrl, previousUsedLevel);
  ctrl.updateEvenChessAutoShapes();
  ctrl.redraw();
}

function requestOverlayRefreshIfUsedLevelRaised(ctrl: RoundController, previousUsedLevel: number): void {
  if (displayUsedLevel(ctrl.data, ctrl.data.evenchess?.live) > previousUsedLevel) {
    ctrl.requestEvenChessOverlayRefresh();
  }
}

export function syncEvenChessCoachTextSnapshot(ctrl: RoundController): boolean {
  if (!ctrl.chessground || !evenChessPlayerTurn(ctrl)) return false;

  const overlay = ctrl.data.evenchess?.live;
  const current = currentEvenChessBoardSnapshot(ctrl);
  if (overlayStaleReason(overlay, current) || !overlay) return false;

  const card = renderableEvenChessCards(overlay, current)[0];
  const previous = ctrl.data.evenchess?.coachText;
  const next = card ? coachTextSnapshotFromCard(card, overlay) : undefined;

  if (sameCoachTextSnapshot(previous, next)) return false;

  ctrl.data.evenchess = {
    ...ctrl.data.evenchess,
    coachText: next,
  };
  return true;
}

export function proposedMoveQuotaForUsedLevel(usedLevel: number): number {
  if (usedLevel >= 8) return 3;
  if (usedLevel >= 6) return 2;
  if (usedLevel >= 5) return 1;
  return 0;
}

export function potentialMoveQuotaForUsedLevel(
  usedLevel: number,
  kind: EvenChessPotentialMoveKind,
): number {
  if (kind === 'player') {
    if (usedLevel >= 8) return 3;
    if (usedLevel >= 7) return 2;
    if (usedLevel >= 6) return 1;
    return 0;
  }

  if (usedLevel >= 8) return 3;
  if (usedLevel >= 7) return 2;
  if (usedLevel >= 5) return 1;
  return 0;
}

export function readEvenChessProposedMoveSelection(ctrl: RoundController): EvenChessProposedMoveSelection {
  const ply = ctrl.ply;
  const fen = evenChessTestGroundFullFen(ctrl.data, ply, ctrl.stepAt(ply).fen);
  const usedLevel = displayUsedLevel(ctrl.data, ctrl.data.evenchess?.live);
  const turnKey = proposedMoveTurnKey(ctrl.data, ply, fen, usedLevel);
  const greenArrows = (ctrl.chessground?.state.drawable.shapes ?? []).filter(
    shape => shape.brush === 'green' && Boolean(shape.dest),
  );

  if (greenArrows.length === 0)
    return {
      kind: 'error',
      code: 'no-arrow',
      message: 'Draw one green arrow first',
      turnKey,
      usedLevel,
      ply,
      fen,
    };
  if (greenArrows.length > 1)
    return {
      kind: 'error',
      code: 'multiple-arrows',
      message: 'Use one green arrow only',
      turnKey,
      usedLevel,
      ply,
      fen,
    };
  if (!ctrl.canMove())
    return {
      kind: 'error',
      code: 'not-turn',
      message: 'Proposed Move is available on your turn',
      turnKey,
      usedLevel,
      ply,
      fen,
    };

  const arrow = greenArrows[0]!;
  const orig = arrow.orig as Key;
  const dest = arrow.dest as Key;
  const legalDests = ctrl.chessground?.state.movable.dests?.get(orig) ?? [];
  if (!legalDests.includes(dest))
    return {
      kind: 'error',
      code: 'illegal',
      message: 'Draw a legal move',
      turnKey,
      usedLevel,
      ply,
      fen,
    };
  if (isPromotionArrow(ctrl, orig, dest))
    return {
      kind: 'error',
      code: 'promotion',
      message: 'Promotion arrows need a promotion piece',
      turnKey,
      usedLevel,
      ply,
      fen,
    };

  const moveUci = `${orig}${dest}`;
  const key = proposedMoveCacheKey(turnKey, moveUci);
  return { kind: 'move', orig, dest, moveUci, key, turnKey, usedLevel, ply, fen };
}

export function syncEvenChessProposedMovePreview(ctrl: RoundController): boolean {
  const state = ctrl.data.evenchess?.proposedMove;
  if (!state || state.status === 'idle') return false;

  const selection = readEvenChessProposedMoveSelection(ctrl);
  const selectedKey = selection.kind === 'move' ? selection.key : undefined;
  if (state.activeKey && state.activeKey === selectedKey) return false;
  if (
    state.active &&
    state.activeKey &&
    selection.kind === 'error' &&
    selection.code !== 'no-arrow' &&
    selection.code !== 'not-turn' &&
    state.active.gameId === ctrl.data.game.id &&
    state.active.ply === selection.ply &&
    state.active.boardStateKey === selection.fen
  )
    return false;
  if (
    !state.activeKey &&
    state.status === 'error' &&
    selection.kind === 'error' &&
    selection.code !== 'no-arrow'
  )
    return false;

  ctrl.data.evenchess = {
    ...ctrl.data.evenchess,
    proposedMove: clearActiveProposedMove(state),
  };
  return true;
}

export function requestEvenChessPotentialMoves(
  ctrl: RoundController,
  kind: EvenChessPotentialMoveKind,
): void {
  const current = currentEvenChessBoardSnapshot(ctrl);
  const overlay = ctrl.data.evenchess?.live;
  const usedLevel = displayUsedLevel(ctrl.data, overlay);
  const quota = potentialMoveQuotaForUsedLevel(usedLevel, kind);
  const now = Date.now();

  if (kind === 'player' && !evenChessPlayerTurn(ctrl)) {
    setPotentialMoveState(ctrl, {
      ...ctrl.data.evenchess?.potentialMoves,
      status: 'error',
      message: 'Available on your turn',
      activeKey: undefined,
      activeKind: kind,
      updatedAt: now,
    });
    return;
  }

  if (quota < 1) {
    setPotentialMoveState(ctrl, {
      ...ctrl.data.evenchess?.potentialMoves,
      status: 'error',
      message: kind === 'player' ? 'Level 6+' : 'Level 5+',
      activeKey: undefined,
      activeKind: kind,
      updatedAt: now,
    });
    return;
  }

  if (overlayStaleReason(overlay, current) || !overlay) {
    setPotentialMoveState(ctrl, {
      ...ctrl.data.evenchess?.potentialMoves,
      status: 'error',
      message: 'Awaiting payload',
      activeKey: undefined,
      activeKind: kind,
      updatedAt: now,
    });
    return;
  }

  const key = potentialMoveRevealKey(ctrl.data, current, usedLevel, kind);
  const state = ctrl.data.evenchess?.potentialMoves;

  if (state?.status === 'ready' && state.activeKey === key && state.activeKind === kind) {
    setPotentialMoveState(ctrl, clearActivePotentialMoves(state) ?? { status: 'idle', updatedAt: now });
    return;
  }

  const cached = state?.cache?.[key];
  if (cached) {
    setPotentialMoveState(ctrl, {
      ...state,
      status: 'ready',
      message: undefined,
      activeKey: key,
      activeKind: kind,
      active: { ...cached, cached: true },
      updatedAt: now,
    });
    return;
  }

  setPotentialMoveState(ctrl, {
    ...state,
    status: 'loading',
    message: 'Checking',
    activeKey: key,
    activeKind: kind,
    active: undefined,
    updatedAt: now,
  });

  void requestEvenChessTestGroundPotentialMoves(ctrl.data, current.ply, current.boardStateKey, kind, usedLevel).then(
    result => {
      const latestCurrent = currentEvenChessBoardSnapshot(ctrl);
      const latestOverlay = ctrl.data.evenchess?.live;
      const latestKey = potentialMoveRevealKey(ctrl.data, latestCurrent, usedLevel, kind);
      if (
        latestKey !== key ||
        latestCurrent.ply !== current.ply ||
        latestCurrent.boardStateKey !== current.boardStateKey ||
        overlayStaleReason(latestOverlay, latestCurrent)
      )
        return;

      if (!result.reveal) {
        setPotentialMoveState(ctrl, {
          ...ctrl.data.evenchess?.potentialMoves,
          status: 'error',
          message: result.error ?? 'Potential Moves unavailable',
          activeKey: undefined,
          activeKind: kind,
          active: undefined,
          updatedAt: Date.now(),
        });
        return;
      }

      if (
        result.reveal.key !== key ||
        result.reveal.gameId !== ctrl.data.game.id ||
        result.reveal.ply !== current.ply ||
        result.reveal.boardStateKey !== current.boardStateKey ||
        result.reveal.kind !== kind
      ) {
        setPotentialMoveState(ctrl, {
          ...ctrl.data.evenchess?.potentialMoves,
          status: 'error',
          message: 'Potential Moves payload no longer matches the board',
          activeKey: undefined,
          activeKind: kind,
          active: undefined,
          updatedAt: Date.now(),
        });
        return;
      }

      const reveal = { ...result.reveal, createdAt: Date.now() };
      const currentState = ctrl.data.evenchess?.potentialMoves;
      setPotentialMoveState(ctrl, {
        ...currentState,
        status: 'ready',
        message: undefined,
        activeKey: key,
        activeKind: kind,
        active: reveal,
        cache: {
          ...currentState?.cache,
          [key]: reveal,
        },
        consumedByKind: {
          ...currentState?.consumedByKind,
          [kind]: reveal.consumed,
        },
        updatedAt: Date.now(),
      });
    },
  );
}

export function requestEvenChessProposedMovePreview(ctrl: RoundController): void {
  const selection = readEvenChessProposedMoveSelection(ctrl);
  const now = Date.now();
  const quota = proposedMoveQuotaForUsedLevel(selection.usedLevel);
  const current = ctrl.data.evenchess?.proposedMove;
  const baseOverlay = proposedMoveBaseOverlay(ctrl, selection);

  if (quota < 1) {
    setProposedMoveState(ctrl, {
      status: 'error',
      message: 'Proposed Move starts at level 5',
      updatedAt: now,
    });
    return;
  }

  if (selection.kind === 'error') {
    setProposedMoveState(ctrl, {
      ...current,
      status: 'error',
      message: selection.message,
      updatedAt: now,
    });
    return;
  }

  if (current?.status === 'loading' && current.activeKey === selection.key) return;

  if (current?.status === 'ready' && current.activeKey === selection.key && current.active) {
    const cleared = clearActiveProposedMove(current);
    setProposedMoveState(ctrl, cleared ?? { status: 'idle', updatedAt: now });
    return;
  }

  const cached = current?.cache?.[selection.key];
  if (cached) {
    setProposedMoveState(ctrl, {
      ...current,
      status: 'ready',
      message: undefined,
      activeKey: selection.key,
      active: { ...cached, cached: true },
      baseOverlay,
      updatedAt: now,
    });
    return;
  }

  setProposedMoveState(ctrl, {
    ...current,
    status: 'loading',
    message: 'Checking',
    activeKey: selection.key,
    baseOverlay,
    updatedAt: now,
  });

  void requestEvenChessTestGroundProposedMove(
    ctrl.data,
    selection.ply,
    selection.fen,
    selection.moveUci,
    selection.usedLevel,
  ).then(result => {
    const currentSelection = readEvenChessProposedMoveSelection(ctrl);
    if (currentSelection.kind !== 'move' || currentSelection.key !== selection.key) return;

    if (!result.card) {
      setProposedMoveState(ctrl, {
        ...ctrl.data.evenchess?.proposedMove,
        status: 'error',
        message: result.error ?? 'Proposed Move unavailable',
        activeKey: undefined,
        active: undefined,
        updatedAt: Date.now(),
      });
      return;
    }

    if (
      result.card.gameId !== ctrl.data.game.id ||
      result.card.ply !== selection.ply ||
      result.card.boardStateKey !== selection.fen ||
      result.card.moveUci !== selection.moveUci
    ) {
      setProposedMoveState(ctrl, {
        ...ctrl.data.evenchess?.proposedMove,
        status: 'error',
        message: 'Proposed Move payload no longer matches the board',
        activeKey: undefined,
        active: undefined,
        updatedAt: Date.now(),
      });
      return;
    }

    const card = { ...result.card, key: selection.key, createdAt: Date.now() };
    const state = ctrl.data.evenchess?.proposedMove;
    setProposedMoveState(ctrl, {
      ...state,
      status: 'ready',
      message: undefined,
      activeKey: selection.key,
      active: card,
      baseOverlay: baseOverlay ?? state?.baseOverlay,
      cache: {
        ...state?.cache,
        [selection.key]: card,
      },
      consumedByTurn: {
        ...state?.consumedByTurn,
        [selection.turnKey]: selection.key,
      },
      consumed: result.consumed ?? state?.consumed,
      quota: result.quota ?? state?.quota,
      updatedAt: Date.now(),
    });
  });
}

function setProposedMoveState(ctrl: RoundController, state: EvenChessProposedMoveState): void {
  ctrl.data.evenchess = {
    ...ctrl.data.evenchess,
    proposedMove: state,
  };
  ctrl.redraw();
}

function setPotentialMoveState(ctrl: RoundController, state: EvenChessPotentialMoveState): void {
  ctrl.data.evenchess = {
    ...ctrl.data.evenchess,
    potentialMoves: state,
  };
  ctrl.redraw();
}

function clearActiveProposedMove(
  state: EvenChessProposedMoveState | undefined,
): EvenChessProposedMoveState | undefined {
  if (!state) return undefined;
  return {
    ...state,
    status: 'idle',
    message: undefined,
    activeKey: undefined,
    active: undefined,
    updatedAt: Date.now(),
  };
}

function normalEvenChessBoardStateOverlay(
  data: RoundData,
  current: EvenChessBoardSnapshot,
): EvenChessLiveOverlay | undefined {
  const live = data.evenchess?.live;
  if (live && !overlayStaleReason(live, current)) return live;

  const base = data.evenchess?.proposedMove?.baseOverlay;
  if (base && !overlayStaleReason(base, current)) return base;

  return live;
}

function proposedMoveBaseOverlay(
  ctrl: RoundController,
  selection: EvenChessProposedMoveSelection,
): EvenChessLiveOverlay | undefined {
  if (selection.kind !== 'move') return undefined;
  const current = {
    gameId: ctrl.data.game.id,
    ply: selection.ply,
    boardStateKey: selection.fen,
  };
  return normalEvenChessBoardStateOverlay(ctrl.data, current);
}

function clearActivePotentialMoves(
  state: EvenChessPotentialMoveState | undefined,
): EvenChessPotentialMoveState | undefined {
  if (!state) return undefined;
  return {
    ...state,
    status: 'idle',
    message: undefined,
    activeKey: undefined,
    activeKind: undefined,
    updatedAt: Date.now(),
  };
}

function proposedMoveConsumedCount(state: EvenChessProposedMoveState | undefined): number {
  if (typeof state?.consumed === 'number') return state.consumed;
  return Object.keys(state?.consumedByTurn ?? {}).length;
}

function potentialMoveConsumedCount(
  state: EvenChessPotentialMoveState | undefined,
  kind: EvenChessPotentialMoveKind,
): number {
  const serverConsumed = state?.consumedByKind?.[kind];
  if (typeof serverConsumed === 'number') return serverConsumed;
  return Object.keys(state?.consumedByKey ?? {}).filter(key => key.includes(`:potential:${kind}:`)).length;
}

function potentialMoveButtonModel(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
  kind: EvenChessPotentialMoveKind,
): EvenChessPotentialMoveButton {
  const usedLevel = displayUsedLevel(data, overlay);
  const quota = potentialMoveQuotaForUsedLevel(usedLevel, kind);
  const state = data.evenchess?.potentialMoves;
  const key = potentialMoveRevealKey(data, current, usedLevel, kind);
  const consumed = potentialMoveConsumedCount(state, kind);
  const active = state?.status === 'ready' && state.activeKind === kind && state.activeKey === key;
  const errorForKind = state?.status === 'error' && state.activeKind === kind;
  const loadingForKind = state?.status === 'loading' && state.activeKind === kind;
  const message = errorForKind
    ? state?.message || ''
    : loadingForKind
      ? 'Checking'
      : quota < 1
        ? kind === 'player'
          ? 'Level 6+'
          : 'Level 5+'
        : active
          ? `Shown ${Math.min(consumed, quota)}/${quota}`
          : `Used ${Math.min(consumed, quota)}/${quota}`;

  return {
    kind,
    label: kind === 'player' ? 'My Potentials' : 'Opponent Potentials',
    quota,
    consumed,
    active,
    disabled: quota < 1 || loadingForKind,
    message,
  };
}

function visibleProposedMoveCard(
  ctrl: RoundController,
  selection: EvenChessProposedMoveSelection,
): EvenChessProposedMoveCard | undefined {
  const state = ctrl.data.evenchess?.proposedMove;
  if (!state?.active) return undefined;
  if (selection.kind === 'move') {
    if (state.activeKey !== selection.key || state.active.key !== selection.key) return undefined;
    if (
      state.active.gameId !== ctrl.data.game.id ||
      state.active.ply !== selection.ply ||
      state.active.boardStateKey !== selection.fen ||
      state.active.moveUci !== selection.moveUci
    )
      return undefined;
  } else {
    if (selection.code === 'no-arrow' || selection.code === 'not-turn') return undefined;
    if (
      state.active.gameId !== ctrl.data.game.id ||
      state.active.ply !== selection.ply ||
      state.active.boardStateKey !== selection.fen
    )
      return undefined;
  }
  return state.active;
}

function activeProposedMoveOverlay(
  ctrl: RoundController,
  current: EvenChessBoardSnapshot,
): EvenChessLiveOverlay | undefined {
  const selection = readEvenChessProposedMoveSelection(ctrl);
  const card = visibleProposedMoveCard(ctrl, selection);
  if (!card?.postMoveBoardStateKey || !card.cards || !card.visuals) return undefined;
  if (card.gameId !== current.gameId || card.ply !== current.ply) return undefined;
  const postMoveAuditId =
    card.cards.find(postMoveCard => postMoveCard.auditId)?.auditId ??
    card.visuals.find(visual => visual.auditId)?.auditId ??
    card.auditId;
  return {
    enabled: true,
    gameId: card.gameId,
    ply: card.ply,
    boardStateKey: card.postMoveBoardStateKey,
    perspective: card.perspective,
    auditId: postMoveAuditId,
    serverAuthorized: card.serverAuthorized,
    ttlMillis: 60_000,
    stale: false,
    createdAt: card.createdAt,
    cards: card.cards,
    visuals: card.visuals,
  };
}

function proposedMoveBoardSnapshot(
  current: EvenChessBoardSnapshot,
  overlay: EvenChessLiveOverlay,
): EvenChessBoardSnapshot {
  return {
    ...current,
    boardStateKey: overlay.boardStateKey,
  };
}

function proposedMoveTurnKey(data: RoundData, ply: number, fen: string, usedLevel: number): string {
  return `${data.game.id}:${ply}:${data.player?.color ?? 'white'}:L${usedLevel}:${fen}`;
}

function proposedMoveCacheKey(turnKey: string, moveUci: string): string {
  return `${turnKey}:uci:${moveUci}`;
}

function potentialMoveRevealKey(
  data: RoundData,
  current: EvenChessBoardSnapshot,
  usedLevel: number,
  kind: EvenChessPotentialMoveKind,
): string {
  return `${data.game.id}:${data.player?.color ?? 'white'}:potential:${kind}:L${usedLevel}:${current.ply}:${current.boardStateKey}`;
}

function isPromotionArrow(ctrl: RoundController, orig: Key, dest: Key): boolean {
  const piece = ctrl.chessground?.state.pieces.get(orig);
  if (!piece || piece.role !== 'pawn') return false;
  return (piece.color === 'white' && dest[1] === '8') || (piece.color === 'black' && dest[1] === '1');
}

function displayableEvenChessCards(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): EvenChessCoachCard[] {
  const toggles = displayToggles(data);
  if (!toggles.coachCards || overlayStaleReason(overlay, current) || !overlay) return [];
  return (overlay.cards ?? [])
    .filter(card => cardRenderable(card, overlay))
    .filter(card => cardFeatureEnabled(data, card))
    .sort((a, b) => Number(Boolean(b.defaultActive)) - Number(Boolean(a.defaultActive)))
    .slice(0, maxCards);
}

function displayableEvenChessCoachDisplay(
  ctrl: RoundController,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): EvenChessCoachDisplay | undefined {
  const currentCard = evenChessPlayerTurn(ctrl) ? displayableEvenChessCards(ctrl.data, overlay, current)[0] : undefined;
  if (currentCard && overlay) return { card: currentCard, overlay };

  const snapshot = ctrl.data.evenchess?.coachText;
  if (!snapshot || !coachTextSnapshotVisible(ctrl.data, snapshot)) return undefined;
  return {
    card: snapshot.card,
    overlay: overlayFromCoachTextSnapshot(ctrl.data, snapshot),
  };
}

function evenChessPlayerTurn(ctrl: RoundController): boolean {
  const activeColor = ctrl.data.game.player;
  if (activeColor === 'white' || activeColor === 'black')
    return !ctrl.replaying() && activeColor === ctrl.data.player.color;
  return ctrl.canMove();
}

function coachTextSnapshotFromCard(
  card: EvenChessCoachCard,
  overlay: EvenChessLiveOverlay,
): EvenChessCoachTextSnapshot {
  return {
    card,
    overlayAuditId: overlay.auditId,
    overlayServerAuthorized: overlay.serverAuthorized,
    capturedPly: card.ply,
    capturedBoardStateKey: card.boardStateKey,
    updatedAt: Date.now(),
  };
}

function sameCoachTextSnapshot(
  left: EvenChessCoachTextSnapshot | undefined,
  right: EvenChessCoachTextSnapshot | undefined,
): boolean {
  if (!left || !right) return left === right;
  return (
    left.card.id === right.card.id &&
    left.card.auditId === right.card.auditId &&
    left.card.ply === right.card.ply &&
    left.card.boardStateKey === right.card.boardStateKey &&
    left.card.title === right.card.title &&
    left.card.body === right.card.body &&
    left.card.level === right.card.level
  );
}

function coachTextSnapshotVisible(data: RoundData, snapshot: EvenChessCoachTextSnapshot): boolean {
  const card = snapshot.card;
  const toggles = displayToggles(data);
  return (
    toggles.coachCards &&
    snapshot.overlayServerAuthorized &&
    Boolean(card.id) &&
    Boolean(card.featureKey) &&
    Boolean(card.title) &&
    Boolean(card.body) &&
    card.serverAuthorized &&
    card.approvedDisplayPayload &&
    !card.stale &&
    !card.rawStockfishLine &&
    !card.hiddenDebugData &&
    cardFeatureEnabled(data, card)
  );
}

function overlayFromCoachTextSnapshot(
  data: RoundData,
  snapshot: EvenChessCoachTextSnapshot,
): EvenChessLiveOverlay {
  const card = snapshot.card;
  return {
    enabled: true,
    gameId: card.gameId || data.game.id,
    ply: snapshot.capturedPly,
    boardStateKey: snapshot.capturedBoardStateKey,
    perspective: data.player?.color ?? 'white',
    auditId: snapshot.overlayAuditId || card.auditId,
    serverAuthorized: snapshot.overlayServerAuthorized,
    ttlMillis: card.ttlMillis ?? 0,
    cards: [card],
    visuals: [],
  };
}

function displayableEvenChessVisuals(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): EvenChessBoardVisual[] {
  const toggles = displayToggles(data);
  if (!toggles.boardVisuals || overlayVisualStaleReason(overlay, current) || !overlay) return [];
  return (overlay.visuals ?? [])
    .filter(visual => visualRenderable(visual, overlay))
    .filter(visual => visualFeatureEnabled(data, visual))
    .sort((a, b) => Number(Boolean(b.primary)) - Number(Boolean(a.primary)))
    .slice(0, maxVisuals);
}

function displayableEvenChessBoardLayerVisuals(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): EvenChessBoardVisual[] {
  const toggles = displayToggles(data);
  if (!toggles.boardVisuals || overlayVisualStaleReason(overlay, current) || !overlay) return [];
  const reveal = activePotentialMoveReveal(data, overlay, current);
  const overlayVisuals = (overlay.visuals ?? [])
    .filter(visual => visualRenderable(visual, overlay))
    .filter(visual => visualFeatureEnabled(data, visual))
    .filter(visual => potentialMoveVisualRevealAllowed(data, overlay, current, visual));
  const revealVisuals = (reveal?.visuals ?? [])
    .filter(visual => revealVisualRenderable(visual, reveal))
    .filter(() => potentialRevealFeatureEnabled(data, reveal));
  return [...overlayVisuals, ...revealVisuals]
    .slice(0, maxBoardOverlayVisuals);
}

export function evenChessOpeningWikiPathFromSteps(
  steps: RoundData['steps'] | undefined,
  ply: number,
): string {
  return evenChessOpeningWikiPathParts(steps, ply).join('/').replace(/[+!#?]/g, '');
}

function evenChessOpeningWikiPathParts(
  steps: RoundData['steps'] | undefined,
  ply: number,
): string[] {
  return openingSanSteps(steps, ply)
    .slice(0, 30)
    .map(step => `${openingWikiPlyPrefix(step.ply)}${step.san}`);
}

function openingSanSteps(steps: RoundData['steps'] | undefined, ply: number): Array<{ ply: number; san: string }> {
  return (steps ?? [])
    .filter(step => step.ply > 0 && step.ply <= ply && typeof step.san === 'string' && step.san.trim().length > 0)
    .map(step => ({
      ply: step.ply,
      san: step.san.trim(),
    }));
}

function openingWikiPlyPrefix(ply: number): string {
  return `${Math.floor((ply + 1) / 2)}${ply % 2 === 1 ? '._' : '...'}`;
}

function hydrateEvenChessOpeningWikiCard(card: HTMLElement, defaultPath: string, force = false): void {
  prepareEvenChessOpeningWikiToggle(card);
  const body = card.querySelector<HTMLElement>('.analyse__wiki-text');
  if (!body) return;

  const path = defaultPath;
  if (!path) {
    setOpeningWikiBody(card, body, 'empty', '');
    return;
  }

  if (!force && body.dataset.openingPath === path && body.dataset.state === 'ready') return;

  body.dataset.openingPath = path;

  void fetchOpeningWikiHtml(path).then(html => {
    const activePath = card.getAttribute('data-opening-path') || defaultPath;
    if (activePath !== path) return;
    if (html) setOpeningWikiBody(card, body, 'ready', html);
    else setOpeningWikiBody(card, body, 'empty', '');
  });
}

function prepareEvenChessOpeningWikiToggle(card: HTMLElement): void {
  if (card.dataset.openingWikiToggleReady === '1') return;
  card.dataset.openingWikiToggleReady = '1';
  card.classList.add('toggle-box--ready');

  const storedOpen = readEvenChessOpeningWikiOpen();
  card.classList.toggle('toggle-box--toggle-off', !storedOpen);
}

function prepareEvenChessOpeningWikiLegend(legend: HTMLElement): void {
  if (legend.dataset.openingWikiLegendReady === '1') return;
  legend.dataset.openingWikiLegendReady = '1';
  if (typeof $ === 'function') $(legend).off('click keydown keypress');

  legend.addEventListener('click', toggleEvenChessOpeningWikiFromEvent, { passive: false });
  legend.addEventListener(
    'keydown',
    event => {
      if (event.key === 'Enter' || event.key === ' ') toggleEvenChessOpeningWikiFromEvent(event);
    },
    { passive: false },
  );
}

function toggleEvenChessOpeningWikiFromEvent(event: Event): void {
  const card = (event.currentTarget as HTMLElement | null)?.closest?.(
    '.evenchess-live__opening-wiki',
  ) as HTMLElement | null;
  if (!card) return;
  const nextOpen = card.classList.contains('toggle-box--toggle-off');
  card.classList.toggle('toggle-box--toggle-off', !nextOpen);
  writeEvenChessOpeningWikiOpen(nextOpen);
  event.preventDefault();
  event.stopPropagation();
  event.stopImmediatePropagation?.();
}

const openingWikiOpenStorageKey = 'evenchess.openingWiki.open';

function readEvenChessOpeningWikiOpen(): boolean {
  try {
    return window.localStorage.getItem(openingWikiOpenStorageKey) !== 'false';
  } catch (_) {
    return true;
  }
}

function writeEvenChessOpeningWikiOpen(open: boolean): void {
  try {
    window.localStorage.setItem(openingWikiOpenStorageKey, open ? 'true' : 'false');
  } catch (_) {
    // Ignore storage failures; the fieldset still toggles for the current page.
  }
}

function setOpeningWikiBody(card: HTMLElement, body: HTMLElement, state: string, html: string): void {
  body.dataset.state = state;
  card.classList.toggle('empty', !html);
  body.innerHTML = html || openingWikiEmptyHtml();
}

function openingWikiEmptyNode(): VNode {
  return hl('p.evenchess-live__opening-wiki-empty', 'No WikiBook entry for this line yet.');
}

function openingWikiEmptyHtml(): string {
  return '<p class="evenchess-live__opening-wiki-empty">No WikiBook entry for this line yet.</p>';
}

function fetchOpeningWikiHtml(path: string): Promise<string> {
  const cached = openingWikiCache.get(path);
  if (cached !== undefined) return Promise.resolve(cached);

  const pending = openingWikiPending.get(path);
  if (pending) return pending;

  const title = `Chess_Opening_Theory/${path}`;
  const request = fetch(`${wikiBooksUrl}/w/api.php?titles=${title}&${apiArgs}`)
    .then(async res => {
      if (!res.ok) return '';
      const json = await res.json();
      const page = json.query?.pages?.[0];
      if (!page || page.missing || page.extract?.length === 0) return '';
      if (page.invalid) return `<p>Opening book unavailable: ${escapeHtml(page.invalidreason ?? 'invalid request')}</p>`;
      if (!page.extract) return '';
      return transformWikiHtml(page.extract, title);
    })
    .catch(() => '')
    .then(html => {
      openingWikiCache.set(path, html);
      openingWikiPending.delete(path);
      return html;
    });

  openingWikiPending.set(path, request);
  return request;
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, char => {
    switch (char) {
      case '&':
        return '&amp;';
      case '<':
        return '&lt;';
      case '>':
        return '&gt;';
      case '"':
        return '&quot;';
      default:
        return '&#39;';
    }
  });
}

export function applyEvenChessLevelPreset(data: RoundData, level: number): void {
  initializeEvenChessDisplayForGame(data);

  const setLevel = setLevelForData(data);
  const appliedLevel = clampLevel(level, setLevel);
  const current = displayToggles(data);
  const selectedFeatures = levelFeatureTogglesForAppliedLevel(
    appliedLevel,
    setLevel,
    defaultFeatureTogglesForData(data),
  );

  data.evenchess = {
    ...data.evenchess,
    display: {
      ...data.evenchess?.display,
      usedLevel: Math.max(data.evenchess?.display?.usedLevel ?? 0, appliedLevel),
      toggles: {
        ...current,
        appliedLevel,
        levelFeatures: selectedFeatures,
      },
    },
  };
}

export function setEvenChessLevelFeature(
  data: RoundData,
  key: EvenChessLevelFeatureKey,
  enabled: boolean,
): void {
  initializeEvenChessDisplayForGame(data);

  const feature = featureDefinition(key);
  if (!feature || feature.level > setLevelForData(data)) return;

  const current = displayToggles(data);
  data.evenchess = {
    ...data.evenchess,
    display: {
      ...data.evenchess?.display,
      usedLevel: enabled
        ? Math.max(data.evenchess?.display?.usedLevel ?? 0, feature.level)
        : data.evenchess?.display?.usedLevel,
      toggles: {
        ...current,
        levelFeatures: {
          ...current.levelFeatures,
          [key]: enabled,
        },
      },
    },
  };
}

export function selectedEvenChessDisplayLevel(data: RoundData): number {
  const enabledLevels = levelFeatures
    .filter(feature => featureEnabled(data, feature.key))
    .map(feature => feature.level);
  return Math.max(0, ...enabledLevels);
}

function featureEnabled(data: RoundData, key: EvenChessLevelFeatureKey): boolean {
  const feature = featureDefinition(key);
  if (!feature) return false;

  const setLevel = setLevelForData(data);
  if (feature.level > setLevel) return false;

  const toggles = displayToggles(data);
  return toggles.levelFeatures?.[key] === true;
}

function displayFeatureSelectionKey(data: RoundData): string {
  const toggles = displayToggles(data);
  const potential = data.evenchess?.potentialMoves;
  const proposed = data.evenchess?.proposedMove;
  const featureState = levelFeatures
    .map(feature => `${feature.key}:${featureEnabled(data, feature.key) ? 1 : 0}`)
    .join(',');
  return [
    `cards:${toggles.coachCards ? 1 : 0}`,
    `board:${toggles.boardVisuals ? 1 : 0}`,
    `potential:${potential?.activeKind ?? ''}:${potential?.activeKey ?? ''}:${potential?.status ?? ''}`,
    `proposed:${proposed?.activeKey ?? ''}:${proposed?.status ?? ''}`,
    featureState,
  ].join(';');
}

function cardFeatureEnabled(data: RoundData, card: EvenChessCoachCard): boolean {
  return featureEnabled(data, cardFeatureKey(card));
}

function visualFeatureEnabled(data: RoundData, visual: EvenChessBoardVisual): boolean {
  return featureEnabled(data, visualFeatureKey(visual));
}

function potentialMoveVisualRevealAllowed(
  data: RoundData,
  overlay: EvenChessLiveOverlay,
  current: EvenChessBoardSnapshot,
  visual: EvenChessBoardVisual,
): boolean {
  if (!isPotentialMoveVisual(visual)) return true;

  return Boolean(activePotentialMoveReveal(data, overlay, current));
}

function activePotentialMoveReveal(
  data: RoundData,
  overlay: EvenChessLiveOverlay | undefined,
  current: EvenChessBoardSnapshot,
): EvenChessPotentialMoveReveal | undefined {
  const state = data.evenchess?.potentialMoves;
  if (state?.status !== 'ready' || !state.active || overlayStaleReason(overlay, current) || !overlay) return undefined;
  if (state.activeKey !== state.active.key) return undefined;
  if (
    state.active.gameId !== data.game.id ||
    state.active.ply !== current.ply ||
    state.active.boardStateKey !== current.boardStateKey ||
    state.active.level !== displayUsedLevel(data, overlay) ||
    state.active.serverAuthorized !== true ||
    state.active.approvedDisplayPayload !== true
  )
    return undefined;
  return state.active;
}

function potentialRevealFeatureEnabled(
  data: RoundData,
  reveal: EvenChessPotentialMoveReveal | undefined,
): boolean {
  if (!reveal) return false;
  return featureEnabled(data, reveal.kind === 'player' ? 'candidate2' : 'candidate1');
}

function isPotentialMoveVisual(visual: EvenChessBoardVisual): boolean {
  const text = `${visual.featureKey} ${visual.label}`.toLowerCase();
  return Boolean(candidateFeatureFromKey(visual.featureKey)) || text.includes('candidate') || text.includes('potential');
}

function featureDefinition(key: EvenChessLevelFeatureKey): EvenChessLevelFeature | undefined {
  return levelFeatures.find(feature => feature.key === key);
}

function cardFeatureKey(card: EvenChessCoachCard): EvenChessLevelFeatureKey {
  const text = `${card.featureKey} ${card.title} ${card.body}`.toLowerCase();
  const candidate = candidateFeatureFromKey(card.featureKey);
  if (candidate) return candidate;
  if (
    text.includes('eval') ||
    text.includes('wdl') ||
    text.includes('centipawn') ||
    text.includes('precision')
  ) {
    return 'evalNumbers';
  }
  if (text.includes('candidate') || text.includes('potential') || text.includes('hint'))
    return candidateFeatureForLevel(card.level);
  if (text.includes('human-risk') || text.includes('human risk') || text.includes('risk')) {
    return 'humanRisk';
  }
  if (
    text.includes('why-not') ||
    text.includes('why not') ||
    text.includes('branch') ||
    text.includes('sparring')
  ) {
    return 'expertLines';
  }
  if (text.includes('copilot') || text.includes('co-pilot')) return 'fullSpecificity';
  if (text.includes('legal') || text.includes('rule')) return 'rules';
  if (text.includes('summary') || text.includes('plan') || text.includes('opening')) return 'coachText';
  if (card.level >= 4) return 'coachText';
  return featureKeyFromLevel(card.level, 'coach');
}

function hasExplicitNonAttackableCue(text: string): boolean {
  return (
    text.includes('hanging_not_attackable') ||
    text.includes('not_currently_attackable') ||
    text.includes('not currently attackable') ||
    text.includes('not attackable') ||
    text.includes('not_attackable') ||
    text.includes('not capturable') ||
    text.includes('not_capturable')
  );
}

function hasExplicitAttackableCue(text: string): boolean {
  return (
    text.includes('hanging_attackable') ||
    text.includes('currently_attackable') ||
    text.includes('currently attackable') ||
    text.includes('can_be_taken') ||
    text.includes('can be taken') ||
    text.includes('can_be_captured') ||
    text.includes('can be captured') ||
    text.includes('takeable') ||
    text.includes('capturable') ||
    text.includes('attackable')
  );
}

function isAttackableSafetyText(text: string): boolean {
  return !hasExplicitNonAttackableCue(text) && hasExplicitAttackableCue(text);
}

function isNonAttackableSafetyText(text: string): boolean {
  if (hasExplicitNonAttackableCue(text)) return true;
  if (hasExplicitAttackableCue(text)) return false;
  return text.includes('loose') || text.includes('undefended') || text.includes('unprotected');
}

function isSafetyMarkerText(text: string): boolean {
  return isAttackableSafetyText(text) || isNonAttackableSafetyText(text) || text.includes('hanging');
}

function isStudentThreatText(text: string): boolean {
  return text.includes('student_threat') || text.includes('student threat') || text.includes('player threat');
}

function isOpponentThreatText(text: string): boolean {
  return text.includes('opponent_threat') || text.includes('opponent threat');
}

function isStudentAttackableSafetyText(text: string): boolean {
  return (
    text.includes('student_hanging_attackable') ||
    text.includes('hanging_attackable.student') ||
    text.includes('student hanging') ||
    text.includes('student piece') ||
    text.includes('player hanging') ||
    text.includes('own hanging')
  );
}

function isOpponentAttackableSafetyText(text: string): boolean {
  return (
    text.includes('opponent_hanging_attackable') ||
    text.includes('hanging_attackable.opponent') ||
    text.includes('opponent hanging') ||
    text.includes('opponent piece')
  );
}

function visualFeatureKey(visual: EvenChessBoardVisual): EvenChessLevelFeatureKey {
  const text = `${visual.featureKey} ${visual.label}`.toLowerCase();
  const candidate = candidateFeatureFromKey(visual.featureKey);
  if (candidate) return candidate;
  if (
    text.includes('eval') ||
    text.includes('wdl') ||
    text.includes('centipawn') ||
    text.includes('precision')
  ) {
    return 'evalBar';
  }
  if (text.includes('candidate') || text.includes('potential') || text.includes('hint'))
    return candidateFeatureForLevel(10);
  if (isAttackableSafetyText(text)) return 'hangingPieces';
  if (isNonAttackableSafetyText(text)) return 'loosePieces';
  if (text.includes('hanging')) return 'hangingPieces';
  if (text.includes('danger')) return 'loosePieces';
  if (text.includes('offset') || text.includes('exchange') || text.includes('equal')) return 'offsetCount';
  if (text.includes('pin')) return 'pins';
  if (isOpponentThreatText(text)) return 'opponentThreats';
  if (isStudentThreatText(text) || text.includes('threat')) return 'studentThreats';
  if (text.includes('motif') || text.includes('pattern')) return 'studentThreats';
  return featureKeyFromLevel(4, 'board');
}

function candidateFeatureForLevel(level: number): EvenChessLevelFeatureKey {
  if (level >= 7) return 'candidate3';
  if (level >= 6) return 'candidate2';
  return 'candidate1';
}

function candidateFeatureFromKey(key: string): EvenChessLevelFeatureKey | undefined {
  if (key.includes('candidate.1') || key.includes('potential.1')) return 'candidate1';
  if (key.includes('candidate.2') || key.includes('potential.2')) return 'candidate2';
  if (key.includes('candidate.3') || key.includes('potential.3')) return 'candidate3';
  return undefined;
}

function featureKeyFromLevel(level: number, surface: 'coach' | 'board'): EvenChessLevelFeatureKey {
  const normalizedLevel = clampLevel(level, 10);
  if (normalizedLevel <= 1) return 'rules';
  if (normalizedLevel === 2) return 'hangingPieces';
  if (normalizedLevel === 3) return 'offsetCount';
  if (normalizedLevel === 4) return surface === 'board' ? 'studentThreats' : 'coachText';
  if (normalizedLevel === 5) return 'candidate1';
  if (normalizedLevel === 6) return 'candidate2';
  if (normalizedLevel === 7) return 'candidate3';
  if (normalizedLevel === 8) return surface === 'board' ? 'evalBar' : 'evalNumbers';
  if (normalizedLevel === 9) return 'expertLines';
  return 'fullSpecificity';
}

function setLevelForData(data: RoundData): number {
  return clampLevel(data.evenchess?.display?.setLevel ?? data.evenchess?.testGround?.level ?? 10, 10);
}

function preferredUsedLevelForData(data: RoundData, setLevel: number): number {
  const rawLevel =
    data.evenchess?.display?.preferredUsedLevel ??
    data.pref?.evenchess?.preferredUsedLevel ??
    0;
  return clampLevel(typeof rawLevel === 'number' ? rawLevel : Number(rawLevel), setLevel);
}

function defaultFeatureTogglesForData(data: RoundData): EvenChessLevelFeatureToggles {
  return data.pref?.evenchess?.defaultFeatureToggles ?? {};
}

function displayUsedLevel(data: RoundData, overlay: EvenChessLiveOverlay | undefined): number {
  return Math.max(
    data.evenchess?.display?.usedLevel ?? 0,
    payloadUsedLevel(overlay),
    selectedEvenChessDisplayLevel(data),
  );
}

function clampLevel(level: number, max: number): number {
  if (!Number.isFinite(level)) return 0;
  return Math.max(0, Math.min(Math.trunc(level), Math.max(0, Math.min(max, 10))));
}

function surfaceLabel(surface: EvenChessLevelFeature['surface']): string {
  switch (surface) {
    case 'both':
      return 'Board + coach';
    case 'board':
      return 'Board';
    case 'coach':
      return 'Coach';
  }
}

function shouldShowEvenChessShell(ctrl: RoundController): boolean {
  return (
    ctrl.data.evenchess?.testGround?.enabled ||
    !!ctrl.data.local ||
    !ctrl.data.player.spectator
  );
}

function renderCoachShell(
  testGround: EvenChessTestGroundState | undefined,
  evalStrip: VNode | undefined,
  potentialFooter: VNode | undefined,
): VNode {
  const status = testGround?.status;
  const message = testGround?.message || 'Awaiting payload';
  return hl(
    'section.evenchess-live__card.evenchess-live__card--coach.evenchess-live__card--shell',
    {
      attrs: {
        'data-evenchess-testground-status': status ?? 'idle',
      },
    },
    [
      evalStrip,
      hl('div.evenchess-live__head', [
        hl('strong.evenchess-live__label', 'EvenChess Coach'),
        renderLevelBadge(testGround?.level ?? 10),
      ]),
      hl('h2.evenchess-live__title', status ? testGroundTitle(status) : 'Coach'),
      hl('p.evenchess-live__body', message),
      potentialFooter,
    ],
  );
}

function renderLevelBadge(level: number): VNode {
  return hl('span.evenchess-live__level', `Level ${level}`);
}

function testGroundTitle(status: EvenChessTestGroundState['status']): string {
  switch (status) {
    case 'ready':
      return 'Ready';
    case 'unavailable':
      return 'Waiting';
    case 'loading':
      return 'Loading';
  }
}

export function evenChessTtsConfigForData(data: RoundData): EvenChessTtsConfig | undefined {
  if (data.evenchess?.tts) return data.evenchess.tts;

  const pref = data.pref?.evenchess;
  if (!pref) return undefined;

  return {
    enabled: Boolean(pref.ttsEnabled),
    provider: 'browser-speech',
    voice: pref.ttsVoice,
    ratePercent: pref.ttsRatePercent,
    volumePercent: pref.ttsVolumePercent,
    queueBehavior: pref.ttsQueueBehavior,
    muteDuringOpponentTurn: pref.ttsMuteDuringOpponentTurn,
    autoSpeak: Boolean(pref.ttsAutoSpeak),
    autoDelaySeconds: pref.ttsAutoDelaySeconds,
    serverAuthorized: true,
    policyVersion: 'tts-v1',
  };
}

export function evenChessTtsAutoDelayMillis(config: EvenChessTtsConfig | undefined): number {
  const seconds = Math.trunc(Number(config?.autoDelaySeconds ?? 0));
  if (!Number.isFinite(seconds)) return 0;
  return Math.max(0, Math.min(seconds, maxTtsAutoDelaySeconds)) * 1000;
}

function scheduleEvenChessAutoTts(
  data: RoundData,
  config: EvenChessTtsConfig | undefined,
  item: EvenChessTtsItem,
): void {
  const state = autoTtsState.get(data) ?? {};
  const cancelScheduled = () => {
    if (state.timer) clearTimeout(state.timer);
    state.timer = undefined;
    state.scheduledKey = undefined;
    autoTtsState.set(data, state);
  };

  if (!config?.autoSpeak || ttsSafetyReason(config, item)) {
    cancelScheduled();
    return;
  }

  const key = `${item.auditId}:${item.id}:${item.displayedText}`;
  if (state.spokenKey === key || state.scheduledKey === key) return;
  cancelScheduled();

  state.scheduledKey = key;
  state.timer = setTimeout(() => {
    const current = autoTtsState.get(data) ?? {};
    if (current.scheduledKey !== key) return;
    const result = speakEvenChessTts(config, item);
    current.timer = undefined;
    current.scheduledKey = undefined;
    current.spokenKey = key;
    autoTtsState.set(data, current);
  }, evenChessTtsAutoDelayMillis(config));
  autoTtsState.set(data, state);
}

function renderTtsButton(config: EvenChessTtsConfig | undefined, item: EvenChessTtsItem): VNode {
  const reason = ttsSafetyReason(config, item);
  const disabled = Boolean(reason);
  return hl(
    'button.evenchess-live__tts',
    {
      attrs: {
        ...dataIcon(licon.Voice),
        type: 'button',
        title: ttsButtonTitle(reason),
        'aria-label': 'Read EvenChess coach card aloud',
        disabled,
      },
      hook: bind(
        'click',
        (event: Event) => {
          event.preventDefault();
          event.stopPropagation();
          if (!disabled) speakEvenChessTts(config, item);
        },
        undefined,
        false,
      ),
    },
    [hl('span.evenchess-live__tts-label', 'Speak')],
  );
}

function ttsButtonTitle(reason: ReturnType<typeof ttsSafetyReason>): string {
  switch (reason) {
    case undefined:
      return 'Read aloud';
    case 'disabled':
      return 'Enable TTS Coach in EvenChess settings';
    case 'muted-opponent-turn':
      return 'TTS is muted during the opponent turn';
    case 'unsupported-provider':
      return 'This TTS provider is not available in the browser';
    case 'unsupported-browser':
      return 'This browser does not support speech synthesis';
    case 'unauthorized':
      return 'This coach text is not authorized for TTS';
    case 'text-mismatch':
    case 'unsafe-payload':
      return 'This coach text is not safe to read aloud';
    case 'missing-audit':
      return 'This coach text is missing an audit id';
  }
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

function revealVisualRenderable(
  visual: EvenChessBoardVisual,
  reveal: EvenChessPotentialMoveReveal | undefined,
): boolean {
  return Boolean(
    reveal &&
      visual.id &&
      visual.gameId === reveal.gameId &&
      visual.ply === reveal.ply &&
      visual.boardStateKey === reveal.boardStateKey &&
      visual.auditId === reveal.auditId &&
      visual.featureKey &&
      visual.label &&
      visual.serverAuthorized &&
      visual.approvedDisplayPayload &&
      !visual.stale &&
      !visual.rawStockfishLine &&
      !visual.hiddenDebugData,
  );
}

function boardOverlayArrowFromVisual(visual: EvenChessBoardVisual): EvenChessBoardOverlayArrow | undefined {
  const arrow = arrowVisualPattern.exec(visual.label.trim());
  if (!arrow) return undefined;

  const candidateIndex = candidateIndexFromVisual(visual);
  const text = `${visual.featureKey} ${arrow[3]}`.toLowerCase();
  const isPin = text.includes('pin');
  const isCandidate = candidateIndex !== undefined || text.includes('candidate') || text.includes('proposed');
  if (isPin) return undefined;
  const isOpponent = isOpponentThreatText(text);
  const label = candidateIndex !== undefined ? String.fromCharCode(64 + candidateIndex) : '';

  return {
    id: visual.id,
    from: asKey(arrow[1]),
    to: asKey(arrow[2]),
    colour: isCandidate
      ? overlayColours.studentThreat
      : isOpponent
        ? overlayColours.opponentThreat
        : overlayColours.studentThreat,
    width: candidateIndex !== undefined ? Math.max(6, 9 - candidateIndex) : isOpponent ? 5.5 : 6,
    label,
    lineStyle: isCandidate ? 'solid' : 'dotted',
  };
}

function boardOverlayIndicatorFromVisual(
  visual: EvenChessBoardVisual,
): EvenChessBoardOverlayIndicator | undefined {
  const square = squareVisualPattern.exec(visual.label.trim());
  if (!square) return undefined;

  const label = boardLabelText(square[2]);
  const text = `${visual.featureKey} ${label}`.toLowerCase();
  const key = asKey(square[1]);

  if (text.includes('offset') || text.includes('exchange')) {
    const value = offsetValueFromLabel(text);
    if (text.includes('unknown') || value === undefined) {
      return {
        id: visual.id,
        square: key,
        text: '?',
        colour: overlayColours.offsetUnknown,
        tooltip: 'Offset Count: exchange result unknown',
        position: 'top_right',
      };
    }
    if (value === 0 || text.includes('equal')) {
      return {
        id: visual.id,
        square: key,
        text: '0',
        colour: overlayColours.offsetEqual,
        tooltip: 'Offset Count: Even trade, 0',
        position: 'top_right',
        icon: 'shield',
      };
    }
    const opponentWins = value < 0 || text.includes('unfavorable') || text.includes('opponent');
    const count = String(Math.max(1, Math.abs(value)));
    return {
      id: visual.id,
      square: key,
      text: count,
      colour: opponentWins ? overlayColours.offsetOpponent : overlayColours.offsetStudent,
      tooltip: opponentWins
        ? `Offset Count: Opponent wins pieces: ${count}`
        : `Offset Count: You win pieces: ${count}`,
      position: 'top_right',
    };
  }

  if (text.includes('pin')) {
    return {
      id: visual.id,
      square: key,
      text: '',
      colour: overlayColours.pin,
      tooltip: 'Pinned piece',
      position: 'top_left',
      icon: 'pin',
    };
  }

  if (isSafetyMarkerText(text)) {
    const attackable = isAttackableSafetyText(text);
    const studentAttackable = isStudentAttackableSafetyText(text) || !isOpponentAttackableSafetyText(text);
    return {
      id: visual.id,
      square: key,
      text: '!',
      colour: attackable
        ? studentAttackable
          ? overlayColours.studentHangingPiece
          : overlayColours.opponentHangingPiece
        : overlayColours.loosePiece,
      tooltip: attackable
        ? studentAttackable
          ? 'Student hanging piece that can be taken'
          : 'Opponent hanging piece that can be taken'
        : 'Vulnerable Count: unprotected piece; not capturable this move',
      position: 'bottom_left',
    };
  }

  return undefined;
}

function boardOverlayHighlightFromVisual(
  visual: EvenChessBoardVisual,
): EvenChessBoardOverlayHighlight | undefined {
  const square = squareVisualPattern.exec(visual.label.trim());
  if (!square) return undefined;

  const label = boardLabelText(square[2]);
  const text = `${visual.featureKey} ${label}`.toLowerCase();
  const key = asKey(square[1]);

  if (text.includes('offset') || text.includes('exchange')) return undefined;
  if (text.includes('pin')) return undefined;
  if (isSafetyMarkerText(text)) {
    const attackable = isAttackableSafetyText(text);
    if (!attackable) return undefined;
    const studentAttackable = isStudentAttackableSafetyText(text) || !isOpponentAttackableSafetyText(text);
    return {
      id: `${visual.id}-highlight`,
      square: key,
      colour: studentAttackable ? overlayColours.studentHangingPiece : overlayColours.opponentHangingPiece,
      tooltip: studentAttackable
        ? 'Student hanging piece that can be taken'
        : 'Opponent hanging piece that can be taken',
    };
  }

  return undefined;
}

function renderBoardOverlayArrow(arrow: EvenChessBoardOverlayArrow, orientation: Color): VNode | undefined {
  const start = squareCenter(arrow.from, orientation);
  const end = squareCenter(arrow.to, orientation);
  const dx = end.x - start.x;
  const dy = end.y - start.y;
  const length = Math.sqrt(dx * dx + dy * dy);
  if (length < 0.1) return undefined;

  const unitX = dx / length;
  const unitY = dy / length;
  const lineStart = {
    x: start.x + unitX * 2.25,
    y: start.y + unitY * 2.25,
  };
  const lineEnd = {
    x: end.x - unitX * 3.5,
    y: end.y - unitY * 3.5,
  };
  const angle = Math.atan2(dy, dx);
  const headSize = 2.9;
  const left = {
    x: lineEnd.x - headSize * Math.cos(angle - Math.PI / 6),
    y: lineEnd.y - headSize * Math.sin(angle - Math.PI / 6),
  };
  const right = {
    x: lineEnd.x - headSize * Math.cos(angle + Math.PI / 6),
    y: lineEnd.y - headSize * Math.sin(angle + Math.PI / 6),
  };
  const strokeWidth = Math.max(0.55, arrow.width * 0.13);
  const lineAttrs: Record<string, string> = {
    x1: fixed(lineStart.x),
    y1: fixed(lineStart.y),
    x2: fixed(lineEnd.x),
    y2: fixed(lineEnd.y),
    stroke: arrow.colour,
    'stroke-width': fixed(strokeWidth),
    'stroke-opacity': '0.76',
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round',
  };
  if (arrow.lineStyle === 'dotted') lineAttrs['stroke-dasharray'] = '1.1 1.6';

  return hl('g.evenchess-board-overlay__arrow', { key: arrow.id }, [
    hl('line', {
      attrs: lineAttrs,
    }),
    hl('polygon', {
      attrs: {
        points: `${fixed(lineEnd.x)},${fixed(lineEnd.y)} ${fixed(left.x)},${fixed(left.y)} ${fixed(right.x)},${fixed(right.y)}`,
        fill: arrow.colour,
        'fill-opacity': '0.82',
      },
    }),
    arrow.label
      ? [
          hl('circle', {
            attrs: {
              cx: fixed(start.x),
              cy: fixed(start.y),
              r: '2.1',
              fill: arrow.colour,
              'fill-opacity': '0.92',
              stroke: '#fffdf6',
              'stroke-width': '0.35',
            },
          }),
          hl(
            'text.evenchess-board-overlay__arrow-label',
            {
              attrs: {
                x: fixed(start.x),
                y: fixed(start.y + 0.3),
                'text-anchor': 'middle',
                'dominant-baseline': 'middle',
              },
            },
            arrow.label,
          ),
        ]
      : undefined,
  ]);
}

function renderBoardOverlayHighlight(highlight: EvenChessBoardOverlayHighlight, orientation: Color): VNode {
  return hl('span.evenchess-board-overlay__highlight', {
    key: highlight.id,
    attrs: {
      title: highlight.tooltip,
      'aria-label': highlight.tooltip,
      style: styleAttr(
        squareBoxStyle(highlight.square, orientation, {
          boxShadow: `inset 0 0 0 2px ${highlight.colour}`,
        }),
      ),
    },
  });
}

function renderBoardOverlayIndicator(indicator: EvenChessBoardOverlayIndicator, orientation: Color): VNode {
  return hl(
    'span.evenchess-board-overlay__indicator',
    {
      key: indicator.id,
      attrs: {
        title: indicator.tooltip,
        'aria-label': indicator.tooltip,
        style: styleAttr(indicatorStyle(indicator, orientation)),
      },
    },
    indicator.icon
      ? hl('span.evenchess-board-overlay__indicator-icon', {
          attrs: dataIcon(indicator.icon === 'shield' ? licon.Shield : licon.Padlock),
        })
      : indicator.text,
  );
}

function squareCenter(square: Key, orientation: Color): EvenChessBoardOverlayPoint {
  const file = files.indexOf(square[0]);
  const rank = Number.parseInt(square[1], 10) - 1;
  if (orientation === 'black') {
    return {
      x: (7 - file + 0.5) * squareSize,
      y: (rank + 0.5) * squareSize,
    };
  }
  return {
    x: (file + 0.5) * squareSize,
    y: (7 - rank + 0.5) * squareSize,
  };
}

function squareBoxStyle(
  square: Key,
  orientation: Color,
  extra: Record<string, string> = {},
): Record<string, string> {
  const center = squareCenter(square, orientation);
  const squareLeft = Math.floor(center.x / squareSize) * squareSize;
  const squareTop = Math.floor(center.y / squareSize) * squareSize;
  return {
    left: `${fixed(squareLeft)}%`,
    top: `${fixed(squareTop)}%`,
    width: `${fixed(squareSize)}%`,
    height: `${fixed(squareSize)}%`,
    ...extra,
  };
}

function indicatorStyle(
  indicator: EvenChessBoardOverlayIndicator,
  orientation: Color,
): Record<string, string> {
  const center = squareCenter(indicator.square, orientation);
  const squareLeft = Math.floor(center.x / squareSize) * squareSize;
  const squareTop = Math.floor(center.y / squareSize) * squareSize;
  const size = indicator.position === 'centre' ? 5.2 : 3.8;
  const margin = 1.05;
  let left = squareLeft + squareSize - size - margin;
  let top = squareTop + margin;

  if (indicator.position === 'bottom_right') {
    top = squareTop + squareSize - size - margin;
  } else if (indicator.position === 'top_left') {
    left = squareLeft + margin;
  } else if (indicator.position === 'bottom_left') {
    left = squareLeft + margin;
    top = squareTop + squareSize - size - margin;
  } else if (indicator.position === 'centre') {
    left = center.x - size / 2;
    top = center.y - size / 2;
  }

  return {
    left: `${fixed(left)}%`,
    top: `${fixed(top)}%`,
    width: `${fixed(size)}%`,
    height: `${fixed(size)}%`,
    backgroundColor: indicator.colour,
  };
}

function styleAttr(style: Record<string, string>): string {
  return Object.entries(style)
    .map(([key, value]) => `${key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}: ${value}`)
    .join('; ');
}

function boardOrientationForCtrl(ctrl: RoundController): Color {
  return ctrl.data.game.variant?.key === 'racingKings'
    ? ctrl.flip
      ? 'black'
      : 'white'
    : ctrl.flip
      ? ctrl.data.opponent.color
      : ctrl.data.player.color;
}

function candidateIndexFromVisual(visual: EvenChessBoardVisual): number | undefined {
  const match =
    /(?:candidate|potential)\.(\d)/i.exec(visual.featureKey) ||
    /(?:candidate|potential)\s*([abc123])/i.exec(visual.label);
  if (!match) return undefined;
  const value = match[1].toLowerCase();
  if (value === 'a') return 1;
  if (value === 'b') return 2;
  if (value === 'c') return 3;
  const parsed = Number.parseInt(value, 10);
  return parsed >= 1 && parsed <= 3 ? parsed : undefined;
}

function offsetValueFromLabel(label: string): number | undefined {
  if (label.includes('equal')) return 0;
  const number = /(?:^|\s)([-+]?\d+)(?:\s|$)/.exec(label);
  if (number) return Number.parseInt(number[1], 10);
  if (label.includes('favorable') || label.includes('you win') || label.includes('student')) return 1;
  if (label.includes('unfavorable') || label.includes('opponent')) return -1;
  return undefined;
}

function dedupeOverlayItems<T>(items: T[], key: (item: T) => string): T[] {
  const seen = new Set<string>();
  return items.filter(item => {
    const itemKey = key(item);
    if (seen.has(itemKey)) return false;
    seen.add(itemKey);
    return true;
  });
}

function fixed(value: number): string {
  return value.toFixed(3).replace(/\.?0+$/, '');
}

function visualToBoardShape(visual: EvenChessBoardVisual): DrawShape | undefined {
  const arrow = arrowVisualPattern.exec(visual.label.trim());
  if (arrow) {
    return {
      orig: asKey(arrow[1]),
      dest: asKey(arrow[2]),
      brush: boardBrushForVisual(visual),
      modifiers: { lineWidth: 11 },
      label: {
        text: boardLabelText(arrow[3]),
        fill: boardLabelFill(visual),
      },
    };
  }

  const square = squareVisualPattern.exec(visual.label.trim());
  if (!square) return undefined;
  return {
    orig: asKey(square[1]),
    brush: boardBrushForVisual(visual),
    modifiers: { lineWidth: 16 },
    label: {
      text: boardLabelText(square[2]),
      fill: boardLabelFill(visual),
    },
  };
}

function asKey(value: string): Key {
  return value.toLowerCase() as Key;
}

function boardBrushForVisual(visual: EvenChessBoardVisual): string {
  const text = `${visual.featureKey} ${visual.label}`.toLowerCase();
  if (text.includes('opponent') || text.includes('unfavorable') || text.includes('attackable')) return 'red';
  if (text.includes('student') || text.includes('favorable')) return 'green';
  if (text.includes('equal')) return 'blue';
  if (text.includes('pin')) return 'purple';
  return 'yellow';
}

function boardLabelFill(visual: EvenChessBoardVisual): string | undefined {
  return boardBrushForVisual(visual) === 'yellow' ? '#a25d00' : undefined;
}

function boardLabelText(label: string): string {
  return label.trim().replace(/\s+/g, ' ').slice(0, 28);
}

function displayToggles(data: RoundData): EvenChessDisplayToggles {
  const setLevel = setLevelForData(data);
  const stored = data.evenchess?.display?.toggles;
  const defaultFeatureToggles = defaultFeatureTogglesForData(data);
  const appliedLevel = clampLevel(
    stored?.appliedLevel ?? preferredUsedLevelForData(data, setLevel),
    setLevel,
  );

  return {
    ...defaultDisplayToggles,
    ...stored,
    appliedLevel,
    levelFeatures: {
      ...levelFeatureTogglesForAppliedLevel(appliedLevel, setLevel, defaultFeatureToggles),
      ...stored?.levelFeatures,
    },
  };
}

function levelFeatureTogglesForAppliedLevel(
  appliedLevel: number,
  setLevel: number,
  defaultFeatureToggles: EvenChessLevelFeatureToggles = {},
): EvenChessLevelFeatureToggles {
  const selectedFeatures: EvenChessLevelFeatureToggles = {};
  for (const feature of levelFeatures) {
    selectedFeatures[feature.key] =
      feature.level <= appliedLevel &&
      feature.level <= setLevel &&
      defaultFeatureToggles[feature.key] !== false;
  }
  return selectedFeatures;
}

function payloadUsedLevel(overlay: EvenChessLiveOverlay | undefined): number {
  if (!overlay || overlay.stale || !overlay.serverAuthorized || payloadHasUnsafeDisplayData(overlay))
    return 0;
  return Math.max(
    0,
    ...(overlay.cards ?? []).map(card =>
      card.serverAuthorized && card.approvedDisplayPayload ? card.level : 0,
    ),
  );
}
