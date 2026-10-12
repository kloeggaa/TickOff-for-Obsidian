package io.github.kloeggaa.tickoff.api

import io.github.kloeggaa.tickoff.api.models.ChatRequest
import io.github.kloeggaa.tickoff.api.models.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
