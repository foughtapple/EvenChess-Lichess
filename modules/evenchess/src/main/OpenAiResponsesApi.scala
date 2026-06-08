package lila.evenchess

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

import AiCoachPolicy.*
import CoachingLadder.ExactnessClass
import OpenAiCoaching.*

object OpenAiResponsesApi:

  final case class ApiSecret(value: String):
    def nonEmpty: Boolean = value.trim.nonEmpty
    def redacted: String = "[redacted-openai-key]"

  trait SecretSource:
    def read(secretRef: String): Option[ApiSecret]

  object EnvironmentSecretSource extends SecretSource:
    def read(secretRef: String): Option[ApiSecret] =
      sys.env.get(secretRef).map(_.trim).filter(_.nonEmpty).map(ApiSecret.apply)

  final case class RuntimeOverrides(
      model: Option[String] = None,
      endpoint: Option[String] = None,
      apiKeySecretRef: Option[String] = None
  )

  object RuntimeOverrides:
    def fromEnvironment(env: Map[String, String] = sys.env): RuntimeOverrides =
      RuntimeOverrides(
        model = env.get("EVENCHESS_OPENAI_MODEL").map(_.trim).filter(_.nonEmpty),
        endpoint = env.get("EVENCHESS_OPENAI_ENDPOINT").map(_.trim).filter(_.nonEmpty),
        apiKeySecretRef = env.get("EVENCHESS_OPENAI_API_KEY_SECRET_REF").map(_.trim).filter(_.nonEmpty)
      )

  def runtimeConfig(overrides: RuntimeOverrides = RuntimeOverrides.fromEnvironment()): OpenAiRuntimeConfig =
    OpenAiRuntimeConfig.defaultLocal.copy(
      model = overrides.model.getOrElse(OpenAiRuntimeConfig.defaultLocal.model),
      endpoint = overrides.endpoint.getOrElse(OpenAiRuntimeConfig.defaultLocal.endpoint),
      apiKeySecretRef = overrides.apiKeySecretRef.getOrElse(OpenAiRuntimeConfig.defaultLocal.apiKeySecretRef)
    )

  final case class HttpRequestRecord(
      endpoint: String,
      bearerToken: ApiSecret,
      body: String,
      timeout: Duration
  ):
    def safeForLogs: Boolean =
      !body.contains(bearerToken.value) &&
        endpoint.startsWith("https://") &&
        timeout.toMillis > 0

  final case class HttpResult(
      status: Int,
      body: String,
      latencyMillis: Int
  ):
    def success: Boolean = status >= 200 && status < 300

  trait ResponsesHttpTransport:
    def postJson(request: HttpRequestRecord): HttpResult

  final class JavaNetResponsesHttpTransport(
      client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
      now: () => Long = () => System.currentTimeMillis()
  ) extends ResponsesHttpTransport:

    def postJson(request: HttpRequestRecord): HttpResult =
      val started = now()
      val httpRequest =
        HttpRequest
          .newBuilder(URI.create(request.endpoint))
          .timeout(request.timeout)
          .header("Content-Type", "application/json")
          .header("Authorization", s"Bearer ${request.bearerToken.value}")
          .POST(HttpRequest.BodyPublishers.ofString(request.body))
          .build()
      val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
      HttpResult(response.statusCode(), response.body(), math.max(0L, now() - started).toInt)

  enum ProviderFailure:
    case InvalidConfig
    case MissingSecret
    case UnsafeRequestBody
    case HttpFailure
    case ResponseParseFailure

  final class ResponsesApiLiveProvider(
      secretSource: SecretSource = EnvironmentSecretSource,
      transport: ResponsesHttpTransport = JavaNetResponsesHttpTransport(),
      timeout: Duration = Duration.ofSeconds(20)
  ) extends LiveOpenAiProvider:

    def generateLive(
        request: AiLiveRequest,
        prompt: PromptEnvelope,
        config: OpenAiRuntimeConfig,
        attempt: Int
    ): OpenAiLiveProviderResponse =
      generateLiveEither(request, prompt, config, attempt).fold(
        failure => FailureResponses.live(request, attempt, failure),
        identity
      )

    def generateLiveEither(
        request: AiLiveRequest,
        prompt: PromptEnvelope,
        config: OpenAiRuntimeConfig,
        attempt: Int
    ): Either[ProviderFailure, OpenAiLiveProviderResponse] =
      if !config.validServerSide then Left(ProviderFailure.InvalidConfig)
      else
        secretSource.read(config.apiKeySecretRef) match
          case None => Left(ProviderFailure.MissingSecret)
          case Some(secret) =>
            val body = RequestBodies.live(request, prompt, config, attempt)
            val httpRequest = HttpRequestRecord(config.endpoint, secret, body, timeout)
            if !httpRequest.safeForLogs then Left(ProviderFailure.UnsafeRequestBody)
            else
              val response = transport.postJson(httpRequest)
              if !response.success then Left(ProviderFailure.HttpFailure)
              else ResponseParsers.live(response.body, response.latencyMillis)

  final class ResponsesApiSummaryProvider(
      secretSource: SecretSource = EnvironmentSecretSource,
      transport: ResponsesHttpTransport = JavaNetResponsesHttpTransport(),
      timeout: Duration = Duration.ofSeconds(30)
  ) extends SummaryOpenAiProvider:

    def generateSummary(
        job: PostGameSummaryJob,
        config: OpenAiRuntimeConfig,
        selectedGames: List[SummaryGameWindowItem]
    ): OpenAiSummaryProviderResponse =
      generateSummaryEither(job, config, selectedGames).fold(
        failure => FailureResponses.summary(job, failure),
        identity
      )

    def generateSummaryEither(
        job: PostGameSummaryJob,
        config: OpenAiRuntimeConfig,
        selectedGames: List[SummaryGameWindowItem]
    ): Either[ProviderFailure, OpenAiSummaryProviderResponse] =
      if !config.validServerSide then Left(ProviderFailure.InvalidConfig)
      else
        secretSource.read(config.apiKeySecretRef) match
          case None => Left(ProviderFailure.MissingSecret)
          case Some(secret) =>
            val body = RequestBodies.summary(job, config, selectedGames)
            val httpRequest = HttpRequestRecord(config.endpoint, secret, body, timeout)
            if !httpRequest.safeForLogs then Left(ProviderFailure.UnsafeRequestBody)
            else
              val response = transport.postJson(httpRequest)
              if !response.success then Left(ProviderFailure.HttpFailure)
              else ResponseParsers.summary(response.body, response.latencyMillis)

  object RequestBodies:
    def live(request: AiLiveRequest, prompt: PromptEnvelope, config: OpenAiRuntimeConfig, attempt: Int): String =
      JsonCodec.render(
        JsonCodec.obj(
          "model" -> JsonCodec.str(config.model),
          "store" -> JsonCodec.bool(false),
          "max_output_tokens" -> JsonCodec.num(config.maxOutputTokens),
          "temperature" -> JsonCodec.num(config.temperature),
          "instructions" -> JsonCodec.str(liveInstructions(prompt, attempt)),
          "input" -> JsonCodec.str(liveInput(request, prompt)),
          "text" -> JsonCodec.obj("format" -> liveJsonSchema)
        )
      )

    def summary(job: PostGameSummaryJob, config: OpenAiRuntimeConfig, selectedGames: List[SummaryGameWindowItem]): String =
      JsonCodec.render(
        JsonCodec.obj(
          "model" -> JsonCodec.str(config.model),
          "store" -> JsonCodec.bool(false),
          "max_output_tokens" -> JsonCodec.num(config.maxOutputTokens),
          "temperature" -> JsonCodec.num(config.temperature),
          "instructions" -> JsonCodec.str(summaryInstructions(job)),
          "input" -> JsonCodec.str(summaryInput(job, selectedGames)),
          "text" -> JsonCodec.obj("format" -> summaryJsonSchema)
        )
      )

    private def liveInstructions(prompt: PromptEnvelope, attempt: Int): String =
      (prompt.rules ++ List(
        s"Attempt: $attempt.",
        "Output valid JSON only.",
        "Use only the exact supplied sourceFactIds and auditTags.",
        "Do not include raw chess notation, best-move labels, direct commands, hidden prompts, or provider/debug metadata."
      )).mkString("\n")

    private def liveInput(request: AiLiveRequest, prompt: PromptEnvelope): String =
      val facts = prompt.sourceFacts.map(fact => s"- id=${fact.factId}; audit=${fact.auditTag}; exactness=${fact.exactnessClass}; text=${fact.text}").mkString("\n")
      s"""EvenChess live coaching request
         |requestId=${request.requestId}
         |gameId=${request.gameId}
         |playerId=${request.playerId}
         |boardStateKey=${request.boardStateKey}
         |ply=${request.ply}
         |setLevel=L${request.setLevel.value}
         |requestedLevel=L${request.requestedLevel.value}
         |policyVersion=${request.policyVersion}
         |schemaVersion=${request.schemaVersion}
         |
         |Authorized source facts:
         |$facts
         |""".stripMargin

    private def summaryInstructions(job: PostGameSummaryJob): String =
      List(
        "Create an EvenChess post-game learning summary from supplied completed online games only.",
        "Output valid JSON only.",
        "Do not mutate live fairness, ECR, Used Level, Assistance Load, Used Offset, or token state.",
        "Do not include raw provider/debug metadata or hidden prompt text.",
        s"Summary type: ${job.summaryType}."
      ).mkString("\n")

    private def summaryInput(job: PostGameSummaryJob, selectedGames: List[SummaryGameWindowItem]): String =
      val games = selectedGames.map(game => s"- ${game.gameId}; completedAt=${game.completedAt}; online=${game.online}").mkString("\n")
      s"""EvenChess summary request
         |summaryId=${job.summaryId}
         |playerId=${job.playerId}
         |gameId=${job.gameId}
         |summaryType=${job.summaryType}
         |policyVersion=${job.policyVersion}
         |schemaVersion=${job.schemaVersion}
         |
         |Eligible source games:
         |$games
         |""".stripMargin

    private val exactnessNames = ExactnessClass.values.map(_.toString).toList

    private val liveJsonSchema =
      val requiredLiveFields =
        List("policyVersion", "schemaVersion", "exactnessClass", "message", "visualCues", "sourceFactIds", "auditTags", "boardStateKey")
      JsonCodec.obj(
        "type" -> JsonCodec.str("json_schema"),
        "name" -> JsonCodec.str("evenchess_live_ai"),
        "strict" -> JsonCodec.bool(true),
        "schema" -> JsonCodec.obj(
          "type" -> JsonCodec.str("object"),
          "additionalProperties" -> JsonCodec.bool(false),
          "required" -> JsonCodec.arr(requiredLiveFields.map(JsonCodec.str)),
          "properties" -> JsonCodec.obj(
            "policyVersion" -> JsonCodec.obj("type" -> JsonCodec.str("string")),
            "schemaVersion" -> JsonCodec.obj("type" -> JsonCodec.str("string")),
            "exactnessClass" -> JsonCodec.obj("type" -> JsonCodec.str("string"), "enum" -> JsonCodec.arr(exactnessNames.map(JsonCodec.str))),
            "message" -> JsonCodec.obj("type" -> JsonCodec.str("string")),
            "visualCues" -> JsonCodec.obj("type" -> JsonCodec.str("array"), "items" -> JsonCodec.obj("type" -> JsonCodec.str("string"))),
            "sourceFactIds" -> JsonCodec.obj("type" -> JsonCodec.str("array"), "items" -> JsonCodec.obj("type" -> JsonCodec.str("string"))),
            "auditTags" -> JsonCodec.obj("type" -> JsonCodec.str("array"), "items" -> JsonCodec.obj("type" -> JsonCodec.str("string"))),
            "boardStateKey" -> JsonCodec.obj("type" -> JsonCodec.str("string"))
          )
        )
      )

    private val summaryJsonSchema =
      JsonCodec.obj(
        "type" -> JsonCodec.str("json_schema"),
        "name" -> JsonCodec.str("evenchess_post_game_summary"),
        "strict" -> JsonCodec.bool(true),
        "schema" -> JsonCodec.obj(
          "type" -> JsonCodec.str("object"),
          "additionalProperties" -> JsonCodec.bool(false),
          "required" -> JsonCodec.arr(List("summaryId", "summaryType", "title", "bullets", "sourceGameIds", "sourceFactIds", "policyVersion", "schemaVersion").map(JsonCodec.str)),
          "properties" -> JsonCodec.obj(
            "summaryId" -> JsonCodec.obj("type" -> JsonCodec.str("string")),
            "summaryType" -> JsonCodec.obj("type" -> JsonCodec.str("string"), "enum" -> JsonCodec.arr(SummaryType.values.map(value => JsonCodec.str(value.toString)).toList)),
            "title" -> JsonCodec.obj("type" -> JsonCodec.str("string")),
            "bullets" -> JsonCodec.obj("type" -> JsonCodec.str("array"), "items" -> JsonCodec.obj("type" -> JsonCodec.str("string"))),
            "sourceGameIds" -> JsonCodec.obj("type" -> JsonCodec.str("array"), "items" -> JsonCodec.obj("type" -> JsonCodec.str("string"))),
            "sourceFactIds" -> JsonCodec.obj("type" -> JsonCodec.str("array"), "items" -> JsonCodec.obj("type" -> JsonCodec.str("string"))),
            "policyVersion" -> JsonCodec.obj("type" -> JsonCodec.str("string")),
            "schemaVersion" -> JsonCodec.obj("type" -> JsonCodec.str("string"))
          )
        )
      )

  object ResponseParsers:
    def live(body: String, latencyMillis: Int): Either[ProviderFailure, OpenAiLiveProviderResponse] =
      for
        parsed <- JsonCodec.parse(body).left.map(_ => ProviderFailure.ResponseParseFailure)
        responseId <- responseId(parsed).toRight(ProviderFailure.ResponseParseFailure)
        outputText <- outputText(parsed).toRight(ProviderFailure.ResponseParseFailure)
        outputJson <- JsonCodec.parse(outputText).left.map(_ => ProviderFailure.ResponseParseFailure)
        output <- liveOutput(outputJson)
      yield OpenAiLiveProviderResponse(
        providerRequestId = responseId,
        output = output,
        usage = usage(parsed),
        latencyMillis = latencyMillis,
        rawProviderTextStored = false
      )

    def summary(body: String, latencyMillis: Int): Either[ProviderFailure, OpenAiSummaryProviderResponse] =
      for
        parsed <- JsonCodec.parse(body).left.map(_ => ProviderFailure.ResponseParseFailure)
        responseId <- responseId(parsed).toRight(ProviderFailure.ResponseParseFailure)
        outputText <- outputText(parsed).toRight(ProviderFailure.ResponseParseFailure)
        outputJson <- JsonCodec.parse(outputText).left.map(_ => ProviderFailure.ResponseParseFailure)
        output <- summaryOutput(outputJson)
      yield OpenAiSummaryProviderResponse(
        providerRequestId = responseId,
        output = output,
        usage = usage(parsed),
        latencyMillis = latencyMillis,
        rawProviderTextStored = false
      )

    private def responseId(json: JsonCodec.JsonValue): Option[String] =
      JsonCodec.field(json, "id").flatMap(JsonCodec.asString)

    private def outputText(json: JsonCodec.JsonValue): Option[String] =
      JsonCodec.field(json, "output_text").flatMap(JsonCodec.asString)
        .orElse(
          for
            output <- JsonCodec.field(json, "output").flatMap(JsonCodec.asArray)
            item <- output.find(item => JsonCodec.field(item, "type").flatMap(JsonCodec.asString).contains("message"))
            content <- JsonCodec.field(item, "content").flatMap(JsonCodec.asArray)
            textItem <- content.find(item => JsonCodec.field(item, "type").flatMap(JsonCodec.asString).exists(value => value == "output_text" || value == "text"))
            text <- JsonCodec.field(textItem, "text").flatMap(JsonCodec.asString)
          yield text
        )

    private def usage(json: JsonCodec.JsonValue): OpenAiUsage =
      val usageJson = JsonCodec.field(json, "usage")
      OpenAiUsage(
        inputTokens = usageJson.flatMap(JsonCodec.field(_, "input_tokens")).flatMap(JsonCodec.asInt).getOrElse(0),
        outputTokens = usageJson.flatMap(JsonCodec.field(_, "output_tokens")).flatMap(JsonCodec.asInt).getOrElse(0),
        costMicros = 0L
      )

    private def liveOutput(json: JsonCodec.JsonValue): Either[ProviderFailure, AiLiveOutput] =
      for
        policyVersion <- requiredString(json, "policyVersion")
        schemaVersion <- requiredString(json, "schemaVersion")
        exactnessClassName <- requiredString(json, "exactnessClass")
        exactness <- exactnessClass(exactnessClassName)
        message <- requiredString(json, "message")
        visualCues <- requiredStringList(json, "visualCues")
        sourceFactIds <- requiredStringList(json, "sourceFactIds")
        auditTags <- requiredStringList(json, "auditTags")
        boardStateKey <- requiredString(json, "boardStateKey")
      yield AiLiveOutput(policyVersion, schemaVersion, exactness, message, visualCues, sourceFactIds, auditTags, boardStateKey)

    private def summaryOutput(json: JsonCodec.JsonValue): Either[ProviderFailure, PostGameSummaryOutput] =
      for
        summaryId <- requiredString(json, "summaryId")
        summaryTypeName <- requiredString(json, "summaryType")
        summaryType <- parseSummaryType(summaryTypeName)
        title <- requiredString(json, "title")
        bullets <- requiredStringList(json, "bullets")
        sourceGameIds <- requiredStringList(json, "sourceGameIds")
        sourceFactIds <- requiredStringList(json, "sourceFactIds")
        policyVersion <- requiredString(json, "policyVersion")
        schemaVersion <- requiredString(json, "schemaVersion")
      yield PostGameSummaryOutput(summaryId, summaryType, title, bullets, sourceGameIds, sourceFactIds, policyVersion, schemaVersion)

    private def requiredString(json: JsonCodec.JsonValue, name: String): Either[ProviderFailure, String] =
      JsonCodec.field(json, name).flatMap(JsonCodec.asString).filter(_.nonEmpty).toRight(ProviderFailure.ResponseParseFailure)

    private def requiredStringList(json: JsonCodec.JsonValue, name: String): Either[ProviderFailure, List[String]] =
      JsonCodec.field(json, name)
        .flatMap(JsonCodec.asArray)
        .map(_.flatMap(JsonCodec.asString))
        .filter(_.nonEmpty)
        .toRight(ProviderFailure.ResponseParseFailure)

    private def exactnessClass(name: String): Either[ProviderFailure, ExactnessClass] =
      ExactnessClass.values.find(_.toString == name).toRight(ProviderFailure.ResponseParseFailure)

    private def parseSummaryType(name: String): Either[ProviderFailure, SummaryType] =
      SummaryType.values.find(_.toString == name).toRight(ProviderFailure.ResponseParseFailure)

  object FailureResponses:
    def live(request: AiLiveRequest, attempt: Int, failure: ProviderFailure): OpenAiLiveProviderResponse =
      OpenAiLiveProviderResponse(
        providerRequestId = s"openai-responses-${failure.toString}-$attempt",
        output = AiLiveOutput(
          policyVersion = request.policyVersion,
          schemaVersion = request.schemaVersion,
          exactnessClass = ExactnessClass.None,
          message = "",
          visualCues = Nil,
          sourceFactIds = Nil,
          auditTags = Nil,
          boardStateKey = request.boardStateKey
        ),
        usage = OpenAiUsage.zero,
        latencyMillis = 0,
        rawProviderTextStored = false
      )

    def summary(job: PostGameSummaryJob, failure: ProviderFailure): OpenAiSummaryProviderResponse =
      OpenAiSummaryProviderResponse(
        providerRequestId = s"openai-responses-${failure.toString}",
        output = PostGameSummaryOutput(
          summaryId = job.summaryId,
          summaryType = job.summaryType,
          title = "",
          bullets = Nil,
          sourceGameIds = Nil,
          sourceFactIds = Nil,
          policyVersion = job.policyVersion,
          schemaVersion = job.schemaVersion
        ),
        usage = OpenAiUsage.zero,
        latencyMillis = 0,
        rawProviderTextStored = false
      )

  object JsonCodec:
    enum JsonValue:
      case Obj(fields: Map[String, JsonValue])
      case Arr(values: List[JsonValue])
      case Str(value: String)
      case Num(value: BigDecimal)
      case Bool(value: Boolean)
      case Null

    def obj(fields: (String, JsonValue)*): JsonValue = JsonValue.Obj(fields.toMap)
    def arr(values: List[JsonValue]): JsonValue = JsonValue.Arr(values)
    def str(value: String): JsonValue = JsonValue.Str(value)
    def num(value: Int): JsonValue = JsonValue.Num(BigDecimal(value))
    def num(value: BigDecimal): JsonValue = JsonValue.Num(value)
    def bool(value: Boolean): JsonValue = JsonValue.Bool(value)

    def field(value: JsonValue, name: String): Option[JsonValue] =
      value match
        case JsonValue.Obj(fields) => fields.get(name)
        case _                     => None

    def asString(value: JsonValue): Option[String] =
      value match
        case JsonValue.Str(text) => Some(text)
        case _                   => None

    def asArray(value: JsonValue): Option[List[JsonValue]] =
      value match
        case JsonValue.Arr(values) => Some(values)
        case _                     => None

    def asInt(value: JsonValue): Option[Int] =
      value match
        case JsonValue.Num(number) if number.isValidInt => Some(number.toInt)
        case _                                          => None

    def render(value: JsonValue): String =
      value match
        case JsonValue.Obj(fields) =>
          fields.map { case (key, fieldValue) => s"${renderString(key)}:${render(fieldValue)}" }.mkString("{", ",", "}")
        case JsonValue.Arr(values) => values.map(render).mkString("[", ",", "]")
        case JsonValue.Str(text)   => renderString(text)
        case JsonValue.Num(number) => number.toString
        case JsonValue.Bool(value) => value.toString
        case JsonValue.Null        => "null"

    private def renderString(text: String): String =
      text.flatMap {
        case '"'  => "\\\""
        case '\\' => "\\\\"
        case '\b' => "\\b"
        case '\f' => "\\f"
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case c if c < ' ' => f"\\u${c.toInt}%04x"
        case c            => c.toString
      }.mkString("\"", "", "\"")

    def parse(input: String): Either[String, JsonValue] =
      Parser(input).parse()

    private final class Parser(input: String):
      private var index = 0

      def parse(): Either[String, JsonValue] =
        skipWhitespace()
        parseValue().flatMap: value =>
          skipWhitespace()
          if index == input.length then Right(value)
          else Left(s"Trailing JSON at $index")

      private def parseValue(): Either[String, JsonValue] =
        skipWhitespace()
        if index >= input.length then Left("Unexpected end of JSON")
        else
          input(index) match
            case '{' => parseObject()
            case '[' => parseArray()
            case '"' => parseString().map(JsonValue.Str.apply)
            case 't' => consumeLiteral("true", JsonValue.Bool(true))
            case 'f' => consumeLiteral("false", JsonValue.Bool(false))
            case 'n' => consumeLiteral("null", JsonValue.Null)
            case '-' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' => parseNumber()
            case other => Left(s"Unexpected JSON character '$other' at $index")

      private def parseObject(): Either[String, JsonValue] =
        index += 1
        skipWhitespace()
        var fields = Map.empty[String, JsonValue]
        if consumeIf('}') then Right(JsonValue.Obj(fields))
        else
          var done = false
          while !done do
            skipWhitespace()
            parseString() match
              case Left(error) => return Left(error)
              case Right(key) =>
                skipWhitespace()
                if !consumeIf(':') then return Left(s"Expected ':' at $index")
                parseValue() match
                  case Left(error) => return Left(error)
                  case Right(value) => fields = fields.updated(key, value)
                skipWhitespace()
                if consumeIf('}') then done = true
                else if !consumeIf(',') then return Left(s"Expected ',' or '}' at $index")
          Right(JsonValue.Obj(fields))

      private def parseArray(): Either[String, JsonValue] =
        index += 1
        skipWhitespace()
        var values = List.empty[JsonValue]
        if consumeIf(']') then Right(JsonValue.Arr(values))
        else
          var done = false
          while !done do
            parseValue() match
              case Left(error) => return Left(error)
              case Right(value) => values = values :+ value
            skipWhitespace()
            if consumeIf(']') then done = true
            else if !consumeIf(',') then return Left(s"Expected ',' or ']' at $index")
          Right(JsonValue.Arr(values))

      private def parseString(): Either[String, String] =
        if !consumeIf('"') then Left(s"""Expected '"' at $index""")
        else
          val builder = StringBuilder()
          while index < input.length do
            val char = input(index)
            index += 1
            char match
              case '"' => return Right(builder.result())
              case '\\' =>
                if index >= input.length then return Left("Unterminated JSON escape")
                val escaped = input(index)
                index += 1
                escaped match
                  case '"'  => builder.append('"')
                  case '\\' => builder.append('\\')
                  case '/'  => builder.append('/')
                  case 'b'  => builder.append('\b')
                  case 'f'  => builder.append('\f')
                  case 'n'  => builder.append('\n')
                  case 'r'  => builder.append('\r')
                  case 't'  => builder.append('\t')
                  case 'u' =>
                    if index + 4 > input.length then return Left("Invalid unicode escape")
                    val code = input.substring(index, index + 4)
                    index += 4
                    try builder.append(Integer.parseInt(code, 16).toChar)
                    catch case _: NumberFormatException => return Left("Invalid unicode escape")
                  case other => return Left(s"Invalid JSON escape '$other'")
              case other => builder.append(other)
          Left("Unterminated JSON string")

      private def parseNumber(): Either[String, JsonValue] =
        val start = index
        if consumeIf('-') then ()
        consumeDigits()
        if consumeIf('.') then consumeDigits()
        if index < input.length && (input(index) == 'e' || input(index) == 'E') then
          index += 1
          if index < input.length && (input(index) == '+' || input(index) == '-') then index += 1
          consumeDigits()
        try Right(JsonValue.Num(BigDecimal(input.substring(start, index))))
        catch case _: NumberFormatException => Left(s"Invalid number at $start")

      private def consumeDigits(): Unit =
        while index < input.length && input(index).isDigit do index += 1

      private def consumeLiteral(literal: String, value: JsonValue): Either[String, JsonValue] =
        if input.startsWith(literal, index) then
          index += literal.length
          Right(value)
        else Left(s"Expected '$literal' at $index")

      private def consumeIf(char: Char): Boolean =
        if index < input.length && input(index) == char then
          index += 1
          true
        else false

      private def skipWhitespace(): Unit =
        while index < input.length && input(index).isWhitespace do index += 1
