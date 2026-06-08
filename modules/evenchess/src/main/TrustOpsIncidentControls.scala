package lila.evenchess

import AbuseTrustControls.{
  AbuseControlDecision,
  CandidateProbePattern,
  ExploitRegister,
  ExploitRegisterRow,
  ExploitType,
  FairPlayReportContext,
  FairnessValues,
  GuidanceContext,
  InvasiveControlRequest,
  MitigationAction,
  ModerationDisclosurePolicy,
  MvpTrustControls,
  ProbeControlDecision,
  RuntimeFairnessHealth,
  RuntimeTrustPolicy,
  RuntimeTrustRemedy
}
import AdminOperations.{
  DashboardRegistry,
  FeatureFlag,
  GameOperationalVersions,
  IncidentPlaybooks,
  IncidentRecord,
  IncidentStatus,
  IncidentType,
  OpsAction,
  OpsHealthSnapshot,
  PauseNotice,
  RuntimeMonitoring
}
import DataModelsAndSeams.{ IntegrationSeam, IntegrationSeamRegistry }
import ProductInvariants.RequirementClass

object TrustOpsIncidentControls:

  enum PhaseKRequirement:
    case LichessModerationAndAdminFoundation
    case NonPlatformGuidanceEscalation
    case ExploitRegisterRuntimeMapping
    case AuditedFairnessRemedies
    case CandidateBudgetsAndProbeSuppression
    case IncidentRecordsAndPlaybooks
    case OpsDashboardsAndVersionVisibility
    case RollbackableRiskyFeatures
    case MvpInvasiveControlsExcluded
    case AppendixVTrustOpsAcceptance
    case AdminOpsSeam

  final case class PhaseKRequirementClassification(
      requirement: PhaseKRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseKRequirementClassifications:
    val all: List[PhaseKRequirementClassification] = List(
      PhaseKRequirementClassification(
        PhaseKRequirement.LichessModerationAndAdminFoundation,
        RequirementClass.LichessProvided,
        "Use existing lila moderation/admin foundations where adapters are needed; do not build a parallel platform."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.NonPlatformGuidanceEscalation,
        RequirementClass.EvenChessSpecific,
        "External help remains prohibited and is escalated with EvenChess game/player/ledger context."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.ExploitRegisterRuntimeMapping,
        RequirementClass.EvenChessSpecific,
        "Map Appendix Q exploit signals to mitigations, review paths, and incident types."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.AuditedFairnessRemedies,
        RequirementClass.EvenChessSpecific,
        "No-rate, annul, downgrade, and pause remedies need explicit audited action state."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.CandidateBudgetsAndProbeSuppression,
        RequirementClass.EvenChessSpecific,
        "High-volume hover/probe/reveal patterns can be capped only with audited suppression."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.IncidentRecordsAndPlaybooks,
        RequirementClass.EvenChessSpecific,
        "Known incidents require playbook coverage, audit trails, and valid public notices when shown."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.OpsDashboardsAndVersionVisibility,
        RequirementClass.EvenChessSpecific,
        "Operations must expose health, dashboards, and active policy/model/config/engine versions."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.RollbackableRiskyFeatures,
        RequirementClass.EvenChessSpecific,
        "Risky features need flags, owners, audit IDs, and rollback versions."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.MvpInvasiveControlsExcluded,
        RequirementClass.UnresolvedProductOwnerDecision,
        "Phone/device/IP/cluster controls stay off unless product-owner and privacy approvals exist."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.AppendixVTrustOpsAcceptance,
        RequirementClass.EvenChessSpecific,
        "Appendix V trust/ops acceptance gates must be explicit before production release."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.AdminOpsSeam,
        RequirementClass.AdaptedToLichessFork,
        "Future lila admin/moderation adapters should call this narrow EvenChess AdminOps seam."
      )
    )

  final case class TrustSignal(
      signalId: String,
      exploit: ExploitType,
      gameId: String,
      playerId: String,
      moveRange: String,
      evidence: String,
      guidance: Option[GuidanceContext],
      probePattern: Option[CandidateProbePattern],
      runtimeHealth: Option[RuntimeFairnessHealth],
      copyScanHits: List[String],
      auditId: String,
      occurredAt: Long
  ):
    def valid: Boolean =
      signalId.nonEmpty &&
        gameId.nonEmpty &&
        playerId.nonEmpty &&
        evidence.nonEmpty &&
        auditId.nonEmpty &&
        occurredAt > 0

    def externalGuidanceProhibited: Boolean =
      guidance.exists(context => !context.legalInRatedEvenChess)

    def highVolumeProbe(maxEvents: Int, minWindowSeconds: Int): Boolean =
      probePattern.exists(_.highVolume(maxEvents, minWindowSeconds))

    def runtimeRemedy: Option[RuntimeTrustRemedy] =
      runtimeHealth.map(RuntimeTrustPolicy.remedy)

  final case class FairPlayEscalation(
      report: FairPlayReportContext,
      moderationPolicy: ModerationDisclosurePolicy,
      guidance: GuidanceContext
  ):
    def valid: Boolean =
      report.complete &&
        moderationPolicy.safe &&
        !guidance.legalInRatedEvenChess

  object FairPlayEscalationService:
    def fromSignal(signal: TrustSignal, ledgerSummaryId: String, visibleAssistanceState: String): Option[FairPlayEscalation] =
      signal.guidance.filter(context => !context.legalInRatedEvenChess).map: context =>
        FairPlayEscalation(
          report = FairPlayReportContext(
            reportId = s"fairplay-${signal.signalId}",
            gameId = signal.gameId,
            playerId = signal.playerId,
            moveRange = signal.moveRange,
            coachingLedgerSummaryId = ledgerSummaryId,
            visibleAssistanceState = visibleAssistanceState
          ),
          moderationPolicy = ModerationDisclosurePolicy(
            exposesAntiCheatInternals = false,
            usesExistingLilaModerationPatterns = true,
            includesEvenChessLedgerContext = true
          ),
          guidance = context
        )

  object ExploitIncidentMapping:
    def incidentType(exploit: ExploitType): Option[IncidentType] =
      exploit match
        case ExploitType.EngineOutage             => Some(IncidentType.EngineOutage)
        case ExploitType.AiTimeout                => Some(IncidentType.AiOutage)
        case ExploitType.PromptInjection          => Some(IncidentType.AiOutage)
        case ExploitType.StaleCoaching            => Some(IncidentType.StaleCoaching)
        case ExploitType.Desync                   => Some(IncidentType.StaleCoaching)
        case ExploitType.SubscriptionAbuse        => Some(IncidentType.TokenBillingIssue)
        case ExploitType.AbortAbuse               => Some(IncidentType.TokenBillingIssue)
        case ExploitType.AdTokenFarming           => Some(IncidentType.TokenBillingIssue)
        case ExploitType.TargetAbuse              => Some(IncidentType.RatingCorruption)
        case ExploitType.PremiumMisrepresentation => Some(IncidentType.MarketingCopyIssue)
        case _                                    => None

    def incidentActions(exploit: ExploitType): Set[OpsAction] =
      incidentType(exploit)
        .flatMap(IncidentPlaybooks.byType.get)
        .map(_.requiredResponses)
        .getOrElse(Set.empty)

    def publicNotice(exploit: ExploitType): Option[PauseNotice] =
      incidentType(exploit).collect:
        case IncidentType.EngineOutage =>
          PauseNotice("Engine help is degraded; affected games are being reviewed.", public = true, manipulatesHiddenQueueOrFairness = false)
        case IncidentType.AiOutage =>
          PauseNotice("AI wording is degraded; fallback coaching remains bounded by policy.", public = true, manipulatesHiddenQueueOrFairness = false)
        case IncidentType.QueueHealthIssue =>
          PauseNotice("Paid traffic is paused while queue health recovers.", public = true, manipulatesHiddenQueueOrFairness = false)
        case IncidentType.MarketingCopyIssue =>
          PauseNotice("A campaign variant is paused while copy is corrected.", public = true, manipulatesHiddenQueueOrFairness = false)
        case IncidentType.DataPrivacyIssue =>
          PauseNotice("Data capture is paused while retention is reviewed.", public = true, manipulatesHiddenQueueOrFairness = false)

  final case class TrustOpsDecision(
      signal: TrustSignal,
      exploitRow: ExploitRegisterRow,
      mitigations: Set[MitigationAction],
      incidentType: Option[IncidentType],
      incidentActions: Set[OpsAction],
      fairPlayEscalation: Option[FairPlayEscalation],
      probeControl: Option[ProbeControlDecision],
      runtimeRemedy: Option[RuntimeTrustRemedy],
      abuseDecision: AbuseControlDecision,
      incidentRecord: Option[IncidentRecord]
  ):
    def audited: Boolean = signal.auditId.nonEmpty

    def valid: Boolean =
      signal.valid &&
        exploitRow.complete &&
        mitigations.nonEmpty &&
        fairPlayEscalation.forall(_.valid) &&
        probeControl.forall(_.valid) &&
        abuseDecision.allowed &&
        incidentRecord.forall(_.valid)

    def fairnessRemedyAudited: Boolean =
      !abuseDecision.fairnessChanged || abuseDecision.allowed

  object TrustOpsPolicy:
    val maxProbeEvents = 50
    val minProbeWindowSeconds = 30

    def decide(
        signal: TrustSignal,
        beforeFairness: FairnessValues,
        afterFairness: FairnessValues,
        visibleReason: String,
        ledgerSummaryId: String,
        visibleAssistanceState: String
    ): TrustOpsDecision =
      val row = ExploitRegister.byExploit(signal.exploit)
      val incident = ExploitIncidentMapping.incidentType(signal.exploit)
      val incidentActions = ExploitIncidentMapping.incidentActions(signal.exploit)
      val fairPlay = FairPlayEscalationService.fromSignal(signal, ledgerSummaryId, visibleAssistanceState)
      val probe = signal.probePattern.map: pattern =>
        val high = pattern.highVolume(maxProbeEvents, minProbeWindowSeconds)
        ProbeControlDecision(capped = high, cooldown = high, suppressionAudited = high, auditId = signal.auditId)
      val mitigationAction = fairnessMitigation(row.mitigations)
      val abuse = AbuseControlDecision(
        action = mitigationAction,
        before = beforeFairness,
        after = afterFairness,
        auditId = signal.auditId,
        visibleReason = visibleReason
      )
      val incidentRecord = incident.map: incidentType =>
        IncidentRecord(
          incidentId = s"incident-${signal.signalId}",
          incidentType = incidentType,
          status = IncidentStatus.Mitigating,
          actionsTaken = incidentActions,
          auditId = signal.auditId,
          publicNotice = ExploitIncidentMapping.publicNotice(signal.exploit)
        )

      TrustOpsDecision(
        signal = signal,
        exploitRow = row,
        mitigations = row.mitigations,
        incidentType = incident,
        incidentActions = incidentActions,
        fairPlayEscalation = fairPlay,
        probeControl = probe,
        runtimeRemedy = signal.runtimeRemedy,
        abuseDecision = abuse,
        incidentRecord = incidentRecord
      )

    private def fairnessMitigation(mitigations: Set[MitigationAction]): MitigationAction =
      List(MitigationAction.NoRate, MitigationAction.Annul, MitigationAction.Downgrade, MitigationAction.Pause, MitigationAction.Review)
        .find(mitigations.contains)
        .getOrElse(mitigations.head)

  final case class OpsReadinessEvidence(
      healthSnapshot: OpsHealthSnapshot,
      dashboardsComplete: Boolean,
      activeVersions: GameOperationalVersions,
      featureFlags: List[FeatureFlag],
      incidentPlaybooksComplete: Boolean
  ):
    def accepted: Boolean =
      healthSnapshot.monitors(RuntimeMonitoring.requiredOpsSignals) &&
        DashboardRegistry.coversMinimumDashboards(DashboardRegistry.definitions) &&
        DashboardRegistry.coversMinimumSources(DashboardRegistry.definitions) &&
        dashboardsComplete &&
        activeVersions.visibleToOperators &&
        featureFlags.forall(_.safeToEnable) &&
        incidentPlaybooksComplete

  final case class TrustOpsAcceptanceEvidence(
      exploitRegisterComplete: Boolean,
      nonPlatformGuidanceEscalates: Boolean,
      copyScansPass: Boolean,
      repeatCollusionSimulationsPass: Boolean,
      candidateBudgetsAuditPass: Boolean,
      promptInjectionBlocked: Boolean,
      staleAndDesyncHandled: Boolean,
      tokenAbuseHandled: Boolean,
      noRateAnnulAuditPass: Boolean,
      mobileDisclosurePass: Boolean,
      rollbackWorks: Boolean,
      incidentPlaybooksExist: Boolean,
      patchMapCurrent: Boolean,
      opsReadiness: OpsReadinessEvidence
  ):
    def accepted: Boolean =
      exploitRegisterComplete &&
        nonPlatformGuidanceEscalates &&
        copyScansPass &&
        repeatCollusionSimulationsPass &&
        candidateBudgetsAuditPass &&
        promptInjectionBlocked &&
        staleAndDesyncHandled &&
        tokenAbuseHandled &&
        noRateAnnulAuditPass &&
        mobileDisclosurePass &&
        rollbackWorks &&
        incidentPlaybooksExist &&
        patchMapCurrent &&
        opsReadiness.accepted

  object MvpInvasiveControlPolicy:
    val phoneVerificationEnabled = MvpTrustControls.phoneVerificationRequired
    val deviceSessionRiskScoringEnabled = MvpTrustControls.deviceSessionRiskScoringRequired
    val sameIpCreationLimitsEnabled = MvpTrustControls.sameIpCreationLimitsRequired
    val highRiskClusterTokenDelaysEnabled = MvpTrustControls.highRiskClusterTokenDelaysRequired

    def mayEnable(request: InvasiveControlRequest): Boolean =
      request.mayEnable

    def allMvpInvasiveControlsDisabled: Boolean =
      !phoneVerificationEnabled &&
        !deviceSessionRiskScoringEnabled &&
        !sameIpCreationLimitsEnabled &&
        !highRiskClusterTokenDelaysEnabled

  object AdminOpsSeam:
    val seam: IntegrationSeam = IntegrationSeam.AdminOps
    val patchMapRequiredBeforeLilaAdapter = true
    val patchMapEntryRequiredNow = false

    def registered: Boolean =
      IntegrationSeamRegistry.all.exists(rule =>
        rule.seam == seam &&
          rule.adaptedToLila &&
          rule.serverAuthoritative &&
          !rule.fairnessAffecting
      )
