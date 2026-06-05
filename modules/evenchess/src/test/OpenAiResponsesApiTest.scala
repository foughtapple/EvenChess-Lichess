package lila.evenchess

import java.time.Duration

class OpenAiResponsesApiTest extends munit.FunSuite:

  import AiCoachPolicy.*
  import CoachingLadder.{ ExactnessClass, Level }
  import OpenAiCoaching.*
  import OpenAiResponsesApi.*
  import OpenAiResponsesApi.JsonCodec

  private val fact =
    SourceFact(
      factId = "fact-openai-1",
      text = "The authorized packet says the player can improve piece activity.",
      boardStateKey = "board-openai",
      exactnessClass = ExactnessClass.Heuristic,
      auditTag = "audit-openai-1"
    )

  private val liveRequest =
    AiLiveRequest(
      requestId = "live-openai-1",
      gameId = "game-openai-1",
      playerId = "player-openai-1",
      boardStateKey = "board-openai",
      ply = 12,
      setLevel = Level(5),
      requestedLevel = Level(5),
      policyVersion = "policy-openai-v1",
      promptVersion = OpenAiRuntimeConfig.defaultLocal.promptVersion,
      schemaVersion = OpenAiRuntimeConfig.defaultLocal.schemaVersion,
      authorizedFacts = List(fact)
    )

  private val prompt = PromptBuilder.live(liveRequest, OpenAiRuntimeConfig.defaultLocal)

  private final class StaticSecret(secret: Option[String]) extends SecretSource:
    def read(secretRef: String): Option[ApiSecret] = secret.map(ApiSecret.apply)

  private final class CapturingTransport(responseBody: String, status: Int = 200) extends ResponsesHttpTransport:
    var calls = 0
    var lastRequest: Option[HttpRequestRecord] = None

    def postJson(request: HttpRequestRecord): HttpResult =
      calls = calls + 1
      lastRequest = Some(request)
      HttpResult(status = status, body = responseBody, latencyMillis = 37)

  private def jsonResponse(output: JsonCodec.JsonValue): String =
    JsonCodec.render(
      JsonCodec.obj(
        "id" -> JsonCodec.str("resp_evenchess_1"),
        "output_text" -> JsonCodec.str(JsonCodec.render(output)),
        "usage" -> JsonCodec.obj(
          "input_tokens" -> JsonCodec.num(123),
          "output_tokens" -> JsonCodec.num(45)
        )
      )
    )

  private val liveOutputJson =
    JsonCodec.obj(
      "policyVersion" -> JsonCodec.str(liveRequest.policyVersion),
      "schemaVersion" -> JsonCodec.str(liveRequest.schemaVersion),
      "exactnessClass" -> JsonCodec.str(ExactnessClass.Heuristic.toString),
      "message" -> JsonCodec.str("Improve your least active piece before forcing matters."),
      "visualCues" -> JsonCodec.arr(List(JsonCodec.str("soft-highlight"))),
      "sourceFactIds" -> JsonCodec.arr(List(JsonCodec.str(fact.factId))),
      "auditTags" -> JsonCodec.arr(List(JsonCodec.str(fact.auditTag))),
      "boardStateKey" -> JsonCodec.str(liveRequest.boardStateKey)
    )

  private val summaryGame =
    SummaryGameWindowItem(
      gameId = "summary-game-1",
      completed = true,
      online = true,
      botGame = false,
      computerGame = false,
      studyGame = false,
      completedAt = 1000L
    )

  private val summaryJob =
    PostGameSummaryJob(
      summaryId = "summary-openai-1",
      playerId = "player-openai-1",
      summaryType = SummaryType.Match,
      gameId = summaryGame.gameId,
      plan = MonetisationPolicy.PlanTier.FreeAdSupported,
      completedGames = 12,
      remainingFreeTokens = 1,
      remainingPremiumDailyQuota = 0,
      cachedView = false,
      candidateGames = List(summaryGame),
      policyVersion = "summary-policy-v1",
      schemaVersion = "summary-schema-v1",
      createdAt = 123456789L
    )

  test("runtime config keeps OPENAI_API_KEY as the server-side default secret ref"):
    val config = runtimeConfig(RuntimeOverrides(model = Some("gpt-test"), endpoint = Some("https://api.openai.com/v1/responses")))

    assert(config.validServerSide)
    assertEquals(config.apiKeySecretRef, "OPENAI_API_KEY")
    assertEquals(config.model, "gpt-test")
    assert(!config.clientCanReadCredentials)

  test("live Responses API provider posts a secret-safe structured-output request"):
    val transport = CapturingTransport(jsonResponse(liveOutputJson))
    val provider =
      ResponsesApiLiveProvider(
        secretSource = StaticSecret(Some("sk-test-secret-value")),
        transport = transport,
        timeout = Duration.ofSeconds(5)
      )

    val response = provider.generateLive(liveRequest, prompt, OpenAiRuntimeConfig.defaultLocal, attempt = 0)
    val request = transport.lastRequest.getOrElse(fail("expected transport call"))

    assertEquals(transport.calls, 1)
    assert(request.safeForLogs)
    assert(!request.body.contains("sk-test-secret-value"))
    assert(request.body.contains("\"store\":false"))
    assert(request.body.contains("\"json_schema\""))
    assertEquals(response.providerRequestId, "resp_evenchess_1")
    assertEquals(response.output.message, "Improve your least active piece before forcing matters.")
    assertEquals(response.output.sourceFactIds, List(fact.factId))
    assertEquals(response.usage.inputTokens, 123)
    assertEquals(response.usage.outputTokens, 45)
    assert(!response.rawProviderTextStored)

  test("live provider parses nested Responses API message content"):
    val nestedResponse =
      JsonCodec.render(
        JsonCodec.obj(
          "id" -> JsonCodec.str("resp_nested_1"),
          "output" -> JsonCodec.arr(
            List(
              JsonCodec.obj(
                "type" -> JsonCodec.str("message"),
                "content" -> JsonCodec.arr(
                  List(
                    JsonCodec.obj(
                      "type" -> JsonCodec.str("output_text"),
                      "text" -> JsonCodec.str(JsonCodec.render(liveOutputJson))
                    )
                  )
                )
              )
            )
          ),
          "usage" -> JsonCodec.obj(
            "input_tokens" -> JsonCodec.num(10),
            "output_tokens" -> JsonCodec.num(5)
          )
        )
      )
    val provider = ResponsesApiLiveProvider(StaticSecret(Some("sk-test-secret-value")), CapturingTransport(nestedResponse))

    val response = provider.generateLive(liveRequest, prompt, OpenAiRuntimeConfig.defaultLocal, attempt = 0)

    assertEquals(response.providerRequestId, "resp_nested_1")
    assertEquals(response.output.boardStateKey, liveRequest.boardStateKey)
    assertEquals(response.usage.inputTokens, 10)

  test("missing OpenAI secret returns a suppressible response without calling transport"):
    val transport = CapturingTransport(jsonResponse(liveOutputJson))
    val provider = ResponsesApiLiveProvider(StaticSecret(None), transport)

    val response = provider.generateLive(liveRequest, prompt, OpenAiRuntimeConfig.defaultLocal, attempt = 0)

    assertEquals(transport.calls, 0)
    assertEquals(response.output.message, "")
    assert(response.providerRequestId.contains("MissingSecret"))
    assert(!response.rawProviderTextStored)

  test("summary Responses API provider parses post-game summary output"):
    val summaryOutput =
      JsonCodec.obj(
        "summaryId" -> JsonCodec.str(summaryJob.summaryId),
        "summaryType" -> JsonCodec.str(SummaryType.Match.toString),
        "title" -> JsonCodec.str("Development review"),
        "bullets" -> JsonCodec.arr(List(JsonCodec.str("You improved piece activity."), JsonCodec.str("Review when pressure changed."))),
        "sourceGameIds" -> JsonCodec.arr(List(JsonCodec.str(summaryGame.gameId))),
        "sourceFactIds" -> JsonCodec.arr(List(JsonCodec.str("summary-fact-1"))),
        "policyVersion" -> JsonCodec.str(summaryJob.policyVersion),
        "schemaVersion" -> JsonCodec.str(summaryJob.schemaVersion)
      )
    val transport = CapturingTransport(jsonResponse(summaryOutput))
    val provider = ResponsesApiSummaryProvider(StaticSecret(Some("sk-test-secret-value")), transport)

    val response = provider.generateSummary(summaryJob, OpenAiRuntimeConfig.defaultLocal, List(summaryGame))
    val request = transport.lastRequest.getOrElse(fail("expected transport call"))

    assert(request.safeForLogs)
    assert(!request.body.contains("sk-test-secret-value"))
    assertEquals(response.providerRequestId, "resp_evenchess_1")
    assertEquals(response.output.title, "Development review")
    assertEquals(response.output.sourceGameIds, List(summaryGame.gameId))
    assertEquals(response.usage.outputTokens, 45)
    assert(!response.rawProviderTextStored)

  test("invalid provider JSON is converted into validation-failing output, not an exception"):
    val provider = ResponsesApiLiveProvider(StaticSecret(Some("sk-test-secret-value")), CapturingTransport("""{"id":"resp_bad","output_text":"not-json"}"""))

    val response = provider.generateLive(liveRequest, prompt, OpenAiRuntimeConfig.defaultLocal, attempt = 0)

    assert(response.providerRequestId.contains("ResponseParseFailure"))
    assertEquals(response.output.message, "")
    assert(!response.rawProviderTextStored)
