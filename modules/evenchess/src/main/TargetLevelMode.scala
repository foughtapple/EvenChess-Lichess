package lila.evenchess

import CoachingLadder.Level
import EcrRating.{ EcrPool, RatedMode }
import ProductInvariants.RequirementClass

object TargetLevelMode:

  enum TargetRequirement:
    case PlayerSelectedPracticeContext
    case NoNormalEcrMutation
    case SeparateQueue
    case AdjacentWideningConfirmation
    case VisibleModeLabels
    case NoNormalEcrLeaderboardEligibility
    case TokenPolicyUnresolved

  final case class TargetRequirementClassification(
      requirement: TargetRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object TargetRequirementClassifications:
    val all: List[TargetRequirementClassification] = List(
      TargetRequirementClassification(
        TargetRequirement.PlayerSelectedPracticeContext,
        RequirementClass.EvenChessSpecific,
        "Preserve Target Level as practice metadata, not Rating Level."
      ),
      TargetRequirementClassification(
        TargetRequirement.NoNormalEcrMutation,
        RequirementClass.EvenChessSpecific,
        "Target MVP games must set normal_ecr_changed=false."
      ),
      TargetRequirementClassification(
        TargetRequirement.SeparateQueue,
        RequirementClass.AdaptedToLichessFork,
        "Use Lichess queue/search surfaces only through a Target-specific queue boundary."
      ),
      TargetRequirementClassification(
        TargetRequirement.AdjacentWideningConfirmation,
        RequirementClass.AdaptedToLichessFork,
        "Any adjacent Target Level widening needs explicit UI confirmation."
      ),
      TargetRequirementClassification(
        TargetRequirement.VisibleModeLabels,
        RequirementClass.AdaptedToLichessFork,
        "Lobby, header, result, and review surfaces must disclose Target Mode."
      ),
      TargetRequirementClassification(
        TargetRequirement.NoNormalEcrLeaderboardEligibility,
        RequirementClass.EvenChessSpecific,
        "Target games do not qualify players for normal ECR leaderboards."
      ),
      TargetRequirementClassification(
        TargetRequirement.TokenPolicyUnresolved,
        RequirementClass.UnresolvedProductOwnerDecision,
        "Tokens may consume resources later, but must never affect rated fairness."
      )
    )

  enum TargetQueue:
    case NormalEvenChess
    case TargetLevel

  enum TargetLabelSurface:
    case Lobby
    case Header
    case Result
    case Review

  final case class TargetLevelContract(
      targetLevel: Level,
      setLevelPolicyVersion: String,
      selectedByPlayerId: String
  ):
    def isPlayerSelectedPracticeContext: Boolean =
      selectedByPlayerId.nonEmpty && setLevelPolicyVersion.nonEmpty

    def isRatingLevel: Boolean = false

    def disclosureLabel: String = s"Target Mode L${targetLevel.value}"

  final case class TargetGamePolicy(
      contract: TargetLevelContract,
      queue: TargetQueue,
      hiddenTargetShadowRatingEnabled: Boolean,
      serverAuthorizedCoaching: Boolean,
      auditedCoaching: Boolean
  ):
    def ratedMode: RatedMode = RatedMode.TargetLevelMvp

    def normalEcrChanged: Boolean = false

    def normalEcrLeaderboardEligible: Boolean = false

    def maySearchNormalEvenChessQueue: Boolean = false

    def usesSeparateTargetQueue: Boolean = queue == TargetQueue.TargetLevel

    def targetShadowPool: Option[EcrPool] =
      Option.when(hiddenTargetShadowRatingEnabled)(EcrPool.TargetShadow)

    def validForMvp: Boolean =
      contract.isPlayerSelectedPracticeContext &&
        usesSeparateTargetQueue &&
        serverAuthorizedCoaching &&
        auditedCoaching &&
        !normalEcrChanged &&
        !normalEcrLeaderboardEligible &&
        !maySearchNormalEvenChessQueue

  object TargetGamePolicy:
    def mvp(
        contract: TargetLevelContract,
        hiddenTargetShadowRatingEnabled: Boolean = false
    ): TargetGamePolicy =
      TargetGamePolicy(
        contract = contract,
        queue = TargetQueue.TargetLevel,
        hiddenTargetShadowRatingEnabled = hiddenTargetShadowRatingEnabled,
        serverAuthorizedCoaching = true,
        auditedCoaching = true
      )

  object TargetQueuePolicy:
    def canPair(searchQueue: TargetQueue, gameQueue: TargetQueue): Boolean =
      searchQueue == TargetQueue.TargetLevel && gameQueue == TargetQueue.TargetLevel

  final case class TargetLevelWidening(
      requestedLevel: Level,
      candidateLevel: Level,
      uiConfirmed: Boolean
  ):
    def isSameLevel: Boolean = requestedLevel == candidateLevel

    def isAdjacent: Boolean = math.abs(requestedLevel.value - candidateLevel.value) == 1

    def allowed: Boolean =
      isSameLevel || (isAdjacent && uiConfirmed)

    def requiresConfirmation: Boolean =
      isAdjacent && !isSameLevel

  object TargetDisclosure:
    val requiredSurfaces: Set[TargetLabelSurface] =
      Set(
        TargetLabelSurface.Lobby,
        TargetLabelSurface.Header,
        TargetLabelSurface.Result,
        TargetLabelSurface.Review
      )

    def hasRequiredSurfaces(surfaces: Set[TargetLabelSurface]): Boolean =
      requiredSurfaces.subsetOf(surfaces)

  final case class TargetSummaryPolicy(
      reviewable: Boolean,
      includedInNormalPerformanceWindow: Boolean,
      mutatesNormalEcr: Boolean
  ):
    def validForMvp: Boolean =
      reviewable && !includedInNormalPerformanceWindow && !mutatesNormalEcr

  object TargetSummaryPolicy:
    val mvp: TargetSummaryPolicy =
      TargetSummaryPolicy(
        reviewable = true,
        includedInNormalPerformanceWindow = false,
        mutatesNormalEcr = false
      )

  object TargetResourcePolicy:
    val tokenConsumptionNeedsProductOwnerDecision = true
    val tokensMayAffectFairness = false
    val subscriptionsMayAffectFairness = false
