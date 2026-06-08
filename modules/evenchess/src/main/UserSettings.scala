package lila.evenchess

import ProductInvariants.RequirementClass

object UserSettings:

  enum PhaseDRequirement:
    case LichessSettingsShell
    case ServerSidePerUserStorage
    case DefaultSetLevel
    case PreferredStartingUsedLevel
    case PreferredStartingFeatureToggles
    case OverlayDensity
    case CoachingCardVerbosity
    case BoardHighlightIntensity
    case OffsetCountDisplayPreference
    case AiSummaryPreference
    case TtsCoachControls
    case StudyOpeningOverlayDefaults
    case PrivacyTelemetryPreference
    case FairnessBoundary

  final case class PhaseDRequirementClassification(
      requirement: PhaseDRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseDRequirementClassifications:
    val all: List[PhaseDRequirementClassification] = List(
      PhaseDRequirementClassification(
        PhaseDRequirement.LichessSettingsShell,
        RequirementClass.LichessProvided,
        "Use the existing Lichess account preference page, authentication, routing, and autosave flow."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.ServerSidePerUserStorage,
        RequirementClass.AdaptedToLichessFork,
        "Persist namespaced EvenChess values in the server-owned Lichess preference document tags map."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.DefaultSetLevel,
        RequirementClass.EvenChessSpecific,
        "Store only a player default/request. Server policy still caps authorized live Set Level."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.PreferredStartingUsedLevel,
        RequirementClass.EvenChessSpecific,
        "Store the preferred starting Used Level for new games as a display/request default capped by the game's Set Level; it never lowers live Used Level once a game has started."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.PreferredStartingFeatureToggles,
        RequirementClass.EvenChessSpecific,
        "Store the preferred per-feature display toggles used when a new game starts or when the in-game bulk level selector is applied."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.OverlayDensity,
        RequirementClass.EvenChessSpecific,
        "Control display density for authorized EvenChess overlay content."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.CoachingCardVerbosity,
        RequirementClass.EvenChessSpecific,
        "Control wording length for authorized coaching cards without changing chess truth or permission."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.BoardHighlightIntensity,
        RequirementClass.EvenChessSpecific,
        "Control visual emphasis for authorized overlays without enabling stronger help."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.OffsetCountDisplayPreference,
        RequirementClass.AdaptedToLichessFork,
        "Display preference for the existing Exchange Resolver / take-take-take Offset Count feature."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.AiSummaryPreference,
        RequirementClass.EvenChessSpecific,
        "Control post-game and learning summary defaults, not live permission."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.TtsCoachControls,
        RequirementClass.EvenChessSpecific,
        "Control whether authorized coaching text may be read aloud and how it sounds."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.StudyOpeningOverlayDefaults,
        RequirementClass.AdaptedToLichessFork,
        "Default overlay preferences for Lichess-provided study/opening surfaces."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.PrivacyTelemetryPreference,
        RequirementClass.EvenChessSpecific,
        "Control optional EvenChess product/calibration telemetry where policy permits."
      ),
      PhaseDRequirementClassification(
        PhaseDRequirement.FairnessBoundary,
        RequirementClass.EvenChessSpecific,
        "Preferences never decide coaching permission, Used Level, Used Offset, Assistance Load, ECR, or audit records."
      )
    )

  val minSetLevel = 0
  val maxSetLevel = 10
  val minTtsRatePercent = 70
  val maxTtsRatePercent = 130
  val minTtsVolumePercent = 0
  val maxTtsVolumePercent = 100
  val minTtsAutoDelaySeconds = 0
  val maxTtsAutoDelaySeconds = 30
  val tagPrefix = "evenchess_"

  final case class DefaultFeatureToggle(key: String, label: String, level: Int, surface: String)

  object DefaultFeatureToggle:
    val all: List[DefaultFeatureToggle] = List(
      DefaultFeatureToggle("rules", "Legal state", 1, "Coach"),
      DefaultFeatureToggle("loosePieces", "Loose pieces", 2, "Board + coach"),
      DefaultFeatureToggle("hangingPieces", "Hanging pieces", 2, "Board + coach"),
      DefaultFeatureToggle("offsetCount", "Exchange count", 3, "Board + coach"),
      DefaultFeatureToggle("studentThreats", "Student threat arrows", 4, "Board"),
      DefaultFeatureToggle("opponentThreats", "Opponent threat arrows", 4, "Board"),
      DefaultFeatureToggle("pins", "Pinned pieces", 4, "Board + coach"),
      DefaultFeatureToggle("coachText", "Coach text", 4, "Coach"),
      DefaultFeatureToggle("candidate1", "Opponent potentials", 5, "Board"),
      DefaultFeatureToggle("candidate2", "My potentials", 6, "Board"),
      DefaultFeatureToggle("openingWiki", "WikiBook", 6, "Coach"),
      DefaultFeatureToggle("candidate3", "Extra potentials", 7, "Board"),
      DefaultFeatureToggle("evalBar", "Eval bar", 8, "Board"),
      DefaultFeatureToggle("evalNumbers", "Eval text", 8, "Coach"),
      DefaultFeatureToggle("humanRisk", "Human-risk note", 9, "Coach"),
      DefaultFeatureToggle("expertLines", "Why-not / branch", 9, "Coach"),
      DefaultFeatureToggle("fullSpecificity", "Full specificity", 10, "Coach")
    )
    val keys: Set[String] = all.map(_.key).toSet

  final case class DefaultFeatureToggles(
      rules: Boolean,
      loosePieces: Boolean,
      hangingPieces: Boolean,
      offsetCount: Boolean,
      studentThreats: Boolean,
      opponentThreats: Boolean,
      pins: Boolean,
      coachText: Boolean,
      candidate1: Boolean,
      candidate2: Boolean,
      openingWiki: Boolean,
      candidate3: Boolean,
      evalBar: Boolean,
      evalNumbers: Boolean,
      humanRisk: Boolean,
      expertLines: Boolean,
      fullSpecificity: Boolean
  ):
    def asMap: Map[String, Boolean] = Map(
      "rules" -> rules,
      "loosePieces" -> loosePieces,
      "hangingPieces" -> hangingPieces,
      "offsetCount" -> offsetCount,
      "studentThreats" -> studentThreats,
      "opponentThreats" -> opponentThreats,
      "pins" -> pins,
      "coachText" -> coachText,
      "candidate1" -> candidate1,
      "candidate2" -> candidate2,
      "openingWiki" -> openingWiki,
      "candidate3" -> candidate3,
      "evalBar" -> evalBar,
      "evalNumbers" -> evalNumbers,
      "humanRisk" -> humanRisk,
      "expertLines" -> expertLines,
      "fullSpecificity" -> fullSpecificity
    )

    def valueFor(key: String): Boolean = asMap.getOrElse(key, true)
    def valid: Boolean = asMap.keySet == DefaultFeatureToggle.keys

  object DefaultFeatureToggles:
    type FormTuple = (
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean,
        Boolean
    )

    val default: DefaultFeatureToggles = DefaultFeatureToggles(
      rules = true,
      loosePieces = true,
      hangingPieces = true,
      offsetCount = true,
      studentThreats = true,
      opponentThreats = true,
      pins = true,
      coachText = true,
      candidate1 = true,
      candidate2 = true,
      openingWiki = true,
      candidate3 = true,
      evalBar = true,
      evalNumbers = true,
      humanRisk = true,
      expertLines = true,
      fullSpecificity = true
    )

    def fromMap(values: Map[String, Boolean]): DefaultFeatureToggles =
      default.copy(
        rules = values.getOrElse("rules", default.rules),
        loosePieces = values.getOrElse("loosePieces", default.loosePieces),
        hangingPieces = values.getOrElse("hangingPieces", default.hangingPieces),
        offsetCount = values.getOrElse("offsetCount", default.offsetCount),
        studentThreats = values.getOrElse("studentThreats", default.studentThreats),
        opponentThreats = values.getOrElse("opponentThreats", default.opponentThreats),
        pins = values.getOrElse("pins", default.pins),
        coachText = values.getOrElse("coachText", default.coachText),
        candidate1 = values.getOrElse("candidate1", default.candidate1),
        candidate2 = values.getOrElse("candidate2", default.candidate2),
        openingWiki = values.getOrElse("openingWiki", default.openingWiki),
        candidate3 = values.getOrElse("candidate3", default.candidate3),
        evalBar = values.getOrElse("evalBar", default.evalBar),
        evalNumbers = values.getOrElse("evalNumbers", default.evalNumbers),
        humanRisk = values.getOrElse("humanRisk", default.humanRisk),
        expertLines = values.getOrElse("expertLines", default.expertLines),
        fullSpecificity = values.getOrElse("fullSpecificity", default.fullSpecificity)
      )

    def unapplyForm(data: DefaultFeatureToggles): Option[FormTuple] =
      Some(
        (
          data.rules,
          data.loosePieces,
          data.hangingPieces,
          data.offsetCount,
          data.studentThreats,
          data.opponentThreats,
          data.pins,
          data.coachText,
          data.candidate1,
          data.candidate2,
          data.openingWiki,
          data.candidate3,
          data.evalBar,
          data.evalNumbers,
          data.humanRisk,
          data.expertLines,
          data.fullSpecificity
        )
      )

  enum OverlayDensity(val key: String, val label: String):
    case Compact extends OverlayDensity("compact", "Compact")
    case Balanced extends OverlayDensity("balanced", "Balanced")
    case Detailed extends OverlayDensity("detailed", "Detailed")

  object OverlayDensity:
    val default = Balanced
    val choices: List[(String, String)] = values.toList.map(value => value.key -> value.label)
    val keys: Set[String] = choices.map(_._1).toSet
    def fromKey(key: String): OverlayDensity = values.find(_.key == key).getOrElse(default)

  enum CoachingCardVerbosity(val key: String, val label: String):
    case Brief extends CoachingCardVerbosity("brief", "Brief")
    case Standard extends CoachingCardVerbosity("standard", "Standard")
    case Detailed extends CoachingCardVerbosity("detailed", "Detailed")

  object CoachingCardVerbosity:
    val default = Standard
    val choices: List[(String, String)] = values.toList.map(value => value.key -> value.label)
    val keys: Set[String] = choices.map(_._1).toSet
    def fromKey(key: String): CoachingCardVerbosity = values.find(_.key == key).getOrElse(default)

  enum BoardHighlightIntensity(val key: String, val label: String):
    case Low extends BoardHighlightIntensity("low", "Low")
    case Medium extends BoardHighlightIntensity("medium", "Medium")
    case High extends BoardHighlightIntensity("high", "High")

  object BoardHighlightIntensity:
    val default = Low
    val choices: List[(String, String)] = values.toList.map(value => value.key -> value.label)
    val keys: Set[String] = choices.map(_._1).toSet
    def fromKey(key: String): BoardHighlightIntensity = values.find(_.key == key).getOrElse(default)

  enum OffsetCountDisplay(val key: String, val label: String):
    case OnDemand extends OffsetCountDisplay("on-demand", "On demand")
    case AlwaysWhenAuthorized extends OffsetCountDisplay("always-authorized", "Always when authorized")
    case HiddenInRated extends OffsetCountDisplay("hidden-in-rated", "Hidden in rated games")

  object OffsetCountDisplay:
    val default = OnDemand
    val choices: List[(String, String)] = values.toList.map(value => value.key -> value.label)
    val keys: Set[String] = choices.map(_._1).toSet
    def fromKey(key: String): OffsetCountDisplay = values.find(_.key == key).getOrElse(default)

  enum AiSummaryPreference(val key: String, val label: String):
    case Ask extends AiSummaryPreference("ask", "Ask after game")
    case AutoAfterGame extends AiSummaryPreference("auto-after-game", "Auto after game")
    case Never extends AiSummaryPreference("never", "Never")

  object AiSummaryPreference:
    val default = Ask
    val choices: List[(String, String)] = values.toList.map(value => value.key -> value.label)
    val keys: Set[String] = choices.map(_._1).toSet
    def fromKey(key: String): AiSummaryPreference = values.find(_.key == key).getOrElse(default)

  enum TtsVoice(val key: String, val label: String):
    case SystemDefault extends TtsVoice("system-default", "System default")
    case Clear extends TtsVoice("clear", "Clear")
    case Warm extends TtsVoice("warm", "Warm")

  object TtsVoice:
    val default = SystemDefault
    val choices: List[(String, String)] = values.toList.map(value => value.key -> value.label)
    val keys: Set[String] = choices.map(_._1).toSet
    def fromKey(key: String): TtsVoice = values.find(_.key == key).getOrElse(default)

  enum TtsQueueBehavior(val key: String, val label: String):
    case ReplaceCurrent extends TtsQueueBehavior("replace-current", "Replace current")
    case Queue extends TtsQueueBehavior("queue", "Queue")

  object TtsQueueBehavior:
    val default = ReplaceCurrent
    val choices: List[(String, String)] = values.toList.map(value => value.key -> value.label)
    val keys: Set[String] = choices.map(_._1).toSet
    def fromKey(key: String): TtsQueueBehavior = values.find(_.key == key).getOrElse(default)

  enum TelemetryPreference(val key: String, val label: String):
    case RequiredOnly extends TelemetryPreference("required-only", "Required only")
    case ProductAndCalibration extends TelemetryPreference("product-calibration", "Product and calibration")
    case OptionalOff extends TelemetryPreference("optional-off", "Optional off")

  object TelemetryPreference:
    val default = RequiredOnly
    val choices: List[(String, String)] = values.toList.map(value => value.key -> value.label)
    val keys: Set[String] = choices.map(_._1).toSet
    def fromKey(key: String): TelemetryPreference = values.find(_.key == key).getOrElse(default)

  final case class TtsCoach(
      enabled: Boolean,
      autoSpeak: Boolean,
      autoDelaySeconds: Int,
      voice: TtsVoice,
      ratePercent: Int,
      volumePercent: Int,
      queueBehavior: TtsQueueBehavior,
      muteDuringOpponentTurn: Boolean
  ):
    def valid: Boolean =
      ratePercent >= minTtsRatePercent &&
        ratePercent <= maxTtsRatePercent &&
        volumePercent >= minTtsVolumePercent &&
        volumePercent <= maxTtsVolumePercent &&
        autoDelaySeconds >= minTtsAutoDelaySeconds &&
        autoDelaySeconds <= maxTtsAutoDelaySeconds

  final case class LearningOverlayDefaults(
      studyAiOverlay: Boolean,
      openingAiOverlay: Boolean
  )

  final case class Preferences(
      defaultSetLevel: Int,
      preferredUsedLevel: Int,
      defaultFeatureToggles: DefaultFeatureToggles,
      overlayDensity: OverlayDensity,
      coachingCardVerbosity: CoachingCardVerbosity,
      boardHighlightIntensity: BoardHighlightIntensity,
      offsetCountDisplay: OffsetCountDisplay,
      aiSummaryPreference: AiSummaryPreference,
      ttsCoach: TtsCoach,
      learningOverlayDefaults: LearningOverlayDefaults,
      telemetryPreference: TelemetryPreference
  ):
    def valid: Boolean =
      defaultSetLevel >= minSetLevel &&
        defaultSetLevel <= maxSetLevel &&
        preferredUsedLevel >= minSetLevel &&
        preferredUsedLevel <= maxSetLevel &&
        defaultFeatureToggles.valid &&
        ttsCoach.valid

    def authorizedDefaultSetLevel(serverMaximum: Int): Int =
      clampSetLevel(defaultSetLevel min clampSetLevel(serverMaximum))

    def startingUsedLevelFor(setLevel: Int): Int =
      clampSetLevel(preferredUsedLevel min clampSetLevel(setLevel))

    def cannotRaiseLiveCoachingStrength(serverMaximum: Int): Boolean =
      authorizedDefaultSetLevel(serverMaximum) <= clampSetLevel(serverMaximum)

    def clientConfig: ClientConfig =
      ClientConfig(
        defaultSetLevel = defaultSetLevel,
        preferredUsedLevel = preferredUsedLevel,
        defaultFeatureToggles = defaultFeatureToggles.asMap,
        overlayDensity = overlayDensity.key,
        coachingCardVerbosity = coachingCardVerbosity.key,
        boardHighlightIntensity = boardHighlightIntensity.key,
        offsetCountDisplay = offsetCountDisplay.key,
        aiSummaryPreference = aiSummaryPreference.key,
        ttsEnabled = ttsCoach.enabled,
        ttsAutoSpeak = ttsCoach.autoSpeak,
        ttsAutoDelaySeconds = ttsCoach.autoDelaySeconds,
        ttsVoice = ttsCoach.voice.key,
        ttsRatePercent = ttsCoach.ratePercent,
        ttsVolumePercent = ttsCoach.volumePercent,
        ttsQueueBehavior = ttsCoach.queueBehavior.key,
        ttsMuteDuringOpponentTurn = ttsCoach.muteDuringOpponentTurn,
        studyAiOverlay = learningOverlayDefaults.studyAiOverlay,
        openingAiOverlay = learningOverlayDefaults.openingAiOverlay,
        telemetryPreference = telemetryPreference.key
      )

    def toFormData: FormData =
      FormData(
        defaultSetLevel = defaultSetLevel,
        preferredUsedLevel = preferredUsedLevel,
        defaultFeatureToggles = defaultFeatureToggles,
        overlayDensity = overlayDensity.key,
        coachingCardVerbosity = coachingCardVerbosity.key,
        boardHighlightIntensity = boardHighlightIntensity.key,
        offsetCountDisplay = offsetCountDisplay.key,
        aiSummaryPreference = aiSummaryPreference.key,
        ttsEnabled = ttsCoach.enabled,
        ttsAutoSpeak = ttsCoach.autoSpeak,
        ttsAutoDelaySeconds = ttsCoach.autoDelaySeconds,
        ttsVoice = ttsCoach.voice.key,
        ttsRatePercent = ttsCoach.ratePercent,
        ttsVolumePercent = ttsCoach.volumePercent,
        ttsQueueBehavior = ttsCoach.queueBehavior.key,
        ttsMuteDuringOpponentTurn = ttsCoach.muteDuringOpponentTurn,
        studyAiOverlay = learningOverlayDefaults.studyAiOverlay,
        openingAiOverlay = learningOverlayDefaults.openingAiOverlay,
        telemetryPreference = telemetryPreference.key
      )

  final case class FormData(
      defaultSetLevel: Int,
      preferredUsedLevel: Int,
      defaultFeatureToggles: DefaultFeatureToggles,
      overlayDensity: String,
      coachingCardVerbosity: String,
      boardHighlightIntensity: String,
      offsetCountDisplay: String,
      aiSummaryPreference: String,
      ttsEnabled: Boolean,
      ttsAutoSpeak: Boolean,
      ttsAutoDelaySeconds: Int,
      ttsVoice: String,
      ttsRatePercent: Int,
      ttsVolumePercent: Int,
      ttsQueueBehavior: String,
      ttsMuteDuringOpponentTurn: Boolean,
      studyAiOverlay: Boolean,
      openingAiOverlay: Boolean,
      telemetryPreference: String
  ):
    def toPreferences: Preferences =
      Preferences(
        defaultSetLevel = clampSetLevel(defaultSetLevel),
        preferredUsedLevel = clampSetLevel(preferredUsedLevel),
        defaultFeatureToggles = defaultFeatureToggles,
        overlayDensity = OverlayDensity.fromKey(overlayDensity),
        coachingCardVerbosity = CoachingCardVerbosity.fromKey(coachingCardVerbosity),
        boardHighlightIntensity = BoardHighlightIntensity.fromKey(boardHighlightIntensity),
        offsetCountDisplay = OffsetCountDisplay.fromKey(offsetCountDisplay),
        aiSummaryPreference = AiSummaryPreference.fromKey(aiSummaryPreference),
        ttsCoach = TtsCoach(
          enabled = ttsEnabled,
          autoSpeak = ttsAutoSpeak,
          autoDelaySeconds = clamp(
            ttsAutoDelaySeconds,
            minTtsAutoDelaySeconds,
            maxTtsAutoDelaySeconds
          ),
          voice = TtsVoice.fromKey(ttsVoice),
          ratePercent = clamp(ttsRatePercent, minTtsRatePercent, maxTtsRatePercent),
          volumePercent = clamp(ttsVolumePercent, minTtsVolumePercent, maxTtsVolumePercent),
          queueBehavior = TtsQueueBehavior.fromKey(ttsQueueBehavior),
          muteDuringOpponentTurn = ttsMuteDuringOpponentTurn
        ),
        learningOverlayDefaults = LearningOverlayDefaults(
          studyAiOverlay = studyAiOverlay,
          openingAiOverlay = openingAiOverlay
        ),
        telemetryPreference = TelemetryPreference.fromKey(telemetryPreference)
      )

  object FormData:
    val default: FormData = UserSettings.default.toFormData

  object FormDataMapping:
    type FormDataTuple = (
        Int,
        Int,
        DefaultFeatureToggles,
        String,
        String,
        String,
        String,
        String,
        Boolean,
        Boolean,
        Int,
        String,
        Int,
        Int,
        String,
        Boolean,
        Boolean,
        Boolean,
        String
    )

    def apply(
        defaultSetLevel: Int,
        preferredUsedLevel: Int,
        defaultFeatureToggles: DefaultFeatureToggles,
        overlayDensity: String,
        coachingCardVerbosity: String,
        boardHighlightIntensity: String,
        offsetCountDisplay: String,
        aiSummaryPreference: String,
        ttsEnabled: Boolean,
        ttsAutoSpeak: Boolean,
        ttsAutoDelaySeconds: Int,
        ttsVoice: String,
        ttsRatePercent: Int,
        ttsVolumePercent: Int,
        ttsQueueBehavior: String,
        ttsMuteDuringOpponentTurn: Boolean,
        studyAiOverlay: Boolean,
        openingAiOverlay: Boolean,
        telemetryPreference: String
    ): FormData =
      FormData(
        defaultSetLevel,
        preferredUsedLevel,
        defaultFeatureToggles,
        overlayDensity,
        coachingCardVerbosity,
        boardHighlightIntensity,
        offsetCountDisplay,
        aiSummaryPreference,
        ttsEnabled,
        ttsAutoSpeak,
        ttsAutoDelaySeconds,
        ttsVoice,
        ttsRatePercent,
        ttsVolumePercent,
        ttsQueueBehavior,
        ttsMuteDuringOpponentTurn,
        studyAiOverlay,
        openingAiOverlay,
        telemetryPreference
      )

    def unapply(data: FormData): Option[FormDataTuple] =
      Some(
        (
          data.defaultSetLevel,
          data.preferredUsedLevel,
          data.defaultFeatureToggles,
          data.overlayDensity,
          data.coachingCardVerbosity,
          data.boardHighlightIntensity,
          data.offsetCountDisplay,
          data.aiSummaryPreference,
          data.ttsEnabled,
          data.ttsAutoSpeak,
          data.ttsAutoDelaySeconds,
          data.ttsVoice,
          data.ttsRatePercent,
          data.ttsVolumePercent,
          data.ttsQueueBehavior,
          data.ttsMuteDuringOpponentTurn,
          data.studyAiOverlay,
          data.openingAiOverlay,
          data.telemetryPreference
        )
      )

  final case class ClientConfig(
      defaultSetLevel: Int,
      preferredUsedLevel: Int,
      defaultFeatureToggles: Map[String, Boolean],
      overlayDensity: String,
      coachingCardVerbosity: String,
      boardHighlightIntensity: String,
      offsetCountDisplay: String,
      aiSummaryPreference: String,
      ttsEnabled: Boolean,
      ttsAutoSpeak: Boolean,
      ttsAutoDelaySeconds: Int,
      ttsVoice: String,
      ttsRatePercent: Int,
      ttsVolumePercent: Int,
      ttsQueueBehavior: String,
      ttsMuteDuringOpponentTurn: Boolean,
      studyAiOverlay: Boolean,
      openingAiOverlay: Boolean,
      telemetryPreference: String
  ):
    def fieldKeys: Set[String] = ClientConfig.fieldKeys
    def exposesFairnessAuthority: Boolean = fieldKeys.exists(fairnessAuthorityKeys)
    def valid: Boolean = !exposesFairnessAuthority && FormData(
      defaultSetLevel,
      preferredUsedLevel,
      DefaultFeatureToggles.fromMap(defaultFeatureToggles),
      overlayDensity,
      coachingCardVerbosity,
      boardHighlightIntensity,
      offsetCountDisplay,
      aiSummaryPreference,
      ttsEnabled,
      ttsAutoSpeak,
      ttsAutoDelaySeconds,
      ttsVoice,
      ttsRatePercent,
      ttsVolumePercent,
      ttsQueueBehavior,
      ttsMuteDuringOpponentTurn,
      studyAiOverlay,
      openingAiOverlay,
      telemetryPreference
    ).toPreferences.valid

  object ClientConfig:
    val fieldKeys: Set[String] = Set(
      "defaultSetLevel",
      "preferredUsedLevel",
      "defaultFeatureToggles",
      "overlayDensity",
      "coachingCardVerbosity",
      "boardHighlightIntensity",
      "offsetCountDisplay",
      "aiSummaryPreference",
      "ttsEnabled",
      "ttsAutoSpeak",
      "ttsAutoDelaySeconds",
      "ttsVoice",
      "ttsRatePercent",
      "ttsVolumePercent",
      "ttsQueueBehavior",
      "ttsMuteDuringOpponentTurn",
      "studyAiOverlay",
      "openingAiOverlay",
      "telemetryPreference"
    )

  final case class ServerRecord(userId: String, preferences: Preferences):
    def valid: Boolean = userId.nonEmpty && preferences.valid
    def safeClientConfig: ClientConfig = preferences.clientConfig

  val fairnessAuthorityKeys: Set[String] = Set(
    "coachingPermission",
    "usedLevel",
    "assistanceLoad",
    "usedOffset",
    "ecr",
    "coachingRender",
    "auditLedger",
    "stockfishLine",
    "providerSecret",
    "subscriptionTier"
  )

  val default: Preferences =
    Preferences(
      defaultSetLevel = minSetLevel,
      preferredUsedLevel = minSetLevel,
      defaultFeatureToggles = DefaultFeatureToggles.default,
      overlayDensity = OverlayDensity.default,
      coachingCardVerbosity = CoachingCardVerbosity.default,
      boardHighlightIntensity = BoardHighlightIntensity.default,
      offsetCountDisplay = OffsetCountDisplay.default,
      aiSummaryPreference = AiSummaryPreference.default,
      ttsCoach = TtsCoach(
        enabled = false,
        autoSpeak = false,
        autoDelaySeconds = 1,
        voice = TtsVoice.default,
        ratePercent = 100,
        volumePercent = 80,
        queueBehavior = TtsQueueBehavior.default,
        muteDuringOpponentTurn = true
      ),
      learningOverlayDefaults = LearningOverlayDefaults(
        studyAiOverlay = false,
        openingAiOverlay = false
      ),
      telemetryPreference = TelemetryPreference.default
    )

  object TagKey:
    val schemaVersion = s"${tagPrefix}schemaVersion"
    val defaultSetLevel = s"${tagPrefix}defaultSetLevel"
    val preferredUsedLevel = s"${tagPrefix}preferredUsedLevel"
    val defaultFeatureTogglePrefix = s"${tagPrefix}defaultFeature_"
    def defaultFeatureToggle(key: String) = s"$defaultFeatureTogglePrefix$key"
    val overlayDensity = s"${tagPrefix}overlayDensity"
    val coachingCardVerbosity = s"${tagPrefix}coachingCardVerbosity"
    val boardHighlightIntensity = s"${tagPrefix}boardHighlightIntensity"
    val offsetCountDisplay = s"${tagPrefix}offsetCountDisplay"
    val aiSummaryPreference = s"${tagPrefix}aiSummaryPreference"
    val ttsEnabled = s"${tagPrefix}ttsEnabled"
    val ttsAutoSpeak = s"${tagPrefix}ttsAutoSpeak"
    val ttsAutoDelaySeconds = s"${tagPrefix}ttsAutoDelaySeconds"
    val ttsVoice = s"${tagPrefix}ttsVoice"
    val ttsRatePercent = s"${tagPrefix}ttsRatePercent"
    val ttsVolumePercent = s"${tagPrefix}ttsVolumePercent"
    val ttsQueueBehavior = s"${tagPrefix}ttsQueueBehavior"
    val ttsMuteDuringOpponentTurn = s"${tagPrefix}ttsMuteDuringOpponentTurn"
    val studyAiOverlay = s"${tagPrefix}studyAiOverlay"
    val openingAiOverlay = s"${tagPrefix}openingAiOverlay"
    val telemetryPreference = s"${tagPrefix}telemetryPreference"

  def fromTags(tags: Map[String, String]): Preferences =
    Preferences(
      defaultSetLevel = clampSetLevel(intTag(tags, TagKey.defaultSetLevel, default.defaultSetLevel)),
      preferredUsedLevel = clampSetLevel(
        intTag(tags, TagKey.preferredUsedLevel, default.preferredUsedLevel)
      ),
      defaultFeatureToggles = DefaultFeatureToggles.fromMap(
        DefaultFeatureToggle.all.map: feature =>
          feature.key -> boolTag(
            tags,
            TagKey.defaultFeatureToggle(feature.key),
            default.defaultFeatureToggles.valueFor(feature.key)
          )
        .toMap
      ),
      overlayDensity =
        OverlayDensity.fromKey(stringTag(tags, TagKey.overlayDensity, default.overlayDensity.key)),
      coachingCardVerbosity = CoachingCardVerbosity.fromKey(
        stringTag(tags, TagKey.coachingCardVerbosity, default.coachingCardVerbosity.key)
      ),
      boardHighlightIntensity = BoardHighlightIntensity.fromKey(
        stringTag(tags, TagKey.boardHighlightIntensity, default.boardHighlightIntensity.key)
      ),
      offsetCountDisplay = OffsetCountDisplay.fromKey(
        stringTag(tags, TagKey.offsetCountDisplay, default.offsetCountDisplay.key)
      ),
      aiSummaryPreference = AiSummaryPreference.fromKey(
        stringTag(tags, TagKey.aiSummaryPreference, default.aiSummaryPreference.key)
      ),
      ttsCoach = TtsCoach(
        enabled = boolTag(tags, TagKey.ttsEnabled, default.ttsCoach.enabled),
        autoSpeak = boolTag(tags, TagKey.ttsAutoSpeak, default.ttsCoach.autoSpeak),
        autoDelaySeconds = clamp(
          intTag(tags, TagKey.ttsAutoDelaySeconds, default.ttsCoach.autoDelaySeconds),
          minTtsAutoDelaySeconds,
          maxTtsAutoDelaySeconds
        ),
        voice = TtsVoice.fromKey(stringTag(tags, TagKey.ttsVoice, default.ttsCoach.voice.key)),
        ratePercent = clamp(
          intTag(tags, TagKey.ttsRatePercent, default.ttsCoach.ratePercent),
          minTtsRatePercent,
          maxTtsRatePercent
        ),
        volumePercent = clamp(
          intTag(tags, TagKey.ttsVolumePercent, default.ttsCoach.volumePercent),
          minTtsVolumePercent,
          maxTtsVolumePercent
        ),
        queueBehavior = TtsQueueBehavior.fromKey(
          stringTag(tags, TagKey.ttsQueueBehavior, default.ttsCoach.queueBehavior.key)
        ),
        muteDuringOpponentTurn = boolTag(
          tags,
          TagKey.ttsMuteDuringOpponentTurn,
          default.ttsCoach.muteDuringOpponentTurn
        )
      ),
      learningOverlayDefaults = LearningOverlayDefaults(
        studyAiOverlay = boolTag(tags, TagKey.studyAiOverlay, default.learningOverlayDefaults.studyAiOverlay),
        openingAiOverlay =
          boolTag(tags, TagKey.openingAiOverlay, default.learningOverlayDefaults.openingAiOverlay)
      ),
      telemetryPreference = TelemetryPreference.fromKey(
        stringTag(tags, TagKey.telemetryPreference, default.telemetryPreference.key)
      )
    )

  def writeToTags(tags: Map[String, String], preferences: Preferences): Map[String, String] =
    val featureTags = preferences.defaultFeatureToggles.asMap.map: (key, enabled) =>
      TagKey.defaultFeatureToggle(key) -> enabled.toString
    tags.filterNot(_._1.startsWith(tagPrefix)) ++ Map(
      TagKey.schemaVersion -> "1",
      TagKey.defaultSetLevel -> preferences.defaultSetLevel.toString,
      TagKey.preferredUsedLevel -> preferences.preferredUsedLevel.toString,
      TagKey.overlayDensity -> preferences.overlayDensity.key,
      TagKey.coachingCardVerbosity -> preferences.coachingCardVerbosity.key,
      TagKey.boardHighlightIntensity -> preferences.boardHighlightIntensity.key,
      TagKey.offsetCountDisplay -> preferences.offsetCountDisplay.key,
      TagKey.aiSummaryPreference -> preferences.aiSummaryPreference.key,
      TagKey.ttsEnabled -> preferences.ttsCoach.enabled.toString,
      TagKey.ttsAutoSpeak -> preferences.ttsCoach.autoSpeak.toString,
      TagKey.ttsAutoDelaySeconds -> preferences.ttsCoach.autoDelaySeconds.toString,
      TagKey.ttsVoice -> preferences.ttsCoach.voice.key,
      TagKey.ttsRatePercent -> preferences.ttsCoach.ratePercent.toString,
      TagKey.ttsVolumePercent -> preferences.ttsCoach.volumePercent.toString,
      TagKey.ttsQueueBehavior -> preferences.ttsCoach.queueBehavior.key,
      TagKey.ttsMuteDuringOpponentTurn -> preferences.ttsCoach.muteDuringOpponentTurn.toString,
      TagKey.studyAiOverlay -> preferences.learningOverlayDefaults.studyAiOverlay.toString,
      TagKey.openingAiOverlay -> preferences.learningOverlayDefaults.openingAiOverlay.toString,
      TagKey.telemetryPreference -> preferences.telemetryPreference.key
    ) ++ featureTags

  def writeFormDataToTags(tags: Map[String, String], formData: FormData): Map[String, String] =
    writeToTags(tags, formData.toPreferences)

  private def clamp(value: Int, min: Int, max: Int): Int = value.max(min).min(max)
  private def clampSetLevel(value: Int): Int = clamp(value, minSetLevel, maxSetLevel)
  private def intTag(tags: Map[String, String], key: String, default: Int): Int =
    tags.get(key).flatMap(_.toIntOption).getOrElse(default)
  private def stringTag(tags: Map[String, String], key: String, default: String): String =
    tags.get(key).filter(_.nonEmpty).getOrElse(default)
  private def boolTag(tags: Map[String, String], key: String, default: Boolean): Boolean =
    tags
      .get(key)
      .fold(default): value =>
        value == "true" || value == "1" || value.equalsIgnoreCase("yes")
