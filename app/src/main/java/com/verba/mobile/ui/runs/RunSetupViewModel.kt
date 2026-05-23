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
import com.verba.mobile.data.api.CreateRunRequest
import com.verba.mobile.data.model.DistractorType
import com.verba.mobile.data.model.EnglishVariant
import com.verba.mobile.data.model.ExplanationLanguage
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.Level
import com.verba.mobile.data.model.Register
import com.verba.mobile.data.model.UniversalSettings
import com.verba.mobile.ui.errors.UiError
import com.verba.mobile.ui.errors.toUiError
import com.verba.mobile.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RunSetupUiState(
    val loadingLesson: Boolean = true,
    val loadError: UiError? = null,
    val taskCount: Int = 5,
    val feedbackMode: FeedbackMode = FeedbackMode.IMMEDIATE,
    val level: Level? = null,
    val englishVariant: EnglishVariant? = null,
    val register: Register? = null,
    val explanationLanguage: ExplanationLanguage? = null,
    val distractorTypes: Set<DistractorType> = emptySet(),
    val creating: Boolean = false,
    val createError: UiError? = null,
    val createdRunId: String? = null,
) {
    val canStart: Boolean
        get() = !creating && level != null && englishVariant != null && register != null
            && explanationLanguage != null && distractorTypes.isNotEmpty()
}

class RunSetupViewModel(
    application: Application,
    private val lessonId: String,
) : AndroidViewModel(application) {

    private val app = application as VerbaApp
    private val runsApi = app.runsApi
    private val lessonsApi = app.lessonsApi

    private val _state = MutableStateFlow(RunSetupUiState())
    val state: StateFlow<RunSetupUiState> = _state.asStateFlow()

    init { loadLesson() }

    fun loadLesson() {
        viewModelScope.launch {
            _state.update { it.copy(loadingLesson = true, loadError = null) }
            when (val r = lessonsApi.getLesson(lessonId)) {
                is ApiResult.Success -> {
                    val es = r.value.effective_settings
                    _state.update {
                        it.copy(
                            loadingLesson = false,
                            level = es?.level,
                            englishVariant = es?.english_variant ?: EnglishVariant.AMERICAN,
                            register = es?.register ?: Register.NEUTRAL,
                            explanationLanguage = es?.explanation_language,
                            distractorTypes = es?.distractor_types?.toSet() ?: emptySet(),
                        )
                    }
                }
                is ApiResult.Error -> _state.update { it.copy(loadingLesson = false, loadError = r.toUiError()) }
                is ApiResult.Network -> _state.update { it.copy(loadingLesson = false, loadError = r.toUiError()) }
            }
        }
    }

    fun setTaskCount(value: Int) { _state.update { it.copy(taskCount = value.coerceIn(1, 100)) } }
    fun setFeedbackMode(mode: FeedbackMode) { _state.update { it.copy(feedbackMode = mode) } }
    fun setLevel(v: Level) { _state.update { it.copy(level = v) } }
    fun setEnglishVariant(v: EnglishVariant) { _state.update { it.copy(englishVariant = v) } }
    fun setRegister(v: Register) { _state.update { it.copy(register = v) } }
    fun setExplanationLanguage(v: ExplanationLanguage) { _state.update { it.copy(explanationLanguage = v) } }

    fun toggleDistractor(t: DistractorType) {
        _state.update {
            val next = if (t in it.distractorTypes) it.distractorTypes - t else it.distractorTypes + t
            it.copy(distractorTypes = next)
        }
    }

    fun createRun() {
        val s = _state.value
        if (!s.canStart) return
        viewModelScope.launch {
            _state.update { it.copy(creating = true, createError = null) }
            val override = UniversalSettings(
                level = s.level,
                english_variant = s.englishVariant,
                register = s.register,
                explanation_language = s.explanationLanguage,
                distractor_types = s.distractorTypes.toList(),
            )
            val req = CreateRunRequest(
                lesson_id = lessonId,
                player_override = override,
                feedback_mode = s.feedbackMode,
                task_count = s.taskCount,
            )
            when (val r = runsApi.create(req)) {
                is ApiResult.Success ->
                    _state.update { it.copy(creating = false, createdRunId = r.value.id) }
                is ApiResult.Error -> _state.update { it.copy(creating = false, createError = r.toUiError()) }
                is ApiResult.Network -> _state.update { it.copy(creating = false, createError = r.toUiError()) }
            }
        }
    }

    fun consumeCreatedRunId() { _state.update { it.copy(createdRunId = null) } }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = createSavedStateHandle()
                val id = handle.get<String>(Routes.ARG_LESSON_ID) ?: error("Missing lessonId nav arg")
                RunSetupViewModel(app, id)
            }
        }
    }
}
