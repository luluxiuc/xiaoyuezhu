package com.xiaoyuezhu.app.ui.home

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoyuezhu.app.ui.theme.*
import com.xiaoyuezhu.app.ui.components.EmptyState
import com.xiaoyuezhu.app.domain.model.Paper
import com.xiaoyuezhu.app.domain.model.Class

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPaperEditor: (String) -> Unit,
    onNavigateToClassDetail: (String) -> Unit,
    onStartGrading: () -> Unit,
    onCalibratePaper: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小阅助", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabButton(
                    text = "答题卡 (${uiState.paperCount})",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "班级 (${uiState.classCount})",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            // Content
            when (selectedTab) {
                0 -> PapersTab(
                    papers = uiState.papers,
                    onCreateNew = { onNavigateToPaperEditor("new") },
                    onEditPaper = { onNavigateToPaperEditor(it.id) },
                    onCalibrate = { onCalibratePaper(it.id) },
                    onDeletePaper = { viewModel.showDeletePaperDialog() }
                )
                1 -> ClassesTab(
                    classes = uiState.classes,
                    onClassClick = { onNavigateToClassDetail(it.id) },
                    onCreateClass = { viewModel.showCreateClassDialog() }
                )
            }
        }
    }

    // Create class dialog
    if (uiState.showCreateClassDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCreateClassDialog() },
            title = { Text("新建班级") },
            text = {
                OutlinedTextField(
                    value = uiState.newClassName,
                    onValueChange = { viewModel.updateNewClassName(it) },
                    label = { Text("班级名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createClass() }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideCreateClassDialog() }) { Text("取消") }
            }
        )
    }

    // Delete paper dialog
    if (uiState.showDeletePaperDialog) {
        val selectedId = uiState.selectedDeletePaperId
        AlertDialog(
            onDismissRequest = { viewModel.hideDeletePaperDialog() },
            title = { Text("删除答题卡") },
            text = {
                if (uiState.papers.isEmpty()) {
                    Text("没有可删除的答题卡")
                } else {
                    Column {
                        Text("选择要删除的答题卡：", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        uiState.papers.forEach { paper ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectPaperToDelete(paper.id) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedId == paper.id,
                                    onClick = { viewModel.selectPaperToDelete(paper.id) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(paper.title, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text(
                                        if (paper.masterJson.isNotBlank()) "已校准 · 删除将同时清除关联的考试和成绩"
                                        else "未校准",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (paper.masterJson.isNotBlank()) Red500 else Gray500
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeletePaper() },
                    enabled = selectedId != null
                ) {
                    Text("删除", color = if (selectedId != null) Red500 else Gray400)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeletePaperDialog() }) { Text("取消") }
            }
        )
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Blue600 else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) androidx.compose.ui.graphics.Color.White else Gray600
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 4.dp else 0.dp
        )
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun PapersTab(
    papers: List<Paper>,
    onCreateNew: () -> Unit,
    onEditPaper: (Paper) -> Unit,
    onCalibrate: (Paper) -> Unit,
    onDeletePaper: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (papers.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.Description,
                    title = "暂无答题卡",
                    subtitle = "点击下方按钮创建第一份答题卡"
                )
            }
        } else {
            items(papers) { paper ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditPaper(paper) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Description, null, tint = Emerald500, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                paper.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (paper.masterJson.isNotBlank()) {
                                Text("已校准", style = MaterialTheme.typography.labelSmall,
                                    color = Emerald500, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(
                            onClick = { onCalibrate(paper) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (paper.masterJson.isNotBlank()) Emerald500 else Blue500
                            )
                        ) {
                            Text(
                                if (paper.masterJson.isNotBlank()) "已校准" else "校准",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = Gray400, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onCreateNew,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("新建答题卡")
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDeletePaper,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500)
            ) {
                Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("删除答题卡")
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun ClassesTab(
    classes: List<Class>,
    onClassClick: (Class) -> Unit,
    onCreateClass: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (classes.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.School,
                    title = "暂无班级",
                    subtitle = "点击下方按钮创建班级"
                )
            }
        } else {
            items(classes) { cls ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClassClick(cls) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.School, null, tint = Blue500, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            cls.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.ChevronRight, null, tint = Gray400, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onCreateClass,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("新建班级")
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
