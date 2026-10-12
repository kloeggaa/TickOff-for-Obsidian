package io.github.kloeggaa.tickoff.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicClientTest {

    @Test
    fun textBecomesContent() {
        val result = AnthropicClient.toCompletionResult("- [ ] A", "end_turn")
        assertEquals("- [ ] A", result.content)
        assertNull(result.error)
    }

    @Test
    fun emptyReplyExplainsWhy() {
        assertTrue(AnthropicClient.toCompletionResult("", "max_tokens").error!!.contains("length limit"))
        assertTrue(AnthropicClient.toCompletionResult(" ", "refusal").error!!.contains("declined"))
        assertTrue(AnthropicClient.toCompletionResult("", null).error!!.contains("empty"))
    }

    @Test
    fun serverFallbackOnlyForModelsThatSupportIt() {
        assertTrue(AnthropicClient.usesServerFallback(AnthropicClient.DEFAULT_MODEL))
        assertTrue(AnthropicClient.usesServerFallback("claude-sonnet-5-5"))
        assertFalse(AnthropicClient.usesServerFallback("claude-haiku-4-5"))
        assertFalse(AnthropicClient.usesServerFallback("claude-sonnet-4-6"))
    }
}
