package controllers

import lila.app.{ *, given }
import lila.evenchess.AdminBackendSettings
import lila.evenchess.AdminOpsDashboard
import lila.evenchess.BotOperations
import lila.evenchess.EvenChessRatingCalibration
import lila.evenchess.PlaySearchIntegration
import lila.core.security.ClearPassword
import lila.core.userId.UserId

final class Dev(env: Env) extends LilaController(env):

  private val evenChessRosterPerfKeys = List(
    lila.rating.PerfType.Bullet.key,
    lila.rating.PerfType.Blitz.key,
    lila.rating.PerfType.Rapid.key,
    lila.rating.PerfType.Classical.key,
    lila.rating.PerfType.Correspondence.key,
    lila.rating.PerfType.Standard.key
  )

  private def establishedRosterPerf(account: String): lila.core.perf.Perf =
    val rating = BotOperations.BotAccountRoster.establishedDisplayRating(account)
    lila.core.perf.Perf(
      glicko = chess.rating.glicko.Glicko(rating.toDouble, 60d, lila.rating.Glicko.defaultVolatility),
      nb = 30,
      recent = List.fill(12)(chess.IntRating(rating)),
      latest = Some(nowInstant)
    )

  private def seedRosterAccountPerfs(account: String): Funit =
    val perf = establishedRosterPerf(account)
    evenChessRosterPerfKeys.foldLeft(funit): (acc, perfKey) =>
      acc.flatMap(_ => env.user.perfsRepo.setPerf(UserId(account), perfKey, perf))

  def settings = Secure(_.Settings) { _ ?=> _ ?=>
    Ok.page:
      views.dev.settings(settingsList)
  }

  def settingsPost(id: String) = SecureBody(_.Settings) { _ ?=> me ?=>
    settingsList.flatMap(_._2).find(_.id == id).so { setting =>
      bindForm(setting.form)(
        _ => BadRequest.page(views.dev.settings(settingsList)),
        v =>
          val newValue = v.toString
          if AdminBackendSettings.isUnsafeEvenChessBackendValue(id, newValue) then
            lila
              .log("setting")
              .warn(s"${me.username} blocked raw-like value update for $id")
            Redirect(routes.Dev.settings).flashFailure("Refusing to persist a raw-seeming EvenChess backend secret; configure provider/API keys in environment instead.")
          else
            val oldValue = AdminBackendSettings.safeLogValue(id, setting.get().toString)
            val safeNewValue = AdminBackendSettings.safeLogValue(id, newValue)
            lila
              .log("setting")
              .info(s"${me.username} changes $id from $oldValue to $safeNewValue")
            setting.setString(newValue).inject(Redirect(routes.Dev.settings))
      )
    }
  }

  def evenChessOps = Secure(_.Settings) { _ ?=> _ ?=>
    val query = AdminOpsDashboard.AuditSearchQuery(get("q").getOrElse(""))
    Ok.page:
      views.dev.evenChessOps(
        AdminOpsDashboard.AdminOpsDashboardModel.build(query, evenChessBackendSettingsSnapshot, System.currentTimeMillis)
      )
  }

  def evenChessBotOpsPanel = Secure(_.Settings) { _ ?=> _ ?=>
    val model = AdminOpsDashboard.AdminOpsDashboardModel.build(
      AdminOpsDashboard.AuditSearchQuery(""),
      evenChessBackendSettingsSnapshot,
      System.currentTimeMillis
    )
    Ok.page:
      views.dev.evenChessBotOpsPanel(model)
  }

  def evenChessEcorPanel = Secure(_.Settings) { _ ?=> _ ?=>
    val model = AdminOpsDashboard.AdminOpsDashboardModel.build(
      AdminOpsDashboard.AuditSearchQuery(""),
      evenChessBackendSettingsSnapshot,
      System.currentTimeMillis
    )
    Ok.page:
      views.dev.evenChessEcorPanel(model)
  }

  def evenChessBotOps(action: String) = SecureBody(parse.tolerantFormUrlEncoded)(_.Settings) { ctx ?=> me ?=>
    val form = ctx.body.body
    val now = System.currentTimeMillis

    def raw(name: String): Option[String] =
      form.get(name).flatMap(_.headOption).map(_.trim)

    def rawInt(name: String, fallback: Int): Int =
      raw(name).flatMap(_.toIntOption).getOrElse(fallback)

    def rawBool(name: String): Boolean =
      raw(name).exists(value => value == "true" || value == "on" || value == "1")

    def redirect(message: String) =
      Redirect(routes.Dev.evenChessBotOpsPanel.url).flashSuccess(message)

    def redirectFailure(message: String) =
      Redirect(routes.Dev.evenChessBotOpsPanel.url).flashFailure(message)

    def safeScope(value: String): String =
      PlaySearchIntegration.BotModeScope.fromRaw(value).getOrElse(PlaySearchIntegration.BotModeScope.Both).label

    val webSettings = env.web.settings

    def currentSharedBotRosterCsv: String =
      val matchmakingRoster = BotOperations.BotAccountRoster.fromCsv(webSettings.evenChessMatchmakingBotAccountRoster.get())
      val simulationRoster = BotOperations.BotAccountRoster.fromCsv(webSettings.evenChessBotSimulationAccountRoster.get())
      BotOperations.BotAccountRoster.csvFor(
        if matchmakingRoster.nonEmpty then matchmakingRoster
        else if simulationRoster.nonEmpty then simulationRoster
        else BotOperations.BotAccountRoster.generatedDefault
      )

    def normalizeBotRoster(rawValue: Option[String]): String =
      val parsed = rawValue.fold(Nil)(BotOperations.BotAccountRoster.fromCsv)
      BotOperations.BotAccountRoster.csvFor(
        if parsed.nonEmpty then parsed else BotOperations.BotAccountRoster.generatedDefault
      )

    def saveSharedBotRoster(accountRoster: String) =
      for
        _ <- webSettings.evenChessMatchmakingBotAccountRoster.setString(accountRoster)
        _ <- webSettings.evenChessBotSimulationAccountRoster.setString(accountRoster)
      yield ()

    def stopSimulationForExclusiveMode(adminId: String, now: Long) =
      val repository = PlaySearchIntegration.SearchRepositoryRuntime.local
      BotOperations.clearSimulationTickets(repository)
      BotOperations.BotSimulationRuntime.stop(adminId, now)
      webSettings.evenChessBotSimulationEnabled.setString("false")

    def provisionRosterAccounts(accounts: List[String], count: Int): Fu[(Int, Int, Int, List[String])] =
      val selected = accounts.take(count)
      val passwordHash = env.security.authenticator.passEnc:
        ClearPassword(s"evenchess-roster-${now}-${me.username.value}-${java.util.UUID.randomUUID()}")
      selected.foldLeft(fuccess((0, 0, 0, List.empty[String]))):
        case (resultFu, account) =>
          resultFu.flatMap: (created, existing, botTitled, skipped) =>
            val userName = UserName(account)
            env.user.repo.byId(userName).flatMap:
              case Some(user) if !user.enabled.yes =>
                fuccess((created, existing, botTitled, skipped :+ s"$account exists but is disabled"))
              case Some(user) if user.isBot =>
                fuccess((created, existing + 1, botTitled + 1, skipped))
              case Some(_) =>
                seedRosterAccountPerfs(account).inject((created, existing + 1, botTitled, skipped))
              case None =>
                env.user.repo
                  .create(
                    name = userName,
                    passwordHash = passwordHash,
                    email = EmailAddress(s"$account@evenchess-bots.local"),
                      blind = false,
                      mustConfirmEmail = false
                    )
                  .flatMap:
                    case Some(_) => seedRosterAccountPerfs(account).inject((created + 1, existing, botTitled, skipped))
                    case None    => fuccess((created, existing, botTitled, skipped :+ s"$account could not be created"))

    action match
      case "update-bot-roster" =>
        val accountRoster = normalizeBotRoster(raw("botAccountRoster"))
        saveSharedBotRoster(accountRoster).map: _ =>
          lila.log("setting").info(s"${me.username} updated EvenChess shared bot account roster count=${BotOperations.BotAccountRoster.fromCsv(accountRoster).size}")
          redirect(s"Bot account roster updated with ${BotOperations.BotAccountRoster.fromCsv(accountRoster).size} account name(s).")

      case "provision-bot-accounts" =>
        val accountRoster = BotOperations.BotAccountRoster.effectiveFromCsv(currentSharedBotRosterCsv)
        val requestedCount = rawInt("botProvisionCount", accountRoster.size)
        val count = requestedCount.max(1).min(accountRoster.size).min(BotOperations.maxSimulationBots)
        provisionRosterAccounts(accountRoster, count).map: (created, existing, botTitled, skipped) =>
          lila
            .log("setting")
            .info(s"${me.username} provisioned EvenChess roster accounts count=$count created=$created existing=$existing botTitled=$botTitled skipped=${skipped.size}")
          val botTitleNote =
            if botTitled > 0 then s" $botTitled existing account(s) have the Lichess BOT title and may render as bots until replaced in the roster."
            else ""
          val skippedNote =
            if skipped.nonEmpty then s" Skipped: ${skipped.take(3).mkString("; ")}${if skipped.size > 3 then " ..." else ""}"
            else ""
          if created == 0 && existing == 0 then redirectFailure(s"No roster accounts were provisioned.$skippedNote")
          else redirect(s"Roster account provisioning complete: $created created, $existing already existed, ${skipped.size} skipped.$botTitleNote$skippedNote")

      case "update-matchmaking" =>
        val enabled = rawBool("matchmakingEnabled")
        val scope = safeScope(raw("matchmakingScope").getOrElse(webSettings.evenChessMatchmakingBotModeScope.get()))
        val timeout = rawInt("matchmakingTimeoutSeconds", webSettings.evenChessMatchmakingBotMatchTimeoutSeconds.get()).max(1).min(3600)
        val accountRoster = normalizeBotRoster(raw("botAccountRoster").orElse(Some(currentSharedBotRosterCsv)))
        for
          _ <- webSettings.evenChessMatchmakingBotModeEnabled.setString(enabled.toString)
          _ <- webSettings.evenChessMatchmakingBotModeScope.setString(scope)
          _ <- webSettings.evenChessMatchmakingBotMatchTimeoutSeconds.setString(timeout.toString)
          _ <- saveSharedBotRoster(accountRoster)
          _ <- if enabled then stopSimulationForExclusiveMode(me.username.value, now) else fuccess(())
        yield
          lila.log("setting").info(s"${me.username} updated EvenChess bot matchmaking mode enabled=$enabled scope=$scope timeout=$timeout roster=${accountRoster.nonEmpty}")
          redirect("Bot matchmaking settings updated.")

      case "start-matchmaking" =>
        for
          _ <- webSettings.evenChessMatchmakingBotModeEnabled.setString("true")
          _ <- saveSharedBotRoster(currentSharedBotRosterCsv)
          _ <- stopSimulationForExclusiveMode(me.username.value, now)
        yield
          lila.log("setting").info(s"${me.username} enabled EvenChess bot matchmaking mode")
          redirect("Bot matchmaking mode enabled. Simulation mode was stopped because only one bot mode can run at a time.")

      case "stop-matchmaking" =>
        webSettings.evenChessMatchmakingBotModeEnabled
          .setString("false")
          .map: _ =>
            lila.log("setting").info(s"${me.username} disabled EvenChess bot matchmaking mode")
            redirect("Bot matchmaking mode disabled.")


      case "update-simulation" | "start-simulation" =>
        val enabled = action == "start-simulation" || rawBool("simulationEnabled")
        val scope = safeScope(raw("simulationScope").getOrElse(webSettings.evenChessBotSimulationScope.get()))
        val persona = BotOperations.BotPersonaMode.fromRaw(raw("simulationPersona").getOrElse(webSettings.evenChessBotSimulationPersona.get())).key
        val botCount = rawInt("simulationBotCount", webSettings.evenChessBotSimulationBotCount.get()).max(0).min(BotOperations.maxSimulationBots)
        val ratingMin = rawInt("simulationRatingMin", webSettings.evenChessBotSimulationRatingMin.get()).max(100).min(5000)
        val ratingMax = rawInt("simulationRatingMax", webSettings.evenChessBotSimulationRatingMax.get()).max(ratingMin).min(5000)
        val levelMin = rawInt("simulationLevelMin", webSettings.evenChessBotSimulationLevelMin.get()).max(0).min(10)
        val levelMax = rawInt("simulationLevelMax", webSettings.evenChessBotSimulationLevelMax.get()).max(levelMin).min(10)
        val timeControlCsv =
          BotOperations.SimulationTimeControlOptions.csvFor(
            BotOperations.SimulationTimeControlOptions.fromCsv(
              form.get("simulationTimeControls").toList.flatten.mkString(",") match
                case "" => webSettings.evenChessBotSimulationTimeControls.get()
                case selected => selected
            )
          )
        val accountRoster = normalizeBotRoster(raw("botAccountRoster").orElse(Some(currentSharedBotRosterCsv)))
        for
          _ <- webSettings.evenChessBotSimulationEnabled.setString(enabled.toString)
          _ <- webSettings.evenChessBotSimulationScope.setString(scope)
          _ <- webSettings.evenChessBotSimulationPersona.setString(persona)
          _ <- webSettings.evenChessBotSimulationBotCount.setString(botCount.toString)
          _ <- webSettings.evenChessBotSimulationRatingMin.setString(ratingMin.toString)
          _ <- webSettings.evenChessBotSimulationRatingMax.setString(ratingMax.toString)
          _ <- webSettings.evenChessBotSimulationLevelMin.setString(levelMin.toString)
          _ <- webSettings.evenChessBotSimulationLevelMax.setString(levelMax.toString)
          _ <- webSettings.evenChessBotSimulationTimeControls.setString(timeControlCsv)
          _ <- saveSharedBotRoster(accountRoster)
          _ <- if enabled then webSettings.evenChessMatchmakingBotModeEnabled.setString("false") else fuccess(())
        yield
          val controls = AdminBackendSettings.BotSimulationControls(
            enabled = enabled,
            scope = scope,
            botCount = botCount,
            ratingMin = ratingMin,
            ratingMax = ratingMax,
            levelMin = levelMin,
            levelMax = levelMax,
            persona = persona,
            timeControls = timeControlCsv,
            accountRoster = accountRoster
          )
          val config = BotOperations.BotSimulationConfig.fromSettings(controls)
          val repository = PlaySearchIntegration.SearchRepositoryRuntime.local
          val wasRunning = BotOperations.BotSimulationRuntime.status.running
          val shouldRun = action == "start-simulation" || (action == "update-simulation" && wasRunning && enabled)
          BotOperations.clearSimulationTickets(repository)
          val runtime =
            if shouldRun then BotOperations.BotSimulationRuntime.start(config, me.username.value, now)
            else if wasRunning && !enabled then BotOperations.BotSimulationRuntime.stop(me.username.value, now)
            else BotOperations.BotSimulationRuntime.status
          if shouldRun then
            val seed = BotOperations.seedSimulation(repository, config, runtime, now)
            BotOperations.BotSimulationRuntime.recordSeed(seed, me.username.value, now)
          lila.log("setting").info(s"${me.username} ${action} EvenChess bot simulation enabled=$enabled scope=$scope count=$botCount")
          redirect(
            if shouldRun then "Bot simulation started and queue filled. Matchmaking bots were turned off because only one bot mode can run at a time."
            else if wasRunning && !enabled then "Bot simulation stopped and settings updated."
            else "Bot simulation settings updated."
          )

      case "stop-simulation" =>
        val repository = PlaySearchIntegration.SearchRepositoryRuntime.local
        val removed = BotOperations.clearSimulationTickets(repository)
        BotOperations.BotSimulationRuntime.stop(me.username.value, now)
        webSettings.evenChessBotSimulationEnabled
          .setString("false")
          .map: _ =>
            lila.log("setting").info(s"${me.username} stopped EvenChess bot simulation and cleared $removed ticket(s)")
            redirect(s"Bot simulation stopped; cleared $removed simulated-player queue entries.")

      case "seed-simulation" =>
        val backend = evenChessBackendSettingsSnapshot
        val config = BotOperations.BotSimulationConfig.fromSettings(backend.botSimulation)
        val runtime =
          if BotOperations.BotSimulationRuntime.status.running then BotOperations.BotSimulationRuntime.status
          else BotOperations.BotSimulationRuntime.start(config, me.username.value, now)
        val seed = BotOperations.seedSimulation(PlaySearchIntegration.SearchRepositoryRuntime.local, config, runtime, now)
        BotOperations.BotSimulationRuntime.recordSeed(seed, me.username.value, now)
        webSettings.evenChessMatchmakingBotModeEnabled.setString("false").map: _ =>
          redirect(s"Bot simulation queue refilled; matchmaking bots are off. ${seed.summary}")

      case _ =>
        fuccess(Redirect(routes.Dev.evenChessBotOpsPanel.url).flashFailure(s"Unknown bot operation: $action"))
  }

  def evenChessEcorOps(action: String) = SecureBody(parse.tolerantFormUrlEncoded)(_.Settings) { ctx ?=> me ?=>
    val form = ctx.body.body
    val now = System.currentTimeMillis
    val webSettings = env.web.settings

    def raw(name: String): Option[String] =
      form.get(name).flatMap(_.headOption).map(_.trim)

    def rawText(name: String): String =
      form.get(name).flatMap(_.headOption).getOrElse("").trim

    def redirectSuccess(message: String) =
      Redirect(routes.Dev.evenChessEcorPanel.url).flashSuccess(message)

    def redirectFailure(message: String) =
      fuccess(Redirect(routes.Dev.evenChessEcorPanel.url).flashFailure(message))

    def currentHistory = webSettings.evenChessEcorSnapshotHistory.get().value

    def persistConfig(
        version: String,
        gapText: String,
        bandText: String,
        reason: String
    ) =
      EvenChessRatingCalibration.EcorTableConfig.fromText(version, gapText, bandText) match
        case Left(error) => redirectFailure(error)
        case Right(config) =>
          val snapshot = EvenChessRatingCalibration.EcorSnapshot(
            timestampMillis = now,
            adminId = me.username.value,
            reason = reason.trim.take(240) match
              case ""    => action
              case value => value,
            version = config.version,
            gapText = config.gapText,
            ratingBandsText = config.ratingBandsText
          )
          val nextHistory = EvenChessRatingCalibration.EcorHistory.append(currentHistory, snapshot)
          for
            _ <- webSettings.evenChessEcorPolicyVersion.setString(config.version)
            _ <- webSettings.evenChessEcorGapOffsets.setString(config.gapText)
            _ <- webSettings.evenChessEcorRatingLevelBands.setString(config.ratingBandsText)
            _ <- webSettings.evenChessEcorSnapshotHistory.setString(nextHistory)
          yield
            EvenChessRatingCalibration.EcorRuntime.activate(config)
            lila.log("setting").info(s"${me.username} updated ECOR table ${config.version} via $action")
            redirectSuccess("ECOR table saved and activated.")

    action match
      case "update-tables" =>
        persistConfig(
          version = raw("ecorPolicyVersion").getOrElse(webSettings.evenChessEcorPolicyVersion.get()),
          gapText = rawText("ecorGapOffsets"),
          bandText = rawText("ecorRatingLevelBands"),
          reason = raw("ecorReason").getOrElse("manual ECOR table update")
        )

      case "update-stockfish-table" =>
        EvenChessRatingCalibration.StockfishAiRatingTableConfig.fromText(rawText("stockfishEquivalentRatingBands")) match
          case Left(error) => redirectFailure(error)
          case Right(config) =>
            for _ <- webSettings.evenChessStockfishEquivalentRatingBands.setString(config.tableText)
            yield
              EvenChessRatingCalibration.StockfishAiRatingRuntime.activate(config)
              lila.log("setting").info(s"${me.username} updated Stockfish equivalent rating table via $action")
              redirectSuccess("Stockfish equivalent rating table saved and activated.")

      case "run-calibration" =>
        evenChessBackendSettingsSnapshot.ecor.config match
          case Left(error) => redirectFailure(error)
          case Right(config) =>
            val samples = EvenChessRatingCalibration.GameHistory.latest()
            val run = EvenChessRatingCalibration.CalibrationEngine.run(samples, config, now)
            EvenChessRatingCalibration.CalibrationRuntime.record(run)
            lila.log("setting").info(s"${me.username} ran ECOR calibration: ${run.summary}")
            fuccess(redirectSuccess(s"ECOR calibration complete: ${run.summary}"))

      case "apply-calibration" =>
        EvenChessRatingCalibration.CalibrationRuntime.lastRun match
          case None => redirectFailure("No ECOR calibration run is available to apply.")
          case Some(run) =>
            persistConfig(
              version = s"${run.currentTable.version}-calibrated-$now",
              gapText = run.calculatedGapText,
              bandText = run.currentTable.ratingBandsText,
              reason = raw("ecorReason").getOrElse("applied latest ECOR calibration")
            )

      case "restore-snapshot" =>
        val timestamp = raw("snapshotTimestamp").flatMap(_.toLongOption)
        timestamp.flatMap(EvenChessRatingCalibration.EcorHistory.find(currentHistory, _)) match
          case None => redirectFailure("Selected ECOR snapshot was not found.")
          case Some(snapshot) =>
            persistConfig(
              version = s"${snapshot.version}-restored-$now",
              gapText = snapshot.gapText,
              bandText = snapshot.ratingBandsText,
              reason = raw("ecorReason").getOrElse(s"restored ECOR snapshot ${snapshot.timestampMillis}")
            )

      case _ =>
        redirectFailure(s"Unknown ECOR operation: $action")
  }

  def cli = Secure(_.Cli) { _ ?=> _ ?=>
    Ok.page:
      views.dev.cli(env.api.cli.form, none)
  }

  def cliPost = SecureBody(_.Cli) { _ ?=> me ?=>
    bindForm(env.api.cli.form)(
      err => BadRequest.page(views.dev.cli(err, "Invalid command".some)),
      command =>
        Ok.async:
          runCommand(command).map: res =>
            views.dev.cli(env.api.cli.form.fill(command), s"$command\n\n$res".some)
    )
  }

  def command = ScopedBody(parse.tolerantText)(Seq(_.Preference.Write)) { ctx ?=> _ ?=>
    isGranted(_.Cli).so:
      runCommand(ctx.body.body).map { Ok(_) }
  }

  def ipTiers = Secure(_.IpTiers) { ctx ?=> _ ?=>
    env.security.ipTiers.form.flatMap: form =>
      Ok.page(views.dev.ipTiers(form))
  }

  def ipTiersPost = SecureBody(_.IpTiers) { ctx ?=> _ ?=>
    Found(env.security.ipTiers.form.map(_.toOption)): form =>
      bindForm(form)(
        err => BadRequest.page(views.dev.ipTiers(Right(err))),
        v => env.security.ipTiers.writeToFile(v).inject(Redirect(routes.Dev.ipTiers).flashSuccess)
      )
  }

  def emailErrorPost = SecuredScopedBody(_.SetEmail)():
    if env.web.emailError.setFromReq().isDefined then NoContent else BadRequest

  def emailErrorGet = Open: ctx ?=>
    ctx.isAnon
      .so(lila.security.EmailConfirm.cookie.get(ctx.req))
      .flatMap(u => env.web.emailError.get(u.email))
      .fold(NoContent)(Ok(_))

  private def runCommand(command: String)(using Me): Fu[String] =
    for
      _ <- env.mod.logApi.cli(command)
      res <- env.api.cli.run(command.split(" ").toList)
    yield res

  private def evenChessBackendSettingsSnapshot: AdminBackendSettings.BackendSettings =
    import AdminBackendSettings.*

    val webSettings = env.web.settings
    val snapshot = BackendSettings(
      openAi = OpenAiBackend(
        provider = webSettings.evenChessOpenAiProvider.get(),
        model = webSettings.evenChessOpenAiModel.get(),
        keyStatus = ProviderKeyStatus(
          configured = webSettings.evenChessOpenAiKeyConfigured.get(),
          rotated = webSettings.evenChessOpenAiKeyRotated.get()
        )
      ),
      tts = TtsBackend(
        provider = webSettings.evenChessTtsProvider.get(),
        keyStatus = ProviderKeyStatus(
          configured = webSettings.evenChessTtsKeyConfigured.get(),
          rotated = webSettings.evenChessTtsKeyRotated.get()
        )
      ),
      stockfish = StockfishBackend(
        profile = webSettings.evenChessStockfishProfile.get(),
        maxDepth = webSettings.evenChessStockfishMaxDepth.get(),
        maxMultipv = webSettings.evenChessStockfishMaxMultipv.get(),
        engineJobsPerMinute = webSettings.evenChessStockfishEngineJobsPerMinute.get(),
        equivalentRatingBandsText = webSettings.evenChessStockfishEquivalentRatingBands.get().value
      ),
      surfaces = SurfaceAiEnablement(
        live = webSettings.evenChessLiveAiEnabled.get(),
        study = webSettings.evenChessStudyAiEnabled.get(),
        opening = webSettings.evenChessOpeningAiEnabled.get(),
        analysis = webSettings.evenChessAnalysisAiEnabled.get()
      ),
      features = FeatureFlags(
        overlays = webSettings.evenChessOverlaysEnabled.get(),
        coachingCards = webSettings.evenChessCoachingCardsEnabled.get(),
        offsetCount = webSettings.evenChessOffsetCountEnabled.get()
      ),
      monetisation = MonetisationSwitches(
        tokens = webSettings.evenChessTokensEnabled.get(),
        rewardedAds = webSettings.evenChessRewardedAdsEnabled.get(),
        payments = webSettings.evenChessPaymentsEnabled.get(),
        freeMatchTokenWindow = FreeMatchTokenWindow(
          enabled = webSettings.evenChessFreeMatchTokensEnabled.get(),
          startsAt = webSettings.evenChessFreeMatchTokensStartsAt.get(),
          endsAt = webSettings.evenChessFreeMatchTokensEndsAt.get()
        )
      ),
      matchmaking = MatchmakingControls(
        botModeEnabled = webSettings.evenChessMatchmakingBotModeEnabled.get(),
        botModeScope = webSettings.evenChessMatchmakingBotModeScope.get(),
        botMatchTimeoutSeconds = webSettings.evenChessMatchmakingBotMatchTimeoutSeconds.get(),
        botAccountRoster = webSettings.evenChessMatchmakingBotAccountRoster.get()
      ),
      botSimulation = BotSimulationControls(
        enabled = webSettings.evenChessBotSimulationEnabled.get(),
        scope = webSettings.evenChessBotSimulationScope.get(),
        botCount = webSettings.evenChessBotSimulationBotCount.get(),
        ratingMin = webSettings.evenChessBotSimulationRatingMin.get(),
        ratingMax = webSettings.evenChessBotSimulationRatingMax.get(),
        levelMin = webSettings.evenChessBotSimulationLevelMin.get(),
        levelMax = webSettings.evenChessBotSimulationLevelMax.get(),
        persona = webSettings.evenChessBotSimulationPersona.get(),
        timeControls = webSettings.evenChessBotSimulationTimeControls.get(),
        accountRoster = webSettings.evenChessBotSimulationAccountRoster.get()
      ),
      ecor = EcorControls(
        policyVersion = webSettings.evenChessEcorPolicyVersion.get(),
        gapOffsetsText = webSettings.evenChessEcorGapOffsets.get().value,
        ratingLevelBandsText = webSettings.evenChessEcorRatingLevelBands.get().value,
        snapshotHistoryText = webSettings.evenChessEcorSnapshotHistory.get().value
      ),
      campaign = CampaignControls(
        variant = webSettings.evenChessCampaignVariant.get(),
        killSwitch = webSettings.evenChessCampaignKillSwitch.get(),
        paidAcquisitionPaused = webSettings.evenChessPaidAcquisitionPaused.get()
      ),
      limits = SafetyLimits(
        aiDailyCostLimitCents = webSettings.evenChessAiDailyCostLimitCents.get(),
        aiRateLimitPerMinute = webSettings.evenChessAiRateLimitPerMinute.get(),
        ttsDailyCostLimitCents = webSettings.evenChessTtsDailyCostLimitCents.get(),
        ttsRateLimitPerMinute = webSettings.evenChessTtsRateLimitPerMinute.get(),
        auditRetentionDays = webSettings.evenChessAuditRetentionDays.get()
      ),
      incident = IncidentControls(
        globalPause = webSettings.evenChessIncidentGlobalPause.get(),
        liveCoachingPaused = webSettings.evenChessIncidentLiveCoachingPaused.get(),
        aiPaused = webSettings.evenChessIncidentAiPaused.get(),
        ttsPaused = webSettings.evenChessIncidentTtsPaused.get(),
        enginePaused = webSettings.evenChessIncidentEnginePaused.get(),
        tokenAdsPaused = webSettings.evenChessIncidentTokenAdsPaused.get(),
        noRate = webSettings.evenChessIncidentNoRate.get(),
        publicNotice = webSettings.evenChessIncidentPublicNotice.get()
      )
    )
    snapshot.ecor.config.foreach(config => EvenChessRatingCalibration.EcorRuntime.activate(config))
    snapshot.stockfish.equivalentRatingTable.foreach(config => EvenChessRatingCalibration.StockfishAiRatingRuntime.activate(config))
    snapshot

  private lazy val settingsList = List[(String, List[lila.memo.SettingStore[?]])](
    "Moderation" -> List(
      env.security.ugcArmedSetting,
      env.security.spamKeywordsSetting,
      env.irwin.irwinApi.thresholds,
      env.irwin.kaladinApi.thresholds,
      env.report.scoreThresholdsSetting,
      env.report.discordScoreThresholdSetting
    ),
    "Cheat" -> List(
      env.round.selfReportEndGame,
      env.round.selfReportMarkUser,
      env.bot.boardReport.domainSetting
    ),
    "Security" -> List(
      env.oAuth.originBlocklistSetting,
      env.security.proxy2faSetting,
      env.security.lichobileLogin
    ),
    "Mailing" -> List(
      env.mailer.mailerSecondaryPermilleSetting,
      env.mailer.canSendEmailsSetting
    ),
    "Streamer" -> List(
      env.streamer.homepageMaxSetting,
      env.streamer.alwaysFeaturedSetting
    ),
    "Permissions" -> List(
      env.web.settings.noDelaySecret,
      env.web.settings.prizeTournamentMakers
    ),
    "Limits" -> List(
      env.web.settings.apiTimeline,
      env.web.settings.apiExplorerGamesPerSecond,
      env.recap.parallelismSetting,
      env.fishnet.openingBookDepth
    ),
    "Broadcast" -> List(
      env.relay.proxyDomainRegex,
      env.relay.proxyHostPort,
      env.relay.proxyCredentials
    ),
    "Tutor" -> List(
      env.tutor.nbAnalysisSetting,
      env.tutor.parallelismSetting
    ),
    "Automod" -> List(
      env.report.automod.imageModelSetting,
      env.report.automod.imagePromptSetting,
      env.report.api.commsModelSetting,
      env.report.api.commsPromptSetting,
      env.ublog.ublogAutomod.modelSetting,
      env.ublog.ublogAutomod.promptSetting
    ),
    "Mobile" -> List(
      env.web.lichobileAnnounceApi.lichobileUpgrade
    ),
    "EvenChess" -> env.web.settings.evenChessBackendSettings,
    "Config" -> List(
      env.plan.donationGoalSetting,
      env.tournament.reloadEndpointSetting
    )
  )
