package lila.evenchess

class OffsetCountTest extends munit.FunSuite:

  import CoachingLadder.Level
  import CoachingOverlays.Perspective
  import OffsetCount.*

  private val completeBasis = CalculationBasis(
    legalCapturesProvided = true,
    forcedFirstCaptureProvided = true,
    optionalRecapturesProvided = true,
    leastValuableAttackersConsidered = true,
    occupancyUpdatesProvided = true,
    pinsProvided = true,
    xRaysProvided = true,
    kingLegalityProvided = true,
    discoveredCapturesProvided = true,
    optimalLocalChoicesProvided = true
  )

  private def step(
      side: ExchangeSide,
      attacker: PieceRole = PieceRole.Pawn,
      captured: PieceRole = PieceRole.Pawn,
      legal: Boolean = true,
      kingLegal: Boolean = true,
      pinned: Boolean = false,
      xray: Boolean = false,
      occupancyUpdated: Boolean = true
  ) =
    LegalExchangeStep(
      capturingSide = side,
      attacker = attacker,
      capturedPiece = captured,
      legalCapture = legal,
      kingLegalAfterCapture = kingLegal,
      illegalBecausePinned = pinned,
      usesXrayOrDiscoveredCapture = xray,
      occupancyUpdated = occupancyUpdated
    )

  private def choice(side: ExchangeSide, alternatives: LegalExchangeStep*) =
    ExchangeChoice(side, alternatives.toList)

  private def input(choices: List[ExchangeChoice], level: Level = Level(3), stale: Boolean = false) =
    OffsetCountInput(
      gameId = "game-1",
      ply = 24,
      boardStateKey = "board-key-24",
      square = "e4",
      initialMove = Some("Nxe4"),
      perspective = Perspective.White,
      setLevel = level,
      serverAuthorized = true,
      rated = true,
      visibleReveal = true,
      stale = stale,
      auditId = "audit-oc-1",
      calculationBasis = completeBasis,
      exchangeChoices = choices,
      repeatedRevealCount = 0,
      repeatedRevealCap = 3
    )

  test("Offset Count identity is L3 Exchange Resolver and not best-move verdict"):
    assertEquals(featureKey, "offset_count")
    assertEquals(unlockLevel.value, 3)
    assert(CalculationBasis.requiresLichessLegalFacts)
    assert(!CalculationBasis.rebuildsLegalMoveGeneration)

  test("display semantics match equal, student-wins, opponent-wins, and unknown states"):
    val equal = DisplaySemantics.forDelta(0)
    val student = DisplaySemantics.forDelta(2)
    val opponent = DisplaySemantics.forDelta(-1)
    val unknown = DisplaySemantics.unknown

    assertEquals(equal.resultState, ResultState.Equal)
    assertEquals(equal.color, DisplayColor.Blue)
    assertEquals(equal.icon, DisplayIcon.Shield)
    assert(!equal.showNumber)

    assertEquals(student.resultState, ResultState.StudentWins)
    assertEquals(student.color, DisplayColor.Green)
    assertEquals(student.displayCount, 2)
    assert(student.showNumber)
    assert(!student.includePlusSign)

    assertEquals(opponent.resultState, ResultState.OpponentWins)
    assertEquals(opponent.color, DisplayColor.Red)
    assertEquals(opponent.displayCount, 1)
    assert(opponent.showNumber)
    assert(!opponent.includePlusSign)

    assertEquals(unknown.resultState, ResultState.Unknown)
    assertEquals(unknown.color, DisplayColor.Grey)
    assertEquals(unknown.icon, DisplayIcon.Disabled)

  test("piece-count display counts every captured piece as one"):
    val payload = Resolver.resolve(
      input(
        List(
          choice(ExchangeSide.Student, step(ExchangeSide.Student, captured = PieceRole.Queen)),
          choice(ExchangeSide.Student, step(ExchangeSide.Student, captured = PieceRole.Pawn)),
          choice(ExchangeSide.Opponent, step(ExchangeSide.Opponent, captured = PieceRole.Rook))
        )
      ),
      sequenceSummaryAllowed = true
    )

    assertEquals(payload.resultState, ResultState.StudentWins)
    assertEquals(payload.displayCount, 1)
    assertEquals(payload.displayNumberText, Some("1"))
    assert(payload.sequenceSummary.exists(_.nonEmpty))

  test("equal outcomes show blue shield with no positive or negative number"):
    val payload = Resolver.resolve(
      input(
        List(
          choice(ExchangeSide.Student, step(ExchangeSide.Student)),
          choice(ExchangeSide.Opponent, step(ExchangeSide.Opponent))
        )
      ),
      sequenceSummaryAllowed = false
    )

    assertEquals(payload.resultState, ResultState.Equal)
    assertEquals(payload.displayColor, DisplayColor.Blue)
    assertEquals(payload.displayIcon, DisplayIcon.Shield)
    assertEquals(payload.displayCount, 0)
    assertEquals(payload.displayNumberText, None)

  test("least valuable legal attacker is selected from local legal alternatives"):
    val selected = StaticExchange.chooseLeastValuableLegalAttacker(
      choice(
        ExchangeSide.Student,
        step(ExchangeSide.Student, attacker = PieceRole.Queen),
        step(ExchangeSide.Student, attacker = PieceRole.Knight),
        step(ExchangeSide.Student, attacker = PieceRole.Rook, pinned = true),
        step(ExchangeSide.Student, attacker = PieceRole.Pawn, legal = false)
      )
    )

    assertEquals(selected.map(_.attacker), Some(PieceRole.Knight))

  test("pins, king legality, occupancy, x-rays, and discovered captures are represented"):
    val pinned = step(ExchangeSide.Student, pinned = true)
    val kingIllegal = step(ExchangeSide.Student, kingLegal = false)
    val noOccupancyUpdate = step(ExchangeSide.Student, occupancyUpdated = false)
    val xray = step(ExchangeSide.Student, xray = true)

    assert(!pinned.usable)
    assert(!kingIllegal.usable)
    assert(!noOccupancyUpdate.usable)
    assert(xray.usable)
    assert(xray.usesXrayOrDiscoveredCapture)
    assert(completeBasis.readyForDeterministicLocalEstimate)

  test("resolver gates rated reveal by server authorization, L3, stale state, and reveal cap"):
    val base = input(List(choice(ExchangeSide.Student, step(ExchangeSide.Student))))

    assert(Resolver.canReveal(base))
    assert(!Resolver.canReveal(base.copy(serverAuthorized = false)))
    assert(!Resolver.canReveal(base.copy(setLevel = Level(2))))
    assert(!Resolver.canReveal(base.copy(stale = true)))
    assert(!Resolver.canReveal(base.copy(repeatedRevealCount = 3, repeatedRevealCap = 3)))

  test("unknown is grey disabled, never blue, for stale or incomplete calculations"):
    val stalePayload = Resolver.resolve(
      input(List(choice(ExchangeSide.Student, step(ExchangeSide.Student))), stale = true),
      sequenceSummaryAllowed = true
    )
    val incompletePayload = Resolver.resolve(
      input(List(choice(ExchangeSide.Student, step(ExchangeSide.Student)))).copy(
        calculationBasis = completeBasis.copy(pinsProvided = false)
      ),
      sequenceSummaryAllowed = true
    )

    assertEquals(stalePayload.resultState, ResultState.Unknown)
    assertEquals(stalePayload.displayColor, DisplayColor.Grey)
    assertEquals(stalePayload.displayIcon, DisplayIcon.Disabled)
    assertEquals(stalePayload.confidence, Confidence.Stale)
    assertEquals(incompletePayload.resultState, ResultState.Unknown)
    assertEquals(incompletePayload.displayColor, DisplayColor.Grey)
    assertEquals(incompletePayload.confidence, Confidence.Unknown)

  test("visible reveal contributes to assistance load, hidden availability does not"):
    val visible = Resolver.resolve(
      input(List(choice(ExchangeSide.Student, step(ExchangeSide.Student)))).copy(visibleReveal = true),
      sequenceSummaryAllowed = false
    )
    val hidden = Resolver.resolve(
      input(List(choice(ExchangeSide.Student, step(ExchangeSide.Student)))).copy(visibleReveal = false),
      sequenceSummaryAllowed = false
    )

    assert(AssistanceRules.visibleRevealContributesToAssistanceLoad)
    assert(!AssistanceRules.hiddenAvailabilityCountsBeforeReveal)
    assert(AssistanceRules.repeatedRevealAbuseControlsRequired)
    assert(AssistanceRules.stalePayloadsMustClear)
    assert(visible.assistanceCounts)
    assert(!hidden.assistanceCounts)
