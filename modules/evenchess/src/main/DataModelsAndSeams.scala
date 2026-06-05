package lila.evenchess

import ProductInvariants.RequirementClass

object DataModelsAndSeams:

  enum DataRequirement:
    case FairnessModelsVersioned
    case RatingOffsetPolicyVersions
    case MarketingConfigVariantVersioned
    case AiEngineVersionedAudit
    case ReplayableMigrations
    case InspectLilaStorageConventions
    case DedicatedEvenChessStores
    case SensitiveRawDataAvoided
    case Stage1LimitedScope
    case LilaIntegrationSeams

  final case class DataRequirementClassification(
      requirement: DataRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object DataRequirementClassifications:
    val all: List[DataRequirementClassification] = List(
      DataRequirementClassification(
        DataRequirement.FairnessModelsVersioned,
        RequirementClass.EvenChessSpecific,
        "Fairness-affecting ledger, event, and config models include schema and model or policy versions."
      ),
      DataRequirementClassification(
        DataRequirement.RatingOffsetPolicyVersions,
        RequirementClass.EvenChessSpecific,
        "Rating and offset calculations record policy and model versions for replay."
      ),
      DataRequirementClassification(
        DataRequirement.MarketingConfigVariantVersioned,
        RequirementClass.EvenChessSpecific,
        "Marketing attribution records config version and landing/campaign variant without affecting fairness."
      ),
      DataRequirementClassification(
        DataRequirement.AiEngineVersionedAudit,
        RequirementClass.EvenChessSpecific,
        "AI and engine records store prompt/schema/profile/model versions and bounded audit metadata."
      ),
      DataRequirementClassification(
        DataRequirement.ReplayableMigrations,
        RequirementClass.EvenChessSpecific,
        "Migrations must preserve replayability of rated assistance, rating, and ledger records."
      ),
      DataRequirementClassification(
        DataRequirement.InspectLilaStorageConventions,
        RequirementClass.AdaptedToLichessFork,
        "Inspect lila storage conventions before choosing database collections, indexes, codecs, or migrations."
      ),
      DataRequirementClassification(
        DataRequirement.DedicatedEvenChessStores,
        RequirementClass.EvenChessSpecific,
        "Prefer dedicated EvenChess stores over broad core Lichess fields."
      ),
      DataRequirementClassification(
        DataRequirement.SensitiveRawDataAvoided,
        RequirementClass.EvenChessSpecific,
        "Avoid raw AI prompts, raw engine lines, raw emails, and sensitive attribution unless required and privacy-reviewed."
      ),
      DataRequirementClassification(
        DataRequirement.Stage1LimitedScope,
        RequirementClass.EvenChessSpecific,
        "Stage 1 data scope is limited to the EvenChess mode marker, overlay event scaffold, patch-map record, and server-side AI request/response scaffold when implemented."
      ),
      DataRequirementClassification(
        DataRequirement.LilaIntegrationSeams,
        RequirementClass.AdaptedToLichessFork,
        "Use narrow lila seams for game creation, seek/challenge, move, clock, websocket, engine, AI, rating, review, account, marketing, and admin integration."
      )
    )

  final case class VersionStamp(
      schemaVersion: String,
      policyVersion: Option[String],
      modelVersion: Option[String],
      configVersion: Option[String]
  ):
    def hasSchema: Boolean = schemaVersion.nonEmpty

    def hasFairnessVersion: Boolean =
      policyVersion.exists(_.nonEmpty) || modelVersion.exists(_.nonEmpty)

    def hasConfigVersion: Boolean =
      configVersion.exists(_.nonEmpty)

  final case class EvenChessGamePolicy(
      gameId: String,
      mode: String,
      rated: Boolean,
      timeControl: String,
      playerIds: Set[String],
      setLevelsByPlayer: Map[String, Int],
      versions: VersionStamp,
      featureFlags: Map[String, String]
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        mode.nonEmpty &&
        timeControl.nonEmpty &&
        playerIds.size == 2 &&
        setLevelsByPlayer.keySet == playerIds &&
        setLevelsByPlayer.values.forall(level => level >= 0 && level <= 10) &&
        versions.hasSchema &&
        (!rated || versions.hasFairnessVersion)

  final case class EvenChessPlayerRating(
      playerId: String,
      poolKey: String,
      rawEcr: Double,
      ratingDeviation: Double,
      volatility: Double,
      gameCount: Int,
      provisional: Boolean,
      createdAt: Long,
      updatedAt: Long,
      modelVersion: String,
      schemaVersion: String
  ):
    def valid: Boolean =
      playerId.nonEmpty &&
        poolKey.nonEmpty &&
        rawEcr > 0 &&
        ratingDeviation >= 0 &&
        volatility >= 0 &&
        gameCount >= 0 &&
        createdAt > 0 &&
        updatedAt >= createdAt &&
        modelVersion.nonEmpty &&
        schemaVersion.nonEmpty

  final case class EvenChessGameAssistanceSummary(
      gameId: String,
      playerId: String,
      usedLevel: Int,
      assistanceLoad: Double,
      usedOffset: Int,
      featureMix: Set[String],
      modelVersion: String,
      policyVersion: String,
      schemaVersion: String
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        playerId.nonEmpty &&
        usedLevel >= 0 &&
        usedLevel <= 10 &&
        assistanceLoad >= 0 &&
        featureMix.nonEmpty &&
        modelVersion.nonEmpty &&
        policyVersion.nonEmpty &&
        schemaVersion.nonEmpty

  final case class CoachingFeatureModel(
      featureKey: String,
      minLevel: Int,
      maxLevel: Int,
      gatingPolicyVersion: String,
      registryVersion: String,
      schemaVersion: String
  ):
    def valid: Boolean =
      featureKey.nonEmpty &&
        minLevel >= 0 &&
        maxLevel <= 10 &&
        minLevel <= maxLevel &&
        gatingPolicyVersion.nonEmpty &&
        registryVersion.nonEmpty &&
        schemaVersion.nonEmpty

  enum CoachingRenderAction:
    case Rendered
    case Suppressed
    case Expanded
    case Blocked

  final case class CoachingRenderEventModel(
      eventId: String,
      gameId: String,
      playerId: String,
      featureKey: String,
      action: CoachingRenderAction,
      visibility: String,
      ply: Int,
      policyVersion: String,
      schemaVersion: String,
      serverAuthoritative: Boolean
  ):
    def valid: Boolean =
      eventId.nonEmpty &&
        gameId.nonEmpty &&
        playerId.nonEmpty &&
        featureKey.nonEmpty &&
        visibility.nonEmpty &&
        ply >= 0 &&
        policyVersion.nonEmpty &&
        schemaVersion.nonEmpty &&
        serverAuthoritative

  final case class OffsetCountPayloadModel(
      gameId: String,
      playerId: String,
      boardHash: String,
      resultState: String,
      displayColor: String,
      materialDelta: Int,
      resolverVersion: String,
      schemaVersion: String
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        playerId.nonEmpty &&
        boardHash.nonEmpty &&
        Set("equal", "student_wins", "opponent_wins").contains(resultState) &&
        Set("blue", "green", "red").contains(displayColor) &&
        resolverVersion.nonEmpty &&
        schemaVersion.nonEmpty

  final case class EngineAnalysisJobModel(
      jobId: String,
      gameId: String,
      playerId: String,
      profileVersion: String,
      cacheKey: String,
      fallbackUsed: Boolean,
      engineVersion: String,
      engineBinaryHash: String,
      schemaVersion: String,
      storesRawEngineLines: Boolean
  ):
    def valid: Boolean =
      jobId.nonEmpty &&
        gameId.nonEmpty &&
        playerId.nonEmpty &&
        profileVersion.nonEmpty &&
        cacheKey.nonEmpty &&
        engineVersion.nonEmpty &&
        engineBinaryHash.nonEmpty &&
        schemaVersion.nonEmpty &&
        !storesRawEngineLines

  final case class AIWordingRequestModel(
      requestId: String,
      gameId: String,
      playerId: String,
      promptTemplateVersion: String,
      responseSchemaVersion: String,
      validationVersion: String,
      modelVersion: String,
      estimatedCostCents: Int,
      schemaVersion: String,
      storesRawPrompt: Boolean
  ):
    def valid: Boolean =
      requestId.nonEmpty &&
        gameId.nonEmpty &&
        playerId.nonEmpty &&
        promptTemplateVersion.nonEmpty &&
        responseSchemaVersion.nonEmpty &&
        validationVersion.nonEmpty &&
        modelVersion.nonEmpty &&
        estimatedCostCents >= 0 &&
        schemaVersion.nonEmpty &&
        !storesRawPrompt

  enum TokenLedgerEvent:
    case OnboardingGranted
    case AdEarned
    case Consumed
    case Refunded

  final case class TokenLedgerEntryModel(
      entryId: String,
      playerId: String,
      event: TokenLedgerEvent,
      amount: Int,
      reason: String,
      gameId: Option[String],
      schemaVersion: String
  ):
    def valid: Boolean =
      entryId.nonEmpty &&
        playerId.nonEmpty &&
        amount > 0 &&
        reason.nonEmpty &&
        schemaVersion.nonEmpty &&
        (event != TokenLedgerEvent.Consumed || gameId.exists(_.nonEmpty))

  final case class SummaryQuotaLedgerEntryModel(
      entryId: String,
      playerId: String,
      summaryType: String,
      consumesToken: Boolean,
      consumesQuota: Boolean,
      gameId: String,
      providerVersion: String,
      schemaVersion: String
  ):
    def valid: Boolean =
      entryId.nonEmpty &&
        playerId.nonEmpty &&
        summaryType.nonEmpty &&
        gameId.nonEmpty &&
        providerVersion.nonEmpty &&
        schemaVersion.nonEmpty &&
        (consumesToken || consumesQuota)

  final case class MarketingAttributionModel(
      attributionId: String,
      pseudonymousAccountId: String,
      utmSource: Option[String],
      utmCampaign: Option[String],
      clickId: Option[String],
      variant: String,
      configVersion: String,
      firstTouch: Option[String],
      latestTouch: Option[String],
      subscriptionId: Option[String],
      storesRawEmail: Boolean,
      schemaVersion: String
  ):
    def valid: Boolean =
      attributionId.nonEmpty &&
        pseudonymousAccountId.nonEmpty &&
        variant.nonEmpty &&
        configVersion.nonEmpty &&
        schemaVersion.nonEmpty &&
        !storesRawEmail &&
        List(utmSource, utmCampaign, clickId, firstTouch, latestTouch, subscriptionId).exists(_.exists(_.nonEmpty))

  final case class PatchMapEntryModel(
      entryId: String,
      fileTouched: String,
      requirement: String,
      risk: String,
      tests: String,
      isolationPlan: String
  ):
    def valid: Boolean =
      entryId.nonEmpty &&
        fileTouched.nonEmpty &&
        requirement.nonEmpty &&
        Set("Low", "Medium", "High", "Unknown").contains(risk) &&
        tests.nonEmpty &&
        isolationPlan.nonEmpty

  enum IntegrationSeam:
    case GameCreation
    case SeekChallengeMatchmaking
    case MoveCommit
    case ClockUpdate
    case BoardWebSocketPayload
    case EngineService
    case AiService
    case RatingUpdate
    case ReviewSurface
    case AccountSubscription
    case MarketingFunnel
    case AdminOps

  final case class IntegrationSeamRule(
      seam: IntegrationSeam,
      requirement: String,
      adaptedToLila: Boolean,
      fairnessAffecting: Boolean,
      serverAuthoritative: Boolean
  ):
    def valid: Boolean =
      requirement.nonEmpty &&
        adaptedToLila &&
        (!fairnessAffecting || serverAuthoritative)

  object IntegrationSeamRegistry:
    val all: List[IntegrationSeamRule] = List(
      IntegrationSeamRule(IntegrationSeam.GameCreation, "Attach server-owned EvenChess mode/policy metadata.", adaptedToLila = true, fairnessAffecting = true, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.SeekChallengeMatchmaking, "Include ECR, expected offset, pool, and level compatibility.", adaptedToLila = true, fairnessAffecting = true, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.MoveCommit, "Clear stale overlays, record position, and evaluate pending coaching.", adaptedToLila = true, fairnessAffecting = true, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.ClockUpdate, "Feed stale-help and clock-pressure accounting.", adaptedToLila = true, fairnessAffecting = true, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.BoardWebSocketPayload, "Deliver only server-authorized overlays.", adaptedToLila = true, fairnessAffecting = true, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.EngineService, "Return bounded truth packets.", adaptedToLila = true, fairnessAffecting = true, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.AiService, "Schema-constrained wording over truth packets.", adaptedToLila = true, fairnessAffecting = false, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.RatingUpdate, "Apply ECR using Used Offset; normal ratings remain separate.", adaptedToLila = true, fairnessAffecting = true, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.ReviewSurface, "Read completed game plus audit/analysis; no live mutation.", adaptedToLila = true, fairnessAffecting = false, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.AccountSubscription, "Store tokens, quotas, and plan state under account extension.", adaptedToLila = true, fairnessAffecting = false, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.MarketingFunnel, "Serve config and events without affecting fairness.", adaptedToLila = true, fairnessAffecting = false, serverAuthoritative = true),
      IntegrationSeamRule(IntegrationSeam.AdminOps, "Query health, ledgers, and feature flags.", adaptedToLila = true, fairnessAffecting = false, serverAuthoritative = true)
    )

    def coversAllSeams: Boolean =
      IntegrationSeam.values.toSet.subsetOf(all.map(_.seam).toSet) && all.forall(_.valid)

  object StoragePolicy:
    val inspectLilaStorageConventionsBeforeDbImplementation = true
    val preferDedicatedEvenChessStores = true
    val broadCoreFieldsPreferred = false
    val migrationsMustPreserveReplayability = true
    val rawAiPromptsAllowedByDefault = false
    val rawEngineLinesAllowedByDefault = false
    val rawEmailsAllowedByDefault = false
    val sensitiveAttributionAllowedWithoutPrivacyReview = false

  object Stage1DataScope:
    val requiredModels: Set[String] = Set(
      "EvenChessGamePolicy",
      "CoachingRenderEvent",
      "PatchMapEntry",
      "AIWordingRequest"
    )

    def withinStage1Scope(modelName: String): Boolean =
      requiredModels.contains(modelName)
