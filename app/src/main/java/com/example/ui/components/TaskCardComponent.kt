package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.TaskDomainLogic
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.TaskItem
import com.example.ui.theme.SemanticColors

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TaskCardComponent(
    task: TaskItem,
    dense: Boolean,
    dateFormatLocale: String,
    isSelected: Boolean,
    selectionModeActive: Boolean,
    showManualReorderControls: Boolean,
    onToggleDone: () -> Unit,
    onToggleSelect: () -> Unit,
    onToggleStar: () -> Unit,
    onStartPomodoro: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onArchiveOrRestore: () -> Unit,
    onDelete: () -> Unit,
    onToggleSubtask: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isOverdue = remember(task.due, task.done) { TaskDomainLogic.isOverdue(task) }
    var menuExpanded by remember { mutableStateOf(false) }

    val (priorityColor, priorityContainer) = when (task.priority) {
        Priority.HIGH -> if (isDark) {
            SemanticColors.PriorityHighDark to SemanticColors.PriorityHighContainerDark
        } else {
            SemanticColors.PriorityHighLight to SemanticColors.PriorityHighContainerLight
        }
        Priority.MED -> if (isDark) {
            SemanticColors.PriorityMedDark to SemanticColors.PriorityMedContainerDark
        } else {
            SemanticColors.PriorityMedLight to SemanticColors.PriorityMedContainerLight
        }
        Priority.LOW -> if (isDark) {
            SemanticColors.PriorityLowDark to SemanticColors.PriorityLowContainerDark
        } else {
            SemanticColors.PriorityLowLight to SemanticColors.PriorityLowContainerLight
        }
    }

    val cardBorder = when {
        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isOverdue -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
        task.starred && !task.done -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        task.done -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surface
    }

    val verticalPad = if (dense) 8.dp else 14.dp
    val horizontalPad = if (dense) 12.dp else 16.dp

    Surface(
        shape = RoundedCornerShape(if (dense) 16.dp else 22.dp),
        color = containerColor,
        border = cardBorder,
        tonalElevation = if (task.done) 0.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionModeActive) onToggleSelect() else onEdit()
                },
                onLongClick = onToggleSelect
            )
            .testTag("task_item_${task.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = horizontalPad, vertical = verticalPad)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectionModeActive || isSelected) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.testTag("select_task_${task.id}")
                    )
                }

                IconButton(
                    onClick = onToggleDone,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Complete ${task.title}" }
                        .testTag("toggle_task_${task.id}")
                ) {
                    Icon(
                        imageVector = if (task.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (task.done) "Completed" else "Mark complete",
                        tint = if (task.done) MaterialTheme.colorScheme.primary else priorityColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        Text(
                            text = task.title,
                            style = if (dense) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            else MaterialTheme.typography.titleMedium,
                            color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    if (task.notes.isNotBlank() && !dense) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Star button
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("star_task_${task.id}")
                ) {
                    Icon(
                        imageVector = if (task.starred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (task.starred) "Unpin task" else "Pin task",
                        tint = if (task.starred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Overflow actions menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("more_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Task options"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Start Focus Timer (25m)") },
                            leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onStartPomodoro()
                            },
                            modifier = Modifier.testTag("menu_pomodoro_${task.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Task") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            modifier = Modifier.testTag("menu_edit_${task.id}")
                        )
                        DropdownMenuItem(
                            text = { Text(if (isSelected) "Deselect" else "Select for Bulk Action") },
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onToggleSelect()
                            },
                            modifier = Modifier.testTag("menu_select_${task.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            },
                            modifier = Modifier.testTag("menu_duplicate_${task.id}")
                        )
                        if (showManualReorderControls) {
                            DropdownMenuItem(
                                text = { Text("Move Up") },
                                leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onMoveUp()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move Down") },
                                leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onMoveDown()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (task.archived) "Restore from Archive" else "Archive") },
                            leadingIcon = {
                                Icon(
                                    if (task.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onArchiveOrRestore()
                            },
                            modifier = Modifier.testTag("menu_archive_${task.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            modifier = Modifier.testTag("menu_delete_${task.id}")
                        )
                    }
                }
            }

            // Metadata chips row
            val hasMetadata = task.due.isNotBlank() ||
                task.category.isNotBlank() ||
                task.tags.isNotEmpty() ||
                task.repeat != RepeatRule.NONE ||
                task.estimateMinutes > 0 ||
                task.remindWhenDue ||
                task.pomodoroCount > 0

            if (hasMetadata) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    // Priority Pill
                    MetadataPill(
                        text = task.priority.label,
                        containerColor = priorityContainer,
                        contentColor = priorityColor
                    )

                    if (task.due.isNotBlank()) {
                        val dueText = TaskDomainLogic.formatDueDisplay(task.due, dateFormatLocale) +
                            if (isOverdue) " · Overdue" else ""
                        MetadataPill(
                            text = dueText,
                            containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (isOverdue) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        )
                    }

                    if (task.category.isNotBlank()) {
                        MetadataPill(
                            text = "@${task.category}",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    task.tags.forEach { tag ->
                        MetadataPill(
                            text = "#$tag",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (task.repeat != RepeatRule.NONE) {
                        MetadataPill(
                            text = task.repeat.label,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        )
                    }

                    if (task.estimateMinutes > 0) {
                        MetadataPill(
                            text = "${task.estimateMinutes}m",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (task.remindWhenDue) {
                        MetadataPill(
                            text = "Remind",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        )
                    }

                    if (task.pomodoroCount > 0) {
                        MetadataPill(
                            text = "🍅 ${task.pomodoroCount}",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Subtasks progress and interactive list
            AnimatedVisibility(visible = task.subtasks.isNotEmpty()) {
                val doneSubs = task.subtasks.count { it.done }
                val totalSubs = task.subtasks.size
                val progress = if (totalSubs > 0) doneSubs.toFloat() / totalSubs.toFloat() else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 8.dp, end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$doneSubs/$totalSubs subtasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    if (!dense) {
                        Spacer(modifier = Modifier.height(6.dp))
                        task.subtasks.forEachIndexed { idx, sub ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(onClick = { onToggleSubtask(idx) })
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (sub.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (sub.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sub.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (sub.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (sub.done) TextDecoration.LineThrough else TextDecoration.None
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
private fun MetadataPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
