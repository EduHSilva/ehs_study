package com.studyflow.app.data.repository

import com.studyflow.app.data.mappers.Mappers
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskInput
import com.studyflow.app.data.model.TaskPage
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.data.model.TaskUpdate
import com.studyflow.app.data.remote.CreateTaskRequest
import com.studyflow.app.data.remote.TasksApi
import com.studyflow.app.data.remote.UpdateTaskRequest
import com.studyflow.app.core.network.unwrap
import com.studyflow.app.domain.tasks.StudyCalendar
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

class TasksRepository(private val api: TasksApi) {
    suspend fun getTasks(page: Int = 0, size: Int = 10): TaskPage {
        val payload = api.list(page = page, size = size).unwrap()
        return TaskPage(
            total = payload.total ?: 0,
            page = payload.page ?: page,
            size = payload.size ?: size,
            items = payload.items.orEmpty().map { Mappers.task(it) },
        )
    }

    /**
     * Filtro por período. O back responde 500 quando `start`/`end` recebem valor; nesse caso o
     * web refaz a busca com uma janela larga e filtra no cliente — e é o que fazemos aqui também.
     * O recorte final usa o calendário local, com um dia de folga em cada lado na consulta.
     */
    suspend fun getTasksInRange(from: LocalDate, to: LocalDate): List<Task> {
        val collected = try {
            fetchAllPages(
                start = StudyCalendar.dayKey(from.minusDays(1)),
                end = StudyCalendar.dayKey(to.plusDays(1)),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fetchAllPages(
                start = LocalDate.of(from.year - 2, 1, 1).toString(),
                end = LocalDate.of(to.year + 2, 12, 31).toString(),
            )
        }
        return collected.filter { task ->
            if (task.status == TaskStatus.DELETED) return@filter false
            if (task.type == TaskType.DAILY) {
                val start = StudyCalendar.taskStart(task)
                start == null || !start.isAfter(to)
            } else {
                val taskFrom = StudyCalendar.taskStart(task) ?: return@filter false
                val taskTo = StudyCalendar.taskEnd(task) ?: return@filter false
                !taskTo.isBefore(from) && !taskFrom.isAfter(to)
            }
        }
    }

    private suspend fun fetchAllPages(start: String?, end: String?, projectId: String? = null): List<Task> {
        val collected = ArrayList<Task>()
        var page = 0
        while (true) {
            val payload = api.list(
                page = page,
                size = RANGE_PAGE_SIZE,
                start = start,
                end = end,
                projectId = projectId,
            ).unwrap()
            val items = payload.items.orEmpty().map { Mappers.task(it) }
            collected += items
            val total = payload.total ?: collected.size
            if (items.isEmpty() || collected.size >= total || page >= MAX_RANGE_PAGES) break
            page += 1
        }
        return collected
    }

    suspend fun getTasksForProject(projectId: String): List<Task> {
        val today = LocalDate.now()
        val tasks = fetchAllPages(
            start = LocalDate.of(today.year - 2, 1, 1).toString(),
            end = LocalDate.of(today.year + 2, 12, 31).toString(),
            projectId = projectId,
        )
        return tasks.filter { it.status != TaskStatus.DELETED }
    }

    suspend fun create(input: TaskInput): Task {
        val payload = api.create(
            CreateTaskRequest(
                title = input.title,
                startDate = input.startDate,
                endDate = input.endDate,
                type = input.type.name,
                projectId = input.projectId,
                stageId = input.stageId,
            ),
        ).unwrap()
        return Mappers.task(payload)
    }

    suspend fun update(id: String, update: TaskUpdate): Task {
        val payload = api.update(
            id,
            UpdateTaskRequest(
                title = update.title,
                type = update.type?.name,
                startDate = update.startDate,
                endDate = update.endDate,
                status = update.status?.name,
                completionDate = update.completionDate,
                projectId = update.projectId,
                stageId = update.stageId,
            ),
        ).unwrap()
        return Mappers.task(payload)
    }

    suspend fun delete(id: String) {
        api.delete(id)
    }

    private companion object {
        const val RANGE_PAGE_SIZE = 100
        const val MAX_RANGE_PAGES = 50
    }
}
