package com.example.data

import com.example.data.local.PreferencesManager
import com.example.data.local.TaskEntity
import com.example.data.local.TaskListEntity
import com.example.data.local.TemplateEntity
import com.example.data.local.TodoDao
import com.example.model.AppSettings
import com.example.model.AppStateSnapshot
import com.example.model.TaskList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class TodoRepository(
    private val dao: TodoDao,
    private val prefs: PreferencesManager
) {
    val stateFlow: Flow<AppStateSnapshot> = combine(
        dao.observeLists(),
        dao.observeTasks(),
        dao.observeCustomTemplates(),
        prefs.currentListIdFlow,
        prefs.settingsFlow
    ) { listEntities, taskEntities, templateEntities, curListId, settings ->
        val lists = if (listEntities.isEmpty()) {
            listOf(TaskList("l1", "Personal"))
        } else {
            listEntities.map { it.toDomain() }
        }
        val validCurId = if (lists.any { it.id == curListId }) curListId else lists.first().id
        AppStateSnapshot(
            lists = lists,
            currentListId = validCurId,
            tasks = taskEntities.map { it.toDomain() },
            customTemplates = templateEntities.map { it.toDomain() },
            settings = settings
        )
    }

    suspend fun ensureInitialized() {
        val currentLists = dao.getListsSnapshot()
        if (currentLists.isEmpty()) {
            dao.insertLists(listOf(TaskListEntity(id = "l1", name = "Personal", sortIndex = 0)))
        }
    }

    suspend fun getSnapshot(): AppStateSnapshot {
        val lists = dao.getListsSnapshot().map { it.toDomain() }.ifEmpty {
            listOf(TaskList("l1", "Personal"))
        }
        val tasks = dao.getTasksSnapshot().map { it.toDomain() }
        val templates = dao.getTemplatesSnapshot().map { it.toDomain() }
        val curId = prefs.currentListIdFlow.first()
        val settings = prefs.settingsFlow.first()
        val validCurId = if (lists.any { it.id == curId }) curId else lists.first().id
        return AppStateSnapshot(
            lists = lists,
            currentListId = validCurId,
            tasks = tasks,
            customTemplates = templates,
            settings = settings
        )
    }

    suspend fun saveSnapshot(snapshot: AppStateSnapshot) {
        val listEntities = snapshot.lists.mapIndexed { idx, l -> TaskListEntity.fromDomain(l, idx) }
        val taskEntities = snapshot.tasks.mapIndexed { idx, t ->
            TaskEntity.fromDomain(t.copy(manualOrder = idx))
        }
        val templateEntities = snapshot.customTemplates.filter { !it.isBuiltIn }.map {
            TemplateEntity.fromDomain(it)
        }
        dao.replaceFullDatabase(listEntities, taskEntities, templateEntities)
        prefs.setCurrentListId(snapshot.currentListId)
        prefs.updateSettings(snapshot.settings)
    }

    suspend fun setCurrentListId(listId: String) {
        prefs.setCurrentListId(listId)
    }

    suspend fun updateSettings(settings: AppSettings) {
        prefs.updateSettings(settings)
    }

    suspend fun markTourCompleted() {
        prefs.markTourCompleted()
    }

    suspend fun resetAllData() {
        val defaultState = AppStateSnapshot(
            lists = listOf(TaskList("l1", "Personal")),
            currentListId = "l1",
            tasks = emptyList(),
            customTemplates = emptyList(),
            settings = AppSettings(toured = true)
        )
        saveSnapshot(defaultState)
    }
}
