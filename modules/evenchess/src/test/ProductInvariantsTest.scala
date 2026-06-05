package lila.evenchess

class ProductInvariantsTest extends munit.FunSuite:

  import ProductInvariants.*

  test("classifies Appendix A vocabulary without rebuilding Lichess basics"):
    assertEquals(Vocabulary.byName("normal chess").classification, RequirementClass.LichessProvided)
    assertEquals(Vocabulary.byName("evenchess").classification, RequirementClass.EvenChessSpecific)
    assertEquals(Vocabulary.byName("offset count").classification, RequirementClass.AdaptedToLichessFork)

  test("public copy guardrail rejects forbidden positioning"):
    assertEquals(PublicPositioning.forbiddenPhraseHits("Secret engine use for normal chess with help"), List("secret engine use", "normal chess with help"))
    assert(PublicPositioning.isAllowedCopy(PublicPositioning.canonicalClaim))

  test("relationship rules preserve Appendix A distinctions"):
    assert(RelationshipRules.setLevelIsPermission)
    assert(RelationshipRules.usedLevelIsActualUse)
    assert(RelationshipRules.usedLevelNeverDecreases)
    assert(!RelationshipRules.targetLevelIsRatingLevel)
    assert(!RelationshipRules.aiTextIsChessAuthority)
    assert(!RelationshipRules.normalLichessBasicsAreEvenChessSpecific)

  test("player modes keep normal chess and target practice isolated"):
    assertEquals(PlayerModes.byKey("normal_evenchess").ratingEffect, RatingEffect.UpdatesEcrAfterUsedOffset)
    assertEquals(PlayerModes.byKey("target_level").ratingEffect, RatingEffect.NoNormalEcrUpdate)
    assertEquals(PlayerModes.byKey("normal_lichess_chess").classification, RequirementClass.LichessProvided)

  test("marketing and account invariants do not allow paid strength"):
    assertEquals(MarketingAccountInvariants.onboardingGameTokens, 10)
    assertEquals(MarketingAccountInvariants.onboardingMatchSummaryTokens, 3)
    assertEquals(MarketingAccountInvariants.performanceSummaryUnlockCompletedGames, 10)
    assertEquals(MarketingAccountInvariants.maxEarnedAdGameTokens, 3)
    assert(!MarketingAccountInvariants.standardMayChangeRatedLiveStrength)
    assert(!MarketingAccountInvariants.premiumMayChangeRatedLiveStrength)
    assert(!MarketingAccountInvariants.premiumAddsStrongerLiveCoaching)

  test("offset count display semantics match Appendix A"):
    assertEquals(OffsetCountDisplay.EqualTrade.color, "blue")
    assertEquals(OffsetCountDisplay.EqualTrade.icon, "shield")
    assertEquals(OffsetCountDisplay.StudentWinsMaterial.color, "green")
    assertEquals(OffsetCountDisplay.OpponentWinsMaterial.color, "red")
