package com.verba.mobile.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface TtsState {
    data object Initializing : TtsState
    data object Idle : TtsState
    data object Speaking : TtsState
    data object Unavailable : TtsState
}

class TtsEngine(context: Context) {

    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status != TextToSpeech.SUCCESS) {
            _state.value = TtsState.Unavailable
            return@TextToSpeech
        }
        val locale = Locale.ENGLISH
        val languageResult = tts.setLanguage(locale)
        val available = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED
        _state.value = if (available) TtsState.Idle else TtsState.Unavailable
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TtsState.Speaking
            }

            override fun onDone(utteranceId: String?) {
                if (_state.value == TtsState.Speaking) _state.value = TtsState.Idle
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Idle
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.value = TtsState.Idle
            }
        })
    }

    fun speak(text: String) {
        if (_state.value == TtsState.Unavailable) return
        if (text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        tts.stop()
        if (_state.value == TtsState.Speaking) _state.value = TtsState.Idle
    }

    fun shutdown() {
        runCatching {
            tts.stop()
            tts.shutdown()
        }
    }

    private companion object {
        const val UTTERANCE_ID = "verba-lesson"
    }
}
