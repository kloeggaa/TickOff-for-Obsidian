package io.github.kloeggaa.tickoff.speech

import android.speech.SpeechRecognizer
import io.github.kloeggaa.tickoff.speech.SpeechToTextManager.Companion.shouldRetryAfterError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechRestartRulesTest {

    @Test
    fun emptyOrSlowRecognitionRestartsInBothModes() {
        listOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_SERVER
        ).forEach { error ->
            assertTrue("error $error continuous", shouldRetryAfterError(error, continuous = true))
            assertTrue("error $error single", shouldRetryAfterError(error, continuous = false))
        }
    }

    @Test
    fun clientErrorRestartsOnlyInContinuousMode() {
        assertTrue(shouldRetryAfterError(SpeechRecognizer.ERROR_CLIENT, continuous = true))
        assertFalse(shouldRetryAfterError(SpeechRecognizer.ERROR_CLIENT, continuous = false))
    }

    @Test
    fun missingPermissionNeverRestarts() {
        assertFalse(shouldRetryAfterError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, continuous = true))
        assertFalse(shouldRetryAfterError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, continuous = false))
    }

    @Test
    fun unknownAndAudioErrorsRestart() {
        // Known, accepted: a muted laptop mic on the emulator produces a fast restart loop (code 11).
        assertTrue(shouldRetryAfterError(SpeechRecognizer.ERROR_AUDIO, continuous = true))
        assertTrue(shouldRetryAfterError(11, continuous = true))
    }
}
