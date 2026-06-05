package lila.evenchess

import CoachingLadder.Level
import EcrRating.EcrPool
import EvenChessMode.TimeControlBucket
import LevelBasedMatchmaking.{ BotMatchPersona, BotMatchProfile, MmrEngine }
import PlaySearchIntegration.{
  BotModeScope,
  ExpectedOffsetEstimate,
  PlayMode,
  PlaySearchRepository,
  SearchIntentRecord,
  SearchRepositoryRuntime,
  SearchStartRequest,
  SearchStartService,
  TimeControlOption,
  TimeControlOptions,
  TokenSnapshot
}

object BotOperations:

  val simulationTicketPrefix = "ec-sim-"
  val maxSimulationBots = 2000
  val defaultSimulationBots = 1000

  enum BotPersonaMode(val key: String, val label: String):
    case Mixed extends BotPersonaMode("mixed", "Mixed")
    case HumanLike extends BotPersonaMode("human-like", "Human-like")
    case Fast extends BotPersonaMode("fast", "Fast")

    def personaFor(index: Int): Option[BotMatchPersona] =
      this match
        case Mixed     => Some(if index % 3 == 0 then BotMatchPersona.Fast else BotMatchPersona.HumanLike)
        case HumanLike => Some(BotMatchPersona.HumanLike)
        case Fast      => Some(BotMatchPersona.Fast)

  object BotPersonaMode:
    val default = Mixed
    val all: List[BotPersonaMode] = values.toList

    def fromRaw(value: String): BotPersonaMode =
      all.find(_.key == value.trim.toLowerCase).getOrElse(default)

  final case class SimulationTimeControlOption(key: String, label: String, help: String):
    def timeControl: TimeControlOption =
      TimeControlOptions.fromKey(key).getOrElse(TimeControlOptions.default)

  object SimulationTimeControlOptions:
    val all: List[SimulationTimeControlOption] = List(
      SimulationTimeControlOption("bullet", "Bullet", "Short clock pool for fast low-latency stress tests."),
      SimulationTimeControlOption("blitz", "Blitz", "Most common quick-search pool; useful for normal launch-fill checks."),
      SimulationTimeControlOption("rapid", "Rapid", "Slower live pool for testing longer games and ECE load over time."),
      SimulationTimeControlOption("classical", "Classical", "Longer live pool for low-frequency simulation coverage.")
    )
    val defaultCsv: String = all.map(_.key).mkString(",")

    def fromCsv(value: String): List[SimulationTimeControlOption] =
      val wanted = value
        .split("[,\\s]+")
        .toList
        .map(_.trim.toLowerCase)
        .filter(_.nonEmpty)
        .distinct
      val parsed = all.filter(option => wanted.contains(option.key))
      if parsed.nonEmpty then parsed else all

    def csvFor(options: List[SimulationTimeControlOption]): String =
      options.map(_.key).distinct.mkString(",")

  object BotAccountRoster:
    private val accountIdPattern = "^[A-Za-z0-9_-]{2,32}$".r
    val generatedDefaultCount = 1000
    val generatedDefaultPrefix = "ecbot"
    private val minEstablishedDisplayRating = 650
    private val maxEstablishedDisplayRating = 2450

    def generated(count: Int = generatedDefaultCount, prefix: String = generatedDefaultPrefix): List[String] =
      val bounded = math.max(0, math.min(count, maxSimulationBots))
      (1 to bounded).toList.map(index => f"$prefix$index%04d")

    val generatedDefault: List[String] = generated()
    val generatedDefaultCsv: String = csvFor(generatedDefault)

    def fromCsv(value: String): List[String] =
      value
        .split("[,\\s]+")
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .filter(account => accountIdPattern.matches(account))
        .distinct

    def effectiveFromCsv(value: String): List[String] =
      fromCsv(value) match
        case Nil      => generatedDefault
        case accounts => accounts

    def csvFor(accounts: List[String]): String =
      accounts.distinct.mkString(",")

    def establishedDisplayRating(account: String, targetRating: Option[Int] = None): Int =
      val base =
        targetRating.getOrElse:
          minEstablishedDisplayRating + (stableHash(account) % (maxEstablishedDisplayRating - minEstablishedDisplayRating + 1))
      val jittered = base + stableJitter(account)
      nonRoundRating(clamp(jittered, minEstablishedDisplayRating, maxEstablishedDisplayRating))

    private def stableHash(value: String): Int =
      val raw = value.foldLeft(0x6d2b79f5): (hash, char) =>
        val next = hash ^ char.toInt
        next * 16777619
      math.abs(raw)

    private def stableJitter(value: String): Int =
      (stableHash(s"$value-rating-jitter") % 41) - 20

    private def nonRoundRating(value: Int): Int =
      val adjusted =
        if value == 1500 then value + 17
        else if value % 100 == 0 then value + 13
        else if value % 50 == 0 then value + 9
        else if value % 10 == 0 then value + 3
        else value
      clamp(adjusted, minEstablishedDisplayRating, maxEstablishedDisplayRating)

    private def clamp(value: Int, min: Int, max: Int): Int =
      math.max(min, math.min(max, value))

  final case class BotSimulationConfig(
      enabled: Boolean,
      scope: BotModeScope,
      botCount: Int,
      ratingMin: Int,
      ratingMax: Int,
      levelMin: Level,
      levelMax: Level,
      persona: BotPersonaMode,
      timeControls: List[SimulationTimeControlOption],
      accountRoster: List[String]
  ):
    def effectiveBotCount: Int =
      if accountRoster.nonEmpty then math.min(botCount, accountRoster.size) else 0

    def valid: Boolean =
      botCount >= 0 &&
        botCount <= maxSimulationBots &&
        ratingMin >= 100 &&
        ratingMax <= 5000 &&
        ratingMin <= ratingMax &&
        levelMin.value <= levelMax.value &&
        timeControls.nonEmpty

    def scopeLabel: String = scope.label
    def timeControlCsv: String = SimulationTimeControlOptions.csvFor(timeControls)
    def accountRosterCsv: String = BotAccountRoster.csvFor(accountRoster)
    def accountBacked: Boolean = accountRoster.nonEmpty

  object BotSimulationConfig:
    val default: BotSimulationConfig =
      BotSimulationConfig(
        enabled = false,
        scope = BotModeScope.Both,
        botCount = defaultSimulationBots,
        ratingMin = 900,
        ratingMax = 2100,
        levelMin = Level(0),
        levelMax = Level(10),
        persona = BotPersonaMode.Mixed,
        timeControls = SimulationTimeControlOptions.all,
        accountRoster = Nil
      )

    def fromSettings(settings: AdminBackendSettings.BotSimulationControls): BotSimulationConfig =
      val normalizedMinRating = clamp(settings.ratingMin, 100, 5000)
      val normalizedMaxRating = clamp(settings.ratingMax, normalizedMinRating, 5000)
      val normalizedMinLevel = clamp(settings.levelMin, Level.min, Level.max)
      val normalizedMaxLevel = clamp(settings.levelMax, normalizedMinLevel, Level.max)
      BotSimulationConfig(
        enabled = settings.enabled,
        scope = BotModeScope.fromRaw(settings.scope).getOrElse(BotModeScope.Both),
        botCount = clamp(settings.botCount, 0, maxSimulationBots),
        ratingMin = normalizedMinRating,
        ratingMax = normalizedMaxRating,
        levelMin = Level(normalizedMinLevel),
        levelMax = Level(normalizedMaxLevel),
        persona = BotPersonaMode.fromRaw(settings.persona),
        timeControls = SimulationTimeControlOptions.fromCsv(settings.timeControls),
        accountRoster = BotAccountRoster.effectiveFromCsv(settings.accountRoster)
      )

  final case class BotSimulationRuntimeState(
      running: Boolean,
      revision: Long,
      startedAt: Option[Long],
      stoppedAt: Option[Long],
      lastActionAt: Option[Long],
      lastAction: String,
      lastAdminId: String,
      seededTicketsTotal: Int,
      activeTickets: Int,
      lastSeededAt: Option[Long],
      lastSeedSummary: String
  ):
    def valid: Boolean =
      revision >= 0 &&
        activeTickets >= 0 &&
        seededTicketsTotal >= 0 &&
        lastAction.nonEmpty &&
        lastAdminId.nonEmpty

    def uptimeMillis(now: Long): Option[Long] =
      startedAt.filter(_ => running).map(start => math.max(0L, now - start))

  object BotSimulationRuntimeState:
    val empty: BotSimulationRuntimeState =
      BotSimulationRuntimeState(
        running = false,
        revision = 0L,
        startedAt = None,
        stoppedAt = None,
        lastActionAt = None,
        lastAction = "idle",
        lastAdminId = "system",
        seededTicketsTotal = 0,
        activeTickets = 0,
        lastSeededAt = None,
        lastSeedSummary = "No simulated-player queue refill has run yet."
      )

  object BotSimulationRuntime:
    @volatile private var current = BotSimulationRuntimeState.empty

    def status: BotSimulationRuntimeState =
      current.copy(activeTickets = activeSimulationTickets(SearchRepositoryRuntime.local).size)

    def start(config: BotSimulationConfig, adminId: String, now: Long): BotSimulationRuntimeState =
      val nextRevision = current.revision + 1
      current = current.copy(
        running = config.enabled,
        revision = nextRevision,
        startedAt = Option.when(config.enabled)(now),
        stoppedAt = None,
        lastActionAt = Some(now),
        lastAction = if config.enabled then "simulation_started" else "simulation_config_saved_disabled",
        lastAdminId = adminId,
        activeTickets = 0,
        lastSeedSummary = if config.enabled then "Simulation started; the simulated-player queue will be filled." else "Simulation is disabled in settings."
      )
      current

    def stop(adminId: String, now: Long): BotSimulationRuntimeState =
      current = current.copy(
        running = false,
        stoppedAt = Some(now),
        lastActionAt = Some(now),
        lastAction = "simulation_stopped",
        lastAdminId = adminId,
        activeTickets = 0,
        lastSeedSummary = "Simulation stopped and simulated-player queue entries were cleared."
      )
      current

    def recordSeed(result: SimulationSeedResult, adminId: String, now: Long): BotSimulationRuntimeState =
      current = current.copy(
        lastActionAt = Some(now),
        lastAction = "simulation_seeded",
        lastAdminId = adminId,
        seededTicketsTotal = current.seededTicketsTotal + result.createdTickets,
        activeTickets = result.activeTickets,
        lastSeededAt = Some(now),
        lastSeedSummary = result.summary
      )
      current

  final case class SimulationSeedResult(
      requestedTickets: Int,
      createdTickets: Int,
      existingTickets: Int,
      removedStaleTickets: Int,
      activeTickets: Int,
      potentialBotVsBotContracts: Int,
      rejectedTickets: List[String]
  ):
    def valid: Boolean =
      requestedTickets >= 0 &&
        createdTickets >= 0 &&
        existingTickets >= 0 &&
        removedStaleTickets >= 0 &&
        activeTickets >= 0 &&
        potentialBotVsBotContracts >= 0

    def summary: String =
      if requestedTickets == 0 && rejectedTickets.nonEmpty then rejectedTickets.mkString("; ")
      else
        s"Target $requestedTickets simulated player(s); $activeTickets currently waiting, $createdTickets added, $existingTickets already waiting, $removedStaleTickets old queue entries removed, $potentialBotVsBotContracts possible sim-vs-sim match(es)."

  object SimulationSeedResult:
    val disabled: SimulationSeedResult =
      SimulationSeedResult(0, 0, 0, 0, 0, 0, Nil)

  final case class MatchmakingAdminState(
      enabled: Boolean,
      scope: String,
      timeoutSeconds: Int,
      accountRoster: String,
      disclosure: String
  ):
    def valid: Boolean =
      scope.nonEmpty && timeoutSeconds >= 5 && disclosure.nonEmpty

  final case class SimulationAdminState(
      config: BotSimulationConfig,
      runtime: BotSimulationRuntimeState,
      activeTickets: Int,
      uptimeMillis: Option[Long],
      potentialBotVsBotContracts: Int
  ):
    def valid: Boolean =
      config.valid && runtime.valid && activeTickets >= 0 && potentialBotVsBotContracts >= 0

  final case class BotOpsAdminState(
      matchmaking: MatchmakingAdminState,
      simulation: SimulationAdminState
  ):
    def valid: Boolean = matchmaking.valid && simulation.valid

  def adminState(
      backend: AdminBackendSettings.BackendSettings,
      runtime: BotSimulationRuntimeState,
      now: Long,
      repository: PlaySearchRepository = SearchRepositoryRuntime.local
  ): BotOpsAdminState =
    val sharedRoster =
      val matchmakingRoster = BotAccountRoster.fromCsv(backend.matchmaking.botAccountRoster)
      val simulationRoster = BotAccountRoster.fromCsv(backend.botSimulation.accountRoster)
      if matchmakingRoster.nonEmpty then matchmakingRoster
      else if simulationRoster.nonEmpty then simulationRoster
      else BotAccountRoster.generatedDefault
    val sharedRosterCsv = BotAccountRoster.csvFor(sharedRoster)
    val simulationConfig =
      BotSimulationConfig.fromSettings(backend.botSimulation.copy(accountRoster = sharedRosterCsv))
    val activeTickets = activeSimulationTickets(repository).size
    BotOpsAdminState(
      matchmaking = MatchmakingAdminState(
        enabled = backend.matchmaking.botModeEnabled,
        scope = PlaySearchIntegration.BotModeScope
          .fromRaw(backend.matchmaking.botModeScope)
          .getOrElse(PlaySearchIntegration.BotModeScope.Both)
          .label,
        timeoutSeconds = PlaySearchIntegration.BotModeConfig
          .fromSettings(
            enabled = backend.matchmaking.botModeEnabled,
            scope = backend.matchmaking.botModeScope,
            timeoutSeconds = backend.matchmaking.botMatchTimeoutSeconds,
            accountRoster = sharedRosterCsv
          )
          .timeoutSeconds,
        accountRoster = sharedRosterCsv,
        disclosure =
          s"Bots may be implemented after long wait times while EvenChess's player pool is low. This will be removed as we grow. Bots are currently ${if backend.matchmaking.botModeEnabled then "On" else "Off"}."
      ),
      simulation = SimulationAdminState(
        config = simulationConfig,
        runtime = runtime.copy(activeTickets = activeTickets),
        activeTickets = activeTickets,
        uptimeMillis = runtime.uptimeMillis(now),
        potentialBotVsBotContracts = potentialBotVsBotContracts(repository)
      )
    )

  def seedSimulation(
      repository: PlaySearchRepository,
      config: BotSimulationConfig,
      runtime: BotSimulationRuntimeState,
      now: Long
  ): SimulationSeedResult =
    if !config.enabled || !runtime.running || config.botCount <= 0 then
      SimulationSeedResult.disabled
    else if config.accountRoster.isEmpty then
      SimulationSeedResult(
        requestedTickets = 0,
        createdTickets = 0,
        existingTickets = 0,
        removedStaleTickets = clearSimulationTickets(repository),
        activeTickets = activeSimulationTickets(repository).size,
        potentialBotVsBotContracts = 0,
        rejectedTickets = List("No roster-backed bot accounts are configured, so simulation cannot create human-style bot players.")
      )
    else
      val desired = desiredSimulationRequests(config, runtime.revision, now)
      val desiredIds = desired.map(_.ticketId).toSet
      val removedStale = repository.removeWhere(record => isSimulationRecord(record) && !desiredIds(record.ticket.ticketId))
      var created = 0
      var existing = 0
      var rejected = List.empty[String]

      desired.foreach: request =>
        repository.get(request.ticketId) match
          case Some(record) if record.ticket.isBotTicket =>
            existing = existing + 1
          case _ =>
            SearchStartService.prepare(request, repository) match
              case Right(_) => created = created + 1
              case Left(error) =>
                rejected = rejected :+ s"${request.ticketId}: $error"

      val activeTickets = activeSimulationTickets(repository).size
      SimulationSeedResult(
        requestedTickets = desired.size,
        createdTickets = created,
        existingTickets = existing,
        removedStaleTickets = removedStale,
        activeTickets = activeTickets,
        potentialBotVsBotContracts = potentialBotVsBotContracts(repository),
        rejectedTickets = rejected
      )

  def clearSimulationTickets(repository: PlaySearchRepository): Int =
    repository.removeWhere(isSimulationRecord)

  def activeSimulationTickets(repository: PlaySearchRepository): List[SearchIntentRecord] =
    repository.active.filter(isSimulationRecord)

  def isSimulationRecord(record: SearchIntentRecord): Boolean =
    record.ticket.ticketId.startsWith(simulationTicketPrefix) && record.ticket.isBotTicket

  def isRosterBackedSimulationRecord(record: SearchIntentRecord): Boolean =
    isSimulationRecord(record) &&
      record.ticket.botProfile.flatMap(_.userRef).contains(record.ticket.playerId) &&
      !record.ticket.playerId.startsWith(simulationTicketPrefix)

  private def playModesForScope(scope: BotModeScope): List[PlayMode] =
    scope match
      case BotModeScope.RatedOnly  => List(PlayMode.RatedEvenChess)
      case BotModeScope.CasualOnly => List(PlayMode.CasualEvenChess)
      case BotModeScope.Both       => List(PlayMode.RatedEvenChess, PlayMode.CasualEvenChess)

  private def simulationSlots(config: BotSimulationConfig): List[(PlayMode, TimeControlOption)] =
    for
      timeControl <- config.timeControls.map(_.timeControl)
      mode <- playModesForScope(config.scope)
    yield mode -> timeControl

  private def pairedCohortRating(config: BotSimulationConfig, cohortIndex: Int, perSlotCount: Int): Int =
    val pairCount = math.max(1, math.ceil(perSlotCount.toDouble / 2.0).toInt)
    val pairIndex = cohortIndex / 2
    val base = centerOutSpread(pairIndex, pairCount, config.ratingMin, config.ratingMax)
    val jitter = if cohortIndex % 2 == 0 then -20 else 20
    clamp(base + jitter, config.ratingMin, config.ratingMax)

  private def desiredSimulationRequests(
      config: BotSimulationConfig,
      revision: Long,
      now: Long
  ): List[SearchStartRequest] =
    val slots = simulationSlots(config)
    val slotCount = math.max(1, slots.size)
    val perSlotCount = math.max(1, math.ceil(config.effectiveBotCount.toDouble / slotCount.toDouble).toInt)
    (0 until config.effectiveBotCount).toList.map: index =>
      val (mode, timeControl) =
        slots.lift(index % slotCount).getOrElse(PlayMode.RatedEvenChess -> TimeControlOptions.default)
      val cohortIndex = index / slotCount
      val poolKey = SearchStartService.poolKeyFor(mode, timeControl.bucket)
      val accountId = config.accountRoster.lift(index)
      val ticketId = s"$simulationTicketPrefix${revision}-${config.scope.label}-$index"
      val targetRating = pairedCohortRating(config, cohortIndex, perSlotCount)
      val profile = profileFor(ticketId, config, index, targetRating, timeControl.bucket, poolKey.ecrPool)
      val random = scala.util.Random(ticketId.hashCode() * 31 + revision.toInt)
      SearchStartRequest(
        ticketId = ticketId,
        playerId = accountId.getOrElse(s"$simulationTicketPrefix-player-${revision}-$index"),
        form = PlaySearchIntegration.PlayForm.default.copy(
          mode = mode,
          timeControl = timeControl,
          setLevel = profile.preferredSetLevel,
          targetLevel = None,
          applyPreferences = false,
          preferredSetLevel = None,
          confirmsOutsideHelpRule = true,
          confirmsLevelContract = true
        ),
        tokenSnapshot = TokenSnapshot.phaseIOnboardingDefault.copy(
          subscriptionActive = true,
          source = "evenchess-bot-simulation"
        ),
        expectedUsedOffset = ExpectedOffsetEstimate.forSetLevel(profile.preferredSetLevel),
        botProfile = Some(profile.copy(userRef = accountId.orElse(profile.userRef))),
        latencyMillis = profile.nextMatchLatency(random),
        createdAt = math.max(1L, now - (index % 12).toLong * 250L)
      )

  private def profileFor(
      ticketId: String,
      config: BotSimulationConfig,
      index: Int,
      rating: Int,
      timeControl: TimeControlBucket,
      pool: EcrPool
  ): BotMatchProfile =
    val levelSpan = config.levelMax.value - config.levelMin.value + 1
    val level = Level(config.levelMin.value + (if levelSpan <= 0 then 0 else index % levelSpan))
    BotMatchProfile
      .fromSeed(
        botId = ticketId,
        userRef = config.accountRoster.lift(index).orElse(Some(s"simulation-$index")),
        timeControl = Some(timeControl),
        pool = pool,
        persona = config.persona.personaFor(index)
      )
      .copy(
        targetEcr = rating,
        preferredSetLevel = level,
        stockfishLevel = LevelBasedMatchmaking.LichessEquivalentStockfishLevel.levelForRating(rating)
      )
  private def potentialBotVsBotContracts(repository: PlaySearchRepository): Int =
    activeSimulationTickets(repository)
      .groupBy(_.ticket.poolKey)
      .values
      .toList
      .map: records =>
        val bounded = records.take(50)
        bounded.count: request =>
          val candidates = bounded.filterNot(_.ticket.ticketId == request.ticket.ticketId)
          candidates.nonEmpty &&
            MmrEngine
              .simulate(
                requestId = s"simulation-monitor-${request.ticket.ticketId}",
                request = request.ticket,
                candidates = candidates.map(_.ticket),
                preferences = request.matchPreferences,
                tokenGateResult = "simulation",
                candidatePreferences = candidates.map(candidate => candidate.ticket.ticketId -> candidate.matchPreferences).toMap
              )
              .matched
      .sum

  private def centerOutSpread(index: Int, count: Int, min: Int, max: Int): Int =
    val midpoint = min + math.round((max - min).toDouble / 2.0).toInt
    if count <= 1 || index <= 0 then midpoint
    else
      val sideSlots = math.max(1, math.ceil((count - 1).toDouble / 2.0).toInt)
      val step = math.max(1, math.round((max - min).toDouble / 2.0 / sideSlots.toDouble).toInt)
      val ring = (index + 1) / 2
      val direction = if index % 2 == 1 then -1 else 1
      clamp(midpoint + direction * step * ring, min, max)

  private def clamp(value: Int, min: Int, max: Int): Int =
    math.max(min, math.min(max, value))
