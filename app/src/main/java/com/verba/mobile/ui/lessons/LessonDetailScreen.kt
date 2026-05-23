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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verba.mobile.R
import com.verba.mobile.data.api.LessonDetailResponse
import com.verba.mobile.data.model.DistractorType
import com.verba.mobile.data.model.EnglishVariant
import com.verba.mobile.data.model.ExplanationLanguage
import com.verba.mobile.data.model.Level
import com.verba.mobile.data.model.Register
import com.verba.mobile.data.model.UniversalSettings
import com.verba.mobile.ui.errors.uiErrorMessage
import com.verba.mobile.ui.labels.label
import com.verba.mobile.ui.labels.labelForRawDistractor
import com.verba.mobile.ui.labels.labelForRawSkill
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    onBack: () -> Unit,
    onStartRun: (lessonId: String) -> Unit,
    viewModel: LessonDetailViewModel = viewModel(factory = LessonDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lesson_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            LessonDetailUiState.Loading -> Centered(padding) { CircularProgressIndicator() }
            is LessonDetailUiState.Error -> Centered(padding) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.lesson_load_error), color = MaterialTheme.colorScheme.error)
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
            is LessonDetailUiState.Loaded -> Body(
                padding = padding,
                data = s.data,
                onStart = { onStartRun(s.data.lesson.id) },
            )
        }
    }
}

@Composable
private fun Body(padding: PaddingValues, data: LessonDetailResponse, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (data.ancestor_chain.isNotEmpty()) {
            Text(
                data.ancestor_chain.joinToString(" / ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(data.lesson.title, style = MaterialTheme.typography.titleLarge)
        data.lesson.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }

        EffectiveSettingsCard(data.effective_settings)
        TypeSettingsCard(data.lesson.type_settings)

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.lesson_start))
        }
        val missing = missingRequiredFields(data.effective_settings)
        if (missing.isNotEmpty()) {
            Text(
                stringResource(R.string.lesson_needs_on_next, missing.joinToString()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun missingRequiredFields(s: UniversalSettings?): List<String> {
    if (s == null) return listOf("level", "english_variant", "register", "explanation_language", "distractor_types")
    val out = mutableListOf<String>()
    if (s.level == null) out += "level"
    if (s.english_variant == null) out += "english_variant"
    if (s.register == null) out += "register"
    if (s.explanation_language == null) out += "explanation_language"
    if (s.distractor_types.isNullOrEmpty()) out += "distractor_types"
    return out
}

@Composable
private fun EffectiveSettingsCard(es: UniversalSettings?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.lesson_settings_header), style = MaterialTheme.typography.titleMedium)
            if (es == null) {
                Text(
                    stringResource(R.string.lesson_settings_not_set),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            es.level?.let { Row(stringResource(R.string.preview_level), label(it)) }
            es.english_variant?.let { Row(stringResource(R.string.preview_variant), label(it)) }
            es.register?.let { Row(stringResource(R.string.preview_register), label(it)) }
            es.explanation_language?.let { Row(stringResource(R.string.preview_explanation_language), label(it)) }
            es.distractor_types?.takeIf { it.isNotEmpty() }?.let { types ->
                val labels = mutableListOf<String>()
                for (d in types) labels += label(d)
                Row(stringResource(R.string.preview_distractors), labels.joinToString())
            }
            es.topic?.let { Row(stringResource(R.string.preview_topic), it) }
            es.grammar_focus?.let { Row(stringResource(R.string.preview_grammar_focus), it) }
        }
    }
}

@Composable
private fun TypeSettingsCard(settings: JsonObject) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.lesson_type_settings_header), style = MaterialTheme.typography.titleMedium)
            for ((k, v) in settings) {
                val raw = (v as? JsonPrimitive)?.content ?: v.toString()
                val displayValue = when (k) {
                    "skill_target" -> labelForRawSkill(raw) ?: raw
                    "distractor_type" -> labelForRawDistractor(raw) ?: raw
                    else -> raw
                }
                Row(k, displayValue)
            }
        }
    }
}

@Composable
private fun Row(label: String, value: String) {
    Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Centered(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { content() }
}
