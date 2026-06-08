package lila.evenchess

import MarketingAttributionFunnel.PaidLaunchGateResult
import ProductInvariants.RequirementClass
import TestingQaAcceptance.{
  BaselineLichessRegression,
  CrossCuttingGateReport,
  FinalAcceptanceEvidence,
  MarketingOpsAcceptanceEvidence,
  PhaseSRegressionHardeningReport,
  RatedEvenChessAcceptanceEvidence,
  ReleaseReadiness,
  Stage1AcceptanceEvidence
}
import TrustOpsIncidentControls.TrustOpsAcceptanceEvidence

object ReleaseHardeningGoNoGo:

  enum PhaseTRequirement:
    case PhaseSRegressionAccepted
    case ReleaseDocumentsCurrent
    case PatchMapIntegrationLogCurrent
    case UpstreamSyncSafeguards
    case HighRiskAreaApproval
    case FullRegressionAndCompileChecks
    case ExistingGoNoGoEvidenceBundle
    case CandidateMetadataAndRollback
    case ApprovalAwareGoNoGo
    case FuturePhaseScopeBlocked

  final case class PhaseTRequirementClassification(
      requirement: PhaseTRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseTRequirementClassifications:
    val all: List[PhaseTRequirementClassification] = List(
      PhaseTRequirementClassification(
        PhaseTRequirement.PhaseSRegressionAccepted,
        RequirementClass.EvenChessSpecific,
        "Release candidate requires the Phase S regression-hardening report to cover every V2 core surface and acceptance gate."
      ),
      PhaseTRequirementClassification(
        PhaseTRequirement.ReleaseDocumentsCurrent,
        RequirementClass.AdaptedToLichessFork,
        "Main requirements, Appendix Z, integration log, upstream-sync notes, and completion evidence must be current."
      ),
      PhaseTRequirementClassification(
        PhaseTRequirement.PatchMapIntegrationLogCurrent,
        RequirementClass.AdaptedToLichessFork,
        "Patch map and integration log must be current before release-candidate evaluation."
      ),
      PhaseTRequirementClassification(
        PhaseTRequirement.UpstreamSyncSafeguards,
        RequirementClass.AdaptedToLichessFork,
        "Release-candidate evidence must show no casual upstream sync, or a clean-tree/current-patch-map sync with post-sync regression."
      ),
      PhaseTRequirementClassification(
        PhaseTRequirement.HighRiskAreaApproval,
        RequirementClass.AdaptedToLichessFork,
        "High-risk Lichess areas such as scalachess, chessground, lila-ws, search, fishnet, rating/perf internals, and game schema need explicit approval."
      ),
      PhaseTRequirementClassification(
        PhaseTRequirement.FullRegressionAndCompileChecks,
        RequirementClass.EvenChessSpecific,
        "Release candidate requires EvenChess tests, EvenChess compile, root compile, and diff hygiene or documented blockers."
      ),
      PhaseTRequirementClassification(
        PhaseTRequirement.ExistingGoNoGoEvidenceBundle,
        RequirementClass.EvenChessSpecific,
        "Final integration composes Appendix V readiness, trust/ops, release documents, full regression, normal-chess separation, and paid-launch gates."
      ),
      PhaseTRequirementClassification(
        PhaseTRequirement.CandidateMetadataAndRollback,
        RequirementClass.EvenChessSpecific,
        "Candidate id, version, commit/reference, requirements version, rollback note, and test summary must be present."
      ),
      PhaseTRequirementClassification(
        PhaseTRequirement.ApprovalAwareGoNoGo,
        RequirementClass.EvenChessSpecific,
        "A candidate with complete evidence but missing product/engineering/security approval remains AwaitingApproval rather than Go."
      ),
      PhaseTRequirementClassification(
        PhaseTRequirement.FuturePhaseScopeBlocked,
        RequirementClass.AdaptedToLichessFork,
        "Phase T must not implement future production rollout automation beyond release-candidate go/no-go evidence."
      )
    )

  enum PhaseLRequirement:
    case BaselineLichessRegression
    case FullEvenChessRegression
    case CrossCuttingAcceptanceGates
    case MarketingOpsTrustOpsGates
    case AppendixZAndDecisionRegister
    case PatchMapAndIntegrationLogCurrent
    case UpstreamSyncDocumented
    case NormalChessSeparation
    case ProductionGoNoGoDecision
    case ExistingLichessPlatformPreserved

  final case class PhaseLRequirementClassification(
      requirement: PhaseLRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseLRequirementClassifications:
    val all: List[PhaseLRequirementClassification] = List(
      PhaseLRequirementClassification(
        PhaseLRequirement.BaselineLichessRegression,
        RequirementClass.LichessProvided,
        "Verify Lichess boot, normal move legality, clocks, result flow, review/history, and accounts rather than rebuilding them."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.FullEvenChessRegression,
        RequirementClass.EvenChessSpecific,
        "Require the full EvenChess test suite and compile checks before go/no-go."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.CrossCuttingAcceptanceGates,
        RequirementClass.EvenChessSpecific,
        "Aggregate invariant, server-authority, audit, engine, AI, ECR, Target, stale-clearing, mobile/accessibility, and patch-map gates."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.MarketingOpsTrustOpsGates,
        RequirementClass.EvenChessSpecific,
        "Combine Appendix V marketing/ops evidence with Appendix R/Q trust, incident, rollback, and dashboard evidence."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.AppendixZAndDecisionRegister,
        RequirementClass.AdaptedToLichessFork,
        "Block release when Appendix Z is stale or an unresolved launch decision has no owner/disposition."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.PatchMapAndIntegrationLogCurrent,
        RequirementClass.AdaptedToLichessFork,
        "Patch map and integration log must be current before release or upstream sync."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.UpstreamSyncDocumented,
        RequirementClass.AdaptedToLichessFork,
        "Release evidence must include upstream sync process status and reviewed conflict zones."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.NormalChessSeparation,
        RequirementClass.EvenChessSpecific,
        "Public EvenChess-only routing cannot corrupt normal Lichess internals or normal ratings."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.ProductionGoNoGoDecision,
        RequirementClass.EvenChessSpecific,
        "Produce an explicit go/no-go decision from evidence, blockers, warnings, and approvals."
      ),
      PhaseLRequirementClassification(
        PhaseLRequirement.ExistingLichessPlatformPreserved,
        RequirementClass.LichessProvided,
        "Underlying chess platform behavior remains Lichess-owned and regression-tested."
      )
    )

  enum DecisionDisposition:
    case Resolved
    case ExplicitlyDeferred
    case LaunchBlocker

  final case class ProductDecisionStatus(
      decisionId: String,
      disposition: DecisionDisposition,
      owner: String,
      notes: String
  ):
    def nonBlocking: Boolean =
      decisionId.nonEmpty &&
        owner.nonEmpty &&
        notes.nonEmpty &&
        disposition != DecisionDisposition.LaunchBlocker

  object Version11DecisionRegister:
    val remainingDecisionIds: Set[String] = Set("DEC-L1-002", "DEC-L1-003", "DEC-L1-008", "DEC-L1-009")

    def coversAppendixZRemainingDecisions(statuses: List[ProductDecisionStatus]): Boolean =
      remainingDecisionIds.subsetOf(statuses.map(_.decisionId).toSet)

  final case class ReleaseDocumentStatus(
      allAppendicesCurrent: Boolean,
      appendixZCurrent: Boolean,
      patchMapCurrent: Boolean,
      integrationLogCurrent: Boolean,
      upstreamSyncDocumented: Boolean,
      noContradictionUnreported: Boolean,
      noUnmappedCoreEdits: Boolean,
      mediumHighPatchRisksReviewed: Boolean,
      remainingDecisions: List[ProductDecisionStatus]
  ):
    def accepted: Boolean =
      allAppendicesCurrent &&
        appendixZCurrent &&
        patchMapCurrent &&
        integrationLogCurrent &&
        upstreamSyncDocumented &&
        noContradictionUnreported &&
        noUnmappedCoreEdits &&
        mediumHighPatchRisksReviewed &&
        Version11DecisionRegister.coversAppendixZRemainingDecisions(remainingDecisions) &&
        remainingDecisions.forall(_.nonBlocking)

  final case class FullRegressionStatus(
      localLilaBootVerified: Boolean,
      normalChessRegressionPassed: Boolean,
      evenChessRegressionPassed: Boolean,
      rootCompilePassed: Boolean,
      diffHygienePassed: Boolean,
      noRequiredTestFailures: Boolean,
      upstreamLilaFilesTouched: Boolean,
      normalRegressionRunForUpstreamTouches: Boolean,
      commandsRun: List[String],
      failingChecks: List[String]
  ):
    def accepted: Boolean =
      localLilaBootVerified &&
        normalChessRegressionPassed &&
        evenChessRegressionPassed &&
        rootCompilePassed &&
        diffHygienePassed &&
        noRequiredTestFailures &&
        (!upstreamLilaFilesTouched || normalRegressionRunForUpstreamTouches) &&
        ReleaseCheckPlan.requiredCommands.subsetOf(commandsRun.toSet) &&
        failingChecks.isEmpty

  object ReleaseCheckPlan:
    val requiredCommands: Set[String] = Set(
      "evenchess/test",
      "evenchess/compile",
      "compile",
      "git diff --check"
    )

  final case class NormalChessSeparationEvidence(
      legalMoveGenerationDelegatedToLichess: Boolean,
      boardAndClockInternalsPreserved: Boolean,
      normalRatingsNotUsedAsEcr: Boolean,
      normalChessInternalsAvailableForRegression: Boolean,
      publicStartFlowsEvenChessOnly: Boolean,
      noNormalChessDeletion: Boolean
  ):
    def accepted: Boolean =
      legalMoveGenerationDelegatedToLichess &&
        boardAndClockInternalsPreserved &&
        normalRatingsNotUsedAsEcr &&
        normalChessInternalsAvailableForRegression &&
        publicStartFlowsEvenChessOnly &&
        noNormalChessDeletion

  final case class ReleaseApproval(
      productOwnerApproved: Boolean,
      engineeringApproved: Boolean,
      securityPrivacyApproved: Boolean,
      approvalId: String,
      approvedAt: Long
  ):
    def approved: Boolean =
      productOwnerApproved &&
        engineeringApproved &&
        securityPrivacyApproved &&
        approvalId.nonEmpty &&
        approvedAt > 0

  final case class ReleaseEvidenceBundle(
      baseline: BaselineLichessRegression,
      crossCutting: CrossCuttingGateReport,
      stage1: Stage1AcceptanceEvidence,
      rated: RatedEvenChessAcceptanceEvidence,
      marketingOps: MarketingOpsAcceptanceEvidence,
      trustOps: TrustOpsAcceptanceEvidence,
      finalAcceptance: FinalAcceptanceEvidence,
      documents: ReleaseDocumentStatus,
      regression: FullRegressionStatus,
      normalChessSeparation: NormalChessSeparationEvidence,
      paidLaunch: PaidLaunchGateResult,
      approval: ReleaseApproval
  ):
    def appendixVReadiness: ReleaseReadiness =
      ReleaseReadiness(baseline, crossCutting, stage1, rated, marketingOps, finalAcceptance)

    def evidenceAccepted: Boolean =
      appendixVReadiness.releaseReady &&
        trustOps.accepted &&
        documents.accepted &&
        regression.accepted &&
        normalChessSeparation.accepted &&
        paidLaunch.allowed

  enum GoNoGoDecision:
    case Go
    case AwaitingApproval
    case NoGo

  final case class GoNoGoReport(
      decision: GoNoGoDecision,
      blockers: List[String],
      warnings: List[String],
      nextStep: String
  ):
    def releaseAllowed: Boolean = decision == GoNoGoDecision.Go && blockers.isEmpty

  object ReleaseGate:
    def evaluate(bundle: ReleaseEvidenceBundle): GoNoGoReport =
      val blockers =
        List(
          Option.when(!bundle.baseline.normalChessSafe)("baseline_lichess_regression_failed"),
          Option.when(!bundle.crossCutting.allRequiredPassed)("cross_cutting_gates_failed"),
          Option.when(!bundle.stage1.accepted)("stage1_acceptance_failed"),
          Option.when(!bundle.rated.accepted)("rated_evenchess_acceptance_failed"),
          Option.when(!bundle.marketingOps.accepted)("marketing_ops_acceptance_failed"),
          Option.when(!bundle.trustOps.accepted)("trust_ops_acceptance_failed"),
          Option.when(!bundle.finalAcceptance.accepted)("final_acceptance_failed"),
          Option.when(!bundle.documents.accepted)("release_documents_or_decisions_not_current"),
          Option.when(!bundle.regression.accepted)("full_regression_failed"),
          Option.when(!bundle.normalChessSeparation.accepted)("normal_chess_separation_failed"),
          Option.when(!bundle.paidLaunch.allowed)("paid_launch_gate_failed")
        ).flatten

      val warnings =
        List(
          Option.when(bundle.regression.upstreamLilaFilesTouched)("upstream_lila_files_touched_review_patch_map"),
          Option.when(bundle.documents.remainingDecisions.exists(_.disposition == DecisionDisposition.ExplicitlyDeferred))("remaining_decisions_explicitly_deferred"),
          Option.when(!bundle.approval.approved)("go_no_go_approval_missing")
        ).flatten

      val decision =
        if blockers.nonEmpty then GoNoGoDecision.NoGo
        else if !bundle.approval.approved then GoNoGoDecision.AwaitingApproval
        else GoNoGoDecision.Go

      GoNoGoReport(
        decision = decision,
        blockers = blockers,
        warnings = warnings,
        nextStep = decision match
          case GoNoGoDecision.Go               => "Proceed to controlled production launch checklist."
          case GoNoGoDecision.AwaitingApproval => "Collect product, engineering, and security/privacy go/no-go approval."
          case GoNoGoDecision.NoGo             => "Resolve blockers, rerun required checks, and regenerate go/no-go report."
      )

  object HighRiskArea:
    val names: Set[String] = Set(
      "scalachess",
      "chessground",
      "pgn-viewer",
      "lila-ws",
      "lila-search",
      "lila-fishnet",
      "fishnet",
      "global-rating-perf-internals",
      "core-game-bson-schema-internals"
    )

  final case class UpstreamSyncReleaseEvidence(
      upstreamSyncAttempted: Boolean,
      workingTreeCleanBeforeSync: Boolean,
      patchMapCurrentBeforeSync: Boolean,
      regressionRunAfterSync: Boolean,
      highRiskAreasTouched: Set[String],
      highRiskApprovalId: String
  ):
    def knownHighRiskAreas: Boolean =
      highRiskAreasTouched.subsetOf(HighRiskArea.names)

    def syncSafe: Boolean =
      !upstreamSyncAttempted ||
        (workingTreeCleanBeforeSync && patchMapCurrentBeforeSync && regressionRunAfterSync)

    def highRiskApproved: Boolean =
      highRiskAreasTouched.isEmpty || highRiskApprovalId.nonEmpty

    def accepted: Boolean =
      knownHighRiskAreas && syncSafe && highRiskApproved

  final case class ReleaseCandidateStamp(
      candidateId: String,
      version: String,
      sourceRef: String,
      requirementsVersion: String,
      createdAt: Long,
      rollbackNote: String,
      testSummary: String
  ):
    def valid: Boolean =
      candidateId.nonEmpty &&
        version.nonEmpty &&
        sourceRef.nonEmpty &&
        requirementsVersion.nonEmpty &&
        createdAt > 0 &&
        rollbackNote.nonEmpty &&
        testSummary.nonEmpty

  final case class ReleaseCandidateEvidence(
      stamp: ReleaseCandidateStamp,
      bundle: ReleaseEvidenceBundle,
      phaseSRegression: PhaseSRegressionHardeningReport,
      upstreamSync: UpstreamSyncReleaseEvidence,
      patchMapCurrent: Boolean,
      integrationLogCurrent: Boolean,
      noFuturePhaseScopeImplemented: Boolean,
      completionReportReady: Boolean
  ):
    def accepted: Boolean =
      stamp.valid &&
        bundle.evidenceAccepted &&
        phaseSRegression.accepted &&
        upstreamSync.accepted &&
        patchMapCurrent &&
        integrationLogCurrent &&
        noFuturePhaseScopeImplemented &&
        completionReportReady

  final case class ReleaseCandidateReport(
      candidateId: String,
      decision: GoNoGoDecision,
      blockers: List[String],
      warnings: List[String],
      nextStep: String
  ):
    def releaseAllowed: Boolean =
      decision == GoNoGoDecision.Go && blockers.isEmpty

  object ReleaseCandidateGate:
    def evaluate(candidate: ReleaseCandidateEvidence): ReleaseCandidateReport =
      val base = ReleaseGate.evaluate(candidate.bundle)
      val candidateBlockers =
        List(
          Option.when(!candidate.stamp.valid)("release_candidate_metadata_incomplete"),
          Option.when(!candidate.phaseSRegression.accepted)("phase_s_regression_hardening_failed"),
          Option.when(!candidate.upstreamSync.accepted)("upstream_sync_or_high_risk_approval_failed"),
          Option.when(!candidate.patchMapCurrent)("patch_map_not_current"),
          Option.when(!candidate.integrationLogCurrent)("integration_log_not_current"),
          Option.when(!candidate.noFuturePhaseScopeImplemented)("future_phase_scope_detected"),
          Option.when(!candidate.completionReportReady)("completion_report_not_ready")
        ).flatten
      val blockers = (base.blockers ++ candidateBlockers).distinct
      val warnings =
        (base.warnings ++
          List(
            Option.when(candidate.upstreamSync.upstreamSyncAttempted)("upstream_sync_attempted_for_release_candidate"),
            Option.when(candidate.upstreamSync.highRiskAreasTouched.nonEmpty)("high_risk_lila_area_reviewed")
          ).flatten).distinct
      val decision =
        if blockers.nonEmpty then GoNoGoDecision.NoGo
        else if !candidate.bundle.approval.approved then GoNoGoDecision.AwaitingApproval
        else GoNoGoDecision.Go

      ReleaseCandidateReport(
        candidateId = candidate.stamp.candidateId,
        decision = decision,
        blockers = blockers,
        warnings = warnings,
        nextStep = decision match
          case GoNoGoDecision.Go               => "Tag and hand off this release candidate to the controlled launch checklist."
          case GoNoGoDecision.AwaitingApproval => "Collect product, engineering, and security/privacy approval before tagging release."
          case GoNoGoDecision.NoGo             => "Resolve release-candidate blockers, rerun Phase S/full regression evidence, and re-evaluate."
      )
