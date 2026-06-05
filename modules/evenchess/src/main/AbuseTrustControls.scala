package lila.evenchess

import ProductInvariants.RequirementClass

object AbuseTrustControls:

  enum PhaseRRequirement:
    case NonPlatformHelpProhibited
    case PublicRulesExplainPlatformOnlyHelp
    case MatchmakingAbuseMonitoring
    case RepeatOpponentCaps
    case StrictPreferenceNotCollusionLoophole
    case TokenFarmingPracticalControls
    case CustomAnalysisRateLimits
    case PaidStatusFairnessBoundary
    case EceCustomInstructionAbuseGuard
    case AiForbiddenWordingValidation
    case StockfishRawOutputNonExposure
    case MajorSystemsFeatureFlagged
    case FairnessAffectingFlagsAudited
    case HealthChecksAndRenderFailures
    case IncidentPauseControls
    case AsymmetricOutageRemedies
    case CampaignCopyKillSwitch
    case LocalDevFlowAndRollback

  final case class PhaseRRequirementClassification(
      requirement: PhaseRRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseRRequirementClassifications:
    val all: List[PhaseRRequirementClassification] = List(
      PhaseRRequirementClassification(
        PhaseRRequirement.NonPlatformHelpProhibited,
        RequirementClass.EvenChessSpecific,
        "Rated EvenChess prohibits external engines, humans, bots, browser extensions, unaudited notes, stream chat, and unaudited analysis."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.PublicRulesExplainPlatformOnlyHelp,
        RequirementClass.AdaptedToLichessFork,
        "Rules and fair-play copy must explain that only disclosed, platform-delivered coaching is legal."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.MatchmakingAbuseMonitoring,
        RequirementClass.EvenChessSpecific,
        "Monitor repeat pairings, collusion, rating transfer, target manipulation, abort abuse, and queue sniping."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.RepeatOpponentCaps,
        RequirementClass.EvenChessSpecific,
        "MMR integration must be able to cap or flag repeat opponents before game creation."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.StrictPreferenceNotCollusionLoophole,
        RequirementClass.EvenChessSpecific,
        "Strict preference search cannot bypass collusion, repeat-pair, or target-manipulation controls."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.TokenFarmingPracticalControls,
        RequirementClass.EvenChessSpecific,
        "Token farming controls use caps, cooldowns, and audit signals without enabling intrusive MVP controls."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.CustomAnalysisRateLimits,
        RequirementClass.EvenChessSpecific,
        "High-cost L10 and full-game custom analysis are rate-limited and token-gated server-side."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.PaidStatusFairnessBoundary,
        RequirementClass.EvenChessSpecific,
        "Paid status cannot change live rated fairness, Set Level authority, ECR, or stronger live help."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.EceCustomInstructionAbuseGuard,
        RequirementClass.EvenChessSpecific,
        "ECE custom instructions remain numeric profile controls and reject hidden, forbidden, or higher-level requests."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.AiForbiddenWordingValidation,
        RequirementClass.EvenChessSpecific,
        "AI output is validated for forbidden best-move or higher-level wording before display."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.StockfishRawOutputNonExposure,
        RequirementClass.EvenChessSpecific,
        "Raw Stockfish and provider output never reaches public payloads or browser diagnostics."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.MajorSystemsFeatureFlagged,
        RequirementClass.EvenChessSpecific,
        "MMR, ECE live calls, Display Engine overlays, AI summaries, proposed move, full-game, token gates, and ads have feature flags."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.FairnessAffectingFlagsAudited,
        RequirementClass.EvenChessSpecific,
        "Feature flags must not silently change rated fairness without audit id, reason, and policy version."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.HealthChecksAndRenderFailures,
        RequirementClass.EvenChessSpecific,
        "Monitor ECE, Stockfish, AI, queues, tokens, custom analysis, rating settlement, stale overlays, and render failures."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.IncidentPauseControls,
        RequirementClass.EvenChessSpecific,
        "Operators can pause ECE live help, AI summaries, proposed-move analysis, custom review, ads, tokens, and paid promotions."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.AsymmetricOutageRemedies,
        RequirementClass.EvenChessSpecific,
        "Asymmetric assistance outages can produce no-rate, annul, or review-only incident plans."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.CampaignCopyKillSwitch,
        RequirementClass.EvenChessSpecific,
        "Campaigns pause when copy implies cheating, hidden engine use, or pay-to-win help."
      ),
      PhaseRRequirementClassification(
        PhaseRRequirement.LocalDevFlowAndRollback,
        RequirementClass.AdaptedToLichessFork,
        "Operational rollout preserves lila-docker local dev flow and records rollback/test notes per phase."
      )
    )

  enum AbuseRequirement:
    case NonPlatformGuidanceProhibited
    case LegalPlatformVsExternalHelp
    case AbuseControlsDoNotSilentlyChangeFairness
    case MvpInvasiveControlsExcluded
    case InvasiveControlsNeedApproval
    case FairPlayReportContext
    case ModerationInternalProtection
    case ProbeBudgetsAndAuditedSuppression
    case ReuseLilaModerationPatterns

  final case class AbuseRequirementClassification(
      requirement: AbuseRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object AbuseRequirementClassifications:
    val all: List[AbuseRequirementClassification] = List(
      AbuseRequirementClassification(
        AbuseRequirement.NonPlatformGuidanceProhibited,
        RequirementClass.EvenChessSpecific,
        "External engines, humans, bots, notes, extensions, stream chat, and unaudited analysis remain prohibited in rated EvenChess."
      ),
      AbuseRequirementClassification(
        AbuseRequirement.LegalPlatformVsExternalHelp,
        RequirementClass.EvenChessSpecific,
        "Legal help is disclosed, platform-delivered, Set Level capped, audited, and priced into ECR."
      ),
      AbuseRequirementClassification(
        AbuseRequirement.AbuseControlsDoNotSilentlyChangeFairness,
        RequirementClass.EvenChessSpecific,
        "Abuse controls can suppress, review, no-rate, or annul only with explicit audited action state."
      ),
      AbuseRequirementClassification(
        AbuseRequirement.MvpInvasiveControlsExcluded,
        RequirementClass.UnresolvedProductOwnerDecision,
        "Phone verification, device/session scoring, same-IP limits, and high-risk cluster delays are excluded unless approved."
      ),
      AbuseRequirementClassification(
        AbuseRequirement.InvasiveControlsNeedApproval,
        RequirementClass.UnresolvedProductOwnerDecision,
        "More invasive controls need product-owner approval and privacy review."
      ),
      AbuseRequirementClassification(
        AbuseRequirement.FairPlayReportContext,
        RequirementClass.AdaptedToLichessFork,
        "Fair-play reports should link Lichess game/player context with EvenChess coaching ledger summary."
      ),
      AbuseRequirementClassification(
        AbuseRequirement.ModerationInternalProtection,
        RequirementClass.AdaptedToLichessFork,
        "Moderation should use existing lila patterns while avoiding unnecessary anti-cheat internals exposure."
      ),
      AbuseRequirementClassification(
        AbuseRequirement.ProbeBudgetsAndAuditedSuppression,
        RequirementClass.EvenChessSpecific,
        "High-volume probing, hover, and reveal patterns are logged and may be capped with audited suppression."
      ),
      AbuseRequirementClassification(
        AbuseRequirement.ReuseLilaModerationPatterns,
        RequirementClass.AdaptedToLichessFork,
        "Use existing lila moderation patterns where possible before adding new trust-control services."
      )
    )

  enum GuidanceSource:
    case PlatformCoaching
    case ExternalEngine
    case HumanCoach
    case Friend
    case StreamChat
    case Notes
    case BrowserExtension
    case Bot
    case UnauditedAnalysis

  final case class GuidanceContext(
      source: GuidanceSource,
      disclosedToOpponent: Boolean,
      setLevelCapped: Boolean,
      serverAudited: Boolean,
      pricedIntoEcr: Boolean
  ):
    def legalInRatedEvenChess: Boolean =
      source == GuidanceSource.PlatformCoaching &&
        disclosedToOpponent &&
        setLevelCapped &&
        serverAudited &&
        pricedIntoEcr

  enum ExploitType:
    case Sandbagging
    case Collusion
    case MultiAccounting
    case NonPlatformGuidance
    case CandidateFollowAbuse
    case PromptInjection
    case SubscriptionAbuse
    case TargetAbuse
    case RematchAbuse
    case AbortAbuse
    case EngineOutage
    case AiTimeout
    case Desync
    case StaleCoaching
    case MobileUiFailure
    case AdTokenFarming
    case PremiumMisrepresentation

  enum MitigationAction:
    case RatingFloor
    case Review
    case LeaderboardRestriction
    case PairCap
    case GraphAnalysis
    case VerificationIfNeeded
    case ManualReview
    case FairPlayReport
    case CandidateBudget
    case Cooldown
    case InputIsolation
    case OutputValidation
    case AccountBasedQuota
    case SeparateTargetPool
    case RepeatPairLimit
    case NoRate
    case Annul
    case Pause
    case Downgrade
    case Suppress
    case ServerResync
    case ClearOnMove
    case TtlExpiry
    case MobileQa
    case PersistentBadge
    case TokenCap
    case CopyGuardrail
    case KillSwitch

  final case class ExploitRegisterRow(
      exploit: ExploitType,
      risk: String,
      signal: String,
      mitigations: Set[MitigationAction]
  ):
    def complete: Boolean =
      risk.nonEmpty && signal.nonEmpty && mitigations.nonEmpty

  object ExploitRegister:
    val rows: List[ExploitRegisterRow] = List(
      ExploitRegisterRow(ExploitType.Sandbagging, "Rating distortion", "Suspicious losses/rebounds", Set(MitigationAction.RatingFloor, MitigationAction.Review, MitigationAction.LeaderboardRestriction)),
      ExploitRegisterRow(ExploitType.Collusion, "Rating transfer", "Repeated pairs, odd resignations", Set(MitigationAction.PairCap, MitigationAction.GraphAnalysis)),
      ExploitRegisterRow(ExploitType.MultiAccounting, "Evasion/farming", "Account clusters", Set(MitigationAction.VerificationIfNeeded, MitigationAction.ManualReview)),
      ExploitRegisterRow(ExploitType.NonPlatformGuidance, "Unpriced help", "Move match, timing, reports", Set(MitigationAction.FairPlayReport, MitigationAction.Review)),
      ExploitRegisterRow(ExploitType.CandidateFollowAbuse, "Hidden engine-like guidance", "Hover/probe volume", Set(MitigationAction.CandidateBudget, MitigationAction.Cooldown)),
      ExploitRegisterRow(ExploitType.PromptInjection, "AI policy bypass", "Validator failures", Set(MitigationAction.InputIsolation, MitigationAction.OutputValidation)),
      ExploitRegisterRow(ExploitType.SubscriptionAbuse, "Paid misuse", "Sharing/quota anomalies", Set(MitigationAction.AccountBasedQuota)),
      ExploitRegisterRow(ExploitType.TargetAbuse, "ECR corruption", "Target residuals", Set(MitigationAction.SeparateTargetPool)),
      ExploitRegisterRow(ExploitType.RematchAbuse, "Rating farming", "Repeated games", Set(MitigationAction.RepeatPairLimit)),
      ExploitRegisterRow(ExploitType.AbortAbuse, "Queue/token gaming", "Abort clusters", Set(MitigationAction.Cooldown)),
      ExploitRegisterRow(ExploitType.EngineOutage, "Asymmetric fairness", "Health difference", Set(MitigationAction.Pause, MitigationAction.Downgrade, MitigationAction.NoRate, MitigationAction.Annul)),
      ExploitRegisterRow(ExploitType.AiTimeout, "Missing/late advice", "Timeout/stale flags", Set(MitigationAction.Suppress)),
      ExploitRegisterRow(ExploitType.Desync, "Wrong advice/clock", "Sequence mismatch", Set(MitigationAction.ServerResync)),
      ExploitRegisterRow(ExploitType.StaleCoaching, "Invalid help", "Board hash mismatch", Set(MitigationAction.ClearOnMove, MitigationAction.TtlExpiry)),
      ExploitRegisterRow(ExploitType.MobileUiFailure, "Disclosure loss", "Confusion/reports", Set(MitigationAction.MobileQa, MitigationAction.PersistentBadge)),
      ExploitRegisterRow(ExploitType.AdTokenFarming, "Token economy abuse", "Ad completions/no play depth", Set(MitigationAction.TokenCap, MitigationAction.Cooldown)),
      ExploitRegisterRow(ExploitType.PremiumMisrepresentation, "Trust/regulatory risk", "Copy scans/complaints", Set(MitigationAction.CopyGuardrail, MitigationAction.KillSwitch))
    )

    val byExploit: Map[ExploitType, ExploitRegisterRow] =
      rows.map(row => row.exploit -> row).toMap

    def coversAllKnownExploits: Boolean =
      ExploitType.values.toSet.subsetOf(byExploit.keySet)

  final case class FairnessValues(
      setLevel: String,
      usedLevel: String,
      assistanceLoad: String,
      usedOffset: String,
      ecrPolicy: String,
      matchmakingPolicy: String,
      stockfishProfile: String,
      aiExactness: String,
      targetIsolation: String
  )

  final case class AbuseControlDecision(
      action: MitigationAction,
      before: FairnessValues,
      after: FairnessValues,
      auditId: String,
      visibleReason: String
  ):
    def fairnessChanged: Boolean = before != after

    def allowed: Boolean =
      !fairnessChanged || (auditId.nonEmpty && visibleReason.nonEmpty)

  object MvpTrustControls:
    val phoneVerificationRequired = false
    val deviceSessionRiskScoringRequired = false
    val sameIpCreationLimitsRequired = false
    val highRiskClusterTokenDelaysRequired = false

  final case class InvasiveControlRequest(
      controlName: String,
      productOwnerApproved: Boolean,
      privacyReviewApproved: Boolean
  ):
    def mayEnable: Boolean =
      controlName.nonEmpty && productOwnerApproved && privacyReviewApproved

  final case class FairPlayReportContext(
      reportId: String,
      gameId: String,
      playerId: String,
      moveRange: String,
      coachingLedgerSummaryId: String,
      visibleAssistanceState: String
  ):
    def complete: Boolean =
      reportId.nonEmpty &&
        gameId.nonEmpty &&
        playerId.nonEmpty &&
        moveRange.nonEmpty &&
        coachingLedgerSummaryId.nonEmpty &&
        visibleAssistanceState.nonEmpty

  final case class ModerationDisclosurePolicy(
      exposesAntiCheatInternals: Boolean,
      usesExistingLilaModerationPatterns: Boolean,
      includesEvenChessLedgerContext: Boolean
  ):
    def safe: Boolean =
      !exposesAntiCheatInternals &&
        usesExistingLilaModerationPatterns &&
        includesEvenChessLedgerContext

  final case class CandidateProbePattern(
      playerId: String,
      hoverCount: Int,
      revealCount: Int,
      requestCount: Int,
      windowSeconds: Int
  ):
    def highVolume(maxEvents: Int, minWindowSeconds: Int): Boolean =
      windowSeconds >= minWindowSeconds && (hoverCount + revealCount + requestCount) > maxEvents

  final case class ProbeControlDecision(
      capped: Boolean,
      cooldown: Boolean,
      suppressionAudited: Boolean,
      auditId: String
  ):
    def valid: Boolean =
      (!capped && !cooldown) || (suppressionAudited && auditId.nonEmpty)

  final case class RuntimeFairnessHealth(
      engineHealthyForBoth: Boolean,
      aiTimedOut: Boolean,
      sequenceMatches: Boolean,
      boardHashMatches: Boolean,
      mobileDisclosureVisible: Boolean
  )

  enum RuntimeTrustRemedy:
    case Continue
    case SuppressAi
    case ServerResync
    case ClearStaleCoaching
    case NoRate
    case Annul
    case Pause

  object RuntimeTrustPolicy:
    def remedy(health: RuntimeFairnessHealth): RuntimeTrustRemedy =
      if !health.engineHealthyForBoth then RuntimeTrustRemedy.NoRate
      else if health.aiTimedOut then RuntimeTrustRemedy.SuppressAi
      else if !health.sequenceMatches then RuntimeTrustRemedy.ServerResync
      else if !health.boardHashMatches then RuntimeTrustRemedy.ClearStaleCoaching
      else if !health.mobileDisclosureVisible then RuntimeTrustRemedy.Pause
      else RuntimeTrustRemedy.Continue

  final case class MatchmakingAbuseSignal(
      playerId: String,
      opponentId: String,
      repeatPairingsInWindow: Int,
      collusionScore: Double,
      ratingTransferScore: Double,
      targetLevelManipulationScore: Double,
      abortCountInWindow: Int,
      queueSnipeScore: Double,
      strictPreferenceSearch: Boolean,
      auditId: String
  ):
    def valid: Boolean =
      playerId.nonEmpty &&
        opponentId.nonEmpty &&
        playerId != opponentId &&
        repeatPairingsInWindow >= 0 &&
        List(collusionScore, ratingTransferScore, targetLevelManipulationScore, queueSnipeScore).forall(score => score >= 0.0 && score <= 1.0) &&
        abortCountInWindow >= 0

    def suspicious: Boolean =
      repeatPairingsInWindow > RepeatOpponentPolicy.default.maxPairingsPerWindow ||
        (strictPreferenceSearch && repeatPairingsInWindow > RepeatOpponentPolicy.default.strictPreferenceMaxPairingsPerWindow) ||
        collusionScore >= 0.7 ||
        ratingTransferScore >= 0.7 ||
        targetLevelManipulationScore >= 0.7 ||
        abortCountInWindow >= 3 ||
        queueSnipeScore >= 0.7

    def auditedIfSuspicious: Boolean =
      !suspicious || auditId.nonEmpty

  final case class RepeatOpponentPolicy(
      maxPairingsPerWindow: Int,
      strictPreferenceMaxPairingsPerWindow: Int,
      windowSeconds: Int
  ):
    def valid: Boolean =
      maxPairingsPerWindow > 0 &&
        strictPreferenceMaxPairingsPerWindow > 0 &&
        strictPreferenceMaxPairingsPerWindow <= maxPairingsPerWindow &&
        windowSeconds > 0

    def caps(signal: MatchmakingAbuseSignal): Boolean =
      signal.repeatPairingsInWindow > maxPairingsPerWindow ||
        (signal.strictPreferenceSearch && signal.repeatPairingsInWindow > strictPreferenceMaxPairingsPerWindow)

  object RepeatOpponentPolicy:
    val default: RepeatOpponentPolicy =
      RepeatOpponentPolicy(maxPairingsPerWindow = 3, strictPreferenceMaxPairingsPerWindow = 1, windowSeconds = 86_400)

  final case class MatchmakingTrustDecision(
      allowed: Boolean,
      mitigations: Set[MitigationAction],
      requiresManualReview: Boolean,
      auditId: String,
      visibleReason: String
  ):
    def valid: Boolean =
      mitigations.nonEmpty &&
        (!requiresManualReview || mitigations.contains(MitigationAction.ManualReview)) &&
        visibleReason.nonEmpty &&
        auditId.nonEmpty

  object MatchmakingTrustPolicy:
    def decide(signal: MatchmakingAbuseSignal, policy: RepeatOpponentPolicy): MatchmakingTrustDecision =
      val mitigations =
        List(
          Option.when(policy.caps(signal))(Set(MitigationAction.RepeatPairLimit, MitigationAction.PairCap)),
          Option.when(signal.collusionScore >= 0.7 || signal.ratingTransferScore >= 0.7)(Set(MitigationAction.GraphAnalysis, MitigationAction.ManualReview)),
          Option.when(signal.targetLevelManipulationScore >= 0.7)(Set(MitigationAction.SeparateTargetPool, MitigationAction.ManualReview)),
          Option.when(signal.abortCountInWindow >= 3)(Set(MitigationAction.Cooldown)),
          Option.when(signal.queueSnipeScore >= 0.7)(Set(MitigationAction.Review))
        ).flatten.flatten.toSet

      if mitigations.isEmpty then
        MatchmakingTrustDecision(allowed = true, Set(MitigationAction.Review), requiresManualReview = false, signal.auditId, "matchmaking_trust_clear")
      else
        MatchmakingTrustDecision(
          allowed = false,
          mitigations = mitigations,
          requiresManualReview = mitigations.contains(MitigationAction.ManualReview),
          auditId = signal.auditId,
          visibleReason = "matchmaking_abuse_guard"
        )

  final case class TokenReviewAbuseLimits(
      adTokenDailyCap: Int,
      adTokenCooldownSeconds: Int,
      customL10DailyCap: Int,
      fullGameDailyCap: Int
  ):
    def valid: Boolean =
      adTokenDailyCap >= 0 &&
        adTokenCooldownSeconds >= 0 &&
        customL10DailyCap > 0 &&
        fullGameDailyCap > 0

    def allowAdGrant(grantsToday: Int, cooldownActive: Boolean): Boolean =
      grantsToday < adTokenDailyCap && !cooldownActive

    def allowCustomAnalysis(isL10: Boolean, usedToday: Int): Boolean =
      !isL10 || usedToday < customL10DailyCap

    def allowFullGameAnalysis(usedToday: Int): Boolean =
      usedToday < fullGameDailyCap

  object TokenReviewAbuseLimits:
    val default: TokenReviewAbuseLimits =
      TokenReviewAbuseLimits(adTokenDailyCap = 3, adTokenCooldownSeconds = 300, customL10DailyCap = 5, fullGameDailyCap = 10)

  final case class EngineAiAbuseGuard(
      customInstructionsNumericOnly: Boolean,
      rejectsHiddenInfoRequests: Boolean,
      rejectsForbiddenOrHigherLevelRequests: Boolean,
      validatesForbiddenBestMoveWording: Boolean,
      stockfishRawOutputExposed: Boolean,
      providerSecretsExposed: Boolean,
      rawProviderPayloadExposed: Boolean
  ):
    def valid: Boolean =
      customInstructionsNumericOnly &&
        rejectsHiddenInfoRequests &&
        rejectsForbiddenOrHigherLevelRequests &&
        validatesForbiddenBestMoveWording &&
        !stockfishRawOutputExposed &&
        !providerSecretsExposed &&
        !rawProviderPayloadExposed

  object EngineAiAbuseGuard:
    val default: EngineAiAbuseGuard =
      EngineAiAbuseGuard(
        customInstructionsNumericOnly = true,
        rejectsHiddenInfoRequests = true,
        rejectsForbiddenOrHigherLevelRequests = true,
        validatesForbiddenBestMoveWording = true,
        stockfishRawOutputExposed = false,
        providerSecretsExposed = false,
        rawProviderPayloadExposed = false
      )

  enum OperationalFeature:
    case MmrEngine
    case EceLiveCalls
    case DisplayEngineOverlays
    case AiSummaries
    case ProposedMoveMode
    case FullGameMode
    case TokenGates
    case Ads

  final case class OperationalFeatureFlag(
      feature: OperationalFeature,
      enabled: Boolean,
      policyVersion: String,
      auditId: String,
      reason: String,
      changesRatedFairness: Boolean
  ):
    def valid: Boolean =
      policyVersion.nonEmpty &&
        (!changesRatedFairness || (auditId.nonEmpty && reason.nonEmpty))

  object OperationalFeatureFlags:
    def coversMajorSystems(flags: List[OperationalFeatureFlag]): Boolean =
      OperationalFeature.values.toSet.subsetOf(flags.map(_.feature).toSet)

    def allAuditedWhenFairnessAffecting(flags: List[OperationalFeatureFlag]): Boolean =
      flags.forall(_.valid)

  final case class OperationalHealthSnapshot(
      eceLatencyMillis: Int,
      stockfishLatencyMillis: Int,
      aiLatencyMillis: Int,
      aiCostCents: Int,
      aiFallbackCount: Int,
      queueMillis: Int,
      tokenFlowHealthy: Boolean,
      customAnalysisBacklog: Int,
      ratingSettlementHealthy: Boolean,
      overlayStalePayloadErrors: Int,
      displayRenderFailures: Int,
      collectedAt: Long,
      schemaVersion: String
  ):
    def valid: Boolean =
      List(eceLatencyMillis, stockfishLatencyMillis, aiLatencyMillis, aiCostCents, aiFallbackCount, queueMillis, customAnalysisBacklog, overlayStalePayloadErrors, displayRenderFailures).forall(_ >= 0) &&
        collectedAt > 0 &&
        schemaVersion.nonEmpty

    def degraded: Boolean =
      eceLatencyMillis > 2_000 ||
        stockfishLatencyMillis > 2_000 ||
        aiLatencyMillis > 4_000 ||
        aiFallbackCount > 0 ||
        queueMillis > 30_000 ||
        !tokenFlowHealthy ||
        customAnalysisBacklog > 50 ||
        !ratingSettlementHealthy ||
        overlayStalePayloadErrors > 0 ||
        displayRenderFailures > 0

  enum PauseTarget:
    case EceLiveHelp
    case AiSummaries
    case ProposedMoveAnalysis
    case CustomReview
    case Ads
    case Tokens
    case PaidPromotions

  enum IncidentOutcome:
    case PauseOnly
    case ReviewAffectedGames
    case NoRateAffectedGames
    case AnnulAffectedGames

  final case class IncidentResponsePlan(
      incidentId: String,
      pauseTargets: Set[PauseTarget],
      outcome: IncidentOutcome,
      publicNotice: String,
      auditId: String,
      rollbackNotes: String,
      testsRun: List[String],
      preservesLocalDevFlow: Boolean
  ):
    def valid: Boolean =
      incidentId.nonEmpty &&
        pauseTargets.nonEmpty &&
        publicNotice.nonEmpty &&
        auditId.nonEmpty &&
        rollbackNotes.nonEmpty &&
        testsRun.nonEmpty &&
        preservesLocalDevFlow

    def pausesRequiredSystems: Boolean =
      Set(
        PauseTarget.EceLiveHelp,
        PauseTarget.AiSummaries,
        PauseTarget.ProposedMoveAnalysis,
        PauseTarget.CustomReview,
        PauseTarget.Ads,
        PauseTarget.Tokens,
        PauseTarget.PaidPromotions
      ).subsetOf(pauseTargets)

  object IncidentResponsePlanner:
    def fromHealth(
        incidentId: String,
        health: OperationalHealthSnapshot,
        asymmetricAssistanceOutage: Boolean,
        auditId: String
    ): IncidentResponsePlan =
      val outcome =
        if asymmetricAssistanceOutage then IncidentOutcome.NoRateAffectedGames
        else if health.degraded then IncidentOutcome.ReviewAffectedGames
        else IncidentOutcome.PauseOnly
      IncidentResponsePlan(
        incidentId = incidentId,
        pauseTargets = Set(
          PauseTarget.EceLiveHelp,
          PauseTarget.AiSummaries,
          PauseTarget.ProposedMoveAnalysis,
          PauseTarget.CustomReview,
          PauseTarget.Ads,
          PauseTarget.Tokens,
          PauseTarget.PaidPromotions
        ),
        outcome = outcome,
        publicNotice = "EvenChess assistance systems are paused while operators review service health.",
        auditId = auditId,
        rollbackNotes = "Disable Phase R flags or remove the incident plan; no upstream Lichess files are changed.",
        testsRun = List("AbuseTrustControlsTest"),
        preservesLocalDevFlow = true
      )

  object CampaignTrustGuard:
    val forbiddenCopyClaims: List[String] = List(
      "cheating allowed",
      "hidden engine",
      "secret engine",
      "pay to win",
      "stronger live help for premium"
    )

    def mustPauseCampaign(copy: String): Boolean =
      val normalized = copy.toLowerCase
      forbiddenCopyClaims.exists(normalized.contains)
