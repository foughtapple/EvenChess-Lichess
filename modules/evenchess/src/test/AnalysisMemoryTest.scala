package lila.evenchess

class AnalysisMemoryTest extends munit.FunSuite:

  import AnalysisMemory.*
  import CoachingLadder.Level
  import CoachingOverlays.Perspective
  import LiveCoaching.*
  import ProductInvariants.RequirementClass

  private val owner = "user-analysis-memory"

  private def output(side: Perspective, level: Level, gameId: String, ply: Int) =
    LiveEceOutputReference(
      side = side,
      outputRef = s"$gameId-$ply-${side.toString.toLowerCase}-l${level.value}",
      auditId = s"audit-$gameId-$ply-${side.toString.toLowerCase}",
      deliveredLevel = level,
      summary = Some(s"$side summary at L${level.value}."),
      plan = Some(s"$side plan at L${level.value}."),
      overlayAtomRefs = List(s"atom-$gameId-$ply-${side.toString.toLowerCase}")
    )

  private def entry(
      gameId: String,
      ply: Int,
      whiteLevel: Level = Level(5),
      blackLevel: Level = Level(4),
      createdAt: Long = 1000L
  ) =
    LiveEceHistoryEntry(
      gameId = gameId,
      ply = ply,
      fen = s"fen-$gameId-$ply",
      moveUci = Some("e2e4"),
      positionHash = s"hash-$gameId-$ply",
      sideToMove = if ply % 2 == 0 then Perspective.White else Perspective.Black,
      whiteRequestedLevel = whiteLevel,
      blackRequestedLevel = blackLevel,
      policyVersion = "analysis-memory-policy-v1",
      eceVersion = "ece-v1",
      whiteOutput = Some(output(Perspective.White, whiteLevel, gameId, ply)),
      blackOutput = Some(output(Perspective.Black, blackLevel, gameId, ply)),
      rawEceRetained = false,
      createdAt = createdAt
    )

  private def history(gameId: String, whiteLevel: Level = Level(5), blackLevel: Level = Level(4)) =
    LiveEceHistoryRecord
      .empty(gameId)
      .append(entry(gameId, 1, whiteLevel, blackLevel, 1001L))
      .append(entry(gameId, 2, whiteLevel, blackLevel, 1002L))

  private def recentGame(gameId: String, completedAt: Long) =
    StoredRecentGame(
      ownerUserId = owner,
      gameId = gameId,
      completedAt = completedAt,
      history = history(gameId)
    )

  private def storedFenPayload(
      gameId: String,
      ply: Int,
      side: Perspective,
      level: Level,
      createdAt: Long,
      source: StoredEcePayloadSource = StoredEcePayloadSource.LiveTurn
  ) =
    StoredEceFenPayload(
      ownerUserId = owner,
      gameId = gameId,
      moveNumber = math.max(0, (ply + 1) / 2),
      ply = ply,
      fen = s"fen-$gameId-$ply",
      positionHash = s"hash-$gameId-$ply",
      side = side,
      deliveredLevel = level,
      eceVersion = "ece-v1",
      policyVersion = "analysis-memory-policy-v1",
      auditId = s"audit-$gameId-$ply-${side.toString.toLowerCase}-l${level.value}",
      approvedLiveOverlayJson =
        s"""{"enabled":true,"gameId":"$gameId","ply":$ply,"boardStateKey":"fen-$gameId-$ply","perspective":"${side.toString.toLowerCase}","auditId":"audit-$gameId-$ply","serverAuthorized":true,"ttlMillis":60000,"cards":[],"visuals":[]}""",
      source = source,
      createdAt = createdAt
    )

  private def analysisKey(
      gameId: String,
      whiteLevel: Level = Level(10),
      blackLevel: Level = Level(7),
      perspective: CustomReviewPerspective = CustomReviewPerspective.SideToMove
  ) =
    FullGameAnalysisKey(
      gameId = gameId,
      whiteLevel = whiteLevel,
      blackLevel = blackLevel,
      perspective = perspective,
      eceVersion = "ece-v1",
      policyVersion = "analysis-memory-policy-v1",
      useAi = false
    )

  private def requestedAnalysis(gameId: String, requestedAt: Long, key: Option[FullGameAnalysisKey] = None) =
    val resolvedKey = key.getOrElse(analysisKey(gameId))
    StoredFullGameAnalysis(
      ownerUserId = owner,
      analysisId = s"analysis-$gameId-${resolvedKey.whiteLevel.value}-${resolvedKey.blackLevel.value}",
      key = resolvedKey,
      requestedAt = requestedAt,
      history = history(gameId, resolvedKey.whiteLevel, resolvedKey.blackLevel),
      tokenQuotaChecked = true
    )

  test("analysis-memory requirements classify retention and shared overlay shell rules"):
    val byRequirement =
      AnalysisMemoryRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(AnalysisMemoryRequirement.RecentLiveHistoryLimit), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AnalysisMemoryRequirement.RequestedAnalysisLimit), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AnalysisMemoryRequirement.MissingHistoryRequiresAnalysisRequest), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(AnalysisMemoryRequirement.SharedOverlayShellAcrossSurfaces), RequirementClass.AdaptedToLichessFork)

  test("recent game memory keeps only the user's last 10 games with attached ECE history"):
    val memory =
      (1 to 12).foldLeft(UserAnalysisMemory.empty(owner)): (current, index) =>
        current.rememberRecentGame(recentGame(s"game-$index", completedAt = index.toLong)).toOption.get

    assert(memory.valid)
    assertEquals(memory.recentGames.size, 10)
    assert(memory.recentHistory("game-1").isEmpty)
    assert(memory.recentHistory("game-2").isEmpty)
    assert(memory.recentHistory("game-12").nonEmpty)

    val retained = memory.liveHistoryFrame("game-12", ply = 1, ReviewMode.LiveWhite)
    val dropped = memory.liveHistoryFrame("game-1", ply = 1, ReviewMode.LiveWhite)

    assert(retained.valid)
    assert(retained.payloadAvailable)
    assertEquals(retained.source, PayloadMemorySource.RecentLiveGame)
    assertEquals(retained.setLevel, Level(5))
    assert(dropped.valid)
    assert(!dropped.payloadAvailable)
    assert(dropped.analysisRequestRequired)
    assertEquals(dropped.setLevel, Level(10))

  test("requested full-game analysis memory keeps last 100 records and keys by game and levels"):
    val initial =
      (1 to 101).foldLeft(UserAnalysisMemory.empty(owner)): (current, index) =>
        val key = analysisKey(s"analysis-game-$index")
        current.rememberRequestedAnalysis(requestedAnalysis(s"analysis-game-$index", index.toLong, Some(key))).toOption.get

    assert(initial.valid)
    assertEquals(initial.requestedAnalyses.size, 100)
    assert(initial.requestedAnalysis(analysisKey("analysis-game-1")).isEmpty)
    assert(initial.requestedAnalysis(analysisKey("analysis-game-101")).nonEmpty)

    val sameGameLow = analysisKey("same-game", whiteLevel = Level(5), blackLevel = Level(4))
    val sameGameHigh = analysisKey("same-game", whiteLevel = Level(10), blackLevel = Level(8))
    val withBoth =
      initial
        .rememberRequestedAnalysis(requestedAnalysis("same-game", 200L, Some(sameGameLow)))
        .toOption
        .get
        .rememberRequestedAnalysis(requestedAnalysis("same-game", 201L, Some(sameGameHigh)))
        .toOption
        .get

    assert(withBoth.requestedAnalysis(sameGameLow).nonEmpty)
    assert(withBoth.requestedAnalysis(sameGameHigh).nonEmpty)
    assertNotEquals(sameGameLow.cacheKey, sameGameHigh.cacheKey)

    val frame = withBoth.requestedAnalysisFrame(sameGameHigh, ply = 1, ReviewMode.LiveBlack)
    assert(frame.valid)
    assert(frame.payloadAvailable)
    assertEquals(frame.source, PayloadMemorySource.RequestedFullGameAnalysis)
    assertEquals(frame.setLevel, Level(8))

  test("evenchess PGN history stores one highest-level ECE payload per FEN and side"):
    val gameId = "ece-pgn-game"
    val low = storedFenPayload(gameId, ply = 4, Perspective.White, Level(4), createdAt = 100L)
    val high = storedFenPayload(gameId, ply = 4, Perspective.White, Level(8), createdAt = 101L)
    val laterLow = storedFenPayload(gameId, ply = 4, Perspective.White, Level(5), createdAt = 102L)
    val black = storedFenPayload(gameId, ply = 4, Perspective.Black, Level(6), createdAt = 103L)

    val history =
      EvenChessPgnHistory
        .empty(owner, gameId)
        .upsertHighestLevel(low)
        .toOption
        .get
        .upsertHighestLevel(high)
        .toOption
        .get
        .upsertHighestLevel(laterLow)
        .toOption
        .get
        .upsertHighestLevel(black)
        .toOption
        .get

    assert(history.valid)
    assertEquals(history.frames.size, 2)
    assertEquals(history.frameAt(4, Perspective.White).map(_.deliveredLevel), Some(Level(8)))
    assertEquals(history.frameAt(4, Perspective.Black).map(_.deliveredLevel), Some(Level(6)))
    assertEquals(history.frameForFen(s"fen-$gameId-4", Perspective.White, minimumLevel = Level(8)).map(_.auditId), Some(high.auditId))
    assert(history.frameForFen(s"fen-$gameId-4", Perspective.White, minimumLevel = Level(9)).isEmpty)

  test("full-game level 10 analysis frames attach using the same saved FEN payload format"):
    val gameId = "full-match-history"
    val live =
      EvenChessPgnHistory
        .empty(owner, gameId)
        .upsertHighestLevel(storedFenPayload(gameId, ply = 2, Perspective.White, Level(5), createdAt = 100L))
        .toOption
        .get
    val fullFrames = List(
      storedFenPayload(gameId, ply = 2, Perspective.White, Level(10), createdAt = 200L, StoredEcePayloadSource.FullGameAnalysis),
      storedFenPayload(gameId, ply = 3, Perspective.Black, Level(10), createdAt = 201L, StoredEcePayloadSource.FullGameAnalysis)
    )
    val upgraded = live.attachFullGameFrames(fullFrames).toOption.get

    assert(upgraded.valid)
    assertEquals(upgraded.frames.size, 2)
    assertEquals(upgraded.frameAt(2, Perspective.White).map(_.deliveredLevel), Some(Level(10)))
    assertEquals(upgraded.frameAt(2, Perspective.White).map(_.source), Some(StoredEcePayloadSource.FullGameAnalysis))
    assertEquals(upgraded.highestLevel(Perspective.White), Level(10))
    assertEquals(upgraded.moveCount, 2)

  test("best available frame uses requested analysis when selected and otherwise live history"):
    val gameId = "best-frame-game"
    val key = analysisKey(gameId, whiteLevel = Level(10), blackLevel = Level(9))
    val memory =
      UserAnalysisMemory
        .empty(owner)
        .rememberRecentGame(recentGame(gameId, completedAt = 10L))
        .toOption
        .get
        .rememberRequestedAnalysis(requestedAnalysis(gameId, requestedAt = 20L, Some(key)))
        .toOption
        .get

    val live = memory.bestAvailableFrame(gameId, None, ply = 1, ReviewMode.LiveWhite)
    val requested = memory.bestAvailableFrame(gameId, Some(key), ply = 1, ReviewMode.LiveWhite)

    assertEquals(live.source, PayloadMemorySource.RecentLiveGame)
    assertEquals(live.setLevel, Level(5))
    assertEquals(requested.source, PayloadMemorySource.RequestedFullGameAnalysis)
    assertEquals(requested.setLevel, Level(10))

  test("mode-neutral overlay shell caps selections and keeps review display separate from live fairness"):
    val live =
      ModeNeutralOverlayPolicy.liveGame(setLevel = Level(6), currentUsedLevel = Level(2), payloadAvailable = true)
    val liveRaised = ModeNeutralOverlayPolicy.withSelectedLevel(live, Level(5))
    val liveLowered = ModeNeutralOverlayPolicy.withSelectedLevel(liveRaised, Level(1))

    assert(live.valid)
    assert(live.mutatesLiveFairnessState)
    assertEquals(liveRaised.usedLevel, Level(5))
    assertEquals(liveLowered.usedLevel, Level(5))
    assertEquals(ModeNeutralOverlayPolicy.visibleLevelCap(Level(6), Level(10)), Level(6))
    assertEquals(ModeNeutralOverlayPolicy.monotonicUsedLevel(Level(10), Level(5), Level(1)), Level(5))

    val memoryForFrame =
      UserAnalysisMemory.empty(owner).rememberRecentGame(recentGame("overlay-game", 1L)).toOption.get
    val frame = memoryForFrame.liveHistoryFrame("overlay-game", ply = 1, ReviewMode.LiveWhite)
    val retained = ModeNeutralOverlayPolicy.retainedHistory(frame)
    val retainedLowered = ModeNeutralOverlayPolicy.withSelectedLevel(retained, Level(1))
    val missing = ModeNeutralOverlayPolicy.missingHistory("missing-game", ply = 1)
    val computer = ModeNeutralOverlayPolicy.computerGame(payloadAvailable = true)

    assert(retained.valid)
    assert(!retained.mutatesLiveFairnessState)
    assert(retained.payloadAvailable)
    assertEquals(retained.setLevel, Level(5))
    assertEquals(retainedLowered.usedLevel, Level(5))
    assert(missing.valid)
    assert(!missing.payloadAvailable)
    assert(missing.analysisRequestRequired)
    assertEquals(missing.setLevel, Level(10))
    assert(computer.valid)
    assertEquals(computer.surface, OverlayShellSurface.ComputerGame)
    assertEquals(computer.setLevel, Level(10))
