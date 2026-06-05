package lila.evenchess

class UserSettingsTest extends munit.FunSuite:

  import ProductInvariants.RequirementClass
  import UserSettings.*

  test("Version 1.2 Phase D requirements are classified before integration"):
    val byRequirement = PhaseDRequirementClassifications.all.map(c => c.requirement -> c.classification).toMap

    assertEquals(byRequirement(PhaseDRequirement.LichessSettingsShell), RequirementClass.LichessProvided)
    assertEquals(
      byRequirement(PhaseDRequirement.ServerSidePerUserStorage),
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(byRequirement(PhaseDRequirement.DefaultSetLevel), RequirementClass.EvenChessSpecific)
    assertEquals(
      byRequirement(PhaseDRequirement.PreferredStartingUsedLevel),
      RequirementClass.EvenChessSpecific
    )
    assertEquals(
      byRequirement(PhaseDRequirement.PreferredStartingFeatureToggles),
      RequirementClass.EvenChessSpecific
    )
    assertEquals(
      byRequirement(PhaseDRequirement.OffsetCountDisplayPreference),
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(PhaseDRequirement.StudyOpeningOverlayDefaults),
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(byRequirement(PhaseDRequirement.FairnessBoundary), RequirementClass.EvenChessSpecific)

  test("new-user defaults are safe and cannot raise live coaching strength"):
    assert(default.valid)
    assertEquals(default.defaultSetLevel, 0)
    assertEquals(default.preferredUsedLevel, 0)
    assert(default.defaultFeatureToggles.asMap.values.forall(identity))
    assertEquals(default.boardHighlightIntensity, BoardHighlightIntensity.Low)
    assert(!default.ttsCoach.enabled)
    assert(!default.ttsCoach.autoSpeak)
    assertEquals(default.ttsCoach.autoDelaySeconds, 1)
    assertEquals(default.ttsCoach.queueBehavior, TtsQueueBehavior.ReplaceCurrent)
    assert(default.ttsCoach.muteDuringOpponentTurn)
    assert(!default.learningOverlayDefaults.studyAiOverlay)
    assert(!default.learningOverlayDefaults.openingAiOverlay)
    assert(default.cannotRaiseLiveCoachingStrength(serverMaximum = 0))
    assert(default.cannotRaiseLiveCoachingStrength(serverMaximum = 4))

  test("user default Set Level is capped by server policy"):
    val wantsL10 = default.copy(defaultSetLevel = 10)

    assertEquals(wantsL10.authorizedDefaultSetLevel(serverMaximum = 3), 3)
    assertEquals(wantsL10.authorizedDefaultSetLevel(serverMaximum = 10), 10)
    assert(wantsL10.cannotRaiseLiveCoachingStrength(serverMaximum = 3))

  test("preferred starting Used Level is capped by the game Set Level"):
    val wantsL8 = default.copy(preferredUsedLevel = 8)

    assertEquals(wantsL8.startingUsedLevelFor(setLevel = 3), 3)
    assertEquals(wantsL8.startingUsedLevelFor(setLevel = 10), 8)

  test("form data is normalized before it becomes server preferences"):
    val dirty = FormData(
      defaultSetLevel = 99,
      preferredUsedLevel = 99,
      defaultFeatureToggles = DefaultFeatureToggles.default.copy(offsetCount = false, candidate2 = false),
      overlayDensity = "unknown",
      coachingCardVerbosity = "unknown",
      boardHighlightIntensity = "unknown",
      offsetCountDisplay = "unknown",
      aiSummaryPreference = "unknown",
      ttsEnabled = true,
      ttsAutoSpeak = true,
      ttsAutoDelaySeconds = 99,
      ttsVoice = "../not-a-voice",
      ttsRatePercent = 500,
      ttsVolumePercent = -50,
      ttsQueueBehavior = "../not-a-queue",
      ttsMuteDuringOpponentTurn = false,
      studyAiOverlay = true,
      openingAiOverlay = true,
      telemetryPreference = "unknown"
    ).toPreferences

    assertEquals(dirty.defaultSetLevel, 10)
    assertEquals(dirty.preferredUsedLevel, 10)
    assertEquals(dirty.defaultFeatureToggles.offsetCount, false)
    assertEquals(dirty.defaultFeatureToggles.candidate2, false)
    assertEquals(dirty.overlayDensity, OverlayDensity.default)
    assertEquals(dirty.coachingCardVerbosity, CoachingCardVerbosity.default)
    assertEquals(dirty.boardHighlightIntensity, BoardHighlightIntensity.default)
    assertEquals(dirty.offsetCountDisplay, OffsetCountDisplay.default)
    assertEquals(dirty.aiSummaryPreference, AiSummaryPreference.default)
    assert(dirty.ttsCoach.autoSpeak)
    assertEquals(dirty.ttsCoach.autoDelaySeconds, maxTtsAutoDelaySeconds)
    assertEquals(dirty.ttsCoach.voice, TtsVoice.default)
    assertEquals(dirty.ttsCoach.ratePercent, maxTtsRatePercent)
    assertEquals(dirty.ttsCoach.volumePercent, minTtsVolumePercent)
    assertEquals(dirty.ttsCoach.queueBehavior, TtsQueueBehavior.default)
    assert(!dirty.ttsCoach.muteDuringOpponentTurn)
    assertEquals(dirty.telemetryPreference, TelemetryPreference.default)
    assert(dirty.valid)

  test("server-side tag storage round trips while preserving unrelated preference tags"):
    val preferences = default.copy(
      defaultSetLevel = 4,
      preferredUsedLevel = 6,
      defaultFeatureToggles = DefaultFeatureToggles.default.copy(
        offsetCount = false,
        candidate2 = false,
        evalNumbers = false
      ),
      overlayDensity = OverlayDensity.Detailed,
      coachingCardVerbosity = CoachingCardVerbosity.Brief,
      boardHighlightIntensity = BoardHighlightIntensity.Medium,
      offsetCountDisplay = OffsetCountDisplay.AlwaysWhenAuthorized,
      aiSummaryPreference = AiSummaryPreference.AutoAfterGame,
      ttsCoach = TtsCoach(
        enabled = true,
        autoSpeak = true,
        autoDelaySeconds = 3,
        voice = TtsVoice.Warm,
        ratePercent = 115,
        volumePercent = 90,
        queueBehavior = TtsQueueBehavior.Queue,
        muteDuringOpponentTurn = false
      ),
      learningOverlayDefaults = LearningOverlayDefaults(studyAiOverlay = true, openingAiOverlay = true),
      telemetryPreference = TelemetryPreference.ProductAndCalibration
    )
    val stored = writeToTags(Map("dgt" -> "1", "evenchess_old" -> "remove"), preferences)

    assertEquals(stored("dgt"), "1")
    assert(!stored.contains("evenchess_old"))
    assertEquals(fromTags(stored), preferences)

  test("client config exposes display/config values only"):
    val config = default.copy(defaultSetLevel = 7, preferredUsedLevel = 5).clientConfig

    assert(config.valid)
    assertEquals(config.defaultSetLevel, 7)
    assertEquals(config.preferredUsedLevel, 5)
    assertEquals(config.defaultFeatureToggles("offsetCount"), true)
    assertEquals(config.fieldKeys.intersect(fairnessAuthorityKeys), Set.empty)
    assert(!config.exposesFairnessAuthority)
    assert(!config.fieldKeys.contains("providerSecret"))
    assert(!config.fieldKeys.contains("usedLevel"))
    assert(!config.fieldKeys.contains("ecr"))
    assertEquals(config.ttsQueueBehavior, TtsQueueBehavior.default.key)
    assertEquals(config.ttsAutoSpeak, false)
    assertEquals(config.ttsAutoDelaySeconds, 1)
    assertEquals(config.ttsMuteDuringOpponentTurn, true)

  test("server records are per-user and carry only safe client config"):
    val a = ServerRecord("user-a", default.copy(defaultSetLevel = 1))
    val b = ServerRecord("user-b", default.copy(defaultSetLevel = 5))

    assert(a.valid)
    assert(b.valid)
    assertNotEquals(a.userId, b.userId)
    assertEquals(a.safeClientConfig.defaultSetLevel, 1)
    assertEquals(b.safeClientConfig.defaultSetLevel, 5)
