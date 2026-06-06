package com.xiaoyuezhu.app.scanner

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Find the answer sheet page border using adaptive threshold + morphological close.
 *
 * Replaces Canny edge detection (brittle on phone photos) with Gaussian adaptive
 * threshold that handles non-uniform lighting. Morphological close fills small
 * gaps in the paper edge so findContours produces a clean quadrilateral.
 *
 * Includes frame stabilization: triggers only when the quadrilateral corners
 * are stable across N consecutive frames.
 */
object PageFinder {

    /** Minimum page area as fraction of frame area */
    private const val MIN_AREA_FRACTION = 0.015
    /** Maximum AR deviation from target (0.30 = ±30%) */
    private const val MAX_AR_DEVIATION = 0.30
    /** Edge margin: reject contours touching within this many pixels of frame edge */
    private const val EDGE_MARGIN = 5

    data class QuadResult(
        val found: Boolean,
        val tl: Point? = null,
        val tr: Point? = null,
        val bl: Point? = null,
        val br: Point? = null,
        val aspectRatio: Double = 0.0,
        val confidence: Double = 0.0   // 0..1, higher = better match
    )

    /**
     * Find the page quadrilateral in a grayscale camera frame.
     * @param targetAR expected aspect ratio (width/height) of the answer sheet
     */
    fun find(gray: Mat, targetAR: Double): QuadResult {
        val iw = gray.cols().toDouble()
        val ih = gray.rows().toDouble()
        val imgArea = iw * ih

        val binary = Mat()
        val closed = Mat()

        try {
            // 1. Adaptive threshold — handles uneven lighting on phone photos
            Imgproc.adaptiveThreshold(
                gray, binary, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                31,  // block size — large enough to capture paper/background boundary
                10.0 // constant subtracted
            )

            // 2. Morphological close — fill gaps from shadows or creases
            val kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, Size(7.0, 7.0)
            )
            Imgproc.morphologyEx(binary, closed, Imgproc.MORPH_CLOSE, kernel)

            // 3. Find contours
            val contours = mutableListOf<MatOfPoint>()
            Imgproc.findContours(closed, contours, Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            // 4. Find the best quadrilateral
            var bestScore = 0.0
            var bestResult: QuadResult? = null

            for (cnt in contours) {
                val area = Imgproc.contourArea(cnt)
                if (area < imgArea * MIN_AREA_FRACTION) continue

                val peri = Imgproc.arcLength(MatOfPoint2f(*cnt.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(
                    MatOfPoint2f(*cnt.toArray()), approx,
                    0.02 * peri, true  // 2% of perimeter — looser than 4-corner anchor
                )

                // Accept quadrilaterals and near-quadrilaterals
                val vertexCount = approx.total().toInt()
                if (vertexCount !in 4..6) continue
                if (vertexCount > 4) {
                    // Could be a pentagon/hexagon from a crease — take the 4 extreme corners
                    val pts = (0 until vertexCount).map { approx.toList()[it] }
                    val tl = pts.minByOrNull { it.x + it.y }!!
                    val br = pts.maxByOrNull { it.x + it.y }!!
                    val bl = pts.maxByOrNull { it.x - it.y }!!  // farther left, lower → higher diff
                    val tr = pts.minByOrNull { it.x - it.y }!!  // farther right, higher → lower diff
                    val best4 = listOf(tl, tr, bl, br)
                    evaluateQuad(best4, iw, ih, targetAR)?.let { result ->
                        val score = scoreQuad(result, area / imgArea)
                        if (score > bestScore) { bestScore = score; bestResult = result }
                    }
                } else {
                    val pts = (0 until 4).map { approx.toList()[it] }
                    evaluateQuad(pts, iw, ih, targetAR)?.let { result ->
                        val score = scoreQuad(result, area / imgArea)
                        if (score > bestScore) { bestScore = score; bestResult = result }
                    }
                }
            }

            val result = bestResult
            if (result != null && bestScore > 0.3) {
                return result
            }
            return QuadResult(false)
        } catch (e: Exception) {
            Timber.e(e, "页面查找异常")
            return QuadResult(false)
        } finally {
            binary.release()
            closed.release()
        }
    }

    private fun evaluateQuad(
        pts: List<Point>, iw: Double, ih: Double, targetAR: Double
    ): QuadResult? {
        // Reject edge-touching
        if (pts.any { it.x <= EDGE_MARGIN || it.x >= iw - EDGE_MARGIN ||
                it.y <= EDGE_MARGIN || it.y >= ih - EDGE_MARGIN }) return null

        // Sort corners: first by Y (top → bottom), then by X within each pair
        val byY = pts.sortedBy { it.y }
        val top = listOf(byY[0], byY[1]).sortedBy { it.x }
        val bottom = listOf(byY[2], byY[3]).sortedBy { it.x }
        val tl = top[0]     // top, smaller x
        val tr = top[1]     // top, larger x
        val bl = bottom[0]  // bottom, smaller x
        val br = bottom[1]  // bottom, larger x

        // Compute AR from quadrilateral
        val topW = sqrt((tr.x - tl.x) * (tr.x - tl.x) + (tr.y - tl.y) * (tr.y - tl.y))
        val botW = sqrt((br.x - bl.x) * (br.x - bl.x) + (br.y - bl.y) * (br.y - bl.y))
        val leftH = sqrt((bl.x - tl.x) * (bl.x - tl.x) + (bl.y - tl.y) * (bl.y - tl.y))
        val rightH = sqrt((br.x - tr.x) * (br.x - tr.x) + (br.y - tr.y) * (br.y - tr.y))
        val avgW = (topW + botW) / 2.0
        val avgH = (leftH + rightH) / 2.0
        if (avgW <= 0 || avgH <= 0) return null

        val ar = avgW / avgH
        val arError = abs(ar - targetAR) / targetAR
        if (arError > MAX_AR_DEVIATION) return null

        val confidence = 1.0 - arError
        return QuadResult(true, tl, tr, bl, br, ar, confidence.coerceIn(0.0, 1.0))
    }

    private fun scoreQuad(result: QuadResult, areaFraction: Double): Double {
        val arScore = result.confidence
        // Prefer larger area (fills more of the frame = closer to camera)
        val areaScore = (areaFraction / 0.8).coerceAtMost(1.0) // max at 80% of frame
        return arScore * 0.6 + areaScore * 0.4
    }
}

// ── Frame Stabilizer ──

/**
 * Require N consecutive frames with stable page position before triggering.
 * Uses mean position smoothing rather than pairwise comparison — more robust
 * against small frame-to-frame jitter.
 */
class FrameStabilizer(
    private val requiredFrames: Int = 8,
    private val positionTolerancePx: Double = 25.0
) {
    private val ringBuffer = Array<PageFinder.QuadResult?>(requiredFrames) { null }
    private var writeIdx = 0
    private var filledCount = 0

    enum class State { SEARCHING, TRIGGERED }

    fun evaluate(quad: PageFinder.QuadResult?): State {
        if (quad == null || !quad.found) {
            reset()
            return State.SEARCHING
        }

        ringBuffer[writeIdx] = quad
        writeIdx = (writeIdx + 1) % requiredFrames
        if (filledCount < requiredFrames) filledCount++

        if (filledCount < requiredFrames) return State.SEARCHING

        // Check stability: all corners must be within tolerance of mean
        val valid = ringBuffer.filterNotNull()
        if (valid.size < requiredFrames) return State.SEARCHING

        val meanTL = meanPoint(valid.mapNotNull { it.tl })
        val meanBR = meanPoint(valid.mapNotNull { it.br })

        val stable = valid.all { q ->
            val qt = q.tl ?: return@all false
            val qb = q.br ?: return@all false
            val dx = abs(qt.x - meanTL.x) + abs(qb.x - meanBR.x)
            val dy = abs(qt.y - meanTL.y) + abs(qb.y - meanBR.y)
            (dx + dy) < positionTolerancePx * 2
        }

        return if (stable) State.TRIGGERED else State.SEARCHING
    }

    fun progress(): Int = filledCount.coerceAtMost(requiredFrames)

    fun reset() {
        for (i in ringBuffer.indices) ringBuffer[i] = null
        writeIdx = 0
        filledCount = 0
    }

    private fun meanPoint(pts: List<Point>): Point {
        val mx = pts.map { it.x }.average()
        val my = pts.map { it.y }.average()
        return Point(mx, my)
    }
}
