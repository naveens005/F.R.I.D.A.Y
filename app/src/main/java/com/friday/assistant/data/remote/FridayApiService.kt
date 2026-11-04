package com.friday.assistant.data.remote

import com.friday.assistant.data.remote.models.AssistantChatRequest
import com.friday.assistant.data.remote.models.AssistantChatResponse
import com.friday.assistant.data.remote.models.HealthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface FridayApiService {

    @POST("api/v1/assistant/chat")
    suspend fun sendChat(@Body request: AssistantChatRequest): Response<AssistantChatResponse>

    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>
}

