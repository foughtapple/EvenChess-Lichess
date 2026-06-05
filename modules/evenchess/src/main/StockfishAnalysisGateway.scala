package lila.evenchess

import AiCoachPolicy.SourceFact
import CoachingLadder.{ ExactnessClass, Level }
import EngineGateway.*
import ProductInvariants.RequirementClass

object StockfishAnalysisGateway:

  enum PhaseNStockfishRequirement:
    case EceSideOnlyProviderCalls
    case NoRawStockfishToClients
    case CandidateCountsLevelGated
    case NumericEvalStartsAtL8
    case ProfilesBounded
    case UnavailableFallsBackSafely

  final case class PhaseNStockfishRequirementClassification(
      requirement: PhaseNStockfishRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseNStockfishRequirementClassifications:
    val all: List[PhaseNStockfishRequirementClassification] = List(
      PhaseNStockfishRequirementClassification(
        PhaseNStockfishRequirement.EceSideOnlyProviderCalls,
        RequirementClass.EvenChessSpecific,
        "The Lichess fork models Stockfish provider policy but does not call Stockfish directly for ECE coaching; ECE owns provider execution."
      ),
      PhaseNStockfishRequirementClassification(
        PhaseNStockfishRequirement.NoRawStockfishToClients,
        RequirementClass.EvenChessSpecific,
        "Raw Stockfish protocol lines are stripped before any client-eligible packet."
      ),
      PhaseNStockfishRequirementClassification(
        PhaseNStockfishRequirement.CandidateCountsLevelGated,
        RequirementClass.EvenChessSpecific,
        "Candidate counts are capped by level: L5 one, L6 two, L7+ configured bounded cap."
      ),
      PhaseNStockfishRequirementClassification(
        PhaseNStockfishRequirement.NumericEvalStartsAtL8,
        RequirementClass.EvenChessSpecific,
        "Numeric eval/WDL display is unavailable before L8 and must carry an approximate label."
      ),
      PhaseNStockfishRequirementClassification(
        PhaseNStockfishRequirement.ProfilesBounded,
        RequirementClass.EvenChessSpecific,
        "Depth, nodes, movetime, MultiPV, threads, and hash limits remain bounded by server profile."
      ),
      PhaseNStockfishRequirementClassification(
        PhaseNStockfishRequirement.UnavailableFallsBackSafely,
        RequirementClass.EvenChessSpecific,
        "Stockfish unavailable/timeout/degraded states produce safe fallback packets rather than raw or stale engine output."
      )
    )

  enum PhaseGRequirement:
    case LichessAnalysisInfrastructureProvided
    case InternalStockfishGateway
    case NoBrowserStockfishForRatedHelp
    case BoundedProfilesByLevelAndTime
    case QueueCacheCancelTimeoutFallback
    case SanitizedTruthPackets
    case EngineInventoryAndLicense
    case AiReceivesTruthPacketsOnly
    case CoreAdapterDeferredToThinSeams

  final case class PhaseGRequirementClassification(
      requirement: PhaseGRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseGRequirementClassifications:
    val all: List[PhaseGRequirementClassification] = List(
      PhaseGRequirementClassification(
        PhaseGRequirement.LichessAnalysisInfrastructureProvided,
        RequirementClass.LichessProvided,
        "Use existing lila analysis, fishnet, eval cache, and review foundations where safe; do not rebuild chess analysis basics."
      ),
      PhaseGRequirementClassification(
        PhaseGRequirement.InternalStockfishGateway,
        RequirementClass.EvenChessSpecific,
        "Live EvenChess engine help must pass through an internal server-side gateway."
      ),
      PhaseGRequirementClassification(
        PhaseGRequirement.NoBrowserStockfishForRatedHelp,
        RequirementClass.EvenChessSpecific,
        "Browser clients and debug endpoints cannot request live rated Stockfish assistance directly."
      ),
      PhaseGRequirementClassification(
        PhaseGRequirement.BoundedProfilesByLevelAndTime,
        RequirementClass.EvenChessSpecific,
        "Depth, nodes, movetime, MultiPV, threads, hash, candidate count, and eval exposure are capped by Set Level and time control."
      ),
      PhaseGRequirementClassification(
        PhaseGRequirement.QueueCacheCancelTimeoutFallback,
        RequirementClass.EvenChessSpecific,
        "The gateway contract includes live/post-game queue selection, cache correctness, cancellation, timeout, stale, degraded, and fallback states."
      ),
      PhaseGRequirementClassification(
        PhaseGRequirement.SanitizedTruthPackets,
        RequirementClass.EvenChessSpecific,
        "Gateway output is a bounded truth packet with no raw Stockfish protocol payloads or hidden debug data."
      ),
      PhaseGRequirementClassification(
        PhaseGRequirement.EngineInventoryAndLicense,
        RequirementClass.AdaptedToLichessFork,
        "Engine version/source/hash and GPL distribution obligations must stay attached to jobs."
      ),
      PhaseGRequirementClassification(
        PhaseGRequirement.AiReceivesTruthPacketsOnly,
        RequirementClass.EvenChessSpecific,
        "AI may explain sanitized gateway truth packets only; it does not choose or deepen engine help."
      ),
      PhaseGRequirementClassification(
        PhaseGRequirement.CoreAdapterDeferredToThinSeams,
        RequirementClass.AdaptedToLichessFork,
        "Later lila/fishnet/eval-cache adapters must call this gateway through narrow patch-mapped seams."
      )
    )

  enum LilaEngineSeam:
    case FishnetAnalysisQueue
    case AnalyseReviewModule
    case EvalCache
    case ExternalEngineRegistry
    case TablebaseExactProof

  final case class LilaEngineSeamInspection(
      seam: LilaEngineSeam,
      inspectedPath: String,
      reusableForEvenChess: Boolean,
      phaseGDirection: String,
      coreEditRequiredNow: Boolean,
      classification: RequirementClass
  )

  object LilaEngineSeamInventory:
    val all: List[LilaEngineSeamInspection] = List(
      LilaEngineSeamInspection(
        LilaEngineSeam.FishnetAnalysisQueue,
        "modules/fishnet/src/main and app/controllers/Fishnet.scala",
        reusableForEvenChess = true,
        "Wrap later for queued/post-game work; do not expose fishnet directly to clients.",
        coreEditRequiredNow = false,
        RequirementClass.AdaptedToLichessFork
      ),
      LilaEngineSeamInspection(
        LilaEngineSeam.AnalyseReviewModule,
        "modules/analyse/src/main and app/controllers/Analyse.scala",
        reusableForEvenChess = true,
        "Use existing review/analysis surfaces later; keep live fairness policy in EvenChess.",
        coreEditRequiredNow = false,
        RequirementClass.LichessProvided
      ),
      LilaEngineSeamInspection(
        LilaEngineSeam.EvalCache,
        "modules/evalCache/src/main",
        reusableForEvenChess = true,
        "Use only through bounded cache keys and stale checks; never as raw client Stockfish.",
        coreEditRequiredNow = false,
        RequirementClass.AdaptedToLichessFork
      ),
      LilaEngineSeamInspection(
        LilaEngineSeam.ExternalEngineRegistry,
        "modules/analyse/src/main/ExternalEngine.scala",
        reusableForEvenChess = false,
        "Do not rely on user-registered external engines for rated EvenChess live help.",
        coreEditRequiredNow = false,
        RequirementClass.SupersededByLichessFork
      ),
      LilaEngineSeamInspection(
        LilaEngineSeam.TablebaseExactProof,
        "no dedicated tablebase module attached during Phase G inspection",
        reusableForEvenChess = false,
        "Keep exact proof support behind a future adapter before L8+ proof display.",
        coreEditRequiredNow = false,
        RequirementClass.AdaptedToLichessFork
      )
    )

    def coreEditsRequiredNow: Boolean = all.exists(_.coreEditRequiredNow)

  enum GatewayCaller:
    case InternalServer
    case BrowserClient
    case DebugEndpoint

  final case class GatewayAccessContext(
      caller: GatewayCaller,
      liveRated: Boolean,
      serverAuthorized: Boolean,
      policyVersion: String
  ):
    def validForLiveEngineHelp: Boolean =
      caller == GatewayCaller.InternalServer &&
        serverAuthorized &&
        policyVersion.nonEmpty

  final case class GatewayRuntimeState(
      cancelled: Boolean,
      timedOut: Boolean,
      stale: Boolean,
      engineHealthy: Boolean
  ):
    def status: EngineJobStatus =
      QueueOperations.statusFromRuntime(cancelled, timedOut, stale, engineHealthy)

  final case class EngineProviderResult(
      jobId: String,
      candidates: List[EngineCandidate],
      engineVersion: String,
      engineSource: String,
      engineBinaryHash: String,
      license: String,
      distributionNoticePresent: Boolean,
      rawEnginePayload: Option[String],
      hiddenDebugData: Option[String]
  ):
    def inventory: LicenseInventory =
      LicenseInventory(
        engineName = "Stockfish",
        version = engineVersion,
        sourceUrl = engineSource,
        license = license,
        binaryHash = engineBinaryHash,
        distributionNoticePresent = distributionNoticePresent
      )

  enum GatewayError:
    case AccessDenied
    case InvalidRequest
    case AboveSetLevel
    case EngineUnavailableForLevel
    case ProfileLimitExceeded
    case IncompleteEngineInventory
    case LicenseObligationMissing

  final case class GatewayEvaluation(
      packet: EngineTruthPacket,
      cacheHit: Boolean,
      aiSourceFacts: List[SourceFact]
  ):
    def safeForClientAndAi: Boolean =
      packet.safeForClient &&
        aiSourceFacts.nonEmpty &&
        aiSourceFacts.forall(_.boardStateKey == packet.boardStateKey) &&
        aiSourceFacts.forall(_.auditTag == packet.audit.jobId)

  object StockfishGatewayService:
    def evaluate(
        request: EngineJobRequest,
        access: GatewayAccessContext,
        runtime: GatewayRuntimeState,
        providerResult: EngineProviderResult,
        cached: Option[EngineTruthPacket] = None
    ): Either[GatewayError, GatewayEvaluation] =
      for _ <- validate(request, access, providerResult)
      yield
        cached.filter(QueueOperations.cacheHitAllowed(request, _)) match
          case Some(packet) =>
            GatewayEvaluation(packet, cacheHit = true, EngineTruthFacts.fromPacket(packet))
          case None =>
            val packet = runtime.status match
              case EngineJobStatus.Ready =>
                readyPacket(request, providerResult)
              case status =>
                statusPacket(request, providerResult, status, runtime)
            GatewayEvaluation(packet, cacheHit = false, EngineTruthFacts.fromPacket(packet))

    private def validate(
        request: EngineJobRequest,
        access: GatewayAccessContext,
        providerResult: EngineProviderResult
    ): Either[GatewayError, Unit] =
      if !access.validForLiveEngineHelp then Left(GatewayError.AccessDenied)
      else if !request.hasRequiredFields then Left(GatewayError.InvalidRequest)
      else if !request.allowedBySetLevel then Left(GatewayError.AboveSetLevel)
      else if !request.profile.liveEngineCandidateAllowed then Left(GatewayError.EngineUnavailableForLevel)
      else if !request.profileValidated then Left(GatewayError.ProfileLimitExceeded)
      else if !providerResult.inventory.complete then Left(GatewayError.IncompleteEngineInventory)
      else if !providerResult.inventory.gplObligationsPreserved then Left(GatewayError.LicenseObligationMissing)
      else Right(())

    private def readyPacket(
        request: EngineJobRequest,
        providerResult: EngineProviderResult
    ): EngineTruthPacket =
      val candidates = sanitizeCandidates(request.profile, providerResult.candidates)
      EngineTruthPacket(
        jobId = providerResult.jobId,
        gameId = request.gameId,
        playerId = request.playerId,
        boardStateKey = request.boardStateKey,
        ply = request.ply,
        requestedFeature = request.requestedFeature,
        candidates = candidates,
        numericEvalLabel = numericEvalLabel(request.profile, candidates),
        status = EngineJobStatus.Ready,
        stale = false,
        degraded = false,
        fallback = candidates.isEmpty,
        audit = audit(request, providerResult),
        rawEnginePayload = None,
        hiddenDebugData = None
      )

    private def statusPacket(
        request: EngineJobRequest,
        providerResult: EngineProviderResult,
        status: EngineJobStatus,
        runtime: GatewayRuntimeState
    ): EngineTruthPacket =
      EngineTruthPacket(
        jobId = providerResult.jobId,
        gameId = request.gameId,
        playerId = request.playerId,
        boardStateKey = request.boardStateKey,
        ply = request.ply,
        requestedFeature = request.requestedFeature,
        candidates = Nil,
        numericEvalLabel = None,
        status = status,
        stale = runtime.stale || status == EngineJobStatus.Stale,
        degraded = status == EngineJobStatus.Degraded,
        fallback = status == EngineJobStatus.TimedOut || status == EngineJobStatus.Degraded || status == EngineJobStatus.Fallback,
        audit = audit(request, providerResult),
        rawEnginePayload = None,
        hiddenDebugData = None
      )

    private def audit(
        request: EngineJobRequest,
        providerResult: EngineProviderResult
    ): EngineAuditMetadata =
      EngineAuditMetadata(
        jobId = providerResult.jobId,
        policyVersion = request.policyVersion,
        profile = request.profile,
        engineVersion = providerResult.engineVersion,
        engineSource = providerResult.engineSource,
        engineBinaryHash = providerResult.engineBinaryHash
      )

    private def sanitizeCandidates(
        profile: EngineProfile,
        candidates: List[EngineCandidate]
    ): List[EngineCandidate] =
      candidates
        .sortBy(_.rank)
        .take(profile.maxCandidates)
        .zipWithIndex
        .map { case (candidate, index) =>
          candidate.copy(
            rank = index + 1,
            scoreCp = Option.when(profile.allowNumericEvalOrWdl)(candidate.scoreCp).flatten,
            wdl = Option.when(profile.allowNumericEvalOrWdl)(candidate.wdl).flatten,
            linePlyCount = if profile.level.value >= 6 then candidate.linePlyCount else 0,
            proof = Option.when(profile.level.value >= 8)(candidate.proof.filter(_.length <= 80)).flatten
          )
        }

    private def numericEvalLabel(
        profile: EngineProfile,
        candidates: List[EngineCandidate]
    ): Option[String] =
      Option.when(
        profile.allowNumericEvalOrWdl &&
          candidates.exists(candidate => candidate.scoreCp.nonEmpty || candidate.wdl.nonEmpty)
      )("Approximate eval")

  object EngineTruthFacts:
    def fromPacket(packet: EngineTruthPacket): List[SourceFact] =
      val candidateFacts =
        packet.candidates.map: candidate =>
          SourceFact(
            factId = s"${packet.jobId}:candidate:${candidate.rank}",
            text = s"Engine candidate ${candidate.rank} is available within the approved L${packet.audit.profile.level.value} profile.",
            boardStateKey = packet.boardStateKey,
            exactnessClass = ExactnessClass.Approximate,
            auditTag = packet.audit.jobId
          )

      if candidateFacts.nonEmpty then candidateFacts
      else
        List(
          SourceFact(
            factId = s"${packet.jobId}:status",
            text = s"Engine status is ${packet.status.toString}.",
            boardStateKey = packet.boardStateKey,
            exactnessClass = ExactnessClass.Approximate,
            auditTag = packet.audit.jobId
          )
        )

  object PhaseGReleaseGuards:
    val browserStockfishNotLegalLiveHelp = true
    val externalUserEnginesNotRatedEvenChessHelp = true
    val engineGatewayDoesNotMutateEcr = true
    val engineGatewayDoesNotDecideCoachingPermission = true
    val futureCoreAdaptersRequirePatchMap = true

    def safePacketForLiveDelivery(packet: EngineTruthPacket): Boolean =
      packet.safeForClient &&
        packet.rawEnginePayload.isEmpty &&
        packet.hiddenDebugData.isEmpty &&
        packet.audit.profile.liveEngineCandidateAllowed

  final case class StockfishCandidateCapPolicy(
      l5: Int,
      l6: Int,
      l7Plus: Int,
      configuredOverrideApproved: Boolean
  ):
    def maxCandidatesFor(level: Level): Int =
      level.value match
        case value if value <= 4 => 0
        case 5                   => l5
        case 6                   => l6
        case _                   => l7Plus

    def valid: Boolean =
      l5 == 1 &&
        l6 == 2 &&
        l7Plus >= 3 &&
        l7Plus <= 4 &&
        (l7Plus == 3 || configuredOverrideApproved)

  object StockfishCandidateCapPolicy:
    val defaultV2: StockfishCandidateCapPolicy =
      StockfishCandidateCapPolicy(l5 = 1, l6 = 2, l7Plus = 3, configuredOverrideApproved = false)

    val currentConfigured: StockfishCandidateCapPolicy =
      StockfishCandidateCapPolicy(l5 = 1, l6 = 2, l7Plus = 4, configuredOverrideApproved = true)

  final case class StockfishProfilePolicy(
      profile: EngineProfile,
      candidateCapPolicy: StockfishCandidateCapPolicy,
      providerCalledByLichess: Boolean,
      rawStockfishOutputExposed: Boolean
  ):
    def candidateCountAllowed: Boolean =
      profile.maxCandidates <= candidateCapPolicy.maxCandidatesFor(profile.level)

    def numericEvalAllowedByLevel: Boolean =
      profile.allowNumericEvalOrWdl == (profile.level.value >= 8)

    def bounded: Boolean =
      profile.maxDepth >= 0 &&
        profile.maxNodes >= 0 &&
        profile.maxMovetimeMillis >= 0 &&
        profile.maxMultiPv >= 0 &&
        profile.maxThreads <= 1 &&
        profile.maxHashMb <= 32

    def valid: Boolean =
      candidateCapPolicy.valid &&
        candidateCountAllowed &&
        numericEvalAllowedByLevel &&
        bounded &&
        !providerCalledByLichess &&
        !rawStockfishOutputExposed
