package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.TaskDomainLogic
import com.example.ui.TodoScreenUiState
import com.example.ui.TodoViewModel
import com.example.ui.theme.ExpressiveAccents
import com.example.ui.theme.parseHexColor
import com.example.util.PlatformHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplatesHubScreen(
    uiState: TodoScreenUiState,
    viewModel: TodoViewModel,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveTemplateDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("templates_hub_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "templates_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task Templates",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "One-tap routines with smart parsing support",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showSaveTemplateDialog = true },
                    shape = CircleShape,
                    modifier = Modifier.testTag("save_as_template_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Current")
                }
            }
        }

        items(uiState.allTemplates, key = { it.id }) { tpl ->
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("template_card_${tpl.name}")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tpl.name,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = if (tpl.isBuiltIn) "Built-in template · ${tpl.items.size} tasks"
                                else "Custom template · ${tpl.items.size} tasks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!tpl.isBuiltIn) {
                                IconButton(
                                    onClick = { viewModel.deleteCustomTemplate(tpl.id) },
                                    modifier = Modifier.testTag("delete_template_${tpl.name}")
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete template",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = {
                                    viewModel.applyTemplate(tpl)
                                    onNavigateToTasks()
                                },
                                modifier = Modifier.testTag("use_template_${tpl.name}")
                            ) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Use")
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tpl.items.forEach { itemText ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = itemText,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveTemplateDialog) {
        TextInputDialog(
            title = "Save Current List as Template",
            label = "Template name",
            initialValue = "",
            confirmText = "Save",
            onDismiss = { showSaveTemplateDialog = false },
            onConfirm = { name ->
                viewModel.saveCurrentVisibleAsTemplate(name)
                showSaveTemplateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndDataScreen(
    uiState: TodoScreenUiState,
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings = uiState.snapshot.settings
    val currentList = uiState.snapshot.lists.find { it.id == uiState.snapshot.currentListId }
        ?: uiState.snapshot.lists.first()

    var customHexInput by rememberSaveable(settings.accent) { mutableStateOf(settings.accent) }
    var showPasteJsonDialog by rememberSaveable { mutableStateOf(false) }
    var showResetConfirmDialog by rememberSaveable { mutableStateOf(false) }

    // SAF Launchers for Export JSON, Export CSV, and Import JSON
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = TaskDomainLogic.exportToJson(uiState.snapshot)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                viewModel.emitToast("Exported todo.json")
            } catch (_: Exception) {
                viewModel.emitToast("Export failed")
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = TaskDomainLogic.exportToCsv(uiState.snapshot.tasks)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                viewModel.emitToast("Exported todo.csv")
            } catch (_: Exception) {
                viewModel.emitToast("Export failed")
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    viewModel.importJsonData(content)
                } else {
                    viewModel.emitToast("Invalid file")
                }
            } catch (_: Exception) {
                viewModel.emitToast("Invalid file")
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_and_data_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "settings_header") {
            Column {
                Text(
                    text = "Settings & Data",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Appearance, regional preferences, backup & print",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Appearance Card
        item(key = "appearance_card") {
            ElevatedCard(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Appearance & Theme", style = MaterialTheme.typography.titleLarge)

                    Text("Theme Mode", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("auto" to "System Auto", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                            FilterChip(
                                selected = settings.theme == key,
                                onClick = { viewModel.updateSettings(settings.copy(theme = key)) },
                                label = { Text(label) },
                                shape = CircleShape,
                                modifier = Modifier.testTag("theme_chip_$key")
                            )
                        }
                    }

                    Text("Expressive Accent Palette", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExpressiveAccents.forEach { palette ->
                            val isSelected = settings.accent.equals(palette.hex, ignoreCase = true)
                            val swatchColor = parseHexColor(palette.hex)
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        customHexInput = palette.hex
                                        viewModel.updateSettings(settings.copy(accent = palette.hex))
                                    }
                                    .testTag("accent_preset_${palette.hex}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = palette.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Custom Hex Accent Input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customHexInput,
                            onValueChange = { customHexInput = it },
                            label = { Text("Custom Accent Hex (#RRGGBB)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_accent_input")
                        )
                        FilledTonalButton(
                            onClick = {
                                if (customHexInput.trim().matches(Regex("^#?[0-9a-fA-F]{6}$"))) {
                                    val normalized = "#" + customHexInput.trim().removePrefix("#").lowercase()
                                    viewModel.updateSettings(settings.copy(accent = normalized))
                                } else {
                                    viewModel.emitToast("Enter a valid 6-digit hex color")
                                }
                            },
                            modifier = Modifier.testTag("apply_custom_accent_button")
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }

        // Preferences & Regional Settings Card
        item(key = "preferences_card") {
            ElevatedCard(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Behavior & Regional Format", style = MaterialTheme.typography.titleLarge)

                    Text("Date Format Locale", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en-US", "en-GB", "de-DE", "sv-SE").forEach { fmt ->
                            FilterChip(
                                selected = settings.dateFormatLocale == fmt,
                                onClick = { viewModel.updateSettings(settings.copy(dateFormatLocale = fmt)) },
                                label = { Text(fmt) },
                                shape = CircleShape,
                                modifier = Modifier.testTag("date_fmt_chip_$fmt")
                            )
                        }
                    }

                    Text("Week Starts On", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Sunday", 1 to "Monday", 6 to "Saturday").forEach { (dayInt, dayLabel) ->
                            FilterChip(
                                selected = settings.weekStart == dayInt,
                                onClick = { viewModel.updateSettings(settings.copy(weekStart = dayInt)) },
                                label = { Text(dayLabel) },
                                shape = CircleShape,
                                modifier = Modifier.testTag("week_start_chip_$dayInt")
                            )
                        }
                    }

                    HorizontalDivider()

                    SettingSwitchRow(
                        title = "Compact density",
                        subtitle = "Tighter vertical padding for high-density task lists",
                        checked = settings.dense,
                        testTag = "switch_compact_density",
                        onCheckedChange = { viewModel.updateSettings(settings.copy(dense = it)) }
                    )

                    SettingSwitchRow(
                        title = "Sound effects",
                        subtitle = "Play a subtle confirmation tone on task completion & timers",
                        checked = settings.sound,
                        testTag = "switch_sound",
                        onCheckedChange = { viewModel.updateSettings(settings.copy(sound = it)) }
                    )

                    SettingSwitchRow(
                        title = "Show smart parsing tips",
                        subtitle = "Display natural-language syntax hint inside the summary card",
                        checked = settings.showTips,
                        testTag = "switch_show_tips",
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showTips = it)) }
                    )
                }
            }
        }

        // Data Import, Export, Print & Reset Card
        item(key = "data_management_card") {
            ElevatedCard(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Data Backup, Export & Print", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Export or import full JSON state (100% compatible with the web version), export CSV spreadsheets, or print.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { exportJsonLauncher.launch("todo.json") },
                            modifier = Modifier.testTag("export_json_button")
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }

                        FilledTonalButton(
                            onClick = { exportCsvLauncher.launch("todo.csv") },
                            modifier = Modifier.testTag("export_csv_button")
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }

                        FilledTonalButton(
                            onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                            modifier = Modifier.testTag("import_json_file_button")
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON File")
                        }

                        OutlinedButton(
                            onClick = { showPasteJsonDialog = true },
                            modifier = Modifier.testTag("import_paste_json_button")
                        ) {
                            Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste / View JSON")
                        }

                        OutlinedButton(
                            onClick = {
                                val json = TaskDomainLogic.exportToJson(uiState.snapshot)
                                PlatformHelper.shareText(context, "Ultimate To-Do Backup", json, "application/json")
                            },
                            modifier = Modifier.testTag("share_json_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Backup")
                        }

                        OutlinedButton(
                            onClick = {
                                PlatformHelper.printTaskList(
                                    context = context,
                                    currentList = currentList,
                                    tasks = uiState.visibleTasks,
                                    localeTag = settings.dateFormatLocale
                                )
                            },
                            modifier = Modifier.testTag("print_tasks_button")
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_all_data_button")
                    ) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Data")
                    }
                }
            }
        }
    }

    if (showPasteJsonDialog) {
        val currentJson = TaskDomainLogic.exportToJson(uiState.snapshot)
        var jsonInput by rememberSaveable { mutableStateOf(currentJson) }
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            title = { Text("JSON Backup & Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Copy your current JSON backup or paste a JSON backup below to import:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paste_json_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importJsonData(jsonInput)
                        showPasteJsonDialog = false
                    },
                    modifier = Modifier.testTag("confirm_import_json_button")
                ) {
                    Text("Import JSON")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            PlatformHelper.copyToClipboard(context, "todo.json", jsonInput)
                            viewModel.emitToast("Copied JSON to clipboard")
                        }
                    ) {
                        Text("Copy")
                    }
                    TextButton(onClick = { showPasteJsonDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Erase ALL data?") },
            text = { Text("Erase ALL data? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_all_button")
                ) {
                    Text("Erase All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
