package lila.evenchess

import CoachingLadder.{ ExactnessClass, Level }
import ProductInvariants.RequirementClass

object AiCoachPolicy:

  enum PhaseNAiRequirement:
    case AiExplainsTruthOnly
    case AiCannotBypassLevelGates
    case BoardStateAtMostOneAiCall
    case FullGameAtMostOneNarrativeCall
    case CredentialsServerSideOnly
    case InvalidOutputFallsBackDeterministically

  final case class PhaseNAiRequirementClassification(
      requirement: PhaseNAiRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseNAiRequirementClassifications:
    val all: List[PhaseNAiRequirementClassification] = List(
      PhaseNAiRequirementClassification(
        PhaseNAiRequirement.AiExplainsTruthOnly,
        RequirementClass.EvenChessSpecific,
        "AI text may explain or compress supplied deterministic/ECE/Stockfish truth packets only."
      ),
      PhaseNAiRequirementClassification(
        PhaseNAiRequirement.AiCannotBypassLevelGates,
        RequirementClass.EvenChessSpecific,
        "AI output cannot introduce higher-level chess facts, best-move wording, or forbidden visual specificity."
      ),
      PhaseNAiRequirementClassification(
        PhaseNAiRequirement.BoardStateAtMostOneAiCall,
        RequirementClass.EvenChessSpecific,
        "Each board-state ECE request may allow at most one AI text call."
      ),
      PhaseNAiRequirementClassification(
        PhaseNAiRequirement.FullGameAtMostOneNarrativeCall,
        RequirementClass.EvenChessSpecific,
        "Full-game ECE review may allow at most one larger AI narrative compression call."
      ),
      PhaseNAiRequirementClassification(
        PhaseNAiRequirement.CredentialsServerSideOnly,
        RequirementClass.EvenChessSpecific,
        "Provider credentials and model choices stay server-side and are never selected or exposed by browser clients."
      ),
      PhaseNAiRequirementClassification(
        PhaseNAiRequirement.InvalidOutputFallsBackDeterministically,
        RequirementClass.EvenChessSpecific,
        "Invalid AI output falls back to deterministic supplied facts or suppression after the allowed retry budget."
      )
    )

  enum AiRequirement:
    case ExplainSuppliedTruthOnly
    case SchemaConstrainedLiveJson
    case ScanUnsafeOutput
    case RegenerateOnceThenFallback
    case AuditCostAndValidation
    case PaidPlansNoStrongerLiveHelp
    case ServerSideProviderAccess
    case PostGameSummariesDoNotMutateFairness
    case SummaryQuotaAndQuality
    case PerformanceSummaryOnlineOnlyWindow

  final case class AiRequirementClassification(
      requirement: AiRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object AiRequirementClassifications:
    val all: List[AiRequirementClassification] = List(
      AiRequirementClassification(
        AiRequirement.ExplainSuppliedTruthOnly,
        RequirementClass.EvenChessSpecific,
        "AI may explain and compress authorized truth packets only."
      ),
      AiRequirementClassification(
        AiRequirement.SchemaConstrainedLiveJson,
        RequirementClass.EvenChessSpecific,
        "Live AI output must conform to a server-validated schema."
      ),
      AiRequirementClassification(
        AiRequirement.ScanUnsafeOutput,
        RequirementClass.EvenChessSpecific,
        "Scan live AI output for illegal notation, direct commands, best-move wording, stale position, and visual overreach."
      ),
      AiRequirementClassification(
        AiRequirement.RegenerateOnceThenFallback,
        RequirementClass.EvenChessSpecific,
        "Invalid AI output may regenerate once, then must fallback or suppress."
      ),
      AiRequirementClassification(
        AiRequirement.AuditCostAndValidation,
        RequirementClass.EvenChessSpecific,
        "Every AI request logs model, prompt/schema versions, tokens, cost, validation, fallback, and delivered exactness."
      ),
      AiRequirementClassification(
        AiRequirement.PaidPlansNoStrongerLiveHelp,
        RequirementClass.EvenChessSpecific,
        "Paid plans cannot receive stronger live help or deeper live engine truth through AI."
      ),
      AiRequirementClassification(
        AiRequirement.ServerSideProviderAccess,
        RequirementClass.EvenChessSpecific,
        "Provider credentials are server-side runtime settings or secrets; clients never expose or decide them."
      ),
      AiRequirementClassification(
        AiRequirement.PostGameSummariesDoNotMutateFairness,
        RequirementClass.EvenChessSpecific,
        "Post-game summaries are review surfaces and must not mutate live rated fairness state."
      ),
      AiRequirementClassification(
        AiRequirement.SummaryQuotaAndQuality,
        RequirementClass.AdaptedToLichessFork,
        "Summary quotas integrate with account/review flows while preserving the same quality pipeline for free and paid users."
      ),
      AiRequirementClassification(
        AiRequirement.PerformanceSummaryOnlineOnlyWindow,
        RequirementClass.AdaptedToLichessFork,
        "Performance summaries select recent completed online games from Lichess history/review data."
      )
    )

  final case class SourceFact(
      factId: String,
      text: String,
      boardStateKey: String,
      exactnessClass: ExactnessClass,
      auditTag: String
  ):
    def valid: Boolean =
      factId.nonEmpty && text.nonEmpty && boardStateKey.nonEmpty && auditTag.nonEmpty

  final case class AiLiveRequest(
      requestId: String,
      gameId: String,
      playerId: String,
      boardStateKey: String,
      ply: Int,
      setLevel: Level,
      requestedLevel: Level,
      policyVersion: String,
      promptVersion: String,
      schemaVersion: String,
      authorizedFacts: List[SourceFact]
  ):
    def hasRequiredFields: Boolean =
      requestId.nonEmpty &&
        gameId.nonEmpty &&
        playerId.nonEmpty &&
        boardStateKey.nonEmpty &&
        ply >= 0 &&
        requestedLevel.value <= setLevel.value &&
        policyVersion.nonEmpty &&
        promptVersion.nonEmpty &&
        schemaVersion.nonEmpty &&
        authorizedFacts.nonEmpty &&
        authorizedFacts.forall(_.valid) &&
        authorizedFacts.forall(_.boardStateKey == boardStateKey)

  final case class AiLiveOutput(
      policyVersion: String,
      schemaVersion: String,
      exactnessClass: ExactnessClass,
      message: String,
      visualCues: List[String],
      sourceFactIds: List[String],
      auditTags: List[String],
      boardStateKey: String
  ):
    def hasRequiredSchemaFields: Boolean =
      policyVersion.nonEmpty &&
        schemaVersion.nonEmpty &&
        message.nonEmpty &&
        sourceFactIds.nonEmpty &&
        auditTags.nonEmpty &&
        boardStateKey.nonEmpty

  enum AiOutputViolation:
    case MissingSchemaField
    case InventedSourceFact
    case MissingAuditTag
    case IllegalNotation
    case OverExactCoordinate
    case DirectCommand
    case BestMoveLabel
    case StalePosition
    case VisualOverreach

  final case class AiValidationResult(violations: List[AiOutputViolation]):
    def valid: Boolean = violations.isEmpty

  object AiLiveOutputScanner:
    private val directCommandFragments = List("play ", "move ", "you must", "make this move", "do this move")
    private val bestMoveFragments = List("best move", "the best is", "engine says")
    private val illegalNotationFragments = List("pv ", "mate in", "#", "!!", "??")
    private val coordinatePattern = "(?s).*[a-h][1-8][a-h][1-8].*"

    def validate(
        request: AiLiveRequest,
        output: AiLiveOutput,
        allowedVisualCues: Set[String]
    ): AiValidationResult =
      val factIds = request.authorizedFacts.map(_.factId).toSet
      val auditTags = request.authorizedFacts.map(_.auditTag).toSet
      val normalized = output.message.toLowerCase
      val violations = List(
        Option.when(!output.hasRequiredSchemaFields)(AiOutputViolation.MissingSchemaField),
        Option.when(!output.sourceFactIds.forall(factIds.contains))(AiOutputViolation.InventedSourceFact),
        Option.when(!output.auditTags.forall(auditTags.contains))(AiOutputViolation.MissingAuditTag),
        Option.when(illegalNotationFragments.exists(normalized.contains))(AiOutputViolation.IllegalNotation),
        Option.when(output.message.matches(coordinatePattern))(AiOutputViolation.OverExactCoordinate),
        Option.when(directCommandFragments.exists(normalized.contains))(AiOutputViolation.DirectCommand),
        Option.when(bestMoveFragments.exists(normalized.contains))(AiOutputViolation.BestMoveLabel),
        Option.when(output.boardStateKey != request.boardStateKey)(AiOutputViolation.StalePosition),
        Option.when(!output.visualCues.forall(allowedVisualCues.contains))(AiOutputViolation.VisualOverreach)
      ).flatten

      AiValidationResult(violations.distinct)

  enum AiDeliveryDecision:
    case Deliver
    case RegenerateOnce
    case FallbackSuppress

  object AiFallbackPolicy:
    val maxRegenerations = 1

    def decide(validation: AiValidationResult, priorRegenerations: Int): AiDeliveryDecision =
      if validation.valid then AiDeliveryDecision.Deliver
      else if priorRegenerations < maxRegenerations then AiDeliveryDecision.RegenerateOnce
      else AiDeliveryDecision.FallbackSuppress

  final case class AiRequestAudit(
      requestId: String,
      model: String,
      promptVersion: String,
      schemaVersion: String,
      inputTokens: Int,
      outputTokens: Int,
      costMicros: Long,
      validation: AiValidationResult,
      fallbackUsed: Boolean,
      deliveredExactness: ExactnessClass,
      createdAt: Long
  ):
    def complete: Boolean =
      requestId.nonEmpty &&
        model.nonEmpty &&
        promptVersion.nonEmpty &&
        schemaVersion.nonEmpty &&
        inputTokens >= 0 &&
        outputTokens >= 0 &&
        costMicros >= 0 &&
        createdAt > 0

  final case class AiProviderAccess(
      providerKey: String,
      configuredAtRuntime: Boolean,
      credentialsServerSideOnly: Boolean,
      clientCanExposeCredentials: Boolean,
      clientCanChooseProvider: Boolean,
      cheapDefaultModelAllowed: Boolean,
      promptsSayExplainSuppliedPacketsOnly: Boolean
  ):
    def valid: Boolean =
      providerKey.nonEmpty &&
        configuredAtRuntime &&
        credentialsServerSideOnly &&
        !clientCanExposeCredentials &&
        !clientCanChooseProvider &&
        promptsSayExplainSuppliedPacketsOnly

  enum PlanTier:
    case Free
    case Premium

  object LivePlanFairness:
    val paidPlansMayReceiveStrongerLiveHelp = false
    val paidPlansMayReceiveDeeperLiveEngineTruth = false

    def liveHelpProfile(tier: PlanTier): String =
      tier match
        case PlanTier.Free | PlanTier.Premium => "same-live-policy"

    def sameLiveStrength(a: PlanTier, b: PlanTier): Boolean =
      liveHelpProfile(a) == liveHelpProfile(b)

  enum SummaryType:
    case Match
    case Performance

  enum SummaryGenerationState:
    case Generated
    case Failed
    case CachedView

  final case class SummaryQuota(
      summaryType: SummaryType,
      freeOnboardingTokens: Int,
      premiumDailyLimit: Int,
      unlockCompletedGames: Int
  )

  object SummaryQuotas:
    val matchSummary: SummaryQuota =
      SummaryQuota(SummaryType.Match, freeOnboardingTokens = 3, premiumDailyLimit = 10, unlockCompletedGames = 0)

    val performanceSummary: SummaryQuota =
      SummaryQuota(SummaryType.Performance, freeOnboardingTokens = 1, premiumDailyLimit = 1, unlockCompletedGames = 10)

    val all: List[SummaryQuota] = List(matchSummary, performanceSummary)

    def quotaFor(summaryType: SummaryType): SummaryQuota =
      all.find(_.summaryType == summaryType).get

    def freeUnlocked(summaryType: SummaryType, completedGames: Int): Boolean =
      completedGames >= quotaFor(summaryType).unlockCompletedGames

  object SummaryConsumption:
    def consumesToken(state: SummaryGenerationState): Boolean =
      state == SummaryGenerationState.Generated

  object SummaryQualityPolicy:
    val freeAndPaidUseSameProductQualityPipeline = true
    val promisesNamedFrontierModel = false
    val promisesReviewQuality = true

    def pipelineKey(tier: PlanTier): String =
      tier match
        case PlanTier.Free | PlanTier.Premium => "full-quality-review-pipeline"

  enum AiCallMode:
    case BoardState
    case ProposedMove
    case FullGameReview

  final case class AiCallBudget(
      mode: AiCallMode,
      enabled: Boolean,
      callsAttempted: Int,
      maxCalls: Int
  ):
    def valid: Boolean =
      callsAttempted >= 0 &&
        maxCalls >= 0 &&
        maxCalls <= 1 &&
        callsAttempted <= maxCalls &&
        (enabled || callsAttempted == 0)

  object AiCallBudget:
    def boardState(enabled: Boolean, callsAttempted: Int): AiCallBudget =
      AiCallBudget(AiCallMode.BoardState, enabled, callsAttempted, maxCalls = if enabled then 1 else 0)

    def proposedMove(enabled: Boolean, callsAttempted: Int): AiCallBudget =
      AiCallBudget(AiCallMode.ProposedMove, enabled, callsAttempted, maxCalls = if enabled then 1 else 0)

    def fullGameReview(enabled: Boolean, callsAttempted: Int): AiCallBudget =
      AiCallBudget(AiCallMode.FullGameReview, enabled, callsAttempted, maxCalls = if enabled then 1 else 0)

  final case class DeterministicFallbackText(
      message: String,
      sourceFactIds: List[String],
      auditTags: List[String],
      boardStateKey: String
  ):
    def validFor(request: AiLiveRequest): Boolean =
      message.nonEmpty &&
        sourceFactIds.nonEmpty &&
        sourceFactIds.forall(request.authorizedFacts.map(_.factId).toSet.contains) &&
        auditTags.nonEmpty &&
        auditTags.forall(request.authorizedFacts.map(_.auditTag).toSet.contains) &&
        boardStateKey == request.boardStateKey

  object AiDeterministicFallback:
    def fromFacts(request: AiLiveRequest): DeterministicFallbackText =
      DeterministicFallbackText(
        message = request.authorizedFacts.headOption.map(_.text).getOrElse("Coaching unavailable."),
        sourceFactIds = request.authorizedFacts.map(_.factId),
        auditTags = request.authorizedFacts.map(_.auditTag),
        boardStateKey = request.boardStateKey
      )

  final case class AiProviderSafetyEnvelope(
      access: AiProviderAccess,
      budget: AiCallBudget,
      validation: AiValidationResult,
      fallback: DeterministicFallbackText,
      request: AiLiveRequest
  ):
    def validForRatedUse: Boolean =
      access.valid &&
        budget.valid &&
        fallback.validFor(request) &&
        (validation.valid || AiFallbackPolicy.decide(validation, AiFallbackPolicy.maxRegenerations) == AiDeliveryDecision.FallbackSuppress)

  final case class PostGameSummaryPolicy(
      summaryType: SummaryType,
      reviewSurface: Boolean,
      mutatesLiveRatedFairnessState: Boolean,
      mutatesNormalEcr: Boolean
  ):
    def valid: Boolean =
      reviewSurface && !mutatesLiveRatedFairnessState && !mutatesNormalEcr

  object PostGameSummaryPolicy:
    def default(summaryType: SummaryType): PostGameSummaryPolicy =
      PostGameSummaryPolicy(summaryType, reviewSurface = true, mutatesLiveRatedFairnessState = false, mutatesNormalEcr = false)

  final case class SummaryGameWindowItem(
      gameId: String,
      completed: Boolean,
      online: Boolean,
      botGame: Boolean,
      computerGame: Boolean,
      studyGame: Boolean,
      completedAt: Long
  ):
    def eligibleForPerformanceSummary: Boolean =
      gameId.nonEmpty && completed && online && !botGame && !computerGame && !studyGame

  object PerformanceSummaryWindow:
    val launchDefaultMaxCompletedOnlineGames = 50

    def eligibleRecentGames(games: List[SummaryGameWindowItem], maxGames: Int = launchDefaultMaxCompletedOnlineGames): List[SummaryGameWindowItem] =
      games
        .filter(_.eligibleForPerformanceSummary)
        .sortBy(game => -game.completedAt)
        .take(maxGames)
