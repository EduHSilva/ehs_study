package com.studyflow.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.model.DashboardData
import com.studyflow.app.data.model.FocusSession
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskUpdate
import com.studyflow.app.data.repository.DashboardRepository
import com.studyflow.app.data.repository.FocusRepository
import com.studyflow.app.data.repository.TasksRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val data: DashboardData? = null,
    val focus: FocusSession? = null,
    val loading: Boolean = true,
    val errorKey: String? = null,
    val savingTaskId: String? = null,
) {
    val progress: Int
        get() {
            val weekly = data?.weeklyProgress ?: return 0
            if (weekly.total == 0) return 0
            return Math.round(weekly.completed.toFloat() / weekly.total * 100)
        }
}

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val focusRepository: FocusRepository,
    private val tasksRepository: TasksRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorKey = null) }
            try {
                val (dashboard, sessions) = coroutineScope {
                    val dashboardDeferred = async { dashboardRepository.load() }
                    val sessionsDeferred = async {
                        runCatching { focusRepository.getTodaySessions() }.getOrDefault(emptyList())
                    }
                    dashboardDeferred.await() to sessionsDeferred.await()
                }
                _state.update {
                    it.copy(
                        data = dashboard,
                        focus = sessions.firstOrNull(),
                        loading = false,
                        errorKey = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, errorKey = "dashboardLoadFailed") }
            }
        }
    }

    /** Mesmo toggle otimista do dashboard web: inverte o status e reverte se o PUT falhar. */
    fun toggleTask(id: String) {
        val data = _state.value.data ?: return
        if (_state.value.savingTaskId == id) return
        val task = data.tasks.firstOrNull { it.id == id } ?: return
        val previous = task
        val next = if (task.status == TaskStatus.DONE) TaskStatus.PENDING else TaskStatus.DONE
        replaceTask(task.copy(status = next))
        _state.update { it.copy(savingTaskId = id, errorKey = null) }
        viewModelScope.launch {
            try {
                val saved = tasksRepository.update(
                    id,
                    TaskUpdate(
                        title = task.title,
                        type = task.type,
                        startDate = task.startDate,
                        endDate = task.endDate,
                        status = next,
                    ),
                )
                replaceTask(saved)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                replaceTask(previous)
                _state.update { it.copy(errorKey = "taskSaveFailed") }
            } finally {
                _state.update { it.copy(savingTaskId = null) }
            }
        }
    }

    private fun replaceTask(task: Task) {
        _state.update { state ->
            val current = state.data ?: return@update state
            state.copy(data = current.copy(tasks = current.tasks.map { if (it.id == task.id) task else it }))
        }
    }
}
