package com.xiaoyuezhu.app.scanner

import com.xiaoyuezhu.app.domain.model.CirclePos
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Auto-discover all bubble positions from a warped answer sheet image.
 *
 * Uses a dual-strategy approach:
 * 1. Canny edge detection → finds circle OUTLINES (works for empty bubbles)
 * 2. Otsu threshold → finds filled circle INTERIORS (works for filled bubbles)
 *
 * Merges both, filters by circularity, groups into row/column grid structure,
 * and identifies ID columns vs answer question rows.
 */
object BubbleDetector {

    /** Minimum circularity for a contour to be considered a bubble */
    private const val MIN_CIRCULARITY = 0.55

    data class DiscoveredLayout(
        val idColumns: List<List<CirclePos>>,    // one list per digit column, circles top-to-bottom
        val questionRows: List<List<CirclePos>>,  // each row = option circles ordered left-to-right
        val optionCount: Int,
        val questionsPerRow: Int
    )

    /**
     * Discover all bubbles from the warped calibration sheet.
     *
     * @param warped grayscale Mat, already perspective-corrected
     * @param expectedOptionCount e.g., 4 for A/B/C/D
     * @param studentIdDigits e.g., 2 for tens+ones digits
     * @param expectedQuestions total expected questions (for validation)
     * @param expectedQuestionsPerRow questions per visual row, from LayoutEngine (default 2)
     */
    fun discover(
        warped: Mat,
        expectedOptionCount: Int,
        studentIdDigits: Int,
        expectedQuestions: Int,
        expectedQuestionsPerRow: Int = 2
    ): DiscoveredLayout? {
        val w = warped.cols()
        val h = warped.rows()
        val estimatedRadius = (w * 45.0 / 1800.0).toInt().coerceIn(20, 45)  // ~30px at 1200w

        Timber.d("BubbleDetector: 图像=%dx%d 预估半径=%d", w, h, estimatedRadius)

        // ── Strategy 1: Canny → contour (finds circle outlines, both empty and filled) ──
        val cannyCenters = detectViaCanny(warped, estimatedRadius)
        Timber.d("  Canny检测: ${cannyCenters.size} 个圆心")

        // ── Strategy 2: Otsu → contour (finds filled circle interiors) ──
        val otsuCenters = detectViaOtsu(warped, estimatedRadius)
        Timber.d("  Otsu检测: ${otsuCenters.size} 个圆心")

        // ── Merge (union by proximity) ──
        val merged = mergeCenters(cannyCenters + otsuCenters, estimatedRadius * 0.6)
        Timber.d("  合并后: ${merged.size} 个圆心")

        if (merged.size < expectedOptionCount * expectedQuestions * 0.4) {
            Timber.w("BubbleDetector: 检测到的圆太少 (${merged.size}), 预期至少 ${expectedOptionCount * expectedQuestions * 0.4}")
            // Try HoughCircles as fallback
            val hough = detectViaHough(warped, estimatedRadius)
            Timber.d("  Hough fallback: ${hough.size} 个圆心")
            if (hough.size > merged.size) {
                val houghMerged = mergeCenters(hough + merged, estimatedRadius * 0.5)
                Timber.d("  Hough合并后: ${houghMerged.size}")
                return buildLayout(houghMerged, w, h, expectedOptionCount, studentIdDigits,
                    expectedQuestions, expectedQuestionsPerRow)
            }
            return null
        }

        return buildLayout(merged, w, h, expectedOptionCount, studentIdDigits,
            expectedQuestions, expectedQuestionsPerRow)
    }

    // ── Detection strategies ──

