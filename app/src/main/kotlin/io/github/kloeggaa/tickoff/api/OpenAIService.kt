package io.github.kloeggaa.tickoff.api

import io.github.kloeggaa.tickoff.api.models.ChatRequest
import io.github.kloeggaa.tickoff.api.models.ChatResponse
import io.github.kloeggaa.tickoff.api.models.ModelList
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>

    /** Looks up one model: checks the key and the model name without generating text. */
    @GET("v1/models/{model}")
    suspend fun getModel(
        @Header("Authorization") authHeader: String,
        @Path("model") model: String
    ): Response<ResponseBody>

    @GET("v1/models")
    suspend fun listModels(
        @Header("Authorization") authHeader: String
    ): Response<ModelList>
}
