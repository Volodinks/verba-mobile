package com.verba.mobile.ui.runs.questions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.verba.mobile.data.model.MultipleChoiceTask
import com.verba.mobile.data.model.PendingAnswer

private enum class OptionVisualState { NEUTRAL, SELECTED, CORRECT, WRONG }

@Composable
fun MultipleChoiceQuestion(
    task: MultipleChoiceTask,
    pending: PendingAnswer.MultipleChoice?,
    answered: Boolean,
    revealAllowed: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(task.stem, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        for ((i, option) in task.options.withIndex()) {
            OptionItem(
                text = option,
                state = optionState(
                    selected = pending?.selectedIndex,
                    correctIndex = task.correct_index,
                    answered = answered,
                    revealAllowed = revealAllowed,
                    index = i,
                ),
                onClick = { onSelect(i) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun optionState(
    selected: Int?,
    correctIndex: Int?,
    answered: Boolean,
    revealAllowed: Boolean,
    index: Int,
): OptionVisualState {
    if (answered && revealAllowed && correctIndex != null) {
        if (correctIndex == index) return OptionVisualState.CORRECT
        if (selected == index) return OptionVisualState.WRONG
        return OptionVisualState.NEUTRAL
    }
    if (selected == index) return OptionVisualState.SELECTED
    return OptionVisualState.NEUTRAL
}

@Composable
private fun OptionItem(text: String, state: OptionVisualState, onClick: () -> Unit) {
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
