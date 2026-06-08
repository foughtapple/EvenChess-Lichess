package lila.web

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient

@Module
final class Env(
    appConfig: play.api.Configuration,
    cacheApi: lila.memo.CacheApi,
    settingStore: lila.memo.SettingStore.Builder,
    ws: StandaloneWSClient,
    net: lila.core.config.NetConfig,
    getFile: lila.common.config.GetRelativeFile
)(using mode: play.api.Mode, scheduler: Scheduler)(using Executor):

  val config = WebConfig.loadFrom(appConfig)
  export config.pagerDuty as pagerDutyConfig
  export net.baseUrl

  val analyseEndpoints = WebConfig.analyseEndpoints(appConfig)
  lazy val lilaVersion = WebConfig.lilaVersion(appConfig)

  val manifest = wire[AssetManifest]

  val referrerRedirect = wire[ReferrerRedirect]

  val github = wire[GitHub]

  lazy val emailError = wire[EmailError]

  private lazy val influxEvent = InfluxEvent(
    ws = ws,
    endpoint = config.influxEventEndpoint,
    env = config.influxEventEnv
  )
  if mode.isProd then scheduler.scheduleOnce(5.seconds)(influxEvent.start())

  wire[PagerDuty]

  val lichobileAnnounceApi = wire[LichobileAnnounceApi]

  AnnounceApi.setupPeriodicUpdate()

  object settings:
    import lila.evenchess.AdminBackendSettings
    import lila.core.data.{ Strings, Text, UserIds }
    import lila.memo.SettingStore.Text.given
    import lila.memo.SettingStore.Strings.given
    import lila.memo.SettingStore.UserIds.given

    private val evenChessSettingIds = AdminBackendSettings.SettingIds
    private val evenChessDefaults = AdminBackendSettings.Defaults

    val apiTimeline = settingStore[Int](
      "apiTimelineEntries",
      default = 10,
      text = "API timeline entries to serve".some
    )
    val noDelaySecret = settingStore[Strings](
      "noDelaySecrets",
      default = Strings(Nil),
      text = "Secret tokens that allows fetching ongoing games without the delay. Separated by commas.".some
    )
    val prizeTournamentMakers = settingStore[UserIds](
      "prizeTournamentMakers",
      default = UserIds(Nil),
      text =
        "User IDs who can make prize tournaments (arena & swiss) without a warning. Separated by commas.".some
    )
    val apiExplorerGamesPerSecond = settingStore[Int](
      "apiExplorerGamesPerSecond",
      default = 300,
      text = "Opening explorer games per second".some
    )

    val evenChessOpenAiProvider = settingStore[String](
      evenChessSettingIds.openAiProvider,
      default = evenChessDefaults.openAiProvider,
      text = "EvenChess OpenAI provider label. Store raw API keys only in server secret/env config.".some
    )
    val evenChessOpenAiModel = settingStore[String](
      evenChessSettingIds.openAiModel,
      default = evenChessDefaults.openAiModel,
      text = "EvenChess OpenAI model for explanation/compression seams, not chess truth.".some
    )
    val evenChessOpenAiKeyConfigured = settingStore[Boolean](
      evenChessSettingIds.openAiKeyConfigured,
      default = false,
      text = "Status only: OpenAI key is configured in server secret/env config.".some
    )
    val evenChessOpenAiKeyRotated = settingStore[Boolean](
      evenChessSettingIds.openAiKeyRotated,
      default = false,
      text = "Status only: OpenAI key has been rotated. Does not store the key.".some
    )
    val evenChessTtsProvider = settingStore[String](
      evenChessSettingIds.ttsProvider,
      default = evenChessDefaults.ttsProvider,
      text = "EvenChess TTS provider label. Store provider secrets only in server secret/env config.".some
    )
    val evenChessTtsKeyConfigured = settingStore[Boolean](
      evenChessSettingIds.ttsKeyConfigured,
      default = false,
      text = "Status only: TTS key is configured in server secret/env config.".some
    )
    val evenChessTtsKeyRotated = settingStore[Boolean](
      evenChessSettingIds.ttsKeyRotated,
      default = false,
      text = "Status only: TTS key has been rotated. Does not store the key.".some
    )
    val evenChessStockfishProfile = settingStore[String](
      evenChessSettingIds.stockfishProfile,
      default = evenChessDefaults.stockfishProfile,
      text = "EvenChess bounded Stockfish profile key for legal assistance jobs.".some
    )
    val evenChessStockfishMaxDepth = settingStore[Int](
      evenChessSettingIds.stockfishMaxDepth,
      default = evenChessDefaults.stockfishMaxDepth,
      text = "Maximum Stockfish depth for EvenChess assistance jobs.".some
    )
    val evenChessStockfishMaxMultipv = settingStore[Int](
      evenChessSettingIds.stockfishMaxMultipv,
      default = evenChessDefaults.stockfishMaxMultipv,
      text = "Maximum MultiPV lines for EvenChess assistance jobs.".some
    )
    val evenChessStockfishEngineJobsPerMinute = settingStore[Int](
      evenChessSettingIds.stockfishEngineJobsPerMinute,
      default = evenChessDefaults.stockfishEngineJobsPerMinute,
      text = "EvenChess engine job rate limit per minute.".some
    )
    val evenChessStockfishEquivalentRatingBands = settingStore[Text](
      evenChessSettingIds.stockfishEquivalentRatingBands,
      default = Text(evenChessDefaults.stockfishEquivalentRatingBands),
      text = "Approximate Lichess Stockfish AI level equivalent rating bands, e.g. SF4=1200-1399.".some
    )
    val evenChessLiveAiEnabled = settingStore[Boolean](
      evenChessSettingIds.liveAiEnabled,
      default = false,
      text = "Enable EvenChess AI explanations on authorized live surfaces only.".some
    )
    val evenChessStudyAiEnabled = settingStore[Boolean](
      evenChessSettingIds.studyAiEnabled,
      default = false,
      text = "Enable EvenChess AI explanations on study surfaces.".some
    )
    val evenChessOpeningAiEnabled = settingStore[Boolean](
      evenChessSettingIds.openingAiEnabled,
      default = false,
      text = "Enable EvenChess AI explanations on opening surfaces.".some
    )
    val evenChessAnalysisAiEnabled = settingStore[Boolean](
      evenChessSettingIds.analysisAiEnabled,
      default = false,
      text = "Enable EvenChess AI explanations on analysis surfaces.".some
    )
    val evenChessOverlaysEnabled = settingStore[Boolean](
      evenChessSettingIds.overlaysEnabled,
      default = true,
      text = "Enable EvenChess overlay rendering when server-authorized.".some
    )
    val evenChessCoachingCardsEnabled = settingStore[Boolean](
      evenChessSettingIds.coachingCardsEnabled,
      default = true,
      text = "Enable EvenChess coaching cards when server-authorized.".some
    )
    val evenChessOffsetCountEnabled = settingStore[Boolean](
      evenChessSettingIds.offsetCountEnabled,
      default = true,
      text = "Enable Offset Count / Exchange Resolver display when authorized.".some
    )
    val evenChessTokensEnabled = settingStore[Boolean](
      evenChessSettingIds.tokensEnabled,
      default = false,
      text = "Enable EvenChess token provider integration. Must not affect rated fairness.".some
    )
    val evenChessRewardedAdsEnabled = settingStore[Boolean](
      evenChessSettingIds.rewardedAdsEnabled,
      default = false,
      text = "Enable EvenChess rewarded ads. Must not provide stronger live help.".some
    )
    val evenChessPaymentsEnabled = settingStore[Boolean](
      evenChessSettingIds.paymentsEnabled,
      default = false,
      text = "Enable EvenChess payment provider integration. Must not provide stronger live help.".some
    )
    val evenChessFreeMatchTokensEnabled = settingStore[Boolean](
      evenChessSettingIds.freeMatchTokensEnabled,
      default = evenChessDefaults.freeMatchTokensEnabled,
      text = "Enable launch free-match-token window. While active, rated/casual starts do not consume startup/ad game tokens.".some
    )
    val evenChessFreeMatchTokensStartsAt = settingStore[String](
      evenChessSettingIds.freeMatchTokensStartsAt,
      default = evenChessDefaults.freeMatchTokensStartsAt,
      text = "Launch free-match-token window start. Use ISO instant, ISO local datetime, or epoch millis.".some
    )
    val evenChessFreeMatchTokensEndsAt = settingStore[String](
      evenChessSettingIds.freeMatchTokensEndsAt,
      default = evenChessDefaults.freeMatchTokensEndsAt,
      text = "Launch free-match-token window end. Use ISO instant, ISO local datetime, or epoch millis.".some
    )
    val evenChessCampaignVariant = settingStore[String](
      evenChessSettingIds.campaignVariant,
      default = evenChessDefaults.campaignVariant,
      text = "EvenChess campaign variant key for marketing/funnel adapters.".some
    )
    val evenChessCampaignKillSwitch = settingStore[Boolean](
      evenChessSettingIds.campaignKillSwitch,
      default = true,
      text = "Pause EvenChess campaign variants and campaign-controlled public messaging.".some
    )
    val evenChessPaidAcquisitionPaused = settingStore[Boolean](
      evenChessSettingIds.paidAcquisitionPaused,
      default = true,
      text = "Pause EvenChess paid acquisition while launch or queue health is not ready.".some
    )
    val evenChessMatchmakingBotModeEnabled = settingStore[Boolean](
      evenChessSettingIds.matchmakingBotModeEnabled,
      default = evenChessDefaults.botModeEnabled,
      text = "Enable timed bot matchmaking fallback when no human opponent is found within timeout.".some
    )
    val evenChessMatchmakingBotModeScope = settingStore[String](
      evenChessSettingIds.matchmakingBotModeScope,
      default = evenChessDefaults.botModeScope,
      text = "Bot matchmaking scope for fallback pairing: rated, casual, or both.".some
    )
    val evenChessMatchmakingBotMatchTimeoutSeconds = settingStore[Int](
      evenChessSettingIds.matchmakingBotMatchTimeoutSeconds,
      default = evenChessDefaults.botMatchTimeoutSeconds,
      text = "Delay in seconds before bot fallback matchmaking is allowed.".some
    )
    val evenChessMatchmakingBotAccountRoster = settingStore[String](
      evenChessSettingIds.matchmakingBotAccountRoster,
      default = evenChessDefaults.botAccountRoster,
      text = "Shared local bot account usernames for human-style bot games. Used by matchmaking fallback and simulation.".some
    )
    val evenChessBotSimulationEnabled = settingStore[Boolean](
      evenChessSettingIds.botSimulationEnabled,
      default = evenChessDefaults.botSimulationEnabled,
      text = "Enable admin-controlled EvenChess simulated-player queue entries for local/staged queue testing.".some
    )
    val evenChessBotSimulationScope = settingStore[String](
      evenChessSettingIds.botSimulationScope,
      default = evenChessDefaults.botSimulationScope,
      text = "Bot simulation scope for roster-backed simulated players: rated, casual, or both.".some
    )
    val evenChessBotSimulationBotCount = settingStore[Int](
      evenChessSettingIds.botSimulationBotCount,
      default = evenChessDefaults.botSimulationBotCount,
      text = "Target number of roster-backed simulated players to keep in EvenChess simulation mode.".some
    )
    val evenChessBotSimulationRatingMin = settingStore[Int](
      evenChessSettingIds.botSimulationRatingMin,
      default = evenChessDefaults.botSimulationRatingMin,
      text = "Minimum target ECR for EvenChess simulation bots.".some
    )
    val evenChessBotSimulationRatingMax = settingStore[Int](
      evenChessSettingIds.botSimulationRatingMax,
      default = evenChessDefaults.botSimulationRatingMax,
      text = "Maximum target ECR for EvenChess simulation bots.".some
    )
    val evenChessBotSimulationLevelMin = settingStore[Int](
      evenChessSettingIds.botSimulationLevelMin,
      default = evenChessDefaults.botSimulationLevelMin,
      text = "Minimum Set Level for EvenChess simulation bots.".some
    )
    val evenChessBotSimulationLevelMax = settingStore[Int](
      evenChessSettingIds.botSimulationLevelMax,
      default = evenChessDefaults.botSimulationLevelMax,
      text = "Maximum Set Level for EvenChess simulation bots.".some
    )
    val evenChessBotSimulationPersona = settingStore[String](
      evenChessSettingIds.botSimulationPersona,
      default = evenChessDefaults.botSimulationPersona,
      text = "Simulation bot timing/persona mix: mixed, human-like, or fast.".some
    )
    val evenChessBotSimulationTimeControls = settingStore[String](
      evenChessSettingIds.botSimulationTimeControls,
      default = evenChessDefaults.botSimulationTimeControls,
      text = "Comma-separated time-control families that simulated players may join: bullet, blitz, rapid, classical.".some
    )
    val evenChessBotSimulationAccountRoster = settingStore[String](
      evenChessSettingIds.botSimulationAccountRoster,
      default = evenChessDefaults.botSimulationAccountRoster,
      text = "Shared local bot account usernames for human-style bot games. Kept in sync with matchmaking bot roster.".some
    )
    val evenChessEcorPolicyVersion = settingStore[String](
      evenChessSettingIds.ecorPolicyVersion,
      default = evenChessDefaults.ecorPolicyVersion,
      text = "Active EvenChess offset ratings table policy version.".some
    )
    val evenChessEcorGapOffsets = settingStore[Text](
      evenChessSettingIds.ecorGapOffsets,
      default = Text(evenChessDefaults.ecorGapOffsets),
      text = "ECOR gap offsets, one row per adjacent level gap, e.g. L4-L5=17.".some
    )
    val evenChessEcorRatingLevelBands = settingStore[Text](
      evenChessSettingIds.ecorRatingLevelBands,
      default = Text(evenChessDefaults.ecorRatingLevelBands),
      text = "Equivalent rating-to-base-level table, one row per band, e.g. 1500-1699=L7.".some
    )
    val evenChessEcorSnapshotHistory = settingStore[Text](
      evenChessSettingIds.ecorSnapshotHistory,
      default = Text(evenChessDefaults.ecorSnapshotHistory),
      text = "Timestamped ECOR table snapshots for rollback/review. Managed by the ECOR admin panel.".some
    )
    val evenChessAiDailyCostLimitCents = settingStore[Int](
      evenChessSettingIds.aiDailyCostLimitCents,
      default = evenChessDefaults.aiDailyCostLimitCents,
      text = "EvenChess AI daily cost limit in cents; 0 means disabled until approved.".some
    )
    val evenChessAiRateLimitPerMinute = settingStore[Int](
      evenChessSettingIds.aiRateLimitPerMinute,
      default = evenChessDefaults.aiRateLimitPerMinute,
      text = "EvenChess AI request limit per minute.".some
    )
    val evenChessTtsDailyCostLimitCents = settingStore[Int](
      evenChessSettingIds.ttsDailyCostLimitCents,
      default = evenChessDefaults.ttsDailyCostLimitCents,
      text = "EvenChess TTS daily cost limit in cents; 0 keeps paid provider use disabled.".some
    )
    val evenChessTtsRateLimitPerMinute = settingStore[Int](
      evenChessSettingIds.ttsRateLimitPerMinute,
      default = evenChessDefaults.ttsRateLimitPerMinute,
      text = "EvenChess TTS request limit per minute.".some
    )
    val evenChessAuditRetentionDays = settingStore[Int](
      evenChessSettingIds.auditRetentionDays,
      default = evenChessDefaults.auditRetentionDays,
      text = "EvenChess audit retention window in days.".some
    )
    val evenChessIncidentGlobalPause = settingStore[Boolean](
      evenChessSettingIds.incidentGlobalPause,
      default = false,
      text = "Incident control: globally pause EvenChess live assistance surfaces.".some
    )
    val evenChessIncidentLiveCoachingPaused = settingStore[Boolean](
      evenChessSettingIds.incidentLiveCoachingPaused,
      default = false,
      text = "Incident control: pause live coaching renders.".some
    )
    val evenChessIncidentAiPaused = settingStore[Boolean](
      evenChessSettingIds.incidentAiPaused,
      default = false,
      text = "Incident control: pause AI explanation calls.".some
    )
    val evenChessIncidentTtsPaused = settingStore[Boolean](
      evenChessSettingIds.incidentTtsPaused,
      default = false,
      text = "Incident control: pause TTS coach reads.".some
    )
    val evenChessIncidentEnginePaused = settingStore[Boolean](
      evenChessSettingIds.incidentEnginePaused,
      default = false,
      text = "Incident control: pause legal engine assistance jobs.".some
    )
    val evenChessIncidentTokenAdsPaused = settingStore[Boolean](
      evenChessSettingIds.incidentTokenAdsPaused,
      default = false,
      text = "Incident control: pause token, ad, and payment flows.".some
    )
    val evenChessIncidentNoRate = settingStore[Boolean](
      evenChessSettingIds.incidentNoRate,
      default = false,
      text = "Incident control: mark affected EvenChess games no-rate through audited ops flow.".some
    )
    val evenChessIncidentPublicNotice = settingStore[String](
      evenChessSettingIds.incidentPublicNotice,
      default = "",
      text = "Public incident notice text. Do not include secrets or anti-cheat internals.".some
    )

    val evenChessBackendSettings: List[lila.memo.SettingStore[?]] = List(
      evenChessOpenAiProvider,
      evenChessOpenAiModel,
      evenChessOpenAiKeyConfigured,
      evenChessOpenAiKeyRotated,
      evenChessTtsProvider,
      evenChessTtsKeyConfigured,
      evenChessTtsKeyRotated,
      evenChessStockfishProfile,
      evenChessStockfishMaxDepth,
      evenChessStockfishMaxMultipv,
      evenChessStockfishEngineJobsPerMinute,
      evenChessStockfishEquivalentRatingBands,
      evenChessLiveAiEnabled,
      evenChessStudyAiEnabled,
      evenChessOpeningAiEnabled,
      evenChessAnalysisAiEnabled,
      evenChessOverlaysEnabled,
      evenChessCoachingCardsEnabled,
      evenChessOffsetCountEnabled,
      evenChessTokensEnabled,
      evenChessRewardedAdsEnabled,
      evenChessPaymentsEnabled,
      evenChessFreeMatchTokensEnabled,
      evenChessFreeMatchTokensStartsAt,
      evenChessFreeMatchTokensEndsAt,
      evenChessCampaignVariant,
      evenChessCampaignKillSwitch,
      evenChessPaidAcquisitionPaused,
      evenChessMatchmakingBotModeEnabled,
      evenChessMatchmakingBotModeScope,
      evenChessMatchmakingBotMatchTimeoutSeconds,
      evenChessMatchmakingBotAccountRoster,
      evenChessBotSimulationEnabled,
      evenChessBotSimulationScope,
      evenChessBotSimulationBotCount,
      evenChessBotSimulationRatingMin,
      evenChessBotSimulationRatingMax,
      evenChessBotSimulationLevelMin,
      evenChessBotSimulationLevelMax,
      evenChessBotSimulationPersona,
      evenChessBotSimulationTimeControls,
      evenChessBotSimulationAccountRoster,
      evenChessEcorPolicyVersion,
      evenChessEcorGapOffsets,
      evenChessEcorRatingLevelBands,
      evenChessEcorSnapshotHistory,
      evenChessAiDailyCostLimitCents,
      evenChessAiRateLimitPerMinute,
      evenChessTtsDailyCostLimitCents,
      evenChessTtsRateLimitPerMinute,
      evenChessAuditRetentionDays,
      evenChessIncidentGlobalPause,
      evenChessIncidentLiveCoachingPaused,
      evenChessIncidentAiPaused,
      evenChessIncidentTtsPaused,
      evenChessIncidentEnginePaused,
      evenChessIncidentTokenAdsPaused,
      evenChessIncidentNoRate,
      evenChessIncidentPublicNotice
    )
