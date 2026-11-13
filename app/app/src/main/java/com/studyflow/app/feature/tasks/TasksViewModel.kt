package com.studyflow.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.model.DailyTaskCompletion
import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.ProjectStage
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskInput
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.data.model.TaskUpdate
import com.studyflow.app.data.repository.ProjectsRepository
import com.studyflow.app.data.repository.TasksRepository
import com.studyflow.app.domain.ApiDates
import com.studyflow.app.domain.tasks.StudyCalendar
import com.studyflow.app.domain.tasks.StudyCalendar.CalendarMode
import com.studyflow.app.domain.tasks.TaskFormat
import com.studyflow.app.domain.tasks.TaskStats
import com.studyflow.app.domain.tasks.TaskStatsBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

const val TASKS_PAGE_SIZE = 10

private const val STATS_DAYS = 83L // últimas 12 semanas

data class TasksUiState(
    val items: List<Task> = emptyList(),
    val loading: Boolean = true,
    val pageLoading: Boolean = false,
    val calendarLoading: Boolean = false,
    val page: Int = 0,
    val total: Int = 0,
    val errorKey: String? = null,
    val savingId: String? = null,
    val deletingId: String? = null,
    val anchor: LocalDate = LocalDate.now(),
    val mode: CalendarMode = CalendarMode.WEEK,
    val periodTasks: List<Task> = emptyList(),
    // A tela abre com o dia de hoje selecionado: sem isso a lista mostra o período visível — ou a
    // janela padrão do back, que não é o dia atual.
    val todayKey: String = LocalDate.now().toString(),
    val selectedDay: String? = todayKey,
    // Lista já filtrada (todas as páginas), usada pelo "copiar tarefas".
    val filteredTasks: List<Task> = emptyList(),
    val projectFilter: String? = null,
    val statsOpen: Boolean = false,
    val statsLoading: Boolean = false,
    val statsTasks: List<TaskStats> = emptyList(),
    val statsErrorKey: String? = null,
    val formOpen: Boolean = false,
    val creating: Boolean = false,
    val updating: Boolean = false,
    val editingId: String? = null,
    val formErrorKey: String? = null,
    val projects: List<Project> = emptyList(),
    val stagesByProject: Map<String, List<ProjectStage>> = emptyMap(),
    val formProjectId: String? = null,
    val formStageId: String? = null,
) {
    val lastPage: Int get() = maxOf(0, (total + TASKS_PAGE_SIZE - 1) / TASKS_PAGE_SIZE - 1)
    val canGoPrev: Boolean get() = page > 0
    val canGoNext: Boolean get() = page < lastPage
    val targetDay: String get() = selectedDay ?: todayKey
    val formStages: List<ProjectStage> get() = formProjectId?.let { stagesByProject[it] }.orEmpty()
    val recurringCount: Int get() = items.count { it.type != TaskType.UNIQUE }
    val visibleTasks: List<Task>
        get() = projectFilter?.let { id -> periodTasks.filter { it.projectId == id } } ?: periodTasks
    val editingTask: Task?
        get() = editingId?.let { id ->
            items.firstOrNull { it.id == id } ?: periodTasks.firstOrNull { it.id == id }
        }
}

