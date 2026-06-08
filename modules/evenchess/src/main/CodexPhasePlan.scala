package lila.evenchess

import ProductInvariants.RequirementClass
import Stage1LocalTesting.Stage1PhaseId

object CodexPhasePlan:

  enum CodexRequirement:
    case RequestedPhaseOnly
    case RequiredReading
    case TestsUnlessDocumentationOnly
    case CompletionReportAndInvariantChecks
    case PatchMapForUpstreamTouches
    case Stage1TaskPackets
    case PostStage1PhaseFamilies
    case CompletionReportTemplate

  final case class CodexRequirementClassification(
      requirement: CodexRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object CodexRequirementClassifications:
    val all: List[CodexRequirementClassification] = List(
      CodexRequirementClassification(
        CodexRequirement.RequestedPhaseOnly,
        RequirementClass.EvenChessSpecific,
        "Codex may implement only the requested EvenChess-Lichess phase or task packet."
      ),
      CodexRequirementClassification(
        CodexRequirement.RequiredReading,
        RequirementClass.AdaptedToLichessFork,
        "Each phase reads AGENTS, main requirements, relevant appendices, Stage 1 handover, diff, patch map, and upstream sync rules."
      ),
      CodexRequirementClassification(
        CodexRequirement.TestsUnlessDocumentationOnly,
        RequirementClass.EvenChessSpecific,
        "Each implementation phase adds or updates tests unless the phase is explicitly documentation-only."
      ),
      CodexRequirementClassification(
        CodexRequirement.CompletionReportAndInvariantChecks,
        RequirementClass.EvenChessSpecific,
        "Each phase prints a completion report with invariant, normal chess, and EvenChess regression status."
      ),
      CodexRequirementClassification(
        CodexRequirement.PatchMapForUpstreamTouches,
        RequirementClass.AdaptedToLichessFork,
        "Any upstream/core Lichess file edit requires a patch-map entry before completion."
      ),
      CodexRequirementClassification(
        CodexRequirement.Stage1TaskPackets,
        RequirementClass.EvenChessSpecific,
        "Stage 1 task packets sequence local proof, boundary, harmless flag, shell, overlay, ledger, AI mock, and go/no-go."
      ),
      CodexRequirementClassification(
        CodexRequirement.PostStage1PhaseFamilies,
        RequirementClass.EvenChessSpecific,
        "Post-Stage 1 phase families route later work to the relevant appendices without authorizing implementation early."
      ),
      CodexRequirementClassification(
        CodexRequirement.CompletionReportTemplate,
        RequirementClass.EvenChessSpecific,
        "Completion reports must include scope, files, patch map, tests, invariant checks, regressions, unresolved items, risks, and next-phase readiness."
      )
    )

  enum RequiredDocument:
    case Agents
    case MainRequirements
    case Stage1Handover
    case RelevantAppendix
    case RequirementsDiff
    case PatchMap
    case UpstreamSyncProcess

  object RequiredReading:
    val baseline: Set[RequiredDocument] = Set(
      RequiredDocument.Agents,
      RequiredDocument.MainRequirements,
      RequiredDocument.Stage1Handover,
      RequiredDocument.RelevantAppendix,
      RequiredDocument.RequirementsDiff,
      RequiredDocument.PatchMap,
      RequiredDocument.UpstreamSyncProcess
    )

    def complete(read: Set[RequiredDocument]): Boolean =
      baseline.subsetOf(read)

  enum Stage1PacketId:
    case S1_1
    case S1_2
    case S1_3
    case S1_4
    case S1_5
    case S1_6
    case S1_7
    case S1_8
    case S1_9

  final case class Stage1TaskPacket(
      id: Stage1PacketId,
      phase: Stage1PhaseId,
      title: String,
      promptStarter: String,
      requiredAppendices: Set[String],
      allowsProductFeatures: Boolean,
      requiresPatchMapForCoreTouches: Boolean
  ):
    def valid: Boolean =
      title.nonEmpty &&
        promptStarter.nonEmpty &&
        requiredAppendices.contains("T") &&
        (!allowsProductFeatures || id != Stage1PacketId.S1_1) &&
        requiresPatchMapForCoreTouches

  object Stage1TaskPackets:
    val all: List[Stage1TaskPacket] = List(
      Stage1TaskPacket(
        Stage1PacketId.S1_1,
        Stage1PhaseId.ArchitectureInspection,
        "Architecture inspection",
        "Implement Stage 1.1 only. Read main, B, C, S, T, AGENTS, patch map. Inspect lila/lila-docker. No product features. Produce architecture inspection.",
        Set("B", "C", "S", "T"),
        allowsProductFeatures = false,
        requiresPatchMapForCoreTouches = true
      ),
      Stage1TaskPacket(
        Stage1PacketId.S1_2,
        Stage1PhaseId.LocalBoot,
        "Local boot",
        "Boot local Lichess via lila-docker or verified path. Confirm accounts and games. Record commands/errors/fixes.",
        Set("S", "T"),
        allowsProductFeatures = false,
        requiresPatchMapForCoreTouches = true
      ),
      Stage1TaskPacket(
        Stage1PacketId.S1_3,
        Stage1PhaseId.ModuleBoundary,
        "Module boundary",
        "Create minimal EvenChess namespace/module boundary. No game behavior changes. Patch-map upstream touches.",
        Set("B", "S", "T"),
        allowsProductFeatures = false,
        requiresPatchMapForCoreTouches = true
      ),
      Stage1TaskPacket(
        Stage1PacketId.S1_4,
        Stage1PhaseId.ModeFlagOnly,
        "Mode flag",
        "Add harmless server-owned EvenChess mode flag/metadata only. No coaching/rating/matchmaking/tokens.",
        Set("D", "S", "T"),
        allowsProductFeatures = true,
        requiresPatchMapForCoreTouches = true
      ),
      Stage1TaskPacket(
        Stage1PacketId.S1_5,
        Stage1PhaseId.BrandShell,
        "Blue shell",
        "Add simple blue EvenChess badge/theme for flagged surfaces only.",
        Set("D", "F", "S", "T"),
        allowsProductFeatures = true,
        requiresPatchMapForCoreTouches = true
      ),
      Stage1TaskPacket(
        Stage1PacketId.S1_6,
        Stage1PhaseId.DummyOverlay,
        "Dummy overlay",
        "Add dummy non-advisory server-authorized overlay. No engine/AI.",
        Set("F", "G", "S", "T"),
        allowsProductFeatures = true,
        requiresPatchMapForCoreTouches = true
      ),
      Stage1TaskPacket(
        Stage1PacketId.S1_7,
        Stage1PhaseId.LedgerFoundation,
        "Ledger foundation",
        "Add append-only audit event foundation for dummy overlay.",
        Set("G", "I", "S", "T"),
        allowsProductFeatures = true,
        requiresPatchMapForCoreTouches = true
      ),
      Stage1TaskPacket(
        Stage1PacketId.S1_8,
        Stage1PhaseId.AiSummaryInterface,
        "AI mock/provider",
        "Define server-side AI provider interface and mock; optional provider only server-side/configurable.",
        Set("M", "S", "T"),
        allowsProductFeatures = true,
        requiresPatchMapForCoreTouches = true
      ),
      Stage1TaskPacket(
        Stage1PacketId.S1_9,
        Stage1PhaseId.GoNoGoReport,
        "Go/no-go",
        "Produce stage1_go_no_go.md with status, tests, risks, next phase.",
        Set("S", "T"),
        allowsProductFeatures = false,
        requiresPatchMapForCoreTouches = true
      )
    )

    val orderedIds: List[Stage1PacketId] =
      List(
        Stage1PacketId.S1_1,
        Stage1PacketId.S1_2,
        Stage1PacketId.S1_3,
        Stage1PacketId.S1_4,
        Stage1PacketId.S1_5,
        Stage1PacketId.S1_6,
        Stage1PacketId.S1_7,
        Stage1PacketId.S1_8,
        Stage1PacketId.S1_9
      )

    def ordered: Boolean =
      all.map(_.id) == orderedIds

    def byId(id: Stage1PacketId): Option[Stage1TaskPacket] =
      all.find(_.id == id)

  enum PostStage1PhaseId:
    case P2
    case P3
    case P4
    case P5
    case P6
    case P7
    case P8
    case P9
    case P10
    case P11
    case P12
    case P13
    case P14

  final case class PhaseFamily(
      id: PostStage1PhaseId,
      purpose: String,
      appendices: Set[String]
  ):
    def valid: Boolean = purpose.nonEmpty && appendices.nonEmpty

  object PostStage1PhaseFamilies:
    val all: List[PhaseFamily] = List(
      PhaseFamily(PostStage1PhaseId.P2, "Source-of-truth hardening", Set("Main", "B", "Z")),
      PhaseFamily(PostStage1PhaseId.P3, "Feature registry and policy", Set("E", "G")),
      PhaseFamily(PostStage1PhaseId.P4, "Offset Count", Set("H", "I", "V")),
      PhaseFamily(PostStage1PhaseId.P5, "Overlay primitives", Set("F", "G", "E")),
      PhaseFamily(PostStage1PhaseId.P6, "Assistance accounting", Set("I", "P", "U")),
      PhaseFamily(PostStage1PhaseId.P7, "Engine gateway", Set("L", "G", "V")),
      PhaseFamily(PostStage1PhaseId.P8, "AI wording", Set("M", "G", "V")),
      PhaseFamily(PostStage1PhaseId.P9, "Post-game summaries", Set("M", "N", "P")),
      PhaseFamily(PostStage1PhaseId.P10, "ECR and matchmaking", Set("J", "K", "P")),
      PhaseFamily(PostStage1PhaseId.P11, "Monetisation", Set("N", "Q", "O")),
      PhaseFamily(PostStage1PhaseId.P12, "Marketing/funnel", Set("O", "P", "R")),
      PhaseFamily(PostStage1PhaseId.P13, "Abuse/ops", Set("Q", "R")),
      PhaseFamily(PostStage1PhaseId.P14, "Release hardening", Set("V", "R", "Z"))
    )

    def coversAllFamilies: Boolean =
      PostStage1PhaseId.values.toSet.subsetOf(all.map(_.id).toSet) && all.forall(_.valid)

    def appendicesFor(id: PostStage1PhaseId): Set[String] =
      all.find(_.id == id).map(_.appendices).getOrElse(Set.empty)

  final case class PhaseExecution(
      requestedPacket: Stage1PacketId,
      attemptedPackets: Set[Stage1PacketId],
      readDocuments: Set[RequiredDocument],
      documentationOnly: Boolean,
      testsAddedOrUpdated: Boolean,
      upstreamFilesTouched: Boolean,
      patchMapUpdated: Boolean,
      completionReportFields: Set[CompletionReportField]
  ):
    def onlyRequestedPacket: Boolean =
      attemptedPackets == Set(requestedPacket)

    def hasRequiredReading: Boolean =
      RequiredReading.complete(readDocuments)

    def satisfiesTestRule: Boolean =
      documentationOnly || testsAddedOrUpdated

    def satisfiesPatchMapRule: Boolean =
      !upstreamFilesTouched || patchMapUpdated

    def complete: Boolean =
      onlyRequestedPacket &&
        hasRequiredReading &&
        satisfiesTestRule &&
        satisfiesPatchMapRule &&
        CompletionReportTemplate.complete(completionReportFields)

  enum CompletionReportField:
    case Phase
    case ScopeCompleted
    case FilesChanged
    case UpstreamLichessFilesTouched
    case PatchMapEntries
    case TestsAddedOrUpdated
    case TestsRun
    case InvariantChecks
    case NormalChessRegressionStatus
    case EvenChessModeRegressionStatus
    case UnresolvedItems
    case Risks
    case ReadyForNextPhase

  object CompletionReportTemplate:
    val requiredFields: Set[CompletionReportField] = CompletionReportField.values.toSet

    def complete(fields: Set[CompletionReportField]): Boolean =
      requiredFields.subsetOf(fields)

  object PhaseSafety:
    val oldAZPlanSuperseded = true
    val futurePhaseFamiliesAuthorizeImplementationNow = false
    val broadUnrequestedWorkAllowed = false
