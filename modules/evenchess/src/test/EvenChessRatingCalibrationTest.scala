package lila.evenchess

class EvenChessRatingCalibrationTest extends munit.FunSuite:

  import CoachingLadder.Level
  import EvenChessRatingCalibration.*

  private val now = 123456789L

  override def beforeEach(context: BeforeEach): Unit =
    EcorRuntime.activate(EcorTableConfig.default)
    CalibrationRuntime.clear()
    GameHistory.clear()

  test("default ECOR table defines adjacent gaps and cumulative offsets"):
    val config = EcorTableConfig.default

    assert(config.valid)
    assertEquals(config.orderedGaps.map(_.ratingPoints), List(5, 5, 8, 10, 17, 20, 25, 30, 35, 35))
    assertEquals(config.offsetValueForLevel(Level(0)), 0)
    assertEquals(config.offsetValueForLevel(Level(4)), 28)
    assertEquals(config.offsetValueForLevel(Level(10)), 190)
    assertEquals(config.levelForRating(674), Level(0))
    assertEquals(config.levelForRating(1500), Level(7))
    assertEquals(config.levelForRating(2200), Level(10))

  test("admin text parser accepts ECOR gap and rating-to-level tables"):
    val parsed = EcorTableConfig.fromText(
      defaultPolicyVersion,
      EcorDefaults.gapText,
      EcorDefaults.ratingBandsText
    ).toOption.get

    assert(parsed.valid)
    assertEquals(parsed.gapText, EcorDefaults.gapText)
    assertEquals(parsed.ratingBandsText, EcorDefaults.ratingBandsText)
    assert(EcorTableConfig.fromText(defaultPolicyVersion, "L0-L1=-1", EcorDefaults.ratingBandsText).isLeft)
    assert(EcorTableConfig.fromText(defaultPolicyVersion, EcorDefaults.gapText, "1500-1600=L7").isLeft)

  test("admin text parser accepts Stockfish equivalent rating bands"):
    val parsed = StockfishAiRatingTableConfig.fromText(StockfishAiRatingDefaults.tableText).toOption.get

    assert(parsed.valid)
    assertEquals(parsed.levelForRating(799), Level(1))
    assertEquals(parsed.levelForRating(1500), Level(5))
    assertEquals(parsed.levelForRating(2200), Level(8))
    assertEquals(parsed.windowText(4), "1200-1399")
    assert(StockfishAiRatingTableConfig.fromText("SF1=400-799").isLeft)
    assert(StockfishAiRatingTableConfig.fromText(StockfishAiRatingDefaults.tableText.replace("SF4=1200-1399", "SF4=1300-1399")).isLeft)

  test("ECOR snapshot history stores rollbackable table memory"):
    val snapshot = EcorSnapshot(
      timestampMillis = now,
      adminId = "admin-1",
      reason = "initial calibration",
      version = defaultPolicyVersion,
      gapText = EcorDefaults.gapText,
      ratingBandsText = EcorDefaults.ratingBandsText
    )
    val history = EcorHistory.append("", snapshot)
    val parsed = EcorHistory.parse(history)

    assertEquals(parsed.length, 1)
    assertEquals(parsed.head.reason, "initial calibration")
    assertEquals(EcorHistory.find(history, now).map(_.gapText), Some(EcorDefaults.gapText))

  test("calibration engine calculates gap recommendations and residual statistics"):
    val samples =
      (1 to 80).toList.flatMap: index =>
        List(
          GameCalibrationSample(s"a-$index", 1500, 1500, Level(6), Level(4), 1.0, now + index),
          GameCalibrationSample(s"b-$index", 1500, 1500, Level(4), Level(6), 0.0, now + 1000 + index),
          GameCalibrationSample(s"c-$index", 1500, 1500, Level(5), Level(5), 0.5, now + 2000 + index)
        )

    val run = CalibrationEngine.run(samples, EcorTableConfig.default, now)

    assert(run.valid)
    assertEquals(run.sampleCount, 240)
    assertEquals(run.informativeSampleCount, 160)
    assert(run.estimates.forall(_.supportSamples >= 0))
    assert(run.residualStdDevRatingPoints >= 0)
    assert(run.meanAbsoluteResidualRatingPoints >= 0)
    assert(run.calculatedGapText.contains("L4-L5="))

  test("game history keeps latest samples and deduplicates by game id"):
    val first = GameCalibrationSample("game-1", 1500, 1500, Level(5), Level(4), 1.0, now)
    val replacement = first.copy(sideAScore = 0.0, playedAt = now + 1)

    GameHistory.record(first)
    GameHistory.record(replacement)

    assertEquals(GameHistory.size, 1)
    assertEquals(GameHistory.latest().head.sideAScore, 0.0)
