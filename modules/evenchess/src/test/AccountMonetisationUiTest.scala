package lila.evenchess

class AccountMonetisationUiTest extends munit.FunSuite:

  import AccountMonetisationUi.*
  import AiCoachPolicy.SummaryType
  import CoachingLadder.{ ExactnessClass, Level }
  import MonetisationPolicy.{ FairnessSnapshot, GameTokenEvent, PlanTier }
  import ProductInvariants.RequirementClass
  import SubscriptionTokensAds.{ EntitlementState, RewardedAdRules, SubscriptionFairnessBoundary }

  private val now = 123456789L

  private def state =
    EntitlementState.empty("account-1", "evenchess-ecr-account-1", now).copy(
      gameTokens = 10,
      earnedAdGameTokens = 1,
      matchSummaryTokens = 3,
      performanceSummaryTokens = 1,
      leaderboardEligible = true
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

  test("Version 1.2 Phase J requirements are classified before implementation"):
    val byRequirement =
      PhaseJRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseJRequirement.LichessAccountFoundation), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseJRequirement.AccountEntitlementDashboard), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.PlayStartUsesAccountTokens), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseJRequirement.StableFunnelEvents), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseJRequirement.FairnessBoundary), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseJRequirement.PaymentProviderSideEffectsExcluded), RequirementClass.UnresolvedProductOwnerDecision)

  test("account dashboard exposes onboarding balances and play token snapshot"):
    val dashboard = AccountDashboard.forLichessUser("player-1", now)

    assert(dashboard.valid)
    assertEquals(dashboard.state.gameTokens, 10)
    assertEquals(dashboard.state.matchSummaryTokens, 3)
    assertEquals(dashboard.state.performanceSummaryTokens, 1)
    assertEquals(dashboard.tokenSnapshot.availableGameStarts, 10)
    assertEquals(dashboard.tokenSnapshot.source, EntitlementSource.tokenSnapshotSource)
    assert(dashboard.tokenSnapshot.fairnessNeutral)
    assert(dashboard.tokenSnapshot.eligibleFor(PlaySearchIntegration.PlayMode.RatedEvenChess))

  test("top bar game-token balance links to rewarded ads without fairness claims"):
    val balance = TopBarGameTokenBalance.fromState(state)

    assert(balance.valid)
    assertEquals(balance.visibleGameTokens, 11)
    assertEquals(balance.displayCount, "11")
    assertEquals(balance.displayLabel, "tokens")
    assertEquals(balance.href, Routes.rewardedAds)
    assert(balance.ariaLabel.contains("EvenChess"))
    assert(balance.title.contains("rewarded ads"))
    assert(!balance.fairnessAffecting)
    assert(!balance.title.contains("stronger"))

  test("plan cards show required prices and never sell stronger live help"):
    val dashboard = AccountDashboard.fromState(state)
    val byTier = dashboard.planCards.map(card => card.tier -> card).toMap

    assertEquals(byTier(PlanTier.Standard).price.total, "$10 AUD / 4 weeks")
    assertEquals(byTier(PlanTier.Standard).price.weekly, Some("$2.50 AUD/week"))
    assertEquals(byTier(PlanTier.Premium).price.total, "$16 AUD / 4 weeks")
    assertEquals(byTier(PlanTier.Premium).price.weekly, Some("$4 AUD/week"))
    assert(byTier(PlanTier.Premium).features.exists(_.contains("live help is not stronger")))
    assert(dashboard.planCards.forall(!_.fairnessAffecting))
    assert(dashboard.planCards.forall(!_.liveStrengthUpgrade))
    assert(dashboard.planCards.filter(_.checkoutHref.nonEmpty).forall(_.serverVerificationRequired))
    assert(dashboard.planCards.filter(_.checkoutHref.nonEmpty).isEmpty)
    assertEquals(byTier(PlanTier.Standard).checkoutEventName, Some("checkout_start"))

  test("rewarded ad and summary quota states are capped and fair"):
    val dashboard = AccountDashboard.fromState(state)
    val fullAdBank = RewardedAdStatus.fromState(state.copy(earnedAdGameTokens = RewardedAdRules.earnedTokenCap))

    assertEquals(dashboard.rewardedAdStatus.earnedTokens, 1)
    assertEquals(dashboard.rewardedAdStatus.cap, 3)
    assert(dashboard.rewardedAdStatus.canRequestAd)
    assert(!dashboard.rewardedAdStatus.providerVerified)
    assertEquals(dashboard.rewardedAdStatus.eventName, "rewarded_ad_complete")
    assert(!dashboard.rewardedAdStatus.fairnessAffecting)

    assert(!fullAdBank.canRequestAd)
    assertEquals(fullAdBank.stateText, "Your rewarded token bank is full.")

    assertEquals(dashboard.summaryQuotaStatus.matchRemaining, 3)
    assertEquals(dashboard.summaryQuotaStatus.performanceRemaining, 1)
    assert(!dashboard.summaryQuotaStatus.failedSummaryConsumesQuota)
    assertEquals(SummaryQuotaStatus.visibleReason(SummaryType.Match, dashboard.summaryQuotaStatus), "match_summary_available")
    assertEquals(dashboard.summaryQuotaStatus.matchEventName, "match_summary_view")
    assertEquals(dashboard.summaryQuotaStatus.performanceEventName, "performance_summary_view")

  test("visible token settlement rules match server consumption and refund policy"):
    val byEvent = SettlementRules.all.map(row => row.event -> row).toMap

    assert(byEvent.values.forall(_.valid))
    assert(!byEvent(GameTokenEvent.FailedQueue).consumesToken)
    assert(!byEvent(GameTokenEvent.FailedQueue).refundsToken)
    assert(byEvent(GameTokenEvent.ValidGameStartedMeaningfulPlay).consumesToken)
    assert(byEvent(GameTokenEvent.OpponentAbortBeforeMeaningfulPlay).refundsToken)
    assert(byEvent(GameTokenEvent.PlatformOutageAbort).refundsToken)
    assert(byEvent(GameTokenEvent.TokenHolderAbortAfterAcceptingOrMeaningfulPlay).consumesToken)
    assert(byEvent(GameTokenEvent.TokenHolderAbortAfterAcceptingOrMeaningfulPlay).triggersCooldown)

  test("account monetisation UX cannot mutate rated fairness fields"):
    val dashboard = AccountDashboard.fromState(state.copy(plan = PlanTier.Premium))
    val before = fairness
    val after = before.copy()
    val stronger = before.copy(liveCoachingStrengthKey = "premium-stronger")

    assert(dashboard.valid)
    assert(!SubscriptionFairnessBoundary.subscriptionsAdsTokensMayChangeRatedFairness)
    assert(!SubscriptionFairnessBoundary.premiumMayProvideStrongerLiveHelp)
    assert(SubscriptionFairnessBoundary.unchangedByPhaseI(before, after))
    assert(!SubscriptionFairnessBoundary.unchangedByPhaseI(before, stronger))
    assert(dashboard.fairnessCopy.contains("never change live rated help strength"))
