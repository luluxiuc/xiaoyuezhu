package com.xiaoyuezhu.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Grade(
    val id: String = UUID.randomUUID().toString(),
    val examId: String = "",
    val studentId: String = "",
    val score: Double = 0.0,
    val totalScore: Double = 0.0,
    val studentAnswerJson: String = "[]",
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val blankCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
