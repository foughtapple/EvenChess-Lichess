package lila.evenchess

import CoachingLadder.{ ExactnessClass, Level, UiSlot }
import CoachingOverlays.OverlayVisibility
import CoachingPolicy.{ AssistanceLedger, AuditEvent }
import EvenChessMode.TimeControlBucket

object AssistanceAccounting:

  enum Criticality:
    case Normal
    case TacticalSwing
    case Mate
    case Tablebase
    case BlunderPoint

  enum ClockPressure:
    case Normal
    case LowTime
    case IncrementScramble

  enum FollowRate:
    case Followed
    case Avoided
    case Ignored
    case Unknown

  enum Quality:
    case Degraded
    case Fallback
    case Normal
    case ExactProof

  final case class AssistanceDimensions(
      timeControl: TimeControlBucket,
      criticality: Criticality,
      clockPressure: ClockPressure,
      followRate: FollowRate,
      quality: Quality,
      staleNonDecisionHelp: Boolean,
      postGameReview: Boolean
  )

  object AssistanceDimensions:
    val defaultLive: AssistanceDimensions =
      AssistanceDimensions(
        TimeControlBucket.Rapid,
        Criticality.Normal,
        ClockPressure.Normal,
        FollowRate.Unknown,
        Quality.Normal,
        staleNonDecisionHelp = false,
        postGameReview = false
      )

  final case class CalibrationParameters(
      modelVersion: String,
      exactnessMultipliers: Map[ExactnessClass, Double],
      surfaceMultipliers: Map[UiSlot, Double],
      timeControlMultipliers: Map[TimeControlBucket, Double],
      criticalityMultipliers: Map[Criticality, Double],
      clockPressureMultipliers: Map[ClockPressure, Double],
      visibilityMultipliers: Map[OverlayVisibility, Double],
      followMultipliers: Map[FollowRate, Double],
      qualityMultipliers: Map[Quality, Double],
      loadToOffsetScale: Double,
      baseOffsetByUsedLevel: Map[Int, Int],
      maxUsedOffset: Int
  ):
    def hasVersion: Boolean = modelVersion.nonEmpty

  object CalibrationParameters:
    val default: CalibrationParameters =
      CalibrationParameters(
        modelVersion = "assistance-load-v1",
        exactnessMultipliers = Map(
          ExactnessClass.None -> 0.0,
          ExactnessClass.ExactRules -> 0.2,
          ExactnessClass.LocalDeterministic -> 0.5,
          ExactnessClass.LocalEstimate -> 0.8,
          ExactnessClass.Heuristic -> 0.9,
          ExactnessClass.Mixed -> 1.0,
          ExactnessClass.Approximate -> 1.2,
          ExactnessClass.ExactWhereConfigured -> 1.4,
          ExactnessClass.AiMustValidate -> 1.1
        ),
        surfaceMultipliers = Map(
          UiSlot.Board -> 0.4,
          UiSlot.BoardLayer -> 1.2,
          UiSlot.MoveList -> 0.2,
          UiSlot.ResultState -> 0.1,
          UiSlot.Summary -> 0.8,
          UiSlot.Warning -> 1.1,
          UiSlot.OffsetCard -> 1.0,
          UiSlot.PlanCard -> 1.0,
          UiSlot.OpeningEndgameCard -> 0.9,
          UiSlot.CandidateArea -> 1.4,
          UiSlot.CoachReview -> 0.0,
          UiSlot.AiExplain -> 1.0,
          UiSlot.Review -> 0.0
        ),
        timeControlMultipliers = Map(
          TimeControlBucket.Bullet -> 1.5,
          TimeControlBucket.Blitz -> 1.3,
          TimeControlBucket.Rapid -> 1.0,
          TimeControlBucket.Classical -> 0.8,
          TimeControlBucket.Correspondence -> 0.9,
          TimeControlBucket.Casual -> 0.0
        ),
        criticalityMultipliers = Map(
          Criticality.Normal -> 1.0,
          Criticality.TacticalSwing -> 1.4,
          Criticality.Mate -> 1.8,
          Criticality.Tablebase -> 1.6,
          Criticality.BlunderPoint -> 1.5
        ),
        clockPressureMultipliers = Map(
          ClockPressure.Normal -> 1.0,
          ClockPressure.LowTime -> 1.4,
          ClockPressure.IncrementScramble -> 1.6
        ),
        visibilityMultipliers = Map(
          OverlayVisibility.Hidden -> 0.0,
          OverlayVisibility.Compact -> 0.8,
          OverlayVisibility.Visible -> 1.0,
          OverlayVisibility.Suppressed -> 0.0
        ),
        followMultipliers = Map(
          FollowRate.Followed -> 1.2,
          FollowRate.Avoided -> 0.8,
          FollowRate.Ignored -> 0.5,
          FollowRate.Unknown -> 1.0
        ),
        qualityMultipliers = Map(
          Quality.Degraded -> 0.5,
          Quality.Fallback -> 0.7,
          Quality.Normal -> 1.0,
          Quality.ExactProof -> 1.4
        ),
        loadToOffsetScale = 0.08,
        baseOffsetByUsedLevel = Map(
          0 -> 0,
          1 -> 5,
          2 -> 10,
          3 -> 18,
          4 -> 28,
          5 -> 45,
          6 -> 65,
          7 -> 90,
          8 -> 120,
          9 -> 155,
          10 -> 190
        ),
        maxUsedOffset = 300
      )

  final case class AssistanceLoad(value: Double):
    def +(other: AssistanceLoad): AssistanceLoad = AssistanceLoad(value + other.value)

  object AssistanceLoad:
    val zero: AssistanceLoad = AssistanceLoad(0.0)

  object AssistanceLoadFormula:
    def fromAuditEvent(
        event: AuditEvent,
        dimensions: AssistanceDimensions,
        calibration: CalibrationParameters
    ): AssistanceLoad =
      if event.assistanceWeightDelta <= 0 ||
        event.visibility == OverlayVisibility.Hidden ||
        event.visibility == OverlayVisibility.Suppressed ||
        dimensions.staleNonDecisionHelp ||
        dimensions.postGameReview
      then AssistanceLoad.zero
      else
        AssistanceLoad(
          event.assistanceWeightDelta *
            calibration.exactnessMultipliers(event.exactnessClass) *
            calibration.surfaceMultipliers(event.surface) *
            calibration.timeControlMultipliers(dimensions.timeControl) *
            calibration.criticalityMultipliers(dimensions.criticality) *
            calibration.clockPressureMultipliers(dimensions.clockPressure) *
            calibration.visibilityMultipliers(event.visibility) *
            calibration.followMultipliers(dimensions.followRate) *
            calibration.qualityMultipliers(dimensions.quality)
        )

  final case class UsedLevelState(playerId: String, usedLevel: Level)

  object UsedLevelState:
    def initial(playerId: String): UsedLevelState = UsedLevelState(playerId, Level(0))

    def afterEvent(
        current: UsedLevelState,
        event: AuditEvent,
        dimensions: AssistanceDimensions
    ): UsedLevelState =
      val deliveredOrConsumed =
        event.deliveredLevel.isDefined &&
          event.assistanceWeightDelta > 0 &&
          !dimensions.staleNonDecisionHelp &&
          !dimensions.postGameReview &&
          event.visibility != OverlayVisibility.Hidden &&
          event.visibility != OverlayVisibility.Suppressed

      if deliveredOrConsumed then
        val nextLevel = math.max(current.usedLevel.value, event.deliveredLevel.get.value)
        current.copy(usedLevel = Level(nextLevel))
      else current

  final case class AccountingRecord(
      event: AuditEvent,
      dimensions: AssistanceDimensions,
      load: AssistanceLoad
  )

  final case class PlayerAssistanceSummary(
      playerId: String,
      usedLevel: Level,
      assistanceLoad: AssistanceLoad,
      modelVersion: String
  )

  object AssistanceSummaries:
    def recomputeForPlayer(
        playerId: String,
        ledger: AssistanceLedger,
        dimensionsByEventId: Map[String, AssistanceDimensions],
        calibration: CalibrationParameters
    ): PlayerAssistanceSummary =
      val initial = PlayerAssistanceSummary(playerId, Level(0), AssistanceLoad.zero, calibration.modelVersion)

      ledger.events.filter(_.playerId == playerId).foldLeft(initial) { (summary, event) =>
        val dimensions = dimensionsByEventId.getOrElse(event.eventId, AssistanceDimensions.defaultLive)
        val load = AssistanceLoadFormula.fromAuditEvent(event, dimensions, calibration)
        val usedLevel = UsedLevelState.afterEvent(
          UsedLevelState(playerId, summary.usedLevel),
          event,
          dimensions
        ).usedLevel

        summary.copy(
          usedLevel = usedLevel,
          assistanceLoad = summary.assistanceLoad + load
        )
      }

  final case class UsedOffset(value: Int, modelVersion: String):
    def nonNegative: Boolean = value >= 0

  object UsedOffset:
    def fromSummary(
        summary: PlayerAssistanceSummary,
        calibration: CalibrationParameters
    ): UsedOffset =
      val base = calibration.baseOffsetByUsedLevel(summary.usedLevel.value)
      val loadCorrection = math.round(summary.assistanceLoad.value * calibration.loadToOffsetScale).toInt
      UsedOffset(math.min(calibration.maxUsedOffset, base + loadCorrection), calibration.modelVersion)

  final case class FairnessExcludedState(
      subscriptionTier: String,
      adCampaign: Option[String],
      tokenBalance: Int,
      marketingVariant: Option[String]
  )

  object FairnessExclusions:
    val subscriptionsMayLowerUsedOffset = false
    val adsMayLowerUsedOffset = false
    val tokensMayLowerUsedOffset = false
    val marketingMayLowerUsedOffset = false

    def offsetWithExcludedState(
        summary: PlayerAssistanceSummary,
        calibration: CalibrationParameters,
        state: FairnessExcludedState
    ): UsedOffset =
      UsedOffset.fromSummary(summary, calibration)

  final case class EffectiveRating(ecr: Int, usedOffset: UsedOffset):
    def value: Int = ecr + usedOffset.value

  object ExpectedScore:
    def expectedScore(player: EffectiveRating, opponent: EffectiveRating): Double =
      1.0 / (1.0 + math.pow(10.0, (opponent.value - player.value) / 400.0))

  object AccountingAuthority:
    val serverAuthoritative = true
    val mayStoreOutsideCoreGameRecordsIfJoinable = true
    val coreRatingFlowEditsRequirePatchMap = true
