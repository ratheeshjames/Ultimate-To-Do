package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.TaskDomainLogic
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.Subtask
import com.example.model.TaskItem
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    task: TaskItem,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        priority: Priority,
        due: String,
        category: String,
        tagsRaw: String,
        estimateMinutes: Int,
        repeat: RepeatRule,
        remindWhenDue: Boolean,
        subtasks: List<Subtask>
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(task.id) { mutableStateOf(task.title) }
    var notes by remember(task.id) { mutableStateOf(task.notes) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }
    var dueDatePart by remember(task.id) { mutableStateOf(task.due.take(10)) }
    var dueTimePart by remember(task.id) {
        mutableStateOf(if (task.due.length > 10) task.due.substring(11).take(5) else "")
    }
    var category by remember(task.id) { mutableStateOf(task.category) }
    var tagsRaw by remember(task.id) { mutableStateOf(task.tags.joinToString(", ")) }
    var estimateText by remember(task.id) {
        mutableStateOf(if (task.estimateMinutes > 0) task.estimateMinutes.toString() else "")
    }
    var repeat by remember(task.id) { mutableStateOf(task.repeat) }
    var remindWhenDue by remember(task.id) { mutableStateOf(task.remindWhenDue) }
    val subtasks = remember(task.id) {
        mutableStateListOf<Subtask>().apply { addAll(task.subtasks) }
    }
    var newSubtaskTitle by remember(task.id) { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Task",
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_title_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_notes_input")
            )

            // Priority Selector
            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.label) },
                        modifier = Modifier.testTag("edit_priority_${p.key}")
                    )
                }
            }

            // Due Date & Time
            Text(
                text = "Due Date & Time",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                dueDatePart = "$y-${(m + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.testTag("edit_pick_date_button")
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(dueDatePart.ifBlank { "Pick Date" })
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                if (dueDatePart.isBlank()) {
                                    dueDatePart = TaskDomainLogic.formatDateYmd(Date())
                                }
                                dueTimePart = "${h.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}"
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    modifier = Modifier.testTag("edit_pick_time_button")
                ) {
                    Icon(Icons.Outlined.AccessTime, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(dueTimePart.ifBlank { "Pick Time" })
                }

                if (dueDatePart.isNotBlank()) {
                    TextButton(
                        onClick = {
                            dueDatePart = ""
                            dueTimePart = ""
                        }
                    ) {
                        Text("Clear Due")
                    }
                }
            }

            // Category & Tags & Estimate
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_category_input")
                )
                OutlinedTextField(
                    value = estimateText,
                    onValueChange = { estimateText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Estimate (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_estimate_input")
                )
            }

            OutlinedTextField(
                value = tagsRaw,
                onValueChange = { tagsRaw = it },
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_tags_input")
            )

            // Repeat Rule
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RepeatRule.entries.forEach { r ->
                    FilterChip(
                        selected = repeat == r,
                        onClick = { repeat = r },
                        label = { Text(r.label) },
                        modifier = Modifier.testTag("edit_repeat_${r.key.ifEmpty { "none" }}")
                    )
                }
            }

            // Remind when due switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remind me when due",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Triggers an alert when the due date & time is reached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = remindWhenDue,
                    onCheckedChange = {
                        remindWhenDue = it
                        if (it && dueDatePart.isNotBlank() && dueTimePart.isBlank()) {
                            dueTimePart = "09:00"
                        }
                    },
                    modifier = Modifier.testTag("edit_remind_switch")
                )
            }

            HorizontalDivider()

            // Subtasks section
            Text(
                text = "Subtasks (${subtasks.count { it.done }}/${subtasks.size})",
                style = MaterialTheme.typography.titleMedium
            )

            subtasks.forEachIndexed { index, subtask ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = subtask.done,
                        onCheckedChange = { checked ->
                            subtasks[index] = subtask.copy(done = checked)
                        }
                    )
                    OutlinedTextField(
                        value = subtask.title,
                        onValueChange = { newText ->
                            subtasks[index] = subtask.copy(title = newText)
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { subtasks.removeAt(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove subtask")
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newSubtaskTitle,
                    onValueChange = { newSubtaskTitle = it },
                    placeholder = { Text("New subtask…") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_subtask_input")
                )
                FilledTonalButton(
                    onClick = {
                        if (newSubtaskTitle.isNotBlank()) {
                            subtasks.add(Subtask(title = newSubtaskTitle.trim(), done = false))
                            newSubtaskTitle = ""
                        }
                    },
                    modifier = Modifier.testTag("add_subtask_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val finalSubtasks = subtasks.toMutableList()
                    if (newSubtaskTitle.isNotBlank()) {
                        finalSubtasks.add(Subtask(title = newSubtaskTitle.trim(), done = false))
                    }
                    val combinedDue = when {
                        dueDatePart.isBlank() -> ""
                        dueTimePart.isNotBlank() -> "${dueDatePart}T$dueTimePart"
                        else -> dueDatePart
                    }
                    onSave(
                        title,
                        notes,
                        priority,
                        combinedDue,
                        category,
                        tagsRaw,
                        estimateText.toIntOrNull() ?: 0,
                        repeat,
                        remindWhenDue,
                        finalSubtasks
                    )
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_edit_button")
            ) {
                Text("Save Changes", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
