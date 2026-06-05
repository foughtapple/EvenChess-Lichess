package lila.evenchess

class EngineGatewayTest extends munit.FunSuite:

  import CoachingLadder.Level
  import CoachingOverlays.Perspective
  import EngineGateway.*
  import EvenChessMode.TimeControlBucket
  import ProductInvariants.RequirementClass

  private val limits =
    EngineLimitRequest(
      depth = 12,
      nodes = 200_000,
      movetimeMillis = 900,
      multiPv = 1,
      threads = 1,
      hashMb = 32
    )

  private val request =
    EngineJobRequest(
      requestId = "request-1",
      gameId = "game-1",
      playerId = "player-1",
      boardStateKey = "fen-hash-1",
      ply = 17,
      perspective = Perspective.White,
      requestedFeature = "single-hint",
      setLevel = Level(5),
      requestedLevel = Level(5),
      timeControl = TimeControlBucket.Rapid,
      queue = EngineQueue.Live,
      limits = limits,
      policyVersion = "engine-policy-v1",
      cancellationToken = "cancel-1",
      cacheKey = "cache-1"
    )

  private val audit =
    EngineAuditMetadata(
      jobId = "job-1",
      policyVersion = "engine-policy-v1",
      profile = request.profile,
      engineVersion = "Stockfish 17",
      engineSource = "https://stockfishchess.org",
      engineBinaryHash = "sha256:abc123"
    )

  private val safePacket =
    EngineTruthPacket(
      jobId = "job-1",
      gameId = "game-1",
      playerId = "player-1",
      boardStateKey = "fen-hash-1",
      ply = 17,
      requestedFeature = "single-hint",
      candidates = List(EngineCandidate("e2e4", rank = 1, scoreCp = None, wdl = None, linePlyCount = 0, proof = None)),
      numericEvalLabel = None,
      status = EngineJobStatus.Ready,
      stale = false,
      degraded = false,
      fallback = false,
      audit = audit,
      rawEnginePayload = None,
      hiddenDebugData = None
    )

  test("Appendix L requirements are classified before implementation"):
    val byRequirement =
      EngineRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(EngineRequirement.InternalGateway), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.LevelBoundedProfiles), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.GplLicensePreserved), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(EngineRequirement.ReuseLilaEngineSeamsFirst), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(EngineRequirement.SeparateEceServiceBoundary), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceServerToServerOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceHealthAndBoardEndpoints), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceBoardStateRequestContract), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceSideGatedResponseContract), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceStalePayloadRejection), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceUnavailableIsNonFatal), RequirementClass.LichessProvided)
    assertEquals(byRequirement(EngineRequirement.EceProposedMoveRequestContract), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceProposedMoveCurrentFenAndSideToMove), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceProposedMovePreviewDistinct), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceFullGameReviewRequestContract), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceFullGameReviewPostGameOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceFullGameReviewDoesNotMutateSettlement), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceProviderBoundaryNoDirectCalls), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceProviderStatusSanitized), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(EngineRequirement.EceNormalizedProviderOutputOnly), RequirementClass.EvenChessSpecific)

  test("live Stockfish access is internal-only and clients receive bounded truth packets"):
    assert(EngineEndpointPolicy.internalGatewayRequired)
    assert(!EngineEndpointPolicy.browserMayRequestLiveRatedStockfish)
    assert(!EngineEndpointPolicy.debugEndpointMayExposeRawStockfish)
    assert(EngineEndpointPolicy.clientsReceiveOnlyBoundedTruthPackets)

  test("L0-L4 have no live engine candidate profile"):
    (0 to 4).foreach: value =>
      val profile = EngineProfiles.forLevel(Level(value), TimeControlBucket.Rapid)
      assert(!profile.liveEngineCandidateAllowed)
      assertEquals(profile.maxCandidates, 0)
      assertEquals(profile.maxMultiPv, 0)

  test("L5-L10 engine profiles bound candidate counts and eval disclosure"):
    assertEquals(EngineProfiles.forLevel(Level(5), TimeControlBucket.Rapid).maxCandidates, 1)
    assertEquals(EngineProfiles.forLevel(Level(6), TimeControlBucket.Rapid).maxCandidates, 2)
    assertEquals(EngineProfiles.forLevel(Level(7), TimeControlBucket.Rapid).maxCandidates, 3)
    assertEquals(EngineProfiles.forLevel(Level(8), TimeControlBucket.Rapid).maxCandidates, 3)
    assertEquals(EngineProfiles.forLevel(Level(9), TimeControlBucket.Rapid).maxCandidates, 4)
    assertEquals(EngineProfiles.forLevel(Level(10), TimeControlBucket.Rapid).maxCandidates, 4)
    assert(!EngineProfiles.forLevel(Level(7), TimeControlBucket.Rapid).allowNumericEvalOrWdl)
    assert(EngineProfiles.forLevel(Level(8), TimeControlBucket.Rapid).allowNumericEvalOrWdl)
    assert(EngineProfiles.forLevel(Level(8), TimeControlBucket.Rapid).approximateEvalLabelRequired)

  test("engine job requests require Set Level authorization and validated limits"):
    assert(request.acceptedByGateway)
    assert(!request.copy(requestedLevel = Level(6)).acceptedByGateway)
    assert(!request.copy(limits = limits.copy(multiPv = 2)).acceptedByGateway)
    assert(!request.copy(cancellationToken = "").acceptedByGateway)

  test("truth packets are safe for clients only when bounded, auditable, and stripped of raw payloads"):
    assert(safePacket.safeForClient)

    val tooManyCandidates = safePacket.copy(candidates = List(
      EngineCandidate("e2e4", 1, None, None, 0, None),
      EngineCandidate("d2d4", 2, None, None, 0, None)
    ))
    val rawPayload = safePacket.copy(rawEnginePayload = Some("info depth 20 pv e2e4 e7e5"))

    assert(!tooManyCandidates.safeForClient)
    assert(!rawPayload.safeForClient)

  test("L8 numeric eval and WDL require an approximate label"):
    val l8Audit = audit.copy(profile = EngineProfiles.forLevel(Level(8), TimeControlBucket.Rapid))
    val evalCandidate = EngineCandidate("e2e4", rank = 1, scoreCp = Some(34), wdl = Some("55/44/1"), linePlyCount = 4, proof = None)
    val missingLabel = safePacket.copy(audit = l8Audit, candidates = List(evalCandidate), numericEvalLabel = None)
    val labelled = missingLabel.copy(numericEvalLabel = Some("Approximate eval"))

    assert(!missingLabel.safeForClient)
    assert(labelled.safeForClient)

  test("live and post-game queues require cache, cancellation, timeout, fallback, and degraded states"):
    assert(QueueOperations.liveQueueRequired)
    assert(QueueOperations.postGameQueueRequired)
    assert(QueueOperations.cachingRequired)
    assert(QueueOperations.cancellationRequired)
    assert(QueueOperations.timeoutRequired)
    assert(QueueOperations.fallbackRequired)
    assert(QueueOperations.degradedStateRequired)
    assertEquals(QueueOperations.statusFromRuntime(cancelled = true, timedOut = false, stale = false, engineHealthy = true), EngineJobStatus.Cancelled)
    assertEquals(QueueOperations.statusFromRuntime(cancelled = false, timedOut = true, stale = false, engineHealthy = true), EngineJobStatus.TimedOut)
    assertEquals(QueueOperations.statusFromRuntime(cancelled = false, timedOut = false, stale = true, engineHealthy = true), EngineJobStatus.Stale)
    assertEquals(QueueOperations.statusFromRuntime(cancelled = false, timedOut = false, stale = false, engineHealthy = false), EngineJobStatus.Degraded)

  test("cache hits require same board context and a safe non-stale packet"):
    assert(QueueOperations.cacheHitAllowed(request, safePacket))
    assert(!QueueOperations.cacheHitAllowed(request.copy(boardStateKey = "other"), safePacket))
    assert(!QueueOperations.cacheHitAllowed(request, safePacket.copy(stale = true)))

  test("engine inventory records version, source, hash, and GPL obligations"):
    val inventory = LicenseInventory(
      engineName = "Stockfish",
      version = "17",
      sourceUrl = "https://stockfishchess.org",
      license = "GPL-3.0",
      binaryHash = "sha256:abc123",
      distributionNoticePresent = true
    )

    assert(inventory.complete)
    assert(inventory.gplObligationsPreserved)
    assert(!inventory.copy(distributionNoticePresent = false).gplObligationsPreserved)

  test("lila engine seams must be inspected before new core engine work"):
    assert(LichessEngineSeamRules.inspectFishnetAnalysisTablebaseBeforeNewService)
    assert(LichessEngineSeamRules.wrapReusableLilaEngineSeamsSafely)
    assert(LichessEngineSeamRules.implementEvenChessGatewayOnlyIfReuseIsUnsafe)
    assert(LichessEngineSeamRules.coreEngineEditsRequirePatchMap)

  test("ECE framework defaults to local server-side health and split board endpoints only"):
    val config = EceServiceConfig()
    val health =
      EceHealthStatus(
        status = "ok",
        service = "EvenChessEngine",
        mode = "mock",
        openAiConfigured = false,
        stockfishConfigured = false
      )

    assert(config.valid)
    assertEquals(config.healthUrl, "http://127.0.0.1:8787/health")
    assertEquals(config.boardQuickUrl, "http://127.0.0.1:8787/v1/ece/board/quick")
    assertEquals(config.boardDeepUrl, "http://127.0.0.1:8787/v1/ece/board/deep")
    assertEquals(config.legacyBoardUrl, "http://127.0.0.1:8787/v1/ece/board")
    assertEquals(config.boardUrl, config.boardQuickUrl)
    assertEquals(config.proposedMoveUrl, "http://127.0.0.1:8787/v1/ece/proposed-move")
    assertEquals(config.gameReviewUrl, "http://127.0.0.1:8787/v1/ece/game-review")
    assertEquals(config.fullMatchUrl, "http://127.0.0.1:8787/v1/ece/full-match")
    assertEquals(config.fullMatchSummaryUrl, "http://127.0.0.1:8787/v1/ece/full-match-summary")
    assert(EceServiceConfig("http://host.docker.internal:8787").valid)
    assertEquals(EceServiceConfig("http://host.docker.internal:8787/").boardQuickUrl, "http://host.docker.internal:8787/v1/ece/board/quick")
    assert(!EceServiceConfig("http://example.com:8787").valid)
    assert(!EceServiceConfig("http://127.0.0.1.evil.test:8787").valid)
    assert(EceEndpointPolicy.valid)
    assert(!EceEndpointPolicy.browserMayCallEceDirectly)
    assert(!EceEndpointPolicy.requestMayContainApiKeys)
    assert(health.reachable)
    assert(health.safeForDiagnostics)

  test("ECE provider awareness blocks direct Stockfish and AI provider calls from Lichess"):
    val statuses =
      EceInternalProvider.values.toList.map: provider =>
        EceProviderStatus(
          provider = provider,
          configured = true,
          reachable = true,
          latencyMillis = Some(25),
          sanitizedLabel = s"$provider-ok",
          exposesSecret = false,
          exposesFilesystemPath = false,
          exposesRawPrompt = false,
          exposesRawProviderOutput = false
        )
    val snapshot =
      EceProviderAwarenessSnapshot(
        statuses = statuses,
        callsEceOnly = true,
        normalizedBeforeDisplay = true
      )

    assert(EceProviderBoundary.valid)
    assert(EceInternalProvider.values.forall(provider => !EceProviderBoundary.directCallAllowed(provider)))
    assert(snapshot.safeForLichess)
    assert(!snapshot.copy(callsEceOnly = false).safeForLichess)
    assert(!snapshot.copy(normalizedBeforeDisplay = false).safeForLichess)
    assert(!snapshot.copy(statuses = List(statuses.head.copy(exposesSecret = true))).safeForLichess)
    assert(!snapshot.copy(statuses = List(statuses.head.copy(exposesFilesystemPath = true))).safeForLichess)
    assert(!snapshot.copy(statuses = List(statuses.head.copy(exposesRawPrompt = true))).safeForLichess)
    assert(!snapshot.copy(statuses = List(statuses.head.copy(exposesRawProviderOutput = true))).safeForLichess)

  test("ECE board-state request sends authoritative quick request with optional deep addendum intent"):
    val request =
      EceBoardStateRequest.boardState(
        gameId = "game-1",
        ply = 18,
        inputFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        whiteEcr = Some(1000),
        blackEcr = Some(1200),
        whiteLevel = Level(4),
        blackLevel = Level(2),
        aiTextAllowed = false
      )
    val envelope = EceFrameworkIntegration.prepareBoardState(EceServiceConfig(), request)

    assert(request.valid)
    assertEquals(request.mode, "board_state")
    assertEquals(request.requestId, "game-1-ply-18-quick")
    assertEquals(request.ratingType, "ecr")
    assertEquals(request.whiteRatingInput, 1000)
    assertEquals(request.blackRatingInput, 1200)
    assertEquals(request.useAi, 0)
    assert(!request.deepRequested)
    assertEquals(request.requestedDeepModules, Nil)
    assertEquals(request.custom, EceCustomProfile.default)
    assert(envelope.exists(_.valid))
    assert(request.cacheKey("ece-v1").contains("ece-v1"))
    assert(!request.copy(useAi = 2).valid)
    assert(!request.copy(custom = EceCustomProfile(opening = 0, instructions = -1)).valid)

    val mixedLevelRequest =
      EceBoardStateRequest.boardState(
        gameId = "game-1",
        ply = 19,
        inputFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        whiteEcr = Some(1000),
        blackEcr = Some(1200),
        whiteLevel = Level(4),
        blackLevel = Level(8),
        aiTextAllowed = false
      )
    val deepRequest = EceBoardDeepRequest.fromQuick(mixedLevelRequest, "ece_ctx_1")

    assert(mixedLevelRequest.deepRequested)
    assert(mixedLevelRequest.requestedDeepModules.contains("stockfish"))
    assert(mixedLevelRequest.requestedDeepModules.contains("lichess_eval_cache"))
    assertEquals(deepRequest.mode, "board_deep")
    assertEquals(deepRequest.requestId, "game-1-ply-19-deep")
    assertEquals(deepRequest.quickRequestId, "game-1-ply-19-quick")
    assertEquals(deepRequest.quickContextId, "ece_ctx_1")
    assert(deepRequest.valid)

  test("bot-driven games use the same ECE quick/deep board bridge as human games"):
    val config = EceServiceConfig()
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
    def participant(id: String, level: Level, ecr: Int, source: EceBridgeGameSource) =
      EceBridgeParticipant(id, level, Some(ecr), source)
    def context(whiteSource: EceBridgeGameSource, blackSource: EceBridgeGameSource) =
      EceBridgeGameContext(
        gameId = "phase-i-game",
        ply = 22,
        inputFen = fen,
        white = participant("white-player", Level(10), 1500, whiteSource),
        black = participant("black-player", Level(8), 1520, blackSource),
        aiTextAllowed = false
      )
    def prepared(ctx: EceBridgeGameContext) =
      EceFrameworkIntegration.prepareBoardStateForGame(config, ctx).fold(error => fail(error), identity)

    val humanEnvelope = prepared(context(EceBridgeGameSource.Human, EceBridgeGameSource.Human))
    val matchmakingBotEnvelope = prepared(context(EceBridgeGameSource.Human, EceBridgeGameSource.MatchmakingBot))
    val simulationBotEnvelope = prepared(context(EceBridgeGameSource.SimulationBot, EceBridgeGameSource.SimulationBot))

    assert(!humanEnvelope.botDriven)
    assert(matchmakingBotEnvelope.botDriven)
    assert(simulationBotEnvelope.botDriven)
    assertEquals(matchmakingBotEnvelope.request, humanEnvelope.request)
    assertEquals(simulationBotEnvelope.request, humanEnvelope.request)
    assertEquals(matchmakingBotEnvelope.request.mode, "board_state")
    assertEquals(config.boardUrl, config.boardQuickUrl)
    assert(matchmakingBotEnvelope.request.deepRequested)
    assert(matchmakingBotEnvelope.request.requestedDeepModules.contains("stockfish"))

    val deepRequest = EceBoardDeepRequest.fromQuick(matchmakingBotEnvelope.request, "phase_i_ctx")
    assert(deepRequest.valid)
    assertEquals(deepRequest.mode, "board_deep")
    assertEquals(deepRequest.quickRequestId, matchmakingBotEnvelope.request.requestId)
    assertEquals(deepRequest.inputFen, matchmakingBotEnvelope.request.inputFen)
    assertEquals(deepRequest.whiteLevel, matchmakingBotEnvelope.request.whiteLevel)
    assertEquals(deepRequest.blackLevel, matchmakingBotEnvelope.request.blackLevel)

    def sideOutput(side: Perspective, level: Level) =
      val opponent = if side == Perspective.White then Perspective.Black else Perspective.White
      EceSideOutput(
        side = side,
        studentSide = side,
        opponentSide = opponent,
        level = EceLevelEcho(level, level, defaulted = false),
        isSideToMove = side == Perspective.Black,
        summary = Some("Accepted through the shared bridge."),
        immediateWarning = None,
        plan = None
      )
    val response =
      EceBoardStateResponse(
        requestEcho =
          EceRequestEcho(
            matchmakingBotEnvelope.request.requestId,
            matchmakingBotEnvelope.request.inputFen,
            matchmakingBotEnvelope.request.whiteLevel,
            matchmakingBotEnvelope.request.blackLevel
          ),
        white = Some(sideOutput(Perspective.White, matchmakingBotEnvelope.request.whiteLevel)),
        black = Some(sideOutput(Perspective.Black, matchmakingBotEnvelope.request.blackLevel)),
        diagnostics = EceDiagnostics(EceDiagnosticsStatus.Ok, engineVersion = "ece-v1.1-phase-i", sanitizedMessage = Some("ready")),
        hasPublicPosition = false,
        hasPublicSharedCalculations = false,
        rawProviderPayload = None
      )
    val outstanding = EceOutstandingRequest(matchmakingBotEnvelope.request, currentFen = fen)

    assert(EceBoardStateValidator.responseUsable(outstanding, response))
    assert(!EceBoardStateValidator.responseUsable(outstanding.copy(currentFen = "stale-fen"), response))
    assert(!EceBoardStateValidator.responseUsable(outstanding, response.copy(hasPublicPosition = true)))
    assert(!EceBoardStateValidator.responseUsable(outstanding, response.copy(rawProviderPayload = Some("raw provider"))))

  test("ECE board-state response is usable only when side-gated, non-stale, and ungated public fields are absent"):
    val request =
      EceBoardStateRequest.boardState(
        gameId = "game-1",
        ply = 18,
        inputFen = "fen-after-ply-18",
        whiteEcr = Some(1000),
        blackEcr = Some(1200),
        whiteLevel = Level(4),
        blackLevel = Level(2),
        aiTextAllowed = false
      )
    val outstanding = EceOutstandingRequest(request, currentFen = "fen-after-ply-18")
    val whiteOutput =
      EceSideOutput(
        side = Perspective.White,
        studentSide = Perspective.White,
        opponentSide = Perspective.Black,
        level = EceLevelEcho(Level(4), Level(4), defaulted = false),
        isSideToMove = true,
        summary = Some("White can improve development."),
        immediateWarning = None,
        plan = Some("Castle soon.")
      )
    val blackOutput =
      EceSideOutput(
        side = Perspective.Black,
        studentSide = Perspective.Black,
        opponentSide = Perspective.White,
        level = EceLevelEcho(Level(2), Level(2), defaulted = false),
        isSideToMove = false,
        summary = Some("Black should finish development."),
        immediateWarning = None,
        plan = None
      )
    val response =
      EceBoardStateResponse(
        requestEcho = EceRequestEcho(request.requestId, request.inputFen, Level(4), Level(2)),
        white = Some(whiteOutput),
        black = Some(blackOutput),
        diagnostics = EceDiagnostics(EceDiagnosticsStatus.Ok, engineVersion = "ece-v1", sanitizedMessage = Some("ready")),
        hasPublicPosition = false,
        hasPublicSharedCalculations = false,
        rawProviderPayload = None
      )
    val accepted =
      EceFrameworkIntegration.acceptBoardState(EceServiceConfig(), outstanding, response, requesterSide = Perspective.White)

    assert(EceBoardStateValidator.responseUsable(outstanding, response))
    assertEquals(EceBoardStateValidator.displayForSide(response, Perspective.White), Some(whiteOutput))
    assertEquals(EceBoardStateValidator.displayForSide(response, Perspective.Black), Some(blackOutput))
    assert(accepted.valid)
    assert(accepted.coachingAvailable)
    assertEquals(accepted.displayableForRequester, Some(whiteOutput))
    assert(!EceBoardStateValidator.responseUsable(outstanding, response.copy(hasPublicPosition = true)))
    assert(!EceBoardStateValidator.responseUsable(outstanding, response.copy(hasPublicSharedCalculations = true)))
    assert(!EceBoardStateValidator.responseUsable(outstanding, response.copy(rawProviderPayload = Some("raw stockfish"))))
    assert(!EceBoardStateValidator.responseUsable(outstanding.copy(currentFen = "new-fen"), response))
    assert(!EceBoardStateValidator.responseUsable(outstanding, response.copy(requestEcho = response.requestEcho.copy(whiteLevel = Level(5)))))

  test("ECE unavailable is non-fatal to Lichess game lifecycle"):
    val decision =
      EceFrameworkIntegration.unavailable(
        EceServiceConfig(),
        Some(EceHealthStatus("down", "EvenChessEngine", "mock", openAiConfigured = false, stockfishConfigured = false))
      )

    assert(decision.valid)
    assert(!decision.coachingAvailable)
    assertEquals(decision.status, EngineJobStatus.Degraded)
    assert(decision.browserDirectCallBlocked)
    assert(decision.nonFatalToGameLifecycle)

  test("ECE proposed-move request sends exactly one UCI move and stays server-side"):
    val request =
      EceProposedMoveRequest.proposedMove(
        gameId = "game-1",
        ply = 18,
        proposalIndex = 1,
        inputFen = "fen-before-proposal",
        proposedMoveUci = "g1f3",
        whiteEcr = Some(1000),
        blackEcr = Some(1200),
        whiteLevel = Level(10),
        blackLevel = Level(10),
        aiTextAllowed = false
      )
    val envelope = EceFrameworkIntegration.prepareProposedMove(EceServiceConfig(), request)

    assert(request.valid)
    assertEquals(request.mode, "proposed_move")
    assertEquals(request.requestId, "game-1-ply-18-pm-1")
    assertEquals(request.proposedMoveUci, "g1f3")
    assertEquals(request.ratingType, "ecr")
    assertEquals(request.useAi, 0)
    assertEquals(request.custom, EceCustomProfile.default)
    assert(request.cacheKey("ece-v1").contains("g1f3"))
    assert(envelope.exists(_.valid))
    assert(!request.copy(proposedMoveUci = "").valid)
    assert(!request.copy(proposedMoveUci = "g1f3 e7e5").valid)
    assert(!request.copy(proposedMoveUci = "z9z1").valid)

  test("ECE proposed-move response requires current FEN, side to move, echo match, and preview-only acceptance"):
    val request =
      EceProposedMoveRequest.proposedMove(
        gameId = "game-1",
        ply = 18,
        proposalIndex = 1,
        inputFen = "fen-before-proposal",
        proposedMoveUci = "g1f3",
        whiteEcr = Some(1000),
        blackEcr = Some(1200),
        whiteLevel = Level(10),
        blackLevel = Level(10),
        aiTextAllowed = false
      )
    val outstanding =
      EceProposedMoveOutstandingRequest(
        request = request,
        currentFen = "fen-before-proposal",
        requesterSide = Perspective.White,
        sideToMove = Perspective.White,
        proposedMoveHelpAllowed = true
      )
    val whiteOutput =
      EceSideOutput(
        side = Perspective.White,
        studentSide = Perspective.White,
        opponentSide = Perspective.Black,
        level = EceLevelEcho(Level(10), Level(10), defaulted = false),
        isSideToMove = true,
        summary = Some("Preview: Nf3 develops and protects h4."),
        immediateWarning = None,
        plan = Some("Prepare to castle.")
      )
    val blackOutput =
      EceSideOutput(
        side = Perspective.Black,
        studentSide = Perspective.Black,
        opponentSide = Perspective.White,
        level = EceLevelEcho(Level(10), Level(10), defaulted = false),
        isSideToMove = false,
        summary = Some("Black preview remains gated to black."),
        immediateWarning = None,
        plan = None
      )
    val response =
      EceProposedMoveResponse(
        requestEcho = EceProposedMoveRequestEcho(request.requestId, request.inputFen, "g1f3", Level(10), Level(10)),
        white = Some(whiteOutput),
        black = Some(blackOutput),
        diagnostics = EceDiagnostics(EceDiagnosticsStatus.Ok, engineVersion = "ece-v1", sanitizedMessage = Some("ready")),
        hasPublicPosition = false,
        hasPublicSharedCalculations = false,
        rawProviderPayload = None
      )
    val accepted =
      EceProposedMoveFrameworkIntegration.acceptProposedMove(EceServiceConfig(), outstanding, response, Perspective.White)

    assert(EceProposedMoveValidator.responseUsable(outstanding, response))
    assert(accepted.valid)
    assert(accepted.previewAvailable)
    assert(accepted.previewOnly)
    assertEquals(accepted.displayableForRequester, Some(whiteOutput))
    assert(!EceProposedMoveValidator.responseUsable(outstanding.copy(currentFen = "new-fen"), response))
    assert(!EceProposedMoveValidator.responseUsable(outstanding.copy(sideToMove = Perspective.Black), response))
    assert(!EceProposedMoveValidator.responseUsable(outstanding.copy(proposedMoveHelpAllowed = false), response))
    assert(!EceProposedMoveValidator.responseUsable(outstanding, response.copy(requestEcho = response.requestEcho.copy(proposedMoveUci = "d2d4"))))
    assert(!EceProposedMoveValidator.responseUsable(outstanding, response.copy(hasPublicPosition = true)))

  test("ECE full-game review request carries whole-game input, snapshots, review level, and token-gated preparation"):
    val game =
      EceGameReviewInput(
        gameId = "game-1",
        initialFen = "startpos",
        pgn = Some("1. e4 e5 2. Nf3"),
        moves = List("e2e4", "e7e5", "g1f3"),
        fenHistory = List("startpos", "fen-1", "fen-2", "fen-3"),
        result = "1-0",
        termination = "checkmate"
      )
    val snapshots = List(
      EceLiveSnapshotRef(0, "startpos", Perspective.White, Some("white-0"), Some("black-0")),
      EceLiveSnapshotRef(1, "fen-1", Perspective.Black, Some("white-1"), Some("black-1"))
    )
    val request =
      EceGameReviewRequest.gameReview(
        gameId = "game-1",
        reviewIndex = 1,
        game = game,
        whiteEcr = Some(1000),
        blackEcr = Some(1200),
        reviewLevel = Level(10),
        aiNarrativeAllowed = true,
        liveEceSnapshots = snapshots
      )
    val prepared = EceFrameworkIntegration.prepareGameReview(EceServiceConfig(), request, tokenQuotaChecked = true)
    val blocked = EceFrameworkIntegration.prepareGameReview(EceServiceConfig(), request, tokenQuotaChecked = false)

    assert(game.valid)
    assert(request.valid)
    assertEquals(request.mode, "full_match")
    assertEquals(request.requestId, "game-1-review-1")
    assertEquals(request.ratingType, "ecr")
    assertEquals(request.reviewLevel, Level(10))
    assertEquals(request.useAi, 1)
    assertEquals(request.liveEceSnapshots.size, 2)
    assert(request.cacheKey("ece-v1").contains("game-1"))
    assert(prepared.exists(_.valid))
    assert(blocked.isLeft)
    assert(!request.copy(game = game.copy(fenHistory = Nil)).valid)
    assert(!request.copy(useAi = 2).valid)

  test("ECE full-game review response is post-game-only and cannot mutate live settlement"):
    val game =
      EceGameReviewInput(
        gameId = "game-1",
        initialFen = "startpos",
        pgn = None,
        moves = List("e2e4", "e7e5"),
        fenHistory = List("startpos", "fen-1", "fen-2"),
        result = "1-0",
        termination = "resignation"
      )
    val request =
      EceGameReviewRequest.gameReview(
        gameId = "game-1",
        reviewIndex = 1,
        game = game,
        whiteEcr = Some(1000),
        blackEcr = Some(1200),
        reviewLevel = Level(10),
        aiNarrativeAllowed = true,
        liveEceSnapshots = Nil
      )
    val outstanding =
      EceGameReviewOutstandingRequest(
        request = request,
        postGameOnly = true,
        tokenQuotaChecked = true
      )
    val whiteOutput =
      EceSideOutput(
        side = Perspective.White,
        studentSide = Perspective.White,
        opponentSide = Perspective.Black,
        level = EceLevelEcho(Level(10), Level(10), defaulted = false),
        isSideToMove = true,
        summary = Some("White converted the attack."),
        immediateWarning = None,
        plan = Some("Keep pressure on the king.")
      )
    val blackOutput =
      EceSideOutput(
        side = Perspective.Black,
        studentSide = Perspective.Black,
        opponentSide = Perspective.White,
        level = EceLevelEcho(Level(10), Level(9), defaulted = false),
        isSideToMove = false,
        summary = Some("Black missed a defensive resource."),
        immediateWarning = None,
        plan = Some("Trade attackers.")
      )
    val response =
      EceGameReviewResponse(
        requestEcho = EceGameReviewRequestEcho(request.requestId, "game-1", Level(10)),
        gameSummary = Some("White built a decisive kingside attack."),
        whitePerformanceSummary = Some("White found the main attacking plan."),
        blackPerformanceSummary = Some("Black needed earlier counterplay."),
        turningPoints = List("Move 18: attack became decisive"),
        recurringMotifs = List("Weak dark squares"),
        missedThreats = List("Back-rank motif"),
        moveOutputs = List(EceGameReviewMoveOutput(18, "fen-18", Some(whiteOutput), Some(blackOutput))),
        diagnostics = EceDiagnostics(EceDiagnosticsStatus.Ok, engineVersion = "ece-v1", sanitizedMessage = Some("ready")),
        hasRawProviderPayload = false,
        hasRawAiPromptOrResponse = false,
        attemptsToAlterGameResult = false,
        attemptsToAlterLiveUsedLevel = false,
        attemptsToAlterAssistanceLoad = false,
        attemptsToAlterUsedOffset = false,
        attemptsToAlterEcr = false,
        attemptsToAlterMatchmakingState = false
      )
    val accepted =
      EceGameReviewFrameworkIntegration.acceptGameReview(EceServiceConfig(), outstanding, response)

    assert(EceGameReviewValidator.responseUsable(outstanding, response))
    assert(response.safeShapeForLichess)
    assert(accepted.valid)
    assert(accepted.reviewAvailable)
    assert(accepted.postGameOnly)
    assert(accepted.tokenQuotaChecked)
    assert(!accepted.mutatesRatedGame)
    assert(!EceGameReviewValidator.responseUsable(outstanding.copy(tokenQuotaChecked = false), response))
    assert(!EceGameReviewValidator.responseUsable(outstanding.copy(postGameOnly = false), response))
    assert(!EceGameReviewValidator.responseUsable(outstanding, response.copy(requestEcho = response.requestEcho.copy(gameId = "other"))))
    assert(!EceGameReviewValidator.responseUsable(outstanding, response.copy(hasRawProviderPayload = true)))
    assert(!EceGameReviewValidator.responseUsable(outstanding, response.copy(hasRawAiPromptOrResponse = true)))
    assert(!EceGameReviewValidator.responseUsable(outstanding, response.copy(attemptsToAlterEcr = true)))
