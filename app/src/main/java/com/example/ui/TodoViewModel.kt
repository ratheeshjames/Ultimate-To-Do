package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TodoRepository
import com.example.domain.TaskDomainLogic
import com.example.model.AppSettings
import com.example.model.AppStateSnapshot
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToastEvent(
    val id: Long = System.nanoTime(),
    val message: String,
    val actionLabel: String? = null,
    val isUndoAction: Boolean = false
)

data class PomodoroState(
    val activeTaskId: String? = null,
    val remainingSeconds: Int = 1500,
    val totalSeconds: Int = 1500,
    val isRunning: Boolean = false
)

data class FilterUiState(
    val filter: TaskFilter = TaskFilter.ACTIVE,
    val searchQuery: String = "",
    val priorityFilter: Priority? = null,
    val categoryOrTagFilter: String = "",
    val sortOption: SortOption = SortOption.MANUAL,
    val groupOption: GroupOption = GroupOption.NONE,
    val selectedTaskIds: Set<String> = emptySet()
)

data class TodoScreenUiState(
    val snapshot: AppStateSnapshot = AppStateSnapshot(),
    val filterState: FilterUiState = FilterUiState(),
    val visibleTasks: List<TaskItem> = emptyList(),
    val groupedTasks: Map<String, List<TaskItem>> = emptyMap(),
    val availableCategoriesAndTags: List<String> = emptyList(),
    val stats: ListStats = TaskDomainLogic.computeStats(emptyList(), "l1", AppSettings()),
    val allTemplates: List<TaskTemplate> = TaskDomainLogic.BUILT_IN_TEMPLATES,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val pomodoroState: PomodoroState = PomodoroState(),
    val activeTourStep: Int? = null,
    val confettiTriggerCount: Int = 0,
    val latestToast: ToastEvent? = null,
    val editingTask: TaskItem? = null,
    val showHelpDialog: Boolean = false
)

