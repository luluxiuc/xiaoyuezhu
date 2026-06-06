package com.xiaoyuezhu.app.ui.exam

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyuezhu.app.domain.model.*
import com.xiaoyuezhu.app.domain.repository.ClassRepository
import com.xiaoyuezhu.app.domain.repository.GradeRepository
import com.xiaoyuezhu.app.domain.repository.PaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject

data class QuestionWrongCount(
    val questionIndex: Int,
    val wrongCount: Int,
    val totalAnswers: Int,
    val wrongRate: Double  // 0..1
)

data class GradeWithDetails(
    val grade: Grade,
    val questionResults: List<QuestionResultDetail>,  // per-question right/wrong
    val studentName: String
)

data class QuestionResultDetail(
    val questionIndex: Int,
    val correctOptions: List<String>,
    val studentOptions: List<String>,
    val isCorrect: Boolean,
    val isPartial: Boolean,
    val isBlank: Boolean,
    val earnedScore: Double,
    val maxScore: Double
)

data class ExamDetailUiState(
    val exam: Exam? = null,
    val grades: List<GradeWithDetails> = emptyList(),
    val averageScore: Double = 0.0,
    val highestScore: Double = 0.0,
    val lowestScore: Double = 0.0,
    val passRate: Double = 0.0,
    val distribution: Map<String, Int> = emptyMap(),
    val mostWrongQuestions: List<QuestionWrongCount> = emptyList(),
    val exportCsvPath: String? = null
)

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val classRepository: ClassRepository,
    private val paperRepository: PaperRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val df = DecimalFormat("0.#")

    private val _uiState = MutableStateFlow(ExamDetailUiState())
    val uiState: StateFlow<ExamDetailUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun loadExam(examId: String) {
        viewModelScope.launch {
            val exam = gradeRepository.getExamById(examId) ?: return@launch

            // Load paper for correct answers and per-question scores
            val paper = paperRepository.getPaperById(exam.paperId)
            val correctAnswers: List<AnswerItemJson> = try {
                json.decodeFromString(paper?.correctAnswerJson ?: "[]")
            } catch (_: Exception) { emptyList() }
            val template = try {
                json.decodeFromString<TemplateJson>(paper?.templateJson ?: "{}")
            } catch (_: Exception) { null }

            val students = classRepository.getStudentsByClass(exam.classId)
            val nameMap = students.associate { it.id to it.name }

            gradeRepository.getGradesByExam(examId).collect { grades ->
                // Build per-question details for each grade
                val gradesWithDetails = grades.map { grade ->
                    val studentAnswers: List<AnswerItemJson> = try {
                        val sa = json.decodeFromString<StudentAnswerJson>(grade.studentAnswerJson)
                        sa.answers
                    } catch (_: Exception) { emptyList() }

                    val details = correctAnswers.map { correct ->
                        val qi = correct.questionIndex
                        val student = studentAnswers.find { it.questionIndex == qi }
                        val studentOpts = student?.selectedOptions ?: emptyList()
                        val correctSet = correct.selectedOptions.toSet()
                        val studentSet = studentOpts.toSet()
                        val isBlank = studentOpts.isEmpty()
                        val hasWrong = studentSet.any { it !in correctSet }
                        val hasCorrect = studentSet.any { it in correctSet }
                        val isFullCorrect = !isBlank && studentSet == correctSet
                        val isPartial = !isBlank && !isFullCorrect && !hasWrong && hasCorrect
                        val maxScore = template?.scoreFor(qi) ?: (100.0 / correctAnswers.size)
                        val earnedScore = when {
                            isFullCorrect -> maxScore
                            isPartial -> maxScore / 2.0
                            else -> 0.0
                        }
                        QuestionResultDetail(
                            questionIndex = qi,
                            correctOptions = correct.selectedOptions,
                            studentOptions = studentOpts,
                            isCorrect = isFullCorrect,
                            isPartial = isPartial,
                            isBlank = isBlank,
                            earnedScore = earnedScore,
                            maxScore = maxScore
                        )
                    }

                    GradeWithDetails(
                        grade = grade,
                        questionResults = details,
                        studentName = nameMap[grade.studentId] ?: "未知"
                    )
                }.sortedByDescending { it.grade.score }

                // Statistics — use per-grade totalScore
                val avg = if (grades.isNotEmpty()) grades.map { it.score }.average() else 0.0
                val highest = grades.maxOfOrNull { it.score } ?: 0.0
                val lowest = grades.minOfOrNull { it.score } ?: 0.0
                val totalForDist = grades.maxOfOrNull { it.totalScore } ?: 100.0
                val passCount = grades.count { it.totalScore > 0 && it.score / it.totalScore >= 0.6 }
                val passRate = if (grades.isNotEmpty()) passCount.toDouble() / grades.size else 0.0

                // Dynamic score ranges based on actual total
                val totalI = totalForDist.toInt().coerceAtLeast(1)
                val segSize = maxOf((totalI + 4) / 5, 1)  // ~5 segments, min 1 point wide
                val dist = linkedMapOf<String, Int>()
                var rangeStart = totalI
                while (rangeStart > 0) {
                    val rangeEnd = maxOf(rangeStart - segSize + 1, 0)
                    val label = if (rangeStart == rangeEnd) "$rangeStart"
                                else "$rangeEnd-$rangeStart"
                    dist[label] = 0
                    rangeStart -= segSize
                }
                grades.forEach { g ->
                    val s = g.score.toInt()
                    val label = dist.keys.firstOrNull { key ->
                        val parts = key.split("-")
                        val lo = parts.first().toIntOrNull() ?: 0
                        val hi = parts.last().toIntOrNull() ?: 0
                        s in lo..hi
                    }
                    if (label != null) dist[label] = (dist[label] ?: 0) + 1
                }

                // Most wrong questions
                val wrongCounts = mutableMapOf<Int, Int>()
                for (gd in gradesWithDetails) {
                    for (qr in gd.questionResults) {
                        if (!qr.isCorrect && !qr.isBlank) {
                            wrongCounts[qr.questionIndex] =
                                (wrongCounts[qr.questionIndex] ?: 0) + 1
                        }
                    }
                }
                val totalStudents = grades.size
                val mostWrong = wrongCounts.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .map { (qi, count) ->
                        QuestionWrongCount(qi, count, totalStudents,
                            if (totalStudents > 0) count.toDouble() / totalStudents else 0.0)
                    }

                _uiState.update {
                    it.copy(
                        exam = exam,
                        grades = gradesWithDetails,
                        averageScore = avg,
                        highestScore = highest,
                        lowestScore = lowest,
                        passRate = passRate,
                        distribution = dist,
                        mostWrongQuestions = mostWrong
                    )
                }
            }
        }
    }

    fun exportCsv() {
        val st = _uiState.value
        val grades = st.grades
        if (grades.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(appContext.cacheDir, "grades_export.csv")
                    file.bufferedWriter().use { writer ->
                        // BOM for Excel UTF-8
                        writer.write("﻿")
                        // Header
                        val totalQuestions = grades.firstOrNull()?.questionResults?.size ?: 0
                        val qHeaders = (1..totalQuestions).joinToString(",") { "第${it}题" }
                        writer.write("学号,姓名,得分,总分,正确,错误,未答,$qHeaders\n")
                        // Rows
                        for (gd in grades) {
                            val qResults = gd.questionResults.joinToString(",") { qr ->
                                when {
                                    qr.isCorrect -> "✓"
                                    qr.isPartial -> "半对"
                                    qr.isBlank -> "未答"
                                    else -> "✗"
                                }
                            }
                            writer.write("${gd.grade.studentId},${gd.studentName}," +
                                "${df.format(gd.grade.score)},${df.format(gd.grade.totalScore)}," +
                                "${gd.grade.correctCount},${gd.grade.wrongCount},${gd.grade.blankCount}," +
                                "$qResults\n")
                        }
                        // Summary
                        writer.write("\n统计\n")
                        writer.write("平均分,${df.format(st.averageScore)}\n")
                        writer.write("最高分,${df.format(st.highestScore)}\n")
                        writer.write("最低分,${df.format(st.lowestScore)}\n")
                        writer.write("及格率,${String.format("%.0f%%", st.passRate * 100)}\n")
                        if (st.mostWrongQuestions.isNotEmpty()) {
                            writer.write("\n易错题\n")
                            st.mostWrongQuestions.forEach { mwq ->
                                writer.write("第${mwq.questionIndex + 1}题," +
                                    "${mwq.wrongCount}/${mwq.totalAnswers}人错\n")
                            }
                        }
                    }
                    _uiState.update { it.copy(exportCsvPath = file.absolutePath) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun clearExport() {
        _uiState.update { it.copy(exportCsvPath = null) }
    }
}
