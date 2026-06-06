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
import com.xiaoyuezhu.app.domain.model.ExamStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExam: (String) -> Unit,
    viewModel: ClassDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(classId) { viewModel.loadClass(classId) }

    // Add student dialog
    if (uiState.showAddStudent) {
        AlertDialog(
            onDismissRequest = { viewModel.hideAddStudent() },
            title = { Text("导入学生") },
            text = {
                Column {
                    Text("每行一个学生姓名，学号自动分配", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.newStudentNames,
                        onValueChange = { viewModel.updateNewStudentNames(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("张三\n李四\n王五") },
                        maxLines = 20
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.addStudents() }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideAddStudent() }) { Text("取消") }
            }
        )
    }

    // Edit student dialog
    uiState.editingStudentId?.let {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = { Text("修改学生") },
            text = {
                OutlinedTextField(
                    value = uiState.editingStudentName,
                    onValueChange = { viewModel.updateEditingStudentName(it) },
                    label = { Text("姓名") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveEditStudent() }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEdit() }) { Text("取消") }
            }
        )
    }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.className.ifEmpty { "班级详情" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("学生 (${uiState.students.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("考试记录 (${uiState.exams.size})") })
            }

            when (selectedTab) {
                0 -> StudentTab(
                    students = uiState.students,
                    onAddStudent = { viewModel.showAddStudent() },
                    onEditStudent = { viewModel.startEditStudent(it) },
                    onDeleteStudent = { viewModel.deleteStudent(it) }
                )
                1 -> ExamTab(
                    exams = uiState.exams,
                    onExamClick = { onNavigateToExam(it) }
                )
            }
        }
    }
}

@Composable
fun StudentTab(
    students: List<com.xiaoyuezhu.app.domain.model.Student>,
    onAddStudent: () -> Unit,
    onEditStudent: (com.xiaoyuezhu.app.domain.model.Student) -> Unit,
    onDeleteStudent: (String) -> Unit
) {
    if (students.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.People,
            title = "暂无学生",
            subtitle = "点击下方按钮导入学生",
            action = {
                OutlinedButton(onClick = onAddStudent) { Text("导入学生") }
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedButton(onClick = onAddStudent, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("导入学生")
                }
            }
            items(students) { student ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(student.id, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                        Text(student.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onEditStudent(student) }) { Icon(Icons.Filled.Edit, "编辑", Modifier.size(20.dp)) }
                        IconButton(onClick = { onDeleteStudent(student.id) }) { Icon(Icons.Filled.Delete, "删除", Modifier.size(20.dp), tint = Red500) }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamTab(
    exams: List<com.xiaoyuezhu.app.domain.model.Exam>,
    onExamClick: (String) -> Unit
) {
    if (exams.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Assignment,
            title = "暂无考试记录",
            subtitle = "批改试卷后自动生成"
        )
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(exams) { exam ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExamClick(exam.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(exam.paperTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            AssistChip(
                                onClick = {},
                                label = { Text(if (exam.status == ExamStatus.COMPLETED) "已完成" else "批改中") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (exam.status == ExamStatus.COMPLETED) Emerald50 else Blue50,
                                    labelColor = if (exam.status == ExamStatus.COMPLETED) Emerald500 else Blue500
                                )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (exam.totalStudents > 0) exam.gradedCount.toFloat() / exam.totalStudents else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = Gray100
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "已批: ${exam.gradedCount}/${exam.totalStudents}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
            }
        }
    }
}
