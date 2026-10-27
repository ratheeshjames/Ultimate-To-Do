package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.TodoScreenUiState
import com.example.ui.TodoViewModel
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SemanticColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusStudioScreen(
    uiState: TodoScreenUiState,
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val pom = uiState.pomodoroState
    val activeTasks = uiState.snapshot.tasks.filter {
        it.listId == uiState.snapshot.currentListId && !it.archived && !it.done
    }
    val selectedTask = uiState.snapshot.tasks.find { it.id == pom.activeTaskId }
        ?: activeTasks.firstOrNull()

    var selectedPresetMinutes by rememberSaveable { mutableIntStateOf(25) }

    val displaySeconds = if (pom.activeTaskId != null) pom.remainingSeconds else selectedPresetMinutes * 60
    val totalSeconds = if (pom.activeTaskId != null) pom.totalSeconds.coerceAtLeast(1) else selectedPresetMinutes * 60
    val progressFraction by animateFloatAsState(
        targetValue = (displaySeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "focus_dial_progress"
    )

    val minsStr = (displaySeconds / 60).toString().padStart(2, '0')
    val secsStr = (displaySeconds % 60).toString().padStart(2, '0')

    val ringTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val ringActiveColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("focus_studio_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "focus_header") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Pomodoro Focus Studio",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Deep work timer linked directly to your tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expressive Oversized Circular Timer Widget (Reference A & Reference F)
        item(key = "focus_dial") {
            Surface(
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Preset Duration Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45).forEach { mins ->
                            FilterChip(
                                selected = selectedPresetMinutes == mins && pom.activeTaskId == null,
                                onClick = {
                                    selectedPresetMinutes = mins
                                    if (pom.activeTaskId != null && selectedTask != null) {
                                        viewModel.startPomodoro(selectedTask.id, mins * 60)
                                    }
                                },
                                label = { Text("${mins}m") },
                                shape = CircleShape
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.size(230.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokePx = 16.dp.toPx()
                            val inset = strokePx / 2f
                            val arcSize = Size(size.width - strokePx, size.height - strokePx)
                            drawArc(
                                color = ringTrackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringActiveColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progressFraction,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$minsStr:$secsStr",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("focus_timer_display")
                            )
                            Text(
                                text = if (pom.activeTaskId != null) {
                                    if (pom.isRunning) "Focusing…" else "Paused"
                                } else {
                                    "Ready"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (selectedTask != null) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Active Target",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = selectedTask.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "🍅 ${selectedTask.pomodoroCount}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Primary Timer Controls
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (pom.activeTaskId == null) {
                            Button(
                                onClick = {
                                    if (selectedTask != null) {
                                        viewModel.startPomodoro(selectedTask.id, selectedPresetMinutes * 60)
                                    } else {
                                        viewModel.emitToast("Add or select an active task first")
                                    }
                                },
                                enabled = selectedTask != null,
                                shape = CircleShape,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("focus_start_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Focus")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.togglePomodoroPause() },
                                shape = CircleShape,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("focus_pause_resume_button")
                            ) {
                                Icon(
                                    if (pom.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (pom.isRunning) "Pause" else "Resume")
                            }
                            FilledTonalButton(
                                onClick = { viewModel.completePomodoroSessionNow() },
                                shape = CircleShape,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("focus_complete_session_button")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log Session")
                            }
                            OutlinedButton(
                                onClick = { viewModel.stopPomodoro() },
                                shape = CircleShape,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("focus_stop_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            }
        }

        // Task Picker List for Focus
        item(key = "focus_task_picker_header") {
            Text(
                text = "Choose a Task to Focus On",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (activeTasks.isEmpty()) {
            item(key = "focus_no_tasks") {
                Text(
                    text = "No active tasks in this list. Add a task in the Tasks tab to start a Pomodoro session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(activeTasks, key = { it.id }) { task ->
                val isCurrentFocus = pom.activeTaskId == task.id
                ElevatedCard(
                    onClick = { viewModel.startPomodoro(task.id, selectedPresetMinutes * 60) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_select_task_${task.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCurrentFocus) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${task.priority.label} priority · 🍅 ${task.pomodoroCount} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { viewModel.startPomodoro(task.id, selectedPresetMinutes * 60) }
                        ) {
                            Text(if (isCurrentFocus) "Restart" else "Focus")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsightsAnalyticsScreen(
    uiState: TodoScreenUiState,
    modifier: Modifier = Modifier
) {
    val stats = uiState.stats
    val isDark = isSystemInDarkTheme()
    val maxDayCount = remember(stats.weeklyCounts) {
        stats.weeklyCounts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("insights_analytics_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "insights_title") {
            Column {
                Text(
                    text = "Insights & Momentum",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Completion rate, priority balance, and weekly activity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bento Row 1: Oversized Completion Card + Streak/Sessions Column (Reference B)
        item(key = "bento_metrics_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(176.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "COMPLETION",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${stats.completionPercent}%",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.testTag("insights_completion_value")
                        )
                        Text(
                            text = "${stats.done} of ${stats.total} tasks done",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("STREAK", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${stats.streakDays}d",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.testTag("insights_streak_value")
                                )
                            }
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("FOCUS", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "🍅 ${stats.pomodoroSessions}",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.testTag("insights_pomodoro_value")
                                )
                            }
                            Icon(Icons.Outlined.Timer, contentDescription = null)
                        }
                    }
                }
            }
        }

        // Archived count pill card
        item(key = "archived_summary_card") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Archive, contentDescription = null)
                        Text("Archived in current list", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = stats.archivedCount.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.testTag("insights_archived_value")
                    )
                }
            }
        }

        // Open Tasks by Priority Breakdown
        item(key = "priority_breakdown_card") {
            ElevatedCard(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalOpen = (stats.openHigh + stats.openMed + stats.openLow).coerceAtLeast(1)
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Open by Priority",
                        style = MaterialTheme.typography.titleLarge
                    )
                    PriorityBarRow(
                        label = "High Priority",
                        count = stats.openHigh,
                        fraction = stats.openHigh.toFloat() / totalOpen,
                        color = if (isDark) SemanticColors.PriorityHighDark else SemanticColors.PriorityHighLight
                    )
                    PriorityBarRow(
                        label = "Medium Priority",
                        count = stats.openMed,
                        fraction = stats.openMed.toFloat() / totalOpen,
                        color = if (isDark) SemanticColors.PriorityMedDark else SemanticColors.PriorityMedLight
                    )
                    PriorityBarRow(
                        label = "Low Priority",
                        count = stats.openLow,
                        fraction = stats.openLow.toFloat() / totalOpen,
                        color = if (isDark) SemanticColors.PriorityLowDark else SemanticColors.PriorityLowLight
                    )
                }
            }
        }

        // Completed This Week 7-Day Bar Chart
        item(key = "weekly_chart_card") {
            ElevatedCard(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_chart_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Completed This Week",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${stats.weeklyCounts.sumOf { it.count }} total",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        stats.weeklyCounts.forEach { day ->
                            val heightFraction = (day.count.toFloat() / maxDayCount.toFloat()).coerceIn(0.06f, 1f)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = day.count.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.65f)
                                        .fillMaxHeight(heightFraction * 0.72f)
                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                        .background(
                                            if (day.isToday) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day.dayShortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityBarRow(
    label: String,
    count: Int,
    fraction: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
        )
    }
}
