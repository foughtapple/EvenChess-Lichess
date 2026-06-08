import type { GameMode, GameType, EvenChessTokenBalance } from './interfaces';

export const evenChessMinLevel = 0;
export const evenChessMaxLevel = 10;
export const evenChessDefaultSetLevel = 5;
export const evenChessComputerSetLevel = 10;

export const evenChessSetLevelForGameType = (
  gameType: GameType | null,
  storedLevel: number | undefined,
): number => (gameType === 'ai' ? evenChessComputerSetLevel : (storedLevel ?? evenChessDefaultSetLevel));

export interface EvenChessLobbyActionGate {
  playban?: boolean;
  hasUnreadLichessMessage?: boolean;
  isBot?: boolean;
  hasOngoingRealTimeGame?: boolean;
}

export const evenChessLobbyActionDisabled = (
  gameType: GameType | 'dev',
  gate: EvenChessLobbyActionGate,
): boolean => {
  if (gameType !== 'hook') return false;
  return !!(gate.playban || gate.hasUnreadLichessMessage || gate.isBot);
};

export const evenChessLevelValid = (value: string | number | undefined): boolean => {
  const raw = typeof value === 'string' ? value.trim() : value;
  if (raw === undefined || raw === '') return true;
  if (typeof raw === 'string' && raw.toLowerCase() === 'any') return true;
  const level = typeof raw === 'number' ? raw : Number(raw);
  return Number.isInteger(level) && level >= evenChessMinLevel && level <= evenChessMaxLevel;
};

export const evenChessPreferredSetLevelSelected = (value: string | undefined): boolean => {
  const normalized = value?.trim().toLowerCase();
  return !!normalized && normalized !== 'any';
};

export const evenChessPreferredSetLevelParam = (value: string | undefined): string | undefined => {
  const raw = value?.trim();
  if (!raw || raw.toLowerCase() === 'any') return undefined;
  const level = Number(raw);
  return Number.isInteger(level) && level >= evenChessMinLevel && level <= evenChessMaxLevel
    ? raw
    : undefined;
};

export const evenChessTimeControlKeyForPoolId = (poolId: string): 'bullet' | 'blitz' | 'rapid' | 'classical' => {
  const match = /^(\d+)\+(\d+)$/.exec(poolId.trim());
  if (!match) return 'rapid';
  const minutes = Number(match[1]);
  const increment = Number(match[2]);
  const estimatedSeconds = minutes * 60 + increment * 40;
  if (estimatedSeconds < 180) return 'bullet';
  if (estimatedSeconds < 480) return 'blitz';
  if (estimatedSeconds < 1500) return 'rapid';
  return 'classical';
};

export const evenChessClockParamsForPoolId = (
  poolId: string,
): { clockLimitSeconds: string; clockIncrementSeconds: string } | undefined => {
  const match = /^(\d+)\+(\d+)$/.exec(poolId.trim());
  if (!match) return undefined;
  const minutes = Number(match[1]);
  const increment = Number(match[2]);
  return Number.isFinite(minutes) && Number.isFinite(increment)
    ? {
        clockLimitSeconds: Math.round(minutes * 60).toString(),
        clockIncrementSeconds: Math.round(increment).toString(),
      }
    : undefined;
};

export interface EvenChessPendingPoolInput {
  gameType: GameType | null;
  variant: VariantKey;
  gameMode: GameMode;
  color: string;
  isRealTime: boolean;
  clock: string;
  poolIds: string[];
}

export const evenChessPendingPoolId = ({
  gameType,
  variant,
  gameMode,
  color,
  isRealTime,
  clock,
  poolIds,
}: EvenChessPendingPoolInput): string | undefined =>
  color === 'random' &&
  gameType === 'hook' &&
  variant === 'standard' &&
  gameMode === 'rated' &&
  isRealTime &&
  poolIds.includes(clock)
    ? clock
    : undefined;

const truthyDebugFlag = (value: string | null | undefined): boolean =>
  ['1', 'true', 'yes', 'on'].includes(value?.trim().toLowerCase() ?? '');

const storedEvenChessSearchDebugFlag = (): string | undefined => {
  try {
    return (
      globalThis.localStorage?.getItem('evenchess.searchStatusDebug') ??
      globalThis.localStorage?.getItem('evenchess.search.debug') ??
      undefined
    );
  } catch {
    return undefined;
  }
};

export const evenChessSearchStatusDebugEnabled = (
  search = globalThis.location?.search ?? '',
  storedFlag = storedEvenChessSearchDebugFlag(),
): boolean => {
  const params = new URLSearchParams(search);
  return (
    truthyDebugFlag(params.get('evenchessSearchDebug')) ||
    truthyDebugFlag(params.get('evenChessSearchDebug')) ||
    truthyDebugFlag(params.get('debugEvenChessSearch')) ||
    truthyDebugFlag(storedFlag)
  );
};

export const evenChessScenarioLabel = (preferredSetLevel: string | undefined): string =>
  evenChessPreferredSetLevelSelected(preferredSetLevel)
    ? 'Preferred set level search'
    : 'Normal search';

export const evenChessSubmitMode = (
  gameType: GameType | null,
  gameMode: GameMode,
): 'rated' | 'casual' | 'target' | 'ai' => {
  if (gameType === 'ai') return 'ai';
  if (gameType === 'friend') return 'casual';
  return gameMode;
};

export const evenChessTokenGateText = (
  tokenBalance: EvenChessTokenBalance | undefined,
  mode: 'rated' | 'casual' | 'target' | 'ai',
): string => {
  if (mode === 'ai' || mode === 'target') return 'No game token required for this search mode.';
  if (!tokenBalance) return 'Token or subscription access will be checked on submit.';
  if (tokenBalance.freeMatchTokensActive)
    return 'This search will not consume startup or earned game tokens.';
  if (tokenBalance.subscriptionActive)
    return 'Subscription access available; live help strength is unchanged.';
  return `${tokenBalance.displayCount} game tokens available; failed search does not spend a token.`;
};

export const evenChessTemporaryFreeTokenMessage = (
  tokenBalance: EvenChessTokenBalance | undefined,
  mode: 'rated' | 'casual' | 'target' | 'ai',
): string | undefined =>
  mode === 'rated' || mode === 'casual'
    ? tokenBalance?.freeMatchTokensActive
      ? tokenBalance.freeMatchTokensMessage || 'Tokens are temporarily free'
      : undefined
    : undefined;
