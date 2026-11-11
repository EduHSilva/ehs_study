package com.studyflow.app.feature.dashboard

import com.studyflow.app.data.model.DashboardData
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.data.model.WeeklyProgress
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DashboardUiStateTest {

    private fun task(id: String, day: LocalDate): Task = Task(
        id = id,
        title = id,
        createdDate = null,
        startDate = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toString(),
        endDate = day.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toString(),
        type = TaskType.UNIQUE,
        status = TaskStatus.PENDING,
        dailyStatistics = null,
        dailyCompletions = emptyList(),
    )

    @Test
    fun `today tasks ignore the backend next 24h window`() {
        val today = LocalDate.now()
        val state = DashboardUiState(
            data = DashboardData(
                weeklyProgress = WeeklyProgress(completed = 0, total = 0),
                tasks = listOf(task("hoje", today), task("amanha", today.plusDays(1))),
                notes = emptyList(),
            ),
        )

        assertEquals(listOf("hoje"), state.todayTasks.map { it.id })
    }
}
