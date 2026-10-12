package io.github.kloeggaa.tickoff.api

import io.github.kloeggaa.tickoff.api.models.Message
import io.github.kloeggaa.tickoff.data.AppSettings
import kotlinx.coroutines.flow.first

/**
 * Single entry point for AI calls. Reads the provider, model and API key from Settings on every
 * call and routes to [OpenAIClient] or [AnthropicClient].
 */
class AiProviderClient(private val settings: AppSettings) {

    private suspend fun isAnthropic(): Boolean = settings.aiProviderFlow.first() == AppSettings.PROVIDER_ANTHROPIC

    /** User-facing provider name, e.g. for "Add a Claude API key…". */
    suspend fun providerName(): String = if (isAnthropic()) "Claude" else "OpenAI"

    private suspend fun apiKey(): String =
        (if (isAnthropic()) settings.anthropicApiKeyFlow.first() else settings.openAiApiKeyFlow.first())
            ?.trim().orEmpty()

    suspend fun hasApiKey(): Boolean = apiKey().isNotBlank()

    suspend fun getCompletion(messages: List<Message>): CompletionResult {
        val key = apiKey()
        if (key.isBlank()) return CompletionResult(error = "Add your ${providerName()} API key in Settings first.")
        return if (isAnthropic()) {
            AnthropicClient(key).getCompletion(settings.anthropicModelFlow.first(), messages)
        } else {
            OpenAIClient(key).getCompletion(settings.openAiModelFlow.first(), messages)
        }
    }
}
