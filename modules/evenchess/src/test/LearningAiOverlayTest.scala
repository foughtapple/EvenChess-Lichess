package lila.evenchess

class LearningAiOverlayTest extends munit.FunSuite:

  import AiCoachPolicy.SourceFact
  import CoachingLadder.ExactnessClass
  import LearningAiOverlay.*
  import ProductInvariants.RequirementClass

  private val context = LearningContext(
    surface = LearningSurface.Analysis,
    contextId = "analysis-game-1",
    boardStateKey = "board-analysis-18",
    ply = 18,
    source = "lichess-analysis",
    ratedLive = false,
    serverAuthorized = true,
    policyVersion = "learning-policy-g",
    schemaVersion = "learning-schema-g",
    auditId = "audit-learning-18"
  )

  private val sourceFacts = List(
    SourceFact(
      factId = "fact-position",
      text = "The approved truth packet says White has a loose back-rank piece.",
      boardStateKey = context.boardStateKey,
      exactnessClass = ExactnessClass.Heuristic,
      auditTag = "audit-position"
    ),
    SourceFact(
      factId = "fact-opening",
      text = "The opening explorer context says the common plan is central pressure.",
      boardStateKey = context.boardStateKey,
      exactnessClass = ExactnessClass.Approximate,
      auditTag = "audit-opening"
    )
  )

  private val positionCard = LearningCard(
    id = "card-position",
    kind = LearningCardKind.PositionExplanation,
    title = "Position explanation",
    body = "The important theme is the loose back-rank piece.",
    bullets = List("Improve coordination before starting a forcing line."),
    sourceFactIds = List("fact-position"),
    auditId = context.auditId,
    serverAuthorized = true,
    approvedDisplayPayload = true,
    rawEnginePayload = None,
    hiddenDebugData = None,
    providerSecret = None,
    rawPrompt = None,
    modelLabel = Some("server-selected")
  )

  private val openingCard = positionCard.copy(
    id = "card-opening",
    kind = LearningCardKind.OpeningPlan,
    title = "Opening plan",
    body = "Play around the central pressure theme already present in the opening data.",
    sourceFactIds = List("fact-opening")
  )

  test("Version 1.2 Phase G learning-surface requirements are classified before integration"):
    val byRequirement =
      PhaseGLearningRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(
      byRequirement(PhaseGLearningRequirement.LichessStudyAnalysisOpeningProvided),
      RequirementClass.LichessProvided
    )
    assertEquals(
      byRequirement(PhaseGLearningRequirement.ServerAuthorizedLearningContext),
      RequirementClass.EvenChessSpecific
    )
    assertEquals(
      byRequirement(PhaseGLearningRequirement.OptionalAnalyseDataField),
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PhaseGLearningRequirement.OptionalOpeningDataField),
      RequirementClass.AdaptedToLichessFork
    )

  test("adapter contract is display-only and uses optional namespaced payload keys"):
    assertEquals(AdapterContract.analyseDataKey, "evenchess")
    assertEquals(AdapterContract.learningPayloadKey, "learning")
    assertEquals(AdapterContract.openingPayloadKey, "openingAi")
    assert(!AdapterContract.clientMayAuthorize)
    assert(!AdapterContract.clientMayReadProviderSecrets)
    assert(!AdapterContract.clientMayReadRawEngineData)
    assert(!AdapterContract.mutatesLiveRatedFairnessState)

  test("server-authorized learning payload renders safe study analysis and opening cards"):
    val payload = LearningPayload(
      enabled = true,
      context = context,
      sourceFacts = sourceFacts,
      cards = List(positionCard, openingCard, positionCard.copy(id = "card-mistake", kind = LearningCardKind.MistakeTheme))
    )

    assert(payload.valid)
    assert(payload.safeForClient)
    assertEquals(
      payload.renderableCards.map(_.kind),
      List(LearningCardKind.PositionExplanation, LearningCardKind.OpeningPlan, LearningCardKind.MistakeTheme)
    )
    assert(!payload.mutatesLiveRatedFairnessState)

  test("live rated contexts are suppressed from learning overlays"):
    val payload = LearningPayload(
      enabled = true,
      context = context.copy(ratedLive = true),
      sourceFacts = sourceFacts,
      cards = List(positionCard)
    )

    assert(!payload.valid)
    assert(!payload.safeForClient)
    assertEquals(payload.renderableCards, Nil)

  test("raw engine debug provider secrets raw prompts and invented facts are blocked"):
    val rawEngine = positionCard.copy(rawEnginePayload = Some("pv e2e4 e7e5"))
    val debug = positionCard.copy(hiddenDebugData = Some("multipv debug"))
    val secret = positionCard.copy(providerSecret = Some("sk-secret"))
    val prompt = positionCard.copy(rawPrompt = Some("raw system prompt"))
    val invented = positionCard.copy(sourceFactIds = List("fact-invented"))

    List(rawEngine, debug, secret, prompt, invented).foreach { unsafeCard =>
      val payload = LearningPayload(
        enabled = true,
        context = context,
        sourceFacts = sourceFacts,
        cards = List(unsafeCard)
      )

      assert(!payload.valid)
      assert(!payload.safeForClient)
      assertEquals(payload.renderableCards, Nil)
    }
