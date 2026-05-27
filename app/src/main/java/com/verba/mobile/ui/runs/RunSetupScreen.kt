package com.verba.mobile.ui.runs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verba.mobile.R
import com.verba.mobile.data.model.Conditional
import com.verba.mobile.data.model.DistractorType
import com.verba.mobile.data.model.EnglishTense
import com.verba.mobile.data.model.EnglishVariant
import com.verba.mobile.data.model.ExplanationLanguage
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.Level
import com.verba.mobile.data.model.Register
import com.verba.mobile.data.model.SentenceType
import com.verba.mobile.ui.errors.uiErrorMessage
import com.verba.mobile.ui.labels.label as labelOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSetupScreen(
    onBack: () -> Unit,
    onRunCreated: (runId: String) -> Unit,
    viewModel: RunSetupViewModel = viewModel(factory = RunSetupViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdRunId) {
        val id = state.createdRunId
        if (id != null) {
            viewModel.consumeCreatedRunId()
            onRunCreated(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.start_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loadingLesson -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.loadError != null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiErrorMessage(state.loadError!!), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::loadLesson) { Text(stringResource(R.string.common_retry)) }
                }
            }

            else -> Form(state = state, vm = viewModel, padding = padding)
        }
    }
}

@Composable
private fun Form(
    state: RunSetupUiState,
    vm: RunSetupViewModel,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Open-ended toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.start_open_ended), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.start_open_ended_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = state.openEnded, onCheckedChange = vm::setOpenEnded)
        }

        if (!state.openEnded) {
            SectionLabel(stringResource(R.string.start_task_count))
            OutlinedTextField(
                value = state.taskCount.toString(),
                onValueChange = { txt -> txt.toIntOrNull()?.let { vm.setTaskCount(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("1..100") },
            )
        }

        SectionLabel(stringResource(R.string.start_feedback_mode))
        ChipRow {
            FilterChip(
                selected = state.feedbackMode == FeedbackMode.IMMEDIATE,
                onClick = { vm.setFeedbackMode(FeedbackMode.IMMEDIATE) },
                label = { Text(stringResource(R.string.start_feedback_immediate)) },
            )
            FilterChip(
                selected = state.feedbackMode == FeedbackMode.END,
                onClick = { vm.setFeedbackMode(FeedbackMode.END) },
                label = { Text(stringResource(R.string.start_feedback_end)) },
            )
        }

        SectionLabel(stringResource(R.string.start_section_level) + requiredMark(state.level == null))
        ChipRow {
            Level.entries.forEach { lv ->
                FilterChip(
                    selected = state.level == lv,
                    onClick = { vm.setLevel(lv) },
                    label = { Text(labelOf(lv)) },
                )
            }
        }

        SectionLabel(stringResource(R.string.start_section_english_variant) + requiredMark(state.englishVariant == null))
        ChipRow {
            EnglishVariant.entries.forEach { v ->
                FilterChip(
                    selected = state.englishVariant == v,
                    onClick = { vm.setEnglishVariant(v) },
                    label = { Text(labelOf(v)) },
                )
            }
        }

        SectionLabel(stringResource(R.string.start_section_register) + requiredMark(state.register == null))
        ChipRow {
            Register.entries.forEach { r ->
                FilterChip(
                    selected = state.register == r,
                    onClick = { vm.setRegister(r) },
                    label = { Text(labelOf(r)) },
                )
            }
        }

        SectionLabel(stringResource(R.string.start_section_explanation_language) + requiredMark(state.explanationLanguage == null))
        ChipRow {
            ExplanationLanguage.entries.forEach { e ->
                FilterChip(
                    selected = state.explanationLanguage == e,
                    onClick = { vm.setExplanationLanguage(e) },
                    label = { Text(labelOf(e)) },
                )
            }
        }

        SectionLabel(stringResource(R.string.start_section_distractor_types) + requiredMark(state.distractorTypes.isEmpty()))
        ChipRow {
            DistractorType.entries.forEach { d ->
                FilterChip(
                    selected = d in state.distractorTypes,
                    onClick = { vm.toggleDistractor(d) },
                    label = { Text(labelOf(d)) },
                )
            }
        }

        AdvancedSection(state = state, vm = vm)

        state.createError?.let { Text(uiErrorMessage(it), color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = vm::createRun,
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.creating) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.start_submitting))
            } else {
                Text(stringResource(R.string.start_submit))
            }
        }
    }
}

@Composable
private fun AdvancedSection(state: RunSetupUiState, vm: RunSetupViewModel) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.start_section_advanced) + if (expanded) "  ▲" else "  ▼")
    }
    if (!expanded) return

    SectionLabel(stringResource(R.string.start_section_explanation_enabled))
    ChipRow {
        FilterChip(
            selected = state.explanationEnabled == true,
            onClick = { vm.setExplanationEnabled(if (state.explanationEnabled == true) null else true) },
            label = { Text(stringResource(R.string.start_feedback_immediate)) },
        )
        FilterChip(
            selected = state.explanationEnabled == false,
            onClick = { vm.setExplanationEnabled(if (state.explanationEnabled == false) null else false) },
            label = { Text(stringResource(R.string.start_feedback_end)) },
        )
    }

    SectionLabel(stringResource(R.string.start_section_english_tenses))
    ChipRow {
        EnglishTense.entries.forEach { t ->
            FilterChip(
                selected = t in state.englishTenses,
                onClick = { vm.toggleTense(t) },
                label = { Text(labelOf(t)) },
            )
        }
    }

    SectionLabel(stringResource(R.string.start_section_conditionals))
    ChipRow {
        Conditional.entries.forEach { c ->
            FilterChip(
                selected = c in state.conditionals,
                onClick = { vm.toggleConditional(c) },
                label = { Text(labelOf(c)) },
            )
        }
    }

    SectionLabel(stringResource(R.string.start_section_sentence_types))
    ChipRow {
        SentenceType.entries.forEach { s ->
            FilterChip(
                selected = s in state.sentenceTypes,
                onClick = { vm.toggleSentenceType(s) },
                label = { Text(labelOf(s)) },
            )
        }
    }
}

@Composable
private fun requiredMark(show: Boolean): String =
    if (show) stringResource(R.string.start_required_marker) else ""

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}
