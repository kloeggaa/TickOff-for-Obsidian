package io.github.kloeggaa.tickoff.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelFilterTest {

    @Test
    fun keepsChatModelsOnly() {
        val ids = listOf(
            "gpt-5.6-luna", "gpt-4o-mini", "o3", "o4-mini", "chatgpt-4o-latest",
            "whisper-1", "tts-1", "gpt-4o-mini-tts", "gpt-4o-transcribe", "gpt-image-1",
            "dall-e-3", "text-embedding-3-small", "omni-moderation-latest",
            "gpt-4o-realtime-preview", "gpt-4o-audio-preview", "gpt-4o-search-preview",
            "gpt-3.5-turbo-instruct", "babbage-002", "davinci-002"
        )
        assertEquals(
            listOf("o4-mini", "o3", "gpt-5.6-luna", "gpt-4o-mini", "chatgpt-4o-latest"),
            ModelFilter.openAiChatModels(ids)
        )
    }

    @Test
    fun removesDuplicatesAndHandlesEmptyList() {
        assertEquals(listOf("gpt-4o"), ModelFilter.openAiChatModels(listOf("gpt-4o", "gpt-4o")))
        assertEquals(emptyList<String>(), ModelFilter.openAiChatModels(emptyList()))
    }

    @Test
    fun omniModerationIsNotAnOSeriesModel() {
        assertEquals(emptyList<String>(), ModelFilter.openAiChatModels(listOf("omni-moderation-2024-09-26")))
    }
}
