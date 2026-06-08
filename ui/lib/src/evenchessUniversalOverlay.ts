export interface EvenChessUniversalOverlayOptions {
  surface: string;
  getFen: () => string | undefined;
  getPly: () => number | undefined;
  getBoardElement: () => HTMLElement | null | undefined;
  getGameId?: () => string | undefined;
  getSide?: () => Color | undefined;
  getOrientation?: () => Color | undefined;
  getLevelsElement?: () => HTMLElement | null | undefined;
  getPanelElement?: () => HTMLElement | null | undefined;
  pollMillis?: number;
}

export interface EvenChessUniversalOverlayHandle {
  refresh: () => void;
  destroy: () => void;
}

interface EvenChessLivePayload {
  enabled?: boolean;
  gameId?: string;
  ply?: number;
  boardStateKey?: string;
  perspective?: Color;
  auditId?: string;
  serverAuthorized?: boolean;
  ttlMillis?: number;
  stale?: boolean;
  cards?: EvenChessPayloadCard[];
  visuals?: EvenChessPayloadVisual[];
}

type EvenChessUniversalFeatureKey =
  | 'rules'
  | 'loosePieces'
  | 'hangingPieces'
  | 'offsetCount'
  | 'studentThreats'
  | 'opponentThreats'
  | 'pins'
  | 'coachText'
  | 'candidate1'
  | 'candidate2'
  | 'openingWiki'
  | 'candidate3'
  | 'evalBar'
  | 'evalNumbers'
  | 'humanRisk'
  | 'expertLines'
  | 'fullSpecificity';

interface EvenChessUniversalLevelFeature {
  key: EvenChessUniversalFeatureKey;
  level: number;
  label: string;
  surface: 'coach' | 'board' | 'both';
}

interface EvenChessUniversalLevelDefinition {
  level: number;
  name: string;
  features: EvenChessUniversalLevelFeature[];
}

export interface EvenChessUniversalDisplayState {
  setLevel: number;
  usedLevel: number;
  appliedLevel: number;
  levelFeatures: Partial<Record<EvenChessUniversalFeatureKey, boolean>>;
}

export interface EvenChessPayloadCard {
  id: string;
  gameId: string;
  ply: number;
  boardStateKey: string;
  featureKey: string;
  title: string;
  body: string;
  level: number;
  auditId: string;
  serverAuthorized: boolean;
  approvedDisplayPayload: boolean;
  stale?: boolean;
  rawStockfishLine?: string;
  hiddenDebugData?: string;
}

export interface EvenChessPayloadVisual {
  id: string;
  gameId: string;
  ply: number;
  boardStateKey: string;
  featureKey: string;
  label: string;
  auditId: string;
  primary?: boolean;
  serverAuthorized: boolean;
  approvedDisplayPayload: boolean;
  stale?: boolean;
  rawStockfishLine?: string;
  hiddenDebugData?: string;
}

interface OverlayPoint {
  x: number;
  y: number;
}

interface OverlayArrow {
  id: string;
  from: Key;
  to: Key;
  colour: string;
  width: number;
  label: string;
  lineStyle: 'solid' | 'dotted';
}

interface OverlayIndicator {
  id: string;
  square: Key;
  text: string;
  colour: string;
  tooltip: string;
  position: 'top_right' | 'bottom_right' | 'top_left' | 'bottom_left' | 'centre';
  icon?: 'shield' | 'pin';
}

interface OverlayHighlight {
  id: string;
  square: Key;
  colour: string;
  tooltip: string;
}

export interface EvenChessUniversalOverlayItems {
  arrows: OverlayArrow[];
  highlights: OverlayHighlight[];
  indicators: OverlayIndicator[];
}

const endpointPath = '/evenchess/ece/board-overlay';
const defaultPollMillis = 700;
const universalLevelStorageKey = 'evenchess.universal.display.v1';
const files = 'abcdefgh';
const squareSize = 12.5;
const squareVisualPattern = /^([a-h][1-8]):\s*(.+)$/i;
const arrowVisualPattern = /^([a-h][1-8])-([a-h][1-8]):\s*(.+)$/i;
const colours = {
  studentThreat: '#22c55e',
  opponentThreat: '#ef4444',
  pin: '#f59e0b',
  loosePiece: '#f97316',
  studentHangingPiece: '#dc2626',
  opponentHangingPiece: '#8b5cf6',
  offsetOpponent: '#dc2626',
  offsetStudent: '#16a34a',
  offsetEqual: '#2563eb',
  offsetUnknown: '#64748b',
};

