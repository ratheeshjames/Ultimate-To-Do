package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.GroupOption
import com.example.model.Priority
import com.example.model.SortOption
import com.example.model.TaskFilter
import com.example.ui.TodoScreenUiState
import com.example.ui. TodoViewModel
import com.example.ui.components.CompletionRingGauge
import com.example.ui.components.TaskCardComponent
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksWorkspaceScreen(
    uiState: TodoScreenUiState,
    viewModel: TodoViewModel,
    onNavigateToFocus: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var quickTaskInput by rememberSaveable { mutableStateOf("") }
    var quickPriority by rememberSaveable { mutableStateOf(Priority.MED) }
    var quickDueDate by rememberSaveable { mutableStateOf("") }

    var showNewListDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameListDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteListDialog by rememberSaveable { mutableStateOf(false) }
    var showBulkDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showBulkMoveDialog by rememberSaveable { mutableStateOf(false) }

    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var filterPriorityMenuExpanded by remember { mutableStateOf(false) }
    var filterCatMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var groupMenuExpanded by remember { mutableStateOf(false) }

    val currentList = uiState.snapshot.lists.find { it.id == uiState.snapshot.currentListId }
        ?: uiState.snapshot.lists.first()
    val stats = uiState.stats
    val settings = uiState.snapshot.settings
    val filterState = uiState.filterState

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tasks_workspace_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top List Switcher & Quick Utility Row
        item(key = "header_lists") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = currentList.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${stats.active} active · ${stats.done} completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = uiState.canUndo,
                            modifier = Modifier.testTag("undo_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = uiState.canRedo,
                            modifier = Modifier.testTag("redo_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }
                        IconButton(
                            onClick = { viewModel.setShowHelpDialog(true) },
                            modifier = Modifier.testTag("help_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Smart Syntax & Tour")
                        }
                    }
                }

                // Horizontal List Pills + List management
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.snapshot.lists.forEach { list ->
                        val selected = list.id == uiState.snapshot.currentListId
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectList(list.id) },
                            label = { Text(list.name) },
                            shape = CircleShape,
                            modifier = Modifier.testTag("list_chip_${list.id}")
                        )
                    }
                    AssistChip(
                        onClick = { showNewListDialog = true },
                        label = { Text("+ List") },
                        shape = CircleShape,
                        modifier = Modifier.testTag("new_list_button")
                    )
                    IconButton(
                        onClick = { showRenameListDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("rename_list_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Rename current list",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (uiState.snapshot.lists.size >= 2) {
                        IconButton(
                            onClick = { showDeleteListDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("delete_list_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete current list",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Material 3 Expressive Hero Summary Card
        item(key = "hero_summary") {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompletionRingGauge(percent = stats.completionPercent)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetricCounter("Active", stats.active)
                                MetricCounter("Done", stats.done)
                                MetricCounter("Total", stats.total)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Streak: ${stats.streakDays} day(s)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.testTag("streak_badge_text")
                                    )
                                }
                            }
                        }
                    }

                    if (settings.showTips) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Try: Call mom tomorrow !high #family @home ~30m weekly",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Smart Quick-Add Task Composer Card
        item(key = "quick_add_composer") {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = quickTaskInput,
                            onValueChange = { quickTaskInput = it },
                            placeholder = {
                                Text(
                                    "Add a task… (!high #tag @cat tomorrow)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.addTask(
                                        rawInput = quickTaskInput,
                                        selectedPriority = quickPriority,
                                        selectedDue = quickDueDate
                                    )
                                    if (quickTaskInput.isNotBlank()) {
                                        quickTaskInput = ""
                                        quickDueDate = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_input")
                        )

                        FilledIconButton(
                            onClick = {
                                viewModel.addTask(
                                    rawInput = quickTaskInput,
                                    selectedPriority = quickPriority,
                                    selectedDue = quickDueDate
                                )
                                if (quickTaskInput.isNotBlank()) {
                                    quickTaskInput = ""
                                    quickDueDate = ""
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("quick_add_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Task")
                        }
                    }

                    // Priority & Due Date selector pills
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box {
                            AssistChip(
                                onClick = { priorityMenuExpanded = true },
                                label = { Text("Priority: ${quickPriority.label}") },
                                shape = CircleShape,
                                modifier = Modifier.testTag("quick_priority_chip")
                            )
                            DropdownMenu(
                                expanded = priorityMenuExpanded,
                                onDismissRequest = { priorityMenuExpanded = false }
                            ) {
                                Priority.entries.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.label) },
                                        onClick = {
                                            quickPriority = p
                                            priorityMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        AssistChip(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        quickDueDate = "$y-${(m + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(quickDueDate.ifBlank { "Due Date" }) },
                            shape = CircleShape,
                            modifier = Modifier.testTag("quick_due_chip")
                        )

                        if (quickDueDate.isNotBlank()) {
                            IconButton(
                                onClick = { quickDueDate = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear due date")
                            }
                        }
                    }
                }
            }
        }

        // 4. Filter Tabs, Search, Sort & Group Controls
        item(key = "filters_and_search") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Filter tabs row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskFilter.entries.forEach { f ->
                        val selected = filterState.filter == f
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setFilter(f) },
                            label = { Text(f.label) },
                            shape = CircleShape,
                            modifier = Modifier.testTag("filter_tab_${f.key}")
                        )
                    }
                }

                // Search input
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search tasks, notes, @category, #tags…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filterState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setSearchQuery("") },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )

                // Secondary filter/sort/group chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Priority filter
                    Box {
                        FilterChip(
                            selected = filterState.priorityFilter != null,
                            onClick = { filterPriorityMenuExpanded = true },
                            label = {
                                Text(filterState.priorityFilter?.let { "Pri: ${it.label}" } ?: "Any Priority")
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = CircleShape,
                            modifier = Modifier.testTag("priority_filter_chip")
                        )
                        DropdownMenu(
                            expanded = filterPriorityMenuExpanded,
                            onDismissRequest = { filterPriorityMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Any Priority") },
                                onClick = {
                                    viewModel.setPriorityFilter(null)
                                    filterPriorityMenuExpanded = false
                                }
                            )
                            Priority.entries.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.label) },
                                    onClick = {
                                        viewModel.setPriorityFilter(p)
                                        filterPriorityMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Category/Tag filter
                    Box {
                        FilterChip(
                            selected = filterState.categoryOrTagFilter.isNotBlank(),
                            onClick = { filterCatMenuExpanded = true },
                            label = {
                                Text(
                                    filterState.categoryOrTagFilter.ifBlank { "All Cats/Tags" }
                                )
                            },
                            shape = CircleShape,
                            modifier = Modifier.testTag("category_filter_chip")
                        )
                        DropdownMenu(
                            expanded = filterCatMenuExpanded,
                            onDismissRequest = { filterCatMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Cats/Tags") },
                                onClick = {
                                    viewModel.setCategoryOrTagFilter("")
                                    filterCatMenuExpanded = false
                                }
                            )
                            uiState.availableCategoriesAndTags.forEach { catOrTag ->
                                DropdownMenuItem(
                                    text = { Text(catOrTag) },
                                    onClick = {
                                        viewModel.setCategoryOrTagFilter(catOrTag)
                                        filterCatMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Sort Option
                    Box {
                        AssistChip(
                            onClick = { sortMenuExpanded = true },
                            label = { Text("Sort: ${filterState.sortOption.label}") },
                            shape = CircleShape,
                            modifier = Modifier.testTag("sort_option_chip")
                        )
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            SortOption.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.label) },
                                    onClick = {
                                        viewModel.setSortOption(s)
                                        sortMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("sort_menu_${s.key}")
                                )
                            }
                        }
                    }

                    // Group Option
                    Box {
                        AssistChip(
                            onClick = { groupMenuExpanded = true },
                            label = { Text(filterState.groupOption.label) },
                            shape = CircleShape,
                            modifier = Modifier.testTag("group_option_chip")
                        )
                        DropdownMenu(
                            expanded = groupMenuExpanded,
                            onDismissRequest = { groupMenuExpanded = false }
                        ) {
                            GroupOption.entries.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.label) },
                                    onClick = {
                                        viewModel.setGroupOption(g)
                                        groupMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("group_menu_${g.key.ifEmpty { "none" }}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Contextual Bulk Selection Action Bar
        if (filterState.selectedTaskIds.isNotEmpty()) {
            item(key = "bulk_action_bar") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_action_bar")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filterState.selectedTaskIds.size} selected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            TextButton(
                                onClick = { viewModel.clearSelection() },
                                modifier = Modifier.testTag("bulk_clear_button")
                            ) {
                                Text("Clear")
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.bulkCompleteSelected() },
                                modifier = Modifier.testTag("bulk_complete_button")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Complete")
                            }
                            FilledTonalButton(
                                onClick = { viewModel.bulkArchiveSelected() },
                                modifier = Modifier.testTag("bulk_archive_button")
                            ) {
                                Icon(Icons.Outlined.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Archive")
                            }
                            FilledTonalButton(
                                onClick = { showBulkMoveDialog = true },
                                modifier = Modifier.testTag("bulk_move_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move…")
                            }
                            FilledTonalButton(
                                onClick = { showBulkDeleteConfirm = true },
                                modifier = Modifier.testTag("bulk_delete_button")
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // 6. Task List or Empty State
        if (uiState.visibleTasks.isEmpty()) {
            item(key = "empty_state") {
                val emptyMessage = when {
                    filterState.searchQuery.isNotBlank() ||
                        filterState.priorityFilter != null ||
                        filterState.categoryOrTagFilter.isNotBlank() ->
                        "No tasks match your filters."
                    filterState.filter == TaskFilter.ARCHIVE ->
                        "Archive is empty."
                    else ->
                        "Nothing here yet! Add a task above or start from a template."
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("empty_state_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TaskAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (filterState.filter == TaskFilter.ACTIVE && filterState.searchQuery.isBlank()) {
                            FilledTonalButton(
                                onClick = onNavigateToTemplates,
                                modifier = Modifier.testTag("empty_browse_templates_button")
                            ) {
                                Text("Browse Templates")
                            }
                        }
                    }
                }
            }
        } else {
            uiState.groupedTasks.forEach { (groupHeader, tasksInGroup) ->
                if (groupHeader.isNotBlank()) {
                    item(key = "group_header_$groupHeader") {
                        Text(
                            text = "${groupHeader.uppercase()} (${tasksInGroup.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 8.dp, start = 4.dp)
                                .testTag("group_header_$groupHeader")
                        )
                    }
                }
                items(
                    items = tasksInGroup,
                    key = { it.id }
                ) { task ->
                    TaskCardComponent(
                        task = task,
                        dense = settings.dense,
                        dateFormatLocale = settings.dateFormatLocale,
                        isSelected = filterState.selectedTaskIds.contains(task.id),
                        selectionModeActive = filterState.selectedTaskIds.isNotEmpty(),
                        showManualReorderControls = filterState.sortOption == SortOption.MANUAL,
                        onToggleDone = { viewModel.toggleTaskDone(task.id) },
                        onToggleSelect = { viewModel.toggleTaskSelection(task.id) },
                        onToggleStar = { viewModel.toggleTaskStar(task.id) },
                        onStartPomodoro = {
                            viewModel.startPomodoro(task.id)
                            onNavigateToFocus()
                        },
                        onEdit = { viewModel.openTaskEditor(task.id) },
                        onDuplicate = { viewModel.duplicateTask(task.id) },
                        onArchiveOrRestore = {
                            if (task.archived) viewModel.restoreTask(task.id) else viewModel.archiveTask(task.id)
                        },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onToggleSubtask = { subIdx -> viewModel.toggleSubtaskDone(task.id, subIdx) },
                        onMoveUp = { viewModel.moveTaskOrder(task.id, moveUp = true) },
                        onMoveDown = { viewModel.moveTaskOrder(task.id, moveUp = false) }
                    )
                }
            }
        }

        // 7. Quick Batch Actions Footer
        item(key = "batch_footer") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.markAllVisibleDone() },
                    modifier = Modifier.testTag("mark_all_done_button")
                ) {
                    Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark All Done")
                }
                OutlinedButton(
                    onClick = { viewModel.clearCompletedInCurrentList() },
                    modifier = Modifier.testTag("clear_completed_button")
                ) {
                    Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Completed")
                }
            }
        }
    }

    // Dialogs for List Creation / Rename / Delete & Bulk Actions
    if (showNewListDialog) {
        TextInputDialog(
            title = "New List",
            label = "List name",
            initialValue = "",
            confirmText = "Create",
            onDismiss = { showNewListDialog = false },
            onConfirm = { name ->
                viewModel.createNewList(name)
                showNewListDialog = false
            }
        )
    }

    if (showRenameListDialog) {
        TextInputDialog(
            title = "Rename List",
            label = "List name",
            initialValue = currentList.name,
            confirmText = "Save",
            onDismiss = { showRenameListDialog = false },
            onConfirm = { name ->
                viewModel.renameCurrentList(name)
                showRenameListDialog = false
            }
        )
    }

    if (showDeleteListDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteListDialog = false },
            title = { Text("Delete List?") },
            text = { Text("Delete \"${currentList.name}\" and all its tasks?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrentList()
                        showDeleteListDialog = false
                    },
                    modifier = Modifier.testTag("confirm_delete_list_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteListDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete Selected Tasks?") },
            text = { Text("Delete ${filterState.selectedTaskIds.size} selected task(s)?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bulkDeleteSelected()
                        showBulkDeleteConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_bulk_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBulkMoveDialog) {
        AlertDialog(
            onDismissRequest = { showBulkMoveDialog = false },
            title = { Text("Move Selected Tasks To…") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.snapshot.lists.forEach { targetList ->
                        FilledTonalButton(
                            onClick = {
                                viewModel.bulkMoveSelectedToList(targetList.id)
                                showBulkMoveDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(targetList.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBulkMoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetricCounter(label: String, value: Int) {
    Column {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_text_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
