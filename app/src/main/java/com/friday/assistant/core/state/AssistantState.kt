package com.friday.assistant.core.state

enum class AssistantOrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING,
    BLOCKED
}

data class UiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val intentType: String? = null,
    val actionDetails: String? = null,
    val mathSteps: List<String> = emptyList(),
    val isSecurityBlocked: Boolean = false
)

enum class MessageSender {
    USER,
    FRIDAY
}

data class VoiceState(
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val partialText: String = "",
    val audioRmsDb: Float = 0f
)

