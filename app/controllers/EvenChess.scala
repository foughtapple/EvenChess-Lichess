package controllers

import cats.mtl.Handle.*
import java.net.URI
import java.net.http.{ HttpClient, HttpRequest as JHttpRequest, HttpResponse }
import java.time.Duration
import scala.collection.mutable
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.blocking
import scala.util.Random

import chess.Clock
import chess.format.Uci
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
  private val publicSearchKeysByTicket = mutable.Map.empty[String, String]
  private val ticketByPublicSearchKey = mutable.Map.empty[String, (String, String)]
  private val matchedGameRedirectByTicket = mutable.Map.empty[String, String]
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

  private final case class FullGameReviewFrameInput(
      ply: Int,
      fen: String,
      moveUci: Option[String]
  ):
    def valid: Boolean =
      ply >= 0 &&
        fen.nonEmpty &&
        moveUci.forall(_.matches("[a-h][1-8][a-h][1-8][qrbn]?"))

  private final case class ParsedEceQuickContext(
      contextId: Option[String],
      quickRequestId: Option[String],
      deepRequested: Boolean,
      deepStatus: Option[String],
      deepEndpoint: Option[String]
  )

  private final case class ParsedEceBoardPayload(
      response: EngineGateway.EceBoardStateResponse,
      white: ParsedEceSidePayload,
      black: ParsedEceSidePayload,
      fieldsDetected: JsObject,
      quickContext: Option[ParsedEceQuickContext],
      phase: String,
      deepMerged: Boolean,
      deepError: Option[String]
  )

  private final case class ParsedEceDeepSidePayload(
      atoms: MockDisplayOverlayAtoms,
      extras: ParsedEceDisplayExtras,
      fieldsDetected: JsObject
  )

  private final case class ParsedEceDeepPayload(
      white: ParsedEceDeepSidePayload,
      black: ParsedEceDeepSidePayload,
      diagnostics: EngineGateway.EceDiagnostics,
      fieldsDetected: JsObject
  )

  def play = Open:
    val hash = get("mode") match
      case Some("ai") => "#ai"
      case _          => "#hook"
    Redirect(s"/$hash")

  def search = Auth { ctx ?=> me ?=>
    Ok.page(views.evenchess.play(searchModel(me.username.value)))
  }

  def searchJson = Auth { ctx ?=> me ?=>
    val model = searchModel(me.username.value)
    maybeCreateMatchedGameRedirect(model).map: redirectUrl =>
      maybeCreateSimulationBotGamesFromSettings()
      JsonOk(searchJsonPayload(model, redirectUrl))
  }

  def account = Auth { ctx ?=> me ?=>
    val dashboard = AccountMonetisationUi.AccountDashboard.forLichessUser(me.username.value, System.currentTimeMillis)
    Ok.page(views.evenchess.account(AccountPageModel(dashboard)))
  }

  def eceBoardOverlay = Auth { ctx ?=> me ?=>
    val fen = get("fen").filter(_.nonEmpty).getOrElse(defaultTestGroundFen)
    val surface = get("surface").filter(value => value.matches("[A-Za-z0-9_-]{1,40}")).getOrElse("board")
    val gameId =
      get("gameId")
        .filter(value => value.matches("[A-Za-z0-9_.:-]{1,80}"))
        .getOrElse(s"non-live-$surface-${math.abs(fen.hashCode)}")
    val playerId = me.username.value
    val ply = get("ply").flatMap(_.toIntOption).filter(_ >= 0).getOrElse(0)
    val requesterSide =
      get("side").flatMap(parsePerspective).getOrElse(sideToMoveFromFen(fen).getOrElse(Perspective.White))
    val level = Level(10)
    val ttlMillis = get("ttlMillis").flatMap(_.toIntOption).filter(_ > 0).getOrElse(60_000)
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

    cachedBoardOverlay(gameId, playerId, requesterSide, fen, ply, level, ttlMillis) match
      case Some(payload) => Future.successful(JsonOk(payload ++ Json.obj("surface" -> surface, "setLevel" -> level.value)))
      case None =>
        Future(blocking(siteEceBoardOverlayPayload(gameId, playerId, ply, fen, requesterSide, request, ttlMillis))).map:
          case Right(payload) =>
            val sanitized = hidePotentialMovePayload(payload, gameId, requesterSide, level)
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
    val level = levelParam("level", 10)
    val whiteLevel = levelParam("whiteLevel", level.value)
    val blackLevel = levelParam("blackLevel", level.value)
    val requesterLevel = if requesterSide == Perspective.White then whiteLevel else blackLevel
    val ttlMillis = get("ttlMillis").flatMap(_.toIntOption).filter(_ > 0).getOrElse(1500)
    val historyOnly = booleanParam("historyOnly")
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

    cachedBoardOverlay(gameId, playerId, requesterSide, fen, ply, requesterLevel, ttlMillis) match
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
              val sanitized = hidePotentialMovePayload(payload, gameId, requesterSide, requesterLevel)
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
                "message" -> error.take(240)
              )
            )

  def testGroundEceProposedMove = Open:
    val fen = get("fen").filter(_.nonEmpty).getOrElse(defaultTestGroundFen)
    val gameId = get("gameId").filter(_.nonEmpty).getOrElse("test-ground-game")
    val playerId = get("playerId").filter(_.nonEmpty).getOrElse("test-ground-student")
    val ply = get("ply").flatMap(_.toIntOption).filter(_ >= 0).getOrElse(10)
    val proposalIndex = get("proposalIndex").flatMap(_.toIntOption).filter(_ > 0).getOrElse(1)
    val proposedMoveUci = get("moveUci").filter(_.matches("[a-h][1-8][a-h][1-8][qrbn]?")).getOrElse("")
    val requesterSide =
      get("side").flatMap(parsePerspective).getOrElse(sideToMoveFromFen(fen).getOrElse(Perspective.White))
    val movingSide = sideToMoveFromFen(fen).getOrElse(Perspective.White)
    val level = levelParam("level", 10)
    val whiteLevel = levelParam("whiteLevel", level.value)
    val blackLevel = levelParam("blackLevel", level.value)
    val requesterLevel = if requesterSide == Perspective.White then whiteLevel else blackLevel
    val quota = proposedMoveQuotaForLevel(requesterLevel)
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
      aiTextAllowed = false
    )

    if proposedMoveUci.isEmpty then BadRequest(Json.obj("ok" -> false, "error" -> "missing_or_invalid_move"))
    else if requesterLevel.value < 5 then
      BadRequest(Json.obj("ok" -> false, "error" -> "proposed_move_unavailable_for_level", "level" -> requesterLevel.value))
    else if requesterSide != movingSide then
      BadRequest(Json.obj("ok" -> false, "error" -> "not_requester_turn", "sideToMove" -> perspectiveKey(movingSide)))
    else if !request.valid then BadRequest(Json.obj("ok" -> false, "error" -> "invalid_proposed_move_request"))
    else if cached.isDefined then JsonOk(cached.get)
    else if proposedTurnAlreadyUsed(turnKey, cacheKey) then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "proposed_move_already_used_this_turn",
          "message" -> "Proposed Move already used this turn",
          "consumed" -> proposedMoveConsumed(gameId, requesterSide),
          "quota" -> quota
        )
      )
    else if proposedMoveConsumed(gameId, requesterSide) >= quota then
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
          case Right(payload) => JsonOk(rememberProposedMove(cacheKey, turnKey, payload, quota))
          case Left(payload)  => ServiceUnavailable(payload)

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
    val quota = potentialMoveQuotaForLevel(requesterLevel, kind)
    val key = potentialMoveRevealKey(gameId, requesterSide, ply, fen, requesterLevel, kind)
    val cached = cachedPotentialMove(key, quota)
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
    else if cached.isDefined then JsonOk(cached.get)
    else if potentialMoveConsumed(gameId, requesterSide, kind) >= quota then
      BadRequest(
        Json.obj(
          "ok" -> false,
          "error" -> "potential_move_limit_reached",
          "message" -> "Potential Move limit reached",
          "consumed" -> potentialMoveConsumed(gameId, requesterSide, kind),
          "quota" -> quota
        )
      )
    else
      val revealSide = if kind == "player" then requesterSide else opponentOf(requesterSide)
      Future(blocking(testGroundEceBridgePayload(config, gameId, playerId, ply, fen, revealSide, request, 60000)))
        .map:
          case Right(payload) =>
            potentialMovePayloadFromBridge(payload, key, kind, playerId, revealSide, requesterLevel, quota) match
              case Right(reveal) => JsonOk(rememberPotentialMove(key, reveal))
              case Left(message) =>
                BadRequest(
                  Json.obj(
                    "ok" -> false,
                    "error" -> "potential_moves_unavailable",
                    "message" -> message,
                    "consumed" -> potentialMoveConsumed(gameId, requesterSide, kind),
                    "quota" -> quota
                  )
                )
          case Left(payload) => ServiceUnavailable(payload)

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
    callEceBoardQuick(config, request) match
      case Left(error) =>
        Left(eceBridgeErrorJson("ece_unavailable", error, config, request))
      case Right(eceJson) =>
        parseEceBoardPayload(eceJson) match
          case Left(error) =>
            Left(eceBridgeErrorJson("ece_payload_rejected", error, config, request))
          case Right(quickParsed) =>
            val parsed = maybeMergeDeepPayload(config, request, quickParsed)
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
            Right(eceBridgeSuccessJson(config, request, requesterSide, result, parsed, sidePayload, ttlMillis))

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

  private def fullGameReviewRequest(
      gameId: String,
      frames: List[FullGameReviewFrameInput],
      level: Level
  ): EngineGateway.EceGameReviewRequest =
    val sorted = frames.sortBy(_.ply)
    val game =
      EngineGateway.EceGameReviewInput(
        gameId = gameId,
        initialFen = sorted.headOption.map(_.fen).getOrElse(defaultTestGroundFen),
        pgn = None,
        moves = sorted.flatMap(_.moveUci),
        fenHistory = sorted.map(_.fen),
        result = "unknown",
        termination = "unknown"
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
      val storedPayload = hidePotentialMovePayload(Json.obj("ok" -> true, "live" -> live), gameId, requesterSide, level)
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
          val sanitized = hidePotentialMovePayload(payload, gameId, requesterSide, level)
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

  private def maybeMergeDeepPayload(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceBoardStateRequest,
      quickParsed: ParsedEceBoardPayload
  ): ParsedEceBoardPayload =
    val contextId =
      quickParsed.quickContext.flatMap(context =>
        context.contextId.filter(_.nonEmpty).filter(_ => context.deepStatus.contains("ready_to_request"))
      )

    if !request.deepRequested then quickParsed
    else
      contextId match
        case None =>
          quickParsed.copy(deepError = Some("deep_not_ready"))
        case Some(id) =>
          val deepRequest = EngineGateway.EceBoardDeepRequest.fromQuick(request, id)
          if !deepRequest.valid then quickParsed.copy(deepError = Some("invalid_deep_request"))
          else
            callEceBoardDeep(config, deepRequest) match
              case Left(error) =>
                quickParsed.copy(deepError = Some(error.take(160)))
              case Right(deepJson) =>
                parseEceDeepPayload(deepJson, request, deepRequest) match
                  case Left(error) => quickParsed.copy(deepError = Some(error.take(160)))
                  case Right(deep) => mergeDeepPayload(quickParsed, deep)

  private def callEceBoardQuick(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceBoardStateRequest
  ): Either[String, JsValue] =
    postEceJson(config.boardQuickUrl, eceBoardRequestJson(request), timeoutSeconds = 8, label = "ECE board quick endpoint")

  private def callEceBoardDeep(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceBoardDeepRequest
  ): Either[String, JsValue] =
    postEceJson(config.boardDeepUrl, eceBoardDeepRequestJson(request), timeoutSeconds = 12, label = "ECE board deep endpoint")

  private def callEceProposedMove(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceProposedMoveRequest
  ): Either[String, JsValue] =
    postEceJson(config.proposedMoveUrl, eceProposedMoveRequestJson(request), timeoutSeconds = 10, label = "ECE proposed-move endpoint")

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
    catch case error: Exception => Left(error.getMessage)

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
        "deep_requested" -> request.deepRequested,
        "requested_deep_modules" -> request.requestedDeepModules,
        "custom" -> Json.obj(
          "opening" -> request.custom.opening,
          "instructions" -> request.custom.instructions
        )
      )
    )

  private def eceBoardDeepRequestJson(request: EngineGateway.EceBoardDeepRequest): JsObject =
    Json.obj(
      "request" -> Json.obj(
        "mode" -> request.mode,
        "request_id" -> request.requestId,
        "quick_request_id" -> request.quickRequestId,
        "quick_context_id" -> request.quickContextId,
        "input_fen" -> request.inputFen,
        "white_level" -> request.whiteLevel.value,
        "black_level" -> request.blackLevel.value,
        "use_ai" -> request.useAi,
        "requested_deep_modules" -> request.requestedDeepModules
      )
    )

  private def eceProposedMoveRequestJson(request: EngineGateway.EceProposedMoveRequest): JsObject =
    Json.obj(
      "request" -> Json.obj(
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
      val quickContext = parseQuickContext(json)
      val phase = stringField(field(json, "schema").getOrElse(Json.obj()), "phase").getOrElse("quick")
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
          "quick_context" -> Json.obj(
            "context_id" -> quickContext.flatMap(_.contextId),
            "quick_request_id" -> quickContext.flatMap(_.quickRequestId),
            "deep_requested" -> quickContext.exists(_.deepRequested),
            "deep_status" -> quickContext.flatMap(_.deepStatus),
            "deep_endpoint" -> quickContext.flatMap(_.deepEndpoint)
          ),
          "has_public_position" -> response.hasPublicPosition,
          "has_public_shared_calculations" -> response.hasPublicSharedCalculations
        ),
        quickContext = quickContext,
        phase = phase,
        deepMerged = false,
        deepError = None
      )

  private def parseQuickContext(json: JsValue): Option[ParsedEceQuickContext] =
    objectField(json, "quick_context").map: context =>
      ParsedEceQuickContext(
        contextId = stringField(context, "context_id"),
        quickRequestId = stringField(context, "quick_request_id"),
        deepRequested = booleanField(context, "deep_requested").getOrElse(false),
        deepStatus = stringField(context, "deep_status"),
        deepEndpoint = stringField(context, "deep_endpoint")
      )

  private def parseEceDeepPayload(
      json: JsValue,
      quickRequest: EngineGateway.EceBoardStateRequest,
      deepRequest: EngineGateway.EceBoardDeepRequest
  ): Either[String, ParsedEceDeepPayload] =
    val echo = field(json, "request_echo").getOrElse(Json.obj())
    val addenda = field(json, "side_output_addenda").getOrElse(Json.obj())
    for
      requestId <- stringField(echo, "request_id").toRight("missing deep request_echo.request_id")
      quickRequestId <- stringField(echo, "quick_request_id").toRight("missing deep request_echo.quick_request_id")
      inputFen <- stringField(echo, "input_fen").toRight("missing deep request_echo.input_fen")
      whiteLevel <- levelField(echo, "white_level").toRight("missing or invalid deep request_echo.white_level")
      blackLevel <- levelField(echo, "black_level").toRight("missing or invalid deep request_echo.black_level")
      _ <- Either.cond(requestId == deepRequest.requestId, (), "deep request_id does not match outstanding request")
      _ <- Either.cond(quickRequestId == quickRequest.requestId, (), "deep quick_request_id does not match quick request")
      _ <- Either.cond(inputFen == quickRequest.inputFen, (), "deep input_fen does not match current FEN")
      _ <- Either.cond(whiteLevel == quickRequest.whiteLevel, (), "deep white_level does not match current level")
      _ <- Either.cond(blackLevel == quickRequest.blackLevel, (), "deep black_level does not match current level")
      diagnostics = parseDiagnostics(json)
      _ <- Either.cond(diagnostics.displayAllowed, (), "deep diagnostics status is not displayable")
      _ <- Either.cond(field(json, "position").isEmpty, (), "deep payload exposed public position")
      _ <- Either.cond(field(json, "shared_calculations").isEmpty, (), "deep payload exposed public shared_calculations")
      _ <- Either.cond(field(json, "raw_provider_output").isEmpty, (), "deep payload exposed raw provider output")
      white <- parseDeepSidePayload(addenda, "white")
      black <- parseDeepSidePayload(addenda, "black")
    yield
      ParsedEceDeepPayload(
        white = white,
        black = black,
        diagnostics = diagnostics,
        fieldsDetected = Json.obj(
          "diagnostics_status" -> diagnostics.status.toString,
          "has_public_position" -> field(json, "position").isDefined,
          "has_public_shared_calculations" -> field(json, "shared_calculations").isDefined,
          "white" -> white.fieldsDetected,
          "black" -> black.fieldsDetected
        )
      )

  private def parseDeepSidePayload(
      sideAddenda: JsValue,
      key: String
  ): Either[String, ParsedEceDeepSidePayload] =
    field(sideAddenda, key) match
      case None =>
        Right(ParsedEceDeepSidePayload(MockDisplayOverlayAtoms.empty, ParsedEceDisplayExtras(Nil, Nil), Json.obj()))
      case Some(sideJson) =>
        Either.cond(
          sideJson.asOpt[Int].contains(0) || sideJson.isInstanceOf[JsObject],
          ParsedEceDeepSidePayload(
            atoms = parseAtoms(sideJson, if key == "white" then Perspective.White else Perspective.Black),
            extras = parseDisplayExtras(sideJson, includeEval = true, evalFeatureKey = "ece.eval.deep"),
            fieldsDetected = sideFieldsDetected(sideJson)
          ),
          s"invalid deep side_output_addenda.$key"
        )

  private def mergeDeepPayload(quick: ParsedEceBoardPayload, deep: ParsedEceDeepPayload): ParsedEceBoardPayload =
    quick.copy(
      white = mergeDeepSide(quick.white, deep.white),
      black = mergeDeepSide(quick.black, deep.black),
      fieldsDetected = quick.fieldsDetected ++ Json.obj(
        "deep_merged" -> true,
        "deep" -> deep.fieldsDetected
      ),
      deepMerged = true,
      deepError = None
    )

  private def mergeDeepSide(quick: ParsedEceSidePayload, deep: ParsedEceDeepSidePayload): ParsedEceSidePayload =
    quick.copy(
      atoms = mergeAtoms(quick.atoms, deep.atoms),
      extras = mergeExtras(quick.extras, deep.extras),
      fieldsDetected = quick.fieldsDetected ++ Json.obj("deep" -> deep.fieldsDetected)
    )

  private def mergeExtras(left: ParsedEceDisplayExtras, right: ParsedEceDisplayExtras): ParsedEceDisplayExtras =
    ParsedEceDisplayExtras(
      cards = (left.cards ++ right.cards).distinctBy(_.featureKey),
      visuals = (left.visuals ++ right.visuals).foldLeft(List.empty[EceLiveBridge.ExtraVisual]): (acc, visual) =>
        acc.filterNot(_.featureKey == visual.featureKey) :+ visual
    )

  private def mergeAtoms(left: MockDisplayOverlayAtoms, right: MockDisplayOverlayAtoms): MockDisplayOverlayAtoms =
    MockDisplayOverlayAtoms(
      hangingAttackable = (left.hangingAttackable ++ right.hangingAttackable).distinct,
      hangingNotAttackable = (left.hangingNotAttackable ++ right.hangingNotAttackable).distinct,
      offsetCount = (left.offsetCount ++ right.offsetCount).distinct,
      studentThreats = (left.studentThreats ++ right.studentThreats).distinct,
      opponentThreats = (left.opponentThreats ++ right.opponentThreats).distinct,
      pins = (left.pins ++ right.pins).distinct,
      studentHangingAttackable = (left.studentHangingAttackable ++ right.studentHangingAttackable).distinct,
      opponentHangingAttackable = (left.opponentHangingAttackable ++ right.opponentHangingAttackable).distinct
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
          summary = safePayloadTextField(sideJson, "summary", 120),
          immediateWarning = safePayloadTextField(sideJson, "immediate_warning", 96),
          plan = safePayloadTextField(sideJson, "plan", 160)
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
    val flatThreats = arrayField(overlays, "threats")
    val pins = arrayField(overlays, "pins") ++
      arrayField(pinGroups, "student_pinned") ++
      arrayField(pinGroups, "opponent_pinned")
    val opponent = opponentOf(requesterSide)
    val studentThreats = arrayField(threatGroups, "student_threats") ++
      flatThreats.filter(item => sideField(item).forall(_ == requesterSide))
    val opponentThreats = arrayField(threatGroups, "opponent_threats") ++
      flatThreats.filter(item => sideField(item).contains(opponent))
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
    val candidateItems =
      (arrayField(sideJson, "potential_moves") ++ arrayField(sideJson, "candidates") ++ arrayField(sideJson, "candidate_moves"))
        .take(3)
        .zipWithIndex
    val candidateCards =
      candidateItems.flatMap {
        case (item, index) =>
          val san = firstSafeText(item, 16, "san", "uci")
          val reason = candidateReasonText(item)
          (san, reason) match
          case (Some(move), Some(body)) =>
            Some(EceLiveBridge.ExtraCard(s"ece.candidate.${index + 1}", s"Potential ${index + 1}", s"$move: $body"))
          case _ => None
      }
    val candidateVisuals =
      candidateItems.flatMap { case (item, index) =>
        candidateUciSquares(item).map { case (from, to) =>
          val label = List(
            Some(s"Potential ${candidateLetter(index)}"),
            firstSafeText(item, 20, "san", "category")
          ).flatten.mkString(" ")
          EceLiveBridge.ExtraVisual(
            s"ece.candidate.${index + 1}",
            s"$from-$to: ${safeDisplayText(label, 40).getOrElse(s"Potential ${candidateLetter(index)}")}",
            primary = false
          )
        }
      }

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
        safeTextField(item, "risk_summary_input", 160).map: body =>
          EceLiveBridge.ExtraCard("ece.human_risk", "Human Risk", body)

    ParsedEceDisplayExtras(
      cards = candidateCards ++ openingCards ++ proposedCards ++ reviewCards ++ humanRiskCards,
      visuals = candidateVisuals ++ evalVisuals
    )

  private def eceBridgeSuccessJson(
      config: EngineGateway.EceServiceConfig,
      request: EngineGateway.EceBoardStateRequest,
      requesterSide: Perspective,
      result: EceLiveBridge.BoardOverlayResult,
      parsed: ParsedEceBoardPayload,
      sidePayload: ParsedEceSidePayload,
      ttlMillis: Int
  ): JsObject =
    val now = System.currentTimeMillis
    val live = roundOverlayJson(result.roundPayload, ttlMillis, now)
    Json.obj(
      "ok" -> result.valid,
      "coachingAvailable" -> result.coachingAvailable,
      "browserDirectCallBlocked" -> result.decision.browserDirectCallBlocked,
      "nonFatalToGameLifecycle" -> result.decision.nonFatalToGameLifecycle,
      "requesterSide" -> perspectiveKey(requesterSide),
      "ece" -> Json.obj(
        "boardUrl" -> config.boardQuickUrl,
        "boardQuickUrl" -> config.boardQuickUrl,
        "boardDeepUrl" -> config.boardDeepUrl,
        "requestId" -> request.requestId,
        "deepRequested" -> request.deepRequested,
        "requestedDeepModules" -> request.requestedDeepModules,
        "deepMerged" -> parsed.deepMerged,
        "deepError" -> parsed.deepError,
        "quickContextStatus" -> parsed.quickContext.flatMap(_.deepStatus),
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

  private def hidePotentialMovePayload(
      payload: JsObject,
      gameId: String,
      requesterSide: Perspective,
      level: Level
  ): JsObject =
    objectField(payload, "live") match
      case None => payload
      case Some(live) =>
        val filteredLive = liveWithAssistanceUsage(filterPotentialMoveLivePayload(live), gameId, requesterSide, level)
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

  private def cachedBoardOverlay(
      gameId: String,
      playerId: String,
      requesterSide: Perspective,
      fen: String,
      ply: Int,
      requestedLevel: Level,
      ttlMillis: Int
  ): Option[JsObject] =
    val key = boardOverlayCacheKey(gameId, playerId, requesterSide, fen)
    val now = System.currentTimeMillis
    boardOverlayCacheLock.synchronized:
      boardOverlayCache.get(key).filter(_.level.value >= requestedLevel.value).map: cached =>
        val refreshed = cached.copy(cachedAt = now)
        boardOverlayCache.update(key, refreshed)
        refreshCachedBoardOverlayPayload(refreshed, requestedLevel, ply, ttlMillis, now)

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
      now: Long
  ): JsObject =
    val live =
      objectField(cached.payload, "live")
        .map(live =>
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
      level: Level
  ): JsObject =
    live ++ Json.obj(
      "assistance" -> Json.obj(
        "proposedMove" -> Json.obj(
          "consumed" -> proposedMoveConsumed(gameId, requesterSide),
          "quota" -> proposedMoveQuotaForLevel(level)
        ),
        "potentialMoves" -> Json.obj(
          "consumedByKind" -> Json.obj(
            "player" -> potentialMoveConsumed(gameId, requesterSide, "player"),
            "opponent" -> potentialMoveConsumed(gameId, requesterSide, "opponent")
          ),
          "quotaByKind" -> Json.obj(
            "player" -> potentialMoveQuotaForLevel(level, "player"),
            "opponent" -> potentialMoveQuotaForLevel(level, "opponent")
          )
        )
      )
    )

  private def filterPotentialMoveLivePayload(live: JsObject): JsObject =
    live ++ Json.obj(
      "cards" -> arrayField(live, "cards").filterNot(isPotentialMoveDisplayItem),
      "visuals" -> arrayField(live, "visuals").filterNot(isPotentialMoveDisplayItem)
    )

  private def potentialMovePayloadFromBridge(
      payload: JsObject,
      key: String,
      kind: String,
      playerId: String,
      revealSide: Perspective,
      level: Level,
      quota: Int
  ): Either[String, JsObject] =
    objectField(payload, "live").toRight("No live payload returned by ECE").flatMap: live =>
      val cards = arrayField(live, "cards").filter(isPotentialMoveDisplayItem)
      val visuals = arrayField(live, "visuals").filter(isPotentialMoveDisplayItem)
      if cards.isEmpty && visuals.isEmpty then Left("No potential moves in payload")
      else
        Right(
          Json.obj(
            "ok" -> true,
            "potential" -> Json.obj(
              "key" -> key,
              "gameId" -> stringField(live, "gameId").getOrElse(""),
              "playerId" -> playerId,
              "ply" -> intField(live, "ply").getOrElse(0),
              "boardStateKey" -> stringField(live, "boardStateKey").getOrElse(""),
              "perspective" -> perspectiveKey(revealSide),
              "kind" -> kind,
              "level" -> level.value,
              "quota" -> quota,
              "consumed" -> 0,
              "cards" -> cards,
              "visuals" -> visuals,
              "auditId" -> stringField(live, "auditId").getOrElse(""),
              "serverAuthorized" -> true,
              "approvedDisplayPayload" -> true
            )
          )
        )

  private def isPotentialMoveDisplayItem(item: JsValue): Boolean =
    val text = List(
      stringField(item, "featureKey"),
      stringField(item, "title"),
      stringField(item, "body"),
      stringField(item, "label")
    ).flatten.mkString(" ").toLowerCase
    text.contains("candidate") || text.contains("potential")

  private def proposedMoveQuotaForLevel(level: Level): Int =
    if level.value >= 8 then 3
    else if level.value >= 6 then 2
    else if level.value >= 5 then 1
    else 0

  private def potentialMoveQuotaForLevel(level: Level, kind: String): Int =
    if kind == "player" then
      if level.value >= 8 then 3
      else if level.value >= 7 then 2
      else if level.value >= 6 then 1
      else 0
    else if level.value >= 8 then 3
    else if level.value >= 7 then 2
    else if level.value >= 5 then 1
    else 0

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

  private def proposedMoveConsumed(gameId: String, requesterSide: Perspective): Int =
    assistanceLock.synchronized:
      proposedTurnToMove.keys.count(_.startsWith(assistanceSidePrefix(gameId, requesterSide)))

  private def potentialMoveConsumed(gameId: String, requesterSide: Perspective, kind: String): Int =
    assistanceLock.synchronized:
      potentialMoveCache.keys.count(key => key.startsWith(assistanceSidePrefix(gameId, requesterSide)) && key.contains(s":potential:$kind:"))

  private def cachedProposedMove(
      cacheKey: String,
      gameId: String,
      requesterSide: Perspective,
      quota: Int
  ): Option[JsObject] =
    assistanceLock.synchronized:
      proposedMoveCache
        .get(cacheKey)
        .map(payload => payload ++ Json.obj("consumed" -> proposedMoveConsumed(gameId, requesterSide), "quota" -> quota))

  private def proposedTurnAlreadyUsed(turnKey: String, cacheKey: String): Boolean =
    assistanceLock.synchronized:
      proposedTurnToMove.get(turnKey).exists(_ != cacheKey)

  private def rememberProposedMove(cacheKey: String, turnKey: String, payload: JsObject, quota: Int): JsObject =
    assistanceLock.synchronized:
      proposedTurnToMove.update(turnKey, cacheKey)
      val consumed = proposedTurnToMove.keys.count(_.startsWith(assistanceSidePrefixFromKey(turnKey)))
      val enriched = payload ++ Json.obj("consumed" -> math.min(consumed, quota), "quota" -> quota)
      proposedMoveCache.update(cacheKey, enriched)
      enriched

  private def cachedPotentialMove(key: String, quota: Int): Option[JsObject] =
    assistanceLock.synchronized:
      potentialMoveCache.get(key).map(payload => updatePotentialConsumed(payload, key, quota, cached = true))

  private def rememberPotentialMove(key: String, payload: JsObject): JsObject =
    assistanceLock.synchronized:
      val quota = objectField(payload, "potential").flatMap(potential => intField(potential, "quota")).getOrElse(0)
      val enriched = updatePotentialConsumed(payload, key, quota, cached = false)
      potentialMoveCache.update(key, enriched)
      enriched

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
            "cached" -> cached
          ))
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
        "boardUrl" -> config.boardQuickUrl,
        "boardQuickUrl" -> config.boardQuickUrl,
        "boardDeepUrl" -> config.boardDeepUrl,
        "requestId" -> request.requestId,
        "deepRequested" -> request.deepRequested,
        "requestedDeepModules" -> request.requestedDeepModules
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
      echoMove <- stringField(echo, "proposed_move_uci").toRight("missing request_echo.proposed_move_uci")
      _ <- Either.cond(requestId == request.requestId, (), "proposed request_id does not match outstanding request")
      _ <- Either.cond(inputFen == fen && inputFen == request.inputFen, (), "proposed input_fen does not match current FEN")
      _ <- Either.cond(echoMove == request.proposedMoveUci, (), "proposed move echo does not match current arrow")
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
        safePayloadTextField(evaluation, "summary", 180)
          .orElse(safeTextField(evaluation, "sentence", 180))
          .orElse(objectField(evaluation, "coaching").flatMap(coaching => safePayloadTextField(coaching, "text", 180)))
          .orElse(safePayloadTextField(evaluation, "advice", 180))
      val planText =
        safePayloadTextField(evaluation, "plan", 160)
      val warningText =
        arrayField(evaluation, "warnings")
          .flatMap(value => stringValue(value).flatMap(safeDisplayText(_, 140)))
          .headOption
      val legal = booleanField(evaluation, "legal")
      val newFen = safePayloadTextField(evaluation, "new_fen", 180)
      val postMoveBoardStateKey = newFen.getOrElse(s"$fen ${request.proposedMoveUci}")
      val evalAfter = field(evaluation, "eval_after").filterNot(isZeroValue)
      val afterMoveSideOutput =
        if legal.contains(true) then
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

  private def sideFieldsDetected(sideJson: JsValue): JsObject =
    val overlays = field(sideJson, "overlays").getOrElse(Json.obj())
    val tradeStatus = field(overlays, "trade_status").getOrElse(Json.obj())
    val threatGroups = field(overlays, "threats").getOrElse(Json.obj())
    val pinGroups = field(overlays, "pinned_pieces").getOrElse(Json.obj())
    Json.obj(
      "summary" -> safePayloadTextField(sideJson, "summary", 120).isDefined,
      "immediateWarning" -> safePayloadTextField(sideJson, "immediate_warning", 96).isDefined,
      "plan" -> safePayloadTextField(sideJson, "plan", 160).isDefined,
      "cards" -> arrayField(sideJson, "cards").size,
      "offsetCount" -> (arrayField(overlays, "offset_count").size + arrayField(tradeStatus, "offset_count").size),
      "hangingPieces" -> (
        arrayField(overlays, "hanging_pieces").size +
          arrayField(tradeStatus, "hanging_pieces").size +
          arrayField(tradeStatus, "hanging_attackable").size +
          arrayField(tradeStatus, "hanging_not_attackable").size
      ),
      "threats" -> (
        arrayField(overlays, "threats").size +
          arrayField(threatGroups, "student_threats").size +
          arrayField(threatGroups, "opponent_threats").size
      ),
      "pins" -> (
        arrayField(overlays, "pins").size +
          arrayField(pinGroups, "student_pinned").size +
          arrayField(pinGroups, "opponent_pinned").size
      ),
      "opening" -> (arrayField(overlays, "opening").size + objectField(sideJson, "opening").fold(0)(_ => 1)),
      "eval" -> (field(overlays, "eval").isDefined || field(sideJson, "evaluation").isDefined),
      "potentialMoves" -> (
        arrayField(sideJson, "potential_moves").size +
          arrayField(sideJson, "candidates").size +
          arrayField(sideJson, "candidate_moves").size
      ),
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

  private def objectField(json: JsValue, key: String): Option[JsObject] =
    field(json, key).collect { case obj: JsObject => obj }

  private def stringField(json: JsValue, key: String): Option[String] =
    field(json, key).flatMap(_.asOpt[String]).filter(_.nonEmpty)

  private def stringValue(json: JsValue): Option[String] =
    json.asOpt[String].filter(_.nonEmpty)

  private def intField(json: JsValue, key: String): Option[Int] =
    field(json, key).flatMap(value => value.asOpt[Int].orElse(value.asOpt[String].flatMap(_.toIntOption)))

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

  private def safePayloadTextField(json: JsValue, key: String, maxLength: Int): Option[String] =
    safeTextField(json, key, maxLength).orElse(field(json, key).flatMap(value => safeTextField(value, "text", maxLength)))

  private def firstSafeText(json: JsValue, maxLength: Int, keys: String*): Option[String] =
    keys.toList.view.flatMap(key => safeTextField(json, key, maxLength)).headOption

  private def safeDisplayText(text: String, maxLength: Int): Option[String] =
    val normalized = text.replaceAll("[\\r\\n\\t]+", " ").trim
    Option.when(normalized.nonEmpty && !unsafeDisplayText(normalized))(normalized.take(maxLength))

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
    keys.toList.view.flatMap(key => stringField(json, key)).find(_.matches("[a-h][1-8]"))

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
    stringValue(json).flatMap(fromToText).orElse:
      for
        from <- squareFromKeys(json, "from", "from_square", "source_square", "attacker_square")
        to <- squareFromKeys(json, "to", "to_square", "target_square", "victim_square", "attacked_square")
      yield from -> to

  private def fromToText(text: String): Option[(String, String)] =
    val arrowPattern = "(?i)([a-h][1-8])\\s*(?:-|->|to|x)\\s*([a-h][1-8])".r
    arrowPattern.findFirstMatchIn(text).map(matchResult =>
      matchResult.group(1).toLowerCase -> matchResult.group(2).toLowerCase
    )

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

  private def sideField(json: JsValue): Option[Perspective] =
    stringField(json, "side").flatMap(parsePerspective)

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

  private def perspectiveKey(side: Perspective): String =
    side match
      case Perspective.White => "white"
      case Perspective.Black => "black"

  private def sideToMoveFromFen(fen: String): Option[Perspective] =
    fen.trim.split("\\s+").lift(1).flatMap:
      case "w" => Some(Perspective.White)
      case "b" => Some(Perspective.Black)
      case _   => None

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
    searchStateLock.synchronized:
      ticketByPublicSearchKey.get(searchKey).collect:
        case (ticketId, ownerPlayerId) if ownerPlayerId == playerId => ticketId

  private def publicSearchKeyFor(ticketId: String, playerId: String): String =
    searchStateLock.synchronized:
      publicSearchKeysByTicket.getOrElseUpdate(
        ticketId,
        {
          val key = s"ecs${java.util.UUID.randomUUID().toString.replace("-", "").take(30)}"
          ticketByPublicSearchKey.put(key, (ticketId, playerId))
          key
        }
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

  private def searchModel(playerId: String)(using Context): PlayPageModel =
    activateEcorRuntimeFromSettings()
    val now = System.currentTimeMillis
    val freeMatchTokenWindow = freeMatchTokenWindowFromSettings
    val baseAccountDashboard = AccountMonetisationUi.AccountDashboard.forLichessUser(playerId, now)
    val accountDashboard =
      baseAccountDashboard.copy(
        tokenSnapshot = baseAccountDashboard.tokenSnapshot.withFreeMatchTokenWindow(freeMatchTokenWindow.activeAt(now))
      )
    val botMode =
      PlaySearchIntegration.BotModeConfig.fromSettings(
        enabled = env.web.settings.evenChessMatchmakingBotModeEnabled.get(),
        scope = env.web.settings.evenChessMatchmakingBotModeScope.get(),
        timeoutSeconds = env.web.settings.evenChessMatchmakingBotMatchTimeoutSeconds.get(),
        accountRoster = env.web.settings.evenChessMatchmakingBotAccountRoster.get()
      )
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

  private def maybeCreateMatchedGameRedirect(model: PlayPageModel): Future[Option[String]] =
    val ticketId = model.prepared.map(_.record.ticket.ticketId)
    val existingRedirect = ticketId.flatMap(id => searchStateLock.synchronized(matchedGameRedirectByTicket.get(id)))
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
            case false => Future.successful(None)
            case true =>
              allow:
                env.challenge.api
                  .accept(challenge, anonSecret = None, requestedColor = Some(candidateColor))(using Some(destMe))
                  .map:
                    case Some(acceptedPov) =>
                      persistMatchedGamePolicy(challenge.gameId.value, requestRecord, candidateRecord, contract)
                      registerManagedBotsForGame(acceptedPov.game, requestRecord, candidateRecord)
                      val requestRedirect = routes.Round.player(acceptedPov.game.fullIdOf(requestColor)).url
                      val candidateRedirect = routes.Round.player(acceptedPov.game.fullIdOf(candidateColor)).url
                      searchStateLock.synchronized:
                        matchedGameRedirectByTicket.put(requestRecord.ticket.ticketId, requestRedirect)
                        matchedGameRedirectByTicket.put(candidateRecord.ticket.ticketId, candidateRedirect)
                      retireMatchedTickets(requestRecord, candidateRecord)
                      Some(requestRedirect)
                    case None => None
              .rescue: _ =>
                Future.successful(None)
        case _ => Future.successful(None)
    yield redirect

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
