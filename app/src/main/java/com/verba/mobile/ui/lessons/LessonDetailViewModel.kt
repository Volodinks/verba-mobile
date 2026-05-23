package com.verba.mobile.ui.lessons

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.verba.mobile.VerbaApp
import com.verba.mobile.data.Lesson
import com.verba.mobile.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LessonDetailUiState {
    data object Loading : LessonDetailUiState
    data class Loaded(val lesson: Lesson) : LessonDetailUiState
    data object NotFound : LessonDetailUiState
    data object Error : LessonDetailUiState
}

class LessonDetailViewModel(
    application: Application,
    private val lessonId: String,
) : AndroidViewModel(application) {

    private val lessonsRepository = (application as VerbaApp).lessonsRepository

    private val _state = MutableStateFlow<LessonDetailUiState>(LessonDetailUiState.Loading)
    val state: StateFlow<LessonDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = LessonDetailUiState.Loading
            lessonsRepository.getLessonById(lessonId).fold(
                onSuccess = { lesson ->
                    _state.value = if (lesson == null) LessonDetailUiState.NotFound
                    else LessonDetailUiState.Loaded(lesson)
                },
                onFailure = { _state.value = LessonDetailUiState.Error },
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = createSavedStateHandle()
                val lessonId = handle.get<String>(Routes.ARG_LESSON_ID)
                    ?: error("Missing lessonId nav arg")
                LessonDetailViewModel(app, lessonId)
            }
        }
    }
}
