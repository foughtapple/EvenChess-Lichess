package lila.evenchess

import ProductInvariants.RequirementClass

object LichessProvidedCapabilities:

  final case class ProvidedCapability(
      key: String,
      name: String,
      classification: RequirementClass,
      lichessBaseline: String,
      evenChessAction: String,
      rebuildForbidden: Boolean
  )

  object Capabilities:

    val all: List[ProvidedCapability] = List(
      ProvidedCapability(
        "legal_move_generation",
        "Legal chess rules and move legality",
        RequirementClass.LichessProvided,
        "lila/scalachess/chessops",
        "Use existing legality; do not write a new legal move engine.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "board_ui",
        "Standard board UI",
        RequirementClass.LichessProvided,
        "chessground and lila board surfaces",
        "Add EvenChess overlays only; do not replace chessground for normal play.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "game_lifecycle",
        "Base game lifecycle",
        RequirementClass.LichessProvided,
        "lila game and round lifecycle",
        "Hook for EvenChess metadata and audit only.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "clocks_time_controls",
        "Clocks and time controls",
        RequirementClass.AdaptedToLichessFork,
        "lila clocks and time-control state",
        "Use existing clocks; add assistance multipliers and stale-help policy only.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "pgn_history_replay",
        "Move list, PGN, replay/history",
        RequirementClass.AdaptedToLichessFork,
        "lila PGN, replay, and history foundations",
        "Add assisted metadata and review; do not rebuild ordinary PGN.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "rooms_play_pages",
        "Game rooms/play pages",
        RequirementClass.AdaptedToLichessFork,
        "lila room and play-page surfaces",
        "Add mode routing and disclosures.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "challenge_seek_matchmaking",
        "Challenges/seeks/matchmaking base",
        RequirementClass.AdaptedToLichessFork,
        "lila challenge, seek, lobby, and pairing foundations",
        "Adapt for ECR, Set Level, and level compatibility.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "accounts_sessions",
        "Accounts/session basics",
        RequirementClass.AdaptedToLichessFork,
        "lila account and session foundations",
        "Verify gaps and add EvenChess token/subscription metadata.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "chat_controls",
        "Chat and standard controls",
        RequirementClass.LichessProvided,
        "lila chat and standard controls",
        "Keep unless an EvenChess-specific restriction is required.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "analysis_review",
        "Analysis/review foundation",
        RequirementClass.AdaptedToLichessFork,
        "lila analysis and review surfaces",
        "Adapt summaries and audit-aware review.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "normal_ratings",
        "Normal ratings",
        RequirementClass.LichessProvided,
        "lila ordinary chess ratings",
        "Do not use as ECR unless explicitly approved.",
        rebuildForbidden = true
      ),
      ProvidedCapability(
        "mobile_play",
        "Mobile/responsive chess play",
        RequirementClass.AdaptedToLichessFork,
        "lila mobile and responsive play surfaces",
        "Add EvenChess mobile overlay and card behavior.",
        rebuildForbidden = true
      )
    )

    val byKey: Map[String, ProvidedCapability] = all.map(capability => capability.key -> capability).toMap

    val notToRebuild: List[ProvidedCapability] = all.filter(_.rebuildForbidden)

  final case class GapVerificationArea(
      key: String,
      area: String,
      lichessBaseline: String,
      evenChessGap: String,
      classification: RequirementClass
  )

  object GapVerification:

    val all: List[GapVerificationArea] = List(
      GapVerificationArea(
        "account_creation",
        "Account creation",
        "Existing account system.",
        "One active account/email, reused email token lockout, onboarding tokens.",
        RequirementClass.AdaptedToLichessFork
      ),
      GapVerificationArea(
        "rating_pools",
        "Rating pools",
        "Existing rating logic.",
        "ECR, Effective Rating, Used Offset, assisted pool isolation.",
        RequirementClass.AdaptedToLichessFork
      ),
      GapVerificationArea(
        "engine_analysis",
        "Engine analysis",
        "Existing analysis/fishnet/tablebase paths.",
        "Live server-authorized assistance with gates and audit.",
        RequirementClass.AdaptedToLichessFork
      ),
      GapVerificationArea(
        "studies_lessons",
        "Studies/lessons",
        "Existing study/analysis features.",
        "Future classroom is not active Version 1 scope.",
        RequirementClass.UnresolvedProductOwnerDecision
      ),
      GapVerificationArea(
        "moderation",
        "Moderation",
        "Existing moderation primitives.",
        "Non-platform guidance, assistance abuse, token/ad abuse.",
        RequirementClass.AdaptedToLichessFork
      ),
      GapVerificationArea(
        "payment",
        "Payment",
        "No paid-product assumption for this fork.",
        "Standard/Premium/rewarded-ad economy.",
        RequirementClass.EvenChessSpecific
      ),
      GapVerificationArea(
        "landing_pages",
        "Landing pages",
        "Existing public pages.",
        "Marketing config, variants, UTM, conversion events.",
        RequirementClass.AdaptedToLichessFork
      )
    )

    val byKey: Map[String, GapVerificationArea] = all.map(area => area.key -> area).toMap

  final case class SupersededRequirement(id: String, description: String)

  object SupersededRequirements:
    val all: List[SupersededRequirement] = List(
      SupersededRequirement("C-L1-001", "Custom chess server from scratch is superseded."),
      SupersededRequirement("C-L1-002", "Custom legal move generation is superseded."),
      SupersededRequirement("C-L1-003", "Custom primary board renderer is superseded."),
      SupersededRequirement("C-L1-004", "Custom base game rooms/clocks/history/PGN are superseded."),
      SupersededRequirement("C-L1-005", "Replacing normal Lichess chess with EvenChess is superseded."),
      SupersededRequirement("C-L1-006", "Old custom UI specs must become overlay/surface requirements, not full replacements.")
    )

  final case class AdaptedRequirement(
      key: String,
      requirement: String,
      lichessSeam: String
  )

  object AdaptedRequirements:
    val all: List[AdaptedRequirement] = List(
      AdaptedRequirement(
        "time_controls",
        "Assistance multipliers and stale-help logic.",
        "Existing clock/time-control flow."
      ),
      AdaptedRequirement(
        "matchmaking",
        "ECR, offset, and Set Level compatibility.",
        "Existing challenge, seek, lobby, and pairing flow."
      ),
      AdaptedRequirement(
        "accounts",
        "Token/subscription account rules.",
        "Existing account and session flow."
      ),
      AdaptedRequirement(
        "review_summaries",
        "Assisted review and summaries.",
        "Existing analysis/review surfaces."
      ),
      AdaptedRequirement(
        "operations",
        "Engine, AI, fairness, and marketing health.",
        "Existing ops/admin patterns where suitable."
      )
    )

  enum InfrastructureDecision:
    case StopNoSpecificGap
    case AdaptExistingLichessSeam
    case UnknownCapabilityInspectBeforeCoding

  object PlatformInfrastructureGuard:

    val mustStateWhyLichessDoesNotProvideItBeforeCoding = true

    def decide(capabilityKey: String, specificGapIdentified: Boolean): InfrastructureDecision =
      Capabilities.byKey.get(capabilityKey) match
        case Some(_) if specificGapIdentified => InfrastructureDecision.AdaptExistingLichessSeam
        case Some(_)                          => InfrastructureDecision.StopNoSpecificGap
        case None                             => InfrastructureDecision.UnknownCapabilityInspectBeforeCoding

    def mayContinueWithAdaptedSeam(capabilityKey: String, specificGapIdentified: Boolean): Boolean =
      decide(capabilityKey, specificGapIdentified) == InfrastructureDecision.AdaptExistingLichessSeam

  object RatingSeparation:
    val normalLichessRatingsAreEcr = false
    val normalRatingsMayBeCorruptedByEvenChess = false
