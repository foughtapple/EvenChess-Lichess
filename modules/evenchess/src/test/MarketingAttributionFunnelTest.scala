package lila.evenchess

class MarketingAttributionFunnelTest extends munit.FunSuite:

  import AdminOperations.{ LaunchHealth, LaunchSurface, PaidLaunchCheck, PaidLaunchReadiness, LaunchCheckStatus }
  import CoachingLadder.{ ExactnessClass, Level }
  import MarketingAttributionFunnel.*
  import MarketingFunnelPolicy.{ AdminMarketingControls, AttributionEvent, LandingVariant, MarketingConfig, PaidAcquisitionHealth, PlayWindow }
  import MonetisationPolicy.{ FairnessSnapshot, PlanTier }
  import ProductInvariants.RequirementClass
  import TelemetryAnalytics.{ AccountType, PrivacyScan, QueueHealth, SummarySource, TokenSource }

  private val now = 123456789L

  private val config = MarketingConfig.safeFallback(now).copy(
    version = "campaign-v1",
    landingVariant = LandingVariant.FreeTokens,
    playWindows = List(PlayWindow("window-1", startsAt = 1L, endsAt = 2L, label = "launch"))
  )

  private val controls =
    AdminMarketingControls(
      marketingSiteEnabled = true,
      activeLandingVariant = LandingVariant.FreeTokens,
      offerVersion = "offer-v1",
      playWindows = List(PlayWindow("window-1", startsAt = 1L, endsAt = 2L, label = "launch")),
      rewardedAdsEnabled = true,
      standardPlanEnabled = true,
      premiumPlanEnabled = true,
      paidAcquisitionMode = true,
      campaignPauseNotice = None
    )

  private val paidHealth =
    PaidAcquisitionHealth(trackingOk = true, paymentsOk = true, queueOk = true, copySafetyOk = true)

  private val launchHealth =
    LaunchHealth(
      rewardedAdsHealthy = true,
      standardPromotionHealthy = true,
      premiumPromotionHealthy = true,
      campaignVariantsHealthy = true,
      trackingDestinationsHealthy = true,
      playWindowsHealthy = true,
      queueFacingCampaignsHealthy = true
    )

  private val privacyScan =
    PrivacyScan(
      collectsOnlyNeededEvents = true,
      usesPseudonymousAnalyticsIds = true,
      separatesRetentionTiers = true,
      avoidsUnnecessarySensitiveAdData = true
    )

  private def capture =
    AttributionCapture.capture(
      FunnelCaptureInput(
        attributionId = "attr-1",
        pseudonymousAccountId = "acct-hash-1",
        utmSource = Some("google"),
        utmMedium = Some("cpc"),
        utmCampaign = Some("launch"),
        utmContent = Some("hero-a"),
        utmTerm = Some("assisted chess"),
        clickId = Some("click-1"),
        variant = LandingVariant.FreeTokens,
        configVersion = config.version,
        firstTouch = Some("google"),
        latestTouch = Some("retarget"),
        signupId = Some("signup-1"),
        firstGameId = Some("game-1"),
        subscriptionId = Some("sub-1"),
        rawEmail = Some("player@example.com")
      )
    )

  private def fairness =
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

  test("Version 1.1 Phase J requirements are classified before implementation"):
    val byRequirement =
      PhaseJRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseJRequirement.LichessPublicPageFoundation), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseJRequirement.BackendReadableMarketingConfig), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.CampaignAttributionCapture), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.LaunchDashboardGrouping), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseJRequirement.MarketingFunnelSeam), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseJRequirement.PrivacyAndNoInvasiveRiskScoring), RequirementClass.EvenChessSpecific)

  test("configured marketing copy serves only when site, copy, and launch health are safe"):
    val render = FunnelConfigService.resolve(config, controls, paidHealth, launchHealth, now)

    assert(render.valid)
    assert(render.servingConfiguredCopy)
    assertEquals(render.activeVariant, LandingVariant.FreeTokens)
    assertEquals(render.config.version, config.version)
    assertEquals(render.pausedSurfaces, Set.empty)
    assert(render.trackingDestinationsEnabled)
    assert(render.notice.isEmpty)

  test("unsafe copy, kill switch, and disabled site use safe fallback or public pause notice"):
    val unsafe = config.copy(heroHeadline = "Premium gives stronger help")
    val unsafeRender = FunnelConfigService.resolve(unsafe, controls, paidHealth, launchHealth, now + 1)
    val killed = FunnelConfigService.resolve(config.copy(killSwitch = true), controls, paidHealth, launchHealth, now + 2)
    val disabledSite = FunnelConfigService.resolve(config, controls.copy(marketingSiteEnabled = false), paidHealth, launchHealth, now + 3)

    assertEquals(unsafeRender.decision, FunnelDecision.ServeSafeFallback)
    assertEquals(unsafeRender.config.version, "marketing-safe-fallback-v1")
    assert(unsafeRender.pausedSurfaces.contains(LaunchSurface.CampaignVariants))
    assert(unsafeRender.notice.exists(_.valid))

    assertEquals(killed.decision, FunnelDecision.Paused)
    assertEquals(killed.activeVariant, LandingVariant.Default)
    assert(killed.notice.exists(_.valid))

    assertEquals(disabledSite.decision, FunnelDecision.Paused)
    assert(disabledSite.pausedSurfaces.contains(LaunchSurface.QueueFacingCampaigns))
    assert(disabledSite.notice.exists(_.valid))

  test("paid acquisition health pauses tracking destinations without manipulating fairness"):
    val degradedLaunch = launchHealth.copy(
      rewardedAdsHealthy = false,
      standardPromotionHealthy = false,
      trackingDestinationsHealthy = false,
      playWindowsHealthy = false,
      queueFacingCampaignsHealthy = false
    )
    val render = FunnelConfigService.resolve(
      config,
      controls.copy(campaignPauseNotice = Some("Paid acquisition paused while queue health recovers.")),
      paidHealth.copy(queueOk = false),
      degradedLaunch,
      now + 1
    )

    assertEquals(render.decision, FunnelDecision.Paused)
    assert(render.paidAcquisitionPaused)
    assert(!render.trackingDestinationsEnabled)
    assert(render.pausedSurfaces.contains(LaunchSurface.RewardedAds))
    assert(render.pausedSurfaces.contains(LaunchSurface.StandardPromotion))
    assert(render.pausedSurfaces.contains(LaunchSurface.TrackingDestinations))
    assert(render.pausedSurfaces.contains(LaunchSurface.PlayWindows))
    assert(render.pausedSurfaces.contains(LaunchSurface.QueueFacingCampaigns))
    assert(render.notice.exists(_.valid))
    assert(render.notice.exists(!_.manipulatesHiddenQueueOrFairness))

  test("attribution capture stores pseudonymous campaign fields and never raw email"):
    val captured = capture

    assert(captured.valid)
    assert(captured.storage.valid)
    assertEquals(captured.storage.configVersion, config.version)
    assertEquals(captured.storage.variant, "free_tokens")
    assertEquals(captured.storage.utmSource, Some("google"))
    assertEquals(captured.storage.utmCampaign, Some("launch"))
    assertEquals(captured.storage.subscriptionId, Some("sub-1"))
    assert(!captured.storage.storesRawEmail)
    assert(captured.telemetry.hasCampaignIdentity)
    assert(captured.telemetry.linksLifecycle)

  test("required funnel event catalog uses stable Appendix O event names"):
    assert(FunnelEventCatalog.coversAppendixORequiredEvents)
    assertEquals(FunnelEventCatalog.stableName(AttributionEvent.LandingPageView), "landing_page_view")
    assertEquals(FunnelEventCatalog.stableName(AttributionEvent.SignUpComplete), "sign_up_complete")
    assertEquals(FunnelEventCatalog.stableName(AttributionEvent.FirstGameStarted), "first_game_started")
    assertEquals(FunnelEventCatalog.stableName(AttributionEvent.TenGamesCompleted), "ten_games_completed")
    assertEquals(FunnelEventCatalog.stableName(AttributionEvent.RewardedAdComplete), "rewarded_ad_complete")
    assertEquals(FunnelEventCatalog.stableName(AttributionEvent.PerformanceSummaryView), "performance_summary_view")

  test("conversion events carry stable shape and dedupe before launch reporting"):
    val attribution = capture.telemetry
    val purchase = ConversionEventService.build(
      name = AttributionEvent.Purchase,
      occurredAt = now + 1,
      dedupeId = "dedupe-1",
      pseudonymousUserId = "user-hash-1",
      attribution = attribution,
      valueAudCents = Some(1000),
      plan = Some(PlanTier.Standard)
    )
    val duplicate = purchase.copy(valueAudCents = Some(1600))

    assert(purchase.validShape)
    assertEquals(FunnelEventCatalog.stableName(purchase.name), "purchase")
    assertEquals(ConversionEventService.dedupe(List(purchase, duplicate)), List(purchase))
    assert(!purchase.copy(dedupeId = "").validShape)

  test("launch dashboard rows group by source, campaign, variant, account, token, summary, queue, and plan"):
    val attribution = capture.telemetry
    val conversion = ConversionEventService.build(
      AttributionEvent.SignUpComplete,
      now + 1,
      "dedupe-signup",
      "user-hash-1",
      attribution,
      valueAudCents = None,
      plan = Some(PlanTier.FreeAdSupported)
    )
    val group = LaunchDashboardBuilder.groupFor(
      attribution,
      accountType = AccountType.New,
      tokenSource = TokenSource.Onboarding,
      summarySource = SummarySource.FreeToken,
      queueHealth = QueueHealth.Healthy,
      plan = Some(PlanTier.FreeAdSupported)
    )
    val row = LaunchDashboardBuilder.row(group, List(conversion))

    assert(LaunchDashboardBuilder.hasRequiredDimensions(LaunchDashboardBuilder.requiredGroupDimensions))
    assert(group.validForMvp)
    assertEquals(group.source, "google")
    assertEquals(group.campaign, "launch")
    assertEquals(group.variant, LandingVariant.FreeTokens)
    assert(!group.usesInvasiveRiskScoring)
    assert(row.valid)
    assertEquals(row.conversionCount, 1)
    assertEquals(row.uniqueUsers, 1)

  test("paid launch gate requires readiness, privacy, dedupe, dashboard validity, and unpaused tracking"):
    val render = FunnelConfigService.resolve(config, controls, paidHealth, launchHealth, now)
    val attribution = capture.telemetry
    val purchase = ConversionEventService.build(
      AttributionEvent.Purchase,
      now + 1,
      "dedupe-purchase",
      "user-hash-1",
      attribution,
      valueAudCents = Some(1600),
      plan = Some(PlanTier.Premium)
    )
    val group = LaunchDashboardBuilder.groupFor(
      attribution,
      AccountType.Subscriber,
      TokenSource.Subscription,
      SummarySource.PremiumDaily,
      QueueHealth.Healthy,
      Some(PlanTier.Premium)
    )
    val row = LaunchDashboardBuilder.row(group, List(purchase))

    val allowed = PaidLaunchGate.assess(PaidLaunchGate.allRequiredChecksVerified, render, List(purchase), privacyScan, List(row))
    assert(allowed.allowed)

    val missingDecisionReadiness =
      PaidLaunchReadiness(
        PaidLaunchCheck.values.map(check => check -> LaunchCheckStatus(verified = true, explicitlyUnavailableDecision = None)).toMap
          .updated(PaidLaunchCheck.MetaPixelCapi, LaunchCheckStatus(verified = false, explicitlyUnavailableDecision = None))
      )
    val duplicateBlocked = PaidLaunchGate.assess(
      PaidLaunchGate.allRequiredChecksVerified,
      render,
      List(purchase, purchase.copy(valueAudCents = Some(2000))),
      privacyScan,
      List(row)
    )
    val readinessBlocked = PaidLaunchGate.assess(missingDecisionReadiness, render, List(purchase), privacyScan, List(row))
    val privacyBlocked = PaidLaunchGate.assess(
      PaidLaunchGate.allRequiredChecksVerified,
      render,
      List(purchase),
      privacyScan.copy(avoidsUnnecessarySensitiveAdData = false),
      List(row)
    )

    assert(!duplicateBlocked.allowed)
    assert(duplicateBlocked.blockedReasons.contains("conversion_dedupe_failed"))
    assert(!readinessBlocked.allowed)
    assert(readinessBlocked.blockedReasons.contains("paid_launch_checks_incomplete"))
    assert(!privacyBlocked.allowed)
    assert(privacyBlocked.blockedReasons.contains("privacy_scan_failed"))

  test("future lila marketing adapter uses registered seam and no patch map entry is needed yet"):
    assert(MarketingFunnelSeam.registered)
    assert(MarketingFunnelSeam.patchMapRequiredBeforeLilaAdapter)
    assert(!MarketingFunnelSeam.patchMapEntryRequiredNow)

  test("marketing attribution and funnel controls cannot change rated fairness fields"):
    val before = fairness
    val unchanged = before.copy()
    val changed = before.copy(stockfishProfileKey = "campaign-stockfish")

    assert(!FunnelFairnessBoundary.marketingMayChangeRatedFairness)
    assert(FunnelFairnessBoundary.unchangedByFunnel(before, unchanged))
    assert(!FunnelFairnessBoundary.unchangedByFunnel(before, changed))
