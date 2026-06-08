package lila.evenchess

import ProductInvariants.RequirementClass

object Version12ProductionReadiness:

  enum Version12PhaseLRequirement:
    case VisualQaAcrossSurfaces
    case AccessibilityAndTtsQa
    case PerformanceBudgets
    case BrowserSmokeCoverage
    case FullRegressionEvidence
    case ReleaseEvidenceReport
    case PatchMapAndIntegrationLogCurrent
    case UnresolvedDecisionRegister
    case UpstreamSyncReadiness
    case ExistingLichessPlatformPreserved

  final case class Version12PhaseLRequirementClassification(
      requirement: Version12PhaseLRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object Version12PhaseLRequirementClassifications:
    val all: List[Version12PhaseLRequirementClassification] = List(
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.VisualQaAcrossSurfaces,
        RequirementClass.EvenChessSpecific,
        "Collect visual QA evidence for the deep-blue EvenChess shell and all EvenChess overlay/admin/account surfaces."
      ),
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.AccessibilityAndTtsQa,
        RequirementClass.EvenChessSpecific,
        "Verify overlay disclosure, keyboard-safe controls, no color-only signaling, and same-text TTS behavior."
      ),
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.PerformanceBudgets,
        RequirementClass.AdaptedToLichessFork,
        "Check EvenChess overlays, AI/TTS seams, and board-adjacent UI without degrading Lichess board interaction."
      ),
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.BrowserSmokeCoverage,
        RequirementClass.AdaptedToLichessFork,
        "Smoke the public routes, learning routes, settings/admin gates, and live overlay path through the local Lichess site."
      ),
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.FullRegressionEvidence,
        RequirementClass.EvenChessSpecific,
        "Record full EvenChess regression, root compile, route checks, and diff hygiene for the release evidence."
      ),
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.ReleaseEvidenceReport,
        RequirementClass.EvenChessSpecific,
        "Write the Version 1.2 release evidence report with pass/fail/unavailable results and launch blockers."
      ),
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.PatchMapAndIntegrationLogCurrent,
        RequirementClass.AdaptedToLichessFork,
        "Confirm every upstream/core Lichess edit has a patch-map entry and every Version 1.2 phase has an integration-log entry."
      ),
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.UnresolvedDecisionRegister,
        RequirementClass.AdaptedToLichessFork,
        "List unresolved Version 1.1 and 1.2 product/provider decisions before production launch."
      ),
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.UpstreamSyncReadiness,
        RequirementClass.AdaptedToLichessFork,
        "Record upstream sync process readiness and known high-risk reimplementation zones."
      ),
      Version12PhaseLRequirementClassification(
        Version12PhaseLRequirement.ExistingLichessPlatformPreserved,
        RequirementClass.LichessProvided,
        "Lichess remains owner of normal chess, accounts, study, opening explorer, analysis, puzzles, admin shell, and moderation foundations."
      )
    )

  enum ReadinessStatus:
    case Passed
    case Warning
    case Unavailable
    case Failed

    def phaseAcceptable: Boolean = this != Failed
    def productionReady: Boolean = this == Passed

  enum QaSurface:
    case Homepage
    case PlaySearch
    case Study
    case OpeningExplorer
    case Analysis
    case UserEvenChessSettings
    case AdminOperations
    case LiveOverlayPath
    case MobileHomepage
    case MobilePlaySearch

  enum QaCheckKind:
    case Visual
    case MobileVisual
    case Accessibility
    case Performance
    case BrowserSmoke
    case RouteSmoke
    case AdminGate
    case OverlayPath

  final case class SurfaceQaResult(
      surface: QaSurface,
      checkKind: QaCheckKind,
      status: ReadinessStatus,
      evidence: String,
      followUp: Option[String] = None
  ):
    def recorded: Boolean = evidence.nonEmpty
    def phaseAcceptable: Boolean = recorded && status.phaseAcceptable
    def productionReady: Boolean = recorded && status.productionReady

  object SurfaceCoverage:
    val requiredVisualSurfaces: Set[QaSurface] = Set(
      QaSurface.Homepage,
      QaSurface.PlaySearch,
      QaSurface.Study,
      QaSurface.OpeningExplorer,
      QaSurface.Analysis,
      QaSurface.UserEvenChessSettings,
      QaSurface.AdminOperations,
      QaSurface.MobileHomepage,
      QaSurface.MobilePlaySearch
    )

    val requiredBrowserSmokeSurfaces: Set[QaSurface] = Set(
      QaSurface.Homepage,
      QaSurface.PlaySearch,
      QaSurface.Study,
      QaSurface.OpeningExplorer,
      QaSurface.Analysis,
      QaSurface.UserEvenChessSettings,
      QaSurface.AdminOperations,
      QaSurface.LiveOverlayPath
    )

  final case class VisualQaEvidence(results: List[SurfaceQaResult]):
    private def visualResults = results.filter(result => result.checkKind == QaCheckKind.Visual || result.checkKind == QaCheckKind.MobileVisual)
    def coveredSurfaces: Set[QaSurface] = visualResults.map(_.surface).toSet
    def requiredSurfacesCovered: Boolean = SurfaceCoverage.requiredVisualSurfaces.subsetOf(coveredSurfaces)
    def phaseComplete: Boolean = requiredSurfacesCovered && visualResults.forall(_.phaseAcceptable)
    def productionReady: Boolean = requiredSurfacesCovered && visualResults.forall(_.productionReady)

  final case class BrowserSmokeEvidence(results: List[SurfaceQaResult]):
    private def smokeResults = results.filter(result => result.checkKind == QaCheckKind.BrowserSmoke || result.checkKind == QaCheckKind.RouteSmoke || result.checkKind == QaCheckKind.AdminGate || result.checkKind == QaCheckKind.OverlayPath)
    def coveredSurfaces: Set[QaSurface] = smokeResults.map(_.surface).toSet
    def requiredSurfacesCovered: Boolean = SurfaceCoverage.requiredBrowserSmokeSurfaces.subsetOf(coveredSurfaces)
    def phaseComplete: Boolean = requiredSurfacesCovered && smokeResults.forall(_.phaseAcceptable)
    def productionReady: Boolean = requiredSurfacesCovered && smokeResults.forall(_.productionReady)

  final case class AccessibilityQaEvidence(
      overlayDisclosurePersistent: Boolean,
      noColorOnlySignals: Boolean,
      keyboardAndScreenReaderLabelsPresent: Boolean,
      ttsOffByDefault: Boolean,
      ttsReadsSameVisibleText: Boolean,
      liveTtsRequiresAuditIdentity: Boolean,
      evidence: String
  ):
    def accepted: Boolean =
      overlayDisclosurePersistent &&
        noColorOnlySignals &&
        keyboardAndScreenReaderLabelsPresent &&
        ttsOffByDefault &&
        ttsReadsSameVisibleText &&
        liveTtsRequiresAuditIdentity &&
        evidence.nonEmpty

  final case class PerformanceQaEvidence(
      overlayRenderBudgetMs: Int,
      overlayRenderObservedMs: Int,
      ttsStartBudgetMs: Int,
      ttsStartObservedMs: Int,
      aiHealthBudgetMs: Int,
      aiHealthObservedMs: Int,
      boardInteractionNotBlocked: Boolean,
      evidence: String
  ):
    def accepted: Boolean =
      overlayRenderBudgetMs > 0 &&
        overlayRenderObservedMs <= overlayRenderBudgetMs &&
        ttsStartBudgetMs > 0 &&
        ttsStartObservedMs <= ttsStartBudgetMs &&
        aiHealthBudgetMs > 0 &&
        aiHealthObservedMs <= aiHealthBudgetMs &&
        boardInteractionNotBlocked &&
        evidence.nonEmpty

  enum LaunchDecisionDisposition:
    case Resolved
    case DeferredWithOwner
    case LaunchBlocker

  final case class LaunchDecisionStatus(
      decisionId: String,
      disposition: LaunchDecisionDisposition,
      owner: String,
      notes: String
  ):
    def covered: Boolean = decisionId.nonEmpty && owner.nonEmpty && notes.nonEmpty
    def nonBlocking: Boolean = covered && disposition != LaunchDecisionDisposition.LaunchBlocker

  object Version12DecisionRegister:
    val remainingDecisionIds: Set[String] = Set(
      "DEC-L1-002",
      "DEC-L1-003",
      "DEC-L1-008",
      "DEC-L1-009",
      "DEC-V12-001",
      "DEC-V12-002",
      "DEC-V12-003",
      "DEC-V12-004",
      "DEC-V12-005"
    )

    def coversRequiredDecisions(statuses: List[LaunchDecisionStatus]): Boolean =
      remainingDecisionIds.subsetOf(statuses.map(_.decisionId).toSet)

  final case class ReleaseRecordsEvidence(
      patchMapCurrent: Boolean,
      integrationLogCurrent: Boolean,
      releaseEvidenceDocCurrent: Boolean,
      upstreamSyncProcessReviewed: Boolean,
      noUnmappedCoreLichessEdits: Boolean,
      highRiskPatchEntriesReviewed: Boolean,
      unresolvedDecisions: List[LaunchDecisionStatus]
  ):
    def accepted: Boolean =
      patchMapCurrent &&
        integrationLogCurrent &&
        releaseEvidenceDocCurrent &&
        upstreamSyncProcessReviewed &&
        noUnmappedCoreLichessEdits &&
        highRiskPatchEntriesReviewed &&
        Version12DecisionRegister.coversRequiredDecisions(unresolvedDecisions) &&
        unresolvedDecisions.forall(_.nonBlocking)

  final case class RegressionEvidence(
      evenChessTestPassed: Boolean,
      rootCompilePassed: Boolean,
      diffCheckPassed: Boolean,
      routeSmokePassed: Boolean,
      normalChessRegressionEvidence: String,
      commandsRun: List[String],
      failingChecks: List[String]
  ):
    val requiredCommands: Set[String] = Set("evenchess/test", "compile", "git diff --check", "route-smoke")

    def accepted: Boolean =
      evenChessTestPassed &&
        rootCompilePassed &&
        diffCheckPassed &&
        routeSmokePassed &&
        normalChessRegressionEvidence.nonEmpty &&
        requiredCommands.subsetOf(commandsRun.toSet) &&
        failingChecks.isEmpty

  final case class Version12ProductionEvidence(
      visualQa: VisualQaEvidence,
      browserSmoke: BrowserSmokeEvidence,
      accessibility: AccessibilityQaEvidence,
      performance: PerformanceQaEvidence,
      records: ReleaseRecordsEvidence,
      regression: RegressionEvidence,
      existingLichessPlatformPreserved: Boolean,
      productApprovalCaptured: Boolean
  ):
    def phaseEvidenceRecorded: Boolean =
      visualQa.phaseComplete &&
        browserSmoke.phaseComplete &&
        accessibility.accepted &&
        performance.accepted &&
        records.accepted &&
        regression.accepted &&
        existingLichessPlatformPreserved

    def productionLaunchAllowed: Boolean =
      phaseEvidenceRecorded &&
        visualQa.productionReady &&
        browserSmoke.productionReady &&
        productApprovalCaptured

    def launchBlockers: List[String] =
      List(
        Option.when(!visualQa.phaseComplete)("visual_qa_evidence_missing_or_failed"),
        Option.when(!browserSmoke.phaseComplete)("browser_smoke_evidence_missing_or_failed"),
        Option.when(!accessibility.accepted)("accessibility_or_tts_qa_failed"),
        Option.when(!performance.accepted)("performance_qa_failed"),
        Option.when(!records.accepted)("release_records_or_decisions_not_current"),
        Option.when(!regression.accepted)("regression_evidence_failed"),
        Option.when(!existingLichessPlatformPreserved)("lichess_platform_preservation_failed"),
        Option.when(phaseEvidenceRecorded && !visualQa.productionReady)("visual_qa_not_production_ready"),
        Option.when(phaseEvidenceRecorded && !browserSmoke.productionReady)("browser_smoke_not_production_ready"),
        Option.when(phaseEvidenceRecorded && !productApprovalCaptured)("product_approval_missing")
      ).flatten
