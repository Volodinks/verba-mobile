package com.verba.mobile.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class LessonsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun getMyLessons(uid: String): Result<List<Lesson>> = runCatching {
        val snapshot = firestore.collection("lessons")
            .whereEqualTo("ownerUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        snapshot.documents.map { doc ->
            Lesson(
                id = doc.id,
                ownerUid = doc.getString("ownerUid").orEmpty(),
                body = doc.getString("body").orEmpty(),
                createdBy = doc.getString("createdBy"),
                createdAt = doc.getString("createdAt"),
            )
        }
    }

    suspend fun getLessonById(id: String): Result<Lesson?> = runCatching {
        val doc = firestore.collection("lessons").document(id).get().await()
        if (!doc.exists()) return@runCatching null
        Lesson(
            id = doc.id,
            ownerUid = doc.getString("ownerUid").orEmpty(),
            body = doc.getString("body").orEmpty(),
            createdBy = doc.getString("createdBy"),
            createdAt = doc.getString("createdAt"),
        )
    }
}
