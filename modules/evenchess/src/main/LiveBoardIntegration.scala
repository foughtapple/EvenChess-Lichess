package lila.evenchess

import ProductInvariants.RequirementClass

object LiveBoardIntegration:

  enum PhaseFIntegrationRequirement:
    case LichessRoundBoardAndInput
    case ServerAuthorizedRoundPayload
    case OptionalRoundDataField
    case RoundSocketThinAdapter
    case MoveClearsStaleOverlay
    case MobileBoardFirstLayout
    case NormalGameNoOverlay
    case RawEngineAndDebugBlocked

  final case class PhaseFIntegrationRequirementClassification(
      requirement: PhaseFIntegrationRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseFIntegrationRequirementClassifications:
    val all: List[PhaseFIntegrationRequirementClassification] = List(
      PhaseFIntegrationRequirementClassification(
        PhaseFIntegrationRequirement.LichessRoundBoardAndInput,
        RequirementClass.LichessProvided,
        "Use existing Lichess round/chessground legal move input, clocks, premoves, keyboard controls, and board sizing."
      ),
      PhaseFIntegrationRequirementClassification(
        PhaseFIntegrationRequirement.ServerAuthorizedRoundPayload,
        RequirementClass.EvenChessSpecific,
        "Only server-authorized EvenChess overlay payloads with audit identity can render on live boards."
      ),
      PhaseFIntegrationRequirementClassification(
        PhaseFIntegrationRequirement.OptionalRoundDataField,
        RequirementClass.AdaptedToLichessFork,
        "Expose an optional namespaced `evenchess` field on round data so normal games omit overlays entirely."
      ),
      PhaseFIntegrationRequirementClassification(
        PhaseFIntegrationRequirement.RoundSocketThinAdapter,
        RequirementClass.AdaptedToLichessFork,
        "Add one display-only round socket handler for server-owned EvenChess live overlay messages."
      ),
      PhaseFIntegrationRequirementClassification(
        PhaseFIntegrationRequirement.MoveClearsStaleOverlay,
        RequirementClass.AdaptedToLichessFork,
        "Clear current EvenChess overlay state on Lichess move/drop updates before rendering the new position."
      ),
      PhaseFIntegrationRequirementClassification(
        PhaseFIntegrationRequirement.MobileBoardFirstLayout,
        RequirementClass.AdaptedToLichessFork,
        "Place coaching cards in a non-overlapping grid area below or beside the board without owning board layout."
      ),
      PhaseFIntegrationRequirementClassification(
        PhaseFIntegrationRequirement.NormalGameNoOverlay,
        RequirementClass.EvenChessSpecific,
        "Normal non-EvenChess games do not receive the optional EvenChess round field or socket payloads."
      ),
      PhaseFIntegrationRequirementClassification(
        PhaseFIntegrationRequirement.RawEngineAndDebugBlocked,
        RequirementClass.EvenChessSpecific,
        "Round UI adapters must suppress payloads that carry raw Stockfish lines or hidden debug data."
      )
    )

  object AdapterContract:
    val roundDataKey = "evenchess"
    val livePayloadKey = "live"
    val socketEventType = "evenchessLive"
    val rootCssClass = "evenchess-live"
    val gridArea = "coach"
    val dataAttribute = "data-evenchess-overlay"
    val clientMayAuthorizeCoaching = false
    val lilaBoardOwnsMoveInput = true
    val maxDefaultCards = 1
    val maxPrimaryVisuals = 1

  final case class RoundPayloadIdentity(
      gameId: String,
      ply: Int,
      boardStateKey: String,
      perspective: String,
      auditId: String,
      serverAuthorized: Boolean
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        ply >= 0 &&
        boardStateKey.nonEmpty &&
        perspective.nonEmpty &&
        auditId.nonEmpty &&
        serverAuthorized

  final case class RoundCard(
      id: String,
      gameId: String,
      ply: Int,
      boardStateKey: String,
      featureKey: String,
      title: String,
      body: String,
      level: Int,
      auditId: String,
      defaultActive: Boolean,
      serverAuthorized: Boolean,
      approvedDisplayPayload: Boolean,
      stale: Boolean,
      ttlMillis: Int,
      rawStockfishLine: Option[String],
      hiddenDebugData: Option[String]
  ):
    def unsafePayload: Boolean =
      rawStockfishLine.exists(_.nonEmpty) || hiddenDebugData.exists(_.nonEmpty)

    def identityMatches(identity: RoundPayloadIdentity): Boolean =
      gameId == identity.gameId &&
        ply == identity.ply &&
        boardStateKey == identity.boardStateKey &&
        auditId == identity.auditId

    def renderable(identity: RoundPayloadIdentity): Boolean =
      id.nonEmpty &&
        featureKey.nonEmpty &&
        title.nonEmpty &&
        body.nonEmpty &&
        level >= 0 &&
        identityMatches(identity) &&
        serverAuthorized &&
        approvedDisplayPayload &&
        !stale &&
        ttlMillis > 0 &&
        !unsafePayload

  final case class RoundVisual(
      id: String,
      gameId: String,
      ply: Int,
      boardStateKey: String,
      featureKey: String,
      label: String,
      auditId: String,
      primary: Boolean,
      serverAuthorized: Boolean,
      approvedDisplayPayload: Boolean,
      stale: Boolean,
      rawStockfishLine: Option[String],
      hiddenDebugData: Option[String],
      evalCpWhite: Option[Int] = None,
      evalMateWhite: Option[Int] = None,
      evalWinWhite: Option[Int] = None,
      evalDrawWhite: Option[Int] = None,
      evalLossWhite: Option[Int] = None,
      evalSource: Option[String] = None
  ):
    def unsafePayload: Boolean =
      rawStockfishLine.exists(_.nonEmpty) || hiddenDebugData.exists(_.nonEmpty)

    def identityMatches(identity: RoundPayloadIdentity): Boolean =
      gameId == identity.gameId &&
        ply == identity.ply &&
        boardStateKey == identity.boardStateKey &&
        auditId == identity.auditId

    def renderable(identity: RoundPayloadIdentity): Boolean =
      id.nonEmpty &&
        featureKey.nonEmpty &&
        label.nonEmpty &&
        identityMatches(identity) &&
        serverAuthorized &&
        approvedDisplayPayload &&
        !stale &&
        !unsafePayload

  final case class RoundLivePayload(
      enabled: Boolean,
      identity: RoundPayloadIdentity,
      cards: List[RoundCard],
      visuals: List[RoundVisual],
      clearReason: Option[String]
  ):
    def renderableCards: List[RoundCard] =
      if !enabled || !identity.valid then Nil
      else cards.filter(_.renderable(identity)).sortBy(card => !card.defaultActive).take(AdapterContract.maxDefaultCards)

    def renderableVisuals: List[RoundVisual] =
      if !enabled || !identity.valid then Nil
      else visuals.filter(_.renderable(identity)).sortBy(visual => !visual.primary).take(AdapterContract.maxPrimaryVisuals)

    def clearOnly: Boolean =
      enabled &&
        identity.valid &&
        clearReason.exists(_.nonEmpty) &&
        cards.isEmpty &&
        visuals.isEmpty

    def hasOnlyServerApprovedContent: Boolean =
      cards.forall(card =>
        card.serverAuthorized &&
          card.approvedDisplayPayload &&
          !card.unsafePayload &&
          card.identityMatches(identity)
      ) &&
        visuals.forall(visual =>
          visual.serverAuthorized &&
            visual.approvedDisplayPayload &&
            !visual.unsafePayload &&
            visual.identityMatches(identity)
        )

    def valid: Boolean =
      (!enabled && cards.isEmpty && visuals.isEmpty) ||
        (identity.valid &&
          hasOnlyServerApprovedContent &&
          renderableCards.size <= AdapterContract.maxDefaultCards &&
          renderableVisuals.size <= AdapterContract.maxPrimaryVisuals)

    def normalGamesShouldOmitPayload: Boolean =
      !enabled && cards.isEmpty && visuals.isEmpty && clearReason.isEmpty
