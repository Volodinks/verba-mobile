package com.verba.mobile.ui.runs.questions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.verba.mobile.R
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.JudgeVerdict
import com.verba.mobile.data.model.PendingAnswer
import com.verba.mobile.data.model.TextInputTask

/**
 * Renders the text-input stem_template with one OutlinedTextField per gap and (in immediate mode)
 * an "Appeal answer" affordance when the server marked the answer wrong without a prior verdict.
 */
@Composable
fun TextInputQuestion(
    task: TextInputTask,
    pending: PendingAnswer.TextInput?,
    answered: Boolean,
    isCorrect: Boolean?,
    revealAllowed: Boolean,
    feedbackMode: FeedbackMode,
    judging: Boolean,
    judgeVerdict: JudgeVerdict?,
    onValueChange: (gapIndex: Int, value: String) -> Unit,
    onAppeal: () -> Unit,
) {
    val segments = remember(task.stem_template, task.gaps.size) {
        splitStemTemplate(task.stem_template, task.gaps.size)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        for (segment in segments) {
            when (segment) {
                is com.verba.mobile.ui.runs.questions.Segment.TextSpan ->
                    Text(segment.text, style = MaterialTheme.typography.bodyLarge)
                is com.verba.mobile.ui.runs.questions.Segment.GapSpan -> {
                    val gap = task.gaps.getOrNull(segment.index) ?: continue
                    val value = pending?.inputs?.getOrNull(segment.index).orEmpty()
                    val placeholder = gap.hint?.let { stringResource(R.string.run_text_placeholder_hint, it) }
                    OutlinedTextField(
                        value = value,
                        onValueChange = { onValueChange(segment.index, it) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !answered,
                        singleLine = true,
                        label = { Text("${segment.index + 1}.") },
                        placeholder = placeholder?.let { { Text(it) } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = if (segment.index == task.gaps.lastIndex) ImeAction.Done else ImeAction.Next,
                        ),
                    )
                    if (answered && revealAllowed) {
                        val canonical = gap.correct_answers?.firstOrNull()
                        if (canonical != null && !value.equals(canonical, ignoreCase = true)) {
                            Text(
                                "✓ $canonical",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (pending != null && !pending.isComplete) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.run_text_submit_disabled_until_all),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (answered && feedbackMode == FeedbackMode.IMMEDIATE) {
            AppealBlock(
                isCorrect = isCorrect,
                judging = judging,
                verdict = judgeVerdict,
                onAppeal = onAppeal,
            )
        }
    }
}

@Composable
private fun AppealBlock(
    isCorrect: Boolean?,
    judging: Boolean,
    verdict: JudgeVerdict?,
    onAppeal: () -> Unit,
) {
    when {
        verdict?.is_actually_correct == true -> {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.run_text_appeal_accepted),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                verdict.reason,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        verdict != null && !verdict.is_actually_correct -> {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.run_text_appeal_rejected),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                verdict.reason,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        isCorrect == false -> {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAppeal, enabled = !judging) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (judging) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.Unspecified,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.run_text_appeal_pending))
                    } else {
                        Text(stringResource(R.string.run_text_appeal))
                    }
                }
            }
        }
    }
}
