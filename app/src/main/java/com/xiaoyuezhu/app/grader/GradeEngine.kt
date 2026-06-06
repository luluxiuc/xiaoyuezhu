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
        val isBlank: Boolean
    )

    fun grade(
        correctAnswerJson: String,
        studentAnswerJson: String,
        totalScore: Double
    ): GradingResult? {
        return try {
            val correctAnswers: List<AnswerItemJson> = json.decodeFromString(correctAnswerJson)
            val studentAnswer: StudentAnswerJson = json.decodeFromString(studentAnswerJson)

            val totalQuestions = correctAnswers.size
            if (totalQuestions == 0) {
                Timber.e("正确答案为空，无法判分")
                return null
            }

            val perQuestionScore = if (totalQuestions > 0) totalScore / totalQuestions else 0.0

            var correctCount = 0
            var wrongCount = 0
            var blankCount = 0
            val details = mutableListOf<QuestionResult>()

            for (correctItem in correctAnswers) {
                val studentItem = studentAnswer.answers.find { it.questionIndex == correctItem.questionIndex }
                val studentSelected = studentItem?.selectedOptions ?: emptyList()
                val isBlank = studentSelected.isEmpty()
                val isCorrect = if (isBlank) false else studentSelected.sorted() == correctItem.selectedOptions.sorted()

                if (isCorrect) correctCount++
                else if (isBlank) blankCount++
                else wrongCount++

                details.add(
                    QuestionResult(
                        index = correctItem.questionIndex,
                        correctAnswer = correctItem.selectedOptions,
                        studentAnswer = studentSelected,
                        isCorrect = isCorrect,
                        isBlank = isBlank
                    )
                )
            }

            val score = correctCount * perQuestionScore

            Timber.i("判分完成: 得分=$score/$totalScore, 正确=$correctCount, 错误=$wrongCount, 未答=$blankCount")

            GradingResult(
                score = score,
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
