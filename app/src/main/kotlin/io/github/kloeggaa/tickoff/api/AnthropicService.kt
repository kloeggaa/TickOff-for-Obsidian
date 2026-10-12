package io.github.kloeggaa.tickoff.api

import io.github.kloeggaa.tickoff.api.models.AnthropicMessageRequest
import io.github.kloeggaa.tickoff.api.models.AnthropicMessageResponse
import io.github.kloeggaa.tickoff.api.models.ModelList
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AnthropicService {
    @POST("v1/messages")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        /** Omitted when null (Retrofit skips null header values). */
        @Header("anthropic-beta") beta: String? = null,
        @Body request: AnthropicMessageRequest
    ): Response<AnthropicMessageResponse>

    /** Looks up one model: checks the key and the model name without generating text. */
    @GET("v1/models/{model}")
    suspend fun getModel(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Path("model") model: String
    ): Response<ResponseBody>

    /** Newest models first; one page of up to 1000 entries covers every Claude model. */
    @GET("v1/models")
    suspend fun listModels(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Query("limit") limit: Int = 1000
    ): Response<ModelList>
}
