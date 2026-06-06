package com.xiaoyuezhu.app.ui.tutorial

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyuezhu.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(onDismiss: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 6

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("使用教程") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("退出", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0
                    ) { Text("上一步") }

                    // Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(totalPages) { i ->
                            Box(
                                Modifier
                                    .size(if (i == currentPage) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == currentPage) Blue500
                                        else Gray400.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    if (currentPage < totalPages - 1) {
                        Button(onClick = { currentPage++ }) { Text("下一步") }
                    } else {
                        Button(onClick = onDismiss) { Text("开始使用") }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Blue50, Emerald50, Blue50)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    else
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
            ) { page ->
                TutorialPage(page, modifier = Modifier.padding(24.dp))
            }
        }
    }
}

@Composable
private fun TutorialPage(page: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            val (icon, title, description) = when (page) {
                0 -> Triple(
                    Icons.Filled.School, "欢迎使用小阅助",
                    "100% 离线运行的答题卡批改系统。\n无需网络，保护隐私。\n\n拍照即可自动识别填涂并判分，\n成绩按班级和学号归档。"
                )
                1 -> Triple(
                    Icons.Filled.Description, "答题卡制作",
                    "① 新建答题卡：设置名称、题目数量\n② 为每道题设置选项数（2-8个）和分值\n③ 点选正确答案（支持多选）\n④ 保存后导出打印\n\n题目和选项可随时编辑调整"
                )
                2 -> Triple(
                    Icons.Filled.People, "班级与学生",
                    "① 新建班级，输入班级名称\n② 在班级详情中导入学生名单\n③ 学号从 01 自动递增，支持手动修改\n④ 学生名单用于批改时验证学号"
                )
                3 -> Triple(
                    Icons.Filled.Tune, "答题卡校准（重要！）",
                    "⚠️ 首次使用答题卡必须先校准！\n\n① 打印答题卡后，老师用铅笔填一份\n   标准卷：学号填 00，选择题填正确答案\n② 在答题卡列表点「校准」扫描这份标准卷\n③ 系统自动识别所有气泡位置和正确答案\n④ 校准完成 → 保存为批改模板\n\n为什么必须校准？\n不同打印机缩放比例不同，校准让系统\n从真实打印稿学习气泡位置，消除偏差。"
                )
                4 -> Triple(
                    Icons.Filled.CameraAlt, "扫描批改",
                    "① 在班级列表选择班级和答题卡\n② 将学生答题卡平放在桌面\n③ 手机对准答题卡，保持稳定\n④ 边框变绿并稳定后自动拍照识别\n⑤ 系统自动判分并保存成绩\n\n成绩在班级→考试记录中查看\n可逐题查看对错和得分"
                )
                5 -> Triple(
                    Icons.Filled.Info, "注意事项",
                    "☀️ 光线：请在光线充足均匀的环境下扫描\n📐 平整：答题卡需平放，避免折痕和卷曲\n✏️ 填涂：用铅笔或黑色笔填满气泡\n📱 稳定：手持手机保持稳定，等待自动触发\n🔄 重复：同一学生可重复扫描，选择覆盖\n📤 日志：遇到问题请在设置页导出日志\n    联系作者反馈"
                )
                else -> Triple(Icons.Filled.Help, "", "")
            }

            Icon(
                icon, contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = when (page) {
                    3 -> Orange500
                    5 -> Blue500
                    else -> Blue500
                }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray700,
                lineHeight = 24.sp
            )
        }
    }
}
