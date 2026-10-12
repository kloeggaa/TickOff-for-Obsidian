package io.github.kloeggaa.tickoff.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Wraps the device's [SpeechRecognizer] for continuous dictation: the recognizer stops after each
 * utterance, so in continuous mode it is reopened until [stopListening] is called. On-device
 * recognition is preferred (EXTRA_PREFER_OFFLINE).
 */
class SpeechToTextManager(private val context: Context) : RecognitionListener {
    companion object {
        private const val TAG = "TickOffSTT"

        /** Whether the recognizer is reopened after [error] (a `SpeechRecognizer.ERROR_*` code). */
        internal fun shouldRetryAfterError(error: Int, continuous: Boolean): Boolean = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_SERVER -> true
            SpeechRecognizer.ERROR_CLIENT -> continuous
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> false
            else -> true
        }
    }
    private val minSpeechLengthMs = 3_000L
    private val completeSilenceMs = 6_000L
    private val possibleSilenceMs = 5_000L
    private val endOfSpeechCommitDelayMs = 3_000L

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var shouldKeepListening = false
    private var keepContinuousListening = false
    private var startRequested = false
    private var latestPartialTranscript: String = ""
    private var resultDeliveredForCurrentUtterance: Boolean = false

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _lastError = MutableStateFlow("")
    val lastError: StateFlow<String> = _lastError

    var onResult: ((String) -> Unit)? = null
    var onPartialResult: ((String) -> Unit)? = null

    /** False when the device has no speech recognition service at all. */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(continuous: Boolean = false, resetTranscript: Boolean = true) {
        if (!isAvailable()) {
            _isListening.value = false
            _lastError.value = "Speech recognition is not available on this device"
            return
        }

        keepContinuousListening = continuous
        shouldKeepListening = true
        if (_isListening.value || startRequested) {
            return
        }
        startRequested = true
        if (resetTranscript) {
            _transcript.value = ""
        }
        latestPartialTranscript = ""
        resultDeliveredForCurrentUtterance = false

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechToTextManager)
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // On-device only: the online engine can lag and never deliver a final result.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, minSpeechLengthMs)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, completeSilenceMs)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, possibleSilenceMs)
        }
        speechRecognizer?.startListening(intent)
        mainHandler.postDelayed({ startRequested = false }, 250L)
    }

    fun stopListening() {
        // Only an active session needs stopping; calling it on an idle recognizer logs
        // "not connected to the recognition service" errors.
        val sessionActive = shouldKeepListening || startRequested || _isListening.value
        shouldKeepListening = false
        keepContinuousListening = false
        startRequested = false
        mainHandler.removeCallbacksAndMessages(null)
        if (sessionActive) speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun destroy() {
        shouldKeepListening = false
        keepContinuousListening = false
        startRequested = false
        mainHandler.removeCallbacksAndMessages(null)
        _isListening.value = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
        _lastError.value = ""
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        mainHandler.postDelayed({
            if (keepContinuousListening) return@postDelayed
            if (!resultDeliveredForCurrentUtterance) {
                commitPartialAsFinalIfAvailable()
            }
        }, endOfSpeechCommitDelayMs)
    }

    override fun onError(error: Int) {
        _isListening.value = false
        startRequested = false
        val isContinuousClientRestart =
            error == SpeechRecognizer.ERROR_CLIENT && shouldKeepListening && keepContinuousListening
        _lastError.value = if (isContinuousClientRestart) "" else mapError(error)
        Log.d(TAG, "onError code=$error")

        if (!keepContinuousListening &&
            !resultDeliveredForCurrentUtterance &&
            (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        ) {
            commitPartialAsFinalIfAvailable()
        }

        if (shouldKeepListening && shouldRetryAfterError(error, keepContinuousListening)) {
            scheduleRestart()
        }
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        startRequested = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            dispatchFinalResult(matches[0])
        } else {
            commitPartialAsFinalIfAvailable()
        }

        if (shouldKeepListening && keepContinuousListening) {
            scheduleRestart(delayMs = 180L)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partial = matches[0]
            _transcript.value = partial
            latestPartialTranscript = partial
            onPartialResult?.invoke(partial)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun scheduleRestart(delayMs: Long = 450L) {
        if (!shouldKeepListening) return
        val continuousMode = keepContinuousListening
        mainHandler.postDelayed(
            {
                if (shouldKeepListening) {
                    startListening(continuous = continuousMode, resetTranscript = false)
                }
            },
            delayMs
        )
    }

    private fun commitPartialAsFinalIfAvailable() {
        val partial = latestPartialTranscript.trim()
        if (partial.isNotEmpty()) {
            dispatchFinalResult(partial)
        }
    }

    private fun dispatchFinalResult(text: String) {
        if (resultDeliveredForCurrentUtterance) return
        val finalText = text.trim()
        if (finalText.isEmpty()) return
        resultDeliveredForCurrentUtterance = true
        _transcript.value = finalText
        onResult?.invoke(finalText)
    }

    private fun mapError(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side recognition error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing microphone permission"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Recognition error code: $error"
        }
    }
}
