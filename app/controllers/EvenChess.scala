package controllers

import cats.mtl.Handle.*
import java.net.URI
import java.net.http.{ HttpClient, HttpRequest as JHttpRequest, HttpResponse }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.security.MessageDigest
import java.time.Duration
import scala.collection.mutable
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.blocking
import scala.util.Random

import chess.Clock
import chess.format.{ Fen, Uci }
import play.api.libs.json.{ JsArray, JsObject, JsValue, Json }
import scalalib.model.Days

import lila.app.*
import lila.common.{ Bus, LilaFuture }
import lila.core.game.Pov
import lila.core.id.{ GameId, GamePlayerId }
import lila.core.round.{ RoundBus, Tell }
import lila.core.userId.UserId
import lila.evenchess.AccountMonetisationUi
import lila.evenchess.AdminBackendSettings
import lila.evenchess.BotOperations
import lila.evenchess.CoachingLadder.Level
import lila.evenchess.CoachingOverlays.{ MockDisplayOverlayAtoms, Perspective }
import lila.evenchess.EceLiveBridge
import lila.evenchess.EngineGateway
import lila.evenchess.EvenChessMode
import lila.evenchess.EvenChessRatingCalibration
import lila.evenchess.GamePolicy
import lila.evenchess.LevelBasedMatchmaking
import lila.evenchess.LiveBoardIntegration
import lila.evenchess.PlaySearchIntegration
import lila.evenchess.PlaySearchIntegration.*
import lila.game.actorApi.MoveGameEvent

private object EvenChessManagedBotRuntime:

  private final case class BotKey(gameId: GameId, playerId: GamePlayerId)

  private final case class Entry(
      botUserId: String,
      playerId: GamePlayerId,
      color: chess.Color,
      profile: LevelBasedMatchmaking.BotMatchProfile,
      lastScheduledPly: Int
  )

  private val active = TrieMap.empty[BotKey, Entry]

  def register(
      game: lila.core.game.Game,
      botUserId: String,
      profile: LevelBasedMatchmaking.BotMatchProfile
  )(using Executor, Scheduler): Unit =
    Pov(game, UserId(botUserId)) match
      case Some(pov) =>
        val key = BotKey(game.id, pov.playerId)
        active
          .put(key, Entry(botUserId, pov.playerId, pov.color, profile, lastScheduledPly = -1))
          .foreach: previous =>
            Bus.pub(Tell(game.id, RoundBus.BotConnected(previous.color, v = false)))
        Bus.pub(Tell(game.id, RoundBus.BotConnected(pov.color, v = true)))
        lila
          .log("evenchess")
          .info(s"Registered managed bot runner game=${game.id} bot=$botUserId playerId=${pov.playerId} side=${pov.color.name}")
        Bus.subscribeFunDyn(MoveGameEvent.makeChan(game.id)):
          case MoveGameEvent(nextGame, _, _) if nextGame.id == game.id =>
            scheduleIfTurn(nextGame)
        scheduleIfTurn(game)
      case None =>
        lila
          .log("evenchess")
          .warn(s"Could not register managed bot runner; bot=$botUserId is not a player in game=${game.id}")

  private def scheduleIfTurn(game: lila.core.game.Game)(using Executor, Scheduler): Unit =
    active.filter(_._1.gameId == game.id).foreach: (key, entry) =>
      if game.finished then unregister(game.id, key, entry)
      else if game.player(game.turnColor).id == entry.playerId && entry.lastScheduledPly != game.ply.value then
        active.update(key, entry.copy(lastScheduledPly = game.ply.value))
        val random = Random(game.id.value.hashCode * 31 + game.ply.value * 97 + entry.profile.botId.hashCode)
        val delay = entry.profile.nextThinkDelay(random).max(300).min(7000).millis
        LilaFuture.delay(delay):
          if active.contains(key) then chooseMove(game, entry.profile).foreach: uci =>
            val promise = Promise[Unit]()
            promise.future.foreach: _ =>
              lila.log("evenchess").info(s"Managed bot move accepted game=${game.id} bot=${entry.botUserId} ply=${game.ply.value} uci=$uci")
            promise.future.failed.foreach: error =>
              lila
                .log("evenchess")
                .warn(s"Managed bot move rejected game=${game.id} bot=${entry.botUserId} ply=${game.ply.value} uci=$uci error=${error.getMessage}")
            lila.log("evenchess").info(s"Managed bot move dispatch game=${game.id} bot=${entry.botUserId} ply=${game.ply.value} uci=$uci delay=${delay.toMillis}")
            Bus.pub(Tell(game.id, RoundBus.BotPlay(entry.playerId, uci, Some(promise))))
          fuccess(())

  private def unregister(gameId: GameId, key: BotKey, entry: Entry): Unit =
    active.remove(key).foreach: _ =>
      Bus.pub(Tell(gameId, RoundBus.BotConnected(entry.color, v = false)))

  private def chooseMove(game: lila.core.game.Game, profile: LevelBasedMatchmaking.BotMatchProfile): Option[Uci] =
    val moves = game.position.legalMoves
    if moves.isEmpty then None
    else
      val random = Random(profile.botId.hashCode * 41 + game.id.value.hashCode + game.ply.value)
      val strength = profile.stockfishLevel.value
      val scored: List[(chess.Move, Int)] = moves.toList.map: move =>
        val centerBonus =
          if Set("d4", "e4", "d5", "e5", "c3", "f3", "c6", "f6").contains(move.dest.key) then 2 else 0
        val tacticalBonus =
          (if move.captures then 6 else 0) + (if move.promotes then 8 else 0)
        val developmentBonus =
          if game.ply < 12 && Set("b1", "g1", "b8", "g8", "c1", "f1", "c8", "f8").contains(move.orig.key) then 2 else 0
        move -> (tacticalBonus + centerBonus + developmentBonus + random.nextInt(math.max(1, 12 - strength)))
      val best = scored.map(_._2).max
      val tolerance = math.max(0, 9 - strength)
      val candidates = scored.collect { case (move, score) if score >= best - tolerance => move }
      Some(candidates(random.nextInt(candidates.size)).toUci)

