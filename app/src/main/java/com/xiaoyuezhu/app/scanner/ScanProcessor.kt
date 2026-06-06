package com.xiaoyuezhu.app.scanner

import androidx.camera.core.ImageProxy
import com.xiaoyuezhu.app.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.opencv.core.Core
import org.opencv.core.Mat
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sealed result types — public API, unchanged from original for UI compatibility.
 */
sealed class ScanFrameResult {
    data class Searching(val borderFound: Boolean, val triggerProgress: Int) : ScanFrameResult()
    data class Success(val studentId: String, val studentAnswerJson: String) : ScanFrameResult()
    data class Calibrated(val masterJson: String, val correctAnswerJson: String) : ScanFrameResult()
    data class Error(val message: String) : ScanFrameResult()
    object Processing : ScanFrameResult()
}

/**
 * Central orchestrator for answer sheet scanning.
 *
 * Two modes:
 * - **Calibration**: scan a master sheet (correct answers filled, ID=00)
 *   → auto-discover bubble positions → detect fills → save MasterTemplate
 * - **Scanning**: scan a student sheet → use MasterTemplate positions → measure fills
 *   → classify using OMRChecker-style "largest gap" thresholding → return answers
 */
@Singleton
class ScanProcessor @Inject constructor() {

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    // ── Mode state ──
    private var isCalibrationMode = false
    private var isProcessing = false
    private var frameCount = 0
    private var cooldownFrames = 0   // prevent immediate re-trigger after scan

    // ── Template state ──
    private var templateJson: TemplateJson? = null
    private var masterTemplate: MasterTemplate? = null

    // ── Pipeline components ──
    private val stabilizer = FrameStabilizer(requiredFrames = 8, positionTolerancePx = 25.0)
    private var targetAR = 1.414  // A4 ratio default

    // ── Public API ──

    /** Enter calibration mode. Template provides expected counts. */
    fun setCalibrationMode(template: TemplateJson) {
        this.templateJson = template
        this.isCalibrationMode = true
        this.masterTemplate = null
        targetAR = template.canvasWidth.toDouble() / template.canvasHeight.toDouble()
        reset()
        Timber.i("校准模式: ${template.canvasWidth}x${template.canvasHeight}, " +
                "${template.questionCount}题/${template.optionCount}选项")
    }

    /** Enter scan mode with a calibrated master template. */
    fun setScanMode(template: TemplateJson, master: MasterTemplate) {
        this.templateJson = template
        this.masterTemplate = master
        this.isCalibrationMode = false
        targetAR = if (master.warpWidth > 0 && master.warpHeight > 0) {
            master.warpWidth.toDouble() / master.warpHeight.toDouble()
        } else {
            template.canvasWidth.toDouble() / template.canvasHeight.toDouble()
        }
        reset()
        Timber.i("扫描模式: master ${master.questions.size}题, warp=${master.warpWidth}x${master.warpHeight}")
    }

    /** Process one camera frame. Non-blocking — returns quickly if already processing. */
    fun processFrame(imageProxy: ImageProxy): ScanFrameResult {
        if (isProcessing) return ScanFrameResult.Processing
        // Cooldown after each scan to prevent immediate re-triggering
        if (cooldownFrames > 0) {
            cooldownFrames--
            return ScanFrameResult.Searching(false, 0)
        }
        frameCount++

        // Convert frame to grayscale
        val gray = ScanFrameConverter.toGray(imageProxy) ?: run {
            return ScanFrameResult.Searching(false, stabilizer.progress())
        }

        try {
            // Find page border
            val quad = PageFinder.find(gray, targetAR)

            // Check stability
            val state = stabilizer.evaluate(quad)

            // Log progress periodically
            if (frameCount % 15 == 0 && quad.found) {
                Timber.d("帧#$frameCount: 边框=✓ 稳定=${stabilizer.progress()}/8")
            }

            if (state == FrameStabilizer.State.TRIGGERED && templateJson != null) {
                isProcessing = true
                Timber.i("触发! 稳定${stabilizer.progress()}帧")

                val result = try {
                    if (isCalibrationMode) {
                        runCalibration(gray, quad)
                    } else {
                        runScan(gray, quad)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "处理失败")
                    ScanFrameResult.Error(e.message ?: "处理异常")
                }
                return result
            }

            return ScanFrameResult.Searching(quad.found, stabilizer.progress())
        } finally {
            gray.release()
        }
    }

    /** Reset for next scan cycle */
    fun reset() {
        isProcessing = false
        frameCount = 0
        cooldownFrames = 30   // ~1s cooldown at 30fps
        stabilizer.reset()
    }

    // ── Calibration pipeline ──

