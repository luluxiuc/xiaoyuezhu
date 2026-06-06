package com.xiaoyuezhu.app.engine

import android.graphics.*
import com.xiaoyuezhu.app.domain.model.TemplateJson

class CanvasDrawer {

    private val borderPaint = Paint().apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 14f; isAntiAlias = true
    }
    private val circlePaint = Paint().apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true
    }
    private val labelPaint = Paint().apply {
        color = Color.BLACK; textSize = 20f; isAntiAlias = true
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val digitPaint = Paint().apply {
        color = Color.BLACK; textSize = 16f; isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val titlePaint = Paint().apply {
        color = Color.BLACK; textSize = 32f; isAntiAlias = true
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val qNumPaint = Paint().apply {
        color = Color.BLACK; textSize = 18f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD
    }
    private val hintPaint = Paint().apply {
        color = Color.parseColor("#BBBBBB"); textSize = 11f; isAntiAlias = true; textAlign = Paint.Align.CENTER
    }

    fun draw(layout: LayoutResult, spec: PaperSpec): Pair<Bitmap, TemplateJson> {
        val w = layout.canvasWidth.toFloat()
        val h = layout.canvasHeight.toFloat()
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        // Border
        c.drawRect(LayoutEngine.BORDER_INSET, LayoutEngine.BORDER_INSET,
            w - LayoutEngine.BORDER_INSET, h - LayoutEngine.BORDER_INSET, borderPaint)

        // Title
        c.drawText("答 题 卡", w / 2f, 44f, titlePaint)

        for (row in layout.rows) {
            if (row.isIdRow) {
                // ID section: vertical columns, same circles as answers
                c.drawText("学号", 34f, row.bubbles.first().cy - 36f,
                    Paint(titlePaint).apply { textSize = 20f; textAlign = Paint.Align.LEFT })
                for (b in row.bubbles) {
                    c.drawCircle(b.cx, b.cy, LayoutEngine.CIRCLE_R, circlePaint)
                    val fm = digitPaint.fontMetrics
                    c.drawText(b.label, b.cx, b.cy - (fm.ascent + fm.descent) / 2f, digitPaint)
                }
                if (spec.studentIdDigits >= 2) {
                    val cols = row.bubbles.groupBy { it.cx }
                    val xs = cols.keys.sorted()
                    if (xs.size >= 2) {
                        c.drawText("十位", xs[0], row.bubbles.last().cy + 18f, hintPaint)
                        c.drawText("个位", xs[1], row.bubbles.last().cy + 18f, hintPaint)
                    }
                }
            } else {
                // Answer row — draw each bubble with its label
                for (b in row.bubbles) {
                    c.drawCircle(b.cx, b.cy, LayoutEngine.CIRCLE_R, circlePaint)
                    c.drawText(b.label, b.cx, b.cy + LayoutEngine.CIRCLE_R + 18f, labelPaint)
                }
                // Draw question numbers using per-question option counts
                var bubbleIdx = 0
                for (q in 0 until LayoutEngine.Q_PER_ROW) {
                    val qi = row.index * LayoutEngine.Q_PER_ROW + q
                    if (qi >= spec.questionCount) break
                    val oc = spec.optionCounts.getOrElse(qi) { 4 }
                    if (bubbleIdx < row.bubbles.size) {
                        val firstBubble = row.bubbles[bubbleIdx]
                        c.drawText("${qi + 1}.", firstBubble.cx - LayoutEngine.CIRCLE_R - 15f,
                            firstBubble.cy + 7f, qNumPaint)
                        bubbleIdx += oc
                    }
                }
            }
        }

        // Build reference positions
        val idCols = mutableListOf<com.xiaoyuezhu.app.domain.model.IdColumnRef>()
        val idRow = layout.rows.firstOrNull { it.isIdRow }
        if (idRow != null) {
            val byX = idRow.bubbles.groupBy { it.cx }.entries.sortedBy { it.key }
            for ((cx, bubbles) in byX) {
                val topY = bubbles.minOf { it.cy } - LayoutEngine.CIRCLE_R
                val botY = bubbles.maxOf { it.cy } + LayoutEngine.CIRCLE_R
                idCols.add(com.xiaoyuezhu.app.domain.model.IdColumnRef(
                    cx - LayoutEngine.CIRCLE_R, topY,
                    LayoutEngine.CIRCLE_D, botY - topY
                ))
            }
        }

        val qRects = mutableListOf<com.xiaoyuezhu.app.domain.model.QuestionRectRef>()
        for (row in layout.rows) {
            if (row.isIdRow) continue
            var bubbleIdx = 0
            for (q in 0 until LayoutEngine.Q_PER_ROW) {
                val qi = row.index * LayoutEngine.Q_PER_ROW + q
                if (qi >= spec.questionCount) break
                val oc = spec.optionCounts.getOrElse(qi) { 4 }
                if (bubbleIdx + oc <= row.bubbles.size) {
                    val group = row.bubbles.subList(bubbleIdx, bubbleIdx + oc)
                    val x0 = group.first().cx - LayoutEngine.CIRCLE_R
                    val x1 = group.last().cx + LayoutEngine.CIRCLE_R
                    val y0 = row.y - LayoutEngine.CIRCLE_R
                    val y1 = row.y + LayoutEngine.CIRCLE_R
                    qRects.add(com.xiaoyuezhu.app.domain.model.QuestionRectRef(
                        qi, x0, y0, x1 - x0, y1 - y0
                    ))
                    bubbleIdx += oc
                }
            }
        }

        val json = TemplateJson(
            version = 6, canvasWidth = layout.canvasWidth, canvasHeight = layout.canvasHeight,
            questionCount = spec.questionCount, optionCount = spec.optionCounts.firstOrNull() ?: 4,
            optionCounts = spec.optionCounts,
            studentIdDigits = spec.studentIdDigits, idDigitCount = 10,
            questionsPerRow = LayoutEngine.Q_PER_ROW,
            idColumns = idCols, questionRects = qRects,
            questionScores = spec.questionScores
        )
        return Pair(bmp, json)
    }
}
