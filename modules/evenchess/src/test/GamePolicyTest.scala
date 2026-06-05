package lila.evenchess

class GamePolicyTest extends munit.FunSuite:

  import CoachingPolicy.{ AssistanceLedger, GameCompletionRatingState, Stage1DummyAudit }
  import EvenChessMode.{ ClientDisplayClaim, SetLevel, TimeControlBucket }
  import GamePolicy.*
  import ProductInvariants.RequirementClass

  private def player(id: String, level: Int, poolKey: String = "rapid-normal-evenchess") =
    PlayerPolicy(id, SetLevel(level), poolKey)

  private def request(
      gameId: String = "game-policy-1",
      rated: Boolean = true,
      mode: GamePolicyMode = GamePolicyMode.NormalRatedEvenChess
  ) =
    GamePolicyCreateRequest(
      gameId = gameId,
      mode = mode,
      rated = rated,
      timeControlBucket = TimeControlBucket.Rapid,
      white = player("white-user", 4),
      black = player("black-user", 6),
      versions = PolicyVersions.current,
      featureFlags = Map("evenchess_mode" -> "enabled", "server_policy" -> "phase-c"),
      createdAt = 1000L
    )

  private def value[A](result: Either[PersistenceError, A]): A =
    result match
      case Right(value) => value
      case Left(error)  => fail(s"Expected Right, got $error")

  test("Phase C requirements are classified before policy persistence work"):
    val byRequirement =
      PhaseCRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseCRequirement.ServerOwnedModeMetadata), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseCRequirement.DedicatedPolicyPersistence), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseCRequirement.ClientClaimsDisplayOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseCRequirement.NormalChessEffectsSuppressed), RequirementClass.LichessProvided)
    assertEquals(
      byRequirement(PhaseCRequirement.LilaCoreIntegrationDeferredToThinSeams),
      RequirementClass.AdaptedToLichessFork
    )

  test("game policy record produces server-owned EvenChess metadata and data-model projection"):
    val repo = InMemoryGamePolicyRepository()
    val record = value(PolicyService.create(request(), now = 1100L, repo))

    assert(record.valid)
    assert(record.serverMetadata.isValid)
    assertEquals(record.serverMetadata.playerModeKey, "normal_rated_evenchess")
    assertEquals(record.serverMetadata.playerSetLevels.white.value, 4)
    assertEquals(record.serverMetadata.playerSetLevels.black.value, 6)

    val decision = record.authorityDecision(ClientDisplayClaim(claimsEvenChess = false))
    assert(decision.isEvenChess)
    assert(!decision.clientFlagAcceptedAsAuthority)
    assert(decision.displayAsAssisted)
    assert(decision.mayRenderEvenChessOverlays)
    assert(decision.mayUseEcrSystems)

    val dataModel = record.asDataModel
    assert(dataModel.valid)
    assertEquals(dataModel.gameId, "game-policy-1")
    assertEquals(dataModel.mode, "normal_rated_evenchess")
    assertEquals(dataModel.setLevelsByPlayer, Map("white-user" -> 4, "black-user" -> 6))
    assert(dataModel.versions.hasSchema)
    assert(dataModel.versions.hasFairnessVersion)

  test("normal Lichess games suppress EvenChess effects even if the client claims assistance"):
    val clientClaim = ClientDisplayClaim(claimsEvenChess = true)
    val decision = NormalChessPolicy.authorityDecision(clientClaim)

    assert(clientClaim.claimsEvenChess)
    assert(!decision.isEvenChess)
    assert(!decision.clientFlagAcceptedAsAuthority)
    assert(NormalChessPolicy.evenChessEffectsSuppressed(clientClaim))

  test("repository persists policy records and append-only audit event references"):
    val repo = InMemoryGamePolicyRepository()
    val record = value(GamePolicyRecord.fromRequest(request(), now = 1001L))

    assertEquals(repo.get(record.gameId), None)
    value(repo.put(record))
    assertEquals(repo.requireEvenChess(record.gameId), Right(record))

    val stored1 = value(repo.appendAuditEventRef(record.gameId, "audit-1", updatedAt = 1010L))
    val stored2 = value(repo.appendAuditEventRef(record.gameId, "audit-2", updatedAt = 1020L))

    assertEquals(stored1.auditEventIds, Vector("audit-1"))
    assertEquals(stored2.auditEventIds, Vector("audit-1", "audit-2"))
    assert(stored2.valid)
    assertEquals(repo.appendAuditEventRef("missing-game", "audit-3", 1030L), Left(PersistenceError.MissingGamePolicy))

  test("rated EvenChess completion requires computable assistance summary unless no-rated or annulled"):
    val repo = InMemoryGamePolicyRepository()
    val record = value(PolicyService.create(request(), now = 1001L, repo))

    assertEquals(
      repo.complete(record.gameId, AssistanceLedger.empty, GameCompletionRatingState.Rate, completedAt = 2000L),
      Left(PersistenceError.CompletionBlockedByMissingAssistanceSummary)
    )
    assert(value(repo.complete(record.gameId, AssistanceLedger.empty, GameCompletionRatingState.NoRate, 2000L)).completedAt.isDefined)

    val ratedRepo = InMemoryGamePolicyRepository()
    val ratedRecord = value(PolicyService.create(request(gameId = "rated-with-ledger"), now = 1001L, ratedRepo))
    val event = Stage1DummyAudit.event(ratedRecord.gameId, "white-user", createdAt = 1500L).copy(rated = true)
    val ledger = AssistanceLedger.empty.append(event)

    val completed = value(ratedRepo.complete(ratedRecord.gameId, ledger, GameCompletionRatingState.Rate, 2000L))
    assertEquals(completed.completedAt, Some(2000L))

    val casualRepo = InMemoryGamePolicyRepository()
    val casualRecord = value(PolicyService.create(request(gameId = "casual-policy", rated = false), now = 1001L, casualRepo))
    val casualCompleted =
      value(casualRepo.complete(casualRecord.gameId, AssistanceLedger.empty, GameCompletionRatingState.Rate, 2000L))
    assertEquals(casualCompleted.completedAt, Some(2000L))

  test("policy service exposes server-owned Set Level and rejects unknown players"):
    val repo = InMemoryGamePolicyRepository()
    value(PolicyService.create(request(), now = 1001L, repo))

    assertEquals(PolicyService.setLevelFor("game-policy-1", "white-user", repo).map(_.value), Right(4))
    assertEquals(PolicyService.setLevelFor("game-policy-1", "WHITE-USER", repo).map(_.value), Right(4))
    assertEquals(PolicyService.setLevelFor("game-policy-1", "black-user", repo).map(_.value), Right(6))
    assertEquals(
      PolicyService.setLevelFor("game-policy-1", "observer", repo),
      Left(PersistenceError.MissingPlayerPolicy)
    )
    assertEquals(
      PolicyService.setLevelFor("missing-game", "white-user", repo),
      Left(PersistenceError.MissingGamePolicy)
    )

  test("persistence plan uses dedicated EvenChess collections and avoids broad core fields"):
    assert(PersistencePlan.valid)
    assertEquals(PersistencePlan.policyCollection, "evenchess_game_policy")
    assertEquals(PersistencePlan.assistanceLedgerCollection, "evenchess_assistance_ledger")
    assert(!PersistencePlan.broadCoreGameFieldsRequired)
    assert(PersistencePlan.inspectLilaStorageConventionsBeforeMongoBinding)
    assert(PersistencePlan.policyIndexes.exists(_.contains("gameId")))
    assert(PersistencePlan.ledgerIndexes.exists(_.contains("eventId")))

  test("invalid policy records cannot be persisted"):
    val repo = InMemoryGamePolicyRepository()
    val invalidRequest = request().copy(white = player("same-user", 4), black = player("same-user", 5))

    assertEquals(GamePolicyRecord.fromRequest(invalidRequest, now = 1001L), Left(PersistenceError.InvalidRecord))

    val invalidRecord = GamePolicyRecord(
      gameId = "",
      mode = GamePolicyMode.NormalRatedEvenChess,
      rated = true,
      timeControlBucket = TimeControlBucket.Rapid,
      white = player("white-user", 4),
      black = player("black-user", 5),
      versions = PolicyVersions.current,
      featureFlags = Map.empty,
      createdAt = 1000L,
      updatedAt = 1001L
    )

    assertEquals(repo.put(invalidRecord), Left(PersistenceError.InvalidRecord))
