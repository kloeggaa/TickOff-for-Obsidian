package io.github.kloeggaa.tickoff.api

import android.util.Log
import io.github.kloeggaa.tickoff.SafeLog
import io.github.kloeggaa.tickoff.api.models.ChatRequest
import io.github.kloeggaa.tickoff.api.models.Message
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAIClient(private val apiKey: String) {
    companion object {
        /** Default OpenAI model for fresh installs; the user can change it in Settings. */
        const val DEFAULT_MODEL = "gpt-5.6-luna"
        private const val TAG = "TickOffOpenAI"
    }

    private val service: OpenAIService = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .dns(Ipv4PreferredDns)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        )
        .build()
        .create(OpenAIService::class.java)

    /** Checks the key and [model] with a model lookup; no text is generated or sent. */
    suspend fun checkKey(model: String): KeyCheckResult = try {
        val response = service.getModel("Bearer $apiKey", model.ifBlank { DEFAULT_MODEL })
        response.body()?.close()
        response.errorBody()?.close()
        KeyCheckResult(KeyCheck.outcomeFor(response.code()), response.code())
    } catch (_: IOException) {
        KeyCheckResult(KeyCheckOutcome.NETWORK_ERROR)
    } catch (e: Exception) {
        SafeLog.e(TAG, "key check failed", e)
        KeyCheckResult(KeyCheckOutcome.OTHER)
    }

    /** The models this key can use (newest first for Claude, chat models only for OpenAI). */
    suspend fun listModels(): ModelListResult = try {
        val response = service.listModels("Bearer $apiKey")
        val ids = response.body()?.data.orEmpty().map { it.id }
        response.errorBody()?.close()
        ModelListResult(ModelFilter.openAiChatModels(ids), KeyCheckResult(KeyCheck.outcomeFor(response.code()), response.code()))
    } catch (_: IOException) {
        ModelListResult(emptyList(), KeyCheckResult(KeyCheckOutcome.NETWORK_ERROR))
    } catch (e: Exception) {
        SafeLog.e(TAG, "model list failed", e)
        ModelListResult(emptyList(), KeyCheckResult(KeyCheckOutcome.OTHER))
    }

    suspend fun getCompletion(model: String, messages: List<Message>): CompletionResult {
        val maxAttempts = 3
        var lastError = "Request failed."
        repeat(maxAttempts) { attempt ->
            try {
                val response = service.getChatCompletion("Bearer $apiKey", ChatRequest(model, messages))
                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    return if (!content.isNullOrBlank()) {
                        CompletionResult(content = content)
                    } else {
                        CompletionResult(error = "OpenAI returned an empty response.")
                    }
                }

                val code = response.code()
                val errorSnippet = response.errorBody()?.string()?.take(300)?.replace("\n", " ")
                lastError = buildString {
                    append("OpenAI request failed (HTTP ").append(code).append(").")
                    if (!errorSnippet.isNullOrBlank()) append(" ").append(errorSnippet)
                }
                if (code !in 500..599 || attempt == maxAttempts - 1) {
                    Log.w(TAG, "chat failed code=$code")
                    return CompletionResult(error = lastError)
                }
            } catch (e: IOException) {
                lastError = "Network error while contacting OpenAI: ${e.localizedMessage ?: "connection failed"}."
                if (attempt == maxAttempts - 1) return CompletionResult(error = lastError)
            } catch (e: Exception) {
                SafeLog.e(TAG, "chat request failed", e)
                return CompletionResult(error = "Unexpected error while contacting OpenAI: ${e.localizedMessage ?: "unknown error"}.")
            }
            delay(500L * (attempt + 1))
        }
        return CompletionResult(error = lastError)
    }
}
