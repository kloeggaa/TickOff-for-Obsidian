package io.github.kloeggaa.tickoff.speech

import android.speech.SpeechRecognizer
import io.github.kloeggaa.tickoff.speech.SpeechToTextManager.Companion.shouldRetryAfterError
import io.github.kloeggaa.tickoff.speech.SpeechToTextManager.Companion.shouldStopStreaming
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechRestartRulesTest {

    @Test
    fun emptyOrSlowRecognitionRestarts() {
        listOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_CLIENT
        ).forEach { error ->
            assertTrue("error $error", shouldRetryAfterError(error))
        }
    }

    @Test
    fun missingPermissionNeverRestarts() {
        assertFalse(shouldRetryAfterError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
    }

    @Test
    fun unknownAndAudioErrorsRestart() {
        // Known, accepted: a muted laptop mic on the emulator produces a fast restart loop (code 11).
        assertTrue(shouldRetryAfterError(SpeechRecognizer.ERROR_AUDIO))
        assertTrue(shouldRetryAfterError(11))
    }

    @Test
    fun unavailableLanguageStopsStreamingAtOnce() {
        assertTrue(shouldStopStreaming(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, failuresWithoutSpeech = 1))
        assertTrue(shouldStopStreaming(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, failuresWithoutSpeech = 1))
    }

    @Test
    fun streamingSurvivesOccasionalFailures() {
        assertFalse(shouldStopStreaming(SpeechRecognizer.ERROR_NO_MATCH, failuresWithoutSpeech = 1))
        assertFalse(shouldStopStreaming(SpeechRecognizer.ERROR_SERVER, failuresWithoutSpeech = 2))
    }

    @Test
    fun repeatedFailuresWithoutSpeechStopStreaming() {
        assertTrue(shouldStopStreaming(SpeechRecognizer.ERROR_NO_MATCH, failuresWithoutSpeech = 3))
    }
}
