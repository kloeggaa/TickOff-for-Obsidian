package io.github.kloeggaa.tickoff

import android.util.Log

/**
 * Warning/error logging without user data in release builds. Exception messages and stack traces
 * can contain document URIs (vault and file paths), so release builds log only the exception type;
 * debug builds keep the full exception.
 */
object SafeLog {
    fun w(tag: String, message: String, e: Throwable) {
        if (BuildConfig.DEBUG) Log.w(tag, message, e) else Log.w(tag, "$message (${e.javaClass.name})")
    }

    fun e(tag: String, message: String, e: Throwable) {
        if (BuildConfig.DEBUG) Log.e(tag, message, e) else Log.e(tag, "$message (${e.javaClass.name})")
    }
}