    private fun runCalibration(gray: Mat, quad: PageFinder.QuadResult): ScanFrameResult {
        val t = templateJson!!

        // Warp to normalized size
        val warpResult = PerspectiveTransformer.warp(gray, quad, targetAR)
        val warped = warpResult.warped

        try {
            // Auto-discover bubbles + analyze fills → MasterTemplate
            val calOutput = CalibrationProcessor.calibrate(
                warped = warped,
                optionCount = t.optionCount,
                studentIdDigits = t.studentIdDigits,
                expectedQuestions = t.questionCount,
                questionsPerRow = t.questionsPerRow,
                warpWidth = warpResult.dstWidth,
                warpHeight = warpResult.dstHeight
            )

            if (calOutput == null) {
                Timber.e("校准失败: 未找到足够的填涂气泡")
                isProcessing = false
                return ScanFrameResult.Error("校准失败：未检测到足够的填涂。请确保标准卷已正确填写，并在光线充足的环境下重试。")
            }

            val masterJsonStr = json.encodeToString(calOutput.master)
            val correctJsonStr = json.encodeToString(calOutput.correctAnswers)

            Timber.i("校准成功: ${calOutput.master.questions.size}题, " +
                    "${calOutput.master.idColumns.size}个学号列, " +
                    "全局阈值=%.1f".format(calOutput.master.globalThreshold))

            return ScanFrameResult.Calibrated(masterJsonStr, correctJsonStr)
        } finally {
            warped.release()
        }
    }

    // ── Scan pipeline ──

    private fun runScan(gray: Mat, quad: PageFinder.QuadResult): ScanFrameResult {
        val m = masterTemplate
            ?: return ScanFrameResult.Error("未加载母版模板，请先校准")

        // Warp to EXACT same dimensions as calibration
        val warped = if (m.warpWidth > 0 && m.warpHeight > 0) {
            PerspectiveTransformer.warpToSize(gray, quad, m.warpWidth, m.warpHeight)
        } else {
            PerspectiveTransformer.warp(gray, quad, targetAR).warped
        }

        try {
            // Invert for measurement (dark=filled=high value)
            val inverted = Mat()
            Core.bitwise_not(warped, inverted)

            // Measure each bubble at master positions
            val idMeasurements = mutableListOf<List<Double>>()
            for (col in m.idColumns) {
                val colMeasures = col.circles.map { c ->
                    FillAnalyzer.circleMean(inverted, c.cx, c.cy, c.r * 0.7f)
                }
                idMeasurements.add(colMeasures)
            }

            val answerMeasurements = mutableListOf<List<Double>>()
            for (q in m.questions) {
                val qMeasures = q.options.map { c ->
                    FillAnalyzer.circleMean(inverted, c.cx, c.cy, c.r * 0.7f)
                }
                answerMeasurements.add(qMeasures)
            }
            inverted.release()

            // Read student ID: per column, find the digit with max intensity above threshold
            val studentId = readStudentId(idMeasurements)

            // Classify answers using calibration thresholds (with per-sheet re-derivation for robustness)
            val answerFills = FillAnalyzer.classify(
                answerMeasurements, m.rowThresholds, m.globalThreshold,
                rederivePerSheet = true
            )

            // Build answer output
            val answers = answerFills.mapIndexed { qi, fills ->
                val masterQ = m.questions.getOrNull(qi)
                val isMultiSelect = (masterQ?.allCorrect?.size ?: 1) > 1
                val selected = if (isMultiSelect) {
                    // Multi-select: collect ALL filled options
                    fills.indices.filter { fills[it] }.map { idx ->
                        m.optionLabels.getOrElse(idx) { ('A' + idx).toString() }
                    }
                } else {
                    // Single-select: over-selection check
                    val filledCount = fills.count { it }
                    if (filledCount > 1) {
                        // Student filled multiple options on a single-select question → wrong
                        Timber.d("  题${qi + 1}: 单选涂多(填了${filledCount}个) → 判错")
                        emptyList()
                    } else {
                        val maxIdx = fills.indices.maxByOrNull { answerMeasurements[qi][it] } ?: 0
                        if (fills[maxIdx]) {
                            val label = m.optionLabels.getOrElse(maxIdx) { ('A' + maxIdx).toString() }
                            listOf(label)
                        } else {
                            emptyList()
                        }
                    }
                }
                AnswerItemJson(qi, selected)
            }

            Timber.i("扫描完成: 学号=$studentId, " +
                    "已答=${answers.count { it.selectedOptions.isNotEmpty() }}/${answers.size}")

            val studentAnswer = StudentAnswerJson(studentId, answers)
            return ScanFrameResult.Success(studentId, json.encodeToString(studentAnswer))
        } finally {
            warped.release()
        }
    }

    /**
     * Read student ID from measured column intensities.
     * For each column, find the digit (0-9) whose bubble is darkest.
     * Uses per-column "largest gap" for threshold.
     */
    private fun readStudentId(
        measurements: List<List<Double>>
    ): String {
        val sb = StringBuilder()

        for (colValues in measurements) {
            if (colValues.isEmpty()) { sb.append("?"); continue }

            val gapResult = FillAnalyzer.findLargestGap(colValues.toDoubleArray())

            // Find the darkest digit
            val maxIdx = colValues.indices.maxByOrNull { colValues[it] } ?: 0
            val maxVal = colValues[maxIdx]

            // Check: is the max value above threshold AND clearly separated?
            val isFilled = if (gapResult.hadClearSeparation) {
                maxVal > gapResult.threshold
            } else {
                // No clear gap — use simple relative check
                val avg = colValues.average()
                maxVal > avg * 1.3 && maxVal > 15.0
            }

            sb.append(if (isFilled) maxIdx.toString() else "?")
        }

        return sb.toString()
    }
}
