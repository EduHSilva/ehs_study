package com.studyflow.app.data.mappers

import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.remote.TaskDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    @Test
    fun `task keeps the unique completion date`() {
        val task = Mappers.task(
            TaskDto(
                id = "task-1",
                title = "Revisar capítulo",
                type = "UNIQUE",
                status = "DONE",
                completedDate = "2026-03-11",
            ),
        )

        assertEquals(TaskStatus.DONE, task.status)
        assertEquals("2026-03-11", task.completedDate)
    }

    @Test
    fun `task without completion date stays null`() {
        val task = Mappers.task(TaskDto(id = "task-2", title = "Ler artigo"))

        assertEquals(null, task.completedDate)
    }

    @Test
    fun `task keeps recurrence configuration`() {
        val task = Mappers.task(
            TaskDto(
                id = "task-3",
                title = "Meta semanal",
                type = "WEEKLY_GOAL",
                weeklyGoal = 2,
            ),
        )
        val monthly = Mappers.task(
            TaskDto(
                id = "task-4",
                title = "Relatório mensal",
                type = "MONTHLY",
                recurrenceDayOfMonth = 31,
            ),
        )
        val weekly = Mappers.task(
            TaskDto(
                id = "task-5",
                title = "Aula",
                type = "WEEKLY",
                recurrenceDays = listOf(2, 4),
            ),
        )

        assertEquals(com.studyflow.app.data.model.TaskType.WEEKLY_GOAL, task.type)
        assertEquals(2, task.weeklyGoal ?: 0)
        assertEquals(31, monthly.recurrenceDayOfMonth ?: 0)
        assertEquals(listOf(2, 4), weekly.recurrenceDays)
    }
}
