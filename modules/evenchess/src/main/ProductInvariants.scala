package lila.evenchess

object ProductInvariants:

  enum RequirementClass:
    case LichessProvided
    case EvenChessSpecific
    case AdaptedToLichessFork
    case SupersededByLichessFork
    case UnresolvedProductOwnerDecision

  final case class VocabularyTerm(
      name: String,
      definition: String,
      authority: String,
      classification: RequirementClass
  )

  object Vocabulary:

    val all: List[VocabularyTerm] = List(
      VocabularyTerm(
        "EvenChess",
        "The disclosed assisted chess mode added by this fork.",
        "Product invariant",
        RequirementClass.EvenChessSpecific
      ),
      VocabularyTerm(
        "Normal chess",
        "Ordinary chess without EvenChess platform coaching.",
        "Lichess-provided and separate",
        RequirementClass.LichessProvided
      ),
      VocabularyTerm(
        "ECR",
        "Stored estimate of underlying human skill in an EvenChess rated pool after assistance correction.",
        "Appendix J",
        RequirementClass.EvenChessSpecific
      ),
      VocabularyTerm(
        "Rating Level",
        "Platform-assigned default or recommended coaching level derived from ECR/policy.",
        "Appendix J",
        RequirementClass.EvenChessSpecific
      ),
      VocabularyTerm(
        "Set Level",
        "Server-authorized maximum assistance level for a game. Permission, not actual use.",
        "Appendix G",
        RequirementClass.EvenChessSpecific
      ),
      VocabularyTerm(
        "Used Level",
        "Highest public coaching level actually delivered or consumed; never decreases.",
        "Appendix I",
        RequirementClass.EvenChessSpecific
      ),
      VocabularyTerm(
        "Assistance Load",
        "Measured dose of assistance from feature, exactness, surface, timing, latency, criticality, visibility, follow-rate, and quality.",
        "Appendix I",
        RequirementClass.EvenChessSpecific
      ),
      VocabularyTerm(
        "Used Offset",
        "Rating-point correction assigned to actual assistance consumed.",
        "Appendix I/J",
        RequirementClass.EvenChessSpecific
      ),
      VocabularyTerm(
        "Effective Rating",
        "Game-specific playing strength: ECR + Used Offset.",
        "Appendix J",
        RequirementClass.EvenChessSpecific
      ),
      VocabularyTerm(
        "Target Level",
        "Player-selected assistance target for Target Level mode; not Rating Level and not normal ECR input.",
        "Appendix K",
        RequirementClass.EvenChessSpecific
      ),
      VocabularyTerm(
        "Offset Count",
        "Existing Exchange Resolver / take-take-take local exchange feature.",
        "Appendix H",
        RequirementClass.AdaptedToLichessFork
      ),
      VocabularyTerm(
        "Truth packet",
        "Server-generated chess facts supplied to AI for compression/explanation.",
        "Appendix M",
        RequirementClass.EvenChessSpecific
      )
    )

    val byName: Map[String, VocabularyTerm] = all.map(term => term.name.toLowerCase -> term).toMap

  object PublicPositioning:

    val canonicalClaim =
      "EvenChess is a separate rated assisted chess variant where platform-delivered guidance is disclosed, capped by Set Level, logged by the server, and accounted for in ECR."

    val forbiddenPhrases: List[String] = List(
      "cheating allowed",
      "secret engine use",
      "normal chess with help",
      "pay to win"
    )

    val prohibitedExternalGuidance: List[String] = List(
      "external engines",
      "coaches",
      "friends",
      "stream chat",
      "notes",
      "browser extensions",
      "bots",
      "unaudited analysis"
    )

    val requiredDisclosureSurfaces: List[String] = List(
      "lobby",
      "queue",
      "game-start confirmation",
      "persistent game header",
      "result screen",
      "review/history",
      "fair-play report flow",
      "onboarding",
      "FAQ",
      "pricing",
      "landing pages"
    )

    def forbiddenPhraseHits(copy: String): List[String] =
      val normalized = copy.toLowerCase
      forbiddenPhrases.filter(phrase => normalized.contains(phrase))

    def isAllowedCopy(copy: String): Boolean = forbiddenPhraseHits(copy).isEmpty

  enum RatingEffect:
    case UpdatesEcrAfterUsedOffset
    case NoPublicEcrUpdate
    case NoNormalEcrUpdate
    case RatingNeutral
    case NormalLichessRatingOnly
    case ReservedNoNormalEcr

  final case class PlayerMode(
      key: String,
      displayName: String,
      purpose: String,
      ratingEffect: RatingEffect,
      implementationDirection: String,
      classification: RequirementClass
  )

  object PlayerModes:

    val all: List[PlayerMode] = List(
      PlayerMode(
        "normal_evenchess",
        "Normal EvenChess",
        "Main rated assisted mode.",
        RatingEffect.UpdatesEcrAfterUsedOffset,
        "Separate mode/layer and pool; normal chess unaffected.",
        RequirementClass.EvenChessSpecific
      ),
      PlayerMode(
        "casual_evenchess",
        "Casual EvenChess",
        "Flexible assisted play.",
        RatingEffect.NoPublicEcrUpdate,
        "Reuse Lichess casual patterns with EvenChess metadata.",
        RequirementClass.AdaptedToLichessFork
      ),
      PlayerMode(
        "target_level",
        "Target Level Mode",
        "Player-selected practice level.",
        RatingEffect.NoNormalEcrUpdate,
        "Separate queue and metadata.",
        RequirementClass.EvenChessSpecific
      ),
      PlayerMode(
        "ai_bot_practice",
        "AI/Bot practice",
        "Training/drill play.",
        RatingEffect.RatingNeutral,
        "May reuse Lichess AI/bot paths if safe.",
        RequirementClass.AdaptedToLichessFork
      ),
      PlayerMode(
        "post_game_review",
        "Post-game review",
        "Learn after completed game.",
        RatingEffect.RatingNeutral,
        "Integrate with analysis/review; no live mutation.",
        RequirementClass.AdaptedToLichessFork
      ),
      PlayerMode(
        "normal_lichess_chess",
        "Normal Lichess chess",
        "Ordinary chess in the fork.",
        RatingEffect.NormalLichessRatingOnly,
        "No EvenChess assistance.",
        RequirementClass.LichessProvided
      ),
      PlayerMode(
        "future_classroom_coach",
        "Future classroom/coach",
        "Coach-led learning.",
        RatingEffect.ReservedNoNormalEcr,
        "Reserved.",
        RequirementClass.UnresolvedProductOwnerDecision
      )
    )

    val byKey: Map[String, PlayerMode] = all.map(mode => mode.key -> mode).toMap

  object RelationshipRules:
    val setLevelIsPermission = true
    val usedLevelIsActualUse = true
    val usedLevelNeverDecreases = true
    val targetLevelIsRatingLevel = false
    val aiTextIsChessAuthority = false
    val normalLichessBasicsAreEvenChessSpecific = false

  enum OffsetCountDisplay(val resultState: String, val color: String, val icon: String):
    case EqualTrade extends OffsetCountDisplay("equal", "blue", "shield")
    case StudentWinsMaterial extends OffsetCountDisplay("student_wins", "green", "number")
    case OpponentWinsMaterial extends OffsetCountDisplay("opponent_wins", "red", "number")
    case Unknown extends OffsetCountDisplay("unknown", "grey", "disabled")

  object MarketingAccountInvariants:
    val onboardingGameTokens = 10
    val onboardingMatchSummaryTokens = 3
    val performanceSummaryTokens = 1
    val performanceSummaryUnlockCompletedGames = 10
    val maxEarnedAdGameTokens = 3
    val standardMayChangeRatedLiveStrength = false
    val premiumMayChangeRatedLiveStrength = false
    val premiumAddsStrongerLiveCoaching = false
