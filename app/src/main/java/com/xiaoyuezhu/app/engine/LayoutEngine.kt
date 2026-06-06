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

        // 90px diameter (0.75× from 120px)
        const val CIRCLE_R = 45f; const val CIRCLE_D = 90f
        const val OPT_GAP = 18f; const val Q_GAP = 105f; const val Q_PER_ROW = 2
        const val ROW_H = 165f

        // ID: vertical columns, same size
        const val ID_V_GAP = 15f; const val ID_COL_GAP = 30f
    }

    fun calculate(spec: PaperSpec): LayoutResult {
        val top = TITLE_H + 80f

        // ── ID: 2 vertical columns on left ──
        val idColH = 10 * CIRCLE_D + 9 * ID_V_GAP  // 10*90 + 9*15 = 1035
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

        // ── Answer section: per-question option counts ──
        val idRightEdge = idColCX + (spec.studentIdDigits - 1) * (CIRCLE_D + ID_COL_GAP) + CIRCLE_R + 38f
        val ansAvailW = CANVAS_WIDTH - idRightEdge - 30f
        val ansStep = CIRCLE_D + OPT_GAP  // 90+18=108
        val ansStartY = top + CIRCLE_R + 15f

        // Compute total width needed: sum of per-question widths + gaps between them
        val totalRows = ceil(spec.questionCount.toDouble() / Q_PER_ROW).toInt()
        val answerRows = mutableListOf<RowData>()
        val labels = ('A'..'Z').toList()

        // Find max row width to center-align
        var maxRowW = 0f
        val rowLayouts = mutableListOf<List<Float>>() // per-row list of question widths
        for (ri in 0 until totalRows) {
            val qWidths = mutableListOf<Float>()
            var rowW = 0f
            for (q in 0 until Q_PER_ROW) {
                val qi = ri * Q_PER_ROW + q
                if (qi >= spec.questionCount) break
                val oc = spec.optionCounts.getOrElse(qi) { 4 }
                val qw = oc * ansStep  // width of this question's bubbles
                qWidths.add(qw)
                rowW += qw
                if (q > 0) rowW += Q_GAP
            }
            rowLayouts.add(qWidths)
            if (rowW > maxRowW) maxRowW = rowW
        }

        val ansStartX = idRightEdge + ((ansAvailW - maxRowW) / 2f).coerceAtLeast(0f)

        for (ri in 0 until totalRows) {
            val ry = ansStartY + ri * ROW_H
            val bubbles = mutableListOf<BubblePos>()
            var qx = ansStartX
            for (q in 0 until Q_PER_ROW) {
                val qi = ri * Q_PER_ROW + q
                if (qi >= spec.questionCount) break
                val oc = spec.optionCounts.getOrElse(qi) { 4 }
                for (o in 0 until oc) {
                    val label = labels.getOrElse(o) { ('A' + o) }.toString()
                    bubbles.add(BubblePos(qx + o * ansStep + CIRCLE_R, ry, label, false))
                }
                val qw = oc * ansStep
                qx += qw + Q_GAP
            }
            answerRows.add(RowData(ry, ri, false, bubbles))
        }

        val idRow = RowData(0f, -1, true, idBubbles)
        val rows = listOf(idRow) + answerRows
        val lastRow = rows.last()
        val canvasH = (lastRow.y + CIRCLE_R + 75f).toInt()
        val paddedH = maxOf(canvasH.toFloat(), idStartY + idColH + 75f).toInt()
        return LayoutResult(CANVAS_WIDTH, paddedH, rows)
    }
}
