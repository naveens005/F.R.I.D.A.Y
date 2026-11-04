package com.friday.assistant.ui.screens.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.friday.assistant.core.state.AssistantOrbState
import com.friday.assistant.core.state.UiMessage
import com.friday.assistant.core.state.VoiceState
import com.friday.assistant.domain.orchestrator.PendingConfirmation
import com.friday.assistant.domain.orchestrator.TaskOrchestrator
import com.friday.assistant.services.voice.SpeechRecognitionManager
import com.friday.assistant.services.voice.TextToSpeechManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val speechManager = SpeechRecognitionManager(application)
    private val ttsManager = TextToSpeechManager(application)
    val taskOrchestrator = TaskOrchestrator(application, viewModelScope).apply {
        setTtsManager(ttsManager)
    }

    init {
        viewModelScope.launch {
            taskOrchestrator.preferencesManager.voicePitch.collect { pitch ->
                ttsManager.setPitch(pitch)
            }
        }
        viewModelScope.launch {
            taskOrchestrator.preferencesManager.voiceRate.collect { rate ->
                ttsManager.setSpeechRate(rate)
            }
        }
        viewModelScope.launch {
            taskOrchestrator.preferencesManager.voiceLanguage.collect { lang ->
                ttsManager.setLanguage(lang)
            }
        }
    }

    val orbState: StateFlow<AssistantOrbState> = taskOrchestrator.orbState
    val voiceState: StateFlow<VoiceState> = speechManager.voiceState
    val messages: StateFlow<List<UiMessage>> = taskOrchestrator.messages
    val pendingConfirmation: StateFlow<PendingConfirmation?> = taskOrchestrator.pendingConfirmation

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning, Boss."
            in 12..16 -> "Good Afternoon, Boss."
            in 17..21 -> "Good Evening, Boss."
            else -> "System Online, Boss."
        }
    }

    fun startVoiceListening() {
        if (!voiceState.value.isListening) {
            ttsManager.stop()
            com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
            taskOrchestrator.setOrbState(AssistantOrbState.LISTENING)
            speechManager.startListening(
                onResult = { recognizedText ->
                    taskOrchestrator.processCommand(recognizedText)
                },
                onErrorOrEnd = {
                    if (taskOrchestrator.orbState.value == AssistantOrbState.LISTENING) {
                        taskOrchestrator.setOrbState(AssistantOrbState.IDLE)
                    }
                }
            )
        }
    }

    fun toggleVoiceListening() {
        if (voiceState.value.isListening) {
            speechManager.stopListening()
            taskOrchestrator.setOrbState(AssistantOrbState.IDLE)
        } else {
            ttsManager.stop()
            com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
            taskOrchestrator.setOrbState(AssistantOrbState.LISTENING)
            speechManager.startListening(
                onResult = { recognizedText ->
                    taskOrchestrator.processCommand(recognizedText)
                },
                onErrorOrEnd = {
                    if (taskOrchestrator.orbState.value == AssistantOrbState.LISTENING) {
                        taskOrchestrator.setOrbState(AssistantOrbState.IDLE)
                    }
                }
            )
        }
    }

    fun submitTextCommand(text: String) {
        if (text.isNotBlank()) {
            ttsManager.stop()
            taskOrchestrator.processCommand(text)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        taskOrchestrator.setOrbState(AssistantOrbState.IDLE)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
    }
}
