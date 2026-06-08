package lila.evenchess

class SubscriptionTokensAdsTest extends munit.FunSuite:

  import AbuseTrustControls.MitigationAction
  import AiCoachPolicy.SummaryType
  import CoachingLadder.{ ExactnessClass, Level }
  import DataModelsAndSeams.TokenLedgerEvent
  import MarketingFunnelPolicy.{ AdminMarketingControls, LandingVariant, PaidAcquisitionHealth, PlayWindow }
  import MonetisationPolicy.{ AccountIdentity, AccountLifecycle, FairnessSnapshot, GameTokenEvent, PlanTier }
  import ProductInvariants.RequirementClass
  import SubscriptionTokensAds.*

  private val now = 123456789L

  private val account =
    AccountIdentity(
      accountId = "account-1",
      email = "player@example.com",
      username = "player",
      lifecycle = AccountLifecycle.Active,
      emailWasPreviouslyClosed = false
    )

  private val controls =
    AdminMarketingControls(
      marketingSiteEnabled = true,
      activeLandingVariant = LandingVariant.FreeTokens,
      offerVersion = "offer-v1",
      playWindows = List(PlayWindow("launch", 1L, 2L, "launch")),
      rewardedAdsEnabled = true,
      standardPlanEnabled = true,
      premiumPlanEnabled = true,
      paidAcquisitionMode = true,
      campaignPauseNotice = None
    )

  private val healthy =
    PaidAcquisitionHealth(trackingOk = true, paymentsOk = true, queueOk = true, copySafetyOk = true)

  private def emptyState =
    EntitlementState.empty("account-1", "ecr-account-1", now)

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

  test("Version 1.1 Phase I requirements are classified before implementation"):
    val byRequirement =
      PhaseIRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseIRequirement.LichessAccountFoundation), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseIRequirement.AccountSubscriptionStorageSeam), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseIRequirement.OnboardingGrant), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseIRequirement.StandardPremiumPlans), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseIRequirement.RewardedAdGrant), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseIRequirement.CampaignAndAbuseControls), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseIRequirement.MvpInvasiveControlsExcluded), RequirementClass.UnresolvedProductOwnerDecision)

  test("Version 2 Phase P monetisation requirements are classified before adapter work"):
    val byRequirement =
      PhasePRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhasePRequirement.LichessAccountFoundation), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhasePRequirement.AccountStateIncludesMonetisationEntitlements), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhasePRequirement.TopBarAndAccountTokenVisibility), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhasePRequirement.GameTokenMeaningfulPlaySettlement), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhasePRequirement.SubscriptionBypassesAdTokenLimitsOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhasePRequirement.SavedGameCountsConfigurable), RequirementClass.UnresolvedProductOwnerDecision)
    assertEquals(byRequirement(PhasePRequirement.MonetisationFairnessBoundary), RequirementClass.EvenChessSpecific)

  test("eligible onboarding grants tokens and rejects duplicate or reused account identities"):
    val granted = OnboardingGrantService.grant(account, existingAccounts = Nil, now = now)

    assert(granted.allowed)
    assertEquals(granted.reason, "eligible_new_account")
    assertEquals(granted.state.map(_.gameTokens), Some(10))
    assertEquals(granted.state.map(_.matchSummaryTokens), Some(3))
    assertEquals(granted.state.map(_.performanceSummaryTokens), Some(1))
    assertEquals(granted.state.map(_.plan), Some(PlanTier.FreeAdSupported))
    assert(granted.state.exists(_.valid))
    assertEquals(granted.ledgerEntries.map(_.event), List(TokenLedgerEvent.OnboardingGranted))
    assertEquals(granted.ledgerEntries.map(_.amount), List(10))
    assert(granted.ledgerEntries.forall(_.valid))

    val duplicate =
      account.copy(accountId = "account-2", username = "other")
    val duplicateGrant = OnboardingGrantService.grant(duplicate, existingAccounts = List(account), now = now)
    assert(!duplicateGrant.allowed)
    assertEquals(duplicateGrant.reason, "duplicate_active_email")

    val reused =
      account.copy(accountId = "account-3", email = "reused@example.com", emailWasPreviouslyClosed = true)
    val reusedGrant = OnboardingGrantService.grant(reused, existingAccounts = Nil, now = now)
    assert(!reusedGrant.allowed)
    assertEquals(reusedGrant.reason, "reused_closed_account_email")

  test("Standard and Premium plans are access and quota entitlements only"):
    val standard = SubscriptionPlanService.activate(emptyState, PlanTier.Standard, controls, now + 1)
    val premium = SubscriptionPlanService.activate(emptyState, PlanTier.Premium, controls, now + 2)
    val disabledPremium =
      SubscriptionPlanService.activate(emptyState, PlanTier.Premium, controls.copy(premiumPlanEnabled = false), now + 3)

    assert(standard.allowed)
    assertEquals(standard.billingAmountAudCents, Some(1000))
    assert(standard.state.canStartAdFreeGame)
    assertEquals(standard.state.premiumMatchSummaryDailyRemaining, 0)

    assert(premium.allowed)
    assertEquals(premium.billingAmountAudCents, Some(1600))
    assert(premium.state.canStartAdFreeGame)
    assertEquals(premium.state.premiumMatchSummaryDailyRemaining, 10)
    assertEquals(premium.state.premiumPerformanceSummaryDailyRemaining, 1)

    assert(!disabledPremium.allowed)
    assertEquals(disabledPremium.reason, "premium_plan_disabled")
    assert(!SubscriptionFairnessBoundary.premiumMayProvideStrongerLiveHelp)

  test("rewarded ads grant one capped game token to free accounts and honor campaign gates"):
    val first = RewardedAdService.completeAd(emptyState, controls, healthy, TokenAbuseState.clear, now + 1)
    val full = RewardedAdService.completeAd(emptyState.copy(earnedAdGameTokens = 3), controls, healthy, TokenAbuseState.clear, now + 2)
    val subscribed = RewardedAdService.completeAd(emptyState.copy(plan = PlanTier.Standard), controls, healthy, TokenAbuseState.clear, now + 3)
    val disabled = RewardedAdService.completeAd(emptyState, controls.copy(rewardedAdsEnabled = false), healthy, TokenAbuseState.clear, now + 4)
    val pausedHealth = RewardedAdService.completeAd(emptyState, controls, healthy.copy(queueOk = false), TokenAbuseState.clear, now + 5)
    val pausedCampaign =
      RewardedAdService.completeAd(emptyState, controls.copy(campaignPauseNotice = Some("paused")), healthy, TokenAbuseState.clear, now + 6)

    assert(first.granted)
    assertEquals(first.state.earnedAdGameTokens, 1)
    assertEquals(first.ledgerEntries.map(_.event), List(TokenLedgerEvent.AdEarned))
    assert(first.ledgerEntries.forall(_.valid))

    assert(!full.granted)
    assertEquals(full.reason, "earned_ad_token_bank_full")
    assert(!subscribed.granted)
    assertEquals(subscribed.reason, "rewarded_ads_free_accounts_only")
    assert(!disabled.granted)
    assertEquals(disabled.reason, "rewarded_ads_disabled")
    assert(!pausedHealth.granted)
    assertEquals(pausedHealth.reason, "campaign_or_health_paused")
    assert(!pausedCampaign.granted)
    assertEquals(pausedCampaign.reason, "campaign_or_health_paused")

  test("game token settlement consumes only meaningful play and refunds reserved aborts"):
    val funded = emptyState.copy(gameTokens = 1, earnedAdGameTokens = 1)
    val started = GameTokenService.settle(
      funded,
      GameTokenSettlementRequest(GameTokenEvent.ValidGameStartedMeaningfulPlay, "game-1", reservedToken = None, now = now + 1)
    )
    val failedQueue = GameTokenService.settle(
      funded,
      GameTokenSettlementRequest(GameTokenEvent.FailedQueue, "game-2", reservedToken = None, now = now + 2)
    )
    val opponentAbort = GameTokenService.settle(
      funded,
      GameTokenSettlementRequest(GameTokenEvent.OpponentAbortBeforeMeaningfulPlay, "game-3", reservedToken = Some(TokenBucket.RewardedAdGameToken), now = now + 3)
    )
    val platformAbort = GameTokenService.settle(
      funded,
      GameTokenSettlementRequest(GameTokenEvent.PlatformOutageAbort, "game-4", reservedToken = Some(TokenBucket.OnboardingGameToken), now = now + 4)
    )
    val tokenHolderAbort = GameTokenService.settle(
      emptyState.copy(gameTokens = 1),
      GameTokenSettlementRequest(GameTokenEvent.TokenHolderAbortAfterAcceptingOrMeaningfulPlay, "game-5", reservedToken = None, now = now + 5)
    )
    val noTokens = GameTokenService.settle(
      emptyState,
      GameTokenSettlementRequest(GameTokenEvent.ValidGameStartedMeaningfulPlay, "game-6", reservedToken = None, now = now + 6)
    )
    val standardAccess = GameTokenService.settle(
      emptyState.copy(plan = PlanTier.Standard),
      GameTokenSettlementRequest(GameTokenEvent.ValidGameStartedMeaningfulPlay, "game-7", reservedToken = None, now = now + 7)
    )
    val launchFreeWindow = GameTokenService.settle(
      funded,
      GameTokenSettlementRequest(
        GameTokenEvent.ValidGameStartedMeaningfulPlay,
        "game-8",
        reservedToken = None,
        now = now + 8,
        freeMatchTokenWindowActive = true
      )
    )

    assert(started.allowed)
    assertEquals(started.reason, "earned_ad_token_consumed")
    assertEquals(started.state.earnedAdGameTokens, 0)
    assertEquals(started.state.gameTokens, 1)
    assertEquals(started.ledgerEntries.map(_.event), List(TokenLedgerEvent.Consumed))
    assert(started.ledgerEntries.forall(_.valid))

    assert(failedQueue.allowed)
    assertEquals(failedQueue.reason, "no_token_event")
    assertEquals(failedQueue.ledgerEntries, Nil)

    assert(opponentAbort.allowed)
    assertEquals(opponentAbort.decision, EntitlementDecision.Refunded)
    assertEquals(opponentAbort.state.earnedAdGameTokens, 2)
    assertEquals(opponentAbort.ledgerEntries.map(_.event), List(TokenLedgerEvent.Refunded))

    assert(platformAbort.allowed)
    assertEquals(platformAbort.state.gameTokens, 2)
    assertEquals(platformAbort.ledgerEntries.map(_.event), List(TokenLedgerEvent.Refunded))

    assert(tokenHolderAbort.allowed)
    assert(tokenHolderAbort.triggersCooldown)
    assertEquals(tokenHolderAbort.state.gameTokens, 0)

    assert(!noTokens.allowed)
    assertEquals(noTokens.reason, "game_token_unavailable")

    assert(standardAccess.allowed)
    assertEquals(standardAccess.reason, "subscription_access")
    assertEquals(standardAccess.ledgerEntries, Nil)

    assert(launchFreeWindow.allowed)
    assertEquals(launchFreeWindow.reason, "launch_free_token_window")
    assertEquals(launchFreeWindow.state.earnedAdGameTokens, funded.earnedAdGameTokens)
    assertEquals(launchFreeWindow.state.gameTokens, funded.gameTokens)
    assertEquals(launchFreeWindow.ledgerEntries, Nil)

  test("summary quota consumption uses onboarding tokens before Premium daily quotas"):
    val onboarding = emptyState.copy(matchSummaryTokens = 1, performanceSummaryTokens = 1)
    val premium = emptyState.copy(
      plan = PlanTier.Premium,
      premiumMatchSummaryDailyRemaining = 10,
      premiumPerformanceSummaryDailyRemaining = 1
    )

    val onboardingMatch = SummaryQuotaService.consume(onboarding, SummaryType.Match, now + 1)
    val onboardingPerformance = SummaryQuotaService.consume(onboarding, SummaryType.Performance, now + 2)
    val premiumMatch = SummaryQuotaService.consume(premium, SummaryType.Match, now + 3)
    val standardBlocked = SummaryQuotaService.consume(emptyState.copy(plan = PlanTier.Standard), SummaryType.Match, now + 4)

    assert(onboardingMatch.allowed)
    assertEquals(onboardingMatch.reason, "onboarding_match_summary_token")
    assertEquals(onboardingMatch.state.matchSummaryTokens, 0)

    assert(onboardingPerformance.allowed)
    assertEquals(onboardingPerformance.reason, "onboarding_performance_summary_token")
    assertEquals(onboardingPerformance.state.performanceSummaryTokens, 0)

    assert(premiumMatch.allowed)
    assertEquals(premiumMatch.reason, "premium_match_summary_daily")
    assertEquals(premiumMatch.state.premiumMatchSummaryDailyRemaining, 9)

    assert(!standardBlocked.allowed)
    assertEquals(standardBlocked.reason, "summary_quota_unavailable")

  test("review and custom-analysis tokens gate only plans that require extra analysis"):
    val liveEssentials = LiveCoaching.LiveEceLimitedReviewEssentials(
      gameId = "game-1",
      fenHistory = List("fen-1"),
      moveHistory = List("e2e4"),
      highestWhiteLevel = Level(5),
      highestBlackLevel = Level(5),
      outputRefs = List("white-1-5"),
      auditIds = List("audit-1")
    )
    val freeReplayRequest = LiveCoaching.CustomReviewRequest(
      gameId = "game-1",
      whiteLevel = Level(5),
      blackLevel = Level(5),
      perspective = LiveCoaching.CustomReviewPerspective.White,
      eceVersion = "ece-v1",
      policyVersion = "policy-v1",
      useAi = false
    )
    val l10Request = freeReplayRequest.copy(whiteLevel = Level(10), blackLevel = Level(10))
    val freeReplayPlan = LiveCoaching.ReviewModeEngine.planCustomReview(freeReplayRequest, liveEssentials)
    val l10Plan = LiveCoaching.ReviewModeEngine.planCustomReview(l10Request, liveEssentials)
    val noToken = ReviewTokenService.consumeCustomAnalysis(emptyState, l10Plan, now + 1)
    val consumed =
      ReviewTokenService.consumeCustomAnalysis(emptyState.copy(customAnalysisTokens = 1), l10Plan, now + 2)
    val replay =
      ReviewTokenService.consumeCustomAnalysis(emptyState, freeReplayPlan, now + 3)

    assert(freeReplayPlan.valid)
    assert(!freeReplayPlan.requiresCustomAnalysisTokens)
    assert(l10Plan.valid)
    assert(l10Plan.requiresCustomAnalysisTokens)
    assert(!noToken.allowed)
    assertEquals(noToken.reason, "custom_ece_analysis_token_unavailable")
    assert(consumed.allowed)
    assertEquals(consumed.reason, "custom_ece_analysis_token_consumed")
    assertEquals(consumed.state.customAnalysisTokens, 0)
    assertEquals(consumed.ledgerEntries.map(_.event), List(TokenLedgerEvent.Consumed))
    assert(consumed.ledgerEntries.forall(_.valid))
    assert(replay.allowed)
    assertEquals(replay.decision, EntitlementDecision.NoOp)
    assertEquals(replay.reason, "custom_analysis_token_not_required")

  test("full-game ECE review consumes match-review or full-analysis tokens after quota check"):
    val history =
      LiveCoaching.LiveEceHistoryRecord
        .empty("game-1")
        .append(
          LiveCoaching.LiveEceHistoryEntry(
            gameId = "game-1",
            ply = 1,
            fen = "fen-1",
            moveUci = Some("e2e4"),
            positionHash = "hash-1",
            sideToMove = CoachingOverlays.Perspective.Black,
            whiteRequestedLevel = Level(5),
            blackRequestedLevel = Level(5),
            policyVersion = "policy-v1",
            eceVersion = "ece-v1",
            whiteOutput = Some(
              LiveCoaching.LiveEceOutputReference(
                side = CoachingOverlays.Perspective.White,
                outputRef = "white-1-5",
                auditId = "audit-w",
                deliveredLevel = Level(5),
                summary = Some("summary"),
                plan = None,
                overlayAtomRefs = List("atom-1")
              )
            ),
            blackOutput = None,
            rawEceRetained = false,
            createdAt = now
          )
        )
    val game = EngineGateway.EceGameReviewInput(
      gameId = "game-1",
      initialFen = "startpos",
      pgn = None,
      moves = List("e2e4"),
      fenHistory = history.fenHistory,
      result = "1-0",
      termination = "normal"
    )
    val matchPlan = LiveCoaching.ReviewModeEngine.planFullGameReview(
      game = game,
      history = history,
      reviewIndex = 1,
      whiteEcr = None,
      blackEcr = None,
      reviewLevel = Level(8),
      aiNarrativeAllowed = false,
      tokenKind = LiveCoaching.FullGameReviewTokenKind.MatchReview,
      tokenQuotaChecked = true
    )
    val fullPlan = matchPlan.copy(tokenKind = LiveCoaching.FullGameReviewTokenKind.FullAnalysis)
    val blockedPlan = matchPlan.copy(tokenQuotaChecked = false)
    val matchConsumed =
      ReviewTokenService.consumeFullGameReview(emptyState.copy(matchReviewTokens = 1), matchPlan, now + 1)
    val fullConsumed =
      ReviewTokenService.consumeFullGameReview(emptyState.copy(fullAnalysisTokens = 1), fullPlan, now + 2)
    val invalid =
      ReviewTokenService.consumeFullGameReview(emptyState.copy(matchReviewTokens = 1), blockedPlan, now + 3)

    assert(matchPlan.valid)
    assert(matchConsumed.allowed)
    assertEquals(matchConsumed.reason, "match_review_token_consumed")
    assertEquals(matchConsumed.state.matchReviewTokens, 0)
    assert(fullConsumed.allowed)
    assertEquals(fullConsumed.reason, "full_analysis_token_consumed")
    assertEquals(fullConsumed.state.fullAnalysisTokens, 0)
    assert(!invalid.allowed)
    assertEquals(invalid.reason, "invalid_full_game_review_plan")

  test("ad token farming controls block grants only with audited abuse state"):
    val abuse = TokenAbuseState(adTokenFarmingCooldownActive = true, auditId = Some("audit-1"))
    val blocked = RewardedAdService.completeAd(emptyState, controls, healthy, abuse, now + 1)

    assert(abuse.auditedSuppression)
    assert(!blocked.granted)
    assertEquals(blocked.reason, "ad_token_farming_cooldown")
    assertEquals(blocked.auditId, Some("audit-1"))
    assert(RewardedAdRules.adTokenFarmingMitigations.contains(MitigationAction.TokenCap))
    assert(RewardedAdRules.adTokenFarmingMitigations.contains(MitigationAction.Cooldown))
    assert(!TokenAbuseState(adTokenFarmingCooldownActive = true, auditId = None).auditedSuppression)

  test("account entitlements are non-transferable and use the account-subscription seam"):
    val first = emptyState.copy(gameTokens = 10, matchSummaryTokens = 3)
    val second = emptyState.copy(accountId = "account-2", ecrAccountKey = "ecr-account-2")

    assert(first.separateFrom(second))
    assert(!EntitlementTransfers.canTransferTokens(first, second))
    assert(!EntitlementTransfers.canTransferQuotas(first, second))
    assert(AccountSubscriptionSeam.registered)
    assert(AccountSubscriptionSeam.dedicatedStoreNames.contains("evenchess_token_ledger"))
    assert(!AccountSubscriptionSeam.storesRawEmail)
    assert(AccountSubscriptionSeam.patchMapRequiredBeforeLilaAdapter)
    assert(!AccountSubscriptionSeam.patchMapEntryRequiredNow)

  test("saved-game retention is configurable and paid saves persist after downgrade"):
    val config = SavedGameRetentionConfig.default
    val freeBlocked = SavedGameService.save(emptyState, "game-free", config, now + 1)
    val standardState = emptyState.copy(plan = PlanTier.Standard)
    val saved = SavedGameService.save(standardState, "game-paid", config, now + 2)
    val duplicate = SavedGameService.save(saved.state, "game-paid", config, now + 3)
    val downgraded = saved.state.copy(plan = PlanTier.FreeAdSupported)
    val blockedAfterDowngrade = SavedGameService.save(downgraded, "game-new", config, now + 4)

    assert(config.valid)
    assertEquals(config.savedSlotLimit(PlanTier.FreeAdSupported), 0)
    assertEquals(config.savedSlotLimit(PlanTier.Standard), 25)
    assert(!freeBlocked.allowed)
    assertEquals(freeBlocked.reason, "saved_game_slot_unavailable")
    assert(saved.allowed)
    assertEquals(saved.state.savedGameIds, List("game-paid"))
    assert(saved.record.exists(_.valid))
    assert(saved.record.exists(SavedGameService.canKeepAfterDowngrade))
    assert(duplicate.allowed)
    assertEquals(duplicate.decision, EntitlementDecision.NoOp)
    assertEquals(duplicate.reason, "already_saved")
    assert(!blockedAfterDowngrade.allowed)
    assertEquals(downgraded.savedGameIds, List("game-paid"))

  test("subscriptions, tokens, rewarded ads, and quotas do not mutate rated fairness fields"):
    val before = fairness
    val afterPlan = before.copy()
    val afterAd = before.copy()
    val afterToken = before.copy()
    val afterQuota = before.copy()
    val stronger = before.copy(liveCoachingStrengthKey = "premium-stronger")

    assert(!SubscriptionFairnessBoundary.subscriptionsAdsTokensMayChangeRatedFairness)
    assert(SubscriptionFairnessBoundary.unchangedByPhaseI(before, afterPlan))
    assert(SubscriptionFairnessBoundary.unchangedByPhaseI(before, afterAd))
    assert(SubscriptionFairnessBoundary.unchangedByPhaseI(before, afterToken))
    assert(SubscriptionFairnessBoundary.unchangedByPhaseI(before, afterQuota))
    assert(!SubscriptionFairnessBoundary.unchangedByPhaseI(before, stronger))

    val snapshot = MonetisationFairnessSnapshot(
      before = before,
      after = afterPlan,
      operation = "phase_p_subscription_or_token_update",
      touchesPaymentProvider = false,
      changesLiveStrength = false,
      changesRatedSettlement = false
    )

    assert(SubscriptionFairnessBoundary.unchangedByPhaseP(before, afterPlan))
    assert(snapshot.valid)
    assert(!snapshot.copy(after = stronger, changesLiveStrength = true).valid)
