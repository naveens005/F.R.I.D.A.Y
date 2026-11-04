package com.friday.assistant.data.remote.models

import com.google.gson.annotations.SerializedName

data class AssistantChatRequest(
    @SerializedName("query") val query: String,
    @SerializedName("conversation_history") val conversationHistory: List<ApiMessageItem> = emptyList(),
    @SerializedName("installed_packages") val installedPackages: List<String> = emptyList()
)

data class ApiMessageItem(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class AssistantChatResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("is_security_blocked") val isSecurityBlocked: Boolean,
    @SerializedName("intent") val intent: String,
    @SerializedName("speech_response") val speechResponse: String,
    @SerializedName("display_text") val displayText: String,
    @SerializedName("action") val action: ActionPayloadDto? = null,
    @SerializedName("math_details") val mathDetails: MathDetailsDto? = null,
    @SerializedName("model_used") val modelUsed: String? = null
)

data class ActionPayloadDto(
    @SerializedName("type") val type: String,
    @SerializedName("package_name") val packageName: String? = null,
    @SerializedName("app_name") val appName: String? = null,
    @SerializedName("action_type") val actionType: String? = null,
    @SerializedName("query") val query: String? = null,
    @SerializedName("url") val url: String? = null
)

data class MathDetailsDto(
    @SerializedName("operation") val operation: String? = null,
    @SerializedName("result") val result: String? = null,
    @SerializedName("steps") val steps: List<String> = emptyList(),
    @SerializedName("solutions") val solutions: List<String> = emptyList(),
    @SerializedName("mean") val mean: Double? = null,
    @SerializedName("standard_deviation") val standardDeviation: Double? = null,
    @SerializedName("formatted") val formatted: String? = null
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("engines") val engines: Map<String, String>? = null
)

