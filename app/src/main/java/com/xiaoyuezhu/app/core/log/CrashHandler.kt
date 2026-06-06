package com.xiaoyuezhu.app.core.log

import timber.log.Timber

class CrashHandler(private val defaultHandler: Thread.UncaughtExceptionHandler?) :
    Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        AppLogger.captureCrash(throwable)
        Timber.e(throwable, "未捕获的异常: ${throwable.message}")
        defaultHandler?.uncaughtException(thread, throwable)
        if (defaultHandler == null) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    companion object {
        fun setup() {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(current))
        }
    }
}
