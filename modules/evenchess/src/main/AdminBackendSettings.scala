package lila.evenchess

import scala.util.Try

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

import ProductInvariants.RequirementClass

object AdminBackendSettings:
  // phase c marker

  enum PhaseERequirement:
    case ExistingAdminSettingsShell
    case AuthorizedAdminOnly
    case ProviderModelAndKeyStatus
    case TtsProviderAndKeyStatus
    case StockfishProfileControls
    case PerSurfaceAiEnablement
    case OverlayFeatureFlags
    case TokenAdPaymentSwitches
    case CampaignKillSwitches
    case BotSimulationControls
    case EcorCalibrationControls
    case CostAndRateLimits
    case AuditRetention
    case IncidentPauseControls
    case SecretNonExposure
    case FairnessBoundary
    case SecretEntryMechanismDecision

  final case class PhaseERequirementClassification(
      requirement: PhaseERequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseERequirementClassifications:
    val all: List[PhaseERequirementClassification] = List(
      PhaseERequirementClassification(
        PhaseERequirement.ExistingAdminSettingsShell,
        RequirementClass.LichessProvided,
        "Use the existing Lichess dev settings page, SettingStore persistence, and Settings permission gate."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.AuthorizedAdminOnly,
        RequirementClass.LichessProvided,
        "View and change access remains behind the existing Secure(_.Settings) Lichess admin permission."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.ProviderModelAndKeyStatus,
        RequirementClass.EvenChessSpecific,
        "Track OpenAI provider/model and key status without storing or rendering raw provider secrets."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.TtsProviderAndKeyStatus,
        RequirementClass.EvenChessSpecific,
        "Track TTS provider and key status as server-side operational config, not user-visible coaching strength."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.StockfishProfileControls,
        RequirementClass.EvenChessSpecific,
        "Expose bounded Stockfish profile controls for EvenChess assistance infrastructure only."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.PerSurfaceAiEnablement,
        RequirementClass.AdaptedToLichessFork,
        "Represent live, study, opening, and analysis AI enablement separately so future adapters can respect surface boundaries."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.OverlayFeatureFlags,
        RequirementClass.EvenChessSpecific,
        "Keep overlay, coaching card, and Offset Count feature flags in EvenChess-owned settings."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.TokenAdPaymentSwitches,
        RequirementClass.EvenChessSpecific,
        "Keep token, rewarded-ad, and payment provider switches out of live-rated fairness decisions."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.CampaignKillSwitches,
        RequirementClass.EvenChessSpecific,
        "Campaign and paid-acquisition controls can pause marketing but must not change rated help strength."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.BotSimulationControls,
        RequirementClass.EvenChessSpecific,
        "Admin-only bot matchmaking and simulation controls govern fallback seeding and local queue stress testing without changing normal Lichess chess rules."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.EcorCalibrationControls,
        RequirementClass.EvenChessSpecific,
        "Admin-only ECOR controls expose versioned level-gap offsets, rating-to-level bands, calibration history, and calculated recommendations without leaking internal diagnostics publicly."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.CostAndRateLimits,
        RequirementClass.EvenChessSpecific,
        "Server-side cost and rate limits bound AI, TTS, and engine work before provider integration."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.AuditRetention,
        RequirementClass.EvenChessSpecific,
        "Audit retention is an EvenChess operational policy setting."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.IncidentPauseControls,
        RequirementClass.EvenChessSpecific,
        "Incident controls may pause or no-rate affected flows only through auditable admin settings."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.SecretNonExposure,
        RequirementClass.EvenChessSpecific,
        "Raw API keys are not SettingStore values, browser config values, or log values."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.FairnessBoundary,
        RequirementClass.EvenChessSpecific,
        "Backend settings cannot silently alter rated fairness outside explicit incident controls."
      ),
      PhaseERequirementClassification(
        PhaseERequirement.SecretEntryMechanismDecision,
        RequirementClass.UnresolvedProductOwnerDecision,
        "Version 1.2 prefers env/secret-store status display; raw secret entry is deferred until a safe mechanism is approved."
      )
    )

  object SettingIds:
    val prefix = "evenchess.backend."

    val openAiProvider = s"${prefix}openAi.provider"
    val openAiModel = s"${prefix}openAi.model"
    val openAiKeyConfigured = s"${prefix}openAi.keyConfigured"
    val openAiKeyRotated = s"${prefix}openAi.keyRotated"

    val ttsProvider = s"${prefix}tts.provider"
    val ttsKeyConfigured = s"${prefix}tts.keyConfigured"
    val ttsKeyRotated = s"${prefix}tts.keyRotated"

    val stockfishProfile = s"${prefix}stockfish.profile"
    val stockfishMaxDepth = s"${prefix}stockfish.maxDepth"
    val stockfishMaxMultipv = s"${prefix}stockfish.maxMultipv"
    val stockfishEngineJobsPerMinute = s"${prefix}stockfish.engineJobsPerMinute"
    val stockfishEquivalentRatingBands = s"${prefix}stockfish.equivalentRatingBands"

    val liveAiEnabled = s"${prefix}surface.liveAiEnabled"
    val studyAiEnabled = s"${prefix}surface.studyAiEnabled"
    val openingAiEnabled = s"${prefix}surface.openingAiEnabled"
    val analysisAiEnabled = s"${prefix}surface.analysisAiEnabled"

    val overlaysEnabled = s"${prefix}feature.overlaysEnabled"
    val coachingCardsEnabled = s"${prefix}feature.coachingCardsEnabled"
    val offsetCountEnabled = s"${prefix}feature.offsetCountEnabled"

    val tokensEnabled = s"${prefix}monetisation.tokensEnabled"
    val rewardedAdsEnabled = s"${prefix}monetisation.rewardedAdsEnabled"
    val paymentsEnabled = s"${prefix}monetisation.paymentsEnabled"
    val freeMatchTokensEnabled = s"${prefix}monetisation.freeMatchTokens.enabled"
    val freeMatchTokensStartsAt = s"${prefix}monetisation.freeMatchTokens.startsAt"
    val freeMatchTokensEndsAt = s"${prefix}monetisation.freeMatchTokens.endsAt"

    val campaignVariant = s"${prefix}campaign.variant"
    val campaignKillSwitch = s"${prefix}campaign.killSwitch"
    val paidAcquisitionPaused = s"${prefix}campaign.paidAcquisitionPaused"

    val matchmakingBotModeEnabled = s"${prefix}matchmaking.botModeEnabled"
    val matchmakingBotModeScope = s"${prefix}matchmaking.botModeScope"
    val matchmakingBotMatchTimeoutSeconds = s"${prefix}matchmaking.botMatchTimeoutSeconds"
    val matchmakingBotAccountRoster = s"${prefix}matchmaking.botAccountRoster"

    val botSimulationEnabled = s"${prefix}simulation.enabled"
    val botSimulationScope = s"${prefix}simulation.scope"
    val botSimulationBotCount = s"${prefix}simulation.botCount"
    val botSimulationRatingMin = s"${prefix}simulation.ratingMin"
    val botSimulationRatingMax = s"${prefix}simulation.ratingMax"
    val botSimulationLevelMin = s"${prefix}simulation.levelMin"
    val botSimulationLevelMax = s"${prefix}simulation.levelMax"
    val botSimulationPersona = s"${prefix}simulation.persona"
    val botSimulationTimeControls = s"${prefix}simulation.timeControls"
    val botSimulationAccountRoster = s"${prefix}simulation.accountRoster"

    val ecorPolicyVersion = s"${prefix}ecor.policyVersion"
    val ecorGapOffsets = s"${prefix}ecor.gapOffsets"
    val ecorRatingLevelBands = s"${prefix}ecor.ratingLevelBands"
    val ecorSnapshotHistory = s"${prefix}ecor.snapshotHistory"

    val aiDailyCostLimitCents = s"${prefix}limit.aiDailyCostLimitCents"
    val aiRateLimitPerMinute = s"${prefix}limit.aiRateLimitPerMinute"
    val ttsDailyCostLimitCents = s"${prefix}limit.ttsDailyCostLimitCents"
    val ttsRateLimitPerMinute = s"${prefix}limit.ttsRateLimitPerMinute"
    val auditRetentionDays = s"${prefix}audit.retentionDays"

    val incidentGlobalPause = s"${prefix}incident.globalPause"
    val incidentLiveCoachingPaused = s"${prefix}incident.liveCoachingPaused"
    val incidentAiPaused = s"${prefix}incident.aiPaused"
    val incidentTtsPaused = s"${prefix}incident.ttsPaused"
    val incidentEnginePaused = s"${prefix}incident.enginePaused"
    val incidentTokenAdsPaused = s"${prefix}incident.tokenAdsPaused"
    val incidentNoRate = s"${prefix}incident.noRate"
    val incidentPublicNotice = s"${prefix}incident.publicNotice"

    val all: List[String] = List(
      openAiProvider,
      openAiModel,
      openAiKeyConfigured,
      openAiKeyRotated,
      ttsProvider,
      ttsKeyConfigured,
      ttsKeyRotated,
      stockfishProfile,
      stockfishMaxDepth,
      stockfishMaxMultipv,
      stockfishEngineJobsPerMinute,
      stockfishEquivalentRatingBands,
      liveAiEnabled,
      studyAiEnabled,
      openingAiEnabled,
      analysisAiEnabled,
      overlaysEnabled,
      coachingCardsEnabled,
      offsetCountEnabled,
      tokensEnabled,
      rewardedAdsEnabled,
      paymentsEnabled,
      freeMatchTokensEnabled,
      freeMatchTokensStartsAt,
      freeMatchTokensEndsAt,
      campaignVariant,
      campaignKillSwitch,
      paidAcquisitionPaused,
      matchmakingBotModeEnabled,
      matchmakingBotModeScope,
      matchmakingBotMatchTimeoutSeconds,
      matchmakingBotAccountRoster,
      botSimulationEnabled,
      botSimulationScope,
      botSimulationBotCount,
      botSimulationRatingMin,
      botSimulationRatingMax,
      botSimulationLevelMin,
      botSimulationLevelMax,
      botSimulationPersona,
      botSimulationTimeControls,
      botSimulationAccountRoster,
      ecorPolicyVersion,
      ecorGapOffsets,
      ecorRatingLevelBands,
      ecorSnapshotHistory,
      aiDailyCostLimitCents,
      aiRateLimitPerMinute,
      ttsDailyCostLimitCents,
      ttsRateLimitPerMinute,
      auditRetentionDays,
      incidentGlobalPause,
      incidentLiveCoachingPaused,
      incidentAiPaused,
      incidentTtsPaused,
      incidentEnginePaused,
      incidentTokenAdsPaused,
      incidentNoRate,
      incidentPublicNotice
    )

    val secretStatusControls: Set[String] =
      Set(openAiKeyConfigured, openAiKeyRotated, ttsKeyConfigured, ttsKeyRotated)

    val incidentControls: Set[String] =
      Set(
        incidentGlobalPause,
        incidentLiveCoachingPaused,
        incidentAiPaused,
        incidentTtsPaused,
        incidentEnginePaused,
        incidentTokenAdsPaused,
        incidentNoRate,
        incidentPublicNotice
      )

    val fairnessIncidentControls: Set[String] =
      Set(incidentGlobalPause, incidentLiveCoachingPaused, incidentEnginePaused, incidentNoRate)

    def isEvenChessBackend(id: String): Boolean =
      all.contains(id) || id.startsWith(prefix)

    def canAffectRatedFairness(id: String): Boolean =
      fairnessIncidentControls.contains(id)

  object Defaults:
    val openAiProvider = "openai"
    val openAiModel = "gpt-4.1-mini"
    val ttsProvider = "browser-speech"
    val stockfishProfile = "bounded-live-v1"
    val stockfishMaxDepth = 12
    val stockfishMaxMultipv = 3
    val stockfishEngineJobsPerMinute = 120
    val stockfishEquivalentRatingBands = EvenChessRatingCalibration.StockfishAiRatingDefaults.tableText
    val campaignVariant = "default"
    val freeMatchTokensEnabled = false
    val freeMatchTokensStartsAt = ""
    val freeMatchTokensEndsAt = ""
    val botModeEnabled = false
    val botModeScope = "both"
    val botMatchTimeoutSeconds = 45
    val botAccountRoster = BotOperations.BotAccountRoster.generatedDefaultCsv
    val botSimulationEnabled = false
    val botSimulationScope = "both"
    val botSimulationBotCount = BotOperations.defaultSimulationBots
    val botSimulationRatingMin = 900
    val botSimulationRatingMax = 2100
    val botSimulationLevelMin = 0
    val botSimulationLevelMax = 10
    val botSimulationPersona = "mixed"
    val botSimulationTimeControls = "bullet,blitz,rapid,classical"
    val botSimulationAccountRoster = BotOperations.BotAccountRoster.generatedDefaultCsv
    val ecorPolicyVersion = EvenChessRatingCalibration.defaultPolicyVersion
    val ecorGapOffsets = EvenChessRatingCalibration.EcorDefaults.gapText
    val ecorRatingLevelBands = EvenChessRatingCalibration.EcorDefaults.ratingBandsText
    val ecorSnapshotHistory = EvenChessRatingCalibration.EcorDefaults.historyText
    val aiDailyCostLimitCents = 0
    val aiRateLimitPerMinute = 30
    val ttsDailyCostLimitCents = 0
    val ttsRateLimitPerMinute = 60
    val auditRetentionDays = 365

  enum SecretStatus(val key: String, val label: String):
    case Missing extends SecretStatus("missing", "Missing")
    case Configured extends SecretStatus("configured", "Configured")
    case Rotated extends SecretStatus("rotated", "Rotated")

  object SecretStatus:
    def from(configured: Boolean, rotated: Boolean): SecretStatus =
      if !configured then Missing else if rotated then Rotated else Configured

  final case class ProviderKeyStatus(configured: Boolean, rotated: Boolean):
    def status: SecretStatus = SecretStatus.from(configured, rotated)
    def safeAdminLabel: String = status.label
    def exposesRawSecret: Boolean = false

  final case class OpenAiBackend(
      provider: String,
      model: String,
      keyStatus: ProviderKeyStatus
  ):
    def safeRows: List[SafeAdminRow] = List(
      SafeAdminRow(SettingIds.openAiProvider, "OpenAI provider", provider),
      SafeAdminRow(SettingIds.openAiModel, "OpenAI model", model),
      SafeAdminRow(SettingIds.openAiKeyConfigured, "OpenAI key status", keyStatus.safeAdminLabel)
    )

  final case class TtsBackend(
      provider: String,
      keyStatus: ProviderKeyStatus
  ):
    def safeRows: List[SafeAdminRow] = List(
      SafeAdminRow(SettingIds.ttsProvider, "TTS provider", provider),
      SafeAdminRow(SettingIds.ttsKeyConfigured, "TTS key status", keyStatus.safeAdminLabel)
    )

  final case class StockfishBackend(
      profile: String,
      maxDepth: Int,
      maxMultipv: Int,
      engineJobsPerMinute: Int,
      equivalentRatingBandsText: String
  ):
    def equivalentRatingTable: Either[String, EvenChessRatingCalibration.StockfishAiRatingTableConfig] =
      EvenChessRatingCalibration.StockfishAiRatingTableConfig.fromText(equivalentRatingBandsText)

    def valid: Boolean =
      profile.nonEmpty && maxDepth > 0 && maxMultipv > 0 && engineJobsPerMinute > 0 && equivalentRatingTable.exists(_.valid)

  final case class SurfaceAiEnablement(
      live: Boolean,
      study: Boolean,
      opening: Boolean,
      analysis: Boolean
  )

  final case class FeatureFlags(
      overlays: Boolean,
      coachingCards: Boolean,
      offsetCount: Boolean
  )

  final case class MonetisationSwitches(
      tokens: Boolean,
      rewardedAds: Boolean,
      payments: Boolean,
      freeMatchTokenWindow: FreeMatchTokenWindow
  )

  final case class FreeMatchTokenWindow(
      enabled: Boolean,
      startsAt: String,
      endsAt: String
  ):
    def startsAtMillis: Option[Long] = FreeMatchTokenWindow.parseMillis(startsAt)
    def endsAtMillis: Option[Long] = FreeMatchTokenWindow.parseMillis(endsAt)

    def activeAt(now: Long): Boolean =
      enabled &&
        startsAtMillis.exists(_ <= now) &&
        endsAtMillis.exists(now < _)

    def publicMessageAt(now: Long): Option[String] =
      Option.when(activeAt(now))(FreeMatchTokenWindow.publicMessage)

    def valid: Boolean =
      if !enabled then true
      else
        (startsAtMillis, endsAtMillis) match
          case (Some(start), Some(end)) => start < end
          case _                       => false

  object FreeMatchTokenWindow:
    val publicMessage = "Tokens are temporarily free"
    val disabled: FreeMatchTokenWindow =
      FreeMatchTokenWindow(
        enabled = Defaults.freeMatchTokensEnabled,
        startsAt = Defaults.freeMatchTokensStartsAt,
        endsAt = Defaults.freeMatchTokensEndsAt
      )

    def parseMillis(value: String): Option[Long] =
      val raw = value.trim
      if raw.isEmpty then None
      else
        raw.toLongOption
          .orElse(Try(Instant.parse(raw).toEpochMilli).toOption)
          .orElse(Try(OffsetDateTime.parse(raw).toInstant.toEpochMilli).toOption)
          .orElse(Try(LocalDateTime.parse(raw).atZone(ZoneId.systemDefault).toInstant.toEpochMilli).toOption)

  final case class MatchmakingControls(
      botModeEnabled: Boolean,
      botModeScope: String,
      botMatchTimeoutSeconds: Int,
      botAccountRoster: String = Defaults.botAccountRoster
  )

  final case class BotSimulationControls(
      enabled: Boolean,
      scope: String,
      botCount: Int,
      ratingMin: Int,
      ratingMax: Int,
      levelMin: Int,
      levelMax: Int,
      persona: String,
      timeControls: String = Defaults.botSimulationTimeControls,
      accountRoster: String = Defaults.botSimulationAccountRoster
  ):
    def valid: Boolean =
      List("rated", "casual", "both").contains(scope) &&
        botCount >= 0 &&
        botCount <= BotOperations.maxSimulationBots &&
        ratingMin >= 100 &&
        ratingMax <= 5000 &&
        ratingMin <= ratingMax &&
        levelMin >= 0 &&
        levelMax <= 10 &&
        levelMin <= levelMax &&
        BotOperations.BotPersonaMode.all.exists(_.key == persona) &&
        BotOperations.SimulationTimeControlOptions.fromCsv(timeControls).nonEmpty &&
        BotOperations.BotAccountRoster.fromCsv(accountRoster).forall(_.nonEmpty)

  final case class EcorControls(
      policyVersion: String,
      gapOffsetsText: String,
      ratingLevelBandsText: String,
      snapshotHistoryText: String
  ):
    def config: Either[String, EvenChessRatingCalibration.EcorTableConfig] =
      EvenChessRatingCalibration.EcorTableConfig.fromText(policyVersion, gapOffsetsText, ratingLevelBandsText)

    def adminState: EvenChessRatingCalibration.EcorAdminState =
      EvenChessRatingCalibration.EcorAdminState.fromText(policyVersion, gapOffsetsText, ratingLevelBandsText, snapshotHistoryText)

    def valid: Boolean =
      config.exists(_.valid)

  final case class CampaignControls(
      variant: String,
      killSwitch: Boolean,
      paidAcquisitionPaused: Boolean
  ):
    def valid: Boolean = variant.nonEmpty

  final case class SafetyLimits(
      aiDailyCostLimitCents: Int,
      aiRateLimitPerMinute: Int,
      ttsDailyCostLimitCents: Int,
      ttsRateLimitPerMinute: Int,
      auditRetentionDays: Int
  ):
    def valid: Boolean =
      aiDailyCostLimitCents >= 0 &&
        aiRateLimitPerMinute > 0 &&
        ttsDailyCostLimitCents >= 0 &&
        ttsRateLimitPerMinute > 0 &&
        auditRetentionDays >= 30

  final case class IncidentControls(
      globalPause: Boolean,
      liveCoachingPaused: Boolean,
      aiPaused: Boolean,
      ttsPaused: Boolean,
      enginePaused: Boolean,
      tokenAdsPaused: Boolean,
      noRate: Boolean,
      publicNotice: String
  ):
    def active: Boolean =
      globalPause || liveCoachingPaused || aiPaused || ttsPaused || enginePaused || tokenAdsPaused || noRate

    def canAffectRatedFairness: Boolean =
      globalPause || liveCoachingPaused || enginePaused || noRate

  final case class BackendSettings(
      openAi: OpenAiBackend,
      tts: TtsBackend,
      stockfish: StockfishBackend,
      surfaces: SurfaceAiEnablement,
      features: FeatureFlags,
      monetisation: MonetisationSwitches,
      matchmaking: MatchmakingControls,
      botSimulation: BotSimulationControls,
      ecor: EcorControls,
      campaign: CampaignControls,
      limits: SafetyLimits,
      incident: IncidentControls
  ):
    def valid: Boolean =
      stockfish.valid && botSimulation.valid && ecor.valid && campaign.valid && monetisation.freeMatchTokenWindow.valid && limits.valid

    def safeAdminSnapshot: SafeAdminSnapshot =
      SafeAdminSnapshot(
        openAi.safeRows ++
          tts.safeRows ++
          List(
            SafeAdminRow(SettingIds.stockfishProfile, "Stockfish profile", stockfish.profile),
            SafeAdminRow(SettingIds.stockfishMaxDepth, "Stockfish max depth", stockfish.maxDepth.toString),
            SafeAdminRow(SettingIds.stockfishMaxMultipv, "Stockfish max MultiPV", stockfish.maxMultipv.toString),
            SafeAdminRow(SettingIds.stockfishEngineJobsPerMinute, "Engine jobs per minute", stockfish.engineJobsPerMinute.toString),
            SafeAdminRow(SettingIds.stockfishEquivalentRatingBands, "Stockfish equivalent rating bands", stockfish.equivalentRatingBandsText),
            SafeAdminRow(SettingIds.liveAiEnabled, "Live AI enabled", surfaces.live.toString),
            SafeAdminRow(SettingIds.studyAiEnabled, "Study AI enabled", surfaces.study.toString),
            SafeAdminRow(SettingIds.openingAiEnabled, "Opening AI enabled", surfaces.opening.toString),
            SafeAdminRow(SettingIds.analysisAiEnabled, "Analysis AI enabled", surfaces.analysis.toString),
            SafeAdminRow(SettingIds.overlaysEnabled, "Overlays enabled", features.overlays.toString),
            SafeAdminRow(SettingIds.coachingCardsEnabled, "Coaching cards enabled", features.coachingCards.toString),
            SafeAdminRow(SettingIds.offsetCountEnabled, "Offset Count enabled", features.offsetCount.toString),
            SafeAdminRow(SettingIds.tokensEnabled, "Tokens enabled", monetisation.tokens.toString),
            SafeAdminRow(SettingIds.rewardedAdsEnabled, "Rewarded ads enabled", monetisation.rewardedAds.toString),
            SafeAdminRow(SettingIds.paymentsEnabled, "Payments enabled", monetisation.payments.toString),
            SafeAdminRow(SettingIds.freeMatchTokensEnabled, "Free match tokens enabled", monetisation.freeMatchTokenWindow.enabled.toString),
            SafeAdminRow(SettingIds.freeMatchTokensStartsAt, "Free match tokens starts at", monetisation.freeMatchTokenWindow.startsAt),
            SafeAdminRow(SettingIds.freeMatchTokensEndsAt, "Free match tokens ends at", monetisation.freeMatchTokenWindow.endsAt),
            SafeAdminRow(SettingIds.campaignVariant, "Campaign variant", campaign.variant),
            SafeAdminRow(SettingIds.campaignKillSwitch, "Campaign kill switch", campaign.killSwitch.toString),
            SafeAdminRow(SettingIds.paidAcquisitionPaused, "Paid acquisition paused", campaign.paidAcquisitionPaused.toString),
            SafeAdminRow(SettingIds.matchmakingBotModeEnabled, "Bot-mode matchmaking enabled", matchmaking.botModeEnabled.toString),
            SafeAdminRow(SettingIds.matchmakingBotModeScope, "Bot-mode scope", matchmaking.botModeScope),
            SafeAdminRow(
              SettingIds.matchmakingBotMatchTimeoutSeconds,
              "Bot match timeout (seconds)",
              matchmaking.botMatchTimeoutSeconds.toString
            ),
            SafeAdminRow(
              SettingIds.matchmakingBotAccountRoster,
              "Bot matchmaking account roster",
              s"${BotOperations.BotAccountRoster.effectiveFromCsv(matchmaking.botAccountRoster).size} account(s)"
            ),
            SafeAdminRow(SettingIds.botSimulationEnabled, "Bot simulation enabled", botSimulation.enabled.toString),
            SafeAdminRow(SettingIds.botSimulationScope, "Bot simulation scope", botSimulation.scope),
            SafeAdminRow(SettingIds.botSimulationBotCount, "Bot simulation count", botSimulation.botCount.toString),
            SafeAdminRow(SettingIds.botSimulationRatingMin, "Bot simulation rating min", botSimulation.ratingMin.toString),
            SafeAdminRow(SettingIds.botSimulationRatingMax, "Bot simulation rating max", botSimulation.ratingMax.toString),
            SafeAdminRow(SettingIds.botSimulationLevelMin, "Bot simulation level min", botSimulation.levelMin.toString),
            SafeAdminRow(SettingIds.botSimulationLevelMax, "Bot simulation level max", botSimulation.levelMax.toString),
            SafeAdminRow(SettingIds.botSimulationPersona, "Bot simulation persona", botSimulation.persona),
            SafeAdminRow(SettingIds.botSimulationTimeControls, "Bot simulation time controls", botSimulation.timeControls),
            SafeAdminRow(
              SettingIds.botSimulationAccountRoster,
              "Bot simulation account roster",
              s"${BotOperations.BotAccountRoster.effectiveFromCsv(botSimulation.accountRoster).size} account(s)"
            ),
            SafeAdminRow(SettingIds.ecorPolicyVersion, "ECOR policy version", ecor.policyVersion),
            SafeAdminRow(SettingIds.ecorGapOffsets, "ECOR gap offsets", ecor.gapOffsetsText),
            SafeAdminRow(SettingIds.ecorRatingLevelBands, "ECOR rating-to-level bands", ecor.ratingLevelBandsText),
            SafeAdminRow(
              SettingIds.ecorSnapshotHistory,
              "ECOR snapshot history",
              s"${EvenChessRatingCalibration.EcorHistory.parse(ecor.snapshotHistoryText).size} snapshot(s)"
            ),
            SafeAdminRow(SettingIds.aiDailyCostLimitCents, "AI daily cost limit cents", limits.aiDailyCostLimitCents.toString),
            SafeAdminRow(SettingIds.aiRateLimitPerMinute, "AI rate limit per minute", limits.aiRateLimitPerMinute.toString),
            SafeAdminRow(SettingIds.ttsDailyCostLimitCents, "TTS daily cost limit cents", limits.ttsDailyCostLimitCents.toString),
            SafeAdminRow(SettingIds.ttsRateLimitPerMinute, "TTS rate limit per minute", limits.ttsRateLimitPerMinute.toString),
            SafeAdminRow(SettingIds.auditRetentionDays, "Audit retention days", limits.auditRetentionDays.toString),
            SafeAdminRow(SettingIds.incidentGlobalPause, "Incident global pause", incident.globalPause.toString),
            SafeAdminRow(SettingIds.incidentLiveCoachingPaused, "Incident live coaching paused", incident.liveCoachingPaused.toString),
            SafeAdminRow(SettingIds.incidentAiPaused, "Incident AI paused", incident.aiPaused.toString),
            SafeAdminRow(SettingIds.incidentTtsPaused, "Incident TTS paused", incident.ttsPaused.toString),
            SafeAdminRow(SettingIds.incidentEnginePaused, "Incident engine paused", incident.enginePaused.toString),
            SafeAdminRow(SettingIds.incidentTokenAdsPaused, "Incident token/ads paused", incident.tokenAdsPaused.toString),
            SafeAdminRow(SettingIds.incidentNoRate, "Incident no-rate", incident.noRate.toString),
            SafeAdminRow(SettingIds.incidentPublicNotice, "Incident public notice", incident.publicNotice)
          )
      )

    def canChangeRatedFairness(settingId: String): Boolean =
      SettingIds.canAffectRatedFairness(settingId)

  final case class SafeAdminRow(id: String, label: String, value: String):
    def safeForBrowser: Boolean = !looksLikeRawSecret(value)

  final case class SafeAdminSnapshot(rows: List[SafeAdminRow]):
    def safeForBrowser: Boolean = rows.forall(_.safeForBrowser)
    def containsRawSecret: Boolean = !safeForBrowser

  val redactedLogValue = "[redacted-evenchess-secret]"

  def safeLogValue(settingId: String, value: String): String =
    if SettingIds.isEvenChessBackend(settingId) && looksLikeRawSecret(value) then redactedLogValue
    else value

  def isUnsafeEvenChessBackendValue(settingId: String, value: String): Boolean =
    SettingIds.isEvenChessBackend(settingId) && looksLikeRawSecret(value)

  final case class ConfigChangeAudit(
      settingId: String,
      adminId: String,
      before: String,
      after: String,
      reason: String,
      auditId: String
  ):
    def rollbackValue: String = before
    def affectsRatedFairness: Boolean = SettingIds.canAffectRatedFairness(settingId)
    def safeForLogs: Boolean =
      safeLogValue(settingId, before) == before && safeLogValue(settingId, after) == after
    def rollbackable: Boolean =
      SettingIds.isEvenChessBackend(settingId) &&
        adminId.nonEmpty &&
        auditId.nonEmpty
    def allowed: Boolean =
      rollbackable && safeForLogs && (!affectsRatedFairness || reason.nonEmpty)

  val default: BackendSettings =
    BackendSettings(
      openAi = OpenAiBackend(
        provider = Defaults.openAiProvider,
        model = Defaults.openAiModel,
        keyStatus = ProviderKeyStatus(configured = false, rotated = false)
      ),
      tts = TtsBackend(
        provider = Defaults.ttsProvider,
        keyStatus = ProviderKeyStatus(configured = false, rotated = false)
      ),
      stockfish = StockfishBackend(
        profile = Defaults.stockfishProfile,
        maxDepth = Defaults.stockfishMaxDepth,
        maxMultipv = Defaults.stockfishMaxMultipv,
        engineJobsPerMinute = Defaults.stockfishEngineJobsPerMinute,
        equivalentRatingBandsText = Defaults.stockfishEquivalentRatingBands
      ),
      surfaces = SurfaceAiEnablement(
        live = false,
        study = false,
        opening = false,
        analysis = false
      ),
      features = FeatureFlags(
        overlays = true,
        coachingCards = true,
        offsetCount = true
      ),
      monetisation = MonetisationSwitches(
        tokens = false,
        rewardedAds = false,
        payments = false,
        freeMatchTokenWindow = FreeMatchTokenWindow.disabled
      ),
      matchmaking = MatchmakingControls(
        botModeEnabled = Defaults.botModeEnabled,
        botModeScope = Defaults.botModeScope,
        botMatchTimeoutSeconds = Defaults.botMatchTimeoutSeconds,
        botAccountRoster = Defaults.botAccountRoster
      ),
      botSimulation = BotSimulationControls(
        enabled = Defaults.botSimulationEnabled,
        scope = Defaults.botSimulationScope,
        botCount = Defaults.botSimulationBotCount,
        ratingMin = Defaults.botSimulationRatingMin,
        ratingMax = Defaults.botSimulationRatingMax,
        levelMin = Defaults.botSimulationLevelMin,
        levelMax = Defaults.botSimulationLevelMax,
        persona = Defaults.botSimulationPersona,
        timeControls = Defaults.botSimulationTimeControls,
        accountRoster = Defaults.botSimulationAccountRoster
      ),
      ecor = EcorControls(
        policyVersion = Defaults.ecorPolicyVersion,
        gapOffsetsText = Defaults.ecorGapOffsets,
        ratingLevelBandsText = Defaults.ecorRatingLevelBands,
        snapshotHistoryText = Defaults.ecorSnapshotHistory
      ),
      campaign = CampaignControls(
        variant = Defaults.campaignVariant,
        killSwitch = true,
        paidAcquisitionPaused = true
      ),
      limits = SafetyLimits(
        aiDailyCostLimitCents = Defaults.aiDailyCostLimitCents,
        aiRateLimitPerMinute = Defaults.aiRateLimitPerMinute,
        ttsDailyCostLimitCents = Defaults.ttsDailyCostLimitCents,
        ttsRateLimitPerMinute = Defaults.ttsRateLimitPerMinute,
        auditRetentionDays = Defaults.auditRetentionDays
      ),
      incident = IncidentControls(
        globalPause = false,
        liveCoachingPaused = false,
        aiPaused = false,
        ttsPaused = false,
        enginePaused = false,
        tokenAdsPaused = false,
        noRate = false,
        publicNotice = ""
      )
    )

  private val secretMarkers: List[String] =
    List("sk-", "api_key=", "apikey=", "secret=", "token=", "BEGIN PRIVATE KEY", "xoxb-", "eyJhbGci")

  private def looksLikeRawSecret(value: String): Boolean =
    val trimmed = value.trim
    trimmed.nonEmpty &&
      (secretMarkers.exists(marker => trimmed.toLowerCase.contains(marker.toLowerCase)) ||
        (trimmed.length >= 48 && !trimmed.exists(_.isWhitespace) && trimmed.exists(_.isLetter) && trimmed.exists(_.isDigit)))
