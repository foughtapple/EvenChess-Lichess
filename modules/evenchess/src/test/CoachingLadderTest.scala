package lila.evenchess

class CoachingLadderTest extends munit.FunSuite:

  import CoachingLadder.*
  import ProductInvariants.RequirementClass

  test("ladder contains exactly L0-L10 with increasing base weights"):
    assertEquals(Levels.all.map(_.level.value), (0 to 10).toList)
    assertEquals(Levels.all.map(_.baseWeight), Levels.all.map(_.baseWeight).sorted)
    assert(Levels.all.forall(_.ratedAllowed))
    assert(LadderRules.allPublicLevelsMayBeRatedWhenAssignedOrAllowed)

  test("L0 is a non-coaching baseline and normal board capability"):
    val l0 = Levels.byValue(0)

    assertEquals(l0.name, "Standard Board")
    assertEquals(l0.baseWeight, 0)
    assert(!l0.advisoryCoaching)
    assertEquals(l0.classification, RequirementClass.LichessProvided)

  test("Offset Count is L3 and remains the Exchange Resolver feature"):
    val l3 = Levels.byValue(3)
    val offsetFeature = FeatureRegistry.byKey("offset_count")

    assertEquals(l3.name, "Offset Count")
    assertEquals(offsetFeature.unlockLevel.value, 3)
    assertEquals(offsetFeature.uiSlot, UiSlot.OffsetCard)
    assert(offsetFeature.implementationNotes.contains("Existing Exchange Resolver"))

  test("live engine-backed candidates start at L5 and remain bounded"):
    assertEquals(Levels.firstLiveEngineCandidateLevel.value, 5)
    assertEquals(Levels.byValue(4).liveEngineBackedCandidateCount, 0)
    assertEquals(Levels.byValue(5).liveEngineBackedCandidateCount, 1)
    assertEquals(Levels.byValue(6).liveEngineBackedCandidateCount, 2)
    assertEquals(Levels.byValue(7).liveEngineBackedCandidateCount, 3)
    assert(Levels.byValue(10).liveEngineBackedCandidateCount <= 4)
    assert(!LadderRules.unrestrictedRawEngineAccessAllowed)

  test("numeric eval and WDL first appear at L8 and require approximate labelling"):
    assertEquals(Levels.firstNumericEvalLevel.value, 8)
    assert(!(0 until 8).exists(Levels.byValue(_).numericEvalOrWdl))
    assert((8 to 10).forall(Levels.byValue(_).numericEvalApproximateLabelRequired))
    assertEquals(FeatureRegistry.byKey("eval_difference").unlockLevel.value, 8)
    assertEquals(FeatureRegistry.byKey("eval_difference").exactnessClass, ExactnessClass.Approximate)

  test("higher levels improve specificity and timing, not text volume"):
    assert(LadderRules.higherLevelsImproveSpecificityAndTimingNotTextVolume)
    assert(Levels.byValue(10).compactByDefault)
    assert(!LadderRules.liveRatedBestMoveLabelApproved)

  test("feature registry rows include required metadata for rated use"):
    assert(FeatureRegistry.all.nonEmpty)
    assert(FeatureRegistry.all.forall(_.featureKey.nonEmpty))
    assert(FeatureRegistry.all.forall(_.displayName.nonEmpty))
    assert(FeatureRegistry.all.forall(feature => Level.isValid(feature.unlockLevel.value)))
    assert(FeatureRegistry.all.forall(_.auditRequired))
    assert(FeatureRegistry.all.forall(_.telemetryRequired))
    assert(FeatureRegistry.all.forall(_.testsRequired.nonEmpty))

  test("rated features require server authorization, audit metadata, and level access"):
    val allowed = FeaturePolicyInput(
      featureKey = "move_advice",
      setLevel = Level(5),
      rated = true,
      serverAuthorized = true,
      clientRequested = true
    )
    val belowLevel = allowed.copy(setLevel = Level(4))
    val clientOnly = allowed.copy(serverAuthorized = false)

    assert(FeaturePolicy.canEnable(allowed))
    assert(!FeaturePolicy.canEnable(belowLevel))
    assert(!FeaturePolicy.canEnable(clientOnly))
    assert(!FeaturePolicy.canEnableFromClientOnly(clientOnly))
    assert(!FeaturePolicy.clientCanSelfEnableRatedFeatures)

  test("subscription visibility and fork adaptation cannot alter rated live strength"):
    assert(!FeaturePolicy.subscriptionVisibilityMayChangeRatedLiveStrength)
    assert(ForkAdaptation.featuresAreServerAuthorizedPayloads)
    assert(ForkAdaptation.chessgroundOverlayPayloadsRequireBoardHashFenPlyValidation)
    assert(ForkAdaptation.registryLivesInEvenChessConfigOrService)
    assert(ForkAdaptation.registryIsOfflineTestableWithPolicyInputs)
