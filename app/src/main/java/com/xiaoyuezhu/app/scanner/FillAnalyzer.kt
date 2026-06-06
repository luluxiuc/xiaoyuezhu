package com.xiaoyuezhu.app.scanner

import com.xiaoyuezhu.app.domain.model.CirclePos
import org.opencv.core.*
import timber.log.Timber
import kotlin.math.sqrt

/**
 * OMRChecker-style "First Large Gap" adaptive thresholding.
 *
 * Completely data-driven: no fixed thresholds. Works by sorting bubble intensities
 * and finding the biggest jump — the natural separation between filled and empty.
 *
 * Two-tier: global threshold (all bubbles) + local threshold (per question row),
 * with confidence-based fallback to global when local separation is weak.
 */
object FillAnalyzer {

    /** Minimum gap magnitude to consider a valid separation (out of 255) */
    const val MIN_GAP = 20.0

    /** Minimum confidence surplus beyond MIN_GAP for local threshold to be trusted */
    const val CONFIDENT_SURPLUS = 10.0

    // ── Result types ──

    data class ThresholdResult(
        val threshold: Double,       // midpoint of the largest gap
        val gapMagnitude: Double,    // size of the gap (0..255)
        val confidence: Double,      // gap / totalRange, 0=none 1=perfect
        val values: DoubleArray,     // sorted descending
        val hadClearSeparation: Boolean
    )

    data class FillResult(
        val globalThreshold: Double,
        val rowThresholds: List<Double>,
        val rows: List<RowFillResult>
    )

    data class RowFillResult(
        val rowIndex: Int,
        val bubbles: List<BubbleFill>,
        val localThreshold: Double,
        val usedGlobalFallback: Boolean
    )

    data class BubbleFill(
        val cx: Float, val cy: Float, val r: Float,
        val meanIntensity: Double,   // 0=white(empty) .. 255=black(filled), measured from INVERTED gray
        val isFilled: Boolean,
        val label: String = ""
    )

    // ── Public API ──

    /**
     * Full analysis for calibration: measure all bubbles across the entire sheet,
     * compute global threshold from all measurements, then per-row local thresholds.
     */
    fun analyze(
        gray: Mat,
        allBubblePositions: List<List<CirclePos>>  // outer: rows, inner: bubbles in row
    ): FillResult {
        // Measure every bubble
        val inverted = Mat()
        Core.bitwise_not(gray, inverted)
        val allMeasurements = mutableListOf<Double>()
        val rows = mutableListOf<Pair<Int, List<BubbleFill>>>()

        for ((ri, row) in allBubblePositions.withIndex()) {
            val fills = row.map { pos ->
                val intensity = circleMean(inverted, pos.cx, pos.cy, pos.r * 0.7f)
                BubbleFill(pos.cx, pos.cy, pos.r, intensity, false)
            }
            rows.add(ri to fills)
            allMeasurements.addAll(fills.map { it.meanIntensity })
        }
        inverted.release()

        // Global threshold from all bubbles
        val globalResult = findLargestGap(allMeasurements.toDoubleArray(), MIN_GAP)
        val globalThreshold = globalResult.threshold

        if (globalResult.hadClearSeparation) {
            Timber.i("FillAnalyzer 全局阈值: %.1f (gap=%.1f, conf=%.2f)",
                globalThreshold, globalResult.gapMagnitude, globalResult.confidence)
        } else {
            Timber.w("FillAnalyzer 全局间隙弱 (%.1f), 使用中位阈值 %.1f",
                globalResult.gapMagnitude, globalThreshold)
        }

        // Local threshold per row
        val rowThresholds = mutableListOf<Double>()
        val rowResults = mutableListOf<RowFillResult>()

        for ((ri, fills) in rows) {
            val rowValues = fills.map { it.meanIntensity }.toDoubleArray()
            val localResult = findLargestGap(rowValues, MIN_GAP)

            // Confidence check: use local only if gap clearly exceeds MIN_GAP
            val useLocal = localResult.hadClearSeparation &&
                    localResult.gapMagnitude >= MIN_GAP + CONFIDENT_SURPLUS

            val effectiveThreshold = if (useLocal) localResult.threshold else globalThreshold
            rowThresholds.add(effectiveThreshold)

            val classifiedFills = fills.map { f ->
                f.copy(isFilled = f.meanIntensity > effectiveThreshold)
            }
            rowResults.add(RowFillResult(ri, classifiedFills, effectiveThreshold, !useLocal))

            if (ri < 5 || useLocal) {
                Timber.d("  Row$ri thr=%.1f local=%s gap=%.1f: %s",
                    effectiveThreshold, if (useLocal) "Y" else "GLOBAL",
                    localResult.gapMagnitude,
                    classifiedFills.joinToString(" ") { "${"%.0f".format(it.meanIntensity)}${if(it.isFilled)"✓" else "○"}" })
            }
        }

        return FillResult(globalThreshold, rowThresholds, rowResults)
    }

