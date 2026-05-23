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
import com.verba.mobile.data.model.UniversalSettings
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
                title = { Text(stringResource(R.string.lesson_detail_title)) },
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
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::load) { Text(stringResource(R.string.lessons_retry)) }
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
            Text(stringResource(R.string.start_lesson))
        }
        val missing = missingRequiredFields(data.effective_settings)
        if (missing.isNotEmpty()) {
            Text(
                "На наступному кроці треба буде встановити: ${missing.joinToString()}",
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
            Text(stringResource(R.string.effective_settings_title), style = MaterialTheme.typography.titleMedium)
            if (es == null) {
                Text(stringResource(R.string.effective_settings_incomplete), color = MaterialTheme.colorScheme.error)
                return@Column
            }
            es.level?.let { Field("Рівень", it.name) }
            es.english_variant?.let { Field("Варіант англ.", it.name.lowercase()) }
            es.register?.let { Field("Регістр", it.name.lowercase()) }
            es.explanation_language?.let { Field("Мова пояснень", it.name.lowercase()) }
            es.distractor_types?.takeIf { it.isNotEmpty() }
                ?.let { Field("Типи дистракторів", it.joinToString { d -> d.name.lowercase() }) }
            es.topic?.let { Field("Тематика", it) }
            es.grammar_focus?.let { Field("Граматичний фокус", it) }
        }
    }
}

@Composable
private fun TypeSettingsCard(settings: JsonObject) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.type_settings_title), style = MaterialTheme.typography.titleMedium)
            for ((k, v) in settings) {
                val display = (v as? JsonPrimitive)?.content ?: v.toString()
                Field(k, display)
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun Centered(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { content() }
}
