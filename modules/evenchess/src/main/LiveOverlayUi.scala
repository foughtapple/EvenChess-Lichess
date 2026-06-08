package lila.evenchess

import CoachingLadder.{ FeatureRegistry, Level, UiSlot }
import CoachingOverlays.{ AccessibilityRules, BoardStateContext, ClearReason, OverlayPayload, OverlayVisibility, StableSurface, TextBudgets, UiPrinciples, VisualSignal }
import LiveCoaching.LiveTransportEnvelope
import OffsetCount.{ OffsetCountPayload, ResultState }
import ProductInvariants.RequirementClass

object LiveOverlayUi:

  enum PhaseFRequirement:
    case BaseBoardUiProvidedByLichess
    case ServerAuthorizedOverlayCards
    case TextBudgetsSchemaEnforced
    case OffsetCountCardSemantics
    case AccessibilityRedundancy
    case ClientDisplayOnly
    case OneActiveCardDefault
    case CoreUiAdapterDeferredToThinSeams

  final case class PhaseFRequirementClassification(
      requirement: PhaseFRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseFRequirementClassifications:
    val all: List[PhaseFRequirementClassification] = List(
      PhaseFRequirementClassification(
        PhaseFRequirement.BaseBoardUiProvidedByLichess,
        RequirementClass.LichessProvided,
        "Use Lichess/chessground as the board and move-input surface; EvenChess only supplies server-authorized overlays."
      ),
      PhaseFRequirementClassification(
        PhaseFRequirement.ServerAuthorizedOverlayCards,
        RequirementClass.EvenChessSpecific,
        "Coaching cards are view models derived from server-approved overlay transport and audit IDs."
      ),
      PhaseFRequirementClassification(
        PhaseFRequirement.TextBudgetsSchemaEnforced,
        RequirementClass.EvenChessSpecific,
        "Card renderability depends on schema text budgets, not AI discretion."
      ),
      PhaseFRequirementClassification(
        PhaseFRequirement.OffsetCountCardSemantics,
        RequirementClass.AdaptedToLichessFork,
        "Offset Count renders the existing Exchange Resolver semantics as an EvenChess card."
      ),
      PhaseFRequirementClassification(
        PhaseFRequirement.AccessibilityRedundancy,
        RequirementClass.EvenChessSpecific,
        "Cards and markers must not use color as the only signal; Offset Count requires color, icon, and text."
      ),
      PhaseFRequirementClassification(
        PhaseFRequirement.ClientDisplayOnly,
        RequirementClass.EvenChessSpecific,
        "Client card navigation is display-only and does not alter coaching permission or assistance accounting."
      ),
      PhaseFRequirementClassification(
        PhaseFRequirement.OneActiveCardDefault,
        RequirementClass.EvenChessSpecific,
        "The default frame exposes at most one active coaching card and one primary board visual."
      ),
      PhaseFRequirementClassification(
        PhaseFRequirement.CoreUiAdapterDeferredToThinSeams,
        RequirementClass.AdaptedToLichessFork,
        "Later phases may attach this model to lila/chessground through a small patch-mapped UI adapter."
      )
    )

  enum CardKind:
    case Summary
    case Warning
    case Plan
    case OffsetCount
    case Candidate
    case AiExplain
    case Review

  enum CardDisplayState:
    case Collapsed
    case Expanded
    case Disabled

  enum BoardVisualKind:
    case LegalTargets
    case WarningBadge
    case CandidateArrow
    case OffsetMarker
    case EvalChip

  final case class CoachCard(
      id: String,
      gameId: String,
      ply: Int,
      boardStateKey: String,
      surface: StableSurface,
      kind: CardKind,
      featureKey: String,
      title: String,
      body: String,
      lineCount: Int,
      level: Level,
      visibility: OverlayVisibility,
      displayState: CardDisplayState,
      defaultActive: Boolean,
      assistanceCounts: Boolean,
      auditId: String,
      ttlMillis: Int,
      signals: Set[VisualSignal],
      serverAuthorized: Boolean,
      approvedDisplayPayload: Boolean
  ):
    def hasRequiredIdentity: Boolean =
      id.nonEmpty &&
        gameId.nonEmpty &&
        ply >= 0 &&
        boardStateKey.nonEmpty &&
        featureKey.nonEmpty &&
        auditId.nonEmpty

    def withinTextBudget: Boolean =
      TextBudgets.withinBudget(surface, body, lineCount)

    def hasAccessibleSignals: Boolean =
      if kind == CardKind.OffsetCount then AccessibilityRules.offsetCountHasRequiredRedundancy(signals)
      else !signals.contains(VisualSignal.Color) || AccessibilityRules.hasRedundantSignal(signals)

    def renderable: Boolean =
      hasRequiredIdentity &&
        serverAuthorized &&
        approvedDisplayPayload &&
        withinTextBudget &&
        hasAccessibleSignals &&
        ttlMillis > 0 &&
        visibility != OverlayVisibility.Suppressed

  final case class BoardVisual(
      id: String,
      gameId: String,
      ply: Int,
      boardStateKey: String,
      kind: BoardVisualKind,
      featureKey: String,
      level: Level,
      auditId: String,
      primary: Boolean,
      signals: Set[VisualSignal],
      serverAuthorized: Boolean,
      approvedDisplayPayload: Boolean
  ):
    def renderable: Boolean =
      id.nonEmpty &&
        gameId.nonEmpty &&
        ply >= 0 &&
        boardStateKey.nonEmpty &&
        featureKey.nonEmpty &&
        auditId.nonEmpty &&
        serverAuthorized &&
        approvedDisplayPayload &&
        (!signals.contains(VisualSignal.Color) || AccessibilityRules.hasRedundantSignal(signals)) &&
        !CoachingOverlays.ClientCompositionGuard.overlaysMayInterfereWithMoveInput

  final case class ClearInstruction(
      gameId: String,
      ply: Int,
      boardStateKey: String,
      reason: ClearReason,
      auditId: String
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        ply >= 0 &&
        boardStateKey.nonEmpty &&
        reason != ClearReason.None &&
        auditId.nonEmpty

  final case class OverlayFrame(
      boardState: BoardStateContext,
      cards: List[CoachCard],
      visuals: List[BoardVisual],
      clearInstructions: List[ClearInstruction],
      boardPrimary: Boolean,
      clientNavigationDisplayOnly: Boolean
  ):
    def activeCards: List[CoachCard] = cards.filter(card => card.defaultActive && card.renderable)

    def primaryVisuals: List[BoardVisual] = visuals.filter(visual => visual.primary && visual.renderable)

    def valid: Boolean =
      boardPrimary &&
        clientNavigationDisplayOnly &&
        activeCards.size <= 1 &&
        primaryVisuals.size <= 1 &&
        cards.forall(_.renderable) &&
        visuals.forall(_.renderable) &&
        clearInstructions.forall(_.valid)

  object OverlayFrame:
    def empty(boardState: BoardStateContext): OverlayFrame =
      OverlayFrame(
        boardState = boardState,
        cards = Nil,
        visuals = Nil,
        clearInstructions = Nil,
        boardPrimary = UiPrinciples.boardFirst,
        clientNavigationDisplayOnly = !UiPrinciples.clientNavigationIsAuthoritative
      )

  object SurfaceMapping:
    def fromUiSlot(uiSlot: UiSlot): StableSurface =
      uiSlot match
        case UiSlot.Board | UiSlot.BoardLayer => StableSurface.BoardLayer
        case UiSlot.MoveList | UiSlot.ResultState | UiSlot.Summary => StableSurface.SummaryCard
        case UiSlot.Warning => StableSurface.WarningCard
        case UiSlot.OffsetCard => StableSurface.OffsetCountCard
        case UiSlot.PlanCard => StableSurface.PlanCard
        case UiSlot.OpeningEndgameCard => StableSurface.OpeningEndgameCard
        case UiSlot.CandidateArea => StableSurface.CandidateArea
        case UiSlot.AiExplain => StableSurface.AiExplain
        case UiSlot.CoachReview | UiSlot.Review => StableSurface.PostGameReview

  object OffsetCountCard:
    def fromPayload(
        payload: OffsetCountPayload,
        overlay: OverlayPayload
    ): Option[CoachCard] =
      Option.when(compatible(payload, overlay)) {
        val display = displayText(payload)
        CoachCard(
          id = s"offset-count-${payload.gameId}-${payload.ply}-${payload.square}-${payload.auditId}",
          gameId = payload.gameId,
          ply = payload.ply,
          boardStateKey = payload.boardStateKey,
          surface = StableSurface.OffsetCountCard,
          kind = CardKind.OffsetCount,
          featureKey = OffsetCount.featureKey,
          title = "Offset Count",
          body = display,
          lineCount = 1,
          level = overlay.level,
          visibility = overlay.visibility,
          displayState =
            if payload.resultState == ResultState.Unknown then CardDisplayState.Disabled
            else CardDisplayState.Collapsed,
          defaultActive = overlay.visibility == OverlayVisibility.Visible,
          assistanceCounts = payload.assistanceCounts && payload.isKnown,
          auditId = payload.auditId,
          ttlMillis = overlay.ttlMillis,
          signals = Set(VisualSignal.Color, VisualSignal.Icon, VisualSignal.Text),
          serverAuthorized = overlay.serverAuthorized && payload.serverAuthorized,
          approvedDisplayPayload = overlay.approvedDisplayPayload
        )
      }

    def displayText(payload: OffsetCountPayload): String =
      payload.resultState match
        case ResultState.Equal => "Equal trade"
        case ResultState.StudentWins =>
          s"You win ${pieceText(payload.displayCount)}"
        case ResultState.OpponentWins =>
          s"Opponent wins ${pieceText(payload.displayCount)}"
        case ResultState.Unknown => "Exchange unknown"

    def compatible(payload: OffsetCountPayload, overlay: OverlayPayload): Boolean =
      payload.featureKey == OffsetCount.featureKey &&
        overlay.featureKey == OffsetCount.featureKey &&
        payload.gameId == overlay.gameId &&
        payload.ply == overlay.ply &&
        payload.boardStateKey == overlay.boardStateKey &&
        payload.auditId == overlay.auditId &&
        payload.serverAuthorized &&
        overlay.isRenderable

    private def pieceText(count: Int): String =
      if count == 1 then "1 piece" else s"$count pieces"

  object FrameComposer:
    def fromTransport(
        envelope: LiveTransportEnvelope,
        offsetPayload: Option[OffsetCountPayload] = None
    ): OverlayFrame =
      val boardState = BoardStateContext(
        gameId = envelope.gameId,
        ply = envelope.ply,
        boardStateKey = envelope.boardStateKey,
        perspective = envelope.perspective
      )
      val empty = OverlayFrame.empty(boardState)

      envelope.overlay match
        case None =>
          if envelope.clearReason == ClearReason.None then empty
          else
            empty.copy(clearInstructions = List(clearInstruction(envelope)))

        case Some(overlay) if !envelope.approvedForClientTransport || !overlay.isRenderable =>
          empty.copy(clearInstructions = List(clearInstruction(envelope, ClearReason.Suppressed)))

        case Some(overlay) if overlay.featureKey == OffsetCount.featureKey =>
          offsetPayload.flatMap(OffsetCountCard.fromPayload(_, overlay)) match
            case Some(card) => empty.copy(cards = List(card))
            case None       => empty.copy(clearInstructions = List(clearInstruction(envelope, ClearReason.Suppressed)))

        case Some(overlay) =>
          FeatureRegistry.byKey.get(overlay.featureKey) match
            case Some(row) if SurfaceMapping.fromUiSlot(row.uiSlot) == StableSurface.BoardLayer =>
              empty.copy(visuals = List(boardVisualFrom(overlay)))
            case Some(row) =>
              empty.copy(cards = List(cardFrom(overlay, SurfaceMapping.fromUiSlot(row.uiSlot), row.displayName)))
            case None =>
              empty.copy(clearInstructions = List(clearInstruction(envelope, ClearReason.Suppressed)))

    private def cardFrom(
        overlay: OverlayPayload,
        surface: StableSurface,
        displayName: String
    ): CoachCard =
      CoachCard(
        id = s"coach-card-${overlay.gameId}-${overlay.ply}-${overlay.featureKey}-${overlay.auditId}",
        gameId = overlay.gameId,
        ply = overlay.ply,
        boardStateKey = overlay.boardStateKey,
        surface = surface,
        kind = cardKind(surface),
        featureKey = overlay.featureKey,
        title = displayName,
        body = displayName,
        lineCount = 1,
        level = overlay.level,
        visibility = overlay.visibility,
        displayState = CardDisplayState.Collapsed,
        defaultActive = overlay.visibility == OverlayVisibility.Visible,
        assistanceCounts = overlay.visibility == OverlayVisibility.Visible,
        auditId = overlay.auditId,
        ttlMillis = overlay.ttlMillis,
        signals = Set(VisualSignal.Text, VisualSignal.Icon),
        serverAuthorized = overlay.serverAuthorized,
        approvedDisplayPayload = overlay.approvedDisplayPayload
      )

    private def boardVisualFrom(overlay: OverlayPayload): BoardVisual =
      BoardVisual(
        id = s"board-visual-${overlay.gameId}-${overlay.ply}-${overlay.featureKey}-${overlay.auditId}",
        gameId = overlay.gameId,
        ply = overlay.ply,
        boardStateKey = overlay.boardStateKey,
        kind = BoardVisualKind.LegalTargets,
        featureKey = overlay.featureKey,
        level = overlay.level,
        auditId = overlay.auditId,
        primary = true,
        signals = Set(VisualSignal.Shape, VisualSignal.Icon),
        serverAuthorized = overlay.serverAuthorized,
        approvedDisplayPayload = overlay.approvedDisplayPayload
      )

    private def clearInstruction(
        envelope: LiveTransportEnvelope,
        reasonOverride: ClearReason = ClearReason.None
    ): ClearInstruction =
      ClearInstruction(
        gameId = envelope.gameId,
        ply = envelope.ply,
        boardStateKey = envelope.boardStateKey,
        reason = if reasonOverride == ClearReason.None then envelope.clearReason else reasonOverride,
        auditId = envelope.auditId
      )

    private def cardKind(surface: StableSurface): CardKind =
      surface match
        case StableSurface.SummaryCard => CardKind.Summary
        case StableSurface.WarningCard => CardKind.Warning
        case StableSurface.PlanCard => CardKind.Plan
        case StableSurface.OffsetCountCard => CardKind.OffsetCount
        case StableSurface.CandidateArea => CardKind.Candidate
        case StableSurface.AiExplain => CardKind.AiExplain
        case StableSurface.PostGameReview => CardKind.Review
        case StableSurface.OpeningEndgameCard => CardKind.Plan
        case StableSurface.BoardLayer => CardKind.Summary

  object DisplayGuards:
    val baseBoardProvidedByLichess = true
    val clientMayNotAuthorizeCards = true
    val liveCardsDoNotShowRawStockfishLines = true
    val hiddenDebugDataNotRendered = true
    val keyboardAndDragInputRemainLichessOwned = true

    def frameKeepsDefaultLimits(frame: OverlayFrame): Boolean =
      frame.activeCards.size <= 1 && frame.primaryVisuals.size <= 1

    def cardsKeepRawEngineDataOut(cards: List[CoachCard]): Boolean =
      cards.forall: card =>
        val normalized = card.body.toLowerCase
        !normalized.contains("stockfish") &&
          !normalized.contains("multipv") &&
          !normalized.startsWith("pv ") &&
          !normalized.contains(" pv ")
