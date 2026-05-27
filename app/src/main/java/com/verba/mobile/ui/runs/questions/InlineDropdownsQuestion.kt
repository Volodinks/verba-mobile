package com.verba.mobile.ui.runs.questions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.verba.mobile.R
import com.verba.mobile.data.model.InlineDropdownsTask
import com.verba.mobile.data.model.PendingAnswer

/**
 * Renders the stem_template as a flow of plain-text spans interleaved with inline dropdowns.
 *
 * To keep the layout simple we render the template's "before-first-gap" / "between-gaps" /
 * "after-last-gap" text as separate Text blocks, with a chip-style dropdown between them.
 */
@Composable
fun InlineDropdownsQuestion(
    task: InlineDropdownsTask,
    pending: PendingAnswer.InlineDropdowns?,
    answered: Boolean,
    revealAllowed: Boolean,
    onSelect: (gapIndex: Int, optionIndex: Int) -> Unit,
) {
    val segments = remember(task.stem_template, task.gaps.size) {
        splitStemTemplate(task.stem_template, task.gaps.size)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        for (segment in segments) {
            when (segment) {
                is Segment.TextSpan ->
                    Text(AnnotatedString(segment.text), style = MaterialTheme.typography.bodyLarge)
                is Segment.GapSpan -> {
                    val gap = task.gaps.getOrNull(segment.index) ?: continue
                    GapChip(
                        index = segment.index,
                        options = gap.options,
                        selection = pending?.selections?.getOrNull(segment.index),
                        correctIndex = gap.correct_index,
                        answered = answered,
                        revealAllowed = revealAllowed,
                        onPick = { optIdx -> onSelect(segment.index, optIdx) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (pending != null && !pending.isComplete) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.run_inline_submit_disabled_until_all),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GapChip(
    index: Int,
    options: List<String>,
    selection: Int?,
    correctIndex: Int?,
    answered: Boolean,
    revealAllowed: Boolean,
    onPick: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selection?.let { options.getOrNull(it) } ?: stringResource(R.string.run_inline_choose_for_gap)
    val containerColor = when {
        answered && revealAllowed && correctIndex != null && selection == correctIndex ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        answered && revealAllowed && correctIndex != null && selection != correctIndex ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    Box {
        AssistChip(
            onClick = { if (!answered) expanded = true },
            label = { Text("${index + 1}. $label") },
            colors = AssistChipDefaults.assistChipColors(containerColor = containerColor),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onPick(i)
                    },
                )
            }
        }
    }
    if (answered && revealAllowed && correctIndex != null && selection != correctIndex) {
        Text(
            "✓ ${options.getOrNull(correctIndex) ?: ""}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        )
    }
}

internal sealed interface Segment {
    data class TextSpan(val text: String) : Segment
    data class GapSpan(val index: Int) : Segment
}

private val GAP_REGEX = Regex("""\{\{gap(\d+)\}\}""")

/** Split a stem template like "I {{gap0}} a book {{gap1}} yesterday." into ordered segments. */
internal fun splitStemTemplate(template: String, expectedGapCount: Int): List<Segment> {
    val result = mutableListOf<Segment>()
    var cursor = 0
    for (match in GAP_REGEX.findAll(template)) {
        if (match.range.first > cursor) {
            result += Segment.TextSpan(template.substring(cursor, match.range.first))
        }
        val gapIndex = match.groupValues[1].toIntOrNull() ?: continue
        result += Segment.GapSpan(gapIndex)
        cursor = match.range.last + 1
    }
    if (cursor < template.length) {
        result += Segment.TextSpan(template.substring(cursor))
    }
    // Defensive: if the template didn't contain any placeholders, render the full stem plus a chip
    // per expected gap so the user can still answer.
    if (result.none { it is Segment.GapSpan } && expectedGapCount > 0) {
        if (result.isEmpty()) result += Segment.TextSpan(template)
        for (i in 0 until expectedGapCount) result += Segment.GapSpan(i)
    }
    return result
}

/** Build a [buildAnnotatedString] that interleaves plain text with placeholder markers. */
@Suppress("unused")
private fun renderStemAsAnnotated(template: String): AnnotatedString = buildAnnotatedString { append(template) }
