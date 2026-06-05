package lila.evenchess

import CoachingLadder.Level
import CoachingOverlays.Perspective

object OffsetCount:

  val featureKey = "offset_count"
  val unlockLevel: Level = Level(3)

  enum ExchangeSide:
    case Student
    case Opponent

  enum PieceRole(val leastValuableOrder: Int):
    case Pawn extends PieceRole(1)
    case Knight extends PieceRole(2)
    case Bishop extends PieceRole(3)
    case Rook extends PieceRole(4)
    case Queen extends PieceRole(5)
    case King extends PieceRole(6)

  enum ResultState(val wireName: String):
    case Equal extends ResultState("equal")
    case StudentWins extends ResultState("student_wins")
    case OpponentWins extends ResultState("opponent_wins")
    case Unknown extends ResultState("unknown")

  enum DisplayColor(val wireName: String):
    case Blue extends DisplayColor("blue")
    case Green extends DisplayColor("green")
    case Red extends DisplayColor("red")
    case Grey extends DisplayColor("grey")

  enum DisplayIcon(val wireName: String):
    case Shield extends DisplayIcon("shield")
    case Number extends DisplayIcon("number")
    case Disabled extends DisplayIcon("disabled")

  enum Confidence(val wireName: String):
    case DeterministicLocal extends Confidence("deterministic/local")
    case Stale extends Confidence("stale")
    case Unknown extends Confidence("unknown")

  final case class DisplaySemantics(
      resultState: ResultState,
      color: DisplayColor,
      icon: DisplayIcon,
      displayCount: Int,
      showNumber: Boolean,
      includePlusSign: Boolean
  )

  object DisplaySemantics:
    def forDelta(pieceCountDelta: Int): DisplaySemantics =
      if pieceCountDelta == 0 then
        DisplaySemantics(
          ResultState.Equal,
          DisplayColor.Blue,
          DisplayIcon.Shield,
          displayCount = 0,
          showNumber = false,
          includePlusSign = false
        )
      else if pieceCountDelta > 0 then
        DisplaySemantics(
          ResultState.StudentWins,
          DisplayColor.Green,
          DisplayIcon.Number,
          displayCount = pieceCountDelta,
          showNumber = true,
          includePlusSign = false
        )
      else
        DisplaySemantics(
          ResultState.OpponentWins,
          DisplayColor.Red,
          DisplayIcon.Number,
          displayCount = pieceCountDelta.abs,
          showNumber = true,
          includePlusSign = false
        )

    val unknown: DisplaySemantics =
      DisplaySemantics(
        ResultState.Unknown,
        DisplayColor.Grey,
        DisplayIcon.Disabled,
        displayCount = 0,
        showNumber = false,
        includePlusSign = false
      )

  final case class LegalExchangeStep(
      capturingSide: ExchangeSide,
      attacker: PieceRole,
      capturedPiece: PieceRole,
      legalCapture: Boolean,
      kingLegalAfterCapture: Boolean,
      illegalBecausePinned: Boolean,
      usesXrayOrDiscoveredCapture: Boolean,
      occupancyUpdated: Boolean
  ):
    def usable: Boolean =
      legalCapture && kingLegalAfterCapture && !illegalBecausePinned && occupancyUpdated

  final case class ExchangeChoice(capturingSide: ExchangeSide, alternatives: List[LegalExchangeStep])

  object StaticExchange:
    def chooseLeastValuableLegalAttacker(choice: ExchangeChoice): Option[LegalExchangeStep] =
      choice.alternatives.filter(_.usable).sortBy(_.attacker.leastValuableOrder).headOption

    def resolveChoices(choices: List[ExchangeChoice]): List[LegalExchangeStep] =
      choices.flatMap(chooseLeastValuableLegalAttacker)

    def pieceCountDeltaForStudent(steps: List[LegalExchangeStep]): Int =
      steps.filter(_.usable).foldLeft(0) { (delta, step) =>
        step.capturingSide match
          case ExchangeSide.Student  => delta + 1
          case ExchangeSide.Opponent => delta - 1
      }

  final case class CalculationBasis(
      legalCapturesProvided: Boolean,
      forcedFirstCaptureProvided: Boolean,
      optionalRecapturesProvided: Boolean,
      leastValuableAttackersConsidered: Boolean,
      occupancyUpdatesProvided: Boolean,
      pinsProvided: Boolean,
      xRaysProvided: Boolean,
      kingLegalityProvided: Boolean,
      discoveredCapturesProvided: Boolean,
      optimalLocalChoicesProvided: Boolean
  ):
    def readyForDeterministicLocalEstimate: Boolean =
      legalCapturesProvided &&
        forcedFirstCaptureProvided &&
        optionalRecapturesProvided &&
        leastValuableAttackersConsidered &&
        occupancyUpdatesProvided &&
        pinsProvided &&
        xRaysProvided &&
        kingLegalityProvided &&
        discoveredCapturesProvided &&
        optimalLocalChoicesProvided

  object CalculationBasis:
    val requiresLichessLegalFacts = true
    val rebuildsLegalMoveGeneration = false

  final case class OffsetCountInput(
      gameId: String,
      ply: Int,
      boardStateKey: String,
      square: String,
      initialMove: Option[String],
      perspective: Perspective,
      setLevel: Level,
      serverAuthorized: Boolean,
      rated: Boolean,
      visibleReveal: Boolean,
      stale: Boolean,
      auditId: String,
      calculationBasis: CalculationBasis,
      exchangeChoices: List[ExchangeChoice],
      repeatedRevealCount: Int,
      repeatedRevealCap: Int
  ):
    def hasRequiredPayloadIdentity: Boolean =
      gameId.nonEmpty && ply >= 0 && boardStateKey.nonEmpty && square.nonEmpty && auditId.nonEmpty

  final case class OffsetCountPayload(
      featureKey: String,
      gameId: String,
      ply: Int,
      boardStateKey: String,
      square: String,
      initialMove: Option[String],
      resultState: ResultState,
      displayColor: DisplayColor,
      displayIcon: DisplayIcon,
      displayCount: Int,
      sequenceSummary: Option[String],
      confidence: Confidence,
      auditId: String,
      assistanceCounts: Boolean,
      serverAuthorized: Boolean
  ):
    def isKnown: Boolean = resultState != ResultState.Unknown

    def displayNumberText: Option[String] =
      if displayIcon == DisplayIcon.Number then Some(displayCount.toString) else None

  object Resolver:
    def canReveal(input: OffsetCountInput): Boolean =
      input.serverAuthorized &&
        input.setLevel.value >= unlockLevel.value &&
        input.hasRequiredPayloadIdentity &&
        !input.stale &&
        input.repeatedRevealCount < input.repeatedRevealCap

    def resolve(input: OffsetCountInput, sequenceSummaryAllowed: Boolean): OffsetCountPayload =
      val known =
        canReveal(input) &&
          input.calculationBasis.readyForDeterministicLocalEstimate &&
          input.exchangeChoices.nonEmpty

      if !known then unknownPayload(input)
      else
        val chosenSteps = StaticExchange.resolveChoices(input.exchangeChoices)
        if chosenSteps.isEmpty then unknownPayload(input)
        else
          val semantics = DisplaySemantics.forDelta(StaticExchange.pieceCountDeltaForStudent(chosenSteps))
          OffsetCountPayload(
            featureKey = featureKey,
            gameId = input.gameId,
            ply = input.ply,
            boardStateKey = input.boardStateKey,
            square = input.square,
            initialMove = input.initialMove,
            resultState = semantics.resultState,
            displayColor = semantics.color,
            displayIcon = semantics.icon,
            displayCount = semantics.displayCount,
            sequenceSummary = if sequenceSummaryAllowed then Some(boundedSummary(chosenSteps)) else None,
            confidence = Confidence.DeterministicLocal,
            auditId = input.auditId,
            assistanceCounts = input.visibleReveal,
            serverAuthorized = input.serverAuthorized
          )

    private def unknownPayload(input: OffsetCountInput): OffsetCountPayload =
      OffsetCountPayload(
        featureKey = featureKey,
        gameId = input.gameId,
        ply = input.ply,
        boardStateKey = input.boardStateKey,
        square = input.square,
        initialMove = input.initialMove,
        resultState = ResultState.Unknown,
        displayColor = DisplayColor.Grey,
        displayIcon = DisplayIcon.Disabled,
        displayCount = 0,
        sequenceSummary = None,
        confidence = if input.stale then Confidence.Stale else Confidence.Unknown,
        auditId = input.auditId,
        assistanceCounts = false,
        serverAuthorized = input.serverAuthorized
      )

    private def boundedSummary(steps: List[LegalExchangeStep]): String =
      steps
        .take(6)
        .map(step => s"${step.capturingSide.toString}:${step.capturedPiece.toString}")
        .mkString(" ")

  object AssistanceRules:
    val hiddenAvailabilityCountsBeforeReveal = false
    val visibleRevealContributesToAssistanceLoad = true
    val repeatedRevealAbuseControlsRequired = true
    val stalePayloadsMustClear = true
