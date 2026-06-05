package lila.evenchess

import CoachingLadder.{ ExactnessClass, FeatureRegistryRow, Level, SourceType, UiSlot }
import CoachingOverlays.OverlayVisibility
import EvenChessMode.{ GameMode, TimeControlBucket }

object CoachingPolicy:

  enum CoachingRequestType:
    case Request
    case Display
    case Expand
    case Collapse

  final case class ClockContext(
      millisRemaining: Int,
      incrementMillis: Int,
      premoveCommitted: Boolean,
      staleForDecision: Boolean
  )

  enum AbuseState:
    case Clear
    case Cooldown
    case CapTriggered

  enum ServiceHealth:
    case Healthy
    case Degraded
    case Unavailable

  final case class PolicyInput(
      gameId: String,
      playerId: String,
      mode: GameMode,
      rated: Boolean,
      timeControlBucket: TimeControlBucket,
      ply: Int,
      boardStateKey: String,
      clockContext: ClockContext,
      setLevel: Level,
      currentUsedLevel: Level,
      requestedFeature: String,
      requestType: CoachingRequestType,
      registryRow: FeatureRegistryRow,
      exactnessClass: ExactnessClass,
      abuseState: AbuseState,
      engineHealth: ServiceHealth,
      aiHealth: ServiceHealth,
      policyVersion: String
  ):
    def hasRequiredFields: Boolean =
      gameId.nonEmpty &&
        playerId.nonEmpty &&
        ply >= 0 &&
        boardStateKey.nonEmpty &&
        requestedFeature.nonEmpty &&
        requestedFeature == registryRow.featureKey &&
        policyVersion.nonEmpty

    def requestedLevel: Level = registryRow.unlockLevel

  enum PolicyOutcome:
    case AllowRender
    case AllowHidden
    case SuppressLevel
    case Stale
    case Degraded
    case BlockAbuse

  final case class PolicyDecision(
      outcome: PolicyOutcome,
      deliveredLevel: Option[Level],
      usedLevelAfter: Level,
      assistanceWeightDelta: Int,
      visibility: OverlayVisibility,
      auditRequired: Boolean,
      renderAllowed: Boolean
  ):
    def isAuditable: Boolean = auditRequired

  object ServerAuthority:
    val serverOwnsCoachingPermission = true
    val serverOwnsBoardState = true
    val serverOwnsClockState = true
    val serverOwnsAssistanceAccounting = true
    val serverOwnsTokenConsumption = true
    val serverOwnsRatingUpdates = true
    val serverOwnsAuditEvents = true
    val clientCanDecidePermission = false
    val clientSideHidingIsSecurity = false
    val debugEndpointsMayExposeUnrestrictedEngineOutput = false
    val debugEndpointsMayExposeHiddenPolicyData = false

  object PolicyEngine:
    def decide(input: PolicyInput): PolicyDecision =
      if input.abuseState != AbuseState.Clear then
        suppressed(input, PolicyOutcome.BlockAbuse, OverlayVisibility.Suppressed)
      else if input.clockContext.staleForDecision || input.clockContext.premoveCommitted then
        suppressed(input, PolicyOutcome.Stale, OverlayVisibility.Suppressed)
      else if input.requestedLevel.value > input.setLevel.value then
        suppressed(input, PolicyOutcome.SuppressLevel, OverlayVisibility.Suppressed)
      else if serviceUnavailable(input) then
        hiddenOrDegraded(input)
      else if input.requestType == CoachingRequestType.Request then
        PolicyDecision(
          PolicyOutcome.AllowHidden,
          deliveredLevel = None,
          usedLevelAfter = input.currentUsedLevel,
          assistanceWeightDelta = 0,
          visibility = OverlayVisibility.Hidden,
          auditRequired = true,
          renderAllowed = false
        )
      else
        val delivered = input.requestedLevel
        val usedAfter = Level(math.max(input.currentUsedLevel.value, delivered.value))
        PolicyDecision(
          PolicyOutcome.AllowRender,
          deliveredLevel = Some(delivered),
          usedLevelAfter = usedAfter,
          assistanceWeightDelta = input.registryRow.assistanceWeight,
          visibility = OverlayVisibility.Visible,
          auditRequired = true,
          renderAllowed = true
        )

    private def suppressed(
        input: PolicyInput,
        outcome: PolicyOutcome,
        visibility: OverlayVisibility
    ): PolicyDecision =
      PolicyDecision(
        outcome,
        deliveredLevel = None,
        usedLevelAfter = input.currentUsedLevel,
        assistanceWeightDelta = 0,
        visibility,
        auditRequired = true,
        renderAllowed = false
      )

    private def hiddenOrDegraded(input: PolicyInput): PolicyDecision =
      PolicyDecision(
        PolicyOutcome.Degraded,
        deliveredLevel = Some(Level(math.min(input.requestedLevel.value, input.setLevel.value))),
        usedLevelAfter = Level(math.max(input.currentUsedLevel.value, input.requestedLevel.value)),
        assistanceWeightDelta = input.registryRow.assistanceWeight,
        visibility = OverlayVisibility.Compact,
        auditRequired = true,
        renderAllowed = true
      )

    private def serviceUnavailable(input: PolicyInput): Boolean =
      val needsEngine = input.registryRow.sourceType match
        case SourceType.Stockfish | SourceType.StockfishAiWording | SourceType.HybridStockfish => true
        case _                                                                                 => false
      val needsAi = input.registryRow.sourceType == SourceType.AiOverTruthPackets

      (needsEngine && input.engineHealth != ServiceHealth.Healthy) ||
        (needsAi && input.aiHealth != ServiceHealth.Healthy)

  final case class DeliveredKinds(
      visualIdea: Boolean,
      text: Boolean,
      candidate: Boolean,
      line: Boolean,
      eval: Boolean,
      proof: Boolean,
      warning: Boolean
  ):
    def reconstructable: Boolean =
      List(visualIdea, text, candidate, line, eval, proof, warning).exists(identity)

  final case class AuditEvent(
      eventId: String,
      gameId: String,
      playerId: String,
      ply: Int,
      boardStateKey: String,
      featureKey: String,
      requestedLevel: Level,
      setLevel: Level,
      deliveredLevel: Option[Level],
      usedLevelAfter: Level,
      assistanceWeightDelta: Int,
      exactnessClass: ExactnessClass,
      surface: UiSlot,
      visibility: OverlayVisibility,
      sourceType: SourceType,
      engineJobId: Option[String],
      aiRequestId: Option[String],
      policyVersion: String,
      schemaVersion: String,
      createdAt: Long,
      outcome: PolicyOutcome,
      deliveredKinds: DeliveredKinds,
      rated: Boolean
  ):
    def hasRequiredFields: Boolean =
      eventId.nonEmpty &&
        gameId.nonEmpty &&
        playerId.nonEmpty &&
        ply >= 0 &&
        boardStateKey.nonEmpty &&
        featureKey.nonEmpty &&
        policyVersion.nonEmpty &&
        schemaVersion.nonEmpty &&
        createdAt > 0

    def appendOnlySchemaVersioned: Boolean = schemaVersion.nonEmpty

  object AuditEvent:
    val currentSchemaVersion = "evenchess-assistance-ledger-v1"

    def fromDecision(
        eventId: String,
        input: PolicyInput,
        decision: PolicyDecision,
        createdAt: Long,
        engineJobId: Option[String] = None,
        aiRequestId: Option[String] = None
    ): AuditEvent =
      AuditEvent(
        eventId = eventId,
        gameId = input.gameId,
        playerId = input.playerId,
        ply = input.ply,
        boardStateKey = input.boardStateKey,
        featureKey = input.requestedFeature,
        requestedLevel = input.requestedLevel,
        setLevel = input.setLevel,
        deliveredLevel = decision.deliveredLevel,
        usedLevelAfter = decision.usedLevelAfter,
        assistanceWeightDelta = decision.assistanceWeightDelta,
        exactnessClass = input.exactnessClass,
        surface = input.registryRow.uiSlot,
        visibility = decision.visibility,
        sourceType = input.registryRow.sourceType,
        engineJobId = engineJobId,
        aiRequestId = aiRequestId,
        policyVersion = input.policyVersion,
        schemaVersion = currentSchemaVersion,
        createdAt = createdAt,
        outcome = decision.outcome,
        deliveredKinds = deliveredKindsFor(input.registryRow.uiSlot, decision),
        rated = input.rated
      )

    private def deliveredKindsFor(uiSlot: UiSlot, decision: PolicyDecision): DeliveredKinds =
      if !decision.renderAllowed then DeliveredKinds(false, false, false, false, false, false, false)
      else
        DeliveredKinds(
          visualIdea = uiSlot == UiSlot.BoardLayer || uiSlot == UiSlot.PlanCard || uiSlot == UiSlot.OffsetCard,
          text = uiSlot != UiSlot.BoardLayer,
          candidate = uiSlot == UiSlot.CandidateArea,
          line = uiSlot == UiSlot.CandidateArea && decision.deliveredLevel.exists(_.value >= 6),
          eval = decision.deliveredLevel.exists(_.value >= 8),
          proof = decision.deliveredLevel.exists(_.value >= 8),
          warning = uiSlot == UiSlot.Warning
        )

  final case class AssistanceLedger(events: Vector[AuditEvent]):
    def append(event: AuditEvent): AssistanceLedger = copy(events = events :+ event)

    def replaceEvent(event: AuditEvent): AssistanceLedger = this

    def allRatedEventsSchemaVersioned: Boolean =
      events.filter(_.rated).forall(_.appendOnlySchemaVersioned)

    def maxUsedLevel: Level =
      if events.isEmpty then Level(0) else Level(events.map(_.usedLevelAfter.value).max)

    def assistanceSummaryComputable: Boolean =
      events.forall(_.hasRequiredFields) && events.forall(_.usedLevelAfter.value <= maxUsedLevel.value)

  object AssistanceLedger:
    val empty: AssistanceLedger = AssistanceLedger(Vector.empty)

  enum GameCompletionRatingState:
    case Rate
    case NoRate
    case Annul

  object GameCompletionGuard:
    def mayCompleteRatedEvenChess(
        ledger: AssistanceLedger,
        ratingState: GameCompletionRatingState
    ): Boolean =
      ratingState match
        case GameCompletionRatingState.Rate   => ledger.events.nonEmpty && ledger.assistanceSummaryComputable
        case GameCompletionRatingState.NoRate => true
        case GameCompletionRatingState.Annul  => true

  object Stage1DummyAudit:
    def event(gameId: String, playerId: String, createdAt: Long): AuditEvent =
      val input = PolicyInput(
        gameId = gameId,
        playerId = playerId,
        mode = GameMode.EvenChess,
        rated = false,
        timeControlBucket = TimeControlBucket.Rapid,
        ply = 0,
        boardStateKey = "stage1-dummy",
        clockContext = ClockContext(0, 0, premoveCommitted = false, staleForDecision = false),
        setLevel = Level(0),
        currentUsedLevel = Level(0),
        requestedFeature = "move_history",
        requestType = CoachingRequestType.Display,
        registryRow = CoachingLadder.FeatureRegistry.byKey("move_history"),
        exactnessClass = ExactnessClass.ExactRules,
        abuseState = AbuseState.Clear,
        engineHealth = ServiceHealth.Healthy,
        aiHealth = ServiceHealth.Healthy,
        policyVersion = "stage1-dummy-policy"
      )
      AuditEvent.fromDecision(
        "stage1-dummy-audit",
        input,
        PolicyEngine.decide(input),
        createdAt
      )
