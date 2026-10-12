package io.github.kloeggaa.tickoff.api

/** Result of checking an API key together with the chosen model, without generating any text. */
enum class KeyCheckOutcome {
    /** Key accepted and the model exists for this key. */
    OK,
    /** Key rejected (wrong, revoked or mistyped). */
    INVALID_KEY,
    /** Key valid, but it may not use this model or API. */
    NO_ACCESS,
    /** Key accepted, but the model name is unknown. */
    MODEL_NOT_FOUND,
    /** Key accepted, but rate limit or quota reached. */
    RATE_LIMITED,
    SERVER_ERROR,
    NETWORK_ERROR,
    OTHER
}

data class KeyCheckResult(val outcome: KeyCheckOutcome, val httpCode: Int? = null)

object KeyCheck {
    /** Maps the HTTP status of `GET /v1/models/{model}` to an outcome; both providers use the same codes. */
    fun outcomeFor(httpCode: Int): KeyCheckOutcome = when (httpCode) {
        in 200..299 -> KeyCheckOutcome.OK
        401 -> KeyCheckOutcome.INVALID_KEY
        403 -> KeyCheckOutcome.NO_ACCESS
        404 -> KeyCheckOutcome.MODEL_NOT_FOUND
        429 -> KeyCheckOutcome.RATE_LIMITED
        in 500..599 -> KeyCheckOutcome.SERVER_ERROR
        else -> KeyCheckOutcome.OTHER
    }
}

data class ModelListResult(val models: List<String>, val check: KeyCheckResult)

object ModelFilter {
    private val OPENAI_NON_CHAT = listOf(
        "audio", "realtime", "tts", "transcribe", "whisper", "image", "dall-e",
        "search", "embedding", "moderation", "instruct", "codex"
    )

    /** OpenAI lists every model of the account; keep only chat models usable for task extraction. */
    fun openAiChatModels(ids: List<String>): List<String> = ids
        .filter { id -> id.startsWith("gpt-") || id.startsWith("chatgpt-") || Regex("^o\\d").containsMatchIn(id) }
        .filterNot { id -> OPENAI_NON_CHAT.any { id.contains(it) } }
        .distinct()
        .sortedDescending()
}
