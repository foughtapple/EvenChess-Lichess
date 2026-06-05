package lila.evenchess

class MonetisationPolicyTest extends munit.FunSuite:

  import AiCoachPolicy.SummaryType
  import CoachingLadder.{ ExactnessClass, Level }
  import MonetisationPolicy.*
  import ProductInvariants.RequirementClass

  private val activeAccount =
    AccountIdentity(
      accountId = "account-1",
      email = "player@example.com",
      username = "player",
      lifecycle = AccountLifecycle.Active,
      emailWasPreviouslyClosed = false
    )

  test("Appendix N requirements are classified before implementation"):
    val byRequirement =
      MonetisationRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(MonetisationRequirement.AccountIdentity), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(MonetisationRequirement.OnboardingTokens), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(MonetisationRequirement.FairnessBoundary), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(MonetisationRequirement.MvpRiskControlsExcluded), RequirementClass.UnresolvedProductOwnerDecision)

  test("plan policies match onboarding, Standard, and Premium requirements"):
    assertEquals(Plans.onboarding.gameTokensGranted, 10)
    assertEquals(Plans.onboarding.matchSummaryTokens, 3)
    assertEquals(Plans.onboarding.performanceSummaryTokens, 1)
    assertEquals(Plans.standard.billing.map(_.amountAudCents), Some(1000))
    assertEquals(Plans.standard.billing.map(_.weeklyAmountAudCents), Some(250))
    assert(Plans.standard.adFreeGameAccess)
    assert(Plans.standard.reasonableUseApplies)
    assertEquals(Plans.premium.billing.map(_.amountAudCents), Some(1600))
    assertEquals(Plans.premium.billing.map(_.weeklyAmountAudCents), Some(400))
    assertEquals(Plans.premium.matchSummaryDailyLimit, 10)
    assertEquals(Plans.premium.performanceSummaryDailyLimit, 1)

  test("account rules block duplicate active email and deny onboarding to reused closed-account email"):
    val closedReuse =
      AccountIdentity(
        accountId = "account-2",
        email = "closed@example.com",
        username = "closed",
        lifecycle = AccountLifecycle.Active,
        emailWasPreviouslyClosed = true
      )

    assert(activeAccount.hasRequiredIdentity)
    assert(AccountRules.sameEmailHasActiveAccount("player@example.com", List(activeAccount)))
    assert(!AccountRules.canCreateActiveAccount("player@example.com", List(activeAccount)))
    assert(AccountRules.canCreateActiveAccount("new@example.com", List(activeAccount)))
    assert(AccountRules.onboardingEligible(activeAccount))
    assert(!AccountRules.onboardingEligible(closedReuse))

  test("multiple accounts keep separate ECR, tokens, subscriptions, quotas, and leaderboard eligibility"):
    val first = AccountEntitlements(
      accountId = "account-1",
      ecrAccountKey = "ecr-1",
      gameTokens = 10,
      earnedAdGameTokens = 0,
      matchSummaryTokens = 3,
      performanceSummaryTokens = 1,
      subscription = None,
      leaderboardEligible = false
    )
    val second = first.copy(
      accountId = "account-2",
      ecrAccountKey = "ecr-2",
      gameTokens = 0,
      subscription = Some(PlanTier.Premium),
      leaderboardEligible = true
    )

    assert(first.separateFrom(second))
    assert(first.ownsTokensAndQuotas)
    assert(!EntitlementTransfers.canTransferTokens(first, second))
    assert(!EntitlementTransfers.canTransferQuotas(first, second))

  test("rewarded ad completion grants capped free game tokens"):
    val empty = RewardedAdBank(earnedGameTokens = 0)
    val full = RewardedAdBank(earnedGameTokens = 3)

    assert(empty.canWatchRewardedAd)
    assertEquals(empty.grantCompletedAdToken.earnedGameTokens, 1)
    assert(!full.canWatchRewardedAd)
    assertEquals(full.grantCompletedAdToken.earnedGameTokens, 3)

  test("token consumption and refund rules follow meaningful-play outcomes"):
    assertEquals(
      TokenConsumption.decide(GameTokenEvent.FailedQueue),
      TokenConsumptionDecision(consumesToken = false, refundsToken = false, triggersCooldown = false)
    )
    assertEquals(
      TokenConsumption.decide(GameTokenEvent.ValidGameStartedMeaningfulPlay),
      TokenConsumptionDecision(consumesToken = true, refundsToken = false, triggersCooldown = false)
    )
    assertEquals(
      TokenConsumption.decide(GameTokenEvent.OpponentAbortBeforeMeaningfulPlay),
      TokenConsumptionDecision(consumesToken = false, refundsToken = true, triggersCooldown = false)
    )
    assertEquals(
      TokenConsumption.decide(GameTokenEvent.TokenHolderAbortAfterAcceptingOrMeaningfulPlay),
      TokenConsumptionDecision(consumesToken = true, refundsToken = false, triggersCooldown = true)
    )
    assertEquals(
      TokenConsumption.decide(GameTokenEvent.PlatformOutageAbort),
      TokenConsumptionDecision(consumesToken = false, refundsToken = true, triggersCooldown = false)
    )

  test("summary access changes quotas only, not live fairness"):
    assertEquals(
      SummaryAccess.decide(PlanTier.FreeAdSupported, SummaryType.Match, remainingFreeTokens = 1, remainingPremiumDailyQuota = 0),
      SummaryAccessDecision(allowed = true, consumesQuota = true, "free_token")
    )
    assertEquals(
      SummaryAccess.decide(PlanTier.Premium, SummaryType.Match, remainingFreeTokens = 0, remainingPremiumDailyQuota = 10),
      SummaryAccessDecision(allowed = true, consumesQuota = true, "premium_match_daily")
    )
    assertEquals(
      SummaryAccess.decide(PlanTier.Standard, SummaryType.Performance, remainingFreeTokens = 0, remainingPremiumDailyQuota = 0),
      SummaryAccessDecision(allowed = false, consumesQuota = false, "quota_unavailable")
    )

  test("payment, ads, tokens, and quotas cannot mutate rated fairness fields"):
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
    val after = before.copy()
    val stronger = before.copy(liveCoachingStrengthKey = "premium-stronger")

    assert(!FairnessBoundary.subscriptionsAdsTokensQuotasMayBypassFairness)
    assert(!FairnessBoundary.premiumMayProvideStrongerLiveHelp)
    assert(FairnessBoundary.unchangedByMonetisation(before, after))
    assert(!FairnessBoundary.unchangedByMonetisation(before, stronger))

  test("leaderboard eligibility requires games, rating certainty, and clear abuse state"):
    val eligible = LeaderboardEligibility(completedGames = 30, ratingDeviation = 80.0, abuseClear = true)
    val uncertain = eligible.copy(ratingDeviation = 200.0)
    val tooFewGames = eligible.copy(completedGames = 9)
    val abuse = eligible.copy(abuseClear = false)

    assert(eligible.eligible(minGames = 20, maxRatingDeviation = 100.0))
    assert(!uncertain.eligible(minGames = 20, maxRatingDeviation = 100.0))
    assert(!tooFewGames.eligible(minGames = 20, maxRatingDeviation = 100.0))
    assert(!abuse.eligible(minGames = 20, maxRatingDeviation = 100.0))

  test("MVP excludes later risk controls unless approved"):
    assert(!MvpExcludedRiskControls.phoneVerificationRequired)
    assert(!MvpExcludedRiskControls.deviceSessionRiskScoringRequired)
    assert(!MvpExcludedRiskControls.sameIpCreationLimitsRequired)
    assert(!MvpExcludedRiskControls.highRiskClusterTokenDelaysRequired)