    private fun detectViaCanny(gray: Mat, estRadius: Int): List<Point> {
        val edges = Mat()
        try {
            // Canny finds circle outline edges
            Imgproc.Canny(gray, edges, 40.0, 120.0)
            // Dilate slightly to connect fragmented outlines
            Imgproc.dilate(edges, edges,
                Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0)))
            return extractCircleCenters(edges, estRadius)
        } finally {
            edges.release()
        }
    }

    private fun detectViaOtsu(gray: Mat, estRadius: Int): List<Point> {
        val binary = Mat()
        try {
            // Otsu threshold — adaptive to lighting
            Imgproc.threshold(gray, binary, 0.0, 255.0,
                Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
            // Morphological open to remove noise
            Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_OPEN,
                Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0)))
            return extractCircleCenters(binary, estRadius)
        } finally {
            binary.release()
        }
    }

    private fun detectViaHough(gray: Mat, estRadius: Int): List<Point> {
        val circles = Mat()
        try {
            // Gaussian blur first for Hough
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(9.0, 9.0), 2.0)
            val minR = (estRadius * 0.6).toInt().coerceAtLeast(15)
            val maxR = (estRadius * 1.5).toInt().coerceAtMost(80)
            // param2=15: lower accumulator threshold for better sensitivity on sparse ID circles
            Imgproc.HoughCircles(blurred, circles, Imgproc.HOUGH_GRADIENT,
                1.0, estRadius * 1.5, 100.0, 15.0, minR, maxR)
            blurred.release()

            val points = mutableListOf<Point>()
            for (i in 0 until circles.cols()) {
                val v = circles.get(0, i)
                points.add(Point(v[0], v[1]))
            }
            return points
        } catch (e: Exception) {
            Timber.w("HoughCircles失败: ${e.message}")
            return emptyList()
        } finally {
            circles.release()
        }
    }

    // ── Shared contour extraction ──

    private fun extractCircleCenters(binary: Mat, estRadius: Int): List<Point> {
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(binary, contours, Mat(),
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val centers = mutableListOf<Point>()
        val minArea = PI * (estRadius * 0.4) * (estRadius * 0.4)  // ~500 at 40px
        val maxArea = PI * (estRadius * 1.6) * (estRadius * 1.6)  // ~12800 at 40px

        for (cnt in contours) {
            val area = Imgproc.contourArea(cnt)
            if (area < minArea || area > maxArea) continue

            val peri = Imgproc.arcLength(MatOfPoint2f(*cnt.toArray()), true)
            if (peri <= 0) continue

            val circularity = 4.0 * PI * area / (peri * peri)
            if (circularity < MIN_CIRCULARITY) continue

            // Get center from moments
            val moments = Imgproc.moments(cnt)
            if (moments.m00 <= 0) continue
            val cx = moments.m10 / moments.m00
            val cy = moments.m01 / moments.m00

            // Use minEnclosingCircle for radius sanity check
            val center = Point()
            val radius = FloatArray(1)
            Imgproc.minEnclosingCircle(MatOfPoint2f(*cnt.toArray()), center, radius)
            if (radius[0] < estRadius * 0.4 || radius[0] > estRadius * 1.8) continue

            centers.add(Point(cx, cy))
        }
        return centers
    }

    // ── Merge nearby centers ──

    private fun mergeCenters(all: List<Point>, proximityThreshold: Double): List<Point> {
        if (all.isEmpty()) return emptyList()

        // Sort by x then y for deterministic grouping
        val sorted = all.sortedBy { it.x * 100000 + it.y }
        val merged = mutableListOf<Point>()
        val used = BooleanArray(sorted.size)

        for (i in sorted.indices) {
            if (used[i]) continue
            var cx = sorted[i].x; var cy = sorted[i].y; var count = 1
            for (j in i + 1 until sorted.size) {
                if (used[j]) continue
                val dx = sorted[j].x - cx / count
                val dy = sorted[j].y - cy / count
                if (sqrt(dx * dx + dy * dy) < proximityThreshold) {
                    cx += sorted[j].x; cy += sorted[j].y; count++
                    used[j] = true
                }
            }
            merged.add(Point(cx / count, cy / count))
        }
        return merged
    }

    // ── Build grid layout ──

    private fun buildLayout(
        centers: List<Point>, imgW: Int, imgH: Int,
        optionCount: Int, studentIdDigits: Int,
        expectedQuestions: Int, questionsPerRow: Int
    ): DiscoveredLayout? {
        Timber.d("buildLayout: ${centers.size}圆心, ${expectedQuestions}题, ${optionCount}选项, ${studentIdDigits}学号位")

        // ── Step 1: Cluster circles by X into vertical columns ──
        // This cleanly separates ID columns (left) from answer columns (right)
        val allColumns = groupIntoColumns(centers, medianGap(centers, byY = false) * 0.6)
        Timber.d("  X聚类: ${allColumns.size}列 → ${allColumns.map { it.size }.joinToString(",")}")

        if (allColumns.isEmpty()) return null

        // ── Step 2: Identify ID columns ──
        // ID columns: leftmost groups with ~10 circles (digits 0-9), sorted top-to-bottom
        // Answer columns: everything else, each has circles for one option slot
        val sortedCols = allColumns.sortedBy { it.first().x }
        val idColumns = mutableListOf<List<CirclePos>>()
        val answerCols = mutableListOf<List<Point>>()

        for (col in sortedCols) {
            val sorted = col.sortedBy { it.y }
            // ID column: has ~10 circles (digits), tightly packed vertically
            if (idColumns.size < studentIdDigits && sorted.size in 6..14) {
                idColumns.add(sorted.map { CirclePos(it.x.toFloat(), it.y.toFloat()) })
                Timber.d("  ID列${idColumns.size}: ${sorted.size}个圆, x≈${"%.0f".format(sorted.first().x)}")
            } else {
                answerCols.add(sorted)
            }
        }

        // ── Step 3: Build answer rows ──
        // Answer columns map to option positions (A, B, C, D from left to right)
        // Within each option column, circles at different Y = different questions
        // Group answer circles by Y to form rows
        val allAnswerCenters = answerCols.flatten()
        if (allAnswerCenters.isEmpty()) {
            Timber.w("无答案圆")
            return null
        }
        // Group by Y with a fixed tolerance (30px at 1200w ≈ 2.5% of height).
        // Don't use median gap — it's dominated by tiny intra-row jitter and
        // produces too-small tolerances that fragment rows.
        val answerRows = groupIntoRows(allAnswerCenters, 30.0)
        Timber.d("  答案行(Y容差=30): ${answerRows.size}行 → ${answerRows.map { it.size }.joinToString(",")}")

        // Filter: keep rows with roughly the expected number of circles per row
        val expectedPerRow = optionCount * questionsPerRow
        val validRows = answerRows
            .filter { it.size in (expectedPerRow - 3)..(expectedPerRow + 3) }
            .map { row ->
                row.sortedBy { it.x }.map { CirclePos(it.x.toFloat(), it.y.toFloat()) }
            }

        val totalCircles = validRows.sumOf { it.size }
        val expectedTotal = expectedQuestions * optionCount
        Timber.d("布局: ${idColumns.size}学号列, ${validRows.size}答案行, " +
                "共${totalCircles}个答案圆, 预期${expectedTotal}, 图像=${imgW}x${imgH}")

        if (totalCircles < expectedTotal * 0.5) {
            Timber.w("答案圆太少: ${totalCircles} < ${expectedTotal * 0.5}")
            return null
        }

        return DiscoveredLayout(idColumns, validRows, optionCount, questionsPerRow)
    }

    private fun medianGap(pts: List<Point>, byY: Boolean): Double {
        val sorted = pts.sortedBy { if (byY) it.y else it.x }
        val gaps = (1 until sorted.size)
            .map { if (byY) sorted[it].y - sorted[it - 1].y else sorted[it].x - sorted[it - 1].x }
            .filter { it > 0 }.sorted()
        return if (gaps.isNotEmpty()) gaps[gaps.size / 2] else if (byY) 30.0 else 50.0
    }

    // ── Grid grouping helpers ──

    private fun groupIntoRows(pts: List<Point>, yTolerance: Double): List<List<Point>> {
        if (pts.isEmpty()) return emptyList()
        val sorted = pts.sortedBy { it.y }
        val rows = mutableListOf<MutableList<Point>>()
        rows.add(mutableListOf(sorted[0]))

        for (i in 1 until sorted.size) {
            val gap = sorted[i].y - sorted[i - 1].y
            if (gap <= yTolerance) {
                rows.last().add(sorted[i])
            } else {
                rows.add(mutableListOf(sorted[i]))
            }
        }
        return rows.filter { it.size >= 2 }  // at least 2 circles per row
    }

    private fun groupIntoColumns(pts: List<Point>, xTolerance: Double): List<List<Point>> {
        if (pts.isEmpty()) return emptyList()
        val sorted = pts.sortedBy { it.x }
        val cols = mutableListOf<MutableList<Point>>()
        cols.add(mutableListOf(sorted[0]))

        for (i in 1 until sorted.size) {
            val gap = sorted[i].x - sorted[i - 1].x
            if (gap <= xTolerance) {
                cols.last().add(sorted[i])
            } else {
                cols.add(mutableListOf(sorted[i]))
            }
        }
        return cols
    }

}
