package com.xiaoyuezhu.app.ui.classui

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
fun ClassListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCamera: (String, String) -> Unit,
    viewModel: ClassViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Create class dialog
    if (uiState.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCreateDialog() },
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
                TextButton(onClick = { viewModel.hideCreateDialog() }) { Text("取消") }
            }
        )
    }

    // Select class+paper dialog
    if (uiState.showSelectDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideSelectDialog() },
            title = { Text("开始批改") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("选择班级", style = MaterialTheme.typography.labelMedium)
                    uiState.classes.forEach { cls ->
                        val isSelected = uiState.selectedClassId == cls.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectClass(cls.id) },
                            label = { Text(cls.name) }
                        )
                    }
                    if (uiState.selectedClassId != null) {
                        HorizontalDivider()
                        Text("选择答题卡", style = MaterialTheme.typography.labelMedium)
                        uiState.papers.forEach { paper ->
                            val isSelected = uiState.selectedPaperId == paper.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectPaper(paper.id) },
                                label = { Text(paper.title) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cid = uiState.selectedClassId
                        val pid = uiState.selectedPaperId
                        if (cid != null && pid != null) {
                            viewModel.hideSelectDialog()
                            onNavigateToCamera(cid, pid)
                        }
                    },
                    enabled = uiState.selectedClassId != null && uiState.selectedPaperId != null
                ) { Text("开始扫描") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideSelectDialog() }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("班级管理") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showSelectDialog() },
                icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                text = { Text("开始批改") }
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
            if (uiState.classes.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.School,
                        title = "暂无班级",
                        subtitle = "点击下方按钮创建班级",
                        action = {
                            OutlinedButton(onClick = { viewModel.showCreateDialog() }) {
                                Text("新建班级")
                            }
                        }
                    )
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("我的班级", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { viewModel.showCreateDialog() }) {
                            Icon(Icons.Filled.Add, contentDescription = "新建")
                        }
                    }
                }

                items(uiState.classes) { cls ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetail(cls.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.School, null, tint = Blue500, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(cls.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                viewModel.deleteClass(cls.id)
                            }) {
                                Icon(Icons.Filled.Delete, "删除", tint = Red500)
                            }
                        }
                    }
                }
            }
        }
    }
}
