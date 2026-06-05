import type { MoveMetadata as CgMoveMetadata } from '@lichess-org/chessground/types';

import type { ChatOpts as BaseChatOpts, ChatCtrl, ChatPlugin } from 'lib/chat/interfaces';
import type { EvenChessTtsConfig } from 'lib/evenchessTts';
import type { GameData, Status, RoundStep } from 'lib/game';
import type { ClockData } from 'lib/game/clock/clockCtrl';
import * as Prefs from 'lib/prefs';
import type { EnhanceOpts } from 'lib/richText';
import type { NodeCrazy } from 'lib/tree/types';
import type { VNode } from 'lib/view';

import type { CorresClockData } from './corresClock/corresClockCtrl';
import type { RoundSocket } from './socket';

export { type RoundSocket } from './socket';
export { type CorresClockData } from './corresClock/corresClockCtrl';
export type { RoundStep as Step } from 'lib/game';
export type { default as RoundController } from './ctrl';
export type { ClockData } from 'lib/game/clock/clockCtrl';

export interface NvuiPlugin {
  submitMove?: (submitStoredPremove?: boolean) => void;
  playPremove: () => void;
  premoveInput: string;
  render(): VNode;
}

export interface SocketMove {
  u: Uci;
  b?: 1;
}
export interface SocketDrop {
  role: Role;
  pos: Key;
  b?: 1;
}

export interface EventsWithPayload {
  rep: { n: string };
  flag: Color;
  move: SocketMove;
  drop: SocketDrop;
}

export type EventsWithoutPayload =
  | 'moretime'
  | 'berserk'
  | 'rematch-yes'
  | 'rematch-no'
  | 'takeback-yes'
  | 'takeback-no'
  | 'draw-yes'
  | 'draw-no'
  | 'blindfold-yes'
  | 'blindfold-no'
  | 'draw-force'
  | 'bye2'
  | 'resign-force'
  | 'draw-claim'
  | 'resign'
  | 'abort';

export interface RoundSocketSend {
  <K extends keyof EventsWithPayload>(
    type: K,
    data: EventsWithPayload[K],
    opts?: { ackable?: boolean },
    noRetry?: boolean,
  ): void;
  <K extends EventsWithoutPayload>(
    type: K,
    data?: undefined,
    opts?: { ackable?: boolean },
    noRetry?: boolean,
  ): void;
}

export type EncodedDests = string | Record<string, string>;

export interface RoundData extends GameData {
  clock?: ClockData;
  pref: Pref;
  steps: RoundStep[];
  evenchess?: EvenChessRoundData;
  possibleMoves?: EncodedDests;
  possibleDrops?: string;
  forecastCount?: number;
  opponentSignal?: number;
  crazyhouse?: NodeCrazy;
  correspondence?: CorresClockData;
  tv?: Tv;
  userTv?: {
    id: UserId;
  };
  expiration?: Expiration;
  local?: RoundProxy;
  noab?: boolean;
}

export interface EvenChessRoundData {
  coachText?: EvenChessCoachTextSnapshot;
  display?: EvenChessDisplayState;
  live?: EvenChessLiveOverlay;
  potentialMoves?: EvenChessPotentialMoveState;
  proposedMove?: EvenChessProposedMoveState;
  testGround?: EvenChessTestGroundState;
  tts?: EvenChessTtsConfig;
}

export interface EvenChessDisplayState {
  initializedForGameId?: string;
  preferredUsedLevel?: number;
  setLevel?: number;
  usedLevel?: number;
  toggles?: EvenChessDisplayToggles;
}

export interface EvenChessPreferenceConfig {
  defaultSetLevel?: number;
  defaultFeatureToggles?: EvenChessLevelFeatureToggles;
  preferredUsedLevel?: number;
  ttsEnabled?: boolean;
  ttsAutoSpeak?: boolean;
  ttsAutoDelaySeconds?: number;
  ttsVoice?: string;
  ttsRatePercent?: number;
  ttsVolumePercent?: number;
  ttsQueueBehavior?: 'replace-current' | 'queue';
  ttsMuteDuringOpponentTurn?: boolean;
}

