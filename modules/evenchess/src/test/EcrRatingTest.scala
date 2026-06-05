package lila.evenchess

class EcrRatingTest extends munit.FunSuite:

  import AssistanceAccounting.UsedOffset
  import CoachingLadder.Level
  import EcrRating.*
  import EvenChessMode.TimeControlBucket

  private val now = 123456789L
  private val rapid = EcrRecord.provisional("player-1", EcrPool.Rapid, now)
  private val opponent = EcrRecord.provisional("player-2", EcrPool.Rapid, now).copy(rating = 1500)

  test("ECR records represent EvenChess Rating, not normal chess Elo"):
    assertEquals(rapid.labelledName, "EvenChess Rating (ECR)")
    assert(!rapid.isNormalChessElo)
    assert(rapid.hasRequiredFields)
    assertEquals(rapid.pool, EcrPool.Rapid)
    assertEquals(rapid.ratingPolicyVersion, "ecr-rating-v1")
    assertEquals(rapid.calibrationPolicyVersion, "ecr-calibration-v1")
    assertEquals(rapid.matchmakingPolicyVersion, "ecr-matchmaking-v1")

  test("pool policies isolate EvenChess pools, Target shadow, and normal Lichess ratings"):
    assertEquals(PoolPolicies.byPool(EcrPool.Rapid).status, PoolLaunchStatus.PrimaryLikelyLaunch)
    assert(PoolPolicies.byPool(EcrPool.Rapid).updatesNormalEcr)
    assert(!PoolPolicies.byPool(EcrPool.Rapid).updatesNormalLichessRating)
    assert(!PoolPolicies.byPool(EcrPool.TargetShadow).updatesNormalEcr)
    assert(!PoolPolicies.byPool(EcrPool.TargetShadow).updatesNormalLichessRating)
    assert(!PoolPolicies.byPool(EcrPool.NormalLichess).updatesNormalEcr)
    assert(PoolPolicies.byPool(EcrPool.NormalLichess).updatesNormalLichessRating)

  test("only normal rated EvenChess updates normal ECR"):
    assert(EcrUpdateEligibility.updatesAreServerSide)
    assert(EcrUpdateEligibility.updatesAreAuditable)
    assert(EcrUpdateEligibility.updatesNormalEcr(RatedMode.NormalRatedEvenChess))
    assert(!EcrUpdateEligibility.updatesNormalEcr(RatedMode.CasualEvenChess))
    assert(!EcrUpdateEligibility.updatesNormalEcr(RatedMode.TargetLevelMvp))
    assert(!EcrUpdateEligibility.normalLichessRatingMayBePollutedByEvenChess)

  test("expected score uses Effective Rating equal to ECR plus Used Offset"):
    val result = EcrGameResult(
      player = rapid.copy(rating = 1500),
      opponent = opponent.copy(rating = 1500),
      playerUsedOffset = UsedOffset(100, "assistance-load-v1"),
      opponentUsedOffset = UsedOffset(0, "assistance-load-v1"),
      score = 1.0,
      modelVersion = "ecr-rating-v1",
      auditEventIds = List("audit-1")
    )

    assert(result.expectedScore > 0.5)
    assert(result.auditable)

  test("rating replay is versioned and auditable without touching normal ratings"):
    val result = EcrGameResult(
      player = rapid.copy(rating = 1500, gameCount = 9),
      opponent = opponent.copy(rating = 1500),
      playerUsedOffset = UsedOffset(0, "assistance-load-v1"),
      opponentUsedOffset = UsedOffset(0, "assistance-load-v1"),
      score = 1.0,
      modelVersion = "ecr-rating-v1",
      auditEventIds = List("audit-1", "audit-2")
    )
    val updated = RatingReplay.applySimpleUpdate(result, kFactor = 20, updatedAt = now + 1)

    assert(RatingReplay.simpleDelta(result, 20) > 0)
    assertEquals(updated.gameCount, 10)
    assert(!updated.provisional)
    assertEquals(updated.updatedAt, now + 1)

  test("matchmaking initial stage requires same pool/time control and tight ECR/effective windows"):
    val a = MatchmakingProfile("a", rapid.copy(rating = 1500), UsedOffset(20, "assistance-load-v1"), TimeControlBucket.Rapid, Level(5), 40, abuseClear = true)
    val b = MatchmakingProfile("b", opponent.copy(rating = 1540), UsedOffset(10, "assistance-load-v1"), TimeControlBucket.Rapid, Level(5), 50, abuseClear = true)
    val decision = Matchmaking.decide(a, b, SearchWindows.byStage(SearchStage.Initial), maxLatencyMillis = 100)

    assert(decision.allowed)
    assert(!decision.requiresConfirmation)
    assert(decision.reasons.isEmpty)

  test("search widening expands ECR first, then effective rating, then level compatibility with confirmation"):
    val initial = SearchWindows.byStage(SearchStage.Initial)
    val widening1 = SearchWindows.byStage(SearchStage.Widening1)
    val widening2 = SearchWindows.byStage(SearchStage.Widening2)
    val widening3 = SearchWindows.byStage(SearchStage.Widening3)

    assert(widening1.maxEcrDelta > initial.maxEcrDelta)
    assertEquals(widening1.maxEffectiveRatingDelta, initial.maxEffectiveRatingDelta)
    assert(widening2.maxEffectiveRatingDelta > widening1.maxEffectiveRatingDelta)
    assert(widening3.allowLevelCompatibilityExpansion)
    assert(widening3.requiresConfirmationForLevelContractChange)
  test("level contract widening requires confirmation when otherwise compatible"):
    val a = MatchmakingProfile("a", rapid.copy(rating = 1500), UsedOffset(0, "assistance-load-v1"), TimeControlBucket.Rapid, Level(4), 40, abuseClear = true)
    val b = MatchmakingProfile("b", opponent.copy(rating = 1510), UsedOffset(0, "assistance-load-v1"), TimeControlBucket.Rapid, Level(5), 40, abuseClear = true)
    val initialDecision = Matchmaking.decide(a, b, SearchWindows.byStage(SearchStage.Initial), maxLatencyMillis = 100)
    val widenedDecision = Matchmaking.decide(a, b, SearchWindows.byStage(SearchStage.Widening3), maxLatencyMillis = 100)

    assert(!initialDecision.allowed)
    assert(initialDecision.reasons.contains("level_contract"))
    assert(widenedDecision.allowed)
    assert(widenedDecision.requiresConfirmation)
    assert(Matchmaking.gameStartConfirmationShowsSetLevelAndPool)
    assert(Matchmaking.postGameReportingExplainsUsedLevelAndUsedOffset)

  test("repeat rematches and rating transfer patterns can be flagged"):
    val normal = RepeatPattern("a", "b", rematchCount = 2, ratingTransferDelta = 20)
    val repeated = RepeatPattern("a", "b", rematchCount = 8, ratingTransferDelta = 20)
    val transfer = RepeatPattern("a", "b", rematchCount = 2, ratingTransferDelta = 120)

    assert(Matchmaking.monitorsRepeatRematchesAndRatingTransfer)
    assert(!RepeatPatternControls.flagged(normal, maxRematches = 5, maxRatingTransferDelta = 80))
    assert(RepeatPatternControls.flagged(repeated, maxRematches = 5, maxRatingTransferDelta = 80))
    assert(RepeatPatternControls.flagged(transfer, maxRematches = 5, maxRatingTransferDelta = 80))

  test("calibration dashboards and old-game explainability preserve model versions"):
    assert(CalibrationDashboard.includesRequiredDimensions(CalibrationDashboard.requiredDimensions))
    assert(!CalibrationDashboard.includesRequiredDimensions(Set("Used Level", "ECR band")))

    val version = ExplainableGameVersion("game-1", "ecr-rating-v1", "ecr-calibration-v1", "ecr-matchmaking-v1")
    assert(version.explainableUnderOriginalVersions)

  test("L8-L10 are not hard-banned but need acceptable safety metrics for public rated rollout"):
    val unsafe = CalibrationSafetyMetrics(acceptableResiduals = true, acceptableAbuseSignals = false, acceptableCompletionRate = true)
    val safe = CalibrationSafetyMetrics(acceptableResiduals = true, acceptableAbuseSignals = true, acceptableCompletionRate = true)

    assert(!HighLevelRollout.l8ToL10HardBannedByPolicy)
    assert(HighLevelRollout.publicRatedAllowed(Level(7), unsafe))
    assert(!HighLevelRollout.publicRatedAllowed(Level(8), unsafe))
    assert(HighLevelRollout.publicRatedAllowed(Level(10), safe))

  test("integration rules require inspection and patch map before core rating or pairing seams"):
    assert(IntegrationRules.inspectLilaRatingAndPairingBeforeSeams)
    assert(IntegrationRules.mayUseSeparateEvenChessRatingServiceIfCoreIntegrationUnsafe)
    assert(IntegrationRules.coreRatingOrPairingEditsRequirePatchMap)
