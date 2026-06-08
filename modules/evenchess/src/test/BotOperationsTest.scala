package lila.evenchess

class BotOperationsTest extends munit.FunSuite:

  import AdminBackendSettings.BotSimulationControls
  import BotOperations.*
  import PlaySearchIntegration.*

  private val now = 123456789L
  private val roster = (1 to 12).map(index => s"evenbot$index").mkString(",")

  private val controls =
    BotSimulationControls(
      enabled = true,
      scope = "both",
      botCount = 12,
      ratingMin = 900,
      ratingMax = 2100,
      levelMin = 0,
      levelMax = 10,
      persona = "mixed",
      timeControls = "bullet,blitz,rapid,classical",
      accountRoster = roster
    )

  test("simulation config normalizes admin settings into bounded bot population controls"):
    val config = BotSimulationConfig.fromSettings(controls)

    assert(config.valid)
    assertEquals(config.scope, BotModeScope.Both)
    assertEquals(config.botCount, 12)
    assertEquals(config.ratingMin, 900)
    assertEquals(config.ratingMax, 2100)
    assertEquals(config.levelMin.value, 0)
    assertEquals(config.levelMax.value, 10)
    assertEquals(config.persona, BotPersonaMode.Mixed)
    assertEquals(config.timeControlCsv, "bullet,blitz,rapid,classical")
    assert(config.accountBacked)
    assertEquals(config.effectiveBotCount, 12)

  test("bot account roster generates a default 1000-name local roster"):
    val generated = BotAccountRoster.generatedDefault

    assertEquals(generated.size, 1000)
    assertEquals(generated.headOption, Some("ecbot0001"))
    assertEquals(generated.lastOption, Some("ecbot1000"))
    assertEquals(BotAccountRoster.effectiveFromCsv("").size, 1000)

  test("bot account roster display ratings are established-looking and non-round"):
    val ratings = List("ecbot0001", "ecbot0904", "evenbot17").map(BotAccountRoster.establishedDisplayRating(_, Some(1500)))

    assert(ratings.forall(rating => rating >= 650 && rating <= 2450))
    assert(ratings.forall(_ != 1500))
    assert(ratings.forall(rating => rating % 10 != 0))

  test("simulation config falls back to generated roster when saved roster is blank"):
    val repo = new InMemoryPlaySearchRepository
    val config = BotSimulationConfig.fromSettings(controls.copy(accountRoster = "", botCount = 3))
    val runtime = BotSimulationRuntimeState.empty.copy(running = true, revision = 13L, startedAt = Some(now), lastAction = "simulation_started")

    val result = seedSimulation(repo, config, runtime, now)
    val active = activeSimulationTickets(repo)

    assert(result.valid)
    assertEquals(result.requestedTickets, 3)
    assertEquals(result.createdTickets, 3)
    assertEquals(active.map(_.ticket.playerId), List("ecbot0001", "ecbot0002", "ecbot0003"))

  test("simulation seeding writes regular bot search records into the existing repository seam"):
    val repo = new InMemoryPlaySearchRepository
    val config = BotSimulationConfig.fromSettings(controls)
    val runtime = BotSimulationRuntimeState.empty.copy(running = true, revision = 7L, startedAt = Some(now), lastAction = "simulation_started")

    val result = seedSimulation(repo, config, runtime, now)
    val active = activeSimulationTickets(repo)

    assert(result.valid)
    assertEquals(result.requestedTickets, 12)
    assertEquals(result.createdTickets, 12)
    assertEquals(active.size, 12)
    assert(active.forall(_.ticket.isBotTicket))
    assert(active.forall(isRosterBackedSimulationRecord))
    assert(active.exists(_.mode == PlayMode.RatedEvenChess))
    assert(active.exists(_.mode == PlayMode.CasualEvenChess))
    assert(active.forall(_.ticket.botProfile.exists(_.timeControl.isDefined)))
    assert(active.exists(_.ticket.poolKey.timeControl == EvenChessMode.TimeControlBucket.Blitz))
    assert(active.map(_.ticket.poolKey.timeControl).toSet.size > 1)
    assert(active.exists(record =>
      record.mode == PlayMode.RatedEvenChess && record.ticket.poolKey.timeControl == EvenChessMode.TimeControlBucket.Blitz
    ))
    assert(active.exists(record =>
      record.mode == PlayMode.CasualEvenChess && record.ticket.poolKey.timeControl == EvenChessMode.TimeControlBucket.Blitz
    ))
    assert(active.exists(record => record.ticket.ecr.rating >= 1450 && record.ticket.ecr.rating <= 1550))
    assert(active.forall(record => record.ticket.ecr.rating >= 900 && record.ticket.ecr.rating <= 2100))
    assert(active.forall(record => record.ticket.botProfile.exists(_.targetEcr == record.ticket.ecr.rating)))
    assert(active.forall(_.tokenSnapshot.subscriptionActive))
    assert(result.potentialBotVsBotContracts > 0)
    assert(
      active
        .groupBy(_.ticket.poolKey)
        .values
        .exists(records =>
          records.combinations(2).exists:
            case List(a, b) => math.abs(a.ticket.ecr.rating - b.ticket.ecr.rating) <= 80
            case _          => false
        )
    )
    assert(result.summary.contains("simulated player"))
    assert(result.summary.contains("currently waiting"))
    assert(result.summary.contains("queue"))
    assert(!result.summary.toLowerCase.contains("seed"))
    assert(!result.summary.toLowerCase.contains("ticket"))

  test("simulation seeding respects selected time-control families"):
    val repo = new InMemoryPlaySearchRepository
    val config = BotSimulationConfig.fromSettings(controls.copy(botCount = 8, timeControls = "blitz,rapid"))
    val runtime = BotSimulationRuntimeState.empty.copy(running = true, revision = 10L, startedAt = Some(now), lastAction = "simulation_started")

    val result = seedSimulation(repo, config, runtime, now)
    val activeBuckets = activeSimulationTickets(repo).map(_.ticket.poolKey.timeControl).toSet

    assert(result.valid)
    assertEquals(result.requestedTickets, 8)
    assertEquals(activeBuckets, Set(EvenChessMode.TimeControlBucket.Blitz, EvenChessMode.TimeControlBucket.Rapid))

  test("simulation seeding can use real account roster ids for human-style game creation"):
    val repo = new InMemoryPlaySearchRepository
    val config = BotSimulationConfig.fromSettings(
      controls.copy(botCount = 4, accountRoster = "evenbot1, evenbot2, bad$id, also.bad")
    )
    val runtime = BotSimulationRuntimeState.empty.copy(running = true, revision = 12L, startedAt = Some(now), lastAction = "simulation_started")

    val result = seedSimulation(repo, config, runtime, now)
    val active = activeSimulationTickets(repo)

    assert(result.valid)
    assertEquals(result.requestedTickets, 2)
    assertEquals(active.map(_.ticket.playerId).toSet, Set("evenbot1", "evenbot2"))
    assert(active.forall(isRosterBackedSimulationRecord))
    assert(active.forall(_.ticket.botProfile.flatMap(_.userRef).isDefined))

  test("simulation reseeding restores consumed tickets without duplicating active bot population"):
    val repo = new InMemoryPlaySearchRepository
    val config = BotSimulationConfig.fromSettings(controls.copy(botCount = 4))
    val runtime = BotSimulationRuntimeState.empty.copy(running = true, revision = 9L, startedAt = Some(now), lastAction = "simulation_started")

    val first = seedSimulation(repo, config, runtime, now)
    val consumedTicket = activeSimulationTickets(repo).head.ticket.ticketId
    repo.removeWhere(_.ticket.ticketId == consumedTicket)
    val second = seedSimulation(repo, config, runtime, now + 1_000L)
    val active = activeSimulationTickets(repo)

    assertEquals(first.createdTickets, 4)
    assertEquals(second.createdTickets, 1)
    assertEquals(second.existingTickets, 3)
    assertEquals(active.size, 4)
    assert(active.exists(_.ticket.ticketId == consumedTicket))

  test("simulation stop clears only simulation bot tickets"):
    val repo = new InMemoryPlaySearchRepository
    val config = BotSimulationConfig.fromSettings(controls.copy(botCount = 4))
    val runtime = BotSimulationRuntimeState.empty.copy(running = true, revision = 8L, startedAt = Some(now), lastAction = "simulation_started")
    val human =
      SearchStartService
        .prepare(
          SearchStartRequest(
            ticketId = "human-ticket",
            playerId = "human-player",
            form = PlayForm.default,
            tokenSnapshot = TokenSnapshot.phaseIOnboardingDefault,
            expectedUsedOffset = ExpectedOffsetEstimate.forSetLevel(PlayForm.default.setLevel),
            latencyMillis = 40,
            createdAt = now
          ),
          repo
        )
        .toOption
        .get

    seedSimulation(repo, config, runtime, now)
    val removed = clearSimulationTickets(repo)

    assertEquals(removed, 4)
    assertEquals(activeSimulationTickets(repo).size, 0)
    assertEquals(repo.get(human.record.ticket.ticketId), Some(human.record))

  test("admin state exposes matchmaking and simulation monitor values"):
    val backend =
      AdminBackendSettings.default.copy(
        matchmaking = AdminBackendSettings.default.matchmaking.copy(
          botModeEnabled = true,
          botModeScope = "rated",
          botMatchTimeoutSeconds = 30,
          botAccountRoster = "evenbot1"
        ),
        botSimulation = controls
      )
    val model = adminState(backend, BotSimulationRuntimeState.empty, now, new InMemoryPlaySearchRepository)

    assert(model.valid)
    assert(model.matchmaking.enabled)
    assertEquals(model.matchmaking.scope, "rated")
    assertEquals(model.matchmaking.timeoutSeconds, 30)
    assertEquals(model.matchmaking.accountRoster, "evenbot1")
    assertEquals(model.simulation.config.accountRosterCsv, "evenbot1")
    assert(model.matchmaking.disclosure.contains("On"))
    assertEquals(model.simulation.config.botCount, 12)
    assertEquals(model.simulation.activeTickets, 0)
