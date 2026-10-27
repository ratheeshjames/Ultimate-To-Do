package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.domain.TaskDomainLogic
import com.example.model.AppSettings
import com.example.model.AppStateSnapshot
import com.example.model.Priority
import com.example.model.Subtask
import com.example.model.TaskItem
import com.example.model.TaskList
import com.example.model.TaskTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app_name from context matches Ultimate To-Do`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ultimate To-Do", appName)
    }

    @Test
    fun `json export and import round-trip preserves lists, tasks, subtasks, templates, and settings`() {
        val original = AppStateSnapshot(
            lists = listOf(TaskList("l1", "Personal"), TaskList("l2", "Work")),
            currentListId = "l2",
            tasks = listOf(
                TaskItem(
                    id = "t100",
                    listId = "l2",
                    title = "Prepare Q4 presentation",
                    done = false,
                    priority = Priority.HIGH,
                    due = "2026-11-10T14:30",
                    category = "office",
                    tags = listOf("slides", "exec"),
                    notes = "Include revenue chart",
                    subtasks = listOf(Subtask("Draft outline", true), Subtask("Review metrics", false)),
                    starred = true,
                    estimateMinutes = 45,
                    pomodoroCount = 2
                )
            ),
            customTemplates = listOf(
                TaskTemplate(id = "tpl1", name = "Sprint Kickoff", items = listOf("Backlog grooming !high", "Assign stories"))
            ),
            settings = AppSettings(theme = "dark", accent = "#0d9488", dense = true, weekStart = 0)
        )

        val json = TaskDomainLogic.exportToJson(original)
        val restoredResult = TaskDomainLogic.importFromJson(json)
        assertTrue(restoredResult.isSuccess)
        val restored = restoredResult.getOrThrow()

        assertEquals(2, restored.lists.size)
        assertEquals("l2", restored.currentListId)
        assertEquals(1, restored.tasks.size)
        val task = restored.tasks.first()
        assertEquals("Prepare Q4 presentation", task.title)
        assertEquals(Priority.HIGH, task.priority)
        assertEquals("office", task.category)
        assertEquals(listOf("slides", "exec"), task.tags)
        assertEquals(2, task.subtasks.size)
        assertEquals(2, task.pomodoroCount)
        assertEquals("dark", restored.settings.theme)
        assertEquals("#0d9488", restored.settings.accent)
    }

    @Test
    fun `principal user workflow - skip tour, add smart task, verify metadata, and navigate destinations`() {
        composeTestRule.waitForIdle()

        // 1. Dismiss onboarding tour if visible
        runCatching {
            composeTestRule.onNodeWithTag("tour_skip_button").performClick()
            composeTestRule.waitForIdle()
        }

        // 2. Add a task using smart syntax
        composeTestRule.onNodeWithTag("quick_add_input")
            .performTextInput("Ship Android release !high #launch @mobile ~25m")
        composeTestRule.onNodeWithTag("quick_add_button").performClick()
        composeTestRule.waitForIdle()

        // 3. Scroll LazyColumn to newly added task and verify parsed task title and metadata pills appear
        composeTestRule.onNodeWithTag("tasks_workspace_list")
            .performScrollToNode(androidx.compose.ui.test.hasText("Ship Android release"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ship Android release").assertIsDisplayed()
        composeTestRule.onNodeWithText("@mobile").assertIsDisplayed()
        composeTestRule.onNodeWithText("#launch").assertIsDisplayed()

        // 4. Navigate to Focus Studio, Insights, Templates, and Settings
        composeTestRule.onNodeWithTag("nav_tab_focus").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("focus_studio_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_insights").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("insights_analytics_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_templates").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("templates_hub_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_and_data_screen").assertIsDisplayed()
    }
}
