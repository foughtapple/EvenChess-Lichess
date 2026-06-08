package views.lobby

import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.Preload.Homepage
import lila.core.perf.UserWithPerfs
import lila.evenchess.{ AccountMonetisationUi, AdminBackendSettings, PublicShell }

object home:

  def apply(homepage: Homepage)(using ctx: Context) =
    import homepage.*
    val evenChessCopy = PublicShell.PublicCopy
    Page("")
      .copy(fullTitle = s"${evenChessCopy.productName} - ${evenChessCopy.eyebrow}".some)
      .i18n(_.variant)
      .js(
        PageModule(
          "lobby",
          Json
            .obj(
              "data" -> data,
              "showRatings" -> ctx.pref.showRatings
            )
            .add(
              "evenChessTokenBalance",
              ctx.me.map: me =>
                val now = System.currentTimeMillis
                val balance = AccountMonetisationUi.TopBarGameTokenBalance.forLichessUser(me.username.value, now)
                val freeMatchTokenWindow = AdminBackendSettings.FreeMatchTokenWindow(
                  enabled = env.web.settings.evenChessFreeMatchTokensEnabled.get(),
                  startsAt = env.web.settings.evenChessFreeMatchTokensStartsAt.get(),
                  endsAt = env.web.settings.evenChessFreeMatchTokensEndsAt.get()
                )
                Json.obj(
                  "visibleGameTokens" -> balance.visibleGameTokens,
                  "displayCount" -> balance.displayCount,
                  "displayLabel" -> balance.displayLabel,
                  "href" -> balance.href,
                  "source" -> balance.source,
                  "subscriptionActive" -> balance.subscriptionActive,
                  "freeMatchTokensActive" -> freeMatchTokenWindow.activeAt(now),
                  "freeMatchTokensMessage" -> freeMatchTokenWindow.publicMessageAt(now)
                )
            )
            .add("hasUnreadLichessMessage", hasUnreadLichessMessage)
            .add("bots", Granter.opt(_.Beta))
            .add("playban", playban.map(lila.playban.TempBan.lobbyJson))
        )
      )
      .css("lobby")
      .graph(
        OpenGraph(
          image = staticAssetUrl("logo/lichess-tile-wide.png").some,
          title = evenChessCopy.homepageTitle,
          url = netBaseUrl.into(Url),
          description = evenChessCopy.homepageSummary
        )
      )
      .hrefLangs(lila.ui.LangPath("/")):
        given Option[UserWithPerfs] = homepage.me
        main(
          cls := List(
            "lobby" -> true,
            "lobby-nope" -> (playban.isDefined || currentGame.isDefined || homepage.hasUnreadLichessMessage)
          )
        )(
          div(cls := "lobby__side")(
            ctx.blind.option(h2(trans.nvui.featuredEvents())),
            ctx.kid.no.option(views.streamer.bits.liveStreams(streams)),
            div(cls := "lobby__spotlights"):
              val eventTags = events.map(bits.spotlight)
              val relayTags = views.relay.ui.spotlight(relays)
              frag(
                eventTags,
                relayTags,
                ctx.noBot.option {
                  val nbManual = eventTags.size + relayTags.size
                  val simulBBB = simuls.find(isFeaturable(_) && nbManual < 4)
                  val nbForced = nbManual + simulBBB.size.toInt
                  val tourBBBs = if nbForced > 3 then 0 else if nbForced == 3 then 1 else 3 - nbForced
                  frag(
                    lila.tournament.Spotlight.select(tours, tourBBBs).map {
                      views.tournament.list.homepageSpotlight(_)
                    },
                    swiss.ifTrue(nbForced < 3).map(views.swiss.ui.homepageSpotlight),
                    simulBBB.map(views.simul.ui.homepageSpotlight)
                  )
                }
              )
            ,
            classes.nonEmpty.option:
              div(cls := "lobby__classes"):
                classes.map: clas =>
                  a(href := routes.Clas.show(clas.id), dataIcon := Icon.Group)(clas.name)
            ,
            if ctx.isAuth then
              div(cls := "lobby__timeline")(
                ctx.blind.option(h2(trans.site.timeline())),
                views.timeline.entries(userTimeline),
                userTimeline.nonEmpty.option:
                  a(cls := "more", href := routes.Timeline.home)(trans.site.more(), " »")
              )
            else
              div(cls := "about-side")(
                ctx.blind.option(h2(trans.site.about())),
                strong(evenChessCopy.productName),
                " is a Lichess-powered assisted chess variant. ",
                "Platform coaching is disclosed, capped, logged, and reflected in ECR. ",
                " ",
                a(href := "/source")("Source and attribution")
              )
          ),
          currentGame
            .map(bits.currentGameInfo)
            .orElse:
              hasUnreadLichessMessage.option(bits.showUnreadLichessMessage)
            .orElse:
              playban.map(bits.playbanInfo)
            .getOrElse:
              if ctx.blind then blindLobby(blindGames) else bits.lobbyApp
          ,
          div(cls := "lobby__evenchess-summary")(
            h2("What is EvenChess?"),
            p(evenChessCopy.homepageSummary),
            ul(cls := "lobby__evenchess-facts"):
              evenChessCopy.homepageFacts.map: fact =>
                li(
                  strong(fact.label),
                  span(fact.body)
                )
          ),
          div(cls := "lobby__table")(
            div(cls := "lobby__start")(
              button(cls := "button button-metal lobby__start__button lobby__start__button--hook")(
                trans.site.createLobbyGame()
              ),
              button(cls := "button button-metal lobby__start__button lobby__start__button--friend")(
                trans.site.challengeAFriend()
              ),
              button(cls := "button button-metal lobby__start__button lobby__start__button--ai")(
                trans.site.playAgainstComputer()
              )
            )
          ),
          div(cls := "lobby__tv"):
            featured.map: g =>
              views.game.mini(Pov.naturalOrientation(g), tv = true)
          ,
          puzzle.map: p =>
            views.puzzle.bits.dailyLink(p)(cls := "lobby__puzzle"),
          views.ublog.ui.homeCarousel(ublogPosts),
          div(cls := "lobby__feed"):
            views.feed.lobbyUpdates(lastUpdates)
          ,
          ctx.noBot.option(bits.underboards(tours, simuls)),
          div(cls := "lobby__about")(
            ctx.blind.option(h2(trans.site.about())),
            a(href := "/about")(trans.site.aboutX("Lichess")),
            a(href := "/faq")(trans.faq.faqAbbreviation()),
            a(href := "/contact")(trans.contact.contact()),
            a(href := "/app")(trans.site.mobileApp()),
            a(href := routes.Cms.tos)(trans.site.termsOfService()),
            a(href := "/privacy")(trans.site.privacy()),
            a(href := "/source")(trans.site.sourceCode()),
            a(href := "/ads")("Ads"),
            views.bits.connectLinks
          )
        )
