package io.github.kloeggaa.tickoff.api

import io.github.kloeggaa.tickoff.api.models.AnthropicMessageRequest
import io.github.kloeggaa.tickoff.api.models.AnthropicMessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AnthropicService {
    @POST("v1/messages")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        /** Omitted when null (Retrofit skips null header values). */
        @Header("anthropic-beta") beta: String? = null,
        @Body request: AnthropicMessageRequest
    ): Response<AnthropicMessageResponse>
}
