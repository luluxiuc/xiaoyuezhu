package com.xiaoyuezhu.app.ui.exam

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoyuezhu.app.ui.theme.*
import com.xiaoyuezhu.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    examId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Statistics card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("成绩统计", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("平均分", String.format("%.1f", uiState.averageScore))
                            StatItem("最高分", String.format("%.0f", uiState.highestScore))
                            StatItem("最低分", String.format("%.0f", uiState.lowestScore))
                            StatItem("及格率", String.format("%.0f%%", uiState.passRate * 100))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("分数分布", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        uiState.distribution.forEach { (range, count) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(range, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(56.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        val max = uiState.distribution.values.maxOrNull()?.toFloat() ?: 1f
                                        if (max > 0) count.toFloat() / max else 0f
                                    },
                                    modifier = Modifier.weight(1f).height(8.dp),
                                    trackColor = Gray100
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("$count", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp))
                            }
                        }
                    }
                }
            }

            // Grade list
            item {
                Text(
                    "成绩列表",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.grades.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.ListAlt,
                        title = "尚未批改",
                        subtitle = "请先扫描答题卡"
                    )
                }
            } else {
                items(uiState.grades) { grade ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    grade.studentId,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(48.dp)
                                )
                                Text(
                                    uiState.studentNames[grade.studentId] ?: "未知",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${grade.score.toInt()}/${grade.totalScore.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        grade.score / grade.totalScore >= 0.9 -> Emerald500
                                        grade.score / grade.totalScore >= 0.6 -> Blue500
                                        else -> Red500
                                    }
                                )
                                Icon(
                                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("正确: ${grade.correctCount}", color = Emerald500, style = MaterialTheme.typography.bodySmall)
                                    Text("错误: ${grade.wrongCount}", color = Red500, style = MaterialTheme.typography.bodySmall)
                                    Text("未答: ${grade.blankCount}", color = Gray500, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Gray500)
    }
}