    /**
     * Scan-time classification: use pre-computed thresholds from calibration.
     * Optionally re-derive per-sheet thresholds for extra robustness.
     */
    fun classify(
        measurements: List<List<Double>>,  // per row, per bubble
        rowThresholds: List<Double>,
        globalThreshold: Double,
        rederivePerSheet: Boolean = true
    ): List<List<Boolean>> {
        return measurements.mapIndexed { ri, rowValues ->
            val effectiveThreshold = if (rederivePerSheet) {
                // Re-derive local for this specific sheet (handles lighting changes)
                val local = findLargestGap(rowValues.toDoubleArray(), MIN_GAP)
                if (local.hadClearSeparation && local.gapMagnitude >= MIN_GAP + CONFIDENT_SURPLUS)
                    local.threshold
                else {
                    // Fall back to calibration threshold
                    rowThresholds.getOrElse(ri) { globalThreshold }
                }
            } else {
                rowThresholds.getOrElse(ri) { globalThreshold }
            }
            rowValues.map { it > effectiveThreshold }
        }
    }

    // ── Core algorithm: Find Largest Gap ──

    /**
     * Sort values descending and find the biggest jump between consecutive values.
     * The threshold is the midpoint of that jump.
     *
     * This is OMRChecker's key innovation: instead of a fixed threshold like "50% dark",
     * it finds the natural separation point in the data.
     */
    fun findLargestGap(values: DoubleArray, minGap: Double = MIN_GAP): ThresholdResult {
        if (values.isEmpty()) {
            return ThresholdResult(128.0, 0.0, 0.0, values, false)
        }
        if (values.size == 1) {
            return ThresholdResult(values[0] / 2.0, 0.0, 0.0, values, false)
        }

        val sorted = values.sortedArrayDescending()
        val totalRange = sorted.first() - sorted.last()

        var maxGap = 0.0
        var threshold = sorted.first() / 2.0

        for (i in 0 until sorted.size - 1) {
            val gap = sorted[i] - sorted[i + 1]
            if (gap > maxGap) {
                maxGap = gap
                threshold = (sorted[i] + sorted[i + 1]) / 2.0
            }
        }

        val confidence = if (totalRange > 0) maxGap / totalRange else 0.0
        val hadClearSeparation = maxGap >= minGap

        return ThresholdResult(threshold, maxGap, confidence, sorted, hadClearSeparation)
    }

    // ── Intensity measurement ──

    /**
     * Measure mean pixel intensity within a circular window.
     * Uses 70% of the circle radius to avoid edge artifacts.
     *
     * @param gray INVERTED grayscale (dark=high=filled), or use inverted=false for normal
     * @param inverted if true, expects normal gray (white paper=high) and inverts meaning
     */
    fun circleMean(gray: Mat, cx: Float, cy: Float, radius: Float): Double {
        val r = radius.toInt().coerceAtLeast(3)
        val xi = cx.toInt().coerceIn(r, gray.cols() - r - 1)
        val yi = cy.toInt().coerceIn(r, gray.rows() - r - 1)

        var sum = 0L
        var count = 0L
        val r2 = r * r

        val yStart = (yi - r).coerceAtLeast(0)
        val yEnd = (yi + r).coerceAtMost(gray.rows() - 1)
        val xStart = (xi - r).coerceAtLeast(0)
        val xEnd = (xi + r).coerceAtMost(gray.cols() - 1)

        for (row in yStart..yEnd) {
            val dy = row - yi
            val dxMax = sqrt((r2 - dy * dy).toDouble()).toInt()
            val colStart = (xi - dxMax).coerceAtLeast(xStart)
            val colEnd = (xi + dxMax).coerceAtMost(xEnd)

            if (colStart > colEnd) continue

            val buf = ByteArray((colEnd - colStart + 1) * gray.channels())
            gray.get(row, colStart, buf)
            for (i in buf.indices step gray.channels()) {
                sum += buf[i].toLong() and 0xFF
                count++
            }
        }

        return if (count > 0) sum.toDouble() / count else 0.0
    }
}
