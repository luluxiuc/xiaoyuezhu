package com.xiaoyuezhu.app.scanner

import androidx.camera.core.ImageProxy
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Converts CameraX ImageProxy to OpenCV Mat.
 *
 * Direct Y-plane extraction from YUV_420_888 — avoids the NV21 interleaving
 * copy-paste dance. Handles EXIF rotation and pixel stride correctly.
 */
object ScanFrameConverter {

    /**
     * Extract grayscale Mat from ImageProxy.
     * Returns null if image is unavailable or conversion fails.
     */
    fun toGray(proxy: ImageProxy): Mat? {
        return try {
            val image = proxy.image ?: return null
            val planes = image.planes
            if (planes.isEmpty()) return null

            val w = image.width
            val h = image.height

            // Plane[0] is Y (luminance) — already grayscale
            val yPlane = planes[0]
            val yBuffer = yPlane.buffer
            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride

            val gray = Mat(h, w, CvType.CV_8UC1)

            if (yPixelStride == 1 && yRowStride == w) {
                // Fast path: contiguous Y plane
                val data = ByteArray(yBuffer.remaining())
                yBuffer.get(data)
                gray.put(0, 0, data)
            } else {
                // Slow path: extract row by row (handles stride/padding)
                val rowBuf = ByteArray(w)
                for (row in 0 until h) {
                    yBuffer.position(row * yRowStride)
                    if (yPixelStride == 1) {
                        yBuffer.get(rowBuf, 0, w)
                    } else {
                        for (col in 0 until w) {
                            yBuffer.position(row * yRowStride + col * yPixelStride)
                            rowBuf[col] = if (yBuffer.hasRemaining()) yBuffer.get() else 0
                        }
                    }
                    gray.put(row, 0, rowBuf)
                }
            }

            // Handle EXIF rotation
            val rotation = proxy.imageInfo.rotationDegrees
            if (rotation != 0) {
                val rotated = rotateMat(gray, rotation)
                gray.release()
                rotated
            } else {
                gray
            }
        } catch (e: Exception) {
            Timber.e(e, "帧转换失败")
            null
        }
    }

    /**
     * Rotate the image to match display orientation.
     *
     * imageInfo.rotationDegrees = CW rotation needed for sensor→display alignment.
     * OpenCV getRotationMatrix2D rotates CCW, so we convert: CCW = (360 - CW) % 360.
     */
    private fun rotateMat(src: Mat, cwDegrees: Int): Mat {
        if (cwDegrees == 0) return src.clone()

        // Convert clockwise to counter-clockwise for OpenCV
        val ccwDegrees = (360 - cwDegrees) % 360
        val center = Point((src.cols() / 2).toDouble(), (src.rows() / 2).toDouble())
        val matrix = Imgproc.getRotationMatrix2D(center, ccwDegrees.toDouble(), 1.0)
        val dst = Mat()
        val size = if (ccwDegrees == 90 || ccwDegrees == 270) {
            Size(src.rows().toDouble(), src.cols().toDouble())
        } else {
            Size(src.cols().toDouble(), src.rows().toDouble())
        }
        Imgproc.warpAffine(src, dst, matrix, size, Imgproc.INTER_LINEAR,
            Core.BORDER_CONSTANT, Scalar(255.0))
        return dst
    }
}
