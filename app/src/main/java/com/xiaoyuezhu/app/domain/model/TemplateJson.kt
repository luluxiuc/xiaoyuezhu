package com.xiaoyuezhu.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TemplateJson(
    val version: Int = 5,
    val canvasWidth: Int = 1800,
    val canvasHeight: Int = 0,
    val questionCount: Int = 20,
    val optionCount: Int = 4,
    val studentIdDigits: Int = 2,
    val idDigitCount: Int = 10,
    val questionsPerRow: Int = 4,
    // Approximate positions for bin-based reading
    val idColumns: List<IdColumnRef> = emptyList(),
    val questionRects: List<QuestionRectRef> = emptyList()
)

@Serializable
data class IdColumnRef(val x: Float, val y: Float, val w: Float, val h: Float)

@Serializable
data class QuestionRectRef(val index: Int, val x: Float, val y: Float, val w: Float, val h: Float)
