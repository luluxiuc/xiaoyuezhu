package com.xiaoyuezhu.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TemplateJson(
    val version: Int = 6,
    val canvasWidth: Int = 1800,
    val canvasHeight: Int = 0,
    val questionCount: Int = 20,
    val optionCount: Int = 4,           // kept for backward compat; prefer optionCounts
    val optionCounts: List<Int> = emptyList(), // per-question option counts
    val studentIdDigits: Int = 2,
    val idDigitCount: Int = 10,
    val questionsPerRow: Int = 3,
    // Approximate positions for bin-based reading
    val idColumns: List<IdColumnRef> = emptyList(),
    val questionRects: List<QuestionRectRef> = emptyList(),
    // Per-question scores (for grading). If empty, use equal split.
    val questionScores: List<Double> = emptyList()
) {
    /** Total score = sum of all question scores, or 100 if not configured */
    val totalScore: Double
        get() = if (questionScores.isNotEmpty()) questionScores.sum() else 100.0

    /** Get option count for a question, falling back to global optionCount */
    fun optionCountFor(qIndex: Int): Int =
        optionCounts.getOrElse(qIndex) { optionCount }

    /** Get score for a question, or equal split of total */
    fun scoreFor(qIndex: Int): Double =
        questionScores.getOrElse(qIndex) {
            if (questionCount > 0) totalScore / questionCount else 0.0
        }
}

@Serializable
data class IdColumnRef(val x: Float, val y: Float, val w: Float, val h: Float)

@Serializable
data class QuestionRectRef(val index: Int, val x: Float, val y: Float, val w: Float, val h: Float)
