package lila.evenchess

import AiCoachPolicy.SummaryType
import MarketingAttributionFunnel.FunnelEventCatalog
import MarketingFunnelPolicy.AttributionEvent
import MonetisationPolicy.{ BillingCadence, GameTokenEvent, PlanPolicy, PlanTier, Plans, TokenConsumption }
import PlaySearchIntegration.TokenSnapshot
import ProductInvariants.RequirementClass
import SubscriptionTokensAds.{ EntitlementState, RewardedAdRules }

object AccountMonetisationUi:

  enum PhaseJRequirement:
    case LichessAccountFoundation
    case AccountEntitlementDashboard
    case PlayStartUsesAccountTokens
    case PlanFeatureDescriptions
    case RewardedAdGrantState
    case GameTokenSettlementCopy
    case SummaryQuotaState
    case StableFunnelEvents
    case FairnessBoundary
    case PaymentProviderSideEffectsExcluded

  final case class PhaseJRequirementClassification(
      requirement: PhaseJRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseJRequirementClassifications:
    val all: List[PhaseJRequirementClassification] = List(
      PhaseJRequirementClassification(
        PhaseJRequirement.LichessAccountFoundation,
        RequirementClass.LichessProvided,
        "Use Lichess login/session identity for the account surface; do not rebuild accounts."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.AccountEntitlementDashboard,
        RequirementClass.EvenChessSpecific,
        "Expose EvenChess tokens, earned ad tokens, summary quotas, and subscription state from a namespaced entitlement model."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.PlayStartUsesAccountTokens,
        RequirementClass.AdaptedToLichessFork,
        "The EvenChess play route reads token eligibility from the account entitlement contract before creating search intent."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.PlanFeatureDescriptions,
        RequirementClass.EvenChessSpecific,
        "Free, Standard, and Premium descriptions explain access and review quotas without promising stronger live help."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.RewardedAdGrantState,
        RequirementClass.EvenChessSpecific,
        "Rewarded ad state shows cap, availability, and provider-verification status while grants remain server-verified."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.GameTokenSettlementCopy,
        RequirementClass.EvenChessSpecific,
        "User-facing settlement states mirror server token consumption/refund policy."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.SummaryQuotaState,
        RequirementClass.EvenChessSpecific,
        "Summary quotas expose remaining match/performance access and failed-summary non-consumption."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.StableFunnelEvents,
        RequirementClass.AdaptedToLichessFork,
        "Account and plan surfaces use the existing EvenChess stable funnel event names for future adapters."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.FairnessBoundary,
        RequirementClass.EvenChessSpecific,
        "Payment, ads, tokens, subscriptions, and campaign state cannot mutate ECR or live coaching strength."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.PaymentProviderSideEffectsExcluded,
        RequirementClass.UnresolvedProductOwnerDecision,
        "Real checkout and rewarded-ad provider callbacks require provider credentials and server verification before activation."
      )
    )

  object Routes:
    val account = "/evenchess/account"
    val plans = s"$account#plans"
    val rewardedAds = s"$account#rewarded-ads"
    val summaries = s"$account#summaries"

  final case class TopBarGameTokenBalance(
      accountId: String,
      visibleGameTokens: Int,
      href: String,
      source: String,
      subscriptionActive: Boolean,
      fairnessAffecting: Boolean
  ):
    def displayCount: String = if subscriptionActive then "Plan" else visibleGameTokens.toString
    def displayLabel: String = if subscriptionActive then "access" else "tokens"
    def title: String =
      if subscriptionActive then "EvenChess plan access; tokens do not change live help strength."
      else s"$visibleGameTokens EvenChess game tokens; open rewarded ads and token balance."
    def ariaLabel: String = s"EvenChess $displayCount $displayLabel"

    def valid: Boolean =
      accountId.nonEmpty &&
        visibleGameTokens >= 0 &&
        href == Routes.rewardedAds &&
        source == EntitlementSource.tokenSnapshotSource &&
        !fairnessAffecting &&
        !title.toLowerCase.contains("stronger live help")

  object TopBarGameTokenBalance:
    def fromState(state: EntitlementState): TopBarGameTokenBalance =
      val snapshot = EntitlementSource.toPlayTokenSnapshot(state)
      TopBarGameTokenBalance(
        accountId = state.accountId,
        visibleGameTokens = snapshot.availableGameStarts,
        href = Routes.rewardedAds,
        source = snapshot.source,
        subscriptionActive = snapshot.subscriptionActive,
        fairnessAffecting = false
      )

    def forLichessUser(accountId: String, now: Long): TopBarGameTokenBalance =
      fromState(EntitlementSource.onboardingForLichessUser(accountId, now))

  object EntitlementSource:
    val tokenSnapshotSource = "evenchess-account-entitlements-v1"
    val checkoutProviderVerified = false
    val rewardedAdProviderVerified = false
    val failedSummaryConsumesQuota = false

    def onboardingForLichessUser(accountId: String, now: Long): EntitlementState =
      EntitlementState.empty(accountId, s"evenchess-ecr-$accountId", now).copy(
        gameTokens = Plans.onboarding.gameTokensGranted,
        matchSummaryTokens = Plans.onboarding.matchSummaryTokens,
        performanceSummaryTokens = Plans.onboarding.performanceSummaryTokens,
        leaderboardEligible = true
      )

    def toPlayTokenSnapshot(state: EntitlementState, abuseClear: Boolean = true): TokenSnapshot =
      TokenSnapshot(
        plan = state.plan,
        gameTokens = state.gameTokens,
        earnedAdGameTokens = state.earnedAdGameTokens,
        subscriptionActive = state.subscribed,
        abuseClear = abuseClear,
        source = tokenSnapshotSource
      )

  final case class PriceText(
      total: String,
      weekly: Option[String]
  ):
    def valid: Boolean = total.nonEmpty && weekly.forall(_.nonEmpty)

  object PriceText:
    val included: PriceText = PriceText("Included", None)

    def fromBilling(billing: BillingCadence): PriceText =
      PriceText(
        total = s"${formatAud(billing.amountAudCents)} / ${billing.weeks} weeks",
        weekly = Some(s"${formatAud(billing.weeklyAmountAudCents)}/week")
      )

    private def formatAud(cents: Int): String =
      if cents % 100 == 0 then s"$$${cents / 100} AUD"
      else f"$$${cents / 100.0}%.2f AUD"

  final case class PlanCard(
      tier: PlanTier,
      title: String,
      price: PriceText,
      features: List[String],
      current: Boolean,
      checkoutHref: Option[String],
      checkoutEventName: Option[String],
      serverVerificationRequired: Boolean,
      fairnessAffecting: Boolean,
      liveStrengthUpgrade: Boolean
  ):
    def valid: Boolean =
      title.nonEmpty &&
        price.valid &&
        features.nonEmpty &&
        checkoutEventName.forall(_ == FunnelEventCatalog.stableName(AttributionEvent.CheckoutStart)) &&
        !fairnessAffecting &&
        !liveStrengthUpgrade

  object PlanCards:
    def all(current: PlanTier): List[PlanCard] =
      List(
        freeCard(current),
        paidCard(
          Plans.standard,
          "Standard",
          List(
            "Ad-free game access under reasonable-use controls.",
            "Uses the same Set Level, ECR, Stockfish profile, AI exactness, and live coaching strength as free accounts."
          ),
          current
        ),
        paidCard(
          Plans.premium,
          "Premium",
          List(
            "Standard access plus 10 match summaries per day.",
            "Includes 1 performance summary per day.",
            "Adds review quantity and convenience only; live help is not stronger."
          ),
          current
        )
      )

    private def freeCard(current: PlanTier): PlanCard =
      PlanCard(
        tier = PlanTier.FreeAdSupported,
        title = "Free",
        price = PriceText.included,
        features = List(
          "Starter accounts receive 10 game tokens, 3 match summaries, and 1 performance summary.",
          s"Rewarded ads can earn game tokens up to a cap of ${RewardedAdRules.earnedTokenCap}.",
          "Tokens are account-scoped and non-transferable."
        ),
        current = current == PlanTier.FreeAdSupported || current == PlanTier.NewAccountOnboarding,
        checkoutHref = None,
        checkoutEventName = None,
        serverVerificationRequired = false,
        fairnessAffecting = false,
        liveStrengthUpgrade = false
      )

    private def paidCard(policy: PlanPolicy, title: String, features: List[String], current: PlanTier): PlanCard =
      PlanCard(
        tier = policy.tier,
        title = title,
        price = policy.billing.map(PriceText.fromBilling).getOrElse(PriceText.included),
        features = features,
        current = current == policy.tier,
        checkoutHref = Option.when(EntitlementSource.checkoutProviderVerified)(
          s"${Routes.account}?checkout=${tierKey(policy.tier)}#plans"
        ),
        checkoutEventName = Some(FunnelEventCatalog.stableName(AttributionEvent.CheckoutStart)),
        serverVerificationRequired = true,
        fairnessAffecting = false,
        liveStrengthUpgrade = false
      )

    def tierKey(tier: PlanTier): String =
      tier match
        case PlanTier.NewAccountOnboarding => "onboarding"
        case PlanTier.FreeAdSupported      => "free"
        case PlanTier.Standard             => "standard"
        case PlanTier.Premium              => "premium"

  final case class RewardedAdStatus(
      earnedTokens: Int,
      cap: Int,
      canRequestAd: Boolean,
      providerVerified: Boolean,
      grantAmount: Int,
      eventName: String,
      stateText: String,
      fairnessAffecting: Boolean
  ):
    def valid: Boolean =
      earnedTokens >= 0 &&
        earnedTokens <= cap &&
        cap == RewardedAdRules.earnedTokenCap &&
        grantAmount == RewardedAdRules.grantAmount &&
        eventName == FunnelEventCatalog.stableName(AttributionEvent.RewardedAdComplete) &&
        !fairnessAffecting

  object RewardedAdStatus:
    def fromState(state: EntitlementState): RewardedAdStatus =
      val canRequest = state.canWatchRewardedAd
      RewardedAdStatus(
        earnedTokens = state.earnedAdGameTokens,
        cap = RewardedAdRules.earnedTokenCap,
        canRequestAd = canRequest,
        providerVerified = EntitlementSource.rewardedAdProviderVerified,
        grantAmount = RewardedAdRules.grantAmount,
        eventName = FunnelEventCatalog.stableName(AttributionEvent.RewardedAdComplete),
        stateText =
          if !canRequest then "Your rewarded token bank is full."
          else if !EntitlementSource.rewardedAdProviderVerified then "Rewarded token earning is temporarily unavailable."
          else "Complete a reward to earn 1 game token.",
        fairnessAffecting = false
      )

  final case class SummaryQuotaStatus(
      matchRemaining: Int,
      performanceRemaining: Int,
      failedSummaryConsumesQuota: Boolean,
      matchEventName: String,
      performanceEventName: String,
      fairnessAffecting: Boolean
  ):
    def totalRemaining: Int = matchRemaining + performanceRemaining

    def valid: Boolean =
      matchRemaining >= 0 &&
        performanceRemaining >= 0 &&
        !failedSummaryConsumesQuota &&
        matchEventName == FunnelEventCatalog.stableName(AttributionEvent.MatchSummaryView) &&
        performanceEventName == FunnelEventCatalog.stableName(AttributionEvent.PerformanceSummaryView) &&
        !fairnessAffecting

  object SummaryQuotaStatus:
    def fromState(state: EntitlementState): SummaryQuotaStatus =
      SummaryQuotaStatus(
        matchRemaining = state.matchSummaryTokens + state.premiumMatchSummaryDailyRemaining,
        performanceRemaining = state.performanceSummaryTokens + state.premiumPerformanceSummaryDailyRemaining,
        failedSummaryConsumesQuota = EntitlementSource.failedSummaryConsumesQuota,
        matchEventName = FunnelEventCatalog.stableName(AttributionEvent.MatchSummaryView),
        performanceEventName = FunnelEventCatalog.stableName(AttributionEvent.PerformanceSummaryView),
        fairnessAffecting = false
      )

    def visibleReason(summaryType: SummaryType, status: SummaryQuotaStatus): String =
      summaryType match
        case SummaryType.Match if status.matchRemaining > 0             => "match_summary_available"
        case SummaryType.Performance if status.performanceRemaining > 0 => "performance_summary_available"
        case _                                                          => "summary_quota_unavailable"

  final case class SettlementRuleRow(
      event: GameTokenEvent,
      label: String,
      consumesToken: Boolean,
      refundsToken: Boolean,
      triggersCooldown: Boolean,
      description: String
  ):
    def valid: Boolean =
      label.nonEmpty &&
        description.nonEmpty &&
        TokenConsumption.decide(event).consumesToken == consumesToken &&
        TokenConsumption.decide(event).refundsToken == refundsToken &&
        TokenConsumption.decide(event).triggersCooldown == triggersCooldown

  object SettlementRules:
    val all: List[SettlementRuleRow] = List(
      row(GameTokenEvent.FailedQueue, "No game found", "No token is used if a search does not create a game."),
      row(GameTokenEvent.ValidGameStartedMeaningfulPlay, "Game played", "A token is used once a valid game reaches meaningful play."),
      row(GameTokenEvent.OpponentAbortBeforeMeaningfulPlay, "Opponent leaves early", "Your reserved token is returned if the opponent leaves before meaningful play."),
      row(GameTokenEvent.TokenHolderAbortAfterAcceptingOrMeaningfulPlay, "You leave after accepting", "A token may be used, and a short cooldown can apply after accepting or meaningful play."),
      row(GameTokenEvent.PlatformOutageAbort, "Service interruption", "Your reserved token is returned if EvenChess cannot complete the game start.")
    )

    private def row(event: GameTokenEvent, label: String, copy: String): SettlementRuleRow =
      val decision = TokenConsumption.decide(event)
      SettlementRuleRow(
        event = event,
        label = label,
        consumesToken = decision.consumesToken,
        refundsToken = decision.refundsToken,
        triggersCooldown = decision.triggersCooldown,
        description = copy
      )

  final case class FunnelEventNames(
      planViewed: String,
      checkoutStart: String,
      purchase: String,
      rewardedAdComplete: String,
      matchSummaryView: String,
      performanceSummaryView: String
  ):
    def valid: Boolean =
      planViewed == FunnelEventCatalog.stableName(AttributionEvent.ViewPricing) &&
        checkoutStart == FunnelEventCatalog.stableName(AttributionEvent.CheckoutStart) &&
        purchase == FunnelEventCatalog.stableName(AttributionEvent.Purchase) &&
        rewardedAdComplete == FunnelEventCatalog.stableName(AttributionEvent.RewardedAdComplete) &&
        matchSummaryView == FunnelEventCatalog.stableName(AttributionEvent.MatchSummaryView) &&
        performanceSummaryView == FunnelEventCatalog.stableName(AttributionEvent.PerformanceSummaryView)

  object FunnelEventNames:
    val current: FunnelEventNames =
      FunnelEventNames(
        planViewed = FunnelEventCatalog.stableName(AttributionEvent.ViewPricing),
        checkoutStart = FunnelEventCatalog.stableName(AttributionEvent.CheckoutStart),
        purchase = FunnelEventCatalog.stableName(AttributionEvent.Purchase),
        rewardedAdComplete = FunnelEventCatalog.stableName(AttributionEvent.RewardedAdComplete),
        matchSummaryView = FunnelEventCatalog.stableName(AttributionEvent.MatchSummaryView),
        performanceSummaryView = FunnelEventCatalog.stableName(AttributionEvent.PerformanceSummaryView)
      )

  final case class AccountDashboard(
      state: EntitlementState,
      tokenSnapshot: TokenSnapshot,
      planCards: List[PlanCard],
      rewardedAdStatus: RewardedAdStatus,
      summaryQuotaStatus: SummaryQuotaStatus,
      settlementRules: List[SettlementRuleRow],
      funnelEvents: FunnelEventNames,
      fairnessCopy: String
  ):
    def valid: Boolean =
      state.valid &&
        tokenSnapshot.fairnessNeutral &&
        planCards.nonEmpty &&
        planCards.forall(_.valid) &&
        planCards.count(_.current) == 1 &&
        rewardedAdStatus.valid &&
        summaryQuotaStatus.valid &&
        settlementRules.nonEmpty &&
        settlementRules.forall(_.valid) &&
        funnelEvents.valid &&
        fairnessCopy.nonEmpty &&
        fairnessCopy.contains("never change live rated help strength")

  object AccountDashboard:
    val fairnessCopy =
      "Plans, tokens, rewards, and summaries never change live rated help strength. Every rated game still follows its disclosed Set Level and normal EvenChess rating rules."

    def fromState(state: EntitlementState): AccountDashboard =
      AccountDashboard(
        state = state,
        tokenSnapshot = EntitlementSource.toPlayTokenSnapshot(state),
        planCards = PlanCards.all(state.plan),
        rewardedAdStatus = RewardedAdStatus.fromState(state),
        summaryQuotaStatus = SummaryQuotaStatus.fromState(state),
        settlementRules = SettlementRules.all,
        funnelEvents = FunnelEventNames.current,
        fairnessCopy = fairnessCopy
      )

    def forLichessUser(accountId: String, now: Long): AccountDashboard =
      fromState(EntitlementSource.onboardingForLichessUser(accountId, now))
