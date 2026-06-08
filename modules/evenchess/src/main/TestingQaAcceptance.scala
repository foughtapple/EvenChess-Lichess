package lila.evenchess

import ProductInvariants.RequirementClass

object TestingQaAcceptance:

  enum PhaseSRequirement:
    case NativeSetupSearchOpens
    case SearchCreatesEvenChessContract
    case NormalLichessMechanicsPreserved
    case EcrMmrSeparateFromNormalRatings
    case LevelGatedPayloads
    case DisplayEngineRejectsStalePayloads
    case OverlayVisualRegression
    case FixedSizeCardsNoLayoutJump
    case ProposedMoveSingleArrowCache
    case LiveReviewHistoryModes
    case CustomReviewTokenLogic
    case FullGameEceSettlementNeutral
    case AcceptanceGateBundle

  final case class PhaseSRequirementClassification(
      requirement: PhaseSRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseSRequirementClassifications:
    val all: List[PhaseSRequirementClassification] = List(
      PhaseSRequirementClassification(
        PhaseSRequirement.NativeSetupSearchOpens,
        RequirementClass.AdaptedToLichessFork,
        "Public play must open the native Lichess-style setup/search flow with EvenChess controls."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.SearchCreatesEvenChessContract,
        RequirementClass.EvenChessSpecific,
        "Search submission must produce an EvenChess search or match contract instead of normal Lichess rated search."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.NormalLichessMechanicsPreserved,
        RequirementClass.LichessProvided,
        "Board, legal moves, clocks, PGN/replay, analysis, and result flow remain Lichess-owned regression surfaces."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.EcrMmrSeparateFromNormalRatings,
        RequirementClass.EvenChessSpecific,
        "ECR/MMR acceptance evidence must prove separation from normal Lichess ratings."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.LevelGatedPayloads,
        RequirementClass.EvenChessSpecific,
        "Level-gated payload tests must prove lower-level side outputs never expose higher-level data."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.DisplayEngineRejectsStalePayloads,
        RequirementClass.EvenChessSpecific,
        "Display Engine regression must reject stale, wrong-board, or invalid payloads."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.OverlayVisualRegression,
        RequirementClass.EvenChessSpecific,
        "Offset Count, hanging pieces, threats, pins, opening, and eval overlays need visual or fixture evidence."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.FixedSizeCardsNoLayoutJump,
        RequirementClass.EvenChessSpecific,
        "Summary and Plan card regression must prove stable fixed-size behavior on board surfaces."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.ProposedMoveSingleArrowCache,
        RequirementClass.EvenChessSpecific,
        "Proposed-move mode must run only for exactly one legal green arrow and must cache or clear correctly."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.LiveReviewHistoryModes,
        RequirementClass.EvenChessSpecific,
        "Review regression must cover saved live ECE history for Live White, Live Black, and Live Both."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.CustomReviewTokenLogic,
        RequirementClass.EvenChessSpecific,
        "Custom review regression must cover configured token consumption and non-bypass behavior."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.FullGameEceSettlementNeutral,
        RequirementClass.EvenChessSpecific,
        "Full-game ECE review must not alter live rating settlement."
      ),
      PhaseSRequirementClassification(
        PhaseSRequirement.AcceptanceGateBundle,
        RequirementClass.AdaptedToLichessFork,
        "Phase acceptance needs tests or documented reason, integration log/patch-map discipline, invariant conflict reporting, server authority, secret safety, and desktop/mobile layout checks."
      )
    )

  enum TestingRequirement:
    case LocalLichessBootBeforeFeatures
    case NormalChessRegression
    case AccountSessionRegression
    case UpstreamTouchesNeedNormalRegression
    case CrossCuttingGates
    case Stage1Acceptance
    case RatedEvenChessAcceptance
    case MarketingOpsAcceptance
    case FinalAcceptance

  final case class TestingRequirementClassification(
      requirement: TestingRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object TestingRequirementClassifications:
    val all: List[TestingRequirementClassification] = List(
      TestingRequirementClassification(
        TestingRequirement.LocalLichessBootBeforeFeatures,
        RequirementClass.LichessProvided,
        "Local Lichess boot is the baseline proof that EvenChess features can be tested safely."
      ),
      TestingRequirementClassification(
        TestingRequirement.NormalChessRegression,
        RequirementClass.LichessProvided,
        "Normal chess legal moves, clocks, result flow, review, and history remain Lichess-provided and must keep working."
      ),
      TestingRequirementClassification(
        TestingRequirement.AccountSessionRegression,
        RequirementClass.LichessProvided,
        "Ordinary account and session behavior must not regress from EvenChess code."
      ),
      TestingRequirementClassification(
        TestingRequirement.UpstreamTouchesNeedNormalRegression,
        RequirementClass.AdaptedToLichessFork,
        "Any upstream/core lila touch requires normal chess regression checks and patch-map discipline."
      ),
      TestingRequirementClassification(
        TestingRequirement.CrossCuttingGates,
        RequirementClass.EvenChessSpecific,
        "EvenChess requires invariant, level, server authority, ledger, engine, AI, ECR, Target, accessibility, stale, and patch-map gates."
      ),
      TestingRequirementClassification(
        TestingRequirement.Stage1Acceptance,
        RequirementClass.EvenChessSpecific,
        "Stage 1 acceptance proves local boot, baseline play, module boundary, harmless mode flag, dummy overlay, dummy audit, AI mock if completed, patch map, and go/no-go."
      ),
      TestingRequirementClassification(
        TestingRequirement.RatedEvenChessAcceptance,
        RequirementClass.EvenChessSpecific,
        "Rated EvenChess acceptance requires server policy, L0-L10 gates, replay, isolation, security, validators, no bypass, monetisation fairness, dashboards, and no contradictions."
      ),
      TestingRequirementClassification(
        TestingRequirement.MarketingOpsAcceptance,
        RequirementClass.EvenChessSpecific,
        "Marketing and ops acceptance covers token, ad, pricing, copy, attribution, health, fallback, rollback, incident, and patch-map gates."
      ),
      TestingRequirementClassification(
        TestingRequirement.FinalAcceptance,
        RequirementClass.AdaptedToLichessFork,
        "Final acceptance combines all appendices, Appendix Z, patch map, upstream sync, full regression, reported contradictions, normal chess separation, invariants, and go/no-go approval."
      )
    )

  final case class BaselineLichessRegression(
      localLichessBoots: Boolean,
      legalMovesWork: Boolean,
      clocksWork: Boolean,
      resultFlowWorks: Boolean,
      reviewHistoryWorks: Boolean,
      accountSessionWorks: Boolean,
      upstreamLilaFilesTouched: Boolean,
      normalRegressionRunForUpstreamTouches: Boolean
  ):
    def normalChessSafe: Boolean =
      localLichessBoots &&
        legalMovesWork &&
        clocksWork &&
        resultFlowWorks &&
        reviewHistoryWorks &&
        accountSessionWorks &&
        (!upstreamLilaFilesTouched || normalRegressionRunForUpstreamTouches)

  enum CrossCuttingGate:
    case InvariantTests
    case LevelGates
    case ServerAuthority
    case UsedLevelMonotonicity
    case EveryRenderAudited
    case OffsetCountFixtures
    case AiValidatorPromptInjection
    case StockfishBoundedProfiles
    case EcrReplay
    case TargetIsolation
    case MobileAccessibilityQa
    case StaleClearing
    case PatchMapCompleteness

  final case class GateEvidence(
      gate: CrossCuttingGate,
      passed: Boolean,
      evidence: String
  ):
    def complete: Boolean = passed && evidence.nonEmpty

  final case class CrossCuttingGateReport(evidence: List[GateEvidence]):
    def passed(gate: CrossCuttingGate): Boolean =
      evidence.exists(item => item.gate == gate && item.complete)

    def allRequiredPassed: Boolean =
      CrossCuttingGate.values.forall(passed)

  final case class Stage1AcceptanceEvidence(
      localLilaBoots: Boolean,
      accountsWork: Boolean,
      localGamesWork: Boolean,
      normalChessBaselineRemains: Boolean,
      evenChessBoundaryExists: Boolean,
      harmlessModeFlagDisplays: Boolean,
      dummyServerAuthorizedOverlayWorksWithoutAdvice: Boolean,
      dummyAuditEventWrites: Boolean,
      aiMockExistsIfS18Completed: Boolean,
      patchMapUpdated: Boolean,
      goNoGoReportExists: Boolean
  ):
    def accepted: Boolean =
      localLilaBoots &&
        accountsWork &&
        localGamesWork &&
        normalChessBaselineRemains &&
        evenChessBoundaryExists &&
        harmlessModeFlagDisplays &&
        dummyServerAuthorizedOverlayWorksWithoutAdvice &&
        dummyAuditEventWrites &&
        aiMockExistsIfS18Completed &&
        patchMapUpdated &&
        goNoGoReportExists

  final case class RatedEvenChessAcceptanceEvidence(
      serverPolicyImplemented: Boolean,
      l0L10GatesPass: Boolean,
      assistanceUsedOffsetReplayPasses: Boolean,
      ecrIsolatedFromNormalRatings: Boolean,
      targetIsolationPasses: Boolean,
      offsetCountTestsPass: Boolean,
      engineSecurityPasses: Boolean,
      aiValidatorsPass: Boolean,
      clientCannotBypassPermission: Boolean,
      subscriptionsAdsTokensDoNotAffectFairness: Boolean,
      calibrationDashboardsPresent: Boolean,
      noHiddenContradictions: Boolean
  ):
    def accepted: Boolean =
      serverPolicyImplemented &&
        l0L10GatesPass &&
        assistanceUsedOffsetReplayPasses &&
        ecrIsolatedFromNormalRatings &&
        targetIsolationPasses &&
        offsetCountTestsPass &&
        engineSecurityPasses &&
        aiValidatorsPass &&
        clientCannotBypassPermission &&
        subscriptionsAdsTokensDoNotAffectFairness &&
        calibrationDashboardsPresent &&
        noHiddenContradictions

  final case class MarketingOpsAcceptanceEvidence(
      tokensCorrect: Boolean,
      adCapCorrect: Boolean,
      consumptionRefundCorrect: Boolean,
      pricingDisplayCorrect: Boolean,
      premiumNonStrength: Boolean,
      backendLandingConfigWorks: Boolean,
      utmEventsPresent: Boolean,
      killSwitchesWork: Boolean,
      unsafeCopyScanPasses: Boolean,
      eventDedupeWorks: Boolean,
      engineAiHealthVisible: Boolean,
      fallbackDegradedStatesWork: Boolean,
      staleClearingWorks: Boolean,
      noRateAnnulPathWorks: Boolean,
      rollbackWorks: Boolean,
      incidentPlaybookExists: Boolean,
      patchMapCurrent: Boolean
  ):
    def accepted: Boolean =
      tokensCorrect &&
        adCapCorrect &&
        consumptionRefundCorrect &&
        pricingDisplayCorrect &&
        premiumNonStrength &&
        backendLandingConfigWorks &&
        utmEventsPresent &&
        killSwitchesWork &&
        unsafeCopyScanPasses &&
        eventDedupeWorks &&
        engineAiHealthVisible &&
        fallbackDegradedStatesWork &&
        staleClearingWorks &&
        noRateAnnulPathWorks &&
        rollbackWorks &&
        incidentPlaybookExists &&
        patchMapCurrent

  final case class FinalAcceptanceEvidence(
      allAppendicesCurrent: Boolean,
      appendixZCurrent: Boolean,
      patchMapCurrent: Boolean,
      upstreamSyncDocumented: Boolean,
      fullRegressionPasses: Boolean,
      noContradictionUnreported: Boolean,
      normalChessSeparate: Boolean,
      evenChessInvariantsPass: Boolean,
      goNoGoApproved: Boolean
  ):
    def accepted: Boolean =
      allAppendicesCurrent &&
        appendixZCurrent &&
        patchMapCurrent &&
        upstreamSyncDocumented &&
        fullRegressionPasses &&
        noContradictionUnreported &&
        normalChessSeparate &&
        evenChessInvariantsPass &&
        goNoGoApproved

  final case class ReleaseReadiness(
      baseline: BaselineLichessRegression,
      crossCutting: CrossCuttingGateReport,
      stage1: Stage1AcceptanceEvidence,
      rated: RatedEvenChessAcceptanceEvidence,
      marketingOps: MarketingOpsAcceptanceEvidence,
      finalAcceptance: FinalAcceptanceEvidence
  ):
    def stage1Ready: Boolean =
      baseline.normalChessSafe && stage1.accepted

    def ratedReady: Boolean =
      stage1Ready && crossCutting.allRequiredPassed && rated.accepted

    def releaseReady: Boolean =
      ratedReady && marketingOps.accepted && finalAcceptance.accepted

  object QaPolicy:
    val localBootRequiredBeforeEvenChessFeatures = true
    val normalChessRegressionRequiredForUpstreamTouches = true
    val testsRequiredUnlessDocumentationOnly = true
    val hiddenContradictionsAllowed = false
    val normalChessMayBeReplaced = false

  enum PhaseSRegressionSurface:
    case NativeSetupSearch
    case EvenChessSearchContract
    case NormalLichessMechanics
    case EcrMmrSeparation
    case LevelGatedPayloads
    case DisplayEngineStaleRejection
    case OverlayVisuals
    case FixedSizeSummaryPlanCards
    case ProposedMoveSingleArrow
    case LiveReviewHistoryModes
    case CustomReviewTokens
    case FullGameEceSettlementNeutral

  final case class PhaseSRegressionEvidence(
      surface: PhaseSRegressionSurface,
      passed: Boolean,
      evidence: String,
      commandOrReason: String,
      rollbackNote: String
  ):
    def complete: Boolean =
      passed &&
        evidence.nonEmpty &&
        commandOrReason.nonEmpty &&
        rollbackNote.nonEmpty

  final case class PhaseSAcceptanceGateEvidence(
      testsRunOrDocumentedReason: Boolean,
      patchMapOrIntegrationLogCurrent: Boolean,
      noUnreportedInvariantConflicts: Boolean,
      noAccidentalNormalRatedPoolUse: Boolean,
      noClientSideCoachingPermissionDecision: Boolean,
      noApiKeysOrSecretsExposed: Boolean,
      desktopMobileBoardLayoutChecked: Boolean
  ):
    def accepted: Boolean =
      testsRunOrDocumentedReason &&
        patchMapOrIntegrationLogCurrent &&
        noUnreportedInvariantConflicts &&
        noAccidentalNormalRatedPoolUse &&
        noClientSideCoachingPermissionDecision &&
        noApiKeysOrSecretsExposed &&
        desktopMobileBoardLayoutChecked

  final case class PhaseSRegressionHardeningReport(
      evidence: List[PhaseSRegressionEvidence],
      gates: PhaseSAcceptanceGateEvidence
  ):
    def passed(surface: PhaseSRegressionSurface): Boolean =
      evidence.exists(item => item.surface == surface && item.complete)

    def coversEveryCoreSurface: Boolean =
      PhaseSRegressionSurface.values.forall(passed)

    def accepted: Boolean =
      coversEveryCoreSurface && gates.accepted

    def missingSurfaces: Set[PhaseSRegressionSurface] =
      PhaseSRegressionSurface.values.filterNot(passed).toSet
