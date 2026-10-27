package com.example.domain

import com.example.model.AppSettings
import com.example.model.AppStateSnapshot
import com.example.model.DayCompletion
import com.example.model.GroupOption
import com.example.model.ListStats
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.SortOption
import com.example.model.Subtask
import com.example.model.TaskFilter
import com.example.model.TaskItem
import com.example.model.TaskList
import com.example.model.TaskTemplate
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.roundToInt

object TaskDomainLogic {

    val MOTIVATIONAL_QUOTES = listOf(
        "Great work! ✨",
        "One step closer.",
        "Momentum is building!",
        "Crushed it! 💪",
        "Done and dusted.",
        "Keep it rolling!"
    )

    val BUILT_IN_TEMPLATES = listOf(
        TaskTemplate(
            id = "builtin_morning",
            name = "Morning routine",
            items = listOf("Stretch", "Drink water", "Plan the day !high"),
            isBuiltIn = true
        ),
        TaskTemplate(
            id = "builtin_weekly",
            name = "Weekly review",
            items = listOf("Clear inbox", "Review goals", "Plan next week weekly"),
            isBuiltIn = true
        ),
        TaskTemplate(
            id = "builtin_grocery",
            name = "Grocery run",
            items = listOf("Milk", "Eggs", "Bread"),
            isBuiltIn = true
        )
    )

    val TOUR_STEPS = listOf(
        "Welcome!" to "Type a task in the quick-add box and tap Add. Smart parsing understands !high, #tags, @category, ~30m, today/tomorrow, and daily/weekly/monthly.",
        "Manage tasks" to "Tap the check circle to complete, tap a task to edit subtasks & notes, star to pin to top, tap the timer icon to launch a 25-minute Pomodoro session, or archive.",
        "Organize" to "Use filter chips, instant search, priority & category filters, sort order, and grouping. Long-press or use the selection toggle for bulk actions.",
        "Make it yours" to "Customize theme mode, expressive accent palettes, compact density, sound effects, date locale, and week start in Settings."
    )

    fun generateId(): String = UUID.randomUUID().toString().replace("-", "").take(9)

    fun formatDateYmd(date: Date, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone)
        cal.time = date
        val y = cal.get(Calendar.YEAR)
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    /**
     * Faithfully replicates the HTML `parse(txt)` smart natural-language task parser.
     * Supports:
     * - !(high|med|low|h|m|l) -> Priority
     * - #tag -> Multiple tags
     * - @category -> Category
     * - ~30m -> Estimate in minutes
     * - daily|weekly|monthly -> Repeat rule
     * - today|tomorrow|next week -> Relative due date
     */
    fun parseSmartTask(
        rawText: String,
        listId: String,
        overridePriority: Priority? = null,
        overrideDue: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
        manualOrder: Int = 0
    ): TaskItem {
        var txt = rawText
        var pri = Priority.MED
        var hasInlinePriority = false
        val tags = mutableListOf<String>()
        var cat = ""
        var est = 0
        var rep = RepeatRule.NONE
        var due = ""

        // 1. Priority: /!(high|med|low|h|m|l)\b/i
        val priRegex = Regex("""!(high|med|low|h|m|l)\b""", RegexOption.IGNORE_CASE)
        val priMatch = priRegex.find(txt)
        if (priMatch != null) {
            hasInlinePriority = true
            pri = Priority.fromKey(priMatch.groupValues[1].take(1))
            txt = txt.replaceRange(priMatch.range, "")
        }

        // 2. Tags: /#(\w+)/g
        val tagRegex = Regex("""#(\w+)""")
        txt = tagRegex.replace(txt) { match ->
            tags.add(match.groupValues[1])
            ""
        }

        // 3. Category: /@(\w+)/
        val catRegex = Regex("""@(\w+)""")
        val catMatch = catRegex.find(txt)
        if (catMatch != null) {
            cat = catMatch.groupValues[1]
            txt = txt.replaceRange(catMatch.range, "")
        }

        // 4. Estimate: /~(\d+)m/
        val estRegex = Regex("""~(\d+)m""")
        val estMatch = estRegex.find(txt)
        if (estMatch != null) {
            est = estMatch.groupValues[1].toIntOrNull() ?: 0
            txt = txt.replaceRange(estMatch.range, "")
        }

        // 5. Repeat: /\b(daily|weekly|monthly)\b/i
        val repRegex = Regex("""\b(daily|weekly|monthly)\b""", RegexOption.IGNORE_CASE)
        val repMatch = repRegex.find(txt)
        if (repMatch != null) {
            rep = RepeatRule.fromKey(repMatch.groupValues[1])
            txt = txt.replaceRange(repMatch.range, "")
        }

        // 6. Relative due date: /\b(today|tomorrow|next week)\b/i
        val dueRegex = Regex("""\b(today|tomorrow|next week)\b""", RegexOption.IGNORE_CASE)
        val dueMatch = dueRegex.find(txt)
        if (dueMatch != null) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = nowMillis
            when (dueMatch.groupValues[1].lowercase()) {
                "tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "next week" -> cal.add(Calendar.DAY_OF_YEAR, 7)
            }
            due = formatDateYmd(cal.time)
            txt = txt.replaceRange(dueMatch.range, "")
        }

