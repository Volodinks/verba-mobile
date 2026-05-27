package com.verba.mobile.data.api

import com.verba.mobile.data.model.Folder
import com.verba.mobile.data.model.Lesson
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class ListFoldersResponse(val folders: List<Folder> = emptyList())

@Serializable
data class ListLessonsResponse(val lessons: List<Lesson> = emptyList())

/**
 * Catalog endpoints from verba-web. Replaces the previous direct-Firestore reads — the API
 * applies the same access-control + visibility logic as the web client and computes recursive
 * lesson counts server-side.
 */
class CatalogApi(private val client: VerbaApiClient) {

    suspend fun listFolders(): ApiResult<ListFoldersResponse> = try {
        val token = client.bearerToken()
        val response = client.http.get(client.url("/api/folders")) {
            bearerAuth(token)
        }
        handleResponse<ListFoldersResponse>(client.json, response)
    } catch (t: Throwable) {
        ApiResult.Network(t)
    }

    suspend fun listLessons(folderId: String): ApiResult<ListLessonsResponse> = try {
        val token = client.bearerToken()
        val encoded = java.net.URLEncoder.encode(folderId, "UTF-8")
        val response = client.http.get(client.url("/api/lessons?folder_id=$encoded")) {
            bearerAuth(token)
        }
        handleResponse<ListLessonsResponse>(client.json, response)
    } catch (t: Throwable) {
        ApiResult.Network(t)
    }
}
