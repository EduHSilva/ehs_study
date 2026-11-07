package com.studyflow.app.data.repository

import com.studyflow.app.core.network.unwrap
import com.studyflow.app.data.mappers.Mappers
import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.ProjectDetails
import com.studyflow.app.data.model.ProjectVersion
import com.studyflow.app.data.model.ProjectStage
import com.studyflow.app.data.remote.CreateProjectRequest
import com.studyflow.app.data.remote.VersionRequest
import com.studyflow.app.data.remote.ProjectsApi
import com.studyflow.app.data.remote.StageRequest

class ProjectsRepository(private val api: ProjectsApi) {
    suspend fun getProjects(): List<Project> =
        api.list(page = 0, size = LIST_PAGE_SIZE).unwrap().items.orEmpty().map(Mappers::project)

    suspend fun getProjectDetails(id: String): ProjectDetails =
        Mappers.projectDetails(api.details(id).unwrap())

    suspend fun createProject(title: String, advisor: String): ProjectDetails =
        Mappers.projectDetails(
            api.create(
                CreateProjectRequest(title = title, advisor = advisor.ifBlank { null }),
            ).unwrap(),
        )

    suspend fun createStage(projectId: String, title: String): ProjectStage =
        Mappers.stage(api.createStage(StageRequest(projectId, title, false)).unwrap())

    suspend fun updateStage(projectId: String, stageId: String, title: String, done: Boolean): ProjectStage =
        Mappers.stage(api.updateStage(stageId, StageRequest(projectId, title, done)).unwrap())

    suspend fun createVersion(projectId: String, title: String, dueDate: String): ProjectVersion =
        Mappers.version(api.createVersion(VersionRequest(projectId, title, dueDate)).unwrap())

    suspend fun updateVersion(
        projectId: String,
        versionId: String,
        title: String,
        dueDate: String,
    ): ProjectVersion =
        Mappers.version(api.updateVersion(versionId, VersionRequest(projectId, title, dueDate)).unwrap())

    private companion object {
        const val LIST_PAGE_SIZE = 100
    }
}
