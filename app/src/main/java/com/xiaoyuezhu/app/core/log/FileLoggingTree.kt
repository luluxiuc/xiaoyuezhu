package com.xiaoyuezhu.app.core.log

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(private val logDir: File) : Timber.DebugTree() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = when (priority) {
            Log.ERROR -> "E"
            Log.WARN -> "W"
            Log.INFO -> "I"
            Log.DEBUG -> "D"
            Log.VERBOSE -> "V"
            else -> "?"
        }
        val time = timeFormat.format(Date())
        val line = "$time $level/${tag ?: "APP"}: $message"
        AppLogger.pushToCrashQueue(line)
        try {
            val today = dateFormat.format(Date())
            val logFile = File(logDir, "app_$today.log")
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(line)
                t?.let {
                    writer.appendLine("  ${Log.getStackTraceString(it)}")
                }
            }
        } catch (_: Exception) { }
    }
}
