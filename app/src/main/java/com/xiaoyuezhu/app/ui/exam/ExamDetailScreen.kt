package com.xiaoyuezhu.app.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoyuezhu.app.ui.theme.*
import com.xiaoyuezhu.app.ui.components.EmptyState
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    examId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val df = remember { DecimalFormat("0.#") }

    LaunchedEffect(examId) { viewModel.loadExam(examId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.exam?.paperTitle ?: "考试详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Statistics card ──
            item {
                Card(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("成绩统计", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("平均分", String.format("%.1f", uiState.averageScore))
                            StatItem("最高分", df.format(uiState.highestScore))
                            StatItem("最低分", df.format(uiState.lowestScore))
                            StatItem("及格率", String.format("%.0f%%", uiState.passRate * 100))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("分数分布", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        uiState.distribution.forEach { (range, count) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(range, style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(56.dp))
                                val max = uiState.distribution.values.maxOrNull()?.toFloat() ?: 1f
                                LinearProgressIndicator(
                                    progress = { if (max > 0) count.toFloat() / max else 0f },
                                    modifier = Modifier.weight(1f).height(8.dp),
                                    trackColor = Gray100
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("$count", style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(24.dp))
                            }
                        }
                    }
                }
            }

            // ── Most wrong questions ──
            if (uiState.mostWrongQuestions.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Red500.copy(alpha = 0.05f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("易错题", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = Red500)
                            Spacer(Modifier.height(8.dp))
                            uiState.mostWrongQuestions.forEach { mwq ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("第${mwq.questionIndex + 1}题",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(56.dp))
                                    LinearProgressIndicator(
                                        progress = { mwq.wrongRate.toFloat() },
                                        modifier = Modifier.weight(1f).height(6.dp),
                                        color = Red500, trackColor = Gray100
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("${mwq.wrongCount}/${mwq.totalAnswers}人错",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray600, modifier = Modifier.width(64.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Grade list ──
            item {
                Text("成绩列表", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
            }

            if (uiState.grades.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Filled.ListAlt,
                        title = "尚未批改", subtitle = "请先扫描答题卡")
                }
            } else {
                items(uiState.grades) { gd ->
                    Card(shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface)) {
                        var expanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(gd.grade.studentId,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(48.dp))
                                Text(gd.studentName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f))
                                Text("${df.format(gd.grade.score)}/${df.format(gd.grade.totalScore)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        gd.grade.score / gd.grade.totalScore >= 0.9 -> Emerald500
                                        gd.grade.score / gd.grade.totalScore >= 0.6 -> Blue500
                                        else -> Red500
                                    })
                                Icon(
                                    if (expanded) Icons.Filled.ExpandLess
                                    else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (expanded) {
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("✓ ${gd.grade.correctCount}",
                                        color = Emerald500,
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("✗ ${gd.grade.wrongCount}",
                                        color = Red500,
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("○ ${gd.grade.blankCount}",
                                        color = Gray500,
                                        style = MaterialTheme.typography.bodySmall)
                                }

                                Spacer(Modifier.height(8.dp))
                                // Per-question indicators
                                Text("答题详情", style = MaterialTheme.typography.labelMedium,
                                    color = Gray600)
                                Spacer(Modifier.height(4.dp))
                                // Show questions in rows of 10
                                val chunked = gd.questionResults.chunked(10)
                                for (chunk in chunked) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        chunk.forEach { qr ->
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            qr.isCorrect -> Emerald500
                                                            qr.isPartial -> Orange500
                                                            qr.isBlank -> Gray400
                                                            else -> Red500
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "${qr.questionIndex + 1}",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                // Legend
                                Row(
                                    Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(8.dp).clip(CircleShape)
                                            .background(Emerald500))
                                        Spacer(Modifier.width(4.dp))
                                        Text("对", fontSize = 10.sp, color = Gray600)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(8.dp).clip(CircleShape)
                                            .background(Orange500))
                                        Spacer(Modifier.width(4.dp))
                                        Text("半对", fontSize = 10.sp, color = Gray600)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(8.dp).clip(CircleShape)
                                            .background(Red500))
                                        Spacer(Modifier.width(4.dp))
                                        Text("错", fontSize = 10.sp, color = Gray600)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(8.dp).clip(CircleShape)
                                            .background(Gray400))
                                        Spacer(Modifier.width(4.dp))
                                        Text("未答", fontSize = 10.sp, color = Gray600)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Gray500)
    }
}
