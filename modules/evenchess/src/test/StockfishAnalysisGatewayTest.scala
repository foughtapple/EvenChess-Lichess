package lila.evenchess

class StockfishAnalysisGatewayTest extends munit.FunSuite:

  import CoachingLadder.Level
  import CoachingOverlays.Perspective
  import EngineGateway.*
  import EvenChessMode.TimeControlBucket
  import ProductInvariants.RequirementClass
  import StockfishAnalysisGateway.*

  private def limits(multiPv: Int, level: Int) =
    EngineLimitRequest(
      depth = 8 + level,
      nodes = 50_000 * 3 * level,
      movetimeMillis = 150 * 3 * level,
      multiPv = multiPv,
      threads = 1,
      hashMb = 32
    )

  private def request(level: Int = 6, multiPv: Int = 2) =
    EngineJobRequest(
      requestId = s"engine-request-l$level",
      gameId = "game-engine",
      playerId = "white-user",
      boardStateKey = "board-engine-22",
      ply = 22,
      perspective = Perspective.White,
      requestedFeature = "candidate_cards",
      setLevel = Level(level),
      requestedLevel = Level(level),
      timeControl = TimeControlBucket.Rapid,
      queue = EngineQueue.Live,
      limits = limits(multiPv, level),
      policyVersion = "engine-policy-v1",
      cancellationToken = "cancel-engine-1",
      cacheKey = s"game-engine:white-user:board-engine-22:l$level"
    )

  private val access =
    GatewayAccessContext(
      caller = GatewayCaller.InternalServer,
      liveRated = true,
      serverAuthorized = true,
      policyVersion = "engine-policy-v1"
    )

  private val runtime =
    GatewayRuntimeState(
      cancelled = false,
      timedOut = false,
      stale = false,
      engineHealthy = true
    )

  private val provider =
    EngineProviderResult(
      jobId = "engine-job-1",
      candidates = List(
        EngineCandidate("d2d4", rank = 2, scoreCp = Some(25), wdl = Some("51/47/2"), linePlyCount = 6, proof = Some("short proof")),
        EngineCandidate("e2e4", rank = 1, scoreCp = Some(34), wdl = Some("55/44/1"), linePlyCount = 8, proof = Some("short proof")),
        EngineCandidate("c2c4", rank = 3, scoreCp = Some(12), wdl = Some("50/48/2"), linePlyCount = 5, proof = Some("short proof"))
      ),
      engineVersion = "Stockfish 17",
      engineSource = "https://stockfishchess.org",
      engineBinaryHash = "sha256:phase-g",
      license = "GPL-3.0",
      distributionNoticePresent = true,
      rawEnginePayload = Some("info depth 20 multipv 1 pv e2e4 e7e5"),
      hiddenDebugData = Some("debug search tree")
    )

  private def value[A](result: Either[GatewayError, A]): A =
    result match
      case Right(value) => value
      case Left(error)  => fail(s"Expected Right, got $error")

  test("Phase G requirements are classified before Stockfish gateway work"):
    val byRequirement =
      PhaseGRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseGRequirement.LichessAnalysisInfrastructureProvided), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseGRequirement.InternalStockfishGateway), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseGRequirement.NoBrowserStockfishForRatedHelp), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseGRequirement.EngineInventoryAndLicense), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseGRequirement.CoreAdapterDeferredToThinSeams), RequirementClass.AdaptedToLichessFork)

  test("Phase N Stockfish requirements are classified before provider-boundary hardening"):
    val byRequirement =
      PhaseNStockfishRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseNStockfishRequirement.EceSideOnlyProviderCalls), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNStockfishRequirement.NoRawStockfishToClients), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNStockfishRequirement.CandidateCountsLevelGated), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNStockfishRequirement.NumericEvalStartsAtL8), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNStockfishRequirement.ProfilesBounded), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNStockfishRequirement.UnavailableFallsBackSafely), RequirementClass.EvenChessSpecific)

  test("Phase N Stockfish policy keeps provider execution ECE-side and level-gated"):
    val defaultCaps = StockfishCandidateCapPolicy.defaultV2
    val configuredCaps = StockfishCandidateCapPolicy.currentConfigured
    val l7Profile = EngineProfiles.forLevel(Level(7), TimeControlBucket.Rapid)
    val l10Profile = EngineProfiles.forLevel(Level(10), TimeControlBucket.Rapid)
    val l8Profile = EngineProfiles.forLevel(Level(8), TimeControlBucket.Rapid)
    val l7Policy =
      StockfishProfilePolicy(
        profile = l7Profile,
        candidateCapPolicy = defaultCaps,
        providerCalledByLichess = false,
        rawStockfishOutputExposed = false
      )
    val l10ConfiguredPolicy = l7Policy.copy(profile = l10Profile, candidateCapPolicy = configuredCaps)
    val l10DefaultPolicy = l7Policy.copy(profile = l10Profile, candidateCapPolicy = defaultCaps)

    assert(defaultCaps.valid)
    assert(configuredCaps.valid)
    assertEquals(defaultCaps.maxCandidatesFor(Level(5)), 1)
    assertEquals(defaultCaps.maxCandidatesFor(Level(6)), 2)
    assertEquals(defaultCaps.maxCandidatesFor(Level(7)), 3)
    assert(l7Policy.valid)
    assert(l10ConfiguredPolicy.valid)
    assert(!l10DefaultPolicy.valid)
    assert(!l7Policy.copy(providerCalledByLichess = true).valid)
    assert(!l7Policy.copy(rawStockfishOutputExposed = true).valid)
    assert(StockfishProfilePolicy(l8Profile, defaultCaps, providerCalledByLichess = false, rawStockfishOutputExposed = false).numericEvalAllowedByLevel)

  test("Phase G records inspected lila fishnet, analysis, eval cache, external engine, and tablebase seams"):
    val bySeam = LilaEngineSeamInventory.all.map(item => item.seam -> item).toMap

    assert(bySeam(LilaEngineSeam.FishnetAnalysisQueue).inspectedPath.contains("modules/fishnet"))
    assert(bySeam(LilaEngineSeam.AnalyseReviewModule).inspectedPath.contains("modules/analyse"))
    assert(bySeam(LilaEngineSeam.EvalCache).inspectedPath.contains("modules/evalCache"))
    assert(!bySeam(LilaEngineSeam.ExternalEngineRegistry).reusableForEvenChess)
    assert(!LilaEngineSeamInventory.coreEditsRequiredNow)

  test("gateway rejects browser, debug, unauthorised, and below-engine-level access"):
    assertEquals(
      StockfishGatewayService
        .evaluate(request(), access.copy(caller = GatewayCaller.BrowserClient), runtime, provider)
        .left
        .toOption,
      Some(GatewayError.AccessDenied)
    )
    assertEquals(
      StockfishGatewayService
        .evaluate(request(), access.copy(caller = GatewayCaller.DebugEndpoint), runtime, provider)
        .left
        .toOption,
      Some(GatewayError.AccessDenied)
    )
    assertEquals(
      StockfishGatewayService
        .evaluate(request(), access.copy(serverAuthorized = false), runtime, provider)
        .left
        .toOption,
      Some(GatewayError.AccessDenied)
    )
    assertEquals(
      StockfishGatewayService.evaluate(request(level = 4, multiPv = 0), access, runtime, provider).left.toOption,
      Some(GatewayError.EngineUnavailableForLevel)
    )

  test("gateway validates Set Level and requested Stockfish limits"):
    assertEquals(
      StockfishGatewayService
        .evaluate(request(level = 6).copy(setLevel = Level(5)), access, runtime, provider)
        .left
        .toOption,
      Some(GatewayError.AboveSetLevel)
    )
    assertEquals(
      StockfishGatewayService
        .evaluate(request(level = 6, multiPv = 3), access, runtime, provider)
        .left
        .toOption,
      Some(GatewayError.ProfileLimitExceeded)
    )
    assert(value(StockfishGatewayService.evaluate(request(level = 6, multiPv = 2), access, runtime, provider)).packet.safeForClient)

  test("provider output is bounded, rank-normalised, and stripped of raw engine/debug data"):
    val result = value(StockfishGatewayService.evaluate(request(level = 6, multiPv = 2), access, runtime, provider))
    val packet = result.packet

    assert(!result.cacheHit)
    assert(packet.safeForClient)
    assertEquals(packet.candidates.size, 2)
    assertEquals(packet.candidates.map(_.rank), List(1, 2))
    assertEquals(packet.candidates.map(_.uci), List("e2e4", "d2d4"))
    assert(packet.candidates.forall(_.scoreCp.isEmpty))
    assert(packet.candidates.forall(_.wdl.isEmpty))
    assert(packet.candidates.forall(_.proof.isEmpty))
    assert(packet.rawEnginePayload.isEmpty)
    assert(packet.hiddenDebugData.isEmpty)
    assert(result.safeForClientAndAi)

  test("L8 numeric eval and WDL are preserved only with approximate label"):
    val l8Result = value(StockfishGatewayService.evaluate(request(level = 8, multiPv = 3), access, runtime, provider))
    val packet = l8Result.packet

    assert(packet.safeForClient)
    assertEquals(packet.candidates.size, 3)
    assert(packet.candidates.exists(_.scoreCp.nonEmpty))
    assert(packet.candidates.exists(_.wdl.nonEmpty))
    assertEquals(packet.numericEvalLabel, Some("Approximate eval"))
    assert(packet.audit.profile.approximateEvalLabelRequired)

  test("cache hits require same safe board context and avoid provider recomputation result"):
    val first = value(StockfishGatewayService.evaluate(request(level = 6, multiPv = 2), access, runtime, provider))
    val cacheResult = value(
      StockfishGatewayService.evaluate(
        request(level = 6, multiPv = 2),
        access,
        runtime,
        provider.copy(jobId = "engine-job-2", candidates = Nil),
        cached = Some(first.packet)
      )
    )

    assert(cacheResult.cacheHit)
    assertEquals(cacheResult.packet.jobId, first.packet.jobId)
    assertEquals(cacheResult.packet.candidates.map(_.uci), first.packet.candidates.map(_.uci))
    assert(cacheResult.packet.safeForClient)
  test("cancelled, timed-out, stale, and degraded runtime states return safe non-candidate packets"):
    val cancelled = value(
      StockfishGatewayService.evaluate(
        request(),
        access,
        runtime.copy(cancelled = true),
        provider
      )
    ).packet
    val timedOut = value(
      StockfishGatewayService.evaluate(
        request(),
        access,
        runtime.copy(timedOut = true),
        provider
      )
    ).packet
    val stale = value(
      StockfishGatewayService.evaluate(
        request(),
        access,
        runtime.copy(stale = true),
        provider
      )
    ).packet
    val degraded = value(
      StockfishGatewayService.evaluate(
        request(),
        access,
        runtime.copy(engineHealthy = false),
        provider
      )
    ).packet

    assertEquals(cancelled.status, EngineJobStatus.Cancelled)
    assertEquals(timedOut.status, EngineJobStatus.TimedOut)
    assertEquals(stale.status, EngineJobStatus.Stale)
    assertEquals(degraded.status, EngineJobStatus.Degraded)
    assert(stale.stale)
    assert(timedOut.fallback)
    assert(degraded.degraded)
    assert(List(cancelled, timedOut, stale, degraded).forall(_.candidates.isEmpty))
    assert(List(cancelled, timedOut, stale, degraded).forall(_.safeForClient))

  test("engine inventory and GPL obligations are enforced before jobs run"):
    assertEquals(
      StockfishGatewayService
        .evaluate(request(), access, runtime, provider.copy(engineBinaryHash = ""))
        .left
        .toOption,
      Some(GatewayError.IncompleteEngineInventory)
    )
    assertEquals(
      StockfishGatewayService
        .evaluate(request(), access, runtime, provider.copy(distributionNoticePresent = false))
        .left
        .toOption,
      Some(GatewayError.LicenseObligationMissing)
    )

  test("AI receives same-position truth facts only and cannot deepen engine help"):
    val result = value(StockfishGatewayService.evaluate(request(level = 6, multiPv = 2), access, runtime, provider))

    assert(result.safeForClientAndAi)
    assert(result.aiSourceFacts.nonEmpty)
    assert(result.aiSourceFacts.forall(_.valid))
    assert(result.aiSourceFacts.forall(_.boardStateKey == result.packet.boardStateKey))
    assert(result.aiSourceFacts.forall(_.auditTag == result.packet.audit.jobId))
    assert(PhaseGReleaseGuards.browserStockfishNotLegalLiveHelp)
    assert(PhaseGReleaseGuards.externalUserEnginesNotRatedEvenChessHelp)
    assert(PhaseGReleaseGuards.engineGatewayDoesNotMutateEcr)
    assert(PhaseGReleaseGuards.engineGatewayDoesNotDecideCoachingPermission)
    assert(PhaseGReleaseGuards.safePacketForLiveDelivery(result.packet))
