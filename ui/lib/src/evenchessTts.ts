export type EvenChessTtsSurface = 'live' | 'analysis' | 'study' | 'opening';
export type EvenChessTtsProvider = 'browser-speech' | 'server-provider';
export type EvenChessTtsQueueBehavior = 'replace-current' | 'queue';

export interface EvenChessTtsConfig {
  enabled: boolean;
  provider?: EvenChessTtsProvider;
  voice?: string;
  ratePercent?: number;
  volumePercent?: number;
  queueBehavior?: EvenChessTtsQueueBehavior;
  muteDuringOpponentTurn?: boolean;
  autoSpeak?: boolean;
  autoDelaySeconds?: number;
  serverAuthorized?: boolean;
  policyVersion?: string;
  providerSecret?: string;
  rawPrompt?: string;
  rawEnginePayload?: string;
  hiddenDebugData?: string;
}

export interface EvenChessTtsItem {
  id: string;
  surface: EvenChessTtsSurface;
  displayedText: string;
  text?: string;
  auditId: string;
  serverAuthorized: boolean;
  approvedDisplayPayload: boolean;
  ratedLive?: boolean;
  isPlayerTurn?: boolean;
  providerSecret?: string;
  rawPrompt?: string;
  rawEnginePayload?: string;
  rawStockfishLine?: string;
  hiddenDebugData?: string;
}

export type EvenChessTtsSafetyReason =
  | 'disabled'
  | 'unauthorized'
  | 'unsupported-provider'
  | 'unsafe-payload'
  | 'text-mismatch'
  | 'missing-audit'
  | 'muted-opponent-turn'
  | 'unsupported-browser';

export interface EvenChessTtsSpeakResult {
  spoken: boolean;
  reason?: EvenChessTtsSafetyReason;
  auditId?: string;
  auditEvent?: EvenChessTtsAuditEvent;
}

export interface EvenChessTtsAuditEvent {
  sourceAuditId: string;
  surface: EvenChessTtsSurface;
  itemId: string;
  policyVersion?: string;
  charCount: number;
  ratedLive: boolean;
}

export interface EvenChessSpeechDriver {
  supported: boolean;
  cancel(): void;
  speak(text: string, options: EvenChessSpeechOptions): void;
}

export interface EvenChessSpeechOptions {
  voice?: string;
  rate: number;
  volume: number;
}

export function normalizeEvenChessTtsText(text: string): string {
  return text.replace(/\s+/g, ' ').trim();
}

export function shownTtsText(title: string, body: string, bullets: string[] = []): string {
  return normalizeEvenChessTtsText([title, body, ...bullets].filter(Boolean).join(' '));
}

export function ttsSafetyReason(
  config: EvenChessTtsConfig | undefined,
  item: EvenChessTtsItem,
): EvenChessTtsSafetyReason | undefined {
  if (!config?.enabled) return 'disabled';
  if (hasUnsafeTtsFields(config) || hasUnsafeTtsFields(item)) return 'unsafe-payload';
  if ((config.provider ?? 'browser-speech') !== 'browser-speech') return 'unsupported-provider';
  if (!config.serverAuthorized || !item.serverAuthorized || !item.approvedDisplayPayload)
    return 'unauthorized';

  const displayed = normalizeEvenChessTtsText(item.displayedText);
  const speech = normalizeEvenChessTtsText(item.text ?? item.displayedText);

  if (!displayed || !speech) return 'unsafe-payload';
  if (displayed !== speech) return 'text-mismatch';
  if (!item.auditId) return 'missing-audit';
  if (config.muteDuringOpponentTurn && item.surface === 'live' && item.isPlayerTurn === false)
    return 'muted-opponent-turn';

  return undefined;
}

export function shouldOfferEvenChessTts(
  config: EvenChessTtsConfig | undefined,
  item: EvenChessTtsItem,
): boolean {
  return !ttsSafetyReason(config, item);
}

export function ttsAuditEvent(
  config: EvenChessTtsConfig | undefined,
  item: EvenChessTtsItem,
): EvenChessTtsAuditEvent | undefined {
  if (ttsSafetyReason(config, item) || !item.auditId) return undefined;
  return {
    sourceAuditId: item.auditId,
    surface: item.surface,
    itemId: item.id,
    policyVersion: config?.policyVersion,
    charCount: normalizeEvenChessTtsText(item.text ?? item.displayedText).length,
    ratedLive: Boolean(item.ratedLive),
  };
}

export function speakEvenChessTts(
  config: EvenChessTtsConfig | undefined,
  item: EvenChessTtsItem,
  driver: EvenChessSpeechDriver = browserSpeechDriver(),
): EvenChessTtsSpeakResult {
  const reason = ttsSafetyReason(config, item);
  if (reason) return { spoken: false, reason };
  if (!driver.supported) return { spoken: false, reason: 'unsupported-browser' };

  const speechText = normalizeEvenChessTtsText(item.text ?? item.displayedText);
  if ((config?.queueBehavior ?? 'replace-current') === 'replace-current') driver.cancel();
  driver.speak(speechText, {
    voice: config?.voice,
    rate: clampNumber((config?.ratePercent ?? 100) / 100, 0.7, 1.3),
    volume: clampNumber((config?.volumePercent ?? 80) / 100, 0, 1),
  });
  return { spoken: true, auditId: item.auditId, auditEvent: ttsAuditEvent(config, item) };
}

export function browserSpeechDriver(): EvenChessSpeechDriver {
  const synthesis = typeof window === 'undefined' ? undefined : window.speechSynthesis;
  const utteranceCtor =
    typeof SpeechSynthesisUtterance === 'undefined' ? undefined : SpeechSynthesisUtterance;

  return {
    supported: Boolean(synthesis && utteranceCtor),
    cancel: () => synthesis?.cancel(),
    speak: (text, options) => {
      if (!synthesis || !utteranceCtor) return;
      const utterance = new utteranceCtor(text);
      utterance.rate = options.rate;
      utterance.volume = options.volume;
      const voice = findVoice(synthesis, options.voice);
      if (voice) utterance.voice = voice;
      synthesis.speak(utterance);
    },
  };
}

function findVoice(synthesis: SpeechSynthesis, voiceKey?: string): SpeechSynthesisVoice | undefined {
  if (!voiceKey || voiceKey === 'system-default') return undefined;
  return synthesis.getVoices().find(voice => voice.name === voiceKey || voice.lang === voiceKey);
}

function hasUnsafeTtsFields(value: {
  providerSecret?: string;
  rawPrompt?: string;
  rawEnginePayload?: string;
  rawStockfishLine?: string;
  hiddenDebugData?: string;
}): boolean {
  return Boolean(
    value.providerSecret ||
    value.rawPrompt ||
    value.rawEnginePayload ||
    value.rawStockfishLine ||
    value.hiddenDebugData,
  );
}

function clampNumber(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}
