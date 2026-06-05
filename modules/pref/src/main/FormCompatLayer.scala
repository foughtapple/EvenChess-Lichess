package lila.pref

import play.api.mvc.Request

import lila.evenchess.UserSettings

// because the form structure has changed
// and the mobile app keeps sending the old format
object FormCompatLayer:

  private type FormData = Map[String, Seq[String]]

  def apply(pref: Pref, req: Request[?]): FormData =
    reqToFormData(req)
      .pipe(
        moveToAndRename(
          "clock",
          List(
            "clockTenths" -> "tenths",
            "clockBar" -> "bar",
            "clockSound" -> "sound",
            "moretime" -> "moretime"
          )
        )
      )
      .pipe(addMissing("clock.moretime", pref.moretime.toString))
      .pipe(
        moveTo(
          "behavior",
          List(
            "moveEvent",
            "premove",
            "takeback",
            "autoQueen",
            "autoThreefold",
            "submitMove",
            "confirmResign",
            "keyboardMove"
          )
        )
      )
      .pipe(
        moveTo(
          "display",
          List(
            "animation",
            "captured",
            "highlight",
            "destination",
            "coords",
            "replay",
            "pieceNotation",
            "blindfold"
          )
        )
      )
      .pipe(addEvenChessDefaults(pref))

  private def addMissing(path: String, default: String)(data: FormData): FormData =
    data.updated(path, data.get(path).filter(_.nonEmpty) | List(default))

  private def addEvenChessDefaults(pref: Pref)(data: FormData): FormData =
    val settings = UserSettings.fromTags(pref.tags).toFormData
    List(
      "evenchess.defaultSetLevel" -> settings.defaultSetLevel.toString,
      "evenchess.preferredUsedLevel" -> settings.preferredUsedLevel.toString,
      "evenchess.overlayDensity" -> settings.overlayDensity,
      "evenchess.coachingCardVerbosity" -> settings.coachingCardVerbosity,
      "evenchess.boardHighlightIntensity" -> settings.boardHighlightIntensity,
      "evenchess.offsetCountDisplay" -> settings.offsetCountDisplay,
      "evenchess.aiSummaryPreference" -> settings.aiSummaryPreference,
      "evenchess.ttsEnabled" -> settings.ttsEnabled.toString,
      "evenchess.ttsAutoSpeak" -> settings.ttsAutoSpeak.toString,
      "evenchess.ttsAutoDelaySeconds" -> settings.ttsAutoDelaySeconds.toString,
      "evenchess.ttsVoice" -> settings.ttsVoice,
      "evenchess.ttsRatePercent" -> settings.ttsRatePercent.toString,
      "evenchess.ttsVolumePercent" -> settings.ttsVolumePercent.toString,
      "evenchess.ttsQueueBehavior" -> settings.ttsQueueBehavior,
      "evenchess.ttsMuteDuringOpponentTurn" -> settings.ttsMuteDuringOpponentTurn.toString,
      "evenchess.studyAiOverlay" -> settings.studyAiOverlay.toString,
      "evenchess.openingAiOverlay" -> settings.openingAiOverlay.toString,
      "evenchess.telemetryPreference" -> settings.telemetryPreference
    ).foldLeft(data) { case (d, (path, default)) => addMissing(path, default)(d) }

  private def moveTo(prefix: String, fields: List[String]) =
    moveToAndRename(prefix, fields.map(f => (f, f)))

  private def moveToAndRename(prefix: String, fields: List[(String, String)])(data: FormData): FormData =
    fields.foldLeft(data) { case (d, (orig, dest)) =>
      val newField = s"$prefix.$dest"
      d + (newField -> ~d.get(newField).orElse(d.get(orig)))
    }

  private def reqToFormData(req: Request[?]): FormData =
    (req.body match
      case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
      case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined =>
        body.asMultipartFormData.get.asFormUrlEncoded
      case _ => Map.empty[String, Seq[String]]
    ) ++ req.queryString
