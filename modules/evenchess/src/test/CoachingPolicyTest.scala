package lila.evenchess

class CoachingPolicyTest extends munit.FunSuite:

  import CoachingLadder.*
  import CoachingPolicy.*
  import EvenChessMode.*

  private def input(
      featureKey: String = "move_advice",
      setLevel: Level = Level(5),
      currentUsedLevel: Level = Level(2),
      requestType: CoachingRequestType = CoachingRequestType.Display,
      abuseState: AbuseState = AbuseState.Clear,
      stale: Boolean = false,
      premoveCommitted: Boolean = false,
      engineHealth: ServiceHealth = ServiceHealth.Healthy,
      aiHealth: ServiceHealth = ServiceHealth.Healthy
  ) =
    val row = FeatureRegistry.byKey(featureKey)
    PolicyInput(
      gameId = "game-1",
      playerId = "player-1",
      mode = GameMode.EvenChess,
      rated = true,
      timeControlBucket = TimeControlBucket.Rapid,
      ply = 18,
      boardStateKey = "board-key-18",
      clockContext = ClockContext(
        millisRemaining = 30000,
        incrementMillis = 2000,
        premoveCommitted = premoveCommitted,
        staleForDecision = stale
      ),
      setLevel = setLevel,
      currentUsedLevel = currentUsedLevel,
      requestedFeature = featureKey,
      requestType = requestType,
      registryRow = row,
      exactnessClass = row.exactnessClass,
      abuseState = abuseState,
      engineHealth = engineHealth,
      aiHealth = aiHealth,
      policyVersion = "policy-v1"
    )

  test("server owns coaching authority and debug output remains restricted"):
    assert(ServerAuthority.serverOwnsCoachingPermission)
    assert(ServerAuthority.serverOwnsBoardState)
    assert(ServerAuthority.serverOwnsClockState)
    assert(ServerAuthority.serverOwnsAssistanceAccounting)
    assert(ServerAuthority.serverOwnsTokenConsumption)
    assert(ServerAuthority.serverOwnsRatingUpdates)
    assert(ServerAuthority.serverOwnsAuditEvents)
    assert(!ServerAuthority.clientCanDecidePermission)
    assert(!ServerAuthority.clientSideHidingIsSecurity)
    assert(!ServerAuthority.debugEndpointsMayExposeUnrestrictedEngineOutput)
    assert(!ServerAuthority.debugEndpointsMayExposeHiddenPolicyData)

  test("policy input records all required Appendix G fields"):
    val policyInput = input()

    assert(policyInput.hasRequiredFields)
    assertEquals(policyInput.gameId, "game-1")
    assertEquals(policyInput.playerId, "player-1")
    assertEquals(policyInput.mode, GameMode.EvenChess)
    assert(policyInput.rated)
    assertEquals(policyInput.timeControlBucket, TimeControlBucket.Rapid)
    assertEquals(policyInput.registryRow.featureKey, policyInput.requestedFeature)
    assertEquals(policyInput.policyVersion, "policy-v1")

  test("allowed render is server-authorized, audited, and raises Used Level monotonically"):
    val decision = PolicyEngine.decide(input(currentUsedLevel = Level(3), setLevel = Level(5)))

    assertEquals(decision.outcome, PolicyOutcome.AllowRender)
    assertEquals(decision.deliveredLevel.map(_.value), Some(5))
    assertEquals(decision.usedLevelAfter.value, 5)
    assert(decision.assistanceWeightDelta > 0)
    assert(decision.renderAllowed)
    assert(decision.isAuditable)

  test("used level never decreases when lower-level feature renders"):
    val decision = PolicyEngine.decide(input(featureKey = "legal_targets", setLevel = Level(5), currentUsedLevel = Level(5)))

    assertEquals(decision.outcome, PolicyOutcome.AllowRender)
    assertEquals(decision.deliveredLevel.map(_.value), Some(1))
    assertEquals(decision.usedLevelAfter.value, 5)

  test("request-only permission can compute hidden but does not charge assistance"):
    val decision = PolicyEngine.decide(input(requestType = CoachingRequestType.Request))

    assertEquals(decision.outcome, PolicyOutcome.AllowHidden)
    assertEquals(decision.deliveredLevel, None)
    assertEquals(decision.assistanceWeightDelta, 0)
    assert(!decision.renderAllowed)
    assert(decision.isAuditable)

  test("policy suppresses above Set Level, stale/premove, and abuse outcomes"):
    assertEquals(
      PolicyEngine.decide(input(featureKey = "candidate_cards", setLevel = Level(4))).outcome,
      PolicyOutcome.SuppressLevel
    )
    assertEquals(PolicyEngine.decide(input(stale = true)).outcome, PolicyOutcome.Stale)
    assertEquals(PolicyEngine.decide(input(premoveCommitted = true)).outcome, PolicyOutcome.Stale)
    assertEquals(
      PolicyEngine.decide(input(abuseState = AbuseState.Cooldown)).outcome,
      PolicyOutcome.BlockAbuse
    )

  test("degraded engine or AI health produces audited bounded fallback"):
    val engineDecision = PolicyEngine.decide(input(engineHealth = ServiceHealth.Unavailable))
    val aiDecision = PolicyEngine.decide(input(featureKey = "ai_explain", setLevel = Level(5), aiHealth = ServiceHealth.Degraded))

    assertEquals(engineDecision.outcome, PolicyOutcome.Degraded)
    assert(engineDecision.renderAllowed)
    assert(engineDecision.isAuditable)
    assertEquals(aiDecision.outcome, PolicyOutcome.Degraded)
    assert(aiDecision.renderAllowed)

  test("audit event captures required ledger fields and reconstructable delivery"):
    val policyInput = input()
    val decision = PolicyEngine.decide(policyInput)
    val event = AuditEvent.fromDecision("event-1", policyInput, decision, createdAt = 123456789L, engineJobId = Some("engine-1"))

    assert(event.hasRequiredFields)
    assertEquals(event.eventId, "event-1")
    assertEquals(event.requestedLevel.value, 5)
    assertEquals(event.setLevel.value, 5)
    assertEquals(event.deliveredLevel.map(_.value), Some(5))
    assertEquals(event.usedLevelAfter.value, 5)
    assertEquals(event.assistanceWeightDelta, policyInput.registryRow.assistanceWeight)
    assertEquals(event.surface, policyInput.registryRow.uiSlot)
    assertEquals(event.sourceType, policyInput.registryRow.sourceType)
    assertEquals(event.engineJobId, Some("engine-1"))
    assertEquals(event.policyVersion, "policy-v1")
    assertEquals(event.schemaVersion, AuditEvent.currentSchemaVersion)
    assert(event.deliveredKinds.reconstructable)
    assert(event.appendOnlySchemaVersioned)

  test("suppressed and stale decisions are audited even when no visual idea is delivered"):
    val policyInput = input(stale = true)
    val decision = PolicyEngine.decide(policyInput)
    val event = AuditEvent.fromDecision("event-stale", policyInput, decision, createdAt = 123456789L)

    assertEquals(event.outcome, PolicyOutcome.Stale)
    assertEquals(event.deliveredLevel, None)
    assertEquals(event.assistanceWeightDelta, 0)
    assert(!event.deliveredKinds.reconstructable)
    assert(event.hasRequiredFields)

  test("ledger appends events without replacing rated schema-versioned history"):
    val policyInput = input()
    val event1 = AuditEvent.fromDecision("event-1", policyInput, PolicyEngine.decide(policyInput), 123456789L)
    val event2 = event1.copy(eventId = "event-2", createdAt = 123456790L)
    val ledger = AssistanceLedger.empty.append(event1).append(event2)

    assertEquals(ledger.events.map(_.eventId), Vector("event-1", "event-2"))
    assertEquals(ledger.replaceEvent(event1), ledger)
    assert(ledger.allRatedEventsSchemaVersioned)
    assert(ledger.assistanceSummaryComputable)
    assertEquals(ledger.maxUsedLevel.value, 5)

  test("rated EvenChess completion requires computable assistance summary unless no-rate or annul"):
    val policyInput = input()
    val event = AuditEvent.fromDecision("event-1", policyInput, PolicyEngine.decide(policyInput), 123456789L)
    val ledger = AssistanceLedger.empty.append(event)

    assert(GameCompletionGuard.mayCompleteRatedEvenChess(ledger, GameCompletionRatingState.Rate))
    assert(!GameCompletionGuard.mayCompleteRatedEvenChess(AssistanceLedger.empty, GameCompletionRatingState.Rate))
    assert(GameCompletionGuard.mayCompleteRatedEvenChess(AssistanceLedger.empty, GameCompletionRatingState.NoRate))
    assert(GameCompletionGuard.mayCompleteRatedEvenChess(AssistanceLedger.empty, GameCompletionRatingState.Annul))

  test("Stage 1 dummy overlay audit event exists before real coaching strength"):
    val event = Stage1DummyAudit.event("game-stage1", "player-stage1", createdAt = 123456789L)

    assertEquals(event.gameId, "game-stage1")
    assertEquals(event.playerId, "player-stage1")
    assertEquals(event.policyVersion, "stage1-dummy-policy")
    assertEquals(event.featureKey, "move_history")
    assert(event.hasRequiredFields)
