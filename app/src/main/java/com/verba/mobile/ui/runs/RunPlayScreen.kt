package com.verba.mobile.ui.runs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verba.mobile.R
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.MultipleChoiceTask
import com.verba.mobile.ui.errors.uiErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunPlayScreen(
    onFinished: (runId: String) -> Unit,
    runId: String,
    viewModel: RunPlayViewModel = viewModel(factory = RunPlayViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) {
        if (state.finished) onFinished(runId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val current = (state.currentIndex + 1).coerceAtMost(state.taskCount.coerceAtLeast(1))
                    Text(stringResource(R.string.run_progress, current, state.taskCount.coerceAtLeast(1)))
                },
            )
        },
    ) { padding ->
        val taskCount = state.taskCount.coerceAtLeast(1)
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { state.currentIndex.coerceAtLeast(0).toFloat() / taskCount.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            when {
                state.error != null -> ErrorBlock(uiErrorMessage(state.error!!))
                state.currentTask == null || state.isLoadingTask -> Loading()
                else -> TaskBody(
                    state = state,
                    onSelect = viewModel::selectOption,
                    onSubmit = viewModel::submitAnswer,
                    onNext = viewModel::next,
                )
            }
        }
    }
}

@Composable
private fun TaskBody(
    state: RunPlayUiState,
    onSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
) {
    val task = state.currentTask ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(task.stem, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        for ((i, option) in task.options.withIndex()) {
            OptionItem(
                text = option,
                state = optionState(state, task, i),
                onClick = { onSelect(i) },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))

        when {
            state.feedback == null -> Button(
                onClick = onSubmit,
                enabled = state.selectedOption != null && !state.submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(stringResource(R.string.run_submit))
                }
            }
            else -> {
                if (state.feedbackMode == FeedbackMode.IMMEDIATE) {
                    val correct = state.feedback!!.is_correct == true
                    Text(
                        text = if (correct) stringResource(R.string.run_correct)
                        else stringResource(R.string.run_incorrect),
                        color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.feedback!!.explanation?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    val isLast = state.currentIndex + 1 >= state.taskCount
                    Text(
                        if (isLast) stringResource(R.string.run_see_result)
                        else stringResource(R.string.run_next),
                    )
                }
            }
        }
    }
}

private enum class OptionVisualState { NEUTRAL, SELECTED, CORRECT, WRONG }

private fun optionState(state: RunPlayUiState, task: MultipleChoiceTask, index: Int): OptionVisualState {
    val feedback = state.feedback
    if (feedback != null && state.feedbackMode == FeedbackMode.IMMEDIATE) {
        if (feedback.correct_index == index) return OptionVisualState.CORRECT
        if (state.selectedOption == index && feedback.is_correct == false) return OptionVisualState.WRONG
        if (state.selectedOption == index) return OptionVisualState.SELECTED
    } else if (state.selectedOption == index) {
        return OptionVisualState.SELECTED
    }
    return OptionVisualState.NEUTRAL
}

@Composable
private fun OptionItem(
    text: String,
    state: OptionVisualState,
    onClick: () -> Unit,
) {
    val border = when (state) {
        OptionVisualState.NEUTRAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        OptionVisualState.SELECTED -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        OptionVisualState.CORRECT -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        OptionVisualState.WRONG -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
    }
    val bg = when (state) {
        OptionVisualState.CORRECT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        OptionVisualState.WRONG -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        border = border,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(bg).padding(16.dp)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.run_generating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorBlock(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.run_load_error), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
