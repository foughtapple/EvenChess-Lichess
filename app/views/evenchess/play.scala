package views.evenchess

import controllers.PlayPageModel

import lila.app.UiEnv.{ *, given }
import lila.evenchess.{ AccountMonetisationUi, EvenChessTheme, PlaySearchIntegration, PublicShell }

object play:

  def apply(model: PlayPageModel) =
    Page("Play EvenChess")
      .copy(fullTitle = "EvenChess Play \u2022 Level search".some)
      .css("bits.page"):
        main(cls := "page evenchess-play", style := "max-width:960px;margin:auto")(
          st.section(cls := "box box-pad", style := "border-top:4px solid #1f5f99")(
            h1(style := "margin-top:0")("Play EvenChess"),
            p(style := "max-width:760px;line-height:1.55")(
              "Choose a time control, pick optional level preferences, and start a fair EvenChess search."
            ),
            div(style := "display:flex;gap:.5rem;flex-wrap:wrap")(
              a(cls := "button button-empty", href := PublicShell.PublicRoutes.study)("Study"),
              a(cls := "button button-empty", href := PublicShell.PublicRoutes.analysis)("Analysis"),
              a(cls := "button button-empty", href := PublicShell.PublicRoutes.preferences)("Settings"),
              a(cls := "button button-empty", href := PublicShell.PublicRoutes.entitlements)("Tokens & plans")
            )
          ),
          st.section(cls := "box box-pad", style := "margin-top:16px")(
            h2(style := "margin:0 0 14px;font-size:22px;line-height:1.25;color:#102033")("Search setup"),
            ul(style := "margin:0 0 16px;padding-left:20px;line-height:1.55")(
              li("ECR is separate from normal Lichess ratings."),
              li("Used Level never decreases during a game."),
              li("Both players see the match levels before coaching can appear.")
            ),
            model.error.map: error =>
              p(style := "margin:0 0 14px;color:#8a1f11;font-weight:700")(error)
            ,
            model.accountDashboard.map(tokenBalance),
            if model.authenticated then searchForm(model)
            else authPrompt,
            model.prepared.map(searchStatus)
          )
        )

  private def authPrompt =
    div(style := s"${EvenChessTheme.Style.card};max-width:760px")(
      h3(style := "margin:0 0 8px;font-size:18px;line-height:1.25;color:#102033")("Log in to start search"),
      p(style := "margin:0 0 14px;line-height:1.5")(
        "Log in so games, ratings, tokens, and review history stay connected to your account."
      ),
      div(style := EvenChessTheme.Style.actions)(
        a(style := EvenChessTheme.Style.primaryButton, href := routes.Auth.signup)("Create account"),
        a(style := EvenChessTheme.Style.secondaryButton, href := PublicShell.PublicRoutes.login)("Log in")
      )
    )

  private def tokenBalance(dashboard: AccountMonetisationUi.AccountDashboard) =
    div(style := s"${EvenChessTheme.Style.darkCard};margin-bottom:16px")(
      div(style := "display:flex;justify-content:space-between;gap:16px;align-items:flex-start;flex-wrap:wrap")(
        div(
          h2(style := "margin:0 0 8px;font-size:20px;line-height:1.25;color:#ffffff")("Your EvenChess access"),
          p(style := "margin:0;line-height:1.5;color:#d7e5f8")(
            "Game starts use your account access. Plans and rewarded tokens never make live rated help stronger."
          )
        ),
        a(style := EvenChessTheme.Style.secondaryButton, href := PublicShell.PublicRoutes.entitlements)("Manage tokens")
      ),
      div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px;margin-top:14px")(
        stat("Game tokens", dashboard.state.gameTokens.toString),
        stat("Earned tokens", s"${dashboard.state.earnedAdGameTokens}/${dashboard.rewardedAdStatus.cap}"),
        stat("Plan", AccountMonetisationUi.PlanCards.tierKey(dashboard.state.plan)),
        stat("Search access", accessLabel(dashboard.tokenSnapshot.accessReason(PlaySearchIntegration.PlayMode.RatedEvenChess)))
      ),
      Option.when(dashboard.tokenSnapshot.freeMatchTokenWindowActive)(
        p(style := "margin:14px 0 0;color:#ffffff;font-weight:700")("Tokens are temporarily free")
      )
    )

  private def searchForm(model: PlayPageModel) =
    st.form(
      action := PlaySearchIntegration.Routes.search,
      method := "GET",
      style := s"${EvenChessTheme.Style.card};max-width:900px"
    )(
      div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:14px")(
        field("Mode")(
          select(name := "mode", style := inputStyle)(
            PlaySearchIntegration.PlayMode.publicOptions.map: mode =>
              st.option(st.value := mode.key, Option.when(mode == model.form.mode)(selected))(mode.label)
          )
        ),
        field("Time control")(
          select(name := "timeControl", style := inputStyle)(
            PlaySearchIntegration.TimeControlOptions.all.map: choice =>
              st.option(st.value := choice.key, Option.when(choice == model.form.timeControl)(selected))(choice.label)
          )
        ),
        field("Set Level")(
          input(
            tpe := "number",
            name := "setLevel",
            value := model.form.setLevel.value.toString,
            style := inputStyle
          )
        ),
        field("Target Level")(
          input(
            tpe := "number",
            name := "targetLevel",
            value := model.form.targetLevel.map(_.value.toString).getOrElse(""),
            placeholder := "Target only",
            style := inputStyle
          )
        )
      ),
      input(tpe := "hidden", name := "outsideHelp", value := "acknowledged"),
      p(style := "margin:16px 0 0;line-height:1.5")(
        strong("Disclosure: "),
        "EvenChess allows only the assistance shown in-game. Other engines, people, notes, bots, chat help, and outside analysis remain prohibited in rated EvenChess."
      ),
      label(style := "display:flex;gap:10px;align-items:flex-start;margin:14px 0;line-height:1.45")(
        input(tpe := "checkbox", name := "confirmLevelContract", value := "true"),
        span("I accept that EvenChess may widen matching if my exact level preferences are not available.")
      ),
      button(tpe := "submit", cls := "button button-fat", style := "margin-top:4px")("Start EvenChess search")
    )

  private def searchStatus(prepared: PlaySearchIntegration.PreparedSearch) =
    val record = prepared.record
    div(style := s"${EvenChessTheme.Style.darkCard};margin-top:18px")(
      h2(style := "margin:0 0 12px;font-size:20px;line-height:1.25;color:#ffffff")("Search started"),
      div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:10px")(
        stat("Queue", record.queueState.label),
        stat("Set Level", s"L${record.ticket.setLevel.value}"),
        stat("Preferences", record.matchPreferences.scenario.label),
        stat("Access", accessLabel(record.tokenSnapshot.accessReason(record.mode)))
      ),
      p(style := "margin:14px 0 0;line-height:1.55;color:#d7e5f8")(
        if prepared.coachingMayRender then "Your game is ready."
        else "Searching for a fair match. Coaching appears only after the game is created and levels are confirmed."
      )
    )

  private def field(labelText: String)(content: Frag) =
    label(style := "display:grid;gap:6px;font-weight:700;color:#102033")(
      span(labelText),
      content
    )

  private def stat(labelText: String, valueText: String) =
    div(style := "border:1px solid rgba(215,229,248,.24);border-radius:8px;padding:10px;background:rgba(255,255,255,.05)")(
      small(style := "display:block;color:#b7cae6")(labelText),
      strong(style := "display:block;color:#ffffff;overflow-wrap:anywhere")(valueText)
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

  private val inputStyle =
    "width:100%;box-sizing:border-box;border:1px solid #b7cae6;border-radius:6px;padding:10px;background:#ffffff;color:#102033"
