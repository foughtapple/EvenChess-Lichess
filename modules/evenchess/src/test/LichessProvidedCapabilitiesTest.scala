package lila.evenchess

class LichessProvidedCapabilitiesTest extends munit.FunSuite:

  import LichessProvidedCapabilities.*
  import ProductInvariants.RequirementClass

  test("classifies platform basics as Lichess-provided or adapted, not EvenChess-owned rebuilds"):
    val legalMoves = Capabilities.byKey("legal_move_generation")
    val boardUi = Capabilities.byKey("board_ui")
    val clocks = Capabilities.byKey("clocks_time_controls")

    assertEquals(legalMoves.classification, RequirementClass.LichessProvided)
    assertEquals(boardUi.classification, RequirementClass.LichessProvided)
    assertEquals(clocks.classification, RequirementClass.AdaptedToLichessFork)
    assert(legalMoves.rebuildForbidden)
    assert(boardUi.rebuildForbidden)
    assert(clocks.rebuildForbidden)

  test("normal Lichess ratings remain separate from ECR"):
    val ratings = Capabilities.byKey("normal_ratings")

    assertEquals(ratings.classification, RequirementClass.LichessProvided)
    assert(ratings.evenChessAction.contains("Do not use as ECR"))
    assert(!RatingSeparation.normalLichessRatingsAreEcr)
    assert(!RatingSeparation.normalRatingsMayBeCorruptedByEvenChess)

  test("records Appendix C superseded custom-platform requirements"):
    val supersededIds = SupersededRequirements.all.map(_.id).toSet
    val supersededDescriptions = SupersededRequirements.all.map(_.description).mkString(" ")

    assertEquals(
      supersededIds,
      Set("C-L1-001", "C-L1-002", "C-L1-003", "C-L1-004", "C-L1-005", "C-L1-006")
    )
    assert(supersededDescriptions.contains("Custom chess server"))
    assert(supersededDescriptions.contains("Custom legal move generation"))
    assert(supersededDescriptions.contains("Custom primary board renderer"))
    assert(supersededDescriptions.contains("Replacing normal Lichess chess"))

  test("records gap verification areas without treating Lichess foundations as missing"):
    assertEquals(
      GapVerification.byKey("account_creation").classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      GapVerification.byKey("rating_pools").classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      GapVerification.byKey("engine_analysis").classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(GapVerification.byKey("payment").classification, RequirementClass.EvenChessSpecific)
    assertEquals(
      GapVerification.byKey("studies_lessons").classification,
      RequirementClass.UnresolvedProductOwnerDecision
    )
    assert(GapVerification.byKey("engine_analysis").evenChessGap.contains("server-authorized assistance"))

  test("records adapted requirements as Lichess seams"):
    val adaptedKeys = AdaptedRequirements.all.map(_.key).toSet

    assert(adaptedKeys.contains("time_controls"))
    assert(adaptedKeys.contains("matchmaking"))
    assert(adaptedKeys.contains("accounts"))
    assert(adaptedKeys.contains("review_summaries"))
    assert(adaptedKeys.contains("operations"))
    assert(AdaptedRequirements.all.forall(_.lichessSeam.nonEmpty))

  test("platform infrastructure guard stops work when no specific gap is identified"):
    assertEquals(
      PlatformInfrastructureGuard.decide("legal_move_generation", specificGapIdentified = false),
      InfrastructureDecision.StopNoSpecificGap
    )
    assertEquals(
      PlatformInfrastructureGuard.decide("challenge_seek_matchmaking", specificGapIdentified = true),
      InfrastructureDecision.AdaptExistingLichessSeam
    )
    assertEquals(
      PlatformInfrastructureGuard.decide("unknown_platform_area", specificGapIdentified = false),
      InfrastructureDecision.UnknownCapabilityInspectBeforeCoding
    )
    assert(!PlatformInfrastructureGuard.mayContinueWithAdaptedSeam("board_ui", specificGapIdentified = false))
    assert(PlatformInfrastructureGuard.mustStateWhyLichessDoesNotProvideItBeforeCoding)
