package lila.evenchess

import java.nio.charset.StandardCharsets
import java.util.Base64

import AssistanceAccounting.UsedOffset
import CoachingLadder.Level

object EvenChessRatingCalibration:

  val maxStoredGameSamples = 1_000_000
  val defaultPolicyVersion = "ecor-table-v1"
  val calibrationModelVersion = "ecor-calibration-v1"

  final case class EcorLevelGap(
      from: Level,
      to: Level,
      ratingPoints: Int
  ):
    def valid: Boolean =
      to.value == from.value + 1 &&
        from.value >= Level.min &&
        to.value <= Level.max &&
        ratingPoints >= 0 &&
        ratingPoints <= 500

    def key: String = s"L${from.value}-L${to.value}"
    def line: String = s"$key=$ratingPoints"

  final case class RatingLevelBand(
      minInclusive: Option[Int],
      maxInclusive: Option[Int],
      level: Level
  ):
    def contains(rating: Int): Boolean =
      minInclusive.forall(rating >= _) && maxInclusive.forall(rating <= _)

    def valid: Boolean =
      Level.isValid(level.value) &&
        minInclusive.forall(_ >= 0) &&
        maxInclusive.forall(_ >= 0) &&
        ((minInclusive, maxInclusive) match
          case (Some(min), Some(max)) => min <= max
          case _                      => true)

    def label: String =
      (minInclusive, maxInclusive) match
        case (None, Some(max))      => s"<=$max"
        case (Some(min), None)      => s">=$min"
        case (Some(min), Some(max)) => s"$min-$max"
        case (None, None)           => "any"

    def line: String = s"$label=L${level.value}"

  final case class EcorTableConfig(
      version: String,
      gaps: List[EcorLevelGap],
      ratingBands: List[RatingLevelBand]
  ):
    lazy val orderedGaps: List[EcorLevelGap] = gaps.sortBy(_.from.value)
    lazy val orderedBands: List[RatingLevelBand] =
      ratingBands.sortBy(band => band.minInclusive.getOrElse(Int.MinValue))

    lazy val cumulativeOffsets: Map[Int, Int] =
      val values =
        orderedGaps.foldLeft(List(Level.min -> 0)): (acc, gap) =>
          val previous = acc.last._2
          acc :+ (gap.to.value -> (previous + gap.ratingPoints))
      values.toMap

    def offsetValueForLevel(level: Level): Int =
      cumulativeOffsets.getOrElse(level.value, 0)

    def offsetForLevel(level: Level): UsedOffset =
      UsedOffset(offsetValueForLevel(level), version)

    def nearestLevelForOffset(offset: Int): Level =
      val closest = cumulativeOffsets.toList.minBy { case (_, value) => math.abs(value - offset) }
      Level(closest._1)

    def levelForRating(rating: Int): Level =
      orderedBands.find(_.contains(rating)).map(_.level).getOrElse:
        if rating < orderedBands.flatMap(_.minInclusive).minOption.getOrElse(0) then Level(Level.min)
        else orderedBands.lastOption.map(_.level).getOrElse(Level(Level.max))

    def bandForLevel(level: Level): Option[RatingLevelBand] =
      orderedBands.find(_.level == level)

    def ratingForBandCenter(level: Level): Int =
      bandForLevel(level) match
        case None => 1200
        case Some(band) =>
          (band.minInclusive, band.maxInclusive) match
            case (Some(min), Some(max)) => min + ((max - min) / 2)
            case (Some(min), None)      => min + 250
            case (None, Some(max))      => Math.max(0, max - 50)
            case (None, None)           => 1200

    def gapText: String =
      orderedGaps.map(_.line).mkString("\n")

    def ratingBandsText: String =
      orderedBands.map(_.line).mkString("\n")

    def valid: Boolean =
      version.nonEmpty &&
        orderedGaps.length == Level.max &&
        orderedGaps.forall(_.valid) &&
        orderedGaps.map(_.from.value) == (Level.min until Level.max).toList &&
        orderedGaps.map(_.to.value) == ((Level.min + 1) to Level.max).toList &&
        cumulativeOffsets.get(Level.min).contains(0) &&
        cumulativeOffsets.size == Level.max + 1 &&
        orderedBands.length == Level.max + 1 &&
        orderedBands.forall(_.valid) &&
        orderedBands.map(_.level.value).toSet == (Level.min to Level.max).toSet

  object EcorTableConfig:
    val default: EcorTableConfig =
      EcorTableConfig(
        version = defaultPolicyVersion,
        gaps = EcorDefaults.gaps,
        ratingBands = EcorDefaults.ratingBands
      )

    def fromText(
        version: String,
        gapText: String,
        ratingBandsText: String
    ): Either[String, EcorTableConfig] =
      for
        gaps <- EcorParsers.parseGaps(gapText)
        bands <- EcorParsers.parseRatingBands(ratingBandsText)
        config = EcorTableConfig(version.trim.filterNot(_.isWhitespace), gaps, bands)
        valid <- Either.cond(config.valid, config, "ECOR config must include ten ordered gaps and eleven rating bands.")
      yield valid

  object EcorDefaults:
    val gaps: List[EcorLevelGap] =
      List(5, 5, 8, 10, 17, 20, 25, 30, 35, 35).zipWithIndex.map: (value, from) =>
        EcorLevelGap(Level(from), Level(from + 1), value)

    val ratingBands: List[RatingLevelBand] =
      List(
        RatingLevelBand(None, Some(674), Level(0)),
        RatingLevelBand(Some(675), Some(799), Level(1)),
        RatingLevelBand(Some(800), Some(924), Level(2)),
        RatingLevelBand(Some(925), Some(1049), Level(3)),
        RatingLevelBand(Some(1050), Some(1179), Level(4)),
        RatingLevelBand(Some(1180), Some(1324), Level(5)),
        RatingLevelBand(Some(1325), Some(1499), Level(6)),
        RatingLevelBand(Some(1500), Some(1699), Level(7)),
        RatingLevelBand(Some(1700), Some(1899), Level(8)),
        RatingLevelBand(Some(1900), Some(2099), Level(9)),
        RatingLevelBand(Some(2100), None, Level(10))
      )

    val gapText: String = gaps.map(_.line).mkString("\n")
    val ratingBandsText: String = ratingBands.map(_.line).mkString("\n")
    val historyText: String = ""

  object EcorParsers:
    private val gapPattern =
      """(?i)^\s*L?(\d+)\s*(?:-|->|to)\s*L?(\d+)\s*[:=]\s*(-?\d+)\s*$""".r
    private val levelPattern = """(?i)^\s*L?(\d+)\s*$""".r
    private val ltPattern = """^\s*<\s*(\d+)\s*$""".r
    private val ltePattern = """^\s*<=\s*(\d+)\s*$""".r
    private val gtPattern = """^\s*>\s*(\d+)\s*$""".r
    private val gtePattern = """^\s*>=\s*(\d+)\s*$""".r
    private val plusPattern = """^\s*(\d+)\s*\+\s*$""".r
    private val rangePattern = """^\s*(\d+)\s*-\s*(\d+)\s*$""".r

    def parseGaps(text: String): Either[String, List[EcorLevelGap]] =
      val lines = splitLines(text)
      val parsed = lines.map:
        case gapPattern(from, to, value) =>
          for
            f <- parseLevelNumber(from)
            t <- parseLevelNumber(to)
            rating <- value.toIntOption.toRight(s"Invalid gap value: $value")
          yield EcorLevelGap(Level(f), Level(t), rating)
        case other => Left(s"Invalid ECOR gap row: $other")
      sequence(parsed).flatMap: gaps =>
        val ordered = gaps.sortBy(_.from.value)
        Either.cond(
          ordered.length == 10 && ordered.forall(_.valid) && ordered.map(_.from.value) == (0 until 10).toList,
          ordered,
          "ECOR gaps must be non-negative rows from L0-L1 through L9-L10."
        )

    def parseRatingBands(text: String): Either[String, List[RatingLevelBand]] =
      val lines = splitLines(text)
      val parsed = lines.map: line =>
        splitBandLine(line) match
          case Some((range, level)) =>
            for
              parsedRange <- parseRange(range)
              parsedLevel <- parseLevelLabel(level)
            yield RatingLevelBand(parsedRange._1, parsedRange._2, Level(parsedLevel))
          case None => Left(s"Invalid rating band row: $line")
      sequence(parsed).flatMap: bands =>
        val ordered = bands.sortBy(_.minInclusive.getOrElse(Int.MinValue))
        Either.cond(
          ordered.length == 11 &&
            ordered.forall(_.valid) &&
            ordered.map(_.level.value).toSet == (0 to 10).toSet,
          ordered,
          "Rating-to-level table must include exactly one valid band for each L0-L10 level."
        )

    def splitLinesForTables(text: String): List[String] =
      text
        .split("[\\n;]+")
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)

    private def splitLines(text: String): List[String] =
      splitLinesForTables(text)

    private def splitBandLine(line: String): Option[(String, String)] =
      val equalsIndex = line.lastIndexOf('=')
      val colonIndex = line.lastIndexOf(':')
      val index = math.max(equalsIndex, colonIndex)
      Option.when(index > 0 && index < line.length - 1)(
        line.take(index).trim -> line.drop(index + 1).trim
      )

    private def parseLevelNumber(raw: String): Either[String, Int] =
      raw.toIntOption.filter(Level.isValid).toRight(s"Invalid level: $raw")

    private def parseLevelLabel(raw: String): Either[String, Int] =
      raw.trim match
        case levelPattern(level) => parseLevelNumber(level)
        case _                   => Left(s"Invalid level label: $raw")

    def parseRangeForTables(raw: String): Either[String, (Option[Int], Option[Int])] =
      raw.trim match
        case ltPattern(max)    => max.toIntOption.map(value => None -> Some(value - 1)).toRight(s"Invalid range: $raw")
        case ltePattern(max)   => max.toIntOption.map(value => None -> Some(value)).toRight(s"Invalid range: $raw")
        case gtPattern(min)    => min.toIntOption.map(value => Some(value + 1) -> None).toRight(s"Invalid range: $raw")
        case gtePattern(min)   => min.toIntOption.map(value => Some(value) -> None).toRight(s"Invalid range: $raw")
        case plusPattern(min)  => min.toIntOption.map(value => Some(value) -> None).toRight(s"Invalid range: $raw")
        case rangePattern(a, b) =>
          for
            min <- a.toIntOption.toRight(s"Invalid range minimum: $raw")
            max <- b.toIntOption.toRight(s"Invalid range maximum: $raw")
          yield Some(min) -> Some(max)
        case _ => Left(s"Invalid rating range: $raw")

    private def parseRange(raw: String): Either[String, (Option[Int], Option[Int])] =
      parseRangeForTables(raw)

    def sequenceForTables[A](values: List[Either[String, A]]): Either[String, List[A]] =
      values.foldRight(Right(Nil): Either[String, List[A]]): (value, acc) =>
        for
          item <- value
          list <- acc
        yield item :: list

    private def sequence[A](values: List[Either[String, A]]): Either[String, List[A]] =
      sequenceForTables(values)

  object EcorRuntime:
    @volatile private var current: EcorTableConfig = EcorTableConfig.default

    def active: EcorTableConfig = current

    def activate(config: EcorTableConfig): Either[String, EcorTableConfig] =
      Either.cond(config.valid, config, "Cannot activate invalid ECOR table.").map: valid =>
        current = valid
        valid

    def activateFromText(version: String, gapText: String, ratingBandsText: String): Either[String, EcorTableConfig] =
      EcorTableConfig.fromText(version, gapText, ratingBandsText).flatMap(activate)

  final case class StockfishAiLevelBand(
      level: Int,
      minInclusive: Int,
      maxInclusive: Int
  ):
    def contains(rating: Int): Boolean =
      rating >= minInclusive && rating <= maxInclusive

    def valid: Boolean =
      level >= 1 &&
        level <= 8 &&
        minInclusive >= 0 &&
        maxInclusive >= minInclusive

    def label: String =
      if maxInclusive >= 100000 then s"${minInclusive}+" else s"$minInclusive-$maxInclusive"

    def line: String = s"SF$level=$label"

  final case class StockfishAiRatingTableConfig(
      bands: List[StockfishAiLevelBand]
  ):
    lazy val orderedBands: List[StockfishAiLevelBand] = bands.sortBy(_.level)

    def levelForRating(rating: Int): Level =
      Level(
        orderedBands.find(_.contains(rating))
          .map(_.level)
          .getOrElse:
            if rating <= orderedBands.headOption.map(_.maxInclusive).getOrElse(0) then 1 else 8
      )

    def windowFor(level: Int): StockfishAiLevelBand =
      orderedBands.find(_.level == math.max(1, math.min(8, level))).getOrElse(orderedBands.last)

    def windowText(level: Int): String =
      windowFor(level).label

    def tableText: String =
      orderedBands.map(_.line).mkString("\n")

    def valid: Boolean =
      orderedBands.length == 8 &&
        orderedBands.forall(_.valid) &&
        orderedBands.map(_.level) == (1 to 8).toList &&
        orderedBands.sliding(2).forall:
          case List(previous, next) => next.minInclusive == previous.maxInclusive + 1
          case _                    => true

  object StockfishAiRatingTableConfig:
    val default: StockfishAiRatingTableConfig =
      StockfishAiRatingTableConfig(StockfishAiRatingDefaults.bands)

    def fromText(text: String): Either[String, StockfishAiRatingTableConfig] =
      val rows = EcorParsers.splitLinesForTables(text)
      val parsed = rows.map:
        case StockfishAiRatingDefaults.rowPattern(level, range) =>
          for
            parsedLevel <- level.toIntOption.filter(value => value >= 1 && value <= 8).toRight(s"Invalid Stockfish level: $level")
            parsedRange <- EcorParsers.parseRangeForTables(range)
          yield StockfishAiLevelBand(parsedLevel, parsedRange._1.getOrElse(0), parsedRange._2.getOrElse(100000))
        case row => Left(s"Invalid Stockfish rating band row: $row")
      EcorParsers.sequenceForTables(parsed).flatMap: bands =>
        val config = StockfishAiRatingTableConfig(bands)
        Either.cond(config.valid, config, "Stockfish equivalent rating table must include contiguous SF1-SF8 bands.")

  object StockfishAiRatingDefaults:
    val bands: List[StockfishAiLevelBand] = List(
      StockfishAiLevelBand(1, 400, 799),
      StockfishAiLevelBand(2, 800, 999),
      StockfishAiLevelBand(3, 1000, 1199),
      StockfishAiLevelBand(4, 1200, 1399),
      StockfishAiLevelBand(5, 1400, 1599),
      StockfishAiLevelBand(6, 1600, 1799),
      StockfishAiLevelBand(7, 1800, 1999),
      StockfishAiLevelBand(8, 2000, 100000)
    )
    val tableText: String = bands.map(_.line).mkString("\n")
    val rowPattern = """(?i)^\s*SF?(\d+)\s*[:=]\s*(.+?)\s*$""".r

  object StockfishAiRatingRuntime:
    @volatile private var current: StockfishAiRatingTableConfig = StockfishAiRatingTableConfig.default

    def active: StockfishAiRatingTableConfig = current

    def activate(config: StockfishAiRatingTableConfig): Either[String, StockfishAiRatingTableConfig] =
      Either.cond(config.valid, config, "Cannot activate invalid Stockfish equivalent rating table.").map: valid =>
        current = valid
        valid

    def activateFromText(text: String): Either[String, StockfishAiRatingTableConfig] =
      StockfishAiRatingTableConfig.fromText(text).flatMap(activate)

  final case class EcorSnapshot(
      timestampMillis: Long,
      adminId: String,
      reason: String,
      version: String,
      gapText: String,
      ratingBandsText: String
  ):
    def valid: Boolean =
      timestampMillis > 0 &&
        adminId.nonEmpty &&
        version.nonEmpty &&
        EcorTableConfig.fromText(version, gapText, ratingBandsText).isRight

    def label: String =
      s"$timestampMillis - $version - ${reason.take(80)}"

    def line: String =
      List(
        timestampMillis.toString,
        encode(adminId),
        encode(reason),
        encode(version),
        encode(gapText),
        encode(ratingBandsText)
      ).mkString("|")

  object EcorSnapshot:
    def parse(line: String): Option[EcorSnapshot] =
      line.split("\\|", -1).toList match
        case timestamp :: admin :: reason :: version :: gaps :: bands :: Nil =>
          timestamp.toLongOption.map: millis =>
            EcorSnapshot(millis, decode(admin), decode(reason), decode(version), decode(gaps), decode(bands))
        case _ => None

  object EcorHistory:
    val maxSnapshots = 30

    def parse(text: String): List[EcorSnapshot] =
      text
        .split("\\n")
        .toList
        .flatMap(line => EcorSnapshot.parse(line.trim))
        .filter(_.valid)
        .sortBy(-_.timestampMillis)

    def append(text: String, snapshot: EcorSnapshot): String =
      (snapshot :: parse(text).filterNot(_.timestampMillis == snapshot.timestampMillis))
        .sortBy(-_.timestampMillis)
        .take(maxSnapshots)
        .map(_.line)
        .mkString("\n")

    def find(text: String, timestampMillis: Long): Option[EcorSnapshot] =
      parse(text).find(_.timestampMillis == timestampMillis)

  final case class GameCalibrationSample(
      gameId: String,
      sideARating: Int,
      sideBRating: Int,
      sideALevel: Level,
      sideBLevel: Level,
      sideAScore: Double,
      playedAt: Long
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        sideARating > 0 &&
        sideBRating > 0 &&
        Level.isValid(sideALevel.value) &&
        Level.isValid(sideBLevel.value) &&
        sideAScore >= 0.0 &&
        sideAScore <= 1.0 &&
        playedAt > 0

    def informative: Boolean = valid && sideALevel != sideBLevel

    def featureVector: Vector[Double] =
      (0 until Level.max).toVector.map: gap =>
        val a = if sideALevel.value > gap then 1.0 else 0.0
        val b = if sideBLevel.value > gap then 1.0 else 0.0
        a - b

  object GameCalibrationSample:
    def fromRatingResult(
        gameId: String,
        result: EcrRating.EcrGameResult,
        sideALevel: Option[Level],
        sideBLevel: Option[Level],
        playedAt: Long,
        table: EcorTableConfig = EcorRuntime.active
    ): GameCalibrationSample =
      GameCalibrationSample(
        gameId = gameId,
        sideARating = result.player.rating,
        sideBRating = result.opponent.rating,
        sideALevel = sideALevel.getOrElse(table.nearestLevelForOffset(result.playerUsedOffset.value)),
        sideBLevel = sideBLevel.getOrElse(table.nearestLevelForOffset(result.opponentUsedOffset.value)),
        sideAScore = result.score,
        playedAt = playedAt
      )

  object GameHistory:
    private var samples = Vector.empty[GameCalibrationSample]

    def record(sample: GameCalibrationSample): Unit =
      if sample.valid then synchronized:
        samples =
          (samples.filterNot(_.gameId == sample.gameId) :+ sample)
            .takeRight(maxStoredGameSamples)

    def latest(limit: Int = maxStoredGameSamples): List[GameCalibrationSample] =
      synchronized:
        samples.takeRight(limit.max(0).min(maxStoredGameSamples)).toList

    def clear(): Unit =
      synchronized:
        samples = Vector.empty

    def size: Int = synchronized(samples.size)

  final case class EcorGapEstimate(
      gap: EcorLevelGap,
      currentRatingPoints: Int,
      calculatedRatingPoints: Int,
      delta: Int,
      supportSamples: Int
  ):
    def valid: Boolean =
      gap.valid &&
        currentRatingPoints >= 0 &&
        calculatedRatingPoints >= 0 &&
        supportSamples >= 0

  final case class CalibrationRun(
      generatedAt: Long,
      modelVersion: String,
      sampleCount: Int,
      informativeSampleCount: Int,
      residualMeanRatingPoints: Double,
      residualStdDevRatingPoints: Double,
      meanAbsoluteResidualRatingPoints: Double,
      currentTable: EcorTableConfig,
      estimates: List[EcorGapEstimate]
  ):
    def valid: Boolean =
      generatedAt > 0 &&
        modelVersion.nonEmpty &&
        sampleCount >= informativeSampleCount &&
        informativeSampleCount >= 0 &&
        currentTable.valid &&
        estimates.length == 10 &&
        estimates.forall(_.valid)

    def calculatedGapText: String =
      estimates.map(estimate => estimate.gap.copy(ratingPoints = estimate.calculatedRatingPoints).line).mkString("\n")

    def summary: String =
      s"$informativeSampleCount/$sampleCount informative samples; residual sd ${formatDouble(residualStdDevRatingPoints)} rating points; MAE ${formatDouble(meanAbsoluteResidualRatingPoints)}."

  object CalibrationRuntime:
    @volatile private var previousRun: Option[CalibrationRun] = None

    def lastRun: Option[CalibrationRun] = previousRun

    def record(run: CalibrationRun): CalibrationRun =
      previousRun = Some(run)
      run

    def clear(): Unit =
      previousRun = None

  object CalibrationEngine:
    private final case class Bucket(
        ratingDiff: Int,
        sideALevel: Level,
        sideBLevel: Level,
        averageScore: Double,
        count: Int
    ):
      def featureVector: Vector[Double] =
        GameCalibrationSample("bucket", 1, 1, sideALevel, sideBLevel, averageScore, 1).featureVector

      def targetOffsetDiff: Double =
        scoreToEloDiff(averageScore) - ratingDiff.toDouble

    private final case class BucketKey(ratingDiff: Int, sideALevel: Int, sideBLevel: Int)

    def run(
        samples: List[GameCalibrationSample],
        current: EcorTableConfig = EcorRuntime.active,
        generatedAt: Long = System.currentTimeMillis
    ): CalibrationRun =
      val validSamples = samples.takeRight(maxStoredGameSamples).filter(_.valid)
      val buckets = aggregate(validSamples.filter(_.informative))
      val currentGapValues = current.orderedGaps.map(_.ratingPoints.toDouble).toVector
      val calculated =
        if buckets.isEmpty then currentGapValues
        else solveWeightedLeastSquares(buckets, currentGapValues).getOrElse(currentGapValues)
      val rounded = calculated.map(value => math.max(0, math.min(500, math.round(value).toInt)))
      val stats = residualStats(buckets, rounded.map(_.toDouble))
      val support = supportByGap(validSamples)
      val estimates =
        current.orderedGaps.zipWithIndex.map: (gap, index) =>
          val calculatedValue = rounded(index)
          EcorGapEstimate(
            gap = gap,
            currentRatingPoints = gap.ratingPoints,
            calculatedRatingPoints = calculatedValue,
            delta = calculatedValue - gap.ratingPoints,
            supportSamples = support.getOrElse(index, 0)
          )

      CalibrationRun(
        generatedAt = generatedAt,
        modelVersion = calibrationModelVersion,
        sampleCount = validSamples.length,
        informativeSampleCount = validSamples.count(_.informative),
        residualMeanRatingPoints = stats._1,
        residualStdDevRatingPoints = stats._2,
        meanAbsoluteResidualRatingPoints = stats._3,
        currentTable = current,
        estimates = estimates
      )

    private def aggregate(samples: List[GameCalibrationSample]): List[Bucket] =
      samples
        .groupBy(sample => BucketKey(roundToNearestTen(sample.sideARating - sample.sideBRating), sample.sideALevel.value, sample.sideBLevel.value))
        .toList
        .map: (key, rows) =>
          Bucket(
            ratingDiff = key.ratingDiff,
            sideALevel = Level(key.sideALevel),
            sideBLevel = Level(key.sideBLevel),
            averageScore = rows.map(_.sideAScore).sum / rows.length.toDouble,
            count = rows.length
          )

    private def solveWeightedLeastSquares(
        buckets: List[Bucket],
        currentGapValues: Vector[Double]
    ): Option[Vector[Double]] =
      val n = Level.max
      val matrix = Array.fill(n, n)(0.0)
      val rhs = Array.fill(n)(0.0)
      val ridge = 35.0

      for bucket <- buckets do
        val features = bucket.featureVector
        val weight = bucket.count.toDouble
        val target = bucket.targetOffsetDiff
        for i <- 0 until n do
          rhs(i) += weight * features(i) * target
          for j <- 0 until n do matrix(i)(j) += weight * features(i) * features(j)

      for i <- 0 until n do
        matrix(i)(i) += ridge
        rhs(i) += ridge * currentGapValues(i)

      gaussianSolve(matrix, rhs).map(_.toVector)

    private def gaussianSolve(matrix: Array[Array[Double]], rhs: Array[Double]): Option[Array[Double]] =
      val n = rhs.length
      val a = matrix.map(_.clone)
      val b = rhs.clone

      var singular = false
      for i <- 0 until n if !singular do
        val pivot = (i until n).maxBy(row => math.abs(a(row)(i)))
        if math.abs(a(pivot)(i)) < 1e-9 then singular = true
        else
          val tmpRow = a(i)
          a(i) = a(pivot)
          a(pivot) = tmpRow
          val tmpValue = b(i)
          b(i) = b(pivot)
          b(pivot) = tmpValue
          val divisor = a(i)(i)
          for col <- i until n do a(i)(col) = a(i)(col) / divisor
          b(i) = b(i) / divisor
          for row <- 0 until n if row != i do
            val factor = a(row)(i)
            for col <- i until n do a(row)(col) = a(row)(col) - factor * a(i)(col)
            b(row) = b(row) - factor * b(i)

      Option.when(!singular)(b)

    private def residualStats(buckets: List[Bucket], gaps: Vector[Double]): (Double, Double, Double) =
      if buckets.isEmpty then (0.0, 0.0, 0.0)
      else
        val weightedResiduals = buckets.map: bucket =>
          val predicted = dot(bucket.featureVector, gaps)
          bucket.count -> (bucket.targetOffsetDiff - predicted)
        val total = weightedResiduals.map(_._1).sum.toDouble
        val mean = weightedResiduals.map((count, residual) => count * residual).sum / total
        val variance = weightedResiduals.map((count, residual) => count * math.pow(residual - mean, 2)).sum / total
        val mae = weightedResiduals.map((count, residual) => count * math.abs(residual)).sum / total
        (mean, math.sqrt(variance), mae)

    private def supportByGap(samples: List[GameCalibrationSample]): Map[Int, Int] =
      (0 until Level.max).map: index =>
        index -> samples.count(sample => math.abs(sample.featureVector(index)) > 0.0)
      .toMap

    private def dot(a: Vector[Double], b: Vector[Double]): Double =
      a.zip(b).map(_ * _).sum

    private def roundToNearestTen(value: Int): Int =
      math.round(value / 10.0).toInt * 10

    private def scoreToEloDiff(score: Double): Double =
      val clamped = math.max(0.02, math.min(0.98, score))
      400.0 * math.log10(clamped / (1.0 - clamped))

  final case class EcorAdminState(
      config: EcorTableConfig,
      history: List[EcorSnapshot],
      latestCalibration: Option[CalibrationRun],
      storedSampleCount: Int,
      parseError: Option[String]
  ):
    def valid: Boolean =
      config.valid &&
        history.forall(_.valid) &&
        latestCalibration.forall(_.valid) &&
        storedSampleCount >= 0

  object EcorAdminState:
    def fromText(
        version: String,
        gapText: String,
        ratingBandsText: String,
        historyText: String
    ): EcorAdminState =
      val parsed = EcorTableConfig.fromText(version, gapText, ratingBandsText)
      EcorAdminState(
        config = parsed.getOrElse(EcorTableConfig.default),
        history = EcorHistory.parse(historyText),
        latestCalibration = CalibrationRuntime.lastRun,
        storedSampleCount = GameHistory.size,
        parseError = parsed.left.toOption
      )

  private def encode(value: String): String =
    Base64.getUrlEncoder.withoutPadding.encodeToString(value.getBytes(StandardCharsets.UTF_8))

  private def decode(value: String): String =
    String(Base64.getUrlDecoder.decode(value), StandardCharsets.UTF_8)

  private def formatDouble(value: Double): String =
    f"$value%.1f"
