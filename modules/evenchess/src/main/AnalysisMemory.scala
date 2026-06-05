package lila.evenchess

import CoachingLadder.Level
import CoachingOverlays.Perspective
import LiveCoaching.*
import ProductInvariants.RequirementClass

object AnalysisMemory:

  enum AnalysisMemoryRequirement:
    case RecentLiveHistoryLimit
    case RequestedAnalysisLimit
    case MissingHistoryRequiresAnalysisRequest
    case RequestedAnalysisKeyedByLevels
    case SharedOverlayShellAcrossSurfaces
    case ReviewDisplayDoesNotMutateLiveFairness

  final case class AnalysisMemoryRequirementClassification(
      requirement: AnalysisMemoryRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object AnalysisMemoryRequirementClassifications:
    val all: List[AnalysisMemoryRequirementClassification] = List(
      AnalysisMemoryRequirementClassification(
        AnalysisMemoryRequirement.RecentLiveHistoryLimit,
        RequirementClass.EvenChessSpecific,
        "Each user keeps a rolling default of the last 10 completed games with attached live ECE per-ply history."
      ),
      AnalysisMemoryRequirementClassification(
        AnalysisMemoryRequirement.RequestedAnalysisLimit,
        RequirementClass.EvenChessSpecific,
        "Each user keeps a rolling default of the last 100 requested full-game/custom analysis records."
      ),
      AnalysisMemoryRequirementClassification(
        AnalysisMemoryRequirement.MissingHistoryRequiresAnalysisRequest,
        RequirementClass.AdaptedToLichessFork,
        "If a replayed game is outside retained memory, the analysis surface has no stored payload and must request analysis before displaying ECE overlays."
      ),
      AnalysisMemoryRequirementClassification(
        AnalysisMemoryRequirement.RequestedAnalysisKeyedByLevels,
        RequirementClass.EvenChessSpecific,
        "Requested analyses are keyed by game, White level, Black level, perspective, ECE version, policy version, and AI flag."
      ),
      AnalysisMemoryRequirementClassification(
        AnalysisMemoryRequirement.SharedOverlayShellAcrossSurfaces,
        RequirementClass.AdaptedToLichessFork,
        "Live, computer, analysis, study, and replay surfaces use the same EvenChess overlay shell and local display toggles."
      ),
      AnalysisMemoryRequirementClassification(
        AnalysisMemoryRequirement.ReviewDisplayDoesNotMutateLiveFairness,
        RequirementClass.EvenChessSpecific,
        "Replay and analysis display selections may track a viewing-session Used Level but never mutate live Used Level, Assistance Load, Used Offset, or ECR."
      )
    )

  final case class RetentionPolicy(
      recentLiveGameLimit: Int = 10,
      requestedFullGameAnalysisLimit: Int = 100,
      retainRawEcePayloads: Boolean = false
  ):
    def valid: Boolean =
      recentLiveGameLimit > 0 &&
        recentLiveGameLimit <= 500 &&
        requestedFullGameAnalysisLimit > 0 &&
        requestedFullGameAnalysisLimit <= 5000 &&
        !retainRawEcePayloads

  object RetentionPolicy:
    val default: RetentionPolicy = RetentionPolicy()

  enum PayloadMemorySource:
    case RecentLiveGame
    case RequestedFullGameAnalysis
    case MissingHistory

  enum StoredEcePayloadSource:
    case LiveTurn
    case FullGameAnalysis

  final case class StoredEceFenPayload(
      ownerUserId: String,
      gameId: String,
      moveNumber: Int,
      ply: Int,
      fen: String,
      positionHash: String,
      side: Perspective,
      deliveredLevel: Level,
      eceVersion: String,
      policyVersion: String,
      auditId: String,
      approvedLiveOverlayJson: String,
      source: StoredEcePayloadSource,
      createdAt: Long
  ):
    def frameKey: String =
      List(ownerUserId, gameId, perspectiveKey(side), ply.toString, fen).mkString("|")

    def sameFrameAs(other: StoredEceFenPayload): Boolean =
      frameKey == other.frameKey

    def valid: Boolean =
      ownerUserId.nonEmpty &&
        gameId.nonEmpty &&
        moveNumber >= 0 &&
        ply >= 0 &&
        fen.nonEmpty &&
        positionHash.nonEmpty &&
        eceVersion.nonEmpty &&
        policyVersion.nonEmpty &&
        auditId.nonEmpty &&
        approvedLiveOverlayJson.trim.startsWith("{") &&
        safeApprovedPayloadJson(approvedLiveOverlayJson) &&
        createdAt > 0

  final case class EvenChessPgnHistory(
      ownerUserId: String,
      gameId: String,
      completedAt: Long,
      frames: List[StoredEceFenPayload]
  ):
    def valid: Boolean =
      ownerUserId.nonEmpty &&
        gameId.nonEmpty &&
        completedAt >= 0 &&
        frames.forall(frame => frame.ownerUserId == ownerUserId && frame.gameId == gameId && frame.valid)

    def upsertHighestLevel(frame: StoredEceFenPayload): Either[String, EvenChessPgnHistory] =
      if frame.ownerUserId != ownerUserId || frame.gameId != gameId || !frame.valid then Left("invalid_ece_fen_payload")
      else
        val existing = frames.find(_.sameFrameAs(frame))
        val keepIncoming =
          existing.forall(current =>
            frame.deliveredLevel.value > current.deliveredLevel.value ||
              (frame.deliveredLevel == current.deliveredLevel && frame.createdAt >= current.createdAt)
          )
        val nextFrames =
          if keepIncoming then frame :: frames.filterNot(_.sameFrameAs(frame))
          else frames
        Right(copy(frames = nextFrames.sortBy(frame => (frame.ply, perspectiveKey(frame.side), frame.createdAt))))

    def frameAt(ply: Int, side: Perspective): Option[StoredEceFenPayload] =
      frames
        .filter(frame => frame.ply == ply && frame.side == side)
        .sortBy(frame => (frame.deliveredLevel.value, frame.createdAt))
        .lastOption

    def frameForFen(fen: String, side: Perspective, minimumLevel: Level = Level(0)): Option[StoredEceFenPayload] =
      frames
        .filter(frame => frame.fen == fen && frame.side == side && frame.deliveredLevel.value >= minimumLevel.value)
        .sortBy(frame => (frame.deliveredLevel.value, frame.createdAt))
        .lastOption

    def attachFullGameFrames(fullGameFrames: List[StoredEceFenPayload]): Either[String, EvenChessPgnHistory] =
      fullGameFrames.foldLeft[Either[String, EvenChessPgnHistory]](Right(this)): (current, frame) =>
        current.flatMap(_.upsertHighestLevel(frame.copy(source = StoredEcePayloadSource.FullGameAnalysis)))

    def highestLevel(side: Perspective): Level =
      Level(frames.filter(_.side == side).map(_.deliveredLevel.value).foldLeft(0)(math.max))

    def moveCount: Int =
      frames.map(_.moveNumber).foldLeft(0)(math.max)

  object EvenChessPgnHistory:
    def empty(ownerUserId: String, gameId: String, completedAt: Long = 0L): EvenChessPgnHistory =
      EvenChessPgnHistory(ownerUserId, gameId, completedAt, Nil)

  final case class StoredRecentGame(
      ownerUserId: String,
      gameId: String,
      completedAt: Long,
      history: LiveEceHistoryRecord
  ):
    def valid: Boolean =
      ownerUserId.nonEmpty &&
        gameId.nonEmpty &&
        completedAt > 0 &&
        history.valid &&
        history.gameId == gameId &&
        history.entries.nonEmpty &&
        history.entries.forall(entry => !entry.rawEceRetained)

    def frame(ply: Int, mode: ReviewMode): Option[LiveReviewFrame] =
      ReviewModeEngine.liveReviewFrame(history, ply, mode)

  final case class FullGameAnalysisKey(
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
        "full_game_analysis",
        gameId,
        s"w${whiteLevel.value}",
        s"b${blackLevel.value}",
        perspective.toString,
        eceVersion,
        policyVersion,
        if useAi then "ai" else "deterministic"
      ).mkString("|")

    def setLevelFor(side: Perspective): Level =
      side match
        case Perspective.White => whiteLevel
        case Perspective.Black => blackLevel

    def maxLevel: Level =
      Level(math.max(whiteLevel.value, blackLevel.value))

  object FullGameAnalysisKey:
    def fromCustomReviewRequest(request: CustomReviewRequest): FullGameAnalysisKey =
      FullGameAnalysisKey(
        gameId = request.gameId,
        whiteLevel = request.whiteLevel,
        blackLevel = request.blackLevel,
        perspective = request.perspective,
        eceVersion = request.eceVersion,
        policyVersion = request.policyVersion,
        useAi = request.useAi
      )

  final case class StoredFullGameAnalysis(
      ownerUserId: String,
      analysisId: String,
      key: FullGameAnalysisKey,
      requestedAt: Long,
      history: LiveEceHistoryRecord,
      tokenQuotaChecked: Boolean
  ):
    def valid: Boolean =
      ownerUserId.nonEmpty &&
        analysisId.nonEmpty &&
        key.valid &&
        requestedAt > 0 &&
        history.valid &&
        history.gameId == key.gameId &&
        tokenQuotaChecked &&
        history.entries.nonEmpty &&
        history.entries.forall(entry =>
          !entry.rawEceRetained &&
            entry.whiteOutput.forall(_.deliveredLevel.value <= key.whiteLevel.value) &&
            entry.blackOutput.forall(_.deliveredLevel.value <= key.blackLevel.value)
        )

    def frame(ply: Int, mode: ReviewMode): Option[LiveReviewFrame] =
      ReviewModeEngine.liveReviewFrame(history, ply, mode)

  final case class AnalysisFrameLookup(
      source: PayloadMemorySource,
      gameId: String,
      ply: Int,
      frame: Option[LiveReviewFrame],
      setLevel: Level,
      analysisRequestRequired: Boolean,
      reason: String
  ):
    def payloadAvailable: Boolean =
      frame.exists(_.valid)

    def valid: Boolean =
      gameId.nonEmpty &&
        ply >= 0 &&
        reason.nonEmpty &&
        frame.forall(_.gameId == gameId) &&
        analysisRequestRequired == !payloadAvailable

  object AnalysisFrameLookup:
    def missing(gameId: String, ply: Int): AnalysisFrameLookup =
      AnalysisFrameLookup(
        source = PayloadMemorySource.MissingHistory,
        gameId = gameId,
        ply = ply,
        frame = None,
        setLevel = Level(10),
        analysisRequestRequired = true,
        reason = "no_retained_ece_history"
      )

  final case class UserAnalysisMemory(
      ownerUserId: String,
      recentGames: List[StoredRecentGame],
      requestedAnalyses: List[StoredFullGameAnalysis]
  ):
    def valid: Boolean =
      ownerUserId.nonEmpty &&
        recentGames.forall(record => record.ownerUserId == ownerUserId && record.valid) &&
        requestedAnalyses.forall(record => record.ownerUserId == ownerUserId && record.valid)

    def rememberRecentGame(
        record: StoredRecentGame,
        policy: RetentionPolicy = RetentionPolicy.default
    ): Either[String, UserAnalysisMemory] =
      if !policy.valid then Left("invalid_retention_policy")
      else if record.ownerUserId != ownerUserId || !record.valid then Left("invalid_recent_game_history")
      else
        val updated =
          (record :: recentGames.filterNot(_.gameId == record.gameId))
            .sortWith((left, right) => left.completedAt > right.completedAt)
            .take(policy.recentLiveGameLimit)
        Right(copy(recentGames = updated))

    def rememberRequestedAnalysis(
        record: StoredFullGameAnalysis,
        policy: RetentionPolicy = RetentionPolicy.default
    ): Either[String, UserAnalysisMemory] =
      if !policy.valid then Left("invalid_retention_policy")
      else if record.ownerUserId != ownerUserId || !record.valid then Left("invalid_requested_analysis")
      else
        val updated =
          (record :: requestedAnalyses.filterNot(existing => existing.key == record.key))
            .sortWith((left, right) => left.requestedAt > right.requestedAt)
            .take(policy.requestedFullGameAnalysisLimit)
        Right(copy(requestedAnalyses = updated))

    def recentHistory(gameId: String): Option[StoredRecentGame] =
      recentGames.find(_.gameId == gameId)

    def requestedAnalysis(key: FullGameAnalysisKey): Option[StoredFullGameAnalysis] =
      requestedAnalyses.find(_.key == key)

    def liveHistoryFrame(gameId: String, ply: Int, mode: ReviewMode): AnalysisFrameLookup =
      recentHistory(gameId).flatMap(_.frame(ply, mode)) match
        case Some(frame) =>
          AnalysisFrameLookup(
            source = PayloadMemorySource.RecentLiveGame,
            gameId = gameId,
            ply = ply,
            frame = Some(frame),
            setLevel = frame.output.deliveredLevel,
            analysisRequestRequired = false,
            reason = "retained_live_history"
          )
        case None =>
          AnalysisFrameLookup.missing(gameId, ply)

    def requestedAnalysisFrame(key: FullGameAnalysisKey, ply: Int, mode: ReviewMode): AnalysisFrameLookup =
      requestedAnalysis(key).flatMap(_.frame(ply, mode)) match
        case Some(frame) =>
          AnalysisFrameLookup(
            source = PayloadMemorySource.RequestedFullGameAnalysis,
            gameId = key.gameId,
            ply = ply,
            frame = Some(frame),
            setLevel = key.setLevelFor(frame.sourceSide),
            analysisRequestRequired = false,
            reason = "retained_requested_analysis"
          )
        case None =>
          AnalysisFrameLookup.missing(key.gameId, ply)

    def bestAvailableFrame(
        gameId: String,
        requestedAnalysisKey: Option[FullGameAnalysisKey],
        ply: Int,
        mode: ReviewMode
    ): AnalysisFrameLookup =
      requestedAnalysisKey match
        case Some(key) => requestedAnalysisFrame(key, ply, mode)
        case None      => liveHistoryFrame(gameId, ply, mode)

  object UserAnalysisMemory:
    def empty(ownerUserId: String): UserAnalysisMemory =
      UserAnalysisMemory(ownerUserId, Nil, Nil)

  private def perspectiveKey(side: Perspective): String =
    side match
      case Perspective.White => "white"
      case Perspective.Black => "black"

  private def safeApprovedPayloadJson(payloadJson: String): Boolean =
    val lower = payloadJson.toLowerCase
    !lower.contains("raw_provider_output") &&
      !lower.contains("raw_prompt") &&
      !lower.contains("raw_ai") &&
      !lower.contains("provider_path") &&
      !lower.contains("api_key") &&
      !lower.contains("secret") &&
      !lower.contains("password") &&
      !lower.contains("external_engines") &&
      !payloadJson.matches(".*[A-Za-z]:\\\\.*")

  enum OverlayShellSurface:
    case LiveGame
    case ComputerGame
    case AnalysisReplay
    case StudyBoard
    case OpeningExplorer

  final case class ModeNeutralOverlayShell(
      surface: OverlayShellSurface,
      setLevel: Level,
      usedLevel: Level,
      payloadAvailable: Boolean,
      analysisRequestRequired: Boolean,
      togglesMayHideAuthorizedFeatures: Boolean,
      mutatesLiveFairnessState: Boolean,
      displayAdapterKey: String
  ):
    def valid: Boolean =
      displayAdapterKey == ModeNeutralOverlayPolicy.sharedDisplayAdapterKey &&
        usedLevel.value <= setLevel.value &&
        togglesMayHideAuthorizedFeatures &&
        !(payloadAvailable && analysisRequestRequired)

  object ModeNeutralOverlayPolicy:
    val sharedDisplayAdapterKey = "evenchess-overlay-shell"

    def visibleLevelCap(setLevel: Level, selectedLevel: Level): Level =
      Level(math.min(setLevel.value, selectedLevel.value))

    def monotonicUsedLevel(previousUsedLevel: Level, setLevel: Level, selectedLevel: Level): Level =
      val cappedPrevious = math.min(previousUsedLevel.value, setLevel.value)
      Level(math.max(cappedPrevious, visibleLevelCap(setLevel, selectedLevel).value))

    def liveGame(setLevel: Level, currentUsedLevel: Level, payloadAvailable: Boolean): ModeNeutralOverlayShell =
      ModeNeutralOverlayShell(
        surface = OverlayShellSurface.LiveGame,
        setLevel = setLevel,
        usedLevel = Level(math.min(currentUsedLevel.value, setLevel.value)),
        payloadAvailable = payloadAvailable,
        analysisRequestRequired = false,
        togglesMayHideAuthorizedFeatures = true,
        mutatesLiveFairnessState = true,
        displayAdapterKey = sharedDisplayAdapterKey
      )

    def computerGame(setLevel: Level = Level(10), payloadAvailable: Boolean): ModeNeutralOverlayShell =
      nonLive(OverlayShellSurface.ComputerGame, setLevel, payloadAvailable)

    def retainedHistory(frame: AnalysisFrameLookup, previousUsedLevel: Level = Level(0)): ModeNeutralOverlayShell =
      nonLive(
        OverlayShellSurface.AnalysisReplay,
        frame.setLevel,
        frame.payloadAvailable,
        previousUsedLevel,
        frame.analysisRequestRequired
      )

    def requestedFullGameAnalysis(frame: AnalysisFrameLookup, previousUsedLevel: Level = Level(0)): ModeNeutralOverlayShell =
      nonLive(
        OverlayShellSurface.AnalysisReplay,
        frame.setLevel,
        frame.payloadAvailable,
        previousUsedLevel,
        frame.analysisRequestRequired
      )

    def missingHistory(gameId: String, ply: Int, previousUsedLevel: Level = Level(0)): ModeNeutralOverlayShell =
      retainedHistory(AnalysisFrameLookup.missing(gameId, ply), previousUsedLevel)

    def withSelectedLevel(shell: ModeNeutralOverlayShell, selectedLevel: Level): ModeNeutralOverlayShell =
      if shell.payloadAvailable then
        shell.copy(usedLevel = monotonicUsedLevel(shell.usedLevel, shell.setLevel, selectedLevel))
      else shell

    private def nonLive(
        surface: OverlayShellSurface,
        setLevel: Level,
        payloadAvailable: Boolean,
        previousUsedLevel: Level = Level(0),
        analysisRequestRequired: Boolean = false
    ): ModeNeutralOverlayShell =
      ModeNeutralOverlayShell(
        surface = surface,
        setLevel = setLevel,
        usedLevel = if payloadAvailable then monotonicUsedLevel(previousUsedLevel, setLevel, setLevel) else previousUsedLevel,
        payloadAvailable = payloadAvailable,
        analysisRequestRequired = analysisRequestRequired,
        togglesMayHideAuthorizedFeatures = true,
        mutatesLiveFairnessState = false,
        displayAdapterKey = sharedDisplayAdapterKey
      )
