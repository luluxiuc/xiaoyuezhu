package com.xiaoyuezhu.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xiaoyuezhu.app.domain.model.Exam
import com.xiaoyuezhu.app.domain.model.ExamStatus

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PaperEntity::class,
            parentColumns = ["id"],
            childColumns = ["paper_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("class_id"), Index("paper_id")]
)
data class ExamEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "paper_id") val paperId: String,
    @ColumnInfo(name = "paper_title") val paperTitle: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "graded_count") val gradedCount: Int,
    @ColumnInfo(name = "total_students") val totalStudents: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long
) {
    fun toDomain(): Exam = Exam(
        id = id,
        classId = classId,
        paperId = paperId,
        paperTitle = paperTitle,
        status = if (status == "COMPLETED") ExamStatus.COMPLETED else ExamStatus.SCANNING,
        gradedCount = gradedCount,
        totalStudents = totalStudents,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(exam: Exam): ExamEntity = ExamEntity(
            id = exam.id,
            classId = exam.classId,
            paperId = exam.paperId,
            paperTitle = exam.paperTitle,
            status = exam.status.name,
            gradedCount = exam.gradedCount,
            totalStudents = exam.totalStudents,
            createdAt = exam.createdAt
        )
    }
}
