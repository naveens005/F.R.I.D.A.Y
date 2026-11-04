package com.friday.assistant.services.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.friday.assistant.core.state.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow(VoiceState())
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onErrorOrEndCallback: (() -> Unit)? = null

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        }
    }

    fun startListening(onResult: (String) -> Unit, onErrorOrEnd: (() -> Unit)? = null) {
        this.onFinalResultCallback = onResult
        this.onErrorOrEndCallback = onErrorOrEnd

        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            _voiceState.value = _voiceState.value.copy(isListening = true, partialText = "")
        } catch (e: Exception) {
            _voiceState.value = _voiceState.value.copy(isListening = false)
            onErrorOrEndCallback?.invoke()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignored
        } finally {
            _voiceState.value = _voiceState.value.copy(isListening = false, audioRmsDb = 0f)
            onErrorOrEndCallback?.invoke()
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = _voiceState.value.copy(isListening = true)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize and broadcast audio RMS level for visualizer
                val normalized = rmsdB.coerceIn(0f, 10f)
                _voiceState.value = _voiceState.value.copy(audioRmsDb = normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceState.value = _voiceState.value.copy(audioRmsDb = 0f)
            }

            override fun onError(error: Int) {
                _voiceState.value = _voiceState.value.copy(isListening = false, audioRmsDb = 0f)
                onErrorOrEndCallback?.invoke()
            }

            override fun onResults(results: Bundle?) {
                _voiceState.value = _voiceState.value.copy(isListening = false, audioRmsDb = 0f)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""
                if (recognizedText.isNotBlank()) {
                    onFinalResultCallback?.invoke(recognizedText)
                } else {
                    onErrorOrEndCallback?.invoke()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                _voiceState.value = _voiceState.value.copy(partialText = partial)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
