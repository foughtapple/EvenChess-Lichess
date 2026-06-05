package lila.evenchess

class FeatureSurfacePolicyTest extends munit.FunSuite:

  import CoachingLadder.Level
  import FeatureSurfacePolicy.*
  import LiveCoaching.*
  import ProductInvariants.RequirementClass

  private def context(
      surface: FeatureSurface,
      mode: SurfaceUseMode,
      level: Level = Level(4),
      currentFen: Option[String] = Some("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
      requestedOpening: Option[Int] = Some(12),
      reviewMode: Option[ReviewMode] = None,
      customReviewPlan: Option[CustomReviewPlan] = None,
      liveHistoryAvailable: Boolean = false,
      settingAllowsOverlays: Boolean = true,
      productRuleAllowsOverlays: Boolean = true,
      puzzleRatingShown: Boolean = false,
      labelsPuzzleRatingAsEcr: Boolean = false,
      separateRatedComputerModeDefined: Boolean = false
  ) =
    FeatureSurfaceContext(
      surface = surface,
      mode = mode,
      level = level,
      currentFen = currentFen,
      requestedOpening = requestedOpening,
      reviewMode = reviewMode,
      customReviewPlan = customReviewPlan,
      liveHistoryAvailable = liveHistoryAvailable,
      settingAllowsOverlays = settingAllowsOverlays,
      productRuleAllowsOverlays = productRuleAllowsOverlays,
      puzzleRatingShown = puzzleRatingShown,
      labelsPuzzleRatingAsEcr = labelsPuzzleRatingAsEcr,
      separateRatedComputerModeDefined = separateRatedComputerModeDefined
    )

  private val customPlan =
    val request = CustomReviewRequest(
      gameId = "game-review",
      whiteLevel = Level(5),
      blackLevel = Level(4),
      perspective = CustomReviewPerspective.SideToMove,
      eceVersion = "ece-v1",
      policyVersion = "phase-o",
      useAi = false
    )
    CustomReviewPlan(
      request = request,
      cacheKey = request.cacheKey,
      requiresCustomAnalysisTokens = true,
      cachedAnalysisAllowed = true,
      mutatesLiveUsedLevel = false,
      mutatesEcrSettlement = false
    )

  test("Phase O feature-surface requirements are classified before UI adapter work"):
    val byRequirement =
      PhaseORequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseORequirement.UseLichessPuzzleFoundations), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseORequirement.UseLichessStudyFoundations), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseORequirement.UseLichessOpeningFoundations), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseORequirement.UseLichessAnalysisReplayFoundations), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseORequirement.UseLichessComputerPlayFoundations), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseORequirement.AnalysisLayersSavedOrCustomEceReview), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseORequirement.ComputerPlayDoesNotCorruptNormalEcr), RequirementClass.EvenChessSpecific)

  test("all Phase O surfaces reuse Lichess foundations and forbid rebuilds"):
    assertEquals(Foundations.all.map(_.surface).toSet, FeatureSurface.values.toSet)
    assert(Foundations.all.forall(_.reused))
    assert(Foundations.all.forall(foundation => !foundation.rebuildAllowed))
    assert(AdapterGuard.valid)
    assert(!AdapterGuard.browserMayCallEceDirectly)
    assert(AdapterGuard.futureCoreSurfaceEditsRequirePatchMap)

  test("puzzle overlays are training-only and puzzle rating stays distinct from ECR"):
    val training = FeatureSurfacePlanner.decide(
      context(FeatureSurface.PuzzleTraining, SurfaceUseMode.Training, puzzleRatingShown = true)
    )
    val live = FeatureSurfacePlanner.decide(
      context(FeatureSurface.PuzzleTraining, SurfaceUseMode.LiveRated, puzzleRatingShown = true)
    )
    val confusedRating = FeatureSurfacePlanner.decide(
      context(
        FeatureSurface.PuzzleTraining,
        SurfaceUseMode.Training,
        puzzleRatingShown = true,
        labelsPuzzleRatingAsEcr = true
      )
    )

    assert(training.valid)
    assert(training.overlayEligible)
    assert(!training.eceRequestAllowed)
    assert(!training.mutatesNormalEcr)
    assert(!live.overlayEligible)
    assert(!confusedRating.valid)
    assert(!confusedRating.puzzleRatingDistinctFromEcr)

  test("study overlays are review or training surfaces and never mutate live fairness"):
    val training = FeatureSurfacePlanner.decide(context(FeatureSurface.StudyBoard, SurfaceUseMode.Training))
    val review = FeatureSurfacePlanner.decide(context(FeatureSurface.StudyBoard, SurfaceUseMode.Review))
    val live = FeatureSurfacePlanner.decide(context(FeatureSurface.StudyBoard, SurfaceUseMode.LiveRated))

    assert(training.valid)
    assert(review.valid)
    assert(training.overlayEligible)
    assert(review.overlayEligible)
    assert(!training.eceRequestAllowed)
    assert(!review.mutatesLiveFairnessState)
    assert(!review.mutatesNormalEcr)
    assert(!live.overlayEligible)

  test("opening guidance requires current FEN requested opening input and L4 plus compact output"):
    val allowed = FeatureSurfacePlanner.decide(
      context(FeatureSurface.OpeningExplorer, SurfaceUseMode.Training, level = Level(4), requestedOpening = Some(12))
    )
    val lowLevel = FeatureSurfacePlanner.decide(
      context(FeatureSurface.OpeningExplorer, SurfaceUseMode.Training, level = Level(3), requestedOpening = Some(12))
    )
    val missingFen = FeatureSurfacePlanner.decide(
      context(FeatureSurface.OpeningExplorer, SurfaceUseMode.Training, currentFen = None, requestedOpening = Some(12))
    )
    val noOpening = FeatureSurfacePlanner.decide(
      context(FeatureSurface.OpeningExplorer, SurfaceUseMode.Training, requestedOpening = Some(0))
    )

    assert(allowed.valid)
    assert(allowed.overlayEligible)
    assert(allowed.eceRequestAllowed)
    assert(allowed.compactGuidanceRequired)
    assertEquals(allowed.eceCustomProfile, EngineGateway.EceCustomProfile(opening = 12, instructions = 0))
    assert(!lowLevel.overlayEligible)
    assert(!missingFen.overlayEligible)
    assert(!noOpening.overlayEligible)

  test("analysis and replay layer saved history review modes or valid custom review plans"):
    val liveBoth = FeatureSurfacePlanner.decide(
      context(
        FeatureSurface.AnalysisReplay,
        SurfaceUseMode.Review,
        reviewMode = Some(ReviewMode.LiveBoth),
        liveHistoryAvailable = true
      )
    )
    val custom = FeatureSurfacePlanner.decide(
      context(FeatureSurface.AnalysisReplay, SurfaceUseMode.Review, customReviewPlan = Some(customPlan))
    )
    val missingReviewSource = FeatureSurfacePlanner.decide(
      context(FeatureSurface.AnalysisReplay, SurfaceUseMode.Review)
    )
    val liveRated = FeatureSurfacePlanner.decide(
      context(FeatureSurface.AnalysisReplay, SurfaceUseMode.LiveRated, reviewMode = Some(ReviewMode.LiveWhite))
    )

    assert(liveBoth.valid)
    assert(liveBoth.overlayEligible)
    assert(!liveBoth.eceRequestAllowed)
    assert(custom.valid)
    assert(custom.overlayEligible)
    assert(custom.eceRequestAllowed)
    assert(!custom.mutatesNormalEcr)
    assert(!missingReviewSource.overlayEligible)
    assert(!liveRated.overlayEligible)

  test("computer play is training or review by default and cannot corrupt normal ECR"):
    val training = FeatureSurfacePlanner.decide(context(FeatureSurface.ComputerPlay, SurfaceUseMode.ComputerTraining))
    val review = FeatureSurfacePlanner.decide(context(FeatureSurface.ComputerPlay, SurfaceUseMode.Review))
    val liveRated = FeatureSurfacePlanner.decide(context(FeatureSurface.ComputerPlay, SurfaceUseMode.LiveRated))
    val separateRated = FeatureSurfacePlanner.decide(
      context(
        FeatureSurface.ComputerPlay,
        SurfaceUseMode.ComputerTraining,
        separateRatedComputerModeDefined = true
      )
    )

    assert(training.valid)
    assert(training.overlayEligible)
    assert(training.eceRequestAllowed)
    assert(review.overlayEligible)
    assert(!review.mutatesNormalEcr)
    assert(!review.mutatesLiveFairnessState)
    assert(!liveRated.overlayEligible)
    assert(!separateRated.overlayEligible)

  test("settings and product rules can suppress all feature-surface overlays"):
    val settingsOff = FeatureSurfacePlanner.decide(
      context(FeatureSurface.OpeningExplorer, SurfaceUseMode.Training, settingAllowsOverlays = false)
    )
    val productOff = FeatureSurfacePlanner.decide(
      context(FeatureSurface.AnalysisReplay, SurfaceUseMode.Review, reviewMode = Some(ReviewMode.LiveBlack), productRuleAllowsOverlays = false)
    )

    assert(settingsOff.valid)
    assert(!settingsOff.overlayEligible)
    assert(!settingsOff.eceRequestAllowed)
    assertEquals(settingsOff.reason, "settings_or_product_rules_disable_overlays")
    assert(productOff.valid)
    assert(!productOff.overlayEligible)
    assert(!productOff.eceRequestAllowed)