class TasksViewModel(
    private val tasksRepository: TasksRepository,
    private val projectsRepository: ProjectsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, calendarLoading = true) }
            try {
                loadProjectOptions()
                refreshInternal()
            } finally {
                _state.update { it.copy(loading = false, calendarLoading = false) }
            }
        }
    }

    private suspend fun loadProjectOptions() {
        val projects = runCatching { projectsRepository.getProjects() }.getOrDefault(emptyList())
        _state.update { it.copy(projects = projects) }
    }

    fun selectFormProject(id: String?) {
        _state.update { it.copy(formProjectId = id, formStageId = null) }
        if (id == null || _state.value.stagesByProject.containsKey(id)) return
        viewModelScope.launch {
            val stages = runCatching { projectsRepository.getProjectDetails(id).stages }.getOrDefault(emptyList())
            _state.update { it.copy(stagesByProject = it.stagesByProject + (id to stages)) }
        }
    }

    fun selectFormStage(id: String?) {
        _state.update { it.copy(formStageId = id) }
    }

    fun setProjectFilter(id: String?) {
        if (_state.value.projectFilter == id) return
        _state.update { it.copy(projectFilter = id, page = 0) }
        applyFilter()
    }

    fun refresh() {
        viewModelScope.launch { refreshInternal() }
    }

    private suspend fun refreshInternal() {
        _state.update { it.copy(pageLoading = true, errorKey = null) }
        try {
            fetchPeriod()
            resolveStageCache()
            applyFilter()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _state.update { it.copy(errorKey = "tasksLoadFailed") }
        } finally {
            _state.update { it.copy(pageLoading = false) }
        }
    }

    private suspend fun fetchPeriod() {
        val current = _state.value
        val (from, to) = StudyCalendar.gridRangeFor(current.anchor, current.mode)
        val tasks = tasksRepository.getTasksInRange(from, to)
        _state.update { it.copy(periodTasks = tasks) }
    }

    // Nomes de etapa das tags da lista: só o detalhe do projeto traz as etapas.
    private suspend fun resolveStageCache() {
        val current = _state.value
        val missing = current.periodTasks
            .mapNotNull { it.projectId }
            .distinct()
            .filterNot { current.stagesByProject.containsKey(it) }
        if (missing.isEmpty()) return
        var cache = current.stagesByProject
        for (id in missing) {
            val stages = runCatching { projectsRepository.getProjectDetails(id).stages }.getOrDefault(emptyList())
            cache = cache + (id to stages)
        }
        _state.update { it.copy(stagesByProject = cache) }
    }

    private fun paginate(all: List<Task>) {
        _state.update { current ->
            val maxPage = maxOf(0, (all.size + TASKS_PAGE_SIZE - 1) / TASKS_PAGE_SIZE - 1)
            val page = current.page.coerceAtMost(maxPage)
            current.copy(
                total = all.size,
                page = page,
                items = all.drop(page * TASKS_PAGE_SIZE).take(TASKS_PAGE_SIZE),
                filteredTasks = all,
            )
        }
    }

    private fun applyFilter() {
        val current = _state.value
        val key = current.selectedDay
        if (key != null) {
            paginate(current.visibleTasks.filter { StudyCalendar.taskCoversDay(it, key, current.todayKey) })
            return
        }
        val (from, to) = StudyCalendar.rangeFor(current.anchor, current.mode)
        paginate(
            current.visibleTasks.filter { task ->
                if (task.status == TaskStatus.DELETED) return@filter false
                if (task.type != TaskType.UNIQUE) {
                    val start = StudyCalendar.taskStart(task)
                    start == null || !start.isAfter(to)
                } else {
                    val taskFrom = StudyCalendar.taskStart(task) ?: return@filter false
                    val taskTo = StudyCalendar.taskEnd(task) ?: return@filter false
                    !taskTo.isBefore(from) && !taskFrom.isAfter(to)
                }
            },
        )
    }

    fun navigate(step: Int) {
        viewModelScope.launch {
            _state.update { current ->
                val anchor = if (current.mode == CalendarMode.WEEK) {
                    current.anchor.plusWeeks(step.toLong())
                } else {
                    current.anchor.withDayOfMonth(1).plusMonths(step.toLong())
                }
                current.copy(anchor = anchor, selectedDay = null, page = 0, calendarLoading = true)
            }
            try {
                fetchPeriod()
                applyFilter()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "tasksLoadFailed") }
            } finally {
                _state.update { it.copy(calendarLoading = false) }
            }
        }
    }

    fun jumpTo(key: String) {
        viewModelScope.launch {
            val current = _state.value
            val day = runCatching { StudyCalendar.parseDayKey(key) }.getOrNull() ?: return@launch
            val (from, to) = StudyCalendar.rangeFor(current.anchor, current.mode)
            if (day.isBefore(from) || day.isAfter(to)) {
                _state.update { it.copy(anchor = day, selectedDay = null, page = 0, calendarLoading = true) }
                try {
                    fetchPeriod()
                    applyFilter()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _state.update { it.copy(errorKey = "tasksLoadFailed") }
                } finally {
                    _state.update { it.copy(calendarLoading = false) }
                }
                return@launch
            }
            _state.update {
                it.copy(
                    selectedDay = if (it.selectedDay == key) null else key,
                    page = 0,
                )
            }
            applyFilter()
        }
    }

    fun clearDayFilter() {
        _state.update { it.copy(selectedDay = null, page = 0) }
        applyFilter()
    }

    fun setMode(mode: CalendarMode) {
        if (_state.value.mode == mode) return
        viewModelScope.launch {
            _state.update { it.copy(mode = mode, selectedDay = null, page = 0, calendarLoading = true) }
            try {
                fetchPeriod()
                applyFilter()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "tasksLoadFailed") }
            } finally {
                _state.update { it.copy(calendarLoading = false) }
            }
        }
    }

    fun goToPage(target: Int) {
        _state.update { current ->
            val page = target.coerceIn(0, current.lastPage)
            if (page == current.page) current else current.copy(page = page)
        }
        applyFilter()
    }

    fun isDone(task: Task): Boolean = isDone(task, _state.value)

    fun canToggle(task: Task): Boolean = StudyCalendar.canComplete(task, _state.value.targetDay)

    // Copia a lista filtrada inteira (não só a página visível) como checklist para colar fora do app.
    fun copyTasksText(): String? =
        TaskFormat.formatTaskList(_state.value.filteredTasks.map { it.title }).ifEmpty { null }

    private fun isDone(task: Task, current: TasksUiState): Boolean =
        if (task.type != TaskType.UNIQUE) {
            StudyCalendar.isCompletedOn(task, current.targetDay, current.todayKey)
        } else {
            task.status == TaskStatus.DONE
        }

    fun toggleForm() {
        _state.update {
            it.copy(
                formOpen = !it.formOpen,
                formErrorKey = null,
                editingId = null,
                formProjectId = null,
                formStageId = null,
            )
        }
    }

    fun openEdit(task: Task) {
        _state.update {
            it.copy(
                formOpen = true,
                editingId = task.id,
                formErrorKey = null,
                formProjectId = task.projectId,
                formStageId = task.stageId,
            )
        }
        val projectId = task.projectId ?: return
        if (_state.value.stagesByProject.containsKey(projectId)) return
        viewModelScope.launch {
            val stages = runCatching {
                projectsRepository.getProjectDetails(projectId).stages
            }.getOrDefault(emptyList())
            _state.update { it.copy(stagesByProject = it.stagesByProject + (projectId to stages)) }
        }
    }

    fun submitForm(
        title: String,
        start: LocalDateTime?,
        end: LocalDateTime?,
        type: TaskType,
        recurrenceDays: List<Int>,
        recurrenceDayOfMonth: Int?,
        weeklyGoal: Int?,
    ) {
        if (_state.value.editingId != null) {
            saveEdit(title, start, end, type, recurrenceDays, recurrenceDayOfMonth, weeklyGoal)
        } else {
            create(title, start, end, type, recurrenceDays, recurrenceDayOfMonth, weeklyGoal)
        }
    }

    private fun validate(
        title: String,
        start: LocalDateTime?,
        end: LocalDateTime?,
        type: TaskType,
        recurrenceDays: List<Int>,
        recurrenceDayOfMonth: Int?,
        weeklyGoal: Int?,
    ): String? = when {
        title.trim().isEmpty() -> "taskTitleRequired"
        type == TaskType.UNIQUE && start != null && end != null && start.isAfter(end) -> "taskRangeInverted"
        type == TaskType.WEEKLY && recurrenceDays.isEmpty() -> "taskWeekdaysRequired"
        type == TaskType.MONTHLY && (recurrenceDayOfMonth == null || recurrenceDayOfMonth !in 1..31) ->
            "taskMonthDayRequired"
        type == TaskType.WEEKLY_GOAL && (weeklyGoal == null || weeklyGoal !in 1..7) ->
            "taskWeeklyGoalRequired"
        else -> null
    }

    private fun closeForm() {
        _state.update {
            it.copy(
                formOpen = false,
                formErrorKey = null,
                editingId = null,
                formProjectId = null,
                formStageId = null,
            )
        }
    }

    private fun create(
        title: String,
        start: LocalDateTime?,
        end: LocalDateTime?,
        type: TaskType,
        recurrenceDays: List<Int>,
        recurrenceDayOfMonth: Int?,
        weeklyGoal: Int?,
    ) {
        val current = _state.value
        if (current.creating) return
        validate(title, start, end, type, recurrenceDays, recurrenceDayOfMonth, weeklyGoal)?.let { error ->
            _state.update { it.copy(formErrorKey = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(creating = true, formErrorKey = null) }
            try {
                val endIso = end?.let(ApiDates::toIso)
                val linkProject = current.formProjectId
                val linkStage = current.formProjectId?.let { current.formStageId }
                val input = if (type != TaskType.UNIQUE) {
                    TaskInput(
                        title = title.trim(),
                        startDate = start?.let(ApiDates::toIso) ?: Instant.now().toString(),
                        endDate = endIso,
                        type = type,
                        projectId = linkProject,
                        stageId = linkStage,
                        recurrenceDays = if (type == TaskType.WEEKLY) recurrenceDays.sorted() else null,
                        recurrenceDayOfMonth = if (type == TaskType.MONTHLY) recurrenceDayOfMonth else null,
                        weeklyGoal = if (type == TaskType.WEEKLY_GOAL) weeklyGoal else null,
                    )
                } else {
                    TaskInput(
                        title = title.trim(),
                        startDate = start?.let(ApiDates::toIso) ?: endIso,
                        endDate = endIso,
                        type = TaskType.UNIQUE,
                        projectId = linkProject,
                        stageId = linkStage,
                    )
                }
                val focusDay = ApiDates.dayKeyFrom(input.startDate ?: input.endDate ?: Instant.now().toString())
                    ?: LocalDate.now().toString()
                tasksRepository.create(input)

                _state.update { state ->
                    val day = runCatching { StudyCalendar.parseDayKey(focusDay) }.getOrNull()
                    val (from, to) = StudyCalendar.rangeFor(state.anchor, state.mode)
                    val anchor = if (day != null && (day.isBefore(from) || day.isAfter(to))) day else state.anchor
                    state.copy(
                        selectedDay = focusDay,
                        anchor = anchor,
                        page = 0,
                        formOpen = false,
                        formErrorKey = null,
                        formProjectId = null,
                        formStageId = null,
                    )
                }
                fetchPeriod()
                applyFilter()
                if (_state.value.statsOpen) loadStats()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(formErrorKey = "taskCreateFailed") }
            } finally {
                _state.update { it.copy(creating = false) }
            }
        }
    }

    // Na edição o início/fim vazios não vão no corpo (o back mantém o valor atual), mas o
    // vínculo com projeto/etapa é sempre enviado — null explícito desvincula, como no web.
    private fun saveEdit(
        title: String,
        start: LocalDateTime?,
        end: LocalDateTime?,
        type: TaskType,
        recurrenceDays: List<Int>,
        recurrenceDayOfMonth: Int?,
        weeklyGoal: Int?,
    ) {
        val current = _state.value
        if (current.updating) return
        val task = current.editingTask
        if (task == null) {
            closeForm()
            return
        }
        validate(title, start, end, type, recurrenceDays, recurrenceDayOfMonth, weeklyGoal)?.let { error ->
            _state.update { it.copy(formErrorKey = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(updating = true, formErrorKey = null) }
            try {
                val saved = tasksRepository.update(
                    task.id,
                    TaskUpdate(
                        title = title.trim(),
                        type = type,
                        startDate = start?.let(ApiDates::toIso),
                        endDate = end?.let(ApiDates::toIso),
                        projectId = current.formProjectId,
                        stageId = current.formProjectId?.let { current.formStageId },
                        clearProjectLink = current.formProjectId == null,
                        recurrenceDays = if (type == TaskType.WEEKLY) recurrenceDays.sorted() else null,
                        recurrenceDayOfMonth = if (type == TaskType.MONTHLY) recurrenceDayOfMonth else null,
                        weeklyGoal = if (type == TaskType.WEEKLY_GOAL) weeklyGoal else null,
                    ),
                )
                // O PUT não devolve o histórico completo das recorrentes; preserva o da lista.
                val merged = saved.copy(dailyCompletions = task.dailyCompletions)
                closeForm()
                replaceTask(merged)
                try {
                    fetchPeriod()
                    applyFilter()
                    if (_state.value.statsOpen) loadStats()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _state.update { it.copy(errorKey = "tasksLoadFailed") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(formErrorKey = "taskEditFailed") }
            } finally {
                _state.update { it.copy(updating = false) }
            }
        }
    }

    fun toggle(id: String) {
        val current = _state.value
        if (current.savingId == id) return
        val task = current.items.firstOrNull { it.id == id } ?: return
        val day = current.targetDay
        val today = current.todayKey

        // O back recusa `completionDate` futuro e, sem intervalo, recusa qualquer data explícita.
        if (task.type != TaskType.UNIQUE) {
            if (day > today) {
                _state.update { it.copy(errorKey = "taskCompletionFuture") }
                return
            }
            if (!StudyCalendar.canComplete(task, day)) {
                _state.update { it.copy(errorKey = "taskCompletionNotScheduled") }
                return
            }
            if (day != today && task.startDate.isNullOrBlank() && task.endDate.isNullOrBlank()) {
                _state.update { it.copy(errorKey = "taskCompletionNoRange") }
                return
            }
        }

        val wasDone = isDone(task, current)
        val next = if (wasDone) TaskStatus.PENDING else TaskStatus.DONE
        val previous = task
        // Numa recorrente o que muda é a lista de dias concluídos; sem intervalo o back recusa uma
        // data explícita. Numa UNIQUE o dia vai como `completionDate`, que vira o `completedDate`.
        val completionDate = if (task.type != TaskType.UNIQUE) {
            if (!task.startDate.isNullOrBlank() || !task.endDate.isNullOrBlank()) day else null
        } else {
            day
        }

        val optimistic = task.copy(
            status = next,
            completedDate = if (task.type != TaskType.UNIQUE) {
                task.completedDate
            } else {
                if (next == TaskStatus.DONE) day else null
            },
            dailyStatistics = task.dailyStatistics?.copy(
                completedToday = next == TaskStatus.DONE && day == today,
            ),
            dailyCompletions = if (next == TaskStatus.DONE) {
                task.dailyCompletions.filterNot { ApiDates.dayKeyFrom(it.completionDate) == day } +
                    DailyTaskCompletion(day, Instant.now().toString())
            } else {
                task.dailyCompletions.filterNot { ApiDates.dayKeyFrom(it.completionDate) == day }
            },
        )

        replaceTask(optimistic)
        _state.update { it.copy(savingId = id, errorKey = null) }
        viewModelScope.launch {
            try {
                val saved = tasksRepository.update(
                    id,
                    TaskUpdate(status = next, completionDate = completionDate),
                )
                if (task.type != TaskType.UNIQUE) {
                    replaceTask(saved.copy(dailyCompletions = optimistic.dailyCompletions))
                    fetchPeriod()
                    applyFilter()
                    if (_state.value.statsOpen) loadStats()
                } else {
                    replaceTask(saved)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                replaceTask(previous)
                _state.update { it.copy(errorKey = "taskSaveFailed") }
            } finally {
                _state.update { it.copy(savingId = null) }
            }
        }
    }

    fun toggleStats() {
        val open = !_state.value.statsOpen
        _state.update { it.copy(statsOpen = open, statsErrorKey = null) }
        if (open) loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _state.update { it.copy(statsLoading = true, statsErrorKey = null) }
            try {
                val to = LocalDate.now()
                val from = to.minusDays(STATS_DAYS)
                val tasks = tasksRepository.getTasksInRange(from, to)
                val filter = _state.value.projectFilter
                val filtered = if (filter != null) tasks.filter { it.projectId == filter } else tasks
                val stats = TaskStatsBuilder.build(filtered, from, to, _state.value.todayKey)
                _state.update { it.copy(statsTasks = stats) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(statsErrorKey = "taskStatsFailed", statsTasks = emptyList()) }
            } finally {
                _state.update { it.copy(statsLoading = false) }
            }
        }
    }

    fun remove(id: String) {
        if (_state.value.deletingId == id) return
        _state.update { it.copy(deletingId = id) }
        viewModelScope.launch {
            try {
                tasksRepository.delete(id)
                _state.update { current ->
                    if (current.editingId == id) {
                        current.copy(
                            formOpen = false,
                            editingId = null,
                            formErrorKey = null,
                            formProjectId = null,
                            formStageId = null,
                            periodTasks = current.periodTasks.filterNot { it.id == id },
                            items = current.items.filterNot { it.id == id },
                        )
                    } else {
                        current.copy(
                            periodTasks = current.periodTasks.filterNot { it.id == id },
                            items = current.items.filterNot { it.id == id },
                        )
                    }
                }
                applyFilter()
                if (_state.value.statsOpen) loadStats()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "taskDeleteFailed") }
            } finally {
                _state.update { it.copy(deletingId = null) }
            }
        }
    }

    private fun replaceTask(task: Task) {
        _state.update { current ->
            current.copy(
                items = current.items.map { if (it.id == task.id) task else it },
                periodTasks = current.periodTasks.map { if (it.id == task.id) task else it },
            )
        }
    }
}
