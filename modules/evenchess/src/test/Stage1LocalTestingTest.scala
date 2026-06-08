package lila.evenchess

class Stage1LocalTestingTest extends munit.FunSuite:

  import ProductInvariants.RequirementClass
  import Stage1LocalTesting.*

  private val passingEvidence =
    SmokeTest.values.toList.map: test =>
      SmokeTestEvidence(test, passed = true, evidence = s"$test passed locally")

  test("Appendix S requirements are classified before implementation"):
    val byRequirement =
      Stage1RequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(Stage1Requirement.ArchitectureInspection), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(Stage1Requirement.LocalLichessBoot), RequirementClass.LichessProvided)
    assertEquals(byRequirement(Stage1Requirement.EvenChessModuleBoundary), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(Stage1Requirement.ModeFlagOnly), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(Stage1Requirement.DummyServerAuthorisedOverlay), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(Stage1Requirement.StopRules), RequirementClass.AdaptedToLichessFork)

  test("Stage 1 phase registry preserves Appendix S order and deliverables"):
    assert(Stage1Plan.ordered)
    assertEquals(Stage1Plan.phases.size, 9)
    assertEquals(Stage1Plan.byId(Stage1PhaseId.ArchitectureInspection).deliverable, "docs/evenchess/stage1_architecture_inspection.md")
    assertEquals(Stage1Plan.byId(Stage1PhaseId.GoNoGoReport).deliverable, "docs/evenchess/stage1_go_no_go.md")
    assert(Stage1Plan.byId(Stage1PhaseId.ModuleBoundary).behaviorImpact == BehaviorImpact.NoGameBehaviorChange)

  test("core-touching Stage 1 phases declare patch-map need"):
    val coreTouching = Stage1Plan.phases.filter(_.mayTouchCoreLichess)
    val isolated = Stage1Plan.phases.filterNot(_.mayTouchCoreLichess)

    assert(coreTouching.exists(_.id == Stage1PhaseId.ModeFlagOnly))
    assert(coreTouching.forall(_.requiresPatchMapIfImplemented))
    assert(isolated.forall(!_.requiresPatchMapIfImplemented))

  test("Stage 1 smoke report requires local Lichess and module baseline evidence"):
    val report = Stage1SmokeReport(passingEvidence)
    val missingSite = Stage1SmokeReport(passingEvidence.filterNot(_.test == SmokeTest.SiteLoads))
    val missingEvidence = Stage1SmokeReport(
      passingEvidence.map {
        case item if item.test == SmokeTest.HumanGameStarts => item.copy(evidence = "")
        case item                                           => item
      }
    )

    assert(report.requiredBaselinePassed)
    assert(!missingSite.requiredBaselinePassed)
    assert(!missingEvidence.requiredBaselinePassed)
    assert(report.passed(SmokeTest.PatchMapCurrent))

  test("Appendix S stop rules block unsafe implementation attempts"):
    val safe = Stage1ChangeProposal(
      localLilaBoots = true,
      normalChessStillWorks = true,
      evenChessCanRemainIsolated = true,
      broadCoreRewriteRequired = false,
      requirementConflict = false,
      attemptsLiveEngineAiCoaching = false,
      ledgerFoundationComplete = false
    )
    val unsafeEngine = safe.copy(attemptsLiveEngineAiCoaching = true, ledgerFoundationComplete = false)
    val unsafeBoot = safe.copy(localLilaBoots = false)
    val unsafeRewrite = safe.copy(broadCoreRewriteRequired = true)

    assert(safe.mayProceed)
    assert(unsafeEngine.stopReasons.contains(StopReason.LiveEngineAiCoachingBeforeLedgerFoundation))
    assert(unsafeBoot.stopReasons.contains(StopReason.LocalLilaDoesNotBoot))
    assert(unsafeRewrite.stopReasons.contains(StopReason.BroadCoreRewriteRequired))
    assert(!unsafeEngine.mayProceed)

  test("go/no-go requires all Stage 1 evidence categories"):
    val go = Stage1GoNoGoEvidence(
      smokeReport = Stage1SmokeReport(passingEvidence),
      architectureInspectionRecorded = true,
      moduleBoundaryCreated = true,
      modeFlagSafe = true,
      overlayServerAuthorised = true,
      ledgerAppendOnly = true,
      aiProviderMockFirst = true,
      patchMapCurrent = true,
      risksRecorded = true
    )

    assert(go.go)
    assert(!go.copy(modeFlagSafe = false).go)
    assert(!go.copy(smokeReport = Stage1SmokeReport(passingEvidence.filterNot(_.test == SmokeTest.ClocksWork))).go)

  test("local deployment path forbids full product before Stage 1 go"):
    assert(!LocalDeploymentPath.deployableBeforeStage1Go)
    assert(LocalDeploymentPath.normalLichessBaselineRequired)
    assert(!LocalDeploymentPath.fullProductImplementationBeforeLocalBaseline)
    assert(!LocalDeploymentPath.destructiveLocalCommandsAllowedByDefault)
