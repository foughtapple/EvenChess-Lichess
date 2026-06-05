package lila.evenchess

class TestingQaAcceptanceTest extends munit.FunSuite:

  import ProductInvariants.RequirementClass
  import TestingQaAcceptance.*

  private val baseline =
    BaselineLichessRegression(
      localLichessBoots = true,
      legalMovesWork = true,
      clocksWork = true,
      resultFlowWorks = true,
      reviewHistoryWorks = true,
      accountSessionWorks = true,
      upstreamLilaFilesTouched = false,
      normalRegressionRunForUpstreamTouches = false
    )

  private val crossCutting =
    CrossCuttingGateReport(
      CrossCuttingGate.values.toList.map: gate =>
        GateEvidence(gate, passed = true, evidence = s"$gate passed")
    )

  private val stage1 =
    Stage1AcceptanceEvidence(
      localLilaBoots = true,
      accountsWork = true,
      localGamesWork = true,
      normalChessBaselineRemains = true,
      evenChessBoundaryExists = true,
      harmlessModeFlagDisplays = true,
      dummyServerAuthorizedOverlayWorksWithoutAdvice = true,
      dummyAuditEventWrites = true,
      aiMockExistsIfS18Completed = true,
      patchMapUpdated = true,
      goNoGoReportExists = true
    )

  private val rated =
    RatedEvenChessAcceptanceEvidence(
      serverPolicyImplemented = true,
      l0L10GatesPass = true,
      assistanceUsedOffsetReplayPasses = true,
      ecrIsolatedFromNormalRatings = true,
      targetIsolationPasses = true,
      offsetCountTestsPass = true,
      engineSecurityPasses = true,
      aiValidatorsPass = true,
      clientCannotBypassPermission = true,
      subscriptionsAdsTokensDoNotAffectFairness = true,
      calibrationDashboardsPresent = true,
      noHiddenContradictions = true
    )

  private val marketingOps =
    MarketingOpsAcceptanceEvidence(
      tokensCorrect = true,
      adCapCorrect = true,
      consumptionRefundCorrect = true,
      pricingDisplayCorrect = true,
      premiumNonStrength = true,
      backendLandingConfigWorks = true,
      utmEventsPresent = true,
      killSwitchesWork = true,
      unsafeCopyScanPasses = true,
      eventDedupeWorks = true,
      engineAiHealthVisible = true,
      fallbackDegradedStatesWork = true,
      staleClearingWorks = true,
      noRateAnnulPathWorks = true,
      rollbackWorks = true,
      incidentPlaybookExists = true,
      patchMapCurrent = true
    )

  private val finalAcceptance =
    FinalAcceptanceEvidence(
      allAppendicesCurrent = true,
      appendixZCurrent = true,
      patchMapCurrent = true,
      upstreamSyncDocumented = true,
      fullRegressionPasses = true,
      noContradictionUnreported = true,
      normalChessSeparate = true,
      evenChessInvariantsPass = true,
      goNoGoApproved = true
    )

  private val phaseSGates =
    PhaseSAcceptanceGateEvidence(
      testsRunOrDocumentedReason = true,
      patchMapOrIntegrationLogCurrent = true,
      noUnreportedInvariantConflicts = true,
      noAccidentalNormalRatedPoolUse = true,
      noClientSideCoachingPermissionDecision = true,
      noApiKeysOrSecretsExposed = true,
      desktopMobileBoardLayoutChecked = true
    )

  private val phaseSEvidence =
    PhaseSRegressionSurface.values.toList.map: surface =>
      PhaseSRegressionEvidence(
        surface = surface,
        passed = true,
        evidence = s"$surface regression evidence",
        commandOrReason = "evenchess/testOnly lila.evenchess.TestingQaAcceptanceTest",
        rollbackNote = "Remove Phase S regression-hardening evidence only; no upstream Lichess file changed."
      )

  test("Appendix V requirements are classified before implementation"):
    val byRequirement =
      TestingRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(TestingRequirement.LocalLichessBootBeforeFeatures), RequirementClass.LichessProvided)
    assertEquals(byRequirement(TestingRequirement.NormalChessRegression), RequirementClass.LichessProvided)
    assertEquals(byRequirement(TestingRequirement.AccountSessionRegression), RequirementClass.LichessProvided)
    assertEquals(byRequirement(TestingRequirement.UpstreamTouchesNeedNormalRegression), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(TestingRequirement.CrossCuttingGates), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(TestingRequirement.FinalAcceptance), RequirementClass.AdaptedToLichessFork)

  test("Version 2 Phase S regression hardening requirements are classified before release-candidate work"):
    val byRequirement =
      PhaseSRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseSRequirement.NativeSetupSearchOpens), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseSRequirement.SearchCreatesEvenChessContract), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseSRequirement.NormalLichessMechanicsPreserved), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseSRequirement.EcrMmrSeparateFromNormalRatings), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseSRequirement.LevelGatedPayloads), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseSRequirement.DisplayEngineRejectsStalePayloads), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseSRequirement.AcceptanceGateBundle), RequirementClass.AdaptedToLichessFork)

  test("baseline Lichess regression requires boot, normal game flow, account sessions, and checks after upstream touches"):
    assert(baseline.normalChessSafe)
    assert(!baseline.copy(localLichessBoots = false).normalChessSafe)
    assert(!baseline.copy(legalMovesWork = false).normalChessSafe)
    assert(!baseline.copy(accountSessionWorks = false).normalChessSafe)
    assert(!baseline.copy(upstreamLilaFilesTouched = true, normalRegressionRunForUpstreamTouches = false).normalChessSafe)
    assert(baseline.copy(upstreamLilaFilesTouched = true, normalRegressionRunForUpstreamTouches = true).normalChessSafe)

  test("cross-cutting gate report requires every Appendix V gate with evidence"):
    assert(crossCutting.allRequiredPassed)
    assert(crossCutting.passed(CrossCuttingGate.InvariantTests))
    assert(crossCutting.passed(CrossCuttingGate.EveryRenderAudited))
    assert(crossCutting.passed(CrossCuttingGate.PatchMapCompleteness))

    val missingGate = CrossCuttingGateReport(crossCutting.evidence.filterNot(_.gate == CrossCuttingGate.EcrReplay))
    val missingEvidence = CrossCuttingGateReport(
      crossCutting.evidence.map {
        case item if item.gate == CrossCuttingGate.ServerAuthority => item.copy(evidence = "")
        case item                                                  => item
      }
    )

    assert(!missingGate.allRequiredPassed)
    assert(!missingEvidence.allRequiredPassed)

  test("Stage 1 acceptance requires local baseline, boundary, harmless flag, dummy overlay, dummy audit, patch map, and go/no-go"):
    assert(stage1.accepted)
    assert(!stage1.copy(localLilaBoots = false).accepted)
    assert(!stage1.copy(normalChessBaselineRemains = false).accepted)
    assert(!stage1.copy(dummyServerAuthorizedOverlayWorksWithoutAdvice = false).accepted)
    assert(!stage1.copy(dummyAuditEventWrites = false).accepted)
    assert(!stage1.copy(goNoGoReportExists = false).accepted)

  test("rated EvenChess acceptance requires fairness, security, replay, isolation, and monetisation non-effect gates"):
    assert(rated.accepted)
    assert(!rated.copy(serverPolicyImplemented = false).accepted)
    assert(!rated.copy(l0L10GatesPass = false).accepted)
    assert(!rated.copy(ecrIsolatedFromNormalRatings = false).accepted)
    assert(!rated.copy(clientCannotBypassPermission = false).accepted)
    assert(!rated.copy(subscriptionsAdsTokensDoNotAffectFairness = false).accepted)
    assert(!rated.copy(noHiddenContradictions = false).accepted)

  test("marketing and ops acceptance covers token, ad, copy, attribution, health, rollback, incident, and patch-map gates"):
    assert(marketingOps.accepted)
    assert(!marketingOps.copy(adCapCorrect = false).accepted)
    assert(!marketingOps.copy(premiumNonStrength = false).accepted)
    assert(!marketingOps.copy(unsafeCopyScanPasses = false).accepted)
    assert(!marketingOps.copy(fallbackDegradedStatesWork = false).accepted)
    assert(!marketingOps.copy(noRateAnnulPathWorks = false).accepted)
    assert(!marketingOps.copy(patchMapCurrent = false).accepted)

  test("final acceptance requires current appendices, patch map, upstream sync, regression, separation, invariants, and go/no-go"):
    assert(finalAcceptance.accepted)
    assert(!finalAcceptance.copy(allAppendicesCurrent = false).accepted)
    assert(!finalAcceptance.copy(appendixZCurrent = false).accepted)
    assert(!finalAcceptance.copy(upstreamSyncDocumented = false).accepted)
    assert(!finalAcceptance.copy(fullRegressionPasses = false).accepted)
    assert(!finalAcceptance.copy(normalChessSeparate = false).accepted)
    assert(!finalAcceptance.copy(evenChessInvariantsPass = false).accepted)

  test("release readiness composes baseline, cross-cutting, Stage 1, rated, marketing ops, and final acceptance"):
    val readiness = ReleaseReadiness(baseline, crossCutting, stage1, rated, marketingOps, finalAcceptance)

    assert(readiness.stage1Ready)
    assert(readiness.ratedReady)
    assert(readiness.releaseReady)
    assert(!readiness.copy(stage1 = stage1.copy(goNoGoReportExists = false)).stage1Ready)
    assert(!readiness.copy(crossCutting = CrossCuttingGateReport(Nil)).ratedReady)
    assert(!readiness.copy(marketingOps = marketingOps.copy(rollbackWorks = false)).releaseReady)

  test("QA policy forbids skipping boot, tests, contradictions, and normal chess separation"):
    assert(QaPolicy.localBootRequiredBeforeEvenChessFeatures)
    assert(QaPolicy.normalChessRegressionRequiredForUpstreamTouches)
    assert(QaPolicy.testsRequiredUnlessDocumentationOnly)
    assert(!QaPolicy.hiddenContradictionsAllowed)
    assert(!QaPolicy.normalChessMayBeReplaced)

  test("Phase S regression hardening requires every V2 core surface with evidence command and rollback note"):
    val report = PhaseSRegressionHardeningReport(phaseSEvidence, phaseSGates)
    val missingOverlay = report.copy(
      evidence = phaseSEvidence.filterNot(_.surface == PhaseSRegressionSurface.OverlayVisuals)
    )
    val missingRollback = report.copy(
      evidence = phaseSEvidence.map {
        case item if item.surface == PhaseSRegressionSurface.FullGameEceSettlementNeutral => item.copy(rollbackNote = "")
        case item                                                                        => item
      }
    )

    assert(report.coversEveryCoreSurface)
    assert(report.accepted)
    assert(report.passed(PhaseSRegressionSurface.NativeSetupSearch))
    assert(report.passed(PhaseSRegressionSurface.NormalLichessMechanics))
    assert(report.passed(PhaseSRegressionSurface.FullGameEceSettlementNeutral))
    assert(!missingOverlay.coversEveryCoreSurface)
    assertEquals(missingOverlay.missingSurfaces, Set(PhaseSRegressionSurface.OverlayVisuals))
    assert(!missingRollback.accepted)

  test("Phase S acceptance gates block undocumented tests pool misuse client authority secrets and layout regressions"):
    val report = PhaseSRegressionHardeningReport(phaseSEvidence, phaseSGates)

    assert(report.accepted)
    assert(!report.copy(gates = phaseSGates.copy(testsRunOrDocumentedReason = false)).accepted)
    assert(!report.copy(gates = phaseSGates.copy(patchMapOrIntegrationLogCurrent = false)).accepted)
    assert(!report.copy(gates = phaseSGates.copy(noAccidentalNormalRatedPoolUse = false)).accepted)
    assert(!report.copy(gates = phaseSGates.copy(noClientSideCoachingPermissionDecision = false)).accepted)
    assert(!report.copy(gates = phaseSGates.copy(noApiKeysOrSecretsExposed = false)).accepted)
    assert(!report.copy(gates = phaseSGates.copy(desktopMobileBoardLayoutChecked = false)).accepted)
