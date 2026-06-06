package com.xiaoyuezhu.app.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyuezhu.app.domain.model.Exam
import com.xiaoyuezhu.app.domain.model.Grade
import com.xiaoyuezhu.app.domain.repository.ClassRepository
import com.xiaoyuezhu.app.domain.repository.GradeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExamDetailUiState(
    val exam: Exam? = null,
    val grades: List<Grade> = emptyList(),
    val studentNames: Map<String, String> = emptyMap(),
    val averageScore: Double = 0.0,
    val highestScore: Double = 0.0,
    val lowestScore: Double = 0.0,
    val passRate: Double = 0.0,
    val distribution: Map<String, Int> = emptyMap()
)

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val classRepository: ClassRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamDetailUiState())
    val uiState: StateFlow<ExamDetailUiState> = _uiState.asStateFlow()

    fun loadExam(examId: String) {
        viewModelScope.launch {
            val exam = gradeRepository.getExamById(examId)
            if (exam != null) {
                val students = classRepository.getStudentsByClass(exam.classId)
                val nameMap = students.associate { it.id to it.name }

                gradeRepository.getGradesByExam(examId).collect { grades ->
                    val sorted = grades.sortedByDescending { it.score }
                    val avg = if (sorted.isNotEmpty()) sorted.map { it.score }.average() else 0.0
                    val highest = sorted.maxOfOrNull { it.score } ?: 0.0
                    val lowest = sorted.minOfOrNull { it.score } ?: 0.0
                    val passCount = sorted.count { it.score / it.totalScore >= 0.6 }
                    val passRate = if (sorted.isNotEmpty()) passCount.toDouble() / sorted.size else 0.0

                    val dist = mutableMapOf(
                        "90-100" to 0, "80-89" to 0, "70-79" to 0,
                        "60-69" to 0, "0-59" to 0
                    )
                    sorted.forEach { g ->
                        val pct = g.score / g.totalScore * 100
                        when {
                            pct >= 90 -> dist["90-100"] = (dist["90-100"] ?: 0) + 1
                            pct >= 80 -> dist["80-89"] = (dist["80-89"] ?: 0) + 1
                            pct >= 70 -> dist["70-79"] = (dist["70-79"] ?: 0) + 1
                            pct >= 60 -> dist["60-69"] = (dist["60-69"] ?: 0) + 1
                            else -> dist["0-59"] = (dist["0-59"] ?: 0) + 1
                        }
                    }

                    _uiState.update {
                        it.copy(
                            exam = exam,
                            grades = sorted,
                            studentNames = nameMap,
                            averageScore = avg,
                            highestScore = highest,
                            lowestScore = lowest,
                            passRate = passRate,
                            distribution = dist
                        )
                    }
                }
            }
        }
    }
}
