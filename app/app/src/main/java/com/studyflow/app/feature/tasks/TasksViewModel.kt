package com.studyflow.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.model.DailyTaskCompletion
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskInput
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.data.model.TaskUpdate
import com.studyflow.app.data.repository.TasksRepository
import com.studyflow.app.domain.ApiDates
import com.studyflow.app.domain.tasks.StudyCalendar
import com.studyflow.app.domain.tasks.StudyCalendar.CalendarMode
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
    val selectedDay: String? = null,
    val todayKey: String = LocalDate.now().toString(),
    val formOpen: Boolean = false,
    val creating: Boolean = false,
    val formErrorKey: String? = null,
) {
    val lastPage: Int get() = maxOf(0, (total + TASKS_PAGE_SIZE - 1) / TASKS_PAGE_SIZE - 1)
    val canGoPrev: Boolean get() = page > 0
    val canGoNext: Boolean get() = page < lastPage
    val targetDay: String get() = selectedDay ?: todayKey
}

class TasksViewModel(
    private val tasksRepository: TasksRepository,
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
                refreshInternal()
            } finally {
                _state.update { it.copy(loading = false, calendarLoading = false) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshInternal() }
    }

    private suspend fun refreshInternal() {
        _state.update { it.copy(pageLoading = true, errorKey = null) }
        try {
            fetchPeriod()
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

    private fun paginate(all: List<Task>) {
        _state.update { current ->
            val maxPage = maxOf(0, (all.size + TASKS_PAGE_SIZE - 1) / TASKS_PAGE_SIZE - 1)
            val page = current.page.coerceAtMost(maxPage)
            current.copy(
                total = all.size,
                page = page,
                items = all.drop(page * TASKS_PAGE_SIZE).take(TASKS_PAGE_SIZE),
            )
        }
    }

    private fun applyFilter() {
        val current = _state.value
        val key = current.selectedDay
        if (key != null) {
            paginate(current.periodTasks.filter { StudyCalendar.taskCoversDay(it, key, current.todayKey) })
            return
        }
        val (from, to) = StudyCalendar.rangeFor(current.anchor, current.mode)
        paginate(
            current.periodTasks.filter { task ->
                if (task.status == TaskStatus.DELETED) return@filter false
                if (task.type == TaskType.DAILY) {
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

    private fun isDone(task: Task, current: TasksUiState): Boolean =
        (task.type == TaskType.DAILY &&
            StudyCalendar.isCompletedOn(task, current.targetDay, current.todayKey)) ||
            (task.type != TaskType.DAILY && task.status == TaskStatus.DONE)

    fun toggleForm() {
        _state.update { it.copy(formOpen = !it.formOpen, formErrorKey = null) }
    }

    fun create(
        title: String,
        start: LocalDateTime?,
        end: LocalDateTime?,
        type: TaskType,
    ) {
        val current = _state.value
        if (current.creating) return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(formErrorKey = "taskTitleRequired") }
            return
        }
        if (type == TaskType.UNIQUE && start != null && end != null && start.isAfter(end)) {
            _state.update { it.copy(formErrorKey = "taskRangeInverted") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(creating = true, formErrorKey = null) }
            try {
                val endIso = end?.let(ApiDates::toIso)
                val input = if (type == TaskType.DAILY) {
                    TaskInput(
                        title = trimmed,
                        startDate = start?.let(ApiDates::toIso) ?: Instant.now().toString(),
                        endDate = endIso,
                        type = TaskType.DAILY,
                    )
                } else {
                    TaskInput(
                        title = trimmed,
                        startDate = start?.let(ApiDates::toIso) ?: endIso,
                        endDate = endIso,
                        type = TaskType.UNIQUE,
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
                    )
                }
                fetchPeriod()
                applyFilter()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(formErrorKey = "taskCreateFailed") }
            } finally {
                _state.update { it.copy(creating = false) }
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
        if (task.type == TaskType.DAILY) {
            if (day > today) {
                _state.update { it.copy(errorKey = "taskCompletionFuture") }
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
        val completionDate = if (
            task.type == TaskType.DAILY && (!task.startDate.isNullOrBlank() || !task.endDate.isNullOrBlank())
        ) {
            day
        } else {
            null
        }

        val optimistic = task.copy(
            status = next,
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
                if (task.type == TaskType.DAILY) {
                    replaceTask(saved.copy(dailyCompletions = optimistic.dailyCompletions))
                    fetchPeriod()
                    applyFilter()
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

    fun remove(id: String) {
        if (_state.value.deletingId == id) return
        _state.update { it.copy(deletingId = id) }
        viewModelScope.launch {
            try {
                tasksRepository.delete(id)
                _state.update { current ->
                    current.copy(
                        periodTasks = current.periodTasks.filterNot { it.id == id },
                        items = current.items.filterNot { it.id == id },
                    )
                }
                applyFilter()
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
