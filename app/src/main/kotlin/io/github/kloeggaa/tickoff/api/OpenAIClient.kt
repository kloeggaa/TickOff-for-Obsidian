package io.github.kloeggaa.tickoff.api

import android.util.Log
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
                Log.e(TAG, "chat request failed", e)
                return CompletionResult(error = "Unexpected error while contacting OpenAI: ${e.localizedMessage ?: "unknown error"}.")
            }
            delay(500L * (attempt + 1))
        }
        return CompletionResult(error = lastError)
    }
}
