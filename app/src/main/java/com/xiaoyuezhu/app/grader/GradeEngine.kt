package com.xiaoyuezhu.app.grader

import com.xiaoyuezhu.app.domain.model.AnswerItemJson
import com.xiaoyuezhu.app.domain.model.StudentAnswerJson
import kotlinx.serialization.json.Json
import timber.log.Timber

object GradeEngine {

    private val json = Json { ignoreUnknownKeys = true }

    data class GradingResult(
        val score: Double,
        val correctCount: Int,
        val wrongCount: Int,
        val blankCount: Int,
        val details: List<QuestionResult>
    )

    data class QuestionResult(
        val index: Int,
        val correctAnswer: List<String>,
        val studentAnswer: List<String>,
        val isCorrect: Boolean,
        val isPartial: Boolean,  // multi-select: some correct options selected, no wrong ones
        val isBlank: Boolean,
        val earnedScore: Double,
        val maxScore: Double
    )

    /**
     * Grade a student's answers against correct answers.
     *
     * Multi-select logic:
     * - Full match (all correct, no extra): full score
     * - Partial match (some correct options, no wrong ones): half score
     * - Wrong answer (includes any wrong option): zero
     * - Blank (no selection): zero
     *
     * @param correctAnswerJson JSON array of AnswerItemJson
     * @param studentAnswerJson JSON StudentAnswerJson
     * @param questionScores per-question max scores; if empty, split totalScore equally
     * @param totalScore fallback total if questionScores is empty
     */
    fun grade(
        correctAnswerJson: String,
        studentAnswerJson: String,
        totalScore: Double,
        questionScores: List<Double> = emptyList()
    ): GradingResult? {
        return try {
            val correctAnswers: List<AnswerItemJson> = json.decodeFromString(correctAnswerJson)
            val studentAnswer: StudentAnswerJson = json.decodeFromString(studentAnswerJson)

            val totalQuestions = correctAnswers.size
            if (totalQuestions == 0) {
                Timber.e("正确答案为空，无法判分")
                return null
            }

            // Determine per-question scores
            val perQuestionScores = if (questionScores.size == totalQuestions) {
                questionScores
            } else {
                List(totalQuestions) { totalScore / totalQuestions }
            }

            var totalEarned = 0.0
            var correctCount = 0
            var wrongCount = 0
            var blankCount = 0
            val details = mutableListOf<QuestionResult>()

            for (correctItem in correctAnswers) {
                val qi = correctItem.questionIndex
                val studentItem = studentAnswer.answers.find { it.questionIndex == qi }
                val studentSelected = studentItem?.selectedOptions ?: emptyList()
                val correctSet = correctItem.selectedOptions.toSet()
                val studentSet = studentSelected.toSet()
                val maxScore = perQuestionScores.getOrElse(qi) { totalScore / totalQuestions }

                val isBlank = studentSelected.isEmpty()
                val isFullCorrect = !isBlank && studentSet == correctSet

                // Partial: at least one correct selected, no wrong ones, but not all correct
                val hasWrong = studentSet.any { it !in correctSet }
                val hasCorrect = studentSet.any { it in correctSet }
                val isPartial = !isBlank && !isFullCorrect && !hasWrong && hasCorrect

                val earnedScore = when {
                    isFullCorrect -> maxScore
                    isPartial -> maxScore / 2.0  // half credit for partial match
                    else -> 0.0
                }

                totalEarned += earnedScore

                when {
                    isFullCorrect -> correctCount++
                    isPartial -> { /* counts as partial, not fully correct or wrong */
                        // Count as wrong for simple stats
                        wrongCount++
                    }
                    isBlank -> blankCount++
                    else -> wrongCount++
                }

                details.add(
                    QuestionResult(
                        index = qi,
                        correctAnswer = correctItem.selectedOptions,
                        studentAnswer = studentSelected,
                        isCorrect = isFullCorrect,
                        isPartial = isPartial,
                        isBlank = isBlank,
                        earnedScore = earnedScore,
                        maxScore = maxScore
                    )
                )
            }

            val totalPossible = perQuestionScores.sum()

            Timber.i("判分完成: 得分=$totalEarned/$totalPossible, 正确=$correctCount, 错误=$wrongCount, 未答=$blankCount")

            GradingResult(
                score = totalEarned,
                correctCount = correctCount,
                wrongCount = wrongCount,
                blankCount = blankCount,
                details = details
            )
        } catch (e: Exception) {
            Timber.e(e, "判分失败: ${e.message}")
            null
        }
    }
}
