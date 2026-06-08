package lila.evenchess

import ProductInvariants.RequirementClass

object CoachingLadder:

  final case class Level(value: Int):
    require(Level.isValid(value), s"EvenChess level must be L0-L10, got L$value")

  object Level:
    val min = 0
    val max = 10

    def isValid(value: Int): Boolean = value >= min && value <= max

  enum SourceType:
    case None
    case HardCodedExact
    case HardCodedLocal
    case HardCodedSeeLocal
    case HardCodedHybrid
    case HybridHeuristic
    case HybridStockfish
    case Stockfish
    case StockfishAiWording
    case AiOverTruthPackets
    case Mixed

  enum ExactnessClass:
    case None
    case ExactRules
    case LocalDeterministic
    case LocalEstimate
    case Heuristic
    case Approximate
    case ExactWhereConfigured
    case Mixed
    case AiMustValidate

  enum UiSlot:
    case Board
    case BoardLayer
    case MoveList
    case ResultState
    case Summary
    case Warning
    case OffsetCard
    case PlanCard
    case OpeningEndgameCard
    case CandidateArea
    case CoachReview
    case AiExplain
    case Review

  enum FeatureCategory:
    case Baseline
    case RulesClarity
    case LocalAwareness
    case ExchangeCalculation
    case MotifAwareness
    case Summary
    case Plan
    case EngineCandidate
    case Evaluation
    case Review

  enum SubscriptionVisibility:
    case AlwaysVisible
    case MarketingOrQuotaOnly
    case ReviewOrQuotaOnly

  final case class LadderLevel(
      level: Level,
      name: String,
      purpose: String,
      allowedSurfaces: List[UiSlot],
      sourceExactness: String,
      baseWeight: Int,
      advisoryCoaching: Boolean,
      liveEngineBackedCandidateCount: Int,
      numericEvalOrWdl: Boolean,
      numericEvalApproximateLabelRequired: Boolean,
      compactByDefault: Boolean,
      ratedAllowed: Boolean,
      classification: RequirementClass
  ):
    def key: String = s"L${level.value}"

  object Levels:
    val all: List[LadderLevel] = List(
      LadderLevel(
        Level(0),
        "Standard Board",
        "No coaching baseline",
        List(UiSlot.Board, UiSlot.MoveList, UiSlot.ResultState),
        "None",
        0,
        advisoryCoaching = false,
        liveEngineBackedCandidateCount = 0,
        numericEvalOrWdl = false,
        numericEvalApproximateLabelRequired = false,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.LichessProvided
      ),
      LadderLevel(
        Level(1),
        "Legal Moves",
        "Rules clarity",
        List(UiSlot.BoardLayer),
        "Hard-coded exact rules",
        2,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 0,
        numericEvalOrWdl = false,
        numericEvalApproximateLabelRequired = false,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.AdaptedToLichessFork
      ),
      LadderLevel(
        Level(2),
        "Safety Scanner",
        "Local attack/defence awareness",
        List(UiSlot.BoardLayer, UiSlot.Warning),
        "Local deterministic",
        5,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 0,
        numericEvalOrWdl = false,
        numericEvalApproximateLabelRequired = false,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.EvenChessSpecific
      ),
      LadderLevel(
        Level(3),
        "Offset Count",
        "Exchange calculation",
        List(UiSlot.OffsetCard),
        "Local exchange estimate",
        9,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 0,
        numericEvalOrWdl = false,
        numericEvalApproximateLabelRequired = false,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.AdaptedToLichessFork
      ),
      LadderLevel(
        Level(4),
        "Pattern Coach",
        "Motif awareness without engine advice",
        List(UiSlot.BoardLayer, UiSlot.Warning),
        "Heuristic/local motif; AI may compress facts",
        14,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 0,
        numericEvalOrWdl = false,
        numericEvalApproximateLabelRequired = false,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.EvenChessSpecific
      ),
      LadderLevel(
        Level(5),
        "Single Hint",
        "One on-demand engine-backed candidate",
        List(UiSlot.CandidateArea),
        "Bounded Stockfish MultiPV=1",
        20,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 1,
        numericEvalOrWdl = false,
        numericEvalApproximateLabelRequired = false,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.EvenChessSpecific
      ),
      LadderLevel(
        Level(6),
        "Choice Coach",
        "Two-candidate comparison",
        List(UiSlot.CandidateArea),
        "Bounded Stockfish MultiPV=2",
        28,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 2,
        numericEvalOrWdl = false,
        numericEvalApproximateLabelRequired = false,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.EvenChessSpecific
      ),
      LadderLevel(
        Level(7),
        "Guided Engine",
        "Three candidates and compact plan cue",
        List(UiSlot.CandidateArea, UiSlot.PlanCard),
        "Bounded Stockfish MultiPV=3",
        37,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 3,
        numericEvalOrWdl = false,
        numericEvalApproximateLabelRequired = false,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.EvenChessSpecific
      ),
      LadderLevel(
        Level(8),
        "Precision Engine",
        "Numeric eval and exact proof badges",
        List(UiSlot.CandidateArea, UiSlot.Summary),
        "Approx eval; exact tablebase where configured",
        47,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 3,
        numericEvalOrWdl = true,
        numericEvalApproximateLabelRequired = true,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.EvenChessSpecific
      ),
      LadderLevel(
        Level(9),
        "Expert Sparring",
        "Deeper contrast and why-not analysis",
        List(UiSlot.CandidateArea),
        "Bounded MultiPV=4/searchmoves",
        58,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 4,
        numericEvalOrWdl = true,
        numericEvalApproximateLabelRequired = true,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.EvenChessSpecific
      ),
      LadderLevel(
        Level(10),
        "Full Co-pilot",
        "Maximum disclosed assistance in stable UI",
        List(UiSlot.CandidateArea, UiSlot.PlanCard),
        "Dynamic bounded profiles; richest specificity",
        70,
        advisoryCoaching = true,
        liveEngineBackedCandidateCount = 4,
        numericEvalOrWdl = true,
        numericEvalApproximateLabelRequired = true,
        compactByDefault = true,
        ratedAllowed = true,
        RequirementClass.EvenChessSpecific
      )
    )

    val byValue: Map[Int, LadderLevel] = all.map(level => level.level.value -> level).toMap

    val firstLiveEngineCandidateLevel: Level =
      all.filter(_.liveEngineBackedCandidateCount > 0).minBy(_.level.value).level

    val firstNumericEvalLevel: Level =
      all.filter(_.numericEvalOrWdl).minBy(_.level.value).level

  object LadderRules:
    val higherLevelsImproveSpecificityAndTimingNotTextVolume = true
    val allPublicLevelsMayBeRatedWhenAssignedOrAllowed = true
    val unrestrictedRawEngineAccessAllowed = false
    val liveRatedBestMoveLabelApproved = false

  final case class FeatureRegistryRow(
      featureKey: String,
      displayName: String,
      unlockLevel: Level,
      category: FeatureCategory,
      sourceType: SourceType,
      exactnessClass: ExactnessClass,
      uiSlot: UiSlot,
      assistanceWeight: Int,
      auditRequired: Boolean,
      telemetryRequired: Boolean,
      ratedAllowed: Boolean,
      subscriptionVisibility: SubscriptionVisibility,
      implementationNotes: String,
      testsRequired: String
  )

  object FeatureRegistry:
    val all: List[FeatureRegistryRow] = List(
      row("move_history", "Move history", 0, FeatureCategory.Baseline, SourceType.HardCodedExact, ExactnessClass.ExactRules, UiSlot.MoveList, 0, "Standard move list and PGN basis."),
      row("legal_targets", "Legal targets", 1, FeatureCategory.RulesClarity, SourceType.HardCodedExact, ExactnessClass.ExactRules, UiSlot.BoardLayer, 2, "Legal move dots/highlights."),
      row("material_panel", "Material panel", 1, FeatureCategory.RulesClarity, SourceType.HardCodedLocal, ExactnessClass.LocalDeterministic, UiSlot.Summary, 2, "Displays material state; not engine advice. Extends into L2."),
      row("loose_pieces", "Loose pieces", 2, FeatureCategory.LocalAwareness, SourceType.HardCodedLocal, ExactnessClass.LocalDeterministic, UiSlot.Warning, 5, "Undefended/tactically loose cue."),
      row("king_safety", "King safety", 2, FeatureCategory.LocalAwareness, SourceType.HardCodedHybrid, ExactnessClass.Heuristic, UiSlot.Warning, 5, "Simple king danger. Extends through L4."),
      row("offset_count", "Offset Count", 3, FeatureCategory.ExchangeCalculation, SourceType.HardCodedSeeLocal, ExactnessClass.LocalEstimate, UiSlot.OffsetCard, 9, "Existing Exchange Resolver; shield/blue/green/red semantics."),
      row("pins", "Pins", 4, FeatureCategory.MotifAwareness, SourceType.HardCodedHybrid, ExactnessClass.Mixed, UiSlot.Warning, 14, "Absolute pins affect legality; relative pins are warnings."),
      row("x_rays", "X-rays", 4, FeatureCategory.MotifAwareness, SourceType.HardCodedHybrid, ExactnessClass.Mixed, UiSlot.OffsetCard, 14, "Relevant to Offset Count and motifs. Extends into L5."),
      row("student_threats", "Student threats", 4, FeatureCategory.MotifAwareness, SourceType.HybridHeuristic, ExactnessClass.Heuristic, UiSlot.PlanCard, 14, "Threats without exact move below allowed level."),
      row("opponent_threats", "Opponent threats", 4, FeatureCategory.MotifAwareness, SourceType.HybridStockfish, ExactnessClass.Mixed, UiSlot.Warning, 14, "Higher impact under clock pressure."),
      row("pressure_markers", "Pressure markers", 4, FeatureCategory.MotifAwareness, SourceType.HardCodedHybrid, ExactnessClass.Heuristic, UiSlot.PlanCard, 14, "File/diagonal/square pressure."),
      row("summary", "Summary", 1, FeatureCategory.Summary, SourceType.Mixed, ExactnessClass.Mixed, UiSlot.Summary, 2, "Compact coach headline and game state."),
      row("plan", "Plan", 4, FeatureCategory.Plan, SourceType.AiOverTruthPackets, ExactnessClass.AiMustValidate, UiSlot.PlanCard, 14, "No AI invention."),
      row("opening_endgame", "Opening/endgame", 4, FeatureCategory.MotifAwareness, SourceType.HardCodedHybrid, ExactnessClass.ExactWhereConfigured, UiSlot.OpeningEndgameCard, 14, "Tablebase exactness only where source exists."),
      row("coarse_eval_band", "Coarse eval band", 6, FeatureCategory.Evaluation, SourceType.Stockfish, ExactnessClass.Approximate, UiSlot.Summary, 28, "No raw CP until L8."),
      row("move_advice", "Move advice", 5, FeatureCategory.EngineCandidate, SourceType.Stockfish, ExactnessClass.Approximate, UiSlot.CandidateArea, 20, "First exact candidate at L5; avoid best-move label in rated live."),
      row("candidate_cards", "Candidate cards", 5, FeatureCategory.EngineCandidate, SourceType.StockfishAiWording, ExactnessClass.Mixed, UiSlot.CandidateArea, 20, "Candidate count level-gated."),
      row("move_pool", "Move pool", 6, FeatureCategory.EngineCandidate, SourceType.Stockfish, ExactnessClass.Approximate, UiSlot.CandidateArea, 28, "No raw unrestricted engine access."),
      row("eval_difference", "Eval difference", 8, FeatureCategory.Evaluation, SourceType.Stockfish, ExactnessClass.Approximate, UiSlot.AiExplain, 47, "Label approximate."),
      row("themes", "Themes", 4, FeatureCategory.Review, SourceType.AiOverTruthPackets, ExactnessClass.AiMustValidate, UiSlot.CoachReview, 14, "Used in review/drills."),
      row("warnings", "Warnings", 2, FeatureCategory.LocalAwareness, SourceType.HybridStockfish, ExactnessClass.Mixed, UiSlot.Warning, 5, "Priority over summary/plan."),
      row("ai_explain", "AI explain", 5, FeatureCategory.Plan, SourceType.AiOverTruthPackets, ExactnessClass.AiMustValidate, UiSlot.AiExplain, 20, "Must validate and cannot invent. Also allowed in review."),
      row("post_game_review", "Post-game review", 0, FeatureCategory.Review, SourceType.Mixed, ExactnessClass.Mixed, UiSlot.Review, 0, "Rating-neutral after game.")
    )

    val byKey: Map[String, FeatureRegistryRow] = all.map(feature => feature.featureKey -> feature).toMap

    private def row(
        featureKey: String,
        displayName: String,
        unlockLevel: Int,
        category: FeatureCategory,
        sourceType: SourceType,
        exactnessClass: ExactnessClass,
        uiSlot: UiSlot,
        assistanceWeight: Int,
        implementationNotes: String
    ): FeatureRegistryRow =
      FeatureRegistryRow(
        featureKey,
        displayName,
        Level(unlockLevel),
        category,
        sourceType,
        exactnessClass,
        uiSlot,
        assistanceWeight,
        auditRequired = true,
        telemetryRequired = true,
        ratedAllowed = true,
        SubscriptionVisibility.AlwaysVisible,
        implementationNotes,
        testsRequired = "Offline registry and policy tests."
      )

  final case class FeaturePolicyInput(
      featureKey: String,
      setLevel: Level,
      rated: Boolean,
      serverAuthorized: Boolean,
      clientRequested: Boolean
  )

  object FeaturePolicy:
    val clientCanSelfEnableRatedFeatures = false
    val subscriptionVisibilityMayChangeRatedLiveStrength = false

    def canEnable(input: FeaturePolicyInput): Boolean =
      FeatureRegistry.byKey.get(input.featureKey).exists { feature =>
        val levelAllowsFeature = input.setLevel.value >= feature.unlockLevel.value
        val ratedMetadataOk =
          !input.rated || (feature.ratedAllowed && feature.auditRequired && feature.telemetryRequired)

        input.serverAuthorized && levelAllowsFeature && ratedMetadataOk
      }

    def canEnableFromClientOnly(input: FeaturePolicyInput): Boolean =
      input.clientRequested && !input.serverAuthorized && clientCanSelfEnableRatedFeatures

  object ForkAdaptation:
    val featuresAreServerAuthorizedPayloads = true
    val chessgroundOverlayPayloadsRequireBoardHashFenPlyValidation = true
    val registryLivesInEvenChessConfigOrService = true
    val registryIsOfflineTestableWithPolicyInputs = true
