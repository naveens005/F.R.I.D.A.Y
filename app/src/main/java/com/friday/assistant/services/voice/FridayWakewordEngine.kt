package com.friday.assistant.services.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stark Wakeword & Ambient Voice Trigger Engine.
 * Listens for the keyphrase "Friday" or "Hey Friday" to trigger instant hands-free assistant execution.
 */
class FridayWakewordEngine(
    private val context: Context,
    private val onWakewordDetected: (String) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _isWakewordActive = MutableStateFlow(false)
    val isWakewordActive: StateFlow<Boolean> = _isWakewordActive.asStateFlow()

    fun startWakewordDetection() {
        if (_isWakewordActive.value) return

        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@FridayWakewordEngine)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                _isWakewordActive.value = true
            }
        } catch (e: Exception) {
            _isWakewordActive.value = false
        }
    }

    fun stopWakewordDetection() {
        _isWakewordActive.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {}
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
        var triggered = false

        for (match in matches) {
            val lower = match.lowercase().trim()
            if (lower.startsWith("hey friday") || lower.startsWith("friday") || lower.contains("hey friday")) {
                val directive = lower.removePrefix("hey friday").removePrefix("friday").trim()
                triggered = true
                onWakewordDetected(directive)
                break
            }
        }

        // Auto restart loop if still active
        if (_isWakewordActive.value && !triggered) {
            restartListening()
        }
    }

    override fun onError(error: Int) {
        if (_isWakewordActive.value) {
            restartListening()
        }
    }

    private fun restartListening() {
        try {
            speechRecognizer?.cancel()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {}
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (match in matches) {
            val lower = match.lowercase().trim()
            if (lower.startsWith("hey friday") || lower.startsWith("friday")) {
                val directive = lower.removePrefix("hey friday").removePrefix("friday").trim()
                stopWakewordDetection()
                onWakewordDetected(directive)
                break
            }
        }
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}
}

