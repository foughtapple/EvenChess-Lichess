import type { EncodedDests } from './interfaces';
import * as util from './util';

export const evenChessTestGroundMoveEvent = 'evenchess:testground:move';
export const evenChessTestGroundMoveResultEvent = 'evenchess:testground:move-result';
export const evenChessTestGroundMoveQueryParam = 'evenchessTestMove';

export interface EvenChessTestGroundMoveResult {
  status: 'accepted' | 'rejected';
  uci?: string;
  reason?: string;
  gameId?: string;
  ply?: number;
}

interface EvenChessTestGroundMoveAccepted {
  ok: true;
  uci: string;
  orig: Key;
  dest: Key;
  promotion?: Role;
}

interface EvenChessTestGroundMoveRejected {
  ok: false;
  uci?: string;
  reason: string;
}

export type EvenChessTestGroundMoveValidation =
  | EvenChessTestGroundMoveAccepted
  | EvenChessTestGroundMoveRejected;

export function isEvenChessTestGroundMoveBridgeAllowed(location: Pick<Location, 'hostname' | 'protocol'>): boolean {
  const hostname = location.hostname.toLowerCase().replace(/^\[|\]$/g, '');
  return (
    (location.protocol === 'http:' || location.protocol === 'https:') &&
    (hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1' || hostname.endsWith('.localhost'))
  );
}

export function readEvenChessTestGroundMoveUci(detail: unknown): string | undefined {
  if (typeof detail === 'string') return detail;
  if (!detail || typeof detail !== 'object') return undefined;
  const record = detail as Record<string, unknown>;
  const raw = record.uci ?? record.moveUci;
  return typeof raw === 'string' ? raw : undefined;
}

export function readEvenChessTestGroundMoveFromLocation(location: Pick<Location, 'search'>): string | undefined {
  const raw = new URLSearchParams(location.search).get(evenChessTestGroundMoveQueryParam);
  return raw || undefined;
}

export function validateEvenChessTestGroundMove(
  rawUci: unknown,
  possibleMoves?: EncodedDests,
): EvenChessTestGroundMoveValidation {
  const uci = typeof rawUci === 'string' ? rawUci.trim().toLowerCase() : '';
  if (!/^[a-h][1-8][a-h][1-8][qrbn]?$/.test(uci)) return { ok: false, reason: 'invalid_uci' };

  const orig = uci.slice(0, 2) as Key;
  const dest = uci.slice(2, 4) as Key;
  const legalDests = util.parsePossibleMoves(possibleMoves).get(orig);
  if (!legalDests?.includes(dest)) return { ok: false, uci, reason: 'illegal_move' };

  const promotion = promotionRole(uci[4]);
  return promotion ? { ok: true, uci, orig, dest, promotion } : { ok: true, uci, orig, dest };
}

function promotionRole(promotion: string | undefined): Role | undefined {
  switch (promotion) {
    case 'q':
      return 'queen';
    case 'r':
      return 'rook';
    case 'b':
      return 'bishop';
    case 'n':
      return 'knight';
    default:
      return undefined;
  }
}
