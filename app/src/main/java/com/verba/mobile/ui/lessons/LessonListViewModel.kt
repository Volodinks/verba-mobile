package com.verba.mobile.ui.lessons

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verba.mobile.VerbaApp
import com.verba.mobile.data.Lesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LessonListUiState {
    data object Loading : LessonListUiState
    data class Loaded(val lessons: List<Lesson>) : LessonListUiState
    data object Error : LessonListUiState
}

class LessonListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VerbaApp
    private val lessonsRepository = app.lessonsRepository
    private val authRepository = app.authRepository

    private val _state = MutableStateFlow<LessonListUiState>(LessonListUiState.Loading)
    val state: StateFlow<LessonListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _state.value = LessonListUiState.Error
            return
        }
        viewModelScope.launch {
            _state.value = LessonListUiState.Loading
            lessonsRepository.getMyLessons(uid).fold(
                onSuccess = { _state.value = LessonListUiState.Loaded(it) },
                onFailure = { _state.value = LessonListUiState.Error },
            )
        }
    }
}
