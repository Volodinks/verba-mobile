package com.verba.mobile.ui.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verba.mobile.R
import com.verba.mobile.data.Lesson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListScreen(
    onOpenLesson: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: LessonListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lessons_title)) },
                actions = {
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
            LessonListUiState.Loading -> CenteredLoading(padding)
            LessonListUiState.Error -> CenteredError(padding, onRetry = viewModel::load)
            is LessonListUiState.Loaded -> {
                if (s.lessons.isEmpty()) {
                    CenteredEmpty(padding)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = s.lessons, key = { it.id }) { lesson ->
                            LessonCard(lesson = lesson, onClick = { onOpenLesson(lesson.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = lesson.body,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CenteredLoading(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredEmpty(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.lessons_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredError(padding: PaddingValues, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.lessons_load_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.lessons_retry))
            }
        }
    }
}
