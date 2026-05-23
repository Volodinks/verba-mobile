package com.verba.mobile.ui.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verba.mobile.VerbaApp
import com.verba.mobile.data.AllowedCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface AppState {
    data object Initializing : AppState
    data object NeedsLogin : AppState
    data object CheckingAccess : AppState
    data class AccessDenied(val email: String) : AppState
    data class AccessError(val email: String) : AppState
    data class Authorized(val uid: String, val email: String) : AppState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VerbaApp
    private val authRepository = app.authRepository
    private val allowedUsersRepository = app.allowedUsersRepository

    private val _state = MutableStateFlow<AppState>(AppState.Initializing)
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collectLatest { user ->
                if (user == null) {
                    _state.value = AppState.NeedsLogin
                    return@collectLatest
                }
                val email = user.email
                if (email.isNullOrBlank()) {
                    _state.value = AppState.AccessDenied(email = "")
                    return@collectLatest
                }
                _state.value = AppState.CheckingAccess
                when (val result = allowedUsersRepository.check(email)) {
                    AllowedCheckResult.Allowed -> _state.value = AppState.Authorized(user.uid, email)
                    AllowedCheckResult.Denied -> _state.value = AppState.AccessDenied(email)
                    is AllowedCheckResult.Error -> _state.value = AppState.AccessError(email)
                }
            }
        }
    }

    fun retryAccessCheck() {
        val current = authRepository.currentUser ?: return
        val email = current.email ?: return
        viewModelScope.launch {
            _state.value = AppState.CheckingAccess
            when (allowedUsersRepository.check(email)) {
                AllowedCheckResult.Allowed -> _state.value = AppState.Authorized(current.uid, email)
                AllowedCheckResult.Denied -> _state.value = AppState.AccessDenied(email)
                is AllowedCheckResult.Error -> _state.value = AppState.AccessError(email)
            }
        }
    }

    fun signOut(context: android.content.Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
        }
    }
}
