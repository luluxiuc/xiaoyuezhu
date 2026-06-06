package com.xiaoyuezhu.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaoyuezhu.app.ui.theme.*

@Composable
fun ScoreCircle(
    score: Double,
    totalScore: Double,
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp
) {
    val ratio = if (totalScore > 0) (score / totalScore).toFloat() else 0f
    var animatedRatio by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animatedRatio,
        animationSpec = tween(durationMillis = 1000),
        label = "score_anim"
    )

    LaunchedEffect(ratio) { animatedRatio = ratio }

    val color = when {
        ratio >= 0.9f -> Emerald500
        ratio >= 0.6f -> Blue500
        ratio >= 0.4f -> Orange500
        else -> Red500
    }

    val bgColor = when {
        ratio >= 0.9f -> Emerald50
        ratio >= 0.6f -> Blue50
        ratio >= 0.4f -> androidx.compose.ui.graphics.Color(0xFFFFF7ED)
        else -> Red50
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            drawCircle(color = bgColor, radius = (size / 2).toPx() - stroke / 2)
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${score.toInt()}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "/ ${totalScore.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
    }
}