class TodoViewModel(
    private val repository: TodoRepository,
    private val onSoundEffect: (Boolean) -> Unit = {}
) : ViewModel() {

    private val undoStack = ArrayDeque<AppStateSnapshot>()
    private val redoStack = ArrayDeque<AppStateSnapshot>()

    private val snapshotFlow = MutableStateFlow(AppStateSnapshot())
    private val filterStateFlow = MutableStateFlow(FilterUiState())
    private val pomodoroStateFlow = MutableStateFlow(PomodoroState())
    private val historyFlagsFlow = MutableStateFlow(false to false)
    private val activeTourStepFlow = MutableStateFlow<Int?>(null)
    private val confettiCountFlow = MutableStateFlow(0)
    private val toastFlow = MutableStateFlow<ToastEvent?>(null)
    private val editingTaskIdFlow = MutableStateFlow<String?>(null)
    private val showHelpDialogFlow = MutableStateFlow(false)

    private var pomodoroJob: Job? = null
    private var reminderCheckJob: Job? = null
    private var tourChecked = false

    val uiState: StateFlow<TodoScreenUiState> = combine(
        snapshotFlow,
        filterStateFlow,
        pomodoroStateFlow,
        historyFlagsFlow,
        combine(
            activeTourStepFlow,
            confettiCountFlow,
            toastFlow,
            editingTaskIdFlow,
            showHelpDialogFlow
        ) { tour, confetti, toast, editId, help ->
             AuxUiState(tour, confetti, toast, editId, help)
        }
    ) { snapshot, filterState, pomState, (canUndo, canRedo), aux ->
        val visible = TaskDomainLogic.filterAndSortTasks(
            allTasks = snapshot.tasks,
            currentListId = snapshot.currentListId,
            filter = filterState.filter,
            searchQuery = filterState.searchQuery,
            priorityFilter = filterState.priorityFilter,
            categoryOrTagFilter = filterState.categoryOrTagFilter,
            sortOption = filterState.sortOption
        )
        val grouped = TaskDomainLogic.groupTasks(visible, filterState.groupOption)
        val catsAndTags = TaskDomainLogic.extractCategoriesAndTags(snapshot.tasks)
        val stats = TaskDomainLogic.computeStats(
            allTasks = snapshot.tasks,
            currentListId = snapshot.currentListId,
            settings = snapshot.settings
        )
        val templates = TaskDomainLogic.BUILT_IN_TEMPLATES + snapshot.customTemplates
        val editingTask = aux.editingTaskId?.let { id -> snapshot.tasks.find { it.id == id } }

        TodoScreenUiState(
            snapshot = snapshot,
            filterState = filterState,
            visibleTasks = visible,
            groupedTasks = grouped,
            availableCategoriesAndTags = catsAndTags,
            stats = stats,
            allTemplates = templates,
            canUndo = canUndo,
            canRedo = canRedo,
            pomodoroState = pomState,
            activeTourStep = aux.activeTourStep,
            confettiTriggerCount = aux.confettiCount,
            latestToast = aux.toast,
            editingTask = editingTask,
            showHelpDialog = aux.showHelpDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodoScreenUiState()
    )

    private data class AuxUiState(
        val activeTourStep: Int?,
        val confettiCount: Int,
        val toast: ToastEvent?,
        val editingTaskId: String?,
        val showHelpDialog: Boolean
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            repository.stateFlow.collect { dbSnapshot ->
                snapshotFlow.value = dbSnapshot
                if (!tourChecked) {
                    tourChecked = true
                    if (!dbSnapshot.settings.toured) {
                        activeTourStepFlow.value = 0
                    }
                }
            }
        }
        startReminderLoop()
    }

    private fun startReminderLoop() {
        reminderCheckJob?.cancel()
        reminderCheckJob = viewModelScope.launch {
            while (true) {
                delay(20_000L)
                checkDueReminders()
            }
        }
    }

    fun checkDueReminders(nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val snap = repository.getSnapshot()
            var changed = false
            val firedTitles = mutableListOf<String>()
            val updatedTasks = snap.tasks.map { task ->
                if (task.remindWhenDue && !task.done && task.due.length > 10 && !task.reminderFired) {
                    val dueMs = TaskDomainLogic.parseDueToMillis(task.due, defaultEndOfDay = false)
                    if (dueMs != null && dueMs <= nowMillis) {
                        changed = true
                        firedTitles.add(task.title)
                        task.copy(reminderFired = true)
                    } else {
                        task
                    }
                } else {
                    task
                }
            }
            if (changed) {
                repository.saveSnapshot(snap.copy(tasks = updatedTasks))
                firedTitles.forEach { title ->
                    emitToast("🔔 Due: $title")
                    onSoundEffect(snap.settings.sound)
                }
            }
        }
    }

    private suspend fun mutateState(transform: (AppStateSnapshot) -> AppStateSnapshot) {
        val current = snapshotFlow.value
        undoStack.addLast(current)
        if (undoStack.size > 60) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        historyFlagsFlow.value = (undoStack.isNotEmpty() to redoStack.isNotEmpty())
        val next = transform(current)
        snapshotFlow.value = next
        repository.saveSnapshot(next)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        viewModelScope.launch {
            val current = snapshotFlow.value
            val previous = undoStack.removeLast()
            redoStack.addLast(current)
            historyFlagsFlow.value = (undoStack.isNotEmpty() to redoStack.isNotEmpty())
            snapshotFlow.value = previous
            repository.saveSnapshot(previous)
            emitToast("Undone")
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        viewModelScope.launch {
            val current = snapshotFlow.value
            val next = redoStack.removeLast()
            undoStack.addLast(current)
            historyFlagsFlow.value = (undoStack.isNotEmpty() to redoStack.isNotEmpty())
            snapshotFlow.value = next
            repository.saveSnapshot(next)
            emitToast("Redone")
        }
    }

    fun emitToast(message: String, actionLabel: String? = null, isUndoAction: Boolean = false) {
        toastFlow.value = ToastEvent(
            message = message,
            actionLabel = actionLabel,
            isUndoAction = isUndoAction
        )
    }

    fun clearToast() {
        toastFlow.value = null
    }

    // --- Task Creation & Operations ---

    fun addTask(rawInput: String, selectedPriority: Priority? = null, selectedDue: String? = null) {
        if (rawInput.isBlank()) {
            emitToast("Type a task first")
            return
        }
        viewModelScope.launch {
            mutateState { snap ->
                val newTask = TaskDomainLogic.parseSmartTask(
                    rawText = rawInput,
                    listId = snap.currentListId,
                    overridePriority = selectedPriority,
                    overrideDue = selectedDue,
                    manualOrder = 0
                )
                val reindexed = (listOf(newTask) + snap.tasks).mapIndexed { idx, t ->
                    t.copy(manualOrder = idx)
                }
                snap.copy(tasks = reindexed)
            }
        }
    }

    fun toggleTaskDone(taskId: String) {
        viewModelScope.launch {
            val currentSnap = repository.getSnapshot()
            val target = currentSnap.tasks.find { it.id == taskId } ?: return@launch
            val willBeDone = !target.done

            mutateState { snap ->
                val t = snap.tasks.find { it.id == taskId } ?: return@mutateState snap
                val (updated, spawned) = TaskDomainLogic.toggleTaskCompletion(t)
                val updatedList = snap.tasks.map { if (it.id == taskId) updated else it }
                val finalList = if (spawned != null) {
                    listOf(spawned) + updatedList
                } else {
                    updatedList
                }
                snap.copy(tasks = finalList.mapIndexed { idx, item -> item.copy(manualOrder = idx) })
            }

            if (willBeDone) {
                confettiCountFlow.update { it + 1 }
                onSoundEffect(currentSnap.settings.sound)
                val quote = TaskDomainLogic.MOTIVATIONAL_QUOTES.random()
                emitToast(quote)
            }
        }
    }

    fun toggleSubtaskDone(taskId: String, subtaskIndex: Int) {
        viewModelScope.launch {
            mutateState { snap ->
                val updated = snap.tasks.map { t ->
                    if (t.id == taskId && subtaskIndex in t.subtasks.indices) {
                        val newSubs = t.subtasks.mapIndexed { i, s ->
                            if (i == subtaskIndex) s.copy(done = !s.done) else s
                        }
                        t.copy(subtasks = newSubs)
                    } else {
                        t
                    }
                }
                snap.copy(tasks = updated)
            }
        }
    }

    fun toggleTaskStar(taskId: String) {
        viewModelScope.launch {
            mutateState { snap ->
                snap.copy(
                    tasks = snap.tasks.map { t ->
                        if (t.id == taskId) t.copy(starred = !t.starred) else t
                    }
                )
            }
        }
    }

    fun duplicateTask(taskId: String) {
        viewModelScope.launch {
            mutateState { snap ->
                val orig = snap.tasks.find { it.id == taskId } ?: return@mutateState snap
                val copy = orig.copy(
                    id = TaskDomainLogic.generateId(),
                    title = "${orig.title} (copy)",
                    done = false,
                    doneAt = 0L,
                    created = System.currentTimeMillis()
                )
                val newTasks = (listOf(copy) + snap.tasks).mapIndexed { idx, t ->
                    t.copy(manualOrder = idx)
                }
                snap.copy(tasks = newTasks)
            }
            emitToast("Task duplicated")
        }
    }

    fun archiveTask(taskId: String) {
        viewModelScope.launch {
            mutateState { snap ->
                snap.copy(
                    tasks = snap.tasks.map { t ->
                        if (t.id == taskId) t.copy(archived = true) else t
                    }
                )
            }
            emitToast("Archived", actionLabel = "Undo", isUndoAction = true)
        }
    }

    fun restoreTask(taskId: String) {
        viewModelScope.launch {
            mutateState { snap ->
                snap.copy(
                    tasks = snap.tasks.map { t ->
                        if (t.id == taskId) t.copy(archived = false) else t
                    }
                )
            }
            emitToast("Restored from archive")
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            mutateState { snap ->
                snap.copy(tasks = snap.tasks.filter { it.id != taskId })
            }
            if (pomodoroStateFlow.value.activeTaskId == taskId) {
                stopPomodoro()
            }
            emitToast("Deleted", actionLabel = "Undo", isUndoAction = true)
        }
    }

    fun openTaskEditor(taskId: String) {
        editingTaskIdFlow.value = taskId
    }

    fun closeTaskEditor() {
        editingTaskIdFlow.value = null
    }

    fun saveEditedTask(
        taskId: String,
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
    ) {
        viewModelScope.launch {
            mutateState { snap ->
                val cleanTags = tagsRaw.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val cleanSubtasks = subtasks.filter { it.title.trim().isNotEmpty() }
                    .map { it.copy(title = it.title.trim()) }

                snap.copy(
                    tasks = snap.tasks.map { t ->
                        if (t.id == taskId) {
                            t.copy(
                                title = title.trim().ifEmpty { t.title },
                                notes = notes,
                                priority = priority,
                                due = due.trim(),
                                category = category.trim(),
                                tags = cleanTags,
                                estimateMinutes = estimateMinutes.coerceAtLeast(0),
                                repeat = repeat,
                                remindWhenDue = remindWhenDue,
                                reminderFired = if (due.trim() != t.due) false else t.reminderFired,
                                subtasks = cleanSubtasks
                            )
                        } else {
                            t
                        }
                    }
                )
            }
            editingTaskIdFlow.value = null
            emitToast("Task updated")
        }
    }

    fun moveTaskOrder(taskId: String, moveUp: Boolean) {
        viewModelScope.launch {
            mutateState { snap ->
                val list = snap.tasks.sortedBy { it.manualOrder }.toMutableList()
                val currentListTasks = list.filter { it.listId == snap.currentListId && !it.archived }
                val posInVisible = currentListTasks.indexOfFirst { it.id == taskId }
                if (posInVisible == -1) return@mutateState snap
                val targetNeighborId = if (moveUp && posInVisible > 0) {
                    currentListTasks[posInVisible - 1].id
                } else if (!moveUp && posInVisible < currentListTasks.lastIndex) {
                    currentListTasks[posInVisible + 1].id
                } else {
                    return@mutateState snap
                }
                val idxA = list.indexOfFirst { it.id == taskId }
                val idxB = list.indexOfFirst { it.id == targetNeighborId }
                if (idxA != -1 && idxB != -1) {
                    val item = list.removeAt(idxA)
                    list.add(idxB, item)
                }
                snap.copy(tasks = list.mapIndexed { index, taskItem -> taskItem.copy(manualOrder = index) })
            }
        }
    }

    fun markAllVisibleDone() {
        val visibleIds = uiState.value.visibleTasks.filter { !it.done }.map { it.id }.toSet()
        if (visibleIds.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            mutateState { snap ->
                snap.copy(
                    tasks = snap.tasks.map { t ->
                        if (visibleIds.contains(t.id) && !t.done) {
                            t.copy(done = true, doneAt = now)
                        } else {
                            t
                        }
                    }
                )
            }
            confettiCountFlow.update { it + 1 }
            emitToast("Marked ${visibleIds.size} task(s) done")
        }
    }

    fun clearCompletedInCurrentList() {
        viewModelScope.launch {
            val snapBefore = repository.getSnapshot()
            val count = snapBefore.tasks.count { it.listId == snapBefore.currentListId && it.done && !it.archived }
            if (count == 0) {
                emitToast("No completed tasks to archive")
                return@launch
            }
            mutateState { snap ->
                snap.copy(
                    tasks = snap.tasks.map { t ->
                        if (t.listId == snap.currentListId && t.done) {
                            t.copy(archived = true)
                        } else {
                            t
                        }
                    }
                )
            }
            emitToast("Completed tasks archived")
        }
    }

    // --- Filter, Sort, Group & Bulk Selection ---

    fun setFilter(filter: TaskFilter) {
        filterStateFlow.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        filterStateFlow.update { it.copy(searchQuery = query) }
    }

    fun setPriorityFilter(priority: Priority?) {
        filterStateFlow.update { it.copy(priorityFilter = priority) }
    }

    fun setCategoryOrTagFilter(catOrTag: String) {
        filterStateFlow.update { it.copy(categoryOrTagFilter = catOrTag) }
    }

    fun setSortOption(sortOption: SortOption) {
        filterStateFlow.update { it.copy(sortOption = sortOption) }
    }

    fun setGroupOption(groupOption: GroupOption) {
        filterStateFlow.update { it.copy(groupOption = groupOption) }
    }

    fun toggleTaskSelection(taskId: String) {
        filterStateFlow.update { state ->
            val next = if (state.selectedTaskIds.contains(taskId)) {
                state.selectedTaskIds - taskId
            } else {
                state.selectedTaskIds + taskId
            }
            state.copy(selectedTaskIds = next)
        }
    }

    fun clearSelection() {
        filterStateFlow.update { it.copy(selectedTaskIds = emptySet()) }
    }

    fun bulkCompleteSelected() {
        val selected = filterStateFlow.value.selectedTaskIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            mutateState { snap ->
                snap.copy(
                    tasks = snap.tasks.map { t ->
                        if (selected.contains(t.id)) t.copy(done = true, doneAt = now) else t
                    }
                )
            }
            clearSelection()
            confettiCountFlow.update { it + 1 }
            emitToast("Completed ${selected.size} task(s)")
        }
    }

    fun bulkArchiveSelected() {
        val selected = filterStateFlow.value.selectedTaskIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            mutateState { snap ->
                snap.copy(
                    tasks = snap.tasks.map { t ->
                        if (selected.contains(t.id)) t.copy(archived = true) else t
                    }
                )
            }
            clearSelection()
            emitToast("Archived ${selected.size} task(s)")
        }
    }

    fun bulkDeleteSelected() {
        val selected = filterStateFlow.value.selectedTaskIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            mutateState { snap ->
                snap.copy(tasks = snap.tasks.filter { !selected.contains(it.id) })
            }
            clearSelection()
            emitToast("Deleted ${selected.size} task(s)", actionLabel = "Undo", isUndoAction = true)
        }
    }

    fun bulkMoveSelectedToList(targetListId: String) {
        val selected = filterStateFlow.value.selectedTaskIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            mutateState { snap ->
                snap.copy(
                    tasks = snap.tasks.map { t ->
                        if (selected.contains(t.id)) t.copy(listId = targetListId) else t
                    }
                )
            }
            clearSelection()
            emitToast("Moved ${selected.size} task(s)")
        }
    }

    // --- List Management ---

    fun selectList(listId: String) {
        viewModelScope.launch {
            repository.setCurrentListId(listId)
            clearSelection()
        }
    }

    fun createNewList(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            mutateState { snap ->
                val newList = TaskList(id = TaskDomainLogic.generateId(), name = clean)
                snap.copy(
                    lists = snap.lists + newList,
                    currentListId = newList.id
                )
            }
            emitToast("List \"$clean\" created")
        }
    }

    fun renameCurrentList(newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            mutateState { snap ->
                snap.copy(
                    lists = snap.lists.map { l ->
                        if (l.id == snap.currentListId) l.copy(name = clean) else l
                    }
                )
            }
            emitToast("List renamed")
        }
    }

    fun deleteCurrentList() {
        viewModelScope.launch {
            val snapBefore = repository.getSnapshot()
            if (snapBefore.lists.size < 2) {
                emitToast("Keep at least one list")
                return@launch
            }
            mutateState { snap ->
                val remainingLists = snap.lists.filter { it.id != snap.currentListId }
                val remainingTasks = snap.tasks.filter { it.listId != snap.currentListId }
                snap.copy(
                    lists = remainingLists,
                    tasks = remainingTasks,
                    currentListId = remainingLists.first().id
                )
            }
            emitToast("List deleted", actionLabel = "Undo", isUndoAction = true)
        }
    }

    // --- Templates ---

    fun applyTemplate(template: TaskTemplate) {
        viewModelScope.launch {
            mutateState { snap ->
                val parsedItems = template.items.reversed().map { raw ->
                    TaskDomainLogic.parseSmartTask(rawText = raw, listId = snap.currentListId)
                }.reversed()
                val combined = (parsedItems + snap.tasks).mapIndexed { idx, t ->
                    t.copy(manualOrder = idx)
                }
                snap.copy(tasks = combined)
            }
            emitToast("Added ${template.items.size} tasks from \"${template.name}\"")
        }
    }

    fun saveCurrentVisibleAsTemplate(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val titles = uiState.value.visibleTasks.map { it.title }.filter { it.isNotBlank() }
        if (titles.isEmpty()) {
            emitToast("Add tasks to the current view first")
            return
        }
        viewModelScope.launch {
            mutateState { snap ->
                val newTpl = TaskTemplate(
                    id = TaskDomainLogic.generateId(),
                    name = clean,
                    items = titles,
                    isBuiltIn = false
                )
                snap.copy(customTemplates = snap.customTemplates + newTpl)
            }
            emitToast("Template \"$clean\" saved")
        }
    }

    fun deleteCustomTemplate(templateId: String) {
        viewModelScope.launch {
            mutateState { snap ->
                snap.copy(customTemplates = snap.customTemplates.filter { it.id != templateId })
            }
            emitToast("Template removed")
        }
    }

    // --- Pomodoro Timer ---

    fun startPomodoro(taskId: String, durationSeconds: Int = 1500) {
        pomodoroJob?.cancel()
        pomodoroStateFlow.value = PomodoroState(
            activeTaskId = taskId,
            remainingSeconds = durationSeconds,
            totalSeconds = durationSeconds,
            isRunning = true
        )
        runPomodoroLoop()
    }

    fun togglePomodoroPause() {
        val cur = pomodoroStateFlow.value
        if (cur.activeTaskId == null) return
        if (cur.isRunning) {
            pomodoroJob?.cancel()
            pomodoroStateFlow.update { it.copy(isRunning = false) }
        } else {
            pomodoroStateFlow.update { it.copy(isRunning = true) }
            runPomodoroLoop()
        }
    }

    fun stopPomodoro() {
        pomodoroJob?.cancel()
        pomodoroStateFlow.value = PomodoroState()
    }

    fun completePomodoroSessionNow() {
        val activeId = pomodoroStateFlow.value.activeTaskId ?: return
        pomodoroJob?.cancel()
        pomodoroStateFlow.value = PomodoroState()
        viewModelScope.launch {
            val snap = repository.getSnapshot()
            val updated = snap.tasks.map { t ->
                if (t.id == activeId) t.copy(pomodoroCount = t.pomodoroCount + 1) else t
            }
            repository.saveSnapshot(snap.copy(tasks = updated))
            onSoundEffect(snap.settings.sound)
            confettiCountFlow.update { it + 1 }
            emitToast("🍅 Focus session complete!")
        }
    }

    private fun runPomodoroLoop() {
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            while (pomodoroStateFlow.value.isRunning && pomodoroStateFlow.value.remainingSeconds > 0) {
                delay(1000L)
                val nextLeft = pomodoroStateFlow.value.remainingSeconds - 1
                if (nextLeft <= 0) {
                    completePomodoroSessionNow()
                    break
                } else {
                    pomodoroStateFlow.update { it.copy(remainingSeconds = nextLeft) }
                }
            }
        }
    }

    // --- Settings, Tour, Help, Import/Export & Reset ---

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            emitToast("Settings saved")
        }
    }

    fun startTour() {
        showHelpDialogFlow.value = false
        activeTourStepFlow.value = 0
    }

    fun nextTourStep() {
        val cur = activeTourStepFlow.value ?: return
        if (cur + 1 >= TaskDomainLogic.TOUR_STEPS.size) {
            finishTour()
        } else {
            activeTourStepFlow.value = cur + 1
        }
    }

    fun finishTour() {
        activeTourStepFlow.value = null
        viewModelScope.launch {
            repository.markTourCompleted()
        }
    }

    fun setShowHelpDialog(show: Boolean) {
        showHelpDialogFlow.value = show
    }

    fun importJsonData(jsonString: String) {
        val parsed = TaskDomainLogic.importFromJson(jsonString)
        parsed.onSuccess { importedSnapshot ->
            viewModelScope.launch {
                mutateState { importedSnapshot.copy(settings = importedSnapshot.settings.copy(toured = true)) }
                emitToast("Imported ${importedSnapshot.tasks.size} tasks")
            }
        }.onFailure {
            emitToast("Invalid file")
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            undoStack.clear()
            redoStack.clear()
            historyFlagsFlow.value = (false to false)
            stopPomodoro()
            clearSelection()
            repository.resetAllData()
            emitToast("All data reset")
        }
    }
}

class TodoViewModelFactory(
    private val repository: TodoRepository,
    private val onSoundEffect: (Boolean) -> Unit = {}
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            return TodoViewModel(repository, onSoundEffect) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
