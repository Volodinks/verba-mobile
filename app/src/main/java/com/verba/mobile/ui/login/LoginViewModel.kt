package com.verba.mobile.ui.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verba.mobile.VerbaApp
import com.verba.mobile.auth.SignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Cancelled : LoginUiState
    data object NoGoogleAccount : LoginUiState
    data object NetworkError : LoginUiState
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = (application as VerbaApp).authRepository

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun signIn(activityContext: Context) {
        if (_state.value is LoginUiState.Loading) return
        viewModelScope.launch {
            _state.value = LoginUiState.Loading
            _state.value = when (val result = authRepository.signInWithGoogle(activityContext)) {
                is SignInResult.Success -> LoginUiState.Idle  // AppViewModel reacts to auth state change.
                SignInResult.Cancelled -> LoginUiState.Cancelled
                SignInResult.NoAccountAvailable -> LoginUiState.NoGoogleAccount
                is SignInResult.Failure -> LoginUiState.NetworkError
            }
        }
    }

    fun dismissError() {
        _state.value = LoginUiState.Idle
    }
}
