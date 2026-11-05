package com.studyflow.app.domain.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TaskFormatTest {
    private val ptBr = Locale.forLanguageTag("pt-BR")

    @Test
    fun `empty end date renders a dash`() {
        assertEquals("—", TaskFormat.formatEndDate(null, ptBr))
        assertEquals("—", TaskFormat.formatEndDate("", ptBr))
    }

    @Test
    fun `end date is formatted as day and time`() {
        assertEquals("10/03, 14:30", TaskFormat.formatEndDate("2026-03-10T14:30:00", ptBr))
    }

    @Test
    fun `period with the same day shows only the due date`() {
        val period = TaskFormat.formatTaskPeriod(
            startDate = "2026-03-10T09:00:00",
            endDate = "2026-03-10T18:00:00",
            locale = ptBr,
        )

        assertEquals("10/03, 18:00", period)
    }

    @Test
    fun `period across days shows start arrow end`() {
        val period = TaskFormat.formatTaskPeriod(
            startDate = "2026-03-10T09:00:00",
            endDate = "2026-03-12T18:00:00",
            locale = ptBr,
        )

        assertEquals("10/03 → 12/03, 18:00", period)
    }

    @Test
    fun `period without start shows the due date`() {
        val period = TaskFormat.formatTaskPeriod(
            startDate = null,
            endDate = "2026-03-12T18:00:00",
            locale = ptBr,
        )

        assertEquals("12/03, 18:00", period)
    }

    @Test
    fun `day chip renders weekday and day`() {
        val chip = TaskFormat.formatDayChip("2026-03-10", ptBr)
        assertTrue(chip.contains("10/03"))
    }

    @Test
    fun `invalid day chip falls back to the raw key`() {
        assertEquals("not-a-date", TaskFormat.formatDayChip("not-a-date", ptBr))
    }
}
