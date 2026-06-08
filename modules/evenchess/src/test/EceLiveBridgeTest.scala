package lila.evenchess

class EceLiveBridgeTest extends munit.FunSuite:

  import CoachingLadder.Level
  import CoachingOverlays.*
  import EngineGateway.*

  private val fen = "rnbqkbnr/pppp1ppp/5n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
  private val request =
    EceBoardStateRequest.boardState(
      gameId = "test-game",
      ply = 10,
      inputFen = fen,
      whiteEcr = None,
      blackEcr = None,
      whiteLevel = Level(10),
      blackLevel = Level(10),
      aiTextAllowed = false
    )

  private def sideOutput(side: Perspective) =
    val opponent = if side == Perspective.White then Perspective.Black else Perspective.White
    EceSideOutput(
      side = side,
      studentSide = side,
      opponentSide = opponent,
      level = EceLevelEcho(Level(10), Level(10), defaulted = false),
      isSideToMove = side == Perspective.White,
      summary = Some("Fixture summary."),
      immediateWarning = Some("Fixture warning."),
      plan = Some("Fixture plan.")
    )

  private val response =
    EceBoardStateResponse(
      requestEcho = EceRequestEcho(request.requestId, request.inputFen, Level(10), Level(10)),
      white = Some(sideOutput(Perspective.White)),
      black = Some(sideOutput(Perspective.Black)),
      diagnostics = EceDiagnostics(EceDiagnosticsStatus.Ok, "test-ece-v2-ground-1", Some("ready")),
      hasPublicPosition = false,
      hasPublicSharedCalculations = false,
      rawProviderPayload = None
    )

  private val atoms =
    MockDisplayOverlayAtoms(
      hangingAttackable = Nil,
      hangingNotAttackable = List("c4"),
      offsetCount = List("e4" -> 0, "c4" -> 1, "f6" -> -1),
      studentThreats = List("d1" -> "h5"),
      opponentThreats = List("d8" -> "h4"),
      pins = List(("f6", "g5", "e7")),
      studentHangingAttackable = List("h5"),
      opponentHangingAttackable = List("g7")
    )

  test("board bridge compiles accepted ECE output into server-approved round overlay payload"):
    val result =
      EceLiveBridge.compileBoardOverlay(
        config = EceServiceConfig(),
        gameId = "test-game",
        playerId = "student",
        ply = 10,
        boardStateKey = fen,
        requesterSide = Perspective.White,
        authorizedLevel = Level(10),
        request = request,
        currentFen = fen,
        response = response,
        atoms = atoms,
        auditId = "audit-test",
        ttlMillis = 1500,
        extraCards = List(EceLiveBridge.ExtraCard("ece.candidate.1", "Candidate", "Nf3: develop.")),
        extraVisuals = List(
          EceLiveBridge.ExtraVisual(
            "ece.eval.deep",
            "Approximate eval +42cp",
            evalCpWhite = Some(42),
            evalWinWhite = Some(54),
            evalDrawWhite = Some(36),
            evalLossWhite = Some(10),
            evalSource = Some("stockfish")
          )
        )
      )

    assert(result.valid)
    assert(result.coachingAvailable)
    assert(result.roundPayload.valid)
    assert(result.roundPayload.hasOnlyServerApprovedContent)
    assertEquals(result.roundPayload.identity.perspective, "white")
    assert(result.roundPayload.cards.exists(_.featureKey == "ece.card.summarycard"))
    assert(result.roundPayload.cards.exists(_.featureKey == "ece.card.warningcard"))
    assert(result.roundPayload.cards.exists(_.featureKey == "ece.card.plancard"))
    assert(result.roundPayload.cards.exists(_.featureKey == "ece.candidate.1"))
    assert(result.roundPayload.visuals.exists(visual =>
      visual.featureKey == "ece.marker.hanging_attackable.student" && visual.label.contains("h5: Student hanging")
    ))
    assert(result.roundPayload.visuals.exists(visual =>
      visual.featureKey == "ece.marker.hanging_attackable.opponent" && visual.label.contains("g7: Opponent hanging")
    ))
    assert(result.roundPayload.visuals.exists(visual =>
      visual.label.contains("Approximate eval +42cp") &&
        visual.evalCpWhite.contains(42) &&
        visual.evalWinWhite.contains(54) &&
        visual.evalSource.contains("stockfish")
    ))
    assertEquals(result.roundPayload.renderableCards.size, 1)
    assertEquals(result.roundPayload.renderableVisuals.size, 1)

  test("board bridge clears stale or mismatched ECE output instead of rendering coaching"):
    val stale =
      EceLiveBridge.compileBoardOverlay(
        config = EceServiceConfig(),
        gameId = "test-game",
        playerId = "student",
        ply = 10,
        boardStateKey = fen,
        requesterSide = Perspective.White,
        authorizedLevel = Level(10),
        request = request,
        currentFen = "new-fen",
        response = response,
        atoms = atoms,
        auditId = "audit-stale",
        ttlMillis = 1500
      )

    assert(stale.valid)
    assert(!stale.coachingAvailable)
    assert(stale.roundPayload.clearOnly)
    assert(stale.roundPayload.cards.isEmpty)
    assert(stale.roundPayload.visuals.isEmpty)

  test("board bridge keeps parsed ECE extras level-gated"):
    val lowRequest = request.copy(whiteLevel = Level(2), blackLevel = Level(2))
    val lowResponse =
      response.copy(
        requestEcho = EceRequestEcho(lowRequest.requestId, lowRequest.inputFen, Level(2), Level(2)),
        white = Some(sideOutput(Perspective.White).copy(level = EceLevelEcho(Level(2), Level(2), defaulted = false))),
        black = Some(sideOutput(Perspective.Black).copy(level = EceLevelEcho(Level(2), Level(2), defaulted = false)))
      )
    val low =
      EceLiveBridge.compileBoardOverlay(
        config = EceServiceConfig(),
        gameId = "test-game",
        playerId = "student",
        ply = 10,
        boardStateKey = fen,
        requesterSide = Perspective.White,
        authorizedLevel = Level(2),
        request = lowRequest,
        currentFen = fen,
        response = lowResponse,
        atoms = atoms,
        auditId = "audit-low",
        ttlMillis = 1500,
        extraCards = List(EceLiveBridge.ExtraCard("ece.candidate.1", "Candidate", "Nf3: develop.")),
        extraVisuals = List(EceLiveBridge.ExtraVisual("ece.eval.deep", "Approximate eval +42cp"))
      )

    assert(low.valid)
    assert(!low.roundPayload.cards.exists(_.featureKey == "ece.candidate.1"))
    assert(!low.roundPayload.visuals.exists(_.featureKey.startsWith("ece.eval")))

  test("board bridge allows all returned potential move visuals at L5+"):
    val l5Request = request.copy(whiteLevel = Level(5), blackLevel = Level(5))
    val l5Response =
      response.copy(
        requestEcho = EceRequestEcho(l5Request.requestId, l5Request.inputFen, Level(5), Level(5)),
        white = Some(sideOutput(Perspective.White).copy(level = EceLevelEcho(Level(5), Level(5), defaulted = false))),
        black = Some(sideOutput(Perspective.Black).copy(level = EceLevelEcho(Level(5), Level(5), defaulted = false)))
      )
    val l5 =
      EceLiveBridge.compileBoardOverlay(
        config = EceServiceConfig(),
        gameId = "test-game",
        playerId = "student",
        ply = 10,
        boardStateKey = fen,
        requesterSide = Perspective.White,
        authorizedLevel = Level(5),
        request = l5Request,
        currentFen = fen,
        response = l5Response,
        atoms = atoms,
        auditId = "audit-l5",
        ttlMillis = 1500,
        extraVisuals = List(
          EceLiveBridge.ExtraVisual("ece.candidate.1", "g1-f3: Potential A"),
          EceLiveBridge.ExtraVisual("ece.candidate.2", "d2-d4: Potential B"),
          EceLiveBridge.ExtraVisual("ece.candidate.3", "c2-c3: Potential C")
        )
      )

    assert(l5.valid)
    assertEquals(
      l5.roundPayload.visuals.filter(_.featureKey.startsWith("ece.candidate.")).map(_.featureKey),
      List("ece.candidate.1", "ece.candidate.2", "ece.candidate.3")
    )

  test("stored board frame bridge compiles review history into the same round payload shape"):
    val payload =
      EceLiveBridge.compileStoredBoardFrame(
        gameId = "review-game",
        playerId = "student",
        ply = 12,
        boardStateKey = fen,
        requesterSide = Perspective.White,
        authorizedLevel = Level(10),
        sideOutput = sideOutput(Perspective.White),
        atoms = atoms,
        auditId = "audit-review-frame",
        ttlMillis = 60_000,
        extraCards = List(EceLiveBridge.ExtraCard("ece.full_review", "Review", "Stored review frame."))
      )

    assert(payload.valid)
    assertEquals(payload.identity.gameId, "review-game")
    assertEquals(payload.identity.ply, 12)
    assertEquals(payload.identity.boardStateKey, fen)
    assert(payload.cards.exists(_.featureKey == "ece.full_review"))
    assert(payload.hasOnlyServerApprovedContent)
