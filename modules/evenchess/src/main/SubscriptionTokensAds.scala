package lila.evenchess

import AbuseTrustControls.{ ExploitRegister, ExploitType, MitigationAction }
import AiCoachPolicy.SummaryType
import DataModelsAndSeams.{ IntegrationSeam, IntegrationSeamRegistry, TokenLedgerEntryModel, TokenLedgerEvent }
import MarketingFunnelPolicy.{ AdminMarketingControls, PaidAcquisitionHealth }
import MonetisationPolicy.{ AccountIdentity, AccountRules, FairnessSnapshot, GameTokenEvent, PlanPolicy, PlanTier, Plans }
import ProductInvariants.RequirementClass

object SubscriptionTokensAds:

  enum PhasePRequirement:
    case LichessAccountFoundation
    case AccountStateIncludesMonetisationEntitlements
    case TopBarAndAccountTokenVisibility
    case GameTokenMeaningfulPlaySettlement
    case SubscriptionBypassesAdTokenLimitsOnly
    case ReviewAndCustomAnalysisTokenGates
    case LiveHistoryNeedsNoCustomTokens
    case SavedGamesRollingAndPaidPersistence
    case SavedGameCountsConfigurable
    case MonetisationFairnessBoundary

  final case class PhasePRequirementClassification(
      requirement: PhasePRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhasePRequirementClassifications:
    val all: List[PhasePRequirementClassification] = List(
      PhasePRequirementClassification(
        PhasePRequirement.LichessAccountFoundation,
        RequirementClass.LichessProvided,
        "Use Lichess account/session foundations; EvenChess adds namespaced entitlement records only."
      ),
      PhasePRequirementClassification(
        PhasePRequirement.AccountStateIncludesMonetisationEntitlements,
        RequirementClass.EvenChessSpecific,
        "Account entitlement state includes game tokens, ad tokens, subscriptions, review/custom-analysis tokens, saved-game ids, and ECR identity."
      ),
      PhasePRequirementClassification(
        PhasePRequirement.TopBarAndAccountTokenVisibility,
        RequirementClass.AdaptedToLichessFork,
        "Top-bar/account-native surfaces may display token and plan state without implying stronger live help."
      ),
      PhasePRequirementClassification(
        PhasePRequirement.GameTokenMeaningfulPlaySettlement,
        RequirementClass.EvenChessSpecific,
        "Game-token settlement follows server-side meaningful-play outcomes and refund rules."
      ),
      PhasePRequirementClassification(
        PhasePRequirement.SubscriptionBypassesAdTokenLimitsOnly,
        RequirementClass.EvenChessSpecific,
        "Standard and Premium can bypass ad-supported game-token limits but never alter rated fairness."
      ),
      PhasePRequirementClassification(
        PhasePRequirement.ReviewAndCustomAnalysisTokenGates,
        RequirementClass.EvenChessSpecific,
        "Custom ECE analysis and full-game review consume account-scoped review tokens only when the review plan requires them."
      ),
      PhasePRequirementClassification(
        PhasePRequirement.LiveHistoryNeedsNoCustomTokens,
        RequirementClass.EvenChessSpecific,
        "Saved live ECE outputs are game history and do not consume custom-analysis tokens when replayed."
      ),
      PhasePRequirementClassification(
        PhasePRequirement.SavedGamesRollingAndPaidPersistence,
        RequirementClass.EvenChessSpecific,
        "Recent games may roll through retention while paid-tier saved games remain saved after downgrade."
      ),
      PhasePRequirementClassification(
        PhasePRequirement.SavedGameCountsConfigurable,
        RequirementClass.UnresolvedProductOwnerDecision,
        "Exact free/paid saved-game counts are configurable product values, not hard-coded fairness rules."
      ),
      PhasePRequirementClassification(
        PhasePRequirement.MonetisationFairnessBoundary,
        RequirementClass.EvenChessSpecific,
        "Payments, ads, tokens, quotas, saved games, and subscriptions cannot mutate ECR, Used Level, Assistance Load, Used Offset, or live coaching strength."
      )
    )

  enum PhaseIRequirement:
    case LichessAccountFoundation
    case AccountSubscriptionStorageSeam
    case OnboardingGrant
    case StandardPremiumPlans
    case SummaryQuotas
    case RewardedAdGrant
    case GameTokenSettlement
    case AccountScopedEntitlements
    case FairnessBoundary
    case CampaignAndAbuseControls
    case MvpInvasiveControlsExcluded

  final case class PhaseIRequirementClassification(
      requirement: PhaseIRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseIRequirementClassifications:
    val all: List[PhaseIRequirementClassification] = List(
      PhaseIRequirementClassification(
        PhaseIRequirement.LichessAccountFoundation,
        RequirementClass.LichessProvided,
        "Use Lichess account/session identity as the base; do not rebuild account basics."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.AccountSubscriptionStorageSeam,
        RequirementClass.AdaptedToLichessFork,
        "Add a narrow EvenChess account extension/ledger seam later instead of broad core account fields."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.OnboardingGrant,
        RequirementClass.EvenChessSpecific,
        "Grant eligible new EvenChess accounts 10 game tokens, 3 match summaries, and 1 performance summary."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.StandardPremiumPlans,
        RequirementClass.EvenChessSpecific,
        "Model paid access and convenience only; plans never change live rated coaching strength."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.SummaryQuotas,
        RequirementClass.EvenChessSpecific,
        "Account-scoped summary quotas unlock review volume, not live help."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.RewardedAdGrant,
        RequirementClass.EvenChessSpecific,
        "Rewarded ad completion may grant one capped game token to free accounts."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.GameTokenSettlement,
        RequirementClass.EvenChessSpecific,
        "Consume/refund game tokens from server-side meaningful-play outcomes."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.AccountScopedEntitlements,
        RequirementClass.EvenChessSpecific,
        "Keep ECR, tokens, subscriptions, quotas, and leaderboard eligibility separate per account."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.FairnessBoundary,
        RequirementClass.EvenChessSpecific,
        "Subscriptions, ads, tokens, quotas, and campaign config cannot alter rated fairness fields."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.CampaignAndAbuseControls,
        RequirementClass.AdaptedToLichessFork,
        "Use backend marketing/admin and abuse signals as gates around grants without changing fairness."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.MvpInvasiveControlsExcluded,
        RequirementClass.UnresolvedProductOwnerDecision,
        "Phone/device/IP/cluster risk controls remain excluded until product-owner and privacy approval."
      )
    )

  enum TokenBucket:
    case OnboardingGameToken
    case RewardedAdGameToken

  enum EntitlementDecision:
    case Granted
    case Allowed
    case Blocked
    case Refunded
    case NoOp

  final case class EntitlementState(
      accountId: String,
      ecrAccountKey: String,
      plan: PlanTier,
      gameTokens: Int,
      earnedAdGameTokens: Int,
      matchSummaryTokens: Int,
      performanceSummaryTokens: Int,
      premiumMatchSummaryDailyRemaining: Int,
      premiumPerformanceSummaryDailyRemaining: Int,
      leaderboardEligible: Boolean,
      updatedAt: Long,
      schemaVersion: String,
      savedGameIds: List[String] = Nil,
      customAnalysisTokens: Int = 0,
      matchReviewTokens: Int = 0,
      fullAnalysisTokens: Int = 0
  ):
    def valid: Boolean =
      accountId.nonEmpty &&
        ecrAccountKey.nonEmpty &&
        gameTokens >= 0 &&
        earnedAdGameTokens >= 0 &&
        earnedAdGameTokens <= RewardedAdRules.earnedTokenCap &&
        matchSummaryTokens >= 0 &&
        performanceSummaryTokens >= 0 &&
        premiumMatchSummaryDailyRemaining >= 0 &&
        premiumPerformanceSummaryDailyRemaining >= 0 &&
        updatedAt > 0 &&
        schemaVersion.nonEmpty &&
        savedGameIds.forall(_.nonEmpty) &&
        savedGameIds.distinct == savedGameIds &&
        customAnalysisTokens >= 0 &&
        matchReviewTokens >= 0 &&
        fullAnalysisTokens >= 0

    def totalGameTokens: Int = gameTokens + earnedAdGameTokens

    def totalReviewTokens: Int =
      customAnalysisTokens + matchReviewTokens + fullAnalysisTokens

    def subscribed: Boolean =
      plan == PlanTier.Standard || plan == PlanTier.Premium

    def canStartAdFreeGame: Boolean =
      Plans.all.get(plan).exists(_.adFreeGameAccess)

    def canWatchRewardedAd: Boolean =
      plan == PlanTier.FreeAdSupported && earnedAdGameTokens < RewardedAdRules.earnedTokenCap

    def separateFrom(other: EntitlementState): Boolean =
      accountId != other.accountId && ecrAccountKey != other.ecrAccountKey

    def withPlan(planPolicy: PlanPolicy, now: Long): EntitlementState =
      copy(
        plan = planPolicy.tier,
        premiumMatchSummaryDailyRemaining = planPolicy.matchSummaryDailyLimit,
        premiumPerformanceSummaryDailyRemaining = planPolicy.performanceSummaryDailyLimit,
        updatedAt = now
      )

  object EntitlementState:
    val schemaVersion = "evenchess-entitlement-v1"

    def empty(accountId: String, ecrAccountKey: String, now: Long): EntitlementState =
      EntitlementState(
        accountId = accountId,
        ecrAccountKey = ecrAccountKey,
        plan = PlanTier.FreeAdSupported,
        gameTokens = 0,
        earnedAdGameTokens = 0,
        matchSummaryTokens = 0,
        performanceSummaryTokens = 0,
        premiumMatchSummaryDailyRemaining = 0,
        premiumPerformanceSummaryDailyRemaining = 0,
        leaderboardEligible = false,
        updatedAt = now,
        schemaVersion = schemaVersion
      )

    def onboarding(account: AccountIdentity, now: Long): EntitlementState =
      empty(account.accountId, s"evenchess-ecr-${account.accountId}", now).copy(
        gameTokens = Plans.onboarding.gameTokensGranted,
        matchSummaryTokens = Plans.onboarding.matchSummaryTokens,
        performanceSummaryTokens = Plans.onboarding.performanceSummaryTokens
      )

  object EntitlementTransfers:
    def canTransferTokens(from: EntitlementState, to: EntitlementState): Boolean = false

    def canTransferQuotas(from: EntitlementState, to: EntitlementState): Boolean = false

  object Ledger:
    val schemaVersion = "evenchess-token-ledger-v1"

    def token(
        prefix: String,
        accountId: String,
        event: TokenLedgerEvent,
        amount: Int,
        reason: String,
        gameId: Option[String],
        now: Long
    ): TokenLedgerEntryModel =
      TokenLedgerEntryModel(
        entryId = s"$prefix-$accountId-$now",
        playerId = accountId,
        event = event,
        amount = amount,
        reason = reason,
        gameId = gameId,
        schemaVersion = schemaVersion
      )

  final case class OnboardingGrantResult(
      decision: EntitlementDecision,
      state: Option[EntitlementState],
      ledgerEntries: List[TokenLedgerEntryModel],
      reason: String
  ):
    def allowed: Boolean = decision == EntitlementDecision.Granted

  object OnboardingGrantService:
    def grant(account: AccountIdentity, existingAccounts: List[AccountIdentity], now: Long): OnboardingGrantResult =
      val sameEmailActiveElsewhere =
        existingAccounts.exists(existing => existing.accountId != account.accountId && existing.email == account.email && existing.active)

      if !account.hasRequiredIdentity then denied("missing_identity")
      else if !account.active then denied("account_not_active")
      else if sameEmailActiveElsewhere then denied("duplicate_active_email")
      else if !AccountRules.onboardingEligible(account) then denied("reused_closed_account_email")
      else
        val state = EntitlementState.onboarding(account, now)
        OnboardingGrantResult(
          decision = EntitlementDecision.Granted,
          state = Some(state),
          ledgerEntries = List(
            Ledger.token(
              prefix = "onboarding-game-tokens",
              accountId = account.accountId,
              event = TokenLedgerEvent.OnboardingGranted,
              amount = Plans.onboarding.gameTokensGranted,
              reason = "new_account_onboarding",
              gameId = None,
              now = now
            )
          ),
          reason = "eligible_new_account"
        )

    private def denied(reason: String): OnboardingGrantResult =
      OnboardingGrantResult(EntitlementDecision.Blocked, None, Nil, reason)

  final case class PlanActivationResult(
      decision: EntitlementDecision,
      state: EntitlementState,
      planPolicy: Option[PlanPolicy],
      billingAmountAudCents: Option[Int],
      reason: String
  ):
    def allowed: Boolean = decision == EntitlementDecision.Allowed

  object SubscriptionPlanService:
    def activate(
        state: EntitlementState,
        tier: PlanTier,
        controls: AdminMarketingControls,
        now: Long
    ): PlanActivationResult =
      if tier == PlanTier.NewAccountOnboarding then blocked(state, "onboarding_is_not_subscription")
      else if tier == PlanTier.Standard && !controls.standardPlanEnabled then blocked(state, "standard_plan_disabled")
      else if tier == PlanTier.Premium && !controls.premiumPlanEnabled then blocked(state, "premium_plan_disabled")
      else
        Plans.all.get(tier) match
          case None => blocked(state, "unknown_plan")
          case Some(planPolicy) =>
            val next = state.withPlan(planPolicy, now)
            PlanActivationResult(
              decision = EntitlementDecision.Allowed,
              state = next,
              planPolicy = Some(planPolicy),
              billingAmountAudCents = planPolicy.billing.map(_.amountAudCents),
              reason = "plan_active"
            )

    private def blocked(state: EntitlementState, reason: String): PlanActivationResult =
      PlanActivationResult(EntitlementDecision.Blocked, state, None, None, reason)

  final case class TokenAbuseState(
      adTokenFarmingCooldownActive: Boolean,
      auditId: Option[String]
  ):
    def blocksRewardedAds: Boolean = adTokenFarmingCooldownActive

    def auditedSuppression: Boolean =
      !adTokenFarmingCooldownActive || auditId.exists(_.nonEmpty)

  object TokenAbuseState:
    val clear: TokenAbuseState = TokenAbuseState(adTokenFarmingCooldownActive = false, auditId = None)

  object RewardedAdRules:
    val earnedTokenCap = 3
    val grantAmount = 1

    val adTokenFarmingMitigations: Set[MitigationAction] =
      ExploitRegister.byExploit(ExploitType.AdTokenFarming).mitigations

  final case class RewardedAdGrantResult(
      decision: EntitlementDecision,
      state: EntitlementState,
      ledgerEntries: List[TokenLedgerEntryModel],
      reason: String,
      auditId: Option[String]
  ):
    def granted: Boolean = decision == EntitlementDecision.Granted

  object RewardedAdService:
    def completeAd(
        state: EntitlementState,
        controls: AdminMarketingControls,
        health: PaidAcquisitionHealth,
        abuse: TokenAbuseState,
        now: Long
    ): RewardedAdGrantResult =
      if state.plan != PlanTier.FreeAdSupported then blocked(state, "rewarded_ads_free_accounts_only", abuse.auditId)
      else if !controls.rewardedAdsEnabled then blocked(state, "rewarded_ads_disabled", abuse.auditId)
      else if controls.campaignPauseNotice.exists(_.nonEmpty) || health.shouldPause then blocked(state, "campaign_or_health_paused", abuse.auditId)
      else if abuse.blocksRewardedAds then blocked(state, "ad_token_farming_cooldown", abuse.auditId)
      else if !state.canWatchRewardedAd then blocked(state, "earned_ad_token_bank_full", abuse.auditId)
      else
        val next = state.copy(earnedAdGameTokens = state.earnedAdGameTokens + RewardedAdRules.grantAmount, updatedAt = now)
        RewardedAdGrantResult(
          decision = EntitlementDecision.Granted,
          state = next,
          ledgerEntries = List(
            Ledger.token(
              prefix = "rewarded-ad-token",
              accountId = state.accountId,
              event = TokenLedgerEvent.AdEarned,
              amount = RewardedAdRules.grantAmount,
              reason = "rewarded_ad_complete",
              gameId = None,
              now = now
            )
          ),
          reason = "rewarded_ad_complete",
          auditId = abuse.auditId
        )

    private def blocked(state: EntitlementState, reason: String, auditId: Option[String]): RewardedAdGrantResult =
      RewardedAdGrantResult(EntitlementDecision.Blocked, state, Nil, reason, auditId)

  final case class GameTokenSettlementRequest(
      event: GameTokenEvent,
      gameId: String,
      reservedToken: Option[TokenBucket],
      now: Long,
      freeMatchTokenWindowActive: Boolean = false
  )

  final case class GameTokenSettlementResult(
      decision: EntitlementDecision,
      state: EntitlementState,
      ledgerEntries: List[TokenLedgerEntryModel],
      reason: String,
      triggersCooldown: Boolean
  ):
    def allowed: Boolean =
      decision != EntitlementDecision.Blocked

  object GameTokenService:
    def settle(state: EntitlementState, request: GameTokenSettlementRequest): GameTokenSettlementResult =
      val policyDecision = MonetisationPolicy.TokenConsumption.decide(request.event)

      if policyDecision.refundsToken then refund(state, request, policyDecision.triggersCooldown)
      else if policyDecision.consumesToken && request.freeMatchTokenWindowActive then
        GameTokenSettlementResult(EntitlementDecision.Allowed, state.copy(updatedAt = request.now), Nil, "launch_free_token_window", triggersCooldown = false)
      else if policyDecision.consumesToken then consume(state, request, policyDecision.triggersCooldown)
      else GameTokenSettlementResult(EntitlementDecision.NoOp, state, Nil, "no_token_event", triggersCooldown = false)

    private def consume(
        state: EntitlementState,
        request: GameTokenSettlementRequest,
        cooldown: Boolean
    ): GameTokenSettlementResult =
      val ledger = Ledger.token(
        prefix = "game-token-consumed",
        accountId = state.accountId,
        event = TokenLedgerEvent.Consumed,
        amount = 1,
        reason = request.event.toString,
        gameId = Some(request.gameId),
        now = request.now
      )

      request.reservedToken match
        case Some(_) =>
          GameTokenSettlementResult(EntitlementDecision.Allowed, state.copy(updatedAt = request.now), List(ledger), "reserved_token_consumed", cooldown)
        case None if state.canStartAdFreeGame =>
          GameTokenSettlementResult(EntitlementDecision.Allowed, state.copy(updatedAt = request.now), Nil, "subscription_access", cooldown)
        case None if state.earnedAdGameTokens > 0 =>
          GameTokenSettlementResult(
            EntitlementDecision.Allowed,
            state.copy(earnedAdGameTokens = state.earnedAdGameTokens - 1, updatedAt = request.now),
            List(ledger),
            "earned_ad_token_consumed",
            cooldown
          )
        case None if state.gameTokens > 0 =>
          GameTokenSettlementResult(
            EntitlementDecision.Allowed,
            state.copy(gameTokens = state.gameTokens - 1, updatedAt = request.now),
            List(ledger),
            "game_token_consumed",
            cooldown
          )
        case None =>
          GameTokenSettlementResult(EntitlementDecision.Blocked, state, Nil, "game_token_unavailable", cooldown)

    private def refund(
        state: EntitlementState,
        request: GameTokenSettlementRequest,
        cooldown: Boolean
    ): GameTokenSettlementResult =
      request.reservedToken match
        case None =>
          GameTokenSettlementResult(EntitlementDecision.NoOp, state, Nil, "no_reserved_token_to_refund", cooldown)
        case Some(TokenBucket.RewardedAdGameToken) =>
          refunded(state.copy(earnedAdGameTokens = state.earnedAdGameTokens + 1, updatedAt = request.now), request, cooldown, "earned_ad_token_refunded")
        case Some(TokenBucket.OnboardingGameToken) =>
          refunded(state.copy(gameTokens = state.gameTokens + 1, updatedAt = request.now), request, cooldown, "game_token_refunded")

    private def refunded(
        state: EntitlementState,
        request: GameTokenSettlementRequest,
        cooldown: Boolean,
        reason: String
    ): GameTokenSettlementResult =
      GameTokenSettlementResult(
        EntitlementDecision.Refunded,
        state,
        List(
          Ledger.token(
            prefix = "game-token-refunded",
            accountId = state.accountId,
            event = TokenLedgerEvent.Refunded,
            amount = 1,
            reason = request.event.toString,
            gameId = Some(request.gameId),
            now = request.now
          )
        ),
        reason,
        cooldown
      )

  final case class SummaryQuotaResult(
      decision: EntitlementDecision,
      state: EntitlementState,
      reason: String
  ):
    def allowed: Boolean = decision == EntitlementDecision.Allowed

  object SummaryQuotaService:
    def consume(state: EntitlementState, summaryType: SummaryType, now: Long): SummaryQuotaResult =
      summaryType match
        case SummaryType.Match if state.matchSummaryTokens > 0 =>
          allowed(state.copy(matchSummaryTokens = state.matchSummaryTokens - 1, updatedAt = now), "onboarding_match_summary_token")
        case SummaryType.Performance if state.performanceSummaryTokens > 0 =>
          allowed(state.copy(performanceSummaryTokens = state.performanceSummaryTokens - 1, updatedAt = now), "onboarding_performance_summary_token")
        case SummaryType.Match if state.plan == PlanTier.Premium && state.premiumMatchSummaryDailyRemaining > 0 =>
          allowed(state.copy(premiumMatchSummaryDailyRemaining = state.premiumMatchSummaryDailyRemaining - 1, updatedAt = now), "premium_match_summary_daily")
        case SummaryType.Performance if state.plan == PlanTier.Premium && state.premiumPerformanceSummaryDailyRemaining > 0 =>
          allowed(state.copy(premiumPerformanceSummaryDailyRemaining = state.premiumPerformanceSummaryDailyRemaining - 1, updatedAt = now), "premium_performance_summary_daily")
        case _ =>
          SummaryQuotaResult(EntitlementDecision.Blocked, state, "summary_quota_unavailable")

    private def allowed(state: EntitlementState, reason: String): SummaryQuotaResult =
      SummaryQuotaResult(EntitlementDecision.Allowed, state, reason)

  enum ReviewTokenBucket:
    case CustomAnalysis
    case MatchReview
    case FullAnalysis

  final case class ReviewTokenConsumptionResult(
      decision: EntitlementDecision,
      state: EntitlementState,
      ledgerEntries: List[TokenLedgerEntryModel],
      reason: String
  ):
    def allowed: Boolean = decision != EntitlementDecision.Blocked

  object ReviewTokenService:
    def consumeCustomAnalysis(
        state: EntitlementState,
        plan: LiveCoaching.CustomReviewPlan,
        now: Long
    ): ReviewTokenConsumptionResult =
      if !plan.valid then blocked(state, "invalid_custom_review_plan")
      else if !plan.requiresCustomAnalysisTokens then noOp(state, "custom_analysis_token_not_required")
      else consume(
        state = state,
        bucket = ReviewTokenBucket.CustomAnalysis,
        gameId = plan.request.gameId,
        reason = "custom_ece_analysis",
        now = now
      )

    def consumeFullGameReview(
        state: EntitlementState,
        plan: LiveCoaching.FullGameReviewPlan,
        now: Long
    ): ReviewTokenConsumptionResult =
      if !plan.valid then blocked(state, "invalid_full_game_review_plan")
      else
        plan.tokenKind match
          case LiveCoaching.FullGameReviewTokenKind.MatchReview =>
            consume(
              state = state,
              bucket = ReviewTokenBucket.MatchReview,
              gameId = plan.request.game.gameId,
              reason = "match_review",
              now = now
            )
          case LiveCoaching.FullGameReviewTokenKind.FullAnalysis =>
            consume(
              state = state,
              bucket = ReviewTokenBucket.FullAnalysis,
              gameId = plan.request.game.gameId,
              reason = "full_analysis",
              now = now
            )

    private def consume(
        state: EntitlementState,
        bucket: ReviewTokenBucket,
        gameId: String,
        reason: String,
        now: Long
    ): ReviewTokenConsumptionResult =
      val available = bucket match
        case ReviewTokenBucket.CustomAnalysis => state.customAnalysisTokens
        case ReviewTokenBucket.MatchReview    => state.matchReviewTokens
        case ReviewTokenBucket.FullAnalysis   => state.fullAnalysisTokens

      if available <= 0 then blocked(state, s"${reason}_token_unavailable")
      else
        val next = bucket match
          case ReviewTokenBucket.CustomAnalysis =>
            state.copy(customAnalysisTokens = state.customAnalysisTokens - 1, updatedAt = now)
          case ReviewTokenBucket.MatchReview =>
            state.copy(matchReviewTokens = state.matchReviewTokens - 1, updatedAt = now)
          case ReviewTokenBucket.FullAnalysis =>
            state.copy(fullAnalysisTokens = state.fullAnalysisTokens - 1, updatedAt = now)

        ReviewTokenConsumptionResult(
          EntitlementDecision.Allowed,
          next,
          List(
            Ledger.token(
              prefix = s"$reason-token-consumed",
              accountId = state.accountId,
              event = TokenLedgerEvent.Consumed,
              amount = 1,
              reason = reason,
              gameId = Some(gameId),
              now = now
            )
          ),
          s"${reason}_token_consumed"
        )

    private def noOp(state: EntitlementState, reason: String): ReviewTokenConsumptionResult =
      ReviewTokenConsumptionResult(EntitlementDecision.NoOp, state, Nil, reason)

    private def blocked(state: EntitlementState, reason: String): ReviewTokenConsumptionResult =
      ReviewTokenConsumptionResult(EntitlementDecision.Blocked, state, Nil, reason)

  final case class SavedGameRetentionConfig(
      rollingRecentLimit: Int,
      freeSavedSlots: Int,
      standardSavedSlots: Int,
      premiumSavedSlots: Int,
      schemaVersion: String
  ):
    def valid: Boolean =
      rollingRecentLimit >= 0 &&
        freeSavedSlots >= 0 &&
        standardSavedSlots >= freeSavedSlots &&
        premiumSavedSlots >= standardSavedSlots &&
        schemaVersion.nonEmpty

    def savedSlotLimit(plan: PlanTier): Int =
      plan match
        case PlanTier.NewAccountOnboarding => freeSavedSlots
        case PlanTier.FreeAdSupported      => freeSavedSlots
        case PlanTier.Standard             => standardSavedSlots
        case PlanTier.Premium              => premiumSavedSlots

  object SavedGameRetentionConfig:
    val default: SavedGameRetentionConfig =
      SavedGameRetentionConfig(
        rollingRecentLimit = 20,
        freeSavedSlots = 0,
        standardSavedSlots = 25,
        premiumSavedSlots = 100,
        schemaVersion = "evenchess-saved-game-retention-v1"
      )

  final case class SavedGameRecord(
      accountId: String,
      gameId: String,
      savedWhileTier: PlanTier,
      savedAt: Long,
      persistsAfterDowngrade: Boolean,
      eceHistoryRetained: Boolean,
      rawEceRetained: Boolean,
      schemaVersion: String
  ):
    def valid: Boolean =
      accountId.nonEmpty &&
        gameId.nonEmpty &&
        savedAt > 0 &&
        persistsAfterDowngrade &&
        eceHistoryRetained &&
        !rawEceRetained &&
        schemaVersion.nonEmpty

  final case class SavedGameSaveResult(
      decision: EntitlementDecision,
      state: EntitlementState,
      record: Option[SavedGameRecord],
      reason: String
  ):
    def allowed: Boolean =
      decision == EntitlementDecision.Allowed || decision == EntitlementDecision.NoOp

  object SavedGameService:
    val schemaVersion = "evenchess-saved-game-v1"

    def save(
        state: EntitlementState,
        gameId: String,
        config: SavedGameRetentionConfig,
        now: Long
    ): SavedGameSaveResult =
      if !state.valid then blocked(state, "invalid_entitlement_state")
      else if !config.valid then blocked(state, "invalid_saved_game_config")
      else if gameId.isEmpty then blocked(state, "missing_game_id")
      else if state.savedGameIds.contains(gameId) then SavedGameSaveResult(EntitlementDecision.NoOp, state, None, "already_saved")
      else if !canCreateNewSave(state, config) then blocked(state, "saved_game_slot_unavailable")
      else
        val record = SavedGameRecord(
          accountId = state.accountId,
          gameId = gameId,
          savedWhileTier = state.plan,
          savedAt = now,
          persistsAfterDowngrade = true,
          eceHistoryRetained = true,
          rawEceRetained = false,
          schemaVersion = schemaVersion
        )
        SavedGameSaveResult(
          EntitlementDecision.Allowed,
          state.copy(savedGameIds = state.savedGameIds :+ gameId, updatedAt = now),
          Some(record),
          "saved_game_recorded"
        )

    def canCreateNewSave(state: EntitlementState, config: SavedGameRetentionConfig): Boolean =
      config.savedSlotLimit(state.plan) > state.savedGameIds.size

    def canKeepAfterDowngrade(record: SavedGameRecord): Boolean =
      record.valid && record.persistsAfterDowngrade

    private def blocked(state: EntitlementState, reason: String): SavedGameSaveResult =
      SavedGameSaveResult(EntitlementDecision.Blocked, state, None, reason)

  final case class MonetisationFairnessSnapshot(
      before: FairnessSnapshot,
      after: FairnessSnapshot,
      operation: String,
      touchesPaymentProvider: Boolean,
      changesLiveStrength: Boolean,
      changesRatedSettlement: Boolean
  ):
    def valid: Boolean =
      operation.nonEmpty &&
        !touchesPaymentProvider &&
        !changesLiveStrength &&
        !changesRatedSettlement &&
        SubscriptionFairnessBoundary.unchangedByPhaseP(before, after)

  object SubscriptionFairnessBoundary:
    val subscriptionsAdsTokensMayChangeRatedFairness = false
    val premiumMayProvideStrongerLiveHelp = false

    def unchangedByPhaseI(before: FairnessSnapshot, after: FairnessSnapshot): Boolean =
      before == after

    def unchangedByPhaseP(before: FairnessSnapshot, after: FairnessSnapshot): Boolean =
      before == after

  object AccountSubscriptionSeam:
    val seam: IntegrationSeam = IntegrationSeam.AccountSubscription
    val dedicatedStoreNames: Set[String] = Set(
      "evenchess_entitlements",
      "evenchess_token_ledger",
      "evenchess_subscription_events",
      "evenchess_rewarded_ad_events"
    )
    val storesRawEmail = false
    val patchMapRequiredBeforeLilaAdapter = true
    val patchMapEntryRequiredNow = false

    def registered: Boolean =
      IntegrationSeamRegistry.all.exists(rule =>
        rule.seam == seam &&
          rule.adaptedToLila &&
          rule.serverAuthoritative &&
          !rule.fairnessAffecting
      )