export type EvenChessLevelFeatureKey =
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

export type EvenChessLevelFeatureToggles = Partial<Record<EvenChessLevelFeatureKey, boolean>>;

export interface EvenChessDisplayToggles {
  coachCards: boolean;
  boardVisuals: boolean;
  appliedLevel?: number;
  levelFeatures?: EvenChessLevelFeatureToggles;
}

export interface EvenChessCoachTextSnapshot {
  card: EvenChessCoachCard;
  overlayAuditId: string;
  overlayServerAuthorized: boolean;
  capturedPly: number;
  capturedBoardStateKey: string;
  updatedAt?: number;
}

export interface EvenChessTestGroundState {
  enabled: boolean;
  level: number;
  status: 'loading' | 'ready' | 'unavailable';
  message: string;
  requestedAt?: number;
  updatedAt?: number;
}

export interface EvenChessProposedMoveState {
  status: 'idle' | 'loading' | 'ready' | 'error';
  message?: string;
  activeKey?: string;
  active?: EvenChessProposedMoveCard;
  baseOverlay?: EvenChessLiveOverlay;
  cache?: Record<string, EvenChessProposedMoveCard>;
  consumedByTurn?: Record<string, string>;
  consumed?: number;
  quota?: number;
  updatedAt?: number;
}

export type EvenChessPotentialMoveKind = 'opponent' | 'player';

export interface EvenChessPotentialMoveState {
  status: 'idle' | 'loading' | 'ready' | 'error';
  message?: string;
  activeKey?: string;
  activeKind?: EvenChessPotentialMoveKind;
  active?: EvenChessPotentialMoveReveal;
  cache?: Record<string, EvenChessPotentialMoveReveal>;
  consumedByKey?: Record<string, true>;
  consumedByKind?: Partial<Record<EvenChessPotentialMoveKind, number>>;
  updatedAt?: number;
}

export interface EvenChessPotentialMoveReveal {
  key: string;
  gameId: string;
  playerId?: string;
  ply: number;
  boardStateKey: string;
  perspective: Color;
  kind: EvenChessPotentialMoveKind;
  level: number;
  quota: number;
  consumed: number;
  cards: EvenChessCoachCard[];
  visuals: EvenChessBoardVisual[];
  auditId: string;
  serverAuthorized: boolean;
  approvedDisplayPayload: boolean;
  cached?: boolean;
  createdAt?: number;
}

export interface EvenChessProposedMoveCard {
  key: string;
  gameId: string;
  playerId?: string;
  ply: number;
  boardStateKey: string;
  perspective: Color;
  moveUci: string;
  san?: string;
  legal?: boolean;
  postMoveBoardStateKey?: string;
  level: number;
  title: string;
  body: string;
  source?: string;
  cards?: EvenChessCoachCard[];
  visuals?: EvenChessBoardVisual[];
  auditId: string;
  serverAuthorized: boolean;
  approvedDisplayPayload: boolean;
  cached?: boolean;
  createdAt?: number;
}

export interface EvenChessLiveOverlay {
  enabled: boolean;
  gameId: string;
  ply: number;
  boardStateKey: string;
  perspective: Color;
  auditId: string;
  serverAuthorized: boolean;
  ttlMillis: number;
  stale?: boolean;
  createdAt?: number;
  expiresAt?: number;
  cards?: EvenChessCoachCard[];
  visuals?: EvenChessBoardVisual[];
  clear?: EvenChessClearInstruction[];
  assistance?: EvenChessAssistanceUsage;
}

export interface EvenChessAssistanceUsage {
  proposedMove?: {
    consumed: number;
    quota: number;
  };
  potentialMoves?: {
    consumedByKind?: Partial<Record<EvenChessPotentialMoveKind, number>>;
    quotaByKind?: Partial<Record<EvenChessPotentialMoveKind, number>>;
  };
}

