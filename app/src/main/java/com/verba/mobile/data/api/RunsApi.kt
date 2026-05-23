package com.verba.mobile.data.api

import com.verba.mobile.data.model.EffectiveSettings
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.LessonRun
import com.verba.mobile.data.model.MultipleChoiceTask
import com.verba.mobile.data.model.MultipleChoiceUserAnswer
import com.verba.mobile.data.model.Statistics
import com.verba.mobile.data.model.UniversalSettings
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

@Serializable
data class CreateRunRequest(
    val lesson_id: String,
    val player_override: UniversalSettings? = null,
    val feedback_mode: FeedbackMode,
    val task_count: Int,
)

@Serializable
data class CreateRunResponse(
    val id: String,
    val effective_settings: EffectiveSettings,
    val task_count: Int,
)

@Serializable
data class NextTaskResponse(val task: MultipleChoiceTask)

@Serializable
data class AnswerRequest(
    val index: Int,
    val user_answer: MultipleChoiceUserAnswer,
)

/** Server returns different shapes for immediate vs end feedback — we tolerate both with optional fields. */
@Serializable
data class AnswerResponse(
    val is_correct: Boolean? = null,
    val correct_index: Int? = null,
    val explanation: String? = null,
    val ok: Boolean? = null,
)

@Serializable
data class CompleteRunResponse(
    val status: String,
    val statistics: Statistics,
    val tasks: List<MultipleChoiceTask> = emptyList(),
)

@Serializable
data class GetRunResponse(val run: LessonRun)

class RunsApi(private val client: VerbaApiClient) {

    suspend fun create(request: CreateRunRequest): ApiResult<CreateRunResponse> = call {
        client.http.post(client.url("/api/runs")) {
            bearerAuth(it)
            setBody(request)
        }
    }

    suspend fun nextTask(runId: String, index: Int? = null): ApiResult<NextTaskResponse> = call {
        val suffix = index?.let { i -> "?index=$i" } ?: ""
        client.http.post(client.url("/api/runs/$runId/next-task$suffix")) {
            bearerAuth(it)
        }
    }

    suspend fun answer(runId: String, request: AnswerRequest): ApiResult<AnswerResponse> = call {
        client.http.post(client.url("/api/runs/$runId/answer")) {
            bearerAuth(it)
            setBody(request)
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
