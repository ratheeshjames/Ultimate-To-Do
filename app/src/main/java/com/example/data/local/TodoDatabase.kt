package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.Subtask
import com.example.model.TaskItem
import com.example.model.TaskList
import com.example.model.TaskTemplate
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "task_lists")
data class TaskListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortIndex: Int = 0
) {
    fun toDomain() = TaskList(id = id, name = name)
    companion object {
        fun fromDomain(list: TaskList, index: Int = 0) = TaskListEntity(
            id = list.id,
            name = list.name,
            sortIndex = index
        )
    }
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val title: String,
    val done: Boolean,
    val priorityKey: String,
    val due: String,
    val category: String,
    val tagsJson: String,
    val notes: String,
    val subtasksJson: String,
    val repeatKey: String,
    val starred: Boolean,
    val estimateMinutes: Int,
    val created: Long,
    val doneAt: Long,
    val archived: Boolean,
    val remindWhenDue: Boolean,
    val reminderFired: Boolean,
    val pomodoroCount: Int,
    val manualOrder: Int
) {
    fun toDomain(): TaskItem {
        val tags = mutableListOf<String>()
        try {
            val arr = JSONArray(tagsJson)
            for (i in 0 until arr.length()) {
                val t = arr.optString(i)
                if (t.isNotEmpty()) tags.add(t)
            }
        } catch (_: Exception) {}

        val subs = mutableListOf<Subtask>()
        try {
            val arr = JSONArray(subtasksJson)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                subs.add(Subtask(title = obj.optString("t"), done = obj.optBoolean("d", false)))
            }
        } catch (_: Exception) {}

        return TaskItem(
            id = id,
            listId = listId,
            title = title,
            done = done,
            priority = Priority.fromKey(priorityKey),
            due = due,
            category = category,
            tags = tags,
            notes = notes,
            subtasks = subs,
            repeat = RepeatRule.fromKey(repeatKey),
            starred = starred,
            estimateMinutes = estimateMinutes,
            created = created,
            doneAt = doneAt,
            archived = archived,
            remindWhenDue = remindWhenDue,
            reminderFired = reminderFired,
            pomodoroCount = pomodoroCount,
            manualOrder = manualOrder
        )
    }

    companion object {
        fun fromDomain(task: TaskItem): TaskEntity {
            val tagsArr = JSONArray(task.tags)
            val subsArr = JSONArray()
            task.subtasks.forEach { s ->
                subsArr.put(JSONObject().apply {
                    put("t", s.title)
                    put("d", s.done)
                })
            }
            return TaskEntity(
                id = task.id,
                listId = task.listId,
                title = task.title,
                done = task.done,
                priorityKey = task.priority.key,
                due = task.due,
                category = task.category,
                tagsJson = tagsArr.toString(),
                notes = task.notes,
                subtasksJson = subsArr.toString(),
                repeatKey = task.repeat.key,
                starred = task.starred,
                estimateMinutes = task.estimateMinutes,
                created = task.created,
                doneAt = task.doneAt,
                archived = task.archived,
                remindWhenDue = task.remindWhenDue,
                reminderFired = task.reminderFired,
                pomodoroCount = task.pomodoroCount,
                manualOrder = task.manualOrder
            )
        }
    }
}

@Entity(tableName = "custom_templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val itemsJson: String
) {
    fun toDomain(): TaskTemplate {
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(itemsJson)
            for (i in 0 until arr.length()) {
                val item = arr.optString(i)
                if (item.isNotEmpty()) list.add(item)
            }
        } catch (_: Exception) {}
        return TaskTemplate(id = id, name = name, items = list, isBuiltIn = false)
    }

    companion object {
        fun fromDomain(template: TaskTemplate) = TemplateEntity(
            id = template.id,
            name = template.name,
            itemsJson = JSONArray(template.items).toString()
        )
    }
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM task_lists ORDER BY sortIndex ASC")
    fun observeLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists ORDER BY sortIndex ASC")
    suspend fun getListsSnapshot(): List<TaskListEntity>

    @Query("SELECT * FROM tasks ORDER BY manualOrder ASC, created DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY manualOrder ASC, created DESC")
    suspend fun getTasksSnapshot(): List<TaskEntity>

    @Query("SELECT * FROM custom_templates")
    fun observeCustomTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM custom_templates")
    suspend fun getTemplatesSnapshot(): List<TemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(lists: List<TaskListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TemplateEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM custom_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: String)

    @Query("DELETE FROM task_lists")
    suspend fun clearAllLists()

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Query("DELETE FROM custom_templates")
    suspend fun clearAllTemplates()

    @Transaction
    suspend fun replaceFullDatabase(
        lists: List<TaskListEntity>,
        tasks: List<TaskEntity>,
        templates: List<TemplateEntity>
    ) {
        clearAllLists()
        clearAllTasks()
        clearAllTemplates()
        insertLists(lists)
        insertTasks(tasks)
        insertTemplates(templates)
    }
}

@Database(
    entities = [TaskListEntity::class, TaskEntity::class, TemplateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun getInstance(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "ultimate_todo.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
