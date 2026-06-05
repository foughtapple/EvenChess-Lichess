package lila.evenchess

import AiCoachPolicy.SourceFact
import ProductInvariants.RequirementClass

object LearningAiOverlay:

  enum PhaseGLearningRequirement:
    case LichessStudyAnalysisOpeningProvided
    case ServerAuthorizedLearningContext
    case OptionalAnalyseDataField
    case OptionalOpeningDataField
    case ReuseLichessTruthSources
    case LiveRatedRestrictionsSeparated
    case ClientDisplayOnlyNoProviderSecrets
    case NoRawEngineOrDebugPayloads

  final case class PhaseGLearningRequirementClassification(
      requirement: PhaseGLearningRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseGLearningRequirementClassifications:
    val all: List[PhaseGLearningRequirementClassification] = List(
      PhaseGLearningRequirementClassification(
        PhaseGLearningRequirement.LichessStudyAnalysisOpeningProvided,
        RequirementClass.LichessProvided,
        "Use existing Lichess study, analysis, and opening explorer surfaces instead of rebuilding them."
      ),
      PhaseGLearningRequirementClassification(
        PhaseGLearningRequirement.ServerAuthorizedLearningContext,
        RequirementClass.EvenChessSpecific,
        "EvenChess AI learning cards render only from server-authorized context and audited source facts."
      ),
      PhaseGLearningRequirementClassification(
        PhaseGLearningRequirement.OptionalAnalyseDataField,
        RequirementClass.AdaptedToLichessFork,
        "Expose an optional namespaced `evenchess.learning` field on analysis/study data."
      ),
      PhaseGLearningRequirementClassification(
        PhaseGLearningRequirement.OptionalOpeningDataField,
        RequirementClass.AdaptedToLichessFork,
        "Expose an optional namespaced `evenchess.openingAi` field on opening explorer data."
      ),
      PhaseGLearningRequirementClassification(
        PhaseGLearningRequirement.ReuseLichessTruthSources,
        RequirementClass.AdaptedToLichessFork,
        "Ground AI copy in existing Lichess analysis, study, and opening context plus EvenChess truth packets."
      ),
      PhaseGLearningRequirementClassification(
        PhaseGLearningRequirement.LiveRatedRestrictionsSeparated,
        RequirementClass.EvenChessSpecific,
        "Learning overlays are review/learning surfaces and must not mutate live rated fairness state."
      ),
      PhaseGLearningRequirementClassification(
        PhaseGLearningRequirement.ClientDisplayOnlyNoProviderSecrets,
        RequirementClass.EvenChessSpecific,
        "Browser adapters display approved cards only and never choose providers or receive secrets."
      ),
      PhaseGLearningRequirementClassification(
        PhaseGLearningRequirement.NoRawEngineOrDebugPayloads,
        RequirementClass.EvenChessSpecific,
        "Payloads with raw engine lines, raw prompts, provider secrets, or hidden debug data are not client safe."
      )
    )

  enum LearningSurface:
    case Analysis
    case Study
    case OpeningExplorer

    def clientKey: String = this match
      case Analysis        => "analysis"
      case Study           => "study"
      case OpeningExplorer => "opening"

    def displayName: String = this match
      case Analysis        => "Analysis"
      case Study           => "Study"
      case OpeningExplorer => "Opening explorer"

  enum LearningCardKind:
    case ChapterSummary
    case PositionExplanation
    case OpeningPlan
    case MistakeTheme
    case ExplainMove

  object AdapterContract:
    val analyseDataKey = "evenchess"
    val learningPayloadKey = "learning"
    val openingPayloadKey = "openingAi"
    val analyseCssClass = "evenchess-ai"
    val openingCssClass = "opening__evenchess-ai"
    val clientMayAuthorize = false
    val clientMayReadProviderSecrets = false
    val clientMayReadRawEngineData = false
    val mutatesLiveRatedFairnessState = false
    val maxCards = 3
    val maxBullets = 5

  final case class LearningContext(
      surface: LearningSurface,
      contextId: String,
      boardStateKey: String,
      ply: Int,
      source: String,
      ratedLive: Boolean,
      serverAuthorized: Boolean,
      policyVersion: String,
      schemaVersion: String,
      auditId: String
  ):
    def outsideRatedLive: Boolean = !ratedLive

    def valid: Boolean =
      contextId.nonEmpty &&
        boardStateKey.nonEmpty &&
        ply >= 0 &&
        source.nonEmpty &&
        outsideRatedLive &&
        serverAuthorized &&
        policyVersion.nonEmpty &&
        schemaVersion.nonEmpty &&
        auditId.nonEmpty

  final case class LearningCard(
      id: String,
      kind: LearningCardKind,
      title: String,
      body: String,
      bullets: List[String],
      sourceFactIds: List[String],
      auditId: String,
      serverAuthorized: Boolean,
      approvedDisplayPayload: Boolean,
      rawEnginePayload: Option[String],
      hiddenDebugData: Option[String],
      providerSecret: Option[String],
      rawPrompt: Option[String],
      modelLabel: Option[String]
  ):
    def unsafePayload: Boolean =
      rawEnginePayload.exists(_.nonEmpty) ||
        hiddenDebugData.exists(_.nonEmpty) ||
        providerSecret.exists(_.nonEmpty) ||
        rawPrompt.exists(_.nonEmpty)

    def safeForClient(context: LearningContext, sourceFacts: List[SourceFact]): Boolean =
      val factIds = sourceFacts.map(_.factId).toSet
      id.nonEmpty &&
        title.nonEmpty &&
        body.nonEmpty &&
        bullets.forall(_.nonEmpty) &&
        bullets.size <= AdapterContract.maxBullets &&
        sourceFactIds.nonEmpty &&
        sourceFactIds.forall(factIds.contains) &&
        auditId == context.auditId &&
        serverAuthorized &&
        approvedDisplayPayload &&
        !unsafePayload

  final case class LearningPayload(
      enabled: Boolean,
      context: LearningContext,
      sourceFacts: List[SourceFact],
      cards: List[LearningCard]
  ):
    private def sourceFactsValid: Boolean =
      sourceFacts.nonEmpty &&
        sourceFacts.forall(_.valid) &&
        sourceFacts.forall(_.boardStateKey == context.boardStateKey)

    def renderableCards: List[LearningCard] =
      if !safeForClient then Nil
      else cards.filter(_.safeForClient(context, sourceFacts)).take(AdapterContract.maxCards)

    def safeForClient: Boolean =
      enabled &&
        context.valid &&
        sourceFactsValid &&
        cards.nonEmpty &&
        cards.forall(_.safeForClient(context, sourceFacts))

    def valid: Boolean =
      (!enabled && cards.isEmpty) ||
        (safeForClient && renderableCards.nonEmpty && renderableCards.size <= AdapterContract.maxCards)

    def mutatesLiveRatedFairnessState: Boolean =
      AdapterContract.mutatesLiveRatedFairnessState
