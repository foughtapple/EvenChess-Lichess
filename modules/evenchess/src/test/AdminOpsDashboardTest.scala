package lila.evenchess

class AdminOpsDashboardTest extends munit.FunSuite:

  import AdminBackendSettings.{ SettingIds, default as backendDefault }
  import AdminOperations.{ Dashboard, IncidentType, OpsAction }
  import AdminOpsDashboard.*
  import ProductInvariants.RequirementClass

  private val now = 123456789L

  test("Version 1.2 Phase K dashboard requirements are classified before implementation"):
    val byRequirement =
      PhaseKDashboardRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseKDashboardRequirement.LichessAdminShell), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseKDashboardRequirement.DashboardRouteAdapter), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseKDashboardRequirement.RuntimeHealthPanels), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKDashboardRequirement.ActiveVersionVisibility), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseKDashboardRequirement.AntiCheatInternalRedaction), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseKDashboardRequirement.FairnessActionsUseVersionedPolicy), RequirementClass.EvenChessSpecific)

  test("dashboard model covers required operations panels and sources"):
    val model = AdminOpsDashboardModel.build(AuditSearchQuery(""), backendDefault, now)
    val dashboards = model.panels.map(_.dashboard).toSet

    assert(model.valid)
    assertEquals(dashboards, Dashboard.values.toSet)
    assert(model.panels.exists(_.dashboard == Dashboard.EngineAiHealth))
    assert(model.panels.exists(_.dashboard == Dashboard.QueueHealth))
    assert(model.panels.exists(_.dashboard == Dashboard.TokensAndSummaries))
    assert(model.panels.exists(_.dashboard == Dashboard.ActiveVersions))
    assert(model.panels.forall(_.tiles.nonEmpty))
    assert(model.botOperations.valid)
    assert(!model.botOperations.simulation.runtime.running)
    assert(model.ecor.valid)
    assertEquals(model.ecor.config.offsetValueForLevel(CoachingLadder.Level(10)), 190)
    assert(model.stockfishAiTable.valid)
    assertEquals(model.stockfishAiTable.levelForRating(1500), CoachingLadder.Level(5))

  test("active versions expose policy, AI, TTS, config, Stockfish, engine, and feature flag values"):
    val model = AdminOpsDashboardModel.build(AuditSearchQuery(""), backendDefault, now)
    val labels = model.activeVersions.map(_.label).toSet

    assert(labels.contains("Game policy"))
    assert(labels.contains("AI model"))
    assert(labels.contains("TTS provider"))
    assert(labels.contains("Config version"))
    assert(labels.contains("ECOR table"))
    assert(labels.contains("Stockfish profile"))
    assert(labels.contains("Engine version"))
    assert(labels.contains("Feature flags"))
    assert(model.activeVersions.forall(_.valid))

  test("dashboard model reflects supplied admin backend settings"):
    val backend =
      backendDefault.copy(
        openAi = backendDefault.openAi.copy(model = "gpt-evenchess-admin-test"),
        tts = backendDefault.tts.copy(provider = "provider-admin-test"),
        stockfish = backendDefault.stockfish.copy(profile = "stockfish-admin-test"),
        incident = backendDefault.incident.copy(enginePaused = true, publicNotice = "Engine assistance degraded.")
      )
    val model = AdminOpsDashboardModel.build(AuditSearchQuery(""), backend, now)
    val byLabel = model.activeVersions.map(row => row.label -> row.value).toMap

    assertEquals(byLabel("AI model"), "gpt-evenchess-admin-test")
    assertEquals(byLabel("TTS provider"), "provider-admin-test")
    assertEquals(byLabel("Stockfish profile"), "stockfish-admin-test")
    assert(model.incidentControlState.active)
    assertEquals(model.incidentControlState.publicNotice, Some("Engine assistance degraded."))
    assert(model.degradedPanels.nonEmpty)

  test("operator actions expose pause no-rate rollback paths with audit and settings linkage"):
    val model = AdminOpsDashboardModel.build(AuditSearchQuery(""), backendDefault, now)
    val byAction = model.operatorActions.map(action => action.action -> action).toMap

    assertEquals(byAction(OpsAction.Pause).settingId, Some(SettingIds.incidentLiveCoachingPaused))
    assertEquals(byAction(OpsAction.NoRate).settingId, Some(SettingIds.incidentNoRate))
    assert(byAction(OpsAction.NoRate).fairnessAffecting)
    assert(byAction(OpsAction.NoRate).requiresAuditTrail)
    assert(byAction(OpsAction.NoRate).rollbackable)
    assert(model.operatorActions.forall(_.valid))
    assert(OperatorActions.fairnessActions.forall(_.settingId.exists(SettingIds.canAffectRatedFairness)))

  test("audit search is queryable without raw secrets or anti-cheat internals"):
    val model = AdminOpsDashboardModel.build(AuditSearchQuery("engine"), backendDefault, now)

    assert(model.auditQuery.active)
    assert(model.auditResults.nonEmpty)
    assert(model.auditResults.exists(_.category == "engine_outage"))
    assert(model.auditResults.forall(_.safeForAdminDashboard))
    assert(model.auditResults.forall(!_.exposesRawProviderSecret))
    assert(model.auditResults.forall(!_.exposesAntiCheatInternals))

  test("paid launch rows fail closed through explicit unavailable decisions in local dashboard"):
    val model = AdminOpsDashboardModel.build(AuditSearchQuery(""), backendDefault, now)

    assert(model.paidLaunchRows.nonEmpty)
    assert(model.paidLaunchRows.forall(_.valid))
    assert(model.paidLaunchRows.forall(!_.verified))
    assert(model.paidLaunchRows.forall(_.explicitlyUnavailableDecision.nonEmpty))

  test("incident records and feature flags are auditable and rollbackable"):
    val model = AdminOpsDashboardModel.build(AuditSearchQuery(""), backendDefault, now)

    assert(model.incidentRecords.exists(_.incidentType == IncidentType.EngineOutage))
    assert(model.incidentRecords.forall(_.valid))
    assert(model.incidentRecords.forall(_.hasAuditTrail))
    assert(model.featureFlags.nonEmpty)
    assert(model.featureFlags.forall(_.safeToEnable))
    assert(model.featureFlags.exists(_.rollbackVersion.contains("flags-v0")))

  test("dashboard remains secret-safe and redacts anti-cheat internals"):
    val model = AdminOpsDashboardModel.build(AuditSearchQuery(""), backendDefault, now)

    assert(model.secretSafe)
    assert(model.antiCheatInternalsRedacted)
    assert(!model.activeVersions.exists(_.value.contains("sk-")))
    assert(!model.auditResults.exists(_.summary.toLowerCase.contains("anti-cheat")))
