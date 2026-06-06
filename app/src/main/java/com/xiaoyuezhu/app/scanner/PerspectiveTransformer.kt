package com.xiaoyuezhu.app.scanner

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import timber.log.Timber

/**
 * Perspective correction: warp the detected page quadrilateral to a
 * normalized rectangular canvas for downstream processing.
 *
 * Target width is fixed; height is derived from the template's aspect ratio
 * so bubble proportions are consistent between calibration and scan.
 */
object PerspectiveTransformer {

    /** Target width for the warped output. Higher = more detail for bubble reading. */
    const val TARGET_WIDTH = 1200

    data class WarpResult(
        val warped: Mat,
        val matrix: Mat,
        val dstWidth: Int,
        val dstHeight: Int
    )

    /**
     * Warp the grayscale image using the detected quadrilateral corners.
     *
     * @param src source grayscale Mat
     * @param quad detected page corners (TL, TR, BL, BR)
     * @param targetAR target aspect ratio (width/height), from template
     */
    fun warp(src: Mat, quad: PageFinder.QuadResult, targetAR: Double): WarpResult {
        val dstW = TARGET_WIDTH
        val dstH = (TARGET_WIDTH.toDouble() / targetAR).toInt().coerceAtLeast(100)

        val srcPts = MatOfPoint2f(quad.tl, quad.tr, quad.bl, quad.br)
        val dstPts = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(dstW - 1.0, 0.0),
            Point(0.0, dstH - 1.0),
            Point(dstW - 1.0, dstH - 1.0)
        )

        val matrix = Imgproc.getPerspectiveTransform(srcPts, dstPts)
        val warped = Mat()
        Imgproc.warpPerspective(src, warped, matrix, Size(dstW.toDouble(), dstH.toDouble()),
            Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar(255.0))

        Timber.d("透视变换: ${src.cols()}x${src.rows()} → ${warped.cols()}x${warped.rows()} " +
                "targetAR=%.3f".format(targetAR))
        return WarpResult(warped, matrix, dstW, dstH)
    }

    /**
     * Warp to a specific target size (from stored MasterTemplate dimensions).
     * Used during scanning to maintain exact positional consistency with calibration.
     */
    fun warpToSize(
        src: Mat, quad: PageFinder.QuadResult, dstW: Int, dstH: Int
    ): Mat {
        val srcPts = MatOfPoint2f(quad.tl, quad.tr, quad.bl, quad.br)
        val dstPts = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(dstW - 1.0, 0.0),
            Point(0.0, dstH - 1.0),
            Point(dstW - 1.0, dstH - 1.0)
        )
        val matrix = Imgproc.getPerspectiveTransform(srcPts, dstPts)
        val warped = Mat()
        Imgproc.warpPerspective(src, warped, matrix, Size(dstW.toDouble(), dstH.toDouble()),
            Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar(255.0))
        return warped
    }

    /** Transform a point from source space to warped space */
    fun transformPoint(matrix: Mat, point: Point): Point {
        val src = MatOfPoint2f(point)
        val dst = MatOfPoint2f()
        Core.perspectiveTransform(src, dst, matrix)
        return dst.toList()[0]
    }
}
