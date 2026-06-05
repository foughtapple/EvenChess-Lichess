package lila.evenchess

class AdminOperationsTest extends munit.FunSuite:

  import AdminOperations.*
  import CoachingLadder.{ ExactnessClass, Level }
  import MonetisationPolicy.FairnessSnapshot
  import ProductInvariants.RequirementClass

  private val fairness =
    FairnessSnapshot(
      setLevel = Level(5),
      usedLevel = Level(4),
      assistanceLoadPolicyVersion = "load-v1",
      usedOffsetPolicyVersion = "offset-v1",
      ecrPolicyVersion = "ecr-v1",
      matchmakingPolicyVersion = "match-v1",
      stockfishProfileKey = "sf-l5-rapid",
      aiExactnessClass = ExactnessClass.Heuristic,
      targetIsolationKey = "target-isolated",
      liveCoachingStrengthKey = "same-live-policy"
    )

  private val allMetrics =
    OpsSignal.values.toList.map: signal =>
      OpsMetric(signal, value = 1, unit = "count", healthy = true)

  test("Appendix R requirements are classified before implementation"):
    val byRequirement =
      AdminOpsRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(AdminOpsRequirement.MonitorRuntimeHealth), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AdminOpsRequirement.OutageFairnessRemedies), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AdminOpsRequirement.RollbackableRiskyFeatures), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AdminOpsRequirement.FairnessChangesThroughVersionedPolicy), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AdminOpsRequirement.ExistingLichessAdminPatterns), RequirementClass.AdaptedToLichessFork)

  test("runtime monitoring includes Stockfish queue, engine latency, AI latency, fallback, stale events, and cost"):
    val snapshot = OpsHealthSnapshot(allMetrics)
    val degraded = snapshot.copy(metrics = allMetrics.map {
      case metric if metric.signal == OpsSignal.EngineLatency => metric.copy(healthy = false)
      case metric                                             => metric
    })

    assert(snapshot.monitors(RuntimeMonitoring.requiredOpsSignals))
    assert(degraded.degradedSignals.contains(OpsSignal.EngineLatency))
    assert(allMetrics.forall(_.valid))

  test("minimum dashboards cover required Appendix R surfaces and sources"):
    assert(DashboardRegistry.coversMinimumDashboards(DashboardRegistry.definitions))
    assert(DashboardRegistry.coversMinimumSources(DashboardRegistry.definitions))
    assert(!DashboardRegistry.coversMinimumDashboards(DashboardRegistry.definitions.filterNot(_.dashboard == Dashboard.ActiveVersions)))
    assert(!DashboardDefinition(Dashboard.EngineAiHealth, Set.empty, "empty").complete)

  test("incident playbooks cover all known Appendix R incidents with required responses"):
    assert(IncidentPlaybooks.coversAllKnownIncidents)
    assert(IncidentPlaybooks.byType(IncidentType.EngineOutage).requiredResponses.contains(OpsAction.NoRate))
    assert(IncidentPlaybooks.byType(IncidentType.EngineOutage).requiredResponses.contains(OpsAction.Annul))
    assert(IncidentPlaybooks.byType(IncidentType.AiOutage).requiredResponses.contains(OpsAction.Fallback))
    assert(IncidentPlaybooks.byType(IncidentType.StaleCoaching).requiredResponses.contains(OpsAction.InvestigateTtlHash))
    assert(IncidentPlaybooks.byType(IncidentType.MarketingCopyIssue).requiredResponses.contains(OpsAction.KillVariant))
    assert(IncidentPlaybooks.byType(IncidentType.DataPrivacyIssue).requiredResponses.contains(OpsAction.ReviewRetention))

  test("asymmetric assistance outages expose pause, downgrade, no-rate, and annul remedies"):
    val outage = AssistanceOutage(
      engineHealthyForBothPlayers = false,
      aiHealthy = true,
      staleCoachingDetected = false,
      asymmetricAssistance = true,
      fairnessAffected = true
    )
    val aiOutage = outage.copy(engineHealthyForBothPlayers = true, aiHealthy = false, asymmetricAssistance = false, fairnessAffected = false)
    val stale = outage.copy(engineHealthyForBothPlayers = true, aiHealthy = true, staleCoachingDetected = true, asymmetricAssistance = false, fairnessAffected = false)

    assertEquals(OutageRemedyPolicy.requiredRemedies(outage), OutageRemedyPolicy.fairnessRemedies)
    assert(OutageRemedyPolicy.requiredRemedies(aiOutage).contains(OpsAction.Fallback))
    assert(OutageRemedyPolicy.requiredRemedies(stale).contains(OpsAction.ClearStale))

  test("risky feature flags require admin metadata and rollback versions"):
    val risky = FeatureFlag(
      key = "l10-live-candidate",
      risky = true,
      enabled = true,
      configVersion = "flags-v1",
      rollbackVersion = Some("flags-v0"),
      owner = "ops",
      auditId = "audit-1"
    )
    val missingRollback = risky.copy(rollbackVersion = None)
    val disabledDraft = risky.copy(enabled = false, auditId = "", rollbackVersion = None)

    assert(risky.safeToEnable)
    assert(risky.rollbackable)
    assert(!missingRollback.safeToEnable)
    assert(disabledDraft.safeToEnable)

  test("operators can identify active policy, model, config, engine, and feature flag versions for a game"):
    val versions = GameOperationalVersions(
      gameId = "game-1",
      policyVersion = "policy-v1",
      modelVersion = "model-v1",
      configVersion = "config-v1",
      engineVersion = "stockfish-16.1",
      featureFlagVersions = Map("l7-candidates" -> "flag-v1")
    )

    assert(versions.visibleToOperators)
    assert(!versions.copy(policyVersion = "").visibleToOperators)
    assert(!versions.copy(featureFlagVersions = Map.empty).visibleToOperators)

  test("admin fairness changes require a complete versioned policy path"):
    val unchanged = AdminFairnessChange(fairness, fairness, None)
    val changed = fairness.copy(stockfishProfileKey = "sf-l6-rapid")
    val validPath = VersionedPolicyPath(
      policyVersion = "policy-v2",
      configVersion = "config-v2",
      auditId = "audit-2",
      operatorId = "admin-1",
      visibleReason = "engine outage downgrade"
    )

    assert(unchanged.allowed)
    assert(AdminFairnessChange(fairness, changed, Some(validPath)).allowed)
    assert(!AdminFairnessChange(fairness, changed, None).allowed)
    assert(!AdminFairnessChange(fairness, changed, Some(validPath.copy(auditId = ""))).allowed)

  test("launch health pauses degraded ads, promotions, variants, tracking, windows, and queue campaigns"):
    val degraded = LaunchHealth(
      rewardedAdsHealthy = false,
      standardPromotionHealthy = false,
      premiumPromotionHealthy = false,
      campaignVariantsHealthy = false,
      trackingDestinationsHealthy = false,
      playWindowsHealthy = false,
      queueFacingCampaignsHealthy = false
    )
    val notice = PauseNotice(
      message = "Paid acquisition paused while queue health recovers.",
      public = true,
      manipulatesHiddenQueueOrFairness = false
    )

    assertEquals(LaunchHealthPolicy.surfacesToPause(degraded), LaunchSurface.values.toSet)
    assertEquals(LaunchHealthPolicy.surfacesToPause(degraded.copy(rewardedAdsHealthy = true)).size, LaunchSurface.values.size - 1)
    assert(notice.valid)
    assert(!notice.copy(public = false).valid)
    assert(!notice.copy(manipulatesHiddenQueueOrFairness = true).valid)

  test("serious paid launch requires verified or explicitly unavailable tracking and conversion checks"):
    val verified =
      PaidLaunchReadiness(
        PaidLaunchCheck.values.map(check => check -> LaunchCheckStatus(verified = true, explicitlyUnavailableDecision = None)).toMap
      )
    val unavailableWithDecision =
      PaidLaunchReadiness(
        PaidLaunchCheck.values.map(check => check -> LaunchCheckStatus(verified = false, explicitlyUnavailableDecision = Some(s"$check unavailable decision"))).toMap
      )
    val missingDecision =
      PaidLaunchReadiness(
        PaidLaunchCheck.values.map(check => check -> LaunchCheckStatus(verified = true, explicitlyUnavailableDecision = None)).toMap
          .updated(PaidLaunchCheck.MetaPixelCapi, LaunchCheckStatus(verified = false, explicitlyUnavailableDecision = None))
      )

    assert(verified.seriousPaidLaunchAllowed)
    assert(unavailableWithDecision.seriousPaidLaunchAllowed)
    assert(!missingDecision.seriousPaidLaunchAllowed)

  test("incident records require audit trail, playbook coverage, and valid public notices"):
    val record = IncidentRecord(
      incidentId = "incident-1",
      incidentType = IncidentType.EngineOutage,
      status = IncidentStatus.Mitigating,
      actionsTaken = Set(OpsAction.Suppress, OpsAction.Downgrade, OpsAction.NoRate, OpsAction.Annul),
      auditId = "audit-1",
      publicNotice = Some(PauseNotice("Engine help degraded; affected games are under review.", public = true, manipulatesHiddenQueueOrFairness = false))
    )

    assert(record.valid)
    assert(record.requiredResponsesCovered)
    assert(!record.copy(actionsTaken = Set(OpsAction.Suppress)).valid)
    assert(!record.copy(auditId = "").valid)
    assert(!record.copy(publicNotice = Some(PauseNotice("", public = true, manipulatesHiddenQueueOrFairness = false))).valid)
