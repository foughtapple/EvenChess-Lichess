package lila.evenchess

import java.net.URI

import CoachingLadder.Level
import CoachingOverlays.Perspective
import EvenChessMode.TimeControlBucket
import ProductInvariants.RequirementClass

object EngineGateway:

  enum EngineRequirement:
    case InternalGateway
    case NoUnrestrictedClientAccess
    case LevelBoundedProfiles
    case QueuesCachingCancellationTimeoutFallback
    case VersionHashInventory
    case GplLicensePreserved
    case ReuseLilaEngineSeamsFirst
    case SeparateEceServiceBoundary
    case EceServerToServerOnly
    case EceHealthAndBoardEndpoints
    case EceBoardStateRequestContract
    case EceSideGatedResponseContract
    case EceStalePayloadRejection
    case EceUnavailableIsNonFatal
    case EceProposedMoveRequestContract
    case EceProposedMoveCurrentFenAndSideToMove
    case EceProposedMovePreviewDistinct
    case EceFullGameReviewRequestContract
    case EceFullGameReviewPostGameOnly
    case EceFullGameReviewDoesNotMutateSettlement
    case EceProviderBoundaryNoDirectCalls
    case EceProviderStatusSanitized
    case EceNormalizedProviderOutputOnly

  final case class EngineRequirementClassification(
      requirement: EngineRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object EngineRequirementClassifications:
    val all: List[EngineRequirementClassification] = List(
      EngineRequirementClassification(
        EngineRequirement.InternalGateway,
        RequirementClass.EvenChessSpecific,
        "Live EvenChess engine help must pass through an internal Engine Gateway."
      ),
      EngineRequirementClassification(
        EngineRequirement.NoUnrestrictedClientAccess,
        RequirementClass.EvenChessSpecific,
        "Clients receive approved coaching truth packets, never raw Stockfish access."
      ),
      EngineRequirementClassification(
        EngineRequirement.LevelBoundedProfiles,
        RequirementClass.EvenChessSpecific,
        "Depth, nodes, movetime, MultiPV, threads, and hash are capped by Set Level and time control."
      ),
      EngineRequirementClassification(
        EngineRequirement.QueuesCachingCancellationTimeoutFallback,
        RequirementClass.EvenChessSpecific,
        "Live/post-game queues, cancellation, cache, timeout, degraded, and fallback states are required."
      ),
      EngineRequirementClassification(
        EngineRequirement.VersionHashInventory,
        RequirementClass.EvenChessSpecific,
        "Engine binary/source/version/hash must be recorded with jobs."
      ),
      EngineRequirementClassification(
        EngineRequirement.GplLicensePreserved,
        RequirementClass.AdaptedToLichessFork,
        "GPL/license obligations must remain visible when binaries or source are distributed."
      ),
      EngineRequirementClassification(
        EngineRequirement.ReuseLilaEngineSeamsFirst,
        RequirementClass.AdaptedToLichessFork,
        "Inspect lila/fishnet/analysis/tablebase before adding any new engine service."
      ),
      EngineRequirementClassification(
        EngineRequirement.SeparateEceServiceBoundary,
        RequirementClass.EvenChessSpecific,
        "EvenChess-Lichess calls the separate private ECE service and does not reimplement ECE chess/provider internals."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceServerToServerOnly,
        RequirementClass.EvenChessSpecific,
        "ECE base URLs, provider details, and board-state calls stay server-side and are never exposed as browser endpoints."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceHealthAndBoardEndpoints,
        RequirementClass.EvenChessSpecific,
        "The integration framework defaults to GET /health, POST /v1/ece/board/quick, and conditional POST /v1/ece/board/deep on the local ECE service."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceBoardStateRequestContract,
        RequirementClass.EvenChessSpecific,
        "Board-state requests send authoritative FEN, ECR rating context, server-authorized levels, AI flag, and numeric custom profile only."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceSideGatedResponseContract,
        RequirementClass.EvenChessSpecific,
        "Only side_outputs.white and side_outputs.black are display candidates; public position and shared_calculations are rejected."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceStalePayloadRejection,
        RequirementClass.EvenChessSpecific,
        "ECE output is usable only when request id, FEN echo, level echo, current FEN, and diagnostics status all match."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceUnavailableIsNonFatal,
        RequirementClass.LichessProvided,
        "Missing or slow ECE must not block ordinary Lichess page loading, legal moves, clocks, or game lifecycle."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceProposedMoveRequestContract,
        RequirementClass.EvenChessSpecific,
        "Proposed-move mode sends one UCI move with authoritative FEN, ECR context, levels, AI flag, and numeric custom profile."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceProposedMoveCurrentFenAndSideToMove,
        RequirementClass.EvenChessSpecific,
        "Proposed-move ECE output is accepted only when current FEN still matches and the requester is the side to move."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceProposedMovePreviewDistinct,
        RequirementClass.EvenChessSpecific,
        "Proposed-move output is marked as preview output and must not replace actual-position board-state history."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceFullGameReviewRequestContract,
        RequirementClass.EvenChessSpecific,
        "Full-game ECE review sends whole-game input, review level, ECR context, numeric custom profile, and saved live ECE snapshot references."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceFullGameReviewPostGameOnly,
        RequirementClass.EvenChessSpecific,
        "Full-game ECE output is accepted only as post-game review output."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceFullGameReviewDoesNotMutateSettlement,
        RequirementClass.EvenChessSpecific,
        "Full-game review output must not alter result, live Used Level, Assistance Load, Used Offset, ECR, or matchmaking state."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceProviderBoundaryNoDirectCalls,
        RequirementClass.EvenChessSpecific,
        "EvenChess-Lichess calls ECE only; Stockfish, Syzygy, Maia, opening-book, rules, eval-cache, and AI provider calls stay inside ECE."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceProviderStatusSanitized,
        RequirementClass.EvenChessSpecific,
        "Lichess-side provider status may report availability and latency but never provider secrets, keys, paths, prompts, or raw outputs."
      ),
      EngineRequirementClassification(
        EngineRequirement.EceNormalizedProviderOutputOnly,
        RequirementClass.EvenChessSpecific,
        "EvenChess-Lichess may display provider-derived material only after ECE normalizes, side-gates, and level-gates it."
      )
    )

  enum EngineQueue:
    case Live
    case PostGame

  enum EngineJobStatus:
    case Ready
    case Queued
    case Running
    case Cancelled
    case TimedOut
    case Stale
    case Degraded
    case Fallback

  final case class EngineProfile(
      level: Level,
      timeControl: TimeControlBucket,
      maxDepth: Int,
      maxNodes: Int,
      maxMovetimeMillis: Int,
      maxMultiPv: Int,
      maxThreads: Int,
      maxHashMb: Int,
      maxCandidates: Int,
      allowNumericEvalOrWdl: Boolean,
      approximateEvalLabelRequired: Boolean
  ):
    def liveEngineCandidateAllowed: Boolean = maxCandidates > 0 && maxMultiPv > 0

    def allowsCandidateCount(count: Int): Boolean =
      count >= 0 && count <= maxCandidates && count <= maxMultiPv

    def allowsLimits(depth: Int, nodes: Int, movetimeMillis: Int, multiPv: Int, threads: Int, hashMb: Int): Boolean =
      depth <= maxDepth &&
        nodes <= maxNodes &&
        movetimeMillis <= maxMovetimeMillis &&
        multiPv <= maxMultiPv &&
        threads <= maxThreads &&
        hashMb <= maxHashMb

  object EngineProfiles:
    def forLevel(level: Level, timeControl: TimeControlBucket): EngineProfile =
      val timeMultiplier = timeControl match
        case TimeControlBucket.Bullet         => 1
        case TimeControlBucket.Blitz          => 2
        case TimeControlBucket.Rapid          => 3
        case TimeControlBucket.Classical      => 4
        case TimeControlBucket.Correspondence => 4
        case TimeControlBucket.Casual         => 2

      level.value match
        case value if value <= 4 =>
          EngineProfile(level, timeControl, 0, 0, 0, 0, 0, 0, 0, allowNumericEvalOrWdl = false, approximateEvalLabelRequired = false)
        case 5 =>
          bounded(level, timeControl, timeMultiplier, maxMultiPv = 1, maxCandidates = 1, eval = false)
        case 6 =>
          bounded(level, timeControl, timeMultiplier, maxMultiPv = 2, maxCandidates = 2, eval = false)
        case 7 =>
          bounded(level, timeControl, timeMultiplier, maxMultiPv = 3, maxCandidates = 3, eval = false)
        case 8 =>
          bounded(level, timeControl, timeMultiplier, maxMultiPv = 3, maxCandidates = 3, eval = true)
        case 9 =>
          bounded(level, timeControl, timeMultiplier, maxMultiPv = 4, maxCandidates = 4, eval = true)
        case _ =>
          bounded(level, timeControl, timeMultiplier, maxMultiPv = 4, maxCandidates = 4, eval = true)

    private def bounded(
        level: Level,
        timeControl: TimeControlBucket,
        timeMultiplier: Int,
        maxMultiPv: Int,
        maxCandidates: Int,
        eval: Boolean
    ): EngineProfile =
      EngineProfile(
        level = level,
        timeControl = timeControl,
        maxDepth = 8 + level.value,
        maxNodes = 50_000 * timeMultiplier * level.value,
        maxMovetimeMillis = 150 * timeMultiplier * level.value,
        maxMultiPv = maxMultiPv,
        maxThreads = 1,
        maxHashMb = 32,
        maxCandidates = maxCandidates,
        allowNumericEvalOrWdl = eval,
        approximateEvalLabelRequired = eval
      )

  final case class EngineLimitRequest(
      depth: Int,
      nodes: Int,
      movetimeMillis: Int,
      multiPv: Int,
      threads: Int,
      hashMb: Int
  )

  final case class EngineJobRequest(
      requestId: String,
      gameId: String,
      playerId: String,
      boardStateKey: String,
      ply: Int,
      perspective: Perspective,
      requestedFeature: String,
      setLevel: Level,
      requestedLevel: Level,
      timeControl: TimeControlBucket,
      queue: EngineQueue,
      limits: EngineLimitRequest,
      policyVersion: String,
      cancellationToken: String,
      cacheKey: String
  ):
    def hasRequiredFields: Boolean =
      requestId.nonEmpty &&
        gameId.nonEmpty &&
        playerId.nonEmpty &&
        boardStateKey.nonEmpty &&
        ply >= 0 &&
        requestedFeature.nonEmpty &&
        policyVersion.nonEmpty &&
        cancellationToken.nonEmpty &&
        cacheKey.nonEmpty

    def profile: EngineProfile = EngineProfiles.forLevel(requestedLevel, timeControl)

    def allowedBySetLevel: Boolean = requestedLevel.value <= setLevel.value

    def profileValidated: Boolean =
      profile.liveEngineCandidateAllowed &&
        profile.allowsLimits(
          limits.depth,
          limits.nodes,
          limits.movetimeMillis,
          limits.multiPv,
          limits.threads,
          limits.hashMb
        )

    def acceptedByGateway: Boolean = hasRequiredFields && allowedBySetLevel && profileValidated

  final case class EngineCandidate(
      uci: String,
      rank: Int,
      scoreCp: Option[Int],
      wdl: Option[String],
      linePlyCount: Int,
      proof: Option[String]
  ):
    def hasNoRawLine: Boolean = linePlyCount >= 0 && proof.forall(_.length <= 80)

  final case class EngineAuditMetadata(
      jobId: String,
      policyVersion: String,
      profile: EngineProfile,
      engineVersion: String,
      engineSource: String,
      engineBinaryHash: String
  ):
    def complete: Boolean =
      jobId.nonEmpty &&
        policyVersion.nonEmpty &&
        engineVersion.nonEmpty &&
        engineSource.nonEmpty &&
        engineBinaryHash.nonEmpty

  final case class EngineTruthPacket(
      jobId: String,
      gameId: String,
      playerId: String,
      boardStateKey: String,
      ply: Int,
      requestedFeature: String,
      candidates: List[EngineCandidate],
      numericEvalLabel: Option[String],
      status: EngineJobStatus,
      stale: Boolean,
      degraded: Boolean,
      fallback: Boolean,
      audit: EngineAuditMetadata,
      rawEnginePayload: Option[String],
      hiddenDebugData: Option[String]
  ):
    def candidateCountBounded: Boolean =
      audit.profile.allowsCandidateCount(candidates.size)

    def noUnrestrictedRawEnginePayload: Boolean =
      rawEnginePayload.isEmpty && hiddenDebugData.isEmpty && candidates.forall(_.hasNoRawLine)

    def evalLabelCompliant: Boolean =
      if candidates.exists(candidate => candidate.scoreCp.nonEmpty || candidate.wdl.nonEmpty) then
        audit.profile.allowNumericEvalOrWdl &&
          (!audit.profile.approximateEvalLabelRequired || numericEvalLabel.exists(_.contains("Approximate")))
      else true

    def auditable: Boolean = audit.complete && jobId == audit.jobId

    def safeForClient: Boolean =
      candidateCountBounded && noUnrestrictedRawEnginePayload && evalLabelCompliant && auditable

  object EngineEndpointPolicy:
    val internalGatewayRequired = true
    val browserMayRequestLiveRatedStockfish = false
    val debugEndpointMayExposeRawStockfish = false
    val clientsReceiveOnlyBoundedTruthPackets = true

  object QueueOperations:
    val liveQueueRequired = true
    val postGameQueueRequired = true
    val cachingRequired = true
    val cancellationRequired = true
    val timeoutRequired = true
    val fallbackRequired = true
    val degradedStateRequired = true

    def cacheHitAllowed(request: EngineJobRequest, cached: EngineTruthPacket): Boolean =
      cached.gameId == request.gameId &&
        cached.playerId == request.playerId &&
        cached.boardStateKey == request.boardStateKey &&
        cached.ply == request.ply &&
        !cached.stale &&
        cached.safeForClient

    def statusFromRuntime(cancelled: Boolean, timedOut: Boolean, stale: Boolean, engineHealthy: Boolean): EngineJobStatus =
      if cancelled then EngineJobStatus.Cancelled
      else if timedOut then EngineJobStatus.TimedOut
      else if stale then EngineJobStatus.Stale
      else if !engineHealthy then EngineJobStatus.Degraded
      else EngineJobStatus.Ready

  final case class LicenseInventory(
      engineName: String,
      version: String,
      sourceUrl: String,
      license: String,
      binaryHash: String,
      distributionNoticePresent: Boolean
  ):
    def complete: Boolean =
      engineName.nonEmpty &&
        version.nonEmpty &&
        sourceUrl.nonEmpty &&
        license.nonEmpty &&
        binaryHash.nonEmpty

    def gplObligationsPreserved: Boolean =
      license.toUpperCase.contains("GPL") && sourceUrl.nonEmpty && distributionNoticePresent

  object LichessEngineSeamRules:
    val inspectFishnetAnalysisTablebaseBeforeNewService = true
    val wrapReusableLilaEngineSeamsSafely = true
    val implementEvenChessGatewayOnlyIfReuseIsUnsafe = true
    val coreEngineEditsRequirePatchMap = true

  final case class EceServiceConfig(
      baseUrl: String = EceServiceConfig.defaultBaseUrl,
      healthPath: String = "/health",
      boardQuickPath: String = "/v1/ece/board/quick",
      boardDeepPath: String = "/v1/ece/board/deep",
      boardPath: String = "/v1/ece/board",
      proposedMovePath: String = "/v1/ece/proposed-move",
      gameReviewPath: String = "/v1/ece/game-review",
      fullMatchPath: String = "/v1/ece/full-match",
      fullMatchSummaryPath: String = "/v1/ece/full-match-summary"
  ):
    def valid: Boolean =
      EceServiceConfig.validLocalBaseUrl(baseUrl) &&
        healthPath == "/health" &&
        boardQuickPath == "/v1/ece/board/quick" &&
        boardDeepPath == "/v1/ece/board/deep" &&
        boardPath == "/v1/ece/board" &&
        proposedMovePath == "/v1/ece/proposed-move" &&
        gameReviewPath == "/v1/ece/game-review" &&
        fullMatchPath == "/v1/ece/full-match" &&
        fullMatchSummaryPath == "/v1/ece/full-match-summary"

    private def cleanBaseUrl: String = EceServiceConfig.normalizeBaseUrl(baseUrl)

    def healthUrl: String = s"$cleanBaseUrl$healthPath"

    def boardQuickUrl: String = s"$cleanBaseUrl$boardQuickPath"

    def boardDeepUrl: String = s"$cleanBaseUrl$boardDeepPath"

    def legacyBoardUrl: String = s"$cleanBaseUrl$boardPath"

    def boardUrl: String = boardQuickUrl

    def proposedMoveUrl: String = s"$cleanBaseUrl$proposedMovePath"

    def gameReviewUrl: String = s"$cleanBaseUrl$gameReviewPath"

    def fullMatchUrl: String = s"$cleanBaseUrl$fullMatchPath"

    def fullMatchSummaryUrl: String = s"$cleanBaseUrl$fullMatchSummaryPath"

  object EceServiceConfig:
    val defaultBaseUrl = "http://127.0.0.1:8787"
    private val allowedLocalHosts = Set("127.0.0.1", "localhost", "host.docker.internal", "::1", "[::1]")

    def normalizeBaseUrl(value: String): String =
      value.trim.stripSuffix("/")

    def validLocalBaseUrl(value: String): Boolean =
      try
        val uri = URI.create(normalizeBaseUrl(value))
        val host = Option(uri.getHost).getOrElse("")
        val path = Option(uri.getRawPath).getOrElse("")
        uri.getScheme == "http" &&
        allowedLocalHosts.contains(host.toLowerCase) &&
        uri.getRawUserInfo == null &&
        (uri.getPort == -1 || (uri.getPort > 0 && uri.getPort <= 65535)) &&
        (path.isEmpty || path == "/") &&
        uri.getRawQuery == null &&
        uri.getRawFragment == null
      catch case _: IllegalArgumentException => false

  object EceEndpointPolicy:
    val serverToServerOnly = true
    val browserMayCallEceDirectly = false
    val requestMayContainApiKeys = false
    val requestMayContainProviderPaths = false
    val clientMaySupplyEcePayload = false
    val eceMayOverrideMatchContract = false

    def valid: Boolean =
      serverToServerOnly &&
        !browserMayCallEceDirectly &&
        !requestMayContainApiKeys &&
        !requestMayContainProviderPaths &&
        !clientMaySupplyEcePayload &&
        !eceMayOverrideMatchContract

  enum EceInternalProvider:
    case Stockfish
    case SyzygyTablebase
    case OpeningBook
    case LichessEvalCache
    case RulesLegalMove
    case MaiaHumanRisk
    case AiText

  final case class EceProviderStatus(
      provider: EceInternalProvider,
      configured: Boolean,
      reachable: Boolean,
      latencyMillis: Option[Int],
      sanitizedLabel: String,
      exposesSecret: Boolean,
      exposesFilesystemPath: Boolean,
      exposesRawPrompt: Boolean,
      exposesRawProviderOutput: Boolean
  ):
    def safeForLichessDiagnostics: Boolean =
      sanitizedLabel.nonEmpty &&
        latencyMillis.forall(_ >= 0) &&
        !exposesSecret &&
        !exposesFilesystemPath &&
        !exposesRawPrompt &&
        !exposesRawProviderOutput

  object EceProviderBoundary:
    val lichessMayCallEce = true
    val lichessMayCallProvidersDirectly = false
    val browserMayCallProvidersDirectly = false
    val providerSecretsMayEnterLichessPayloads = false
    val rawProviderOutputMayEnterPublicPayloads = false
    val displayRequiresEceNormalizedSideOutput = true

    def directCallAllowed(provider: EceInternalProvider): Boolean = false

    def valid: Boolean =
      lichessMayCallEce &&
        !lichessMayCallProvidersDirectly &&
        !browserMayCallProvidersDirectly &&
        !providerSecretsMayEnterLichessPayloads &&
        !rawProviderOutputMayEnterPublicPayloads &&
        displayRequiresEceNormalizedSideOutput

  final case class EceProviderAwarenessSnapshot(
      statuses: List[EceProviderStatus],
      callsEceOnly: Boolean,
      normalizedBeforeDisplay: Boolean
  ):
    def safeForLichess: Boolean =
      callsEceOnly &&
        normalizedBeforeDisplay &&
        EceProviderBoundary.valid &&
        statuses.forall(_.safeForLichessDiagnostics) &&
        EceInternalProvider.values.forall(provider => !EceProviderBoundary.directCallAllowed(provider))

  final case class EceHealthStatus(
      status: String,
      service: String,
      mode: String,
      openAiConfigured: Boolean,
      stockfishConfigured: Boolean
  ):
    def reachable: Boolean =
      status == "ok" && service == "EvenChessEngine"

    def safeForDiagnostics: Boolean =
      service.nonEmpty && mode.nonEmpty

  final case class EceCustomProfile(
      opening: Int,
      instructions: Int
  ):
    def valid: Boolean =
      opening >= 0 && instructions >= 0

  object EceCustomProfile:
    val default: EceCustomProfile = EceCustomProfile(opening = 0, instructions = 0)

  final case class EceBoardStateRequest(
      mode: String,
      requestId: String,
      inputFen: String,
      ratingType: String,
      whiteRatingInput: Int,
      blackRatingInput: Int,
      whiteLevel: Level,
      blackLevel: Level,
      useAi: Int,
      custom: EceCustomProfile,
      deepRequested: Boolean,
      requestedDeepModules: List[String]
  ):
    def valid: Boolean =
      mode == "board_state" &&
        requestId.nonEmpty &&
        inputFen.nonEmpty &&
        (ratingType == "ecr" || ratingType == "unknown") &&
        whiteRatingInput >= 0 &&
        blackRatingInput >= 0 &&
        (useAi == 0 || useAi == 1) &&
        custom.valid &&
        requestedDeepModules.forall(EceBoardStateRequest.validDeepModule) &&
        (deepRequested || requestedDeepModules.isEmpty)

    def cacheKey(engineVersion: String): String =
      List(
        mode,
        inputFen,
        whiteLevel.value,
        blackLevel.value,
        useAi,
        custom.opening,
        custom.instructions,
        deepRequested,
        requestedDeepModules.mkString(","),
        engineVersion
      )
        .mkString("|")

  object EceBoardStateRequest:
    val allowedDeepModules: Set[String] =
      Set("stockfish", "syzygy", "lichess_eval_cache", "maia", "ai_text", "opening_book")

    def validDeepModule(module: String): Boolean =
      allowedDeepModules.contains(module)

    def deepRequiredForLevels(whiteLevel: Level, blackLevel: Level, custom: EceCustomProfile, aiTextAllowed: Boolean): Boolean =
      val highestRequestedLevel = math.max(whiteLevel.value, blackLevel.value)
      highestRequestedLevel >= 5 ||
        (custom.opening != 0 && highestRequestedLevel >= 4) ||
        (aiTextAllowed && highestRequestedLevel >= 4)

    def requestedDeepModulesFor(
        whiteLevel: Level,
        blackLevel: Level,
        custom: EceCustomProfile,
        aiTextAllowed: Boolean
    ): List[String] =
      val highestRequestedLevel = math.max(whiteLevel.value, blackLevel.value)
      if !deepRequiredForLevels(whiteLevel, blackLevel, custom, aiTextAllowed) then Nil
      else
        List(
          Option.when(highestRequestedLevel >= 5)("stockfish"),
          Option.when(highestRequestedLevel >= 8)("lichess_eval_cache"),
          Option.when(highestRequestedLevel >= 8)("syzygy"),
          Option.when(highestRequestedLevel >= 9)("maia"),
          Option.when(custom.opening != 0 && highestRequestedLevel >= 4)("opening_book"),
          Option.when(aiTextAllowed)("ai_text")
        ).flatten

    def boardState(
        gameId: String,
        ply: Int,
        inputFen: String,
        whiteEcr: Option[Int],
        blackEcr: Option[Int],
        whiteLevel: Level,
        blackLevel: Level,
        aiTextAllowed: Boolean,
        custom: EceCustomProfile = EceCustomProfile.default
    ): EceBoardStateRequest =
      val deepRequested = deepRequiredForLevels(whiteLevel, blackLevel, custom, aiTextAllowed)
      EceBoardStateRequest(
        mode = "board_state",
        requestId = s"$gameId-ply-$ply-quick",
        inputFen = inputFen,
        ratingType = if whiteEcr.isDefined || blackEcr.isDefined then "ecr" else "unknown",
        whiteRatingInput = whiteEcr.getOrElse(0),
        blackRatingInput = blackEcr.getOrElse(0),
        whiteLevel = whiteLevel,
        blackLevel = blackLevel,
        useAi = if aiTextAllowed && deepRequested then 1 else 0,
        custom = custom,
        deepRequested = deepRequested,
        requestedDeepModules = requestedDeepModulesFor(whiteLevel, blackLevel, custom, aiTextAllowed)
      )

  final case class EceBoardDeepRequest(
      mode: String,
      requestId: String,
      quickRequestId: String,
      quickContextId: String,
      inputFen: String,
      whiteLevel: Level,
      blackLevel: Level,
      useAi: Int,
      requestedDeepModules: List[String]
  ):
    def valid: Boolean =
      mode == "board_deep" &&
        requestId.nonEmpty &&
        quickRequestId.nonEmpty &&
        quickContextId.nonEmpty &&
        inputFen.nonEmpty &&
        (useAi == 0 || useAi == 1) &&
        requestedDeepModules.forall(EceBoardStateRequest.validDeepModule)

  object EceBoardDeepRequest:
    def fromQuick(quick: EceBoardStateRequest, quickContextId: String): EceBoardDeepRequest =
      EceBoardDeepRequest(
        mode = "board_deep",
        requestId = quick.requestId.stripSuffix("-quick") + "-deep",
        quickRequestId = quick.requestId,
        quickContextId = quickContextId,
        inputFen = quick.inputFen,
        whiteLevel = quick.whiteLevel,
        blackLevel = quick.blackLevel,
        useAi = quick.useAi,
        requestedDeepModules = quick.requestedDeepModules
      )

  final case class EceProposedMoveRequest(
      mode: String,
      requestId: String,
      inputFen: String,
      proposedMoveUci: String,
      ratingType: String,
      whiteRatingInput: Int,
      blackRatingInput: Int,
      whiteLevel: Level,
      blackLevel: Level,
      useAi: Int,
      custom: EceCustomProfile
  ):
    def exactlyOneProposedMove: Boolean =
      proposedMoveUci.matches("[a-h][1-8][a-h][1-8][qrbn]?")

    def valid: Boolean =
      mode == "proposed_move" &&
        requestId.nonEmpty &&
        inputFen.nonEmpty &&
        exactlyOneProposedMove &&
        (ratingType == "ecr" || ratingType == "unknown") &&
        whiteRatingInput >= 0 &&
        blackRatingInput >= 0 &&
        (useAi == 0 || useAi == 1) &&
        custom.valid

    def cacheKey(engineVersion: String): String =
      List(mode, inputFen, proposedMoveUci, whiteLevel.value, blackLevel.value, useAi, custom.opening, custom.instructions, engineVersion)
        .mkString("|")

  object EceProposedMoveRequest:
    def proposedMove(
        gameId: String,
        ply: Int,
        proposalIndex: Int,
        inputFen: String,
        proposedMoveUci: String,
        whiteEcr: Option[Int],
        blackEcr: Option[Int],
        whiteLevel: Level,
        blackLevel: Level,
        aiTextAllowed: Boolean,
        custom: EceCustomProfile = EceCustomProfile.default
    ): EceProposedMoveRequest =
      EceProposedMoveRequest(
        mode = "proposed_move",
        requestId = s"$gameId-ply-$ply-pm-$proposalIndex",
        inputFen = inputFen,
        proposedMoveUci = proposedMoveUci,
        ratingType = if whiteEcr.isDefined || blackEcr.isDefined then "ecr" else "unknown",
        whiteRatingInput = whiteEcr.getOrElse(0),
        blackRatingInput = blackEcr.getOrElse(0),
        whiteLevel = whiteLevel,
        blackLevel = blackLevel,
        useAi = if aiTextAllowed then 1 else 0,
        custom = custom
      )

  final case class EceGameReviewInput(
      gameId: String,
      initialFen: String,
      pgn: Option[String],
      moves: List[String],
      fenHistory: List[String],
      result: String,
      termination: String
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        initialFen.nonEmpty &&
        pgn.forall(_.nonEmpty) &&
        moves.forall(_.nonEmpty) &&
        fenHistory.nonEmpty &&
        fenHistory.forall(_.nonEmpty) &&
        result.nonEmpty &&
        termination.nonEmpty

  final case class EceLiveSnapshotRef(
      ply: Int,
      fen: String,
      sideToMove: Perspective,
      whiteOutputRef: Option[String],
      blackOutputRef: Option[String]
  ):
    def valid: Boolean =
      ply >= 0 &&
        fen.nonEmpty &&
        whiteOutputRef.forall(_.nonEmpty) &&
        blackOutputRef.forall(_.nonEmpty)

  final case class EceGameReviewRequest(
      mode: String,
      requestId: String,
      game: EceGameReviewInput,
      ratingType: String,
      whiteRatingInput: Int,
      blackRatingInput: Int,
      reviewLevel: Level,
      useAi: Int,
      custom: EceCustomProfile,
      liveEceSnapshots: List[EceLiveSnapshotRef]
  ):
    def valid: Boolean =
      (mode == "full_match" || mode == "game_review") &&
        requestId.nonEmpty &&
        game.valid &&
        (ratingType == "ecr" || ratingType == "unknown") &&
        whiteRatingInput >= 0 &&
        blackRatingInput >= 0 &&
        (useAi == 0 || useAi == 1) &&
        custom.valid &&
        liveEceSnapshots.forall(_.valid)

    def cacheKey(engineVersion: String): String =
      List(
        mode,
        game.gameId,
        game.initialFen,
        game.moves.mkString(","),
        game.fenHistory.mkString(","),
        game.result,
        game.termination,
        reviewLevel.value,
        useAi,
        custom.opening,
        custom.instructions,
        liveEceSnapshots.map(snapshot => s"${snapshot.ply}:${snapshot.whiteOutputRef.getOrElse("")}:${snapshot.blackOutputRef.getOrElse("")}").mkString(","),
        engineVersion
      ).mkString("|")

  object EceGameReviewRequest:
    def gameReview(
        gameId: String,
        reviewIndex: Int,
        game: EceGameReviewInput,
        whiteEcr: Option[Int],
        blackEcr: Option[Int],
        reviewLevel: Level,
        aiNarrativeAllowed: Boolean,
        liveEceSnapshots: List[EceLiveSnapshotRef],
        custom: EceCustomProfile = EceCustomProfile.default
    ): EceGameReviewRequest =
      EceGameReviewRequest(
        mode = "full_match",
        requestId = s"$gameId-review-$reviewIndex",
        game = game,
        ratingType = if whiteEcr.isDefined || blackEcr.isDefined then "ecr" else "unknown",
        whiteRatingInput = whiteEcr.getOrElse(0),
        blackRatingInput = blackEcr.getOrElse(0),
        reviewLevel = reviewLevel,
        useAi = if aiNarrativeAllowed then 1 else 0,
        custom = custom,
        liveEceSnapshots = liveEceSnapshots
      )

  enum EceBridgeGameSource:
    case Human
    case MatchmakingBot
    case SimulationBot

    def botDriven: Boolean =
      this != Human

    def label: String =
      this match
        case Human          => "human"
        case MatchmakingBot => "matchmaking_bot"
        case SimulationBot  => "simulation_bot"

  final case class EceBridgeParticipant(
      playerId: String,
      level: Level,
      ecr: Option[Int],
      source: EceBridgeGameSource
  ):
    def valid: Boolean =
      playerId.nonEmpty &&
        Level.isValid(level.value) &&
        ecr.forall(_ >= 0)

    def botDriven: Boolean =
      source.botDriven

  final case class EceBridgeGameContext(
      gameId: String,
      ply: Int,
      inputFen: String,
      white: EceBridgeParticipant,
      black: EceBridgeParticipant,
      aiTextAllowed: Boolean,
      custom: EceCustomProfile = EceCustomProfile.default
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        ply >= 0 &&
        inputFen.nonEmpty &&
        white.valid &&
        black.valid &&
        custom.valid

    def botDriven: Boolean =
      white.botDriven || black.botDriven

    def boardStateRequest: EceBoardStateRequest =
      EceBoardStateRequest.boardState(
        gameId = gameId,
        ply = ply,
        inputFen = inputFen,
        whiteEcr = white.ecr,
        blackEcr = black.ecr,
        whiteLevel = white.level,
        blackLevel = black.level,
        aiTextAllowed = aiTextAllowed,
        custom = custom
      )

    def usesSameBoardContractAsHuman: Boolean =
      valid &&
        boardStateRequest.valid &&
        boardStateRequest.mode == "board_state"

  final case class EceBoardStateEnvelope(
      request: EceBoardStateRequest,
      gameContext: Option[EceBridgeGameContext] = None
  ):
    def valid: Boolean =
      request.valid &&
        gameContext.forall(context => context.valid && context.boardStateRequest == request && context.usesSameBoardContractAsHuman)

    def botDriven: Boolean =
      gameContext.exists(_.botDriven)

  final case class EceBoardDeepEnvelope(request: EceBoardDeepRequest):
    def valid: Boolean = request.valid

  final case class EceProposedMoveEnvelope(request: EceProposedMoveRequest):
    def valid: Boolean = request.valid

  final case class EceGameReviewEnvelope(request: EceGameReviewRequest):
    def valid: Boolean = request.valid

  final case class EceRequestEcho(
      requestId: String,
      inputFen: String,
      whiteLevel: Level,
      blackLevel: Level
  )

  final case class EceProposedMoveRequestEcho(
      requestId: String,
      inputFen: String,
      proposedMoveUci: String,
      whiteLevel: Level,
      blackLevel: Level
  )

  final case class EceGameReviewRequestEcho(
      requestId: String,
      gameId: String,
      reviewLevel: Level
  )

  enum EceDiagnosticsStatus:
    case Ok
    case Partial
    case InvalidRequest
    case InvalidFen
    case InvalidGame
    case StockfishUnavailable
    case AiUnavailable
    case InternalError

    def displayAllowed: Boolean =
      this == Ok || this == Partial || this == StockfishUnavailable || this == AiUnavailable

    def sanitizedUserLabel: String =
      this match
        case Ok                   => "ready"
        case Partial              => "partial"
        case StockfishUnavailable => "degraded"
        case AiUnavailable        => "ai_unavailable"
        case _                    => "unavailable"

  final case class EceDiagnostics(
      status: EceDiagnosticsStatus,
      engineVersion: String,
      sanitizedMessage: Option[String]
  ):
    def displayAllowed: Boolean = status.displayAllowed

    def safeForClient: Boolean =
      sanitizedMessage.forall(message => !message.toLowerCase.contains("key") && !message.contains("\\"))

  final case class EceLevelEcho(
      requestedLevel: Level,
      deliveredLevel: Level,
      defaulted: Boolean
  ):
    def valid: Boolean =
      deliveredLevel.value <= requestedLevel.value

  final case class EceSideOutput(
      side: Perspective,
      studentSide: Perspective,
      opponentSide: Perspective,
      level: EceLevelEcho,
      isSideToMove: Boolean,
      summary: Option[String],
      immediateWarning: Option[String],
      plan: Option[String]
  ):
    def sideConsistent: Boolean =
      side == studentSide && side != opponentSide

    def valid: Boolean =
      sideConsistent && level.valid

  final case class EceBoardStateResponse(
      requestEcho: EceRequestEcho,
      white: Option[EceSideOutput],
      black: Option[EceSideOutput],
      diagnostics: EceDiagnostics,
      hasPublicPosition: Boolean,
      hasPublicSharedCalculations: Boolean,
      rawProviderPayload: Option[String]
  ):
    def sideOutputsPresent: Boolean =
      white.exists(_.valid) && black.exists(_.valid)

    def noPublicUngatedCalculations: Boolean =
      !hasPublicPosition && !hasPublicSharedCalculations && rawProviderPayload.isEmpty

    def safeShapeForLichess: Boolean =
      sideOutputsPresent && noPublicUngatedCalculations && diagnostics.safeForClient

  final case class EceOutstandingRequest(
      request: EceBoardStateRequest,
      currentFen: String
  ):
    def valid: Boolean =
      request.valid && currentFen.nonEmpty

  final case class EceProposedMoveResponse(
      requestEcho: EceProposedMoveRequestEcho,
      white: Option[EceSideOutput],
      black: Option[EceSideOutput],
      diagnostics: EceDiagnostics,
      hasPublicPosition: Boolean,
      hasPublicSharedCalculations: Boolean,
      rawProviderPayload: Option[String]
  ):
    def sideOutputsPresent: Boolean =
      white.exists(_.valid) && black.exists(_.valid)

    def noPublicUngatedCalculations: Boolean =
      !hasPublicPosition && !hasPublicSharedCalculations && rawProviderPayload.isEmpty

    def safeShapeForLichess: Boolean =
      sideOutputsPresent && noPublicUngatedCalculations && diagnostics.safeForClient

  final case class EceProposedMoveOutstandingRequest(
      request: EceProposedMoveRequest,
      currentFen: String,
      requesterSide: Perspective,
      sideToMove: Perspective,
      proposedMoveHelpAllowed: Boolean
  ):
    def requesterIsSideToMove: Boolean =
      requesterSide == sideToMove

    def valid: Boolean =
      request.valid &&
        currentFen.nonEmpty &&
        currentFen == request.inputFen &&
        requesterIsSideToMove &&
        proposedMoveHelpAllowed

  final case class EceGameReviewMoveOutput(
      ply: Int,
      fen: String,
      white: Option[EceSideOutput],
      black: Option[EceSideOutput]
  ):
    def valid(reviewLevel: Level): Boolean =
      ply >= 0 &&
        fen.nonEmpty &&
        white.forall(output => output.side == Perspective.White && output.level.deliveredLevel.value <= reviewLevel.value && output.valid) &&
        black.forall(output => output.side == Perspective.Black && output.level.deliveredLevel.value <= reviewLevel.value && output.valid)

  final case class EceGameReviewResponse(
      requestEcho: EceGameReviewRequestEcho,
      gameSummary: Option[String],
      whitePerformanceSummary: Option[String],
      blackPerformanceSummary: Option[String],
      turningPoints: List[String],
      recurringMotifs: List[String],
      missedThreats: List[String],
      moveOutputs: List[EceGameReviewMoveOutput],
      diagnostics: EceDiagnostics,
      hasRawProviderPayload: Boolean,
      hasRawAiPromptOrResponse: Boolean,
      attemptsToAlterGameResult: Boolean,
      attemptsToAlterLiveUsedLevel: Boolean,
      attemptsToAlterAssistanceLoad: Boolean,
      attemptsToAlterUsedOffset: Boolean,
      attemptsToAlterEcr: Boolean,
      attemptsToAlterMatchmakingState: Boolean
  ):
    def narrativePresent: Boolean =
      List(gameSummary, whitePerformanceSummary, blackPerformanceSummary).exists(_.exists(_.nonEmpty)) ||
        turningPoints.nonEmpty ||
        recurringMotifs.nonEmpty ||
        missedThreats.nonEmpty

    def noUnsafeRawData: Boolean =
      !hasRawProviderPayload && !hasRawAiPromptOrResponse

    def doesNotMutateRatedGame: Boolean =
      !attemptsToAlterGameResult &&
        !attemptsToAlterLiveUsedLevel &&
        !attemptsToAlterAssistanceLoad &&
        !attemptsToAlterUsedOffset &&
        !attemptsToAlterEcr &&
        !attemptsToAlterMatchmakingState

    def safeShapeForLichess: Boolean =
      diagnostics.safeForClient &&
        noUnsafeRawData &&
        doesNotMutateRatedGame &&
        narrativePresent &&
        moveOutputs.forall(_.valid(requestEcho.reviewLevel))

  final case class EceGameReviewOutstandingRequest(
      request: EceGameReviewRequest,
      postGameOnly: Boolean,
      tokenQuotaChecked: Boolean
  ):
    def valid: Boolean =
      request.valid &&
        postGameOnly &&
        tokenQuotaChecked

  object EceBoardStateValidator:
    def responseUsable(outstanding: EceOutstandingRequest, response: EceBoardStateResponse): Boolean =
      outstanding.valid &&
        response.safeShapeForLichess &&
        response.diagnostics.displayAllowed &&
        response.requestEcho.requestId == outstanding.request.requestId &&
        response.requestEcho.inputFen == outstanding.request.inputFen &&
        response.requestEcho.inputFen == outstanding.currentFen &&
        response.requestEcho.whiteLevel == outstanding.request.whiteLevel &&
        response.requestEcho.blackLevel == outstanding.request.blackLevel

    def displayForSide(response: EceBoardStateResponse, side: Perspective): Option[EceSideOutput] =
      side match
        case Perspective.White => response.white.filter(_.valid)
        case Perspective.Black => response.black.filter(_.valid)

  object EceProposedMoveValidator:
    def responseUsable(outstanding: EceProposedMoveOutstandingRequest, response: EceProposedMoveResponse): Boolean =
      outstanding.valid &&
        response.safeShapeForLichess &&
        response.diagnostics.displayAllowed &&
        response.requestEcho.requestId == outstanding.request.requestId &&
        response.requestEcho.inputFen == outstanding.request.inputFen &&
        response.requestEcho.proposedMoveUci == outstanding.request.proposedMoveUci &&
        response.requestEcho.whiteLevel == outstanding.request.whiteLevel &&
        response.requestEcho.blackLevel == outstanding.request.blackLevel

    def displayForSide(response: EceProposedMoveResponse, side: Perspective): Option[EceSideOutput] =
      side match
        case Perspective.White => response.white.filter(_.valid)
        case Perspective.Black => response.black.filter(_.valid)

  object EceGameReviewValidator:
    def responseUsable(outstanding: EceGameReviewOutstandingRequest, response: EceGameReviewResponse): Boolean =
      outstanding.valid &&
        response.safeShapeForLichess &&
        response.diagnostics.displayAllowed &&
        response.requestEcho.requestId == outstanding.request.requestId &&
        response.requestEcho.gameId == outstanding.request.game.gameId &&
        response.requestEcho.reviewLevel == outstanding.request.reviewLevel

  final case class EceFrameworkDecision(
      config: EceServiceConfig,
      request: Option[EceBoardStateEnvelope],
      health: Option[EceHealthStatus],
      response: Option[EceBoardStateResponse],
      displayableForRequester: Option[EceSideOutput],
      status: EngineJobStatus,
      browserDirectCallBlocked: Boolean,
      nonFatalToGameLifecycle: Boolean
  ):
    def valid: Boolean =
      config.valid &&
        EceEndpointPolicy.valid &&
        browserDirectCallBlocked &&
        nonFatalToGameLifecycle &&
        request.forall(_.valid) &&
        response.forall(_.safeShapeForLichess)

    def coachingAvailable: Boolean =
      status == EngineJobStatus.Ready && displayableForRequester.exists(_.valid)

  object EceFrameworkIntegration:
    def unavailable(config: EceServiceConfig, health: Option[EceHealthStatus]): EceFrameworkDecision =
      EceFrameworkDecision(
        config = config,
        request = None,
        health = health,
        response = None,
        displayableForRequester = None,
        status = EngineJobStatus.Degraded,
        browserDirectCallBlocked = true,
        nonFatalToGameLifecycle = true
      )

    def prepareBoardState(
        config: EceServiceConfig,
        request: EceBoardStateRequest
    ): Either[String, EceBoardStateEnvelope] =
      Either.cond(config.valid && request.valid, EceBoardStateEnvelope(request), "invalid_ece_board_state_request")

    def prepareBoardStateForGame(
        config: EceServiceConfig,
        context: EceBridgeGameContext
    ): Either[String, EceBoardStateEnvelope] =
      val request = context.boardStateRequest
      Either.cond(
        config.valid && context.usesSameBoardContractAsHuman && request.valid,
        EceBoardStateEnvelope(request, Some(context)),
        "invalid_ece_board_state_game_context"
      )

    def prepareProposedMove(
        config: EceServiceConfig,
        request: EceProposedMoveRequest
    ): Either[String, EceProposedMoveEnvelope] =
      Either.cond(config.valid && request.valid, EceProposedMoveEnvelope(request), "invalid_ece_proposed_move_request")

    def prepareGameReview(
        config: EceServiceConfig,
        request: EceGameReviewRequest,
        tokenQuotaChecked: Boolean
    ): Either[String, EceGameReviewEnvelope] =
      Either.cond(config.valid && request.valid && tokenQuotaChecked, EceGameReviewEnvelope(request), "invalid_ece_game_review_request")

    def acceptBoardState(
        config: EceServiceConfig,
        outstanding: EceOutstandingRequest,
        response: EceBoardStateResponse,
        requesterSide: Perspective
    ): EceFrameworkDecision =
      val usable = EceBoardStateValidator.responseUsable(outstanding, response)
      EceFrameworkDecision(
        config = config,
        request = Some(EceBoardStateEnvelope(outstanding.request)),
        health = None,
        response = Some(response),
        displayableForRequester = Option.when(usable)(EceBoardStateValidator.displayForSide(response, requesterSide)).flatten,
        status = if usable then EngineJobStatus.Ready else EngineJobStatus.Stale,
        browserDirectCallBlocked = true,
        nonFatalToGameLifecycle = true
      )

  final case class EceProposedMoveFrameworkDecision(
      config: EceServiceConfig,
      request: Option[EceProposedMoveEnvelope],
      response: Option[EceProposedMoveResponse],
      displayableForRequester: Option[EceSideOutput],
      status: EngineJobStatus,
      previewOnly: Boolean,
      browserDirectCallBlocked: Boolean,
      nonFatalToGameLifecycle: Boolean
  ):
    def valid: Boolean =
      config.valid &&
        EceEndpointPolicy.valid &&
        previewOnly &&
        browserDirectCallBlocked &&
        nonFatalToGameLifecycle &&
        request.forall(_.valid) &&
        response.forall(_.safeShapeForLichess)

    def previewAvailable: Boolean =
      status == EngineJobStatus.Ready && displayableForRequester.exists(_.valid)

  object EceProposedMoveFrameworkIntegration:
    def acceptProposedMove(
        config: EceServiceConfig,
        outstanding: EceProposedMoveOutstandingRequest,
        response: EceProposedMoveResponse,
        requesterSide: Perspective
    ): EceProposedMoveFrameworkDecision =
      val usable = EceProposedMoveValidator.responseUsable(outstanding, response)
      EceProposedMoveFrameworkDecision(
        config = config,
        request = Some(EceProposedMoveEnvelope(outstanding.request)),
        response = Some(response),
        displayableForRequester = Option.when(usable)(EceProposedMoveValidator.displayForSide(response, requesterSide)).flatten,
        status = if usable then EngineJobStatus.Ready else EngineJobStatus.Stale,
        previewOnly = true,
        browserDirectCallBlocked = true,
        nonFatalToGameLifecycle = true
      )

  final case class EceGameReviewFrameworkDecision(
      config: EceServiceConfig,
      request: Option[EceGameReviewEnvelope],
      response: Option[EceGameReviewResponse],
      status: EngineJobStatus,
      postGameOnly: Boolean,
      tokenQuotaChecked: Boolean,
      browserDirectCallBlocked: Boolean,
      nonFatalToGameLifecycle: Boolean,
      mutatesRatedGame: Boolean
  ):
    def valid: Boolean =
      config.valid &&
        EceEndpointPolicy.valid &&
        postGameOnly &&
        tokenQuotaChecked &&
        browserDirectCallBlocked &&
        nonFatalToGameLifecycle &&
        !mutatesRatedGame &&
        request.forall(_.valid) &&
        response.forall(_.safeShapeForLichess)

    def reviewAvailable: Boolean =
      status == EngineJobStatus.Ready && response.exists(_.safeShapeForLichess)

  object EceGameReviewFrameworkIntegration:
    def acceptGameReview(
        config: EceServiceConfig,
        outstanding: EceGameReviewOutstandingRequest,
        response: EceGameReviewResponse
    ): EceGameReviewFrameworkDecision =
      val usable = EceGameReviewValidator.responseUsable(outstanding, response)
      EceGameReviewFrameworkDecision(
        config = config,
        request = Some(EceGameReviewEnvelope(outstanding.request)),
        response = Some(response),
        status = if usable then EngineJobStatus.Ready else EngineJobStatus.Stale,
        postGameOnly = outstanding.postGameOnly,
        tokenQuotaChecked = outstanding.tokenQuotaChecked,
        browserDirectCallBlocked = true,
        nonFatalToGameLifecycle = true,
        mutatesRatedGame = !response.doesNotMutateRatedGame
      )
