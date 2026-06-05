package lila.evenchess

class EvenChessModeTest extends munit.FunSuite:

  import EvenChessMode.*
  import ProductInvariants.{ RatingEffect, RequirementClass }

  private val validMetadata = ServerOwnedMetadata(
    mode = GameMode.EvenChess,
    rated = true,
    playerModeKey = "normal_rated_evenchess",
    timeControlBucket = TimeControlBucket.Rapid,
    setLevelPolicyVersion = "set-level-v1",
    playerSetLevels = PlayerSetLevels(SetLevel(3), SetLevel(4)),
    assistancePolicyVersion = "assistance-v1",
    ecrPolicyVersion = "ecr-v1",
    auditLedgerVersion = "ledger-v1"
  )

  test("server-owned metadata carries Appendix D mode identity fields"):
    assert(validMetadata.isEvenChess)
    assert(validMetadata.playerSetLevels.allValid)
    assert(validMetadata.hasRequiredPolicyVersions)
    assert(validMetadata.isValid)
    assertEquals(validMetadata.timeControlBucket, TimeControlBucket.Rapid)

  test("client display flags never decide whether a game is EvenChess"):
    val clientClaim = ClientDisplayClaim(claimsEvenChess = true)
    val normalDecision = ModeAuthority.decide(None, clientClaim)
    val evenChessDecision = ModeAuthority.decide(Some(validMetadata), ClientDisplayClaim(claimsEvenChess = false))

    assert(!normalDecision.isEvenChess)
    assert(normalDecision.clientClaimedEvenChess)
    assert(!normalDecision.clientFlagAcceptedAsAuthority)
    assert(!normalDecision.displayAsAssisted)
    assert(!normalDecision.mayRenderEvenChessOverlays)
    assert(!normalDecision.mayUseEcrSystems)
    assert(!normalDecision.mayConsumeEvenChessTokens)
    assert(!normalDecision.mayWriteAssistanceLogs)

    assert(evenChessDecision.isEvenChess)
    assert(!evenChessDecision.clientClaimedEvenChess)
    assert(!evenChessDecision.clientFlagAcceptedAsAuthority)
    assert(evenChessDecision.displayAsAssisted)
    assert(evenChessDecision.mayRenderEvenChessOverlays)

  test("primary modes preserve normal chess and reserve future classroom"):
    assertEquals(
      PlayerModes.byKey("normal_rated_evenchess").ratingEffect,
      RatingEffect.UpdatesEcrAfterUsedOffset
    )
    assertEquals(
      PlayerModes.byKey("target_level").ratingEffect,
      RatingEffect.NoNormalEcrUpdate
    )
    assertEquals(
      PlayerModes.byKey("normal_lichess_chess").classification,
      RequirementClass.LichessProvided
    )
    assertEquals(
      PlayerModes.byKey("future_classroom_coach").classification,
      RequirementClass.UnresolvedProductOwnerDecision
    )
    assertEquals(PlayerModes.byKey("future_classroom_coach").inclusion, ModeInclusion.Reserved)

  test("disclosure and routing requirements are recorded"):
    val disclosureIds = Disclosures.all.map(_.id).toSet

    assert(disclosureIds.contains("MODE-L1-010"))
    assert(disclosureIds.contains("MODE-L1-011"))
    assert(disclosureIds.contains("MODE-L1-013"))
    assert(disclosureIds.contains("MODE-L1-020"))
    assert(Disclosures.requiresSearchWideningConfirmation(changesMaterialLevelContract = true))
    assert(!Disclosures.requiresSearchWideningConfirmation(changesMaterialLevelContract = false))
    assertEquals(Routing.bucketFor(GameEntryPoint.OnlineSearchChallenge), RouteBucket.LiveGames)
    assertEquals(Routing.bucketFor(GameEntryPoint.Computer), RouteBucket.AiGames)
    assertEquals(Routing.bucketFor(GameEntryPoint.Completed), RouteBucket.Review)
    assert(!Routing.clientMayDecideOnlineLeaveResult)

  test("time-control buckets follow Appendix D boundaries"):
    assertEquals(TimeControlBuckets.fromEstimatedDurationSeconds(179), TimeControlBucket.Bullet)
    assertEquals(TimeControlBuckets.fromEstimatedDurationSeconds(180), TimeControlBucket.Blitz)
    assertEquals(TimeControlBuckets.fromEstimatedDurationSeconds(479), TimeControlBucket.Blitz)
    assertEquals(TimeControlBuckets.fromEstimatedDurationSeconds(480), TimeControlBucket.Rapid)
    assertEquals(TimeControlBuckets.fromEstimatedDurationSeconds(1499), TimeControlBucket.Rapid)
    assertEquals(TimeControlBuckets.fromEstimatedDurationSeconds(1500), TimeControlBucket.Classical)
    assertEquals(
      TimeControlBuckets.resolve(Some(86400), rated = true, correspondence = true),
      TimeControlBucket.Correspondence
    )
    assertEquals(
      TimeControlBuckets.resolve(Some(60), rated = false, correspondence = false),
      TimeControlBucket.Casual
    )

  test("premove and stale assistance timing rules do not charge timely live help"):
    assertEquals(
      AssistanceTiming.chargeFor(shownAfterPremoveCommitment = true, staleOrLate = false),
      AssistanceTimingCharge.NotDecisionAssistance
    )
    assertEquals(
      AssistanceTiming.chargeFor(shownAfterPremoveCommitment = false, staleOrLate = true),
      AssistanceTimingCharge.StaleOrLateNotTimely
    )
    assertEquals(
      AssistanceTiming.chargeFor(shownAfterPremoveCommitment = false, staleOrLate = false),
      AssistanceTimingCharge.TimelyLiveDecisionHelp
    )

  test("Stage 1 remains a harmless mode flag and metadata path only"):
    assert(Stage1ModeFlagOnly.serverOwnedMetadataPathOnly)
    assert(!Stage1ModeFlagOnly.coachingLogicEnabled)
    assert(!Stage1ModeFlagOnly.ratingLogicEnabled)
    assert(!Stage1ModeFlagOnly.tokenLogicEnabled)
    assert(Stage1ModeFlagOnly.requiresLocalBaselineBeforeExpansion)
