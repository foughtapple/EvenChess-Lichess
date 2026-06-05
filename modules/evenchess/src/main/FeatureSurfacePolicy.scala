package lila.evenchess

import CoachingLadder.Level
import LiveCoaching.{ CustomReviewPlan, ReviewMode }
import ProductInvariants.RequirementClass

object FeatureSurfacePolicy:

  enum PhaseORequirement:
    case UseLichessPuzzleFoundations
    case PuzzleOverlaysTrainingOnly
    case PuzzleRatingDistinctFromEcr
    case UseLichessStudyFoundations
    case StudyOverlaysReviewTrainingOnly
    case StudyDoesNotAffectLiveEcr
    case UseLichessOpeningFoundations
    case OpeningGuidanceFenOpeningAndLevelGated
    case UseLichessAnalysisReplayFoundations
    case AnalysisLayersSavedOrCustomEceReview
    case UseLichessComputerPlayFoundations
    case ComputerPlayTrainingReviewDefault
    case ComputerPlayDoesNotCorruptNormalEcr

  final case class PhaseORequirementClassification(
      requirement: PhaseORequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseORequirementClassifications:
    val all: List[PhaseORequirementClassification] = List(
      PhaseORequirementClassification(
        PhaseORequirement.UseLichessPuzzleFoundations,
        RequirementClass.LichessProvided,
        "Reuse native Lichess puzzle/training foundations; do not rebuild puzzle generation, scoring, or solving."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.PuzzleOverlaysTrainingOnly,
        RequirementClass.EvenChessSpecific,
        "EvenChess puzzle overlays are training display only and are suppressed in live rated contexts."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.PuzzleRatingDistinctFromEcr,
        RequirementClass.EvenChessSpecific,
        "Puzzle rating labels must remain distinct from ECR unless a later explicit integration says otherwise."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.UseLichessStudyFoundations,
        RequirementClass.LichessProvided,
        "Reuse native Lichess study boards, chapters, and collaboration foundations."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.StudyOverlaysReviewTrainingOnly,
        RequirementClass.EvenChessSpecific,
        "EvenChess study cards may render only as review/training overlays governed by settings and product rules."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.StudyDoesNotAffectLiveEcr,
        RequirementClass.EvenChessSpecific,
        "Study usage must not mutate live rated ECR, Used Level, Assistance Load, or Used Offset."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.UseLichessOpeningFoundations,
        RequirementClass.LichessProvided,
        "Reuse native Lichess opening explorer/book foundations where available."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.OpeningGuidanceFenOpeningAndLevelGated,
        RequirementClass.EvenChessSpecific,
        "Opening guidance may prepare numeric ECE custom opening context only from current FEN, requested opening input, and allowed level."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.UseLichessAnalysisReplayFoundations,
        RequirementClass.LichessProvided,
        "Reuse native Lichess analysis and replay foundations."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.AnalysisLayersSavedOrCustomEceReview,
        RequirementClass.AdaptedToLichessFork,
        "Analysis/replay should layer saved live ECE history or valid custom ECE review plans onto the existing surface."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.UseLichessComputerPlayFoundations,
        RequirementClass.LichessProvided,
        "Reuse native Lichess computer-play foundations where feasible."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.ComputerPlayTrainingReviewDefault,
        RequirementClass.AdaptedToLichessFork,
        "Computer games are training/review by default unless a later separate rated mode is explicitly defined."
      ),
      PhaseORequirementClassification(
        PhaseORequirement.ComputerPlayDoesNotCorruptNormalEcr,
        RequirementClass.EvenChessSpecific,
        "Computer-play overlays must not corrupt normal EvenChess ECR or live rated settlement."
      )
    )

  enum FeatureSurface:
    case PuzzleTraining
    case StudyBoard
    case OpeningExplorer
    case AnalysisReplay
    case ComputerPlay

    def clientKey: String = this match
      case PuzzleTraining  => "puzzle"
      case StudyBoard      => "study"
      case OpeningExplorer => "opening"
      case AnalysisReplay  => "analysis"
      case ComputerPlay    => "computer"

  enum SurfaceUseMode:
    case Training
    case Review
    case LiveRated
    case ComputerTraining

  final case class FeatureSurfaceFoundation(
      surface: FeatureSurface,
      lichessArea: String,
      reused: Boolean,
      rebuildAllowed: Boolean,
      classification: RequirementClass
  )

  object Foundations:
    val all: List[FeatureSurfaceFoundation] = List(
      FeatureSurfaceFoundation(
        FeatureSurface.PuzzleTraining,
        "puzzle/training",
        reused = true,
        rebuildAllowed = false,
        RequirementClass.LichessProvided
      ),
      FeatureSurfaceFoundation(
        FeatureSurface.StudyBoard,
        "study",
        reused = true,
        rebuildAllowed = false,
        RequirementClass.LichessProvided
      ),
      FeatureSurfaceFoundation(
        FeatureSurface.OpeningExplorer,
        "opening explorer/book",
        reused = true,
        rebuildAllowed = false,
        RequirementClass.LichessProvided
      ),
      FeatureSurfaceFoundation(
        FeatureSurface.AnalysisReplay,
        "analysis/replay",
        reused = true,
        rebuildAllowed = false,
        RequirementClass.LichessProvided
      ),
      FeatureSurfaceFoundation(
        FeatureSurface.ComputerPlay,
        "computer play",
        reused = true,
        rebuildAllowed = false,
        RequirementClass.LichessProvided
      )
    )

    val bySurface: Map[FeatureSurface, FeatureSurfaceFoundation] =
      all.map(foundation => foundation.surface -> foundation).toMap

  final case class FeatureSurfaceContext(
      surface: FeatureSurface,
      mode: SurfaceUseMode,
      level: Level,
      currentFen: Option[String],
      requestedOpening: Option[Int],
      reviewMode: Option[ReviewMode],
      customReviewPlan: Option[CustomReviewPlan],
      liveHistoryAvailable: Boolean,
      settingAllowsOverlays: Boolean,
      productRuleAllowsOverlays: Boolean,
      puzzleRatingShown: Boolean,
      labelsPuzzleRatingAsEcr: Boolean,
      separateRatedComputerModeDefined: Boolean
  ):
    def foundation: FeatureSurfaceFoundation =
      Foundations.bySurface(surface)

    def settingsAndProductAllow: Boolean =
      settingAllowsOverlays && productRuleAllowsOverlays

    def puzzleRatingDistinctFromEcr: Boolean =
      !puzzleRatingShown || !labelsPuzzleRatingAsEcr

    def hasCurrentFen: Boolean =
      currentFen.exists(_.nonEmpty)

    def requestedOpeningProfile: Option[Int] =
      requestedOpening.filter(_ > 0)

    def validCustomReviewPlan: Boolean =
      customReviewPlan.exists(_.valid)

  final case class FeatureSurfaceDecision(
      surface: FeatureSurface,
      usesLichessFoundation: Boolean,
      rebuildsLichessFeature: Boolean,
      overlayEligible: Boolean,
      eceRequestAllowed: Boolean,
      eceCustomProfile: EngineGateway.EceCustomProfile,
      compactGuidanceRequired: Boolean,
      mutatesNormalEcr: Boolean,
      mutatesLiveFairnessState: Boolean,
      puzzleRatingDistinctFromEcr: Boolean,
      requiresPatchMappedAdapterLater: Boolean,
      reason: String
  ):
    def valid: Boolean =
      usesLichessFoundation &&
        !rebuildsLichessFeature &&
        !mutatesNormalEcr &&
        !mutatesLiveFairnessState &&
        puzzleRatingDistinctFromEcr &&
        eceCustomProfile.valid &&
        (!eceRequestAllowed || overlayEligible)

  object FeatureSurfacePlanner:
    val firstOpeningGuidanceLevel: Level = Level(4)

    def decide(context: FeatureSurfaceContext): FeatureSurfaceDecision =
      val surfaceEligible = context.surface match
        case FeatureSurface.PuzzleTraining =>
          context.mode == SurfaceUseMode.Training &&
            context.puzzleRatingDistinctFromEcr

        case FeatureSurface.StudyBoard =>
          context.mode == SurfaceUseMode.Training ||
            context.mode == SurfaceUseMode.Review

        case FeatureSurface.OpeningExplorer =>
          (context.mode == SurfaceUseMode.Training || context.mode == SurfaceUseMode.Review) &&
            context.hasCurrentFen &&
            context.requestedOpeningProfile.nonEmpty &&
            context.level.value >= firstOpeningGuidanceLevel.value

        case FeatureSurface.AnalysisReplay =>
          context.mode == SurfaceUseMode.Review &&
            (context.reviewMode.nonEmpty || context.validCustomReviewPlan || context.liveHistoryAvailable)

        case FeatureSurface.ComputerPlay =>
          (context.mode == SurfaceUseMode.ComputerTraining || context.mode == SurfaceUseMode.Review) &&
            !context.separateRatedComputerModeDefined

      val overlayEligible = context.settingsAndProductAllow && surfaceEligible
      val openingProfile = if overlayEligible && context.surface == FeatureSurface.OpeningExplorer then
        context.requestedOpeningProfile.getOrElse(0)
      else 0
      val eceRequestAllowed = overlayEligible && (context.surface match
        case FeatureSurface.OpeningExplorer => true
        case FeatureSurface.AnalysisReplay  => context.validCustomReviewPlan
        case FeatureSurface.ComputerPlay    => context.mode == SurfaceUseMode.ComputerTraining
        case _                              => false
      )

      FeatureSurfaceDecision(
        surface = context.surface,
        usesLichessFoundation = context.foundation.reused,
        rebuildsLichessFeature = context.foundation.rebuildAllowed,
        overlayEligible = overlayEligible,
        eceRequestAllowed = eceRequestAllowed,
        eceCustomProfile = EngineGateway.EceCustomProfile(opening = openingProfile, instructions = 0),
        compactGuidanceRequired = context.surface == FeatureSurface.OpeningExplorer && overlayEligible,
        mutatesNormalEcr = false,
        mutatesLiveFairnessState = false,
        puzzleRatingDistinctFromEcr = context.puzzleRatingDistinctFromEcr,
        requiresPatchMappedAdapterLater = true,
        reason = reasonFor(context, overlayEligible, eceRequestAllowed)
      )

    private def reasonFor(
        context: FeatureSurfaceContext,
        overlayEligible: Boolean,
        eceRequestAllowed: Boolean
    ): String =
      if !context.settingsAndProductAllow then "settings_or_product_rules_disable_overlays"
      else if !context.puzzleRatingDistinctFromEcr then "puzzle_rating_label_conflicts_with_ecr"
      else if overlayEligible && eceRequestAllowed then "overlay_and_server_side_ece_request_allowed"
      else if overlayEligible then "overlay_allowed_without_new_ece_request"
      else context.surface match
        case FeatureSurface.PuzzleTraining  => "puzzle_overlays_are_training_only"
        case FeatureSurface.StudyBoard      => "study_overlays_are_review_or_training_only"
        case FeatureSurface.OpeningExplorer => "opening_guidance_requires_fen_opening_input_and_level"
        case FeatureSurface.AnalysisReplay  => "analysis_requires_saved_history_review_mode_or_custom_plan"
        case FeatureSurface.ComputerPlay    => "computer_play_is_training_or_review_unless_separate_rated_mode_exists"

  object AdapterGuard:
    val browserMayCallEceDirectly = false
    val adaptersMustReuseLichessFoundations = true
    val futureCoreSurfaceEditsRequirePatchMap = true
    val clientMayMutateEcrFromFeatureSurface = false
    val clientMayRelabelPuzzleRatingAsEcr = false

    def valid: Boolean =
      !browserMayCallEceDirectly &&
        adaptersMustReuseLichessFoundations &&
        futureCoreSurfaceEditsRequirePatchMap &&
        !clientMayMutateEcrFromFeatureSurface &&
        !clientMayRelabelPuzzleRatingAsEcr
