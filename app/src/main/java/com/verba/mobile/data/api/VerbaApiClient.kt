package com.verba.mobile.data.api

import com.google.firebase.auth.FirebaseAuth
import com.verba.mobile.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

class VerbaApiClient(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    val http: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    /** Force-refresh-safe ID token retrieval. Throws if no signed-in user. */
    suspend fun bearerToken(forceRefresh: Boolean = false): String {
        val user = auth.currentUser ?: error("Not signed in")
        val result = user.getIdToken(forceRefresh).await()
        return result.token ?: error("ID token unavailable")
    }

    fun url(path: String): String {
        val sep = if (path.startsWith("/")) "" else "/"
        return "$baseUrl$sep$path"
    }
}

/**
 * Uniform result envelope for API calls.
 * `Error` includes parsed `{ error }` field from the server when available, otherwise raw body.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Error(val status: Int, val code: String?, val raw: String) : ApiResult<Nothing>
    data class Network(val cause: Throwable) : ApiResult<Nothing>
}

internal suspend inline fun <reified T> handleResponse(
    json: Json,
    response: HttpResponse,
): ApiResult<T> {
    val body = response.bodyAsText()
    android.util.Log.d(
        "VerbaHttp",
        "→ ${response.status.value} (${body.length}b): ${body.take(500)}",
    )
    return if (response.status.isSuccess()) {
        try {
            ApiResult.Success(json.decodeFromString<T>(body))
        } catch (t: Throwable) {
            android.util.Log.w("VerbaHttp", "parse failed", t)
            ApiResult.Error(response.status.value, "parse_error", "${t::class.simpleName}: ${t.message}\nbody=$body")
        }
    } else {
        val code = runCatching {
            json.parseToJsonElement(body)
                .let { (it as? kotlinx.serialization.json.JsonObject)?.get("error") }
                ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        }.getOrNull()
        ApiResult.Error(response.status.value, code, body)
    }
}

