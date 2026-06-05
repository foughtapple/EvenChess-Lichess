package lila.evenchess

class TargetLevelModeTest extends munit.FunSuite:

  import CoachingLadder.Level
  import EcrRating.{ EcrPool, RatedMode }
  import ProductInvariants.RequirementClass
  import TargetLevelMode.*

  private val contract =
    TargetLevelContract(
      targetLevel = Level(5),
      setLevelPolicyVersion = "target-set-level-v1",
      selectedByPlayerId = "player-1"
    )

  test("Target Level is player-selected practice context, not Rating Level"):
    assert(contract.isPlayerSelectedPracticeContext)
    assert(!contract.isRatingLevel)
    assertEquals(contract.disclosureLabel, "Target Mode L5")

  test("MVP Target games do not mutate normal ECR and may use hidden shadow rating only"):
    val withoutShadow = TargetGamePolicy.mvp(contract)
    val withShadow = TargetGamePolicy.mvp(contract, hiddenTargetShadowRatingEnabled = true)

    assertEquals(withoutShadow.ratedMode, RatedMode.TargetLevelMvp)
    assert(!withoutShadow.normalEcrChanged)
    assert(!withoutShadow.normalEcrLeaderboardEligible)
    assertEquals(withoutShadow.targetShadowPool, None)
    assertEquals(withShadow.targetShadowPool, Some(EcrPool.TargetShadow))
    assert(withShadow.validForMvp)

  test("Target Mode is separated from the Normal EvenChess queue"):
    assert(TargetQueuePolicy.canPair(TargetQueue.TargetLevel, TargetQueue.TargetLevel))
    assert(!TargetQueuePolicy.canPair(TargetQueue.TargetLevel, TargetQueue.NormalEvenChess))
    assert(!TargetQueuePolicy.canPair(TargetQueue.NormalEvenChess, TargetQueue.TargetLevel))
    assert(TargetGamePolicy.mvp(contract).usesSeparateTargetQueue)
    assert(!TargetGamePolicy.mvp(contract).maySearchNormalEvenChessQueue)

  test("adjacent Target Level widening requires explicit UI confirmation"):
    val same = TargetLevelWidening(Level(5), Level(5), uiConfirmed = false)
    val adjacentUnconfirmed = TargetLevelWidening(Level(5), Level(6), uiConfirmed = false)
    val adjacentConfirmed = TargetLevelWidening(Level(5), Level(6), uiConfirmed = true)
    val nonAdjacent = TargetLevelWidening(Level(5), Level(7), uiConfirmed = true)

    assert(same.allowed)
    assert(!same.requiresConfirmation)
    assert(adjacentUnconfirmed.requiresConfirmation)
    assert(!adjacentUnconfirmed.allowed)
    assert(adjacentConfirmed.allowed)
    assert(!nonAdjacent.allowed)

  test("Target Mode labels must be present in lobby, header, result, and review"):
    assert(TargetDisclosure.hasRequiredSurfaces(TargetDisclosure.requiredSurfaces))
    assert(!TargetDisclosure.hasRequiredSurfaces(Set(TargetLabelSurface.Lobby, TargetLabelSurface.Header)))

  test("Target summaries are reviewable but excluded from normal performance windows"):
    assert(TargetSummaryPolicy.mvp.validForMvp)
    assert(TargetSummaryPolicy.mvp.reviewable)
    assert(!TargetSummaryPolicy.mvp.includedInNormalPerformanceWindow)
    assert(!TargetSummaryPolicy.mvp.mutatesNormalEcr)

  test("token and subscription policy cannot affect fairness while product decision is open"):
    assert(TargetResourcePolicy.tokenConsumptionNeedsProductOwnerDecision)
    assert(!TargetResourcePolicy.tokensMayAffectFairness)
    assert(!TargetResourcePolicy.subscriptionsMayAffectFairness)

  test("Appendix K requirements are classified before implementation"):
    val byRequirement =
      TargetRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(
      byRequirement(TargetRequirement.PlayerSelectedPracticeContext),
      RequirementClass.EvenChessSpecific
    )
    assertEquals(
      byRequirement(TargetRequirement.SeparateQueue),
      RequirementClass.AdaptedToLichessFork
    )
    assertEquals(
      byRequirement(TargetRequirement.TokenPolicyUnresolved),
      RequirementClass.UnresolvedProductOwnerDecision
    )
