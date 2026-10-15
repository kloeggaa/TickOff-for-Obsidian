package io.github.kloeggaa.tickoff.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import io.github.kloeggaa.tickoff.SafeLog
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread

/**
 * Records the microphone for a whole dictation and streams it as 16-bit mono PCM into a pipe that
 * the speech recognizer reads (EXTRA_AUDIO_SOURCE). Audio recorded while no recognizer is reading,
 * e.g. while a session restarts, is buffered and written to the next pipe, so nothing said is lost.
 * The audio stays in memory and is never stored.
 */
internal class MicAudioStream(private val context: Context) {
    companion object {
        private const val TAG = "TickOffMic"
        const val SAMPLE_RATE = 16_000
        private const val CHUNK_BYTES = SAMPLE_RATE * 2 / 10 // 100 ms
        private const val MAX_BUFFERED_CHUNKS = 600 // 60 s
        private val END_OF_AUDIO = ByteArray(0)
    }

    private val chunks = LinkedBlockingDeque<ByteArray>()
    private val lock = Object()
    private var output: OutputStream? = null
    @Volatile private var recording = false
    @Volatile private var cancelled = false

    /** Starts recording. False when the microphone can't be opened. */
    fun start(): Boolean {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return false
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, CHUNK_BYTES * 4)
            )
        } catch (e: Exception) {
            SafeLog.e(TAG, "AudioRecord failed", e)
            return false
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        record.startRecording()
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            record.release()
            return false
        }
        recording = true

        thread(name = "tickoff-mic-reader") {
            try {
                while (recording) {
                    val buffer = ByteArray(CHUNK_BYTES)
                    val read = record.read(buffer, 0, buffer.size)
                    if (read < 0) break
                    if (read == 0) continue
                    chunks.offerLast(if (read == buffer.size) buffer else buffer.copyOf(read))
                    while (chunks.size > MAX_BUFFERED_CHUNKS) chunks.pollFirst()
                }
            } finally {
                record.stop()
                record.release()
                chunks.offerLast(END_OF_AUDIO)
            }
        }
        thread(name = "tickoff-mic-writer") { writeLoop() }
        return true
    }

    /**
     * A new pipe for the next recognizer session; returns its read end for EXTRA_AUDIO_SOURCE.
     * The previous pipe, if any, is closed.
     */
    fun newPipe(): ParcelFileDescriptor {
        val (readEnd, writeEnd) = ParcelFileDescriptor.createPipe()
        synchronized(lock) {
            closeQuietly(output)
            output = ParcelFileDescriptor.AutoCloseOutputStream(writeEnd)
            lock.notifyAll()
        }
        return readEnd
    }

    /** Stops recording; the remaining audio is written, then the pipe closes and the session ends. */
    fun finish() {
        recording = false
    }

    /** Stops at once and drops buffered audio. */
    fun cancel() {
        recording = false
        cancelled = true
        chunks.clear()
        chunks.offerLast(END_OF_AUDIO)
        synchronized(lock) {
            closeQuietly(output)
            output = null
            lock.notifyAll()
        }
    }

    private fun writeLoop() {
        while (true) {
            val chunk = chunks.takeFirst()
            if (chunk === END_OF_AUDIO || cancelled) break
            while (true) {
                val out = awaitOutput() ?: return
                try {
                    out.write(chunk)
                    break
                } catch (_: IOException) {
                    // The recognizer closed its end; keep the chunk for the next session's pipe.
                    synchronized(lock) {
                        if (output === out) output = null
                    }
                    closeQuietly(out)
                }
            }
        }
        synchronized(lock) {
            closeQuietly(output)
            output = null
        }
    }

    private fun awaitOutput(): OutputStream? = synchronized(lock) {
        while (output == null && !cancelled) lock.wait()
        if (cancelled) null else output
    }

    private fun closeQuietly(stream: OutputStream?) {
        try {
            stream?.close()
        } catch (_: IOException) {
        }
    }
}
