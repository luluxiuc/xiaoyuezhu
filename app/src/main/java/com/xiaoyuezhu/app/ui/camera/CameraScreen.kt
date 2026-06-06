package com.xiaoyuezhu.app.ui.camera

import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoyuezhu.app.ui.theme.*

@Composable
fun CameraScreen(
    classId: String, paperId: String,
    isCalibration: Boolean = false,
    onNavigateBack: () -> Unit,
    onScanComplete: (String) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(classId, paperId, isCalibration) { viewModel.init(classId, paperId, isCalibration) }

    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().setTargetResolution(Size(1280, 720)).build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also {
                            it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                viewModel.processFrame(proxy); proxy.close()
                            }
                        }
                    try { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) }
                    catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Border detection overlay
        AnchorOverlay(borderFound = uiState.borderFound)

        // Progress bar
        if (uiState.status == ScanStatus.SEARCHING && uiState.triggerProgress > 0) {
            LinearProgressIndicator(
                progress = { uiState.triggerProgress.toFloat() / 12f },
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(4.dp),
                color = Emerald500, trackColor = Color.Transparent
            )
        }

        // Top status bar
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                Icon(Icons.Filled.ArrowBack, "返回", tint = Color.White)
            }
            Text(
                when (uiState.status) {
                    ScanStatus.SEARCHING -> if (uiState.borderFound) "已找到边框 ${uiState.triggerProgress}/10" else if (uiState.isCalibration) "请对准校准答题卡" else "请对准答题卡边框"
                    ScanStatus.PROCESSING -> "处理中..."
                    ScanStatus.CALIBRATED -> "校准完成!"
                    else -> ""
                },
                color = Color.White, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Box(Modifier.size(48.dp))
        }

        // Processing
        if (uiState.status == ScanStatus.PROCESSING) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text("正在处理...", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Error
        uiState.errorMessage?.let { msg ->
            AlertDialog(onDismissRequest = { viewModel.clearError() }, title = { Text("扫描失败") }, text = { Text(msg) },
                confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("重新扫描") } },
                dismissButton = { TextButton(onClick = onNavigateBack) { Text("返回") } })
        }

        // Duplicate
        if (uiState.status == ScanStatus.DUPLICATE) {
            AlertDialog(onDismissRequest = {}, title = { Text("重复扫描") },
                text = { Text("学号 ${uiState.studentId} 已有成绩。\n得分: ${uiState.score.toInt()}分") },
                confirmButton = { TextButton(onClick = { viewModel.resetForNextScan(); onScanComplete(uiState.studentId) }) { Text("跳过") } },
                dismissButton = { TextButton(onClick = { viewModel.resetForNextScan() }) { Text("覆盖") } })
        }

        // Success / Calibrated
        LaunchedEffect(uiState.status) {
            when (uiState.status) {
                ScanStatus.SUCCESS -> onScanComplete(uiState.studentId)
                ScanStatus.CALIBRATED -> {
                    onNavigateBack()
                }
                else -> {}
            }
        }
    }
}
