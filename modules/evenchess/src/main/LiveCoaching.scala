package lila.evenchess

import AssistanceAccounting.{ AssistanceDimensions, AssistanceSummaries, CalibrationParameters, PlayerAssistanceSummary, UsedOffset }
import CoachingLadder.{ FeatureRegistry, Level }
import CoachingOverlays.{ BoardStateContext, ClearReason, OverlayInvalidation, OverlayPayload, OverlayVisibility, Perspective }
import CoachingPolicy.{ AbuseState, AssistanceLedger, AuditEvent, ClockContext, CoachingRequestType, PolicyDecision, PolicyEngine, PolicyInput, PolicyOutcome, ServiceHealth }
import EvenChessMode.GameMode
import GamePolicy.GamePolicyRecord
import ProductInvariants.RequirementClass

object LiveCoaching:

  enum PhaseMRequirement:
    case FullGameConsumesWholeGameInput
    case FullGameCarriesSavedLiveEceSnapshots
    case FullGamePostGameReviewOnly
    case FullGameAtMostOneAiNarrativeCall
    case FullGameRequiresTokenQuotaCheck
    case FullGameDoesNotMutateLiveSettlement

  final case class PhaseMRequirementClassification(
      requirement: PhaseMRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseMRequirementClassifications:
    val all: List[PhaseMRequirementClassification] = List(
      PhaseMRequirementClassification(
        PhaseMRequirement.FullGameConsumesWholeGameInput,
        RequirementClass.EvenChessSpecific,
        "Full-game ECE review uses whole-game input including result, termination, moves, FEN history, and optional PGN."
      ),
      PhaseMRequirementClassification(
        PhaseMRequirement.FullGameCarriesSavedLiveEceSnapshots,
        RequirementClass.EvenChessSpecific,
        "Full-game review requests may include saved live ECE snapshot references instead of raw provider payloads."
      ),
      PhaseMRequirementClassification(
        PhaseMRequirement.FullGamePostGameReviewOnly,
        RequirementClass.EvenChessSpecific,
        "Full-game ECE output is post-game review output and is not eligible for live coaching transport."
      ),
      PhaseMRequirementClassification(
        PhaseMRequirement.FullGameAtMostOneAiNarrativeCall,
        RequirementClass.EvenChessSpecific,
        "Full-game ECE review records at most one AI narrative compression allowance."
      ),
      PhaseMRequirementClassification(
        PhaseMRequirement.FullGameRequiresTokenQuotaCheck,
        RequirementClass.EvenChessSpecific,
        "Full-game review plans require a server-side match-review/full-analysis token or quota check before request preparation."
      ),
      PhaseMRequirementClassification(
        PhaseMRequirement.FullGameDoesNotMutateLiveSettlement,
        RequirementClass.EvenChessSpecific,
        "Full-game review must not retroactively alter live Used Level, Assistance Load, Used Offset, ECR, result, or matchmaking."
      )
    )

  enum PhaseLRequirement:
    case LiveWhiteReviewMode
    case LiveBlackReviewMode
    case LiveBothReviewMode
    case LiveModesUseSavedHistoryWithoutCustomTokens
    case CustomReviewSelectableLevels
    case CustomReviewPerspectiveModes
    case CustomReviewCacheAndTokenIntent

  final case class PhaseLRequirementClassification(
      requirement: PhaseLRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseLRequirementClassifications:
    val all: List[PhaseLRequirementClassification] = List(
      PhaseLRequirementClassification(
        PhaseLRequirement.LiveWhiteReviewMode,
        RequirementClass.EvenChessSpecific,
        "Live White review displays White's saved live ECE perception at the canonical level used for that ply."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.LiveBlackReviewMode,
        RequirementClass.EvenChessSpecific,
        "Live Black review displays Black's saved live ECE perception at the canonical level used for that ply."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.LiveBothReviewMode,
        RequirementClass.EvenChessSpecific,
        "Live Both review switches to the saved side-to-move perception for each reviewed position."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.LiveModesUseSavedHistoryWithoutCustomTokens,
        RequirementClass.EvenChessSpecific,
        "Live review modes consume retained ECE history only and do not spend custom analysis tokens."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.CustomReviewSelectableLevels,
        RequirementClass.EvenChessSpecific,
        "Custom review mode carries selectable White and Black levels without changing live used levels."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.CustomReviewPerspectiveModes,
        RequirementClass.EvenChessSpecific,
        "Custom review mode supports White, Black, and side-to-move/Both perspectives."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.CustomReviewCacheAndTokenIntent,
        RequirementClass.EvenChessSpecific,
        "Custom review analysis is cache-keyed by game, levels, perspective, ECE version, and policy version, with token need recorded separately."
      )
    )

  enum PhaseKRequirement:
    case SingleProposedMoveOnly
    case ProposedMoveUsesCurrentFen
    case RequesterMustBeSideToMove
    case ProposedMoveRequiresPolicyPermission
    case ProposedMovePreviewNotActualPosition
    case ProposedMoveDoesNotMutateLiveHistoryOrSettlement

  final case class PhaseKRequirementClassification(
      requirement: PhaseKRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseKRequirementClassifications:
    val all: List[PhaseKRequirementClassification] = List(
      PhaseKRequirementClassification(
        PhaseKRequirement.SingleProposedMoveOnly,
        RequirementClass.EvenChessSpecific,
        "Proposed-move requests carry exactly one UCI move and reject multiple/blank proposed moves."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.ProposedMoveUsesCurrentFen,
        RequirementClass.EvenChessSpecific,
        "Proposed-move ECE output is accepted only while the current FEN matches the requested FEN."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.RequesterMustBeSideToMove,
        RequirementClass.EvenChessSpecific,
        "The requester must be the side to move because ECE derives the moving side from FEN."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.ProposedMoveRequiresPolicyPermission,
        RequirementClass.EvenChessSpecific,
        "Server policy must explicitly allow proposed-move help before scheduling a proposed-move ECE request."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.ProposedMovePreviewNotActualPosition,
        RequirementClass.EvenChessSpecific,
        "Proposed-move results are preview-only and must be distinguished from actual-position board-state output."
      ),
      PhaseKRequirementClassification(
        PhaseKRequirement.ProposedMoveDoesNotMutateLiveHistoryOrSettlement,
        RequirementClass.EvenChessSpecific,
        "Proposed-move preview records do not replace actual-position live ECE history or rating settlement inputs."
      )
    )

  enum PhaseJRequirement:
    case ScheduleEceAfterMove
    case StoreFenAndEceMetadataPerPly
    case StoreSideToMoveLevelsVersionsAndOutputRefs
    case HigherLevelResultCanonical
    case LoweringVisibleLevelDoesNotReduceUsedLevel
    case LimitedHistoryRetainsReviewEssentials
    case RawEceRetentionPolicyControlled
    case LiveReviewUsesSavedHistoryLater

  final case class PhaseJRequirementClassification(
      requirement: PhaseJRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseJRequirementClassifications:
    val all: List[PhaseJRequirementClassification] = List(
      PhaseJRequirementClassification(
        PhaseJRequirement.ScheduleEceAfterMove,
        RequirementClass.EvenChessSpecific,
        "The history framework can create a board-state request record for a committed FEN and server-authorized side levels."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.StoreFenAndEceMetadataPerPly,
        RequirementClass.EvenChessSpecific,
        "Each history entry stores game id, ply, FEN, position hash, side to move, and ECE metadata."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.StoreSideToMoveLevelsVersionsAndOutputRefs,
        RequirementClass.EvenChessSpecific,
        "History entries store requested/delivered levels, policy version, ECE version, output references, and audit ids."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.HigherLevelResultCanonical,
        RequirementClass.EvenChessSpecific,
        "When multiple live ECE results exist for a ply and side, the highest delivered level is canonical for used-level and review history."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.LoweringVisibleLevelDoesNotReduceUsedLevel,
        RequirementClass.EvenChessSpecific,
        "The game-level history used-level summary is monotonic and never decreases after lower-level later entries."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.LimitedHistoryRetainsReviewEssentials,
        RequirementClass.EvenChessSpecific,
        "Limited retention keeps FENs, moves, highest side levels, summary/plan text, overlay references, and audit atoms."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.RawEceRetentionPolicyControlled,
        RequirementClass.EvenChessSpecific,
        "Raw ECE output is not retained unless a retention policy explicitly allows it."
      ),
      PhaseJRequirementClassification(
        PhaseJRequirement.LiveReviewUsesSavedHistoryLater,
        RequirementClass.AdaptedToLichessFork,
        "Later review modes should consume saved live history instead of spending custom analysis tokens."
      )
    )

  enum PhaseERequirement:
    case ServerAuthoritativeDecision
    case ClientRequestDisplayOnly
    case EveryDecisionAudited
    case OverlayTransportServerAuthorized
    case UsedLevelLoadOffsetUpdatedServerSide
    case HiddenStaleSuppressedNotCharged
    case MoveAndBoardMismatchClearPayloads
    case LilaWebSocketIntegrationDeferredToThinSeams

  final case class PhaseERequirementClassification(
      requirement: PhaseERequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseERequirementClassifications:
    val all: List[PhaseERequirementClassification] = List(
      PhaseERequirementClassification(
        PhaseERequirement.ServerAuthoritativeDecision,
        RequirementClass.EvenChessSpecific,
        "Live coaching permission is decided from server game policy, board, clock, Set Level, registry, abuse, and service health."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.ClientRequestDisplayOnly,
        RequirementClass.EvenChessSpecific,
        "Client requests may ask, expand, collapse, or display; client permission claims are never accepted as authority."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.EveryDecisionAudited,
        RequirementClass.EvenChessSpecific,
        "Every render, suppression, hidden request, stale result, fallback, and block produces a schema-versioned audit event."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.OverlayTransportServerAuthorized,
        RequirementClass.AdaptedToLichessFork,
        "Only server-approved overlay payloads are eligible for future lila board/WebSocket transport."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.UsedLevelLoadOffsetUpdatedServerSide,
        RequirementClass.EvenChessSpecific,
        "The server recomputes Used Level, Assistance Load, and Used Offset from the ledger after each live decision."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.HiddenStaleSuppressedNotCharged,
        RequirementClass.EvenChessSpecific,
        "Hidden prefetch, stale decisions, and suppressed decisions are audited but do not increase live assistance accounting."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.MoveAndBoardMismatchClearPayloads,
        RequirementClass.AdaptedToLichessFork,
        "Lichess move/board state updates will clear stale EvenChess overlays through a narrow invalidation seam."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.LilaWebSocketIntegrationDeferredToThinSeams,
        RequirementClass.AdaptedToLichessFork,
        "This phase defines the EvenChess-owned service contract; later phases patch-map the smallest lila transport adapter."
      )
    )

  enum LiveCoachingError:
    case InvalidRequest
    case InvalidContext
    case MissingPlayerPolicy
    case UnknownFeature
    case GameMismatch

  final case class ClientCoachingRequest(
      gameId: String,
      playerId: String,
      ply: Int,
      boardStateKey: String,
      perspective: Perspective,
      featureKey: String,
      requestType: CoachingRequestType,
      clientClaimedAllowed: Boolean
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        playerId.nonEmpty &&
        ply >= 0 &&
        boardStateKey.nonEmpty &&
        featureKey.nonEmpty

  final case class ServerLiveContext(
      policyRecord: GamePolicyRecord,
      ledger: AssistanceLedger,
      dimensionsByEventId: Map[String, AssistanceDimensions],
      currentUsedLevel: Level,
      clockContext: ClockContext,
      abuseState: AbuseState,
      engineHealth: ServiceHealth,
      aiHealth: ServiceHealth,
      dimensions: AssistanceDimensions,
      calibration: CalibrationParameters,
      now: Long,
      ttlMillis: Int
  ):
    def valid: Boolean =
      policyRecord.valid &&
        policyRecord.serverMetadata.isValid &&
        calibration.hasVersion &&
        now > 0 &&
        ttlMillis > 0

  final case class LiveTransportEnvelope(
      gameId: String,
      playerId: String,
      ply: Int,
      boardStateKey: String,
      perspective: Perspective,
      overlay: Option[OverlayPayload],
      clearReason: ClearReason,
      auditId: String,
      serverAuthorized: Boolean
  ):
    def hasRequiredIdentity: Boolean =
      gameId.nonEmpty &&
        playerId.nonEmpty &&
        ply >= 0 &&
        boardStateKey.nonEmpty &&
        auditId.nonEmpty

    def renderableOverlay: Boolean =
      overlay.exists(_.isRenderable)

    def clearOnly: Boolean =
      overlay.isEmpty && clearReason != ClearReason.None

    def approvedForClientTransport: Boolean =
      hasRequiredIdentity &&
        serverAuthorized &&
        overlay.forall(payload =>
          payload.serverAuthorized &&
            payload.approvedDisplayPayload &&
            payload.rawStockfishLine.isEmpty &&
            payload.hiddenDebugData.isEmpty
        )

  final case class LiveCoachingResult(
      policyInput: PolicyInput,
      decision: PolicyDecision,
      auditEvent: AuditEvent,
      ledger: AssistanceLedger,
      dimensionsByEventId: Map[String, AssistanceDimensions],
      transport: LiveTransportEnvelope,
      assistanceSummary: PlayerAssistanceSummary,
      usedOffset: UsedOffset,
      clientClaimAcceptedAsAuthority: Boolean
  ):
    def audited: Boolean =
      decision.auditRequired &&
        auditEvent.hasRequiredFields &&
        ledger.events.exists(_.eventId == auditEvent.eventId)

    def serverAuthoritative: Boolean =
      !clientClaimAcceptedAsAuthority &&
        transport.serverAuthorized &&
        transport.approvedForClientTransport

  object LiveTransportContract:
    val clientsMayRequestButNotAuthorize = true
    val overlayPayloadsMustBeServerAuthorized = true
    val rawStockfishLinesAreNotTransported = true
    val hiddenDebugDataIsNotTransported = true
    val lilaWebSocketAdapterMustRemainThin = true

  object LiveTransportInvalidation:
    def clearReasonFor(
        payload: OverlayPayload,
        context: BoardStateContext,
        movePlayed: Boolean
    ): ClearReason =
      OverlayInvalidation.clearReason(payload, context, movePlayed)

  final case class LiveEceOutputReference(
      side: Perspective,
      outputRef: String,
      auditId: String,
      deliveredLevel: Level,
      summary: Option[String],
      plan: Option[String],
      overlayAtomRefs: List[String]
  ):
    def valid: Boolean =
      outputRef.nonEmpty &&
        auditId.nonEmpty &&
        overlayAtomRefs.forall(_.nonEmpty)

  final case class LiveEceHistoryEntry(
      gameId: String,
      ply: Int,
      fen: String,
      moveUci: Option[String],
      positionHash: String,
      sideToMove: Perspective,
      whiteRequestedLevel: Level,
      blackRequestedLevel: Level,
      policyVersion: String,
      eceVersion: String,
      whiteOutput: Option[LiveEceOutputReference],
      blackOutput: Option[LiveEceOutputReference],
      rawEceRetained: Boolean,
      createdAt: Long
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        ply >= 0 &&
        fen.nonEmpty &&
        moveUci.forall(_.nonEmpty) &&
        positionHash.nonEmpty &&
        policyVersion.nonEmpty &&
        eceVersion.nonEmpty &&
        whiteOutput.forall(output => output.side == Perspective.White && output.deliveredLevel.value <= whiteRequestedLevel.value && output.valid) &&
        blackOutput.forall(output => output.side == Perspective.Black && output.deliveredLevel.value <= blackRequestedLevel.value && output.valid) &&
        !rawEceRetained &&
        createdAt > 0

    def outputFor(side: Perspective): Option[LiveEceOutputReference] =
      side match
        case Perspective.White => whiteOutput
        case Perspective.Black => blackOutput

    def reconstructableWithLimitedRetention: Boolean =
      valid &&
        fen.nonEmpty &&
        positionHash.nonEmpty &&
        List(whiteOutput, blackOutput).flatten.forall(output =>
          output.summary.nonEmpty ||
            output.plan.nonEmpty ||
            output.overlayAtomRefs.nonEmpty
        )

  final case class LiveEceScheduledRequest(
      gameId: String,
      ply: Int,
      fen: String,
      positionHash: String,
      request: EngineGateway.EceBoardStateRequest,
      scheduledAt: Long
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        ply >= 0 &&
        fen.nonEmpty &&
        positionHash.nonEmpty &&
        request.valid &&
        request.inputFen == fen &&
      scheduledAt > 0

  final case class LiveEceProposedMoveScheduledRequest(
      gameId: String,
      ply: Int,
      fen: String,
      positionHash: String,
      requesterSide: Perspective,
      sideToMove: Perspective,
      request: EngineGateway.EceProposedMoveRequest,
      scheduledAt: Long,
      previewOnly: Boolean
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        ply >= 0 &&
        fen.nonEmpty &&
        positionHash.nonEmpty &&
        requesterSide == sideToMove &&
        request.valid &&
        request.inputFen == fen &&
        scheduledAt > 0 &&
        previewOnly

  object LiveEceHistoryScheduler:
    def scheduleBoardState(
        gameId: String,
        ply: Int,
        fen: String,
        positionHash: String,
        whiteEcr: Option[Int],
        blackEcr: Option[Int],
        whiteLevel: Level,
        blackLevel: Level,
        aiTextAllowed: Boolean,
        scheduledAt: Long
    ): LiveEceScheduledRequest =
      LiveEceScheduledRequest(
        gameId = gameId,
        ply = ply,
        fen = fen,
        positionHash = positionHash,
        request = EngineGateway.EceBoardStateRequest.boardState(
          gameId = gameId,
          ply = ply,
          inputFen = fen,
          whiteEcr = whiteEcr,
          blackEcr = blackEcr,
          whiteLevel = whiteLevel,
          blackLevel = blackLevel,
          aiTextAllowed = aiTextAllowed
        ),
        scheduledAt = scheduledAt
      )

    def scheduleProposedMove(
        gameId: String,
        ply: Int,
        proposalIndex: Int,
        fen: String,
        positionHash: String,
        proposedMoveUci: String,
        requesterSide: Perspective,
        sideToMove: Perspective,
        whiteEcr: Option[Int],
        blackEcr: Option[Int],
        whiteLevel: Level,
        blackLevel: Level,
        aiTextAllowed: Boolean,
        proposedMoveHelpAllowed: Boolean,
        scheduledAt: Long
    ): Either[String, LiveEceProposedMoveScheduledRequest] =
      val request = EngineGateway.EceProposedMoveRequest.proposedMove(
        gameId = gameId,
        ply = ply,
        proposalIndex = proposalIndex,
        inputFen = fen,
        proposedMoveUci = proposedMoveUci,
        whiteEcr = whiteEcr,
        blackEcr = blackEcr,
        whiteLevel = whiteLevel,
        blackLevel = blackLevel,
        aiTextAllowed = aiTextAllowed
      )
      val scheduled = LiveEceProposedMoveScheduledRequest(
        gameId = gameId,
        ply = ply,
        fen = fen,
        positionHash = positionHash,
        requesterSide = requesterSide,
        sideToMove = sideToMove,
        request = request,
        scheduledAt = scheduledAt,
        previewOnly = true
      )
      Either.cond(
        proposedMoveHelpAllowed && scheduled.valid,
        scheduled,
        "invalid_or_unauthorized_proposed_move_request"
      )

  final case class LiveEceHistoryRecord(
      gameId: String,
      entries: List[LiveEceHistoryEntry]
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        entries.forall(entry => entry.gameId == gameId && entry.valid)

    def append(entry: LiveEceHistoryEntry): LiveEceHistoryRecord =
      copy(entries = (entries :+ entry).sortBy(entry => (entry.ply, entry.createdAt)))

    def canonicalOutput(ply: Int, side: Perspective): Option[LiveEceOutputReference] =
      canonicalOutputWithEntry(ply, side).map(_._2)

    def canonicalOutputWithEntry(ply: Int, side: Perspective): Option[(LiveEceHistoryEntry, LiveEceOutputReference)] =
      entries
        .filter(_.ply == ply)
        .flatMap(entry => entry.outputFor(side).map(output => entry -> output))
        .sortBy { case (entry, output) => (output.deliveredLevel.value, entry.createdAt) }
        .lastOption

    def latestEntry(ply: Int): Option[LiveEceHistoryEntry] =
      entries.filter(_.ply == ply).sortBy(_.createdAt).lastOption

    def fullGameSnapshotRefs: List[EngineGateway.EceLiveSnapshotRef] =
      entries
        .sortBy(entry => (entry.ply, entry.createdAt))
        .map(entry =>
          EngineGateway.EceLiveSnapshotRef(
            ply = entry.ply,
            fen = entry.fen,
            sideToMove = entry.sideToMove,
            whiteOutputRef = entry.whiteOutput.map(_.outputRef),
            blackOutputRef = entry.blackOutput.map(_.outputRef)
          )
        )

    def highestUsedLevel(side: Perspective): Level =
      val highest = entries.flatMap(_.outputFor(side)).map(_.deliveredLevel.value).foldLeft(0)(math.max)
      Level(highest)

    def fenHistory: List[String] =
      entries.sortBy(entry => (entry.ply, entry.createdAt)).map(_.fen)

    def limitedReviewEssentials: LiveEceLimitedReviewEssentials =
      LiveEceLimitedReviewEssentials(
        gameId = gameId,
        fenHistory = fenHistory,
        moveHistory = entries.sortBy(_.ply).flatMap(_.moveUci),
        highestWhiteLevel = highestUsedLevel(Perspective.White),
        highestBlackLevel = highestUsedLevel(Perspective.Black),
        outputRefs = entries.flatMap(entry => List(entry.whiteOutput, entry.blackOutput).flatten.map(_.outputRef)).distinct,
        auditIds = entries.flatMap(entry => List(entry.whiteOutput, entry.blackOutput).flatten.map(_.auditId)).distinct
      )

  object LiveEceHistoryRecord:
    def empty(gameId: String): LiveEceHistoryRecord =
      LiveEceHistoryRecord(gameId, Nil)

  final case class LiveEceLimitedReviewEssentials(
      gameId: String,
      fenHistory: List[String],
      moveHistory: List[String],
      highestWhiteLevel: Level,
      highestBlackLevel: Level,
      outputRefs: List[String],
      auditIds: List[String]
  ):
    def reconstructable: Boolean =
      gameId.nonEmpty &&
        fenHistory.nonEmpty &&
        outputRefs.forall(_.nonEmpty) &&
        auditIds.forall(_.nonEmpty)

  enum ReviewMode:
    case LiveWhite
    case LiveBlack
    case LiveBoth
    case Custom

  enum CustomReviewPerspective:
    case White
    case Black
    case SideToMove

  final case class LiveReviewFrame(
      gameId: String,
      ply: Int,
      fen: String,
      positionHash: String,
      mode: ReviewMode,
      sourceSide: Perspective,
      output: LiveEceOutputReference,
      consumesCustomAnalysisTokens: Boolean,
      mutatesLiveFairnessState: Boolean
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        ply >= 0 &&
        fen.nonEmpty &&
        positionHash.nonEmpty &&
        mode != ReviewMode.Custom &&
        output.side == sourceSide &&
        output.valid &&
        !consumesCustomAnalysisTokens &&
        !mutatesLiveFairnessState

  final case class CustomReviewRequest(
      gameId: String,
      whiteLevel: Level,
      blackLevel: Level,
      perspective: CustomReviewPerspective,
      eceVersion: String,
      policyVersion: String,
      useAi: Boolean
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        eceVersion.nonEmpty &&
        policyVersion.nonEmpty

    def cacheKey: String =
      List(
        "custom_review",
        gameId,
        s"w${whiteLevel.value}",
        s"b${blackLevel.value}",
        perspective.toString,
        eceVersion,
        policyVersion,
        if useAi then "ai" else "deterministic"
      ).mkString("|")

  final case class CustomReviewPlan(
      request: CustomReviewRequest,
      cacheKey: String,
      requiresCustomAnalysisTokens: Boolean,
      cachedAnalysisAllowed: Boolean,
      mutatesLiveUsedLevel: Boolean,
      mutatesEcrSettlement: Boolean
  ):
    def valid: Boolean =
      request.valid &&
        cacheKey == request.cacheKey &&
        cachedAnalysisAllowed &&
        !mutatesLiveUsedLevel &&
        !mutatesEcrSettlement

  enum FullGameReviewTokenKind:
    case MatchReview
    case FullAnalysis

  final case class FullGameReviewPlan(
      request: EngineGateway.EceGameReviewRequest,
      tokenKind: FullGameReviewTokenKind,
      tokenQuotaChecked: Boolean,
      postGameOnly: Boolean,
      maxAiNarrativeCalls: Int,
      mutatesLiveUsedLevel: Boolean,
      mutatesAssistanceLoad: Boolean,
      mutatesUsedOffset: Boolean,
      mutatesEcrSettlement: Boolean,
      mutatesMatchmakingState: Boolean
  ):
    def valid: Boolean =
      request.valid &&
        tokenQuotaChecked &&
        postGameOnly &&
        maxAiNarrativeCalls >= 0 &&
        maxAiNarrativeCalls <= 1 &&
        !mutatesLiveUsedLevel &&
        !mutatesAssistanceLoad &&
        !mutatesUsedOffset &&
        !mutatesEcrSettlement &&
        !mutatesMatchmakingState

  object ReviewModeEngine:
    def liveReviewFrame(
        history: LiveEceHistoryRecord,
        ply: Int,
        mode: ReviewMode
    ): Option[LiveReviewFrame] =
      val sourceSide = mode match
        case ReviewMode.LiveWhite => Some(Perspective.White)
        case ReviewMode.LiveBlack => Some(Perspective.Black)
        case ReviewMode.LiveBoth  => history.latestEntry(ply).map(_.sideToMove)
        case ReviewMode.Custom    => None

      for
        side <- sourceSide
        (entry, output) <- history.canonicalOutputWithEntry(ply, side)
      yield LiveReviewFrame(
        gameId = history.gameId,
        ply = ply,
        fen = entry.fen,
        positionHash = entry.positionHash,
        mode = mode,
        sourceSide = side,
        output = output,
        consumesCustomAnalysisTokens = false,
        mutatesLiveFairnessState = false
      )

    def planCustomReview(
        request: CustomReviewRequest,
        liveEssentials: LiveEceLimitedReviewEssentials
    ): CustomReviewPlan =
      val exceedsSavedLevels =
        request.whiteLevel.value > liveEssentials.highestWhiteLevel.value ||
          request.blackLevel.value > liveEssentials.highestBlackLevel.value
      val usesL10 =
        request.whiteLevel.value == Level.max || request.blackLevel.value == Level.max

      CustomReviewPlan(
        request = request,
        cacheKey = request.cacheKey,
        requiresCustomAnalysisTokens = request.useAi || usesL10 || exceedsSavedLevels,
        cachedAnalysisAllowed = true,
        mutatesLiveUsedLevel = false,
        mutatesEcrSettlement = false
      )

    def planFullGameReview(
        game: EngineGateway.EceGameReviewInput,
        history: LiveEceHistoryRecord,
        reviewIndex: Int,
        whiteEcr: Option[Int],
        blackEcr: Option[Int],
        reviewLevel: Level,
        aiNarrativeAllowed: Boolean,
        tokenKind: FullGameReviewTokenKind,
        tokenQuotaChecked: Boolean
    ): FullGameReviewPlan =
      val request = EngineGateway.EceGameReviewRequest.gameReview(
        gameId = game.gameId,
        reviewIndex = reviewIndex,
        game = game,
        whiteEcr = whiteEcr,
        blackEcr = blackEcr,
        reviewLevel = reviewLevel,
        aiNarrativeAllowed = aiNarrativeAllowed,
        liveEceSnapshots = history.fullGameSnapshotRefs
      )

      FullGameReviewPlan(
        request = request,
        tokenKind = tokenKind,
        tokenQuotaChecked = tokenQuotaChecked,
        postGameOnly = true,
        maxAiNarrativeCalls = if aiNarrativeAllowed then 1 else 0,
        mutatesLiveUsedLevel = false,
        mutatesAssistanceLoad = false,
        mutatesUsedOffset = false,
        mutatesEcrSettlement = false,
        mutatesMatchmakingState = false
      )

  object LiveCoachingService:
    def process(
        request: ClientCoachingRequest,
        context: ServerLiveContext
    ): Either[LiveCoachingError, LiveCoachingResult] =
      for
        _ <- Either.cond(request.valid, (), LiveCoachingError.InvalidRequest)
        _ <- Either.cond(context.valid, (), LiveCoachingError.InvalidContext)
        _ <- Either.cond(request.gameId == context.policyRecord.gameId, (), LiveCoachingError.GameMismatch)
        setLevel <- context.policyRecord
          .setLevelFor(request.playerId)
          .map(level => Level(level.value))
          .toRight(LiveCoachingError.MissingPlayerPolicy)
        registryRow <- FeatureRegistry.byKey.get(request.featureKey).toRight(LiveCoachingError.UnknownFeature)
      yield
        val policyInput = PolicyInput(
          gameId = request.gameId,
          playerId = request.playerId,
          mode = GameMode.EvenChess,
          rated = context.policyRecord.rated,
          timeControlBucket = context.policyRecord.timeControlBucket,
          ply = request.ply,
          boardStateKey = request.boardStateKey,
          clockContext = context.clockContext,
          setLevel = setLevel,
          currentUsedLevel = context.currentUsedLevel,
          requestedFeature = request.featureKey,
          requestType = request.requestType,
          registryRow = registryRow,
          exactnessClass = registryRow.exactnessClass,
          abuseState = context.abuseState,
          engineHealth = context.engineHealth,
          aiHealth = context.aiHealth,
          policyVersion = context.policyRecord.versions.assistancePolicyVersion
        )
        val decision = PolicyEngine.decide(policyInput)
        val auditEvent = AuditEvent.fromDecision(
          eventId = eventIdFor(request, context.now),
          input = policyInput,
          decision = decision,
          createdAt = context.now
        )
        val nextLedger = context.ledger.append(auditEvent)
        val eventDimensions = dimensionsFor(decision, context.dimensions)
        val nextDimensions = context.dimensionsByEventId.updated(auditEvent.eventId, eventDimensions)
        val summary = AssistanceSummaries.recomputeForPlayer(
          request.playerId,
          nextLedger,
          nextDimensions,
          context.calibration
        )
        val overlay = overlayFor(request, context, decision, auditEvent)
        val transport = LiveTransportEnvelope(
          gameId = request.gameId,
          playerId = request.playerId,
          ply = request.ply,
          boardStateKey = request.boardStateKey,
          perspective = request.perspective,
          overlay = overlay,
          clearReason = clearReasonFor(decision),
          auditId = auditEvent.eventId,
          serverAuthorized = true
        )
        LiveCoachingResult(
          policyInput = policyInput,
          decision = decision,
          auditEvent = auditEvent,
          ledger = nextLedger,
          dimensionsByEventId = nextDimensions,
          transport = transport,
          assistanceSummary = summary,
          usedOffset = UsedOffset.fromSummary(summary, context.calibration),
          clientClaimAcceptedAsAuthority = false
        )

    private def eventIdFor(request: ClientCoachingRequest, now: Long): String =
      s"evenchess-live-${request.gameId}-${request.playerId}-${request.ply}-${request.featureKey}-$now"

    private def dimensionsFor(
        decision: PolicyDecision,
        dimensions: AssistanceDimensions
    ): AssistanceDimensions =
      dimensions.copy(
        staleNonDecisionHelp = dimensions.staleNonDecisionHelp || decision.outcome == PolicyOutcome.Stale
      )

    private def overlayFor(
        request: ClientCoachingRequest,
        context: ServerLiveContext,
        decision: PolicyDecision,
        auditEvent: AuditEvent
    ): Option[OverlayPayload] =
      Option.when(decision.renderAllowed)(
        OverlayPayload(
          gameId = request.gameId,
          ply = request.ply,
          boardStateKey = request.boardStateKey,
          perspective = request.perspective,
          featureKey = request.featureKey,
          level = decision.deliveredLevel.getOrElse(context.currentUsedLevel),
          visibility = decision.visibility,
          ttlMillis = context.ttlMillis,
          stale = false,
          auditId = auditEvent.eventId,
          serverAuthorized = true,
          approvedDisplayPayload = true,
          rawStockfishLine = None,
          hiddenDebugData = None
        )
      )

    private def clearReasonFor(decision: PolicyDecision): ClearReason =
      decision.outcome match
        case PolicyOutcome.Stale        => ClearReason.StalePayload
        case PolicyOutcome.SuppressLevel => ClearReason.Suppressed
        case PolicyOutcome.BlockAbuse   => ClearReason.Suppressed
        case _ if !decision.renderAllowed && decision.visibility == OverlayVisibility.Suppressed =>
          ClearReason.Suppressed
        case _ => ClearReason.None
