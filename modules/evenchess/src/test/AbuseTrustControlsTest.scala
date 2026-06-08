package lila.evenchess

class AbuseTrustControlsTest extends munit.FunSuite:

  import AbuseTrustControls.*
  import ProductInvariants.RequirementClass

  private val fairness =
    FairnessValues(
      setLevel = "L5",
      usedLevel = "L4",
      assistanceLoad = "medium",
      usedOffset = "offset-v1",
      ecrPolicy = "ecr-v1",
      matchmakingPolicy = "match-v1",
      stockfishProfile = "sf-l5-rapid",
      aiExactness = "heuristic",
      targetIsolation = "target-isolated"
    )

  test("Appendix Q requirements are classified before implementation"):
    val byRequirement =
      AbuseRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(AbuseRequirement.NonPlatformGuidanceProhibited), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AbuseRequirement.LegalPlatformVsExternalHelp), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AbuseRequirement.MvpInvasiveControlsExcluded), RequirementClass.UnresolvedProductOwnerDecision)
    assertEquals(byRequirement(AbuseRequirement.FairPlayReportContext), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(AbuseRequirement.ReuseLilaModerationPatterns), RequirementClass.AdaptedToLichessFork)

  test("Version 2 Phase R abuse and ops requirements are classified before adapter work"):
    val byRequirement =
      PhaseRRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseRRequirement.NonPlatformHelpProhibited), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseRRequirement.PublicRulesExplainPlatformOnlyHelp), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseRRequirement.MatchmakingAbuseMonitoring), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseRRequirement.RepeatOpponentCaps), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseRRequirement.StrictPreferenceNotCollusionLoophole), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseRRequirement.MajorSystemsFeatureFlagged), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseRRequirement.AsymmetricOutageRemedies), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseRRequirement.LocalDevFlowAndRollback), RequirementClass.AdaptedToLichessFork)

  test("legal platform coaching is distinct from prohibited non-platform guidance"):
    val legal = GuidanceContext(
      source = GuidanceSource.PlatformCoaching,
      disclosedToOpponent = true,
      setLevelCapped = true,
      serverAudited = true,
      pricedIntoEcr = true
    )

    assert(legal.legalInRatedEvenChess)
    assert(!legal.copy(source = GuidanceSource.ExternalEngine).legalInRatedEvenChess)
    assert(!legal.copy(source = GuidanceSource.StreamChat).legalInRatedEvenChess)
    assert(!legal.copy(serverAudited = false).legalInRatedEvenChess)
    assert(!legal.copy(pricedIntoEcr = false).legalInRatedEvenChess)

  test("exploit register covers all Appendix Q exploit rows with signals and mitigations"):
    assert(ExploitRegister.coversAllKnownExploits)
    assert(ExploitRegister.rows.forall(_.complete))
    assert(ExploitRegister.byExploit(ExploitType.Sandbagging).mitigations.contains(MitigationAction.RatingFloor))
    assert(ExploitRegister.byExploit(ExploitType.Collusion).mitigations.contains(MitigationAction.GraphAnalysis))
    assert(ExploitRegister.byExploit(ExploitType.PromptInjection).mitigations.contains(MitigationAction.OutputValidation))
    assert(ExploitRegister.byExploit(ExploitType.TargetAbuse).mitigations.contains(MitigationAction.SeparateTargetPool))
    assert(ExploitRegister.byExploit(ExploitType.PremiumMisrepresentation).mitigations.contains(MitigationAction.KillSwitch))

  test("abuse controls cannot silently change fairness values"):
    val unchanged = AbuseControlDecision(
      action = MitigationAction.Review,
      before = fairness,
      after = fairness,
      auditId = "",
      visibleReason = ""
    )
    val changedAudited = unchanged.copy(
      action = MitigationAction.NoRate,
      after = fairness.copy(ecrPolicy = "no-rate"),
      auditId = "audit-1",
      visibleReason = "engine outage"
    )
    val changedSilent = changedAudited.copy(auditId = "", visibleReason = "")

    assert(unchanged.allowed)
    assert(changedAudited.allowed)
    assert(!changedSilent.allowed)

  test("MVP excludes invasive controls unless product and privacy approval exist"):
    assert(!MvpTrustControls.phoneVerificationRequired)
    assert(!MvpTrustControls.deviceSessionRiskScoringRequired)
    assert(!MvpTrustControls.sameIpCreationLimitsRequired)
    assert(!MvpTrustControls.highRiskClusterTokenDelaysRequired)

    assert(!InvasiveControlRequest("phone", productOwnerApproved = true, privacyReviewApproved = false).mayEnable)
    assert(!InvasiveControlRequest("phone", productOwnerApproved = false, privacyReviewApproved = true).mayEnable)
    assert(InvasiveControlRequest("phone", productOwnerApproved = true, privacyReviewApproved = true).mayEnable)

  test("fair-play reports include game, player, move range, ledger summary, and visible assistance state"):
    val report = FairPlayReportContext(
      reportId = "report-1",
      gameId = "game-1",
      playerId = "player-1",
      moveRange = "12-20",
      coachingLedgerSummaryId = "ledger-summary-1",
      visibleAssistanceState = "L5 disclosed"
    )

    assert(report.complete)
    assert(!report.copy(coachingLedgerSummaryId = "").complete)
    assert(!report.copy(visibleAssistanceState = "").complete)

  test("moderation protects anti-cheat internals and reuses lila patterns with EvenChess context"):
    val safe = ModerationDisclosurePolicy(
      exposesAntiCheatInternals = false,
      usesExistingLilaModerationPatterns = true,
      includesEvenChessLedgerContext = true
    )

    assert(safe.safe)
    assert(!safe.copy(exposesAntiCheatInternals = true).safe)
    assert(!safe.copy(usesExistingLilaModerationPatterns = false).safe)

  test("high-volume probing can be capped only with audited suppression"):
    val normal = CandidateProbePattern("player-1", hoverCount = 5, revealCount = 2, requestCount = 1, windowSeconds = 60)
    val high = CandidateProbePattern("player-1", hoverCount = 80, revealCount = 15, requestCount = 10, windowSeconds = 60)

    assert(!normal.highVolume(maxEvents = 50, minWindowSeconds = 30))
    assert(high.highVolume(maxEvents = 50, minWindowSeconds = 30))
    assert(ProbeControlDecision(capped = true, cooldown = true, suppressionAudited = true, auditId = "audit-1").valid)
    assert(!ProbeControlDecision(capped = true, cooldown = true, suppressionAudited = false, auditId = "").valid)

  test("runtime trust policy handles outage, AI timeout, desync, stale coaching, and disclosure loss"):
    val healthy = RuntimeFairnessHealth(
      engineHealthyForBoth = true,
      aiTimedOut = false,
      sequenceMatches = true,
      boardHashMatches = true,
      mobileDisclosureVisible = true
    )

    assertEquals(RuntimeTrustPolicy.remedy(healthy), RuntimeTrustRemedy.Continue)
    assertEquals(RuntimeTrustPolicy.remedy(healthy.copy(engineHealthyForBoth = false)), RuntimeTrustRemedy.NoRate)
    assertEquals(RuntimeTrustPolicy.remedy(healthy.copy(aiTimedOut = true)), RuntimeTrustRemedy.SuppressAi)
    assertEquals(RuntimeTrustPolicy.remedy(healthy.copy(sequenceMatches = false)), RuntimeTrustRemedy.ServerResync)
    assertEquals(RuntimeTrustPolicy.remedy(healthy.copy(boardHashMatches = false)), RuntimeTrustRemedy.ClearStaleCoaching)
    assertEquals(RuntimeTrustPolicy.remedy(healthy.copy(mobileDisclosureVisible = false)), RuntimeTrustRemedy.Pause)

  test("matchmaking abuse policy caps repeat opponents and blocks strict-preference collusion loops"):
    val policy = RepeatOpponentPolicy.default
    val clear = MatchmakingAbuseSignal(
      playerId = "player-1",
      opponentId = "player-2",
      repeatPairingsInWindow = 0,
      collusionScore = 0.0,
      ratingTransferScore = 0.0,
      targetLevelManipulationScore = 0.0,
      abortCountInWindow = 0,
      queueSnipeScore = 0.0,
      strictPreferenceSearch = false,
      auditId = "audit-clear"
    )
    val repeatStrict = clear.copy(repeatPairingsInWindow = 2, strictPreferenceSearch = true, auditId = "audit-repeat")
    val collusion = clear.copy(collusionScore = 0.9, ratingTransferScore = 0.8, auditId = "audit-collusion")
    val unaudited = collusion.copy(auditId = "")

    assert(policy.valid)
    assert(clear.valid)
    assert(!policy.caps(clear))
    assert(policy.caps(repeatStrict))
    assert(repeatStrict.suspicious)
    assert(repeatStrict.auditedIfSuspicious)
    assert(!unaudited.auditedIfSuspicious)

    val repeatDecision = MatchmakingTrustPolicy.decide(repeatStrict, policy)
    val collusionDecision = MatchmakingTrustPolicy.decide(collusion, policy)
    val clearDecision = MatchmakingTrustPolicy.decide(clear, policy)

    assert(!repeatDecision.allowed)
    assert(repeatDecision.mitigations.contains(MitigationAction.RepeatPairLimit))
    assert(!collusionDecision.allowed)
    assert(collusionDecision.requiresManualReview)
    assert(collusionDecision.mitigations.contains(MitigationAction.GraphAnalysis))
    assert(clearDecision.allowed)
    assert(clearDecision.valid)

  test("token and review abuse limits cover ad grants L10 custom analysis and full-game analysis"):
    val limits = TokenReviewAbuseLimits.default

    assert(limits.valid)
    assert(limits.allowAdGrant(grantsToday = 0, cooldownActive = false))
    assert(!limits.allowAdGrant(grantsToday = limits.adTokenDailyCap, cooldownActive = false))
    assert(!limits.allowAdGrant(grantsToday = 0, cooldownActive = true))
    assert(limits.allowCustomAnalysis(isL10 = false, usedToday = 999))
    assert(limits.allowCustomAnalysis(isL10 = true, usedToday = limits.customL10DailyCap - 1))
    assert(!limits.allowCustomAnalysis(isL10 = true, usedToday = limits.customL10DailyCap))
    assert(limits.allowFullGameAnalysis(usedToday = limits.fullGameDailyCap - 1))
    assert(!limits.allowFullGameAnalysis(usedToday = limits.fullGameDailyCap))

  test("engine and AI abuse guard rejects hidden higher-level requests and raw provider exposure"):
    val guard = EngineAiAbuseGuard.default

    assert(guard.valid)
    assert(!guard.copy(customInstructionsNumericOnly = false).valid)
    assert(!guard.copy(rejectsHiddenInfoRequests = false).valid)
    assert(!guard.copy(rejectsForbiddenOrHigherLevelRequests = false).valid)
    assert(!guard.copy(validatesForbiddenBestMoveWording = false).valid)
    assert(!guard.copy(stockfishRawOutputExposed = true).valid)
    assert(!guard.copy(providerSecretsExposed = true).valid)
    assert(!guard.copy(rawProviderPayloadExposed = true).valid)

  test("operational feature flags cover major systems and audit fairness-affecting changes"):
    val flags = OperationalFeature.values.toList.map { feature =>
      OperationalFeatureFlag(
        feature = feature,
        enabled = feature != OperationalFeature.Ads,
        policyVersion = "ops-v1",
        auditId = if feature == OperationalFeature.EceLiveCalls then "audit-ece-live" else "",
        reason = if feature == OperationalFeature.EceLiveCalls then "live assistance pause drill" else "",
        changesRatedFairness = feature == OperationalFeature.EceLiveCalls
      )
    }
    val silentFairness = flags.find(_.feature == OperationalFeature.EceLiveCalls).get.copy(auditId = "", reason = "")

    assert(OperationalFeatureFlags.coversMajorSystems(flags))
    assert(OperationalFeatureFlags.allAuditedWhenFairnessAffecting(flags))
    assert(!silentFairness.valid)

  test("operational health snapshot detects latency queue token settlement stale overlay and render failures"):
    val healthy = OperationalHealthSnapshot(
      eceLatencyMillis = 100,
      stockfishLatencyMillis = 100,
      aiLatencyMillis = 500,
      aiCostCents = 0,
      aiFallbackCount = 0,
      queueMillis = 1_000,
      tokenFlowHealthy = true,
      customAnalysisBacklog = 0,
      ratingSettlementHealthy = true,
      overlayStalePayloadErrors = 0,
      displayRenderFailures = 0,
      collectedAt = 123456789L,
      schemaVersion = "ops-health-v1"
    )
    val degraded = healthy.copy(eceLatencyMillis = 2_500, overlayStalePayloadErrors = 1)

    assert(healthy.valid)
    assert(!healthy.degraded)
    assert(degraded.valid)
    assert(degraded.degraded)
    assert(!healthy.copy(collectedAt = 0).valid)

  test("incident planner can pause required systems and no-rate asymmetric assistance outages"):
    val health = OperationalHealthSnapshot(
      eceLatencyMillis = 2_500,
      stockfishLatencyMillis = 100,
      aiLatencyMillis = 500,
      aiCostCents = 0,
      aiFallbackCount = 0,
      queueMillis = 1_000,
      tokenFlowHealthy = true,
      customAnalysisBacklog = 0,
      ratingSettlementHealthy = true,
      overlayStalePayloadErrors = 0,
      displayRenderFailures = 0,
      collectedAt = 123456789L,
      schemaVersion = "ops-health-v1"
    )
    val plan =
      IncidentResponsePlanner.fromHealth("incident-1", health, asymmetricAssistanceOutage = true, auditId = "audit-incident")

    assert(plan.valid)
    assert(plan.pausesRequiredSystems)
    assertEquals(plan.outcome, IncidentOutcome.NoRateAffectedGames)
    assert(plan.rollbackNotes.contains("no upstream Lichess files"))
    assert(plan.testsRun.contains("AbuseTrustControlsTest"))
    assert(plan.preservesLocalDevFlow)

  test("campaign trust guard pauses copy that implies cheating hidden engines or pay-to-win help"):
    assert(CampaignTrustGuard.mustPauseCampaign("Premium gives stronger live help for premium accounts."))
    assert(CampaignTrustGuard.mustPauseCampaign("Secret engine help for your games."))
    assert(CampaignTrustGuard.mustPauseCampaign("Pay to win more games."))
    assert(!CampaignTrustGuard.mustPauseCampaign("Disclosed platform coaching is capped, logged, and reflected in ECR."))
