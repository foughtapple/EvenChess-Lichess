package lila.evenchess

import ProductInvariants.RequirementClass

object PublicShell:

  enum PublicShellRequirement:
    case EvenChessOnlyPublicProduct
    case LichessFirstPublicShell
    case MinimalEvenChessEntrypoints
    case LichessPoweredProductShell
    case PreserveLichessInternals
    case PreserveCompatibleLichessFeatureAreas
    case RestoreLichessFeatureNavigation
    case PreserveProfileAndAccountNavigation
    case RatedPlayRemainsEvenChessOwned
    case PublicNavigationTargetsEvenChess
    case LobbyStartControlsTargetEvenChess
    case LichessStyleEvenChessSearchControls
    case PublicCopyDisclosesAssistance
    case PublicCopyForbidsOutsideHelp
    case PublicCopyAvoidsPayToWin
    case MarketingCannotAlterFairness

  final case class PublicShellRequirementClassification(
      requirement: PublicShellRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PublicShellRequirementClassifications:
    val all: List[PublicShellRequirementClassification] = List(
      PublicShellRequirementClassification(
        PublicShellRequirement.EvenChessOnlyPublicProduct,
        RequirementClass.AdaptedToLichessFork,
        "Current V1 direction keeps the public play/search product EvenChess-only while preserving Lichess shell structure and internals."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.LichessFirstPublicShell,
        RequirementClass.AdaptedToLichessFork,
        "Use the standard Lichess homepage and navigation structure, but brand it EvenChess and route public play/search to EvenChess."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.MinimalEvenChessEntrypoints,
        RequirementClass.SupersededByLichessFork,
        "The minimal-entrypoint-only reading of Version 1.3 is superseded; public Play and lobby search are EvenChess entrypoints."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.LichessPoweredProductShell,
        RequirementClass.AdaptedToLichessFork,
        "Version 1.2 presents EvenChess as a polished Lichess-powered platform with EvenChess-specific assistance layers."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.PreserveLichessInternals,
        RequirementClass.LichessProvided,
        "Normal Lichess chess internals remain available for platform reuse, diagnostics, regression tests, and upstream sync."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.PreserveCompatibleLichessFeatureAreas,
        RequirementClass.LichessProvided,
        "Compatible Lichess feature areas such as study, openings, analysis, puzzles, accounts, settings, and community surfaces remain available for branded integration."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.RestoreLichessFeatureNavigation,
        RequirementClass.AdaptedToLichessFork,
        "Version 1.2 restores safe public navigation to Lichess-provided learning, exploration, community, and account areas under EvenChess branding."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.PreserveProfileAndAccountNavigation,
        RequirementClass.LichessProvided,
        "Lichess account, profile, settings, and security surfaces remain reachable instead of being rebuilt or hidden."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.RatedPlayRemainsEvenChessOwned,
        RequirementClass.EvenChessSpecific,
        "Rated public play entrypoints remain EvenChess/ECR owned and do not route to normal Lichess rated pools."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.PublicNavigationTargetsEvenChess,
        RequirementClass.AdaptedToLichessFork,
        "The primary Play navigation targets EvenChess search while preserving Lichess-provided shell structure and features."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.LobbyStartControlsTargetEvenChess,
        RequirementClass.AdaptedToLichessFork,
        "The Lichess lobby start controls are reused as EvenChess start controls instead of opening normal Lichess game creation."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.LichessStyleEvenChessSearchControls,
        RequirementClass.AdaptedToLichessFork,
        "EvenChess Set Level, mode, time control, and disclosure inputs appear inside the Lichess-style lobby/search surface."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.PublicCopyDisclosesAssistance,
        RequirementClass.EvenChessSpecific,
        "Public copy states that platform coaching is disclosed, capped, and accounted for in ECR."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.PublicCopyForbidsOutsideHelp,
        RequirementClass.EvenChessSpecific,
        "Public copy prohibits non-platform engines, humans, notes, bots, chat, and unaudited analysis in rated EvenChess."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.PublicCopyAvoidsPayToWin,
        RequirementClass.EvenChessSpecific,
        "Public copy avoids cheating, hidden-engine, normal-chess-with-help, and pay-to-win positioning."
      ),
      PublicShellRequirementClassification(
        PublicShellRequirement.MarketingCannotAlterFairness,
        RequirementClass.EvenChessSpecific,
        "Landing copy, CTAs, offers, and navigation do not alter fairness fields or live help strength."
      )
    )

  enum PublicSurface:
    case Home
    case LiveGames
    case AiGames
    case Review
    case Pricing
    case Signup
    case Login
    case Learn
    case Practice
    case Puzzles
    case Study
    case Openings
    case Analysis
    case OpeningExplorer
    case Community
    case Teams
    case Forum
    case Blog
    case Profile
    case Entitlements
    case Preferences
    case Security

  enum PublicNavKind:
    case EvenChessPlay
    case LichessFeature
    case AccountArea
    case Marketing

  final case class PublicNavTarget(
      surface: PublicSurface,
      label: String,
      href: String,
      startsNormalLichessGame: Boolean,
      fairnessAffecting: Boolean,
      kind: PublicNavKind,
      startsRatedPlay: Boolean,
      requiresAuth: Boolean
  ):
    def validForEvenChessOnlyPublicNav: Boolean =
      label.nonEmpty &&
        href.nonEmpty &&
        !startsNormalLichessGame &&
        !fairnessAffecting &&
        (!startsRatedPlay || kind == PublicNavKind.EvenChessPlay)

    def validForSafeFeatureRestoration: Boolean =
      validForEvenChessOnlyPublicNav &&
        (kind != PublicNavKind.LichessFeature || !startsRatedPlay) &&
        (kind != PublicNavKind.AccountArea || requiresAuth)

  object PublicRoutes:
    val home = "/"
    val liveGames = "/#hook"
    val playSearch = PlaySearchIntegration.Routes.search
    val playSearchJson = PlaySearchIntegration.Routes.searchJson
    val fallbackPlaySetup = liveGames
    val aiGames = "/#ai"
    val targetLevel = liveGames
    val review = "/#evenchess-review"
    val pricing = "/#evenchess-pricing"
    val signup = "/signup"
    val login = "/login"
    val learn = "/learn"
    val practice = "/practice"
    val puzzles = "/training"
    val study = "/study"
    val openings = "/opening"
    val analysis = "/analysis"
    val openingExplorer = "/analysis#explorer"
    val community = "/player"
    val teams = "/team"
    val forum = "/forum"
    val blog = "/blog/community"
    val profile = "/account/profile"
    val entitlements = AccountMonetisationUi.Routes.account
    val preferences = "/account/preferences/evenchess"
    val security = "/account/security"
    def navTargets: List[PublicNavTarget] = Navigation.all

  object Navigation:
    private def evenChess(
        surface: PublicSurface,
        label: String,
        href: String,
        startsRatedPlay: Boolean = false
    ) =
      PublicNavTarget(surface, label, href, false, false, PublicNavKind.EvenChessPlay, startsRatedPlay, false)

    private def feature(surface: PublicSurface, label: String, href: String) =
      PublicNavTarget(surface, label, href, false, false, PublicNavKind.LichessFeature, false, false)

    private def account(surface: PublicSurface, label: String, href: String) =
      PublicNavTarget(surface, label, href, false, false, PublicNavKind.AccountArea, false, true)

    private def marketing(surface: PublicSurface, label: String, href: String) =
      PublicNavTarget(surface, label, href, false, false, PublicNavKind.Marketing, false, false)

    val playTargets: List[PublicNavTarget] = List(
      evenChess(PublicSurface.LiveGames, "Live EvenChess", PublicRoutes.liveGames, startsRatedPlay = true),
      evenChess(PublicSurface.AiGames, "AI Games", PublicRoutes.aiGames),
      evenChess(PublicSurface.Review, "Review", PublicRoutes.review),
      marketing(PublicSurface.Pricing, "Plans", PublicRoutes.pricing)
    )

    val lichessFeatureTargets: List[PublicNavTarget] = List(
      feature(PublicSurface.Learn, "Learn", PublicRoutes.learn),
      feature(PublicSurface.Practice, "Practice", PublicRoutes.practice),
      feature(PublicSurface.Puzzles, "Puzzles", PublicRoutes.puzzles),
      feature(PublicSurface.Study, "Study", PublicRoutes.study),
      feature(PublicSurface.Openings, "Openings", PublicRoutes.openings),
      feature(PublicSurface.Analysis, "Analysis board", PublicRoutes.analysis),
      feature(PublicSurface.OpeningExplorer, "Opening explorer", PublicRoutes.openingExplorer),
      feature(PublicSurface.Community, "Players", PublicRoutes.community),
      feature(PublicSurface.Teams, "Teams", PublicRoutes.teams),
      feature(PublicSurface.Forum, "Forum", PublicRoutes.forum),
      feature(PublicSurface.Blog, "Blog", PublicRoutes.blog)
    )

    val accountTargets: List[PublicNavTarget] = List(
      account(PublicSurface.Profile, "Profile", PublicRoutes.profile),
      account(PublicSurface.Entitlements, "Tokens & plans", PublicRoutes.entitlements),
      account(PublicSurface.Preferences, "Preferences", PublicRoutes.preferences),
      account(PublicSurface.Security, "Security", PublicRoutes.security)
    )

    val all: List[PublicNavTarget] =
      playTargets ++ lichessFeatureTargets ++ accountTargets

    val minimalEvenChessEntrypoints: List[PublicNavTarget] = List(
      playTargets.find(_.surface == PublicSurface.LiveGames),
      accountTargets.find(_.surface == PublicSurface.Entitlements),
      accountTargets.find(_.surface == PublicSurface.Preferences)
    ).flatten

    val requiredPhaseCSurfaces: Set[PublicSurface] = Set(
      PublicSurface.LiveGames,
      PublicSurface.Learn,
      PublicSurface.Puzzles,
      PublicSurface.Study,
      PublicSurface.Openings,
      PublicSurface.Analysis,
      PublicSurface.Community,
      PublicSurface.Profile,
      PublicSurface.Preferences
    )

    def restoresRequiredFeatureAreas: Boolean =
      requiredPhaseCSurfaces.subsetOf(all.map(_.surface).toSet)

    def ratedPlayBoundaryValid: Boolean =
      all.filter(_.startsRatedPlay).forall(target =>
        target.kind == PublicNavKind.EvenChessPlay && !target.startsNormalLichessGame
      )

    def minimalEntrypointsValid: Boolean =
      minimalEvenChessEntrypoints.map(_.surface).toSet == Set(
        PublicSurface.LiveGames,
        PublicSurface.Entitlements,
        PublicSurface.Preferences
      ) &&
        minimalEvenChessEntrypoints.forall(_.validForEvenChessOnlyPublicNav)

    def valid: Boolean =
      all.forall(_.validForSafeFeatureRestoration) &&
        restoresRequiredFeatureAreas &&
        ratedPlayBoundaryValid &&
        minimalEntrypointsValid

  final case class HomepageFact(label: String, body: String):
    def valid: Boolean = label.nonEmpty && body.nonEmpty

  object PublicCopy:
    val productName = "EvenChess"
    val title = "EvenChess"
    val eyebrow = "Lichess-powered assisted chess"
    val headline = "Chess that teaches you while you play."
    val subheading =
      "EvenChess is a separate assisted chess mode where platform coaching is built into the rules. Your opponent sees it, help is capped, and ECR accounts for what you use."
    val homepageTitle = "EvenChess - disclosed assisted chess"
    val homepageSummary =
      "EvenChess is chess where platform coaching is disclosed, capped by Set Level, logged by the server, and reflected in ECR."
    val homepageFacts: List[HomepageFact] = List(
      HomepageFact("Disclosed", "Your opponent knows platform coaching is part of the mode."),
      HomepageFact("Capped", "Set Level limits what live coaching can show."),
      HomepageFact("Rated fairly", "ECR accounts for authorised help and actual use.")
    )
    val navSubtitle = "Lichess-powered coaching"
    val cta = "Start with 10 free games"
    val offerChip = "10 game tokens; 3 match summaries; 1 performance summary after 10 completed games."
    val trustStrip: List[String] = List("Opponent knows", "Help is capped", "Rating adjusts")
    val platformStrip: List[String] = List("Learn", "Puzzles", "Study", "Openings", "Analysis", "Community")
    val outsideHelpRule =
      "External engines, coaches, friends, stream chat, notes, bots, browser extensions, and unaudited analysis remain prohibited in rated EvenChess."
    val preservedPlatformNote =
      "Lichess powers the board, clocks, accounts, study, openings, analysis, puzzles, and game lifecycle; EvenChess adds the disclosed assisted layer."
    val normalChessNotPublic =
      "Ordinary Lichess internals remain preserved for reuse and regression; public Play opens the Lichess setup modal with EvenChess search settings."

    private val forbiddenPhrases = List(
      "cheat legally",
      "secret engine",
      "normal chess with help",
      "pay to win",
      "premium gives stronger help",
      "best move shown live"
    )

    def forbiddenPhraseHits(copy: String): List[String] =
      val normalized = copy.toLowerCase
      forbiddenPhrases.filter(normalized.contains)

    def safeForPublicUse: Boolean =
      forbiddenPhraseHits(
        List(
          eyebrow,
          headline,
          subheading,
          homepageTitle,
          homepageSummary,
          homepageFacts.flatMap(fact => List(fact.label, fact.body)).mkString(" "),
          navSubtitle,
          cta,
          offerChip,
          outsideHelpRule,
          preservedPlatformNote,
          normalChessNotPublic
        )
          .mkString(" ")
      ).isEmpty

  final case class ShellMetric(
      label: String,
      value: String,
      explanation: String
  ):
    def valid: Boolean =
      label.nonEmpty &&
        value.nonEmpty &&
        explanation.nonEmpty &&
        value.length <= 12

  object Metrics:
    val all: List[ShellMetric] = List(
      ShellMetric("Included games", "10", "starter game tokens"),
      ShellMetric("Live ladder", "L0-L10", "server-capped levels"),
      ShellMetric("Rating", "ECR", "assistance-adjusted")
    )

  final case class ShellSection(
      id: String,
      title: String,
      body: String
  ):
    def valid: Boolean = id.nonEmpty && title.nonEmpty && body.nonEmpty

  final case class PlatformFeature(
      label: String,
      href: String,
      body: String,
      lichessProvided: Boolean,
      evenChessAddsOverlayLayer: Boolean,
      startsRatedNormalChess: Boolean
  ):
    def valid: Boolean =
      label.nonEmpty &&
        href.nonEmpty &&
        body.nonEmpty &&
        lichessProvided &&
        evenChessAddsOverlayLayer &&
        !startsRatedNormalChess

  object PlatformFeatures:
    val all: List[PlatformFeature] = List(
      PlatformFeature(
        "Study",
        PublicRoutes.study,
        "Use Lichess study foundations with EvenChess coaching panels added through later thin adapters.",
        lichessProvided = true,
        evenChessAddsOverlayLayer = true,
        startsRatedNormalChess = false
      ),
      PlatformFeature(
        "Openings",
        PublicRoutes.openings,
        "Keep the opening explorer foundation and add grounded plan explanations without exposing raw engine help.",
        lichessProvided = true,
        evenChessAddsOverlayLayer = true,
        startsRatedNormalChess = false
      ),
      PlatformFeature(
        "Analysis",
        PublicRoutes.analysis,
        "Build post-game summaries on existing analysis and review surfaces without mutating live fairness.",
        lichessProvided = true,
        evenChessAddsOverlayLayer = true,
        startsRatedNormalChess = false
      ),
      PlatformFeature(
        "Puzzles",
        PublicRoutes.puzzles,
        "Preserve Lichess puzzle training as a platform feature separate from rated EvenChess ECR.",
        lichessProvided = true,
        evenChessAddsOverlayLayer = true,
        startsRatedNormalChess = false
      )
    )

  object Sections:
    val liveGames = ShellSection(
      "evenchess-live-games",
      "Live Games",
      "Search will match by ECR, Set Level, expected offset, and time control."
    )
    val aiGames = ShellSection(
      "evenchess-ai-games",
      "AI Games",
      "Computer practice stays rating-neutral and separate from online ECR."
    )
    val review = ShellSection(
      "evenchess-review",
      "Review",
      "Post-game summaries explain completed games without mutating live rated fairness."
    )
    val pricing = ShellSection(
      "evenchess-pricing",
      "Plans",
      "Plans and tokens change access and review quotas, never live rated help strength."
    )
    val platform = ShellSection(
      "evenchess-platform",
      "Lichess-powered platform",
      "Study, openings, analysis, puzzles, accounts, and settings remain part of the platform; EvenChess adds the assisted layer."
    )
    val all: List[ShellSection] = List(liveGames, aiGames, review, pricing, platform)

  object ProductionPolicy:
    val publicNormalLichessPlayEnabled = false
    val normalLichessPublicShellPreserved = true
    val normalLichessInternalsPreserved = true
    val compatibleLichessFeaturesPreserved = true
    val publicNavigationStartsNormalGames = false
    val primaryPlayNavigationTargetsEvenChess = true
    val lobbyStartControlsTargetEvenChess = true
    val lichessStyleEvenChessSearchControls = true
    val evenChessEntrypointsExplicit = true
    val publicMarketingChangesFairness = false

    def valid: Boolean =
      !publicNormalLichessPlayEnabled &&
        normalLichessPublicShellPreserved &&
        normalLichessInternalsPreserved &&
        compatibleLichessFeaturesPreserved &&
        !publicNavigationStartsNormalGames &&
        primaryPlayNavigationTargetsEvenChess &&
        lobbyStartControlsTargetEvenChess &&
        lichessStyleEvenChessSearchControls &&
        evenChessEntrypointsExplicit &&
        !publicMarketingChangesFairness &&
        Navigation.minimalEntrypointsValid &&
        Sections.all.forall(_.valid) &&
        Metrics.all.forall(_.valid) &&
        PlatformFeatures.all.forall(_.valid) &&
        Navigation.valid &&
        EvenChessTheme.default.valid &&
        PublicCopy.safeForPublicUse
