package com.studyflow.app.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.ProjectDetails
import com.studyflow.app.data.model.ProjectStatus
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.repository.ProjectsRepository
import com.studyflow.app.data.repository.TasksRepository
import com.studyflow.app.domain.ApiDates
import com.studyflow.app.domain.projects.ProjectSorting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val loading: Boolean = true,
    val savingProject: Boolean = false,
    val errorKey: String? = null,
    val newProjectId: String? = null,
)

class ProjectsViewModel(
    private val projectsRepository: ProjectsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsUiState())
    val state: StateFlow<ProjectsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorKey = null) }
            try {
                val projects = ProjectSorting.sort(projectsRepository.getProjects())
                _state.update { it.copy(projects = projects, loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, errorKey = "projectsLoadFailed") }
            }
        }
    }

    fun create(title: String, advisor: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(errorKey = "projectTitleRequired") }
            return
        }
        if (_state.value.savingProject) return
        viewModelScope.launch {
            _state.update { it.copy(savingProject = true, errorKey = null) }
            try {
                val created = projectsRepository.createProject(trimmed, advisor.trim())
                _state.update {
                    it.copy(
                        projects = ProjectSorting.sort(listOf(created.project) + it.projects),
                        savingProject = false,
                        newProjectId = created.project.id,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(savingProject = false, errorKey = "projectCreateFailed") }
            }
        }
    }

    fun consumeNewProject() {
        _state.update { it.copy(newProjectId = null) }
    }
}

data class ProjectDetailUiState(
    val details: ProjectDetails? = null,
    val tasks: List<Task> = emptyList(),
    val loading: Boolean = true,
    val tasksLoading: Boolean = false,
    val errorKey: String? = null,
    val tasksErrorKey: String? = null,
    val savingStageId: String? = null,
    val savingVersionId: String? = null,
    val savingNewStage: Boolean = false,
    val savingNewVersion: Boolean = false,
    val savingProject: Boolean = false,
    val projectDeleted: Boolean = false,
    val todayKey: String = LocalDate.now().toString(),
)

class ProjectDetailViewModel(
    private val projectId: String,
    private val projectsRepository: ProjectsRepository,
    private val tasksRepository: TasksRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectDetailUiState())
    val state: StateFlow<ProjectDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorKey = null) }
            try {
                val details = projectsRepository.getProjectDetails(projectId)
                _state.update { it.copy(details = details, loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, errorKey = "projectDetailsLoadFailed") }
                return@launch
            }
            loadTasks()
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _state.update { it.copy(tasksLoading = true, tasksErrorKey = null) }
            try {
                val tasks = tasksRepository.getTasksForProject(projectId)
                _state.update { it.copy(tasks = tasks) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(tasks = emptyList(), tasksErrorKey = "projectTasksLoadFailed") }
            } finally {
                _state.update { it.copy(tasksLoading = false) }
            }
        }
    }

    fun saveProject(title: String, advisor: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(errorKey = "projectTitleRequired") }
            return
        }
        if (_state.value.savingProject) return
        _state.update { it.copy(savingProject = true, errorKey = null) }
        viewModelScope.launch {
            try {
                val updated = projectsRepository.updateProject(projectId, trimmed, advisor.trim())
                _state.update { it.copy(details = updated) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "projectEditFailed") }
            } finally {
                _state.update { it.copy(savingProject = false) }
            }
        }
    }

    fun toggleProjectStatus() {
        val details = _state.value.details ?: return
        if (_state.value.savingProject) return
        val next = if (details.project.status == ProjectStatus.DONE) {
            ProjectStatus.WAITING
        } else {
            ProjectStatus.DONE
        }
        _state.update { it.copy(savingProject = true, errorKey = null) }
        viewModelScope.launch {
            try {
                val updated = projectsRepository.updateProject(
                    id = projectId,
                    title = details.project.title,
                    advisor = details.project.advisor,
                    status = next,
                )
                _state.update { it.copy(details = updated) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "projectUpdateFailed") }
            } finally {
                _state.update { it.copy(savingProject = false) }
            }
        }
    }

    fun removeProject() {
        if (_state.value.savingProject) return
        _state.update { it.copy(savingProject = true, errorKey = null) }
        viewModelScope.launch {
            try {
                projectsRepository.deleteProject(projectId)
                _state.update { it.copy(details = null, projectDeleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "projectDeleteFailed") }
            } finally {
                _state.update { it.copy(savingProject = false) }
            }
        }
    }

    fun toggleStage(stageId: String) {
        val details = _state.value.details ?: return
        if (_state.value.savingStageId == stageId) return
        val stage = details.stages.firstOrNull { it.id == stageId } ?: return
        _state.update { it.copy(savingStageId = stageId, errorKey = null) }
        viewModelScope.launch {
            try {
                val saved = projectsRepository.updateStage(projectId, stageId, stage.name, !stage.done)
                updateStages { stages -> stages.map { if (it.id == saved.id) saved else it } }
                syncProgress()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "stageSaveFailed") }
            } finally {
                _state.update { it.copy(savingStageId = null) }
            }
        }
    }

    fun createStage(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || _state.value.details == null) return
        if (_state.value.savingNewStage) return
        _state.update { it.copy(savingNewStage = true, errorKey = null) }
        viewModelScope.launch {
            try {
                val stage = projectsRepository.createStage(projectId, trimmed)
                updateStages { it + stage }
                syncProgress()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "stageCreateFailed") }
            } finally {
                _state.update { it.copy(savingNewStage = false) }
            }
        }
    }

    fun createVersion(title: String, dueDate: LocalDateTime?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || dueDate == null || _state.value.details == null) return
        if (_state.value.savingNewVersion) return
        _state.update { it.copy(savingNewVersion = true, errorKey = null) }
        viewModelScope.launch {
            try {
                val version = projectsRepository.createVersion(projectId, trimmed, ApiDates.toIso(dueDate))
                updateDetails { it.copy(versions = it.versions + version) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "versionCreateFailed") }
            } finally {
                _state.update { it.copy(savingNewVersion = false) }
            }
        }
    }

    fun updateVersion(id: String, title: String, dueDate: LocalDateTime?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || dueDate == null) return
        if (_state.value.savingVersionId == id) return
        _state.update { it.copy(savingVersionId = id, errorKey = null) }
        viewModelScope.launch {
            try {
                val saved = projectsRepository.updateVersion(projectId, id, trimmed, ApiDates.toIso(dueDate))
                updateDetails { details ->
                    details.copy(versions = details.versions.map { if (it.id == id) saved else it })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "versionSaveFailed") }
            } finally {
                _state.update { it.copy(savingVersionId = null) }
            }
        }
    }

    private fun updateStages(block: (List<com.studyflow.app.data.model.ProjectStage>) -> List<com.studyflow.app.data.model.ProjectStage>) {
        updateDetails { it.copy(stages = block(it.stages)) }
    }

    private fun updateDetails(block: (ProjectDetails) -> ProjectDetails) {
        _state.update { current ->
            current.copy(details = current.details?.let(block))
        }
    }

    private fun syncProgress() {
        _state.update { current ->
            val details = current.details ?: return@update current
            val total = details.stages.size
            val progress = if (total == 0) 0 else Math.round(details.stages.count { it.done }.toFloat() / total * 100)
            current.copy(details = details.copy(project = details.project.copy(progress = progress)))
        }
    }
}
