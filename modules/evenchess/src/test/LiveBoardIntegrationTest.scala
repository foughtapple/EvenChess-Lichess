package lila.evenchess

class LiveBoardIntegrationTest extends munit.FunSuite:

  import LiveBoardIntegration.*
  import ProductInvariants.RequirementClass

  private val identity = RoundPayloadIdentity(
    gameId = "game-live",
    ply = 18,
    boardStateKey = "fen-or-board-key-18",
    perspective = "white",
    auditId = "audit-live-18",
    serverAuthorized = true
  )

  private val card = RoundCard(
    id = "card-1",
    gameId = identity.gameId,
    ply = identity.ply,
    boardStateKey = identity.boardStateKey,
    featureKey = "offset_count",
    title = "Offset Count",
    body = "Equal trade",
    level = 3,
    auditId = identity.auditId,
    defaultActive = true,
    serverAuthorized = true,
    approvedDisplayPayload = true,
    stale = false,
    ttlMillis = 5000,
    rawStockfishLine = None,
    hiddenDebugData = None
  )

  private val visual = RoundVisual(
    id = "visual-1",
    gameId = identity.gameId,
    ply = identity.ply,
    boardStateKey = identity.boardStateKey,
    featureKey = "offset_count",
    label = "Exchange marker",
    auditId = identity.auditId,
    primary = true,
    serverAuthorized = true,
    approvedDisplayPayload = true,
    stale = false,
    rawStockfishLine = None,
    hiddenDebugData = None
  )

  test("Version 1.2 Phase F live-board requirements are classified before integration"):
    val byRequirement =
      PhaseFIntegrationRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(
      byRequirement(PhaseFIntegrationRequirement.LichessRoundBoardAndInput),
      RequirementClass.LichessProvided
    )
    assertEquals(
      byRequirement(PhaseFIntegrationRequirement.ServerAuthorizedRoundPayload),
      RequirementClass.EvenChessSpecific
    )
    assertEquals(
      byRequirement(PhaseFIntegrationRequirement.OptionalRoundDataField),
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PhaseFIntegrationRequirement.RoundSocketThinAdapter),
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PhaseFIntegrationRequirement.RawEngineAndDebugBlocked),
      RequirementClass.EvenChessSpecific
    )

  test("adapter contract is display-only and leaves Lichess board input authoritative"):
    assertEquals(AdapterContract.roundDataKey, "evenchess")
    assertEquals(AdapterContract.livePayloadKey, "live")
    assertEquals(AdapterContract.socketEventType, "evenchessLive")
    assertEquals(AdapterContract.gridArea, "coach")
    assert(!AdapterContract.clientMayAuthorizeCoaching)
    assert(AdapterContract.lilaBoardOwnsMoveInput)

  test("normal games omit EvenChess live overlay payloads"):
    val payload = RoundLivePayload(
      enabled = false,
      identity = identity.copy(serverAuthorized = false),
      cards = Nil,
      visuals = Nil,
      clearReason = None
    )

    assert(payload.normalGamesShouldOmitPayload)
    assert(payload.valid)
    assertEquals(payload.renderableCards, Nil)
    assertEquals(payload.renderableVisuals, Nil)

  test("server-authorized payload exposes at most one card and one visual"):
    val payload = RoundLivePayload(
      enabled = true,
      identity = identity,
      cards = List(card, card.copy(id = "card-2", defaultActive = false)),
      visuals = List(visual, visual.copy(id = "visual-2", primary = false)),
      clearReason = None
    )

    assert(payload.valid)
    assertEquals(payload.renderableCards.map(_.id), List("card-1"))
    assertEquals(payload.renderableVisuals.map(_.id), List("visual-1"))
    assert(payload.hasOnlyServerApprovedContent)

  test("raw engine and hidden debug payloads are blocked before round UI rendering"):
    val unsafePayload = RoundLivePayload(
      enabled = true,
      identity = identity,
      cards = List(card.copy(rawStockfishLine = Some("pv e2e4 e7e5"))),
      visuals = List(visual.copy(hiddenDebugData = Some("multipv debug"))),
      clearReason = None
    )

    assert(!unsafePayload.hasOnlyServerApprovedContent)
    assert(!unsafePayload.valid)
    assertEquals(unsafePayload.renderableCards, Nil)
    assertEquals(unsafePayload.renderableVisuals, Nil)

  test("clear-only payloads carry an audited stale-state instruction"):
    val clearPayload = RoundLivePayload(
      enabled = true,
      identity = identity,
      cards = Nil,
      visuals = Nil,
      clearReason = Some("move-played")
    )

    assert(clearPayload.clearOnly)
    assert(clearPayload.valid)
    assertEquals(clearPayload.renderableCards, Nil)
    assertEquals(clearPayload.renderableVisuals, Nil)
