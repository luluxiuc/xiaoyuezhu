package com.xiaoyuezhu.app.scanner

import com.xiaoyuezhu.app.domain.model.*
import org.opencv.core.Mat
import timber.log.Timber

/**
 * Orchestrates the calibration pipeline:
 * warped calibration sheet → auto-discover bubble positions → analyze fills → build MasterTemplate.
 *
 * The MasterTemplate stores ALL circle positions (filled and empty) so the scan phase
 * can measure every bubble on student sheets at exactly the right positions.
 */
object CalibrationProcessor {

    data class CalibrationOutput(
        val master: MasterTemplate,
        val correctAnswers: List<AnswerItemJson>
    )

    /**
     * Run full calibration on a warped calibration sheet.
     *
     * @param warped grayscale Mat, already perspective-corrected
     * @param optionCount e.g., 4 for A/B/C/D
     * @param studentIdDigits number of ID columns (typically 2)
     * @param expectedQuestions total number of questions defined in the template
     * @param questionsPerRow questions per visual row (from LayoutEngine, default 2)
     * @param warpWidth width of the warped image (stored for scan consistency)
     * @param warpHeight height of the warped image
     * @return CalibrationOutput with MasterTemplate and detected correct answers, or null on failure
     */
    fun calibrate(
        warped: Mat,
        optionCount: Int,
        studentIdDigits: Int,
        expectedQuestions: Int,
        questionsPerRow: Int = 2,
        warpWidth: Int,
        warpHeight: Int
    ): CalibrationOutput? {
        Timber.i("CalibrationProcessor: 开始校准 ${warpWidth}x${warpHeight}")

        // Step 1: Discover all bubble positions
        val layout = BubbleDetector.discover(
            warped, optionCount, studentIdDigits,
            expectedQuestions, questionsPerRow
        )
        if (layout == null) {
            Timber.e("校准失败: BubbleDetector 未能发现气泡位置")
            return null
        }

        Timber.i("发现: ${layout.idColumns.size}个学号列, ${layout.questionRows.size}个答案行")

        // Step 2: Measure fill levels using FillAnalyzer
        val allRows = mutableListOf<List<CirclePos>>()

        // ID columns are treated as separate row-groups for per-column threshold
        for (col in layout.idColumns) {
            allRows.add(col)
        }

        // Answer rows
        for (row in layout.questionRows) {
            allRows.add(row)
        }

        val fillResult = FillAnalyzer.analyze(warped, allRows)

        // Step 3: Build ID columns with fill states
        val idColumns = mutableListOf<IdColumn>()
        var rowIdx = 0
        for (colIdx in 0 until layout.idColumns.size) {
            if (rowIdx >= fillResult.rows.size) break
            val rowFill = fillResult.rows[rowIdx]
            val circles = rowFill.bubbles.map { bf ->
                CirclePos(bf.cx, bf.cy, bf.r, intensity = bf.meanIntensity)
            }
            idColumns.add(IdColumn(circles))
            rowIdx++
        }

        // Step 4: Build questions with correct option detection
        val questions = mutableListOf<QuestionMaster>()
        val optionLabels = ('A'..'Z').take(optionCount).map { it.toString() }

        for (qrIdx in layout.questionRows.indices) {
            if (rowIdx >= fillResult.rows.size) break
            val rowFill = fillResult.rows[rowIdx]
            val bubbles = rowFill.bubbles

            // Split row's bubbles into individual questions
            // Each row has questionsPerRow * optionCount bubbles
            val perQuestion = optionCount
            for (qi in 0 until questionsPerRow) {
                val start = qi * perQuestion
                val end = start + perQuestion
                if (start >= bubbles.size) break
                val qBubbles = bubbles.subList(start, end.coerceAtMost(bubbles.size))

                val options = qBubbles.map { bf ->
                    CirclePos(bf.cx, bf.cy, bf.r, intensity = bf.meanIntensity)
                }

                // The filled option = the correct answer
                val filledIdx = qBubbles.indexOfFirst { it.isFilled }
                val correctOption = if (filledIdx >= 0) filledIdx else {
                    // If none clearly filled, pick the darkest
                    qBubbles.indices.maxByOrNull { qBubbles[it].meanIntensity } ?: 0
                }

                val qIndex = qrIdx * questionsPerRow + qi
                if (qIndex < expectedQuestions) {
                    questions.add(QuestionMaster(qIndex, options, correctOption))
                }
            }
            rowIdx++
        }

        // Step 5: Extract row thresholds for answer rows
        val answerRowThresholds = mutableListOf<Double>()
        for (i in layout.idColumns.size until fillResult.rowThresholds.size) {
            answerRowThresholds.add(fillResult.rowThresholds[i])
        }

        // Step 6: Build correct answers
        val correctAnswers = questions.map { q ->
            AnswerItemJson(q.index, listOf(optionLabels[q.correctOption]))
        }

        // Step 7: Build MasterTemplate
        val master = MasterTemplate(
            version = 2,
            canvasWidth = warpWidth,
            canvasHeight = warpHeight,
            studentIdDigits = studentIdDigits,
            idColumns = idColumns,
            questions = questions,
            optionLabels = optionLabels,
            globalThreshold = fillResult.globalThreshold,
            rowThresholds = answerRowThresholds,
            warpWidth = warpWidth,
            warpHeight = warpHeight
        )

        Timber.i("校准完成: ${questions.size}题, ${idColumns.size}学号列, " +
                "全局阈值=%.1f".format(fillResult.globalThreshold))

        return CalibrationOutput(master, correctAnswers)
    }
}
