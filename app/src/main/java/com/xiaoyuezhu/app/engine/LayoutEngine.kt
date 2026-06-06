package com.xiaoyuezhu.app.engine

import kotlin.math.ceil

data class BubblePos(val cx: Float, val cy: Float, val label: String, val isId: Boolean)
data class RowData(val y: Float, val index: Int, val isIdRow: Boolean, val bubbles: List<BubblePos>)
data class LayoutResult(val canvasWidth: Int = 1600, val canvasHeight: Int = 0, val rows: List<RowData>)

class LayoutEngine {
    companion object {
        const val CANVAS_WIDTH = 1800
        const val BORDER_INSET = 10f; const val BORDER_W = 16f
        const val TITLE_H = 64f

        // All circles doubled — 120px diameter
        const val CIRCLE_R = 60f; const val CIRCLE_D = 120f; const val CIRCLE_STROKE = 5f
        const val OPT_GAP = 24f; const val Q_GAP = 140f; const val Q_PER_ROW = 2
        const val ROW_H = 220f

        // ID: vertical columns, same size
        const val ID_V_GAP = 20f; const val ID_COL_GAP = 40f
    }

    fun calculate(spec: PaperSpec): LayoutResult {
        val top = TITLE_H + 80f

        // ── ID: 2 vertical columns on left ──
        val idColH = 10 * CIRCLE_D + 9 * ID_V_GAP  // 10*60 + 9*12 = 708
        val idStartY = top
        val idStartX = 52f
        val idColCX = idStartX + CIRCLE_R

        val idBubbles = mutableListOf<BubblePos>()
        for (col in 0 until spec.studentIdDigits) {
            val cx = idColCX + col * (CIRCLE_D + ID_COL_GAP)
            for (row in 0 until 10) {
                val cy = idStartY + row * (CIRCLE_D + ID_V_GAP)
                idBubbles.add(BubblePos(cx, cy, "$row", isId = true))
            }
        }

        // ── Answer section: same circles, right of ID ──
        val idRightEdge = idColCX + (spec.studentIdDigits - 1) * (CIRCLE_D + ID_COL_GAP) + CIRCLE_R + 50f
        val ansAvailW = CANVAS_WIDTH - idRightEdge - 30f
        val ansStep = CIRCLE_D + OPT_GAP  // 60+18=78
        val perQ = spec.optionCount * ansStep - OPT_GAP  // 4*78-18=294
        val ansTotalW = Q_PER_ROW * perQ + (Q_PER_ROW - 1) * Q_GAP  // 4*294+3*100=1476
        val ansStartX = idRightEdge + ((ansAvailW - ansTotalW) / 2f).coerceAtLeast(0f)
        val ansStartY = top + CIRCLE_R + 20f

        val totalRows = ceil(spec.questionCount.toDouble() / Q_PER_ROW).toInt()
        val answerRows = mutableListOf<RowData>()

        for (ri in 0 until totalRows) {
            val ry = ansStartY + ri * ROW_H
            val bubbles = mutableListOf<BubblePos>()
            val l = ('A'..'Z').toList()
            for (q in 0 until Q_PER_ROW) {
                val qi = ri * Q_PER_ROW + q
                if (qi >= spec.questionCount) break
                val qx = ansStartX + q * (perQ + Q_GAP)
                for (o in 0 until spec.optionCount) {
                    bubbles.add(BubblePos(qx + o * ansStep + CIRCLE_R, ry, "${l[o]}", false))
                }
            }
            answerRows.add(RowData(ry, ri, false, bubbles))
        }

        val idRow = RowData(0f, -1, true, idBubbles)
        val rows = listOf(idRow) + answerRows
        val lastRow = rows.last()
        val canvasH = (lastRow.y + CIRCLE_R + 100f).toInt()
        val paddedH = maxOf(canvasH.toFloat(), idStartY + idColH + 100f).toInt()
        return LayoutResult(CANVAS_WIDTH, paddedH, rows)
    }
}
