package lila.evenchess

class AdminBackendSettingsTest extends munit.FunSuite:

  import AdminBackendSettings.*
  import ProductInvariants.RequirementClass

  test("Version 1.2 Phase E requirements are classified before integration"):
    val byRequirement = PhaseERequirementClassifications.all.map(c => c.requirement -> c.classification).toMap

    assertEquals(byRequirement(PhaseERequirement.ExistingAdminSettingsShell), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseERequirement.AuthorizedAdminOnly), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseERequirement.ProviderModelAndKeyStatus), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseERequirement.PerSurfaceAiEnablement), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseERequirement.BotSimulationControls), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseERequirement.EcorCalibrationControls), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseERequirement.SecretEntryMechanismDecision), RequirementClass.UnresolvedProductOwnerDecision)

  test("setting ids are namespaced and cover Phase E backend surfaces"):
    assert(SettingIds.all.forall(_.startsWith(SettingIds.prefix)))
    assert(SettingIds.secretStatusControls.subsetOf(SettingIds.all.toSet))
    assert(SettingIds.incidentControls.subsetOf(SettingIds.all.toSet))
    assert(SettingIds.all.contains(SettingIds.openAiModel))
    assert(SettingIds.all.contains(SettingIds.ttsProvider))
    assert(SettingIds.all.contains(SettingIds.stockfishProfile))
    assert(SettingIds.all.contains(SettingIds.stockfishEquivalentRatingBands))
    assert(SettingIds.all.contains(SettingIds.liveAiEnabled))
    assert(SettingIds.all.contains(SettingIds.overlaysEnabled))
    assert(SettingIds.all.contains(SettingIds.tokensEnabled))
    assert(SettingIds.all.contains(SettingIds.freeMatchTokensEnabled))
    assert(SettingIds.all.contains(SettingIds.freeMatchTokensStartsAt))
    assert(SettingIds.all.contains(SettingIds.freeMatchTokensEndsAt))
    assert(SettingIds.all.contains(SettingIds.campaignKillSwitch))
    assert(SettingIds.all.contains(SettingIds.botSimulationEnabled))
    assert(SettingIds.all.contains(SettingIds.botSimulationBotCount))
    assert(SettingIds.all.contains(SettingIds.ecorPolicyVersion))
    assert(SettingIds.all.contains(SettingIds.ecorGapOffsets))
    assert(SettingIds.all.contains(SettingIds.ecorRatingLevelBands))
    assert(SettingIds.all.contains(SettingIds.ecorSnapshotHistory))
    assert(SettingIds.all.contains(SettingIds.aiDailyCostLimitCents))
    assert(SettingIds.all.contains(SettingIds.auditRetentionDays))
    assert(SettingIds.all.contains(SettingIds.incidentNoRate))

  test("default backend settings are conservative and browser safe"):
    assert(default.valid)
    assertEquals(default.openAi.keyStatus.status, SecretStatus.Missing)
    assertEquals(default.tts.keyStatus.status, SecretStatus.Missing)
    assert(!default.surfaces.live)
    assert(!default.monetisation.payments)
    assert(!default.monetisation.freeMatchTokenWindow.enabled)
    assert(!default.botSimulation.enabled)
    assert(default.botSimulation.valid)
    assertEquals(BotOperations.BotAccountRoster.fromCsv(default.matchmaking.botAccountRoster).size, 1000)
    assertEquals(BotOperations.BotAccountRoster.fromCsv(default.botSimulation.accountRoster).size, 1000)
    assertEquals(default.botSimulation.botCount, 1000)
    assert(default.ecor.valid)
    assert(default.stockfish.equivalentRatingTable.exists(_.valid))
    assertEquals(default.ecor.config.toOption.map(_.offsetValueForLevel(CoachingLadder.Level(10))), Some(190))
    assertEquals(default.stockfish.equivalentRatingTable.toOption.map(_.levelForRating(1500)), Some(CoachingLadder.Level(5)))
    assert(default.campaign.killSwitch)
    assert(default.campaign.paidAcquisitionPaused)
    assert(!default.incident.active)
    assert(default.safeAdminSnapshot.safeForBrowser)

  test("free match token window is disabled by default and active only inside a valid date window"):
    val window =
      FreeMatchTokenWindow(
        enabled = true,
        startsAt = "2026-06-03T00:00:00Z",
        endsAt = "2026-06-04T00:00:00Z"
      )
    val before = FreeMatchTokenWindow.parseMillis("2026-06-02T23:59:59Z").get
    val inside = FreeMatchTokenWindow.parseMillis("2026-06-03T12:00:00Z").get
    val after = FreeMatchTokenWindow.parseMillis("2026-06-04T00:00:00Z").get
    val invalid = window.copy(endsAt = "2026-06-02T00:00:00Z")

    assert(FreeMatchTokenWindow.disabled.valid)
    assert(window.valid)
    assert(!window.activeAt(before))
    assert(window.activeAt(inside))
    assert(!window.activeAt(after))
    assertEquals(window.publicMessageAt(inside), Some("Tokens are temporarily free"))
    assert(!invalid.valid)

  test("key status exposes only configured, missing, or rotated labels"):
    val missing = ProviderKeyStatus(configured = false, rotated = false)
    val configured = ProviderKeyStatus(configured = true, rotated = false)
    val rotated = ProviderKeyStatus(configured = true, rotated = true)

    assertEquals(missing.status, SecretStatus.Missing)
    assertEquals(configured.status, SecretStatus.Configured)
    assertEquals(rotated.status, SecretStatus.Rotated)
    assertEquals(rotated.safeAdminLabel, "Rotated")
    assert(!rotated.exposesRawSecret)

  test("raw-like EvenChess backend values are detected before persistence"):
    val backendModel = SettingIds.openAiModel
    val backendNotice = SettingIds.incidentPublicNotice
    val normalSetting = "site.description"

    assert(AdminBackendSettings.isUnsafeEvenChessBackendValue(backendModel, "sk-test-123456789012345678901234567890123456"))
    assert(AdminBackendSettings.isUnsafeEvenChessBackendValue(backendNotice, "token=abc123"))
    assert(!AdminBackendSettings.isUnsafeEvenChessBackendValue(backendNotice, "Engine assistance degraded; queue delayed"))
    assert(!AdminBackendSettings.isUnsafeEvenChessBackendValue(normalSetting, "sk-test-123456789012345678901234567890123456"))

  test("EvenChess backend logging redacts accidental raw-looking secrets"):
    val secret = "sk-" + "a1" * 32

    assertEquals(safeLogValue(SettingIds.openAiModel, secret), redactedLogValue)
    assertEquals(safeLogValue(SettingIds.ttsProvider, "token=abc123"), redactedLogValue)
    assertEquals(safeLogValue(SettingIds.openAiModel, "gpt-4.1-mini"), "gpt-4.1-mini")
    assertEquals(safeLogValue("normal.lichess.setting", secret), secret)

  test("only explicit incident controls may affect rated fairness"):
    assert(!SettingIds.canAffectRatedFairness(SettingIds.openAiModel))
    assert(!SettingIds.canAffectRatedFairness(SettingIds.paymentsEnabled))
    assert(SettingIds.canAffectRatedFairness(SettingIds.incidentGlobalPause))
    assert(SettingIds.canAffectRatedFairness(SettingIds.incidentLiveCoachingPaused))
    assert(SettingIds.canAffectRatedFairness(SettingIds.incidentEnginePaused))
    assert(SettingIds.canAffectRatedFairness(SettingIds.incidentNoRate))
    assert(default.canChangeRatedFairness(SettingIds.incidentNoRate))

  test("config change audit records are rollbackable and require reasons for fairness controls"):
    val normalChange = ConfigChangeAudit(
      settingId = SettingIds.openAiModel,
      adminId = "admin-1",
      before = "gpt-4.1-mini",
      after = "gpt-4.1",
      reason = "",
      auditId = "audit-1"
    )
    val fairnessChange = normalChange.copy(
      settingId = SettingIds.incidentNoRate,
      before = "false",
      after = "true",
      reason = "engine assistance outage"
    )
    val missingReason = fairnessChange.copy(reason = "")
    val unsafeChange = normalChange.copy(after = "sk-" + "a1" * 32)
    val emptyNoticeRollback = normalChange.copy(
      settingId = SettingIds.incidentPublicNotice,
      before = "",
      after = "Engine assistance degraded; affected games are under review.",
      reason = "public incident notice",
      auditId = "audit-2"
    )

    assert(normalChange.allowed)
    assertEquals(normalChange.rollbackValue, "gpt-4.1-mini")
    assert(fairnessChange.allowed)
    assert(!missingReason.allowed)
    assert(!unsafeChange.allowed)
    assert(emptyNoticeRollback.rollbackable)
    assert(emptyNoticeRollback.allowed)

  test("safety limits reject non-positive rate limits and too-short audit retention"):
    assert(default.limits.valid)
    assert(!default.copy(limits = default.limits.copy(aiRateLimitPerMinute = 0)).valid)
    assert(!default.copy(limits = default.limits.copy(ttsRateLimitPerMinute = 0)).valid)
    assert(!default.copy(limits = default.limits.copy(auditRetentionDays = 7)).valid)
    assert(!default.copy(stockfish = default.stockfish.copy(maxDepth = 0)).valid)
