package com.studyflow.app.domain.tasks

import com.studyflow.app.data.model.DailyTaskCompletion
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskStatsTest {

    private fun task(
        type: TaskType,
        start: String?,
        end: String?,
        days: List<Int> = emptyList(),
        goal: Int? = null,
        completions: List<String> = emptyList(),
    ) = Task(
        id = type.name,
        title = type.name,
        createdDate = null,
        startDate = start,
        endDate = end,
        type = type,
        status = TaskStatus.PENDING,
        recurrenceDays = days,
        weeklyGoal = goal,
        dailyStatistics = null,
        dailyCompletions = completions.map { DailyTaskCompletion(it, null) },
    )

    @Test
    fun `daily stats count completions and closed failures`() {
        // 2026-03-10 (terça) a 13 (sexta), hoje = 13: concluído só na terça.
        val daily = task(
            TaskType.DAILY,
            start = "2026-03-10T08:00:00",
            end = "2026-03-13T08:00:00",
            completions = listOf("2026-03-10"),
        )

        val stats = TaskStatsBuilder.build(
            tasks = listOf(daily),
            from = LocalDate.of(2026, 3, 10),
            to = LocalDate.of(2026, 3, 13),
            today = "2026-03-13",
        ).single()

        assertEquals(1, stats.completed)
        assertEquals(2, stats.failed)
        assertEquals(4, stats.scheduled)
        assertEquals(25, stats.rate)
    }

    @Test
    fun `today is not counted as a failure yet`() {
        val daily = task(
            TaskType.DAILY,
            start = "2026-03-13T08:00:00",
            end = "2026-03-13T20:00:00",
        )

        val stats = TaskStatsBuilder.build(
            tasks = listOf(daily),
            from = LocalDate.of(2026, 3, 13),
            to = LocalDate.of(2026, 3, 13),
            today = "2026-03-13",
        ).single()

        assertEquals(0, stats.failed)
        assertEquals(1, stats.scheduled)
        assertEquals(0, stats.rate)
    }

    @Test
    fun `weekly stats only count scheduled weekdays`() {
        // Segunda 02 a domingo 15. Semanas: 02-08 e 09-15 (terças e quintas).
        val weekly = task(
            TaskType.WEEKLY,
            start = "2026-03-02T08:00:00",
            end = "2026-03-15T20:00:00",
            days = listOf(2, 4),
            completions = listOf("2026-03-03", "2026-03-05"),
        )

        val stats = TaskStatsBuilder.build(
            tasks = listOf(weekly),
            from = LocalDate.of(2026, 3, 2),
            to = LocalDate.of(2026, 3, 15),
            today = "2026-03-13",
        ).single()

        assertEquals(2, stats.completed)
        assertEquals(2, stats.failed)
        assertEquals(4, stats.scheduled)
        assertEquals(50, stats.rate)
    }

    @Test
    fun `weekly goal evaluates closed weeks and the current one separately`() {
        // Semana 1 (02-08): meta batida; semana 2 (09-15): só 1 de 2; hoje 18 está na semana 3.
        val goalTask = task(
            TaskType.WEEKLY_GOAL,
            start = "2026-03-02T08:00:00",
            end = "2026-03-22T20:00:00",
            goal = 2,
            completions = listOf("2026-03-03", "2026-03-05", "2026-03-10"),
        )

        val stats = TaskStatsBuilder.build(
            tasks = listOf(goalTask),
            from = LocalDate.of(2026, 3, 2),
            to = LocalDate.of(2026, 3, 22),
            today = "2026-03-18",
        ).single()

        assertEquals(1, stats.weeksOk)
        assertEquals(1, stats.weeksFailed)
        assertEquals(50, stats.rate)
        assertEquals(0, stats.currentWeekDone)
        assertEquals(2, stats.currentWeekGoal ?: 0)
        assertFalse(stats.currentWeekMet)
        assertEquals(3, stats.completed)
    }

    @Test
    fun `unique tasks are left out of recurring stats`() {
        val unique = task(TaskType.UNIQUE, start = "2026-03-10T08:00:00", end = "2026-03-12T20:00:00")

        val stats = TaskStatsBuilder.build(
            tasks = listOf(unique),
            from = LocalDate.of(2026, 3, 2),
            to = LocalDate.of(2026, 3, 15),
            today = "2026-03-13",
        )

        assertTrue(stats.isEmpty())
    }
}
