package lila.evenchess

import MonetisationPolicy.FairnessSnapshot
import ProductInvariants.RequirementClass

object AdminOperations:

  enum AdminOpsRequirement:
    case MonitorRuntimeHealth
    case OutageFairnessRemedies
    case RollbackableRiskyFeatures
    case GameVersionVisibility
    case FairnessChangesThroughVersionedPolicy
    case LaunchHealthControls
    case MinimumDashboards
    case PaidLaunchReadiness
    case IncidentResponsePlaybooks
    case ExistingLichessAdminPatterns

  final case class AdminOpsRequirementClassification(
      requirement: AdminOpsRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object AdminOpsRequirementClassifications:
    val all: List[AdminOpsRequirementClassification] = List(
      AdminOpsRequirementClassification(
        AdminOpsRequirement.MonitorRuntimeHealth,
        RequirementClass.EvenChessSpecific,
        "EvenChess ops must monitor engine/AI latency, stale coaching, fallback, queue, cost, funnel, and fairness signals."
      ),
      AdminOpsRequirementClassification(
        AdminOpsRequirement.OutageFairnessRemedies,
        RequirementClass.EvenChessSpecific,
        "Asymmetric assistance outages require audited pause, downgrade, no-rate, or annul remedies."
      ),
      AdminOpsRequirementClassification(
        AdminOpsRequirement.RollbackableRiskyFeatures,
        RequirementClass.EvenChessSpecific,
        "Risky coaching levels and features require feature flags with rollback versions."
      ),
      AdminOpsRequirementClassification(
        AdminOpsRequirement.GameVersionVisibility,
        RequirementClass.EvenChessSpecific,
        "Operators must identify active policy, model, config, engine, and feature-flag versions for a game."
      ),
      AdminOpsRequirementClassification(
        AdminOpsRequirement.FairnessChangesThroughVersionedPolicy,
        RequirementClass.EvenChessSpecific,
        "Admin controls cannot silently alter hidden rated fairness outside a versioned policy path."
      ),
      AdminOpsRequirementClassification(
        AdminOpsRequirement.LaunchHealthControls,
        RequirementClass.EvenChessSpecific,
        "Launch controls can pause promotions, campaigns, tracking destinations, play windows, and queue-facing campaigns when health degrades."
      ),
      AdminOpsRequirementClassification(
        AdminOpsRequirement.MinimumDashboards,
        RequirementClass.EvenChessSpecific,
        "Minimum EvenChess dashboards cover runtime health, coaching delivery, assistance accounting, ECR, queue, tokens, funnel, purchases, abuse, flags, and versions."
      ),
      AdminOpsRequirementClassification(
        AdminOpsRequirement.PaidLaunchReadiness,
        RequirementClass.EvenChessSpecific,
        "Serious paid launch needs explicit verification or unavailability decisions for acquisition, token, purchase, cancellation, and tracking flows."
      ),
      AdminOpsRequirementClassification(
        AdminOpsRequirement.IncidentResponsePlaybooks,
        RequirementClass.EvenChessSpecific,
        "Incident types require predefined responses for fairness, billing, marketing, queue, and privacy failures."
      ),
      AdminOpsRequirementClassification(
        AdminOpsRequirement.ExistingLichessAdminPatterns,
        RequirementClass.AdaptedToLichessFork,
        "Future routes and permissions should extend existing lila admin/ops patterns instead of creating a parallel admin platform."
      )
    )

  enum OpsSignal:
    case StockfishQueue
    case EngineLatency
    case AiLatency
    case Fallback
    case StaleEvents
    case Cost
    case CoachingDelivery
    case CoachingSuppressed
    case AssistanceLoad
    case UsedOffset
    case EcrResiduals
    case QueueTime
    case FailedMatchRate
    case TokenGrants
    case TokenConsumption
    case Summaries
    case Purchases
    case Cancellations
    case AbuseCases
    case FeatureFlags
    case ActivePolicyVersions
    case Signups
    case FirstGames
    case ThreeGameProgression
    case TenGameProgression
    case Ads
    case CampaignSourceVariant

  final case class OpsMetric(
      signal: OpsSignal,
      value: Double,
      unit: String,
      healthy: Boolean
  ):
    def valid: Boolean = unit.nonEmpty && value >= 0

  final case class OpsHealthSnapshot(metrics: List[OpsMetric]):
    def monitors(signals: Set[OpsSignal]): Boolean =
      signals.subsetOf(metrics.map(_.signal).toSet)

    def degradedSignals: Set[OpsSignal] =
      metrics.collect { case metric if !metric.healthy => metric.signal }.toSet

  object RuntimeMonitoring:
    val requiredOpsSignals: Set[OpsSignal] = Set(
      OpsSignal.StockfishQueue,
      OpsSignal.EngineLatency,
      OpsSignal.AiLatency,
      OpsSignal.Fallback,
      OpsSignal.StaleEvents,
      OpsSignal.Cost
    )

  enum Dashboard:
    case EngineAiHealth
    case CoachingDelivery
    case AssistanceAccounting
    case RatingCalibration
    case QueueHealth
    case TokensAndSummaries
    case FunnelPurchases
    case AbuseCases
    case FeatureFlags
    case ActiveVersions

  final case class DashboardDefinition(
      dashboard: Dashboard,
      sources: Set[OpsSignal],
      sourceDescription: String
  ):
    def complete: Boolean = sources.nonEmpty && sourceDescription.nonEmpty

  object DashboardRegistry:
    val definitions: List[DashboardDefinition] = List(
      DashboardDefinition(
        Dashboard.EngineAiHealth,
        Set(OpsSignal.StockfishQueue, OpsSignal.EngineLatency, OpsSignal.AiLatency, OpsSignal.Fallback, OpsSignal.Cost),
        "Stockfish queue, engine latency, AI latency, fallback, and cost."
      ),
      DashboardDefinition(
        Dashboard.CoachingDelivery,
        Set(OpsSignal.CoachingDelivery, OpsSignal.CoachingSuppressed, OpsSignal.StaleEvents),
        "Coaching delivery, stale, and suppressed events."
      ),
      DashboardDefinition(
        Dashboard.AssistanceAccounting,
        Set(OpsSignal.AssistanceLoad, OpsSignal.UsedOffset),
        "Assistance Load and Used Offset."
      ),
      DashboardDefinition(
        Dashboard.RatingCalibration,
        Set(OpsSignal.EcrResiduals),
        "ECR residuals and calibration safety."
      ),
      DashboardDefinition(
        Dashboard.QueueHealth,
        Set(OpsSignal.QueueTime, OpsSignal.FailedMatchRate),
        "Queue time and failed match rate."
      ),
      DashboardDefinition(
        Dashboard.TokensAndSummaries,
        Set(OpsSignal.TokenGrants, OpsSignal.TokenConsumption, OpsSignal.Summaries, OpsSignal.Ads),
        "Token grants, consumption, rewarded ads, and summaries."
      ),
      DashboardDefinition(
        Dashboard.FunnelPurchases,
        Set(
          OpsSignal.Signups,
          OpsSignal.FirstGames,
          OpsSignal.ThreeGameProgression,
          OpsSignal.TenGameProgression,
          OpsSignal.CampaignSourceVariant,
          OpsSignal.Purchases,
          OpsSignal.Cancellations
        ),
        "Signup, first game, progression, campaign, purchase, and cancellation funnel."
      ),
      DashboardDefinition(Dashboard.AbuseCases, Set(OpsSignal.AbuseCases), "Abuse cases and trust review load."),
      DashboardDefinition(Dashboard.FeatureFlags, Set(OpsSignal.FeatureFlags), "Feature flags and rollback readiness."),
      DashboardDefinition(Dashboard.ActiveVersions, Set(OpsSignal.ActivePolicyVersions), "Active policy, model, and config versions.")
    )

    val requiredDashboards: Set[Dashboard] = Dashboard.values.toSet

    def coversMinimumDashboards(dashboards: List[DashboardDefinition]): Boolean =
      requiredDashboards.subsetOf(dashboards.map(_.dashboard).toSet) && dashboards.forall(_.complete)

    def coversMinimumSources(dashboards: List[DashboardDefinition]): Boolean =
      val sources = dashboards.flatMap(_.sources).toSet
      OpsSignal.values.toSet.subsetOf(sources)

  enum OpsAction:
    case Continue
    case Suppress
    case Downgrade
    case Pause
    case NoRate
    case Annul
    case Fallback
    case ClearStale
    case InvestigateTtlHash
    case StopRatingFlow
    case ReplayRatings
    case CorrectByAudit
    case RefundOrRestore
    case DisableBadPath
    case KillVariant
    case CorrectConfig
    case PauseCampaigns
    case ShowWindowOrWaitlist
    case StopCapture
    case StopExport
    case ReviewRetention

  enum IncidentType:
    case EngineOutage
    case AiOutage
    case StaleCoaching
    case RatingCorruption
    case TokenBillingIssue
    case MarketingCopyIssue
    case QueueHealthIssue
    case DataPrivacyIssue

  enum IncidentStatus:
    case Detected
    case Triaged
    case Mitigating
    case Resolved
    case Reviewed

  final case class IncidentPlaybook(
      incidentType: IncidentType,
      examples: Set[String],
      requiredResponses: Set[OpsAction]
  ):
    def complete: Boolean = examples.nonEmpty && requiredResponses.nonEmpty

  object IncidentPlaybooks:
    val all: List[IncidentPlaybook] = List(
      IncidentPlaybook(
        IncidentType.EngineOutage,
        Set("high latency", "missing candidates", "asymmetric help"),
        Set(OpsAction.Suppress, OpsAction.Downgrade, OpsAction.NoRate, OpsAction.Annul)
      ),
      IncidentPlaybook(
        IncidentType.AiOutage,
        Set("timeout", "invalid output", "cost spike"),
        Set(OpsAction.Fallback, OpsAction.Suppress)
      ),
      IncidentPlaybook(
        IncidentType.StaleCoaching,
        Set("advice for old state"),
        Set(OpsAction.ClearStale, OpsAction.InvestigateTtlHash)
      ),
      IncidentPlaybook(
        IncidentType.RatingCorruption,
        Set("wrong offset", "wrong model"),
        Set(OpsAction.StopRatingFlow, OpsAction.ReplayRatings, OpsAction.CorrectByAudit)
      ),
      IncidentPlaybook(
        IncidentType.TokenBillingIssue,
        Set("wrong consumption", "wrong subscription state"),
        Set(OpsAction.RefundOrRestore, OpsAction.DisableBadPath)
      ),
      IncidentPlaybook(
        IncidentType.MarketingCopyIssue,
        Set("cheating implication", "pay-to-win implication"),
        Set(OpsAction.KillVariant, OpsAction.CorrectConfig)
      ),
      IncidentPlaybook(
        IncidentType.QueueHealthIssue,
        Set("paid traffic causes waits"),
        Set(OpsAction.PauseCampaigns, OpsAction.ShowWindowOrWaitlist)
      ),
      IncidentPlaybook(
        IncidentType.DataPrivacyIssue,
        Set("excessive capture", "excessive export"),
        Set(OpsAction.StopCapture, OpsAction.StopExport, OpsAction.ReviewRetention)
      )
    )

    val byType: Map[IncidentType, IncidentPlaybook] =
      all.map(playbook => playbook.incidentType -> playbook).toMap

    def coversAllKnownIncidents: Boolean =
      IncidentType.values.toSet.subsetOf(byType.keySet) && all.forall(_.complete)

  final case class AssistanceOutage(
      engineHealthyForBothPlayers: Boolean,
      aiHealthy: Boolean,
      staleCoachingDetected: Boolean,
      asymmetricAssistance: Boolean,
      fairnessAffected: Boolean
  )

  object OutageRemedyPolicy:
    val fairnessRemedies: Set[OpsAction] =
      Set(OpsAction.Pause, OpsAction.Downgrade, OpsAction.NoRate, OpsAction.Annul)

    def requiredRemedies(outage: AssistanceOutage): Set[OpsAction] =
      if outage.fairnessAffected && outage.asymmetricAssistance then fairnessRemedies
      else if outage.staleCoachingDetected then Set(OpsAction.ClearStale, OpsAction.Suppress)
      else if !outage.aiHealthy then Set(OpsAction.Fallback, OpsAction.Suppress)
      else Set(OpsAction.Continue)

  final case class FeatureFlag(
      key: String,
      risky: Boolean,
      enabled: Boolean,
      configVersion: String,
      rollbackVersion: Option[String],
      owner: String,
      auditId: String
  ):
    def hasRequiredAdminMetadata: Boolean =
      key.nonEmpty && configVersion.nonEmpty && owner.nonEmpty && auditId.nonEmpty

    def rollbackable: Boolean =
      !risky || rollbackVersion.exists(_.nonEmpty)

    def safeToEnable: Boolean =
      !enabled || (hasRequiredAdminMetadata && rollbackable)

  final case class GameOperationalVersions(
      gameId: String,
      policyVersion: String,
      modelVersion: String,
      configVersion: String,
      engineVersion: String,
      featureFlagVersions: Map[String, String]
  ):
    def visibleToOperators: Boolean =
      gameId.nonEmpty &&
        policyVersion.nonEmpty &&
        modelVersion.nonEmpty &&
        configVersion.nonEmpty &&
        engineVersion.nonEmpty &&
        featureFlagVersions.nonEmpty &&
        featureFlagVersions.values.forall(_.nonEmpty)

  final case class VersionedPolicyPath(
      policyVersion: String,
      configVersion: String,
      auditId: String,
      operatorId: String,
      visibleReason: String
  ):
    def complete: Boolean =
      policyVersion.nonEmpty &&
        configVersion.nonEmpty &&
        auditId.nonEmpty &&
        operatorId.nonEmpty &&
        visibleReason.nonEmpty

  final case class AdminFairnessChange(
      before: FairnessSnapshot,
      after: FairnessSnapshot,
      path: Option[VersionedPolicyPath]
  ):
    def changesFairness: Boolean = before != after

    def allowed: Boolean =
      !changesFairness || path.exists(_.complete)

  enum LaunchSurface:
    case RewardedAds
    case StandardPromotion
    case PremiumPromotion
    case CampaignVariants
    case TrackingDestinations
    case PlayWindows
    case QueueFacingCampaigns

  final case class LaunchHealth(
      rewardedAdsHealthy: Boolean,
      standardPromotionHealthy: Boolean,
      premiumPromotionHealthy: Boolean,
      campaignVariantsHealthy: Boolean,
      trackingDestinationsHealthy: Boolean,
      playWindowsHealthy: Boolean,
      queueFacingCampaignsHealthy: Boolean
  )

  object LaunchHealthPolicy:
    def surfacesToPause(health: LaunchHealth): Set[LaunchSurface] =
      Set(
        Option.when(!health.rewardedAdsHealthy)(LaunchSurface.RewardedAds),
        Option.when(!health.standardPromotionHealthy)(LaunchSurface.StandardPromotion),
        Option.when(!health.premiumPromotionHealthy)(LaunchSurface.PremiumPromotion),
        Option.when(!health.campaignVariantsHealthy)(LaunchSurface.CampaignVariants),
        Option.when(!health.trackingDestinationsHealthy)(LaunchSurface.TrackingDestinations),
        Option.when(!health.playWindowsHealthy)(LaunchSurface.PlayWindows),
        Option.when(!health.queueFacingCampaignsHealthy)(LaunchSurface.QueueFacingCampaigns)
      ).flatten

  final case class PauseNotice(
      message: String,
      public: Boolean,
      manipulatesHiddenQueueOrFairness: Boolean
  ):
    def valid: Boolean =
      message.nonEmpty && public && !manipulatesHiddenQueueOrFairness

  enum PaidLaunchCheck:
    case Signup
    case FirstGame
    case TenGameMilestone
    case AdTokenFlow
    case StandardPurchase
    case PremiumPurchase
    case Cancellation
    case Ga4
    case GoogleAds
    case MetaPixelCapi
    case Dedupe

  final case class LaunchCheckStatus(
      verified: Boolean,
      explicitlyUnavailableDecision: Option[String]
  ):
    def acceptable: Boolean =
      verified || explicitlyUnavailableDecision.exists(_.nonEmpty)

  final case class PaidLaunchReadiness(statuses: Map[PaidLaunchCheck, LaunchCheckStatus]):
    def seriousPaidLaunchAllowed: Boolean =
      PaidLaunchCheck.values.forall(check => statuses.get(check).exists(_.acceptable))

  final case class IncidentRecord(
      incidentId: String,
      incidentType: IncidentType,
      status: IncidentStatus,
      actionsTaken: Set[OpsAction],
      auditId: String,
      publicNotice: Option[PauseNotice]
  ):
    def hasAuditTrail: Boolean = incidentId.nonEmpty && auditId.nonEmpty

    def requiredResponsesCovered: Boolean =
      IncidentPlaybooks.byType.get(incidentType).exists(playbook => playbook.requiredResponses.subsetOf(actionsTaken))

    def valid: Boolean =
      hasAuditTrail &&
        requiredResponsesCovered &&
        publicNotice.forall(_.valid)
