package com.studyflow.app.domain.kanban

import com.studyflow.app.data.model.DailyTaskCompletion
import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.ProjectStatus
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.domain.ApiDates
import com.studyflow.app.domain.tasks.StudyCalendar
import java.time.Instant

/**
 * Regras do quadro Kanban, portadas de `features/kanban/useKanban.ts`. Tudo é puro (sem Android)
 * para ser testado na JVM.
 *
 * A coluna da tarefa prioriza a conclusão efetiva — numa DAILY isso depende do dia, não do
 * `status` — e só depois olha o andamento e o atraso, mantendo a semântica da lista/calendário.
 */
enum class KanbanEntity { TASKS, PROJECTS }

enum class TaskColumn { PENDING, WORKING, DELAYED, DONE }

enum class ProjectColumn { WAITING, WORKING, DONE }

data class TaskColumnView(
    val column: TaskColumn,
    val labelKey: String,
    val items: List<Task>,
)

data class ProjectColumnView(
    val column: ProjectColumn,
    val labelKey: String,
    val items: List<Project>,
)

/** O que o back precisa receber e como o cartão fica no quadro enquanto a chamada não volta. */
data class TaskMove(
    val status: TaskStatus,
    val completionDate: String?,
    val optimistic: Task,
)

object KanbanBoard {
    /** Atraso é derivado do prazo no backend: só uma UNIQUE vencida pode ir para DELAYED. */
    fun isTaskOverdue(task: Task, today: String): Boolean {
        if (task.type == TaskType.DAILY) return false
        val end = task.endDate?.let(ApiDates::dayKeyFrom) ?: return false
        return end < today
    }

    fun taskColumn(task: Task, today: String): TaskColumn {
        if (StudyCalendar.isTaskDone(task, today)) return TaskColumn.DONE
        if (task.status == TaskStatus.DELAYED) return TaskColumn.DELAYED
        if (task.status == TaskStatus.WORKING) return TaskColumn.WORKING
        return TaskColumn.PENDING
    }

    fun projectColumn(project: Project): ProjectColumn = when (project.status) {
        ProjectStatus.WAITING -> ProjectColumn.WAITING
        ProjectStatus.WORKING -> ProjectColumn.WORKING
        ProjectStatus.DONE -> ProjectColumn.DONE
    }

    fun taskColumns(tasks: List<Task>, today: String): List<TaskColumnView> {
        val groups = tasks.groupBy { taskColumn(it, today) }
        return listOf(
            TaskColumnView(TaskColumn.PENDING, "kanbanTasksPending", groups[TaskColumn.PENDING].orEmpty()),
            TaskColumnView(TaskColumn.WORKING, "kanbanTasksWorking", groups[TaskColumn.WORKING].orEmpty()),
            TaskColumnView(TaskColumn.DELAYED, "kanbanTasksDelayed", groups[TaskColumn.DELAYED].orEmpty()),
            TaskColumnView(TaskColumn.DONE, "kanbanTasksDone", groups[TaskColumn.DONE].orEmpty()),
        )
    }

    fun projectColumns(projects: List<Project>): List<ProjectColumnView> {
        val groups = projects.groupBy(::projectColumn)
        return listOf(
            ProjectColumnView(
                ProjectColumn.WAITING,
                "kanbanProjectsWaiting",
                groups[ProjectColumn.WAITING].orEmpty(),
            ),
            ProjectColumnView(
                ProjectColumn.WORKING,
                "kanbanProjectsInProgress",
                groups[ProjectColumn.WORKING].orEmpty(),
            ),
            ProjectColumnView(ProjectColumn.DONE, "kanbanProjectsDone", groups[ProjectColumn.DONE].orEmpty()),
        )
    }

    /**
     * O back só aceita `completionDate` explícito numa DAILY quando ela tem intervalo; sem ele, o
     * dia corrente fica implícito. Numa UNIQUE o dia acompanha a conclusão/reação — exceto no
     * atraso, que não é conclusão. DAILY não tem DELAYED, então esse movimento é inválido.
     */
    fun planTaskMove(
        task: Task,
        column: TaskColumn,
        today: String,
        now: String = Instant.now().toString(),
    ): TaskMove? {
        if (column == TaskColumn.DELAYED && !isTaskOverdue(task, today)) return null

        val done = column == TaskColumn.DONE
        val status = when (column) {
            TaskColumn.DELAYED -> TaskStatus.DELAYED
            TaskColumn.WORKING -> TaskStatus.WORKING
            TaskColumn.DONE -> TaskStatus.DONE
            TaskColumn.PENDING -> TaskStatus.PENDING
        }
        val completionDate = if (task.type == TaskType.DAILY) {
            if (!task.startDate.isNullOrBlank() || !task.endDate.isNullOrBlank()) today else null
        } else {
            if (column == TaskColumn.DELAYED) null else today
        }

        val optimistic = task.copy(
            status = status,
            completedDate = if (task.type == TaskType.DAILY) {
                task.completedDate
            } else {
                if (done) today else null
            },
            dailyStatistics = if (task.type == TaskType.DAILY) {
                task.dailyStatistics?.copy(completedToday = done)
            } else {
                task.dailyStatistics
            },
            dailyCompletions = if (task.type == TaskType.DAILY) {
                val withoutToday = task.dailyCompletions.filterNot {
                    ApiDates.dayKeyFrom(it.completionDate) == today
                }
                if (done) withoutToday + DailyTaskCompletion(today, now) else withoutToday
            } else {
                task.dailyCompletions
            },
        )

        return TaskMove(status = status, completionDate = completionDate, optimistic = optimistic)
    }

    fun planProjectMove(project: Project, column: ProjectColumn): Project =
        project.copy(
            status = when (column) {
                ProjectColumn.WAITING -> ProjectStatus.WAITING
                ProjectColumn.WORKING -> ProjectStatus.WORKING
                ProjectColumn.DONE -> ProjectStatus.DONE
            },
        )
}
