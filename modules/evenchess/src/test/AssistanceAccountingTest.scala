package lila.evenchess

class AssistanceAccountingTest extends munit.FunSuite:

  import AssistanceAccounting.*
  import CoachingLadder.*
  import CoachingOverlays.OverlayVisibility
  import CoachingPolicy.*
  import EvenChessMode.*

  private val calibration = CalibrationParameters.default

  private def policyInput(
      playerId: String = "player-1",
      featureKey: String = "move_advice",
      setLevel: Level = Level(5),
      currentUsedLevel: Level = Level(0),
      requestType: CoachingRequestType = CoachingRequestType.Display
  ) =
    val row = FeatureRegistry.byKey(featureKey)
    PolicyInput(
      gameId = "game-1",
      playerId = playerId,
      mode = GameMode.EvenChess,
      rated = true,
      timeControlBucket = TimeControlBucket.Rapid,
      ply = 12,
      boardStateKey = "board-12",
      clockContext = ClockContext(
        millisRemaining = 30000,
        incrementMillis = 2000,
        premoveCommitted = false,
        staleForDecision = false
      ),
      setLevel = setLevel,
      currentUsedLevel = currentUsedLevel,
      requestedFeature = featureKey,
      requestType = requestType,
      registryRow = row,
      exactnessClass = row.exactnessClass,
      abuseState = AbuseState.Clear,
      engineHealth = ServiceHealth.Healthy,
      aiHealth = ServiceHealth.Healthy,
      policyVersion = "policy-v1"
    )

  private def event(
      eventId: String,
      playerId: String = "player-1",
      featureKey: String = "move_advice",
      setLevel: Level = Level(5),
      currentUsedLevel: Level = Level(0),
      requestType: CoachingRequestType = CoachingRequestType.Display
  ) =
    val input = policyInput(playerId, featureKey, setLevel, currentUsedLevel, requestType)
    AuditEvent.fromDecision(eventId, input, PolicyEngine.decide(input), createdAt = 123456789L)

  test("Used Level starts at L0 and never decreases"):
    val initial = UsedLevelState.initial("player-1")
    val l5 = UsedLevelState.afterEvent(initial, event("e1", featureKey = "move_advice"), AssistanceDimensions.defaultLive)
    val l1 = UsedLevelState.afterEvent(l5, event("e2", featureKey = "legal_targets"), AssistanceDimensions.defaultLive)

    assertEquals(initial.usedLevel.value, 0)
    assertEquals(l5.usedLevel.value, 5)
    assertEquals(l1.usedLevel.value, 5)

  test("hidden prefetch and stale non-decision help do not count as Used Level or load"):
    val hiddenEvent = event("hidden", requestType = CoachingRequestType.Request)
    val staleDimensions = AssistanceDimensions.defaultLive.copy(staleNonDecisionHelp = true)
    val visibleEvent = event("visible")

    val hiddenLevel = UsedLevelState.afterEvent(UsedLevelState.initial("player-1"), hiddenEvent, AssistanceDimensions.defaultLive)
    val staleLevel = UsedLevelState.afterEvent(UsedLevelState.initial("player-1"), visibleEvent, staleDimensions)
    val hiddenLoad = AssistanceLoadFormula.fromAuditEvent(hiddenEvent, AssistanceDimensions.defaultLive, calibration)
    val staleLoad = AssistanceLoadFormula.fromAuditEvent(visibleEvent, staleDimensions, calibration)

    assertEquals(hiddenLevel.usedLevel.value, 0)
    assertEquals(staleLevel.usedLevel.value, 0)
    assertEquals(hiddenLoad.value, 0.0)
    assertEquals(staleLoad.value, 0.0)

  test("Assistance Load uses feature weight and all configured multipliers"):
    val audit = event("weighted")
    val dimensions = AssistanceDimensions.defaultLive.copy(
      criticality = Criticality.TacticalSwing,
      clockPressure = ClockPressure.LowTime,
      followRate = FollowRate.Followed,
      quality = Quality.Normal
    )
    val load = AssistanceLoadFormula.fromAuditEvent(audit, dimensions, calibration)
    val expected =
      audit.assistanceWeightDelta *
        calibration.exactnessMultipliers(audit.exactnessClass) *
        calibration.surfaceMultipliers(audit.surface) *
        calibration.timeControlMultipliers(dimensions.timeControl) *
        calibration.criticalityMultipliers(dimensions.criticality) *
        calibration.clockPressureMultipliers(dimensions.clockPressure) *
        calibration.visibilityMultipliers(audit.visibility) *
        calibration.followMultipliers(dimensions.followRate) *
        calibration.qualityMultipliers(dimensions.quality)

    assertEquals(load.value, expected)
    assert(load.value > audit.assistanceWeightDelta)

  test("post-game review cannot mutate live Used Level or live load"):
    val reviewInput = policyInput(featureKey = "post_game_review", setLevel = Level(5))
    val reviewEvent = AuditEvent.fromDecision(
      "review",
      reviewInput,
      PolicyEngine.decide(reviewInput),
      createdAt = 123456789L
    )
    val reviewDimensions = AssistanceDimensions.defaultLive.copy(postGameReview = true)
    val state = UsedLevelState.afterEvent(UsedLevelState.initial("player-1"), reviewEvent, reviewDimensions)
    val load = AssistanceLoadFormula.fromAuditEvent(reviewEvent, reviewDimensions, calibration)

    assertEquals(state.usedLevel.value, 0)
    assertEquals(load.value, 0.0)

  test("offline recomputation separates players and aggregates only their ledger events"):
    val p1e1 = event("p1-e1", playerId = "player-1", featureKey = "legal_targets")
    val p1e2 = event("p1-e2", playerId = "player-1", featureKey = "move_advice")
    val p2e1 = event("p2-e1", playerId = "player-2", featureKey = "candidate_cards", setLevel = Level(5))
    val ledger = AssistanceLedger.empty.append(p1e1).append(p1e2).append(p2e1)

    val dimensions = ledger.events.map(_.eventId -> AssistanceDimensions.defaultLive).toMap
    val p1 = AssistanceSummaries.recomputeForPlayer("player-1", ledger, dimensions, calibration)
    val p2 = AssistanceSummaries.recomputeForPlayer("player-2", ledger, dimensions, calibration)

    assertEquals(p1.usedLevel.value, 5)
    assertEquals(p2.usedLevel.value, 5)
    assert(p1.assistanceLoad.value != p2.assistanceLoad.value)
    assertEquals(p1.modelVersion, calibration.modelVersion)

  test("Used Offset is versioned, non-negative, and based on Used Level plus Assistance Load"):
    val ledger = AssistanceLedger.empty.append(event("e1", featureKey = "move_advice"))
    val dimensions = ledger.events.map(_.eventId -> AssistanceDimensions.defaultLive).toMap
    val summary = AssistanceSummaries.recomputeForPlayer("player-1", ledger, dimensions, calibration)
    val offset = UsedOffset.fromSummary(summary, calibration)

    assert(offset.nonNegative)
    assertEquals(offset.modelVersion, calibration.modelVersion)
    assert(offset.value >= calibration.baseOffsetByUsedLevel(summary.usedLevel.value))
  test("subscription, ad, token, and marketing state cannot lower Used Offset"):
    val summary = PlayerAssistanceSummary("player-1", Level(8), AssistanceLoad(200.0), calibration.modelVersion)
    val baseline = UsedOffset.fromSummary(summary, calibration)
    val withMarketingState = FairnessExclusions.offsetWithExcludedState(
      summary,
      calibration,
      FairnessExcludedState(
        subscriptionTier = "premium",
        adCampaign = Some("launch-ad"),
        tokenBalance = 99,
        marketingVariant = Some("variant-b")
      )
    )

    assertEquals(withMarketingState, baseline)
    assert(!FairnessExclusions.subscriptionsMayLowerUsedOffset)
    assert(!FairnessExclusions.adsMayLowerUsedOffset)
    assert(!FairnessExclusions.tokensMayLowerUsedOffset)
    assert(!FairnessExclusions.marketingMayLowerUsedOffset)

  test("Effective Rating equals ECR plus Used Offset and changes expected score"):
    val noHelp = EffectiveRating(1500, UsedOffset(0, calibration.modelVersion))
    val helped = EffectiveRating(1500, UsedOffset(100, calibration.modelVersion))
    val opponent = EffectiveRating(1500, UsedOffset(0, calibration.modelVersion))

    assertEquals(noHelp.value, 1500)
    assertEquals(helped.value, 1600)
    assertEquals(ExpectedScore.expectedScore(noHelp, opponent), 0.5)
    assert(ExpectedScore.expectedScore(helped, opponent) > 0.5)

  test("accounting authority remains server-side and joinable outside core game records"):
    assert(AccountingAuthority.serverAuthoritative)
    assert(AccountingAuthority.mayStoreOutsideCoreGameRecordsIfJoinable)
    assert(AccountingAuthority.coreRatingFlowEditsRequirePatchMap)
