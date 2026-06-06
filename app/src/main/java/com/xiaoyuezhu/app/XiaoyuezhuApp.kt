package com.xiaoyuezhu.app

import android.app.Application
import com.xiaoyuezhu.app.core.log.AppLogger
import com.xiaoyuezhu.app.core.log.CrashHandler
import com.xiaoyuezhu.app.domain.repository.GradeRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class XiaoyuezhuApp : Application() {

    @Inject
    lateinit var gradeRepository: GradeRepository

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        CrashHandler.setup()

        // Initialize OpenCV native library
        try {
            System.loadLibrary("opencv_java4")
            Timber.i("OpenCV 原生库加载成功")
        } catch (e: UnsatisfiedLinkError) {
            Timber.e(e, "OpenCV 原生库加载失败")
        }

        // Data integrity check on startup
        appScope.launch {
            try {
                gradeRepository.checkIntegrity()
            } catch (e: Exception) {
                Timber.e(e, "启动数据完整性检查失败")
            }
        }
    }
}
