package com.xiaoyuezhu.app.ui.paper

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoyuezhu.app.ui.theme.*
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperEditorScreen(
    paperId: String,
    onNavigateBack: () -> Unit,
    viewModel: PaperViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val df = remember { DecimalFormat("0.#") }

    LaunchedEffect(paperId) { viewModel.loadPaper(paperId) }

    state.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("确定") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (paperId == "new") "新建答题卡" else "编辑答题卡")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.savePaper() }, enabled = !state.isSaving) {
                        if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Save, "保存")
                    }
                }
            )
        },
        bottomBar = {
            if (state.generateResult != null) {
                Surface(tonalElevation = 4.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                val file = File(state.generateResult!!.imagePath)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "导出答题卡"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("导出打印")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("答题卡名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Settings row: question count + total score
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.questionCountText,
                        onValueChange = { viewModel.updateQuestionCountText(it) },
                        label = { Text("题目数量") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Blue500.copy(alpha = 0.1f))
                    ) {
                        Text(
                            "总分: ${df.format(state.totalScore)}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Blue500
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            // Answer grid header
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("正确答案设置", style = MaterialTheme.typography.titleSmall)
                    Text("点击选项切换多选 · 得分可为小数",
                        style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
            }

            // Per-question cards
            val questionCount = state.questionCount.coerceIn(1, 200)
            val optionLabels = ('A'..'Z').toList()

            itemsIndexed((0 until questionCount).toList()) { _, qIdx ->
                val optCount = state.optionCounts[qIdx] ?: 4
                val labels = optionLabels.take(optCount)
                val score = state.questionScores[qIdx] ?: 5.0
                val selected = state.correctAnswers[qIdx] ?: emptyList()
                var showScoreEdit by remember { mutableStateOf(false) }
                var scoreText by remember(score) { mutableStateOf(df.format(score)) }

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        // Row 1: question number + option count + score
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${qIdx + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(28.dp))

                            // Option count selector
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Gray200, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.setQuestionOptionCount(qIdx, optCount - 1) },
                                    enabled = optCount > 2,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, null, Modifier.size(14.dp))
                                }
                                Text("${optCount}选项", fontSize = 12.sp,
                                    color = Gray600, textAlign = TextAlign.Center,
                                    modifier = Modifier.width(44.dp))
                                IconButton(
                                    onClick = { viewModel.setQuestionOptionCount(qIdx, optCount + 1) },
                                    enabled = optCount < 8,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Add, null, Modifier.size(14.dp))
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // Score with +/- 0.5 buttons
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Gray200, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.setQuestionScore(qIdx, (score - 0.5).coerceAtLeast(0.0)) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, null, Modifier.size(14.dp))
                                }

                                if (showScoreEdit) {
                                    OutlinedTextField(
                                        value = scoreText,
                                        onValueChange = { v ->
                                            scoreText = v
                                            v.toDoubleOrNull()?.let { viewModel.setQuestionScore(qIdx, it) }
                                        },
                                        modifier = Modifier.width(56.dp).height(44.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                                    )
                                    IconButton(
                                        onClick = { showScoreEdit = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Check, null, Modifier.size(14.dp),
                                            tint = Emerald500)
                                    }
                                } else {
                                    TextButton(
                                        onClick = { showScoreEdit = true; scoreText = df.format(score) },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("${df.format(score)}分", fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium)
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.setQuestionScore(qIdx, score + 0.5) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Add, null, Modifier.size(14.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Row 2: option toggle buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            labels.forEach { label ->
                                val isSelected = selected.contains(label.toString())
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Blue500 else Gray200)
                                        .clickable { viewModel.toggleAnswer(qIdx, label.toString()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label.toString(),
                                        color = if (isSelected) Color.White else Gray700,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