const universalLevels: EvenChessUniversalLevelDefinition[] = [
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

const universalLevelFeatures = universalLevels.reduce<EvenChessUniversalLevelFeature[]>(
  (features, level) => features.concat(level.features),
  [],
);

let styleInstalled = false;

export function installEvenChessUniversalOverlay(
  options: EvenChessUniversalOverlayOptions,
): EvenChessUniversalOverlayHandle {
  installOverlayStyles();

  let lastKey = '';
  let currentKey = '';
  let destroyed = false;
  let abort: AbortController | undefined;
  let poll: number | undefined;
  let displayState = loadUniversalDisplayState(options.surface);
  let lastBoard: HTMLElement | undefined;
  let lastLevelsPanel: HTMLElement | undefined;
  let lastPanel: HTMLElement | undefined;
  let lastLive: EvenChessLivePayload | undefined;
  let lastFen = '';
  let lastPly = 0;
  let lastOrientation: Color = 'white';

  const clearBoard = () => {
    const host = overlayHost(options.getBoardElement());
    host?.querySelector('.evenchess-universal-board-overlay')?.remove();
  };
  const clearPanel = () => {
    const panelHost = options.getPanelElement?.();
    const levelsHost = options.getLevelsElement?.();
    panelHost?.querySelector('.evenchess-universal-panel')?.remove();
    panelHost?.querySelector('.evenchess-universal-levels')?.remove();
    levelsHost?.querySelector('.evenchess-universal-levels')?.remove();
  };
  const clear = () => {
    clearBoard();
    clearPanel();
  };
  const rerender = (state: EvenChessUniversalDisplayState) => {
    displayState = normalizeUniversalDisplayState(state);
    saveUniversalDisplayState(options.surface, displayState);
    if (lastLevelsPanel) renderUniversalLevelControls(lastLevelsPanel, displayState, rerender);
    if (lastPanel) renderUniversalPanel(lastPanel, lastLive, lastFen, lastPly, lastLive ? 'ready' : 'unavailable', displayState);
    if (lastBoard && lastLive) renderUniversalOverlay(lastBoard, lastLive, lastOrientation, lastFen, lastPly, displayState);
  };

  const refresh = () => {
    if (destroyed) return;
    const fen = cleanFen(options.getFen());
    const board = overlayHost(options.getBoardElement());
    const panel = options.getPanelElement?.();
    const levelsPanel = options.getLevelsElement?.() ?? panel;
    if (!fen || !board) {
      lastKey = '';
      clear();
      return;
    }

    const ply = Math.max(0, options.getPly() ?? 0);
    const side = options.getSide?.();
    const orientation = options.getOrientation?.() ?? side ?? 'white';
    const gameId = safeId(options.getGameId?.() || `${options.surface}-${hashText(fen)}`);
    const key = `${options.surface}:${gameId}:${ply}:${side ?? ''}:${orientation}:${panel ? 'panel' : 'nopanel'}:${levelsPanel ? 'levels' : 'nolevels'}:${fen}`;
    lastBoard = board;
    lastLevelsPanel = levelsPanel ?? undefined;
    lastPanel = panel ?? undefined;
    lastFen = fen;
    lastPly = ply;
    lastOrientation = orientation;
    if (key === lastKey) return;
    lastKey = key;
    currentKey = key;
    if (levelsPanel) renderUniversalLevelControls(levelsPanel, displayState, rerender);
    if (panel) renderUniversalPanel(panel, lastLive, fen, ply, 'loading', displayState);

    abort?.abort();
    abort = new AbortController();
    const url = evenChessUniversalOverlayUrl({
      surface: options.surface,
      gameId,
      fen,
      ply,
      side,
    });

    requestOverlayJson(url, abort.signal)
      .then(json => {
        if (destroyed || currentKey !== key) return;
        if (!json?.live) {
          lastLive = undefined;
          clearBoard();
          if (levelsPanel) renderUniversalLevelControls(levelsPanel, displayState, rerender);
          if (panel) renderUniversalPanel(panel, undefined, fen, ply, 'unavailable', displayState);
          return;
        }
        lastLive = json.live;
        renderUniversalOverlay(board, json.live, orientation, fen, ply, displayState);
        if (levelsPanel) renderUniversalLevelControls(levelsPanel, displayState, rerender);
        if (panel) renderUniversalPanel(panel, json.live, fen, ply, 'ready', displayState);
      })
      .catch(() => {
        if (!destroyed && currentKey === key) {
          clearBoard();
          lastLive = undefined;
          if (levelsPanel) renderUniversalLevelControls(levelsPanel, displayState, rerender);
          if (panel) renderUniversalPanel(panel, undefined, fen, ply, 'unavailable', displayState);
          else clearPanel();
        }
      });
  };

  refresh();
  poll = window.setInterval(refresh, Math.max(250, options.pollMillis ?? defaultPollMillis));

  return {
    refresh,
    destroy: () => {
      destroyed = true;
      abort?.abort();
      if (poll) window.clearInterval(poll);
      clear();
    },
  };
}

function requestOverlayJson(url: string, signal: AbortSignal): Promise<any> {
  const fetchFn =
    typeof globalThis !== 'undefined' && typeof globalThis.fetch === 'function'
      ? globalThis.fetch.bind(globalThis)
      : undefined;
  if (fetchFn)
    return fetchFn(url, {
      credentials: 'same-origin',
      signal,
      headers: { accept: 'application/json' },
    }).then(res => {
      if (!res.ok) throw new Error(`Overlay request failed: ${res.status}`);
      return res.json();
    });

  return requestOverlayJsonWithXhr(url, signal);
}

function requestOverlayJsonWithXhr(url: string, signal: AbortSignal): Promise<any> {
  return new Promise((resolve, reject) => {
    if (typeof XMLHttpRequest !== 'function') {
      reject(new Error('No browser request API is available for EvenChess overlay hydration.'));
      return;
    }

    const xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.withCredentials = true;
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.onload = () => {
      if (xhr.status < 200 || xhr.status >= 300) {
        reject(new Error(`Overlay request failed: ${xhr.status}`));
        return;
      }
      try {
        resolve(JSON.parse(xhr.responseText));
      } catch (error) {
        reject(error);
      }
    };
    xhr.onerror = () => reject(new Error('Overlay request failed.'));
    xhr.onabort = () => reject(new Error('Overlay request aborted.'));
    signal.addEventListener('abort', () => xhr.abort(), { once: true });
    xhr.send();
  });
}

export function evenChessUniversalPanelCards(
  live: EvenChessLivePayload,
  fen: string,
  ply: number,
  limit = 3,
  displayState: EvenChessUniversalDisplayState = defaultUniversalDisplayState(),
): EvenChessPayloadCard[] {
  if (!live.enabled || live.stale || !live.serverAuthorized || live.boardStateKey !== fen || live.ply !== ply)
    return [];

  return (live.cards ?? [])
    .filter(card => cardRenderable(card, live) && universalCardFeatureEnabled(displayState, card))
    .sort((a, b) => cardPriority(a) - cardPriority(b))
    .slice(0, Math.max(0, limit));
}

export function evenChessUniversalOverlayUrl(args: {
  surface: string;
  gameId: string;
  fen: string;
  ply: number;
  side?: Color;
}): string {
  const origin = typeof location === 'undefined' ? 'http://localhost' : location.origin;
  const url = new URL(endpointPath, origin);
  url.searchParams.set('surface', args.surface);
  url.searchParams.set('gameId', args.gameId);
  url.searchParams.set('fen', args.fen);
  url.searchParams.set('ply', String(Math.max(0, args.ply)));
  if (args.side) url.searchParams.set('side', args.side);
  return url.pathname + url.search;
}

export function evenChessUniversalOverlayItems(
  live: EvenChessLivePayload,
  fen: string,
  ply: number,
  displayState: EvenChessUniversalDisplayState = defaultUniversalDisplayState(),
): EvenChessUniversalOverlayItems {
  if (!live.enabled || live.stale || !live.serverAuthorized || live.boardStateKey !== fen || live.ply !== ply)
    return { arrows: [], highlights: [], indicators: [] };

  const visuals = (live.visuals ?? []).filter(
    visual => visualRenderable(visual, live) && universalVisualFeatureEnabled(displayState, visual),
  );
  return {
    arrows: dedupe(
      visuals.flatMap(visual => {
        const arrow = overlayArrowFromVisual(visual);
        return arrow ? [arrow] : [];
      }),
      arrow => `${arrow.from}-${arrow.to}-${arrow.lineStyle}-${arrow.label}-${arrow.colour}`,
    ),
    highlights: dedupe(
      visuals.flatMap(visual => {
        const highlight = overlayHighlightFromVisual(visual);
        return highlight ? [highlight] : [];
      }),
      highlight => `${highlight.square}-${highlight.colour}`,
    ),
    indicators: dedupe(
      visuals.flatMap(visual => {
        const indicator = overlayIndicatorFromVisual(visual);
        return indicator ? [indicator] : [];
      }),
      indicator => `${indicator.square}-${indicator.position}-${indicator.text}-${indicator.colour}`,
    ),
  };
}

function renderUniversalOverlay(
  board: HTMLElement,
  live: EvenChessLivePayload,
  orientation: Color,
  fen: string,
  ply: number,
  displayState: EvenChessUniversalDisplayState,
): void {
  const items = evenChessUniversalOverlayItems(live, fen, ply, displayState);
  const hasItems = items.arrows.length || items.highlights.length || items.indicators.length;
  const existing = board.querySelector<HTMLElement>('.evenchess-universal-board-overlay');
  if (!hasItems) {
    existing?.remove();
    return;
  }

  const overlay = existing ?? document.createElement('div');
  overlay.className = 'evenchess-universal-board-overlay';
  overlay.setAttribute('data-evenchess-board-overlay', 'non-live');
  overlay.setAttribute('data-orientation', orientation);
  overlay.setAttribute('data-audit-id', live.auditId ?? 'unknown');
  overlay.setAttribute('aria-hidden', 'true');
  overlay.replaceChildren(
    ...items.highlights.map(item => highlightElement(item, orientation)),
    arrowsElement(items.arrows, orientation),
    ...items.indicators.map(item => indicatorElement(item, orientation)),
  );

  board.classList.add('evenchess-universal-board-host');
  if (!existing) board.append(overlay);
}

function renderUniversalPanel(
  host: HTMLElement,
  live: EvenChessLivePayload | undefined,
  fen: string,
  ply: number,
  state: 'loading' | 'ready' | 'unavailable',
  displayState: EvenChessUniversalDisplayState,
): void {
  const cards = live ? evenChessUniversalPanelCards(live, fen, ply, 3, displayState) : [];
  const panel = host.querySelector<HTMLElement>('.evenchess-universal-panel') ?? document.createElement('div');
  panel.className = 'evenchess-universal-panel';
  panel.setAttribute('data-evenchess-universal-panel', state);
  panel.setAttribute('data-ply', String(ply));
  if (live?.auditId) panel.setAttribute('data-audit-id', live.auditId);
  else panel.removeAttribute('data-audit-id');

  const header = document.createElement('div');
  header.className = 'evenchess-universal-panel__header';
  const title = document.createElement('span');
  title.className = 'evenchess-universal-panel__title';
  title.textContent = 'EvenChess Coach';
  const badge = document.createElement('span');
  badge.className = 'evenchess-universal-panel__badge';
  badge.textContent = `Level ${displayState.usedLevel}`;
  header.append(title, badge);

  const body = document.createElement('div');
  body.className = 'evenchess-universal-panel__body';
  if (cards.length) {
    body.replaceChildren(...cards.map(cardElement));
  } else {
    const empty = document.createElement('p');
    empty.className = 'evenchess-universal-panel__empty';
    empty.textContent =
      state === 'loading'
        ? 'Loading puzzle coaching...'
        : state === 'unavailable'
          ? 'Coaching unavailable.'
          : 'No coaching text for this position.';
    body.append(empty);
  }

  panel.replaceChildren(header, body);
  if (!panel.parentElement) host.append(panel);
}

function renderUniversalLevelControls(
  host: HTMLElement,
  displayState: EvenChessUniversalDisplayState,
  onDisplayChange: (state: EvenChessUniversalDisplayState) => void,
): void {
  const state = normalizeUniversalDisplayState(displayState);
  const card =
    host.querySelector<HTMLElement>('.evenchess-universal-levels') ?? document.createElement('section');
  card.className = 'evenchess-universal-levels';
  card.setAttribute('data-evenchess-universal-levels', 'true');

  const head = document.createElement('div');
  head.className = 'evenchess-universal-levels__head';
  const label = document.createElement('strong');
  label.textContent = 'EvenChess Levels';
  const badges = document.createElement('span');
  badges.className = 'evenchess-universal-levels__badges';
  const setBadge = document.createElement('span');
  setBadge.textContent = `Set Level: ${state.setLevel}`;
  const usedBadge = document.createElement('span');
  usedBadge.textContent = `Used Level: ${state.usedLevel}`;
  badges.append(setBadge, usedBadge);
  head.append(label, badges);

  const apply = document.createElement('label');
  apply.className = 'evenchess-universal-levels__apply';
  const applyText = document.createElement('span');
  applyText.textContent = 'Apply up to';
  const select = document.createElement('select');
  select.setAttribute('aria-label', 'Apply EvenChess features up to level');
  universalLevels.forEach(level => {
    const option = document.createElement('option');
    option.value = String(level.level);
    option.textContent = `L${level.level} ${level.name}`;
    option.selected = level.level === state.appliedLevel;
    option.disabled = level.level > state.setLevel;
    select.append(option);
  });
  select.addEventListener('change', event => {
    event.stopPropagation();
    onDisplayChange(universalDisplayStateForLevel(Number.parseInt(select.value, 10), state));
  });
  apply.append(applyText, select);

  const list = document.createElement('div');
  list.className = 'evenchess-universal-levels__list';
  universalLevels.forEach(level => list.append(universalLevelRow(level, state, onDisplayChange)));

  card.replaceChildren(head, apply, list);
  if (!card.parentElement) host.prepend(card);
}

function universalLevelRow(
  level: EvenChessUniversalLevelDefinition,
  state: EvenChessUniversalDisplayState,
  onDisplayChange: (state: EvenChessUniversalDisplayState) => void,
): HTMLElement {
  const row = document.createElement('div');
  row.className = 'evenchess-universal-levels__row';
  row.setAttribute('data-evenchess-level', String(level.level));
  if (level.level > state.setLevel) row.classList.add('is-disabled');

  const head = document.createElement('div');
  head.className = 'evenchess-universal-levels__row-head';
  const number = document.createElement('strong');
  number.textContent = `L${level.level}`;
  const name = document.createElement('span');
  name.textContent = level.name;
  head.append(number, name);
  row.append(head);

  if (!level.features.length) {
    const empty = document.createElement('p');
    empty.className = 'evenchess-universal-levels__empty';
    empty.textContent = 'No coaching';
    row.append(empty);
    return row;
  }

  const features = document.createElement('div');
  features.className = 'evenchess-universal-levels__features';
  level.features.forEach(feature => features.append(universalFeatureToggle(feature, state, onDisplayChange)));
  row.append(features);
  return row;
}

function universalFeatureToggle(
  feature: EvenChessUniversalLevelFeature,
  state: EvenChessUniversalDisplayState,
  onDisplayChange: (state: EvenChessUniversalDisplayState) => void,
): HTMLElement {
  const label = document.createElement('label');
  label.className = 'evenchess-universal-levels__toggle';

  const input = document.createElement('input');
  input.type = 'checkbox';
  input.checked = universalFeatureEnabled(state, feature.key);
  input.disabled = feature.level > state.setLevel;
  input.addEventListener('change', event => {
    event.stopPropagation();
    onDisplayChange(universalDisplayStateForFeature(feature.key, input.checked, state));
  });

  const text = document.createElement('span');
  text.className = 'evenchess-universal-levels__toggle-text';
  const title = document.createElement('span');
  title.className = 'evenchess-universal-levels__toggle-label';
  title.textContent = feature.label;
  const surface = document.createElement('span');
  surface.className = 'evenchess-universal-levels__surface';
  surface.textContent = universalSurfaceLabel(feature.surface);
  text.append(title, surface);

  label.append(input, text);
  return label;
}

function overlayHost(element: HTMLElement | null | undefined): HTMLElement | undefined {
  if (!element) return undefined;
  if (element.tagName === 'CG-BOARD') return element.parentElement ?? element;
  const cg = element.querySelector<HTMLElement>('cg-board');
  return cg?.parentElement ?? element;
}

function cardRenderable(card: EvenChessPayloadCard, live: EvenChessLivePayload): boolean {
  return (
    !!card.id &&
    !!card.title &&
    !!card.body &&
    card.gameId === live.gameId &&
    card.ply === live.ply &&
    card.boardStateKey === live.boardStateKey &&
    card.auditId === live.auditId &&
    card.serverAuthorized &&
    card.approvedDisplayPayload &&
    !card.stale &&
    !card.rawStockfishLine &&
    !card.hiddenDebugData
  );
}

function visualRenderable(visual: EvenChessPayloadVisual, live: EvenChessLivePayload): boolean {
  return (
    !!visual.id &&
    !!visual.label &&
    visual.gameId === live.gameId &&
    visual.ply === live.ply &&
    visual.boardStateKey === live.boardStateKey &&
    visual.auditId === live.auditId &&
    visual.serverAuthorized &&
    visual.approvedDisplayPayload &&
    !visual.stale &&
    !visual.rawStockfishLine &&
    !visual.hiddenDebugData
  );
}

function cardPriority(card: EvenChessPayloadCard): number {
  const key = `${card.featureKey} ${card.title}`.toLowerCase();
  if (key.includes('warning')) return 0;
  if (key.includes('summary')) return 1;
  if (key.includes('plan')) return 2;
  if (key.includes('candidate') || key.includes('potential')) return 3;
  if (key.includes('proposed')) return 4;
  if (key.includes('eval')) return 5;
  return 6;
}

function cardElement(card: EvenChessPayloadCard): HTMLElement {
  const item = document.createElement('section');
  item.className = 'evenchess-universal-panel__item';
  const title = document.createElement('h3');
  title.className = 'evenchess-universal-panel__item-title';
  title.textContent = card.title;
  const body = document.createElement('p');
  body.className = 'evenchess-universal-panel__item-body';
  body.textContent = card.body;
  item.append(title, body);
  return item;
}

function defaultUniversalDisplayState(): EvenChessUniversalDisplayState {
  return universalDisplayStateForLevel(10, {
    setLevel: 10,
    usedLevel: 10,
    appliedLevel: 10,
    levelFeatures: {},
  });
}

function universalDisplayStateForLevel(
  level: number,
  previous: EvenChessUniversalDisplayState = defaultUniversalDisplayState(),
): EvenChessUniversalDisplayState {
  const setLevel = clampUniversalLevel(previous.setLevel, 10);
  const appliedLevel = clampUniversalLevel(level, setLevel);
  return {
    setLevel,
    usedLevel: Math.max(previous.usedLevel ?? 0, appliedLevel),
    appliedLevel,
    levelFeatures: Object.fromEntries(
      universalLevelFeatures.map(feature => [feature.key, feature.level <= appliedLevel && feature.level <= setLevel]),
    ) as Partial<Record<EvenChessUniversalFeatureKey, boolean>>,
  };
}

function universalDisplayStateForFeature(
  key: EvenChessUniversalFeatureKey,
  enabled: boolean,
  previous: EvenChessUniversalDisplayState,
): EvenChessUniversalDisplayState {
  const state = normalizeUniversalDisplayState(previous);
  const feature = universalFeatureDefinition(key);
  if (!feature || feature.level > state.setLevel) return state;
  const levelFeatures = {
    ...state.levelFeatures,
    [key]: enabled,
  };
  const appliedLevel = Math.max(
    0,
    ...universalLevelFeatures
      .filter(feature => levelFeatures[feature.key] === true)
      .map(feature => feature.level),
  );
  return {
    ...state,
    usedLevel: enabled ? Math.max(state.usedLevel, feature.level) : state.usedLevel,
    appliedLevel,
    levelFeatures,
  };
}

function normalizeUniversalDisplayState(value: Partial<EvenChessUniversalDisplayState> | undefined): EvenChessUniversalDisplayState {
  const fallback = defaultUniversalDisplayState();
  const setLevel = clampUniversalLevel(value?.setLevel ?? fallback.setLevel, 10);
  const appliedLevel = clampUniversalLevel(value?.appliedLevel ?? fallback.appliedLevel, setLevel);
  const usedLevel = Math.max(clampUniversalLevel(value?.usedLevel ?? appliedLevel, setLevel), appliedLevel);
  return {
    setLevel,
    usedLevel,
    appliedLevel,
    levelFeatures: Object.fromEntries(
      universalLevelFeatures.map(feature => [
        feature.key,
        feature.level <= setLevel
          ? value?.levelFeatures?.[feature.key] !== undefined
            ? value.levelFeatures[feature.key] === true
            : feature.level <= appliedLevel
          : false,
      ]),
    ) as Partial<Record<EvenChessUniversalFeatureKey, boolean>>,
  };
}

function loadUniversalDisplayState(surface: string): EvenChessUniversalDisplayState {
  if (typeof localStorage === 'undefined') return defaultUniversalDisplayState();
  try {
    const raw = localStorage.getItem(`${universalLevelStorageKey}.${safeId(surface)}`);
    return normalizeUniversalDisplayState(raw ? JSON.parse(raw) : undefined);
  } catch {
    return defaultUniversalDisplayState();
  }
}

function saveUniversalDisplayState(surface: string, state: EvenChessUniversalDisplayState): void {
  if (typeof localStorage === 'undefined') return;
  try {
    localStorage.setItem(`${universalLevelStorageKey}.${safeId(surface)}`, JSON.stringify(state));
  } catch {
    // Display preferences are optional; failing to persist them must not break puzzles.
  }
}

function universalFeatureEnabled(
  state: EvenChessUniversalDisplayState,
  key: EvenChessUniversalFeatureKey,
): boolean {
  const feature = universalFeatureDefinition(key);
  if (!feature || feature.level > state.setLevel) return false;
  return state.levelFeatures[key] === true;
}

function universalCardFeatureEnabled(
  state: EvenChessUniversalDisplayState,
  card: EvenChessPayloadCard,
): boolean {
  return universalFeatureEnabled(state, universalCardFeatureKey(card));
}

function universalVisualFeatureEnabled(
  state: EvenChessUniversalDisplayState,
  visual: EvenChessPayloadVisual,
): boolean {
  return universalFeatureEnabled(state, universalVisualFeatureKey(visual));
}

function universalFeatureDefinition(
  key: EvenChessUniversalFeatureKey,
): EvenChessUniversalLevelFeature | undefined {
  return universalLevelFeatures.find(feature => feature.key === key);
}

function universalCardFeatureKey(card: EvenChessPayloadCard): EvenChessUniversalFeatureKey {
  const text = `${card.featureKey} ${card.title} ${card.body}`.toLowerCase();
  const candidate = candidateFeatureFromKey(card.featureKey);
  if (candidate) return candidate;
  if (text.includes('eval') || text.includes('wdl') || text.includes('centipawn') || text.includes('precision'))
    return 'evalNumbers';
  if (text.includes('candidate') || text.includes('potential') || text.includes('hint'))
    return candidateFeatureForLevel(card.level);
  if (text.includes('human-risk') || text.includes('human risk') || text.includes('risk')) return 'humanRisk';
  if (text.includes('why-not') || text.includes('why not') || text.includes('branch') || text.includes('sparring'))
    return 'expertLines';
  if (text.includes('copilot') || text.includes('co-pilot')) return 'fullSpecificity';
  if (text.includes('legal') || text.includes('rule')) return 'rules';
  if (text.includes('summary') || text.includes('plan') || text.includes('opening')) return 'coachText';
  if (card.level >= 4) return 'coachText';
  return featureKeyFromLevel(card.level, 'coach');
}

function universalVisualFeatureKey(visual: EvenChessPayloadVisual): EvenChessUniversalFeatureKey {
  const text = `${visual.featureKey} ${visual.label}`.toLowerCase();
  const candidate = candidateFeatureFromKey(visual.featureKey);
  if (candidate) return candidate;
  if (text.includes('eval') || text.includes('wdl') || text.includes('centipawn') || text.includes('precision'))
    return 'evalBar';
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

function candidateFeatureForLevel(level: number): EvenChessUniversalFeatureKey {
  if (level >= 7) return 'candidate3';
  if (level >= 6) return 'candidate2';
  return 'candidate1';
}

function candidateFeatureFromKey(key: string): EvenChessUniversalFeatureKey | undefined {
  if (key.includes('candidate.1') || key.includes('potential.1')) return 'candidate1';
  if (key.includes('candidate.2') || key.includes('potential.2')) return 'candidate2';
  if (key.includes('candidate.3') || key.includes('potential.3')) return 'candidate3';
  return undefined;
}

function featureKeyFromLevel(level: number, surface: 'coach' | 'board'): EvenChessUniversalFeatureKey {
  const normalizedLevel = clampUniversalLevel(level, 10);
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

function universalSurfaceLabel(surface: EvenChessUniversalLevelFeature['surface']): string {
  switch (surface) {
    case 'both':
      return 'Board + coach';
    case 'board':
      return 'Board';
    case 'coach':
      return 'Coach';
  }
}

function clampUniversalLevel(level: number, max: number): number {
  if (!Number.isFinite(level)) return 0;
  return Math.max(0, Math.min(Math.trunc(level), Math.max(0, Math.min(max, 10))));
}

function overlayArrowFromVisual(visual: EvenChessPayloadVisual): OverlayArrow | undefined {
  const match = arrowVisualPattern.exec(visual.label.trim());
  if (!match) return undefined;

  const text = `${visual.featureKey} ${match[3]}`.toLowerCase();
  if (text.includes('pin')) return undefined;

  const candidateIndex = candidateIndexFromVisual(visual);
  const opponent = text.includes('opponent') || text.includes('black threat');
  return {
    id: visual.id,
    from: asKey(match[1]),
    to: asKey(match[2]),
    colour: candidateIndex ? colours.studentThreat : opponent ? colours.opponentThreat : colours.studentThreat,
    width: candidateIndex ? Math.max(6, 9 - candidateIndex) : opponent ? 5.5 : 6,
    label: candidateIndex ? String.fromCharCode(64 + candidateIndex) : '',
    lineStyle: candidateIndex ? 'solid' : 'dotted',
  };
}

function overlayIndicatorFromVisual(visual: EvenChessPayloadVisual): OverlayIndicator | undefined {
  const match = squareVisualPattern.exec(visual.label.trim());
  if (!match) return undefined;

  const square = asKey(match[1]);
  const label = boardLabelText(match[2]);
  const text = `${visual.featureKey} ${label}`.toLowerCase();

  if (text.includes('offset') || text.includes('exchange')) {
    const value = offsetValueFromLabel(text);
    if (text.includes('unknown') || value === undefined) {
      return {
        id: visual.id,
        square,
        text: '?',
        colour: colours.offsetUnknown,
        tooltip: 'Offset Count: exchange result unknown',
        position: 'top_right',
      };
    }
    if (value === 0 || text.includes('equal')) {
      return {
        id: visual.id,
        square,
        text: '0',
        colour: colours.offsetEqual,
        tooltip: 'Offset Count: even trade, 0',
        position: 'top_right',
        icon: 'shield',
      };
    }
    const opponentWins = value < 0 || text.includes('unfavorable') || text.includes('opponent');
    return {
      id: visual.id,
      square,
      text: String(Math.max(1, Math.abs(value))),
      colour: opponentWins ? colours.offsetOpponent : colours.offsetStudent,
      tooltip: opponentWins ? 'Offset Count: opponent favoured' : 'Offset Count: student favoured',
      position: 'top_right',
    };
  }

  if (text.includes('pin')) {
    return {
      id: visual.id,
      square,
      text: '',
      colour: colours.pin,
      tooltip: 'Pinned piece',
      position: 'top_left',
      icon: 'pin',
    };
  }

  if (isSafetyMarkerText(text)) {
    const attackable = isAttackableSafetyText(text);
    const student = isStudentAttackableSafetyText(text) || !isOpponentAttackableSafetyText(text);
    return {
      id: visual.id,
      square,
      text: '!',
      colour: attackable ? (student ? colours.studentHangingPiece : colours.opponentHangingPiece) : colours.loosePiece,
      tooltip: attackable
        ? student
          ? 'Student hanging piece that can be taken'
          : 'Opponent hanging piece that can be taken'
        : 'Unprotected piece; not capturable this move',
      position: 'bottom_left',
    };
  }

  return undefined;
}

function overlayHighlightFromVisual(visual: EvenChessPayloadVisual): OverlayHighlight | undefined {
  const match = squareVisualPattern.exec(visual.label.trim());
  if (!match) return undefined;

  const square = asKey(match[1]);
  const text = `${visual.featureKey} ${boardLabelText(match[2])}`.toLowerCase();
  if (!isSafetyMarkerText(text) || !isAttackableSafetyText(text)) return undefined;
  const student = isStudentAttackableSafetyText(text) || !isOpponentAttackableSafetyText(text);
  return {
    id: `${visual.id}-highlight`,
    square,
    colour: student ? colours.studentHangingPiece : colours.opponentHangingPiece,
    tooltip: student ? 'Student hanging piece that can be taken' : 'Opponent hanging piece that can be taken',
  };
}

function arrowsElement(arrows: OverlayArrow[], orientation: Color): SVGSVGElement {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.classList.add('evenchess-universal-board-overlay__arrows');
  svg.setAttribute('viewBox', '0 0 100 100');
  svg.setAttribute('preserveAspectRatio', 'none');
  svg.setAttribute('focusable', 'false');
  arrows.forEach(arrow => {
    const group = arrowElement(arrow, orientation);
    if (group) svg.append(group);
  });
  return svg;
}

function arrowElement(arrow: OverlayArrow, orientation: Color): SVGGElement | undefined {
  const start = squareCenter(arrow.from, orientation);
  const end = squareCenter(arrow.to, orientation);
  const dx = end.x - start.x;
  const dy = end.y - start.y;
  const length = Math.sqrt(dx * dx + dy * dy);
  if (length < 0.1) return undefined;

  const unitX = dx / length;
  const unitY = dy / length;
  const lineStart = { x: start.x + unitX * 2.25, y: start.y + unitY * 2.25 };
  const lineEnd = { x: end.x - unitX * 3.5, y: end.y - unitY * 3.5 };
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

  const group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
  group.classList.add('evenchess-universal-board-overlay__arrow');
  const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
  line.setAttribute('x1', fixed(lineStart.x));
  line.setAttribute('y1', fixed(lineStart.y));
  line.setAttribute('x2', fixed(lineEnd.x));
  line.setAttribute('y2', fixed(lineEnd.y));
  line.setAttribute('stroke', arrow.colour);
  line.setAttribute('stroke-width', fixed(Math.max(0.55, arrow.width * 0.13)));
  line.setAttribute('stroke-opacity', '0.76');
  line.setAttribute('stroke-linecap', 'round');
  line.setAttribute('stroke-linejoin', 'round');
  if (arrow.lineStyle === 'dotted') line.setAttribute('stroke-dasharray', '1.1 1.6');
  group.append(line);

  const head = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
  head.setAttribute(
    'points',
    `${fixed(lineEnd.x)},${fixed(lineEnd.y)} ${fixed(left.x)},${fixed(left.y)} ${fixed(right.x)},${fixed(right.y)}`,
  );
  head.setAttribute('fill', arrow.colour);
  head.setAttribute('fill-opacity', '0.82');
  group.append(head);
  return group;
}

function highlightElement(highlight: OverlayHighlight, orientation: Color): HTMLSpanElement {
  const el = document.createElement('span');
  el.className = 'evenchess-universal-board-overlay__highlight';
  el.title = highlight.tooltip;
  el.setAttribute('aria-label', highlight.tooltip);
  Object.assign(
    el.style,
    squareBoxStyle(highlight.square, orientation, {
      boxShadow: `inset 0 0 0 2px ${highlight.colour}`,
    }),
  );
  return el;
}

function indicatorElement(indicator: OverlayIndicator, orientation: Color): HTMLSpanElement {
  const el = document.createElement('span');
  el.className = 'evenchess-universal-board-overlay__indicator';
  el.title = indicator.tooltip;
  el.setAttribute('aria-label', indicator.tooltip);
  Object.assign(el.style, indicatorStyle(indicator, orientation), { backgroundColor: indicator.colour });
  if (indicator.icon === 'shield') el.textContent = '0';
  else if (indicator.icon === 'pin') el.textContent = 'L';
  else el.textContent = indicator.text;
  return el;
}

function squareCenter(square: Key, orientation: Color): OverlayPoint {
  const file = files.indexOf(square[0]);
  const rank = Number.parseInt(square[1], 10) - 1;
  if (orientation === 'black') return { x: (7 - file + 0.5) * squareSize, y: (rank + 0.5) * squareSize };
  return { x: (file + 0.5) * squareSize, y: (7 - rank + 0.5) * squareSize };
}

function squareBoxStyle(square: Key, orientation: Color, extra: Record<string, string> = {}): Record<string, string> {
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

function indicatorStyle(indicator: OverlayIndicator, orientation: Color): Record<string, string> {
  const center = squareCenter(indicator.square, orientation);
  const squareLeft = Math.floor(center.x / squareSize) * squareSize;
  const squareTop = Math.floor(center.y / squareSize) * squareSize;
  const size = indicator.position === 'centre' ? 5.2 : 3.8;
  const margin = 1.05;
  let left = squareLeft + squareSize - size - margin;
  let top = squareTop + margin;
  if (indicator.position === 'bottom_right') top = squareTop + squareSize - size - margin;
  else if (indicator.position === 'top_left') left = squareLeft + margin;
  else if (indicator.position === 'bottom_left') {
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
  };
}

function installOverlayStyles(): void {
  if (styleInstalled || typeof document === 'undefined') return;
  styleInstalled = true;
  const style = document.createElement('style');
  style.textContent = `
.evenchess-universal-board-host { position: relative; }
.evenchess-universal-board-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 4;
}
.evenchess-universal-board-overlay__arrows,
.evenchess-universal-board-overlay__highlight {
  position: absolute;
  inset: 0;
}
.evenchess-universal-board-overlay__highlight {
  box-sizing: border-box;
  border-radius: 2px;
}
.evenchess-universal-board-overlay__indicator {
  position: absolute;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font: 700 clamp(10px, 1.35vw, 16px) / 1 sans-serif;
  box-shadow: 0 0 0 2px rgba(255,255,255,.88), 0 2px 5px rgba(0,0,0,.28);
}
.evenchess-puzzle-coach,
.evenchess-puzzle-levels {
  width: 100%;
}
.evenchess-universal-levels {
  box-sizing: border-box;
  width: 100%;
  margin-top: .75rem;
  padding: .85rem .95rem;
  color: #dbeafe;
  background: #061426;
  border: 1px solid rgba(56,189,248,.62);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0,0,0,.22);
}
.evenchess-universal-levels__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .75rem;
  margin-bottom: .7rem;
}
.evenchess-universal-levels__head strong {
  color: #2dd4bf;
}
.evenchess-universal-levels__badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: .35rem;
}
.evenchess-universal-levels__badges span {
  padding: .16rem .48rem;
  border: 1px solid rgba(45,212,191,.8);
  border-radius: 999px;
  color: #e0f2fe;
  font-size: .74rem;
  font-weight: 700;
  white-space: nowrap;
}
.evenchess-universal-levels__apply {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: .55rem;
  margin-bottom: .7rem;
  font-size: .86rem;
}
.evenchess-universal-levels__apply select {
  min-width: 0;
  height: 2.2rem;
  padding: 0 2rem 0 .7rem;
  color: #e2e8f0;
  background: #08213d;
  border: 1px solid rgba(56,189,248,.78);
  border-radius: 8px;
}
.evenchess-universal-levels__list {
  max-height: min(44vh, 26rem);
  overflow-y: auto;
  padding-right: .25rem;
}
.evenchess-universal-levels__row {
  padding: .45rem 0;
  border-top: 1px solid rgba(148,163,184,.22);
}
.evenchess-universal-levels__row.is-disabled {
  opacity: .55;
}
.evenchess-universal-levels__row-head {
  display: grid;
  grid-template-columns: 2.4rem minmax(0, 1fr);
  gap: .4rem;
  align-items: baseline;
  margin-bottom: .35rem;
}
.evenchess-universal-levels__row-head strong {
  color: #67e8f9;
}
.evenchess-universal-levels__empty,
.evenchess-universal-levels__surface {
  color: #93c5fd;
  font-size: .78rem;
}
.evenchess-universal-levels__features {
  display: grid;
  gap: .35rem;
}
.evenchess-universal-levels__toggle {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: .45rem;
  padding: .42rem .48rem;
  border: 1px solid rgba(59,130,246,.45);
  border-radius: 7px;
  background: rgba(15,38,68,.82);
}
.evenchess-universal-levels__toggle input {
  width: 1.05rem;
  height: 1.05rem;
  accent-color: #2dd4bf;
}
.evenchess-universal-levels__toggle-text {
  display: grid;
  min-width: 0;
}
.evenchess-universal-levels__toggle-label {
  overflow-wrap: anywhere;
  font-weight: 650;
}
.evenchess-universal-panel {
  box-sizing: border-box;
  width: 100%;
  margin-top: .75rem;
  padding: .85rem .95rem;
  color: #dbeafe;
  background: #061426;
  border: 1px solid rgba(56,189,248,.62);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0,0,0,.22);
}
.evenchess-universal-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .75rem;
  margin-bottom: .65rem;
}
.evenchess-universal-panel__title {
  color: #2dd4bf;
  font-weight: 700;
}
.evenchess-universal-panel__badge {
  flex: 0 0 auto;
  padding: .18rem .5rem;
  border: 1px solid rgba(45,212,191,.8);
  border-radius: 999px;
  color: #e0f2fe;
  font-size: .78rem;
  font-weight: 700;
}
.evenchess-universal-panel__item + .evenchess-universal-panel__item {
  margin-top: .75rem;
  padding-top: .75rem;
  border-top: 1px solid rgba(148,163,184,.22);
}
.evenchess-universal-panel__item-title {
  margin: 0 0 .35rem;
  color: #f8fafc;
  font-size: .95rem;
  font-weight: 650;
}
.evenchess-universal-panel__item-body,
.evenchess-universal-panel__empty {
  margin: 0;
  color: #cbd5e1;
  line-height: 1.45;
}`;
  document.head.append(style);
}

function candidateIndexFromVisual(visual: EvenChessPayloadVisual): number | undefined {
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
    text.includes('own hanging') ||
    text.includes('own') ||
    text.includes('your')
  );
}

function isOpponentAttackableSafetyText(text: string): boolean {
  return (
    text.includes('opponent_hanging_attackable') ||
    text.includes('hanging_attackable.opponent') ||
    text.includes('opponent hanging') ||
    text.includes('opponent piece') ||
    text.includes('opponent')
  );
}

function boardLabelText(label: string): string {
  return label.trim().replace(/\s+/g, ' ').slice(0, 28);
}

function cleanFen(fen: string | undefined): string | undefined {
  const clean = fen?.replace(/_/g, ' ').trim();
  return clean && clean.split(/\s+/).length >= 2 ? clean : undefined;
}

function asKey(value: string): Key {
  return value.toLowerCase() as Key;
}

function safeId(value: string): string {
  return value.replace(/[^A-Za-z0-9_.:-]/g, '-').slice(0, 80) || 'evenchess-board';
}

function hashText(value: string): string {
  let hash = 0;
  for (let i = 0; i < value.length; i++) hash = (hash * 31 + value.charCodeAt(i)) | 0;
  return Math.abs(hash).toString(36);
}

function dedupe<T>(items: T[], key: (item: T) => string): T[] {
  const seen = new Set<string>();
  return items.filter(item => {
    const id = key(item);
    if (seen.has(id)) return false;
    seen.add(id);
    return true;
  });
}

function fixed(value: number): string {
  return value.toFixed(3).replace(/\.?0+$/, '');
}
