package lila.evenchess

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import MonetisationPolicy.PlanTier
import ProductInvariants.RequirementClass

object TtsCoach:

  enum PhaseHRequirement:
    case LichessSpeechShell
    case UserTtsControls
    case SameAuthorizedShownText
    case BrowserSpeechFirst
    case ServerProviderSeam
    case AdminProviderLimitsKillSwitchCost
    case LiveRatedTtsAudit
    case NoRawEnginePromptSecretLeak
    case PremiumCannotAddStrongerLiveTts
    case ServerProviderDecisionDeferred

  final case class PhaseHRequirementClassification(
      requirement: PhaseHRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseHRequirementClassifications:
    val all: List[PhaseHRequirementClassification] = List(
      PhaseHRequirementClassification(
        PhaseHRequirement.LichessSpeechShell,
        RequirementClass.LichessProvided,
        "Reuse browser speech synthesis and Lichess UI surfaces instead of rebuilding audio infrastructure."
      ),
      PhaseHRequirementClassification(
        PhaseHRequirement.UserTtsControls,
        RequirementClass.EvenChessSpecific,
        "Store opt-in TTS Coach controls for voice, speed, volume, queue behavior, and opponent-turn muting."
      ),
      PhaseHRequirementClassification(
        PhaseHRequirement.SameAuthorizedShownText,
        RequirementClass.EvenChessSpecific,
        "TTS may read only the same server-authorized sanitized text already shown in the coaching card."
      ),
      PhaseHRequirementClassification(
        PhaseHRequirement.BrowserSpeechFirst,
        RequirementClass.AdaptedToLichessFork,
        "Use browser speech synthesis from thin round, analysis, study, and opening adapters."
      ),
      PhaseHRequirementClassification(
        PhaseHRequirement.ServerProviderSeam,
        RequirementClass.EvenChessSpecific,
        "Keep a provider seam so higher-quality server-side voices can be approved later without client secrets."
      ),
      PhaseHRequirementClassification(
        PhaseHRequirement.AdminProviderLimitsKillSwitchCost,
        RequirementClass.AdaptedToLichessFork,
        "Use EvenChess admin backend settings for provider, key status, rate/cost limits, and TTS incident pause."
      ),
      PhaseHRequirementClassification(
        PhaseHRequirement.LiveRatedTtsAudit,
        RequirementClass.EvenChessSpecific,
        "Live rated TTS reads are tied to an existing coaching audit id and produce a text hash, not raw spoken text."
      ),
      PhaseHRequirementClassification(
        PhaseHRequirement.NoRawEnginePromptSecretLeak,
        RequirementClass.EvenChessSpecific,
        "Client-safe TTS payloads cannot carry raw engine data, hidden debug data, provider secrets, or raw prompts."
      ),
      PhaseHRequirementClassification(
        PhaseHRequirement.PremiumCannotAddStrongerLiveTts,
        RequirementClass.EvenChessSpecific,
        "Paid tiers may not receive stronger live rated coaching through TTS."
      ),
      PhaseHRequirementClassification(
        PhaseHRequirement.ServerProviderDecisionDeferred,
        RequirementClass.UnresolvedProductOwnerDecision,
        "Exact production TTS provider, quota model, and server-side voice route remain product-owner decisions."
      )
    )

  enum Surface(val clientKey: String):
    case LiveRound extends Surface("live")
    case Analysis extends Surface("analysis")
    case Study extends Surface("study")
    case OpeningExplorer extends Surface("opening")

  enum Provider(val key: String):
    case BrowserSpeech extends Provider("browser-speech")
    case ServerProvider extends Provider("server-provider")

  object Provider:
    val default = BrowserSpeech
    def fromKey(key: String): Provider =
      values.find(_.key == key).getOrElse(default)

  enum QueueBehavior(val key: String):
    case ReplaceCurrent extends QueueBehavior("replace-current")
    case Queue extends QueueBehavior("queue")

  object QueueBehavior:
    val default = ReplaceCurrent
    def fromKey(key: String): QueueBehavior =
      values.find(_.key == key).getOrElse(default)

  enum DecisionReason:
    case Allowed
    case Disabled
    case Unauthorized
    case UnsupportedProvider
    case UnsafePayload
    case TextMismatch
    case MissingAudit
    case MutedOpponentTurn

  final case class DisplayedCoachText(
      title: String,
      body: String,
      bullets: List[String] = Nil
  ):
    def spokenText: String =
      normalizeText((title :: body :: bullets).filter(_.nonEmpty).mkString(" "))

    def valid: Boolean =
      spokenText.nonEmpty &&
        title.nonEmpty &&
        body.nonEmpty &&
        bullets.forall(_.nonEmpty)

  final case class ClientConfig(
      enabled: Boolean,
      provider: Provider,
      voiceKey: String,
      ratePercent: Int,
      volumePercent: Int,
      queueBehavior: QueueBehavior,
      muteDuringOpponentTurn: Boolean,
      serverAuthorized: Boolean,
      policyVersion: String,
      providerSecret: Option[String] = None,
      rawPrompt: Option[String] = None,
      rawEnginePayload: Option[String] = None,
      hiddenDebugData: Option[String] = None
  ):
    def safeForBrowser: Boolean =
      serverAuthorized &&
        policyVersion.nonEmpty &&
        providerSecret.forall(_.isBlank) &&
        rawPrompt.forall(_.isBlank) &&
        rawEnginePayload.forall(_.isBlank) &&
        hiddenDebugData.forall(_.isBlank)

  object ClientConfig:
    def fromSettings(
        preferences: UserSettings.Preferences,
        backend: AdminBackendSettings.BackendSettings,
        policyVersion: String
    ): ClientConfig =
      ClientConfig(
        enabled = preferences.ttsCoach.enabled && !backend.incident.globalPause && !backend.incident.ttsPaused,
        provider = Provider.fromKey(backend.tts.provider),
        voiceKey = preferences.ttsCoach.voice.key,
        ratePercent = preferences.ttsCoach.ratePercent,
        volumePercent = preferences.ttsCoach.volumePercent,
        queueBehavior = QueueBehavior.fromKey(preferences.ttsCoach.queueBehavior.key),
        muteDuringOpponentTurn = preferences.ttsCoach.muteDuringOpponentTurn,
        serverAuthorized = !backend.incident.globalPause && !backend.incident.ttsPaused,
        policyVersion = policyVersion
      )

  final case class AdminTtsPolicy(
      provider: Provider,
      dailyCostLimitCents: Int,
      rateLimitPerMinute: Int,
      killSwitchActive: Boolean
  ):
    def browserSpeechOnlyForClient: Boolean = provider == Provider.BrowserSpeech
    def safeForClientConfig: Boolean = !killSwitchActive && dailyCostLimitCents >= 0 && rateLimitPerMinute > 0

  object AdminTtsPolicy:
    def fromBackend(backend: AdminBackendSettings.BackendSettings): AdminTtsPolicy =
      AdminTtsPolicy(
        provider = Provider.fromKey(backend.tts.provider),
        dailyCostLimitCents = backend.limits.ttsDailyCostLimitCents,
        rateLimitPerMinute = backend.limits.ttsRateLimitPerMinute,
        killSwitchActive = backend.incident.globalPause || backend.incident.ttsPaused
      )

  final case class PlaybackRequest(
      surface: Surface,
      contextId: String,
      cardId: String,
      auditId: String,
      ratedLive: Boolean,
      isPlayerTurn: Boolean,
      displayedText: DisplayedCoachText,
      requestedSpeechText: Option[String],
      serverAuthorized: Boolean,
      approvedDisplayPayload: Boolean,
      rawEnginePayload: Option[String] = None,
      rawStockfishLine: Option[String] = None,
      hiddenDebugData: Option[String] = None,
      providerSecret: Option[String] = None,
      rawPrompt: Option[String] = None
  ):
    def speechText: String =
      normalizeText(requestedSpeechText.getOrElse(displayedText.spokenText))

    def sameAsShown: Boolean =
      speechText == displayedText.spokenText

    def unsafePayload: Boolean =
      rawEnginePayload.exists(_.nonEmpty) ||
        rawStockfishLine.exists(_.nonEmpty) ||
        hiddenDebugData.exists(_.nonEmpty) ||
        providerSecret.exists(_.nonEmpty) ||
        rawPrompt.exists(_.nonEmpty)

    def requiresAudit: Boolean =
      true

  final case class TtsAuditEvent(
      eventId: String,
      sourceAuditId: String,
      surface: Surface,
      contextId: String,
      cardId: String,
      policyVersion: String,
      textSha256: String,
      charCount: Int,
      ratedLive: Boolean
  ):
    def hasRequiredFields: Boolean =
      eventId.nonEmpty &&
        sourceAuditId.nonEmpty &&
        contextId.nonEmpty &&
        cardId.nonEmpty &&
        policyVersion.nonEmpty &&
        textSha256.nonEmpty &&
        charCount > 0

    def storesRawText: Boolean = false

  final case class PlaybackDecision(
      allowed: Boolean,
      reason: DecisionReason,
      speechText: Option[String],
      auditEvent: Option[TtsAuditEvent]
  ):
    def requiresAuditSatisfied: Boolean =
      !allowed || auditEvent.forall(_.hasRequiredFields)

  object PlaybackPolicy:
    def decide(config: ClientConfig, request: PlaybackRequest): PlaybackDecision =
      if !config.enabled then deny(DecisionReason.Disabled)
      else if !config.safeForBrowser then deny(DecisionReason.UnsafePayload)
      else if config.provider != Provider.BrowserSpeech then deny(DecisionReason.UnsupportedProvider)
      else if !config.serverAuthorized || !request.serverAuthorized || !request.approvedDisplayPayload then
        deny(DecisionReason.Unauthorized)
      else if request.unsafePayload || !request.displayedText.valid || request.speechText.isEmpty then
        deny(DecisionReason.UnsafePayload)
      else if !request.sameAsShown then deny(DecisionReason.TextMismatch)
      else if config.muteDuringOpponentTurn && request.surface == Surface.LiveRound && !request.isPlayerTurn then
        deny(DecisionReason.MutedOpponentTurn)
      else if request.requiresAudit && request.auditId.isEmpty then deny(DecisionReason.MissingAudit)
      else
        val event = Option.when(request.auditId.nonEmpty)(
          TtsAuditEvent(
            eventId = s"evenchess-tts-${request.surface.clientKey}-${request.cardId}-${request.auditId}",
            sourceAuditId = request.auditId,
            surface = request.surface,
            contextId = request.contextId,
            cardId = request.cardId,
            policyVersion = config.policyVersion,
            textSha256 = sha256(request.speechText),
            charCount = request.speechText.length,
            ratedLive = request.ratedLive
          )
        )
        PlaybackDecision(
          allowed = true,
          reason = DecisionReason.Allowed,
          speechText = Some(request.speechText),
          auditEvent = event
        )

    private def deny(reason: DecisionReason): PlaybackDecision =
      PlaybackDecision(allowed = false, reason = reason, speechText = None, auditEvent = None)

  object LiveTtsFairness:
    val liveTtsAddsSeparateAdviceChannel = false
    val premiumMayProvideStrongerLiveTts = false

    def liveStrengthKey(plan: PlanTier): String =
      plan match
        case PlanTier.NewAccountOnboarding => "authorized-shown-text-only"
        case PlanTier.FreeAdSupported     => "authorized-shown-text-only"
        case PlanTier.Standard            => "authorized-shown-text-only"
        case PlanTier.Premium             => "authorized-shown-text-only"

    def sameLiveStrengthAcrossPlans: Boolean =
      PlanTier.values.map(liveStrengthKey).toSet.size == 1

  def normalizeText(text: String): String =
    text.replaceAll("\\s+", " ").trim

  private def sha256(value: String): String =
    val digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
    digest.map("%02x".format(_)).mkString
