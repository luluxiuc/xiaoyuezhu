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

/** One question: option circles A/B/C/D and which one is correct */
@Serializable
data class QuestionMaster(
    val index: Int,
    val options: List<CirclePos>,
    val correctOption: Int  // 0=A, 1=B, 2=C, 3=D
)

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
