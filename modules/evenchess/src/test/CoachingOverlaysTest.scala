package lila.evenchess

class CoachingOverlaysTest extends munit.FunSuite:

  import CoachingLadder.Level
  import CoachingOverlays.*
  import ProductInvariants.RequirementClass

  private val validPayload = OverlayPayload(
    gameId = "game-1",
    ply = 12,
    boardStateKey = "fen-hash-12",
    perspective = Perspective.White,
    featureKey = "legal_targets",
    level = Level(1),
    visibility = OverlayVisibility.Visible,
    ttlMillis = 1500,
    stale = false,
    auditId = "audit-1",
    serverAuthorized = true,
    approvedDisplayPayload = true,
    rawStockfishLine = None,
    hiddenDebugData = None
  )

  private val matchingContext = BoardStateContext(
    gameId = "game-1",
    ply = 12,
    boardStateKey = "fen-hash-12",
    perspective = Perspective.White
  )

  private def sideOutput(level: Level = Level(4), side: Perspective = Perspective.White) =
    val opponent = if side == Perspective.White then Perspective.Black else Perspective.White
    EngineGateway.EceSideOutput(
      side = side,
      studentSide = side,
      opponentSide = opponent,
      level = EngineGateway.EceLevelEcho(level, level, defaulted = false),
      isSideToMove = true,
      summary = Some("The knight on e4 is loose."),
      immediateWarning = Some("Watch the fork on c7."),
      plan = Some("Castle and contest the open file.")
    )

  private def displayRequest(
      level: Level = Level(4),
      side: Perspective = Perspective.White,
      requesterSide: Perspective = Perspective.White,
      displayMode: DisplayMode = DisplayMode.ActualPosition,
      stale: Boolean = false,
      ttlMillis: Int = 1500
  ) =
    DisplayEngineRequest(
      gameId = "game-1",
      playerId = "white-user",
      ply = 12,
      boardStateKey = "fen-hash-12",
      requesterSide = requesterSide,
      sideOutput = sideOutput(level, side),
      authorizedLevel = level,
      displayMode = displayMode,
      atoms = MockDisplayOverlayAtoms(
        hangingAttackable = Nil,
        hangingNotAttackable = List("a2"),
        offsetCount = List(("d5", 1), ("f7", -1), ("c3", 0)),
        studentThreats = List("e4" -> "f6"),
        opponentThreats = List("c5" -> "f2"),
        pins = List(("f3", "b7", "g2")),
        studentHangingAttackable = List("e4"),
        opponentHangingAttackable = List("h5")
      ),
      auditId = "audit-display-1",
      ttlMillis = ttlMillis,
      stale = stale,
      serverAuthorized = true
    )

  test("stable surface catalog keeps board first and higher help collapsed by default"):
    assertEquals(Surfaces.bySurface(StableSurface.BoardLayer).classification, RequirementClass.LichessProvided)
    assert(Surfaces.bySurface(StableSurface.BoardLayer).defaultVisible)
    assert(!Surfaces.bySurface(StableSurface.CandidateArea).defaultVisible)
    assert(Surfaces.all.forall(_.maxDefaultActiveCards <= 1))
    assert(UiPrinciples.boardFirst)
    assert(UiPrinciples.oneActiveCardByDefault)
    assert(UiPrinciples.onePrimaryVisualIdeaByDefault)

  test("Display Engine requirements are classified before live display integration"):
    val byRequirement =
      DisplayRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(DisplayRequirement.PlayerSideOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(DisplayRequirement.NoSideSwitchingLive), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(DisplayRequirement.ActualPositionVsProposedPreviewDistinct), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(DisplayRequirement.StalePayloadsDoNotRenderCurrentAdvice), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(DisplayRequirement.LevelGatedCardsAndOverlays), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(DisplayRequirement.CompactTextBudgets), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(DisplayRequirement.DeterministicOverlayMapping), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(DisplayRequirement.DisplayEngineFrameworkOnly), RequirementClass.EvenChessSpecific)

  test("text budgets are schema-enforced and reject oversized live text"):
    assert(TextBudgets.enforcedBySchemaNotAiDiscretion)
    assert(TextBudgets.withinBudget(StableSurface.SummaryCard, "Safe king, equal material.", lineCount = 1))
    assert(!TextBudgets.withinBudget(StableSurface.CandidateArea, "x" * 81, lineCount = 1))
    assert(!TextBudgets.withinBudget(StableSurface.WarningCard, "short", lineCount = 3))

  test("accessibility rules reject color-only signalling and protect baseline accessibility"):
    assert(!UiPrinciples.colorMayBeOnlySignal)
    assert(!UiPrinciples.accessibilityFeaturesChargedAsCoaching)
    assert(!AccessibilityRules.hasRedundantSignal(Set(VisualSignal.Color)))
    assert(AccessibilityRules.hasRedundantSignal(Set(VisualSignal.Color, VisualSignal.Icon)))
    assert(AccessibilityRules.offsetCountHasRequiredRedundancy(Set(VisualSignal.Color, VisualSignal.Icon, VisualSignal.Text)))

  test("desktop and mobile layout contracts preserve board-primary play"):
    assertEquals(LiveLayouts.desktop.layoutShape, "three-column rectangle")
    assertEquals(LiveLayouts.desktop.levelControlsPosition, "left")
    assertEquals(LiveLayouts.desktop.boardPosition, "centre")
    assert(LiveLayouts.desktop.sideRailContents.contains("Moves"))
    assert(LiveLayouts.desktop.sideRailContents.contains("Coach"))
    assert(LiveLayouts.mobile.boardPrimary)
    assert(LiveLayouts.mobile.sideRailContents.contains("Level"))
    assert(!LiveLayouts.desktop.keyboardMoveEntryDefaultOnForNewAccounts)
    assert(LiveLayouts.evalBarNeedsAccessibleAlternative)

  test("overlay payloads require server authority, identity fields, freshness, and audit id"):
    assert(validPayload.hasRequiredIdentityFields)
    assert(validPayload.isRenderable)
    assert(!validPayload.copy(serverAuthorized = false).isRenderable)
    assert(!validPayload.copy(auditId = "").isRenderable)
    assert(!validPayload.copy(stale = true).isRenderable)
    assert(!validPayload.copy(ttlMillis = 0).isRenderable)
    assert(!validPayload.copy(visibility = OverlayVisibility.Suppressed).isRenderable)

  test("overlays clear on move, mismatch, stale payload, suppression, or expiry"):
    assertEquals(OverlayInvalidation.clearReason(validPayload, matchingContext, movePlayed = false), ClearReason.None)
    assertEquals(OverlayInvalidation.clearReason(validPayload, matchingContext, movePlayed = true), ClearReason.MovePlayed)
    assertEquals(
      OverlayInvalidation.clearReason(validPayload.copy(stale = true), matchingContext, movePlayed = false),
      ClearReason.StalePayload
    )
    assertEquals(
      OverlayInvalidation.clearReason(validPayload.copy(visibility = OverlayVisibility.Suppressed), matchingContext, movePlayed = false),
      ClearReason.Suppressed
    )
    assertEquals(
      OverlayInvalidation.clearReason(validPayload.copy(ttlMillis = 0), matchingContext, movePlayed = false),
      ClearReason.PayloadExpired
    )
    assertEquals(
      OverlayInvalidation.clearReason(validPayload, matchingContext.copy(boardStateKey = "different"), movePlayed = false),
      ClearReason.BoardMismatch
    )

  test("client cannot construct stronger help from hidden data or raw engine lines"):
    val hidden = validPayload.copy(hiddenDebugData = Some("debug pv"))
    val rawLineUnapproved = validPayload.copy(approvedDisplayPayload = false, rawStockfishLine = Some("e2e4 e7e5"))

    assert(!UiPrinciples.clientNavigationIsAuthoritative)
    assert(!ClientCompositionGuard.clientMayConstructStrongerHelpFromHiddenData)
    assert(!ClientCompositionGuard.rawStockfishLinesMayDisplayWithoutServerPayload)
    assert(!ClientCompositionGuard.overlaysMayInterfereWithMoveInput)
    assert(ClientCompositionGuard.blocksStrongerHelp(List(validPayload, hidden)))
    assert(!hidden.hiddenDebugMayDisplay)
    assert(!rawLineUnapproved.rawStockfishMayDisplay)

  test("landing and review surfaces are config-driven and fairness-neutral"):
    val sectionKeys = LandingSurfaces.requiredSections.map(_.key).toSet

    assert(sectionKeys.contains("hero"))
    assert(sectionKeys.contains("pricing"))
    assert(sectionKeys.contains("faq"))
    assert(LandingSurfaces.allCopyComesFromMarketingConfig)
    assert(LandingSurfaces.topCreateAccountRequired)
    assert(LandingSurfaces.topLoginRequired)
    assert(LandingSurfaces.pricingShowsFourWeekAmountFirst)
    assert(LandingSurfaces.pricingShowsWeeklyEquivalentSecond)
    assert(LandingSurfaces.pricingIncludesFairnessFootnote)
    assert(ReviewSurfaces.resultAndTerminationAtTop)
    assert(ReviewSurfaces.behavesLikePostGameBoardExperience)
    assert(ReviewSurfaces.reviewLegalCoachingMustNotMutateLiveFairnessState)

  test("display engine compiles approved side output into renderable cards, markers, and arrows"):
    val result = DisplayEngine.compile(displayRequest(level = Level(4)))

    assert(result.renderable)
    assertEquals(result.modeBadge, "Current position")
    assert(result.overlay.exists(_.isRenderable))
    assert(result.cards.exists(_.surface == StableSurface.SummaryCard))
    assert(result.cards.exists(_.surface == StableSurface.WarningCard))
    assert(result.cards.exists(_.surface == StableSurface.PlanCard))
    assert(result.cards.forall(_.valid))
    assert(result.markers.exists(_.kind == MarkerKind.StudentHangingAttackable))
    assert(result.markers.exists(_.kind == MarkerKind.OpponentHangingAttackable))
    assert(result.markers.exists(_.kind == MarkerKind.HangingNotAttackable))
    assert(result.markers.exists(_.kind == MarkerKind.OffsetStudentFavorable))
    assert(result.markers.exists(_.kind == MarkerKind.OffsetStudentUnfavorable))
    assert(result.markers.exists(_.kind == MarkerKind.OffsetEqual))
    assert(result.markers.exists(_.kind == MarkerKind.Pin))
    assert(result.arrows.exists(_.kind == ArrowKind.StudentThreat))
    assert(result.arrows.exists(_.kind == ArrowKind.OpponentThreat))
    assert(result.arrows.find(_.kind == ArrowKind.OpponentThreat).exists(_.layer == 2))
    assert(!result.clientMaySwitchSide)
    assert(!result.rawEcePayloadExposed)

  test("display engine level gates cards and deterministic overlay atoms"):
    val l2 = DisplayEngine.compile(displayRequest(level = Level(2)))
    val l3 = DisplayEngine.compile(displayRequest(level = Level(3)))
    val l5 = DisplayEngine.compile(displayRequest(level = Level(5)))

    assert(l2.renderable)
    assert(!l2.cards.exists(_.surface == StableSurface.SummaryCard))
    assert(l2.cards.exists(_.surface == StableSurface.WarningCard))
    assert(l2.markers.exists(_.kind == MarkerKind.StudentHangingAttackable))
    assert(l2.markers.exists(_.kind == MarkerKind.OpponentHangingAttackable))
    assert(!l2.markers.exists(_.kind == MarkerKind.OffsetStudentFavorable))
    assert(l3.markers.exists(_.kind == MarkerKind.OffsetStudentFavorable))
    assert(LevelDisplayGates.surfaceAllowed(StableSurface.CandidateArea, Level(5)))
    assert(!LevelDisplayGates.surfaceAllowed(StableSurface.CandidateArea, Level(4)))
    assert(l5.renderable)

  test("display engine rejects side switching, stale payloads, expired payloads, and oversized text"):
    val sideSwitch = DisplayEngine.compile(displayRequest(side = Perspective.Black, requesterSide = Perspective.White))
    val stale = DisplayEngine.compile(displayRequest(stale = true))
    val expired = DisplayEngine.compile(displayRequest(ttlMillis = 0))
    val longOutput = sideOutput(Level(4)).copy(summary = Some("x" * 121))
    val oversized =
      DisplayEngine.compile(displayRequest(level = Level(4)).copy(sideOutput = longOutput))

    assert(sideSwitch.clearOnly)
    assertEquals(sideSwitch.clearReason, ClearReason.BoardMismatch)
    assert(stale.clearOnly)
    assertEquals(stale.clearReason, ClearReason.StalePayload)
    assert(expired.clearOnly)
    assertEquals(expired.clearReason, ClearReason.PayloadExpired)
    assert(oversized.renderable)
    assert(!oversized.cards.exists(_.surface == StableSurface.SummaryCard))

  test("display engine distinguishes actual position output from proposed-move preview output"):
    val actual = DisplayEngine.compile(displayRequest(displayMode = DisplayMode.ActualPosition))
    val preview = DisplayEngine.compile(displayRequest(displayMode = DisplayMode.ProposedMovePreview))

    assertEquals(actual.modeBadge, "Current position")
    assertEquals(preview.modeBadge, "Proposed move preview")
    assertEquals(actual.overlay.map(_.featureKey), Some("display_engine.actual_position"))
    assertEquals(preview.overlay.map(_.featureKey), Some("display_engine.proposed_move_preview"))
