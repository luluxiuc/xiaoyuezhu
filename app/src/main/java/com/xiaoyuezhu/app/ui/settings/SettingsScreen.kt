package com.xiaoyuezhu.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.xiaoyuezhu.app.core.log.AppLogger
import com.xiaoyuezhu.app.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateToTutorial: () -> Unit = {}) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tutorial
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("使用教程", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("快速了解答题卡制作、校准和扫描批改的完整流程。",
                        style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onNavigateToTutorial) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("查看教程")
                    }
                }
            }

            // Export logs
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("使用日志", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("如果应用出现异常，可以导出日志文件帮助排查问题。",
                        style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { context.startActivity(AppLogger.getLogExportIntent(context)) }
                    ) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("导出日志")
                    }
                }
            }

            // Contact
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("联系作者", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "发现问题请添加作者好友，导出日志发送给作者，感谢！",
                        style = MaterialTheme.typography.bodySmall,
                        color = Red500
                    )
                    Spacer(Modifier.height(12.dp))

                    ContactRow("QQ", "2121815491")
                    ContactRow("微信", "w0m0s9")
                    ContactRow("GitHub", "luluxiuc")
                }
            }

            // App info
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("小阅助 v5.0.0", style = MaterialTheme.typography.bodyMedium)
                    Text("离线答题卡批改系统", style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
            }
        }
    }
}

@Composable
private fun ContactRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Blue500)
    }
}
