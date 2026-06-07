package com.xiaoyuezhu.app.scanner

import com.xiaoyuezhu.app.domain.model.*
import com.xiaoyuezhu.app.engine.LayoutEngine
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
     * @param optionCounts per-question option counts (for splitting rows into questions)
     * @param studentIdDigits number of ID columns (typically 2)
     * @param expectedQuestions total number of questions defined in the template
     * @param warpWidth width of the warped image (stored for scan consistency)
     * @param warpHeight height of the warped image
     * @return CalibrationOutput with MasterTemplate and detected correct answers, or null on failure
     */
    fun calibrate(
        warped: Mat,
        optionCounts: List<Int>,
        studentIdDigits: Int,
        expectedQuestions: Int,
        warpWidth: Int,
        warpHeight: Int
    ): CalibrationOutput? {
        Timber.i("CalibrationProcessor: 开始校准 ${warpWidth}x${warpHeight}")

        // Step 1: Discover all bubble positions
        val maxOptCount = optionCounts.maxOrNull() ?: 4
        val layout = BubbleDetector.discover(
            warped, maxOptCount, studentIdDigits,
            expectedQuestions, LayoutEngine.MAX_PER_ROW
        )
        if (layout == null) {
            Timber.e("校准失败: BubbleDetector 未能发现气泡位置")
            return null
        }

        Timber.i("发现: ${layout.idColumns.size}个学号列, ${layout.questionRows.size}个答案行")

        // Step 2: Measure fill levels using FillAnalyzer
        val allRows = mutableListOf<List<CirclePos>>()
        for (col in layout.idColumns) { allRows.add(col) }
        for (row in layout.questionRows) { allRows.add(row) }

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

        // Step 4: Build questions — split each row using sequential per-question option counts
        val questions = mutableListOf<QuestionMaster>()
        val optionLabels = ('A'..'Z').map { it.toString() }
        var questionIndex = 0

        for (qrIdx in layout.questionRows.indices) {
            if (rowIdx >= fillResult.rows.size) break
            val rowFill = fillResult.rows[rowIdx]
            val bubbles = rowFill.bubbles

            // Split this row's bubbles into questions by consuming per-question option counts
            var bubbleOffset = 0
            while (bubbleOffset < bubbles.size && questionIndex < expectedQuestions) {
                val oc = optionCounts.getOrElse(questionIndex) { 4 }
                val end = (bubbleOffset + oc).coerceAtMost(bubbles.size)
                if (end - bubbleOffset < 2) break  // not enough bubbles for a question

                val qBubbles = bubbles.subList(bubbleOffset, end)
                val options = qBubbles.map { bf ->
                    CirclePos(bf.cx, bf.cy, bf.r, intensity = bf.meanIntensity)
                }

                val filledIndices = qBubbles.indices.filter { qBubbles[it].isFilled }
                val correctOptions = if (filledIndices.isNotEmpty()) filledIndices
                else { listOf(qBubbles.indices.maxByOrNull { qBubbles[it].meanIntensity } ?: 0) }

                questions.add(QuestionMaster(questionIndex, options, correctOptions))
                bubbleOffset = end
                questionIndex++
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
            AnswerItemJson(q.index, q.allCorrect.map { optionLabels.getOrElse(it) { ('A' + it).toString() } })
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
