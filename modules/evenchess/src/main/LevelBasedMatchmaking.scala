package lila.evenchess

import AssistanceAccounting.UsedOffset
import CoachingLadder.Level
import EcrRating.{ EcrGameResult, EcrPool, EcrRecord, EcrUpdateEligibility, MatchmakingProfile, RatedMode, RatingReplay, SearchStage, SearchWindow, SearchWindows }
import EvenChessMode.TimeControlBucket
import ProductInvariants.RequirementClass
import TelemetryAnalytics.{ EventAuthority, EventFamily, TelemetryEvent, TelemetryEventName, VersionSet }
import scala.util.Random

object LevelBasedMatchmaking:

  final case class AntiDetectLatencyProfile(
      baseDelayMillis: Int,
      jitterRangeMillis: Int,
      minDelayMillis: Int,
      maxDelayMillis: Int
  ):
    def valid: Boolean =
      baseDelayMillis >= 0 &&
        jitterRangeMillis >= 0 &&
        minDelayMillis >= 0 &&
        maxDelayMillis >= minDelayMillis &&
        baseDelayMillis <= maxDelayMillis

    def sampleDelayMillis(random: Random): Int =
      val jitter = if jitterRangeMillis == 0 then 0 else random.nextInt(jitterRangeMillis * 2 + 1) - jitterRangeMillis
      val raw = baseDelayMillis + jitter
      math.max(minDelayMillis, math.min(maxDelayMillis, raw))

    def sampleDelayMillis(random: Random, maxLatencyMillis: Int): Int =
      val jitter = if jitterRangeMillis == 0 then 0 else random.nextInt(jitterRangeMillis * 2 + 1) - jitterRangeMillis
      val raw = baseDelayMillis + jitter
      val cappedMax = math.max(minDelayMillis, math.min(maxDelayMillis, maxLatencyMillis))
      math.max(minDelayMillis, math.min(cappedMax, raw))

    def withDefaultsFor(timeControl: Option[TimeControlBucket]): AntiDetectLatencyProfile =
      timeControl.getOrElse(TimeControlBucket.Rapid) match
        case TimeControlBucket.Bullet       => copy(baseDelayMillis = 1700, jitterRangeMillis = 600, maxDelayMillis = 4000)
        case TimeControlBucket.Blitz        => copy(baseDelayMillis = 1900, jitterRangeMillis = 700, maxDelayMillis = 4500)
        case TimeControlBucket.Rapid        => copy(baseDelayMillis = 2200, jitterRangeMillis = 900, maxDelayMillis = 5500)
        case TimeControlBucket.Classical    => copy(baseDelayMillis = 2600, jitterRangeMillis = 1200, maxDelayMillis = 7000)
        case TimeControlBucket.Correspondence => copy(baseDelayMillis = 2800, jitterRangeMillis = 1100, maxDelayMillis = 6500)
        case TimeControlBucket.Casual       => copy(baseDelayMillis = 2000, jitterRangeMillis = 650, maxDelayMillis = 4200)

    def describe: String =
      s"delay(base=$baseDelayMillis, jitter=$jitterRangeMillis, range=$minDelayMillis-$maxDelayMillis)"

  object AntiDetectLatencyProfile:
    val default: AntiDetectLatencyProfile =
      AntiDetectLatencyProfile(
        baseDelayMillis = 2200,
        jitterRangeMillis = 900,
        minDelayMillis = 800,
        maxDelayMillis = 5200
      )

  enum BotMatchPersona:
    case HumanLike
    case Fast

    def label: String =
      this match
        case HumanLike => "human-like"
        case Fast      => "fast"

    def isHumanLike: Boolean = this == HumanLike
    def isFast: Boolean = this == Fast

  final case class StockfishStrengthProfile(
      setLevel: Level,
      sfDepth: Int,
      skill: Int,
      multipv: Int
  ):
    def valid: Boolean =
      Level.isValid(setLevel.value) &&
        sfDepth > 0 &&
        skill >= 0 &&
        skill <= 20 &&
        multipv > 0

  object StockfishStrengthProfile:
    private val byLevel: Map[Int, StockfishStrengthProfile] = Map(
      0 -> StockfishStrengthProfile(Level(0), 1, 0, 1),
      1 -> StockfishStrengthProfile(Level(1), 4, 4, 1),
      2 -> StockfishStrengthProfile(Level(2), 6, 6, 1),
      3 -> StockfishStrengthProfile(Level(3), 10, 8, 1),
      4 -> StockfishStrengthProfile(Level(4), 12, 10, 1),
      5 -> StockfishStrengthProfile(Level(5), 16, 12, 2),
      6 -> StockfishStrengthProfile(Level(6), 20, 14, 2),
      7 -> StockfishStrengthProfile(Level(7), 22, 16, 2),
      8 -> StockfishStrengthProfile(Level(8), 24, 18, 3),
      9 -> StockfishStrengthProfile(Level(9), 26, 19, 3),
      10 -> StockfishStrengthProfile(Level(10), 28, 20, 3)
    )

    val default: StockfishStrengthProfile = byLevel(5)

    def forLevel(level: Level): StockfishStrengthProfile =
      byLevel.getOrElse(level.value, default)

  final case class StockfishLevelWindow(
      level: Level,
      minRating: Int,
      maxRating: Int
  ):
    def contains(rating: Int): Boolean =
      rating >= minRating && rating <= maxRating

    def windowText: String =
      s"${minRating}-${maxRating}"

  object StockfishRatingByElo:
    private val windows: List[StockfishLevelWindow] = List(
      StockfishLevelWindow(Level(0), 0, 699),
      StockfishLevelWindow(Level(1), 700, 899),
      StockfishLevelWindow(Level(2), 900, 1099),
      StockfishLevelWindow(Level(3), 1100, 1299),
      StockfishLevelWindow(Level(4), 1300, 1499),
      StockfishLevelWindow(Level(5), 1500, 1699),
      StockfishLevelWindow(Level(6), 1700, 1899),
      StockfishLevelWindow(Level(7), 1900, 2149),
      StockfishLevelWindow(Level(8), 2150, 2349),
      StockfishLevelWindow(Level(9), 2350, 2599),
      StockfishLevelWindow(Level(10), 2600, 100000)
    )

    def windowFor(level: Level): StockfishLevelWindow =
      windows.find(_.level.value == level.value).getOrElse(windows.last)

    def ratingWindowText(level: Level): String =
      windowFor(level).windowText

    def levelForRating(rating: Int): Level =
      windows.find(_.contains(rating)).map(_.level).getOrElse(
        if rating <= windows.head.maxRating then windows.head.level
        else windows.last.level
      )

  object LichessEquivalentStockfishLevel:
    private def normalizedLevel(level: Int): Int =
      math.max(1, math.min(8, level))

    def windowFor(level: Int): EvenChessRatingCalibration.StockfishAiLevelBand =
      EvenChessRatingCalibration.StockfishAiRatingRuntime.active.windowFor(normalizedLevel(level))

    def windowFor(level: Level): EvenChessRatingCalibration.StockfishAiLevelBand =
      windowFor(level.value)

    def ratingWindowText(level: Int): String =
      EvenChessRatingCalibration.StockfishAiRatingRuntime.active.windowText(normalizedLevel(level))

    def ratingWindowText(level: Level): String =
      ratingWindowText(level.value)

    def levelForRating(rating: Int): Level =
      EvenChessRatingCalibration.StockfishAiRatingRuntime.active.levelForRating(rating)

    def levelForSetLevel(level: Level): Level =
      val setLevelRating = EvenChessRatingCalibration.EcorRuntime.active.ratingForBandCenter(level)
      val aiLevel = levelForRating(setLevelRating).value
      Level(math.max(1, math.min(8, aiLevel)))

  object LevelOffsetTable:
    def policyVersion: String = EvenChessRatingCalibration.EcorRuntime.active.version
    def offsets: Map[Int, Int] = EvenChessRatingCalibration.EcorRuntime.active.cumulativeOffsets

    def maxOffset: Int = offsets.values.max

    def offsetForLevel(level: Level): UsedOffset =
      UsedOffset(offsets.getOrElse(level.value, 0), policyVersion)

    def valueForLevel(level: Level): Int =
      offsetForLevel(level).value

    def nearestLevelForOffset(offset: Int): Level =
      EvenChessRatingCalibration.EcorRuntime.active.nearestLevelForOffset(offset)

    def valid: Boolean =
      (Level.min to Level.max).forall(offsets.contains) &&
        offsets(Level.min) == 0 &&
        offsets.toList.sortBy(_._1).sliding(2).forall:
          case List((_, previous), (_, next)) => next >= previous
          case _                             => true

  object BaseSetLevelByRatingTable:
    def policyVersion: String = EvenChessRatingCalibration.EcorRuntime.active.version

    def levelForRating(rating: Int): Level =
      EvenChessRatingCalibration.EcorRuntime.active.levelForRating(rating)

  object FriendLevelContract:

    enum Mode(val key: String, val label: String):
      case AutoLevel extends Mode("auto", "Auto level")
      case SetMyLevel extends Mode("my", "Set my level")
      case SetOpponentLevel extends Mode("opponent", "Set opponent level")
      case SetBothLevels extends Mode("both", "Set both levels")

    object Mode:
      def fromKey(key: Option[String]): Mode =
        key.flatMap(raw => values.find(_.key == raw.trim.toLowerCase)).getOrElse(AutoLevel)

    final case class Request(
        mode: Mode,
        myLevel: Option[Level],
        opponentLevel: Option[Level]
    ):
      def valid: Boolean =
        mode match
          case Mode.AutoLevel        => myLevel.isEmpty && opponentLevel.isEmpty
          case Mode.SetMyLevel       => myLevel.exists(l => Level.isValid(l.value)) && opponentLevel.isEmpty
          case Mode.SetOpponentLevel => opponentLevel.exists(l => Level.isValid(l.value)) && myLevel.isEmpty
          case Mode.SetBothLevels =>
            myLevel.exists(l => Level.isValid(l.value)) &&
              opponentLevel.exists(l => Level.isValid(l.value))

    object Request:
      private def parseLevel(value: Option[String]): Option[Level] =
        value.flatMap(raw => raw.trim.toIntOption).filter(Level.isValid).map(Level(_))

      def fromFormValues(
          mode: Option[String],
          myLevel: Option[String],
          opponentLevel: Option[String]
      ): Request =
        val selectedMode = Mode.fromKey(mode)
        selectedMode match
          case Mode.AutoLevel =>
            Request(selectedMode, None, None)
          case Mode.SetMyLevel =>
            Request(selectedMode, parseLevel(myLevel), None)
          case Mode.SetOpponentLevel =>
            Request(selectedMode, None, parseLevel(opponentLevel))
          case Mode.SetBothLevels =>
            Request(selectedMode, parseLevel(myLevel), parseLevel(opponentLevel))

    final case class Result(
        mode: Mode,
        challengerLevel: Level,
        opponentLevel: Level,
        challengerOffset: UsedOffset,
        opponentOffset: UsedOffset,
        challengerEffectiveRating: Int,
        opponentEffectiveRating: Int,
        unevenMatch: Boolean,
        unevenReason: Option[String]
    ):
      def valid: Boolean =
        Level.isValid(challengerLevel.value) &&
          Level.isValid(opponentLevel.value) &&
          challengerOffset.nonNegative &&
          opponentOffset.nonNegative &&
          challengerEffectiveRating > 0 &&
          opponentEffectiveRating > 0 &&
          mode.key.nonEmpty &&
          unevenReason.forall(_.nonEmpty)

      def requestCardSummary: String =
        val uneven = if unevenMatch then " Uneven match flagged for rating settlement." else ""
        s"${mode.label}: challenger L${challengerLevel.value}, recipient L${opponentLevel.value}.$uneven"

    private def levels: List[Level] =
      (Level.min to Level.max).map(Level(_)).toList

    private def baseLowerLevel(challengerRating: Int, opponentRating: Int): (Boolean, Level) =
      if challengerRating <= opponentRating then true -> BaseSetLevelByRatingTable.levelForRating(challengerRating)
      else false -> BaseSetLevelByRatingTable.levelForRating(opponentRating)

    def assign(
        challengerRating: Int,
        opponentRating: Int,
        request: Request
    ): Option[Result] =
      if !request.valid then None
      else
        val (challengerIsLower, lowerBaseLevel) = baseLowerLevel(challengerRating, opponentRating)
        val challengerLevels =
          request.myLevel
            .map(List(_))
            .getOrElse(if challengerIsLower && request.mode == Mode.AutoLevel then List(lowerBaseLevel) else levels)
        val opponentLevels =
          request.opponentLevel
            .map(List(_))
            .getOrElse(if !challengerIsLower && request.mode == Mode.AutoLevel then List(lowerBaseLevel) else levels)

        val assignments =
          for
            challengerLevel <- challengerLevels
            opponentLevel <- opponentLevels
            challengerOffset = LevelOffsetTable.offsetForLevel(challengerLevel)
            opponentOffset = LevelOffsetTable.offsetForLevel(opponentLevel)
            challengerEffective = challengerRating + challengerOffset.value
            opponentEffective = opponentRating + opponentOffset.value
          yield
            (
              challengerLevel,
              opponentLevel,
              challengerOffset,
              opponentOffset,
              challengerEffective,
              opponentEffective
            )

        assignments
          .sortBy { case (challengerLevel, opponentLevel, _, _, challengerEffective, opponentEffective) =>
            (
              math.abs(challengerEffective - opponentEffective),
              math.abs(challengerRating - opponentRating),
              math.abs(challengerLevel.value - opponentLevel.value)
            )
          }
          .headOption
          .map { case (challengerLevel, opponentLevel, challengerOffset, opponentOffset, challengerEffective, opponentEffective) =>
            val fixedBoth = request.myLevel.isDefined && request.opponentLevel.isDefined
            val effectiveDelta = math.abs(challengerEffective - opponentEffective)
            val uneven = fixedBoth && effectiveDelta > SearchWindows.byStage(SearchStage.Widening3).maxEffectiveRatingDelta
            Result(
              mode = request.mode,
              challengerLevel = challengerLevel,
              opponentLevel = opponentLevel,
              challengerOffset = challengerOffset,
              opponentOffset = opponentOffset,
              challengerEffectiveRating = challengerEffective,
              opponentEffectiveRating = opponentEffective,
              unevenMatch = uneven,
              unevenReason = Option.when(uneven)("fixed_friend_levels_outside_effective_rating_window")
            )
          }

  final case class BotMatchProfile(
      botId: String,
      userRef: Option[String],
      targetEcr: Int,
      preferredSetLevel: Level,
      stockfishLevel: Level,
      timeControl: Option[TimeControlBucket],
      antiDetectProfile: AntiDetectLatencyProfile = AntiDetectLatencyProfile.default,
      matchLatencyProfile: AntiDetectLatencyProfile = AntiDetectLatencyProfile(160, 55, 95, 235),
      persona: BotMatchPersona = BotMatchPersona.HumanLike
  ):
    def valid: Boolean =
      botId.nonEmpty &&
        targetEcr > 0 &&
        Level.isValid(preferredSetLevel.value) &&
        Level.isValid(stockfishLevel.value) &&
        targetEcr <= 5000 &&
        antiDetectProfile.valid &&
        matchLatencyProfile.valid &&
        minMatchLatency <= matchLatencyProfile.maxDelayMillis &&
        maxMatchLatency >= matchLatencyProfile.minDelayMillis

    def stockfishProfile: StockfishStrengthProfile =
      StockfishStrengthProfile.forLevel(stockfishLevel)

    def nextThinkDelay(random: Random): Int =
      antiDetectProfile.sampleDelayMillis(random)

    def nextMatchLatency(random: Random, capMillis: Int = 250): Int =
      matchLatencyProfile.sampleDelayMillis(random, capMillis)

    def matchabilityWindowHumanized: Boolean = persona.isHumanLike

    private def minMatchLatency = 40
    private def maxMatchLatency = 250

  object BotMatchProfile:
    private case class DistributionConfig(mean: Int, standardDeviation: Int, min: Int, max: Int)

    private val ratedDistribution = DistributionConfig(1500, 250, 700, 2400)
    private val casualDistribution = DistributionConfig(1350, 300, 600, 2100)

    private def distributionFor(pool: EcrPool): DistributionConfig =
      pool match
        case EcrPool.TargetShadow => ratedDistribution
        case EcrPool.NormalLichess => casualDistribution
        case EcrPool.Bullet | EcrPool.Blitz | EcrPool.Rapid | EcrPool.Classical | EcrPool.Correspondence =>
          ratedDistribution

    private def poolBounds(pool: EcrPool): (Int, Int) =
      val config = distributionFor(pool)
      (config.min, config.max)

    private def normalizeLevel(value: Int): Level =
      Level(math.max(Level.min, math.min(Level.max, value)))

    private def seedFromBotId(botId: String): Long =
      var value = 0x9E3779B97F4A7C15L
      botId.foreach(c => value = value ^ (value << 5) + c.toLong + (value >>> 2))
      value

    private def clamp(value: Int, min: Int, max: Int): Int =
      math.max(min, math.min(max, value))

    private def randomIntFromGaussian(random: Random, config: DistributionConfig): Int =
      val sample = random.nextGaussian() * config.standardDeviation + config.mean
      clamp(math.round(sample).toInt, config.min, config.max)

    private def pickPersona(seedRandom: Random, pool: EcrPool): BotMatchPersona =
      val fastTarget = pool match
        case EcrPool.Bullet | EcrPool.Blitz => 72
        case EcrPool.Correspondence         => 40
        case _                             => 55

      if seedRandom.nextInt(100) < fastTarget then BotMatchPersona.Fast else BotMatchPersona.HumanLike

    private def moveProfile(
        persona: BotMatchPersona,
        timeControl: Option[TimeControlBucket]
    ): AntiDetectLatencyProfile =
      val base = AntiDetectLatencyProfile.default.withDefaultsFor(timeControl)
      persona match
        case BotMatchPersona.HumanLike =>
          base
        case BotMatchPersona.Fast =>
          base.copy(
            baseDelayMillis = math.max(600, base.baseDelayMillis / 2),
            jitterRangeMillis = math.max(60, base.jitterRangeMillis / 2),
            minDelayMillis = math.max(200, base.minDelayMillis / 2),
            maxDelayMillis = math.max(1200, base.maxDelayMillis - 1200)
          )

    private def matchProfileFor(timeControl: Option[TimeControlBucket], persona: BotMatchPersona): AntiDetectLatencyProfile =
      persona match
        case BotMatchPersona.HumanLike =>
          timeControl match
            case Some(TimeControlBucket.Bullet)      => AntiDetectLatencyProfile(150, 45, 90, 220)
            case Some(TimeControlBucket.Blitz)       => AntiDetectLatencyProfile(170, 60, 90, 230)
            case Some(TimeControlBucket.Correspondence) => AntiDetectLatencyProfile(170, 60, 100, 240)
            case Some(_) | None                     => AntiDetectLatencyProfile(160, 55, 95, 235)
        case BotMatchPersona.Fast =>
          timeControl match
            case Some(TimeControlBucket.Bullet)      => AntiDetectLatencyProfile(95, 20, 40, 175)
            case Some(TimeControlBucket.Blitz)       => AntiDetectLatencyProfile(110, 22, 45, 185)
            case Some(TimeControlBucket.Correspondence) => AntiDetectLatencyProfile(150, 40, 60, 200)
            case Some(_) | None                     => AntiDetectLatencyProfile(120, 24, 50, 190)

    private def setLevelFromEcr(ecr: Int): Level =
      BaseSetLevelByRatingTable.levelForRating(ecr)

    def alignToPlayerPreference(
        playerProfile: BotMatchProfile,
        preferredEcr: Int,
        preferredSetLevel: Level,
        pool: EcrPool
    ): BotMatchProfile =
      val (minEcr, maxEcr) = poolBounds(pool)
      val jitterRandom = scala.util.Random(seedFromBotId(s"${playerProfile.botId}|$preferredEcr|${preferredSetLevel.value}"))
      val ecrJitter = (jitterRandom.nextGaussian() * 80.0).toInt
      val targetedEcr = clamp(preferredEcr + ecrJitter, minEcr, maxEcr)
      val levelJitter = jitterRandom.nextInt(5) - 2
      val targetedSetLevel = normalizeLevel(preferredSetLevel.value + levelJitter)
      playerProfile.copy(
        targetEcr = targetedEcr,
        preferredSetLevel = targetedSetLevel,
        stockfishLevel = LichessEquivalentStockfishLevel.levelForRating(targetedEcr),
        antiDetectProfile = playerProfile.antiDetectProfile,
        matchLatencyProfile = playerProfile.matchLatencyProfile
      )

    def fromSeed(
        botId: String,
        userRef: Option[String],
        timeControl: Option[TimeControlBucket],
        pool: EcrPool,
        persona: Option[BotMatchPersona] = None
    ): BotMatchProfile =
      val random = Random(seedFromBotId(botId))
      val config = distributionFor(pool)
      val ecr = randomIntFromGaussian(random, config)
      val selectedPersona = persona.getOrElse(pickPersona(random, pool))
      fromValues(
        botId = botId,
        userRef = userRef,
        targetEcr = ecr,
        preferredSetLevel = setLevelFromEcr(ecr),
        stockfishLevel = LichessEquivalentStockfishLevel.levelForRating(ecr),
        timeControl = timeControl,
        persona = selectedPersona
      )

    def random(
        botId: String,
        timeControl: Option[TimeControlBucket],
        pool: EcrPool
    ): BotMatchProfile =
      val random = Random()
      val config = distributionFor(pool)
      val ecr = randomIntFromGaussian(random, config)
      val selectedPersona = pickPersona(random, pool)
      fromValues(
        botId = botId,
        userRef = None,
        targetEcr = ecr,
        preferredSetLevel = setLevelFromEcr(ecr),
        stockfishLevel = LichessEquivalentStockfishLevel.levelForRating(ecr),
        timeControl = timeControl,
        persona = selectedPersona
      )

    private def fromValues(
        botId: String,
        userRef: Option[String],
        targetEcr: Int,
        preferredSetLevel: Level,
        stockfishLevel: Level,
        timeControl: Option[TimeControlBucket],
        persona: BotMatchPersona
    ): BotMatchProfile =
      BotMatchProfile(
        botId = botId,
        userRef = userRef,
        targetEcr = targetEcr,
        preferredSetLevel = preferredSetLevel,
        stockfishLevel = stockfishLevel,
        timeControl = timeControl,
        antiDetectProfile = moveProfile(persona, timeControl),
        matchLatencyProfile = matchProfileFor(timeControl, persona),
        persona = persona
      )

  enum PhaseDRequirement:
    case SearchUsesEcrAndSetLevel
    case NormalLichessRatingsExcluded
    case MmrEngineOwnsPublicMatchmaking
    case MmrEngineOutputsMatchContract
    case SearchWideningOrder
    case TwoStateSearchScenarios
    case LevelContractConfirmation
    case WideningVisibleToPlayer
    case MatchContractsLoggedForAudit
    case SimulationBeforeProduction
    case TargetQueueIsolation
    case RatingAppliedServerSide
    case TelemetryForReplayAndCalibration
    case LilaPairingIntegrationDeferredToThinSeams

  final case class PhaseDRequirementClassification(
      requirement: PhaseDRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseDRequirementClassifications:
    val all: List[PhaseDRequirementClassification] = List(
      PhaseDRequirementClassification(
        PhaseDRequirement.SearchUsesEcrAndSetLevel,
        RequirementClass.EvenChessSpecific,
        "Search tickets carry ECR, expected Used Offset, Effective Rating, Set Level, time control, pool key, and server-owned policy versions."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.NormalLichessRatingsExcluded,
        RequirementClass.LichessProvided,
        "Normal Lichess ratings remain separate and are not accepted as EvenChess ECR inputs."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.MmrEngineOwnsPublicMatchmaking,
        RequirementClass.EvenChessSpecific,
        "The EvenChess MMR Engine owns public matchmaking decisions and hands Lichess a finalized contract."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.MmrEngineOutputsMatchContract,
        RequirementClass.EvenChessSpecific,
        "The framework outputs a match contract with ECR/MMR, expected offset, effective rating, preference flags, token gate result, and policy version."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.SearchWideningOrder,
        RequirementClass.EvenChessSpecific,
        "Widen ECR first, then expected Effective Rating, then level compatibility only with confirmation."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.TwoStateSearchScenarios,
        RequirementClass.EvenChessSpecific,
        "Normal and preferred-own-set-level searches are represented explicitly before production pairing."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.LevelContractConfirmation,
        RequirementClass.AdaptedToLichessFork,
        "Any material Set Level or Target Level contract change requires explicit confirmation before pairing."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.WideningVisibleToPlayer,
        RequirementClass.AdaptedToLichessFork,
        "Match contracts expose whether target preferences were strict, widened, or relaxed so the UI can explain the outcome."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.MatchContractsLoggedForAudit,
        RequirementClass.EvenChessSpecific,
        "Every generated contract has a matching audit/calibration log row and stable policy version."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.SimulationBeforeProduction,
        RequirementClass.EvenChessSpecific,
        "A deterministic simulation runner evaluates candidate tickets without touching live lila pairing."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.TargetQueueIsolation,
        RequirementClass.EvenChessSpecific,
        "Target Level mode uses a separate queue and never updates normal ECR."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.RatingAppliedServerSide,
        RequirementClass.EvenChessSpecific,
        "ECR updates are server-side, auditable, versioned, and isolated from normal Lichess rating updates."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.TelemetryForReplayAndCalibration,
        RequirementClass.EvenChessSpecific,
        "Search, game start, and rating events are server-authored and versioned for replay and calibration."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.LilaPairingIntegrationDeferredToThinSeams,
        RequirementClass.AdaptedToLichessFork,
        "Later lila search/pool integration should call this service from a narrow patch-mapped adapter."
      )
    )

  enum SearchQueue:
    case NormalEvenChess
    case CasualEvenChess
    case TargetLevel
    case AiPractice

    def updatesNormalEcr: Boolean = this == NormalEvenChess

    def ratedMode: RatedMode =
      this match
        case NormalEvenChess => RatedMode.NormalRatedEvenChess
        case CasualEvenChess => RatedMode.CasualEvenChess
        case TargetLevel     => RatedMode.TargetLevelMvp
        case AiPractice      => RatedMode.CasualEvenChess

  final case class PoolKey(
      queue: SearchQueue,
      timeControl: TimeControlBucket,
      ecrPool: EcrPool
  ):
    def key: String =
      s"${queue.toString.toLowerCase}-${timeControl.toString.toLowerCase}-${ecrPool.toString.toLowerCase}"

    def valid: Boolean =
      ecrPool != EcrPool.NormalLichess &&
        (queue match
          case SearchQueue.NormalEvenChess =>
            timeControl != TimeControlBucket.Casual &&
              ecrPool != EcrPool.TargetShadow
          case SearchQueue.CasualEvenChess =>
            ecrPool != EcrPool.TargetShadow
          case SearchQueue.TargetLevel =>
            ecrPool == EcrPool.TargetShadow
          case SearchQueue.AiPractice =>
            ecrPool != EcrPool.TargetShadow
        )

  object PoolKey:
    def normal(timeControl: TimeControlBucket): PoolKey =
      PoolKey(SearchQueue.NormalEvenChess, timeControl, ecrPoolFor(timeControl))

    def casual(timeControl: TimeControlBucket): PoolKey =
      PoolKey(SearchQueue.CasualEvenChess, timeControl, ecrPoolFor(timeControl))

    def target(timeControl: TimeControlBucket): PoolKey =
      PoolKey(SearchQueue.TargetLevel, timeControl, EcrPool.TargetShadow)

    private def ecrPoolFor(timeControl: TimeControlBucket): EcrPool =
      timeControl match
        case TimeControlBucket.Bullet         => EcrPool.Bullet
        case TimeControlBucket.Blitz          => EcrPool.Blitz
        case TimeControlBucket.Rapid          => EcrPool.Rapid
        case TimeControlBucket.Classical      => EcrPool.Classical
        case TimeControlBucket.Correspondence => EcrPool.Correspondence
        case TimeControlBucket.Casual         => EcrPool.Rapid

  final case class SearchTicket(
      ticketId: String,
      playerId: String,
      poolKey: PoolKey,
      requestedClock: Option[RequestedClock],
      ecr: EcrRecord,
      expectedUsedOffset: UsedOffset,
      setLevel: Level,
      targetLevel: Option[Level],
      botProfile: Option[BotMatchProfile],
      latencyMillis: Int,
      abuseClear: Boolean,
      policyVersion: String,
      createdAt: Long
  ):
    def rated: Boolean = poolKey.queue == SearchQueue.NormalEvenChess

    def expectedEffectiveRating: Int = ecr.rating + expectedUsedOffset.value

    def isBotTicket: Boolean = botProfile.isDefined

    def valid: Boolean =
      ticketId.nonEmpty &&
        playerId.nonEmpty &&
        poolKey.valid &&
        requestedClock.forall(_.valid) &&
        ecr.hasRequiredFields &&
        ecr.playerId == playerId &&
        ecr.pool == poolKey.ecrPool &&
        expectedUsedOffset.nonNegative &&
        botProfile.forall(_.valid) &&
        latencyMillis >= 0 &&
        policyVersion.nonEmpty &&
        createdAt > 0 &&
        targetLevel.isDefined == (poolKey.queue == SearchQueue.TargetLevel) &&
        ecr.pool != EcrPool.NormalLichess

    def profile: MatchmakingProfile =
      MatchmakingProfile(
        playerId = playerId,
        ecr = ecr,
        expectedUsedOffset = expectedUsedOffset,
        timeControl = poolKey.timeControl,
        setLevel = setLevel,
        latencyMillis = latencyMillis,
        abuseClear = abuseClear
      )

    def withAssignedSetLevel(level: Level): SearchTicket =
      copy(
        setLevel = level,
        expectedUsedOffset = LevelOffsetTable.offsetForLevel(level)
      )

  final case class RequestedClock(limitSeconds: Int, incrementSeconds: Int):
    def valid: Boolean =
      limitSeconds >= 0 &&
        limitSeconds <= 10 * 60 * 60 &&
        incrementSeconds >= 0 &&
        incrementSeconds <= 180

  final case class LevelCompatibility(
      levelDelta: Int,
      allowedByStage: Boolean,
      requiresConfirmation: Boolean
  )

  object LevelCompatibility:
    def normal(a: SearchTicket, b: SearchTicket, window: SearchWindow): LevelCompatibility =
      val delta = math.abs(a.setLevel.value - b.setLevel.value)
      val allowedDelta = if window.allowLevelCompatibilityExpansion then 1 else 0
      LevelCompatibility(
        levelDelta = delta,
        allowedByStage = delta <= allowedDelta,
        requiresConfirmation = delta > 0 && window.requiresConfirmationForLevelContractChange
      )

    def target(a: SearchTicket, b: SearchTicket): LevelCompatibility =
      val delta = math.abs(a.targetLevel.map(_.value).getOrElse(-100) - b.targetLevel.map(_.value).getOrElse(100))
      LevelCompatibility(
        levelDelta = delta,
        allowedByStage = delta <= 1,
        requiresConfirmation = delta == 1
      )

  final case class PairingConfirmation(
      poolKey: PoolKey,
      rated: Boolean,
      whiteSetLevel: Level,
      blackSetLevel: Level,
      outsideHelpRule: String
  ):
    def valid: Boolean =
      poolKey.valid &&
        outsideHelpRule.nonEmpty &&
        outsideHelpRule.toLowerCase.contains("outside help")

  object PairingConfirmation:
    val outsideHelpRule =
      "Non-platform outside help is prohibited in rated EvenChess."

    def fromTickets(a: SearchTicket, b: SearchTicket): PairingConfirmation =
      PairingConfirmation(
        poolKey = a.poolKey,
        rated = a.rated && b.rated,
        whiteSetLevel = a.setLevel,
        blackSetLevel = b.setLevel,
        outsideHelpRule = outsideHelpRule
      )

  final case class PairingDecision(
      allowed: Boolean,
      requiresConfirmation: Boolean,
      reasons: List[String],
      stage: SearchStage,
      confirmation: Option[PairingConfirmation]
  )

  enum SearchPreferenceScenario:
    case NormalSearch
    case PreferredOwnSetLevel

    def label: String =
      this match
        case NormalSearch         => "Normal search"
        case PreferredOwnSetLevel => "Preferred set level search"

  final case class MatchPreferences(
      preferredOwnSetLevel: Option[Level]
  ):
    def scenario: SearchPreferenceScenario =
      if preferredOwnSetLevel.isDefined then SearchPreferenceScenario.PreferredOwnSetLevel
      else SearchPreferenceScenario.NormalSearch

    def valid: Boolean = true

  object MatchPreferences:
    val normal: MatchPreferences = MatchPreferences(None)

  final case class PreferenceMatchFlags(
      scenario: SearchPreferenceScenario,
      requesterPreferredLevelMatched: Boolean,
      candidatePreferredLevelMatched: Boolean,
      widenedSearch: Boolean,
      unevenMatch: Boolean,
      unevenReason: Option[String]
  ):
    def allFixedPreferencesMet: Boolean =
      requesterPreferredLevelMatched && candidatePreferredLevelMatched

    def visibleWideningSummary: String =
      if unevenMatch then "Uneven match accepted; rating adjustment accounts for the assistance levels used."
      else if widenedSearch then "Search window widened without relaxing level targets."
      else if scenario == SearchPreferenceScenario.PreferredOwnSetLevel then "Preferred set level applied."
      else "Initial search window matched."

    def valid: Boolean =
      scenario.label.nonEmpty &&
        unevenReason.forall(_.nonEmpty)

  object PreferenceMatchFlags:
    def fromTickets(
        white: SearchTicket,
        black: SearchTicket,
        requestPreferences: MatchPreferences,
        candidatePreferences: MatchPreferences,
        stage: SearchStage,
        unevenMatch: Boolean,
        unevenReason: Option[String]
    ): PreferenceMatchFlags =
      PreferenceMatchFlags(
        scenario =
          if requestPreferences.preferredOwnSetLevel.isDefined || candidatePreferences.preferredOwnSetLevel.isDefined
          then SearchPreferenceScenario.PreferredOwnSetLevel
          else SearchPreferenceScenario.NormalSearch,
        requesterPreferredLevelMatched =
          requestPreferences.preferredOwnSetLevel.forall(level => white.setLevel == level),
        candidatePreferredLevelMatched =
          candidatePreferences.preferredOwnSetLevel.forall(level => black.setLevel == level),
        widenedSearch = stage != SearchStage.Initial,
        unevenMatch = unevenMatch,
        unevenReason = unevenReason
      )

  final case class MatchQualityScore(
      value: Int,
      ecrDelta: Int,
      effectiveRatingDelta: Int,
      levelDelta: Int
  ):
    def valid: Boolean =
      value >= 0 &&
        value <= 1000 &&
        ecrDelta >= 0 &&
        effectiveRatingDelta >= 0 &&
        levelDelta >= 0

  object MatchQualityScore:
    def fromTickets(white: SearchTicket, black: SearchTicket): MatchQualityScore =
      val ecrDelta = math.abs(white.ecr.rating - black.ecr.rating)
      val effectiveDelta = math.abs(white.expectedEffectiveRating - black.expectedEffectiveRating)
      val levelDelta = math.abs(white.setLevel.value - black.setLevel.value)
      val penalty = ecrDelta / 2 + effectiveDelta + levelDelta * 5
      MatchQualityScore(
        value = math.max(0, 1000 - penalty),
        ecrDelta = ecrDelta,
        effectiveRatingDelta = effectiveDelta,
        levelDelta = levelDelta
      )

  final case class MatchContract(
      requestId: String,
      gameId: Option[String],
      timeControl: TimeControlBucket,
      rated: Boolean,
      whitePlayerId: String,
      blackPlayerId: String,
      whiteEcr: Int,
      blackEcr: Int,
      whiteMmr: Int,
      blackMmr: Int,
      whiteSetLevel: Level,
      blackSetLevel: Level,
      whiteExpectedOffset: UsedOffset,
      blackExpectedOffset: UsedOffset,
      whiteEffectiveRating: Int,
      blackEffectiveRating: Int,
      matchQuality: MatchQualityScore,
      preferenceFlags: PreferenceMatchFlags,
      tokenGateResult: String,
      policyVersion: String,
      stage: SearchStage
  ):
    def unevenMatch: Boolean = preferenceFlags.unevenMatch

    def ecrSeparatedFromLichess: Boolean =
      whiteEcr > 0 && blackEcr > 0

    def valid: Boolean =
      requestId.nonEmpty &&
        gameId.forall(_.nonEmpty) &&
        whitePlayerId.nonEmpty &&
        blackPlayerId.nonEmpty &&
        whitePlayerId != blackPlayerId &&
        whiteMmr == whiteEcr &&
        blackMmr == blackEcr &&
        whiteExpectedOffset.nonNegative &&
        blackExpectedOffset.nonNegative &&
        whiteEffectiveRating == whiteEcr + whiteExpectedOffset.value &&
        blackEffectiveRating == blackEcr + blackExpectedOffset.value &&
        matchQuality.valid &&
        preferenceFlags.valid &&
        tokenGateResult.nonEmpty &&
        policyVersion.nonEmpty &&
        ecrSeparatedFromLichess

  final case class MatchContractAuditRecord(
      contract: MatchContract,
      calibrationModelVersion: String,
      abuseSignalKeys: List[String],
      loggedAt: Long
  ):
    def valid: Boolean =
      contract.valid &&
        calibrationModelVersion.nonEmpty &&
        loggedAt > 0 &&
        abuseSignalKeys.distinct == abuseSignalKeys

  final case class MmrSimulationResult(
      requestId: String,
      contract: Option[MatchContract],
      evaluatedStages: List[SearchStage],
      visibleMessages: List[String],
      reasons: List[String]
  ):
    def matched: Boolean = contract.exists(_.valid)
    def valid: Boolean =
      requestId.nonEmpty &&
        evaluatedStages.nonEmpty &&
        visibleMessages.nonEmpty &&
        contract.forall(_.valid)

  object MmrEngine:
    val policyVersion = "evenchess-mmr-engine-v2-framework"
    val calibrationModelVersion = "expected-offset-calibration-v1"

    def contractFromTickets(
        requestId: String,
        white: SearchTicket,
        black: SearchTicket,
        stage: SearchStage,
        preferences: MatchPreferences,
        tokenGateResult: String,
        candidatePreferences: MatchPreferences = MatchPreferences.normal,
        unevenMatch: Boolean = false,
        unevenReason: Option[String] = None
    ): Either[String, MatchContract] =
      val flags = PreferenceMatchFlags.fromTickets(
        white,
        black,
        preferences,
        candidatePreferences,
        stage,
        unevenMatch,
        unevenReason
      )
      val pairing = PairingEngine.decide(white, black, stage, uiConfirmedLevelContract = true)
      val pairingBlocked =
        !pairing.allowed && !(unevenMatch && pairing.reasons.forall(_ == "effective_rating_window"))
      val reasons =
        (
          List(
            Option.when(!white.valid || !black.valid)("invalid_ticket"),
            Option.when(white.poolKey != black.poolKey)("pool_key_mismatch")
          ).flatten ++
            Option.when(pairingBlocked)(pairing.reasons.mkString(", "))
        ).filter(_.nonEmpty)

      if reasons.nonEmpty then Left(reasons.distinct.mkString(", "))
      else
        val contract = MatchContract(
          requestId = requestId,
          gameId = None,
          timeControl = white.poolKey.timeControl,
          rated = white.rated && black.rated,
          whitePlayerId = white.playerId,
          blackPlayerId = black.playerId,
          whiteEcr = white.ecr.rating,
          blackEcr = black.ecr.rating,
          whiteMmr = white.ecr.rating,
          blackMmr = black.ecr.rating,
          whiteSetLevel = white.setLevel,
          blackSetLevel = black.setLevel,
          whiteExpectedOffset = white.expectedUsedOffset,
          blackExpectedOffset = black.expectedUsedOffset,
          whiteEffectiveRating = white.expectedEffectiveRating,
          blackEffectiveRating = black.expectedEffectiveRating,
          matchQuality = MatchQualityScore.fromTickets(white, black),
          preferenceFlags = flags,
          tokenGateResult = tokenGateResult,
          policyVersion = policyVersion,
          stage = stage
        )
        Either.cond(contract.valid, contract, "invalid_match_contract")

    def auditRecord(contract: MatchContract, abuseSignalKeys: List[String], loggedAt: Long): MatchContractAuditRecord =
      MatchContractAuditRecord(
        contract = contract,
        calibrationModelVersion = calibrationModelVersion,
        abuseSignalKeys = abuseSignalKeys,
        loggedAt = loggedAt
      )

    def simulate(
        requestId: String,
        request: SearchTicket,
        candidates: List[SearchTicket],
        preferences: MatchPreferences,
        tokenGateResult: String,
        candidatePreferences: Map[String, MatchPreferences] = Map.empty
    ): MmrSimulationResult =
      val stages = SearchWideningPlan.orderedStages

      final case class AssignedCandidate(
          requestTicket: SearchTicket,
          candidateTicket: SearchTicket,
          candidatePreferences: MatchPreferences,
          unevenMatch: Boolean,
          unevenReason: Option[String]
      ):
        def effectiveRatingDelta: Int =
          math.abs(requestTicket.expectedEffectiveRating - candidateTicket.expectedEffectiveRating)

      def fixedLevelFor(ticket: SearchTicket, prefs: MatchPreferences): Option[Level] =
        prefs.preferredOwnSetLevel.orElse(Option.when(ticket.poolKey.queue == SearchQueue.TargetLevel)(ticket.setLevel))

      def baseLowerLevel(a: SearchTicket, b: SearchTicket): Option[(String, Level)] =
        if a.ecr.rating <= b.ecr.rating then Some(a.playerId -> BaseSetLevelByRatingTable.levelForRating(a.ecr.rating))
        else Some(b.playerId -> BaseSetLevelByRatingTable.levelForRating(b.ecr.rating))

      def levelOptions(
          ticket: SearchTicket,
          prefs: MatchPreferences,
          request: SearchTicket,
          candidate: SearchTicket
      ): List[Level] =
        fixedLevelFor(ticket, prefs)
          .map(List(_))
          .getOrElse:
            baseLowerLevel(request, candidate) match
              case Some((playerId, level)) if ticket.playerId == playerId => List(level)
              case _ => (Level.min to Level.max).map(Level(_)).toList

      def assignLevels(candidate: SearchTicket): Option[AssignedCandidate] =
        val candidatePrefs = candidatePreferences.getOrElse(candidate.ticketId, MatchPreferences.normal)
        val requestLevels = levelOptions(request, preferences, request, candidate)
        val candidateLevels = levelOptions(candidate, candidatePrefs, request, candidate)
        val assignments =
          for
            requestLevel <- requestLevels
            candidateLevel <- candidateLevels
            assignedRequest = request.withAssignedSetLevel(requestLevel)
            assignedCandidate = candidate.withAssignedSetLevel(candidateLevel)
          yield assignedRequest -> assignedCandidate

        assignments
          .sortBy { case (assignedRequest, assignedCandidate) =>
            (
              math.abs(assignedRequest.expectedEffectiveRating - assignedCandidate.expectedEffectiveRating),
              math.abs(assignedRequest.ecr.rating - assignedCandidate.ecr.rating),
              math.abs(assignedRequest.setLevel.value - assignedCandidate.setLevel.value)
            )
          }
          .headOption
          .map { case (assignedRequest, assignedCandidate) =>
            val bothFixed =
              preferences.preferredOwnSetLevel.isDefined && candidatePrefs.preferredOwnSetLevel.isDefined
            val effectiveDelta =
              math.abs(assignedRequest.expectedEffectiveRating - assignedCandidate.expectedEffectiveRating)
            val uneven = bothFixed && effectiveDelta > SearchWindows.byStage(SearchStage.Widening3).maxEffectiveRatingDelta
            AssignedCandidate(
              requestTicket = assignedRequest,
              candidateTicket = assignedCandidate,
              candidatePreferences = candidatePrefs,
              unevenMatch = uneven,
              unevenReason = Option.when(uneven)(
                "both_players_fixed_preferred_set_levels_outside_effective_rating_window"
              )
            )
          }

      def contractsForStage(stage: SearchStage): List[MatchContract] =
        for
          candidate <- candidates
          assignment <- assignLevels(candidate).toList
          pairing = PairingEngine.decide(assignment.requestTicket, assignment.candidateTicket, stage, uiConfirmedLevelContract = true)
          if pairing.allowed || (
            assignment.unevenMatch && pairing.reasons.forall(_ == "effective_rating_window")
          )
          contract <- contractFromTickets(
            requestId,
            assignment.requestTicket,
            assignment.candidateTicket,
            stage,
            preferences,
            tokenGateResult,
            candidatePreferences = assignment.candidatePreferences,
            unevenMatch = assignment.unevenMatch,
            unevenReason = assignment.unevenReason
          ).toOption
        yield contract

      def bestContract(contracts: List[MatchContract]): Option[MatchContract] =
        contracts.sortBy(contract =>
          (
            contract.preferenceFlags.unevenMatch,
            -contract.matchQuality.value,
            contract.matchQuality.effectiveRatingDelta,
            contract.matchQuality.ecrDelta,
            contract.matchQuality.levelDelta
          )
        ).headOption

      val contract =
        stages.view
          .map(stage => bestContract(contractsForStage(stage)))
          .collectFirst { case Some(contract) => contract }

      val messages =
        contract
          .map(c => List(c.preferenceFlags.visibleWideningSummary))
          .getOrElse(List("No simulated match contract found before cancellation or further waiting."))

      MmrSimulationResult(
        requestId = requestId,
        contract = contract,
        evaluatedStages = stages,
        visibleMessages = messages,
        reasons = if contract.isDefined then Nil else List("no_candidate_contract")
      )

  object PairingEngine:
    val maxLatencyMillis = 250

    def decide(
        a: SearchTicket,
        b: SearchTicket,
        stage: SearchStage,
        uiConfirmedLevelContract: Boolean
    ): PairingDecision =
      val window = SearchWindows.byStage(stage)
      val baseReasons = List(
        Option.when(!a.valid || !b.valid)("invalid_ticket"),
        Option.when(a.playerId == b.playerId)("same_player"),
        Option.when(a.poolKey != b.poolKey)("pool_key_mismatch"),
        Option.when(a.poolKey.queue == SearchQueue.TargetLevel && b.poolKey.queue != SearchQueue.TargetLevel)(
          "target_queue_mismatch"
        ),
        Option.when(a.poolKey.queue != SearchQueue.TargetLevel && b.poolKey.queue == SearchQueue.TargetLevel)(
          "target_queue_mismatch"
        )
      ).flatten

      val ecrDecision =
        if a.poolKey.queue == SearchQueue.TargetLevel then
          EcrRating.Matchmaking.decide(a.profile, b.profile, window, maxLatencyMillis)
        else
          val reasons = List(
            Option.when(a.ecr.pool != b.ecr.pool)("pool_mismatch"),
            Option.when(a.poolKey.timeControl != b.poolKey.timeControl)("time_control_mismatch"),
            Option.when(!a.abuseClear || !b.abuseClear)("abuse_controls"),
            Option.when(a.latencyMillis > maxLatencyMillis || b.latencyMillis > maxLatencyMillis)("latency"),
            Option.when(math.abs(a.expectedEffectiveRating - b.expectedEffectiveRating) > window.maxEffectiveRatingDelta)(
              "effective_rating_window"
            )
          ).flatten
          EcrRating.MatchmakingDecision(
            allowed = reasons.isEmpty,
            requiresConfirmation = false,
            reasons = reasons
          )
      val levelCompatibility =
        if a.poolKey.queue == SearchQueue.TargetLevel then LevelCompatibility.target(a, b)
        else LevelCompatibility(
          levelDelta = math.abs(a.setLevel.value - b.setLevel.value),
          allowedByStage = true,
          requiresConfirmation = false
        )
      val levelReasons =
        Option.when(!levelCompatibility.allowedByStage)("level_contract").toList
      val reasons = (baseReasons ++ ecrDecision.reasons.filterNot(_ == "level_contract") ++ levelReasons).distinct
      val requiresConfirmation =
        reasons.isEmpty && levelCompatibility.requiresConfirmation && !uiConfirmedLevelContract
      val allowed = reasons.isEmpty && !requiresConfirmation

      PairingDecision(
        allowed = allowed,
        requiresConfirmation = requiresConfirmation,
        reasons = reasons,
        stage = stage,
        confirmation = Option.when(allowed || requiresConfirmation)(PairingConfirmation.fromTickets(a, b))
      )

  object SearchWideningPlan:
    val orderedStages: List[SearchStage] =
      List(SearchStage.Initial, SearchStage.Widening1, SearchStage.Widening2, SearchStage.Widening3)

    def next(stage: SearchStage): Option[SearchStage] =
      val index = orderedStages.indexOf(stage)
      Option.when(index >= 0 && index < orderedStages.size - 1)(orderedStages(index + 1))

    def widensInRequiredOrder: Boolean =
      val initial = SearchWindows.byStage(SearchStage.Initial)
      val w1 = SearchWindows.byStage(SearchStage.Widening1)
      val w2 = SearchWindows.byStage(SearchStage.Widening2)
      val w3 = SearchWindows.byStage(SearchStage.Widening3)

      w1.maxEcrDelta > initial.maxEcrDelta &&
        w1.maxEffectiveRatingDelta == initial.maxEffectiveRatingDelta &&
        w2.maxEffectiveRatingDelta > w1.maxEffectiveRatingDelta &&
        w3.allowLevelCompatibilityExpansion &&
        w3.requiresConfirmationForLevelContractChange

  final case class SearchTelemetryContext(
      schemaVersion: String,
      occurredAt: Long,
      pseudonymousUserId: String
  ):
    def valid: Boolean =
      schemaVersion.nonEmpty && occurredAt > 0 && pseudonymousUserId.nonEmpty

  object SearchTelemetry:
    def searchStarted(ticket: SearchTicket, context: SearchTelemetryContext): TelemetryEvent =
      TelemetryEvent(
        eventId = s"match.search_started.${ticket.ticketId}",
        family = EventFamily.MatchLifecycle,
        name = TelemetryEventName.MatchSearchStarted,
        authority = EventAuthority.Server,
        schemaVersion = context.schemaVersion,
        occurredAt = context.occurredAt,
        pseudonymousUserId = Some(context.pseudonymousUserId),
        gameId = None,
        rated = ticket.rated,
        versions = VersionSet(
          schemaVersion = context.schemaVersion,
          policyVersion = Some(ticket.policyVersion),
          modelVersion = Some(ticket.ecr.matchmakingPolicyVersion),
          configVersion = Some(ticket.poolKey.key)
        ),
        dedupeId = Some(ticket.ticketId)
      )

    def gameStarted(gameId: String, a: SearchTicket, b: SearchTicket, context: SearchTelemetryContext): TelemetryEvent =
      TelemetryEvent(
        eventId = s"game.started.$gameId",
        family = EventFamily.MatchLifecycle,
        name = TelemetryEventName.GameStarted,
        authority = EventAuthority.Server,
        schemaVersion = context.schemaVersion,
        occurredAt = context.occurredAt,
        pseudonymousUserId = None,
        gameId = Some(gameId),
        rated = a.rated && b.rated,
        versions = VersionSet(
          schemaVersion = context.schemaVersion,
          policyVersion = Some(a.policyVersion),
          modelVersion = Some(a.ecr.matchmakingPolicyVersion),
          configVersion = Some(a.poolKey.key)
        ),
        dedupeId = Some(gameId)
      )

  final case class RatingApplication(
      gameId: String,
      result: EcrGameResult,
      mode: RatedMode,
      poolKey: PoolKey,
      kFactor: Int,
      updatedAt: Long,
      playerUsedLevel: Option[Level] = None,
      opponentUsedLevel: Option[Level] = None
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        result.auditable &&
        poolKey.valid &&
        kFactor > 0 &&
        updatedAt >= result.player.updatedAt &&
        result.player.pool == poolKey.ecrPool &&
        result.opponent.pool == poolKey.ecrPool &&
        result.player.pool != EcrPool.NormalLichess

  final case class RatingApplicationDecision(
      normalEcrChanged: Boolean,
      normalLichessRatingChanged: Boolean,
      updatedRecord: Option[EcrRecord],
      telemetry: Option[TelemetryEvent],
      reasons: List[String]
  )

  object RatingApplicationService:
    def apply(
        application: RatingApplication,
        context: SearchTelemetryContext
    ): RatingApplicationDecision =
      val reasons = List(
        Option.when(!application.valid)("invalid_rating_application"),
        Option.when(!EcrUpdateEligibility.updatesNormalEcr(application.mode))("mode_does_not_update_normal_ecr"),
        Option.when(application.poolKey.queue != SearchQueue.NormalEvenChess)("queue_does_not_update_normal_ecr"),
        Option.when(application.poolKey.ecrPool == EcrPool.NormalLichess)("normal_lichess_rating_excluded")
      ).flatten

      if reasons.nonEmpty then
        RatingApplicationDecision(
          normalEcrChanged = false,
          normalLichessRatingChanged = false,
          updatedRecord = None,
          telemetry = None,
          reasons = reasons.distinct
        )
      else
        EvenChessRatingCalibration.GameHistory.record(
          EvenChessRatingCalibration.GameCalibrationSample.fromRatingResult(
            gameId = application.gameId,
            result = application.result,
            sideALevel = application.playerUsedLevel,
            sideBLevel = application.opponentUsedLevel,
            playedAt = application.updatedAt
          )
        )
        val updated = RatingReplay.applySimpleUpdate(application.result, application.kFactor, application.updatedAt)
        val telemetry = TelemetryEvent(
          eventId = s"rating.applied.${application.gameId}.${updated.playerId}",
          family = EventFamily.Rating,
          name = TelemetryEventName.RatingApplied,
          authority = EventAuthority.Server,
          schemaVersion = context.schemaVersion,
          occurredAt = context.occurredAt,
          pseudonymousUserId = Some(context.pseudonymousUserId),
          gameId = Some(application.gameId),
          rated = true,
          versions = VersionSet(
            schemaVersion = context.schemaVersion,
            policyVersion = Some(updated.ratingPolicyVersion),
            modelVersion = Some(application.result.modelVersion),
            configVersion = Some(application.poolKey.key)
          ),
          dedupeId = Some(s"${application.gameId}.${updated.playerId}")
        )

        RatingApplicationDecision(
          normalEcrChanged = true,
          normalLichessRatingChanged = false,
          updatedRecord = Some(updated),
          telemetry = Some(telemetry),
          reasons = Nil
        )
