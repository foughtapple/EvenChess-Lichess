package lila.evenchess

import CoachingLadder.{ Level, UiSlot }
import ProductInvariants.RequirementClass

object CoachingOverlays:

  enum DeviceClass:
    case Desktop
    case Mobile

  enum StableSurface:
    case BoardLayer
    case SummaryCard
    case WarningCard
    case PlanCard
    case OffsetCountCard
    case OpeningEndgameCard
    case CandidateArea
    case AiExplain
    case PostGameReview

  final case class SurfaceContract(
      surface: StableSurface,
      purpose: String,
      defaultState: String,
      higherLevelExpansion: String,
      defaultVisible: Boolean,
      maxDefaultActiveCards: Int,
      uiSlot: UiSlot,
      classification: RequirementClass
  )

  object Surfaces:
    val all: List[SurfaceContract] = List(
      SurfaceContract(
        StableSurface.BoardLayer,
        "Primary play surface",
        "Board, clocks, move list",
        "Legal dots, rings, arrows, eval chip by level.",
        defaultVisible = true,
        maxDefaultActiveCards = 0,
        UiSlot.BoardLayer,
        RequirementClass.LichessProvided
      ),
      SurfaceContract(
        StableSurface.SummaryCard,
        "Game/coach headline",
        "Exact state",
        "Safety summary and source badges.",
        defaultVisible = true,
        maxDefaultActiveCards = 1,
        UiSlot.Summary,
        RequirementClass.EvenChessSpecific
      ),
      SurfaceContract(
        StableSurface.WarningCard,
        "Critical warning",
        "Collapsed headline",
        "Reason/line if allowed.",
        defaultVisible = false,
        maxDefaultActiveCards = 1,
        UiSlot.Warning,
        RequirementClass.EvenChessSpecific
      ),
      SurfaceContract(
        StableSurface.PlanCard,
        "Strategic idea",
        "Hidden/minimal",
        "Motif, plan, counterplay tabs.",
        defaultVisible = false,
        maxDefaultActiveCards = 1,
        UiSlot.PlanCard,
        RequirementClass.EvenChessSpecific
      ),
      SurfaceContract(
        StableSurface.OffsetCountCard,
        "Local exchange resolver",
        "Hidden/minimal",
        "Shield/equal, student-wins, opponent-wins, or disabled unknown exchange marker.",
        defaultVisible = false,
        maxDefaultActiveCards = 1,
        UiSlot.OffsetCard,
        RequirementClass.AdaptedToLichessFork
      ),
      SurfaceContract(
        StableSurface.OpeningEndgameCard,
        "Phase guidance",
        "Compact",
        "Tablebase/proof when available.",
        defaultVisible = false,
        maxDefaultActiveCards = 1,
        UiSlot.OpeningEndgameCard,
        RequirementClass.EvenChessSpecific
      ),
      SurfaceContract(
        StableSurface.CandidateArea,
        "Candidate moves",
        "Hidden before L5",
        "1/2/3 candidates and comparisons.",
        defaultVisible = false,
        maxDefaultActiveCards = 1,
        UiSlot.CandidateArea,
        RequirementClass.EvenChessSpecific
      ),
      SurfaceContract(
        StableSurface.AiExplain,
        "Grounded explanation",
        "Hidden/compact",
        "Validated concise explanation.",
        defaultVisible = false,
        maxDefaultActiveCards = 1,
        UiSlot.AiExplain,
        RequirementClass.EvenChessSpecific
      ),
      SurfaceContract(
        StableSurface.PostGameReview,
        "Rating-neutral review",
        "After game",
        "Deep review/drills.",
        defaultVisible = false,
        maxDefaultActiveCards = 1,
        UiSlot.Review,
        RequirementClass.AdaptedToLichessFork
      )
    )

    val bySurface: Map[StableSurface, SurfaceContract] =
      all.map(contract => contract.surface -> contract).toMap

  final case class TextBudget(surface: StableSurface, maxCharacters: Int, maxLines: Int)

  object TextBudgets:
    val budgets: Map[StableSurface, TextBudget] = Map(
      StableSurface.SummaryCard -> TextBudget(StableSurface.SummaryCard, maxCharacters = 120, maxLines = 2),
      StableSurface.WarningCard -> TextBudget(StableSurface.WarningCard, maxCharacters = 96, maxLines = 2),
      StableSurface.PlanCard -> TextBudget(StableSurface.PlanCard, maxCharacters = 160, maxLines = 3),
      StableSurface.OffsetCountCard -> TextBudget(StableSurface.OffsetCountCard, maxCharacters = 96, maxLines = 2),
      StableSurface.OpeningEndgameCard -> TextBudget(StableSurface.OpeningEndgameCard, maxCharacters = 140, maxLines = 3),
      StableSurface.CandidateArea -> TextBudget(StableSurface.CandidateArea, maxCharacters = 80, maxLines = 2),
      StableSurface.AiExplain -> TextBudget(StableSurface.AiExplain, maxCharacters = 220, maxLines = 4),
      StableSurface.PostGameReview -> TextBudget(StableSurface.PostGameReview, maxCharacters = 320, maxLines = 6)
    )

    val enforcedBySchemaNotAiDiscretion = true

    def withinBudget(surface: StableSurface, text: String, lineCount: Int): Boolean =
      budgets.get(surface).forall(budget => text.length <= budget.maxCharacters && lineCount <= budget.maxLines)

  object UiPrinciples:
    val boardFirst = true
    val oneActiveCardByDefault = true
    val onePrimaryVisualIdeaByDefault = true
    val mobileKeepsBoardPrimary = true
    val colorMayBeOnlySignal = false
    val accessibilityFeaturesChargedAsCoaching = false
    val clientNavigationIsAuthoritative = false

  enum VisualSignal:
    case Color
    case Icon
    case Text
    case Shape

  object AccessibilityRules:
    def hasRedundantSignal(signals: Set[VisualSignal]): Boolean =
      signals.size >= 2 && signals.exists(_ != VisualSignal.Color)

    def offsetCountHasRequiredRedundancy(signals: Set[VisualSignal]): Boolean =
      signals.contains(VisualSignal.Color) &&
        signals.contains(VisualSignal.Icon) &&
        signals.contains(VisualSignal.Text)

  final case class LiveLayoutContract(
      device: DeviceClass,
      boardPrimary: Boolean,
      layoutShape: String,
      levelControlsPosition: String,
      boardPosition: String,
      sideRailContents: List[String],
      boardActionsUnderBoard: Boolean,
      keyboardMoveEntryDefaultOnForNewAccounts: Boolean
  )

  object LiveLayouts:
    val desktop = LiveLayoutContract(
      DeviceClass.Desktop,
      boardPrimary = true,
      layoutShape = "three-column rectangle",
      levelControlsPosition = "left",
      boardPosition = "centre",
      sideRailContents = List("Moves", "Coach", "Chat", "Search"),
      boardActionsUnderBoard = true,
      keyboardMoveEntryDefaultOnForNewAccounts = false
    )

    val mobile = LiveLayoutContract(
      DeviceClass.Mobile,
      boardPrimary = true,
      layoutShape = "full-width board with bottom or adjacent card/sheet",
      levelControlsPosition = "tabs",
      boardPosition = "top",
      sideRailContents = List("Moves", "Coach", "Chat", "Search", "Level"),
      boardActionsUnderBoard = true,
      keyboardMoveEntryDefaultOnForNewAccounts = false
    )

    val evalBarNeedsAccessibleAlternative = true
    val sideCardsAlignWithBoardHeaderAndActions = true
    val boardHeaderIsCompactStatusSummary = true

  enum Perspective:
    case White
    case Black

  enum OverlayVisibility:
    case Hidden
    case Compact
    case Visible
    case Suppressed

  final case class OverlayPayload(
      gameId: String,
      ply: Int,
      boardStateKey: String,
      perspective: Perspective,
      featureKey: String,
      level: Level,
      visibility: OverlayVisibility,
      ttlMillis: Int,
      stale: Boolean,
      auditId: String,
      serverAuthorized: Boolean,
      approvedDisplayPayload: Boolean,
      rawStockfishLine: Option[String],
      hiddenDebugData: Option[String]
  ):
    def hasRequiredIdentityFields: Boolean =
      gameId.nonEmpty && ply >= 0 && boardStateKey.nonEmpty && featureKey.nonEmpty && auditId.nonEmpty

    def isRenderable: Boolean =
      hasRequiredIdentityFields &&
        serverAuthorized &&
        approvedDisplayPayload &&
        !stale &&
        ttlMillis > 0 &&
        visibility != OverlayVisibility.Suppressed

    def rawStockfishMayDisplay: Boolean =
      rawStockfishLine.isEmpty || approvedDisplayPayload

    def hiddenDebugMayDisplay: Boolean = false

  final case class BoardStateContext(
      gameId: String,
      ply: Int,
      boardStateKey: String,
      perspective: Perspective
  )

  enum ClearReason:
    case MovePlayed
    case BoardMismatch
    case StalePayload
    case Suppressed
    case PayloadExpired
    case None

  object OverlayInvalidation:
    def clearReason(
        payload: OverlayPayload,
        context: BoardStateContext,
        movePlayed: Boolean
    ): ClearReason =
      if movePlayed then ClearReason.MovePlayed
      else if payload.visibility == OverlayVisibility.Suppressed then ClearReason.Suppressed
      else if payload.stale then ClearReason.StalePayload
      else if payload.ttlMillis <= 0 then ClearReason.PayloadExpired
      else if payload.gameId != context.gameId || payload.ply != context.ply ||
        payload.boardStateKey != context.boardStateKey || payload.perspective != context.perspective
      then ClearReason.BoardMismatch
      else ClearReason.None

  object ClientCompositionGuard:
    val clientMayConstructStrongerHelpFromHiddenData = false
    val rawStockfishLinesMayDisplayWithoutServerPayload = false
    val overlaysMayInterfereWithMoveInput = false

    def blocksStrongerHelp(payloads: List[OverlayPayload]): Boolean =
      payloads.forall(payload => payload.hiddenDebugData.isEmpty || !payload.hiddenDebugMayDisplay)

  final case class LandingSection(key: String, contentSource: String)

  object LandingSurfaces:
    val requiredSections: List[LandingSection] = List(
      LandingSection("hero", "marketing_config"),
      LandingSection("trust_strip", "marketing_config"),
      LandingSection("difference", "marketing_config"),
      LandingSection("how_it_works", "marketing_config"),
      LandingSection("product_proof", "marketing_config"),
      LandingSection("pricing", "marketing_config"),
      LandingSection("faq", "marketing_config"),
      LandingSection("final_cta", "marketing_config")
    )

    val topCreateAccountRequired = true
    val topLoginRequired = true
    val pricingShowsFourWeekAmountFirst = true
    val pricingShowsWeeklyEquivalentSecond = true
    val pricingIncludesFairnessFootnote = true

    def allCopyComesFromMarketingConfig: Boolean =
      requiredSections.forall(_.contentSource == "marketing_config")

  object ReviewSurfaces:
    val resultAndTerminationAtTop = true
    val behavesLikePostGameBoardExperience = true
    val reviewLegalCoachingMustNotMutateLiveFairnessState = true

  enum DisplayRequirement:
    case PlayerSideOnly
    case NoSideSwitchingLive
    case ActualPositionVsProposedPreviewDistinct
    case StalePayloadsDoNotRenderCurrentAdvice
    case LevelGatedCardsAndOverlays
    case CompactTextBudgets
    case DeterministicOverlayMapping
    case DisplayEngineFrameworkOnly

  final case class DisplayRequirementClassification(
      requirement: DisplayRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object DisplayRequirementClassifications:
    val all: List[DisplayRequirementClassification] = List(
      DisplayRequirementClassification(
        DisplayRequirement.PlayerSideOnly,
        RequirementClass.EvenChessSpecific,
        "Live display is compiled from the requesting player's side output only."
      ),
      DisplayRequirementClassification(
        DisplayRequirement.NoSideSwitchingLive,
        RequirementClass.AdaptedToLichessFork,
        "The live display contract rejects side switching; review modes can add explicit separate policies later."
      ),
      DisplayRequirementClassification(
        DisplayRequirement.ActualPositionVsProposedPreviewDistinct,
        RequirementClass.EvenChessSpecific,
        "Display payloads carry a mode badge so actual-position output and proposed-move preview output cannot be confused."
      ),
      DisplayRequirementClassification(
        DisplayRequirement.StalePayloadsDoNotRenderCurrentAdvice,
        RequirementClass.EvenChessSpecific,
        "Stale, mismatched, expired, or suppressed payloads compile to clear-only decisions instead of current advice."
      ),
      DisplayRequirementClassification(
        DisplayRequirement.LevelGatedCardsAndOverlays,
        RequirementClass.EvenChessSpecific,
        "Summary, warning, plan, candidate, offset, threat, and pin surfaces are emitted only at allowed levels."
      ),
      DisplayRequirementClassification(
        DisplayRequirement.CompactTextBudgets,
        RequirementClass.EvenChessSpecific,
        "Text cards must fit schema budgets before they are eligible for display."
      ),
      DisplayRequirementClassification(
        DisplayRequirement.DeterministicOverlayMapping,
        RequirementClass.EvenChessSpecific,
        "Mock deterministic overlay atoms map to stable square markers and arrows without client-side chess interpretation."
      ),
      DisplayRequirementClassification(
        DisplayRequirement.DisplayEngineFrameworkOnly,
        RequirementClass.EvenChessSpecific,
        "This framework compiles approved/mock payloads only; live ECE calls and browser rendering remain later phases."
      )
    )

  enum DisplayMode:
    case ActualPosition
    case ProposedMovePreview

    def badge: String =
      this match
        case ActualPosition      => "Current position"
        case ProposedMovePreview => "Proposed move preview"

  enum MarkerKind:
    case HangingAttackable
    case StudentHangingAttackable
    case OpponentHangingAttackable
    case HangingNotAttackable
    case OffsetEqual
    case OffsetStudentFavorable
    case OffsetStudentUnfavorable
    case Pin

  final case class SquareMarker(
      square: String,
      kind: MarkerKind,
      color: String,
      icon: String,
      label: String
  ):
    def accessible: Boolean =
      square.nonEmpty && color.nonEmpty && icon.nonEmpty && label.nonEmpty

  enum ArrowKind:
    case StudentThreat
    case OpponentThreat
    case PinLine

  final case class BoardArrow(
      from: String,
      to: String,
      kind: ArrowKind,
      color: String,
      dashPattern: String,
      layer: Int,
      label: String
  ):
    def accessible: Boolean =
      from.nonEmpty &&
        to.nonEmpty &&
        color.nonEmpty &&
        dashPattern.nonEmpty &&
        layer >= 0 &&
        label.nonEmpty

  final case class DisplayCard(
      surface: StableSurface,
      title: String,
      body: String,
      level: Level,
      visibility: OverlayVisibility,
      auditId: String,
      mode: DisplayMode
  ):
    def lineCount: Int =
      math.max(1, body.linesIterator.size)

    def valid: Boolean =
      title.nonEmpty &&
        body.nonEmpty &&
        auditId.nonEmpty &&
        visibility != OverlayVisibility.Suppressed &&
        LevelDisplayGates.surfaceAllowed(surface, level) &&
        TextBudgets.withinBudget(surface, body, lineCount)

  object LevelDisplayGates:
    def surfaceAllowed(surface: StableSurface, level: Level): Boolean =
      surface match
        case StableSurface.BoardLayer         => level.value >= 1
        case StableSurface.SummaryCard       => level.value >= 4
        case StableSurface.WarningCard       => level.value >= 2
        case StableSurface.PlanCard          => level.value >= 4
        case StableSurface.OffsetCountCard   => level.value >= 3
        case StableSurface.OpeningEndgameCard => level.value >= 4
        case StableSurface.CandidateArea     => level.value >= 5
        case StableSurface.AiExplain         => level.value >= 8
        case StableSurface.PostGameReview    => true

  final case class MockDisplayOverlayAtoms(
      hangingAttackable: List[String],
      hangingNotAttackable: List[String],
      offsetCount: List[(String, Int)],
      studentThreats: List[(String, String)],
      opponentThreats: List[(String, String)],
      pins: List[(String, String, String)],
      studentHangingAttackable: List[String] = Nil,
      opponentHangingAttackable: List[String] = Nil
  ):
    def valid: Boolean =
      hangingAttackable.forall(_.nonEmpty) &&
        studentHangingAttackable.forall(_.nonEmpty) &&
        opponentHangingAttackable.forall(_.nonEmpty) &&
        hangingNotAttackable.forall(_.nonEmpty) &&
        offsetCount.forall { case (square, _) => square.nonEmpty } &&
        studentThreats.forall { case (from, to) => from.nonEmpty && to.nonEmpty } &&
        opponentThreats.forall { case (from, to) => from.nonEmpty && to.nonEmpty } &&
        pins.forall { case (pinned, pinning, target) => pinned.nonEmpty && pinning.nonEmpty && target.nonEmpty }

  object MockDisplayOverlayAtoms:
    val empty: MockDisplayOverlayAtoms =
      MockDisplayOverlayAtoms(Nil, Nil, Nil, Nil, Nil, Nil)

  final case class DisplayEngineRequest(
      gameId: String,
      playerId: String,
      ply: Int,
      boardStateKey: String,
      requesterSide: Perspective,
      sideOutput: EngineGateway.EceSideOutput,
      authorizedLevel: Level,
      displayMode: DisplayMode,
      atoms: MockDisplayOverlayAtoms,
      auditId: String,
      ttlMillis: Int,
      stale: Boolean,
      serverAuthorized: Boolean
  ):
    def sideMatchesRequester: Boolean =
      sideOutput.side == requesterSide && sideOutput.studentSide == requesterSide

    def levelAllowed: Boolean =
      sideOutput.level.deliveredLevel.value <= authorizedLevel.value

    def valid: Boolean =
      gameId.nonEmpty &&
        playerId.nonEmpty &&
        ply >= 0 &&
        boardStateKey.nonEmpty &&
        sideOutput.valid &&
        sideMatchesRequester &&
        levelAllowed &&
        atoms.valid &&
        auditId.nonEmpty &&
        ttlMillis > 0 &&
        serverAuthorized &&
        !stale

  final case class DisplayEngineResult(
      request: DisplayEngineRequest,
      overlay: Option[OverlayPayload],
      cards: List[DisplayCard],
      markers: List[SquareMarker],
      arrows: List[BoardArrow],
      clearReason: ClearReason,
      modeBadge: String,
      clientMaySwitchSide: Boolean,
      rawEcePayloadExposed: Boolean
  ):
    def renderable: Boolean =
      overlay.exists(_.isRenderable) &&
        clearReason == ClearReason.None &&
        cards.forall(_.valid) &&
        markers.forall(_.accessible) &&
        arrows.forall(_.accessible) &&
        !clientMaySwitchSide &&
        !rawEcePayloadExposed

    def clearOnly: Boolean =
      overlay.isEmpty && clearReason != ClearReason.None

  object DisplayEngine:
    def compile(request: DisplayEngineRequest): DisplayEngineResult =
      if !request.valid then
        DisplayEngineResult(
          request = request,
          overlay = None,
          cards = Nil,
          markers = Nil,
          arrows = Nil,
          clearReason = clearReasonForInvalid(request),
          modeBadge = request.displayMode.badge,
          clientMaySwitchSide = false,
          rawEcePayloadExposed = false
        )
      else
        val overlay = OverlayPayload(
          gameId = request.gameId,
          ply = request.ply,
          boardStateKey = request.boardStateKey,
          perspective = request.requesterSide,
          featureKey = displayFeatureKey(request.displayMode),
          level = request.sideOutput.level.deliveredLevel,
          visibility = OverlayVisibility.Visible,
          ttlMillis = request.ttlMillis,
          stale = false,
          auditId = request.auditId,
          serverAuthorized = true,
          approvedDisplayPayload = true,
          rawStockfishLine = None,
          hiddenDebugData = None
        )
        DisplayEngineResult(
          request = request,
          overlay = Some(overlay),
          cards = cardsFrom(request),
          markers = markersFrom(request),
          arrows = arrowsFrom(request),
          clearReason = ClearReason.None,
          modeBadge = request.displayMode.badge,
          clientMaySwitchSide = false,
          rawEcePayloadExposed = false
        )

    private def clearReasonForInvalid(request: DisplayEngineRequest): ClearReason =
      if request.stale then ClearReason.StalePayload
      else if request.ttlMillis <= 0 then ClearReason.PayloadExpired
      else if !request.sideMatchesRequester || request.boardStateKey.isEmpty then ClearReason.BoardMismatch
      else ClearReason.Suppressed

    private def displayFeatureKey(mode: DisplayMode): String =
      mode match
        case DisplayMode.ActualPosition      => "display_engine.actual_position"
        case DisplayMode.ProposedMovePreview => "display_engine.proposed_move_preview"

    private def cardsFrom(request: DisplayEngineRequest): List[DisplayCard] =
      val level = request.sideOutput.level.deliveredLevel
      List(
        request.sideOutput.summary.map(text =>
          DisplayCard(StableSurface.SummaryCard, "Summary", text, level, OverlayVisibility.Visible, request.auditId, request.displayMode)
        ),
        request.sideOutput.immediateWarning.map(text =>
          DisplayCard(StableSurface.WarningCard, "Warning", text, level, OverlayVisibility.Visible, request.auditId, request.displayMode)
        ),
        request.sideOutput.plan.map(text =>
          DisplayCard(StableSurface.PlanCard, "Plan", text, level, OverlayVisibility.Visible, request.auditId, request.displayMode)
        )
      ).flatten.filter(_.valid)

    private def markersFrom(request: DisplayEngineRequest): List[SquareMarker] =
      val level = request.sideOutput.level.deliveredLevel
      val hanging =
        if level.value >= 2 then
          request.atoms.studentHangingAttackable.map(square =>
            SquareMarker(square, MarkerKind.StudentHangingAttackable, "red", "!", "Student hanging and attackable")
          ) ++ request.atoms.opponentHangingAttackable.map(square =>
            SquareMarker(square, MarkerKind.OpponentHangingAttackable, "purple", "!", "Opponent hanging and attackable")
          ) ++ request.atoms.hangingAttackable.map(square =>
            SquareMarker(square, MarkerKind.HangingAttackable, "red", "!", "Hanging and attackable")
          ) ++ request.atoms.hangingNotAttackable.map(square =>
            SquareMarker(square, MarkerKind.HangingNotAttackable, "orange", "!", "Hanging")
          )
        else Nil
      val offset =
        if level.value >= 3 then
          request.atoms.offsetCount.map { case (square, value) =>
            if value == 0 then SquareMarker(square, MarkerKind.OffsetEqual, "blue", "shield", "Equal exchange")
            else if value > 0 then SquareMarker(square, MarkerKind.OffsetStudentFavorable, "green", value.toString, "Favorable exchange")
            else SquareMarker(square, MarkerKind.OffsetStudentUnfavorable, "red", math.abs(value).toString, "Unfavorable exchange")
          }
        else Nil
      val pins =
        if level.value >= 4 then
          request.atoms.pins.map { case (pinned, _, _) => SquareMarker(pinned, MarkerKind.Pin, "purple", "pin", "Pinned piece") }
        else Nil
      hanging ++ offset ++ pins

    private def arrowsFrom(request: DisplayEngineRequest): List[BoardArrow] =
      val level = request.sideOutput.level.deliveredLevel
      if level.value < 4 then Nil
      else
        val student = request.atoms.studentThreats.map { case (from, to) =>
          BoardArrow(from, to, ArrowKind.StudentThreat, "green", "dotted", layer = 1, "Student threat")
        }
        val opponent = request.atoms.opponentThreats.map { case (from, to) =>
          BoardArrow(from, to, ArrowKind.OpponentThreat, "red", "dotted-offset", layer = 2, "Opponent threat")
        }
        val pins = request.atoms.pins.map { case (_, pinning, target) =>
          BoardArrow(pinning, target, ArrowKind.PinLine, "purple", "solid", layer = 1, "Pin line")
        }
        student ++ opponent ++ pins
