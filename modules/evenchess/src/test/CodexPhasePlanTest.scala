package lila.evenchess

class CodexPhasePlanTest extends munit.FunSuite:

  import CodexPhasePlan.*
  import ProductInvariants.RequirementClass

  private val completeExecution =
    PhaseExecution(
      requestedPacket = Stage1PacketId.S1_3,
      attemptedPackets = Set(Stage1PacketId.S1_3),
      readDocuments = RequiredReading.baseline,
      documentationOnly = false,
      testsAddedOrUpdated = true,
      upstreamFilesTouched = false,
      patchMapUpdated = false,
      completionReportFields = CompletionReportTemplate.requiredFields
    )

  test("Appendix T requirements are classified before implementation"):
    val byRequirement =
      CodexRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(CodexRequirement.RequestedPhaseOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(CodexRequirement.RequiredReading), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(CodexRequirement.TestsUnlessDocumentationOnly), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(CodexRequirement.PatchMapForUpstreamTouches), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(CodexRequirement.PostStage1PhaseFamilies), RequirementClass.EvenChessSpecific)

  test("required reading includes main, Stage 1 handover, relevant appendix, diff, patch map, sync, and AGENTS"):
    assert(RequiredReading.complete(RequiredReading.baseline))
    assert(!RequiredReading.complete(RequiredReading.baseline - RequiredDocument.RelevantAppendix))
    assert(!RequiredReading.complete(RequiredReading.baseline - RequiredDocument.PatchMap))

  test("Stage 1 task packets preserve Appendix T order, prompts, and no-product-feature boundaries"):
    assert(Stage1TaskPackets.ordered)
    assertEquals(Stage1TaskPackets.all.size, 9)
    assert(Stage1TaskPackets.all.forall(_.valid))

    val architecture = Stage1TaskPackets.byId(Stage1PacketId.S1_1).get
    val modeFlag = Stage1TaskPackets.byId(Stage1PacketId.S1_4).get
    val goNoGo = Stage1TaskPackets.byId(Stage1PacketId.S1_9).get

    assert(architecture.promptStarter.contains("No product features"))
    assert(!architecture.allowsProductFeatures)
    assert(modeFlag.promptStarter.contains("No coaching/rating/matchmaking/tokens"))
    assert(modeFlag.allowsProductFeatures)
    assert(goNoGo.promptStarter.contains("stage1_go_no_go.md"))
    assert(Stage1TaskPackets.all.forall(_.requiresPatchMapForCoreTouches))

  test("post-Stage 1 phase families match Appendix T appendix routing without authorizing early implementation"):
    assert(PostStage1PhaseFamilies.coversAllFamilies)
    assertEquals(PostStage1PhaseFamilies.appendicesFor(PostStage1PhaseId.P2), Set("Main", "B", "Z"))
    assertEquals(PostStage1PhaseFamilies.appendicesFor(PostStage1PhaseId.P7), Set("L", "G", "V"))
    assertEquals(PostStage1PhaseFamilies.appendicesFor(PostStage1PhaseId.P14), Set("V", "R", "Z"))
    assert(!PhaseSafety.futurePhaseFamiliesAuthorizeImplementationNow)
    assert(PhaseSafety.oldAZPlanSuperseded)
    assert(!PhaseSafety.broadUnrequestedWorkAllowed)

  test("phase execution is complete only for the requested packet with reading, tests, patch-map rules, and report fields"):
    assert(completeExecution.complete)
    assert(!completeExecution.copy(attemptedPackets = Set(Stage1PacketId.S1_3, Stage1PacketId.S1_4)).complete)
    assert(!completeExecution.copy(readDocuments = RequiredReading.baseline - RequiredDocument.Stage1Handover).complete)
    assert(!completeExecution.copy(testsAddedOrUpdated = false, documentationOnly = false).complete)
    assert(completeExecution.copy(testsAddedOrUpdated = false, documentationOnly = true).complete)
    assert(!completeExecution.copy(upstreamFilesTouched = true, patchMapUpdated = false).complete)
    assert(completeExecution.copy(upstreamFilesTouched = true, patchMapUpdated = true).complete)

  test("completion report template requires every Appendix T field"):
    assert(CompletionReportTemplate.complete(CompletionReportTemplate.requiredFields))
    assert(!CompletionReportTemplate.complete(CompletionReportTemplate.requiredFields - CompletionReportField.InvariantChecks))
    assert(!CompletionReportTemplate.complete(CompletionReportTemplate.requiredFields - CompletionReportField.ReadyForNextPhase))
