package com.xiaoyuezhu.app.core.log

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import java.io.File

@Composable
fun LogExportDialog(
    title: String = "应用遇到错误",
    message: String,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                text = message,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onExport()
                context.startActivity(AppLogger.getLogExportIntent(context))
                onDismiss()
            }) {
                Text("导出日志")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun CrashLogsDialog(
    crashFiles: List<File>,
    onDismiss: () -> Unit,
    onDelete: (File) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现崩溃日志") },
        text = {
            Text("检测到 ${crashFiles.size} 个崩溃报告。建议导出日志以帮助排查问题。")
        },
        confirmButton = {
            TextButton(onClick = {
                context.startActivity(AppLogger.getLogExportIntent(context))
                onDismiss()
            }) {
                Text("导出全部日志")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                crashFiles.forEach { AppLogger.deleteCrashLog(it) }
                onDismiss()
            }) {
                Text("忽略")
            }
        }
    )
}
