package com.xiaoyuezhu.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val GRADING = "grading"
    const val SETTINGS = "settings"
    const val CLASS_DETAIL = "class_detail/{classId}"
    const val PAPER_EDITOR = "paper_editor/{paperId}"
    const val CAMERA_SCAN = "camera_scan/{classId}/{paperId}"
    const val CAMERA_CALIBRATE = "camera_calibrate/{paperId}"
    const val SCAN_RESULT = "scan_result/{classId}/{paperId}/{studentId}"
    const val EXAM_DETAIL = "exam_detail/{examId}"

    fun classDetail(classId: String) = "class_detail/$classId"
    fun paperEditor(paperId: String) = "paper_editor/$paperId"
    fun cameraScan(classId: String, paperId: String) = "camera_scan/$classId/$paperId"
    fun cameraCalibrate(paperId: String) = "camera_calibrate/$paperId"
    fun scanResult(classId: String, paperId: String, studentId: String) = "scan_result/$classId/$paperId/$studentId"
    fun examDetail(examId: String) = "exam_detail/$examId"
}
