package io.github.kloeggaa.tickoff.api.models

data class Message(
    val role: String,
    val content: String
)

// OpenAI Chat Completions (v1/chat/completions)
data class ChatRequest(
    val model: String,
    val messages: List<Message>
)

data class ChatResponse(
    val choices: List<Choice> = emptyList()
)

data class Choice(
    val message: Message? = null
)

// Anthropic Messages API (v1/messages)
data class AnthropicMessageRequest(
    val model: String,
    val max_tokens: Int,
    val system: String? = null,
    val messages: List<Message>,
    /** "default" re-runs a refused request on Anthropic's recommended fallback model; null omits it. */
    val fallbacks: String? = null
)

data class AnthropicMessageResponse(
    val content: List<AnthropicContent> = emptyList(),
    val stop_reason: String? = null
)

data class AnthropicContent(
    val type: String,
    val text: String? = null
)

// Model list (GET v1/models), same shape at OpenAI and Anthropic
data class ModelList(
    val data: List<ModelEntry> = emptyList(),
    val has_more: Boolean? = null,
    val last_id: String? = null
)

data class ModelEntry(
    val id: String,
    val display_name: String? = null
)
