package io.github.kloeggaa.tickoff.ui.record

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.SafeLog
import io.github.kloeggaa.tickoff.api.AiProviderClient
import io.github.kloeggaa.tickoff.api.models.Message
import io.github.kloeggaa.tickoff.data.AppSettings
import io.github.kloeggaa.tickoff.speech.SpeechToTextManager
import io.github.kloeggaa.tickoff.storage.TaskMarkdown
import io.github.kloeggaa.tickoff.storage.VaultFiles
import io.github.kloeggaa.tickoff.widget.TaskListWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dictation -> AI task extraction -> append to the task file. Speech is recognized on the device;
 * only the final transcript is sent to the AI provider the user configured.
 */
class TaskRecorderViewModel(
    private val appContext: Context,
    private val settings: AppSettings,
    private val sttManager: SpeechToTextManager
) : ViewModel() {
    private val aiClient = AiProviderClient(settings)

    companion object {
        private const val TAG = "TickOffRecorder"
    }

    val isListening: StateFlow<Boolean> = sttManager.isListening
    val lastError: StateFlow<String> = sttManager.lastError

    private val _recordingActive = MutableStateFlow(false)
    val recordingActive: StateFlow<Boolean> = _recordingActive

    private val _isExtractingTasks = MutableStateFlow(false)
    val isExtractingTasks: StateFlow<Boolean> = _isExtractingTasks

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText

    /** True when the status asks the user to fix something in Settings. */
    private val _needsSettings = MutableStateFlow(false)
    val needsSettings: StateFlow<Boolean> = _needsSettings

    private val _finalTranscript = MutableStateFlow("")
    val finalTranscript: StateFlow<String> = _finalTranscript

    private val _taskMarkdown = MutableStateFlow("")
    val taskMarkdown: StateFlow<String> = _taskMarkdown

    private var latestCapturedTranscript: String = ""
    private var accumulatedTranscript: String = ""
    private var captureSessionId: Long = 0L
    private var extractionRequestId: Long = 0L
    private var extractionAllowedByStop: Boolean = false

    fun startCapture() {
        if (_recordingActive.value) {
            return
        }
        if (!sttManager.isAvailable()) {
            _statusText.value = appContext.getString(R.string.record_no_recognizer)
            return
        }

        captureSessionId += 1L
        extractionAllowedByStop = false
        _taskMarkdown.value = ""
        _finalTranscript.value = ""
        _needsSettings.value = false
        latestCapturedTranscript = ""
        accumulatedTranscript = ""
        _statusText.value = ""

        sttManager.onPartialResult = { text -> onPartialTranscriptUpdated(text) }
        sttManager.onResult = { text -> onTranscriptCaptured(text) }

        sttManager.startListening(continuous = true)
        _recordingActive.value = true
    }

    fun stopCapture() {
        if (!_recordingActive.value) {
            return
        }
        extractionAllowedByStop = true
        val sessionAtStop = captureSessionId
        val requestId = extractionRequestId + 1L
        extractionRequestId = requestId
        _recordingActive.value = false
        sttManager.stopListening()
        sttManager.onPartialResult = null
        sttManager.onResult = null

        // A partial result that never became final still counts as what the user said.
        val transcriptToExtract = latestCapturedTranscript
            .ifBlank { _finalTranscript.value.trim() }

        if (transcriptToExtract.isBlank()) {
            _statusText.value = appContext.getString(R.string.record_no_speech)
            return
        }

        _finalTranscript.value = transcriptToExtract
        extractTasksFromTranscript(transcriptToExtract, requestId, sessionAtStop)
    }

    fun onScreenClosed() {
        cancelCapture()
    }

    private fun onTranscriptCaptured(text: String) {
        val cleaned = text.trim()
        if (cleaned.isBlank()) return
        accumulatedTranscript = TranscriptMerge.merge(accumulatedTranscript, cleaned)
        latestCapturedTranscript = accumulatedTranscript
        _finalTranscript.value = accumulatedTranscript
    }

    private fun onPartialTranscriptUpdated(text: String) {
        val cleaned = text.trim()
        if (cleaned.isBlank()) return
        _finalTranscript.value = TranscriptMerge.merge(accumulatedTranscript, cleaned)
    }

    private fun extractTasksFromTranscript(cleaned: String, requestId: Long, sessionAtStop: Long) {
        if (!extractionAllowedByStop || _recordingActive.value || _isExtractingTasks.value) {
            return
        }

        viewModelScope.launch {
            _isExtractingTasks.value = true
            try {
                val location = settings.taskFileLocation()
                if (location == null) {
                    _needsSettings.value = true
                    _statusText.value = appContext.getString(R.string.record_no_vault)
                    return@launch
                }
                if (!aiClient.hasApiKey()) {
                    _needsSettings.value = true
                    _statusText.value = appContext.getString(R.string.record_no_key, aiClient.providerName())
                    return@launch
                }

                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val extractionPrompt = """
                    You extract actionable tasks from voice transcripts.
                    Today's date on the phone is $currentDate.

                    Rules:
                    - Convert transcript into clear task items.
                    - Resolve relative dates using today's date.
                    - Output markdown lines only.
                    - Use this format when a date is present:
                      - [ ] This is the task 📅 YYYY-MM-DD
                    - Use this format when no date is mentioned:
                      - [ ] This is the task
                    - Do not invent a date when the transcript does not mention one.
                    - Do not add headings, explanations, or code fences.
                """.trimIndent()

                val result = aiClient.getCompletion(
                    listOf(Message("system", extractionPrompt), Message("user", cleaned))
                )
                // A new recording started meanwhile: this result belongs to an old one.
                val shouldApplyResult = requestId == extractionRequestId &&
                    !_recordingActive.value &&
                    sessionAtStop == captureSessionId &&
                    extractionAllowedByStop
                if (!shouldApplyResult) return@launch

                if (result.content.isNullOrBlank()) {
                    _statusText.value = appContext.getString(
                        R.string.record_extract_failed,
                        result.error ?: "empty response"
                    )
                    return@launch
                }

                val markdown = TaskMarkdown.normalizeTaskMarkdown(result.content)
                _taskMarkdown.value = markdown
                val saved = withContext(Dispatchers.IO) {
                    VaultFiles.appendTasks(appContext, location, markdown)
                }
                if (saved) {
                    _statusText.value = appContext.getString(R.string.record_saved, location.relativePath)
                    withContext(Dispatchers.IO) { TaskListWidgetProvider.refreshAll(appContext) }
                } else {
                    _needsSettings.value = true
                    _statusText.value = appContext.getString(R.string.record_save_failed, location.relativePath)
                }
            } catch (e: Exception) {
                SafeLog.e(TAG, "extraction failed", e)
                _statusText.value = appContext.getString(R.string.record_extract_failed, e.localizedMessage ?: "error")
            } finally {
                _isExtractingTasks.value = false
            }
        }
    }

    private fun cancelCapture() {
        _recordingActive.value = false
        sttManager.stopListening()
        sttManager.onPartialResult = null
        sttManager.onResult = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelCapture()
        sttManager.destroy()
    }
}
