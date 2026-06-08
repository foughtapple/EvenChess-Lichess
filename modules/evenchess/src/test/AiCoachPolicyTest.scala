package lila.evenchess

class AiCoachPolicyTest extends munit.FunSuite:

  import AiCoachPolicy.*
  import CoachingLadder.{ ExactnessClass, Level }
  import ProductInvariants.RequirementClass

  private val fact =
    SourceFact(
      factId = "fact-1",
      text = "White has a safer development idea.",
      boardStateKey = "board-1",
      exactnessClass = ExactnessClass.Heuristic,
      auditTag = "audit-1"
    )

  private val request =
    AiLiveRequest(
      requestId = "ai-1",
      gameId = "game-1",
      playerId = "player-1",
      boardStateKey = "board-1",
      ply = 12,
      setLevel = Level(4),
      requestedLevel = Level(4),
      policyVersion = "ai-policy-v1",
      promptVersion = "ai-prompt-v1",
      schemaVersion = "ai-live-json-v1",
      authorizedFacts = List(fact)
    )

  private val output =
    AiLiveOutput(
      policyVersion = "ai-policy-v1",
      schemaVersion = "ai-live-json-v1",
      exactnessClass = ExactnessClass.Heuristic,
      message = "Focus on improving the least active piece.",
      visualCues = List("soft-highlight"),
      sourceFactIds = List("fact-1"),
      auditTags = List("audit-1"),
      boardStateKey = "board-1"
    )

  test("Appendix M requirements are classified before implementation"):
    val byRequirement =
      AiRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(AiRequirement.ExplainSuppliedTruthOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AiRequirement.SchemaConstrainedLiveJson), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(AiRequirement.SummaryQuotaAndQuality), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(AiRequirement.PerformanceSummaryOnlineOnlyWindow), RequirementClass.AdaptedToLichessFork)

  test("Phase N AI requirements are classified before provider-boundary hardening"):
    val byRequirement =
      PhaseNAiRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseNAiRequirement.AiExplainsTruthOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNAiRequirement.AiCannotBypassLevelGates), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNAiRequirement.BoardStateAtMostOneAiCall), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNAiRequirement.FullGameAtMostOneNarrativeCall), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNAiRequirement.CredentialsServerSideOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseNAiRequirement.InvalidOutputFallsBackDeterministically), RequirementClass.EvenChessSpecific)

  test("AI live requests are grounded in authorized same-position source facts"):
    assert(request.hasRequiredFields)
    assert(!request.copy(authorizedFacts = Nil).hasRequiredFields)
    assert(!request.copy(requestedLevel = Level(5)).hasRequiredFields)
    assert(!request.copy(authorizedFacts = List(fact.copy(boardStateKey = "stale"))).hasRequiredFields)

  test("valid live output is schema-constrained and uses only supplied facts"):
    val validation = AiLiveOutputScanner.validate(request, output, allowedVisualCues = Set("soft-highlight"))

    assert(output.hasRequiredSchemaFields)
    assert(validation.valid)

  test("scanner rejects invented facts, missing audit tags, stale board state, and visual overreach"):
    val invalid = output.copy(
      sourceFactIds = List("made-up"),
      auditTags = List("missing"),
      visualCues = List("draw-arrow"),
      boardStateKey = "old-board"
    )
    val validation = AiLiveOutputScanner.validate(request, invalid, allowedVisualCues = Set("soft-highlight"))

    assert(validation.violations.contains(AiOutputViolation.InventedSourceFact))
    assert(validation.violations.contains(AiOutputViolation.MissingAuditTag))
    assert(validation.violations.contains(AiOutputViolation.StalePosition))
    assert(validation.violations.contains(AiOutputViolation.VisualOverreach))

  test("scanner rejects illegal notation, exact coordinates, direct commands, and best-move labels"):
    val invalid = output.copy(message = "The best move is e2e4. Play it now.")
    val validation = AiLiveOutputScanner.validate(request, invalid, allowedVisualCues = Set("soft-highlight"))

    assert(validation.violations.contains(AiOutputViolation.BestMoveLabel))
    assert(validation.violations.contains(AiOutputViolation.OverExactCoordinate))
    assert(validation.violations.contains(AiOutputViolation.DirectCommand))

  test("invalid AI output regenerates once then falls back or suppresses"):
    val invalid = AiValidationResult(List(AiOutputViolation.BestMoveLabel))

    assertEquals(AiFallbackPolicy.decide(AiValidationResult(Nil), priorRegenerations = 0), AiDeliveryDecision.Deliver)
    assertEquals(AiFallbackPolicy.decide(invalid, priorRegenerations = 0), AiDeliveryDecision.RegenerateOnce)
    assertEquals(AiFallbackPolicy.decide(invalid, priorRegenerations = 1), AiDeliveryDecision.FallbackSuppress)

  test("AI request audits include model, versions, tokens, cost, validation, fallback, and exactness"):
    val audit = AiRequestAudit(
      requestId = "ai-1",
      model = "cheap-configurable-model",
      promptVersion = "ai-prompt-v1",
      schemaVersion = "ai-live-json-v1",
      inputTokens = 120,
      outputTokens = 40,
      costMicros = 25,
      validation = AiValidationResult(Nil),
      fallbackUsed = false,
      deliveredExactness = ExactnessClass.Heuristic,
      createdAt = 123456789L
    )

    assert(audit.complete)
    assert(!audit.copy(model = "").complete)

  test("provider access is server-side and prompts are packet-grounded"):
    val access = AiProviderAccess(
      providerKey = "default-ai",
      configuredAtRuntime = true,
      credentialsServerSideOnly = true,
      clientCanExposeCredentials = false,
      clientCanChooseProvider = false,
      cheapDefaultModelAllowed = true,
      promptsSayExplainSuppliedPacketsOnly = true
    )

    assert(access.valid)
    assert(!access.copy(clientCanChooseProvider = true).valid)
    assert(!access.copy(promptsSayExplainSuppliedPacketsOnly = false).valid)

  test("Phase N AI call budgets allow at most one provider call and deterministic fallback"):
    val access = AiProviderAccess(
      providerKey = "default-ai",
      configuredAtRuntime = true,
      credentialsServerSideOnly = true,
      clientCanExposeCredentials = false,
      clientCanChooseProvider = false,
      cheapDefaultModelAllowed = true,
      promptsSayExplainSuppliedPacketsOnly = true
    )
    val validBudget = AiCallBudget.boardState(enabled = true, callsAttempted = 1)
    val tooManyBoardCalls = AiCallBudget.boardState(enabled = true, callsAttempted = 2)
    val disabledButAttempted = AiCallBudget.proposedMove(enabled = false, callsAttempted = 1)
    val fullGameBudget = AiCallBudget.fullGameReview(enabled = true, callsAttempted = 1)
    val invalidValidation = AiValidationResult(List(AiOutputViolation.BestMoveLabel))
    val fallback = AiDeterministicFallback.fromFacts(request)
    val envelope =
      AiProviderSafetyEnvelope(
        access = access,
        budget = validBudget,
        validation = invalidValidation,
        fallback = fallback,
        request = request
      )

    assert(validBudget.valid)
    assert(!tooManyBoardCalls.valid)
    assert(!disabledButAttempted.valid)
    assert(fullGameBudget.valid)
    assertEquals(fullGameBudget.maxCalls, 1)
    assert(fallback.validFor(request))
    assert(envelope.validForRatedUse)
    assert(!envelope.copy(access = access.copy(clientCanExposeCredentials = true)).validForRatedUse)
    assert(!envelope.copy(budget = tooManyBoardCalls).validForRatedUse)

  test("paid plans cannot receive stronger live help or deeper live engine truth through AI"):
    assert(!LivePlanFairness.paidPlansMayReceiveStrongerLiveHelp)
    assert(!LivePlanFairness.paidPlansMayReceiveDeeperLiveEngineTruth)
    assert(LivePlanFairness.sameLiveStrength(PlanTier.Free, PlanTier.Premium))

  test("post-game summaries do not mutate live fairness state or normal ECR"):
    val matchPolicy = PostGameSummaryPolicy.default(SummaryType.Match)
    val performancePolicy = PostGameSummaryPolicy.default(SummaryType.Performance)

    assert(matchPolicy.valid)
    assert(performancePolicy.valid)
    assert(!matchPolicy.mutatesLiveRatedFairnessState)
    assert(!performancePolicy.mutatesNormalEcr)

  test("summary quotas and quality match Appendix M"):
    assertEquals(SummaryQuotas.matchSummary.freeOnboardingTokens, 3)
    assertEquals(SummaryQuotas.matchSummary.premiumDailyLimit, 10)
    assertEquals(SummaryQuotas.performanceSummary.freeOnboardingTokens, 1)
    assertEquals(SummaryQuotas.performanceSummary.premiumDailyLimit, 1)
    assert(!SummaryQuotas.freeUnlocked(SummaryType.Performance, completedGames = 9))
    assert(SummaryQuotas.freeUnlocked(SummaryType.Performance, completedGames = 10))
    assert(SummaryQualityPolicy.freeAndPaidUseSameProductQualityPipeline)
    assertEquals(SummaryQualityPolicy.pipelineKey(PlanTier.Free), SummaryQualityPolicy.pipelineKey(PlanTier.Premium))
    assert(SummaryQualityPolicy.promisesReviewQuality)
    assert(!SummaryQualityPolicy.promisesNamedFrontierModel)

  test("failed generation and cached views do not consume token quota"):
    assert(SummaryConsumption.consumesToken(SummaryGenerationState.Generated))
    assert(!SummaryConsumption.consumesToken(SummaryGenerationState.Failed))
    assert(!SummaryConsumption.consumesToken(SummaryGenerationState.CachedView))

  test("performance summaries use recent completed online games only"):
    val eligibleOld = SummaryGameWindowItem("online-old", completed = true, online = true, botGame = false, computerGame = false, studyGame = false, completedAt = 1)
    val eligibleNew = eligibleOld.copy(gameId = "online-new", completedAt = 2)
    val bot = eligibleOld.copy(gameId = "bot", botGame = true, completedAt = 3)
    val computer = eligibleOld.copy(gameId = "computer", computerGame = true, completedAt = 4)
    val study = eligibleOld.copy(gameId = "study", studyGame = true, completedAt = 5)
    val offline = eligibleOld.copy(gameId = "offline", online = false, completedAt = 6)
    val incomplete = eligibleOld.copy(gameId = "incomplete", completed = false, completedAt = 7)

    val selected = PerformanceSummaryWindow.eligibleRecentGames(
      List(eligibleOld, eligibleNew, bot, computer, study, offline, incomplete),
      maxGames = 50
    )

    assertEquals(selected.map(_.gameId), List("online-new", "online-old"))
    assertEquals(PerformanceSummaryWindow.launchDefaultMaxCompletedOnlineGames, 50)
