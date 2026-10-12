package io.github.kloeggaa.tickoff.api

import android.util.Log
import io.github.kloeggaa.tickoff.api.models.AnthropicMessageRequest
import io.github.kloeggaa.tickoff.api.models.Message
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class AnthropicClient(private val apiKey: String) {
    companion object {
        private const val TAG = "TickOffAnthropic"
        /** Default Claude model for fresh installs; the user can change it in Settings. */
        const val DEFAULT_MODEL = "claude-opus-5-5"
        // Current models think before answering and those tokens count toward this cap.
        private const val MAX_TOKENS = 16000
        private const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
        // Models that accept the server-side `fallbacks: "default"` refusal fallback.
        private val FALLBACK_MODELS = setOf("claude-fable-5-1", "claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5")

        /** Whether [model] accepts the server-side `fallbacks: "default"` refusal fallback. */
        internal fun usesServerFallback(model: String): Boolean = model in FALLBACK_MODELS

        /** Joins the reply's text blocks (thinking blocks carry no text) and explains an empty reply. */
        internal fun toCompletionResult(content: String, stopReason: String?): CompletionResult {
            if (content.isNotBlank()) return CompletionResult(content = content)
            return CompletionResult(
                error = when (stopReason) {
                    "max_tokens" -> "Claude hit the reply length limit before writing an answer."
                    "refusal" -> "Claude declined to answer this request."
                    else -> "Anthropic returned an empty response."
                }
            )
        }
    }

    private val service: AnthropicService = Retrofit.Builder()
        .baseUrl("https://api.anthropic.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .dns(Ipv4PreferredDns)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        )
        .build()
        .create(AnthropicService::class.java)

    /** Checks the key and [model] with a model lookup; no text is generated or sent. */
    suspend fun checkKey(model: String): KeyCheckResult = try {
        val response = service.getModel(apiKey, model = model.ifBlank { DEFAULT_MODEL })
        response.body()?.close()
        response.errorBody()?.close()
        KeyCheckResult(KeyCheck.outcomeFor(response.code()), response.code())
    } catch (_: IOException) {
        KeyCheckResult(KeyCheckOutcome.NETWORK_ERROR)
    } catch (e: Exception) {
        Log.e(TAG, "key check failed", e)
        KeyCheckResult(KeyCheckOutcome.OTHER)
    }

    /** The models this key can use (newest first for Claude, chat models only for OpenAI). */
    suspend fun listModels(): ModelListResult = try {
        val response = service.listModels(apiKey)
        val ids = response.body()?.data.orEmpty().map { it.id }
        response.errorBody()?.close()
        ModelListResult(ids.distinct(), KeyCheckResult(KeyCheck.outcomeFor(response.code()), response.code()))
    } catch (_: IOException) {
        ModelListResult(emptyList(), KeyCheckResult(KeyCheckOutcome.NETWORK_ERROR))
    } catch (e: Exception) {
        Log.e(TAG, "model list failed", e)
        ModelListResult(emptyList(), KeyCheckResult(KeyCheckOutcome.OTHER))
    }

    suspend fun getCompletion(model: String, messages: List<Message>): CompletionResult {
        val resolvedModel = model.ifBlank { DEFAULT_MODEL }
        val useFallback = usesServerFallback(resolvedModel)
        var lastError = "Request failed."
        repeat(3) { attempt ->
            try {
                // Claude takes system prompts in a top-level field, not as a message role.
                val systemPrompt = messages
                    .filter { it.role == "system" }
                    .joinToString("\n") { it.content }
                    .ifBlank { null }
                val response = service.sendMessage(
                    apiKey,
                    beta = if (useFallback) FALLBACK_BETA else null,
                    request = AnthropicMessageRequest(
                        model = resolvedModel,
                        max_tokens = MAX_TOKENS,
                        system = systemPrompt,
                        messages = messages.filterNot { it.role == "system" },
                        fallbacks = if (useFallback) "default" else null
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val content = body?.content.orEmpty()
                        .filter { it.type == "text" }
                        .mapNotNull { it.text }
                        .joinToString("\n")
                        .trim()
                    return toCompletionResult(content, body?.stop_reason)
                }
                lastError = buildError(response.code(), response.errorBody()?.string())
                if (response.code() !in 500..599 || attempt == 2) return CompletionResult(error = lastError)
            } catch (e: IOException) {
                lastError = "Network error while contacting Anthropic: ${e.localizedMessage ?: "connection failed"}."
                if (attempt == 2) return CompletionResult(error = lastError)
            } catch (e: Exception) {
                Log.e(TAG, "message request failed", e)
                return CompletionResult(error = "Unexpected error while contacting Anthropic: ${e.localizedMessage ?: "unknown error"}.")
            }
            delay(500L * (attempt + 1))
        }
        return CompletionResult(error = lastError)
    }

    private fun buildError(code: Int, body: String?): String {
        return "Anthropic request failed (HTTP $code)." + body.orEmpty().take(300).replace("\n", " ").let {
            if (it.isBlank()) "" else " $it"
        }
    }
}
