package com.verba.mobile.data

import com.google.firebase.firestore.FirebaseFirestore
import com.verba.mobile.data.model.LessonTypeId
import kotlinx.coroutines.tasks.await

/**
 * Read-only access to the `folders` and `lessons` collections — used to build the navigation tree.
 * For full lesson detail (with effective_settings + ancestor_chain) the client hits GET /api/lessons/{id}.
 */
class FoldersRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    data class FolderNode(
        val id: String,
        val parent_id: String?,
        val name: String,
        val position: Double,
    )

    data class LessonStub(
        val id: String,
        val folder_id: String,
        val title: String,
        val description: String?,
        val type: LessonTypeId?,
    )

    suspend fun listFolders(): Result<List<FolderNode>> = runCatching {
        val snap = firestore.collection("folders").get().await()
        snap.documents.map { doc ->
            FolderNode(
                id = doc.id,
                parent_id = doc.getString("parent_id"),
                name = doc.getString("name") ?: "(unnamed)",
                position = (doc.get("position") as? Number)?.toDouble() ?: 0.0,
            )
        }.sortedWith(compareBy({ it.parent_id ?: "" }, { it.position }, { it.name }))
    }

    suspend fun listLessons(): Result<List<LessonStub>> = runCatching {
        val snap = firestore.collection("lessons").get().await()
        snap.documents.map { doc ->
            LessonStub(
                id = doc.id,
                folder_id = doc.getString("folder_id") ?: "",
                title = doc.getString("title") ?: "(untitled)",
                description = doc.getString("description"),
                type = when (doc.getString("type")) {
                    "multiple_choice" -> LessonTypeId.MULTIPLE_CHOICE
                    else -> null
                },
            )
        }.sortedBy { it.title.lowercase() }
    }
}
