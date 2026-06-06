package com.xiaoyuezhu.app.domain.repository

import com.xiaoyuezhu.app.domain.model.Exam
import com.xiaoyuezhu.app.domain.model.Grade
import kotlinx.coroutines.flow.Flow

sealed class SaveGradeResult {
    data class Success(val grade: Grade) : SaveGradeResult()
    data class Conflict(val existingGrade: Grade) : SaveGradeResult()
    data class Error(val message: String) : SaveGradeResult()
}

interface GradeRepository {
    suspend fun saveGrade(
        classId: String,
        paperId: String,
        paperTitle: String,
        studentId: String,
        score: Double,
        totalScore: Double,
        studentAnswerJson: String,
        correctCount: Int,
        wrongCount: Int,
        blankCount: Int
    ): SaveGradeResult

    suspend fun overwriteGrade(
        classId: String,
        paperId: String,
        paperTitle: String,
        studentId: String,
        score: Double,
        totalScore: Double,
        studentAnswerJson: String,
        correctCount: Int,
        wrongCount: Int,
        blankCount: Int
    ): SaveGradeResult

    fun getExamsByClass(classId: String): Flow<List<Exam>>
    fun getGradesByExam(examId: String): Flow<List<Grade>>
    suspend fun getExamById(examId: String): Exam?
    suspend fun checkIntegrity()
}
