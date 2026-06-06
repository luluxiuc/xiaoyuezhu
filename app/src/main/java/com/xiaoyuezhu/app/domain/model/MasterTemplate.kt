package com.xiaoyuezhu.app.domain.model

import kotlinx.serialization.Serializable

/** A detected circle center + radius, with measured intensity during calibration */
@Serializable
data class CirclePos(
    val cx: Float,
    val cy: Float,
    val r: Float = 30f,
    val intensity: Double = 0.0  // mean pixel intensity during calibration (0=white/empty, 255=black/filled)
)

/** One ID digit column: circles top-to-bottom for digit 0..9 */
@Serializable
data class IdColumn(val circles: List<CirclePos>)

/** One question: option circles A/B/C/D and which options are correct */
@Serializable
data class QuestionMaster(
    val index: Int,
    val options: List<CirclePos>,
    val correctOptions: List<Int> = emptyList(),  // 0=A, 1=B, etc. Multiple = multi-select
    // Backward compat — prefer correctOptions
    @Deprecated("Use correctOptions")
    val correctOption: Int = -1
) {
    /** All correct option indices, handling legacy single-value field */
    val allCorrect: List<Int>
        get() = if (correctOptions.isNotEmpty()) correctOptions
                else if (correctOption >= 0) listOf(correctOption)
                else emptyList()
}

/** Master calibration — all circle positions learned from teacher's filled calibration sheet */
@Serializable
data class MasterTemplate(
    val version: Int = 2,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val studentIdDigits: Int = 2,
    val idColumns: List<IdColumn>,        // per-digit columns
    val questions: List<QuestionMaster>,   // per question
    val optionLabels: List<String> = listOf("A", "B", "C", "D"),
    // Thresholds discovered during calibration — reused during scan for consistency
    val globalThreshold: Double = 0.0,
    val rowThresholds: List<Double> = emptyList(),
    // Warp dimensions used during calibration — scan uses same size for position consistency
    val warpWidth: Int = 0,
    val warpHeight: Int = 0
)
