package com.verba.mobile.ui.runs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.verba.mobile.VerbaApp
import com.verba.mobile.data.api.ApiResult
import com.verba.mobile.data.api.CompleteRunResponse
import com.verba.mobile.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RunResultUiState {
    data object Loading : RunResultUiState
    data class Loaded(val data: CompleteRunResponse) : RunResultUiState
    data class Error(val message: String) : RunResultUiState
}

class RunResultViewModel(
    application: Application,
    private val runId: String,
) : AndroidViewModel(application) {

    private val runsApi = (application as VerbaApp).runsApi

    private val _state = MutableStateFlow<RunResultUiState>(RunResultUiState.Loading)
    val state: StateFlow<RunResultUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = RunResultUiState.Loading
            _state.value = when (val r = runsApi.complete(runId)) {
                is ApiResult.Success -> RunResultUiState.Loaded(r.value)
                is ApiResult.Error -> RunResultUiState.Error(
                    "HTTP ${r.status}${r.code?.let { c -> " · $c" } ?: ""}"
                )
                is ApiResult.Network -> RunResultUiState.Error(
                    "Мережа: ${r.cause.message ?: r.cause::class.simpleName}"
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = createSavedStateHandle()
                val id = handle.get<String>(Routes.ARG_RUN_ID)
                    ?: error("Missing runId nav arg")
                RunResultViewModel(app, id)
            }
        }
    }
}
