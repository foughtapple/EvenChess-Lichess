package views.evenchess

import controllers.AccountPageModel

import lila.app.UiEnv.*
import lila.evenchess.{ AccountMonetisationUi, EvenChessTheme, PublicShell }

object account:

  def apply(model: AccountPageModel) =
    val dashboard = model.dashboard
    Page("EvenChess account")
      .copy(fullTitle = "EvenChess Account \u2022 Tokens and plans".some)
      .css("bits.page"):
        main(cls := "page evenchess-account", style := "max-width:960px;margin:auto")(
          st.section(cls := "box box-pad", style := "border-top:4px solid #1f5f99")(
            h1(style := "margin-top:0")("EvenChess account"),
            p(style := "max-width:760px;line-height:1.55")(
              "Manage game access, review summaries, rewarded tokens, and plans from one account page."
            ),
            div(style := "display:flex;gap:.5rem;flex-wrap:wrap")(
              a(cls := "button", href := PublicShell.PublicRoutes.liveGames)("Play EvenChess"),
              a(cls := "button button-empty", href := PublicShell.PublicRoutes.preferences)("Settings")
            ),
            p(style := "margin:16px 0 0;line-height:1.55")(
              strong("Fair play: "),
              dashboard.fairnessCopy
            )
          ),
          st.section(cls := "box box-pad", style := "margin-top:16px")(
            h2(style := "margin:0 0 14px;font-size:22px;line-height:1.25;color:#102033")("Balance"),
            div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:12px")(
              metric("Game tokens", dashboard.state.gameTokens.toString, "Used to start rated or casual EvenChess games."),
              metric("Rewarded tokens", s"${dashboard.state.earnedAdGameTokens}/${dashboard.rewardedAdStatus.cap}", "Earned tokens are capped per account."),
              metric("Match summaries", dashboard.summaryQuotaStatus.matchRemaining.toString, "Review summaries available for completed games."),
              metric("Performance summaries", dashboard.summaryQuotaStatus.performanceRemaining.toString, "Performance review access, separate from live play.")
            )
          ),
          st.section(id := "plans", cls := "box box-pad", style := "margin-top:16px")(
            h2(style := "margin:0 0 14px;font-size:22px;line-height:1.25;color:#102033")("Plans"),
            div(style := EvenChessTheme.Style.cardGrid)(
              dashboard.planCards.map(planCard)
            )
          ),
          st.section(id := "rewarded-ads", cls := "box box-pad", style := "margin-top:16px")(
            h2(style := "margin:0 0 14px;font-size:22px;line-height:1.25;color:#102033")("Earn game tokens"),
            div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px")(
              metric("Rewarded token bank", s"${dashboard.rewardedAdStatus.earnedTokens}/${dashboard.rewardedAdStatus.cap}", "Available earned tokens for game starts."),
              metric("Per completed reward", dashboard.rewardedAdStatus.grantAmount.toString, "One completed reward grants one game token."),
              metric("Availability", rewardedAvailability(dashboard.rewardedAdStatus), rewardedAvailabilityNote(dashboard.rewardedAdStatus))
            ),
            p(style := "margin:14px 0 0;line-height:1.55;color:#102033")(dashboard.rewardedAdStatus.stateText)
          ),
          st.section(id := "summaries", cls := "box box-pad", style := "margin-top:16px")(
            h2(style := "margin:0 0 14px;font-size:22px;line-height:1.25;color:#102033")("Summary quota"),
            p(style := "margin:0 0 12px;line-height:1.55;color:#102033")(
              "Match and performance summaries are for review only. They do not change live coaching strength or ratings."
            ),
            div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:12px")(
              metric("Match summaries", dashboard.summaryQuotaStatus.matchRemaining.toString, "Available game-by-game review summaries."),
              metric("Performance summaries", dashboard.summaryQuotaStatus.performanceRemaining.toString, "Available broader progress summaries."),
              metric("Failed attempts", "Not charged", "If a summary cannot be generated, your quota is not spent.")
            )
          ),
          st.section(cls := "box box-pad", style := "margin-top:16px")(
            h2(style := "margin:0 0 14px;font-size:22px;line-height:1.25;color:#102033")("When tokens are used"),
            div(style := "display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px")(
              dashboard.settlementRules.map(settlementRule)
            )
          )
        )

  private def metric(labelText: String, valueText: String, note: String) =
    div(style := s"${EvenChessTheme.Style.card};min-height:104px")(
      small(style := "display:block;color:#46617f;font-weight:700;text-transform:uppercase;font-size:11px;letter-spacing:.04em")(labelText),
      strong(style := "display:block;margin-top:6px;font-size:24px;line-height:1.15;color:#102033;overflow-wrap:anywhere")(valueText),
      p(style := "margin:8px 0 0;line-height:1.4;color:#46617f")(note)
    )

  private def planCard(card: AccountMonetisationUi.PlanCard) =
    div(style := s"${EvenChessTheme.Style.darkCard};display:flex;flex-direction:column;gap:12px")(
      div(
        div(style := "display:flex;align-items:center;justify-content:space-between;gap:10px")(
          h3(style := "margin:0;font-size:19px;line-height:1.25;color:#ffffff")(card.title),
          Option.when(card.current)(
            span(style := "border:1px solid rgba(255,255,255,.3);border-radius:999px;padding:3px 8px;color:#d7e5f8;font-size:12px")("Current")
          )
        ),
        strong(style := "display:block;margin-top:8px;font-size:26px;line-height:1.15;color:#ffffff")(card.price.total),
        card.price.weekly.map: weekly =>
          small(style := "display:block;margin-top:4px;color:#b7cae6")(weekly)
      ),
      ul(style := "margin:0;padding-left:20px;line-height:1.55;color:#d7e5f8")(
        card.features.map(feature => li(feature))
      ),
      card.checkoutHref match
        case Some(url) =>
          a(style := EvenChessTheme.Style.secondaryButton, href := url)(
            if card.current then "Current plan" else "Choose plan"
          )
        case None if !card.current =>
          span(style := "color:#b7cae6;font-weight:700")("Plan upgrades are not available yet")
        case None =>
          span(style := "color:#b7cae6;font-weight:700")("Included account access")
    )

  private def settlementRule(row: AccountMonetisationUi.SettlementRuleRow) =
    div(style := EvenChessTheme.Style.darkCard)(
      h3(style := "margin:0 0 8px;font-size:18px;line-height:1.25;color:#ffffff")(row.label),
      p(style := "margin:0 0 12px;line-height:1.45;color:#d7e5f8")(row.description),
      div(style := "display:flex;gap:8px;flex-wrap:wrap")(
        badge(if row.consumesToken then "Token used" else "No token used"),
        badge(if row.refundsToken then "Token returned" else "No refund needed"),
        Option.when(row.triggersCooldown)(badge("Cooldown"))
      )
    )

  private def badge(text: String) =
    span(style := "border:1px solid rgba(215,229,248,.28);border-radius:999px;padding:4px 8px;color:#d7e5f8;font-size:12px")(text)

  private def rewardedAvailability(status: AccountMonetisationUi.RewardedAdStatus) =
    if !status.providerVerified then "Unavailable"
    else if status.canRequestAd then "Available"
    else "Full"

  private def rewardedAvailabilityNote(status: AccountMonetisationUi.RewardedAdStatus) =
    if !status.providerVerified then "Rewarded earning is off until it is enabled for this site."
    else "Earned tokens are available while below the account cap."
