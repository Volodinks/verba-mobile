package com.verba.mobile.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.util.Locale

sealed interface AllowedCheckResult {
    data object Allowed : AllowedCheckResult
    data object Denied : AllowedCheckResult
    data class Error(val cause: Throwable) : AllowedCheckResult
}

class AllowedUsersRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun check(email: String): AllowedCheckResult {
        val key = email.lowercase(Locale.ROOT)
        return try {
            val doc = firestore.collection("allowedUsers").document(key).get().await()
            if (doc.exists()) AllowedCheckResult.Allowed else AllowedCheckResult.Denied
        } catch (e: FirebaseFirestoreException) {
            // Rules-denied reads are indistinguishable from "not allowed" for UX purposes.
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                AllowedCheckResult.Denied
            } else {
                AllowedCheckResult.Error(e)
            }
        } catch (e: Exception) {
            AllowedCheckResult.Error(e)
        }
    }
}
