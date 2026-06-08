package lila.evenchess

class TrustOpsIncidentControlsTest extends munit.FunSuite:

  import AbuseTrustControls.{
    CandidateProbePattern,
    ExploitRegister,
    ExploitType,
    FairnessValues,
    GuidanceContext,
    GuidanceSource,
    InvasiveControlRequest,
    MitigationAction,
    RuntimeFairnessHealth,
    RuntimeTrustRemedy
  }
  import AdminOperations.{
    DashboardRegistry,
    FeatureFlag,
    GameOperationalVersions,
    IncidentPlaybooks,
    IncidentType,
    OpsAction,
    OpsHealthSnapshot,
    OpsMetric,
    OpsSignal
  }
  import ProductInvariants.RequirementClass
  import TrustOpsIncidentControls.*

  private val now = 123456789L

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

  private val legalGuidance =
    GuidanceContext(
      source = GuidanceSource.PlatformCoaching,
      disclosedToOpponent = true,
      setLevelCapped = true,
      serverAudited = true,
      pricedIntoEcr = true
    )

  private def signal(
      exploit: ExploitType,
      guidance: Option[GuidanceContext] = Some(legalGuidance),
      probe: Option[CandidateProbePattern] = None,
      runtimeHealth: Option[RuntimeFairnessHealth] = None,
      copyScanHits: List[String] = Nil
  ) =
    TrustSignal(
      signalId = s"sig-$exploit",
      exploit = exploit,
      gameId = "game-1",
      playerId = "player-1",
      moveRange = "12-20",
      evidence = "test evidence",
      guidance = guidance,
      probePattern = probe,
      runtimeHealth = runtimeHealth,
      copyScanHits = copyScanHits,
      auditId = "audit-1",
      occurredAt = now
    )

  private val versions =
    GameOperationalVersions(
      gameId = "game-1",
      policyVersion = "policy-v1",
      modelVersion = "model-v1",
      configVersion = "config-v1",
      engineVersion = "stockfish-16.1",
      featureFlagVersions = Map("l7-candidates" -> "flags-v1")
    )

  private val rollbackFlag =
    FeatureFlag(
      key = "l8-candidate-depth",
      risky = true,
      enabled = true,
      configVersion = "flags-v1",
      rollbackVersion = Some("flags-v0"),
      owner = "ops",
      auditId = "audit-flags"
    )

  private val healthSnapshot =
    OpsHealthSnapshot(
      OpsSignal.values.toList.map(signal => OpsMetric(signal, value = 1, unit = "count", healthy = true))
    )

  test("Version 1.1 Phase K requirements are classified before implementation"):
    val byRequirement =
      PhaseKRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseKRequirement.LichessModerationAndAdminFoundation), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseKRequirement.NonPlatformGuidanceEscalation), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKRequirement.ExploitRegisterRuntimeMapping), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKRequirement.IncidentRecordsAndPlaybooks), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKRequirement.MvpInvasiveControlsExcluded), RequirementClass.UnresolvedProductOwnerDecision)
    assertEquals(byRequirement(PhaseKRequirement.AdminOpsSeam), RequirementClass.AdaptedToLichessFork)

  test("non-platform guidance creates fair-play escalation with safe lila moderation context"):
    val external = legalGuidance.copy(source = GuidanceSource.ExternalEngine)
    val decision = TrustOpsPolicy.decide(
      signal(ExploitType.NonPlatformGuidance, guidance = Some(external)),
      fairness,
      fairness,
      visibleReason = "external engine report",
      ledgerSummaryId = "ledger-summary-1",
      visibleAssistanceState = "L5 disclosed"
    )

    assert(decision.valid)
    assert(decision.fairPlayEscalation.exists(_.valid))
    assert(decision.fairPlayEscalation.exists(_.moderationPolicy.usesExistingLilaModerationPatterns))
    assert(decision.fairPlayEscalation.exists(!_.moderationPolicy.exposesAntiCheatInternals))
    assert(decision.mitigations.contains(MitigationAction.FairPlayReport))

  test("legal platform coaching does not create external-help escalation"):
    val decision = TrustOpsPolicy.decide(
      signal(ExploitType.CandidateFollowAbuse),
      fairness,
      fairness,
      visibleReason = "candidate budget review",
      ledgerSummaryId = "ledger-summary-1",
      visibleAssistanceState = "L5 disclosed"
    )

    assert(decision.valid)
    assert(decision.fairPlayEscalation.isEmpty)
    assert(decision.signal.guidance.exists(_.legalInRatedEvenChess))

  test("exploit mapping covers engine, AI, stale, token, target, and marketing incidents"):
    assertEquals(ExploitIncidentMapping.incidentType(ExploitType.EngineOutage), Some(IncidentType.EngineOutage))
    assertEquals(ExploitIncidentMapping.incidentType(ExploitType.AiTimeout), Some(IncidentType.AiOutage))
    assertEquals(ExploitIncidentMapping.incidentType(ExploitType.PromptInjection), Some(IncidentType.AiOutage))
    assertEquals(ExploitIncidentMapping.incidentType(ExploitType.Desync), Some(IncidentType.StaleCoaching))
    assertEquals(ExploitIncidentMapping.incidentType(ExploitType.AdTokenFarming), Some(IncidentType.TokenBillingIssue))
    assertEquals(ExploitIncidentMapping.incidentType(ExploitType.TargetAbuse), Some(IncidentType.RatingCorruption))
    assertEquals(ExploitIncidentMapping.incidentType(ExploitType.PremiumMisrepresentation), Some(IncidentType.MarketingCopyIssue))
    assert(ExploitRegister.coversAllKnownExploits)

  test("fairness-changing abuse remedies require audit id and visible reason"):
    val changed = fairness.copy(ecrPolicy = "no-rate")
    val good = TrustOpsPolicy.decide(
      signal(ExploitType.EngineOutage),
      fairness,
      changed,
      visibleReason = "asymmetric engine health",
      ledgerSummaryId = "ledger-summary-1",
      visibleAssistanceState = "L5 disclosed"
    )
    val silent = good.copy(abuseDecision = good.abuseDecision.copy(auditId = "", visibleReason = ""))

    assert(good.valid)
    assert(good.fairnessRemedyAudited)
    assert(good.mitigations.contains(MitigationAction.NoRate))
    assert(!silent.valid)

  test("high-volume candidate probing is capped only with audited suppression"):
    val highProbe = CandidateProbePattern("player-1", hoverCount = 75, revealCount = 20, requestCount = 10, windowSeconds = 60)
    val decision = TrustOpsPolicy.decide(
      signal(ExploitType.CandidateFollowAbuse, probe = Some(highProbe)),
      fairness,
      fairness,
      visibleReason = "high-volume candidate probing",
      ledgerSummaryId = "ledger-summary-1",
      visibleAssistanceState = "L5 disclosed"
    )
    val unaudited = decision.copy(probeControl = decision.probeControl.map(_.copy(suppressionAudited = false, auditId = "")))

    assert(decision.valid)
    assert(decision.probeControl.exists(_.capped))
    assert(decision.probeControl.exists(_.cooldown))
    assert(decision.probeControl.exists(_.valid))
    assert(!unaudited.valid)

  test("runtime trust remedies handle outage, AI timeout, desync, stale board state, and mobile disclosure loss"):
    val healthy = RuntimeFairnessHealth(
      engineHealthyForBoth = true,
      aiTimedOut = false,
      sequenceMatches = true,
      boardHashMatches = true,
      mobileDisclosureVisible = true
    )

    assertEquals(signal(ExploitType.EngineOutage, runtimeHealth = Some(healthy.copy(engineHealthyForBoth = false))).runtimeRemedy, Some(RuntimeTrustRemedy.NoRate))
    assertEquals(signal(ExploitType.AiTimeout, runtimeHealth = Some(healthy.copy(aiTimedOut = true))).runtimeRemedy, Some(RuntimeTrustRemedy.SuppressAi))
    assertEquals(signal(ExploitType.Desync, runtimeHealth = Some(healthy.copy(sequenceMatches = false))).runtimeRemedy, Some(RuntimeTrustRemedy.ServerResync))
    assertEquals(signal(ExploitType.StaleCoaching, runtimeHealth = Some(healthy.copy(boardHashMatches = false))).runtimeRemedy, Some(RuntimeTrustRemedy.ClearStaleCoaching))
    assertEquals(signal(ExploitType.MobileUiFailure, runtimeHealth = Some(healthy.copy(mobileDisclosureVisible = false))).runtimeRemedy, Some(RuntimeTrustRemedy.Pause))

  test("incident records include playbook actions, audit trail, and valid public notice where needed"):
    val decision = TrustOpsPolicy.decide(
      signal(ExploitType.EngineOutage),
      fairness,
      fairness.copy(ecrPolicy = "no-rate"),
      visibleReason = "asymmetric engine health",
      ledgerSummaryId = "ledger-summary-1",
      visibleAssistanceState = "L5 disclosed"
    )

    assert(decision.incidentRecord.exists(_.valid))
    assert(decision.incidentRecord.exists(_.requiredResponsesCovered))
    assert(decision.incidentActions.contains(OpsAction.NoRate))
    assert(decision.incidentActions.contains(OpsAction.Annul))
    assert(decision.incidentRecord.flatMap(_.publicNotice).exists(_.valid))
    assert(IncidentPlaybooks.coversAllKnownIncidents)

  test("ops readiness requires dashboards, runtime signals, active versions, and rollbackable flags"):
    val evidence = OpsReadinessEvidence(
      healthSnapshot = healthSnapshot,
      dashboardsComplete = true,
      activeVersions = versions,
      featureFlags = List(rollbackFlag),
      incidentPlaybooksComplete = IncidentPlaybooks.coversAllKnownIncidents
    )

    assert(evidence.accepted)
    assert(DashboardRegistry.coversMinimumDashboards(DashboardRegistry.definitions))
    assert(DashboardRegistry.coversMinimumSources(DashboardRegistry.definitions))
    assert(!evidence.copy(activeVersions = versions.copy(modelVersion = "")).accepted)
    assert(!evidence.copy(featureFlags = List(rollbackFlag.copy(rollbackVersion = None))).accepted)

  test("MVP invasive controls stay disabled unless product and privacy approvals exist"):
    assert(MvpInvasiveControlPolicy.allMvpInvasiveControlsDisabled)
    assert(!MvpInvasiveControlPolicy.mayEnable(InvasiveControlRequest("phone", productOwnerApproved = true, privacyReviewApproved = false)))
    assert(!MvpInvasiveControlPolicy.mayEnable(InvasiveControlRequest("device", productOwnerApproved = false, privacyReviewApproved = true)))
    assert(MvpInvasiveControlPolicy.mayEnable(InvasiveControlRequest("future-approved-control", productOwnerApproved = true, privacyReviewApproved = true)))

  test("Appendix V trust and ops acceptance fails closed on missing audit, rollback, playbook, or patch-map evidence"):
    val ops = OpsReadinessEvidence(
      healthSnapshot = healthSnapshot,
      dashboardsComplete = true,
      activeVersions = versions,
      featureFlags = List(rollbackFlag),
      incidentPlaybooksComplete = true
    )
    val accepted = TrustOpsAcceptanceEvidence(
      exploitRegisterComplete = true,
      nonPlatformGuidanceEscalates = true,
      copyScansPass = true,
      repeatCollusionSimulationsPass = true,
      candidateBudgetsAuditPass = true,
      promptInjectionBlocked = true,
      staleAndDesyncHandled = true,
      tokenAbuseHandled = true,
      noRateAnnulAuditPass = true,
      mobileDisclosurePass = true,
      rollbackWorks = true,
      incidentPlaybooksExist = true,
      patchMapCurrent = true,
      opsReadiness = ops
    )

    assert(accepted.accepted)
    assert(!accepted.copy(noRateAnnulAuditPass = false).accepted)
    assert(!accepted.copy(rollbackWorks = false).accepted)
    assert(!accepted.copy(incidentPlaybooksExist = false).accepted)
    assert(!accepted.copy(patchMapCurrent = false).accepted)
    assert(!accepted.copy(opsReadiness = ops.copy(dashboardsComplete = false)).accepted)

  test("future lila admin adapter uses registered AdminOps seam and no patch map entry is needed yet"):
    assert(AdminOpsSeam.registered)
    assert(AdminOpsSeam.patchMapRequiredBeforeLilaAdapter)
    assert(!AdminOpsSeam.patchMapEntryRequiredNow)
