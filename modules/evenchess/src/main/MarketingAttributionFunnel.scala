package lila.evenchess

import AdminOperations.{
  LaunchCheckStatus,
  LaunchHealth,
  LaunchHealthPolicy,
  LaunchSurface,
  PaidLaunchCheck,
  PaidLaunchReadiness,
  PauseNotice
}
import DataModelsAndSeams.{ IntegrationSeam, IntegrationSeamRegistry, MarketingAttributionModel }
import MarketingFunnelPolicy.{
  AdminMarketingControls,
  AttributionEvent,
  AttributionEvents,
  LandingSection,
  LandingVariant,
  LandingVariants,
  MarketingConfig,
  MarketingFairnessBoundary,
  PaidAcquisitionHealth
}
import MonetisationPolicy.{ FairnessSnapshot, PlanTier }
import ProductInvariants.RequirementClass
import TelemetryAnalytics.{
  AccountType,
  ConversionDedupe,
  ConversionEvent,
  FunnelAttribution,
  LaunchDashboardGroup,
  PrivacyScan,
  QueueHealth,
  SummarySource,
  TokenSource
}

object MarketingAttributionFunnel:

  enum PhaseJRequirement:
    case LichessPublicPageFoundation
    case BackendReadableMarketingConfig
    case LandingVariantsAndCopySafety
    case CampaignAttributionCapture
    case StableConversionEvents
    case LaunchDashboardGrouping
    case LaunchControlsAndPauseNotices
    case PaidLaunchReadinessGate
    case MarketingFunnelSeam
    case FairnessBoundary
    case PrivacyAndNoInvasiveRiskScoring

  final case class PhaseJRequirementClassification(
      requirement: PhaseJRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseJRequirementClassifications:
    val all: List[PhaseJRequirementClassification] = List(
      PhaseJRequirementClassification(
        PhaseJRequirement.LichessPublicPageFoundation,
        RequirementClass.LichessProvided,
        "Use the existing lila public-page/routing foundation; do not rebuild a second site platform."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.BackendReadableMarketingConfig,
        RequirementClass.EvenChessSpecific,
        "Serve EvenChess marketing copy from versioned backend-readable config with safe fallback."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.LandingVariantsAndCopySafety,
        RequirementClass.EvenChessSpecific,
        "Variants may change copy order/emphasis only and unsafe copy falls back or pauses."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.CampaignAttributionCapture,
        RequirementClass.EvenChessSpecific,
        "Capture UTM, click ID, variant, first/latest touch, signup, first-game, and subscription linkage."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.StableConversionEvents,
        RequirementClass.EvenChessSpecific,
        "Emit stable conversion event names with timestamps, dedupe IDs, pseudonymous IDs, value, and plan."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.LaunchDashboardGrouping,
        RequirementClass.AdaptedToLichessFork,
        "Group launch dashboards through lila-compatible ops/analytics seams."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.LaunchControlsAndPauseNotices,
        RequirementClass.EvenChessSpecific,
        "Pause ads, promotions, variants, tracking, play windows, and queue-facing campaigns when health degrades."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.PaidLaunchReadinessGate,
        RequirementClass.EvenChessSpecific,
        "Block serious paid launch unless required conversion/tracking checks are verified or explicitly unavailable."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.MarketingFunnelSeam,
        RequirementClass.AdaptedToLichessFork,
        "Use a narrow MarketingFunnel seam for future lila routes, admin controls, and analytics adapters."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.FairnessBoundary,
        RequirementClass.EvenChessSpecific,
        "Marketing, attribution, launch windows, and campaign controls cannot mutate rated fairness fields."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.PrivacyAndNoInvasiveRiskScoring,
        RequirementClass.EvenChessSpecific,
        "Use pseudonymous attribution and MVP dashboard grouping without invasive risk scoring or raw email storage."
      )
    )

  enum FunnelDecision:
    case ServeConfigured
    case ServeSafeFallback
    case Paused

  final case class FunnelRenderDecision(
      decision: FunnelDecision,
      config: MarketingConfig,
      activeVariant: LandingVariant,
      sectionOrder: List[LandingSection],
      pausedSurfaces: Set[LaunchSurface],
      notice: Option[PauseNotice],
      paidAcquisitionPaused: Boolean,
      trackingDestinationsEnabled: Boolean,
      reasons: List[String]
  ):
    def valid: Boolean =
      config.safeForUse &&
        sectionOrder.nonEmpty &&
        notice.forall(_.valid) &&
        !MarketingFairnessBoundary.marketingMayBypassFairness &&
        !MarketingFairnessBoundary.campaignMayAlterCoachingPermission &&
        !MarketingFairnessBoundary.campaignMayAlterStockfishExposure &&
        !MarketingFairnessBoundary.paidPlansMayAlterLiveStrength

    def servingConfiguredCopy: Boolean =
      decision == FunnelDecision.ServeConfigured

  object FunnelConfigService:
    def resolve(
        candidate: MarketingConfig,
        controls: AdminMarketingControls,
        acquisitionHealth: PaidAcquisitionHealth,
        launchHealth: LaunchHealth,
        now: Long
    ): FunnelRenderDecision =
      val configSafe = candidate.safeForUse && !candidate.killSwitch
      val siteEnabled = controls.marketingSiteEnabled
      val launchPaused = LaunchHealthPolicy.surfacesToPause(launchHealth)
      val paidPaused =
        controls.paidAcquisitionMode &&
          (acquisitionHealth.shouldPause || controls.campaignPauseNotice.exists(_.nonEmpty))
      val pausedSurfaces =
        launchPaused ++
          Option.when(paidPaused)(LaunchSurface.TrackingDestinations).toSet ++
          Option.when(!configSafe)(LaunchSurface.CampaignVariants).toSet ++
          Option.when(!siteEnabled || candidate.killSwitch)(LaunchSurface.QueueFacingCampaigns).toSet

      val effectiveConfig =
        if siteEnabled && configSafe then candidate.copy(
          landingVariant = controls.activeLandingVariant,
          playWindows = controls.playWindows
        )
        else MarketingConfig.safeFallback(now)

      val variant =
        if siteEnabled && configSafe then controls.activeLandingVariant else LandingVariant.Default
      val sectionOrder =
        LandingVariants.all.find(_.variant == variant).map(_.sectionOrder).getOrElse(MarketingFunnelPolicy.LandingDefaults.requiredSections)
      val reasons =
        List(
          Option.when(!siteEnabled)("marketing_site_disabled"),
          Option.when(candidate.killSwitch)("marketing_kill_switch"),
          Option.when(!candidate.safeForUse)("unsafe_or_incomplete_copy"),
          Option.when(acquisitionHealth.shouldPause)("paid_acquisition_health_paused"),
          Option.when(controls.campaignPauseNotice.exists(_.nonEmpty))("campaign_pause_notice"),
          Option.when(launchPaused.nonEmpty)("launch_surface_health_paused")
        ).flatten
      val decision =
        if !siteEnabled || candidate.killSwitch then FunnelDecision.Paused
        else if !configSafe then FunnelDecision.ServeSafeFallback
        else if pausedSurfaces.nonEmpty then FunnelDecision.Paused
        else FunnelDecision.ServeConfigured
      val notice =
        if pausedSurfaces.nonEmpty || reasons.nonEmpty then
          Some(
            PauseNotice(
              message = controls.campaignPauseNotice.getOrElse("Marketing or paid acquisition is paused while launch health recovers."),
              public = true,
              manipulatesHiddenQueueOrFairness = false
            )
          )
        else None

      FunnelRenderDecision(
        decision = decision,
        config = effectiveConfig,
        activeVariant = variant,
        sectionOrder = sectionOrder,
        pausedSurfaces = pausedSurfaces,
        notice = notice,
        paidAcquisitionPaused = paidPaused,
        trackingDestinationsEnabled = siteEnabled && !paidPaused && !pausedSurfaces.contains(LaunchSurface.TrackingDestinations),
        reasons = reasons
      )

  final case class FunnelCaptureInput(
      attributionId: String,
      pseudonymousAccountId: String,
      utmSource: Option[String],
      utmMedium: Option[String],
      utmCampaign: Option[String],
      utmContent: Option[String],
      utmTerm: Option[String],
      clickId: Option[String],
      variant: LandingVariant,
      configVersion: String,
      firstTouch: Option[String],
      latestTouch: Option[String],
      signupId: Option[String],
      firstGameId: Option[String],
      subscriptionId: Option[String],
      rawEmail: Option[String]
  ):
    def valid: Boolean =
      attributionId.nonEmpty &&
        pseudonymousAccountId.nonEmpty &&
        configVersion.nonEmpty &&
        List(utmSource, utmCampaign, clickId, firstTouch, latestTouch, subscriptionId).exists(_.exists(_.nonEmpty))

  final case class CapturedAttribution(
      storage: MarketingAttributionModel,
      telemetry: FunnelAttribution
  ):
    def valid: Boolean =
      storage.valid &&
        telemetry.hasCampaignIdentity &&
        !storage.storesRawEmail

  object AttributionCapture:
    val schemaVersion = "evenchess-funnel-attribution-v1"

    def capture(input: FunnelCaptureInput): CapturedAttribution =
      CapturedAttribution(
        storage = MarketingAttributionModel(
          attributionId = input.attributionId,
          pseudonymousAccountId = input.pseudonymousAccountId,
          utmSource = input.utmSource,
          utmCampaign = input.utmCampaign,
          clickId = input.clickId,
          variant = FunnelEventCatalog.stableVariant(input.variant),
          configVersion = input.configVersion,
          firstTouch = input.firstTouch,
          latestTouch = input.latestTouch,
          subscriptionId = input.subscriptionId,
          storesRawEmail = false,
          schemaVersion = schemaVersion
        ),
        telemetry = FunnelAttribution(
          utmSource = input.utmSource,
          utmMedium = input.utmMedium,
          utmCampaign = input.utmCampaign,
          utmContent = input.utmContent,
          utmTerm = input.utmTerm,
          clickId = input.clickId,
          variant = input.variant,
          firstTouch = input.firstTouch,
          latestTouch = input.latestTouch,
          signupId = input.signupId,
          firstGameId = input.firstGameId,
          subscriptionId = input.subscriptionId
        )
      )

  object FunnelEventCatalog:
    val requiredEvents: Set[AttributionEvent] = AttributionEvents.required

    def coversAppendixORequiredEvents: Boolean =
      AttributionEvents.includesRequired(requiredEvents) &&
        requiredEvents.map(stableName).forall(_.nonEmpty)

    def stableName(event: AttributionEvent): String =
      event match
        case AttributionEvent.LandingPageView       => "landing_page_view"
        case AttributionEvent.ViewPricing           => "view_pricing"
        case AttributionEvent.BeginSignup           => "begin_signup"
        case AttributionEvent.SignUpComplete        => "sign_up_complete"
        case AttributionEvent.FirstGameStarted      => "first_game_started"
        case AttributionEvent.FirstGameCompleted    => "first_game_completed"
        case AttributionEvent.GamesCompleted3       => "games_completed_3"
        case AttributionEvent.TenGamesCompleted     => "ten_games_completed"
        case AttributionEvent.RewardedAdComplete    => "rewarded_ad_complete"
        case AttributionEvent.MatchSummaryView      => "match_summary_view"
        case AttributionEvent.PerformanceSummaryView => "performance_summary_view"
        case AttributionEvent.CheckoutStart         => "checkout_start"
        case AttributionEvent.Purchase              => "purchase"
        case AttributionEvent.Renew                 => "renew"
        case AttributionEvent.Cancel                => "cancel"

    def stableVariant(variant: LandingVariant): String =
      variant match
        case LandingVariant.Default        => "default"
        case LandingVariant.AdultImprover  => "adult_improver"
        case LandingVariant.ParentLearning => "parent_learning"
        case LandingVariant.FairRating     => "fair_rating"
        case LandingVariant.SummaryLoop    => "summary_loop"
        case LandingVariant.FreeTokens     => "free_tokens"

  object ConversionEventService:
    def build(
        name: AttributionEvent,
        occurredAt: Long,
        dedupeId: String,
        pseudonymousUserId: String,
        attribution: FunnelAttribution,
        valueAudCents: Option[Int],
        plan: Option[PlanTier]
    ): ConversionEvent =
      ConversionEvent(
        name = name,
        occurredAt = occurredAt,
        dedupeId = dedupeId,
        pseudonymousUserId = pseudonymousUserId,
        attribution = attribution,
        valueAudCents = valueAudCents,
        plan = plan
      )

    def dedupe(events: List[ConversionEvent]): List[ConversionEvent] =
      ConversionDedupe.unique(events)

  final case class LaunchDashboardRow(
      group: LaunchDashboardGroup,
      conversionCount: Int,
      revenueAudCents: Int,
      uniqueUsers: Int
  ):
    def valid: Boolean =
      group.validForMvp &&
        conversionCount >= 0 &&
        revenueAudCents >= 0 &&
        uniqueUsers >= 0

  object LaunchDashboardBuilder:
    val requiredGroupDimensions: Set[String] =
      Set("source", "campaign", "variant", "account_type", "token_source", "summary_source", "queue_health", "plan")

    def groupFor(
        attribution: FunnelAttribution,
        accountType: AccountType,
        tokenSource: TokenSource,
        summarySource: SummarySource,
        queueHealth: QueueHealth,
        plan: Option[PlanTier]
    ): LaunchDashboardGroup =
      LaunchDashboardGroup(
        source = attribution.utmSource.orElse(attribution.firstTouch).getOrElse("unknown"),
        campaign = attribution.utmCampaign.orElse(attribution.latestTouch).getOrElse("unknown"),
        variant = attribution.variant,
        accountType = accountType,
        tokenSource = tokenSource,
        summarySource = summarySource,
        queueHealth = queueHealth,
        plan = plan,
        usesInvasiveRiskScoring = false
      )

    def row(group: LaunchDashboardGroup, conversions: List[ConversionEvent]): LaunchDashboardRow =
      val uniqueUsers = conversions.map(_.pseudonymousUserId).toSet.size
      LaunchDashboardRow(
        group = group,
        conversionCount = conversions.size,
        revenueAudCents = conversions.flatMap(_.valueAudCents).sum,
        uniqueUsers = uniqueUsers
      )

    def hasRequiredDimensions(dimensions: Set[String]): Boolean =
      requiredGroupDimensions.subsetOf(dimensions)

  final case class PaidLaunchGateResult(
      allowed: Boolean,
      blockedReasons: List[String],
      pausedSurfaces: Set[LaunchSurface]
  )

  object PaidLaunchGate:
    def assess(
        readiness: PaidLaunchReadiness,
        render: FunnelRenderDecision,
        conversions: List[ConversionEvent],
        privacyScan: PrivacyScan,
        dashboardRows: List[LaunchDashboardRow]
    ): PaidLaunchGateResult =
      val dedupeOk = ConversionEventService.dedupe(conversions).size == conversions.size
      val dashboardOk = dashboardRows.nonEmpty && dashboardRows.forall(_.valid)
      val reasons =
        List(
          Option.when(!readiness.seriousPaidLaunchAllowed)("paid_launch_checks_incomplete"),
          Option.when(!render.valid)("marketing_render_invalid"),
          Option.when(render.pausedSurfaces.nonEmpty)("launch_surfaces_paused"),
          Option.when(!render.trackingDestinationsEnabled)("tracking_destinations_disabled"),
          Option.when(!dedupeOk)("conversion_dedupe_failed"),
          Option.when(!privacyScan.passes)("privacy_scan_failed"),
          Option.when(!dashboardOk)("launch_dashboard_invalid")
        ).flatten

      PaidLaunchGateResult(
        allowed = reasons.isEmpty,
        blockedReasons = reasons,
        pausedSurfaces = render.pausedSurfaces
      )

    def allRequiredChecksVerified: PaidLaunchReadiness =
      PaidLaunchReadiness(
        PaidLaunchCheck.values.map(check => check -> LaunchCheckStatus(verified = true, explicitlyUnavailableDecision = None)).toMap
      )

  object MarketingFunnelSeam:
    val seam: IntegrationSeam = IntegrationSeam.MarketingFunnel
    val patchMapRequiredBeforeLilaAdapter = true
    val patchMapEntryRequiredNow = false

    def registered: Boolean =
      IntegrationSeamRegistry.all.exists(rule =>
        rule.seam == seam &&
          rule.adaptedToLila &&
          rule.serverAuthoritative &&
          !rule.fairnessAffecting
      )

  object FunnelFairnessBoundary:
    val marketingMayChangeRatedFairness = false

    def unchangedByFunnel(before: FairnessSnapshot, after: FairnessSnapshot): Boolean =
      before == after
