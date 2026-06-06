package com.xiaoyuezhu.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class ExamStatus {
    SCANNING,
    COMPLETED
}

@Serializable
data class Exam(
    val id: String = UUID.randomUUID().toString(),
    val classId: String = "",
    val paperId: String = "",
    val paperTitle: String = "",
    val status: ExamStatus = ExamStatus.SCANNING,
    val gradedCount: Int = 0,
    val totalStudents: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
