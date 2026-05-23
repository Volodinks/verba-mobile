package com.verba.mobile.ui.runs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.verba.mobile.data.model.DistractorType
import com.verba.mobile.data.model.EnglishVariant
import com.verba.mobile.data.model.ExplanationLanguage
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.Level
import com.verba.mobile.data.model.Register

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
                title = { Text(stringResource(R.string.run_setup_title)) },
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
                    Text(state.loadError!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::loadLesson) { Text(stringResource(R.string.lessons_retry)) }
                }
            }

            else -> Form(state = state, vm = viewModel, padding = padding)
        }
    }
}

@Composable
private fun Form(state: RunSetupUiState, vm: RunSetupViewModel, padding: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionLabel(stringResource(R.string.run_setup_count_label))
        OutlinedTextField(
            value = state.taskCount.toString(),
            onValueChange = { txt -> txt.toIntOrNull()?.let { vm.setTaskCount(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("1..100") },
        )

        SectionLabel(stringResource(R.string.run_setup_feedback_label))
        ChipRow {
            FilterChip(
                selected = state.feedbackMode == FeedbackMode.IMMEDIATE,
                onClick = { vm.setFeedbackMode(FeedbackMode.IMMEDIATE) },
                label = { Text(stringResource(R.string.feedback_immediate)) },
            )
            FilterChip(
                selected = state.feedbackMode == FeedbackMode.END,
                onClick = { vm.setFeedbackMode(FeedbackMode.END) },
                label = { Text(stringResource(R.string.feedback_end)) },
            )
        }

        SectionLabel("Рівень${if (state.level == null) " *" else ""}")
        ChipRow {
            Level.entries.forEach { lv ->
                FilterChip(
                    selected = state.level == lv,
                    onClick = { vm.setLevel(lv) },
                    label = { Text(lv.name) },
                )
            }
        }

        SectionLabel("Варіант англ.${if (state.englishVariant == null) " *" else ""}")
        ChipRow {
            FilterChip(
                selected = state.englishVariant == EnglishVariant.BRITISH,
                onClick = { vm.setEnglishVariant(EnglishVariant.BRITISH) },
                label = { Text("British") },
            )
            FilterChip(
                selected = state.englishVariant == EnglishVariant.AMERICAN,
                onClick = { vm.setEnglishVariant(EnglishVariant.AMERICAN) },
                label = { Text("American") },
            )
        }

        SectionLabel("Регістр${if (state.register == null) " *" else ""}")
        ChipRow {
            FilterChip(
                selected = state.register == Register.FORMAL,
                onClick = { vm.setRegister(Register.FORMAL) },
                label = { Text("Formal") },
            )
            FilterChip(
                selected = state.register == Register.NEUTRAL,
                onClick = { vm.setRegister(Register.NEUTRAL) },
                label = { Text("Neutral") },
            )
            FilterChip(
                selected = state.register == Register.INFORMAL,
                onClick = { vm.setRegister(Register.INFORMAL) },
                label = { Text("Informal") },
            )
        }

        SectionLabel("Мова пояснень${if (state.explanationLanguage == null) " *" else ""}")
        ChipRow {
            FilterChip(
                selected = state.explanationLanguage == ExplanationLanguage.ENGLISH,
                onClick = { vm.setExplanationLanguage(ExplanationLanguage.ENGLISH) },
                label = { Text("English") },
            )
            FilterChip(
                selected = state.explanationLanguage == ExplanationLanguage.UKRAINIAN,
                onClick = { vm.setExplanationLanguage(ExplanationLanguage.UKRAINIAN) },
                label = { Text("Ukrainian") },
            )
        }

        SectionLabel("Типи дистракторів${if (state.distractorTypes.isEmpty()) " *" else ""}")
        ChipRow {
            DistractorType.entries.forEach { d ->
                FilterChip(
                    selected = d in state.distractorTypes,
                    onClick = { vm.toggleDistractor(d) },
                    label = { Text(d.name.lowercase()) },
                )
            }
        }

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = vm::createRun,
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.creating) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.starting))
            } else {
                Text(stringResource(R.string.start_lesson))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}
