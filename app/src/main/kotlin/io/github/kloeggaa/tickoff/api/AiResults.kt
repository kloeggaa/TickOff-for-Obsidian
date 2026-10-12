package io.github.kloeggaa.tickoff.api

/** Provider-neutral result shared by OpenAIClient and AnthropicClient. */
data class CompletionResult(
    val content: String? = null,
    val error: String? = null
)
