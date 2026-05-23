package com.verba.mobile.data.api

import com.verba.mobile.data.model.Lesson
import com.verba.mobile.data.model.UniversalSettings
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class LessonAncestorEntry(val id: String, val name: String)

@Serializable
data class LessonDetailResponse(
    val lesson: Lesson,
    /**
     * Server returns this as a Partial<UniversalSettings> via mergeInheritedPartial — required fields
     * may still be null at this stage; the run-create endpoint is what assertCompleteSettings.
     */
    val effective_settings: UniversalSettings? = null,
    val ancestor_chain: List<LessonAncestorEntry> = emptyList(),
)

class LessonsApi(private val client: VerbaApiClient) {

    suspend fun getLesson(id: String): ApiResult<LessonDetailResponse> = try {
        val token = client.bearerToken()
        val response = client.http.get(client.url("/api/lessons/$id")) {
            bearerAuth(token)
        }
        handleResponse<LessonDetailResponse>(client.json, response)
    } catch (t: Throwable) {
        ApiResult.Network(t)
    }
}
