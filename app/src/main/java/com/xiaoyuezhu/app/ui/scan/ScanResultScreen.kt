package com.xiaoyuezhu.app.ui.scan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xiaoyuezhu.app.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    classId: String,
    paperId: String,
    studentId: String,
    score: Double = 0.0,
    total: Double = 0.0,
    correct: Int = 0,
    wrong: Int = 0,
    blank: Int = 0,
    onContinueScan: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val df = remember { DecimalFormat("0.#") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("批改结果") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Emerald500,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "批改完成",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("学号", style = MaterialTheme.typography.labelLarge, color = Gray500)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        studentId,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(20.dp))

                    // Score display
                    Text("得分", style = MaterialTheme.typography.labelLarge, color = Gray500)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${df.format(score)} / ${df.format(total)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            total > 0 && score / total >= 0.9 -> Emerald500
                            total > 0 && score / total >= 0.6 -> Blue500
                            else -> Red500
                        }
                    )

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(20.dp))

                    // Detail stats
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBadge("正确", correct, Emerald500)
                        StatBadge("错误", wrong, Red500)
                        StatBadge("未答", blank, Gray500)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Continue / Home buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateHome,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("返回主页")
                }
                Button(
                    onClick = onContinueScan,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("继续批改")
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$count",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = Gray500)
    }
}
