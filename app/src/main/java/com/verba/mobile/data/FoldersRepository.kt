package com.verba.mobile.data

import com.verba.mobile.data.api.ApiResult
import com.verba.mobile.data.api.CatalogApi
import com.verba.mobile.data.model.Folder
import com.verba.mobile.data.model.Lesson

/**
 * Thin facade over [CatalogApi] for catalog browsing. Returns the full [Folder] / [Lesson]
 * models from the server so callers can use [Folder.path] for breadcrumbs and the server's
 * recursive [Folder.lesson_count].
 *
 * No direct Firestore reads — the catalog goes through the verba-web HTTP API so the same
 * access-control and visibility logic applies as on the web.
 */
class FoldersRepository(private val catalogApi: CatalogApi) {

    suspend fun listFolders(): Result<List<Folder>> = when (val r = catalogApi.listFolders()) {
        is ApiResult.Success -> Result.success(r.value.folders)
        is ApiResult.Error -> Result.failure(
            IllegalStateException("folders: ${r.status} ${r.code ?: "no_code"}"),
        )
        is ApiResult.Network -> Result.failure(r.cause)
    }

    suspend fun listLessons(folderId: String): Result<List<Lesson>> =
        when (val r = catalogApi.listLessons(folderId)) {
            is ApiResult.Success -> Result.success(r.value.lessons)
            is ApiResult.Error -> Result.failure(
                IllegalStateException("lessons: ${r.status} ${r.code ?: "no_code"}"),
            )
            is ApiResult.Network -> Result.failure(r.cause)
        }
}
