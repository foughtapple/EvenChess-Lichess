package lila.evenchess

import ProductInvariants.RequirementClass

object ForkArchitecture:

  final case class SourceAssumption(
      key: String,
      url: String,
      role: String,
      classification: RequirementClass
  )

  object SourceAssumptions:
    val upstreamRemoteName = "upstream"
    val upstreamLilaUrl = "https://github.com/lichess-org/lila.git"
    val originForkUrl = "https://github.com/foughtapple/EvenChess-Lichess.git"
    val localDockerRoot = "~/dev/lila-docker"
    val localRepoRoot = "~/dev/lila-docker/repos/lila"

    val all: List[SourceAssumption] = List(
      SourceAssumption("lila", "https://github.com/lichess-org/lila", "Main backend/frontend foundation.", RequirementClass.LichessProvided),
      SourceAssumption("lila-docker", "https://github.com/lichess-org/lila-docker", "Local development foundation.", RequirementClass.LichessProvided),
      SourceAssumption("lichess-source", "https://lichess.org/source", "Public source index.", RequirementClass.LichessProvided),
      SourceAssumption("scalachess", "https://github.com/lichess-org/scalachess", "Rules/legal move foundation.", RequirementClass.LichessProvided),
      SourceAssumption("chessground", "https://github.com/lichess-org/chessground", "Board UI foundation.", RequirementClass.LichessProvided)
    )

  enum PatchMapRule:
    case NoPatchRisk
    case RecordLilaHooks
    case RecordModelOrSerializationEdits
    case RecordUiOrBoardEdits
    case RecordEngineOrAnalysisHooks
    case RecordPairingOrRatingEdits
    case RecordAccountOrBillingEdits

  enum ForkLayer:
    case RequirementsDocs
    case BackendPolicy
    case GameMetadata
    case Ui
    case EngineAi
    case RatingMatchmaking
    case MarketingMonetisation

  final case class ForkStrategy(
      layer: ForkLayer,
      desiredApproach: String,
      patchMapRule: PatchMapRule,
      classification: RequirementClass
  )

  object ForkStrategies:
    val all: List[ForkStrategy] = List(
      ForkStrategy(ForkLayer.RequirementsDocs, "Own files under docs/requirements and docs/evenchess.", PatchMapRule.NoPatchRisk, RequirementClass.EvenChessSpecific),
      ForkStrategy(ForkLayer.BackendPolicy, "Prefer a new EvenChess package, module, or service.", PatchMapRule.RecordLilaHooks, RequirementClass.EvenChessSpecific),
      ForkStrategy(ForkLayer.GameMetadata, "Add minimal EvenChess mode and policy metadata.", PatchMapRule.RecordModelOrSerializationEdits, RequirementClass.AdaptedToLichessFork),
      ForkStrategy(ForkLayer.Ui, "Prefer EvenChess-specific components, classes, and server data payloads.", PatchMapRule.RecordUiOrBoardEdits, RequirementClass.AdaptedToLichessFork),
      ForkStrategy(ForkLayer.EngineAi, "Use an internal server-side service boundary only.", PatchMapRule.RecordEngineOrAnalysisHooks, RequirementClass.AdaptedToLichessFork),
      ForkStrategy(ForkLayer.RatingMatchmaking, "Keep separate ECR pools and matchmaking policy.", PatchMapRule.RecordPairingOrRatingEdits, RequirementClass.AdaptedToLichessFork),
      ForkStrategy(ForkLayer.MarketingMonetisation, "Use config-driven fork-specific surfaces.", PatchMapRule.RecordAccountOrBillingEdits, RequirementClass.EvenChessSpecific)
    )

  object NamespaceRules:
    val canonicalNamespace = "evenchess"
    val codeName = "EvenChess"

    val preferredOwnedPaths: List[String] = List(
      "modules/evenchess/",
      "ui/evenchess/",
      "app/views/evenchess/",
      "public/evenchess/",
      "docs/evenchess/"
    )

    val preferredTerms: List[String] = List(
      "evenchess",
      "EvenChess",
      "ECR",
      "SetLevel",
      "UsedLevel",
      "AssistanceLoad",
      "UsedOffset",
      "TargetLevel",
      "OffsetCount",
      "CoachingOverlay",
      "AssistanceLedger"
    )

    def isEvenChessOwnedPath(path: String): Boolean =
      val normalized = path.replace('\\', '/')
      preferredOwnedPaths.exists(normalized.startsWith)

    def hasEvenChessName(name: String): Boolean =
      val normalized = name.toLowerCase
      normalized.contains("evenchess") || normalized.contains("ecr")

  enum MergeRisk:
    case Low
    case Medium
    case High
    case Unknown

  enum IntegrationOutcome:
    case UseLichessProvidedCapability
    case ImplementInsideEvenChessNamespace
    case AddSmallPatchMappedHook
    case RequireServerSideBoundaryAndPatchMap
    case RequireSeparatedFairnessServiceAndPatchMap
    case StopBecauseNormalChessWouldChange
    case InspectBeforeChoosingSeam

  final case class IntegrationQuestion(
      lichessProvided: Boolean = false,
      evenChessSpecific: Boolean = false,
      needsLilaLifecycleHook: Boolean = false,
      altersNormalChess: Boolean = false,
      exposesEngineOrAiTruth: Boolean = false,
      affectsRatingTokensBillingOrMatchmaking: Boolean = false
  )

  object IntegrationDecisionTree:
    def decide(question: IntegrationQuestion): IntegrationOutcome =
      if question.altersNormalChess then IntegrationOutcome.StopBecauseNormalChessWouldChange
      else if question.lichessProvided && !question.evenChessSpecific then IntegrationOutcome.UseLichessProvidedCapability
      else if question.exposesEngineOrAiTruth then IntegrationOutcome.RequireServerSideBoundaryAndPatchMap
      else if question.affectsRatingTokensBillingOrMatchmaking then IntegrationOutcome.RequireSeparatedFairnessServiceAndPatchMap
      else if question.needsLilaLifecycleHook then IntegrationOutcome.AddSmallPatchMappedHook
      else if question.evenChessSpecific then IntegrationOutcome.ImplementInsideEvenChessNamespace
      else IntegrationOutcome.InspectBeforeChoosingSeam

  object CoreEditRules:
    val broadMixedLogicAllowed = false
    val normalChessMayChangeWithoutApproval = false
    val coreEditsMustBeSmallHooksDelegatorsOrFeatureFlags = true
    val touchedUpstreamFilesRequirePatchMap = true

  object UpstreamSyncRules:
    val keepUpstreamRemote = true
    val upstreamRemoteName = SourceAssumptions.upstreamRemoteName
    val useFeatureOrControlledIntegrationBranch = true
    val reviewPatchMapBeforeSync = true
    val runBaselineLilaChecksAfterSync = true
    val runEvenChessRegressionsAfterSync = true
    val silentlyDeleteEvenChessHooksDuringConflictResolution = false
    val allowedPatchMapRiskValues: Set[MergeRisk] = Set(MergeRisk.Low, MergeRisk.Medium, MergeRisk.High, MergeRisk.Unknown)

  object BrandingRules:
    val retainOpenSourceNotices = true
    val mayImplyOfficialLichessAffiliation = false
    val publicNameMustDistinguishForkProduct = true
    val distributionHostingSourcePublicationRequiresReview = true

  object Stage1ArchitectureOutput:
    val requiredSections: List[String] = List(
      "local boot status",
      "inspected commit/version",
      "account/game status",
      "proposed backend/UI seams",
      "game metadata seam",
      "overlay delivery seam",
      "rating/matchmaking seam candidates",
      "patch map entries",
      "go/no-go decision"
    )
