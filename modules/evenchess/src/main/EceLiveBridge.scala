package lila.evenchess

import CoachingLadder.Level
import CoachingOverlays.*
import EngineGateway.*
import LiveBoardIntegration.*

object EceLiveBridge:

  final case class ExtraCard(
      featureKey: String,
      title: String,
      body: String,
      defaultActive: Boolean = false
  ):
    def valid: Boolean =
      featureKey.nonEmpty && title.nonEmpty && body.nonEmpty

  final case class ExtraVisual(
      featureKey: String,
      label: String,
      primary: Boolean = false,
      evalCpWhite: Option[Int] = None,
      evalMateWhite: Option[Int] = None,
      evalWinWhite: Option[Int] = None,
      evalDrawWhite: Option[Int] = None,
      evalLossWhite: Option[Int] = None,
      evalSource: Option[String] = None
  ):
    def valid: Boolean =
      featureKey.nonEmpty && label.nonEmpty

  final case class BoardOverlayResult(
      decision: EceFrameworkDecision,
      display: Option[DisplayEngineResult],
      roundPayload: RoundLivePayload
  ):
    def valid: Boolean =
      decision.valid &&
        roundPayload.valid &&
        display.forall(result => result.renderable || result.clearOnly)

    def coachingAvailable: Boolean =
      decision.coachingAvailable &&
        (roundPayload.renderableCards.nonEmpty || roundPayload.renderableVisuals.nonEmpty)

  def compileBoardOverlay(
      config: EceServiceConfig,
      gameId: String,
      playerId: String,
      ply: Int,
      boardStateKey: String,
      requesterSide: Perspective,
      authorizedLevel: Level,
      request: EceBoardStateRequest,
      currentFen: String,
      response: EceBoardStateResponse,
      atoms: MockDisplayOverlayAtoms,
      auditId: String,
      ttlMillis: Int,
      extraCards: List[ExtraCard] = Nil,
      extraVisuals: List[ExtraVisual] = Nil
  ): BoardOverlayResult =
    val outstanding = EceOutstandingRequest(request, currentFen)
    val decision =
      EceFrameworkIntegration.acceptBoardState(config, outstanding, response, requesterSide)

    decision.displayableForRequester match
      case Some(sideOutput) =>
        val displayRequest =
          DisplayEngineRequest(
            gameId = gameId,
            playerId = playerId,
            ply = ply,
            boardStateKey = boardStateKey,
            requesterSide = requesterSide,
            sideOutput = sideOutput,
            authorizedLevel = authorizedLevel,
            displayMode = DisplayMode.ActualPosition,
            atoms = atoms,
            auditId = auditId,
            ttlMillis = ttlMillis,
            stale = false,
            serverAuthorized = true
          )
        val display = DisplayEngine.compile(displayRequest)
        BoardOverlayResult(
          decision = decision,
          display = Some(display),
          roundPayload = roundPayloadFromDisplay(display, extraCards.filter(_.valid), extraVisuals.filter(_.valid))
        )

      case None =>
        BoardOverlayResult(
          decision = decision,
          display = None,
          roundPayload = clearPayload(gameId, ply, boardStateKey, requesterSide, auditId, decision.status.toString)
        )

  def compileProposedMovePreview(
      gameId: String,
      playerId: String,
      ply: Int,
      boardStateKey: String,
      requesterSide: Perspective,
      authorizedLevel: Level,
      sideOutput: EceSideOutput,
      atoms: MockDisplayOverlayAtoms,
      auditId: String,
      ttlMillis: Int,
      extraCards: List[ExtraCard] = Nil,
      extraVisuals: List[ExtraVisual] = Nil
  ): RoundLivePayload =
    val displayRequest =
      DisplayEngineRequest(
        gameId = gameId,
        playerId = playerId,
        ply = ply,
        boardStateKey = boardStateKey,
        requesterSide = requesterSide,
        sideOutput = sideOutput,
        authorizedLevel = authorizedLevel,
        displayMode = DisplayMode.ProposedMovePreview,
        atoms = atoms,
        auditId = auditId,
        ttlMillis = ttlMillis,
        stale = false,
        serverAuthorized = true
      )
    val display = DisplayEngine.compile(displayRequest)
    roundPayloadFromDisplay(display, extraCards.filter(_.valid), extraVisuals.filter(_.valid))

  def compileStoredBoardFrame(
      gameId: String,
      playerId: String,
      ply: Int,
      boardStateKey: String,
      requesterSide: Perspective,
      authorizedLevel: Level,
      sideOutput: EceSideOutput,
      atoms: MockDisplayOverlayAtoms,
      auditId: String,
      ttlMillis: Int,
      extraCards: List[ExtraCard] = Nil,
      extraVisuals: List[ExtraVisual] = Nil
  ): RoundLivePayload =
    val displayRequest =
      DisplayEngineRequest(
        gameId = gameId,
        playerId = playerId,
        ply = ply,
        boardStateKey = boardStateKey,
        requesterSide = requesterSide,
        sideOutput = sideOutput,
        authorizedLevel = authorizedLevel,
        displayMode = DisplayMode.ActualPosition,
        atoms = atoms,
        auditId = auditId,
        ttlMillis = ttlMillis,
        stale = false,
        serverAuthorized = true
      )
    val display = DisplayEngine.compile(displayRequest)
    roundPayloadFromDisplay(display, extraCards.filter(_.valid), extraVisuals.filter(_.valid))

  private def roundPayloadFromDisplay(
      display: DisplayEngineResult,
      extraCards: List[ExtraCard],
      extraVisuals: List[ExtraVisual]
  ): RoundLivePayload =
    val identity = RoundPayloadIdentity(
      gameId = display.request.gameId,
      ply = display.request.ply,
      boardStateKey = display.request.boardStateKey,
      perspective = perspectiveKey(display.request.requesterSide),
      auditId = display.request.auditId,
      serverAuthorized = true
    )

    if display.clearReason != ClearReason.None || display.overlay.isEmpty then
      RoundLivePayload(
        enabled = true,
        identity = identity,
        cards = Nil,
        visuals = Nil,
        clearReason = Some(display.clearReason.toString)
      )
    else
      val overlay = display.overlay.get
      val allowedExtraCards = extraCards.filter(card => extraCardAllowed(card, overlay.level))
      val allowedExtraVisuals = extraVisuals.filter(visual => extraVisualAllowed(visual, overlay.level))
      RoundLivePayload(
        enabled = true,
        identity = identity,
        cards =
          display.cards.zipWithIndex.map { case (card, index) =>
            roundCard(identity, overlay, card, defaultActive = index == 0)
          } ++ allowedExtraCards.zipWithIndex.map { case (card, index) =>
            extraRoundCard(identity, overlay, card, index)
          },
        visuals =
          (display.markers.map(marker => markerVisual(marker)) ++ display.arrows.map(arrow => arrowVisual(arrow))).zipWithIndex.map {
            case ((featureKey, label), index) =>
              RoundVisual(
                id = s"ece-visual-${index}-${safeKey(identity.auditId)}",
                gameId = identity.gameId,
                ply = identity.ply,
                boardStateKey = identity.boardStateKey,
                featureKey = featureKey,
                label = label,
                auditId = identity.auditId,
                primary = index == 0,
                serverAuthorized = overlay.serverAuthorized,
                approvedDisplayPayload = overlay.approvedDisplayPayload,
                stale = overlay.stale,
                rawStockfishLine = None,
                hiddenDebugData = None
              )
          } ++ allowedExtraVisuals.zipWithIndex.map { case (visual, index) =>
            extraRoundVisual(identity, overlay, visual, index)
          },
        clearReason = None
      )

  private def clearPayload(
      gameId: String,
      ply: Int,
      boardStateKey: String,
      requesterSide: Perspective,
      auditId: String,
      reason: String
  ): RoundLivePayload =
    RoundLivePayload(
      enabled = true,
      identity = RoundPayloadIdentity(
        gameId = gameId,
        ply = ply,
        boardStateKey = boardStateKey,
        perspective = perspectiveKey(requesterSide),
        auditId = auditId,
        serverAuthorized = true
      ),
      cards = Nil,
      visuals = Nil,
      clearReason = Some(reason)
    )

  private def roundCard(
      identity: RoundPayloadIdentity,
      overlay: OverlayPayload,
      card: DisplayCard,
      defaultActive: Boolean
  ): RoundCard =
    RoundCard(
      id = s"ece-card-${safeKey(card.surface.toString)}-${safeKey(identity.auditId)}",
      gameId = identity.gameId,
      ply = identity.ply,
      boardStateKey = identity.boardStateKey,
      featureKey = s"ece.card.${safeKey(card.surface.toString)}",
      title = card.title,
      body = card.body,
      level = card.level.value,
      auditId = identity.auditId,
      defaultActive = defaultActive,
      serverAuthorized = overlay.serverAuthorized,
      approvedDisplayPayload = overlay.approvedDisplayPayload,
      stale = overlay.stale,
      ttlMillis = overlay.ttlMillis,
      rawStockfishLine = None,
      hiddenDebugData = None
    )

  private def extraRoundCard(
      identity: RoundPayloadIdentity,
      overlay: OverlayPayload,
      card: ExtraCard,
      index: Int
  ): RoundCard =
    RoundCard(
      id = s"ece-extra-card-${index}-${safeKey(identity.auditId)}",
      gameId = identity.gameId,
      ply = identity.ply,
      boardStateKey = identity.boardStateKey,
      featureKey = card.featureKey,
      title = card.title,
      body = card.body,
      level = overlay.level.value,
      auditId = identity.auditId,
      defaultActive = card.defaultActive,
      serverAuthorized = overlay.serverAuthorized,
      approvedDisplayPayload = overlay.approvedDisplayPayload,
      stale = overlay.stale,
      ttlMillis = overlay.ttlMillis,
      rawStockfishLine = None,
      hiddenDebugData = None
    )

  private def extraRoundVisual(
      identity: RoundPayloadIdentity,
      overlay: OverlayPayload,
      visual: ExtraVisual,
      index: Int
  ): RoundVisual =
    RoundVisual(
      id = s"ece-extra-visual-${index}-${safeKey(identity.auditId)}",
      gameId = identity.gameId,
      ply = identity.ply,
      boardStateKey = identity.boardStateKey,
      featureKey = visual.featureKey,
      label = visual.label,
      auditId = identity.auditId,
      primary = visual.primary,
      serverAuthorized = overlay.serverAuthorized,
      approvedDisplayPayload = overlay.approvedDisplayPayload,
      stale = overlay.stale,
      rawStockfishLine = None,
      hiddenDebugData = None,
      evalCpWhite = visual.evalCpWhite,
      evalMateWhite = visual.evalMateWhite,
      evalWinWhite = visual.evalWinWhite,
      evalDrawWhite = visual.evalDrawWhite,
      evalLossWhite = visual.evalLossWhite,
      evalSource = visual.evalSource
    )

  private def markerVisual(marker: SquareMarker): (String, String) =
    val featureKey = marker.kind match
      case MarkerKind.HangingAttackable          => "ece.marker.hanging_attackable.student"
      case MarkerKind.StudentHangingAttackable   => "ece.marker.hanging_attackable.student"
      case MarkerKind.OpponentHangingAttackable  => "ece.marker.hanging_attackable.opponent"
      case MarkerKind.HangingNotAttackable       => "ece.marker.hanging_not_attackable"
      case MarkerKind.OffsetEqual                => "ece.marker.offset_count.equal"
      case MarkerKind.OffsetStudentFavorable     => "ece.marker.offset_count.student_win"
      case MarkerKind.OffsetStudentUnfavorable => "ece.marker.offset_count.opponent_win"
      case MarkerKind.Pin                        => "ece.marker.pin"
    val label = marker.kind match
      case MarkerKind.OffsetEqual =>
        s"${marker.square}: Offset Count 0"
      case MarkerKind.OffsetStudentFavorable =>
        s"${marker.square}: Offset Count ${marker.icon}"
      case MarkerKind.OffsetStudentUnfavorable =>
        s"${marker.square}: Offset Count -${marker.icon}"
      case _ =>
        s"${marker.square}: ${marker.label}"
    featureKey -> label

  private def arrowVisual(arrow: BoardArrow): (String, String) =
    val featureKey = arrow.kind match
      case ArrowKind.StudentThreat  => "ece.arrow.student_threat"
      case ArrowKind.OpponentThreat => "ece.arrow.opponent_threat"
      case ArrowKind.PinLine        => "ece.arrow.pin_line"
    featureKey -> s"${arrow.from}-${arrow.to}: ${arrow.label}"

  private def perspectiveKey(perspective: Perspective): String =
    perspective match
      case Perspective.White => "white"
      case Perspective.Black => "black"

  private def extraCardAllowed(card: ExtraCard, level: Level): Boolean =
    card.featureKey match
      case key if key.startsWith("ece.candidate.")          => level.value >= 5
      case "ece.proposed_move_preview" | "ece.review_modes" => level.value >= 5
      case "ece.opening"                                    => level.value >= 4
      case _                                                => level.value >= 4

  private def extraVisualAllowed(visual: ExtraVisual, level: Level): Boolean =
    visual.featureKey match
      case key if key.startsWith("ece.eval")               => level.value >= 8
      case key if key.startsWith("ece.candidate.")         => level.value >= 5
      case key if key.startsWith("ece.potential.")         => level.value >= 5
      case _                                               => level.value >= 4

  private def safeKey(value: String): String =
    value.replaceAll("[^A-Za-z0-9_.-]", "-").toLowerCase
