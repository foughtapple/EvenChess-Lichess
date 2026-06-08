package lila.evenchess

class LevelBasedMatchmakingTest extends munit.FunSuite:

  import AssistanceAccounting.UsedOffset
  import CoachingLadder.Level
  import EcrRating.{ EcrGameResult, EcrPool, EcrRecord, RatedMode, SearchStage }
  import EvenChessMode.TimeControlBucket
  import LevelBasedMatchmaking.*
  import ProductInvariants.RequirementClass

  private val now = 123456789L
  private val context =
    SearchTelemetryContext(
      schemaVersion = "matchmaking-telemetry-v1",
      occurredAt = now,
      pseudonymousUserId = "user-hash-1"
    )

  private def ecr(playerId: String, pool: EcrPool, rating: Int) =
    EcrRecord
      .provisional(playerId, pool, now)
      .copy(rating = rating, gameCount = 12, provisional = false)

  private def ticket(
      ticketId: String,
      playerId: String,
      poolKey: PoolKey = PoolKey.normal(TimeControlBucket.Rapid),
      rating: Int = 1500,
      setLevel: Level = Level(5),
      expectedOffset: Int = 0,
      targetLevel: Option[Level] = None,
      botProfile: Option[BotMatchProfile] = None,
      latencyMillis: Int = 40,
      abuseClear: Boolean = true
  ) =
    SearchTicket(
      ticketId = ticketId,
      playerId = playerId,
      poolKey = poolKey,
      requestedClock = None,
      ecr = ecr(playerId, poolKey.ecrPool, rating),
      expectedUsedOffset = UsedOffset(expectedOffset, "assistance-load-v1"),
      setLevel = setLevel,
      targetLevel = targetLevel,
      botProfile = botProfile,
      latencyMillis = latencyMillis,
      abuseClear = abuseClear,
      policyVersion = "matchmaking-policy-v1",
      createdAt = now
    )

  private def resultFor(player: EcrRecord, opponent: EcrRecord, score: Double = 1.0) =
    EcrGameResult(
      player = player,
      opponent = opponent,
      playerUsedOffset = UsedOffset(0, "assistance-load-v1"),
      opponentUsedOffset = UsedOffset(0, "assistance-load-v1"),
      score = score,
      modelVersion = "ecr-rating-v1",
      auditEventIds = List("audit-1", "audit-2")
    )

  test("Phase D/F requirements are classified before search and rating integration"):
    val byRequirement =
      PhaseDRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseDRequirement.SearchUsesEcrAndSetLevel), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseDRequirement.NormalLichessRatingsExcluded), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseDRequirement.MmrEngineOwnsPublicMatchmaking), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseDRequirement.MmrEngineOutputsMatchContract), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseDRequirement.TwoStateSearchScenarios), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseDRequirement.LevelContractConfirmation), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseDRequirement.WideningVisibleToPlayer), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseDRequirement.MatchContractsLoggedForAudit), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseDRequirement.SimulationBeforeProduction), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseDRequirement.TargetQueueIsolation), RequirementClass.EvenChessSpecific)
    assertEquals(
      byRequirement(PhaseDRequirement.LilaPairingIntegrationDeferredToThinSeams),
      RequirementClass.AdaptedToLichessFork
    )

  test("search tickets carry valid ECR, Set Level, pool, offset, and reject normal Lichess ratings"):
    val rapid = ticket("t1", "player-1")
    val normalLichessPool = PoolKey(SearchQueue.NormalEvenChess, TimeControlBucket.Rapid, EcrPool.NormalLichess)
    val invalidNormalRating = ticket("t2", "player-2", poolKey = normalLichessPool)

    assert(rapid.valid)
    assert(rapid.rated)
    assertEquals(rapid.expectedEffectiveRating, 1500)
    assertEquals(rapid.profile.setLevel, Level(5))
    assert(!normalLichessPool.valid)
    assert(!invalidNormalRating.valid)

  test("friend level contract auto assigns lower player from base table and balances the recipient"):
    val request = FriendLevelContract.Request.fromFormValues(None, None, None)
    val result = FriendLevelContract.assign(
      challengerRating = 1000,
      opponentRating = 1150,
      request = request
    )

    assert(result.exists(_.valid))
    assertEquals(result.map(_.mode), Some(FriendLevelContract.Mode.AutoLevel))
    assertEquals(result.map(_.challengerLevel), Some(BaseSetLevelByRatingTable.levelForRating(1000)))
    assert(result.exists(_.requestCardSummary.contains("challenger L")))

  test("friend level contract can fix challenger level and choose recipient level"):
    val request = FriendLevelContract.Request.fromFormValues(
      mode = Some("my"),
      myLevel = Some("6"),
      opponentLevel = None
    )
    val result = FriendLevelContract.assign(
      challengerRating = 1300,
      opponentRating = 1400,
      request = request
    )

    assert(result.exists(_.valid))
    assertEquals(result.map(_.mode), Some(FriendLevelContract.Mode.SetMyLevel))
    assertEquals(result.map(_.challengerLevel), Some(Level(6)))
    assert(result.exists(_.opponentLevel.value >= Level.min))

  test("friend level contract can fix recipient level and choose challenger level"):
    val request = FriendLevelContract.Request.fromFormValues(
      mode = Some("opponent"),
      myLevel = None,
      opponentLevel = Some("3")
    )
    val result = FriendLevelContract.assign(
      challengerRating = 1500,
      opponentRating = 1420,
      request = request
    )

    assert(result.exists(_.valid))
    assertEquals(result.map(_.mode), Some(FriendLevelContract.Mode.SetOpponentLevel))
    assertEquals(result.map(_.opponentLevel), Some(Level(3)))
    assert(result.exists(_.challengerLevel.value <= Level.max))

  test("friend level contract can fix both levels and flag highly uneven matches"):
    val request = FriendLevelContract.Request.fromFormValues(
      mode = Some("both"),
      myLevel = Some("0"),
      opponentLevel = Some("10")
    )
    val result = FriendLevelContract.assign(
      challengerRating = 2400,
      opponentRating = 900,
      request = request
    )

    assert(result.exists(_.valid))
    assertEquals(result.map(_.mode), Some(FriendLevelContract.Mode.SetBothLevels))
    assertEquals(result.map(_.challengerLevel), Some(Level(0)))
    assertEquals(result.map(_.opponentLevel), Some(Level(10)))
    assert(result.exists(_.unevenMatch))

  test("bot profile maps setLevel to stockfish profile and can be validated"):
    val sfProfile = StockfishStrengthProfile.forLevel(Level(7))
    val profile =
      BotMatchProfile(
        botId = "bot-alpha",
        userRef = Some("user-42"),
        targetEcr = 1720,
        preferredSetLevel = Level(7),
        stockfishLevel = Level(4),
        timeControl = Some(TimeControlBucket.Rapid)
      )

    assert(sfProfile.valid)
    assert(profile.valid)
    assertEquals(profile.stockfishProfile.setLevel, Level(4))
    assertEquals(profile.preferredSetLevel, Level(7))
    assertEquals(profile.stockfishLevel, Level(4))
    assertNotEquals(profile.preferredSetLevel, profile.stockfishLevel)
    (0 to 10).foreach(level => assert(StockfishStrengthProfile.forLevel(Level(level)).valid))

  test("deterministic undeclared bot profiles are reproducible by botId"):
    val a = BotMatchProfile.fromSeed(
      botId = "bot-deterministic",
      userRef = None,
      timeControl = Some(TimeControlBucket.Rapid),
      pool = EcrPool.Rapid
    )
    val b = BotMatchProfile.fromSeed(
      botId = "bot-deterministic",
      userRef = None,
      timeControl = Some(TimeControlBucket.Rapid),
      pool = EcrPool.Rapid
    )

    assertEquals(a, b)
    assert(a.valid)
    assert(a.targetEcr > 0)
    assert(Level.isValid(a.preferredSetLevel.value))
    assertEquals(a.userRef, None)

  test("random bot profile generation uses pool distribution and supports anti-detect sampling"):
    val generated = BotMatchProfile.random(
      botId = "bot-random",
      timeControl = Some(TimeControlBucket.Blitz),
      pool = EcrPool.Blitz
    )
    val botTicket = ticket("bot-ticket", "bot-player", botProfile = Some(generated))
    val randomDelay = generated.nextThinkDelay(scala.util.Random(1234L))

    assert(generated.valid)
    assert(randomDelay >= generated.antiDetectProfile.minDelayMillis && randomDelay <= generated.antiDetectProfile.maxDelayMillis)
    assert(botTicket.isBotTicket)
    assertEquals(botTicket.botProfile, Some(generated))

  test("player preference alignment nudges bot rating and level while respecting pool/time-control constraints"):
    val playerProfile = BotMatchProfile.fromSeed(
      botId = "align-profile",
      userRef = None,
      timeControl = Some(TimeControlBucket.Rapid),
      pool = EcrPool.Rapid
    )
    val aligned = BotMatchProfile.alignToPlayerPreference(
      playerProfile = playerProfile,
      preferredEcr = 1600,
      preferredSetLevel = Level(6),
      pool = EcrPool.Rapid
    )

    assert(aligned.valid)
    assertEquals(aligned.timeControl, Some(TimeControlBucket.Rapid))
    assertEquals(aligned.persona, playerProfile.persona)
    assert(aligned.targetEcr >= 700)
    assert(aligned.targetEcr <= 2400)
    assert(Math.abs(aligned.preferredSetLevel.value - 6) <= 2)
    assert(
      aligned.stockfishProfile.setLevel == LichessEquivalentStockfishLevel.levelForRating(aligned.targetEcr),
      "stockfish level should track rating-derived Lichess-equivalent stockfish level"
    )

  test("bot personas map to different human-like versus fast timing profiles"):
    val human = BotMatchProfile.fromSeed(
      botId = "persona-human",
      userRef = None,
      timeControl = Some(TimeControlBucket.Rapid),
      pool = EcrPool.Rapid,
      persona = Some(BotMatchPersona.HumanLike)
    )
    val fast = BotMatchProfile.fromSeed(
      botId = "persona-fast",
      userRef = None,
      timeControl = Some(TimeControlBucket.Rapid),
      pool = EcrPool.Rapid,
      persona = Some(BotMatchPersona.Fast)
    )

    assert(human.valid)
    assert(fast.valid)
    assertEquals(human.persona, BotMatchPersona.HumanLike)
    assertEquals(fast.persona, BotMatchPersona.Fast)
    assert(fast.matchLatencyProfile.maxDelayMillis < human.matchLatencyProfile.maxDelayMillis)
    assert(fast.matchLatencyProfile.minDelayMillis < human.matchLatencyProfile.minDelayMillis)
    assert(human.nextMatchLatency(scala.util.Random(7), capMillis = 250) <= 250)
    assert(fast.nextMatchLatency(scala.util.Random(7), capMillis = 250) <= 250)
    assert(fast.nextMatchLatency(scala.util.Random(7), capMillis = 250) >= 0)

  test("bot seeding profile can enforce matchmaking latency cap independently from move profile"):
    val profile =
      BotMatchProfile.fromSeed(
        botId = "match-latency-profile",
        userRef = None,
        timeControl = Some(TimeControlBucket.Classical),
        pool = EcrPool.NormalLichess,
        persona = Some(BotMatchPersona.Fast)
      )
    val matchLatencies = (1 to 25).map(i => profile.nextMatchLatency(scala.util.Random(i), capMillis = 220))

    assert(matchLatencies.forall(_ >= profile.matchLatencyProfile.minDelayMillis))
    assert(matchLatencies.forall(_ <= 220))
    assert(profile.antiDetectProfile.maxDelayMillis > profile.antiDetectProfile.minDelayMillis)
  test("initial pairing requires same EvenChess pool, time control, ECR window, and effective window"):
    val a = ticket("a", "player-a", rating = 1500, setLevel = Level(5), expectedOffset = 10)
    val b = ticket("b", "player-b", rating = 1530, setLevel = Level(5), expectedOffset = 0)
    val decision = PairingEngine.decide(a, b, SearchStage.Initial, uiConfirmedLevelContract = false)

    assert(decision.allowed)
    assert(!decision.requiresConfirmation)
    assert(decision.reasons.isEmpty)
    assert(decision.confirmation.exists(_.valid))
    assert(decision.confirmation.exists(_.outsideHelpRule.contains("outside help")))

  test("search widening uses level-offset effective rating windows"):
    assert(SearchWideningPlan.widensInRequiredOrder)
    assertEquals(SearchWideningPlan.next(SearchStage.Initial), Some(SearchStage.Widening1))
    assertEquals(SearchWideningPlan.next(SearchStage.Widening1), Some(SearchStage.Widening2))
    assertEquals(SearchWideningPlan.next(SearchStage.Widening2), Some(SearchStage.Widening3))
    assertEquals(SearchWideningPlan.next(SearchStage.Widening3), None)

    val a = ticket("a", "player-a", rating = 1500, expectedOffset = 100)
    val b = ticket("b", "player-b", rating = 1620)

    assert(PairingEngine.decide(a, b, SearchStage.Initial, uiConfirmedLevelContract = false).allowed)

    val effectiveA = ticket("ea", "effective-a", rating = 1500)
    val effectiveMismatch = ticket("eb", "effective-b", rating = 1500, expectedOffset = 140)
    assert(
      !PairingEngine
        .decide(effectiveA, effectiveMismatch, SearchStage.Widening1, uiConfirmedLevelContract = false)
        .allowed
    )
    assert(
      PairingEngine
        .decide(effectiveA, effectiveMismatch, SearchStage.Widening2, uiConfirmedLevelContract = false)
        .allowed
    )

  test("rated and casual matchmaking can assign different set levels when effective rating remains fair"):
    val a = ticket("a", "player-a", setLevel = Level(4))
    val adjacent = ticket("b", "player-b", setLevel = Level(5))
    val far = ticket("c", "player-c", setLevel = Level(7))

    val initial = PairingEngine.decide(a, adjacent, SearchStage.Initial, uiConfirmedLevelContract = false)
    val farDecision = PairingEngine.decide(a, far, SearchStage.Initial, uiConfirmedLevelContract = false)

    assert(initial.allowed)
    assert(!initial.requiresConfirmation)
    assert(farDecision.allowed)
    assert(!farDecision.reasons.contains("level_contract"))

  test("MMR engine contract carries ECR, expected offsets, effective ratings, quality, token gate, and audit fields"):
    val white = ticket("w", "white", rating = 1500, setLevel = Level(5), expectedOffset = 50)
    val black = ticket("b", "black", rating = 1530, setLevel = Level(5), expectedOffset = 20)

    val contract =
      MmrEngine.contractFromTickets(
        requestId = "request-1",
        white = white,
        black = black,
        stage = SearchStage.Initial,
        preferences = MatchPreferences.normal,
        tokenGateResult = "game_token_available"
      ) match
        case Right(contract) => contract
        case Left(error)     => fail(error)
    val audit =
      MmrEngine.auditRecord(
        contract,
        abuseSignalKeys = List("repeat_pairing_monitor", "abort_abuse_monitor", "level_target_manipulation"),
        loggedAt = now + 1
      )

    assert(contract.valid)
    assertEquals(contract.gameId, None)
    assertEquals(contract.whiteEcr, 1500)
    assertEquals(contract.blackEcr, 1530)
    assertEquals(contract.whiteMmr, 1500)
    assertEquals(contract.blackMmr, 1530)
    assertEquals(contract.whiteExpectedOffset, UsedOffset(50, "assistance-load-v1"))
    assertEquals(contract.blackExpectedOffset, UsedOffset(20, "assistance-load-v1"))
    assertEquals(contract.whiteEffectiveRating, 1550)
    assertEquals(contract.blackEffectiveRating, 1550)
    assertEquals(contract.preferenceFlags.scenario, SearchPreferenceScenario.NormalSearch)
    assertEquals(contract.tokenGateResult, "game_token_available")
    assertEquals(contract.policyVersion, MmrEngine.policyVersion)
    assert(contract.matchQuality.valid)
    assert(audit.valid)
    assertEquals(audit.calibrationModelVersion, MmrEngine.calibrationModelVersion)

  test("MMR engine represents normal and preferred-own-level scenarios"):
    assert(LevelOffsetTable.valid)
    assertEquals(LevelOffsetTable.valueForLevel(Level(10)), 190)
    assertEquals(BaseSetLevelByRatingTable.levelForRating(1500), Level(7))
    assertEquals(MatchPreferences.normal.scenario, SearchPreferenceScenario.NormalSearch)
    assertEquals(MatchPreferences(Some(Level(4))).scenario, SearchPreferenceScenario.PreferredOwnSetLevel)

    val playerLevelFour = ticket("p4", "player-four", setLevel = Level(4), expectedOffset = 28)
    val opponentLevelFour = ticket("o4", "opponent-four", setLevel = Level(4), expectedOffset = 28)
    val preferredContract =
      MmrEngine.contractFromTickets(
        requestId = "preferred",
        white = playerLevelFour,
        black = opponentLevelFour,
        stage = SearchStage.Initial,
        preferences = MatchPreferences(Some(Level(4))),
        tokenGateResult = "game_token_available"
      ) match
        case Right(contract) => contract
        case Left(error)     => fail(error)

    assert(preferredContract.valid)
    assertEquals(preferredContract.preferenceFlags.scenario, SearchPreferenceScenario.PreferredOwnSetLevel)
    assert(preferredContract.preferenceFlags.requesterPreferredLevelMatched)
    assert(!preferredContract.preferenceFlags.unevenMatch)

  test("normal search can assign player and opponent set levels from any L0-L10 combination"):
    val request = ticket("request-any", "request-any-player", rating = 1500, setLevel = Level(5))
    val candidate = ticket("candidate-any", "candidate-any-player", rating = 1570, setLevel = Level(5))

    val simulation =
      MmrEngine.simulate(
        requestId = "normal-any",
        request = request,
        candidates = List(candidate),
        preferences = MatchPreferences.normal,
        tokenGateResult = "game_token_available"
      )

    val contract = simulation.contract.getOrElse(fail("normal search should assign a level-balanced contract"))
    assert(simulation.valid)
    assertEquals(contract.stage, SearchStage.Initial)
    assertEquals(contract.preferenceFlags.scenario, SearchPreferenceScenario.NormalSearch)
    assert(contract.matchQuality.effectiveRatingDelta <= 75)
    assert(contract.whiteSetLevel != contract.blackSetLevel)

  test("MMR simulation fixes preferred own level and marks impossible double-preference matches as uneven"):
    val request = ticket("request", "request-player", rating = 1500, setLevel = Level(4))
    val adjacentCandidate = ticket("candidate", "candidate-player", rating = 1700, setLevel = Level(5))
    val preferences = MatchPreferences(Some(Level(4)))

    val preferred =
      MmrEngine.simulate(
        requestId = "simulation-preferred",
        request = request,
        candidates = List(adjacentCandidate),
        preferences = preferences,
        tokenGateResult = "game_token_available"
      )

    assert(preferred.valid)
    assert(preferred.matched)
    assertEquals(preferred.contract.map(_.stage), Some(SearchStage.Widening3))
    assert(preferred.contract.exists(_.whiteSetLevel == Level(4)))
    assert(preferred.contract.exists(_.preferenceFlags.requesterPreferredLevelMatched))
    assert(preferred.visibleMessages.exists(_.contains("Search window widened")))
    assertEquals(preferred.evaluatedStages, SearchWideningPlan.orderedStages)

    val fixedHighRequest = ticket("fixed-high-request", "fixed-high-request-player", rating = 1500, setLevel = Level(10))
    val fixedHighCandidate = ticket("fixed-high-candidate", "fixed-high-candidate-player", rating = 2500, setLevel = Level(10))
    val uneven =
      MmrEngine.simulate(
        requestId = "simulation-uneven",
        request = fixedHighRequest,
        candidates = List(fixedHighCandidate),
        preferences = MatchPreferences(Some(Level(10))),
        tokenGateResult = "game_token_available",
        candidatePreferences = Map("fixed-high-candidate" -> MatchPreferences(Some(Level(10))))
      )

    assert(uneven.valid)
    assert(uneven.matched)
    assert(uneven.contract.exists(_.unevenMatch))
    assert(uneven.contract.exists(_.preferenceFlags.unevenReason.contains("both_players_fixed_preferred_set_levels_outside_effective_rating_window")))

  test("Target Level queue is isolated from Normal EvenChess and requires target confirmation"):
    val targetPool = PoolKey.target(TimeControlBucket.Rapid)
    val targetA = ticket("ta", "target-a", poolKey = targetPool, targetLevel = Some(Level(5)), setLevel = Level(5))
    val targetB = ticket("tb", "target-b", poolKey = targetPool, targetLevel = Some(Level(6)), setLevel = Level(6))
    val normal = ticket("n", "normal")

    val targetUnconfirmed =
      PairingEngine.decide(targetA, targetB, SearchStage.Widening3, uiConfirmedLevelContract = false)
    val targetConfirmed =
      PairingEngine.decide(targetA, targetB, SearchStage.Widening3, uiConfirmedLevelContract = true)
    val mixed = PairingEngine.decide(targetA, normal, SearchStage.Widening3, uiConfirmedLevelContract = true)

    assert(targetA.valid)
    assert(!targetA.rated)
    assert(!targetUnconfirmed.allowed)
    assert(targetUnconfirmed.requiresConfirmation)
    assert(targetConfirmed.allowed)
    assert(!mixed.allowed)
    assert(mixed.reasons.contains("pool_key_mismatch"))

  test("rating application updates only normal rated EvenChess ECR and never normal Lichess ratings"):
    EvenChessRatingCalibration.GameHistory.clear()
    val poolKey = PoolKey.normal(TimeControlBucket.Rapid)
    val player = ecr("player-a", EcrPool.Rapid, 1500)
    val opponent = ecr("player-b", EcrPool.Rapid, 1500)
    val normalApplication = RatingApplication(
      gameId = "game-1",
      result = resultFor(player, opponent),
      mode = RatedMode.NormalRatedEvenChess,
      poolKey = poolKey,
      kFactor = 20,
      updatedAt = now + 1
    )

    val normalDecision = RatingApplicationService.apply(normalApplication, context)
    assert(normalDecision.normalEcrChanged)
    assert(!normalDecision.normalLichessRatingChanged)
    assert(normalDecision.updatedRecord.exists(_.rating > player.rating))
    assert(normalDecision.telemetry.exists(_.readyForRatedLedger))
    assertEquals(EvenChessRatingCalibration.GameHistory.size, 1)
    assertEquals(EvenChessRatingCalibration.GameHistory.latest().head.gameId, "game-1")

    val targetPool = PoolKey.target(TimeControlBucket.Rapid)
    val targetPlayer = ecr("target-a", EcrPool.TargetShadow, 1500)
    val targetOpponent = ecr("target-b", EcrPool.TargetShadow, 1500)
    val targetApplication = normalApplication.copy(
      gameId = "target-game",
      result = resultFor(targetPlayer, targetOpponent),
      mode = RatedMode.TargetLevelMvp,
      poolKey = targetPool
    )
    val targetDecision = RatingApplicationService.apply(targetApplication, context)

    assert(!targetDecision.normalEcrChanged)
    assert(!targetDecision.normalLichessRatingChanged)
    assertEquals(targetDecision.updatedRecord, None)
    assert(targetDecision.reasons.contains("mode_does_not_update_normal_ecr"))
    assertEquals(EvenChessRatingCalibration.GameHistory.size, 1)

  test("search and game-start telemetry are server-authored and versioned for replay"):
    val a = ticket("a", "player-a")
    val b = ticket("b", "player-b")
    val search = SearchTelemetry.searchStarted(a, context)
    val started = SearchTelemetry.gameStarted("game-1", a, b, context)

    assert(context.valid)
    assert(search.readyForRatedLedger)
    assert(started.readyForRatedLedger)
    assertEquals(search.name, TelemetryAnalytics.TelemetryEventName.MatchSearchStarted)
    assertEquals(started.name, TelemetryAnalytics.TelemetryEventName.GameStarted)
    assertEquals(search.authority, TelemetryAnalytics.EventAuthority.Server)
    assert(started.versions.hasRelevantVersion)
