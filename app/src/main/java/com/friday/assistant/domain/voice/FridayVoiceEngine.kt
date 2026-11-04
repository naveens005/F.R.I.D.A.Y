package com.friday.assistant.domain.voice

import android.content.Context
import com.friday.assistant.data.preferences.PreferencesManager
import com.friday.assistant.services.voice.SpeechRecognitionManager
import com.friday.assistant.services.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FridayVoiceEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val preferencesManager = PreferencesManager(context)
    val sttManager = SpeechRecognitionManager(context)
    val ttsManager = TextToSpeechManager(context)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    init {
        // Observe voice state from STT manager
        scope.launch {
            sttManager.voiceState.collect { vs ->
                _isListening.value = vs.isListening
                _audioAmplitude.value = vs.audioRmsDb
            }
        }
        // Observe TTS state
        scope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                _isSpeaking.value = speaking
            }
        }
        // Apply saved preferences
        scope.launch {
            val lang = preferencesManager.voiceLanguage.first()
            val pitch = preferencesManager.voicePitch.first()
            val rate = preferencesManager.voiceRate.first()

            ttsManager.setLanguage(lang)
            ttsManager.setPitch(pitch)
            ttsManager.setSpeechRate(rate)
        }
    }

    fun startListening(onResult: (String) -> Unit, onErrorOrEnd: (() -> Unit)? = null) {
        // Interrupt TTS if F.R.I.D.A.Y. is currently speaking
        if (_isSpeaking.value) {
            ttsManager.stop()
        }
        sttManager.startListening(
            onResult = { text ->
                _isListening.value = false
                onResult(text)
            },
            onErrorOrEnd = {
                _isListening.value = false
                onErrorOrEnd?.invoke()
            }
        )
    }

    fun stopListening() {
        sttManager.stopListening()
        _isListening.value = false
        _audioAmplitude.value = 0f
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        ttsManager.speak(text, onDone)
    }

    fun stopSpeaking() {
        ttsManager.stop()
        _isSpeaking.value = false
    }

    fun setLanguage(langCode: String) {
        ttsManager.setLanguage(langCode)
        scope.launch { preferencesManager.setVoiceLanguage(langCode) }
    }

    fun setPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
        scope.launch { preferencesManager.setVoicePitch(pitch) }
    }

    fun setRate(rate: Float) {
        ttsManager.setSpeechRate(rate)
        scope.launch { preferencesManager.setVoiceRate(rate) }
    }

    fun shutdown() {
        sttManager.destroy()
        ttsManager.shutdown()
    }
}

