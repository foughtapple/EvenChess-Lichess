package lila.evenchess

import ProductInvariants.{ RatingEffect, RequirementClass }

object EvenChessMode:

  enum GameMode:
    case NormalLichessChess
    case EvenChess

  final case class SetLevel(value: Int)

  object SetLevel:
    val min = 0
    val max = 10

    def isValid(value: Int): Boolean = value >= min && value <= max

  final case class PlayerSetLevels(white: SetLevel, black: SetLevel):
    def allValid: Boolean = SetLevel.isValid(white.value) && SetLevel.isValid(black.value)

  enum TimeControlBucket:
    case Bullet
    case Blitz
    case Rapid
    case Classical
    case Correspondence
    case Casual

  final case class TimeControlPolicy(
      bucket: TimeControlBucket,
      definition: String,
      evenChessMeaning: String
  )

  object TimeControlBuckets:
    val policies: List[TimeControlPolicy] = List(
      TimeControlPolicy(
        TimeControlBucket.Bullet,
        "Estimated duration <=179s",
        "High live-assistance multiplier; visual over text."
      ),
      TimeControlPolicy(
        TimeControlBucket.Blitz,
        "Estimated duration 180-479s",
        "High multiplier; compact cards."
      ),
      TimeControlPolicy(
        TimeControlBucket.Rapid,
        "Estimated duration 480-1499s",
        "Neutral reference bucket."
      ),
      TimeControlPolicy(
        TimeControlBucket.Classical,
        "Estimated duration >=1500s non-correspondence",
        "Lower time-saving multiplier but exact help still strong."
      ),
      TimeControlPolicy(
        TimeControlBucket.Correspondence,
        "Async/daily",
        "Split model: low time-saving, high exact-candidate/tablebase value."
      ),
      TimeControlPolicy(
        TimeControlBucket.Casual,
        "Non-rated",
        "No public ECR effect."
      )
    )

    val byBucket: Map[TimeControlBucket, TimeControlPolicy] =
      policies.map(policy => policy.bucket -> policy).toMap

    def fromEstimatedDurationSeconds(seconds: Int): TimeControlBucket =
      if seconds <= 179 then TimeControlBucket.Bullet
      else if seconds <= 479 then TimeControlBucket.Blitz
      else if seconds <= 1499 then TimeControlBucket.Rapid
      else TimeControlBucket.Classical

    def resolve(
        estimatedSeconds: Option[Int],
        rated: Boolean,
        correspondence: Boolean
    ): TimeControlBucket =
      if !rated then TimeControlBucket.Casual
      else if correspondence then TimeControlBucket.Correspondence
      else fromEstimatedDurationSeconds(estimatedSeconds.getOrElse(480))

  final case class ServerOwnedMetadata(
      mode: GameMode,
      rated: Boolean,
      playerModeKey: String,
      timeControlBucket: TimeControlBucket,
      setLevelPolicyVersion: String,
      playerSetLevels: PlayerSetLevels,
      assistancePolicyVersion: String,
      ecrPolicyVersion: String,
      auditLedgerVersion: String
  ):
    def isEvenChess: Boolean = mode == GameMode.EvenChess

    def hasRequiredPolicyVersions: Boolean =
      List(
        setLevelPolicyVersion,
        assistancePolicyVersion,
        ecrPolicyVersion,
        auditLedgerVersion
      ).forall(_.nonEmpty)

    def isValid: Boolean = isEvenChess && playerSetLevels.allValid && hasRequiredPolicyVersions

  final case class ClientDisplayClaim(claimsEvenChess: Boolean)

  final case class ModeAuthorityDecision(
      isEvenChess: Boolean,
      clientClaimedEvenChess: Boolean,
      clientFlagAcceptedAsAuthority: Boolean,
      displayAsAssisted: Boolean,
      mayRenderEvenChessOverlays: Boolean,
      mayUseEcrSystems: Boolean,
      mayConsumeEvenChessTokens: Boolean,
      mayWriteAssistanceLogs: Boolean
  )

  object ModeAuthority:
    def decide(
        serverMetadata: Option[ServerOwnedMetadata],
        clientClaim: ClientDisplayClaim
    ): ModeAuthorityDecision =
      val serverSaysEvenChess = serverMetadata.exists(_.isEvenChess)
      ModeAuthorityDecision(
        isEvenChess = serverSaysEvenChess,
        clientClaimedEvenChess = clientClaim.claimsEvenChess,
        clientFlagAcceptedAsAuthority = false,
        displayAsAssisted = serverSaysEvenChess,
        mayRenderEvenChessOverlays = serverSaysEvenChess,
        mayUseEcrSystems = serverSaysEvenChess,
        mayConsumeEvenChessTokens = serverSaysEvenChess,
        mayWriteAssistanceLogs = serverSaysEvenChess
      )

  enum ModeInclusion:
    case Included
    case IncludedIfLowRisk
    case Stage1EarlyTestingIfConvenient
    case Retained
    case Reserved

  final case class PlayerModeSpec(
      key: String,
      displayName: String,
      inclusion: ModeInclusion,
      requiredBehavior: String,
      ratingEffect: RatingEffect,
      classification: RequirementClass
  )

  object PlayerModes:
    val all: List[PlayerModeSpec] = List(
      PlayerModeSpec(
        "normal_rated_evenchess",
        "Normal rated EvenChess",
        ModeInclusion.Included,
        "Uses ECR, Set Level, Used Level, Assistance Load, Used Offset, audit ledger, and rated result.",
        RatingEffect.UpdatesEcrAfterUsedOffset,
        RequirementClass.EvenChessSpecific
      ),
      PlayerModeSpec(
        "casual_evenchess",
        "Casual EvenChess",
        ModeInclusion.IncludedIfLowRisk,
        "Same policy without public ECR update.",
        RatingEffect.NoPublicEcrUpdate,
        RequirementClass.AdaptedToLichessFork
      ),
      PlayerModeSpec(
        "target_level",
        "Target Level mode",
        ModeInclusion.Included,
        "Player-selected Target Level; no normal ECR update in MVP.",
        RatingEffect.NoNormalEcrUpdate,
        RequirementClass.EvenChessSpecific
      ),
      PlayerModeSpec(
        "ai_bot_games",
        "AI/Bot games",
        ModeInclusion.Stage1EarlyTestingIfConvenient,
        "Rating-neutral and excluded from performance-summary online-game windows.",
        RatingEffect.RatingNeutral,
        RequirementClass.AdaptedToLichessFork
      ),
      PlayerModeSpec(
        "post_game_review",
        "Post-game review",
        ModeInclusion.Included,
        "No live mutation; review-legal coaching may be deeper.",
        RatingEffect.RatingNeutral,
        RequirementClass.AdaptedToLichessFork
      ),
      PlayerModeSpec(
        "normal_lichess_chess",
        "Normal Lichess chess",
        ModeInclusion.Retained,
        "No EvenChess assistance.",
        RatingEffect.NormalLichessRatingOnly,
        RequirementClass.LichessProvided
      ),
      PlayerModeSpec(
        "future_classroom_coach",
        "Future classroom/coach",
        ModeInclusion.Reserved,
        "Reserved.",
        RatingEffect.ReservedNoNormalEcr,
        RequirementClass.UnresolvedProductOwnerDecision
      )
    )

    val byKey: Map[String, PlayerModeSpec] = all.map(mode => mode.key -> mode).toMap

  final case class DisclosureRequirement(
      id: String,
      requirement: String,
      classification: RequirementClass
  )

  object Disclosures:
    val all: List[DisclosureRequirement] = List(
      DisclosureRequirement(
        "MODE-L1-010",
        "Game creation must expose mode, time control, rated/casual state, Set Level or level-matching rules, and outside-help prohibition.",
        RequirementClass.AdaptedToLichessFork
      ),
      DisclosureRequirement(
        "MODE-L1-011",
        "Both players must see that the game is assisted EvenChess.",
        RequirementClass.EvenChessSpecific
      ),
      DisclosureRequirement(
        "MODE-L1-012",
        "Game-start confirmation displays Set Level and time-control pool.",
        RequirementClass.EvenChessSpecific
      ),
      DisclosureRequirement(
        "MODE-L1-013",
        "Post-game summary displays actual Used Level and Used Offset.",
        RequirementClass.EvenChessSpecific
      ),
      DisclosureRequirement(
        "MODE-L1-014",
        "Search widening that changes a material level contract requires confirmation.",
        RequirementClass.AdaptedToLichessFork
      ),
      DisclosureRequirement(
        "MODE-L1-020",
        "Online/search/challenge games belong in Live Games; computer games in AI Games; completed games in Review.",
        RequirementClass.AdaptedToLichessFork
      ),
      DisclosureRequirement(
        "MODE-L1-021",
        "Leaving an online game must not let the client decide the result.",
        RequirementClass.LichessProvided
      )
    )

    def requiresSearchWideningConfirmation(changesMaterialLevelContract: Boolean): Boolean =
      changesMaterialLevelContract

  enum GameEntryPoint:
    case OnlineSearchChallenge
    case Computer
    case Completed

  enum RouteBucket:
    case LiveGames
    case AiGames
    case Review

  object Routing:
    def bucketFor(entryPoint: GameEntryPoint): RouteBucket =
      entryPoint match
        case GameEntryPoint.OnlineSearchChallenge => RouteBucket.LiveGames
        case GameEntryPoint.Computer              => RouteBucket.AiGames
        case GameEntryPoint.Completed             => RouteBucket.Review

    val clientMayDecideOnlineLeaveResult = false

  enum AssistanceTimingCharge:
    case TimelyLiveDecisionHelp
    case NotDecisionAssistance
    case StaleOrLateNotTimely

  object AssistanceTiming:
    def chargeFor(
        shownAfterPremoveCommitment: Boolean,
        staleOrLate: Boolean
    ): AssistanceTimingCharge =
      if shownAfterPremoveCommitment then AssistanceTimingCharge.NotDecisionAssistance
      else if staleOrLate then AssistanceTimingCharge.StaleOrLateNotTimely
      else AssistanceTimingCharge.TimelyLiveDecisionHelp

  object Stage1ModeFlagOnly:
    val serverOwnedMetadataPathOnly = true
    val coachingLogicEnabled = false
    val ratingLogicEnabled = false
    val tokenLogicEnabled = false
    val requiresLocalBaselineBeforeExpansion = true
