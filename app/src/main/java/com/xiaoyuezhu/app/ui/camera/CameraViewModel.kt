package com.xiaoyuezhu.app.ui.camera

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyuezhu.app.domain.model.MasterTemplate
import com.xiaoyuezhu.app.domain.model.TemplateJson
import com.xiaoyuezhu.app.domain.repository.ClassRepository
import com.xiaoyuezhu.app.domain.repository.GradeRepository
import com.xiaoyuezhu.app.domain.repository.PaperRepository
import com.xiaoyuezhu.app.domain.repository.SaveGradeResult
import com.xiaoyuezhu.app.scanner.ScanFrameResult
import com.xiaoyuezhu.app.scanner.ScanProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

enum class ScanStatus { IDLE, SEARCHING, PROCESSING, SUCCESS, CALIBRATED, DUPLICATE, ERROR }

data class CameraUiState(
    val status: ScanStatus = ScanStatus.IDLE,
    val classId: String = "",
    val paperId: String = "",
    val isCalibration: Boolean = false,
    val borderFound: Boolean = false,
    val triggerProgress: Int = 0,
    val studentId: String = "",
    val score: Double = 0.0,
    val totalScore: Double = 0.0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val blankCount: Int = 0,
    val errorMessage: String? = null,
    val duplicateGrade: Any? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val paperRepository: PaperRepository,
    private val classRepository: ClassRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    val scanProcessor = ScanProcessor()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var initDone = false

    /**
     * @param isCalibration true = teacher calibrating a filled master sheet
     */
    fun init(classId: String, paperId: String, isCalibration: Boolean = false) {
        if (initDone) return
        initDone = true
        _uiState.update { it.copy(classId = classId, paperId = paperId, isCalibration = isCalibration) }
        viewModelScope.launch {
            try {
                val paper = paperRepository.getPaperById(paperId)
                if (paper == null) {
                    _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = "答题卡不存在") }
                    return@launch
                }
                val template = json.decodeFromString<TemplateJson>(paper.templateJson)

                if (isCalibration) {
                    scanProcessor.setCalibrationMode(template)
                    _uiState.update { it.copy(status = ScanStatus.SEARCHING) }
                    Timber.i("校准模式: ${paper.title}")
                } else {
                    // Load master from paper
                    if (paper.masterJson.isBlank()) {
                        _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = "请先校准答题卡") }
                        return@launch
                    }
                    val master = json.decodeFromString<MasterTemplate>(paper.masterJson)
                    scanProcessor.setScanMode(template, master)
                    _uiState.update { it.copy(status = ScanStatus.SEARCHING) }
                    Timber.i("扫描模式: ${paper.title}, master ${master.questions.size}题")
                }
            } catch (e: Exception) {
                Timber.e(e, "初始化失败")
                _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = "初始化失败: ${e.message}") }
            }
        }
    }

    fun processFrame(imageProxy: ImageProxy) {
        if (_uiState.value.status != ScanStatus.SEARCHING) return

        when (val result = scanProcessor.processFrame(imageProxy)) {
            is ScanFrameResult.Searching -> {
                _uiState.update { it.copy(borderFound = result.borderFound, triggerProgress = result.triggerProgress) }
            }
            is ScanFrameResult.Calibrated -> {
                _uiState.update { it.copy(status = ScanStatus.PROCESSING) }
                handleCalibration(result.masterJson, result.correctAnswerJson)
            }
            is ScanFrameResult.Success -> {
                _uiState.update { it.copy(status = ScanStatus.PROCESSING) }
                handleScanResult(result.studentId, result.studentAnswerJson)
            }
            is ScanFrameResult.Error -> {
                _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = result.message) }
            }
            is ScanFrameResult.Processing -> {}
        }
    }

    private fun handleCalibration(masterJson: String, correctAnswerJson: String) {
        viewModelScope.launch {
            try {
                val st = _uiState.value
                val paper = paperRepository.getPaperById(st.paperId)
                    ?: run { _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = "答题卡不存在") }; return@launch }

                // Save master + updated correct answers
                val updated = paper.copy(masterJson = masterJson, correctAnswerJson = correctAnswerJson)
                paperRepository.savePaper(updated)
                Timber.i("校准已保存: ${paper.title}")
                _uiState.update { it.copy(status = ScanStatus.CALIBRATED) }
            } catch (e: Exception) {
                Timber.e(e, "保存校准失败")
                _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = "保存校准失败: ${e.message}") }
            }
        }
    }

    private fun handleScanResult(studentId: String, studentAnswerJson: String) {
        viewModelScope.launch {
            try {
                val st = _uiState.value
                val paper = paperRepository.getPaperById(st.paperId)
                    ?: run { _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = "答题卡不存在") }; return@launch }

                if (studentId.contains("?")) {
                    _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = "学号识别不完整，请重新扫描") }
                    return@launch
                }

                // Grade using the calibrated correct answers and per-question scores
                val template = try {
                    json.decodeFromString<TemplateJson>(paper.templateJson)
                } catch (_: Exception) { null }
                val qScores = template?.questionScores ?: emptyList()
                val total = template?.totalScore ?: 100.0

                val gradingResult = withContext(Dispatchers.Default) {
                    com.xiaoyuezhu.app.grader.GradeEngine.grade(
                        paper.correctAnswerJson, studentAnswerJson, total, qScores
                    )
                } ?: run { _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = "判分失败") }; return@launch }

                when (val saveResult = gradeRepository.saveGrade(
                    st.classId, st.paperId, paper.title, studentId, gradingResult.score, total,
                    studentAnswerJson, gradingResult.correctCount, gradingResult.wrongCount, gradingResult.blankCount
                )) {
                    is SaveGradeResult.Success -> _uiState.update {
                        it.copy(status = ScanStatus.SUCCESS, studentId = studentId,
                            score = gradingResult.score, totalScore = total,
                            correctCount = gradingResult.correctCount,
                            wrongCount = gradingResult.wrongCount,
                            blankCount = gradingResult.blankCount)
                    }
                    is SaveGradeResult.Conflict -> _uiState.update {
                        it.copy(status = ScanStatus.DUPLICATE, studentId = studentId,
                            score = gradingResult.score, totalScore = total,
                            correctCount = gradingResult.correctCount,
                            wrongCount = gradingResult.wrongCount,
                            blankCount = gradingResult.blankCount,
                            duplicateGrade = saveResult.existingGrade)
                    }
                    is SaveGradeResult.Error -> _uiState.update {
                        it.copy(status = ScanStatus.ERROR, errorMessage = saveResult.message)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "处理扫描结果失败")
                _uiState.update { it.copy(status = ScanStatus.ERROR, errorMessage = e.message) }
            }
        }
    }

    fun resetForNextScan() {
        scanProcessor.reset()
        _uiState.update { it.copy(status = ScanStatus.SEARCHING, studentId = "", score = 0.0,
            correctCount = 0, wrongCount = 0, blankCount = 0, errorMessage = null,
            duplicateGrade = null, borderFound = false, triggerProgress = 0) }
    }

    fun clearError() = resetForNextScan()
}
