package lila.evenchess

class EvenChessThemeTest extends munit.FunSuite:

  import EvenChessTheme.*
  import ProductInvariants.RequirementClass

  test("Version 1.2 Phase B theme requirements are classified before integration"):
    val byRequirement = PhaseBRequirementClassifications.all.map(c => c.requirement -> c.classification).toMap

    assertEquals(byRequirement(PhaseBRequirement.DeepBlueVisualIdentity), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseBRequirement.LichessLayoutCompatibility), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseBRequirement.BoardFirstFutureCompatibility), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseBRequirement.PublicShellPolish), RequirementClass.AdaptedToLichessFork)

  test("default theme is deep blue without becoming one-note"):
    assert(EvenChessTheme.default.valid)
    assert(EvenChessTheme.default.colors.exists(color => color.hueFamily == "blue" && color.role.contains("brand")))
    assert(EvenChessTheme.default.colors.exists(_.hueFamily == "green"))
    assert(EvenChessTheme.default.colors.exists(_.hueFamily == "yellow"))
    assert(EvenChessTheme.default.colors.exists(_.hueFamily == "neutral"))
    assert(EvenChessTheme.default.colors.forall(_.valid))

  test("component tokens protect stable dimensions and accessibility"):
    assert(EvenChessTheme.default.components.forall(_.valid))
    assert(EvenChessTheme.default.component("primary-button").exists(_.minHeightPx >= 44))
    assert(EvenChessTheme.default.component("feature-card").exists(_.radiusPx <= 8))
    assert(EvenChessTheme.default.component("coach-card").exists(!_.usesColorOnly))
    assert(EvenChessTheme.default.component("settings-row").nonEmpty)
    assert(EvenChessTheme.default.component("admin-status-chip").nonEmpty)

  test("responsive and inline style rules avoid viewport-scaled text and negative letter spacing"):
    assert(EvenChessTheme.default.responsiveRules.forall(_.valid))
    assert(EvenChessTheme.default.responsiveRules.exists(rule => rule.name == "content-grid" && rule.rule.contains("auto-fit")))
    assert(EvenChessTheme.Style.page.contains("width:100%"))
    assert(EvenChessTheme.Style.page.contains("max-width:min(1180px,100vw)"))
    assert(EvenChessTheme.Style.page.contains("min-width:0"))
    assert(EvenChessTheme.Style.page.contains("overflow-x:hidden"))
    assert(EvenChessTheme.Style.hero.contains("auto-fit"))
    assert(EvenChessTheme.Style.hero.contains("min(320px,100%)"))
    assert(EvenChessTheme.Style.hero.contains("box-sizing:border-box"))
    assert(EvenChessTheme.Style.hero.contains("overflow:hidden"))
    assert(EvenChessTheme.Style.heroCopy.contains("calc(100vw - 56px)"))
    assert(EvenChessTheme.Style.panel.contains("calc(100vw - 56px)"))
    assert(EvenChessTheme.Style.band.contains("box-sizing:border-box"))
    assert(EvenChessTheme.Style.darkBand.contains("box-sizing:border-box"))
    assert(EvenChessTheme.Style.avoidsViewportScaledText)
    assert(EvenChessTheme.Style.avoidsNegativeLetterSpacing)
    assert(!EvenChessTheme.Style.all.exists(_.contains("font-size:42vw")))
    assert(!EvenChessTheme.default.usesNestedPageCards)
