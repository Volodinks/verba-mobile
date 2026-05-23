package com.verba.mobile.ui.lessons

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.verba.mobile.VerbaApp
import com.verba.mobile.data.api.ApiResult
import com.verba.mobile.data.api.LessonDetailResponse
import com.verba.mobile.ui.errors.UiError
import com.verba.mobile.ui.errors.toUiError
import com.verba.mobile.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LessonDetailUiState {
    data object Loading : LessonDetailUiState
    data class Loaded(val data: LessonDetailResponse) : LessonDetailUiState
    data class Error(val error: UiError) : LessonDetailUiState
}

class LessonDetailViewModel(
    application: Application,
    private val lessonId: String,
) : AndroidViewModel(application) {

    private val lessonsApi = (application as VerbaApp).lessonsApi

    private val _state = MutableStateFlow<LessonDetailUiState>(LessonDetailUiState.Loading)
    val state: StateFlow<LessonDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = LessonDetailUiState.Loading
            _state.value = when (val r = lessonsApi.getLesson(lessonId)) {
                is ApiResult.Success -> LessonDetailUiState.Loaded(r.value)
                is ApiResult.Error -> LessonDetailUiState.Error(r.toUiError())
                is ApiResult.Network -> LessonDetailUiState.Error(r.toUiError())
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = createSavedStateHandle()
                val id = handle.get<String>(Routes.ARG_LESSON_ID)
                    ?: error("Missing lessonId nav arg")
                LessonDetailViewModel(app, id)
            }
        }
    }
}
