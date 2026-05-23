package com.verba.mobile.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.verba.mobile.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed interface SignInResult {
    data class Success(val user: FirebaseUser) : SignInResult
    data object Cancelled : SignInResult
    data object NoAccountAvailable : SignInResult
    data class Failure(val cause: Throwable) : SignInResult
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(context: Context): SignInResult {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val idToken: String = try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return SignInResult.Failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GetCredentialCancellationException) {
            return SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            return SignInResult.NoAccountAvailable
        } catch (e: GoogleIdTokenParsingException) {
            return SignInResult.Failure(e)
        } catch (e: GetCredentialException) {
            return SignInResult.Failure(e)
        }

        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val user = auth.signInWithCredential(firebaseCredential).await().user
                ?: return SignInResult.Failure(IllegalStateException("Firebase returned null user"))
            SignInResult.Success(user)
        } catch (e: Exception) {
            SignInResult.Failure(e)
        }
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
