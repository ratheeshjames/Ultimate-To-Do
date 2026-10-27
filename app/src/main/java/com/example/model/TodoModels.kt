package com.example.model

enum class Priority(val key: String, val label: String, val weight: Int) {
    LOW("low", "Low", 1),
    MED("med", "Medium", 2),
    HIGH("high", "High", 3);

    companion object {
        fun fromKey(key: String?): Priority = when (key?.lowercase()?.trim()) {
            "high", "h" -> HIGH
            "low", "l" -> LOW
            else -> MED
        }
    }
}

enum class RepeatRule(val key: String, val label: String) {
    NONE("", "None"),
    DAILY("daily", "Daily"),
    WEEKLY("weekly", "Weekly"),
    MONTHLY("monthly", "Monthly");

    companion object {
        fun fromKey(key: String?): RepeatRule = when (key?.lowercase()?.trim()) {
            "daily" -> DAILY
            "weekly" -> WEEKLY
            "monthly" -> MONTHLY
            else -> NONE
        }
    }
}

enum class TaskFilter(val key: String, val label: String) {
    ACTIVE("active", "Active"),
    ALL("all", "All"),
    TODAY("today", "Today"),
    UPCOMING("up", "Upcoming"),
    STARRED("star", "Starred"),
    DONE("done", "Done"),
    ARCHIVE("arch", "Archive")
}

enum class SortOption(val key: String, val label: String) {
    MANUAL("manual", "Manual"),
    DUE("due", "Due Date"),
    PRIORITY("pri", "Priority"),
    NAME("name", "Name"),
    NEWEST("new", "Newest")
}

enum class GroupOption(val key: String, val label: String) {
    NONE("", "No Groups"),
    CATEGORY("cat", "By Category"),
    PRIORITY("pri", "By Priority")
}

data class Subtask(
    val title: String,
    val done: Boolean = false
)

data class TaskItem(
    val id: String,
    val listId: String = "l1",
    val title: String,
    val done: Boolean = false,
    val priority: Priority = Priority.MED,
    val due: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val subtasks: List<Subtask> = emptyList(),
    val repeat: RepeatRule = RepeatRule.NONE,
    val starred: Boolean = false,
    val estimateMinutes: Int = 0,
    val created: Long = System.currentTimeMillis(),
    val doneAt: Long = 0L,
    val archived: Boolean = false,
    val remindWhenDue: Boolean = false,
    val reminderFired: Boolean = false,
    val pomodoroCount: Int = 0,
    val manualOrder: Int = 0
)

data class TaskList(
    val id: String,
    val name: String
)

data class TaskTemplate(
    val id: String,
    val name: String,
    val items: List<String>,
    val isBuiltIn: Boolean = false
)

data class AppSettings(
    val theme: String = "auto",      // "auto", "light", "dark"
    val accent: String = "#6366f1",
    val dense: Boolean = false,
    val sound: Boolean = false,
    val weekStart: Int = 1,          // 0 = Sunday, 1 = Monday, 6 = Saturday
    val dateFormatLocale: String = "en-US", // "en-US", "en-GB", "de-DE", "sv-SE"
    val showTips: Boolean = true,
    val toured: Boolean = false
)

data class AppStateSnapshot(
    val lists: List<TaskList> = listOf(TaskList("l1", "Personal")),
    val currentListId: String = "l1",
    val tasks: List<TaskItem> = emptyList(),
    val customTemplates: List<TaskTemplate> = emptyList(),
    val settings: AppSettings = AppSettings()
)

data class ListStats(
    val total: Int,
    val done: Int,
    val active: Int,
    val completionPercent: Int,
    val archivedCount: Int,
    val pomodoroSessions: Int,
    val openHigh: Int,
    val openMed: Int,
    val openLow: Int,
    val streakDays: Int,
    val weeklyCounts: List<DayCompletion>
)

data class DayCompletion(
    val dateStr: String,
    val dayShortLabel: String,
    val count: Int,
    val isToday: Boolean
)
