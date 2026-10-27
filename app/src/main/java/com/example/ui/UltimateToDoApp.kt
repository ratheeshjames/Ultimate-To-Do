package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ConfettiBurstOverlay
import com.example.ui.components.FloatingPomodoroPill
import com.example.ui.components.OnboardingTourDialog
import com.example.ui.components.SmartSyntaxHelpDialog
import com.example.ui.components.TaskEditorSheet
import com.example.ui.screens.FocusStudioScreen
import com.example.ui.screens.InsightsAnalyticsScreen
import com.example.ui.screens.SettingsAndDataScreen
import com.example.ui.screens.TasksWorkspaceScreen
import com.example.ui.screens.TemplatesHubScreen
import com.example.ui.theme.UltimateToDoTheme

enum class MainDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TASKS("tasks", "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    FOCUS("focus", "Focus", Icons.Filled.Timer, Icons.Outlined.Timer),
    INSIGHTS("insights", "Insights", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    TEMPLATES("templates", "Templates", Icons.Filled.AutoAwesomeMotion, Icons.Outlined.AutoAwesomeMotion),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun UltimateToDoApp(
    viewModel: TodoViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.snapshot.settings
    var currentDestination by rememberSaveable { mutableStateOf(MainDestination.TASKS) }
    val snackbarHostState = remember { SnackbarHostState() }

    // System Back Handling
    BackHandler(enabled = uiState.filterState.selectedTaskIds.isNotEmpty() || currentDestination != MainDestination.TASKS) {
        if (uiState.filterState.selectedTaskIds.isNotEmpty()) {
            viewModel.clearSelection()
        } else if (currentDestination != MainDestination.TASKS) {
            currentDestination = MainDestination.TASKS
        }
    }

    // Observe Toast / Snackbar events
    LaunchedEffect(uiState.latestToast?.id) {
        val toast = uiState.latestToast ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = toast.message,
            actionLabel = toast.actionLabel,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed && toast.isUndoAction) {
            viewModel.undo()
        }
        viewModel.clearToast()
    }

    UltimateToDoTheme(
        themeMode = settings.theme,
        accentHex = settings.accent
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                            MainDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_tab_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(modifier = Modifier.testTag("side_navigation_rail")) {
                            MainDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_rail_${dest.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        Crossfade(
                            targetState = currentDestination,
                            label = "main_screen_crossfade"
                        ) { destination ->
                            when (destination) {
                                MainDestination.TASKS -> TasksWorkspaceScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onNavigateToFocus = { currentDestination = MainDestination.FOCUS },
                                    onNavigateToTemplates = { currentDestination = MainDestination.TEMPLATES }
                                )
                                MainDestination.FOCUS -> FocusStudioScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                                MainDestination.INSIGHTS -> InsightsAnalyticsScreen(
                                    uiState = uiState
                                )
                                MainDestination.TEMPLATES -> TemplatesHubScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onNavigateToTasks = { currentDestination = MainDestination.TASKS }
                                )
                                MainDestination.SETTINGS -> SettingsAndDataScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }
                        }

                        // Floating Pomodoro Pill when active on non-Focus screens
                        if (uiState.pomodoroState.activeTaskId != null && currentDestination != MainDestination.FOCUS) {
                            val activeTask = uiState.snapshot.tasks.find {
                                it.id == uiState.pomodoroState.activeTaskId
                            }
                            FloatingPomodoroPill(
                                pomodoroState = uiState.pomodoroState,
                                activeTask = activeTask,
                                onTogglePause = { viewModel.togglePomodoroPause() },
                                onStop = { viewModel.stopPomodoro() },
                                onOpenFocusScreen = { currentDestination = MainDestination.FOCUS },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                            )
                        }

                        // Confetti Burst Celebration Overlay
                        ConfettiBurstOverlay(
                            triggerCount = uiState.confettiTriggerCount,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Task Editor Modal Sheet
            val editingTask = uiState.editingTask
            if (editingTask != null) {
                TaskEditorSheet(
                    task = editingTask,
                    onDismiss = { viewModel.closeTaskEditor() },
                    onSave = { title, notes, pri, due, cat, tagsRaw, est, rep, rem, subs ->
                        viewModel.saveEditedTask(
                            taskId = editingTask.id,
                            title = title,
                            notes = notes,
                            priority = pri,
                            due = due,
                            category = cat,
                            tagsRaw = tagsRaw,
                            estimateMinutes = est,
                            repeat = rep,
                            remindWhenDue = rem,
                            subtasks = subs
                        )
                    }
                )
            }

            // Onboarding Tour Modal
            val tourStep = uiState.activeTourStep
            if (tourStep != null) {
                OnboardingTourDialog(
                    stepIndex = tourStep,
                    onNext = { viewModel.nextTourStep() },
                    onSkip = { viewModel.finishTour() }
                )
            }

            // Smart Syntax & Help Modal
            if (uiState.showHelpDialog) {
                SmartSyntaxHelpDialog(
                    onDismiss = { viewModel.setShowHelpDialog(false) },
                    onReplayTour = { viewModel.startTour() }
                )
            }
        }
    }
}
