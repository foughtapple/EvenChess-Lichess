package lila.evenchess

class Version12ProductionReadinessTest extends munit.FunSuite:

  import ProductInvariants.RequirementClass
  import Version12ProductionReadiness.*

  private val visualResults =
    SurfaceCoverage.requiredVisualSurfaces.toList.map: surface =>
      SurfaceQaResult(surface, if surface.toString.startsWith("Mobile") then QaCheckKind.MobileVisual else QaCheckKind.Visual, ReadinessStatus.Passed, s"$surface visual checked")

  private val browserResults =
    SurfaceCoverage.requiredBrowserSmokeSurfaces.toList.map: surface =>
      val kind =
        surface match
          case QaSurface.AdminOperations => QaCheckKind.AdminGate
          case QaSurface.LiveOverlayPath => QaCheckKind.OverlayPath
          case _                         => QaCheckKind.BrowserSmoke
      SurfaceQaResult(surface, kind, ReadinessStatus.Passed, s"$surface smoke checked")

  private val decisions =
    Version12DecisionRegister.remainingDecisionIds.toList.sorted.map: id =>
      LaunchDecisionStatus(
        decisionId = id,
        disposition = LaunchDecisionDisposition.DeferredWithOwner,
        owner = "product-owner",
        notes = s"$id tracked before production launch"
      )

  private val accessibility =
    AccessibilityQaEvidence(
      overlayDisclosurePersistent = true,
      noColorOnlySignals = true,
      keyboardAndScreenReaderLabelsPresent = true,
      ttsOffByDefault = true,
      ttsReadsSameVisibleText = true,
      liveTtsRequiresAuditIdentity = true,
      evidence = "Overlay/TTS accessibility contract tests pass."
    )

  private val performance =
    PerformanceQaEvidence(
      overlayRenderBudgetMs = 100,
      overlayRenderObservedMs = 20,
      ttsStartBudgetMs = 500,
      ttsStartObservedMs = 100,
      aiHealthBudgetMs = 2_000,
      aiHealthObservedMs = 250,
      boardInteractionNotBlocked = true,
      evidence = "Contract-level budgets satisfied; live provider latency still requires production telemetry."
    )

  private val records =
    ReleaseRecordsEvidence(
      patchMapCurrent = true,
      integrationLogCurrent = true,
      releaseEvidenceDocCurrent = true,
      upstreamSyncProcessReviewed = true,
      noUnmappedCoreLichessEdits = true,
      highRiskPatchEntriesReviewed = true,
      unresolvedDecisions = decisions
    )

  private val regression =
    RegressionEvidence(
      evenChessTestPassed = true,
      rootCompilePassed = true,
      diffCheckPassed = true,
      routeSmokePassed = true,
      normalChessRegressionEvidence = "Route smoke and compile covered touched Lichess seams; human-vs-human local play remains the Stage 1 baseline.",
      commandsRun = List("evenchess/test", "compile", "git diff --check", "route-smoke"),
      failingChecks = Nil
    )

  private def evidence(
      visual: VisualQaEvidence = VisualQaEvidence(visualResults),
      browser: BrowserSmokeEvidence = BrowserSmokeEvidence(browserResults),
      approval: Boolean = true,
      releaseRecords: ReleaseRecordsEvidence = records
  ) =
    Version12ProductionEvidence(
      visualQa = visual,
      browserSmoke = browser,
      accessibility = accessibility,
      performance = performance,
      records = releaseRecords,
      regression = regression,
      existingLichessPlatformPreserved = true,
      productApprovalCaptured = approval
    )

  test("Version 1.2 Phase L requirements are classified before implementation"):
    val byRequirement =
      Version12PhaseLRequirementClassifications.all.map(item => item.requirement -> item.classification).toMap

    assertEquals(byRequirement(Version12PhaseLRequirement.VisualQaAcrossSurfaces), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(Version12PhaseLRequirement.AccessibilityAndTtsQa), RequirementClass.EvenChessSpecific)
    assertEquals(byRequirement(Version12PhaseLRequirement.PerformanceBudgets), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(Version12PhaseLRequirement.BrowserSmokeCoverage), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(Version12PhaseLRequirement.PatchMapAndIntegrationLogCurrent), RequirementClass.AdaptedToLichessFork)
    assertEquals(byRequirement(Version12PhaseLRequirement.ExistingLichessPlatformPreserved), RequirementClass.LichessProvided)

  test("visual QA requires every Version 1.2 public settings admin and mobile surface"):
    val visual = VisualQaEvidence(visualResults)

    assert(visual.phaseComplete)
    assert(visual.productionReady)
    assert(SurfaceCoverage.requiredVisualSurfaces.subsetOf(visual.coveredSurfaces))
    assert(!VisualQaEvidence(visualResults.filterNot(_.surface == QaSurface.AdminOperations)).phaseComplete)
    assert(!VisualQaEvidence(visualResults.map {
      case item if item.surface == QaSurface.MobilePlaySearch => item.copy(status = ReadinessStatus.Failed)
      case item                                               => item
    }).phaseComplete)
    assert(!VisualQaEvidence(visualResults.map {
      case item if item.surface == QaSurface.MobilePlaySearch => item.copy(status = ReadinessStatus.Warning)
      case item                                               => item
    }).productionReady)

  test("browser smoke requires public routes settings admin gate and live overlay path coverage"):
    val smoke = BrowserSmokeEvidence(browserResults)

    assert(smoke.phaseComplete)
    assert(smoke.productionReady)
    assert(SurfaceCoverage.requiredBrowserSmokeSurfaces.subsetOf(smoke.coveredSurfaces))
    assert(!BrowserSmokeEvidence(browserResults.filterNot(_.surface == QaSurface.LiveOverlayPath)).phaseComplete)
    assert(!BrowserSmokeEvidence(browserResults.map {
      case item if item.surface == QaSurface.PlaySearch => item.copy(status = ReadinessStatus.Unavailable)
      case item                                         => item
    }).productionReady)

  test("accessibility and TTS evidence enforces same visible text and audit identity"):
    assert(accessibility.accepted)
    assert(!accessibility.copy(ttsReadsSameVisibleText = false).accepted)
    assert(!accessibility.copy(liveTtsRequiresAuditIdentity = false).accepted)
    assert(!accessibility.copy(noColorOnlySignals = false).accepted)

  test("performance evidence protects overlay TTS AI budgets and board interaction"):
    assert(performance.accepted)
    assert(!performance.copy(overlayRenderObservedMs = 101).accepted)
    assert(!performance.copy(ttsStartObservedMs = 501).accepted)
    assert(!performance.copy(aiHealthObservedMs = 2_001).accepted)
    assert(!performance.copy(boardInteractionNotBlocked = false).accepted)

  test("release records require patch map integration log upstream sync and all unresolved decisions covered"):
    assert(records.accepted)
    assert(Version12DecisionRegister.coversRequiredDecisions(decisions))
    assert(!records.copy(integrationLogCurrent = false).accepted)
    assert(!records.copy(unresolvedDecisions = decisions.filterNot(_.decisionId == "DEC-V12-001")).accepted)
    assert(!records.copy(unresolvedDecisions = decisions.map {
      case item if item.decisionId == "DEC-V12-004" => item.copy(disposition = LaunchDecisionDisposition.LaunchBlocker)
      case item                                     => item
    }).accepted)

  test("regression evidence requires EvenChess test compile diff route smoke and normal chess evidence"):
    assert(regression.accepted)
    assert(!regression.copy(evenChessTestPassed = false).accepted)
    assert(!regression.copy(rootCompilePassed = false).accepted)
    assert(!regression.copy(diffCheckPassed = false).accepted)
    assert(!regression.copy(routeSmokePassed = false).accepted)
    assert(!regression.copy(commandsRun = List("evenchess/test", "compile")).accepted)

  test("production evidence distinguishes phase evidence from launch approval"):
    val ready = evidence()
    val missingApproval = evidence(approval = false)
    val unavailableBrowser = evidence(
      browser = BrowserSmokeEvidence(browserResults.map {
        case item if item.surface == QaSurface.Analysis => item.copy(status = ReadinessStatus.Unavailable, evidence = "Browser plugin unavailable.")
        case item                                      => item
      })
    )

    assert(ready.phaseEvidenceRecorded)
    assert(ready.productionLaunchAllowed)
    assert(missingApproval.phaseEvidenceRecorded)
    assert(!missingApproval.productionLaunchAllowed)
    assert(missingApproval.launchBlockers.contains("product_approval_missing"))
    assert(unavailableBrowser.phaseEvidenceRecorded)
    assert(!unavailableBrowser.productionLaunchAllowed)
    assert(unavailableBrowser.launchBlockers.contains("browser_smoke_not_production_ready"))
