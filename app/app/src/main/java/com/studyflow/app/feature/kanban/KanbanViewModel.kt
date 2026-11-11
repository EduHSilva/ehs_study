package com.studyflow.app.feature.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.data.model.TaskUpdate
import com.studyflow.app.data.repository.ProjectsRepository
import com.studyflow.app.data.repository.TasksRepository
import com.studyflow.app.domain.kanban.KanbanBoard
import com.studyflow.app.domain.kanban.KanbanEntity
import com.studyflow.app.domain.kanban.ProjectColumn
import com.studyflow.app.domain.kanban.ProjectColumnView
import com.studyflow.app.domain.kanban.TaskColumn
import com.studyflow.app.domain.kanban.TaskColumnView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class KanbanUiState(
    val entity: KanbanEntity = KanbanEntity.TASKS,
    val tasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val loading: Boolean = true,
    val errorKey: String? = null,
    val savingId: String? = null,
    val todayKey: String = LocalDate.now().toString(),
    val tasksLoaded: Boolean = false,
    val projectsLoaded: Boolean = false,
) {
    val taskColumns: List<TaskColumnView> get() = KanbanBoard.taskColumns(tasks, todayKey)
    val projectColumns: List<ProjectColumnView> get() = KanbanBoard.projectColumns(projects)

    fun projectName(task: Task): String? =
        task.projectId?.let { id -> projects.firstOrNull { it.id == id }?.title }
}

class KanbanViewModel(
    private val tasksRepository: TasksRepository,
    private val projectsRepository: ProjectsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(KanbanUiState())
    val state: StateFlow<KanbanUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorKey = null) }
            try {
                // As duas listas são carregadas juntas para a troca de aba ser instantânea e para os
                // cartões de tarefa mostrarem o nome do projeto. Uma falha não derruba a outra.
                coroutineScope {
                    val tasksDeferred = async { attempt { tasksRepository.getAll() } }
                    val projectsDeferred = async { attempt { projectsRepository.getProjects() } }
                    val tasksResult = tasksDeferred.await()
                    val projectsResult = projectsDeferred.await()

                    _state.update { current ->
                        current.copy(
                            tasks = tasksResult.getOrNull() ?: current.tasks,
                            projects = projectsResult.getOrNull() ?: current.projects,
                            tasksLoaded = current.tasksLoaded || tasksResult.isSuccess,
                            projectsLoaded = current.projectsLoaded || projectsResult.isSuccess,
                        )
                    }

                    val currentFailed = if (_state.value.entity == KanbanEntity.TASKS) {
                        tasksResult.isFailure
                    } else {
                        projectsResult.isFailure
                    }
                    if (currentFailed) _state.update { it.copy(errorKey = "kanbanLoadFailed") }
                }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun setEntity(next: KanbanEntity) {
        if (_state.value.entity == next) return
        _state.update { it.copy(entity = next, errorKey = null) }
        val loaded = if (next == KanbanEntity.TASKS) {
            _state.value.tasksLoaded
        } else {
            _state.value.projectsLoaded
        }
        if (!loaded && !_state.value.loading) load()
    }

    fun moveTask(id: String, column: TaskColumn) {
        val current = _state.value
        if (current.savingId == id) return
        val task = current.tasks.firstOrNull { it.id == id } ?: return
        if (KanbanBoard.taskColumn(task, current.todayKey) == column) return

        val previous = task
        val plan = KanbanBoard.planTaskMove(task, column, current.todayKey) ?: return

        replaceTask(plan.optimistic)
        _state.update { it.copy(savingId = id, errorKey = null) }
        viewModelScope.launch {
            try {
                val saved = tasksRepository.update(
                    id,
                    TaskUpdate(status = plan.status, completionDate = plan.completionDate),
                )
                // O PUT não devolve o histórico completo da DAILY; preserva o que o quadro já tinha.
                replaceTask(
                    if (task.type == TaskType.DAILY) {
                        saved.copy(dailyCompletions = plan.optimistic.dailyCompletions)
                    } else {
                        saved
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                replaceTask(previous)
                _state.update { it.copy(errorKey = "kanbanMoveFailed") }
            } finally {
                _state.update { it.copy(savingId = null) }
            }
        }
    }

    fun moveProject(id: String, column: ProjectColumn) {
        val current = _state.value
        if (current.savingId == id) return
        val project = current.projects.firstOrNull { it.id == id } ?: return
        if (KanbanBoard.projectColumn(project) == column) return

        val previous = project
        val optimistic = KanbanBoard.planProjectMove(project, column)

        replaceProject(optimistic)
        _state.update { it.copy(savingId = id, errorKey = null) }
        viewModelScope.launch {
            try {
                val saved = projectsRepository.updateProject(
                    id = id,
                    title = project.title,
                    advisor = project.advisor,
                    status = optimistic.status,
                )
                replaceProject(saved.project)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                replaceProject(previous)
                _state.update { it.copy(errorKey = "kanbanMoveFailed") }
            } finally {
                _state.update { it.copy(savingId = null) }
            }
        }
    }

    private fun replaceTask(task: Task) {
        _state.update { current ->
            current.copy(tasks = current.tasks.map { if (it.id == task.id) task else it })
        }
    }

    private fun replaceProject(project: Project) {
        _state.update { current ->
            current.copy(projects = current.projects.map { if (it.id == project.id) project else it })
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
