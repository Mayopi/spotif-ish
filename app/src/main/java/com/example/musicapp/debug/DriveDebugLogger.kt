package com.example.musicapp.debug

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveDebugLogger @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val lock = Any()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS XXX")

    fun log(event: String, detail: String) {
        val line = "${timestamp()} | $event | $detail"
        Log.d(TAG, line)
        synchronized(lock) {
            runCatching {
                val file = logFile()
                if (file.exists() && file.length() > MAX_BYTES) {
                    file.writeText("")
                }
                file.parentFile?.mkdirs()
                file.appendText(line + "\n")
            }.onFailure { throwable ->
                Log.e(TAG, "Could not write drive debug log", throwable)
            }
        }
    }

    fun logError(event: String, throwable: Throwable) {
        log(event, "${throwable::class.java.simpleName}: ${throwable.message.orEmpty()}\n${Log.getStackTraceString(throwable)}")
    }

    fun path(): String = logFile().absolutePath

    private fun logFile(): File = File(context.filesDir, LOG_RELATIVE_PATH)

    private fun timestamp(): String = ZonedDateTime.now().format(formatter)

    private companion object {
        private const val TAG = "DriveDebugLogger"
        private const val LOG_RELATIVE_PATH = "logs/drive-debug.log"
        private const val MAX_BYTES = 256 * 1024L
    }
}
