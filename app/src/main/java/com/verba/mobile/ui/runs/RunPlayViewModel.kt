package com.verba.mobile.ui.runs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.verba.mobile.VerbaApp
import com.verba.mobile.data.api.AnswerRequest
import com.verba.mobile.data.api.AnswerResponse
import com.verba.mobile.data.api.ApiResult
import com.verba.mobile.data.api.RunsApi
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.MultipleChoiceTask
import com.verba.mobile.data.model.MultipleChoiceUserAnswer
import com.verba.mobile.ui.errors.UiError
import com.verba.mobile.ui.errors.toUiError
import com.verba.mobile.ui.navigation.Routes
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RunPlayUiState(
    val taskCount: Int = 0,
    val feedbackMode: FeedbackMode = FeedbackMode.IMMEDIATE,
    val currentIndex: Int = 0,
    val currentTask: MultipleChoiceTask? = null,
    val isLoadingTask: Boolean = true,
    val selectedOption: Int? = null,
    val submitting: Boolean = false,
    val feedback: AnswerResponse? = null,
    val finished: Boolean = false,
    val error: UiError? = null,
)

class RunPlayViewModel(
    application: Application,
    private val runId: String,
) : AndroidViewModel(application) {

    private val runsApi: RunsApi = (application as VerbaApp).runsApi

    private val _state = MutableStateFlow(RunPlayUiState())
    val state: StateFlow<RunPlayUiState> = _state.asStateFlow()

    /** Pending prefetch for the NEXT task after the current one. */
    private var prefetched: Deferred<MultipleChoiceTask?>? = null

    init { bootstrap() }

    private fun bootstrap() {
        viewModelScope.launch {
            when (val r = runsApi.get(runId)) {
                is ApiResult.Success -> {
                    val run = r.value.run
                    _state.update { it.copy(taskCount = run.task_count, feedbackMode = run.feedback_mode) }
                    loadCurrent()
                }
                is ApiResult.Error -> setError(r.toUiError())
                is ApiResult.Network -> setError(r.toUiError())
            }
        }
    }

    private fun loadCurrent() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingTask = true, selectedOption = null, feedback = null) }
            val cached = prefetched?.let { runCatching { it.await() }.getOrNull() }
            prefetched = null
            val task = cached ?: fetchTask(_state.value.currentIndex)
            if (task == null) return@launch // setError already called by fetchTask path
            _state.update { it.copy(currentTask = task, isLoadingTask = false) }
            launchPrefetch()
        }
    }

    private fun launchPrefetch() {
        val nextIdx = _state.value.currentIndex + 1
        if (nextIdx >= _state.value.taskCount) return
        prefetched = viewModelScope.async { fetchTask(nextIdx) }
    }

    private suspend fun fetchTask(index: Int): MultipleChoiceTask? =
        when (val r = runsApi.nextTask(runId, index)) {
            is ApiResult.Success -> r.value.task
            is ApiResult.Error -> { setError(r.toUiError()); null }
            is ApiResult.Network -> { setError(r.toUiError()); null }
        }

    fun selectOption(optionIndex: Int) {
        if (_state.value.feedback != null) return
        _state.update { it.copy(selectedOption = optionIndex) }
    }

    fun submitAnswer() {
        val sel = _state.value.selectedOption ?: return
        if (_state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            val req = AnswerRequest(
                index = _state.value.currentIndex,
                user_answer = MultipleChoiceUserAnswer(selected_index = sel),
            )
            when (val r = runsApi.answer(runId, req)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, feedback = r.value) }
                is ApiResult.Error -> setError(r.toUiError())
                is ApiResult.Network -> setError(r.toUiError())
            }
        }
    }

    fun next() {
        val nextIdx = _state.value.currentIndex + 1
        if (nextIdx >= _state.value.taskCount) {
            _state.update { it.copy(finished = true) }
            return
        }
        _state.update { it.copy(currentIndex = nextIdx) }
        loadCurrent()
    }

    private fun setError(error: UiError) {
        _state.update { it.copy(isLoadingTask = false, submitting = false, error = error) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = createSavedStateHandle()
                val id = handle.get<String>(Routes.ARG_RUN_ID) ?: error("Missing runId nav arg")
                RunPlayViewModel(app, id)
            }
        }
    }
}
