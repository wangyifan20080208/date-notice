package com.smartagenda.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartagenda.AppContainer
import com.smartagenda.ui.board.BoardRoute
import com.smartagenda.ui.detail.TaskDetailScreen
import com.smartagenda.ui.detail.TaskDetailViewModel
import com.smartagenda.ui.settings.SettingsRoute
import com.smartagenda.ui.theme.LocalAgendaColors

private object Routes {
    const val BOARD = "board"
    const val SETTINGS = "settings"
    const val DETAIL = "task/{taskId}"

    fun detail(taskId: Long): String = "task/$taskId"
}

@Composable
fun AgendaNavHost(
    container: AppContainer,
    pendingTaskId: Long?,
    onPendingTaskConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val extras = LocalAgendaColors.current

    val showBottomBar = currentRoute == Routes.BOARD || currentRoute == Routes.SETTINGS

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.BOARD,
                        onClick = {
                            navController.navigate(Routes.BOARD) {
                                popUpTo(Routes.BOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                        },
                        label = { Text("日程") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = extras.accentSoft,
                            unselectedIconColor = extras.textTertiary,
                            unselectedTextColor = extras.textTertiary,
                        ),
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(Routes.BOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(Icons.Filled.Settings, contentDescription = null)
                        },
                        label = { Text("设置") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = extras.accentSoft,
                            unselectedIconColor = extras.textTertiary,
                            unselectedTextColor = extras.textTertiary,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.BOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.BOARD) {
                BoardRoute(
                    container = container,
                    onOpenTask = { taskId -> navController.navigate(Routes.detail(taskId)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsRoute(container = container)
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: return@composable
                val detailViewModel: TaskDetailViewModel = viewModel(
                    key = "detail-$taskId",
                    factory = viewModelFactory {
                        initializer {
                            TaskDetailViewModel(container.taskRepository, taskId)
                        }
                    },
                )
                TaskDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    LaunchedEffect(pendingTaskId) {
        if (pendingTaskId != null) {
            navController.navigate(Routes.detail(pendingTaskId)) { launchSingleTop = true }
            onPendingTaskConsumed()
        }
    }
}