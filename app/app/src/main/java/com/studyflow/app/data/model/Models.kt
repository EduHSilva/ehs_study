package com.studyflow.app.data.model

enum class TaskStatus { PENDING, DONE, DELAYED, DELETED }

enum class TaskType { UNIQUE, DAILY }

data class DailyTaskStatistics(
    val scheduledDays: Int,
    val completedDays: Int,
    val skippedDays: Int,
    val completedToday: Boolean,
)

data class DailyTaskCompletion(
    val completionDate: String,
    val completedAt: String?,
)

data class Task(
    val id: String,
    val title: String,
    val createdDate: String?,
    val startDate: String?,
    val endDate: String?,
    val type: TaskType,
    val status: TaskStatus,
    val projectId: String? = null,
    val stageId: String? = null,
    val dailyStatistics: DailyTaskStatistics?,
    val dailyCompletions: List<DailyTaskCompletion>,
)

data class TaskInput(
    val title: String,
    val startDate: String?,
    val endDate: String?,
    val type: TaskType,
    val projectId: String? = null,
    val stageId: String? = null,
)

/** Campos opcionais: o back só recebe o que for enviado (`UpdateTaskDTO`). */
data class TaskUpdate(
    val title: String? = null,
    val type: TaskType? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: TaskStatus? = null,
    val completionDate: String? = null,
    val projectId: String? = null,
    val stageId: String? = null,
)

data class TaskPage(
    val total: Int,
    val page: Int,
    val size: Int,
    val items: List<Task>,
)

data class Note(val id: String, val folderId: String?, val title: String, val content: String)

data class NoteFolder(val id: String, val name: String, val notes: List<Note>)

data class NotePreview(val id: String, val title: String, val content: String)

data class FocusSession(val id: String, val title: String, val minutes: Int, val completedAt: String?)

data class WeeklyProgress(val completed: Int, val total: Int)

data class DashboardData(
    val weeklyProgress: WeeklyProgress,
    val tasks: List<Task>,
    val notes: List<NotePreview>,
)

enum class ProjectStatus { IN_PROGRESS, DONE }

data class Project(
    val id: String,
    val title: String,
    val progress: Int,
    val status: ProjectStatus,
    val advisor: String,
)

data class ProjectStage(val id: String, val name: String, val done: Boolean)

data class ProjectVersion(val id: String, val title: String, val dueDate: String)

data class ProjectDetails(
    val project: Project,
    val stages: List<ProjectStage>,
    val versions: List<ProjectVersion>,
)

data class AbntCheck(val label: String, val passed: Boolean)

enum class ReferenceStatus { VALID, ATTENTION }

data class ReferenceCheck(val citation: String, val status: ReferenceStatus)

data class ReviewReport(
    val documentTitle: String,
    val excerpt: String,
    val flaggedExcerpt: String,
    val similarityPercent: Int,
    val abntChecks: List<AbntCheck>,
    val references: List<ReferenceCheck>,
)
