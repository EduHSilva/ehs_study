package com.studyflow.app.domain.tasks

import com.studyflow.app.data.model.DailyTaskCompletion
import com.studyflow.app.data.model.DailyTaskStatistics
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StudyCalendarTest {

    private fun uniqueTask(
        start: String?,
        end: String?,
        status: TaskStatus = TaskStatus.PENDING,
        completedDate: String? = null,
    ) = Task(
        id = "unique",
        title = "Revisar capítulo",
        createdDate = null,
        startDate = start,
        endDate = end,
        type = TaskType.UNIQUE,
        status = status,
        completedDate = completedDate,
        dailyStatistics = null,
        dailyCompletions = emptyList(),
    )

    private fun dailyTask(
        start: String?,
        end: String?,
        completions: List<String> = emptyList(),
        completedToday: Boolean = false,
    ) = Task(
        id = "daily",
        title = "Ler 10 páginas",
        createdDate = null,
        startDate = start,
        endDate = end,
        type = TaskType.DAILY,
        status = TaskStatus.PENDING,
        dailyStatistics = DailyTaskStatistics(
            scheduledDays = 0,
            completedDays = completions.size,
            skippedDays = 0,
            completedToday = completedToday,
        ),
        dailyCompletions = completions.map { DailyTaskCompletion(it, null) },
    )

    @Test
    fun `week starts on monday`() {
        // 2026-03-15 é domingo.
        assertEquals(LocalDate.of(2026, 3, 9), StudyCalendar.startOfWeek(LocalDate.of(2026, 3, 15)))
        assertEquals(LocalDate.of(2026, 3, 9), StudyCalendar.startOfWeek(LocalDate.of(2026, 3, 9)))
        assertEquals(LocalDate.of(2026, 3, 9), StudyCalendar.startOfWeek(LocalDate.of(2026, 3, 11)))
    }

    @Test
    fun `unique task covers every day of its range`() {
        val task = uniqueTask("2026-03-10T09:00:00", "2026-03-12T18:00:00")
        val today = "2026-03-11"

        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-10", today).isNotEmpty())
        assertEquals(1, StudyCalendar.tasksForDay(listOf(task), "2026-03-11", today).size)
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-12", today).isNotEmpty())
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-13", today).isEmpty())
    }

    @Test
    fun `unique task without end date is a single day`() {
        val task = uniqueTask("2026-03-10T09:00:00", null)
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-10", "2026-03-11").isNotEmpty())
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-11", "2026-03-11").isEmpty())
    }

    @Test
    fun `unique task is done when status is done`() {
        val pending = uniqueTask("2026-03-10T09:00:00", "2026-03-10T18:00:00")
        val done = pending.copy(status = TaskStatus.DONE)

        assertFalse(StudyCalendar.tasksForDay(listOf(pending), "2026-03-10", "2026-03-10").single().done)
        assertTrue(StudyCalendar.tasksForDay(listOf(done), "2026-03-10", "2026-03-10").single().done)
        assertTrue(StudyCalendar.isTaskDone(done, "2026-03-10"))
    }

    @Test
    fun `done unique task only occupies its completed date`() {
        val task = uniqueTask(
            start = "2026-03-10T09:00:00",
            end = "2026-03-12T18:00:00",
            status = TaskStatus.DONE,
            completedDate = "2026-03-11",
        )
        val today = "2026-03-11"

        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-10", today).isEmpty())
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-11", today).single().done)
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-12", today).isEmpty())
    }

    @Test
    fun `legacy done unique task without completed date keeps the whole range`() {
        val task = uniqueTask(
            start = "2026-03-10T09:00:00",
            end = "2026-03-12T18:00:00",
            status = TaskStatus.DONE,
        )
        val today = "2026-03-11"

        assertEquals(1, StudyCalendar.tasksForDay(listOf(task), "2026-03-11", today).size)
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-11", today).single().done)
    }

    @Test
    fun `daily task only appears from its start date`() {
        val task = dailyTask("2026-03-10T08:00:00", null, completions = listOf("2026-03-10", "2026-03-11"))
        val today = "2026-03-12"

        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-09", today).isEmpty())
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-10", today).single().done)
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-11", today).single().done)
        assertFalse(StudyCalendar.tasksForDay(listOf(task), "2026-03-12", today).single().done)
    }

    @Test
    fun `daily completion falls back to completedToday`() {
        val task = dailyTask("2026-03-10T08:00:00", null, completedToday = true)
        assertTrue(StudyCalendar.isCompletedOn(task, "2026-03-12", "2026-03-12"))
        assertFalse(StudyCalendar.isCompletedOn(task, "2026-03-11", "2026-03-12"))
    }

    @Test
    fun `deleted tasks are ignored`() {
        val task = uniqueTask("2026-03-10T09:00:00", "2026-03-10T18:00:00", TaskStatus.DELETED)
        assertTrue(StudyCalendar.tasksForDay(listOf(task), "2026-03-10", "2026-03-10").isEmpty())
    }

    @Test
    fun `month grid pads the weeks around the month`() {
        val grid = StudyCalendar.buildCalendarGrid(
            tasks = emptyList(),
            anchor = LocalDate.of(2026, 3, 15),
            mode = StudyCalendar.CalendarMode.MONTH,
            today = "2026-03-15",
        )

        assertEquals(42, grid.size)
        assertEquals("2026-02-23", grid.first().key)
        assertEquals("2026-04-05", grid.last().key)
        assertTrue(grid.first { it.key == "2026-02-23" }.outside)
        assertFalse(grid.first { it.key == "2026-03-10" }.outside)
        assertTrue(grid.first { it.key == "2026-04-01" }.outside)
    }

    @Test
    fun `week grid has seven days`() {
        val grid = StudyCalendar.buildCalendarGrid(
            tasks = emptyList(),
            anchor = LocalDate.of(2026, 3, 15),
            mode = StudyCalendar.CalendarMode.WEEK,
            today = "2026-03-15",
        )

        assertEquals(7, grid.size)
        assertEquals("2026-03-09", grid.first().key)
        assertEquals("2026-03-15", grid.last().key)
        assertTrue(grid.none { it.outside })
    }

    @Test
    fun `strict range for month ignores the grid padding`() {
        val (from, to) = StudyCalendar.rangeFor(LocalDate.of(2026, 3, 15), StudyCalendar.CalendarMode.MONTH)
        assertEquals(LocalDate.of(2026, 3, 1), from)
        assertEquals(LocalDate.of(2026, 3, 31), to)

        val (gridFrom, gridTo) = StudyCalendar.gridRangeFor(LocalDate.of(2026, 3, 15), StudyCalendar.CalendarMode.MONTH)
        assertEquals(LocalDate.of(2026, 2, 23), gridFrom)
        assertEquals(LocalDate.of(2026, 4, 5), gridTo)
    }

    @Test
    fun `day summaries count done and total`() {
        val done = uniqueTask("2026-03-10T09:00:00", "2026-03-11T18:00:00", TaskStatus.DONE)
        val pending = uniqueTask("2026-03-10T09:00:00", "2026-03-10T18:00:00")

        val summaries = StudyCalendar.buildDaySummaries(
            tasks = listOf(done, pending),
            from = LocalDate.of(2026, 3, 10),
            to = LocalDate.of(2026, 3, 11),
            today = "2026-03-10",
        )

        assertEquals(2, summaries.size)
        assertEquals(2, summaries[0].total)
        assertEquals(1, summaries[0].done)
        assertEquals(StudyCalendar.DayStatus.PARTIAL, summaries[0].status)
        assertEquals(StudyCalendar.DayStatus.DONE, summaries[1].status)
    }
}
