package com.verba.mobile.ui.runs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verba.mobile.R
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.InlineDropdownsTask
import com.verba.mobile.data.model.MultipleChoiceTask
import com.verba.mobile.data.model.PendingAnswer
import com.verba.mobile.data.model.TextInputTask
import com.verba.mobile.ui.errors.uiErrorMessage
import com.verba.mobile.ui.runs.questions.InlineDropdownsQuestion
import com.verba.mobile.ui.runs.questions.MultipleChoiceQuestion
import com.verba.mobile.ui.runs.questions.TextInputQuestion

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
            TopAppBar(title = { Text(progressTitle(state)) })
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ProgressBar(state)
            when {
                state.error != null -> ErrorBlock(uiErrorMessage(state.error!!))
                state.currentTask == null || state.isLoadingTask -> GeneratingBlock(state.stillGeneratingHint)
                else -> TaskBody(state = state, vm = viewModel)
            }
        }
    }
}

@Composable
private fun progressTitle(state: RunPlayUiState): String {
    val current = state.currentIndex + 1
    return if (state.isOpenEnded) {
        stringResource(R.string.run_count_unbounded, current)
    } else {
        val total = state.taskCount ?: 1
        stringResource(R.string.run_progress, current.coerceAtMost(total), total)
    }
}

@Composable
private fun ProgressBar(state: RunPlayUiState) {
    val total = state.taskCount
    if (total == null || total <= 0) {
        // Open-ended: no determinate progress.
        Box(Modifier.fillMaxWidth().height(4.dp))
    } else {
        LinearProgressIndicator(
            progress = { state.currentIndex.coerceAtLeast(0).toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TaskBody(state: RunPlayUiState, vm: RunPlayViewModel) {
    val task = state.currentTask ?: return
    val revealAllowed = state.answered && state.feedbackMode == FeedbackMode.IMMEDIATE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        when (task) {
            is MultipleChoiceTask -> MultipleChoiceQuestion(
                task = task,
                pending = state.pending as? PendingAnswer.MultipleChoice,
                answered = state.answered,
                revealAllowed = revealAllowed,
                onSelect = vm::selectMultipleChoice,
            )
            is InlineDropdownsTask -> InlineDropdownsQuestion(
                task = task,
                pending = state.pending as? PendingAnswer.InlineDropdowns,
                answered = state.answered,
                revealAllowed = revealAllowed,
                onSelect = vm::setInlineDropdownSelection,
            )
            is TextInputTask -> TextInputQuestion(
                task = task,
                pending = state.pending as? PendingAnswer.TextInput,
                answered = state.answered,
                isCorrect = state.isCorrect,
                revealAllowed = revealAllowed,
                feedbackMode = state.feedbackMode,
                judging = state.judging,
                judgeVerdict = state.judgeVerdict,
                onValueChange = vm::setTextInputValue,
                onAppeal = vm::appealAnswer,
            )
        }

        Spacer(Modifier.height(16.dp))

        FeedbackPanel(state)

        Spacer(Modifier.height(8.dp))

        ActionRow(state = state, vm = vm)
    }
}

@Composable
private fun FeedbackPanel(state: RunPlayUiState) {
    if (!state.answered) return
    when (state.feedbackMode) {
        FeedbackMode.IMMEDIATE -> {
            val correct = state.isCorrect == true
            Text(
                text = if (correct) stringResource(R.string.run_correct) else stringResource(R.string.run_incorrect),
                color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
            state.explanation?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        FeedbackMode.END -> {
            Text(
                stringResource(R.string.run_answer_submitted),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ActionRow(state: RunPlayUiState, vm: RunPlayViewModel) {
    if (!state.answered) {
        Button(
            onClick = vm::submitAnswer,
            enabled = (state.pending?.isComplete == true) && !state.submitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp).width(20.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.run_submitting))
            } else {
                Text(stringResource(R.string.run_submit))
            }
        }
        return
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = vm::next, modifier = Modifier.weight(1f)) {
            val label = when {
                state.isOpenEnded -> stringResource(R.string.run_next)
                state.isLastTask -> stringResource(R.string.run_see_result)
                else -> stringResource(R.string.run_next)
            }
            Text(label)
        }
        if (state.canFinishOpenEnded) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = vm::finishOpenEnded, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.run_finish_now))
            }
        }
    }
}

@Composable
private fun GeneratingBlock(stillGenerating: Boolean) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.run_generating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (stillGenerating) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.run_still_generating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
