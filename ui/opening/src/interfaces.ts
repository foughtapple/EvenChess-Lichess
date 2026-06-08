import type { EvenChessTtsConfig } from 'lib/evenchessTts';

export interface OpeningPage {
  history: number[];
  sans: string[];
  evenchess?: EvenChessOpeningData;
}

export interface EvenChessOpeningData {
  openingAi?: EvenChessOpeningAiPayload;
  tts?: EvenChessTtsConfig;
}

export interface EvenChessOpeningSourceFact {
  factId: string;
  text?: string;
  boardStateKey: string;
  exactnessClass?: string;
  auditTag?: string;
}

export interface EvenChessOpeningAiCard {
  id: string;
  kind: 'chapterSummary' | 'positionExplanation' | 'openingPlan' | 'mistakeTheme' | 'explainMove';
  title: string;
  body: string;
  bullets?: string[];
  sourceFactIds: string[];
  auditId: string;
  serverAuthorized: boolean;
  approvedDisplayPayload: boolean;
  rawEnginePayload?: string;
  hiddenDebugData?: string;
  providerSecret?: string;
  rawPrompt?: string;
  modelLabel?: string;
  ttsText?: string;
}

export interface EvenChessOpeningAiPayload {
  enabled: boolean;
  surface: 'opening';
  contextId: string;
  boardStateKey: string;
  ply: number;
  source?: string;
  ratedLive?: boolean;
  serverAuthorized: boolean;
  policyVersion?: string;
  schemaVersion?: string;
  auditId: string;
  expiresAt?: number;
  sourceFacts?: EvenChessOpeningSourceFact[];
  cards?: EvenChessOpeningAiCard[];
}
