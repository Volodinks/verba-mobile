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
import com.verba.mobile.data.api.NextTaskOutcome
import com.verba.mobile.data.api.RunsApi
import com.verba.mobile.data.api.UserAnswerWire
import com.verba.mobile.data.api.VerbaApiClient
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.InlineDropdownsTask
import com.verba.mobile.data.model.JudgeVerdict
import com.verba.mobile.data.model.LessonTypeId
import com.verba.mobile.data.model.MultipleChoiceTask
import com.verba.mobile.data.model.PendingAnswer
import com.verba.mobile.data.model.Task
import com.verba.mobile.data.model.TextInputTask
import com.verba.mobile.ui.errors.UiError
import com.verba.mobile.ui.errors.toUiError
import com.verba.mobile.ui.navigation.Routes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the run-play screen. Polymorphic task carried as [Task] (sealed); the screen's
 * `when (task)` selects the right renderer per type.
 *
 * [taskCount] may be null — that means the run is open-ended (the learner ends it manually).
 * In that mode the progress header shows "Tasks answered: N" and a "Finish now" action is offered
 * once at least one task has been answered.
 *
 * [stillGeneratingHint] turns true after the first 3 seconds of sustained 202-polling.
 */
data class RunPlayUiState(
    val taskCount: Int? = 0,
    val feedbackMode: FeedbackMode = FeedbackMode.IMMEDIATE,
    val lessonType: LessonTypeId = LessonTypeId.MULTIPLE_CHOICE,
    val currentIndex: Int = 0,
    val currentTask: Task? = null,
    val isLoadingTask: Boolean = true,
    val stillGeneratingHint: Boolean = false,
    val pending: PendingAnswer? = null,
    val submitting: Boolean = false,
    val answered: Boolean = false,
    val isCorrect: Boolean? = null,
    val explanation: String? = null,
    val judging: Boolean = false,
    val judgeVerdict: JudgeVerdict? = null,
    val finished: Boolean = false,
    val error: UiError? = null,
) {
    val isOpenEnded: Boolean get() = taskCount == null
    val isLastTask: Boolean get() = taskCount?.let { currentIndex + 1 >= it } ?: false
    val canFinishOpenEnded: Boolean get() = isOpenEnded && currentIndex >= 1 && !submitting
}

