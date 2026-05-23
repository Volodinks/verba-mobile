package com.verba.mobile.ui.folders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verba.mobile.VerbaApp
import com.verba.mobile.data.FoldersRepository
import kotlinx.coroutines.async
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
    val supported: Boolean,
)

sealed interface FolderTreeUiState {
    data object Loading : FolderTreeUiState
    data class Loaded(
        val currentFolderId: String?,
        val breadcrumb: List<Pair<String?, String>>, // (folderId|null=root, name)
        val subfolders: List<FolderEntry>,
        val lessons: List<LessonEntry>,
    ) : FolderTreeUiState
    data class Error(val message: String) : FolderTreeUiState
}

class FolderTreeViewModel(application: Application) : AndroidViewModel(application) {

    private val foldersRepo = (application as VerbaApp).foldersRepository

    private val _state = MutableStateFlow<FolderTreeUiState>(FolderTreeUiState.Loading)
    val state: StateFlow<FolderTreeUiState> = _state.asStateFlow()

    private var folders: List<FoldersRepository.FolderNode> = emptyList()
    private var lessons: List<FoldersRepository.LessonStub> = emptyList()
    private val stack: MutableList<String?> = mutableListOf(null) // root

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _state.value = FolderTreeUiState.Loading
            val foldersDef = async { foldersRepo.listFolders() }
            val lessonsDef = async { foldersRepo.listLessons() }
            val foldersR = foldersDef.await()
            val lessonsR = lessonsDef.await()
            val foldersErr = foldersR.exceptionOrNull()
            val lessonsErr = lessonsR.exceptionOrNull()
            if (foldersErr != null || lessonsErr != null) {
                val err = foldersErr ?: lessonsErr!!
                val source = if (foldersErr != null) "folders" else "lessons"
                _state.value = FolderTreeUiState.Error(
                    "$source: ${err::class.simpleName} · ${err.message ?: "no message"}"
                )
                return@launch
            }
            folders = foldersR.getOrThrow()
            lessons = lessonsR.getOrThrow()
            recompute()
        }
    }

    fun openFolder(id: String) {
        stack.add(id)
        recompute()
    }

    /** Pop one level. Returns true if we popped; false if already at root. */
    fun goUp(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        recompute()
        return true
    }

    private fun recompute() {
        val current = stack.last()
        val subfolders = folders.filter { it.parent_id == current }.map { f ->
            FolderEntry(
                id = f.id,
                name = f.name,
                childrenCount = folders.count { it.parent_id == f.id },
                lessonsCount = countLessonsRecursive(f.id),
            )
        }
        val visibleLessons = lessons.filter { it.folder_id == current }.map { l ->
            LessonEntry(
                id = l.id,
                title = l.title,
                description = l.description,
                supported = l.type == com.verba.mobile.data.model.LessonTypeId.MULTIPLE_CHOICE,
            )
        }
        val breadcrumb = buildBreadcrumb()
        _state.value = FolderTreeUiState.Loaded(
            currentFolderId = current,
            breadcrumb = breadcrumb,
            subfolders = subfolders,
            lessons = visibleLessons,
        )
    }

    private fun buildBreadcrumb(): List<Pair<String?, String>> = stack.map { id ->
        if (id == null) null to "Уроки"
        else id to (folders.firstOrNull { it.id == id }?.name ?: "?")
    }

    private fun countLessonsRecursive(folderId: String): Int {
        var total = lessons.count { it.folder_id == folderId }
        val children = folders.filter { it.parent_id == folderId }
        for (c in children) total += countLessonsRecursive(c.id)
        return total
    }
}
