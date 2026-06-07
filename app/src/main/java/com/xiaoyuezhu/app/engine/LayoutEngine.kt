package com.xiaoyuezhu.app.engine

data class BubblePos(val cx: Float, val cy: Float, val label: String, val isId: Boolean)
data class RowData(
    val y: Float,
    val index: Int,
    val isIdRow: Boolean,
    val bubbles: List<BubblePos>,
    val questionIndices: List<Int> = emptyList()  // which questions are in this row
)
data class LayoutResult(val canvasWidth: Int = 1600, val canvasHeight: Int = 0, val rows: List<RowData>)

class LayoutEngine {
    companion object {
        const val CANVAS_WIDTH = 1800
        const val BORDER_INSET = 10f; const val BORDER_W = 16f
        const val TITLE_H = 64f

        // 90px diameter (0.75× from 120px)
        const val CIRCLE_R = 45f; const val CIRCLE_D = 90f
        const val OPT_GAP = 18f; const val Q_GAP = 72f
        const val MAX_PER_ROW = 3   // try to fit 3, fall back to 2 or 1
        const val ROW_H = 165f

        // ID: vertical columns, same size
        const val ID_V_GAP = 15f; const val ID_COL_GAP = 30f
    }

    fun calculate(spec: PaperSpec): LayoutResult {
        val top = TITLE_H + 80f

        // ── ID: 2 vertical columns on left ──
        val idColH = 10 * CIRCLE_D + 9 * ID_V_GAP
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

        // ── Answer section: adaptive per-row filling ──
        val idRightEdge = idColCX + (spec.studentIdDigits - 1) * (CIRCLE_D + ID_COL_GAP) + CIRCLE_R + 24f
        val ansAvailW = CANVAS_WIDTH - idRightEdge - 20f
        val ansStep = CIRCLE_D + OPT_GAP
        val ansStartY = top + CIRCLE_R + 15f
        val labels = ('A'..'Z').toList()

        // Step 1: Assign questions to rows adaptively
        var qi = 0
        val rowAssignments = mutableListOf<MutableList<Int>>() // row → list of question indices
        while (qi < spec.questionCount) {
            var rowW = 0f
            var count = 0
            val assigned = mutableListOf<Int>()
            // Try to fill this row with up to MAX_PER_ROW questions
            while (qi < spec.questionCount && count < MAX_PER_ROW) {
                val oc = spec.optionCounts.getOrElse(qi) { 4 }
                val qw = oc * ansStep
                val totalW = rowW + qw + (if (count > 0) Q_GAP else 0f)
                if (totalW > ansAvailW && count > 0) break  // doesn't fit, start new row
                assigned.add(qi)
                rowW = totalW
                count++
                qi++
            }
            if (assigned.isEmpty()) {
                // Single question too wide — force it in anyway
                assigned.add(qi)
                qi++
            }
            rowAssignments.add(assigned)
        }

        // Step 2: Compute positions
        val totalRows = rowAssignments.size
        val answerRows = mutableListOf<RowData>()

        for (ri in 0 until totalRows) {
            val ry = ansStartY + ri * ROW_H
            val bubbles = mutableListOf<BubblePos>()
            val indices = rowAssignments[ri]
            var qx = idRightEdge + ((ansAvailW - computeRowWidth(indices, spec, ansStep)) / 2f).coerceAtLeast(0f)

            for (qIdx in indices) {
                val oc = spec.optionCounts.getOrElse(qIdx) { 4 }
                for (o in 0 until oc) {
                    val label = labels.getOrElse(o) { ('A' + o) }.toString()
                    bubbles.add(BubblePos(qx + o * ansStep + CIRCLE_R, ry, label, false))
                }
                qx += oc * ansStep + Q_GAP
            }
            answerRows.add(RowData(ry, ri, false, bubbles, indices))
        }

        val idRow = RowData(0f, -1, true, idBubbles)
        val rows = listOf(idRow) + answerRows
        val lastRow = rows.last()
        val canvasH = (lastRow.y + CIRCLE_R + 75f).toInt()
        val paddedH = maxOf(canvasH.toFloat(), idStartY + idColH + 75f).toInt()
        return LayoutResult(CANVAS_WIDTH, paddedH, rows)
    }

    private fun computeRowWidth(indices: List<Int>, spec: PaperSpec, ansStep: Float): Float {
        var w = 0f
        for ((i, qi) in indices.withIndex()) {
            val oc = spec.optionCounts.getOrElse(qi) { 4 }
            w += oc * ansStep
            if (i > 0) w += Q_GAP
        }
        return w
    }
}
