package com.verba.mobile.ui.folders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verba.mobile.VerbaApp
import com.verba.mobile.data.model.Folder
import com.verba.mobile.data.model.Lesson
import com.verba.mobile.data.model.LessonTypeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FolderEntry(
    val id: String,
    val name: String,
    val childrenCount: Int,
    val lessonsCount: Int,
)

data class LessonEntry(
    val id: String,
    val title: String,
    val description: String?,
    val type: LessonTypeId,
)

sealed interface FolderTreeUiState {
    data object Loading : FolderTreeUiState
    data class Loaded(
        val currentFolderId: String?,
        val breadcrumb: List<Pair<String?, String>>, // (folderId|null=root, name)
        val subfolders: List<FolderEntry>,
        val lessons: List<LessonEntry>,
        val lessonsLoading: Boolean,
        val lessonsError: String? = null,
    ) : FolderTreeUiState
    data class Error(val message: String) : FolderTreeUiState
}

class FolderTreeViewModel(application: Application) : AndroidViewModel(application) {

    private val foldersRepo = (application as VerbaApp).foldersRepository

    private val _state = MutableStateFlow<FolderTreeUiState>(FolderTreeUiState.Loading)
    val state: StateFlow<FolderTreeUiState> = _state.asStateFlow()

    private var folders: List<Folder> = emptyList()
    private val lessonsByFolder: MutableMap<String, List<Lesson>> = mutableMapOf()
    private val stack: MutableList<String?> = mutableListOf(null) // root

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _state.value = FolderTreeUiState.Loading
            val foldersR = foldersRepo.listFolders()
            val err = foldersR.exceptionOrNull()
            if (err != null) {
                _state.value = FolderTreeUiState.Error(
                    "folders: ${err::class.simpleName} · ${err.message ?: "no message"}",
                )
                return@launch
            }
            folders = foldersR.getOrThrow()
            lessonsByFolder.clear()
            recompute()
            loadLessonsForCurrentFolder()
        }
    }

    fun openFolder(id: String) {
        stack.add(id)
        recompute()
        viewModelScope.launch { loadLessonsForCurrentFolder() }
    }

    /** Pop one level. Returns true if we popped; false if already at root. */
    fun goUp(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        recompute()
        viewModelScope.launch { loadLessonsForCurrentFolder() }
        return true
    }

    private suspend fun loadLessonsForCurrentFolder() {
        val current = stack.last() ?: run {
            // Root has no API-supplied lessons (lessons must live inside a folder).
            recompute()
            return
        }
        if (lessonsByFolder.containsKey(current)) return // already cached
        // Show "loading lessons" marker without dropping the rest of the loaded state.
        (_state.value as? FolderTreeUiState.Loaded)?.let { loaded ->
            _state.value = loaded.copy(lessonsLoading = true, lessonsError = null)
        }
        val r = foldersRepo.listLessons(current)
        val err = r.exceptionOrNull()
        if (err != null) {
            (_state.value as? FolderTreeUiState.Loaded)?.let { loaded ->
                _state.value = loaded.copy(
                    lessonsLoading = false,
                    lessonsError = err.message ?: err::class.simpleName,
                )
            }
            return
        }
        lessonsByFolder[current] = r.getOrThrow()
        recompute()
    }

    private fun recompute() {
        val current = stack.last()
        val subfolders = folders
            .filter { it.parent_id == current }
            .sortedWith(compareBy({ it.position }, { it.name }))
            .map { f ->
                FolderEntry(
                    id = f.id,
                    name = f.name,
                    childrenCount = folders.count { it.parent_id == f.id },
                    lessonsCount = f.lesson_count ?: 0,
                )
            }
        val lessons = (current?.let { lessonsByFolder[it] } ?: emptyList()).map { l ->
            LessonEntry(
                id = l.id,
                title = l.title,
                description = l.description,
                type = l.type,
            )
        }
        val breadcrumb = buildBreadcrumb()
        _state.value = FolderTreeUiState.Loaded(
            currentFolderId = current,
            breadcrumb = breadcrumb,
            subfolders = subfolders,
            lessons = lessons,
            lessonsLoading = current != null && !lessonsByFolder.containsKey(current),
        )
    }

    private fun buildBreadcrumb(): List<Pair<String?, String>> = stack.map { id ->
        if (id == null) null to "Уроки"
        else id to (folders.firstOrNull { it.id == id }?.name ?: "?")
    }
}
