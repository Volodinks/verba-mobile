package com.verba.mobile.ui.runs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verba.mobile.R
import com.verba.mobile.data.api.CompleteRunResponse
import com.verba.mobile.data.model.SkillBreakdownEntry
import com.verba.mobile.data.model.Statistics
import com.verba.mobile.data.model.StatisticsMistake
import com.verba.mobile.ui.errors.uiErrorMessage
import com.verba.mobile.ui.labels.labelForRawDistractor
import com.verba.mobile.ui.labels.labelForRawSkill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunResultScreen(
    onDone: () -> Unit,
    viewModel: RunResultViewModel = viewModel(factory = RunResultViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.result_title)) }) },
    ) { padding ->
        when (val s = state) {
            RunResultUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.result_computing),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is RunResultUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.result_load_error), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        uiErrorMessage(s.error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::load) { Text(stringResource(R.string.common_retry)) }
                }
            }

            is RunResultUiState.Loaded -> Body(padding, s.data, onDone)
        }
    }
}

@Composable
private fun Body(padding: PaddingValues, data: CompleteRunResponse, onDone: () -> Unit) {
    val stat = data.statistics
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SummaryCard(stat)
        if (stat.breakdown_by_skill.isNotEmpty()) BreakdownCard(
            title = stringResource(R.string.result_by_skill),
            breakdown = stat.breakdown_by_skill,
            kind = BreakdownKind.SKILL,
        )
        if (stat.breakdown_by_distractor.isNotEmpty()) BreakdownCard(
            title = stringResource(R.string.result_by_distractor),
            breakdown = stat.breakdown_by_distractor,
            kind = BreakdownKind.DISTRACTOR,
        )
        if (stat.mistakes.isNotEmpty()) MistakesCard(stat.mistakes)

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.result_back_to_catalog))
        }
    }
}

@Composable
private fun SummaryCard(stat: Statistics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${(stat.accuracy * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.result_summary_correct, stat.correct, stat.total_tasks),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.result_avg_time, (stat.avg_time_per_task_ms / 1000).toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class BreakdownKind { SKILL, DISTRACTOR }

@Composable
private fun BreakdownCard(
    title: String,
    breakdown: Map<String, SkillBreakdownEntry>,
    kind: BreakdownKind,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            for ((key, entry) in breakdown) {
                val displayKey = when (kind) {
                    BreakdownKind.SKILL -> labelForRawSkill(key) ?: key
                    BreakdownKind.DISTRACTOR -> labelForRawDistractor(key) ?: key
                }
                Text("$displayKey: ${entry.correct}/${entry.total} (${(entry.accuracy * 100).toInt()}%)")
            }
        }
    }
}

@Composable
private fun MistakesCard(mistakes: List<StatisticsMistake>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.result_mistakes), style = MaterialTheme.typography.titleMedium)
            for (m in mistakes) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(m.stem, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "✗ ${m.user_selected_text}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "✓ ${m.correct_text}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    m.explanation?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