        if (!hasInlinePriority && overridePriority != null) {
            pri = overridePriority
        }
        if (due.isEmpty() && !overrideDue.isNullOrBlank()) {
            due = overrideDue.trim()
        }

        val cleanTitle = txt.replace(Regex("""\s+"""), " ").trim()

        return TaskItem(
            id = generateId(),
            listId = listId,
            title = cleanTitle.ifEmpty { rawText.trim() },
            done = false,
            priority = pri,
            due = due,
            category = cat,
            tags = tags,
            notes = "",
            subtasks = emptyList(),
            repeat = rep,
            starred = false,
            estimateMinutes = est,
            created = nowMillis,
            doneAt = 0L,
            archived = false,
            remindWhenDue = false,
            reminderFired = false,
            pomodoroCount = 0,
            manualOrder = manualOrder
        )
    }

    /**
     * Advances a due date string (`YYYY-MM-DD` or `YYYY-MM-DDTHH:mm`) according to the repeat rule.
     */
    fun advanceDueDate(due: String, repeat: RepeatRule): String {
        if (due.isBlank() || repeat == RepeatRule.NONE) return due
        val datePart = due.take(10)
        val timeSuffix = if (due.length > 10) due.substring(10) else ""
        val parts = datePart.split("-")
        if (parts.size != 3) return due
        val year = parts[0].toIntOrNull() ?: return due
        val month = (parts[1].toIntOrNull() ?: return due) - 1
        val day = parts[2].toIntOrNull() ?: return due

        val cal = Calendar.getInstance()
        cal.set(year, month, day, 12, 0, 0)
        when (repeat) {
            RepeatRule.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
            RepeatRule.WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 7)
            RepeatRule.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RepeatRule.NONE -> {}
        }
        return formatDateYmd(cal.time) + timeSuffix
    }

    /**
     * Toggles task completion and spawns next recurring instance if completing a recurring task.
     * Returns updated task + optional newly spawned recurring task.
     */
    fun toggleTaskCompletion(
        task: TaskItem,
        nowMillis: Long = System.currentTimeMillis(),
        nextManualOrder: Int = 0
    ): Pair<TaskItem, TaskItem?> {
        val nowDone = !task.done
        val updated = task.copy(
            done = nowDone,
            doneAt = if (nowDone) nowMillis else 0L
        )
        val spawned = if (nowDone && task.repeat != RepeatRule.NONE) {
            task.copy(
                id = generateId(),
                done = false,
                doneAt = 0L,
                subtasks = task.subtasks.map { it.copy(done = false) },
                created = nowMillis,
                due = advanceDueDate(task.due, task.repeat),
                reminderFired = false,
                manualOrder = nextManualOrder
            )
        } else {
            null
        }
        return updated to spawned
    }

    /**
     * Determines if a task is overdue at `nowMillis`.
     * Matches HTML: `t.due && !t.done && new Date(t.due.length>10 ? t.due : t.due+'T23:59') < new Date()`
     */
    fun isOverdue(task: TaskItem, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (task.done || task.due.isBlank()) return false
        val dueMillis = parseDueToMillis(task.due, defaultEndOfDay = true) ?: return false
        return dueMillis < nowMillis
    }

    fun parseDueToMillis(due: String, defaultEndOfDay: Boolean = true): Long? {
        if (due.length < 10) return null
        return try {
            val datePart = due.take(10)
            val timePart = if (due.length > 10) {
                due.substring(11).take(5)
            } else if (defaultEndOfDay) {
                "23:59"
            } else {
                "09:00"
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            sdf.parse("$datePart $timePart")?.time
        } catch (_: Exception) {
            null
        }
    }

    fun formatDueDisplay(due: String, localeTag: String = "en-US"): String {
        if (due.isBlank()) return ""
        val locale = Locale.forLanguageTag(localeTag)
        return try {
            val hasTime = due.length > 10
            val millis = parseDueToMillis(due, defaultEndOfDay = false) ?: return due
            val date = Date(millis)
            val dateFmt = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale)
            if (hasTime) {
                val timeFmt = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale)
                "${dateFmt.format(date)}, ${timeFmt.format(date)}"
            } else {
                dateFmt.format(date)
            }
        } catch (_: Exception) {
            due
        }
    }

    fun formatTimestampDate(millis: Long, localeTag: String = "en-US"): String {
        if (millis <= 0L) return ""
        return try {
            val locale = Locale.forLanguageTag(localeTag)
            val dateFmt = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale)
            dateFmt.format(Date(millis))
        } catch (_: Exception) {
            formatDateYmd(Date(millis))
        }
    }

    /**
     * Replicates `view()` from the HTML application:
     * 1. Filters by active list (`listId`) and archive status (`filter == ARCHIVE ? t.archived : !t.archived`)
     * 2. Applies tab filter (`ACTIVE`, `ALL`, `TODAY`, `UPCOMING`, `STARRED`, `DONE`, `ARCHIVE`)
     * 3. Applies search query across `title + notes + category + tags.joinToString(" ")`
     * 4. Applies priority filter and category/tag filter
     * 5. Applies sort (`MANUAL`, `DUE`, `PRIORITY`, `NAME`, `NEWEST`)
     * 6. Pins starred tasks first (`starred + unstarred`)
     */
    fun filterAndSortTasks(
        allTasks: List<TaskItem>,
        currentListId: String,
        filter: TaskFilter,
        searchQuery: String = "",
        priorityFilter: Priority? = null,
        categoryOrTagFilter: String = "",
        sortOption: SortOption = SortOption.MANUAL,
        nowMillis: Long = System.currentTimeMillis()
    ): List<TaskItem> {
        val todayStr = formatDateYmd(Date(nowMillis))
        val q = searchQuery.trim().lowercase()

        var list = allTasks.filter { t ->
            t.listId == currentListId && (if (filter == TaskFilter.ARCHIVE) t.archived else !t.archived)
        }

        list = list.filter { t ->
            when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.ACTIVE -> !t.done
                TaskFilter.DONE -> t.done
                TaskFilter.TODAY -> !t.done && t.due.take(10) == todayStr
                TaskFilter.UPCOMING -> !t.done && t.due.isNotBlank() && t.due.take(10) > todayStr
                TaskFilter.STARRED -> t.starOrStarred()
                TaskFilter.ARCHIVE -> true
            }
        }

        if (q.isNotEmpty()) {
            list = list.filter { t ->
                val searchable = "${t.title} ${t.notes} ${t.category} ${t.tags.joinToString(" ")}".lowercase()
                searchable.contains(q)
            }
        }

        if (priorityFilter != null) {
            list = list.filter { it.priority == priorityFilter }
        }

        if (categoryOrTagFilter.isNotBlank()) {
            list = list.filter { t ->
                t.category == categoryOrTagFilter || t.tags.contains(categoryOrTagFilter)
            }
        }

        val sorted = when (sortOption) {
            SortOption.MANUAL -> list.sortedBy { it.manualOrder }
            SortOption.DUE -> list.sortedWith(compareBy<TaskItem> { it.due.ifBlank { "9999-99-99" } }.thenBy { it.manualOrder })
            SortOption.PRIORITY -> list.sortedWith(compareByDescending<TaskItem> { it.priority.weight }.thenBy { it.manualOrder })
            SortOption.NAME -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            SortOption.NEWEST -> list.sortedByDescending { it.created }
        }

        val starred = sorted.filter { it.starred }
        val unstarred = sorted.filter { !it.starred }
        return starred + unstarred
    }

    private fun TaskItem.starOrStarred(): Boolean = this.starred

    /**
     * Groups filtered tasks according to `GroupOption`.
     */
    fun groupTasks(tasks: List<TaskItem>, groupOption: GroupOption): Map<String, List<TaskItem>> {
        return when (groupOption) {
            GroupOption.NONE -> mapOf("" to tasks)
            GroupOption.CATEGORY -> tasks.groupBy { it.category.ifBlank { "No category" } }
            GroupOption.PRIORITY -> tasks.groupBy { it.priority.label }
        }
    }

    /**
     * Extracts all unique categories and tags across all tasks.
     */
    fun extractCategoriesAndTags(tasks: List<TaskItem>): List<String> {
        return tasks.flatMap { listOf(it.category) + it.tags }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    /**
     * Calculates consecutive day completion streak matching HTML `streak()`:
     * Checks today; if no task completed today, starts from yesterday and counts consecutive days backwards.
     */
    fun calculateStreak(allTasks: List<TaskItem>, nowMillis: Long = System.currentTimeMillis()): Int {
        val completedDays = allTasks
            .filter { it.doneAt > 0L }
            .map { formatDateYmd(Date(it.doneAt)) }
            .toSet()

        if (completedDays.isEmpty()) return 0

        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        if (!completedDays.contains(formatDateYmd(cal.time))) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        var streak = 0
        while (completedDays.contains(formatDateYmd(cal.time))) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    /**
     * Computes full list statistics and 7-day completion chart starting on `weekStart` (0=Sun, 1=Mon, 6=Sat).
     */
    fun computeStats(
        allTasks: List<TaskItem>,
        currentListId: String,
        settings: AppSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): ListStats {
        val listTasks = allTasks.filter { it.listId == currentListId }
        val nonArchived = listTasks.filter { !it.archived }
        val total = nonArchived.size
        val done = nonArchived.count { it.done }
        val active = total - done
        val pct = if (total > 0) ((done.toDouble() / total.toDouble()) * 100.0).roundToInt() else 0
        val archivedCount = listTasks.count { it.archived }
        val pomCount = listTasks.sumOf { it.pomodoroCount }

        val openHigh = nonArchived.count { !it.done && it.priority == Priority.HIGH }
        val openMed = nonArchived.count { !it.done && it.priority == Priority.MED }
        val openLow = nonArchived.count { !it.done && it.priority == Priority.LOW }

        val streak = calculateStreak(allTasks, nowMillis)

        // 7-day chart starting on settings.weekStart (0=Sunday, 1=Monday, 6=Saturday in JS Date.getDay())
        val nowCal = Calendar.getInstance()
        nowCal.timeInMillis = nowMillis
        val todayStr = formatDateYmd(nowCal.time)
        // Convert Calendar.DAY_OF_WEEK (1=Sun..7=Sat) to JS getDay() (0=Sun..6=Sat)
        val jsDayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK) - 1
        val offsetFromWeekStart = (jsDayOfWeek - settings.weekStart + 7) % 7

        val locale = Locale.forLanguageTag(settings.dateFormatLocale)
        val weekdayFormat = SimpleDateFormat("EEE", locale)

        val weeklyCounts = (0 until 7).map { i ->
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = nowMillis
            dayCal.add(Calendar.DAY_OF_YEAR, -offsetFromWeekStart + i)
            val dStr = formatDateYmd(dayCal.time)
            val count = listTasks.count { t ->
                t.doneAt > 0L && formatDateYmd(Date(t.doneAt)) == dStr
            }
            DayCompletion(
                dateStr = dStr,
                dayShortLabel = weekdayFormat.format(dayCal.time),
                count = count,
                isToday = dStr == todayStr
            )
        }

        return ListStats(
            total = total,
            done = done,
            active = active,
            completionPercent = pct,
            archivedCount = archivedCount,
            pomodoroSessions = pomCount,
            openHigh = openHigh,
            openMed = openMed,
            openLow = openLow,
            streakDays = streak,
            weeklyCounts = weeklyCounts
        )
    }

    /**
     * Exports full application state to JSON 100% compatible with the original HTML `ultimate-todo-v1` schema.
     */
    fun exportToJson(snapshot: AppStateSnapshot): String {
        val root = JSONObject()
        val listsArr = JSONArray()
        snapshot.lists.forEach { l ->
            listsArr.put(JSONObject().apply {
                put("id", l.id)
                put("name", l.name)
            })
        }
        root.put("lists", listsArr)
        root.put("cur", snapshot.currentListId)

        val tasksArr = JSONArray()
        snapshot.tasks.sortedBy { it.manualOrder }.forEach { t ->
            tasksArr.put(JSONObject().apply {
                put("id", t.id)
                put("list", t.listId)
                put("title", t.title)
                put("done", t.done)
                put("pri", t.priority.key)
                put("due", t.due)
                put("cat", t.category)
                put("tags", JSONArray(t.tags))
                put("notes", t.notes)
                val subsArr = JSONArray()
                t.subtasks.forEach { s ->
                    subsArr.put(JSONObject().apply {
                        put("t", s.title)
                        put("d", s.done)
                    })
                }
                put("subs", subsArr)
                put("rep", t.repeat.key)
                put("star", t.starred)
                put("est", t.estimateMinutes)
                put("created", t.created)
                put("doneAt", t.doneAt)
                put("arch", t.archived)
                put("rem", t.remindWhenDue)
                if (t.reminderFired) put("fired", 1)
                put("pom", t.pomodoroCount)
            })
        }
        root.put("tasks", tasksArr)

        val tplArr = JSONArray()
        snapshot.customTemplates.filter { !it.isBuiltIn }.forEach { tpl ->
            tplArr.put(JSONObject().apply {
                put("n", tpl.name)
                put("i", JSONArray(tpl.items))
            })
        }
        root.put("tpl", tplArr)

        val setObj = JSONObject().apply {
            put("theme", snapshot.settings.theme)
            put("accent", snapshot.settings.accent)
            put("dense", snapshot.settings.dense)
            put("sound", snapshot.settings.sound)
            put("week", snapshot.settings.weekStart)
            put("fmt", snapshot.settings.dateFormatLocale)
            put("tips", snapshot.settings.showTips)
            if (snapshot.settings.toured) put("toured", 1)
        }
        root.put("set", setObj)

        return root.toString(2)
    }

    /**
     * Parses and validates imported JSON (from either the original HTML app or Android app).
     */
    fun importFromJson(jsonString: String): Result<AppStateSnapshot> {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("tasks") || !root.has("lists")) {
                return Result.failure(IllegalArgumentException("Missing required 'tasks' or 'lists' fields"))
            }
            val listsJson = root.getJSONArray("lists")
            val lists = mutableListOf<TaskList>()
            for (i in 0 until listsJson.length()) {
                val obj = listsJson.getJSONObject(i)
                val id = obj.optString("id").ifBlank { generateId() }
                val name = obj.optString("name").ifBlank { "List ${i + 1}" }
                lists.add(TaskList(id, name))
            }
            if (lists.isEmpty()) {
                lists.add(TaskList("l1", "Personal"))
            }

            val cur = root.optString("cur").takeIf { c -> lists.any { it.id == c } } ?: lists.first().id

            val tasksJson = root.getJSONArray("tasks")
            val tasks = mutableListOf<TaskItem>()
            for (i in 0 until tasksJson.length()) {
                val obj = tasksJson.getJSONObject(i)
                val tagsArr = obj.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArr != null) {
                    for (j in 0 until tagsArr.length()) {
                        val tag = tagsArr.optString(j).trim()
                        if (tag.isNotEmpty()) tags.add(tag)
                    }
                }
                val subsArr = obj.optJSONArray("subs")
                val subs = mutableListOf<Subtask>()
                if (subsArr != null) {
                    for (j in 0 until subsArr.length()) {
                        val sObj = subsArr.optJSONObject(j)
                        if (sObj != null) {
                            val stTitle = sObj.optString("t").trim()
                            if (stTitle.isNotEmpty()) {
                                subs.add(Subtask(title = stTitle, done = sObj.optBoolean("d", false)))
                            }
                        }
                    }
                }
                tasks.add(
                    TaskItem(
                        id = obj.optString("id").ifBlank { generateId() },
                        listId = obj.optString("list").ifBlank { cur },
                        title = obj.optString("title", "Untitled task"),
                        done = obj.optBoolean("done", false),
                        priority = Priority.fromKey(obj.optString("pri", "med")),
                        due = obj.optString("due", ""),
                        category = obj.optString("cat", ""),
                        tags = tags,
                        notes = obj.optString("notes", ""),
                        subtasks = subs,
                        repeat = RepeatRule.fromKey(obj.optString("rep", "")),
                        starred = obj.optBoolean("star", false),
                        estimateMinutes = obj.optInt("est", 0),
                        created = obj.optLong("created", System.currentTimeMillis()),
                        doneAt = obj.optLong("doneAt", 0L),
                        archived = obj.optBoolean("arch", false),
                        remindWhenDue = obj.optBoolean("rem", false),
                        reminderFired = obj.optInt("fired", 0) == 1 || obj.optBoolean("fired", false),
                        pomodoroCount = obj.optInt("pom", 0),
                        manualOrder = i
                    )
                )
            }

            val tplArr = root.optJSONArray("tpl")
            val customTemplates = mutableListOf<TaskTemplate>()
            if (tplArr != null) {
                for (i in 0 until tplArr.length()) {
                    val tObj = tplArr.optJSONObject(i) ?: continue
                    val name = tObj.optString("n").trim()
                    val itemsArr = tObj.optJSONArray("i")
                    val items = mutableListOf<String>()
                    if (itemsArr != null) {
                        for (j in 0 until itemsArr.length()) {
                            val itemTxt = itemsArr.optString(j).trim()
                            if (itemTxt.isNotEmpty()) items.add(itemTxt)
                        }
                    }
                    if (name.isNotEmpty() && items.isNotEmpty()) {
                        customTemplates.add(TaskTemplate(id = generateId(), name = name, items = items, isBuiltIn = false))
                    }
                }
            }

            val setObj = root.optJSONObject("set")
            val settings = if (setObj != null) {
                AppSettings(
                    theme = setObj.optString("theme", "auto"),
                    accent = setObj.optString("accent", "#6366f1"),
                    dense = setObj.optBoolean("dense", false),
                    sound = setObj.optBoolean("sound", false),
                    weekStart = setObj.optInt("week", 1),
                    dateFormatLocale = setObj.optString("fmt", "en-US"),
                    showTips = setObj.optBoolean("tips", true),
                    toured = setObj.optInt("toured", 0) == 1 || setObj.optBoolean("toured", false)
                )
            } else {
                AppSettings()
            }

            Result.success(
                AppStateSnapshot(
                    lists = lists,
                    currentListId = cur,
                    tasks = tasks,
                    customTemplates = customTemplates,
                    settings = settings
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports tasks to CSV format matching the original HTML:
     * `title,done,priority,due,category,tags,created`
     */
    fun exportToCsv(tasks: List<TaskItem>): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val header = "title,done,priority,due,category,tags,created"
        val rows = tasks.sortedBy { it.manualOrder }.map { t ->
            val fields = listOf(
                t.title,
                t.done.toString(),
                t.priority.key,
                t.due,
                t.category,
                t.tags.joinToString(";"),
                isoFormat.format(Date(t.created))
            )
            fields.joinToString(",") { field ->
                "\"${field.replace("\"", "\"\"")}\""
            }
        }
        return (listOf(header) + rows).joinToString("\n")
    }
}
