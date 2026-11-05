package com.studyflow.app.data.mappers

import com.studyflow.app.data.model.AbntCheck
import com.studyflow.app.data.model.DailyTaskCompletion
import com.studyflow.app.data.model.DailyTaskStatistics
import com.studyflow.app.data.model.DashboardData
import com.studyflow.app.data.model.FocusSession
import com.studyflow.app.data.model.Note
import com.studyflow.app.data.model.NoteFolder
import com.studyflow.app.data.model.NotePreview
import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.ProjectDetails
import com.studyflow.app.data.model.ProjectMilestone
import com.studyflow.app.data.model.ProjectStage
import com.studyflow.app.data.model.ProjectStatus
import com.studyflow.app.data.model.ReferenceCheck
import com.studyflow.app.data.model.ReferenceStatus
import com.studyflow.app.data.model.ReviewReport
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.data.model.WeeklyProgress
import com.studyflow.app.data.remote.DashboardDto
import com.studyflow.app.data.remote.DailyCompletionDto
import com.studyflow.app.data.remote.DailyStatsDto
import com.studyflow.app.data.remote.FocusSessionDto
import com.studyflow.app.data.remote.FolderDto
import com.studyflow.app.data.remote.MilestoneDto
import com.studyflow.app.data.remote.NoteDto
import com.studyflow.app.data.remote.ProjectDto
import com.studyflow.app.data.remote.ReviewReportDto
import com.studyflow.app.data.remote.StageDto
import com.studyflow.app.data.remote.TaskDto

/**
 * Conversão tolerante dos payloads da API, equivalente ao que os `*.api.ts` do web fazem:
 * campos ausentes/desconhecidos não quebram a tela e ganham um fallback coerente.
 */
object Mappers {
    private val statuses = TaskStatus.entries
    private val types = TaskType.entries

    private fun toStatus(value: String?): TaskStatus =
        statuses.firstOrNull { it.name == value } ?: TaskStatus.PENDING

    private fun toType(value: String?): TaskType =
        types.firstOrNull { it.name == value } ?: TaskType.UNIQUE

    private fun toStats(dto: DailyStatsDto?): DailyTaskStatistics? = dto?.let {
        DailyTaskStatistics(
            scheduledDays = it.scheduledDays ?: 0,
            completedDays = it.completedDays ?: 0,
            skippedDays = it.skippedDays ?: 0,
            completedToday = it.completedToday == true,
        )
    }

    private fun toCompletions(items: List<DailyCompletionDto>?): List<DailyTaskCompletion> =
        items.orEmpty().mapNotNull { entry ->
            val date = entry.completionDate ?: return@mapNotNull null
            DailyTaskCompletion(date, entry.completedAt)
        }

    fun task(dto: TaskDto, fallbackId: String? = null): Task {
        val endDate = dto.endDate ?: dto.dueDate ?: dto.schedule
        val status = when {
            dto.status != null -> toStatus(dto.status)
            dto.completed == true -> TaskStatus.DONE
            else -> TaskStatus.PENDING
        }
        return Task(
            id = dto.id ?: fallbackId ?: "",
            title = dto.title.orEmpty(),
            createdDate = dto.createdDate,
            startDate = dto.startDate,
            endDate = endDate,
            type = toType(dto.type),
            status = status,
            dailyStatistics = toStats(dto.dailyStatistics),
            dailyCompletions = toCompletions(dto.dailyCompletions),
        )
    }

    fun dashboard(dto: DashboardDto): DashboardData {
        val list = dto.tasks ?: dto.reminders ?: emptyList()
        return DashboardData(
            weeklyProgress = WeeklyProgress(
                completed = dto.weeklyProgress?.completed ?: 0,
                total = dto.weeklyProgress?.total ?: 0,
            ),
            tasks = list.mapIndexed { index, item -> task(item, "dashboard-task-$index") },
            notes = dto.notes.orEmpty().map {
                NotePreview(id = it.id, title = it.title, content = it.content)
            },
        )
    }

    fun note(dto: NoteDto, fallbackFolderId: String?): Note = Note(
        id = dto.id,
        folderId = dto.folderId ?: fallbackFolderId,
        title = dto.title.orEmpty(),
        content = dto.content.orEmpty(),
    )

    fun folder(dto: FolderDto): NoteFolder = NoteFolder(
        id = dto.id,
        name = dto.name.orEmpty(),
        notes = dto.notes.orEmpty().map { note(it, dto.id) },
    )

    fun project(dto: ProjectDto): Project = Project(
        id = dto.id,
        title = dto.title,
        progress = dto.progress ?: 0,
        status = if (dto.status == "DONE") ProjectStatus.DONE else ProjectStatus.IN_PROGRESS,
        advisor = dto.advisor.orEmpty(),
    )

    fun projectDetails(dto: ProjectDto): ProjectDetails = ProjectDetails(
        project = project(dto),
        stages = dto.stages.orEmpty().map(::stage),
        milestones = dto.milestone.orEmpty().map(::milestone),
    )

    fun stage(dto: StageDto): ProjectStage = ProjectStage(
        id = dto.id,
        name = dto.name ?: dto.title.orEmpty(),
        done = dto.done == true,
    )

    fun milestone(dto: MilestoneDto): ProjectMilestone = ProjectMilestone(
        id = dto.id,
        title = dto.title,
        dueDate = dto.dueDate,
    )

    fun focus(dto: FocusSessionDto): FocusSession = FocusSession(
        id = dto.id,
        title = dto.title,
        minutes = dto.minutes,
        completedAt = dto.completedAt,
    )

    fun review(dto: ReviewReportDto): ReviewReport = ReviewReport(
        documentTitle = dto.documentTitle,
        excerpt = dto.excerpt,
        flaggedExcerpt = dto.flaggedExcerpt,
        similarityPercent = dto.similarityPercent,
        abntChecks = dto.abntChecks.orEmpty().map { AbntCheck(it.label, it.passed) },
        references = dto.references.orEmpty().map {
            ReferenceCheck(
                citation = it.citation,
                status = if (it.status == "Válida") ReferenceStatus.VALID else ReferenceStatus.ATTENTION,
            )
        },
    )
}
