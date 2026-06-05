package lila.evenchess

class MarketingFunnelPolicyTest extends munit.FunSuite:

  import CoachingLadder.{ ExactnessClass, Level }
  import MarketingFunnelPolicy.*
  import MonetisationPolicy.FairnessSnapshot
  import ProductInvariants.RequirementClass

  private val now = 123456789L

  test("Appendix O requirements are classified before implementation"):
    val byRequirement =
      MarketingRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(MarketingRequirement.FairnessNonBypass), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(MarketingRequirement.CampaignCannotAlterFairnessFields), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(MarketingRequirement.BackendReadableConfig), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(MarketingRequirement.AdminControlsAndPause), RequirementClass.AdaptedToLichessFork)

  test("backend-readable marketing config has version, timestamp, required fields, and safe fallback"):
    val config = MarketingConfig.safeFallback(now)

    assert(config.hasRequiredFields)
    assert(config.safeForUse)
    assertEquals(config.version, "marketing-safe-fallback-v1")
    assertEquals(config.updatedAt, now)
    assertEquals(config.heroHeadline, LandingDefaults.headline)
    assert(config.marketingCopy.contains(LandingDefaults.safeDefaultClaim))
    assert(!config.killSwitch)

  test("landing defaults preserve required copy and section taxonomy"):
    assertEquals(LandingDefaults.headline, "Chess that teaches you while you play.")
    assert(LandingDefaults.subheading.contains("separate assisted chess mode"))
    assertEquals(LandingDefaults.cta, "Start with 10 free games.")
    assertEquals(LandingDefaults.offerChip, "10 game tokens; 3 full match summaries; 1 performance summary after 10 completed games.")
    assertEquals(LandingDefaults.trustStrip, List("Opponent knows", "Help is capped", "Rating adjusts"))
    assertEquals(LandingDefaults.requiredSections.toSet, LandingSection.values.toSet)

  test("campaign variants change copy order and emphasis only"):
    assertEquals(LandingVariants.all.map(_.variant).toSet, LandingVariant.values.toSet)
    assert(LandingVariants.all.forall(_.valid))
    assert(!LandingVariants.all.head.copy(changesFairnessFields = true).valid)
    assert(!LandingVariants.all.head.copy(changesOfferAmounts = true).valid)

  test("public copy rejects cheating, hidden engine, off-platform, best-move, and paid strength claims"):
    assert(CopySafety.isAllowed(LandingDefaults.safeDefaultClaim))

    val unsafe = "Premium gives stronger help. Use Stockfish during games. Best move shown live."
    val hits = CopySafety.forbiddenPhraseHits(unsafe)

    assert(hits.contains("premium gives stronger help"))
    assert(hits.contains("use stockfish during games"))
    assert(hits.contains("best move shown live"))
    assert(!CopySafety.isAllowed(unsafe))

  test("offers and pricing include required amounts and fairness footnote"):
    assertEquals(PricingCopy.freeOffer, "10 game tokens, 3 match summaries, 1 performance summary after 10 games.")
    assertEquals(PricingCopy.standard, "$10 AUD/4 weeks ($2.50/week).")
    assertEquals(PricingCopy.premium, "$16 AUD/4 weeks ($4/week), plus 10 match summaries/day and 1 performance summary/day.")
    assert(PricingCopy.includesFairnessFootnote(PricingCopy.fairnessFootnote))
    assert(!PricingCopy.includesFairnessFootnote(PricingCopy.standard))

  test("play windows may show labels but cannot manipulate hidden queue or fairness"):
    val active = PlayWindow("window-1", startsAt = 100, endsAt = 200, label = "launch")
    val future = PlayWindow("window-2", startsAt = 300, endsAt = 400, label = "later")

    assert(active.activeAt(150))
    assertEquals(PlayWindowDisplay.labelFor(150, List(active, future)), "Play now")
    assertEquals(PlayWindowDisplay.labelFor(250, List(active, future)), "Next window")
    assert(!PlayWindowDisplay.manipulatesHiddenQueueOrFairness)

  test("attribution captures campaign tags and includes all Appendix O events"):
    val tags = AttributionTags(
      utmSource = Some("search"),
      utmMedium = Some("cpc"),
      utmCampaign = Some("launch"),
      utmContent = Some("hero-a"),
      utmTerm = Some("assisted chess"),
      clickId = Some("click-1"),
      variant = LandingVariant.Default,
      firstCampaign = Some("launch"),
      latestCampaign = Some("retarget")
    )

    assert(tags.valid)
    assert(AttributionEvents.includesRequired(AttributionEvents.required))
    assert(!AttributionEvents.includesRequired(Set(AttributionEvent.LandingPageView, AttributionEvent.ViewPricing)))

  test("admin controls include required flags and paid acquisition pause notice state"):
    val controls = AdminMarketingControls(
      marketingSiteEnabled = true,
      activeLandingVariant = LandingVariant.FreeTokens,
      offerVersion = "offer-v1",
      playWindows = Nil,
      rewardedAdsEnabled = true,
      standardPlanEnabled = true,
      premiumPlanEnabled = true,
      paidAcquisitionMode = true,
      campaignPauseNotice = Some("paused")
    )

    assert(controls.hasRequiredFields)
    assert(controls.marketingSiteEnabled)
    assert(controls.rewardedAdsEnabled)
    assertEquals(controls.activeLandingVariant, LandingVariant.FreeTokens)

  test("paid acquisition pauses if tracking, payments, queue, or copy safety breaks"):
    assert(!PaidAcquisitionHealth(trackingOk = true, paymentsOk = true, queueOk = true, copySafetyOk = true).shouldPause)
    assert(PaidAcquisitionHealth(trackingOk = false, paymentsOk = true, queueOk = true, copySafetyOk = true).shouldPause)
    assert(PaidAcquisitionHealth(trackingOk = true, paymentsOk = false, queueOk = true, copySafetyOk = true).shouldPause)
    assert(PaidAcquisitionHealth(trackingOk = true, paymentsOk = true, queueOk = false, copySafetyOk = true).shouldPause)
    assert(PaidAcquisitionHealth(trackingOk = true, paymentsOk = true, queueOk = true, copySafetyOk = false).shouldPause)

  test("marketing config and campaigns cannot change rated fairness fields"):
    val before = FairnessSnapshot(
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
    val unchanged = before.copy()
    val changed = before.copy(stockfishProfileKey = "premium-sf")

    assert(!MarketingFairnessBoundary.marketingMayBypassFairness)
    assert(!MarketingFairnessBoundary.campaignMayAlterCoachingPermission)
    assert(!MarketingFairnessBoundary.campaignMayAlterStockfishExposure)
    assert(!MarketingFairnessBoundary.paidPlansMayAlterLiveStrength)
    assert(MarketingFairnessBoundary.unchangedByCampaign(before, unchanged))
    assert(!MarketingFairnessBoundary.unchangedByCampaign(before, changed))
