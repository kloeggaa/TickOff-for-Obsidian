package io.github.kloeggaa.tickoff.speech

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.Locale

/**
 * Continuous dictation from [startListening] until [finish] or [cancel], using the device's
 * [SpeechRecognizer] with on-device recognition requested (EXTRA_PREFER_OFFLINE).
 *
 * Android 13+: TickOff records the microphone itself ([MicAudioStream]) and streams the audio into
 * one segmented recognizer session (EXTRA_AUDIO_SOURCE). Each pause delivers a segment while the
 * microphone stays open. If the session ends anyway it is reopened, and the audio recorded in the
 * meantime is passed on.
 *
 * Older versions, or when streaming fails: the recognizer opens the microphone itself, stops after
 * each utterance and is reopened until [finish]. Words said during a restart can be lost.
 */
class SpeechToTextManager(private val context: Context) : RecognitionListener {
    companion object {
        private const val TAG = "TickOffSTT"
        private const val STREAMING_RESTART_DELAY_MS = 100L
        private const val LEGACY_RESTART_DELAY_MS = 180L
        private const val FINISH_TIMEOUT_MS = 5_000L

        /** Whether the recognizer is reopened after [error] (a `SpeechRecognizer.ERROR_*` code). */
        internal fun shouldRetryAfterError(error: Int): Boolean =
            error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS

        /**
         * Whether streaming gives way to the recognizer's own microphone: the language isn't
         * available for app-supplied audio, or sessions keep failing without recognizing anything.
         * Streaming runs on Android 13+ only, where the API 31 error codes exist.
         */
        @SuppressLint("InlinedApi")
        internal fun shouldStopStreaming(error: Int, failuresWithoutSpeech: Int): Boolean =
            error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
                failuresWithoutSpeech >= 3
    }

    // Legacy mode only: how long the recognizer waits in silence before it ends an utterance.
    private val minSpeechLengthMs = 3_000L
    private val completeSilenceMs = 6_000L
    private val possibleSilenceMs = 5_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var mic: MicAudioStream? = null
    /** Read end of the pipe the current session reads from (streaming mode). */
    private var sessionAudio: ParcelFileDescriptor? = null

    private var active = false
    private var streaming = false
    private var sessionRunning = false
    private var pendingFinish: (() -> Unit)? = null
    private var failuresWithoutSpeech = 0
    private var latestPartial = ""

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _lastError = MutableStateFlow("")
    val lastError: StateFlow<String> = _lastError

    /** A finished utterance. */
    var onResult: ((String) -> Unit)? = null
    /** The utterance being spoken, recognized so far; replaced by [onResult] when it ends. */
    var onPartialResult: ((String) -> Unit)? = null

