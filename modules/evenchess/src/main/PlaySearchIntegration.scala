package lila.evenchess

import AssistanceAccounting.UsedOffset
import CoachingLadder.Level
import EcrRating.{ EcrPool, EcrRecord, SearchStage }
import EvenChessMode.{ SetLevel, TimeControlBucket }
import GamePolicy.{ GamePolicyMode, PlayerPolicy }
import LevelBasedMatchmaking.{
  MatchContract,
  MatchContractAuditRecord,
  MatchPreferences,
  MmrSimulationResult,
  MmrEngine,
  PairingConfirmation,
  PairingDecision,
  PoolKey,
  RequestedClock,
  SearchQueue,
  SearchTelemetryContext,
  SearchTicket
}
import MonetisationPolicy.PlanTier
import ProductInvariants.RequirementClass
import TelemetryAnalytics.TelemetryEvent

object PlaySearchIntegration:

  enum PhaseIRequirement:
    case LichessGameLifecyclePreserved
    case PublicCtasUseEvenChessPlayRoutes
    case LevelTimeRatedTokenSearchForm
    case SearchUsesEcrSetLevelAndExpectedOffset
    case ServerOwnedMetadataBeforeCoaching
    case EcrIsolationFromNormalLichessRatings
    case TargetLevelQueueIsolation
    case LilaPairingAdapterThinAndPatchMapped
    case SubscriptionTokenStateAccessOnly
    case SearchHandsOffToMmrEngine
    case MmrContractReadyBeforeLilaGameCreation

  final case class PhaseIRequirementClassification(
      requirement: PhaseIRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseIRequirementClassifications:
    val all: List[PhaseIRequirementClassification] = List(
      PhaseIRequirementClassification(
        PhaseIRequirement.LichessGameLifecyclePreserved,
        RequirementClass.LichessProvided,
        "Lichess remains owner of legal moves, boards, clocks, game lifecycle, move history, and normal rating internals."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.PublicCtasUseEvenChessPlayRoutes,
        RequirementClass.AdaptedToLichessFork,
        "Public play CTAs now route to EvenChess play/search surfaces instead of homepage anchors or normal Lichess pools."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.LevelTimeRatedTokenSearchForm,
        RequirementClass.EvenChessSpecific,
        "The play flow captures Set Level, time control, rated/casual/target mode, outside-help acknowledgement, and token eligibility."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.SearchUsesEcrSetLevelAndExpectedOffset,
        RequirementClass.EvenChessSpecific,
        "Search tickets are built from EvenChess ECR pools, Set Level, expected Used Offset, latency, abuse state, and server policy versions."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.ServerOwnedMetadataBeforeCoaching,
        RequirementClass.EvenChessSpecific,
        "Coaching render permission is blocked until the server has persisted an EvenChess game policy record for the paired game."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.EcrIsolationFromNormalLichessRatings,
        RequirementClass.EvenChessSpecific,
        "Search and rating flow metadata reject NormalLichess ELO pools and expose only ECR pool keys for public EvenChess starts."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.TargetLevelQueueIsolation,
        RequirementClass.EvenChessSpecific,
        "Target Level starts use the target queue and TargetShadow ECR pool so normal ECR is not updated."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.LilaPairingAdapterThinAndPatchMapped,
        RequirementClass.AdaptedToLichessFork,
        "The future lila hook/pairing/game-creation adapter should call this service and remain the only upstream pairing seam."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.SubscriptionTokenStateAccessOnly,
        RequirementClass.EvenChessSpecific,
        "Subscription and token state may allow access or consume quota but cannot alter live coaching strength or ECR."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.SearchHandsOffToMmrEngine,
        RequirementClass.EvenChessSpecific,
        "Prepared search records are evaluated by the EvenChess MMR Engine instead of ordinary Lichess public pools."
      ),
      PhaseIRequirementClassification(
        PhaseIRequirement.MmrContractReadyBeforeLilaGameCreation,
        RequirementClass.AdaptedToLichessFork,
        "The integration seam can hand a finalized MMR match contract to a later lila game-creation adapter without creating the game itself."
      )
    )

  object Routes:
    val play = "/evenchess/play"
    val search = "/evenchess/play/search"
    val searchJson = "/evenchess/play/search.json"
    val aiPractice = "/evenchess/play?mode=ai"
    val targetLevel = "/evenchess/play?mode=target"

  object LichessLobbyAdapter:
    val reusesLichessLobbyTable = true
    val normalLobbyStartsDisabledForPublicFlow = true
    val playHref = "/#hook"
    val searchAction = Routes.searchJson
    val aiPracticeHref = "/#ai"
    val targetLevelHref = "/#hook"
    val fallbackSetupHref = "/#hook"
    val includesSetLevel = true
    val includesTimeControl = true
    val includesMode = true
    val includesOutsideHelpDisclosure = true
    val includesPreferredSetLevel = true
    val clientMayCreateNormalRatedGame = false
    val clientMayAuthorizeCoaching = false

    def valid: Boolean =
      reusesLichessLobbyTable &&
        normalLobbyStartsDisabledForPublicFlow &&
        playHref == "/#hook" &&
        searchAction == Routes.searchJson &&
        aiPracticeHref == "/#ai" &&
        targetLevelHref == "/#hook" &&
        fallbackSetupHref == "/#hook" &&
        includesSetLevel &&
        includesTimeControl &&
        includesMode &&
        includesOutsideHelpDisclosure &&
        includesPreferredSetLevel &&
        !clientMayCreateNormalRatedGame &&
        !clientMayAuthorizeCoaching

  enum PlayMode:
    case RatedEvenChess
    case CasualEvenChess
    case TargetLevel
    case AiPractice

    def key: String =
      this match
        case RatedEvenChess  => "rated"
        case CasualEvenChess => "casual"
        case TargetLevel     => "target"
        case AiPractice      => "ai"

    def label: String =
      this match
        case RatedEvenChess  => "Rated EvenChess"
        case CasualEvenChess => "Casual EvenChess"
        case TargetLevel     => "Target Level"
        case AiPractice      => "AI practice"

    def queue: SearchQueue =
      this match
        case RatedEvenChess  => SearchQueue.NormalEvenChess
        case CasualEvenChess => SearchQueue.CasualEvenChess
        case TargetLevel     => SearchQueue.TargetLevel
        case AiPractice      => SearchQueue.AiPractice

    def publicRated: Boolean = this == RatedEvenChess

    def needsTokenAccess: Boolean =
      this match
        case RatedEvenChess | CasualEvenChess => true
        case TargetLevel | AiPractice         => false

  object PlayMode:
    val publicOptions: List[PlayMode] = List(RatedEvenChess, CasualEvenChess, TargetLevel, AiPractice)

    def fromKey(key: String): Option[PlayMode] =
      publicOptions.find(_.key == key)

  final case class TimeControlOption(
      key: String,
      label: String,
      bucket: TimeControlBucket,
      estimatedSeconds: Int
  )

  object TimeControlOptions:
    val all: List[TimeControlOption] = List(
      TimeControlOption("bullet", "Bullet", TimeControlBucket.Bullet, 120),
      TimeControlOption("rapid", "Rapid 10+0", TimeControlBucket.Rapid, 600),
      TimeControlOption("blitz", "Blitz 5+0", TimeControlBucket.Blitz, 300),
      TimeControlOption("classical", "Classical 30+0", TimeControlBucket.Classical, 1800),
      TimeControlOption("correspondence", "Correspondence", TimeControlBucket.Correspondence, 86400),
      TimeControlOption("casual", "Casual", TimeControlBucket.Casual, 600)
    )

    val default: TimeControlOption = all.find(_.key == "rapid").getOrElse(all.head)

    def fromKey(key: String): Option[TimeControlOption] =
      all.find(_.key == key)

  final case class TokenSnapshot(
      plan: PlanTier,
      gameTokens: Int,
      earnedAdGameTokens: Int,
      subscriptionActive: Boolean,
      abuseClear: Boolean,
      source: String,
      freeMatchTokenWindowActive: Boolean = false
  ):
    def availableGameStarts: Int = gameTokens + earnedAdGameTokens

    def eligibleFor(mode: PlayMode): Boolean =
      abuseClear && (!mode.needsTokenAccess || freeMatchTokenWindowActive || subscriptionActive || availableGameStarts > 0)

    def accessReason(mode: PlayMode): String =
      if !abuseClear then "abuse_controls"
      else if !mode.needsTokenAccess then "mode_does_not_consume_game_token"
      else if freeMatchTokenWindowActive then "launch_free_token_window"
      else if subscriptionActive then "subscription_access"
      else if availableGameStarts > 0 then "game_token_available"
      else "game_token_required"

    def withFreeMatchTokenWindow(active: Boolean): TokenSnapshot =
      if active then copy(freeMatchTokenWindowActive = true, source = s"$source+launch-free-token-window-v1")
      else copy(freeMatchTokenWindowActive = false)

    def fairnessNeutral: Boolean = true

  object TokenSnapshot:
    val phaseIOnboardingDefault: TokenSnapshot =
      TokenSnapshot(
        plan = PlanTier.NewAccountOnboarding,
        gameTokens = 10,
        earnedAdGameTokens = 0,
        subscriptionActive = false,
        abuseClear = true,
        source = "phase-i-onboarding-default"
      )

    val emptyFreeAccount: TokenSnapshot =
      TokenSnapshot(
        plan = PlanTier.FreeAdSupported,
        gameTokens = 0,
        earnedAdGameTokens = 0,
        subscriptionActive = false,
        abuseClear = true,
        source = "phase-i-empty-free-account"
      )

  final case class PlayForm(
      mode: PlayMode,
      timeControl: TimeControlOption,
      setLevel: Level,
      targetLevel: Option[Level],
      applyPreferences: Boolean,
      preferredSetLevel: Option[Level],
      requestedClock: Option[RequestedClock],
      confirmsOutsideHelpRule: Boolean,
      confirmsLevelContract: Boolean
  ):
    def targetLevelForTicket: Option[Level] =
      Option.when(mode == PlayMode.TargetLevel)(
        preferredSetLevel.orElse(targetLevel).getOrElse(setLevel)
      )

    def effectivePreferredSetLevel: Option[Level] =
      Option.when(applyPreferences)(preferredSetLevel).flatten

    def searchScenarioLabel: String =
      if effectivePreferredSetLevel.isDefined then "Preferred set level search"
      else "Normal search"

    def valid: Boolean =
      confirmsOutsideHelpRule &&
        (mode != PlayMode.TargetLevel || targetLevelForTicket.isDefined)

  object PlayForm:
    val default: PlayForm =
      PlayForm(
        mode = PlayMode.RatedEvenChess,
        timeControl = TimeControlOptions.default,
        setLevel = Level(5),
        targetLevel = None,
        applyPreferences = false,
        preferredSetLevel = None,
        requestedClock = None,
        confirmsOutsideHelpRule = true,
        confirmsLevelContract = false
      )

    def parseLevel(value: String): Option[Level] =
      value.toIntOption.filter(Level.isValid).map(Level(_))

    def fromValues(
        modeKey: String,
        timeControlKey: String,
        setLevelValue: String,
        targetLevelValue: Option[String],
        confirmsOutsideHelpRule: Boolean,
        confirmsLevelContract: Boolean,
        applyPreferences: Boolean = false,
        preferredSetLevelValue: Option[String] = None,
        playerTargetLevelValue: Option[String] = None,
        clockLimitSecondsValue: Option[String] = None,
        clockIncrementSecondsValue: Option[String] = None
    ): Either[String, PlayForm] =
      val rawPreferredSetLevel =
        preferredSetLevelValue.orElse(playerTargetLevelValue).map(_.trim).filter(value => value.nonEmpty && value.toLowerCase != "any")
      val preferenceSelected = rawPreferredSetLevel.isDefined
      val requestedClock =
        (clockLimitSecondsValue.flatMap(_.trim.toIntOption), clockIncrementSecondsValue.flatMap(_.trim.toIntOption)) match
          case (Some(limit), Some(increment)) =>
            Some(RequestedClock(limit, increment)).filter(_.valid)
          case _ => None
      for
        mode <- PlayMode.fromKey(modeKey).toRight("Unknown EvenChess play mode.")
        timeControl <- TimeControlOptions.fromKey(timeControlKey).toRight("Unknown EvenChess time control.")
        setLevel <- parseLevel(setLevelValue).toRight("Set Level must be L0-L10.")
        targetLevel <- targetLevelValue.filter(_.nonEmpty) match
          case None        => Right(None)
          case Some(value) => parseLevel(value).map(Some(_)).toRight("Target Level must be L0-L10.")
        preferredSetLevel <- rawPreferredSetLevel match
          case None        => Right(None)
          case Some(value) => parseLevel(value).map(Some(_)).toRight("Preferred Set Level must be L0-L10.")
        form = PlayForm(
          mode,
          timeControl,
          setLevel,
          targetLevel,
          applyPreferences || preferenceSelected,
          preferredSetLevel,
          requestedClock.filter(_ => timeControl.bucket != TimeControlBucket.Casual && timeControl.bucket != TimeControlBucket.Correspondence),
          confirmsOutsideHelpRule,
          confirmsLevelContract
        )
        valid <- Either.cond(form.valid, form, "Outside-help disclosure must be acknowledged before search.")
      yield valid

  final case class SearchAdmissionDecision(
      allowed: Boolean,
      tokenEligible: Boolean,
      reasons: List[String]
  ):
    def valid: Boolean =
      allowed == reasons.isEmpty

  object SearchAdmission:
    def decide(form: PlayForm, tokenSnapshot: TokenSnapshot): SearchAdmissionDecision =
      val tokenEligible = tokenSnapshot.eligibleFor(form.mode)
      val reasons = List(
        Option.when(!form.valid)("invalid_play_form"),
        Option.when(!tokenEligible)(tokenSnapshot.accessReason(form.mode))
      ).flatten.distinct

      SearchAdmissionDecision(
        allowed = reasons.isEmpty,
        tokenEligible = tokenEligible,
        reasons = reasons
      )

  final case class SearchStartRequest(
      ticketId: String,
      playerId: String,
      form: PlayForm,
      tokenSnapshot: TokenSnapshot,
      expectedUsedOffset: UsedOffset,
      botProfile: Option[LevelBasedMatchmaking.BotMatchProfile] = None,
      latencyMillis: Int,
      createdAt: Long
  ):
    def valid: Boolean =
      ticketId.nonEmpty &&
        playerId.nonEmpty &&
        form.valid &&
        tokenSnapshot.fairnessNeutral &&
        botProfile.forall(_.valid) &&
        expectedUsedOffset.nonNegative &&
        latencyMillis >= 0 &&
        createdAt > 0

  object ExpectedOffsetEstimate:
    def modelVersion: String = LevelBasedMatchmaking.LevelOffsetTable.policyVersion

    def forSetLevel(level: Level): UsedOffset =
      LevelBasedMatchmaking.LevelOffsetTable.offsetForLevel(level)

  final case class QueueState(
      key: String,
      label: String,
      persisted: Boolean,
      waitingForPairing: Boolean,
      requiresPairingConfirmation: Boolean
  ):
    def valid: Boolean =
      key.nonEmpty && label.nonEmpty && persisted

  final case class SearchIntentRecord(
      ticket: SearchTicket,
      mode: PlayMode,
      matchPreferences: MatchPreferences,
      tokenSnapshot: TokenSnapshot,
      admission: SearchAdmissionDecision,
      queueState: QueueState,
      createdAt: Long,
      updatedAt: Long
  ):
    def valid: Boolean =
      ticket.valid &&
        matchPreferences.valid &&
        admission.allowed &&
        queueState.valid &&
        tokenSnapshot.fairnessNeutral &&
        createdAt > 0 &&
        updatedAt >= createdAt &&
        ticket.rated == mode.publicRated &&
        ticket.poolKey.ecrPool != EcrPool.NormalLichess

  final case class PreparedSearch(
      record: SearchIntentRecord,
      telemetry: TelemetryEvent,
      coachingMayRender: Boolean
  ):
    def valid: Boolean =
      record.valid &&
        telemetry.readyForRatedLedger &&
        !coachingMayRender

  enum PersistenceError:
    case InvalidSearchIntent
    case MissingSearchIntent

  trait PlaySearchRepository:
    def put(record: SearchIntentRecord): Either[PersistenceError, SearchIntentRecord]
    def get(ticketId: String): Option[SearchIntentRecord]
    def active: List[SearchIntentRecord]
    def activeInPool(poolKey: PoolKey): List[SearchIntentRecord]
    def removeWhere(predicate: SearchIntentRecord => Boolean): Int

  final class InMemoryPlaySearchRepository extends PlaySearchRepository:
    private var records = Map.empty[String, SearchIntentRecord]

    def put(record: SearchIntentRecord): Either[PersistenceError, SearchIntentRecord] =
      if !record.valid then Left(PersistenceError.InvalidSearchIntent)
      else
        records = records.updated(record.ticket.ticketId, record)
        Right(record)

    def get(ticketId: String): Option[SearchIntentRecord] =
      records.get(ticketId)

    def active: List[SearchIntentRecord] =
      records.values.filter(_.queueState.waitingForPairing).toList

    def activeInPool(poolKey: PoolKey): List[SearchIntentRecord] =
      records.values.filter(record => record.ticket.poolKey == poolKey && record.queueState.waitingForPairing).toList

    def removeWhere(predicate: SearchIntentRecord => Boolean): Int =
      val before = records.size
      records = records.filterNot { case (_, record) => predicate(record) }
      before - records.size

  object SearchRepositoryRuntime:
    val local: PlaySearchRepository = new InMemoryPlaySearchRepository

  enum BotModeScope:
    case RatedOnly
    case CasualOnly
    case Both

    def allows(queue: SearchQueue): Boolean =
      this match
        case RatedOnly => queue == SearchQueue.NormalEvenChess
        case CasualOnly => queue == SearchQueue.CasualEvenChess
        case Both =>
          queue == SearchQueue.NormalEvenChess || queue == SearchQueue.CasualEvenChess

    def label: String =
      this match
        case RatedOnly  => "rated"
        case CasualOnly => "casual"
        case Both       => "both"

  object BotModeScope:
    def fromRaw(value: String): Option[BotModeScope] =
      value.trim.toLowerCase match
        case "rated" => Some(BotModeScope.RatedOnly)
        case "casual" => Some(BotModeScope.CasualOnly)
        case "both" => Some(BotModeScope.Both)
        case _ => None

  final case class BotModeConfig(
      enabled: Boolean,
      scope: BotModeScope,
      timeoutSeconds: Int,
      accountRoster: List[String] = Nil
  ):
    def valid: Boolean = !enabled || (timeoutSeconds >= 1 && timeoutSeconds <= 3600)
    def hasAccountRoster: Boolean = accountRoster.nonEmpty

  object BotModeConfig:
    val defaultScope = BotModeScope.Both
    val defaultTimeoutSeconds = 45
    val default: BotModeConfig = BotModeConfig(
      enabled = false,
      scope = defaultScope,
      timeoutSeconds = defaultTimeoutSeconds,
      accountRoster = Nil
    )

    def fromSettings(
        enabled: Boolean,
        scope: String,
        timeoutSeconds: Int,
        accountRoster: String = ""
    ): BotModeConfig =
      val parsedScope = BotModeScope.fromRaw(scope).getOrElse(defaultScope)
      BotModeConfig(
        enabled = enabled,
        scope = parsedScope,
        timeoutSeconds = if timeoutSeconds >= 1 && timeoutSeconds <= 3600 then timeoutSeconds else defaultTimeoutSeconds,
        accountRoster = BotOperations.BotAccountRoster.effectiveFromCsv(accountRoster)
      )

  final case class BotModeStatus(
      enabled: Boolean,
      scope: BotModeScope,
      timeoutSeconds: Int,
      elapsedMillis: Long,
      seedAttempted: Boolean,
      botSeeded: Boolean,
      botCandidatesVisible: Boolean
  ):
    def valid: Boolean = timeoutSeconds >= 1 && elapsedMillis >= 0

  enum MatchContractSource:
    case Bot
    case Human

    def label: String = this match
      case Bot => "bot"
      case Human => "human"

  object SearchStartService:
    val policyVersion = "evenchess-play-search-v1"
    val telemetrySchemaVersion = "evenchess-play-search-telemetry-v1"

    def poolKeyFor(mode: PlayMode, timeControl: TimeControlBucket): PoolKey =
      mode.queue match
        case SearchQueue.NormalEvenChess => PoolKey.normal(timeControl)
        case SearchQueue.CasualEvenChess => PoolKey.casual(timeControl)
        case SearchQueue.TargetLevel     => PoolKey.target(timeControl)
        case SearchQueue.AiPractice      => PoolKey(SearchQueue.AiPractice, timeControl, ecrPoolFor(timeControl))

    private def ecrPoolFor(timeControl: TimeControlBucket): EcrPool =
      timeControl match
        case TimeControlBucket.Bullet         => EcrPool.Bullet
        case TimeControlBucket.Blitz          => EcrPool.Blitz
        case TimeControlBucket.Rapid          => EcrPool.Rapid
        case TimeControlBucket.Classical      => EcrPool.Classical
        case TimeControlBucket.Correspondence => EcrPool.Correspondence
        case TimeControlBucket.Casual         => EcrPool.Rapid

    private def preparedFromRecord(record: SearchIntentRecord, now: Long, playerId: String): PreparedSearch =
      val telemetryContext = SearchTelemetryContext(
        schemaVersion = telemetrySchemaVersion,
        occurredAt = now,
        pseudonymousUserId = playerId
      )
      PreparedSearch(
        record = record,
        telemetry = LevelBasedMatchmaking.SearchTelemetry.searchStarted(record.ticket, telemetryContext),
        coachingMayRender = false
      )

    def prepare(
        request: SearchStartRequest,
        repository: PlaySearchRepository
    ): Either[String, PreparedSearch] =
      if !request.valid then Left("Invalid EvenChess search request.")
      else
        val admission = SearchAdmission.decide(request.form, request.tokenSnapshot)
        if !admission.allowed then Left(admission.reasons.mkString(", "))
        else
          val poolKey = poolKeyFor(request.form.mode, request.form.timeControl.bucket)
          val ecr = request.botProfile.fold(
            EcrRecord.provisional(request.playerId, poolKey.ecrPool, request.createdAt)
          ): botProfile =>
            EcrRecord.provisional(request.playerId, poolKey.ecrPool, request.createdAt).copy(
              rating = botProfile.targetEcr,
              gameCount = 20,
              provisional = false
            )
          val ticket = SearchTicket(
              ticketId = request.ticketId,
              playerId = request.playerId,
              poolKey = poolKey,
              requestedClock = request.form.requestedClock,
              ecr = ecr,
            expectedUsedOffset = request.expectedUsedOffset,
            setLevel = request.form.effectivePreferredSetLevel.getOrElse(request.form.setLevel),
            targetLevel = request.form.targetLevelForTicket,
            botProfile = request.botProfile,
            latencyMillis = request.latencyMillis,
            abuseClear = request.tokenSnapshot.abuseClear,
            policyVersion = policyVersion,
            createdAt = request.createdAt
          )
          val queueState = QueueState(
            key = poolKey.key,
            label = request.form.mode.label,
            persisted = true,
            waitingForPairing = request.form.mode != PlayMode.AiPractice,
            requiresPairingConfirmation = request.form.mode == PlayMode.TargetLevel || request.form.confirmsLevelContract
          )
          val record = SearchIntentRecord(
            ticket = ticket,
            mode = request.form.mode,
            matchPreferences = MatchPreferences(
              preferredOwnSetLevel = request.form.effectivePreferredSetLevel
            ),
            tokenSnapshot = request.tokenSnapshot,
            admission = admission,
            queueState = queueState,
            createdAt = request.createdAt,
            updatedAt = request.createdAt
          )
          val telemetryContext = SearchTelemetryContext(
            schemaVersion = telemetrySchemaVersion,
            occurredAt = request.createdAt,
            pseudonymousUserId = request.playerId
          )
          val persisted = repository.put(record).map: persisted =>
            PreparedSearch(
              record = persisted,
              telemetry = LevelBasedMatchmaking.SearchTelemetry.searchStarted(persisted.ticket, telemetryContext),
              coachingMayRender = false
            )
          persisted.left.map(_.toString)

    def resume(
        ticketId: String,
        playerId: String,
        repository: PlaySearchRepository,
        now: Long
    ): Either[String, PreparedSearch] =
      repository
        .get(ticketId)
        .filter(_.ticket.playerId == playerId)
        .toRight("Search ticket not found.")
        .map(record => preparedFromRecord(record, now, playerId))

  final case class MatchmakingIntegrationResult(
      request: SearchIntentRecord,
      matchedCandidate: Option[SearchIntentRecord],
      contractSource: Option[MatchContractSource],
      simulation: MmrSimulationResult,
      auditRecord: Option[MatchContractAuditRecord],
      botMode: BotModeStatus,
      coachingMayRender: Boolean
  ):
    def contract: Option[MatchContract] = simulation.contract

    def matched: Boolean = simulation.matched && matchedCandidate.isDefined

    def readyForLilaGameCreationAdapter: Boolean =
      matched && contract.exists(_.valid) && !coachingMayRender

    def visibleStatus: String =
      simulation.visibleMessages.headOption.getOrElse("Waiting for an EvenChess MMR match contract.")

    def normalLichessRatingsExcluded: Boolean =
      request.ticket.poolKey.ecrPool != EcrPool.NormalLichess &&
      matchedCandidate.forall(_.ticket.poolKey.ecrPool != EcrPool.NormalLichess)

    def valid: Boolean =
      request.valid &&
        simulation.valid &&
        botMode.valid &&
        !coachingMayRender &&
        normalLichessRatingsExcluded &&
        (simulation.contract.isDefined == matchedCandidate.isDefined) &&
        (auditRecord.isDefined == simulation.contract.isDefined) &&
        (contractSource.isDefined == simulation.contract.isDefined)

  object MatchmakingIntegrationService:
    val auditSignalKeys: List[String] =
      List("repeat_pairing_monitor", "collusion_pattern_monitor", "abort_abuse_monitor", "level_target_manipulation")
    val simulationTicketPrefix = "ec-sim-"

    private def isSimulationBotCandidate(record: SearchIntentRecord): Boolean =
      record.ticket.isBotTicket &&
        record.ticket.ticketId.startsWith(simulationTicketPrefix) &&
        record.ticket.botProfile.flatMap(_.userRef).contains(record.ticket.playerId)

    def evaluate(
        requestTicketId: String,
        repository: PlaySearchRepository,
        now: Long,
        botMode: BotModeConfig = BotModeConfig.default
    ): Either[String, MatchmakingIntegrationResult] =
      def botModeStatusFor(
          elapsedMillis: Long,
          seedAttempted: Boolean,
          botSeeded: Boolean,
          botCandidatesVisible: Boolean
      ): BotModeStatus =
        BotModeStatus(
          enabled = botMode.enabled,
          scope = botMode.scope,
          timeoutSeconds = botMode.timeoutSeconds,
          elapsedMillis = elapsedMillis,
          seedAttempted = seedAttempted,
          botSeeded = botSeeded,
          botCandidatesVisible = botCandidatesVisible
        )

      def buildResult(
          request: SearchIntentRecord,
          matchedCandidate: Option[SearchIntentRecord],
          simulation: MmrSimulationResult,
          auditRecord: Option[MatchContractAuditRecord],
          botModeStatus: BotModeStatus
      ): MatchmakingIntegrationResult =
        val contractSource =
          matchedCandidate.map(candidate =>
            if candidate.ticket.isBotTicket then MatchContractSource.Bot
            else MatchContractSource.Human
          )
        MatchmakingIntegrationResult(
          request = request,
          matchedCandidate = matchedCandidate,
          contractSource = contractSource,
          simulation = simulation,
          auditRecord = auditRecord,
          botMode = botModeStatus,
          coachingMayRender = false
        )

      def simulate(request: SearchIntentRecord, candidates: List[SearchIntentRecord]) =
        MmrEngine.simulate(
          requestId = request.ticket.ticketId,
          request = request.ticket,
          candidates = candidates.map(_.ticket),
          preferences = request.matchPreferences,
          tokenGateResult = request.tokenSnapshot.accessReason(request.mode),
          candidatePreferences = candidates.map(candidate => candidate.ticket.ticketId -> candidate.matchPreferences).toMap
        )

      def matchedCandidateFor(
          request: SearchIntentRecord,
          candidates: List[SearchIntentRecord],
          contract: MatchContract
      ): Option[SearchIntentRecord] =
        val candidatePlayerId =
          if contract.whitePlayerId == request.ticket.playerId then Some(contract.blackPlayerId)
          else if contract.blackPlayerId == request.ticket.playerId then Some(contract.whitePlayerId)
          else None
        candidatePlayerId.flatMap(playerId => candidates.find(_.ticket.playerId == playerId))

      def botModeAllowedForQueue(request: SearchIntentRecord): Boolean =
        botMode.enabled && botMode.scope.allows(request.ticket.poolKey.queue)

      def hasExistingBotSeed(request: SearchIntentRecord): Boolean =
        val botTicketId = botTicketIdFor(request)
        repository.get(botTicketId).exists: record =>
          record.ticket.isBotTicket &&
            record.ticket.botProfile.flatMap(_.userRef).contains(record.ticket.playerId)

      def botTicketIdFor(request: SearchIntentRecord): String =
        s"ec-bot-${request.ticket.ticketId}-seed"

      def rosterAccountFor(request: SearchIntentRecord): Option[String] =
        val eligible = botMode.accountRoster.filterNot(_ == request.ticket.playerId)
        if eligible.isEmpty then None
        else Some(eligible(math.abs(request.ticket.ticketId.hashCode) % eligible.size))

      def seedBot(request: SearchIntentRecord, now: Long): Either[String, SearchIntentRecord] =
        if hasExistingBotSeed(request) then
          repository.get(botTicketIdFor(request)).toRight("Bot seed not found after check.")
        else
          rosterAccountFor(request)
            .toRight("No roster-backed bot account is configured for matchmaking fallback.")
            .flatMap: account =>
            val botPlayerId = account
            def clamp(value: Int, min: Int, max: Int): Int =
              math.max(min, math.min(max, value))

            val (minEcr, maxEcr) = request.ticket.poolKey.ecrPool match
              case EcrRating.EcrPool.TargetShadow     => (700, 2400)
              case EcrRating.EcrPool.NormalLichess    => (600, 2100)
              case EcrRating.EcrPool.Bullet          => (700, 2400)
              case EcrRating.EcrPool.Blitz           => (700, 2400)
              case EcrRating.EcrPool.Rapid           => (700, 2400)
              case EcrRating.EcrPool.Classical       => (700, 2400)
              case EcrRating.EcrPool.Correspondence   => (700, 2400)
            val botProfileBase = LevelBasedMatchmaking.BotMatchProfile.fromSeed(
              botId = botTicketIdFor(request),
              userRef = Some(account),
              timeControl = Some(request.ticket.poolKey.timeControl),
              pool = request.ticket.poolKey.ecrPool
            )
            val ecrAlignRandom = scala.util.Random(botProfileBase.botId.hashCode() + request.ticket.ticketId.hashCode() + now.toInt)
            val ecrAligned = clamp(
              request.ticket.ecr.rating + (ecrAlignRandom.nextGaussian() * 80.0).toInt,
              minEcr,
              maxEcr
            )
            val botProfile =
              request.matchPreferences.preferredOwnSetLevel.fold(
                botProfileBase.copy(
                  targetEcr = ecrAligned,
                  preferredSetLevel = LevelBasedMatchmaking.BaseSetLevelByRatingTable.levelForRating(ecrAligned),
                  stockfishLevel = LevelBasedMatchmaking.LichessEquivalentStockfishLevel.levelForRating(ecrAligned)
                )
              ) { preferredSetLevel =>
                LevelBasedMatchmaking.BotMatchProfile.alignToPlayerPreference(
                  playerProfile = botProfileBase,
                  preferredEcr = request.ticket.ecr.rating,
                  preferredSetLevel = preferredSetLevel,
                  pool = request.ticket.poolKey.ecrPool
                )
              }
            val matchRandom = scala.util.Random(
              botProfile.botId.hashCode() * 97 + request.ticket.ticketId.hashCode() + now.toInt
            )
            val botEcr = EcrRating.EcrRecord.provisional(botPlayerId, request.ticket.poolKey.ecrPool, now).copy(
              rating = botProfile.targetEcr,
              gameCount = request.ticket.ecr.gameCount,
              provisional = false
            )
            val botTicket = SearchTicket(
              ticketId = botTicketIdFor(request),
              playerId = botPlayerId,
              poolKey = request.ticket.poolKey,
              requestedClock = request.ticket.requestedClock,
              ecr = botEcr,
              expectedUsedOffset = request.ticket.expectedUsedOffset,
              setLevel = botProfile.preferredSetLevel,
              targetLevel = request.ticket.targetLevel,
              botProfile = Some(botProfile),
              latencyMillis = botProfile.nextMatchLatency(matchRandom),
              abuseClear = request.ticket.abuseClear,
              policyVersion = SearchStartService.policyVersion,
              createdAt = now
            )
            val botRecord = SearchIntentRecord(
              ticket = botTicket,
              mode = request.mode,
              matchPreferences = MatchPreferences.normal,
              tokenSnapshot = request.tokenSnapshot,
              admission = SearchAdmissionDecision(
                allowed = true,
                tokenEligible = true,
                reasons = Nil
              ),
              queueState = QueueState(
                key = request.ticket.poolKey.key,
                label = request.mode.label,
                persisted = true,
                waitingForPairing = request.mode != PlayMode.AiPractice,
                requiresPairingConfirmation = request.queueState.requiresPairingConfirmation
              ),
              createdAt = now,
              updatedAt = now
            )
            repository.put(botRecord).left.map(_.toString).map(_ => botRecord)

      repository.get(requestTicketId).toRight("Search ticket not found.").flatMap: request =>
        val elapsedMillis = if now >= request.createdAt then now - request.createdAt else 0L
        val baseCandidates =
          repository
            .activeInPool(request.ticket.poolKey)
            .filterNot(_.ticket.ticketId == request.ticket.ticketId)

        val nonFallbackCandidates =
          baseCandidates.filter(record => !record.ticket.isBotTicket || isSimulationBotCandidate(record))
        val humanCandidates = baseCandidates.filterNot(_.ticket.isBotTicket)
        val withoutBotSimulation = simulate(request, nonFallbackCandidates)
        val matchedWithoutBot =
          withoutBotSimulation.contract.flatMap(matchedCandidateFor(request, nonFallbackCandidates, _))
        val matchedResult =
          if withoutBotSimulation.matched then
            withoutBotSimulation
          else
            withoutBotSimulation
        val withoutBotAudit =
          withoutBotSimulation.contract.map(contract => MmrEngine.auditRecord(contract, auditSignalKeys, now))

        if matchedWithoutBot.isDefined then
          Right(
            buildResult(
              request = request,
              matchedCandidate = matchedWithoutBot,
              simulation = matchedResult,
              auditRecord = withoutBotAudit,
              botModeStatus = botModeStatusFor(elapsedMillis, false, false, false)
            )
          )
        else if !botModeAllowedForQueue(request) then
          Right(
            buildResult(
              request = request,
              matchedCandidate = None,
              simulation = withoutBotSimulation,
              auditRecord = withoutBotAudit,
              botModeStatus = botModeStatusFor(elapsedMillis, false, false, false)
            )
          )
        else if elapsedMillis < botMode.timeoutSeconds.toLong * 1000L then
          Right(
            buildResult(
              request = request,
              matchedCandidate = None,
              simulation = withoutBotSimulation,
              auditRecord = withoutBotAudit,
              botModeStatus = botModeStatusFor(elapsedMillis, false, false, false)
            )
          )
        else if !botMode.hasAccountRoster then
          Right(
            buildResult(
              request = request,
              matchedCandidate = None,
              simulation = withoutBotSimulation,
              auditRecord = withoutBotAudit,
              botModeStatus = botModeStatusFor(elapsedMillis, true, false, false)
            )
          )
        else
          seedBot(request, now).map: botRecord =>
            val botCandidates =
              if botRecord.queueState.waitingForPairing then
                humanCandidates :+ botRecord
              else
                humanCandidates
            val withBotSimulation = simulate(request, botCandidates)
            val matchedWithBot =
              withBotSimulation.contract.flatMap(matchedCandidateFor(request, botCandidates, _))
            val withBotAudit = withBotSimulation.contract.map(contract => MmrEngine.auditRecord(contract, auditSignalKeys, now))
            val botSeeded = hasExistingBotSeed(request)
            val botModeStatus = botModeStatusFor(
              elapsedMillis = elapsedMillis,
              seedAttempted = true,
              botSeeded = botSeeded,
              botCandidatesVisible = botCandidates.exists(_.ticket.isBotTicket)
            )

            buildResult(
              request = request,
              matchedCandidate = matchedWithBot,
              simulation = withBotSimulation,
              auditRecord = withBotAudit,
              botModeStatus = botModeStatus
            )
  final case class GameStartPersistence(
      gameId: String,
      pairing: PairingDecision,
      confirmation: PairingConfirmation,
      policyRecord: GamePolicy.GamePolicyRecord,
      telemetry: TelemetryEvent
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        pairing.allowed &&
        confirmation.valid &&
        policyRecord.valid &&
        telemetry.readyForRatedLedger &&
        policyRecord.gameId == gameId

  object GameStartService:
    def policyModeFor(queue: SearchQueue): GamePolicyMode =
      queue match
        case SearchQueue.NormalEvenChess => GamePolicyMode.NormalRatedEvenChess
        case SearchQueue.CasualEvenChess => GamePolicyMode.CasualEvenChess
        case SearchQueue.TargetLevel     => GamePolicyMode.TargetLevel
        case SearchQueue.AiPractice      => GamePolicyMode.AiBotPractice

    private def assignedLevelFor(record: SearchIntentRecord, contract: MatchContract): Option[Level] =
      if record.ticket.playerId == contract.whitePlayerId then Some(contract.whiteSetLevel)
      else if record.ticket.playerId == contract.blackPlayerId then Some(contract.blackSetLevel)
      else None

    private def ticketWithContractLevel(record: SearchIntentRecord, contract: Option[MatchContract]): Either[String, SearchTicket] =
      contract match
        case None => Right(record.ticket)
        case Some(matchContract) =>
          assignedLevelFor(record, matchContract)
            .map(level => record.ticket.withAssignedSetLevel(level))
            .toRight("match_contract_player_mismatch")

    def persistBeforeCoaching(
        gameId: String,
        white: SearchIntentRecord,
        black: SearchIntentRecord,
        stage: SearchStage,
        uiConfirmedLevelContract: Boolean,
        policyRepository: GamePolicy.GamePolicyRepository,
        now: Long,
        assignedContract: Option[MatchContract] = None
    ): Either[String, GameStartPersistence] =
      def validateContract(contract: MatchContract): Either[String, MatchContract] =
        Either.cond(contract.valid, contract, "invalid_match_contract")

      val contractResult: Either[String, Option[MatchContract]] =
        assignedContract match
          case None           => Right(None)
          case Some(contract) => validateContract(contract).map(Some(_))

      for
        contract <- contractResult
        whiteTicket <- ticketWithContractLevel(white, contract)
        blackTicket <- ticketWithContractLevel(black, contract)
        pairing <- Right(
          LevelBasedMatchmaking.PairingEngine.decide(
            whiteTicket,
            blackTicket,
            contract.map(_.stage).getOrElse(stage),
            uiConfirmedLevelContract
          )
        )
        persistence <-
          if !pairing.allowed then Left(pairing.reasons.mkString(", "))
          else
            val confirmation = pairing.confirmation.getOrElse(PairingConfirmation.fromTickets(whiteTicket, blackTicket))
            val request = GamePolicy.GamePolicyCreateRequest(
              gameId = gameId,
              mode = policyModeFor(white.ticket.poolKey.queue),
              rated = whiteTicket.rated && blackTicket.rated,
              timeControlBucket = white.ticket.poolKey.timeControl,
              white = PlayerPolicy(whiteTicket.playerId, SetLevel(whiteTicket.setLevel.value), white.ticket.poolKey.key),
              black = PlayerPolicy(blackTicket.playerId, SetLevel(blackTicket.setLevel.value), black.ticket.poolKey.key),
              versions = GamePolicy.PolicyVersions.current,
              featureFlags =
                Map(
                  "evenchess.playSearchPolicy" -> SearchStartService.policyVersion,
                  "evenchess.queue" -> white.ticket.poolKey.key
                ) ++
                  contract
                    .map(c =>
                      Map(
                        "evenchess.matchContractPolicy" -> c.policyVersion,
                        "evenchess.matchContractRequestId" -> c.requestId
                      )
                    )
                    .getOrElse(Map.empty),
              createdAt = now
            )

            for
              policyRecord <- GamePolicy.GamePolicyRecord.fromRequest(request, now).left.map(_.toString)
              persistedPolicy <- policyRepository.put(policyRecord).left.map(_.toString)
            yield
              val context = SearchTelemetryContext(
                schemaVersion = SearchStartService.telemetrySchemaVersion,
                occurredAt = now,
                pseudonymousUserId = s"${whiteTicket.playerId}:${blackTicket.playerId}"
              )
              GameStartPersistence(
                gameId = gameId,
                pairing = pairing,
                confirmation = confirmation,
                policyRecord = persistedPolicy,
                telemetry = LevelBasedMatchmaking.SearchTelemetry.gameStarted(gameId, whiteTicket, blackTicket, context)
              )
      yield persistence

  object CoachingRenderGate:
    def mayRender(gameId: String, policyRepository: GamePolicy.GamePolicyRepository): Boolean =
      policyRepository.requireEvenChess(gameId).isRight
