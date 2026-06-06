package com.xiaoyuezhu.app.ui.paper

import android.content.Intent
import androidx.compose.foundation.*
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoyuezhu.app.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperEditorScreen(
    paperId: String,
    onNavigateBack: () -> Unit,
    viewModel: PaperViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                title = { Text(if (paperId == "new") "新建答题卡" else "编辑答题卡") },
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
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            // Settings section
            item {
                Text("题目设置", style = MaterialTheme.typography.titleSmall)
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Custom question count — free input text field
                    OutlinedTextField(
                        value = state.questionCountText,
                        onValueChange = { viewModel.updateQuestionCountText(it) },
                        label = { Text("题目数量") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    // Option count — dropdown (2-6)
                    Column(Modifier.weight(1f)) {
                        Text("选项数量", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = "${state.optionCount} 个",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf(2, 3, 4, 5, 6).forEach { n ->
                                    DropdownMenuItem(
                                        text = { Text("$n 个选项") },
                                        onClick = { viewModel.updateOptionCount(n); expanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            // Answer grid
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("正确答案设置", style = MaterialTheme.typography.titleSmall)
                    Text("点击选项切换", style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
            }

            val optionLabels = ('A'..'Z').take(state.optionCount)
            val questionCount = state.questionCount.coerceIn(1, 100)

            itemsIndexed((0 until questionCount).toList()) { _, qIdx ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${qIdx + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            optionLabels.forEach { label ->
                                val isSelected = state.correctAnswers[qIdx]?.contains(label.toString()) ?: false
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Blue500 else Gray200)
                                        .clickable { viewModel.toggleAnswer(qIdx, label.toString()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label.toString(), color = if (isSelected) Color.White else Gray700, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
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
