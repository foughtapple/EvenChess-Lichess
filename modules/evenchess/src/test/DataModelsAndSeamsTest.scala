package lila.evenchess

class DataModelsAndSeamsTest extends munit.FunSuite:

  import DataModelsAndSeams.*
  import ProductInvariants.RequirementClass

  private val versions =
    VersionStamp(
      schemaVersion = "schema-v1",
      policyVersion = Some("policy-v1"),
      modelVersion = Some("model-v1"),
      configVersion = Some("config-v1")
    )

  test("Appendix U requirements are classified before implementation"):
    val byRequirement =
      DataRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(DataRequirement.FairnessModelsVersioned), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(DataRequirement.RatingOffsetPolicyVersions), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(DataRequirement.AiEngineVersionedAudit), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(DataRequirement.InspectLilaStorageConventions), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(DataRequirement.LilaIntegrationSeams), RequirementClass.AdaptedToLichessFork)

  test("game policy records mode, players, Set Levels, feature flags, and fairness versions"):
    val policy = EvenChessGamePolicy(
      gameId = "game-1",
      mode = "normal_evenchess",
      rated = true,
      timeControl = "rapid",
      playerIds = Set("p1", "p2"),
      setLevelsByPlayer = Map("p1" -> 5, "p2" -> 6),
      versions = versions,
      featureFlags = Map("dummy_overlay" -> "flag-v1")
    )

    assert(policy.valid)
    assert(!policy.copy(setLevelsByPlayer = Map("p1" -> 11, "p2" -> 6)).valid)
    assert(!policy.copy(versions = VersionStamp("schema-v1", None, None, None)).valid)
    assert(policy.copy(rated = false, versions = VersionStamp("schema-v1", None, None, None)).valid)

  test("player rating and assistance summary preserve model and policy versions for replay"):
    val rating = EvenChessPlayerRating(
      playerId = "p1",
      poolKey = "rapid-normal-evenchess",
      rawEcr = 1500,
      ratingDeviation = 80,
      volatility = 0.06,
      gameCount = 12,
      provisional = false,
      createdAt = 1000,
      updatedAt = 1200,
      modelVersion = "ecr-model-v1",
      schemaVersion = "rating-schema-v1"
    )
    val summary = EvenChessGameAssistanceSummary(
      gameId = "game-1",
      playerId = "p1",
      usedLevel = 5,
      assistanceLoad = 12.5,
      usedOffset = 80,
      featureMix = Set("hint", "offset_count"),
      modelVersion = "assist-model-v1",
      policyVersion = "offset-policy-v1",
      schemaVersion = "summary-schema-v1"
    )

    assert(rating.valid)
    assert(!rating.copy(updatedAt = 900).valid)
    assert(summary.valid)
    assert(!summary.copy(usedLevel = 11).valid)
    assert(!summary.copy(featureMix = Set.empty).valid)

  test("coaching feature, render event, and offset payload keep server authority and display constraints"):
    val feature = CoachingFeatureModel(
      featureKey = "offset_count",
      minLevel = 3,
      maxLevel = 10,
      gatingPolicyVersion = "gate-v1",
      registryVersion = "registry-v1",
      schemaVersion = "feature-schema-v1"
    )
    val render = CoachingRenderEventModel(
      eventId = "render-1",
      gameId = "game-1",
      playerId = "p1",
      featureKey = "dummy_overlay",
      action = CoachingRenderAction.Rendered,
      visibility = "visible",
      ply = 12,
      policyVersion = "render-policy-v1",
      schemaVersion = "render-schema-v1",
      serverAuthoritative = true
    )
    val offset = OffsetCountPayloadModel(
      gameId = "game-1",
      playerId = "p1",
      boardHash = "board-1",
      resultState = "equal",
      displayColor = "blue",
      materialDelta = 0,
      resolverVersion = "resolver-v1",
      schemaVersion = "offset-schema-v1"
    )

    assert(feature.valid)
    assert(!feature.copy(minLevel = 8, maxLevel = 3).valid)
    assert(render.valid)
    assert(!render.copy(serverAuthoritative = false).valid)
    assert(offset.valid)
    assert(!offset.copy(resultState = "unknown").valid)
  test("engine and AI audit records keep versions and reject raw sensitive payload storage"):
    val engine = EngineAnalysisJobModel(
      jobId = "engine-1",
      gameId = "game-1",
      playerId = "p1",
      profileVersion = "sf-l5-v1",
      cacheKey = "cache-1",
      fallbackUsed = false,
      engineVersion = "Stockfish 17",
      engineBinaryHash = "sha256:abc",
      schemaVersion = "engine-schema-v1",
      storesRawEngineLines = false
    )
    val ai = AIWordingRequestModel(
      requestId = "ai-1",
      gameId = "game-1",
      playerId = "p1",
      promptTemplateVersion = "prompt-v1",
      responseSchemaVersion = "response-schema-v1",
      validationVersion = "validator-v1",
      modelVersion = "mock-v1",
      estimatedCostCents = 0,
      schemaVersion = "ai-schema-v1",
      storesRawPrompt = false
    )

    assert(engine.valid)
    assert(!engine.copy(storesRawEngineLines = true).valid)
    assert(ai.valid)
    assert(!ai.copy(storesRawPrompt = true).valid)

  test("token, summary quota, marketing attribution, and patch map records carry required identity"):
    val token = TokenLedgerEntryModel(
      entryId = "token-1",
      playerId = "p1",
      event = TokenLedgerEvent.Consumed,
      amount = 1,
      reason = "valid game started",
      gameId = Some("game-1"),
      schemaVersion = "token-schema-v1"
    )
    val summary = SummaryQuotaLedgerEntryModel(
      entryId = "summary-1",
      playerId = "p1",
      summaryType = "match",
      consumesToken = true,
      consumesQuota = false,
      gameId = "game-1",
      providerVersion = "mock-v1",
      schemaVersion = "summary-quota-schema-v1"
    )
    val attribution = MarketingAttributionModel(
      attributionId = "attr-1",
      pseudonymousAccountId = "acct-hash-1",
      utmSource = Some("search"),
      utmCampaign = Some("launch"),
      clickId = Some("click-1"),
      variant = "free_tokens",
      configVersion = "marketing-v1",
      firstTouch = Some("search"),
      latestTouch = Some("retarget"),
      subscriptionId = Some("sub-1"),
      storesRawEmail = false,
      schemaVersion = "attr-schema-v1"
    )
    val patch = PatchMapEntryModel(
      entryId = "PM-2026-001",
      fileTouched = "build.sbt",
      requirement = "ARCH-L1-010",
      risk = "Low",
      tests = "evenchess/test",
      isolationPlan = "Keep EvenChess code in modules/evenchess."
    )

    assert(token.valid)
    assert(!token.copy(gameId = None).valid)
    assert(summary.valid)
    assert(!summary.copy(consumesToken = false, consumesQuota = false).valid)
    assert(attribution.valid)
    assert(!attribution.copy(storesRawEmail = true).valid)
    assert(patch.valid)
    assert(!patch.copy(risk = "Severe").valid)

  test("integration seam registry covers all Appendix U seams with server authority for fairness effects"):
    assert(IntegrationSeamRegistry.coversAllSeams)

    val fairnessSeams = IntegrationSeamRegistry.all.filter(_.fairnessAffecting)
    assert(fairnessSeams.nonEmpty)
    assert(fairnessSeams.forall(_.serverAuthoritative))
    assert(IntegrationSeamRegistry.all.exists(_.seam == IntegrationSeam.GameCreation))
    assert(IntegrationSeamRegistry.all.exists(_.seam == IntegrationSeam.RatingUpdate))
    assert(IntegrationSeamRegistry.all.exists(_.seam == IntegrationSeam.AdminOps))

  test("storage policy prefers dedicated stores, replayable migrations, and privacy-reviewed sensitive data"):
    assert(StoragePolicy.inspectLilaStorageConventionsBeforeDbImplementation)
    assert(StoragePolicy.preferDedicatedEvenChessStores)
    assert(!StoragePolicy.broadCoreFieldsPreferred)
    assert(StoragePolicy.migrationsMustPreserveReplayability)
    assert(!StoragePolicy.rawAiPromptsAllowedByDefault)
    assert(!StoragePolicy.rawEngineLinesAllowedByDefault)
    assert(!StoragePolicy.rawEmailsAllowedByDefault)
    assert(!StoragePolicy.sensitiveAttributionAllowedWithoutPrivacyReview)

  test("Stage 1 data scope remains limited"):
    assert(Stage1DataScope.withinStage1Scope("EvenChessGamePolicy"))
    assert(Stage1DataScope.withinStage1Scope("CoachingRenderEvent"))
    assert(Stage1DataScope.withinStage1Scope("PatchMapEntry"))
    assert(Stage1DataScope.withinStage1Scope("AIWordingRequest"))
    assert(!Stage1DataScope.withinStage1Scope("EvenChessPlayerRating"))
