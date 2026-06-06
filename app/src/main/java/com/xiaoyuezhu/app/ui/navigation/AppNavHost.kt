package com.xiaoyuezhu.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaoyuezhu.app.ui.home.HomeScreen
import com.xiaoyuezhu.app.ui.classui.ClassListScreen
import com.xiaoyuezhu.app.ui.classui.ClassDetailScreen
import com.xiaoyuezhu.app.ui.paper.PaperEditorScreen
import com.xiaoyuezhu.app.ui.camera.CameraScreen
import com.xiaoyuezhu.app.ui.scan.ScanResultScreen
import com.xiaoyuezhu.app.ui.exam.ExamDetailScreen
import com.xiaoyuezhu.app.ui.settings.SettingsScreen

data class BottomNavItem(
    val route: String, val label: String,
    val selectedIcon: ImageVector, val unselectedIcon: ImageVector,
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem(Routes.HOME, "首页", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem(Routes.GRADING, "批改", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
        BottomNavItem(Routes.SETTINGS, "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) },
                            label = { Text(item.label) }, selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)) {

            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToPaperEditor = { navController.navigate(Routes.paperEditor(it)) },
                    onNavigateToClassDetail = { navController.navigate(Routes.classDetail(it)) },
                    onStartGrading = { navController.navigate(Routes.GRADING) },
                    onCalibratePaper = { navController.navigate(Routes.cameraCalibrate(it)) }
                )
            }

            composable(Routes.GRADING) {
                ClassListScreen(
                    onNavigateToDetail = { navController.navigate(Routes.classDetail(it)) },
                    onNavigateToCamera = { cid, pid -> navController.navigate(Routes.cameraScan(cid, pid)) }
                )
            }

            composable(Routes.SETTINGS) { SettingsScreen() }

            composable(Routes.CLASS_DETAIL, arguments = listOf(
                navArgument("classId") { type = NavType.StringType })) { entry ->
                ClassDetailScreen(classId = entry.arguments?.getString("classId") ?: "",
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToExam = { navController.navigate(Routes.examDetail(it)) })
            }

            composable(Routes.PAPER_EDITOR, arguments = listOf(
                navArgument("paperId") { type = NavType.StringType })) { entry ->
                PaperEditorScreen(paperId = entry.arguments?.getString("paperId") ?: "",
                    onNavigateBack = { navController.popBackStack() })
            }

            // Calibration camera (standalone, no class needed)
            composable(Routes.CAMERA_CALIBRATE, arguments = listOf(
                navArgument("paperId") { type = NavType.StringType })) { entry ->
                CameraScreen(
                    classId = "", paperId = entry.arguments?.getString("paperId") ?: "",
                    isCalibration = true,
                    onNavigateBack = { navController.popBackStack() },
                    onScanComplete = { navController.popBackStack() }
                )
            }

            // Grading camera
            composable(Routes.CAMERA_SCAN, arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("paperId") { type = NavType.StringType })) { entry ->
                CameraScreen(
                    classId = entry.arguments?.getString("classId") ?: "",
                    paperId = entry.arguments?.getString("paperId") ?: "",
                    onNavigateBack = { navController.popBackStack() },
                    onScanComplete = { sid ->
                        val cid = entry.arguments?.getString("classId") ?: ""
                        val pid = entry.arguments?.getString("paperId") ?: ""
                        navController.navigate(Routes.scanResult(cid, pid, sid)) {
                            popUpTo(Routes.CAMERA_SCAN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SCAN_RESULT, arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("paperId") { type = NavType.StringType },
                navArgument("studentId") { type = NavType.StringType })) { entry ->
                ScanResultScreen(
                    classId = entry.arguments?.getString("classId") ?: "",
                    paperId = entry.arguments?.getString("paperId") ?: "",
                    studentId = entry.arguments?.getString("studentId") ?: "",
                    onContinueScan = {
                        val cid = entry.arguments?.getString("classId") ?: ""
                        val pid = entry.arguments?.getString("paperId") ?: ""
                        navController.navigate(Routes.cameraScan(cid, pid)) { popUpTo(Routes.HOME) }
                    },
                    onNavigateHome = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
                )
            }

            composable(Routes.EXAM_DETAIL, arguments = listOf(
                navArgument("examId") { type = NavType.StringType })) { entry ->
                ExamDetailScreen(examId = entry.arguments?.getString("examId") ?: "",
                    onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
