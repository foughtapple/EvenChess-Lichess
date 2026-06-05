package lila.web
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.common.RawHtml.nl2br
import lila.evenchess.AdminOpsDashboard
import lila.evenchess.BotOperations
import lila.evenchess.EvenChessRatingCalibration

final class DevUi(helpers: Helpers)(modMenu: String => Context ?=> Frag):
  import helpers.*

  def evenChessOps(model: AdminOpsDashboard.AdminOpsDashboardModel)(using Context) =
    val title = "EvenChess operations"
    Page(title).css("mod.misc"):
      main(cls := "page-menu")(
        modMenu("evenchess-ops"),
        div(id := "evenchess-ops", cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          p(
            "Dashboard for EvenChess-specific runtime health, bot operations, active versions, audit search, incident controls, and paid-launch readiness."
          ),
          div(style := "display:flex;gap:10px;flex-wrap:wrap;margin-bottom:18px")(
            statusBadge(if model.valid then "Dashboard contract valid" else "Dashboard contract incomplete", if model.valid then AdminOpsDashboard.HealthSeverity.Healthy else AdminOpsDashboard.HealthSeverity.Warning),
            statusBadge(if model.secretSafe then "Secrets safe" else "Secret exposure risk", if model.secretSafe then AdminOpsDashboard.HealthSeverity.Healthy else AdminOpsDashboard.HealthSeverity.Critical),
            statusBadge(if model.antiCheatInternalsRedacted then "Anti-cheat internals redacted" else "Anti-cheat exposure risk", if model.antiCheatInternalsRedacted then AdminOpsDashboard.HealthSeverity.Healthy else AdminOpsDashboard.HealthSeverity.Critical),
            statusBadge(if model.incidentControlState.active then "Incident controls active" else "No incident pause active", if model.incidentControlState.active then AdminOpsDashboard.HealthSeverity.Paused else AdminOpsDashboard.HealthSeverity.Healthy)
          ),
          botOpsSection(model.botOperations),
          ecorSection(model.ecor, model.stockfishAiTable),
          h2("Active versions"),
          table(cls := "slist slist-pad")(
            thead(tr(th("Area"), th("Value"), th("Source"))),
            tbody(
              model.activeVersions.map: row =>
                tr(td(row.label), td(code(row.value)), td(row.source))
            )
          ),
          h2("Runtime dashboards"),
          div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:14px;margin-bottom:24px")(
            model.panels.map(panelCard)
          ),
          h2("Operator actions"),
          p(
            "Actions shown here must be executed through audited backend settings or future ledger-backed tooling. Fairness-affecting actions require a versioned policy/config path."
          ),
          table(cls := "slist slist-pad")(
            thead(tr(th("Action"), th("Setting"), th("Fairness"), th("Audit"), th("Rollback"), th("Description"))),
            tbody(
              model.operatorActions.map: action =>
                tr(
                  td(action.label),
                  td(action.settingId.fold[Frag]("-")(id => code(id))),
                  td(if action.fairnessAffecting then "versioned policy required" else "non-fairness"),
                  td(if action.requiresAuditTrail then "required" else "not required"),
                  td(if action.rollbackable then "yes" else "no"),
                  td(action.description)
                )
            )
          ),
          p(
            a(cls := "button button-empty", href := routes.Dev.settings)("Open audited backend settings")
          ),
          h2("Audit ledger search"),
          st.form(cls := "search", action := routes.Dev.evenChessOps, method := "GET")(
            input(name := "q", placeholder := "game, player, audit id, policy, category", value := model.auditQuery.value),
            submitButton(cls := "button button-empty")("Search")
          ),
          if model.auditQuery.active then
            if model.auditResults.nonEmpty then
              table(cls := "slist slist-pad")(
                thead(tr(th("Audit"), th("Game"), th("Player"), th("Category"), th("Policy"), th("Summary"))),
                tbody(
                  model.auditResults.map: result =>
                    tr(
                      td(code(result.auditId)),
                      td(result.gameId),
                      td(result.playerId),
                      td(result.category),
                      td(result.policyVersion),
                      td(result.summary)
                    )
                )
              )
            else p("No EvenChess audit rows matched that query.")
          else p("Enter a query to inspect sanitized EvenChess audit summaries."),
          h2("Incident records"),
          table(cls := "slist slist-pad")(
            thead(tr(th("Incident"), th("Type"), th("Status"), th("Actions"), th("Audit"), th("Public notice"))),
            tbody(
              model.incidentRecords.map: incident =>
                tr(
                  td(incident.incidentId),
                  td(incident.incidentType.toString),
                  td(incident.status.toString),
                  td(incident.actionsTaken.toList.map(_.toString).sorted.mkString(", ")),
                  td(code(incident.auditId)),
                  td(incident.publicNotice.fold("-")(_.message))
                )
            )
          ),
          h2("Paid launch readiness"),
          table(cls := "slist slist-pad")(
            thead(tr(th("Check"), th("Verified"), th("Decision"))),
            tbody(
              model.paidLaunchRows.map: row =>
                tr(
                  td(row.label),
                  td(row.verified.toString),
                  td(row.explicitlyUnavailableDecision.getOrElse(if row.verified then "verified" else "missing"))
                )
            )
          ),
          h2("Rollbackable feature flags"),
          table(cls := "slist slist-pad")(
            thead(tr(th("Flag"), th("Enabled"), th("Version"), th("Rollback"), th("Owner"), th("Audit"))),
            tbody(
              model.featureFlags.map: flag =>
                tr(
                  td(code(flag.key)),
                  td(flag.enabled.toString),
                  td(flag.configVersion),
                  td(flag.rollbackVersion.getOrElse("-")),
                  td(flag.owner),
                  td(code(flag.auditId))
                )
            )
          )
        )
      )

  def evenChessBotOpsPanel(model: AdminOpsDashboard.AdminOpsDashboardModel)(using Context) =
    val title = "EvenChess bot operations"
    Page(title).css("mod.misc"):
      main(cls := "page-menu")(
        modMenu("evenchess-ops"),
        div(id := "evenchess-bot-ops", cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          p("Admin-only controls for low-pool matchmaking fallback and simulated player populations."),
          botOpsSection(model.botOperations)
        )
      )

  def evenChessEcorPanel(model: AdminOpsDashboard.AdminOpsDashboardModel)(using Context) =
    val title = "ECE rating calibration"
    Page(title).css("mod.misc"):
      main(cls := "page-menu")(
        modMenu("evenchess-ops"),
        div(id := "evenchess-ecor", cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          p("Admin-only controls for the EvenChess offset ratings table, rating-to-level table, calibration runs, and rollback snapshots."),
          ecorSection(model.ecor, model.stockfishAiTable)
        )
      )

  def settings(settings: List[(String, List[lila.memo.SettingStore[?]])])(using Context) =
    val title = "Settings"
    Page(title).css("mod.misc"):
      main(cls := "page-menu")(
        modMenu("setting"),
        div(id := "settings", cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          settings.map: (group, list) =>
            form3.fieldset(group, false.some):
              div: // necessary for styling
                div: // necessary for styling
                  list.map: s =>
                    postForm(action := routes.Dev.settingsPost(s.id))(
                      label(`for` := "v")(s.text | s.id),
                      s.form.value match
                        case Some(v: Boolean) => form3.nativeCheckbox(s.id, "v", v)
                        case Some(v: lila.core.data.Text) => textarea(name := "v")(v.value)
                        case v => input(name := "v", value := v.map(_.toString))
                      ,
                      submitButton(cls := "button button-empty", dataIcon := Icon.Checkmark)
                    )
        )
      )

  def ipTiers(form: Either[String, Form[?]])(using Context) =
    val title = "IP limit tiers"
    Page(title)
      .css("mod.misc")
      .css("bits.form3"):
        main(cls := "page-menu")(
          modMenu("ip-tiers"),
          div(id := "ip-tiers", cls := "page-menu__content box box-pad")(
            h1(cls := "box__top")(title),
            p(
              "Upgrade rate limits for specific IP addresses.",
              br,
              "Only necessary when more than 20 devices connect from the same IP at the same time.",
              br,
              "This requires a service to copy the lila file to the nginx server and reload nginx."
            ),
            p(
              "Format: ",
              br,
              code("{IP} {tier}; # contact info"),
              br,
              nl2br("""
  Tier 1: normal limits (default, up to 30 players)
  Tier 2: higher limits (well enough for schools and hotels)
  Tier 3: much higher limits (only for official bots like maia)
  """)
            ),
            standardFlash,
            postForm(action := routes.Dev.ipTiersPost, cls := "form3")(
              form match
                case Left(err) => p(cls := "error")(err)
                case Right(form) =>
                  val field = form("list")
                  frag(
                    div(cls := "form-group")(
                      form3.textarea(field)(spellcheck := "false"),
                      field.errors.map: err =>
                        p(cls := "error")(nl2br(err.message))
                    ),
                    br,
                    form3.submit(frag("Save and reload nginx"))
                  )
            )
          )
        )

  def cli(form: Form[?], res: Option[String])(using Context) =
    val title = "Command Line Interface"
    Page(title)
      .css("mod.misc")
      .css("bits.form3"):
        main(cls := "page-menu")(
          modMenu("cli"),
          div(id := "dev-cli", cls := "page-menu__content box box-pad")(
            h1(cls := "box__top")(title),
            p(
              "Run arbitrary lila commands.",
              br,
              "Only use if you know exactly what you're doing."
            ),
            res.map { pre(_) },
            postForm(action := routes.Dev.cliPost)(
              form3.input(form("command"))(autofocus),
              br,
              form3.submit(frag("Submit"))
            ),
            hr,
            postForm(action := routes.Dev.cliPost)(
              p("Same thing but with a textarea for multiline commands:"),
              form3.textarea(form("command"))(style := "height:8em"),
              br,
              form3.submit(frag("Submit"))
            ),
            h2("Command examples:"),
            pre(cliExamples)
          )
        )

  private val cliExamples = """uptime
announce 10 minutes Lichess will restart!
announce cancel
change asset version
fishnet client create {username}
msg multi {sender} {recipient1,recipient2} {message}
team members add {teamId} {username1,username2,username3}
notify url users {username1,username2,username3} {url} {link title} | {link description}
notify url titled {url} {link title} | {link description}
notify url titled-arena {url} {link title} | {link description}
patron lifetime {username}
patron gift-months {username} 3
patron remove {username}
patron set-months {username} {months}
tournament feature {id}
tournament unfeature {id}
eval-cache drop standard 8/8/1k6/8/2K5/1P6/8/8 w - - 0 1
disposable test msumain.edu.ph
disposable reload msumain.edu.ph
test-email {primary | secondary} {email}
video sheet
puzzle issue {id} {longer-win | ambiguous | ...}
cache clear security.session.info
fide player sync
fide player rip 2026961 2025
fide player delete 2026961
"""

  private def botOpsSection(state: BotOperations.BotOpsAdminState) =
    val runtime = state.simulation.runtime
    val config = state.simulation.config
    val sharedRosterCsv = state.matchmaking.accountRoster
    val sharedRosterAccounts = BotOperations.BotAccountRoster.fromCsv(sharedRosterCsv)
    val sharedRosterTextareaValue = sharedRosterAccounts.grouped(4).map(_.mkString(", ")).mkString("\n")
    div(id := "bot-operations", style := "margin:0 0 24px")(
      h2("Bot operations"),
      p(style := "color:var(--c-font-dim)")(
        "Matchmaking fallback and simulation use the same bot-account roster. Only one bot mode can run at a time: starting one automatically stops the other."
      ),
      div(style := "display:flex;gap:10px;flex-wrap:wrap;margin-bottom:14px")(
        statusBadge(if state.matchmaking.enabled then "Matchmaking bots on" else "Matchmaking bots off", if state.matchmaking.enabled then AdminOpsDashboard.HealthSeverity.Warning else AdminOpsDashboard.HealthSeverity.Healthy),
        statusBadge(
          if runtime.running then "Simulation running" else "Simulation stopped",
          if runtime.running then AdminOpsDashboard.HealthSeverity.Warning else AdminOpsDashboard.HealthSeverity.Healthy,
          "Whether simulated players are currently being kept in the EvenChess search queue."
        ),
        statusBadge(
          s"${state.simulation.activeTickets} simulated player(s) waiting",
          AdminOpsDashboard.HealthSeverity.Healthy,
          "Current roster-backed simulated-player searches waiting in the queue."
        ),
        statusBadge(
          s"${state.simulation.potentialBotVsBotContracts} possible sim-vs-sim match(es)",
          AdminOpsDashboard.HealthSeverity.Healthy,
          "Potential matches that could be formed between two simulated players."
        ),
        statusBadge(
          s"${sharedRosterAccounts.size} roster account name(s)",
          if sharedRosterAccounts.nonEmpty then AdminOpsDashboard.HealthSeverity.Healthy else AdminOpsDashboard.HealthSeverity.Warning,
          "Shared bot-account names used by both matchmaking fallback and simulation."
        )
      ),
      div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page);margin-bottom:14px")(
        h3(style := "margin-top:0")("Shared bot account roster"),
        p(style := "color:var(--c-font-dim)")(
          s"Default generated roster: ${BotOperations.BotAccountRoster.generatedDefaultCount} names from ${BotOperations.BotAccountRoster.generatedDefault.headOption.getOrElse("ecbot0001")} to ${BotOperations.BotAccountRoster.generatedDefault.lastOption.getOrElse("ecbot1000")}. These names must exist as local user accounts before bot games can render as human-style rounds."
        ),
        postForm(action := routes.Dev.evenChessBotOps("update-bot-roster"))(
          formRow(
            "Account names",
            textarea(
              name := "botAccountRoster",
              rows := 12,
              style := "width:100%;font-family:monospace;min-height:14rem",
              placeholder := "ecbot0001, ecbot0002, ecbot0003\nor paste one username per line",
              title := "Paste local bot account usernames. Commas, spaces, and new lines are accepted."
            )(sharedRosterTextareaValue),
            "Bulk roster shared by matchmaking fallback and simulation. Leave blank and save to restore the generated 1000-name roster."
          ),
          submitButton(cls := "button", dataIcon := Icon.Checkmark)("Save shared roster")
        ),
        postForm(action := routes.Dev.evenChessBotOps("provision-bot-accounts"))(
          div(style := "margin-top:14px;border-top:1px solid var(--border);padding-top:12px")(
            formRow(
              "Provision/check accounts",
              input(
                tpe := "number",
                name := "botProvisionCount",
                value := sharedRosterAccounts.size.toString,
                attr("min") := "1",
                attr("max") := BotOperations.maxSimulationBots.toString
              ),
              "Creates missing roster usernames as normal local/staging accounts with private generated passwords, and reports disabled or BOT-titled accounts. Required before human-style bot rounds can be created."
            ),
            submitButton(
              cls := "button button-empty",
              dataIcon := Icon.Checkmark,
              title := "Creates any missing local accounts from the saved shared roster. Existing accounts are not changed."
            )("Create/check roster accounts")
          )
        )
      ),
      div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;margin-bottom:18px")(
        div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page)")(
          h3(style := "margin-top:0")("Matchmaking fallback"),
          p(style := "color:var(--c-font-dim)")("After the configured wait, a human search can be filled by a same-pool bot opponent."),
          postForm(action := routes.Dev.evenChessBotOps("update-matchmaking"))(
            div(cls := "form-group")(
              label(`for` := "matchmaking-enabled")("Enabled"),
              form3.nativeCheckbox("matchmaking-enabled", "matchmakingEnabled", state.matchmaking.enabled)
            ),
            formRow("Scope", scopeSelect("matchmakingScope", state.matchmaking.scope)),
            formRow(
              "Timeout seconds",
              input(tpe := "number", name := "matchmakingTimeoutSeconds", value := state.matchmaking.timeoutSeconds.toString, attr("min") := "1", attr("max") := "3600")
            ),
            submitButton(cls := "button button-empty", dataIcon := Icon.Checkmark)("Save matchmaking")
          ),
          div(style := "display:flex;gap:8px;flex-wrap:wrap;margin-top:10px")(
            postForm(action := routes.Dev.evenChessBotOps("start-matchmaking"))(
              submitButton(cls := "button", dataIcon := Icon.Checkmark)("Start matchmaking bots")
            ),
            postForm(action := routes.Dev.evenChessBotOps("stop-matchmaking"))(
              submitButton(cls := "button button-empty", dataIcon := Icon.X)("Stop matchmaking bots")
            )
          ),
          p(style := "margin-bottom:0;color:var(--c-font-dim)")(state.matchmaking.disclosure)
        ),
        div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page)")(
          h3(style := "margin-top:0")("Simulation bots"),
          p(style := "color:var(--c-font-dim)")(
            "Creates roster-backed simulated players that enter the normal EvenChess search pool for local/staging stress tests. Start or update applies the settings below and fills the queue to the target count."
          ),
          simulationForm(config.copy(enabled = true), "start-simulation", if runtime.running then "Update running simulation" else "Start simulation"),
          div(style := "display:flex;gap:8px;flex-wrap:wrap;margin-top:10px")(
            if runtime.running then
              postForm(action := routes.Dev.evenChessBotOps("seed-simulation"))(
                submitButton(
                  cls := "button button-empty",
                  dataIcon := Icon.Checkmark,
                  title := "Immediately tops up the simulated-player queue to the configured bot count. Use this if the displayed waiting count looks lower than expected."
                )("Refill queue now")
              )
            else frag(),
            postForm(action := routes.Dev.evenChessBotOps("stop-simulation"))(
              submitButton(
                cls := "button button-empty",
                dataIcon := Icon.X,
                title := "Stops simulation mode and removes only simulated-player queue entries. Human searches are left alone."
              )("Stop simulation")
            )
          )
        )
      ),
      table(cls := "slist slist-pad")(
        thead(tr(th("Area"), th("Value"))),
        tbody(
          monitorRow("Simulation runtime", if runtime.running then "running" else "stopped", "Shows whether simulated players are currently maintained in the search queue."),
          monitorRow("Run version", runtime.revision.toString, "Increments each time simulation starts so old simulated-player entries can be replaced cleanly."),
          monitorRow("Simulated players waiting", state.simulation.activeTickets.toString, "Number of roster-backed simulated-player searches currently waiting to be matched."),
          monitorRow("Possible sim-vs-sim matches", state.simulation.potentialBotVsBotContracts.toString, "Number of possible matches among simulated players in the current queue."),
          monitorRow("Uptime", state.simulation.uptimeMillis.fold("-")(ms => s"${ms / 1000}s"), "How long the current simulation run has been active."),
          monitorRow("Queue entries added total", runtime.seededTicketsTotal.toString, "Total simulated-player searches added during this app session."),
          monitorRow("Last action", simulationActionLabel(runtime.lastAction), "Most recent simulation control action recorded by the server."),
          monitorRow("Last admin", runtime.lastAdminId, "Admin account that last changed simulation state."),
          monitorRow("Last queue refill", runtime.lastSeedSummary, "Plain-language summary of the most recent queue fill/update.")
        )
      )
    )

  private def simulationForm(
      config: BotOperations.BotSimulationConfig,
      actionKey: String,
      buttonLabel: String
  ) =
    postForm(action := routes.Dev.evenChessBotOps(actionKey))(
      input(tpe := "hidden", name := "simulationEnabled", value := "true"),
      formRow(
        "Game types",
        scopeSelect("simulationScope", config.scopeLabel),
        "Choose which EvenChess search pools the simulated players join. Rated and casual means both pools are filled."
      ),
      formRow(
        "Bot style",
        personaSelect("simulationPersona", config.persona.key),
        "Controls move timing style. Human-like waits more naturally; Fast is useful for load tests; Mixed uses both."
      ),
      formRow(
        "Time controls",
        timeControlCheckboxes(config),
        "Tick the time-control families simulated players are allowed to search. Human searches only match same-pool simulated players."
      ),
      formRow(
        "Simulated players",
        input(tpe := "number", name := "simulationBotCount", value := config.botCount.toString, attr("min") := "0", attr("max") := BotOperations.maxSimulationBots.toString),
        s"Target number of simulated-player searches to keep in the queue. With a bot-account roster, this is capped by the roster size. Maximum ${BotOperations.maxSimulationBots}."
      ),
      formRow(
        "Lowest rating",
        input(tpe := "number", name := "simulationRatingMin", value := config.ratingMin.toString, attr("min") := "100", attr("max") := "5000"),
        "Lowest target ECR/rating assigned to generated simulated players."
      ),
      formRow(
        "Highest rating",
        input(tpe := "number", name := "simulationRatingMax", value := config.ratingMax.toString, attr("min") := "100", attr("max") := "5000"),
        "Highest target ECR/rating assigned to generated simulated players."
      ),
      formRow(
        "Lowest Set Level",
        input(tpe := "number", name := "simulationLevelMin", value := config.levelMin.value.toString, attr("min") := "0", attr("max") := "10"),
        "Lowest EvenChess Set Level that simulated players may request."
      ),
      formRow(
        "Highest Set Level",
        input(tpe := "number", name := "simulationLevelMax", value := config.levelMax.value.toString, attr("min") := "0", attr("max") := "10"),
        "Highest EvenChess Set Level that simulated players may request."
      ),
      submitButton(
        cls := "button",
        dataIcon := Icon.Checkmark,
        title := "Saves these settings, starts simulation mode if needed, and fills the queue to the configured simulated-player count."
      )(buttonLabel)
    )

  private def timeControlCheckboxes(config: BotOperations.BotSimulationConfig) =
    val selected = config.timeControls.map(_.key).toSet
    div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:6px")(
      BotOperations.SimulationTimeControlOptions.all.map: option =>
        label(
          style := "display:flex;align-items:center;gap:6px",
          title := option.help
        )(
          input(
            tpe := "checkbox",
            name := "simulationTimeControls",
            value := option.key,
            if selected.contains(option.key) then checked else frag()
          ),
          option.label
        )
    )

  private def ecorSection(
      state: EvenChessRatingCalibration.EcorAdminState,
      stockfishAiTable: EvenChessRatingCalibration.StockfishAiRatingTableConfig
  ) =
    val config = state.config
    div(id := "ecor-calibration", style := "margin:0 0 24px")(
      h2("ECE rating calibration"),
      p(style := "color:var(--c-font-dim)")(
        "ECOR stores one rating-point value per adjacent Set Level gap. Cumulative sums produce each level's effective rating offset for matchmaking and rating settlement review."
      ),
      div(style := "display:flex;gap:10px;flex-wrap:wrap;margin-bottom:14px")(
        statusBadge(s"ECOR ${config.version}", if state.parseError.isDefined then AdminOpsDashboard.HealthSeverity.Warning else AdminOpsDashboard.HealthSeverity.Healthy),
        statusBadge(s"${state.storedSampleCount} stored game sample(s)", AdminOpsDashboard.HealthSeverity.Healthy),
        statusBadge(
          state.latestCalibration.fold("No calibration run yet")(run => s"Last run: ${run.informativeSampleCount} informative"),
          state.latestCalibration.fold(AdminOpsDashboard.HealthSeverity.Warning)(_ => AdminOpsDashboard.HealthSeverity.Healthy)
        )
      ),
      state.parseError.map(error => p(cls := "error")(error)),
      div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;margin-bottom:18px")(
        div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page)")(
          h3(style := "margin-top:0")("Active ECOR table"),
          table(cls := "slist slist-pad")(
            thead(tr(th("Gap"), th("Gap value"), th("Cumulative offset"))),
            tbody(
              config.orderedGaps.map: gap =>
                tr(
                  td(gap.key),
                  td(gap.ratingPoints.toString),
                  td(config.offsetValueForLevel(gap.to).toString)
                )
            )
          )
        ),
        div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page)")(
          h3(style := "margin-top:0")("Equivalent rating to level"),
          table(cls := "slist slist-pad")(
            thead(tr(th("Rating band"), th("Base Set Level"))),
            tbody(
              config.orderedBands.map: band =>
                tr(td(band.label), td(s"L${band.level.value}"))
            )
          )
        ),
        div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page)")(
          h3(style := "margin-top:0")("Stockfish AI equivalent rating"),
          table(cls := "slist slist-pad")(
            thead(tr(th("Lichess AI level"), th("Equivalent rating band"))),
            tbody(
              stockfishAiTable.orderedBands.map: band =>
                tr(td(s"SF${band.level}"), td(band.label))
            )
          )
        )
      ),
      latestCalibrationSection(state.latestCalibration),
      div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:14px;margin-bottom:18px")(
        div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page)")(
          h3(style := "margin-top:0")("Adjust ECOR tables"),
          postForm(action := routes.Dev.evenChessEcorOps("update-tables"))(
            formRow("Policy version", input(name := "ecorPolicyVersion", value := config.version)),
            formRow(
              "ECOR gap offsets",
              textarea(name := "ecorGapOffsets", attr("rows") := "11", spellcheck := "false")(config.gapText)
            ),
            formRow(
              "Rating-to-level bands",
              textarea(name := "ecorRatingLevelBands", attr("rows") := "12", spellcheck := "false")(config.ratingBandsText)
            ),
            formRow("Change reason", input(name := "ecorReason", placeholder := "required for rollback context")),
            submitButton(cls := "button button-empty", dataIcon := Icon.Checkmark)("Save and activate ECOR")
          )
        ),
        div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page)")(
          h3(style := "margin-top:0")("Adjust Stockfish AI rating bands"),
          postForm(action := routes.Dev.evenChessEcorOps("update-stockfish-table"))(
            formRow(
              "Stockfish equivalent bands",
              textarea(name := "stockfishEquivalentRatingBands", attr("rows") := "9", spellcheck := "false")(stockfishAiTable.tableText)
            ),
            submitButton(cls := "button button-empty", dataIcon := Icon.Checkmark)("Save Stockfish table")
          )
        ),
        div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page)")(
          h3(style := "margin-top:0")("Calibration actions"),
          p(style := "color:var(--c-font-dim)")("Runs against the latest stored game-result samples and shows calculated gap values for admin review."),
          div(style := "display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px")(
            postForm(action := routes.Dev.evenChessEcorOps("run-calibration"))(
              submitButton(cls := "button", dataIcon := Icon.Checkmark)("Run calibration")
            ),
            postForm(action := routes.Dev.evenChessEcorOps("apply-calibration"))(
              input(tpe := "hidden", name := "ecorReason", value := "applied latest ECOR calibration"),
              submitButton(cls := "button button-empty", dataIcon := Icon.Checkmark)("Apply latest calculated gaps")
            )
          ),
          if state.history.nonEmpty then
            postForm(action := routes.Dev.evenChessEcorOps("restore-snapshot"))(
              formRow(
                "Restore snapshot",
                st.select(name := "snapshotTimestamp")(
                  state.history.map: snapshot =>
                    st.option(value := snapshot.timestampMillis.toString)(snapshot.label)
                )
              ),
              formRow("Restore reason", input(name := "ecorReason", placeholder := "why this snapshot is being restored")),
              submitButton(cls := "button button-empty", dataIcon := Icon.X)("Restore selected snapshot")
            )
          else p("No ECOR snapshots have been recorded yet.")
        )
      )
    )

  private def latestCalibrationSection(run: Option[EvenChessRatingCalibration.CalibrationRun]) =
    run.fold[Frag](frag()) { calibration =>
      div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page);margin-bottom:18px")(
        h3(style := "margin-top:0")("Latest calculated ECOR values"),
        p(style := "color:var(--c-font-dim)")(calibration.summary),
        table(cls := "slist slist-pad")(
          thead(tr(th("Gap"), th("Current"), th("Calculated"), th("Delta"), th("Support"))),
          tbody(
            calibration.estimates.map: estimate =>
              tr(
                td(estimate.gap.key),
                td(estimate.currentRatingPoints.toString),
                td(estimate.calculatedRatingPoints.toString),
                td(if estimate.delta > 0 then s"+${estimate.delta}" else estimate.delta.toString),
                td(estimate.supportSamples.toString)
              )
          )
        )
      )
    }

  private def formRow(labelText: String, control: Frag, helpText: String = "") =
    div(cls := "form-group")(
      label(title := helpText)(
        labelText,
        if helpText.nonEmpty then span(style := "margin-left:.35em;color:var(--c-link);cursor:help", title := helpText)("?") else frag()
      ),
      div(title := helpText)(control),
      if helpText.nonEmpty then
        small(style := "display:block;margin-top:3px;color:var(--c-font-dim)", title := helpText)(helpText)
      else frag()
    )

  private def monitorRow(labelText: String, valueText: String, helpText: String) =
    tr(title := helpText)(
      td(labelText),
      td(valueText)
    )

  private def simulationActionLabel(action: String): String =
    action match
      case "idle"                             => "Idle"
      case "simulation_started"               => "Simulation started"
      case "simulation_config_saved_disabled" => "Settings saved while stopped"
      case "simulation_stopped"               => "Simulation stopped"
      case "simulation_seeded"                => "Queue refilled"
      case other                              => other.replace('_', ' ')

  private def scopeSelect(nameValue: String, selectedValue: String) =
    st.select(name := nameValue)(
      List("both" -> "Rated and casual", "rated" -> "Rated only", "casual" -> "Casual only").map: (key, labelText) =>
        st.option(value := key, (key == selectedValue).option(st.selected))(labelText)
    )

  private def personaSelect(nameValue: String, selectedValue: String) =
    st.select(name := nameValue)(
      BotOperations.BotPersonaMode.all.map: mode =>
        st.option(value := mode.key, (mode.key == selectedValue).option(st.selected))(mode.label)
    )

  private def panelCard(panel: AdminOpsDashboard.DashboardPanel) =
    div(style := "border:1px solid var(--border);border-radius:6px;padding:12px;background:var(--bg-page)")(
      div(style := "display:flex;justify-content:space-between;gap:10px;align-items:flex-start")(
        h3(style := "margin:0;font-size:1.1em")(panel.title),
        statusBadge(panel.severity.label, panel.severity)
      ),
      p(style := "margin:.6em 0;color:var(--c-font-dim)")(panel.sourceDescription),
      table(cls := "slist slist-pad")(
        tbody(
          panel.tiles.map: tile =>
            tr(
              td(tile.label),
              td(code(tile.value), " ", tile.unit),
              td(statusBadge(tile.severity.label, tile.severity))
            )
        )
      )
    )

  private def statusBadge(text: String, severity: AdminOpsDashboard.HealthSeverity, helpText: String = "") =
    span(
      title := helpText,
      style := s"display:inline-flex;align-items:center;border-radius:999px;padding:2px 8px;font-size:.85em;font-weight:700;${severityStyle(severity)}"
    )(text)

  private def severityStyle(severity: AdminOpsDashboard.HealthSeverity) =
    severity match
      case AdminOpsDashboard.HealthSeverity.Healthy  => "background:#dff6e8;color:#0d5f2a"
      case AdminOpsDashboard.HealthSeverity.Warning  => "background:#fff2cc;color:#7a4b00"
      case AdminOpsDashboard.HealthSeverity.Critical => "background:#ffd7d7;color:#8a1f11"
      case AdminOpsDashboard.HealthSeverity.Paused   => "background:#d7e5f8;color:#102033"
