package lila.evenchess

class TelemetryAnalyticsTest extends munit.FunSuite:

  import AssistanceAccounting.{ AssistanceLoad, PlayerAssistanceSummary, UsedOffset }
  import CoachingLadder.Level
  import CoachingOverlays.Perspective
  import CoachingPolicy.*
  import EcrRating.SearchStage
  import EvenChessMode.{ GameMode, TimeControlBucket }
  import LevelBasedMatchmaking.*
  import MarketingFunnelPolicy.{ AttributionEvent, LandingVariant }
  import MonetisationPolicy.PlanTier
  import ProductInvariants.RequirementClass
  import TelemetryAnalytics.*

  private val versions =
    VersionSet(
      schemaVersion = "telemetry-v1",
      policyVersion = Some("policy-v1"),
      modelVersion = Some("model-v1"),
      configVersion = Some("config-v1")
    )

  private def event(name: TelemetryEventName, family: EventFamily = EventFamily.MatchLifecycle): TelemetryEvent =
    TelemetryEvent(
      eventId = name.toString,
      family = family,
      name = name,
      authority = EventAuthority.Server,
      schemaVersion = "telemetry-v1",
      occurredAt = 123456789L,
      pseudonymousUserId = Some("user-hash-1"),
      gameId = Some("game-1"),
      rated = true,
      versions = versions,
      dedupeId = Some(name.toString)
    )

  private def matchContract =
    MatchContract(
      requestId = "match-request-1",
      gameId = Some("game-1"),
      timeControl = TimeControlBucket.Rapid,
      rated = true,
      whitePlayerId = "white-user",
      blackPlayerId = "black-user",
      whiteEcr = 1500,
      blackEcr = 1510,
      whiteMmr = 1500,
      blackMmr = 1510,
      whiteSetLevel = Level(5),
      blackSetLevel = Level(5),
      whiteExpectedOffset = UsedOffset(45, "offset-v1"),
      blackExpectedOffset = UsedOffset(45, "offset-v1"),
      whiteEffectiveRating = 1545,
      blackEffectiveRating = 1555,
      matchQuality = MatchQualityScore(value = 980, ecrDelta = 10, effectiveRatingDelta = 10, levelDelta = 0),
      preferenceFlags = PreferenceMatchFlags(
        scenario = SearchPreferenceScenario.NormalSearch,
        requesterPreferredLevelMatched = true,
        candidatePreferredLevelMatched = true,
        widenedSearch = false,
        unevenMatch = false,
        unevenReason = None
      ),
      tokenGateResult = "eligible",
      policyVersion = "mmr-policy-v1",
      stage = SearchStage.Initial
    )

  private def policyAudit =
    val row = CoachingLadder.FeatureRegistry.byKey("move_advice")
    val input = PolicyInput(
      gameId = "game-1",
      playerId = "white-user",
      mode = GameMode.EvenChess,
      rated = true,
      timeControlBucket = TimeControlBucket.Rapid,
      ply = 12,
      boardStateKey = "board-12",
      clockContext = ClockContext(30000, 2000, premoveCommitted = false, staleForDecision = false),
      setLevel = Level(5),
      currentUsedLevel = Level(4),
      requestedFeature = "move_advice",
      requestType = CoachingRequestType.Display,
      registryRow = row,
      exactnessClass = row.exactnessClass,
      abuseState = AbuseState.Clear,
      engineHealth = ServiceHealth.Healthy,
      aiHealth = ServiceHealth.Healthy,
      policyVersion = "policy-v1"
    )
    CoachingPolicy.AuditEvent.fromDecision("audit-live-1", input, PolicyEngine.decide(input), createdAt = 123456789L)

  private def historyEntry(
      ply: Int,
      sideToMove: Perspective = Perspective.Black,
      whiteOutput: Boolean = true,
      blackOutput: Boolean = true
  ) =
    LiveCoaching.LiveEceHistoryEntry(
      gameId = "game-1",
      ply = ply,
      fen = s"fen-$ply",
      moveUci = Some("e2e4"),
      positionHash = s"hash-$ply",
      sideToMove = sideToMove,
      whiteRequestedLevel = Level(5),
      blackRequestedLevel = Level(5),
      policyVersion = "policy-v1",
      eceVersion = "ece-v1",
      whiteOutput =
        if whiteOutput then
          Some(
            LiveCoaching.LiveEceOutputReference(
              side = Perspective.White,
              outputRef = s"white-$ply",
              auditId = s"audit-w-$ply",
              deliveredLevel = Level(5),
              summary = Some("summary"),
              plan = None,
              overlayAtomRefs = List(s"atom-w-$ply")
            )
          )
        else None,
      blackOutput =
        if blackOutput then
          Some(
            LiveCoaching.LiveEceOutputReference(
              side = Perspective.Black,
              outputRef = s"black-$ply",
              auditId = s"audit-b-$ply",
              deliveredLevel = Level(5),
              summary = Some("summary"),
              plan = None,
              overlayAtomRefs = List(s"atom-b-$ply")
            )
          )
        else None,
      rawEceRetained = false,
      createdAt = 123456789L + ply
    )

  test("Appendix P requirements are classified before implementation"):
    val byRequirement =
      TelemetryRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(TelemetryRequirement.AppendOnlyServerLedger), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(TelemetryRequirement.CalibrationDashboards), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(TelemetryRequirement.FunnelAttribution), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(TelemetryRequirement.LaunchDashboardGrouping), RequirementClass.AdaptedToLichessFork)

  test("Version 2 Phase Q telemetry and audit requirements are classified before adapter work"):
    val byRequirement =
      PhaseQRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseQRequirement.AuditMatchContracts), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseQRequirement.AuditSetLevelAtGameStart), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseQRequirement.AuditUsedLevelIncrease), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseQRequirement.AuditLiveEcePayloadGenerated), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseQRequirement.DisplayEngineEventEmission), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseQRequirement.PrivacySafeRawDataPolicy), RequirementClass.EvenChessSpecific)

  test("rated telemetry events require server authority, schema versions, and relevant versions"):
    val good = event(TelemetryEventName.GameStarted)
    val client = good.copy(authority = EventAuthority.ClientSupplement)
    val missingVersion = good.copy(versions = VersionSet("telemetry-v1", None, None, None))

    assert(good.readyForRatedLedger)
    assert(!client.readyForRatedLedger)
    assert(!missingVersion.readyForRatedLedger)
    assert(good.hasRequiredFields)

  test("server telemetry ledger is append-only and schema-versioned for rated games"):
    val first = event(TelemetryEventName.GameStarted)
    val replacement = first.copy(eventId = "replacement")
    val ledger = TelemetryLedger.empty.append(first)

    assertEquals(ledger.events.size, 1)
    assertEquals(ledger.replaceEvent(replacement).events, ledger.events)
    assert(ledger.allRatedEventsAppendOnlyServerSchemaVersioned)
    assert(!ledger.append(first.copy(schemaVersion = "")).allRatedEventsAppendOnlyServerSchemaVersioned)

  test("ledger completeness supports rating replay, assistance recomputation, and review incident handling"):
    val ledger = TelemetryLedger.empty
      .append(event(TelemetryEventName.GameStarted))
      .append(event(TelemetryEventName.GameEnded))
      .append(event(TelemetryEventName.AssistanceSummaryComputed, EventFamily.Rating))
      .append(event(TelemetryEventName.OffsetComputed, EventFamily.Rating))
      .append(event(TelemetryEventName.RatingApplied, EventFamily.Rating))
      .append(event(TelemetryEventName.CoachingRequested, EventFamily.Coaching))
      .append(event(TelemetryEventName.CoachingSurfaced, EventFamily.Coaching))
      .append(event(TelemetryEventName.PositionStateRecorded, EventFamily.Move))
      .append(event(TelemetryEventName.AbuseSignalRecorded, EventFamily.Abuse))
      .append(event(TelemetryEventName.ReviewCaseOpened, EventFamily.Abuse))

    assert(ledger.supportsRatingReplay)
    assert(ledger.supportsAssistanceRecomputation)
    assert(ledger.supportsFairPlayReviewAndIncidents)

  test("Phase Q audit envelopes cover match contracts Set Level Used Level ECE display and settlement"):
    val matchRecord = LevelBasedMatchmaking.MmrEngine.auditRecord(matchContract, abuseSignalKeys = Nil, loggedAt = 123456789L)
    val matchAudit = AuditEnvelope.matchContract(matchRecord, "audit-match", 123456789L)
    val setLevel = AuditEnvelope.setLevelAtGameStart("audit-set-level", "game-1", "white-user", Level(5), "policy-v1", 123456790L)
    val usedLevel =
      AuditEnvelope.usedLevelIncrease("audit-used-level", "game-1", "white-user", Level(4), Level(5), "policy-v1", 123456791L)
    val ece = AuditEnvelope.liveEcePayload("audit-ece", historyEntry(12), Perspective.White, "policy-v1", 123456792L)
    val display = AuditEnvelope.coachingDisplay("audit-display", policyAudit, DisplayAuditAction.Shown, 123456793L)
    val settlement = AuditEnvelope.finalSettlement(
      eventId = "audit-settlement",
      gameId = "game-1",
      summary = PlayerAssistanceSummary("white-user", Level(5), AssistanceLoad(40.5), "assistance-v1"),
      usedOffset = UsedOffset(48, "offset-v1"),
      ratingDelta = 12,
      policyVersion = "settlement-v1",
      createdAt = 123456794L
    )
    val ledger = AuditLedger.empty
      .append(matchAudit)
      .append(setLevel)
      .append(usedLevel)
      .append(ece)
      .append(display)
      .append(settlement)

    assert(matchAudit.valid)
    assert(setLevel.valid)
    assert(usedLevel.valid)
    assert(ece.valid)
    assert(display.valid)
    assert(settlement.valid)
    assert(ledger.allValid)
    assert(ledger.supportsAppendixRLiveAudit)
    assertEquals(ledger.replaceEvent(matchAudit.copy(eventId = "replacement")).events, ledger.events)
    assert(!usedLevel.copy(usedLevelBefore = Some(Level(5)), usedLevelAfter = Some(Level(4))).valid)
    assert(!ece.copy(storesRawProviderPayload = true).valid)

  test("proposed-move checks record arrow legality level and whether result was shown"):
    val scheduled = LiveCoaching.LiveEceHistoryScheduler
      .scheduleProposedMove(
        gameId = "game-1",
        ply = 18,
        proposalIndex = 1,
        fen = "fen-18",
        positionHash = "hash-18",
        proposedMoveUci = "e2e4",
        requesterSide = Perspective.White,
        sideToMove = Perspective.White,
        whiteEcr = None,
        blackEcr = None,
        whiteLevel = Level(5),
        blackLevel = Level(5),
        aiTextAllowed = false,
        proposedMoveHelpAllowed = true,
        scheduledAt = 123456789L
      )
      .toOption
      .getOrElse(fail("expected scheduled proposed move"))
    val audit = AuditEnvelope.proposedMove(
      eventId = "audit-proposed",
      scheduled = scheduled,
      playerId = "white-user",
      proposedMoveLegal = true,
      resultShown = true,
      policyVersion = "policy-v1",
      createdAt = 123456790L
    )

    assert(audit.valid)
    assertEquals(audit.proposedMoveUci, Some("e2e4"))
    assertEquals(audit.proposedMoveLegal, Some(true))
    assertEquals(audit.resultShown, Some(true))
    assertEquals(audit.setLevel, Some(Level(5)))
    assert(!audit.copy(proposedMoveUci = Some("e2e4 e7e5")).valid)

  test("audit completeness snapshot joins append-only audit events with calibration dimensions"):
    val auditLedger = AuditLedger.empty
      .append(AuditEnvelope.matchContract(LevelBasedMatchmaking.MmrEngine.auditRecord(matchContract, Nil, 123456789L), "audit-match", 123456789L))
      .append(AuditEnvelope.setLevelAtGameStart("audit-set-level", "game-1", "white-user", Level(5), "policy-v1", 123456790L))
      .append(AuditEnvelope.usedLevelIncrease("audit-used-level", "game-1", "white-user", Level(4), Level(5), "policy-v1", 123456791L))
      .append(AuditEnvelope.liveEcePayload("audit-ece", historyEntry(12), Perspective.White, "policy-v1", 123456792L))
      .append(AuditEnvelope.coachingDisplay("audit-display", policyAudit, DisplayAuditAction.Expanded, 123456793L))
      .append(
        AuditEnvelope.finalSettlement(
          eventId = "audit-settlement",
          gameId = "game-1",
          summary = PlayerAssistanceSummary("white-user", Level(5), AssistanceLoad(40.5), "assistance-v1"),
          usedOffset = UsedOffset(48, "offset-v1"),
          ratingDelta = 12,
          policyVersion = "settlement-v1",
          createdAt = 123456794L
        )
      )
    val telemetryLedger = TelemetryLedger.empty
      .append(event(TelemetryEventName.GameStarted))
      .append(event(TelemetryEventName.GameEnded))
    val snapshot = AuditCompletenessSnapshot(
      ledger = auditLedger,
      telemetryLedger = telemetryLedger,
      calibrationDimensions = CalibrationDashboard.requiredDimensions,
      generatedAt = 123456795L
    )

    assert(snapshot.valid)
    assert(!snapshot.copy(calibrationDimensions = Set("Used Level")).valid)
    assert(!snapshot.copy(ledger = auditLedger.append(auditLedger.events.head.copy(serverAuthoritative = false))).valid)

  test("client-only analytics supplement product metrics but authorize no fairness decisions"):
    assert(!AnalyticsAuthority.clientAnalyticsMayAuthorize(DecisionDomain.Fairness))
    assert(!AnalyticsAuthority.clientAnalyticsMayAuthorize(DecisionDomain.Rating))
    assert(!AnalyticsAuthority.clientAnalyticsMayAuthorize(DecisionDomain.Token))
    assert(!AnalyticsAuthority.clientAnalyticsMayAuthorize(DecisionDomain.CoachingPermission))
    assert(AnalyticsAuthority.clientAnalyticsMaySupplement(DecisionDomain.ProductAnalytics))

  test("calibration dashboards require residual dimensions and mode separation"):
    val slice = CalibrationSlice(
      usedLevel = "L5",
      assistanceLoad = "medium",
      timeControl = "rapid",
      ecrBand = "1500-1700",
      exactness = "heuristic",
      featureMix = "hint+plan",
      followRate = "40-60",
      modeSegment = GameModeSegment.NormalEvenChess,
      residual = 0.12
    )

    assert(slice.hasRequiredDimensions)
    assert(CalibrationDashboard.includesRequiredDimensions(CalibrationDashboard.requiredDimensions))
    assert(CalibrationDashboard.separatesModeSegments(GameModeSegment.values.toSet))
    assert(!CalibrationDashboard.separatesModeSegments(Set(GameModeSegment.NormalEvenChess, GameModeSegment.Target)))

  test("calibration identifies stale, degraded, fallback, and pricing safety signals"):
    assert(RuntimeQualityFlags(stale = true, degraded = false, engineFallback = false).flagged)
    assert(RuntimeQualityFlags(stale = false, degraded = true, engineFallback = false).flagged)
    assert(RuntimeQualityFlags(stale = false, degraded = false, engineFallback = true).flagged)
    assertEquals(CalibrationSignalDetector.detect(residual = 0.2, usageRate = 0.2, abuseRate = 0.0), CalibrationSignal.Underpriced)
    assertEquals(CalibrationSignalDetector.detect(residual = -0.2, usageRate = 0.2, abuseRate = 0.0), CalibrationSignal.Overpriced)
    assertEquals(CalibrationSignalDetector.detect(residual = 0.0, usageRate = 0.9, abuseRate = 0.0), CalibrationSignal.Overused)
    assertEquals(CalibrationSignalDetector.detect(residual = 0.0, usageRate = 0.2, abuseRate = 0.1), CalibrationSignal.Unsafe)

  test("funnel attribution captures campaign fields and lifecycle linkage"):
    val attribution = FunnelAttribution(
      utmSource = Some("search"),
      utmMedium = Some("cpc"),
      utmCampaign = Some("launch"),
      utmContent = Some("hero-a"),
      utmTerm = Some("assisted chess"),
      clickId = Some("click-1"),
      variant = LandingVariant.Default,
      firstTouch = Some("search"),
      latestTouch = Some("retarget"),
      signupId = Some("signup-1"),
      firstGameId = Some("game-1"),
      subscriptionId = Some("sub-1")
    )

    assert(attribution.hasCampaignIdentity)
    assert(attribution.linksLifecycle)

  test("conversion events have stable shape and dedupe IDs"):
    val attribution = FunnelAttribution(
      utmSource = Some("search"),
      utmMedium = None,
      utmCampaign = Some("launch"),
      utmContent = None,
      utmTerm = None,
      clickId = Some("click-1"),
      variant = LandingVariant.FreeTokens,
      firstTouch = Some("search"),
      latestTouch = Some("search"),
      signupId = Some("signup-1"),
      firstGameId = Some("game-1"),
      subscriptionId = None
    )
    val conversion = ConversionEvent(
      name = AttributionEvent.Purchase,
      occurredAt = 123456789L,
      dedupeId = "dedupe-1",
      pseudonymousUserId = "user-hash-1",
      attribution = attribution,
      valueAudCents = Some(1000),
      plan = Some(PlanTier.Standard)
    )

    assert(conversion.validShape)
    assertEquals(ConversionDedupe.unique(List(conversion, conversion.copy(valueAudCents = Some(1600)))).size, 1)
    assert(!conversion.copy(dedupeId = "").validShape)

  test("launch dashboard grouping excludes invasive MVP risk scoring"):
    val group = LaunchDashboardGroup(
      source = "search",
      campaign = "launch",
      variant = LandingVariant.Default,
      accountType = AccountType.New,
      tokenSource = TokenSource.Onboarding,
      summarySource = SummarySource.FreeToken,
      queueHealth = QueueHealth.Healthy,
      plan = Some(PlanTier.FreeAdSupported),
      usesInvasiveRiskScoring = false
    )

    assert(group.validForMvp)
    assert(!group.copy(usesInvasiveRiskScoring = true).validForMvp)

  test("privacy scan and retention tiers enforce minimal collection and separation"):
    assert(PrivacyScan(
      collectsOnlyNeededEvents = true,
      usesPseudonymousAnalyticsIds = true,
      separatesRetentionTiers = true,
      avoidsUnnecessarySensitiveAdData = true
    ).passes)

    assert(RetentionPolicy(RetentionTier.HotRawLogs, maxDays = 14, containsRawGameplay = true).valid)
    assert(RetentionPolicy(RetentionTier.MediumDerivedMetrics, maxDays = 180, containsRawGameplay = false).valid)
    assert(RetentionPolicy(RetentionTier.LongTermAggregates, maxDays = 730, containsRawGameplay = false).valid)
    assert(!RetentionPolicy(RetentionTier.HotRawLogs, maxDays = 90, containsRawGameplay = true).valid)
    assert(!RetentionPolicy(RetentionTier.LongTermAggregates, maxDays = 730, containsRawGameplay = true).valid)

  test("ECE history retention supports live review modes without raw AI or provider payloads"):
    val history = LiveCoaching.LiveEceHistoryRecord
      .empty("game-1")
      .append(historyEntry(1, sideToMove = Perspective.White, whiteOutput = true, blackOutput = true))
      .append(historyEntry(2, sideToMove = Perspective.Black, whiteOutput = true, blackOutput = true))
    val plan = EceHistoryRetentionPlan.fromHistory(history, rollingRecentLimit = 20, paidSavedGame = false)

    assert(plan.valid)
    assert(plan.supportsReviewModes)
    assert(plan.safeStorage)
    assertEquals(plan.retainedFenCount, 2)
    assertEquals(plan.retainedOutputRefCount, 4)
    assertEquals(plan.aiDiagnosticLoggingPolicy, AiDiagnosticLoggingPolicy.SanitizedMetadataOnly)
    assert(!plan.copy(storesRawAiPrompt = true).valid)
    assert(!plan.copy(aiDiagnosticLoggingPolicy = AiDiagnosticLoggingPolicy.PrivacyReviewedRedactedSamples).valid)
