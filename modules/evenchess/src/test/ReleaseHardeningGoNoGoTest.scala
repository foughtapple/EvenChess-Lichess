package lila.evenchess

class ReleaseHardeningGoNoGoTest extends munit.FunSuite:

  import AdminOperations.{ FeatureFlag, GameOperationalVersions, IncidentPlaybooks, OpsHealthSnapshot, OpsMetric, OpsSignal }
  import MarketingAttributionFunnel.PaidLaunchGateResult
  import ProductInvariants.RequirementClass
  import ReleaseHardeningGoNoGo.*
  import TestingQaAcceptance.*
  import TrustOpsIncidentControls.{ OpsReadinessEvidence, TrustOpsAcceptanceEvidence }

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
    CrossCuttingGateReport(CrossCuttingGate.values.toList.map(gate => GateEvidence(gate, passed = true, evidence = s"$gate passed")))

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

  private val opsReadiness =
    OpsReadinessEvidence(
      healthSnapshot = OpsHealthSnapshot(OpsSignal.values.toList.map(signal => OpsMetric(signal, value = 1, unit = "count", healthy = true))),
      dashboardsComplete = true,
      activeVersions = GameOperationalVersions(
        gameId = "game-1",
        policyVersion = "policy-v1",
        modelVersion = "model-v1",
        configVersion = "config-v1",
        engineVersion = "stockfish-16.1",
        featureFlagVersions = Map("l7-candidates" -> "flags-v1")
      ),
      featureFlags = List(
        FeatureFlag(
          key = "l8-candidate-depth",
          risky = true,
          enabled = true,
          configVersion = "flags-v1",
          rollbackVersion = Some("flags-v0"),
          owner = "ops",
          auditId = "audit-flags"
        )
      ),
      incidentPlaybooksComplete = IncidentPlaybooks.coversAllKnownIncidents
    )

  private val trustOps =
    TrustOpsAcceptanceEvidence(
      exploitRegisterComplete = true,
      nonPlatformGuidanceEscalates = true,
      copyScansPass = true,
      repeatCollusionSimulationsPass = true,
      candidateBudgetsAuditPass = true,
      promptInjectionBlocked = true,
      staleAndDesyncHandled = true,
      tokenAbuseHandled = true,
      noRateAnnulAuditPass = true,
      mobileDisclosurePass = true,
      rollbackWorks = true,
      incidentPlaybooksExist = true,
      patchMapCurrent = true,
      opsReadiness = opsReadiness
    )

  private val remainingDecisions =
    Version11DecisionRegister.remainingDecisionIds.toList.sorted.map: id =>
      ProductDecisionStatus(
        decisionId = id,
        disposition = DecisionDisposition.ExplicitlyDeferred,
        owner = "product-owner",
        notes = s"$id deferred with launch-owner tracking"
      )

  private val documents =
    ReleaseDocumentStatus(
      allAppendicesCurrent = true,
      appendixZCurrent = true,
      patchMapCurrent = true,
      integrationLogCurrent = true,
      upstreamSyncDocumented = true,
      noContradictionUnreported = true,
      noUnmappedCoreEdits = true,
      mediumHighPatchRisksReviewed = true,
      remainingDecisions = remainingDecisions
    )

  private val regression =
    FullRegressionStatus(
      localLilaBootVerified = true,
      normalChessRegressionPassed = true,
      evenChessRegressionPassed = true,
      rootCompilePassed = true,
      diffHygienePassed = true,
      noRequiredTestFailures = true,
      upstreamLilaFilesTouched = false,
      normalRegressionRunForUpstreamTouches = false,
      commandsRun = List("evenchess/test", "evenchess/compile", "compile", "git diff --check"),
      failingChecks = Nil
    )

  private val separation =
    NormalChessSeparationEvidence(
      legalMoveGenerationDelegatedToLichess = true,
      boardAndClockInternalsPreserved = true,
      normalRatingsNotUsedAsEcr = true,
      normalChessInternalsAvailableForRegression = true,
      publicStartFlowsEvenChessOnly = true,
      noNormalChessDeletion = true
    )

  private val paidLaunch =
    PaidLaunchGateResult(allowed = true, blockedReasons = Nil, pausedSurfaces = Set.empty)

  private val approval =
    ReleaseApproval(
      productOwnerApproved = true,
      engineeringApproved = true,
      securityPrivacyApproved = true,
      approvalId = "approval-1",
      approvedAt = 123456789L
    )

  private val phaseSRegression =
    PhaseSRegressionHardeningReport(
      evidence = PhaseSRegressionSurface.values.toList.map: surface =>
        PhaseSRegressionEvidence(
          surface = surface,
          passed = true,
          evidence = s"$surface regression evidence",
          commandOrReason = "evenchess/test",
          rollbackNote = "Remove Phase S evidence only; no upstream Lichess file changed."
        ),
      gates = PhaseSAcceptanceGateEvidence(
        testsRunOrDocumentedReason = true,
        patchMapOrIntegrationLogCurrent = true,
        noUnreportedInvariantConflicts = true,
        noAccidentalNormalRatedPoolUse = true,
        noClientSideCoachingPermissionDecision = true,
        noApiKeysOrSecretsExposed = true,
        desktopMobileBoardLayoutChecked = true
      )
    )

  private val upstreamSync =
    UpstreamSyncReleaseEvidence(
      upstreamSyncAttempted = false,
      workingTreeCleanBeforeSync = false,
      patchMapCurrentBeforeSync = false,
      regressionRunAfterSync = false,
      highRiskAreasTouched = Set.empty,
      highRiskApprovalId = ""
    )

  private val candidateStamp =
    ReleaseCandidateStamp(
      candidateId = "rc-v2-1",
      version = "2.0.0-rc.1",
      sourceRef = "phase-t-local",
      requirementsVersion = "EvenChess-Lichess V2",
      createdAt = 123456789L,
      rollbackNote = "Remove the Phase T release-candidate wrapper; no upstream Lichess file changed.",
      testSummary = "Full release evidence accepted with Java test caveat documented separately."
    )

  private def bundle =
    ReleaseEvidenceBundle(
      baseline = baseline,
      crossCutting = crossCutting,
      stage1 = stage1,
      rated = rated,
      marketingOps = marketingOps,
      trustOps = trustOps,
      finalAcceptance = finalAcceptance,
      documents = documents,
      regression = regression,
      normalChessSeparation = separation,
      paidLaunch = paidLaunch,
      approval = approval
    )

  private def candidate =
    ReleaseCandidateEvidence(
      stamp = candidateStamp,
      bundle = bundle,
      phaseSRegression = phaseSRegression,
      upstreamSync = upstreamSync,
      patchMapCurrent = true,
      integrationLogCurrent = true,
      noFuturePhaseScopeImplemented = true,
      completionReportReady = true
    )

  test("Version 1.1 Phase L requirements are classified before implementation"):
    val byRequirement =
      PhaseLRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseLRequirement.BaselineLichessRegression), RequirementClass.LichessProvided)
    assertEquals(byRequirement(PhaseLRequirement.FullEvenChessRegression), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseLRequirement.CrossCuttingAcceptanceGates), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseLRequirement.AppendixZAndDecisionRegister), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseLRequirement.PatchMapAndIntegrationLogCurrent), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseLRequirement.ExistingLichessPlatformPreserved), RequirementClass.LichessProvided)

  test("Version 2 Phase T release-candidate requirements are classified before production launch work"):
    val byRequirement =
      PhaseTRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(PhaseTRequirement.PhaseSRegressionAccepted), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseTRequirement.ReleaseDocumentsCurrent), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseTRequirement.PatchMapIntegrationLogCurrent), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseTRequirement.UpstreamSyncSafeguards), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseTRequirement.HighRiskAreaApproval), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(PhaseTRequirement.ExistingGoNoGoEvidenceBundle), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(PhaseTRequirement.FuturePhaseScopeBlocked), RequirementClass.AdaptedToLichessFork)

  test("Appendix Z remaining decisions must be covered and non-blocking"):
    assert(Version11DecisionRegister.coversAppendixZRemainingDecisions(remainingDecisions))
    assert(documents.accepted)

    val missingDecision = documents.copy(remainingDecisions = remainingDecisions.filterNot(_.decisionId == "DEC-L1-008"))
    val blocker = documents.copy(
      remainingDecisions = remainingDecisions.map {
        case item if item.decisionId == "DEC-L1-003" => item.copy(disposition = DecisionDisposition.LaunchBlocker)
        case item                                    => item
      }
    )

    assert(!missingDecision.accepted)
    assert(!blocker.accepted)

  test("release documents require appendices, Appendix Z, patch map, integration log, upstream sync, and no unmapped core edits"):
    assert(documents.accepted)
    assert(!documents.copy(allAppendicesCurrent = false).accepted)
    assert(!documents.copy(appendixZCurrent = false).accepted)
    assert(!documents.copy(patchMapCurrent = false).accepted)
    assert(!documents.copy(integrationLogCurrent = false).accepted)
    assert(!documents.copy(upstreamSyncDocumented = false).accepted)
    assert(!documents.copy(noUnmappedCoreEdits = false).accepted)

  test("full regression evidence requires local boot, normal chess, EvenChess, compile, diff hygiene, and command coverage"):
    assert(regression.accepted)
    assert(ReleaseCheckPlan.requiredCommands.subsetOf(regression.commandsRun.toSet))
    assert(!regression.copy(localLilaBootVerified = false).accepted)
    assert(!regression.copy(normalChessRegressionPassed = false).accepted)
    assert(!regression.copy(evenChessRegressionPassed = false).accepted)
    assert(!regression.copy(rootCompilePassed = false).accepted)
    assert(!regression.copy(commandsRun = List("evenchess/test")).accepted)
    assert(!regression.copy(failingChecks = List("compile")).accepted)

  test("upstream/core touches require normal regression before release"):
    assert(!regression.copy(upstreamLilaFilesTouched = true, normalRegressionRunForUpstreamTouches = false).accepted)
    assert(regression.copy(upstreamLilaFilesTouched = true, normalRegressionRunForUpstreamTouches = true).accepted)

  test("normal chess separation preserves Lichess internals while public start flows are EvenChess-only"):
    assert(separation.accepted)
    assert(!separation.copy(legalMoveGenerationDelegatedToLichess = false).accepted)
    assert(!separation.copy(boardAndClockInternalsPreserved = false).accepted)
    assert(!separation.copy(normalRatingsNotUsedAsEcr = false).accepted)
    assert(!separation.copy(normalChessInternalsAvailableForRegression = false).accepted)
    assert(!separation.copy(publicStartFlowsEvenChessOnly = false).accepted)
    assert(!separation.copy(noNormalChessDeletion = false).accepted)

  test("release bundle composes Appendix V, trust ops, documents, regression, separation, and paid launch"):
    assert(bundle.appendixVReadiness.releaseReady)
    assert(bundle.evidenceAccepted)
    assert(!bundle.copy(marketingOps = marketingOps.copy(eventDedupeWorks = false)).evidenceAccepted)
    assert(!bundle.copy(trustOps = trustOps.copy(noRateAnnulAuditPass = false)).evidenceAccepted)
    assert(!bundle.copy(documents = documents.copy(patchMapCurrent = false)).evidenceAccepted)
    assert(!bundle.copy(paidLaunch = paidLaunch.copy(allowed = false, blockedReasons = List("tracking"))).evidenceAccepted)

  test("go/no-go report allows release only when evidence and approvals pass"):
    val report = ReleaseGate.evaluate(bundle)

    assertEquals(report.decision, GoNoGoDecision.Go)
    assert(report.releaseAllowed)
    assertEquals(report.blockers, Nil)

  test("go/no-go report awaits approval when evidence passes but approval is missing"):
    val report = ReleaseGate.evaluate(bundle.copy(approval = approval.copy(securityPrivacyApproved = false)))

    assertEquals(report.decision, GoNoGoDecision.AwaitingApproval)
    assert(!report.releaseAllowed)
    assert(report.blockers.isEmpty)
    assert(report.warnings.contains("go_no_go_approval_missing"))

  test("go/no-go report blocks release on failed evidence and reports concrete blockers"):
    val report = ReleaseGate.evaluate(
      bundle.copy(
        regression = regression.copy(rootCompilePassed = false),
        normalChessSeparation = separation.copy(normalRatingsNotUsedAsEcr = false),
        documents = documents.copy(integrationLogCurrent = false),
        paidLaunch = paidLaunch.copy(allowed = false, blockedReasons = List("tracking_destinations_disabled"))
      )
    )

    assertEquals(report.decision, GoNoGoDecision.NoGo)
    assert(!report.releaseAllowed)
    assert(report.blockers.contains("release_documents_or_decisions_not_current"))
    assert(report.blockers.contains("full_regression_failed"))
    assert(report.blockers.contains("normal_chess_separation_failed"))
    assert(report.blockers.contains("paid_launch_gate_failed"))

  test("release-candidate upstream sync evidence blocks casual syncs and unapproved high-risk Lichess areas"):
    val safeSync = upstreamSync.copy(
      upstreamSyncAttempted = true,
      workingTreeCleanBeforeSync = true,
      patchMapCurrentBeforeSync = true,
      regressionRunAfterSync = true
    )
    val highRiskApproved = safeSync.copy(
      highRiskAreasTouched = Set("scalachess", "chessground"),
      highRiskApprovalId = "approval-high-risk"
    )
    val highRiskUnapproved = highRiskApproved.copy(highRiskApprovalId = "")
    val unknownHighRisk = highRiskApproved.copy(highRiskAreasTouched = Set("unknown-core"))

    assert(upstreamSync.accepted)
    assert(safeSync.accepted)
    assert(highRiskApproved.accepted)
    assert(!safeSync.copy(regressionRunAfterSync = false).accepted)
    assert(!highRiskUnapproved.accepted)
    assert(!unknownHighRisk.accepted)

  test("release-candidate metadata requires version source requirements rollback and test summary"):
    assert(candidateStamp.valid)
    assert(!candidateStamp.copy(candidateId = "").valid)
    assert(!candidateStamp.copy(version = "").valid)
    assert(!candidateStamp.copy(sourceRef = "").valid)
    assert(!candidateStamp.copy(requirementsVersion = "").valid)
    assert(!candidateStamp.copy(rollbackNote = "").valid)
    assert(!candidateStamp.copy(testSummary = "").valid)

  test("release-candidate gate allows Go only when evidence Phase S documents sync and approvals pass"):
    val report = ReleaseCandidateGate.evaluate(candidate)

    assert(candidate.accepted)
    assertEquals(report.candidateId, "rc-v2-1")
    assertEquals(report.decision, GoNoGoDecision.Go)
    assert(report.releaseAllowed)
    assertEquals(report.blockers, Nil)

  test("release-candidate gate awaits approval when evidence passes but sign-off is missing"):
    val awaiting =
      ReleaseCandidateGate.evaluate(candidate.copy(bundle = bundle.copy(approval = approval.copy(engineeringApproved = false))))

    assertEquals(awaiting.decision, GoNoGoDecision.AwaitingApproval)
    assert(!awaiting.releaseAllowed)
    assert(awaiting.blockers.isEmpty)
    assert(awaiting.warnings.contains("go_no_go_approval_missing"))

  test("release-candidate gate blocks Phase S document sync future-scope and base evidence failures"):
    val badPhaseS = phaseSRegression.copy(evidence = phaseSRegression.evidence.filterNot(_.surface == PhaseSRegressionSurface.ProposedMoveSingleArrow))
    val report = ReleaseCandidateGate.evaluate(
      candidate.copy(
        bundle = bundle.copy(regression = regression.copy(rootCompilePassed = false)),
        phaseSRegression = badPhaseS,
        upstreamSync = upstreamSync.copy(
          upstreamSyncAttempted = true,
          workingTreeCleanBeforeSync = true,
          patchMapCurrentBeforeSync = false,
          regressionRunAfterSync = true
        ),
        patchMapCurrent = false,
        integrationLogCurrent = false,
        noFuturePhaseScopeImplemented = false,
        completionReportReady = false
      )
    )

    assertEquals(report.decision, GoNoGoDecision.NoGo)
    assert(!report.releaseAllowed)
    assert(report.blockers.contains("full_regression_failed"))
    assert(report.blockers.contains("phase_s_regression_hardening_failed"))
    assert(report.blockers.contains("upstream_sync_or_high_risk_approval_failed"))
    assert(report.blockers.contains("patch_map_not_current"))
    assert(report.blockers.contains("integration_log_not_current"))
    assert(report.blockers.contains("future_phase_scope_detected"))
    assert(report.blockers.contains("completion_report_not_ready"))
