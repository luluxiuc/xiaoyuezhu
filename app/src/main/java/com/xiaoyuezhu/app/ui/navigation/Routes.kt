package com.xiaoyuezhu.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val GRADING = "grading"
    const val SETTINGS = "settings"
    const val CLASS_DETAIL = "class_detail/{classId}"
    const val PAPER_EDITOR = "paper_editor/{paperId}"
    const val CAMERA_SCAN = "camera_scan/{classId}/{paperId}"
    const val CAMERA_CALIBRATE = "camera_calibrate/{paperId}"
    const val SCAN_RESULT = "scan_result/{classId}/{paperId}/{studentId}?score={score}&total={total}&correct={correct}&wrong={wrong}&blank={blank}"
    const val EXAM_DETAIL = "exam_detail/{examId}"
    const val TUTORIAL = "tutorial"

    fun classDetail(classId: String) = "class_detail/$classId"
    fun paperEditor(paperId: String) = "paper_editor/$paperId"
    fun cameraScan(classId: String, paperId: String) = "camera_scan/$classId/$paperId"
    fun cameraCalibrate(paperId: String) = "camera_calibrate/$paperId"
    fun scanResult(classId: String, paperId: String, studentId: String,
                   score: Double = 0.0, total: Double = 0.0,
                   correct: Int = 0, wrong: Int = 0, blank: Int = 0
    ) = "scan_result/$classId/$paperId/$studentId?score=$score&total=$total&correct=$correct&wrong=$wrong&blank=$blank"
    fun examDetail(examId: String) = "exam_detail/$examId"
}