class RunPlayViewModel(
    application: Application,
    private val runId: String,
) : AndroidViewModel(application) {

    private val app = application as VerbaApp
    private val runsApi: RunsApi = app.runsApi
    private val apiClient: VerbaApiClient = app.apiClient

    private val _state = MutableStateFlow(RunPlayUiState())
    val state: StateFlow<RunPlayUiState> = _state.asStateFlow()

    /** The currently active task-loading coroutine. Cancelled when the user navigates away or moves on. */
    private var loadJob: Job? = null

    init { bootstrap() }

    private fun bootstrap() {
        viewModelScope.launch {
            when (val r = runsApi.get(runId)) {
                is ApiResult.Success -> {
                    val run = r.value.run
                    _state.update {
                        it.copy(
                            taskCount = run.task_count,
                            feedbackMode = run.feedback_mode,
                            lessonType = run.lesson_type,
                        )
                    }
                    loadCurrent()
                }
                is ApiResult.Error -> setError(r.toUiError())
                is ApiResult.Network -> setError(r.toUiError())
            }
        }
    }

    private fun loadCurrent() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoadingTask = true,
                    stillGeneratingHint = false,
                    pending = null,
                    answered = false,
                    isCorrect = null,
                    explanation = null,
                    judging = false,
                    judgeVerdict = null,
                )
            }
            val task = fetchTaskWithPolling(_state.value.currentIndex) ?: return@launch
            _state.update {
                it.copy(
                    currentTask = task,
                    isLoadingTask = false,
                    stillGeneratingHint = false,
                    pending = task.initialPendingAnswer(),
                    answered = task.hasUserAnswer,
                    isCorrect = task.isCorrect,
                )
            }
        }
    }

    /**
     * Fetch [index]; if the server returns `Generating`, poll with backoff (base 700ms, ceiling 30s)
     * and surface a "still generating" hint after 3 s. Returns null if an error was set.
     */
    private suspend fun fetchTaskWithPolling(index: Int): Task? {
        val deadline = System.currentTimeMillis() + 30_000L
        val hintDeadline = System.currentTimeMillis() + 3_000L
        while (true) {
            when (val outcome = runsApi.nextTask(runId, _state.value.lessonType, index)) {
                is NextTaskOutcome.Ready -> return outcome.task
                is NextTaskOutcome.Failed -> { setError(outcome.error.toUiError()); return null }
                is NextTaskOutcome.NetworkFailed -> { setError(outcome.cause.toUiError()); return null }
                NextTaskOutcome.Generating -> {
                    if (System.currentTimeMillis() > hintDeadline && !_state.value.stillGeneratingHint) {
                        _state.update { it.copy(stillGeneratingHint = true) }
                    }
                    if (System.currentTimeMillis() > deadline) {
                        setError(UiError.Api(status = 504, code = "timeout", fieldsArg = null, raw = ""))
                        return null
                    }
                    delay(700)
                }
            }
        }
    }

    private fun Task.initialPendingAnswer(): PendingAnswer = when (this) {
        is MultipleChoiceTask -> PendingAnswer.MultipleChoice(user_answer?.selected_index)
        is InlineDropdownsTask -> PendingAnswer.InlineDropdowns(
            user_answer?.selections?.map { it as Int? } ?: List(gaps.size) { null },
        )
        is TextInputTask -> PendingAnswer.TextInput(
            user_answer?.inputs ?: List(gaps.size) { "" },
        )
    }

    fun selectMultipleChoice(index: Int) {
        if (_state.value.answered) return
        _state.update { it.copy(pending = PendingAnswer.MultipleChoice(index)) }
    }

    fun setInlineDropdownSelection(gapIndex: Int, optionIndex: Int) {
        if (_state.value.answered) return
        val current = (_state.value.pending as? PendingAnswer.InlineDropdowns) ?: return
        val next = current.selections.toMutableList().also { it[gapIndex] = optionIndex }
        _state.update { it.copy(pending = PendingAnswer.InlineDropdowns(next)) }
    }

    fun setTextInputValue(gapIndex: Int, value: String) {
        if (_state.value.answered) return
        val current = (_state.value.pending as? PendingAnswer.TextInput) ?: return
        val next = current.inputs.toMutableList().also { it[gapIndex] = value }
        _state.update { it.copy(pending = PendingAnswer.TextInput(next)) }
    }

    fun submitAnswer() {
        val s = _state.value
        val pending = s.pending ?: return
        if (!pending.isComplete || s.submitting || s.answered) return
        val wire = when (pending) {
            is PendingAnswer.MultipleChoice -> UserAnswerWire.multipleChoice(pending.selectedIndex!!)
            is PendingAnswer.InlineDropdowns -> UserAnswerWire.inlineDropdowns(pending.selections.map { it!! })
            is PendingAnswer.TextInput -> UserAnswerWire.textInput(pending.inputs)
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            when (val r = runsApi.answer(runId, s.currentIndex, wire)) {
                is ApiResult.Success -> {
                    val resp = r.value
                    val task = resp.task?.let { runCatching { Task.parse(s.lessonType, apiClient.json, it) }.getOrNull() }
                        ?: s.currentTask
                    _state.update {
                        it.copy(
                            submitting = false,
                            answered = true,
                            isCorrect = resp.is_correct ?: task?.isCorrect,
                            explanation = task.explanationOrNull(),
                            currentTask = task,
                            judgeVerdict = (task as? TextInputTask)?.judge_verdict,
                        )
                    }
                }
                is ApiResult.Error -> _state.update { it.copy(submitting = false, error = r.toUiError()) }
                is ApiResult.Network -> _state.update { it.copy(submitting = false, error = r.toUiError()) }
            }
        }
    }

    fun appealAnswer() {
        val s = _state.value
        val task = s.currentTask as? TextInputTask ?: return
        if (s.judging || task.judge_verdict != null || s.isCorrect == true) return
        if (s.feedbackMode != FeedbackMode.IMMEDIATE) return
        viewModelScope.launch {
            _state.update { it.copy(judging = true, error = null) }
            when (val r = runsApi.judge(runId, s.currentIndex)) {
                is ApiResult.Success -> {
                    val verdict = JudgeVerdict(
                        is_actually_correct = r.value.is_actually_correct,
                        reason = r.value.reason,
                        judged_at = "",
                    )
                    val updatedTask = r.value.task
                        ?.let { runCatching { Task.parse(s.lessonType, apiClient.json, it) }.getOrNull() }
                        ?: s.currentTask
                    _state.update {
                        it.copy(
                            judging = false,
                            judgeVerdict = verdict,
                            isCorrect = if (verdict.is_actually_correct) true else it.isCorrect,
                            currentTask = updatedTask,
                        )
                    }
                }
                is ApiResult.Error -> _state.update { it.copy(judging = false, error = r.toUiError()) }
                is ApiResult.Network -> _state.update { it.copy(judging = false, error = r.toUiError()) }
            }
        }
    }

    fun next() {
        val s = _state.value
        val taskCount = s.taskCount
        val nextIdx = s.currentIndex + 1
        if (taskCount != null && nextIdx >= taskCount) {
            _state.update { it.copy(finished = true) }
            return
        }
        _state.update { it.copy(currentIndex = nextIdx, currentTask = null) }
        loadCurrent()
    }

    /** Open-ended runs only: finalize on demand. */
    fun finishOpenEnded() {
        if (!_state.value.canFinishOpenEnded) return
        loadJob?.cancel()
        _state.update { it.copy(finished = true) }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }

    private fun setError(error: UiError) {
        _state.update {
            it.copy(isLoadingTask = false, submitting = false, judging = false, error = error)
        }
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

private fun Task?.explanationOrNull(): String? = when (this) {
    is MultipleChoiceTask -> explanation
    is InlineDropdownsTask -> gaps.firstOrNull { !it.explanation.isNullOrBlank() }?.explanation
    is TextInputTask -> gaps.firstOrNull { !it.explanation.isNullOrBlank() }?.explanation
    null -> null
}

private fun Throwable.toUiError(): UiError = UiError.Throwable(this)
