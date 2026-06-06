package com.xiaoyuezhu.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xiaoyuezhu.app.domain.model.Grade

@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["exam_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exam_id"), Index(value = ["exam_id", "student_id"], unique = true)]
)
data class GradeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "exam_id") val examId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "score") val score: Double,
    @ColumnInfo(name = "total_score") val totalScore: Double,
    @ColumnInfo(name = "student_answer_json") val studentAnswerJson: String,
    @ColumnInfo(name = "correct_count") val correctCount: Int,
    @ColumnInfo(name = "wrong_count") val wrongCount: Int,
    @ColumnInfo(name = "blank_count") val blankCount: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long
) {
    fun toDomain(): Grade = Grade(
        id = id,
        examId = examId,
        studentId = studentId,
        score = score,
        totalScore = totalScore,
        studentAnswerJson = studentAnswerJson,
        correctCount = correctCount,
        wrongCount = wrongCount,
        blankCount = blankCount,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(grade: Grade): GradeEntity = GradeEntity(
            id = grade.id,
            examId = grade.examId,
            studentId = grade.studentId,
            score = grade.score,
            totalScore = grade.totalScore,
            studentAnswerJson = grade.studentAnswerJson,
            correctCount = grade.correctCount,
            wrongCount = grade.wrongCount,
            blankCount = grade.blankCount,
            createdAt = grade.createdAt
        )
    }
}
