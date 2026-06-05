package lila.evenchess

import ProductInvariants.RequirementClass

object EvenChessTheme:

  enum PhaseBRequirement:
    case DeepBlueVisualIdentity
    case LichessLayoutCompatibility
    case BoardFirstFutureCompatibility
    case AccessibleColorAndTextSignals
    case StableResponsiveDimensions
    case ReusableComponentTokens
    case PublicShellPolish

  final case class PhaseBRequirementClassification(
      requirement: PhaseBRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseBRequirementClassifications:
    val all: List[PhaseBRequirementClassification] = List(
      PhaseBRequirementClassification(
        PhaseBRequirement.DeepBlueVisualIdentity,
        RequirementClass.EvenChessSpecific,
        "EvenChess uses a restrained deep-blue brand system with supporting neutral and accent colors."
      ),
      PhaseBRequirementClassification(
        PhaseBRequirement.LichessLayoutCompatibility,
        RequirementClass.LichessProvided,
        "Use Lichess layout density, account controls, board assumptions, and page composition instead of rebuilding the UI shell."
      ),
      PhaseBRequirementClassification(
        PhaseBRequirement.BoardFirstFutureCompatibility,
        RequirementClass.LichessProvided,
        "Theme tokens cannot assume control over chessground, clocks, move input, or future live-board dimensions."
      ),
      PhaseBRequirementClassification(
        PhaseBRequirement.AccessibleColorAndTextSignals,
        RequirementClass.EvenChessSpecific,
        "Status, Offset Count, and coaching surfaces need text/icon redundancy and contrast-conscious colors."
      ),
      PhaseBRequirementClassification(
        PhaseBRequirement.StableResponsiveDimensions,
        RequirementClass.EvenChessSpecific,
        "Shell components use fixed radii, min sizes, grid constraints, and no viewport-scaled text."
      ),
      PhaseBRequirementClassification(
        PhaseBRequirement.ReusableComponentTokens,
        RequirementClass.EvenChessSpecific,
        "Buttons, cards, badges, coaching panels, settings rows, admin chips, and nav treatments share named tokens."
      ),
      PhaseBRequirementClassification(
        PhaseBRequirement.PublicShellPolish,
        RequirementClass.AdaptedToLichessFork,
        "The existing Lichess homepage seam consumes EvenChess theme tokens through a small adapter."
      )
    )

  final case class ColorToken(
      name: String,
      value: String,
      role: String,
      hueFamily: String
  ):
    def valid: Boolean =
      name.nonEmpty &&
        value.matches("#[0-9a-fA-F]{6}") &&
        role.nonEmpty &&
        hueFamily.nonEmpty

  final case class ComponentToken(
      name: String,
      role: String,
      minHeightPx: Int,
      radiusPx: Int,
      usesColorOnly: Boolean
  ):
    def valid: Boolean =
      name.nonEmpty &&
        role.nonEmpty &&
        minHeightPx >= 32 &&
        radiusPx <= 8 &&
        !usesColorOnly

  final case class ResponsiveRule(
      name: String,
      rule: String,
      protectsTextFit: Boolean,
      protectsBoardFit: Boolean
  ):
    def valid: Boolean =
      name.nonEmpty &&
        rule.nonEmpty &&
        protectsTextFit &&
        protectsBoardFit &&
        !rule.contains("font-size:") &&
        !rule.contains("vw")

  final case class Theme(
      name: String,
      colors: List[ColorToken],
      components: List[ComponentToken],
      responsiveRules: List[ResponsiveRule],
      maxContentWidthPx: Int,
      shellRadiusPx: Int,
      heroMinHeightPx: Int,
      usesNegativeLetterSpacing: Boolean,
      usesNestedPageCards: Boolean,
      usesViewportScaledText: Boolean
  ):
    def valid: Boolean =
      name.nonEmpty &&
        colors.forall(_.valid) &&
        colors.exists(color => color.hueFamily == "blue" && color.role.contains("brand")) &&
        colors.exists(color => color.hueFamily != "blue") &&
        components.forall(_.valid) &&
        responsiveRules.forall(_.valid) &&
        maxContentWidthPx >= 960 &&
        shellRadiusPx <= 8 &&
        heroMinHeightPx >= 420 &&
        !usesNegativeLetterSpacing &&
        !usesNestedPageCards &&
        !usesViewportScaledText

    def component(name: String): Option[ComponentToken] =
      components.find(_.name == name)

  val default: Theme =
    Theme(
      name = "EvenChess Deep Blue",
      colors = List(
        ColorToken("deep-navy", "#071326", "brand background", "blue"),
        ColorToken("lichess-blue", "#0f3d7a", "brand action", "blue"),
        ColorToken("coach-cyan", "#2dd4bf", "coaching accent", "green"),
        ColorToken("level-gold", "#f7c948", "level accent", "yellow"),
        ColorToken("panel-ink", "#eaf2ff", "dark surface text", "neutral"),
        ColorToken("panel-soft", "#f8fafc", "light surface", "neutral")
      ),
      components = List(
        ComponentToken("primary-button", "main EvenChess action", minHeightPx = 44, radiusPx = 6, usesColorOnly = false),
        ComponentToken("secondary-button", "secondary navigation action", minHeightPx = 40, radiusPx = 6, usesColorOnly = false),
        ComponentToken("trust-badge", "disclosure and fairness status", minHeightPx = 36, radiusPx = 8, usesColorOnly = false),
        ComponentToken("feature-card", "repeated platform feature item", minHeightPx = 132, radiusPx = 8, usesColorOnly = false),
        ComponentToken("coach-card", "future coaching overlay card", minHeightPx = 120, radiusPx = 8, usesColorOnly = false),
        ComponentToken("settings-row", "future EvenChess settings control row", minHeightPx = 44, radiusPx = 6, usesColorOnly = false),
        ComponentToken("admin-status-chip", "future admin backend status chip", minHeightPx = 32, radiusPx = 8, usesColorOnly = false)
      ),
      responsiveRules = List(
        ResponsiveRule(
          "content-grid",
          "grid-template-columns:repeat(auto-fit,minmax(min(320px,100%),1fr))",
          protectsTextFit = true,
          protectsBoardFit = true
        ),
        ResponsiveRule("mobile-stack", "grid-template-columns:1fr", protectsTextFit = true, protectsBoardFit = true),
        ResponsiveRule("card-grid", "grid-template-columns:repeat(auto-fit,minmax(220px,1fr))", protectsTextFit = true, protectsBoardFit = true)
      ),
      maxContentWidthPx = 1180,
      shellRadiusPx = 8,
      heroMinHeightPx = 420,
      usesNegativeLetterSpacing = false,
      usesNestedPageCards = false,
      usesViewportScaledText = false
    )

  object Style:
    val page: String =
      "width:100%;max-width:min(1180px,100vw);min-width:0;box-sizing:border-box;margin:0 auto;color:#eaf2ff;background:#071326;border-radius:8px;overflow:hidden;overflow-x:hidden"
    val hero: String =
      "width:100%;max-width:100%;min-height:420px;box-sizing:border-box;display:grid;grid-template-columns:repeat(auto-fit,minmax(min(320px,100%),1fr));gap:28px;align-items:center;padding:44px 28px;background:#071326;overflow:hidden"
    val heroCopy: String =
      "max-width:min(680px,calc(100vw - 56px));min-width:0;overflow-wrap:anywhere"
    val eyebrow: String =
      "display:inline-flex;min-height:32px;align-items:center;padding:0 12px;border:1px solid #2dd4bf;border-radius:8px;color:#2dd4bf;background:#08203a"
    val headline: String =
      "margin:14px 0 12px;font-size:42px;line-height:1.08;font-weight:700;letter-spacing:0"
    val subheading: String =
      "margin:0 0 22px;font-size:18px;line-height:1.55;color:#d7e5f8"
    val actions: String =
      "display:flex;flex-wrap:wrap;gap:12px;align-items:center"
    val primaryButton: String =
      "min-height:44px;display:inline-flex;align-items:center;padding:0 18px;border-radius:6px;background:#2dd4bf;color:#071326;font-weight:700"
    val secondaryButton: String =
      "min-height:40px;display:inline-flex;align-items:center;padding:0 16px;border-radius:6px;border:1px solid #7aa2d6;color:#eaf2ff"
    val panel: String =
      "max-width:calc(100vw - 56px);min-width:0;box-sizing:border-box;overflow:hidden;background:#0b1f3f;border:1px solid #234f88;border-radius:8px;padding:22px"
    val statGrid: String =
      "display:grid;grid-template-columns:1fr;gap:12px"
    val stat: String =
      "min-height:56px;padding:12px;border-radius:8px;background:#0e2a52;border:1px solid #1f5796"
    val band: String =
      "width:100%;max-width:100%;box-sizing:border-box;padding:34px 44px;background:#f8fafc;color:#102033;overflow:hidden"
    val darkBand: String =
      "width:100%;max-width:100%;box-sizing:border-box;padding:34px 44px;background:#0b1f3f;color:#eaf2ff;overflow:hidden"
    val cardGrid: String =
      "display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:14px"
    val card: String =
      "min-height:132px;padding:18px;border-radius:8px;border:1px solid #c9d7ea;background:#ffffff;color:#102033"
    val darkCard: String =
      "min-height:132px;padding:18px;border-radius:8px;border:1px solid #234f88;background:#102b52;color:#eaf2ff"
    val trustList: String =
      "display:flex;flex-wrap:wrap;gap:10px;margin:18px 0 0;padding:0;list-style:none"
    val trustBadge: String =
      "min-height:36px;display:flex;align-items:center;padding:0 12px;border-radius:8px;background:#eaf6ff;color:#0b2d57;border:1px solid #b8d4f1"

    val all: List[String] = List(
      page,
      hero,
      heroCopy,
      eyebrow,
      headline,
      subheading,
      actions,
      primaryButton,
      secondaryButton,
      panel,
      statGrid,
      stat,
      band,
      darkBand,
      cardGrid,
      card,
      darkCard,
      trustList,
      trustBadge
    )

    def avoidsViewportScaledText: Boolean =
      all.forall(style => !style.contains("font-size:") || !style.contains("vw"))

    def avoidsNegativeLetterSpacing: Boolean =
      all.forall(style => !style.contains("letter-spacing:-"))
