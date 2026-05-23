package com.verba.mobile.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SttState {
    data object Idle : SttState
    data object Listening : SttState
    data class Recognized(val text: String) : SttState
    data class Error(val code: Int) : SttState
}

class SpeechListener(private val context: Context) {

    private val _state = MutableStateFlow<SttState>(SttState.Idle)
    val state: StateFlow<SttState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (!isAvailable()) {
            _state.value = SttState.Error(SpeechRecognizer.ERROR_CLIENT)
            return
        }
        if (_state.value is SttState.Listening) return

        val rec = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        rec.setRecognitionListener(internalListener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        _state.value = SttState.Listening
        rec.startListening(intent)
    }

    fun cancel() {
        recognizer?.cancel()
        if (_state.value is SttState.Listening) _state.value = SttState.Idle
    }

    fun destroy() {
        runCatching {
            recognizer?.destroy()
        }
        recognizer = null
    }

    private val internalListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.takeIf { it.isNotBlank() }
            _state.value = if (text != null) SttState.Recognized(text) else SttState.Error(SpeechRecognizer.ERROR_NO_MATCH)
        }

        override fun onError(error: Int) {
            _state.value = SttState.Error(error)
        }
    }
}
