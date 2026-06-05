package lila.evenchess

class ForkArchitectureTest extends munit.FunSuite:

  import ForkArchitecture.*
  import ProductInvariants.RequirementClass

  test("records Lichess foundation assumptions as provided capabilities"):
    assertEquals(SourceAssumptions.upstreamRemoteName, "upstream")
    assertEquals(SourceAssumptions.upstreamLilaUrl, "https://github.com/lichess-org/lila.git")
    assert(SourceAssumptions.all.exists(source => source.key == "scalachess" && source.classification == RequirementClass.LichessProvided))
    assert(SourceAssumptions.all.exists(source => source.key == "chessground" && source.classification == RequirementClass.LichessProvided))

  test("fork strategies preserve Appendix B patch-map posture"):
    val docs = ForkStrategies.all.find(_.layer == ForkLayer.RequirementsDocs).get
    val rating = ForkStrategies.all.find(_.layer == ForkLayer.RatingMatchmaking).get
    val engine = ForkStrategies.all.find(_.layer == ForkLayer.EngineAi).get

    assertEquals(docs.patchMapRule, PatchMapRule.NoPatchRisk)
    assertEquals(rating.patchMapRule, PatchMapRule.RecordPairingOrRatingEdits)
    assertEquals(engine.patchMapRule, PatchMapRule.RecordEngineOrAnalysisHooks)

  test("namespace rules identify EvenChess-owned paths and names"):
    assert(NamespaceRules.isEvenChessOwnedPath("modules/evenchess/src/main/ForkArchitecture.scala"))
    assert(NamespaceRules.isEvenChessOwnedPath("ui/evenchess/component.ts"))
    assert(!NamespaceRules.isEvenChessOwnedPath("modules/round/src/main/RoundApi.scala"))
    assert(NamespaceRules.hasEvenChessName("EvenChessGamePolicy"))
    assert(NamespaceRules.hasEvenChessName("ECRPolicy"))
    assert(!NamespaceRules.hasEvenChessName("RoundApi"))

  test("integration decision tree blocks broad normal chess changes"):
    assertEquals(
      IntegrationDecisionTree.decide(IntegrationQuestion(altersNormalChess = true, evenChessSpecific = true)),
      IntegrationOutcome.StopBecauseNormalChessWouldChange
    )
    assertEquals(
      IntegrationDecisionTree.decide(IntegrationQuestion(lichessProvided = true)),
      IntegrationOutcome.UseLichessProvidedCapability
    )
    assertEquals(
      IntegrationDecisionTree.decide(IntegrationQuestion(evenChessSpecific = true)),
      IntegrationOutcome.ImplementInsideEvenChessNamespace
    )

  test("integration decision tree escalates high-risk seams"):
    assertEquals(
      IntegrationDecisionTree.decide(IntegrationQuestion(evenChessSpecific = true, needsLilaLifecycleHook = true)),
      IntegrationOutcome.AddSmallPatchMappedHook
    )
    assertEquals(
      IntegrationDecisionTree.decide(IntegrationQuestion(exposesEngineOrAiTruth = true)),
      IntegrationOutcome.RequireServerSideBoundaryAndPatchMap
    )
    assertEquals(
      IntegrationDecisionTree.decide(IntegrationQuestion(affectsRatingTokensBillingOrMatchmaking = true)),
      IntegrationOutcome.RequireSeparatedFairnessServiceAndPatchMap
    )

  test("sync and branding rules keep fork updateable and distinct"):
    assert(UpstreamSyncRules.keepUpstreamRemote)
    assert(UpstreamSyncRules.reviewPatchMapBeforeSync)
    assert(UpstreamSyncRules.allowedPatchMapRiskValues.contains(MergeRisk.Unknown))
    assert(BrandingRules.retainOpenSourceNotices)
    assert(!BrandingRules.mayImplyOfficialLichessAffiliation)
    assert(BrandingRules.publicNameMustDistinguishForkProduct)

  test("stage 1 architecture output fields match Appendix B"):
    assert(Stage1ArchitectureOutput.requiredSections.contains("local boot status"))
    assert(Stage1ArchitectureOutput.requiredSections.contains("game metadata seam"))
    assert(Stage1ArchitectureOutput.requiredSections.contains("patch map entries"))
    assert(Stage1ArchitectureOutput.requiredSections.contains("go/no-go decision"))
