package lila.evenchess

import CoachingLadder.Level
import CoachingPolicy.{ AssistanceLedger, GameCompletionGuard, GameCompletionRatingState }
import DataModelsAndSeams.{ EvenChessGamePolicy, VersionStamp }
import EvenChessMode.{ ClientDisplayClaim, GameMode, ModeAuthority, ModeAuthorityDecision, PlayerSetLevels, ServerOwnedMetadata, SetLevel, TimeControlBucket }
import ProductInvariants.RequirementClass

object GamePolicy:

  enum PhaseCRequirement:
    case ServerOwnedModeMetadata
    case DedicatedPolicyPersistence
    case ClientClaimsDisplayOnly
    case NormalChessEffectsSuppressed
    case RatedCompletionRequiresAssistanceSummary
    case VersionedReplayablePolicy
    case LilaCoreIntegrationDeferredToThinSeams

  final case class PhaseCRequirementClassification(
      requirement: PhaseCRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseCRequirementClassifications:
    val all: List[PhaseCRequirementClassification] = List(
      PhaseCRequirementClassification(
        PhaseCRequirement.ServerOwnedModeMetadata,
        RequirementClass.EvenChessSpecific,
        "EvenChess games carry server-owned mode, player Set Levels, time-control, policy, ECR, and ledger metadata."
      ),
      PhaseCRequirementClassification(
        PhaseCRequirement.DedicatedPolicyPersistence,
        RequirementClass.EvenChessSpecific,
        "Persist EvenChess policy records in dedicated stores instead of broad core Lichess game fields."
      ),
      PhaseCRequirementClassification(
        PhaseCRequirement.ClientClaimsDisplayOnly,
        RequirementClass.EvenChessSpecific,
        "Client flags and display claims never decide whether a game is EvenChess."
      ),
      PhaseCRequirementClassification(
        PhaseCRequirement.NormalChessEffectsSuppressed,
        RequirementClass.LichessProvided,
        "Normal Lichess chess remains available internally and receives no EvenChess overlays, ECR, token, or ledger effects."
      ),
      PhaseCRequirementClassification(
        PhaseCRequirement.RatedCompletionRequiresAssistanceSummary,
        RequirementClass.EvenChessSpecific,
        "Rated EvenChess completion requires a computable assistance summary unless the game is no-rated or annulled."
      ),
      PhaseCRequirementClassification(
        PhaseCRequirement.VersionedReplayablePolicy,
        RequirementClass.EvenChessSpecific,
        "Fairness-affecting policy records carry schema, policy, ECR, and ledger versions so outcomes are replayable."
      ),
      PhaseCRequirementClassification(
        PhaseCRequirement.LilaCoreIntegrationDeferredToThinSeams,
        RequirementClass.AdaptedToLichessFork,
        "Later phases attach this policy at lila game-creation and live seams with narrow patch-mapped adapters."
      )
    )

  enum GamePolicyMode:
    case NormalRatedEvenChess
    case CasualEvenChess
    case TargetLevel
    case AiBotPractice

    def key: String =
      this match
        case NormalRatedEvenChess => "normal_rated_evenchess"
        case CasualEvenChess      => "casual_evenchess"
        case TargetLevel          => "target_level"
        case AiBotPractice        => "ai_bot_games"

  final case class PlayerPolicy(
      playerId: String,
      setLevel: SetLevel,
      poolKey: String
  ):
    def valid: Boolean =
      playerId.nonEmpty &&
        SetLevel.isValid(setLevel.value) &&
        poolKey.nonEmpty

  final case class PolicyVersions(
      schemaVersion: String,
      setLevelPolicyVersion: String,
      assistancePolicyVersion: String,
      ecrPolicyVersion: String,
      auditLedgerVersion: String
  ):
    def valid: Boolean =
      List(
        schemaVersion,
        setLevelPolicyVersion,
        assistancePolicyVersion,
        ecrPolicyVersion,
        auditLedgerVersion
      ).forall(_.nonEmpty)

    def asVersionStamp: VersionStamp =
      VersionStamp(
        schemaVersion = schemaVersion,
        policyVersion = Some(assistancePolicyVersion),
        modelVersion = Some(ecrPolicyVersion),
        configVersion = Some(setLevelPolicyVersion)
      )

  object PolicyVersions:
    val current: PolicyVersions =
      PolicyVersions(
        schemaVersion = "evenchess-game-policy-v1",
        setLevelPolicyVersion = "set-level-policy-v1",
        assistancePolicyVersion = "assistance-policy-v1",
        ecrPolicyVersion = "ecr-policy-v1",
        auditLedgerVersion = "assistance-ledger-v1"
      )

  final case class GamePolicyCreateRequest(
      gameId: String,
      mode: GamePolicyMode,
      rated: Boolean,
      timeControlBucket: TimeControlBucket,
      white: PlayerPolicy,
      black: PlayerPolicy,
      versions: PolicyVersions,
      featureFlags: Map[String, String],
      createdAt: Long
  ):
    def valid: Boolean =
      gameId.nonEmpty &&
        white.valid &&
        black.valid &&
        white.playerId != black.playerId &&
        versions.valid &&
        createdAt > 0 &&
        featureFlags.values.forall(_.nonEmpty)

  final case class GamePolicyRecord(
      gameId: String,
      mode: GamePolicyMode,
      rated: Boolean,
      timeControlBucket: TimeControlBucket,
      white: PlayerPolicy,
      black: PlayerPolicy,
      versions: PolicyVersions,
      featureFlags: Map[String, String],
      createdAt: Long,
      updatedAt: Long
  ):
    def valid: Boolean =
      GamePolicyCreateRequest(
        gameId,
        mode,
        rated,
        timeControlBucket,
        white,
        black,
        versions,
        featureFlags,
        createdAt
      ).valid && updatedAt >= createdAt

    def playerIds: Set[String] = Set(white.playerId, black.playerId)

    def setLevelFor(playerId: String): Option[SetLevel] =
      if playerId == white.playerId || playerId.equalsIgnoreCase(white.playerId) then Some(white.setLevel)
      else if playerId == black.playerId || playerId.equalsIgnoreCase(black.playerId) then Some(black.setLevel)
      else None

    def serverMetadata: ServerOwnedMetadata =
      ServerOwnedMetadata(
        mode = GameMode.EvenChess,
        rated = rated,
        playerModeKey = mode.key,
        timeControlBucket = timeControlBucket,
        setLevelPolicyVersion = versions.setLevelPolicyVersion,
        playerSetLevels = PlayerSetLevels(white.setLevel, black.setLevel),
        assistancePolicyVersion = versions.assistancePolicyVersion,
        ecrPolicyVersion = versions.ecrPolicyVersion,
        auditLedgerVersion = versions.auditLedgerVersion
      )

    def authorityDecision(clientClaim: ClientDisplayClaim): ModeAuthorityDecision =
      ModeAuthority.decide(Some(serverMetadata), clientClaim)

    def asDataModel: EvenChessGamePolicy =
      EvenChessGamePolicy(
        gameId = gameId,
        mode = mode.key,
        rated = rated,
        timeControl = timeControlBucket.toString,
        playerIds = playerIds,
        setLevelsByPlayer = Map(white.playerId -> white.setLevel.value, black.playerId -> black.setLevel.value),
        versions = versions.asVersionStamp,
        featureFlags = featureFlags
      )

    def mayCompleteWith(
        ledger: AssistanceLedger,
        ratingState: GameCompletionRatingState
    ): Boolean =
      if !rated then true
      else GameCompletionGuard.mayCompleteRatedEvenChess(ledger, ratingState)

  object GamePolicyRecord:
    def fromRequest(request: GamePolicyCreateRequest, now: Long): Either[PersistenceError, GamePolicyRecord] =
      val record = GamePolicyRecord(
        gameId = request.gameId,
        mode = request.mode,
        rated = request.rated,
        timeControlBucket = request.timeControlBucket,
        white = request.white,
        black = request.black,
        versions = request.versions,
        featureFlags = request.featureFlags,
        createdAt = request.createdAt,
        updatedAt = now
      )
      Either.cond(record.valid, record, PersistenceError.InvalidRecord)

  object NormalChessPolicy:
    def authorityDecision(clientClaim: ClientDisplayClaim): ModeAuthorityDecision =
      ModeAuthority.decide(None, clientClaim)

    def evenChessEffectsSuppressed(clientClaim: ClientDisplayClaim): Boolean =
      val decision = authorityDecision(clientClaim)
      !decision.isEvenChess &&
        !decision.displayAsAssisted &&
        !decision.mayRenderEvenChessOverlays &&
        !decision.mayUseEcrSystems &&
        !decision.mayConsumeEvenChessTokens &&
        !decision.mayWriteAssistanceLogs

  enum PersistenceError:
    case InvalidRecord
    case MissingGamePolicy
    case MissingPlayerPolicy
    case CompletionBlockedByMissingAssistanceSummary

  final case class StoredGamePolicy(
      record: GamePolicyRecord,
      auditEventIds: Vector[String],
      completedAt: Option[Long]
  ):
    def valid: Boolean =
      record.valid &&
        auditEventIds.forall(_.nonEmpty) &&
        completedAt.forall(_ >= record.createdAt)

    def appendAuditEvent(eventId: String, updatedAt: Long): Either[PersistenceError, StoredGamePolicy] =
      if eventId.isEmpty then Left(PersistenceError.InvalidRecord)
      else
        Right(
          copy(
            record = record.copy(updatedAt = updatedAt),
            auditEventIds = auditEventIds :+ eventId
          )
        )

    def complete(
        ledger: AssistanceLedger,
        ratingState: GameCompletionRatingState,
        completedAt: Long
    ): Either[PersistenceError, StoredGamePolicy] =
      if record.mayCompleteWith(ledger, ratingState) then
        Right(copy(record = record.copy(updatedAt = completedAt), completedAt = Some(completedAt)))
      else Left(PersistenceError.CompletionBlockedByMissingAssistanceSummary)

  trait GamePolicyRepository:
    def put(record: GamePolicyRecord): Either[PersistenceError, GamePolicyRecord]
    def get(gameId: String): Option[StoredGamePolicy]
    def requireEvenChess(gameId: String): Either[PersistenceError, GamePolicyRecord]
    def appendAuditEventRef(gameId: String, eventId: String, updatedAt: Long): Either[PersistenceError, StoredGamePolicy]
    def complete(gameId: String, ledger: AssistanceLedger, ratingState: GameCompletionRatingState, completedAt: Long): Either[PersistenceError, StoredGamePolicy]

  final class InMemoryGamePolicyRepository extends GamePolicyRepository:
    private var records = Map.empty[String, StoredGamePolicy]

    def put(record: GamePolicyRecord): Either[PersistenceError, GamePolicyRecord] =
      if !record.valid then Left(PersistenceError.InvalidRecord)
      else
        records = records.updated(record.gameId, StoredGamePolicy(record, Vector.empty, None))
        Right(record)

    def get(gameId: String): Option[StoredGamePolicy] =
      records.get(gameId)

    def requireEvenChess(gameId: String): Either[PersistenceError, GamePolicyRecord] =
      records.get(gameId).map(_.record).toRight(PersistenceError.MissingGamePolicy)

    def appendAuditEventRef(
        gameId: String,
        eventId: String,
        updatedAt: Long
    ): Either[PersistenceError, StoredGamePolicy] =
      records.get(gameId).toRight(PersistenceError.MissingGamePolicy).flatMap: stored =>
        stored.appendAuditEvent(eventId, updatedAt).map: next =>
          records = records.updated(gameId, next)
          next

    def complete(
        gameId: String,
        ledger: AssistanceLedger,
        ratingState: GameCompletionRatingState,
        completedAt: Long
    ): Either[PersistenceError, StoredGamePolicy] =
      records.get(gameId).toRight(PersistenceError.MissingGamePolicy).flatMap: stored =>
        stored.complete(ledger, ratingState, completedAt).map: next =>
          records = records.updated(gameId, next)
          next

  object Runtime:
    val gamePolicyRepository: GamePolicyRepository = new InMemoryGamePolicyRepository

  object PersistencePlan:
    val policyCollection = "evenchess_game_policy"
    val assistanceLedgerCollection = "evenchess_assistance_ledger"
    val broadCoreGameFieldsRequired = false
    val inspectLilaStorageConventionsBeforeMongoBinding = true

    val policyIndexes: List[String] = List(
      "_id gameId unique",
      "playerIds + createdAt",
      "mode + rated + timeControlBucket",
      "versions.schemaVersion"
    )

    val ledgerIndexes: List[String] = List(
      "gameId + playerId + ply + createdAt",
      "eventId unique",
      "schemaVersion + policyVersion"
    )

    def valid: Boolean =
      policyCollection.nonEmpty &&
        assistanceLedgerCollection.nonEmpty &&
        !broadCoreGameFieldsRequired &&
        inspectLilaStorageConventionsBeforeMongoBinding &&
        policyIndexes.nonEmpty &&
        ledgerIndexes.nonEmpty

  object PolicyService:
    def create(
        request: GamePolicyCreateRequest,
        now: Long,
        repository: GamePolicyRepository
    ): Either[PersistenceError, GamePolicyRecord] =
      GamePolicyRecord.fromRequest(request, now).flatMap(repository.put)

    def setLevelFor(
        gameId: String,
        playerId: String,
        repository: GamePolicyRepository
    ): Either[PersistenceError, Level] =
      repository
        .requireEvenChess(gameId)
        .flatMap(_.setLevelFor(playerId).toRight(PersistenceError.MissingPlayerPolicy))
        .map(level => Level(level.value))