final class EvenChess(env: Env) extends LilaController(env):

  private val searchRepository = PlaySearchIntegration.SearchRepositoryRuntime.local
  private val eceHttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
  private val defaultTestGroundFen = "rnbqkbnr/pppp1ppp/5n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
  private val assistanceLock = new Object
  private val boardOverlayCacheLock = new Object
  private val gamePolicyRepository = GamePolicy.Runtime.gamePolicyRepository
  private val boardOverlayCache = mutable.Map.empty[String, CachedBoardOverlay]
  private val proposedMoveCache = mutable.Map.empty[String, JsObject]
  private val positionEcsCache = mutable.Map.empty[String, JsObject]
  private val adminUnlimitedAssistanceQuota = 999_999
  private val reviewStoreRoot =
    sys.env
      .get("EVENCHESS_REVIEW_STORE_DIR")
      .orElse(sys.props.get("evenchess.review.store.dir"))
      .map(Paths.get(_))
      .getOrElse(Paths.get("data", "evenchess-review-store"))
  private val postGameReviewStore = PostGameReviewStore(reviewStoreRoot)
  private val nonLiveAskAiLedger = NonLiveAskAiLedger(reviewStoreRoot)
  private val postGameReviewJobs = TrieMap.empty[String, Boolean]

  private def establishedRosterBotPerf(account: String, targetRating: Option[Int]): lila.core.perf.Perf =
    val rating = BotOperations.BotAccountRoster.establishedDisplayRating(account, targetRating)
    lila.core.perf.Perf(
      glicko = chess.rating.glicko.Glicko(rating.toDouble, 60d, lila.rating.Glicko.defaultVolatility),
      nb = 30,
      recent = List.fill(12)(chess.IntRating(rating)),
      latest = Some(nowInstant)
    )
  private val proposedTurnToMove = mutable.Map.empty[String, String]
  private val potentialMoveCache = mutable.Map.empty[String, JsObject]
  private val searchStateLock = new Object
  private val searchRedirectLedger = new PlaySearchIntegration.PublicSearchRedirectLedger
  private val matchedGameFailureLogAt = mutable.Map.empty[String, Long]
  private val simulationBotGameCreationInFlight = mutable.Set.empty[String]
  private val simulationBotGameCreationCompleted = mutable.Set.empty[String]
  private val simulationBotGamePumpMinIntervalMillis = 15_000L
  private var simulationBotGamePumpRevision = 0L
  private var lastSimulationBotGamePumpAt = 0L

  private final case class ParsedEceSidePayload(
      output: EngineGateway.EceSideOutput,
      atoms: MockDisplayOverlayAtoms,
      extras: ParsedEceDisplayExtras,
      auditId: String,
      fieldsDetected: JsObject
  )

  private final case class ParsedEceDisplayExtras(
      cards: List[EceLiveBridge.ExtraCard],
      visuals: List[EceLiveBridge.ExtraVisual]
  )

  private final case class CachedBoardOverlay(
      payload: JsObject,
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      fen: String,
      level: Level,
      ply: Int,
      cachedAt: Long
  )

  private final case class PositionEcsUsage(
      level: Level,
      interval: Int,
      ownMoves: Int,
      accrued: Int,
      consumed: Int,
      adminUnlimitedTokens: Boolean
  ):
    def quota: Int = if adminUnlimitedTokens then adminUnlimitedAssistanceQuota else accrued
    def available: Int = if adminUnlimitedTokens then adminUnlimitedAssistanceQuota else math.max(0, accrued - consumed)

  private final case class FullGameReviewFrameInput(
      ply: Int,
      fen: String,
      moveUci: Option[String]
  ):
    def valid: Boolean =
      ply >= 0 &&
        fen.nonEmpty &&
        moveUci.forall(_.matches("[a-h][1-8][a-h][1-8][qrbn]?"))

  private final case class StoredReviewFrame(
      ply: Int,
      fen: String,
      side: Perspective,
      level: Level,
      source: String,
      payload: JsObject
  )

  private final case class NonLiveAskAiSpendResult(
      allowed: Boolean,
      source: String,
      quota: JsObject,
      finalized: Boolean
  )

  private final class PostGameReviewStore(root: Path):
    private val lock = new Object
    private val cache = mutable.Map.empty[String, JsObject]
    Files.createDirectories(root)

    def get(ownerUserId: String, gameId: String): JsObject = lock.synchronized:
      load(ownerUserId, gameId)

    def putFullMatch(
        ownerUserId: String,
        gameId: String,
        reviewLevel: Level,
        fullMatch: JsObject,
        frames: List[StoredReviewFrame],
        fenHash: String,
        eceVersion: String,
        policyVersion: String
    ): JsObject = lock.synchronized:
      val doc = load(ownerUserId, gameId)
      val mergedFrames = mergeFrames(arrayObjects(doc, "frames"), frames.map(frameToJson))
      val next =
        doc ++ Json.obj(
          "schema" -> "evenchess-review-store-v1",
          "ownerUserId" -> ownerUserId,
          "gameId" -> gameId,
          "reviewLevel" -> reviewLevel.value,
          "eceVersion" -> eceVersion,
          "policyVersion" -> policyVersion,
          "fenHash" -> fenHash,
          "ecemfStatus" -> "ready",
          "evenchessFullGame" -> fullMatch,
          "frames" -> mergedFrames,
          "updatedAt" -> System.currentTimeMillis
        )
      save(ownerUserId, gameId, next)
      next

    def framesAtPly(ownerUserId: String, gameId: String, ply: Int, fen: Option[String]): List[JsObject] =
      arrayObjects(get(ownerUserId, gameId), "frames")
        .filter(frame => intField(frame, "ply").contains(ply))
        .filter(frame => fen.forall(value => stringField(frame, "fen").contains(value)))

    def frameFor(
        ownerUserId: String,
        gameId: String,
        ply: Int,
        fen: Option[String],
        side: Perspective
    ): Option[JsObject] =
      framesAtPly(ownerUserId, gameId, ply, fen)
        .find(frame => stringField(frame, "side").contains(perspectiveKey(side)))

    def putMatchSummary(ownerUserId: String, gameId: String, side: Perspective, summary: JsObject): JsObject = lock.synchronized:
      val doc = load(ownerUserId, gameId)
      val summaries = objectField(doc, "matchSummaries").getOrElse(Json.obj())
      val next =
        doc ++ Json.obj(
          "matchSummaries" -> (summaries ++ Json.obj(perspectiveKey(side) -> summary)),
          "updatedAt" -> System.currentTimeMillis
        )
      save(ownerUserId, gameId, next)
      next

    def putAskAi(
        ownerUserId: String,
        gameId: String,
        ply: Int,
        fen: String,
        side: Perspective,
        payload: JsObject
    ): JsObject = lock.synchronized:
      val doc = load(ownerUserId, gameId)
      val targetKey = frameIdentity(ply, fen, perspectiveKey(side))
      val updatedFrames =
        arrayObjects(doc, "frames").map: frame =>
          if frameKey(frame) == targetKey then frame ++ Json.obj("askAi" -> payload, "updatedAt" -> System.currentTimeMillis)
          else frame
      val askAiEntries =
        arrayObjects(doc, "nonLiveAskAi") :+ Json.obj(
          "ply" -> ply,
          "fen" -> fen,
          "side" -> perspectiveKey(side),
          "payload" -> payload,
          "createdAt" -> System.currentTimeMillis
        )
      val next =
        doc ++ Json.obj(
          "frames" -> JsArray(updatedFrames),
          "nonLiveAskAi" -> JsArray(askAiEntries.takeRight(500)),
          "updatedAt" -> System.currentTimeMillis
        )
      save(ownerUserId, gameId, next)
      next

    private def load(ownerUserId: String, gameId: String): JsObject =
      val id = key(ownerUserId, gameId)
      cache.getOrElseUpdate(
        id,
        {
          val path = file(ownerUserId, gameId)
          if Files.exists(path) then
            try Json.parse(Files.readString(path, StandardCharsets.UTF_8)).asOpt[JsObject].getOrElse(empty(ownerUserId, gameId))
            catch case _: Exception => empty(ownerUserId, gameId)
          else empty(ownerUserId, gameId)
        }
      )

    private def save(ownerUserId: String, gameId: String, doc: JsObject): Unit =
      val path = file(ownerUserId, gameId)
      Files.createDirectories(path.getParent)
      Files.writeString(path, Json.prettyPrint(doc), StandardCharsets.UTF_8)
      cache.update(key(ownerUserId, gameId), doc)

    private def empty(ownerUserId: String, gameId: String): JsObject =
      Json.obj(
        "schema" -> "evenchess-review-store-v1",
        "ownerUserId" -> ownerUserId,
        "gameId" -> gameId,
        "reviewLevel" -> 10,
        "ecemfStatus" -> "missing",
        "frames" -> Json.arr(),
        "matchSummaries" -> Json.obj(),
        "nonLiveAskAi" -> Json.arr(),
        "createdAt" -> System.currentTimeMillis
      )

    private def frameToJson(frame: StoredReviewFrame): JsObject =
      Json.obj(
        "ply" -> frame.ply,
        "fen" -> frame.fen,
        "fenHash" -> sha256Hex(frame.fen).take(24),
        "side" -> perspectiveKey(frame.side),
        "level" -> frame.level.value,
        "source" -> frame.source,
        "payload" -> frame.payload,
        "createdAt" -> System.currentTimeMillis
      )

    private def mergeFrames(existing: List[JsObject], incoming: List[JsObject]): JsArray =
      val byKey = mutable.LinkedHashMap.empty[String, JsObject]
      existing.foreach(frame => byKey.update(frameKey(frame), frame))
      incoming.foreach(frame => byKey.update(frameKey(frame), frame))
      JsArray(byKey.values.toSeq.sortBy(frame => (intField(frame, "ply").getOrElse(0), stringField(frame, "side").getOrElse(""))))

    private def frameKey(frame: JsObject): String =
      frameIdentity(
        intField(frame, "ply").getOrElse(0),
        stringField(frame, "fen").getOrElse(""),
        stringField(frame, "side").getOrElse("")
      )

    private def frameIdentity(ply: Int, fen: String, side: String): String = s"$ply:$side:$fen"

    private def arrayObjects(json: JsValue, key: String): List[JsObject] =
      arrayField(json, key).collect { case obj: JsObject => obj }

    private def file(ownerUserId: String, gameId: String): Path =
      root.resolve("reviews").resolve(s"${safeFilePart(ownerUserId)}__${safeFilePart(gameId)}.json")

    private def key(ownerUserId: String, gameId: String): String = s"$ownerUserId:$gameId"

    private def safeFilePart(value: String): String =
      value.replaceAll("[^A-Za-z0-9_.-]", "_").take(120).nn

  private final class NonLiveAskAiLedger(root: Path):
    private val lock = new Object
    private val cache = mutable.Map.empty[String, JsObject]
    Files.createDirectories(root.resolve("quotas"))

    def quota(accountId: String, tier: String, now: Long, adminUnlimited: Boolean): JsObject = lock.synchronized:
      val (_, normalized) = normalize(accountId, tier, now, adminUnlimited)
      quotaJson(normalized, adminUnlimited)

    def grantAdCredits(accountId: String, now: Long): JsObject = lock.synchronized:
      val (doc, _) = normalize(accountId, "standard", now, adminUnlimited = false)
      val credits = arrayObjects(doc, "adCredits") :+ Json.obj(
        "remaining" -> 5,
        "expiresAt" -> (now + 30L * 24L * 60L * 60L * 1000L),
        "createdAt" -> now
      )
      val next = doc ++ Json.obj("adCredits" -> JsArray(credits), "updatedAt" -> now)
      save(accountId, next)
      quotaJson(next, adminUnlimited = false)

    def reserve(accountId: String, tier: String, now: Long, adminUnlimited: Boolean): NonLiveAskAiSpendResult = lock.synchronized:
      val (doc, normalized) = normalize(accountId, tier, now, adminUnlimited)
      if adminUnlimited then NonLiveAskAiSpendResult(true, "admin_unlimited", quotaJson(doc, adminUnlimited = true), finalized = true)
      else if dailyRemaining(normalized) > 0 then
        val next = normalized ++ Json.obj("dailyUsed" -> (intField(normalized, "dailyUsed").getOrElse(0) + 1), "updatedAt" -> now)
        save(accountId, next)
        NonLiveAskAiSpendResult(true, "daily", quotaJson(next, adminUnlimited = false), finalized = true)
      else
        val credits = arrayObjects(normalized, "adCredits")
        val index = credits.indexWhere(credit => intField(credit, "remaining").exists(_ > 0))
        if index >= 0 then
          val updatedCredits = credits.zipWithIndex.map:
            case (credit, idx) if idx == index =>
              credit ++ Json.obj("remaining" -> math.max(0, intField(credit, "remaining").getOrElse(0) - 1))
            case (credit, _) => credit
          val next = normalized ++ Json.obj("adCredits" -> JsArray(updatedCredits), "updatedAt" -> now)
          save(accountId, next)
          NonLiveAskAiSpendResult(true, "ad_credit", quotaJson(next, adminUnlimited = false), finalized = true)
        else NonLiveAskAiSpendResult(false, "none", quotaJson(normalized, adminUnlimited = false), finalized = false)

    def refund(accountId: String, source: String, now: Long): JsObject = lock.synchronized:
      if source == "admin_unlimited" || source == "none" then quota(accountId, "standard", now, adminUnlimited = source == "admin_unlimited")
      else
        val (_, normalized) = normalize(accountId, "standard", now, adminUnlimited = false)
        val next =
          if source == "daily" then
            normalized ++ Json.obj("dailyUsed" -> math.max(0, intField(normalized, "dailyUsed").getOrElse(0) - 1), "updatedAt" -> now)
          else
            val credits = arrayObjects(normalized, "adCredits")
            val updatedCredits =
              credits.headOption match
                case Some(first) => (first ++ Json.obj("remaining" -> (intField(first, "remaining").getOrElse(0) + 1))) :: credits.drop(1)
                case None =>
                  Json.obj("remaining" -> 1, "expiresAt" -> (now + 30L * 24L * 60L * 60L * 1000L), "createdAt" -> now) :: Nil
            normalized ++ Json.obj("adCredits" -> JsArray(updatedCredits), "updatedAt" -> now)
        save(accountId, next)
        quotaJson(next, adminUnlimited = false)

    private def normalize(accountId: String, tier: String, now: Long, adminUnlimited: Boolean): (JsObject, JsObject) =
      val doc = load(accountId)
      val resetAt = longField(doc, "dailyResetAt").getOrElse(0L)
      val limit = dailyLimit(tier)
      val normalizedDaily =
        if resetAt <= now then doc ++ Json.obj("dailyLimit" -> limit, "dailyUsed" -> 0, "dailyResetAt" -> (now + 24L * 60L * 60L * 1000L))
        else doc ++ Json.obj("dailyLimit" -> limit)
      val activeCredits =
        arrayObjects(normalizedDaily, "adCredits")
          .filter(credit => longField(credit, "expiresAt").exists(_ > now) && intField(credit, "remaining").exists(_ > 0))
          .sortBy(credit => longField(credit, "expiresAt").getOrElse(Long.MaxValue))
      val normalized = normalizedDaily ++ Json.obj(
        "accountId" -> accountId,
        "tier" -> tier,
        "adCredits" -> JsArray(activeCredits),
        "adminUnlimited" -> adminUnlimited
      )
      save(accountId, normalized)
      (doc, normalized)

    private def quotaJson(doc: JsObject, adminUnlimited: Boolean): JsObject =
      val dailyLimitValue = intField(doc, "dailyLimit").getOrElse(5)
      val dailyUsed = intField(doc, "dailyUsed").getOrElse(0)
      val adRemaining = arrayObjects(doc, "adCredits").flatMap(credit => intField(credit, "remaining")).sum
      Json.obj(
        "bucket" -> "non_live_ask_ai",
        "dailyLimit" -> dailyLimitValue,
        "dailyRemaining" -> (if adminUnlimited then adminUnlimitedAssistanceQuota else math.max(0, dailyLimitValue - dailyUsed)),
        "adRemaining" -> (if adminUnlimited then adminUnlimitedAssistanceQuota else adRemaining),
        "available" -> (if adminUnlimited then adminUnlimitedAssistanceQuota else math.max(0, dailyLimitValue - dailyUsed) + adRemaining),
        "adminUnlimited" -> adminUnlimited,
        "resetAt" -> longField(doc, "dailyResetAt")
      )

    private def dailyRemaining(doc: JsObject): Int =
      math.max(0, intField(doc, "dailyLimit").getOrElse(5) - intField(doc, "dailyUsed").getOrElse(0))

    private def dailyLimit(tier: String): Int =
      tier match
        case "premium" => 10
        case _         => 5

    private def load(accountId: String): JsObject =
      cache.getOrElseUpdate(
        accountId,
        {
          val path = file(accountId)
          if Files.exists(path) then
            try Json.parse(Files.readString(path, StandardCharsets.UTF_8)).asOpt[JsObject].getOrElse(empty(accountId))
            catch case _: Exception => empty(accountId)
          else empty(accountId)
        }
      )

    private def save(accountId: String, doc: JsObject): Unit =
      val path = file(accountId)
      Files.createDirectories(path.getParent)
      Files.writeString(path, Json.prettyPrint(doc), StandardCharsets.UTF_8)
      cache.update(accountId, doc)

    private def empty(accountId: String): JsObject =
      Json.obj(
        "schema" -> "evenchess-non-live-ask-ai-quota-v1",
        "accountId" -> accountId,
        "tier" -> "standard",
        "dailyLimit" -> 5,
        "dailyUsed" -> 0,
        "dailyResetAt" -> 0L,
        "adCredits" -> Json.arr()
      )

    private def arrayObjects(json: JsValue, key: String): List[JsObject] =
      arrayField(json, key).collect { case obj: JsObject => obj }

    private def file(accountId: String): Path =
      root.resolve("quotas").resolve(s"${accountId.replaceAll("[^A-Za-z0-9_.-]", "_").take(120)}.json")

  private final case class ParsedEcePositionContext(
      positionEcsId: Option[String],
      status: Option[String],
      expiresAtMs: Option[Long],
      endpoint: Option[String]
  )

  private final case class ParsedEceBoardPayload(
      response: EngineGateway.EceBoardStateResponse,
      white: ParsedEceSidePayload,
      black: ParsedEceSidePayload,
      fieldsDetected: JsObject,
      positionContext: Option[ParsedEcePositionContext],
      phase: String
  )

  def play = Open:
    val hash = get("mode") match
      case Some("ai") => "#ai"
      case _          => "#hook"
    Redirect(s"/$hash")

  def search = Auth { ctx ?=> me ?=>
    Ok.page(views.evenchess.play(searchModel(me.userId.value)))
  }

  def searchJson = Auth { ctx ?=> me ?=>
    val playerId = me.userId.value
    publicSearchKeyFromQuery.flatMap(searchRedirectLedger.redirectForPublicSearchKey(_, playerId)) match
      case Some(redirect) =>
        Future.successful(JsonOk(matchedSearchRedirectPayload(redirect.searchKey, redirect.url, botModeConfigFromSettings)))
      case None =>
        val model = searchModel(playerId)
        maybeCreateMatchedGameRedirect(model).map: redirectUrl =>
          maybeCreateSimulationBotGamesFromSettings()
          JsonOk(searchJsonPayload(model, redirectUrl))
  }

  def account = Auth { ctx ?=> me ?=>
    val dashboard = AccountMonetisationUi.AccountDashboard.forLichessUser(me.username.value, System.currentTimeMillis)
    Ok.page(views.evenchess.account(AccountPageModel(dashboard)))
  }

  def reviewState(rawGameId: String) = Auth { ctx ?=> me ?=>
    val gameId = GameId.take(rawGameId)
    Found(env.game.gameRepo.game(gameId)): game =>
      val ownerId = me.userId.value
      val ply = get("ply").flatMap(_.toIntOption).filter(_ >= 0).getOrElse(game.ply.value)
      val fen = get("fen").filter(_.nonEmpty)
      val viewerSide =
        get("side")
          .flatMap(parsePerspective)
          .orElse(Pov(game, me.userId).map(pov => perspectiveFromColor(pov.color)))
          .getOrElse(Perspective.White)
      val adminUnlimited = adminUnlimitedTokensEnabled
      val tier = reviewPlanTierKey(adminUnlimited)
      Future(blocking(reviewStatePayload(gameId.value, ownerId, ply, fen, viewerSide, tier, adminUnlimited))).map(JsonOk(_))
  }

  def reviewEcemf(rawGameId: String) = AuthBody(parse.json) { ctx ?=> me ?=>
    val gameId = GameId.take(rawGameId)
    Found(env.game.gameRepo.game(gameId)): game =>
      val body = ctx.body.body
      val ownerId = me.userId.value
      val level = levelField(body, "level").getOrElse(Level(10))
      val frames = parseFullGameReviewFrameInputs(body)
      val jobKey = s"$ownerId:${gameId.value}:ecemf"
      val result = stringField(body, "result").getOrElse("unknown")
      val termination = stringField(body, "termination").getOrElse(if game.finished then "completed" else "ongoing")

      if !game.finished then BadRequest(Json.obj("ok" -> false, "error" -> "review_ecemf_requires_finished_game")).toFuccess
      else if level.value != 10 then BadRequest(Json.obj("ok" -> false, "error" -> "review_ecemf_requires_level_10")).toFuccess
      else if frames.isEmpty || frames.exists(!_.valid) then
        BadRequest(Json.obj("ok" -> false, "error" -> "invalid_review_ecemf_frames")).toFuccess
      else if postGameReviewJobs.putIfAbsent(jobKey, true).isDefined then
        Conflict(Json.obj("ok" -> false, "error" -> "review_ecemf_job_in_flight")).toFuccess
      else
        Future(blocking(runPostGameReviewGeneration(siteEceConfigs, gameId.value, ownerId, frames, level, result, termination)))
          .andThen { case _ => postGameReviewJobs.remove(jobKey) }
          .map:
            case Right(payload) => JsonOk(payload)
            case Left(payload)  => ServiceUnavailable(payload)
  }

  def reviewMatchSummary(rawGameId: String) = AuthBody(parse.json) { ctx ?=> me ?=>
    val gameId = GameId.take(rawGameId)
    Found(env.game.gameRepo.game(gameId)): game =>
      val body = ctx.body.body
      val ownerId = me.userId.value
      val viewerSide =
        stringField(body, "side")
          .flatMap(parsePerspective)
          .orElse(Pov(game, me.userId).map(pov => perspectiveFromColor(pov.color)))
          .getOrElse(Perspective.White)
      val stored = postGameReviewStore.get(ownerId, gameId.value)
      val maybeFullMatch = objectField(stored, "evenchessFullGame")
      val adminUnlimited = adminUnlimitedTokensEnabled

      maybeFullMatch match
        case None =>
          BadRequest(Json.obj("ok" -> false, "error" -> "review_ecemf_required_before_match_summary")).toFuccess
        case Some(fullMatch) if hasForbiddenEcePublicField(fullMatch) =>
          BadRequest(Json.obj("ok" -> false, "error" -> "stored_full_match_contains_forbidden_public_field")).toFuccess
        case Some(fullMatch) =>
          val now = System.currentTimeMillis
          val entitlement = AccountMonetisationUi.EntitlementSource.onboardingForLichessUser(ownerId, now)
          val summaryQuota =
            lila.evenchess.SubscriptionTokensAds.SummaryQuotaService.consume(
              entitlement,
              lila.evenchess.AiCoachPolicy.SummaryType.Match,
              now
            )
          if !adminUnlimited && !summaryQuota.allowed then
            Forbidden(Json.obj("ok" -> false, "error" -> "match_summary_quota_unavailable", "reason" -> summaryQuota.reason)).toFuccess
          else
            val request = eceFullMatchSummaryRequestJson(s"${gameId.value}-$ownerId-match-summary", ownerId, viewerSide, useAi = true, fullMatch)
            Future(blocking(callEceFullMatchSummaryFirst(siteEceConfigs, request))).map:
              case Right(summaryJson) if !hasForbiddenEcePublicField(summaryJson) =>
                postGameReviewStore.putMatchSummary(ownerId, gameId.value, viewerSide, summaryJson.asOpt[JsObject].getOrElse(Json.obj("summary" -> summaryJson)))
                JsonOk(
                  Json.obj(
                    "ok" -> true,
                    "gameId" -> gameId.value,
                    "side" -> perspectiveKey(viewerSide),
                    "summary" -> summaryJson,
                    "quota" -> Json.obj("source" -> (if adminUnlimited then "admin_unlimited" else summaryQuota.reason))
                  )
                )
              case Right(_) =>
                BadRequest(Json.obj("ok" -> false, "error" -> "match_summary_contains_forbidden_public_field"))
              case Left(error) =>
                ServiceUnavailable(Json.obj("ok" -> false, "error" -> "ece_full_match_summary_unavailable", "message" -> safeEceErrorMessage(error, "ECE full-match summary unavailable")))
  }

  def reviewNonLiveAskAi(rawGameId: String) = AuthBody(parse.json) { ctx ?=> me ?=>
    val gameId = GameId.take(rawGameId)
    Found(env.game.gameRepo.game(gameId)): game =>
      val body = ctx.body.body
      val ownerId = me.userId.value
      val ply = intField(body, "ply").filter(_ >= 0).getOrElse(game.ply.value)
      val fenFromBody = stringField(body, "fen").filter(_.nonEmpty)
      val viewerSide =
        stringField(body, "viewerSide")
          .orElse(stringField(body, "side"))
          .flatMap(parsePerspective)
          .orElse(Pov(game, me.userId).map(pov => perspectiveFromColor(pov.color)))
          .getOrElse(Perspective.White)
      val displaySide =
        fenFromBody
          .flatMap(sideToMoveFromFen)
          .orElse(stringField(body, "displaySide").flatMap(parsePerspective))
          .getOrElse(viewerSide)
      val frame =
        postGameReviewStore
          .frameFor(ownerId, gameId.value, ply, fenFromBody, displaySide)
          .orElse(postGameReviewStore.frameFor(ownerId, gameId.value, ply, None, displaySide))
      val adminUnlimited = adminUnlimitedTokensEnabled
      val tier = reviewPlanTierKey(adminUnlimited)
      val now = System.currentTimeMillis

      frame match
        case None =>
          BadRequest(Json.obj("ok" -> false, "error" -> "review_frame_missing", "message" -> "Generate ECEMF before using non-live Ask AI.")).toFuccess
        case Some(storedFrame) =>
          val fen = stringField(storedFrame, "fen").getOrElse(fenFromBody.getOrElse(""))
          val side = stringField(storedFrame, "side").flatMap(parsePerspective).getOrElse(displaySide)
          val level = levelField(storedFrame, "level").getOrElse(Level(10))
          val spend = nonLiveAskAiLedger.reserve(ownerId, tier, now, adminUnlimited)
          if !spend.allowed then
            Forbidden(Json.obj("ok" -> false, "error" -> "non_live_ask_ai_quota_unavailable", "quota" -> spend.quota)).toFuccess
          else if fen.isEmpty then
            val quota = nonLiveAskAiLedger.refund(ownerId, spend.source, now)
            BadRequest(Json.obj("ok" -> false, "error" -> "review_frame_missing_fen", "quota" -> quota)).toFuccess
          else
            val request =
              EngineGateway.EcePositionEcsRequest.positionEcs(
                gameId = gameId.value,
                ply = ply,
                inputFen = fen,
                userSide = side,
                whiteEcr = None,
                blackEcr = None,
                whiteLevel = Level(10),
                blackLevel = Level(10),
                positionEcsId = None
              )
            val key = positionEcsCacheKey(gameId.value, side, ply, fen, level)
            Future(blocking(callEcePositionEcsFirst(siteEceConfigs, request))).map:
              case Right((config, json)) =>
                parseEcePositionEcsPayload(config, json, request, gameId.value, ownerId, ply, fen, side, key) match
                  case Right(payload) if approvedPositionEcsPayload(payload) =>
                    postGameReviewStore.putAskAi(ownerId, gameId.value, ply, fen, side, payload)
                    JsonOk(
                      Json.obj(
                        "ok" -> true,
                        "gameId" -> gameId.value,
                        "ply" -> ply,
                        "side" -> perspectiveKey(side),
                        "askAi" -> payload,
                        "quota" -> nonLiveAskAiLedger.quota(ownerId, tier, System.currentTimeMillis, adminUnlimited),
                        "stored" -> true
                      )
                    )
                  case Right(payload) =>
                    val quota = nonLiveAskAiLedger.refund(ownerId, spend.source, System.currentTimeMillis)
                    BadRequest(payload ++ Json.obj("quota" -> quota))
                  case Left(error) =>
                    val quota = nonLiveAskAiLedger.refund(ownerId, spend.source, System.currentTimeMillis)
                    BadRequest(Json.obj("ok" -> false, "error" -> "position_ecs_payload_not_approved", "message" -> safeEceErrorMessage(error, "ECE Position ECS payload not approved"), "quota" -> quota))
              case Left(error) =>
                val quota = nonLiveAskAiLedger.refund(ownerId, spend.source, System.currentTimeMillis)
                ServiceUnavailable(Json.obj("ok" -> false, "error" -> "ece_position_unavailable", "message" -> safeEceErrorMessage(error, "ECE position unavailable"), "quota" -> quota))
  }

  def reviewNonLiveAskAiAdGrant = Auth { ctx ?=> me ?=>
    JsonOk(
      Json.obj(
        "ok" -> true,
        "quota" -> nonLiveAskAiLedger.grantAdCredits(me.userId.value, System.currentTimeMillis)
      )
    )
  }

  def recordUsedLevel = Auth { ctx ?=> me ?=>
    val gameId = get("gameId").filter(value => value.matches("[A-Za-z0-9_.:-]{1,80}"))
    val playerId = me.userId.value
    val requestedLevel =
      get("level")
        .flatMap(_.toIntOption)
        .filter(Level.isValid)
        .map(Level(_))

    (gameId, requestedLevel) match
      case (Some(id), Some(level)) =>
        GamePolicy.PolicyService.recordUsedLevel(
          gameId = id,
          playerId = playerId,
          requestedLevel = level,
          now = System.currentTimeMillis,
          repository = gamePolicyRepository
        ) match
          case Right(usedLevel) =>
            JsonOk(
              Json.obj(
                "ok" -> true,
                "gameId" -> id,
                "usedLevel" -> usedLevel.value
              )
            )
          case Left(GamePolicy.PersistenceError.MissingGamePolicy) =>
            NotFound(Json.obj("ok" -> false, "error" -> "missing_evenchess_game_policy"))
          case Left(GamePolicy.PersistenceError.MissingPlayerPolicy) =>
            Forbidden(Json.obj("ok" -> false, "error" -> "missing_evenchess_player_policy"))
          case Left(error) =>
            BadRequest(Json.obj("ok" -> false, "error" -> error.toString))
      case _ =>
        BadRequest(Json.obj("ok" -> false, "error" -> "invalid_used_level_request"))
  }

  def recordDisplayState = AuthBody(parse.json) { ctx ?=> me ?=>
    val body = ctx.body.body
    val gameId = stringField(body, "gameId").filter(value => value.matches("[A-Za-z0-9_.:-]{1,80}"))
    val playerId = me.userId.value
    val requestedUsedLevel = levelField(body, "usedLevel")
    val requestedState = parseDisplayStateInput(body)

    (gameId, requestedState) match
      case (Some(id), Some(state)) =>
        val now = System.currentTimeMillis
        val repository = gamePolicyRepository
        val usedLevelResult =
          requestedUsedLevel match
            case Some(level) => GamePolicy.PolicyService.recordUsedLevel(id, playerId, level, now, repository).map(Some(_))
            case None        => Right(None)
        val result =
          usedLevelResult.flatMap: maybeUsedLevel =>
            GamePolicy.PolicyService.recordDisplayState(id, playerId, state, now, repository).map: storedState =>
              val currentUsedLevel =
                maybeUsedLevel
                  .orElse(repository.get(id).flatMap(_.usedLevelFor(playerId)))
                  .map(_.value)
                  .getOrElse(storedState.appliedLevel.value)
              Json.obj(
                "ok" -> true,
                "gameId" -> id,
                "usedLevel" -> currentUsedLevel,
                "display" -> displayStateJson(storedState)
              )
        result match
          case Right(payload) => JsonOk(payload)
          case Left(GamePolicy.PersistenceError.MissingGamePolicy) =>
            NotFound(Json.obj("ok" -> false, "error" -> "missing_evenchess_game_policy"))
          case Left(GamePolicy.PersistenceError.MissingPlayerPolicy) =>
            Forbidden(Json.obj("ok" -> false, "error" -> "missing_player_policy"))
          case Left(error) =>
            BadRequest(Json.obj("ok" -> false, "error" -> error.toString))
      case _ =>
        BadRequest(Json.obj("ok" -> false, "error" -> "invalid_display_state_request"))
  }

  def eceBoardOverlay = Auth { ctx ?=> me ?=>
    val fen = get("fen").filter(_.nonEmpty).getOrElse(defaultTestGroundFen)
    val surface = get("surface").filter(value => value.matches("[A-Za-z0-9_-]{1,40}")).getOrElse("board")
    val gameId =
      get("gameId")
        .filter(value => value.matches("[A-Za-z0-9_.:-]{1,80}"))
        .getOrElse(s"non-live-$surface-${math.abs(fen.hashCode)}")
    val playerId = me.userId.value
    val ply = get("ply").flatMap(_.toIntOption).filter(_ >= 0).getOrElse(0)
    val requesterSide =
      get("side").flatMap(parsePerspective).getOrElse(sideToMoveFromFen(fen).getOrElse(Perspective.White))
    val level = Level(10)
    val ttlMillis = get("ttlMillis").flatMap(_.toIntOption).filter(_ > 0).getOrElse(60_000)
    val adminUnlimited = adminUnlimitedTokensEnabled
    val request =
      EngineGateway.EceBoardStateRequest.boardState(
        gameId = gameId,
        ply = ply,
        inputFen = fen,
        whiteEcr = None,
        blackEcr = None,
        whiteLevel = level,
        blackLevel = level,
        aiTextAllowed = false
      )

    storedReviewBoardOverlay(surface, gameId, playerId, requesterSide, fen, ply, level, ttlMillis, adminUnlimited) match
      case Some(payload) => Future.successful(JsonOk(payload ++ Json.obj("surface" -> surface, "setLevel" -> level.value, "source" -> "stored_ecemf")))
      case None =>
        cachedBoardOverlay(gameId, playerId, requesterSide, fen, ply, level, ttlMillis, adminUnlimited) match
          case Some(payload) => Future.successful(JsonOk(payload ++ Json.obj("surface" -> surface, "setLevel" -> level.value)))
          case None =>
            Future(blocking(siteEceBoardOverlayPayload(gameId, playerId, ply, fen, requesterSide, request, ttlMillis))).map:
              case Right(payload) =>
                val sanitized = hidePotentialMovePayload(payload, gameId, requesterSide, level, ply, adminUnlimited)
                JsonOk(
                  rememberBoardOverlay(gameId, playerId, requesterSide, fen, level, ply, sanitized) ++
                    Json.obj("surface" -> surface, "setLevel" -> level.value)
                )
              case Left(payload) => ServiceUnavailable(payload ++ Json.obj("surface" -> surface, "setLevel" -> level.value))
  }

  def testGroundEceBoardOverlay = Open:
    val fen = get("fen").filter(_.nonEmpty).getOrElse(defaultTestGroundFen)
    val gameId = get("gameId").filter(_.nonEmpty).getOrElse("test-ground-game")
    val playerId = get("playerId").filter(_.nonEmpty).getOrElse("test-ground-student")
    val ply = get("ply").flatMap(_.toIntOption).filter(_ >= 0).getOrElse(10)
    val requesterSide =
      get("side").flatMap(parsePerspective).getOrElse(sideToMoveFromFen(fen).getOrElse(Perspective.White))
    val movingSide = sideToMoveFromFen(fen).getOrElse(Perspective.White)
    val level = levelParam("level", 10)
    val whiteLevel = levelParam("whiteLevel", level.value)
    val blackLevel = levelParam("blackLevel", level.value)
    val requesterLevel = if requesterSide == Perspective.White then whiteLevel else blackLevel
    val ttlMillis = get("ttlMillis").flatMap(_.toIntOption).filter(_ > 0).getOrElse(1500)
    val historyOnly = booleanParam("historyOnly")
    val adminUnlimited = adminUnlimitedTokensEnabled
    val config = testGroundEceConfig
    val request =
      EngineGateway.EceBoardStateRequest.boardState(
        gameId = gameId,
        ply = ply,
        inputFen = fen,
        whiteEcr = None,
        blackEcr = None,
        whiteLevel = whiteLevel,
        blackLevel = blackLevel,
        aiTextAllowed = false
      )

    cachedBoardOverlay(gameId, playerId, requesterSide, fen, ply, requesterLevel, ttlMillis, adminUnlimited) match
      case Some(payload) => Future.successful(JsonOk(payload))
      case None if historyOnly =>
        Future.successful(
          NotFound(
            Json.obj(
              "ok" -> false,
              "error" -> "ece_history_payload_missing",
              "gameId" -> gameId,
              "ply" -> ply,
              "fen" -> fen,
              "side" -> perspectiveKey(requesterSide),
              "requestedLevel" -> requesterLevel.value,
              "analysisRequestRequired" -> true
            )
          )
        )
      case None =>
        Future(blocking(testGroundEceBridgePayload(config, gameId, playerId, ply, fen, requesterSide, request, ttlMillis)))
          .map:
            case Right(payload) =>
              val sanitized = hidePotentialMovePayload(payload, gameId, requesterSide, requesterLevel, ply, adminUnlimited)
              JsonOk(rememberBoardOverlay(gameId, playerId, requesterSide, fen, requesterLevel, ply, sanitized))
            case Left(payload) => ServiceUnavailable(payload)

  def testGroundEceGameHistory = Open:
    val gameId = get("gameId").filter(_.nonEmpty).getOrElse("test-ground-game")
    val playerId = get("playerId").filter(_.nonEmpty).getOrElse("test-ground-student")
    val requesterSide = get("side").flatMap(parsePerspective).getOrElse(Perspective.White)
    JsonOk(boardOverlayHistoryJson(gameId, playerId, requesterSide))

  def testGroundEceFullGameReview = OpenBodyOf(parse.json): ctx ?=>
    val body = ctx.body.body
    val gameId = stringField(body, "gameId").getOrElse("test-ground-game")
    val playerId = stringField(body, "playerId").getOrElse("test-ground-student")
    val requesterSide = stringField(body, "side").flatMap(parsePerspective).getOrElse(Perspective.White)
    val level = levelField(body, "level").getOrElse(Level(10))
    val frames = parseFullGameReviewFrameInputs(body)
    val config =
      stringField(body, "eceBaseUrl")
        .map(EngineGateway.EceServiceConfig.normalizeBaseUrl)
        .filter(url => EngineGateway.EceServiceConfig(baseUrl = url).valid)
        .fold(EngineGateway.EceServiceConfig())(url => EngineGateway.EceServiceConfig(baseUrl = url))

    if level.value != 10 then BadRequest(Json.obj("ok" -> false, "error" -> "full_game_review_requires_level_10")).toFuccess
    else if frames.isEmpty || frames.exists(!_.valid) then
      BadRequest(Json.obj("ok" -> false, "error" -> "invalid_full_game_review_frames")).toFuccess
    else
      Future(blocking(runFullGameReviewBackfill(config, gameId, playerId, requesterSide, frames, level))).map:
        case Right(payload) => Ok(payload)
        case Left(payload)  => ServiceUnavailable(payload)

  def testGroundEceFullMatchSummary = OpenBodyOf(parse.json): ctx ?=>
    val body = ctx.body.body
    val playerId = stringField(body, "playerId").orElse(stringField(body, "userId")).getOrElse("test-ground-student")
    val requesterSide = stringField(body, "side")
      .orElse(stringField(body, "user_side"))
      .flatMap(parsePerspective)
      .getOrElse(Perspective.White)
    val requestId = stringField(body, "requestId").orElse(stringField(body, "request_id")).getOrElse(s"$playerId-full-match-summary")
    val useAi = booleanField(body, "useAi").orElse(booleanField(body, "use_ai")).getOrElse(intField(body, "use_ai").contains(1))
    val fullMatch =
      objectField(body, "full_match")
        .orElse(objectField(body, "fullMatch"))
        .orElse(canonicalFullMatchPayload(body))
        .map(normalizeFullMatchForSummary)
    val config =
      stringField(body, "eceBaseUrl")
        .map(EngineGateway.EceServiceConfig.normalizeBaseUrl)
        .filter(url => EngineGateway.EceServiceConfig(baseUrl = url).valid)
        .fold(EngineGateway.EceServiceConfig())(url => EngineGateway.EceServiceConfig(baseUrl = url))

    fullMatch match
      case None =>
        BadRequest(Json.obj("ok" -> false, "error" -> "missing_full_match")).toFuccess
      case Some(matchPayload) if hasForbiddenEcePublicField(matchPayload) =>
        BadRequest(Json.obj("ok" -> false, "error" -> "full_match_contains_forbidden_public_field")).toFuccess
      case Some(matchPayload) =>
        val request = eceFullMatchSummaryRequestJson(requestId, playerId, requesterSide, useAi, matchPayload)
        Future(blocking(callEceFullMatchSummary(config, request))).map:
          case Right(summaryJson) => Ok(summaryJson)
          case Left(error) =>
            ServiceUnavailable(
              Json.obj(
                "ok" -> false,
                "error" -> "ece_full_match_summary_unavailable",
                "message" -> safeEceErrorMessage(error, "ECE board overlay unavailable")
              )
            )

  def testGroundEcePerformanceSummary = OpenBodyOf(parse.json): ctx ?=>
    val body = ctx.body.body
    val playerId = stringField(body, "playerId").orElse(stringField(body, "userId")).getOrElse("test-ground-student")
    val requestId = stringField(body, "requestId").orElse(stringField(body, "request_id")).getOrElse(s"$playerId-performance-summary")
    val useAi = booleanField(body, "useAi").orElse(booleanField(body, "use_ai")).getOrElse(intField(body, "use_ai").contains(1))
    val summaries =
      List("full_match_summaries", "fullMatchSummaries", "matchSummaries")
        .view
        .map(key => arrayField(body, key))
        .find(_.nonEmpty)
        .getOrElse(Nil)
        .collect { case obj: JsObject => obj }
        .take(10)
    val config =
      stringField(body, "eceBaseUrl")
        .map(EngineGateway.EceServiceConfig.normalizeBaseUrl)
        .filter(url => EngineGateway.EceServiceConfig(baseUrl = url).valid)
        .fold(EngineGateway.EceServiceConfig())(url => EngineGateway.EceServiceConfig(baseUrl = url))

    if summaries.isEmpty then
      BadRequest(Json.obj("ok" -> false, "error" -> "missing_full_match_summaries")).toFuccess
    else if summaries.exists(hasForbiddenEcePublicField) then
      BadRequest(Json.obj("ok" -> false, "error" -> "performance_summary_contains_forbidden_public_field")).toFuccess
    else
      val request = ecePerformanceSummaryRequestJson(requestId, playerId, useAi, summaries)
      Future(blocking(callEcePerformanceSummary(config, request))).map:
        case Right(summaryJson) => Ok(summaryJson)
        case Left(error) =>
          ServiceUnavailable(
            Json.obj(
              "ok" -> false,
              "error" -> "ece_performance_summary_unavailable",
              "message" -> safeEceErrorMessage(error, "ECE board overlay unavailable")
            )
          )

  def testGroundEceProposedMove = Open:
    val fen = get("fen").filter(_.nonEmpty).getOrElse(defaultTestGroundFen)
    val gameId = get("gameId").filter(_.nonEmpty).getOrElse("test-ground-game")
    val playerId = get("playerId").filter(_.nonEmpty).getOrElse("test-ground-student")
    val ply = get("ply").flatMap(_.toIntOption).filter(_ >= 0).getOrElse(10)
    val proposalIndex = get("proposalIndex").flatMap(_.toIntOption).filter(_ > 0).getOrElse(1)
    val proposedMoveUci = get("moveUci").filter(_.matches("[a-h][1-8][a-h][1-8][qrbn]?")).getOrElse("")
    val proposedMoveSan = sanForProposedMove(fen, proposedMoveUci)
    val requesterSide =
      get("side").flatMap(parsePerspective).getOrElse(sideToMoveFromFen(fen).getOrElse(Perspective.White))
    val movingSide = sideToMoveFromFen(fen).getOrElse(Perspective.White)
    val level = levelParam("level", 10)
    val whiteLevel = levelParam("whiteLevel", level.value)
    val blackLevel = levelParam("blackLevel", level.value)
    val requesterLevel = if requesterSide == Perspective.White then whiteLevel else blackLevel
    val adminUnlimited = adminUnlimitedTokensEnabled
    val quota = proposedMoveQuotaForLevel(requesterLevel, adminUnlimited)
    val cacheKey = proposedMoveCacheKey(gameId, requesterSide, ply, fen, requesterLevel, proposedMoveUci)
    val turnKey = proposedMoveTurnKey(gameId, requesterSide, ply, fen, requesterLevel)
    val cached = cachedProposedMove(cacheKey, gameId, requesterSide, quota)
    val config = testGroundEceConfig
    val request = EngineGateway.EceProposedMoveRequest.proposedMove(
      gameId = gameId,
      ply = ply,
      proposalIndex = proposalIndex,
      inputFen = fen,
      proposedMoveUci = proposedMoveUci,
      whiteEcr = None,
      blackEcr = None,
      whiteLevel = whiteLevel,
      blackLevel = blackLevel,
      aiTextAllowed = false,
      proposedMoveSan = proposedMoveSan
    )

    if proposedMoveUci.isEmpty then BadRequest(Json.obj("ok" -> false, "error" -> "missing_or_invalid_move"))
    else if proposedMoveSan.isEmpty then BadRequest(Json.obj("ok" -> false, "error" -> "missing_or_invalid_san", "message" -> "Proposed Move arrow is not a legal move in the current FEN."))
    else if requesterLevel.value < 5 then
      BadRequest(Json.obj("ok" -> false, "error" -> "proposed_move_unavailable_for_level", "level" -> requesterLevel.value))
    else if requesterSide != movingSide then
      BadRequest(Json.obj("ok" -> false, "error" -> "not_requester_turn", "sideToMove" -> perspectiveKey(movingSide)))
    else if !request.valid then BadRequest(Json.obj("ok" -> false, "error" -> "invalid_proposed_move_request"))
    else if cached.isDefined then JsonOk(cached.get)
    else if !adminUnlimited && proposedTurnAlreadyUsed(turnKey, cacheKey) then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "proposed_move_already_used_this_turn",
          "message" -> "Proposed Move already used this turn",
          "consumed" -> proposedMoveConsumed(gameId, requesterSide),
          "quota" -> quota
        )
      )
    else if !adminUnlimited && proposedMoveConsumed(gameId, requesterSide) >= quota then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "proposed_move_limit_reached",
          "message" -> "Proposed Move limit reached",
          "consumed" -> proposedMoveConsumed(gameId, requesterSide),
          "quota" -> quota
        )
      )
    else
      Future(blocking(testGroundEceProposedMovePayload(config, gameId, playerId, ply, fen, requesterSide, request)))
        .map:
          case Right(payload) if approvedProposedMovePayload(payload) =>
            JsonOk(rememberProposedMove(cacheKey, turnKey, payload, quota))
          case Right(payload) =>
            BadRequest(withProposedMoveUsage(payload, gameId, requesterSide, quota))
          case Left(payload)  => ServiceUnavailable(withProposedMoveUsage(payload, gameId, requesterSide, quota))

  def testGroundEcePotentialMove = Open:
    val fen = get("fen").filter(_.nonEmpty).getOrElse(defaultTestGroundFen)
    val gameId = get("gameId").filter(_.nonEmpty).getOrElse("test-ground-game")
    val playerId = get("playerId").filter(_.nonEmpty).getOrElse("test-ground-student")
    val ply = get("ply").flatMap(_.toIntOption).filter(_ >= 0).getOrElse(10)
    val requesterSide =
      get("side").flatMap(parsePerspective).getOrElse(sideToMoveFromFen(fen).getOrElse(Perspective.White))
    val movingSide = sideToMoveFromFen(fen).getOrElse(Perspective.White)
    val kind = get("kind").filter(value => value == "player" || value == "opponent").getOrElse("player")
    val level = levelParam("level", 10)
    val whiteLevel = levelParam("whiteLevel", level.value)
    val blackLevel = levelParam("blackLevel", level.value)
    val requesterLevel = if requesterSide == Perspective.White then whiteLevel else blackLevel
    val movingLevel = if movingSide == Perspective.White then whiteLevel else blackLevel
    val adminUnlimited = adminUnlimitedTokensEnabled
    val quota = potentialMoveQuotaForLevel(requesterLevel, kind, adminUnlimited)
    val key = potentialMoveRevealKey(gameId, requesterSide, ply, fen, requesterLevel, kind)
    val cached = cachedPotentialMove(key, quota)
    val config = testGroundEceConfig
    val request =
      EngineGateway.EcePotentialEcsRequest.potentialEcs(
        gameId = gameId,
        ply = ply,
        inputFen = fen,
        whiteLevel = whiteLevel,
        blackLevel = blackLevel
      )

    if quota < 1 then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "potential_moves_unavailable_for_level",
          "message" -> (if kind == "player" then "My Potential Moves start at level 6" else "Opponent Potential Moves start at level 5"),
          "consumed" -> potentialMoveConsumed(gameId, requesterSide, kind),
          "quota" -> quota
        )
      )
    else if kind == "player" && requesterSide != movingSide then
      BadRequest(Json.obj("ok" -> false, "error" -> "not_requester_turn", "message" -> "Available on your turn"))
    else if kind == "opponent" && requesterSide == movingSide then
      BadRequest(Json.obj("ok" -> false, "error" -> "not_opponent_turn", "message" -> "Available on opponent's turn"))
    else if movingLevel.value < 5 then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "potential_ecs_unavailable_for_side_level",
          "message" -> "Potential ECS starts when the side to move has level 5",
          "consumed" -> potentialMoveConsumed(gameId, requesterSide, kind),
          "quota" -> quota
        )
      )
    else if cached.isDefined then JsonOk(cached.get)
    else if !adminUnlimited && potentialMoveConsumed(gameId, requesterSide, kind) >= quota then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "potential_move_limit_reached",
          "message" -> "Potential Move limit reached",
          "consumed" -> potentialMoveConsumed(gameId, requesterSide, kind),
          "quota" -> quota
        )
      )
    else if !request.valid then BadRequest(Json.obj("ok" -> false, "error" -> "invalid_potential_ecs_request"))
    else
      val revealSide = movingSide
      Future(blocking(testGroundEcePotentialMovePayload(config, gameId, playerId, ply, fen, kind, revealSide, quota, request, key)))
        .map:
          case Right(reveal) if approvedPotentialMovePayload(reveal) =>
            JsonOk(rememberPotentialMove(key, reveal))
          case Right(reveal) =>
            BadRequest(withPotentialMoveUsage(reveal, gameId, requesterSide, kind, quota))
          case Left(payload) =>
            val enriched =
              payload ++ Json.obj(
                "consumed" -> potentialMoveConsumed(gameId, requesterSide, kind),
                "quota" -> quota,
                "adminUnlimitedTokens" -> isAdminUnlimitedQuota(quota)
              )
            if stringField(payload, "error").contains("ece_unavailable") then ServiceUnavailable(enriched)
            else BadRequest(enriched)

  def testGroundEcePotentialMoveRefund = Open:
    val gameId = get("gameId").filter(_.nonEmpty).getOrElse("test-ground-game")
    val requesterSide = get("side").flatMap(parsePerspective).getOrElse(Perspective.White)
    val kind = get("kind").filter(value => value == "player" || value == "opponent").getOrElse("player")
    val key = get("key").filter(_.nonEmpty).getOrElse("")
    val level = levelParam("level", 10)
    val adminUnlimited = adminUnlimitedTokensEnabled
    val quota = potentialMoveQuotaForLevel(level, kind, adminUnlimited)

    if key.isEmpty then BadRequest(Json.obj("ok" -> false, "error" -> "missing_potential_move_key"))
    else
      val refunded = refundPotentialMove(key, gameId, requesterSide, kind)
      JsonOk(
        Json.obj(
          "ok" -> true,
          "refunded" -> refunded,
          "key" -> key,
          "kind" -> kind,
          "consumed" -> potentialMoveConsumed(gameId, requesterSide, kind),
          "quota" -> quota,
          "adminUnlimitedTokens" -> isAdminUnlimitedQuota(quota)
        )
      )

  def testGroundEcePositionEcs = Open:
    val fen = get("fen").filter(_.nonEmpty).getOrElse(defaultTestGroundFen)
    val gameId = get("gameId").filter(_.nonEmpty).getOrElse("test-ground-game")
    val playerId = get("playerId").filter(_.nonEmpty).getOrElse("test-ground-student")
    val ply = get("ply").flatMap(_.toIntOption).filter(_ >= 0).getOrElse(10)
    val requesterSide =
      get("side").flatMap(parsePerspective).getOrElse(sideToMoveFromFen(fen).getOrElse(Perspective.White))
    val movingSide = sideToMoveFromFen(fen).getOrElse(Perspective.White)
    val level = levelParam("level", 10)
    val whiteLevel = levelParam("whiteLevel", level.value)
    val blackLevel = levelParam("blackLevel", level.value)
    val requesterLevel = if requesterSide == Perspective.White then whiteLevel else blackLevel
    val positionEcsId = get("positionEcsId").filter(_.nonEmpty)
    val adminUnlimited = adminUnlimitedTokensEnabled
    val key = positionEcsCacheKey(gameId, requesterSide, ply, fen, requesterLevel)
    val cached = cachedPositionEcs(key, gameId, requesterSide, requesterLevel, ply, adminUnlimited)
    val usage = positionEcsUsage(gameId, requesterSide, requesterLevel, ply, adminUnlimitedTokens = adminUnlimited)
    val config = testGroundEceConfig
    val request =
      EngineGateway.EcePositionEcsRequest.positionEcs(
        gameId = gameId,
        ply = ply,
        inputFen = fen,
        userSide = requesterSide,
        whiteEcr = None,
        blackEcr = None,
        whiteLevel = whiteLevel,
        blackLevel = blackLevel,
        positionEcsId = positionEcsId
      )

    if requesterLevel.value < 4 then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "position_ecs_unavailable_for_level",
          "message" -> "Ask AI starts at level 4",
          "positionEcs" -> positionEcsUsageJson(usage)
        )
      )
    else if requesterSide != movingSide then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "not_requester_turn",
          "message" -> "Ask AI is available on your turn",
          "sideToMove" -> perspectiveKey(movingSide),
          "positionEcs" -> positionEcsUsageJson(usage)
        )
      )
    else if cached.isDefined then JsonOk(cached.get)
    else if usage.available < 1 then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "position_ecs_limit_reached",
          "message" -> positionEcsNoTokenMessage(usage),
          "positionEcs" -> positionEcsUsageJson(usage)
        )
      )
    else if !request.valid then BadRequest(Json.obj("ok" -> false, "error" -> "invalid_position_ecs_request"))
    else
      Future(blocking(testGroundEcePositionEcsPayload(config, gameId, playerId, ply, fen, requesterSide, request, key)))
        .map:
          case Right(payload) if approvedPositionEcsPayload(payload) =>
            JsonOk(rememberPositionEcs(key, payload, gameId, requesterSide, requesterLevel, ply, adminUnlimited))
          case Right(payload) =>
            BadRequest(withPositionEcsUsage(payload, gameId, requesterSide, requesterLevel, ply, adminUnlimited))
          case Left(payload)  => ServiceUnavailable(withPositionEcsUsage(payload, gameId, requesterSide, requesterLevel, ply, adminUnlimited))

  private def testGroundEceBridgePayload(
      config: EngineGateway.EceServiceConfig,
      gameId: String,
      playerId: String,
      ply: Int,
      fen: String,
      requesterSide: Perspective,
      request: EngineGateway.EceBoardStateRequest,
      ttlMillis: Int
  ): Either[JsObject, JsObject] =
    callEceBoardStandard(config, request) match
      case Left(error) =>
        Left(eceBridgeErrorJson("ece_unavailable", error, config, request))
      case Right(eceJson) =>
        parseEceBoardPayload(eceJson) match
          case Left(error) =>
            Left(eceBridgeErrorJson("ece_payload_rejected", error, config, request))
          case Right(parsed) =>
            val sidePayload = if requesterSide == Perspective.White then parsed.white else parsed.black
            val authorizedLevel = if requesterSide == Perspective.White then request.whiteLevel else request.blackLevel
            val result =
              EceLiveBridge.compileBoardOverlay(
                config = config,
                gameId = gameId,
                playerId = playerId,
                ply = ply,
                boardStateKey = fen,
                requesterSide = requesterSide,
                authorizedLevel = authorizedLevel,
                request = request,
                currentFen = fen,
                response = parsed.response,
                atoms = sidePayload.atoms,
                auditId = sidePayload.auditId,
                ttlMillis = ttlMillis,
                extraCards = sidePayload.extras.cards,
                extraVisuals = sidePayload.extras.visuals
              )
            Right(eceBridgeSuccessJson(config, request, gameId, playerId, requesterSide, result, parsed, sidePayload, ttlMillis))

  private def siteEceBoardOverlayPayload(
      gameId: String,
      playerId: String,
      ply: Int,
      fen: String,
      requesterSide: Perspective,
      request: EngineGateway.EceBoardStateRequest,
      ttlMillis: Int
  ): Either[JsObject, JsObject] =
    var latest: Either[JsObject, JsObject] = Left(Json.obj("ok" -> false, "error" -> "ece_config_unavailable"))
    siteEceConfigs.foreach: config =>
      if latest.isLeft then
        latest = testGroundEceBridgePayload(config, gameId, playerId, ply, fen, requesterSide, request, ttlMillis)
    latest

  private def siteEceConfigs: List[EngineGateway.EceServiceConfig] =
    val configured =
      sys.env
        .get("EVENCHESS_ECE_BASE_URL")
        .orElse(sys.props.get("evenchess.ece.baseUrl"))
        .map(EngineGateway.EceServiceConfig.normalizeBaseUrl)
        .filter(url => EngineGateway.EceServiceConfig(baseUrl = url).valid)
    val default = EngineGateway.EceServiceConfig()
    val dockerHost = EngineGateway.EceServiceConfig(baseUrl = "http://host.docker.internal:8787")
    (configured.map(url => EngineGateway.EceServiceConfig(baseUrl = url)).toList ++ List(default, dockerHost))
      .filter(_.valid)
      .distinctBy(_.baseUrl)

  private def parseFullGameReviewFrameInputs(body: JsValue): List[FullGameReviewFrameInput] =
    arrayField(body, "frames").flatMap: item =>
      for
        ply <- intField(item, "ply")
        fen <- stringField(item, "fen")
      yield FullGameReviewFrameInput(ply, fen, stringField(item, "moveUci").orElse(stringField(item, "move_uci")))

  private def parseDisplayStateInput(body: JsValue): Option[GamePolicy.StoredDisplayState] =
    val toggles = objectField(body, "toggles").getOrElse(Json.obj())
    val appliedLevel = levelField(toggles, "appliedLevel").orElse(levelField(body, "appliedLevel"))
    val levelFeatures =
      objectField(toggles, "levelFeatures")
        .map: features =>
          features.fields.flatMap:
            case (key, value) if key.matches("[A-Za-z0-9_-]{1,48}") =>
              value.asOpt[Boolean].map(key -> _)
            case _ => None
          .toMap
        .getOrElse(Map.empty)
    appliedLevel
      .filter(level => Level.isValid(level.value))
      .map: level =>
        GamePolicy.StoredDisplayState(
          appliedLevel = level,
          coachCards = booleanField(toggles, "coachCards").getOrElse(true),
          boardVisuals = booleanField(toggles, "boardVisuals").getOrElse(true),
          levelFeatures = levelFeatures
        )

  private def displayStateJson(state: GamePolicy.StoredDisplayState): JsObject =
    Json.obj(
      "toggles" -> Json.obj(
        "coachCards" -> state.coachCards,
        "boardVisuals" -> state.boardVisuals,
        "appliedLevel" -> state.appliedLevel.value,
        "levelFeatures" -> state.levelFeatures
      )
    )

  private def reviewStatePayload(
      gameId: String,
      ownerUserId: String,
      ply: Int,
      requestedFen: Option[String],
      viewerSide: Perspective,
      tier: String,
      adminUnlimited: Boolean
  ): JsObject =
    val doc = postGameReviewStore.get(ownerUserId, gameId)
    val exactFrames = postGameReviewStore.framesAtPly(ownerUserId, gameId, ply, requestedFen)
    val frames = if exactFrames.nonEmpty then exactFrames else postGameReviewStore.framesAtPly(ownerUserId, gameId, ply, None)
    val selectedFen = requestedFen.orElse(frames.headOption.flatMap(frame => stringField(frame, "fen")))
    val displaySide = selectedFen.flatMap(sideToMoveFromFen).getOrElse(viewerSide)
    val frame =
      postGameReviewStore
        .frameFor(ownerUserId, gameId, ply, requestedFen, displaySide)
        .orElse(postGameReviewStore.frameFor(ownerUserId, gameId, ply, None, displaySide))
        .orElse(frames.headOption)
    val payload = frame.flatMap(frame => objectField(frame, "payload"))
    val live = payload.flatMap(payload => objectField(payload, "live"))
    val askAi = frame.flatMap(frame => objectField(frame, "askAi"))
    val summaries = objectField(doc, "matchSummaries").getOrElse(Json.obj())
    val summary = objectField(summaries, perspectiveKey(viewerSide))
    val frameStatus =
      if frame.isDefined then "ready"
      else stringField(doc, "ecemfStatus").getOrElse("missing")

    Json.obj(
      "ok" -> true,
      "gameId" -> gameId,
      "ownerUserId" -> ownerUserId,
      "ply" -> ply,
      "viewerSide" -> perspectiveKey(viewerSide),
      "displaySide" -> perspectiveKey(displaySide),
      "label" -> (if displaySide == viewerSide then "Your coach text" else "Opponent text"),
      "ecemfStatus" -> stringField(doc, "ecemfStatus").getOrElse("missing"),
      "frameStatus" -> frameStatus,
      "review" -> reviewDocSummary(doc),
      "frame" -> frame,
      "live" -> live,
      "askAi" -> askAi,
      "matchSummary" -> summary,
      "quota" -> Json.obj(
        "nonLiveAskAi" -> nonLiveAskAiLedger.quota(ownerUserId, tier, System.currentTimeMillis, adminUnlimited)
      )
    )

  private def reviewDocSummary(doc: JsObject): JsObject =
    Json.obj(
      "schema" -> stringField(doc, "schema"),
      "status" -> stringField(doc, "ecemfStatus").getOrElse("missing"),
      "reviewLevel" -> intField(doc, "reviewLevel").getOrElse(10),
      "framesStored" -> arrayField(doc, "frames").size,
      "hasFullMatch" -> objectField(doc, "evenchessFullGame").isDefined,
      "fenHash" -> stringField(doc, "fenHash"),
      "eceVersion" -> stringField(doc, "eceVersion"),
      "policyVersion" -> stringField(doc, "policyVersion"),
      "updatedAt" -> longField(doc, "updatedAt")
    )

  private def runFullGameReviewBackfill(
      config: EngineGateway.EceServiceConfig,
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      frames: List[FullGameReviewFrameInput],
      level: Level
  ): Either[JsObject, JsObject] =
    val request = fullGameReviewRequest(gameId, frames, level)
    val fromGameReview =
      if request.valid then
        callEceGameReview(config, request).toOption.flatMap: eceJson =>
          storeFullGameReviewFramesFromEce(gameId, playerId, requesterSide, level, eceJson).toOption
      else None
    fromGameReview match
      case Some(stored) =>
        Right(
          Json.obj(
            "ok" -> true,
            "source" -> "ece_full_match",
            "framesStored" -> stored,
            "history" -> boardOverlayHistoryJson(gameId, playerId, requesterSide)
          )
        )
      case None =>
        sequentialFullGameReviewBackfill(config, gameId, playerId, requesterSide, frames, level)

  private def runPostGameReviewGeneration(
      configs: List[EngineGateway.EceServiceConfig],
      gameId: String,
      ownerUserId: String,
      frames: List[FullGameReviewFrameInput],
      level: Level,
      result: String,
      termination: String
  ): Either[JsObject, JsObject] =
    val request = fullGameReviewRequest(gameId, frames, level, result, termination)
    if !request.valid then Left(Json.obj("ok" -> false, "error" -> "invalid_review_ecemf_request"))
    else
      var latest: Either[JsObject, JsObject] =
        Left(Json.obj("ok" -> false, "error" -> "ece_full_match_unavailable", "message" -> "ECE full-match unavailable"))
      configs.foreach: config =>
        if latest.isLeft then
          latest =
            callEceGameReview(config, request) match
              case Left(error) =>
                Left(Json.obj("ok" -> false, "error" -> "ece_full_match_unavailable", "message" -> safeEceErrorMessage(error, "ECE full-match unavailable")))
              case Right(eceJson) =>
                storePostGameReviewFramesFromEce(gameId, ownerUserId, level, frames, eceJson) match
                  case Left(error) =>
                    Left(Json.obj("ok" -> false, "error" -> error))
                  case Right((framesStored, doc)) =>
                    Right(
                      Json.obj(
                        "ok" -> true,
                        "source" -> "ece_full_match",
                        "gameId" -> gameId,
                        "framesStored" -> framesStored,
                        "ecemfStatus" -> "ready",
                        "review" -> reviewDocSummary(doc)
                      )
                    )
      latest

  private def fullGameReviewRequest(
      gameId: String,
      frames: List[FullGameReviewFrameInput],
      level: Level,
      result: String = "unknown",
      termination: String = "unknown"
  ): EngineGateway.EceGameReviewRequest =
    val sorted = frames.sortBy(_.ply)
    val game =
      EngineGateway.EceGameReviewInput(
        gameId = gameId,
        initialFen = sorted.headOption.map(_.fen).getOrElse(defaultTestGroundFen),
        pgn = None,
        moves = sorted.flatMap(_.moveUci),
        fenHistory = sorted.map(_.fen),
        result = result,
        termination = termination
      )
    EngineGateway.EceGameReviewRequest.gameReview(
      gameId = gameId,
      reviewIndex = 1,
      game = game,
      whiteEcr = None,
      blackEcr = None,
      reviewLevel = level,
      aiNarrativeAllowed = false,
      liveEceSnapshots = Nil
    )

  private def storeFullGameReviewFramesFromEce(
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      level: Level,
      json: JsValue
  ): Either[String, Int] =
    val diagnostics = parseDiagnostics(json)
    if !diagnostics.displayAllowed then Left("ece_game_review_diagnostics_not_displayable")
    else if hasForbiddenEcePublicField(json) then Left("ece_game_review_exposed_forbidden_field")
    else
      val frames = fullMatchTurns(json)
      if frames.isEmpty then Left("ece_game_review_returned_no_frames")
      else
        val stored =
          frames.flatMap(frame =>
            fullGameReviewFramePayload(gameId, playerId, requesterSide, level, frame).map { case (fen, ply, payload) =>
              rememberBoardOverlay(gameId, playerId, requesterSide, fen, level, ply, payload)
            }.toOption
        )
        Either.cond(stored.nonEmpty, stored.size, "ece_game_review_frames_not_displayable")

  private def storePostGameReviewFramesFromEce(
      gameId: String,
      ownerUserId: String,
      level: Level,
      requestFrames: List[FullGameReviewFrameInput],
      json: JsValue
  ): Either[String, (Int, JsObject)] =
    val diagnostics = parseDiagnostics(json)
    if !diagnostics.displayAllowed then Left("ece_full_match_diagnostics_not_displayable")
    else if hasForbiddenEcePublicField(json) then Left("ece_full_match_exposed_forbidden_public_field")
    else
      val fullMatch = canonicalFullMatchPayload(json).toRight("ece_full_match_missing_canonical_payload")
      fullMatch.flatMap: matchPayload =>
        if hasForbiddenEcePublicField(matchPayload) then Left("ece_full_match_contains_forbidden_public_field")
        else
          val turns = fullMatchTurns(json)
          if turns.isEmpty then Left("ece_full_match_returned_no_frames")
          else
            val reviewFrames =
              turns.flatMap: frame =>
                List(Perspective.White, Perspective.Black).flatMap: side =>
                  fullGameReviewFramePayload(gameId, ownerUserId, side, level, frame).toOption.map { case (fen, ply, payload) =>
                    rememberBoardOverlay(gameId, ownerUserId, side, fen, level, ply, payload)
                    StoredReviewFrame(
                      ply = ply,
                      fen = fen,
                      side = side,
                      level = level,
                      source = "ecemf",
                      payload = payload
                    )
                  }
            if reviewFrames.isEmpty then Left("ece_full_match_frames_not_displayable")
            else
              val fenHash = sha256Hex(requestFrames.sortBy(_.ply).map(frame => s"${frame.ply}:${frame.fen}").mkString("|")).take(32)
              val doc =
                postGameReviewStore.putFullMatch(
                  ownerUserId = ownerUserId,
                  gameId = gameId,
                  reviewLevel = level,
                  fullMatch = matchPayload,
                  frames = reviewFrames,
                  fenHash = fenHash,
                  eceVersion = diagnostics.engineVersion,
                  policyVersion = "evenchess-post-game-review-v1"
                )
              Right(reviewFrames.size -> doc)

  private def fullMatchTurns(json: JsValue): List[JsValue] =
    val canonicalTurns =
      canonicalFullMatchPayload(json).map(fullGame => arrayField(fullGame, "turns")).getOrElse(Nil)
    if canonicalTurns.nonEmpty then canonicalTurns
    else arrayField(json, "frames") ++ arrayField(json, "move_outputs")

  private def canonicalFullMatchPayload(json: JsValue): Option[JsObject] =
    objectField(json, "evenchess_full_game").map(normalizeFullMatchForSummary)

  private def normalizeFullMatchForSummary(fullMatch: JsObject): JsObject =
    fullMatch ++ Json.obj(
      "format_name" -> "evenchess_full_game",
      "format_version" -> "1.0"
    )

  private def eceFullMatchSummaryRequestJson(
      requestId: String,
      userId: String,
      userSide: Perspective,
      useAi: Boolean,
      fullMatch: JsObject
  ): JsObject =
    Json.obj(
      "request" -> Json.obj(
        "mode" -> "full_match_summary",
        "request_id" -> requestId,
        "user_id" -> userId,
        "user_side" -> perspectiveKey(userSide),
        "use_ai" -> (if useAi then 1 else 0),
        "full_match" -> fullMatch
      )
    )

  private def ecePerformanceSummaryRequestJson(
      requestId: String,
      userId: String,
      useAi: Boolean,
      summaries: List[JsObject]
  ): JsObject =
    Json.obj(
      "request" -> Json.obj(
        "mode" -> "performance_summary",
        "request_id" -> requestId,
        "user_id" -> userId,
        "use_ai" -> (if useAi then 1 else 0),
        "full_match_summaries" -> summaries
      )
    )

  private def fullGameReviewFramePayload(
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      level: Level,
      frame: JsValue
  ): Either[String, (String, Int, JsObject)] =
    val sideKey = perspectiveKey(requesterSide)
    val ecePayload = objectField(frame, "ece_payload").getOrElse(frame.asOpt[JsObject].getOrElse(Json.obj()))
    val sideOutputs =
      field(ecePayload, "side_outputs")
        .orElse(field(frame, "side_outputs"))
        .getOrElse(frame)
    val echo = field(ecePayload, "request_echo").getOrElse(Json.obj())
    for
      ply <- intField(frame, "ply").toRight("missing full-game frame ply")
      fen <- stringField(frame, "fen")
        .orElse(stringField(frame, "fen_after_move"))
        .orElse(stringField(echo, "input_fen"))
        .toRight("missing full-game frame fen")
      parsed <- parseSidePayload(sideOutputs, sideKey, requesterSide, includeEval = true)
    yield
      val payload =
        EceLiveBridge.compileStoredBoardFrame(
          gameId = gameId,
          playerId = playerId,
          ply = ply,
          boardStateKey = fen,
          requesterSide = requesterSide,
          authorizedLevel = level,
          sideOutput = parsed.output,
          atoms = parsed.atoms,
          auditId = parsed.auditId,
          ttlMillis = 60_000,
          extraCards = parsed.extras.cards,
          extraVisuals = parsed.extras.visuals
        )
      val now = System.currentTimeMillis
      val live = roundOverlayJson(payload, 60_000, now)
      val storedPayload = hidePotentialMovePayload(Json.obj("ok" -> true, "live" -> live), gameId, requesterSide, level, ply)
      (fen, ply, storedPayload)

  private def sequentialFullGameReviewBackfill(
      config: EngineGateway.EceServiceConfig,
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      frames: List[FullGameReviewFrameInput],
      level: Level
  ): Either[JsObject, JsObject] =
    val stored =
      frames.sortBy(_.ply).flatMap: frame =>
        val request =
          EngineGateway.EceBoardStateRequest.boardState(
            gameId = gameId,
            ply = frame.ply,
            inputFen = frame.fen,
            whiteEcr = None,
            blackEcr = None,
            whiteLevel = level,
            blackLevel = level,
            aiTextAllowed = false
          )
        testGroundEceBridgePayload(config, gameId, playerId, frame.ply, frame.fen, requesterSide, request, 60_000).toOption.map: payload =>
          val sanitized = hidePotentialMovePayload(payload, gameId, requesterSide, level, frame.ply)
          rememberBoardOverlay(gameId, playerId, requesterSide, frame.fen, level, frame.ply, sanitized)
    Either.cond(
      stored.nonEmpty,
      Json.obj(
        "ok" -> true,
        "source" -> "sequential_board_state_backfill",
        "framesStored" -> stored.size,
        "history" -> boardOverlayHistoryJson(gameId, playerId, requesterSide)
      ),
      Json.obj(
        "ok" -> false,
        "error" -> "full_game_review_unavailable",
        "message" -> "ECE game-review did not return displayable frames and board-state backfill could not store a payload."
      )
    )

  private def callEceBoardStandard(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceBoardStateRequest
  ): Either[String, JsValue] =
    postEceJson(config.standardUrl, eceBoardRequestJson(request), timeoutSeconds = 8, label = "ECE standard endpoint")

  private def callEceProposedMove(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceProposedMoveRequest
  ): Either[String, JsValue] =
    postEceJson(config.proposedMoveUrl, eceProposedMoveRequestJson(request), timeoutSeconds = 10, label = "ECE proposed-move endpoint")

  private def callEcePositionEcs(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EcePositionEcsRequest
  ): Either[String, JsValue] =
    postEceJson(config.positionUrl, ecePositionEcsRequestJson(request), timeoutSeconds = 25, label = "ECE position endpoint")

  private def callEcePositionEcsFirst(
      configs: List[EngineGateway.EceServiceConfig],
      request: EngineGateway.EcePositionEcsRequest
  ): Either[String, (EngineGateway.EceServiceConfig, JsValue)] =
    var latest: Either[String, (EngineGateway.EceServiceConfig, JsValue)] = Left("ECE position unavailable")
    configs.foreach: config =>
      if latest.isLeft then
        latest = callEcePositionEcs(config, request).map(json => config -> json)
    latest

  private def callEcePotentialEcs(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EcePotentialEcsRequest
  ): Either[String, JsValue] =
    postEceJson(config.potentialUrl, ecePotentialEcsRequestJson(request), timeoutSeconds = 10, label = "ECE potential endpoint")

  private def callEceGameReview(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceGameReviewRequest
  ): Either[String, JsValue] =
    postEceJson(config.fullMatchUrl, eceGameReviewRequestJson(request), timeoutSeconds = 45, label = "ECE full-match endpoint")

  private def callEceFullMatchSummary(
      config: EngineGateway.EceServiceConfig,
      request: JsObject
  ): Either[String, JsValue] =
    postEceJson(config.fullMatchSummaryUrl, request, timeoutSeconds = 45, label = "ECE full-match-summary endpoint")

  private def callEceFullMatchSummaryFirst(
      configs: List[EngineGateway.EceServiceConfig],
      request: JsObject
  ): Either[String, JsValue] =
    var latest: Either[String, JsValue] = Left("ECE full-match summary unavailable")
    configs.foreach: config =>
      if latest.isLeft then latest = callEceFullMatchSummary(config, request)
    latest

  private def callEcePerformanceSummary(
      config: EngineGateway.EceServiceConfig,
      request: JsObject
  ): Either[String, JsValue] =
    postEceJson(config.performanceSummaryUrl, request, timeoutSeconds = 45, label = "ECE performance-summary endpoint")

  private def postEceJson(
      url: String,
      body: JsObject,
      timeoutSeconds: Int,
      label: String
  ): Either[String, JsValue] =
    try
      val httpRequest =
        JHttpRequest
          .newBuilder(URI.create(url))
          .timeout(Duration.ofSeconds(timeoutSeconds))
          .header("content-type", "application/json")
          .POST(JHttpRequest.BodyPublishers.ofString(Json.stringify(body)))
          .build()
      val response = eceHttpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
      if response.statusCode() >= 200 && response.statusCode() < 300 then
        try Right(Json.parse(response.body()))
        catch case error: Exception => Left(s"ECE returned invalid JSON: ${error.getMessage}")
      else Left(s"$label returned HTTP ${response.statusCode()}")
    catch case error: Exception => Left(Option(error.getMessage).filter(_.nonEmpty).getOrElse(error.getClass.getSimpleName))

  private def eceBoardRequestJson(request: EngineGateway.EceBoardStateRequest): JsObject =
    Json.obj(
      "request" -> Json.obj(
        "mode" -> request.mode,
        "request_id" -> request.requestId,
        "input_fen" -> request.inputFen,
        "rating_type" -> request.ratingType,
        "white_rating_input" -> request.whiteRatingInput,
        "black_rating_input" -> request.blackRatingInput,
        "white_level" -> request.whiteLevel.value,
        "black_level" -> request.blackLevel.value,
        "use_ai" -> request.useAi,
        "custom" -> Json.obj(
          "opening" -> request.custom.opening,
          "instructions" -> request.custom.instructions
        )
      )
    )

  private def eceProposedMoveRequestJson(request: EngineGateway.EceProposedMoveRequest): JsObject =
    Json.obj(
      "request" -> (
        Json.obj(
          "mode" -> request.mode,
          "request_id" -> request.requestId,
          "input_fen" -> request.inputFen,
          "proposed_move_uci" -> request.proposedMoveUci,
          "rating_type" -> request.ratingType,
          "white_rating_input" -> request.whiteRatingInput,
          "black_rating_input" -> request.blackRatingInput,
          "white_level" -> request.whiteLevel.value,
          "black_level" -> request.blackLevel.value,
          "use_ai" -> request.useAi,
          "requested_modules" -> Json.arr("stockfish"),
          "custom" -> Json.obj(
            "opening" -> request.custom.opening,
            "instructions" -> request.custom.instructions
          )
        ) ++ request.proposedMoveSan.fold(Json.obj())(san => Json.obj("proposed_move_san" -> san))
      )
    )

  private def ecePositionEcsRequestJson(request: EngineGateway.EcePositionEcsRequest): JsObject =
    Json.obj(
      "request" -> (
        Json.obj(
          "mode" -> request.mode,
          "request_id" -> request.requestId,
          "input_fen" -> request.inputFen,
          "user_side" -> perspectiveKey(request.userSide),
          "rating_type" -> request.ratingType,
          "white_rating_input" -> request.whiteRatingInput,
          "black_rating_input" -> request.blackRatingInput,
          "white_level" -> request.whiteLevel.value,
          "black_level" -> request.blackLevel.value,
          "use_ai" -> request.useAi,
          "consume_cache" -> request.consumeCache,
          "custom" -> Json.obj(
            "opening" -> request.custom.opening,
            "instructions" -> request.custom.instructions
          )
        ) ++ request.positionEcsId.fold(Json.obj())(id => Json.obj("position_ecs_id" -> id))
      )
    )

  private def ecePotentialEcsRequestJson(request: EngineGateway.EcePotentialEcsRequest): JsObject =
    Json.obj(
      "request" -> Json.obj(
        "mode" -> request.mode,
        "request_id" -> request.requestId,
        "input_fen" -> request.inputFen,
        "white_level" -> request.whiteLevel.value,
        "black_level" -> request.blackLevel.value,
        "stockfish" -> Json.obj(
          "depth" -> request.stockfishDepth,
          "timeout_ms" -> request.stockfishTimeoutMillis
        ),
        "custom" -> Json.obj(
          "opening" -> request.custom.opening,
          "instructions" -> request.custom.instructions
        )
      )
    )

  private def eceGameReviewRequestJson(request: EngineGateway.EceGameReviewRequest): JsObject =
    Json.obj(
      "request" -> Json.obj(
        "mode" -> request.mode,
        "request_id" -> request.requestId,
        "game" -> Json.obj(
          "game_id" -> request.game.gameId,
          "initial_fen" -> request.game.initialFen,
          "pgn" -> request.game.pgn,
          "moves" -> request.game.moves,
          "fen_history" -> request.game.fenHistory,
          "result" -> request.game.result,
          "termination" -> request.game.termination
        ),
        "rating_type" -> request.ratingType,
        "white_rating_input" -> request.whiteRatingInput,
        "black_rating_input" -> request.blackRatingInput,
        "review_level" -> request.reviewLevel.value,
        "use_ai" -> request.useAi,
        "custom" -> Json.obj(
          "opening" -> request.custom.opening,
          "instructions" -> request.custom.instructions
        ),
        "live_ece_snapshots" -> request.liveEceSnapshots.map(snapshot =>
          Json.obj(
            "ply" -> snapshot.ply,
            "fen" -> snapshot.fen,
            "side_to_move" -> perspectiveKey(snapshot.sideToMove),
            "white_output_ref" -> snapshot.whiteOutputRef,
            "black_output_ref" -> snapshot.blackOutputRef
          )
        )
      )
    )

  private def testGroundEceProposedMovePayload(
      config: EngineGateway.EceServiceConfig,
      gameId: String,
      playerId: String,
      ply: Int,
      fen: String,
      requesterSide: Perspective,
      request: EngineGateway.EceProposedMoveRequest
  ): Either[JsObject, JsObject] =
    callEceProposedMove(config, request) match
      case Left(error) =>
        Left(eceBridgeErrorJson("ece_proposed_move_unavailable", error, config, request))
      case Right(eceJson) =>
        parseEceProposedMovePayload(config, eceJson, request, gameId, playerId, ply, fen, requesterSide) match
          case Left(error)    => Left(eceBridgeErrorJson("ece_proposed_move_rejected", error, config, request))
          case Right(payload) => Right(payload)

  private def testGroundEcePotentialMovePayload(
      config: EngineGateway.EceServiceConfig,
      gameId: String,
      playerId: String,
      ply: Int,
      fen: String,
      kind: String,
      revealSide: Perspective,
      quota: Int,
      request: EngineGateway.EcePotentialEcsRequest,
      key: String
  ): Either[JsObject, JsObject] =
    callEcePotentialEcs(config, request) match
      case Left(error) =>
        Left(eceBridgeErrorJson("ece_unavailable", error, config, request))
      case Right(eceJson) =>
        parseEcePotentialEcsPayload(config, eceJson, request, gameId, playerId, ply, fen, kind, revealSide, quota, key) match
          case Left(error)    => Left(eceBridgeErrorJson("ece_potential_ecs_rejected", error, config, request))
          case Right(payload) => Right(payload)

  private def testGroundEcePositionEcsPayload(
      config: EngineGateway.EceServiceConfig,
      gameId: String,
      playerId: String,
      ply: Int,
      fen: String,
      requesterSide: Perspective,
      request: EngineGateway.EcePositionEcsRequest,
      key: String
  ): Either[JsObject, JsObject] =
    callEcePositionEcs(config, request) match
      case Left(error) =>
        Left(eceBridgeErrorJson("ece_position_ecs_unavailable", error, config, request))
      case Right(eceJson) =>
        parseEcePositionEcsPayload(config, eceJson, request, gameId, playerId, ply, fen, requesterSide, key) match
          case Left(error)    => Left(eceBridgeErrorJson("ece_position_ecs_rejected", error, config, request))
          case Right(payload) => Right(payload)

  private def testGroundEceConfig(using Context): EngineGateway.EceServiceConfig =
    val baseUrl =
      get("eceBaseUrl")
        .map(EngineGateway.EceServiceConfig.normalizeBaseUrl)
        .filter(url => EngineGateway.EceServiceConfig(baseUrl = url).valid)
        .getOrElse(EngineGateway.EceServiceConfig.defaultBaseUrl)
    EngineGateway.EceServiceConfig(baseUrl = baseUrl)

  private def parseEceBoardPayload(json: JsValue): Either[String, ParsedEceBoardPayload] =
    val echo = field(json, "request_echo").getOrElse(Json.obj())
    val sideOutputs = field(json, "side_outputs").getOrElse(Json.obj())
    for
      requestId <- stringField(echo, "request_id").toRight("missing request_echo.request_id")
      inputFen <- stringField(echo, "input_fen").toRight("missing request_echo.input_fen")
      whiteLevel <- levelField(echo, "white_level").toRight("missing or invalid request_echo.white_level")
      blackLevel <- levelField(echo, "black_level").toRight("missing or invalid request_echo.black_level")
      white <- parseSidePayload(sideOutputs, "white", Perspective.White)
      black <- parseSidePayload(sideOutputs, "black", Perspective.Black)
    yield
      val diagnostics = parseDiagnostics(json)
      val positionContext = parsePositionContext(json)
      val phase = stringField(field(json, "schema").getOrElse(Json.obj()), "phase").getOrElse("standard_ecs")
      val response =
        EngineGateway.EceBoardStateResponse(
          requestEcho = EngineGateway.EceRequestEcho(requestId, inputFen, whiteLevel, blackLevel),
          white = Some(white.output),
          black = Some(black.output),
          diagnostics = diagnostics,
          hasPublicPosition = field(json, "position").isDefined,
          hasPublicSharedCalculations = field(json, "shared_calculations").isDefined,
          rawProviderPayload = field(json, "raw_provider_output").map(_.toString)
        )
      ParsedEceBoardPayload(
        response = response,
        white = white,
        black = black,
        fieldsDetected = Json.obj(
          "phase" -> phase,
          "white" -> white.fieldsDetected,
          "black" -> black.fieldsDetected,
          "diagnostics_status" -> diagnostics.status.toString,
          "position_ecs" -> Json.obj(
            "position_ecs_id" -> positionContext.flatMap(_.positionEcsId),
            "status" -> positionContext.flatMap(_.status),
            "expires_at_ms" -> positionContext.flatMap(_.expiresAtMs),
            "endpoint" -> positionContext.flatMap(_.endpoint)
          ),
          "has_public_position" -> response.hasPublicPosition,
          "has_public_shared_calculations" -> response.hasPublicSharedCalculations
        ),
        positionContext = positionContext,
        phase = phase
      )

  private def parsePositionContext(json: JsValue): Option[ParsedEcePositionContext] =
    val standard = field(json, "standard").getOrElse(Json.obj())
    val standardEcs = field(json, "standard_ecs").getOrElse(Json.obj())
    val contexts =
      List(
        objectField(json, "position_ecs"),
        objectField(standard, "position_ecs"),
        objectField(standardEcs, "position_ecs")
      ).flatten
    val echoes =
      List(
        objectField(json, "request_echo"),
        objectField(standard, "request_echo"),
        objectField(standardEcs, "request_echo")
      ).flatten
    val positionEcsId =
      contexts.flatMap(context => stringField(context, "position_ecs_id")).headOption
        .orElse(echoes.flatMap(echo => stringField(echo, "position_ecs_id")).headOption)
    val status = contexts.flatMap(context => stringField(context, "status")).headOption
    val expiresAtMs = contexts.flatMap(context => longField(context, "expires_at_ms")).headOption
    val endpoint = contexts.flatMap(context => stringField(context, "endpoint")).headOption
    Option.when(positionEcsId.isDefined || status.isDefined || expiresAtMs.isDefined || endpoint.isDefined):
      ParsedEcePositionContext(
        positionEcsId = positionEcsId,
        status = status,
        expiresAtMs = expiresAtMs,
        endpoint = endpoint
      )

  private def parseSidePayload(
      sideOutputs: JsValue,
      key: String,
      defaultSide: Perspective,
      includeEval: Boolean = false
  ): Either[String, ParsedEceSidePayload] =
    field(sideOutputs, key).toRight(s"missing side_outputs.$key").flatMap: sideJson =>
      val levelJson = field(sideJson, "level").getOrElse(Json.obj())
      val requested = levelField(levelJson, "requested").orElse(levelField(levelJson, "requested_level")).getOrElse(Level(10))
      val delivered = levelField(levelJson, "delivered").orElse(levelField(levelJson, "delivered_level")).getOrElse(requested)
      val side = stringField(sideJson, "side").flatMap(parsePerspective).getOrElse(defaultSide)
      val studentSide = stringField(sideJson, "student_side").flatMap(parsePerspective).getOrElse(side)
      val opponentSide =
        stringField(sideJson, "opponent_side").flatMap(parsePerspective).getOrElse(opponentOf(side))
      val output =
        EngineGateway.EceSideOutput(
          side = side,
          studentSide = studentSide,
          opponentSide = opponentSide,
          level = EngineGateway.EceLevelEcho(
            requestedLevel = requested,
            deliveredLevel = delivered,
            defaulted = booleanField(levelJson, "defaulted").getOrElse(false)
          ),
          isSideToMove = booleanField(sideJson, "is_side_to_move").getOrElse(false),
          summary = safePayloadCoachTextField(sideJson, "summary"),
          immediateWarning = safePayloadCoachTextField(sideJson, "immediate_warning"),
          plan = safePayloadCoachTextField(sideJson, "plan")
        )

      Either.cond(
        output.valid,
        ParsedEceSidePayload(
          output = output,
          atoms = parseAtoms(sideJson, side),
          extras = parseDisplayExtras(sideJson, includeEval = includeEval),
          auditId = auditIdFrom(sideJson).getOrElse(s"ece-${key}-${System.currentTimeMillis}"),
          fieldsDetected = sideFieldsDetected(sideJson)
        ),
        s"invalid side output for $key"
      )

  private def parseDiagnostics(json: JsValue): EngineGateway.EceDiagnostics =
    val diagnostics = field(json, "diagnostics").getOrElse(Json.obj())
    EngineGateway.EceDiagnostics(
      status = diagnosticsStatus(stringField(diagnostics, "status").getOrElse("internal_error")),
      engineVersion =
        stringField(diagnostics, "engine_version")
          .orElse(stringField(json, "engine_version"))
          .getOrElse("unknown"),
      sanitizedMessage = safeTextField(diagnostics, "message", 80).orElse(safeTextField(diagnostics, "mode", 80))
    )

  private def parseAtoms(sideJson: JsValue, requesterSide: Perspective): MockDisplayOverlayAtoms =
    val overlays = field(sideJson, "overlays").getOrElse(Json.obj())
    val tradeStatus = field(overlays, "trade_status").getOrElse(Json.obj())
    val threatGroups = field(overlays, "threats").getOrElse(Json.obj())
    val pinGroups = field(overlays, "pinned_pieces").getOrElse(Json.obj())
    val hanging = arrayField(overlays, "hanging_pieces") ++ arrayField(tradeStatus, "hanging_pieces")
    val explicitHangingAttackable = arrayField(tradeStatus, "hanging_attackable")
    val explicitHangingNotAttackable = arrayField(tradeStatus, "hanging_not_attackable")
    val explicitStudentHangingAttackable =
      arrayField(overlays, "student_hanging_attackable") ++
        arrayField(overlays, "student_hanging_pieces_attackable") ++
        arrayField(tradeStatus, "student_hanging_attackable") ++
        arrayField(tradeStatus, "student_hanging_pieces_attackable")
    val explicitOpponentHangingAttackable =
      arrayField(overlays, "opponent_hanging_attackable") ++
        arrayField(overlays, "opponent_hanging_pieces_attackable") ++
        arrayField(tradeStatus, "opponent_hanging_attackable") ++
        arrayField(tradeStatus, "opponent_hanging_pieces_attackable")
    val offset = arrayField(overlays, "offset_count") ++ arrayField(tradeStatus, "offset_count")
    val flatThreats =
      threatArrayField(overlays, "threats") ++
        threatArrayField(sideJson, "threats")
    val pins = arrayField(overlays, "pins") ++
      arrayField(pinGroups, "student_pinned") ++
      arrayField(pinGroups, "opponent_pinned")
    val opponent = opponentOf(requesterSide)
    val studentThreats =
      threatArrayField(threatGroups, "student_threats") ++
        threatArrayField(threatGroups, "student") ++
        threatArrayField(overlays, "student_threats") ++
        threatArrayField(overlays, "student") ++
        threatArrayField(sideJson, "student_threats") ++
        threatArrayField(sideJson, "student") ++
        threatArrayField(threatGroups, "player_threats") ++
        threatArrayField(threatGroups, "player") ++
        threatArrayField(overlays, "player_threats") ++
        threatArrayField(overlays, "player") ++
        threatArrayField(sideJson, "player_threats") ++
        threatArrayField(sideJson, "player") ++
        flatThreats.filter(item => threatSideField(item).forall(_ == requesterSide))
    val opponentThreats =
      threatArrayField(threatGroups, "opponent_threats") ++
        threatArrayField(threatGroups, "opponent") ++
        threatArrayField(overlays, "opponent_threats") ++
        threatArrayField(overlays, "opponent") ++
        threatArrayField(sideJson, "opponent_threats") ++
        threatArrayField(sideJson, "opponent") ++
        flatThreats.filter(item => threatSideField(item).contains(opponent))
    val hangingAttackableItems = hanging.filter(hangingItemIsAttackable) ++ explicitHangingAttackable
    val studentHangingAttackable =
      (
        hangingAttackableItems.filter(item => ownerField(item, requesterSide).contains(requesterSide)) ++
          explicitStudentHangingAttackable
      ).flatMap(squareFrom).distinct
    val opponentHangingAttackable =
      (
        hangingAttackableItems.filter(item => ownerField(item, requesterSide).contains(opponent)) ++
          explicitOpponentHangingAttackable
      ).flatMap(squareFrom).distinct
    val genericHangingAttackable =
      hangingAttackableItems.filter(item => ownerField(item, requesterSide).isEmpty).flatMap(squareFrom).distinct

    MockDisplayOverlayAtoms(
      hangingAttackable = genericHangingAttackable,
      hangingNotAttackable =
        (hanging.filter(hangingItemIsNotAttackable) ++ explicitHangingNotAttackable).flatMap(squareFrom).distinct,
      offsetCount =
        (offset.flatMap: item =>
          for
            square <- offsetSquareFrom(item)
            count <- offsetCountValue(item)
          yield square -> count
        ).distinct,
      studentThreats = studentThreats.flatMap(fromTo).distinct,
      opponentThreats = opponentThreats.flatMap(fromTo).distinct,
      pins =
        (pins.flatMap: item =>
          squareFromKeys(item, "pinned_square", "piece_square", "square").map: pinned =>
            val pinning =
              squareFromKeys(item, "pinning_piece_square", "pinning_square", "attacker_square", "from", "from_square")
                .getOrElse(pinned)
            val target = squareFromKeys(item, "target_square", "king_square", "to", "to_square").getOrElse(pinned)
            (pinned, pinning, target)
        ).distinct,
      studentHangingAttackable = studentHangingAttackable,
      opponentHangingAttackable = opponentHangingAttackable
    )

  private def parseDisplayExtras(
      sideJson: JsValue,
      includeEval: Boolean,
      evalFeatureKey: String = "ece.eval"
  ): ParsedEceDisplayExtras =
    val overlays = field(sideJson, "overlays").getOrElse(Json.obj())
    val openingCards =
      (arrayField(overlays, "opening") ++ objectField(sideJson, "opening").toList).take(1).flatMap: item =>
        firstSafeText(item, 80, "detected_opening", "name", "opening", "line").map: name =>
          val move = firstSafeText(item, 16, "move", "uci").fold("")(m => s"$m: ")
          EceLiveBridge.ExtraCard("ece.opening", "Opening", s"$move$name")

    val proposedCards =
      field(sideJson, "proposed_move_preview").toList.flatMap: item =>
        for
          uci <- safeTextField(item, "uci", 16)
          verdict <- safeTextField(item, "verdict", 80)
        yield EceLiveBridge.ExtraCard("ece.proposed_move_preview", "Proposed Move Preview", s"$uci is $verdict.")

    val reviewCards =
      field(sideJson, "review_modes").toList.map: item =>
        val liveModes =
          List(
            Option.when(booleanField(item, "live_white_available").contains(true))("Live White"),
            Option.when(booleanField(item, "live_black_available").contains(true))("Live Black"),
            Option.when(booleanField(item, "live_both_available").contains(true))("Live Both"),
            Option.when(booleanField(item, "full_game_available").contains(true))("Full Game")
          ).flatten.mkString(", ")
        val custom = if booleanField(item, "custom_token_required").contains(true) then " Custom analysis requires quota." else ""
        EceLiveBridge.ExtraCard("ece.review_modes", "Review Modes", s"${if liveModes.nonEmpty then liveModes else "No review modes"}.${custom}")

    val evalVisuals =
      if includeEval then
        (field(overlays, "eval").toList ++ field(sideJson, "evaluation").toList)
          .flatMap(evalVisualFrom(_, evalFeatureKey))
      else Nil

    val humanRiskCards =
      objectField(sideJson, "human_risk").toList.flatMap: item =>
        safeCoachTextField(item, "risk_summary_input").map: body =>
          EceLiveBridge.ExtraCard("ece.human_risk", "Human Risk", body)

    ParsedEceDisplayExtras(
      cards = openingCards ++ proposedCards ++ reviewCards ++ humanRiskCards,
      visuals = evalVisuals
    )

  private def eceBridgeSuccessJson(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceBoardStateRequest,
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      result: EceLiveBridge.BoardOverlayResult,
      parsed: ParsedEceBoardPayload,
      sidePayload: ParsedEceSidePayload,
      ttlMillis: Int
  ): JsObject =
    val now = System.currentTimeMillis
    val baseLive = roundOverlayJson(result.roundPayload, ttlMillis, now)
    val liveWithPositionContext = parsed.positionContext.fold(baseLive)(context => liveWithPositionContextJson(baseLive, context))
    val live = liveDisplayJson(gameId, playerId).fold(liveWithPositionContext)(display => liveWithPositionContext ++ Json.obj("display" -> display))
    Json.obj(
      "ok" -> result.valid,
      "coachingAvailable" -> result.coachingAvailable,
      "browserDirectCallBlocked" -> result.decision.browserDirectCallBlocked,
      "nonFatalToGameLifecycle" -> result.decision.nonFatalToGameLifecycle,
      "requesterSide" -> perspectiveKey(requesterSide),
      "ece" -> Json.obj(
        "boardUrl" -> config.standardUrl,
        "standardUrl" -> config.standardUrl,
        "requestId" -> request.requestId,
        "mode" -> request.mode,
        "phase" -> parsed.phase,
        "positionEcs" -> Json.obj(
          "positionEcsId" -> parsed.positionContext.flatMap(_.positionEcsId),
          "status" -> parsed.positionContext.flatMap(_.status),
          "expiresAtMs" -> parsed.positionContext.flatMap(_.expiresAtMs),
          "endpoint" -> parsed.positionContext.flatMap(_.endpoint)
        ),
        "diagnostics" -> Json.obj(
          "status" -> parsed.response.diagnostics.status.toString,
          "label" -> parsed.response.diagnostics.status.sanitizedUserLabel,
          "engineVersion" -> parsed.response.diagnostics.engineVersion
        ),
        "acceptedStatus" -> result.decision.status.toString
      ),
      "roundData" -> Json.obj("evenchess" -> Json.obj("live" -> live)),
      "live" -> live,
      "display" -> Json.obj(
        "cards" -> result.roundPayload.cards.size,
        "visuals" -> result.roundPayload.visuals.size,
        "renderableCards" -> result.roundPayload.renderableCards.size,
        "renderableVisuals" -> result.roundPayload.renderableVisuals.size,
        "clearReason" -> result.roundPayload.clearReason,
        "modeBadge" -> result.display.map(_.modeBadge)
      ),
      "fieldsDetected" -> parsed.fieldsDetected,
      "selectedSideFieldsDetected" -> sidePayload.fieldsDetected
    )

  private def liveDisplayJson(gameId: String, playerId: String): Option[JsObject] =
    gamePolicyRepository.get(gameId).flatMap: stored =>
      stored.record.playerPolicyFor(playerId).map: playerPolicy =>
        val currentUsedLevel =
          stored
            .usedLevelFor(playerPolicy.playerId)
            .orElse(stored.displayStateFor(playerPolicy.playerId).map(_.appliedLevel))
            .map(level => math.min(level.value, playerPolicy.setLevel.value))
            .getOrElse(0)
        val currentDisplay =
          Json.obj(
            "setLevel" -> playerPolicy.setLevel.value,
            "usedLevel" -> currentUsedLevel
          ) ++ stored.displayStateFor(playerPolicy.playerId).fold(Json.obj())(displayStateJson)
        val opponentPolicy =
          if playerPolicy.playerId == stored.record.white.playerId then stored.record.black
          else stored.record.white
        val opponentUsedLevel =
          stored
            .usedLevelFor(opponentPolicy.playerId)
            .orElse(stored.displayStateFor(opponentPolicy.playerId).map(_.appliedLevel))
            .map(level => math.min(level.value, opponentPolicy.setLevel.value))
            .getOrElse(0)
        currentDisplay ++ Json.obj(
          "opponent" -> Json.obj(
            "setLevel" -> opponentPolicy.setLevel.value,
            "usedLevel" -> opponentUsedLevel
          )
        )

  private def adminUnlimitedTokensEnabled(using Context): Boolean =
    import lila.ui.Context.ctxMe
    env.web.settings.evenChessAdminUnlimitedTokens.get() && isGrantedOpt(_.Settings)

  private def hidePotentialMovePayload(
      payload: JsObject,
      gameId: String,
      requesterSide: Perspective,
      level: Level,
      ply: Int,
      adminUnlimitedTokens: Boolean = false
  ): JsObject =
    objectField(payload, "live") match
      case None => payload
      case Some(live) =>
        val filteredLive = liveWithAssistanceUsage(filterPotentialMoveLivePayload(live), gameId, requesterSide, level, ply, adminUnlimitedTokens)
        val roundData = objectField(payload, "roundData").getOrElse(Json.obj())
        val evenchess = objectField(roundData, "evenchess").getOrElse(Json.obj())
        payload ++ Json.obj(
          "live" -> filteredLive,
          "roundData" -> (roundData ++ Json.obj("evenchess" -> (evenchess ++ Json.obj("live" -> filteredLive)))),
          "display" -> Json.obj(
            "cards" -> arrayField(filteredLive, "cards").size,
            "visuals" -> arrayField(filteredLive, "visuals").size,
            "renderableCards" -> arrayField(filteredLive, "cards").size,
            "renderableVisuals" -> arrayField(filteredLive, "visuals").size,
            "clearReason" -> field(filteredLive, "clear"),
            "modeBadge" -> field(field(payload, "display").getOrElse(Json.obj()), "modeBadge")
          )
        )

  private val boardOverlayRecentGameLimit = 10
  private val boardOverlayMaxPositionsPerGame = 260

  private def storedReviewBoardOverlay(
      surface: String,
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      fen: String,
      ply: Int,
      level: Level,
      ttlMillis: Int,
      adminUnlimitedTokens: Boolean
  ): Option[JsObject] =
    if !Set("analysis", "study", "review").contains(surface) then None
    else
      postGameReviewStore
        .frameFor(playerId, gameId, ply, Some(fen), requesterSide)
        .orElse(postGameReviewStore.frameFor(playerId, gameId, ply, None, requesterSide))
        .flatMap(frame => objectField(frame, "payload"))
        .map: payload =>
          val now = System.currentTimeMillis
          val cached =
            CachedBoardOverlay(
              payload = payload,
              gameId = gameId,
              playerId = playerId,
              requesterSide = requesterSide,
              fen = fen,
              level = level,
              ply = ply,
              cachedAt = now
            )
          withCurrentLiveDisplay(
            refreshCachedBoardOverlayPayload(cached, level, ply, ttlMillis, now, adminUnlimitedTokens),
            gameId,
            playerId
          )

  private def cachedBoardOverlay(
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      fen: String,
      ply: Int,
      requestedLevel: Level,
      ttlMillis: Int,
      adminUnlimitedTokens: Boolean = false
  ): Option[JsObject] =
    val key = boardOverlayCacheKey(gameId, playerId, requesterSide, fen)
    val now = System.currentTimeMillis
    boardOverlayCacheLock.synchronized:
      boardOverlayCache.get(key).filter(_.level.value >= requestedLevel.value).map: cached =>
        val refreshed = cached.copy(cachedAt = now)
        boardOverlayCache.update(key, refreshed)
        withCurrentLiveDisplay(
          refreshCachedBoardOverlayPayload(refreshed, requestedLevel, ply, ttlMillis, now, adminUnlimitedTokens),
          gameId,
          playerId
        )

  private def rememberBoardOverlay(
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      fen: String,
      level: Level,
      ply: Int,
      payload: JsObject
  ): JsObject =
    val now = System.currentTimeMillis
    val entry =
      CachedBoardOverlay(
        payload = cacheMetadataPayload(payload, hit = false, level, level, ply, now),
        gameId = gameId,
        playerId = playerId,
        requesterSide = requesterSide,
        fen = fen,
        level = level,
        ply = ply,
        cachedAt = now
      )
    boardOverlayCacheLock.synchronized:
      boardOverlayCache.update(boardOverlayCacheKey(gameId, playerId, requesterSide, fen), entry)
      pruneBoardOverlayCache(playerId, requesterSide, gameId)
    entry.payload

  private def refreshCachedBoardOverlayPayload(
      cached: CachedBoardOverlay,
      requestedLevel: Level,
      ply: Int,
      ttlMillis: Int,
      now: Long,
      adminUnlimitedTokens: Boolean = false
  ): JsObject =
    val live =
      objectField(cached.payload, "live")
        .map(live =>
          val restamped =
            live ++ Json.obj(
            "ply" -> ply,
            "ttlMillis" -> ttlMillis,
            "stale" -> false,
            "createdAt" -> now,
            "expiresAt" -> (now + ttlMillis),
            "cards" -> restampBoardOverlayItems(arrayField(live, "cards"), ply),
            "visuals" -> restampBoardOverlayItems(arrayField(live, "visuals"), ply),
            "clear" -> restampBoardOverlayItems(arrayField(live, "clear"), ply)
          )
          liveWithAssistanceUsage(restamped, cached.gameId, cached.requesterSide, requestedLevel, ply, adminUnlimitedTokens)
        )
    val refreshed =
      live.fold(cached.payload)(updatedLive => withBoardOverlayLive(cached.payload, updatedLive))
    cacheMetadataPayload(refreshed, hit = true, cached.level, requestedLevel, ply, now)

  private def cacheMetadataPayload(
      payload: JsObject,
      hit: Boolean,
      storedLevel: Level,
      requestedLevel: Level,
      ply: Int,
      at: Long
  ): JsObject =
    payload ++ Json.obj(
      "cache" -> Json.obj(
        "hit" -> hit,
        "scope" -> "game_fen",
        "storedLevel" -> storedLevel.value,
        "requestedLevel" -> requestedLevel.value,
        "ply" -> ply,
        "updatedAt" -> at
      )
    )

  private def withBoardOverlayLive(payload: JsObject, live: JsObject): JsObject =
    val roundData = objectField(payload, "roundData").getOrElse(Json.obj())
    val evenchess = objectField(roundData, "evenchess").getOrElse(Json.obj())
    payload ++ Json.obj(
      "live" -> live,
      "roundData" -> (roundData ++ Json.obj("evenchess" -> (evenchess ++ Json.obj("live" -> live))))
    )

  private def withCurrentLiveDisplay(payload: JsObject, gameId: String, playerId: String): JsObject =
    (objectField(payload, "live"), liveDisplayJson(gameId, playerId)) match
      case (Some(live), Some(display)) => withBoardOverlayLive(payload, live ++ Json.obj("display" -> display))
      case _                           => payload

  private def restampBoardOverlayItems(items: List[JsValue], ply: Int): List[JsValue] =
    items.map:
      case obj: JsObject => obj ++ Json.obj("ply" -> ply)
      case item          => item

  private def boardOverlayHistoryJson(
      gameId: String,
      playerId: String,
      requesterSide: Perspective
  ): JsObject =
    val owner = boardOverlayOwnerKey(playerId, requesterSide)
    val frames =
      boardOverlayCacheLock.synchronized:
        boardOverlayCache.values.toList
          .filter(entry => entry.gameId == gameId && boardOverlayOwnerKey(entry.playerId, entry.requesterSide) == owner)
          .sortBy(entry => (entry.ply, entry.fen, entry.cachedAt))
          .map: entry =>
            Json.obj(
              "gameId" -> entry.gameId,
              "playerId" -> entry.playerId,
              "side" -> perspectiveKey(entry.requesterSide),
              "moveNumber" -> moveNumberForPly(entry.ply),
              "ply" -> entry.ply,
              "fen" -> entry.fen,
              "level" -> entry.level.value,
              "cachedAt" -> entry.cachedAt,
              "payload" -> objectField(entry.payload, "live")
            )
    Json.obj(
      "ok" -> true,
      "gameId" -> gameId,
      "playerId" -> playerId,
      "side" -> perspectiveKey(requesterSide),
      "format" -> "evenchess-pgn-history-v1",
      "frames" -> frames
    )

  private def moveNumberForPly(ply: Int): Int =
    math.max(0, (ply + 1) / 2)

  private def boardOverlayCacheKey(
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      fen: String
  ): String =
    s"${boardOverlayOwnerKey(playerId, requesterSide)}:$gameId:$fen"

  private def boardOverlayOwnerKey(playerId: String, requesterSide: Perspective): String =
    s"$playerId:${perspectiveKey(requesterSide)}"

  private def pruneBoardOverlayCache(playerId: String, requesterSide: Perspective, activeGameId: String): Unit =
    val owner = boardOverlayOwnerKey(playerId, requesterSide)
    val ownerEntries =
      boardOverlayCache.toList.filter { case (_, entry) =>
        boardOverlayOwnerKey(entry.playerId, entry.requesterSide) == owner
      }
    val keptGames =
      ownerEntries
        .groupBy(_._2.gameId)
        .view
        .mapValues(_.map(_._2.cachedAt).max)
        .toList
        .sortBy { case (gameId, lastSeen) => (if gameId == activeGameId then Long.MaxValue else lastSeen) * -1 }
        .take(boardOverlayRecentGameLimit)
        .map(_._1)
        .toSet
    ownerEntries.collect { case (key, entry) if !keptGames(entry.gameId) => key }.foreach(boardOverlayCache.remove)

    val activeEntries =
      boardOverlayCache.toList
        .filter { case (_, entry) =>
          boardOverlayOwnerKey(entry.playerId, entry.requesterSide) == owner && entry.gameId == activeGameId
        }
        .sortBy(_._2.cachedAt)
    activeEntries
      .take(math.max(0, activeEntries.size - boardOverlayMaxPositionsPerGame))
      .map(_._1)
      .foreach(boardOverlayCache.remove)

  private def liveWithAssistanceUsage(
      live: JsObject,
      gameId: String,
      requesterSide: Perspective,
      level: Level,
      ply: Int,
      adminUnlimitedTokens: Boolean = false
  ): JsObject =
    val positionUsage = positionEcsUsage(gameId, requesterSide, level, ply, adminUnlimitedTokens = adminUnlimitedTokens)
    val existingAssistance = objectField(live, "assistance").getOrElse(Json.obj())
    val existingPositionEcs =
      objectField(existingAssistance, "positionEcs").getOrElse(Json.obj())
    live ++ Json.obj(
      "assistance" -> (existingAssistance ++ Json.obj(
        "proposedMove" -> Json.obj(
          "consumed" -> proposedMoveConsumed(gameId, requesterSide),
          "quota" -> proposedMoveQuotaForLevel(level, adminUnlimitedTokens),
          "adminUnlimitedTokens" -> adminUnlimitedTokens
        ),
        "potentialMoves" -> Json.obj(
          "consumedByKind" -> Json.obj(
            "player" -> potentialMoveConsumed(gameId, requesterSide, "player"),
            "opponent" -> potentialMoveConsumed(gameId, requesterSide, "opponent")
          ),
          "quotaByKind" -> Json.obj(
            "player" -> potentialMoveQuotaForLevel(level, "player", adminUnlimitedTokens),
            "opponent" -> potentialMoveQuotaForLevel(level, "opponent", adminUnlimitedTokens)
          ),
          "adminUnlimitedTokens" -> adminUnlimitedTokens
        ),
        "positionEcs" -> (existingPositionEcs ++ positionEcsUsageJson(positionUsage))
      ))
    )

  private def liveWithPositionContextJson(live: JsObject, context: ParsedEcePositionContext): JsObject =
    val contextJson =
      context.positionEcsId.fold(Json.obj())(id => Json.obj("positionEcsId" -> id)) ++
        context.status.fold(Json.obj())(status => Json.obj("status" -> status)) ++
        context.expiresAtMs.fold(Json.obj())(expiresAtMs => Json.obj("expiresAtMs" -> expiresAtMs)) ++
        context.endpoint.fold(Json.obj())(endpoint => Json.obj("endpoint" -> endpoint))
    if contextJson.value.isEmpty then live
    else
      val existingAssistance = objectField(live, "assistance").getOrElse(Json.obj())
      val existingPositionEcs = objectField(existingAssistance, "positionEcs").getOrElse(Json.obj())
      live ++ Json.obj("assistance" -> (existingAssistance ++ Json.obj("positionEcs" -> (existingPositionEcs ++ contextJson))))

  private def filterPotentialMoveLivePayload(live: JsObject): JsObject =
    live ++ Json.obj(
      "cards" -> arrayField(live, "cards").filterNot(isPotentialMoveDisplayItem),
      "visuals" -> arrayField(live, "visuals").filterNot(isPotentialMoveDisplayItem)
    )

  private def isPotentialMoveDisplayItem(item: JsValue): Boolean =
    val text = List(
      stringField(item, "featureKey"),
      stringField(item, "title"),
      stringField(item, "body"),
      stringField(item, "label")
    ).flatten.mkString(" ").toLowerCase
    text.contains("candidate") || text.contains("potential")

  private def proposedMoveQuotaForLevel(level: Level, adminUnlimitedTokens: Boolean = false): Int =
    if adminUnlimitedTokens then adminUnlimitedAssistanceQuota
    else if level.value >= 8 then 3
    else if level.value >= 6 then 2
    else if level.value >= 5 then 1
    else 0

  private def potentialMoveQuotaForLevel(level: Level, kind: String, adminUnlimitedTokens: Boolean = false): Int =
    if adminUnlimitedTokens then adminUnlimitedAssistanceQuota
    else if kind == "player" then
      if level.value >= 8 then 3
      else if level.value >= 7 then 2
      else if level.value >= 6 then 1
      else 0
    else if level.value >= 8 then 3
    else if level.value >= 7 then 2
    else if level.value >= 5 then 1
    else 0

  private def isAdminUnlimitedQuota(quota: Int): Boolean =
    quota >= adminUnlimitedAssistanceQuota

  private def positionEcsIntervalForLevel(level: Level): Int =
    level.value match
      case 4  => 10
      case 5  => 9
      case 6  => 8
      case 7  => 7
      case 8  => 6
      case 9  => 5
      case 10 => 4
      case _  => 0

  private def positionEcsOwnMovesForPly(ply: Int, requesterSide: Perspective): Int =
    if requesterSide == Perspective.White then (math.max(0, ply) + 1) / 2
    else math.max(0, ply) / 2

  private def positionEcsUsage(
      gameId: String,
      requesterSide: Perspective,
      level: Level,
      ply: Int,
      consumedOverride: Option[Int] = None,
      adminUnlimitedTokens: Boolean = false
  ): PositionEcsUsage =
    val interval = positionEcsIntervalForLevel(level)
    val ownMoves = positionEcsOwnMovesForPly(ply, requesterSide)
    val accrued = if interval > 0 then ownMoves / interval else 0
    PositionEcsUsage(
      level = level,
      interval = interval,
      ownMoves = ownMoves,
      accrued = accrued,
      consumed = consumedOverride.getOrElse(positionEcsConsumed(gameId, requesterSide)),
      adminUnlimitedTokens = adminUnlimitedTokens
    )

  private def positionEcsUsageJson(usage: PositionEcsUsage): JsObject =
    Json.obj(
      "level" -> usage.level.value,
      "interval" -> usage.interval,
      "ownMoves" -> usage.ownMoves,
      "accrued" -> usage.accrued,
      "quota" -> usage.quota,
      "consumed" -> usage.consumed,
      "available" -> usage.available,
      "startsWithFreeCall" -> usage.adminUnlimitedTokens,
      "adminUnlimitedTokens" -> usage.adminUnlimitedTokens
    )

  private def positionEcsNoTokenMessage(usage: PositionEcsUsage): String =
    if usage.adminUnlimitedTokens then "Ask AI is unlimited for admin debug."
    else if usage.interval <= 0 then "Ask AI starts at level 4"
    else
      val next = ((usage.ownMoves / usage.interval) + 1) * usage.interval
      s"Ask AI accrues every ${usage.interval} of your moves. Next call at $next moves."

  private def assistanceSidePrefix(gameId: String, requesterSide: Perspective): String =
    s"$gameId:${perspectiveKey(requesterSide)}:"

  private def assistanceSidePrefixFromKey(key: String): String =
    key.split(":", 3).take(2).mkString(":") + ":"

  private def proposedMoveTurnKey(
      gameId: String,
      requesterSide: Perspective,
      ply: Int,
      fen: String,
      level: Level
  ): String =
    s"${assistanceSidePrefix(gameId, requesterSide)}L${level.value}:$ply:$fen"

  private def proposedMoveCacheKey(
      gameId: String,
      requesterSide: Perspective,
      ply: Int,
      fen: String,
      level: Level,
      moveUci: String
  ): String =
    s"${proposedMoveTurnKey(gameId, requesterSide, ply, fen, level)}:uci:$moveUci"

  private def potentialMoveRevealKey(
      gameId: String,
      requesterSide: Perspective,
      ply: Int,
      fen: String,
      level: Level,
      kind: String
  ): String =
    s"${assistanceSidePrefix(gameId, requesterSide)}potential:$kind:L${level.value}:$ply:$fen"

  private def positionEcsCacheKey(
      gameId: String,
      requesterSide: Perspective,
      ply: Int,
      fen: String,
      level: Level
  ): String =
    s"${assistanceSidePrefix(gameId, requesterSide)}position-ecs:L${level.value}:$ply:$fen"

  private def proposedMoveConsumed(gameId: String, requesterSide: Perspective): Int =
    assistanceLock.synchronized:
      proposedTurnToMove.keys.count(_.startsWith(assistanceSidePrefix(gameId, requesterSide)))

  private def potentialMoveConsumed(gameId: String, requesterSide: Perspective, kind: String): Int =
    assistanceLock.synchronized:
      potentialMoveCache.keys.count(key => key.startsWith(assistanceSidePrefix(gameId, requesterSide)) && key.contains(s":potential:$kind:"))

  private def refundPotentialMove(key: String, gameId: String, requesterSide: Perspective, kind: String): Boolean =
    assistanceLock.synchronized:
      val expectedPrefix = assistanceSidePrefix(gameId, requesterSide)
      if key.startsWith(expectedPrefix) && key.contains(s":potential:$kind:") then potentialMoveCache.remove(key).isDefined
      else false

  private def positionEcsConsumed(gameId: String, requesterSide: Perspective): Int =
    assistanceLock.synchronized:
      positionEcsCache.keys.count(key => key.startsWith(assistanceSidePrefix(gameId, requesterSide)) && key.contains(":position-ecs:"))

  private def cachedProposedMove(
      cacheKey: String,
      gameId: String,
      requesterSide: Perspective,
      quota: Int
  ): Option[JsObject] =
    assistanceLock.synchronized:
      proposedMoveCache
        .get(cacheKey)
        .map(payload =>
          payload ++ Json.obj(
            "consumed" -> proposedMoveConsumed(gameId, requesterSide),
            "quota" -> quota,
            "adminUnlimitedTokens" -> isAdminUnlimitedQuota(quota)
          )
        )

  private def proposedTurnAlreadyUsed(turnKey: String, cacheKey: String): Boolean =
    assistanceLock.synchronized:
      proposedTurnToMove.get(turnKey).exists(_ != cacheKey)

  private def rememberProposedMove(cacheKey: String, turnKey: String, payload: JsObject, quota: Int): JsObject =
    if !approvedProposedMovePayload(payload) then payload
    else
      assistanceLock.synchronized:
        proposedTurnToMove.update(turnKey, cacheKey)
        val consumed = proposedTurnToMove.keys.count(_.startsWith(assistanceSidePrefixFromKey(turnKey)))
        val enriched =
          payload ++ Json.obj(
            "consumed" -> math.min(consumed, quota),
            "quota" -> quota,
            "adminUnlimitedTokens" -> isAdminUnlimitedQuota(quota)
          )
        proposedMoveCache.update(cacheKey, enriched)
        enriched

  private def cachedPotentialMove(key: String, quota: Int): Option[JsObject] =
    assistanceLock.synchronized:
      potentialMoveCache.get(key).map(payload => updatePotentialConsumed(payload, key, quota, cached = true))

  private def rememberPotentialMove(key: String, payload: JsObject): JsObject =
    if !approvedPotentialMovePayload(payload) then payload
    else
      assistanceLock.synchronized:
        val quota = objectField(payload, "potential").flatMap(potential => intField(potential, "quota")).getOrElse(0)
        val enriched = updatePotentialConsumed(payload, key, quota, cached = false)
        potentialMoveCache.update(key, enriched)
        enriched

  private def cachedPositionEcs(
      key: String,
      gameId: String,
      requesterSide: Perspective,
      level: Level,
      ply: Int,
      adminUnlimitedTokens: Boolean = false
  ): Option[JsObject] =
    assistanceLock.synchronized:
      positionEcsCache.get(key).map(payload =>
        updatePositionEcsUsage(payload, key, gameId, requesterSide, level, ply, cached = true, adminUnlimitedTokens = adminUnlimitedTokens)
      )

  private def rememberPositionEcs(
      key: String,
      payload: JsObject,
      gameId: String,
      requesterSide: Perspective,
      level: Level,
      ply: Int,
      adminUnlimitedTokens: Boolean = false
  ): JsObject =
    if !approvedPositionEcsPayload(payload) then payload
    else
      assistanceLock.synchronized:
        val enriched = updatePositionEcsUsage(payload, key, gameId, requesterSide, level, ply, cached = false, adminUnlimitedTokens = adminUnlimitedTokens)
        positionEcsCache.update(key, enriched)
        enriched

  private def approvedProposedMovePayload(payload: JsObject): Boolean =
    approvedActionPayload(payload, "proposed")

  private def approvedPotentialMovePayload(payload: JsObject): Boolean =
    approvedActionPayload(payload, "potential")

  private def approvedPositionEcsPayload(payload: JsObject): Boolean =
    approvedActionPayload(payload, "position")

  private def approvedActionPayload(payload: JsObject, actionField: String): Boolean =
    booleanField(payload, "ok").contains(true) &&
      objectField(payload, actionField).exists(action =>
        booleanField(action, "serverAuthorized").contains(true) &&
          booleanField(action, "approvedDisplayPayload").contains(true)
      )

  private def withProposedMoveUsage(payload: JsObject, gameId: String, requesterSide: Perspective, quota: Int): JsObject =
    rejectedActionPayload(payload, "proposed_move_payload_not_approved", "Proposed Move did not return an approved display payload") ++ Json.obj(
      "consumed" -> proposedMoveConsumed(gameId, requesterSide),
      "quota" -> quota,
      "adminUnlimitedTokens" -> isAdminUnlimitedQuota(quota)
    )

  private def withPotentialMoveUsage(payload: JsObject, gameId: String, requesterSide: Perspective, kind: String, quota: Int): JsObject =
    val rejected = rejectedActionPayload(payload, "potential_move_payload_not_approved", "Potential ECS did not return an approved reveal payload")
    objectField(payload, "potential") match
      case Some(potential) =>
        rejected ++ Json.obj(
          "potential" -> (potential ++ Json.obj(
            "consumed" -> potentialMoveConsumed(gameId, requesterSide, kind),
            "quota" -> quota,
            "adminUnlimitedTokens" -> isAdminUnlimitedQuota(quota)
          ))
        )
      case None =>
        rejected ++ Json.obj(
          "consumed" -> potentialMoveConsumed(gameId, requesterSide, kind),
          "quota" -> quota,
          "adminUnlimitedTokens" -> isAdminUnlimitedQuota(quota)
        )

  private def withPositionEcsUsage(
      payload: JsObject,
      gameId: String,
      requesterSide: Perspective,
      level: Level,
      ply: Int,
      adminUnlimitedTokens: Boolean = false
  ): JsObject =
    val usage = positionEcsUsage(gameId, requesterSide, level, ply, adminUnlimitedTokens = adminUnlimitedTokens)
    rejectedActionPayload(payload, "position_ecs_payload_not_approved", "Ask AI did not return an approved display payload") ++
      Json.obj("positionEcs" -> positionEcsUsageJson(usage))

  private def rejectedActionPayload(payload: JsObject, error: String, message: String): JsObject =
    payload ++
      Json.obj("ok" -> false) ++
      stringField(payload, "error").fold(Json.obj("error" -> error))(_ => Json.obj()) ++
      stringField(payload, "message").fold(Json.obj("message" -> message))(_ => Json.obj())

  private def updatePotentialConsumed(payload: JsObject, key: String, quota: Int, cached: Boolean): JsObject =
    val prefix = assistanceSidePrefixFromKey(key)
    val kind = key.split(":").dropWhile(_ != "potential").drop(1).headOption.getOrElse("player")
    val consumed = potentialMoveCache.keys.count(cacheKey => cacheKey.startsWith(prefix) && cacheKey.contains(s":potential:$kind:")) + Option.when(!potentialMoveCache.contains(key))(1).getOrElse(0)
    objectField(payload, "potential") match
      case None => payload
      case Some(potential) =>
        payload ++ Json.obj(
          "potential" -> (potential ++ Json.obj(
            "consumed" -> math.min(consumed, quota),
            "quota" -> quota,
            "adminUnlimitedTokens" -> isAdminUnlimitedQuota(quota),
            "cached" -> cached
          ))
        )

  private def updatePositionEcsUsage(
      payload: JsObject,
      key: String,
      gameId: String,
      requesterSide: Perspective,
      level: Level,
      ply: Int,
      cached: Boolean,
      adminUnlimitedTokens: Boolean = false
  ): JsObject =
    val consumed =
      positionEcsCache.keys.count(cacheKey => cacheKey.startsWith(assistanceSidePrefix(gameId, requesterSide)) && cacheKey.contains(":position-ecs:")) +
        Option.when(!positionEcsCache.contains(key))(1).getOrElse(0)
    val usage = positionEcsUsage(gameId, requesterSide, level, ply, consumedOverride = Some(consumed), adminUnlimitedTokens = adminUnlimitedTokens)
    objectField(payload, "position") match
      case None => payload
      case Some(position) =>
        val usageJson = positionEcsUsageJson(usage)
        payload ++ Json.obj(
          "position" -> (position ++ usageJson ++ Json.obj("cached" -> cached)),
          "positionEcs" -> usageJson,
          "consumed" -> usage.consumed,
          "quota" -> usage.quota,
          "accrued" -> usage.accrued,
          "available" -> usage.available,
          "interval" -> usage.interval,
          "ownMoves" -> usage.ownMoves,
          "adminUnlimitedTokens" -> usage.adminUnlimitedTokens
        )

  private def eceBridgeErrorJson(
      code: String,
      message: String,
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceBoardStateRequest
  ): JsObject =
    val safeMessage =
      Option(message).filter(_.nonEmpty).getOrElse("ECE request failed without a diagnostic message")
    Json.obj(
      "ok" -> false,
      "error" -> code,
      "message" -> safeMessage.take(240),
      "ece" -> Json.obj(
        "boardUrl" -> config.standardUrl,
        "standardUrl" -> config.standardUrl,
        "requestId" -> request.requestId,
        "mode" -> request.mode
      ),
      "browserDirectCallBlocked" -> !EngineGateway.EceEndpointPolicy.browserMayCallEceDirectly,
      "nonFatalToGameLifecycle" -> true
    )

  private def eceBridgeErrorJson(
      code: String,
      message: String,
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceProposedMoveRequest
  ): JsObject =
    val safeMessage =
      Option(message).filter(_.nonEmpty).getOrElse("ECE proposed-move request failed without a diagnostic message")
    Json.obj(
      "ok" -> false,
      "error" -> code,
      "message" -> safeMessage.take(240),
      "ece" -> Json.obj(
        "proposedMoveUrl" -> config.proposedMoveUrl,
        "requestId" -> request.requestId
      ),
      "browserDirectCallBlocked" -> !EngineGateway.EceEndpointPolicy.browserMayCallEceDirectly,
      "nonFatalToGameLifecycle" -> true
    )

  private def eceBridgeErrorJson(
      code: String,
      message: String,
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EcePotentialEcsRequest
  ): JsObject =
    val safeMessage =
      Option(message).filter(_.nonEmpty).getOrElse("ECE potential request failed without a diagnostic message")
    Json.obj(
      "ok" -> false,
      "error" -> code,
      "message" -> safeMessage.take(240),
      "ece" -> Json.obj(
        "potentialUrl" -> config.potentialUrl,
        "requestId" -> request.requestId
      ),
      "browserDirectCallBlocked" -> !EngineGateway.EceEndpointPolicy.browserMayCallEceDirectly,
      "nonFatalToGameLifecycle" -> true
    )

  private def eceBridgeErrorJson(
      code: String,
      message: String,
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EcePositionEcsRequest
  ): JsObject =
    val safeMessage =
      Option(message).filter(_.nonEmpty).getOrElse("ECE position request failed without a diagnostic message")
    Json.obj(
      "ok" -> false,
      "error" -> code,
      "message" -> safeMessage.take(240),
      "ece" -> Json.obj(
        "positionUrl" -> config.positionUrl,
        "requestId" -> request.requestId
      ),
      "browserDirectCallBlocked" -> !EngineGateway.EceEndpointPolicy.browserMayCallEceDirectly,
      "nonFatalToGameLifecycle" -> true
    )

  private def parseEcePositionEcsPayload(
      config: EngineGateway.EceServiceConfig,
      json: JsValue,
      request: EngineGateway.EcePositionEcsRequest,
      gameId: String,
      playerId: String,
      ply: Int,
      fen: String,
      requesterSide: Perspective,
      key: String
  ): Either[String, JsObject] =
    val echo = field(json, "request_echo").getOrElse(Json.obj())
    val position = objectField(json, "position_ecs").getOrElse(Json.obj())
    val diagnostics = parseDiagnostics(json)
    for
      requestId <- stringField(echo, "request_id").toRight("missing request_echo.request_id")
      inputFen <- stringField(echo, "input_fen").toRight("missing request_echo.input_fen")
      _ <- Either.cond(requestId == request.requestId, (), "position_ecs request_id does not match outstanding request")
      _ <- Either.cond(inputFen == fen && inputFen == request.inputFen, (), "position_ecs input_fen does not match current FEN")
      _ <- Either.cond(diagnostics.displayAllowed, (), "position_ecs diagnostics status is not displayable")
      _ <- Either.cond(field(json, "raw_provider_output").isEmpty, (), "position_ecs payload exposed raw provider output")
      _ <- Either.cond(!hasForbiddenEcePublicField(position), (), "position_ecs payload exposed forbidden field")
      _ <- Either.cond(booleanField(position, "ai_used").contains(true), (), "position_ecs did not use AI")
      summary <- safePayloadCoachTextField(position, "summary_text").toRight("position_ecs returned no summary text")
    yield
      val points =
        arrayField(position, "key_points")
          .flatMap(value => stringValue(value).flatMap(safeDisplayText))
          .take(3)
      val focus = safePayloadCoachTextField(position, "suggested_focus")
      val caution = safePayloadCoachTextField(position, "caution")
      val pointText =
        Option.when(points.nonEmpty)(points.zipWithIndex.map { case (point, index) => s"${index + 1}. $point" }.mkString(" "))
      val body =
        List(
          Some(summary),
          pointText.map(points => s"Key points: $points"),
          focus.map(text => s"Focus: $text"),
          caution.map(text => s"Caution: $text")
        ).flatten.mkString(" ")
      val deliveredLevel =
        levelField(echo, if requesterSide == Perspective.White then "white_level" else "black_level")
          .getOrElse(if requesterSide == Perspective.White then request.whiteLevel else request.blackLevel)
      val auditId = s"${request.requestId}-${diagnostics.engineVersion}".take(120)
      val evalVisuals =
        field(position, "evaluation")
          .flatMap(evalVisualFrom(_, "ece.eval.position"))
          .toList
      val positionJson = Json.obj(
        "key" -> key,
        "gameId" -> gameId,
        "playerId" -> playerId,
        "ply" -> ply,
        "boardStateKey" -> fen,
        "perspective" -> perspectiveKey(requesterSide),
        "level" -> deliveredLevel.value,
        "title" -> "Ask AI",
        "body" -> body,
        "source" -> safeTextField(position, "source", 48).getOrElse("ai_position_ecs"),
        "auditId" -> auditId,
        "serverAuthorized" -> true,
        "approvedDisplayPayload" -> true,
        "visuals" -> evalVisuals.zipWithIndex.map { case (visual, index) =>
          positionEcsVisualJson(visual, gameId, ply, fen, auditId, index)
        }
      )
      Json.obj(
        "ok" -> true,
        "position" -> positionJson,
          "ece" -> Json.obj(
            "positionUrl" -> config.positionUrl,
            "requestId" -> request.requestId,
            "positionEcsId" -> request.positionEcsId,
            "diagnostics" -> Json.obj(
            "status" -> diagnostics.status.toString,
            "label" -> diagnostics.status.sanitizedUserLabel,
            "engineVersion" -> diagnostics.engineVersion
          )
        ),
        "browserDirectCallBlocked" -> !EngineGateway.EceEndpointPolicy.browserMayCallEceDirectly,
        "nonFatalToGameLifecycle" -> true
      )

  private def parseEcePotentialEcsPayload(
      config: EngineGateway.EceServiceConfig,
      json: JsValue,
      request: EngineGateway.EcePotentialEcsRequest,
      gameId: String,
      playerId: String,
      ply: Int,
      fen: String,
      kind: String,
      revealSide: Perspective,
      quota: Int,
      key: String
  ): Either[String, JsObject] =
    val echo = field(json, "request_echo").getOrElse(Json.obj())
    val potential = objectField(json, "potential_ecs").getOrElse(Json.obj())
    val diagnostics = parseDiagnostics(json)
    val moves = arrayField(potential, "moves").take(3)
    val phase = stringField(field(json, "schema").getOrElse(Json.obj()), "phase")
    val sideFromFen = sideToMoveFromFen(fen)
    for
      requestId <- stringField(echo, "request_id").toRight("missing request_echo.request_id")
      inputFen <- stringField(echo, "input_fen").toRight("missing request_echo.input_fen")
      echoSide <- stringField(echo, "side_to_move")
        .orElse(stringField(potential, "side_to_move"))
        .flatMap(parsePerspective)
        .toRight("missing or invalid potential_ecs side_to_move")
      _ <- Either.cond(phase.forall(_ == "potential_ecs"), (), "potential_ecs schema phase is not potential_ecs")
      _ <- Either.cond(requestId == request.requestId, (), "potential_ecs request_id does not match outstanding request")
      _ <- Either.cond(inputFen == fen && inputFen == request.inputFen, (), "potential_ecs input_fen does not match current FEN")
      _ <- Either.cond(sideFromFen.contains(echoSide), (), "potential_ecs side_to_move does not match FEN")
      _ <- Either.cond(echoSide == revealSide, (), "potential_ecs side_to_move does not match requested reveal side")
      _ <- Either.cond(diagnostics.displayAllowed, (), "potential_ecs diagnostics status is not displayable")
      _ <- Either.cond(field(json, "raw_provider_output").isEmpty, (), "potential_ecs payload exposed raw provider output")
      _ <- Either.cond(!hasForbiddenEcePublicField(potential), (), "potential_ecs payload exposed forbidden field")
      _ <- Either.cond(moves.nonEmpty, (), safeTextField(potential, "unavailable", 80).getOrElse("potential_ecs returned no moves"))
    yield
      val deliveredLevel =
        levelField(echo, "resolved_level")
          .orElse(levelField(potential, "level"))
          .getOrElse(request.sideToMoveLevel(revealSide))
      val auditId = s"${request.requestId}-${diagnostics.engineVersion}".take(120)
      val cards = moves.zipWithIndex.flatMap { case (move, index) =>
        potentialMoveCardJson(move, index, gameId, ply, fen, deliveredLevel, auditId)
      }
      val moveVisuals = moves.zipWithIndex.flatMap { case (move, index) =>
        potentialMoveVisualJson(move, index, gameId, ply, fen, auditId)
      }
      val evalVisuals =
        field(potential, "evaluation").toList
          .flatMap(evalVisualFrom(_, "ece.eval.potential"))
          .zipWithIndex
          .map { case (visual, index) =>
            eceEvalVisualJson(visual, gameId, ply, fen, auditId, index, "ece-potential-eval")
          }
      val visuals = moveVisuals ++ evalVisuals
      if cards.isEmpty && moveVisuals.isEmpty then
        Json.obj("ok" -> false, "error" -> "potential_moves_unavailable", "message" -> "Potential ECS returned moves without displayable SAN or UCI")
      else
        Json.obj(
          "ok" -> true,
          "potential" -> Json.obj(
            "key" -> key,
            "gameId" -> gameId,
            "playerId" -> playerId,
            "ply" -> ply,
            "boardStateKey" -> fen,
            "perspective" -> perspectiveKey(revealSide),
            "kind" -> kind,
            "level" -> deliveredLevel.value,
            "quota" -> quota,
            "consumed" -> 0,
            "cards" -> cards,
            "visuals" -> visuals,
            "auditId" -> auditId,
            "serverAuthorized" -> true,
            "approvedDisplayPayload" -> true
          ),
          "ece" -> Json.obj(
            "potentialUrl" -> config.potentialUrl,
            "requestId" -> request.requestId,
            "diagnostics" -> Json.obj(
              "status" -> diagnostics.status.toString,
              "label" -> diagnostics.status.sanitizedUserLabel,
              "engineVersion" -> diagnostics.engineVersion
            )
          ),
          "browserDirectCallBlocked" -> !EngineGateway.EceEndpointPolicy.browserMayCallEceDirectly,
          "nonFatalToGameLifecycle" -> true
        )

  private def parseEceProposedMovePayload(
      config: EngineGateway.EceServiceConfig,
      json: JsValue,
      request: EngineGateway.EceProposedMoveRequest,
      gameId: String,
      playerId: String,
      ply: Int,
      fen: String,
      requesterSide: Perspective
  ): Either[String, JsObject] =
    val echo = field(json, "request_echo").getOrElse(Json.obj())
    val evaluation = field(json, "proposed_move_evaluation").getOrElse(Json.obj())
    val moveJson = field(evaluation, "move").getOrElse(Json.obj())
    val diagnostics = parseDiagnostics(json)
    for
      requestId <- stringField(echo, "request_id").toRight("missing request_echo.request_id")
      inputFen <- stringField(echo, "input_fen").toRight("missing request_echo.input_fen")
      echoSan = stringField(echo, "proposed_move_san")
      echoUci =
        stringField(echo, "proposed_move_uci")
          .orElse(objectField(evaluation, "move").flatMap(move => safeTextField(move, "uci", 16)))
      _ <- Either.cond(echoSan.isDefined || echoUci.isDefined, (), "missing request_echo.proposed_move_san_or_uci")
      _ <- Either.cond(requestId == request.requestId, (), "proposed request_id does not match outstanding request")
      _ <- Either.cond(inputFen == fen && inputFen == request.inputFen, (), "proposed input_fen does not match current FEN")
      _ <- Either.cond(
        echoUci.contains(request.proposedMoveUci) ||
          echoSan.exists(san => request.proposedMoveSan.contains(san) || san == request.proposedMoveUci),
        (),
        "proposed move echo does not match current arrow"
      )
      _ <- Either.cond(diagnostics.displayAllowed, (), "proposed diagnostics status is not displayable")
      _ <- Either.cond(field(json, "raw_provider_output").isEmpty, (), "proposed payload exposed raw provider output")
    yield
      val levelJson = field(evaluation, "level").getOrElse(Json.obj())
      val deliveredLevel =
        levelField(levelJson, "delivered_level")
          .orElse(levelField(levelJson, "delivered"))
          .getOrElse(if requesterSide == Perspective.White then request.whiteLevel else request.blackLevel)
      val san =
        objectField(evaluation, "move")
          .flatMap(move => safeTextField(move, "san", 16))
          .orElse(safeTextField(evaluation, "san", 16))
          .filterNot(_ == "0")
      val moveUci =
        objectField(evaluation, "move")
          .flatMap(move => safeTextField(move, "uci", 16))
          .orElse(stringValue(moveJson).flatMap(value => safeDisplayText(value, 16)))
          .getOrElse(request.proposedMoveUci)
      val summaryText =
        safePayloadCoachTextField(evaluation, "summary")
          .orElse(safeCoachTextField(evaluation, "sentence"))
          .orElse(objectField(evaluation, "coaching").flatMap(coaching => safePayloadCoachTextField(coaching, "text")))
          .orElse(safePayloadCoachTextField(evaluation, "advice"))
      val planText =
        safePayloadCoachTextField(evaluation, "plan")
      val warningText =
        arrayField(evaluation, "warnings")
          .flatMap(value => stringValue(value).flatMap(safeDisplayText))
          .headOption
      val legal = booleanField(evaluation, "legal")
      val newFen = safePayloadTextField(evaluation, "new_fen", 180)
      val postMoveBoardStateKey = newFen.getOrElse(s"$fen ${request.proposedMoveUci}")
      val evalAfter = field(evaluation, "eval_after").filterNot(isZeroValue)
      val afterMoveSideOutput =
        if legal.contains(true) then
          proposedMoveAfterMoveStandardOverlay(
            evaluation = evaluation,
            gameId = gameId,
            playerId = playerId,
            ply = ply,
            newFen = postMoveBoardStateKey,
            requesterSide = requesterSide,
            request = request,
            auditId = s"${request.requestId}-${diagnostics.engineVersion}-after-standard".take(120),
            evalAfter = evalAfter
          ).orElse:
            field(evaluation, "after_move_side_output").flatMap(afterMove =>
              val sideJson = withEvaluationFallback(afterMove, evalAfter)
              proposedMoveAfterMoveOverlay(
                gameId = gameId,
                playerId = playerId,
                ply = ply,
                newFen = postMoveBoardStateKey,
                requesterSide = requesterSide,
                request = request,
                auditId = s"${request.requestId}-${diagnostics.engineVersion}-after".take(120),
                sideJson = sideJson
              )
            )
        else None
      val body =
        if legal.contains(false) then warningText.orElse(summaryText).getOrElse("That move is not available from this position.")
        else summaryText.orElse(planText).orElse(warningText).getOrElse("Review what this move changes before committing.")
      val evalDelta =
        intField(evaluation, "eval_delta").map(delta => s" Eval delta ${if delta > 0 then "+" else ""}$delta cp.").getOrElse("")
      val source =
        objectField(evaluation, "summary")
          .flatMap(summary => safeTextField(summary, "source", 48))
          .orElse(safeTextField(evaluation, "source", 48))
          .getOrElse("ece")
      val auditId = s"${request.requestId}-${diagnostics.engineVersion}".take(120)
      val proposedJson = Json.obj(
        "key" -> request.cacheKey(diagnostics.engineVersion),
        "gameId" -> gameId,
        "playerId" -> playerId,
        "ply" -> ply,
        "boardStateKey" -> fen,
        "perspective" -> perspectiveKey(requesterSide),
        "moveUci" -> moveUci,
        "san" -> san,
        "level" -> deliveredLevel.value,
        "title" -> s"Proposed Move ${san.getOrElse(moveUci)}",
        "body" -> s"$body$evalDelta",
        "source" -> source,
        "auditId" -> auditId,
        "serverAuthorized" -> true,
        "approvedDisplayPayload" -> true
      ) ++ legal.fold(Json.obj())(value => Json.obj("legal" -> value)) ++
        afterMoveSideOutput
          .map(payload => Json.obj("postMoveBoardStateKey" -> payload.identity.boardStateKey))
          .getOrElse(newFen.fold(Json.obj())(value => Json.obj("postMoveBoardStateKey" -> value))) ++
        afterMoveSideOutput.fold(Json.obj())(payload =>
          Json.obj(
            "cards" -> payload.cards.map(roundCardJson),
            "visuals" -> payload.visuals.map(roundVisualJson)
          )
        )
      Json.obj(
        "ok" -> true,
        "proposed" -> proposedJson,
        "ece" -> Json.obj(
          "proposedMoveUrl" -> config.proposedMoveUrl,
          "requestId" -> request.requestId,
          "diagnostics" -> Json.obj(
            "status" -> diagnostics.status.toString,
            "label" -> diagnostics.status.sanitizedUserLabel,
            "engineVersion" -> diagnostics.engineVersion
          )
        ),
        "browserDirectCallBlocked" -> !EngineGateway.EceEndpointPolicy.browserMayCallEceDirectly,
        "nonFatalToGameLifecycle" -> true
      )

  private def proposedMoveAfterMoveStandardOverlay(
      evaluation: JsValue,
      gameId: String,
      playerId: String,
      ply: Int,
      newFen: String,
      requesterSide: Perspective,
      request: EngineGateway.EceProposedMoveRequest,
      auditId: String,
      evalAfter: Option[JsValue]
  ): Option[LiveBoardIntegration.RoundLivePayload] =
    val key = perspectiveKey(requesterSide)
    val overlays =
      proposedMoveAfterMoveStandardPayloads(evaluation).flatMap: standardPayload =>
        proposedMoveSideOutputFromAfterMovePayload(standardPayload, key)
          .flatMap: sideJson =>
            proposedMoveAfterMoveOverlay(
              gameId = gameId,
              playerId = playerId,
              ply = ply,
              newFen = newFen,
              requesterSide = requesterSide,
              request = request,
              auditId = auditId,
              sideJson = withEvaluationFallback(sideJson, evalAfter)
            )
    overlays.headOption

  private def proposedMoveAfterMoveStandardPayloads(evaluation: JsValue): List[JsValue] =
    val afterMoveEcs = objectField(evaluation, "after_move_ecs")
    List(
      field(evaluation, "after_move_standard_ecs"),
      field(evaluation, "after_move_standard_payload"),
      field(evaluation, "after_move_standard"),
      field(evaluation, "after_move_initial_ecs"),
      field(evaluation, "after_move_initial_payload"),
      field(evaluation, "after_move_initial"),
      afterMoveEcs.flatMap(payload => field(payload, "standard_ecs")),
      afterMoveEcs.flatMap(payload => field(payload, "standard")),
      afterMoveEcs.flatMap(payload => field(payload, "initial_ecs")),
      afterMoveEcs.flatMap(payload => field(payload, "initial")),
      field(evaluation, "after_move_advanced_ecs"),
      field(evaluation, "after_move_advanced_payload"),
      field(evaluation, "after_move_advanced"),
      afterMoveEcs.flatMap(payload => field(payload, "advanced_ecs")),
      afterMoveEcs.flatMap(payload => field(payload, "advanced"))
    ).flatten.filterNot(isZeroValue)

  private def proposedMoveSideOutputFromAfterMovePayload(payload: JsValue, key: String): Option[JsValue] =
    field(payload, "side_outputs")
      .flatMap(sideOutputs => field(sideOutputs, key))
      .orElse(field(payload, "side_output_addenda").flatMap(addenda => field(addenda, key)))
      .orElse(field(payload, key))
      .orElse(field(payload, "side_output").filterNot(isZeroValue))

  private def proposedMoveAfterMoveOverlay(
      gameId: String,
      playerId: String,
      ply: Int,
      newFen: String,
      requesterSide: Perspective,
      request: EngineGateway.EceProposedMoveRequest,
      auditId: String,
      sideJson: JsValue
  ): Option[LiveBoardIntegration.RoundLivePayload] =
    if sideJson.asOpt[Int].contains(0) then None
    else
      val key = perspectiveKey(requesterSide)
      parseSidePayload(Json.obj(key -> sideJson), key, requesterSide, includeEval = true).toOption.map: parsed =>
        val authorizedLevel = if requesterSide == Perspective.White then request.whiteLevel else request.blackLevel
        EceLiveBridge.compileProposedMovePreview(
          gameId = gameId,
          playerId = playerId,
          ply = ply,
          boardStateKey = newFen,
          requesterSide = requesterSide,
          authorizedLevel = authorizedLevel,
          sideOutput = parsed.output,
          atoms = parsed.atoms,
          auditId = auditId,
          ttlMillis = 60_000,
          extraCards = parsed.extras.cards,
          extraVisuals = parsed.extras.visuals
        )

  private def roundOverlayJson(payload: LiveBoardIntegration.RoundLivePayload, ttlMillis: Int, now: Long): JsObject =
    val clear = payload.clearReason.toList.map: reason =>
      Json.obj(
        "gameId" -> payload.identity.gameId,
        "ply" -> payload.identity.ply,
        "boardStateKey" -> payload.identity.boardStateKey,
        "reason" -> reason,
        "auditId" -> payload.identity.auditId
      )
    Json.obj(
      "enabled" -> payload.enabled,
      "gameId" -> payload.identity.gameId,
      "ply" -> payload.identity.ply,
      "boardStateKey" -> payload.identity.boardStateKey,
      "perspective" -> payload.identity.perspective,
      "auditId" -> payload.identity.auditId,
      "serverAuthorized" -> payload.identity.serverAuthorized,
      "ttlMillis" -> ttlMillis,
      "stale" -> false,
      "createdAt" -> now,
      "expiresAt" -> (now + ttlMillis),
      "cards" -> payload.cards.map(roundCardJson),
      "visuals" -> payload.visuals.map(roundVisualJson),
      "clear" -> clear
    )

  private def roundCardJson(card: LiveBoardIntegration.RoundCard): JsObject =
    Json.obj(
      "id" -> card.id,
      "gameId" -> card.gameId,
      "ply" -> card.ply,
      "boardStateKey" -> card.boardStateKey,
      "featureKey" -> card.featureKey,
      "title" -> card.title,
      "body" -> card.body,
      "level" -> card.level,
      "auditId" -> card.auditId,
      "defaultActive" -> card.defaultActive,
      "visibility" -> "visible",
      "serverAuthorized" -> card.serverAuthorized,
      "approvedDisplayPayload" -> card.approvedDisplayPayload,
      "stale" -> card.stale,
      "ttlMillis" -> card.ttlMillis
    )

  private def roundVisualJson(visual: LiveBoardIntegration.RoundVisual): JsObject =
    val base = Json.obj(
      "id" -> visual.id,
      "gameId" -> visual.gameId,
      "ply" -> visual.ply,
      "boardStateKey" -> visual.boardStateKey,
      "featureKey" -> visual.featureKey,
      "label" -> visual.label,
      "auditId" -> visual.auditId,
      "primary" -> visual.primary,
      "serverAuthorized" -> visual.serverAuthorized,
      "approvedDisplayPayload" -> visual.approvedDisplayPayload,
      "stale" -> visual.stale
    )
    base ++
      visual.evalCpWhite.fold(Json.obj())(value => Json.obj("evalCpWhite" -> value)) ++
      visual.evalMateWhite.fold(Json.obj())(value => Json.obj("evalMateWhite" -> value)) ++
      visual.evalWinWhite.fold(Json.obj())(value => Json.obj("evalWinWhite" -> value)) ++
      visual.evalDrawWhite.fold(Json.obj())(value => Json.obj("evalDrawWhite" -> value)) ++
      visual.evalLossWhite.fold(Json.obj())(value => Json.obj("evalLossWhite" -> value)) ++
      visual.evalSource.fold(Json.obj())(value => Json.obj("evalSource" -> value))

  private def positionEcsVisualJson(
      visual: EceLiveBridge.ExtraVisual,
      gameId: String,
      ply: Int,
      boardStateKey: String,
      auditId: String,
      index: Int
  ): JsObject =
    eceEvalVisualJson(visual, gameId, ply, boardStateKey, auditId, index, "ece-position-visual")

  private def eceEvalVisualJson(
      visual: EceLiveBridge.ExtraVisual,
      gameId: String,
      ply: Int,
      boardStateKey: String,
      auditId: String,
      index: Int,
      idPrefix: String
  ): JsObject =
    Json.obj(
      "id" -> s"$idPrefix-$index-${math.abs(auditId.hashCode)}",
      "gameId" -> gameId,
      "ply" -> ply,
      "boardStateKey" -> boardStateKey,
      "featureKey" -> visual.featureKey,
      "label" -> visual.label,
      "auditId" -> auditId,
      "primary" -> visual.primary,
      "serverAuthorized" -> true,
      "approvedDisplayPayload" -> true,
      "stale" -> false
    ) ++
      visual.evalCpWhite.fold(Json.obj())(value => Json.obj("evalCpWhite" -> value)) ++
      visual.evalMateWhite.fold(Json.obj())(value => Json.obj("evalMateWhite" -> value)) ++
      visual.evalWinWhite.fold(Json.obj())(value => Json.obj("evalWinWhite" -> value)) ++
      visual.evalDrawWhite.fold(Json.obj())(value => Json.obj("evalDrawWhite" -> value)) ++
      visual.evalLossWhite.fold(Json.obj())(value => Json.obj("evalLossWhite" -> value)) ++
      visual.evalSource.fold(Json.obj())(value => Json.obj("evalSource" -> value))

  private def sideFieldsDetected(sideJson: JsValue): JsObject =
    val overlays = field(sideJson, "overlays").getOrElse(Json.obj())
    val tradeStatus = field(overlays, "trade_status").getOrElse(Json.obj())
    val threatGroups = field(overlays, "threats").getOrElse(Json.obj())
    val pinGroups = field(overlays, "pinned_pieces").getOrElse(Json.obj())
    Json.obj(
      "summary" -> safePayloadCoachTextField(sideJson, "summary").isDefined,
      "immediateWarning" -> safePayloadCoachTextField(sideJson, "immediate_warning").isDefined,
      "plan" -> safePayloadCoachTextField(sideJson, "plan").isDefined,
      "cards" -> arrayField(sideJson, "cards").size,
      "offsetCount" -> (arrayField(overlays, "offset_count").size + arrayField(tradeStatus, "offset_count").size),
      "hangingPieces" -> (
        arrayField(overlays, "hanging_pieces").size +
          arrayField(tradeStatus, "hanging_pieces").size +
          arrayField(tradeStatus, "hanging_attackable").size +
          arrayField(tradeStatus, "hanging_not_attackable").size
      ),
      "threats" -> (
        threatArrayField(overlays, "threats").size +
          threatArrayField(sideJson, "threats").size +
          threatArrayField(overlays, "student_threats").size +
          threatArrayField(overlays, "student").size +
          threatArrayField(overlays, "opponent_threats").size +
          threatArrayField(overlays, "opponent").size +
          threatArrayField(overlays, "player_threats").size +
          threatArrayField(overlays, "player").size +
          threatArrayField(threatGroups, "student_threats").size +
          threatArrayField(threatGroups, "student").size +
          threatArrayField(threatGroups, "opponent_threats").size +
          threatArrayField(threatGroups, "opponent").size +
          threatArrayField(threatGroups, "player_threats").size +
          threatArrayField(threatGroups, "player").size
      ),
      "pins" -> (
        arrayField(overlays, "pins").size +
          arrayField(pinGroups, "student_pinned").size +
          arrayField(pinGroups, "opponent_pinned").size
      ),
      "opening" -> (arrayField(overlays, "opening").size + objectField(sideJson, "opening").fold(0)(_ => 1)),
      "eval" -> (field(overlays, "eval").isDefined || field(sideJson, "evaluation").isDefined),
      "potentialMoves" -> 0,
      "humanRisk" -> objectField(sideJson, "human_risk").isDefined,
      "proposedMovePreview" -> field(sideJson, "proposed_move_preview").isDefined,
      "reviewModes" -> field(sideJson, "review_modes").isDefined
    )

  private def field(json: JsValue, key: String): Option[JsValue] =
    (json \ key).toOption

  private val forbiddenEcePublicFields = Set(
    "position",
    "shared_calculations",
    "raw_provider_output",
    "raw_prompt",
    "raw_unrestricted_engine_output",
    "provider_path",
    "api_key",
    "secret",
    "token",
    "password"
  )

  private def hasForbiddenEcePublicField(json: JsValue): Boolean =
    json match
      case obj: JsObject =>
        obj.fields.exists { case (key, value) =>
          forbiddenEcePublicFields.contains(key) || hasForbiddenEcePublicField(value)
        }
      case JsArray(values) => values.exists(hasForbiddenEcePublicField)
      case _               => false

  private def arrayField(json: JsValue, key: String): List[JsValue] =
    field(json, key).collect { case JsArray(values) => values.toList }.getOrElse(Nil)

  private def threatArrayField(json: JsValue, key: String): List[JsValue] =
    List(
      key,
      s"${key}_arrows",
      s"${key}_lines",
      s"${key}_moves",
      s"${key}_captures",
      s"${key}_attacks"
    ).flatMap(arrayField(json, _))

  private def objectField(json: JsValue, key: String): Option[JsObject] =
    field(json, key).collect { case obj: JsObject => obj }

  private def stringField(json: JsValue, key: String): Option[String] =
    field(json, key).flatMap(_.asOpt[String]).filter(_.nonEmpty)

  private def safeEceErrorMessage(error: String, fallback: String): String =
    Option(error).map(_.trim).filter(_.nonEmpty).getOrElse(fallback).take(240)

  private def stringValue(json: JsValue): Option[String] =
    json.asOpt[String].filter(_.nonEmpty)

  private def intField(json: JsValue, key: String): Option[Int] =
    field(json, key).flatMap(value => value.asOpt[Int].orElse(value.asOpt[String].flatMap(_.toIntOption)))

  private def longField(json: JsValue, key: String): Option[Long] =
    field(json, key).flatMap(value => value.asOpt[Long].orElse(value.asOpt[Int].map(_.toLong)).orElse(value.asOpt[String].flatMap(_.toLongOption)))

  private def doubleValue(json: JsValue): Option[Double] =
    json.asOpt[Double]
      .orElse(json.asOpt[Int].map(_.toDouble))
      .orElse(json.asOpt[String].flatMap(_.trim.toDoubleOption))

  private def doubleField(json: JsValue, key: String): Option[Double] =
    field(json, key).flatMap(doubleValue)

  private def booleanField(json: JsValue, key: String): Option[Boolean] =
    field(json, key).flatMap(value => value.asOpt[Boolean].orElse(value.asOpt[String].map(_.toBooleanOption).flatten))

  private def levelField(json: JsValue, key: String): Option[Level] =
    intField(json, key).filter(Level.isValid).map(Level.apply)

  private def safeTextField(json: JsValue, key: String, maxLength: Int): Option[String] =
    stringField(json, key).flatMap(safeDisplayText(_, maxLength))

  private def safeCoachTextField(json: JsValue, key: String): Option[String] =
    stringField(json, key).flatMap(safeDisplayText)

  private def safePayloadTextField(json: JsValue, key: String, maxLength: Int): Option[String] =
    safeTextField(json, key, maxLength).orElse(field(json, key).flatMap(value => safeTextField(value, "text", maxLength)))

  private def safePayloadCoachTextField(json: JsValue, key: String): Option[String] =
    safeCoachTextField(json, key).orElse(field(json, key).flatMap(value => safeCoachTextField(value, "text")))

  private def firstSafeText(json: JsValue, maxLength: Int, keys: String*): Option[String] =
    keys.toList.view.flatMap(key => safeTextField(json, key, maxLength)).headOption

  private def safeDisplayText(text: String, maxLength: Int): Option[String] =
    safeDisplayText(text).map(_.take(maxLength))

  private def safeDisplayText(text: String): Option[String] =
    val normalized = text.replaceAll("[\\r\\n\\t]+", " ").trim
    Option.when(normalized.nonEmpty && !unsafeDisplayText(normalized))(normalized)

  private def evalVisualFrom(json: JsValue, featureKey: String): Option[EceLiveBridge.ExtraVisual] =
    evalDisplayData(json).map: data =>
      EceLiveBridge.ExtraVisual(
        featureKey = featureKey,
        label = data.label,
        primary = false,
        evalCpWhite = data.cpWhite,
        evalMateWhite = data.mateWhite,
        evalWinWhite = data.winWhite,
        evalDrawWhite = data.drawWhite,
        evalLossWhite = data.lossWhite,
        evalSource = data.source
      )

  private final case class EvalDisplayData(
      label: String,
      cpWhite: Option[Int],
      mateWhite: Option[Int],
      winWhite: Option[Int],
      drawWhite: Option[Int],
      lossWhite: Option[Int],
      source: Option[String]
  )

  private def evalDisplayData(json: JsValue): Option[EvalDisplayData] =
    if json.asOpt[Int].contains(0) || json.asOpt[Double].contains(0d) then None
    else
      val score = field(json, "score").getOrElse(Json.obj())
      val wdl = field(json, "wdl").getOrElse(Json.obj())
      val source =
        safeTextField(json, "source", 32)
          .orElse(safeTextField(score, "source", 32))
          .orElse(safeTextField(json, "label", 32))
      val labelPrefix = source.map(source => s"$source eval").getOrElse("Eval")
      val mateWhite =
        intField(json, "mate_white")
          .orElse(intField(score, "mate_white"))
          .filter(_ != 0)
      val cpWhite =
        intField(json, "cp_white")
          .orElse(intField(score, "cp_white"))
          .orElse(doubleField(json, "cp_white").map(_.round.toInt))
          .orElse(doubleField(score, "cp_white").map(_.round.toInt))
      val legacyCp =
        cpWhite.orElse(
          intField(json, "centipawns")
            .orElse(intField(json, "cp"))
            .orElse(doubleField(json, "centipawns").map(_.round.toInt))
            .orElse(doubleField(json, "cp").map(_.round.toInt))
            .orElse(intField(score, "centipawns"))
            .orElse(intField(score, "cp"))
            .orElse(doubleField(score, "centipawns").map(_.round.toInt))
            .orElse(doubleField(score, "cp").map(_.round.toInt))
            .orElse(doubleField(json, "evaluation").map(evalNumberToCentipawns))
            .orElse(doubleField(json, "value").map(evalNumberToCentipawns))
            .orElse(doubleField(json, "score").map(evalNumberToCentipawns))
            .orElse(doubleValue(json).map(evalNumberToCentipawns))
        )
      val winWhite = intField(wdl, "win_white").orElse(doubleField(wdl, "win_white").map(_.round.toInt))
      val drawWhite = intField(wdl, "draw_white").orElse(doubleField(wdl, "draw_white").map(_.round.toInt))
      val lossWhite = intField(wdl, "loss_white").orElse(doubleField(wdl, "loss_white").map(_.round.toInt))
      val numericText =
        mateWhite match
          case Some(value) => Some(s"$labelPrefix #${if value >= 0 then "+" else ""}$value")
          case None => legacyCp.map: value =>
            s"$labelPrefix ${if value >= 0 then "+" else ""}$value cp"
      numericText
        .orElse(stringValue(json).flatMap(safeDisplayText(_, 60)).filter(text => text.exists(_.isDigit) && text.toLowerCase.contains("eval")))
        .orElse:
          val wdlText = safeTextField(json, "wdl", 24)
          wdlText.map(value => s"${source.getOrElse("Eval")} WDL $value")
        .map: label =>
          EvalDisplayData(
            label = label,
            cpWhite = cpWhite.orElse(legacyCp),
            mateWhite = mateWhite,
            winWhite = winWhite,
            drawWhite = drawWhite,
            lossWhite = lossWhite,
            source = source
          )

  private def evalNumberToCentipawns(value: Double): Int =
    if math.abs(value) <= 20 then math.round(value * 100).toInt else math.round(value).toInt

  private def withEvaluationFallback(sideJson: JsValue, fallback: Option[JsValue]): JsValue =
    fallback match
      case Some(evaluation) if !sideJson.asOpt[Int].contains(0) && field(sideJson, "evaluation").forall(isZeroValue) =>
        sideJson.asOpt[JsObject].fold(sideJson)(side => side ++ Json.obj("evaluation" -> evaluation))
      case _ => sideJson

  private def isZeroValue(value: JsValue): Boolean =
    value.asOpt[Int].contains(0) || value.asOpt[Double].contains(0d)

  private def unsafeDisplayText(text: String): Boolean =
    val lower = text.toLowerCase
    text.matches(".*[A-Za-z]:\\\\.*") ||
      lower.contains("external_engines\\") ||
      List("api_key", "secret", "password", "provider_path", "raw_prompt", "raw_provider_output").exists(lower.contains)

  private def squareFrom(json: JsValue): Option[String] =
    squareFromKeys(json, "square", "piece_square", "pinned_square", "target_square")
      .orElse(stringValue(json).flatMap(squareFromText))

  private def offsetSquareFrom(json: JsValue): Option[String] =
    squareFromKeys(json, "target_square", "square", "piece_square", "pinned_square")
      .orElse(stringValue(json).flatMap(squareFromText))

  private def squareFromKeys(json: JsValue, keys: String*): Option[String] =
    keys.toList.view
      .flatMap: key =>
        stringField(json, key)
          .orElse(field(json, key).flatMap(squareFromNestedValue))
      .find(_.matches("[a-h][1-8]"))

  private def squareFromNestedValue(json: JsValue): Option[String] =
    stringValue(json).flatMap(squareFromText)
      .orElse(squareFromKeys(json, "square", "square_name", "from", "from_square", "to", "to_square", "target_square"))

  private def squareFromText(text: String): Option[String] =
    val squarePattern = "(?i)([a-h][1-8])".r
    squarePattern.findFirstMatchIn(text).map(_.group(1).toLowerCase)

  private def hangingItemIsAttackable(json: JsValue): Boolean =
    val text = lowerTextFields(json, "category", "status", "reason", "label", "type")
    !hasExplicitNonAttackableCue(text) &&
      (
        hasExplicitAttackableCue(text) ||
          booleanField(json, "attackable").contains(true) ||
          booleanField(json, "currently_attackable").contains(true) ||
          booleanField(json, "can_be_taken").contains(true) ||
          booleanField(json, "can_be_captured").contains(true)
      )

  private def hangingItemIsNotAttackable(json: JsValue): Boolean =
    val text = lowerTextFields(json, "category", "status", "reason", "label", "type")
    hasExplicitNonAttackableCue(text) ||
      (
        !hasExplicitAttackableCue(text) &&
          (
            text.contains("undefended") ||
              text.contains("unprotected") ||
              text.contains("loose") ||
              booleanField(json, "attackable").contains(false) ||
              booleanField(json, "currently_attackable").contains(false) ||
              booleanField(json, "can_be_taken").contains(false) ||
              booleanField(json, "can_be_captured").contains(false)
          )
      )

  private def lowerTextFields(json: JsValue, keys: String*): String =
    keys.toList.flatMap(key => stringField(json, key)).mkString(" ").toLowerCase

  private def hasExplicitAttackableCue(text: String): Boolean =
    text.contains("hanging_attackable") ||
      text.contains("currently_attackable") ||
      text.contains("currently attackable") ||
      text.contains("can_be_taken") ||
      text.contains("can be taken") ||
      text.contains("can_be_captured") ||
      text.contains("can be captured") ||
      text.contains("takeable") ||
      text.contains("capturable") ||
      text.contains("attackable")

  private def hasExplicitNonAttackableCue(text: String): Boolean =
    text.contains("hanging_not_attackable") ||
      text.contains("not_currently_attackable") ||
      text.contains("not currently attackable") ||
      text.contains("not attackable") ||
      text.contains("not_attackable") ||
      text.contains("not capturable") ||
      text.contains("not_capturable")

  private def offsetCountValue(json: JsValue): Option[Int] =
    val numeric =
      intField(json, "piece_count_delta")
        .orElse(intField(json, "material_delta_student"))
        .orElse(intField(json, "value"))
        .orElse(intField(json, "count"))
        .orElse(intField(json, "offset"))
        .orElse(intField(json, "offset_value"))
    val label = stringField(json, "label").flatMap(label => label.replace("+", "").toIntOption)
    numeric
      .orElse(label)
      .orElse:
        stringField(json, "result").flatMap:
          case "student_favorable"   => Some(1)
          case "student_unfavorable" => Some(-1)
          case "student_gain"        => Some(1)
          case "student_loss"        => Some(-1)
          case "student_win"         => Some(1)
          case "opponent_win"        => Some(-1)
          case "opponent_gain"       => Some(-1)
          case "equal"               => Some(0)
          case _                     => None

  private def fromTo(json: JsValue): Option[(String, String)] =
    stringValue(json).flatMap(fromToText)
      .orElse(firstMoveText(json).flatMap(fromToText))
      .orElse(nestedThreatMove(json))
      .orElse:
        for
          from <- squareFromKeys(
            json,
            "attacker",
            "attacker_origin",
            "attacker_from",
            "from",
            "from_square",
            "source",
            "source_square",
            "origin",
            "origin_square",
            "start_square",
            "attacker_square",
            "attacking_square",
            "piece",
            "piece_origin",
            "piece_square"
          )
          to <- squareFromKeys(
            json,
            "target",
            "to",
            "to_square",
            "destination",
            "dest",
            "end_square",
            "landing_square",
            "target_square",
            "target_piece_square",
            "attacked_piece_square",
            "defender_square",
            "victim",
            "victim_square",
            "attacked_square",
            "capture_square"
          )
        yield from -> to

  private def nestedThreatMove(json: JsValue): Option[(String, String)] =
    List(
      "move",
      "move_uci",
      "first_move",
      "first_move_uci",
      "first_capture_move",
      "capture",
      "capture_move",
      "attack",
      "attack_move",
      "threat",
      "threat_move",
      "line",
      "line_uci",
      "arrow",
      "arrow_uci",
      "principal_move"
    ).view
      .flatMap(key => field(json, key))
      .flatMap(fromTo)
      .headOption

  private def fromToText(text: String): Option[(String, String)] =
    val arrowPattern = "(?i)([a-h][1-8])\\s*(?:-|->|to|x)\\s*([a-h][1-8])".r
    val uciPattern = "(?i)\\b([a-h][1-8])([a-h][1-8])(?:[qrbn])?\\b".r
    arrowPattern
      .findFirstMatchIn(text)
      .orElse(uciPattern.findFirstMatchIn(text))
      .map(matchResult => matchResult.group(1).toLowerCase -> matchResult.group(2).toLowerCase)

  private def firstMoveText(json: JsValue): Option[String] =
    firstSafeText(
      json,
      24,
      "uci",
      "move",
      "move_uci",
      "first_move",
      "first_move_uci",
      "first_capture_move",
      "student_first_move",
      "capture_move",
      "attack_move",
      "threat_move",
      "line_uci",
      "arrow_uci"
    )

  private def potentialMoveCardJson(
      move: JsValue,
      index: Int,
      gameId: String,
      ply: Int,
      fen: String,
      level: Level,
      auditId: String
  ): Option[JsObject] =
    val moveText =
      firstSafeText(move, 24, "san", "uci", "move", "label")
        .orElse(intField(move, "rank").map(rank => s"Potential $rank"))
    val reason =
      candidateReasonText(move)
        .orElse(firstSafeText(move, 96, "exactness_class", "exactness", "source"))
        .getOrElse("engine candidate")
    moveText.map: text =>
      Json.obj(
        "id" -> s"$gameId-ply-$ply-potential-card-${index + 1}",
        "gameId" -> gameId,
        "ply" -> ply,
        "boardStateKey" -> fen,
        "featureKey" -> s"ece.candidate.${index + 1}",
        "title" -> s"Potential ${candidateLetter(index)}",
        "body" -> s"$text: $reason",
        "level" -> level.value,
        "auditId" -> auditId,
        "defaultActive" -> false,
        "visibility" -> "visible",
        "serverAuthorized" -> true,
        "approvedDisplayPayload" -> true,
        "stale" -> false,
        "ttlMillis" -> 60000
      )

  private def potentialMoveVisualJson(
      move: JsValue,
      index: Int,
      gameId: String,
      ply: Int,
      fen: String,
      auditId: String
  ): Option[JsObject] =
    candidateUciSquares(move).map { case (from, to) =>
      val label = List(
        Some(s"Candidate ${candidateLetter(index)}"),
        firstSafeText(move, 24, "category", "label", "san")
      ).flatten.mkString(" ")
      Json.obj(
        "id" -> s"$gameId-ply-$ply-potential-visual-${index + 1}",
        "gameId" -> gameId,
        "ply" -> ply,
        "boardStateKey" -> fen,
        "featureKey" -> s"ece.candidate.${index + 1}",
        "label" -> s"$from-$to: ${safeDisplayText(label, 48).getOrElse(s"Candidate ${candidateLetter(index)}")}",
        "auditId" -> auditId,
        "primary" -> false,
        "serverAuthorized" -> true,
        "approvedDisplayPayload" -> true,
        "stale" -> false
      )
    }

  private def candidateUciSquares(json: JsValue): Option[(String, String)] =
    stringField(json, "uci")
      .filter(_.matches("[a-h][1-8][a-h][1-8][qrbn]?"))
      .map(uci => uci.take(2) -> uci.slice(2, 4))

  private def candidateLetter(index: Int): String =
    ('A' + index).toChar.toString

  private def candidateReasonText(json: JsValue): Option[String] =
    val reason =
      safeTextField(json, "reason", 64)
        .orElse(safeTextField(json, "category", 48).map(_.replace("_", " ")))
    val score = field(json, "score").flatMap(scoreText)
    val text = List(reason, score).flatten.distinct.mkString(", ")
    safeDisplayText(text, 96)

  private def scoreText(json: JsValue): Option[String] =
    val label = safeTextField(json, "label", 40)
    val score =
      intField(json, "centipawns")
        .orElse(intField(json, "cp"))
        .map(value => s"${if value >= 0 then "+" else ""}$value cp")
        .orElse(intField(json, "mate").filter(_ != 0).map(value => s"mate $value"))
    val text = List(label, score).flatten.mkString(" ")
    safeDisplayText(text, 80)

  private def threatSideField(json: JsValue): Option[Perspective] =
    List(
      "attacking_side",
      "attacker_side",
      "threat_side",
      "threatening_side",
      "source_side",
      "side",
      "student_side",
      "perspective_side",
      "color",
      "colour"
    )
      .view
      .flatMap(key => stringField(json, key).flatMap(parsePerspective))
      .headOption

  private def ownerField(json: JsValue, requesterSide: Perspective): Option[Perspective] =
    val direct =
      List(
        "owner",
        "owner_side",
        "owner_color",
        "owner_colour",
        "piece_owner",
        "piece_side",
        "piece_color",
        "piece_colour",
        "target_owner",
        "target_side",
        "target_color",
        "target_colour",
        "color",
        "colour",
        "side"
      )
        .view
        .flatMap(key => stringField(json, key))
        .flatMap(value => parseOwnerPerspective(value, requesterSide))
        .headOption
    direct.orElse:
      stringField(json, "piece").flatMap: piece =>
        val lower = piece.toLowerCase
        if lower.contains("white") then Some(Perspective.White)
        else if lower.contains("black") then Some(Perspective.Black)
        else if lower.contains("student") then Some(requesterSide)
        else if lower.contains("opponent") then Some(opponentOf(requesterSide))
        else None

  private def parseOwnerPerspective(value: String, requesterSide: Perspective): Option[Perspective] =
    value.toLowerCase match
      case "student" | "player" | "self" => Some(requesterSide)
      case "opponent"                    => Some(opponentOf(requesterSide))
      case other                         => parsePerspective(other)

  private def auditIdFrom(sideJson: JsValue): Option[String] =
    field(sideJson, "audit").flatMap(audit => safeTextField(audit, "audit_id", 96))

  private def diagnosticsStatus(status: String): EngineGateway.EceDiagnosticsStatus =
    status.toLowerCase match
      case "ok"                    => EngineGateway.EceDiagnosticsStatus.Ok
      case "partial"               => EngineGateway.EceDiagnosticsStatus.Partial
      case "invalid_request"       => EngineGateway.EceDiagnosticsStatus.InvalidRequest
      case "invalid_fen"           => EngineGateway.EceDiagnosticsStatus.InvalidFen
      case "invalid_game"          => EngineGateway.EceDiagnosticsStatus.InvalidGame
      case "stockfish_unavailable" => EngineGateway.EceDiagnosticsStatus.StockfishUnavailable
      case "ai_unavailable"        => EngineGateway.EceDiagnosticsStatus.AiUnavailable
      case _                       => EngineGateway.EceDiagnosticsStatus.InternalError

  private def parsePerspective(value: String): Option[Perspective] =
    value.toLowerCase match
      case "white" | "w" => Some(Perspective.White)
      case "black" | "b" => Some(Perspective.Black)
      case _             => None

  private def opponentOf(side: Perspective): Perspective =
    side match
      case Perspective.White => Perspective.Black
      case Perspective.Black => Perspective.White

  private def perspectiveFromColor(color: chess.Color): Perspective =
    if color.name == "white" then Perspective.White else Perspective.Black

  private def perspectiveKey(side: Perspective): String =
    side match
      case Perspective.White => "white"
      case Perspective.Black => "black"

  private def reviewPlanTierKey(adminUnlimited: Boolean)(using Context): String =
    val requested = get("tier").filter(Set("standard", "premium").contains)
    if adminUnlimited then requested.getOrElse("premium") else "standard"

  private def sha256Hex(value: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(value.getBytes(StandardCharsets.UTF_8))
      .map(byte => f"${byte & 0xff}%02x")
      .mkString

  private def sideToMoveFromFen(fen: String): Option[Perspective] =
    fen.trim.split("\\s+").lift(1).flatMap:
      case "w" => Some(Perspective.White)
      case "b" => Some(Perspective.Black)
      case _   => None

  private def sanForProposedMove(fen: String, moveUci: String): Option[String] =
    for
      uci <- Uci.Move(moveUci)
      parsed <- Fen.readWithMoveNumber(Fen.Full.clean(fen))
      move <- parsed.position.move(uci).toOption
    yield move.toSanStr.value

  private def levelParam(key: String, default: Int)(using Context): Level =
    Level(get(key).flatMap(_.toIntOption).filter(Level.isValid).getOrElse(default).max(Level.min).min(Level.max))

  private def booleanParam(key: String)(using Context): Boolean =
    get(key).exists(value => value == "1" || value == "true" || value == "on" || value == "yes")

  private def formFromQuery(requireAcknowledgement: Boolean)(using Context): Either[String, PlayForm] =
    PlayForm.fromValues(
      modeKey = get("mode").getOrElse(PlayForm.default.mode.key),
      timeControlKey = get("timeControl").getOrElse(PlayForm.default.timeControl.key),
      setLevelValue = get("setLevel").getOrElse(PlayForm.default.setLevel.value.toString),
      targetLevelValue = get("targetLevel"),
      confirmsOutsideHelpRule =
        !requireAcknowledgement || get("outsideHelp").exists(value => value == "acknowledged" || value == "true"),
      confirmsLevelContract = get("confirmLevelContract").exists(value => value == "true" || value == "on" || value == "1"),
      applyPreferences = get("preferredSetLevel").exists(value => value.nonEmpty && value != "any") ||
        get("playerTargetLevel").exists(value => value.nonEmpty && value != "any"),
      preferredSetLevelValue = get("preferredSetLevel"),
      playerTargetLevelValue = get("playerTargetLevel"),
      clockLimitSecondsValue = get("clockLimitSeconds"),
      clockIncrementSecondsValue = get("clockIncrementSeconds")
    )

  private def publicSearchKeyFromQuery(using Context): Option[String] =
    get("searchKey").filter(_.matches("[A-Za-z0-9_-]{16,80}"))

  private def ticketIdForPublicSearchKey(searchKey: String, playerId: String): Option[String] =
    searchRedirectLedger.ticketIdForPublicSearchKey(searchKey, playerId)

  private def publicSearchKeyFor(ticketId: String, playerId: String): String =
    searchRedirectLedger.publicSearchKeyFor(
      ticketId,
      playerId,
      () => s"ecs${java.util.UUID.randomUUID().toString.replace("-", "").take(30)}"
    )

  private def formFromSearchRecord(record: SearchIntentRecord): PlayForm =
    val timeControl =
      TimeControlOptions.all.find(_.bucket == record.ticket.poolKey.timeControl).getOrElse(TimeControlOptions.default)
    val applyPreferences = record.matchPreferences.preferredOwnSetLevel.isDefined
    PlayForm(
      mode = record.mode,
      timeControl = timeControl,
      setLevel = record.ticket.setLevel,
      targetLevel = record.ticket.targetLevel,
      applyPreferences = applyPreferences,
      preferredSetLevel = record.matchPreferences.preferredOwnSetLevel,
      requestedClock = record.ticket.requestedClock,
      confirmsOutsideHelpRule = true,
      confirmsLevelContract = record.queueState.requiresPairingConfirmation
    )

  private def activateEcorRuntimeFromSettings(): Unit =
    EvenChessRatingCalibration.EcorRuntime.activateFromText(
      env.web.settings.evenChessEcorPolicyVersion.get(),
      env.web.settings.evenChessEcorGapOffsets.get().value,
      env.web.settings.evenChessEcorRatingLevelBands.get().value
    )
    EvenChessRatingCalibration.StockfishAiRatingRuntime.activateFromText(
      env.web.settings.evenChessStockfishEquivalentRatingBands.get().value
    )

  private def freeMatchTokenWindowFromSettings =
    AdminBackendSettings.FreeMatchTokenWindow(
      enabled = env.web.settings.evenChessFreeMatchTokensEnabled.get(),
      startsAt = env.web.settings.evenChessFreeMatchTokensStartsAt.get(),
      endsAt = env.web.settings.evenChessFreeMatchTokensEndsAt.get()
    )

  private def botSimulationConfigFromSettings =
    BotOperations.BotSimulationConfig.fromSettings(
      AdminBackendSettings.BotSimulationControls(
        enabled = env.web.settings.evenChessBotSimulationEnabled.get(),
        scope = env.web.settings.evenChessBotSimulationScope.get(),
        botCount = env.web.settings.evenChessBotSimulationBotCount.get(),
        ratingMin = env.web.settings.evenChessBotSimulationRatingMin.get(),
        ratingMax = env.web.settings.evenChessBotSimulationRatingMax.get(),
        levelMin = env.web.settings.evenChessBotSimulationLevelMin.get(),
        levelMax = env.web.settings.evenChessBotSimulationLevelMax.get(),
        persona = env.web.settings.evenChessBotSimulationPersona.get(),
        timeControls = env.web.settings.evenChessBotSimulationTimeControls.get(),
        accountRoster = env.web.settings.evenChessBotSimulationAccountRoster.get()
      )
    )

  private def botModeConfigFromSettings =
    PlaySearchIntegration.BotModeConfig.fromSettings(
      enabled = env.web.settings.evenChessMatchmakingBotModeEnabled.get(),
      scope = env.web.settings.evenChessMatchmakingBotModeScope.get(),
      timeoutSeconds = env.web.settings.evenChessMatchmakingBotMatchTimeoutSeconds.get(),
      accountRoster = env.web.settings.evenChessMatchmakingBotAccountRoster.get()
    )

  private def searchModel(playerId: String)(using Context): PlayPageModel =
    activateEcorRuntimeFromSettings()
    val now = System.currentTimeMillis
    SearchStartService.pruneStaleHumanTickets(searchRepository, now)
    val freeMatchTokenWindow = freeMatchTokenWindowFromSettings
    val baseAccountDashboard = AccountMonetisationUi.AccountDashboard.forLichessUser(playerId, now)
    val accountDashboard =
      baseAccountDashboard.copy(
        tokenSnapshot = baseAccountDashboard.tokenSnapshot.withFreeMatchTokenWindow(freeMatchTokenWindow.activeAt(now))
      )
    val botMode = botModeConfigFromSettings
    val botSimulationConfig = botSimulationConfigFromSettings
    val botSimulationRuntime = BotOperations.BotSimulationRuntime.status
    if botSimulationRuntime.running && botSimulationConfig.enabled then
      val seed = BotOperations.seedSimulation(searchRepository, botSimulationConfig, botSimulationRuntime, now)
      BotOperations.BotSimulationRuntime.recordSeed(seed, "search-endpoint", now)
    val publicSearchKey = publicSearchKeyFromQuery
    val resumedTicketId = publicSearchKey.flatMap(ticketIdForPublicSearchKey(_, playerId))
    val preparedSearch =
      resumedTicketId match
        case Some(ticketId) =>
          SearchStartService.resume(ticketId, playerId, searchRepository, now)
        case None if publicSearchKey.isDefined =>
          Left("Search ticket not found.")
        case None =>
          formFromQuery(requireAcknowledgement = true).flatMap: form =>
            val request = SearchStartRequest(
              ticketId = s"ec-${java.util.UUID.randomUUID().toString.take(12)}",
              playerId = playerId,
              form = form,
              tokenSnapshot = accountDashboard.tokenSnapshot,
              expectedUsedOffset = ExpectedOffsetEstimate.forSetLevel(form.effectivePreferredSetLevel.getOrElse(form.setLevel)),
              latencyMillis = 40,
              createdAt = now
            )
            SearchStartService.prepare(request, searchRepository)

    preparedSearch match
      case Left(error) =>
        PlayPageModel(
          form = PlayForm.default,
          botMode = botMode,
          prepared = None,
          matchmaking = None,
          error = error.some,
          authenticated = true,
          accountDashboard = accountDashboard.some
        )
      case Right(prepared) =>
        val matchmaking =
          MatchmakingIntegrationService
            .evaluate(prepared.record.ticket.ticketId, searchRepository, now, botMode)
            .toOption
            .filter(_.valid)
        PlayPageModel(
          form = formFromSearchRecord(prepared.record),
          botMode = botMode,
          prepared = prepared.some,
          matchmaking = matchmaking,
          error = None,
          authenticated = true,
          accountDashboard = accountDashboard.some
        )

  private def searchJsonPayload(model: PlayPageModel, redirectUrl: Option[String]) =
    model.prepared match
      case Some(prepared) =>
        val ticket = prepared.record.ticket
        val searchKey = publicSearchKeyFor(ticket.ticketId, ticket.playerId)
        val botModeStatus =
          model.matchmaking.map(_.botMode).getOrElse(
            BotModeStatus(
              enabled = model.botMode.enabled,
              scope = model.botMode.scope,
              timeoutSeconds = model.botMode.timeoutSeconds,
              elapsedMillis = 0L,
              seedAttempted = false,
              botSeeded = false,
              botCandidatesVisible = false
            )
          )
        Json.obj(
          "ok" -> true,
          "searchKey" -> searchKey,
          "pollUrl" -> s"${PlaySearchIntegration.Routes.searchJson}?searchKey=$searchKey",
          "redirectUrl" -> redirectUrl,
          "queueLabel" -> prepared.record.queueState.label,
          "setLevel" -> ticket.setLevel.value,
          "targetLevel" -> ticket.targetLevel.map(_.value),
          "preferredSetLevel" -> model.form.effectivePreferredSetLevel.map(_.value),
          "searchScenario" -> model.form.searchScenarioLabel,
          "accessLabel" -> accessLabel(prepared.record.tokenSnapshot.accessReason(model.form.mode)),
          "tokenWaiverMessage" -> Option.when(prepared.record.tokenSnapshot.freeMatchTokenWindowActive)(AdminBackendSettings.FreeMatchTokenWindow.publicMessage),
          "waitingForPairing" -> prepared.record.queueState.waitingForPairing,
          "requiresPairingConfirmation" -> prepared.record.queueState.requiresPairingConfirmation,
          "matchmaking" -> Json.obj(
            "matched" -> model.matchmaking.exists(_.matched),
            "status" -> model.matchmaking.map(_.visibleStatus).getOrElse("Waiting for an EvenChess match."),
            "matchContract" -> model.matchmaking.flatMap(m =>
              m.contract.map(matchContractJson)
            ),
            "botMode" -> Json.obj(
              "enabled" -> botModeStatus.enabled,
              "disclosure" ->
                s"Bots may be implemented after long wait times while EvenChess's player pool is low. This will be removed as we grow. Bots are currently ${if botModeStatus.enabled then "On" else "Off"}.",
              "elapsedMillis" -> botModeStatus.elapsedMillis
            )
          )
        )
      case None =>
        Json.obj(
          "ok" -> false,
          "error" -> model.error.getOrElse("EvenChess search could not start."),
          "matchmaking" -> Json.obj(
            "botMode" -> Json.obj(
              "enabled" -> model.botMode.enabled,
              "disclosure" ->
                s"Bots may be implemented after long wait times while EvenChess's player pool is low. This will be removed as we grow. Bots are currently ${if model.botMode.enabled then "On" else "Off"}.",
              "elapsedMillis" -> 0L
            )
          )
        )

  private def matchedSearchRedirectPayload(searchKey: String, redirectUrl: String, botMode: BotModeConfig) =
    Json.obj(
      "ok" -> true,
      "searchKey" -> searchKey,
      "pollUrl" -> s"${PlaySearchIntegration.Routes.searchJson}?searchKey=$searchKey",
      "redirectUrl" -> redirectUrl,
      "waitingForPairing" -> false,
      "requiresPairingConfirmation" -> false,
      "matchmaking" -> Json.obj(
        "matched" -> true,
        "status" -> "EvenChess game ready.",
        "botMode" -> Json.obj(
          "enabled" -> botMode.enabled,
          "disclosure" ->
            s"Bots may be implemented after long wait times while EvenChess's player pool is low. This will be removed as we grow. Bots are currently ${if botMode.enabled then "On" else "Off"}.",
          "elapsedMillis" -> 0L
        )
      )
    )

  private def maybeCreateMatchedGameRedirect(model: PlayPageModel): Future[Option[String]] =
    val ticketId = model.prepared.map(_.record.ticket.ticketId)
    val existingRedirect = ticketId.flatMap(searchRedirectLedger.redirectForTicket)
    existingRedirect match
      case Some(url) => Future.successful(Some(url))
      case None =>
        val matchedGame =
          for
            prepared <- model.prepared
            matchmaking <- model.matchmaking
            candidate <- matchmaking.matchedCandidate
            contract <- matchmaking.contract
            contractSource <- matchmaking.contractSource
            if matchmaking.readyForLilaGameCreationAdapter
          yield (prepared.record, candidate, contract, contractSource)

        matchedGame match
          case None => Future.successful(None)
          case Some((requestRecord, candidate, contract, MatchContractSource.Bot)) if isRosterBackedBotRecord(candidate) =>
            createHumanMatchedGameRedirect(requestRecord, candidate, contract)
          case Some((_, candidate, _, MatchContractSource.Bot)) if candidate.ticket.isBotTicket =>
            Future.successful(None)
          case Some((requestRecord, candidate, contract, MatchContractSource.Human)) if !candidate.ticket.isBotTicket =>
            createHumanMatchedGameRedirect(requestRecord, candidate, contract)
          case _ => Future.successful(None)

  private def isRosterBackedBotRecord(record: SearchIntentRecord): Boolean =
    record.ticket.isBotTicket &&
      record.ticket.botProfile.flatMap(_.userRef).contains(record.ticket.playerId) &&
      !record.ticket.playerId.startsWith("ec-bot-") &&
      !record.ticket.playerId.startsWith(BotOperations.simulationTicketPrefix)

  private def ensureRosterBackedBotPerf(record: SearchIntentRecord, perfType: lila.rating.PerfType): Funit =
    if isRosterBackedBotRecord(record) then
      env.user.perfsRepo.setPerf(
        UserId(record.ticket.playerId),
        perfType.key,
        establishedRosterBotPerf(record.ticket.playerId, record.ticket.botProfile.map(_.targetEcr))
      )
    else funit

  private def createHumanMatchedGameRedirect(
      requestRecord: SearchIntentRecord,
      candidateRecord: SearchIntentRecord,
      contract: LevelBasedMatchmaking.MatchContract
  ): Future[Option[String]] =
    val requestIsWhite = contract.whitePlayerId == requestRecord.ticket.playerId
    val requestColor = if requestIsWhite then chess.Color.white else chess.Color.black
    val candidateColor = !requestColor
    val timeControl = challengeTimeControlFor(contract.timeControl, requestRecord.ticket.requestedClock.orElse(candidateRecord.ticket.requestedClock))
    val perfType = challengePerfTypeFor(timeControl)

    for
      _ <- ensureRosterBackedBotPerf(requestRecord, perfType)
      _ <- ensureRosterBackedBotPerf(candidateRecord, perfType)
      requestUser <- env.user.api.enabledWithPerf(UserStr(requestRecord.ticket.playerId), perfType)
      candidateUser <- env.user.api.enabledWithPerf(UserStr(candidateRecord.ticket.playerId), perfType)
      candidateMe <- env.user.api.me(UserId(candidateRecord.ticket.playerId))
      redirect <- (requestUser, candidateUser, candidateMe) match
        case (Some(challenger), Some(dest), Some(destMe)) =>
          val challenge = lila.challenge.Challenge.make(
            variant = chess.variant.Standard,
            initialFen = None,
            timeControl = timeControl,
            rated = chess.Rated(contract.rated),
            color = requestColor.name,
            challenger = lila.challenge.Challenge.toRegistered(challenger),
            destUser = Some(dest),
            rematchOf = None
          )
          env.challenge.api.create(challenge).flatMap:
            case false =>
              logMatchedGameRedirectFailure(requestRecord, candidateRecord, "challenge_create_false")
              Future.successful(None)
            case true =>
              allow:
                env.challenge.api
                  .accept(
                    challenge,
                    anonSecret = None,
                    requestedColor = Some(candidateColor),
                    gameSource = Some(lila.core.game.Source.Lobby)
                  )(using Some(destMe))
                  .map:
                    case Some(acceptedPov) =>
                      persistMatchedGamePolicy(challenge.gameId.value, requestRecord, candidateRecord, contract)
                      registerManagedBotsForGame(acceptedPov.game, requestRecord, candidateRecord)
                      val requestRedirect = routes.Round.player(acceptedPov.game.fullIdOf(requestColor)).url
                      val candidateRedirect = routes.Round.player(acceptedPov.game.fullIdOf(candidateColor)).url
                      searchRedirectLedger.putRedirects(
                        List(
                          requestRecord.ticket.ticketId -> requestRedirect,
                          candidateRecord.ticket.ticketId -> candidateRedirect
                        )
                      )
                      retireMatchedTickets(requestRecord, candidateRecord)
                      Some(requestRedirect)
                    case None =>
                      logMatchedGameRedirectFailure(requestRecord, candidateRecord, "challenge_accept_none")
                      None
              .rescue: _ =>
                logMatchedGameRedirectFailure(requestRecord, candidateRecord, "challenge_accept_exception")
                Future.successful(None)
        case _ =>
          val reason =
            List(
              Option.when(requestUser.isEmpty)("request_user_missing_or_disabled"),
              Option.when(candidateUser.isEmpty)("candidate_user_missing_or_disabled"),
              Option.when(candidateMe.isEmpty)("candidate_me_missing")
            ).flatten.mkString("+")
          logMatchedGameRedirectFailure(requestRecord, candidateRecord, reason)
          Future.successful(None)
    yield redirect

  private def logMatchedGameRedirectFailure(
      requestRecord: SearchIntentRecord,
      candidateRecord: SearchIntentRecord,
      reason: String
  ): Unit =
    val now = System.currentTimeMillis
    val key = List(requestRecord.ticket.ticketId, candidateRecord.ticket.ticketId).sorted.mkString("|")
    val shouldLog =
      searchStateLock.synchronized:
        matchedGameFailureLogAt.get(key).forall(previous => now - previous >= 10_000L) &&
          {
            matchedGameFailureLogAt.update(key, now)
            true
          }
    if shouldLog then
      lila
        .log("evenchess")
        .warn(
          s"Matched EvenChess search could not create game reason=$reason request=${requestRecord.ticket.ticketId}/${requestRecord.ticket.playerId} candidate=${candidateRecord.ticket.ticketId}/${candidateRecord.ticket.playerId} pool=${requestRecord.ticket.poolKey.key}"
        )

  private def persistMatchedGamePolicy(
      gameId: String,
      a: SearchIntentRecord,
      b: SearchIntentRecord,
      contract: LevelBasedMatchmaking.MatchContract
  ): Unit =
    val records = List(a, b)
    val white = records.find(_.ticket.playerId == contract.whitePlayerId)
    val black = records.find(_.ticket.playerId == contract.blackPlayerId)
    (white, black).tupled.foreach: (whiteRecord, blackRecord) =>
      GameStartService.persistBeforeCoaching(
        gameId = gameId,
        white = whiteRecord,
        black = blackRecord,
        stage = contract.stage,
        uiConfirmedLevelContract = true,
        policyRepository = gamePolicyRepository,
        now = System.currentTimeMillis,
        assignedContract = Some(contract)
      )

  private def registerManagedBotsForGame(
      game: lila.core.game.Game,
      a: SearchIntentRecord,
      b: SearchIntentRecord
  ): Unit =
    val botRecords = List(a, b).flatMap: record =>
      record.ticket.botProfile.map(profile => record.ticket.playerId -> profile)
    if botRecords.nonEmpty then
      lila
        .log("evenchess")
        .info(s"Attaching ${botRecords.size} managed bot runner(s) to game=${game.id}")
      botRecords.foreach: (botUserId, profile) =>
        EvenChessManagedBotRuntime.register(game, botUserId, profile)

  private def maybeCreateSimulationBotGamesFromSettings(): Unit =
    val config = botSimulationConfigFromSettings
    val runtime = BotOperations.BotSimulationRuntime.status
    maybeCreateSimulationBotGames(config, runtime, System.currentTimeMillis)

  private def maybeCreateSimulationBotGames(
      config: BotOperations.BotSimulationConfig,
      runtime: BotOperations.BotSimulationRuntimeState,
      now: Long
  ): Unit =
    if config.enabled && runtime.running then
      val shouldPump =
        searchStateLock.synchronized:
          if simulationBotGamePumpRevision != runtime.revision then
            simulationBotGameCreationInFlight.clear()
            simulationBotGameCreationCompleted.clear()
            simulationBotGamePumpRevision = runtime.revision
            lastSimulationBotGamePumpAt = 0L
          if now - lastSimulationBotGamePumpAt < simulationBotGamePumpMinIntervalMillis then false
          else
            lastSimulationBotGamePumpAt = now
            true
      if shouldPump then simulationBotMatchPairs(maxPairs = 3).foreach: (request, candidate, contract) =>
        val pairKey = List(request.ticket.ticketId, candidate.ticket.ticketId).sorted.mkString("|")
        val shouldCreate =
          searchStateLock.synchronized:
            if simulationBotGameCreationInFlight(pairKey) || simulationBotGameCreationCompleted(pairKey) then false
            else
              simulationBotGameCreationInFlight.add(pairKey)
              true
        if shouldCreate then
          val creation = createHumanMatchedGameRedirect(request, candidate, contract)
          creation.foreach: redirect =>
            searchStateLock.synchronized:
              simulationBotGameCreationInFlight.remove(pairKey)
              redirect.foreach(_ => simulationBotGameCreationCompleted.add(pairKey))
            lila
              .log("evenchess")
              .info(s"Simulation bot-vs-bot game creation pair=$pairKey result=${redirect.getOrElse("none")}")
          creation.failed.foreach: error =>
            searchStateLock.synchronized:
              simulationBotGameCreationInFlight.remove(pairKey)
            lila
              .log("evenchess")
              .warn(s"Simulation bot-vs-bot game creation failed pair=$pairKey error=${error.getMessage}")

  private def simulationBotMatchPairs(
      maxPairs: Int
  ): List[(SearchIntentRecord, SearchIntentRecord, LevelBasedMatchmaking.MatchContract)] =
    val active =
      BotOperations
        .activeSimulationTickets(searchRepository)
        .filter(BotOperations.isRosterBackedSimulationRecord)
        .sortBy(_.createdAt)
    val selected = mutable.ListBuffer.empty[(SearchIntentRecord, SearchIntentRecord, LevelBasedMatchmaking.MatchContract)]
    val usedTicketIds = mutable.Set.empty[String]
    active
      .groupBy(_.ticket.poolKey)
      .values
      .foreach: poolRecords =>
        poolRecords.foreach: request =>
          if selected.size < maxPairs && !usedTicketIds(request.ticket.ticketId) then
            val candidates =
              poolRecords.filter(candidate =>
                candidate.ticket.ticketId != request.ticket.ticketId &&
                  candidate.ticket.playerId != request.ticket.playerId &&
                  !usedTicketIds(candidate.ticket.ticketId)
              )
            val simulation =
              LevelBasedMatchmaking.MmrEngine.simulate(
                requestId = s"simulation-auto-${request.ticket.ticketId}",
                request = request.ticket,
                candidates = candidates.map(_.ticket),
                preferences = request.matchPreferences,
                tokenGateResult = request.tokenSnapshot.accessReason(request.mode),
                candidatePreferences = candidates.map(candidate => candidate.ticket.ticketId -> candidate.matchPreferences).toMap
              )
            for
              contract <- simulation.contract
              candidatePlayerId <-
                if contract.whitePlayerId == request.ticket.playerId then Some(contract.blackPlayerId)
                else if contract.blackPlayerId == request.ticket.playerId then Some(contract.whitePlayerId)
                else None
              candidate <- candidates.find(_.ticket.playerId == candidatePlayerId)
            yield
              usedTicketIds.add(request.ticket.ticketId)
              usedTicketIds.add(candidate.ticket.ticketId)
              selected.addOne((request, candidate, contract))
    selected.toList

  private def retireMatchedTickets(a: SearchIntentRecord, b: SearchIntentRecord): Unit =
    searchRepository.removeWhere(record =>
      record.ticket.ticketId == a.ticket.ticketId ||
        record.ticket.ticketId == b.ticket.ticketId
    )

  private def challengeTimeControlFor(
      bucket: EvenChessMode.TimeControlBucket,
      requestedClock: Option[LevelBasedMatchmaking.RequestedClock]
  ): lila.challenge.Challenge.TimeControl =
    val clockableBucket = bucket match
      case EvenChessMode.TimeControlBucket.Bullet | EvenChessMode.TimeControlBucket.Blitz | EvenChessMode.TimeControlBucket.Rapid |
          EvenChessMode.TimeControlBucket.Classical => true
      case _ => false
    requestedClock.filter(clock => clock.valid && clockableBucket) match
      case Some(clock) =>
        lila.challenge.Challenge.TimeControl.Clock(
          Clock.Config(Clock.LimitSeconds(clock.limitSeconds), Clock.IncrementSeconds(clock.incrementSeconds))
        )
      case None =>
        bucket match
          case EvenChessMode.TimeControlBucket.Bullet =>
            lila.challenge.Challenge.TimeControl.Clock(
              Clock.Config(Clock.LimitSeconds(60), Clock.IncrementSeconds(0))
            )
          case EvenChessMode.TimeControlBucket.Blitz =>
            lila.challenge.Challenge.TimeControl.Clock(
              Clock.Config(Clock.LimitSeconds(300), Clock.IncrementSeconds(0))
            )
          case EvenChessMode.TimeControlBucket.Rapid =>
            lila.challenge.Challenge.TimeControl.Clock(
              Clock.Config(Clock.LimitSeconds(600), Clock.IncrementSeconds(0))
            )
          case EvenChessMode.TimeControlBucket.Classical =>
            lila.challenge.Challenge.TimeControl.Clock(
              Clock.Config(Clock.LimitSeconds(1800), Clock.IncrementSeconds(0))
            )
          case EvenChessMode.TimeControlBucket.Correspondence =>
            lila.challenge.Challenge.TimeControl.Correspondence(Days(5))
          case EvenChessMode.TimeControlBucket.Casual =>
            lila.challenge.Challenge.TimeControl.Unlimited

  private def challengePerfTypeFor(timeControl: lila.challenge.Challenge.TimeControl): lila.rating.PerfType =
    val speed = timeControl match
      case lila.challenge.Challenge.TimeControl.Clock(config) => chess.Speed(config)
      case _                                                  => chess.Speed.Correspondence
    lila.rating.PerfType(chess.variant.Standard, speed)

  private def matchContractJson(contract: LevelBasedMatchmaking.MatchContract) =
    Json.obj(
      "timeControl" -> contract.timeControl.toString,
      "rated" -> contract.rated,
      "whiteSetLevel" -> contract.whiteSetLevel.value,
      "blackSetLevel" -> contract.blackSetLevel.value,
      "preferenceFlags" -> Json.obj(
        "scenario" -> contract.preferenceFlags.scenario.label,
        "requesterPreferredLevelMatched" -> contract.preferenceFlags.requesterPreferredLevelMatched,
        "candidatePreferredLevelMatched" -> contract.preferenceFlags.candidatePreferredLevelMatched,
        "widenedSearch" -> contract.preferenceFlags.widenedSearch,
        "unevenMatch" -> contract.preferenceFlags.unevenMatch,
        "unevenReason" -> contract.preferenceFlags.unevenReason
      )
    )

  private def accessLabel(reason: String) =
    reason match
      case "abuse_controls"                   => "Unavailable"
      case "mode_does_not_consume_game_token" => "Included"
      case "launch_free_token_window"         => "Temporarily free"
      case "subscription_access"              => "Plan access"
      case "game_token_available"             => "Token available"
      case "game_token_required"              => "Token required"
      case other                              => other.replace('_', ' ')

final case class PlayPageModel(
    form: PlaySearchIntegration.PlayForm,
    botMode: BotModeConfig,
    prepared: Option[PlaySearchIntegration.PreparedSearch],
    matchmaking: Option[PlaySearchIntegration.MatchmakingIntegrationResult],
    error: Option[String],
    authenticated: Boolean,
    accountDashboard: Option[AccountMonetisationUi.AccountDashboard]
)

final case class AccountPageModel(
    dashboard: AccountMonetisationUi.AccountDashboard
)
