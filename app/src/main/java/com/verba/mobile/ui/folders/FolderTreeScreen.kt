package com.verba.mobile.ui.folders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.ArrowDropDownCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verba.mobile.R
import com.verba.mobile.VerbaApp
import com.verba.mobile.data.model.LessonTypeId
import com.verba.mobile.locale.LocaleController
import com.verba.mobile.ui.labels.label
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderTreeScreen(
    onOpenLesson: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: FolderTreeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(
        enabled = state is FolderTreeUiState.Loaded &&
            (state as FolderTreeUiState.Loaded).currentFolderId != null,
    ) {
        viewModel.goUp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                actions = {
                    LanguageMenu()
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.sign_out),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            FolderTreeUiState.Loading -> CenteredLoading(padding)
            is FolderTreeUiState.Error -> CenteredError(padding, s.message, viewModel::reload)
            is FolderTreeUiState.Loaded -> Loaded(
                padding = padding,
                state = s,
                onOpenFolder = viewModel::openFolder,
                onGoUp = { viewModel.goUp() },
                onOpenLesson = onOpenLesson,
            )
        }
    }
}

@Composable
private fun LanguageMenu() {
    val context = LocalContext.current
    val app = context.applicationContext as VerbaApp
    val activity = remember(context) { context.findActivity() }
    var expanded by remember { mutableStateOf(false) }
    val current = remember { LocaleController.current(context) ?: "uk" }
    val scope = rememberCoroutineScope()

    val change: (String) -> Unit = { tag ->
        android.util.Log.d("VerbaLocale", "click → tag=$tag, current=${LocaleController.current(context)}")
        expanded = false
        scope.launch {
            app.localePreferences.set(tag)
            android.util.Log.d("VerbaLocale", "saved to DataStore")
        }
        LocaleController.apply(context, tag)
        android.util.Log.d("VerbaLocale", "after apply: current=${LocaleController.current(context)}")
        activity?.recreate()
        android.util.Log.d("VerbaLocale", "recreate() called")
    }

    IconButton(onClick = {
        android.util.Log.d("VerbaLocale", "icon clicked, opening menu")
        expanded = true
    }) {
        Icon(
            imageVector = Icons.Filled.Translate,
            contentDescription = stringResource(R.string.language_switcher_cd),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        LocaleItem(label = stringResource(R.string.locale_name_uk), active = current == "uk") {
            change("uk")
        }
        LocaleItem(label = stringResource(R.string.locale_name_en), active = current == "en") {
            change("en")
        }
    }
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun LocaleItem(label: String, active: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = { if (active) Icon(Icons.Filled.Check, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
private fun Loaded(
    padding: PaddingValues,
    state: FolderTreeUiState.Loaded,
    onOpenFolder: (String) -> Unit,
    onGoUp: () -> Unit,
    onOpenLesson: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Breadcrumb(state.breadcrumb, onGoUp)

        if (state.subfolders.isEmpty() && state.lessons.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.catalog_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.subfolders, key = { "f_${it.id}" }) { f ->
                    FolderRow(folder = f, onClick = { onOpenFolder(f.id) })
                }
                if (state.lessonsLoading) {
                    item("lessons_loading") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                state.lessonsError?.let { msg ->
                    item("lessons_error") {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                items(state.lessons, key = { "l_${it.id}" }) { l ->
                    LessonRow(lesson = l, onClick = { onOpenLesson(l.id) })
                }
            }
        }
    }
}

@Composable
private fun Breadcrumb(items: List<Pair<String?, String>>, onGoUp: () -> Unit) {
    if (items.size <= 1) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onGoUp) { Text("← ${items[items.lastIndex - 1].second}") }
        Spacer(Modifier.width(8.dp))
        Text(items.last().second, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FolderRow(folder: FolderEntry, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    summary(folder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun summary(folder: FolderEntry): String {
    val parts = mutableListOf<String>()
    if (folder.childrenCount > 0) {
        parts += pluralStringResource(R.plurals.subfolder_count, folder.childrenCount, folder.childrenCount)
    }
    if (folder.lessonsCount > 0) {
        parts += pluralStringResource(R.plurals.lesson_count, folder.lessonsCount, folder.lessonsCount)
    }
    return if (parts.isEmpty()) stringResource(R.string.catalog_folder_empty) else parts.joinToString(" · ")
}

@Composable
private fun LessonRow(lesson: LessonEntry, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(lessonIcon(lesson.type), contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(lesson.title, style = MaterialTheme.typography.bodyLarge)
                val sub = lesson.description ?: label(lesson.type)
                Text(
                    sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun lessonIcon(type: LessonTypeId) = when (type) {
    LessonTypeId.MULTIPLE_CHOICE -> Icons.Filled.PlayArrow
    LessonTypeId.INLINE_DROPDOWNS -> Icons.Outlined.ArrowDropDownCircle
    LessonTypeId.TEXT_INPUT -> Icons.Filled.Edit
}

@Composable
private fun CenteredLoading(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun CenteredError(padding: PaddingValues, detail: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.catalog_load_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
        }
    }
}
