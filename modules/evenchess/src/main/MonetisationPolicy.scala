package lila.evenchess

import AiCoachPolicy.SummaryType
import CoachingLadder.{ ExactnessClass, Level }
import ProductInvariants.RequirementClass

object MonetisationPolicy:

  enum MonetisationRequirement:
    case AccountIdentity
    case OnboardingTokens
    case RewardedAdTokens
    case StandardPlanAccess
    case PremiumSummaryQuotas
    case SeparateAccountEntitlements
    case NonTransferableTokensAndQuotas
    case TokenConsumptionAndRefunds
    case FairnessBoundary
    case MvpRiskControlsExcluded

  final case class MonetisationRequirementClassification(
      requirement: MonetisationRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object MonetisationRequirementClassifications:
    val all: List[MonetisationRequirementClassification] = List(
      MonetisationRequirementClassification(
        MonetisationRequirement.AccountIdentity,
        RequirementClass.AdaptedToLichessFork,
        "Use Lichess account foundations where possible; add EvenChess token/subscription eligibility rules."
      ),
      MonetisationRequirementClassification(
        MonetisationRequirement.OnboardingTokens,
        RequirementClass.EvenChessSpecific,
        "New eligible accounts receive game and summary token grants."
      ),
      MonetisationRequirementClassification(
        MonetisationRequirement.RewardedAdTokens,
        RequirementClass.EvenChessSpecific,
        "Rewarded ads grant capped game tokens to free accounts only."
      ),
      MonetisationRequirementClassification(
        MonetisationRequirement.StandardPlanAccess,
        RequirementClass.EvenChessSpecific,
        "Standard changes access/convenience only, subject to reasonable-use and abuse controls."
      ),
      MonetisationRequirementClassification(
        MonetisationRequirement.PremiumSummaryQuotas,
        RequirementClass.EvenChessSpecific,
        "Premium adds review-summary quotas, never stronger live help."
      ),
      MonetisationRequirementClassification(
        MonetisationRequirement.SeparateAccountEntitlements,
        RequirementClass.EvenChessSpecific,
        "Multiple accounts keep separate ECR, tokens, subscriptions, quotas, and leaderboard eligibility."
      ),
      MonetisationRequirementClassification(
        MonetisationRequirement.NonTransferableTokensAndQuotas,
        RequirementClass.EvenChessSpecific,
        "Tokens and quotas are account-owned and non-transferable."
      ),
      MonetisationRequirementClassification(
        MonetisationRequirement.TokenConsumptionAndRefunds,
        RequirementClass.EvenChessSpecific,
        "Game tokens consume only when valid games start and reach meaningful play, with required refund/non-consumption cases."
      ),
      MonetisationRequirementClassification(
        MonetisationRequirement.FairnessBoundary,
        RequirementClass.EvenChessSpecific,
        "Payment, ads, tokens, and quotas cannot alter rated fairness fields."
      ),
      MonetisationRequirementClassification(
        MonetisationRequirement.MvpRiskControlsExcluded,
        RequirementClass.UnresolvedProductOwnerDecision,
        "Phone verification, device/session risk scoring, same-IP creation limits, and cluster token delays are excluded unless later approved."
      )
    )

  enum PlanTier:
    case NewAccountOnboarding
    case FreeAdSupported
    case Standard
    case Premium

  final case class BillingCadence(weeks: Int, amountAudCents: Int):
    def weeklyAmountAudCents: Int = amountAudCents / weeks

  final case class PlanPolicy(
      tier: PlanTier,
      billing: Option[BillingCadence],
      adFreeGameAccess: Boolean,
      gameTokensGranted: Int,
      matchSummaryTokens: Int,
      performanceSummaryTokens: Int,
      matchSummaryDailyLimit: Int,
      performanceSummaryDailyLimit: Int,
      reasonableUseApplies: Boolean
  )

  object Plans:
    val onboarding: PlanPolicy =
      PlanPolicy(
        tier = PlanTier.NewAccountOnboarding,
        billing = None,
        adFreeGameAccess = false,
        gameTokensGranted = 10,
        matchSummaryTokens = 3,
        performanceSummaryTokens = 1,
        matchSummaryDailyLimit = 0,
        performanceSummaryDailyLimit = 0,
        reasonableUseApplies = true
      )

    val freeAdSupported: PlanPolicy =
      PlanPolicy(
        tier = PlanTier.FreeAdSupported,
        billing = None,
        adFreeGameAccess = false,
        gameTokensGranted = 0,
        matchSummaryTokens = 0,
        performanceSummaryTokens = 0,
        matchSummaryDailyLimit = 0,
        performanceSummaryDailyLimit = 0,
        reasonableUseApplies = true
      )

    val standard: PlanPolicy =
      PlanPolicy(
        tier = PlanTier.Standard,
        billing = Some(BillingCadence(weeks = 4, amountAudCents = 1000)),
        adFreeGameAccess = true,
        gameTokensGranted = 0,
        matchSummaryTokens = 0,
        performanceSummaryTokens = 0,
        matchSummaryDailyLimit = 0,
        performanceSummaryDailyLimit = 0,
        reasonableUseApplies = true
      )

    val premium: PlanPolicy =
      PlanPolicy(
        tier = PlanTier.Premium,
        billing = Some(BillingCadence(weeks = 4, amountAudCents = 1600)),
        adFreeGameAccess = true,
        gameTokensGranted = 0,
        matchSummaryTokens = 0,
        performanceSummaryTokens = 0,
        matchSummaryDailyLimit = 10,
        performanceSummaryDailyLimit = 1,
        reasonableUseApplies = true
      )

    val all: Map[PlanTier, PlanPolicy] =
      List(onboarding, freeAdSupported, standard, premium).map(plan => plan.tier -> plan).toMap

  enum AccountLifecycle:
    case Active
    case Closed

  final case class AccountIdentity(
      accountId: String,
      email: String,
      username: String,
      lifecycle: AccountLifecycle,
      emailWasPreviouslyClosed: Boolean
  ):
    def hasRequiredIdentity: Boolean =
      accountId.nonEmpty && email.nonEmpty && username.nonEmpty

    def active: Boolean = lifecycle == AccountLifecycle.Active

  object AccountRules:
    def sameEmailHasActiveAccount(email: String, accounts: List[AccountIdentity]): Boolean =
      accounts.exists(account => account.email == email && account.active)

    def canCreateActiveAccount(email: String, accounts: List[AccountIdentity]): Boolean =
      email.nonEmpty && !sameEmailHasActiveAccount(email, accounts)

    def onboardingEligible(account: AccountIdentity): Boolean =
      account.active && account.hasRequiredIdentity && !account.emailWasPreviouslyClosed

  final case class AccountEntitlements(
      accountId: String,
      ecrAccountKey: String,
      gameTokens: Int,
      earnedAdGameTokens: Int,
      matchSummaryTokens: Int,
      performanceSummaryTokens: Int,
      subscription: Option[PlanTier],
      leaderboardEligible: Boolean
  ):
    def separateFrom(other: AccountEntitlements): Boolean =
      accountId != other.accountId &&
        ecrAccountKey != other.ecrAccountKey

    def ownsTokensAndQuotas: Boolean = accountId.nonEmpty

  object EntitlementTransfers:
    def canTransferTokens(from: AccountEntitlements, to: AccountEntitlements): Boolean = false

    def canTransferQuotas(from: AccountEntitlements, to: AccountEntitlements): Boolean = false

  final case class RewardedAdBank(earnedGameTokens: Int):
    val cap: Int = 3

    def canWatchRewardedAd: Boolean = earnedGameTokens < cap

    def grantCompletedAdToken: RewardedAdBank =
      if canWatchRewardedAd then copy(earnedGameTokens = earnedGameTokens + 1) else this

  enum GameTokenEvent:
    case FailedQueue
    case ValidGameStartedMeaningfulPlay
    case OpponentAbortBeforeMeaningfulPlay
    case TokenHolderAbortAfterAcceptingOrMeaningfulPlay
    case PlatformOutageAbort

  final case class TokenConsumptionDecision(
      consumesToken: Boolean,
      refundsToken: Boolean,
      triggersCooldown: Boolean
  )

  object TokenConsumption:
    def decide(event: GameTokenEvent): TokenConsumptionDecision =
      event match
        case GameTokenEvent.FailedQueue =>
          TokenConsumptionDecision(consumesToken = false, refundsToken = false, triggersCooldown = false)
        case GameTokenEvent.ValidGameStartedMeaningfulPlay =>
          TokenConsumptionDecision(consumesToken = true, refundsToken = false, triggersCooldown = false)
        case GameTokenEvent.OpponentAbortBeforeMeaningfulPlay =>
          TokenConsumptionDecision(consumesToken = false, refundsToken = true, triggersCooldown = false)
        case GameTokenEvent.TokenHolderAbortAfterAcceptingOrMeaningfulPlay =>
          TokenConsumptionDecision(consumesToken = true, refundsToken = false, triggersCooldown = true)
        case GameTokenEvent.PlatformOutageAbort =>
          TokenConsumptionDecision(consumesToken = false, refundsToken = true, triggersCooldown = false)

  final case class SummaryAccessDecision(
      allowed: Boolean,
      consumesQuota: Boolean,
      reason: String
  )

  object SummaryAccess:
    def decide(
        plan: PlanTier,
        summaryType: SummaryType,
        remainingFreeTokens: Int,
        remainingPremiumDailyQuota: Int
    ): SummaryAccessDecision =
      if remainingFreeTokens > 0 then SummaryAccessDecision(allowed = true, consumesQuota = true, "free_token")
      else
        (plan, summaryType) match
          case (PlanTier.Premium, SummaryType.Match) if remainingPremiumDailyQuota > 0 =>
            SummaryAccessDecision(allowed = true, consumesQuota = true, "premium_match_daily")
          case (PlanTier.Premium, SummaryType.Performance) if remainingPremiumDailyQuota > 0 =>
            SummaryAccessDecision(allowed = true, consumesQuota = true, "premium_performance_daily")
          case _ =>
            SummaryAccessDecision(allowed = false, consumesQuota = false, "quota_unavailable")

  final case class FairnessSnapshot(
      setLevel: Level,
      usedLevel: Level,
      assistanceLoadPolicyVersion: String,
      usedOffsetPolicyVersion: String,
      ecrPolicyVersion: String,
      matchmakingPolicyVersion: String,
      stockfishProfileKey: String,
      aiExactnessClass: ExactnessClass,
      targetIsolationKey: String,
      liveCoachingStrengthKey: String
  )

  object FairnessBoundary:
    val subscriptionsAdsTokensQuotasMayBypassFairness = false
    val premiumMayProvideStrongerLiveHelp = false

    def unchangedByMonetisation(before: FairnessSnapshot, after: FairnessSnapshot): Boolean =
      before == after

  final case class LeaderboardEligibility(
      completedGames: Int,
      ratingDeviation: Double,
      abuseClear: Boolean
  ):
    def eligible(minGames: Int, maxRatingDeviation: Double): Boolean =
      completedGames >= minGames && ratingDeviation <= maxRatingDeviation && abuseClear

  object MvpExcludedRiskControls:
    val phoneVerificationRequired = false
    val deviceSessionRiskScoringRequired = false
    val sameIpCreationLimitsRequired = false
    val highRiskClusterTokenDelaysRequired = false
