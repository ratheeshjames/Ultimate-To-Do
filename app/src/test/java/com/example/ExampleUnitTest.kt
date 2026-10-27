package com.example

import com.example.domain.TaskDomainLogic
import com.example.model.AppSettings
import com.example.model.GroupOption
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.SortOption
import com.example.model.Subtask
import com.example.model.TaskFilter
import com.example.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun `smart parser extracts priority, tags, category, estimate, repeat, and relative due date`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 10, 0, 0)
        }
        val parsed = TaskDomainLogic.parseSmartTask(
            rawText = "Call mom tomorrow !high #family #urgent @home ~30m weekly",
            listId = "l1",
            nowMillis = cal.timeInMillis
        )

        assertEquals("Call mom", parsed.title)
        assertEquals(Priority.HIGH, parsed.priority)
        assertEquals(listOf("family", "urgent"), parsed.tags)
        assertEquals("home", parsed.category)
        assertEquals(30, parsed.estimateMinutes)
        assertEquals(RepeatRule.WEEKLY, parsed.repeat)
        assertEquals("2026-10-27", parsed.due)
    }

    @Test
    fun `smart parser respects shorthand priorities and override defaults`() {
        val lowTask = TaskDomainLogic.parseSmartTask(
            rawText = "Read chapter 4 !l",
            listId = "l1",
            overridePriority = Priority.HIGH
        )
        assertEquals(Priority.LOW, lowTask.priority)
        assertEquals("Read chapter 4", lowTask.title)

        val fallbackTask = TaskDomainLogic.parseSmartTask(
            rawText = "Submit expense report",
            listId = "l1",
            overridePriority = Priority.HIGH,
            overrideDue = "2026-11-01"
        )
        assertEquals(Priority.HIGH, fallbackTask.priority)
        assertEquals("2026-11-01", fallbackTask.due)
    }

    @Test
    fun `completing recurring task spawns next occurrence with reset subtasks and advanced due date`() {
        val recurringTask = TaskItem(
            id = "t1",
            listId = "l1",
            title = "Pay rent",
            done = false,
            repeat = RepeatRule.MONTHLY,
            due = "2026-10-15T09:00",
            subtasks = listOf(Subtask("Check balance", done = true), Subtask("Transfer", done = true))
        )

        val (completed, spawned) = TaskDomainLogic.toggleTaskCompletion(recurringTask, nowMillis = 1700000000000L)
        assertTrue(completed.done)
        assertEquals(1700000000000L, completed.doneAt)
        assertNotNull(spawned)
        assertFalse(spawned!!.done)
        assertEquals("2026-11-15T09:00", spawned.due)
        assertTrue(spawned.subtasks.all { !it.done })
    }

    @Test
    fun `filtering, sorting, and starred pinning match original HTML rules`() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 12, 0, 0)
        }
        val todayStr = TaskDomainLogic.formatDateYmd(nowCal.time)

        val tasks = listOf(
            TaskItem(id = "1", listId = "l1", title = "B Task", priority = Priority.LOW, due = todayStr, starred = false, manualOrder = 0),
            TaskItem(id = "2", listId = "l1", title = "A Task", priority = Priority.HIGH, due = "2026-10-30", starred = false, manualOrder = 1),
            TaskItem(id = "3", listId = "l1", title = "Pinned Task", priority = Priority.MED, due = todayStr, starred = true, manualOrder = 2),
            TaskItem(id = "4", listId = "l1", title = "Done Task", done = true, priority = Priority.HIGH, manualOrder = 3),
            TaskItem(id = "5", listId = "l1", title = "Archived Task", archived = true, manualOrder = 4)
        )

        // Active filter + Priority sort -> Pinned first, then High, then Low
        val activeByPriority = TaskDomainLogic.filterAndSortTasks(
            allTasks = tasks,
            currentListId = "l1",
            filter = TaskFilter.ACTIVE,
            sortOption = SortOption.PRIORITY,
            nowMillis = nowCal.timeInMillis
        )
        assertEquals(listOf("3", "2", "1"), activeByPriority.map { it.id })

        // Today filter
        val todayTasks = TaskDomainLogic.filterAndSortTasks(
            allTasks = tasks,
            currentListId = "l1",
            filter = TaskFilter.TODAY,
            nowMillis = nowCal.timeInMillis
        )
        assertEquals(listOf("3", "1"), todayTasks.map { it.id })

        // Upcoming filter
        val upcomingTasks = TaskDomainLogic.filterAndSortTasks(
            allTasks = tasks,
            currentListId = "l1",
            filter = TaskFilter.UPCOMING,
            nowMillis = nowCal.timeInMillis
        )
        assertEquals(listOf("2"), upcomingTasks.map { it.id })

        // Group by Priority
        val grouped = TaskDomainLogic.groupTasks(activeByPriority, GroupOption.PRIORITY)
        assertEquals(3, grouped.size)
    }

    @Test
    fun `streak calculation counts consecutive completed days from today or yesterday`() {
        val nowCal = Calendar.getInstance()
        val todayMs = nowCal.timeInMillis
        val yesterdayMs = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        val twoDaysAgoMs = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -2) }.timeInMillis

        val tasks = listOf(
            TaskItem(id = "1", title = "T1", done = true, doneAt = todayMs),
            TaskItem(id = "2", title = "T2", done = true, doneAt = yesterdayMs),
            TaskItem(id = "3", title = "T3", done = true, doneAt = twoDaysAgoMs)
        )

        val streak = TaskDomainLogic.calculateStreak(tasks, nowMillis = todayMs)
        assertEquals(3, streak)

        val stats = TaskDomainLogic.computeStats(tasks, "l1", AppSettings(), nowMillis = todayMs)
        assertEquals(100, stats.completionPercent)
        assertEquals(7, stats.weeklyCounts.size)
    }

    @Test
    fun `csv export escapes quotes and formats fields properly`() {
        val task = TaskItem(
            id = "c1",
            title = "Read \"Clean Code\"",
            done = true,
            priority = Priority.HIGH,
            due = "2026-10-26",
            category = "study",
            tags = listOf("books", "dev"),
            created = 1700000000000L
        )
        val csv = TaskDomainLogic.exportToCsv(listOf(task))
        val lines = csv.lines()
        assertEquals("title,done,priority,due,category,tags,created", lines[0])
        assertTrue(lines[1].startsWith("\"Read \"\"Clean Code\"\"\",\"true\",\"high\",\"2026-10-26\",\"study\",\"books;dev\""))
    }
}
