package lila.evenchess

import ProductInvariants.RequirementClass

object Stage1LocalTesting:

  enum Stage1Requirement:
    case ArchitectureInspection
    case LocalLichessBoot
    case EvenChessModuleBoundary
    case ModeFlagOnly
    case BrandShell
    case DummyServerAuthorisedOverlay
    case AssistanceLedgerFoundation
    case AiSummaryMockProvider
    case GoNoGoReport
    case StopRules

  final case class Stage1RequirementClassification(
      requirement: Stage1Requirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object Stage1RequirementClassifications:
    val all: List[Stage1RequirementClassification] = List(
      Stage1RequirementClassification(
        Stage1Requirement.ArchitectureInspection,
        RequirementClass.AdaptedToLichessFork,
        "Inspect lila, lila-docker, module boundaries, routes, UI build path, tests, upstream SHA, and patch map before product hooks."
      ),
      Stage1RequirementClassification(
        Stage1Requirement.LocalLichessBoot,
        RequirementClass.LichessProvided,
        "Use lila-docker to prove the base Lichess site, accounts, legal moves, clocks, results, and history work locally."
      ),
      Stage1RequirementClassification(
        Stage1Requirement.EvenChessModuleBoundary,
        RequirementClass.EvenChessSpecific,
        "Keep Stage 1 EvenChess code in a separated namespace/module boundary without changing game behavior."
      ),
      Stage1RequirementClassification(
        Stage1Requirement.ModeFlagOnly,
        RequirementClass.AdaptedToLichessFork,
        "Add only harmless server-owned mode metadata in controlled dev paths; normal chess must remain unchanged."
      ),
      Stage1RequirementClassification(
        Stage1Requirement.BrandShell,
        RequirementClass.EvenChessSpecific,
        "Apply any blue badge/theme only to flagged EvenChess surfaces and avoid implying official Lichess endorsement."
      ),
      Stage1RequirementClassification(
        Stage1Requirement.DummyServerAuthorisedOverlay,
        RequirementClass.EvenChessSpecific,
        "Client may display only a non-advisory server payload; no engine, AI, or client-side coaching authority."
      ),
      Stage1RequirementClassification(
        Stage1Requirement.AssistanceLedgerFoundation,
        RequirementClass.EvenChessSpecific,
        "Create append-only dummy overlay render/suppression events before live coaching or AI/engine help."
      ),
      Stage1RequirementClassification(
        Stage1Requirement.AiSummaryMockProvider,
        RequirementClass.EvenChessSpecific,
        "Add server-side summary interface with mock first; optional provider credentials stay server-side and schema-validated."
      ),
      Stage1RequirementClassification(
        Stage1Requirement.GoNoGoReport,
        RequirementClass.EvenChessSpecific,
        "Record boot, account, game, boundary, mode, overlay, ledger, AI, patch map, tests, risks, and next-phase decision."
      ),
      Stage1RequirementClassification(
        Stage1Requirement.StopRules,
        RequirementClass.AdaptedToLichessFork,
        "Stop if local lila cannot boot, normal chess breaks, isolation fails, a broad rewrite is required, invariants conflict, or live engine/AI coaching is attempted too early."
      )
    )

  enum Stage1PhaseId:
    case ArchitectureInspection
    case LocalBoot
    case ModuleBoundary
    case ModeFlagOnly
    case BrandShell
    case DummyOverlay
    case LedgerFoundation
    case AiSummaryInterface
    case GoNoGoReport

  enum BehaviorImpact:
    case ReadOnly
    case NoGameBehaviorChange
    case FlaggedEvenChessOnly
    case NormalChessRegressionRequired

  final case class Stage1Phase(
      id: Stage1PhaseId,
      requirementId: String,
      deliverable: String,
      behaviorImpact: BehaviorImpact,
      mayTouchCoreLichess: Boolean,
      blocksFullProductUntilComplete: Boolean
  ):
    def isolatedEnoughForStage1: Boolean =
      behaviorImpact != BehaviorImpact.NormalChessRegressionRequired || mayTouchCoreLichess

    def requiresPatchMapIfImplemented: Boolean = mayTouchCoreLichess

  object Stage1Plan:
    val phases: List[Stage1Phase] = List(
      Stage1Phase(
        Stage1PhaseId.ArchitectureInspection,
        "STAGE1-L1-001",
        "docs/evenchess/stage1_architecture_inspection.md",
        BehaviorImpact.ReadOnly,
        mayTouchCoreLichess = false,
        blocksFullProductUntilComplete = true
      ),
      Stage1Phase(
        Stage1PhaseId.LocalBoot,
        "STAGE1-L1-010",
        "Local lila-docker boot and normal game smoke evidence.",
        BehaviorImpact.ReadOnly,
        mayTouchCoreLichess = false,
        blocksFullProductUntilComplete = true
      ),
      Stage1Phase(
        Stage1PhaseId.ModuleBoundary,
        "STAGE1-L1-020",
        "EvenChess namespace/module boundary.",
        BehaviorImpact.NoGameBehaviorChange,
        mayTouchCoreLichess = false,
        blocksFullProductUntilComplete = true
      ),
      Stage1Phase(
        Stage1PhaseId.ModeFlagOnly,
        "STAGE1-L1-030",
        "Harmless server-owned EvenChess mode flag/metadata in a controlled dev path.",
        BehaviorImpact.FlaggedEvenChessOnly,
        mayTouchCoreLichess = true,
        blocksFullProductUntilComplete = true
      ),
      Stage1Phase(
        Stage1PhaseId.BrandShell,
        "STAGE1-L1-040",
        "Blue EvenChess badge/theme on flagged surfaces only.",
        BehaviorImpact.FlaggedEvenChessOnly,
        mayTouchCoreLichess = true,
        blocksFullProductUntilComplete = false
      ),
      Stage1Phase(
        Stage1PhaseId.DummyOverlay,
        "STAGE1-L1-050",
        "Dummy non-advisory server-authorised overlay.",
        BehaviorImpact.FlaggedEvenChessOnly,
        mayTouchCoreLichess = true,
        blocksFullProductUntilComplete = true
      ),
      Stage1Phase(
        Stage1PhaseId.LedgerFoundation,
        "STAGE1-L1-060",
        "Append-only dummy overlay render/suppression event foundation.",
        BehaviorImpact.FlaggedEvenChessOnly,
        mayTouchCoreLichess = true,
        blocksFullProductUntilComplete = true
      ),
      Stage1Phase(
        Stage1PhaseId.AiSummaryInterface,
        "STAGE1-L1-070",
        "Server-side AI summary interface with mock provider first.",
        BehaviorImpact.FlaggedEvenChessOnly,
        mayTouchCoreLichess = false,
        blocksFullProductUntilComplete = false
      ),
      Stage1Phase(
        Stage1PhaseId.GoNoGoReport,
        "STAGE1-L1-080",
        "docs/evenchess/stage1_go_no_go.md",
        BehaviorImpact.ReadOnly,
        mayTouchCoreLichess = false,
        blocksFullProductUntilComplete = true
      )
    )

    val byId: Map[Stage1PhaseId, Stage1Phase] =
      phases.map(phase => phase.id -> phase).toMap

    def ordered: Boolean =
      phases.map(_.requirementId) == List(
        "STAGE1-L1-001",
        "STAGE1-L1-010",
        "STAGE1-L1-020",
        "STAGE1-L1-030",
        "STAGE1-L1-040",
        "STAGE1-L1-050",
        "STAGE1-L1-060",
        "STAGE1-L1-070",
        "STAGE1-L1-080"
      )

  enum SmokeTest:
    case SiteLoads
    case SeededAccountLogin
    case SeededAccountLogout
    case SecondAccountLogin
    case HumanGameStarts
    case LegalMovesWork
    case ClocksWork
    case ResultRecorded
    case ReviewHistoryOpens
    case ModuleBoundaryCompiles
    case PatchMapCurrent

  final case class SmokeTestEvidence(
      test: SmokeTest,
      passed: Boolean,
      evidence: String
  ):
    def complete: Boolean =
      passed && evidence.nonEmpty

  final case class Stage1SmokeReport(evidence: List[SmokeTestEvidence]):
    def passed(test: SmokeTest): Boolean =
      evidence.exists(item => item.test == test && item.complete)

    def requiredBaselinePassed: Boolean =
      Set(
        SmokeTest.SiteLoads,
        SmokeTest.SeededAccountLogin,
        SmokeTest.SeededAccountLogout,
        SmokeTest.SecondAccountLogin,
        SmokeTest.HumanGameStarts,
        SmokeTest.LegalMovesWork,
        SmokeTest.ClocksWork,
        SmokeTest.ModuleBoundaryCompiles,
        SmokeTest.PatchMapCurrent
      ).forall(passed)

  enum StopReason:
    case LocalLilaDoesNotBoot
    case NormalChessBreaks
    case EvenChessIsolationFails
    case BroadCoreRewriteRequired
    case RequirementConflictsWithInvariant
    case LiveEngineAiCoachingBeforeLedgerFoundation

  final case class Stage1ChangeProposal(
      localLilaBoots: Boolean,
      normalChessStillWorks: Boolean,
      evenChessCanRemainIsolated: Boolean,
      broadCoreRewriteRequired: Boolean,
      requirementConflict: Boolean,
      attemptsLiveEngineAiCoaching: Boolean,
      ledgerFoundationComplete: Boolean
  ):
    def stopReasons: Set[StopReason] =
      Set(
        Option.when(!localLilaBoots)(StopReason.LocalLilaDoesNotBoot),
        Option.when(!normalChessStillWorks)(StopReason.NormalChessBreaks),
        Option.when(!evenChessCanRemainIsolated)(StopReason.EvenChessIsolationFails),
        Option.when(broadCoreRewriteRequired)(StopReason.BroadCoreRewriteRequired),
        Option.when(requirementConflict)(StopReason.RequirementConflictsWithInvariant),
        Option.when(attemptsLiveEngineAiCoaching && !ledgerFoundationComplete)(StopReason.LiveEngineAiCoachingBeforeLedgerFoundation)
      ).flatten

    def mayProceed: Boolean = stopReasons.isEmpty

  final case class Stage1GoNoGoEvidence(
      smokeReport: Stage1SmokeReport,
      architectureInspectionRecorded: Boolean,
      moduleBoundaryCreated: Boolean,
      modeFlagSafe: Boolean,
      overlayServerAuthorised: Boolean,
      ledgerAppendOnly: Boolean,
      aiProviderMockFirst: Boolean,
      patchMapCurrent: Boolean,
      risksRecorded: Boolean
  ):
    def go: Boolean =
      smokeReport.requiredBaselinePassed &&
        architectureInspectionRecorded &&
        moduleBoundaryCreated &&
        modeFlagSafe &&
        overlayServerAuthorised &&
        ledgerAppendOnly &&
        aiProviderMockFirst &&
        patchMapCurrent &&
        risksRecorded

  object LocalDeploymentPath:
    val deployableBeforeStage1Go = false
    val normalLichessBaselineRequired = true
    val fullProductImplementationBeforeLocalBaseline = false
    val destructiveLocalCommandsAllowedByDefault = false
