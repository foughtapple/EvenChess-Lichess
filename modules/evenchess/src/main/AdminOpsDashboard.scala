package lila.evenchess

import AdminBackendSettings.{ BackendSettings, SettingIds }
import AdminOperations.{
  Dashboard,
  DashboardDefinition,
  DashboardRegistry,
  FeatureFlag,
  GameOperationalVersions,
  IncidentPlaybooks,
  IncidentRecord,
  IncidentStatus,
  IncidentType,
  LaunchCheckStatus,
  OpsAction,
  OpsHealthSnapshot,
  OpsMetric,
  OpsSignal,
  PaidLaunchCheck,
  PaidLaunchReadiness,
  RuntimeMonitoring
}
import ProductInvariants.RequirementClass

object AdminOpsDashboard:

  enum PhaseKDashboardRequirement:
    case LichessAdminShell
    case DashboardRouteAdapter
    case RuntimeHealthPanels
    case ActiveVersionVisibility
    case IncidentControls
    case AuditLedgerSearch
    case PaidLaunchReadiness
    case SecretNonExposure
    case AntiCheatInternalRedaction
    case FairnessActionsUseVersionedPolicy

  final case class PhaseKDashboardRequirementClassification(
      requirement: PhaseKDashboardRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseKDashboardRequirementClassifications:
    val all: List[PhaseKDashboardRequirementClassification] = List(
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.LichessAdminShell,
        RequirementClass.LichessProvided,
        "Use existing lila dev/mod admin shell and Settings permission; do not rebuild an admin platform."
      ),
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.DashboardRouteAdapter,
        RequirementClass.AdaptedToLichessFork,
        "Add a thin `/dev/evenchess/ops` route and view that render namespaced EvenChess ops state."
      ),
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.RuntimeHealthPanels,
        RequirementClass.EvenChessSpecific,
        "Expose AI, TTS, Stockfish/engine, queue, overlay, token, funnel, and calibration health panels."
      ),
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.ActiveVersionVisibility,
        RequirementClass.EvenChessSpecific,
        "Operators can see active policy, model, config, engine, TTS, overlay, and feature-flag versions."
      ),
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.IncidentControls,
        RequirementClass.EvenChessSpecific,
        "Pause, no-rate, rollback, downgrade, and incident actions are shown with audit/version requirements."
      ),
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.AuditLedgerSearch,
        RequirementClass.EvenChessSpecific,
        "Audit search exposes game/player/audit summaries without raw provider secrets or anti-cheat internals."
      ),
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.PaidLaunchReadiness,
        RequirementClass.EvenChessSpecific,
        "Paid-launch checks show verified/unavailable status for signup, first game, tokens, purchases, cancellation, and tracking."
      ),
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.SecretNonExposure,
        RequirementClass.EvenChessSpecific,
        "Admin dashboard rows must never render raw API keys, prompts, tokens, or provider secrets."
      ),
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.AntiCheatInternalRedaction,
        RequirementClass.AdaptedToLichessFork,
        "Use existing lila moderation/admin patterns and avoid exposing sensitive anti-cheat internals."
      ),
      PhaseKDashboardRequirementClassification(
        PhaseKDashboardRequirement.FairnessActionsUseVersionedPolicy,
        RequirementClass.EvenChessSpecific,
        "Fairness-affecting actions require an audit id and versioned policy/config path."
      )
    )

  enum HealthSeverity:
    case Healthy
    case Warning
    case Critical
    case Paused

    def label: String =
      this match
        case Healthy  => "Healthy"
        case Warning  => "Warning"
        case Critical => "Critical"
        case Paused   => "Paused"

  final case class HealthTile(
      signal: OpsSignal,
      label: String,
      value: String,
      unit: String,
      severity: HealthSeverity,
      source: String
  ):
    def valid: Boolean =
      label.nonEmpty &&
        value.nonEmpty &&
        unit.nonEmpty &&
        source.nonEmpty

  object HealthTile:
    def fromMetric(metric: OpsMetric): HealthTile =
      HealthTile(
        signal = metric.signal,
        label = signalLabel(metric.signal),
        value =
          if metric.value.isWhole then metric.value.toLong.toString
          else f"${metric.value}%.2f",
        unit = metric.unit,
        severity = if metric.healthy then HealthSeverity.Healthy else HealthSeverity.Warning,
        source = "evenchess-ops-snapshot"
      )

  final case class DashboardPanel(
      dashboard: Dashboard,
      title: String,
      sourceDescription: String,
      tiles: List[HealthTile],
      severity: HealthSeverity
  ):
    def valid: Boolean =
      title.nonEmpty &&
        sourceDescription.nonEmpty &&
        tiles.nonEmpty &&
        tiles.forall(_.valid)

  object DashboardPanels:
    def from(snapshot: OpsHealthSnapshot, definitions: List[DashboardDefinition] = DashboardRegistry.definitions): List[DashboardPanel] =
      definitions.map: definition =>
        val tiles = definition.sources.toList.sortBy(_.ordinal).flatMap: signal =>
          snapshot.metrics.find(_.signal == signal).map(HealthTile.fromMetric)
        DashboardPanel(
          dashboard = definition.dashboard,
          title = dashboardTitle(definition.dashboard),
          sourceDescription = definition.sourceDescription,
          tiles = tiles,
          severity =
            if tiles.exists(_.severity == HealthSeverity.Critical) then HealthSeverity.Critical
            else if tiles.exists(_.severity == HealthSeverity.Warning) then HealthSeverity.Warning
            else HealthSeverity.Healthy
        )

    private def dashboardTitle(dashboard: Dashboard): String =
      dashboard match
        case Dashboard.EngineAiHealth      => "Engine, AI, and TTS health"
        case Dashboard.CoachingDelivery    => "Overlay delivery"
        case Dashboard.AssistanceAccounting => "Assistance accounting"
        case Dashboard.RatingCalibration   => "ECR calibration"
        case Dashboard.QueueHealth         => "Queue health"
        case Dashboard.TokensAndSummaries  => "Tokens, ads, and summaries"
        case Dashboard.FunnelPurchases     => "Funnel and purchases"
        case Dashboard.AbuseCases          => "Trust and abuse"
        case Dashboard.FeatureFlags        => "Feature flags"
        case Dashboard.ActiveVersions      => "Active versions"

  final case class VersionRow(
      label: String,
      value: String,
      source: String
  ):
    def valid: Boolean =
      label.nonEmpty &&
        value.nonEmpty &&
        source.nonEmpty &&
        AdminBackendSettings.safeLogValue(label, value) == value

  object ActiveVersions:
    def rows(versions: GameOperationalVersions, backend: BackendSettings): List[VersionRow] = List(
      VersionRow("Game policy", versions.policyVersion, "game policy record"),
      VersionRow("AI model", backend.openAi.model, "EvenChess backend settings"),
      VersionRow("TTS provider", backend.tts.provider, "EvenChess backend settings"),
      VersionRow("Config version", versions.configVersion, "game policy/config"),
      VersionRow("ECOR table", backend.ecor.policyVersion, "EvenChess offset ratings table"),
      VersionRow("Stockfish profile", backend.stockfish.profile, "EvenChess backend settings"),
      VersionRow("Engine version", versions.engineVersion, "engine inventory"),
      VersionRow("Feature flags", versions.featureFlagVersions.values.toList.sorted.mkString(", "), "game policy feature flags")
    )
  final case class OperatorActionRow(
      action: OpsAction,
      label: String,
      settingId: Option[String],
      fairnessAffecting: Boolean,
      requiresAuditTrail: Boolean,
      rollbackable: Boolean,
      description: String
  ):
    def valid: Boolean =
      label.nonEmpty &&
        description.nonEmpty &&
        (!fairnessAffecting || (requiresAuditTrail && settingId.exists(SettingIds.canAffectRatedFairness))) &&
        rollbackable

  object OperatorActions:
    val all: List[OperatorActionRow] = List(
      OperatorActionRow(OpsAction.Pause, "Pause live assistance", Some(SettingIds.incidentLiveCoachingPaused), fairnessAffecting = true, requiresAuditTrail = true, rollbackable = true, "Use for asymmetric assistance or stale live coaching incidents."),
      OperatorActionRow(OpsAction.NoRate, "No-rate affected games", Some(SettingIds.incidentNoRate), fairnessAffecting = true, requiresAuditTrail = true, rollbackable = true, "Use only with audit id and versioned policy reason."),
      OperatorActionRow(OpsAction.Downgrade, "Downgrade help surface", Some(SettingIds.incidentEnginePaused), fairnessAffecting = true, requiresAuditTrail = true, rollbackable = true, "Suppress or degrade engine-backed help while preserving disclosure."),
      OperatorActionRow(OpsAction.Fallback, "Fallback AI wording", Some(SettingIds.incidentAiPaused), fairnessAffecting = false, requiresAuditTrail = true, rollbackable = true, "Use deterministic fallback or suppress invalid AI output."),
      OperatorActionRow(OpsAction.ClearStale, "Clear stale overlays", None, fairnessAffecting = false, requiresAuditTrail = true, rollbackable = true, "Clear advice for mismatched board hash, ply, clock, or TTL."),
      OperatorActionRow(OpsAction.RefundOrRestore, "Refund or restore tokens", Some(SettingIds.incidentTokenAdsPaused), fairnessAffecting = false, requiresAuditTrail = true, rollbackable = true, "Pause bad token/ad paths before restoring balances through ledger-backed tooling."),
      OperatorActionRow(OpsAction.KillVariant, "Kill campaign variant", Some(SettingIds.campaignKillSwitch), fairnessAffecting = false, requiresAuditTrail = true, rollbackable = true, "Use for unsafe marketing copy or broken funnel state.")
    )

    def fairnessActions: List[OperatorActionRow] =
      all.filter(_.fairnessAffecting)

  final case class AuditSearchQuery(value: String):
    def normalized: String = value.trim.toLowerCase
    def active: Boolean = normalized.nonEmpty

  final case class AuditSearchResult(
      auditId: String,
      gameId: String,
      playerId: String,
      category: String,
      summary: String,
      policyVersion: String,
      exposesRawProviderSecret: Boolean,
      exposesAntiCheatInternals: Boolean
  ):
    def matches(query: AuditSearchQuery): Boolean =
      !query.active ||
        List(auditId, gameId, playerId, category, summary, policyVersion)
          .exists(_.toLowerCase.contains(query.normalized))

    def safeForAdminDashboard: Boolean =
      auditId.nonEmpty &&
        gameId.nonEmpty &&
        playerId.nonEmpty &&
        category.nonEmpty &&
        summary.nonEmpty &&
        policyVersion.nonEmpty &&
        !exposesRawProviderSecret &&
        !exposesAntiCheatInternals

  object AuditSearch:
    val sampleRows: List[AuditSearchResult] = List(
      AuditSearchResult(
        auditId = "audit-engine-1",
        gameId = "game-1",
        playerId = "player-1",
        category = "engine_outage",
        summary = "Engine latency exceeded live threshold; no-rate review path available.",
        policyVersion = "policy-v1",
        exposesRawProviderSecret = false,
        exposesAntiCheatInternals = false
      ),
      AuditSearchResult(
        auditId = "audit-overlay-1",
        gameId = "game-2",
        playerId = "player-2",
        category = "stale_overlay",
        summary = "Overlay cleared after board hash mismatch.",
        policyVersion = "overlay-policy-v1",
        exposesRawProviderSecret = false,
        exposesAntiCheatInternals = false
      ),
      AuditSearchResult(
        auditId = "audit-token-1",
        gameId = "game-3",
        playerId = "player-3",
        category = "token_billing",
        summary = "Token/ad path paused pending ledger refund review.",
        policyVersion = "token-ledger-v1",
        exposesRawProviderSecret = false,
        exposesAntiCheatInternals = false
      )
    )

    def search(query: AuditSearchQuery): List[AuditSearchResult] =
      if query.active then sampleRows.filter(_.matches(query))
      else Nil

  final case class PaidLaunchCheckRow(
      check: PaidLaunchCheck,
      label: String,
      verified: Boolean,
      explicitlyUnavailableDecision: Option[String]
  ):
    def acceptable: Boolean =
      verified || explicitlyUnavailableDecision.exists(_.nonEmpty)

    def valid: Boolean =
      label.nonEmpty && acceptable

  object PaidLaunchRows:
    def from(readiness: PaidLaunchReadiness): List[PaidLaunchCheckRow] =
      PaidLaunchCheck.values.toList.map: check =>
        val status = readiness.statuses.getOrElse(check, LaunchCheckStatus(verified = false, explicitlyUnavailableDecision = None))
        PaidLaunchCheckRow(check, paidLaunchLabel(check), status.verified, status.explicitlyUnavailableDecision)

  final case class IncidentControlState(
      active: Boolean,
      publicNotice: Option[String],
      fairnessAffecting: Boolean,
      settingsHref: String
  ):
    def valid: Boolean =
      settingsHref.nonEmpty &&
        publicNotice.forall(_.nonEmpty)

  final case class AdminOpsDashboardModel(
      generatedAt: Long,
      panels: List[DashboardPanel],
      activeVersions: List[VersionRow],
      incidentControlState: IncidentControlState,
      operatorActions: List[OperatorActionRow],
      auditQuery: AuditSearchQuery,
      auditResults: List[AuditSearchResult],
      paidLaunchRows: List[PaidLaunchCheckRow],
      botOperations: BotOperations.BotOpsAdminState,
      ecor: EvenChessRatingCalibration.EcorAdminState,
      stockfishAiTable: EvenChessRatingCalibration.StockfishAiRatingTableConfig,
      incidentRecords: List[IncidentRecord],
      featureFlags: List[FeatureFlag],
      secretSafe: Boolean,
      antiCheatInternalsRedacted: Boolean
  ):
    def valid: Boolean =
      generatedAt > 0 &&
        DashboardRegistry.coversMinimumDashboards(DashboardRegistry.definitions) &&
        DashboardRegistry.coversMinimumSources(DashboardRegistry.definitions) &&
        panels.nonEmpty &&
        panels.forall(_.valid) &&
        panels.exists(_.dashboard == Dashboard.EngineAiHealth) &&
        activeVersions.nonEmpty &&
        activeVersions.forall(_.valid) &&
        incidentControlState.valid &&
        operatorActions.nonEmpty &&
        operatorActions.forall(_.valid) &&
        auditResults.forall(_.safeForAdminDashboard) &&
        paidLaunchRows.nonEmpty &&
        paidLaunchRows.forall(_.valid) &&
        botOperations.valid &&
        ecor.valid &&
        stockfishAiTable.valid &&
        incidentRecords.forall(_.valid) &&
        featureFlags.forall(_.safeToEnable) &&
        secretSafe &&
        antiCheatInternalsRedacted

    def degradedPanels: List[DashboardPanel] =
      panels.filter(panel => panel.severity != HealthSeverity.Healthy)

  object AdminOpsDashboardModel:
    def build(query: AuditSearchQuery, backend: BackendSettings, now: Long): AdminOpsDashboardModel =
      val snapshot = defaultHealthSnapshot(backend)
      val versions = defaultVersions(backend)
      val readiness = defaultPaidLaunchReadiness
      val incidentControls = backend.incident
      AdminOpsDashboardModel(
        generatedAt = now,
        panels = DashboardPanels.from(snapshot),
        activeVersions = ActiveVersions.rows(versions, backend),
        incidentControlState = IncidentControlState(
          active = incidentControls.active,
          publicNotice = Option.when(incidentControls.publicNotice.nonEmpty)(incidentControls.publicNotice),
          fairnessAffecting = incidentControls.canAffectRatedFairness,
          settingsHref = "/dev/settings"
        ),
        operatorActions = OperatorActions.all,
        auditQuery = query,
        auditResults = AuditSearch.search(query),
        paidLaunchRows = PaidLaunchRows.from(readiness),
        botOperations = BotOperations.adminState(
          backend,
          BotOperations.BotSimulationRuntime.status,
          now
        ),
        ecor = backend.ecor.adminState,
        stockfishAiTable = backend.stockfish.equivalentRatingTable.getOrElse(EvenChessRatingCalibration.StockfishAiRatingTableConfig.default),
        incidentRecords = defaultIncidents,
        featureFlags = defaultFeatureFlags,
        secretSafe = backend.safeAdminSnapshot.safeForBrowser,
        antiCheatInternalsRedacted = true
      )

    private def defaultHealthSnapshot(backend: BackendSettings): OpsHealthSnapshot =
      val pausedSignals: Set[OpsSignal] =
        Set(
          Option.when(backend.incident.enginePaused)(OpsSignal.EngineLatency),
          Option.when(backend.incident.aiPaused)(OpsSignal.AiLatency),
          Option.when(backend.incident.ttsPaused)(OpsSignal.Cost),
          Option.when(backend.incident.liveCoachingPaused)(OpsSignal.CoachingDelivery),
          Option.when(backend.incident.tokenAdsPaused)(OpsSignal.TokenGrants),
          Option.when(backend.campaign.paidAcquisitionPaused)(OpsSignal.CampaignSourceVariant)
        ).flatten

      OpsHealthSnapshot(
        OpsSignal.values.toList.map: signal =>
          OpsMetric(
            signal = signal,
            value =
              if RuntimeMonitoring.requiredOpsSignals.contains(signal) then 1
              else 0,
            unit = defaultUnit(signal),
            healthy = !pausedSignals.contains(signal)
          )
      )

    private def defaultVersions(backend: BackendSettings): GameOperationalVersions =
      GameOperationalVersions(
        gameId = "ops-dashboard-sample",
        policyVersion = "evenchess-policy-v1",
        modelVersion = backend.openAi.model,
        configVersion = "evenchess-backend-settings-v1",
        engineVersion = backend.stockfish.profile,
        featureFlagVersions = Map(
          "overlays" -> backend.features.overlays.toString,
          "coachingCards" -> backend.features.coachingCards.toString,
          "offsetCount" -> backend.features.offsetCount.toString
        )
      )

    private def defaultPaidLaunchReadiness: PaidLaunchReadiness =
      PaidLaunchReadiness(
        PaidLaunchCheck.values.map(check =>
          check -> LaunchCheckStatus(verified = false, explicitlyUnavailableDecision = Some("Not live in local Phase K dashboard."))
        ).toMap
      )

    private def defaultIncidents: List[IncidentRecord] = List(
      IncidentRecord(
        incidentId = "incident-engine-sample",
        incidentType = IncidentType.EngineOutage,
        status = IncidentStatus.Mitigating,
        actionsTaken = IncidentPlaybooks.byType(IncidentType.EngineOutage).requiredResponses,
        auditId = "audit-engine-1",
        publicNotice = Some(AdminOperations.PauseNotice("Engine help degraded; affected games are under review.", public = true, manipulatesHiddenQueueOrFairness = false))
      ),
      IncidentRecord(
        incidentId = "incident-token-sample",
        incidentType = IncidentType.TokenBillingIssue,
        status = IncidentStatus.Triaged,
        actionsTaken = IncidentPlaybooks.byType(IncidentType.TokenBillingIssue).requiredResponses,
        auditId = "audit-token-1",
        publicNotice = None
      )
    )

    private def defaultFeatureFlags: List[FeatureFlag] = List(
      FeatureFlag("evenchess.live.overlays", risky = true, enabled = true, configVersion = "flags-v1", rollbackVersion = Some("flags-v0"), owner = "evenchess-ops", auditId = "audit-flags-overlays"),
      FeatureFlag("evenchess.ai.learningSurfaces", risky = true, enabled = true, configVersion = "flags-v1", rollbackVersion = Some("flags-v0"), owner = "evenchess-ops", auditId = "audit-flags-ai"),
      FeatureFlag("evenchess.tts.coach", risky = true, enabled = false, configVersion = "flags-v1", rollbackVersion = Some("flags-v0"), owner = "evenchess-ops", auditId = "audit-flags-tts")
    )

  private def signalLabel(signal: OpsSignal): String =
    signal.toString.replaceAll("([a-z])([A-Z])", "$1 $2")

  private def paidLaunchLabel(check: PaidLaunchCheck): String =
    check.toString.replaceAll("([a-z])([A-Z])", "$1 $2")

  private def defaultUnit(signal: OpsSignal): String =
    signal match
      case OpsSignal.EngineLatency | OpsSignal.AiLatency | OpsSignal.QueueTime => "ms"
      case OpsSignal.Cost                                                      => "cents"
      case OpsSignal.FailedMatchRate | OpsSignal.EcrResiduals                  => "rate"
      case _                                                                   => "count"
