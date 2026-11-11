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
}
