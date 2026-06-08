package lila.evenchess

import MonetisationPolicy.FairnessSnapshot
import ProductInvariants.RequirementClass

object MarketingFunnelPolicy:

  enum MarketingRequirement:
    case FairnessNonBypass
    case CampaignCannotAlterFairnessFields
    case PaidPlansAccessQuotaOnly
    case VariantsCopyOrderOnly
    case PublicCopySafety
    case BackendReadableConfig
    case LandingDefaults
    case OffersWindowsAttribution
    case AdminControlsAndPause

  final case class MarketingRequirementClassification(
      requirement: MarketingRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object MarketingRequirementClassifications:
    val all: List[MarketingRequirementClassification] = List(
      MarketingRequirementClassification(
        MarketingRequirement.FairnessNonBypass,
        RequirementClass.EvenChessSpecific,
        "Marketing config, variants, windows, subscriptions, ads, tokens, offers, and prompts cannot bypass fairness."
      ),
      MarketingRequirementClassification(
        MarketingRequirement.CampaignCannotAlterFairnessFields,
        RequirementClass.EvenChessSpecific,
        "Advertising and funnel control cannot alter server-authoritative fairness fields."
      ),
      MarketingRequirementClassification(
        MarketingRequirement.PaidPlansAccessQuotaOnly,
        RequirementClass.EvenChessSpecific,
        "Paid plans change access, quotas, convenience, and review frequency only."
      ),
      MarketingRequirementClassification(
        MarketingRequirement.VariantsCopyOrderOnly,
        RequirementClass.EvenChessSpecific,
        "Campaign variants may change copy order and emphasis only."
      ),
      MarketingRequirementClassification(
        MarketingRequirement.PublicCopySafety,
        RequirementClass.EvenChessSpecific,
        "Public copy must not imply cheating, hidden engine use, off-platform assistance, or stronger paid live help."
      ),
      MarketingRequirementClassification(
        MarketingRequirement.BackendReadableConfig,
        RequirementClass.AdaptedToLichessFork,
        "Landing/funnel config must be backend-readable and safe-fallback capable."
      ),
      MarketingRequirementClassification(
        MarketingRequirement.LandingDefaults,
        RequirementClass.EvenChessSpecific,
        "Default landing copy and section taxonomy must preserve disclosed assisted-mode positioning."
      ),
      MarketingRequirementClassification(
        MarketingRequirement.OffersWindowsAttribution,
        RequirementClass.EvenChessSpecific,
        "Offers, play-window display, and attribution events are marketing systems with fairness guardrails."
      ),
      MarketingRequirementClassification(
        MarketingRequirement.AdminControlsAndPause,
        RequirementClass.AdaptedToLichessFork,
        "Admin controls should integrate later with Lichess admin/ops patterns without changing game fairness."
      )
    )

  enum LandingVariant:
    case Default
    case AdultImprover
    case ParentLearning
    case FairRating
    case SummaryLoop
    case FreeTokens

  enum LandingSection:
    case Hero
    case Trust
    case Difference
    case HowItWorks
    case Proof
    case Pricing
    case FAQ
    case FinalCTA

  object LandingDefaults:
    val safeDefaultClaim = "Chess that teaches you while you play. Opponent knows. Help is capped. Rating adjusts."
    val headline = "Chess that teaches you while you play."
    val subheading =
      "EvenChess is a separate assisted chess mode where platform coaching is built into the rules. Your opponent sees it, help is capped, and ECR accounts for what you use."
    val cta = "Start with 10 free games."
    val offerChip = "10 game tokens; 3 full match summaries; 1 performance summary after 10 completed games."
    val trustStrip: List[String] = List("Opponent knows", "Help is capped", "Rating adjusts")
    val requiredSections: List[LandingSection] = List(
      LandingSection.Hero,
      LandingSection.Trust,
      LandingSection.Difference,
      LandingSection.HowItWorks,
      LandingSection.Proof,
      LandingSection.Pricing,
      LandingSection.FAQ,
      LandingSection.FinalCTA
    )

  final case class MarketingConfig(
      version: String,
      updatedAt: Long,
      heroHeadline: String,
      heroSubheading: String,
      offerChip: String,
      trustStrip: List[String],
      planWording: String,
      pricing: String,
      faq: List[String],
      demoUrl: String,
      launchRegion: String,
      landingVariant: LandingVariant,
      rewardedAdWording: String,
      summaryTokenWording: String,
      killSwitch: Boolean,
      playWindows: List[PlayWindow]
  ):
    def hasRequiredFields: Boolean =
      version.nonEmpty &&
        updatedAt > 0 &&
        heroHeadline.nonEmpty &&
        heroSubheading.nonEmpty &&
        offerChip.nonEmpty &&
        trustStrip.nonEmpty &&
        planWording.nonEmpty &&
        pricing.nonEmpty &&
        faq.nonEmpty &&
        demoUrl.nonEmpty &&
        launchRegion.nonEmpty &&
        rewardedAdWording.nonEmpty &&
        summaryTokenWording.nonEmpty

    def safeForUse: Boolean =
      hasRequiredFields && CopySafety.isAllowed(marketingCopy)

    def marketingCopy: String =
      List(
        heroHeadline,
        heroSubheading,
        offerChip,
        trustStrip.mkString(" "),
        planWording,
        pricing,
        faq.mkString(" "),
        rewardedAdWording,
        summaryTokenWording
      ).mkString(" ")

  object MarketingConfig:
    def safeFallback(now: Long): MarketingConfig =
      MarketingConfig(
        version = "marketing-safe-fallback-v1",
        updatedAt = now,
        heroHeadline = LandingDefaults.headline,
        heroSubheading = LandingDefaults.subheading,
        offerChip = LandingDefaults.offerChip,
        trustStrip = LandingDefaults.trustStrip,
        planWording = "Free games, Standard access, and Premium summaries never change live rated help.",
        pricing = PricingCopy.fairnessFootnote,
        faq = List(LandingDefaults.safeDefaultClaim),
        demoUrl = "/evenchess/demo",
        launchRegion = "AU",
        landingVariant = LandingVariant.Default,
        rewardedAdWording = "Rewarded ads may grant game tokens, never stronger help.",
        summaryTokenWording = "Summary tokens unlock review, not live strength.",
        killSwitch = false,
        playWindows = Nil
      )

  object CopySafety:
    val forbiddenPhrases: List[String] = List(
      "cheat legally",
      "secret engine",
      "use stockfish during games",
      "beat stronger players with ai",
      "best move shown live",
      "premium gives stronger help"
    )

    def forbiddenPhraseHits(copy: String): List[String] =
      val normalized = copy.toLowerCase
      forbiddenPhrases.filter(normalized.contains)

    def isAllowed(copy: String): Boolean =
      forbiddenPhraseHits(copy).isEmpty

  final case class LandingVariantPolicy(
      variant: LandingVariant,
      sectionOrder: List[LandingSection],
      emphasis: String,
      changesFairnessFields: Boolean,
      changesOfferAmounts: Boolean
  ):
    def valid: Boolean =
      LandingDefaults.requiredSections.forall(sectionOrder.contains) &&
        emphasis.nonEmpty &&
        !changesFairnessFields &&
        !changesOfferAmounts

  object LandingVariants:
    val all: List[LandingVariantPolicy] =
      LandingVariant.values.toList.map: variant =>
        LandingVariantPolicy(
          variant = variant,
          sectionOrder = LandingDefaults.requiredSections,
          emphasis = variant.toString,
          changesFairnessFields = false,
          changesOfferAmounts = false
        )

  object PricingCopy:
    val freeOffer = "10 game tokens, 3 match summaries, 1 performance summary after 10 games."
    val standard = "$10 AUD/4 weeks ($2.50/week)."
    val premium = "$16 AUD/4 weeks ($4/week), plus 10 match summaries/day and 1 performance summary/day."
    val fairnessFootnote = "Plans change access, quotas, convenience, and review frequency only; live rated strength does not change."

    def includesFairnessFootnote(copy: String): Boolean =
      copy.contains(fairnessFootnote)

  final case class PlayWindow(
      windowId: String,
      startsAt: Long,
      endsAt: Long,
      label: String
  ):
    def activeAt(now: Long): Boolean =
      startsAt <= now && now <= endsAt

  object PlayWindowDisplay:
    def labelFor(now: Long, windows: List[PlayWindow]): String =
      windows.find(_.activeAt(now)).map(_ => "Play now").getOrElse("Next window")

    def manipulatesHiddenQueueOrFairness: Boolean = false

  final case class AttributionTags(
      utmSource: Option[String],
      utmMedium: Option[String],
      utmCampaign: Option[String],
      utmContent: Option[String],
      utmTerm: Option[String],
      clickId: Option[String],
      variant: LandingVariant,
      firstCampaign: Option[String],
      latestCampaign: Option[String]
  ):
    def valid: Boolean = firstCampaign.nonEmpty || latestCampaign.nonEmpty || clickId.nonEmpty

  enum AttributionEvent:
    case LandingPageView
    case ViewPricing
    case BeginSignup
    case SignUpComplete
    case FirstGameStarted
    case FirstGameCompleted
    case GamesCompleted3
    case TenGamesCompleted
    case RewardedAdComplete
    case MatchSummaryView
    case PerformanceSummaryView
    case CheckoutStart
    case Purchase
    case Renew
    case Cancel

  object AttributionEvents:
    val required: Set[AttributionEvent] = AttributionEvent.values.toSet

    def includesRequired(events: Set[AttributionEvent]): Boolean =
      required.subsetOf(events)

  final case class AdminMarketingControls(
      marketingSiteEnabled: Boolean,
      activeLandingVariant: LandingVariant,
      offerVersion: String,
      playWindows: List[PlayWindow],
      rewardedAdsEnabled: Boolean,
      standardPlanEnabled: Boolean,
      premiumPlanEnabled: Boolean,
      paidAcquisitionMode: Boolean,
      campaignPauseNotice: Option[String]
  ):
    def hasRequiredFields: Boolean = offerVersion.nonEmpty

  final case class PaidAcquisitionHealth(
      trackingOk: Boolean,
      paymentsOk: Boolean,
      queueOk: Boolean,
      copySafetyOk: Boolean
  ):
    def shouldPause: Boolean = !(trackingOk && paymentsOk && queueOk && copySafetyOk)

  object MarketingFairnessBoundary:
    val marketingMayBypassFairness = false
    val campaignMayAlterCoachingPermission = false
    val campaignMayAlterStockfishExposure = false
    val paidPlansMayAlterLiveStrength = false

    def unchangedByCampaign(before: FairnessSnapshot, after: FairnessSnapshot): Boolean =
      before == after
