package com.verba.mobile.data.api

import com.verba.mobile.data.model.EffectiveSettings
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.LessonRun
import com.verba.mobile.data.model.LessonTypeId
import com.verba.mobile.data.model.Statistics
import com.verba.mobile.data.model.Task
import com.verba.mobile.data.model.UniversalSettings
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class CreateRunRequest(
    val lesson_id: String,
    val player_override: UniversalSettings? = null,
    val feedback_mode: FeedbackMode,
    /** Nullable: null means "open-ended; the learner ends the run manually". */
    val task_count: Int? = null,
)

@Serializable
data class CreateRunResponse(
    val id: String,
    val effective_settings: EffectiveSettings,
    val task_count: Int? = null,
)

@Serializable
data class GeneratingResponse(val status: String)

/** Wire response of POST /api/runs/:id/answer. Server returns the full task for either mode. */
@Serializable
data class AnswerResponse(
    val is_correct: Boolean? = null,
    val task: JsonObject? = null,
    val ok: Boolean? = null,
)

@Serializable
data class JudgeResponse(
    val is_actually_correct: Boolean,
    val reason: String,
    val task: JsonObject? = null,
)

@Serializable
data class CompleteRunResponse(
    val status: String,
    val statistics: Statistics,
    val tasks: List<JsonObject> = emptyList(),
)

@Serializable
data class GetRunResponse(val run: LessonRun)

/**
 * Outcome of POST /api/runs/:id/next-task. Server may return a generated task, a `202 generating`
 * signal (another in-flight request is calling OpenAI), or a real error.
 */
sealed interface NextTaskOutcome {
    data class Ready(val task: Task) : NextTaskOutcome
    data object Generating : NextTaskOutcome
    data class Failed(val error: ApiResult.Error) : NextTaskOutcome
    data class NetworkFailed(val cause: Throwable) : NextTaskOutcome
}

class RunsApi(private val client: VerbaApiClient) {

    suspend fun create(request: CreateRunRequest): ApiResult<CreateRunResponse> = call {
        client.http.post(client.url("/api/runs")) {
            bearerAuth(it)
            setBody(request)
        }
    }

    /**
     * Fetch or generate the task at [index] (or the next slot if [index] is null).
     * Maps HTTP 202 → [NextTaskOutcome.Generating] so the view-model can poll without treating it
     * as a generic error.
     */
    suspend fun nextTask(
        runId: String,
        lessonType: LessonTypeId,
        index: Int? = null,
    ): NextTaskOutcome = try {
        val token = client.bearerToken()
        val suffix = index?.let { i -> "?index=$i" } ?: ""
        val response = client.http.post(client.url("/api/runs/$runId/next-task$suffix")) {
            bearerAuth(token)
        }
        if (response.status == HttpStatusCode.Accepted) {
            NextTaskOutcome.Generating
        } else {
            when (val parsed = handleResponse<NextTaskRawResponse>(client.json, response)) {
                is ApiResult.Success -> {
                    val taskJson = parsed.value.task
                    if (taskJson != null) {
                        runCatching { Task.parse(lessonType, client.json, taskJson) }
                            .fold(
                                onSuccess = { NextTaskOutcome.Ready(it) },
                                onFailure = {
                                    NextTaskOutcome.Failed(
                                        ApiResult.Error(
                                            response.status.value,
                                            "parse_error",
                                            "${it::class.simpleName}: ${it.message}",
                                        ),
                                    )
                                },
                            )
                    } else if (parsed.value.status == "generating") {
                        NextTaskOutcome.Generating
                    } else {
                        NextTaskOutcome.Failed(
                            ApiResult.Error(response.status.value, "parse_error", "no task in response"),
                        )
                    }
                }
                is ApiResult.Error -> NextTaskOutcome.Failed(parsed)
                is ApiResult.Network -> NextTaskOutcome.NetworkFailed(parsed.cause)
            }
        }
    } catch (t: Throwable) {
        NextTaskOutcome.NetworkFailed(t)
    }

    suspend fun answer(runId: String, index: Int, userAnswer: JsonObject): ApiResult<AnswerResponse> = call {
        client.http.post(client.url("/api/runs/$runId/answer")) {
            bearerAuth(it)
            setBody(AnswerWireRequest(index = index, user_answer = userAnswer))
        }
    }

    suspend fun judge(runId: String, index: Int): ApiResult<JudgeResponse> = call {
        client.http.post(client.url("/api/runs/$runId/judge")) {
            bearerAuth(it)
            setBody(JudgeWireRequest(index = index))
        }
    }

    suspend fun complete(runId: String): ApiResult<CompleteRunResponse> = call {
        client.http.post(client.url("/api/runs/$runId/complete")) {
            bearerAuth(it)
        }
    }

    suspend fun get(runId: String): ApiResult<GetRunResponse> = call {
        client.http.get(client.url("/api/runs/$runId")) {
            bearerAuth(it)
        }
    }

    private suspend inline fun <reified T> call(
        crossinline block: suspend (token: String) -> io.ktor.client.statement.HttpResponse,
    ): ApiResult<T> = try {
        val token = client.bearerToken()
        val response = block(token)
        handleResponse<T>(client.json, response)
    } catch (t: Throwable) {
        ApiResult.Network(t)
    }
}

/** Internal wire shape — the server may return either a task or a status string. */
@Serializable
private data class NextTaskRawResponse(
    val task: JsonObject? = null,
    val status: String? = null,
)

@Serializable
private data class AnswerWireRequest(
    val index: Int,
    val user_answer: JsonObject,
)

@Serializable
private data class JudgeWireRequest(val index: Int)

/** Helpers to build the wire `user_answer` JSON object for each task type. */
object UserAnswerWire {
    fun multipleChoice(selectedIndex: Int): JsonObject = JsonObject(
        mapOf("selected_index" to JsonPrimitive(selectedIndex)),
    )

    fun inlineDropdowns(selections: List<Int>): JsonObject = JsonObject(
        mapOf("selections" to kotlinx.serialization.json.JsonArray(selections.map { JsonPrimitive(it) })),
    )

    fun textInput(inputs: List<String>): JsonObject = JsonObject(
        mapOf("inputs" to kotlinx.serialization.json.JsonArray(inputs.map { JsonPrimitive(it) })),
    )
}
