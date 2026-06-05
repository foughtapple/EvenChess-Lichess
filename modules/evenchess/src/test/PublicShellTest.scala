package lila.evenchess

class PublicShellTest extends munit.FunSuite:

  import ProductInvariants.RequirementClass
  import PublicShell.*

  test("public shell requirements are classified before integration"):
    val byRequirement = PublicShellRequirementClassifications.all.map(c => c.requirement -> c).toMap

    assertEquals(
      byRequirement(PublicShellRequirement.EvenChessOnlyPublicProduct).classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PublicShellRequirement.LichessFirstPublicShell).classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PublicShellRequirement.MinimalEvenChessEntrypoints).classification,
      RequirementClass.SupersededByLichessFork
    )
    assertEquals(
      byRequirement(PublicShellRequirement.LichessPoweredProductShell).classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PublicShellRequirement.PreserveLichessInternals).classification,
      RequirementClass.LichessProvided
    )
    assertEquals(
      byRequirement(PublicShellRequirement.PreserveCompatibleLichessFeatureAreas).classification,
      RequirementClass.LichessProvided
    )
    assertEquals(
      byRequirement(PublicShellRequirement.RestoreLichessFeatureNavigation).classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PublicShellRequirement.PreserveProfileAndAccountNavigation).classification,
      RequirementClass.LichessProvided
    )
    assertEquals(
      byRequirement(PublicShellRequirement.RatedPlayRemainsEvenChessOwned).classification,
      RequirementClass.EvenChessSpecific
    )
    assertEquals(
      byRequirement(PublicShellRequirement.PublicNavigationTargetsEvenChess).classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PublicShellRequirement.LobbyStartControlsTargetEvenChess).classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PublicShellRequirement.LichessStyleEvenChessSearchControls).classification,
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PublicShellRequirement.MarketingCannotAlterFairness).classification,
      RequirementClass.EvenChessSpecific
    )

  test("evenchess play/account routes remain namespaced and explicit"):
    assertEquals(PublicRoutes.playSearch, PlaySearchIntegration.Routes.search)
    assertEquals(PublicRoutes.playSearchJson, PlaySearchIntegration.Routes.searchJson)
    assertEquals(PublicRoutes.entitlements, AccountMonetisationUi.Routes.account)
    assert(PublicRoutes.playSearch.startsWith("/evenchess/"))
    assert(PublicRoutes.playSearchJson.startsWith("/evenchess/"))
    assert(PublicRoutes.playSearch.endsWith("/play/search"))
    assert(PublicRoutes.playSearchJson.endsWith("/play/search.json"))
    assert(PublicRoutes.entitlements.startsWith("/evenchess/"))
    assertEquals(PublicRoutes.liveGames, "/#hook")

  test("explicit EvenChess entrypoints do not start normal Lichess games or change fairness"):
    assert(PublicRoutes.navTargets.nonEmpty)
    assert(Navigation.minimalEvenChessEntrypoints.forall(_.validForEvenChessOnlyPublicNav))
    assert(Navigation.minimalEntrypointsValid)
    assert(PublicRoutes.navTargets.exists(_.surface == PublicSurface.LiveGames))
    assert(PublicRoutes.navTargets.forall(_.href.startsWith("/")))
    assertEquals(PublicRoutes.liveGames, "/#hook")
    assertEquals(PublicRoutes.fallbackPlaySetup, PublicRoutes.liveGames)
    assertEquals(PublicRoutes.playSearch, PlaySearchIntegration.Routes.search)
    assertEquals(PublicRoutes.playSearchJson, PlaySearchIntegration.Routes.searchJson)
    assert(!PublicRoutes.playSearchJson.contains("#"))
    assert(Navigation.valid)
    assert(Navigation.ratedPlayBoundaryValid)

  test("Phase C restores safe Lichess feature and account navigation"):
    val restoredSurfaces = Navigation.all.map(_.surface).toSet

    assert(Navigation.restoresRequiredFeatureAreas)
    assert(Navigation.requiredPhaseCSurfaces.subsetOf(restoredSurfaces))
    assert(Navigation.lichessFeatureTargets.forall(_.kind == PublicNavKind.LichessFeature))
    assert(Navigation.lichessFeatureTargets.forall(!_.startsRatedPlay))
    assert(Navigation.accountTargets.forall(_.kind == PublicNavKind.AccountArea))
    assert(Navigation.accountTargets.forall(_.requiresAuth))
    assert(restoredSurfaces.contains(PublicSurface.Openings))
    assert(restoredSurfaces.contains(PublicSurface.OpeningExplorer))
    assert(restoredSurfaces.contains(PublicSurface.Profile))
    assert(restoredSurfaces.contains(PublicSurface.Entitlements))
    assert(restoredSurfaces.contains(PublicSurface.Preferences))
    assertEquals(PublicRoutes.entitlements, "/evenchess/account")
    assertEquals(PublicRoutes.preferences, "/account/preferences/evenchess")

  test("public copy discloses assistance and outside-help prohibition"):
    val copy = List(
      PublicCopy.subheading,
      PublicCopy.homepageTitle,
      PublicCopy.homepageSummary,
      PublicCopy.homepageFacts.flatMap(fact => List(fact.label, fact.body)).mkString(" "),
      PublicCopy.outsideHelpRule,
      PublicCopy.preservedPlatformNote,
      PublicCopy.normalChessNotPublic,
      PublicCopy.platformStrip.mkString(" ")
    ).mkString(" ")

    assert(copy.contains("platform coaching"))
    assert(copy.contains("disclosed"))
    assert(copy.contains("Set Level"))
    assert(copy.contains("logged"))
    assert(copy.contains("ECR"))
    assert(PublicCopy.homepageFacts.forall(_.valid))
    assertEquals(PublicCopy.homepageFacts.map(_.label), List("Disclosed", "Capped", "Rated fairly"))
    assert(copy.contains("External engines"))
    assert(copy.contains("study"))
    assert(copy.contains("Openings"))
    assert(copy.contains("Community"))
    assert(copy.contains("public Play"))
    assert(copy.contains("setup modal"))
    assert(PublicCopy.safeForPublicUse)
    assertEquals(PublicCopy.forbiddenPhraseHits(copy), Nil)

  test("production shell keeps Lichess styling while public play starts EvenChess"):
    assert(ProductionPolicy.valid)
    assert(!ProductionPolicy.publicNormalLichessPlayEnabled)
    assert(ProductionPolicy.normalLichessPublicShellPreserved)
    assert(ProductionPolicy.normalLichessInternalsPreserved)
    assert(ProductionPolicy.compatibleLichessFeaturesPreserved)
    assert(!ProductionPolicy.publicNavigationStartsNormalGames)
    assert(ProductionPolicy.primaryPlayNavigationTargetsEvenChess)
    assert(ProductionPolicy.lobbyStartControlsTargetEvenChess)
    assert(ProductionPolicy.lichessStyleEvenChessSearchControls)
    assert(ProductionPolicy.evenChessEntrypointsExplicit)
    assert(!ProductionPolicy.publicMarketingChangesFairness)

  test("landing shell has required public sections"):
    val sectionIds = Sections.all.map(_.id).toSet

    assert(sectionIds.contains("evenchess-live-games"))
    assert(sectionIds.contains("evenchess-ai-games"))
    assert(sectionIds.contains("evenchess-review"))
    assert(sectionIds.contains("evenchess-pricing"))
    assert(sectionIds.contains("evenchess-platform"))
    assert(Sections.all.forall(_.valid))

  test("platform feature cards preserve Lichess capabilities without starting normal rated games"):
    val labels = PlatformFeatures.all.map(_.label).toSet

    assertEquals(labels, Set("Study", "Openings", "Analysis", "Puzzles"))
    assert(PlatformFeatures.all.forall(_.valid))
    assert(PlatformFeatures.all.forall(_.href.startsWith("/")))
    assert(PlatformFeatures.all.forall(_.lichessProvided))
    assert(PlatformFeatures.all.forall(_.evenChessAddsOverlayLayer))
    assert(PlatformFeatures.all.forall(!_.startsRatedNormalChess))

  test("homepage metrics are compact and tied to EvenChess assistance concepts"):
    assert(Metrics.all.forall(_.valid))
    assert(Metrics.all.exists(metric => metric.value == "L0-L10"))
    assert(Metrics.all.exists(metric => metric.value == "ECR"))
