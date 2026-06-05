package lila.evenchess

import AssistanceAccounting.{ EffectiveRating, ExpectedScore, UsedOffset }
import CoachingLadder.Level
import EvenChessMode.TimeControlBucket

object EcrRating:

  enum EcrPool:
    case Rapid
    case Blitz
    case Bullet
    case Classical
    case Correspondence
    case TargetShadow
    case NormalLichess

  enum PoolLaunchStatus:
    case PrimaryLikelyLaunch
    case OptionalLaunchBeta
    case LaterUnlessApproved
    case Optional
    case LaterUncertain
    case HiddenOnlyIfEnabled
    case SeparateDoNotPollute

  final case class PoolPolicy(
      pool: EcrPool,
      status: PoolLaunchStatus,
      notes: String,
      updatesNormalEcr: Boolean,
      updatesNormalLichessRating: Boolean
  )

  object PoolPolicies:
    val all: List[PoolPolicy] = List(
      PoolPolicy(EcrPool.Rapid, PoolLaunchStatus.PrimaryLikelyLaunch, "Start here unless changed.", true, false),
      PoolPolicy(EcrPool.Blitz, PoolLaunchStatus.OptionalLaunchBeta, "Needs compact UI calibration.", true, false),
      PoolPolicy(EcrPool.Bullet, PoolLaunchStatus.LaterUnlessApproved, "High assistance timing risk.", true, false),
      PoolPolicy(EcrPool.Classical, PoolLaunchStatus.Optional, "Lower time-saving but exact help remains strong.", true, false),
      PoolPolicy(EcrPool.Correspondence, PoolLaunchStatus.LaterUncertain, "Different assistance model.", true, false),
      PoolPolicy(EcrPool.TargetShadow, PoolLaunchStatus.HiddenOnlyIfEnabled, "Must not update normal ECR.", false, false),
      PoolPolicy(EcrPool.NormalLichess, PoolLaunchStatus.SeparateDoNotPollute, "Normal Lichess rating stays separate.", false, true)
    )

    val byPool: Map[EcrPool, PoolPolicy] = all.map(policy => policy.pool -> policy).toMap

  final case class EcrRecord(
      playerId: String,
      rating: Int,
      ratingDeviation: Double,
      volatility: Double,
      gameCount: Int,
      provisional: Boolean,
      pool: EcrPool,
      createdAt: Long,
      updatedAt: Long,
      ratingPolicyVersion: String,
      calibrationPolicyVersion: String,
      matchmakingPolicyVersion: String
  ):
    def labelledName: String = "EvenChess Rating (ECR)"

    def isNormalChessElo: Boolean = false

    def hasRequiredFields: Boolean =
      playerId.nonEmpty &&
        rating > 0 &&
        ratingDeviation >= 0 &&
        volatility >= 0 &&
        gameCount >= 0 &&
        createdAt > 0 &&
        updatedAt >= createdAt &&
        ratingPolicyVersion.nonEmpty &&
        calibrationPolicyVersion.nonEmpty &&
        matchmakingPolicyVersion.nonEmpty

  object EcrRecord:
    def provisional(playerId: String, pool: EcrPool, createdAt: Long): EcrRecord =
      EcrRecord(
        playerId = playerId,
        rating = 1500,
        ratingDeviation = 350.0,
        volatility = 0.06,
        gameCount = 0,
        provisional = true,
        pool = pool,
        createdAt = createdAt,
        updatedAt = createdAt,
        ratingPolicyVersion = "ecr-rating-v1",
        calibrationPolicyVersion = "ecr-calibration-v1",
        matchmakingPolicyVersion = "ecr-matchmaking-v1"
      )

  enum RatedMode:
    case NormalRatedEvenChess
    case CasualEvenChess
    case TargetLevelMvp
    case NormalLichessChess

  object EcrUpdateEligibility:
    val updatesAreServerSide = true
    val updatesAreAuditable = true
    val normalLichessRatingMayBePollutedByEvenChess = false

    def updatesNormalEcr(mode: RatedMode): Boolean =
      mode == RatedMode.NormalRatedEvenChess

    def updatesNormalLichessRating(mode: RatedMode): Boolean =
      mode == RatedMode.NormalLichessChess

  final case class EcrGameResult(
      player: EcrRecord,
      opponent: EcrRecord,
      playerUsedOffset: UsedOffset,
      opponentUsedOffset: UsedOffset,
      score: Double,
      modelVersion: String,
      auditEventIds: List[String]
  ):
    def expectedScore: Double =
      ExpectedScore.expectedScore(
        EffectiveRating(player.rating, playerUsedOffset),
        EffectiveRating(opponent.rating, opponentUsedOffset)
      )

    def auditable: Boolean = modelVersion.nonEmpty && auditEventIds.nonEmpty

  object RatingReplay:
    def simpleDelta(result: EcrGameResult, kFactor: Int): Int =
      math.round(kFactor * (result.score - result.expectedScore)).toInt

    def applySimpleUpdate(result: EcrGameResult, kFactor: Int, updatedAt: Long): EcrRecord =
      val delta = simpleDelta(result, kFactor)
      result.player.copy(
        rating = result.player.rating + delta,
        gameCount = result.player.gameCount + 1,
        provisional = result.player.gameCount + 1 < 10,
        updatedAt = updatedAt
      )

  final case class MatchmakingProfile(
      playerId: String,
      ecr: EcrRecord,
      expectedUsedOffset: UsedOffset,
      timeControl: TimeControlBucket,
      setLevel: Level,
      latencyMillis: Int,
      abuseClear: Boolean
  ):
    def expectedEffectiveRating: Int = ecr.rating + expectedUsedOffset.value

  enum SearchStage:
    case Initial
    case Widening1
    case Widening2
    case Widening3
    case Confirmation
    case PostGame

  final case class SearchWindow(
      stage: SearchStage,
      maxEcrDelta: Int,
      maxEffectiveRatingDelta: Int,
      allowLevelCompatibilityExpansion: Boolean,
      requiresConfirmationForLevelContractChange: Boolean
  )

  object SearchWindows:
    val all: List[SearchWindow] = List(
      SearchWindow(SearchStage.Initial, maxEcrDelta = 75, maxEffectiveRatingDelta = 75, false, false),
      SearchWindow(SearchStage.Widening1, maxEcrDelta = 150, maxEffectiveRatingDelta = 75, false, false),
      SearchWindow(SearchStage.Widening2, maxEcrDelta = 150, maxEffectiveRatingDelta = 150, false, false),
      SearchWindow(SearchStage.Widening3, maxEcrDelta = 200, maxEffectiveRatingDelta = 200, true, true)
    )

    val byStage: Map[SearchStage, SearchWindow] = all.map(window => window.stage -> window).toMap

  final case class MatchmakingDecision(
      allowed: Boolean,
      requiresConfirmation: Boolean,
      reasons: List[String]
  )

  object Matchmaking:
    val monitorsRepeatRematchesAndRatingTransfer = true
    val gameStartConfirmationShowsSetLevelAndPool = true
    val postGameReportingExplainsUsedLevelAndUsedOffset = true

    def decide(
        a: MatchmakingProfile,
        b: MatchmakingProfile,
        window: SearchWindow,
        maxLatencyMillis: Int
    ): MatchmakingDecision =
      val reasons = List(
        Option.when(a.ecr.pool != b.ecr.pool)("pool_mismatch"),
        Option.when(a.timeControl != b.timeControl)("time_control_mismatch"),
        Option.when(!a.abuseClear || !b.abuseClear)("abuse_controls"),
        Option.when(a.latencyMillis > maxLatencyMillis || b.latencyMillis > maxLatencyMillis)("latency"),
        Option.when(math.abs(a.ecr.rating - b.ecr.rating) > window.maxEcrDelta)("ecr_window"),
        Option.when(math.abs(a.expectedEffectiveRating - b.expectedEffectiveRating) > window.maxEffectiveRatingDelta)("effective_rating_window"),
        Option.when(a.setLevel != b.setLevel && !window.allowLevelCompatibilityExpansion)("level_contract")
      ).flatten

      MatchmakingDecision(
        allowed = reasons.isEmpty,
        requiresConfirmation = reasons.isEmpty && a.setLevel != b.setLevel && window.requiresConfirmationForLevelContractChange,
        reasons = reasons
      )

  final case class RepeatPattern(
      playerId: String,
      opponentId: String,
      rematchCount: Int,
      ratingTransferDelta: Int
  )

  object RepeatPatternControls:
    def flagged(pattern: RepeatPattern, maxRematches: Int, maxRatingTransferDelta: Int): Boolean =
      pattern.rematchCount > maxRematches || math.abs(pattern.ratingTransferDelta) > maxRatingTransferDelta

  final case class CalibrationResidualSlice(
      usedLevel: Level,
      assistanceLoadBand: String,
      timeControl: TimeControlBucket,
      ecrBand: String,
      exactness: String,
      featureMix: String,
      followRate: String,
      residual: Double
  )

  object CalibrationDashboard:
    val requiredDimensions: Set[String] = Set(
      "Used Level",
      "Assistance Load",
      "time control",
      "ECR band",
      "exactness",
      "feature mix",
      "follow-rate"
    )

    def includesRequiredDimensions(dimensions: Set[String]): Boolean =
      requiredDimensions.subsetOf(dimensions)

  final case class CalibrationSafetyMetrics(
      acceptableResiduals: Boolean,
      acceptableAbuseSignals: Boolean,
      acceptableCompletionRate: Boolean
  ):
    def acceptable: Boolean = acceptableResiduals && acceptableAbuseSignals && acceptableCompletionRate

  object HighLevelRollout:
    val l8ToL10HardBannedByPolicy = false

    def publicRatedAllowed(level: Level, metrics: CalibrationSafetyMetrics): Boolean =
      level.value < 8 || metrics.acceptable

  final case class ExplainableGameVersion(
      gameId: String,
      ratingPolicyVersion: String,
      calibrationPolicyVersion: String,
      matchmakingPolicyVersion: String
  ):
    def explainableUnderOriginalVersions: Boolean =
      gameId.nonEmpty &&
        ratingPolicyVersion.nonEmpty &&
        calibrationPolicyVersion.nonEmpty &&
        matchmakingPolicyVersion.nonEmpty

  object IntegrationRules:
    val inspectLilaRatingAndPairingBeforeSeams = true
    val mayUseSeparateEvenChessRatingServiceIfCoreIntegrationUnsafe = true
    val coreRatingOrPairingEditsRequirePatchMap = true
