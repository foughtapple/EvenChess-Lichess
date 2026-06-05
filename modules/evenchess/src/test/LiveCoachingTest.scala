package lila.evenchess

class LiveCoachingTest extends munit.FunSuite:

  import AssistanceAccounting.*
  import CoachingLadder.*
  import CoachingOverlays.*
  import CoachingPolicy.*
  import EvenChessMode.*
  import GamePolicy.*
  import LiveCoaching.*
  import ProductInvariants.RequirementClass

  private val calibration = CalibrationParameters.default

  private def player(id: String, level: Int, poolKey: String = "rapid-normal-evenchess") =
    PlayerPolicy(id, SetLevel(level), poolKey)

  private def policyRecord(
      gameId: String = "live-game",
      whiteLevel: Int = 5,
      blackLevel: Int = 5,
      rated: Boolean = true
  ): GamePolicyRecord =
    policyValue(
      GamePolicyRecord.fromRequest(
        GamePolicyCreateRequest(
          gameId = gameId,
          mode = GamePolicyMode.NormalRatedEvenChess,
          rated = rated,
          timeControlBucket = TimeControlBucket.Rapid,
          white = player("white-user", whiteLevel),
          black = player("black-user", blackLevel),
          versions = PolicyVersions.current,
          featureFlags = Map("evenchess_mode" -> "enabled", "server_policy" -> "phase-e"),
          createdAt = 1000L
        ),
        now = 1001L
      )
    )

  private def request(
      playerId: String = "white-user",
      featureKey: String = "move_advice",
      requestType: CoachingRequestType = CoachingRequestType.Display,
      clientClaimedAllowed: Boolean = false
  ): ClientCoachingRequest =
    ClientCoachingRequest(
      gameId = "live-game",
      playerId = playerId,
      ply = 18,
      boardStateKey = "board-key-18",
      perspective = Perspective.White,
      featureKey = featureKey,
      requestType = requestType,
      clientClaimedAllowed = clientClaimedAllowed
    )

  private def context(
      record: GamePolicyRecord = policyRecord(),
      ledger: AssistanceLedger = AssistanceLedger.empty,
      currentUsedLevel: Level = Level(0),
      stale: Boolean = false,
      premoveCommitted: Boolean = false,
      abuseState: AbuseState = AbuseState.Clear,
      engineHealth: ServiceHealth = ServiceHealth.Healthy,
      aiHealth: ServiceHealth = ServiceHealth.Healthy
  ): ServerLiveContext =
    ServerLiveContext(
      policyRecord = record,
      ledger = ledger,
      dimensionsByEventId = Map.empty,
      currentUsedLevel = currentUsedLevel,
      clockContext = ClockContext(
        millisRemaining = 30000,
        incrementMillis = 2000,
        premoveCommitted = premoveCommitted,
        staleForDecision = stale
      ),
      abuseState = abuseState,
      engineHealth = engineHealth,
      aiHealth = aiHealth,
      dimensions = AssistanceDimensions.defaultLive,
      calibration = calibration,
      now = 123456789L,
      ttlMillis = 5000
    )

  private def liveValue[A](result: Either[LiveCoachingError, A]): A =
    result match
      case Right(value) => value
      case Left(error)  => fail(s"Expected Right, got $error")

  private def policyValue[A](result: Either[PersistenceError, A]): A =
    result match
      case Right(value) => value
      case Left(error)  => fail(s"Expected Right, got $error")

  private def historyOutput(
      side: Perspective,
      level: Level,
      ref: String,
      auditId: String
  ) =
    LiveEceOutputReference(
      side = side,
      outputRef = ref,
      auditId = auditId,
      deliveredLevel = level,
      summary = Some(s"${side.toString} summary"),
      plan = Some(s"${side.toString} plan"),
      overlayAtomRefs = List(s"$ref-atoms")
    )

  private def historyEntry(
      ply: Int,
      whiteLevel: Level,
      blackLevel: Level,
      whiteDelivered: Level,
      blackDelivered: Level,
      createdAt: Long
  ) =
    LiveEceHistoryEntry(
      gameId = "live-game",
      ply = ply,
      fen = s"fen-$ply",
      moveUci = Some("e2e4"),
      positionHash = s"hash-$ply",
      sideToMove = Perspective.Black,
      whiteRequestedLevel = whiteLevel,
      blackRequestedLevel = blackLevel,
      policyVersion = "policy-v1",
      eceVersion = "ece-v1",
      whiteOutput = Some(historyOutput(Perspective.White, whiteDelivered, s"white-$ply-${whiteDelivered.value}", s"audit-w-$ply-${whiteDelivered.value}")),
      blackOutput = Some(historyOutput(Perspective.Black, blackDelivered, s"black-$ply-${blackDelivered.value}", s"audit-b-$ply-${blackDelivered.value}")),
      rawEceRetained = false,
      createdAt = createdAt
    )

  test("Phase E requirements are classified before live coaching transport work"):
    val byRequirement =
      PhaseERequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseERequirement.ServerAuthoritativeDecision), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseERequirement.ClientRequestDisplayOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseERequirement.EveryDecisionAudited), RequirementClass.EvenChessSpecific)
    assertEquals(
      byRequirement(PhaseERequirement.OverlayTransportServerAuthorized),
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PhaseERequirement.LilaWebSocketIntegrationDeferredToThinSeams),
      RequirementClass.AdaptedToLichessFork
    )

  test("Phase J requirements are classified before live ECE history storage work"):
    val byRequirement =
      PhaseJRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseJRequirement.ScheduleEceAfterMove), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.StoreFenAndEceMetadataPerPly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.StoreSideToMoveLevelsVersionsAndOutputRefs), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.HigherLevelResultCanonical), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.LoweringVisibleLevelDoesNotReduceUsedLevel), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.LimitedHistoryRetainsReviewEssentials), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.RawEceRetentionPolicyControlled), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.LiveReviewUsesSavedHistoryLater), RequirementClass.AdaptedToLichessFork)

  test("Phase K requirements are classified before proposed-move integration work"):
    val byRequirement =
      PhaseKRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseKRequirement.SingleProposedMoveOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKRequirement.ProposedMoveUsesCurrentFen), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKRequirement.RequesterMustBeSideToMove), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKRequirement.ProposedMoveRequiresPolicyPermission), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKRequirement.ProposedMovePreviewNotActualPosition), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKRequirement.ProposedMoveDoesNotMutateLiveHistoryOrSettlement), RequirementClass.EvenChessSpecific)

  test("Phase L requirements are classified before review mode integration work"):
    val byRequirement =
      PhaseLRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseLRequirement.LiveWhiteReviewMode), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseLRequirement.LiveBlackReviewMode), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseLRequirement.LiveBothReviewMode), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseLRequirement.LiveModesUseSavedHistoryWithoutCustomTokens), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseLRequirement.CustomReviewSelectableLevels), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseLRequirement.CustomReviewPerspectiveModes), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseLRequirement.CustomReviewCacheAndTokenIntent), RequirementClass.EvenChessSpecific)

  test("Phase M requirements are classified before full-game ECE integration work"):
    val byRequirement =
      PhaseMRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseMRequirement.FullGameConsumesWholeGameInput), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseMRequirement.FullGameCarriesSavedLiveEceSnapshots), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseMRequirement.FullGamePostGameReviewOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseMRequirement.FullGameAtMostOneAiNarrativeCall), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseMRequirement.FullGameRequiresTokenQuotaCheck), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseMRequirement.FullGameDoesNotMutateLiveSettlement), RequirementClass.EvenChessSpecific)

  test("live service renders only server-authorized overlays and recomputes accounting"):
    val result = liveValue(LiveCoachingService.process(request(), context()))
    val payload = result.transport.overlay.getOrElse(fail("Expected live overlay payload"))

    assertEquals(result.decision.outcome, PolicyOutcome.AllowRender)
    assert(result.audited)
    assert(result.serverAuthoritative)
    assert(payload.isRenderable)
    assertEquals(payload.auditId, result.auditEvent.eventId)
    assertEquals(payload.level.value, 5)
    assert(payload.rawStockfishLine.isEmpty)
    assert(payload.hiddenDebugData.isEmpty)
    assert(result.transport.renderableOverlay)
    assert(result.transport.approvedForClientTransport)
    assertEquals(result.assistanceSummary.usedLevel.value, 5)
    assert(result.assistanceSummary.assistanceLoad.value > 0.0)
    assert(result.usedOffset.value >= calibration.baseOffsetByUsedLevel(5))
    assert(!result.clientClaimAcceptedAsAuthority)

  test("client permission claim cannot bypass server Set Level"):
    val lowSetLevel = policyRecord(whiteLevel = 4)
    val result =
      liveValue(LiveCoachingService.process(request(clientClaimedAllowed = true), context(record = lowSetLevel)))

    assert(result.audited)
    assert(result.serverAuthoritative)
    assertEquals(result.decision.outcome, PolicyOutcome.SuppressLevel)
    assertEquals(result.transport.overlay, None)
    assertEquals(result.transport.clearReason, ClearReason.Suppressed)
    assert(result.transport.clearOnly)
    assertEquals(result.assistanceSummary.usedLevel.value, 0)
    assertEquals(result.assistanceSummary.assistanceLoad.value, 0.0)
    assertEquals(result.usedOffset.value, 0)
    assert(!result.clientClaimAcceptedAsAuthority)

  test("request-only hidden prefetch is audited but not transported or charged"):
    val result =
      liveValue(LiveCoachingService.process(request(requestType = CoachingRequestType.Request), context()))

    assert(result.audited)
    assertEquals(result.decision.outcome, PolicyOutcome.AllowHidden)
    assertEquals(result.transport.overlay, None)
    assertEquals(result.transport.clearReason, ClearReason.None)
    assertEquals(result.auditEvent.deliveredLevel, None)
    assertEquals(result.auditEvent.visibility, OverlayVisibility.Hidden)
    assertEquals(result.assistanceSummary.usedLevel.value, 0)
    assertEquals(result.assistanceSummary.assistanceLoad.value, 0.0)
    assertEquals(result.usedOffset.value, 0)

  test("stale and premove decisions clear transport and do not charge assistance"):
    val staleResult = liveValue(LiveCoachingService.process(request(), context(stale = true)))
    val premoveResult = liveValue(LiveCoachingService.process(request(), context(premoveCommitted = true)))

    assertEquals(staleResult.decision.outcome, PolicyOutcome.Stale)
    assertEquals(staleResult.transport.overlay, None)
    assertEquals(staleResult.transport.clearReason, ClearReason.StalePayload)
    assert(staleResult.dimensionsByEventId(staleResult.auditEvent.eventId).staleNonDecisionHelp)
    assertEquals(staleResult.assistanceSummary.assistanceLoad.value, 0.0)
    assertEquals(premoveResult.decision.outcome, PolicyOutcome.Stale)
    assertEquals(premoveResult.transport.clearReason, ClearReason.StalePayload)
    assertEquals(premoveResult.assistanceSummary.usedLevel.value, 0)

  test("L5 remains first live engine candidate level"):
    val l4 = policyRecord(whiteLevel = 4)
    val l5 = policyRecord(whiteLevel = 5)

    val blocked = liveValue(LiveCoachingService.process(request(featureKey = "move_advice"), context(record = l4)))
    val allowed = liveValue(LiveCoachingService.process(request(featureKey = "move_advice"), context(record = l5)))

    assertEquals(Levels.firstLiveEngineCandidateLevel.value, 5)
    assertEquals(FeatureRegistry.byKey("move_advice").unlockLevel.value, 5)
    assertEquals(blocked.decision.outcome, PolicyOutcome.SuppressLevel)
    assertEquals(allowed.decision.outcome, PolicyOutcome.AllowRender)
    assert(allowed.transport.renderableOverlay)

  test("transport invalidation clears overlays on moves, board mismatch, stale, and expiry"):
    val result = liveValue(LiveCoachingService.process(request(), context()))
    val payload = result.transport.overlay.getOrElse(fail("Expected overlay payload"))
    val matchingContext = BoardStateContext("live-game", 18, "board-key-18", Perspective.White)
    val mismatchContext = matchingContext.copy(boardStateKey = "other-board")

    assertEquals(
      LiveTransportInvalidation.clearReasonFor(payload, matchingContext, movePlayed = false),
      ClearReason.None
    )
    assertEquals(
      LiveTransportInvalidation.clearReasonFor(payload, matchingContext, movePlayed = true),
      ClearReason.MovePlayed
    )
    assertEquals(
      LiveTransportInvalidation.clearReasonFor(payload, mismatchContext, movePlayed = false),
      ClearReason.BoardMismatch
    )
    assertEquals(
      LiveTransportInvalidation.clearReasonFor(payload.copy(stale = true), matchingContext, movePlayed = false),
      ClearReason.StalePayload
    )
    assertEquals(
      LiveTransportInvalidation.clearReasonFor(payload.copy(ttlMillis = 0), matchingContext, movePlayed = false),
      ClearReason.PayloadExpired
    )

  test("invalid live requests are rejected before a policy decision"):
    assertEquals(
      LiveCoachingService.process(request(playerId = "observer"), context()).left.toOption,
      Some(LiveCoachingError.MissingPlayerPolicy)
    )
    assertEquals(
      LiveCoachingService.process(request(featureKey = "unknown_feature"), context()).left.toOption,
      Some(LiveCoachingError.UnknownFeature)
    )
    assertEquals(
      LiveCoachingService.process(request().copy(gameId = "other-game"), context()).left.toOption,
      Some(LiveCoachingError.GameMismatch)
    )

  test("live ECE history scheduler creates server-side board-state requests for committed FEN and levels"):
    val scheduled =
      LiveEceHistoryScheduler.scheduleBoardState(
        gameId = "live-game",
        ply = 18,
        fen = "fen-after-move",
        positionHash = "hash-18",
        whiteEcr = Some(1500),
        blackEcr = Some(1510),
        whiteLevel = Level(5),
        blackLevel = Level(4),
        aiTextAllowed = false,
        scheduledAt = 123456790L
      )

    assert(scheduled.valid)
    assertEquals(scheduled.request.mode, "board_state")
    assertEquals(scheduled.request.inputFen, "fen-after-move")
    assertEquals(scheduled.request.whiteLevel, Level(5))
    assertEquals(scheduled.request.blackLevel, Level(4))
    assertEquals(scheduled.request.useAi, 0)
    assertEquals(scheduled.request.whiteRatingInput, 1500)
    assertEquals(scheduled.request.blackRatingInput, 1510)

  test("live ECE history stores FEN, side-to-move, levels, versions, output refs, and audit ids per ply"):
    val entry = historyEntry(18, Level(5), Level(4), Level(5), Level(4), createdAt = 123456790L)
    val history = LiveEceHistoryRecord.empty("live-game").append(entry)

    assert(entry.valid)
    assert(entry.reconstructableWithLimitedRetention)
    assert(history.valid)
    assertEquals(history.fenHistory, List("fen-18"))
    assertEquals(history.canonicalOutput(18, Perspective.White).map(_.outputRef), Some("white-18-5"))
    assertEquals(history.canonicalOutput(18, Perspective.Black).map(_.auditId), Some("audit-b-18-4"))

  test("higher-level live ECE result is canonical and lowering later visible level does not reduce used level"):
    val low = historyEntry(18, Level(5), Level(5), Level(4), Level(3), createdAt = 100L)
    val raised = historyEntry(18, Level(6), Level(5), Level(6), Level(3), createdAt = 101L)
    val laterLower = historyEntry(19, Level(4), Level(4), Level(4), Level(2), createdAt = 102L)
    val history =
      LiveEceHistoryRecord
        .empty("live-game")
        .append(low)
        .append(raised)
        .append(laterLower)

    assert(history.valid)
    assertEquals(history.canonicalOutput(18, Perspective.White).map(_.deliveredLevel), Some(Level(6)))
    assertEquals(history.canonicalOutput(18, Perspective.Black).map(_.deliveredLevel), Some(Level(3)))
    assertEquals(history.highestUsedLevel(Perspective.White), Level(6))
    assertEquals(history.highestUsedLevel(Perspective.Black), Level(3))

  test("limited live ECE history keeps review essentials and rejects raw ECE retention by default"):
    val entry = historyEntry(18, Level(5), Level(4), Level(5), Level(4), createdAt = 123456790L)
    val rawRetained = entry.copy(rawEceRetained = true)
    val essentials = LiveEceHistoryRecord.empty("live-game").append(entry).limitedReviewEssentials

    assert(!rawRetained.valid)
    assert(essentials.reconstructable)
    assertEquals(essentials.highestWhiteLevel, Level(5))
    assertEquals(essentials.highestBlackLevel, Level(4))
    assert(essentials.outputRefs.contains("white-18-5"))
    assert(essentials.auditIds.contains("audit-b-18-4"))

  test("proposed-move scheduler creates preview-only ECE requests for one current-side move"):
    val scheduled =
      LiveEceHistoryScheduler.scheduleProposedMove(
        gameId = "live-game",
        ply = 18,
        proposalIndex = 1,
        fen = "fen-before-proposal",
        positionHash = "hash-18",
        proposedMoveUci = "g1f3",
        requesterSide = Perspective.White,
        sideToMove = Perspective.White,
        whiteEcr = Some(1500),
        blackEcr = Some(1510),
        whiteLevel = Level(10),
        blackLevel = Level(10),
        aiTextAllowed = false,
        proposedMoveHelpAllowed = true,
        scheduledAt = 123456791L
      ).toOption.get

    assert(scheduled.valid)
    assert(scheduled.previewOnly)
    assertEquals(scheduled.request.mode, "proposed_move")
    assertEquals(scheduled.request.proposedMoveUci, "g1f3")
    assertEquals(scheduled.request.inputFen, "fen-before-proposal")
    assertEquals(scheduled.request.useAi, 0)

  test("proposed-move scheduler rejects wrong side, unauthorized help, and multiple proposed moves"):
    val wrongSide =
      LiveEceHistoryScheduler.scheduleProposedMove(
        "live-game",
        18,
        1,
        "fen-before-proposal",
        "hash-18",
        "g1f3",
        requesterSide = Perspective.White,
        sideToMove = Perspective.Black,
        whiteEcr = Some(1500),
        blackEcr = Some(1510),
        whiteLevel = Level(10),
        blackLevel = Level(10),
        aiTextAllowed = false,
        proposedMoveHelpAllowed = true,
        scheduledAt = 123456791L
      )
    val unauthorized =
      LiveEceHistoryScheduler.scheduleProposedMove(
        "live-game",
        18,
        1,
        "fen-before-proposal",
        "hash-18",
        "g1f3",
        requesterSide = Perspective.White,
        sideToMove = Perspective.White,
        whiteEcr = Some(1500),
        blackEcr = Some(1510),
        whiteLevel = Level(10),
        blackLevel = Level(10),
        aiTextAllowed = false,
        proposedMoveHelpAllowed = false,
        scheduledAt = 123456791L
      )
    val multipleMoves =
      LiveEceHistoryScheduler.scheduleProposedMove(
        "live-game",
        18,
        1,
        "fen-before-proposal",
        "hash-18",
        "g1f3 e7e5",
        requesterSide = Perspective.White,
        sideToMove = Perspective.White,
        whiteEcr = Some(1500),
        blackEcr = Some(1510),
        whiteLevel = Level(10),
        blackLevel = Level(10),
        aiTextAllowed = false,
        proposedMoveHelpAllowed = true,
        scheduledAt = 123456791L
      )

    assert(wrongSide.isLeft)
    assert(unauthorized.isLeft)
    assert(multipleMoves.isLeft)

  test("live review modes select saved White, Black, and side-to-move ECE history without custom tokens"):
    val entry = historyEntry(18, Level(5), Level(4), Level(5), Level(4), createdAt = 123456790L)
    val history = LiveEceHistoryRecord.empty("live-game").append(entry)
    val liveWhite = ReviewModeEngine.liveReviewFrame(history, 18, ReviewMode.LiveWhite).getOrElse(fail("Expected Live White frame"))
    val liveBlack = ReviewModeEngine.liveReviewFrame(history, 18, ReviewMode.LiveBlack).getOrElse(fail("Expected Live Black frame"))
    val liveBoth = ReviewModeEngine.liveReviewFrame(history, 18, ReviewMode.LiveBoth).getOrElse(fail("Expected Live Both frame"))

    assert(liveWhite.valid)
    assert(liveBlack.valid)
    assert(liveBoth.valid)
    assertEquals(liveWhite.sourceSide, Perspective.White)
    assertEquals(liveWhite.output.outputRef, "white-18-5")
    assertEquals(liveBlack.sourceSide, Perspective.Black)
    assertEquals(liveBlack.output.outputRef, "black-18-4")
    assertEquals(liveBoth.sourceSide, Perspective.Black)
    assertEquals(liveBoth.output.outputRef, "black-18-4")
    assert(!liveWhite.consumesCustomAnalysisTokens)
    assert(!liveBoth.mutatesLiveFairnessState)
    assert(ReviewModeEngine.liveReviewFrame(history, 18, ReviewMode.Custom).isEmpty)

  test("custom review mode records selectable levels, perspective, cache key, and token intent"):
    val essentials =
      LiveEceHistoryRecord
        .empty("live-game")
        .append(historyEntry(18, Level(5), Level(4), Level(5), Level(4), createdAt = 123456790L))
        .limitedReviewEssentials
    val tokenFreeRequest =
      CustomReviewRequest(
        gameId = "live-game",
        whiteLevel = Level(5),
        blackLevel = Level(4),
        perspective = CustomReviewPerspective.White,
        eceVersion = "ece-v1",
        policyVersion = "policy-v1",
        useAi = false
      )
    val tokenFreePlan = ReviewModeEngine.planCustomReview(tokenFreeRequest, essentials)
    val tokenRequiredRequest =
      tokenFreeRequest.copy(
        whiteLevel = Level(10),
        blackLevel = Level(7),
        perspective = CustomReviewPerspective.SideToMove,
        useAi = true
      )
    val tokenRequiredPlan = ReviewModeEngine.planCustomReview(tokenRequiredRequest, essentials)

    assert(tokenFreePlan.valid)
    assert(!tokenFreePlan.requiresCustomAnalysisTokens)
    assert(tokenFreePlan.cacheKey.contains("live-game"))
    assert(tokenFreePlan.cacheKey.contains("w5"))
    assert(tokenFreePlan.cacheKey.contains("b4"))
    assert(tokenFreePlan.cacheKey.contains("White"))
    assert(tokenFreePlan.cacheKey.contains("ece-v1"))
    assert(tokenFreePlan.cacheKey.contains("policy-v1"))
    assert(tokenRequiredPlan.valid)
    assert(tokenRequiredPlan.requiresCustomAnalysisTokens)
    assert(tokenRequiredPlan.cacheKey.contains("SideToMove"))
    assert(!tokenRequiredPlan.mutatesLiveUsedLevel)
    assert(!tokenRequiredPlan.mutatesEcrSettlement)

  test("full-game review plan builds token-gated ECE request from whole game and saved snapshots"):
    val history =
      LiveEceHistoryRecord
        .empty("live-game")
        .append(historyEntry(18, Level(5), Level(4), Level(5), Level(4), createdAt = 123456790L))
        .append(historyEntry(19, Level(6), Level(5), Level(6), Level(5), createdAt = 123456791L))
    val game =
      EngineGateway.EceGameReviewInput(
        gameId = "live-game",
        initialFen = "startpos",
        pgn = Some("1. e4 e5 2. Nf3"),
        moves = List("e2e4", "e7e5", "g1f3"),
        fenHistory = history.fenHistory,
        result = "1-0",
        termination = "checkmate"
      )
    val plan =
      ReviewModeEngine.planFullGameReview(
        game = game,
        history = history,
        reviewIndex = 1,
        whiteEcr = Some(1500),
        blackEcr = Some(1510),
        reviewLevel = Level(10),
        aiNarrativeAllowed = true,
        tokenKind = FullGameReviewTokenKind.MatchReview,
        tokenQuotaChecked = true
      )
    val blocked =
      ReviewModeEngine.planFullGameReview(
        game = game,
        history = history,
        reviewIndex = 2,
        whiteEcr = Some(1500),
        blackEcr = Some(1510),
        reviewLevel = Level(10),
        aiNarrativeAllowed = true,
        tokenKind = FullGameReviewTokenKind.FullAnalysis,
        tokenQuotaChecked = false
      )

    assert(plan.valid)
    assertEquals(plan.request.mode, "full_match")
    assertEquals(plan.request.requestId, "live-game-review-1")
    assertEquals(plan.request.game.moves, List("e2e4", "e7e5", "g1f3"))
    assertEquals(plan.request.game.fenHistory, List("fen-18", "fen-19"))
    assertEquals(plan.request.liveEceSnapshots.size, 2)
    assertEquals(plan.request.liveEceSnapshots.head.whiteOutputRef, Some("white-18-5"))
    assertEquals(plan.request.liveEceSnapshots.last.blackOutputRef, Some("black-19-5"))
    assertEquals(plan.maxAiNarrativeCalls, 1)
    assert(!plan.mutatesLiveUsedLevel)
    assert(!plan.mutatesAssistanceLoad)
    assert(!plan.mutatesUsedOffset)
    assert(!plan.mutatesEcrSettlement)
    assert(!plan.mutatesMatchmakingState)
    assert(!blocked.valid)
