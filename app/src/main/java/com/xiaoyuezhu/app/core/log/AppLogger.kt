package com.xiaoyuezhu.app.core.log

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val LOG_DIR = "logs"
    private const val MAX_DAYS = 7
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024L

    private var appContext: Context? = null
    private val crashLogQueue = mutableListOf<String>()

    fun init(context: Context) {
        appContext = context.applicationContext
        val logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) logDir.mkdirs()
        // Clear ALL old logs on startup for clean debugging
        logDir.listFiles()?.forEach { it.delete() }
        cleanupOldLogs(logDir)
        Timber.plant(FileLoggingTree(logDir))
        Timber.i("日志系统初始化完成 (旧日志已清除)")
    }

    fun getCrashLogs(): List<File> {
        val logDir = File(appContext?.filesDir ?: return emptyList(), LOG_DIR)
        return logDir.listFiles()?.filter { it.name.startsWith("crash_") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteCrashLog(file: File) {
        file.delete()
    }

    fun captureCrash(throwable: Throwable) {
        val ctx = appContext ?: return
        val logDir = File(ctx.filesDir, LOG_DIR)
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val crashFile = File(logDir, "crash_$timestamp.log")
        try {
            FileOutputStream(crashFile).use { fos ->
                fos.write("=== CRASH REPORT ===\n".toByteArray())
                fos.write("Time: $timestamp\n".toByteArray())
                fos.write("App Version: 1.0.0\n\n".toByteArray())
                fos.write("=== DEVICE INFO ===\n".toByteArray())
                fos.write("Manufacturer: ${android.os.Build.MANUFACTURER}\n".toByteArray())
                fos.write("Model: ${android.os.Build.MODEL}\n".toByteArray())
                fos.write("SDK: ${android.os.Build.VERSION.SDK_INT}\n\n".toByteArray())
                fos.write("=== STACK TRACE ===\n".toByteArray())
                fos.write(Log.getStackTraceString(throwable).toByteArray())
                fos.write("\n\n".toByteArray())
                fos.write("=== RECENT LOGS ===\n".toByteArray())
                crashLogQueue.takeLast(200).forEach { line ->
                    fos.write("$line\n".toByteArray())
                }
            }
            Timber.e(throwable, "应用崩溃，报告已保存: ${crashFile.name}")
        } catch (e: Exception) {
            Timber.e(e, "写入崩溃日志失败")
        }
    }

    fun getLogExportIntent(context: Context): Intent {
        val logDir = File(context.filesDir, LOG_DIR)
        val exportFile = File(context.cacheDir, "logs_export.txt")
        FileOutputStream(exportFile).use { fos ->
            logDir.listFiles()?.filter { it.name.startsWith("app_") && !it.name.startsWith("crash_") }
                ?.sortedByDescending { it.lastModified() }?.forEach { file ->
                    fos.write("=== ${file.name} ===\n".toByteArray())
                    fos.write(file.readBytes())
                    fos.write("\n\n".toByteArray())
                }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun cleanupOldLogs(logDir: File) {
        val cutoff = System.currentTimeMillis() - MAX_DAYS * 24 * 60 * 60 * 1000L
        logDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
                Timber.d("清理过期日志: ${file.name}")
            }
        }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayLog = File(logDir, "app_$todayStr.log")
        if (todayLog.exists() && todayLog.length() > MAX_FILE_SIZE) {
            todayLog.delete()
            Timber.d("日志文件超过5MB，已重置")
        }
    }

    internal fun pushToCrashQueue(line: String) {
        crashLogQueue.add(line)
        if (crashLogQueue.size > 300) {
            crashLogQueue.removeAt(0)
        }
    }
}
