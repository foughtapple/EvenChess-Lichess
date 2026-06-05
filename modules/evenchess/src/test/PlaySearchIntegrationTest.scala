package lila.evenchess

class PlaySearchIntegrationTest extends munit.FunSuite:

  import AssistanceAccounting.UsedOffset
  import CoachingLadder.Level
  import EcrRating.{ EcrPool, SearchStage }
  import EvenChessMode.TimeControlBucket
  import PlaySearchIntegration.*
  import ProductInvariants.RequirementClass

  private val now = 123456789L

  private def request(
      ticketId: String = "ticket-1",
      playerId: String = "player-1",
      form: PlayForm = PlayForm.default,
      tokenSnapshot: TokenSnapshot = TokenSnapshot.phaseIOnboardingDefault,
      botProfile: Option[LevelBasedMatchmaking.BotMatchProfile] = None
  ) =
    SearchStartRequest(
      ticketId = ticketId,
      playerId = playerId,
      form = form,
      tokenSnapshot = tokenSnapshot,
      expectedUsedOffset = ExpectedOffsetEstimate.forSetLevel(form.setLevel),
      botProfile = botProfile,
      latencyMillis = 40,
      createdAt = now
    )

  private def prepared(ticketId: String, playerId: String, setLevel: Level = Level(5)) =
    val repo = new InMemoryPlaySearchRepository
    SearchStartService
      .prepare(request(ticketId, playerId, PlayForm.default.copy(setLevel = setLevel)), repo)
      .toOption
      .get
      .record

  test("Phase I requirements are classified before play/search integration"):
    val byRequirement =
      PhaseIRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseIRequirement.LichessGameLifecyclePreserved), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseIRequirement.PublicCtasUseEvenChessPlayRoutes), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseIRequirement.LevelTimeRatedTokenSearchForm), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseIRequirement.SearchUsesEcrSetLevelAndExpectedOffset), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseIRequirement.ServerOwnedMetadataBeforeCoaching), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseIRequirement.EcrIsolationFromNormalLichessRatings), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseIRequirement.LilaPairingAdapterThinAndPatchMapped), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseIRequirement.SearchHandsOffToMmrEngine), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseIRequirement.MmrContractReadyBeforeLilaGameCreation), RequirementClass.AdaptedToLichessFork)

  test("public play routes replace homepage anchors for EvenChess search starts"):
    assertEquals(Routes.play, "/evenchess/play")
    assertEquals(Routes.search, "/evenchess/play/search")
    assertEquals(Routes.searchJson, "/evenchess/play/search.json")
    assert(Routes.aiPractice.startsWith(Routes.play))
    assert(!Routes.play.contains("#"))
    assert(!Routes.search.contains("#"))

  test("Lichess lobby adapter reuses the native table while starting EvenChess search"):
    assert(LichessLobbyAdapter.valid)
    assertEquals(LichessLobbyAdapter.playHref, "/#hook")
    assertEquals(LichessLobbyAdapter.searchAction, Routes.searchJson)
    assertEquals(LichessLobbyAdapter.aiPracticeHref, "/#ai")
    assertEquals(LichessLobbyAdapter.targetLevelHref, "/#hook")
    assertEquals(LichessLobbyAdapter.fallbackSetupHref, "/#hook")
    assert(!LichessLobbyAdapter.clientMayCreateNormalRatedGame)
    assert(!LichessLobbyAdapter.clientMayAuthorizeCoaching)
    assert(LichessLobbyAdapter.includesSetLevel)
    assert(LichessLobbyAdapter.includesOutsideHelpDisclosure)
    assert(LichessLobbyAdapter.includesPreferredSetLevel)

  test("play form parses level, time control, mode, and outside-help acknowledgement"):
    val form =
      PlayForm.fromValues(
        modeKey = "rated",
        timeControlKey = "rapid",
        setLevelValue = "6",
        targetLevelValue = None,
        confirmsOutsideHelpRule = true,
        confirmsLevelContract = false
      )

    assertEquals(form.map(_.setLevel), Right(Level(6)))
    assertEquals(form.map(_.timeControl.bucket), Right(TimeControlBucket.Rapid))
    assert(PlayForm.fromValues("rated", "rapid", "11", None, true, false).isLeft)
    assert(PlayForm.fromValues("rated", "rapid", "5", None, false, false).isLeft)

  test("play form preserves exact real-time clock metadata for lila game creation"):
    val clock = LevelBasedMatchmaking.RequestedClock(limitSeconds = 180, incrementSeconds = 2)
    val form =
      PlayForm
        .fromValues(
          modeKey = "rated",
          timeControlKey = "blitz",
          setLevelValue = "5",
          targetLevelValue = None,
          confirmsOutsideHelpRule = true,
          confirmsLevelContract = false,
          clockLimitSecondsValue = Some(clock.limitSeconds.toString),
          clockIncrementSecondsValue = Some(clock.incrementSeconds.toString)
        )
        .toOption
        .get
    val repo = new InMemoryPlaySearchRepository
    val prepared = SearchStartService.prepare(request(ticketId = "clock-ticket", playerId = "clock-player", form = form), repo).toOption.get

    assertEquals(form.requestedClock, Some(clock))
    assertEquals(prepared.record.ticket.requestedClock, Some(clock))

  test("play form parses preferred set level UI preference"):
    val form =
      PlayForm.fromValues(
        modeKey = "target",
        timeControlKey = "rapid",
        setLevelValue = "5",
        targetLevelValue = None,
        confirmsOutsideHelpRule = true,
        confirmsLevelContract = true,
        preferredSetLevelValue = Some("4")
      ).toOption.get

    assertEquals(form.preferredSetLevel, Some(Level(4)))
    assertEquals(form.targetLevelForTicket, Some(Level(4)))
    assertEquals(form.searchScenarioLabel, "Preferred set level search")
    assert(
      PlayForm
        .fromValues(
          "target",
          "rapid",
          "5",
          None,
          confirmsOutsideHelpRule = true,
          confirmsLevelContract = false,
          preferredSetLevelValue = Some("11")
        )
        .isLeft
    )

  test("play form treats Any as normal search and accepts old player target as a compatibility alias"):
    val alias =
      PlayForm.fromValues(
        modeKey = "rated",
        timeControlKey = "rapid",
        setLevelValue = "5",
        targetLevelValue = None,
        confirmsOutsideHelpRule = true,
        confirmsLevelContract = false,
        playerTargetLevelValue = Some("4")
      ).toOption.get
    val anyTargets =
      PlayForm.fromValues(
        modeKey = "casual",
        timeControlKey = "rapid",
        setLevelValue = "5",
        targetLevelValue = None,
        confirmsOutsideHelpRule = true,
        confirmsLevelContract = false,
        preferredSetLevelValue = Some("any")
      ).toOption.get

    assertEquals(alias.searchScenarioLabel, "Preferred set level search")
    assertEquals(alias.effectivePreferredSetLevel, Some(Level(4)))
    assertEquals(anyTargets.searchScenarioLabel, "Normal search")
    assertEquals(anyTargets.effectivePreferredSetLevel, None)

  test("search start persists ECR and Set Level search intent without allowing coaching render"):
    val repo = new InMemoryPlaySearchRepository
    val prepared = SearchStartService.prepare(request(), repo).toOption.get
    val record = prepared.record

    assert(prepared.valid)
    assertEquals(repo.get(record.ticket.ticketId), Some(record))
    assertEquals(record.ticket.ecr.pool, EcrPool.Rapid)
    assertEquals(record.ticket.setLevel, Level(5))
    assertEquals(record.ticket.expectedUsedOffset, UsedOffset(45, ExpectedOffsetEstimate.modelVersion))
    assert(record.queueState.persisted)
    assert(record.queueState.waitingForPairing)
    assertEquals(record.matchPreferences, LevelBasedMatchmaking.MatchPreferences.normal)
    assert(!prepared.coachingMayRender)
    assert(prepared.telemetry.readyForRatedLedger)
    assertEquals(record.ticket.ecr.isNormalChessElo, false)

  test("launch free-token window allows rated starts without consuming startup token eligibility"):
    val repo = new InMemoryPlaySearchRepository
    val waivedSnapshot =
      TokenSnapshot.emptyFreeAccount.withFreeMatchTokenWindow(active = true)
    val prepared =
      SearchStartService
        .prepare(request(tokenSnapshot = waivedSnapshot), repo)
        .toOption
        .get
    val record = prepared.record

    assert(prepared.valid)
    assertEquals(record.tokenSnapshot.availableGameStarts, 0)
    assert(record.tokenSnapshot.freeMatchTokenWindowActive)
    assertEquals(record.tokenSnapshot.accessReason(PlayMode.RatedEvenChess), "launch_free_token_window")
    assertEquals(record.admission.tokenEligible, true)

  test("empty free accounts remain blocked outside the launch free-token window"):
    val blocked =
      SearchStartService.prepare(request(tokenSnapshot = TokenSnapshot.emptyFreeAccount), new InMemoryPlaySearchRepository)

    assertEquals(blocked, Left("game_token_required"))

  test("search start service resumes an existing ticket without resetting wait time"):
    val repo = new InMemoryPlaySearchRepository
    val prepared = SearchStartService.prepare(request(ticketId = "resume-ticket", playerId = "resume-player"), repo).toOption.get
    val resumed =
      SearchStartService.resume("resume-ticket", "resume-player", repo, now + 6_000L).toOption.get

    assert(resumed.valid)
    assertEquals(resumed.record.ticket.ticketId, prepared.record.ticket.ticketId)
    assertEquals(resumed.record.ticket.playerId, "resume-player")
    assertEquals(resumed.record.createdAt, now)
    assertEquals(repo.active.count(_.ticket.ticketId == "resume-ticket"), 1)
    assert(SearchStartService.resume("resume-ticket", "other-player", repo, now + 6_000L).isLeft)

  test("search start request can carry bot match profile metadata"):
    val botProfile =
      LevelBasedMatchmaking.BotMatchProfile(
        botId = "bot-profile-1",
        userRef = Some("user-ref-1"),
        targetEcr = 1750,
        preferredSetLevel = Level(6),
        stockfishLevel = Level(4),
        timeControl = Some(TimeControlBucket.Rapid)
      )
    val prepared = SearchStartService.prepare(request(botProfile = Some(botProfile)), new InMemoryPlaySearchRepository).toOption.get
    val persistedProfile = prepared.record.ticket.botProfile

    assert(prepared.valid)
    assertEquals(prepared.record.ticket.isBotTicket, true)
    assertEquals(persistedProfile.map(_.botId), Some("bot-profile-1"))
    assert(persistedProfile.forall(_.targetEcr == 1750))
    assertEquals(prepared.record.ticket.ecr.rating, 1750)
    assert(!prepared.record.ticket.ecr.provisional)

  test("matchmaking integration evaluates persisted searches through the MMR engine"):
    val repo = new InMemoryPlaySearchRepository
    val whitePrepared = SearchStartService.prepare(request("white-ticket", "white-player"), repo).toOption.get
    val blackPrepared = SearchStartService.prepare(request("black-ticket", "black-player"), repo).toOption.get
    val integrated =
      MatchmakingIntegrationService.evaluate(whitePrepared.record.ticket.ticketId, repo, now + 1).toOption.get

    assert(integrated.valid)
    assert(integrated.matched)
    assert(integrated.readyForLilaGameCreationAdapter)
    assert(!integrated.coachingMayRender)
    assertEquals(integrated.matchedCandidate, Some(blackPrepared.record))
    assertEquals(integrated.contractSource, Some(MatchContractSource.Human))
    assertEquals(integrated.contract.map(_.whitePlayerId), Some("white-player"))
    assertEquals(integrated.contract.map(_.blackPlayerId), Some("black-player"))
    assertEquals(integrated.contract.flatMap(_.gameId), None)
    assert(integrated.contract.exists(_.valid))
    assert(integrated.auditRecord.exists(_.valid))
    assert(integrated.normalLichessRatingsExcluded)

  test("game-start policy persists MMR-assigned contract levels instead of queued level hints"):
    val repo = new InMemoryPlaySearchRepository
    val whitePrepared = SearchStartService.prepare(request("white-contract-ticket", "white-contract-player"), repo).toOption.get
    val blackPrepared = SearchStartService.prepare(request("black-contract-ticket", "black-contract-player"), repo).toOption.get
    val blackRecord =
      blackPrepared.record.copy(
        ticket = blackPrepared.record.ticket.copy(
          ecr = blackPrepared.record.ticket.ecr.copy(rating = 1570)
        )
      )
    val policyRepo = new GamePolicy.InMemoryGamePolicyRepository
    repo.put(blackRecord)

    val integrated =
      MatchmakingIntegrationService.evaluate(whitePrepared.record.ticket.ticketId, repo, now + 1).toOption.get
    val contract = integrated.contract.getOrElse(fail("MMR should assign a contract"))
    val gameStart =
      GameStartService.persistBeforeCoaching(
        gameId = "game-contract-levels",
        white = whitePrepared.record,
        black = blackRecord,
        stage = SearchStage.Initial,
        uiConfirmedLevelContract = true,
        policyRepository = policyRepo,
        now = now + 2,
        assignedContract = Some(contract)
      )

    assert(contract.whiteSetLevel != whitePrepared.record.ticket.setLevel || contract.blackSetLevel != blackRecord.ticket.setLevel)
    assert(gameStart.toOption.exists(_.valid))
    assertEquals(gameStart.toOption.map(_.policyRecord.white.setLevel.value), Some(contract.whiteSetLevel.value))
    assertEquals(gameStart.toOption.map(_.policyRecord.black.setLevel.value), Some(contract.blackSetLevel.value))
    assertEquals(gameStart.toOption.map(_.confirmation.whiteSetLevel), Some(contract.whiteSetLevel))
    assertEquals(gameStart.toOption.map(_.confirmation.blackSetLevel), Some(contract.blackSetLevel))

  test("matchmaking integration applies preferred own set level without changing mode"):
    val repo = new InMemoryPlaySearchRepository
    val preferredForm = PlayForm.default.copy(
      setLevel = Level(4),
      applyPreferences = true,
      preferredSetLevel = Some(Level(4))
    )
    val adjacentForm = PlayForm.default.copy(setLevel = Level(5))
    val preferredPrepared =
      SearchStartService.prepare(request("preferred-ticket", "preferred-player", preferredForm), repo).toOption.get
    val adjacentPrepared =
      SearchStartService.prepare(request("adjacent-ticket", "adjacent-player", adjacentForm), repo).toOption.get
    repo.put(
      adjacentPrepared.record.copy(
        ticket = adjacentPrepared.record.ticket.copy(
          ecr = adjacentPrepared.record.ticket.ecr.copy(rating = 1700)
        )
      )
    )

    val integrated =
      MatchmakingIntegrationService.evaluate(preferredPrepared.record.ticket.ticketId, repo, now + 1).toOption.get

    assert(integrated.valid)
    assert(integrated.matched)
    assert(integrated.readyForLilaGameCreationAdapter)
    assertEquals(integrated.request.mode, PlayMode.RatedEvenChess)
    assert(integrated.contract.exists(_.whiteSetLevel == Level(4)))
    assert(integrated.contract.exists(_.preferenceFlags.requesterPreferredLevelMatched))

  test("matchmaking integration can expose widened preferred set level contracts without changing mode"):
    val repo = new InMemoryPlaySearchRepository
    val preferredForm = PlayForm.default.copy(
      setLevel = Level(4),
      applyPreferences = true,
      preferredSetLevel = Some(Level(4))
    )
    val adjacentForm = PlayForm.default.copy(setLevel = Level(5))
    val requestPrepared =
      SearchStartService.prepare(request("preferred-wide-ticket", "preferred-wide-player", preferredForm), repo).toOption.get
    val adjacentPrepared =
      SearchStartService.prepare(request("adjacent-candidate", "adjacent-candidate-player", adjacentForm), repo).toOption.get
    repo.put(
      adjacentPrepared.record.copy(
        ticket = adjacentPrepared.record.ticket.copy(
          ecr = adjacentPrepared.record.ticket.ecr.copy(rating = 1700)
        )
      )
    )

    val integrated =
      MatchmakingIntegrationService.evaluate(requestPrepared.record.ticket.ticketId, repo, now + 1).toOption.get

    assert(integrated.valid)
    assert(integrated.matched)
    assertEquals(integrated.request.mode, PlayMode.RatedEvenChess)
    assertEquals(integrated.contract.map(_.stage), Some(SearchStage.Widening3))
    assert(integrated.contract.exists(_.whiteSetLevel == Level(4)))
    assert(!integrated.contract.exists(_.preferenceFlags.unevenMatch))
    assert(integrated.visibleStatus.contains("Search window widened"))

  test("bot mode config normalizes settings input from env-like values"):
    val explicitRated =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "rated",
        timeoutSeconds = 30
      )
    assertEquals(explicitRated.scope, BotModeScope.RatedOnly)
    assertEquals(explicitRated.timeoutSeconds, 30)

    val invalidScopeDefaultsToBoth =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "not-a-scope",
        timeoutSeconds = 30
      )
    assertEquals(invalidScopeDefaultsToBoth.scope, BotModeConfig.defaultScope)

    val localTestTimeout =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "both",
        timeoutSeconds = 1
      )
    assertEquals(localTestTimeout.timeoutSeconds, 1)

    val clampedTimeoutDefaults =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "both",
        timeoutSeconds = 0
      )
    assertEquals(clampedTimeoutDefaults.timeoutSeconds, BotModeConfig.defaultTimeoutSeconds)

  test("bot mode does not seed before configured timeout"):
    val repo = new InMemoryPlaySearchRepository
    val requestPrepared =
      SearchStartService.prepare(request(ticketId = "test-ticket", playerId = "test-player"), repo).toOption.get
    val botMode =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "both",
        timeoutSeconds = 60
      )

    val integrated = MatchmakingIntegrationService
      .evaluate(requestPrepared.record.ticket.ticketId, repo, now + 10_000L, botMode)
      .toOption
      .get

    assert(!integrated.botMode.seedAttempted)
    assert(!integrated.botMode.botSeeded)
    assert(!integrated.botMode.botCandidatesVisible)
    assert(!integrated.matched)

  test("simulation bot tickets are visible candidates without fallback timeout"):
    val repo = new InMemoryPlaySearchRepository
    val requestPrepared =
      SearchStartService.prepare(request(ticketId = "simulation-human-ticket", playerId = "simulation-human"), repo).toOption.get
    val simulationProfile =
      LevelBasedMatchmaking.BotMatchProfile
        .fromSeed(
          botId = "ec-sim-1-bot",
          userRef = Some("evenbot1"),
          timeControl = Some(TimeControlBucket.Rapid),
          pool = EcrPool.Rapid
        )
        .copy(
          targetEcr = requestPrepared.record.ticket.ecr.rating,
          preferredSetLevel = requestPrepared.record.ticket.setLevel,
          stockfishLevel = LevelBasedMatchmaking.LichessEquivalentStockfishLevel.levelForRating(requestPrepared.record.ticket.ecr.rating)
        )
    val simulationPrepared =
      SearchStartService
        .prepare(
          request(
            ticketId = "ec-sim-1",
            playerId = "evenbot1",
            botProfile = Some(simulationProfile)
          ),
          repo
        )
        .toOption
        .get
    val botModeDisabled =
      BotModeConfig.fromSettings(
        enabled = false,
        scope = "both",
        timeoutSeconds = 60
      )

    val integrated =
      MatchmakingIntegrationService.evaluate(requestPrepared.record.ticket.ticketId, repo, now + 1_000L, botModeDisabled).toOption.get

    assert(integrated.valid)
    assert(integrated.matched)
    assertEquals(integrated.matchedCandidate, Some(simulationPrepared.record))
    assertEquals(integrated.contractSource, Some(MatchContractSource.Bot))
    assert(!integrated.botMode.seedAttempted)
    assert(!repo.get(s"ec-bot-${requestPrepared.record.ticket.ticketId}-seed").isDefined)

  test("simulation seeding creates same-pool casual blitz candidates for quick search"):
    val repo = new InMemoryPlaySearchRepository
    val controls =
      AdminBackendSettings.BotSimulationControls(
        enabled = true,
        scope = "both",
        botCount = 12,
        ratingMin = 1450,
        ratingMax = 1550,
        levelMin = 5,
        levelMax = 5,
        persona = "human-like",
        accountRoster = (1 to 12).map(index => s"evenbot$index").mkString(",")
      )
    val config = BotOperations.BotSimulationConfig.fromSettings(controls)
    val runtime =
      BotOperations.BotSimulationRuntimeState.empty.copy(running = true, revision = 11L, startedAt = Some(now))
    val seed = BotOperations.seedSimulation(repo, config, runtime, now)
    val blitz = TimeControlOptions.fromKey("blitz").getOrElse(TimeControlOptions.default)
    val humanForm =
      PlayForm.default.copy(
        mode = PlayMode.CasualEvenChess,
        timeControl = blitz,
        setLevel = Level(5),
        confirmsOutsideHelpRule = true,
        confirmsLevelContract = true
      )
    val requestPrepared =
      SearchStartService
        .prepare(request(ticketId = "casual-blitz-human", playerId = "casual-blitz-human", form = humanForm), repo)
        .toOption
        .get
    val botModeDisabled = BotModeConfig.fromSettings(enabled = false, scope = "both", timeoutSeconds = 60)
    val integrated =
      MatchmakingIntegrationService.evaluate(requestPrepared.record.ticket.ticketId, repo, now + 1_000L, botModeDisabled).toOption.get

    assert(seed.valid)
    assert(BotOperations.activeSimulationTickets(repo).exists(record =>
      record.mode == PlayMode.CasualEvenChess && record.ticket.poolKey.timeControl == TimeControlBucket.Blitz
    ))
    assert(integrated.valid)
    assert(integrated.matched)
    assert(integrated.matchedCandidate.exists(BotOperations.isSimulationRecord))
    assertEquals(integrated.contractSource, Some(MatchContractSource.Bot))
    assert(!integrated.botMode.seedAttempted)

  test("small simulation batches include mid-rating rated blitz candidates"):
    val repo = new InMemoryPlaySearchRepository
    val controls =
      AdminBackendSettings.BotSimulationControls(
        enabled = true,
        scope = "both",
        botCount = 24,
        ratingMin = 900,
        ratingMax = 2100,
        levelMin = 0,
        levelMax = 10,
        persona = "human-like",
        timeControls = "bullet,blitz,rapid,classical",
        accountRoster = (1 to 24).map(index => s"evenbot$index").mkString(",")
      )
    val config = BotOperations.BotSimulationConfig.fromSettings(controls)
    val runtime =
      BotOperations.BotSimulationRuntimeState.empty.copy(running = true, revision = 12L, startedAt = Some(now))
    val seed = BotOperations.seedSimulation(repo, config, runtime, now)
    val blitz = TimeControlOptions.fromKey("blitz").getOrElse(TimeControlOptions.default)
    val humanForm =
      PlayForm.default.copy(
        mode = PlayMode.RatedEvenChess,
        timeControl = blitz,
        setLevel = Level(5),
        confirmsOutsideHelpRule = true,
        confirmsLevelContract = true
      )
    val requestPrepared =
      SearchStartService
        .prepare(request(ticketId = "rated-blitz-human", playerId = "rated-blitz-human", form = humanForm), repo)
        .toOption
        .get
    val botModeDisabled = BotModeConfig.fromSettings(enabled = false, scope = "both", timeoutSeconds = 60)
    val integrated =
      MatchmakingIntegrationService.evaluate(requestPrepared.record.ticket.ticketId, repo, now + 1_000L, botModeDisabled).toOption.get

    assert(seed.valid)
    assert(BotOperations.activeSimulationTickets(repo).exists(record =>
      record.mode == PlayMode.RatedEvenChess &&
        record.ticket.poolKey.timeControl == TimeControlBucket.Blitz &&
        record.ticket.ecr.rating >= 1450 &&
        record.ticket.ecr.rating <= 1550
    ))
    assert(integrated.valid)
    assert(integrated.matched)
    assert(integrated.matchedCandidate.exists(BotOperations.isSimulationRecord))
    assertEquals(integrated.contractSource, Some(MatchContractSource.Bot))
    assert(!integrated.botMode.seedAttempted)

  test("bot mode uses the generated shared roster when saved roster is blank"):
    val repo = new InMemoryPlaySearchRepository
    val requestPrepared =
      SearchStartService.prepare(request(ticketId = "test-ticket", playerId = "test-player"), repo).toOption.get
    val botMode =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "both",
        timeoutSeconds = 5
      )

    val integrated = MatchmakingIntegrationService
      .evaluate(requestPrepared.record.ticket.ticketId, repo, now + 6_000L, botMode)
      .toOption
      .get

    assert(integrated.botMode.seedAttempted)
    assert(integrated.botMode.botSeeded)
    assert(integrated.botMode.botCandidatesVisible)
    assert(integrated.matched)
    assertEquals(integrated.contractSource, Some(MatchContractSource.Bot))
    val botSeed = repo
      .get(s"ec-bot-${requestPrepared.record.ticket.ticketId}-seed")
      .getOrElse(fail("generated roster bot seed should exist after match attempt"))
    assert(BotOperations.BotAccountRoster.generatedDefault.contains(botSeed.ticket.playerId))
    assertEquals(botSeed.ticket.botProfile.flatMap(_.userRef), Some(botSeed.ticket.playerId))

  test("bot mode can seed a roster-backed fallback candidate for human-style game creation"):
    val repo = new InMemoryPlaySearchRepository
    val clock = LevelBasedMatchmaking.RequestedClock(limitSeconds = 300, incrementSeconds = 5)
    val form = PlayForm.default.copy(timeControl = TimeControlOptions.fromKey("blitz").get, requestedClock = Some(clock))
    val requestPrepared =
      SearchStartService.prepare(request(ticketId = "roster-ticket", playerId = "human-player", form = form), repo).toOption.get
    val botMode =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "both",
        timeoutSeconds = 5,
        accountRoster = "evenbot1, human-player"
      )

    val integrated =
      MatchmakingIntegrationService.evaluate(requestPrepared.record.ticket.ticketId, repo, now + 6_000L, botMode).toOption.get
    val botSeed = repo
      .get(s"ec-bot-${requestPrepared.record.ticket.ticketId}-seed")
      .getOrElse(fail("bot seed should exist after match attempt"))

    assert(integrated.matched)
    assertEquals(botSeed.ticket.playerId, "evenbot1")
    assertEquals(botSeed.ticket.ecr.playerId, "evenbot1")
    assertEquals(botSeed.ticket.botProfile.flatMap(_.userRef), Some("evenbot1"))
    assertEquals(botSeed.ticket.requestedClock, Some(clock))
    assert(botSeed.ticket.isBotTicket)

  test("bot fallback preserves requester preferred set level and assigns the bot level from MMR"):
    val repo = new InMemoryPlaySearchRepository
    val clock = LevelBasedMatchmaking.RequestedClock(limitSeconds = 300, incrementSeconds = 0)
    val preferredForm = PlayForm.default.copy(
      timeControl = TimeControlOptions.fromKey("blitz").get,
      applyPreferences = true,
      preferredSetLevel = Some(Level(6)),
      requestedClock = Some(clock)
    )
    val requestPrepared =
      SearchStartService
        .prepare(request(ticketId = "preferred-bot-ticket", playerId = "human-player", form = preferredForm), repo)
        .toOption
        .get
    val botMode =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "both",
        timeoutSeconds = 1,
        accountRoster = "evenbot2"
      )

    val integrated =
      MatchmakingIntegrationService.evaluate(requestPrepared.record.ticket.ticketId, repo, now + 2_000L, botMode).toOption.get
    val contract = integrated.contract.getOrElse(fail("bot fallback should produce a match contract"))
    val humanLevel =
      if contract.whitePlayerId == "human-player" then contract.whiteSetLevel else contract.blackSetLevel
    val botLevel =
      if contract.whitePlayerId == "human-player" then contract.blackSetLevel else contract.whiteSetLevel
    val botSeed = repo
      .get(s"ec-bot-${requestPrepared.record.ticket.ticketId}-seed")
      .getOrElse(fail("bot seed should exist after preferred fallback match attempt"))

    assert(integrated.matched)
    assertEquals(integrated.contractSource, Some(MatchContractSource.Bot))
    assertEquals(humanLevel, Level(6))
    assert(Level.isValid(botLevel.value))
    assertEquals(botSeed.ticket.playerId, "evenbot2")
    assertEquals(botSeed.ticket.botProfile.flatMap(_.userRef), Some("evenbot2"))
    assertEquals(botSeed.ticket.requestedClock, Some(clock))
    assert(contract.preferenceFlags.requesterPreferredLevelMatched)

  test("bot mode disabled prevents bot seeding regardless of timeout"):
    val repo = new InMemoryPlaySearchRepository
    val requestPrepared =
      SearchStartService.prepare(request(ticketId = "disabled-ticket", playerId = "disabled-player"), repo).toOption.get
    val botMode =
      BotModeConfig.fromSettings(
        enabled = false,
        scope = "both",
        timeoutSeconds = 5
      )

    val integrated =
      MatchmakingIntegrationService.evaluate(requestPrepared.record.ticket.ticketId, repo, now + 6_000L, botMode).toOption.get

    assert(!integrated.botMode.seedAttempted)
    assert(!integrated.botMode.botSeeded)
    assert(!integrated.botMode.botCandidatesVisible)
    assertEquals(
      repo.get(s"ec-bot-${requestPrepared.record.ticket.ticketId}-seed").isEmpty,
      true
    )

  test("bot mode scope prevents seeds outside configured queue"):
    val repo = new InMemoryPlaySearchRepository
    val requestPrepared =
      SearchStartService.prepare(
        request(
          ticketId = "casual-ticket",
          playerId = "casual-player",
          form = PlayForm.default.copy(mode = PlayMode.CasualEvenChess)
        ),
        repo
      ).toOption
      .get
    val ratedOnly =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "rated",
        timeoutSeconds = 5
      )
    val integrated =
      MatchmakingIntegrationService.evaluate(requestPrepared.record.ticket.ticketId, repo, now + 6_000L, ratedOnly).toOption.get

    assert(!integrated.botMode.seedAttempted)
    assert(!integrated.botMode.botSeeded)
    assert(!integrated.matched)
    assert(repo.get(s"ec-bot-${requestPrepared.record.ticket.ticketId}-seed").isEmpty)

    val both =
      BotModeConfig.fromSettings(
        enabled = true,
        scope = "both",
        timeoutSeconds = 5,
        accountRoster = "evenbot1"
      )
    val integratedBoth =
      MatchmakingIntegrationService.evaluate(requestPrepared.record.ticket.ticketId, repo, now + 6_000L, both).toOption.get

    assert(integratedBoth.botMode.seedAttempted)
    assert(integratedBoth.botMode.botSeeded)
    assert(integratedBoth.botMode.botCandidatesVisible)
    assert(integratedBoth.matched)
    assert(integratedBoth.matchedCandidate.exists(_.ticket.playerId == "evenbot1"))
    assertEquals(
      repo.get(s"ec-bot-${requestPrepared.record.ticket.ticketId}-seed").exists(_.ticket.isBotTicket),
      true
    )

  test("token state gates access only and does not alter fairness fields"):
    val repo = new InMemoryPlaySearchRepository
    val emptyTokens = TokenSnapshot.emptyFreeAccount
    val denied = SearchStartService.prepare(request(tokenSnapshot = emptyTokens), repo)
    val subscribed = emptyTokens.copy(subscriptionActive = true, source = "test-subscription")
    val allowed = SearchStartService.prepare(request(ticketId = "subscribed", tokenSnapshot = subscribed), repo)

    assert(denied.isLeft)
    assert(denied.swap.toOption.exists(_.contains("game_token_required")))
    assert(allowed.toOption.exists(_.record.tokenSnapshot.fairnessNeutral))
    assert(allowed.toOption.exists(_.record.ticket.setLevel == Level(5)))
    assert(allowed.toOption.exists(_.record.ticket.ecr.pool == EcrPool.Rapid))

  test("target level search is isolated from normal ECR and normal Lichess ratings"):
    val targetForm = PlayForm.default.copy(
      mode = PlayMode.TargetLevel,
      targetLevel = Some(Level(7)),
      confirmsLevelContract = true
    )
    val repo = new InMemoryPlaySearchRepository
    val prepared = SearchStartService.prepare(request(form = targetForm), repo).toOption.get
    val ticket = prepared.record.ticket

    assert(prepared.valid)
    assert(!ticket.rated)
    assertEquals(ticket.poolKey.queue, LevelBasedMatchmaking.SearchQueue.TargetLevel)
    assertEquals(ticket.poolKey.ecrPool, EcrPool.TargetShadow)
    assertEquals(ticket.targetLevel, Some(Level(7)))
    assert(!ticket.poolKey.queue.updatesNormalEcr)

  test("game-start metadata must be persisted before coaching can render"):
    val white = prepared("white-ticket", "white-player")
    val black = prepared("black-ticket", "black-player")
    val policyRepo = new GamePolicy.InMemoryGamePolicyRepository

    assert(!CoachingRenderGate.mayRender("game-1", policyRepo))

    val gameStart =
      GameStartService.persistBeforeCoaching(
        gameId = "game-1",
        white = white,
        black = black,
        stage = SearchStage.Initial,
        uiConfirmedLevelContract = false,
        policyRepository = policyRepo,
        now = now + 1
      )

    assert(gameStart.toOption.exists(_.valid))
    assert(CoachingRenderGate.mayRender("game-1", policyRepo))
    assert(gameStart.toOption.exists(_.policyRecord.rated))
    assert(gameStart.toOption.exists(_.policyRecord.white.poolKey == white.ticket.poolKey.key))
    assert(gameStart.toOption.exists(_.telemetry.readyForRatedLedger))

  test("game-start metadata accepts server-assigned rated or casual set levels"):
    val white = prepared("white-ticket", "white-player", Level(4))
    val black = prepared("black-ticket", "black-player", Level(5))
    val policyRepo = new GamePolicy.InMemoryGamePolicyRepository

    val unconfirmed =
      GameStartService.persistBeforeCoaching(
        gameId = "game-2",
        white = white,
        black = black,
        stage = SearchStage.Widening3,
        uiConfirmedLevelContract = false,
        policyRepository = policyRepo,
        now = now + 1
      )
    val confirmed =
      GameStartService.persistBeforeCoaching(
        gameId = "game-3",
        white = white,
        black = black,
        stage = SearchStage.Widening3,
        uiConfirmedLevelContract = true,
        policyRepository = policyRepo,
        now = now + 1
      )

    assert(unconfirmed.toOption.exists(_.valid))
    assert(confirmed.toOption.exists(_.valid))
    assert(CoachingRenderGate.mayRender("game-2", policyRepo))
    assert(CoachingRenderGate.mayRender("game-3", policyRepo))
