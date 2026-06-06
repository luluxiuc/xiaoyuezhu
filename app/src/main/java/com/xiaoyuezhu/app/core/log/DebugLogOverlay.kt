package com.xiaoyuezhu.app.core.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogBuffer {
    private val buffer = mutableListOf<String>()
    private const val MAX_LINES = 50

    fun add(line: String) {
        synchronized(buffer) {
            buffer.add(line)
            if (buffer.size > MAX_LINES) buffer.removeAt(0)
        }
    }

    fun getLines(): List<String> = synchronized(buffer) { buffer.toList() }
}

@Composable
fun DebugLogOverlay() {
    var expanded by remember { mutableStateOf(true) }
    var minimized by remember { mutableStateOf(false) }
    val logLines = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    // Poll log buffer every 500ms
    LaunchedEffect(expanded) {
        while (expanded) {
            val lines = DebugLogBuffer.getLines()
            logLines.clear()
            logLines.addAll(lines)
            delay(500)
        }
    }

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty() && expanded && !minimized) {
            listState.animateScrollToItem(logLines.size - 1)
        }
    }

    AnimatedVisibility(
        visible = expanded,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (minimized) Modifier.height(32.dp) else Modifier.height(200.dp))
                .zIndex(999f)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(Color(0xDD000000))
                .clickable { minimized = !minimized }
        ) {
            Column {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF333333))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📋 DEBUG LOG (${logLines.size})",
                        color = Color(0xFFAAAAAA),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row {
                        Text(
                            if (minimized) "▲" else "▼",
                            color = Color(0xFF00FF00),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { minimized = !minimized }
                                .padding(horizontal = 4.dp)
                        )
                        Text(
                            "✕",
                            color = Color(0xFFFF4444),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { expanded = false }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                // Log content
                if (!minimized) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        if (logLines.isEmpty()) {
                            item {
                                Text(
                                    "等待日志...",
                                    color = Color(0xFF666666),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            items(logLines) { line ->
                                val color = when {
                                    line.contains(" E/") -> Color(0xFFFF4444)
                                    line.contains(" W/") -> Color(0xFFFFAA00)
                                    line.contains(" I/") -> Color(0xFF44FF44)
                                    line.contains(" D/") -> Color(0xFF888888)
                                    else -> Color(0xFFCCCCCC)
                                }
                                Text(
                                    text = line,
                                    color = color,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 11.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