export interface EvenChessCoachCard {
  id: string;
  gameId: string;
  ply: number;
  boardStateKey: string;
  featureKey: string;
  title: string;
  body: string;
  level: number;
  auditId: string;
  defaultActive?: boolean;
  visibility?: string;
  serverAuthorized: boolean;
  approvedDisplayPayload: boolean;
  stale?: boolean;
  ttlMillis?: number;
  rawStockfishLine?: string;
  hiddenDebugData?: string;
  ttsText?: string;
}

export interface EvenChessBoardVisual {
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
  evalCpWhite?: number;
  evalMateWhite?: number;
  evalWinWhite?: number;
  evalDrawWhite?: number;
  evalLossWhite?: number;
  evalSource?: string;
}

export interface EvenChessClearInstruction {
  gameId: string;
  ply: number;
  boardStateKey: string;
  reason: string;
  auditId: string;
}

export interface Expiration {
  idleMillis: number;
  movedAt: number;
  millisToMove: number;
}

export interface Tv {
  channel: string;
  flip: boolean;
}

export interface RoundProxy extends RoundSocket {
  analyse(): void;
  newOpponent(): void;
}

export interface RoundOpts {
  data: RoundData;
  userId?: string;
  noab?: boolean;
  socketSend?: RoundSocketSend;
  onChange(d: RoundData): void;
  element?: HTMLElement;
  crosstableEl?: HTMLElement;
  chat?: ChatOpts;
}

export interface ChatOpts extends BaseChatOpts {
  preset?: 'start' | 'end';
  enhance?: EnhanceOpts;
  plugin?: ChatPlugin;
  alwaysEnabled: boolean;
  noteId?: string;
  noteAge?: number;
  noteText?: string;
  instance?: ChatCtrl;
}

export interface ApiMove {
  dests: string | Record<string, string>;
  ply: number;
  fen: string;
  san: string;
  uci: string;
  clock?: {
    white: Seconds;
    black: Seconds;
    lag?: Centis;
  };
  status?: Status;
  winner?: Color;
  check?: boolean;
  threefold?: boolean;
  fiftyMoves?: boolean;
  wDraw?: boolean;
  bDraw?: boolean;
  crazyhouse?: NodeCrazy;
  role?: Role;
  drops?: string;
  promotion?: {
    key: Key;
    pieceClass: Role;
  };
  castle?: {
    king: [Key, Key];
    rook: [Key, Key];
    color: Color;
  };
  isMove?: true;
  isDrop?: true;
  volume?: number;
}

export interface ApiEnd {
  winner?: Color;
  status: Status;
  ratingDiff?: {
    white: number;
    black: number;
  };
  boosted: boolean;
  clock?: {
    wc: Centis;
    bc: Centis;
  };
}

export interface Pref {
  animationDuration: number;
  autoQueen: Prefs.AutoQueen;
  blindfold: boolean;
  clockBar: boolean;
  clockSound: boolean;
  clockTenths: Prefs.ShowClockTenths;
  confirmResign: boolean;
  coords: Prefs.Coords;
  destination: boolean;
  enablePremove: boolean;
  evenchess?: EvenChessPreferenceConfig;
  highlight: boolean;
  is3d: boolean;
  keyboardMove: boolean;
  voiceMove: boolean;
  moveEvent: Prefs.MoveEvent;
  ratings: boolean;
  replay: Prefs.Replay;
  rookCastle?: boolean;
  showCaptured: boolean;
  submitMove: boolean;
  resizeHandle: Prefs.ShowResizeHandle;
}

export interface MoveMetadata extends CgMoveMetadata {
  preConfirmed?: boolean;
  justDropped?: Role;
  justCaptured?: Piece;
}

export interface RoundTour {
  corresRematchOffline: () => void;
}
