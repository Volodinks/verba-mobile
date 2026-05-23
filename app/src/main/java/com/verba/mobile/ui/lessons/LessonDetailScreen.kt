package com.verba.mobile.ui.lessons

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verba.mobile.R
import com.verba.mobile.voice.SpeechListener
import com.verba.mobile.voice.SttState
import com.verba.mobile.voice.TtsEngine
import com.verba.mobile.voice.TtsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    onBack: () -> Unit,
    viewModel: LessonDetailViewModel = viewModel(factory = LessonDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val tts = remember { TtsEngine(context.applicationContext) }
    val ttsState by tts.state.collectAsStateWithLifecycle()

    val stt = remember { SpeechListener(context.applicationContext) }
    val sttState by stt.state.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            tts.shutdown()
            stt.destroy()
        }
    }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var lastRecognized by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sttState) {
        val current = sttState
        if (current is SttState.Recognized) lastRecognized = current.text
    }

    val permissionDeniedMsg = stringResource(R.string.mic_permission_required)
    val permanentlyDeniedMsg = stringResource(R.string.mic_permission_denied_permanently)
    val openSettingsLabel = stringResource(R.string.open_settings)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            stt.start()
        } else {
            val activity = context as? Activity
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
            } ?: true
            scope.launch {
                if (showRationale) {
                    snackbarHost.showSnackbar(permissionDeniedMsg)
                } else {
                    val result = snackbarHost.showSnackbar(
                        message = permanentlyDeniedMsg,
                        actionLabel = openSettingsLabel,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }

    val onListen: () -> Unit = onListen@{
        if (!stt.isAvailable()) {
            scope.launch { snackbarHost.showSnackbar(context.getString(R.string.listen_unavailable)) }
            return@onListen
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) stt.start() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lesson_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
    ) { padding ->
        when (val s = state) {
            LessonDetailUiState.Loading -> CenteredLoading(padding)
            LessonDetailUiState.Error -> CenteredMessage(
                padding = padding,
                message = stringResource(R.string.lessons_load_error),
                actionLabel = stringResource(R.string.lessons_retry),
                onAction = viewModel::load,
            )
            LessonDetailUiState.NotFound -> CenteredMessage(
                padding = padding,
                message = stringResource(R.string.lesson_not_found),
                actionLabel = stringResource(R.string.back_to_list),
                onAction = onBack,
            )
            is LessonDetailUiState.Loaded -> LessonDetailBody(
                bodyText = s.lesson.body,
                padding = padding,
                ttsState = ttsState,
                onSpeakToggle = {
                    if (ttsState == TtsState.Speaking) tts.stop() else tts.speak(s.lesson.body)
                },
                sttState = sttState,
                lastRecognized = lastRecognized,
                onListen = onListen,
            )
        }
    }
}

@Composable
private fun LessonDetailBody(
    bodyText: String,
    padding: PaddingValues,
    ttsState: TtsState,
    onSpeakToggle: () -> Unit,
    sttState: SttState,
    lastRecognized: String?,
    onListen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        )

        Spacer(Modifier.height(16.dp))

        VoiceControls(
            ttsState = ttsState,
            onSpeakToggle = onSpeakToggle,
            sttState = sttState,
            lastRecognized = lastRecognized,
            onListen = onListen,
        )
    }
}

@Composable
private fun VoiceControls(
    ttsState: TtsState,
    onSpeakToggle: () -> Unit,
    sttState: SttState,
    lastRecognized: String?,
    onListen: () -> Unit,
) {
    Column {
        val speakLabel = when (ttsState) {
            TtsState.Speaking -> R.string.speak_stop
            TtsState.Unavailable -> R.string.speak_unavailable
            else -> R.string.speak
        }
        Button(
            onClick = onSpeakToggle,
            enabled = ttsState == TtsState.Idle || ttsState == TtsState.Speaking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (ttsState == TtsState.Speaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(speakLabel))
        }

        Spacer(Modifier.height(12.dp))

        val listenLabel = when (sttState) {
            SttState.Listening -> R.string.listen_active
            else -> R.string.listen
        }
        Button(
            onClick = onListen,
            enabled = sttState !is SttState.Listening,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Filled.Mic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(listenLabel))
        }

        Spacer(Modifier.height(12.dp))

        val resultLine: String = when (val s = sttState) {
            is SttState.Recognized -> s.text
            is SttState.Error -> when (s.code) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> stringResource(R.string.stt_error_no_match)
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                -> stringResource(R.string.stt_error_network)
                else -> stringResource(R.string.stt_error_generic)
            }
            else -> lastRecognized ?: stringResource(R.string.recognized_placeholder)
        }

        Text(
            text = resultLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredLoading(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator() }
}

@Composable
private fun CenteredMessage(
    padding: PaddingValues,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