    /** False when the device has no speech recognition service at all. */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isAvailable()) {
            _lastError.value = "Speech recognition is not available on this device"
            return
        }
        if (active) return
        active = true
        pendingFinish = null
        failuresWithoutSpeech = 0
        latestPartial = ""
        _lastError.value = ""
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechToTextManager)
            }
        }
        streaming = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && startMic()
        startSession()
    }

    /**
     * Stops listening and calls [onDone] once the last words are recognized and delivered through
     * [onResult]. In streaming mode the recognizer first finishes the audio recorded up to now.
     */
    fun finish(onDone: () -> Unit) {
        if (!active) {
            onDone()
            return
        }
        Log.d(TAG, "finish streaming=$streaming")
        if (!streaming) {
            commitPartial()
            stopEverything()
            onDone()
            return
        }
        pendingFinish = onDone
        // The pipe closes once the buffered audio is written; the session then ends by itself.
        mic?.finish()
        mainHandler.postDelayed({
            Log.d(TAG, "finish timed out")
            commitPartial()
            completeFinish()
        }, FINISH_TIMEOUT_MS)
    }

    /** Stops at once; words not yet delivered are dropped. */
    fun cancel() {
        pendingFinish = null
        stopEverything()
    }

    fun destroy() {
        cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun startMic(): Boolean {
        val stream = MicAudioStream(context)
        if (!stream.start()) {
            Log.d(TAG, "own microphone unavailable, recognizer records itself")
            return false
        }
        mic = stream
        return true
    }

    private fun startSession() {
        if (!active) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // On-device only: the online engine can lag and never deliver a final result.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        }
        val stream = mic
        if (streaming && stream != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addAudioSource(intent, stream)
        } else {
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, minSpeechLengthMs)
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, completeSilenceMs)
            intent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                possibleSilenceMs
            )
        }
        Log.d(TAG, "startSession streaming=$streaming")
        sessionRunning = true
        recognizer?.startListening(intent)
    }

    /** One session for as long as audio arrives; it ends when [MicAudioStream] closes the pipe. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun addAudioSource(intent: Intent, stream: MicAudioStream) {
        val audio = stream.newPipe()
        closeSessionAudio()
        sessionAudio = audio
        intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audio)
        intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
        intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, MicAudioStream.SAMPLE_RATE)
        intent.putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
    }

    private fun scheduleSession() {
        val delay = if (streaming) STREAMING_RESTART_DELAY_MS else LEGACY_RESTART_DELAY_MS
        mainHandler.postDelayed({ if (active && !sessionRunning) startSession() }, delay)
    }

    private fun sessionEnded() {
        sessionRunning = false
        closeSessionAudio()
        when {
            pendingFinish != null -> completeFinish()
            active -> scheduleSession()
            else -> _isListening.value = false
        }
    }

    private fun completeFinish() {
        val done = pendingFinish ?: return
        pendingFinish = null
        stopEverything()
        done()
    }

    private fun stopEverything() {
        active = false
        mainHandler.removeCallbacksAndMessages(null)
        mic?.cancel()
        mic = null
        // Cancelling an idle recognizer logs "not connected to the recognition service" errors.
        if (sessionRunning) recognizer?.cancel()
        sessionRunning = false
        closeSessionAudio()
        latestPartial = ""
        _isListening.value = false
    }

    private fun closeSessionAudio() {
        try {
            sessionAudio?.close()
        } catch (_: IOException) {
        }
        sessionAudio = null
    }

    private fun deliver(text: String) {
        val cleaned = text.trim()
        latestPartial = ""
        if (cleaned.isEmpty()) return
        failuresWithoutSpeech = 0
        onResult?.invoke(cleaned)
    }

    /** The utterance in progress counts as said, e.g. when its session fails or is stopped. */
    private fun commitPartial() {
        if (latestPartial.isNotBlank()) deliver(latestPartial)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
        _isListening.value = true
        _lastError.value = ""
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
        // In streaming mode the microphone stays open across utterances.
        if (!streaming) _isListening.value = false
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partial = firstResult(partialResults).trim()
        if (partial.isEmpty()) return
        latestPartial = partial
        failuresWithoutSpeech = 0
        onPartialResult?.invoke(partial)
    }

    override fun onSegmentResults(segmentResults: Bundle) {
        val segment = firstResult(segmentResults)
        // Lengths only: the transcript itself stays out of the log.
        Log.d(TAG, "onSegmentResults chars=${segment.trim().length}")
        if (segment.isBlank()) commitPartial() else deliver(segment)
    }

    override fun onEndOfSegmentedSession() {
        Log.d(TAG, "onEndOfSegmentedSession")
        sessionEnded()
    }

    override fun onResults(results: Bundle?) {
        val result = firstResult(results)
        Log.d(TAG, "onResults chars=${result.trim().length}")
        if (result.isBlank()) commitPartial() else deliver(result)
        sessionEnded()
    }

    override fun onError(error: Int) {
        Log.d(TAG, "onError code=$error streaming=$streaming")
        sessionRunning = false
        closeSessionAudio()
        if (pendingFinish != null) {
            commitPartial()
            completeFinish()
            return
        }
        if (!active) return
        commitPartial()
        if (error != SpeechRecognizer.ERROR_CLIENT) _lastError.value = mapError(error)
        if (!shouldRetryAfterError(error)) {
            stopEverything()
            return
        }
        failuresWithoutSpeech += 1
        if (streaming && shouldStopStreaming(error, failuresWithoutSpeech)) {
            Log.d(TAG, "streaming failed, recognizer records itself")
            mic?.cancel()
            mic = null
            streaming = false
        }
        scheduleSession()
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun firstResult(bundle: Bundle?): String =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()

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
