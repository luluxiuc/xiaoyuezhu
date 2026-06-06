package com.xiaoyuezhu.app.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun AnchorOverlay(
    borderFound: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val margin = 0.04f * w
        val color = if (borderFound) Color(0xFF10B981) else Color(0xFFEF4444)

        // Draw a dashed-ish border outline showing the detection search area
        drawRect(
            color = color.copy(alpha = 0.4f),
            topLeft = Offset(margin, margin),
            size = androidx.compose.ui.geometry.Size(w - 2 * margin, h - 2 * margin),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}
