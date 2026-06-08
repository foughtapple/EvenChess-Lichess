package lila.evenchess

class TtsCoachTest extends munit.FunSuite:

  import MonetisationPolicy.PlanTier
  import ProductInvariants.RequirementClass
  import TtsCoach.*

  private val displayed =
    DisplayedCoachText(
      title = "Improve the piece first",
      body = "Your bishop is loose, so stabilize it before forcing trades.",
      bullets = List("Read the shown coaching card only.")
    )

  private def enabledConfig: ClientConfig =
    ClientConfig.fromSettings(
      preferences = UserSettings.default.copy(ttsCoach = UserSettings.default.ttsCoach.copy(enabled = true)),
      backend = AdminBackendSettings.default,
      policyVersion = "tts-v1"
    )

  private def request(
      surface: Surface = Surface.LiveRound,
      auditId: String = "audit-live-1",
      ratedLive: Boolean = true,
      isPlayerTurn: Boolean = true,
      requestedSpeechText: Option[String] = None
  ): PlaybackRequest =
    PlaybackRequest(
      surface = surface,
      contextId = "game-1",
      cardId = "card-1",
      auditId = auditId,
      ratedLive = ratedLive,
      isPlayerTurn = isPlayerTurn,
      displayedText = displayed,
      requestedSpeechText = requestedSpeechText,
      serverAuthorized = true,
      approvedDisplayPayload = true
    )

  test("Version 1.2 Phase H requirements are classified before integration"):
    val byRequirement = PhaseHRequirementClassifications.all.map(c => c.requirement -> c.classification).toMap

    assertEquals(byRequirement(PhaseHRequirement.LichessSpeechShell), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseHRequirement.UserTtsControls), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseHRequirement.SameAuthorizedShownText), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseHRequirement.BrowserSpeechFirst), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseHRequirement.AdminProviderLimitsKillSwitchCost), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseHRequirement.ServerProviderDecisionDeferred), RequirementClass.UnresolvedProductOwnerDecision)

  test("TTS is off by default and browser speech is the safe initial provider seam"):
    val config = ClientConfig.fromSettings(UserSettings.default, AdminBackendSettings.default, "tts-v1")
    val adminPolicy = AdminTtsPolicy.fromBackend(AdminBackendSettings.default)

    assert(!config.enabled)
    assertEquals(config.provider, Provider.BrowserSpeech)
    assertEquals(config.queueBehavior, QueueBehavior.ReplaceCurrent)
    assert(config.muteDuringOpponentTurn)
    assert(config.safeForBrowser)
    assert(adminPolicy.browserSpeechOnlyForClient)
    assert(adminPolicy.safeForClientConfig)

  test("authorized TTS reads only the same text already shown in the card"):
    val decision = PlaybackPolicy.decide(enabledConfig, request())

    assert(decision.allowed)
    assertEquals(decision.reason, DecisionReason.Allowed)
    assertEquals(decision.speechText, Some(displayed.spokenText))
    assert(decision.auditEvent.exists(_.hasRequiredFields))

  test("stronger or separate speech text is rejected"):
    val decision = PlaybackPolicy.decide(
      enabledConfig,
      request(requestedSpeechText = Some("Play the engine move Re8 now."))
    )

    assert(!decision.allowed)
    assertEquals(decision.reason, DecisionReason.TextMismatch)

  test("raw engine prompts provider secrets and hidden debug data block playback"):
    val unsafe = request().copy(rawEnginePayload = Some("pv e2e4 e7e5"), providerSecret = Some("sk-secret"))
    val decision = PlaybackPolicy.decide(enabledConfig, unsafe)

    assert(!decision.allowed)
    assertEquals(decision.reason, DecisionReason.UnsafePayload)

  test("live rated TTS is auditable without storing raw spoken text"):
    val decision = PlaybackPolicy.decide(enabledConfig, request())
    val event = decision.auditEvent.get

    assert(decision.allowed)
    assertEquals(event.sourceAuditId, "audit-live-1")
    assertEquals(event.surface, Surface.LiveRound)
    assertEquals(event.charCount, displayed.spokenText.length)
    assertNotEquals(event.textSha256, displayed.spokenText)
    assert(!event.storesRawText)

  test("live TTS without audit identity is rejected"):
    val decision = PlaybackPolicy.decide(enabledConfig, request(auditId = ""))

    assert(!decision.allowed)
    assertEquals(decision.reason, DecisionReason.MissingAudit)

  test("learning-surface TTS without overlay audit identity is rejected"):
    val decision = PlaybackPolicy.decide(
      enabledConfig,
      request(surface = Surface.Analysis, auditId = "", ratedLive = false)
    )

    assert(!decision.allowed)
    assertEquals(decision.reason, DecisionReason.MissingAudit)

  test("mute during opponent turn suppresses live playback"):
    val decision = PlaybackPolicy.decide(enabledConfig, request(isPlayerTurn = false))

    assert(!decision.allowed)
    assertEquals(decision.reason, DecisionReason.MutedOpponentTurn)

  test("server-side provider seam is present but not browser-playable until approved"):
    val config = enabledConfig.copy(provider = Provider.ServerProvider)
    val decision = PlaybackPolicy.decide(config, request())

    assert(!decision.allowed)
    assertEquals(decision.reason, DecisionReason.UnsupportedProvider)

  test("paid plans cannot receive stronger live TTS help"):
    assert(!LiveTtsFairness.liveTtsAddsSeparateAdviceChannel)
    assert(!LiveTtsFairness.premiumMayProvideStrongerLiveTts)
    assert(LiveTtsFairness.sameLiveStrengthAcrossPlans)
    assertEquals(PlanTier.values.map(LiveTtsFairness.liveStrengthKey).toSet.size, 1)
