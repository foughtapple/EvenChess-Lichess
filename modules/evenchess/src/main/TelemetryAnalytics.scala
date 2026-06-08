package lila.evenchess

import AssistanceAccounting.{ PlayerAssistanceSummary, UsedOffset }
import CoachingLadder.Level
import MarketingFunnelPolicy.{ AttributionEvent, LandingVariant }
import MonetisationPolicy.PlanTier
import ProductInvariants.RequirementClass

object TelemetryAnalytics:

  enum PhaseQRequirement:
    case AuditMatchContracts
    case AuditSetLevelAtGameStart
    case AuditUsedLevelIncrease
    case AuditLiveEcePayloadGenerated
    case AuditCoachingDisplayActions
    case AuditProposedMoveChecks
    case AuditFinalSettlement
    case DisplayEngineEventEmission
    case LiveReviewRetentionSupport
    case PrivacySafeRawDataPolicy

  final case class PhaseQRequirementClassification(
      requirement: PhaseQRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object PhaseQRequirementClassifications:
    val all: List[PhaseQRequirementClassification] = List(
      PhaseQRequirementClassification(
        PhaseQRequirement.AuditMatchContracts,
        RequirementClass.EvenChessSpecific,
        "Every finalized EvenChess match contract is represented by a server-authored audit envelope."
      ),
      PhaseQRequirementClassification(
        PhaseQRequirement.AuditSetLevelAtGameStart,
        RequirementClass.EvenChessSpecific,
        "Each player's game-start Set Level is auditable and versioned."
      ),
      PhaseQRequirementClassification(
        PhaseQRequirement.AuditUsedLevelIncrease,
        RequirementClass.EvenChessSpecific,
        "Used Level increases are append-only audit events and never record decreases."
      ),
      PhaseQRequirementClassification(
        PhaseQRequirement.AuditLiveEcePayloadGenerated,
        RequirementClass.EvenChessSpecific,
        "Live ECE payload generation records request/output references and levels, not raw provider payloads."
      ),
      PhaseQRequirementClassification(
        PhaseQRequirement.AuditCoachingDisplayActions,
        RequirementClass.EvenChessSpecific,
        "Coaching render, hide, expand, suppress, and clear actions are display events where feasible."
      ),
      PhaseQRequirementClassification(
        PhaseQRequirement.AuditProposedMoveChecks,
        RequirementClass.EvenChessSpecific,
        "Proposed-move checks record the arrow move, legality, level, and whether the result was shown."
      ),
      PhaseQRequirementClassification(
        PhaseQRequirement.AuditFinalSettlement,
        RequirementClass.EvenChessSpecific,
        "Final Used Level, Assistance Load, Used Offset, and rating settlement are auditable with model versions."
      ),
      PhaseQRequirementClassification(
        PhaseQRequirement.DisplayEngineEventEmission,
        RequirementClass.AdaptedToLichessFork,
        "Future Display Engine adapters should emit shown, hidden, expanded, and suppressed card/overlay telemetry."
      ),
      PhaseQRequirementClassification(
        PhaseQRequirement.LiveReviewRetentionSupport,
        RequirementClass.EvenChessSpecific,
        "Retention keeps enough FEN/ECE references for Live White, Live Black, and Live Both review modes."
      ),
      PhaseQRequirementClassification(
        PhaseQRequirement.PrivacySafeRawDataPolicy,
        RequirementClass.EvenChessSpecific,
        "Telemetry avoids unnecessary raw AI prompts, AI responses, provider outputs, secrets, and filesystem paths."
      )
    )

  enum TelemetryRequirement:
    case AppendOnlyServerLedger
    case CompletenessForReplayAndReview
    case ClientAnalyticsSupplementOnly
    case VersionedEvents
    case CalibrationDashboards
    case ModeSeparation
    case StaleDegradedFallbackIdentification
    case PricingSafetySignals
    case FunnelAttribution
    case ConversionEventShape
    case LaunchDashboardGrouping
    case PrivacyAndRetention

  final case class TelemetryRequirementClassification(
      requirement: TelemetryRequirement,
      classification: RequirementClass,
      implementationDirection: String
  )

  object TelemetryRequirementClassifications:
    val all: List[TelemetryRequirementClassification] = List(
      TelemetryRequirementClassification(
        TelemetryRequirement.AppendOnlyServerLedger,
        RequirementClass.EvenChessSpecific,
        "Rated EvenChess games require an append-only server ledger with schema versions."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.CompletenessForReplayAndReview,
        RequirementClass.EvenChessSpecific,
        "Ledger completeness must support rating replay, assistance recomputation, fair-play review, and incident handling."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.ClientAnalyticsSupplementOnly,
        RequirementClass.EvenChessSpecific,
        "Client-only analytics can supplement product metrics but never authorize fairness, rating, or token decisions."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.VersionedEvents,
        RequirementClass.EvenChessSpecific,
        "Events include policy, model, and config versions where relevant."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.CalibrationDashboards,
        RequirementClass.EvenChessSpecific,
        "Dashboards expose residuals by Used Level, Assistance Load, time control, ECR band, exactness, feature mix, and follow-rate."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.ModeSeparation,
        RequirementClass.EvenChessSpecific,
        "Normal EvenChess, Target, casual, bot/AI practice, and review must remain separable."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.StaleDegradedFallbackIdentification,
        RequirementClass.EvenChessSpecific,
        "Dashboards identify stale, degraded, and engine-fallback games."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.PricingSafetySignals,
        RequirementClass.EvenChessSpecific,
        "Calibration surfaces show underpriced, overpriced, overused, and unsafe features or levels."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.FunnelAttribution,
        RequirementClass.EvenChessSpecific,
        "Funnel telemetry captures campaign attribution and signup/first-game/subscription linkage."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.ConversionEventShape,
        RequirementClass.EvenChessSpecific,
        "Conversion events need stable names, timestamps, dedupe IDs, pseudonymous IDs, campaign fields, and value/plan."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.LaunchDashboardGrouping,
        RequirementClass.AdaptedToLichessFork,
        "Launch dashboards group by source, campaign, variant, account type, token source, summary source, queue health, and plan."
      ),
      TelemetryRequirementClassification(
        TelemetryRequirement.PrivacyAndRetention,
        RequirementClass.EvenChessSpecific,
        "Collect only needed telemetry, use pseudonymous IDs, tier retention, and avoid sensitive ad data."
      )
    )

  enum EventFamily:
    case MatchLifecycle
    case Move
    case Coaching
    case Feature
    case Rating
    case AiEngine
    case Abuse
    case Funnel

  enum TelemetryEventName:
    case MatchSearchStarted
    case GameStarted
    case GameEnded
    case RatingApplied
    case MoveServerCommitted
    case ClockUpdated
    case PositionStateRecorded
    case CoachingRequested
    case CoachingGenerated
    case CoachingSurfaced
    case CoachingViewed
    case CoachingExpanded
    case CoachingSuppressed
    case OffsetCountShown
    case PlanViewed
    case ExplanationExpanded
    case AssistanceSummaryComputed
    case OffsetComputed
    case AiRequested
    case EngineJobCompleted
    case FallbackUsed
    case AbuseSignalRecorded
    case ReviewCaseOpened
    case LandingPageView
    case SignUpComplete
    case FirstGameCompleted
    case Purchase
    case MatchContractAudited
    case SetLevelAudited
    case UsedLevelIncreased
    case LiveEcePayloadGenerated
    case CoachingDisplayAction
    case ProposedMoveChecked
    case FinalSettlementAudited

  final case class VersionSet(
      schemaVersion: String,
      policyVersion: Option[String],
      modelVersion: Option[String],
      configVersion: Option[String]
  ):
    def hasSchemaVersion: Boolean = schemaVersion.nonEmpty

    def hasRelevantVersion: Boolean =
      policyVersion.exists(_.nonEmpty) || modelVersion.exists(_.nonEmpty) || configVersion.exists(_.nonEmpty)

  enum EventAuthority:
    case Server
    case ClientSupplement

  final case class TelemetryEvent(
      eventId: String,
      family: EventFamily,
      name: TelemetryEventName,
      authority: EventAuthority,
      schemaVersion: String,
      occurredAt: Long,
      pseudonymousUserId: Option[String],
      gameId: Option[String],
      rated: Boolean,
      versions: VersionSet,
      dedupeId: Option[String]
  ):
    def hasRequiredFields: Boolean =
      eventId.nonEmpty &&
        schemaVersion.nonEmpty &&
        occurredAt > 0 &&
        versions.hasSchemaVersion

    def serverAuthoritative: Boolean = authority == EventAuthority.Server

    def readyForRatedLedger: Boolean =
      !rated || (serverAuthoritative && hasRequiredFields && versions.hasRelevantVersion)

  final case class TelemetryLedger(events: Vector[TelemetryEvent]):
    def append(event: TelemetryEvent): TelemetryLedger =
      copy(events = events :+ event)

    def replaceEvent(event: TelemetryEvent): TelemetryLedger = this

    def allRatedEventsAppendOnlyServerSchemaVersioned: Boolean =
      events.filter(_.rated).forall(_.readyForRatedLedger)

    def supportsRatingReplay: Boolean =
      contains(TelemetryEventName.GameStarted) &&
        contains(TelemetryEventName.GameEnded) &&
        contains(TelemetryEventName.AssistanceSummaryComputed) &&
        contains(TelemetryEventName.OffsetComputed) &&
        contains(TelemetryEventName.RatingApplied)

    def supportsAssistanceRecomputation: Boolean =
      contains(TelemetryEventName.CoachingRequested) &&
        contains(TelemetryEventName.CoachingSurfaced) &&
        contains(TelemetryEventName.AssistanceSummaryComputed)

    def supportsFairPlayReviewAndIncidents: Boolean =
      contains(TelemetryEventName.PositionStateRecorded) &&
        contains(TelemetryEventName.AbuseSignalRecorded) &&
        contains(TelemetryEventName.ReviewCaseOpened)

    private def contains(name: TelemetryEventName): Boolean =
      events.exists(_.name == name)

  object TelemetryLedger:
    val empty: TelemetryLedger = TelemetryLedger(Vector.empty)

  enum AuditEventKind:
    case MatchContract
    case SetLevelAtGameStart
    case UsedLevelIncrease
    case LiveEcePayloadGenerated
    case CoachingDisplayAction
    case ProposedMoveCheck
    case FinalSettlement

  enum DisplayAuditAction:
    case Shown
    case Hidden
    case Expanded
    case Suppressed
    case Cleared

  final case class AuditEnvelope(
      eventId: String,
      kind: AuditEventKind,
      gameId: String,
      playerId: Option[String],
      ply: Option[Int],
      boardStateKey: Option[String],
      setLevel: Option[Level],
      usedLevelBefore: Option[Level],
      usedLevelAfter: Option[Level],
      assistanceLoad: Option[Double],
      usedOffset: Option[Int],
      ratingDelta: Option[Int],
      eceRequestId: Option[String],
      eceOutputRef: Option[String],
      proposedMoveUci: Option[String],
      proposedMoveLegal: Option[Boolean],
      resultShown: Option[Boolean],
      displayAction: Option[DisplayAuditAction],
      schemaVersion: String,
      policyVersion: String,
      modelVersion: Option[String],
      createdAt: Long,
      serverAuthoritative: Boolean,
      storesRawAiPrompt: Boolean,
      storesRawAiResponse: Boolean,
      storesRawProviderPayload: Boolean,
      exposesProviderSecret: Boolean,
      exposesFilesystemPath: Boolean
  ):
    def safePayload: Boolean =
      !storesRawAiPrompt &&
        !storesRawAiResponse &&
        !storesRawProviderPayload &&
        !exposesProviderSecret &&
        !exposesFilesystemPath

    def hasRequiredIdentity: Boolean =
      eventId.nonEmpty &&
        gameId.nonEmpty &&
        schemaVersion.nonEmpty &&
        policyVersion.nonEmpty &&
        createdAt > 0 &&
        serverAuthoritative

    def kindValid: Boolean =
      kind match
        case AuditEventKind.MatchContract =>
          playerId.isEmpty && modelVersion.exists(_.nonEmpty)
        case AuditEventKind.SetLevelAtGameStart =>
          playerId.exists(_.nonEmpty) && setLevel.nonEmpty
        case AuditEventKind.UsedLevelIncrease =>
          playerId.exists(_.nonEmpty) &&
            usedLevelBefore.nonEmpty &&
            usedLevelAfter.nonEmpty &&
            usedLevelAfter.exists(after => usedLevelBefore.exists(before => after.value > before.value))
        case AuditEventKind.LiveEcePayloadGenerated =>
          ply.exists(_ >= 0) &&
            boardStateKey.exists(_.nonEmpty) &&
            eceRequestId.exists(_.nonEmpty) &&
            eceOutputRef.exists(_.nonEmpty) &&
            setLevel.nonEmpty
        case AuditEventKind.CoachingDisplayAction =>
          playerId.exists(_.nonEmpty) &&
            ply.exists(_ >= 0) &&
            boardStateKey.exists(_.nonEmpty) &&
            displayAction.nonEmpty
        case AuditEventKind.ProposedMoveCheck =>
          playerId.exists(_.nonEmpty) &&
            ply.exists(_ >= 0) &&
            boardStateKey.exists(_.nonEmpty) &&
            proposedMoveUci.exists(_.matches("[a-h][1-8][a-h][1-8][qrbn]?")) &&
            proposedMoveLegal.nonEmpty &&
            resultShown.nonEmpty &&
            setLevel.nonEmpty
        case AuditEventKind.FinalSettlement =>
          playerId.exists(_.nonEmpty) &&
            usedLevelAfter.nonEmpty &&
            assistanceLoad.exists(_ >= 0) &&
            usedOffset.exists(_ >= 0) &&
            ratingDelta.nonEmpty &&
            modelVersion.exists(_.nonEmpty)

    def valid: Boolean =
      hasRequiredIdentity && kindValid && safePayload

  object AuditEnvelope:
    val currentSchemaVersion = "evenchess-telemetry-audit-v2-phase-q"

    def matchContract(
        record: LevelBasedMatchmaking.MatchContractAuditRecord,
        eventId: String,
        createdAt: Long
    ): AuditEnvelope =
      base(eventId, AuditEventKind.MatchContract, record.contract.gameId.getOrElse(record.contract.requestId), createdAt, record.contract.policyVersion)
        .copy(modelVersion = Some(record.calibrationModelVersion))

    def setLevelAtGameStart(
        eventId: String,
        gameId: String,
        playerId: String,
        setLevel: Level,
        policyVersion: String,
        createdAt: Long
    ): AuditEnvelope =
      base(eventId, AuditEventKind.SetLevelAtGameStart, gameId, createdAt, policyVersion)
        .copy(playerId = Some(playerId), setLevel = Some(setLevel))

    def usedLevelIncrease(
        eventId: String,
        gameId: String,
        playerId: String,
        before: Level,
        after: Level,
        policyVersion: String,
        createdAt: Long
    ): AuditEnvelope =
      base(eventId, AuditEventKind.UsedLevelIncrease, gameId, createdAt, policyVersion)
        .copy(playerId = Some(playerId), usedLevelBefore = Some(before), usedLevelAfter = Some(after))

    def liveEcePayload(
        eventId: String,
        entry: LiveCoaching.LiveEceHistoryEntry,
        side: CoachingOverlays.Perspective,
        policyVersion: String,
        createdAt: Long
    ): AuditEnvelope =
      val output = entry.outputFor(side)
      base(eventId, AuditEventKind.LiveEcePayloadGenerated, entry.gameId, createdAt, policyVersion)
        .copy(
          playerId = None,
          ply = Some(entry.ply),
          boardStateKey = Some(entry.positionHash),
          setLevel = Some(if side == CoachingOverlays.Perspective.White then entry.whiteRequestedLevel else entry.blackRequestedLevel),
          eceRequestId = Some(s"${entry.gameId}-ply-${entry.ply}-board"),
          eceOutputRef = output.map(_.outputRef)
        )

    def coachingDisplay(
        eventId: String,
        audit: CoachingPolicy.AuditEvent,
        action: DisplayAuditAction,
        createdAt: Long
    ): AuditEnvelope =
      base(eventId, AuditEventKind.CoachingDisplayAction, audit.gameId, createdAt, audit.policyVersion)
        .copy(
          playerId = Some(audit.playerId),
          ply = Some(audit.ply),
          boardStateKey = Some(audit.boardStateKey),
          setLevel = Some(audit.setLevel),
          usedLevelAfter = Some(audit.usedLevelAfter),
          displayAction = Some(action),
          modelVersion = Some(audit.schemaVersion)
        )

    def proposedMove(
        eventId: String,
        scheduled: LiveCoaching.LiveEceProposedMoveScheduledRequest,
        playerId: String,
        proposedMoveLegal: Boolean,
        resultShown: Boolean,
        policyVersion: String,
        createdAt: Long
    ): AuditEnvelope =
      base(eventId, AuditEventKind.ProposedMoveCheck, scheduled.gameId, createdAt, policyVersion)
        .copy(
          playerId = Some(playerId),
          ply = Some(scheduled.ply),
          boardStateKey = Some(scheduled.positionHash),
          setLevel = Some(
            if scheduled.requesterSide == CoachingOverlays.Perspective.White then scheduled.request.whiteLevel
            else scheduled.request.blackLevel
          ),
          eceRequestId = Some(scheduled.request.requestId),
          proposedMoveUci = Some(scheduled.request.proposedMoveUci),
          proposedMoveLegal = Some(proposedMoveLegal),
          resultShown = Some(resultShown)
        )

    def finalSettlement(
        eventId: String,
        gameId: String,
        summary: PlayerAssistanceSummary,
        usedOffset: UsedOffset,
        ratingDelta: Int,
        policyVersion: String,
        createdAt: Long
    ): AuditEnvelope =
      base(eventId, AuditEventKind.FinalSettlement, gameId, createdAt, policyVersion)
        .copy(
          playerId = Some(summary.playerId),
          usedLevelAfter = Some(summary.usedLevel),
          assistanceLoad = Some(summary.assistanceLoad.value),
          usedOffset = Some(usedOffset.value),
          ratingDelta = Some(ratingDelta),
          modelVersion = Some(usedOffset.modelVersion)
        )

    private def base(
        eventId: String,
        kind: AuditEventKind,
        gameId: String,
        createdAt: Long,
        policyVersion: String
    ): AuditEnvelope =
      AuditEnvelope(
        eventId = eventId,
        kind = kind,
        gameId = gameId,
        playerId = None,
        ply = None,
        boardStateKey = None,
        setLevel = None,
        usedLevelBefore = None,
        usedLevelAfter = None,
        assistanceLoad = None,
        usedOffset = None,
        ratingDelta = None,
        eceRequestId = None,
        eceOutputRef = None,
        proposedMoveUci = None,
        proposedMoveLegal = None,
        resultShown = None,
        displayAction = None,
        schemaVersion = currentSchemaVersion,
        policyVersion = policyVersion,
        modelVersion = None,
        createdAt = createdAt,
        serverAuthoritative = true,
        storesRawAiPrompt = false,
        storesRawAiResponse = false,
        storesRawProviderPayload = false,
        exposesProviderSecret = false,
        exposesFilesystemPath = false
      )

  final case class AuditLedger(events: Vector[AuditEnvelope]):
    def append(event: AuditEnvelope): AuditLedger =
      copy(events = events :+ event)

    def replaceEvent(event: AuditEnvelope): AuditLedger = this

    def allValid: Boolean =
      events.forall(_.valid)

    def contains(kind: AuditEventKind): Boolean =
      events.exists(_.kind == kind)

    def supportsAppendixRLiveAudit: Boolean =
      Set(
        AuditEventKind.MatchContract,
        AuditEventKind.SetLevelAtGameStart,
        AuditEventKind.UsedLevelIncrease,
        AuditEventKind.LiveEcePayloadGenerated,
        AuditEventKind.CoachingDisplayAction,
        AuditEventKind.FinalSettlement
      ).forall(contains)

  object AuditLedger:
    val empty: AuditLedger = AuditLedger(Vector.empty)

  final case class AuditCompletenessSnapshot(
      ledger: AuditLedger,
      telemetryLedger: TelemetryLedger,
      calibrationDimensions: Set[String],
      generatedAt: Long
  ):
    def valid: Boolean =
      generatedAt > 0 &&
        ledger.allValid &&
        ledger.supportsAppendixRLiveAudit &&
        telemetryLedger.allRatedEventsAppendOnlyServerSchemaVersioned &&
        CalibrationDashboard.includesRequiredDimensions(calibrationDimensions)

  enum DecisionDomain:
    case Fairness
    case Rating
    case Token
    case CoachingPermission
    case ProductAnalytics

  object AnalyticsAuthority:
    def clientAnalyticsMayAuthorize(domain: DecisionDomain): Boolean = false

    def clientAnalyticsMaySupplement(domain: DecisionDomain): Boolean =
      domain == DecisionDomain.ProductAnalytics

  enum GameModeSegment:
    case NormalEvenChess
    case Target
    case Casual
    case BotAiPractice
    case Review

  final case class CalibrationSlice(
      usedLevel: String,
      assistanceLoad: String,
      timeControl: String,
      ecrBand: String,
      exactness: String,
      featureMix: String,
      followRate: String,
      modeSegment: GameModeSegment,
      residual: Double
  ):
    def hasRequiredDimensions: Boolean =
      List(usedLevel, assistanceLoad, timeControl, ecrBand, exactness, featureMix, followRate).forall(_.nonEmpty)

  final case class RuntimeQualityFlags(
      stale: Boolean,
      degraded: Boolean,
      engineFallback: Boolean
  ):
    def flagged: Boolean = stale || degraded || engineFallback

  enum CalibrationSignal:
    case Balanced
    case Underpriced
    case Overpriced
    case Overused
    case Unsafe

  object CalibrationSignalDetector:
    def detect(residual: Double, usageRate: Double, abuseRate: Double): CalibrationSignal =
      if abuseRate > 0.05 then CalibrationSignal.Unsafe
      else if usageRate > 0.8 then CalibrationSignal.Overused
      else if residual > 0.15 then CalibrationSignal.Underpriced
      else if residual < -0.15 then CalibrationSignal.Overpriced
      else CalibrationSignal.Balanced

  object CalibrationDashboard:
    val requiredDimensions: Set[String] = Set(
      "Used Level",
      "Assistance Load",
      "time control",
      "ECR band",
      "exactness",
      "feature mix",
      "follow-rate"
    )

    def includesRequiredDimensions(dimensions: Set[String]): Boolean =
      requiredDimensions.subsetOf(dimensions)

    def separatesModeSegments(segments: Set[GameModeSegment]): Boolean =
      GameModeSegment.values.toSet.subsetOf(segments)

  final case class FunnelAttribution(
      utmSource: Option[String],
      utmMedium: Option[String],
      utmCampaign: Option[String],
      utmContent: Option[String],
      utmTerm: Option[String],
      clickId: Option[String],
      variant: LandingVariant,
      firstTouch: Option[String],
      latestTouch: Option[String],
      signupId: Option[String],
      firstGameId: Option[String],
      subscriptionId: Option[String]
  ):
    def hasCampaignIdentity: Boolean =
      List(utmSource, utmCampaign, clickId, firstTouch, latestTouch).exists(_.nonEmpty)

    def linksLifecycle: Boolean =
      signupId.nonEmpty && firstGameId.nonEmpty

  final case class ConversionEvent(
      name: AttributionEvent,
      occurredAt: Long,
      dedupeId: String,
      pseudonymousUserId: String,
      attribution: FunnelAttribution,
      valueAudCents: Option[Int],
      plan: Option[PlanTier]
  ):
    def validShape: Boolean =
      occurredAt > 0 &&
        dedupeId.nonEmpty &&
        pseudonymousUserId.nonEmpty &&
        attribution.hasCampaignIdentity &&
        valueAudCents.forall(_ >= 0)

  object ConversionDedupe:
    def unique(events: List[ConversionEvent]): List[ConversionEvent] =
      events.foldLeft((Set.empty[String], List.empty[ConversionEvent])) { case ((seen, kept), event) =>
        if seen.contains(event.dedupeId) then (seen, kept)
        else (seen + event.dedupeId, kept :+ event)
      }._2

  enum AccountType:
    case New
    case Returning
    case Subscriber

  enum TokenSource:
    case Onboarding
    case RewardedAd
    case Subscription
    case None

  enum SummarySource:
    case FreeToken
    case PremiumDaily
    case Cached
    case None

  enum QueueHealth:
    case Healthy
    case Slow
    case Degraded

  final case class LaunchDashboardGroup(
      source: String,
      campaign: String,
      variant: LandingVariant,
      accountType: AccountType,
      tokenSource: TokenSource,
      summarySource: SummarySource,
      queueHealth: QueueHealth,
      plan: Option[PlanTier],
      usesInvasiveRiskScoring: Boolean
  ):
    def validForMvp: Boolean =
      source.nonEmpty &&
        campaign.nonEmpty &&
        !usesInvasiveRiskScoring

  enum RetentionTier:
    case HotRawLogs
    case MediumDerivedMetrics
    case LongTermAggregates

  final case class RetentionPolicy(
      tier: RetentionTier,
      maxDays: Int,
      containsRawGameplay: Boolean
  ):
    def valid: Boolean =
      tier match
        case RetentionTier.HotRawLogs             => maxDays > 0 && maxDays <= 30 && containsRawGameplay
        case RetentionTier.MediumDerivedMetrics   => maxDays > 30 && maxDays <= 400
        case RetentionTier.LongTermAggregates     => maxDays >= 365 && !containsRawGameplay

  final case class PrivacyScan(
      collectsOnlyNeededEvents: Boolean,
      usesPseudonymousAnalyticsIds: Boolean,
      separatesRetentionTiers: Boolean,
      avoidsUnnecessarySensitiveAdData: Boolean
  ):
    def passes: Boolean =
      collectsOnlyNeededEvents &&
        usesPseudonymousAnalyticsIds &&
        separatesRetentionTiers &&
        avoidsUnnecessarySensitiveAdData

  enum AiDiagnosticLoggingPolicy:
    case Disabled
    case SanitizedMetadataOnly
    case PrivacyReviewedRedactedSamples

  final case class EceHistoryRetentionPlan(
      gameId: String,
      retainedFenCount: Int,
      retainedOutputRefCount: Int,
      supportsLiveWhite: Boolean,
      supportsLiveBlack: Boolean,
      supportsLiveBoth: Boolean,
      rollingRecentLimit: Int,
      paidSavedGame: Boolean,
      storesRawEcePayload: Boolean,
      storesRawProviderPayload: Boolean,
      storesRawAiPrompt: Boolean,
      storesRawAiResponse: Boolean,
      aiDiagnosticLoggingPolicy: AiDiagnosticLoggingPolicy,
      schemaVersion: String
  ):
    def supportsReviewModes: Boolean =
      supportsLiveWhite && supportsLiveBlack && supportsLiveBoth

    def safeStorage: Boolean =
      !storesRawEcePayload &&
        !storesRawProviderPayload &&
        !storesRawAiPrompt &&
        !storesRawAiResponse &&
        aiDiagnosticLoggingPolicy != AiDiagnosticLoggingPolicy.PrivacyReviewedRedactedSamples

    def valid: Boolean =
      gameId.nonEmpty &&
        retainedFenCount > 0 &&
        retainedOutputRefCount > 0 &&
        rollingRecentLimit >= 0 &&
        schemaVersion.nonEmpty &&
        supportsReviewModes &&
        safeStorage

  object EceHistoryRetentionPlan:
    def fromHistory(
        history: LiveCoaching.LiveEceHistoryRecord,
        rollingRecentLimit: Int,
        paidSavedGame: Boolean
    ): EceHistoryRetentionPlan =
      val refs = history.entries.flatMap(entry => List(entry.whiteOutput, entry.blackOutput).flatten.map(_.outputRef))
      EceHistoryRetentionPlan(
        gameId = history.gameId,
        retainedFenCount = history.fenHistory.size,
        retainedOutputRefCount = refs.distinct.size,
        supportsLiveWhite = history.entries.exists(_.whiteOutput.nonEmpty),
        supportsLiveBlack = history.entries.exists(_.blackOutput.nonEmpty),
        supportsLiveBoth = history.entries.exists(entry => entry.outputFor(entry.sideToMove).nonEmpty),
        rollingRecentLimit = rollingRecentLimit,
        paidSavedGame = paidSavedGame,
        storesRawEcePayload = false,
        storesRawProviderPayload = false,
        storesRawAiPrompt = false,
        storesRawAiResponse = false,
        aiDiagnosticLoggingPolicy = AiDiagnosticLoggingPolicy.SanitizedMetadataOnly,
        schemaVersion = "evenchess-ece-history-retention-v2-phase-q"
      )
