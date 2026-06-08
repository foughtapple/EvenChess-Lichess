package lila.evenchess

class LiveOverlayUiTest extends munit.FunSuite:

  import CoachingLadder.Level
  import CoachingOverlays.*
  import LiveCoaching.*
  import LiveOverlayUi.*
  import OffsetCount.*
  import ProductInvariants.RequirementClass

  private val overlay = OverlayPayload(
    gameId = "game-ui",
    ply = 22,
    boardStateKey = "board-key-22",
    perspective = Perspective.White,
    featureKey = OffsetCount.featureKey,
    level = Level(3),
    visibility = OverlayVisibility.Visible,
    ttlMillis = 5000,
    stale = false,
    auditId = "audit-ui-1",
    serverAuthorized = true,
    approvedDisplayPayload = true,
    rawStockfishLine = None,
    hiddenDebugData = None
  )

  private val envelope = LiveTransportEnvelope(
    gameId = overlay.gameId,
    playerId = "white-user",
    ply = overlay.ply,
    boardStateKey = overlay.boardStateKey,
    perspective = overlay.perspective,
    overlay = Some(overlay),
    clearReason = ClearReason.None,
    auditId = overlay.auditId,
    serverAuthorized = true
  )

  private def offsetPayload(
      state: ResultState = ResultState.Equal,
      displayCount: Int = 0,
      assistanceCounts: Boolean = true
  ) =
    val semantics = state match
      case ResultState.Equal => DisplaySemantics.forDelta(0)
      case ResultState.StudentWins => DisplaySemantics.forDelta(displayCount.max(1))
      case ResultState.OpponentWins => DisplaySemantics.forDelta(-displayCount.max(1))
      case ResultState.Unknown => DisplaySemantics.unknown

    OffsetCountPayload(
      featureKey = OffsetCount.featureKey,
      gameId = overlay.gameId,
      ply = overlay.ply,
      boardStateKey = overlay.boardStateKey,
      square = "e4",
      initialMove = Some("Nxe4"),
      resultState = semantics.resultState,
      displayColor = semantics.color,
      displayIcon = semantics.icon,
      displayCount = semantics.displayCount,
      sequenceSummary = None,
      confidence =
        if semantics.resultState == ResultState.Unknown then Confidence.Unknown
        else Confidence.DeterministicLocal,
      auditId = overlay.auditId,
      assistanceCounts = assistanceCounts,
      serverAuthorized = true
    )

  test("Phase F requirements are classified before overlay UI work"):
    val byRequirement =
      PhaseFRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseFRequirement.BaseBoardUiProvidedByLichess), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseFRequirement.ServerAuthorizedOverlayCards), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseFRequirement.TextBudgetsSchemaEnforced), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseFRequirement.OffsetCountCardSemantics), RequirementClass.AdaptedToLichessFork)
    assertEquals(
      byRequirement(PhaseFRequirement.CoreUiAdapterDeferredToThinSeams),
      RequirementClass.AdaptedToLichessFork
    )

  test("Offset Count has a stable card surface with schema text budget and accessible signals"):
    val surface = Surfaces.bySurface(StableSurface.OffsetCountCard)

    assertEquals(surface.uiSlot, CoachingLadder.UiSlot.OffsetCard)
    assertEquals(surface.classification, RequirementClass.AdaptedToLichessFork)
    assert(!surface.defaultVisible)
    assert(surface.maxDefaultActiveCards <= 1)
    assert(TextBudgets.withinBudget(StableSurface.OffsetCountCard, "Equal trade", lineCount = 1))
    assert(!TextBudgets.withinBudget(StableSurface.OffsetCountCard, "x" * 97, lineCount = 1))
    assert(AccessibilityRules.offsetCountHasRequiredRedundancy(Set(VisualSignal.Color, VisualSignal.Icon, VisualSignal.Text)))

  test("Offset Count card semantics match shield, student-wins, opponent-wins, and unknown states"):
    val equal = OffsetCountCard.fromPayload(offsetPayload(), overlay).getOrElse(fail("Expected equal card"))
    val student =
      OffsetCountCard.fromPayload(offsetPayload(ResultState.StudentWins, displayCount = 2), overlay)
        .getOrElse(fail("Expected student-wins card"))
    val opponent =
      OffsetCountCard.fromPayload(offsetPayload(ResultState.OpponentWins, displayCount = 1), overlay)
        .getOrElse(fail("Expected opponent-wins card"))
    val unknown =
      OffsetCountCard.fromPayload(offsetPayload(ResultState.Unknown, assistanceCounts = false), overlay)
        .getOrElse(fail("Expected unknown card"))

    assertEquals(equal.body, "Equal trade")
    assertEquals(equal.displayState, CardDisplayState.Collapsed)
    assertEquals(equal.signals, Set(VisualSignal.Color, VisualSignal.Icon, VisualSignal.Text))
    assert(equal.assistanceCounts)

    assertEquals(student.body, "You win 2 pieces")
    assert(!student.body.contains("+"))
    assertEquals(opponent.body, "Opponent wins 1 piece")
    assert(!opponent.body.contains("+"))
    assertEquals(unknown.body, "Exchange unknown")
    assertEquals(unknown.displayState, CardDisplayState.Disabled)
    assert(!unknown.assistanceCounts)

  test("frame composer creates one active server-authorized Offset Count card"):
    val frame = FrameComposer.fromTransport(envelope, Some(offsetPayload(ResultState.StudentWins, displayCount = 2)))

    assert(frame.valid)
    assertEquals(frame.cards.size, 1)
    assertEquals(frame.visuals, Nil)
    assertEquals(frame.activeCards.size, 1)
    assert(DisplayGuards.frameKeepsDefaultLimits(frame))
    assert(frame.clientNavigationDisplayOnly)

    val card = frame.cards.head
    assert(card.renderable)
    assertEquals(card.gameId, overlay.gameId)
    assertEquals(card.auditId, overlay.auditId)
    assertEquals(card.featureKey, OffsetCount.featureKey)
    assertEquals(card.surface, StableSurface.OffsetCountCard)
    assert(card.hasAccessibleSignals)
    assert(card.withinTextBudget)

  test("clear-only transport creates clear instructions instead of cards"):
    val clearEnvelope = envelope.copy(
      overlay = None,
      clearReason = ClearReason.StalePayload
    )
    val frame = FrameComposer.fromTransport(clearEnvelope)

    assert(frame.valid)
    assertEquals(frame.cards, Nil)
    assertEquals(frame.visuals, Nil)
    assertEquals(frame.clearInstructions.size, 1)
    assertEquals(frame.clearInstructions.head.reason, ClearReason.StalePayload)
    assertEquals(frame.clearInstructions.head.auditId, overlay.auditId)

  test("unapproved, mismatched, or missing Offset Count payloads suppress client rendering"):
    val unapproved = envelope.copy(overlay = Some(overlay.copy(serverAuthorized = false)))
    val mismatchedPayload = offsetPayload().copy(auditId = "other-audit")

    val noPayloadFrame = FrameComposer.fromTransport(envelope, None)
    val unapprovedFrame = FrameComposer.fromTransport(unapproved, Some(offsetPayload()))
    val mismatchedFrame = FrameComposer.fromTransport(envelope, Some(mismatchedPayload))

    assertEquals(noPayloadFrame.cards, Nil)
    assertEquals(noPayloadFrame.clearInstructions.head.reason, ClearReason.Suppressed)
    assertEquals(unapprovedFrame.cards, Nil)
    assertEquals(unapprovedFrame.clearInstructions.head.reason, ClearReason.Suppressed)
    assertEquals(mismatchedFrame.cards, Nil)
    assertEquals(mismatchedFrame.clearInstructions.head.reason, ClearReason.Suppressed)

  test("board-layer overlays become non-interfering board visuals, not coaching cards"):
    val legalTargetsOverlay = overlay.copy(featureKey = "legal_targets", level = Level(1), auditId = "audit-legal-1")
    val frame = FrameComposer.fromTransport(
      envelope.copy(
        overlay = Some(legalTargetsOverlay),
        auditId = legalTargetsOverlay.auditId
      )
    )

    assert(frame.valid)
    assertEquals(frame.cards, Nil)
    assertEquals(frame.visuals.size, 1)
    assertEquals(frame.primaryVisuals.size, 1)
    assert(frame.visuals.head.renderable)
    assert(!ClientCompositionGuard.overlaysMayInterfereWithMoveInput)

  test("card renderability blocks oversized text and color-only signalling"):
    val oversized = OffsetCountCard
      .fromPayload(offsetPayload(ResultState.StudentWins, displayCount = 2), overlay)
      .getOrElse(fail("Expected card"))
      .copy(body = "x" * 97)
    val colorOnly = oversized.copy(body = "Short", signals = Set(VisualSignal.Color))

    assert(!oversized.withinTextBudget)
    assert(!oversized.renderable)
    assert(!colorOnly.hasAccessibleSignals)
    assert(!colorOnly.renderable)

  test("coaching cards suppress unsafe transport and keep raw engine data out of display bodies"):
    val unsafeOverlay = overlay.copy(
      featureKey = "candidate_cards",
      level = Level(5),
      auditId = "audit-candidate-1",
      rawStockfishLine = Some("pv e2e4 e7e5"),
      hiddenDebugData = Some("debug multipv")
    )
    val unsafeFrame = FrameComposer.fromTransport(
      envelope.copy(
        overlay = Some(unsafeOverlay),
        auditId = unsafeOverlay.auditId
      )
    )
    val safeOverlay = unsafeOverlay.copy(rawStockfishLine = None, hiddenDebugData = None)
    val frame = FrameComposer.fromTransport(
      envelope.copy(
        overlay = Some(safeOverlay),
        auditId = safeOverlay.auditId
      )
    )

    assertEquals(unsafeFrame.cards, Nil)
    assertEquals(unsafeFrame.clearInstructions.head.reason, ClearReason.Suppressed)
    assert(frame.valid)
    assertEquals(frame.cards.size, 1)
    assert(DisplayGuards.liveCardsDoNotShowRawStockfishLines)
    assert(DisplayGuards.hiddenDebugDataNotRendered)
    assert(DisplayGuards.cardsKeepRawEngineDataOut(frame.cards))
    assert(!frame.cards.head.body.toLowerCase.contains("pv"))
    assert(!frame.cards.head.body.toLowerCase.contains("multipv"))
