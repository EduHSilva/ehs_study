package com.studyflow.app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ---------------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------------

@Serializable
data class LoginRequest(
    val email: String? = null,
    val password: String? = null,
    val tokenGmail: String? = null,
    val app: String,
)

@Serializable
data class LoginDto(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val lastLogin: String? = null,
    val token: String? = null,
    val course: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val role: String,
)

@Serializable
data class RegisterDto(
    val id: String = "",
    val createdDate: String? = null,
    val email: String = "",
    val username: String = "",
    val status: String = "",
    val roles: List<String> = emptyList(),
    val companyName: String? = null,
)

@Serializable
data class UpdateUserRequest(val username: String)

// ---------------------------------------------------------------------------
// Tasks
// ---------------------------------------------------------------------------

@Serializable
data class DailyStatsDto(
    val scheduledDays: Int? = null,
    val completedDays: Int? = null,
    val skippedDays: Int? = null,
    val completedToday: Boolean? = null,
)

@Serializable
data class DailyCompletionDto(
    val completionDate: String? = null,
    val completedAt: String? = null,
)

@Serializable
data class TaskDto(
    val id: String? = null,
    val title: String? = null,
    val createdDate: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val dueDate: String? = null,
    val schedule: String? = null,
    val type: String? = null,
    val status: String? = null,
    val completed: Boolean? = null,
    val completedDate: String? = null,
    val recurrenceDays: List<Int>? = null,
    val recurrenceDayOfMonth: Int? = null,
    val weeklyGoal: Int? = null,
    val projectId: String? = null,
    val stageId: String? = null,
    val dailyStatistics: DailyStatsDto? = null,
    val dailyCompletions: List<DailyCompletionDto>? = null,
)

@Serializable
data class TaskPageDto(
    val total: Int? = null,
    val page: Int? = null,
    val size: Int? = null,
    val items: List<TaskDto>? = null,
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val type: String,
    val projectId: String? = null,
    val stageId: String? = null,
    val recurrenceDays: List<Int>? = null,
    val recurrenceDayOfMonth: Int? = null,
    val weeklyGoal: Int? = null,
)

/**
 * `projectId`/`stageId` são `JsonElement` porque o cliente precisa distinguir "não enviar"
 * (null omitido) de "enviar null" (JsonNull), que é como o web desvincula a tarefa do projeto.
 */
@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val type: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String? = null,
    val completionDate: String? = null,
    val projectId: JsonElement? = null,
    val stageId: JsonElement? = null,
    val recurrenceDays: List<Int>? = null,
    val recurrenceDayOfMonth: Int? = null,
    val weeklyGoal: Int? = null,
)

// ---------------------------------------------------------------------------
// Notes
// ---------------------------------------------------------------------------

@Serializable
data class NoteDto(
    val id: String = "",
    val folderId: String? = null,
    val title: String? = null,
    val content: String? = null,
)

@Serializable
data class FolderDto(
    val id: String = "",
    val name: String? = null,
    val notes: List<NoteDto>? = null,
)

@Serializable
data class FolderPageDto(val items: List<FolderDto>? = null)

@Serializable
data class SaveNoteRequest(val title: String, val content: String)

@Serializable
data class FolderRequest(val name: String)

// ---------------------------------------------------------------------------
// Focus / Pomodoro
// ---------------------------------------------------------------------------

@Serializable
data class FocusSessionDto(
    val id: String = "",
    val title: String = "",
    val minutes: Int = 0,
    val completedAt: String? = null,
)

@Serializable
data class FocusPageDto(val items: List<FocusSessionDto>? = null)

@Serializable
data class CreateFocusRequest(val title: String, val minutes: Int)

// ---------------------------------------------------------------------------
// Dashboard
// ---------------------------------------------------------------------------

@Serializable
data class WeeklyProgressDto(val completed: Int? = null, val total: Int? = null)

@Serializable
data class NotePreviewDto(
    val id: String = "",
    val title: String = "",
    val content: String = "",
)

@Serializable
data class DashboardDto(
    val weeklyProgress: WeeklyProgressDto? = null,
    val tasks: List<TaskDto>? = null,
    // Nome antigo do contrato: o endpoint ainda pode entregar a lista como `reminders`.
    val reminders: List<TaskDto>? = null,
    val notes: List<NotePreviewDto>? = null,
)

// ---------------------------------------------------------------------------
// Projects
// ---------------------------------------------------------------------------

@Serializable
data class VersionDto(
    val id: String = "",
    val title: String = "",
    val dueDate: String = "",
)

@Serializable
data class StageDto(
    val id: String = "",
    val name: String? = null,
    val title: String? = null,
    val done: Boolean? = null,
)

@Serializable
data class ProjectDto(
    val id: String = "",
    val title: String = "",
    val status: String? = null,
    val progress: Int? = null,
    val advisor: String? = null,
    val stages: List<StageDto>? = null,
    val versions: List<VersionDto>? = null,
)

@Serializable
data class ProjectPageDto(val items: List<ProjectDto>? = null)

@Serializable
data class CreateProjectRequest(
    val title: String,
    val status: String = "WAITING",
    val advisor: String? = null,
)

@Serializable
data class UpdateProjectRequest(
    val title: String,
    val advisor: String? = null,
    val status: String? = null,
)

@Serializable
data class StageRequest(val idProject: String, val title: String, val done: Boolean)

@Serializable
data class VersionRequest(val idProject: String, val title: String, val dueDate: String)

// ---------------------------------------------------------------------------
// Review
// ---------------------------------------------------------------------------

@Serializable
data class AbntCheckDto(val label: String = "", val passed: Boolean = false)

@Serializable
data class ReferenceCheckDto(val citation: String = "", val status: String = "")

@Serializable
data class ReviewReportDto(
    val documentTitle: String = "",
    val excerpt: String = "",
    val flaggedExcerpt: String = "",
    val similarityPercent: Int = 0,
    val abntChecks: List<AbntCheckDto>? = null,
    val references: List<ReferenceCheckDto>? = null,
)
